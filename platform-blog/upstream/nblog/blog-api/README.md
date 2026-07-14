# blog-api

`blog-api` 是项目唯一可运行的 Spring Boot 后端。Java 21、Spring Boot 3.5、MyBatis/JDBC、Flyway、MySQL、Redis、BCrypt 和 HS512 JWT 在一个进程中运行；训练模块以 library 方式进程内组装。

浏览器通过 Nginx 的 `/api/**` 访问，网关会去掉 `/api`；直接访问本服务时使用 `/login`、`/player/**`、`/admin/**` 等原始路径。

## 模块职责

- 提供文章 ZIP 下载、管理员全量文章备份、分类、标签、公开/内部评论、公开比赛与获奖记录、公开作者资料、首页滚动精选图片和首页精选分组 API。
- 普通用户只能管理本人文章；管理员可管理全部文章，新建文章作者固定为当前认证账号。
- 作者或管理员删除文章都只进入固定七天回收站；到期前仅作者本人或管理员可恢复，到期后才物理清理关联内容。
- 统一负责 BCrypt 密码、JWT、账号、角色、个人资料、OJ handle 和训练用户身份。
- 在 `top.naccl` 下提供训练查询、采集任务和数仓刷新 HTTP adapter；训练模块本身不依赖 Spring MVC。
- 使用一个 `DataSource`、事务管理器和 Flyway history 管理 Blog 与训练 schema。
- 使用本地托管图片资产保存头像、文章首图/正文图片和首页滚动精选图片，并在事务提交后清理失效文件。
- Redis 承载有统一 TTL 的可降级缓存、失败登录的五秒冷却，以及普通用户文章下载的 30 秒原子限流窗口；缓存失败回退数据库，安全窗口状态不可用时对应操作失败关闭并返回 503。
- 管理员以只添加/删除的方式维护比赛、参赛用户和奖项；参赛用户可把本人公开且已发布文章绑定到比赛，个人名片响应同时装配公开可见的获奖记录。

当前范围已移除 About/Friends/Moments、访问/登录/操作/异常日志统计后台、旧访客统计、Quartz 任务管理、邮件/Telegram 评论通知、QQ 资料查询、动态首页横幅和 GitHub/又拍云上传。`V035__drop_retired_nblog_features.sql` 删除旧页面/运行时表，`V040__retire_homepage_banners_and_add_featured_thumbnails.sql` 删除旧横幅表；不要恢复相关 Controller、Service、Mapper、实体、配置或依赖。

## 登录冷却

`POST /login` 在校验凭据前按规范化 username 写入 Redis 五秒原子占位。正确凭据删除占位并签发 JWT；用户名或密码错误保留占位，首次响应为 HTTP 401、`AUTH_BAD_CREDENTIALS` 与 `Retry-After: 5`，窗口内再次请求返回 HTTP 429、`AUTH_LOGIN_COOLDOWN` 和剩余 `Retry-After`。Redis 无法完成占位或释放时返回 HTTP 503 与 `AUTH_LOGIN_COOLDOWN_UNAVAILABLE`，不绕过保护。Redis key 只保存 username 的 SHA-256，不写入日志或响应。

## 账号与 OJ handle

管理员用户接口为：

```text
GET    /admin/users
POST   /admin/users:batch-create
PUT    /admin/users/{username}
DELETE /admin/users/{username}
```

`PUT /admin/users/{username}` 是唯一编辑入口。请求体 `AdminUserUpdateRequest` 同时描述 `newUsername`、nickname、email、role、password、完整 `handles` 集合和 `needCollect`。服务在同一事务内完成账号更新；对被移除或改变的 OJ handle，先清理该用户对应 OJ 的 ODS/DWD/DWM/DWS 数据，再精确替换绑定并重置该 OJ 的采集游标。改名或改密码时响应要求重新登录。

