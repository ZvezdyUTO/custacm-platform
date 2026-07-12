# Deploy Agent Notes

- `docker-compose.yml` 恰好定义四个服务：`blog-db`、`blog-redis`、`blog-api`、`frontend`。不要把已移除的独立后端写回部署拓扑。
- `blog-api` 是唯一后端；`frontend` 是一个 Nginx 服务，由 Vue 3 Blog 持有 `/` 与训练外壳 `/training/**`，内部训练产物位于 `/training-app/**`。业务 `/api/**` 转发 Blog API，`/api/image/**` 从只读 `uploads/` 挂载直接返回。
- host 端口分别由 `BACKEND_PORT` 和 `FRONTEND_PORT` 控制。Compose 内部固定使用 `blog-api:8090` 与 `frontend:80`。
- `deploy/.env` 仅供本机使用，不得提交；`.env.example` 只能保存占位值。HS512 secret、数据库密码、bootstrap 密码、token 和 Authorization header 不得进入日志或版本库。
- MySQL 与 Redis 使用 `BLOG_DB_VOLUME_NAME`、`BLOG_REDIS_VOLUME_NAME` 指定的命名卷。普通部署或更新不得运行 `docker compose down --volumes`；旧数据卷也不得自动引用或删除。
- 启动入口恰好两个：`./scripts/dev.sh` 用于本机前端开发（Docker 后端三服务 + 两个 Vite/HMR 进程），`./scripts/deploy.sh` 用于完整稳定构建（四个 Compose 服务）。不要新增第三个入口、按模块部署、自动拉取部署或 `deploy/` 下的包装脚本。
- 配置变更至少运行 `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config`。构建/部署后检查后端 `/health`、前端 `/`、`/training/multiple` 与 `/api/health`。
- 当前文件只描述本地/单机 Compose 能力；没有实际执行证据时不得声称已部署到服务器。
- 修改部署配置时同步本文件、`README.md`、`UPDATE.md`、`../frontend/README.md`、`../docs/architecture.md`、`../docs/server-deployment.md` 和 agent context 文档。
