# 任务拆分 — 低代码设计器工作台视觉统一
> 拆分顺序：研究对照 → 外层工作台 → 中间画布 → 左右面板 → 验证
> 每个任务尽量控制在 3-5 个文件内
> 本变更只改布局、样式和展示交互，不改业务协议和保存逻辑

## 前置条件

- [x] 已阅读 `copyPage/src` 参考页结构和样式。
- [x] 已阅读现有表单设计器入口、画布、左侧组件库、右侧属性面板。
- [x] 已阅读现有列表设计器入口和 `ListPageGridDesigner.vue` 画布结构。
- [x] 用户确认本计划后进入编码阶段。

## Task 1: 统一表单设计器外层工作台
> status: completed

- **目标**：让表单设计器外层布局对齐参考页工作台，并减少额外外框和重复滚动。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue` — 调整表单设计页外层高度、头部密度、`form-builder-grid` 包裹样式。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue` — 统一三栏宽度、顶部工具栏、表单切换条基础布局。
- **关键约束**：
  - 不改 `syncFormDesignerSchemaToPageSchema()`、`flushDesigner()`、`appendField()`、`switchFormAsset()` 等 schema 逻辑。
  - 保留拖拽时收起右侧面板的现有交互。

## Task 2: 统一表单中间画布四项视觉
> status: completed

- **目标**：表单中间画布只调整页面切换条、点阵背景、居中白色页面、悬浮视图控制。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue` — 将 `designer-form-tabs-bar` 改成参考页 PageDesignSwitcher 风格。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue` — 背景改点阵、白色页面样式统一、视口/缩放控制改悬浮胶囊并避免滚动遮挡。
- **关键约束**：
  - 不改 `handleCanvasWheel()`、`applyCanvasPreviewMode()`、`updateDesignWidth()`、`updateCanvasZoom()` 逻辑。
  - 不改画布内节点渲染、拖拽接收、容器插入规则。

## Task 3: 统一列表设计器顶部和页面切换区
> status: completed

- **目标**：重做 `list-designer-head` 和 `list-page-switch` 的布局，使列表设计页与表单设计页一致，并降低页面配置行占用高度。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue` — 统一顶部工具栏、页面切换条、更多菜单/折叠配置区、工作区滚动边界。
- **关键约束**：
  - 不改页面类型、页面参数、接口配置、模板选择和保存更新函数。
  - `page-config-row`、`page-param-row`、`page-data-row` 可改成折叠/弹出展示，但字段绑定和事件回调保持原样。

## Task 4: 统一列表中间画布四项视觉
> status: completed

- **目标**：列表中间画布与表单画布使用一致的点阵背景、居中白色页面、悬浮视图控制样式。
- **涉及文件**：
  - `forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue` — 调整 `.canvas-panel`、`.canvas-scroll`、`.canvas-zoom-stage`、`.canvas-grid`、`.canvas-viewport-dock`、`.canvas-toolbar`。
- **关键约束**：
  - 不改 `startMove()`、`startResize()`、`handleCanvasDrop()`、`resolveBlockStyle()`、`updateCanvasZoom()`、`updateDesignWidth()` 等逻辑。
  - 隐藏或弱化中间工具栏中的行列信息，不删除内部计算。

## Task 5: 统一左右面板视觉密度
> status: completed

- **目标**：表单和列表左右面板风格统一，形成企业低代码搭建器的一致体验。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue` — 统一左侧组件库搜索、分组、卡片、字段列表和折叠按钮样式。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue` — 统一右侧属性面板标题、Tab、搜索、折叠项、控件密度。
  - `forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue` — 同步列表左侧 `palette-panel` 和右侧属性面板样式。
- **关键约束**：
  - 不改字段使用状态、拖拽 MIME、禁用原因、属性更新函数。
  - 不移除现有属性配置项。

## Task 6: 响应式和滚动验收
> status: completed

- **目标**：确保 1366x768、1440x900、1920x1080 下核心工作区稳定可用。
- **涉及文件**：
  - 上述已修改 Vue 文件的 scoped CSS。
- **验收点**：
  - 表单/列表设计页没有横向挤压导致按钮换行错乱。
  - 中间画布滚动时悬浮视图控制可见。
  - 左右面板可收起、展开，中心画布保持居中。
  - 页面切换条不展示冗余行列信息。

## Task 7: 验证与记录
> status: completed

- **目标**：完成前端静态和构建验证，并记录结果。
- **命令**：
  - `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessFormDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`
  - `pnpm --dir forge-admin-ui build`
  - `git diff --check`
- **记录文件**：
  - `code-copilot/changes/lowcode-designer-workbench-visual-unification/execution-log.md`
