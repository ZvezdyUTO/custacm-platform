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

`/player/me/profile`、`/player/me/profile-links` 和新的管理接口校验失败时通过统一异常处理返回 HTTP 4xx 与稳定 `errorCode`。部分保留的历史 Blog 读取响应仍允许 `errorCode=null`，客户端不能据此推断 HTTP status。

## 登录与当前用户

| Method | 后端路径 | Access | Description |
| --- | --- | --- | --- |
| POST | `/login` | Guest | 使用 `username`/password 登录，返回 user 与 bearer token；失败后按 username 冷却五秒 |
| GET | `/profiles/{username}` | Guest | 返回文章作者公开名片及其主动开启展示的 `achievements` |
| GET | `/player/me` | Player/Admin | 返回当前用户资料及全部本人 `achievements`，每项带 `profileVisible` |
| PATCH | `/player/me/profile` | Player/Admin | 修改本人 nickname 和/或个性签名，返回含 `achievements` 的完整资料；nickname 1–30 字符，签名最多 40 字符 |
| PUT | `/player/me/profile-links` | Player/Admin | 整体替换本人个人友情链接并返回含 `achievements` 的完整资料；按数组顺序保存，最多 8 条且仅允许 HTTP(S) 绝对地址 |
| GET | `/player/me/oj-handles` | Player/Admin | 仅返回当前用户绑定的 Codeforces/AtCoder handle map；未绑定时返回空对象 |
| POST | `/player/me/avatar` | Player/Admin | 上传前端裁剪后的 512×512 PNG 头像并返回含 `achievements` 的完整资料，multipart 字段为 `file`，最大 2MB |
| POST | `/player/images` | Player/Admin | 上传文章首图或正文图，multipart 字段为 `file`、`purpose` |
| DELETE | `/player/images/{id}` | Player/Admin | 幂等删除当前用户尚未绑定文章的临时图片 |
| PATCH | `/player/me/password` | Player/Admin | 使用旧密码修改本人密码 |
| POST | `/player/comment` | Player/Admin | 登录用户提交 Blog 评论 |
| GET | `/player/blogs` | Player/Admin | 分页查询当前用户的文章 |
| GET | `/player/blogs/recycle-bin` | Player/Admin | 分页查询当前用户仍在七天保留期内的回收站文章 |
| GET | `/player/blog` | Player/Admin | 读取当前用户的指定文章 |
| POST | `/player/blog` | Player/Admin | 以当前用户为作者新建文章 |
| PUT | `/player/blog` | Player/Admin | 修改当前用户的指定文章 |
| DELETE | `/player/blog` | Player/Admin | 将当前用户的指定文章移入固定保留七天的回收站 |
| PUT | `/player/blog/restore?id={id}` | Player/Admin | 在七天保留期内恢复当前用户的指定文章 |
| GET | `/player/blog/download?id={id}` | Player/Admin | 下载已发布文章的 Markdown 与托管图片 ZIP；Player 跨文章 30 秒一次，Admin 不限频 |
| GET | `/admin/blogs` | Admin | 按标题、分类分页查询全部文章，供管理文章页使用 |
| GET | `/admin/blogs/recycle-bin` | Admin | 按标题、分类分页查询仍在七天保留期内的全部回收站文章 |
| GET | `/admin/blogs/backup` | Admin | 下载全部文章状态、评论、作者资料和托管图片的去敏 ZIP 备份 |
| DELETE | `/admin/blog?id={id}` | Admin | 将任意正常文章移入固定保留七天的回收站 |
| PUT | `/admin/blog/restore?id={id}` | Admin | 在七天保留期内恢复任意回收站文章 |
| GET | `/admin/homepage-featured-groups` | Admin | 按顺序返回首页精选组及成员当前可用状态 |
| GET | `/admin/homepage-featured-groups/candidates?query={title}` | Admin | 按标题搜索当前公开、已发布且未回收的候选文章 |
| POST | `/admin/homepage-featured-groups` | Admin | 创建一个固定三篇文章的精选组 |
| PUT | `/admin/homepage-featured-groups/{id}` | Admin | 整体替换一个精选组的标题和三篇文章 |
| DELETE | `/admin/homepage-featured-groups/{id}` | Admin | 删除精选组，不删除文章 |
| PUT | `/admin/homepage-featured-groups/order` | Admin | 用当前全部组 ID 整体替换组顺序 |
| GET | `/health` | Guest | 进程健康检查 |

