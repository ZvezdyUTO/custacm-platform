# training-data-atcoder

AtCoder 垂直 OJ jar，负责 Kenkoooo submission/problem/problem-model 访问、payload 解析、ODS 存储、AtCoder 清洗 SQL、purge/refresh adapter、可选元数据调度和测试。

## 目录结构

```text
src/main/java/com/custacm/platform/trainingdata/atcoder/
  app/     ODS ingest、submission collection 和 problem metadata collection
  config/  source 属性、Spring bean、可选 bootstrap 和调度
  domain/  batch、ODS record、parser/writer/source port
  infra/   REST client、Jackson parser、JDBC writer/purge/interval adapter
src/main/resources/
  db/migration/  AtCoder ODS 与 problem-model 迁移
  sql/           ODS upsert、DWD/DWM/DWS 清洗和 task manifest
src/test/        parser、collector、JDBC、SQL task 和配置测试
```

## 依赖与分层规则

- 依赖 `training-data-common` 契约和 `common-core` SQL task runner。
- 不定义 HTTP Controller；Blog API 通过 common contract 和 `ojName=ATCODER` dispatch。
- Kenkoooo payload、分页、错误映射、ODS、problem metadata 和清洗 SQL 只存在本模块。
- AtCoder DWD/DWM/DWS 建表可由 common 提供，字段清洗与 refresh manifest 留在本模块。
- 外部访问测试使用 fake/local client，不访问线上 Kenkoooo。

## 默认运行策略

- Submission 自动采集由 common schedule 驱动，Blog API 中对应 schedule 默认 `enabled=false`；管理员手动任务不受影响。
- Problem metadata 的 `enabled` 与 `bootstrapOnStartup` 默认都为 `false`。缺少配置时不会发起外部请求。
- `bootstrapOnlyWhenEmpty` 默认 `true`；只有显式开启 bootstrap 且题目/难度表为空时才启动补齐。
- Metadata schedule 与 submission schedule 相互独立，显式启用后仍复用同一个 `AtcoderProblemListCollectionService`。

## 文件与路径职责

| 文件/路径 | 职责 |
| --- | --- |
| `pom.xml` | AtCoder jar、common、HTTP/Jackson/JDBC 和测试依赖 |
| `app/AtcoderOdsIngestService.java` | 写入 submission/problem/problem-model ODS |
| `app/AtcoderSubmissionCollectionAdapter.java` | 将 Kenkoooo `from_second` 分页接入 common 采集合同 |
| `app/AtcoderProblemListCollectionService.java` | 拉取并写入题目与难度模型元数据 |
| `config/AtcoderCollectorProperties.java` | Submission source 的 API、分页、间隔、重试和超时配置 |
| `config/AtcoderProblemListCollectorProperties.java` | 默认关闭的 metadata bootstrap/调度配置 |
| `config/AtcoderTrainingDataConfig.java` | 注册 source、writer、collector、purge 和 refresh bean |
| `config/AtcoderProblemListBootstrapRunner.java` | 仅在显式开启时执行启动补齐；默认直接跳过 |
| `config/AtcoderProblemListSchedulingConfig.java` | 仅在 `enabled=true` 时注册低频 metadata schedule |
| `domain/` | AtCoder batch、ODS、parser/writer/source port |
| `infra/RestClientAtcoderSourceClient.java` | 调用并校验 Kenkoooo API |
| `infra/JacksonAtcoderPayloadParser.java` | 解析 submission/problem/problem-model payload |
| `infra/JdbcAtcoderOds*Writer.java` | 幂等写入三类 ODS |
| `infra/JdbcAtcoderOdsDataPurgeRepository.java` | 删除指定 handle 的 AtCoder submission ODS |
| `infra/JdbcAtcoderWarehouseRefreshIntervalRepository.java` | 选择最新有效 batch，并从指定 batch 推导刷新区间 |
| `src/main/resources/db/migration/` | AtCoder ODS 和 problem-model 表迁移 |
| `src/main/resources/sql/` | ODS upsert、DWD/DWM/DWS 幂等刷新与 manifest |
| `src/test/.../AtcoderCollectorPropertiesTest.java` | Source 和 metadata 默认关闭配置测试 |
| `src/test/.../JdbcAtcoderWarehouseRefreshIntervalRepositoryTest.java` | 指定/最新 batch 区间 JDBC 测试 |

## 验证

从仓库根目录运行：

```bash
mvn clean test
```
