# 代码应用元数据配置化测试计划

## 验证范围

本轮验证覆盖后端配置保存/合并、代码应用复用现有表单/列表/详情设计器、采购审批列表/详情配置消费和待办字段权限兼容。

## 命令

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint 'src/views/app-center/object-designer.[objectCode].vue' src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue src/views/app-center/components/designer/BusinessDetailDesigner.vue src/views/business/purchase-order-test.vue src/composables/useCodeAppMetadata.js` | 通过 |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check` | 无空白错误 |

## 本轮增量验证 2026-06-30

本轮新增风险点：

- 采购示例发起流程不应覆盖流程设计器已保存的 BPMN 节点字段权限。
- 待办业务表单上下文需要在 `taskId` 缺失或不完整时，通过 `processInstanceId/businessKey/processDefKey/taskDefKey` 兜底读取 BPMN `formFieldPermissions`。
- 显式字段权限存在时，未配置字段默认可见但只读，显式 `readable=false` 的字段隐藏；采购审批页顶部详情字段也要遵守节点权限。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/composables/useBusinessTaskFormContext.js src/views/business/purchase-order-test.vue src/composables/useCodeAppMetadata.js` | 通过 |
| 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过 |
| 后端流程/业务编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core,forge-flow/forge-flow-client,forge-flow/forge-flow-server,forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check` | 无空白错误 |

## 手工验收

1. 打开 `/app-center/object/sample_purchase_order/designer?codeApp=1&name=采购申请`，确认左侧可见“表单设计”“列表设计”“业务流程配置”。
2. 打开“业务流程配置”，确认业务配置中心只显示“流程配置”，不显示“字段与视图”。
3. 在“列表设计”隐藏采购审批列表字段并保存，刷新采购审批页，确认列表列变化。
4. 在“表单设计 > 详情设置”隐藏详情字段并保存，打开采购单详情，确认详情字段变化。
5. 在流程设计器节点抽屉配置字段权限，进入待办审批，确认未授权可写字段可见但只读，显式不可见字段不展示。
6. 修改采购流程节点字段权限后发起一次采购单，再回到流程设计器，确认 BPMN 节点权限未被示例初始化重置。

## 本轮增量验证 2026-06-30 字段基准兼容

本轮新增风险点：

- `codeAppMetadata.fields` 不能覆盖 Provider 当前字段，必须支持业务代码新增字段自动进入设计器。
- 代码应用默认 `formDesignerSchema/pageSchema/viewSchema` 不能为空，旧空配置应自动补齐到既有“表单设计 / 列表设计 / 详情设置”组件。
- 表单设计的字段隐藏需要同步到 `fields[].formVisible`，并被采购新增/编辑/待办表单消费。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint 'src/views/app-center/object-designer.[objectCode].vue' src/views/app-center/components/designer/BusinessDetailDesigner.vue src/views/app-center/components/designer/form-first/formDesignerSchema.js src/composables/useCodeAppMetadata.js src/views/business/purchase-order-test.vue` | 通过 |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 |
| 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过 |
| Diff 检查 | `git diff --check` | 无空白错误 |

## 本轮增量验证 2026-06-30 业务表单资产与流程全局表单

本轮新增风险点：

- 代码应用业务表单资产配置不能覆盖表单设计、列表设计、详情设置已经保存的 `codeAppMetadata`。
- 流程设计器在业务对象上下文下不能继续把全局表单主入口引向动态表单设计器。
- 节点表单权限字段目录需要优先取节点选择的表单资产；节点未选择时应继承流程全局业务表单资产。
- 后端待办上下文解析需要支持 BPMN 流程级全局业务表单引用作为节点表单缺省值。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue 'src/views/app-center/object-designer.[objectCode].vue' src/views/flow/design.vue src/components/flow-designer/panel/ApproverConfig.vue` | 通过 |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-business/forge-business-core -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check` | 无空白错误 |

## 本轮手工验收补充

1. 进入采购审批代码应用设计器，打开“业务流程配置”，确认可以看到“业务表单资产”和“流程配置”两个配置项。
2. 修改采购审批表单资产的表单名称、URL、说明并保存，刷新后确认回显，且表单设计/列表设计/详情设置不丢失。
3. 从采购审批业务配置打开流程设计器，进入“更多设置 > 表单配置”，确认主入口是选择应用表单资产，不显示动态表单设计按钮。
4. 选择全局业务表单资产后保存流程草稿，再打开审批节点表单权限，确认字段目录来自所选业务表单资产。

## 本轮增量验证 2026-06-30 资产提示与设计器空白修正

本轮新增风险点：

- 业务表单资产面板不能继续暴露重复的“应用名称 / 业务对象名称 / 业务名称”编辑项，Provider 来源需要给业务用户明确提示。
- 保存过旧空配置的代码应用进入左侧“表单设计 / 列表设计 / 详情设置”时，不能因为空 `pageSchema/formDesignerSchema` 被判定可用而没有字段内容。
- 资产配置要优先使用应用管理保存的 `formKey/formUrl/providerKey`，同时仍以 Provider 当前字段作为字段基准。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/designer/BusinessCodeAppFormAssetPanel.vue 'src/views/app-center/object-designer.[objectCode].vue'` | 通过 |
| 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过 |
| Diff 检查 | `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessCodeAppFormAssetPanel.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue'` | 无空白错误 |

