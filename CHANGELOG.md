# 更新日志

给人类看的项目更新记录。每次 MR 合并前，由 agent 按 [docs/agent/changelog.md](docs/agent/changelog.md) 的格式在最上方追加本次交付成果。

## 未发布

### 2026-07-14 - 修复 Markdown 编辑器多图光标定位

- 成果：正文图片预览改为 CodeMirror 原子块，图片异步加载后主动复测布局；方向键经过图片边界时不再落入隐藏的 Markdown 源码，明确点击图片仍可进入源码编辑。
- 影响：移除图片、代码块和块公式预览不参与编辑器高度模型的垂直外边距，避免多张图片后鼠标定位误差累积，并保持上下键按视觉行移动而非跨自然段跳转。
- 验证：已运行 Blog `npm ci`、`npm test`（103 项）、`npm run build`、`./scripts/check-doc-sync.sh origin/main WORKTREE` 和 `git diff --check`。

### 2026-07-14 - 调整自动采集回看窗口

- 成果：Codeforces 与 AtCoder 自动采集继续按各用户、各 OJ 的 `lastCollectedAt` 计算窗口；每日任务默认回看 100 小时，日内半小时任务默认使用零回看从上次成功游标直接续爬，首次无游标仍抓取全部历史。
- 影响：新增 `BLOG_DAILY_COLLECTION_LOOKBACK` 与 `BLOG_INTRADAY_COLLECTION_LOOKBACK` 部署变量；旧 `deploy/.env` 不含这两个变量时，Compose 会注入 `100h` 与 `0h`，因此重建 `blog-api` 后旧的 120 小时/1 小时镜像内默认值会被替换。自动采集开关仍默认关闭，手动采集仍要求正数回看小时数。
- 验证：已运行 `mvn clean test`、`mvn clean package -DskipTests`，使用不含新增变量的环境展开 Compose 并确认得到 `100h`/`0h`，以及 `git diff --check` 和文档同步检查。

### 2026-07-13 - 增加文章归档、全量备份与七天回收站

- 成果：文章详情为登录用户提供包含标题、简介、原始正文与托管图片的 ZIP 归档；普通用户跨全部文章共享 30 秒限流窗口，管理员不限频。管理员文章页增加全量备份按钮，导出所有文章状态、去敏评论、作者资料、文章图片和作者头像。作者端和管理员端文章管理同时增加当前文章/回收站切换、删除时间、剩余保留期与恢复操作。
- 影响：单篇归档会把托管图片链接改为包内相对路径，图片以 `cover-*`、`content-{n}-*` 扁平命名，不再嵌套资产 ID，归档与全量备份均不附带额外 README；作者头像也去除多余资产目录。下载文件名限制 UTF-8 长度，避免超长标题触发 Nginx 502；文章详情的作者、日期、阅读信息与下载/编辑动作统一图标字号、文本行高和垂直基线。全量备份不包含密码、token 或评论 IP 等敏感字段，也不抓取外站图片。`V036` 为文章增加 `deleted_at` 和回收站索引；作者或管理员删除都只进入固定七天回收站，期间正文、评论、标签和图片完整保留且仅作者本人或管理员可恢复。到期任务加锁并在单事务中物理清理，所有正常读取、编辑、评论、精选和下载均排除回收站文章。
- 验证：已运行 `mvn clean test`（Blog API 138 项，跳过 2 项，Java 共 347 项）和 `mvn clean package -DskipTests`；Blog 82 项测试/生产构建、训练前端 lint/81 项测试/类型检查/生产构建、文档同步检查和 `git diff --check` 均通过。

### 2026-07-13 - 按现用界面收敛 Blog 单体架构

