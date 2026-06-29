# 我的工作台 + 节点就地配置 + 渲染引擎收敛
> status: confirmed (待 /apply 指令)
> created: 2026-06-29
> complexity: 🔴 复杂（结构性变更，3 个独立子迭代）
> related:
> - `code-copilot/changes/unified-business-flow-app-config/spec.md`（第一轮整合：统一配置 Facade）
> - `code-copilot/changes/unified-app-flow-experience/spec.md`（第二轮整合：7 个具体痛点 + 菜单弱化）
> - 本变更（第三轮整合：产品结构性重组）

## 1. 背景与目标

`unified-business-flow-app-config`（第一轮）打通后端契约，`unified-app-flow-experience`（第二轮）修复用户感知 7 个痛点。两轮做完后仍有 **3 个结构性问题** 没解，让"业务应用"与"流程管理"无法真正合体：

| 结构性问题 | 现状 | 用户感受 |
| --- | --- | --- |
| **A. 没有"我的工作台"产品概念** | 待办 / 已办 / 我发起 / 抄送我 仍然挂在 `/flow/*` 路由下 | 弱化 `/flow/*` 后，普通业务用户找不到待办入口；"流程"作为模块名仍然出现在导航 |
| **B. 节点配置必须跳出应用中心** | 应用中心"业务流程"tab → 点按钮 → 跳 `/flow/design` → 进画布选节点 | "两个模块拼在一起"，用户始终意识到自己离开了业务应用 |
| **C. 双渲染引擎共存** | 业务侧 `AiForm`、流程节点动态表单 `FlowFormCreateRenderer`（form-create） | 同一个待办列表里两种样式；独立流程的待办与业务待办视觉割裂 |

本变更目标：把上述 3 个结构性问题落地为**一轮独立变更**，但拆成 **3 个可独立交付的子迭代（A / B / C）**，用户可按业务节奏分批发布。

完成后期望效果（采购单审批 + 任意独立审批流程为试金石）：

- **顶部导航**：`应用中心 │ 我的工作台 │ 能力中心（高级）`。普通业务用户日常只在前两项之间切换。
- **应用中心节点就地配置**：业务管理员在应用中心"业务流程"tab 选中节点，右侧抽屉直接配审批人 + 字段权限 + 高级设置；只有需要重画 BPMN 拓扑时才进入完整流程设计器。
- **一套表单渲染引擎**：待办 / 已办 / 业务运行时 / 节点动态表单全部走 `AiForm`，form-create 完成迁移并标记为 deprecated。

非目标（明确不做）：

- 不重写 BPMN 引擎、不替换 Flowable。
- 不动后端 `BusinessFlowAppConfigService` / `BusinessFlowService` 的契约（前两轮已稳定）。
- 不引入"流程数据看板"等新功能。
- 不做 i18n（仍以中文为主语种，对英文环境只保证不显示乱码）。

## 2. 代码现状（Research Findings）

### 2.1 顶部导航与路由

1. **顶部导航当前结构**：
   - `forge-admin-ui/src/views/app-center/index.vue:9` 顶部按钮"能力中心"跳 `/app-center/engines`。
   - `forge-admin-ui/src/router/index.js`：`/app-center/*` 与 `/flow/*` 是平级路由，没有"我的工作台"概念。
   - 顶部布局组件：`forge-admin-ui/src/layout/components/AppHeader.vue`（待核对路径），承担顶部导航与切换。

2. **待办相关页面**：
   - `forge-admin-ui/src/views/flow/todo.vue` — 我的待办（已在第二轮 spec 改造列表中文名 / 字段权限 / 移除技术标签）。
   - `forge-admin-ui/src/views/flow/done.vue` — 我的已办。
   - `forge-admin-ui/src/views/flow/started.vue` — 我发起的。
   - `forge-admin-ui/src/views/flow/cc.vue` — 抄送我的。

3. **`/flow/*` 管理类页面**（第二轮 spec F5/Task 8 已规划弱化）：
   - `forge-admin-ui/src/views/flow/model.vue` 流程模型库
   - `forge-admin-ui/src/views/flow/form.vue` 通用表单管理
   - `forge-admin-ui/src/views/flow/template.vue` 流程模板
   - `forge-admin-ui/src/views/flow/monitor.vue` 流程监控

