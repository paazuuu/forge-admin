# 统一业务流程应用配置
> status: applied
> created: 2026-06-28
> complexity: 🔴 复杂
> related: `code-copilot/changes/business-flow-lowcode-integration/spec.md`

## 1. 背景与目标

当前业务模块与流程模块已经具备基础联动能力，但配置入口仍然分散：

- 业务对象设计器里有“单据设置”和“流程与自动化”两个面板。
- 流程设计器里也能配置节点表单、表单 URL、节点字段权限。
- 代码实现的复杂业务可以通过 `BusinessCodeFormProvider` 接入，但前台配置还没有把它作为“可选择的业务表单资产”呈现。
- 采购单审批测试流程里仍有 `flowable:formUrl="/business/purchase-order-test"` 这类手写路径，对业务人员不可用，也容易与业务应用配置产生双写。
- 之前把节点表单策略做成应用配置里的列表，和流程图里的节点属性割裂；用户真正需要的是打开流程设计器，选中节点后配置审批办理、表单资产和字段权限。

本变更目标是把“表单/单据/流程/节点表单/字段权限/自动化触发/发布检查”收口成一套业务应用流程配置：

```text
业务表单资产
  -> 单据规则
  -> 主流程绑定
  -> 变量映射
  -> 打开流程设计器
  -> 选中节点
  -> 节点配置（审批设置 / 表单权限 / 高级设置）
  -> 自动化触发
  -> 发布检查
  -> 待办/已办/业务详情统一运行
```

首期不重写低代码表单设计器和动态 CRUD 底层，不新增独立审批引擎；重点是统一配置入口、统一运行契约，并把节点表单权限从“应用配置里的孤立配置项”收回到流程设计器节点配置。业务用户在应用中心完成流程关联和变量映射后，通过入口进入流程设计器配置节点，不再维护两份节点配置。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

1. 业务对象设计器当前把单据与流程拆成两个独立面板：
   - `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue:150` 使用 `BusinessDocumentPanel`。
   - `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue:164` 使用 `BusinessFlowBindingPanel`。
   - `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue:331` 的闭环步骤仍把 `document` 和 `flow` 作为两个步骤。

2. 单据配置服务已经维护状态字段、编号规则和主流程摘要：
   - `BusinessDocumentConfigService#getConfig`：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessDocumentConfigService.java:65`
   - `BusinessDocumentConfigService#saveConfig`：同文件 `:123`

3. 流程绑定面板已经具备流程模型、业务记录绑定、变量映射，节点配置需要跳转到流程设计器维护：
   - `BusinessFlowBindingPanel.vue:33` 选择流程模型。
   - `BusinessFlowBindingPanel.vue:64` 配置业务记录绑定。
   - `BusinessFlowBindingPanel.vue:155` 配置变量映射。
   - `BusinessFlowBindingPanel.vue` 提供“打开流程设计器”入口。

4. 前端 API 已经拆成单据和流程两组：
   - `businessDocumentConfig/saveBusinessDocumentConfig`：`forge-admin-ui/src/api/business-app.js:346`
   - `businessFlowBinding/saveBusinessFlowBinding`：`forge-admin-ui/src/api/business-app.js:370`
   - `businessFlowFormAssets`：`forge-admin-ui/src/api/business-app.js:382`

5. 后端流程绑定接口已经存在，但尚未提供“单据 + 流程”的统一 Facade：
   - `BusinessFlowController#getBinding`：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessFlowController.java:45`
   - `BusinessFlowController#formAssets`：同文件 `:60`
   - `BusinessFlowController#saveBinding`：同文件 `:88`

6. 运行时待办表单应优先读取流程节点配置，并仅把 `ai_business_binding.binding_config.nodeForms` 作为历史兜底：
   - `BusinessFlowService#buildTaskFormContext`：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java:454`
   - `BusinessFlowService#resolveFlowNodeForm`：读取 Flow 服务任务节点 `formKey/formFieldPermissions`
   - `BusinessFlowService#findNodeForm`：仅在流程节点没有配置时兜底读取历史 `nodeForms`
   - `BusinessFlowService#saveTaskFormContext`：同文件 `:237`

