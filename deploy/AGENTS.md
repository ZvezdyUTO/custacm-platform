# Deploy Agent Notes

- `docker-compose.yml` 固定定义 `blog-db`、`blog-redis`、`blog-api`、`frontend` 四个服务；不得恢复独立认证或训练后端。
- `blog-api` 是唯一后端；一个 Nginx `frontend` 同时提供 Blog `/`、Training 外壳 `/training/**`、内部 `/training-app/**` 和浏览器 `/api/**` 网关。
- 启动入口只有根目录 `./scripts/dev.sh` 与 `./scripts/deploy.sh`。不得增加模块级部署、第三种启动模式、隐式 Git 拉取或切换分支。
- `deploy/.env` 仅供本机或部署环境使用，不得提交。`.env.example` 只能保存占位值；密码、JWT secret、token、Cookie、Authorization header 和 TLS 私钥不得进入仓库、镜像或日志。
- MySQL、Redis 使用显式命名卷。普通部署或升级不得执行 `docker compose down --volumes`，也不得自动引用、迁移或删除旧卷。
- TLS 私钥只能放在 `TLS_CERT_DIR` 指向的宿主机受限目录，并以只读方式挂载。
- OJ 自动采集、AtCoder 题目计划和启动补采默认关闭，只能由实际环境显式开启。
- 配置变化至少运行 `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config`；运行方式、环境变量、数据安全或升级步骤变化时同步更新 `README.md`。
- 没有真实执行证据时，不得声称已经部署到服务器。
