# frontend

`frontend` 是 Vue 3 + Vite + TypeScript 训练中心。生产环境中，它与 `platform-blog/upstream/nblog/blog-view` 的 Vue 3 + Vite Blog 一起打进同一个 Nginx 镜像：

| 浏览器路径 | 页面/服务 |
| --- | --- |
| `/` | Vue Blog |
| `/training/**` | Vue 3 训练中心 |
| `/api/**` | Nginx 去掉 `/api` 后转发到唯一的 Blog API |

Vue Blog 持有公开 `/training/**` 路由和唯一顶栏，训练内容通过同源 frame 加载内部 `/training-app/**` 产物。两套 Vue Router 仍彼此独立，但进入训练中心时 Blog 的 `Nav.vue` 不会卸载或被复制顶栏替换。

## 页面与权限

训练中心的正式路由如下：

```text
/training/login
/training/multiple
/training/single
/training/problem
/training/admin/create-users
/training/admin/users
/training/admin/training
/training/admin/appearance
```

- `multiple`：从受保护用户目录取得允许采集且已绑定 OJ 的用户，以最大并发数 6 拉取个人汇总并生成多人视图。
- `single`：按 `username`、OJ、日期和难度范围查询个人汇总、提交与首 AC。
- `problem`：按题目、OJ 和日期范围查询提交与首 AC。
- `admin/create-users`：文本导入后生成可编辑信息行，也可逐行新增或删除，确认后批量创建账号并绑定 OJ handle。
- `admin/users`：承载编辑、改名、密码、角色、OJ handle、采集状态和删除用户；“保存修改”会一次提交账号字段、handle 与现役/退役状态，全部成功后才显示成功。
- `admin/training`：按 OJ 对全部或单个现役队员发起采集并查看任务；每次采集都固定在完成后刷新数仓。
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

## 本地开发

要求 Node.js 20.19+、pnpm 10.33.2，并先启动监听 `http://localhost:8090` 的 Blog API。

分别在两个终端启动训练应用和 Blog 外壳：

```bash
# 终端 1：训练应用
cd frontend
corepack enable
pnpm install --frozen-lockfile
pnpm dev

# 终端 2：统一 Blog 外壳
cd platform-blog/upstream/nblog/blog-view
npm ci
npm run dev
```

统一访问 `http://localhost:4180/training/multiple`。Blog Vite 在 4180 提供公开路由，将内部 `/training-app/**` 和训练 HMR 通道代理到 5173，并将 `/api/**` 代理到 8090。修改任一 Vue 应用的源码后会自动热更新；生产环境仍由 Nginx 提供构建后的静态资源。

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

统一前端镜像会在 `frontend/Dockerfile` 内执行这两套锁定安装与构建。Compose 启动后默认入口是：

```text
Blog:     http://localhost:3000/
Training: http://localhost:3000/training/multiple
API:      http://localhost:3000/api/health
```

端口由 `deploy/.env` 的 `FRONTEND_PORT` 控制。只重建前端服务可运行：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build frontend
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
| `Dockerfile` | 用 Node 20.19 分别构建两套 Vue 3 应用，再复制到 Nginx 1.27 镜像 |
| `nginx.conf` | Blog `/training/**` 外壳、内部 `/training-app/**` 产物 fallback 和 `/api/**` 反向代理 |
| `vite.config.ts` | 内部 `/training-app/` base、本地开发服务器与 `/api` proxy |
| `src/main.ts` | Vue 应用与 Vue Router 挂载入口 |
| `src/App.vue` | 顶层会话恢复、权限分流与页面组合 |
| `src/router/index.ts` | 内部 `/training-app/**` 路由表与 history base |
| `src/views/TrainingView.vue` | 训练查询和管理员页面的路由级容器 |
| `src/routing.ts` | 训练路径、管理员独立页面与安全登录回跳解析 |
| `src/types.ts` | 当前页面和 Blog API DTO 类型 |
| `src/auth/session.ts` | 共享 JWT/用户摘要的校验、持久化与清理 |
| `src/api/client.ts` | `/api` 基址、Blog envelope 解析、Bearer header 与 `ApiError` |
| `src/api/auth.ts` | 登录、当前用户、修改本人密码 |
| `src/api/training.ts` | 用户目录和多人/单人/题目训练查询 |
| `src/api/admin.ts` | 用户、OJ handle、采集任务、数仓刷新与首页图片管理 |
| `src/composables/useAuthSession.ts` | 会话恢复、登录竞态隔离、退出与密码修改 |
| `src/composables/usePlatformDashboard.ts` | 查询、分页、用户管理、采集与刷新状态编排 |
| `src/utils/runLimited.ts` | 保持结果顺序的有限并发执行器 |
| `src/utils/adminUsers.ts` | 创建用户文本导入、角色和 handle 行模型转换 |
| `src/utils/adminTraining.ts` | 构造固定刷新数仓的采集请求 |
| `src/components/AppShell.vue` | 训练内容外壳；独立开发时提供调试顶栏，同源嵌入 Blog 时不渲染第二条顶栏 |
| `src/components/LoginPanel.vue` | 登录表单和安全回跳 |
| `src/components/TrainingQueryPanel.vue` | 多人、单人、题目查询界面 |
| `src/components/TrainingAdminPanel.vue` | 创建用户、管理用户、数据采集、首页图片四个独立管理员页面的导航容器 |
| `src/components/HomepageBannerAdminPanel.vue` | 最多两张首页图片的多选、固定比例裁剪、左右排序与删除；未满时在列表末尾显示透明加号卡片 |
| `src/components/CreateUsersPanel.vue` | 文本导入、可编辑创建行与批量创建提交 |
| `src/components/AdminUserManagementPanel.vue` | 已有用户的账号、角色、密码、采集状态与 OJ handle 管理 |
| `src/components/TrainingDataOpsPanel.vue` | 全部/单人采集、固定数仓刷新与采集任务记录 |
| `src/styles/theme.css` | Blog 风格的颜色、字体和视觉变量 |
| `src/styles/homepage-banner-admin.css` | 首页图片列表和裁剪弹窗样式 |
| `src/styles.css`、其余 `src/styles/*.css` | 样式入口、桌面外壳、内容、表格和侧栏规则 |
| `src/test/session.test.ts` | 共享会话读写与坏数据清理测试 |
| `src/test/routing.test.ts` | 路径、管理员 section 与安全回跳测试 |
| `src/test/login-panel-vue.test.ts` | 登录表单和安全回跳测试 |
| `src/test/run-limited.test.ts` | 有限并发、顺序与失败传播测试 |
| `src/test/` | API、认证、路由、并发器、composable 与 Vue 页面回归测试 |

## 模块边界

- Blog API 是唯一后端，前端不签发 JWT、不保存密码、不复制服务端授权逻辑。
- 训练数据查询和管理都通过 `/player/**`、`/admin/**` 的正式 HTTP contract 完成。
- 原始数据写入仍是后端接口能力，不在当前管理员 UI 中暴露。
- Vue Blog 继续负责公开内容浏览与评论界面；独立的 Vue 3 应用负责训练中心。
