# 任务清单：business-flow-lowcode-integration
> status: proposed
> created: 2026-06-28
> 原则：复用现有低代码与 Flow 能力；不新建独立审批引擎；查询 SQL 写 Mapper XML；分页参数使用 `pageNum/pageSize`；Flyway 脚本防重复；内置数据 `tenant_id=1`；流程回调必须具备租户上下文和幂等。

## 阶段总览

| 阶段 | 目标 | 交付结果 |
| --- | --- | --- |
| Phase 0 | 基线冻结 | 明确当前能力、兼容范围和迁移边界 |
| Phase 1 | 契约统一 | `BusinessFlowContract` 协议和发布态快照 |
| Phase 2 | 运行网关 | 统一发起、撤回、取消、重提和状态查询 |
| Phase 3 | 事件桥接 | Flow 全量事件消费、幂等、状态机和 trace |
| Phase 4 | 表单资产与待办表单 | 表单资产目录、`BUSINESS_OBJECT_FORM` / `BUSINESS_CODE_FORM` 和节点字段权限 |
| Phase 5 | 设计器一体化 | 表单、单据、流程、节点权限、表单资产选择、自动化合并成流程应用搭建链路 |
| Phase 6 | 模板与生成 | BPMN 模板库、变量清单、节点权限默认值和脚手架 |
| Phase 7 | 验收与治理 | 自动化验收、文档、迁移和运行监控 |

## 任务总览

| Task | 阶段 | 名称 | 状态 | 优先级 |
| --- | --- | --- | --- | --- |
| Task 0 | Phase 0 | 业务流程现状基线和兼容范围确认 | completed | P0 |
| Task 1 | Phase 1 | 定义 BusinessFlowContract 协议 | in_progress | P0 |
| Task 2 | Phase 1 | 流程契约保存与发布态快照 | in_progress | P0 |
| Task 3 | Phase 1 | businessKey 和 recordKey 通用化 | pending | P0 |
| Task 4 | Phase 2 | BusinessFlowGateway 运行网关 | pending | P0 |
| Task 5 | Phase 2 | BusinessRecordRuntimeFacade 记录运行门面 | pending | P0 |
| Task 6 | Phase 2 | 驳回修改重提运行接口 | in_progress | P0 |
| Task 7 | Phase 3 | BusinessFlowEventBridge 全量事件桥 | pending | P0 |
| Task 8 | Phase 3 | BusinessFlowStateMachine 状态机 | pending | P0 |
| Task 9 | Phase 3 | 流程 trace 和幂等日志 | pending | P1 |
| Task 10 | Phase 4 | 表单资产目录与 TaskFormInfo 统一表单引用 | in_progress | P0 |
| Task 11 | Phase 4 | 待办审批页低代码/代码表单渲染 | in_progress | P0 |
| Task 12 | Phase 4 | 节点字段权限后端校验与代码表单目录 | in_progress | P0 |
| Task 13 | Phase 5 | 对象设计器流程应用向导与表单资产选择器 | in_progress | P0 |
| Task 14 | Phase 5 | 发布检查合并流程契约规则 | pending | P0 |
| Task 15 | Phase 5 | 触发器与流程事件副作用编排 | pending | P1 |
| Task 16 | Phase 6 | BPMN 模板库与变量清单 | pending | P0 |
| Task 17 | Phase 6 | 代码优先复杂业务接入 SDK/Adapter | in_progress | P1 |
| Task 18 | Phase 6 | AI/代码生成流程脚手架 | pending | P1 |
| Task 19 | Phase 7 | 标准流程自动化验收模板 | pending | P0 |
| Task 20 | Phase 7 | 历史数据迁移和兼容验证 | pending | P0 |
| Task 21 | Phase 7 | 文档、示例和操作手册 | pending | P1 |

---

## Phase 0：业务流程现状基线

### Task 0：业务流程现状基线和兼容范围确认

**目标**: 冻结本轮优化边界，避免重复建设已完成能力。

**涉及文件**:
- `code-copilot/changes/business-flow-lowcode-integration/spec.md`
- `code-copilot/changes/lowcode-app-full-loop-optimization/spec.md`
- `code-copilot/changes/form-first-business-object-designer/spec.md`
- `code-copilot/changes/lowcode-runtime-datasource-isolation/spec.md`