JWT 的 `sub` 是 `username`，角色只允许 `ROLE_admin` 和 `ROLE_player`。

Vue Blog 在 `/profile` 展示“我的主页”并分页消费 `GET /player/blogs`；旧 `/about` 页面和 API 已删除。`/write` 发布文章，`/write/{id}` 编辑本人文章；Markdown 文件仅在浏览器中读取，文章图片则通过 `/player/images` 单独上传。

`POST /login` 在 BCrypt 校验前按规范化 username 获取一个 Redis 五秒原子占位。正确凭据释放占位并签发 JWT；用户名或密码错误保留占位，首次返回 HTTP 401、`errorCode=AUTH_BAD_CREDENTIALS` 与 `Retry-After: 5`，五秒内重复请求返回 HTTP 429、`errorCode=AUTH_LOGIN_COOLDOWN` 和剩余 `Retry-After`。Redis 无法判定或释放窗口时返回 HTTP 503 与 `AUTH_LOGIN_COOLDOWN_UNAVAILABLE`。两种凭据错误使用相同响应，不暴露用户名是否存在。

`GET /player/blog/download?id={id}` 返回 `application/zip` attachment，不使用 Blog `Result` envelope。ZIP 直接包含 `article.md`、文章元数据和已绑定的本地托管首图/正文图，不附带额外 README；`article.md` 依次包含标题、简介和原始正文，Markdown 中对应 URL 改为包内相对路径。图片使用 `cover-original.jpg`、`content-1-original.jpg` 等扁平语义化文件名，外站图片 URL 原样保留且服务端不会抓取。它允许登录用户下载公开或内部的已发布文章，不允许下载草稿。普通用户按 JWT 当前 username 在全部文章下载间共享一个 30 秒固定窗口，切换文章不能绕过；重复请求返回 HTTP 429、`errorCode=ARTICLE_DOWNLOAD_RATE_LIMITED` 和 `Retry-After` 剩余秒数。管理员不检查该窗口。普通用户无法访问 Redis 限流状态时返回 HTTP 503 和 `ARTICLE_DOWNLOAD_RATE_LIMIT_UNAVAILABLE`。

`GET /admin/blogs/backup` 返回固定短 ASCII 文件名的 `application/zip` attachment。范围覆盖数据库中仍存在的已发布、内部、草稿和七天回收站文章；每篇文章目录包含带标题/简介的 Markdown、状态/分类/标签元数据、评论父子关系及扁平命名的托管图片，根目录汇总文章作者资料与按作者目录扁平保存的托管头像，不附带额外 README。备份不导出密码哈希、token、评论 IP/邮箱/通知字段，也不抓取外站图片；缺失的本地托管文件记录在 `manifest.json`。

`GET /player/me`、资料修改、个人友链整体替换和头像更新都返回本人资料 DTO，字段为 `username`、`nickname`、`email`、`avatar`、`avatarOriginalUrl`、`signature`、`role`、`links` 和 `achievements`。本人响应中的 `achievements` 包含所有正常比赛的本人奖项，并以 `profileVisible` 告知是否公开展示，供个人页编辑。`avatar` 是 96×96 缩略图。每条 link 包含 `id`、`label`、`url`、`sortOrder`；清空友链时提交 `{"links":[]}`。

`GET /profiles/{username}` 是匿名只读的作者名片接口，返回用户主动公开展示的 `username`、`nickname`、`email`、`avatar`、`signature`、`links` 和 `achievements`，其中只包含本人将 `profileVisible` 开启的奖项；不返回 role 或 OJ handle。文章详情左侧名片用文章的 `authorUsername` 调用该接口。

