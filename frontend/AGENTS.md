# frontend Agent Notes

`frontend` 是 Vue 3 训练中心，不是独立后端或通用站点外壳。生产环境中，Vue Blog 持有 `/training/**` 外层路由和唯一顶栏，并在其下方同源嵌入训练运行时；训练静态产物只挂载在内部路径 `/training-app/**`。浏览器业务请求统一走 `/api/**` 并代理 Blog API；`/api/image/**` 从只读上传目录由 Nginx 直接返回并设置 immutable 缓存。

## 当前范围

- 训练中心路由只有 `/training/login`、`/training/multiple`、`/training/single`、`/training/problem`、`/training/admin/create-users`、`/training/admin/users`、`/training/admin/articles`、`/training/admin/categories`、`/training/admin/training` 和 `/training/admin/appearance`；`/training` 在生产环境重定向到 `/training/multiple`，旧 `/training/admin?section=...` 只作兼容解析。
- 多人、单人和题目查询都要求登录。业务身份字段是 `username`，角色只能是 `ROLE_admin` 或 `ROLE_player`。
- 多人和单人查询没有独立查询按钮；OJ、队员、日期和 rating 等筛选参数变更后自动刷新，文本/数字输入使用短防抖，无效范围不得发起请求。题目查询保留深色显式查询按钮，题号和日期修改后由用户统一提交。
- 单人查询加载用户目录后不得自动选择第一名队员，必须保持空选择直到用户主动选择。
- 管理区分为“创建用户”“管理用户”“管理文章”“分类与标签”“数据采集”“首页图片”六个独立页面。分类可自定义名称和颜色；标签只新增或删除，颜色由服务端随机生成深色并持久化。管理文章与管理用户的永久删除均必须经过严肃二次确认；数据采集确认后才能执行。
- 管理员工作区使用勃艮第主题：酒红用于导航与主操作，陶土色用于当前页标记，暖雾灰用于工作区背景；成功、警告、危险等业务状态不得改成主题色。
- 前端不提供原始数据上传入口，也不重新实现后端账号、权限、采集或数仓规则。
- 管理用户页对空头像统一展示 Blog 构建内置的 `/img/default-avatar.jpg`；`root` 只显示管理员身份，不显示现役/退役状态或 handle 编辑入口。
- 多人查询先读取 `GET /player/training-data/users` 返回的可采集用户目录，再以最大并发数 6 查询各用户汇总。目录响应只消费 `username`、`nickname`、`ojNames`。
- 正式验收范围是 1280–2560 px 桌面端，重点检查 1440×900 和 1920×1080；移动端不在当前交付范围。
- 进入 `/training/**` 后必须继续使用 Blog 已挂载的同一个 `Nav.vue` 实例，只替换其下方内容并把“训练中心”显示为选中态；不得在训练应用中复制或显示第二条顶栏。

## 认证与请求规则

- 训练中心与 Vue Blog 共享 `localStorage` 键 `custacm.accessToken` 和 `custacm.user`。用户摘要只用于展示；Blog API 始终根据 JWT 与数据库中的当前用户/角色做授权。
- 训练中心启动时通过 `/player/me` 恢复会话。只有 401 可以清除失效会话；403 与网络错误不得伪装成退出登录。
- `src/api/client.ts` 只提供通用请求封装，不得全局附加 JWT。受保护的请求由对应 API adapter 显式传入 `Authorization: Bearer <token>`。
- 公开 Blog 请求不得携带训练 JWT。Vue 评论提交是例外：它读取共享会话，并只为 `POST /player/comment` 显式附加 Bearer JWT；密码文章 token 保持原格式，不能和登录 JWT 混用。
- 登录回跳必须经过 `safeReturnPath` 白名单，禁止开放重定向。
- `safeReturnPath` 可接受固定训练路由和 Blog 的首页、分类、标签、动态、友链、个人页、文章及写作路由；登录成功后回到登录入口携带的原位置。Blog 回跳必须使用同源 URL并导航顶层窗口，禁止在训练 frame 内加载第二套 Blog 外壳。
- 不在源码、文档、日志或测试夹具中写真实密码、JWT 或签名密钥。

## 结构与依赖规则

- `src/App.vue` 负责会话和顶层组合，`src/router/index.ts` 负责训练路由；认证生命周期归 `src/composables/useAuthSession.ts`，业务状态编排归 `src/composables/usePlatformDashboard.ts`。
- `src/api/` 按 auth、training、admin 拆分 Blog API adapter；组件不得自行拼接散落的 HTTP 请求。
- `src/components/` 负责可访问的页面与复用组件；`TrainingAdminPanel` 是六个管理员页面的统一导航入口。
- `src/utils/` 只放纯函数或可独立测试的并发工具；多人请求必须复用 `runLimited` 的并发限制。
- `src/styles.css` 只作为样式入口；当前生效规则按 foundation、theme、shell、dashboard、table、side-panel 拆分。
- `src/test/` 覆盖路由、会话、API adapter、并发器和关键页面交互。新增业务行为必须在同一变更中补测试。
- 不跨应用复制业务组件，也不把两套 Vue Router 合并；Blog Router 持有公开 `/training/**`，训练 Router 只在同源 frame 的内部 `/training-app/**` 运行并同步公开 URL。

## 关键文件职责

