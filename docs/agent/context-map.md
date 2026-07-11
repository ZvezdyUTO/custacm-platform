# Context Map

| Path | Current responsibility |
| --- | --- |
| `platform-blog/upstream/nblog/blog-api/` | 唯一 Spring Boot 后端；Blog、首页图片、认证、用户资料/个人友链、用户/OJ handle、训练 HTTP adapter 与统一 Flyway runtime |
| `platform-blog/upstream/nblog/blog-view/` | Vue 3 + Vite 公开 Blog；生产路径 `/`，左侧纯展示个人名片/签名/友情链接，右侧编辑本人资料并提供头像裁剪，为受保护评论和本人资料更新显式使用 JWT |
| `platform-blog/upstream/nblog/blog-cms/` | 保留的 NBlog 上游管理端源码；当前不构建、不部署 |
| `frontend/` | Vue 3 训练运行时；公开路径 `/training/**` 由 Blog 外壳承载，内部产物路径 `/training-app/**`；查询页与创建用户、管理用户、数据采集、首页图片四个管理员页面；同时拥有双前端 Nginx/Docker 构建 |
| `platform-training-data/training-data-common/` | OJ-neutral 用户目录 contract、查询、job、调度、warehouse 与 purge logic |
| `platform-training-data/training-data-codeforces/` | Codeforces source、ODS、warehouse SQL 与 adapter |
| `platform-training-data/training-data-atcoder/` | AtCoder source、metadata、ODS、warehouse SQL 与 adapter |
| `platform-common/common-core/` | 公共 SQL task 等后端基础能力 |
| `platform-common/common-web/` | 空的未来 Web placeholder，不是运行服务 |
| `deploy/` | `blog-db` + `blog-redis` + `blog-api` + `frontend` 的本地/单机 Compose 配置 |
| `docs/` | 架构、API、鉴权、日志、部署和 agent 上下文文档 |

运行拓扑：Vue 3 Blog `/` 与其训练外壳 `/training/**` → 同源 Vue 3 Training `/training-app/**` → Nginx `/api/**` → Blog API。Blog `Nav.vue` 在页面切换时保持挂载；Blog API 在进程内组装训练模块并访问统一 MySQL。业务身份统一为 `username`，角色只允许 `ROLE_admin` 与 `ROLE_player`。
