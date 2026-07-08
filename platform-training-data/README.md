# platform-training-data

训练数据模块第一版实现 Codeforces/AtCoder 独立 OJ 数仓建模、最近窗口采集、通用学生训练数据清理，并把 DWD/DWM/DWS 公共读侧查询扩展为按 OJ 名选择同层同结构表。

当前范围：

- `training-data-common`：训练数据 OJ 公共 app/domain/infra/web 模块；当前承载 OJ handle-account 用例、仓储、HTTP 入口和 Spring bean 装配，公共 DWD/DWM/DWS query 用例、仓储和 HTTP 入口，公共同层 DWD/DWM/DWS 建表迁移，学生训练数据清理编排和 HTTP 入口，submission 采集的窗口计算、handle 解析、限速/重试、窗口过滤、结果聚合模型、OJ collector dispatch、通用 warehouse refresh interval/service/handler 和 dispatcher、进程内采集任务编排、采集 HTTP 入口、通用 SQL task runner bean、定时采集配置和训练数据内通用文本参数校验，不拥有 OJ 源客户端、ODS record/parser/writer、OJ-specific ODS 建表、OJ-specific source parsing、OJ-specific refresh SQL 或 OJ SQL task manifest。
- `training-data-codeforces`：Codeforces ingest app service、最近小时数 submission 采集 adapter/facade、批次上下文、ODS record/parser/writer、Codeforces ODS purge adapter、Codeforces refresh interval JDBC adapter、Codeforces ODS/DWD/DWM/DWS DDL、幂等 SQL 任务、SQL 任务 manifest、fixture/tests；Spring 配置注册 Codeforces 自己的 source、ODS、采集实现，并用 common 的 refresh service/handler 绑定 Codeforces manifest 和 interval adapter。
- `training-data-atcoder`：AtCoder 垂直 OJ 模块；当前承载 Kenkoooo AtCoder submission/problem ODS 落地表迁移、Kenkoooo source client、submission/problem parser、ODS writer、AtCoder 最近窗口 submission 采集 adapter/facade、启动和低频题目列表采集服务、AtCoder ODS purge adapter、AtCoder refresh interval JDBC adapter、AtCoder DWD/DWM/DWS 幂等 SQL 任务、SQL 任务 manifest 和测试。AtCoder DWD/DWM/DWS 物理表遵守公共同层仓库契约，由 `training-data-common` 提供建表迁移。
- `training-data-web`：Spring Boot 入口、模块健康检查、模块信息、文件日志、Flyway 迁移和平台 URL 授权；common/codeforces/atcoder 模块提供实际业务 bean 与 HTTP controller。

当前 Codeforces ODS 到仓库刷新链路：

```text
ods_codeforces__submission
 -> dwd_codeforces__submission
 -> dwm_codeforces__handle_problem_first_accepted
 -> dws_codeforces__handle_daily_rating_accepted_summary
```

Codeforces 采集按单个 `user.status?handle=...` 页面执行：如果返回团队提交，系统会校验被采集的 handle 出现在 `author.members` 中，并把该 submission 归因给本次采集的 handle。若同一团队提交后来又从另一个已绑定成员的页面采到，`submission + handle` 唯一键允许它作为另一个成员的独立记录存在。

当前 AtCoder ODS 到仓库刷新链路：

```text
ods_atcoder__submission
ods_atcoder__problem
 -> dwd_atcoder__submission
 -> dwm_atcoder__handle_problem_first_accepted
 -> dws_atcoder__handle_daily_rating_accepted_summary
```

`ods_atcoder__submission` 对应 Kenkoooo `user/submissions?user={user_id}&from_second={unix_second}`，`ods_atcoder__problem` 对应 `resources/problems.json`。AtCoder submission 最近窗口采集通过公共 Codeforces-compatible route 传 `ojName=ATCODER` 触发；批量采集任务设置 `refreshWarehouse=true` 时会通过公共 refresh dispatcher 调用 AtCoder 自己的 SQL task manifest，定时自动采集产生 `batchId` 后也会调用同一套 refresh dispatcher。AtCoder DWD SQL 会左连接 `ods_atcoder__problem` 补齐题目序号和展示名。题目列表变化低频，通过启动 bootstrap 和默认开启的低频调度刷新，不再暴露手动 HTTP 刷新入口。Kenkoooo 文档要求访问间隔超过 1 秒，当前 AtCoder 默认请求间隔为 `2s`。

每个 OJ 是独立数仓域，OJ-specific 数据组织在各自 Maven 模块里，公共 HTTP 入口在 `training-data-common`；ADS、持久化 pipeline 调度器和 pipeline run state 还没有实现。当前 Codeforces/AtCoder DWD/DWM/DWS SQL 任务资源由 `platform-common/common-core` 的 SQL task DAG 执行器触发，common refresh service 传 OJ 配置的本地 manifest、`batchId` 和由 OJ-specific interval repository 推导出的 UTC+8 日期闭区间；执行器每次读取 manifest、重建图并校验 DAG。任务按区间删除上层表后从下层表回填。

