# 业务应用与流程模块体验整合
> status: applied (P0/P1 完成，P2 部分完成)
> created: 2026-06-29
> complexity: 🔴 复杂
> related:
> - `code-copilot/changes/unified-business-flow-app-config/spec.md`
> - `code-copilot/changes/business-flow-lowcode-integration/spec.md`
> - `code-copilot/changes/lowcode-app-full-loop-optimization/spec.md`

## 1. 背景与目标

`unified-business-flow-app-config` 已经完成第一轮整合：统一配置 Facade、节点抽屉、表单资产目录、字段权限后端兜底。但用户实测采购单审批样例时，仍然反馈"流程模块和低代码应用模块割裂、用户没法用"。

具体痛点（用户原话，针对采购单审批样例）：

1. 流程待办列表显示 `sample_purchase_order` 等英文 key，不是中文流程名。
2. 待办详情顶部出现"代码业务"等技术标签，业务字段里也露出 `id` 等技术字段。
3. 从 `/flow/model` 列表直接点"设计"进入流程设计器时，没有业务上下文，表单资产只能选通用流程表单，业务表单加载不上。
4. 流程设计器节点配置里的"节点表单资产"是 `n-select` 下拉，没有预览、没有字段数、没有来源标签，样式与低代码应用的资产卡片体验不一致。
5. `/flow/form`、`/flow/template` 等通用流程表单配置与业务应用表单完全平行，普通业务用户不知道何时该用哪一个。
6. 流程节点配置的字段权限保存到了 BPMN，但待办使用 `<AiForm>` 渲染业务表单时没有传 `field-permissions`，导致权限在前端不生效（后端兜底过滤还在，但用户感知是"配了等于没配"）。
7. 待办节点动态表单（`FlowFormCreateRenderer` / form-create 引擎）与业务侧 `<AiForm>` 是两套渲染引擎，样式割裂；同时代码型业务对象（采购申请样例）在应用中心设计器里没有"表单设计"面板入口，用户找不到表单字段在哪定义。

**目标**：以采购单审批为试金石，建立"业务应用 = 业务的家、流程 = 运输工具、`/flow/*` = 高级/独立流程入口"的产品分工，把上述 7 个问题（P0/P1 必修，P2 收口）落到可验证的代码变更：

- 普通用户在待办里看到的永远是中文业务摘要 + 中文流程名，不再出现 `formKey`、`processDefinitionKey` 等技术概念。
- 待办里渲染的业务表单与业务侧运行时的业务表单视觉一致；字段权限在前端立即生效（不可见隐藏、不可编辑置灰、必填红星）。
- 业务管理员配置流程节点表单资产时看到的是"资产卡片库"（中文名、字段数、来源标签、字段预览），与低代码应用中心选业务字段、关联流程时的卡片体验一致。
- 代码型业务对象（采购申请这类 Provider 注册的）在应用中心设计器里出现"表单字段"只读面板，告知用户字段来自代码 Provider，并可视化字段目录、必填、字段权限矩阵；同时弱化 `/flow/form`、`/flow/template`、`/flow/model` 在普通用户视图中的曝光（加 Banner、加入"高级 / 流程库管理"分组）。

首期仍不重写 form-create 动态表单引擎、不新增独立审批引擎；范围严格收敛在"用户体验整合 + 字段权限生效 + 代码表单可视化"。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

1. 待办列表展示流程名错误：
   - `forge-admin-ui/src/views/flow/todo.vue:70-72`：`#title` 插槽渲染 `{{ row.title || row.taskName }}`，`title` 是流程发起时拼的 businessKey/key 字符串，不是中文流程名。
   - `forge-admin-ui/src/views/flow/todo.vue:160`：详情区使用 `getProcessDisplayName(currentTask)` 回退链取 `processName / processTitle / modelName`，证明后端已经返回中文 `processName`，但列表没用。
   - 后端：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/.../FlowTaskMapper.xml` `selectTodoTasks` 已 join `m.model_name AS process_name`。

2. 待办详情顶部出现技术标签：
   - `forge-admin-ui/src/views/flow/todo.vue:249-254`：业务托管表单段落显示 `<n-tag>代码业务</n-tag>`，标题 `businessFormTitle`（todo.vue:525 附近）直接来自后端 `formName`。
   - 后端：`SamplePurchaseOrderCodeFormProvider#FORM_NAME`（`forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/provider/SamplePurchaseOrderCodeFormProvider.java:29`）= "采购单审批表单"；字段定义里 `field("id", "采购单ID", ...)` 等技术字段也通过 `buildFields` 暴露给前端（同文件 :120-180）。
   - `BusinessFlowFormAssetVO` / `BusinessCodeFormAsset` 当前没有 `internal` 字段，前端无法区分系统字段。

