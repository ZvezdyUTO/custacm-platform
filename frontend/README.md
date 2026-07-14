# frontend

`frontend` 是 Vue 3 + Vite + TypeScript 训练中心，也是统一 Nginx 前端镜像的构建目录。

生产环境只有一个 `frontend` Nginx 服务和一个门户；“两份前端”指镜像内的两份 Vue 源码构建，不是两套独立站点或服务：

| 源码构建 | 浏览器路径 | 职责 |
| --- | --- | --- |
| `../platform-blog/upstream/nblog/blog-view` | `/` | 公开 Blog、唯一顶栏和 `/training/**` 外层路由 |
| 本目录 | `/training/**` | 训练查询与管理员工作区；内部静态 base 为 `/training-app/` |
| Blog API | `/api/**` | Nginx 去掉 `/api` 后代理到唯一后端 |

两份 Vue Router 彼此独立。训练运行时通过同源 frame 嵌入 Blog 外壳；进入训练中心时继续使用已挂载的同一个 `Nav.vue`，不会渲染第二条顶栏。

两份构建固定使用共享浅色语义 token，不读取系统配色、不持久化视觉模式，也不在 frame 间传递主题消息。Blog 顶栏和 Training 独立开发外壳都不提供主题按钮；文章目录等固定深色页面仍由 Blog 自身页面样式维护，不属于全局夜间模式。个人训练难度分布继续使用 rating 色阶和清晰轨道保持边界可辨。

## 页面与权限

```text
/training/login
/training/multiple
/training/single
/training/problem
/training/admin/create-users
/training/admin/users
/training/admin/articles
/training/admin/categories
/training/admin/competitions
/training/admin/training
/training/admin/appearance
```

- `multiple`：先读取用户目录，再用一次 `GET /player/training-data/accepted-summaries` 批量取得当前 OJ 的全部汇总。缺失项或整体失败显示可重试错误，不能伪装成 0 题；不再按用户并发调用单人接口。
- `single`：按 username、OJ、日期和难度查询个人汇总、提交与首 AC；固定加载包含退役队员的全量目录，输入 username 或姓名子串后通过下拉候选选择，放大镜或回车查询当前高亮候选，不自动选择第一项。
- `problem`：按题目、OJ 和日期查询提交与首 AC，保留显式查询按钮并让五个筛选控件填满整行。
- `admin/create-users`：文本导入或手动编辑后批量创建账号与 OJ 绑定。
- `admin/users`：编辑账号、改名、密码、角色、完整 OJ handle 集合和采集状态；一次保存只调用 `PUT /admin/users/{username}`。更换或移除 handle 前要求高危确认，服务端在同一事务内清理数据并精确替换绑定。
- `admin/articles`：同一路由内提供“首页编排”、当前文章和回收站三个子视图。“首页编排”最多创建三组，每组可修改标题、固定选择三篇文章并调整组顺序；候选只包含当前已发布、公开且未回收的文章，同一篇文章不能在首页任何位置重复。当前文章支持筛选、分页和移入固定七天回收站，管理员可恢复任意作者文章，还可下载包含全部文章状态、评论、作者资料与托管图片的全量 ZIP。
- `admin/categories`：分类和标签各自分页；分类支持名称/颜色增删改，标签支持新增/删除。
- `admin/competitions`：按年份与十个规范赛事分类筛选比赛，以单选分类创建比赛并动态维护固定奖档、参赛用户和个人/团队奖项；奖牌类要求排名，普通奖项不记录排名。比赛根删除进入固定七天回收站，子关系删除立即生效，数据更正通过删除后重加完成。
- `admin/training`：创建和查看显式采集任务。自动采集默认关闭，不影响管理员手动触发。
- `admin/appearance`：裁剪、上传、排序和删除最多十二张 1200×800 滚动精选图片；旧动态横幅管理已移除。

训练查询要求 `ROLE_player` 或 `ROLE_admin`；管理员页面只允许 `ROLE_admin`。业务身份统一使用 `username`，固定 `root` 不提供改名、降权、handle、采集状态或删除操作。

当前产品验收范围为 1280–2560 px 桌面端，重点检查 1440×900 与 1920×1080；移动训练业务页不在当前范围。

## 数据加载规则

