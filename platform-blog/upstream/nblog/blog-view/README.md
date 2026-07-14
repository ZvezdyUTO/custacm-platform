# blog-view

`blog-view` 是门户中负责公开 Blog 的 Vue 3 + Vite 构建。它提供首页、文章、分类、标签、个人主页、写作和评论，并持有 `/training/**` 外层路由以保持同一个 Blog 顶栏持续挂载。

用户只访问一个站点、一个 Nginx `frontend` 服务。源码中另一份 `frontend/` Vue 构建只负责训练业务，作为同源 `/training-app/**` 运行时嵌入本模块；两份 Vue Router 不合并，也不会显示两条顶栏。

Blog 顶栏也是生产环境唯一主题入口。两份构建共享 `custacm.theme=light|dark`；没有显式值时跟随系统偏好，首屏在样式加载前应用主题，并通过同源存储事件和受校验的 frame 消息保持 Blog 与 Training 一致。

About、Friends、Moments 页面和 API 已删除，旧 URL 不保留页面。个人资料、OJ handle、友情链接、密码和本人文章统一收敛到 `/profile`。

## 页面范围

```text
/
/home
/blog/:id
/write/:id?
/category/:name
/tag/:name
/profile
/training/**
```

`/login` 转交 `/training/login`。训练路径只允许登录、多人、单人、题目和当前六个管理员页面；无效训练子路径回退到多人统计。

## 目录结构

```text
public/          默认横幅、头像、favicon 与本地 Noto Emoji sprite 等静态资源
src/api/         Blog、评论、分类、标签、资料和本人文章 adapter
src/auth/        与训练构建共享的浏览器会话
src/components/  导航、文章、评论、个人资料和侧栏组件
src/plugins/     Axios、编辑器和表情资源
src/router/      Blog 与训练外壳路由
src/store/       Blog/评论页面状态
src/utils/       训练 frame 路径白名单等纯函数
src/views/       首页、文章、分类、标签、个人主页、写作和训练宿主
src/test/        API、会话、路由和关键交互回归测试
```

## 依赖与边界

- 使用 Vue 3、Vite、Vue Router 4、Vuex 4、Axios、Element Plus 和 Semantic UI CSS；组件继续允许 Options API。
- 日间模式的文章代码块和 Markdown 实时预览使用浅灰白底、深墨文字及蓝紫绿语法色；深夜主题恢复高对比度代码色，并使用暖炭黑/深咖黑表面、暖灰白文字和低饱和琥珀/铜橙交互色。状态语义色及分类/标签业务色保持原义。文章图片、头像、横幅和背景图仅以 260ms 过渡到 `brightness(.84) saturate(.95)`，不得反色；减少动态效果偏好下立即切换。
- Axios 默认 `baseURL` 为 `/api/`。公开请求不得全局附加 `Authorization`，也不再保存或发送游客评论 `identification`；需要登录的 adapter 显式携带 Bearer token。
- 共享登录键只有 `custacm.accessToken` 和 `custacm.user`。用户摘要只用于展示，权限始终由 Blog API 校验。
- 评论只有登录账号可以提交；请求体只包含 `content`、`blogId` 和 `parentCommentId`。公开文章评论匿名读取，内部文章评论显式 Bearer 读取。
- 评论表情选择器按常用、笑脸、情绪、爱心展示本地 Google Noto Emoji SVG；选择后只把标准 Unicode 写入评论，读取时再映射为同源 Noto 图形。历史 tv/阿鲁/泡泡短码保留展示兼容，不再作为新评论入口。
- 本人文章列表、回收站、发布、编辑、删除和恢复只调用 `/player/blog**`；后端按当前认证用户最终校验所有权。删除只移入固定七天回收站，期间关联内容保持不变。
- 文章列表、分类、标签、搜索和精选读取在存在会话时显式发送 Bearer，因此登录用户可看到内部文章；游客只看到公开文章。
- 文章详情只为登录用户显示文章包下载动作；ZIP 的 `article.md` 包含标题、简介和原始正文，本地托管首图/正文图使用扁平语义化文件名，请求显式携带 Bearer，普通用户跨文章重复下载时展示服务端 `Retry-After`，管理员不受 30 秒限制。
- 所有 `v-html` 内容先经过 `src/util/sanitizeHtml.js` 清洗。
- 头像、文章首图和正文图片只使用 Blog API 的本地托管资产接口，不引入旧 GitHub/又拍云上传。正文图片预览作为 CodeMirror 原子块参与光标移动，异步加载后主动触发布局复测；块级预览不得使用不计入编辑器高度模型的垂直外边距。
- 不在本模块复制 Training 组件、服务端授权逻辑或后端业务代码。

