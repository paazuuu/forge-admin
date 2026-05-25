# 任务拆分 — 流程审批动作权限与办理要求控制

## Task 1: 变更说明

- [x] 创建 `spec.md`
- [x] 创建 `tasks.md`

## Task 2: 后端模型与迁移

- [ ] 扩展 `FlowNodeConfig`、`TaskFormInfo`、`FlowTask`
- [ ] 新增 Flyway 迁移脚本
- [ ] 更新初始化 SQL 表结构片段

## Task 3: 后端办理控制

- [ ] 扩展 `FlowTaskService` 接口
- [ ] 在 `FlowTaskServiceImpl` 读取节点办理配置
- [ ] 对通过、拒绝、转办执行服务端权限校验
- [ ] 新增退回上一节点、终结流程任务方法
- [ ] 扩展 `FlowTaskController` 接口

## Task 4: 前端设计器配置

- [ ] 扩展 `flowable-moddle.json`
- [ ] 在 `NodePropertiesPanel.vue` 增加办理控制配置 Tab
- [ ] 保存/回显 BPMN 扩展属性

## Task 5: 前端待办办理

- [ ] 扩展 `flow.js` 任务 API
- [ ] 待办抽屉按配置展示通过、拒绝、退回、终结、转办
- [ ] 审批意见、签名按配置必填
- [ ] 外置表单提交流程接入同一套校验

## Task 6: 验证

- [ ] 后端编译
- [ ] 前端构建或 lint
