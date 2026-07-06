# deploy

本目录提供服务器 / 开发环境的一键部署编排。当前会启动：

- `auth-db`：`platform-auth` 使用的 MySQL
- `custacm-backend`：运行 `platform-auth/auth-web`
- `training-data-db`：`platform-training-data` 使用的 MySQL
- `custacm-training-data-web`：运行 `platform-training-data/training-data-web`
- `custacm-frontend`：运行前端静态站点和同源 API 反向代理

## Quick Start

在仓库根目录执行：

```bash
cp deploy/.env.example deploy/.env
mkdir -p deploy/secrets
openssl genrsa -out deploy/secrets/auth-private-key.pem 2048
openssl rsa -in deploy/secrets/auth-private-key.pem -pubout -out deploy/secrets/auth-public-key.pem
```

编辑 `deploy/.env`，至少修改：

```env
AUTH_DB_VOLUME_NAME=custacm-platform_auth-db-data
TRAINING_DATA_DB_VOLUME_NAME=custacm-platform_training-data-db-data
AUTH_DB_PASSWORD=change-me-auth-db-password
AUTH_DB_ROOT_PASSWORD=change-me-auth-root-password
TRAINING_DATA_DB_PASSWORD=change-me-training-data-db-password
TRAINING_DATA_DB_ROOT_PASSWORD=change-me-training-data-root-password
AUTH_BOOTSTRAP_ADMIN_STUDENT_IDENTITY=root
AUTH_BOOTSTRAP_ADMIN_PASSWORD=change-me-root-password
```

`AUTH_DB_VOLUME_NAME` 和 `TRAINING_DATA_DB_VOLUME_NAME` 是 MySQL 持久化卷名。默认值固定到项目级卷名，
避免因为启动目录、Compose project name 或 `-p` 参数变化而挂到一套新的空数据库。日常执行
`./scripts/deploy.sh`、`docker compose up -d --build`、`docker compose restart` 或 `docker compose down && docker compose up -d`
都会保留这两个卷里的数据。只有显式执行 `docker compose down --volumes`、`docker volume rm` 或 `docker volume prune`
才会删除数据库数据。

启动：

```bash
./scripts/deploy.sh
```

部署脚本会先运行一次性的 `frontend-build` 服务生成 `frontend/dist`，
再启动 Compose。`custacm-frontend` 本身是固定的 Nginx 容器，后续只改
前端时不需要重建前端镜像。

日常只更新某个业务模块时，见 [UPDATE.md](UPDATE.md)。
服务器部署和远端 AI 查日志方式见 [server-deployment.md](../docs/server-deployment.md)。

默认地址：

- Frontend: http://localhost:3000/
- Auth health: http://localhost:8081/health
- Training-data health: http://localhost:8082/health
- Login API: http://localhost:8081/api/auth/login
- Auth database: MySQL service `auth-db`, database `custacm_auth`
- Training-data database: MySQL service `training-data-db`, database `custacm_training`
- Database volumes: `${AUTH_DB_VOLUME_NAME}`, `${TRAINING_DATA_DB_VOLUME_NAME}`
- Backend logs: `../logs/combined.log`, `../logs/error.log`

## Auth Model

平台自己管理账号、密码和 JWT：

- 登录名是 `studentIdentity`，一个不可变字符串，例如 `230511213黄炳睿`。
- 账号角色为 `admin`、`player` 或 `disable`；`disable` 账号不能登录。`guest` 表示未登录访问者，不需要 JWT，也不进账号表。
- 密码使用 BCrypt 哈希存储。
- `auth-web` 使用 RSA 私钥签发 JWT，其它后端使用 RSA 公钥验证 JWT。
- JWT 只放标准时间字段、`sub`（用户 ID）和 `role`（`admin` / `player`）。
- 默认 access token 有效期为 `2h`，没有 refresh token。

## JWT Key Settings

Compose 默认把本地 PEM 文件挂到容器内：

```env
AUTH_JWT_PRIVATE_KEY_HOST_PATH=./secrets/auth-private-key.pem
AUTH_JWT_PUBLIC_KEY_HOST_PATH=./secrets/auth-public-key.pem
AUTH_JWT_ACCESS_TOKEN_TTL=2h
```

这些 PEM 文件是本地秘密，不要提交。其它需要验证平台 JWT 的后端只需要同一份公钥。

## Frontend Proxy

`frontend-build` 使用 `node:22-alpine` 在容器里执行 `pnpm install` 和
`pnpm build`，把静态产物写入 `frontend/dist`。`custacm-frontend` 使用
固定的 `nginx:1.27-alpine` 镜像挂载 `frontend/dist`，并反向代理：

```text
/api/auth/**          -> custacm-backend:8081
/api/training-data/** -> custacm-training-data-web:8082
/health/auth          -> custacm-backend:8081/health
/health/training-data -> custacm-training-data-web:8082/health
```

浏览器访问前端容器时只需要同源请求，不依赖后端 CORS。

只更新前端时运行：

```bash
./scripts/update-module.sh frontend
```

该命令只刷新 `frontend/dist` 并 reload Nginx，不重建或重启后端容器。

## Bootstrap Admin

首次启动时，如果 `AUTH_BOOTSTRAP_ADMIN_STUDENT_IDENTITY` 和 `AUTH_BOOTSTRAP_ADMIN_PASSWORD` 都已设置，`auth-web` 会在账号不存在时创建一个 `admin` 账号。已有账号不会被覆盖。

部署后应尽快登录并修改初始管理员密码。

## Notes

- 不要提交 `deploy/.env`。
- 不要提交 `deploy/secrets/*.pem`。
- 不要提交 `logs/` 下的运行时日志。
- 不要在日常更新中使用 `docker compose down --volumes` 或删除 `${AUTH_DB_VOLUME_NAME}` / `${TRAINING_DATA_DB_VOLUME_NAME}`，否则会清空 MySQL 数据。
- 如果修改数据库密码、JWT 密钥路径、端口或 Compose 结构，需要执行全量部署。
