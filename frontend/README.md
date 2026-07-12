# frontend

`frontend` 是 Vue 3 + Vite + TypeScript 训练中心。生产环境中，它与 `platform-blog/upstream/nblog/blog-view` 的 Vue 3 + Vite Blog 一起打进同一个 Nginx 镜像：

| 浏览器路径 | 页面/服务 |
| --- | --- |
| `/` | Vue Blog |
| `/training/**` | Vue 3 训练中心 |
| `/api/**` | Nginx 去掉 `/api` 后转发到唯一的 Blog API |

Vue Blog 持有公开 `/training/**` 路由和唯一顶栏，训练内容通过同源 frame 加载内部 `/training-app/**` 产物。两套 Vue Router 仍彼此独立，但进入训练中心时 Blog 的 `Nav.vue` 不会卸载或被复制顶栏替换。

管理员工作区使用独立的勃艮第主题：酒红承担导航和主要操作，陶土色只标记当前管理员页面，暖雾灰作为工作区底色；成功、警告和危险状态继续保留各自语义色。

## 页面与权限

训练中心的正式路由如下：

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

- `multiple`：从受保护用户目录取得允许采集且已绑定 OJ 的用户，以最大并发数 6 拉取个人汇总并生成多人视图；结果按总过题量降序排列，同分时按 `username` 升序排列，查询失败项置于末尾；筛选参数变更后自动刷新，无需查询按钮。
- `single`：按 `username`、OJ、日期和难度范围查询个人汇总、提交与首 AC。
- `problem`：按题目、OJ 和日期范围查询提交与首 AC。
- `admin/create-users`：文本导入后生成可编辑信息行，也可逐行新增或删除，确认后批量创建账号并绑定 OJ handle。
- `admin/users`：承载编辑、改名、密码、角色、OJ handle、采集状态和删除用户；更换已有 OJ handle 时使用高危确认弹窗，确认后调用独立接口清理该 OJ 的全部历史训练数据、旧绑定与采集状态再换绑。永久删除前弹窗说明 OJ 绑定、训练数据清理和评论匿名化影响。空头像回退到构建内置默认头像，固定 `root` 不显示删除入口。
- `admin/articles`：筛选文章、控制首页侧栏精选状态，并在明确提示文章、标签关联和评论不可恢复后执行管理员永久删除。
- `admin/categories`：分类可自定义名称和颜色；标签只允许新增和删除，新增颜色由服务端从连续数值空间随机生成并持久化。有关联文章时后端拒绝删除，Blog 标签云使用深色背景和白字。
- `admin/training`：按 OJ 对全部或单个现役队员发起采集并查看任务；执行前弹窗确认目标、OJ、回看范围和数仓刷新影响，每次采集都固定在完成后刷新数仓。
- `admin/appearance`：选择一张或多张本地图片，逐张裁成 1920×1080，上传后按从左到右的首页切换顺序调整或删除。

训练查询要求 `ROLE_player` 或 `ROLE_admin`；管理员页面只允许 `ROLE_admin`。账号业务身份统一使用 `username`，角色只有 `ROLE_admin` 和 `ROLE_player`。

当前产品验收只覆盖 1280–2560 px 桌面端，重点分辨率为 1440×900 与 1920×1080；移动端不在本阶段范围内。

## 认证与 API

训练中心与 Vue Blog 共享以下浏览器会话：

```text
custacm.accessToken
custacm.user
```

`custacm.user` 是展示摘要，不能作为权限依据。训练中心启动时会调用 `/player/me` 校验 JWT；受保护请求逐个显式发送 `Authorization: Bearer <token>`。只有确定的 401 会清理本地会话，403 和网络错误保留当前会话并显示错误。

所有浏览器 API 使用 `/api/**`。例如用户目录：

```text
浏览器：GET /api/player/training-data/users
Blog API：GET /player/training-data/users
```

该接口只返回可采集且至少绑定一个 OJ 账号的用户摘要：

```json
[
  {
    "username": "player1",
    "nickname": "队员一",
    "ojNames": ["CODEFORCES", "ATCODER"]
  }
]
```

