# frontend Agent Notes

`frontend` 是统一门户中的 Vue 3 Training 构建，不是独立后端或第二个对外站点。生产环境只有一个 Nginx `frontend` 服务：Blog 构建服务 `/` 并持有唯一顶栏和 `/training/**` 外层路由，Training 构建内部挂载于 `/training-app/**`。

## 当前范围

- 正式训练路由只有 `/training/login`、`/training/multiple`、`/training/single`、`/training/problem`、`/training/admin/create-users`、`/training/admin/users`、`/training/admin/articles`、`/training/admin/categories`、`/training/admin/competitions`、`/training/admin/training` 和 `/training/admin/appearance`。
- Blog 回跳白名单只允许首页、文章、分类、标签、公开赛事、个人主页和写作页；不得恢复 About、Friends、Moments 路径或保留页。
- 多人、单人和题目查询要求登录；业务身份是 `username`，角色只有 `ROLE_admin` 与 `ROLE_player`。
- 多人查询先读用户目录，再用一次 `GET /player/training-data/accepted-summaries` 批量取得当前 OJ 的全部汇总。不得按用户调用单人接口；缺失项和整体失败显示可重试错误，不能当作 0 题。
- 多人和单人筛选变化后自动刷新；题目查询保留显式查询按钮并使用整行填充布局。各查询页的 OJ 选择统一使用站内风格点击下拉菜单。单人页不自动选择第一名队员，固定加载包含退役队员的全量目录，通过 username 或姓名子串搜索下拉候选，放大镜或回车查询当前高亮候选。
- 管理区分为创建用户、管理用户、管理文章、分类与标签、比赛与奖项、数据采集、首页图片七页。比赛与奖项页以十个规范赛事分类单选创建比赛，创建表单不展示冗余的参赛形态控件而由分类直接生成请求值（省赛沿用团队默认）；按分类动态提供固定奖档、参赛用户、奖项和七天回收站，不暴露底层历史类型标签的任意组合，也不提供普通编辑入口；首页图片页只管理最多十二张 3:2 滚动精选图片，旧动态横幅管理已删除。各页只加载当前功能需要的数据。
- 管理文章页必须在现有 `/training/admin/articles` 内提供“首页编排”、当前文章和回收站三个子视图，不新增顶层管理路由。“首页编排”最多创建三组，每组标题可编辑且必须选择三篇互不重复的文章；同一篇文章在全首页只能出现一次，候选仅限当前已发布、公开且未回收的文章，并允许调整组顺序。成员后来失去公开资格时后台保留该组并明确标记待替换，公开首页暂不展示整组。
- 当前文章/回收站子视图的删除只移入固定七天回收站，展示删除时间与剩余保留期，并允许管理员恢复。页面还要提供管理员全量文章 ZIP 备份，覆盖草稿、内部文章和回收站，并包含去敏评论、作者资料和托管图片。不得提供或暗示提前永久删除。
- 管理用户一次保存只调用 `PUT /admin/users/{username}`，提交账号字段、改名、角色、密码、完整 handle 集合和 `needCollect`。handle 移除或变化必须先高危确认，由后端同一事务负责数据清理和精确替换；不得恢复拆分 patch/replace API。
- 分类和标签使用各自的分页状态及分页条；刷新一个列表不得重置另一个列表。
- 管理用户页空头像回退 `/img/default-avatar.jpg`。固定 `root` 只显示管理员身份，不提供改名、降权、handle、采集状态或删除入口。
- 自动采集默认关闭；管理员手动采集仍需确认目标、OJ、倒退范围和数仓刷新影响。
- Blog 与 Training 固定使用共享浅色语义 token，不提供主题切换、系统主题跟随、主题持久化或 frame 主题消息。Blog 文章目录等固定深色页面属于页面自身设计，不得误当成全局夜间模式删除。
- 管理区确认操作统一使用 `AdminConfirmDialog.vue`，不得调用浏览器原生 `confirm/alert/prompt`。创建用户、全量文章下载、分类/标签删除、比赛/参赛用户/奖项删除和首页图片删除必须先展示明确影响范围再执行。
- 验收范围是 1280–2560 px 桌面端，重点检查 1440×900 和 1920×1080。

