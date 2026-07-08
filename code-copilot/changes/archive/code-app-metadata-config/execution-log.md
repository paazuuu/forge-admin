# 执行日志

## 2026-06-30

- 创建 SDD 变更目录 `code-app-metadata-config`。
- 创建 `spec.md`、`tasks.md`、`test-spec.md` 和本执行日志。

### 增量实现与验证

- 变更范围：
  - 后端：`BusinessFlowAppConfigService`、`BusinessFlowService`。
  - 前端：`BusinessFlowAppConfigPanel.vue`、`BusinessCodeAppMetadataPanel.vue`、`useCodeAppMetadata.js`、`purchase-order-test.vue`。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue src/views/app-center/components/designer/BusinessCodeAppMetadataPanel.vue src/views/business/purchase-order-test.vue src/composables/useCodeAppMetadata.js`
    - 结果：通过。
  - `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动前后端服务做浏览器手工验证；本轮先完成代码路径和静态/编译验证，交互验收需本地服务和测试数据配合。
- 服务清理：
  - 本轮未启动常驻服务。

### 审批表单加载误查流程模型和动态表单修正

- 执行时间：2026-06-30 15:12 CST
- 问题定位：
  - 用户 trace 显示审批单加载时执行 `sys_flow_model WHERE model_key = d98f5733-743e-11f1-9216-bec23233f0f6` 并返回 0；该参数是 Flowable 的 UUID 型流程定义 ID，不是业务流程模型 Key。
  - 同一 trace 显示 `sys_flow_form WHERE form_key = sample_purchase_order_approval_form` 返回 0；采购审批使用的是业务代码表单资产，不是动态表单资产，不应该走 `sys_flow_form` 查询。
  - 业务表单上下文加载时，`taskId` 表单信息已可返回完整配置，但 generator 侧仍继续调用流程实例表单信息接口，导致同请求内再次兜底查询流程模型/表单。
- 变更范围：
  - 后端：`FlowTaskServiceImpl`、`BusinessFlowService`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - `FlowTaskServiceImpl` 新增流程定义 Key 解析：优先从 Flowable `ProcessDefinition.getKey()` 读取，必要时从 BPMN process id 兜底，不再把 UUID 型 `processDefinitionId` 当 `sys_flow_model.model_key`。
  - 节点表单配置读取兼容 `formMode/formType/type`；当节点 `formKey` 对应流程全局业务表单，或 `formMode` 为 `BUSINESS_CODE_FORM/BUSINESS_OBJECT_FORM` 时，返回 `formType=business`，不再调用 `resolveFormJson` 查询 `sys_flow_form`。
  - `BusinessFlowService#loadFlowNodeFormInfo` 在 `taskId` 返回完整表单信息时直接复用，避免继续调用流程实例表单信息接口造成重复查询。
- 验证命令：
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check -- forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动后端、flow 服务和前端 dev server 做真实接口日志复核；本轮已完成后端相关模块编译。实际查询日志需要重启服务后重新打开采购审批待办确认 `model_key` 不再出现 UUID，`sample_purchase_order_approval_form` 不再走 `sys_flow_form` 查询。
- 服务清理：
  - 本轮未启动常驻服务。

### 待办节点展示与业务上下文查询优化

- 执行时间：2026-06-30 14:48 CST
- 问题定位：
  - 待办、已办、我发起页面直接展示 `taskName`，当后端返回 `部门负责人审批 dept_leader_approve` 时会把技术编码暴露给业务用户。
  - `BusinessFlowService#enrichBusinessListDisplay` 在列表循环中每条任务调用 `resolveCanonicalObjectCode`，同一业务对象会重复查询 `ai_business_document_config`。
  - `resolveTaskFormRuntimeContext` 先解析 canonical object，再重新查询运行配置和单据配置，审批表单加载链路存在重复解析。
- 变更范围：
  - 前端：`processDisplay.js`、`todo.vue`、`done.vue`、`started.vue`。
  - 后端：`BusinessFlowService`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - 新增 `getTaskDisplayName`，按 `taskDefKey/taskDefinitionKey/activityId/activityKey/nodeKey` 去掉节点名尾部技术编码；纯编码节点名使用业务兜底文案。
  - 待办、已办、我发起页面的列表元信息、抽屉副标题、基础信息和待办批量处理错误提示统一使用可读节点名。
  - 列表增强新增请求内 `BusinessRuntimeContext` 缓存，同一业务对象只解析一次运行配置和单据配置，再按 canonical object 分组处理。
  - 审批表单上下文加载复用同一个业务运行上下文，避免 canonical object、`AiCrudConfig` 和 `AiBusinessDocumentConfig` 重复查询。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/flow/utils/processDisplay.js src/views/flow/todo.vue src/views/flow/done.vue src/views/flow/started.vue`
    - 首次结果：失败，`processDisplay.js` 通用清理正则触发 `regexp/no-super-linear-backtracking`、`regexp/no-useless-escape`、`regexp/prefer-w`、`regexp/use-ignore-case`。
    - 修复后复跑结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
    - 结果：通过，`✓ built in 2m 32s`。
    - 警告：既有 `UserSelectModal` 命名冲突、动态/静态混合 import chunk 提示、CSS `//` 注释 minify 提示。
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check -- forge-admin-ui/src/views/flow/utils/processDisplay.js forge-admin-ui/src/views/flow/todo.vue forge-admin-ui/src/views/flow/done.vue forge-admin-ui/src/views/flow/started.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动后端、flow 服务和前端 dev server 做真实待办接口计数验证；本轮已覆盖静态检查、前端生产构建和后端编译。实际查询次数可在本地服务重启后通过待办列表日志确认同一业务对象不再按任务条数重复查询。
- 服务清理：
  - 本轮未启动常驻服务。

### 字段权限默认可见语义修正

- 问题定位：
  - 节点字段权限的预期语义是“默认全部可见，显式配置可写/必填/隐藏”。上一轮把显式权限列表错误当成可见字段白名单，导致工程部经理审批节点只显示 `engineeringManagerRemark`，采购单基础信息都被隐藏。
