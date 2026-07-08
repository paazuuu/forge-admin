# 业务模块与流程模块通用联动架构落地方案
> status: proposed
> created: 2026-06-28
> complexity: 🔴 复杂
> related: `code-copilot/changes/lowcode-app-full-loop-optimization/spec.md`, `code-copilot/changes/form-first-business-object-designer/spec.md`, `code-copilot/changes/lowcode-runtime-datasource-isolation/spec.md`

## 1. 背景

参考文档 `/Users/yaomindong/Desktop/project/mdframe/shenrong/shenrong-projman/code-copilot/knowledge/business-flow-fast-adaptation-guide.md` 的核心判断是：

`flow 管通用流转，business 管业务规则`

这个方向与 Forge 当前架构决策一致：审批不再建设独立审批引擎，统一归入 Flowable 流程引擎；低代码业务对象已经围绕“表单优先、动态 CRUD、单据配置、流程绑定、触发器、消息、报表”形成主链路。

但 Forge 不是传统手写业务系统。当前工程已经具备：

- 低代码对象设计器、表单设计器、字段注册、运行态动态 CRUD。
- 单据模式配置、状态映射、运行态操作按钮、流程进度展示。
- `BusinessFlowService` 动态发起流程、写入 `ai_business_flow_instance_link`、回写单据状态。
- Flow 客户端、Flow 事件 Redis/Webhook 发布、租户上下文分发。
- Flow 入口表单创建低代码业务记录的桥接能力。

因此参考方案里“新增 `BusinessFlowAdapter` + `AbstractBusinessFlowService`”不能原样作为主方案。它适合手写业务模块，但对 Forge 低代码平台来说，应升级为：

`业务对象流程契约 + 运行时流程网关 + 流程事件桥 + 低代码发布编译`

目标不是让每个新流程少写几个 Service 方法，而是让低代码用户完成：

`表单创建 -> 单据生命周期 -> 流程配置 -> 节点表单/权限 -> 发布检查 -> 运行态填报/审批/回写/消息`

## 2. 现状评估

### 2.1 已经合理的现有能力

1. `BusinessFlowService` 已承担低代码业务对象与流程引擎的动态集成。
   - 从 `ai_business_binding` 读取 `binding_type=FLOW` 配置。
   - 从动态 CRUD 读取业务记录。
   - 根据变量映射构建流程变量。
   - 调用 `FlowClient.startProcess` 发起流程。
   - 写入 `ai_business_flow_instance_link`。
   - 按流程结果回写单据状态并发布业务事件。

2. `BusinessDocumentRuntimeService` 已把单据状态、流程实例、可执行动作合并为运行态协议。
   - `AiCrudPage` 可根据运行态动作显示“发起主流程”。
   - 详情页可展示流程时间轴和流程图。

3. Flow 侧已经具备事件发布和租户上下文能力。
   - `FlowTaskEventListener` 发布 `TASK_CREATED`、`TASK_COMPLETED`、`PROCESS_COMPLETED`、`PROCESS_REJECTED`、`PROCESS_CANCELED`。
   - `FlowEventMessage` 已携带 `tenantId`、`businessKey`、`processInstanceId`、`processDefKey`、`taskDefKey`、`variables` 等关键字段。
   - `FlowEventSubscriber` 收到事件后会用 `TenantContextHolder` 分发。

4. 低代码设计器已经具备流程配置入口。
   - `BusinessDocumentPanel` 配置单据模式、编号、状态映射、流程展示开关。
   - `BusinessFlowBindingPanel` 配置流程模型、发起方式、标题模板和变量映射。
   - `FlowVariableMappingEditor` 支持推荐映射。

5. Flow 入口表单也能反向创建低代码业务记录。
   - `FlowBusinessObjectRuntimeAdapterImpl` 支持 `BUSINESS_OBJECT/HYBRID` 数据模式。
   - 这为长期“从流程入口表单创建业务单据”提供了基础。

### 2.2 仍然存在的核心问题

1. 集成层协议偏薄。
   - 当前只覆盖默认流程、标题模板、变量映射和简单状态回写。
   - 缺少完整的“业务对象流程契约”，无法统一描述流程版本、节点表单、字段权限、驳回重提、撤回、抄送、回调策略、幂等策略和副作用。

2. 两条链路尚未统一。
   - 链路 A：业务单据运行页发起流程。
   - 链路 B：流程入口表单创建业务记录后启动流程。
   - 两者都写 `ai_business_flow_instance_link`，但记录主键、变量快照、状态机、表单快照和回调语义没有统一契约。

3. 事件消费不适合动态低代码主流程。
   - `FlowEventSubscriber` 面向 `@FlowBind(modelKey=...)` 的手写 Bean 分发。
   - 低代码流程模型是运行时配置，不能要求每个模型生成一个手写 `@FlowBind` Bean。
   - 需要一个平台级 `BusinessFlowEventBridge` 订阅所有 Flow 事件，再按 `businessKey/objectCode/processDefKey` 路由到低代码对象。

4. 记录主键协议不够通用。
   - `BusinessFlowStartDTO.recordId`、`AiBusinessFlowInstanceLink.recordId`、`TaskFormInfo.recordId` 和 `FlowBusinessObjectRuntimeAdapter.BusinessRecordCreateResult.recordId` 当前是 `Long`。
   - 低代码运行时已支持自定义单字段主键和多数据源，流程链路仍未完全适配。

5. 驳回修改重提没有产品级协议。
   - 参考文档指出“驳回到发起人修改节点”不是流程终态，这个判断正确。
   - 当前 `BusinessFlowService.normalizeCallbackResult` 只按最终结果回写 `APPROVED/REJECTED/CANCELED`，缺少 `TASK_COMPLETED + approvalResult=reject + modifyTaskKey` 的中间状态处理。

