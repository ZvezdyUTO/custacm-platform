# Agent Context

- 唯一可运行后端是 `platform-blog/upstream/nblog/blog-api`，使用 Spring Boot 3.5.16 与 Java 21。
- `username` 同时是 JWT subject 与训练业务身份；角色严格为 `ROLE_admin`、`ROLE_player`，guest 表示未认证。
- Blog API 负责 BCrypt 密码、HS512 JWT、账号、角色、OJ handle、Blog HTTP 与训练 HTTP adapter。
- 训练模块只保留采集、ODS/DWD/DWM/DWS、查询、调度、刷新和清理能力；没有独立 Web runtime。
- Vue 3 + Vite Blog 位于 `/` 并持有保留原顶栏的训练外壳 `/training/**`，独立 Vue 3 训练运行时位于内部 `/training-app/**`；它们由一个 Nginx 前端服务托管，浏览器 API 统一使用 `/api/**`。
- 训练中心管理区分为“创建用户”“管理用户”“管理文章”“管理分类”“数据采集”“首页图片”六个独立页面，并使用勃艮第酒红、陶土当前页标记和暖雾灰背景；分类页维护 Blog 顶栏和文章编辑器使用的分类；首页图片支持多选、16:9 裁剪和有序管理，所有手动采集完成后固定刷新数仓。正式验收为 1280–2560 px 桌面端，重点 1440×900 与 1920×1080，移动端不在当前范围。
- Vue Blog 构建内置唯一默认首页图 `public/img/homepage-banner-default.png`，Flyway 初始化/升级和接口失败回退都使用它。
- 两套 Vue 3 应用共享 `custacm.accessToken`、`custacm.user`；公开 Blog 请求不全局带 JWT，受保护评论提交显式使用共享 Bearer。
- Vue Blog 的“我的主页”内嵌本人文章；登录用户从顶栏发布纯文本/Markdown 导入文章，并从本人文章详情进入编辑。
- 文章首图和正文图片由 Blog API 托管并生成高清/缩略图；正文最大 15MB，阅读默认缩略图，删除文章/图片或更换头像后立即回收文件。
- `GET /player/training-data/users` 只返回可采集用户的 `username`、`nickname`、`ojNames`，不返回真实 handle 或管理员字段。
- 多人和单人查询筛选参数变更后自动刷新，没有独立查询按钮；连续输入短防抖，无效范围不发请求。题目查询保留深色显式查询按钮。
- 单人查询默认保持“请选择队员”，用户主动选择后才加载个人训练数据。
- 根 reactor 包含 `platform-common`、`platform-training-data` 和 Blog API。API 见 [../api.md](../api.md)，授权见 [../authorization.md](../authorization.md)。
- Java MR 门禁只要求 `mvn clean test` 运行已有单测；历史代码不强制追补覆盖率，新增或实质修改的业务逻辑应同步增加针对性单测。详见 [quality-gates.md](quality-gates.md)。
- 本地/单机 Compose 包含 `blog-db`、`blog-redis`、`blog-api`、`frontend` 四个服务；没有证据时不要声称已发布到服务器。

修改代码前先读最近的 `AGENTS.md`、[../logging.md](../logging.md) 和 [doc-sync.md](doc-sync.md)。涉及文件/模块职责变化时同步对应 README、[context-map.md](context-map.md) 和 `docs/doc-sync-map.tsv` 指定文档。