- 变更范围：
  - 后端：`BusinessFlowService#applyBusinessCodeFieldPermissions`、`BusinessFlowService#buildTaskFormFields`。
  - 前端：`useBusinessTaskFormContext.canShowField`。
  - 文档：`spec.md`、`tasks.md`、`test-spec.md`。
- 修正结果：
  - 未出现在节点权限列表中的字段默认可见但只读。
  - 显式 `readable=false` 的字段才隐藏。
  - 显式 `writable=true` 的字段才可编辑。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/composables/useBusinessTaskFormContext.js src/views/business/purchase-order-test.vue`
    - 结果：通过。
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未重跑前端生产构建；本轮只改了权限判断语义，已执行目标 eslint 和后端编译。
- 服务清理：
  - 本轮未启动常驻服务。

### 协议修正

- 根据反馈，取消新写入 `views.LIST/DETAIL` 作为主协议。
- `BusinessCodeAppMetadataPanel.vue` 改为输出现有兼容结构：
  - `formDesignerSchema.schemaVersion=form-first-v2`
  - `formDesignerSchema.settings.formAssets`
  - `viewSchema.schemaVersion=view-schema-v1`
  - `viewSchema.list.columns`
  - `viewSchema.detail.sections`
- `useCodeAppMetadata.js` 改为优先读取 `viewSchema`，旧 `views` 仅保留历史兜底。
- 复跑命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/designer/BusinessCodeAppMetadataPanel.vue src/composables/useCodeAppMetadata.js src/views/business/purchase-order-test.vue src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue`
  - 结果：通过。

### 字段与视图页面可见改造

- 修正点：上一轮只改了底层协议，页面视觉仍像原字段勾选面板，用户侧感知不到“兼容表单设计/列表设计”。
- 已改 `BusinessCodeAppMetadataPanel.vue`：
  - 标题改为“代码应用设计兼容配置”。
  - 顶部新增协议提示区：`formDesignerSchema.settings.formAssets`、`viewSchema.list.columns`、`viewSchema.detail.sections`。
  - 字段区改为“表单资产字段”，明确写入 `formDesignerSchema`。
  - 列表/详情区明确标注 `viewSchema.list.columns` 和 `viewSchema.detail.sections`。
- 复跑命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/designer/BusinessCodeAppMetadataPanel.vue`
  - 结果：通过。

### 复用既有设计器修正

- 需求修正：
  - 取消“业务流程配置”里的“字段与视图”自定义面板。
  - 代码应用改为复用对象设计器左侧已有“表单设计”“列表设计”“详情设置”组件。
- 变更范围：
  - 前端：`object-designer.[objectCode].vue`、`BusinessFlowAppConfigPanel.vue`、`BusinessDetailDesigner.vue`。
  - 后端：`BusinessFlowAppConfigService` 提示文案与 metadata 随流程绑定保存路径。
  - 文档：`spec.md`、`tasks.md`、`test-spec.md`、`code-copilot/memory/decisions.md`。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint 'src/views/app-center/object-designer.[objectCode].vue' src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue src/views/app-center/components/designer/BusinessDetailDesigner.vue src/views/business/purchase-order-test.vue src/composables/useCodeAppMetadata.js`
    - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue`
    - 结果：通过。用于覆盖最终恢复代码应用流程绑定必选校验的小修。
  - `cd forge-admin-ui && source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
    - 结果：通过，最终复跑输出 `✓ built in 3m 4s`。
    - 警告：既有 `UserSelectModal` 组件命名冲突提示；既有动态/静态混合 import chunk 提示；既有 CSS `//` 注释 minify 警告。
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动前后端服务做浏览器手工验收；本轮已完成 lint、生产构建、后端编译和 diff 检查，交互验收需本地服务与采购审批测试数据配合。
- 服务清理：
  - 本轮未启动常驻服务。

### 流程节点字段权限缺陷修复

- 问题定位：
  - `SamplePurchaseOrderServiceImpl#ensureFlowModel` 在发起采购单前发现现有 BPMN XML 与 `SamplePurchaseOrderFlowBpmn.build()` 不一致时会 `updateModel` 并重新发布，覆盖流程设计器节点抽屉保存的字段权限。
  - `BusinessFlowService#resolveFlowNodeForm` 只按 `taskId` 读取任务表单信息，读不到时退回旧 `bindingConfig.nodeForms`，无法稳定读取 BPMN 主数据源中的 `formFieldPermissions`。
  - `purchase-order-test.vue` 只传 `taskId` 时会先解析业务单 ID，解析不到直接返回，业务表单上下文和字段权限都不会加载；顶部“采购单信息”只按详情视图过滤，没有叠加节点字段权限。
- 变更范围：
  - 后端业务：`SamplePurchaseOrderServiceImpl`。
  - 后端流程：`FlowClient`、`FlowTaskService`、`FlowTaskServiceImpl`、`FlowTaskController`、`FlowCcController`。
  - 后端业务表单运行态：`BusinessFlowService`。
  - 前端：`useBusinessTaskFormContext.js`、`purchase-order-test.vue`。
  - 文档/记忆：`spec.md`、`tasks.md`、`test-spec.md`、`code-copilot/memory/pitfalls.md`。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/composables/useBusinessTaskFormContext.js src/views/business/purchase-order-test.vue src/composables/useCodeAppMetadata.js`
    - 结果：通过。
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core,forge-flow/forge-flow-client,forge-flow/forge-flow-server,forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`。
    - 警告：既有 Lombok `@Builder` 默认值警告；既有 deprecated/unchecked 编译提示。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
    - 结果：通过，`✓ built in 2m 13s`。
    - 警告：既有 `UserSelectModal` 命名冲突、动态/静态混合 import chunk 提示、CSS `//` 注释 minify 提示。
  - `git diff --check`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动后端、flow 服务和前端 dev server 做浏览器手工验收；本轮已完成静态、生产构建和后端编译验证，实际流程权限保留需本地数据库中已有采购流程模型和测试账号配合。
- 服务清理：
  - 本轮未启动常驻服务。

