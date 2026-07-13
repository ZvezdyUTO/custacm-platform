# Blog API

Blog API 是项目唯一后端。默认本地直连地址为 `http://localhost:8090`；浏览器通过前端 Nginx 的 `/api/**` 访问，Nginx 会去掉 `/api` 前缀再转发。因此：

```text
浏览器 GET /api/player/me
后端   GET /player/me
```

响应使用 Blog envelope：

```json
{"code": 200, "errorCode": null, "msg": "ok", "data": {}}
```

客户端不能只用 HTTP 2xx 判断成功。Vue 3 训练中心的 `requestData` 同时要求 HTTP status 成功且 `Result.code == 200`；其它客户端也应同时检查 transport status 与 envelope code。

部分 NBlog 兼容路径仍保留旧响应语义。例如 `/player/me/nickname` 的 PATCH 参数校验可能返回 HTTP 200，同时 envelope 为 `code=400`、`errorCode=null`；`/admin/login` 的旧 filter 错误响应也不保证 `errorCode`。新的 `/player/me/profile` 与 `/player/me/profile-links` 校验失败时通过统一异常处理返回 HTTP 400 和 `errorCode=BAD_REQUEST`。稳定 `errorCode` 只适用于显式统一的鉴权与新错误处理分支，其余响应必须允许 `errorCode=null`，不能据此推断 HTTP status。

## 登录与当前用户

| Method | 后端路径 | Access | Description |
| --- | --- | --- | --- |
| POST | `/login` | Guest | 使用 `username`/password 登录，返回 user 与 bearer token |
| POST | `/admin/login` | Guest | 兼容入口，与 `/login` 使用同一账号体系 |
| GET | `/profiles/{username}` | Guest | 返回文章作者公开名片：username、nickname、avatar、signature、links |
| GET | `/player/me` | Player/Admin | 返回数据库中的当前用户摘要 |
| PATCH | `/player/me/profile` | Player/Admin | 修改本人 nickname 和/或个性签名，返回完整资料；nickname 1–30 字符，签名最多 160 字符 |
| PUT | `/player/me/profile-links` | Player/Admin | 整体替换本人个人友情链接，按数组顺序保存，最多 8 条且仅允许 HTTP(S) 绝对地址 |
| GET | `/player/me/oj-handles` | Player/Admin | 仅返回当前用户绑定的 Codeforces/AtCoder handle map；未绑定时返回空对象 |
| PATCH | `/player/me/nickname` | Player/Admin | 修改本人昵称的兼容接口 |
| POST | `/player/me/avatar` | Player/Admin | 上传前端裁剪后的 512×512 PNG 头像，multipart 字段为 `file`，最大 2MB |
| POST | `/player/images` | Player/Admin | 上传文章首图或正文图，multipart 字段为 `file`、`purpose` |
| DELETE | `/player/images/{id}` | Player/Admin | 幂等删除当前用户尚未绑定文章的临时图片 |
| PATCH | `/player/me/password` | Player/Admin | 使用旧密码修改本人密码 |
| POST | `/player/comment` | Player/Admin | 登录用户提交 Blog 评论 |
| GET | `/player/blogs` | Player/Admin | 分页查询当前用户的文章 |
| GET | `/player/blog` | Player/Admin | 读取当前用户的指定文章 |
| POST | `/player/blog` | Player/Admin | 以当前用户为作者新建文章 |
| PUT | `/player/blog` | Player/Admin | 修改当前用户的指定文章 |
| DELETE | `/player/blog` | Player/Admin | 删除当前用户的指定文章 |
| POST | `/admin/blog` | Admin | 新建文章，作者由当前认证管理员决定 |
| GET | `/admin/blogs` | Admin | 按标题、分类分页查询全部文章，供管理文章页使用 |
| PUT | `/admin/blog/recommend?id={id}&recommend={boolean}` | Admin | 设置或取消首页侧栏精选文章 |
| GET | `/health` | Guest | 进程健康检查 |

JWT 的 `sub` 是 `username`，角色只允许 `ROLE_admin` 和 `ROLE_player`。

Vue Blog 将“个人资料”统一命名为“我的主页”，并在 `/about` 内分页消费 `GET /player/blogs`。`/write` 发布文章，`/write/{id}` 编辑本人文章；Markdown 文件仅在浏览器中读取，文章图片则通过 `/player/images` 单独上传。

`GET /player/me`、资料修改、个人友链整体替换和头像更新都返回本人资料 DTO，字段为 `username`、`nickname`、`email`、`avatar`、`avatarOriginalUrl`、`signature`、`role` 和 `links`。`avatar` 是 96×96 缩略图。每条 link 包含 `id`、`label`、`url`、`sortOrder`；清空友链时提交 `{"links":[]}`。

`GET /profiles/{username}` 是匿名只读的作者名片接口，返回用户主动公开展示的 `username`、`nickname`、`email`、`avatar`、`signature` 和 `links`，不返回 role 或 OJ handle。文章详情左侧名片用文章的 `authorUsername` 调用该接口。

