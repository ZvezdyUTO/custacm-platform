# 更新本地/单机集成栈

## 2026-07-14 发布说明

本次更新同时调整 Vue Blog、Vue 3 训练中心、Blog API、训练数据模块和 Flyway schema。新旧前后端接口不完全兼容，必须使用完整更新入口同步发布，不能只替换前端或后端。发布前应备份 MySQL 数据库和 `uploads/`，发布范围以最终交付提交为准。

### 1. 功能发布

- 训练查询支持按需显示或选择退役队员；多人统计改为单次批量查询，缺失结果会明确提示，不再伪装为零数据。
- 管理员通过一个保存动作原子更新账号、角色、密码、现役状态和完整 OJ handle 集合；更换或解绑 handle 时，同一事务会清理对应 OJ 的历史训练数据并重置采集状态。
- 登录用户可下载包含标题、简介、Markdown 正文和本地托管图片的文章 ZIP。普通用户跨全部文章共享 30 秒下载窗口，管理员不限频。
- 管理员可下载覆盖公开文章、内部文章、草稿、回收站文章、去敏评论、作者资料和托管图片的全量备份。
- 文章删除改为固定七天回收站：删除后立即从正常读取中隐藏，七天内作者本人或管理员可恢复，到期任务才物理清理文章、评论、标签关系和托管图片。
- Blog 与训练中心固定使用共享浅色语义 token；管理文章页增加“首页编排”/当前文章/回收站切换。首页最多三组、每组固定三篇且全首页文章不重复，管理员可编辑组标题、选择公开文章和调整组顺序；分类、标签、首页图片和批量创建用户使用统一确认框。
- 公开 Blog 收敛为首页、文章、分类、标签、搜索、个人主页与评论；About、全站友链、动态、游客评论身份链路、阅读数、旧统计日志后台、Quartz 管理及旧通知/上传能力不再兼容。

### 2. 前端发布

- 生产环境仍只有一个 `frontend` Nginx 服务：Vue Blog 位于 `/`，训练中心位于 `/training/**`，浏览器 API 统一使用 `/api/**`。
- 两份 Vue 构建继续共享 `custacm.accessToken`、`custacm.user`；不再读取或写入 `custacm.theme`，公开 Blog 请求不会全局附加 JWT。
- 发布前分别完成训练中心的 lint、测试、类型检查和生产构建，以及 Vue Blog 的测试和生产构建，具体命令见下方“更新前检查”。
- 发布后重点验收 `/` 的多组精选布局、16:9 首图完整显示和作者信息，`/articles` 的固定深色目录，`/training/admin/articles` 的首页编排/当前文章/回收站切换，以及 `/profile`、文章详情、`/training/multiple`、管理员用户页面、跨应用登录连续性、文章下载、回收站恢复和两套 history fallback。
- 正式验收范围为 1280～2560 px 桌面端，重点检查 1440×900 与 1920×1080；移动端不属于当前发布范围。

### 3. 后端发布

- Blog API 继续是唯一 Spring Boot 后端；Redis 内容缓存统一使用 `BLOG_CACHE_TTL`，默认 `10m`，内容缓存故障时回退数据库。文章下载限流不可降级，普通用户无法检查限流状态时返回 503。
- `V033` 移除文章阅读数，`V034` 将旧 OJ JSON 账号规范化为 `training_member` 与 `oj_handle_binding`，`V035` 删除已退役功能的独占表，`V036` 增加文章回收站字段和索引，`V037` 增加比赛与获奖记录聚合，`V038` 增加 `homepage_featured_group` 与 `homepage_featured_group_article`。
- V038 升级回填只按旧 `is_top desc, is_recommend desc, update_time desc, id desc` 选取当前公开、已发布且未回收的前三篇；满三篇才创建默认“精选文章”组，不足三篇保持无组。升级后 `is_recommend` 仅保留历史存储/备份兼容，不再影响首页，旧 `/admin/blog/recommend` 不再可用。
- 升级前必须完成下方 V034 JSON/重复 handle 只读预检，两个查询均返回 0 行后才能启动新后端。发布本身不会重建 ODS/DWD/DWM/DWS；只有管理员后续更换或解绑 handle 时才会清理对应用户、对应 OJ 的训练数据。
- 在 `deploy/.env` 中补充 `BLOG_CACHE_TTL`、`BLOG_DAILY_COLLECTION_LOOKBACK=100h`、`BLOG_INTRADAY_COLLECTION_LOOKBACK=0h` 以及 Codeforces/AtCoder 自动采集开关。旧 `.env` 未补回看变量时 Compose 也会注入新默认；自动采集、AtCoder 题目定时采集和启动补采默认全部关闭，管理员手动采集不受影响。
- 完整更新不会改写服务器已有的 `deploy/.env`，也不会清空数据库中的 `last_collected_at`。缺少新增回看变量时只替换旧镜像内的 120 小时/1 小时默认值；下一次自动任务会基于原有成功游标按 100 小时/0 小时计算窗口。
- Flyway 成功迁移到 V038 后，检查四个 Compose 服务、登录与 player/admin 权限、V038 回填结果、`GET /site` 的 `featuredGroups`、后台首页编排、文章下载 429/503、回收站恢复、托管图片读写和应用日志。
- `V035` 会删除旧功能表，不能只回滚应用镜像。需要回滚时应停止写入，恢复发布前的 MySQL 与 `uploads/` 备份，再切回与备份匹配的前后端版本。

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

6. 从使用旧 `oj_handle_account` 的版本升级时，先备份数据库并执行只读预检：

   ```sql
   SELECT username
   FROM oj_handle_account
   WHERE NOT JSON_VALID(handles_json)
      OR (collection_states_json IS NOT NULL
          AND collection_states_json <> ''
          AND NOT JSON_VALID(collection_states_json));

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

   两个查询都必须返回 0 行。重复判断大小写精确，与 V034 新表约束一致。发现脏 JSON 或重复绑定时，先由项目负责人确认归属并修正旧表，不要直接忽略记录。

只执行与本次变更相符的构建门禁；纯文档变更不需要 Maven 或前端构建。

如果 V034 因历史脏数据失败：保持 `blog-api` 停止，保留数据库备份，修正旧表后使用部署环境批准的 Flyway repair 流程清除失败记录，再重新启动。V034 每次重跑会先删除它自己创建的两个新表、重新完成临时表校验并重建，不会删除旧 `oj_handle_account`。TODO：仓库目前没有封装生产凭据的自动 repair 命令，不能在通用脚本中猜测生产连接参数。

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
