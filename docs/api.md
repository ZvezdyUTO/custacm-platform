# HTTP API

`blog-api` 是唯一后端。浏览器请求使用 `/api/**`，Nginx 去掉 `/api` 后转发；直连后端时路径不带 `/api`。

多数 JSON 响应使用：

```json
{"code": 200, "errorCode": null, "msg": "ok", "data": {}}
```

客户端应同时检查 HTTP status 与 envelope `code`。ZIP 和图片等文件响应不使用该 envelope。

失败响应不使用 HTTP 200 伪装成功：请求体解析、参数绑定和参数校验失败返回 400，未认证/无权限返回 401/403，资源不存在返回 404，唯一性或引用冲突返回 409，限流返回 429，服务端或依赖故障返回 5xx。Spring MVC 产生的协议错误保留其原始 HTTP status，例如方法不支持返回 405、媒体类型不支持返回 415，并使用稳定的 `REQUEST_ERROR`。失败 envelope 的 `code` 与 HTTP status 一致，并提供稳定的 `errorCode`；客户端不要依赖可读 `msg` 做分支判断。

下表是路由索引，不复制全部 DTO 字段。精确请求/响应以 `top.naccl.controller`、`model.dto`、`model.vo` 和聚焦测试为准。

## 公开路径

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/login` | username/password 登录并返回 JWT 与用户摘要 |
| `GET` | `/health` | 健康检查 |
| `GET` | `/site` | Blog 首页最小初始化数据与 `featuredGroups` |
| `GET` | `/homepage-featured-images` | 有序首页精选图片 |
| `GET` | `/blogs`, `/blog`, `/searchBlog` | 文章列表、详情和标题搜索 |
| `GET` | `/categories`, `/category`, `/tag` | 分类与标签读取 |
| `GET` | `/comments` | 公开评论树；`pageNum >= 1`，`1 <= pageSize <= 100`，当前根评论页最多加载 500 条公开回复 |
| `GET` | `/profiles/{username}` | 公开作者资料与本人已公开、当前访问者可见的荣誉 |
| `GET` | `/competitions`, `/competitions/{id}` | 比赛分页与详情；游客响应不包含仅登录后可见的奖项 |

## Player 路径

`ROLE_player` 与 `ROLE_admin` 都可访问；所有权仍按当前 JWT username 校验。

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/player/me` | 当前用户资料 |
| `PATCH` | `/player/me/profile`, `/player/me/password` | 修改本人资料或密码 |
| `PUT` | `/player/me/profile-links` | 整体替换本人友情链接 |
| `GET` | `/player/me/oj-handles` | 本人 OJ handle map |
| `POST` | `/player/me/avatar` | 上传本人头像 |
| `POST`, `DELETE` | `/player/images`, `/player/images/{id}` | 创建或删除本人临时文章图片 |
| `GET` | `/player/blogs`, `/player/blogs/recycle-bin` | 本人文章与回收站分页 |
| `GET` | `/player/blog`, `/player/internal-blog` | 本人文章或内部文章读取 |
| `GET` | `/player/categoryAndTag` | 写作所需分类与标签 |
| `POST`, `PUT`, `DELETE` | `/player/blog` | 新建、修改或移入回收站 |
| `PUT` | `/player/blog/restore` | 恢复本人回收站文章 |
| `GET` | `/player/blog/download` | 下载已发布文章 ZIP |
| `GET`, `POST` | `/player/comments`, `/player/comment` | 内部评论读取与登录评论提交；读取分页同样限制 `pageSize <= 100` 和每页 500 条回复 |
| `POST`, `DELETE` | `/player/competitions/{competitionId}/articles/{blogId}` | 参赛用户绑定或解绑本人文章 |
| `PUT` | `/player/competitions/{competitionId}/awards/{awardId}/profile-visibility` | 修改本人奖项公开状态 |
| `PUT` | `/player/competitions/achievement-order` | 重排本人公开奖项 |
| `GET` | `/player/training-data/users` | 可查询训练成员目录 |
| `GET` | `/player/training-data/accepted-summary`, `/player/training-data/accepted-summaries` | 单人或批量 AC 汇总 |
| `GET` | `/player/training-data/submissions/by-user`, `/player/training-data/submissions/by-problem` | 提交查询 |
| `GET` | `/player/training-data/first-accepted/by-user`, `/player/training-data/first-accepted/by-problem` | 首 AC 查询 |

