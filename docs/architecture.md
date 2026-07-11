# System Architecture

## 后端运行时

`platform-blog/upstream/nblog/blog-api` 是唯一可运行后端。它使用 Java 21 与 Spring Boot 3.5.16，保留 NBlog 的 `top.naccl` package；Blog MyBatis 与训练 JDBC service 共享一个 MySQL `DataSource`、事务管理器和 Flyway history，Redis 是外部基础设施。

根 Maven reactor 包含：

```text
platform-common/
  common-core/
  common-web/
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

Vue Blog 的公开请求不全局附加 JWT。只有受保护评论提交读取共享 token 并为该请求显式发送 Bearer；密码文章 token 与登录 JWT 保持分离。训练中心的 `/player/**`、`/admin/**` 请求也由具体 API adapter 显式发送 Bearer。

Vue Blog 左侧个人卡片从共享会话纯展示当前用户的 avatar、nickname、username、160 字符个性签名和最多八条有序 HTTP(S) 个人友情链接；`/about` 右侧使用统一编辑面板修改 nickname、签名和链接。头像继续先在浏览器裁剪为 512×512 PNG，再通过显式 Bearer 请求写入 Blog API 的上传目录和 `user.avatar`。

训练中心使用 `/training/login`、`multiple`、`single`、`problem` 以及 `/training/admin/create-users|users|training|appearance`。管理员区把“创建用户”“管理用户”“数据采集”“首页图片”拆成四个独立页面；数据采集完成后固定刷新数仓，不向管理员暴露可选刷新开关。首页图片页在浏览器裁剪为 1920×1080 JPEG，再通过管理员 API 上传、左右调整完整顺序或删除。正式布局范围为 1280–2560 px 桌面端，重点验收 1440×900 与 1920×1080，移动端不在当前范围。

Vue Blog 首页通过公开 `GET /homepage-banners` 读取任意数量的有序图片；后台不可用时退回原有三张静态图片。Header 保留鼠标横向移动驱动相邻图片交叉淡入淡出的交互，数据库顺序对应从左到右的切换顺序。

## 身份与授权

- `user.username` 是业务身份，也是 JWT `sub`。
- 存储角色只有 `ROLE_admin` 和 `ROLE_player`；guest 表示未认证。
- BCrypt 密码、HS512 JWT、账号、角色与 OJ handle 都由 Blog API 负责。
- 受保护请求校验 token 后会从 MySQL 重新加载用户与当前角色；改名、改角色或删除会在下一次请求生效。
- `/admin/**` 仅管理员可用；`/player/**` 接受管理员或队员；未命中这两层的公开 GET 可匿名读取。

## 训练数据边界

训练模块保留 Codeforces/AtCoder 采集、ODS ingestion、DWD/DWM/DWS processing、查询、调度、刷新和清理实现。它们不拥有登录、JWT、用户管理 HTTP、handle 管理 HTTP 或 Spring Boot entrypoint。

训练 application service 依赖 `TrainingUserDirectory` 获取 `username`/handle 并更新采集状态。Blog API 提供进程内实现，通过 `/player/training-data/**` 与 `/admin/training-data/**` 暴露使用 Blog `Result` envelope 的 HTTP adapter。

`GET /player/training-data/users` 只向已登录用户返回可采集成员的 `username`、`nickname`、`ojNames`，不暴露真实 OJ handle 或管理员字段；Vue 3 多人查询在浏览器侧以最大并发数 6 消费该目录。

## 持久化

新数据库由 Flyway 初始化：

- `V001` 创建不含硬编码管理员的 NBlog schema。
- `V010` 至 `V023` 创建并演进 Codeforces/AtCoder ODS 与 warehouse 表。
- `V024` 将 `oj_handle_account.student_identity` 改为 `username`，约束两种角色，增加 user/handle cascade foreign key，并将文章/评论作者 foreign key 改为 `ON DELETE SET NULL`。
- `V025` 创建 `homepage_banner_image`，记录首页图片同源 URL 与唯一排序序号，并以原有三张图片初始化。
- `V026` 将历史首页图片收敛为前两张；此后服务端限制首页总数为一至两张。
- `V027` 为 `user` 增加个性签名，并创建按 `user_id` 级联删除的有序 `user_profile_link`。

改名更新 `user` 主行并由 handle foreign key 级联。删除用户先清理其绑定 OJ 的 ODS/DWD/DWM/DWS 行并匿名化评论，再删除用户；handle 级联删除，文章与评论保留并显示“已注销用户”。

个人友情链接属于 Blog API 用户附属数据，不复用全站 `friend` 表；每条使用稳定 user id 外键，用户改名不影响链接，删除用户时链接级联删除。整体替换在同一事务内完成。

管理员上传的首页图片写入 Blog API 的统一上传目录；数据库只保存 `/api/image/**` 同源 URL 与顺序。Nginx 将该路径转发给 Blog API 的图片读取接口，因此公开 Blog 和训练管理页面使用同一个文件来源。

## Compose 拓扑

`deploy/docker-compose.yml` 定义恰好四个服务：

| Service | Responsibility |
| --- | --- |
| `blog-db` | MySQL 8.4，统一 Blog/训练 schema |
| `blog-redis` | Redis 7，Blog cache/运行支持 |
| `blog-api` | 唯一 Spring Boot 后端，host port 由 `BACKEND_PORT` 控制 |
| `frontend` | 同时托管 Vue 3 Blog 与 Vue 3 训练中心的 Nginx，host port 由 `FRONTEND_PORT` 控制 |

MySQL 与 Redis 使用新的命名卷；旧数据卷不会被挂载或自动删除。当前仓库提供本地/单机 Compose 配置，不代表已经完成任何服务器发布。