7. 代码优先复杂业务已有扩展点：
   - `BusinessCodeFormProvider`：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessCodeFormProvider.java:16`
   - `BusinessCodeFormProviderRegistry#listAssets`：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessCodeFormProviderRegistry.java:33`

8. 采购单审批样例已经注册代码表单资产，但资产信息还不完整：
   - `SamplePurchaseOrderCodeFormProvider#formAssets`：`forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/provider/SamplePurchaseOrderCodeFormProvider.java:38`
   - `SamplePurchaseOrderCodeFormProvider#buildFields`：同文件 `:120`
   - 采购单 BPMN 仍手写 `flowable:formUrl`：`forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/support/SamplePurchaseOrderFlowBpmn.java:24`

9. 标准 BPMN 流程设计器仍暴露节点表单配置：
   - `NodePropertiesPanel.vue:357` 显示“表单类型”。
   - `NodePropertiesPanel.vue:368` 选择 `formKey`。
   - `NodePropertiesPanel.vue:443` 输入 `formUrl`。

10. 钉钉样式流程设计器也有节点表单权限与条件配置：
    - `DingFlowDesigner.vue:32` 接收 `formFieldCatalog`。
    - `ApproverConfig.vue:76` 显示“表单权限”页签。
    - `FormPermissionConfig.vue:3` 根据流程全局动态表单字段配置权限。
    - `ConditionConfig.vue:7` 支持业务字段条件，`ConditionConfig.vue:77` 支持同意/驳回等业务选项。

11. 业务流程配置页不应再维护节点表单列表：
    - `BusinessFlowBindingPanel.vue` 只展示流程节点配置入口，跳转 `/flow/design` 并携带 `businessObjectCode/codeApp`。
    - 字段权限矩阵在流程设计器节点抽屉中呈现，用户选中节点后配置当前节点。