| 文件 | 职责 |
| --- | --- |
| `Dockerfile` | 分别构建 Vue 3 训练中心和 Vue 3 Blog，并将两份静态产物复制进 Nginx 镜像 |
| `nginx.conf` | 让 `/training/**` 使用 Blog history fallback、`/training-app/**` 使用训练产物，并代理 `/api/**` |
| `vite.config.ts` | 设置内部 `/training-app/` base、本地 5173 端口、独立 HMR 通道和 `/api` 到 8090 的开发代理 |
| `src/main.ts` | Vue 应用与 Vue Router 入口 |
| `src/App.vue` | 组合认证、训练查询和管理员页面 |
| `src/router/index.ts` | 定义内部 `/training-app/**` 路由；公开路径由 Blog Router 持有 |
| `src/views/TrainingView.vue` | 查询和管理员页面的路由级容器 |
| `src/routing.ts` | 定义训练页面类型并校验登录回跳白名单 |
| `src/types.ts` | 训练前端使用的 Blog API DTO 和页面模型 |
| `src/auth/session.ts` | 成对校验、读写、清理共享 JWT 与用户摘要 |
| `src/api/client.ts` | Blog `Result` envelope、错误类型和基础 fetch 封装 |
| `src/api/auth.ts` | 登录、当前用户和修改本人密码接口 |
| `src/api/training.ts` | 用户目录、多人/单人/题目训练查询接口 |
| `src/api/admin.ts` | 用户、文章精选、文章分类、OJ handle、采集任务、数仓刷新和首页图片管理接口 |
| `src/composables/useAuthSession.ts` | 会话恢复、竞态隔离、登录、退出和修改密码 |
| `src/composables/usePlatformDashboard.ts` | 查询筛选、分页、用户管理、采集任务与提示状态编排 |
| `src/utils/runLimited.ts` | 有序执行并限制最大并发数的纯工具 |
| `src/utils/adminUsers.ts` | 创建用户文本导入与可编辑行转换 |
| `src/utils/adminTraining.ts` | 固定携带 `refreshWarehouse: true` 的采集请求构造 |
| `src/components/AppShell.vue` | 训练内容外壳；独立开发时提供调试顶栏，生产同源嵌入时必须隐藏该顶栏 |
| `src/components/LoginPanel.vue` | 登录表单与安全回跳 |
| `src/components/TrainingQueryPanel.vue` | 多人、单人自动筛选查询页面，以及保留深色查询按钮的题目查询页面 |
| `src/components/TrainingAdminPanel.vue` | “创建用户/管理用户/管理文章/管理分类/数据采集/首页图片”独立页面导航容器 |
| `src/components/ArticleAdminPanel.vue` | 管理员文章筛选、分页、精选状态开关与带危险确认的永久删除 |
| `src/components/CategoryAdminPanel.vue` | 文章分类分页、新增、改名和删除界面 |
| `src/components/HomepageBannerAdminPanel.vue` | 首页图片多选队列、1920×1080 裁剪、排序与删除界面 |
| `src/styles/homepage-banner-admin.css` | 首页图片管理卡片和裁剪弹窗样式 |
| `src/components/CreateUsersPanel.vue` | 文本导入、创建信息行和批量创建操作 |
| `src/components/AdminUserManagementPanel.vue` | 只读展示头像，并以宽幅个人信息列集中账号、邮箱与 OJ handle；编辑区管理角色、密码和采集状态，但不接受头像 URL，头像只走本人图片上传接口 |
| `src/components/TrainingDataOpsPanel.vue` | 全部/单人采集、任务详情和固定数仓刷新 |
| `src/styles/theme.css` | 与 Blog 一致的颜色、字体和视觉变量 |
| `src/styles/*.css` | 基础、外壳、内容、表格、侧栏及桌面宽度样式 |
| `src/test/session.test.ts` | 共享会话读写、非法数据与孤儿键清理测试 |
| `src/test/routing.test.ts` | 安全登录回跳测试 |
| `src/test/login-panel-vue.test.ts` | 登录表单与安全回跳测试 |
| `src/test/run-limited.test.ts` | 并发上限、结果顺序与失败传播测试 |
| `src/test/*.test.ts` | 路由、会话、API、并发和组件行为回归测试 |

## 本地运行与验证

Vue 3 训练中心使用 Node.js 20.19+ 和锁定的 pnpm：

```bash
corepack enable
pnpm install --frozen-lockfile
pnpm dev -- --host 0.0.0.0
```

训练应用的独立开发入口是 `http://localhost:5173/training-app/multiple`；与 Blog Vite 同时启动时，统一热更新入口是 `http://localhost:4180/training/multiple`。部署验收仍使用 Nginx 的 `/training/**`，并要求 Blog API 可通过 `http://localhost:8090` 访问。提交前运行：

```bash
pnpm lint
pnpm test
pnpm typecheck
pnpm build
```

Vue Blog 的可复现生产构建命令是：

```bash
cd ../platform-blog/upstream/nblog/blog-view
npm ci
npm test
npm run build
```

涉及渲染的变更还要在 1440×900 和 1920×1080 的真实浏览器中检查 `/`、`/training/**`、刷新 history fallback、登录/退出、权限隔离和控制台错误。

## 文档同步

职责、路由、API、构建或部署边界变化时，同步更新 `README.md`、`../platform-blog/README.md`、`../docs/api.md`、`../docs/architecture.md`、`../docs/authorization.md`、`../docs/agent/context-map.md` 和部署文档。