## 本轮增量验证 2026-06-30 Provider 目录与字段兜底协议

本轮新增风险点：

- Provider 下拉必须来自后端所有已注册 `BusinessCodeFormProvider` Bean，而不是从当前资产草稿里临时拼出来。
- 表单 Key 是系统引用，不能作为业务用户可编辑项。
- 采购审批这类代码应用即使有 `ai_business_object` 占位对象，也没有低代码模型；左侧“表单设计 / 列表设计 / 详情设置”必须从 Provider 字段目录或 `providerCatalog` 兜底生成字段。
- 既有代码应用配置不能因为新增 `providerCatalog` 而丢失 `codeAppMetadata`。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/designer/BusinessCodeAppFormAssetPanel.vue src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue 'src/views/app-center/object-designer.[objectCode].vue'` | 通过 |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 |
| 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过 |
| Diff 检查 | `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessCodeAppFormAssetPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessCodeFormProviderRegistry.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowAppConfigService.java` | 无空白错误 |

## 本轮增量验证 2026-06-30 已有代码应用对象入口修正

本轮新增风险点：

- 采购审批已有 `ai_business_object` 占位对象，嵌入式设计器不能因为有 objectId 就落回普通低代码 `businessObjectDesigner(object.id)`。
- 嵌入式打开时 URL 没有 `codeApp=1`，必须从对象 `options/designerOptions` 识别代码应用。
- 已有业务对象的代码应用保存表单设计/列表设计时，`options.codeAppMetadata` 必须写入流程绑定，不能只在“无对象代码应用”分支生效。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint 'src/views/app-center/object-designer.[objectCode].vue'` | 通过 |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 |
| 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过 |
| Diff 检查 | `git diff --check -- 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowAppConfigService.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

## 本轮增量验证 2026-06-30 待办节点展示与业务上下文查询优化

本轮新增风险点：

- 节点名称清理不能影响流程标题、业务摘要和已结束任务兜底展示。
- 待办、已办、我发起三个页面的节点展示入口要保持一致。
- `BusinessFlowService` 列表增强只做请求内缓存，不能引入跨请求脏数据。
- 审批表单上下文加载要保留代码表单和低代码表单的兼容解析。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/flow/utils/processDisplay.js src/views/flow/todo.vue src/views/flow/done.vue src/views/flow/started.vue` | 通过 |
| 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过 |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check -- forge-admin-ui/src/views/flow/utils/processDisplay.js forge-admin-ui/src/views/flow/todo.vue forge-admin-ui/src/views/flow/done.vue forge-admin-ui/src/views/flow/started.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

## 本轮增量验证 2026-06-30 审批表单加载误查修正

本轮新增风险点：

- Flowable `processDefinitionId` 可能是 UUID 或 `key:version:id`，审批表单加载必须解析为真实流程模型 Key 后再查 `sys_flow_model`。
- 采购审批业务代码表单使用 `formType=business` / `formMode=BUSINESS_CODE_FORM`，不能被当成动态表单去查 `sys_flow_form`。
- `taskId` 已能返回完整表单信息时，业务侧不应再调用流程实例表单信息接口做重复兜底。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check -- forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