## Admin 路径

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/admin/users:batch-create` | 批量创建用户 |
| `GET`, `PUT`, `DELETE` | `/admin/users`, `/admin/users/{username}` | 列表、原子更新或删除用户 |
| `GET` | `/admin/blogs`, `/admin/blogs/recycle-bin`, `/admin/blogs/backup` | 管理文章、回收站和全量备份 |
| `DELETE`, `PUT` | `/admin/blog`, `/admin/blog/restore` | 移入回收站或恢复文章 |
| `GET`, `POST`, `PUT`, `DELETE` | `/admin/categories`, `/admin/category` | 分类管理 |
| `GET`, `POST`, `DELETE` | `/admin/tags`, `/admin/tag` | 标签管理 |
| `GET` | `/admin/competitions` | 返回包含全部奖项的当前比赛分页，供管理端使用 |
| `PUT` | `/admin/competitions/{competitionId}/awards/{awardId}/login-requirement` | 设置单项奖项是否仅登录后可见 |
| `POST`, `GET`, `DELETE`, `PUT` | `/admin/competitions/**` | 创建比赛、管理参赛/奖项、回收站与恢复 |
| `GET`, `POST`, `PUT`, `DELETE` | `/admin/homepage-featured-groups/**` | 精选组、候选、整组更新与排序 |
| `GET`, `POST`, `PUT`, `DELETE` | `/admin/homepage-featured-images/**` | 精选图片上传、排序和删除 |
| `POST`, `GET` | `/admin/training-data/submission-collection-jobs/**` | 创建、列表和查看采集任务 |

## 关键合同

- 所有同时接受 `pageNum` 与 `pageSize` 的列表接口统一要求 `pageNum >= 1`、`1 <= pageSize <= 100`；越界请求在访问业务 service 前返回 400。
- JWT `sub` 是 `username`。登录失败按规范化 username 使用 Redis 五秒窗口：首次错误为 401，窗口内重复为 429；`Retry-After` 给出剩余秒数，窗口状态不可用时返回 503。
- 分类名、标签名、文章分类引用和文章标签关联由数据库约束裁决；创建分类、创建标签或修改分类名称发生冲突，删除仍被文章引用的分类/标签，或文章写入期间分类/标签已变化时，返回 HTTP 409 / `RESOURCE_CONFLICT`。修改或删除不存在的分类、删除不存在的标签返回 404；文章请求中的重复标签按首次出现保序去重，不通过“先查询再写入”或“先计数再删除”推断结果。
- 评论响应中的 `repliesTruncated=true` 表示本次响应内当前根评论页的全部根评论合计已达到 500 条回复保护上限，并非每个根评论各 500 条；字段为 `false` 时当前页回复完整。
- `PUT /admin/users/{username}` 是账号、角色、密码、完整 handle 集合和采集状态的唯一原子编辑入口。handle 变化前先清理对应 OJ 数据；固定 `root` 受保护。
- 文章和比赛删除只进入七天回收站；到期前可恢复，没有提前物理删除接口。
- 首页精选最多三组，每组固定三篇当前公开、已发布且未回收的文章，全首页不可重复；精选图片最多十二张并保持显式顺序。
- 公开比赛分类和奖档枚举以 `CompetitionCategory`、`CompetitionAwardTier` 为准。奖项 `requiresLogin` 默认为 `false`；设为 `true` 后，游客的比赛列表、详情和公开个人名片均完整移除该奖项，任意有效登录账号可见。管理员除该专用开关和回收站恢复外不提供普通比赛、参赛或奖项编辑接口。
- 创建比赛使用可空的 ISO 日期字段 `competitionDate`（`YYYY-MM-DD`），不再要求单独提交年份。比赛响应保留可空的 `year` 作为年份筛选键：新记录填写日期时由服务端从日期派生，未填写日期时为 `null`；历史记录原有年份保持不变。`startYear`、`endYear` 仍只按该年份键筛选，因此无日期且无历史年份的记录不会命中限定年份的查询。
- 当前赛事列表默认按 `competitionDate` 降序；只有历史记录缺少具体日期时，才以该记录的 `year` 年初作为排序回退，日期和年份都缺失的记录排在最后。
- 公开文章目录、分类和标签列表每页固定返回 6 篇文章；首页文章分页缓存键随该分页合同版本化，部署后不会继续读取旧的 5 篇分页缓存。
- 托管图片只允许所有者绑定；文章下载只接受已发布文章，普通用户共享 30 秒 Redis 窗口，管理员豁免。管理员全量备份必须去除密码、token 和评论 IP 等敏感信息。
- 自动采集默认关闭；管理员手动采集任务仍可用，任务状态只存在当前 JVM。手动任务按 OJ 互斥：同一 OJ 已有运行中任务时返回该任务，Codeforces 与 AtCoder 任务可同时运行。

权限与前端会话规则见 [authorization.md](authorization.md)。
