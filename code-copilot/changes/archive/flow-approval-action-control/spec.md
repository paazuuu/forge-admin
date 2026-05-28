# 流程审批动作权限与办理要求控制
> status: implement
> created: 2026-05-23
> complexity: 中等

## 1. 背景与目标

当前流程办理页默认展示同意、驳回、转办，并且审批意见固定必填。业务需要按审批节点控制办理人可执行的动作，并配置办理时是否必须签名、是否必须填写审批意见。

目标：
- 在流程设计器用户任务节点中配置办理动作权限：通过、拒绝、退回、终结流程、转办等。
- 在审批办理接口侧校验动作权限，避免只靠前端隐藏按钮。
- 在待办办理页按配置展示动作按钮，并校验签名、审批意见必填规则。
- 将审批签名写入任务记录，满足后续审计展示。

## 2. 现状

相关文件：
- `forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue`：节点属性配置，当前只维护 BPMN XML 属性。
- `forge-admin-ui/src/views/flow/todo.vue`：待办审批抽屉，当前固定要求审批意见，并展示同意/驳回/转办。
- `forge-admin-ui/src/api/flow.js`：流程任务 API。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/.../FlowTaskServiceImpl.java`：审批通过、驳回、转办、任务表单信息。
- `forge/forge-flow/forge-flow-server/src/main/java/.../FlowTaskController.java`：任务办理接口。
- `forge/db/migration/`：正式 Flyway 迁移脚本。

已有能力：
- `TaskFormInfo` 已返回 `allowDelegate`、`allowReject`、`allowRejectToStart`。
- `FlowNodeConfig` 已有转办/转交/加签/减签/驳回等部分字段，但设计器主链路主要通过 BPMN XML 保存节点属性。
- `FlowInstanceService` 已支持按流程实例终止、回退到指定活动、任务转派。

## 3. 功能范围

### 功能 1：节点办理动作配置

在用户任务节点增加“办理控制”配置：
- 允许通过
- 允许拒绝
- 允许退回
- 允许终结流程
- 允许转办
- 需要审批意见
- 需要签名

保存到 BPMN 的 `flowable:*` 扩展属性，同时后端兼容 `sys_flow_node_config` 字段。

### 功能 2：办理页按配置展示与校验

待办抽屉：
- 只展示当前节点允许的动作按钮。
- 审批意见按 `requireComment` 决定是否必填。
- 签名按 `requireSignature` 决定是否显示并必填。
- 外置表单提交也沿用同一套校验规则。

### 功能 3：服务端权限与必填校验

接口侧：
- `approve` 校验允许通过、审批意见、签名。
- `reject` 校验允许拒绝、审批意见、签名。
- `delegate` 校验允许转办。
- 新增 `return` 校验允许退回，并退回上一已完成用户任务。
- 新增 `terminate` 校验允许终结流程，并终止当前流程实例。

### 功能 4：数据持久化

数据库迁移新增：
- `sys_flow_node_config.allow_approve`
- `sys_flow_node_config.allow_return`
- `sys_flow_node_config.allow_terminate`
- `sys_flow_node_config.require_signature`
- `sys_flow_node_config.require_comment`
- `sys_flow_task.signature`

## 4. 非目标

- 不实现加签/减签的完整流程语义，只保留配置扩展空间。
- 不新增手写签名板，签名先按文本签名保存。
- 不改造 Flowable BPMN 拒绝网关建模规则，仅在拒绝时补充 `approved=false` 变量。

## 5. 业务规则

- 未配置时保持兼容默认：允许通过、允许拒绝、允许转办；不允许退回、终结；审批意见必填；签名非必填。
- 审批动作权限既可来自 BPMN `flowable:*` 属性，也可来自 `sys_flow_node_config`，节点配置表优先级高于 BPMN。
- 退回默认退到当前流程实例中最近一个已完成的用户任务；没有可退回节点时返回错误。
- 终结流程会结束当前流程实例并将本地任务标记完成。
- 审批意见、签名必填校验以去除首尾空格后的内容为准。

## 6. 验证策略

- 后端编译：`mvn -pl forge-flow/forge-flow-server -am compile -DskipTests` 或可编译覆盖 flow 模块的命令。
- 前端构建：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm build`。
- 手工验证：设计器配置用户任务办理控制，保存并发布后，在待办页验证按钮展示与必填校验。
