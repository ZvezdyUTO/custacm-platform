# platform-blog

`platform-blog` 保留 NBlog 的上游目录结构，承载当前唯一后端和门户中的 Blog 页面。

生产环境只有一个名为 `frontend` 的 Nginx 服务和一个对外站点，不是两套独立部署的前端。不过源码中保留两份职责不同的 Vue 3 构建：

| 源码 | 浏览器路径 | 职责 |
| --- | --- | --- |
| `upstream/nblog/blog-view` | `/` | 公开 Blog、个人主页、文章写作与评论，以及训练页面的外层导航 |
| `../frontend` | `/training/**` | 训练查询与管理员工作区；静态运行时内部挂载在 `/training-app/**` |

两份静态产物由 `frontend/Dockerfile` 一次构建并复制进同一个 Nginx 镜像。浏览器 API 统一走 `/api/**`；Nginx 去掉 `/api` 后转发给 Blog API。两份 Vue Router 彼此独立，用户会话共享 `custacm.accessToken` 和 `custacm.user`，日间/暖黑橙深夜主题共享 `custacm.theme` 并在同源 frame 间同步；Blog 顶栏的太阳/月亮拨杆切换时，文章与实时编辑器代码块在浅色/高对比度深色语法主题之间切换，业务图片以 reduced-motion 兼容的短过渡轻度渐暗。

## 模块职责

- `upstream/nblog/blog-api`：唯一 Spring Boot 后端，统一提供 Blog、评论、比赛与获奖记录、首页图片、认证、账号/OJ handle 和训练数据 HTTP API。
- `upstream/nblog/blog-view`：公开 Blog，页面范围为首页、文章、分类、标签、个人主页和写作页；About、Friends、Moments 页面及其后端链路已移除。
- `../frontend`：训练查询与账号、文章、分类/标签、采集和首页图片管理。

Blog API 负责 BCrypt 密码、HS512 JWT、`ROLE_admin`/`ROLE_player`、`sub=username`，并在进程内组装训练模块。管理员账号编辑统一使用 `PUT /admin/users/{username}`：账号字段、改名、角色、密码、完整 handle 集合和采集状态在同一事务内更新；移除或更换 handle 前先清理对应 OJ 数据。

比赛记录由 Blog API 作为独立聚合管理。公开端按闭区间年份和可选类型标签分页查询比赛及详情；管理员只添加或删除比赛、参赛用户和奖项，不提供普通编辑入口。比赛可同时拥有多个类型标签，根记录明确 `INDIVIDUAL`、`TEAM` 或 `MIXED`，奖项再独立记录个人/团队形态、1–4 级、`(x/y)` 排名和可空的省/国范围（专项赛会强制范围）；个人奖绑定一人，团队奖绑定至少两人。

训练数据仍由 `platform-training-data` 实现采集、ODS/DWD/DWM/DWS、查询、刷新与清理。多人统计通过一次 `GET /player/training-data/accepted-summaries` 批量查询，避免按用户逐个请求。自动提交采集及 AtCoder 题目元数据 bootstrap/调度均默认关闭，只有显式配置后才运行。

当前运行范围不再包含旧 NBlog 的 About/Friends/Moments、访问/登录/操作/异常统计后台、Quartz 任务后台、邮件/Telegram 通知、QQ 资料补全或 GitHub/又拍云上传通道。图片只通过本地托管资产和首页横幅服务管理；Redis 用于有 TTL 的可降级内容缓存、失败登录的五秒冷却，以及普通用户文章下载的 30 秒原子限流窗口。

## 边界规则

- Blog API 是唯一 HTTP 层；训练模块不定义 Spring MVC Controller、认证或账号管理。
- 登录按 username 共享五秒服务端冷却；首次错误响应携带 `Retry-After`，窗口内重复请求返回 429，正确凭据不保留窗口。
- 公开 Blog 请求不得全局附加 JWT；受保护请求由各 API adapter 显式发送 Bearer token。
- 普通用户只能管理本人文章；管理员可管理全部文章，但新建文章作者始终来自当前认证身份。
- 已登录用户可把已发布文章下载为包含 Markdown 与本地托管图片的 ZIP；普通用户跨全部文章共享 30 秒窗口，管理员不限频，游客和草稿均不可下载。管理员文章页还可导出包含所有文章状态、评论、作者资料、文章图片和作者头像的全量 ZIP。
- 作者或管理员删除文章都只会移入固定保留七天的回收站；到期前作者本人或管理员可恢复，关联评论、标签和图片不提前清理。
- 管理员删除比赛也只写入七天回收站标记；期间参赛用户、奖项、获奖人和文章绑定全部保留，公开查询与个人获奖记录立即隐藏，恢复后重新出现，到期后才级联物理清理。同名比赛可在旧记录位于回收站时重新创建，旧记录恢复时若已有同名正常比赛则拒绝恢复。
- 参赛用户只能把本人公开且已发布的文章绑定到比赛，也只能解绑自己的绑定；文章改为草稿、内部文章或进入回收站后从公开比赛响应隐藏，重新公开或恢复后自动重现。
- `GET /player/me` 以及本人头像/资料更新响应包含全部本人 `achievements` 和 `profileVisible`，公开 `GET /profiles/{username}` 只显示选手主动开启的奖项。新奖项默认隐藏，团队成员各自设置；比赛关系以 `username` 为身份并随改名级联，删除账号后使用昵称快照保留历史参赛和获奖记录。
- `root` 是受保护的固定系统管理员，不允许删除、改名、降权或绑定 OJ handle。
- 文章首图、正文图片、头像和首页横幅使用本地托管文件；数据库事务提交后再回收失效文件，并由清理任务兜底。
- 评论选择器插入标准 Unicode，页面使用本地 Google Noto Emoji SVG sprite 渲染，不依赖表情 CDN；历史短码继续兼容读取。
- 当前 UI 验收范围是 1280–2560 px 桌面端，重点分辨率为 1440×900 与 1920×1080。