- 页面只加载当前功能所需数据。多人页按“显示退役队员”开关加载用户目录，单人页固定加载包含退役队员的全量目录；题目页不加载用户目录；管理员页只加载对应 section 的用户、任务、文章、分类/标签、比赛聚合或滚动精选图片。
- 多人批量接口按 OJ、日期、难度和 `includeRetired` 过滤，响应按 username 与当前目录合并。
- 管理用户列表由后端一次批量装配账号和 handle，不产生逐用户查询。
- 分类和标签使用互不影响的分页状态及分页条；新增/删除后只刷新对应列表。
- 进入 `/training/admin/articles` 的“首页编排”子视图时只加载精选组；打开文章选择器后再按标题加载候选。若组内文章后来变为草稿、内部文章或进入回收站，后台仍显示原关系和失效状态供管理员修复，公开首页在修复或文章恢复资格前隐藏整组。
- 独立开发外壳只调用公开 `/categories` 构建分类菜单，不加载完整 `/site`。嵌入 Blog 时不重复请求导航数据。

## 认证与 API

训练中心与 Vue Blog 共享：

```text
custacm.accessToken
custacm.user
```

`custacm.user` 只是展示摘要。启动时使用 `/player/me` 校验 JWT；受保护请求由对应 API adapter 显式发送 `Authorization: Bearer <token>`。只有明确的 401 清理本地会话，403 和网络错误保留会话并显示错误。登录凭据首次错误时 Blog API 返回 401 与 `Retry-After: 5`，同 username 五秒内重复请求返回 429；Training 登录按钮读取该响应头展示倒计时并禁用重复提交，正确登录不会保留冷却。

所有浏览器 API 使用 `/api/**`。例如：

```text
浏览器：GET /api/player/training-data/accepted-summaries
Blog API：GET /player/training-data/accepted-summaries
```

公开 Blog 请求不全局携带 JWT。安全回跳白名单只包含当前 Blog 首页、文章、分类、标签、公开赛事、个人主页、写作页和当前训练路由；已删除的 About/Friends/Moments 路由不再保留。Blog 回跳导航顶层窗口，避免把 Blog 外壳加载进训练 frame。

Blog 文章详情的标题/简介/正文与扁平图片 ZIP 下载同样通过 `/api/player/**` 进入唯一后端并显式携带共享 Bearer；普通用户跨全部文章共享的 30 秒限流和管理员豁免完全由 Blog API 判定，本训练构建不复制该逻辑。管理员全量备份通过 `/api/admin/blogs/backup` 下载，并使用相同的可读 Markdown 与图片命名规则。

首页编排使用 `/api/admin/homepage-featured-groups` 下的管理员接口：列表、创建、按 ID 整组更新/删除、`/order` 排序和 `/candidates` 候选搜索。创建/更新请求提交非空 `title` 和按展示顺序排列的三个不同 `articleIds`；排序请求提交当前全部组 ID。旧 `/api/admin/blog/recommend` 已移除，`is_recommend` 仅作为历史存储与备份兼容字段，不参与首页展示。

比赛管理读取匿名 `/api/competitions` 完整聚合树，并通过 `/api/admin/competitions/**` 显式携带管理员 Bearer 完成创建、参赛用户/奖项增删、移入回收站和恢复。当前比赛、回收站、规范 `category` 筛选和分页请求均只允许最后一次交互回写列表与加载状态；前端维护与后端一致的十个规范分类和分类限定奖档，创建表单不展示参赛形态控件，提交时由分类映射自动生成（省赛沿用团队默认），不允许操作底层兼容类型标签，比赛、参赛用户和奖项没有普通编辑接口。

生产 Nginx 直接从只读挂载提供 `/api/image/**` 托管文件，其他 `/api/**` 请求去前缀后代理 Blog API。

## 目录结构

```text
frontend/
  Dockerfile
  nginx.conf
  nginx-https.conf
  package.json
  pnpm-lock.yaml
  vite.config.ts
  src/
    api/
    auth/
    components/
    composables/
    router/
    styles/
    test/
    utils/
    views/
    App.vue
    main.ts
    routing.ts
    types.ts
  public/img/custacm-training-logo.jpg
```

## 依赖与边界

