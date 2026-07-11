# Blog 与训练中心前端整合设计

## 1. 目标

在不推翻现有 Blog 和训练数据页面的前提下，交付一个可实际使用的桌面端初版：

- Blog 继续作为公开首页。
- Blog 顶部导航增加「训练中心」。
- 登录后的队员和管理员可以访问训练数据查询。
- 管理员在统一管理页面中切换「用户管理」和「训练数据管理」。
- 训练查询继续沿用现有 React 前端反复打磨过的高密度筛选器、多人 rating 表格、单人查询和题目查询排版。
- Blog 与训练中心使用同一个域名、同一账号和同一 Blog 主题语言。

初版只面向桌面浏览器。正式验收宽度为 1280px 到 2560px，重点检查 1440×900 和 1920×1080。移动端导航、触摸交互和卡片重排不在本轮范围。

## 2. 已选方案

采用“同域双前端，产品体验统一”的方式：

- NBlog `blog-view` 继续使用 Vue 2，保留公开文章、分类、归档、标签、动态和关于页。
- 现有 `frontend` 继续使用 React、Vite 和 TypeScript，负责训练中心。
- Nginx 在同一站点下按路径分发两个静态产物，并把 `/api/**` 反向代理到唯一 Blog API。
- 两个前端通过同源存储共享登录摘要，不引入微前端框架或跨框架组件运行时。

未选择的方案：

1. React 总壳加载 Vue 子应用。它会引入生命周期、样式隔离和构建编排成本，对初版没有足够收益。
2. 将 Blog 全量迁移到 React。它需要重写文章详情、评论、搜索、归档、动态和首页动画，范围远超“先能用”的目标。

## 3. 路由和部署边界

### 3.1 浏览器路由

Vue Blog 负责：

```text
/
/home
/blog/**
/archives
/category/**
/tag/**
/moments
/friends
/about
```

React 训练中心负责：

```text
/training/login
/training/multiple
/training/single
/training/problem
/training/admin
```

`/training` 重定向到 `/training/multiple`。原 Blog `/login` 路由改为跳转 `/training/login`，避免存在两套登录体验。

### 3.2 API 路由

浏览器统一请求 `/api/**`，Nginx 去掉 `/api` 前缀后转发到 Blog API。例如：

```text
POST /api/login
GET  /api/player/me
GET  /api/player/training-data/accepted-summary
GET  /api/admin/users
POST /api/admin/training-data/submission-collection-jobs
```

这样可以避免 Blog 的 `/archives`、`/about`、`/blog/**` 页面路由与同名后端接口冲突。后端对外 HTTP 合同本身仍保持 `/login`、`/player/**` 和 `/admin/**`。

Nginx 路由优先级为：

1. `/api/**` 转发 Blog API。
2. `/training/**` 使用 React 产物及其 history fallback。
3. 其它路径使用 Vue Blog 产物及其 history fallback。

Compose 增加一个 Nginx 前端服务，同时挂载或包含两个构建产物；MySQL、Redis 和 Blog API 继续保持现有单体后端部署方式。

## 4. 登录和权限

### 4.1 共享状态

两个前端共享以下同源 `localStorage` 键：

```text
custacm.accessToken
custacm.user
```

`custacm.user` 只保存展示所需的 `username`、昵称、头像和角色。它不是授权依据。

React 训练中心启动时必须携带 JWT 调用 `/api/player/me` 重新加载当前用户。Blog 顶栏可以先用本地摘要完成首屏展示，但进入受保护页面时同样以 Blog API 校验结果为准。

### 4.2 导航行为

- 游客可以浏览所有公开 Blog 页面。
- 游客点击「训练中心」时进入 `/training/login`。
- 登录成功后返回进入登录页前的训练中心目标地址；没有目标地址时进入 `/training/multiple`。
- `ROLE_player` 和 `ROLE_admin` 均可访问训练查询。
- 只有 `ROLE_admin` 显示并可访问 `/training/admin`。
- 管理员在 Blog 顶栏和训练中心都看到当前身份及退出入口。

### 4.3 失效处理

- `401`：清理共享登录状态，跳转 `/training/login`，并保留安全的返回地址。
- `403`：保留当前登录状态，显示权限不足页面，不反复跳转。
- 用户名修改成功：清理登录状态并要求重新登录。
- 用户被删除、角色变化或 JWT 过期：下次受保护请求立即按后端结果生效。

## 5. 视觉和页面外壳

