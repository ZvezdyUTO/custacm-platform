# System Architecture

## 后端运行时

`platform-blog/upstream/nblog/blog-api` 是唯一可运行后端。它使用 Java 21 与 Spring Boot 3.5.16，保留 NBlog 的 `top.naccl` package；Blog MyBatis 与训练 JDBC service 共享一个 MySQL `DataSource`、事务管理器和 Flyway history，Redis 是外部基础设施。

根 Maven reactor 包含：

```text
platform-common/
  common-core/
platform-training-data/
  training-data-common/
  training-data-codeforces/
  training-data-atcoder/
platform-blog/upstream/nblog/blog-api/
```

原有独立认证和训练 Web 运行时已从 reactor 移除；现行前端只调用 Blog API。

比赛与个人获奖记录同样属于 Blog API 的 `top.naccl` 业务边界，不新增独立服务或公共模块实体。公开 Controller 提供比赛分页/详情，admin Controller 只承担添加、删除和回收站恢复，player Controller 只承担参赛用户本人文章绑定；同一应用服务在事务中维护完整比赛聚合。

## 前端运行时

生产环境对外只有一个 `frontend`（Nginx）服务。仓库内保留两份职责不同的 Vue 3 构建产物，不是两套独立部署：

```text
/                 Vue 3 + Vite Blog（platform-blog/upstream/nblog/blog-view）
/training/**      Vue Blog 外壳中的训练中心公开路由
/training-app/**  Vue 3 训练运行时静态产物（内部）
/api/**           去掉 /api 后反向代理到 blog-api:8090
```

`/training/**` 使用 Blog history fallback，因此进入训练中心后原 `Nav.vue` 实例继续挂载，只替换下方内容；训练运行时在同源 frame 内使用独立的 `/training-app/**` history fallback。两套应用通过 `custacm.accessToken`、`custacm.user` 共享登录会话；用户摘要只用于展示。

Vue Blog 通过 `/competitions` 与 `/competitions/:id` 匿名读取比赛聚合，个人主页消费本人 `achievements` 并通过受保护 player adapter 修改逐项公开偏好、绑定本人公开文章；Vue Training 在 `/training/admin/competitions` 组合管理员比赛、参赛用户、奖项和回收站接口。两端只消费后端授权结果，不复制比赛授权规则。

两套应用固定使用共享浅色语义 token，不读取系统配色、不写视觉模式存储，也不在同源 frame 间同步主题。两份 HTML 不运行主题探测脚本，Blog 顶栏和 Training 独立开发外壳都没有主题按钮。文章目录、黑色页脚等固定深色区域是 Blog 的页面级视觉，不依赖根节点模式；文章与实时编辑器代码块固定使用浅色语法配色。

前端视觉改版以 `frontend-design-tokens/tokens.css` 为单一 token 源，通过仓库脚本生成两端构建树内的副本，避免改变当前两个独立 Docker build context。Training 在阶段 2 接入 token、有限玻璃和可降级路由动效；Blog 在阶段 3 接入同一 token、Element Plus 语义变量、有限玻璃和弹层焦点管理，并在阶段 6 将遗留 Semantic UI 布局替换为项目自有 class、把图标统一到 Lucide。两套 Router 和运行拓扑均不因此变化。

Blog 顶栏“训练中心”与“分类”一致采用点击开关型下拉菜单：点击标题只展开或收起，悬停不展示，选择多人、单人或题目查询项后才发生路由跳转。

Vue Blog 的公开请求不全局附加 JWT。文章列表、分类、标签和搜索读取仅在存在共享会话时由具体 API adapter 显式发送 Bearer，使登录用户获得内部文章；受保护写入、内部文章正文读取和文章图片 ZIP 下载同样由具体 adapter 发送 token。首页 `GET /site` 即使收到可选 Bearer，`featuredGroups` 也始终只投影公开、已发布且未回收的文章。训练中心的 `/player/**`、`/admin/**` 请求也采用相同方式。

文章搜索只在文章目录中提供，输入时不发请求，回车或点击左侧搜索按钮后才按全站文章标题匹配并返回文章简介作为结果摘要。文章阅读数不属于当前产品数据：详情读取不计数，Blog DTO、Mapper、Redis 同步任务和数据库列均不保留该字段；文章字数与估算阅读时长继续由发布流程维护。

