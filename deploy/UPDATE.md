# 更新本地/单机集成栈

## 更新前检查

1. 对照 `.env.example` 手工更新 `deploy/.env`，不要复制 placeholder secret。
2. Java 或 Maven 变更运行：

   ```bash
   mvn clean verify
   ./scripts/check-test-policy.sh
   mvn clean package -DskipTests
   ```

3. Vue 3 训练中心变更在 `frontend/` 运行：

   ```bash
   pnpm lint
   pnpm test
   pnpm typecheck
   pnpm build
   ```

4. Vue Blog 变更在 `platform-blog/upstream/nblog/blog-view/` 运行：

   ```bash
   npm ci
   npm test
   npm run build
   ```

5. 验证 Compose：

   ```bash
   docker compose --env-file deploy/.env -f deploy/docker-compose.yml config
   ```

只执行与本次变更相符的构建门禁；纯文档变更不需要 Maven 或前端构建。

## 完整更新

以下命令构建并启动 `blog-db`、`blog-redis`、`blog-api` 和 `frontend`：

```bash
./scripts/deploy.sh
```

## 普通模式更新入口

验收、稳定运行和服务器更新统一运行：

```bash
./scripts/deploy.sh
```

该入口一起构建并启动 `blog-db`、`blog-redis`、`blog-api` 和 `frontend`，Compose 会复用未变化的镜像层与持久化命名卷。源码更新由操作者在运行脚本前显式完成，部署脚本不会隐式执行 Git 操作。

本地只频繁修改前端时运行 `./scripts/dev.sh`；它不强制重建后端镜像，使用 Vite/HMR 即时加载前端变化。开发结束后按 Ctrl-C，再运行 `./scripts/deploy.sh` 即可切回普通 Nginx 模式。仓库不提供按单个服务更新、自动拉取部署或第三个启动入口。

## 更新后验证

先从仓库根目录加载本次部署实际使用的端口；`docker compose --env-file` 本身不会设置后续 curl 所在 shell 的环境变量：

```bash
set -a
. deploy/.env
set +a

curl -fsS "http://localhost:${BACKEND_PORT}/health"
if [ "${TLS_ENABLED:-false}" = "true" ]; then
  curl -fkLsS "https://localhost:${FRONTEND_HTTPS_PORT}/"
  curl -fkLsS "https://localhost:${FRONTEND_HTTPS_PORT}/training/multiple"
  curl -fkLsS "https://localhost:${FRONTEND_HTTPS_PORT}/api/health"
else
  curl -fsS "http://localhost:${FRONTEND_PORT}/"
  curl -fsS "http://localhost:${FRONTEND_PORT}/training/multiple"
  curl -fsS "http://localhost:${FRONTEND_PORT}/api/health"
fi
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
```

启用 HTTPS 时，还要检查证书目录中存在 `origin.pem` 和 `origin.key`，且 `TLS_ENABLED=true`、`FRONTEND_HTTPS_PORT` 已设置为预期端口；私钥不得输出到终端或提交到 Git。

同时检查登录、player/admin protected routes、Flyway 日志、`logs/combined.log` 与 `logs/error.log`。前端变更要检查两套 Vue 3 应用的 history fallback、跨前端会话显示和浏览器 console。

涉及托管图片时，再确认 Blog API 能写入 `uploads/assets/`、Nginx 能通过 `/api/image/assets/{uuid}/thumbnail.*` 读取同一文件，并在删除文章或更换头像后返回 404。

## 数据安全

- 普通更新不得执行 `docker compose down --volumes`。
- 用户名、角色、handle、Blog 内容和训练 warehouse schema 变化必须通过正式 API/Flyway 交付。
- 不打印或提交数据库密码、JWT secret、bootstrap 密码、token、cookie 或 Authorization header。
