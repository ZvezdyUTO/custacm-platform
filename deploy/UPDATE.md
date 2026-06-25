# Module Update Guide

本文档记录服务器和本机的日常模块更新流程。之后要更新某个模块时，先看这里。

## Current Shape

当前 Compose 有 3 个容器：

```text
keycloak-db       Keycloak 的 PostgreSQL
keycloak          登录、用户管理、JWT 签发
custacm-backend   当前业务后端容器，实际运行 platform-auth/auth-web
```

当前可单独更新的业务模块：

```text
auth-web -> platform-auth/auth-web -> custacm-backend
```

未来规则保持简单：

```text
一个可独立运行的 Spring Boot *-web 模块 = 一个 docker compose service = 一个容器
```

## Full Deploy

第一次部署、环境变量变更、Compose 结构变更时，用全量部署：

```bash
cp deploy/.env.example deploy/.env
vim deploy/.env
./scripts/deploy.sh
```

`deploy.sh` 会执行：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
```

服务器上推荐用一键部署入口：

```bash
./scripts/server-deploy.sh
```

它会拉取 `origin/main` 的 fast-forward 更新、创建 `logs/`、构建并启动 Compose、检查 `/health`。

## Update One Module

从 GitHub 更新代码后，只更新一个业务模块：

```bash
git pull origin main
./scripts/update-module.sh auth-web
```

脚本会执行：

```text
1. 创建 logs/ 目录
2. docker compose build custacm-backend
3. docker compose up -d --no-deps custacm-backend
4. curl /health 验证容器是否恢复
```

后端 Maven 构建在 Dockerfile 的 build stage 中执行，服务器不需要安装 Maven/JDK。

`--no-deps` 表示只重启这个业务容器，不重启 Keycloak 和 PostgreSQL。

查看当前支持的模块：

```bash
./scripts/update-module.sh list
```

## Auto Update Main

服务器上如果希望 `main` 分支更新后自动拉取并更新容器，可以跑：

```bash
./scripts/auto-update-main.sh watch
```

默认行为：

```text
1. 每 60 秒 git fetch origin main
2. 发现新 commit 后，只允许 fast-forward 更新
3. 工作区存在 tracked 本地改动时停止，避免覆盖服务器上的手工修改
4. 根据变更文件识别需要更新的容器
5. 成功部署后记录 deploy/.auto-update-main.state
```

一次性检查并更新：

```bash
./scripts/auto-update-main.sh once
```

查看状态：

```bash
./scripts/auto-update-main.sh status
```

修改轮询间隔：

```bash
AUTO_UPDATE_INTERVAL_SECONDS=300 ./scripts/auto-update-main.sh watch
```

后台运行的最简单方式：

```bash
nohup ./scripts/auto-update-main.sh watch >> auto-update-main.log 2>&1 &
```

更正式的服务器部署可以用 systemd 托管这个命令。

当前自动识别规则：

```text
Dockerfile
pom.xml
deploy/docker-compose.yml
deploy/.env.example
deploy/keycloak/*
scripts/deploy.sh
scripts/server-deploy.sh
  -> ./scripts/deploy.sh

platform-auth/pom.xml
platform-auth/auth-core/*
platform-auth/auth-interface/*
platform-auth/auth-web/*
  -> ./scripts/update-module.sh auth-web

docs / README / frontend / 其他未映射路径
  -> 只拉代码，不重启容器
```

注意：当前仓库还没有 GitHub remote 时，自动更新脚本会提示缺少 `origin`，不会执行。

## When To Use Which Command

只改 `platform-auth/auth-web`：

```bash
./scripts/update-module.sh auth-web
```

改了 `platform-auth/auth-core`，并且只影响 auth 后端：

```bash
./scripts/update-module.sh auth-web
```

改了 `deploy/docker-compose.yml`、`Dockerfile`、`.env.example`：

```bash
./scripts/deploy.sh
```

改了 `deploy/keycloak/custacm-realm.json`：

```bash
./scripts/deploy.sh
```

注意：Keycloak 使用数据库持久化。已有 realm 不一定会被 `--import-realm` 覆盖。开发环境如果要强制重新导入 realm，需要清理 Keycloak 数据库 volume；这会删除本地 Keycloak 用户和配置。

## Add A New Business Container

以后新增一个可运行模块，比如 `platform-blog/blog-web`，按这几个点扩展。

1. 在 `deploy/docker-compose.yml` 增加一个 service。

示例：

```yaml
custacm-blog-web:
  build:
    context: ..
    dockerfile: Dockerfile
    args:
      MODULE_PATH: platform-blog/blog-web
      JAR_PATH: platform-blog/blog-web/target/blog-web-0.1.0-SNAPSHOT.jar
      APP_PORT: 8082
  environment:
    BLOG_WEB_PORT: 8082
    KEYCLOAK_ISSUER_URI: ${KEYCLOAK_ISSUER_URI}
    KEYCLOAK_JWK_SET_URI: ${KEYCLOAK_JWK_SET_URI}
  ports:
    - "8082:8082"
  depends_on:
    keycloak:
      condition: service_started
```

2. 在 `scripts/update-module.sh` 的 `case` 里加映射。

示例：

```bash
blog-web|blog|custacm-blog-web)
  MODULE_PATH="platform-blog/blog-web"
  SERVICE_NAME="custacm-blog-web"
  HEALTH_PORT_VAR="BLOG_WEB_PORT"
  HEALTH_PATH="/health"
  ;;
```

3. 在 `scripts/auto-update-main.sh` 的 `classify_changes` 里加路径识别。

示例：

```bash
platform-blog/blog-web/*)
  needs_blog=1
  has_runtime_change=1
  ;;
```

并让分类输出包含新模块，例如：

```bash
echo "modules:blog-web"
```

如果同一个提交可能同时影响多个业务模块，用逗号分隔：

```bash
echo "modules:auth-web,blog-web"
```

4. 在 `deploy/.env.example` 和 `deploy/.env` 增加端口。

示例：

```env
BLOG_WEB_PORT=8082
```

5. 在本文档的 Current Shape 和 Auto Update Main 里补模块映射。

## Useful Commands

查看容器：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
```

看某个模块日志：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f custacm-backend
```

看落盘日志：

```bash
tail -f logs/combined.log
tail -f logs/error.log
```

手动只重启某个服务：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml restart custacm-backend
```

手动无缓存重建某个服务：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml build --no-cache custacm-backend
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --no-deps custacm-backend
```