## 认证与请求规则

- Training 与 Blog 共享 `custacm.accessToken`、`custacm.user`。用户摘要只用于展示，Blog API 依据 JWT 和数据库当前角色授权。
- 启动时调用 `/player/me` 恢复会话。只有 401 清理会话；403 与网络错误保留会话并展示错误。
- 登录失败后的五秒冷却由 Blog API 按 username 强制；首次错误 401 和窗口内 429 都通过 `Retry-After` 告知剩余秒数。`ApiError` 必须保留该字段，`LoginPanel` 在倒计时结束前禁用提交；不得以纯前端计时替代服务端窗口。
- `src/api/client.ts` 不全局附加 JWT；受保护 adapter 显式传入 `Authorization: Bearer <token>`。
- 浏览器请求统一使用 `/api/**`；`/api/image/**` 由 Nginx 从只读上传目录直接返回。
- `safeReturnPath` 必须使用显式白名单，Blog 回跳导航顶层窗口，禁止在训练 frame 中再加载 Blog 外壳。
- 不在源码、文档、日志或测试夹具中写真实密码、JWT 或签名密钥。

## 结构与依赖规则

- `src/App.vue` 负责会话和顶层组合，`src/router/index.ts` 负责内部训练路由。
- `src/composables/useAuthSession.ts` 管认证生命周期，`usePlatformDashboard.ts` 管页面数据；dashboard 必须根据 route/admin section 按需加载。
- `src/api/` 按 auth、training、admin 拆分，组件不得自行拼接散落的业务请求。
- 首页编排只调用 `/admin/homepage-featured-groups` 下的列表、候选、创建、整组更新、删除和排序接口。旧 `/admin/blog/recommend` 不再调用；`is_recommend` 只是历史存储/备份字段，对首页展示没有作用。
- `src/components/` 负责页面与复用组件；`TrainingAdminPanel` 是七个管理员页面的导航入口。
- `src/utils/` 只放仍被生产代码复用的纯函数；删除生产引用后应连同只覆盖死代码的测试一起裁剪。
- `src/test/` 覆盖路由、会话、API、批量查询、分页和关键交互。新增或修改业务行为必须同步补测试。
- 不跨应用复制业务组件，不合并两套 Router；Blog Router 持有公开 `/training/**`，Training Router 只在 `/training-app/**` 运行。
- 独立开发的 `AppShell` 只读 `/categories` 构建分类菜单；嵌入 Blog 时隐藏调试顶栏并跳过该请求，不能为导航加载完整 `/site`。
- `index.html` 不得恢复主题探测或首屏主题脚本；Training frame 只同步路由，不同步视觉模式。
- `src/styles/tokens.css` 是由根 `frontend-design-tokens/tokens.css` 通过 `scripts/sync-design-tokens.sh` 生成的副本，禁止手工编辑；它必须在样式入口最先加载，`training-redesign.css` 承载 Training 试点覆盖。

## 文件职责

