# blog-view

`blog-view` 是门户中负责公开 Blog 的 Vue 3 + Vite 构建。它提供首页、文章、分类、标签、赛事与获奖记录、个人主页、写作和评论，并持有 `/training/**` 外层路由以保持同一个 Blog 顶栏持续挂载。

用户只访问一个站点、一个 Nginx `frontend` 服务。源码中另一份 `frontend/` Vue 构建只负责训练业务，作为同源 `/training-app/**` 运行时嵌入本模块；两份 Vue Router 不合并，也不会显示两条顶栏。

Blog 与 Training 固定使用共享浅色语义 token，不读取系统配色、不持久化视觉模式，也不在 frame 间传递主题消息。Blog 唯一顶栏在首页、文章详情、写作和 Training 外壳使用 `rgba(250, 249, 245, .72)` 米白毛玻璃，使其在同色画布上与 `#faf9f5` 底色一致；固定暖黑的文章目录继续使用原暗色毛玻璃背景和浅色半透明边框，两者复用同一模糊强度；顶栏不提供主题切换入口，文章目录、黑色页脚等固定深色区域仍是页面本身的视觉设计。

About、Friends、Moments 页面和 API 已删除，旧 URL 不保留页面。个人资料、友情链接、密码、头像和本人文章统一收敛到 `/profile`；OJ handle 仍由训练管理端统一维护，个人主页不重复展示 OJ 卡片。

## 页面范围

```text
/
/home
/articles
/blog/:id
/write/:id?
/category/:name
/tag/:name
/competitions
/competitions/:id
/profile
/training/**
```

`/login` 转交 `/training/login`。训练路径只允许登录、多人、单人、题目和当前七个管理员页面；无效训练子路径回退到多人统计。

## 目录结构

```text
public/          默认横幅、头像、favicon 与本地 Noto Emoji sprite 等静态资源
src/api/         Blog、评论、分类、标签、比赛、资料和本人文章 adapter
src/auth/        与训练构建共享的浏览器会话
src/components/  导航、文章、评论、个人资料和侧栏组件
src/plugins/     Axios、编辑器和表情资源
src/router/      Blog 与训练外壳路由
src/store/       Blog/评论页面状态
src/utils/       训练 frame 路径白名单等纯函数
src/views/       首页、文章、分类、标签、公开比赛、个人主页、写作和训练宿主
src/test/        API、会话、路由和关键交互回归测试
```

## 依赖与边界

