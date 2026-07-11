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

## 按服务更新

只更新唯一后端：

```bash
./scripts/update-module.sh blog-api
```

该脚本只支持 `blog-api`（别名 `blog`、`backend`），完成后轮询直接后端 `/health`。

只更新包含两份静态应用的前端 Nginx：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build frontend
```

仓库当前没有前端专用 update script，不要记录不存在的命令。

## 更新后验证

先从仓库根目录加载本次部署实际使用的端口；`docker compose --env-file` 本身不会设置后续 curl 所在 shell 的环境变量：

```bash
set -a
. deploy/.env
set +a

curl -fsS "http://localhost:${BACKEND_PORT}/health"
curl -fsS "http://localhost:${FRONTEND_PORT}/"
curl -fsS "http://localhost:${FRONTEND_PORT}/training/multiple"
curl -fsS "http://localhost:${FRONTEND_PORT}/api/health"
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
```

同时检查登录、player/admin protected routes、Flyway 日志、`logs/combined.log` 与 `logs/error.log`。前端变更要检查两套 Vue 3 应用的 history fallback、跨前端会话显示和浏览器 console。

## 数据安全

- 普通更新不得执行 `docker compose down --volumes`。
- 用户名、角色、handle、Blog 内容和训练 warehouse schema 变化必须通过正式 API/Flyway 交付。
- 不打印或提交数据库密码、JWT secret、bootstrap 密码、token、cookie 或 Authorization header。
