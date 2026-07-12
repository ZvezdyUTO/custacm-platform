# blog-view

## 模块职责

`blog-view` 是 NBlog 的 Vue 3 站点外壳，负责首页、文章、分类、标签、动态、友链、“我的主页”和登录用户的文章/评论界面，并持有 `/training/**` 外层路由以保证同一个 Blog 顶栏持续挂载。训练业务页面仍由同域的独立 Vue 3 运行时负责，本模块只用同源 frame 承载其内容。

浏览器 API 统一从 `/api/` 发起，由站点网关转发到 Blog API。公开 Blog 请求不得通过 Axios 全局拦截器携带训练 JWT。

## 目录结构

- `public/`：无需打包转换的公开静态资源。
- `src/api/`：公开 Blog 与评论接口适配。
- `src/auth/`：与训练中心共享的本地会话摘要读取和清理逻辑。
- `src/components/`：Blog 导航、页脚、文章、评论、个人头像裁剪和侧栏组件。
- `src/plugins/`：Axios 实例及评论访客 `identification` 兼容逻辑。
- `src/router/`：Vue Blog 路由；`/login` 转交训练中心，`/training/**` 保留 Blog 外壳。
- `src/store/`：Vuex 页面状态。
- `src/views/`：公开 Blog 页面。
- `src/test/`：共享会话与 Vue 3 迁移后的基础行为回归测试。
- `dist/`：生产构建产物，不作为源代码手工编辑。

## 依赖与边界

- 使用 Vue 3、Vite、Vue Router 4、Vuex 4、Axios、Element Plus 和 Semantic UI CSS。
- 组件暂时保留 Options API，避免把框架升级和业务重写耦合在同一变更中。
- 公开训练中心路径为 `/training/**`，由 Blog Router 承载并在原 `Nav.vue` 下嵌入内部 `/training-app/**`；两套 Router 不合并。
- 共享登录摘要只使用 `custacm.accessToken` 和 `custacm.user`。`custacm.user` 仅用于展示，不作为授权依据。
- Axios 默认 `baseURL` 为 `/api/`，保留匿名评论的 `identification` 头，不得全局附加 `Authorization`。
- 首页 `/site` 初始化响应只消费 `siteInfo.reward`、`siteInfo.commentAdminFlag`、`introduction.avatar`、`introduction.name`、分类、标签和精选文章；不得恢复旧徽章、收藏或最新文章侧栏字段。
- 所有 `v-html` 内容必须先经过 `src/util/sanitizeHtml.js` 的 DOMPurify 清洗，作为服务端白名单之外的前端纵深防御。
- 登录后的评论提交通过 `src/auth/session.js` 校验共享会话，再为该请求显式发送 `Authorization: Bearer <token>`。
- 顶栏登录入口将当前完整 Blog 路径作为 `returnTo`，登录成功后回到进入登录页之前的位置。
- 本人文章列表、发布、编辑和删除只调用 `/player/blog**`；前端显式携带 Bearer JWT，后端仍以 `blog.user_id` 做最终所有权校验。
- 首页文章标题最多展示三行；发布与编辑共用标题 100 字、简介 255 字、正文 200000 字的输入约束，后端仍执行同样的最终校验。
- 本人文章图片只调用 `/player/images`：首图在浏览器裁剪为 1920×1080，正文 JPEG/PNG 最大 15MB；正文插入标准图片 Markdown 并在实时编辑区直接显示缩略图，阅读预览仅在点击“加载原图”后请求高清图。尚未保存的正文图片从编辑器移除后立即回收，已绑定图片在文章保存成功后回收。
- 文章不再支持独立密码；内部文章通过共享登录 JWT 调用 `/player/internal-blog` 和 `/player/comments` 读取正文及评论；文章列表、分类、标签、搜索和精选读取在存在会话时显式发送 Bearer，因此仅登录用户会在这些聚合结果中看到内部文章。
- 受保护请求的 JWT 和权限校验由训练中心及 Blog API 负责。
- 不在本模块中引入训练中心组件、后端业务代码或部署配置。

## 文件职责