| 文件 | 职责 |
| --- | --- |
| `Dockerfile` | 构建 Blog 与 Training 两份 Vue 静态产物并复制进同一个 Nginx 镜像 |
| `nginx.conf`、`nginx-https.conf` | Blog/Training fallback、`/api/**` 代理、托管图片与 HTTP/TLS 入口 |
| `vite.config.ts` | `/training-app/` base、5173 开发端口与 `/api` 代理 |
| `src/main.ts`、`src/App.vue` | Vue/Router 入口、认证和页面组合 |
| `src/router/index.ts` | 内部 `/training-app/**` 路由 |
| `src/views/TrainingView.vue` | 路由级页面容器和当前 admin section |
| `src/routing.ts` | 页面类型与当前有效登录回跳白名单 |
| `src/types.ts` | Blog API DTO 与页面模型，包括原子用户更新、比赛/奖项和首页精选组模型 |
| `src/auth/session.ts` | 成对校验、读写和清理共享会话 |
| `src/api/client.ts` | Result envelope、含 `Retry-After` 的错误类型、JSON 请求与受保护文件下载 fetch 封装 |
| `src/api/auth.ts` | 登录、当前用户和本人密码修改 |
| `src/api/training.ts` | 用户目录、单人汇总、多人批量汇总、提交和首 AC 查询 |
| `src/api/admin.ts` | 用户单 PUT、比赛/奖项、文章/全量备份、首页精选组、分类/标签分页、采集任务和滚动精选图片 API |
| `src/composables/useAuthSession.ts` | 会话恢复、登录竞态、退出与密码修改 |
| `src/composables/usePlatformDashboard.ts` | 按页面加载、批量汇总、比赛聚合、分页、首页精选组、精选图片和管理员状态编排 |
| `src/components/AppShell.vue` | 独立调试顶栏与分类目录；嵌入时隐藏且不重复请求 |
| `src/components/LoginPanel.vue` | 登录表单、服务端五秒冷却倒计时和安全回跳 |
| `src/components/TrainingQueryPanel.vue` | 站内风格 OJ 下拉、多人/单人自动筛选、单人全量队员子串搜索、批量错误展示和整行题目显式查询 |
| `src/components/TrainingAdminPanel.vue` | 七个管理员页面导航 |
| `src/components/AdminConfirmDialog.vue` | 管理区统一确认框、语义图标、键盘取消和共享浅色外观 |
| `src/components/AdminUserManagementPanel.vue` | 用户单 PUT 编辑与 handle 高危确认 |
| `src/components/CategoryAdminPanel.vue` | 分类/标签各自分页和写操作 |
| `src/components/CompetitionAdminPanel.vue` | 规范赛事分类筛选/创建、动态奖档与排名字段、参赛用户和奖项增删、比赛七天回收站与恢复 |
| `src/components/ArticleAdminPanel.vue` | `/training/admin/articles` 内“首页编排”/当前文章/回收站切换、全量备份、筛选、分页、删除和恢复 |
| `src/components/HomepageFeaturedGroupsPanel.vue` | 最多三组的标题、固定三篇文章、全首页去重、候选搜索、失效提示、组排序和删除交互 |
| `src/components/TrainingDataOpsPanel.vue` | 手动采集任务、详情和数仓刷新 |
| `src/components/HomepageFeaturedImagesAdminPanel.vue` | 最多十二张滚动精选图片的批量选择、3:2 裁剪、缩略图预览、排序和删除 |
| `src/styles/homepage-featured-images-admin.css` | 精选图片管理网格、计数与裁剪布局 |
| `src/styles/competition-admin.css` | 比赛聚合列表、详情、表单和奖项层级布局 |
| `src/styles/tokens.css` | 共享设计 token 的生成副本；后续视觉试点接入，禁止手工编辑 |
| `src/styles/training-redesign.css` | Training 阶段 2 的统一颜色角色、圆角、玻璃、字号和路由动效覆盖 |
| `src/test/training-redesign-style.test.js`、`admin-confirm-dialog.test.ts` | token/玻璃/动效加载边界与确认弹层焦点生命周期测试 |
| `src/test/platform-dashboard-batch.test.ts` | 多人页面单次批量请求测试 |
| `src/test/admin-users-vue.test.ts` | 用户原子保存和危险确认测试 |
| `src/test/category-admin.test.ts` | 分类/标签独立分页测试 |
| `src/test/competition-admin.test.ts` | 比赛创建、参赛用户/奖项操作与回收站确认交互测试 |
| `src/test/competition-admin-api.test.ts` | 比赛公开列表与管理员写入路径、方法、请求体和 Bearer 合同测试 |
| `src/test/homepage-featured-groups-admin.test.ts` | 精选组固定三篇保存、完整组排序和删除确认交互测试 |
| `src/test/app-shell-vue.test.ts` | 独立/嵌入外壳的加载边界测试 |
| `src/test/platform-api.test.ts` | Training/Admin API contract 测试 |

## 验证与文档同步

```bash
pnpm lint
pnpm test
pnpm typecheck
pnpm build
```

涉及统一门户时还要验证 Blog 构建，并在 1440×900 与 1920×1080 检查 `/`、`/training/**`、刷新 fallback、登录/退出和权限隔离。

职责、路由、API、构建或部署边界变化时，同步更新本文件、`README.md`、`../platform-blog/README.md` 及 `docs/doc-sync-map.tsv` 指定文档。
