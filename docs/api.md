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
| GET | `/player/me` | Player/Admin | 返回数据库中的当前用户摘要 |
| PATCH | `/player/me/profile` | Player/Admin | 修改本人 nickname 和/或个性签名，返回完整资料；nickname 1–30 字符，签名最多 160 字符 |
| PUT | `/player/me/profile-links` | Player/Admin | 整体替换本人个人友情链接，按数组顺序保存，最多 8 条且仅允许 HTTP(S) 绝对地址 |
| GET | `/player/me/oj-handles` | Player/Admin | 仅返回当前用户绑定的 Codeforces/AtCoder handle map；未绑定时返回空对象 |
| PATCH | `/player/me/nickname` | Player/Admin | 修改本人昵称的兼容接口 |
| POST | `/player/me/avatar` | Player/Admin | 上传前端裁剪后的 512×512 PNG 头像，multipart 字段为 `file`，最大 2MB |
| PATCH | `/player/me/password` | Player/Admin | 使用旧密码修改本人密码 |
| POST | `/player/comment` | Player/Admin | 登录用户提交 Blog 评论 |
| GET | `/health` | Guest | 进程健康检查 |

JWT 的 `sub` 是 `username`，角色只允许 `ROLE_admin` 和 `ROLE_player`。

`GET /player/me`、资料修改、个人友链整体替换和头像更新都返回不含 email 的本人资料 DTO，字段为 `username`、`nickname`、`avatar`、`signature`、`role` 和 `links`。每条 link 包含 `id`、`label`、`url`、`sortOrder`；清空友链时提交 `{"links":[]}`。资料与友链写接口只从 JWT 身份选择用户，不接受请求体中的 username/userId。

## 用户管理

以下路径均要求 `ROLE_admin`：

| Method | 路径 | Description |
| --- | --- | --- |
| POST | `/admin/users` | 创建一个用户，可同时提供 `handles` 和 `needCollect` |
| POST | `/admin/users:batch-create` | 在一个事务中创建 JSON 用户数组 |
| GET | `/admin/users` | 列出用户及 OJ handle 管理信息 |
| GET | `/admin/users/{username}` | 查询一个用户 |
| PATCH | `/admin/users/{username}` | 修改 `newUsername`、nickname、email、avatar、role 或 password |
| DELETE | `/admin/users/{username}` | 清理训练数据、保留作者内容并删除用户 |
| PUT | `/admin/users/{username}/oj-handles` | 创建/更新 OJ handle 与 `needCollect` |

`username` 会 trim，长度为 1–128，可包含 Unicode 字母、数字、`.`、`_` 和 `-`。最后一个管理员不能被删除或降级。创建时省略 password 会一次性返回生成密码；PATCH 传空 password 会一次性返回重置密码。改名响应包含 `reloginRequired=true`。

## 训练用户目录

`GET /player/training-data/users` 要求 `ROLE_player` 或 `ROLE_admin`。它只列出 `needCollect=true` 且至少绑定一个 OJ 账号的用户，按 `username` 排序。

浏览器路径与直连路径：

```text
GET /api/player/training-data/users
GET /player/training-data/users
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

用户维度接口使用 `username`；适用的接口使用 `ojName`。日期、难度、题目和分页参数以 controller contract 为准。

## Admin 训练操作

以下接口均要求 `ROLE_admin`：

- `POST /admin/training-data/submissions:collect`
- `POST /admin/training-data/submission-collection-jobs`
- `GET /admin/training-data/submission-collection-jobs`
- `GET /admin/training-data/submission-collection-jobs/{jobId}`
- `POST /admin/training-data/ods/codeforces/submissions:batch-upsert`
- `POST /admin/training-data/{ojName}/warehouse:refresh`

原始 Codeforces batch-upsert 是后端 API，不在当前 Vue 3 管理页面中暴露。训练中心管理区提供“创建用户”“管理用户”“数据采集”“首页图片”四个独立页面；数据采集页调用采集任务接口，并始终请求在采集完成后刷新数仓。

## 首页图片

公开首页按 `sortOrder` 从左到右读取图片：

| Method | 路径 | Access | Description |
| --- | --- | --- | --- |
| GET | `/homepage-banners` | Guest | 返回全部首页图片，字段为 `id`、`imageUrl`、`sortOrder` |
| GET | `/admin/homepage-banners` | Admin | 返回管理员首页图片列表 |
| POST | `/admin/homepage-banners` | Admin | 上传裁剪后的图片，multipart 字段为 `file` |
| PUT | `/admin/homepage-banners/order` | Admin | 按请求体 `{"ids":[3,1,2]}` 替换完整顺序 |
| DELETE | `/admin/homepage-banners/{id}` | Admin | 删除一张图片并压紧剩余顺序 |

管理页面在浏览器内按 16:9 裁剪并导出为 1920×1080 JPEG；后端再次校验格式、尺寸和 10MB 上限。首页只允许一至两张图片，达到两张后前端隐藏新增入口，后端拒绝额外上传。排序请求必须恰好包含当前全部 ID 且不能重复。上传文件保存在 Blog API 的上传目录，`imageUrl` 使用浏览器同源的 `/api/image/**` 路径。

## 公开 Blog API

文章、分类、标签、归档、动态、友链、关于页、首页图片及评论列表等公开 GET 请求不要求登录。没有公开 OJ handle map、guest 训练查询、独立 handle 管理 API 或独立用户训练数据删除 API；删除用户时由用户服务在内部编排清理。
