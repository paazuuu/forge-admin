# 代码应用元数据配置化任务清单

## Task 1 SDD 文档

- [x] 创建 `code-copilot/changes/code-app-metadata-config/spec.md`
- [x] 创建 `code-copilot/changes/code-app-metadata-config/tasks.md`
- [x] 创建 `code-copilot/changes/code-app-metadata-config/test-spec.md`
- [x] 创建 `code-copilot/changes/code-app-metadata-config/execution-log.md`

## Task 2 后端元数据保存与合并

- [x] 修改 `BusinessFlowAppConfigService`：代码应用保存时把 `dto.options.codeAppMetadata` 合并进 `flowBinding.options`。
- [x] 修改 `BusinessFlowAppConfigService`：代码应用读取时返回 `options.codeAppMetadata` 和配置后的 `formAssets`。
- [x] 修改 `BusinessFlowService#getFormAssets`：代码 Provider 默认资产与 `binding_config.options.codeAppMetadata` 合并。
- [x] 修改 `BusinessFlowService#buildBusinessCodeFormContext`：待办上下文按配置化字段目录过滤 Provider 返回字段。
- [x] 保持无配置时 Provider 默认资产完全兼容。

## Task 3 复用现有表单/列表/详情设计器

- [x] 删除错误新增的 `BusinessCodeAppMetadataPanel.vue` 字段配置面板。
- [x] 修改 `BusinessFlowAppConfigPanel.vue`：代码应用业务配置中心只保留“流程配置”。
- [x] 修改 `object-designer.[objectCode].vue`：代码应用左侧开放“表单设计”“列表设计”“业务流程配置”。
- [x] 代码应用进入设计器时从 Provider/formAssets 初始化 `fields/modelSchema/formDesignerSchema/viewSchema/pageSchema`。
- [x] 表单设计保存复用 `BusinessFormDesigner.syncDesignerDraft()` 输出 `formDesignerSchema`。
- [x] 列表设计保存复用 `BusinessListDesigner.syncDesignerDraft()` 输出 `viewSchema.list.columns`。
- [x] 详情设置保存复用 `BusinessDetailDesigner.syncDesignerDraft()` 输出 `viewSchema.detail.sections`。
- [x] 保存应用配置时携带 `options.codeAppMetadata`，不丢失流程绑定配置。

## Task 4 采购审批列表/详情消费配置

- [x] 新增前端 composable `useCodeAppMetadata.js`。
- [x] 修改 `purchase-order-test.vue`：加载 `sample_purchase_order` 代码应用元数据。
- [x] 列表列按 `LIST` 配置过滤和排序。
- [x] 详情弹窗字段按 `DETAIL` 配置过滤。
- [x] 无配置时保持现有默认列和详情字段。

## Task 5 验证

- [x] 读取 `code-copilot/rules/automated-testing-standard.md`。
- [x] 编写 `test-spec.md`。
- [x] 执行前端 eslint。
- [x] 执行后端 Maven 编译。
- [x] 执行 `git diff --check`。
- [x] 追加 `execution-log.md`。

## Task 6 流程节点权限缺陷修复

- [x] 修改 `SamplePurchaseOrderServiceImpl#ensureFlowModel`：已存在且 BPMN XML 非空的采购流程模型不再被示例代码覆盖。
- [x] 修改 flow 表单信息接口/client：支持按 `processInstanceId/businessKey/processDefKey/taskDefKey` 兜底读取 BPMN 节点表单配置。
- [x] 修改 `BusinessFlowService#resolveFlowNodeForm`：任务表单信息为空或不完整时合并流程表单信息，确保 `formFieldPermissions` 进入业务表单上下文。
- [x] 修改 `useBusinessTaskFormContext`：显式字段权限存在时，未配置字段默认可见但不可编辑。
- [x] 修改 `purchase-order-test.vue`：待办模式先加载业务表单上下文，再按上下文定位采购单；顶部采购单信息同步叠加节点字段权限。
- [x] 执行本轮前后端增量验证并追加 `execution-log.md`。

