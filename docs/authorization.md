# HTTP Authorization

## 角色

| Identity | Stored role | Access |
| --- | --- | --- |
| Administrator | `ROLE_admin` | Public、player 与 admin routes |
| Team member | `ROLE_player` | Public 与 player routes |
| Guest | 无账号/token | 公开 GET、OPTIONS 预检，以及两个登录 POST |

不存在 `ban`、`disable` 或持久化 guest role。业务身份是 `username`。

## URL 层级

```text
OPTIONS /**                 -> public
POST /login, /admin/login   -> public
/admin/**                   -> ROLE_admin
/player/**                  -> ROLE_admin or ROLE_player
GET /**                     -> public unless matched above
all remaining requests      -> denied
```

匹配顺序很重要：登录 POST 在 `/admin/**` 之前放行。`POST /login` 与 `POST /admin/login` 是仅有的匿名业务写入口；其余未匹配请求都会被拒绝。`GET /health` 公开，训练查询（包括 `GET /player/training-data/users`）不再是 guest endpoint。

首页读取 `GET /homepage-banners` 属于公开 GET；首页图片的列表管理、上传、排序和删除均位于 `/admin/homepage-banners/**`，只允许 `ROLE_admin`。

本人资料读取和修改位于 `/player/me/**`。`PATCH /player/me/profile` 只能修改当前认证用户的 nickname/个性签名；`PUT /player/me/profile-links` 只能整体替换当前认证用户的个人友情链接。两者都不接受目标 username 或 userId，管理员访问时也只修改管理员自己的资料。

浏览器访问时会在这些后端路径前增加 `/api`，例如 `/api/player/training-data/users`；Nginx 只负责去前缀和转发，不改变权限层级。

## JWT

Blog API 通过 `POST /login` 签发 HS512 bearer token；`POST /admin/login` 是兼容入口。token 包含：

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
- Vue Blog 的公开文章、列表和导航请求不得通过 Axios 全局拦截器附加共享 JWT。
- Vue Blog 的个人页为 `GET /player/me`、`PATCH /player/me/profile`、`PUT /player/me/profile-links` 和 `POST /player/me/avatar` 显式附加 Bearer token；头像文件必须先在浏览器裁剪为 512×512 PNG。
- Vue Blog 公开读取首页图片时不附加 JWT；训练中心首页图片管理页只为 `/admin/homepage-banners/**` 请求显式附加 Bearer token，上传前在浏览器裁剪为 1920×1080 JPEG。
- 登录用户提交评论时，Vue 从共享会话读取 JWT，只为 `POST /player/comment` 显式发送 `Authorization: Bearer <token>`。该请求收到 401 会清理共享会话并转到 `/training/login`；403 与网络错误不清理会话。
- 密码文章的 `blog{id}` token 只用于对应文章/评论读取，并保持原始 header 格式，不得加 Bearer 或当作登录 JWT。