## 目录结构

```text
platform-blog/
  upstream/nblog/
    blog-api/   唯一 Spring Boot 后端
    blog-view/  门户中的公开 Vue Blog
```

## 文件与路径职责

| 文件/路径 | 职责 |
| --- | --- |
| `upstream/nblog/blog-api/pom.xml` | 后端依赖、测试与 Spring Boot 打包；排除不再使用的旧 IP 库资源 |
| `upstream/nblog/blog-api/src/main/java/top/naccl/BlogApiApplication.java` | 唯一后端启动入口 |
| `upstream/nblog/blog-api/src/main/java/top/naccl/config/` | 安全、JWT、训练模块组装和 bootstrap 管理员配置 |
| `upstream/nblog/blog-api/src/main/java/top/naccl/controller/` | 公开、player 和 admin HTTP adapter |
| `upstream/nblog/blog-api/src/main/java/top/naccl/service/LoginAttemptLimiter.java` | 登录校验前的 Redis 五秒占位、成功释放与失败关闭 |
| `upstream/nblog/blog-api/src/main/java/top/naccl/service/impl/AdminUserService.java` | 管理员用户原子更新、`root` 保护与 OJ 数据清理编排 |
| `upstream/nblog/blog-api/src/main/java/top/naccl/service/ArticleDownloadService.java`、`ArticleArchiveService.java` | 已发布文章下载限流、标题/简介/正文与扁平图片归档、管理员全量备份编排 |
| `upstream/nblog/blog-api/src/main/java/top/naccl/service/ArticleRecycleBinService.java` | 七天软删除、本人/管理员恢复与到期事务清理 |
| `upstream/nblog/blog-api/src/main/java/top/naccl/controller/CompetitionController.java`、`controller/admin/CompetitionAdminController.java`、`controller/player/PlayerCompetitionController.java` | 公开比赛查询、管理员添加/删除/回收站操作、参赛用户本人文章绑定与名片奖项展示偏好 |
| `upstream/nblog/blog-api/src/main/java/top/naccl/service/CompetitionService.java` | 比赛聚合校验、获奖记录装配、七天回收站与文章可见性 |
| `upstream/nblog/blog-api/src/main/resources/db/migration/V037__create_competition_records.sql` | 比赛、类型、参赛用户、奖项/获奖人和文章关联 schema |
| `upstream/nblog/blog-api/src/main/resources/db/migration/` | Blog 与训练 schema 的统一 Flyway 迁移 |
| `upstream/nblog/blog-view/src/router/index.js` | Blog 路由、训练外壳路由和登录转交 |
| `upstream/nblog/blog-view/src/views/Index.vue` | Blog 三栏外壳；桌面左右侧栏在顶栏下吸附并在页脚前结束 |
| `upstream/nblog/blog-view/src/views/profile/Profile.vue` | 个人主页、OJ handle 展示、资料/密码/友情链接编辑和本人文章/回收站入口 |
| `upstream/nblog/blog-view/src/views/article/ArticleEditor.vue` | Markdown 发布/编辑与托管图片上传 |
| `upstream/nblog/blog-view/src/assets/css/typo.css`、`src/components/article/LiveMarkdownEditor.vue`、`src/plugins/articleImagePreview.js` | 文章阅读区与实时编辑器的代码主题、视觉行光标移动及正文图片原子预览/高度复测 |
| `upstream/nblog/blog-view/src/views/blog/Blog.vue` | 文章详情、登录用户下载按钮与错误反馈 |
| `upstream/nblog/blog-view/src/components/comment/CommentForm.vue`、`src/plugins/notoEmoji.js` | 本地 Noto emoji 选择、Unicode 插入与评论展示映射 |
| `upstream/nblog/blog-view/src/auth/session.js` | 两份 Vue 构建共享的登录摘要/JWT 管理 |
| `upstream/nblog/blog-view/src/plugins/axios.js` | `/api/` 同源 Blog client；不得全局附加 JWT |
| `upstream/nblog/blog-view/README.md` | Blog 前端边界、文件职责与验证命令 |

更细的实现与验证说明见两个子模块 README 和 `../frontend/README.md`。