3. 流程模型直接进入设计器无业务上下文：
   - `forge-admin-ui/src/views/flow/model.vue:747-749` `handleDesign(row)` 仅设置 `currentDesignModelId.value = row.id`，弹出 `FlowDesignPage` 模态。
   - `forge-admin-ui/src/views/flow/design.vue:714-715` `businessObjectCode = route.query.businessObjectCode || objectCode`，仅从路由查询；嵌入式打开时 `businessContextActive` = false。
   - `forge-admin-ui/src/views/flow/design.vue:903-922` `nodeFormAssetOptions` 业务上下文不存在时落到 `formOptions`（通用流程表单），业务表单资产被屏蔽。
   - 反查接口已规划但未接入：spec `unified-business-flow-app-config` §6 列出"可选新增 `/ai/business/flow/model/{modelKey}/business-bindings`"。

4. 节点表单资产为下拉控件：
   - `forge-admin-ui/src/components/flow-designer/panel/ApproverConfig.vue:131-144`：`<n-select :options="normalizedFormAssetOptions">` 直接渲染表单 key 列表。
   - `forge-admin-ui/src/components/flow-designer/panel/NodeConfigDrawer.vue:42,73` 透传 `formAssetOptions`。
   - `forge-admin-ui/src/components/flow-designer/panel/FormPermissionConfig.vue` 只做字段权限矩阵，与资产选择是两个孤立块。
   - 已存在的卡片选择器组件：`forge-admin-ui/src/views/app-center/components/designer/BusinessFlowFormAssetSelect.vue`，但仅在应用中心侧使用，未跨用到流程设计器。

5. 通用流程表单与业务应用表单平行：
   - `forge-admin-ui/src/views/flow/form.vue`：通用流程表单的 CRUD 列表（标题"表单管理"）。
   - `forge-admin-ui/src/views/flow/template.vue`：流程模板（含表单）的 CRUD 列表。
   - `forge-admin-ui/src/views/flow/design.vue:686` 通过 `businessFlowFormAssets` API 拿业务侧资产；`design.vue:903` 两条分支：业务上下文用 `businessFormAssets`，否则用 `formOptions`（通用流程表单），两套表单源未在 UI 上统一标识。
   - `forge-admin-ui/src/router/index.js` 中流程相关路由与应用中心路由平等，没有"高级 / 流程库管理"分组。

6. 字段权限未在前端生效：
   - 后端链路（已存在）：`BusinessFlowService#buildTaskFormContext`（`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java:463`）→ `normalizeFieldPermissions(nodeForm.get("fieldPermissions"))`（同文件 :517）→ `vo.setFieldPermissions(permissions)`（:529）→ 代码表单兜底 `applyBusinessCodeFieldPermissions`（:696）。
   - 前端：`forge-admin-ui/src/views/flow/todo.vue:256-265` 业务托管 `<AiForm>` 渲染时没有传 `:field-permissions` / `:permissions`，权限信息丢失；只有动态表单分支 `<FlowFormCreateRenderer>`（todo.vue:297-302）传了 `:field-permissions="taskFormInfo.formFieldPermissions"`。
   - `forge-admin-ui/src/components/ai-form/AiForm.vue` 当前不消费字段权限，schema 即字段最终展现。
   - `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` 中的 `<AiForm>` 用法相同，业务侧详情/编辑也不接字段权限（但业务侧不需要，因为业务运行时按数据权限而非节点权限）。

