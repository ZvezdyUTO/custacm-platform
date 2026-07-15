# Frontend Design System

本文定义 custacm-platform 两份 Vue 3 前端共享的视觉语言。它约束设计 token、日间/夜间语义、排版、圆角、动效、玻璃表面和验收方式，不改变两套 Router、认证、API 或部署边界。

当前状态：阶段 4 双端验收进行中。Training 与 Blog 均已接入共享 token；自动验收已通过，等待关键视口和品牌色人工确认。

讨论原型：[frontend-redesign-prototype.html](frontend-redesign-prototype.html)。该文件只消费共享 token、使用示例数据，不进入任何生产构建。

## 设计原则

1. 内容优先：装饰不能压过文章、训练数据和管理操作。
2. 统一角色：两端使用相同的日间/夜间语义 token；日间沿用首页/赛事荣誉浅色体系，夜间以文章目录暖黑色板为基准。
3. 信息密度分级：Blog 阅读页面可以舒展，训练表格和管理界面保持紧凑。
4. 克制使用玻璃：首轮只允许顶栏、下拉菜单和 Modal/Dialog 使用。
5. 可访问性是完成条件：键盘、焦点、对比度和 reduced-motion 与视觉效果同时验收。

## Token 来源与同步

唯一事实来源是：

```text
frontend-design-tokens/tokens.css
```

两份前端使用生成副本：

```text
frontend/src/styles/tokens.css
platform-blog/upstream/nblog/blog-view/src/assets/css/tokens.css
```

更新源文件后运行：

```bash
./scripts/sync-design-tokens.sh
./scripts/sync-design-tokens.sh --check
```

生成文件带有禁止手工编辑的声明。`--check` 在临时目录生成“声明 + 源文件”的期望内容后比较两份副本，任何漂移都会返回非零状态。

## 颜色角色

| Token | 值 | 用途 |
| --- | --- | --- |
| `--color-canvas` | `#ffffff` | 页面底色 |
| `--color-canvas-alternate` | `#f5f5f7` | 交替分区 |
| `--color-surface` | `#ffffff` | 实体内容表面 |
| `--color-text` | `#1d1d1f` | 主文字 |
| `--color-text-muted` | `#606066` | 次要文字 |
| `--color-action` | `#0066cc` | 主交互 |
| `--color-action-hover` | `#0071e3` | 主交互 hover |
| `--color-focus-ring` | `#0071e3` | 键盘焦点环 |

成功、警告、危险使用独立语义 token，不从主交互色推导。用户自定义分类色、文章图片、头像和横幅也不被主交互色覆盖。

品牌色在阶段 4 通过对比截图确认。确认前不得在业务组件中散落新的绝对色值。

## 排版

默认字体栈以 PingFang SC 和系统字体为主，不下载外部字体。

| 场景 | 字号 | 说明 |
| --- | --- | --- |
| Blog 文章正文 | 17px | 配合适宜行高，服务长文阅读 |
| 普通界面正文 | 15–16px | 表单、说明、面板文案 |
| 表格、标签、辅助信息 | 13–14px | 保持训练和管理界面的信息密度 |

标题默认使用 600 字重。负字距只允许用于明确的拉丁或数字展示标题，中文标题不套用 Apple 分析中的负字距和 300 字重。

## 间距与圆角

间距采用 4、8、12、16、24、32、48、80px 阶梯。

| Token | 值 | 用途 |
| --- | --- | --- |
| `--radius-control` | 8px | 输入框、普通按钮、表格内控件 |
| `--radius-card` | 11px | 普通卡片和面板 |
| `--radius-card-large` | 18px | 大型展示卡片 |
| `--radius-pill` | 9999px | CTA、搜索框、筛选胶囊 |

药丸形不能用于所有后台输入框和表格控件。界面层级优先通过留白和实体表面表达，不依赖大量边框、阴影或圆角嵌套。

## 玻璃与表面

首轮允许玻璃的位置：

- Blog 唯一顶栏（日间使用共享浅色玻璃，夜间使用同模糊强度的暗色玻璃）和 Training 独立开发顶栏；
- 下拉菜单；
- Modal/Dialog。

