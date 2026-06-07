# 任务拆分 — 流程管理表单运行态与业务入口优化
> 拆分顺序：数据模型 → 接口协议 → 底层实现 → 上层编排 → 入口层
> 每个任务 = 可独立提交的原子变更（3-5 个文件为宜）
> 进入 `/apply` 前必须先确认 `spec.md` 的 HARD-GATE

## 前置条件

- [x] 用户确认 `spec.md` 中 `PROCESS_ONLY / BUSINESS_OBJECT / HYBRID` 三种数据模式。
- [x] 用户确认组织负责人解析规则：默认复用 `sys_org` 负责人字段。
- [x] 检查 `forge/db/migration/` 最新版本，确定 Flyway 脚本版本号。
- [x] 读取 `code-copilot/rules/automated-testing-standard.md`，创建或复用 `test-spec.md` 和 `execution-log.md`。

## Task 1: 数据模型与迁移脚本

- **执行状态**：完成。
- **目标**：新增流程入口、表单版本、表单实例和组织填报批次数据结构。
- **涉及文件**：
  - `forge/db/migration/V1.0.57__add_flow_form_runtime_entry.sql` — 新增表、字典、菜单和权限，版本号进入 `/apply` 时按最新迁移顺延。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowForm.java` — 扩展表单分类、字段目录、发布状态字段。
  - 新增 `FlowFormVersion.java`, `FlowEntry.java`, `FlowEntryFieldMapping.java`, `FlowFormInstance.java`, `FlowFillBatch.java`, `FlowFillBatchItem.java`。
  - 新增对应 Mapper 接口和 XML。
- **关键签名**：
  ```java
  public interface FlowEntryMapper extends BaseMapper<FlowEntry> {
      IPage<FlowEntry> selectEntryPage(Page<FlowEntry> page, @Param("query") FlowEntryQueryDTO query);
  }
  ```

## Task 2: 表单版本发布与字段目录解析

- **执行状态**：完成。
- **目标**：表单保存草稿、发布版本、解析字段目录，并修正分页参数。
- **涉及文件**：
  - `forge/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowFormController.java` — `page` 改 `pageNum`，新增发布和版本接口。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/FlowFormService.java` — 新增发布、版本、字段目录方法。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowFormServiceImpl.java` — 查询迁移 XML，发布不可变版本。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowFormMapper.xml` — 表单分页、启用列表、Key 查询。
- **关键签名**：
  ```java
  FlowFormVersion publishVersion(Long formId);
  List<FormFieldCatalogItemDTO> resolveFieldCatalog(String formKey, Long versionId);
  Page<FlowForm> getPage(String formName, Integer status, Integer pageNum, Integer pageSize);
  ```

## Task 3: 流程入口配置接口

- **执行状态**：完成。
- **目标**：实现流程入口 CRUD、可见范围、字段映射和运行配置查询。
- **涉及文件**：
  - 新增 `FlowEntryController.java`
  - 新增 `FlowEntryService.java`, `FlowEntryServiceImpl.java`
  - 新增 `FlowEntryDTO.java`, `FlowEntryQueryDTO.java`, `FlowEntryRuntimeVO.java`
  - `forge-admin-ui/src/api/flow.js` — 增加入口 API。
- **关键签名**：
  ```java
  IPage<FlowEntry> pageEntries(FlowEntryQueryDTO query, Integer pageNum, Integer pageSize);
  FlowEntryRuntimeVO getRuntimeEntry(String entryCode);
  void saveEntry(FlowEntryDTO dto);
  ```

## Task 4: PROCESS_ONLY 表单运行态

- **执行状态**：完成。
- **目标**：用户从入口填表后保存表单实例快照并发起流程，不创建业务表记录。
- **涉及文件**：
  - 新增 `FlowRuntimeController.java`
  - 新增 `FlowRuntimeService.java`, `FlowRuntimeServiceImpl.java`
  - `FlowInstanceServiceImpl.java` — 支持从入口传入业务键、标题、变量和表单实例关联。
  - `FlowTaskServiceImpl.java` — `getTaskFormInfo` 返回实例快照字段。
- **关键签名**：
  ```java
  FlowStartResultVO submitEntryForm(String entryCode, FlowEntrySubmitDTO dto);
  FlowFormInstanceVO getInstanceByProcessInstanceId(String processInstanceId);
  ```