6. 审批办理页与业务详情页仍会分叉。
   - Flow 侧 `TaskFormInfo` 支持动态表单、外部表单、表单快照和字段权限。
   - 低代码业务详情页使用 `AiCrudPage/AiForm/FieldValueRenderer`。
   - 待办审批时缺少统一的 `BUSINESS_OBJECT_FORM` 表单类型来复用业务对象详情/编辑渲染和字段权限。

7. 发布检查与流程配置还不够一体化。
   - 单据配置和流程绑定目前是两个面板。
   - 发布时应把状态字段、流程模型、变量映射、节点表单权限、按钮权限和触发器动作作为一个整体检查。

8. 可观测性和验收模板不足。
   - 已有 Flow 错误日志、触发器日志、站内消息。
   - 但缺少业务流程链路级 trace：一次发起、变量、Flow 实例、事件回调、状态回写、触发器副作用、消息推送应该能按 `correlationId` 串起来。

## 3. 对参考建议的合理性判断

| 参考建议 | 判断 | Forge 落地方式 |
| --- | --- | --- |
| `flow 管通用流转，business 管业务规则` | 直接采纳 | Flow 只负责模型、实例、任务、事件；业务对象负责状态字段、变量、数据权限、副作用。 |
| 抽 `BusinessFlowAdapter` | 改造采纳 | 手写业务模块可用 Adapter；低代码主链路应以 `BusinessFlowContract` + `BusinessFlowGateway` 替代每业务 Service 继承。 |
| 建业务流程元数据配置 | 直接采纳但复用现有表 | 复用 `ai_business_binding`、`ai_business_document_config`、`ai_business_flow_instance_link`，补充发布态契约快照，不另起重复配置表。 |
| 流程模型关联业务表并配置状态字段 | 分层采纳 | 这是最小可用集成层，适合低代码对象和简单业务表；Forge 需要扩展为 `businessBinding`，同时保留复杂代码业务 Adapter。 |
| 统一事件协议携带 `tenantId/businessKey/processInstanceId/taskDefKey/variables` | 直接采纳 | 现有事件字段基本具备；补 `eventId`、`correlationId`、`objectCode`、`recordKey`、`taskResult`、`formInstanceId`。 |
| BPMN 模板库 | 直接采纳 | 接入 Flow 设计器和 AI 生成器，模板产出 BPMN、变量清单、节点表单权限默认值和发布检查规则。 |
| 驳回修改重提固化为框架能力 | 直接采纳 | 作为 `rejectStrategy=RETURN_TO_MODIFY_TASK` 的契约能力，不要求每个业务对象手写逻辑。 |
| 流程脚手架 | 改造采纳 | 对代码生成模块生成手写业务骨架；对低代码对象生成运行态契约和流程模板，不生成大量重复 Java。 |
| 每条流程手动写 Flyway、菜单、权限 | 不作为低代码主路径 | 手写模块仍需要；低代码应用应通过对象发布自动生成菜单、按钮权限和运行态配置。 |

## 4. 目标架构

### 4.1 总体链路

```text
设计态
Form Designer
    -> Business Object Designer
    -> Document Lifecycle
    -> Flow Binding / Flow Template
    -> Node Form Permission
    -> Publish Validator
    -> BusinessFlowContract Snapshot

运行态发起
AiCrudPage / FlowEntry / Trigger
    -> BusinessFlowGateway
    -> BusinessRecordRuntimeFacade
    -> FlowClient
    -> Flowable
    -> ai_business_flow_instance_link

运行态事件
FlowTaskEventListener
    -> FlowEventPublisher / Webhook
    -> BusinessFlowEventBridge
    -> BusinessFlowStateMachine
    -> DynamicCrud internal update
    -> BusinessEventPublisher
    -> Trigger / Message / Report Metrics

审批办理
Flow Todo
    -> TaskFormInfo
    -> BusinessTaskFormContext
    -> Lowcode Business Form Renderer
    -> Approve / Reject / Return / Resubmit
```

### 4.2 模块职责

#### Flow 模块

- 管理 Flowable 模型、版本、部署、流程实例和任务。
- 处理通过、驳回、撤回、终止、转办、签收、会签等流程动作。
- 发布流程事件。
- 提供流程图、历史、任务表单、节点权限、抄送、站内待办消息。
- 提供通用组织/角色/SPEL 解析能力。
- 不直接查询低代码业务表，不直接决定业务状态。

#### Business / Lowcode 模块

- 管理业务对象、字段、表单、列表、详情、单据、权限和发布态配置。
- 管理业务状态字段和状态流转规则。
- 构建流程变量。
- 解析业务相关审批人变量，例如项目负责人、合同负责人、记录负责人。
- 维护业务记录与流程实例关联。
- 消费流程事件并回写业务状态。
- 执行流程完成后的业务副作用，例如创建关联记录、更新字段、发送消息、刷新统计。

#### Code-first 复杂业务模块

不是所有业务都适合低代码建模。资金、库存、合同、订单、外部系统集成、复杂状态机等代码优先业务，应保留自己的领域模型、数据库表、事务边界和 Service 编排。

这类业务接入流程时遵循：

- 业务模块自己负责业务校验、状态机、领域副作用和数据一致性。
- Flow 仍只负责流程模型、任务流转、审批动作、流程事件和流程历史。
- 平台提供统一接入 SDK/Adapter，让代码业务复用 `BusinessFlowGateway`、`BusinessFlowEventBridge`、流程 trace、待办表单上下文和标准验收模板。
- 业务模块不需要写低代码对象配置，也不要求迁移到动态 CRUD。
- 业务模块必须显式注册 `businessType/objectCode/modelKey/businessKey` 规则，避免流程事件无法路由。

#### 通用联动层

新增或重构为以下平台服务：

