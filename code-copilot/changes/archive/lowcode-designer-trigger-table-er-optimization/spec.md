# 低代码设计器触发器、表格居中与 ER 图优化
> status: apply
> created: 2026-07-05
> complexity: 🟡中等

## 1. 背景与目标

当前低代码自动化触发器主要通过独立的“触发器配置”页面维护，用户在对象设计器中完成表单、列表、关系、流程后，需要跳出业务对象上下文再筛选对象，配置链路割裂且容易误配。低代码应用还缺少已配置对象关系的 ER 图查看入口，已有 ER 图组件未接入对象设计流程。基础表格组件虽然已有居中默认值，但低代码列表设计器生成列配置时仍显式写入 `left`，导致默认居中策略被覆盖。

目标：

- 将低代码触发器配置作为业务对象设计器的一等面板，保留独立页作为全局管理和排查入口。
- 基础表格及低代码列表生成列在未显式配置对齐时，表头和内容默认居中。
- 在对象关系设计区放开 ER 图查看能力，展示当前对象与关联对象的关系图。

## 2. 代码现状（Research Findings）

### 2.1 触发器配置

- 独立页面：`forge-admin-ui/src/views/app-center/trigger.vue`。
- API：`businessTriggerPage/createBusinessTrigger/updateBusinessTrigger/deleteBusinessTrigger/updateBusinessTriggerStatus/businessTriggerLogs/businessTriggerScenarioTemplates`。
- 编辑能力已包含触发条件、定时参数、动作配置、执行日志。
- 对象设计器：`forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue` 当前 `openTriggerConfig()` 跳转到 `/app-center/trigger?objectCode=...`。
- 设计器壳：`BusinessObjectDesignerShell.vue` 仅在更多菜单提供“配置触发器”，左侧导航没有触发器面板。

### 2.2 表格默认对齐

- `AiTable.vue` 已定义 `DEFAULT_COLUMN_ALIGN = 'center'`，并使用 `col.align || DEFAULT_COLUMN_ALIGN` 与 `titleAlign || headerAlign || columnAlign`。
- `BusinessListDesigner.vue` 的运行态预览列构造仍使用 `align: setting.align || 'left'`，会覆盖基础表格默认居中。

### 2.3 ER 图

- 已有组件：`forge-admin-ui/src/components/lowcode-builder/model/LowcodeErDiagram.vue`，支持 SVG 绘制和下载。
- 关系配置面板：`BusinessRelationDesigner.vue` 已持有当前对象字段、关系、业务对象列表和目标对象字段加载逻辑，可直接构造 ER 图模型。
- 当前 ER 图组件未在应用中心对象设计器中开放。

## 3. 功能点

- [x] **设计器内触发器配置**：对象设计器左侧新增“自动化触发器”面板，按当前对象编码锁定上下文，新增/编辑触发器默认归属当前对象。
- [x] **独立触发器页保留**：`/app-center/trigger` 继续作为全局触发器管理入口，可按业务对象/场景筛选。
- [x] **触发器闭环修复入口就地打开**：发布检查或单据闭环中的触发器修复动作，不再跳出设计器，直接切到触发器面板。
- [x] **表格默认居中**：低代码列表预览/运行列未显式设置对齐时，不再写入 `left`，交给 `AiTable` 默认居中；表头跟随内容居中。
- [x] **关系 ER 图**：关系与级联面板新增 ER 图查看入口，基于当前对象、目标对象、关系字段渲染 ER 图，并保留 SVG 下载能力。

## 4. 业务规则

- 触发器仍按现有后端模型保存，必须包含 `objectCode`，`suiteCode` 由对象上下文补齐。
- 对象设计器内嵌触发器配置时，业务单元不可切换，避免跨对象误配。
- 独立触发器页面不改变原有筛选和编辑能力。
- 表格对齐仅影响未显式配置 `align` 的列；已有显式左/右/居中配置继续生效。
- ER 图只读取设计态关系和字段，不新增数据库表、接口或持久化字段。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
| --- | --- | --- | --- |
| 无 | 无 | 无 | 本次为前端设计器入口与展示优化 |

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
| --- | --- | --- | --- |
| 无 | 无 | 无 | 复用现有低代码对象、关系、触发器接口 |

## 7. 影响范围

- `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue`
- `forge-admin-ui/src/views/app-center/trigger.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`

## 8. 风险与关注点

- `trigger.vue` 路由页改为可内嵌组件时，要避免嵌入对象设计器后仍监听路由参数导致筛选被覆盖。
- 内嵌触发器的对象锁定需要覆盖新增、筛选重置和业务单元选择控件。
- ER 图需要处理目标对象字段异步加载，字段未加载时也要至少展示对象节点和关系。
- 当前工作区已有其他未提交改动，修改 `BusinessListDesigner.vue` 时只触碰默认对齐逻辑。

## 8.5 测试策略

- **测试范围**：前端静态检查、构建、对象设计器手工烟测。
- **覆盖点**：触发器面板进入/新增默认对象/日志抽屉、独立触发器页仍可打开、列表预览列默认居中、关系 ER 图非空和下载按钮可见。
- **独立 Test Spec**：是。

## 9. 待澄清

- 无。按用户明确方向：触发器主配置入口收敛到对象设计器，独立页保留为全局管理入口。

## 10. 技术决策

- 不新增后端接口：用现有 `objectCode` 查询参数和触发器 CRUD 接口完成对象内配置。
- 不复制触发器编辑逻辑：将 `trigger.vue` 做成路由/内嵌双态组件，由对象设计器直接复用。
- ER 图复用现有 `LowcodeErDiagram.vue`，在关系设计器内构造模型数据，不另起绘图库。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
| --- | --- | --- | --- |
| Task 0 | completed | `spec.md`, `tasks.md`, `test-spec.md`, `execution-log.md` | 建立 SDD 变更记录 |
| Task 1 | completed | `trigger.vue`, `object-designer.[objectCode].vue`, `BusinessObjectDesignerShell.vue` | 触发器内嵌设计器，独立页保留 |
| Task 2 | completed | `BusinessListDesigner.vue` | 低代码列默认交给 `AiTable` 居中 |
| Task 3 | completed | `BusinessRelationDesigner.vue` | 关系面板新增 ER 图入口 |
| Task 4 | completed | `test-spec.md`, `execution-log.md` | 构建和空白检查通过 |

## 12. 审查结论

已完成实现与验证。`pnpm --dir forge-admin-ui build` 成功，`git diff --check` 通过。构建存在既有动态导入、CSS `//` 注释和 chunk size warning，未阻断本次变更。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-05
- **确认人**：用户
- **确认内容**：用户要求低代码触发器配置优化、基础表格表头和内容默认居中、低代码应用开放关系 ER 图查看，并明确采用 SDD 流程开发。
