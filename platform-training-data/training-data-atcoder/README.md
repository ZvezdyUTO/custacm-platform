# training-data-atcoder

AtCoder 垂直 OJ jar，负责 Kenkoooo submission/problem/problem-model 访问、payload 解析、ODS 存储、AtCoder 清洗 SQL、元数据调度和测试。

## Directory Layout

```text
src/main/java/com/custacm/platform/trainingdata/atcoder/
  app/     ODS ingest、submission collection 和 problem metadata collection
  config/  source 属性、Spring bean、启动 bootstrap 和低频调度
  domain/  batch、ODS record、parser/writer/source port
  infra/   REST client、Jackson parser、JDBC writer/purge/interval adapter
src/main/resources/
  db/migration/  AtCoder ODS 迁移
  sql/           ODS upsert、DWD/DWM/DWS 清洗和 task manifest
src/test/        parser、collector、JDBC、SQL task 和配置测试
```

## Dependency And Layer Rules

- 依赖 `training-data-common` 契约和 `common-core` SQL task runner。
- 不定义 HTTP controller；公共接口通过 `ojName=ATCODER` dispatch。
- Kenkoooo payload、分页、错误映射、ODS、problem metadata 和清洗 SQL 只存在本模块。
- AtCoder DWD/DWM/DWS 建表可由 common 提供，但字段清洗与刷新 manifest 留在本模块。
- 外部访问测试必须使用 fake/local client，不依赖在线 Kenkoooo。

## File And Path Responsibilities

| File or path | Responsibility |
| --- | --- |
| `pom.xml` | AtCoder jar 依赖和测试依赖。 |
| `app/AtcoderOdsIngestService.java` | 写入 submission/problem/problem-model ODS。 |
| `app/AtcoderSubmissionCollectionAdapter.java` | 将 Kenkoooo `from_second` 分页接入 common collection contract。 |
| `app/AtcoderProblemListCollectionService.java` | 拉取并写入题目及难度模型元数据。 |
| `config/AtcoderCollectorProperties.java` | submission source 的分页、间隔、重试和超时配置。 |
| `config/AtcoderProblemListCollectorProperties.java` | metadata bootstrap 与调度配置。 |
| `config/AtcoderTrainingDataConfig.java` | 注册 source、writer、collector、purge 和 refresh bean。 |
| `config/AtcoderProblemListBootstrapRunner.java` | 必要时在启动后补齐空元数据表。 |
| `config/AtcoderProblemListSchedulingConfig.java` | 低频刷新题目元数据。 |
| `domain/` | AtCoder batch、ODS、parser/writer/source port。 |
| `infra/RestClientAtcoderSourceClient.java` | 调用并校验 Kenkoooo API。 |
| `infra/JacksonAtcoderPayloadParser.java` | 解析 submission/problem/problem-model payload。 |
| `infra/JdbcAtcoderOds*Writer.java` | 幂等写入三类 ODS。 |
| `infra/JdbcAtcoderOdsDataPurgeRepository.java` | 删除指定 handle 的 AtCoder submission ODS。 |
| `infra/JdbcAtcoderWarehouseRefreshIntervalRepository.java` | 按 `fetched_at DESC, id DESC` 选择最新有效 ODS batch，并从指定 batch 推导 warehouse 刷新区间。 |
| `src/main/resources/db/migration/` | AtCoder ODS 和 problem-model 表迁移。 |
| `src/main/resources/sql/` | ODS upsert、DWD/DWM/DWS 幂等刷新与 manifest。 |
| `src/test/` | 生产路径对应的聚焦测试。 |
| `src/test/.../JdbcAtcoderWarehouseRefreshIntervalRepositoryTest.java` | 以 H2 覆盖指定 batch 区间与最新有效 batch 的真实 JDBC 查询。 |

## Source Policy

Kenkoooo 请求间隔由配置控制，默认测试不访问线上服务。Problem metadata 与 submission 采集保持独立。