### 代码应用字段基准与表单显隐兼容

- 问题定位：
  - 代码应用加载设计器时优先使用已保存的 `metadata.fields`，导致 Provider/业务表后续新增字段不会进入“表单设计 / 列表设计 / 详情设置”。
  - 旧的空 `pageSchema/viewSchema/formDesignerSchema` 会让现有设计器没有内容，看起来仍然只能在代码里维护。
  - 表单设计器隐藏或删除字段后，没有同步到 `fields[].formVisible`，采购新增/编辑表单和待办自定义 Vue 表单不会消费应用管理里的表单显隐配置。
- 变更范围：
  - 后端：`BusinessFlowService`。
  - 前端设计器：`object-designer.[objectCode].vue`、`BusinessDetailDesigner.vue`、`formDesignerSchema.js`。
  - 前端运行态：`useCodeAppMetadata.js`、`purchase-order-test.vue`。
  - 文档：`spec.md`、`tasks.md`、`test-spec.md`。
- 修正结果：
  - 代码应用字段以 Provider 当前字段为基准，`codeAppMetadata` 只叠加标签、显隐、排序等用户配置。
  - 代码应用默认表单 schema 支持包含只读字段，并自动补齐旧空配置，进入设计器后表单/列表/详情都有内容。
  - 表单设计保存时把字段组件显隐同步为 `formVisible`，后端待办上下文和采购自定义表单都会按 FORM 配置裁剪字段。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint 'src/views/app-center/object-designer.[objectCode].vue' src/views/app-center/components/designer/BusinessDetailDesigner.vue src/views/app-center/components/designer/form-first/formDesignerSchema.js src/composables/useCodeAppMetadata.js src/views/business/purchase-order-test.vue`
    - 结果：通过。
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
    - 结果：通过，`✓ built in 3m 29s`。
    - 警告：既有 `UserSelectModal` 命名冲突、动态/静态混合 import chunk 提示、CSS `//` 注释 minify 提示。
  - `git diff --check`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动后端、flow 服务和前端 dev server 做浏览器手工验收；本轮已完成静态、生产构建和后端编译验证，实际界面验收需要本地服务、采购审批流程绑定和测试数据配合。
- 服务清理：
  - 本轮未启动常驻服务。

### 业务表单资产与流程全局表单接入

- 执行时间：2026-06-30 12:04
- 变更范围：
  - 前端：`BusinessFlowAppConfigPanel.vue`、`BusinessCodeAppFormAssetPanel.vue`、`flow/design.vue`、`ApproverConfig.vue`。
  - 后端：`BusinessFlowService`、`FlowTaskServiceImpl`、`SamplePurchaseOrderFlowDefinition`。
  - SDD：`spec.md`、`tasks.md`、`execution-log.md`。
- 修正结果：
  - 代码应用“业务流程配置”增加“业务表单资产”步骤，资产配置保存到 `options.codeAppMetadata.formAssets`，不覆盖表单/列表/详情设计器输出。
  - 流程设计器在业务对象上下文下，全局“表单配置”改为选择应用表单资产；独立流程仍保留动态表单/外置表单/无表单。
  - 业务全局表单引用保存为 `FlowModel.formType=business` + `formJson` 引用 JSON；节点未配置表单时字段权限目录继承全局业务表单资产。
  - 待办运行时解析表单资产时叠加 `codeAppMetadata.formAssets`，配置化 `formKey/formUrl/providerKey/formMode` 不再被 Provider 原始资产覆盖。
  - 采购审批新建示例流程模型默认写入全局业务表单引用；已有 BPMN XML 非空的模型仍保留用户在流程设计器中的节点配置。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue 'src/views/app-center/object-designer.[objectCode].vue' src/views/flow/design.vue src/components/flow-designer/panel/ApproverConfig.vue`
    - 首次结果：失败，`flow/design.vue` 存在 `no-use-before-define`；`ApproverConfig.vue` 存在导入排序和二元操作符缩进问题。
    - 修复后复跑结果：通过。
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-business/forge-business-core -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-framework/forge-plugin-parent/forge-plugin-generator,forge-business/forge-business-core -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`，覆盖本轮新增修改的 `forge-plugin-flow`。
    - 警告：同上，为既有 deprecated/unchecked 编译提示。
  - `git diff --check`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动后端、flow 服务和前端 dev server 做浏览器手工验收；本轮已完成静态、后端编译和 diff 检查，实际界面验收需要本地数据库、流程模型和采购审批测试账号配合。
- 服务清理：
  - 本轮未启动常驻服务。

### 资产提示与设计器空白修正

- 执行时间：2026-06-30 13:08 CST
- 问题定位：
  - “业务表单资产”面板把 `appName/objectName/businessName` 都暴露为可编辑项，业务用户会误以为需要维护三份业务名称。
  - Provider 实际来自后端注册的 `BusinessCodeFormProvider`，旧 UI 允许自由输入，缺少来源说明。
  - 代码应用加载设计器时，旧空 `pageSchema/formDesignerSchema` 可能被当成可用 schema，导致左侧“表单设计 / 列表设计 / 详情设置”没有字段内容。
- 变更范围：
  - 前端：`BusinessCodeAppFormAssetPanel.vue`、`object-designer.[objectCode].vue`。
  - SDD：`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - 资产面板隐藏重复名称编辑项，改为显示只读业务对象摘要；保留表单名称、表单 Key、Provider、Provider 名称、表单地址和说明。
  - Provider 改为选择已注册项，并用提示说明其来自后端 `BusinessCodeFormProvider` Spring Bean。
  - 代码应用资产合并改为应用配置优先覆盖 `formKey/formUrl/providerKey/formMode` 等引用信息，同时继续以 Provider 当前字段为字段基准。
  - 表单设计主画布为空但字段仍可见时，进入设计器会按当前 Provider 字段补齐主表单 schema；列表/详情 page schema 也会在缺少有效字段引用时回落到默认 schema。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/designer/BusinessCodeAppFormAssetPanel.vue 'src/views/app-center/object-designer.[objectCode].vue'`
    - 首次结果：失败，`BusinessCodeAppFormAssetPanel.vue` 的自定义事件名需要使用 camelCase。
    - 修复后复跑结果：通过。
  - `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessCodeAppFormAssetPanel.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue'`
    - 结果：通过，无空白错误。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
    - 结果：通过，`✓ built in 1m 49s`。
    - 警告：既有 `UserSelectModal` 命名冲突、动态/静态混合 import chunk 提示、CSS `//` 注释 minify 提示。
