# 流程分支条件表单规则配置优化
> status: done
> created: 2026-06-20
> complexity: 🟡中等

## 1. 背景与目标
流程模型设计器的条件分支目前只能手写 SpEL 表达式，使用门槛高且容易写错字段名。优化后，当流程配置了动态表单时，条件分支可通过表单字段、运算符和值组合生成条件表达式；仍保留高级表达式模式，兼容已有 BPMN XML 和后端 Flowable 条件表达式执行。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路
- `forge-admin-ui/src/views/flow/design.vue` 已维护 `formSchema` 和 `formFieldCatalog`，并通过 `refreshFormFieldCatalog()` 从已选表单或内嵌表单解析字段目录。
- `forge-admin-ui/src/components/flow-designer/DingFlowDesigner.vue` 负责流程画布和节点配置抽屉，目前只传 `node/outgoingEdges/nodes` 给 `NodeConfigDrawer`。
- `forge-admin-ui/src/components/flow-designer/panel/NodeConfigDrawer.vue` 通过 `CONFIG_RENDERER_MAP` 调度 `ConditionConfig.vue`。
- `forge-admin-ui/src/components/flow-designer/panel/ConditionConfig.vue` 目前只展示每条出边的 `condition` 输入框和默认分支单选。

### 2.2 现有实现
- 分支条件最终存储在 edge 的 `condition` 字符串中。
- `forge-admin-ui/src/components/flow-designer/converter/json-to-bpmn.js` 将非默认分支的 `edge.condition` 写入 `<bpmn:conditionExpression>`。
- `forge-admin-ui/src/components/flow-designer/canvas/BranchHeader.vue` 直接展示 `edge.condition` 或默认文案。

### 2.3 发现与风险
- BPMN 导出链路只消费 `edge.condition`，因此规则配置器必须生成标准表达式字符串，不能依赖额外结构。
- 动态表单字段可能不存在或尚未配置，需要给出空状态并允许继续手写表达式。
- 已有手写表达式必须可继续编辑，不能被规则模式误覆盖。

## 3. 功能点
- [x] 流程设计页将动态表单字段目录传入 DingFlowDesigner。
- [x] 节点配置抽屉将字段目录传入条件分支配置组件。
- [x] 条件分支配置支持“表单字段条件”和“高级表达式”两种模式。
- [x] 表单字段条件支持多条规则、任一/全部匹配、常用运算符和值输入，并生成 SpEL 表达式。
- [x] 统一动态表单字段目录为空时，前端从已加载表单 Schema 递归解析字段作为兜底。
- [x] 点击条件分支标签时只配置对应分支，点击网关节点时展示全部分支。
- [x] 条件规则支持删除至空状态，并同步清空该分支表达式。
- [x] 条件配置面板约束规则行、表达式预览和高级表达式输入，避免抽屉内容横向溢出。
- [x] 默认分支仍允许在设计器中配置和保留条件表达式，但导出 BPMN 时不写入 default 边条件，避免 Flowable 部署失败。
- [x] 条件规则行按字段、关系、取值三列对齐，默认分支在画布上保持默认分支标签。
- [x] 条件网关支持在配置面板继续添加多条分支，并保持一个默认分支。
- [x] 添加分支操作改到画布分支连线区域，右侧条件面板只负责条件配置。
- [x] 画布分支标签使用“条件已设 / N 条条件”摘要，不直接铺开 SpEL 表达式。
- [x] 表单字段生成的常见表达式重新打开时可回显为字段条件模式。
- [x] 流程设计支持提交人撤回权限配置，并在运行时限制非提交人或禁用配置下撤回。
- [x] 流程设计支持重复审批自动同意策略：仅首个节点需审批、仅连续审批自动同意、每个节点都需审批。
- [x] 审批意见配置文案调整为“审批意见”，节点运行时保持同意/驳回等操作必填校验。
- [x] 审批节点表单字段权限改为按流程全局动态表单字段勾选，不再手工输入字段名。
- [x] 待办动态表单按节点字段权限隐藏不可见字段、禁用只读字段，并对节点必填字段补校验。

## 4. 业务规则
- 默认分支可保留设计器草稿条件，但 Flowable 不允许 default sequenceFlow 携带 `conditionExpression`，导出 BPMN 时必须跳过默认边条件。
- 条件网关可存在多条非默认条件分支，但同一网关必须保持且仅保持一个默认分支。
- 非默认分支规则模式生成 `${...}` 格式 SpEL 表达式。
- 多条规则按“符合全部”生成 `&&`，按“符合任一”生成 `||`。
- 字符串值自动加单引号，数字和布尔值按原值生成。
- 高级表达式模式保持用户原始输入。
- 画布分支标签只展示条件状态摘要；原始表达式可通过配置抽屉继续查看和编辑，避免画布上内容过载。
- 流程级审批策略写入 BPMN process 扩展属性：`flowable:allowSubmitterWithdraw`、`flowable:autoApprovalMode`。
- `autoApprovalMode=firstOnly` 时，同一审批人在流程中已经完成过任一审批，后续再次成为当前任务审批人时自动同意。
- `autoApprovalMode=consecutive` 时，仅上一已完成审批任务和当前任务审批人相同才自动同意。
- `autoApprovalMode=none` 时，所有节点都需要人工审批。
- 表单字段权限写入用户任务 `flowable:formFieldPermissions`，运行时通过 `TaskFormInfo.formFieldPermissions` 下发到待办页。