Vue Blog 的本人文章列表、回收站、发布、编辑、图片上传、删除和恢复通过独立 adapter 显式发送 Bearer；`/write` 和 `/write/{id}` 支持 Markdown 导入、首图裁剪及正文图片选择/拖拽/粘贴。公开文章详情的 `authorUsername` 只决定是否显示编辑入口，写入仍由 Blog API 校验 `blog.user_id`。

登录用户可从文章详情下载已发布文章的 Markdown 与托管图片 ZIP；`article.md` 组合标题、简介和原始正文，归档流式读取本地图片，将托管 URL 改为扁平语义化相对路径且不从外网抓图。普通用户按 username 在全部文章下载间共享一个 Redis 30 秒原子窗口，管理员在限流前豁免；限流存储故障时普通用户返回 503，防止基础设施异常时静默放开频率约束。管理员全量备份同样流式写 ZIP，覆盖所有文章状态、去敏评论、作者资料与托管图片，避免在后端一次性聚合完整压缩包字节。

公开 Blog 只保留首页、文章、分类、标签、搜索、比赛与奖项档案、个人资料与评论页面；About、全站友链和动态页面及其 API 已移除。用户自己的资料、奖项公开偏好和个人友情链接集中在 `/profile`，不会复用已删除的全站友链模型。

公开 Blog 首页只展示带页边距的精选区，不渲染普通文章列表、个人介绍或标签云。精选区最多三组，每组先展示管理员可编辑的标题，再以第一篇横向大卡、下方并排第二和第三篇的层级布局展示固定三篇文章；组和组内文章都按持久化顺序渲染，同一篇文章在全首页不能重复。所有首图使用 16:9 媒体框和 `contain` 完整显示，不裁剪原图；卡片展示分类、标题、三行简介、日期，以及作者头像、昵称和 username。首页固定使用暖米色画布与低饱和浅奶油棕卡片。文章目录页继续固定使用暖黑画布与深色卡片，不随全局模式切换。文章详情页在固定顶栏下使用 IDE 式双区：左侧窄工具栏独立滚动并组合作者名片、目录与评论入口，作者名片右侧向登录用户提供下载操作并仅向当前作者提供编辑操作；右侧阅读画布独立滚动并依次展示大标题、日期、可选首图、首图下的浅色小号衬线简介、正文和完整评论；移动端隐藏工具栏并恢复单列页面流。

Vue Blog 左侧个人卡片只在文章详情页根据 `authorUsername` 匿名读取 `/profiles/{username}` 展示文章作者资料；首页和列表页不展示该卡片。头像先在浏览器裁剪为 512×512 PNG，再由 Blog API 生成 96×96 缩略图，`user.avatar` 保存缩略图 URL，资料响应同时返回高清头像 URL。

托管图片写入 `uploads/assets/{uuid}/original|thumbnail`。正文图片接受最大 15MB JPEG/PNG，高清版最长边 2560、缩略图最长边 960；编辑器插入标准图片 Markdown 并直接预览缩略图，阅读页默认懒加载缩略图，预览中的“加载原图”才请求高清版。`image_asset` 与 `blog_image_reference` 记录所有权和唯一文章引用；尚未保存的正文图片从编辑器移除后立即回收，已绑定图片在文章保存成功后回收。文章进入回收站时不改图片状态，固定七天后物理清理事务提交才删除失效目录；主动移除图片或更换头像仍在事务提交后立即回收。失败记录为 `DELETING` 并由每日任务重试，超过 24 小时的 `TEMP` 和无数据库记录目录也会清理。