`POST /player/images` 的 `purpose` 只能是 `ARTICLE_COVER` 或 `ARTICLE_CONTENT`。正文 JPEG/PNG 最大 15MB，生成最长边 2560 的高清版和最长边 960 的缩略图；首图必须由前端裁剪为 1920×1080 且最大 10MB。响应包含 `id`、`publicId`、`purpose`、`originalUrl`、`thumbnailUrl`、宽高和两个文件大小。文章写请求通过 `firstPictureAssetId` 绑定首图，后端从 Markdown 中解析本站 UUID 图片并绑定正文资产；资产不可跨文章复用。

文章删除只写入 `deleted_at` 并立即从列表、详情、评论、搜索、精选和下载查询中隐藏；正文、评论、标签与托管图片在固定七天保留期内不变。本人或管理员可在到期前恢复，满七天后定时任务才在同一事务中物理删除关联数据并于提交后回收图片。主动从正文移除托管图、更换首图或头像仍在保存事务提交后立即回收；删除失败的资产进入 `DELETING` 重试，超过 24 小时未绑定的 `TEMP` 资产和孤儿目录由每日任务清理。

## 首页精选分组

首页最多保存三组，每组必须有非空标题和按展示顺序排列的三篇不同文章；同一篇文章不能出现在任何两个组或同组的两个位置。创建和更新请求体相同：

```json
{
  "title": "训练方法与复盘",
  "articleIds": [101, 205, 309]
}
```

服务端在写入时重新校验三篇文章都满足 `published=true`、`internal=false`、`deleted_at=null`，并在同一事务内检查组数上限与全首页唯一性。`PUT /admin/homepage-featured-groups/order` 请求体形如 `{"ids":[3,1,2]}`，必须恰好包含当前全部组 ID 且不能重复。创建、更新、删除和排序成功后都返回最新的有序组列表。

管理员组列表不会因成员后来变为草稿、内部文章或进入回收站而丢失关系。组字段包含 `id`、`title`、`sortOrder`、`complete` 和 `articles`；成员包含 `id`、标题、简介、首图、日期、分类、彩色 `tags`、作者 username/nickname/avatar、`sortOrder` 与 `available`。失效成员以 `available=false` 返回，供 `/training/admin/articles` 的“首页编排”子视图提示管理员替换。候选结果包含同样的文章摘要和 `featuredGroupId`，用于禁用已占用的文章。

旧 `PUT /admin/blog/recommend` 已删除。数据库 `blog.is_recommend` 继续作为历史存储与备份兼容字段存在，但运行时首页查询和后台编排都不读取或修改它。

## 比赛与获奖记录

公开读取和受保护写入路径如下：

| Method | 路径 | Access | Description |
| --- | --- | --- | --- |
| GET | `/competitions` | Guest | 按年份闭区间、可选规范分类分页查询正常比赛 |
| GET | `/competitions/{id}` | Guest | 查询一个正常比赛的参赛用户、可见文章、奖项和获奖人 |
| POST | `/admin/competitions` | Admin | 添加比赛 |
| GET | `/admin/competitions/recycle-bin` | Admin | 按同样筛选条件分页查询仍在七天保留期内的比赛根及保留的子关系 |
| DELETE | `/admin/competitions/{id}` | Admin | 仅将正常比赛根移入七天回收站，子关系原样保留 |
| PUT | `/admin/competitions/{id}/restore` | Admin | 恢复仍在保留期内且名称不冲突的比赛 |
| POST | `/admin/competitions/{id}/participants` | Admin | 批量添加比赛参赛用户 |
| DELETE | `/admin/competitions/{id}/participants/{participantId}` | Admin | 删除一个比赛参赛记录 |
| POST | `/admin/competitions/{id}/awards` | Admin | 添加奖项并绑定一个或多个当前比赛参赛用户 |
| DELETE | `/admin/competitions/{id}/awards/{awardId}` | Admin | 删除一个奖项及其获奖人关联 |
| POST | `/player/competitions/{competitionId}/articles/{blogId}` | Player/Admin | 参赛用户绑定本人当前公开且已发布文章 |
| DELETE | `/player/competitions/{competitionId}/articles/{blogId}` | Player/Admin | 参赛用户解绑本人文章 |
| PUT | `/player/competitions/{competitionId}/awards/{awardId}/profile-visibility` | Player/Admin | 获奖人修改本人名片是否展示该奖项 |
| PUT | `/player/competitions/achievement-order` | Player/Admin | 获奖人重排本人当前全部公开奖项 |

