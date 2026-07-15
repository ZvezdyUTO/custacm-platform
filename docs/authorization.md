# HTTP 授权

## 角色与匹配顺序

| 身份 | 存储角色 | 可访问范围 |
| --- | --- | --- |
| 管理员 | `ROLE_admin` | public、player、admin |
| 队员 | `ROLE_player` | public、player |
| 游客 | 无 | public |

Spring Security 按顺序匹配：

```text
OPTIONS /**        public
POST /login        public
/admin/**          ROLE_admin
/player/**         ROLE_admin or ROLE_player
GET /**            public
其它请求           denied
```

`POST /login` 是唯一匿名业务写入口。浏览器的 `/api` 只是 Nginx 前缀，不改变后端权限。

## JWT

- Blog API 签发 HS512 bearer token，`sub=username`，并可携带 `authorities`。
- `Authorization` 必须使用 `Bearer <JWT>` 格式（scheme 大小写不敏感）；裸 token、拼接出的 `BearerBearer` 或其它 scheme 均不作为凭证接受。
- `BLOG_TOKEN_SECRET` 没有可用默认值；缺失、含 `change-me` 占位标记或少于 64 个 UTF-8 字节时，应用在启动阶段拒绝运行。
- 每次受保护请求先校验签名和过期时间，再按 `sub` 从 MySQL 加载当前用户与角色；数据库状态是最终授权来源。
- 在受保护路径上，缺失、无效或过期 token，以及用户已删除或改名，返回 401；已认证但角色不满足返回 403。公开 GET 未携带 token 或携带无效 token 时按游客继续处理。显式 token 的数据库用户解析发生基础设施故障时返回 500 / `AUTH_CONTEXT_RESOLUTION_FAILED`，不会伪装成 401 或静默降级为游客。
- 比赛列表、比赛详情和公开个人名片会按可选认证过滤奖项：游客不收到 `requiresLogin=true` 的奖项及其获奖人，任意持有效登录凭证的账号均可见；管理端通过受保护的 `/admin/competitions` 读取完整列表。
- 固定 `root` 管理员不可删除、改名、降权、绑定 OJ handle 或进入队员采集状态。

## 资源所有权

- `/player/me/**`、文章、评论图片、比赛文章绑定和奖项公开偏好都只操作当前 JWT username；管理员访问 player 路径也不会获得跨用户所有权。
- 普通用户只能修改本人文章。管理员可通过 `/admin/**` 管理全部文章，但新建文章作者仍是当前认证账号。
- 参赛文章绑定同时校验当前用户是比赛参赛者、文章作者且文章当前公开已发布。
- 托管图片只能由所有者绑定到一篇文章，不能跨用户或跨文章复用资产。
- 管理员用户更新使用单一原子入口；移除或更换 handle 前必须清理对应 OJ 数据。

## 前端会话

- Blog 与 Training 共享 `custacm.accessToken`、`custacm.user`；摘要不能替代服务端授权。
- API client 不全局附加 JWT。需要内部可见性或保护的 adapter 显式发送 Bearer token。
- Blog 的比赛和公开个人名片 adapter 仅为读取登录可见奖项显式附加当前 token；会话变化后重新读取，避免登出后保留旧的受限内容。
- 明确的 401 清理本地会话；403 和网络错误保留会话并展示错误。
- 登录回跳只接受显式同源白名单，不能把 Blog 外壳再次嵌入 Training frame。

具体 HTTP 路径见 [api.md](api.md)，安全 matcher 的代码来源是 [SecurityConfig.java](../platform-blog/upstream/nblog/blog-api/src/main/java/top/naccl/config/SecurityConfig.java)。
