# 任务拆分 — 流程分支条件表单规则配置优化
> 拆分顺序：入口传参 → 条件配置实现 → 测试验证

## 前置条件
- [x] 已确认流程设计页存在 `formFieldCatalog`
- [x] 已确认 BPMN 导出使用 `edge.condition`

## Task 1: 接入表单字段目录
- **状态**: 已完成
- **目标**: 将流程设计页动态表单字段目录传入条件分支配置组件。
- **涉及文件**:
  - `forge-admin-ui/src/views/flow/design.vue` — 给 `DingFlowDesigner` 增加 `form-field-catalog` 入参。
  - `forge-admin-ui/src/components/flow-designer/DingFlowDesigner.vue` — 新增 prop 并传给 `NodeConfigDrawer`。
  - `forge-admin-ui/src/components/flow-designer/panel/NodeConfigDrawer.vue` — 新增 prop 并透传给网关配置组件。

## Task 2: 实现条件规则配置器
- **状态**: 已完成
- **目标**: 在条件分支配置里支持字段、运算符和值组合生成 SpEL。
- **涉及文件**:
  - `forge-admin-ui/src/components/flow-designer/panel/ConditionConfig.vue` — 替换单输入框为规则模式 + 高级表达式模式。
- **关键签名**:
  ```js
  function buildExpression(rules, logic) {}
  function updateRule(edgeId, index, patch) {}
  ```

## Task 3: 补充测试并验证
- **状态**: 已完成
- **目标**: 覆盖新交互，执行针对性前端测试。
- **涉及文件**:
  - `forge-admin-ui/src/components/flow-designer/panel/__tests__/ConditionConfig.spec.js` — 新增组件测试。
  - `forge-admin-ui/src/views/flow/utils/form-field-catalog.js` — 新增动态表单字段目录递归解析工具。
  - `forge-admin-ui/src/views/flow/utils/__tests__/form-field-catalog.spec.js` — 覆盖嵌套表单、`_forge.fieldBinding` 和 `ref_` 过滤。
  - `code-copilot/changes/flow-condition-form-rules/execution-log.md` — 记录验证命令和结果。

## Task 4: 修正分支条件交互细节
- **状态**: 已完成
- **目标**: 修复条件表达式内容溢出、规则不能移除，以及点击条件边时错误展示全部分支的问题。
- **涉及文件**:
  - `forge-admin-ui/src/components/flow-designer/DingFlowDesigner.vue` — 分支标签点击时传递当前 edgeId，节点点击时恢复全部分支。
  - `forge-admin-ui/src/components/flow-designer/panel/NodeConfigDrawer.vue` — 透传聚焦分支 ID。
  - `forge-admin-ui/src/components/flow-designer/panel/ConditionConfig.vue` — 支持聚焦单分支、删除最后一条规则后清空条件、收紧规则行和表达式预览布局。
  - `forge-admin-ui/src/components/flow-designer/panel/__tests__/ConditionConfig.spec.js` — 覆盖聚焦分支和删除最后一条规则。
  - `forge-admin-ui/src/components/flow-designer/__tests__/DingFlowDesigner.spec.js` — 覆盖点击分支标签只展示当前分支。

## Task 5: 修正默认分支条件配置和规则行对齐
- **状态**: 已完成
- **目标**: 默认分支仍允许配置条件表达式，并优化规则配置行的视觉对齐。
- **涉及文件**:
  - `forge-admin-ui/src/components/flow-designer/panel/ConditionConfig.vue` — 设置默认分支不再清空条件；默认分支不再禁用条件配置；规则行改为字段/关系/取值对齐布局。
  - `forge-admin-ui/src/components/flow-designer/canvas/BranchHeader.vue` — 默认分支始终展示默认标签，避免误导为条件会参与执行。
  - `forge-admin-ui/src/components/flow-designer/canvas/EdgePath.vue` — 连线标签同步保持默认标签。
  - `forge-admin-ui/src/components/flow-designer/converter/json-to-bpmn.js` — 默认边存在条件时不写出 `conditionExpression`，满足 Flowable 部署校验。
  - `forge-admin-ui/src/components/flow-designer/converter/branch-parser.js` — 导入 BPMN 时不再清空 default 边已有条件。
  - `ConditionConfig.spec.js`, `EdgePath.spec.js`, `json-to-bpmn.spec.js`, `branch-parser.spec.js` — 补默认分支条件保留、展示和导出跳过测试。

