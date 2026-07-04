# 任务拆分 — 表格横向拖拽与表头固定优化
> 拆分顺序：底层交互 → 组件接入 → 样式 → 验证

## 前置条件

- [x] 已确认主链路为 `AiCrudPage -> AiTable -> n-data-table`。
- [x] 已确认全局指令入口为 `forge-admin-ui/src/directives/index.js`。

## Task 1: 表格拖拽滚动指令

- **目标**: 新增通用指令，在表格内部可按住拖动横向滚动。
- **状态**: completed
- **涉及文件**:
  - `forge-admin-ui/src/directives/modules/tableScrollEnhance.js` — 新增，封装拖拽滚动、事件清理和交互元素保护。
  - `forge-admin-ui/src/directives/index.js` — 注册 `v-table-scroll-enhance`。
- **关键签名**:
  ```js
  function bindTableScrollEnhance(el, options = {}) {}
  function cleanup(el) {}
  ```

## Task 2: AiTable 默认接入增强

- **目标**: `AiTable` 表格模式默认启用横向拖拽。
- **状态**: completed
- **涉及文件**:
  - `forge-admin-ui/src/components/ai-form/AiTable.vue` — 在 `n-data-table` 上绑定 `v-table-scroll-enhance` 并增加可关闭 prop。
- **关键签名**:
  ```js
  dragScroll: { type: Boolean, default: true }
  ```

## Task 3: 表头粘性与拖拽状态样式

- **目标**: 全局 Naive DataTable 表头在表格滚动容器内 sticky，拖拽时显示合理光标和选择保护。
- **状态**: completed
- **涉及文件**:
  - `forge-admin-ui/src/styles/theme.css` — 添加 `.n-data-table-th` sticky、拖拽状态、固定列 z-index。

## Task 4: 验证与记录

- **目标**: 执行前端静态/构建验证，必要时浏览器抽查。
- **状态**: completed
- **涉及文件**:
  - `code-copilot/changes/table-scroll-sticky-header/test-spec.md`
  - `code-copilot/changes/table-scroll-sticky-header/execution-log.md`