**工作内容**:
- 梳理现有 `BusinessFlowService`、`BusinessDocumentRuntimeService`、`FlowTaskEventListener`、`FlowBusinessObjectRuntimeAdapterImpl` 的职责。
- 明确手写业务模块和低代码业务对象分别采用的接入方式。
- 明确首期是否新增 `ai_business_flow_contract_version`，还是先复用 `ai_business_binding.binding_config`。
- 明确自定义主键改造范围：至少协议先支持 `recordKey String`，数据库可分阶段落地。

**验收标准**:
- 当前能力、缺口和兼容策略在 spec 中有明确结论。
- 不再出现“低代码审批引擎”作为独立平台能力。

**本轮结果**:
- 已完成现状评估和兼容边界梳理。
- 首期采用复用 `ai_business_binding.binding_config` 的渐进方案，不新增独立审批引擎。

---

## Phase 1：流程契约统一

### Task 1：定义 BusinessFlowContract 协议

**目标**: 用一份契约统一业务对象、单据、流程、节点表单、状态机、变量和事件副作用。

**涉及文件**:
- 新增或扩展 `BusinessFlowBindingDTO`
- 新增 `BusinessFlowContractDTO`
- 新增 `BusinessFlowContractVO`
- 新增 `BusinessFlowContractNormalizer`
- 新增 `BusinessKeyCodec`

**工作内容**:
- 设计 `schemaVersion/objectCode/configKey/recordKey/businessBinding/document/mainFlow/variableMapping/nodeForms/events/options`。
- 兼容旧字段 `flowModelKey/titleTemplate/startMode/variableMapping/options`。
- 增加业务表绑定：`mode/tableName/primaryKeyField/tenantField/statusField/titleField/ownerField`。
- 变量映射继续统一为 `formField/flowVariable`，兼容 `field/variable`。
- 增加 `rejectStrategy/modifyTaskKey`。

**验收标准**:
- 旧配置可无损归一化为新契约。
- 新契约可降级输出旧 `BusinessFlowBindingVO` 给现有前端。
- 低代码对象能自动生成业务表绑定；简单代码业务能手动选择白名单业务表和状态字段。

**本轮结果**:
- `BusinessFlowBindingDTO/VO` 已增加 `businessBinding`。
- `BusinessFlowBindingDTO/VO` 已增加 `nodeForms`，可承载节点表单引用和字段权限。
- 对象设计器流程面板已支持配置接入方式、业务表、主键字段、租户字段、状态字段、标题字段和负责人字段。

### Task 2：流程契约保存与发布态快照

**目标**: 区分设计态草稿和运行态契约，避免运行中流程受设计变更影响。

**涉及文件**:
- `BusinessFlowService`
- `BusinessObjectDesignerService`
- `BusinessObjectPublishService`
- `BusinessObjectDesignVersionService`
- Flyway 脚本（如新增契约版本表或扩展现有表）

**工作内容**:
- 设计态保存到对象设计快照。
- 发布态编译到 `ai_business_binding.binding_config`。
- 对 `LOWCODE_OBJECT` 模式自动解析运行态表名、主键字段、租户字段和状态字段。
- 对 `BUSINESS_TABLE` 模式保存开发者确认过的表字段白名单，不允许运行时任意拼表名字段名。
- 启动流程时把契约摘要写入 `ai_business_flow_instance_link.variables_snapshot` 或独立快照字段。
- 后续如新增 `ai_business_flow_contract_version`，记录对象版本、流程模型版本和表单权限版本。

**验收标准**:
- 编辑设计态不影响已启动流程办理页。
- 发布检查可以读取完整契约。

**本轮结果**:
- 流程绑定保存和查询已兼容 `businessBinding`。
- 低代码对象可根据运行态配置和单据配置补齐默认业务表绑定。
- 节点表单策略已随流程绑定写入 `ai_business_binding.binding_config.nodeForms`，首期不新增契约版本表。

### Task 3：businessKey 和 recordKey 通用化

**目标**: 解决流程链路长期依赖 `Long recordId` 的问题。

**涉及文件**:
- `BusinessFlowStartDTO`
- `AiBusinessFlowInstanceLink`
- `BusinessFlowInstanceLinkMapper.xml`
- `TaskFormInfo`
- `FlowBusinessObjectRuntimeAdapter`
- `FlowBusinessObjectRuntimeAdapterImpl`
- `BusinessDocumentRuntimeService`

