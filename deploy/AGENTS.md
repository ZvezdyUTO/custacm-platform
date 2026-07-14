# Deploy Agent Notes

- `docker-compose.yml` 恰好定义四个服务：`blog-db`、`blog-redis`、`blog-api`、`frontend`。不要把已移除的独立后端写回部署拓扑。
- `blog-api` 是唯一后端；`frontend` 是一个 Nginx 服务，由 Vue 3 Blog 持有 `/` 与训练外壳 `/training/**`，内部训练产物位于 `/training-app/**`。业务 `/api/**` 转发 Blog API，`/api/image/**` 从只读 `uploads/` 挂载直接返回。
- `/api/image/**` 的 Referer 白名单由 `FRONTEND_IMAGE_REFERER_HOSTS` 和 `FRONTEND_ALLOW_LOCAL_REFERERS` 在前端容器启动时生成；不要把 `none` 或 `blocked` 放入可信来源。
- host 端口分别由 `BACKEND_PORT`、`FRONTEND_PORT` 与 `FRONTEND_HTTPS_PORT` 控制。Compose 内部固定使用 `blog-api:8090` 与 `frontend:80/443`；只有 `TLS_ENABLED=true` 且挂载了证书文件时，Nginx 才启用 443。
- `deploy/.env` 仅供本机使用，不得提交；`.env.example` 只能保存占位值。HS512 secret、数据库密码、bootstrap 密码、token 和 Authorization header 不得进入日志或版本库。
- OJ 自动采集、AtCoder 题目计划与启动补采默认关闭；只有操作者在 `.env` 明确将对应 `*_ENABLED` 变量设为 `true` 才运行。自动提交采集每日默认从上次成功游标回看 `100h`，日内默认以 `0h` 直接续爬；旧 `.env` 缺少回看变量时由 Compose 注入这两个新默认值。
- MySQL 与 Redis 使用 `BLOG_DB_VOLUME_NAME`、`BLOG_REDIS_VOLUME_NAME` 指定的命名卷。普通部署或更新不得运行 `docker compose down --volumes`；旧数据卷也不得自动引用或删除。
- TLS 私钥只能位于 `TLS_CERT_DIR` 指定的宿主机受限目录，并以只读方式挂载到前端容器；不得写入仓库、镜像、`.env`、日志或版本库。
- 启动入口恰好两个：`./scripts/dev.sh` 用于本机前端开发（Docker 后端三服务 + 两个 Vite/HMR 进程），`./scripts/deploy.sh` 用于完整稳定构建（四个 Compose 服务）。不要新增第三个入口、按模块部署、自动拉取部署或 `deploy/` 下的包装脚本。
- 配置变更至少运行 `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config`。构建/部署后检查后端 `/health`、前端 `/`、`/training/multiple` 与 `/api/health`。
- 当前文件只描述本地/单机 Compose 能力；没有实际执行证据时不得声称已部署到服务器。
- 修改部署配置时同步本文件、`README.md`、`UPDATE.md`、`../frontend/README.md`、`../docs/architecture.md`、`../docs/server-deployment.md` 和 agent context 文档。