## 5. 数据变更
| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 无 | - | - | 审批策略和字段权限存入 BPMN XML 扩展属性，不新增表结构 |

## 6. 接口变更
| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 兼容增强 | `/flow/task/form/{taskId}` | GET | `TaskFormInfo` 增加 `formFieldPermissions` 字段，用于待办动态表单权限渲染 |
| 兼容增强 | `/flow/task/withdraw` | POST | 后端按流程级 `allowSubmitterWithdraw` 和提交人身份进行撤回校验 |

## 7. 影响范围
- 流程模型设计页
- 钉钉样式流程设计器条件分支配置抽屉
- 审批节点配置抽屉的表单权限页
- 待办审批动态表单渲染
- Flowable 任务撤回与审批通过后的重复审批自动同意
- 条件分支相关前端单元测试

## 8. 风险与关注点
- 涉及流程状态流转和提交人撤回权限，已通过后端 flow 插件编译验证，未做数据库级流程实例实跑。
- 表达式生成必须兼容现有 Flowable 条件表达式。
- 条件面板视觉需保持后台工具风格，避免过度装饰。

## 8.5 测试策略
- **测试范围**：前端组件单元测试、针对性 lint/type/build 可行性验证。
- **覆盖率目标**：覆盖字段条件渲染、规则生成表达式、无字段时回退高级模式。
- **独立 Test Spec**：否，使用当前变更 execution-log 记录。

## 9. 待澄清
- 无。

## 10. 技术决策
- 不改变 BPMN 数据结构，规则配置器作为表达式生成辅助层，保存时仍写入 `edge.condition`。

## 11. 执行日志
| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 1 | 已完成 | `design.vue`, `DingFlowDesigner.vue`, `NodeConfigDrawer.vue` | 接入动态表单字段目录 |
| Task 2 | 已完成 | `ConditionConfig.vue` | 实现规则模式和高级表达式模式 |
| Task 3 | 已完成 | `ConditionConfig.spec.js`, `form-field-catalog.spec.js`, `test-spec.md`, `execution-log.md` | 补测试并完成验证 |
| Task 4 | 已完成 | `DingFlowDesigner.vue`, `NodeConfigDrawer.vue`, `ConditionConfig.vue`, `ConditionConfig.spec.js`, `DingFlowDesigner.spec.js` | 修复分支聚焦、规则删除和表达式溢出 |
| Task 5 | 已完成 | `ConditionConfig.vue`, `BranchHeader.vue`, `EdgePath.vue`, `json-to-bpmn.js`, `branch-parser.js` | 默认分支可配条件并优化规则行对齐 |
| Task 6 | 已完成 | `useFlowDesigner.js`, `ConditionConfig.vue`, `NodeConfigDrawer.vue`, `DingFlowDesigner.vue` | 支持配置面板继续添加条件分支，并保持唯一默认分支 |
| Task 7 | 已完成 | `BranchAddButton.vue`, `BranchHeader.vue`, `EdgePath.vue`, `DingFlowDesigner.vue`, `ConditionConfig.vue` | 添加分支入口移到画布，边标签摘要化，表单表达式回显规则模式 |
| Task 8 | 已完成 | `design.vue`, `DingFlowDesigner.vue`, `FormPermissionConfig.vue`, `FlowFormCreateRenderer.vue`, `todo.vue`, `FlowTaskServiceImpl.java`, `TaskFormInfo.java` | 补齐审批策略、表单字段权限和运行时执行 |

## 12. 审查结论
自检通过：前端组件测试、目标 ESLint、生产构建和 Playwright 交互验证均通过；构建存在项目既有非阻断告警。

## 13. 确认记录（HARD-GATE）
- **确认时间**：2026-06-20
- **确认人**：用户直接提出优化需求，按当前会话执行。

## 归档记录（HARD-GATE）
- **状态**：done
- **归档时间**：2026-06-27
- **归档人**：yaomd（批量归档）
- **归档路径**：code-copilot/changes/archive/2026-06-27-flow-condition-form-rules/
- **判定依据**：任务清单全部完成，execution-log 验证通过（编译/构建/lint 闭环）。
