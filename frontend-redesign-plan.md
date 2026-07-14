# custacm-platform 前端改版方案计划

> 状态：v3（两轮评审意见已合入），阶段 1–6 与最终自动化收尾完成
> 日期：2026-07-14
> 参考：`apple/DESIGN.md`、仓库根 `AGENTS.md`、`platform-blog/AGENTS.md`、`frontend/AGENTS.md`、`scripts/AGENTS.md`

## 核心原则

“统一视觉”和“统一组件库”是两件事。本次改版只承诺统一视觉：统一 token、有限玻璃、现代圆角和克制动效；组件库迁移在视觉验证后单独评估。

执行路线：

```text
设计 token 与页面原型
        ↓
训练端纯视觉试点
        ↓
Blog 保留 Element Plus 完成视觉统一
        ↓
双端验收并确认品牌色
        ↓
单独评估组件库
        ↓
Semantic UI 分阶段退出
```

## 现状与约束

- `platform-blog/upstream/nblog/blog-view` 是 `/` 下的 Vue 3 Blog，持有唯一 `Nav.vue`，使用 Element Plus 与 Lucide；遗留 Semantic UI 已在阶段 6 退出。
- `frontend/` 是 `/training/**` 内嵌的 Vue 3 + TypeScript 训练工作区，无组件库，图标使用 Lucide。
- 两套 Router 保持独立；2026-07-15 起全局视觉固定为浅色，不使用 localStorage 或同源 frame 消息同步视觉模式。
- 文章目录、黑色页脚等固定深色区域继续作为页面级设计保留；`prefers-reduced-motion` 下动画立即降级。
- 训练端正式验收 1280–2560px，重点 1440×900 与 1920×1080；Blog 还要做现有移动端行为基本回归。

## 关键决策

### 1. 本期不迁移组件库

- 训练端继续使用原生组件。
- Blog 保留 Element Plus，通过 `--el-*` 变量适配统一视觉。
- 如需验证 ant-design-vue，只做不进主线的隔离原型。
- 视觉验收后再决定统一到 Element Plus、ant-design-vue 或维持现状。

### 2. 首轮玻璃只用于真正悬浮的层

首轮仅用于 Blog `Nav.vue`、训练端 `AppShell` 顶栏、下拉菜单和 Modal/Dialog。内容卡片、表格、表单、占据网格空间的 sticky sidebar 和普通分页保持实体表面。

玻璃 token 提供固定浅色表面、`backdrop-filter` 与实色回退，同屏不超过三层。阶段 4 只决定侧栏或分页是否值得使用玻璃；若决定采用，在主线验收后另开可选精修 PR，不在验收阶段临时改代码。

### 3. 统一交互语义

统一使用 `--color-action`、`--color-action-hover`、`--color-focus-ring` 等语义 token。全局固定使用 Action Blue；固定深色内容页使用页面自己的局部对比配色，不引入根节点模式分支。

### 4. 图标最终统一到 Lucide

Semantic UI 退出阶段先建立图标映射，再替换 Blog 图标并移除依赖；不和视觉主线混在同一个 PR。

### 5. token 使用单一源文件和仓库级校验

- 唯一源文件：`frontend-design-tokens/tokens.css`。
- `scripts/sync-design-tokens.sh` 默认将“生成声明 + 源文件内容”同步到两端本地副本。
- `--check` 在临时目录生成同样的期望文件后比较，禁止直接拿带生成声明的副本与源文件做原始比较。
- 两份生成文件入库，保证 Vite 开发和当前 Docker 构建上下文无需变化。
- 新顶层目录同步登记到 `docs/agent/context-map.md` 和 `docs/doc-sync-map.tsv`。

### 6. 项目规范文档归入 docs

`apple/DESIGN.md` 与根 `DESIGN.md` 保持参考资料职责；本项目可执行规范写入 `docs/frontend-design-system.md`。

## 设计规范摘要

| 项目 | 规范 |
| --- | --- |
| 圆角 | 控件 8px、卡片 11px、大卡片 18px；药丸仅用于 CTA、搜索框和筛选胶囊 |
| 字号 | Blog 正文 17px；普通界面 15–16px；表格、标签、辅助信息 13–14px |
| 动效 | 150/260/400ms；主要 CTA 可按压至 `scale(.97)`；小按钮、图标按钮和分页不缩放 |
| 玻璃 | 仅首轮批准的悬浮层；支持实色回退与 reduced-motion |
| 表面 | 白与 parchment 交替；实体内容表面不滥用玻璃 |
| 字体 | 中文优先 PingFang SC；标题 600；CJK 不套用拉丁负字距 |

## 分阶段计划

### 阶段 1：设计 token 与页面原型（1–1.5 天）

- 编写 `docs/frontend-design-system.md`。
- 建立 token 源文件、两端生成物、同步脚本与 `--check`。
- 记录两端 JS/CSS gzip 基线。
- 建立适用状态的截图清单并产出关键页面对比原型。
- 同步文档索引、context map、doc-sync map、模块 README 和 CHANGELOG。