## Task 5: BUSINESS_OBJECT / HYBRID 落表编排

- **执行状态**：完成。
- **目标**：入口提交时按映射创建业务记录，复用低代码业务流程链路。
- **涉及文件**：
  - `FlowRuntimeServiceImpl.java` — 根据 `dataMode` 分发 PROCESS_ONLY 或业务对象落表。
  - `FlowBusinessObjectRuntimeAdapterImpl.java` — 接入 `DynamicCrudService.insertInternal` 创建业务记录，并在流程启动后写入 `ai_business_flow_instance_link`。
  - `FlowBusinessObjectRuntimeAdapter.java` — 增加 `afterProcessStarted` 回调，用于业务记录与流程实例关联。
  - `BusinessFlowService.java` — 如需补充入口来源和表单实例 ID 参数，保持兼容旧调用。
  - `BusinessDocumentRuntimeService.java` — 保持单据状态校验和流程状态展示。
  - `DynamicCrudService.java` — 复用安全新增/更新，不新增物理表 SQL。
- **关键签名**：
  ```java
  BusinessRecordCreateResult createBusinessRecordFromForm(FlowEntry entry, Map<String, Object> formData);
  ```

## Task 6: 表单变量目录接入 BPMN 设计器

- **执行状态**：完成。
- **目标**：审批人、候选人、流转条件和模板配置都能选择表单字段。
- **涉及文件**：
  - `forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue` — 接收字段目录，审批人、候选用户、候选组和流转条件可选择表单字段/系统变量。
  - `forge-admin-ui/src/views/flow/design.vue` — 按模型绑定表单拉取字段目录；内联表单 fallback 到前端 schema 解析；透传给节点属性面板。
  - `forge-admin-ui/src/api/flow.js` — 字段目录 API。
- **关键签名**：
  ```js
  flowApi.getFormFieldCatalog({ modelKey, formKey, versionId })
  ```

## Task 7: 组织批量填报

- **执行状态**：调整完成。
- **目标**：保留组织批量填报底层编排能力，但不在流程管理中提供独立页面；该能力由具体业务应用承载。
- **涉及文件**：
  - 新增 `FlowFillBatchController.java`
  - 新增 `FlowFillBatchService.java`, `FlowFillBatchServiceImpl.java`
  - `FlowFillBatchServiceImpl.java` — 发布批次时解析目标组织，默认复用 `sys_org.leader_id/leader_name` 生成明细并发送站内信。
  - `FlowRuntimeServiceImpl.java` — 支持按 `batchItemId` 提交，补充组织/批次流程变量并回写明细。
  - 前端不新增 `flow/fillBatch.vue` 独立页面，避免把业务场景示例固化为流程管理菜单。
- **关键签名**：
  ```java
  void publishBatch(Long batchId);
  FlowStartResultVO submitBatchItem(Long itemId, FlowEntrySubmitDTO dto);
  List<Long> resolveOwnerUsers(FlowOwnerRuleDTO ownerRule, Long orgId);
  ```

## Task 8: 待办站内信自动推送

- **执行状态**：完成。
- **目标**：`TASK_CREATED` 生成待办后立即发送站内信，并保证幂等。
- **涉及文件**：
  - `FlowTaskEventListener.java` — 在 `flowTaskMapper.insert` 成功后发送 WEB 消息。
  - `MessageService.java` / `MessageServiceImpl.java` — 增加按 `bizType + bizKey` 幂等发送能力或查询方法。
  - `MessageSendRequestDTO.java` — 如缺少跳转地址字段则补充 `jumpUrl` 或放入 `params`。
  - `DefaultMessageReceiverResolver.java` — 确认角色/组织用户展开能力。
- **关键签名**：
  ```java
  SysMessage sendIfAbsent(MessageSendRequestDTO req, String bizType, String bizKey);
  void sendTaskCreatedMessage(FlowTask flowTask);
  ```

## Task 9: 入口配置产品边界回收

- **执行状态**：调整完成。
- **目标**：撤掉流程模型页内的“入口配置”产品入口，避免与低代码应用入口配置重复；业务入口展示位置改由低代码应用、业务对象或后续流程发起中心承载。
- **涉及文件**：
  - 删除 `forge-admin-ui/src/views/flow/components/FlowEntryConfigModal.vue`
  - 修改 `forge-admin-ui/src/views/flow/model.vue` — 移除更多操作中的“入口配置”。
  - 删除 `forge-admin-ui/src/views/flow/entry-runtime.[entryCode].vue`
  - 修改 `forge-admin-ui/src/router/index.js` — 移除隐藏入口运行路由。