## 关键行为

- `/profile` 直接读取本人资料和 OJ handle。OJ handle 请求失败显示错误与重试按钮，不得把失败伪装成“未绑定”。
- 个人主页同页编辑 nickname、签名、最多八条 HTTP(S) 友情链接和密码，并展示本人文章。
- 登录评论表单提交期间禁用重复提交；401 清理共享会话并携带当前页面回到登录页。
- 首页文章、分类文章和标签文章均使用服务端分页；文章标签由后端批量装配。
- `/site` 只消费 Blog 首页需要的站点信息、分类、标签和精选文章，不依赖已删除页面字段。
- 首页横幅接口失败时回退到构建内置 `public/img/homepage-banner-default.png`；空头像回退到 `public/img/default-avatar.jpg`。
- 文章包打包期间按钮不可重复点击；401 清理共享会话并带当前文章路径跳转登录，429 显示剩余冷却秒数，503 显示下载服务暂不可用。
- “我的文章”使用克制的当前文章/回收站切换；回收站显示删除时间与剩余保留期，只提供恢复操作，不提供提前永久删除。
- 主题切换持久化明确的日间或深夜选择；无选择时响应系统主题变化。存储或系统主题 API 不可用时仍须安全启动并允许当前页面切换，减少动态效果偏好下关闭主题过渡。
- 桌面三栏中的作者/本人头像框与右侧目录、精选文章、标签云在滚动到固定顶栏下方后整列吸附；侧栏高于可视区时只在自身区域滚动，并在主内容结束、接近页脚时停止。文章页目录排在右栏首位；移动端继续隐藏两侧栏。

## 文件与路径职责