- 成果：按 Blog 与训练中心的实际路由和请求裁掉关于、友链、动态、后台统计与日志、Quartz、邮件/Telegram、旧上传通道及游客评论身份链路；部署仍是一个 Nginx `frontend` 服务，分别承载 `/` 与 `/training/**` 两个 Vue 3 构建产物。
- 成果：用户资料、角色、密码与 OJ handle 改为单事务保存；多人训练统计改为批量查询，文章标签和评论查询消除 N+1，文章删除改为单事务，Redis 增加 TTL、故障降级和提交后失效，图片写入增加失败回滚与孤儿清理。
- 影响：V034 将现行 OJ 绑定规范化为 `training_member` 与 `oj_handle_binding`，旧表暂留一个迁移窗口；V035 删除退役页面与后台能力的独占表。六类自动采集默认关闭，升级后需按需显式启用；已删除页面和接口不再兼容。
- 验证：已运行 `mvn clean test`、`mvn clean verify`、`mvn clean package -DskipTests`、训练前端 `pnpm test`/`pnpm typecheck`/`pnpm lint`/`pnpm build`、Blog `npm test`/`npm run build`、Compose 配置、测试策略、文档同步和 `git diff --check`。

### 2026-07-13 - 精简首页数据并改善训练与移动端体验

- 成果：首页标签云改为随机展示最多 30 个标签，欢迎文字使用 80% 不透明度，移除顶栏“动态”入口并在移动端保留缩小后的头图；个人名片新增邮箱展示。
- 成果：多人训练统计固定队员信息列并允许训练数据横向滚动；修复移动端训练登录卡片被桌面最小宽度挤出视口的问题。
- 影响：公开 `/site` 响应和查询移除未展示的旧徽章、版权、收藏、最新文章等字段与实现，减少首页无效数据库读取；公开个人资料响应新增 `email`。
- 验证：已运行 `mvn clean test`、训练前端 lint/71 项测试/类型检查/生产构建、Blog 66 项测试/生产构建、后端 Docker 镜像重建与健康检查，并在 390×844 浏览器视口验证移动端登录页显示和输入交互。

### 2026-07-12 - 完善 Blog 阅读体验与可选 HTTPS 部署

- 成果：Blog 文章列表和详情页展示首图，修复多行公式、长代码横向滚动、列表内代码块样式污染与窄视口布局问题，并为作者友情链接自动加载站点图标；根 README 同步介绍博客平台的队内沉淀与访客交流定位；训练多人统计按通过数降序展示并区分 AtCoder 难度色阶。
- 影响：Blog API 的公开文章摘要与详情响应新增 `firstPicture`；Nginx 前端支持通过 `TLS_ENABLED` 和挂载证书启用 HTTPS，HTTP 模式仍为默认行为；部署脚本会按实际协议检查网关。
- 验证：已运行 `mvn clean verify`、`mvn clean package -DskipTests`、`./scripts/check-test-policy.sh`、训练前端 lint/69 项测试/类型检查/生产构建、Blog 63 项测试/生产构建、前端 Docker 镜像构建、Compose 配置检查、部署脚本语法检查、文档同步检查和 `git diff --check`。

### 2026-07-12 - 改善 Blog 头像与内部文章浏览

- 成果：个人名片头像优先展示 512×512 原图，文章作者简介卡继续使用缩略图；修复头像裁剪保存后弹窗未自动关闭的问题，并加宽顶栏账号名称显示区域。
- 影响：登录用户现在可在首页、分类、标签、搜索和精选中发现内部文章，游客仍只会获得公开文章；内部文章正文和评论继续要求登录访问。
- 验证：已运行 `mvn clean test`、`mvn package -DskipTests`、Blog `npm test`、Blog `npm run build`、`./scripts/check-doc-sync.sh origin/main WORKTREE` 和 `git diff --check`。

### 2026-07-12 - 清理废弃模块与历史实现

- 成果：移除未参与构建部署的旧管理端、空 Maven/文档占位模块、训练模块旧独立 HTTP Controller、前端旧登录页和未引用资源，并清理已删除认证与训练 Web 服务遗留的本地构建产物。
- 影响：仓库只保留 Blog API、Vue Blog、Vue 训练中心和实际训练数据模块；本地上传目录继续保留并加入 Git 忽略，现行运行拓扑和 API 不变。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、训练前端 lint/测试/类型检查/生产构建、Blog 测试/生产构建、Compose 配置检查、文档同步检查和 `git diff --check`。

### 2026-07-12 - 移除 Blog 播放器与歌词组件