训练中心使用 `/training/login`、`multiple`、`single`、`problem` 以及 `/training/admin/create-users|users|articles|categories|competitions|training|appearance`。管理员区包含“创建用户”“管理用户”“管理文章”“分类与标签”“比赛与奖项”“数据采集”“首页图片”七个独立页面，并使用勃艮第专属主题。创建用户、全量文章下载、分类/标签删除、比赛/参赛用户/奖项删除和首页图片删除都使用同一主题化确认框，不调用浏览器原生确认框；全量文章下载确认明确覆盖草稿、内部文章、回收站、去敏评论、作者资料和托管图片。比赛与奖项页匿名读取正常比赛完整树，受保护地创建比赛、批量添加参赛用户、添加个人或团队奖项，并支持根记录七天回收站与恢复；比赛、参赛用户和奖项均不提供普通编辑，子关系更正采用确认删除后重加。`/training/admin/articles` 不新增顶层路由，而是在管理文章页内提供“首页编排”、当前文章和回收站子视图；首页编排通过 `/admin/homepage-featured-groups/**` 创建最多三组、编辑标题、为每组选择恰好三篇当前公开已发布且未回收的文章，并调整组顺序。旧 `/admin/blog/recommend` 已移除，`is_recommend` 仅保留为历史存储/备份字段，不再影响首页。管理员删除文章仅写入 `deleted_at`，正文、标签、评论和图片固定保留七天，期间作者本人或管理员可恢复，到期定时任务才使用行锁和单事务物理清理。分类允许自定义名称和颜色；标签只新增或删除，新增时服务端从连续数值空间生成并持久化深色随机颜色，Blog 标签云统一以白字渲染。正式布局范围为 1280–2560 px 桌面端，重点验收 1440×900 与 1920×1080。

Vue Blog 首页首屏直接使用构建内置的唯一静态图片 `/img/homepage-banner-default.png`，不再请求动态横幅 API。旧横幅 Controller、Service、Repository、管理面板和表由 `V040` 退出；Header 不做多图交叉淡入或跟随鼠标位移。

Header 渐隐区下方通过公开 `GET /homepage-featured-images` 一次读取最多十二张有序 1200×800 精选图片；空列表时不渲染图片带。服务端同时返回 720×480 `thumbnailUrl` 和原图 `imageUrl`，旧记录首次读取时补生成缩略图。Blog 使用三份相同序列从中间副本开始，实现缓慢向左的无缝循环；图片带初始只加载缩略图，点击打开共享预览器后仍先显示缩略图，用户明确点击“加载原图”才请求高清文件。悬停、键盘聚焦和拖动会暂停自动滚动，原生横向滚动、鼠标拖动与方向键仍可手动控制；左右覆盖层以画布渐变和 `backdrop-filter` 模糊边界。`prefers-reduced-motion: reduce` 下关闭自动滚动。

Vue Blog 外壳通过公开 `GET /site` 获取最小初始化数据：奖励图、评论管理员标记、文章详情作者兜底信息、分类、标签和 `featuredGroups`。分组按后台顺序返回最多三组，每组必须有三篇当前公开已发布且未回收的文章，并带卡片所需作者字段；任何成员后来失去资格时关系仍保留供后台修复，但公开端暂不返回整组，恢复资格后自动重现。API 不组装旧 `featuredBlogList`、徽章、收藏、社交链接或滚动文字。Redis 内容缓存读取或写入失败时降级为数据库读取，不阻断公开页面；所有 Blog 缓存都带 `BLOG_CACHE_TTL`，写事务提交后再失效。登录冷却和文章下载限流不属于可降级内容缓存，对应 Redis 检查失败时拒绝操作。

评论正文继续作为纯文本保存；新表情写入标准 Unicode。Vue Blog 用构建内的 Google Noto Emoji smileys SVG sprite 渲染选择器和受支持 Unicode，资源及许可随静态产物同源发布，不依赖第三方 CDN；旧 tv/阿鲁/泡泡短码只在读取时兼容转换。

## 比赛记录聚合

`competition` 是比赛聚合根，以年份和 `INDIVIDUAL`/`TEAM`/`MIXED` 参赛形态描述比赛；业务层把底层 `competition_type_tag` 的历史标签组合归一为十个互斥 `CompetitionCategory`，创建、筛选和响应都以规范 `category` 为准，不再接受任意标签组合。公开 `GET /competitions` 先按 `startYear`/`endYear` 闭区间、可选规范分类及分页筛选根记录，详情和分页项再装配参赛用户、当前公开文章、奖项及获奖人。ICPC/CCPC 分类固定团队形态，百度之星与蓝桥杯固定个人，GPLT 固定混合；管理员创建表单不单列参赛形态，按分类自动提交，省赛沿用团队默认，底层字段仍用于历史数据和奖项归属校验。

