# training-data-common

共享的 OJ 训练数据 jar。它提供跨 OJ 的领域契约、应用编排、JDBC 查询、无传输层依赖的查询 facade、采集任务和定时调度，不包含具体 OJ payload、ODS 实现或 HTTP Controller。

## Directory Layout

```text
src/main/java/com/custacm/platform/trainingdata/common/
  app/        handle、查询、清理和 warehouse refresh 用例
  collector/  最近窗口采集、OJ dispatch、进程内 job 和结果模型
  config/     common Spring bean 装配
  domain/oj/  模型、criteria、repository port 和 OJ value
  infra/oj/   handle 与同层 DWD/DWM/DWS JDBC adapter
  scheduler/  定时采集
  support/    小型非业务工具
  web/        Blog API 复用的 collector 请求/响应 DTO
src/main/resources/db/migration/  共享身份与同层仓库迁移
src/test/                         对应单元和 JDBC 测试
```

## Dependency And Layer Rules

- 依赖 `common-core` 的 SQL task runner，以及 Spring JDBC/Context；不依赖 Spring MVC、Codeforces 或 AtCoder 模块。
- OJ 模块通过 common port 注册 source collector、ODS purge 和 warehouse refresh 实现。
- common 可以按受控 `ojName` 查询同结构 DWD/DWM/DWS 表，但不能解析 OJ payload 或写 OJ ODS。
- Blog API 的 `top.naccl` package 独占 Spring MVC HTTP adapter；训练查询通过无 MVC 注解的 `OjWarehouseQueryFacade` 暴露 application report。

## File And Path Responsibilities

| File or path | Responsibility |
| --- | --- |
| `pom.xml` | 共享 jar 及 Spring/JDBC/SQL-task 依赖。 |
| `config/CommonTrainingDataConfig.java` | 注册 common repository、service、dispatcher、job executor 和 SQL task runner。 |
| `app/account/TrainingUserDirectory.java` | 训练模块读取 username/handle 和回写采集状态的内部契约。 |
| `app/account/OjHandleAccountService.java` | Blog API 提供的同进程目录实现及 handle/采集资格领域校验；允许在尚未绑定 OJ handle 时先持久化 `needCollect`，普通更新拒绝覆盖已有 handle，清理完成后才允许专用换绑并重置该 OJ 采集状态。 |
| `app/query/*Service.java` | DWD/DWM/DWS 公开查询编排。 |
| `app/query/OjWarehouseQueryFacade.java` | 统一校验查询参数、分页和日期并返回 application report，不依赖 Spring MVC。 |
| `app/purge/OjStudentDataPurgeService.java` | 按学生和 OJ 删除 ODS/warehouse 数据。 |
| `app/warehouse/OjWarehouseRefreshService.java` | 按显式 batch 严格运行 OJ SQL task manifest，或为人工刷新选择最新有效 ODS batch 后复用严格流程。 |
| `collector/OjSubmissionCollectionService.java` | 按用户和 OJ 读取最近成功窗口结束时间；首次执行使用历史起点，后续从该时间向前倒退配置时长；同时负责互斥执行和结果聚合。 |
| `collector/OjCollectionRequestExecutor.java` | 统一请求间隔和重试。 |
| `collector/dispatch/` | 根据规范化 `ojName` 选择 collector。 |
| `collector/job/` | 进程内批量采集状态与 warehouse refresh dispatch。 |
| `domain/oj/repo/OjWarehouseRefreshIntervalRepository.java` | 提供最新有效 ODS batch 选择与指定 batch 刷新区间查询契约。 |
| `domain/oj/` | OJ-generic 模型、查询条件、仓储接口和名称/难度策略。 |
| `infra/oj/repo/` | handle、查询和 warehouse purge 的 JDBC 实现。 |
| `scheduler/` | 从配置读取 OJ schedule 并调用相同采集用例。 |
| `web/collector/request`、`web/collector/response` | Blog API 管理接口复用的采集请求与响应 DTO。 |
| `src/main/resources/db/migration/V018__rename_oj_handle_account_and_store_handles_map.sql` | 将 Codeforces 单 OJ handle 表提升为共享 `oj_handle_account`；版本与内容保持历史兼容。 |
| `src/main/resources/db/migration/` | 共享身份与符合公共查询契约的同层表迁移。 |
| `src/test/` | app/domain/collector/JDBC/scheduler/query facade 的聚焦测试。 |
| `src/test/.../OjWarehouseRefreshServiceTest.java` | 覆盖显式 batch 严格刷新、最新 batch 刷新与无有效 batch 拒绝。 |

## Invariants

- `username` 是唯一训练业务身份；OJ 名由 `OjNames` 规范化。
- 采集窗口使用 `[startInclusive, endExclusive)`。
- 每个用户、每个 OJ 独立保存 `lastCollectedAt`；只有成功采集才将其推进到本次固定的 `endExclusive`，窗口内没有提交也视为成功。
- `lastCollectedAt` 为空时采集全部历史；否则起点为 `lastCollectedAt - lookback`。不保存“历史是否完整”的独立标记。
- job 状态只存在当前 JVM，重启不恢复。
- OJ-specific source、payload、ODS、清洗 SQL 和 manifest 留在 OJ 模块。