## Task 7 代码应用字段基准与表单显隐兼容

- [x] 修改 `BusinessFlowService#mergeCodeAppAssets`：Provider 当前字段作为基准，`codeAppMetadata` 只叠加显示属性，避免保存一次后丢失业务表新增字段。
- [x] 修改 `BusinessFlowService#applyBusinessCodeMetadataFields`：审批上下文字段过滤识别 `formVisible=false`，应用管理表单隐藏对待办代码表单生效。
- [x] 修改 `object-designer.[objectCode].vue`：代码应用进入设计器时合并 Provider 资产和已保存 metadata，自动补齐表单、列表、详情默认 schema。
- [x] 修改 `createDefaultFormDesignerSchema`：支持代码应用显式包含只读字段，便于用既有表单设计器配置展示/隐藏。
- [x] 修改 `BusinessDetailDesigner`：无 editZone 字段时从 `viewSchema.detail.sections` 兜底显示详情字段。
- [x] 修改 `useCodeAppMetadata` 和 `purchase-order-test.vue`：新增 FORM 字段读取，采购新增/编辑/待办表单消费应用管理表单显隐配置。
- [x] 执行本轮 eslint、后端编译、前端构建和 `git diff --check`，追加 `execution-log.md`。

## Task 8 业务表单资产与流程全局表单接入

- [x] 修改 `BusinessFlowAppConfigPanel.vue`：代码应用增加“业务表单资产”配置步骤，可维护表单名称、formKey、formUrl、Provider、说明等通用资产信息。
- [x] 修改 `BusinessFlowAppConfigPanel.vue`：保存代码应用时把业务表单资产配置合并进 `options.codeAppMetadata.formAssets`，不覆盖表单/列表/详情设计器输出。
- [x] 修改 `object-designer.[objectCode].vue`：代码应用保存流程配置时同步当前表单资产配置，避免只保存流程绑定导致资产配置丢失。
- [x] 修改 `flow/design.vue`：业务对象上下文下的全局“表单配置”切换为应用表单资产选择；独立流程保留动态表单设计。
- [x] 修改 `flow/design.vue`：保存流程模型时保存业务全局表单引用，并让节点字段目录优先使用全局表单资产字段。
- [x] 修改流程设计器节点配置：节点表单未配置时继承全局表单资产字段目录，节点抽屉仍只配置字段权限。
- [x] 修改后端 `BusinessFlowService`：解析待办表单时支持流程全局业务表单引用作为节点表单缺省值。
- [x] 清理采购审批示例 Provider 中不必要的资产主配置逻辑，仅保留默认兜底和运行适配。
- [x] 执行本轮增量验证并追加 `execution-log.md`。

## Task 9 业务表单资产提示与设计器空白修正

- [x] 修改 `BusinessCodeAppFormAssetPanel.vue`：隐藏重复的应用名称、业务对象名称、业务名称编辑项，改为只读业务对象摘要。
- [x] 修改 `BusinessCodeAppFormAssetPanel.vue`：Provider 改为选择已注册 Provider，并增加来源说明提示。
- [x] 修改 `object-designer.[objectCode].vue`：代码应用表单资产合并改为配置优先，同时保留 Provider 默认字段和 metadata 额外资产。
- [x] 修改 `object-designer.[objectCode].vue`：旧配置只有空 `pageSchema/formDesignerSchema` 时，按当前 Provider 字段补齐左侧表单设计、列表设计和详情设置需要的 schema。
- [x] 执行本轮前端 eslint、前端构建和 `git diff --check`，追加 `execution-log.md`。

## Task 10 Provider 目录与字段兜底协议修正

