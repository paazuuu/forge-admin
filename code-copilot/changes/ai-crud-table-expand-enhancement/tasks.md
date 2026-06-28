# 任务拆分 — AiCrudPage 表格增强与通用展开面板
> 拆分顺序：配置协议 → 底层表格能力 → 展开数据加载 → 展开渲染器 → 设计器入口 → 预览发布适配
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 每个任务必须精确到文件路径和函数签名

## 前置条件
- [ ] 用户确认 `spec.md` 中 `expandConfig.panels[]` 协议。
- [ ] 用户确认第一期展开内容范围：建议 `table`、`descriptions`、`tabs` 必做，`form`、`custom` 做基础支持。
- [ ] 用户确认运行态列宽拖拽第一期是否持久化。

## Task 1: 扩展 AiCrudPage 配置协议
- **状态**: 已完成
- **目标**: 在 props 层定义表格增强和展开面板配置，不改变未配置页面行为。
- **涉及文件**:
  - `forge-admin-ui/src/components/ai-form/AiCrudPageProps.js` — 新增 `expandConfig`、表格拖拽相关 props 注释和默认值。
  - `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` — 读取并归一化 `expandConfig`，传给 AiTable。
- **关键签名**:
  ```js
  const normalizedExpandConfig = computed(() => normalizeExpandConfig(props.expandConfig, props.childrenConfig))
  ```

## Task 2: 补齐 AiTable 表格基础能力
- **状态**: 已完成
- **目标**: 支持列宽拖拽、固定头部、固定列稳定运行，并把 Naive UI 展开能力开放给 AiCrudPage。
- **涉及文件**:
  - `forge-admin-ui/src/components/ai-form/AiTable.vue` — 支持 `resizable`、`onUpdate:columns`/列宽状态、展开列渲染透传。
  - `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` — 管理拖拽后的列宽状态，合并到 `tableColumns`。
- **关键签名**:
  ```js
  function handleColumnResize(columnKey, width) {}
  const runtimeTableColumns = computed(() => mergeColumnWidthState(tableColumns.value, columnWidthState.value))
  ```

## Task 3: 新增展开数据加载与参数映射
- **状态**: 已完成
- **目标**: 支持 `row`、`api`、`children` 三类数据源，按当前行映射参数并懒加载缓存。
- **涉及文件**:
  - `forge-admin-ui/src/components/ai-form/AiCrudRowExpand.vue` — 新增，负责展开行数据加载、缓存状态和错误展示。
  - `forge-admin-ui/src/components/ai-form/expand-utils.js` — 新增，封装 `resolveExpressionValue`、`buildExpandParams`、`extractExpandData`。
  - `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` — 在展开列中渲染 `AiCrudRowExpand`。
- **关键签名**:
  ```js
  export function buildExpandParams(paramsMap, row, context = {}) {}
  export async function loadExpandPanelData(panel, row, context = {}) {}
  ```

## Task 4: 实现展开内容渲染器
- **状态**: 已完成
- **目标**: 展开内容不局限表格，支持多种飞书式展开展示。
- **涉及文件**:
  - `forge-admin-ui/src/components/ai-form/expand-renderers/ExpandTablePanel.vue` — 新增，渲染子表。
  - `forge-admin-ui/src/components/ai-form/expand-renderers/ExpandDescriptionsPanel.vue` — 新增，渲染描述信息。
  - `forge-admin-ui/src/components/ai-form/expand-renderers/ExpandFormPanel.vue` — 新增，渲染只读 AiForm。
  - `forge-admin-ui/src/components/ai-form/expand-renderers/ExpandTabsPanel.vue` — 新增，渲染多面板 tabs。
  - `forge-admin-ui/src/components/ai-form/expand-renderers/ExpandCustomPanel.vue` — 新增，渲染插槽/注册组件。
- **关键签名**:
  ```vue
  <ExpandPanelRenderer :panel="panel" :row="row" :data="panelData" :context="context" />
  ```

## Task 5: 设计器新增展开面板配置入口
- **状态**: 已完成
- **目标**: 用户通过可视化方式配置展开面板，优先选择“子表 / 描述 / Tabs / 自定义”预设。
- **涉及文件**:
  - `forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue` — AiCrudPage 属性区新增“展开面板”配置。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue` — 表单设计器 CRUD 更多配置新增列宽拖拽和展开面板配置。
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue` — schema 到运行态 props 时透传 `expandConfig`。
  - `forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue` — 设计器画布预览透传 `expandConfig`。
- **关键签名**:
  ```js
  function updateAiCrudExpandConfig(patch) {}
  function createDefaultExpandPanel(type, relation) {}
  ```

## Task 6: 低代码预览与发布链路适配
- **状态**: 已完成（当前运行链路透传）
- **目标**: 运行预览、发布运行页和动态 CRUD 页面都按同一协议展示展开面板。
- **涉及文件**:
  - `forge-admin-ui/src/components/lowcode-builder/preview/LowcodePreviewPane.vue` — 预览运行态透传 `expandConfig`。
  - 低代码 schema 编译相关文件（按实际定位）— 保留 `expandConfig`。
  - 动态 CRUD 配置渲染相关文件（按实际定位）— 保留 `expandConfig`。
- **关键签名**:
  ```js
  function transformExpandConfigForRuntime(expandConfig, modelSchema) {}
  ```

## Task 7: 验证与修正
- **状态**: 已完成
- **目标**: 覆盖表格基础能力、展开子表、描述展开、Tabs 展开和兼容性。
- **涉及文件**:
  - `code-copilot/changes/ai-crud-table-expand-enhancement/execution-log.md` — 追加验证记录。
  - `code-copilot/changes/ai-crud-table-expand-enhancement/test-spec.md` — 按实际实现更新验证证据。
- **关键命令**:
  ```bash
  source ~/.nvm/nvm.sh && nvm use v20.19.0
  pnpm --dir forge-admin-ui build
  ```
