# 前端设计规范

本规范约束 Vue Blog 与 Vue Training 的共享视觉语言，不改变两套 Router、认证或部署边界。

## 单一 token 来源

```text
frontend-design-tokens/tokens.css
```

生成副本：

```text
frontend/src/styles/tokens.css
platform-blog/upstream/nblog/blog-view/src/assets/css/tokens.css
```

不要手工编辑副本。修改源文件后运行：

```bash
./scripts/sync-design-tokens.sh
./scripts/sync-design-tokens.sh --check
```

## 当前决策

- 全局支持手动日间/夜间主题；首次访问固定日间，用户选择写入 `custacm.theme`，跨标签页和同源 Training frame 同步，但不跟随系统主题。
- 日间沿用首页与赛事荣誉的暖米色体系；夜间采用文章目录的暖黑、米白与陶土橙体系。
- Blog 保留 Element Plus 并使用 Lucide；Training 使用原生 Vue 控件和 Lucide。Semantic UI 已退出。
- 两端共享视觉 token，不共享 Router 或运行时组件。

## 使用规则

- 内容优先。Blog 阅读区可以舒展，训练表格和管理界面保持紧凑。
- 使用语义角色，如 `--color-canvas`、`--color-surface`、`--color-text`、`--color-action`、`--color-success` 和 `--color-danger`；不要在业务组件复制一套全局颜色。
- 间距优先使用 4/8/12/16/24/32/48/80px 阶梯。控件、普通卡片和大展示卡片使用各自 radius token；药丸只用于 CTA、搜索和筛选。
- 玻璃只用于顶栏、下拉和 Modal/Dialog 等悬浮层，且必须有不支持 `backdrop-filter` 时的实色回退。内容卡片、表格、表单和布局侧栏使用实体表面。
- 动效使用共享 duration/easing token；只做轻量反馈。`prefers-reduced-motion: reduce` 下必须立即降级。
- 用户定义的分类色、标签色、业务图片和 rating 色不被全局主题覆盖。

## 可访问性

- 所有交互控件保留可见 `focus-visible`，不能只用颜色表达状态。
- Dialog 打开后焦点进入并在内部循环，Esc 可关闭，关闭后归还触发控件。
- 正文、控件文字、状态和玻璃表面达到 WCAG AA。
- 图片和动效提供键盘操作与 reduced-motion 降级；不把真实凭据或敏感数据放入截图夹具。

## 验收

- Training 支持 1280–2560px 桌面，重点检查 1440×900 与 1920×1080；Blog 同时回归现有移动导航和文章单列布局。
- 共享 token 变化必须检查两份生成副本并构建两端；单端组件变化只验证受影响端。
- 若要重新评估组件库，先证明现有原生控件已形成重复的复杂交互成本，并在隔离原型中验证体积、焦点和回归影响。
