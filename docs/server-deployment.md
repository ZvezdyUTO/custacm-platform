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

实际 host 端口分别取自 `FRONTEND_PORT`、`FRONTEND_HTTPS_PORT` 和 `BACKEND_PORT`。Nginx 为 `/` 与 `/training/**` 提供 Vue Blog history fallback，使训练路由继续使用原 Blog 顶栏；内部 `/training-app/**` 提供独立训练运行时，并将 `/api/**` 去前缀后转发到 `blog-api:8090`。

## HTTPS（可选）

准备 Cloudflare Origin CA 或公信 CA 的 PEM 证书后，将证书和私钥分别保存为 `TLS_CERT_DIR/origin.pem`、`TLS_CERT_DIR/origin.key`，设置 `TLS_ENABLED=true` 与服务器的 `FRONTEND_HTTPS_PORT=443`。前端容器仅以只读方式读取该目录，80 会重定向到 HTTPS。Cloudflare 场景在 DNS 记录启用代理后，将 SSL/TLS 模式设为 Full (strict)；不要把私钥保存到仓库或 `.env`。

## 构建与运行数据

- 后端镜像打包 `platform-blog/upstream/nblog/blog-api` 及其 reactor 依赖。
- 前端镜像在 Node 20.19 stage 内分别构建 Vue 3 Training/pnpm 与 Vue 3 Blog/npm 产物，再由 Nginx 1.27 Alpine 提供静态文件。
- 应用日志 bind mount 到 `logs/combined.log`、`logs/error.log`。上传目录对 Blog API 为读写挂载、对前端 Nginx 为只读挂载；托管图片由 Nginx 从 `/api/image/**` 直接返回。
- MySQL 与 Redis 使用 `BLOG_DB_VOLUME_NAME`、`BLOG_REDIS_VOLUME_NAME` 命名卷，容器重建后继续保留。

## 验证

`--env-file` 只供 Compose 解析，不会自动设置当前 shell。请从仓库根目录加载同一份配置，再使用实际端口检查：

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

还要使用真实浏览器验证 `/` 与 `/training/**` 的刷新、登录、`ROLE_player`/`ROLE_admin` 权限，以及“创建用户”“管理用户”“数据采集”三个管理员页面；数据采集页面不得出现可选数仓刷新开关。

## 服务器部署与更新入口

无论首次部署、日常更新、后端变化或前端变化，都从仓库根目录运行：

```bash
./scripts/deploy.sh
```

脚本不会拉取或切换 Git 分支。操作者先显式更新源码，再用普通模式入口一起校验、构建、启动和检查四个服务。`./scripts/dev.sh` 仅用于本机前端 HMR 开发，不用于服务器部署；仓库不维护按模块部署或第三个启动入口。

更完整流程见 [../deploy/UPDATE.md](../deploy/UPDATE.md)。

## 安全约束

- 不要公开 MySQL/Redis 端口，不要记录 secret、密码、token、cookie 或 Authorization header。
- bootstrap 账号只在配置用户名不存在时创建；首次登录后应通过 API 修改初始密码。
- 普通更新不得删除命名卷；`docker compose down --volumes` 只有在用户明确批准数据销毁时才能执行。