## 本轮增量验证 2026-06-30 流程定义匹配兼容

本轮新增风险点：

- 待办业务表单身份校验仍要校验任务 ID、办理人、流程实例、业务 Key 和任务节点，不能因为兼容流程定义表示而放开访问。
- 流程定义比较需要兼容业务模型 Key、Flowable `key:version:id` 和历史 UUID 型值，避免已存在任务误报“不匹配”。
- 流程任务详情返回的 `processDefKey` 应尽量归一化为业务流程模型 Key，减少前端和业务侧继续携带 UUID。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check -- forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

## 本轮增量验证 2026-06-30 待办审批详情表单首屏优化

本轮新增风险点：

- 父级业务表单上下文透传不能影响外部表单和动态表单的原有参数。
- 采购审批组件复用 `initialTaskContext` 时，仍要保留字段权限、只读/可写、必填校验和审批提交变量。
- 待办模式跳过代码应用 metadata 请求后，字段显隐和标签需要从后端上下文字段目录兜底。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/components/common/FlowBusinessForm.vue src/views/flow/todo.vue src/views/business/purchase-order-test.vue src/composables/useBusinessTaskFormContext.js` | 通过 |
| 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过 |
| Diff 检查 | `git diff --check -- forge-admin-ui/src/components/common/FlowBusinessForm.vue forge-admin-ui/src/views/flow/todo.vue forge-admin-ui/src/views/business/purchase-order-test.vue forge-admin-ui/src/composables/useBusinessTaskFormContext.js code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

## 本轮增量验证 2026-06-30 流程定义校验复发修正

本轮新增风险点：

- 待办详情首个业务表单上下文请求不能再携带列表行旧 `processDefKey`，否则并行优化后仍可能误报“流程定义与当前任务不匹配”。
- 后端不能把流程定义表示差异作为硬安全边界，但仍必须保留任务 ID、办理人、流程实例、业务 Key 和任务节点校验。
- 只改 `todo.vue` 参数收敛后，需要确保前端语法检查通过；后端兜底调整需要目标模块编译通过。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/flow/todo.vue` | 通过 |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check -- forge-admin-ui/src/views/flow/todo.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md code-copilot/memory/pitfalls.md` | 无空白错误 |

## 本轮增量验证 2026-06-30 审批详情表单重复请求优化

本轮新增风险点：

- 采购审批业务表单详情跳过前端 `/api/flow/task/form/{taskId}` 后，审批按钮策略仍必须来自业务表单上下文。
- 动态表单和外部表单不能被业务表单优化影响，业务上下文未识别为业务表单时仍要补查流程表单信息。
- `FlowTaskServiceImpl#getTaskFormInfo` 内部去重后，业务表单、动态表单和节点审批策略解析都要编译通过。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/flow/todo.vue` | 通过 |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check -- forge-admin-ui/src/views/flow/todo.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessTaskFormContextVO.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

## 本轮增量验证 2026-06-30 采购审批业务表单节点标签去编码

本轮新增风险点：

- 采购审批自定义业务表单头部标签不能再直接显示 `taskDefKey`。
- 节点中文兜底不能影响 `isDeptLeaderTask/isEngineeringTask/isCountersignTask/isModifyTask` 等业务判断，业务判断仍基于原始 `taskDefKey`。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/business/purchase-order-test.vue` | 通过 |
| Diff 检查 | `git diff --check -- forge-admin-ui/src/views/business/purchase-order-test.vue code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

## 本轮增量验证 2026-06-30 应用中心业务单元编辑

本轮新增风险点：

- 编辑抽屉保存时不能清空业务对象已有 `modelId/modelCode/options`。
- 跨业务域移动业务单元时，访问入口、设计版本、触发器和单据配置需要跟随新业务域；存在对象关系时必须阻止移动。
- 应用总览、业务域详情和业务单元详情三个入口都要能打开同一个编辑能力。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/BusinessUnitCard.vue src/views/app-center/components/ObjectCard.vue src/views/app-center/components/BusinessObjectEditorDrawer.vue src/views/app-center/index.vue 'src/views/app-center/suite.[suiteCode].vue' 'src/views/app-center/object.[objectCode].vue'` | 通过 |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check -- forge-admin-ui/src/views/app-center/components/BusinessUnitCard.vue forge-admin-ui/src/views/app-center/components/ObjectCard.vue forge-admin-ui/src/views/app-center/components/BusinessObjectEditorDrawer.vue forge-admin-ui/src/views/app-center/index.vue 'forge-admin-ui/src/views/app-center/suite.[suiteCode].vue' 'forge-admin-ui/src/views/app-center/object.[objectCode].vue' forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessObjectMapper.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessObjectMapper.xml code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

