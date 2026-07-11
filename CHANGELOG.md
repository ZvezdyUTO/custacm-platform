# 更新日志

给人类看的项目更新记录。每次 MR 合并前，由 agent 按 [docs/agent/changelog.md](docs/agent/changelog.md) 的格式在最上方追加本次交付成果。

## 未发布

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