| 服务 | 作用 |
| --- | --- |
| `BusinessFlowContractService` | 读取/保存/发布业务对象流程契约，兼容现有 `ai_business_binding` 和单据配置。 |
| `BusinessFlowGateway` | 统一发起、撤回、取消、重提、查询运行态、更新变量，不让页面和触发器直接拼 FlowClient 调用。 |
| `BusinessRecordRuntimeFacade` | 屏蔽动态 CRUD、运行数据源、主键类型、租户、审计字段和内部状态更新。 |
| `BusinessFlowEventBridge` | 订阅 Flow 全量事件，按业务键和流程契约路由到低代码对象。 |
| `BusinessFlowStateMachine` | 将流程事件、节点结果、单据状态映射成可审计的业务状态变更。 |
| `BusinessTaskFormContextService` | 为待办页输出低代码业务表单上下文、字段权限和审批动作策略。 |
| `BusinessFlowTraceService` | 记录发起、回调、状态回写、触发器和消息的链路日志。 |
| `BusinessFlowAdapterRegistry` | 注册代码优先业务模块的流程适配器，按 `businessType/modelKey/businessKey` 路由事件。 |
| `BusinessFormAssetRegistry` | 汇聚低代码表单、Flow 动态表单、代码表单和外部表单资产，供设计器选择和待办页解析。 |

## 5. 核心契约设计

### 5.1 BusinessFlowContract

发布态应形成一份业务对象流程契约。首期可以存入 `ai_business_binding.binding_config` 和设计版本快照，后续如版本化需求增强，再独立成 `ai_business_flow_contract_version`。

```json
{
  "schemaVersion": "1.0",
  "objectCode": "crm_opportunity",
  "configKey": "crm_opportunity_runtime",
  "recordKey": {
    "primaryField": "id",
    "valueType": "STRING",
    "businessKeyPattern": "${objectCode}:${recordKey}"
  },
  "businessBinding": {
    "mode": "LOWCODE_OBJECT",
    "tableName": "crm_opportunity",
    "primaryKeyField": "id",
    "tenantField": "tenant_id",
    "statusField": "document_status",
    "titleField": "opportunity_name"
  },
  "document": {
    "enabled": true,
    "statusField": "documentStatus",
    "statusMapping": {
      "DRAFT": "DRAFT",
      "IN_PROCESS": "IN_PROCESS",
      "APPROVED": "APPROVED",
      "REJECTED": "REJECTED",
      "CANCELED": "CANCELED"
    }
  },
  "mainFlow": {
    "flowModelKey": "crm_opportunity_approval",
    "flowModelVersionPolicy": "LATEST_ON_START",
    "startMode": "MANUAL_AND_TRIGGER",
    "titleTemplate": "${opportunityName}-商机审批",
    "rejectStrategy": "RETURN_TO_MODIFY_TASK",
    "modifyTaskKey": "initiator_modify_task"
  },
  "variableMapping": [
    {
      "formField": "amount",
      "flowVariable": "amount",
      "required": true
    },
    {
      "formField": "ownerId",
      "flowVariable": "businessOwnerId",
      "resolver": "FIELD_VALUE"
    }
  ],
  "nodeForms": [
    {
      "taskDefKey": "dept_manager_task",
      "formMode": "BUSINESS_OBJECT_FORM",
      "viewKey": "approval_detail",
      "fieldPermissions": {
        "amount": "READONLY",
        "approvalComment": "EDITABLE"
      }
    },
    {
      "taskDefKey": "initiator_modify_task",
      "formMode": "BUSINESS_OBJECT_FORM",
      "viewKey": "reject_modify",
      "editableFields": ["amount", "expectedCloseDate", "remark"]
    }
  ],
  "events": {
    "onStarted": {
      "status": "IN_PROCESS"
    },
    "onApproved": {
      "status": "APPROVED",
      "publishBusinessEvent": "FLOW_APPROVED"
    },
    "onRejected": {
      "status": "REJECTED",
      "publishBusinessEvent": "FLOW_REJECTED"
    },
    "onCanceled": {
      "status": "CANCELED",
      "publishBusinessEvent": "FLOW_CANCELED"
    }
  },
  "options": {
    "allowDuplicateRunning": false,
    "eventIdempotent": true,
    "detailFlowTimelineVisible": true,
    "detailFlowDiagramVisible": true
  }
}
```

### 5.2 业务表绑定规则

类似 JeecgBoot 在流程模型里关联业务表、配置流程状态字段的做法，在 Forge 中可以作为首期最小闭环能力，但要放进业务流程契约，而不是散落在 BPMN 模型属性里。

建议抽象为 `businessBinding`：

| 字段 | 说明 |
| --- | --- |
| `mode` | `LOWCODE_OBJECT`、`BUSINESS_TABLE`、`ADAPTER` |
| `tableName` | 业务表名，低代码对象可由发布态配置自动生成 |
| `primaryKeyField` | 业务记录主键字段 |
| `tenantField` | 租户字段，默认 `tenant_id` |
| `statusField` | 流程/单据状态字段 |
| `titleField` | 流程标题默认取值字段，可选 |
| `ownerField` | 业务负责人字段，可选，用于变量推荐 |

三种使用方式：

| 模式 | 适用对象 | 回写方式 |
| --- | --- | --- |
| `LOWCODE_OBJECT` | 低代码创建的业务对象 | 由对象元数据自动生成表名、主键、状态字段，`BusinessRecordRuntimeFacade` 回写。 |
| `BUSINESS_TABLE` | 简单代码业务，只有状态同步和基础详情 | 开发者配置白名单表和字段，平台只做状态字段、流程实例关联和标题读取。 |
| `ADAPTER` | 合同、采购、库存、资金等复杂业务 | 不直接改业务表，由 `BusinessFlowAdapter` 调业务 Service 完成校验、状态机和副作用。 |