`POST /player/images` 的 `purpose` 只能是 `ARTICLE_COVER` 或 `ARTICLE_CONTENT`。正文 JPEG/PNG 最大 15MB，生成最长边 2560 的高清版和最长边 960 的缩略图；首图必须由前端裁剪为 1920×1080 且最大 10MB。响应包含 `id`、`publicId`、`purpose`、`originalUrl`、`thumbnailUrl`、宽高和两个文件大小。文章写请求通过 `firstPictureAssetId` 绑定首图，后端从 Markdown 中解析本站 UUID 图片并绑定正文资产；资产不可跨文章复用。

删除文章、从正文移除托管图、更换首图或头像后，失效文件在事务提交后立即删除。删除失败的资产进入 `DELETING` 重试；超过 24 小时未绑定的 `TEMP` 资产和孤儿目录由每日任务清理。历史外链和旧 `/api/image/**` URL 继续兼容。

## 用户管理

以下路径均要求 `ROLE_admin`：

| Method | 路径 | Description |
| --- | --- | --- |
| POST | `/admin/users` | 创建一个用户，可同时提供 `handles` 和 `needCollect` |
| POST | `/admin/users:batch-create` | 在一个事务中创建 JSON 用户数组 |
| GET | `/admin/users` | 列出用户、OJ handle 及各 OJ 最近成功采集窗口结束时间 |
| GET | `/admin/users/{username}` | 查询一个用户及其 OJ 采集状态 |
| PATCH | `/admin/users/{username}` | 修改 `newUsername`、nickname、email、role 或 password；不接受头像 URL |
| DELETE | `/admin/users/{username}` | 清理训练数据、保留作者内容并删除用户；立即回收该用户未被保留文章引用的托管图片 |
| PUT | `/admin/users/{username}/oj-handles` | 首次绑定 OJ handle 或更新 `needCollect`；已有 handle 不允许通过该接口覆盖，允许 `handles={}` 单独保存现役/退役状态 |
| POST | `/admin/users/{username}/oj-handles:replace` | 高危更换单个 OJ handle；先按旧 handle 永久清理该用户该 OJ 的 ODS、DWD、DWM、DWS 数据与旧采集状态，再绑定 `newHandle` |

`username` 会 trim，长度为 1–128，可包含 Unicode 字母、数字、`.`、`_` 和 `-`。最后一个管理员不能被删除或降级。创建时省略 password 会一次性返回生成密码；PATCH 传空 password 会一次性返回重置密码。改名响应包含 `reloginRequired=true`。

管理员用户响应中的 `collectionStates` 按 OJ 名称只返回 `lastCollectedAt`，表示该用户在该 OJ 最近一次成功采集所覆盖窗口的结束时间；从未成功采集时为 `null`。手动采集请求的 `lookbackHours` 表示从该时间向前倒退的重叠小时数，采集上界为本次任务开始时间；`lastCollectedAt=null` 时忽略倒退小时数并抓取全部历史。批量请求按用户、按 OJ 分别计算窗口，失败用户不推进游标，成功但窗口内没有提交的用户仍推进游标。

## 训练用户目录

分类管理 DTO 包含可自定义十六进制 `color`。标签管理界面只允许新增和删除；新增标签由服务端从连续 HSB 数值空间生成深色随机十六进制颜色并持久化，前台标签云始终以白字展示。

`GET /player/training-data/users` 要求 `ROLE_player` 或 `ROLE_admin`。它默认只列出 `needCollect=true` 且至少绑定一个 OJ 账号的用户；可选查询参数 `includeRetired=true` 会取消 `needCollect` 过滤并保留已退役用户。结果按 `username` 排序。

系统保留账号 `root` 不允许通过管理员 API 删除、改名、降为 player 或更新 OJ handle/采集状态。

更换请求体为 `{ "ojName": "CODEFORCES|ATCODER", "newHandle": "..." }`。若旧 handle 未绑定、目标 handle 已归属其他用户或参数无效，请求失败；清理与换绑位于同一事务中，失败时整体回滚。管理前端必须在调用前展示不可恢复的数据清理警告并取得明确确认。

浏览器路径与直连路径：

```text
GET /api/player/training-data/users
GET /player/training-data/users
GET /api/player/training-data/users?includeRetired=true
```

响应 `data` 中每项字段严格为：

```json
{
  "username": "player1",
  "nickname": "队员一",
  "ojNames": ["CODEFORCES", "ATCODER"]
}
```

该目录不返回 email、role、真实 OJ handle、采集状态或管理员私有字段。

## Player 训练查询

以下接口均要求 `ROLE_player` 或 `ROLE_admin`：

