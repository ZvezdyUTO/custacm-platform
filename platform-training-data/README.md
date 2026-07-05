# platform-training-data

训练数据模块第一版实现 Codeforces 独立 OJ 数仓建模和批量写入入口。

当前范围：

- `training-data-codeforces`：Codeforces 入口、ingest app service、批次上下文、ODS record/parser/writer、CF handle-account 维护、ODS/DWD/DWM/DWS DDL、幂等 SQL 任务、fixture/tests。
- `training-data-web`：Spring Boot 入口、模块健康检查、模块信息、文件日志、Flyway 迁移和 OJ-specific HTTP 写入入口。

当前真正落地 Codeforces 垂直数仓链路：

```text
ods_codeforces__submission
 -> dwd_codeforces__submission
 -> dwm_codeforces__handle_problem_first_accepted
 -> dws_codeforces__handle_daily_rating_accepted_summary
```

每个 OJ 是独立数仓域，入口和数据组织也在各自 Maven 模块里；ADS、Java SQL-task 执行器、调度器、DAG 和 HTTP refresh 入口还没有实现。

外置采集器可通过 OJ-specific HTTP 接口写入原始 submission 数组：

```text
POST /api/training-data/admin/ods/codeforces/submissions:batch-upsert
```

这个写入接口属于平台 admin URL tier，需要平台 JWT 中带 `admin` 角色。启动 `training-data-web` 时，Flyway 会从 OJ 模块的 `classpath:db/migration` 下应用 ODS/DWD/DWM/DWS 和 CF handle-account 建表脚本。

Codeforces 模块还维护 `studentIdentity` 到 Codeforces handle 的绑定表：

```text
codeforces_handle_account
```

管理员可以创建绑定和迁移绑定的 `studentIdentity`，但该接口不修改 handle，也不修改 auth 登录账号。游客可以按 `studentIdentity` 查询已绑定的 Codeforces handle。

Codeforces 内部 DWD/DWM/DWS app 查询以平台 `studentIdentity` 作为个人维度入口；app service 会先解析到绑定的 Codeforces handle，再复用按 `author_handle` 建模的仓储和数仓表查询。问题维度查询结果也会反查 `studentIdentity`，遇到未绑定 handle 会失败。

这些 DWD/DWM/DWS 读侧能力已经公开为无需鉴权的 guest HTTP 查询接口，路径位于 `/api/training-data/codeforces/accepted-summary`、`/api/training-data/codeforces/submissions/by-*` 和 `/api/training-data/codeforces/first-accepted/by-*`。

ODS 字段核对、DWD/DWM/DWS 表粒度、SQL 任务资源和写入接口说明见 [docs/ods-submission.md](docs/ods-submission.md)。本地链路测试数据见 [docs/test-data.md](docs/test-data.md)。