- 成果：从 Vue Blog 移除 APlayer/Meting 播放器挂载、歌词样式、CDN 脚本与样式、Meting 自定义元素配置和本地运行脚本。
- 影响：Blog 与训练中心不再显示播放器或歌词，也不再向播放器 CDN 和 Meting API 发起浏览器请求；文章目录等其它前端功能保持不变。
- 验证：已运行 Blog 单元测试与生产构建，并在统一 4180 前端检查页面 DOM、外部资源请求和控制台。

### 2026-07-12 - 修复管理员批量创建与现役状态保存

- 成果：修正 Spring 将批量创建错误注册为 `/admin/users/:batch-create` 的路由组合问题，使正式 `/admin/users:batch-create` 合同可用；创建页在请求前按行提示 6～128 位密码限制；管理页把账号、OJ handle 和现役/退役状态合并为一次“保存修改”。
- 影响：批量创建不再返回无意义的“异常错误”；取消“现役队员”后会真正提交 `needCollect=false`，只有账号和 OJ 状态都保存成功才显示成功。运行环境同时清理为唯一一套 4180 前端、8090 Blog API、MySQL 和 Redis。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、训练前端 58 个测试、lint、类型检查和生产构建；使用临时 `jiangly` 双 OJ 账号真实验证创建、修改、取消现役、查询回读和删除，并在 1440×900 浏览器验证页面状态与错误提示。

### 2026-07-12 - 训练中心迁移到 Vue 3

- 成果：将 `/training/**` 训练中心从 React 重写为 Vue 3 + Vue Router，保留多人、单人和题目查询；训练入口收进 Blog 顶栏的“训练中心”下拉菜单，进入训练中心后品牌、主菜单、搜索和账号区保持不变，仅高亮训练中心；独立“关于我/退出”入口合并到账号菜单，管理员可从账号菜单进入管理界面。管理员区进一步拆成创建用户、管理用户、数据采集三个独立页面：创建页支持文本导入后生成可编辑信息行，数据采集页支持全员和单人操作。
- 影响：两套前端现在均使用 Vue 3 且继续独立构建、独立路由；公开 `/training/**` 由 Blog 外壳持有，训练运行时通过内部 `/training-app/**` 同源嵌入，因此切换时原 Blog 顶栏不会卸载。两端继续通过 `custacm.accessToken`、`custacm.user` 保持登录连续性；所有管理员手动采集现在固定请求完成后刷新数仓，不再展示可选刷新开关，API、角色与其余训练数据合同不变。
- 验证：已运行训练中心 `pnpm lint`、`pnpm test`、`pnpm typecheck`、`pnpm build`，Blog `npm test -- --run`、`npm run build`，并使用统一 Nginx 镜像检查公开与内部 history fallback；浏览器交互与桌面视口检查见本次交付记录。

### 2026-07-11 - 统一 Blog 与训练双前端入口

- 成果：将 Vue Blog `/` 与 React 训练中心 `/training/**` 收敛到同一个 Nginx 服务，浏览器统一通过 `/api/**` 访问唯一 Blog API；两端共享登录摘要，公开 Blog 请求不全局携带 JWT，受保护评论提交显式使用 Bearer。
- 影响：训练中心统一采用 `username` 和 `ROLE_admin`/`ROLE_player`，管理员界面只保留“用户管理”“训练数据管理”；Compose 现在由 Blog API、MySQL、Redis、frontend 四个服务组成，并新增 `FRONTEND_PORT`。本次只完善本地/单机配置，没有执行或声称服务器发布。
- 验证：已运行 `./scripts/check-doc-sync.sh origin/main WORKTREE` 和 `git diff --check`。

### 2026-07-11 - Blog API 与训练系统整合为完整单体

- 成果：以 Spring Boot 3.5.16 Blog API 作为唯一后端，统一 BCrypt 账号、HS512 JWT、`ROLE_admin`/`ROLE_player`、`username` 身份、OJ handle 与 Codeforces/AtCoder 训练数据能力；移除旧 auth 和 training-data-web 运行模块。
- 影响：训练查询改到 `/player/training-data/**`，管理操作改到 `/admin/training-data/**`；用户改名会级联 handle 并让旧 Token 失效，删除用户会清理可再生训练数据，同时保留文章评论并显示“已注销用户”。部署栈收敛为 Blog API、一个 MySQL 和 Redis，使用新的独立数据卷。
- 验证：已运行 Blog/API 聚焦测试、`docker compose ... config`，并在本机 MySQL 8.4/Redis 7 上完成 V001～V024、权限矩阵、ODS/数仓刷新、改名、最后管理员保护及内容保留删除的真实 HTTP 验收；最终全量质量门禁见本次工作交付记录。