12. 发布检查已经检查单据闭环，但仍按分散面板定位修复入口：
    - `BusinessObjectReadinessService#checkDocumentClosureStatus`：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectReadinessService.java:332`
    - 缺单据时跳转 `panel=document`，缺流程时跳转 `panel=automation`。

### 2.2 现有实现的合理部分

- JeecgBoot 那类“流程模型关联业务表 + 配置流程状态字段”的方式，在 Forge 中对应 `BusinessFlowBindingDTO.BusinessBindingDTO`，可作为简单业务表和低代码对象的最小集成能力。
- 低代码对象不应该为每条流程生成手写 Service；现有 `BusinessFlowService` 统一待办表单上下文可以继续作为主链路，节点策略优先来自 BPMN 节点配置，历史 `nodeForms` 只作兜底。
- 代码优先复杂业务不需要迁移到低代码对象，应该通过 `BusinessCodeFormProvider` 注册表单资产、上下文和保存能力。
- 流程设计器里的条件线应继续保留“同意通过 / 驳回修改 / 退回上一步 / 终止流程 / 业务字段条件”，表达式只作为开发者高级配置。

### 2.3 发现与风险

- 配置所有权不清晰：同一节点表单可能在业务应用配置和流程设计器里同时维护。
- 业务人员仍可能看到手写路径、`formKey`、`formUrl`、`${approvalResult == 'reject'}` 这类开发概念。
- 节点表单策略如果继续放在应用配置列表里，用户很难判断当前权限对应流程中的哪个审批节点；字段权限矩阵必须嵌到流程设计器节点配置上下文里。
- 代码表单的字段权限虽然会传给 Provider，但当前 Provider 保存时是否过滤字段取决于业务实现，平台兜底不足。
- 已办/历史表单必须只读展示同一份业务表单内容，否则“我的已办看不见已填写表单内容”的问题会反复出现。
- 如果直接删掉流程设计器表单能力，会破坏独立流程、历史流程和简单流程表单场景。

## 3. 功能点

- [ ] 统一读取业务流程应用配置：输入 `objectCode`，后端一次返回对象基础信息、单据配置、流程绑定、表单资产、发布检查摘要；前端不再分别拼接两套配置状态。
- [ ] 统一保存业务流程应用配置：输入单据配置和流程绑定配置，后端按事务顺序保存单据规则、主流程绑定和变量映射，并返回最新配置摘要；节点级策略由流程设计器保存到流程模型。
- [ ] 统一业务应用设计器入口：把“单据设置”和“流程与自动化”整合为“单据流程”或“业务流程”配置页；旧 `panel=document`、`panel=automation` 路由兼容跳转到新面板并定位到对应分区。
- [ ] 表单资产选择器：流程设计器审批节点的“表单权限”页签通过下拉选择低代码表单、代码表单 Provider 资产或开发者外部地址；普通用户不输入 Vue 路由/URL。
- [ ] 代码优先表单字段权限：代码表单资产必须暴露字段目录；节点权限可配置可见、可编辑、必填；平台保存前按可编辑字段过滤数据，Provider 只收到允许字段。
- [ ] 应用设计拉起流程设计器：在“业务流程配置”中提供打开流程设计器入口，并携带业务对象上下文；节点配置在真实流程画布的节点抽屉中完成。
- [ ] 节点配置抽屉整合：选中人工节点后，右侧抽屉固定为 `审批设置 / 表单权限 / 高级设置` 三个页签；审批人、会签、驳回、抄送等高频能力和节点字段权限在同一处配置。
- [ ] 表单权限矩阵体验：`表单权限` 页签按字段逐行展示字段名、字段编码、字段类型、系统字段标识、可见、可编辑、必填；只读/系统字段禁用可编辑和必填；提供“全部可见”“清空可编辑”等批量操作。
- [ ] 低代码配置边界收敛：低代码只配置业务表单资产、状态字段、流程关联和变量映射；节点级表单权限、审批策略和高级策略统一进入流程节点配置。
- [ ] 保留独立流程表单设计：未绑定业务应用的流程仍可使用流程设计器原有动态表单、表单 URL、节点权限能力。
- [ ] 发布检查收口：发布检查按统一契约校验状态字段、主流程、变量映射、节点表单、代码 Provider 是否存在、触发器是否缺失。
- [ ] 采购单审批样例改造：采购单审批测试流程以代码表单资产接入，不以手写 `formUrl` 作为主要配置；用于验证部门负责人、工程经理、会签、驳回修改、已办只读展示。

## 4. 业务规则

1. 业务应用绑定流程时，主入口是应用设计里的“业务流程配置”，但节点级配置必须拉起真实流程设计器完成。应用配置页不维护脱离流程图的节点配置副本。
2. 节点表单策略属于流程节点配置体验。用户在流程设计器选中节点后，在同一个右侧配置抽屉内完成审批办理、表单资产、表单权限和高级设置。
3. 低代码业务对象配置负责业务表单资产、单据状态字段、流程关联和变量映射；节点级审批策略、会签、抄送、驳回、字段可见/可写/必填放在流程节点配置中。
4. 数据落点以流程节点为准：业务应用绑定流程后的节点表单资产和字段权限保存到 BPMN 节点 `formKey/formUrl/formJson/formFieldPermissions`；`BusinessFlowBinding.nodeForms` 只作为历史兼容兜底。
5. 节点配置抽屉至少包含：
   - `审批设置`：审批人、角色、部门、会签、抄送、驳回、退回、终止等业务化配置。
   - `表单权限`：选择当前节点使用的业务表单资产，并用字段权限矩阵配置可见、可编辑、必填。
   - `高级设置`：节点 ID、表达式、监听器、外部地址、Provider 技术信息等开发者兜底能力。
6. 运行时表单解析优先级固定为：

   ```text
   BPMN 节点 formKey/formUrl/formJson/formFieldPermissions
     > BusinessFlowBinding.nodeForms 兼容兜底
     > 流程默认表单
   ```

7. 外部地址只作为开发者高级兜底，不作为普通业务人员的默认选择。
8. 低代码表单和代码表单都必须走统一 `BusinessTaskFormContextVO`，待办可编辑，已办/历史只读。
9. 驳回修改的默认体验：
   - 低代码简单字段修改：优先在待办审批表单内直接修改并重提。
   - 代码优先复杂业务：可配置“待办内嵌修改”或“跳转业务单据修改”；跳转模式保存业务页后调用重提接口。
10. 表单字段权限必须后端兜底，前端隐藏/禁用只作为交互优化。
11. 采购合同、采购单、库存、资金等复杂业务可以只注册代码表单 Provider，不强制创建低代码应用。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
| --- | --- | --- | --- |
| 无 | `ai_business_document_config` | 无 | 首期继续保存单据配置。 |
| 新增内置数据 | `ai_business_binding` | 无结构变更 | 新增 `V1.0.82__seed_sample_purchase_order_flow_binding.sql`，为采购单代码业务内置默认 `FLOW` 绑定；`nodeForms` 仅作历史兼容兜底。 |
| 新增内置数据 | `ai_business_suite` / `ai_business_object` / `ai_business_app` | 无结构变更 | 新增 `V1.0.83__seed_sample_purchase_order_app_center_entry.sql`，把采购单审批测试作为“采购”业务域下的代码型业务对象和应用入口显示在应用中心。 |
| 更新内置数据 | `ai_business_binding` | 无结构变更 | 新增 `V1.0.84__extend_sample_purchase_order_form_asset_fields.sql`，为已存在采购单旧 `nodeForms` 兜底配置补充申请人修改节点 `needDate` 字段权限。 |
| 无 | `sample_purchase_order` | 无 | 采购单样例只调整配置和 Provider 资产，不新增表字段。 |

首期不新增 Flyway 表结构脚本。采购单样例新增的是可重复执行的 seed 数据脚本，用于内置业务流程绑定和历史 `nodeForms` 兜底；新节点配置以流程模型 BPMN 节点属性为准。若后续要做契约版本化，再单独设计 `ai_business_flow_contract_version`。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
| --- | --- | --- | --- |
| 新增 | `/ai/business/flow-app/config/{objectCode}` | GET | 返回统一业务流程应用配置。 |
| 新增 | `/ai/business/flow-app/config/{objectCode}` | PUT | 事务保存单据配置和流程绑定。 |
| 增强 | `/ai/business/flow/form-assets/{objectCode}` | GET | 表单资产补充 `fields`、`providerKey`、`providerName`、`supportsSave`、`formMode`。 |
| 增强 | `/ai/business/flow/task-form-context` | GET | 代码表单和低代码表单统一返回字段权限与记录数据。 |
| 增强 | `/ai/business/flow/task-form-context/readonly` | GET | 已办/历史统一只读展示业务表单内容。 |
| 增强 | `/ai/business/flow/model/{modelKey}/variables` | GET | 保持变量和人工节点输出，供统一配置页同步节点表单。 |
| 可选新增 | `/ai/business/flow/model/{modelKey}/business-bindings` | GET | 查询流程模型是否被业务应用绑定，供流程设计器识别业务上下文，并兼容流程管理中的独立流程场景。 |

## 7. 影响范围

- 后端：
  - `forge-plugin-generator` 业务应用流程配置、表单资产、待办表单上下文、发布检查。
  - `forge-business-core` 采购单审批样例 Provider 与流程初始化数据。
- 前端：
  - `forge-admin-ui/src/api/business-app.js`
  - `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessDocumentPanel.vue`
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue`
  - 新统一配置面板与表单资产选择器。
  - `forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue`
  - `forge-admin-ui/src/components/flow-designer/*`

