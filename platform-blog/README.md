# platform-blog

`platform-blog` 保留 NBlog 的上游目录结构，并承载当前唯一后端与公开 Blog 前端。

## 模块职责

- `upstream/nblog/blog-api`：唯一 Spring Boot 后端，统一提供 Blog、首页图片、认证、用户/OJ handle 管理和训练数据 HTTP API。
- `upstream/nblog/blog-view`：Vue 3 + Vite 公开 Blog，生产入口为 `/`，负责文章、分类、标签、动态、友链、个人页和评论。

Vue 3 训练中心位于仓库根 `frontend/`，生产入口为 `/training/**`。该公开路由由 Vue Blog 外壳持有，以保证原 `Nav.vue` 始终挂载；训练静态运行时只暴露在内部 `/training-app/**`。两份产物打进同一个 Nginx 镜像，浏览器 API 都使用 `/api/**`；Blog API 的直接 HTTP 路径不带 `/api`。

## 后端边界

Blog API 负责：

- NBlog 文章、评论、分类、标签、Redis、Quartz、邮件和上传。
- 普通用户的本人文章管理与管理员的全量文章管理；管理员发文按当前认证账号记录作者。
- 首页横幅图片公开读取，以及管理员裁剪后图片的持久化、排序和删除。
- 文章分类与标签使用后台可管理的固定深色色票，并在 Blog 分类丝带、文章标签和标签云中统一呈现。
- 新建或升级数据库时把首页横幅收敛为 Blog 构建内置的唯一默认图，后端不可用时前端也回退到同一文件。
- BCrypt 密码、HS512 JWT、账号、`ROLE_admin`/`ROLE_player` 与 `sub=username`。
- 本人头像文件校验、上传目录持久化与 `user.avatar` 更新。
- 本人文章首图/正文图片上传、高清图与缩略图生成、文章绑定及删除/替换后的即时文件回收。
- 本人 nickname、个性签名与最多八条有序个人友情链接的读取和修改。
- 管理员用户/OJ handle 生命周期、固定 `root` 系统管理员保护和保留内容的用户删除；空头像由前端统一回退到构建内置默认头像。
- `top.naccl` 内的 `/player/training-data/**` 与 `/admin/training-data/**` HTTP adapter；查询侧注入训练模块的无 MVC facade。
- Blog 与训练 schema 的统一 Flyway 执行。

训练模块继续实现 Codeforces/AtCoder 采集、ODS/DWD/DWM/DWS、查询、调度、刷新与清理；它们不拥有登录、JWT、账号管理 HTTP 或独立运行入口。

## 前端会话边界

- Vue Blog 与 Vue 3 训练中心共享 `custacm.accessToken` 和 `custacm.user`。
- 用户摘要只负责展示，真正授权由 Blog API 的 JWT 校验与数据库当前角色决定。
- 公开 Blog 请求不全局携带 JWT。登录用户的文章列表、分类、标签、搜索和精选读取，以及评论提交，均由对应 Vue adapter 显式发送 `Authorization: Bearer <token>`；因此内部文章仅对登录用户出现在这些聚合结果中。
- Vue Blog 顶栏向登录用户提供“发布文章”；“我的主页”内嵌本人文章列表，文章详情只对作者显示编辑入口。
- 文章不再支持独立密码；内部文章只对登录用户出现在聚合列表中，正文只允许登录用户通过 `/player/**` 读取。

## 目录结构

```text
platform-blog/
  upstream/nblog/
    blog-api/   唯一后端
    blog-view/  公开 Vue Blog
```

## 关键文件职责

| 文件/路径 | 职责 |
| --- | --- |
| `upstream/nblog/blog-api/pom.xml` | 声明唯一后端的 reactor 依赖、测试与 Spring Boot 打包 |
| `upstream/nblog/blog-api/src/main/java/top/naccl/BlogApiApplication.java` | 唯一后端启动入口 |
| `upstream/nblog/blog-api/src/main/java/top/naccl/config/` | 安全、JWT、训练模块组装和 bootstrap 管理员配置 |
| `upstream/nblog/blog-api/src/main/java/top/naccl/controller/` | 公开 Blog、player 与 admin HTTP adapter |
| `upstream/nblog/blog-api/src/main/java/top/naccl/service/PlayerProfileService.java` | 本人资料校验、个人友链整体替换与安全 URL 约束 |
| `upstream/nblog/blog-api/src/main/resources/db/migration/` | Blog baseline 与训练整合 Flyway 迁移 |
| `upstream/nblog/blog-api/README.md` | 后端依赖、边界、关键文件和用户目录 contract |
| `upstream/nblog/blog-view/src/plugins/axios.js` | `/api/` 同源公开 Blog client，不全局附加 JWT |
| `upstream/nblog/blog-view/src/auth/session.js` | 共享登录摘要/JWT 的读取与清理 |
| `upstream/nblog/blog-view/src/components/index/Nav.vue` | Blog 导航、训练入口、账号摘要与退出 |
| `upstream/nblog/blog-view/public/img/homepage-banner-default.png` | 构建内置的唯一默认首页图 |
| `upstream/nblog/blog-view/public/img/default-avatar.jpg` | 用户头像字段为空时统一展示的构建内置默认头像 |
| `upstream/nblog/blog-view/src/components/sidebar/Introduction.vue` | 当前用户头像、nickname、username、签名与友情链接纯展示卡片 |
| `upstream/nblog/blog-view/src/views/about/About.vue`、`src/components/profile/MyArticles.vue` | “我的主页”资料编辑与本人文章列表 |
| `upstream/nblog/blog-view/src/views/article/ArticleEditor.vue` | Markdown 发布/编辑、文件导入、首图裁剪和正文图片上传 |
| `upstream/nblog/blog-view/src/components/profile/AvatarCropDialog.vue` | 正方形头像拖动、缩放与 512×512 PNG 导出 |
| `upstream/nblog/blog-view/src/api/comment.js`、`src/store/actions.js` | 评论读取与登录评论显式 Bearer 提交 |
| `upstream/nblog/blog-view/src/router/index.js` | Vue Blog 路由及旧登录入口转交 |
| `upstream/nblog/blog-view/vite.config.js` | Vue 3 构建、本地 API 代理和测试环境配置 |
| `upstream/nblog/blog-view/package.json`、`package-lock.json` | Vue 依赖、脚本与可复现 npm 安装 |
| `upstream/nblog/blog-view/README.md` | Vue 模块职责、依赖边界、文件说明和构建命令 |

更细的实现与测试说明见各子模块 README。