用户列表一次读取全部账号及全部 OJ 绑定，不按用户循环查询。固定 `root` 管理员不能删除、改名、降权、绑定 handle 或设置采集状态，系统始终至少保留一个管理员。

`V034__normalize_oj_handle_accounts.sql` 将旧 JSON 结构展开为：

- `training_member(username, need_collect, ...)`：训练成员状态；
- `oj_handle_binding(username, oj_name, handle, last_collected_at, ...)`：每个用户、每个 OJ 一行，并约束 `(oj_name, handle)` 唯一。

生产代码只读写这两张规范化表。旧 `oj_handle_account` 仅为一次迁移回退窗口保留，本版本不再写入；应由后续独立迁移在确认稳定后删除，不能直接改写 V034。

## 比赛与个人获奖记录

公开接口为 `GET /competitions` 和 `GET /competitions/{id}`。列表接受可选 `startYear`、`endYear`、`category` 以及 `pageNum`/`pageSize`；年份上下界组成包含端点的闭区间，只传一端时按单侧范围过滤，规范分类组合在数据库分页前匹配。比赛根以全称作为正常记录唯一标识，并记录 `INDIVIDUAL`、`TEAM` 或 `MIXED` 参赛形态。

业务创建和筛选只使用以下互斥规范分类；底层类型标签仅为历史存储兼容：

| Category | Label | Participation mode |
| --- | --- | --- |
| `PROVINCIAL` | 省赛 | 请求指定 |
| `ICPC_NATIONAL_INVITATIONAL` | ICPC 全国邀请赛 | `TEAM` |
| `CCPC_NATIONAL_INVITATIONAL` | CCPC 全国邀请赛 | `TEAM` |
| `ICPC_ASIA_REGIONAL` | ICPC 亚洲区域赛 | `TEAM` |
| `CCPC_REGIONAL` | CCPC 区域赛 | `TEAM` |
| `EC_FINAL` | EC-Final | `TEAM` |
| `CCPC_FINAL` | CCPC-Final | `TEAM` |
| `BAIDU_STAR` | 百度之星 | `INDIVIDUAL` |
| `GPLT_NATIONAL` | GPLT 团体程序设计天梯赛（国赛） | `MIXED` |
| `LANQIAO_CUP_NATIONAL` | 蓝桥杯程序设计竞赛（国奖） | `INDIVIDUAL` |

响应保留只读 `types` 供既有数据兼容，但创建请求只接受单个 `category`。固定形态分类的请求必须使用表中形态，省赛可以选择个人、团队或混合。

管理员写入口全部位于 `/admin/competitions`：

```text
POST   /admin/competitions
GET    /admin/competitions/recycle-bin
DELETE /admin/competitions/{id}
PUT    /admin/competitions/{id}/restore
POST   /admin/competitions/{id}/participants
DELETE /admin/competitions/{id}/participants/{participantId}
POST   /admin/competitions/{id}/awards
DELETE /admin/competitions/{id}/awards/{awardId}
```

除回收站恢复外不提供比赛、参赛用户或奖项的 PUT/PATCH；需要更正时删除旧子记录后重新添加。已被奖项引用的参赛用户要先删除相关奖项，再删除参赛记录。创建奖项使用分类限定的 `awardTier`：前七类只接受金、银、铜、优胜奖并要求成对合法的 `rankPosition`/`rankTotal`；百度之星只接受国一至国四、省一至省三；GPLT 与蓝桥杯只接受一至三等奖并要求排名成对为空。响应返回稳定 `awardTier`/`awardTierLabel`，旧等级、范围和名称只为兼容保留。`INDIVIDUAL` 必须且只能绑定一名参赛用户且不能填队名；`TEAM` 至少绑定一名参赛用户、不限制队伍人数，可保存可选队名。

比赛参赛用户通过 `POST /player/competitions/{competitionId}/articles/{blogId}` 和对应 DELETE 绑定或解绑自己的文章。POST 同时校验 JWT username 是该比赛参赛用户、文章作者是本人，而且文章当前已发布且公开；DELETE 可移除本人已存在的绑定。绑定关系不会因为文章后来变成草稿、内部文章或进入回收站而删除；这些文章仅从公开比赛响应隐藏，重新公开或恢复后自动重现。