## Task 6: 支持条件网关继续添加多条分支
- **状态**: 已完成
- **目标**: 条件分支不再限制为固定两条，配置面板可继续添加分支，并保持唯一默认分支。
- **涉及文件**:
  - `forge-admin-ui/src/components/flow-designer/composables/useFlowDesigner.js` — 新增 `addBranch(gatewayId)`，追加审批分支并接回已有合流节点，归一化默认分支。
  - `forge-admin-ui/src/components/flow-designer/panel/ConditionConfig.vue` — 在条件摘要区域增加“添加分支”入口。
  - `forge-admin-ui/src/components/flow-designer/panel/NodeConfigDrawer.vue` — 透传添加分支事件。
  - `forge-admin-ui/src/components/flow-designer/DingFlowDesigner.vue` — 接收添加分支事件，创建分支后聚焦新分支条件配置。
  - `useFlowDesigner.spec.js`, `ConditionConfig.spec.js`, `DingFlowDesigner.spec.js` — 覆盖第三分支、唯一默认分支和抽屉聚焦新分支。

## Task 7: 调整分支添加入口与条件回显
- **状态**: 已完成
- **目标**: 将添加分支操作放到画布分支连线区域，并简化边上条件展示；表单字段表达式重新打开时回显到字段条件模式。
- **涉及文件**:
  - `forge-admin-ui/src/components/flow-designer/canvas/BranchAddButton.vue` — 新增画布层添加分支按钮。
  - `forge-admin-ui/src/components/flow-designer/canvas/BranchHeader.vue` — 条件分支标签改为摘要显示，不直接显示 SpEL 原文。
  - `forge-admin-ui/src/components/flow-designer/canvas/EdgePath.vue` — 网关分支边不再在 SVG 层重复渲染条件标签。
  - `forge-admin-ui/src/components/flow-designer/DingFlowDesigner.vue` — 在条件网关分支区域渲染添加分支按钮。
  - `forge-admin-ui/src/components/flow-designer/panel/ConditionConfig.vue` — 移除抽屉内添加分支入口，并支持把常见表单字段表达式反解析为规则行。
  - `AddNodeButton.spec.js`, `EdgePath.spec.js`, `ConditionConfig.spec.js`, `DingFlowDesigner.spec.js` — 覆盖画布按钮、条件摘要和规则模式回显。

## Task 8: 补齐流程审批策略和表单字段权限
- **状态**: 已完成
- **目标**: 在流程设计时支持提交人撤回权限、重复审批自动同意策略、审批意见必填配置，并将表单字段权限从手工输入改为按动态表单字段勾选。
- **涉及文件**:
  - `forge-admin-ui/src/views/flow/design.vue` — 新增审批设置页，配置提交人撤回和重复审批自动同意策略，并写入流程级 BPMN 属性。
  - `forge-admin-ui/src/components/flow-designer/DingFlowDesigner.vue`、`useFlowDesigner.js`、`json-to-bpmn.js`、`bpmn-to-json.js` — 增加 `flowJson.config` 读写和 BPMN process 扩展属性持久化。
  - `forge-admin-ui/src/components/flow-designer/panel/FormPermissionConfig.vue`、`ApproverConfig.vue`、`NodeConfigDrawer.vue` — 表单字段权限改为按全局表单字段目录勾选。
  - `forge-admin-ui/src/components/form-create/FlowFormCreateRenderer.vue`、`forge-admin-ui/src/views/flow/todo.vue` — 待办动态表单按字段权限隐藏、禁用和补必填校验。
  - `forge-server/.../TaskFormInfo.java`、`FlowTaskServiceImpl.java` — 下发表单字段权限，按流程属性限制提交人撤回，审批通过后执行重复审批自动同意。
  - `FormPermissionConfig.spec.js`、`json-to-bpmn.spec.js`、`bpmn-to-json-linear.spec.js`、`user-task-parser-permissions.spec.js` — 补流程级策略和字段权限持久化测试。