边界很重要：业务表绑定只能解决“流程实例关联哪条业务记录、状态字段怎么同步”。它不能替代变量构建、节点表单资产、字段权限、驳回修改重提、业务副作用和复杂事务。

安全约束：

- `tableName/fieldName` 必须来自发布态元数据或开发者注册白名单，禁止前端传什么就拼什么。
- 更新状态必须走 `BusinessRecordRuntimeFacade`，统一租户、数据源、审计字段和幂等控制。
- 代码优先复杂业务默认不允许平台直接 `UPDATE table SET statusField = ?`，除非业务模块明确选择 `BUSINESS_TABLE` 简化模式。

### 5.3 businessKey 规则

统一使用：

```text
${objectCode}:${recordKey}
```

要求：

- `objectCode` 必须是发布态业务对象编码，不能使用页面临时 `configKey`。
- `recordKey` 首期兼容 `Long`，后续必须升级为 `String`，支持自定义单字段主键。
- 解析工具集中到 `BusinessKeyCodec`，禁止散落 `split(":", 2)`。
- `businessKey` 不承载租户，租户来自 `tenantId` 字段和租户上下文。

### 5.4 流程变量规则

系统变量统一注入：

| 变量 | 来源 |
| --- | --- |
| `tenantId` | 当前租户 |
| `objectCode` | 业务对象 |
| `configKey` | 发布态动态 CRUD 配置 |
| `recordKey` | 业务记录主键，字符串 |
| `recordId` | 兼容字段，仅 Long 主键时保留 |
| `businessKey` | `${objectCode}:${recordKey}` |
| `startUserId` | 发起人 |
| `startDeptId` | 发起部门 |

业务变量来自 `variableMapping`。业务相关审批人必须在 Business 侧提前解析为变量，BPMN 只消费变量，不直接查业务表。

### 5.5 状态机规则

标准单据状态：

| 标准状态 | 说明 |
| --- | --- |
| `DRAFT` | 草稿，可编辑、可删除、可发起流程 |
| `IN_PROCESS` | 流程中，主数据默认只读 |
| `REJECTED` | 已驳回，可按策略修改后重提 |
| `APPROVED` | 已通过，不可编辑 |
| `CANCELED` | 已撤回/取消 |
| `CLOSED` | 业务关闭 |

事件映射：

| Flow 事件 | 条件 | 业务动作 |
| --- | --- | --- |
| `PROCESS_STARTED` | 发起成功 | 写关联记录，状态置 `IN_PROCESS` |
| `TASK_CREATED` | 任意节点 | 可记录节点待办，不直接改主状态 |
| `TASK_COMPLETED` | `approvalResult=reject` 且目标为修改节点 | 状态置 `REJECTED`，流程仍为 `RUNNING` |
| `TASK_COMPLETED` | 修改节点重提 | 保存业务数据，更新流程变量，状态置 `IN_PROCESS` |
| `PROCESS_COMPLETED` | 非 reject 终态 | 状态置 `APPROVED`，发布 `FLOW_APPROVED` |
| `PROCESS_COMPLETED` | reject 终态 | 状态置 `REJECTED`，发布 `FLOW_REJECTED` |
| `PROCESS_CANCELED` | 撤回/取消 | 状态置 `CANCELED`，发布 `FLOW_CANCELED` |

### 5.6 待办表单上下文

在 `TaskFormInfo.formType` 基础上新增低代码业务表单类型：

```text
BUSINESS_OBJECT_FORM
```

返回字段：

```json
{
  "formType": "BUSINESS_OBJECT_FORM",
  "formRef": {
    "type": "BUSINESS_OBJECT_FORM",
    "code": "crm_opportunity_default",
    "viewKey": "approval_detail"
  },
  "objectCode": "crm_opportunity",
  "configKey": "crm_opportunity_runtime",
  "recordKey": "10001",
  "businessKey": "crm_opportunity:10001",
  "viewKey": "approval_detail",
  "mode": "APPROVE",
  "fieldPermissions": {},
  "runtimeRules": {},
  "editMode": "READONLY_APPROVE",
  "processInstanceId": "...",
  "taskId": "..."
}
```

前端待办页据此复用低代码表单渲染器或代码表单 Provider，而不是要求每条流程配置外部 Vue 页面。

首期落地采用“Flow 基础任务表单 + 业务侧表单上下文”双段解析：

- `forge-plugin-flow` 继续只输出通用 `TaskFormInfo`，不反向依赖低代码/业务插件。
- 待办页读取 `TaskFormInfo` 后，再调用 `GET /ai/business/flow/task-form-context`，由业务侧按 `businessKey/processInstanceId/taskDefKey` 解析 `nodeForms`、业务记录、低代码表单字段和字段权限。
- 低代码业务表单保存走 `PUT /ai/business/flow/task-form-context`，后端只写当前节点配置为 `writable=true` 的字段。
- `BUSINESS_CODE_FORM` 保持 Provider 协议预留，复杂代码业务后续通过注册目录或跳转业务详情页接入，仍不让普通用户手工输入组件路径。

## 6. 数据模型落地

### 6.1 首期复用与扩展现有表

不新增重复的“流程业务绑定表”，优先复用：

| 表 | 当前用途 | 目标用途 |
| --- | --- | --- |
| `ai_business_binding` | 对象能力绑定，含 `FLOW` | 保存主流程契约概要和兼容字段 |
| `ai_business_document_config` | 单据配置、状态映射 | 保存单据生命周期配置 |
| `ai_business_flow_instance_link` | 单据与流程实例关联 | 扩展为流程实例运行态主索引 |
| `ai_business_trigger` | 触发器 | 消费 `FLOW_*` 业务事件执行副作用 |
| `ai_business_trigger_log` | 触发器日志 | 增加流程链路关联和 TODO/失败原因 |
| `ai_crud_config` | 动态 CRUD 发布态 | 挂接运行数据源、对象编码、页面配置 |
| `ai_business_object_design_version` | 设计版本快照 | 保存完整流程契约设计态快照 |
| `ai_business_form_asset` | 可新增轻量索引表 | 保存低代码/Flow/外部表单资产元数据；代码表单可由 Provider 运行时注册并同步展示 |

