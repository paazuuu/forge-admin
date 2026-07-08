# 任务拆分 — unified-business-flow-app-config
> status: applied
> created: 2026-06-28
> 原则：No Spec No Code；首期不重写低代码运行底层；不新增独立审批引擎；业务应用绑定流程时应用配置只维护流程关联、业务表单资产目录和变量映射，节点业务表单和字段权限在流程设计器节点配置中维护，运行时优先读取 BPMN 节点配置，`nodeForms` 仅作兼容兜底；标准 BPMN 与钉钉样式设计器都必须兼容。

## 前置条件

- [x] 用户确认 `spec.md` 的架构边界和任务拆分。
- [x] 执行编码前读取 `code-copilot/memory/pitfalls.md`、`code-copilot/memory/decisions.md`、`code-copilot/memory/preferences.md`。
- [x] 执行验证前读取 `code-copilot/rules/automated-testing-standard.md` 并追加 `execution-log.md`。

## Task 0：SDD 提案与现状基线

- **目标**：冻结本轮变更范围，明确统一配置、代码优先表单、流程设计器表单能力的边界。
- **状态**：completed
- **涉及文件**：
  - `code-copilot/changes/unified-business-flow-app-config/spec.md` — 新增，记录需求规格。
  - `code-copilot/changes/unified-business-flow-app-config/tasks.md` — 新增，记录任务拆分。
  - `code-copilot/changes/unified-business-flow-app-config/test-spec.md` — 新增，记录测试策略。
  - `code-copilot/changes/unified-business-flow-app-config/execution-log.md` — 新增，记录执行日志。
- **验收标准**：
  - Spec 包含代码现状出处。
  - Tasks 拆分到可独立提交的任务。

## Task 1：后端统一业务流程应用配置 Facade

- **目标**：新增统一读取/保存接口，前端不再分别拼接单据配置和流程绑定状态。
- **状态**：completed
- **涉及文件**：
  - 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessFlowAppConfigDTO.java`
  - 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessFlowAppConfigVO.java`
  - 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowAppConfigService.java`
  - 新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessFlowAppConfigController.java`
- **关键签名**：
  ```java
  public BusinessFlowAppConfigVO getConfig(String objectCode)
  public BusinessFlowAppConfigVO saveConfig(String objectCode, BusinessFlowAppConfigDTO dto)
  ```
- **工作内容**：
  - `getConfig` 根据 `objectCode` 解析 `AiBusinessObject`，返回 `objectId/suiteCode/objectCode/objectName`。
  - 组合 `BusinessDocumentConfigService#getConfig(objectId)` 和 `BusinessFlowService#getFlowBinding(objectCode)`。
  - 返回表单资产目录 `BusinessFlowService#getFormAssets(objectCode)`。
  - `saveConfig` 使用 `@Transactional`，先保存单据配置，再保存流程绑定。
  - 权限使用 `ai:businessFlow:config`，接口加 `@ApiDecrypt`、`@ApiEncrypt`。
- **验收标准**：
  - `GET /ai/business/flow-app/config/{objectCode}` 能返回统一结构。
  - `PUT /ai/business/flow-app/config/{objectCode}` 保存后再次查询一致。
  - 既有 `/ai/business/document/config/{objectId}` 和 `/ai/business/flow/binding/{objectCode}` 保持兼容。

## Task 2：前端统一配置 API 与面板骨架

- **目标**：提供一个业务人员视角的“业务流程配置”页，内部承载单据规则、主流程、变量映射和流程设计器入口。
- **状态**：completed
- **涉及文件**：
  - 修改 `forge-admin-ui/src/api/business-app.js` — 新增统一配置 API。
  - 新增 `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue`
  - 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessDocumentPanel.vue` — 支持被统一面板作为分区复用或抽取纯配置块。
  - 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue` — 支持被统一面板作为分区复用或抽取纯配置块。
- **关键签名**：
  ```js
  export function businessFlowAppConfig(objectCode) {
    return request.get(`/ai/business/flow-app/config/${objectCode}`)
  }

  export function saveBusinessFlowAppConfig(objectCode, data) {
    return request.put(`/ai/business/flow-app/config/${objectCode}`, data)
  }
  ```