训练中心采用已确认的“Blog 外壳 + 原训练主页面”方向：

- 顶部使用 Blog 的黑色导航栏、青色激活态、站点名称和账号区。
- 训练中心不复用 Blog 首页的全屏大图，避免把数据内容推到首屏以下。
- 移除现有 React 工作台左侧模块导航。
- 顶部第二行承载训练中心内部导航。
- 内容区沿用 Blog 的浅灰背景、白色面板、轻阴影和青色强调色。
- 主内容最大宽度约 1400px，并居中展示。
- 表格保持高信息密度，不改成营销式卡片或宽松仪表盘。

Vue 和 React 不共享运行时组件包。两边各自实现结构一致的顶栏，并使用同名设计变量保持颜色、尺寸和状态一致。初版允许少量 CSS 重复，以换取构建稳定性。

## 6. 训练查询页面

### 6.1 页面导航

训练中心顶部第二行提供：

```text
多人统计 | 单人查询 | 题目查询 | 管理员操作（仅管理员）
```

前三项映射到独立 URL，刷新和直接访问时保持当前位置。

### 6.2 多人统计

默认页面保留当前 React 前端的核心结构：

1. 页面标题和数据更新时间。
2. OJ、日期范围、最低 rating、最高 rating 等筛选器。
3. 全部队员做题量统计表。

表格列为：

- 队员 username 和昵称。
- 当前 OJ 的 handle 展示值。
- 通过题目总计。
- 各 rating 区间通过数。

表格按通过题目总数降序。队员列保持吸附，rating 列允许横向滚动，以适应桌面端较窄窗口。

### 6.3 单人查询

保留现有信息结构：

- OJ 和队员选择。
- 日期与 rating 筛选。
- 通过题目数、提交数量和分页摘要。
- rating 分布。
- 提交明细和首次通过题目切换。

### 6.4 题目查询

保留现有信息结构：

- OJ、题目编号和日期筛选。
- 提交总数及首 AC 人数。
- 提交队员、判题结果和提交时间。
- 首 AC 队员及 handle。

筛选条件变化后自动发起查询。旧请求需要被取消或忽略，不能让较慢的旧响应覆盖较新的筛选结果。

## 7. 统一管理页面

`/training/admin` 是一个页面壳，顶部只有两个一级菜单：

```text
用户管理 | 训练数据管理
```

两个菜单切换页面内部模块，不再保留“创建用户、管理用户、数据采集、操作记录”四个并列一级页面。

### 7.1 用户管理

用户管理以用户表格为主体，保留现有前端的列表内展开编辑方式，并提供：

- 单个创建和批量创建。
- username、昵称、邮箱、头像和角色修改。
- 密码重置。
- Codeforces/AtCoder handle 与 `needCollect` 修改。
- handle 历史采集覆盖状态和最后采集时间。
- 用户删除。

创建账号或重置密码后，一次性明文密码只在本次结果区域展示。离开结果后不再从前端状态恢复。

用户名修改成功后提示 `reloginRequired`，如果修改的是当前账号则立即清理登录态。删除和角色降级操作沿用后端“至少保留一个管理员”的约束并展示稳定错误码。

删除用户、重置密码和批量创建前后均提供明确确认或结果汇总。用户删除只调用 Blog API 的统一删除接口，不再由前端串联训练数据清理和账号删除。

### 7.2 训练数据管理

训练数据管理保持高密度操作表格，并提供：

- 按 OJ、队员和回看小时数启动采集任务。
- 批量选择可自动采集的队员。
- 查看采集任务列表、任务状态和逐队员结果。
- 查看 handle 绑定、历史覆盖状态和最近采集时间。
- 手动刷新指定 OJ 数仓。

初版不提供 ODS 原始提交批量上传界面。后端接口继续保留，后续确有运维需求时再增加入口。

## 8. 精简队员目录接口

当前 Blog API 的 `/admin/users` 仅管理员可读，而多人统计需要队员可读的 username 列表。新增：

```text
GET /player/training-data/users
```

响应继续使用 Blog `Result`，`data` 为数组：

```json
[
  {
    "username": "player-a",
    "nickname": "队员 A",
    "ojNames": ["CODEFORCES", "ATCODER"]
  }
]
```

接口只返回 `needCollect=true` 且至少绑定一个 OJ 的训练账号。它不返回邮箱、角色、真实 handle、采集状态或其它管理字段。管理员同样可以读取该目录。