**工作内容**:
- DTO 增加 `recordKey String`，保留 `recordId Long` 兼容。
- 表增加 `record_key`，历史数据用 `record_id` 回填。
- `BusinessKeyCodec` 统一构建/解析 `${objectCode}:${recordKey}`。
- 所有新逻辑优先使用 `recordKey`。

**验收标准**:
- Long 主键旧对象不受影响。
- 自定义单字段主键对象能完成发起、查询、回调和详情展示。

---

## Phase 2：运行网关

### Task 4：BusinessFlowGateway 运行网关

**目标**: 统一页面按钮、触发器、FlowEntry 三类流程发起入口。

**涉及文件**:
- 新增 `BusinessFlowGateway`
- 重构 `BusinessFlowService`
- `BusinessTriggerExecutor`
- `FlowBusinessObjectRuntimeAdapterImpl`
- `BusinessApprovalRuntimeService`

**工作内容**:
- 提供 `start/withdraw/cancel/resubmit/getRuntime/updateVariables`。
- 发起时统一校验流程契约、单据状态、按钮权限和变量完整性。
- 发起成功后统一写实例关联和初始状态。
- 旧 `/ai/business/approval/start` 只保留兼容转发。

**验收标准**:
- 手动按钮和触发器发起走同一服务。
- FlowEntry 创建业务记录后也走同一关联写入逻辑。

### Task 5：BusinessRecordRuntimeFacade 记录运行门面

**目标**: 屏蔽动态 CRUD、多数据源、主键、审计和内部状态更新差异。

**涉及文件**:
- 新增 `BusinessRecordRuntimeFacade`
- `DynamicCrudService`
- `LowcodeRuntimeDataSourceResolver`
- `BusinessDocumentRuntimeService`
- `BusinessFlowService`

**工作内容**:
- 提供 `loadRecord/updateInternalFields/buildRecordSnapshot/resolvePrimaryKey`。
- 支持 `LOWCODE_OBJECT` 和简单 `BUSINESS_TABLE` 两种元数据来源。
- 状态回写按契约中的 `statusField` 更新，统一处理租户字段、审计字段和运行数据源。
- 内部更新状态不触发普通用户权限误判。
- 保持运行数据源上下文一致。

**验收标准**:
- 状态回写不再散落调用 `dynamicCrudService.updateInternalFieldsById`。
- 多数据源对象状态回写命中正确数据源。
- 简单业务表只配置主键字段和状态字段，也能完成发起、流程中、通过、驳回、撤回状态同步。

### Task 6：驳回修改重提运行接口

**目标**: 把“驳回到发起人修改节点”固化为平台能力。

**涉及文件**:
- `BusinessFlowGateway`
- `BusinessFlowStateMachine`
- `FlowClient`
- `AiCrudPage`
- 待办详情页

**工作内容**:
- 契约增加 `rejectStrategy=RETURN_TO_MODIFY_TASK` 和 `modifyTaskKey`。
- 契约增加修改模式：`INLINE_MODIFY`（待办内嵌修改）和 `BUSINESS_PAGE_MODIFY`（跳转业务单据修改）。
- `TASK_COMPLETED + approvalResult=reject` 进入可修改状态但不结束流程。
- 发起人在修改节点保存业务数据后，更新流程变量并完成原任务。
- 重提复用原流程实例。
- 低代码简单表单默认走 `INLINE_MODIFY`；代码优先复杂业务默认走 `BUSINESS_PAGE_MODIFY`，由业务页保存后调用 `BusinessFlowGateway.resubmit(...)`。

**验收标准**:
- 提交 -> 驳回 -> 修改 -> 重提 -> 通过 全链路不新建第二个流程实例。
- 修改节点以外的流程中状态默认不可编辑主数据。
- 审批节点只处理审批意见/审批附件，不开放任意业务主字段修改。

**本轮结果**:
- 新增 `POST /ai/business/flow/resubmit`，供复杂代码业务页在保存主数据后完成当前修改节点。
- 重提接口会校验当前用户是已签收办理人，复用原流程实例，不新建第二个流程。
- 重提成功后业务流程状态回写为 `IN_PROCESS`，并可携带补充流程变量。
- 当前重提接口已具备平台协议能力；完整“驳回到指定修改节点”的模板和端到端样例仍需后续用真实流程模型验证。