- 使用 Vue 3、Vite、Vue Router 4、Vuex 4、Axios、Element Plus 和 Lucide；组件继续允许 Options API。阶段 6 已退出 Semantic UI，页面结构只使用项目自有布局 class，业务模板通过全局 `AppIcon` 使用集中维护的 Lucide 语义映射。代码排版通过 Fontsource 内置 JetBrains Mono Variable，不依赖客户端预装字体或外部 CDN。
- 共享视觉 token 的唯一源位于仓库根 `frontend-design-tokens/tokens.css`；`src/assets/css/tokens.css` 是脚本生成副本，由 `main.js` 在基础样式前加载。`blog-redesign.css` 统一固定浅色语义角色与 Element Plus 视觉变量。
- 文章代码块和 Markdown 实时预览固定使用浅灰白底、深墨文字及蓝紫绿语法色。状态语义色及分类/标签业务色保持原义；文章图片、头像、横幅和背景图不再因全局视觉模式变化而额外滤色。
- Axios 默认 `baseURL` 为 `/api/`。公开请求不得全局附加 `Authorization`，也不再保存或发送游客评论 `identification`；需要登录的 adapter 显式携带 Bearer token。
- 共享登录键只有 `custacm.accessToken` 和 `custacm.user`。用户摘要只用于展示，权限始终由 Blog API 校验。
- 评论只有登录账号可以提交；请求体只包含 `content`、`blogId` 和 `parentCommentId`。公开文章评论匿名读取，内部文章评论显式 Bearer 读取。
- 评论表情选择器按常用、笑脸、情绪、爱心展示本地 Google Noto Emoji SVG；选择后只把标准 Unicode 写入评论，读取时再映射为同源 Noto 图形。历史 tv/阿鲁/泡泡短码保留展示兼容，不再作为新评论入口。
- 本人文章列表、回收站、发布、编辑、删除和恢复只调用 `/player/blog**`；后端按当前认证用户最终校验所有权。删除只移入固定七天回收站，期间关联内容保持不变。
- 公开赛事列表与详情匿名调用 `/competitions`，按年份闭区间、单个规范 `category` 和分页读取完整聚合树，不为列表项制造详情 N+1 请求或在前端二次分页；分页后把焦点和视口带回新结果摘要。筛选菜单只显示省赛、两种全国邀请赛、两种区域赛、EC-Final、CCPC-Final、百度之星、GPLT 国赛和蓝桥杯国奖十项。本人参赛文章绑定、奖项名片展示偏好及顺序通过 `/player/competitions/**` 显式携带 Bearer；比赛只在用户展开某篇可绑定公开文章的轻量“关联赛事”入口后按需读取并在当前页面生命周期缓存。资料、文章和比赛请求按会话代次隔离，旧账号或旧筛选响应不得覆盖当前状态；前端不提供奖项事实编辑或管理员代改他人展示状态。
- 文章列表、分类、标签和搜索读取在存在会话时显式发送 Bearer，因此登录用户可看到内部文章；游客只看到公开文章。`GET /site` 即使携带可选 Bearer，`featuredGroups` 也始终只包含公开、已发布且未回收的文章，登录状态不会改变首页精选内容。
- 文章阅读页和发布/编辑页只复用 Anthropic 文章页的配色体系，不改变现有布局、字号、间距或功能：画布使用 `#faf9f5` 米白，分区使用 `#f0eee6`/`#e8e6dc`，正文使用 `#141413` 炭黑，次要信息与边框使用对应暖灰阶，关键操作使用 `#d97757` 暖橙。阅读页在左侧作者名片右侧以两行紧凑文字为登录用户提供“下载文章”，并仅向当前文章作者提供“编辑文章”；作者主动公开的奖项按个人保存顺序在签名下方显示为一项一行的比赛名/奖项等级荣誉条，紧凑条使用 `10px` 字体和约 `23px` 单行高度，不显示排名，默认显示前三项，更多项目可展开/收起，整条链接对应比赛详情。奖档分别使用低饱和金、银、黄铜、铁棕背景，省赛/邀请赛/区域赛/Final 使用由疏到密的点阵、斜纹、交叉网格和菱形纹，百度之星、蓝桥杯和 GPLT 使用各自低对比专项纹理；比赛详情和个人主页的完整奖项记录仍为奖牌类展示排名、普通奖项不展示排名。字体和阅读模式控件暂不展示。ZIP 的 `article.md` 仍包含标题、简介和原始正文，本地托管首图/正文图使用扁平语义化文件名，普通用户跨文章重复下载受服务端 30 秒窗口约束，管理员不受该窗口限制。
- 所有 `v-html` 内容先经过 `src/util/sanitizeHtml.js` 清洗。
- 头像、文章首图和正文图片只使用 Blog API 的本地托管资产接口，不引入旧 GitHub/又拍云上传。正文图片预览作为 CodeMirror 原子块参与光标移动，异步加载后主动触发布局复测；块级预览不得使用不计入编辑器高度模型的垂直外边距。
- 不在本模块复制 Training 组件、服务端授权逻辑或后端业务代码。

## 关键行为