### 2.2 应用中心 → 流程设计器的当前跳转

1. `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue:227` 按钮"打开流程设计器"，点击触发 `openFlowDesigner`（同文件 :829-843）：`router.push({ path: '/flow/design', query: { id: modelId, businessObjectCode, codeApp, source: 'appCenter' } })`。
2. `forge-admin-ui/src/views/flow/design.vue:686` 通过 `businessFlowFormAssets` API 拿业务侧资产；`design.vue:903-922` 业务上下文存在时使用业务资产，否则使用通用表单。
3. **节点抽屉组件**（第二轮 spec F4 改造的卡片化目标）：
   - `forge-admin-ui/src/components/flow-designer/panel/NodeConfigDrawer.vue` 抽屉壳
   - `forge-admin-ui/src/components/flow-designer/panel/ApproverConfig.vue` 审批办理 / 表单权限 tab
   - `forge-admin-ui/src/components/flow-designer/panel/FormPermissionConfig.vue` 字段权限矩阵
   - `forge-admin-ui/src/components/flow-designer/DingFlowDesigner.vue` 钉钉式画布壳

4. **应用中心"业务流程"tab 当前结构**：
   - `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue` 子 tab：`document`（单据）+ `flow`（绑定）。
   - `BusinessFlowBindingPanel.vue` 内容：流程模型选择 / 业务记录绑定 / 变量映射 / 节点列表（**只展示，不可编辑**）/ "打开流程设计器"按钮。
   - 节点列表数据来源：`businessFlowFormAssets` + 流程模型节点结构。

### 2.3 双渲染引擎链路

1. **业务侧渲染（AiForm）**：
   - `forge-admin-ui/src/components/ai-form/AiForm.vue` —— 自研引擎，支持 schema 驱动、双列布局、字段插槽。
   - `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` —— 业务运行时列表 + 详情 + 表单一体。
   - 即将（第二轮）增加 `field-permissions` prop。

2. **流程节点动态表单（form-create）**：
   - `forge-admin-ui/src/components/flow-form-create/FlowFormCreateRenderer.vue:46,64,115,144-147` —— form-create 引擎，消费 `formJson` schema 与 `fieldPermissions`。
   - 待办分支（todo.vue:297-302）：业务托管表单走 AiForm，节点动态表单走 FlowFormCreateRenderer。
   - 流程独立设计：`/flow/design` 节点 `formJson` 由 BPMN 节点属性承载，与业务对象解耦。

3. **流程通用表单管理**：
   - `forge-admin-ui/src/views/flow/form.vue` —— 通用表单 CRUD，schema 也是 form-create 格式。
   - `forge-admin-ui/src/views/flow/template.vue` —— 模板含表单字段。

4. **AiForm 与 form-create schema 差异**：
   - AiForm schema：`[{code, label, type, required, options, ...}]` 扁平字段数组。
   - form-create schema：嵌套 JSON（含 layout、rule、validate）。
   - 迁移核心：需要一层 schema 适配器 / 或重新建模 form-create 资产。

### 2.4 现有实现的合理部分

- 第二轮已经把"绑定业务对象"的待办切到 AiForm，AiForm 字段权限消费已就绪 —— 本轮可以直接复用作为底座。
- 应用中心"业务流程"tab 节点列表的数据流（`businessFlowFormAssets` + 模型节点）已经存在，本轮只需把"只展示"升级为"就地编辑"。
- `NodeConfigDrawer` / `ApproverConfig` / `FormPermissionConfig` 三个组件本身已经做了流程设计器侧的节点配置，本轮可以把它们封装成独立组件，供应用中心嵌入。

### 2.5 发现与风险