| 文件/路径 | 职责 |
| --- | --- |
| `src/main.js` | 注册 Vue、Router、Vuex 和全局样式 |
| `src/theme.js` | 共享主题解析、根节点应用、持久化、系统偏好和跨文档事件 |
| `src/router/index.js` | Blog 页面、训练外壳和 `/login` 转交；不包含 About/Friends/Moments |
| `src/utils/trainingRoute.js` | 训练 frame 路径白名单和内部 `/training-app/**` 地址构造 |
| `src/views/Index.vue` | Blog 门户三栏与桌面吸附侧栏；训练路由保留唯一 `Nav.vue` 并隐藏 Blog 侧栏 |
| `src/views/training/TrainingHost.vue` | 同源嵌入训练运行时并同步公开 `/training/**` URL |
| `src/assets/css/typo.css` | 文章 Markdown 排版、日间浅色 Prism 语法主题与横向滚动行为 |
| `src/assets/css/night.css` | 最后加载的 Semantic UI、Element Plus、业务页面暖黑橙覆盖及图片渐暗过渡 |
| `src/views/home/Home.vue` | 首页文章分页 |
| `src/views/blog/Blog.vue` | 文章详情、分类/标签、正文、登录用户文章图片归档下载和评论 |
| `src/views/category/Category.vue`、`src/views/tag/Tag.vue` | 分类和标签文章分页 |
| `src/views/profile/Profile.vue` | 个人主页、OJ handle 错误重试、资料/密码/友情链接编辑和本人文章 |
| `src/components/profile/MyArticles.vue` | 本人当前文章/回收站分页、继续编辑、移入回收站和恢复 |
| `src/views/article/ArticleEditor.vue` | Markdown 发布/编辑、首图裁剪、正文图片上传和未保存离开保护 |
| `src/components/article/LiveMarkdownEditor.vue` | CodeMirror 6 编辑、按视觉行移动光标、日间浅色代码高亮、公式与托管图片实时预览 |
| `src/plugins/articleImagePreview.js` | 正文图片原子替换区、源码编辑切换、异步图片高度复测与点击映射 |
| `src/components/article/ArticleCoverUpload.vue` | 1920×1080 首图裁剪 |
| `src/components/article/ManagedImageViewer.vue` | 默认展示缩略图，明确操作后加载高清图 |
| `src/components/index/Nav.vue` | Blog/训练导航、标题搜索、发布入口和账号菜单 |
| `src/components/index/Header.vue` | 首页横幅展示和默认图回退 |
| `src/components/sidebar/Introduction.vue` | 当前用户或文章作者公开名片 |
| `src/components/blog/BlogItem.vue` | 首页、分类和标签页的文章卡片，展示作者身份、发布时间、字数与首图；作者 name 优先占用身份条空间 |
| `src/components/sidebar/Tags.vue`、`FeaturedBlog.vue` | 标签云与管理员选择的精选文章 |
| `src/components/sidebar/Tocbot.vue` | 文章目录生成、标题激活与平滑锚点滚动；定位由 `Index.vue` 的统一吸附容器负责 |
| `src/components/comment/CommentForm.vue` | 登录评论输入、表情与提交状态 |
| `src/plugins/notoEmoji.js`、`src/util/commentContent.js` | Noto emoji 分类/同源 sprite URL、Unicode 渲染、HTML 转义与历史短码兼容 |
| `public/emoji/noto/` | Noto Emoji smileys sprite、来源说明及 Apache-2.0/MIT 许可文件 |
| `src/components/comment/CommentList.vue` | 根评论及批量装配回复的渲染 |
| `src/api/comment.js` | 公开/内部评论读取与显式 Bearer 评论提交 |
| `src/api/profile.js` | 公开作者资料及本人资料、handle、密码、头像和友情链接请求 |
| `src/api/player-blog.js` | 本人文章、七天回收站恢复和托管图片 API |
| `src/api/blog.js`、`src/util/articleDownload.js` | 公开文章读取、显式 Bearer ZIP 下载、有限长度文件名清理和浏览器保存 |
| `src/auth/session.js` | 成对校验、读取、写入和清理共享会话 |
| `src/plugins/axios.js` | `/api/` Axios client、进度条和统一响应解包；不得附加游客身份或全局 JWT |
| `public/img/homepage-banner-default.png` | 唯一默认首页横幅 |
| `public/img/default-avatar.jpg` | 空头像的统一前端回退 |
| `vite.config.js` | 4180 开发入口、训练/HMR 代理、`/api` 代理和 Vitest 配置 |
| `src/test/trainingRoute.test.js` | 训练 frame 路由白名单回归测试 |
| `src/test/theme.test.js`、`trainingThemeBridge.test.js` | 主题容错、切换持久化和同源 frame 同步测试 |
| `src/test/nightThemeStyle.test.js` | Blog 主题关键表面及代码块日间/深夜语法配色契约测试 |
| `src/test/sidebarStickyStyle.test.js` | 左右侧栏吸附、文章目录优先级及 Tocbot 单一定位职责测试 |
| `src/test/commentApi.test.js`、`commentFormState.test.js` | 登录评论请求体、Bearer 和防重复提交测试 |
| `src/test/notoEmoji.test.js`、`commentFormEmoji.test.js` | 本地 sprite 覆盖、Unicode 安全渲染、选择器可访问性与光标插入测试 |
| `src/test/profileApi.test.js` | 本人资料、handle 和友情链接 API 测试 |
| `src/test/publicVisibilityApi.test.js` | 聚合读取仅在有会话时显式附加 Bearer 的测试 |
| `src/test/articleDownloadApi.test.js`、`articleDownload.test.js` | 下载请求、文件名、浏览器保存和 `Retry-After` 测试 |
| `src/test/liveMarkdownEditor.test.js` | 编辑器工具栏、代码块点击、视觉行方向键、图片原子区与预览/源码边界测试 |
| `src/test/articleRecycleBinUi.test.js`、`playerBlogApi.test.js` | 本人回收站文案、删除/恢复交互和受保护路径测试 |
| `package.json`、`package-lock.json` | 固定依赖、脚本和可复现 npm 安装 |

## 本地开发与验证

```bash
npm ci
npm run serve
npm test
npm run build
```

统一开发入口是 `http://localhost:4180/`。开发服务器将 `/api/**` 代理到 `http://localhost:8090`，并将 `/training-app/**` 与训练 HMR 代理到 `http://localhost:5173`。