- `/profile` 直接读取本人资料，以可点击的圆形头像和昵称/username 组成唯一身份头部；点击头像复用正方形裁剪上传流程。页面不再重复展示 Codeforces、AtCoder、用户名、昵称和签名信息卡。
- 个人主页同页编辑 nickname、最多 40 字符的签名、最多八条 HTTP(S) 友情链接和密码，展示全部本人奖项、独立公开开关和公开项顺序调整，并通过紧凑的“关联赛事”展开清单为本人公开已发布文章维护参赛比赛绑定。
- `/competitions` 使用年份与规范分类筛选赛事档案列表，`/competitions/:id` 展示参赛形态、分类、参赛用户、相关文章和完整个人/团队奖项事实；已注销账号以昵称快照展示。赛事列表与详情的页面画布统一使用 `#faf9f5` 米白，内部档案表面保持原有配色。
- 登录评论表单提交期间禁用重复提交；401 清理共享会话并携带当前页面回到登录页。
- 顶栏“文章”直接进入 `/articles`，不再通过“分类”下拉菜单导航，顶栏本身不提供搜索框。文章总览、分类与标签页共享暖黑文章目录：总览标题固定为“全部文章”，分类和标签页标题直接使用当前分类名或标签名，并统一使用可覆盖技术、训练与随笔内容的说明文案；页面左栏提供分类直达，下面从服务端标签列表随机抽取最多十二个标签并允许换一组；右侧提供全站文章标题搜索和网格/列表切换，输入过程不请求，只有回车或点击输入框左侧搜索按钮才调用 `/searchBlog`。桌面网格使用一行三篇的暖黑卡片，首图和无图占位统一为 16:9 黑底，图片以 `contain` 完整显示，不拉伸或裁切；作者行标签保持完整宽度，按卡片可用空间只展示前几个并以省略号表示剩余标签。首页仍不渲染普通文章分页，只展示精选文章，文章标签继续由后端批量装配。
- `/site` 只消费 Blog 首页需要的站点信息、分类、标签和 `featuredGroups`，不依赖已删除页面字段。`featuredGroups` 最多三组，每组由可编辑标题和固定三篇文章组成，并按后台保存的组顺序、组内文章顺序展示；成员文章后来变为草稿、内部文章或进入回收站时整组暂不公开，恢复资格后自动重新出现。
- 首页首屏只展示构建内置 `public/img/homepage-banner-default.png`，不请求动态横幅接口。首图渐隐区下方匿名读取 `GET /homepage-featured-images` 并一次展示全部精选图片，三份副本组成向左缓慢滚动的无缝环；初始只加载 `thumbnailUrl` 压缩图，点击打开共享预览器后仍先显示缩略图，用户明确选择“加载原图”才请求 `imageUrl`。悬停、键盘聚焦或拖动时暂停，支持鼠标拖动、触控板和方向键手动滚动，左右边界使用渐隐与背景模糊。减少动态效果偏好下不自动滚动。空头像回退到 `public/img/default-avatar.jpg`。
- 文章包打包期间按钮不可重复点击；401 清理共享会话并带当前文章路径跳转登录，429 显示剩余冷却秒数，503 显示下载服务暂不可用。
- “我的文章”只承担现有内容管理，不重复提供发布入口；发布文章统一从顶栏进入。区域使用克制的当前文章/回收站切换，回收站显示删除时间与剩余保留期，只提供恢复操作，不提供提前永久删除。
- 全局视觉固定为浅色，不读取 `prefers-color-scheme`，不写主题存储，也不向 Training frame 发送视觉模式消息。
- 首页首屏在现有唯一顶栏下使用居中的大字号 `Welcome to the CUSTACM Platform` 标题和单张横向通栏 16:9 首图；标题采用接近 Notion 展示标题的系统 Display Sans 字体链、800 字重、紧字距、独立单词间距和低行高，并复用旧版逐字错峰浮动节奏形成轻微不规则波浪。首图使用居中 `cover` 裁切并贴满浏览器左右边缘，不添加边框、圆角或阴影，使 PNG 透明区域与 Hero 背景直接融合。ICPC、CCPC 和完整圆形 CUSTACM 校队标志叠放在首图左上方留白区，并以不同周期、相位和振幅轻微上下浮动；首图底部继续以背景渐变与局部模糊柔化硬边。标题和标志动画均服从系统减少动态效果设置。下方只展示带页边距的精选区，不渲染普通文章列表、个人介绍或标签云。首页固定使用 Notion 式中性色：`#faf9f5` 米白画布、`#f7f7f5` 卡面、`#f1f1ef` 悬停面、`#e9e9e7` 边框、`#050505` 主文字、`#37352f` 正文和 `#787774` 次要文字；底部黑色链接栏不随此配色调整。精选区最多展示三组，每组先显示自定义标题，再以第一篇横向大卡和下方并排的第二、三篇组成接近 Notion 的层级布局。所有文章首图使用 16:9 媒体框和 `contain` 完整显示，不裁剪原图；卡片同时展示分类、标题、三行简介、日期，以及作者头像、昵称和 `@username`。文章详情页使用 IDE 式双区，左侧窄工具栏组合作者、目录和评论入口，作者信息右侧上下排列登录用户下载与作者编辑操作；右侧以大标题、日期、可选 16:9 首图和正文组成独立阅读画布；首图与正文图片统一 20px 圆角，移动端隐藏工具栏。

