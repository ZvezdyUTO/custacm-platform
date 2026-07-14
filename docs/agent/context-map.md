# Context Map

| Path | Current responsibility |
| --- | --- |
| `platform-blog/upstream/nblog/blog-api/` | 唯一 Spring Boot 后端；精简 Blog/评论、公开比赛与选手自主管理展示的个人获奖记录、参赛用户本人文章绑定、比赛/文章七天回收站与到期清理、按 username 的 Redis 五秒登录冷却、单篇文章图片 ZIP/管理员全量备份与跨文章限流、最小 `/site`、限时且可降级 Redis 内容缓存、托管图片与孤儿清理、首页图片、认证、资料/个人友链、原子用户/OJ 管理、批量训练查询 adapter 与统一 Flyway runtime；不再包含 About/Friend/Moment、访问统计、Quartz、通知和旧上传通道 |
| `platform-blog/upstream/nblog/blog-view/` | Vue 3 + Vite 公开 Blog；生产路径 `/`，保留首页、文章、登录用户文章图片归档下载、本人回收站/恢复、分类、标签、搜索、支持本地 Google Noto Emoji/Unicode 的评论和 `/profile` 个人主页，桌面左右侧栏在顶栏下吸附，并持有 `/training/**` 外壳；About、全站友链与动态页面已删除 |
| `frontend/` | Vue 3 训练运行时；公开路径 `/training/**` 由 Blog 外壳承载，内部产物路径 `/training-app/**`；登录展示服务端五秒冷却，多人统计单批量请求，管理用户单次原子保存，管理员文章回收站/恢复，分类/标签独立分页；目录还包含构建两份 Vue 静态产物的唯一 Nginx `frontend` 服务 |
| `platform-training-data/training-data-common/` | OJ-neutral 用户目录 contract、关系化 `training_member`/`oj_handle_binding` JDBC 仓储、按用户/OJ 成功窗口游标、批量 query facade、默认关闭的调度、warehouse 与 purge logic；旧 JSON 表暂留一个迁移窗口 |
| `platform-training-data/training-data-codeforces/` | Codeforces source、ODS、OJ 专属迁移、warehouse SQL 与 adapter |
| `platform-training-data/training-data-atcoder/` | AtCoder source、metadata、ODS、warehouse SQL 与 adapter |
| `platform-common/common-core/` | 公共 SQL task 等后端基础能力 |
| `deploy/` | `blog-db` + `blog-redis` + `blog-api` + `frontend` 的本地/单机 Compose 配置 |
| `scripts/` | `dev.sh` 本机双 Vite/HMR 开发入口、`deploy.sh` 四服务稳定构建入口，以及测试策略、文档同步和日志 MCP 安装等辅助工具 |
| `docs/` | 架构、API、鉴权、日志、部署和 agent 上下文文档 |

运行拓扑：一个 Nginx `frontend` 服务托管 Vue 3 Blog `/` 与其训练外壳 `/training/**`，外壳同源嵌入 Vue 3 Training `/training-app/**`；Nginx `/api/**` 转发唯一 Blog API。Blog `Nav.vue` 在页面切换时保持挂载；两份 Vue 构建通过 `custacm.theme`、存储事件与受校验的同源消息保持日间/暖黑橙深夜主题一致，Blog 文章及实时编辑器代码块同步切换浅色/高对比度深色语法主题，太阳/月亮拨杆切换时业务图片同步轻度渐暗。比赛功能本期只增加 Blog API 合同，不新增前端路由。Blog API 在进程内组装训练模块并访问统一 MySQL。业务身份统一为 `username`，比赛参赛关系随改名级联并在删号后保留昵称快照和历史奖项；角色只允许 `ROLE_admin` 与 `ROLE_player`，`root` 是不可删除且不参与 OJ/队员状态的固定系统管理员。