响应不包含邮箱、角色、真实 OJ handle、采集状态或管理员私有字段。

Vue Blog 的公开请求不会全局附加训练 JWT。登录用户提交评论时，Vue 只对该受保护请求显式使用共享 Bearer JWT；密码文章 token 继续使用自己的原始格式。

生产 Nginx 将宿主机 `uploads/` 只读挂载到静态资源目录，直接服务 `/api/image/**` 并对 UUID 托管图片设置长期 immutable 缓存；其他 `/api/**` 请求继续去前缀后代理 Blog API。Nginx 请求体上限为 18m，Blog API 再按头像、首图和正文用途分别执行 2MB、10MB、15MB 限制。

训练登录页的安全回跳白名单覆盖固定训练路由，以及 Vue Blog 的首页、分类、标签、动态、友链、个人页、文章和写作路由；顶栏登录入口携带当前完整路径，登录成功后回到登录前位置。训练路由在内部 Router 回跳，Blog 路由始终导航顶层窗口，不能把 Blog 外壳加载进训练 frame 形成双顶栏。

## 本地开发

要求 Node.js 20.19+、pnpm 10.33.2，且 `deploy/.env` 的 `BACKEND_PORT=8090`。从仓库根目录用一个入口启动 Docker 后端与两份 Vite 前端：

```bash
./scripts/dev.sh
```

脚本停止生产 Nginx，保留 Docker 中的 MySQL、Redis 和 Blog API，在宿主机启动 Training Vite 5173 与 Blog Vite 4180。统一访问 `http://localhost:4180/training/multiple`；Blog Vite 将内部 `/training-app/**` 和训练 HMR 通道代理到 5173，并将 `/api/**` 代理到 8090。修改任一 Vue 应用的源码后会自动热更新；按 Ctrl-C 停止两份 Vite，Docker 后端继续运行。

## 构建与验证

Vue 3 训练中心：

```bash
pnpm lint
pnpm test
pnpm typecheck
pnpm build
```

Vue Blog：

```bash
cd ../platform-blog/upstream/nblog/blog-view
npm ci
npm test
npm run build
```

统一前端镜像会在 `frontend/Dockerfile` 内使用 pnpm 锁定训练依赖，并按 Blog 的 `package.json` 安装其固定版本依赖再构建。Compose 启动后默认入口是：

```text
Blog:     http://localhost:3000/
Training: http://localhost:3000/training/multiple
API:      http://localhost:3000/api/health
```

HTTP 端口由 `deploy/.env` 的 `FRONTEND_PORT` 控制。设置 `TLS_ENABLED=true` 后，Nginx 从只读 `TLS_CERT_DIR` 挂载读取 `origin.pem`、`origin.key`，将 HTTP 重定向到 `FRONTEND_HTTPS_PORT` 对应的 HTTPS 入口。验收或稳定运行时从仓库根目录切回普通模式：

```bash
./scripts/deploy.sh
```

## 目录结构

```text
frontend/
  Dockerfile
  nginx.conf
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
    styles.css
    types.ts
```

## 文件职责

