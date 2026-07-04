# platform-training-data

训练数据模块第一版实现 Codeforces 独立 OJ 数仓建模和批量写入入口。

当前范围：

- `training-data-codeforces`：Codeforces 入口、ingest app service、批次上下文、ODS record/parser/writer、ODS/DWD/DWM/DWS DDL、幂等 SQL 任务、DWD/DWM/DWS 查询 repository/service、fixture/tests。
- `training-data-web`：Spring Boot 入口、模块健康检查、模块信息、文件日志、Flyway 迁移和 OJ-specific HTTP 写入入口。

当前真正落地 Codeforces 垂直数仓链路：

```text
ods_codeforces__submission
 -> dwd_codeforces__submission
 -> dwm_codeforces__handle_problem_first_accepted
 -> dws_codeforces__handle_daily_rating_accepted_summary
```

每个 OJ 是独立数仓域，入口和数据组织也在各自 Maven 模块里；ADS、Java SQL-task 执行器、调度器、DAG 和 HTTP refresh 入口还没有实现。

当前 Codeforces 已有内部 Java 查询能力，可读取 DWD 提交明细、DWM 首次 AC 题目事实和 DWS 每日宽表 rating 汇总。这个能力目前还没有公开 HTTP API。

Codeforces Java 代码按 OJ 垂直边界内部分层：

```text
training-data-codeforces/src/main/java/com/custacm/platform/trainingdata/codeforces/
  app/      # 应用用例层：编排 ingest 和查询，不写 SQL 细节
  config/   # Spring Bean 装配
  domain/   # 领域模型、值对象、查询条件和 repository 契约
  infra/    # 基础设施实现：解析器、JDBC repository/writer
  web/      # HTTP controller 和响应对象
```

训练数据仓库的 `datetime` 和日期粒度统一使用 UTC+8（`Asia/Shanghai`）语义。Codeforces 的 epoch 秒会在 DWD 派生时转换成 UTC+8 本地时间，后续 DWM/DWS 和 Java 查询边界沿用同一日界线。

外置采集器可通过 OJ-specific HTTP 接口写入原始 submission 数组：

```text
POST /api/training-data/ods/codeforces/submissions:batch-upsert
```

这个写入接口需要 Keycloak JWT 中带平台 `admin` 角色。启动 `training-data-web` 时，Flyway 会从 OJ 模块的 `classpath:db/migration` 下应用 ODS/DWD/DWM/DWS 建表脚本。

ODS 字段核对、DWD/DWM/DWS 表粒度、SQL 任务资源和写入接口说明见 [docs/ods-submission.md](docs/ods-submission.md)。本地链路测试数据见 [docs/test-data.md](docs/test-data.md)。
Codeforces Java 目录结构和文件级职责清单见 [training-data-codeforces/README.md](training-data-codeforces/README.md)。