7. 双渲染引擎与代码应用无表单设计入口：
   - 流程节点动态表单：`forge-admin-ui/src/components/flow-form-create/FlowFormCreateRenderer.vue`（form-create 引擎）。
   - 业务运行时表单：`forge-admin-ui/src/components/ai-form/AiForm.vue`（自研引擎），由 `AiCrudPage.vue` 调用。
   - 业务对象设计器面板：`forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue:240-258` nav 列表为 `basic / form / fields / list / relations / flow-app / permission / publish / advanced`；其中 `form` 面板（"表单设计"）目前对代码应用（`isCodeApp=true`）隐藏或不可编辑，导致用户找不到字段定义入口。
   - 代码应用对应 Provider：`SamplePurchaseOrderCodeFormProvider#formAssets` 已经能列字段（`buildFields`），但应用中心设计器没有面板把它呈现出来。

### 2.2 现有实现的合理部分

- 后端 `BusinessFlowService` 已经把字段权限统一沉到 `vo.fieldPermissions`，前端只需要正确消费即可；不需要再改后端契约。
- `BusinessFlowFormAssetSelect.vue` 卡片选择器已经存在且在应用中心生产可用，复用到流程设计器节点抽屉的实现成本低。
- `unified-business-flow-app-config` 已经把"应用中心打开流程设计器"链路稳定下来（`source=appCenter`、`businessObjectCode`、`codeApp`），新增"流程模型反查业务绑定"接口可以平滑接入。
- `<AiForm>` schema 已支持自定义渲染上下文（`businessFormRenderContext`），增加字段权限消费只是 schema 处理层面的扩展，不破坏组件 API。
- 代码 Provider `BusinessCodeFormProvider` 已经返回字段目录，前端"表单字段"只读面板只需新增 UI，不需要后端接口变更。

### 2.3 发现与风险

- 字段权限前端不生效属于"用户感知 P0 bug"。后端虽然兜底过滤，但前端依旧让用户点开输入框，提交后才发现字段被丢弃，体验极差。
- 双渲染引擎在统一前必须保证：动态表单（form-create）字段权限消费已经稳定，新接 `<AiForm>` 字段权限必须与之等价（visible / editable / required 三态语义一致），否则会出现"两个待办两种样子"。
- `/flow/model` `/flow/form` `/flow/template` 直接弱化或加 Banner，会影响"流程库管理员"用户日常操作，必须先确认管理员仍能从某处直达（如顶部"能力中心"或左侧菜单的高级分组），不能误删。
- 代码 Provider 的字段目录目前与节点字段权限矩阵的字段编码契约未严格对齐，新增"表单字段"只读面板时必须同时核对：`provider.fields[].code` ↔ `node.fieldPermissions[].fieldCode`，否则面板里看到的字段和节点权限里能配的字段对不上。
- 流程名国际化：`processName` 来自 `ai_flow_model.model_name`，对历史模型可能为空，前端 fallback 链需要兜底到 `taskName` / `processDefinitionName` / 流程定义 key 翻译。
- 顶部"代码业务"标签去掉后，仍然需要在"高级"折叠区暴露给开发者，以便排查。

## 3. 功能点

