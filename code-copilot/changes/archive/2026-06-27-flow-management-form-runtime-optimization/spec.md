# 流程管理表单运行态与业务入口优化
> status: done
> created: 2026-06-06
> complexity: 🔴复杂
> related: `code-copilot/changes/archive/flow-management-dynamic-form-ux/spec.md`, `code-copilot/changes/lowcode-app-full-loop-optimization/spec.md`

## 1. 背景与目标

当前流程管理已经具备流程模型、BPMN 设计器、流程表单、节点动态表单、待办/已办、流程实例和消息中心能力，但仍缺少“业务人员配置一个全局动态表单，并把它作为流程入口运行”的完整闭环。

本次目标是把流程管理从“流程图 + 节点配置”升级为“流程应用入口 + 表单填报 + 流程流转 + 实例详情 + 可选业务落表”的运行平台：

- 支持流程全局动态表单，表单既可以只作为流程变量和审计快照使用，也可以映射到低代码业务对象或实际业务表。
- 流程设计时，审批人、候选人、流转条件、标题模板和消息模板可以引用当前流程表单字段。
- 流程可以发布为某个业务入口，用户进入入口后直接填表并发起流程，后续可查看每次填报单据的表单详情、流程图和流转记录。
- 支持“管理员下发组织填报任务”的场景：每个组织负责人填报本部门绩效考核，提交后落到实际业务表，再上报管理员审核。
- 优化流程设计和办理体验：流程设计器从独立页面改为全屏弹窗；待办、已办、我发起详情不再使用抽屉。
- 生成待办任务时强制推送站内信，不依赖手动催办或流程事件外部订阅。

## 2. 代码现状（Research Findings）

### 2.1 流程表单已有定义，但缺少运行态和业务映射

- `forge/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowFormController.java:35` 的表单分页接口已存在，但参数使用 `page`，不符合项目约定的 `pageNum`。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowForm.java:24` 至 `61` 已有 `formKey/formName/formType/formSchema/formUrl/version/status`，但没有字段注册表、业务对象映射、发布版本快照和表单实例数据。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowFormServiceImpl.java:27` 至 `48` 使用 `LambdaQueryWrapper` 做表单查询，后续改造必须下沉到 Mapper XML。

### 2.2 流程模型已有全局表单字段，但不是流程应用入口

- `forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowModel.java:47` 至 `59` 已有 `formType/formId/formJson`，可表达模型级表单。
- `forge-admin-ui/src/views/flow/design.vue:86` 至 `113` 可在流程设计页选择或设计动态表单。
- `forge-admin-ui/src/views/flow/design.vue:1671` 至 `1734` 保存草稿和发布部署仍绑定独立设计页面，模型列表进入设计时不是弹窗式工作区。

### 2.3 待办已能渲染动态表单，但只把数据作为任务变量提交

- `forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java:1223` 至 `1356` 会按节点表单优先、模型表单兜底解析 `TaskFormInfo`，并返回流程变量。
- `forge-admin-ui/src/views/flow/todo.vue:189` 至 `240` 已支持外置表单和动态表单渲染。
- `forge-admin-ui/src/views/flow/todo.vue:599` 至 `608` 打开待办时用流程变量初始化表单数据。
- `forge-admin-ui/src/views/flow/todo.vue:717` 至 `759` 审批通过时收集动态表单变量并提交，但没有保存不可变表单实例快照，也没有回写业务表。

### 2.4 节点表单配置已存在，但审批人和条件缺少表单字段目录

- `forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue:328` 至 `408` 用户任务节点支持引用表单、在线设计节点表单和高级 JSON。
- `forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue:1715` 至 `1749` 会把表单配置保存为 `flowable:formKey/formJson/formUrl`。
- 当前审批人、候选人和条件配置仍主要依赖手写变量或表达式，缺少由“流程表单字段 + 业务对象字段 + 系统变量”组成的可选变量目录。

### 2.5 低代码业务对象已有流程绑定和实际落表链路

