# 变更日志 — 业务应用与流程模块体验整合

> 记录决策、踩坑和知识发现。知识飞轮的输入。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
| --- | --- | --- | --- |
| 2026-06-29 | propose | 用户提出 7 个采购单审批样例的具体痛点，要求把低代码应用与流程模块从用户视角整合 | 输入来自实测：流程名英文 key、技术标签、设计器无业务上下文、资产下拉、通用表单割裂、字段权限不生效、双渲染引擎 |
| 2026-06-29 | propose | 创建变更目录 `code-copilot/changes/unified-app-flow-experience/`，生成 `spec.md` / `tasks.md` / `test-spec.md` / `execution-log.md` | 关联 `unified-business-flow-app-config`、`business-flow-lowcode-integration`、`lowcode-app-full-loop-optimization` |
| 2026-06-29 | propose | HARD-GATE 三项确认完成（复用 superadmin / 业务摘要混合策略同意 / P0 单独发布同意） | 用户指示"先不用写代码"，本轮停在 propose 阶段，待后续 `/apply` 指令 |
| 2026-06-29 | apply | 完成 Task 1-7：字段权限前端生效、列表中文名、移除技术标签、流程模型反查、业务上下文注入、节点资产卡片化、代码应用字段只读面板 | P0/P1 已落地。 |
| 2026-06-29 | apply | 部分完成 Task 8：`/flow/form`、`/flow/template` 增加业务应用分流提示 | 菜单按角色隐藏未落地，原因见 Spec-Code 偏差。 |
| 2026-06-29 | verify | 执行 `git diff --check` | 通过，无空白错误。 |
| 2026-06-29 | verify | 执行后端编译：`env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 首次未切 JDK 17 失败：`无效的目标发行版: 17`；切换 JDK 17 后 `BUILD SUCCESS`。 |
| 2026-06-29 | verify | 执行前端构建：`/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build"` | 通过，构建耗时约 3m46s；仅有既有 chunking/CSS warning。 |
| 2026-06-29 | verify | 执行旧路由残留搜索：`rg "/app-center/object-designer|object-designer/sample_purchase_order" forge-admin-ui/src forge-server -g '*.*'` | 通过；仅剩 `router/index.js` 对旧组件文件的兼容 import，未发现旧 URL 使用。 |
| 2026-06-29 | fix | 补修用户反馈的 4 类问题：流程列表业务摘要、动态表单字段权限/样式、代码业务页标准上下文钩子、暂存按钮与变量映射文案 | 列表后端新增业务展示 SPI 与采购单摘要批量查询；`FlowFormCreateRenderer` 对齐权限三态和栅格样式；代码业务页通过 `useBusinessTaskFormContext` 按 `taskId` 拉取字段权限；变量映射改成业务语言。 |
| 2026-06-29 | verify | 执行增量后端编译：`env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-flow/forge-flow-client,forge-framework/forge-plugin-parent/forge-plugin-generator,forge-business/forge-business-core -am compile -DskipTests` | 通过；覆盖新增 flow-client SPI、generator 业务流程服务、采购单 Provider 和 business-core。 |
| 2026-06-29 | verify | 执行前端构建：`/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build"` | 通过，构建耗时约 1m28s；仅有既有 dynamic/static import chunking、CSS `//` 注释、组件命名冲突 warning。 |
| 2026-06-29 | verify | 执行 `forge-plugin-flow` 单模块编译：`env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am compile -DskipTests` | 未通过；失败集中在既有 `FlowFormServiceImpl` 对 `FlowForm` / `FlowFormVersion` / `FormFieldCatalogItemDTO` Lombok getter/setter 的调用，以及 `log.warn` 解析到 MyBatis `Log` 签名。`clean compile` 和显式 `-Dmaven.compiler.proc=full` 复跑结果一致。 |
| 2026-06-29 | verify | 执行 `git diff --check` | 通过，无空白错误。 |
| 2026-06-29 | fix | 修复流程设计器字段权限矩阵“可见 / 可编辑点不了” | 根因是同一权限对象同时存在旧键 `visible/editable` 和新键 `readable/writable` 时，归一化优先读取旧键，用户点击更新的新键会被旧值覆盖；已改为新键优先并在更新时同步双键。 |
| 2026-06-29 | verify | 执行字段权限矩阵定向 Vitest：`/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm --dir forge-admin-ui exec vitest run src/components/flow-designer/panel/__tests__/FormPermissionConfig.spec.js src/components/flow-designer/converter/__tests__/user-task-parser-permissions.spec.js src/components/flow-designer/converter/__tests__/json-to-bpmn.spec.js"` | 通过；3 个测试文件、29 个用例通过。 |
| 2026-06-29 | verify | 执行前端构建：`/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build"` | 通过，构建耗时约 2m；仅有既有 dynamic/static import chunking、CSS `//` 注释、组件命名冲突 warning。 |
| 2026-06-29 | fix | 修复字段权限“第一次改了保存发布，第二次进入又没了” | 根因是节点抽屉维护 `draftNode` 草稿，字段权限变化只进入抽屉草稿；如果用户直接点顶部“保存草稿/发布部署”而没有点抽屉底部“保存”，`getXML()` 会从设计器主 JSON 序列化旧配置。已让 `DingFlowDesigner.getXML()` 在序列化前提交打开中的抽屉草稿。 |
| 2026-06-29 | verify | 执行设计器定向 Vitest：`/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm --dir forge-admin-ui exec vitest run src/components/flow-designer/__tests__/DingFlowDesigner.spec.js src/components/flow-designer/panel/__tests__/FormPermissionConfig.spec.js src/components/flow-designer/converter/__tests__/user-task-parser-permissions.spec.js src/components/flow-designer/converter/__tests__/json-to-bpmn.spec.js"` | 通过；4 个测试文件、45 个用例通过。新增用例证明未点抽屉保存时，全局 `getXML()` 仍会把字段权限写入 BPMN。 |
| 2026-06-29 | verify | 执行 `git diff --check` 和前端构建：`/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build"` | 均通过；构建耗时约 1m33s，仅有既有 dynamic/static import chunking、CSS `//` 注释、组件命名冲突 warning。 |
| 2026-06-29 | fix | 修复待办/已办审批表单运行时字段权限不生效 | 根因是运行时渲染链路各自解析字段权限：`AiForm` 不支持后端 JSON 字符串，业务上下文空数组会挡住任务节点权限，后端归一化仍旧键优先。已抽共享 `field-permissions` 工具，待办/已办页取第一个非空权限源，后端优先 `readable/writable`。 |
| 2026-06-29 | verify | 执行运行时权限定向 Vitest、generator 模块编译、`git diff --check` 和前端构建 | 均通过；Vitest 5 个文件 49 个用例通过，补充单跑字段权限工具 4 个用例通过，generator 编译 `BUILD SUCCESS`，最后一次前端构建耗时约 1m39s；仅有既有 Naive UI stub/API mock、chunking、CSS `//` 注释、组件命名冲突 warning。 |
| 2026-06-29 | fix | 修复点击节点表单资产导致下方权限配置消失 | 根因是 `ApproverConfig.handleFormAssetUpdate()` 每次收到资产选择更新都写入 `formFieldPermissions: []`。已改为按选中资产字段目录重建权限：同名字段保留已有权限，新字段补默认可见/可编辑/按表单必填。 |
| 2026-06-29 | verify | 执行资产选择权限定向 Vitest、`git diff --check` 和前端构建 | 均通过；`ApproverConfig.spec.js` + `FormPermissionConfig.spec.js` 共 5 个用例通过，前端构建耗时约 1m35s；仅有既有 chunking、CSS `//` 注释、组件命名冲突 warning。 |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
| --- | --- | --- | --- |
| 表单引擎统一节奏 | P0/P1 保留双引擎（AiForm + FlowFormCreateRenderer），让绑定业务对象的待办全部走 AiForm | 立即用 AiForm 替换 form-create | form-create 已经稳定承载独立流程动态表单，立即替换风险大；先让"业务样式"在待办里显现，逐步收敛 |
| 资产选择器复用 | 复用已存在的 `BusinessFlowFormAssetSelect.vue` 卡片组件，跨用到流程设计器节点抽屉 | 在 ApproverConfig 内重造卡片 | 已有组件已生产可用，最小化新增 |
| Provider 字段标志承载 | 通过 `field(...)` 工厂方法重载（含 `internal/systemField/description`） | 引入注解 / 新接口 | 旧 Provider 不动即兼容，迁移成本最低 |
| 反查接口轻量化 | 只返回 objectCode/objectName/codeApp/entryRoute | 返回完整 BusinessBindingDTO | 列表只需展示与跳转，性能与契约都简化 |
| 菜单弱化方式 | 前端 `requireRole` 控制可见性 + Banner 提示 | 后端权限模型扩展 | 首期保守不动权限模型；如已有 `flow_admin` 直接复用 |
| 业务摘要来源 | 低代码：`summaryExpression`；代码：`buildSummary(record)`；都缺回退到流程中文名 + 业务编号 | 强制业务对象配置 summary | 混合策略兼容存量，最低用户配置成本 |
| 流程高级菜单隐藏 | 本轮不新增前端 `requireRole` 半成品 | 在现有菜单树上直接加 `requireRole` | 当前菜单链路实际只消费后端资源的 `visible/menuStatus`，盲加字段不会生效；先保留 Banner，后续扩展后端资源/菜单权限模型再做隐藏。 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 沉淀？ |
| --- | --- | --- | --- |
| 待办列表显示流程定义 key 而非中文 | `#title` 绑了任务发起时拼的 `title` 字段，未使用已 join 的 `processName` | 抽 `getRowDisplayTitle(row)` 工具，回退链优先业务摘要/中文流程名 | 已在 Task 2 落地，后续归档时可沉淀为流程列表展示规则。 |
| 字段权限后端兜底但前端不生效 | `<AiForm>` 不消费 `field-permissions`，只有 form-create 路径消费 | `AiForm` 新增 `field-permissions` prop，按字段编码消费三态 | 已写入 `code-copilot/memory/pitfalls.md` 第 80 条。 |
| 双渲染引擎样式割裂 | 业务侧 `AiForm`、流程节点动态表单 `FlowFormCreateRenderer` 是两套引擎 | 让"绑定业务对象"的待办走 AiForm；独立流程仍可用 form-create | 代码应用字段目录只读决策已写入 `code-copilot/memory/decisions.md` 第 13 条；双引擎收敛待端到端验证后归档。 |
| 设计器拿不到业务上下文 | `model.vue` 点设计只传 `modelId`，`design.vue` 仅从 route.query 读 `businessObjectCode` | 新增反查接口 `/business-bindings`，列表预加载并注入设计器 props | 已在 Task 4-5 落地。 |
| 后端编译使用错误 JDK | 当前 shell 非 JDK 17 时 Maven 目标版本 17 编译失败 | 显式设置 `JAVA_HOME` 与 `PATH` 到 OpenJDK 17 后重跑 | 已有 memory 记录（坑点 25），本轮复用。 |
| 旧对象设计器 URL 容易被继续拼接 | 历史入口仍有 `/app-center/object-designer/:objectCode` 兼容文件 | 新入口统一用 `/app-center/object/:objectCode/designer?panel=...`，并用 `rg` 搜索旧 URL | 已写入 `code-copilot/memory/pitfalls.md` 第 81 条。 |
| 流程列表无法展示业务摘要 | `FlowTaskMapper.xml` 只返回流程/任务字段，Provider 的 `buildSummary()` 只在打开任务表单上下文时执行，列表页从未计算业务摘要 | 在 `forge-flow-client` 增加轻量 SPI，flow 插件批量收集 businessKey/objectCode/recordId，generator 侧按业务对象批量补齐 `businessObjectName/businessSummary` | 本轮落地；后续如列表数据量大，可把摘要预计算到业务主表或实例关联表。 |
| 代码业务页拿不到节点字段权限 | 外部业务页跳转只带 `taskId/businessKey` 等参数，没有统一拉取任务表单上下文 | 新增 `useBusinessTaskFormContext(taskId)`，代码业务页用后端上下文作为单一权限数据源，不在 URL 透传权限矩阵 | 本轮落地，作为代码业务页面标准接入方式。 |
| `forge-plugin-flow` 单模块编译被旧表单服务阻断 | `FlowFormServiceImpl` 编译时 Lombok getter/setter 未生效，`@Slf4j` 未生成后 `log` 被解析为 MyBatis-Plus `ServiceImpl` 的 `org.apache.ibatis.logging.Log` | 本轮不手工补大量 getter/setter，记录为既有构建阻断；后续应单独修 `FlowFormServiceImpl`/编译器处理器配置 | 未沉淀到 memory，待单独修复后再归档。 |
| 字段权限矩阵可见/可编辑点击后回弹 | 权限对象同时保存 `visible/editable` 和 `readable/writable`，归一化读取旧键优先，点击事件只改新键，下一次 computed 又被旧值覆盖 | 归一化统一新键优先；`FormPermissionConfig.update()` 更新 `readable/writable/required` 时同步写回 `visible/editable`，BPMN parser/writer 同步采用同一规则 | 已写入 `code-copilot/memory/pitfalls.md` 第 82 条。 |
| 字段权限顶部保存/发布后丢失 | 节点抽屉是草稿态，顶部保存/发布直接取设计器主 JSON 转 XML，未包含打开中的抽屉草稿 | `NodeConfigDrawer` 暴露 `commitDraft()`；`DingFlowDesigner.getXML()` 在转换 XML 前调用 `commitOpenDrawerDraft()`，确保全局保存/发布包含当前抽屉改动 | 已写入 `code-copilot/memory/pitfalls.md` 第 83 条。 |
| 待办审批表单字段权限保存了但渲染不生效 | 运行时消费链路分叉：`AiForm` 只接数组不解析 JSON 字符串，form-create / 代码页各自归一化，`todo.vue` 用空数组短路了后续权限源，后端仍旧键优先 | 前端抽 `utils/field-permissions.js` 统一解析；`AiForm` / `FlowFormCreateRenderer` / `useBusinessTaskFormContext` 复用；待办页取第一个非空权限源；后端归一化优先 `readable/writable` 并在不可写时清掉 required | 已写入 `code-copilot/memory/pitfalls.md` 第 84 条。 |
| 点击节点表单资产后权限配置消失 | 资产选择事件被当成整块节点表单配置替换，`handleFormAssetUpdate()` 固定写入 `formFieldPermissions: []` | 选中资产时用资产字段目录重建权限矩阵，同名字段保留历史配置，新字段补默认权限；只有清除绑定时才清空权限 | 已写入 `code-copilot/memory/pitfalls.md` 第 85 条。 |

