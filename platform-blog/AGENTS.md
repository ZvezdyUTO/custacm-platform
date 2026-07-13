# platform-blog Agent Notes

- `upstream/nblog/blog-api` 是根 Maven reactor 中唯一可运行后端，负责 Blog、首页图片存储、BCrypt 账号、HS512 JWT、`username`、`ROLE_admin`/`ROLE_player`、OJ handle 和训练数据 HTTP adapter。
- Player 文章写操作必须按当前用户校验 `blog.user_id`；管理员可管理全部文章，但新建文章的作者必须绑定当前认证管理员，不得硬编码 user id 或信任请求体作者。
- `upstream/nblog/blog-view` 是生产环境 `/` 下的 Vue 3 + Vite Blog，并持有 `/training/**` 外层路由和唯一 `Nav.vue`；独立训练运行时挂载在内部 `/training-app/**`。两者由 `frontend/Dockerfile` 构建并交给同一个 Nginx 服务。
- Blog 顶栏“训练中心”是点击开关型下拉菜单：标题点击只展开/收起，不直接跳转，悬停不得展开；选择具体查询项后才导航。
- 浏览器 API 统一从 `/api/**` 进入 Nginx，Blog API 的直接路径不带 `/api`。不要在 Vue Axios 全局拦截器附加共享 JWT。
- Vue Blog 可读取 `custacm.accessToken` 与 `custacm.user` 展示账号摘要；普通页面左侧显示登录用户名片，文章详情左侧通过匿名公开资料接口显示文章作者名片。名片纯展示头像、nickname、username、签名和个人友情链接，修改入口统一位于右侧个人资料面板。文章列表、分类、标签、搜索和精选读取在存在会话时显式发送 Bearer，使登录用户看到内部文章；受保护评论提交、内部文章读取，以及本人头像、nickname、个性签名和个人友情链接更新也显式发送 Bearer JWT。个人友情链接仅允许 HTTP(S) 绝对地址且每人最多 8 条；公开 Blog 请求不得全局附加 JWT。
- Vue Blog 的“我的主页”同页展示资料和本人文章；顶栏“发布文章”与作者详情页“编辑文章”只调用 `/player/blog**`，Markdown 导入仅在浏览器读取文本，不上传原文件。
- 顶栏文章搜索只按标题匹配，候选摘要展示文章简介。文章不维护或展示阅读数；不得重新引入详情读取计数、Blog 阅读数 DTO 字段、Redis 计数缓存或定时同步任务。
- 文章首图和正文图片通过 `/player/images` 上传到托管资产目录；正文图片最大 15MB，默认插入缩略图并在预览中按需加载归一化高清图。托管图片不可跨文章复用，删除文章、移除图片或更换头像后必须立即清理失效文件，并由定时任务兜底清理临时/孤儿目录。
- Vue Blog 唯一默认首页图位于 `blog-view/public/img/homepage-banner-default.png`；Flyway 初始化数据与接口失败回退必须指向同一构建内置资源。
- 用户头像字段为空时统一展示 `blog-view/public/img/default-avatar.jpg`；固定系统管理员 `root` 不得删除、改名、降权、绑定 OJ handle 或设置队员采集状态。
- 训练采集、ODS/DWD/DWM/DWS 和数仓逻辑继续留在 `platform-training-data`；Blog API 负责身份、授权、用户/OJ handle 管理及向外暴露 HTTP。
- `GET /player/training-data/users` 默认排除 `needCollect=false` 的已退役用户；仅当 `includeRetired=true` 时保留他们，响应仍不得暴露真实 handle 或采集状态。
- Java 变更后在仓库根目录运行 `mvn clean test`；历史 NBlog 代码不强制补单测，新增或实质修改的业务逻辑应同步增加针对性单测。Vue 变更在 `blog-view` 运行 `npm ci`、`npm test` 和 `npm run build`。
- 职责、路径、构建或权限改变时同步更新本文件、`README.md`、`upstream/nblog/blog-api/README.md`、`upstream/nblog/blog-view/README.md`、`../frontend/README.md`、`../docs/architecture.md` 和 `../docs/agent/context-map.md`。