---

## Phase 3：事件桥、状态机和可观测性

### Task 7：BusinessFlowEventBridge 全量事件桥

**目标**: 低代码流程不依赖 `@FlowBind(modelKey=...)` 手写 Bean。

**涉及文件**:
- 新增 `BusinessFlowEventBridge`
- 新增 Redis 事件监听或扩展现有 `FlowEventSubscriber`
- `BusinessFlowController#callback`
- `BusinessFlowCallbackDTO`

**工作内容**:
- 订阅 `flow:event:all`。
- 支持 Webhook 回调。
- 按 `tenantId/businessKey/processInstanceId/processDefKey` 查找实例关联和契约。
- 统一调用状态机。

**验收标准**:
- 新增低代码流程模型无需写 Java 回调类即可回写业务状态。
- 缺失 `tenantId` 时记录告警并拒绝或按明确策略兜底。

### Task 8：BusinessFlowStateMachine 状态机

**目标**: 把 Flow 事件转为业务状态流转，支持中间节点状态。

**涉及文件**:
- 新增 `BusinessFlowStateMachine`
- `BusinessFlowService`
- `BusinessDocumentConfigService`
- `BusinessEventPublisher`

**工作内容**:
- 处理 `PROCESS_STARTED/TASK_CREATED/TASK_COMPLETED/PROCESS_COMPLETED/PROCESS_CANCELED`。
- 支持终态驳回和驳回修改两类策略。
- 状态回写前校验当前状态，避免乱序覆盖。
- 状态变化后发布 `FLOW_APPROVED/FLOW_REJECTED/FLOW_CANCELED` 业务事件。

**验收标准**:
- 重复事件不会重复触发副作用。
- 乱序事件不会把 `APPROVED` 回退成 `IN_PROCESS`。

### Task 9：流程 trace 和幂等日志

**目标**: 能按一条业务流程查清发起、回调、状态回写和触发器执行结果。

**涉及文件**:
- 新增或扩展 `ai_business_flow_trace_log`
- `BusinessFlowGateway`
- `BusinessFlowEventBridge`
- `BusinessTriggerExecutor`
- `BusinessMessageChannelService`

**工作内容**:
- 生成 `correlationId`。
- 记录发起来源、变量快照、Flow 返回、事件回调、状态前后值、触发器和消息结果。
- 记录 `eventId` 或组合幂等键。

**验收标准**:
- 运维能按 `businessKey/processInstanceId/correlationId` 查询完整链路。
- 回调失败可定位具体阶段并支持补偿重放。

---

## Phase 4：表单资产与待办表单一体化

### Task 10：表单资产目录与 TaskFormInfo 统一表单引用

**目标**: 流程节点配置不再让用户手工输入页面路径，而是选择平台可识别、可校验、可迁移的表单资产。

**涉及文件**:
- `TaskFormInfo`
- `FlowTaskServiceImpl`
- 新增 `BusinessFormAsset`
- 新增 `BusinessFormAssetService`
- 新增 `BusinessCodeFormProvider`
- 新增 `BusinessCodeFormProviderRegistry`
- 新增 `BusinessTaskFormContextService`
- `FlowBusinessObjectRuntimeAdapter`
- `NodePropertiesPanel.vue`
- `BusinessFlowBindingPanel.vue`

**工作内容**:
- 定义统一 `formRef`：`type/code/viewKey/version`，兼容旧 `formKey/formUrl`。
- 支持四类资产：低代码业务表单、Flow 动态表单、代码表单、外部地址。
- 增加 `formType=BUSINESS_OBJECT_FORM` 和 `formType=BUSINESS_CODE_FORM`。
- 返回 `formRef/objectCode/configKey/recordKey/viewKey/mode/editMode/fieldPermissions/runtimeRules`。
- 代码表单通过 `BusinessCodeFormProvider` 或配置注册，不让用户输入组件路径。
- 提供表单资产查询和字段目录查询接口，供流程设计器节点面板使用。
- 支持通过 `businessKey`、流程变量、表单实例快照兜底解析业务记录。