`GET /competitions` 和回收站列表接受 `startYear`、`endYear`、`category`、`pageNum`、`pageSize`。年份上下界均包含端点且只接受 1900–9999；只传一端时应用单侧过滤；同时传入时要求 `startYear <= endYear`。`category` 是单个可选规范分类代码，组合匹配在数据库分页前完成。`pageNum` 从 1 开始，`pageSize` 允许 1–100；分页响应字段为 `pageNum`、`pageSize`、`total`、`totalPages` 和 `list`。

比赛业务分类是互斥单选，稳定代码如下：

| `category` | Label | 固定参赛形态 |
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

响应仍包含只读 `types` 以兼容历史记录，但创建请求不接受标签数组或任意组合。

添加比赛请求示例：

```json
{
  "fullName": "2026 ICPC Asia Shanghai Regional Contest",
  "year": 2026,
  "category": "ICPC_ASIA_REGIONAL",
  "participationMode": "TEAM"
}
```

创建比赛时年份同样只接受 1900–9999，`category` 必须为上表之一，固定形态分类的 `participationMode` 必须匹配；省赛可显式选择 `INDIVIDUAL`、`TEAM` 或 `MIXED`。比赛全称在正常记录中唯一。删除后该名称立即释放，可在旧记录七天保留期间创建同名新比赛；旧记录恢复时若已有同名正常比赛，恢复请求拒绝且旧记录继续留在回收站。管理员除恢复外没有比赛、参赛用户或奖项的 PUT/PATCH 接口，更正数据时删除对应记录后重新添加。

批量添加参赛用户使用 `{"usernames":["player1","player2"]}`，单次最多 100 个不重复的已存在 username。参赛关系以 `username` 外键连接账号，用户改名时级联更新；删除账号时 username 置空，但昵称快照、参赛记录和历史奖项继续保留。删除参赛记录前必须先删除仍绑定该用户的奖项。

添加奖项请求字段为：

```json
{
  "awardMode": "TEAM",
  "teamName": "CustACM",
  "awardTier": "MEDAL_GOLD",
  "rankPosition": 3,
  "rankTotal": 280,
  "recipientUsernames": ["player1", "player2"]
}
```

`awardTier` 受比赛分类严格限制：

- 前七个奖牌分类只接受 `MEDAL_GOLD`、`MEDAL_SILVER`、`MEDAL_BRONZE`、`MEDAL_HONORABLE_MENTION`，并强制 `rankPosition`/`rankTotal` 成对满足 `1 <= x <= y`。
- 百度之星只接受 `BAIDU_NATIONAL_FIRST|SECOND|THIRD|FOURTH` 与 `BAIDU_PROVINCIAL_FIRST|SECOND|THIRD`，按国一至国四、省一至省三排列。
- GPLT 国赛与蓝桥杯国奖只接受 `FIRST_PRIZE`、`SECOND_PRIZE`、`THIRD_PRIZE`；这三类普通奖项的两个排名字段必须都为 `null`。

响应返回 `awardTier` 与最终 `awardTierLabel`，旧的 `awardLevel`、`awardScope`、`awardName` 仅为存储和旧客户端兼容。`awardMode` 只允许 `INDIVIDUAL`/`TEAM`；个人奖必须且只能绑定一名当前比赛参赛用户且不能填写队名，团队奖至少绑定一名参赛用户、不限制队伍人数并可选填写队名。

