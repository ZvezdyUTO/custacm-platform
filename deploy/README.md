# 本地与单机 Compose 部署

本目录是 custacm-platform 唯一的部署入口，适用于本地开发、验收和单台主机运行。仓库没有记录任何已经完成的服务器发布；只有实际执行并验证后才能声明部署成功。

## 服务拓扑

`docker-compose.yml` 固定包含四个服务：

| Service | 作用 |
| --- | --- |
| `blog-db` | MySQL 8.4，保存 Blog 与训练数据 |
| `blog-redis` | Redis 7，提供缓存、登录冷却和运行支持 |
| `blog-api` | 唯一 Spring Boot 后端，容器端口 8090 |
| `frontend` | 一个 Nginx，同时提供 Blog、Training 和 `/api/**` 网关 |

Nginx 路由：

```text
/api/image/**     -> 只读 uploads 挂载
/api/**           -> blog-api:8090/**，去掉 /api
/training         -> 302 /training/multiple
/training/**      -> Vue Blog history fallback 和 Training 外壳
/training-app/**  -> Vue Training 内部 history fallback
/**               -> Vue Blog history fallback
```

## 准备环境

从仓库根目录执行：

```bash
cp deploy/.env.example deploy/.env
# 替换所有 change-me 值，并选择未占用的端口
docker compose --env-file deploy/.env -f deploy/docker-compose.yml config
```

`deploy/.env` 不得提交。变量分为以下几组：

| 范围 | 变量 |
| --- | --- |
| Host 端口 | `BACKEND_PORT`、`FRONTEND_PORT`、`FRONTEND_HTTPS_PORT` |
| TLS | `TLS_ENABLED`、`TLS_CERT_DIR` |
| MySQL | `BLOG_DB_NAME`、`BLOG_DB_USERNAME`、`BLOG_DB_PASSWORD`、`BLOG_DB_ROOT_PASSWORD` |
| 持久化卷 | `BLOG_DB_VOLUME_NAME`、`BLOG_REDIS_VOLUME_NAME` |
| 认证 | `BLOG_TOKEN_SECRET`、`BLOG_TOKEN_TTL_MILLIS`、`BLOG_BOOTSTRAP_ADMIN_PASSWORD` |
| 缓存与采集 | `BLOG_CACHE_TTL`、两个 `BLOG_*_LOOKBACK` 和各项 `*_ENABLED` 开关 |

`.env.example` 是当前变量和默认值的事实来源。自动提交采集、AtCoder 题目定时采集和启动补采默认全部关闭；确认外部 API 配额、带宽和数据库负载后才可逐项开启。每日自动采集默认回看 `100h`，日内任务默认从上次成功游标直接续爬（`0h`）。

`BLOG_TOKEN_SECRET` 必须使用非 placeholder 的值，并至少包含 64 个 UTF-8 字节；不满足时 Blog API 会拒绝启动。

配置 `BLOG_BOOTSTRAP_ADMIN_PASSWORD` 后，应用会幂等创建固定用户名 `root` 的首个管理员。首次登录后应立即通过受保护 API 修改初始密码。

## 两种运行模式

### 前端开发

```bash
./scripts/dev.sh
```

该脚本：

- 停止生产 Nginx `frontend`；
- 保留并启动 Docker 中的 MySQL、Redis 和 Blog API，不强制重建后端镜像；
- 在宿主机启动 Training Vite 5173 和 Blog Vite 4180；
- Ctrl-C 时只停止两份 Vite，后端容器继续运行。

两份 Vite 都代理 `localhost:8090`，所以开发模式要求 `BACKEND_PORT=8090`。

### 稳定运行、验收或服务器更新

```bash
./scripts/deploy.sh
```

该脚本校验配置，构建并启动完整四服务栈，然后检查 Blog、Training、后端 health 和网关 health。它不会执行 Git 拉取、切换分支或删除数据卷。首次部署和后续更新使用同一个入口；需要其他环境文件时，将路径作为唯一参数：

```bash
./scripts/deploy.sh /path/to/environment.env
```

源码更新必须由操作者在运行脚本前显式完成。仓库不提供模块级更新、自动拉取或第三种启动入口。

## 默认地址

使用 `.env.example` 默认端口时：