- [ ] **F1 待办列表中文流程名**：列表 `#title` 渲染优先使用 `processName / processDefinitionName`（中文），并叠加业务对象中文摘要（如"采购申请单"+ 业务编号 + 申请人 + 关键金额/日期）。fallback 链：`businessSummary → processName → processDefinitionName → taskName → processDefinitionKey`。
- [ ] **F2 待办详情移除技术标签**：移除"代码业务"`<n-tag>`；`businessFormTitle` 改用 `businessObjectName`（业务对象中文名）+ 业务摘要；技术字段（如 `id`、`tenant_id`、`createBy`）由 Provider 声明 `internal=true` / `system=true`，前端默认隐藏，仅"高级"折叠区可见。
- [ ] **F3 流程模型自动注入业务上下文**：流程模型列表 `model.vue` 每行新增"绑定业务应用"列；进入设计器时若该模型已被业务对象绑定，自动以 `businessObjectCode` 注入业务上下文；当业务上下文存在时，节点表单资产分支强制走业务资产，并在画布顶部 Banner 显示"当前流程已绑定业务对象 ×××"。
- [ ] **F4 节点表单资产卡片化**：节点抽屉"表单权限"页签中，把 `<n-select>` 替换为复用 `BusinessFlowFormAssetSelect.vue` 的卡片选择器；卡片显示中文名、来源标签（业务对象 / 代码 Provider / 外部地址）、字段数、字段预览（前 N 个字段编码 + 标签）。
- [ ] **F5 流程通用表单弱化与归位**：`/flow/form`、`/flow/template` 页头加 Banner："该列表仅用于未绑定业务应用的独立流程，业务应用请到应用中心配置"；侧边菜单将 `/flow/model`、`/flow/form`、`/flow/template`、`/flow/monitor` 归入"流程库管理"高级分组，仅角色为 `flow_admin` / `superadmin` 默认展开；普通用户从应用中心进入即可，看不到这些入口。
- [ ] **F6 业务表单字段权限前端生效**：`<AiForm>` 新增 `field-permissions` prop（与 `FlowFormCreateRenderer.vue` 已有契约一致：`visible / editable / required` 三态）；`AiForm.vue` 内部按字段编码匹配，不可见字段不渲染、不可编辑字段 `disabled`、必填字段 `required`；`todo.vue` 业务托管表单分支传入 `taskFormInfo.fieldPermissions`；已办（`done.vue`）一律 `editable=false`。
- [ ] **F7 代码应用表单字段只读面板**：业务对象设计器 `BusinessObjectDesignerShell.vue` nav 中新增/启用 `form` 面板对代码应用（`isCodeApp=true`）的入口，进入后显示"表单字段（由代码 Provider 提供，只读）"；列表展示 `provider.fields[]` 的字段编码、标签、类型、是否必填、是否系统字段；右侧支持选择"流程节点"后展示该节点的字段权限矩阵（visible/editable/required），方便业务管理员在不进入流程设计器的情况下检视权限。
- [ ] **F8 流程模型反查业务绑定接口**：新增 `GET /ai/business/flow/model/{modelKey}/business-bindings`，返回该流程是否被业务对象绑定、绑定对象列表（含中文名、入口路径），供 `model.vue` 列表与 `design.vue` 业务上下文反注入。

## 4. 业务规则

1. 列表呈现优先级（待办、已办、我发起、抄送我）：业务摘要 > 中文流程名 > taskName > 流程定义 key。任何场景都不允许 `processDefinitionKey` 作为最终回退之外的展示。
2. 待办详情顶部信息只展示"业务对象中文名 + 业务编号 + 当前节点中文名 + 申请人 + 提交时间"；"代码业务"、`formKey`、`providerKey` 等技术信息只允许在"高级"折叠区展示。
3. 代码 Provider 字段必须支持 `internal` / `system` 标志位（默认 false）。`internal=true` 的字段：业务运行时表单、待办表单、字段权限矩阵默认不展示；流程节点字段权限保存时也不出现在矩阵中。
4. 流程设计器节点表单资产卡片必须能区分三类来源：业务对象表单（绿色徽标）、代码 Provider 表单（蓝色徽标）、外部地址（灰色徽标 + "高级"标识）。
5. 业务上下文存在（`businessContextActive=true`）时：
   - 节点资产选择只允许业务侧资产；外部地址被折叠到"高级设置"。
   - 业务上下文 Banner 必须在画布顶部常驻显示，显示业务对象中文名 + 返回业务应用按钮。
6. 字段权限三态语义（前后端一致）：
   - `visible=false`：前端不渲染、后端拒绝写入。
   - `visible=true && editable=false`：前端只读（disabled）、后端拒绝写入。
   - `visible=true && editable=true && required=true`：前端必填校验、后端必填校验。