新获奖关系默认 `profileVisible=false` 且无公开顺序。获奖人使用 `PUT /player/competitions/{competitionId}/awards/{awardId}/profile-visibility` 和 `{"visible":true|false}` 修改本人名片展示偏好，开启时追加到本人公开序列末尾；使用 `PUT /player/competitions/achievement-order` 和完整 `orderedAwardIds` 重排本人全部公开项目。团队成员独立设置，管理员也不能代替其他人修改。该偏好只影响资料名片，不影响比赛详情中的获奖人列表。

`GET /player/me`、`PATCH /player/me/profile`、`PUT /player/me/profile-links` 和 `POST /player/me/avatar` 的本人资料响应包含全部 `achievements` 及 `profileVisible`/`profileOrder`；公开 `GET /profiles/{username}` 只按保存顺序返回开启展示的项目。每项获奖记录带比赛全称/年份/规范分类、奖项形态、固定奖档以及奖牌类可用排名。参赛关系以 `username` 外键关联账号并在改名时级联；删除账号时 username 置空，但昵称快照、参赛记录和历史奖项继续保留。

管理员删除比赛只在比赛根写入固定七天回收站标记；公开列表、详情、文章绑定目标和个人 `achievements` 立即隐藏，参赛用户、奖项、获奖人和文章关联不变，管理员回收站树仍装配当前公开已发布的已绑定文章。七天内恢复后聚合在公开端全部重现，到期才由清理任务级联物理删除。同名正常比赛保持唯一，但回收站记录不占用名称，因此可在保留期内创建同名新比赛；旧比赛恢复时若已有同名正常比赛则拒绝恢复。

## 查询与内容性能

- `GET /player/training-data/users` 默认只返回现役且至少绑定一个 OJ 的用户摘要；`includeRetired=true` 时包含退役用户。
- `GET /player/training-data/accepted-summaries` 按 OJ、日期和难度一次批量返回所有匹配队员的 AC 汇总，底层使用单条 handle 集合查询；多人页面不得逐用户调用单人接口。
- 单人汇总仍使用 `GET /player/training-data/accepted-summary`；提交与首 AC 查询继续按用户或题目分页。
- 首页/分类/标签文章列表使用一次批量标签查询，不按文章逐个查询标签。
- 公开评论按根评论分页，再用一次查询加载该文章全部已发布回复并在内存中装配树，避免递归 N+1。
- 文章删除只原子写入 `deleted_at`；到期清理才在一个事务内删除评论、文章标签关联和文章记录，并安排托管图片在提交后回收。
- 比赛列表先分页比赛根，再批量装配类型、参赛用户、公开文章、奖项和获奖人；公开列表/详情与管理员回收站列表均使用 `REPEATABLE_READ` 只读事务，保证这些分批查询装配自同一一致快照。个人资料按 username 一次读取可见获奖投影，不按比赛或奖项制造 N+1 查询。
- 图片引用校验和用户资产清理使用批量关联查询，不按图片逐条读取。

## 文章下载与管理员备份

`GET /player/blog/download?id={id}` 要求 `ROLE_player` 或 `ROLE_admin`，以 `application/zip` attachment 流式返回已发布文章。压缩包直接包含 `article.md`、`metadata.json` 和文章已绑定的本地托管首图/正文图，不附带额外 README；`article.md` 依次写入标题、简介与原始正文，托管 URL 会改成 `images/**` 相对路径。图片目录使用 `cover-original.jpg`、`content-1-original.jpg` 等扁平语义化文件名，外站图片 URL 原样保留且服务端不会抓取。公开文章与内部文章均可下载，草稿不可下载。文件名按 Unicode code point 截断，避免超长 UTF-8 `Content-Disposition` 超出网关响应头缓冲区。