内容卡片、表格、表单、普通分页和占据布局空间的 sticky sidebar 使用实体表面。同屏可见玻璃层不超过三层；不支持 `backdrop-filter` 时必须回退到可读的实色或高不透明度表面。

项目只定义一档悬浮阴影 `--floating-shadow`。实体卡片不得为了“层次感”继续堆叠新阴影。

## 动效

| Token | 值 | 用途 |
| --- | --- | --- |
| `--duration-fast` | 150ms | hover、焦点和轻量反馈 |
| `--duration-medium` | 260ms | 路由切换等中等时长反馈 |
| `--duration-slow` | 400ms | 大型面板或路由进入 |
| `--ease-standard` | `cubic-bezier(0.32, 0.72, 0, 1)` | 标准缓动 |

只有主要 CTA 和尺寸足够的按钮可以在按压时缩放到 `0.97`。小型表格按钮、纯图标按钮和分页按钮不缩放。路由动画最多使用淡入和 8px 位移，不能延迟页面可操作时间。

`prefers-reduced-motion: reduce` 下三个时长 token 都变为 `0ms`；新增动画不得绕过这些 token 私自保留时长。

## 可访问性

- 所有原生和自定义交互控件必须保留可见 `focus-visible`。
- 弹层打开后焦点进入弹层，在弹层内循环；Esc 可关闭；关闭后焦点归还触发控件。
- 需要逐项检查 `AdminConfirmDialog`、`AvatarCropDialog`、文章首图裁剪、`ManagedImageViewer` 和评论表情选择器。
- 正文、控件文字、语义状态和玻璃表面文字达到 WCAG AA。
- 不能只用颜色表示成功、危险、选中或禁用状态。

## 页面与状态基线

截图采用“页面 × 适用状态”，不生成无意义的完整组合；不适用项标记 N/A。

首轮核心基线：

| 页面 | 必须覆盖的状态 |
| --- | --- |
| Blog 首页 | 日间/夜间、桌面、代表性移动端导航 |
| 文章目录 | 日间/夜间、桌面、代表性移动端导航 |
| 文章详情 | 正常、请求失败、登录/未登录中适用的状态 |
| 登录页 | 默认、错误、冷却倒计时 |
| 训练查询 | 正常、空数据、加载、失败 |
| 管理员表格页 | 正常、空数据、Modal 打开 |

其余分类/标签、个人主页、写作页和管理面板按实际改动补充。截图基线不得包含真实密码、token 或不适合进入仓库的个人敏感数据。

## 构建体积基线

体积口径是生产构建产物中所有 `.js` 与 `.css` 文件的 gzip 字节总量，不含 source map、图片和字体。阶段 1 记录两端基线；任一端在一个 PR 中增长超过 10% 时，PR 必须解释原因并获得确认。

| 构建 | JS/CSS gzip 总量 | 记录日期 |
| --- | ---: | --- |
| Training | 103,596 bytes（约 101.2 KiB，阶段 1 改版前基线） | 2026-07-14 |
| Blog | 1,247,915 bytes（约 1,218.7 KiB） | 2026-07-14 |
| Training 阶段 2 | 108,548 bytes（约 106.0 KiB，较基线 +4.8%） | 2026-07-14 |
| Blog 阶段 3 | 1,220,880 bytes（约 1,192.3 KiB，较基线 -2.2%） | 2026-07-14 |
| Training 阶段 4 登录页精修 | 110,575 bytes（约 108.0 KiB，较基线 +6.7%） | 2026-07-14 |
| Blog 阶段 6 最终收尾 | 1,121,586 bytes（约 1,095.3 KiB，较基线 -10.1%） | 2026-07-14 |

基线使用当前生产构建的实际 `.js`/`.css` 文件逐个 `gzip -c` 后汇总字节数。Blog 阶段 6 体积包含 Element Plus、Lucide、编辑器和公式渲染依赖，已不包含 Semantic UI CSS；按同一口径较阶段 3 再下降 99,294 bytes（8.1%）。

## 分辨率与完成条件