**验收标准**:
- 流程节点配置默认通过下拉选择表单资产。
- 只有外部地址高级模式允许手工输入 URL。
- 待办页能打开低代码业务详情或代码注册表单。
- 会签子任务缺少本地 `business_key` 时能从流程变量或实例关联兜底。
- 旧 `formKey/formUrl` 配置仍能读取和办理。

**本轮结果**:
- 已新增 `GET /ai/business/flow/form-assets/{objectCode}`，从低代码对象 `formDesignerSchema` 汇聚默认表单、多表单资产和字段目录。
- 已新增 `GET/PUT /ai/business/flow/task-form-context`，待办页可按 `businessKey/processInstanceId/taskDefKey` 获取业务表单上下文并保存授权字段。
- `BusinessFlowVariableResolver` 已解析 BPMN `userTask`，流程配置面板可按人工节点自动生成节点表单策略行。
- 节点表单配置已保存统一 `formRef`：低代码表单使用 `BUSINESS_OBJECT_FORM + objectCode + formKey + viewKey`，代码表单预留 `BUSINESS_CODE_FORM + providerKey + formKey`，外部表单使用 `EXTERNAL + formUrl`。
- 首期不让 `forge-plugin-flow` 反向依赖低代码模块；待办页先在读取 `TaskFormInfo` 后调用业务侧上下文 API 完成运行态桥接。

### Task 11：待办审批页低代码/代码表单渲染

**目标**: 审批人办理时看到与业务详情一致的表单和流程上下文，复杂代码业务也能接入待办页。

**涉及文件**:
- `forge-admin-ui/src/views/flow/todo.vue`
- `FlowTaskDetailShell.vue`
- 新增或复用低代码表单渲染组件
- 新增代码表单运行容器或路由跳转处理
- `AiCrudFlowDetail.vue`
- `FieldValueRenderer.vue`

**工作内容**:
- 根据 `BUSINESS_OBJECT_FORM` 加载业务记录。
- 根据 `BUSINESS_CODE_FORM` 加载业务模块注册的表单上下文，支持内嵌组件或跳转业务详情页。
- 应用节点字段权限：只读、隐藏、可编辑、必填。
- 右侧展示流程历史、流程图和审批动作。
- 支持修改节点两种模式：待办内嵌“保存并重提”、跳转业务单据页修改后重提。
- 审批节点默认只写审批意见、签名和审批附件，不改业务主数据。

**验收标准**:
- 审批办理页不需要每条流程手写外置表单。
- 字典、图片、金额、关联字段等渲染与业务详情页一致。
- 采购合同等代码优先复杂业务可从待办进入业务页修改并重提。
- 审批、办理、驳回修改三类任务在 UI 上有清晰区分。

**本轮结果**:
- `forge-admin-ui/src/views/flow/todo.vue` 已接入低代码业务表单上下文，使用 `AiForm` 渲染节点可见字段。
- 审批“同意”前会先校验并保存当前节点配置为可编辑的业务字段；快捷同意遇到需要填写业务字段的任务会提示进入详情办理。
- 待办页已支持 `BUSINESS_CODE_FORM` 上下文兜底展示；Provider 返回业务页地址时，可从待办打开业务表单处理。
- 代码表单和存在可写业务字段的任务已禁止快捷同意，避免绕过业务表单保存和节点字段权限。
- 代码表单内嵌运行容器仍按 Task 17 后续扩展；当前复杂代码业务优先走业务页跳转模式。

### Task 12：节点字段权限后端校验与代码表单目录

**目标**: 字段权限不能只靠前端控制；代码表单也必须提供可配置的字段或区域目录。

**涉及文件**:
- `BusinessTaskFormContextService`
- `BusinessRecordRuntimeFacade`
- `DynamicCrudService`
- `BusinessCodeFormProvider`
- `BusinessFlowAdapter`

**工作内容**:
- 只允许修改节点配置为可编辑的字段。
- 隐藏字段不返回给无权限办理人。
- 审批意见、签名等流程字段与业务字段隔离。
- 代码表单 Provider 通过 `formAssets()` 暴露字段或区域目录，复杂页面可把区域、附件区、操作按钮抽象为虚拟字段。
- 后端按 `taskDefKey + formRef + fieldPolicies` 校验提交字段。
- 发布检查校验节点权限引用的字段必须存在，历史字段标记为失效但不静默通过。

