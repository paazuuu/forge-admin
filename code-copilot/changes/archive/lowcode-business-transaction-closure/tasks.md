# 任务拆分 — 低代码业务交易闭环能力补齐
> status: apply_completed
> created: 2026-07-03
> 原则：平台能力通用；采购仓储只做验收样例；查询 SQL 写 Mapper XML；动作步骤白名单；内置数据 tenant_id=1。

## 阶段总览

| 阶段 | 目标 | 任务 |
|------|------|------|
| Phase 1 | 动作循环 | Task 1-3 |
| Phase 2 | 选择器增强 | Task 4-6 |
| Phase 3 | 数量查询 | Task 7-9 |
| Phase 4 | 详情区块最小入口 | Task 10-11 |
| Phase 5 | 验证与文档 | Task 12-13 |

## 任务清单

| Task | 名称 | 状态 | 优先级 |
|------|------|------|--------|
| Task 1 | 动作执行上下文支持循环变量 | completed | P0 |
| Task 2 | 新增 `FOREACH` 动作步骤执行器 | completed | P0 |
| Task 3 | 动作设计器提供循环步骤模板 | completed | P1 |
| Task 4 | 选择器后端支持多字段关键词查询 | completed | P0 |
| Task 5 | 选择器运行态支持动态过滤参数 | completed | P0 |
| Task 6 | 选择器配置入口优化 | completed | P1 |
| Task 7 | 数量台账查询 DTO/VO/Mapper | completed | P0 |
| Task 8 | 数量台账查询 Service/Controller | completed | P0 |
| Task 9 | 数量台账查询权限和前端 API | completed | P0 |
| Task 10 | 详情区块运行协议接入数量查询 | completed | P1 |
| Task 11 | 详情设计器最小配置入口 | completed | P1 |
| Task 12 | 定向单测和构建验证 | completed | P0 |
| Task 13 | 采购仓储低代码配置验收文档 | completed | P1 |

## Task 1: 动作执行上下文支持循环变量

**目标**：让动作步骤映射能读取 `${item.xxx}` 和 `${index}`。

**涉及文件**：
- `BusinessActionExecutionContext.java`
- `BusinessActionStepConfigHelper.java`
- `BusinessActionExecutionServiceTest.java`

**验收标准**：
- [x] 父级 record/form/context 解析不受影响。
- [x] 循环上下文可以覆盖 `item` 和 `index`，并在子步骤执行结束后恢复。
- [x] 未进入循环时读取 `item.xxx` 返回 null，不抛 NPE。

## Task 2: 新增 `FOREACH` 动作步骤执行器

**目标**：动作步骤支持遍历集合并执行子步骤。

**涉及文件**：
- `ForeachActionStepExecutor.java`
- `BusinessActionExecutionService.java`
- `BusinessActionStepDTO.java`
- `BusinessActionForeachStepExecutorTest.java`

**验收标准**：
- [x] `collectionPath` 支持 `record.details`、`formData.children.items`、`context.rows`。
- [x] `steps` 只允许已有白名单步骤，禁止递归超过 2 层。
- [x] 子步骤失败时默认回滚父事务。
- [x] 每一行步骤结果包含 `itemIndex` 和子步骤结果。

## Task 3: 动作设计器提供循环步骤模板

**目标**：降低业务配置动作循环的 JSON 成本。

**涉及文件**：
- `BusinessActionDesigner.vue`

**验收标准**：
- [x] 通用动作步骤区新增“循环明细步骤”模板按钮。
- [x] 模板生成 `FOREACH + DOMAIN_ACTION/QUANTITY` 的可编辑 JSON。
- [x] 既有数量台账步骤模板不回退。

## Task 4: 选择器后端支持多字段关键词查询

**目标**：实现物料编号/名称这类多字段 OR 搜索。

**涉及文件**：
- `BusinessRecordSelectorService.java`
- `DynamicCrudService.java` 或选择器专用 Mapper XML
- `BusinessRecordSelectorSearchParamsTest.java`

**验收标准**：
- [x] 单字段关键词兼容旧行为。
- [x] 多字段关键词按 OR 查询。
- [x] 关键词字段必须在请求字段白名单内。
- [x] 排序字段仍只能来自允许字段。

## Task 5: 选择器运行态支持动态过滤参数

**目标**：选择器可按当前仓库、项目、供应商等上下文过滤候选记录。

**涉及文件**：
- `AiFormItem.vue`
- `ChildTableEditor.vue`
- `record-selector-utils.js`
- `BusinessRecordSelectorService.java`

