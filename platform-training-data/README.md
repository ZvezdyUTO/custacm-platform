# platform-training-data

训练数据模块第一版实现 Codeforces 独立 OJ 数仓建模和批量写入入口。

当前范围：

- `training-data-codeforces`：Codeforces 入口、ingest app service、批次上下文、ODS record/parser/writer、ODS/DWD/DWM/DWS DDL、幂等 SQL 任务、fixture/tests。
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
POST /api/training-data/ods/codeforces/submissions:batch-upsert
```

这个写入接口需要 Keycloak JWT 中带平台 `admin` 角色。启动 `training-data-web` 时，Flyway 会从 OJ 模块的 `classpath:db/migration` 下应用 ODS/DWD/DWM/DWS 建表脚本。

ODS 字段核对、DWD/DWM/DWS 表粒度、SQL 任务资源和写入接口说明见 [docs/ods-submission.md](docs/ods-submission.md)。本地链路测试数据见 [docs/test-data.md](docs/test-data.md)。