- Blog API 是唯一后端；前端不签发 JWT、不保存密码、不复制服务端授权和数据清理逻辑。
- `src/api/` 按 auth、training、admin 拆分正式 HTTP contract，组件不自行拼接业务请求。
- `src/composables/useAuthSession.ts` 管理认证生命周期，`usePlatformDashboard.ts` 管理页面数据与操作状态。
- Vue Blog 负责公开内容与唯一顶栏，本应用只负责训练中心；不跨应用复制业务组件或合并 Router。
- 共享视觉 token 的唯一源位于 `../frontend-design-tokens/tokens.css`；`src/styles/tokens.css` 由仓库脚本生成并已接入 Training，阶段 2 覆盖集中在 `src/styles/training-redesign.css`，Blog 使用同一源文件的生成副本。阶段 5 已确认本应用继续使用原生 Vue 控件，不引入 Element Plus 或 ant-design-vue；Blog 则保留 Element Plus、使用 Lucide 并已退出 Semantic UI。只有复杂控件范围显著增长时才按设计规范的触发条件重新评估。
- 原始 ODS 写入仍是后端能力，当前 UI 不提供上传入口。

## 文件与路径职责

| 文件/路径 | 职责 |
| --- | --- |
| `Dockerfile` | 分别构建 Blog 与 Training 两份 Vue 产物，并复制进同一个 Nginx 镜像 |
| `nginx.conf`、`nginx-https.conf` | Blog/Training history fallback、`/api/**` 代理、托管图片和 HTTP/TLS 入口 |
| `vite.config.ts` | `/training-app/` base、5173 开发服务与 `/api` proxy |
| `src/main.ts`、`src/App.vue` | Vue/Router 挂载、会话恢复、权限分流和页面组合 |
| `src/router/index.ts` | 内部 `/training-app/**` 路由表；公开 `/training/**` 由 Blog Router 持有 |
| `src/routing.ts` | 页面类型和当前有效 Blog/Training 登录回跳白名单 |
| `src/views/TrainingView.vue` | 查询和管理员页面的路由级容器，向 dashboard 传递当前 admin section |
| `src/types.ts` | 页面模型和 Blog API DTO，包括用户更新、比赛/奖项及首页精选组模型 |
| `src/auth/session.ts` | 成对校验、读写和清理共享 JWT/用户摘要 |
| `src/api/client.ts` | `/api` 基址、Result envelope、Bearer header、含 `Retry-After` 的 `ApiError` 和响应文件名解析 |
| `src/api/auth.ts` | 登录、当前用户和本人密码修改 |
| `src/api/training.ts` | 用户目录、单人/多人批量汇总、提交和首 AC 查询 |
| `src/api/admin.ts` | 用户原子更新、比赛/奖项、文章/全量备份、首页精选组、分类/标签分页、采集任务、横幅和精选图片管理 |
| `src/composables/useAuthSession.ts` | 会话恢复、登录竞态、退出与密码修改 |
| `src/composables/usePlatformDashboard.ts` | 按页面加载数据、批量汇总、比赛聚合、分页、首页精选组/精选图片和管理员操作状态编排 |
| `src/utils/adminUsers.ts` | 创建用户文本导入、角色与 handle 行转换 |
| `src/utils/adminTraining.ts` | 固定携带数仓刷新的采集请求构造 |
| `src/components/AppShell.vue` | 独立开发调试顶栏；只读分类目录，嵌入 Blog 时隐藏顶栏且不重复加载 |
| `src/components/LoginPanel.vue` | 统一顶栏下的居中账户式登录表单、五秒冷却倒计时和安全回跳 |
| `public/img/custacm-training-logo.jpg` | Training 登录页中央使用的 CUST ACM 圆形徽章原图 |
| `src/components/LoginFooter.vue` | 在 Training iframe 内复现 Blog 项目与竞赛平台页脚，使登录页无需外层滚动即可访问链接 |
| `src/components/TrainingQueryPanel.vue`、`src/styles/dashboard.css` | 站内风格 OJ 下拉、多人/单人自动筛选、可点击放大镜的全量队员子串搜索、整行题目显式查询和个人 rating 彩色难度条 |
| `src/components/TrainingAdminPanel.vue` | 七个管理员页面的统一导航 |
| `src/components/AdminConfirmDialog.vue` | 创建用户、全量文章下载及删除操作共用的统一确认框 |
| `src/components/AdminUserManagementPanel.vue` | 一个 PUT 保存用户完整编辑，并在 handle 变化前高危确认 |
| `src/components/CategoryAdminPanel.vue` | 分类与标签的独立分页、增删改界面 |
| `src/components/CompetitionAdminPanel.vue` | 规范赛事分类筛选/创建、按分类生成参赛形态请求值、动态奖档/排名字段、参赛用户与奖项增删、七天回收站和恢复界面 |
| `src/components/ArticleAdminPanel.vue` | 同一路由内切换“首页编排”/当前文章/回收站，并承载全量 ZIP 备份、筛选、分页、移入回收站与恢复 |
| `src/components/HomepageFeaturedGroupsPanel.vue` | 最多三组的创建、标题编辑、固定三篇文章选择、全首页去重、失效提示、排序和删除 |
| `src/utils/fileDownload.ts` | 将受保护 API 返回的 Blob 与响应文件名保存为本地文件 |
| `src/components/TrainingDataOpsPanel.vue` | 手动采集任务、详情和数仓刷新 |
| `src/components/HomepageFeaturedImagesAdminPanel.vue` | 最多十二张滚动精选图片的批量队列、3:2 裁剪、缩略图预览、完整排序和删除确认 |
| `src/styles/homepage-featured-images-admin.css` | 精选图片后台卡片网格、计数和裁剪布局 |
| `src/styles/competition-admin.css` | 比赛聚合、奖项层级、表单与回收站布局 |
| `src/components/CreateUsersPanel.vue` | 批量创建用户输入与编辑 |
| `src/styles.css`、`src/styles/*.css` | 固定浅色桌面外壳、管理员确认框、表格、分页和业务页面样式 |
| `src/styles/tokens.css` | 从根共享源生成并在样式入口首先加载的视觉 token 副本；禁止手工编辑 |
| `src/styles/training-redesign.css` | Training 阶段 2 的 Action Blue/暖橙角色、现代圆角、有限玻璃、分级字号和路由动效覆盖 |
| `src/test/platform-dashboard-batch.test.ts` | 多人页面只调用一次批量汇总 API 的回归测试 |
| `src/test/admin-users-vue.test.ts` | 管理员用户单 PUT 与高危确认交互测试 |
| `src/test/category-admin.test.ts` | 分类/标签独立分页测试 |
| `src/test/competition-admin.test.ts` | 比赛创建、参赛用户/奖项操作和回收站确认测试 |
| `src/test/competition-admin-api.test.ts` | 比赛公开列表与管理员写入 API 合同测试 |
| `src/test/homepage-featured-groups-admin.test.ts` | 首页精选组补满三篇后保存、完整排序与删除确认交互测试 |
| `src/test/homepage-featured-images-admin.test.ts` | 精选图片上限、完整排序和最后一张删除确认交互测试 |
| `src/test/app-shell-vue.test.ts` | 独立/嵌入外壳的数据加载与顶栏测试 |
| `src/test/platform-api.test.ts` | Training/Admin API 路径和请求合同测试 |
| `src/test/login-panel-vue.test.ts` | 登录提交与服务端冷却倒计时测试 |
| `src/test/training-redesign-style.test.js`、`admin-confirm-dialog.test.ts` | 共享 token 加载顺序、玻璃范围、路由动效和确认弹层焦点行为测试 |
| `src/test/` | 会话、路由、API、composable 和关键页面回归测试 |

## 本地开发

要求 Node.js 20.19+、pnpm 10.33.2。仓库根目录统一启动：

```bash
./scripts/dev.sh
```

该脚本保留 Docker 中的 MySQL、Redis、Blog API，在宿主机启动 Training Vite 5173 与 Blog Vite 4180。统一入口为 `http://localhost:4180/training/multiple`。

## 构建与验证

```bash
pnpm lint
pnpm test
pnpm typecheck
pnpm build
```

Blog 构建验证：

```bash
cd ../platform-blog/upstream/nblog/blog-view
npm ci
npm test
npm run build
```

Compose 默认入口：

```text
Blog:     http://localhost:3000/
Training: http://localhost:3000/training/multiple
API:      http://localhost:3000/api/health
```
