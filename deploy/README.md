# 本地/单机集成部署

`deploy/docker-compose.yml` 定义四个服务：

| Service | Image/build | Responsibility |
| --- | --- | --- |
| `blog-db` | MySQL 8.4 | 统一 NBlog 与训练数据 schema |
| `blog-redis` | Redis 7 Alpine | Blog cache 与运行支持 |
| `blog-api` | 仓库根 `Dockerfile` | 唯一 Spring Boot 后端，容器端口 8090 |
| `frontend` | `frontend/Dockerfile` | 一个 Nginx 同时托管两套 Vue 3 应用，容器端口 80 |

当前配置可用于本地或单台主机；仓库没有记录任何已经完成的服务器发布。

## 启动

从仓库根目录运行：

```bash
cp deploy/.env.example deploy/.env
# 替换所有 change-me 值，并选择未占用的 BACKEND_PORT、FRONTEND_PORT
docker compose --env-file deploy/.env -f deploy/docker-compose.yml config
./scripts/deploy.sh
```

`./scripts/deploy.sh` 使用 `deploy/.env`，最终执行 `docker compose ... up -d --build`。也可以把环境文件显式传给底层脚本：

```bash
./deploy/deploy.sh deploy/.env
```

Flyway 会在空数据库上执行 `V001` 与 `V010`–`V024`。只有配置的用户名尚不存在时，`BLOG_BOOTSTRAP_ADMIN_USERNAME` 与 `BLOG_BOOTSTRAP_ADMIN_PASSWORD` 才会幂等创建首个管理员。

## 访问地址

使用 `.env.example` 默认端口时：

| URL | Meaning |
| --- | --- |
| `http://localhost:3000/` | Vue Blog |
| `http://localhost:3000/training/multiple` | Vue 3 训练中心 |
| `http://localhost:3000/api/health` | 经 Nginx 访问 Blog API health |
| `http://localhost:8090/health` | 直接访问 Blog API health |

Nginx 路由规则：

```text
/api/**       -> blog-api:8090/**（去掉 /api）
/training     -> 302 /training/multiple
/training/**  -> Vue Blog history fallback（保留唯一顶栏并承载训练内容）
/training-app/** -> Vue 3 Training 内部 history fallback
/**           -> Vue Blog history fallback
```

## 环境变量

| Variable | Meaning |
| --- | --- |
| `BACKEND_PORT` | host 映射到 Blog API 8090 的端口 |
| `FRONTEND_PORT` | host 映射到 Nginx 80 的端口 |
| `BLOG_DB_NAME` | 统一数据库名 |
| `BLOG_DB_USERNAME`、`BLOG_DB_PASSWORD` | 应用数据库账号与密码 |
| `BLOG_DB_ROOT_PASSWORD` | MySQL root 密码 |
| `BLOG_DB_VOLUME_NAME` | MySQL 命名卷 |
| `BLOG_REDIS_VOLUME_NAME` | Redis 命名卷 |
| `BLOG_TOKEN_SECRET` | 至少 64 字符的 HS512 secret |
| `BLOG_TOKEN_TTL_MILLIS` | 登录 token TTL（毫秒） |
| `BLOG_BOOTSTRAP_ADMIN_*` | 幂等首管理员凭据 |

`deploy/.env` 不得提交。不要把 placeholder secret 用到共享环境。

## 构建方式

- `blog-api` 镜像从 Maven reactor 打包 `platform-blog/upstream/nblog/blog-api`。
- `frontend` 镜像使用 Node 20.19：通过 pnpm lock 构建 Vue 3 训练中心，通过 npm lock 构建 Vue 3 Blog，然后复制到 Nginx 1.27 Alpine。
- Nginx 内部将 Blog 产物放在 root，将训练中心产物放在内部 `/training-app`；公开 `/training/**` 仍由 Blog 外壳处理。

## 健康检查

Compose 的 `--env-file` 不会自动把变量写入当前 shell。以下命令从仓库根目录显式加载实际端口后再检查：

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

还应在浏览器中验证 Blog、训练中心刷新、登录、player/admin 权限与两份前端之间的普通链接。

## 数据与挂载

- MySQL 与 Redis 数据存放在显式命名卷中，容器重建不会自动删除。
- 应用日志 bind mount 到仓库 `logs/`，上传文件 bind mount 到 `uploads/`。
- 新卷与旧部署卷相互独立；没有单独批准前不要删除旧卷。
- 普通更新禁止使用 `down --volumes`。

更新流程见 [UPDATE.md](UPDATE.md)。