- 跳过项：
  - 未启动后端、flow 服务和前端 dev server 做浏览器手工验收；本轮已完成静态检查和生产构建，实际界面回显需要本地采购审批流程绑定和测试数据配合。
- 服务清理：
  - 本轮未启动常驻服务。

### Provider 目录与字段兜底协议修正

- 执行时间：2026-06-30 13:25 CST
- 问题定位：
  - Provider 下拉只从当前表单资产和草稿里拼接，不能覆盖所有已注册 `BusinessCodeFormProvider` Bean。
  - 表单 Key 是流程运行时引用，不应作为业务用户可编辑配置。
  - 采购审批虽然有 `ai_business_object` 占位对象，但没有低代码模型字段；左侧设计器必须直接消费 Provider 字段目录，不能要求把后台业务表再导入成低代码对象。
- 变更范围：
  - 后端：`BusinessCodeFormProviderRegistry`、`BusinessFlowService`、`BusinessFlowAppConfigService`。
  - 前端：`BusinessCodeAppFormAssetPanel.vue`、`BusinessFlowAppConfigPanel.vue`、`object-designer.[objectCode].vue`。
  - SDD：`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - 后端 `getFormAssets` 返回 `providerCatalog`，目录来自所有 Spring Bean 形式注册的 `BusinessCodeFormProvider`，并携带每个 Provider 的默认资产字段。
  - 已存在 `ai_business_object` 的代码应用也会在配置响应中补齐 `options.codeApp/codeAppMetadata`，避免前端按普通低代码对象理解。
  - 业务表单资产配置顺序改为先选择 Provider；表单 Key 只在上下文中作为“系统表单 Key”展示，不再提供输入框。
  - 代码应用设计器字段基准优先取当前 Provider 资产字段；当前资产为空时，从 `providerCatalog[].assets[].fields` 兜底生成 `modelSchema/pageSchema/formDesignerSchema/viewSchema`。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/designer/BusinessCodeAppFormAssetPanel.vue src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue 'src/views/app-center/object-designer.[objectCode].vue'`
    - 结果：通过。
  - `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessCodeAppFormAssetPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessCodeFormProviderRegistry.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowAppConfigService.java`
    - 结果：通过，无空白错误。
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
    - 结果：通过，`✓ built in 1m 33s`。
    - 警告：既有 `UserSelectModal` 命名冲突、动态/静态混合 import chunk 提示、CSS `//` 注释 minify 提示。
- 跳过项：
  - 未启动后端和前端 dev server 做浏览器手工验收；本轮已覆盖前端 eslint、生产构建、后端编译和 diff 检查。实际页面需要重启后端以获得新的 `providerCatalog` 响应。
- 服务清理：
  - 本轮未启动常驻服务。

### 已有代码应用对象入口修正

- 执行时间：2026-06-30 14:23 CST
- 问题定位：
  - 采购审批已经有 `ai_business_object` 占位对象，嵌入式打开对象设计器时传入了 `objectId`，但 URL 没有 `codeApp=1`。
  - `object-designer.[objectCode].vue` 解析到对象后直接调用普通 `businessObjectDesigner(object.id)`，由于采购审批对象没有低代码模型字段，左侧“表单设计 / 列表设计 / 详情设置”加载到空 schema。
  - 后台 `BusinessFlowAppConfigService#saveConfig` 在已有对象分支没有处理 `options.codeAppMetadata`，导致代码应用表单设计/列表设计保存后无法写入流程绑定 options。
- 变更范围：
  - 前端：`object-designer.[objectCode].vue`。
  - 后端：`BusinessFlowAppConfigService`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - 嵌入式设计器现在会按 `objectCode` 拉完整业务对象，识别 `options/designerOptions.codeApp=true` 后强制进入代码应用虚拟设计器路径。
  - 已有代码应用对象不再走普通低代码 `businessObjectDesigner(object.id)`，而是用 `businessFlowAppConfig` 返回的 Provider/formAssets/providerCatalog 字段生成 `modelSchema/pageSchema/formDesignerSchema/viewSchema`。
  - 已有业务对象的代码应用保存 `options.codeAppMetadata` 时也会写入流程绑定 options；带流程绑定保存时同步合并 metadata 并归一化为代码适配器绑定。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint 'src/views/app-center/object-designer.[objectCode].vue'`
    - 结果：通过。
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
    - 结果：通过，`✓ built in 3m 2s`。
    - 警告：既有 `UserSelectModal` 命名冲突、动态/静态混合 import chunk 提示、CSS `//` 注释 minify 提示。
  - `git diff --check -- 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowAppConfigService.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动后端和前端 dev server 做浏览器手工验收；本轮已完成静态检查、前端生产构建和后端编译。实际界面回显需要本地服务重启并使用采购审批测试数据验证。
- 服务清理：
  - 本轮未启动常驻服务。

### 流程定义匹配兼容修正

- 执行时间：2026-06-30 15:24 CST
- 问题定位：
  - 待办业务表单身份校验中，`BusinessFlowService#validateTaskAccess` 仍把请求里的 `processDefKey` 和任务详情里的 `processDefKey` 做直接字符串比较。
  - 前端、`sys_flow_task` 和 Flowable 运行时可能分别携带业务流程模型 Key、`key:version:id` 或历史 UUID 型流程定义标识，导致同一流程被误判为“流程定义与当前任务不匹配”。
- 变更范围：
  - 后端流程：`FlowTaskServiceImpl#getTaskDetail`。
  - 后端业务表单运行态：`BusinessFlowService#validateTaskAccess`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
  - 记忆：`code-copilot/memory/pitfalls.md`。