### 6.2 建议扩展字段

`ai_business_flow_instance_link` 建议扩展：

| 字段 | 说明 |
| --- | --- |
| `record_key varchar(128)` | 通用主键字符串，替代长期依赖 `record_id bigint` |
| `config_key varchar(128)` | 发布态动态 CRUD 配置 |
| `flow_definition_id varchar(128)` | 实际启动的流程定义 ID |
| `flow_definition_version int` | 实际启动版本 |
| `contract_version_id bigint` | 关联发布态契约版本 |
| `correlation_id varchar(64)` | 发起、回调、触发器、消息共用链路 ID |
| `current_task_key varchar(128)` | 当前关键任务节点 |
| `last_event_type varchar(64)` | 最近一次事件 |
| `last_event_time datetime` | 最近事件时间 |

`ai_business_binding.binding_config` 建议扩展：

- `schemaVersion`
- `recordKey`
- `mainFlow`
- `variableMapping`
- `nodeForms`
- `events`
- `options`

后续如需要流程契约版本独立发布，再新增：

```text
ai_business_flow_contract_version
```

用于锁定“业务对象版本 -> 流程模型版本 -> 表单权限版本 -> 状态映射版本”，避免运行中流程被设计态变更影响。

## 7. 后端接口设计

### 7.1 配置接口

沿用并增强现有 `/ai/business/flow`：

| 接口 | 用途 |
| --- | --- |
| `GET /ai/business/flow/binding/{objectCode}` | 查询流程契约概要 |
| `PUT /ai/business/flow/binding/{objectCode}` | 保存流程契约草稿 |
| `GET /ai/business/flow/model/{modelKey}/variables` | 解析流程变量候选项 |
| `POST /ai/business/flow/contract/{objectCode}/validate` | 发布前校验流程契约 |
| `POST /ai/business/flow/contract/{objectCode}/publish` | 编译并发布流程契约快照 |
| `GET /ai/business/form-assets` | 查询可选表单资产，支持按 `objectCode/businessType/source` 过滤 |
| `GET /ai/business/form-assets/{formCode}/views` | 查询表单可选视图，如审批详情、负责人补充、驳回修改 |
| `GET /ai/business/form-assets/{formCode}/field-catalog` | 查询表单字段或虚拟区域目录，供节点权限配置 |

### 7.2 运行接口

| 接口 | 用途 |
| --- | --- |
| `POST /ai/business/flow/start` | 手动或触发器发起主流程 |
| `POST /ai/business/flow/{businessKey}/withdraw` | 发起人撤回 |
| `POST /ai/business/flow/{businessKey}/cancel` | 业务取消 |
| `POST /ai/business/flow/resubmit` | 驳回修改后重提，复杂业务页保存后调用 |
| `GET /ai/business/flow/status/{objectCode}/{recordKey}` | 查询流程运行态 |
| `GET /ai/business/flow/task-form-context` | 给待办页返回业务表单上下文 |
| `PUT /ai/business/flow/task-form-context` | 保存当前任务节点授权的业务字段 |

### 7.3 内部回调接口

两种方式必须统一到同一个 service：

| 入口 | 场景 |
| --- | --- |
| Redis `flow:event:all` | Admin 与 Flow 共 Redis 或分服务部署 |
| `POST /ai/business/flow/callback` | Flow Webhook 或兼容手动回调 |

内部统一调用：

```java
BusinessFlowEventBridge.handle(FlowEventContext context)
```

不允许页面、触发器、手写业务 Service 分别实现状态回写。

### 7.4 代码优先业务接入面

复杂业务不走低代码对象契约时，提供三种接入方式，按复杂度递进：

| 模式 | 适用场景 | 特点 |
| --- | --- | --- |
| Gateway API 模式 | 业务 Service 已有完整状态机，只需要发起流程和查询进度 | 业务代码主动调用 `BusinessFlowGateway.startCodeFirstFlow(...)`，自己处理状态更新。 |
| Adapter 模式 | 需要复用平台事件桥、trace、待办表单和标准状态回调 | 实现 `BusinessFlowAdapter`，平台统一调用 `validateBeforeStart/buildVariables/onApproved/onRejected/onCanceled`。 |
| Annotation 兼容模式 | 已有 `@FlowBind/@FlowCallback` 老代码 | 保留可用，但新复杂业务优先转 Adapter，避免事件处理散落。 |

建议接口：

```java
public interface BusinessFlowAdapter {

    String businessType();

    String objectCode();

    String modelKey();

    String buildBusinessKey(Object businessId);

    FlowBusinessSnapshot loadBusiness(String businessKey);

    Map<String, Object> buildStartVariables(FlowStartRequest request);

    void validateBeforeStart(FlowStartRequest request);

    void markInProcess(FlowStartRequest request, FlowStartResult result);

    void onTaskCompleted(FlowEventContext context);

    void onApproved(FlowEventContext context);

    void onRejected(FlowEventContext context);

    void onCanceled(FlowEventContext context);

    default BusinessTaskFormContext buildTaskFormContext(FlowEventContext context) {
        return BusinessTaskFormContext.externalDetail();
    }
}
```

代码优先业务推荐调用链：