DWD/DWM/DWS 公共查询使用同层多表模型，表结构和查询方法一致，按透传的受控 OJ 名选择 `dwd_{oj}__submission`、`dwm_{oj}__handle_problem_first_accepted`、`dws_{oj}__handle_daily_rating_accepted_summary`。当前落地建表和清洗刷新覆盖 `CODEFORCES` 和 `ATCODER`；AtCoder 暂无难度来源，DWD/DWM 的 `difficulty` 写入 `null`，DWS 聚合为 `UNRATED`。

ODS 写入现在只作为采集链路内部实现暴露给 app service，不再提供手动 HTTP 导入入口。启动 `training-data-web` 时，Flyway 会从 `classpath:db/migration` 应用 OJ 模块的 ODS 建表/升级脚本、历史 Codeforces 仓库脚本和 common 模块的公共同层 DWD/DWM/DWS 建表脚本。

本分支把 Codeforces DWD/DWM/DWS 调整为和 AtCoder 一致的公共同层仓库表契约。`V017__reshape_codeforces_warehouse_to_common_contract.sql` 是一次明确的破坏性仓库重建迁移：它会删除并重建 `dwd_codeforces__submission`、`dwm_codeforces__handle_problem_first_accepted` 和 `dws_codeforces__handle_daily_rating_accepted_summary`，不会把旧 DWD/DWM/DWS 数据迁入新表。Codeforces ODS 原始表、OJ handle 绑定和 auth 账号不由该迁移删除。部署者应按“上线后从空仓库重新生成查询层数据”处理；在重新采集并刷新仓库前，依赖 DWD/DWM/DWS 的公开查询可能返回空结果。该重建是本次 `V017` 升级的一次性影响，当前其它迁移不要求清空或重建数仓；后续升级默认不重建数仓，除非对应 Flyway 脚本、模块文档和 changelog 再次明确声明破坏性重建。

管理员也可以让后端按 `studentIdentity` 采集最近小时数的 Codeforces 提交并写入 ODS：

```text
POST /api/training-data/admin/codeforces/submissions:collect
```

请求体传 `studentIdentity`、`lookbackHours` 和可选 `ojName`。`ojName` 为空时使用 dispatcher 的默认 OJ，当前默认是 `CODEFORCES`；传 `ATCODER` 时走 Kenkoooo AtCoder 采集器。公共采集编排先从该身份绑定的 `handles_json` map 里取目标 OJ handle，再从当前执行时间往前采集指定小时数；采集右边界始终是服务当前 instant。后台定时采集由 schedule 的 `ojName` 决定读取哪个 OJ handle，并读取 `oj_handle_account.need_collect=true` 的绑定后按该 OJ handle 去重。公共编排负责运行互斥、窗口计算、handle 结果聚合、请求限速和重试；Codeforces adapter 负责 `user.status` 的 `from/count` 分页、`creationTimeSeconds` 窗口过滤、源站错误码映射和 ODS 写入；AtCoder adapter 负责 Kenkoooo `from_second` 分页、`epoch_second` 窗口过滤、源站错误码映射和 ODS 写入。成功采集某个 handle 后会按 `OJ 名 -> collection state` 写回 `oj_handle_account.collection_states_json`，其中 `lastCollectedAt` 是采集器执行时间，不是提交时间；`historyStartReached` 表示本 OJ 该 handle 已经确认采到历史左端。某个 handle 的分页请求重试耗尽后会记录稳定错误码日志，并在响应的 per-handle 结果中返回失败原因。

AtCoder 题目列表由后台机制维护：`training-data-web` 启动后会在 `ods_atcoder__problem` 为空时自动 bootstrap 拉取一次题目列表，避免后续 submission ODS 已写入但 DWD 缺少题目元数据。它也有独立低频调度 `platform.training-data.atcoder.problem-list-collector`，默认打开，默认 cron 为每周一 `Asia/Shanghai` 03:30。默认配置是 `bootstrap-on-startup=true`、`bootstrap-only-when-empty=true`。当前不提供手动 HTTP 刷新入口。

面向前端批量采集，模块还提供进程内后台采集任务入口：

```text
POST /api/training-data/admin/codeforces/submissions:collect-batch-jobs
GET  /api/training-data/admin/codeforces/submissions/collect-batch-jobs
```

任务请求体同样接受可选 `ojName`，为空使用 dispatcher 默认 OJ，当前默认是 `CODEFORCES`。任务按多个 `studentIdentity` 逐个采集并可在每个成功批次后刷新仓库；相邻身份之间的后台 job 间隔由 `platform.training-data.collector.job-item-interval` 控制，默认 `4s`。进程内 job 编排由 `training-data-common` 负责，OJ 模块提供单个身份采集和 refresh interval SQL，common 提供通用 warehouse refresh handler；当前 Codeforces 和 AtCoder 都已接入批次刷新回调。状态只保存在当前 `training-data-web` 进程内，前端通过列表接口轮询和展开查看，不提供单个 job 查询接口，也不是持久化 pipeline run state，后端重启后不会恢复。

