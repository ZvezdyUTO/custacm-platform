# custacm-platform

`custacm-platform` 是面向算法训练队的综合管理平台，目标是把账号管理、训练数据采集、训练数据查询和后续内容工具整合到一个统一后台里。

当前仓库已经包含三个可运行部分：

- `auth-web`：平台自有登录、账号管理、密码哈希和 JWT 签发。
- `training-data-web`：Codeforces 训练数据写入、采集、清理、刷新和查询。
- `frontend`：训练队后台工作台，提供训练查询和管理员操作页面。

平台使用 `studentIdentity` 作为业务身份，格式通常是“学号 + 姓名”，例如 `230511213黄炳睿`。管理员负责创建账号、维护 Codeforces handle 绑定、导入或采集训练数据；选手和管理员都可以通过前端查看训练数据。

## 最近支持

- React/Vite 前端工作台，包含登录、训练查询、管理员操作和同源 API 代理。
- 管理员批量创建账号，并可同时补充 Codeforces handle 绑定。
- Codeforces ODS 导入、最近提交后台采集任务、采集任务列表和 DWD/DWM/DWS 仓库刷新。
- 训练查询支持按选手、日期和 rating 范围查看 AC 汇总、提交明细和首 AC 明细。
- 提交明细支持后端分页，适合查看较大的提交列表。
- 高成本操作增加确认弹窗，包括训练数据采集和彻底删除用户数据。
- 管理员可以清理某个用户的 Codeforces 训练数据，并继续删除 auth 账号。

## 快速启动

准备本地配置和 JWT 密钥：

```bash
cp deploy/.env.example deploy/.env
mkdir -p deploy/secrets
openssl genrsa -out deploy/secrets/auth-private-key.pem 2048
openssl rsa -in deploy/secrets/auth-private-key.pem -pubout -out deploy/secrets/auth-public-key.pem
```

编辑 `deploy/.env`，至少修改数据库密码和初始管理员密码：

```env
AUTH_DB_PASSWORD=change-me-auth-db-password
AUTH_DB_ROOT_PASSWORD=change-me-auth-root-password
TRAINING_DATA_DB_PASSWORD=change-me-training-data-db-password
TRAINING_DATA_DB_ROOT_PASSWORD=change-me-training-data-root-password
AUTH_BOOTSTRAP_ADMIN_STUDENT_IDENTITY=root
AUTH_BOOTSTRAP_ADMIN_PASSWORD=change-me-root-password
```

启动整套本地服务：

```bash
./scripts/deploy.sh
```

默认访问地址：

- 前端：<http://localhost:3000/>
- Auth health：<http://localhost:8081/health>
- Training-data health：<http://localhost:8082/health>

首次登录使用 `deploy/.env` 中配置的 bootstrap admin。部署后建议尽快在前端修改初始管理员密码。

可选：导入本地 Codeforces 演示数据。

```bash
./scripts/seed-local-codeforces-data.sh
```

## 文档入口

- [deploy/README.md](deploy/README.md)：本地和服务器部署说明。
- [frontend/README.md](frontend/README.md)：前端运行、代理和页面职责。
- [docs/api.md](docs/api.md)：已实现 HTTP API。
- [docs/architecture.md](docs/architecture.md)：模块职责和架构边界。
- [docs/authorization.md](docs/authorization.md)：URL 鉴权分层规则。
- [platform-training-data/README.md](platform-training-data/README.md)：训练数据模块说明。
- [platform-training-data/docs/ods-submission.md](platform-training-data/docs/ods-submission.md)：Codeforces 数仓表和刷新语义。
- [AGENTS.md](AGENTS.md)：AI/agent 协作规则和工程约束。