```text
业务 Controller
    -> 业务 Service 校验和保存草稿
    -> BusinessFlowGateway.start(adapterCode, businessId, variables)
    -> FlowClient.startProcess
    -> BusinessFlowEventBridge
    -> BusinessFlowAdapterRegistry
    -> 业务 Adapter 回写状态/执行副作用
```

设计约束：

- Adapter 的 `onApproved/onRejected/onCanceled` 必须幂等。
- Adapter 内查询类 SQL 仍按项目规范写 Mapper XML，不在 Service 拼复杂查询。
- 资金、库存、权限放开等高风险副作用必须在业务 Service 内显式审计，不能只靠流程回调默认处理。
- 待办页业务详情可以返回外部业务详情路由，也可以返回 `BUSINESS_CODE_FORM` 上下文，由业务模块提供只读/编辑组件。
- 平台只保存流程实例关联、trace 和流程上下文，不强制保存业务主数据。

### 7.5 表单资产选择与节点字段权限

当前流程配置里让用户手工输入 `formUrl` 或组件路径不合理。业务用户不知道表单地址，开发者也容易输入错路径，后续迁移路由时还会造成历史 BPMN 失效。

目标设计是：流程节点不直接保存 URL，而是保存“表单资产引用 + 节点表单策略”。

#### 7.5.1 表单资产注册

建立统一表单资产目录，来源可以是低代码、Flow 动态表单或代码表单。

| 表单来源 | 示例 | 面向用户的选择方式 | 运行时解析 |
| --- | --- | --- | --- |
| 低代码业务表单 | 商机详情表单、采购合同编辑表单 | 下拉选择业务对象 + 表单视图 | `BUSINESS_OBJECT_FORM` |
| Flow 动态表单 | 简单请假表单、节点补充信息表单 | 下拉选择流程表单 | `dynamic/formKey` |
| 代码表单 | 采购合同审批详情、合同变更页 | 下拉选择代码注册的表单资产 | `BUSINESS_CODE_FORM` |
| 外部地址 | 历史系统页面、第三方页面 | 高级模式输入 URL | `external/formUrl` |

代码表单由业务模块显式注册，不让用户输入路径。首期服务端扩展点已落地为 `BusinessCodeFormProvider`：

```java
public interface BusinessCodeFormProvider {

    String providerKey();

    default String providerName();

    default List<Map<String, Object>> formAssets(String objectCode);

    BusinessTaskFormContextVO buildContext(BusinessTaskFormContextQueryDTO query,
                                           Map<String, Object> formRef,
                                           List<Map<String, Object>> fieldPermissions);

    default BusinessTaskFormContextVO saveContext(BusinessTaskFormSaveDTO dto,
                                                  Map<String, Object> formRef,
                                                  List<Map<String, Object>> fieldPermissions);
}
```

业务模块注册 Provider 后，`GET /ai/business/flow/form-assets/{objectCode}` 会合并代码表单资产；待办页解析到 `BUSINESS_CODE_FORM` 时由 Provider 返回业务页地址、字段/区域目录或自定义上下文。

注册方式可以有两种：

- 注解注册：`@BusinessForm(formCode="purchase_contract_approval", formName="采购合同审批表单")`
- 配置注册：`business-flow.forms[]`，适合不方便加注解的历史模块。

流程设计器节点表单配置应展示为选择器：

```text
表单类型：继承业务主表单 / 低代码表单 / 流程动态表单 / 代码表单 / 外部地址
表单资产：采购合同审批表单
表单视图：审批详情 / 负责人补充 / 驳回修改
```

只有“外部地址”作为高级模式保留手工输入。

#### 7.5.2 节点表单策略

每个人工节点保存 `NodeFormPolicy`：

```json
{
  "taskDefKey": "contract_owner_fill_task",
  "formRef": {
    "type": "BUSINESS_CODE_FORM",
    "code": "purchase_contract_approval",
    "viewKey": "owner_fill"
  },
  "mode": "FILL",
  "fieldPolicies": [
    {
      "field": "contractName",
      "visible": true,
      "editable": false,
      "required": false
    },
    {
      "field": "purchaseListAttachment",
      "visible": true,
      "editable": true,
      "required": true
    }
  ],
  "actionPolicies": {
    "approve": false,
    "submit": true,
    "reject": false
  }
}
```

字段策略分层：

1. 表单设计默认权限：字段是否默认显示、默认必填。
2. 节点字段权限：当前节点覆盖默认权限，支持可见、只读、可编辑、必填。
3. 运行时规则：按业务状态、用户、金额、部门等动态隐藏或只读。
4. 后端提交校验：最终以服务端策略为准，前端只负责渲染。

对于代码表单，字段目录由 `BusinessCodeFormProvider.formAssets()` 提供。复杂页面如果无法拆成字段，可以把“区域、附件区、操作按钮”抽象成虚拟字段：

```text
baseInfoSection
contractAmount
purchaseListAttachment
riskReviewSection
submitButton
```

这样流程节点仍能统一控制“哪个节点显示哪个区域、哪个字段可编辑”。

#### 7.5.3 采购合同审批示例

采购合同审批建议拆成几个节点：

| 节点 | 节点类型 | 表单策略 |
| --- | --- | --- |
| 发起节点 | 填报 | 合同基础信息、金额、供应商、附件可编辑 |
| 负责人补充清单 | 办理/填写任务 | 只让负责人编辑 `purchaseListAttachment`，其他字段只读 |
| 部门负责人审批 | 审批 | 基础信息和清单可见，全部只读，只填审批意见 |
| 法务审批 | 审批 | 合同条款、附件可见，只读，可上传法务意见附件 |
| 财务审批 | 审批 | 金额、付款条款可见，只读，可填财务意见 |

“只有负责人节点显示上传清单”不建议靠不同 Vue 路径解决，而应靠同一个表单资产 + 不同 `viewKey/fieldPolicies` 控制。这样后续新增节点时不用新增页面。