- `src/main.js`：使用 `createApp` 注册 Vue 插件、全局样式和应用入口。
- `src/router/index.js`：声明公开 Blog 路由、训练外壳路由，并把旧 `/login` 替换跳转到 `/training/login`。
- `src/util/get-page-title.js`：统一生成带 `custacm-platpform` 品牌后缀的浏览器页面标题。
- `src/plugins/axios.js`：创建同源 Blog API 客户端并维护评论访客标识。
- `src/auth/session.js`：成对校验并读取共享用户摘要或裸 JWT；清理时发送稳定的同页 `custacm:session-change` 事件，同时移除旧 `memberToken/memberUser`，但保留评论 identification。
- `src/auth/account-menu.js`：按当前角色生成账号菜单；统一使用“我的主页”，管理员额外显示“管理员界面”。
- `src/components/index/Nav.vue`：使用 `public/img/custacm-wordmark.png` 渲染固定的藏青色 CUSTACM 字标、Blog/训练导航，顶栏不展示“动态”入口；“训练中心”标题只通过点击展开或收起下拉菜单，悬停不展开且标题本身不跳转；登录用户可见顶栏“发布文章”入口；右侧搜索框仅在按 Enter 后查询文章标题，最多展示十条结果，游客只获得公开文章，登录用户也可获得内部文章；账号名称栏为较长用户名保留桌面展示空间。
- `src/components/index/Footer.vue`：渲染平台欢迎语，以及带官网图标的圆角项目仓库、Codeforces、AtCoder、洛谷、牛客竞赛和 QOJ 固定链接。
- `src/components/index/Header.vue`：从公开首页图片接口读取一至两张有序横幅，桌面按视口整屏显示，移动端继续加载并收缩为视口高度的 46%（限制在 280–420px）；保留鼠标左右移动时的相邻图片渐变切换；首屏通过 Google Fonts 加载 Bowlby One SC，以 80% 不透明度的冷调象牙白填充和黑色描边在画面上方呈现双行 `WELCOME TO CUSTACM`，并保留原逐字浮动、品牌浮动和整组淡入动画；接口失败时使用 `src/settings.js` 指向的构建内置默认图。
- `public/img/homepage-banner-default.png`：构建时随 Blog 静态产物发布的唯一默认首页图，同时供 Flyway 初始化数据和接口失败回退使用。
- `public/favicon.svg`：浏览器标签页使用的简约几何气球品牌图标。
- `src/util/homepageBanner.js`：把鼠标位置映射为任意数量横幅的相邻图层透明度。
- `src/assets/css/base.css`：提供 Blog 全局基础样式，以共享的 `#f4f6f8` 雾灰画布与训练、管理页面保持底色一致；前端不再加载播放器或歌词组件。
- `src/assets/css/typo.css`：提供文章 Markdown 的排版与高对比度代码主题；行内代码样式不影响列表中的块级代码，代码被选中时统一使用深蓝选区与白色文字；长代码、表格、公式和不可断行段落在自身区域显示可拖动的横向滚动条，不得撑破文章列。
- `src/components/sidebar/Introduction.vue`：普通页面显示当前登录用户的名片，文章详情页改为显示文章作者的公开头像、nickname、username、email、个性签名和有序友情链接；email 位于 username 下方并复用相同字号和颜色，空 email 不展示；友情链接自动读取目标站点根目录 favicon，加载失败时回退为通用网页图标；当前用户的大尺寸名片优先使用头像原图，小尺寸缩略图仅作为兼容回退，资料保持纯展示，在本人个人页仍可通过原有头像交互打开裁剪器。
- `src/components/profile/AvatarCropDialog.vue`：允许拖动、缩放本地 PNG/JPEG，并导出 512×512 PNG 交给头像 API。
- `src/views/about/About.vue`：“我的主页”，展示当前用户资料、OJ handle、友情链接与本人文章区，并在资料编辑面板内提供本人密码修改表单。
- `src/views/blog/Blog.vue`：渲染公开文章详情、分类标签、正文和评论；分类丝带位于正文网格上方，不参与内容列宽计算；详情页标题下方沿用作者、日期、浏览量、字数和估算阅读时长的横排摘要，并在有首图时以正文列为基准居中展示 16:9 图片。
- `src/components/blog/BlogItem.vue`：渲染首页、分类和标签页的文章摘要卡；卡片稳定保持左右两栏，左侧展示标题、分类、简介和阅读全文按钮，右侧以同宽纵向排列作者信息与 16:9 首图，容器确实不足时才整体改单栏，避免作者栏随桌面窗口缩窄而横向拉满。
- `src/components/profile/MyArticles.vue`：在“我的主页”内分页查询本人文章，已发布文章进入公开详情，草稿进入继续编辑，并支持删除。
- `src/views/article/ArticleEditor.vue`：实时 Markdown 文章发布/编辑页，支持标题/简介计数与长度限制、正文长度校验、首图裁剪、正文图片上传、Markdown 文件读取、草稿/发布、评论开关与未保存离开提示；正文编辑器使用高对比度深色文本选区。
- `src/components/article/ArticleCoverUpload.vue`：16:9 首图拖动裁剪并导出 1920×1080 JPEG。
- `src/components/article/ManagedImageViewer.vue`：先展示正文缩略图，用户明确点击后才加载高清图。
- `src/components/sidebar/Tags.vue`：从全部标签中随机抽取最多 30 个显示为标签云，并按标签名称稳定映射彩色标签样式。
- `src/components/sidebar/FeaturedBlog.vue`：显示由管理员选中的最多五篇精选文章；列表使用服务端固定顺序，不在浏览器端随机刷新。
- `src/components/article/LiveMarkdownEditor.vue`：封装 CodeMirror 6 与 live-markdown，提供工具栏、GFM 表格、高对比度代码语法高亮、按浏览器真实文本命中定位的代码块光标、公式/托管图片实时预览，以及正文图片选择、拖拽和粘贴上传。
- `src/plugins/standardMathPreview.js`、`src/util/markdownEditor.js`：弥合第三方编辑器公式方言差异，并提供标准数学公式识别和工具栏 Markdown 插入模板。
- `src/api/player-blog.js`、`src/util/articleForm.js`、`src/util/articleImages.js`：本人文章/图片 API、请求组装、图片限制、托管图片 URL 转换与整图 Backspace 删除。
- `src/api/profile.js`：匿名读取文章作者公开资料，读取本人完整资料/OJ handle，并只为本人 nickname、签名、友情链接、头像与密码更新请求显式附加共享 Bearer JWT。
- `src/components/comment/Comment.vue`：以放大头像展示评论；账号仍存在时在昵称下显示弱化 username，游客或已注销账号不留空行；同时响应登录状态并监听 session-change/storage 事件。
- `src/components/comment/CommentForm.vue`：提交时重新读取共享 JWT，保留既有未登录和失败提示。
- `src/api/comment.js`：匿名读取公开评论；登录评论提交显式使用共享 Bearer JWT。
- `src/store/actions.js`：编排评论状态；401 清理共享会话并携带当前 Blog 路由跳转登录，403 显示后端拒绝原因。
- `src/views/Index.vue`：组合站点外壳；Blog 内容容器以 1400px 为桌面上限并随实际视口收缩，浏览器侧栏改变可视宽度时不得产生裁切；训练路由保留唯一 `Nav.vue` 并隐藏 Blog 侧栏；普通训练查询页沿用 Blog 页脚，管理员页面隐藏页脚。
- `src/views/training/TrainingHost.vue`：在 Blog 顶栏下同源嵌入训练运行时，并将内部训练路由同步到公开 `/training/**` URL。
- `vite.config.js`：配置 Vue 编译、源码别名、4180 统一开发入口、训练应用/HMR 与 `/api` 代理，以及 Vitest 环境。
- `src/test/session.test.js`：验证共享登录键、孤儿会话清理和稳定变更事件。
- `src/test/accountMenu.test.js`：验证普通队员和管理员的账号菜单权限差异。
- `src/test/dateTimeFormatUtils.test.js`：验证移除 Vue 2 filter 后的日期格式契约。
- `src/test/getPageTitle.test.js`：验证浏览器标题的页面名称与平台品牌组合规则。
- `src/test/homepageBanner.test.js`：验证单图、首尾定位与相邻图片交叉淡入淡出。
- `src/test/profileApi.test.js`：验证本人资料和友情链接请求使用正确路径、方法与显式 Bearer header。
- `src/test/publicVisibilityApi.test.js`：验证文章列表、分类、标签、搜索和精选读取仅在存在会话时显式附加 Bearer。
- `src/test/introduction.test.js`：验证当前用户名片优先使用头像原图，并在原图缺失时回退缩略图或默认头像。
- `src/test/playerBlogApi.test.js`、`src/test/articleForm.test.js`：验证本人文章路径/Bearer header、请求组装、Markdown 导入与作者匹配。
- `src/test/markdownEditor.test.js`、`src/test/liveMarkdownEditor.test.js`：验证标准公式识别、Markdown 插入模板、CodeMirror 挂载、KaTeX 实时预览和正文双向同步。
- `src/test/commentApi.test.js`：验证登录评论只调用 `/player/comment` 并显式附加 Bearer JWT。
- `src/test/commentIdentity.test.js`：验证账号评论展示 username，游客评论不展示空身份行。
- `package.json`：声明固定版本依赖及 Vite/Vitest 脚本。
- `package-lock.json`：锁定 npm 依赖解析结果。

## 本地开发

```bash
npm ci
npm run serve
```

统一本地入口为 `http://localhost:4180/`。开发服务器将 `/api/**` 代理到 `http://localhost:8090`，并将 `/training-app/**` 与训练应用的 HMR 通道代理到 `http://localhost:5173`。先在 `frontend` 启动训练 Vite，再启动本模块，即可在 `/training/**` 下同时热更新两套 Vue 应用。

## 测试与生产构建

```bash
npm ci
npm test
npm run build
```