7. `<AiForm>` 字段权限消费规则：未提供 `field-permissions` 时保持原行为（schema 即权限），保证业务运行时不受影响。
8. 已办 / 历史详情一律 `editable=false`，不论原节点权限矩阵如何配置。
9. `/flow/form`、`/flow/template`、`/flow/model`、`/flow/monitor` 在普通用户菜单中不显示；保留通过直接 URL 访问，仅 `flow_admin` / `superadmin` 角色看到入口。流程库管理员入口归类到"能力中心 → 流程库管理"。
10. 业务对象设计器 `form` 面板对代码应用只读；任何尝试编辑代码 Provider 字段的交互必须给出明确说明"字段由代码 Provider 提供，编辑请联系开发人员"。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
| --- | --- | --- | --- |
| 无 | `ai_flow_model` | 无 | 流程名已存 `model_name`，无需新增字段。 |
| 无 | `ai_business_binding` | 无 | 业务绑定已存在；反查接口基于现有数据实现。 |
| 无 | `ai_business_document_config` | 无 | 不涉及。 |
| 可选 | `ai_business_app` | 无 | 若需新增"流程库管理员"角色入口，在权限种子里追加菜单项，不动表结构。 |

首期不新增 Flyway 脚本。代码 Provider 字段的 `internal` / `system` 标志通过 Java 字段注解或 `field()` 工厂方法签名扩展承载（详见 tasks.md）。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
| --- | --- | --- | --- |
| 新增 | `/ai/business/flow/model/{modelKey}/business-bindings` | GET | 返回流程模型被哪些业务对象绑定（中文名、objectCode、入口路径、codeApp 标志）。 |
| 增强 | `/ai/business/flow/form-assets/{objectCode}` | GET | 资产 VO 增加 `internal`、`systemField` 字段；字段目录 `fields[]` 增加 `internal`、`systemField`、`description`。 |
| 增强 | `/ai/business/flow/task-form-context` | GET | 业务托管表单上下文已含 `fieldPermissions`，本次确认前端契约可用；如缺字段补齐 `businessObjectName`、`businessSummary`。 |
| 增强 | `/ai/flow/task/todo` 列表查询 | GET | 返回字段补齐 `businessObjectName`、`businessSummary`、`processName`（已存在则确认），不破坏既有字段。 |

## 7. 影响范围

- 后端：
  - `forge-plugin-generator`：`BusinessFlowService`、`BusinessFlowController`（新增反查接口）、`BusinessFlowFormAssetVO`、`BusinessCodeFormProviderRegistry`、`BusinessCodeFormAsset`、`BusinessCodeFormField`。
  - `forge-business-core`：`SamplePurchaseOrderCodeFormProvider` 字段补标 `internal` / `system`；`BusinessFlowTaskQueryVO`（或对应 DTO）补 `businessObjectName` / `businessSummary`。
  - `forge-plugin-flow`：`FlowTaskMapper.xml` 列表查询确保 `processName` 等字段对外暴露；列表 VO 补齐字段（如缺）。

