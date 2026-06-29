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

## 知识发现

> 每个 task 后实时记录，/archive 时逐条确认沉淀到 `code-copilot/memory/` 或 `knowledge/`

- [x] **关键词**: `processName fallback chain` — 列表显示中文名的回退链：businessSummary > businessObjectName > processName > processTitle > modelName > taskName > processDefinitionName > processDefinitionKey > '-'
- [x] **关键词**: `AiForm field-permissions contract` — 三态语义：visible/editable/required，未传时保持原行为（schema 即权限），传入时取交集（editable 覆盖、required 取 OR）
- [x] **关键词**: `BusinessFlowFormAssetSelect cross-use` — 卡片选择器组件已在应用中心生产可用，节点抽屉直接复用即可，无需重造
- [x] **关键词**: `code provider internal field flag` — `field(..., internal, systemField, description)` 工厂方法重载，旧 Provider 默认 internal=false
- [x] **关键词**: `flow model -> business-bindings reverse lookup` — 反查接口轻量化只返回摘要 VO，按 `flow_model_key` 索引查询；如 QPS 高再补索引

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
| --- | --- | --- | --- |
| 菜单按角色隐藏 | `/flow/form`、`/flow/template`、`/flow/model`、`/flow/monitor` 普通用户菜单不显示，`superadmin` 可见 | 当前前端菜单处理只支持后端资源树的 `visible/menuStatus`，没有 `requireRole` 生效链路；本轮仅给 `/flow/form`、`/flow/template` 加 Banner | 记录为 Task 8 partial。后续需扩展后端资源/菜单模型或菜单解析层后再实现角色隐藏。 |
| 采购单端到端验证 | 启动 admin/flow/ui 后跑完整申请、审批、驳回修改、已办只读链路 | 本轮未启动服务，未执行 E2E | 记录为 Task 9 partial。后续 `/test` 阶段按 `test-spec.md` §6 跑完整链路。 |

> /apply 阶段如发现偏差，按此格式记录并同步更新 spec.md。

## 代码质量备忘

- `AiForm.vue` 新增字段权限消费时，建议同时抽取 `useFieldPermissions(schema, permissions)` composable，避免组件内逻辑膨胀。
- `getRowDisplayTitle` 抽到 `views/flow/utils/processDisplay.js`，todo/done/started/cc 四处复用，保持单一回退源。
- 后端 `BusinessFlowFormAssetVO` 字段扩展记得回归 `unified-business-flow-app-config` 已有调用点（应用中心侧、节点抽屉侧）的序列化兼容。
- 业务摘要 `buildSummary(record)` 钩子在 `BusinessCodeFormProvider` 接口扩展时，必须给默认实现（default 方法返回 null），避免破坏存量 Provider。
- 代码 Provider `id` 字段标 internal 时，注意"业务编号字段"（如采购单的 orderNo）保留为非 internal，否则用户在表单里看不到关键标识。