手工验收建议：

1. 在应用总览业务单元卡片菜单点击“编辑业务单元”，修改名称或说明保存，刷新后确认回显。
2. 在业务域详情对象卡片菜单点击“编辑业务单元”，修改所属业务域保存，确认页面切换到新业务域且访问入口仍跟随该业务单元展示。
3. 在业务单元详情页点击“编辑业务单元”，修改排序或启用状态保存，确认详情页和卡片状态一致。
4. 对已配置对象关系的业务单元尝试跨业务域移动，确认后端提示需要先调整或删除对象关系。

## 本轮增量验证 2026-06-30 业务单元显示字段配置收敛

本轮新增风险点：

- 新建业务单元时不能再暴露需要用户手输字段编码的“显示字段”。
- 编辑业务单元基础信息时不能把已有对象级 `displayField` 清空。
- 对象设计器仍需保留默认标题字段下拉，关联关系配置继续作为运行态回显字段主入口。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/BusinessObjectWizardDrawer.vue src/views/app-center/components/BusinessObjectEditorDrawer.vue 'src/views/app-center/object-designer.[objectCode].vue'` | 通过 |
| Diff 检查 | `git diff --check -- forge-admin-ui/src/views/app-center/components/BusinessObjectWizardDrawer.vue forge-admin-ui/src/views/app-center/components/BusinessObjectEditorDrawer.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

## 本轮增量验证 2026-06-30 表单资产移除、标题模板和停用确认

本轮新增风险点：