- 修正结果：
  - 任务详情返回前将 `processDefKey` 归一化为业务流程模型 Key，优先从 Flowable `ProcessDefinition.getKey()` / BPMN process id 解析。
  - 业务表单任务身份校验改为兼容 `key`、`key:version:id` 和历史 UUID 型流程定义表示，不再因为表示差异误拦截。
  - 任务 ID、办理人、流程实例、业务 Key 和任务节点校验保持不变。
- 验证命令：
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`，总耗时 13.431 s。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check -- forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md code-copilot/memory/pitfalls.md`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动后端、flow 服务和前端 dev server 做浏览器手工验收；本轮问题在后端任务身份校验和流程定义表示兼容，已完成目标模块编译与 diff 检查。真实待办页面需重启服务后用现有采购审批待办任务复验。
- 服务清理：
  - 本轮未启动常驻服务。

### 待办审批详情表单首屏优化

- 执行时间：2026-06-30 16:05 CST
- 问题定位：
  - 待办抽屉打开时，`todo.vue` 先请求 `/api/flow/task/form/{taskId}`，返回后再串行请求 `/ai/business/flow/task-form-context`。
  - 父级已经拿到 `businessFormContext` 后，`FlowBusinessForm` 没有把它传给采购审批组件；`purchase-order-test.vue` 挂载后又重复请求业务表单上下文、代码应用配置和采购单详情。
  - 这条链路会把多个网络请求串起来，导致采购审批表单首屏明显变慢。
- 变更范围：
  - 前端：`todo.vue`、`FlowBusinessForm.vue`、`purchase-order-test.vue`、`useBusinessTaskFormContext.js`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
  - 记忆：`code-copilot/memory/pitfalls.md`。
- 修正结果：
  - 审批抽屉中流程表单信息和业务表单上下文改为并行加载。
  - `FlowBusinessForm` 新增 `initialTaskContext` 透传协议。
  - 采购审批表单待办模式优先复用父级上下文和 `recordData`，不再重复请求业务上下文、代码应用配置和采购单详情。
  - `useBusinessTaskFormContext` 支持注入已有上下文，并基于上下文字段目录判断显示/编辑。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/components/common/FlowBusinessForm.vue src/views/flow/todo.vue src/views/business/purchase-order-test.vue src/composables/useBusinessTaskFormContext.js`
    - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
    - 结果：通过，`✓ built in 5m 55s`。
    - 警告：既有 `UserSelectModal` 命名冲突、动态/静态混合 import chunk 提示、CSS `//` 注释 minify 提示。
  - `git diff --check -- forge-admin-ui/src/components/common/FlowBusinessForm.vue forge-admin-ui/src/views/flow/todo.vue forge-admin-ui/src/views/business/purchase-order-test.vue forge-admin-ui/src/composables/useBusinessTaskFormContext.js code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md code-copilot/memory/pitfalls.md`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动前后端 dev 服务做浏览器手工计时；本轮已完成静态检查和生产构建。实际耗时需重启前端后在浏览器 Network 面板确认详情表单不再重复请求业务上下文、代码应用配置和采购单详情。
- 服务清理：
  - 本轮未启动常驻服务。

### 流程定义校验复发修正

- 执行时间：2026-06-30 16:35 CST
- 问题定位：
  - 待办详情表单首屏优化后，`todo.vue` 并行加载 `/api/flow/task/form/{taskId}` 和 `/ai/business/flow/task-form-context`。
  - 首个业务上下文请求仍可能从待办列表行携带 `processDefKey`，而列表行里的流程定义值不一定已经按 `FlowTaskServiceImpl#getTaskDetail` 归一化。
  - 业务表单任务身份校验把这个旧值和任务详情值比较时，仍会误报“流程定义与当前任务不匹配”。
- 变更范围：
  - 前端：`todo.vue`。
  - 后端：`BusinessFlowService`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
  - 记忆：`code-copilot/memory/pitfalls.md`。
- 修正结果：
  - 待办详情首个业务表单上下文请求改为只传 `taskId`，由后端任务详情补齐流程实例、业务 Key、任务节点和流程定义。
  - `buildBusinessTaskFormQuery` 不再从待办列表行回退读取 `processDefKey`，只接受已加载表单信息中的归一化值。
  - `BusinessFlowService#assertProcessDefinitionMatches` 对流程定义表示差异只记录 debug 日志，不再硬抛错；任务 ID、办理人、流程实例、业务 Key 和任务节点校验保持不变。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/flow/todo.vue`
    - 结果：通过。
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`，总耗时 23.367 s。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check -- forge-admin-ui/src/views/flow/todo.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md code-copilot/memory/pitfalls.md`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动后端、flow 服务和前端 dev server 做浏览器手工验收；本轮已覆盖前端目标文件 eslint 和后端目标模块编译。真实待办页面需重启服务后用现有采购审批待办任务复验。
- 服务清理：
  - 本轮未启动常驻服务。

### 审批详情表单重复请求与后端重复解析优化

- 执行时间：2026-06-30 16:06 CST
- 问题定位：
  - 日志显示审批详情打开时，前端 `/api/flow/task/form/{taskId}` 请求和业务上下文后台 FlowClient 请求同一接口并存，导致同一个任务表单信息加载两次。
  - 单次 `FlowTaskServiceImpl#getTaskFormInfo` 内部先查流程模型和 BPMN 节点解析表单配置，随后 `getTaskApprovalPolicy` 又重复查询流程模型和解析 BPMN。
  - 采购审批业务代码表单不需要流程表单实例快照，但旧逻辑仍查询 `sys_flow_form_instance`。
