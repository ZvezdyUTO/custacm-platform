# 本地/单机集成部署

`deploy/docker-compose.yml` 定义四个服务：

| Service | Image/build | Responsibility |
| --- | --- | --- |
| `blog-db` | MySQL 8.4 | 统一 NBlog 与训练数据 schema |
| `blog-redis` | Redis 7 Alpine | Blog cache 与运行支持 |
| `blog-api` | 仓库根 `Dockerfile` | 唯一 Spring Boot 后端，容器端口 8090 |
| `frontend` | `frontend/Dockerfile` | 一个 Nginx 同时托管两套 Vue 3 应用，容器端口 80，并可选启用容器端口 443 TLS |

当前配置可用于本地或单台主机，只维护 Compose 部署，不保留未实现的 Kubernetes 占位目录；仓库没有记录任何已经完成的服务器发布。

## 两种启动模式

从仓库根目录运行：

```bash
cp deploy/.env.example deploy/.env
# 替换所有 change-me 值，并选择未占用的 BACKEND_PORT、FRONTEND_PORT
docker compose --env-file deploy/.env -f deploy/docker-compose.yml config
```

本地频繁修改前端时运行开发者模式：

```bash
./scripts/dev.sh
```

它停止生产 Nginx，保留 Docker 中的 `blog-db`、`blog-redis`、`blog-api`，且不强制重建后端镜像；随后在宿主机以前台进程启动 Training Vite 5173 与 Blog Vite 4180。修改 Vue/TS/CSS 后通过 HMR 即时生效，按 Ctrl-C 只停止两份 Vite，后端容器继续运行。开发模式要求 `BACKEND_PORT=8090`。

验收、稳定运行或服务器部署使用普通模式：

```bash
./scripts/deploy.sh
```

它一起构建并启动四个 Compose 服务，将两份前端静态产物固化进 Nginx 镜像并检查所有公开路径。普通模式会重新启用生产 Nginx；需要临时指定另一份环境文件时，两个入口都可把路径作为唯一参数。两个脚本都不会删除 MySQL 或 Redis 命名卷。

Flyway 会在空数据库上执行版本化迁移。配置 `BLOG_BOOTSTRAP_ADMIN_PASSWORD` 后，应用会幂等创建固定用户名 `root` 的首个管理员；`root` 是不可删除、不可改名、不可降权且不可绑定 OJ handle 的系统账号。

升级已有数据库时，V034 会把旧 `oj_handle_account` JSON 拆成关系化的 `training_member` 与 `oj_handle_binding`；生产代码此后只读写新表，旧表保留一个迁移窗口。迁移会先在临时表校验 JSON 与大小写精确的 handle 唯一性，历史脏数据会阻止启动而不会静默丢弃，升级前检查和恢复步骤见 [UPDATE.md](UPDATE.md)。V035 会删除已退役页面、Quartz、访问统计和历史应用日志的独占表。

## 访问地址

普通模式使用 `.env.example` 默认端口时：

| URL | Meaning |
| --- | --- |
| `http://localhost:3000/` | Vue Blog |
| `http://localhost:3000/training/multiple` | Vue 3 训练中心 |
| `http://localhost:3000/api/health` | 经 Nginx 访问 Blog API health |
| `http://localhost:8090/health` | 直接访问 Blog API health |

开发者模式入口为 `http://localhost:4180/` 与 `http://localhost:4180/training/multiple`，API 仍为 `http://localhost:8090/health`。

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
| `FRONTEND_HTTPS_PORT` | host 映射到 Nginx 443 的端口；本地默认使用 3443，服务器通常使用 443 |
| `TLS_ENABLED` | `true` 时前端 Nginx 将 80 重定向到 HTTPS，并从 `TLS_CERT_DIR` 读取 `origin.pem`、`origin.key` |
| `TLS_CERT_DIR` | 宿主机证书目录；仅以只读方式挂载到容器，私钥不得提交到仓库 |
| `BLOG_DB_NAME` | 统一数据库名 |
| `BLOG_DB_USERNAME`、`BLOG_DB_PASSWORD` | 应用数据库账号与密码 |
| `BLOG_DB_ROOT_PASSWORD` | MySQL root 密码 |
| `BLOG_DB_VOLUME_NAME` | MySQL 命名卷 |
| `BLOG_REDIS_VOLUME_NAME` | Redis 命名卷 |
| `BLOG_TOKEN_SECRET` | 至少 64 字符的 HS512 secret |
| `BLOG_TOKEN_TTL_MILLIS` | 登录 token TTL（毫秒） |
| `BLOG_BOOTSTRAP_ADMIN_PASSWORD` | 固定 `root` 系统管理员的初始密码 |
| `BLOG_CACHE_TTL` | Blog Redis 缓存 TTL，默认 `10m`；Redis 故障时读取降级到数据库 |
| `BLOG_DAILY_COLLECTION_LOOKBACK` | 自动采集每日任务从各用户/OJ 上次成功游标向前回看的时长，默认 `100h` |
| `BLOG_INTRADAY_COLLECTION_LOOKBACK` | 自动采集日内半小时任务的回看时长，默认 `0h`，即从上次成功游标直接续爬 |
| `BLOG_CODEFORCES_DAILY_COLLECTION_ENABLED`、`BLOG_CODEFORCES_INTRADAY_COLLECTION_ENABLED` | 是否启用 Codeforces 两组自动采集计划，默认 `false` |
| `BLOG_ATCODER_DAILY_COLLECTION_ENABLED`、`BLOG_ATCODER_INTRADAY_COLLECTION_ENABLED` | 是否启用 AtCoder 两组自动采集计划，默认 `false` |
| `BLOG_ATCODER_PROBLEM_LIST_SCHEDULE_ENABLED` | 是否启用 AtCoder 题目元数据定时采集，默认 `false` |
| `BLOG_ATCODER_PROBLEM_LIST_BOOTSTRAP_ENABLED` | 是否在启动时补采 AtCoder 题目元数据，默认 `false` |

`deploy/.env` 不得提交。不要把 placeholder secret 用到共享环境。

自动采集全部默认关闭。只有确认外部 API 配额、服务器带宽和数据库负载后才应逐项改为 `true`；启用后的每日任务默认回看 100 小时，日内任务默认零回看，首次没有游标时仍采集全部历史。手动“数据采集”页面不受这些定时开关或自动任务回看值影响。

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

还应在浏览器中验证 Blog、训练中心刷新、登录、player/admin 权限与两份前端之间的普通链接。

## 数据与挂载

- MySQL 与 Redis 数据存放在显式命名卷中，容器重建不会自动删除。
- 应用日志 bind mount 到仓库 `logs/`。`uploads/` 以读写方式挂载给 Blog API、以只读方式挂载给 Nginx；Nginx 直接服务 `/api/image/**` 并缓存 UUID 图片，其他 API 仍代理 Blog API。
- HTTPS 是可选模式：准备好 Cloudflare Origin CA 或公信 CA 的 PEM 证书后，将 `TLS_ENABLED=true`、`FRONTEND_HTTPS_PORT=443`，并把 `origin.pem`、`origin.key` 放入 `TLS_CERT_DIR`。启用后只通过 HTTPS 访问站点；`deploy.sh` 会改用 HTTPS 入口验证，`-k` 仅用于本机验证 Origin CA（浏览器不应使用该方式）。
- 新卷与旧部署卷相互独立；没有单独批准前不要删除旧卷。
- 普通更新禁止使用 `down --volumes`。

更新流程见 [UPDATE.md](UPDATE.md)。
