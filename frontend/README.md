# frontend

`frontend` 是 `custacm-platform` 的第一个可运行前端切片。当前实现是 React + Vite + TypeScript 的训练数据管理面板，并已接入本地真实后端接口。页面分为两个工作区：

- `训练查询`：默认入口，包含多人统计、单人查询和题目查询三个页面；查询区可以在 Codeforces 和 AtCoder 之间切换，OJ、队员、题号、日期和 rating 筛选变更后自动查询，刷新页面会自动加载默认最近 7 天范围的多人统计，单人和题目查询页也默认带入该起始日期。单人查询围绕单个队员展示指定 OJ 的 AC、倒序提交、后端分页最近通过和 rating 分布；题目查询按题目编号展示后端分页提交明细和首 AC handle 列表；提交明细、最近通过题目和首 AC handle 列表都按后端 `page + limit` 分页展示，默认每页 15 条，当前都支持每页 15/50/100/200 条和上一页/下一页切换；多人统计不调用后端自动汇总接口，而是按现役队员且绑定当前 OJ handle 的队员逐个读取公开单人 AC 汇总并按通过题数降序展示；左侧按功能模块展示入口，模块列表带图标辅助识别，并把可用功能和暂未开放功能分组展示；当前仅训练数据管理模块可用，博客模块和编辑器模块显示为未支持；
- `管理员操作`：admin 登录后才展示，是独立于训练查询的管理员工作区；左侧菜单切换创建用户、管理用户、数据采集、操作记录四个页面。创建用户页负责文本导入填充创建信息栏、通过批量创建接口创建账号并补充 Codeforces/AtCoder OJ handle 绑定；管理用户页在按学号降序展示的所有账号列表中展开修改角色/密码、补充空缺 OJ handle、迁移 OJ handle 绑定的 `studentIdentity`、调整现役/退役标记和删除用户信息，同时展示每个已绑定 OJ 是否已经采到历史最早数据和最近采集时间；`root` 只展示账号管理项，不展示现役/退役标记，也不提供 OJ 绑定入口；数据采集页按 OJ 列出现役队员中已绑定对应 handle 的队员，支持按统一回看小时数一键全部采集（默认 1440 小时，可清空为不限时间范围），也支持每行按单个队员设置回看小时数并启动采集任务；页面下方轮询当前采集任务列表并支持展开详情；操作记录页展示账号/数据列表和告警信息。登录后右上角账号区支持当前用户修改自己的密码。

工作区和页签使用浏览器路径保存当前页面，刷新或直接打开链接不会回到默认页：

```text
/query/multiple
/query/single
/query/problem
/admin/user-create
/admin/user-edit
/admin/collection
/admin/records
```

- `auth-web`：服务健康/module-info、登录、当前用户、当前用户修改密码、公开用户列表、admin 批量创建账号、admin 用户角色更新、密码重置和 admin 删除账号；
- `training-data-web`：服务健康/module-info、OJ handle 公开全量 `studentIdentity -> account` map（含 per-OJ collection state）与 admin 创建/更新/身份迁移、单人 DWS AC 汇总、DWD 按队员/题目提交明细、DWM 按队员/题目首 AC 明细、admin recent-lookback OJ 采集任务 start/list 和 admin OJ 用户训练数据清理。

前端不修改认证模型，不签发 JWT，不处理密码存储，也不拆分 `studentIdentity`。

## 本地运行

首次安装依赖：

```bash
pnpm install
```

确保本地后端可用：

```text
auth-web:           http://localhost:8081
training-data-web:  http://localhost:8082
```

启动前端：

```bash
pnpm dev -- --host 0.0.0.0
```

默认访问地址：

```text
http://localhost:5173/
```

Vite dev server 会把这些同源路径代理到后端，避免浏览器跨域：

```text
/api/auth/**          -> http://localhost:8081/api/auth/**
/api/training-data/** -> http://localhost:8082/api/training-data/**
/health/auth          -> http://localhost:8081/health
/health/training-data -> http://localhost:8082/health
/module-info/auth     -> http://localhost:8081/module-info
/module-info/training-data -> http://localhost:8082/module-info
```

登录页面不会内置密码。使用 `deploy/.env` 中配置的 bootstrap admin 或其它 admin 账号登录；勾选“记住我一个月”会请求后端签发 30 天有效期的登录 token。

## 本地种子数据

可用脚本通过真实 HTTP API 创建一组本地演示账号和 OJ handle，并启动 Codeforces 采集任务：

