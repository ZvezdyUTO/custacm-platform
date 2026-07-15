# frontend

`frontend` 是 Vue 3 + Vite + TypeScript Training 应用，也是统一 Nginx 前端镜像的构建目录。

## 运行边界

生产环境只有一个 `frontend` Nginx 服务。镜像内包含两份独立的 Vue 3 构建：

| 构建 | 路径 | 职责 |
| --- | --- | --- |
| `../platform-blog/upstream/nblog/blog-view` | `/`、`/training/**` | 公开 Blog、唯一顶栏和 Training 外壳 |
| 本目录 | 内部 `/training-app/**` | 训练查询与管理员工作区 |
| Blog API | 浏览器 `/api/**` | 唯一后端；Nginx 去掉 `/api` 后转发 |

两套 Vue Router 不合并。Blog 外壳通过同源 frame 加载 Training，因而页面切换时仍使用同一条 Blog 顶栏。

两份构建共享日间/夜间视觉语义。首次访问使用日间，Blog 顶栏提供手动开关，选择写入 `custacm.theme` 并同步给同源 Training frame；Training 运行时通过 `src/theme.ts`、`src/styles/light.css` 和 `src/styles/dark.css` 应用对应主题。

## 路由范围

公开 Training 路由为：

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

Training Router 在 `/training-app/` base 下维护对应内部路由。查询页面要求 `ROLE_player` 或 `ROLE_admin`，管理员页面仅允许 `ROLE_admin`；最终授权始终由 Blog API 执行。

主要职责：

- 多人、单人和题目训练数据查询；多人统计使用单次批量汇总接口。
- 创建和管理账号，并以一个原子请求保存账号、角色、密码、完整 OJ handle 集合和采集状态。
- 管理文章与回收站、首页精选分组、分类与标签、比赛与奖项、显式采集任务和首页滚动精选图片。

## 认证与 API

Blog 与 Training 共享：

```text
custacm.accessToken
custacm.user
```

`custacm.user` 只是展示摘要。启动时调用 `/player/me` 验证会话；只有 401 清理本地会话，403 与网络错误保留会话并显示错误。

公开请求不得全局附加 JWT。`src/api/client.ts` 提供统一 `/api` 基址和响应处理，`src/api/auth.ts`、`training.ts`、`admin.ts` 等受保护 adapter 显式发送 Bearer token。组件不直接拼接业务请求。

登录失败冷却由 Blog API 按 username 执行，Training 只读取 `Retry-After` 展示剩余时间。安全回跳由 `safeReturnPath` 白名单控制，并导航顶层窗口，避免把 Blog 外壳再次加载进 Training frame。

## 目录

```text
frontend/
  Dockerfile                 两份 Vue 构建和统一 Nginx 镜像
  nginx.conf                 HTTP 网关与 history fallback
  nginx-https.conf           TLS 网关与 history fallback
  docker-entrypoint.d/       按 TLS_ENABLED 选择 Nginx 配置
  package.json
  pnpm-lock.yaml
  vite.config.ts             /training-app/ base、开发端口和 API proxy
  public/                    Training 静态资源
  src/
    api/                     auth、training、admin HTTP adapter
    auth/                    共享会话读写
    components/              查询、登录和管理页面组件
    composables/             会话与页面数据编排
    router/                  Training 内部路由
    styles/                  共享 token 副本和 Training 样式
    test/                    路由、会话、API 与交互回归测试
    utils/                   生产代码复用的纯函数
    views/                   路由级页面容器
    App.vue
    main.ts
    routing.ts
    types.ts
```

共享视觉 token 的唯一源是 `../frontend-design-tokens/tokens.css`。`src/styles/tokens.css` 是生成副本，使用 `../scripts/sync-design-tokens.sh` 更新，不得手工编辑。

## 本地开发

要求 Node.js 20.19+ 与 pnpm 10.33.2。从仓库根运行：

```bash
cp deploy/.env.example deploy/.env
# 替换所有 change-me 值
./scripts/dev.sh
```

开发入口为：

```text
Blog:     http://localhost:4180/
Training: http://localhost:4180/training/multiple
API:      http://localhost:8090/health
```

`dev.sh` 保留 Docker 中的 MySQL、Redis 和 Blog API，在宿主机启动 Training Vite 5173 与 Blog Vite 4180；开发模式要求 `BACKEND_PORT=8090`。Ctrl-C 只停止两份 Vite，后端容器继续运行。

完整部署与升级流程见 [deploy/README.md](../deploy/README.md)。

## 验证

Training 变更在本目录运行：

```bash
pnpm install --frozen-lockfile
pnpm lint
pnpm test
pnpm typecheck
pnpm build
```

改动共享网关、两端视觉 token 或跨应用路由时，还要验证 Blog：

```bash
cd ../platform-blog/upstream/nblog/blog-view
npm install
npm test
npm run build
```

部署配置检查：

```bash
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
```

浏览器至少检查 `/`、`/training/multiple`、刷新 fallback、登录/退出、player/admin 权限隔离和 `/api/health`。