- **工作内容**：
  - 面板分区顺序：`基础单据`、`主流程`、`变量映射`、`流程设计器入口`、`触发与发布`。
  - 单据分区展示状态字段、编号规则、状态映射。
  - 流程分区展示流程模型、发起方式、标题模板、业务记录绑定。
  - 节点配置分区不编辑 `nodeForms`，只提供打开流程设计器的入口和当前流程节点摘要。
  - 右侧摘要显示状态字段、主流程、节点表单数量、触发器缺口。
- **验收标准**：
  - 普通用户可以在一个页面完成“状态字段 -> 主流程 -> 变量映射 -> 打开流程设计器”的连续配置。
  - 保存按钮一次保存统一配置。
  - 不出现普通用户需要输入 Vue 路径的主入口。
- **本轮补充结果**：
  - 统一配置页已按列表设计器风格重构为工作台布局：顶部标题工具栏、配置链路切换条、单一工作区。
  - 配置链路从左侧滚动锚点改为真实步骤切换，点击“单据规则 / 流程配置”立即切换当前工作区。
  - 统一页嵌入旧单据/流程配置块时隐藏重复标题和右侧摘要，避免卡片堆叠和视觉错位。
  - 代码应用模式只展示业务接入状态和流程绑定，不展示低代码单据规则。
  - 2026-06-29 继续收敛顶部区域：移除内层对象标题、重复刷新/保存按钮和旧摘要块，父级进入业务流程配置时不再展示侧栏“单据闭环配置”，避免状态信息堆成一行。
  - 2026-06-29 进一步移除“自动化触发器 / 发布检查”顶部页签，避免触发器点击跳出当前页面；单据规则和流程配置统一内容宽度、卡片样式、两列布局和响应式规则。
  - 2026-06-29 按用户参考图重构为“业务配置中心”样式：面包屑标题、下划线页签、四步横向卡片、浅灰工作区和白色内容卡片。
  - 2026-06-29 修正四步卡片和下方内容的真实联动：所有步骤补齐 active 态，点击“编号生成 / 状态字典 / 业务记录绑定 / 变量映射 / 节点表单”会同步切换选中样式和对应内容卡片；下方工作区新增当前配置项标题，明确展示当前页签、步骤标题和说明。
  - 2026-06-29 用户复盘后修正产品方向：应用配置页不承载独立节点工作台，只提供打开流程设计器入口；用户在流程画布中选中节点后，在节点配置抽屉的“表单权限”页签配置表单资产和字段权限。

## Task 3：业务对象设计器入口整合与路由兼容

- **目标**：把原“单据设置”和“流程与自动化”收口为统一入口，旧路由继续可用。
- **状态**：completed
- **涉及文件**：
  - 修改 `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`
  - 修改 `forge-admin-ui/src/views/app-center/components/BusinessUnitCard.vue`
  - 修改 `forge-admin-ui/src/views/app-center/components/ObjectCard.vue`
  - 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue`
  - 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessPublishChecklist.vue`
- **关键函数**：
  ```js
  function normalizePanel(panel) { }
  function handleFlowAppSaved(config = {}) { }
  ```
- **工作内容**：
  - 新增或重命名设计器面板 `flow-app`，展示 `BusinessFlowAppConfigPanel`。
  - `panel=document` 和 `panel=automation` 自动映射到 `flow-app`，并传入锚点 `document` 或 `flow`。
  - 闭环步骤把“单据设置”“主流程”合并为“单据流程”，但摘要里仍展示状态字段、主流程、触发器。
  - 发布检查修复入口统一跳转 `?panel=flow-app&section=document|flow|nodeForms|trigger`。
- **验收标准**：
  - 从对象卡片、发布检查、URL 参数进入时都能打开统一配置页。
  - 旧链接不 404，不丢失定位语义。
- **本轮补充结果**：
  - 应用中心首页业务单元卡片、业务域详情对象卡片都会识别 `options/designerOptions.codeApp=true`。
  - 代码型业务对象的主设计按钮显示为“业务流程配置”，点击直接进入 `flow-app` 面板。
  - 对象设计器在后端返回代码型业务对象后自动停留在“业务流程配置”，避免回到低代码“表单设计”。

