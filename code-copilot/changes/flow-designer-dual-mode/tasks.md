# 任务拆分 — 流程模型双设计器模式改造

## Task 1: 恢复 SDD 基线
- **状态**: 已完成
- **目标**: 建立本次变更的 spec、tasks、test-spec 和 execution-log。
- **涉及文件**:
  - `code-copilot/changes/flow-designer-dual-mode/spec.md`
  - `code-copilot/changes/flow-designer-dual-mode/tasks.md`
  - `code-copilot/changes/flow-designer-dual-mode/test-spec.md`
  - `code-copilot/changes/flow-designer-dual-mode/execution-log.md`

## Task 2: 恢复旧 BPMN.js 设计器文件
- **状态**: 已完成
- **目标**: 从 `9a2049b5^` 恢复旧 `forge-admin-ui/src/components/bpmn` 目录。
- **涉及文件**:
  - `forge-admin-ui/src/components/bpmn/*`
  - `forge-admin-ui/package.json`
  - `forge-admin-ui/pnpm-lock.yaml`

## Task 3: 增加设计器类型持久化
- **状态**: 已完成
- **目标**: 为流程模型增加 `designerType`，兼容历史模型默认审批流程。
- **涉及文件**:
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowModel.java`
  - `forge-server/db/migration/V1.0.73__add_flow_model_designer_type.sql`

## Task 4: 流程模型入口支持选择类型
- **状态**: 已完成
- **目标**: 新建模型时选择“审批流程/业务流程”，列表卡片展示类型。
- **涉及文件**:
  - `forge-admin-ui/src/views/flow/model.vue`

## Task 5: 设计页双模式渲染
- **状态**: 已完成
- **目标**: 设计页按 `modelInfo.designerType` 加载 `DingFlowDesigner` 或恢复的 `FlowModeler`。
- **涉及文件**:
  - `forge-admin-ui/src/views/flow/design.vue`

## Task 6: 验证与记录
- **状态**: 已完成
- **目标**: 执行增量验证并记录证据。
- **涉及文件**:
  - `code-copilot/changes/flow-designer-dual-mode/execution-log.md`