参赛用户、奖项与获奖人分别由 `competition_participant`、`competition_award`、`competition_award_recipient` 表达。API 以固定 `awardTier` 表达奖项：前七类赛事只接受金、银、铜、优胜奖并要求成对合法的排名；百度之星只接受国一至国四、省一至省三；GPLT 与蓝桥杯只接受一至三等奖并强制排名为空。旧 `award_level`/`award_scope`/`award_name` 只作为规范奖档的兼容存储。个人奖集合固定一人，团队奖至少一人且不限制队伍人数。获奖关系上的 `profile_visible` 默认关闭，`profile_sort_order` 保存每名获奖人的公开顺序；比赛公开详情仍显示完整获奖事实。个人名片不在 `user` 表复制获奖 JSON，而是从该关系投影 `achievements`，本人资料读取全部项目及偏好，公开资料只按保存顺序投影已开启项目。

`competition_article` 连接参赛记录与 Blog 文章。创建绑定时以 JWT username 同时校验参赛身份和文章作者，并只接受当前已发布公开文章；解绑仍以本人身份校验已有关系。读取时继续联查文章的 `published`、`internal`、`deleted_at`，所以文章转草稿、内部或进入回收站只会暂时退出公开比赛树，不破坏绑定，重新公开或恢复后自动出现。

管理员对比赛、参赛用户和奖项采用添加/删除模型，不提供普通更新；恢复比赛是管理员比赛管理的唯一 PUT，获奖人对本人 `profile_visible` 与 `profile_sort_order` 的偏好写入不改变奖项事实。开启公开时新项目追加到公开序列末尾，顺序更新必须提交本人当前全部公开 award ID，服务端验证集合完全一致后原子重排。删除比赛原子清空仅正常记录使用的 `active_full_name` 并写入 `deleted_at`，使同名新比赛可在旧记录七天回收期间创建；恢复时重新占用活动名称，若名称已被正常比赛占用则拒绝。只有聚合根进入回收站，期间所有子表不变；管理员回收站树继续装配当前公开已发布的文章，但整个聚合从公开比赛、玩家文章绑定目标及 `achievements` 隐藏。七天到期后删除聚合根并由外键级联物理清理子关系。

比赛参赛关系使用可空 `username` 外键：账号改名通过 `ON UPDATE CASCADE` 同步，账号删除通过 `ON DELETE SET NULL` 保留 `display_name_snapshot`、参赛和获奖历史。账号删除后没有可认证 username，历史关系只用于公开记录，不形成新的授权主体。

## 身份与授权

- `user.username` 是业务身份，也是 JWT `sub`。
- 存储角色只有 `ROLE_admin` 和 `ROLE_player`；guest 表示未认证。
- BCrypt 密码、HS512 JWT、账号、角色与 OJ handle 都由 Blog API 负责。
- 登录失败冷却由 Blog API 使用 Redis 按 username 原子执行五秒窗口；正确凭据释放占位，错误凭据保留，窗口状态不可用时失败关闭。Training 只消费 `Retry-After` 展示倒计时。
- 管理员通过一个 `PUT /admin/users/{username}` 原子更新账号字段、角色、密码、现役状态和完整 OJ handle 集合。改变或移除已有 handle 时，同一事务先清理对应 OJ 的 ODS、DWD、DWM、DWS 数据和旧采集状态，再替换绑定；任一步失败整体回滚。
- bootstrap 固定创建 `root` 系统管理员；该账号不可删除、改名、降权、绑定 OJ handle 或进入队员采集状态。用户头像字段为空时，两份 Vue 构建统一显示 Blog 构建内置默认头像。
- 受保护请求校验 token 后会从 MySQL 重新加载用户与当前角色；改名、改角色或删除会在下一次请求生效。
- `/admin/**` 仅管理员可用；`/player/**` 接受管理员或队员；未命中这两层的公开 GET 可匿名读取。
- Player 文章管理以当前 JWT `username` 解析 `user.id`，再以 `blog.user_id` 执行所有权校验；管理员可管理全部文章，其新建文章绑定当前认证管理员。
- Player 比赛文章绑定同样使用当前 JWT `username`，并同时校验比赛参赛关系与 `blog.user_id`；管理员访问 player 路径也不获得跨用户绑定权。