- `forge/db/migration/V1.0.47__add_business_document_flow_closure.sql:3` 至 `55` 已创建 `ai_business_document_config` 和 `ai_business_flow_instance_link`，用于业务单据配置和业务记录到流程实例的关联。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java:91` 至 `137` 已能从业务对象记录发起流程，业务键格式为 `objectCode:recordId`。
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue:33` 至 `80` 已提供业务对象默认流程和变量映射配置。
- `forge-admin-ui/src/views/app-center/components/designer/FlowVariableMappingEditor.vue:16` 至 `33` 已有“单据字段 -> 流程变量”的映射编辑 UI，可复用到表单字段映射。

### 2.6 待办站内信已有消息服务，但任务创建时未强制推送

- `forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java:199` 至 `220` 创建 `sys_flow_task` 后只发布 `TASK_CREATED` 事件。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java:665` 至 `689` 事件通知依赖流程模型的 `notifyType=redis/webhook`。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java:1046` 至 `1090` 只有“催办”会主动调用 `MessageService.send` 发送站内信。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/java/com/mdframe/forge/plugin/message/service/impl/MessageServiceImpl.java:77` 至 `94` 已支持创建消息、接收人和发送记录，`WEB` 渠道可作为站内信复用。

### 2.7 现有接口有规范债务

- `forge/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowInstanceController.java:175` 至 `198` 的流程实例分页使用 `LambdaQueryWrapper`，如本次扩展查询条件，应迁移到 Mapper XML。
- `forge/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowTaskController.java:39` 至 `49` 待办分页已使用 `pageNum/pageSize`，应作为本次新增接口的命名参考。

## 3. 核心概念

### 3.1 流程表单定义

全局可复用的动态表单资产。表单字段的 `field` 是流程变量、条件表达式、审批人表达式、消息模板和业务字段映射的统一变量名。

### 3.2 表单发布版本

流程实例必须绑定提交时的表单版本和 Schema 快照。后续表单被修改，不影响历史实例详情展示和审计。

### 3.3 流程应用入口

面向业务用户的填报入口由业务承载方提供：低代码应用入口、业务对象详情/列表操作，或后续专门的“流程发起中心”。流程模型页只维护流程定义、模型级表单和流程设计配置，不直接承担“入口显示在哪里”的应用导航配置，避免与低代码应用入口重复。

### 3.4 数据模式

- `PROCESS_ONLY`：不落实际业务表，只保存流程变量和表单实例快照，用于请假申请等轻量流程。说明：这里“不落库”指不落业务表；为了详情展示、流程变量和审计，平台仍会保存流程运行数据和表单快照。
- `BUSINESS_OBJECT`：提交时通过低代码动态 CRUD 创建或更新业务记录，并用 `objectCode:recordId` 发起流程。
- `HYBRID`：既保存流程表单实例快照，也映射到业务对象记录，适合需要审计原始填报内容且要参与业务统计的场景。

### 3.5 组织批量填报任务

这是“流程入口 + 业务对象落表 + 组织负责人提交”的业务场景示例，不作为流程管理的独立内置菜单。平台底层可以保留批次、组织负责人、明细状态等编排能力，但具体入口应由业务对象/业务应用承载。

## 4. 功能点

### 4.1 全局流程表单增强

- [ ] 表单管理页支持维护表单分类、字段目录、表单用途、默认数据模式、版本发布状态。
- [ ] 流程表单设计器必须复用低代码表单设计器的业务组件注册能力，包含系统字典选择、行政区划、组织选择、人员选择、文件/图片上传和引用对象等组件。
- [ ] 流程表单预览、发起测试、待办办理和详情展示必须对字典、组织、人员等业务组件补齐运行态选项加载，不能只在设计器中可见。
- [ ] 流程表单字段默认行间距不能过密；设计器表单配置中必须允许统一设置字段行间距，并在预览、发起测试和办理页生效。
- [ ] 表单字段目录从 form-create rules 解析生成，至少包含字段名、标题、组件类型、数据类型、是否必填、选项来源。
- [ ] 表单保存草稿和发布版本分离；流程入口只能绑定已发布表单版本。
- [ ] 表单版本发布后不可修改，修改表单生成新版本。
- [ ] 表单分页接口改为 `pageNum/pageSize`，查询 SQL 写入 Mapper XML。

### 4.2 表单字段驱动流程设计

- [ ] 流程模型绑定表单后，节点属性面板可读取当前表单字段目录。
- [ ] 审批人配置支持从表单字段中选择“用户字段、部门字段、角色字段、区域字段”，并生成受控表达式。
- [ ] 流转条件支持字段、操作符、目标值的结构化配置，并生成 Flowable 条件表达式。
- [ ] 条件表达式必须只引用已注册变量；发布部署前校验缺失变量。
- [ ] 变量目录同时展示系统变量：发起人、发起部门、发起组织、发起人角色、区域编码、业务键、流程实例 ID。
- [ ] 支持消息标题和内容模板引用表单字段，例如 `${leaveDays}`、`${deptName}`。

### 4.3 流程应用入口

- [ ] 流程模型页不提供独立“入口配置”操作；流程模型只维护模型级表单、字段目录、流程图和节点配置。
- [ ] 流程模型页允许对已部署模型执行“发起测试”，用于设计人员按当前模型级表单填写变量并验证流程流转；该入口固定标记为测试发起，不承担业务菜单、可见范围或正式应用入口配置。
- [ ] 业务入口由低代码应用入口、业务对象操作或后续流程发起中心挂载，入口展示位置不在流程模型页配置。
- [ ] `BUSINESS_OBJECT/HYBRID` 的入口和可见范围优先复用低代码应用/业务对象配置，避免重复维护应用导航。
- [ ] `PROCESS_ONLY` 后续需要独立设计轻量流程发起中心或应用挂载协议，再开放给业务用户发起。
- [ ] 用户进入入口后直接看到填报表单，不需要先进入流程模型列表。
- [ ] 提交入口表单后，平台生成业务键、表单实例快照、流程变量，并发起流程。
- [ ] 用户可在“我发起的”或业务入口详情中查看每次填报单据的表单详情、流程图和流转记录。
- [ ] 流程管理模块自身不注册独立入口列表菜单，也不在模型页配置应用菜单入口。

### 4.4 PROCESS_ONLY 运行态

- [ ] 提交表单时创建 `sys_flow_form_instance`，保存表单版本、Schema 快照、表单数据、发起人、发起组织、流程模型 Key、业务键。
- [ ] `businessKey` 默认格式为 `FLOW_FORM:{entryCode}:{instanceId}`。
- [ ] 流程变量来自表单数据、系统变量和业务承载方传入的入口上下文变量。
- [ ] 流程启动成功后回写 `process_instance_id` 和状态。
- [ ] 待办、已办、我发起详情可以根据 `processInstanceId` 查询表单实例快照并只读展示。
- [ ] 流程结束后更新表单实例状态为通过、驳回、撤回或终止。

### 4.5 BUSINESS_OBJECT / HYBRID 运行态

- [ ] 入口绑定业务对象 `objectCode/configKey`，不允许业务用户直接输入物理表名。
- [ ] 表单字段到业务对象字段的映射复用 `formField/flowVariable` 思路，新增 `formField/businessField` 映射。
- [ ] 提交时先按映射构建业务记录，通过 `DynamicCrudService` 创建或更新实际业务记录。
- [ ] 业务记录创建成功后按 `objectCode:recordId` 发起流程，复用 `BusinessFlowService` 和 `ai_business_flow_instance_link`。
- [ ] HYBRID 模式同时保留 `sys_flow_form_instance` 快照，快照中记录 `objectCode/recordId`。
- [ ] 审批通过、驳回、撤回后的状态回写继续复用业务单据状态映射，不新增独立审批引擎。

### 4.6 组织批量填报任务

- [ ] 本场景不新增流程管理独立页面；作为业务应用/业务对象的扩展入口设计。
- [ ] 如业务应用启用批量下发，负责人规则默认复用 `sys_org` 负责人字段。
- [ ] 平台底层可记录批次、组织、负责人、业务记录 ID、流程实例 ID 和填报状态。
- [ ] 创建批次后给每个负责人发送站内信，消息跳转到对应业务填报入口。
- [ ] 负责人提交表单后，按 BUSINESS_OBJECT/HYBRID 模式写入实际业务表并发起流程。
- [ ] 管理员汇总视图应由具体业务页面承载，而不是流程管理内置“组织填报批次”菜单。
- [ ] 同一批次同一组织默认只能有一条有效填报明细，唯一键为 `tenant_id + batch_id + org_id`。
- [ ] 被驳回后可按配置允许原负责人重新提交，并保留历史流程实例。

### 4.7 待办/已办/我发起详情 UI 改造

- [ ] 流程模型列表点击“设计”后打开全屏弹窗工作区，不再跳转到独立设计页面作为主路径。
- [ ] 保留设计路由作为兼容直达入口，但模型管理页默认使用弹窗。
- [ ] 流程设计器顶部流程配置常驻展示，不再默认折叠；配置区分为流程属性、表单配置和说明，表单配置需要直接暴露表单类型、已有表单、设计表单、预览和配置状态。
- [ ] 待办、已办、我发起、抄送的详情和表单办理改为页面右侧内容区或全屏弹窗，不再使用抽屉。
- [ ] 办理页布局固定为：单据概要、表单详情、流程时间线、流程图、审批操作。
- [ ] 已办和我发起详情默认只读展示表单快照，不能依赖当前表单最新 Schema。
- [ ] 本阶段同步适配移动端窄屏，窄屏下使用全屏弹窗和单列布局，避免抽屉宽度不足。

### 4.8 待办任务站内信推送

- [ ] `TASK_CREATED` 生成 `sys_flow_task` 成功后，同步发送站内信。
- [ ] 有 `assignee` 时发送给指定处理人。
- [ ] 无 `assignee` 但有候选用户时发送给候选用户。
- [ ] 只有候选组时通过组织/角色解析器展开用户；无法解析时记录告警但不阻断流程。
- [ ] 消息 `bizType=FLOW_TODO`，`bizKey=taskId`，跳转地址为 `/flow/todo?taskId=:taskId`。
- [ ] 同一 `taskId` 的待办消息必须幂等，避免流程重试或事件重复导致多条消息。
- [ ] 催办仍保留，但它是补充动作，不再承担首次待办通知。

## 5. 业务规则

- 表单字段 `field` 一旦被已发布流程、入口或业务映射引用，不允许直接删除；必须先解除引用或发布新版本。
- 流程实例展示必须使用提交时表单版本和 Schema 快照。
- `PROCESS_ONLY` 的表单快照永久保留，并跟随流程实例归档策略归档，不单独设置保留期限。
- `PROCESS_ONLY` 不创建业务表记录，但必须能查看表单详情和流转记录。
- `BUSINESS_OBJECT` 模式必须先保存业务记录，拿到 `recordId` 后才能发起流程。
- 业务表落库统一走低代码业务对象和动态 CRUD 服务，不允许在流程入口中直接拼接表名或 SQL。
- 审批人和流转条件只能引用变量目录中的字段；高级表达式可保留为开发者模式。
- 组织批量填报是业务场景扩展，若启用必须记录批次、组织、负责人、截止时间和每条明细状态；流程管理不提供独立菜单。
- 状态流转、权限放开和组织填报批量下发属于高风险业务变更，进入 `/apply` 前需要人工确认。
- 所有内置数据 `tenant_id=1`。
- 所有查询类 SQL 写在 Mapper XML 中。
- 分页参数统一使用 `pageNum/pageSize`。
- AiCrudPage API 占位符统一使用 `:id`。

## 6. 数据变更

暂定新增 Flyway：`forge/db/migration/V1.0.57__add_flow_form_runtime_entry.sql`。若进入 `/apply` 时仓库已有更高版本，需顺延版本号。

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 扩展 | `sys_flow_form` | `form_category`, `field_registry`, `default_data_mode`, `publish_status`, `current_version_id` | 表单分类、字段目录、默认数据模式和发布状态 |
| 新增 | `sys_flow_form_version` | `form_id`, `form_key`, `version`, `form_schema`, `field_registry`, `form_config`, `publish_time` | 不可变表单版本 |
| 新增 | `sys_flow_entry` | `entry_code`, `entry_name`, `model_key`, `form_key`, `form_version_id`, `data_mode`, `object_code`, `config_key`, `visible_scope`, `title_template`, `business_key_template`, `status` | 流程应用入口 |
| 新增 | `sys_flow_entry_field_mapping` | `entry_id`, `form_field`, `target_type`, `target_field`, `flow_variable`, `required` | 表单字段到流程变量/业务字段映射 |
| 新增 | `sys_flow_form_instance` | `entry_code`, `business_key`, `process_instance_id`, `model_key`, `form_key`, `form_version`, `schema_snapshot`, `form_data`, `data_mode`, `object_code`, `record_id`, `status` | 每次填报实例快照 |
| 新增 | `sys_flow_fill_batch` | `entry_code`, `batch_name`, `period_key`, `target_scope`, `owner_rule`, `deadline_time`, `status` | 组织批量填报批次 |
| 新增 | `sys_flow_fill_batch_item` | `batch_id`, `org_id`, `owner_user_id`, `form_instance_id`, `object_code`, `record_id`, `process_instance_id`, `submit_status`, `flow_status`, `deadline_time` | 组织填报明细 |
| 新增/调整 | `sys_resource` | 流程入口、表单版本、批量填报、入口提交和实例查看权限 | 菜单和按钮权限，必须 NOT EXISTS |
| 新增 | `sys_dict_type/sys_dict_data` | `sys_flow_data_mode`, `sys_flow_entry_status`, `sys_flow_fill_status` | 数据模式、入口状态、填报状态字典 |

所有新增业务表必须包含 `id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`，字符集 `utf8mb4`，引擎 `InnoDB`。

## 7. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 修改 | `/api/flow/form/page` | GET | 参数改为 `pageNum/pageSize`，兼容旧 `page` 一版 |
| 新增 | `/api/flow/form/{id}/publish` | POST | 发布表单版本 |
| 新增 | `/api/flow/form/{id}/versions` | GET | 查询表单版本 |
| 新增 | `/api/flow/form/field-catalog` | GET | 根据 `formKey/versionId/modelKey` 返回字段变量目录 |
| 新增 | `/api/flow/entry/page` | GET | 流程入口分页 |
| 新增 | `/api/flow/entry` | POST/PUT | 新增和修改流程入口 |
| 新增 | `/api/flow/entry/:id` | GET/DELETE | 入口详情和删除 |
| 新增 | `/api/flow/runtime/entry/:entryCode` | GET | 获取入口运行配置和表单 Schema |
| 新增 | `/api/flow/runtime/submit/:entryCode` | POST | 提交流程入口表单并发起流程 |
| 新增 | `/api/flow/runtime/instance/:processInstanceId` | GET | 查询表单实例快照和流程详情 |
| 修改 | `/api/flow/task/form/:taskId` | GET | 增加 `formInstanceId/schemaSnapshot/formData/dataMode/objectCode/recordId` |
| 新增 | `/api/flow/fill-batch/page` | GET | 组织填报批次分页 |
| 新增 | `/api/flow/fill-batch` | POST/PUT | 创建和修改填报批次 |
| 新增 | `/api/flow/fill-batch/:id/items` | GET | 查询批次组织明细 |
| 新增 | `/api/flow/fill-batch/:id/publish` | POST | 发布批次并生成组织填报项 |
| 新增 | `/api/flow/fill-batch/item/:itemId/submit` | POST | 按批次明细提交表单 |

## 8. 影响范围

| 影响域 | 文件/模块 | 说明 |
|--------|-----------|------|
| 后端 Flow 服务 | `forge-flow/forge-flow-server` | 新增入口、运行态、批量填报接口；扩展任务表单接口 |
| Flow 插件 | `forge-plugin-flow` | 新增实体、Mapper、Service，补任务创建站内信 |
| 消息插件 | `forge-plugin-message` | 复用 `MessageService.send`，可能新增幂等查询方法 |
| 低代码生成器插件 | `forge-plugin-generator` | 复用业务对象落表、业务流程绑定和流程实例关联 |
| 前端流程管理 | `forge-admin-ui/src/views/flow` | 表单、模型、待办、已办、我发起、入口、批量填报页面 |
| BPMN 组件 | `forge-admin-ui/src/components/bpmn` | 字段变量目录、审批人配置、条件配置、全屏弹窗设计器 |
| form-create 组件 | `forge-admin-ui/src/components/form-create` | 支持字段目录提取、版本预览和只读快照渲染 |
| 数据库 | `forge/db/migration` | 新增表、字典、菜单和权限 |

## 9. 风险与关注点

| 风险 | 级别 | 应对措施 |
|------|------|----------|
| 历史表单版本不一致导致已办详情错乱 | 🔴高 | 实例保存 Schema 快照，只读展示使用快照 |
| PROCESS_ONLY 被误解为完全不保存数据 | 🟡中 | 产品文案说明“不落业务表”，流程审计数据必须保存 |
| 业务落表绕过权限或字段校验 | 🔴高 | 只允许绑定已发布业务对象和动态 CRUD 配置，不开放物理表名 |
| 批量组织填报重复提交 | 🟡中 | 批次 + 组织唯一约束，提交接口加幂等锁 |
| 待办消息重复发送 | 🟡中 | `bizType + bizKey` 幂等检查 |
| 任务候选组无法解析用户 | 🟡中 | 记录告警，不阻断流程，管理员可催办或补充候选人 |
| 流程设计器弹窗空间和路由兼容 | 🟡中 | 保留原路由兼容直达，主路径改弹窗 |
| 状态流转和权限变更 | 🔴高 | `/apply` 前人工确认，接口补权限注解和操作日志 |

## 9.5 测试策略

- **测试范围**：
  - 表单发布版本、字段目录解析、入口提交、PROCESS_ONLY 实例快照。
  - BUSINESS_OBJECT/HYBRID 映射落表、流程发起、状态回写。
  - BPMN 节点审批人和条件引用表单字段。
  - 待办、已办、我发起详情不使用抽屉，表单快照展示正确。
  - 组织批量填报创建、发布、负责人提交、管理员审核汇总。
  - TASK_CREATED 站内信发送和幂等。
- **覆盖率目标**：核心 Service 单元/集成测试覆盖入口提交、映射落表、批次发布、消息幂等；前端至少完成构建和关键页面 ESLint。
- **独立 Test Spec**：是。进入 `/test` 前需创建 `test-spec.md`，并按 `code-copilot/rules/automated-testing-standard.md` 增量记录 `execution-log.md`。

## 10. 待澄清

- [x] PROCESS_ONLY 的表单快照保留期限是否需要配置。结论：永久保留，并跟随流程实例归档。
- [x] 组织负责人规则优先复用 `sys_org` 负责人字段，还是通过角色解析“部门负责人”。结论：默认复用 `sys_org` 负责人字段。
- [x] 批量填报是否需要管理员退回到指定组织重新填报。结论：按默认策略支持驳回后重新提交。
- [x] 入口是否需要移动端适配。结论：本阶段适配移动端。

## 11. 技术决策

| 决策点 | 选择 | 原因 |
|--------|------|------|
| 表单设计器 | 继续复用 form-create，并共享低代码业务组件注册 | 已集成并在流程表单/节点表单中使用 |
| 实际落表路径 | 绑定业务对象 + DynamicCrudService | 避免用户直接操作物理表，复用权限和字段校验 |
| 轻量流程数据 | `sys_flow_form_instance` + Flowable variables | 不生成业务表记录，但保留详情和审计 |
| 表单字段变量名 | form-create `field` | 已作为设计器字段 ID，可自然映射流程变量 |
| 流程入口边界 | 流程模型不直接配置应用入口 | 入口展示位置应由低代码应用、业务对象或后续流程发起中心承载，避免和应用入口配置重复 |
| 批量填报 | 批次表 + 明细表作为底层扩展能力 | 支持业务应用实现每组织状态跟踪、逾期和重新提交，不注册流程管理独立菜单 |
| 待办通知 | 任务创建后直接发送 WEB 站内信 | 满足“生成待办即推送”，不依赖外部事件订阅 |
| UI 设计器 | 模型列表内全屏弹窗为主、路由兼容，并适配移动端窄屏 | 满足更大设计空间，同时保留直达能力 |

## 12. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| 创建 Spec | 完成 | `code-copilot/changes/flow-management-form-runtime-optimization/spec.md` | 本文件 |
| 创建 Tasks | 完成 | `code-copilot/changes/flow-management-form-runtime-optimization/tasks.md` | 配套任务拆分 |
| Task 1-4 | 完成 | `V1.0.57__add_flow_form_runtime_entry.sql`、Flow 表单/入口/运行态实体、Mapper、Service、Controller | 已覆盖表单版本、字段目录、流程入口、`PROCESS_ONLY` 表单实例快照和入口提交 |
| Task 5 | 完成 | `FlowBusinessObjectRuntimeAdapter`、`FlowBusinessObjectRuntimeAdapterImpl`、`FlowRuntimeServiceImpl`、`forge-flow-server/pom.xml` | `BUSINESS_OBJECT/HYBRID` 已接入 `DynamicCrudService` 实际落表，并在流程启动后写入业务流程实例关联 |
| Task 6 | 完成 | `NodePropertiesPanel.vue`、`design.vue`、`flow.js` | 模型绑定表单字段目录已透传到 BPMN 节点属性面板；审批人、候选用户、候选组和流转条件可选择表单字段/系统变量 |
| Task 7 | 调整完成 | `FlowFillBatch*` 后端、`FlowRuntimeServiceImpl`、`flow.js` | 批次发布能力保留为业务场景扩展；前端不再暴露流程管理独立“组织填报批次”页面 |
| Task 8 | 完成 | `FlowTaskEventListener`、`MessageService`、`SysMessageMapper.xml` | 待办创建后同步发送 `FLOW_TODO` 站内信，并按 `bizType + bizKey` 做幂等查询 |
| Task 9 | 调整完成 | `model.vue`、`router/index.js` | 撤掉模型页“入口配置”和隐藏入口运行路由；入口展示位置改由低代码应用、业务对象或后续流程发起中心承载 |
| Task 9.5 | 完成 | `model.vue` | 模型页为已部署模型保留“发起测试”，使用 `FLOW_MODEL_TEST` 和 `FLOW_TEST:` 业务键前缀，避免与正式入口配置混淆 |
| Task 10 | 完成 | `model.vue`、`design.vue`、`todo.vue`、`done.vue`、`started.vue`、`cc.vue` | 模型列表设计入口改为全屏弹窗；设计器顶部配置常驻展示；待办/已办/我发起/抄送详情全部改为弹窗并补移动端全屏适配 |
| Task 10.5 | 完成 | `FlowFormCreateDesigner.vue`、`FlowFormCreateRenderer.vue`、`formCreateBridge.js`、`forgeBusinessComponents.js` | 流程表单设计器复用低代码业务组件注册，运行渲染时加载字典、组织、人员等选项；字段默认下方间距为 20px，并在表单配置中统一调整 |
| Task 11 | 完成 | `test-spec.md`、`execution-log.md` | 已记录后端编译、前端构建和空白检查结果 |

## 13. 审查结论

待 `/review flow-management-form-runtime-optimization` 后填写。

## 14. 确认记录（HARD-GATE）

- **确认时间**：2026-06-06
- **确认人**：用户
- **确认内容**：
  1. PROCESS_ONLY 表单快照永久保留，并跟随流程实例归档。
  2. 组织负责人规则默认复用 `sys_org` 负责人字段。
  3. 批量填报按默认策略支持驳回后重新提交。
  4. 流程入口本阶段需要适配移动端。

## 归档记录（HARD-GATE）
- **状态**：done
- **归档时间**：2026-06-27
- **归档人**：yaomd（批量归档）
- **归档路径**：code-copilot/changes/archive/2026-06-27-flow-management-form-runtime-optimization/
- **判定依据**：任务清单全部完成，execution-log 验证通过（编译/构建/lint 闭环）。