#### 7.5.4 驳回修改的产品策略

主流 BPM / 低代码产品的共同思路是“任务绑定表单资产”，而不是让普通用户维护页面路径。Camunda 的 Web Modeler 支持把用户任务链接到同项目表单，并明确推荐 linked form，避免每次变更都复制 JSON；自定义 form key 更偏向外部应用集成。Flowable Form 也以表单 key、字段、outcomes 描述表单定义，任务页展示当前任务对应表单，`Save` 和 `Complete` 分离，outcome 变量再驱动后续流转。参考：

- Camunda form linking: https://docs.camunda.io/docs/8.7/components/modeler/web-modeler/advanced-modeling/form-linking/
- Camunda Forms: https://docs.camunda.io/docs/components/modeler/forms/camunda-forms-reference/
- Flowable Form Introduction: https://www.flowable.com/open-source/docs/form/ch06-Form-Introduction/
- Flowable task form/work tasks: https://documentation.flowable.com/latest/user/work/work-tasks

因此 Forge 的产品默认策略应是：

- 设计态选择表单资产，保存 `formRef`，不保存裸 `formUrl` 作为主配置。
- 待办页按当前任务解析表单资产、字段权限、动作按钮和流程上下文。
- 代码表单通过 Provider 暴露可控字段/区域目录，外部 URL 只作为高级兼容能力。
- 审批动作和业务修改动作分离，`保存草稿`、`完成任务`、`保存并重提` 是不同语义。

主流做法不是让任意审批人在任意审批表单里随意修改业务主数据，而是区分三类动作：

| 动作 | 适用场景 | 推荐设计 |
| --- | --- | --- |
| 审批意见 | 审批人判断通过/拒绝 | 审批节点只写意见、签名、审批附件，不改主数据 |
| 补充资料/办理任务 | 流程中某个角色需要补材料，如负责人上传清单 | 建模为“办理/填写任务节点”，只开放指定字段 |
| 驳回修改 | 申请单核心内容有误，需要发起人或指定角色修改 | 回退到“修改节点”，使用专门的修改视图 |

驳回修改提供两种模式：

1. **内嵌修改模式**：适合低代码表单、字段少、无复杂事务。用户在待办的“修改任务”里直接编辑允许字段，点击“保存并重提”，系统保存业务数据、更新流程变量、完成修改节点。
2. **跳转业务表单模式**：适合采购合同、订单、库存、价格测算等复杂代码业务。待办里展示驳回原因和“去业务单据修改”按钮，打开业务模块自己的编辑页；业务页保存后调用 `BusinessFlowGateway.resubmit(...)` 完成修改节点。

默认建议：

- 低代码对象使用内嵌修改模式。
- 代码优先复杂业务使用跳转业务表单模式，除非业务模块提供了可安全嵌入的 `BUSINESS_CODE_FORM` 修改视图。
- 驳回时必须明确目标：退回发起人、退回上一节点、退回指定办理节点。不要把“拒绝终止”和“退回修改”混成一个按钮。

最终推荐口径：

```text
审批节点：默认只读业务主数据，只写审批意见/审批附件。
办理节点：按节点字段权限补充指定业务字段，如负责人上传清单。
驳回修改节点：低代码简单表单可在待办中内嵌修改；复杂代码业务默认跳回业务单据页修改。
```

## 8. 前端设计

### 8.1 设计器主链路

对象设计器建议调整为一条流程类应用搭建向导：

1. 表单创建：字段、布局、校验、联动、公式。
2. 单据生命周期：状态字段、编号、发起人、负责人、编辑/删除/发起策略。
3. 流程配置：选择模板或已有流程，配置变量映射、标题、发起方式。
4. 节点表单权限：每个审批节点选择表单资产、表单视图和字段权限。
5. 自动化：流程通过/驳回/撤回后的触发器、消息和关联记录动作。
6. 权限：菜单、行操作按钮、数据权限、审批角色可见性。
7. 发布检查：一次性检查表单、状态、流程、变量、权限、触发器。

### 8.2 待办审批页

待办页优先渲染：

```text
TaskFormInfo.formType = BUSINESS_OBJECT_FORM
TaskFormInfo.formType = BUSINESS_CODE_FORM
```

页面布局：

- 左侧：业务数据表单，只读/可编辑由节点字段权限控制。
- 右侧：审批记录、流程图、当前节点信息、审批动作。
- 底部：通过、驳回、退回、转办、撤回等动作。

不再要求每个流程都写一个外部审批表单页面。
复杂代码业务如果选择 `BUSINESS_PAGE_MODIFY`，待办页左侧展示只读摘要和驳回原因，提供“去业务单据修改”动作；业务页保存后调用重提接口。

### 8.3 运行态详情页

继续复用 `AiCrudPage + AiCrudFlowDetail`：

- 业务数据 Tab。
- 流程进度 Tab。
- 触发器/消息日志可以作为后续扩展 Tab。

## 9. 流程模板库

模板不只是 BPMN XML，还应包含变量、节点权限和检查规则。

| 模板 | 适用场景 | 默认能力 |
| --- | --- | --- |
| `serial_approval` | 普通串行审批 | 发起人 -> 审批人 A -> 审批人 B -> 结束 |
| `countersign_approval` | 多人会签 | `multiInstanceLoopCharacteristics`，全部通过才完成 |
| `parallel_or_approval` | 并行或签 | 多分支任一通过即进入结束路径 |
| `reject_modify_resubmit` | 驳回修改重提 | 固定 `initiator_modify_task`，重提复用原实例 |
| `approval_with_cc` | 审批后抄送 | 流程通过后由 Flow 统一抄送 |
| `approval_post_action` | 审批后处理 | 通过后触发 `FLOW_APPROVED` 自动化 |