## Task 4：业务表单资产选择器与代码表单字段目录

- **目标**：节点表单配置从“输入路径/编码”改为“选择业务表单资产”，低代码表单和代码表单都支持字段权限。
- **状态**：completed
- **涉及文件**：
  - 新增 `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowFormAssetSelect.vue`
  - 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue`
  - 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java`
  - 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessCodeFormProviderRegistry.java`
  - 修改 `forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/provider/SamplePurchaseOrderCodeFormProvider.java`
- **关键签名**：
  ```java
  public Map<String, Object> getFormAssets(String objectCode)
  public List<Map<String, Object>> listAssets(String objectCode)
  ```
- **工作内容**：
  - 表单资产统一字段：`formMode/formKey/formName/providerKey/providerName/formUrl/fields/supportsSave/description`。
  - 代码 Provider 资产把 `buildFields()` 输出到 `fields`，用于前端字段权限选择。
  - 流程设计器审批节点选择资产时写入节点 `formKey/formName`，保存流程后进入 BPMN 节点配置。
  - 外部地址折叠到“开发者高级配置”，普通路径不默认展示。
- **验收标准**：
  - 采购单审批表单在节点表单选择器中以“采购单审批表单”展示。
  - 选择代码表单后，可配置 `arrivalListFileIds` 只在部门负责人节点可写。
  - 不再要求业务人员知道 `/business/purchase-order-test`。
- **本轮补充结果**：
  - 采购单代码表单资产已补齐完整单据字段目录，包含只读系统字段和可编辑业务字段。
  - 节点字段权限配置已从三组多选框改为字段权限矩阵，按字段逐行配置可见、可编辑、必填。

## Task 5：待办表单权限后端兜底与已办只读一致性

- **目标**：低代码表单和代码表单都由平台兜底字段权限，已办能看到同一份表单内容且只读。
- **状态**：completed
- **涉及文件**：
  - 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java`
  - 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessTaskFormSaveDTO.java`
  - 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessTaskFormContextVO.java`
  - 修改 `forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/provider/SamplePurchaseOrderCodeFormProvider.java`
- **关键签名**：
  ```java
  private BusinessTaskFormSaveDTO filterSaveDataByPermissions(BusinessTaskFormSaveDTO dto,
                                                              List<Map<String, Object>> permissions)
  private Map<String, Object> filterVisibleRecordData(Map<String, Object> recordData,
                                                      List<Map<String, Object>> fields)
  ```
- **工作内容**：
  - `BUSINESS_OBJECT_FORM` 保持现有可写字段过滤。
  - `BUSINESS_CODE_FORM` 调用 Provider 前先按 `fieldPermissions.writable` 过滤 `dto.data`。
  - 必填字段校验对低代码和代码表单一致生效。
  - `task-form-context/readonly` 对低代码和代码表单都强制 `writable=false/readonly=true/disabled=true`。
- **验收标准**：
  - 接口提交不可写字段不会落库。
  - 必填字段缺失返回业务错误。
  - 我的已办能打开采购单审批表单并只读展示已填写内容。

## Task 6：流程设计器节点表单配置主入口（标准 BPMN + 钉钉样式）

- **目标**：业务应用绑定流程时，流程设计器作为节点业务表单和字段权限主配置入口；独立流程仍保留原能力。
- **状态**：completed
- **涉及文件**：
  - 修改 `forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue`
  - 修改 `forge-admin-ui/src/components/flow-designer/DingFlowDesigner.vue`
  - 修改 `forge-admin-ui/src/components/flow-designer/panel/NodeConfigDrawer.vue`
  - 修改 `forge-admin-ui/src/components/flow-designer/panel/ApproverConfig.vue`
  - 修改 `forge-admin-ui/src/components/flow-designer/panel/FormPermissionConfig.vue`
