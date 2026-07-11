# 单机 Compose 部署说明

仓库当前提供可用于本地或单台主机的 Compose 配置，包含四个服务：`blog-db`、`blog-redis`、唯一后端 `blog-api` 和统一 Nginx `frontend`。本文是操作说明，不表示这些命令已经在某台服务器执行，也不构成已上线声明。

## 配置与启动

从仓库根目录运行：

```bash
cp deploy/.env.example deploy/.env
# 替换所有 change-me secret，配置可用的 BACKEND_PORT 与 FRONTEND_PORT
docker compose --env-file deploy/.env -f deploy/docker-compose.yml config
./scripts/deploy.sh
```

默认地址：

```text
Vue Blog:       http://localhost:3000/
Vue 3 Training: http://localhost:3000/training/multiple
Browser API:    http://localhost:3000/api/**
Backend health: http://localhost:8090/health
Gateway health: http://localhost:3000/api/health
```

实际 host 端口分别取自 `FRONTEND_PORT` 和 `BACKEND_PORT`。Nginx 为 `/` 与 `/training/**` 提供 Vue Blog history fallback，使训练路由继续使用原 Blog 顶栏；内部 `/training-app/**` 提供独立训练运行时，并将 `/api/**` 去前缀后转发到 `blog-api:8090`。

## 构建与运行数据

- 后端镜像打包 `platform-blog/upstream/nblog/blog-api` 及其 reactor 依赖。
- 前端镜像在 Node 20.19 stage 内分别构建 Vue 3 Training/pnpm 与 Vue 3 Blog/npm 产物，再由 Nginx 1.27 Alpine 提供静态文件。
- 应用日志 bind mount 到 `logs/combined.log`、`logs/error.log`，上传目录 bind mount 到 `uploads/`。
- MySQL 与 Redis 使用 `BLOG_DB_VOLUME_NAME`、`BLOG_REDIS_VOLUME_NAME` 命名卷，容器重建后继续保留。

## 验证

`--env-file` 只供 Compose 解析，不会自动设置当前 shell。请从仓库根目录加载同一份配置，再使用实际端口检查：

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

还要使用真实浏览器验证 `/` 与 `/training/**` 的刷新、登录、`ROLE_player`/`ROLE_admin` 权限，以及“创建用户”“管理用户”“数据采集”三个管理员页面；数据采集页面不得出现可选数仓刷新开关。

## 更新

完整重建：

```bash
./scripts/deploy.sh
```

只重建 Blog API：

```bash
./scripts/update-module.sh blog-api
```

只重建统一前端：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build frontend
```

更完整流程见 [../deploy/UPDATE.md](../deploy/UPDATE.md)。

## 安全约束

- 不要公开 MySQL/Redis 端口，不要记录 secret、密码、token、cookie 或 Authorization header。
- bootstrap 账号只在配置用户名不存在时创建；首次登录后应通过 API 修改初始密码。
- 普通更新不得删除命名卷；`docker compose down --volumes` 只有在用户明确批准数据销毁时才能执行。
