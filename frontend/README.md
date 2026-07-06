# frontend

`frontend` 是 `custacm-platform` 的第一个可运行前端切片。当前实现是 React + Vite + TypeScript 的训练数据管理面板，并已接入本地真实后端接口。页面分为两个工作区：

- `训练查询`：默认入口，包含单人查询和多人统计两个页面；单人查询围绕单个选手展示 Codeforces AC、倒序提交、倒序最近通过和 rating 分布，并支持手动选择日期/rating 查询范围；提交明细按后端分页展示，当前支持每页 50/100/200 条和上一页/下一页切换；多人统计读取所有已开启自动更新选手的 DWS AC 汇总，按总通过题量降序表格展示；左侧按功能模块展示入口，模块列表带图标辅助识别，并把可用功能和暂未开放功能分组展示；当前仅训练数据管理模块可用，博客模块和编辑器模块显示为未支持；
- `管理员操作`：admin 登录后才展示，是独立于训练查询的管理员工作区；左侧菜单切换用户信息、数据采集、数据维护、操作记录四个页面。用户信息页负责文本导入填充创建信息栏、创建账号、补充 Codeforces 绑定，并在按学号降序展示的所有账号列表中展开修改角色/密码、Codeforces handle 和自动采集状态；数据采集页只列出已开启自动采集的绑定选手，支持按统一回看小时数一键全部采集（默认 1440 小时，可清空为不限时间范围），也支持每行按单个选手设置回看小时数并启动采集任务，页面下方轮询当前采集任务列表并支持展开详情；数据维护页负责 ODS 上传、仓库刷新，以及高危删除某个用户的 Codeforces 训练数据和 auth 账号；操作记录页展示账号/数据列表和告警信息。

工作区和页签使用浏览器路径保存当前页面，刷新或直接打开链接不会回到默认页：

```text
/query/multiple
/query/single
/admin/users
/admin/collection
/admin/maintenance
/admin/records
```

- `auth-web`：登录、当前用户、公开用户列表、admin 批量创建账号、admin 用户角色更新、密码重置和 admin 删除账号；
- `training-data-web`：服务健康、Codeforces handle 单个公开查询与 admin 创建、Codeforces handle 自动采集开关更新、单人 DWS AC 汇总、自动更新选手 DWS AC 汇总列表、DWD 提交明细、DWM 首 AC 明细、admin ODS upsert、admin warehouse refresh、admin recent-lookback 采集任务 start/list/detail 和 admin Codeforces 用户数据清理。

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
```

登录页面不会内置密码。使用 `deploy/.env` 中配置的 bootstrap admin 或其它 admin 账号登录。

## 本地种子数据

仓库内有稳定 Codeforces fixture。可用脚本通过真实 HTTP API 准备一组本地演示数据：

```bash
./scripts/seed-local-codeforces-data.sh
```

脚本会执行：

1. 调用 `POST /api/auth/login` 获取 admin token；
2. 调用 `POST /api/auth/admin/users:batch-create` 创建或复用样例账号；
3. 调用 `POST /api/training-data/admin/codeforces/handles` 创建或复用 Codeforces handle 绑定；
4. 调用 `POST /api/training-data/admin/ods/codeforces/submissions:batch-upsert` 写入 fixture；
5. 调用 `POST /api/training-data/admin/codeforces/warehouse:refresh` 刷新 DWD/DWM/DWS；
6. 调用公开 DWS 查询接口打印 AC 汇总验证结果。

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
`/api/training-data/**` 和 health 路径反向代理到 Compose 内部后端服务。

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

渲染验证需要真实浏览器打开 `http://localhost:5173/` 或 Compose 前端地址，并确认训练查询范围筛选、左侧模块入口与模块图标、登录、admin 操作区隔离、左侧管理员菜单切换、用户信息创建、所有用户表按学号倒序、用户列表内展开修改、数据采集逐行确认弹窗、逐人采集结果、采集任务列表轮询与展开详情、数据维护页 ODS 上传并刷新、手动刷新、高危用户数据删除确认弹窗、选手切换、真实训练数据展示、console、桌面/移动布局。

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
- `src/api/platform.ts`：auth/training-data HTTP client 和错误封装，包括公开 auth 用户列表、公开 Codeforces handle 单个查询、单人 DWS 查询、自动更新选手 DWS 汇总查询、Codeforces handle 创建/自动采集开关更新、采集任务 start/list/detail、auth 用户删除和 Codeforces 用户数据清理接口。
- `src/hooks/usePlatformDashboard.ts`：登录态、训练查询范围、公开 auth 用户列表驱动的游客查询、单人/多人训练查询刷新、用户创建/修改、Codeforces handle 自动采集开关、ODS 文件上传、批量采集后台任务轮询、采集任务列表、仓库刷新和彻底删除用户数据状态编排。
- `src/utils/dashboardModels.ts`：把真实 API 响应派生为指标、表格、告警、时间线和权限概览。
- `src/data/dashboard.ts`：本地种子 identity 列表和默认采集小时数。
- `src/components/`：应用壳、登录面板、训练查询面板、用户信息管理页（含所有账号倒序总览和列表内编辑）、训练数据采集页、数据维护页（含高危操作确认）、管理员工具栏、表格、侧栏和状态条。
- `src/App.tsx`：工作区组合、URL 路径与页签状态同步、登录弹窗、全局操作提示和跨面板事件编排。
- `src/test/`：筛选、工具栏和表格行为测试。