| URL | 作用 |
| --- | --- |
| `http://localhost:3000/` | Vue Blog |
| `http://localhost:3000/training/multiple` | Vue Training |
| `http://localhost:3000/api/health` | 经 Nginx 访问 Blog API |
| `http://localhost:8090/health` | 直接访问 Blog API |

开发模式入口为 `http://localhost:4180/` 和 `http://localhost:4180/training/multiple`。

## HTTPS

将 PEM 证书和私钥分别保存为：

```text
TLS_CERT_DIR/origin.pem
TLS_CERT_DIR/origin.key
```

然后设置：

```text
TLS_ENABLED=true
FRONTEND_HTTPS_PORT=443
```

证书目录只读挂载到 Nginx；私钥不得放入仓库、镜像、`.env` 或日志。启用 TLS 后，HTTP 入口会重定向到 HTTPS。Cloudflare Origin CA 场景应在代理侧使用 Full (strict)；本机 `curl -k` 只用于诊断，不代表浏览器可以忽略证书校验。

## 从 V024–V033 的旧账号表升级

仅当 Flyway history 已成功执行 V024、尚未成功执行 V034，且数据库仍使用 `oj_handle_account` JSON 存储时执行本节。V018–V023 的列名和字段集合不同，不能运行下面的 SQL，也不能跳过预检直接升级；应先使用该环境批准的分阶段 Flyway 流程停在 V024–V033，再执行本节。仓库不提供带生产连接参数的通用迁移命令。

升级前必须：

1. 停止业务写入。
2. 使用部署环境认可的方式备份完整 MySQL 数据库和 `uploads/`。
3. 在旧库上执行以下三个只读预检。

检查损坏的 JSON：

```sql
SELECT username
FROM oj_handle_account
WHERE NOT JSON_VALID(handles_json)
   OR (collection_states_json IS NOT NULL
       AND collection_states_json <> ''
       AND NOT JSON_VALID(collection_states_json));
```

检查同一 OJ 下大小写精确的重复 handle：

```sql
WITH bindings AS (
    SELECT username, 'CODEFORCES' AS oj_name,
           TRIM(JSON_UNQUOTE(JSON_EXTRACT(handles_json, '$.CODEFORCES'))) AS handle
    FROM oj_handle_account
    UNION ALL
    SELECT username, 'ATCODER' AS oj_name,
           TRIM(JSON_UNQUOTE(JSON_EXTRACT(handles_json, '$.ATCODER'))) AS handle
    FROM oj_handle_account
)
SELECT oj_name, handle, COUNT(*) AS binding_count, GROUP_CONCAT(username) AS usernames
FROM bindings
WHERE handle IS NOT NULL AND handle <> '' AND handle <> 'null'
GROUP BY oj_name, BINARY handle
HAVING COUNT(*) > 1;
```

检查固定管理员是否错误进入旧训练账号表：

```sql
SELECT username
FROM oj_handle_account
WHERE username = 'root';
```

三个查询都必须返回 0 行。发现损坏 JSON 或重复绑定时，由项目负责人确认 handle 归属并修正旧表；发现 `root` 行时停止升级，由项目负责人确认旧训练数据的清理方案后再移除该行。不得忽略记录、让 `root` 迁入训练成员表，或放宽大小写精确的唯一约束。

V034 会把数据迁移到 `training_member` 与 `oj_handle_binding`。生产代码迁移后只读写新表；旧 `oj_handle_account` 仍在 schema 中，删除它需要新的 migration 和运营确认。该迁移不会自动重建 ODS/DWD/DWM/DWS；只有管理员后续更换或解绑 handle 时，才清理对应用户和 OJ 的训练数据。

如果 V034 因历史脏数据失败：

1. 保持 `blog-api` 停止并保留升级前备份。
2. 修正旧 `oj_handle_account`，重新执行三个预检。
3. 使用该部署环境批准的 Flyway repair 流程清除失败记录，再重新启动。

V034 重跑时会删除并重建它自己创建的两个新表，不会删除旧 `oj_handle_account`。仓库不提供带生产连接参数的通用 repair 命令，不得在脚本或文档中猜测生产凭据。

后续 Flyway 版本会删除部分已退役功能表，因此完整回滚不能只切换旧应用镜像。需要回滚时，应停止写入，恢复升级前的 MySQL 与 `uploads/` 备份，再启动与该备份匹配的前后端版本。

