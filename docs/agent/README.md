# Agent Context

- 唯一可运行后端是 `platform-blog/upstream/nblog/blog-api`，使用 Spring Boot 3.5.16 与 Java 21。
- `username` 同时是 JWT subject 与训练业务身份；角色严格为 `ROLE_admin`、`ROLE_player`，guest 表示未认证。
- Blog API 负责 BCrypt 密码、HS512 JWT、账号、角色、OJ handle、Blog HTTP 与训练 HTTP adapter。
- 训练模块只保留采集、ODS/DWD/DWM/DWS、查询、调度、刷新和清理能力；没有独立 Web runtime。提交采集按用户和 OJ 保存最近成功窗口结束时间；自动采集和 AtCoder 题目补采默认关闭，必须由部署变量显式启用。自动提交采集启用后，每日默认回看 100 小时，日内默认以零回看从上次游标续爬。
- 对外只有一个 Nginx `frontend` 服务；其中构建 Vue 3 Blog `/`（并持有保留原顶栏的 `/training/**` 外壳）和内部 Vue 3 Training `/training-app/**` 两份静态产物。浏览器 API 统一使用 `/api/**`。
- 训练中心管理区分为“创建用户”“管理用户”“管理文章”“管理分类”“数据采集”“首页图片”六个独立页面，并使用勃艮第酒红、陶土当前页标记和暖雾灰背景；管理文章页包含当前文章/回收站切换和管理员恢复；分类页维护 Blog 顶栏和文章编辑器使用的分类；首页图片支持多选、16:9 裁剪和有序管理，所有手动采集完成后固定刷新数仓。正式验收为 1280–2560 px 桌面端，重点 1440×900 与 1920×1080，移动端不在当前范围。
- Vue Blog 构建内置唯一默认首页图 `public/img/homepage-banner-default.png`，Flyway 初始化/升级和接口失败回退都使用它。
- 两份 Vue 3 构建共享 `custacm.accessToken`、`custacm.user`；公开 Blog 请求不全局带 JWT，受保护评论提交显式使用共享 Bearer。
- 匿名登录按规范化 username 受 Redis 五秒冷却保护：首次错误 401 携带 `Retry-After: 5`，窗口内重复请求 429，Training 登录按钮同步显示剩余秒数；Redis 不可用时登录失败关闭。
- 两份 Vue 3 构建共享 `custacm.theme=light|dark`；无值时跟随系统，Blog 唯一顶栏以太阳/月亮拨杆负责正式切换，Training frame 同源同步。深夜模式是暖炭黑、深咖黑、暖灰白与低饱和橙，业务图片只做带 reduced-motion 兜底的轻度渐暗，不反色或混淆状态语义。
- 两份前端的共享视觉 token 以 `frontend-design-tokens/tokens.css` 为唯一源，通过 `scripts/sync-design-tokens.sh` 生成本地副本；Training 与 Blog 均已通过各自独立覆盖层接入，保留两套 Router 和组件边界。
- Vue Blog 只保留首页、文章、分类、标签、搜索、`/profile` 个人主页与评论；评论用本地 Google Noto Emoji sprite 选择并渲染标准 Unicode，历史短码只读兼容。About、全站友链和动态页面/API 已移除。“我的主页”内嵌本人文章，登录用户从顶栏发布纯文本/Markdown 导入文章，并从本人文章详情进入编辑。
- Vue Blog 首页只展示精选区，不渲染普通文章列表、个人介绍或标签云；首页使用暖米色画布和统一的低饱和浅奶油棕卡片，黑夜开关会同时切换页面背景与统一暖炭灰卡片。文章详情页仍在左栏展示作者信息，右栏保留目录。
- 登录用户可在文章详情下载已发布文章的 Markdown 与托管图片 ZIP；普通用户跨全部文章共享 30 秒 Redis 窗口，管理员不限频，限流状态故障时普通用户返回 503。管理员文章页可下载包含所有文章状态、去敏评论、作者资料和托管图片的全量 ZIP。
- 文章删除无论由作者还是管理员发起都只进入固定七天回收站；期间作者本人或管理员可恢复，所有正常读取排除回收站文章，到期任务才物理删除评论、标签关联、文章和托管图片。
- 文章首图和正文图片由 Blog API 托管并生成高清/缩略图；正文最大 15MB，阅读默认缩略图；回收站保留期内图片不回收，主动移除图片或更换头像仍在事务提交后回收。
- `GET /player/training-data/users` 默认只返回可采集用户的 `username`、`nickname`、`ojNames`；`includeRetired=true` 会保留已退役用户，但仍不返回真实 handle 或管理员字段。
- 多人统计通过一次 `GET /player/training-data/accepted-summaries` 批量查询；管理员通过单个 `PUT /admin/users/{username}` 原子更新账号、完整 OJ handle 集合和现役状态，改变/移除 handle 会在同一事务清理旧训练数据。
- V034 后生产代码只读写 `training_member`、`oj_handle_binding`；旧 `oj_handle_account` 保留一个迁移窗口。V035 删除已退役页面、Quartz、访问统计和旧应用日志独占表；V036 增加文章回收站时间与索引。
- 多人和单人查询筛选参数变更后自动刷新，没有独立查询按钮；连续输入短防抖，无效范围不发请求。题目查询保留深色显式查询按钮。
- 单人查询默认保持“请选择队员”，下拉以 `username · nickname` 降序展示，用户主动选择后才加载个人训练数据；管理员用户页按 username 子串查询已加载账号。
- 根 reactor 包含 `platform-common`、`platform-training-data` 和 Blog API。API 见 [../api.md](../api.md)，授权见 [../authorization.md](../authorization.md)。
- Java MR 门禁只要求 `mvn clean test` 运行已有单测；历史代码不强制追补覆盖率，新增或实质修改的业务逻辑应同步增加针对性单测。详见 [quality-gates.md](quality-gates.md)。
- 本地/单机 Compose 包含 `blog-db`、`blog-redis`、`blog-api`、`frontend` 四个服务；前端默认 HTTP，设置 `TLS_ENABLED=true` 后从受限宿主机目录只读挂载证书并启用 HTTPS；没有证据时不要声称已发布到服务器。

修改代码前先读最近的 `AGENTS.md`、[../logging.md](../logging.md) 和 [doc-sync.md](doc-sync.md)。涉及文件/模块职责变化时同步对应 README、[context-map.md](context-map.md) 和 `docs/doc-sync-map.tsv` 指定文档。
