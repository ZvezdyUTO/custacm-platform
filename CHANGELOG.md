# 更新日志

给人类看的项目更新记录。每次 MR 合并前，由 agent 按 [docs/agent/changelog.md](docs/agent/changelog.md) 的格式在最上方追加本次交付成果。

## 未发布

### 2026-07-05 - 平台自有账号鉴权

- 成果：将鉴权模块从 Keycloak 适配改为平台自有账号、BCrypt 密码哈希、RSA JWT 签发、玩家自助和管理员用户管理接口。
- 影响：后端接口按 `/admin/**`、`/player/**` 和游客公开路径分层；登录失败后同一 `studentIdentity` 有 5 秒重试冷却，部署需要配置 auth MySQL 与 RSA JWT 密钥文件。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、`./scripts/check-doc-sync.sh origin/main WORKTREE`、`docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config` 和 `git diff --check`。

### 2026-06-27 - 训练数据多 OJ ODS 建模

- 成果：新增 `platform-training-data` Maven 模块，落地 Codeforces 垂直 OJ 数仓模块、独立 submission ODS 表、HTTP 写入入口、record/parser/writer/DDL/upsert/fixture/tests、1000 条本地真实 Codeforces API 样本和 `training-data-web` 文件日志接入。
- 影响：训练数据模块不再只是占位；当前只保留 OJ 独立 ODS 建模与批量写入，不包含 DAG / pipeline / task run / scheduler。外置采集器可以批量提交原始 submission 数组，DWD/DWS/ADS 等下游层后续按真实查询需求再建模。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、`./scripts/check-doc-sync.sh origin/main WORKTREE` 和 `git diff --check`。

### 2026-06-25 - 项目待办和更新日志

- 成果：新增根目录待办列表、面向人类的更新日志，以及 agent 写更新日志的固定格式。
- 影响：后续 MR 需要按“成果 / 影响 / 验证”记录交付结果，未来 agent 也能从仓库文档中读到规则。
- 验证：已运行 `./scripts/check-doc-sync.sh origin/main WORKTREE`、`./scripts/check-test-policy.sh`、`mvn clean verify` 和 `git diff --check`。
