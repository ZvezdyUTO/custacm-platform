# training-data-common

共享 OJ 训练数据 jar，提供 username/OJ 绑定领域契约、无 MVC 查询 facade、批量查询、采集任务、调度、数仓刷新和数据清理。不包含具体 OJ payload、ODS writer、HTTP Controller 或运行入口。

## 目录结构

```text
src/main/java/com/custacm/platform/trainingdata/common/
  app/        账号绑定、查询、清理和 warehouse refresh 用例
  collector/  采集窗口、dispatch、进程内 job 和结果模型
  config/     common Spring bean 装配
  domain/oj/  模型、criteria、repository port 和 OJ value
  infra/oj/   规范化 handle、DWD/DWM/DWS JDBC adapter
  scheduler/  可配置的定时采集
  support/    小型非业务工具
  web/        Blog API 复用的采集请求/响应 DTO
src/main/resources/db/migration/  共享身份与 warehouse 迁移
src/test/                         app/domain/collector/JDBC/scheduler 测试
```

## 依赖与分层规则

- 依赖 `common-core` SQL task runner 以及 Spring JDBC/Context；不依赖 Spring MVC、Blog API、Codeforces 或 AtCoder 模块。
- OJ 模块通过 common port 注册 source collector、ODS purge 和 warehouse refresh 实现。
- common 可以按受控 `ojName` 查询同结构 DWD/DWM/DWS 表，但不能解析 OJ payload 或写 OJ ODS。
- Blog API 的 `top.naccl` package 独占 HTTP adapter；查询只通过 `OjWarehouseQueryFacade` 暴露 transport-neutral report。
- HTTP DTO 仅限采集任务 contract；不能在本模块新增 Controller 或安全逻辑。

## 规范化身份存储

`V034__normalize_oj_handle_accounts.sql` 从旧 `oj_handle_account` JSON 列校验并展开到：

- `training_member`：username、`need_collect` 与时间戳；外键指向 Blog `user.username`，改名/删除级联。
- `oj_handle_binding`：username、`oj_name`、handle、`last_collected_at` 与时间戳；主键为 `(username, oj_name)`，并约束 `(oj_name, handle)` 唯一。

生产 repository 只读写新表。旧表在本版本保留一个迁移窗口且不再写入；后续删除必须使用新 migration，不能修改 V034。每个绑定只保存一个成功窗口结束游标，不再保存 JSON map 或“全量历史完成”标志。

## 核心规则

- `username` 是唯一训练业务身份；OJ 名由 `OjNames` 规范化。
- 管理员更新传入完整 handle 集合。Blog API 先清理每个被移除或改变 OJ 的数据，再调用 `replaceHandlesAfterPurge` 精确替换；未改变 handle 保留原 `lastCollectedAt`，改变或新增 handle 的游标为空。
- `lastCollectedAt` 为空时采集全部历史；否则使用 `[lastCollectedAt - lookback, endExclusive)`。`lookback=0` 表示直接从上次成功游标继续且不主动制造重叠窗口；只有成功完成才推进到本次固定 `endExclusive`。
- 多人 AC 汇总先解析所有可查询成员/handle，再调用 repository 批量方法；JDBC 使用一个 `handle in (...)` 查询，不能循环单人查询。
- job 状态只存在当前 JVM，重启不恢复。
- 所有 schedule 默认关闭；缺省或空配置不得隐式启动外部请求。
- OJ-specific source、payload、ODS、清洗 SQL 和 manifest 留在对应 OJ 模块。

## 文件与路径职责

| 文件/路径 | 职责 |
| --- | --- |
| `pom.xml` | common jar、Spring JDBC/Context、Flyway 与 SQL task runner 依赖 |
| `config/CommonTrainingDataConfig.java` | 注册 repository、service、query facade、dispatcher、job 和 SQL runner |
| `app/account/TrainingUserDirectory.java` | 采集和查询读取 username/handle、按 handle 推进游标的内部契约 |
| `app/account/OjHandleAccountService.java` | 规范化绑定校验、批量列表、完整集合替换和单行游标推进 |
| `domain/oj/repo/OjHandleAccountRepository.java` | 规范化成员/绑定 repository port |
| `infra/oj/repo/account/JdbcOjHandleAccountRepository.java` | `training_member`/`oj_handle_binding` JDBC 读写与聚合 |
| `app/query/OjWarehouseQueryFacade.java` | 校验 OJ、日期、分页和难度并返回无 MVC application report |
| `app/query/OjAcceptedSummaryQueryService.java` | 单人及多人 AC 汇总编排 |
| `domain/oj/repo/OjAcceptedSummaryRepository.java` | 单人和强制批量 AC 汇总查询契约 |
| `infra/oj/repo/query/JdbcOjAcceptedSummaryRepository.java` | 单人/多人共用的 handle 集合 SQL 查询 |
| `app/query/OjSubmissionQueryService.java` | 按用户或题目查询提交 |
| `app/query/OjFirstAcceptedProblemQueryService.java` | 按用户或题目查询首 AC |
| `app/purge/OjStudentDataPurgeService.java` | 按 username/OJ dispatch ODS 与 warehouse 清理 |
| `app/warehouse/OjWarehouseRefreshService.java` | 按显式 batch 或最新有效 batch 执行 OJ manifest |
| `collector/OjSubmissionCollectionService.java` | 计算采集窗口、互斥执行、写入结果和成功游标推进 |
| `collector/OjCollectionRequestExecutor.java` | 外部请求间隔与重试 |
| `collector/job/` | 进程内采集任务状态与 warehouse refresh dispatch |
| `scheduler/OjCollectorSchedulingConfig.java` | 只注册显式启用的采集 schedule |
| `collector/config/OjCollectorSchedulingProperties.java` | 调度配置模型；缺省 `enabled=false`，允许自动调度使用零回看窗口 |
| `web/collector/request/`、`web/collector/response/` | Blog API 管理接口复用的采集 DTO |
| `src/main/resources/db/migration/V018__rename_oj_handle_account_and_store_handles_map.sql` | 旧 JSON 身份表的历史迁移，内容保持不变 |
| `src/main/resources/db/migration/V034__normalize_oj_handle_accounts.sql` | 校验并展开旧数据，创建规范化成员/绑定表 |
| `src/test/.../JdbcOjHandleAccountRepositoryTest.java` | 规范化表 JDBC、精确替换和单行游标测试 |
| `src/test/.../JdbcOjAcceptedSummaryRepositoryTest.java` | 单个 handle 集合 SQL 批量汇总测试 |
| `src/test/.../OjAcceptedSummaryQueryServiceTest.java` | 单人/多人查询编排与缺失数据语义测试 |
| `src/test/.../OjCollectorSchedulingConfigTest.java` | 默认关闭和显式启用调度测试 |

## 验证

从仓库根目录运行：

```bash
mvn clean test
```