节点命名建议：

```text
start_event
submit_task
dept_manager_task
finance_task
initiator_modify_task
cc_task
end_event
```

## 10. 发布检查

发布前必须检查：

- 业务对象已发布或可同步发布。
- 单据模式启用时，状态字段存在且有状态映射。
- `allowStartFlow=true` 的状态至少一个。
- 流程模型存在且已发布。
- 变量映射源字段存在。
- BPMN 中引用的变量要么由系统变量提供，要么由变量映射提供，要么由默认值/表达式提供。
- 业务相关审批人变量不得由 BPMN 直接查询业务表。
- 节点表单权限引用字段存在。
- 节点 `formRef` 引用的表单资产存在，且资产类型与运行时能力匹配。
- 代码表单资产必须提供字段目录；复杂区块至少提供虚拟字段/区域目录。
- 外部 URL 表单只能在高级模式配置，并提示无法参与字段级权限校验。
- 驳回修改策略配置了 `modifyTaskKey`，且 BPMN 存在该节点。
- 驳回修改模式必须明确是 `INLINE_MODIFY` 还是 `BUSINESS_PAGE_MODIFY`。
- `START_FLOW` 按钮权限存在。
- 触发器动作引用的目标对象、字段、消息通道存在。
- 运行数据源可用，主键字段可解析。

## 11. 可观测性

新增统一 trace 维度：

```text
correlationId = flowStart:{tenantId}:{businessKey}:{timestamp or snowflake}
```

链路日志至少覆盖：

- 发起请求来源：手动按钮、触发器、FlowEntry。
- 业务记录快照和变量快照。
- FlowClient 请求与返回。
- `ai_business_flow_instance_link` 写入。
- Flow 事件接收和幂等判断。
- 状态回写前后值。
- 业务事件发布。
- 触发器执行。
- 消息推送。

可先落在 `ai_business_trigger_log` 或新增轻量 `ai_business_flow_trace_log`，后续再接入监控页面。

## 12. 安全与权限

- 流程发起必须校验按钮权限和单据状态，前端禁用只作为体验层。
- 待办审批页业务详情必须校验当前用户是当前任务处理人、候选人、抄送人、发起人或具备业务详情权限。
- 节点字段权限由后端输出，前端只负责渲染，不作为安全唯一依据。
- 业务状态回写使用内部方法，禁止走普通用户可调用的动态 CRUD 更新接口。
- API Key、外部消息通道密钥不能进入流程变量和前端响应。
- 跨数据源业务记录和 Flow 状态不做分布式强事务，采用幂等事件和补偿。

## 13. 迁移策略

### 13.1 对已有低代码对象

- 保留现有 `ai_business_binding` 和 `ai_business_document_config`。
- 首次打开流程配置时，将旧字段归一化为 `BusinessFlowContract`。
- 保存时继续写兼容字段：`flowModelKey`、`titleTemplate`、`startMode`、`variableMapping`。
- 发布时生成契约快照。

### 13.2 对已有流程实例

- 保留 `recordId` 读写。
- 新增 `recordKey` 后，用 `recordId` 回填字符串值。
- 运行中流程继续按启动时 `flowModelKey` 和实例关联处理，不受新设计态变更影响。

### 13.3 对手写业务模块

- 保留 `@FlowBind/@FlowCallback` 能力作为兼容层。
- 新复杂业务优先实现 `BusinessFlowAdapter` 并注册到 `BusinessFlowAdapterRegistry`。
- 手写模块保留自己的实体、Mapper XML、Service、事务和状态机，不迁移到动态 CRUD。
- 手写模块通过 `BusinessFlowGateway` 发起流程，通过 `BusinessFlowEventBridge` 接收事件。
- 如果待办页要展示专属复杂业务表单，Adapter 返回外部详情路由或 `BUSINESS_CODE_FORM` 上下文。
- 平台统一提供流程实例关联、trace、消息、流程历史和验收模板。

## 14. 风险

| 风险 | 影响 | 处理 |
| --- | --- | --- |
| 自定义主键未贯通 | 低代码多库对象无法发起流程 | 将 `recordId Long` 升级为 `recordKey String`，保留兼容字段。 |
| Flow 事件乱序或重复 | 状态被错误回写 | 引入 `eventId/correlationId/last_event_time` 和幂等表/字段。 |
| 设计态变更影响运行中实例 | 审批页表单和变量错位 | 启动流程时固化契约版本和表单快照。 |
| 跨库无分布式事务 | 发起成功但状态未回写 | 使用本地事务 + 幂等补偿 + 可重放 trace。 |
| 待办页绕过业务权限 | 敏感数据泄露 | 后端输出表单上下文时校验 `taskId`、任务状态、当前办理人、流程实例、业务 Key 和节点 Key；保存和重提必须由已签收办理人执行。 |
| BPMN 模板表达力不足 | 复杂流程仍需手工 | 模板覆盖高频场景，保留 Flow 设计器高级模式。 |

## 15. 结论

参考文档提出的优化方向合理，但在 Forge 中不能停留在“每个业务 Service 继承一个抽象类”。Forge 当前已经有低代码运行时和业务应用平台，正确的落地方式是：

1. 以业务对象为中心沉淀 `BusinessFlowContract`。
2. 以 `BusinessFlowGateway` 统一手动、触发器、流程入口三类发起方式。
3. 以 `BusinessFlowEventBridge` 消费 Flow 全量事件，统一状态回写和副作用。
4. 以 `BUSINESS_OBJECT_FORM` 打通待办审批页和低代码业务表单。
5. 以发布检查和模板库把“表单创建 -> 流程配置”变成低代码完整链路。

这样既能解决当前“业务模块与流程模块耦合严重、新增流程多处修改、复用性低”的痛点，也能支撑长期流程类低代码应用搭建。