- **导航重组牵动顶部布局 + 路由表 + 菜单数据 + 权限角色**，影响面比"加按钮"大；需要先冻结 IA（信息架构）再动代码。
- **应用中心嵌入节点抽屉**会导致两处入口同时编辑同一个 BPMN 节点配置（应用中心 + 流程设计器），保存契约必须收敛到一份（建议都通过 `BusinessFlowAppConfigService` / Flow API 写 BPMN 节点属性）。
- **画布预览**：应用中心嵌入是否需要画布缩略图？如果用户在应用中心不看画布只配节点，需要一种"节点拓扑列表 / 步骤条"替代品。
- **form-create 下线影响面**：流程独立设计器的节点 `formJson` 全部基于 form-create；下线需要写 schema 迁移脚本 + 兼容期共存；存量流程模型的 `formJson` 不能丢。
- **schema 迁移精度**：form-create 支持 layout、grid、自定义组件，AiForm schema 不一定 1:1 对齐；迁移期可能出现"老流程渲染不一致"。
- **快捷动作（同意/驳回）批量操作**当前依赖 todo.vue 的实现；导航重组后路由保留兼容是关键。
- **节点配置在应用中心就地完成后**，`/flow/design` 入口仍然存在，必须明确"两个入口都能配，且最后一次保存为准"。否则会产生数据竞态。

## 3. 功能点

> 按子迭代分组（A/B/C）；每个子迭代结束都可独立发布。

### 3.A 子迭代 A — "我的工作台"产品概念

- [ ] **A1 顶部导航重组**：顶部从"应用中心 / 能力中心"二项扩展为"应用中心 / 我的工作台 / 能力中心"三项。我的工作台为新顶级菜单，进入后默认指向"我的待办"。
- [ ] **A2 工作台子菜单**：左侧子菜单含"我的待办 / 我的已办 / 我发起的 / 抄送我"，并预留"我的草稿（待定）"占位。
- [ ] **A3 待办列表搬家**：`/flow/todo` `/flow/done` `/flow/started` `/flow/cc` 在路由层保留兼容（直接访问仍可用），同时新增 `/workspace/todo` 等路由，菜单只显示新路由。
- [ ] **A4 应用中心 → 工作台跳转**：用户在应用中心提交一单后，顶部出现"已提交 · 查看流转"toast，点击直达 `/workspace/started`。
- [ ] **A5 工作台聚合首页**：`/workspace` 默认页展示"今日待办数 / 本周已办数 / 我发起的进行中数 / 抄送我的数"四个卡片，点击进入子页。
- [ ] **A6 应用中心顶部状态徽标**：顶部"我的工作台"按钮显示未读待办数（红点 + 数字），让用户在应用中心也能感知待办。

### 3.B 子迭代 B — 应用中心节点就地配置

- [ ] **B1 抽取共享节点配置抽屉组件**：把 `NodeConfigDrawer` / `ApproverConfig` / `FormPermissionConfig` 抽成无 BPMN 画布依赖的独立组件 `FlowNodeInlineConfig.vue`，输入 `modelKey + nodeId + businessObjectCode`，输出节点配置变更事件。
- [ ] **B2 应用中心节点列表升级**：`BusinessFlowBindingPanel.vue` 中的节点列表从"只展示"升级为可点击；选中节点后右侧抽屉就地打开 `FlowNodeInlineConfig`，编辑审批办理 / 表单权限 / 高级设置。
- [ ] **B3 节点拓扑步骤条**：应用中心节点列表替换为"步骤条"或"垂直时间线"样式，展示节点顺序、节点类型（人工 / 自动 / **网关**）、当前绑定的表单资产摘要。**网关节点显式渲染为菱形节点**，与人工节点视觉区分。
- [ ] **B4 流程设计器入口保留**：应用中心"业务流程"tab 顶部仍保留"打开完整流程设计器"按钮，跳 `/flow/design?source=appCenter`；用户需要画 BPMN / 编辑条件线时使用。
- [ ] **B5 保存契约收敛**：应用中心就地编辑节点保存调用与流程设计器相同的 Flow API（直接修改 BPMN 节点属性），避免数据双写。
- [ ] **B6 并发编辑保护**：同一节点同时被多端编辑时，最后保存方提示"该节点已被他人修改，是否覆盖"；通过版本号 / updateTime 比较。
- [ ] **B7 应用中心"业务流程"tab 重排**：上方步骤"基本配置 → 节点配置 → 变量映射 → 发布检查"作为视觉骨架，让业务管理员明确流程配置的完成度。
- [ ] **B8 网关节点业务化就地配置**（高频条件分支专项）：
   - 步骤条同时展示**人工任务节点 + 网关节点**（exclusiveGateway / inclusiveGateway 优先；parallelGateway 仅展示不可编辑）。
   - 网关节点抽屉提供"业务化条件配置"：选择来源字段、运算符、目标值、流转目标节点；隐藏 BPMN 表达式（`${approvalResult == 'reject'}` 等）到"高级设置"折叠区。
   - 内置"快捷预设"：同意路由 / 驳回路由 / 退回上一步 / 终止流程 四个常用模式一键应用。
   - 网关节点配置保存到 BPMN 条件线（`<sequenceFlow>` 的 `<conditionExpression>`），与完整流程设计器保存契约一致。
   - 复杂网关（含多分支、复合表达式）显示"该网关结构复杂，请在完整流程设计器中编辑"并禁用就地配置；判定逻辑由后端 `BusinessFlowService.isInlineEditable(gatewayNode)` 给出。