- Training：1280px 不出现非设计性的页面级横向溢出；重点检查 1440×900、1920×1080，2560px 不破版。
- Blog：同样检查桌面范围，并对现有移动端导航、侧栏隐藏和正文布局做代表性回归。
- 共享 token 改动验证两端完整生产构建；单端组件改动验证受影响端。
- PR 前运行 `./scripts/sync-design-tokens.sh --check` 和 `./scripts/check-doc-sync.sh origin/main WORKTREE`。

## 后续决策记录

### 2026-07-15 恢复全站双主题

- Blog 唯一顶栏账户入口左侧提供紧凑太阳/月亮开关；首次访问固定日间，用户选择写入 `custacm.theme` 并跨标签页、同源 Training frame 同步，不跟随系统主题。
- 日间沿用首页/赛事荣誉的米白浅色体系；Training 明确使用 `#faf9f5` 暖米色主画布、`#f0eee6` 交替画布和 `#f7f7f5` 浅奶油实体表面，不保留纯白工作区。夜间统一采用文章目录的 `#141413` 暖黑画布、`#faf9f5` 米白文字和 `#d97757` 陶土橙强调色。
- Training 加载 `light.css` 与 `dark.css`，Blog 加载 `night.css` 与 Element Plus 暗色变量；两份 HTML 在 Vue 挂载前应用已保存主题以避免首屏闪色。
- 业务图片、头像、首图和赛事照片不因全局主题统一滤色；只调整其容器、边框和周边 UI。
- Training 首页编排中的精选文章卡片直接复用公开首页的卡片、悬停、媒体、边框、正文和次要文字色值；比赛管理表头及其专用交互变量在夜间映射到同一暖黑、米白与陶土橙语义，不保留浅蓝表头。

### 阶段 2 实现记录

- Training 从 `src/styles.css` 首行加载生成 token，并以 `training-redesign.css` 集中承载试点覆盖。
- 独立开发顶栏、下拉菜单和 `AdminConfirmDialog` 使用玻璃；表格、卡片、表单和侧面板保持实体表面。
- 管理区原酒红交互变量在试点层映射到统一 `--color-action`，业务成功/警告/危险色保持独立。
- Training 路由切换使用 8px 淡入位移和共享动效 token；reduced-motion 时 token 自动归零。
- `AdminConfirmDialog` 支持焦点进入、Tab/Shift+Tab 循环、Esc 取消和关闭后焦点归还。
- 核心前景/背景组合的静态 WCAG 对比度为：Action/白 5.57:1、正文/白 16.83:1、次要文字/白 6.25:1。
- 1280×720 独立登录页浏览器实测无横向溢出；顶栏计算样式为 72% 白色玻璃、20px blur/180% saturate，登录卡圆角 18px。1440×900、1920×1080 与需要认证的业务页仍留待统一环境双端验收。

### 阶段 3 实现记录

- Blog 从 `main.js` 在基础样式前加载生成 token，以 `blog-redesign.css` 承载日间基线并由 `night.css` 覆盖夜间语义和 Element Plus 变量。
- Blog 唯一顶栏、下拉/搜索/表情浮层和裁剪 Dialog 使用玻璃；文章卡片、评论、写作页、个人页、表单和侧栏保持实体表面。
- 主操作使用 Action Blue；分类、标签、成功、警告和危险业务语义不被覆盖。
- `AvatarCropDialog`、文章首图裁剪、`ManagedImageViewer` 与评论表情选择器支持焦点进入或归位、Tab/Shift+Tab 循环、Esc 关闭；正文托管缩略图可用 Enter/空格打开预览并在关闭后归还焦点，保存或加载期间仍阻止不安全关闭。
- Semantic UI class 和图标依赖未在本阶段清理，继续留给阶段 6。
- Blog 阶段 3 通过 36 个测试文件共 114 项测试和生产构建；JS/CSS gzip 总量较阶段 1 基线下降 2.2%。完整桌面、移动端和双端 iframe 截图验收留待阶段 4。

### 阶段 4 验收记录

