# blog-view

## 模块职责

`blog-view` 是 NBlog 的 Vue 3 站点外壳，负责首页、文章、分类、标签、归档、动态、友链、关于页和评论界面，并持有 `/training/**` 外层路由以保证同一个 Blog 顶栏持续挂载。训练业务页面仍由同域的独立 Vue 3 运行时负责，本模块只用同源 frame 承载其内容。

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
- 登录后的评论提交通过 `src/auth/session.js` 校验共享会话，再为该请求显式发送 `Authorization: Bearer <token>`。
- 密码保护文章的 `blog{id}` token 只用于文章和评论列表读取，保持原格式，不与登录 JWT 混用。
- 受保护请求的 JWT 和权限校验由训练中心及 Blog API 负责。
- 不在本模块中引入训练中心组件、后端业务代码或部署配置。

## 文件职责

- `src/main.js`：使用 `createApp` 注册 Vue 插件、全局样式和应用入口。
- `src/router/index.js`：声明公开 Blog 路由、训练外壳路由，并把旧 `/login` 替换跳转到 `/training/login`。
- `src/plugins/axios.js`：创建同源 Blog API 客户端并维护评论访客标识。
- `src/auth/session.js`：成对校验并读取共享用户摘要或裸 JWT；清理时发送稳定的同页 `custacm:session-change` 事件，同时移除旧 `memberToken/memberUser`，但保留评论 identification 与密码文章 token。
- `src/auth/account-menu.js`：按当前角色生成账号菜单；管理员比普通队员多“管理员界面”入口。
- `src/components/index/Nav.vue`：使用 `public/img/custacm-wordmark.png` 渲染固定的藏青色 CUSTACM 字标、精简 Blog 导航和训练中心入口；友人帐页面保留直链但不占用顶栏；登录后账号区统一提供“个人资料”、管理员可见的“管理员界面”和“退出登录”，响应同页 session-change 与跨标签页 storage 变化，并在 1280px 桌面宽度内保持账号操作可达。
- `src/components/index/Footer.vue`：渲染平台欢迎语，以及带官网图标的圆角项目仓库、Codeforces、AtCoder、洛谷、牛客竞赛和 QOJ 固定链接。
- `src/components/index/Header.vue`：从公开首页图片接口读取一至两张有序横幅，保留鼠标左右移动时的相邻图片渐变切换；首屏通过 Google Fonts 加载 Bowlby One SC，以冷调象牙白填充和黑色描边在画面上方呈现双行 `WELCOME TO CUSTACM`，并保留原逐字浮动、品牌浮动和整组淡入动画；接口失败时使用 `src/settings.js` 回退图。
- `public/img/welcome-to-wordmark.svg`：保留旧版 Ethnocentric Rg 静态路径字标作为历史设计资源，当前首页不再引用。
- `src/util/homepageBanner.js`：把鼠标位置映射为任意数量横幅的相邻图层透明度。
- `src/assets/css/base.css`：提供 Blog 全局基础样式，以共享的 `#f4f6f8` 雾灰画布与训练、管理页面保持底色一致；前端不再加载播放器或歌词组件。
- `src/components/sidebar/Introduction.vue`：显示当前登录用户的正方形头像、nickname、username、个性签名和有序友情链接；新增资料保持纯展示，在个人页仍可通过原有头像交互打开裁剪器。
- `src/components/profile/AvatarCropDialog.vue`：允许拖动、缩放本地 PNG/JPEG，并导出 512×512 PNG 交给头像 API。
- `src/views/about/About.vue`：个人资料页，展示当前用户的 Codeforces/AtCoder handle、身份与签名；右侧统一“编辑资料”面板修改 nickname、签名和最多八条可排序友情链接。
- `src/api/profile.js`：读取本人完整资料/OJ handle，并只为本人 nickname、签名、友情链接与头像更新请求显式附加共享 Bearer JWT。
- `src/components/comment/Comment.vue`：以响应式状态展示登录用户的评论入口；监听并清理同页 session-change 和跨标签页 storage 事件。
- `src/components/comment/CommentForm.vue`：提交时重新读取共享 JWT，保留既有未登录和失败提示。
- `src/api/comment.js`：区分密码文章 token 与登录 JWT，后者仅在评论提交请求中显式使用 Bearer 格式。
- `src/store/actions.js`：编排评论状态；仅受保护评论提交的 401 会清理共享会话并替换跳转训练登录页，403 与网络错误保留既有通用提示。
- `src/views/Index.vue`：组合站点外壳；训练路由保留唯一 `Nav.vue` 并隐藏 Blog 侧栏；普通训练查询页沿用 Blog 页脚，管理员页面隐藏页脚。
- `src/views/training/TrainingHost.vue`：在 Blog 顶栏下同源嵌入训练运行时，并将内部训练路由同步到公开 `/training/**` URL。
- `vite.config.js`：配置 Vue 编译、源码别名、4180 统一开发入口、训练应用/HMR 与 `/api` 代理，以及 Vitest 环境。
- `src/test/session.test.js`：验证共享登录键、孤儿会话清理和稳定变更事件。
- `src/test/accountMenu.test.js`：验证普通队员和管理员的账号菜单权限差异。
- `src/test/dateTimeFormatUtils.test.js`：验证移除 Vue 2 filter 后的日期格式契约。
- `src/test/homepageBanner.test.js`：验证单图、首尾定位与相邻图片交叉淡入淡出。
- `src/test/profileApi.test.js`：验证本人资料和友情链接请求使用正确路径、方法与显式 Bearer header。
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
