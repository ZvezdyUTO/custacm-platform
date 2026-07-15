# frontend Agent Notes

## 运行边界

- `frontend` 只负责 Vue 3 Training 运行时；公开 `/training/**` 由 Vue Blog 外壳持有，Training 内部静态路由固定为 `/training-app/**`。
- 生产环境只有一个 Nginx `frontend` 服务和一条 Blog 顶栏。不得合并两套 Router、复制 Blog 业务组件或渲染第二条顶栏。
- 浏览器 API 统一使用 `/api/**`；`/api/image/**` 由 Nginx 读取只读上传目录，其他请求去掉 `/api` 后代理 Blog API。
- Blog API 是唯一后端和授权事实来源。前端不得签发 JWT、保存密码或复制服务端权限与数据清理逻辑。

## 认证与请求

- Blog 与 Training 只共享 `custacm.accessToken` 和 `custacm.user`；用户摘要仅用于展示。
- 启动时使用 `/player/me` 恢复会话。只有 401 清理本地会话；403 与网络错误保留会话并展示错误。
- `src/api/client.ts` 不得全局附加 JWT；受保护 adapter 显式发送 `Authorization: Bearer <token>`。
- `/admin/**` 仅 `ROLE_admin`，`/player/**` 接受 `ROLE_admin` 或 `ROLE_player`。前端展示限制不能替代后端授权。
- `safeReturnPath` 必须使用显式白名单，返回 Blog 时导航顶层窗口，禁止在 Training frame 内嵌套 Blog 外壳。
- 不得在源码、文档、日志或测试夹具中写入真实密码、JWT、签名密钥、Cookie 或 Authorization header。

## 实现约束

- 组件通过 `src/api/` adapter 访问业务 API，不自行拼接散落请求；认证生命周期归 `useAuthSession.ts`，页面数据归 `usePlatformDashboard.ts`。
- 多人统计必须使用一次 `/player/training-data/accepted-summaries` 批量请求，不得按用户制造 N+1；缺失项或整体失败不能伪装为零题。
- 管理员保存用户只调用一次 `PUT /admin/users/{username}`；handle 更换或移除必须先明确确认，固定 `root` 不提供受保护字段的修改或删除入口。
- 管理区确认操作统一使用 `AdminConfirmDialog.vue`，不得调用浏览器原生 `confirm`、`alert` 或 `prompt`。
- `src/styles/tokens.css` 由根 `frontend-design-tokens/tokens.css` 通过 `scripts/sync-design-tokens.sh` 生成，禁止手工编辑，并须在样式入口最先加载。
- 两份前端使用共享日间/夜间语义；主题选择只来自站内手动开关，保存到 `custacm.theme` 并同步同源 Training frame，不跟随系统主题。

## 验证

前端运行时代码变更至少运行：

```bash
pnpm lint
pnpm test
pnpm typecheck
pnpm build
```

Dockerfile、Nginx、Vite base、路由或网关行为变化时，还要验证 Blog 构建、Compose 配置以及 `/`、`/training/**`、`/api/health` 的刷新和权限边界。只有职责、路由、认证或运行方式变化时才同步更新 `README.md`。