- 统一 Blog 顶栏保持不变；Training 登录区域按账户中心式布局改为大留白、184px CUST ACM 圆形徽章、大标题、380px 宽且 46px 高的用户名/密码输入和 40px 圆形箭头提交按钮；登录按钮固定使用徽章金 `#C9962E`，hover/focus 使用 `#A87A1F`，箭头采用深墨色以保持清晰。表单与标题之间保留 52px 留白，使账号密码区域进一步下移。徽章使用项目内置 `public/img/custacm-training-logo.jpg`，保留轻微自然投影和边框。服务端冷却与错误状态保持可见。登录页自身固定为 iframe 视口高度并作为内部纵向滚动容器，提供额外 220px 内容区；鼠标滚轮、触控板和触摸滚动保持可用但隐藏滚动条，且滚动不会继续传递给 Blog 外壳。徽章从顶部原尺寸随内部滚动缩至最低 72%，回到顶部恢复，离开登录页后恢复默认滚动条；`prefers-reduced-motion` 下固定原尺寸。
- Blog `/`、`/training/login` 与经 Blog 代理的 `/training-app/login` 均返回 200，双 Vite 开发链路可用；本机 Blog API 8090 和 Docker daemon 未运行，因此不声称已验收登录后真实业务数据。
- 主题链路按 2026-07-15 双主题决策恢复；当前测试约束默认日间、持久化、跨标签页与 frame 同步、无系统主题跟随、按钮无障碍状态及图片不统一滤色。
- 玻璃表面按实际 alpha 与相邻画布混合后的静态 WCAG 对比度为：正文 16.41:1、次要文字 6.09:1，主按钮 5.57:1。
- 侧栏与分页确认继续使用实体表面：它们占据布局空间且承载连续阅读/导航，不属于悬浮层；本阶段不新增可选玻璃精修。
- 日间品牌操作色使用 Action Blue `#0066cc`，夜间全站操作色使用文章目录陶土橙 `#d97757`；业务成功、警告、危险和 rating 色阶继续保持独立语义。
- Blog 的 Training host 使用 border-box 在 `100vh` 内包含 51px 顶栏预留，且登录路由不再在 iframe 外追加 Footer，修复外层 `100vh + 51px` 与 Footer 共同产生的蓝色滚动条；原 Blog Footer 的项目与竞赛平台链接在 Training iframe 内由 `LoginFooter.vue` 复现，和登录内容共用 `.training-site.is-login-page` 内部滚动容器，因此底部内容无需借助 Blog 外层滚动即可到达。其它 Training 页面如仍需要外层滚动，thumb 固定使用淡灰色圆头。
- 2026-07-15 已在应用内浏览器以 1440×900 检查首页、文章目录、赛事荣誉和 Training 登录 frame 的日间/夜间切换、按钮状态与 frame 实时同步；1920×1080、代表性移动端导航以及登录后的查询/管理员弹层仍需在可用账号和完整数据环境继续验收。

### 阶段 5 组件库评估

结论：**不启动组件库迁移**。Blog 继续使用 Element Plus，Training 继续使用原生 Vue 组件；共享 token 和交互规范负责统一视觉。阶段 6 只清理 Semantic UI，并把 Blog 图标统一到 Lucide，不把组件库迁移夹带进依赖退出。

#### 仓库证据

- Blog 当前固定 `element-plus@2.14.3`，在 `main.js` 全量注册并导入完整 CSS；生产源码中有 11 个文件直接使用 Element Plus，覆盖按钮、输入、表单、分页、下拉、Popover、Divider 与 Backtop 等 11 类标签。现有阶段 3 语义变量和交互测试均建立在这套实现上。
- Blog 的 Semantic UI 仍涉及 18 个 Vue 文件，另有 `badge.css` 和 `main.js` 的全局 CSS 导入；这是一项明确的遗留依赖退出任务，但不构成替换 Element Plus 的理由。
- Training 没有组件库，13 个 Vue 文件共使用 66 个原生按钮、34 个输入框、7 个 select、6 个 table 和 1 个 textarea。现有焦点、确认框、表格滚动和业务状态都有项目内测试；全量替换会扩大回归面，并增加 Training 构建体积。
- 两端 Router、构建产物和运行边界按架构要求保持独立；即使使用同一组件库，也不会共享运行时组件实例。当前共享 token 已经实现视觉统一，因此“同库”不是“同视觉”的必要条件。

