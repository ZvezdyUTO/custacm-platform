# frontend

`frontend` 是 Vue 3 + Vite + TypeScript 训练中心，也是统一 Nginx 前端镜像的构建目录。

生产环境只有一个 `frontend` Nginx 服务和一个门户；“两份前端”指镜像内的两份 Vue 源码构建，不是两套独立站点或服务：

| 源码构建 | 浏览器路径 | 职责 |
| --- | --- | --- |
| `../platform-blog/upstream/nblog/blog-view` | `/` | 公开 Blog、唯一顶栏和 `/training/**` 外层路由 |
| 本目录 | `/training/**` | 训练查询与管理员工作区；内部静态 base 为 `/training-app/` |
| Blog API | `/api/**` | Nginx 去掉 `/api` 后代理到唯一后端 |

两份 Vue Router 彼此独立。训练运行时通过同源 frame 嵌入 Blog 外壳；进入训练中心时继续使用已挂载的同一个 `Nav.vue`，不会渲染第二条顶栏。

两份构建还共享 `custacm.theme` 主题协议。值只允许 `light` 或 `dark`，没有显式值时跟随浏览器系统偏好；Blog 唯一顶栏以太阳/月亮拨杆提供正式切换入口，Training 独立开发外壳提供同款调试入口。主题在应用样式加载前写入根节点，并通过同源存储事件及受校验的 frame 消息同步，避免刷新白闪或 Blog/Training 明暗不一致。进入深夜主题时业务图片统一以 260ms 轻度降低亮度与饱和度，减少动态效果偏好下立即切换。

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
/training/admin/training
/training/admin/appearance
```

- `multiple`：先读取用户目录，再用一次 `GET /player/training-data/accepted-summaries` 批量取得当前 OJ 的全部汇总。缺失项或整体失败显示可重试错误，不能伪装成 0 题；不再按用户并发调用单人接口。
- `single`：按 username、OJ、日期和难度查询个人汇总、提交与首 AC；队员下拉不自动选择第一项。
- `problem`：按题目、OJ 和日期查询提交与首 AC，保留显式查询按钮。
- `admin/create-users`：文本导入或手动编辑后批量创建账号与 OJ 绑定。
- `admin/users`：编辑账号、改名、密码、角色、完整 OJ handle 集合和采集状态；一次保存只调用 `PUT /admin/users/{username}`。更换或移除 handle 前要求高危确认，服务端在同一事务内清理数据并精确替换绑定。
- `admin/articles`：筛选、分页、切换首页精选状态；删除只移入固定七天回收站，管理员可切换回收站并恢复任意作者文章，还可下载包含全部文章状态、评论、作者资料与托管图片的全量 ZIP。
- `admin/categories`：分类和标签各自分页；分类支持名称/颜色增删改，标签支持新增/删除。
- `admin/training`：创建和查看显式采集任务。自动采集默认关闭，不影响管理员手动触发。
- `admin/appearance`：裁剪、上传、排序和删除最多两张 1920×1080 首页横幅。

训练查询要求 `ROLE_player` 或 `ROLE_admin`；管理员页面只允许 `ROLE_admin`。业务身份统一使用 `username`，固定 `root` 不提供改名、降权、handle、采集状态或删除操作。

当前产品验收范围为 1280–2560 px 桌面端，重点检查 1440×900 与 1920×1080；移动训练业务页不在当前范围。

## 数据加载规则

- 页面只加载当前功能所需数据。多人/单人页加载用户目录；题目页不加载用户目录；管理员页只加载对应 section 的用户、任务、文章、分类/标签或横幅。
- 多人批量接口按 OJ、日期、难度和 `includeRetired` 过滤，响应按 username 与当前目录合并。
- 管理用户列表由后端一次批量装配账号和 handle，不产生逐用户查询。
- 分类和标签使用互不影响的分页状态及分页条；新增/删除后只刷新对应列表。
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

公开 Blog 请求不全局携带 JWT。安全回跳白名单只包含当前 Blog 首页、文章、分类、标签、个人主页、写作页和当前训练路由；已删除的 About/Friends/Moments 路由不再保留。Blog 回跳导航顶层窗口，避免把 Blog 外壳加载进训练 frame。

Blog 文章详情的标题/简介/正文与扁平图片 ZIP 下载同样通过 `/api/player/**` 进入唯一后端并显式携带共享 Bearer；普通用户跨全部文章共享的 30 秒限流和管理员豁免完全由 Blog API 判定，本训练构建不复制该逻辑。管理员全量备份通过 `/api/admin/blogs/backup` 下载，并使用相同的可读 Markdown 与图片命名规则。

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
    theme.ts
    types.ts
```

## 依赖与边界

- Blog API 是唯一后端；前端不签发 JWT、不保存密码、不复制服务端授权和数据清理逻辑。
- `src/api/` 按 auth、training、admin 拆分正式 HTTP contract，组件不自行拼接业务请求。
- `src/composables/useAuthSession.ts` 管理认证生命周期，`usePlatformDashboard.ts` 管理页面数据与操作状态。
- Vue Blog 负责公开内容与唯一顶栏，本应用只负责训练中心；不跨应用复制业务组件或合并 Router。
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
| `src/theme.ts` | 共享日间/深夜主题解析、持久化、系统偏好及同源 frame 同步 |
| `src/views/TrainingView.vue` | 查询和管理员页面的路由级容器，向 dashboard 传递当前 admin section |
| `src/types.ts` | 页面模型和 Blog API DTO，包括 `AdminUserUpdateRequest` |
| `src/auth/session.ts` | 成对校验、读写和清理共享 JWT/用户摘要 |
| `src/api/client.ts` | `/api` 基址、Result envelope、Bearer header、含 `Retry-After` 的 `ApiError` 和响应文件名解析 |
| `src/api/auth.ts` | 登录、当前用户和本人密码修改 |
| `src/api/training.ts` | 用户目录、单人/多人批量汇总、提交和首 AC 查询 |
| `src/api/admin.ts` | 用户原子更新、文章/全量备份、分类/标签分页、采集任务和横幅管理 |
| `src/composables/useAuthSession.ts` | 会话恢复、登录竞态、退出与密码修改 |
| `src/composables/usePlatformDashboard.ts` | 按页面加载数据、批量汇总、分页和管理员操作状态编排 |
| `src/utils/adminUsers.ts` | 创建用户文本导入、角色与 handle 行转换 |
| `src/utils/adminTraining.ts` | 固定携带数仓刷新的采集请求构造 |
| `src/components/AppShell.vue` | 独立开发调试顶栏；只读分类目录，嵌入 Blog 时隐藏顶栏且不重复加载 |
| `src/components/LoginPanel.vue` | 登录表单、五秒冷却倒计时和安全回跳 |
| `src/components/TrainingQueryPanel.vue` | 多人/单人自动筛选、批量错误状态和题目显式查询 |
| `src/components/TrainingAdminPanel.vue` | 六个管理员页面的统一导航 |
| `src/components/AdminConfirmDialog.vue` | 创建用户、全量文章下载及删除操作共用的主题化确认框 |
| `src/components/AdminUserManagementPanel.vue` | 一个 PUT 保存用户完整编辑，并在 handle 变化前高危确认 |
| `src/components/CategoryAdminPanel.vue` | 分类与标签的独立分页、增删改界面 |
| `src/components/ArticleAdminPanel.vue` | 当前文章/回收站切换、全量 ZIP 备份、筛选、分页、精选、移入回收站与恢复 |
| `src/utils/fileDownload.ts` | 将受保护 API 返回的 Blob 与响应文件名保存为本地文件 |
| `src/components/TrainingDataOpsPanel.vue` | 手动采集任务、详情和数仓刷新 |
| `src/components/HomepageBannerAdminPanel.vue` | 横幅裁剪、队列、排序和删除 |
| `src/components/CreateUsersPanel.vue` | 批量创建用户输入与编辑 |
| `src/styles.css`、`src/styles/*.css` | 桌面外壳、管理员确认框、表格、分页，以及含图片渐暗过渡的暖黑橙深夜覆盖 |
| `src/test/platform-dashboard-batch.test.ts` | 多人页面只调用一次批量汇总 API 的回归测试 |
| `src/test/admin-users-vue.test.ts` | 管理员用户单 PUT 与高危确认交互测试 |
| `src/test/category-admin.test.ts` | 分类/标签独立分页测试 |
| `src/test/app-shell-vue.test.ts` | 独立/嵌入外壳的数据加载与顶栏测试 |
| `src/test/platform-api.test.ts` | Training/Admin API 路径和请求合同测试 |
| `src/test/login-panel-vue.test.ts` | 登录提交与服务端冷却倒计时测试 |
| `src/test/dark-theme-style.test.js` | 深夜模式关键表面、状态和实心铜橙按钮前景回归测试 |
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