- [x] 修改 `BusinessCodeFormProviderRegistry`：提供所有已注册 `BusinessCodeFormProvider` Bean 的 Provider 目录和默认资产字段。
- [x] 修改 `BusinessFlowService#getFormAssets`：返回 `providerCatalog`，让前端 Provider 下拉不依赖当前表单资产是否已配置。
- [x] 修改 `BusinessFlowAppConfigService`：已有 `ai_business_object` 的代码应用也补齐 `options.codeApp/codeAppMetadata`。
- [x] 修改 `BusinessCodeAppFormAssetPanel.vue`：配置顺序改为先选 Provider，表单 Key 改为系统引用展示，不再作为可编辑项。
- [x] 修改 `object-designer.[objectCode].vue`：字段基准从当前 Provider 资产读取，取不到时使用 `providerCatalog[].assets[].fields` 兜底。
- [x] 执行本轮前端 eslint、后端编译、前端构建和 `git diff --check`，追加 `execution-log.md`。

## Task 11 已有代码应用对象的设计器入口修正

- [x] 修改 `object-designer.[objectCode].vue`：嵌入式设计器也按 objectCode 拉完整业务对象，识别 `options/designerOptions.codeApp=true`。
- [x] 修改 `object-designer.[objectCode].vue`：已有 `ai_business_object` 的代码应用不再调用普通 `businessObjectDesigner(object.id)`，强制走 Provider/formAssets 字段导入路径。
- [x] 修改 `BusinessFlowAppConfigService`：已有业务对象的代码应用保存 `options.codeAppMetadata` 时也写入流程绑定 options，避免表单设计/列表设计保存后丢失。
- [x] 执行本轮前端 eslint、后端编译、前端构建和 `git diff --check`，追加 `execution-log.md`。

## Task 12 待办节点展示与业务上下文查询优化

- [x] 修改流程列表共享展示工具：节点名称展示时去掉尾部 `taskDefKey/activityId` 技术编码。
- [x] 修改待办、已办、我发起页面：列表元信息、抽屉副标题、基础信息和批量处理错误提示统一使用业务可读节点名。
- [x] 修改 `BusinessFlowService#enrichBusinessListDisplay`：同一列表请求内按业务对象缓存运行上下文，避免每条任务重复解析业务单据配置。
- [x] 修改 `BusinessFlowService#resolveTaskFormRuntimeContext`：审批表单上下文一次解析 canonical object、运行配置和单据配置，避免重复查询。
- [x] 执行本轮前端 eslint、前端构建、后端编译和 `git diff --check`，追加 `execution-log.md`。

## Task 13 审批表单加载误查流程模型和动态表单修正

- [x] 修改 `FlowTaskServiceImpl`：流程定义 Key 从 Flowable `ProcessDefinition.getKey()` / BPMN process id 解析，不再把 UUID 型 processDefinitionId 当作 `sys_flow_model.model_key` 查询。
- [x] 修改 `FlowTaskServiceImpl`：节点 `formKey` 引用流程全局业务表单时识别为 `formType=business`，不再当动态表单去查 `sys_flow_form`。
- [x] 修改 `FlowTaskServiceImpl`：节点表单配置读取兼容 `formMode/formType/type`，支持 `BUSINESS_CODE_FORM` 和 `BUSINESS_OBJECT_FORM`。
- [x] 修改 `BusinessFlowService#loadFlowNodeFormInfo`：`taskId` 已返回完整表单信息时不再额外调用流程实例表单信息接口，减少同请求重复查询。
- [x] 执行本轮后端编译和 `git diff --check`，追加 `execution-log.md`。

## Task 14 流程定义匹配兼容修正

- [x] 修改 `FlowTaskServiceImpl#getTaskDetail`：任务详情返回前把 `processDefKey` 归一化为流程模型 Key，避免把 Flowable UUID 或 `key:version:id` 透出给业务校验。
- [x] 修改 `BusinessFlowService#validateTaskAccess`：流程定义校验改为兼容 `key`、`key:version:id` 和历史 UUID 表示，不再误报“流程定义与当前任务不匹配”。
- [x] 保留任务 ID、办理人、流程实例、业务 Key 和任务节点校验，避免兼容流程定义表示时放开任务访问边界。
- [x] 执行本轮后端编译和 `git diff --check`，追加 `execution-log.md`。

## Task 15 待办审批详情表单首屏优化