公开 `GET /blog?id={id}` 与受保护的内部文章详情都直接返回文章的纯文本 `description`，供阅读页在首图下展示；该字段与编辑接口使用同一数据库列，不额外生成或改写。

`ROLE_player` 按 username 在全部文章下载间共享一个 30 秒固定窗口，切换文章不能绕过；命中限制返回 HTTP 429、`ARTICLE_DOWNLOAD_RATE_LIMITED` 和 `Retry-After`。`ROLE_admin` 不限频。Redis 无法完成限流检查时普通用户返回 HTTP 503 和 `ARTICLE_DOWNLOAD_RATE_LIMIT_UNAVAILABLE`，管理员下载不依赖该检查。

`GET /admin/blogs/backup` 仅允许管理员，以固定短 ASCII 文件名流式返回全量 ZIP。范围覆盖数据库中仍存在的已发布文章、内部文章、草稿和七天回收站文章，包内按文章保存 Markdown、状态/分类/标签元数据、评论父子关系与托管图片，并汇总文章作者资料和本地托管头像。备份不包含密码哈希、JWT、评论 IP/邮箱/通知字段，也不主动下载外站图片；本地文件缺失时在 `manifest.json` 的 `warnings` 中记录。

## 文章回收站

`DELETE /player/blog` 与 `DELETE /admin/blog` 只把正常文章的 `deleted_at` 设置为当前时间。回收站文章立即退出列表、详情、搜索、评论状态、精选、编辑和下载查询，但正文、评论、分类/标签关联和托管图片固定保留七天。

作者通过 `GET /player/blogs/recycle-bin` 查看本人回收站，并在期限内调用 `PUT /player/blog/restore` 恢复；管理员使用对应 `/admin/blogs/recycle-bin` 与 `/admin/blog/restore` 管理全部作者的文章。恢复 SQL 同时约束所有权和七天截止时间。每 15 分钟运行的清理任务只选择 `deleted_at <= now - 7d` 的文章并加行锁，在同一事务中删除评论、标签关联和文章；图片只在该事务提交后回收。分类/标签删除计数在保留期内继续包含回收站文章，保证恢复时引用完整。

## 首页精选分组

首页精选不再从 `blog.is_top`、`blog.is_recommend` 和更新时间动态挑选。Blog API 持久化最多三组精选内容；每组标题可编辑、顺序可调整，并且必须恰好绑定三篇互不重复的文章。同一篇文章在整个首页最多出现一次。创建和更新只接受当前 `is_published=true`、`is_internal=false`、`deleted_at IS NULL` 的文章，组内 `articleIds` 顺序就是公开卡片顺序。

管理员接口全部位于 `/admin/homepage-featured-groups`：

```text
GET    /admin/homepage-featured-groups
GET    /admin/homepage-featured-groups/candidates
POST   /admin/homepage-featured-groups
PUT    /admin/homepage-featured-groups/{groupId}
DELETE /admin/homepage-featured-groups/{groupId}
PUT    /admin/homepage-featured-groups/order
```

创建和更新以 `title` 与恰好三个 `articleIds` 保存整组内容；排序请求提交当前全部组 ID。候选接口只返回仍可进入首页的公开文章。成员文章后来变为草稿、内部文章或进入回收站时，关联关系和管理端 `available=false` 状态继续保留；公开端不增加独立路由，`GET /site` 暂不返回含失效成员的整组，文章恢复资格后该组自动重现。其余 `featuredGroups` 按组顺序返回最多三组，每组固定三篇，并为卡片提供文章标题、简介、16:9 首图、日期、分类、批量装配的彩色标签以及 `authorUsername`、`authorNickname`、`authorAvatar`。这些精选文章对游客和登录用户一致，内部文章不能进入首页编排。

