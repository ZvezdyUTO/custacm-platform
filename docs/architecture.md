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

## 前端运行时

生产环境只有一个 Nginx 前端服务，但包含两份独立构建产物：

```text
/                 Vue 3 + Vite Blog（platform-blog/upstream/nblog/blog-view）
/training/**      Vue Blog 外壳中的训练中心公开路由
/training-app/**  Vue 3 训练运行时静态产物（内部）
/api/**           去掉 /api 后反向代理到 blog-api:8090
```

`/training/**` 使用 Blog history fallback，因此进入训练中心后原 `Nav.vue` 实例继续挂载，只替换下方内容；训练运行时在同源 frame 内使用独立的 `/training-app/**` history fallback。两套应用通过 `custacm.accessToken`、`custacm.user` 共享登录会话；用户摘要只用于展示。

Blog 顶栏“训练中心”与“分类”一致采用点击开关型下拉菜单：点击标题只展开或收起，悬停不展示，选择多人、单人或题目查询项后才发生路由跳转。

Vue Blog 的公开请求不全局附加 JWT。受保护写入和内部文章读取由具体 API adapter 显式发送共享 Bearer token。训练中心的 `/player/**`、`/admin/**` 请求也采用相同方式。

Vue Blog 的本人文章列表、发布、编辑、图片上传和删除通过独立 adapter 显式发送 Bearer；`/write` 和 `/write/{id}` 支持 Markdown 导入、首图裁剪及正文图片选择/拖拽/粘贴。公开文章详情的 `authorUsername` 只决定是否显示编辑入口，写入仍由 Blog API 校验 `blog.user_id`。

Vue Blog 左侧个人卡片在普通页面从共享会话展示当前用户资料，在文章详情页根据 `authorUsername` 匿名读取 `/profiles/{username}` 展示文章作者资料；头像先在浏览器裁剪为 512×512 PNG，再由 Blog API 生成 96×96 缩略图，`user.avatar` 保存缩略图 URL，资料响应同时返回高清头像 URL。

托管图片写入 `uploads/assets/{uuid}/original|thumbnail`。正文图片接受最大 15MB JPEG/PNG，高清版最长边 2560、缩略图最长边 960；编辑器插入标准图片 Markdown 并直接预览缩略图，阅读页默认懒加载缩略图，预览中的“加载原图”才请求高清版。`image_asset` 与 `blog_image_reference` 记录所有权和唯一文章引用；尚未保存的正文图片从编辑器移除后立即回收，已绑定图片在文章保存成功后回收。删除文章、移除图片或更换头像后事务提交即删除失效目录，失败记录为 `DELETING` 并由每日任务重试，超过 24 小时的 `TEMP` 和无数据库记录目录也会清理。

训练中心使用 `/training/login`、`multiple`、`single`、`problem` 以及 `/training/admin/create-users|users|articles|categories|training|appearance`。管理员区包含“创建用户”“管理用户”“管理文章”“分类与标签”“数据采集”“首页图片”六个独立页面，并使用勃艮第专属主题。管理文章页复用 Blog 推荐字段控制首页侧栏精选文章，并通过危险二次确认调用管理员删除接口；删除同时清理文章标签关联与评论且不可恢复。分类允许自定义名称和颜色；标签只新增或删除，新增时服务端从连续数值空间生成并持久化深色随机颜色，Blog 标签云统一以白字渲染。公开接口只返回已发布且精选的最多五篇文章，并按置顶、更新时间和 ID 确定性排序。正式布局范围为 1280–2560 px 桌面端，重点验收 1440×900 与 1920×1080。

Vue Blog 首页通过公开 `GET /homepage-banners` 读取任意数量的有序图片；后台不可用时退回构建内置的唯一默认图片。Header 保留鼠标横向移动驱动相邻图片交叉淡入淡出的交互，数据库顺序对应从左到右的切换顺序。

## 身份与授权