## 训练数据边界

训练模块保留 Codeforces/AtCoder 采集、ODS ingestion、DWD/DWM/DWS processing、查询、调度、刷新和清理实现。它们不拥有登录、JWT、用户管理 HTTP、handle 管理 HTTP 或 Spring Boot entrypoint。

唯一 Blog API 运行时保留两种 OJ 的提交采集计划，但自动采集、AtCoder 题目计划和启动补采全部默认关闭，避免未评估外部 API 配额时产生后台负载。部署者可分别通过 `BLOG_CODEFORCES_*_COLLECTION_ENABLED`、`BLOG_ATCODER_*_COLLECTION_ENABLED`、`BLOG_ATCODER_PROBLEM_LIST_SCHEDULE_ENABLED` 与 `BLOG_ATCODER_PROBLEM_LIST_BOOTSTRAP_ENABLED` 显式启用。启用后，每个用户、每个 OJ 独立保存最近一次成功采集窗口的结束时间；没有成功记录时从数据源历史起点采集，有记录时每日任务默认从该时间向前回看 100 小时，日内任务默认以零回看直接续爬，并采集至本次任务开始时间。

训练 application service 依赖 `TrainingUserDirectory` 获取 `username`/handle 并更新每个 OJ 的 `lastCollectedAt` 采集游标，Blog API 通过管理员用户响应只读暴露该字段。采集资格可在绑定 OJ handle 前独立保存；训练查询目录默认只列出 `needCollect=true` 且至少绑定一个 OJ 的用户，显式 `includeRetired=true` 时保留已退役用户。`training-data-common` 通过不带 Spring MVC 注解的 `OjWarehouseQueryFacade` 暴露查询用例；所有训练 HTTP Controller 都留在 Blog API 的 `top.naccl` package，通过 `/player/training-data/**` 与 `/admin/training-data/**` 返回 Blog `Result` envelope。

`GET /player/training-data/users` 只向已登录用户返回成员的 `username`、`nickname`、`ojNames`，不暴露真实 OJ handle 或管理员字段；默认排除已退役成员，只有查询参数 `includeRetired=true` 才保留。Vue 3 多人页随后只调用一次 `GET /player/training-data/accepted-summaries` 批量读取当前 OJ 的全部汇总，避免按用户 N+1 请求；缺项或整体失败显示为可重试错误。

多人和单人查询不提供独立查询按钮；OJ、队员、日期与 rating 等筛选参数变化后由前端自动刷新，连续数字输入使用短防抖，日期或 rating 范围非法时只展示校验错误而不发请求。所有训练查询页的 OJ 选择器使用与 Blog 顶栏一致的点击展开式自定义菜单，不使用浏览器原生 `select`。多人页可显式包含退役队员；单人页固定读取包含退役队员的全量目录，不再提供退役筛选开关。单人查询初次加载用户目录时不预选队员，输入 username 或姓名子串后按 `username · nickname` 降序展示下拉候选，点击放大镜或按回车查询当前高亮候选。管理员用户页在已加载列表中按 username 子串过滤。题目查询使用填满筛选栏的五列布局，保留深色显式查询按钮，题号和日期修改完成后统一提交。

## 持久化

新数据库由 Flyway 初始化：