**验收标准**：
- [x] `searchParams` 支持 `${formData.xxx}`、`${record.xxx}`、`${row.xxx}`、`${query.xxx}`。
- [x] 空值参数不提交。
- [x] 子表选择器能读取父表单数据。

## Task 6: 选择器配置入口优化

**目标**：让字段属性面板能维护动态过滤参数。

**涉及文件**：
- `BusinessFieldPropertyPanel.vue`
- `BusinessRelationDesigner.vue`

**验收标准**：
- [x] 字段选择器配置区新增过滤参数文本入口。
- [x] 子表关系配置可维护选择器按钮、候选对象、映射和过滤参数。

## Task 7: 数量台账查询 DTO/VO/Mapper

**目标**：建立只读查询协议。

**涉及文件**：
- `BusinessQuantityQueryDTO.java`
- `BusinessQuantityBalanceVO.java`
- `BusinessQuantityLedgerVO.java`
- `BusinessQuantityLockVO.java`
- `BusinessQuantityBalanceMapper.java/xml`
- `BusinessQuantityLedgerMapper.java/xml`
- `BusinessQuantityLockMapper.java/xml`

**验收标准**：
- [x] 查询条件支持账户、数量项、维度、来源对象、来源记录和来源明细。
- [x] 分页查询使用 `pageNum/pageSize`。
- [x] XML 查询明确字段，不使用 `SELECT *`。

## Task 8: 数量台账查询 Service/Controller

**目标**：提供低代码运行态可调用的只读接口。

**涉及文件**：
- `BusinessQuantityQueryService.java`
- `BusinessQuantityQueryController.java`

**验收标准**：
- [x] Controller 返回 `RespInfo.success(data)`。
- [x] 只读查询不修改余额、流水和锁定表。
- [x] 查询接口具备权限校验。

## Task 9: 数量台账查询权限和前端 API

**目标**：运行态可安全读取数量视图。

**涉及文件**：
- `business-app.js`
- Flyway 权限脚本或复用 `V1.0.88` 后续版本脚本

**验收标准**：
- [x] 新增权限资源具备 `NOT EXISTS` 防重复。
- [x] 前端 API 封装使用 POST 查询，避免复杂条件拼 URL。

## Task 10: 详情区块运行协议接入数量查询

**目标**：低代码详情/展开面板可展示数量余额和流水。

**涉及文件**：
- `ExpandPanelRenderer.vue`
- `expand-utils.js`
- 新增或扩展数量面板 renderer

**验收标准**：
- [x] 面板类型支持 `quantity-balance` 和 `quantity-ledger`。
- [x] 支持从当前记录映射查询参数。
- [x] 加载失败显示错误并可重试。

**补充记录**：
- [x] 详情设计器保存的 `quantityPanels` 已由后端运行配置发布为 `options.detailPanels`。
- [x] 真实运行页、运行态预览和低代码页面块均已把 `detailPanels` 传递给 `AiCrudPage`。
- [x] 新增 `LowcodeRuntimeConfigBuilderTest` 覆盖详情数量区块发布到运行态 options，避免后续回归。

## Task 11: 详情设计器最小配置入口

**目标**：详情设置能声明数量区块，而不是手写 JSON。

**涉及文件**：
- `BusinessDetailDesigner.vue`
- `ForgePropertyPanel.vue` 如运行配置入口已有更合适位置则复用。

**验收标准**：
- [x] 能选择数量区块类型、标题、查询参数映射和展示字段。
- [x] 不影响已有关系页签和操作/审批日志配置。

## Task 12: 定向单测和构建验证

**目标**：按测试规范做增量验证。

**验收标准**：
- [x] 读取 `code-copilot/rules/automated-testing-standard.md`。
- [x] Maven 定向单测实际执行，日志包含 `Tests run`。
- [x] 后端插件编译通过。
- [x] 前端构建通过或明确跳过原因。
- [x] `execution-log.md` 追加命令、结果、警告和跳过项。

## Task 13: 采购仓储低代码配置验收文档

**目标**：沉淀采购仓储如何通过平台配置实现，不写业务代码。

**涉及文件**：
- `code-copilot/changes/lowcode-business-transaction-closure/procurement-warehouse-acceptance.md`

**验收标准**：
- [x] 覆盖采购入库、出库扣减、调拨转移、供应商报价维护、仓库详情。
- [x] 每个场景说明对象、关系、动作、流程回调、数量动作和详情区块配置。