`V038__create_homepage_featured_groups.sql` 创建精选组及组文章两张表，并从升级前当前已发布、公开且未删除的文章中按 `is_top DESC, is_recommend DESC, update_time DESC, id DESC` 选择前三篇回填第一组；不足三篇时不创建组。旧 `PUT /admin/blog/recommend` 删除，`blog.is_recommend` 仅为历史存储和文章备份兼容保留，不再参与首页读取。

## 首页精选图片

首页动态图片只保留滚动精选图片。精选图片固定裁剪为 1200×800 JPEG，最多十二张，可为零张；持久化顺序就是主页循环图片带的顺序。公开端通过 `GET /homepage-featured-images` 一次返回全部 `id`、原图 `imageUrl`、压缩图 `thumbnailUrl` 和 `sortOrder`，不附带认证。管理员接口位于 `/admin/homepage-featured-images`，支持全量列表、multipart 上传、提交全部 ID 的顺序替换和单张删除。旧 `/homepage-banners` 与 `/admin/homepage-banners/**` 已删除，首屏首图由 Blog 构建内置静态 PNG 提供。

每张精选图片写入 `/api/image/homepage-featured-<uuid>.jpg` 原图与 `/api/image/homepage-featured-<uuid>-thumbnail.jpg` 720×480 压缩缩略图。旧数据第一次读取时自动补生成缩略图；生成失败时响应临时回退原图。上传文件在事务回滚时成对清理，删除只在数据库事务提交后回收两份文件；定时清理任务同时扫描超过一小时且未被数据库引用的精选图片孤儿文件。`V039__create_homepage_featured_images.sql` 创建有序表，`V040__retire_homepage_banners_and_add_featured_thumbnails.sql` 增加缩略图字段并删除旧横幅表。

## 分类、标签与站点初始化

- `GET /categories` 只返回分类名称/颜色列表，供独立训练开发外壳导航使用。
- `GET /category` 与 `GET /tag` 按名称分页返回文章；存在认证时显式 Bearer 读取可包含内部文章。
- 管理端 `GET /admin/categories` 与 `GET /admin/tags` 分别分页，避免一次加载全部分类和标签。
- `/site` 只组装 Blog 首页仍消费的站点配置、分类、标签和 `featuredGroups`；分组按管理员顺序返回，每组固定三篇当前公开文章，未手填简介时使用正文前 280 个字符生成摘要。

## 调度与缓存默认值

Codeforces/AtCoder 提交采集 schedule 以及 AtCoder 题目元数据 bootstrap/调度均默认关闭。只有设置对应 `BLOG_*_COLLECTION_ENABLED`、`BLOG_ATCODER_PROBLEM_LIST_SCHEDULE_ENABLED` 或 `BLOG_ATCODER_PROBLEM_LIST_BOOTSTRAP_ENABLED` 后才会运行；管理员仍可显式创建采集任务。

自动提交采集按每个用户、每个 OJ 的 `lastCollectedAt` 计算窗口：每日任务默认从该游标向前回看 `100h`，日内半小时任务默认使用 `0h`，即直接从上次成功游标继续。两个默认值分别由 `BLOG_DAILY_COLLECTION_LOOKBACK`、`BLOG_INTRADAY_COLLECTION_LOOKBACK` 覆盖；首次没有游标时无论配置值为何都抓取全量历史。手动采集继续要求正数回看小时数。

Blog 缓存 TTL 由 `BLOG_CACHE_TTL` 控制，默认 `10m`。Spring scheduling 只保留本地托管文件生命周期和文章回收站到期清理，不是已删除的 Quartz 业务任务系统；回收站清理 cron 可用 `BLOG_RECYCLE_BIN_CLEANUP_CRON` 覆盖，但七天保留时长不可配置。

## 目录结构

```text
src/main/java/top/naccl/
  config/       安全、JWT、基础设施和训练模块组装
  controller/   公开、player、admin HTTP adapter
  mapper/       Blog/MyBatis mapper interface
  model/        请求与响应模型
  service/      Blog、用户、评论、缓存和图片用例
src/main/resources/
  mapper/       MyBatis XML
  db/migration/ Blog baseline、整合和裁剪迁移
src/test/java/top/naccl/  安全、Controller、Service、schema 与组装测试
```