### 2026-07-09 - 收敛首页 README

- 成果：将根 README 收敛为面向普通访客的项目介绍，并把维护资料入口折叠起来；文档索引同步说明首页只承载项目简介。
- 影响：首页不再展开快速启动、部署、API 和 agent 文档入口，技术资料仍保留在 `docs/README.md` 及各模块文档中。
- 验证：已运行 `./scripts/check-doc-sync.sh origin/main WORKTREE` 和 `git diff --check`。

### 2026-07-09 - 修复 remember-me 部署与文档同步

- 成果：让 Compose 把 `AUTH_JWT_REMEMBER_ME_ACCESS_TOKEN_TTL` 传入 `auth-web`，并补齐部署说明、auth-web 测试说明、根 README 文档入口和过期 AtCoder 实施计划标记。
- 影响：部署者可以通过 `deploy/.env` 调整“记住我”token 有效期；后续 agent 不会再把旧的 AtCoder 全量 `UNRATED` 假设当成当前实现。
- 验证：已运行 `./scripts/check-doc-sync.sh origin/main WORKTREE`、`git diff --check` 和 `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config`。

### 2026-07-08 - 现役退役文案与颜色区分

- 成果：将用户列表中的采集开关展示改为“现役队员 / 已退役”，并为两种状态使用不同颜色区分；采集页说明同步改为只列出现役队员；`root` 不展示现役/退役标记，也不提供 OJ 绑定入口；用户列表的 OJ handle 改为独立边框标签，不再用斜杠拼接。
- 影响：仅调整前端展示和文案，底层 `needCollect` 接口字段和采集过滤逻辑不变。
- 验证：已运行 `pnpm exec vitest run --environment jsdom src/test/admin-user-management-panel.test.tsx src/test/training-data-ops-panel.test.tsx src/test/use-platform-dashboard.test.tsx`、`pnpm lint`、`pnpm test`、`pnpm typecheck`、`pnpm build` 和 `git diff --check`。

### 2026-07-08 - 管理员用户页文案调整

- 成果：将前端管理员侧栏和用户管理页的旧用户页文案统一调整为“管理用户”，并同步测试和前端 README。
- 影响：仅调整 UI 文案和可访问标签，用户管理功能、路由和接口行为不变。
- 验证：已运行 `pnpm exec vitest run --environment jsdom src/test/app-navigation.test.tsx src/test/admin-user-management-panel.test.tsx src/test/use-platform-dashboard.test.tsx`、`pnpm lint`、`pnpm test`、`pnpm build` 和 `git diff --check`。

### 2026-07-08 - AtCoder 难度模型采集与分段桶

- 成果：新增 Kenkoooo `problem-models.json` 采集和 `ods_atcoder__problem_model` 落地，AtCoder DWD 使用 clipped difficulty 写入独立分段桶，并把公共查询侧改为按 OJ 独立 difficulty bucket 聚合；AtCoder 题目元数据刷新改为默认每三天一次；题目查询页优先展示后端返回的题目标题而不是题目代号。
- 影响：AtCoder DWS 不再全部落到 `UNRATED`，有 problem model 且属于非 experimental ABC/ARC/AGC 的题目会进入向下取整后的下界桶 `0`、`400`、`800`、`1200`、`1600`、`2000`、`2400` 或 `2800+`；缺失 model、experimental model 或其它 contest family 的题目仍保留 `UNRATED`。
- 验证：已运行 `mvn -pl :training-data-common,:training-data-atcoder -am test`、`mvn -pl :training-data-web -am test`、`mvn clean verify`、`./scripts/check-test-policy.sh`、`pnpm exec vitest run --environment jsdom src/test/training-query-panel.test.tsx src/test/platform-api.test.ts`、`pnpm lint`、`pnpm test`、`pnpm typecheck`、`pnpm build` 和 `git diff --check`。

