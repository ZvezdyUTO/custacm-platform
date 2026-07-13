# training-data-codeforces

Codeforces 垂直 OJ jar，负责 `user.status` 访问、payload 解析、ODS 存储、Codeforces 清洗 SQL、purge/refresh adapter 和测试。

## 目录结构

```text
src/main/java/com/custacm/platform/trainingdata/codeforces/
  app/     ODS ingest 与 common collection adapter
  config/  source 属性和 Spring bean
  domain/  batch、ODS record、parser/writer/source port
  infra/   REST client、Jackson parser、JDBC writer/purge/interval adapter
src/main/resources/
  db/migration/  Codeforces 专属 ODS 与历史演进迁移
  fixtures/      本地 API fixture
  sql/           ODS upsert、DWD/DWM/DWS 清洗和 task manifest
src/test/        parser、collector、JDBC、SQL task 和配置测试
```

## 依赖与分层规则

- 依赖 `training-data-common` 契约和 `common-core` SQL task runner。
- 不定义 HTTP Controller；Blog API 通过 common contract 和 `ojName=CODEFORCES` dispatch。
- Codeforces payload、分页、错误映射、ODS 和清洗 SQL 只存在本模块。
- username、handle 归属与成功采集游标由 common 的规范化 `training_member`/`oj_handle_binding` repository 提供，本模块不直接读旧 JSON 身份表。
- 外部 API 测试使用 fixture、fake 或 local server，不访问线上 Codeforces。

## 默认运行策略

Codeforces submission 自动采集由 common schedule 驱动，Blog API 的 daily/intraday schedule 默认 `enabled=false`。缺少显式配置时不会访问外部 Codeforces；管理员手动创建采集任务仍可调用同一 collection adapter。

采集窗口首次覆盖全量历史，后续从 common 保存的 `lastCollectedAt - lookback` 开始，并由 adapter 在本次固定上界内分页读取 `user.status`。

## 文件与路径职责

| 文件/路径 | 职责 |
| --- | --- |
| `pom.xml` | Codeforces jar、common、HTTP/Jackson/JDBC 和测试依赖 |
| `app/CodeforcesOdsSubmissionIngestService.java` | 解析并批量写入 submission ODS |
| `app/CodeforcesSubmissionCollectionAdapter.java` | 将 `user.status` 分页接入 common collection contract |
| `config/CodeforcesCollectorProperties.java` | API、分页、间隔、重试和超时配置 |
| `config/CodeforcesTrainingDataConfig.java` | 注册 source、writer、collector、purge 和 refresh bean |
| `domain/` | Codeforces batch、ODS、parser/writer/source port |
| `infra/RestClientCodeforcesSubmissionSourceClient.java` | 调用并校验 Codeforces API |
| `infra/JacksonSubmissionPayloadParser.java` | 将原始 JSON 转成 ODS record |
| `infra/JdbcCodeforcesOdsSubmissionWriter.java` | 执行幂等 ODS upsert |
| `infra/JdbcCodeforcesOdsDataPurgeRepository.java` | 删除指定 handle 的 Codeforces ODS |
| `infra/JdbcCodeforcesWarehouseRefreshIntervalRepository.java` | 选择最新有效 ODS batch，并从指定 batch 推导刷新区间 |
| `src/main/resources/db/migration/` | Codeforces ODS、索引、单 OJ handle 前置历史与 warehouse 迁移；共享 V018/V034 归 common |
| `src/main/resources/sql/` | ODS upsert、DWD/DWM/DWS 幂等刷新与 manifest |
| `src/main/resources/fixtures/codeforces/` | 可重复本地 source payload 和来源说明 |
| `src/test/.../CodeforcesSubmissionCollectionServiceTest.java` | 采集窗口、分页、错误和写入行为测试 |
| `src/test/.../CodeforcesWarehouseSqlTaskTest.java` | Warehouse SQL 与最新有效 batch 的 JDBC 测试 |

## 数据粒度

团队提交只有在被采集 handle 出现在 `author.members` 时才归属于该 handle；ODS 和 DWD 使用 `submission + handle` 粒度。

## 验证

从仓库根目录运行：

```bash
mvn clean test
```