Blog API 可以依赖训练 application contract；`platform-training-data` 不得依赖 `top.naccl` 类。

## 文件与路径职责

| 文件/路径 | 职责 |
| --- | --- |
| `pom.xml` | 后端依赖、测试与 Spring Boot 打包；不再包含 Quartz、邮件、Hutool、校验 starter 或旧上传依赖 |
| `src/main/java/top/naccl/BlogApiApplication.java` | 唯一后端应用入口 |
| `src/main/java/top/naccl/config/SecurityConfig.java`、`JwtFilter.java` | URL 权限层级和基于数据库当前用户的 JWT 授权 |
| `src/main/java/top/naccl/controller/LoginController.java`、`service/LoginAttemptLimiter.java` | BCrypt 登录、JWT 签发与按 username 的 Redis 五秒失败冷却 |
| `src/main/java/top/naccl/exception/Login*Exception.java`、`handler/ControllerExceptionHandler.java` | 登录 401/429/503、稳定 errorCode 与 `Retry-After` 映射 |
| `src/main/java/top/naccl/config/BootstrapAdminInitializer.java` | 幂等创建固定 `root` 管理员 |
| `src/main/java/top/naccl/config/TrainingDataModuleConfiguration.java` | 进程内组装 common、Codeforces 与 AtCoder 训练模块 |
| `src/main/resources/application.properties` | 数据库、Redis、JWT、上传目录、缓存 TTL 和默认关闭的采集配置 |
| `src/main/java/top/naccl/controller/admin/UserAdminController.java` | 用户列表、批量创建、单 PUT 原子更新与删除 API |
| `src/main/java/top/naccl/model/dto/AdminUserUpdateRequest.java` | 管理员原子用户更新请求 |
| `src/main/java/top/naccl/service/impl/AdminUserService.java` | 用户事务、`root`/最后管理员保护、handle 清理与精确替换 |
| `src/main/java/top/naccl/controller/player/TrainingDataQueryController.java` | 用户目录、单人/多人汇总、提交和首 AC 查询 HTTP adapter |
| `src/main/java/top/naccl/controller/admin/TrainingDataAdminController.java` | 采集任务和数仓刷新 API |
| `src/main/java/top/naccl/controller/CompetitionController.java` | 公开比赛分页和详情查询 |
| `src/main/java/top/naccl/controller/admin/CompetitionAdminController.java` | 管理员比赛、参赛用户、奖项的添加/删除与比赛回收站恢复 |
| `src/main/java/top/naccl/controller/player/PlayerCompetitionController.java` | 参赛用户本人公开文章的比赛绑定/解绑和名片奖项展示偏好/顺序 |
| `src/main/java/top/naccl/service/CompetitionService.java`、`mapper/CompetitionMapper.java` | 规范分类/奖档校验、批量装配、文章可见性、个人获奖记录排序与七天清理 |
| `src/main/java/top/naccl/service/CompetitionRecycleBinCleanupJob.java` | 定期触发满七天比赛聚合的物理清理 |
| `src/main/java/top/naccl/entity/Competition*.java` | 比赛根、类型、参赛用户、奖项/获奖人、文章和个人获奖扁平投影 |
| `src/main/java/top/naccl/enums/Competition*.java`、`model/dto/Competition*.java`、`model/vo/Competition*.java` | 规范赛事分类、固定奖档/形态约束及请求、公开树和个人获奖投影 |
| `src/main/java/top/naccl/model/dto/CompetitionAchievementOrderRequest.java` | 当前用户完整公开奖项 ID 顺序请求 |
| `src/main/java/top/naccl/controller/CategoryController.java`、`controller/TagController.java` | 公开分类目录及分类/标签文章分页 |
| `src/main/java/top/naccl/controller/admin/CategoryAdminController.java`、`TagAdminController.java` | 管理员分类/标签分页与写操作 |
| `src/main/java/top/naccl/service/impl/BlogServiceImpl.java` | 正常文章列表、批量标签装配、详情与写入 |
| `src/main/java/top/naccl/model/vo/BlogTagProjection.java`、`mapper/TagMapper.java` | 批量读取文章—标签投影 |
| `src/main/java/top/naccl/controller/admin/HomepageFeaturedGroupAdminController.java` | 精选组列表、候选文章、整组创建/更新、删除和顺序替换 API |
| `src/main/java/top/naccl/service/HomepageFeaturedGroupService.java`、`repository/HomepageFeaturedGroupRepository.java` | 最多三组、每组三篇、全首页文章唯一、公开文章资格校验、标签批量装配与持久化 |
| `src/main/java/top/naccl/model/dto/HomepageFeaturedGroup*.java`、`model/vo/HomepageFeatured*.java` | 精选组写请求、管理视图、候选文章和 `/site` 公开作者卡片 DTO |
| `src/main/java/top/naccl/controller/CommentController.java` | 公开文章评论分页读取 |
| `src/main/java/top/naccl/controller/player/PlayerCommentController.java`、`model/dto/PlayerCommentCreateRequest.java` | 登录评论读取/创建；创建请求不接受访客身份字段 |
| `src/main/java/top/naccl/service/impl/CommentServiceImpl.java` | 根评论分页和批量回复树装配 |
| `src/main/java/top/naccl/service/ImageAssetService.java`、`ImageProcessingService.java` | 托管图片校验、批量引用校验、绑定和文件生命周期 |
| `src/main/java/top/naccl/controller/HomepageFeaturedImageController.java`、`controller/admin/HomepageFeaturedImageAdminController.java` | 精选图片公开全量读取和管理员上传/排序/删除 API |
| `src/main/java/top/naccl/service/HomepageFeaturedImageService.java`、`repository/HomepageFeaturedImageRepository.java` | 最多十二张、1200×800 JPEG 校验、720×480 缩略图生成/旧数据补全、有序持久化、事务文件回收和孤儿清理 |
| `src/main/java/top/naccl/model/vo/HomepageFeaturedImage.java` | 含原图与压缩缩略图 URL 的精选图片公开/管理 DTO |
| `src/main/java/top/naccl/service/ImageAssetCleanupJob.java` | 定期清理临时/孤儿资产与精选图片孤儿文件 |
| `src/main/resources/db/migration/V040__retire_homepage_banners_and_add_featured_thumbnails.sql` | 删除旧动态横幅表并为滚动精选图片增加缩略图字段 |
| `src/main/java/top/naccl/controller/player/PlayerBlogController.java` | 本人文章 CRUD、内部文章读取与登录用户单篇文章 ZIP 响应 |
| `src/main/java/top/naccl/controller/admin/BlogAdminController.java` | 管理文章、回收站恢复与全量文章备份响应 |
| `src/main/java/top/naccl/service/ArticleDownloadService.java`、`ArticleDownloadRateLimiter.java` | 已发布文章读取、普通用户跨文章 Redis 限流与管理员豁免 |
| `src/main/java/top/naccl/service/ArticleArchiveService.java`、`model/vo/ArticleBackupComment.java` | 单篇/全量 ZIP 流式写入、托管图片链接改写和去敏评论投影 |
| `src/main/java/top/naccl/service/ArticleRecycleBinService.java`、`ArticleRecycleBinCleanupJob.java` | 固定七天软删除、本人/管理员恢复、加锁到期事务清理与调度 |
| `src/main/java/top/naccl/service/impl/RedisServiceImpl.java` | 有 TTL 的可降级内容缓存和提交后失效 |
| `src/main/java/top/naccl/controller/PublicProfileController.java`、`service/PlayerProfileService.java` | 公开作者名片和当前用户资料/友情链接管理；个性签名最多 40 字符 |
| `src/main/resources/db/migration/V035__drop_retired_nblog_features.sql` | 删除旧页面、日志统计、访客和 Quartz 表 |
| `src/main/resources/db/migration/V036__add_blog_recycle_bin.sql` | 增加文章 `deleted_at` 与到期查询索引 |
| `src/main/resources/db/migration/V037__create_competition_records.sql`、`V041__normalize_competition_awards_and_profile_order.sql` | 创建比赛聚合；允许普通奖项排名为空、增加个人公开顺序并精确修复已确认的历史分类 |
| `src/main/resources/db/migration/V038__create_homepage_featured_groups.sql` | 创建首页精选组/文章顺序并在至少有三篇可选旧文章时回填首组 |
| `src/main/resources/db/migration/V039__create_homepage_featured_images.sql` | 创建最多十二张的首页精选图片有序表 |
| `src/test/java/top/naccl/UnifiedSchemaMigrationTest.java` | 锁定统一迁移及 V034/V035/V036/V037/V038/V039 schema 合同 |
| `src/test/java/top/naccl/service/HomepageFeaturedGroupServiceTest.java`、`controller/admin/HomepageFeaturedGroupAdminControllerTest.java` | 精选组数量/文章资格/全局去重、排序和管理员 HTTP 委派合同 |
| `src/test/java/top/naccl/service/HomepageFeaturedImageServiceTest.java`、`controller/HomepageFeaturedImageControllerTest.java`、`controller/admin/HomepageFeaturedImageAdminControllerTest.java` | 精选图片数量/尺寸/事务文件生命周期与公开/管理员 HTTP 委派合同 |
| `src/test/java/top/naccl/service/CompetitionServiceTest.java`、`controller/CompetitionControllerTest.java` | 比赛聚合校验、回收站边界、个人获奖投影和三类 Controller 委派合同 |
| `src/test/java/top/naccl/controller/admin/UserAdminControllerTest.java`、`service/AdminUserServiceTest.java` | 原子用户 API 和事务规则测试 |
| `src/test/java/top/naccl/controller/player/TrainingDataBatchQueryControllerTest.java` | 多人汇总批量接口合同测试 |
| `src/test/java/top/naccl/service/impl/BlogListTagBatchTest.java`、`CommentServiceImplTest.java` | 批量标签及两查询评论树回归测试 |
| `src/test/java/top/naccl/service/impl/RedisServiceImplTest.java` | TTL、降级和提交后失效测试 |
| `src/test/java/top/naccl/service/ArticleDownloadServiceTest.java`、`ArticleDownloadRateLimiterTest.java` | 下载可见性、管理员豁免、30 秒限流与故障关闭测试 |
| `src/test/java/top/naccl/service/LoginAttemptLimiterTest.java`、`controller/LoginControllerTest.java`、`handler/LoginExceptionHandlerTest.java` | 五秒占位、失败保留、响应头和 Redis 故障关闭测试 |
| `src/test/java/top/naccl/service/ArticleArchiveServiceTest.java` | 单篇图片归档、全量文章/评论/作者/头像备份与敏感字段排除测试 |
| `src/test/java/top/naccl/service/ArticleRecycleBinServiceTest.java`、`service/impl/BlogDeletionTransactionTest.java` | 七天截止时间、所有权、内容保留和到期物理删除回滚测试 |

## 训练用户目录响应

`GET /player/training-data/users` 要求 `ROLE_player` 或 `ROLE_admin`。每项字段严格为：

```json
{
  "username": "player1",
  "nickname": "队员一",
  "ojNames": ["CODEFORCES", "ATCODER"]
}
```

响应不暴露邮箱、角色、真实 handle、采集状态或管理员私有字段，并按 `username` 排序。

## 验证

从仓库根目录运行：

```bash
mvn clean test
mvn clean package -DskipTests
```

第二条只在打包或镜像行为改变时要求执行。
