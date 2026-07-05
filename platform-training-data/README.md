# platform-training-data

训练数据模块第一版实现 Codeforces 独立 OJ 数仓建模和批量写入入口。

当前范围：

- `training-data-codeforces`：Codeforces 入口、ingest app service、最近小时数 submission 采集器、批次上下文、ODS record/parser/writer、CF handle-account 维护、ODS/DWD/DWM/DWS DDL、幂等 SQL 任务、SQL 任务 manifest、admin warehouse refresh 入口、fixture/tests。
- `training-data-web`：Spring Boot 入口、模块健康检查、模块信息、文件日志、Flyway 迁移、OJ-specific HTTP 写入入口、admin 最近小时数采集入口和 admin SQL refresh 运行时。

当前真正落地 Codeforces 垂直数仓链路：

```text
ods_codeforces__submission
 -> dwd_codeforces__submission
 -> dwm_codeforces__handle_problem_first_accepted
 -> dws_codeforces__handle_daily_rating_accepted_summary
```

每个 OJ 是独立数仓域，入口和数据组织也在各自 Maven 模块里；ADS、持久化 pipeline 调度器和 pipeline run state 还没有实现。当前 Codeforces DWD/DWM/DWS SQL 任务资源由 `platform-common/common-core` 的 SQL task DAG 执行器触发，Codeforces 入口传本地 manifest、`batchId` 和由该 batch 提交时间推导出的 UTC+8 日期闭区间；执行器每次读取 manifest、重建图并校验 DAG。任务按区间删除上层表后从下层表回填。

外置采集器可通过 OJ-specific HTTP 接口写入原始 submission 数组：

```text
POST /api/training-data/admin/ods/codeforces/submissions:batch-upsert
```

这个写入接口属于平台 admin URL tier，需要平台 JWT 中带 `admin` 角色。启动 `training-data-web` 时，Flyway 会从 OJ 模块的 `classpath:db/migration` 下应用 ODS/DWD/DWM/DWS 和 CF handle-account 建表脚本。

管理员也可以让后端按 `studentIdentity` 采集最近小时数的 Codeforces 提交并写入 ODS：

```text
POST /api/training-data/admin/codeforces/submissions:collect
```

请求体传 `studentIdentity` 和 `lookbackHours`，服务先解析出该身份绑定的 Codeforces handle，再从当前执行时间往前采集指定小时数；采集右边界始终是服务当前 instant。后台定时采集当前直接读取 `codeforces_handle_account` 全量绑定并按 handle 去重。每次 Codeforces 分页请求默认使用 `connect-timeout=10s`、`read-timeout=30s`，并最多尝试 `max-request-attempts=3` 次。某个 handle 的分页请求重试耗尽后会记录稳定错误码日志，并在响应的 per-handle 结果中返回失败原因。ODS 写入后保留了后续调度调用的代码注释，当前不触发未完成的调度逻辑。

后台定时采集由 `application.yml` 的 `platform.training-data.codeforces.collector.schedules` 列表驱动，默认调度项 `daily-recent-submissions` 处于关闭状态。显式把该调度项的 `enabled` 改为 `true` 后，服务会按该调度项的 `cron` 和 `zone` 触发，默认每天 12:00 采集最近 `lookback=120h` 的滚动小时窗口。

管理员可以同步触发 Codeforces DWD/DWM/DWS 刷新：

```text
POST /api/training-data/admin/codeforces/warehouse:refresh
```

请求体传 `batchId`，可选 `startFromTaskId` 用于从失败节点继续执行该节点及其下游。若 `batchId` 没有可刷新的 ODS 提交区间，接口返回 `400` 且不会执行 SQL task。一个 SQL 节点使用一个事务；节点失败后当前 DAG 立即停止，并在响应和日志中保留失败节点与稳定错误码。

Codeforces 模块还维护 `studentIdentity` 到 Codeforces handle 的绑定表：

```text
codeforces_handle_account
```

管理员可以创建绑定和迁移绑定的 `studentIdentity`，但该接口不修改 handle，也不修改 auth 登录账号。游客可以按 `studentIdentity` 查询已绑定的 Codeforces handle。

Codeforces 内部 DWD/DWM/DWS app 查询以平台 `studentIdentity` 作为个人维度入口；app service 会先解析到绑定的 Codeforces handle，再复用按 `author_handle` 建模的仓储和数仓表查询。问题维度查询结果也会反查 `studentIdentity`，遇到未绑定 handle 会失败。

这些 DWD/DWM/DWS 读侧能力已经公开为无需鉴权的 guest HTTP 查询接口，路径位于 `/api/training-data/codeforces/accepted-summary`、`/api/training-data/codeforces/submissions/by-*` 和 `/api/training-data/codeforces/first-accepted/by-*`。

ODS 字段核对、DWD/DWM/DWS 表粒度、SQL 任务资源和写入接口说明见 [docs/ods-submission.md](docs/ods-submission.md)。本地链路测试数据见 [docs/test-data.md](docs/test-data.md)。
