# training-data-codeforces

Codeforces 垂直 OJ jar，负责 `user.status` 访问、payload 解析、ODS 存储、Codeforces 清洗 SQL、purge/refresh adapter 和测试。

## Directory Layout

```text
src/main/java/com/custacm/platform/trainingdata/codeforces/
  app/     ODS ingest 与 common collection adapter
  config/  source 配置和 Spring bean
  domain/  batch、ODS record、parser/writer/source port
  infra/   REST client、Jackson parser、JDBC writer/purge/interval adapter
src/main/resources/
  db/migration/  Codeforces ODS 与历史演进迁移
  fixtures/      本地 API fixture
  sql/           ODS upsert、DWD/DWM/DWS 清洗和 task manifest
src/test/        parser、collector、JDBC、SQL task 和配置测试
```

## Dependency And Layer Rules

- 依赖 `training-data-common` 契约和 `common-core` SQL task runner。
- 不定义 HTTP controller；公共接口通过 `ojName=CODEFORCES` dispatch。
- Codeforces payload、分页、错误映射、ODS 和清洗 SQL 只存在本模块。
- 外部 API 测试必须使用 fixture 或 fake/local server。

## File And Path Responsibilities

| File or path | Responsibility |
| --- | --- |
| `pom.xml` | Codeforces jar 依赖和测试依赖。 |
| `app/CodeforcesOdsSubmissionIngestService.java` | 解析并批量写入 ODS。 |
| `app/CodeforcesSubmissionCollectionAdapter.java` | 把 `user.status` 分页接入 common collection contract。 |
| `config/CodeforcesCollectorProperties.java` | API、分页、间隔、重试和超时配置。 |
| `config/CodeforcesTrainingDataConfig.java` | 注册 source、writer、collector、purge 和 refresh bean。 |
| `domain/` | Codeforces batch、ODS、parser/writer/source port。 |
| `infra/RestClientCodeforcesSubmissionSourceClient.java` | 调用并校验 Codeforces API。 |
| `infra/JacksonSubmissionPayloadParser.java` | 将原始 JSON 转成 ODS record。 |
| `infra/JdbcCodeforcesOdsSubmissionWriter.java` | 执行幂等 ODS upsert。 |
| `infra/JdbcCodeforcesOdsDataPurgeRepository.java` | 删除指定 handle 的 Codeforces ODS。 |
| `infra/JdbcCodeforcesWarehouseRefreshIntervalRepository.java` | 按 `fetched_at DESC, id DESC` 选择最新可计算时间的 ODS batch，并从指定 batch 推导 warehouse 刷新区间。 |
| `src/main/resources/db/migration/` | Codeforces ODS、索引、handle 历史和仓库迁移。 |
| `src/main/resources/sql/` | ODS upsert、DWD/DWM/DWS 幂等刷新与 manifest。 |
| `src/main/resources/fixtures/codeforces/` | 可重复的本地源数据及来源说明。 |
| `src/test/` | 生产路径对应的聚焦测试。 |
| `src/test/.../CodeforcesWarehouseSqlTaskTest.java` | 以 H2 覆盖 warehouse SQL 与最新有效 batch 的真实 JDBC 查询。 |

## Data Grain

团队提交只有在被采集 handle 出现在 `author.members` 时才归属于该 handle；ODS 和 DWD 使用 `submission + handle` 粒度。