- `user.username` 是业务身份，也是 JWT `sub`。
- 存储角色只有 `ROLE_admin` 和 `ROLE_player`；guest 表示未认证。
- BCrypt 密码、HS512 JWT、账号、角色与 OJ handle 都由 Blog API 负责。
- 普通用户更新只能新增尚未绑定的 OJ handle；更换已有 handle 必须走独立高危用例，在同一事务内按旧 handle 清理该 OJ 的 ODS、DWD、DWM、DWS 数据和旧采集状态后再换绑，避免历史数据被新归属误认。
- bootstrap 固定创建 `root` 系统管理员；该账号不可删除、改名、降权、绑定 OJ handle 或进入队员采集状态。用户头像字段为空时，两套前端统一显示 Blog 构建内置默认头像。
- 受保护请求校验 token 后会从 MySQL 重新加载用户与当前角色；改名、改角色或删除会在下一次请求生效。
- `/admin/**` 仅管理员可用；`/player/**` 接受管理员或队员；未命中这两层的公开 GET 可匿名读取。
- Player 文章管理以当前 JWT `username` 解析 `user.id`，再以 `blog.user_id` 执行所有权校验；管理员可管理全部文章，其新建文章绑定当前认证管理员。

## 训练数据边界

训练模块保留 Codeforces/AtCoder 采集、ODS ingestion、DWD/DWM/DWS processing、查询、调度、刷新和清理实现。它们不拥有登录、JWT、用户管理 HTTP、handle 管理 HTTP 或 Spring Boot entrypoint。

训练 application service 依赖 `TrainingUserDirectory` 获取 `username`/handle 并更新采集状态。采集资格可在绑定 OJ handle 前独立保存，但训练查询目录仍只列出 `needCollect=true` 且至少绑定一个 OJ 的用户。`training-data-common` 通过不带 Spring MVC 注解的 `OjWarehouseQueryFacade` 暴露查询用例；所有训练 HTTP Controller 都留在 Blog API 的 `top.naccl` package，通过 `/player/training-data/**` 与 `/admin/training-data/**` 返回 Blog `Result` envelope。

`GET /player/training-data/users` 只向已登录用户返回可采集成员的 `username`、`nickname`、`ojNames`，不暴露真实 OJ handle 或管理员字段；Vue 3 多人查询在浏览器侧以最大并发数 6 消费该目录。

多人和单人查询不提供独立查询按钮；OJ、队员、日期与 rating 等筛选参数变化后由前端自动刷新，连续数字输入使用短防抖，日期或 rating 范围非法时只展示校验错误而不发请求。单人查询初次加载用户目录时不预选队员，只有用户主动选择后才加载个人数据。题目查询保留深色显式查询按钮，题号和日期修改完成后统一提交。

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

改名更新 `user` 主行并由 handle foreign key 级联。删除用户先清理其绑定 OJ 的 ODS/DWD/DWM/DWS 行并匿名化评论，再删除用户；handle 级联删除，文章与评论保留并显示“已注销用户”。

个人友情链接属于 Blog API 用户附属数据，不复用全站 `friend` 表；每条使用稳定 user id 外键，用户改名不影响链接，删除用户时链接级联删除。整体替换在同一事务内完成。

默认首页图片随 Blog 前端构建发布。运行时上传目录同时挂载给 Blog API（读写）和 Nginx（只读）；Nginx 直接提供 `/api/image/**` 并对 UUID 文件设置 immutable 缓存，其他 `/api/**` 仍代理 Blog API。

## Compose 拓扑

`deploy/docker-compose.yml` 定义恰好四个服务：

| Service | Responsibility |
| --- | --- |
| `blog-db` | MySQL 8.4，统一 Blog/训练 schema |
| `blog-redis` | Redis 7，Blog cache/运行支持 |
| `blog-api` | 唯一 Spring Boot 后端，host port 由 `BACKEND_PORT` 控制 |
| `frontend` | 同时托管 Vue 3 Blog 与 Vue 3 训练中心的 Nginx，host port 由 `FRONTEND_PORT` 控制 |

MySQL 与 Redis 使用新的命名卷；旧数据卷不会被挂载或自动删除。当前仓库提供本地/单机 Compose 配置，不代表已经完成任何服务器发布。