**验收标准**:
- 前端篡改不可编辑字段时后端拒绝。
- 待办办理人不能通过业务详情 API 看到无权字段。
- 代码表单即使是自定义 Vue 页面，也能在节点配置中选择“负责人节点显示上传清单、其他节点隐藏或只读”。

**本轮结果**:
- 节点字段权限已进入流程绑定协议，支持按字段保存 `readable/writable/required`。
- 后端保存时会归一化 `fieldPermissions`，默认不把未知字段当作可编辑字段。
- `BusinessFlowService#saveTaskFormContext` 已按节点 `taskDefKey + fieldPermissions` 过滤保存字段，隐藏字段不返回给待办页，不可编辑字段即使前端篡改也不会写入。
- 待办业务表单上下文已强制携带 `taskId` 并通过 Flow 服务校验任务状态、办理人、候选人、流程实例、业务 Key 和节点 Key。
- 保存业务字段和重提必须由当前已签收办理人执行；候选任务需先签收，避免多人并发修改业务主数据。
- 新增 `BusinessCodeFormProvider` 和 `BusinessCodeFormProviderRegistry`，业务模块可用 Spring Bean 注册代码表单资产、上下文和保存逻辑。

---

## Phase 5：设计器一体化

### Task 13：对象设计器流程应用向导与表单资产选择器

**目标**: 把表单、单据、流程、节点权限和自动化组织成完整低代码搭建链路。

**涉及文件**:
- `object-designer.[objectCode].vue`
- `BusinessDocumentPanel.vue`
- `BusinessFlowBindingPanel.vue`
- `FlowVariableMappingEditor.vue`
- 新增节点表单权限配置面板
- 新增表单资产选择器

**工作内容**:
- 调整步骤：表单 -> 单据 -> 流程 -> 节点表单权限 -> 自动化 -> 权限 -> 发布检查。
- 流程配置面板展示状态字段、发起状态、已映射变量、缺失变量。
- 节点权限面板读取 BPMN userTask。
- 节点表单配置展示“表单类型 / 表单资产 / 表单视图”，不再默认展示路径输入框。
- 外部地址放到高级模式，并提示无法参与字段级权限校验。
- 选择代码表单后，自动加载 Provider 暴露的字段/区域目录给 `FormPermissionConfig`。

**验收标准**:
- 用户不需要在多个菜单间来回跳转才能完成流程类应用配置。
- 发布前能看到完整闭环状态。
- 业务用户能通过名称选择“采购合同审批表单”，不需要知道 Vue 路由或组件路径。

**本轮结果**:
- `BusinessFlowBindingPanel.vue` 已增加业务记录绑定配置入口。
- 已取消“只靠用户手工输入隐藏路径/表名”的首期使用方式：低代码对象默认由后端回显运行态表和字段，代码优先复杂业务可切换到适配器模式。
- `BusinessFlowBindingPanel.vue` 已增加“节点表单策略”：按 BPMN 人工节点选择低代码业务表单，配置可见、可编辑、必填字段；代码表单和外部表单保留开发者模式入口。
- 待办审批页渲染、运行态字段权限校验和代码表单 Provider 目录已按 Task 10/11/12 完成首期运行态桥接。
- 仿钉钉抄送节点已从手工输入用户 ID 改为“指定人员 / 指定角色 / 表达式”三种业务来源；普通 BPMN 服务任务勾选“作为抄送节点”后使用同一套配置能力。
- 抄送节点导出为 `serviceTask + flowable:type="cc"`，按来源写入 `candidateUsers` 或 `candidateGroups`，默认挂平台 `flowCcNodeDelegate`；流程走到该节点时可按固定人员、角色成员或表达式结果发送抄送，不再只能依赖流程结束后的后台变量抄送。
- 低代码应用完整步骤编排、发布检查联动和表单设计器侧体验改造按用户要求暂缓，本轮不继续扩展。

### Task 14：发布检查合并流程契约规则

**目标**: 发布时一次性检查流程类应用是否可运行。

**涉及文件**:
- `BusinessObjectPublishService`
- `BusinessPublishChecklist.vue`
- `LowcodeSchemaValidator`

