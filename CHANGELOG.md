# 更新日志

给人类看的项目更新记录。每次 MR 合并前，由 agent 按 [docs/agent/changelog.md](docs/agent/changelog.md) 的格式在最上方追加本次交付成果。

## 未发布

### 2026-07-08 - 合并 OJ 数仓刷新公共逻辑

- 成果：将 Codeforces/AtCoder 重复的数仓刷新 interval、SQL task refresh service 和 collection-job refresh handler 收敛到 `training-data-common`，各 OJ 仅保留自己的 interval SQL、manifest 和清洗 SQL 资源。
- 影响：刷新入口和 HTTP 行为不变；新增 OJ 后复用 common refresh service/handler，只需实现 OJ-specific interval repository 并配置 manifest。
- 验证：已运行 `mvn -pl platform-training-data/training-data-common -am test`、`mvn -pl platform-training-data/training-data-codeforces -am test`、`mvn -pl platform-training-data/training-data-atcoder -am test`、`mvn clean verify` 和 `./scripts/check-test-policy.sh`；`training-data-common` 未带 `-am` 的单模块命令会因本地 reactor 依赖未参与解析失败。

### 2026-07-08 - OJ 采集 adapter 失败处理去重

- 成果：新增通用 OJ submission collection adapter 基类，统一 handle 校验、失败 outcome、稳定错误码日志、handle 哈希和 collector batch id 前缀，Codeforces 与 AtCoder adapter 只保留各自分页、过滤和 ODS 写入逻辑。
- 影响：仅内部维护；两个 OJ 的采集结果、批次前缀和源端错误码保持兼容，后续新增 OJ adapter 可以复用同一失败处理框架。
- 验证：已运行 `mvn -pl :training-data-common,:training-data-codeforces,:training-data-atcoder -am test`、`mvn clean verify`、`./scripts/check-test-policy.sh` 和 `./scripts/check-doc-sync.sh origin/main WORKTREE`。

### 2026-07-08 - Codeforces 团队提交按采集 handle 计数

- 成果：Codeforces 采集写入按每个 handle 的采集结果归因，团队提交会归因给本次采集的目标 handle，并新增迁移把 ODS/DWD 唯一键调整为 `submission + handle`。
- 影响：后续 DWM/DWS 统计会把团队提交计入被采集的学生；重新采集历史数据后，已有团队提交会补齐“目标 handle 不是第一成员”的 ODS/DWD/DWM/DWS 数据。
- 验证：已运行 `mvn -pl platform-training-data/training-data-codeforces -am test`、`mvn -pl platform-training-data/training-data-web -am test`、`mvn clean verify` 和 `./scripts/check-test-policy.sh`。

### 2026-07-08 - 训练数据多 OJ 数仓重构

- 成果：将训练数据模块扩展为 Codeforces/AtCoder 多 OJ 数仓结构，新增 OJ handle map、AtCoder Kenkoooo 采集、AtCoder 题目列表刷新、通用 OJ 查询/采集/清理和前端 OJ 切换能力。
- 影响：本次升级会把 Codeforces DWD/DWM/DWS 迁移到新的公共同层表契约，并以破坏性方式删除重建旧仓库表；部署后查询层数据需要从 ODS/后续采集刷新重新生成，在重建完成前公开训练数据查询可能为空。Codeforces ODS、OJ handle 绑定和 auth 账号不由该迁移删除。该重建是本次 `V017` 升级的一次性影响，当前其它迁移不要求清空或重建数仓；后续升级默认保留数仓数据，除非迁移脚本、模块文档和 changelog 再次明确声明破坏性重建。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、`pnpm lint`、`pnpm test`、`pnpm typecheck` 和 `pnpm build`。

### 2026-07-06 - 训练数据前后端工作台与部署联调

- 成果：补齐 Codeforces 训练数据的自动采集标记、后台采集任务、分页查询、用户数据清理和公开用户列表等后端支撑，并新增 React/Vite 前端工作台用于训练查询、用户管理、数据采集、数据维护和操作记录。
- 影响：本地与单机部署现在可以同时启动 auth、training-data 和前端 Nginx；前端通过同源代理访问真实 API，页面路径保留查询/管理员页签状态，管理员可在 UI 中批量创建用户、绑定 Codeforces handle、触发采集任务和执行高风险数据清理。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、`pnpm lint`、`pnpm test`、`pnpm typecheck`、`pnpm build`、`mvn clean package -DskipTests`、`docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config`、`./scripts/check-doc-sync.sh origin/main WORKTREE` 和 `git diff --check`。

