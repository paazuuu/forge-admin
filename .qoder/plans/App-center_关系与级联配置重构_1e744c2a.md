# App-center 关系与级联配置问题诊断及重构方案

## 诊断结论

### 1. 当前架构：两条割裂的配置链路
- **对象级关系配置**：在 [BusinessRelationDesigner.vue](forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue) 中维护 `CHILD_LIST` / `DETAIL` / `REFERENCE` / `MANY_TO_MANY` 四种关系，承载主子表内嵌编辑、子表选择器、详情页签等能力。
- **字段级引用配置**：在字段资产面板 [BusinessFieldPropertyPanel.vue](forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue) 和表单设计器属性面板 [ForgePropertyPanel.vue](forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue) 中维护 `objectReference` / `recordSelector` 两种组件。
- **问题**：同一个“关联到其它业务对象”的语义被拆成两个地方，用户不知道采购单上的“供应商”应该在表单字段里配，还是在对象关系里配。

### 2. `objectReference` 组件实际上无法使用（核心 bug）
- 组件类型存在，但 [BusinessFieldPropertyPanel.vue](forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue) 和 [ForgePropertyPanel.vue](forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue) 都**没有提供 UI** 来输入 `referenceObjectCode` 和 `referenceDisplayField`。
- 运行时 [AiFormItem.vue](forge-admin-ui/src/components/ai-form/AiFormItem.vue) 的 `resolveOptionSource` 只认 `field.optionSource`，不会根据 `referenceObjectCode` 自动生成下拉选项 API。
- 结论：即使用户把字段组件选成“引用对象”，也选不到目标对象，运行时也不会加载选项，所以采购单选择供应商/仓库实现不了。

### 3. `recordSelector` 字段配置体验差
- [BusinessFieldPropertyPanel.vue](forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue) 中使用**纯文本输入框**填写 `objectCode`、`valueField`、`labelField`、`displayFields` 等（lines 151-235）。
- 没有对象选择器、字段选择器、映射可视化，用户需要手写编码。
- 与关系面板的“子表选择器”数据结构不同但能力重复。

### 4. 主子表关系配置门槛高
- [BusinessRelationDesigner.vue](forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue) 中源字段/目标字段标签会随关系类型变化（lines 80-99），但缺少向导。
- 用户需要手动选择 `sourceFieldCode`（主表字段）和 `targetFieldCode`（子表外键字段），容易配反。
- `saveMode`（replace / merge）语义未在界面充分说明，行级合并保存难以正确配置。

### 5. “级联”一词多处混用
- 字段面板的 `cascade` 用于字典父子过滤 / 远程参数过滤（lines 246-285）。
- 关系面板的 `linkage` 用于字段间联动规则（lines 362-500）。
- 两者在界面上都容易被理解为“级联”，造成混淆。

## 推荐的用户配置模型

作为业务用户，期望的配置路径是：

1. **字段级引用（供应商 / 仓库 / 物料选择）**：在**表单设计器**里直接配：
   - `objectReference`（下拉引用）：选择目标对象、值字段、显示字段、过滤参数。
   - `recordSelector`（弹窗选择器）：选择目标对象、展示列、搜索字段、字段映射。
   - 配置直接绑定在表单字段上，所见即所得。

2. **对象级主子表（采购单 + 采购明细）**：在对象设计的**关系与级联**面板中配置：
   - 选择关系类型“包含明细”。
   - 向导式选择子对象及其外键字段。
   - 配置子表保存模式、子表选择器（从物料/报价明细中批量选入明细行）。
   - 这里保留为对象级配置，因为它影响页面布局、保存策略、详情页签。

3. **联动过滤（按仓库过滤物料等）**：
   - 简单场景：在表单字段的“选项过滤”中配置远程参数过滤。
   - 复杂场景：在关系面板的“字段联动规则”中配置字段间联动。
   - 建议界面命名上把“级联”拆分为“选项过滤”和“字段联动规则”。

## 改造任务

### Task 1: 修复 `objectReference` 运行时与配置 UI
- 在表单设计器属性面板中增加 `referenceObjectCode`、`referenceDisplayField` 的可视化选择器（对象下拉 + 字段下拉）。
- 在 [AiFormItem.vue](forge-admin-ui/src/components/ai-form/AiFormItem.vue) 或运行时 schema 构建层，根据 `referenceObjectCode` + `referenceDisplayField` 自动生成 `optionSource`。
- 后端 [BusinessObjectDesignerService.java](forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectDesignerService.java) 发布时同步生成选项源元数据，避免运行时二次推断。

### Task 2: 重构 `recordSelector` 字段配置
- 将 [BusinessFieldPropertyPanel.vue](forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue) 中 recordSelector 的文本输入改为对象/字段选择器。
- 统一字段级 recordSelector 与关系级子表选择器的数据结构，尽量复用 [record-selector-utils.js](forge-admin-ui/src/components/ai-form/record-selector-utils.js)。
- 支持从当前套件或全部已发布对象中选择候选对象。

### Task 3: 优化主子表关系配置向导
- 在 [BusinessRelationDesigner.vue](forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue) 中新增“新增关系向导”。
- 根据选择的关系类型自动推荐 `sourceFieldCode` / `targetFieldCode`。
- 对 `CHILD_LIST` / `DETAIL` 增加外键字段说明和 `saveMode` 提示。
- 保存时校验目标对象已发布、字段存在。

### Task 4: 统一命名与交互
- 关系面板中“级联规则”改名为“字段联动规则”。
- 字段面板中“级联过滤”改名为“选项过滤”或保留“级联”但补充说明。
- 发布检查 [BusinessPublishChecklist.vue](forge-admin-ui/src/views/app-center/components/designer/BusinessPublishChecklist.vue) 增加引用字段配置完整性校验。

### Task 5: 端到端验证
- 按 [procurement-warehouse-acceptance.md](code-copilot/changes/lowcode-business-transaction-closure/procurement-warehouse-acceptance.md) 场景验证：
  - 采购单新增页选择供应商、仓库。
  - 采购明细子表通过子表选择器批量选择物料。
  - 保存时子表行级合并正确。
  - 详情页签正确展示采购明细。