新获奖人关系默认 `profileVisible=false` 且没有 `profileOrder`，不会直接出现在公开名片。获奖人可调用 `PUT /player/competitions/{competitionId}/awards/{awardId}/profile-visibility`，请求体为 `{"visible":true}` 或 `{"visible":false}`；开启时追加到本人公开序列末尾，关闭时移出序列。`PUT /player/competitions/achievement-order` 使用 `{"orderedAwardIds":[71,85]}`，数组必须无重复且恰好包含当前用户全部公开奖项。后端只按当前 JWT username 更新本人获奖关系；团队奖各成员独立选择，管理员通过 player 路径也不能替其他人设置。比赛公开详情中的获奖人事实不受这些偏好影响。

比赛删除后立即从两个公开比赛接口、玩家文章绑定目标和全部个人 `achievements` 中隐藏，但参赛用户、奖项、获奖人和文章关联固定保留七天；管理员回收站响应仍装配这些子关系和其中当前公开已发布的文章。恢复后聚合在公开端原样重现，到期才级联物理清理。已绑定文章后来变为草稿、内部文章或进入文章回收站时，只从比赛响应的文章列表隐藏；重新公开或恢复文章后无需重绑即可重现。

本人资料响应中的每项 `achievements` 包含 `competitionId`、`competitionFullName`、`year`、`category`/`categoryLabel`、只读兼容 `types`、`awardId`、奖项形态/标签、可选团队名、`awardTier`/`awardTierLabel`、可空排名、`profileVisible` 和 `profileOrder`；公开资料只按 `profileOrder` 返回 `profileVisible=true` 的项目。已删除比赛的获奖记录不返回，恢复后沿用删除前的展示选择与顺序；删除用户账号不会抹除其他公开比赛详情中的昵称快照和历史获奖事实。

## 用户管理

以下路径均要求 `ROLE_admin`：

| Method | 路径 | Description |
| --- | --- | --- |
| POST | `/admin/users:batch-create` | 在一个事务中创建 JSON 用户数组 |
| GET | `/admin/users` | 列出用户、OJ handle 及各 OJ 最近成功采集窗口结束时间 |
| PUT | `/admin/users/{username}` | 原子更新 `newUsername`、nickname、email、role、password、完整 `handles` 与 `needCollect`；不接受头像 URL |
| DELETE | `/admin/users/{username}` | 清理训练数据、保留作者内容和比赛昵称快照/历史奖项并删除用户；立即回收该用户未被保留文章引用的托管图片 |

`username` 会 trim，长度为 1–128，可包含 Unicode 字母、数字、`.`、`_` 和 `-`。最后一个管理员不能被删除或降级。创建时省略 password 会一次性返回生成密码；PUT 传空 password 会一次性返回重置密码。改名或改密响应包含 `reloginRequired=true`。非 `root` 更新必须同时提交完整 `handles` 与 `needCollect`，避免分接口写入造成账号与训练身份不一致。

当 PUT 改变或移除已有 handle 时，服务会先按旧绑定永久清理对应 OJ 的 ODS、DWD、DWM、DWS 数据和采集游标，再精确替换完整绑定集合；账号字段、清理和换绑处于同一事务，失败时整体回滚。handle 比较保持大小写精确。

管理员用户响应中的 `collectionStates` 按 OJ 名称只返回 `lastCollectedAt`，表示该用户在该 OJ 最近一次成功采集所覆盖窗口的结束时间；从未成功采集时为 `null`。手动采集请求的 `lookbackHours` 表示从该时间向前倒退的重叠小时数，采集上界为本次任务开始时间；`lastCollectedAt=null` 时忽略倒退小时数并抓取全部历史。批量请求按用户、按 OJ 分别计算窗口，失败用户不推进游标，成功但窗口内没有提交的用户仍推进游标。

## 训练用户目录