## 升级到 V043–V049 分类与标签完整性约束

只要现有数据库的 V043–V049 任一尚未成功执行，就执行本节。部署新版本、让 `blog-api` 启动并自动迁移前，先停止业务写入、备份完整 MySQL，并执行以下全部只读预检：

```sql
SELECT category_name, COUNT(*) AS duplicate_count,
       GROUP_CONCAT(id ORDER BY id) AS category_ids
FROM category
GROUP BY category_name
HAVING COUNT(*) > 1;

SELECT tag_name, COUNT(*) AS duplicate_count,
       GROUP_CONCAT(id ORDER BY id) AS tag_ids
FROM tag
GROUP BY tag_name
HAVING COUNT(*) > 1;

SELECT blog_id, tag_id, COUNT(*) AS duplicate_count
FROM blog_tag
GROUP BY blog_id, tag_id
HAVING COUNT(*) > 1;

SELECT b.id AS blog_id, b.category_id
FROM blog AS b
LEFT JOIN category AS c ON c.id = b.category_id
WHERE c.id IS NULL;

SELECT bt.blog_id, bt.tag_id
FROM blog_tag AS bt
LEFT JOIN blog AS b ON b.id = bt.blog_id
WHERE b.id IS NULL;

SELECT bt.blog_id, bt.tag_id
FROM blog_tag AS bt
LEFT JOIN tag AS t ON t.id = bt.tag_id
WHERE t.id IS NULL;
```

六个查询都必须返回 0 行。若发现重名、重复关联或悬空引用，应先由项目负责人确认业务归属，并在备份后修正引用或删除无效关联；修正后重新执行全部六个查询，全部为 0 行才可启动新版。迁移不会自动合并或删除业务数据。V043/V044 增加分类名和标签名唯一约束，V045/V046 为 `blog_tag` 增加复合主键和反向查询索引，V047–V049 为文章分类及文章标签关联增加外键。每个 Flyway migration 只包含一个 MySQL DDL，避免非事务 DDL 在同一版本内出现部分成功。

## 构建说明

- 后端镜像从根 Maven reactor 打包 `platform-blog/upstream/nblog/blog-api`。
- 前端镜像使用 Node 20.19：Training 通过已跟踪的 pnpm lock 构建；Blog 按当前 Dockerfile 使用 `npm install`；两份产物复制到 Nginx 1.27 Alpine。
- Blog 静态产物位于 Nginx root，Training 静态产物位于内部 `/training-app`。

## 验证

`deploy.sh` 已检查以下入口：

```text
Backend:  http://localhost:${BACKEND_PORT}/health
Blog:     ${frontend_scheme}://localhost:${frontend_port}/
Training: ${frontend_scheme}://localhost:${frontend_port}/training/multiple
Gateway:  ${frontend_scheme}://localhost:${frontend_port}/api/health
```

手工诊断时，Compose 的 `--env-file` 不会设置当前 shell，需显式加载同一份配置：

```bash
set -a
. deploy/.env
set +a

docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
curl -fsS "http://localhost:${BACKEND_PORT}/health"
```

随后按 `TLS_ENABLED` 使用实际 HTTP 或 HTTPS 端口检查 `/`、`/training/multiple` 和 `/api/health`。浏览器还应验证刷新 fallback、登录/退出、player/admin 权限隔离、跨前端会话以及控制台无关键错误。

## 数据与安全

- MySQL 与 Redis 使用 `BLOG_DB_VOLUME_NAME`、`BLOG_REDIS_VOLUME_NAME` 指定的命名卷；容器重建不会自动删除数据。
- 普通部署、更新或排障不得执行 `docker compose down --volumes`。
- 新卷与旧部署卷相互独立；没有明确批准时不得自动引用、迁移或删除旧卷。
- 应用日志挂载到仓库 `logs/`。`uploads/` 对 Blog API 为读写、对 Nginx 为只读；备份和恢复必须同时覆盖数据库与上传目录。
- 不要公开 MySQL 或 Redis 端口。
- 不得打印或提交数据库密码、JWT secret、bootstrap 密码、token、Cookie、Authorization header 或 TLS 私钥。
- `.env.example` 的 placeholder 不得用于共享或生产环境。