#### 候选比较

| 路线 | 收益 | 成本与风险 | 结论 |
| --- | --- | --- | --- |
| Blog Element Plus + Training 原生 | 保留已验证行为和当前体积，只维护一套共享 token | 两端控件实现不同，需要继续用契约测试约束视觉 | **采用** |
| 两端统一 Element Plus | Training 可直接获得复杂表格、表单与弹层组件 | 需要迁移 114 个原生控件并重做焦点、表格、筛选和测试；当前复杂度不足以回收成本 | 不启动 |
| 两端统一 ant-design-vue | 企业后台组件覆盖完整 | Blog 需同时迁出 Element Plus 和 Semantic UI，Training 也需全量迁移，会在过渡期引入第三套体系 | 拒绝 |
| 两端全部原生 | 最低第三方 UI 依赖 | 需重写 Blog 已稳定的分页、下拉、Popover、表单和消息能力，可访问性与维护成本倒退 | 拒绝 |

#### 官方能力与维护信号

- Element Plus 官方文档说明其样式系统支持 CSS 变量和按 class 范围覆盖，和项目当前日间/夜间 token 兼容：[Theming](https://element-plus.org/en-US/guide/theming)。评估日官方仓库显示最新发布为 2.14.3（2026-07-10），与项目锁定版本一致：[Element Plus releases](https://github.com/element-plus/element-plus/releases)。
- ant-design-vue 官方仓库确认其面向 Vue 的企业桌面组件定位，但评估日 Releases 显示最新稳定版仍为 4.2.6（2024-11-11）。它具备候选能力，但没有足以抵消双端重迁成本的项目内优势：[ant-design-vue](https://github.com/vueComponent/ant-design-vue)、[releases](https://github.com/vueComponent/ant-design-vue/releases)。

#### 重新评估触发条件

只有出现以下任一情况，才重新开启组件库迁移评估，并先做不进主线的隔离原型：

1. Training 连续新增至少三类复杂控件，例如虚拟化数据表、日期范围、树选择或多步骤表单，原生实现开始重复维护键盘和弹层行为；
2. Element Plus 无法继续支持项目锁定的 Vue/Vite 或安全维护要求；
3. Semantic UI 退出后，实测证明统一组件库可减少至少 20% 的重复 UI 代码，且两端 gzip 增长均不超过 10%；
4. 项目产品范围改变，要求两端共享同一组件源码包，而不仅是共享视觉 token。

### 阶段 6 Semantic UI 退出记录

- 审计覆盖 18 个生产 Vue 文件；15 个模板中的 `ui` 布局 class 全部改为项目自有的导航、网格、内容表面、侧栏、评论和按钮 class，并改用这些稳定选择器。
- 39 个旧 `<i>` 字体图标统一替换为全局 `AppIcon`。该组件集中维护语义名称到 `@lucide/vue` 的映射、尺寸、旋转加载态和减少动态效果降级，业务模板不直接依赖具体 Lucide 组件名。
- 删除 `semantic-ui-css` 依赖、全局 Semantic CSS、旧 Ali 图标 CSS 入口及其字体资产；Element Plus 保持不变，没有夹带组件库迁移。
- 新增退出契约测试，阻止生产 Vue/JS 重新出现 Semantic UI class、旧 `<i>` 图标、全局 Semantic 导入或依赖声明；最终视觉收尾又覆盖搜索胶囊单一圆角表面、顶栏 Tab 不绘制蓝色矩形轮廓以及浅灰圆头页面滚动条。Blog 共 37 个测试文件、119 项测试及生产构建通过。
- Blog 生产 JS/CSS gzip 总量为 1,121,586 bytes，较阶段 1 基线下降 126,329 bytes（10.1%），较阶段 3 下降 99,294 bytes（8.1%）。Training 最终 lint、17 个测试文件共 107 项测试、类型检查和生产构建通过，体积保持 110,575 bytes（较基线 +6.7%）。应用内浏览器运行时连接冲突，未把 1440×900、1920×1080 和代表性移动端截图虚报为已自动验收。