### 3.C 子迭代 C — 渲染引擎收敛到 AiForm

- [ ] **C1 AiForm 兼容 form-create schema**：新增 schema 适配层 `formCreateToAiSchema(formJson)`，把 form-create 嵌套 JSON 转为 AiForm 扁平 schema；不支持转换的字段（自定义组件）保留 raw JSON，AiForm 通过"未知字段插槽"兜底。
- [ ] **C2 流程节点动态表单切换 AiForm**：`todo.vue` 中 `FlowFormCreateRenderer` 替换为 `AiForm + 适配层`；保留 `FlowFormCreateRenderer` 作为兼容路径（feature flag 控制），仅在 C7 之前过渡使用。
- [ ] **C3 流程独立设计器的表单编辑**：`/flow/form.vue` / `/flow/template.vue` 表单编辑器从 form-create 改为 AiForm 表单设计器（复用业务对象设计器的 `form` 面板）。
- [ ] **C4 schema 迁移工具**：提供后端工具 `FormCreateSchemaMigrationJob`（一次性 / 可重入 / 支持 dry-run / 分批离线），把存量 `ai_flow_model_node.form_json` 转换为 AiForm schema 并落库到新列 `form_schema`；旧列保留作为兜底，直至 C7 清理后才能删除。
- [ ] **C5 form-create 标记 deprecated（过渡步骤）**：保留代码但加 `@deprecated` 注释 + 编译期警告；新流程不允许选择 form-create 模式；为 C7 清理做准备。
- [ ] **C6 渲染一致性回归**：业务待办 / 独立流程待办 / 业务运行时表单 / 流程节点动态表单四处视觉一致（用相同的 AiForm 主题）。
- [ ] **C7 form-create 代码完全清理**（用户确认本轮完成）：
   - 前置条件：C4 迁移工具在生产环境跑完，所有租户 `form_schema` 列填充完毕，feature flag `flow.form.engine=ai` 全量打开 24h 无回滚。
   - 删除 `forge-admin-ui/src/components/flow-form-create/FlowFormCreateRenderer.vue` 与相关 form-create 组件目录。
   - 移除 `form-create-designer` 依赖（如有）：`forge-admin-ui/package.json` 卸载 form-create 相关包。
   - 删除 feature flag `flow.form.engine`（不再需要双引擎切换）。
   - 后端 `ai_flow_model_node.form_json` 字段保留**一个大版本**作为最终降级兜底，下下轮变更再 DROP COLUMN。
   - 提供数据回滚预案：若清理后发现历史数据问题，从 `form_json` 列即时重建 `form_schema`。
   - 此 Task 必须在 C1-C6 全部稳定运行且生产无回滚后才能执行；建议作为子迭代 C 的**最后一个 Task** 单独发布。

## 4. 业务规则

### 通用

