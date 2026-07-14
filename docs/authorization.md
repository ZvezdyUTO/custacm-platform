# HTTP Authorization

## 角色

| Identity | Stored role | Access |
| --- | --- | --- |
| Administrator | `ROLE_admin` | Public、player 与 admin routes |
| Team member | `ROLE_player` | Public 与 player routes |
| Guest | 无账号/token | 公开 GET、OPTIONS 预检，以及 `POST /login` |

不存在 `ban`、`disable` 或持久化 guest role。业务身份是 `username`。

## URL 层级

```text
OPTIONS /**                 -> public
POST /login                 -> public
/admin/**                   -> ROLE_admin
/player/**                  -> ROLE_admin or ROLE_player
GET /**                     -> public unless matched above
all remaining requests      -> denied
```

匹配顺序很重要：`POST /login` 是唯一匿名业务写入口；其余未匹配请求都会被拒绝。`GET /health` 公开，训练查询（包括 `GET /player/training-data/users`）不是 guest endpoint。

匿名 `POST /login` 在凭据校验前按规范化 username 获取 Redis 五秒占位；正确凭据释放，错误凭据保留。首次错误返回 401 与 `Retry-After: 5`，窗口内重复请求返回 429，Redis 状态不可用返回 503。两种凭据错误使用相同响应，前端倒计时只是服务端窗口的呈现，不是授权边界。

首页读取 `GET /homepage-featured-images` 和构建内置静态首图 `/img/homepage-banner-default.png` 都是公开资源；滚动精选图片的列表、上传、排序和删除位于 `/admin/homepage-featured-images/**`，只允许 `ROLE_admin`。旧动态横幅公开/管理员路径已删除。

首页精选通过公开 `GET /site` 的 `featuredGroups` 匿名读取，登录用户也不会因此看到内部文章。精选组列表、候选、创建、整组更新、删除和排序全部位于 `/admin/homepage-featured-groups/**`，只允许 `ROLE_admin`；候选和写入还会校验文章当前已发布、公开且未回收，但这些业务校验不替代 URL 层的管理员授权。

本人资料读取和修改位于 `/player/me/**`。`PATCH /player/me/profile` 只能修改当前认证用户的 nickname/个性签名；`PUT /player/me/profile-links` 只能整体替换当前认证用户的个人友情链接。两者都不接受目标 username 或 userId，管理员访问时也只修改管理员自己的资料。

`GET /profiles/{username}` 位于公开 GET 层，只返回文章作者主动公开展示的头像、nickname、username、email、签名、友情链接和本人已开启 `profileVisible` 的 `achievements`，并按获奖人保存的 `profileOrder` 排序；不暴露 role、密码或 OJ handle，也不提供任何修改能力。`GET /player/me`、本人资料/友情链接修改和头像更新响应返回全部本人获奖记录及各自 `profileVisible`/`profileOrder`，供个人页管理展示偏好与顺序。

`GET /competitions` 与 `GET /competitions/{id}` 位于公开 GET 层，只返回未删除的比赛聚合；年份、规范分类和分页参数只改变查询结果，不改变匿名访问权限。回收站比赛及其个人获奖记录对 Guest、Player 和普通公开查询一律隐藏。

所有比赛管理写入均位于 `/admin/competitions/**`，只允许 `ROLE_admin`。管理员可添加比赛、参赛用户和奖项，可删除这些记录，并可查看/恢复七天回收站比赛；除恢复外不存在比赛、参赛用户或奖项的 PUT/PATCH。公开路径没有匿名比赛写入口。

`POST`/`DELETE /player/competitions/{competitionId}/articles/{blogId}` 要求 `ROLE_player` 或 `ROLE_admin`，并进一步校验当前 JWT username 已是该比赛参赛用户、目标文章属于当前账号。绑定时文章还必须当前已发布且公开；管理员使用 player 路径时也只能操作管理员本人作为参赛用户的文章，不能借角色越权绑定其他作者文章。

`PUT /player/competitions/{competitionId}/awards/{awardId}/profile-visibility` 同样要求 `ROLE_player` 或 `ROLE_admin`，并以当前 JWT username 校验其确实是该奖项获奖人；只能修改本人获奖关系上的名片展示偏好。团队奖成员互不影响，管理员没有代替其他获奖人设置的额外权限。

`PUT /player/competitions/achievement-order` 同样要求 `ROLE_player` 或 `ROLE_admin`。请求必须恰好提交当前 JWT username 的全部公开 award ID，不能遗漏、添加、重复或借管理员角色重排他人项目；服务端验证集合后只更新本人的获奖关系顺序。

Player 文章接口从 JWT 身份选择作者，并以 `blog.user_id` 校验查看、修改、移入回收站和恢复所有权；管理员也通过这些 `/player/blog` 写接口发布自己的文章。管理员可通过 `/admin/**` 管理全部文章、恢复任意仍在七天保留期内的文章，通过 `/admin/homepage-featured-groups/**` 编排首页，并通过 `GET /admin/blogs/backup` 下载包含全部文章状态、去敏评论、作者资料和托管图片的备份。旧 `/admin/blog/recommend` 已移除，`is_recommend` 历史字段不再形成任何首页授权或展示效果。不存在提前物理删除接口。