## 文件与路径职责

| 文件/路径 | 职责 |
| --- | --- |
| `src/main.js` | 注册 Vue、Router、Vuex、Element Plus、全局 `AppIcon` 和项目样式；不得重新导入 Semantic UI 或旧图标字体 |
| `src/components/common/AppIcon.vue` | 将稳定的业务图标名称映射到 Lucide，并统一尺寸、加载旋转与 reduced-motion 降级 |
| `src/router/index.js` | Blog 页面、训练外壳和 `/login` 转交；不包含 About/Friends/Moments |
| `src/utils/trainingRoute.js` | 训练 frame 路径白名单和内部 `/training-app/**` 地址构造 |
| `src/views/Index.vue` | Blog 门户响应式外壳、`/site` 的 `featuredGroups` 首页状态与文章 IDE 式双区；桌面锁定单视口高度，左侧作者/目录工具栏与右侧阅读画布分别独立滚动，作者名片承载登录用户下载与作者编辑操作，训练路由保留唯一 `Nav.vue` 并隐藏 Blog 工具栏 |
| `src/views/training/TrainingHost.vue` | 同源嵌入训练运行时并同步公开 `/training/**` URL；使用 border-box 在单视口内预留顶栏高度，并标记外层 Training 状态以使用中性圆头滚动条 |
| `src/assets/css/typo.css` | 文章 Markdown 排版、统一 JetBrains Mono 行内/块级代码字体、固定浅色 Prism 语法配色与横向滚动行为 |
| `src/assets/css/base.css` | 项目自有的基础重置、导航/网格/内容表面、侧栏与通用控件布局基础，以及全局等宽字体角色 |
| `src/assets/css/tokens.css` | 从仓库根共享源生成并由 `main.js` 首先加载的视觉 token 副本；禁止手工编辑 |
| `src/assets/css/blog-redesign.css` | Blog 固定浅色语义映射、Element Plus 变量、有限玻璃、现代圆角和实体内容表面覆盖 |
| `src/views/home/Home.vue` | 保留的首页文章分页组件；当前首页外壳暂不渲染 |
| `src/views/blog/Blog.vue` | 文章详情的完整换行大标题/日期/可选首图、首图下完整换行的浅色衬线简介、统一圆角正文图片、按持久化业务色展示的分类圆点/彩色标签和完整评论；文章操作由 `Index.vue` 的左侧作者名片承载，本组件不展示字体或阅读模式控件 |
| `src/views/category/Category.vue`、`src/views/tag/Tag.vue` | `/articles` 文章总览及分类/标签分页；共享 Claude 参考页式暖黑目录、分类侧栏、随机标签、显式提交的全站标题搜索和网格/列表视图 |
| `src/views/competition/CompetitionList.vue`、`CompetitionDetail.vue` | 公开赛事紧凑规范分类筛选分页与参赛者、文章、奖项完整详情；列表页头说明文案“勇攀高峰”相对主标题右移 16px 对齐视觉引导线，并使用右贴边、满高低对比的比赛会场全景图，空起止年份均明确显示“不限” |
| `src/utils/competitionTypes.js` | 十个规范赛事分类、旧类型标签只读兼容与公开分类标签折叠 |
| `src/views/profile/Profile.vue` | 个人主页头像身份头部与裁剪上传、奖项加载错误重试、跨会话请求隔离、资料/密码/友情链接编辑、奖项公开偏好/顺序和本人文章 |
| `src/components/profile/AchievementsPanel.vue` | 本人全部奖项、逐项公开开关与公开顺序；紧凑模式在文章作者名片折叠渲染可点击荣誉条 |
| `src/utils/achievementPresentation.js` | 规范奖档金银铜铁色 class、赛事分类纹理 class 与排名展示映射 |
| `src/components/profile/MyArticles.vue` | 本人当前文章/回收站 latest-wins 分页、轻量入口按文章展开紧凑赛事绑定清单、懒加载缓存、继续编辑、移入回收站和恢复 |
| `src/views/article/ArticleEditor.vue` | Markdown 发布/编辑、首图裁剪、正文图片上传和未保存离开保护 |
| `src/components/article/LiveMarkdownEditor.vue` | CodeMirror 6 编辑、JetBrains Mono 等宽排版、按视觉行移动光标、固定浅色代码高亮、公式与托管图片实时预览 |
| `src/plugins/articleImagePreview.js` | 正文图片原子替换区、源码编辑切换、异步图片高度复测与点击映射 |
| `src/components/article/ArticleCoverUpload.vue` | 1920×1080 首图裁剪 |
| `src/components/article/ManagedImageViewer.vue` | 默认展示缩略图，明确操作后加载高清图 |
| `src/util/dialogFocus.js` | Blog 弹层共享的焦点进入、Tab 循环和关闭后焦点归还工具 |
| `src/components/index/Nav.vue` | Blog/训练导航、发布入口和账号菜单；浅色路由使用共享浅色毛玻璃，暖黑文章目录使用同模糊强度的暗色毛玻璃；不承载搜索，“文章”直接进入 `/articles` 且无分类下拉，五个主导航项共享统一规格 |
| `src/components/index/Header.vue` | 首页大标题、构建内置静态首图、底部渐隐与首图留白区赛事/校队标志行 |
| `src/components/index/FeaturedImageMarquee.vue` | 进入首图渐隐区的滚动精选图；压缩图优先、共享高清预览、三副本无缝左滚、悬停/聚焦暂停、拖动/触控板/键盘滚动和左右渐隐模糊 |
| `src/components/index/Footer.vue` | 保持黑色的站点与竞赛友情链接栏；使用不可压缩的最小高度和底部安全间距，避免末行被视口裁切 |
| `src/api/index.js` | `/site` 与公开滚动精选图片读取；公开请求不得附加共享 JWT |
| `src/components/sidebar/Introduction.vue` | 当前用户或文章作者公开名片 |
| `src/components/blog/BlogItem.vue` | 分类和标签页的文章卡片，展示作者身份、发布时间、字数、标签与首图；分类页启用整卡可点击的双栏 16:9 首图卡片，标签页保留单列布局 |
| `src/components/sidebar/Tags.vue`、`FeaturedBlog.vue` | 保留的标签组件与首页分组精选区域；当前外壳不展示标签云，精选最多三组、每组三篇，按服务端顺序渲染一大两小卡片、完整 16:9 首图、作者信息和彩色标签；主卡最多展示五个标签、小卡最多三个并以省略号提示剩余项，顶部主卡标题与简介均最多展示三行，超出后显示省略号 |
| `src/components/sidebar/Tocbot.vue` | 文章目录生成、标题激活与平滑锚点滚动；滚动监听绑定 `Index.vue` 的右侧独立阅读画布，目录标题最多显示两行并对溢出内容使用省略号 |
| `src/components/comment/CommentForm.vue` | 登录评论输入、表情与提交状态 |
| `src/plugins/notoEmoji.js`、`src/util/commentContent.js` | Noto emoji 分类/同源 sprite URL、Unicode 渲染、HTML 转义与历史短码兼容 |
| `public/emoji/noto/` | Noto Emoji smileys sprite、来源说明及 Apache-2.0/MIT 许可文件 |
| `src/components/comment/CommentList.vue` | 根评论及批量装配回复的渲染 |
| `src/api/comment.js` | 公开/内部评论读取与显式 Bearer 评论提交 |
| `src/api/profile.js` | 公开作者资料及本人资料、handle、密码、头像和友情链接请求 |
| `src/api/player-blog.js` | 本人文章、七天回收站恢复和托管图片 API |
| `src/api/competition.js`、`src/api/player-competition.js` | 匿名规范分类比赛查询及显式 Bearer 的文章绑定、奖项展示偏好/顺序请求 |
| `src/api/blog.js`、`src/util/articleDownload.js` | 公开文章读取、显式 Bearer ZIP 下载、有限长度文件名清理和浏览器保存 |
| `src/auth/session.js` | 成对校验、读取、写入和清理共享会话 |
| `src/plugins/axios.js` | `/api/` Axios client、进度条和统一响应解包；不得附加游客身份或全局 JWT |
| `public/img/homepage-banner-default.png` | 首页唯一静态首图；不受后台接口管理 |
| `public/img/competition-archive-contest-hall.jpg` | 赛事荣誉列表页头右侧的低透明度比赛会场全景背景 |
| `public/img/home-logos/` | 首页 ICPC、CCPC 与圆形 CUSTACM 标志及其来源说明 |
| `public/img/default-avatar.jpg` | 空头像的统一前端回退 |
| `vite.config.js` | 4180 开发入口、训练/HMR 代理、`/api` 代理和 Vitest 配置 |
| `src/test/trainingRoute.test.js` | 训练 frame 路由白名单回归测试 |
| `src/test/blogRedesignStyle.test.js` | 共享 token、Element Plus 映射、有限玻璃和 reduced-motion 样式契约测试 |
| `src/test/semanticUiExit.test.js` | 禁止生产代码重新引入 Semantic UI class、旧 `<i>` 图标、全局 CSS 或 npm 依赖 |
| `src/test/avatarCropDialog.test.js` | 头像裁剪弹层的焦点进入、Esc 和焦点归还回归测试 |
| `src/test/managedArticleImageUi.test.js` | 正文托管缩略图的可聚焦标记和键盘打开预览回归测试 |
| `src/test/sidebarStickyStyle.test.js` | 左右侧栏吸附、文章目录优先级及 Tocbot 单一定位职责测试 |
| `src/test/commentApi.test.js`、`commentFormState.test.js` | 登录评论请求体、Bearer 和防重复提交测试 |
| `src/test/notoEmoji.test.js`、`commentFormEmoji.test.js` | 本地 sprite 覆盖、Unicode 安全渲染、选择器可访问性与光标插入测试 |
| `src/test/profileApi.test.js` | 本人资料、handle 和友情链接 API 测试 |
| `src/test/competitionApi.test.js`、`competitionNavigation.test.js`、`competitionPages.test.js` | 公开比赛请求、路由导航和赛事页面渲染测试 |
| `src/test/playerCompetitionApi.test.js`、`achievementsPanel.test.js`、`profileAchievements.test.js` | 本人比赛写入合同、奖项展示开关与 401 会话处理测试 |
| `src/test/articleCompetitionBindings.test.js` | 本人公开文章的参赛比赛筛选、关联/解绑与并发状态测试 |
| `src/test/publicVisibilityApi.test.js` | 聚合读取仅在有会话时显式附加 Bearer 的测试 |
| `src/test/featuredBlog.test.js` | 首页精选最多三组、一大两小布局、16:9 首图完整显示、作者信息和键盘进入文章的回归测试 |
| `src/test/homepageFeaturedImages.test.js` | 精选图片公开无认证请求、首页位置、三副本循环、暂停/手动滚动和边界模糊合同测试 |
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
