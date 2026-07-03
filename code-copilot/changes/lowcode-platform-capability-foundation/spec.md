# 低代码平台通用业务能力底座补齐
> status: review_fix_completed
> created: 2026-07-02
> complexity: 🔴复杂

## 1. 背景与目标

当前应用管理已经具备业务对象、字段、表单、列表、详情、关系、公式、单据编号、流程绑定、触发器、消息、导入导出等基础能力。采购仓储原型评估暴露的问题不是某一个业务页面缺字段，而是平台还缺少一批通用能力：

- 业务按钮只能打开页面、调用 API、发流程或触发器，不能用配置表达“弹窗收集参数 → 多步骤事务 → 跨对象更新 → 写日志 → 刷新页面”。
- 触发器能做创建记录、更新字段、发消息、发流程，但复杂副作用缺统一编排和审计。
- 流程审批通过/驳回后的业务动作没有统一可配置回调入口。
- 主子表已有运行能力，但“选择记录弹窗 → 批量带入子表 → 字段映射 → 行内校验/计算”还不够产品化。
- 库存、资金、合同状态等高风险领域动作不能只靠动态 CRUD 字段更新，需要平台级领域台账/动作接口支撑幂等、事务和审计。

本变更目标是补齐“通用平台能力底座”，使不同业务域都能通过应用管理和流程能力完成真实可用的业务闭环。采购仓储只作为验收样例之一，不允许在平台代码里硬编码采购、仓库、供应商、物料等业务对象。

完成后，平台应支持：