- **工作内容**：
  - 标准 BPMN 人工节点保留节点表单配置能力。
  - 钉钉样式审批节点“表单权限”页承接节点表单资产选择和字段权限矩阵。
  - 条件配置继续保留业务选项和业务字段规则，不回退到手写表达式主入口。
  - 未绑定业务应用时，原有流程表单设计、字段权限和外部 URL 能力不变。
- **验收标准**：
  - 标准 BPMN 和钉钉样式流程设计器都能在节点上配置表单资产和字段权限。
  - 旧独立流程模型仍能配置流程表单。
- **方向修正**：
  - 用户复盘后明确：节点配置工作台属于真实流程设计器页面，不属于低代码应用配置页。
  - 应用设计只提供进入流程设计器的上下文入口，节点表单权限成为流程节点配置的一部分。

## Task 7：发布检查合并统一流程契约规则

- **目标**：发布前能发现状态字段、主流程、变量映射、节点表单、代码 Provider 和触发器缺口。
- **状态**：completed
- **涉及文件**：
  - 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectReadinessService.java`
  - 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java`
  - 修改 `forge-admin-ui/src/views/app-center/components/designer/BusinessPublishChecklist.vue`
  - 修改 `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`
- **关键函数**：
  ```java
  private BusinessReadinessItemVO checkDocumentClosureStatus(AiBusinessObject object, Long tenantId)
  private List<String> validateNodeFormAssets(JSONObject bindingConfig, String objectCode)
  ```
- **工作内容**：
  - `DOCUMENT_CLOSURE_STATUS` 增加节点表单缺口校验。
  - `BUSINESS_CODE_FORM` 校验 Provider 是否注册、资产是否存在。
  - 变量映射缺失按 warning 或 configured 状态提示，避免流程启动后才失败。
  - 修复入口统一指向 `flow-app` 对应分区。
- **验收标准**：
  - 缺状态字段、缺主流程、缺节点表单、缺代码 Provider 都能给出可操作修复入口。
  - 发布检查不会要求未启用单据闭环的普通 CRUD 配置流程。

## Task 8：采购单审批样例迁移到统一配置验收路径