- [x] 修改 `todo.vue`：审批抽屉打开时并行加载流程表单信息和业务表单上下文，不再先等流程表单信息返回后再串行加载业务上下文。
- [x] 修改 `FlowBusinessForm.vue`：支持把父级已加载的业务表单上下文透传给自定义业务组件。
- [x] 修改 `purchase-order-test.vue`：待办模式优先复用父级 `initialTaskContext.recordData` 渲染采购审批表单，避免重复请求业务上下文、代码应用配置和采购单详情。
- [x] 修改 `useBusinessTaskFormContext`：支持注入已加载上下文，并让字段显示/编辑判断直接基于上下文字段目录。
- [x] 执行本轮前端 eslint、前端构建和 `git diff --check`，追加 `execution-log.md`。

## Task 16 流程定义校验兜底与首屏请求参数收敛

- [x] 修改 `todo.vue`：待办详情首个业务表单上下文请求只传可信 `taskId`，不再从列表行携带可能未归一化的 `processDefKey`。
- [x] 修改 `todo.vue#buildBusinessTaskFormQuery`：`processDefKey` 只接受已加载表单信息中的归一化值，不再回退到待办列表行字段。
- [x] 修改 `BusinessFlowService#assertProcessDefinitionMatches`：流程定义表示差异只记录调试日志，不作为硬安全边界抛错。
- [x] 保留任务 ID、办理人、流程实例、业务 Key 和任务节点校验，避免参数收敛影响任务访问控制。
- [x] 执行本轮前端 eslint、后端编译和 `git diff --check`，追加 `execution-log.md`。

## Task 17 审批详情表单重复请求与后端重复解析优化

- [x] 修改 `todo.vue`：审批详情先加载业务表单上下文；若已识别为业务对象/业务代码表单，则不再额外请求 `/api/flow/task/form/{taskId}`。
- [x] 修改 `BusinessTaskFormContextVO` 和 `BusinessFlowService`：业务表单上下文返回节点审批策略，避免跳过前端表单接口后丢失同意/驳回/转办/签名/意见配置。
- [x] 修改 `FlowTaskServiceImpl#getTaskFormInfo`：复用同一次请求内已解析的流程模型和 BPMN 节点，避免 `sys_flow_model` 与 BPMN 解析重复执行。
- [x] 修改 `FlowTaskServiceImpl`：业务表单不再查询 `sys_flow_form_instance` 快照，动态表单才加载表单实例快照。
- [x] 执行本轮前端 eslint、后端编译和 `git diff --check`，追加 `execution-log.md`。

## Task 18 采购审批业务表单节点标签去编码

- [x] 修改 `purchase-order-test.vue`：审批详情业务表单头部节点标签不再直接渲染 `taskDefKey`。
- [x] 复用流程节点展示清洗工具 `getTaskDisplayName`，优先使用后端节点名并清理尾部技术编码，缺省时按采购节点映射中文名称兜底。
- [x] 执行本轮前端 eslint 和 `git diff --check`，追加 `execution-log.md`。

## Task 19 应用中心业务单元编辑

- [x] 新增 `BusinessObjectEditorDrawer.vue`：支持编辑所属业务域、业务单元名称、对象类型、图标、排序、启用状态和业务说明，对象编码只读。
- [x] 修改应用总览 `index.vue`：业务单元卡片菜单增加“编辑业务单元”，保存后刷新工作区。
- [x] 修改业务域详情 `suite.[suiteCode].vue`：对象卡片菜单增加“编辑业务单元”，跨业务域保存后切换到新业务域。
- [x] 修改业务单元详情 `object.[objectCode].vue`：顶部增加“编辑业务单元”按钮，保存后刷新详情并同步 URL 业务域参数。
- [x] 修改 `BusinessObjectService` 与 `BusinessObjectMapper`：跨业务域移动时同步迁移访问入口、设计版本、触发器和单据配置；存在对象关系时阻止移动。
- [x] 执行本轮前端 eslint、后端编译和 `git diff --check`，追加 `execution-log.md`。

## Task 20 业务单元显示字段配置收敛