- `GET /player/training-data/users`
- `GET /player/training-data/accepted-summary`
- `GET /player/training-data/submissions/by-user`
- `GET /player/training-data/submissions/by-problem`
- `GET /player/training-data/first-accepted/by-user`
- `GET /player/training-data/first-accepted/by-problem`

用户维度接口使用 `username`；适用的接口使用 `ojName`。日期、难度、题目和分页参数由无 Spring MVC 依赖的训练查询 facade 统一校验，HTTP 参数绑定与 `Result` 包装只存在于 `top.naccl` Controller。

## Admin 训练操作

以下接口均要求 `ROLE_admin`：

- `POST /admin/training-data/submissions:collect`
- `POST /admin/training-data/submission-collection-jobs`
- `GET /admin/training-data/submission-collection-jobs`
- `GET /admin/training-data/submission-collection-jobs/{jobId}`
- `POST /admin/training-data/ods/codeforces/submissions:batch-upsert`
- `POST /admin/training-data/{ojName}/warehouse:refresh`

原始 Codeforces batch-upsert 是后端 API，不在当前 Vue 3 管理页面中暴露。训练中心管理区提供“创建用户”“管理用户”“管理文章”“数据采集”“首页图片”五个独立页面；管理文章页控制公开侧栏精选状态，数据采集页调用采集任务接口，并始终请求在采集完成后刷新数仓。

## 首页图片

公开首页按 `sortOrder` 从左到右读取图片：

| Method | 路径 | Access | Description |
| --- | --- | --- | --- |
| GET | `/homepage-banners` | Guest | 返回全部首页图片，字段为 `id`、`imageUrl`、`sortOrder` |
| GET | `/admin/homepage-banners` | Admin | 返回管理员首页图片列表 |
| POST | `/admin/homepage-banners` | Admin | 上传裁剪后的图片，multipart 字段为 `file` |
| PUT | `/admin/homepage-banners/order` | Admin | 按请求体 `{"ids":[3,1,2]}` 替换完整顺序 |
| DELETE | `/admin/homepage-banners/{id}` | Admin | 删除一张图片并压紧剩余顺序 |

新建或升级数据库后默认只保留构建内置的 `/img/homepage-banner-default.png`。管理页面在浏览器内按 16:9 裁剪并导出为 1920×1080 JPEG；后端再次校验格式、尺寸和 10MB 上限。首页只允许一至两张图片，达到两张后前端隐藏新增入口，后端拒绝额外上传。排序请求必须恰好包含当前全部 ID 且不能重复。管理员上传文件保存在 Blog API 的上传目录，`imageUrl` 使用浏览器同源的 `/api/image/**` 路径。

## 公开 Blog API

文章、分类、标签、动态、友链、关于页、首页图片及评论列表等公开 GET 请求不要求登录。评论列表仅在评论仍关联现存账号时返回只读 `username`；游客评论或账号删除后的匿名评论不返回该身份。没有公开 OJ handle map、guest 训练查询、独立 handle 管理 API 或独立用户训练数据删除 API；删除用户时由用户服务在内部编排清理。

`GET /site` 只返回 Blog 外壳初始化仍使用的数据：`siteInfo.reward`、`siteInfo.commentAdminFlag`、`introduction.avatar`、`introduction.name`、`categoryList`、`tagList` 和 `featuredBlogList`。它不再查询或返回未展示的 `newBlogList`，也不再返回旧 `badges`、社交链接、滚动文字和收藏配置。后台站点设置管理数据仍保留，不受该公开响应收窄影响。

`GET /searchBlog?query={keyword}` 只对已发布文章标题做大小写不敏感的子串匹配，按更新时间倒序返回最多十条包含 `id`、`title` 和文章 `description` 的候选。游客只获得公开文章；登录用户显式携带 Bearer 时也获得内部文章。正文不参与搜索。空关键词、特殊通配字符或超过 20 个字符的关键词会返回参数错误。

文章列表和详情不返回或维护阅读数；读取文章不会产生计数写入，旧 `blog.views` 列及其 Redis 定时同步任务由迁移移除。文章字数和估算阅读时长仍保留。

文章可见性由 `published` 与 `internal` 共同表达：`published=false` 是仅作者和管理员管理的草稿；`published=true, internal=false` 是公开文章；`published=true, internal=true` 是内部文章。游客的列表、分类、标签、搜索和精选接口排除内部文章；已登录用户在同一聚合接口中会看到内部文章，但正文仍通过 `GET /player/internal-blog?id={id}` 阅读，并通过 `GET /player/comments` 读取评论；评论提交统一要求登录。密码文章及文章密码 token 已移除。

`GET /blog?id={id}` 的文章详情包含 `authorUsername`、`authorNickname` 和 `authorAvatar`。`authorUsername` 用于展示作者自己的编辑入口，不是写接口的授权依据。

`GET /blogs`、按分类和按标签查询的公开文章列表同样包含 `authorUsername`、`authorNickname` 和 `authorAvatar`，供 Vue Blog 的文章摘要卡展示作者身份。
