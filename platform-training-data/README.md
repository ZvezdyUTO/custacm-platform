# platform-training-data

`platform-training-data` 是 Blog API 进程内使用的训练数据 library subsystem，没有 Spring Boot 入口、独立 HTTP 服务、认证或账号管理。

## 模块职责

| 模块 | 职责 |
| --- | --- |
| `training-data-common` | username/OJ 绑定契约、批量/单人查询 facade、采集任务、调度、数仓刷新、清理和 JDBC repository |
| `training-data-codeforces` | Codeforces `user.status` 采集、ODS、迁移、清洗 SQL 和 source/purge/refresh adapter |
| `training-data-atcoder` | Kenkoooo submission/problem 元数据采集、ODS、迁移、清洗 SQL 和 source/purge/refresh adapter |

业务身份统一使用 `username`。Blog API 拥有用户、密码、角色、OJ handle 管理和全部 Spring MVC Controller；训练模块只暴露无传输层依赖的 application contract。

## 身份与采集状态

生产代码只读写 V034 引入的规范化结构：

- `training_member`：每个 username 一行，保存 `need_collect`；
- `oj_handle_binding`：每个 username、每个 OJ 一行，保存 handle 和 `last_collected_at`。

V034 从旧 `oj_handle_account` JSON 展开数据并建立外键/唯一约束。旧表保留一个迁移窗口且不再写入，后续确认稳定后再由新迁移删除；不得改写已发布 V034。

管理员编辑用户时，Blog API 先按 OJ 清理被移除或更改 handle 的 ODS/DWD/DWM/DWS，再调用 common 服务精确替换完整 handle 集合。普通训练代码不能绕过该编排直接覆盖已有绑定。

`lastCollectedAt` 是唯一持久化采集游标：缺失时抓取全量历史，后续从 `lastCollectedAt - lookback` 开始并采集到本次固定上界；`lookback=0` 表示从上次成功游标直接续爬，只有成功完成才推进游标。

## 查询与调度

- 单人 AC 汇总、提交和首 AC 查询通过无 MVC 的 `OjWarehouseQueryFacade` 暴露。
- 多人 AC 汇总一次解析成员与 handle，并用一个 handle 集合 SQL 批量读取，供 Blog API 的 `/player/training-data/accepted-summaries` 使用。
- DWD/DWM/DWS 刷新继续使用各 OJ 的幂等 SQL manifest。
- Codeforces/AtCoder 提交采集 schedule 与 AtCoder 题目元数据 bootstrap/调度默认关闭。显式启用后，每日任务从上次成功游标向前回看 100 小时，日内任务使用零回看窗口直接续爬；手动任务仍使用管理员提交的正数回看小时数。
- 采集 job 状态只存在当前 JVM，重启不恢复。

## 目录结构

```text
platform-training-data/
  pom.xml
  training-data-common/
  training-data-codeforces/
  training-data-atcoder/
```

## 依赖与分层规则

- common 定义跨 OJ port、应用编排和同层 warehouse 查询；OJ 模块实现 source、payload、ODS、purge 和 refresh adapter。
- OJ payload、外部 API 和 ODS 表不能进入 common；公共业务实体不能放到 `platform-common`。
- 本模块不定义 JWT、安全过滤器、用户 HTTP、公开 Controller 或独立 DataSource/Flyway runtime。
- 外部源测试使用 fixture、fake 或本地 server，不依赖线上服务。

## 文件与路径职责

| 文件/路径 | 职责 |
| --- | --- |
| `pom.xml` | Training 多模块 Maven parent |
| `training-data-common/README.md` | common 领域、应用、JDBC 和迁移说明 |
| `training-data-codeforces/README.md` | Codeforces source/ODS/warehouse 边界 |
| `training-data-atcoder/README.md` | AtCoder source/ODS/metadata 边界 |
| `training-data-common/src/main/resources/db/migration/V034__normalize_oj_handle_accounts.sql` | 将旧 JSON 账号展开为规范化成员和绑定表 |
| `training-data-common/src/main/java/.../app/query/OjWarehouseQueryFacade.java` | Blog API 复用的无 MVC 查询入口 |
| `training-data-common/src/main/java/.../app/account/OjHandleAccountService.java` | 规范化绑定领域校验和清理后的精确替换 |
| `training-data-common/src/main/java/.../collector/job/` | 进程内采集任务状态与 refresh dispatch |
| `training-data-codeforces/src/main/resources/sql/` | Codeforces ODS upsert 与 warehouse manifest |
| `training-data-atcoder/src/main/resources/sql/` | AtCoder ODS upsert 与 warehouse manifest |

## 验证

从仓库根目录运行：

```bash
mvn clean test
```