- **目标**：用采购单审批验证代码优先复杂业务接入：表单资产选择、节点字段权限、会签、驳回修改、已办只读。
- **状态**：completed
- **涉及文件**：
  - 修改 `forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/support/SamplePurchaseOrderFlowBpmn.java`
  - 修改 `forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/service/SamplePurchaseOrderService.java`
  - 修改 `forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/service/impl/SamplePurchaseOrderServiceImpl.java`
  - 修改 `forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/provider/SamplePurchaseOrderCodeFormProvider.java`
  - 修改 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java`
  - 修改 `forge-admin-ui/src/views/flow/todo.vue`
  - 修改 `forge-admin-ui/src/views/flow/done.vue`
  - 修改 `forge-admin-ui/src/views/business/purchase-order-test.vue`
  - 新增 `forge-server/db/migration/V1.0.82__seed_sample_purchase_order_flow_binding.sql`
  - 新增 `forge-server/db/migration/V1.0.83__seed_sample_purchase_order_app_center_entry.sql`
  - 新增 `forge-server/db/migration/V1.0.84__extend_sample_purchase_order_form_asset_fields.sql`
- **工作内容**：
  - Flyway 内置采购单代码业务默认业务流程绑定，`nodeForms` 仅作历史兼容兜底。
  - BPMN 不再写死 `formUrl`，每个人工节点写入 `flowable:formKey=sample_purchase_order_approval_form` 和节点 `formFieldPermissions`。
  - 部门负责人节点仅开放 `arrivalListFileIds/deptLeaderRemark` 等字段。
  - 工程经理、会签、申请人修改节点分别开放自己的字段。
  - 待办和已办优先使用 `task-form-context` / `readonly` 的统一业务表单上下文渲染代码业务表单，完整业务页只作为跳转入口。
- **验收标准**：
  - 采购单提交后，部门负责人节点能看到并编辑上传清单，其它节点不能编辑该字段。
  - 驳回修改后申请人能修改业务字段并重提。
  - 已办详情只读显示已填写字段。
- **本轮结果**：
  - 已补齐采购单代码表单资产字段目录、`supportsSave` 和代码优先上下文兼容，纯代码业务没有低代码 `configKey` 时也能进入 Provider 表单上下文。
  - 已新增 `V1.0.82__seed_sample_purchase_order_flow_binding.sql`，把采购单默认节点表单和字段权限写入 `ai_business_binding.binding_config.nodeForms`，且带 `NOT EXISTS` 防重复保护。
  - 已新增 `V1.0.83__seed_sample_purchase_order_app_center_entry.sql`，把采购单审批测试放到应用中心“采购”业务域下，并创建 `采购申请` 业务对象和 `采购单审批测试` 应用入口。
  - 已新增 `V1.0.84__extend_sample_purchase_order_form_asset_fields.sql`，补充申请人修改节点的 `needDate` 字段权限，避免已执行旧 seed 的环境缺少该字段。
  - 已移除采购单样例 BPMN 中的旧 `flowable:formUrl`，改为在人工节点写入 `flowable:formKey` 和 `flowable:formFieldPermissions`，确保运行时读取流程节点配置。
  - 已调整待办/已办页，代码业务和低代码业务都优先走统一业务表单上下文，审批保存前由后端按可写字段过滤。
  - 已补齐统一配置 Facade 的代码应用 fallback：`sample_purchase_order` 没有低代码业务对象时，仍可通过代码表单资产进入 `GET/PUT /ai/business/flow-app/config/{objectCode}`。
  - 已在采购单审批测试页新增“业务流程配置”入口，跳转 `/app-center/object-designer/sample_purchase_order?panel=flow-app&codeApp=1`。
  - 对象设计器支持代码应用虚拟上下文，代码应用只展示业务流程配置，不暴露低代码表单/列表/发布入口。
  - 代码应用模式下节点表单资产在流程设计器节点抽屉中选择；`nodeForms` 只保留兼容旧数据兜底。
  - 未启动本地服务做采购单完整人工流转；提交、审批、会签、驳回修改、已办只读仍需在完整本地环境验收。

## Task 9：自动化验证与日志归档

- **目标**：按项目自动化测试标准完成增量验证，并记录可复用结论。
- **状态**：completed
- **涉及文件**：
  - 修改 `code-copilot/changes/unified-business-flow-app-config/test-spec.md`
  - 修改 `code-copilot/changes/unified-business-flow-app-config/execution-log.md`
  - 视实际发现更新 `code-copilot/memory/pitfalls.md` 或 `code-copilot/memory/decisions.md`
- **验证命令**：
  ```bash
  cd forge-server
  JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile
  ```
  ```bash
  cd forge-server
  JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-business/forge-business-core -am -DskipTests compile
  ```
  ```bash
  cd forge-admin-ui
  source ~/.nvm/nvm.sh && nvm use v20.19.0
  pnpm exec eslint src/api/business-app.js src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue src/views/app-center/components/designer/BusinessFlowFormAssetSelect.vue src/components/bpmn/NodePropertiesPanel.vue src/components/flow-designer/DingFlowDesigner.vue
  ```
  ```bash
  cd forge-admin-ui
  source ~/.nvm/nvm.sh && nvm use v20.19.0
  pnpm build
  ```
  ```bash
  git diff --check
  ```
- **验收标准**：
  - 后端目标模块编译通过。
  - 前端 lint/build 通过。

## Task 10：应用设计拉起流程设计器节点配置

- **目标**：解决“节点表单太丑 / 和流程设计割裂”。应用设计只负责流程关联和进入流程设计器；用户在真实流程设计器页面选中节点后，通过节点配置抽屉完成审批办理、表单资产和字段权限配置。
- **状态**：completed
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue`
  - `forge-admin-ui/src/views/flow/design.vue`
  - `forge-admin-ui/src/components/flow-designer/DingFlowDesigner.vue`
  - `forge-admin-ui/src/components/flow-designer/panel/NodeConfigDrawer.vue`
  - `forge-admin-ui/src/components/flow-designer/panel/ApproverConfig.vue`
  - `forge-admin-ui/src/components/flow-designer/panel/FormPermissionConfig.vue`
  - `forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue`
  - `forge-server/forge-flow/forge-flow-client/src/main/java/com/mdframe/forge/flow/client/FlowClient.java`
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java`
  - `forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/support/SamplePurchaseOrderFlowBpmn.java`
- **工作内容**：
  - `BusinessFlowBindingPanel.vue` 移除应用配置内的独立节点配置工作台，改为展示“流程节点配置”入口卡片。
  - 点击“打开流程设计器”跳转 `/flow/design`，携带 `id/businessObjectCode/codeApp/source=appCenter`。
  - `flow/design.vue` 在携带业务对象上下文时读取业务表单资产，构造流程设计器节点表单资产选项和字段目录。
  - `DingFlowDesigner -> NodeConfigDrawer -> ApproverConfig` 透传节点可选表单资产。
  - 审批节点页签从“审批人设置”调整为“审批办理”；“表单权限”页签新增节点表单资产选择器，并继续展示字段权限矩阵。
  - 选择节点表单资产写入节点 `config.formKey/formName/formType`，保存流程时由现有 BPMN writer 写入 `flowable:formKey`。
  - 字段权限写入节点 `config.formFieldPermissions`，保存流程时写入 `flowable:formFieldPermissions`。
  - 后端待办上下文通过 `FlowClient#getTaskFormInfo(taskId)` 优先读取流程节点 `formKey/formUrl/formFieldPermissions`，找不到时才回退历史 `BusinessFlowBinding.nodeForms`。
  - 采购单样例 BPMN 人工节点补齐 `flowable:formKey` 和节点字段权限，seed 中 `nodeForms` 标注为兼容兜底。