## 8. 风险与关注点

⚠️ 本变更涉及状态流转、字段权限和待办保存，必须重点验证：

- 状态字段配置错误不能导致业务记录进入错误状态。
- 节点不可写字段不能通过接口绕过提交。
- 代码表单 Provider 不存在时，发布检查必须阻断或给出明确警告。
- 应用配置页不能和流程设计器产生两套节点配置副本；业务绑定流程的表单权限以 BPMN 节点配置为准，`BusinessFlowBinding.nodeForms` 仅兜底。
- 流程设计器整合不能删除独立流程的表单能力；未绑定业务应用的流程仍按原流程表单能力运行。
- 采购单样例的 BPMN 部署、会签、驳回修改、已办展示必须保持可用。
- 统一保存接口如果单据保存成功但流程绑定失败，必须整体回滚或明确保持旧配置。

## 8.5 测试策略

- **测试范围**：
  - 后端单元/集成：统一配置 Facade、表单资产目录、节点权限过滤、发布检查。
  - 前端静态和构建：统一配置页、流程设计器入口、节点配置抽屉、资产选择器、字段权限矩阵。
  - 样例验收：采购单审批从提交到通过、驳回修改、会签、已办只读展示。
- **覆盖率目标**：
  - 至少覆盖低代码表单资产、代码表单资产、外部表单高级模式三类选择。
  - 至少覆盖一个可写字段被允许、一个不可写字段被过滤、一个必填字段缺失被拒绝。
  - 至少覆盖“应用设计打开流程设计器 -> 选中流程节点 -> 配置表单资产和字段权限 -> 保存流程 -> 待办按权限生效”的一体化链路。