- 业务表单资产移除必须跨保存持久化；Provider 默认资产不能在后端合并时重新出现。
- 采购审批示例绕过通用业务流程发起服务，需要单独验证标题模板读取和模板变量替换。
- 业务域/业务单元存在按钮停用和编辑抽屉停用两条路径，均需要二次确认；删除确认不能被破坏。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/designer/BusinessCodeAppFormAssetPanel.vue src/views/app-center/index.vue 'src/views/app-center/suite.[suiteCode].vue' 'src/views/app-center/object.[objectCode].vue' src/views/app-center/components/BusinessObjectEditorDrawer.vue src/views/app-center/components/SuiteEditorDrawer.vue` | 通过 |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core,forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessCodeAppFormAssetPanel.vue forge-admin-ui/src/views/app-center/index.vue 'forge-admin-ui/src/views/app-center/suite.[suiteCode].vue' 'forge-admin-ui/src/views/app-center/object.[objectCode].vue' forge-admin-ui/src/views/app-center/components/BusinessObjectEditorDrawer.vue forge-admin-ui/src/views/app-center/components/SuiteEditorDrawer.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/service/impl/SamplePurchaseOrderServiceImpl.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

手工验收建议：

1. 进入采购审批代码应用“业务流程配置 > 业务表单资产”，移除默认表单资产并保存，刷新后确认不再自动出现；点击新增后可重新添加。
2. 配置采购审批流程标题模板，例如 `采购审批-{orderNo}-${title}`，发起采购流程后确认待办/流程实例标题按模板显示。
3. 在应用总览、业务域详情和业务单元详情点击停用业务域/业务单元，确认先弹出二次确认；删除业务域/业务单元仍先弹确认。
4. 在业务域或业务单元编辑抽屉中把启用状态切到停用并保存，确认先弹出二次确认。

## 本轮增量验证 2026-06-30 采购审批驳回状态修正

本轮新增风险点：

- 流程任务 complete 前写入流程实例变量不能影响网关条件判断和正常同意/驳回流转。
- 采购审批回调需要兼容 `approvalResult=reject` 和 `approved=false`，避免驳回动作变量表示差异导致状态不变。
- 修改位于流程插件和业务模块，需要目标模块一起编译。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-business/forge-business-core -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check -- forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/service/impl/SamplePurchaseOrderServiceImpl.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

手工验收建议：

1. 发起一张采购审批单，在部门负责人或工程部经理节点点击“驳回”，确认采购单状态从“审批中”变为“待修改”。
2. 申请人修改节点重新提交后，确认状态回到“审批中”。
3. 申请人修改节点选择终止/驳回后，确认采购单状态进入“已驳回”。

## 本轮增量验证 2026-06-30 采购审批申请人修改状态兜底

本轮新增风险点：

- 监听 `TASK_CREATED` 不能影响采购审批首个部门负责人待办，也不能把已通过/已驳回单据错误改回待修改。
- 对已经卡住的存量申请人修改待办，保存字段时需要自愈状态，避免重新提交前置保存失败。
- 申请人修改节点重新提交后，仍要通过原有 `TASK_COMPLETED` 逻辑把状态切回“审批中”。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check -- forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/service/impl/SamplePurchaseOrderServiceImpl.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

手工验收建议：

1. 对当前已经卡住在“申请人修改”节点但采购单状态仍是“审批中”的实例，直接点击重新提交，确认不再报“当前采购单不是待修改状态”。
2. 新发起采购单，在部门负责人或工程部经理节点驳回，确认生成申请人修改待办后采购单状态变为“待修改”。

## 本轮增量验证 2026-06-30 采购审批重新提交状态兜底

本轮新增风险点：

- 申请人修改后重新提交进入下一轮普通审批节点时，采购单状态必须从 `NEED_MODIFY` 回到 `IN_PROCESS`。
- 普通审批节点创建兜底不能影响初次提交、已通过、已驳回或已取消单据。
- 对已经进入普通审批节点但状态仍为 `NEED_MODIFY` 的存量待办，审批节点字段保存需要自愈状态，避免保存时报“当前采购单不是审批中状态”。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check -- forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/service/impl/SamplePurchaseOrderServiceImpl.java forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/support/SamplePurchaseOrderFlowDefinition.java code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

手工验收建议：

1. 对当前已经重新提交但状态仍是“待修改”的实例，刷新下一轮审批待办并保存/审批，确认不再被“不是审批中状态”拦截。
2. 新发起采购审批，按“驳回 -> 申请人修改 -> 重新提交”走一轮，确认重新提交后采购单状态变为“审批中”。

## 本轮增量验证 2026-06-30 采购审批存量状态对账

本轮新增风险点：

- 采购单列表/详情查询增加对 `sys_flow_task` 当前待办节点的读取，必须避免影响已通过、已驳回、已取消等终态记录。
- 对账只修复 `IN_PROCESS/NEED_MODIFY` 两类流转中状态，避免误改草稿或终态。
- XML 查询必须按租户和业务 Key 限定，并只读取待办/已签收的活跃任务。

| 类型 | 命令 | 预期 |
| --- | --- | --- |
| 后端编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core -am compile -DskipTests` | 通过 |
| Diff 检查 | `git diff --check -- forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/mapper/SamplePurchaseOrderMapper.java forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/service/impl/SamplePurchaseOrderServiceImpl.java forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/support/SamplePurchaseOrderFlowDefinition.java forge-server/forge-business/forge-business-core/src/main/resources/mapper/business/SamplePurchaseOrderMapper.xml code-copilot/changes/code-app-metadata-config/spec.md code-copilot/changes/code-app-metadata-config/tasks.md code-copilot/changes/code-app-metadata-config/test-spec.md code-copilot/changes/code-app-metadata-config/execution-log.md` | 无空白错误 |

手工验收建议：

1. 对当前已经重新提交但状态仍是“待修改”的采购单，刷新采购单列表或打开详情，确认状态自动恢复为“审批中”。
2. 对仍处在申请人修改节点的采购单，刷新列表或详情，确认状态保持“待修改”。