- 前端：
  - `forge-admin-ui/src/views/flow/todo.vue`、`done.vue`、`started.vue`、`cc.vue`：列表 `#title` / `#meta` 渲染规则统一。
  - `forge-admin-ui/src/views/flow/model.vue`：新增"绑定业务应用"列；进入设计器自动注入业务上下文。
  - `forge-admin-ui/src/views/flow/design.vue`：业务上下文存在时强制业务资产；顶部 Banner。
  - `forge-admin-ui/src/views/flow/form.vue`、`template.vue`：页头 Banner、菜单分组。
  - `forge-admin-ui/src/components/flow-designer/panel/ApproverConfig.vue`、`NodeConfigDrawer.vue`：表单资产改为卡片选择器（复用 `BusinessFlowFormAssetSelect.vue`）。
  - `forge-admin-ui/src/components/ai-form/AiForm.vue`：新增 `field-permissions` prop 与消费逻辑。
  - `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`：保持原行为（不传 `field-permissions`）。
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue`、新增 `BusinessFormFieldsReadonlyPanel.vue`：代码应用"表单字段"只读面板。
  - `forge-admin-ui/src/api/business-app.js`：新增反查 API；表单资产 API 字段扩展。
  - `forge-admin-ui/src/router/index.js`、左侧菜单数据：流程库管理分组、权限角色控制。

## 8. 风险与关注点

> ⚠️ 本变更涉及字段权限运行时生效（提交数据写入），必须重点验证。

- **字段权限三态语义对齐**：`AiForm` 与 `FlowFormCreateRenderer` 在 `visible/editable/required` 表达上必须等价；测试用例必须覆盖三态。
- **已办只读强一致**：`done.vue` 必须显式覆盖为 `editable=false`，否则历史任务可能被编辑。
- **菜单弱化的权限回旋**：`/flow/form`、`/flow/template`、`/flow/model` 弱化后，流程库管理员仍需有清晰入口；否则会出现"管理员找不到流程模型"。
- **代码 Provider 字段标志兼容**：旧 Provider 未声明 `internal=true` 字段不能被自动隐藏；首期只对 `SamplePurchaseOrderCodeFormProvider` 与 `id`、`tenant_id` 等显式系统字段补标，其他 Provider 走"全部展示"兜底。
- **反查接口性能**：`/business-bindings` 接口被流程模型列表批量调用，需要按 `modelKey` 索引查询；如无索引需补 `ai_business_binding(flow_model_key)` 索引（首期不动表结构，先观察 QPS）。
- **流程名国际化**：`processName` 来自 `model_name`，对部分历史模型可能为空字符串/英文 key；前端必须有 fallback。
- **跨模块联调**：流程模型设计器是否真的接收业务上下文 Banner、是否产生重复请求，必须在 `/test` 阶段联调验证。

## 8.5 测试策略

- **测试范围**：
  - 后端：表单资产 VO 新字段、反查接口、`buildTaskFormContext` 业务上下文回填。
  - 前端：`AiForm` 字段权限消费（visible/editable/required 三态）、待办列表中文名渲染、节点资产卡片选择器交互、代码应用"表单字段"只读面板。
  - 端到端：以采购单审批为试金石，覆盖"待办中文名 → 打开表单（业务样式 + 权限生效）→ 提交 → 已办只读 → 节点权限改动后再打开"完整链路。
- **覆盖率目标**：
  - 字段权限三态：每一态至少一个用例；同一表单同时混合三态至少一个用例。
  - 资产来源：业务对象 / 代码 Provider / 外部地址各至少一个卡片用例。
  - 列表回退链：业务摘要存在 / 仅有 processName / 仅有 taskName / 仅有 key 四个回退档位。
  - 流程模型反查：绑定 0 / 1 / N 个业务对象三档。
- **独立 Test Spec**：是，见 `test-spec.md`。

## 9. 待澄清

- [x] **菜单分组角色**：复用 `superadmin`，不新增 `flow_admin`。`/flow/form` `/flow/template` `/flow/model` `/flow/monitor` 通过前端 `requireRole: ['superadmin']` 控制可见性，普通业务用户菜单不显示；保留 URL 直访路径供后续按需扩展。（2026-06-29 用户确认）
- [x] **业务摘要字段来源**：采用混合策略 —— 低代码业务对象使用 `summaryExpression`（在业务对象设计器配置），代码 Provider 实现 `buildSummary(record)` 钩子（接口默认实现返回 null）。两者都缺时回退到中文流程名 + 业务编号。（2026-06-29 用户确认）
- [ ] **字段权限矩阵的字段编码契约**：当前 `node.fieldPermissions[].fieldCode` 与 `provider.fields[].code` 是否严格一致？由 `/apply` 阶段 Task 7（采购单样例）落地时回归核对，作为实现期校验项，不阻塞 `/apply` 启动。

> HARD-GATE 已通过。第 3 项为实现期回归校验项。

## 10. 技术决策

1. **不重写表单引擎**：业务侧 `AiForm` 与流程侧 `FlowFormCreateRenderer` 在 P0/P1 阶段保持双引擎；P2 阶段评估将节点动态表单收敛到 `AiForm`。本次只做"`AiForm` 增加字段权限消费"，让"绑定业务对象的流程"的待办全部走 `AiForm`，自然消除业务/流程样式割裂。
2. **复用 `BusinessFlowFormAssetSelect.vue`**：节点抽屉资产选择器不再自造，直接复用已经存在的卡片组件，最小化新增组件。
3. **代码 Provider 字段标志通过工厂方法重载**：`field(code, label, type, required, internal)` 重载签名，向后兼容；旧 Provider 不需要修改即可工作。
4. **流程模型反查接口轻量化**：只返回必要字段（中文名、objectCode、入口路径、codeApp 标志），不返回完整绑定配置；前端按需在设计器点 Banner 跳转。
5. **菜单分组用前端权限控制**：不动后端权限模型；通过菜单配置增加 `requireRole` 字段，由前端路由守卫控制可见性（首期保守方案；如后端权限模型已支持，则改后端控制）。
6. **业务摘要混合策略**：低代码业务对象用 `summaryExpression`（在业务对象设计器配置）；代码 Provider 用 `buildSummary(record)` 钩子；都缺时回退到中文流程名 + 业务编号。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
| --- | --- | --- | --- |
| Task 0 | propose | `spec.md`, `tasks.md`, `test-spec.md`, `execution-log.md` | SDD 提案，未涉及业务代码。 |
| Task 1 | completed | `AiForm.vue`, `todo.vue`, `done.vue` | `<AiForm>` 已消费 `fieldPermissions`，待办传入节点权限，已办强制只读。 |
| Task 2 | completed | `views/flow/utils/processDisplay.js`, `todo.vue`, `done.vue`, `started.vue`, `cc.vue` | 四类流程列表统一业务摘要 / 中文流程名回退链。 |
| Task 3 | completed | `todo.vue`, `done.vue`, `BusinessTaskFormContextVO.java`, `BusinessFlowService.java`, `BusinessCodeFormProvider*.java`, `SamplePurchaseOrderCodeFormProvider.java` | 移除可见“代码业务”标签，补业务对象名/摘要，代码 Provider 支持 internal/systemField/description。 |
| Task 4 | completed | `BusinessFlowController.java`, `BusinessFlowService.java`, `BusinessBindingMapper.java`, `BusinessBindingMapper.xml`, `BusinessBindingSummaryVO.java`, `business-app.js` | 新增 `/ai/business/flow/model/{modelKey}/business-bindings`。 |
| Task 5 | completed | `flow/model.vue`, `flow/design.vue`, `business-app.js` | 流程模型列表展示业务绑定，嵌入式设计器注入业务上下文和 Banner。 |
| Task 6 | completed | `ApproverConfig.vue`, `NodeConfigDrawer.vue`, `BusinessFlowFormAssetSelect.vue`, `BusinessFlowService.java` | 节点表单资产改为卡片选择器，资产接口补字段数/预览/来源信息。 |
| Task 7 | completed | `BusinessObjectDesignerShell.vue`, `BusinessFormFieldsReadonlyPanel.vue`, `BusinessFlowService.java`, `business-app.js` | 代码应用开放“表单字段”只读面板，支持查看 Provider 字段和节点权限矩阵。 |
| Task 8 | partial | `flow/form.vue`, `flow/template.vue` | 通用流程表单/模板页面已加业务应用分流提示；菜单按 `superadmin` 隐藏未落地，见偏差记录。 |
| Task 9 | partial | `tasks.md`, `test-spec.md`, `execution-log.md`, `memory/*.md` | 已完成静态/编译/构建验证和记录；未启动 admin/flow/ui 跑完整采购单端到端。 |

## 12. 审查结论

尚未进入 `/review`。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-06-29
- **确认人**：用户
- **确认内容**：
  - 流程库管理员角色：**复用 `superadmin`**，不新增 `flow_admin`。
  - 业务摘要来源：**同意混合策略**（低代码 `summaryExpression` + 代码 Provider `buildSummary(record)` 钩子，都缺则回退中文流程名 + 业务编号）。
  - **同意 P0（Task 1/2/3）单独提交并发布**，不等 P1/P2。
  - **本轮仅完成 SDD 提案**，暂不进入编码实现阶段（`/apply` 待后续指令）。