**工作内容**:
- 检查业务表绑定、主键字段、状态字段、状态映射、流程模型、变量映射、修改节点、表单资产、节点字段权限、按钮权限、触发器动作。
- 校验 `businessBinding.tableName/statusField/primaryKeyField/tenantField` 来自低代码元数据或开发者白名单。
- 校验节点 `formRef` 引用的表单资产存在；代码表单资产必须提供字段或区域目录。
- 校验驳回修改模式必须明确为 `INLINE_MODIFY` 或 `BUSINESS_PAGE_MODIFY`。
- 对外部 URL 表单输出风险提示：只能做页面级跳转，不能参与平台字段级权限校验。
- 输出阻断项、警告项和跳转修复动作。

**验收标准**:
- 未配置主流程但启用单据模式时给出明确修复路径。
- BPMN 引用未映射变量时发布检查能发现。

### Task 15：触发器与流程事件副作用编排

**目标**: 流程通过、驳回、撤回后可以低代码配置副作用。

**涉及文件**:
- `BusinessTriggerService`
- `BusinessTriggerExecutor`
- `BusinessTriggerMapper.xml`
- 触发器前端配置页

**工作内容**:
- `FLOW_APPROVED/FLOW_REJECTED/FLOW_CANCELED` 作为标准事件源。
- 创建记录、更新字段、发送站内消息、Webhook TODO 统一支持 `correlationId`。
- 动作执行必须幂等。

**验收标准**:
- 审批通过后可创建关联记录。
- 审批驳回后可发送站内消息。
- 重复回调不重复创建记录。

---

## Phase 6：模板与生成

### Task 16：BPMN 模板库与变量清单

**目标**: 让流程设计器和 AI 生成器能快速生成规范流程。

**涉及文件**:
- `FlowTemplate`
- `FlowTemplateFactory`
- `FlowBpmnGenerateService`
- `flow-designer/converter`
- 模板 seed/Flyway

**工作内容**:
- 增加串行、会签、并行或签、驳回修改重提、审批后抄送、审批后处理模板。
- 每个模板输出 BPMN、变量清单、节点权限默认值、发布检查规则。
- 模板节点命名固定。

**验收标准**:
- 选择模板后能自动生成可发布 BPMN。
- 变量候选和低代码字段映射能自动推荐。

### Task 17：代码优先复杂业务接入 SDK/Adapter

**目标**: 让非低代码创建、由用户自己写代码实现的复杂业务，也能灵活接入同一套流程网关、事件桥、trace 和待办展示能力。

**涉及文件**:
- 新增 `BusinessFlowAdapter`
- 新增 `AbstractBusinessFlowAdapter`
- 新增 `BusinessFlowAdapterRegistry`
- 新增 `FlowStartRequest`
- 新增 `FlowStartResult`
- 新增 `FlowBusinessSnapshot`
- 新增 `BusinessTaskFormContext`
- 新增 `BusinessCodeFormProvider`
- 新增 `BusinessCodeFormProviderRegistry`
- `BusinessFlowGateway`
- `BusinessFlowEventBridge`
- 示例业务模块文档

**工作内容**:
- 提供三种接入模式：Gateway API 模式、Adapter 模式、Annotation 兼容模式。
- Adapter 提供 `businessType/objectCode/modelKey/buildBusinessKey/loadBusiness/buildStartVariables/validateBeforeStart/markInProcess/onTaskCompleted/onApproved/onRejected/onCanceled`。
- Adapter 注册表按 `businessType/modelKey/businessKey` 路由流程事件。
- `BusinessFlowGateway` 支持代码优先业务发起流程，不要求存在低代码对象或 `ai_crud_config`。
- `BusinessFlowEventBridge` 识别代码优先业务后调用 Adapter，而不是走动态 CRUD 状态回写。
- Adapter 可返回外部业务详情路由或 `BUSINESS_CODE_FORM` 上下文，供待办页打开复杂业务详情。
- 代码业务如果要参与节点字段权限，必须通过 `BusinessCodeFormProvider.formAssets()` 提供字段或区域目录；复杂区域可用虚拟字段表示。
- 提供示例：采购合同 Adapter + 采购合同审批表单 Provider + 负责人上传清单字段策略。
- 保留 `@FlowBind/@FlowCallback` 兼容，但新复杂业务优先使用 Adapter。