### 2026-07-05 - Codeforces 最近提交采集与数仓刷新

- 成果：新增 Codeforces `studentIdentity` 绑定采集链路、可配置最近窗口采集器、DWD/DWM/DWS SQL task DAG 刷新入口、禁用默认定时任务和对应公开查询能力。
- 影响：`training-data-web` 可以通过 admin API 从真实 Codeforces `user.status` 采集最近提交并写入 ODS，再按 batch 刷新 Codeforces 数仓；游客查询继续按 `studentIdentity` 或 `problemKey` 读取清洗后的 DWD/DWM/DWS 数据。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`，并在 Docker E2E 容器中采集 `tourist` 与 `jiangly` 最近 1488 小时数据、刷新数仓、验证公开查询和负向鉴权/refresh 错误路径。

### 2026-07-05 - 平台自有账号鉴权

- 成果：将鉴权模块从 Keycloak 适配改为平台自有账号、BCrypt 密码哈希、RSA JWT 签发、玩家自助和管理员用户管理接口。
- 影响：后端接口按 `/admin/**`、`/player/**` 和游客公开路径分层；登录失败后同一 `studentIdentity` 有 5 秒重试冷却，部署需要配置 auth MySQL 与 RSA JWT 密钥文件。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、`./scripts/check-doc-sync.sh origin/main WORKTREE`、`docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config` 和 `git diff --check`。

### 2026-07-05 - 调整 PR 审核确认规则

- 成果：明确非项目负责人发起的 PR/MR 必须经过负责人确认后合并，项目负责人本人发起的 PR/MR 在明确要求合并时无需额外审核确认。
- 影响：agent 操作规则、贡献指南、PR 模板和文档同步说明保持一致，后续合并判断可以按发起人区分是否需要额外确认。
- 验证：已运行 `./scripts/check-doc-sync.sh origin/main WORKTREE` 和 `git diff --check`。

### 2026-07-05 - Codeforces 数仓读侧与 UTC+8 汇总

- 成果：重整 `training-data-codeforces` 分层包结构，新增 DWD 提交、DWM 首次 AC、DWS 每日 rating 汇总的内部查询服务、仓储实现、查询条件和覆盖测试。
- 影响：Codeforces 数仓时间字段明确为 UTC+8 语义，DWS 每日汇总调整为固定 rating 桶宽表；后续训练数据读侧能力可以复用这些 app/domain/infra 边界继续暴露 HTTP API。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、`./scripts/check-doc-sync.sh origin/main WORKTREE` 和 `git diff --check`。

### 2026-06-27 - 训练数据多 OJ ODS 建模

- 成果：新增 `platform-training-data` Maven 模块，落地 Codeforces 垂直 OJ 数仓模块、独立 submission ODS 表、HTTP 写入入口、record/parser/writer/DDL/upsert/fixture/tests、1000 条本地真实 Codeforces API 样本和 `training-data-web` 文件日志接入。
- 影响：训练数据模块不再只是占位；当前只保留 OJ 独立 ODS 建模与批量写入，不包含 DAG / pipeline / task run / scheduler。外置采集器可以批量提交原始 submission 数组，DWD/DWS/ADS 等下游层后续按真实查询需求再建模。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、`./scripts/check-doc-sync.sh origin/main WORKTREE` 和 `git diff --check`。

### 2026-06-25 - 项目待办和更新日志

- 成果：新增根目录待办列表、面向人类的更新日志，以及 agent 写更新日志的固定格式。
- 影响：后续 MR 需要按“成果 / 影响 / 验证”记录交付结果，未来 agent 也能从仓库文档中读到规则。
- 验证：已运行 `./scripts/check-doc-sync.sh origin/main WORKTREE`、`./scripts/check-test-policy.sh`、`mvn clean verify` 和 `git diff --check`。
