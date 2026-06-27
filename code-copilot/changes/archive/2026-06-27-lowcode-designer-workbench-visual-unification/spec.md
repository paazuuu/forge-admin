# 低代码设计器工作台视觉统一
> status: done
> created: 2026-06-23
> complexity: 🟡中等

## 1. 背景与目标

`copyPage/src` 提供了一套更接近 Figma/低代码搭建器的工作台布局：顶部轻量工具栏、左右固定配置面板、中间页面切换条、点阵背景、居中白色页面和悬浮视图控制。当前 Forge 表单设计器和列表设计器虽然功能已经接近完成，但布局和视觉不统一：

- 表单设计器、列表设计器的左侧面板宽度、右侧面板宽度、顶部工具栏、页面切换条、画布背景和视图控制条分别维护。
- 表单设计器的缩放/视图控制是画布内绝对定位，滚动时存在被遮挡或找不到的问题。
- 列表设计器的 `list-page-switch`、`list-designer-head` 和页面配置行信息过多，占用中间工作区高度。
- 两个设计器按钮密度、分组样式、面板背景、选中态和滚动边界不一致，导致企业低代码产品感不稳定。

本次目标是只做布局、样式和展示交互统一，不改现有功能逻辑、数据协议、保存发布链路和拖拽判定逻辑。中间画布范围严格限定为：

1. 页面切换条。
2. 点阵背景。
3. 居中白色页面。
4. 悬浮视图控制。

可验证结果：

- 表单设计页和列表设计页进入后整体工作台风格一致。
- 中间画布看起来一致：上方页面切换条、浅色点阵底、居中白色页面、浮动视图控制。
- 列表设计 `list-page-switch` 不再占用过多高度，页面配置、参数和数据配置以更紧凑的面板/弹出层/折叠交互承载。
- 表单设计缩放/视图控制不再因为画布滚动和外层滚动而不可见。
- 保存、预览、发布、撤销重做、清空、拖拽、属性配置、页面切换功能行为保持不变。

## 2. 代码现状（Research Findings）

> 每个结论必须有代码出处（文件路径 + 类名/方法名）

### 2.1 参考页面

- `copyPage/src/app/layouts/RootLayout.tsx`
  - 使用 `Header + flex 三栏工作台`，整体 `h-screen bg-[#f8f9fa] overflow-hidden`。
  - 左右面板和中间画布共用一套浅灰背景、边框和固定高度布局。
- `copyPage/src/app/components/Header.tsx`
  - 顶部 48px 工具栏，左侧对象标识，中间工具胶囊，右侧保存/设置/预览/发布。
- `copyPage/src/app/components/LeftPanel.tsx`
  - 左侧 256px，搜索 + 分组 + 两列组件卡片。
- `copyPage/src/app/components/RightPanel.tsx`
  - 右侧 280px，属性/样式/交互三个 Tab，内容滚动。
- `copyPage/src/app/components/Canvas.tsx`
  - 中间区域使用页面切换条、点阵背景、居中白色页面和底部悬浮视图控制。
- `copyPage/src/app/components/canvas/PageDesignSwitcher.tsx`
  - 页面切换条采用三段式：左侧页面标题和简短信息，中间分段 Tab，右侧更多操作。

### 2.2 表单设计器现状

- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`
  - `<template>` 中外层仍有 `designer-section-head` 和 `form-builder-grid`，形成表单设计独有的头部和工作区外框。
  - `<style scoped>` 中 `.business-form-designer` 使用 `grid-template-columns: minmax(0, 1fr) 280px`，与列表设计器内部三栏不同。
  - `.form-builder-grid` 当前有 `padding: 8px`、`background: #f8fafc`，会包一层额外工作区。
- `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue`
  - `<template>` 已经是左侧 `ForgeFieldShelf`、中间 `ForgeFormCanvas`、右侧 `ForgePropertyPanel`。
  - `.forge-form-designer` 样式存在两段覆盖定义：先定义 `248px/336px`，后又覆盖成 `260px/350px`。
  - `.designer-toolbar` 是表单独有头部，按钮文字较多；`.designer-form-tabs-bar` 是表单切换条，但与列表 `list-page-switch` 样式不统一。
  - `handleCanvasDragStart()` 会拖拽时收起右侧面板，逻辑应保留。