前端按当前 OJ 过滤目录，再以有限并发调用现有 `accepted-summary`。默认并发上限为 6：

- 单个队员失败不阻断整页。
- 失败行显示错误状态和单行重试入口。
- 页面显示已完成数量和总数量。
- 更改筛选器后取消旧队列和在途请求。

`accepted-summary` 继续返回用于表格展示的实际 handle，目录接口本身无需公开 handle。

## 9. 前端 API 适配

React API 层全面迁移到 Blog 单体合同：

- 删除旧 auth/training 双 base URL、双健康检查和 module-info。
- 删除 `studentIdentity`、`disable` 角色和旧 RSA/OAuth2 响应假设。
- 使用 `username`、`ROLE_admin` 和 `ROLE_player`。
- 批量创建直接发送 JSON 数组，不再使用 `{users: [...]}` 包装。
- handle 管理只调用 `/admin/users/{username}/oj-handles`。
- 用户删除只调用 `/admin/users/{username}`。
- 统一解包 `{code,errorCode,msg,data}`。

API 错误类型保留：

- HTTP 状态。
- Blog `errorCode`。
- 用户可读消息。
- 原始响应体，仅用于调试状态，不写入包含敏感内容的日志。

## 10. 状态、并发与安全

- 密码、JWT、Authorization 头和数据库信息不得进入前端日志、导出文件或错误提示。
- 一次性密码只存在于当前页面内存，不写入 `localStorage`。
- 所有受保护请求使用 `Authorization: Bearer <token>`。
- 多人统计使用带取消能力的请求队列，避免同时向后端发出所有队员请求。
- 采集任务通过后台 job API 启动和轮询，不保持长时间阻塞 HTTP 请求。
- 删除用户、批量采集、重置密码和数仓刷新必须二次确认。
- 前端不根据 JWT 中缓存的 authorities 自行放行；最终权限以 Blog API 的 `401/403` 为准。

## 11. 测试与验收

### 11.1 React 自动测试

更新并保留现有测试，覆盖：

- Blog `Result` 成功和错误解包。
- 登录、恢复登录、退出和登录后返回原地址。
- `ROLE_player` 与 `ROLE_admin` 路由矩阵。
- username 替换及无 `studentIdentity`/`disable` 残留。
- 多人统计有限并发、部分失败、单行重试和旧请求取消。
- 多人、单人、题目查询的筛选与分页。
- 用户管理和训练数据管理双菜单。
- 创建、批量创建、改名、密码重置、handle 修改和删除。
- 危险操作二次确认。

执行：

```bash
pnpm lint
pnpm test
pnpm typecheck
pnpm build
```

### 11.2 Vue Blog 自动和构建检查

覆盖或检查：

- 顶栏「训练中心」入口。
- 本地账号摘要展示和退出。
- `/login` 跳转 `/training/login`。
- API base URL 改为同源 `/api`。
- Blog 生产构建成功。

### 11.3 后端测试

精简目录接口覆盖：

- guest 返回 `401`。
- player/admin 返回 `200`。
- 只返回 `needCollect=true` 且有 handle 绑定的账号。
- 不返回邮箱、角色、真实 handle 或采集状态。

Java 变更后执行仓库要求的 Maven、测试策略和覆盖率检查。

### 11.4 本机桌面端验收

使用 Compose 启动 Blog API、MySQL、Redis 和前端 Nginx，以真实 HTTP 和桌面浏览器检查：

1. 游客浏览 Blog。
2. Blog 顶栏进入训练中心并登录。
3. player 查看多人、单人和题目查询。
4. player 不能访问管理页面。
5. admin 在同一管理页面切换用户管理和训练数据管理。
6. admin 创建用户、修改 handle、启动采集、查看任务并刷新数仓。
7. username 修改后旧登录失效并重新登录。
8. 刷新浏览器后保留当前训练路由。
9. 在 1440×900 和 1920×1080 下检查筛选器、表格和顶栏。
10. 浏览器控制台无未处理错误，运行日志不包含 JWT、密码或其它敏感信息。

## 12. 非目标

- 不重写或迁移 Blog CMS。
- 不把 Vue Blog 改写为 React。
- 不引入微前端框架。
- 不新增移动端适配。
- 不新增 ODS 原始提交上传 UI。
- 不改变 Blog API 已确定的角色、JWT 和用户删除语义。
- 不在本轮提交、推送或部署服务器。