后台定时采集由 `application.yml` 的 `platform.training-data.collector.schedules` 列表驱动，每个调度项包含 `oj-name`、`enabled`、`cron`、`zone` 和 `lookback`。默认提供打开状态的 `daily-recent-submissions`（`CODEFORCES`，每天 12:00）和 `atcoder-daily-recent-submissions`（`ATCODER`，每天 12:15），都采集最近 `lookback=120h` 的滚动小时窗口。定时采集写入 ODS 后，如果本次采集产生 `batchId`，会自动通过 common warehouse refresh handler 刷新对应 OJ 的 DWD/DWM/DWS；没有 batch 的空窗口会跳过刷新。

Codeforces/AtCoder DWD/DWM/DWS 刷新不单独暴露 HTTP 接口。批量采集任务在 `refreshWarehouse=true` 且采集产生 `batchId` 时，会在同一个后台任务中调用 common warehouse refresh app service 作为收尾步骤；该 service 使用 OJ 配置的 manifest 和 interval repository。默认定时自动采集也会在有 batch 时自动执行同一条刷新链路。

公共 OJ handle 绑定逻辑在 `training-data-common`，基础物理表来自 Codeforces 历史迁移，后续通用采集状态列由 common 迁移随 `training-data-web` 启动应用：

```text
oj_handle_account
```

表以 `student_identity` 为主键，`handles_json` 保存 `OJ 名 -> handle` 的 JSON map，OJ 名统一使用大写常量，例如 `CODEFORCES`、`ATCODER`。`need_collect` 是当前后台自动采集资格字段，调度器会再按 schedule 的 `ojName` 取对应 OJ handle。`collection_states_json` 保存 `OJ 名 -> { historyStartReached, lastCollectedAt }`，用于记录每个 OJ handle 是否已经采到历史第一条记录以及采集器上次实际执行采集的时间。

管理员可以创建绑定、迁移绑定的 `studentIdentity`，切换 `needCollect` 是否参与后台自动采集，并按 OJ 名合并新增 handle；这些接口不会删除请求体未出现的已有 OJ handle，也不修改 auth 登录账号。游客只通过 `GET /api/training-data/oj-handles` 一次性查询全量绑定，响应是 `studentIdentity -> account` map；每个 account 的 `handles` 字段再用大写 OJ 名映射到 handle，`collectionStates` 字段再用大写 OJ 名映射到采集状态。

管理员可以按某个 `studentIdentity + ojName` 删除指定 OJ 的训练数据；这个通用删除编排和 HTTP 入口在 `training-data-common`。`ojName` 是必填 query 参数：

```text
DELETE /api/training-data/admin/students/{studentIdentity}/oj-data?ojName=CODEFORCES
```

该接口先从当前绑定的 `handles_json` 里解析目标 OJ handle，再按 handle 删除 ODS、DWD、DWM、DWS 中的行；实现上 ODS 删除由各 OJ 自己的 purge adapter 负责，上层 DWD/DWM/DWS 删除走按 OJ 名选择表的通用仓储。整个训练数据删除在一个事务里执行。没有绑定或目标 OJ 未绑定时返回成功且计数为 0。它不删除 `oj_handle_account` 绑定，也不删除 auth 账号；彻底删除用户需要调用方按当前绑定的 OJ 逐个清理训练数据，然后继续调用 auth-web 的用户删除接口。

公共 DWD/DWM/DWS app 查询和 JDBC 仓储在 `training-data-common`，以请求参数 `ojName` 和平台 `studentIdentity` 作为个人维度入口；app service 会先从 handle map 解析到该 OJ 绑定的 handle，再复用按公共 `handle` 字段建模的仓储和数仓表查询。DWS 只提供按单个 `studentIdentity + ojName` 的区间 rating AC 汇总；后台自动采集资格只用于调度采集，不提供公开自动汇总查询。提交明细 DWD 查询由后端按 `page + limit` 分页，按提交时间和 submission id 倒序返回，并在每次响应中返回精确 `total`、`totalPages` 和 `hasMore`。问题维度查询结果也会按请求 OJ 反查 `studentIdentity`，遇到未绑定 handle 会失败。

这些 DWD/DWM/DWS 读侧能力已经公开为无需鉴权的 guest HTTP 查询接口，沿用 `/api/training-data/codeforces/accepted-summary`、`/api/training-data/codeforces/submissions/by-*` 和 `/api/training-data/codeforces/first-accepted/by-*` 路径，并通过可选 `ojName` 参数透传 OJ 名。

ODS 字段核对、DWD/DWM/DWS 表粒度、SQL 任务资源和采集链路说明见 [docs/ods-submission.md](docs/ods-submission.md)。AtCoder/Kenkoooo 采集设计见 [docs/atcoder-collection.md](docs/atcoder-collection.md)。本地链路测试数据见 [docs/test-data.md](docs/test-data.md)。