**验收标准**:
- 手写业务模块不迁移到低代码动态 CRUD，也能发起、审批、撤回、回写状态和查看流程进度。
- 业务状态机和领域副作用仍在业务 Service 内实现，平台不接管业务事务。
- 手写业务模块能复用同一事件桥、trace、消息和标准验收模板。
- 手写业务模块能通过注册表单资产接入待办页，不要求用户填写 Vue 路径。

**本轮结果**:
- 已落地 `BusinessCodeFormProvider` 和 `BusinessCodeFormProviderRegistry` 服务端扩展点。
- `GET /ai/business/flow/form-assets/{objectCode}` 会合并已注册代码表单资产，后续业务模块实现 Provider 后即可出现在资产目录中。
- `BUSINESS_CODE_FORM` 待办上下文已支持 Provider 委派；未注册 Provider 时返回明确 warning，不再默默降级为手工路径。
- 复杂代码业务“业务页保存 -> 调用 `/ai/business/flow/resubmit` 重提”的后端协议已具备；采购合同等具体示例 Provider 尚未新增。
- 同一套 Flow 事件不会被 Adapter 和 `@FlowCallback` 重复消费。
- 不要求低代码对象生成 Java Adapter。
- 已新增“采购单审批测试”代码优先样例，注册 `SamplePurchaseOrderCodeFormProvider`，可通过代码表单资产和外置表单路径验证手写业务接入流程。

### Task 18：AI/代码生成流程脚手架

**目标**: 对需要代码化交付的流程模块，生成规范骨架。

**涉及文件**:
- 代码生成模板
- `forge-codegen-crud` 相关模板
- 前端页面模板

**工作内容**:
- 生成 Adapter、BPMN 模板、Flyway、字典、权限、前端详情页、测试骨架。
- 生成内容遵循 XML SQL、`pageNum/pageSize`、字典和租户规则。

**验收标准**:
- 新手写流程模块不再从零拼接流程接入代码。
- 生成结果符合 Forge 编码规范。

---

## Phase 7：验收与治理

### Task 19：标准流程自动化验收模板

**目标**: 每个流程类应用可复用同一套验收场景。

**涉及文件**:
- `code-copilot/rules/automated-testing-standard.md`
- 新增流程验收用例模板
- 后端测试
- 前端 E2E 或组件测试

**验收场景**:
- 提交 -> 审批通过 -> 状态 `APPROVED` -> 触发器副作用。
- 提交 -> 驳回 -> 修改重提 -> 再审批通过。
- 提交 -> 撤回 -> 状态 `CANCELED`。
- 会签所有人处理后才完成。
- 抄送人能查看详情。
- 多租户上下文正确。

**验收标准**:
- 新流程应用按模板可快速生成测试计划。
- 执行结果追加到当前变更 `execution-log.md`。

**本轮结果**:
- 已新增采购单审批测试样例，覆盖“提交 -> 部门负责人审批 -> 工程部经理审批 -> 会签 -> 审批通过”和“审批节点驳回 -> 申请人修改 -> 重提/终止”的核心验收路径。
- 已提供前端列表工作台和待办外置表单，用于后续真实服务环境下统一跑全链路验证。

### Task 20：历史数据迁移和兼容验证

**目标**: 现有低代码对象和流程实例平滑迁移。

**涉及文件**:
- Flyway 迁移脚本
- 迁移校验 SQL
- 兼容测试

**工作内容**:
- 旧 `recordId` 回填 `recordKey`。
- 旧 `binding_config` 归一化为新契约。
- 旧审批兼容接口仍可转发。
- 运行中流程按旧关联继续可查可办。

**验收标准**:
- 历史商机/离职等样例流程仍可查看进度。
- 新旧配置混合时发布检查给出明确提示。

### Task 21：文档、示例和操作手册

**目标**: 让后续业务流程接入有可复用操作标准。

**涉及文件**:
- `forge-docs`
- `code-copilot/knowledge`
- 示例数据 seed

**工作内容**:
- 编写“流程类低代码应用搭建指南”。
- 编写“业务流程契约字段说明”。
- 编写“驳回修改重提模板使用指南”。
- 编写“流程事件排障指南”。

**验收标准**:
- 新增流程类低代码应用时，业务、开发、测试三方能按同一文档工作。
- 常见问题沉淀到 `code-copilot/memory/pitfalls.md` 或 `code-copilot/knowledge`。
