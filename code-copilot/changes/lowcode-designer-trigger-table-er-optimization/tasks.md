# 任务拆分 — 低代码设计器触发器、表格居中与 ER 图优化
> 拆分顺序：配置入口收敛 → 表格默认值 → ER 图 → 验证

## 前置条件

- [x] 已确认触发器独立页为 `forge-admin-ui/src/views/app-center/trigger.vue`。
- [x] 已确认对象设计器为 `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`。
- [x] 已确认基础表格默认居中在 `AiTable.vue`，低代码列表设计器存在 `left` 覆盖。
- [x] 已确认 ER 图组件为 `LowcodeErDiagram.vue`。

## Task 1: 对象设计器内嵌触发器配置

- **目标**: 让触发器配置成为业务对象设计器的左侧面板，并锁定当前对象。
- **状态**: completed
- **涉及文件**:
  - `forge-admin-ui/src/views/app-center/trigger.vue` — 增加 `embedded/objectCode/objectName/lockObject` props，支持路由页和内嵌双态。
  - `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue` — 新增 `BusinessTriggerConfigPanel` 异步组件和 `activePanel === 'triggers'` 渲染。
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue` — 新增左侧导航项“自动化触发器”，闭环步骤点击切换到该面板。
- **验收点**:
  - 从对象设计器左侧点击“自动化触发器”后不离开当前页面。
  - 新增触发器默认带当前 `objectCode`，业务单元选择禁用。
  - 独立 `/app-center/trigger` 页面仍可全局筛选。

## Task 2: 表格列默认居中修正

- **目标**: 低代码列表设计器生成的列不再用 `left` 覆盖 `AiTable` 默认居中。
- **状态**: completed
- **涉及文件**:
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue` — 将 `align: setting.align || 'left'` 改为只在用户配置时写入对齐，默认交给 `AiTable`。
- **验收点**:
  - 未设置对齐的列，表头和内容默认居中。
  - 已设置 `left/right/center` 的列继续按配置显示。

## Task 3: 关系 ER 图入口

- **目标**: 在“关系与级联”内开放 ER 图查看。
- **状态**: completed
- **涉及文件**:
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue` — 增加 ER 图切换入口，导入 `LowcodeErDiagram`，基于当前对象和目标对象构造模型。
- **验收点**:
  - 当前对象至少展示为主模型。
  - 已配置关系展示为对象连线，目标对象字段加载后补充字段列表。
  - 无关系时展示当前对象节点和空态说明。

## Task 4: 验证与记录

- **目标**: 按自动化测试标准记录命令、结果和跳过项。
- **状态**: completed
- **涉及文件**:
  - `code-copilot/changes/lowcode-designer-trigger-table-er-optimization/test-spec.md`
  - `code-copilot/changes/lowcode-designer-trigger-table-er-optimization/execution-log.md`
- **命令**:
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui build`
  - `git diff --check`