1. 顶部三级导航顺序固定为：`应用中心 / 我的工作台 / 能力中心`。其中"能力中心"对普通业务用户不显示（`requireRole=['superadmin']`，沿用第二轮决策）。
2. 待办未读徽标数 = 我的待办列表条数（状态 `pending`），点击徽标进入 `/workspace/todo`。
3. 路由兼容：所有旧 `/flow/todo` `/flow/done` `/flow/started` `/flow/cc` 仍可直接访问，路由表保留但菜单不显示。

### 节点就地配置

4. 应用中心就地编辑保存与流程设计器保存共享同一份 BPMN 节点配置；以最后保存时间为准。
5. 应用中心步骤条展示节点类型：**人工任务节点（可编辑）+ 网关节点（受限可编辑，见规则 12-14）**；子流程 / 调用活动 / 脚本任务等高级节点仅展示，必须在完整流程设计器中编辑。
6. 应用中心节点列表 / 步骤条对"人工任务节点"展示就地配置抽屉；自动节点（脚本任务 / 服务任务）显示只读说明并提示"请在完整流程设计器中编辑"。
7. 并发编辑提示：抽屉打开时 fetch 节点 `updateTime`；保存时 server 校验 `updateTime` 不匹配则返回 409 + 提示。
8. "打开完整流程设计器"按钮位于应用中心"业务流程"tab 顶部右侧，附文案"高级编辑 / 调整拓扑"。
9. **网关节点就地编辑范围**：仅 `exclusiveGateway`（排他网关）与 `inclusiveGateway`（包容网关）支持就地编辑；`parallelGateway`（并行网关）只展示不可编辑（没有条件可配）；事件网关 / 复杂网关在完整设计器中编辑。
10. **网关就地编辑 UI 契约**：业务化条件配置（来源字段 / 运算符 / 目标值 / 流转目标节点）作为主表单；BPMN 表达式作为"高级设置"折叠区可选编辑；保存时优先生成业务化条件对应的 `<conditionExpression xsi:type="tFormalExpression">` 写回 BPMN。
11. **网关复杂度判定**：后端 `isInlineEditable(gatewayNode)` 判定规则 = "出向条件全部由 (字段 + 简单运算符 + 字面值) 构成 且 分支数 ≤ 5"；超出范围则就地编辑禁用，引导跳完整设计器。

### 渲染引擎收敛（注：受网关规则插入，本节编号顺延为 12 起）

12. AiForm 适配层 `formCreateToAiSchema` 必须支持以下 form-create 字段类型：input / textarea / number / select / radio / checkbox / date / datetime / switch / upload；不支持的类型保留 raw JSON，渲染时显示"该字段需要在流程独立设计器中编辑"。
13. 切换 AiForm 渲染的优先级：`AiForm + 适配层 (默认) > FlowFormCreateRenderer (feature flag 兜底)`；feature flag 名：`flow.form.engine=ai|formCreate`，默认 `ai`。**C7 完成后 feature flag 被移除**。
14. 迁移工具 `FormCreateSchemaMigrationJob` 必须可重入 + 幂等 + 支持 dry-run + 分批离线模式；迁移失败的节点保留原状态并记录到 `ai_flow_schema_migration_log`。
15. 业务对象设计器"表单设计"面板（AiForm 表单设计器）和 `/flow/form` 表单设计器复用同一组件，避免双重表单设计器。
16. **form-create 清理前置条件**：必须满足 (a) 全量租户 `form_schema` 列已填充 (b) feature flag `flow.form.engine=ai` 在生产全量开启至少 24 小时 (c) 监控指标（待办表单渲染错误率、字段权限失效率）无异常。任一条件不满足时，C7 不得执行。
17. **数据兜底保留期**：C7 清理代码后，`ai_flow_model_node.form_json` 列保留一个大版本作为最终兜底，期间若需回滚，从该列重建 `form_schema`；下下轮变更再 DROP COLUMN。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
| --- | --- | --- | --- |
| 新增 | `ai_flow_model_node` | 增列 `form_schema TEXT NULL` | AiForm schema 落库列，旧 `form_json` 保留兜底；C7 清理后保留**一个大版本**作为最终兜底。 |
| 新增 | `ai_flow_schema_migration_log` | (id, model_key, node_id, status, message, migrated_at, dry_run) | 迁移工具日志表，支持 dry_run 标志区分演练与实际。 |
| 可选 | `ai_flow_model_node` | 增列 `update_time` 索引 | 支持并发编辑校验。 |
| 新增 | `ai_user_setting` | key=`workspace.default_page` (value=summary|todo) | 工作台默认页用户偏好。 |
| 新增（B8）| `ai_flow_model_node` | 增列 `gateway_inline_config TEXT NULL` | 网关业务化条件配置缓存（避免每次解析 BPMN 表达式）；不强制使用，BPMN `<conditionExpression>` 仍是权威源。 |