| 文件/目录 | 职责 |
| --- | --- |
| `Dockerfile` | 用 Node 20.19 分别构建两套 Vue 3 应用，再复制到可选择 HTTP 或 HTTPS 配置的 Nginx 1.27 镜像 |
| `nginx.conf`、`nginx-https.conf` | 分别提供 HTTP 与 TLS 配置；两者均支持 Blog `/training/**` 外壳、内部 `/training-app/**` 产物 fallback 和 `/api/**` 反向代理 |
| `vite.config.ts` | 内部 `/training-app/` base、本地开发服务器与 `/api` proxy |
| `src/main.ts` | Vue 应用与 Vue Router 挂载入口 |
| `src/App.vue` | 顶层会话恢复、权限分流与页面组合 |
| `src/router/index.ts` | 内部 `/training-app/**` 路由表与 history base |
| `src/views/TrainingView.vue` | 训练查询和管理员页面的路由级容器 |
| `src/routing.ts` | 训练页面类型与安全登录回跳校验 |
| `src/types.ts` | 当前页面和 Blog API DTO 类型 |
| `src/auth/session.ts` | 共享 JWT/用户摘要的校验、持久化与清理 |
| `src/api/client.ts` | `/api` 基址、Blog envelope 解析、Bearer header 与 `ApiError` |
| `src/api/auth.ts` | 登录、当前用户、修改本人密码 |
| `src/api/training.ts` | 用户目录和多人/单人/题目训练查询 |
| `src/api/admin.ts` | 用户、文章精选、文章分类、OJ handle、采集任务、数仓刷新与首页图片管理 |
| `src/composables/useAuthSession.ts` | 会话恢复、登录竞态隔离、退出与密码修改 |
| `src/composables/usePlatformDashboard.ts` | 查询、分页、用户管理、采集与刷新状态编排 |
| `src/utils/runLimited.ts` | 保持结果顺序的有限并发执行器 |
| `src/utils/adminUsers.ts` | 创建用户文本导入、角色和 handle 行模型转换 |
| `src/utils/adminTraining.ts` | 构造固定刷新数仓的采集请求 |
| `src/components/AppShell.vue` | 训练内容外壳；独立开发时提供调试顶栏，同源嵌入 Blog 时不渲染第二条顶栏 |
| `src/components/LoginPanel.vue` | 登录表单和安全回跳 |
| `src/components/TrainingQueryPanel.vue` | 多人、单人筛选参数短防抖后自动刷新；单人查询不预选队员；题目查询保留深色显式查询按钮 |
| `src/components/TrainingAdminPanel.vue` | 创建用户、管理用户、管理文章、管理分类、数据采集、首页图片六个独立管理员页面的导航容器 |
| `src/components/ArticleAdminPanel.vue` | 文章筛选、分页、首页侧栏精选状态控制与带危险确认的永久删除 |
| `src/components/CategoryAdminPanel.vue` | 文章分类分页、新增、改名和删除 |
| `src/components/HomepageBannerAdminPanel.vue` | 最多两张首页图片的多选、固定比例裁剪、左右排序与删除；未满时在列表末尾显示透明加号卡片 |
| `src/components/CreateUsersPanel.vue` | 文本导入、可编辑创建行与批量创建提交 |
| `src/components/AdminUserManagementPanel.vue` | 以只读头像和宽幅个人信息列集中展示账号、邮箱与 OJ handle，并管理已有用户的角色、密码和采集状态；已有 handle 变化必须经过高危清理确认；头像仅能通过本人图片上传接口更新 |
| `src/components/TrainingDataOpsPanel.vue` | 全部/单人采集、固定数仓刷新与采集任务记录 |
| `src/styles/theme.css` | Blog 风格的颜色、字体和视觉变量 |
| `src/styles/homepage-banner-admin.css` | 首页图片列表和裁剪弹窗样式 |
| `src/styles/article-admin.css` | 管理文章列表、筛选区和精选开关样式 |
| `src/styles/category-admin.css` | 管理分类表单、列表和操作按钮样式 |
| `src/styles.css`、其余 `src/styles/*.css` | 样式入口、桌面外壳、内容、表格和侧栏规则 |
| `src/test/session.test.ts` | 共享会话读写与坏数据清理测试 |
| `src/test/routing.test.ts` | 安全登录回跳测试 |
| `src/test/login-panel-vue.test.ts` | 登录表单和安全回跳测试 |
| `src/test/run-limited.test.ts` | 有限并发、顺序与失败传播测试 |
| `src/test/` | API、认证、路由、并发器、composable 与 Vue 页面回归测试 |

## 模块边界

- Blog API 是唯一后端，前端不签发 JWT、不保存密码、不复制服务端授权逻辑。
- 训练数据查询和管理都通过 `/player/**`、`/admin/**` 的正式 HTTP contract 完成。
- 原始数据写入仍是后端接口能力，不在当前管理员 UI 中暴露。
- Vue Blog 继续负责公开内容浏览与评论界面；独立的 Vue 3 应用负责训练中心。