`GET /player/blog/download?id={id}` 只归档已发布文章，ZIP 的 `article.md` 包含标题、简介和原始正文，本地托管图片使用扁平语义化文件名；公开文章和内部文章均要求登录，草稿不进入下载查询。`ROLE_player` 按当前 username 在全部文章下载间共享 30 秒窗口，下载不同文章也使用同一个窗口；`ROLE_admin` 豁免，游客由 `/player/**` 规则返回 401。限流命中返回 429 和 `Retry-After`，限流状态不可用时普通用户返回 503。

`POST /player/images` 和 `DELETE /player/images/{id}` 只操作当前 JWT 用户的文章图片。托管图片只能绑定当前用户的一篇文章，不能把另一用户或另一文章的资产 ID/URL 重新绑定。`/api/image/assets/{uuid}/**` 使用不可猜测 UUID 公开读取，供草稿预览、公开文章和 Nginx 静态缓存使用。

Vue Blog 只为“我的主页”文章列表、回收站、发布、编辑、删除和恢复显式附加 Bearer JWT。公开文章详情返回 `authorUsername` 用于决定是否展示编辑入口；该字段不替代 `/player/blog` 的服务端所有权校验。

浏览器访问时会在这些后端路径前增加 `/api`，例如 `/api/player/training-data/users`；Nginx 只负责去前缀和转发，不改变权限层级。用户目录的可选 `includeRetired=true` 只改变成员过滤范围，不改变 `/player/**` 的认证要求。

比赛参赛关系以 `username` 外键跟随账号改名，不能通过请求体冒充其他身份。删除账号会清空关系中的 username，但保留昵称快照、参赛关系和历史奖项；已注销用户不能再调用文章绑定接口，保留记录也不会赋予任何认证能力。

## JWT

Blog API 仅通过 `POST /login` 签发 HS512 bearer token。token 包含：

```text
sub         = username
authorities = ROLE_admin or ROLE_player
exp         = expiration instant
```

每个 `/admin/**` 或 `/player/**` 请求都会校验签名/有效期，再用 `sub` 从 `user` 表加载当前用户。授权采用数据库当前角色，而不是 token 中可能过期的 `authorities` claim。

- 缺失 token：受保护路径返回 HTTP 401。
- 无效/过期 token，或用户已删除、已改名：HTTP 401。
- 已认证但角色不满足：HTTP 403。

## 前端会话与路由保护

- Vue 3 训练中心与 Vue Blog 共享 `custacm.accessToken` 和 `custacm.user`；摘要不能替代服务端授权。
- 训练中心访问训练查询时要求已恢复的有效会话；`/training/admin` 还要求 `ROLE_admin`。未经认证会转到 `/training/login`，回跳路径只能来自固定白名单。
- 训练中心的 protected API adapter 为每个请求显式附加 Bearer token；只有后端 401 会清理会话，403 和网络失败不会自动退出。
- Vue Blog 的公开文章、列表和导航请求不得通过 Axios 全局拦截器附加共享 JWT；文章列表、分类、标签和搜索读取会在存在共享会话时由各自 API adapter 显式附带 Bearer，以便获得内部文章。`GET /site` 即使携带可选 Bearer，`featuredGroups` 仍只返回公开文章；其余公开读取保持匿名。
- Vue Blog 在文章详情页匿名读取 `/profiles/{authorUsername}` 显示文章作者名片；非文章页面仍显示共享会话中的当前用户名片。
- Vue Blog 的个人页为 `GET /player/me`、`PATCH /player/me/profile`、`PUT /player/me/profile-links`、`POST /player/me/avatar`、奖项展示偏好 PUT 和公开顺序 PUT 显式附加 Bearer token；本人资料响应附带全部 `achievements` 及 `profileVisible`/`profileOrder`，头像文件必须先在浏览器裁剪为 512×512 PNG。
- 头像只能通过 `POST /player/me/avatar` 上传本地图片更新；管理员创建/修改用户接口不接受 avatar 字段或外部头像 URL。
- 固定 `root` 系统管理员不可删除、改名、降权或绑定 OJ handle；其身份不属于现役/退役队员状态。
- 管理员通过单个 `PUT /admin/users/{username}` 原子更新账号、角色、密码、现役状态和完整 OJ handle 集合；改变或移除已有 handle 会先永久清理对应 OJ 的训练数据，任一步失败则整体回滚。
- Vue Blog 公开读取滚动精选图片时不附加 JWT；训练中心首页图片管理页只为 `/admin/homepage-featured-images/**` 请求显式附加 Bearer token，上传前在浏览器裁剪为 1200×800 JPEG。
- 登录用户提交评论时，Vue 从共享会话读取 JWT，只为 `POST /player/comment` 显式发送 `Authorization: Bearer <token>`。该请求收到 401 会清理共享会话并转到 `/training/login`；403 与网络错误不清理会话。表情作为标准 Unicode 文本提交，客户端不得发送 Noto SVG/HTML。
- 文章编辑与评论 401 跳转会携带经白名单校验的同源 Blog `returnTo`，登录成功后返回原文章或编辑页。
- 单篇文章下载 adapter 显式携带 Bearer，并保留 401 清理会话、429 消费 `Retry-After` 和 503 不清理会话的处理；文章阅读页在左侧作者名片向登录用户展示下载入口，编辑入口仅向当前文章作者展示。
- 文章不再支持独立密码或文章 token。内部文章对游客不进入列表、分类、标签或搜索；登录用户会在这些聚合读取中看到它们，正文仍通过 `GET /player/internal-blog?id={id}` 读取，评论通过 `GET /player/comments` 读取。内部文章永远不能进入首页 `featuredGroups`，无论请求者是否登录。