### 2026-07-08 - 合并 OJ 数仓刷新公共逻辑

- 成果：将 Codeforces/AtCoder 重复的数仓刷新 interval、SQL task refresh service 和 collection-job refresh handler 收敛到 `training-data-common`，各 OJ 仅保留自己的 interval SQL、manifest 和清洗 SQL 资源。
- 影响：刷新入口和 HTTP 行为不变；新增 OJ 后复用 common refresh service/handler，只需实现 OJ-specific interval repository 并配置 manifest。
- 验证：已运行 `mvn -pl platform-training-data/training-data-common -am test`、`mvn -pl platform-training-data/training-data-codeforces -am test`、`mvn -pl platform-training-data/training-data-atcoder -am test`、`mvn clean verify` 和 `./scripts/check-test-policy.sh`；`training-data-common` 未带 `-am` 的单模块命令会因本地 reactor 依赖未参与解析失败。

### 2026-07-08 - OJ 采集 adapter 失败处理去重

- 成果：新增通用 OJ submission collection adapter 基类，统一 handle 校验、失败 outcome、稳定错误码日志、handle 哈希和 collector batch id 前缀，Codeforces 与 AtCoder adapter 只保留各自分页、过滤和 ODS 写入逻辑。
- 影响：仅内部维护；两个 OJ 的采集结果、批次前缀和源端错误码保持兼容，后续新增 OJ adapter 可以复用同一失败处理框架。
- 验证：已运行 `mvn -pl :training-data-common,:training-data-codeforces,:training-data-atcoder -am test`、`mvn clean verify`、`./scripts/check-test-policy.sh` 和 `./scripts/check-doc-sync.sh origin/main WORKTREE`。

### 2026-07-08 - Codeforces 团队提交按采集 handle 计数

- 成果：Codeforces 采集写入按每个 handle 的采集结果归因，团队提交会归因给本次采集的目标 handle，并新增迁移把 ODS/DWD 唯一键调整为 `submission + handle`。
- 影响：后续 DWM/DWS 统计会把团队提交计入被采集的学生；重新采集历史数据后，已有团队提交会补齐“目标 handle 不是第一成员”的 ODS/DWD/DWM/DWS 数据。
- 验证：已运行 `mvn -pl platform-training-data/training-data-codeforces -am test`、`mvn -pl platform-training-data/training-data-web -am test`、`mvn clean verify` 和 `./scripts/check-test-policy.sh`。

### 2026-07-08 - 训练数据多 OJ 数仓重构

- 成果：将训练数据模块扩展为 Codeforces/AtCoder 多 OJ 数仓结构，新增 OJ handle map、AtCoder Kenkoooo 采集、AtCoder 题目列表刷新、通用 OJ 查询/采集/清理和前端 OJ 切换能力。
- 影响：本次升级会把 Codeforces DWD/DWM/DWS 迁移到新的公共同层表契约，并以破坏性方式删除重建旧仓库表；部署后查询层数据需要从 ODS/后续采集刷新重新生成，在重建完成前公开训练数据查询可能为空。Codeforces ODS、OJ handle 绑定和 auth 账号不由该迁移删除。该重建是本次 `V017` 升级的一次性影响，当前其它迁移不要求清空或重建数仓；后续升级默认保留数仓数据，除非迁移脚本、模块文档和 changelog 再次明确声明破坏性重建。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、`pnpm lint`、`pnpm test`、`pnpm typecheck` 和 `pnpm build`。

### 2026-07-06 - 训练数据前后端工作台与部署联调

- 成果：补齐 Codeforces 训练数据的自动采集标记、后台采集任务、分页查询、用户数据清理和公开用户列表等后端支撑，并新增 React/Vite 前端工作台用于训练查询、用户管理、数据采集、数据维护和操作记录。
- 影响：本地与单机部署现在可以同时启动 auth、training-data 和前端 Nginx；前端通过同源代理访问真实 API，页面路径保留查询/管理员页签状态，管理员可在 UI 中批量创建用户、绑定 Codeforces handle、触发采集任务和执行高风险数据清理。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、`pnpm lint`、`pnpm test`、`pnpm typecheck`、`pnpm build`、`mvn clean package -DskipTests`、`docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config`、`./scripts/check-doc-sync.sh origin/main WORKTREE` 和 `git diff --check`。