### 阶段 2：训练端纯视觉试点（2–3 天）

- 不引入组件库；接入 token，调整圆角、间距、分级字号和强调色。
- `AppShell` 顶栏使用玻璃，侧面板保持实体表面。
- 增加路由/面板克制动效。
- `AdminConfirmDialog` 补焦点进入、循环、Esc 关闭和关闭后焦点归还。

实现状态：已完成代码、聚焦测试与生产构建；后续已收敛为固定浅色 Action Blue。

### 阶段 3：Blog 保留 Element Plus 完成视觉统一（3–4 天）

- 接入 token 并覆盖必要的 Element Plus 变量。
- 调整 Nav、文章卡片、评论、写作页和首页表面节奏。
- 检查 `AvatarCropDialog`、文章首图裁剪、`ManagedImageViewer` 和评论表情选择器的完整焦点行为。
- 本阶段不清理 Semantic UI class 或图标。

实现状态：已完成共享 token、Element Plus 变量、导航/浮层/实体内容表面与关键页面节奏接入；头像裁剪、文章首图裁剪、图片预览和评论表情选择器已补齐焦点行为。Semantic UI class 和图标保持不变。

### 阶段 4：双端验收与品牌色确认（1 天）

- 并排检查共享浅色 token、固定深色内容页、玻璃、圆角与动效。
- 对照页面和适用状态截图。
- 确认交互色和侧栏/分页玻璃决策；后者如采用，另开精修 PR。

完成状态：双端服务、样式契约、对比度及构建体积自动验收已通过；侧栏与分页确认继续使用实体表面。全局夜间模式后来于 2026-07-15 退役，文章目录等固定深色页面不受影响。应用内浏览器连接异常且本机后端/Docker 未运行，1440×900、1920×1080、移动端导航及登录后业务页面的人工截图仍作为后续回归清单保留，不虚报为已完成。

### 阶段 5：组件库评估（0.5–1 天）

结论写入设计规范附录。未评估通过前不启动迁移。

完成状态：已盘点两端依赖和控件使用，比较维持现状、统一 Element Plus、统一 ant-design-vue 与全部原生四条路线。结论为不启动组件库迁移：Blog 保留 Element Plus，Training 保留原生控件，两端继续依靠共享 token 统一视觉；阶段 6 只退出 Semantic UI 并统一 Lucide 图标。详细证据、决策矩阵和重新评估触发条件见 `docs/frontend-design-system.md` 的阶段 5 记录。

### 阶段 6：Semantic UI 分阶段退出（2–4 天）

1. 清理当前盘点到的 18 个 Vue 文件中的 Semantic UI 布局 class，并删除残留样式引用。
2. 按映射将 Semantic 图标替换为 Lucide。
3. 删除 `semantic-ui-css` 并全站回归。
4. Element Plus 如需迁移，另立计划。

完成状态：已审计 18 个生产 Vue 文件，清理其中 15 个模板的 Semantic UI 布局 class，并将 39 个旧 `<i>` 字体图标替换为全局 `AppIcon` + Lucide 映射；已删除 `semantic-ui-css`、旧 Ali 图标样式入口及字体资产。最终收尾包含搜索胶囊、顶栏 Tab 轮廓与页面滚动条精修；Blog 通过 37 个测试文件共 119 项测试和生产构建，JS/CSS gzip 总量为 1,121,586 bytes，较阶段 1 基线下降 10.1%。Training 通过 lint、17 个测试文件共 107 项测试、类型检查和生产构建。应用内浏览器运行时连接冲突，因此桌面与代表性移动端截图仍保留为人工回归项。

## 验收标准

1. 受影响的前端执行各自完整测试与生产构建；共享 token 变更同时验证两端。
2. 键盘导航、`focus-visible`、弹层焦点进入/循环/Esc/归还满足要求。
3. 正文、按钮、语义色及玻璃表面文字通过 WCAG AA。
4. 检查 1280、1440×900、1920×1080、2560；Blog 额外检查代表性移动端视口。
5. 截图按“页面 × 适用状态”记录，不做无意义的完整笛卡尔积；不适用标为 N/A。核心基线包括固定浅色首页、固定深色文章目录、文章正常/失败、登录、训练正常/空/失败、一个管理员表格、一个打开的 Modal 和移动端导航。
6. 以构建产物 JS/CSS gzip 总量为体积口径；任一端增长超过 10% 必须在 PR 中解释并确认。
7. `prefers-reduced-motion` 下所有新动画立即降级；长表格无肉眼可见滚动劣化。
8. PR 前运行 `./scripts/check-doc-sync.sh origin/main WORKTREE` 与 `./scripts/sync-design-tokens.sh --check`。

## 工期

| 范围 | 估算 |
| --- | --- |
| 视觉改版主线（阶段 1–4） | 7–10 个工作日 |
| 加上 Semantic UI 退出 | 10–15 个工作日，另留回归缓冲 |
| 组件库迁移 | 若评估通过再单独估算 |

计划按 5–7 个 PR 分批交付；每阶段可独立验收和停止。