- `V001` 创建不含硬编码管理员的 NBlog schema。
- `V010` 至 `V023` 创建并演进 Codeforces/AtCoder ODS、共享 handle 与 warehouse 表；其中把单 OJ handle 提升为共享 `oj_handle_account` 的 `V018` 由 `training-data-common` 持有，保留原版本和校验内容。
- `V024` 将 `oj_handle_account.student_identity` 改为 `username`，约束两种角色，增加 user/handle cascade foreign key，并将文章/评论作者 foreign key 改为 `ON DELETE SET NULL`。
- `V025` 创建 `homepage_banner_image`，记录首页图片同源 URL 与唯一排序序号，并以原有三张图片初始化。
- `V026` 将历史首页图片收敛为前两张；此后服务端限制首页总数为一至两张。
- `V029` 创建托管图片资产与文章图片引用，给文章首图和用户头像增加可空资产外键；历史 URL 保持兼容。
- `V032` 删除文章独立密码字段，并增加 `is_internal`，用于区分公开文章、内部文章和草稿。
- `V027` 为 `user` 增加个性签名，并创建按 `user_id` 级联删除的有序 `user_profile_link`。
- `V028` 删除其余历史首页图，并将唯一保留项指向 Blog 构建内置的 `/img/homepage-banner-default.png`；表为空时补建该默认项。
- `V034` 把 JSON 账号拆成 `training_member` 与 `oj_handle_binding`。handle 使用二进制排序规则保持大小写精确；迁移先在临时表校验 JSON、trim 后的绑定唯一性，再创建正式表。生产代码只读写新表，旧 `oj_handle_account` 保留一个迁移窗口后再单独删除。
- `V035` 删除 About、全站友链、动态、Quartz、访问统计和历史应用日志等已退役功能独占的数据表；`site_setting`、文章和评论数据不受影响。
- `V036` 为文章增加可空 `deleted_at` 与回收站索引；正常查询统一排除该字段非空的文章，分类/标签引用在七天保留期内继续阻止对应分类或标签被删除。
- `V037` 创建 `competition`、`competition_type_tag`、`competition_participant`、`competition_award`、`competition_award_recipient` 与 `competition_article`。活动名称辅助列只约束正常比赛全称唯一，获奖关系以默认关闭的 `profile_visible` 保存个人名片偏好；比赛删除级联子关系，参赛 username 改名级联、账号删除置空并保留昵称快照，文章物理删除级联清理比赛文章绑定。
- `V041` 允许普通奖项排名成对为空，为获奖关系增加公开排序字段，并只按精确比赛全称补齐一条已确认的 ICPC 亚洲区域赛历史标签；不根据标题关键词批量猜测分类。
- `V038` 创建 `homepage_featured_group` 与 `homepage_featured_group_article`，持久化组标题、组顺序及组内三篇文章的顺序。升级时按旧 `is_top desc, is_recommend desc, update_time desc, id desc` 顺序选取前三篇当前公开已发布且未回收文章；满三篇才创建默认“精选文章”组，不足三篇则不创建。迁移完成后 `is_recommend` 只用于历史存储与备份兼容，不参与运行时首页查询。
- `V039` 创建首页精选图片有序表；服务端限制最多十二张，空列表合法。

改名更新 `user` 主行并由 handle、比赛参赛关系 foreign key 级联。删除用户先清理其绑定 OJ 的 ODS/DWD/DWM/DWS 行并匿名化评论，再删除用户；handle 级联删除，文章与评论保留并显示“已注销用户”，比赛参赛 username 置空并继续使用昵称快照展示历史奖项。

个人友情链接属于 Blog API 用户附属数据，不复用全站 `friend` 表；每条使用稳定 user id 外键，用户改名不影响链接，删除用户时链接级联删除。整体替换在同一事务内完成。

默认首页图片随 Blog 前端构建发布。运行时上传目录同时挂载给 Blog API（读写）和 Nginx（只读）；Nginx 直接提供 `/api/image/**` 并对 UUID 文件设置 immutable 缓存，其他 `/api/**` 仍代理 Blog API。

## Compose 拓扑

`deploy/docker-compose.yml` 定义恰好四个服务：

| Service | Responsibility |
| --- | --- |
| `blog-db` | MySQL 8.4，统一 Blog/训练 schema |
| `blog-redis` | Redis 7，Blog cache/运行支持 |
| `blog-api` | 唯一 Spring Boot 后端，host port 由 `BACKEND_PORT` 控制 |
| `frontend` | 同时托管 Vue 3 Blog 与 Vue 3 训练中心的 Nginx，HTTP host port 由 `FRONTEND_PORT` 控制；启用 TLS 时 HTTPS host port 由 `FRONTEND_HTTPS_PORT` 控制，证书从只读宿主机目录挂载 |

MySQL 与 Redis 使用新的命名卷；旧数据卷不会被挂载或自动删除。当前仓库提供本地/单机 Compose 配置，不代表已经完成任何服务器发布。
