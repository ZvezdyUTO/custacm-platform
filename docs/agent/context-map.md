# Context Map

| Path | Current responsibility |
| --- | --- |
| `platform-blog/upstream/nblog/blog-api/` | 唯一 Spring Boot 后端；Blog、文章/头像托管图片高清与缩略图及零垃圾回收、首页图片、认证、用户资料/个人友链、用户/OJ handle、按登录态筛选内部文章聚合结果、`top.naccl` 训练 HTTP adapter 与统一 Flyway runtime |
| `platform-blog/upstream/nblog/blog-view/` | Vue 3 + Vite 公开 Blog；生产路径 `/`，“我的主页”集中资料与本人文章，文章详情左侧显示当前文章作者公开名片，登录用户在列表/分类/标签/搜索/精选中可见内部文章，文章编辑支持 Markdown 导入、首图裁剪、正文图片选择/拖拽/粘贴，阅读页按需加载高清图 |
| `frontend/` | Vue 3 训练运行时；公开路径 `/training/**` 由 Blog 外壳承载，内部产物路径 `/training-app/**`；查询页与使用勃艮第专属主题的创建用户、管理用户、管理文章、管理分类、数据采集、首页图片六个管理员页面；同时拥有双前端 Nginx/Docker 构建 |
| `platform-training-data/training-data-common/` | OJ-neutral 用户目录 contract、无 MVC query facade、共享 handle 迁移、job、调度、warehouse 与 purge logic |
| `platform-training-data/training-data-codeforces/` | Codeforces source、ODS、OJ 专属迁移、warehouse SQL 与 adapter |
| `platform-training-data/training-data-atcoder/` | AtCoder source、metadata、ODS、warehouse SQL 与 adapter |
| `platform-common/common-core/` | 公共 SQL task 等后端基础能力 |
| `deploy/` | `blog-db` + `blog-redis` + `blog-api` + `frontend` 的本地/单机 Compose 配置 |
| `scripts/` | `dev.sh` 本机双 Vite/HMR 开发入口、`deploy.sh` 四服务稳定构建入口，以及测试策略、文档同步和日志 MCP 安装等辅助工具 |
| `docs/` | 架构、API、鉴权、日志、部署和 agent 上下文文档 |

运行拓扑：Vue 3 Blog `/` 与其训练外壳 `/training/**` → 同源 Vue 3 Training `/training-app/**` → Nginx `/api/**` → Blog API。Blog `Nav.vue` 在页面切换时保持挂载；Blog API 在进程内组装训练模块并访问统一 MySQL。业务身份统一为 `username`，角色只允许 `ROLE_admin` 与 `ROLE_player`；`root` 是不可删除且不参与 OJ/队员状态的固定系统管理员，空头像统一回退到 Blog 内置默认图。