分类管理 DTO 包含可自定义十六进制 `color`。标签管理界面只允许新增和删除；新增标签由服务端从连续 HSB 数值空间生成深色随机十六进制颜色并持久化，前台标签云始终以白字展示。

`GET /player/training-data/users` 要求 `ROLE_player` 或 `ROLE_admin`。它默认只列出 `needCollect=true` 且至少绑定一个 OJ 账号的用户；可选查询参数 `includeRetired=true` 会取消 `needCollect` 过滤并保留已退役用户。结果按 `username` 排序。

系统保留账号 `root` 不允许通过管理员 API 删除、改名、降为 player 或更新 OJ handle/采集状态。

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
- `GET /player/training-data/accepted-summaries`
- `GET /player/training-data/submissions/by-user`
- `GET /player/training-data/submissions/by-problem`
- `GET /player/training-data/first-accepted/by-user`
- `GET /player/training-data/first-accepted/by-problem`

用户维度接口使用 `username`；适用的接口使用 `ojName`。`accepted-summaries` 以当前用户目录为范围一次返回该 OJ 的全部成员汇总，供多人页避免 N+1 请求，并支持 `includeRetired`。日期、难度、题目和分页参数由无 Spring MVC 依赖的训练查询 facade 统一校验，HTTP 参数绑定与 `Result` 包装只存在于 `top.naccl` Controller。

## Admin 训练操作

以下接口均要求 `ROLE_admin`：

- `POST /admin/training-data/submission-collection-jobs`
- `GET /admin/training-data/submission-collection-jobs`
- `GET /admin/training-data/submission-collection-jobs/{jobId}`

训练中心管理区提供“创建用户”“管理用户”“管理文章”“分类与标签”“比赛与奖项”“数据采集”“首页图片”七个独立页面；比赛页消费本节公开与管理员比赛接口，维护参赛用户、奖项和七天回收站；`/training/admin/articles` 在同一路由内提供“首页编排”、当前文章和回收站子视图，可管理精选组、恢复文章并下载全量去敏 ZIP 备份；数据采集页调用采集任务接口，并始终请求在采集完成后刷新数仓。未被前端使用的同步采集、原始 ODS 写入和独立 warehouse 刷新 HTTP 入口已删除，应用服务仍由采集任务内部复用。

## 首页图片

首页动态图片只保留按 `sortOrder` 从左到右排列的滚动精选图片；首屏首图是 Blog 构建内置静态资源，不经过 API：

| Method | 路径 | Access | Description |
| --- | --- | --- | --- |
| GET | `/homepage-featured-images` | Guest | 一次返回全部精选图片，字段为 `id`、原图 `imageUrl`、压缩图 `thumbnailUrl`、`sortOrder` |
| GET | `/admin/homepage-featured-images` | Admin | 返回管理员精选图片列表，最多十二张 |
| POST | `/admin/homepage-featured-images` | Admin | 上传裁剪后的 1200×800 JPEG，multipart 字段为 `file` |
| PUT | `/admin/homepage-featured-images/order` | Admin | 按请求体 `{"ids":[3,1,2]}` 替换完整顺序 |
| DELETE | `/admin/homepage-featured-images/{id}` | Admin | 删除一张精选图片并压紧剩余顺序，可删除至空列表 |

管理页面按 3:2 裁剪并导出为 1200×800 JPEG；后端限制 10MB 和最多十二张。每次上传同时保存 `/api/image/homepage-featured-<uuid>.jpg` 原图和 `/api/image/homepage-featured-<uuid>-thumbnail.jpg` 720×480 压缩缩略图；旧记录首次读取时补生成缩略图。精选图片允许为空，公开接口始终一次返回完整有序列表。排序要求包含当前全部 ID 且不能重复。旧 `/homepage-banners`、`/admin/homepage-banners/**` 和 `homepage_banner_image` 表已删除。

## 公开 Blog API