- `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue`
  - `.forge-form-canvas` 当前是线性网格背景，不是参考页点阵背景。
  - `.canvas-stage-shell` 和 `.canvas-stage` 已经具备居中白色页面能力。
  - `.canvas-viewport-dock` 是 `position: absolute; bottom/right`，在双层滚动场景下容易脱离用户视线。
  - `handleCanvasWheel()`、`applyCanvasPreviewMode()`、`updateDesignWidth()`、`updateCanvasZoom()` 已经具备鼠标缩放和视口控制能力，逻辑不应重写。
- `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue`
  - 左侧已有搜索、组件库/字段资产 Tab、两列组件卡片。
  - 背景、分组标题、卡片 hover 和列表设计器 `palette-panel` 不完全一致。
- `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
  - 右侧属性功能丰富，顶部是自定义按钮组，主体是 `n-tabs + n-collapse`。
  - 可在不改配置逻辑的前提下统一标题、Tab、搜索、折叠项和表单控件密度。

### 2.3 列表设计器现状

- `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`
  - `.business-list-designer` 使用 `grid-template-rows: auto auto minmax(0, 1fr)`，顶部 `list-designer-head`、中间 `list-page-switch`、底部 `list-workspace`。
  - `.list-designer-head` 已经压缩到 40px，但仍与表单设计顶部工具栏样式不同。
  - `.list-page-switch` 同时承载页面切换、页面配置、参数配置、数据配置，导致高度过高。
  - `.page-config-row`、`.page-param-row`、`.page-data-row` 当前是铺开的卡片行，和参考页“切换条 + 更多操作”不一致。
  - `.list-workspace` 还有额外 `padding: 8px` 和滚动，和内部 `ListPageGridDesigner` 画布滚动叠加。
- `forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`
  - 顶层 `.list-grid-designer` 是三栏：`280px minmax(0, 1fr) 380px`。
  - `.canvas-panel` 已有点阵背景，但颜色和表单不统一。
  - `.canvas-grid` 是白色页面主体，和表单 `.canvas-stage` 语义类似。
  - `.canvas-viewport-dock` 当前在滚动区内 `position: sticky`，比表单稳定，但样式位置仍需统一。
  - `.canvas-toolbar` 仍显示 `1 个区块 · 20 行 × 12 列 · 1912px · 桌面 1920px / 58%` 这类中间信息，本轮应隐藏行列信息或移到非主展示。

## 3. 功能点

- [ ] 统一工作台框架
  - 输入：现有表单设计器和列表设计器页面结构。
  - 处理：调整模板层级和 scoped CSS，使两者拥有一致的顶部、左栏、中间、右栏、滚动边界和背景。
  - 输出：视觉一致的表单设计页和列表设计页。

- [ ] 统一页面切换条
  - 表单 `designer-form-tabs-bar` 和列表 `list-page-switch` 都对齐 `PageDesignSwitcher`：左侧标题/简短信息，中间分段页签，右侧更多操作。
  - 表单继续切换 `formAssets`，列表继续切换 list/create/edit/detail 等页面配置。
  - 列表页面配置、参数、数据配置从常驻铺开改为紧凑折叠或更多操作弹出，不改配置字段和更新函数。

- [ ] 统一中间画布四项视觉
  - 点阵背景：表单和列表使用同一套浅灰点阵底。
  - 居中白色页面：表单 `.canvas-stage` 和列表 `.canvas-grid` 视觉一致，半径、边框、阴影、留白一致。
  - 悬浮视图控制：表单和列表的视口/宽度/缩放控制使用同样位置、尺寸、图标按钮和弹出层样式。
  - 页面切换条和画布之间边界统一。

- [ ] 统一左侧面板
  - 表单 `ForgeFieldShelf` 和列表 `palette-panel` 的宽度、搜索框、分组标题、两列卡片、禁用态和折叠按钮统一。
  - 保留现有拖拽 MIME、点击追加、禁用原因和字段使用状态逻辑。

- [ ] 统一右侧属性面板
  - 表单 `ForgePropertyPanel` 和列表 `ListPageGridDesigner` 内右侧属性面板统一宽度、背景、标题区、Tab 样式、折叠卡片密度和滚动条。
  - 保留所有属性更新函数、选中逻辑、搜索和源码/交互入口。

- [ ] 按钮展示优化
  - 顶部只保留高频主操作。
  - 撤销/重做/源码/清空/重置/页面设置等次级动作进入图标按钮或更多菜单。
  - 图标按钮保留 `title`，不改变原点击回调。

## 4. 业务规则

- 不修改后端接口、数据库、运行态渲染协议。
- 不修改 `formDesignerSchema`、`pageSchema`、保存发布字段、接口路径和 payload 结构。
- 不修改拖拽容器接收规则、组件树插入规则、列表网格移动/缩放算法。
- 不新增 npm 依赖，不把参考页 React/Tailwind 代码直接迁入 Vue 项目。
- 中间画布只改页面切换条、点阵背景、居中白色页面、悬浮视图控制；画布内业务节点、字段节点、网格项渲染不做功能性改造。
- 可做展示交互：折叠、更多菜单、Popover、图标化按钮、右侧面板拖拽时自动收起、滚动边界优化。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 无 | - | - | 本次仅前端布局、样式和展示交互 |

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 无 | - | - | 不改变接口协议 |

## 7. 影响范围

- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue`
- `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue`
- `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`
- `forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`