- **关键签名**：
  - 入口运行和展示不由流程模型页直接配置；低代码业务应用继续负责业务入口、菜单和可见范围。

## Task 9.5: 流程模型测试发起入口

- **执行状态**：完成。
- **目标**：在流程模型页保留已部署模型的测试发起能力，方便设计人员用模型级表单验证流程流转，但不把它扩展为正式业务入口配置。
- **涉及文件**：
  - `forge-admin-ui/src/views/flow/model.vue` — 已部署模型的更多操作增加“发起测试”，弹窗内按模型 `formJson` 渲染动态表单，提交后调用流程启动接口。
  - `forge-admin-ui/src/api/flow.js` — 复用 `startProcess(modelKey, data)`。
- **关键签名**：
  ```js
  flowApi.startProcess(modelKey, {
    businessKey: `FLOW_TEST:${modelKey}:${Date.now()}`,
    businessType: 'FLOW_MODEL_TEST',
    variables,
    testStart: true,
  })
  ```

## Task 10: 设计器和任务详情 UI 改造

- **执行状态**：完成。
- **目标**：流程设计器全屏弹窗打开；待办/已办/我发起详情不再使用抽屉，并适配移动端。
- **涉及文件**：
  - `forge-admin-ui/src/views/flow/model.vue` — 点击设计打开全屏 `n-modal` 工作区。
  - `forge-admin-ui/src/views/flow/design.vue` — 增加 `embedded/modelId` 入参和 `close/saved/deployed` 事件，保留路由兼容；顶部流程配置改为常驻三栏工作区，直接展示表单配置。
  - `forge-admin-ui/src/views/flow/todo.vue` — 抽屉改为全屏弹窗或详情内容区。
  - `forge-admin-ui/src/views/flow/done.vue`, `started.vue`, `cc.vue` — 详情展示统一改造。
- **关键签名**：
  ```vue
  <FlowDesignWorkspace :model-id="currentModelId" @saved="loadData" />
  <FlowTaskDetailModal v-model:show="showTaskDetail" :task="currentTask" />
  ```

## Task 10.5: 流程表单设计器业务组件对齐

- **执行状态**：完成。
- **目标**：流程表单设计器与低代码表单设计器共享业务组件能力，避免流程表单缺少组织、人员、系统字典等组件；同时补齐动态表单字段默认行间距和表单级统一配置能力。
- **涉及文件**：
  - `forge-admin-ui/src/components/form-create/FlowFormCreateDesigner.vue` — 注册低代码业务组件分组，支持字典选择、行政区划、组织选择、人员选择、文件/图片上传、引用对象等组件；右侧“表单配置”支持统一设置字段行间距。
  - `forge-admin-ui/src/components/form-create/FlowFormCreateRenderer.vue` — 运行渲染时复用业务组件选项补齐逻辑，加载系统字典、组织树、人员列表等数据。
  - `forge-admin-ui/src/components/form-create/formCreateBridge.js` — 统一为流程动态表单字段补默认 `wrap.style.marginBottom=20px`。
  - `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js` — 流程设计器复用组件注册但保留流程字段自由编辑，字段属性面板不注入行间距配置。
- **关键签名**：
  ```js
  installForgeBusinessComponents(designerRef.value, resolveCurrentFields(), {
    installBaseRules: false,
  })
  hydrateForgeBusinessPreviewRules(rules)
  ```

## Task 11: 验证与文档归档

- **执行状态**：完成。
- **目标**：执行增量验证，补充测试记录，准备 Review。
- **涉及文件**：
  - `code-copilot/changes/flow-management-form-runtime-optimization/test-spec.md`
  - `code-copilot/changes/flow-management-form-runtime-optimization/execution-log.md`
  - `code-copilot/changes/flow-management-form-runtime-optimization/spec.md`
  - `code-copilot/changes/flow-management-form-runtime-optimization/tasks.md`
- **关键命令**：
  ```bash
  cd forge && mvn -pl forge-flow/forge-flow-server -am compile -DskipTests
  cd forge-admin-ui && pnpm build
  ```