```bash
./scripts/seed-local-codeforces-data.sh
```

脚本会执行：

1. 调用 `POST /api/auth/login` 获取 admin token；
2. 调用 `POST /api/auth/admin/users:batch-create` 创建或复用样例账号；
3. 调用 `POST /api/training-data/admin/oj-handles` 创建或复用 OJ handle 绑定，请求体使用 `handles` map，例如 `{"CODEFORCES":"tourist"}`；
4. 调用 `POST /api/training-data/admin/codeforces/submissions:collect-batch-jobs` 启动采集任务；
5. 轮询采集任务列表，采集完成后调用公开 DWS 查询接口打印 AC 汇总验证结果。

脚本不会打印 token 或密码。

## 一键部署

项目级部署入口是：

```bash
./scripts/deploy.sh
```

`deploy/docker-compose.yml` 会构建并启动：

- `auth-db`
- `custacm-backend` (`platform-auth/auth-web`)
- `training-data-db`
- `custacm-training-data-web` (`platform-training-data/training-data-web`)
- `custacm-frontend`

部署脚本会先运行 `frontend-build` 一次性服务生成 `frontend/dist`。运行态
`custacm-frontend` 使用固定 `nginx:1.27-alpine` 镜像，挂载
`frontend/dist` 和 `frontend/nginx.conf`，把 `/api/auth/**`、
`/api/training-data/**`、health 和 module-info 路径反向代理到 Compose 内部后端服务。

只改前端时可以运行：

```bash
./scripts/update-module.sh frontend
```

该命令只刷新静态产物并 reload Nginx，不重建前端镜像。

默认前端容器访问地址：

```text
http://localhost:3000/
```

## 验证

```bash
pnpm lint
pnpm test
pnpm typecheck
pnpm build
```

渲染验证需要真实浏览器打开 `http://localhost:5173/` 或 Compose 前端地址，并确认训练查询范围筛选自动触发、刷新后多人统计自动加载、切换 OJ 后自动刷新当前查询、左侧模块入口与模块图标、登录、当前用户改密码、admin 操作区隔离、左侧管理员菜单切换、创建用户页、管理用户页、所有用户表按学号倒序、用户列表内展开修改、OJ handle 修改/身份迁移、最早采集覆盖状态、最近采集时间、数据采集逐行确认弹窗、逐人采集结果、采集任务列表轮询与展开详情、队员切换、题目查询、真实训练数据展示、console、桌面/移动布局。

## 目录结构

```text
frontend/
  nginx.conf
  index.html
  public/
  package.json
  vite.config.ts
  tsconfig.json
  eslint.config.js
  src/
    api/
    components/
    data/
    hooks/
    styles/
    test/
    utils/
    App.tsx
    main.tsx
    styles.css
```

## 文件职责

- `nginx.conf`：生产/Compose 前端同源 API 反向代理配置。
- `vite.config.ts`：React 插件、本地端口和 dev proxy。
- `src/api/platform.ts`：auth/training-data HTTP client 和错误封装，包括服务 module-info、公开 auth 用户列表、当前用户改密码、公开 OJ handle 全量 map 查询、单人/题目 DWD/DWM/DWS 查询、OJ handle 创建/更新/身份迁移、采集任务 start/list、auth 用户删除和 OJ 用户训练数据清理接口。
- `src/hooks/usePlatformDashboard.ts`：登录态、训练查询范围、当前 OJ 选择、公开 auth 用户列表和 OJ handle 全量列表驱动的游客查询、单人/多人/题目训练查询自动刷新、提交明细和 DWM 首 AC 明细后端分页状态、当前用户改密码、用户创建/修改、OJ handle 绑定/迁移与现役/退役开关、批量采集后台任务轮询、采集任务列表和彻底删除用户数据状态编排。
- `src/utils/dashboardModels.ts`：把真实 API 响应派生为指标、表格、告警、时间线和权限概览。
- `src/data/dashboard.ts`：本地种子 identity 列表和默认采集小时数。
- `src/components/`：应用壳（含当前用户改密码入口）、登录面板、训练查询面板（多人/单人/题目筛选即查询）、创建/管理用户页（含所有账号倒序总览和列表内编辑）、训练数据采集页、管理员工具栏、表格、侧栏和状态条。
- `src/App.tsx`：工作区组合、URL 路径与页签状态同步、登录弹窗、全局操作提示和跨面板事件编排。
- `src/test/`：筛选、工具栏和表格行为测试。