- [x] 修改 `BusinessObjectWizardDrawer.vue`：新建业务单元不再展示“显示字段”，避免用户在字段未设计前手输编码。
- [x] 修改 `BusinessObjectEditorDrawer.vue`：基础编辑抽屉不再展示“显示字段”，保存时保留已有对象级默认值。
- [x] 修改 `object-designer.[objectCode].vue`：对象设计器基础信息文案改为“默认标题字段”，明确它只是关联关系未单独配置时的兜底。
- [x] 保持 `BusinessRelationDesigner.vue` 里的“运行态显示字段 / 目标对象回显字段”作为关联关系主配置入口。
- [x] 执行本轮前端 eslint 和 `git diff --check`，追加 `execution-log.md`。

## Task 21 表单资产移除、标题模板和停用确认

- [x] 修改 `BusinessCodeAppFormAssetPanel.vue`：业务表单资产支持移除，并保存 `removedFormAssetKeys`。
- [x] 修改 `BusinessFlowService#mergeCodeAppAssets`：合并 Provider 默认资产时过滤已移除资产，避免保存后重新出现。
- [x] 修改 `SamplePurchaseOrderServiceImpl#submit`：采购审批发起流程时读取绑定配置 `titleTemplate` 生成流程标题。
- [x] 修改应用总览、业务域详情和业务单元详情：业务域/业务单元停用前二次确认，删除保留既有确认。
- [x] 修改 `SuiteEditorDrawer.vue` 与 `BusinessObjectEditorDrawer.vue`：编辑抽屉中把启用状态改为停用并保存时二次确认。
- [x] 执行本轮前端 eslint、后端编译和 `git diff --check`，追加 `execution-log.md`。

## Task 22 采购审批驳回状态修正

- [x] 修改 `FlowTaskServiceImpl#completeTask`：任务 complete 前先把审批动作变量写入流程实例，保证任务完成监听器能读到 `approvalResult/approved`。
- [x] 修改 `SamplePurchaseOrderServiceImpl#handleTaskCompleted`：采购审批回调同时识别 `approvalResult=reject` 和 `approved=false`。
- [x] 执行本轮后端编译和 `git diff --check`，追加 `execution-log.md`。

## Task 23 采购审批申请人修改状态兜底

- [x] 修改 `SamplePurchaseOrderServiceImpl#handleFlowEvent`：订阅 `TASK_CREATED`，申请人修改任务创建时把采购单状态同步为 `NEED_MODIFY`。
- [x] 修改 `SamplePurchaseOrderServiceImpl#saveTaskFields`：存量申请人修改待办保存时，如果采购单仍是 `IN_PROCESS`，先自愈为 `NEED_MODIFY` 再保存字段。
- [x] 执行本轮后端编译和 `git diff --check`，追加 `execution-log.md`。

## Task 24 采购审批重新提交状态兜底

- [x] 修改 `SamplePurchaseOrderFlowDefinition`：增加采购审批普通审批节点判断，统一识别部门负责人、工程部经理和采购会签节点。
- [x] 修改 `SamplePurchaseOrderServiceImpl#handleTaskCreated`：普通审批任务创建时，如果采购单仍是 `NEED_MODIFY`，同步为 `IN_PROCESS`。
- [x] 修改 `SamplePurchaseOrderServiceImpl#saveTaskFields`：存量审批待办保存时，如果采购单仍是 `NEED_MODIFY`，先自愈为 `IN_PROCESS` 再保存字段。
- [x] 执行本轮后端编译和 `git diff --check`，追加 `execution-log.md`。

## Task 25 采购审批存量状态对账

- [x] 修改 `SamplePurchaseOrderMapper`：增加按业务 Key 批量查询当前活跃待办节点的 XML 查询。
- [x] 修改 `SamplePurchaseOrderServiceImpl#page/detail/detailsByIds`：返回前按活跃待办节点对 `IN_PROCESS/NEED_MODIFY` 状态做对账修复。
- [x] 执行本轮后端编译和 `git diff --check`，追加 `execution-log.md`。