- 变更范围：
  - 前端：`todo.vue`。
  - 后端流程：`FlowTaskServiceImpl`。
  - 后端业务上下文：`BusinessFlowService`、`BusinessTaskFormContextVO`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - 待办详情先加载 `/ai/business/flow/task-form-context`；如果返回业务对象表单或业务代码表单，前端不再额外请求 `/api/flow/task/form/{taskId}`。
  - 业务表单上下文新增返回节点审批策略，前端按钮、签名和审批意见要求可继续按节点策略展示。
  - `getTaskFormInfo` 内复用同一次解析出的 `FlowModel` 和 `FlowNode`，审批策略不再重复查模型和 BPMN。
  - 业务表单场景跳过 `sys_flow_form_instance` 查询，只有动态表单需要快照时才查询。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/flow/todo.vue`
    - 结果：通过。
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`，总耗时 20.767 s。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check -- forge-admin-ui/src/views/flow/todo.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessTaskFormContextVO.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动前后端服务做浏览器 Network 复验；本轮已完成目标前端 eslint 和后端目标模块编译。实际复验时应确认采购审批详情只剩一次 `/api/flow/task/form/{taskId}`，且来自 `/ai/business/flow/task-form-context` 后台链路。
- 服务清理：
  - 本轮未启动常驻服务。

### 采购审批业务表单节点标签去编码

- 执行时间：2026-06-30 16:18 CST
- 问题定位：
  - 截图中红框位置来自采购审批自定义业务表单 `purchase-order-test.vue` 头部标签。
  - 该位置直接渲染 `currentTaskDefKey`，所以公共流程列表和抽屉节点展示修正后，这里仍会显示 `dept_leader_approve`。
- 变更范围：
  - 前端：`purchase-order-test.vue`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - 业务表单头部标签改为 `currentNodeDisplayName`，通过 `getTaskDisplayName` 清理节点名尾部技术编码。
  - 无后端节点名时，继续使用采购审批节点中文映射作为兜底；业务判断仍使用原始 `taskDefKey`，不影响字段展示和校验。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/business/purchase-order-test.vue`
    - 结果：通过。
  - `git diff --check -- forge-admin-ui/src/views/business/purchase-order-test.vue code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动前端 dev server 做截图复验；本轮为单点展示文案修正，已完成目标文件 eslint。刷新页面后该标签应显示“部门负责人审批”而不是 `dept_leader_approve`。
- 服务清理：
  - 本轮未启动常驻服务。

### 应用中心业务单元编辑

- 执行时间：2026-06-30 16:28 CST
- 问题定位：
  - 应用总览、业务域详情和业务单元详情只有新建、设计、启停、删除入口，没有编辑业务单元基础配置的入口。
  - 后端业务对象更新接口已支持 `suiteCode`，但如果只更新 `ai_business_object.suite_code`，访问入口、设计版本、触发器和单据配置仍留在旧业务域，应用中心展示会割裂。
- 变更范围：
  - 前端：`BusinessObjectEditorDrawer.vue`、`BusinessUnitCard.vue`、`ObjectCard.vue`、`index.vue`、`suite.[suiteCode].vue`、`object.[objectCode].vue`。
  - 后端：`BusinessObjectService`、`BusinessObjectMapper.java`、`BusinessObjectMapper.xml`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - 新增业务单元编辑抽屉，可维护所属业务域、名称、对象类型、显示字段、图标、排序、启用状态和说明；对象编码只读。
  - 应用总览业务单元卡片、业务域详情对象卡片、业务单元详情页均可进入编辑。
  - 编辑保存前先拉业务单元详情，保存时保留 `modelId/modelCode/options` 等隐藏配置。
  - 跨业务域移动时，后端事务内同步更新访问入口、设计版本、触发器和单据配置的 `suite_code`；存在对象关系时阻止移动并提示先调整关系。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/BusinessUnitCard.vue src/views/app-center/components/ObjectCard.vue src/views/app-center/components/BusinessObjectEditorDrawer.vue src/views/app-center/index.vue 'src/views/app-center/suite.[suiteCode].vue' 'src/views/app-center/object.[objectCode].vue'`
    - 结果：通过。
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`，总耗时 15.662 s。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check -- forge-admin-ui/src/views/app-center/components/BusinessUnitCard.vue forge-admin-ui/src/views/app-center/components/ObjectCard.vue forge-admin-ui/src/views/app-center/components/BusinessObjectEditorDrawer.vue forge-admin-ui/src/views/app-center/index.vue 'forge-admin-ui/src/views/app-center/suite.[suiteCode].vue' 'forge-admin-ui/src/views/app-center/object.[objectCode].vue' forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessObjectMapper.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessObjectMapper.xml code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md`
    - 结果：通过，无空白错误。
  - `git diff --no-index --check /dev/null forge-admin-ui/src/views/app-center/components/BusinessObjectEditorDrawer.vue`
    - 结果：通过，无空白错误；该文件为本轮新增未跟踪文件，单独检查。
- 跳过项：
  - 未启动前后端 dev 服务做浏览器手工验收；本轮已完成目标前端 eslint、后端目标模块编译和 diff 检查。真实页面需重启服务后验证编辑抽屉保存、跨业务域移动和对象关系拦截提示。
- 服务清理：
  - 本轮未启动常驻服务。

### 业务单元显示字段配置收敛

- 执行时间：2026-06-30 16:40 CST
- 问题定位：
  - 新建业务单元时字段尚未设计完成，用户不知道应该填写哪个字段编码，“显示字段”放在创建流程里容易填错。
  - 关联关系设计器已有“运行态显示字段 / 目标对象回显字段”下拉配置，适合作为关联回显的主配置入口。
- 变更范围：
  - 前端：`BusinessObjectWizardDrawer.vue`、`BusinessObjectEditorDrawer.vue`、`object-designer.[objectCode].vue`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - 新建业务单元不再展示“显示字段”。
  - 编辑业务单元基础信息不再展示“显示字段”，但保存时保留已有对象级 `displayField`，避免清空历史配置。
  - 对象设计器基础信息里的字段改名为“默认标题字段”，说明它只是关联关系未单独配置时的兜底。
  - 关联关系里的“运行态显示字段 / 目标对象回显字段”继续作为运行态关联回显字段配置入口。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/BusinessObjectWizardDrawer.vue src/views/app-center/components/BusinessObjectEditorDrawer.vue 'src/views/app-center/object-designer.[objectCode].vue'`
    - 结果：通过。
  - `git diff --check -- forge-admin-ui/src/views/app-center/components/BusinessObjectWizardDrawer.vue forge-admin-ui/src/views/app-center/components/BusinessObjectEditorDrawer.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md`
    - 结果：通过，无空白错误。
  - `git diff --no-index --check /dev/null forge-admin-ui/src/views/app-center/components/BusinessObjectEditorDrawer.vue`
    - 结果：通过，无空白错误；该文件为本轮新增未跟踪文件，单独检查。