首期 Flyway 脚本：
- `V1.0.85__add_flow_model_node_form_schema.sql`
- `V1.0.86__create_flow_schema_migration_log.sql`
- `V1.0.87__add_user_setting_workspace_default_page.sql`
- `V1.0.88__add_flow_model_node_gateway_inline_config.sql`

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
| --- | --- | --- | --- |
| 新增 | `/ai/workspace/summary` | GET | 工作台首页四个聚合数（待办 / 已办本周 / 我发起的进行中 / 抄送我未读）。 |
| 新增 | `/ai/workspace/todo-count` | GET | 顶部徽标轮询接口（轻量，仅返回数字）。 |
| 新增 | `/ai/user/setting/workspace-default-page` | GET/PUT | 用户偏好：进入 `/workspace` 时默认页（summary / todo），存于 `ai_user_setting`。 |
| 新增 | `/ai/business/flow/node/{modelKey}/{nodeId}/config` | GET | 节点配置详情（应用中心就地配置抽屉用，含 nodeType 标记是否网关）。 |
| 新增 | `/ai/business/flow/node/{modelKey}/{nodeId}/config` | PUT | 保存节点配置（含 `expectedUpdateTime` 用于并发校验）。 |
| 新增 | `/ai/business/flow/model/{modelKey}/inline-nodes` | GET | 应用中心步骤条数据（人工任务节点 + 网关节点摘要，含 `inlineEditable` 标志）。 |
| 新增 | `/ai/business/flow/gateway/{modelKey}/{nodeId}/config` | GET | 网关节点业务化条件配置详情（出向条件列表 + 流转目标 + isInlineEditable）。 |
| 新增 | `/ai/business/flow/gateway/{modelKey}/{nodeId}/config` | PUT | 保存网关业务化条件（含 `expectedUpdateTime`）。 |
| 新增 | `/ai/business/flow/gateway/presets` | GET | 网关条件预设列表（同意/驳回/退回/终止）。 |
| 新增 | `/ai/business/flow/schema/migration/{modelKey}` | POST | 触发单个流程模型的 schema 迁移（管理员，支持 dryRun 参数）。 |
| 新增 | `/ai/business/flow/schema/migration` | POST | 批量触发全部模型 schema 迁移（运维，支持 dryRun 与 batchSize）。 |
| 删除（C7） | `/ai/flow/form-create/preview` 等 | — | form-create 相关接口若存在，C7 阶段删除。 |

## 7. 影响范围

- 后端：
  - `forge-plugin-generator`：`BusinessFlowService` 新增节点就地配置 API、迁移服务、工作台聚合服务。
  - `forge-plugin-flow`：`FlowModelNodeMapper` 增列 `form_schema`；并发编辑校验。
  - 新增 `WorkspaceController` / `WorkspaceService`。
  - 迁移工具：`FormCreateSchemaMigrationJob`。

- 前端：
  - 新建 `forge-admin-ui/src/views/workspace/` 目录（index、todo、done、started、cc、summary）。
  - 顶部导航组件改造（`AppHeader.vue` 待核对路径）。
  - `forge-admin-ui/src/router/index.js` 增加 `/workspace/*` 路由组。
  - 新建 `forge-admin-ui/src/components/flow-inline/FlowNodeInlineConfig.vue`（节点就地配置抽屉壳）+ `FlowNodeStepList.vue`（步骤条）。
  - `BusinessFlowBindingPanel.vue` 嵌入步骤条 + 就地抽屉。
  - `AiForm.vue` 增加 form-create schema 适配层。
  - `todo.vue` 渲染分支替换。
  - `flow/form.vue` `flow/template.vue` 表单设计器替换。