### 2026-07-05 - Codeforces 最近提交采集与数仓刷新

- 成果：新增 Codeforces `studentIdentity` 绑定采集链路、可配置最近窗口采集器、DWD/DWM/DWS SQL task DAG 刷新入口、禁用默认定时任务和对应公开查询能力。
- 影响：`training-data-web` 可以通过 admin API 从真实 Codeforces `user.status` 采集最近提交并写入 ODS，再按 batch 刷新 Codeforces 数仓；游客查询继续按 `studentIdentity` 或 `problemKey` 读取清洗后的 DWD/DWM/DWS 数据。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`，并在 Docker E2E 容器中采集 `tourist` 与 `jiangly` 最近 1488 小时数据、刷新数仓、验证公开查询和负向鉴权/refresh 错误路径。

### 2026-07-05 - 平台自有账号鉴权

- 成果：将鉴权模块从 Keycloak 适配改为平台自有账号、BCrypt 密码哈希、RSA JWT 签发、玩家自助和管理员用户管理接口。
- 影响：后端接口按 `/admin/**`、`/player/**` 和游客公开路径分层；登录失败后同一 `studentIdentity` 有 5 秒重试冷却，部署需要配置 auth MySQL 与 RSA JWT 密钥文件。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、`./scripts/check-doc-sync.sh origin/main WORKTREE`、`docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config` 和 `git diff --check`。

### 2026-07-05 - 调整 PR 审核确认规则

- 成果：明确非项目负责人发起的 PR/MR 必须经过负责人确认后合并，项目负责人本人发起的 PR/MR 在明确要求合并时无需额外审核确认。
- 影响：agent 操作规则、贡献指南、PR 模板和文档同步说明保持一致，后续合并判断可以按发起人区分是否需要额外确认。
- 验证：已运行 `./scripts/check-doc-sync.sh origin/main WORKTREE` 和 `git diff --check`。

### 2026-07-05 - Codeforces 数仓读侧与 UTC+8 汇总

- 成果：重整 `training-data-codeforces` 分层包结构，新增 DWD 提交、DWM 首次 AC、DWS 每日 rating 汇总的内部查询服务、仓储实现、查询条件和覆盖测试。
- 影响：Codeforces 数仓时间字段明确为 UTC+8 语义，DWS 每日汇总调整为固定 rating 桶宽表；后续训练数据读侧能力可以复用这些 app/domain/infra 边界继续暴露 HTTP API。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、`./scripts/check-doc-sync.sh origin/main WORKTREE` 和 `git diff --check`。

### 2026-06-27 - 训练数据多 OJ ODS 建模

- 成果：新增 `platform-training-data` Maven 模块，落地 Codeforces 垂直 OJ 数仓模块、独立 submission ODS 表、HTTP 写入入口、record/parser/writer/DDL/upsert/fixture/tests、1000 条本地真实 Codeforces API 样本和 `training-data-web` 文件日志接入。
- 影响：训练数据模块不再只是占位；当前只保留 OJ 独立 ODS 建模与批量写入，不包含 DAG / pipeline / task run / scheduler。外置采集器可以批量提交原始 submission 数组，DWD/DWS/ADS 等下游层后续按真实查询需求再建模。
- 验证：已运行 `mvn clean verify`、`./scripts/check-test-policy.sh`、`./scripts/check-doc-sync.sh origin/main WORKTREE` 和 `git diff --check`。

### 2026-06-25 - 项目待办和更新日志

- 成果：新增根目录待办列表、面向人类的更新日志，以及 agent 写更新日志的固定格式。
- 影响：后续 MR 需要按“成果 / 影响 / 验证”记录交付结果，未来 agent 也能从仓库文档中读到规则。
- 验证：已运行 `./scripts/check-doc-sync.sh origin/main WORKTREE`、`./scripts/check-test-policy.sh`、`mvn clean verify` 和 `git diff --check`。