## 知识发现

> 每个 task 后实时记录，/archive 时逐条确认沉淀到 `code-copilot/memory/` 或 `knowledge/`

- [x] **关键词**: `processName fallback chain` — 列表显示中文名的回退链：businessSummary > businessObjectName > processName > processTitle > modelName > taskName > processDefinitionName > processDefinitionKey > '-'
- [x] **关键词**: `AiForm field-permissions contract` — 三态语义：visible/editable/required，未传时保持原行为（schema 即权限），传入时取交集（editable 覆盖、required 取 OR）
- [x] **关键词**: `BusinessFlowFormAssetSelect cross-use` — 卡片选择器组件已在应用中心生产可用，节点抽屉直接复用即可，无需重造
- [x] **关键词**: `code provider internal field flag` — `field(..., internal, systemField, description)` 工厂方法重载，旧 Provider 默认 internal=false
- [x] **关键词**: `flow model -> business-bindings reverse lookup` — 反查接口轻量化只返回摘要 VO，按 `flow_model_key` 索引查询；如 QPS 高再补索引
- [x] **关键词**: `form permission key precedence` — 字段权限矩阵兼容旧 `visible/editable` 与新 `readable/writable` 时必须新键优先，更新时双写，避免点击后被旧值回滚
- [x] **关键词**: `designer drawer draft commit before getXML` — 流程设计器全局保存/发布前必须提交打开中的节点抽屉草稿，否则用户在抽屉内刚改的字段权限不会进入 BPMN XML
- [x] **关键词**: `runtime field permission normalization` — 待办/已办运行时字段权限必须走共享归一化，支持数组/JSON 字符串/fields 对象，取第一个非空权限源，并优先 `readable/writable`
- [x] **关键词**: `form asset selection preserves permissions` — 节点表单资产选择不能清空 `formFieldPermissions`，应按资产字段目录合并已有权限并补齐新字段默认权限

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
| --- | --- | --- | --- |
| 菜单按角色隐藏 | `/flow/form`、`/flow/template`、`/flow/model`、`/flow/monitor` 普通用户菜单不显示，`superadmin` 可见 | 当前前端菜单处理只支持后端资源树的 `visible/menuStatus`，没有 `requireRole` 生效链路；本轮仅给 `/flow/form`、`/flow/template` 加 Banner | 记录为 Task 8 partial。后续需扩展后端资源/菜单模型或菜单解析层后再实现角色隐藏。 |
| 采购单端到端验证 | 启动 admin/flow/ui 后跑完整申请、审批、驳回修改、已办只读链路 | 本轮未启动服务，未执行 E2E | 记录为 Task 9 partial。后续 `/test` 阶段按 `test-spec.md` §6 跑完整链路。 |
| `forge-plugin-flow` 完整编译 | 本轮改动涉及 `FlowTaskServiceImpl` / `FlowCcServiceImpl`，理想状态应通过 flow 插件单模块编译 | 编译在既有 `FlowFormServiceImpl` 处失败，错误与本轮列表展示 SPI 无直接关系；业务侧增量模块与前端构建已通过 | 记录为验证警告。若发布流程要求全量 `forge-plugin-flow` 绿灯，需要先单独修复 Flow 表单服务 Lombok/日志问题。 |

> /apply 阶段如发现偏差，按此格式记录并同步更新 spec.md。

## 代码质量备忘

- `AiForm.vue` 新增字段权限消费时，建议同时抽取 `useFieldPermissions(schema, permissions)` composable，避免组件内逻辑膨胀。
- `getRowDisplayTitle` 抽到 `views/flow/utils/processDisplay.js`，todo/done/started/cc 四处复用，保持单一回退源。
- 后端 `BusinessFlowFormAssetVO` 字段扩展记得回归 `unified-business-flow-app-config` 已有调用点（应用中心侧、节点抽屉侧）的序列化兼容。
- 业务摘要 `buildSummary(record)` 钩子在 `BusinessCodeFormProvider` 接口扩展时，必须给默认实现（default 方法返回 null），避免破坏存量 Provider。
- 代码 Provider `id` 字段标 internal 时，注意"业务编号字段"（如采购单的 orderNo）保留为非 internal，否则用户在表单里看不到关键标识。