## 8. 风险与关注点

- 表单和列表设计器都有嵌套滚动区，样式调整后要验证中间画布、右侧属性面板和页面外层不会出现双滚动干扰。
- 列表 `ListPageGridDesigner.vue` 文件较大，改动应集中在模板工具区和样式，不触碰网格拖拽、缩放、嵌套块逻辑。
- 表单 `ForgeFormDesigner.vue` 现有 scoped CSS 存在重复覆盖，清理时必须确认折叠状态 `left-collapsed/right-collapsed/right-open` 没有回退。
- 图标化按钮会减少文字说明，必须用 `title` 和可识别图标保证可用性。
- 本次不涉及资金、状态流转、权限变更。

## 8.5 测试策略

- **测试范围**：前端表单设计页、列表设计页、画布缩放/宽度控制、左右栏折叠、页面切换、保存入口、预览入口、拖拽基本路径。
- **覆盖率目标**：样式型变更不设新增单测覆盖率；以构建、lint 和手工视觉验收为主。
- **独立 Test Spec**：是。

## 9. 待澄清

- [ ] 是否确认进入编码阶段后，列表 `page-config-row/page-param-row/page-data-row` 可以从常驻展示改为折叠/弹出式展示，但字段和保存逻辑保持不变？

## 10. 技术决策

- 复用 Vue + Naive UI + scoped CSS，不引入新依赖。
- 优先在现有组件内整理模板和样式，只有出现明显重复且跨表单/列表都需要时，再考虑抽取共享 class。
- 颜色和尺寸按参考页收敛：
  - 页面背景：`#f8f9fa`。
  - 面板背景：左侧 `#fcfcfc`，右侧 `#fafafa`。
  - 边框：`#e4e4e7` / `#e5e7eb`。
  - 主强调：蓝靛色系，避免大面积单一蓝色背景。
  - 控件字号：11px、12px、13px 为主。
  - 左栏约 256px，右栏约 280px，顶部工具区约 48px。
- 视图控制统一为悬浮胶囊：图标触发，点击弹出输入框/选择器；缩放百分比作为紧凑文字按钮保留。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Research | 完成 | 无 | 已对照 `copyPage/src`、表单设计器、列表设计器入口和画布组件 |
| Plan | 完成 | `code-copilot/changes/lowcode-designer-workbench-visual-unification/spec.md` | 本文档 |
| Apply | 完成 | 表单/列表设计器相关 Vue 文件 | 统一工作台、页面切换条、点阵画布、白色页面、悬浮视图控制和左右面板样式 |
| Verify | 完成 | 无 | targeted eslint、`pnpm --dir forge-admin-ui build`、`git diff --check` 均通过 |

## 12. 审查结论

待实现后补充。

## 13. 确认记录（HARD-GATE）

- **确认时间**：待确认
- **确认人**：待确认

## 归档记录（HARD-GATE）
- **状态**：done
- **归档时间**：2026-06-27
- **归档人**：yaomd（批量归档）
- **归档路径**：code-copilot/changes/archive/2026-06-27-lowcode-designer-workbench-visual-unification/
- **判定依据**：任务清单全部完成，execution-log 验证通过（编译/构建/lint 闭环）。