- 跳过项：
  - 未启动前端 dev server 做浏览器手工验收；本轮为字段入口收敛和文案调整，已完成目标文件 eslint 与 diff 检查。
- 服务清理：
  - 本轮未启动常驻服务。

### 表单资产移除、标题模板和停用确认

- 执行时间：2026-06-30 17:30 CST
- 问题定位：
  - 业务表单资产面板只有新增和恢复默认，没有移除入口；即使前端删除草稿，后端 `mergeCodeAppAssets` 仍会把 Provider 默认资产自动补回。
  - 采购审批示例 `SamplePurchaseOrderServiceImpl#submit` 直接硬编码 `采购单审批-采购单号`，绕过了应用管理中的流程标题模板。
  - 应用总览、业务域详情、业务单元详情的停用操作直接执行；业务域/业务单元编辑抽屉里把启用状态改为停用也没有二次确认。
- 变更范围：
  - 前端：`BusinessCodeAppFormAssetPanel.vue`、`index.vue`、`suite.[suiteCode].vue`、`object.[objectCode].vue`、`BusinessObjectEditorDrawer.vue`、`SuiteEditorDrawer.vue`。
  - 后端：`BusinessFlowService`、`SamplePurchaseOrderServiceImpl`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - 业务表单资产增加“移除”操作，保存 `codeAppMetadata.removedFormAssetKeys`；新增或切换 Provider 会清除对应移除标记。
  - 后端合并代码表单资产时过滤 `removedFormAssetKeys`，Provider 默认资产不会在刷新后自动恢复。
  - 采购审批发起流程读取业务对象 FLOW/APPROVAL 绑定的 `titleTemplate`，支持 `${field}` 和 `{field}`，未配置时保留原兜底标题。
  - 业务域/业务单元按钮停用、编辑抽屉停用均增加二次确认；删除业务域/业务单元保留既有确认。
- 验证命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/designer/BusinessCodeAppFormAssetPanel.vue src/views/app-center/index.vue 'src/views/app-center/suite.[suiteCode].vue' 'src/views/app-center/object.[objectCode].vue' src/views/app-center/components/BusinessObjectEditorDrawer.vue src/views/app-center/components/SuiteEditorDrawer.vue`
    - 结果：通过。
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core,forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`，总耗时 18.672 s。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessCodeAppFormAssetPanel.vue forge-admin-ui/src/views/app-center/index.vue 'forge-admin-ui/src/views/app-center/suite.[suiteCode].vue' 'forge-admin-ui/src/views/app-center/object.[objectCode].vue' forge-admin-ui/src/views/app-center/components/BusinessObjectEditorDrawer.vue forge-admin-ui/src/views/app-center/components/SuiteEditorDrawer.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/service/impl/SamplePurchaseOrderServiceImpl.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md`
    - 结果：通过，无空白错误。
  - `git diff --no-index --check /dev/null forge-admin-ui/src/views/app-center/components/designer/BusinessCodeAppFormAssetPanel.vue`
    - 结果：通过，无空白错误；该文件为新增未跟踪文件，单独检查。
  - `git diff --no-index --check /dev/null forge-admin-ui/src/views/app-center/components/BusinessObjectEditorDrawer.vue`
    - 结果：通过，无空白错误；该文件为新增未跟踪文件，单独检查。
- 跳过项：
  - 未启动前后端服务做浏览器手工验收；本轮已覆盖目标前端 eslint、后端目标模块编译和 diff 检查。真实页面需重启服务后验证资产移除保存回显、采购审批标题模板和停用/删除确认交互。
- 服务清理：
  - 本轮未启动常驻服务。

### 采购审批驳回状态修正

- 执行时间：2026-06-30 17:55 CST
- 问题定位：
  - 采购审批节点点击驳回后，`FlowTaskServiceImpl#reject` 只把 `approvalResult=reject`、`approved=false` 作为 `taskService.complete` 参数传入。
  - Flowable 任务完成事件触发时，业务回调读取任务/流程变量存在时序风险，采购单回调没有可靠识别驳回动作，状态停留在 `IN_PROCESS`。
- 变更范围：
  - 后端流程：`FlowTaskServiceImpl`。
  - 后端业务：`SamplePurchaseOrderServiceImpl`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - `completeTask` 在完成任务前先把审批动作变量写入流程实例，再调用 `taskService.complete`，保证任务完成监听器和后续流程事件能读取到本次动作变量。
  - 采购审批 `TASK_COMPLETED` 回调同时识别 `approvalResult=reject` 和 `approved=false`；普通审批节点驳回后状态流转为“待修改”，申请人修改节点驳回后状态流转为“已驳回”。
