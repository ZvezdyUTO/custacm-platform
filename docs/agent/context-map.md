# Context Map

| Path | Current responsibility |
| --- | --- |
| `platform-blog/upstream/nblog/blog-api/` | 唯一 Spring Boot 后端；精简 Blog/评论、十个规范赛事分类与固定奖档、选手自主管理公开状态/顺序的个人获奖记录、参赛用户本人文章绑定、比赛/文章七天回收站与到期清理、首页精选组持久化/管理员编排与 `/site` 的 `featuredGroups` 投影、最多十二张带压缩缩略图的滚动精选图片、按 username 的 Redis 五秒登录冷却、单篇文章图片 ZIP/管理员全量备份与跨文章限流、最小 `/site`、限时且可降级 Redis 内容缓存、托管图片与孤儿清理、认证、资料/个人友链、原子用户/OJ 管理、批量训练查询 adapter 与统一 Flyway runtime；不再包含动态首页横幅、About/Friend/Moment、访问统计、Quartz、通知和旧上传通道 |
| `platform-blog/upstream/nblog/blog-view/` | Vue 3 + Vite 公开 Blog；生产路径 `/`，静态首图渐隐区下以压缩图优先、点击按需加载原图、可暂停、可双向手动滚动、动态多副本换轨和边缘模糊的无尽环展示全部精选图片，首页再按服务端顺序渲染最多三组、每组三篇的一大两小精选卡片，16:9 首图完整显示并带作者信息和彩色文章标签；保留文章、本人回收站/恢复、分类、标签、搜索、规范分类赛事列表/详情、支持本地 Google Noto Emoji/Unicode 的评论和 `/profile` 个人主页，个人主页维护奖项公开偏好/顺序与参赛文章绑定，文章详情桌面端使用独立滚动的左侧作者/目录/评论入口工具栏与右侧阅读画布，作者名片折叠展示前三项公开奖项、向登录用户提供下载并仅向作者提供编辑入口，同时持有 `/training/**` 外壳；About、全站友链与动态页面已删除 |
| `frontend/` | Vue 3 训练运行时；公开路径 `/training/**` 由 Blog 外壳承载，内部产物路径 `/training-app/**`；登录展示服务端五秒冷却，多人统计单批量请求，管理用户单次原子保存，`/training/admin/competitions` 以规范分类和动态奖档维护比赛/参赛用户/奖项与七天回收站，`/training/admin/articles` 内含首页编排/当前文章/回收站子视图，首页图片页只管理最多十二张滚动精选图片，分类/标签独立分页；目录还包含构建两份 Vue 静态产物、代理 `/api/**` 并按环境生成 `/api/image/**` Referer 白名单的唯一 Nginx `frontend` 服务 |
| `platform-training-data/training-data-common/` | OJ-neutral 用户目录 contract、关系化 `training_member`/`oj_handle_binding` JDBC 仓储、按用户/OJ 成功窗口游标、批量 query facade、默认关闭的调度、warehouse 与 purge logic；旧 JSON 表暂留一个迁移窗口 |
| `platform-training-data/training-data-codeforces/` | Codeforces source、ODS、OJ 专属迁移、warehouse SQL 与 adapter |
| `platform-training-data/training-data-atcoder/` | AtCoder source、metadata、ODS、warehouse SQL 与 adapter |
| `platform-common/common-core/` | 公共 SQL task 等后端基础能力 |
| `deploy/` | `blog-db` + `blog-redis` + `blog-api` + `frontend` 的本地/单机 Compose 配置 |
| `scripts/` | `dev.sh` 本机双 Vite/HMR 开发入口、`deploy.sh` 四服务稳定构建入口，以及测试策略、文档同步、设计 token 同步和日志 MCP 安装等辅助工具 |
| `docs/` | 架构、API、鉴权、日志、部署和 agent 上下文文档 |

运行拓扑：一个 Nginx `frontend` 服务托管 Vue 3 Blog `/` 与其训练外壳 `/training/**`，外壳同源嵌入 Vue 3 Training `/training-app/**`；Nginx `/api/**` 转发唯一 Blog API。Blog `Nav.vue` 在页面切换时保持挂载并提供唯一主题开关；两份 Vue 构建共享 `custacm.theme`，首次默认日间，用户选择持久化、跨标签页和同源 frame 同步但不跟随系统主题，夜间统一使用文章目录暖黑色板。Blog 公开 `/competitions` 规范分类赛事档案并在个人主页消费奖项展示偏好/顺序，Training 通过 `/training/admin/competitions` 管理同一聚合。Blog API 在进程内组装训练模块并访问统一 MySQL。业务身份统一为 `username`，比赛参赛关系随改名级联并在删号后保留昵称快照和历史奖项；角色只允许 `ROLE_admin` 与 `ROLE_player`，`root` 是不可删除且不参与 OJ/队员状态的固定系统管理员。
