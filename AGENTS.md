# Agent Instructions

## 开始前

- 本仓库是 CUST ACM 集训队的 Blog 与训练平台。
- 修改目录前先读最近的 `AGENTS.md`；只按任务需要读取 [docs/README.md](docs/README.md) 中对应文档，不要预加载整套文档。
- 代码、配置、迁移和测试是实现事实；文档与它们冲突时先核验事实，再修正文档。

## 当前边界

- `platform-blog/upstream/nblog/blog-api` 是唯一 Spring Boot 后端，保留 `top.naccl` package，统一负责 Blog、账号、认证和训练 HTTP adapter。
- `platform-training-data` 只提供 Codeforces/AtCoder 采集、ODS/DWD/DWM/DWS、查询、调度、刷新和清理库；不拥有 Web runtime、账号或认证。
- `platform-common` 只放已有重复需求证明的通用基础能力，不放业务实体。
- 业务身份和 JWT `sub` 都是 `username`；存储角色只有 `ROLE_admin`、`ROLE_player`，guest 表示未认证。
- 对外只有一个 Nginx `frontend` 服务：Blog 位于 `/`，训练外壳位于 `/training/**`，内部训练产物位于 `/training-app/**`，浏览器 API 位于 `/api/**`。
- Blog 与 Training 保持两套 Vue Router，共享 `custacm.accessToken`、`custacm.user`。公开请求不得全局附加 JWT，受保护请求由 adapter 显式发送 Bearer token。
- URL 授权和资源所有权以 [docs/authorization.md](docs/authorization.md) 为准，HTTP 路径以 [docs/api.md](docs/api.md) 为准。

## 不可破坏的规则

- BCrypt 密码、HS512 JWT、账号、角色、OJ handle 和 token 签发都属于 Blog API；没有公开注册、demo token 或内存登录流程。
- 管理员替换或移除 handle 前必须先清理该用户对应 OJ 的可再生训练数据；不能新增绕过清理的覆盖入口。
- 固定 `root` 管理员不可删除、改名、降权、绑定 OJ handle 或进入队员采集状态。
- 已应用的 Flyway migration 不得修改；结构变化使用新的 migration。
- 自动提交采集和 AtCoder 元数据 bootstrap/调度默认关闭，只能由部署配置显式开启。
- 修改后端日志前读 [docs/logging.md](docs/logging.md)。禁止记录密码、token、cookie、Authorization header、签名密钥、数据库密码或完整敏感个人信息。

## 文档原则

- `AGENTS.md` 只保存不可违反的约束；模块 README 只说明职责、顶层目录、依赖边界、关键入口和验证命令。
- 只有公开 API、安全规则、模块职责、运行拓扑、部署步骤或用户可见行为变化时，才更新对应的唯一权威文档。普通内部重构、测试或样式微调不要求仪式性改文档。
- 文档入口和事实归属见 [docs/README.md](docs/README.md)。不要维护逐文件、逐测试或逐迁移清单；源码搜索比复制清单可靠。
- 无法从仓库证明的事实写成 TODO，不要猜测。`CHANGELOG.md` 只记录有用户、部署、数据或兼容性影响的变化。

## 验证

按改动范围运行最小充分检查：

| 改动 | 检查 |
| --- | --- |
| Java、POM、后端合同 | `mvn clean test` |
| 打包或后端镜像 | `mvn clean package -DskipTests` |
| Training 前端 | `cd frontend && pnpm lint && pnpm test && pnpm typecheck && pnpm build` |
| Blog 前端 | `cd platform-blog/upstream/nblog/blog-view && npm install && npm test && npm run build` |
| 共享设计 token | `./scripts/sync-design-tokens.sh --check`，并构建两端 |
| Compose 配置 | `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config` |
| 文档或任意文本 | `git diff --check`，并检查受影响的本地链接 |

新增或实质修改业务逻辑、安全解析、Controller 或 adapter 时增加聚焦测试。不得为通过检查而删除、跳过或弱化已有测试。文档-only 变更不要求 Maven。

## Git 与协作

- 只有用户明文要求时才提交；只有用户明文要求时才推送。用户说“推送”时默认同时授权提交和推送。
- MR 标题和描述使用中文。非项目负责人发起的 MR 未经负责人明确确认不得合并；负责人本人明确要求合并即可。
- 不提交 secret、真实 `.env`、日志、构建产物或本地数据。
- 新文件确实需要 Author 标记时使用 `huangbingrui.awa`。