- **验收标准**：
  - 从应用中心进入采购单审批测试后，“业务流程配置”只展示流程关联、变量映射和打开流程设计器入口，不再出现独立节点配置列表。
  - 点击“打开流程设计器”进入真实流程设计器页面，并带入 `sample_purchase_order` 业务字段目录。
  - 在流程设计器点击部门负责人、工程经理、会签、申请人修改等节点时，右侧节点配置能显示当前节点配置。
  - 在节点配置的“表单权限”页签中可选择“采购单审批表单”，并按字段配置可见、可编辑、必填。
  - 保存流程后，待办表单优先按 BPMN 节点 `formKey/formFieldPermissions` 生效。
  - 字段权限矩阵样式接近用户参考图：行级字段、紧凑开关、清晰的可见/可编辑/必填列，而不是三组多选框。
  - 未绑定业务应用的独立流程不受影响，仍可在流程管理中配置原流程表单。
  - `execution-log.md` 记录命令、结果、跳过项和未联调原因。
- **本轮结果**：
  - 已按用户纠正移除应用配置内的独立节点配置工作台，避免和流程设计器形成两套配置副本。
  - 应用配置页新增“打开流程设计器”入口卡片，展示当前流程、人工节点数和业务字段目录摘要。
  - 流程设计器带业务对象上下文时加载 `businessFlowFormAssets(objectCode)`，节点抽屉可选择业务表单资产并使用对应字段目录。
  - 审批节点配置页签调整为 `审批办理 / 表单权限 / 审批权限 / 扩展配置`，其中“表单权限”包含表单资产选择和字段权限矩阵。
  - 后端运行时已改为优先读取 Flow 节点表单信息，历史 `nodeForms` 只作兜底。
  - 采购单样例 BPMN 已在人工节点写入 `formKey/formFieldPermissions`，与运行时读取路径一致。
  - 已修复代码业务变量映射字段候选项：`sample_purchase_order` 缺少低代码 `configKey` 时，后端改从 `BusinessCodeFormProviderRegistry` 读取代码表单 Provider 暴露的 `fields/fieldCatalog`，前端变量映射左侧单据字段同时合并接口返回的 `fieldCandidates`。
  - 已在变量映射编辑器增加“业务字段 -> 流程变量”说明，明确只有条件分支、审批人表达式或标题模板需要用到的字段才需要映射，避免用户误以为每个单据字段都必须配置。
  - 尚未启动后端、MySQL、Redis 和 Flow 服务做浏览器联调；完整人工流转仍需在本地服务环境验证。