文章、分类、标签、比赛与获奖记录、首页图片、作者资料及评论列表等公开 GET 请求不要求登录。About、全站友链和动态页面及其 API 已删除。评论列表仅在评论仍关联现存账号时返回只读 `username`；历史游客评论或账号删除后的匿名评论不返回该身份。新评论中的表情以标准 Unicode 原样存储，Google Noto Emoji 只由 Blog 展示层通过同源 SVG sprite 渲染；历史短码继续只读兼容。没有公开 OJ handle map、guest 训练查询、独立 handle 管理 API 或独立用户训练数据删除 API；删除用户时由用户服务在内部编排清理。

`GET /site` 只返回 Blog 外壳初始化仍使用的数据：`siteInfo.reward`、`siteInfo.commentAdminFlag`、文章详情作者兜底所需的 `introduction`、`categoryList`、`tagList` 和 `featuredGroups`。`featuredGroups` 按后台保存顺序最多返回三组，每组字段为 `id`、`title`、`sortOrder`、恒为 `true` 的 `complete` 和固定三项 `articles`；文章字段包含 `id`、`title`、`description`、`firstPicture`、`createTime`、`categoryName`、`authorUsername`、`authorNickname`、`authorAvatar`、`sortOrder` 及恒为 `true` 的 `available`。未手填简介时自动使用正文前 280 个字符生成摘要，正文也为空时返回“暂无简介”。即使请求携带登录用户的可选 Bearer，精选组仍只投影公开、已发布且未回收文章；任何成员失效时整组暂不返回，关系恢复资格后自动重现。它不再返回旧 `featuredBlogList`、`badges`、社交链接、滚动文字和收藏配置。

以下示例只展开每组第一篇文章；实际响应的 `articles` 固定包含三项。

```json
{
  "featuredGroups": [
    {
      "id": 1,
      "title": "精选文章",
      "sortOrder": 0,
      "complete": true,
      "articles": [
        {
          "id": 101,
          "title": "比赛复盘",
          "description": "……",
          "firstPicture": "/api/image/assets/example/thumbnail",
          "createTime": "2026-07-14T10:00:00",
          "categoryName": "训练",
          "authorUsername": "player1",
          "authorNickname": "队员一",
          "authorAvatar": "/api/image/assets/avatar/thumbnail",
          "sortOrder": 0,
          "available": true
        }
      ]
    }
  ]
}
```

`GET /searchBlog?query={keyword}` 只对已发布文章标题做大小写不敏感的子串匹配，按更新时间倒序返回最多十条包含 `id`、`title` 和文章 `description` 的候选。游客只获得公开文章；登录用户显式携带 Bearer 时也获得内部文章。正文不参与搜索。空关键词、特殊通配字符或超过 20 个字符的关键词会返回参数错误。

文章列表和详情不返回或维护阅读数；读取文章不会产生计数写入，旧 `blog.views` 列及其 Redis 定时同步任务由迁移移除。文章字数和估算阅读时长仍保留。

文章可见性由 `published` 与 `internal` 共同表达：`published=false` 是仅作者和管理员管理的草稿；`published=true, internal=false` 是公开文章；`published=true, internal=true` 是内部文章。游客的列表、分类、标签和搜索排除内部文章；已登录用户在这些聚合接口中会看到内部文章，但正文仍通过 `GET /player/internal-blog?id={id}` 阅读，并通过 `GET /player/comments` 读取评论。首页 `featuredGroups` 对所有访问者都只包含公开文章，登录不会扩大其可见范围。评论提交统一要求登录。密码文章及文章密码 token 已移除。

`GET /blog?id={id}` 的文章详情包含原始纯文本 `description`，以及 `authorUsername`、`authorNickname` 和 `authorAvatar`；内部文章详情返回相同字段。`authorUsername` 用于展示作者自己的编辑入口，不是写接口的授权依据。

`GET /blogs`、按分类和按标签查询的公开文章列表同样包含 `authorUsername`、`authorNickname` 和 `authorAvatar`，供 Vue Blog 的文章摘要卡展示作者身份。