- 数据库：新增 2 张表结构变更 + 1 个索引。

## 8. 风险与关注点

> ⚠️ 本变更涉及流程节点配置数据写入路径、表单 schema 迁移、form-create 代码清理，必须重点验证。

- **导航重组兼容性**：所有现有 `/flow/todo` 等链接（包括邮件通知、消息中心、移动端）必须可访问；后端 redirect 兜底。
- **节点配置双写竞态**：应用中心 + 流程设计器同时编辑同一节点时必须有版本校验，否则覆盖丢失。
- **网关配置语义一致**（B8 重点）：业务化条件 → BPMN `<conditionExpression>` 的双向转换必须语义等价；保存后立即回读对比，发现偏差立即报错。
- **网关复杂度判定边界**：`isInlineEditable` 误判会让用户在就地配置看不到复杂网关；建议给出"看不到的网关"列表，而非完全隐藏。
- **schema 迁移精度**：form-create 自定义组件（如手写签名、附件上传插件）AiForm 可能无对应实现，迁移期"未知字段"必须明确提示，不能静默丢字段。
- **feature flag 灰度**：`flow.form.engine` 切换必须支持租户级灰度，先 1 个租户验证再全量。
- **迁移可重入**：`FormCreateSchemaMigrationJob` 必须支持重复执行不副作用、dry-run 模式、批次大小可调；迁移失败可重试。
- **C7 清理前置硬条件**：必须确认全量迁移完成 + feature flag 全量开启 24h 无回滚 + 监控指标正常，三者任一不满足都不得执行 C7；建议自动化前置检查脚本。
- **C7 数据回滚预案**：清理代码后，`form_json` 保留一个大版本作兜底；必须演练"从 form_json 即时重建 form_schema"路径。
- **节点就地配置 UX**：步骤条对长流程（>20 节点）必须有滚动 + 搜索能力，否则不可用。
- **应用中心嵌入抽屉的弹层层级**：与原有抽屉、Modal 的 z-index 冲突需要全局规划。
- **工作台首页轮询**：徽标数轮询频率不能过高（建议 30s），避免压库；页面隐藏时暂停。
- **菜单权限边界**：第二轮已确认"复用 superadmin"，本轮工作台仅普通用户可见即可，能力中心继续 superadmin。
- **用户偏好默认页**：偏好读取失败时回退到 summary；不能因偏好接口异常导致工作台白屏。

## 8.5 测试策略

- **测试范围**：
  - 后端单元/集成：工作台聚合服务、节点就地保存（含并发校验）、schema 迁移工具。
  - 前端静态和构建：工作台路由、节点就地抽屉、AiForm 适配层 unit、步骤条交互。
  - 端到端：以采购单审批 + 任意独立审批流程作为试金石跑通完整链路。

- **覆盖率目标**：
  - 工作台聚合首页：4 个聚合数各覆盖一次。
  - 用户偏好默认页：summary / todo 两值各覆盖一次 + 偏好接口异常回退覆盖。
  - 节点就地配置：保存成功 / 并发冲突 409 / 自动节点不可编辑 三档。
  - **网关就地配置**（B8）：同意/驳回/退回/终止 4 个快捷预设各一档 + 自定义条件一档 + 复杂网关引导跳完整设计器一档 + 业务化 ↔ BPMN 表达式双向转换一档。
  - schema 迁移：input / select / 日期 三种基础控件迁移；未知组件保留 raw；迁移幂等；迁移失败可重试；dry-run 模式不写库。
  - **C7 清理前置检查**：自动化前置检查脚本覆盖三个硬条件；缺一不可。
  - **C7 回滚预案演练**：在测试环境执行"从 form_json 即时重建 form_schema"一次。
  - 渲染一致性回归：业务待办 / 独立流程待办 / 业务运行时 视觉对比（截图回归）。