- **独立 Test Spec**：是，见 `test-spec.md`。

## 9. 待澄清

- 无阻塞澄清项。按当前结论进入实现时，默认采用“后端统一 Facade + 前端统一配置页 + 保留原有接口兼容”的渐进方案。

## 10. 技术决策

1. 配置体验所有权：业务应用绑定流程后，应用配置只负责关联、资产目录和变量映射；节点业务表单在流程设计器节点配置中维护，用户选中节点后通过 `审批办理 / 表单权限 / 高级设置` 完成配置。
2. 数据落点：首期不新增表，复用 `ai_business_document_config` 和 `ai_business_binding.binding_config`。
3. 统一 API：新增 `flow-app/config` Facade，不删除既有 `document/config` 和 `flow/binding` 接口，降低回归风险。
4. 表单资产：用 `BusinessFormAssetRegistry` 思路渐进落地，首期扩展 `BusinessFlowService#getFormAssets` 和 `BusinessCodeFormProviderRegistry#listAssets`。
5. 代码优先业务：继续使用 `BusinessCodeFormProvider`，但资产必须包含字段目录，保存前由平台过滤字段权限。
6. 数据所有权：业务绑定流程的节点表单策略保存到 BPMN 节点属性；`BusinessFlowBinding.nodeForms` 只保留兼容兜底，不再作为新配置主路径。
7. 独立流程兼容：未绑定业务应用的流程仍保留原动态表单、表单 URL 和节点字段权限能力；业务应用带上下文打开流程设计器不应破坏流程管理的独立使用场景。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
| --- | --- | --- | --- |
| Task 0 | completed | `spec.md`, `tasks.md`, `test-spec.md`, `execution-log.md` | 完成 SDD 提案，不涉及业务代码。 |
| Task 1 | completed | `BusinessFlowAppConfigDTO.java`, `BusinessFlowAppConfigVO.java`, `BusinessFlowAppConfigService.java`, `BusinessFlowAppConfigController.java` | 新增统一配置 Facade，事务保存单据配置和流程绑定。 |
| Task 2 | completed | `business-app.js`, `BusinessFlowAppConfigPanel.vue`, `BusinessDocumentPanel.vue`, `BusinessFlowBindingPanel.vue` | 统一面板已接入 `flow-app/config` 一次读取/保存。 |
| Task 3 | completed | `object-designer.[objectCode].vue`, `BusinessObjectDesignerShell.vue`, `BusinessUnitCard.vue`, `ObjectCard.vue`, `BusinessActionDesigner.vue`, `BusinessObjectReadinessService.java` | 入口合并为“业务流程配置”，旧 `document/automation` 路由兼容到 `flow-app`；代码型业务对象在应用中心默认打开“业务流程配置”。 |
| Task 4 | completed | `BusinessFlowFormAssetSelect.vue`, `BusinessFlowBindingPanel.vue`, `BusinessFlowService.java`, `BusinessCodeFormProviderRegistry.java`, `SamplePurchaseOrderCodeFormProvider.java`, `BusinessFlowVariableResolver.java`, `FlowVariableMappingEditor.vue` | 节点表单改为资产选择，代码表单资产暴露字段目录；代码业务没有低代码 `configKey` 时，变量映射字段候选项从 Provider 字段目录兜底读取。 |
| Task 5 | completed | `BusinessFlowService.java` | 低代码和代码表单都做后端字段权限过滤，已办/历史强制只读。 |
| Task 6 | completed | `NodePropertiesPanel.vue`, `DingFlowDesigner.vue`, `NodeConfigDrawer.vue`, `ApproverConfig.vue`, `FormPermissionConfig.vue` | 流程设计器节点抽屉恢复为节点配置主入口，支持表单资产和字段权限。 |
| Task 7 | completed | `BusinessObjectReadinessService.java`, `BusinessFlowService.java` | 发布检查修复入口统一到 `flow-app`，表单资产和代码 Provider 校验能力接入。 |
| Task 8 | completed | `SamplePurchaseOrderCodeFormProvider.java`, `SamplePurchaseOrderFlowBpmn.java`, `SamplePurchaseOrderServiceImpl.java`, `BusinessFlowService.java`, `todo.vue`, `done.vue`, `V1.0.82__seed_sample_purchase_order_flow_binding.sql`, `V1.0.83__seed_sample_purchase_order_app_center_entry.sql`, `V1.0.84__extend_sample_purchase_order_form_asset_fields.sql` | 采购单代码业务默认 `nodeForms` 已迁入业务流程绑定；BPMN 不再写死业务表单 URL/字段权限；待办/已办统一走业务表单上下文；应用中心显示“采购/采购申请/采购单审批测试”；字段目录和申请人修改节点 `needDate` 权限已补齐。 |
| Task 9 | completed | `test-spec.md`, `execution-log.md` | 已执行后端目标编译、前端 lint/build 和 `git diff --check`。 |
| Task 10 | completed | `BusinessFlowBindingPanel.vue`, `flow/design.vue`, `DingFlowDesigner.vue`, `NodeConfigDrawer.vue`, `ApproverConfig.vue`, `FormPermissionConfig.vue`, `BusinessFlowService.java`, `FlowClient.java`, `SamplePurchaseOrderFlowBpmn.java` | 应用配置页移除独立节点工作台，改为打开流程设计器；流程设计器节点抽屉承载审批办理、表单资产和字段权限；运行时优先读 Flow 节点表单信息，`nodeForms` 仅兜底。 |

## 12. 审查结论

尚未进入 `/review`。本轮 `/apply` 已完成实现和自动化验证；采购单“提交 -> 审批 -> 会签 -> 驳回修改 -> 已办只读”的完整人工流程仍需在本地服务与数据库环境验收。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-06-28
- **确认人**：用户
- **确认内容**：用户发起 `apply unified-business-flow-app-config`，确认进入编码实现阶段。