- 验证命令：
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-business/forge-business-core -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`，总耗时 12.297 s。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check -- forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/service/impl/SamplePurchaseOrderServiceImpl.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动后端、flow 服务和前端页面做完整采购审批手工流转；本轮已覆盖目标后端模块编译和 diff 检查。真实验收时需发起采购审批，在普通审批节点驳回后确认采购单变为“待修改”，申请人修改节点驳回后确认采购单变为“已驳回”。
- 服务清理：
  - 本轮未启动常驻服务。

### 采购审批申请人修改状态兜底

- 执行时间：2026-06-30 18:06 CST
- 问题定位：
  - 用户复验后，流程已经进入“申请人修改”节点，但采购单状态仍为 `IN_PROCESS`，重新提交前保存申请人修改字段时报“当前采购单不是待修改状态”。
  - 这说明只依赖上一个审批任务 `TASK_COMPLETED` 事件里的驳回变量仍不够稳，业务状态应以“申请人修改任务已创建”作为兜底事实；已经错过事件的存量待办也需要在保存前自愈。
- 变更范围：
  - 后端业务：`SamplePurchaseOrderServiceImpl`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - 采购审批回调新增监听 `TASK_CREATED`；当创建的任务是 `applicant_modify` 且采购单仍为 `IN_PROCESS` 时，立即同步为 `NEED_MODIFY`。
  - 申请人修改节点保存字段时，如果采购单仍为 `IN_PROCESS`，先在同一事务内修复为 `NEED_MODIFY`，再保存业务字段，兼容已经卡住的旧待办。
  - 已是 `APPROVED/REJECTED/CANCELED` 或非申请人修改节点不会被该兜底逻辑改回待修改。
- 验证命令：
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`，总耗时 13.010 s。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check -- forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/service/impl/SamplePurchaseOrderServiceImpl.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动后端、flow 服务和前端页面做完整采购审批手工流转；本轮已覆盖目标后端模块编译和 diff 检查。真实验收时需先重启服务，再用当前卡住的申请人修改待办直接重新提交，并新发一条采购审批验证驳回后状态立即变为“待修改”。
- 服务清理：
  - 本轮未启动常驻服务。

### 采购审批重新提交状态兜底

- 执行时间：2026-06-30 18:17 CST
- 问题定位：
  - 用户复验后，申请人修改节点可以重新提交，但采购单状态仍停留在 `NEED_MODIFY`。
  - 这说明状态机仍只覆盖了“进入申请人修改”的事实，没有覆盖“重新提交后进入下一轮审批节点”的事实；如果申请人修改 `TASK_COMPLETED` 回调漏掉，状态不会回到 `IN_PROCESS`。
- 变更范围：
  - 后端业务：`SamplePurchaseOrderServiceImpl`、`SamplePurchaseOrderFlowDefinition`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - `SamplePurchaseOrderFlowDefinition` 增加普通审批节点判断，统一识别部门负责人、工程部经理和采购会签。
  - `TASK_CREATED` 回调新增普通审批节点兜底：当新建任务是普通审批节点且采购单仍为 `NEED_MODIFY` 时，自动同步为 `IN_PROCESS`。
  - 审批节点字段保存前新增自愈：已经进入普通审批节点但业务状态仍为 `NEED_MODIFY` 的存量待办，先修复为 `IN_PROCESS` 再保存字段。
- 验证命令：
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`，总耗时 16.364 s。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check -- forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/service/impl/SamplePurchaseOrderServiceImpl.java forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/support/SamplePurchaseOrderFlowDefinition.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动后端、flow 服务和前端页面做完整采购审批手工流转；本轮已覆盖目标后端模块编译和 diff 检查。真实验收时需重启服务后验证“驳回 -> 申请人修改 -> 重新提交”链路，确认重新提交后状态回到“审批中”。
- 服务清理：
  - 本轮未启动常驻服务。

### 采购审批存量状态对账

- 执行时间：2026-06-30 18:21 CST
- 问题定位：
  - 对已经重新提交但错过事件的存量采购单，后续不会再次触发普通审批节点 `TASK_CREATED`，因此仅靠事件兜底无法让列表/详情立即恢复状态。
  - 需要在采购单查询侧按当前活跃待办节点做一次轻量对账，只修复 `IN_PROCESS/NEED_MODIFY` 两类流转中状态。
- 变更范围：
  - 后端业务：`SamplePurchaseOrderMapper`、`SamplePurchaseOrderMapper.xml`、`SamplePurchaseOrderServiceImpl`。
  - SDD：`spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- 修正结果：
  - 新增 XML 查询：按租户和业务 Key 批量读取 `sys_flow_task` 中状态为待办/已签收的当前任务节点。
  - 采购单 `page/detail/detailsByIds` 返回前对账：当前活跃节点是 `applicant_modify` 则状态应为 `NEED_MODIFY`；当前活跃节点是普通审批节点则状态应为 `IN_PROCESS`。
  - 对账只处理 `IN_PROCESS/NEED_MODIFY`，不修改草稿、已通过、已驳回、已取消等状态。
- 验证命令：
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`，总耗时 13.445 s。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check -- forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/mapper/SamplePurchaseOrderMapper.java forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/service/impl/SamplePurchaseOrderServiceImpl.java forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/support/SamplePurchaseOrderFlowDefinition.java forge-server/forge-business/forge-business-core/src/main/resources/mapper/business/SamplePurchaseOrderMapper.xml code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动后端、flow 服务和前端页面做完整采购审批手工流转；本轮已覆盖目标后端模块编译和 diff 检查。真实验收时需重启服务后刷新采购单列表或详情，确认已进入普通审批节点的存量记录从“待修改”恢复为“审批中”。
- 服务清理：
  - 本轮未启动常驻服务。

### 采购审批状态机最终复验

- 执行时间：2026-06-30 18:23 CST
- 复验范围：
  - 采购审批驳回进入申请人修改、申请人修改重新提交进入审批、以及存量待办状态对账三处状态兜底。
  - 本轮未新增业务逻辑，仅复跑目标模块编译和最终 diff 检查。
- 验证命令：
  - `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core -am compile -DskipTests`
    - 结果：通过，`BUILD SUCCESS`，总耗时 12.994 s。
    - 警告：`GenTableServiceImpl` 既有 deprecated API 警告；`BusinessObjectDesignerService` 既有 unchecked/unsafe operations 警告。
  - `git diff --check -- forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/mapper/SamplePurchaseOrderMapper.java forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/service/impl/SamplePurchaseOrderServiceImpl.java forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/support/SamplePurchaseOrderFlowDefinition.java forge-server/forge-business/forge-business-core/src/main/resources/mapper/business/SamplePurchaseOrderMapper.xml code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md code-copilot/memory/pitfalls.md`
    - 结果：通过，无空白错误。
- 跳过项：
  - 未启动后端、flow 服务和前端页面做完整采购审批手工流转；真实验收需重启服务后走“驳回 -> 申请人修改 -> 重新提交”，并刷新列表/详情确认存量状态自动对账。
- 服务清理：
  - 本轮未启动常驻服务。