- **独立 Test Spec**：是，见 `test-spec.md`。

## 9. 待澄清

- [x] **顶部导航是否合并某些入口**：**保持并列**（应用中心 = 配置 / 工作台 = 日常），不合并。（2026-06-29 用户确认）
- [x] **节点就地配置是否支持网关编辑**：**支持**。新增 B8 网关业务化就地配置（exclusiveGateway / inclusiveGateway）+ 快捷预设（同意/驳回/退回/终止）+ 复杂网关引导跳完整设计器。（2026-06-29 用户确认）
- [x] **schema 迁移落地策略**：**分批离线迁移 + feature flag**（C4 工具支持 dry-run / 批次大小 / 重入；feature flag `flow.form.engine`）。（2026-06-29 用户确认）
- [x] **form-create 完全下线时机**：**本轮清理**。新增 C7 代码清理 Task，必须满足前置条件（全量迁移完成 + feature flag 全量开启 24h 无回滚 + 监控指标正常）后才能执行；`form_json` 列保留一个大版本作为最终兜底。（2026-06-29 用户确认）
- [x] **聚合首页是否替代待办列表作为默认页**：**先到 summary，用户偏好可记忆**。新增 `/ai/user/setting/workspace-default-page` 接口与 `ai_user_setting` key=`workspace.default_page`。（2026-06-29 用户确认）
- [x] **工作台是否承载我发起 / 抄送我之外的对象**：**本轮仅 4 项 + summary**，我的草稿 / 我的消息推到下一轮变更。（2026-06-29 用户确认）

> HARD-GATE 已通过。

## 10. 技术决策

1. **三个子迭代独立交付**：A / B / C 任一可单独发布；用户可按业务节奏分批，技术上不强耦合。
2. **AiForm 作为统一渲染引擎**：不引入第三方表单引擎（如 vue-form-builder），自研 AiForm 已经能满足扁平 schema 90% 场景；嵌套 / 自定义组件用插槽兜底。
3. **schema 迁移用新列而非原地改写**：`form_schema` 新列保存 AiForm schema，`form_json` 保留作 form-create 兜底；切换通过 feature flag。
4. **节点就地配置复用 BPMN 数据**：不新建"应用中心节点配置表"，避免数据所有权混乱；统一写 BPMN 节点属性。
5. **并发编辑使用乐观锁**：`updateTime` / `version` 字段在保存时校验；冲突时由用户决定覆盖或合并。
6. **顶部导航重组而非新增 Tab**：直接改顶级 IA 结构，不在某个二级页面里塞工作台 Tab。
7. **工作台首页采用聚合卡片**：避免在首页放大量数据列表（性能 + 决策成本）。
8. **路由兼容期保留 2 个大版本**：`/flow/todo` 等旧路由保留 2 个大版本周期后移除，给老链接 / 通知充分迁移时间。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
| --- | --- | --- | --- |
| Task 0 | propose | `spec.md`, `tasks.md`, `test-spec.md`, `execution-log.md` | SDD 提案，未涉及业务代码。 |
| 子迭代 A | pending | — | 我的工作台导航 + 聚合首页 + 用户偏好默认页。 |
| 子迭代 B | pending | — | 应用中心节点就地配置（含网关 B8）。 |
| 子迭代 C | pending | — | 渲染引擎收敛到 AiForm（含 C7 form-create 完全清理）。 |

## 12. 审查结论

尚未进入 `/review`。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-06-29
- **确认人**：用户
- **确认内容**：
  - §9 六个待澄清项：5 项按默认结论 accept；其中 2 项调整：
    - **节点就地配置支持网关编辑**（新增 B8 + 业务规则 9-11 + 网关接口）。
    - **form-create 本轮清理**（新增 C7 + 业务规则 16-17 + 数据兜底保留期）。
  - 子迭代发布顺序：**A → B → C**（含 C7 清理）；A 单独发布，B/C 视上线后反馈再决定是否并行。
  - **保留 `/flow/*` 旧路由 2 个大版本兼容期**，不删除。
  - **本轮仅完成 SDD 提案**，暂不进入编码（`/apply` 待后续指令）。