- 任意业务对象配置通用动作按钮，动作可带弹窗表单、权限、确认、条件、多步骤事务和执行日志。
- 动作、触发器、流程回调共用同一个动作执行底座。
- 任意对象配置记录选择器，支持选择其他对象记录并按字段映射批量写入主表或子表。
- 对高风险领域动作提供可插拔的领域动作 SPI，首期提供库存台账引擎作为通用“数量台账”实现，不绑定采购仓储。
- 通过 SDD 分阶段开发和验收，先交付动作底座，再交付选择器/主子表，再交付流程回调，最后交付领域台账。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- `forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue`：已有对象动作配置入口，动作类型包括打开页面、调用 API、发起主流程、执行触发器、外部链接。
- `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`：列表设计器会把对象动作转换为运行态列表按钮配置。
- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`：运行态支持行按钮、确认、`START_FLOW`、`CALL_API`、站内路由、外部链接和按钮 loading。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectActionService.java`：保存对象动作配置到 `designerOptions.actions`，目前偏配置管理，不负责执行复杂动作。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/DynamicCrudController.java`：动态 CRUD 新增、修改、删除后发布业务事件。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessEventPublisher.java`：发布记录创建、更新、删除、状态变更和流程结果事件。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerExecutor.java`：触发器执行动作，支持 `START_FLOW`、`SEND_MESSAGE`、`CREATE_RECORD`、`UPDATE_FIELD`、`WEBHOOK`，其中 Webhook 仍为 TODO。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerSchedulerService.java`：定时触发器每 5 分钟扫描到期字段并去重执行。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java`：承载业务对象流程发起、状态回写、节点字段权限、流程结果事件发布。

### 2.2 现有实现

- `DynamicCrudService` 已支持普通单表增删改查、导入导出、自动编号、单据编号、STORED/VIRTUAL 公式、运行时数据源上下文、主子表 `children` 查询和保存。
- `ChildTableEditor.vue` 已支持运行态子表明细增删行、基础组件渲染和必填校验。
- `BusinessTriggerExecutor` 已能复用 `DynamicCrudService.insertInternal` 和 `updateFieldsInternal` 做内部创建/更新字段。
- `BusinessFlowBindingPanel.vue` 和流程相关配置已有字段权限、流程启动方式、变量映射等能力。
- `AiCrudPage.vue` 已有行按钮 loading、全局流程发起 loading、确认弹窗和自定义动作事件。

### 2.3 发现与风险

- 动作执行分散在页面、触发器、流程服务和自定义 API 中，缺少统一动作执行服务、动作日志和事务边界。
- 触发器当前能创建/更新记录，但不适合承载库存扣减、资金核销、合同关闭等高风险领域动作。
- Webhook 动作在 `BusinessTriggerExecutor.executeWebhookAction` 中仍是 TODO，动作引擎需要为异步/外部动作预留 TODO 与重试状态。
- 主子表更新已有 `replace` 风格保存，缺少稳定行级合并、字段映射选择器和服务端行级规则校验。
- 流程结果能发布业务事件，但审批通过后执行多步骤业务副作用缺统一配置点。
- 库存/资金等领域能力不能依赖普通 `UPDATE_FIELD`，必须通过领域服务保证幂等、防重复、并发一致性和审计。

## 3. 功能点

- [ ] **通用动作执行引擎**：对象按钮、触发器、流程回调可以执行同一种动作协议。输入为对象编码、记录 ID、动作编码、表单参数和上下文；输出为执行状态、步骤结果、提示消息和日志 ID。
- [ ] **动作弹窗表单**：动作可配置运行态表单 Schema，用于确认交付、登记付款、提交出库、状态变更等通用交互，不写死业务弹窗。
- [ ] **多步骤事务编排**：动作步骤支持更新字段、创建记录、调用领域动作、发起流程、发送消息、Webhook。数据写入类步骤默认同事务执行。
- [ ] **动作执行日志**：每次动作执行记录发起人、对象、记录、动作、入参摘要、步骤结果、耗时、状态和错误。
- [ ] **通用记录选择器**：表单和子表可配置选择任意运行对象记录，支持搜索、表格列、单选/多选和字段映射。
- [ ] **主子表增强**：支持选择器批量写入子表、子表行级校验、行级公式触发和服务端 payload 校验。
- [ ] **流程回调动作**：流程通过、驳回、取消可配置执行指定动作编码，复用动作引擎。
- [ ] **领域动作 SPI**：动作步骤可调用注册的领域能力，例如数量台账、资金核销、合同状态机，平台只定义 SPI 和审计协议。
- [ ] **数量台账引擎首版**：提供通用数量台账能力：入账、锁定、释放、扣减、转移、流水、余额和幂等键。采购仓储使用它实现库存，但引擎命名和协议保持通用。
- [ ] **提醒规则增强**：在现有定时触发器基础上，支持金额/数量/日期分层规则、接收人规则、去重和重复提醒。

## 4. 业务规则

- 平台能力必须通用，不能出现针对采购仓储的硬编码对象、字段、菜单或接口。
- 采购仓储、合同财务、人事、CRM 都只能作为验收样例，不能作为平台服务命名依据。
- 普通 CRUD、普通状态字段仍使用动态 CRUD；涉及库存、资金、权限放开、合同关闭等高风险副作用必须通过动作引擎和领域服务执行。
- 动作执行必须有权限校验、幂等控制、执行日志和错误可追踪。
- 数据写入类动作步骤默认事务内执行；消息、Webhook 等外部动作必须明确是同步阻断还是异步补偿。
- 流程审批不新建第二套审批引擎，继续统一使用 Flowable；动作引擎只负责流程结果后的业务副作用。
- 主子表和选择器都必须基于业务对象运行配置，不允许直接让业务用户填写表名或 SQL。
- 所有 Long ID 前端按字符串处理，避免 JS 精度丢失。
- 金额字段继续使用 long/bigint，单位分。
- Flyway 脚本必须可重复执行或具备防重复保护；内置数据 `tenant_id=1`。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增 | `ai_business_action_execution_log` | `tenant_id`, `object_code`, `record_id`, `action_code`, `execute_status`, `request_digest`, `step_result`, `error_message`, `correlation_id`, `duration_ms` | 通用动作执行日志 |
| 新增 | `ai_business_quantity_balance` | `tenant_id`, `account_code`, `item_code`, `dimension_key`, `quantity`, `locked_quantity`, 唯一索引 | 通用数量余额，不命名为库存 |
| 新增 | `ai_business_quantity_ledger` | `tenant_id`, `account_code`, `item_code`, `operation_type`, `quantity_delta`, `source_object_code`, `source_record_id`, `idempotency_key` | 通用数量流水 |
| 新增 | `ai_business_quantity_lock` | `tenant_id`, `account_code`, `item_code`, `lock_quantity`, `lock_status`, `source_object_code`, `source_record_id`, `idempotency_key` | 通用数量锁定 |
| 可选新增 | `ai_business_reminder_profile` | `tenant_id`, `profile_code`, `object_code`, `rule_config`, `status` | 分层提醒规则配置 |
| 扩展 | `ai_business_trigger_log` | 复用已有 `correlation_id`, `todo_code` | 动作引擎与触发器链路关联 |
| 扩展 | `ai_business_flow_instance_link` | 不优先加字段 | 流程回调优先通过动作配置引用 |

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 新增 | `/ai/business/action/execute` | POST | 执行业务动作 |
| 新增 | `/ai/business/action/logs` | GET | 查询动作执行日志 |
| 新增 | `/ai/business/action/preview` | POST | 预览动作步骤、权限和字段映射有效性 |
| 新增 | `/ai/business/selector/query` | POST | 按运行对象配置查询选择器数据 |
| 新增 | `/ai/business/quantity/operation` | POST | 执行通用数量台账动作，首期只允许动作引擎内部调用 |
| 扩展 | `/ai/business/trigger/*` | - | 触发器执行改为复用动作引擎 |
| 扩展 | `/ai/business/flow/*` | - | 流程结果回调可绑定动作编码 |

## 7. 影响范围

- 后端：`forge-plugin-generator` 动态业务应用、触发器、流程绑定、动作配置、数量台账。
- 前端：应用中心动作设计器、列表设计器、表单设计器、运行态 `AiCrudPage`、子表编辑器、选择器组件。
- 数据库：新增动作日志和数量台账表。
- 权限：新增动作执行、动作日志、数量台账操作相关权限。
- 测试：后端服务单测、动作事务集成测试、前端构建和关键页面手工验收。

## 8. 风险与关注点

> ⚠️ 本变更涉及状态流转、库存/数量扣减、未来可承载资金核销，属于高风险平台能力。

- 不能把动作引擎做成任意脚本执行器，首期只允许白名单步骤类型。
- `CALL_API` 不能绕过权限和审计成为后门；对内部平台动作优先使用动作步骤，不鼓励业务人员配置裸 API。
- 数量台账必须使用幂等键和并发保护，防止重复扣减或负数余额。
- 流程回调失败必须可见，不允许审批显示成功但业务副作用静默失败。
- 外部动作如 Webhook 不能和数据库事务强绑定，需要明确 TODO/失败/重试状态。
- 选择器查询必须走运行对象权限和数据权限，不能绕过租户、数据范围和字段脱敏。

## 8.5 测试策略

- **测试范围**：动作执行、动作日志、触发器复用动作、流程回调动作、选择器查询、子表映射、数量台账幂等和并发、防负数余额。
- **覆盖率目标**：P0 服务类和台账核心逻辑单测覆盖主要分支；前端以构建和关键交互手工验证为主。
- **独立 Test Spec**：是。

## 9. 待澄清

- [x] 第一轮 `/apply` 是否只实现 Phase 1「通用动作执行引擎」，后续 Phase 2/3/4 单独变更推进。建议：是。
- [x] 数量台账首版是否命名为通用 `quantity`，库存仅作为应用配置样例。建议：是。
- [x] Webhook 是否纳入第一轮实现。建议：不纳入，保留 TODO 状态和日志，避免范围扩大。

## 10. 技术决策

- 动作执行服务作为统一入口：页面按钮、触发器、流程回调都调用同一执行器。
- 动作步骤使用白名单类型：`UPDATE_FIELD`、`CREATE_RECORD`、`START_FLOW`、`SEND_MESSAGE`、`DOMAIN_ACTION`、`WEBHOOK`。
- 领域动作通过 SPI 注册，动作引擎只负责编排、权限、事务和日志，不内置具体业务规则。
- 数量台账作为首个领域动作实现，使用通用 `accountCode/itemCode/dimensionKey` 表达仓库、物料、批次等维度。
- 触发器现有 `CREATE_RECORD`、`UPDATE_FIELD` 后续迁移为动作步骤，兼容旧配置。
- 流程回调不直接写业务表，统一执行动作编码。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| SDD proposal | completed | `spec.md`, `tasks.md`, `test-spec.md` | 已创建通用平台能力变更，不进入编码 |
| Apply start | in_progress | `spec.md`, `tasks.md`, `execution-log.md` | 用户执行 `apply lowcode-platform-capability-foundation`，第一轮编码范围锁定为 Phase 1 |
| Task 1 | completed | `V1.0.87__add_business_action_execution_log.sql`, `BusinessActionExecuteDTO.java`, `BusinessActionStepDTO.java`, `BusinessActionLogQueryDTO.java`, `BusinessActionExecuteResultVO.java`, `BusinessActionStepResultVO.java`, `AiBusinessActionExecutionLog.java`, `BusinessActionExecutionLogMapper.java`, `BusinessActionExecutionLogMapper.xml` | 已新增通用动作协议与执行日志表 |
| Task 2 | completed | `BusinessActionExecutionService.java`, `BusinessActionStepExecutor.java`, `UpdateFieldActionStepExecutor.java`, `CreateRecordActionStepExecutor.java`, `StartFlowActionStepExecutor.java`, `SendMessageActionStepExecutor.java`, `BusinessObjectActionService.java`, `BusinessObjectMapper.java`, `BusinessObjectMapper.xml` | 已实现动作配置解析、权限校验、幂等命中、事务执行和失败日志 |
| Task 3 | completed | `BusinessActionExecutionController.java`, `business-app.js`, `V1.0.87__add_business_action_execution_log.sql` | 已新增动作执行、预览和日志接口及权限资源 |
| Task 4 | completed | `BusinessActionDesigner.vue`, `BusinessListDesigner.vue`, `AiCrudPage.vue`, `business-app.js` | 已支持 `COMMAND` 动作设计、列表运行态映射、动作弹窗表单、执行 loading 和成功刷新 |
| Phase 1 validation | completed | 后端 Maven 编译、前端 pnpm build、`git diff --check`、Flyway placeholder 扫描 | 后端/前端构建通过；首次后端编译因 shell JDK 非 17 失败，指定 Java 17 后通过 |
| Phase 2 start | completed | `tasks.md`, `execution-log.md` | 用户要求继续，进入通用记录选择器能力 |
| Task 5 | completed | `BusinessRecordSelectorController.java`, `BusinessRecordSelectorService.java`, `BusinessRecordSelectorQueryDTO.java`, `BusinessRecordSelectorResultVO.java`, `BusinessObjectMapper.java`, `BusinessObjectMapper.xml`, `business-app.js` | 已新增通用记录选择器查询接口，复用动态 CRUD 查询、运行配置、数据权限和字段白名单，Long ID 返回前端前字符串化 |
| Task 6 | completed | `AiRecordSelectorModal.vue`, `record-selector-utils.js`, `AiFormItem.vue`, `ChildTableEditor.vue`, `BusinessFieldPropertyPanel.vue`, `BusinessFormDesigner.vue`, `ForgeFieldShelf.vue`, `ForgePropertyPanel.vue`, `formDesignerSchema.js`, `autoFieldRegistry.js`, `BusinessFieldSchemaService.java`, `BusinessObjectDesignerService.java`, `LowcodeRuntimeConfigBuilder.java`, `LowcodeSchemaValidator.java` | 已支持记录选择器字段配置、运行态单选回填、字段映射、多选批量追加子表；服务端主子表 merge 保存仍归 Task 7 |
| Phase 2 validation | completed | 后端 Maven 编译、前端 pnpm build、`git diff --check`、Flyway placeholder 扫描 | 后端/前端构建通过；前端仅有既有 CSS `//` 注释和 chunk warning；placeholder 扫描无输出 |
| Task 7 | completed | `DynamicCrudService.java`, `BusinessObjectDesignerService.java`, `LowcodeRuntimeConfigBuilder.java`, `ChildTableEditor.vue`, `BusinessRelationDesigner.vue` | 已支持主子表 `saveMode=merge` 行级新增、修改、删除；默认仍为 `replace`；前端已有子行删除使用 `_deleted` 标记；后端做子行归属、字段白名单、必填和数值范围校验 |
| Task 7 validation | completed | 后端 Maven 编译、前端 pnpm build、`git diff --check`、Flyway placeholder 扫描 | 后端/前端构建通过；前端仅有既有 CSS `//` 注释、chunk warning 和 `UserSelectModal` 命名冲突 warning |
| Task 8 | completed | `BusinessFlowService.java`, `BusinessFlowBindingPanel.vue`, `BusinessFlowAppConfigPanel.vue` | 已支持流程通过/驳回/取消后按 `options.callbackActions` 执行对象动作；动作执行复用通用动作引擎、幂等键和动作日志；流程绑定面板可选择对象动作并展示回调动作数量 |
| Task 8 validation | completed | 后端 Maven 编译、前端 pnpm build、`git diff --check`、Flyway placeholder 扫描 | 后端/前端构建通过；前端仅有既有 CSS `//` 注释、chunk warning 和 `UserSelectModal` 命名冲突 warning |
| Task 9 | completed | `BusinessTriggerExecutor.java`, `TriggerActionConfigPanel.vue`, `trigger.vue` | 已支持触发器新动作类型 `BUSINESS_ACTION` 选择并执行对象动作编码；后端优先调用通用动作引擎，旧 `START_FLOW`、`SEND_MESSAGE`、`CREATE_RECORD`、`UPDATE_FIELD`、`WEBHOOK` 动作仍保持兼容 |
| Task 9 validation | completed | 后端 Maven 编译、前端 pnpm build、`git diff --check`、Flyway placeholder 扫描 | 后端/前端构建通过；本轮补充格式检查和 placeholder 扫描通过；前端仅有既有 CSS `//` 注释、chunk warning 和 `UserSelectModal` 命名冲突 warning |
| Task 10 | completed | `BusinessDomainActionExecutor.java`, `BusinessDomainActionRegistry.java`, `DomainActionStepExecutor.java` | 已新增领域动作 SPI、注册中心和 `DOMAIN_ACTION` 步骤执行器；未注册领域动作返回明确业务异常，执行结果进入动作步骤日志 |
| Task 11 | completed | `V1.0.88__add_business_quantity_ledger.sql`, `AiBusinessQuantityBalance.java`, `AiBusinessQuantityLedger.java`, `AiBusinessQuantityLock.java`, `BusinessQuantityBalanceMapper.java`, `BusinessQuantityLedgerMapper.java`, `BusinessQuantityLockMapper.java`, 对应 Mapper XML | 已新增通用数量余额、流水、锁定表；余额维度唯一，流水和锁定支持幂等键唯一约束 |
| Task 12 | completed | `BusinessQuantityLedgerService.java`, `BusinessQuantityOperationDTO.java`, `BusinessQuantityOperationResultVO.java` | 已实现入账、锁定、释放、扣减、转移、幂等短路、可用数量不足拒绝和事务内流水写入 |
| Task 13 | completed | `BusinessQuantityDomainActionExecutor.java`, `DomainActionStepExecutor.java`, `BusinessActionDesigner.vue`, `BusinessQuantityLedgerServiceTest.java` | 已将数量台账注册为 `QUANTITY` 领域动作，动作步骤可通过 `DOMAIN_ACTION` 调用；动作设计器提供数量台账步骤模板 |
| Task 14 | completed | `BusinessTriggerSchedulerService.java`, `BusinessTriggerExecutor.java`, `trigger.vue` | 已在现有定时触发器上支持 `schedule.tierRules` 分层提醒，按指标区间、提前天数和接收人规则匹配；触发器消息动作可读取分层接收人覆盖 |
| Task 15 | completed | 后端 Maven 编译、前端 pnpm build、`BusinessQuantityLedgerServiceTest.java`, `git diff --check`, Flyway placeholder 扫描 | 后端/前端构建通过；已新增数量台账单测，但根 POM 固定跳过测试导致 Maven 未实际执行测试类，已在执行日志记录 |
| Task 16 | completed | `spec.md`, `tasks.md`, `test-spec.md`, `execution-log.md` | SDD 状态、验证记录、跳过项和遗留风险已回填，可进入 `/review lowcode-platform-capability-foundation` |
| Review Fix | completed | `BusinessActionExecutionService.java`, `BusinessRecordSelectorService.java`, `BusinessQuantityLedgerService.java`, `BusinessQuantityDomainActionExecutor.java`, `BusinessTriggerExecutor.java`, `forge-server/pom.xml`, 三个业务应用服务单测, SDD 文档 | 已修复审查发现的动作幂等并发、选择器权限/字段泄漏、数量台账幂等/转移流水和测试未实际执行问题 |
| Review Fix validation | completed | 后端 Maven 编译、Maven 定向单测、`git diff --check`, Flyway placeholder 扫描、历史方法名扫描 | 编译通过；8 个定向单测全部实际执行通过；未引入 `${...}` placeholder；未保留旧 Mapper 方法名或 `valueText` 调用 |

## 12. 审查结论

Review 发现项已修复：

- 动作执行引擎已补 `RUNNING` 预占日志和唯一键防并发，重复幂等键不会并发执行步骤。
- 记录选择器已补目标对象 `VIEW` 权限、响应字段收口和排序字段收口。
- 数量台账已强制稳定幂等键，转移动作拆分源端/目标端流水并返回目标流水 ID。
- 根 POM 固定跳过测试的问题已通过 `enable-tests` profile 修复，本变更 8 个定向单测已实际执行通过。

遗留风险：未启动本地后端/前端和数据库做真实低代码配置联调；需要在具备 MySQL、Redis 和前端运行环境时做一次端到端验收。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-02
- **确认人**：用户
- **确认内容**：用户执行 `apply lowcode-platform-capability-foundation`；确认本变更必须按通用平台能力实现，采购仓储仅作为验收样例；第一轮编码范围锁定为 Phase 1「通用动作执行引擎」。
