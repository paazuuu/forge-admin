# 低代码业务交易闭环能力补齐
> status: apply_completed
> created: 2026-07-03
> complexity: 🔴复杂

## 1. 背景与目标

`lowcode-platform-capability-foundation` 已补齐通用动作、记录选择器、主子表 merge、流程回调、触发器复用动作、领域动作 SPI、数量台账和分层提醒。采购仓储原型继续暴露的是“交易闭环”能力缺口：

- 审批通过后需要按明细行逐行执行入账、扣减或转移，现有动作步骤只能执行单次领域动作。
- 选择器需要支持“物料编号/名称”多字段关键词搜索，以及按当前仓库、项目、供应商等上下文字段过滤候选记录。
- 数量台账已有余额和流水，但还缺少可配置查询投影，无法直接在低代码详情页展示库存明细、出入库记录、来源单据和预警状态。
- 详情页仍偏“主信息 + 关系页签”，无法通过低代码 1:1 还原原型中的多区块详情：仓库信息、库存明细、出库记录、采购记录、调拨记录。

本变更继续坚持平台级通用能力，采购仓储只作为验收样例，不在平台代码中硬编码采购、仓库、供应商、物料等对象名。

完成后，平台应支持：

- 通用动作步骤可遍历当前记录、表单参数或上下文中的集合，对每一行执行白名单子步骤。
- 记录选择器支持多字段 OR 关键词查询，并支持从表单/记录/路由上下文解析动态过滤参数。
- 数量台账提供只读查询接口，返回余额、流水和锁定记录，可被低代码详情区块或展开面板消费。
- 详情配置支持“关联数据区块/数量台账区块”的运行时配置，为采购仓储原型提供端到端低代码验收基础。

## 2. 代码现状

### 2.1 已有能力

- `BusinessActionExecutionService`：统一执行业务动作，支持日志、幂等、事务和白名单步骤。
- `DomainActionStepExecutor` + `BusinessQuantityDomainActionExecutor`：支持 `DOMAIN_ACTION` 调用通用数量台账。
- `BusinessQuantityLedgerService`：支持入账、锁定、释放、扣减、转移、幂等和防负数。
- `BusinessRecordSelectorService` + `AiRecordSelectorModal.vue`：支持通用对象选择器、字段映射和权限过滤。
- `ChildTableEditor.vue`：支持选择器批量追加子表和 merge 保存。
- `BusinessDetailDesigner.vue`：支持主信息复用表单设计、关联页签、操作日志和审批记录。

### 2.2 已发现缺口

- `BusinessActionStepConfigHelper` 只能从单个 record/form/context 解析字段，缺少集合路径、循环变量和循环结果聚合。
- `DomainActionStepExecutor` 只执行一次领域动作，无法按子表行逐行执行数量动作。
- `BusinessRecordSelectorService#buildSearchParams` 仅在 `keywordFields.size() == 1` 时处理关键词，多字段搜索不生效。
- 选择器 `searchParams` 目前是静态对象，缺少 `${form.xxx}`、`${record.xxx}`、`${query.xxx}` 这类上下文表达式解析。
- 数量台账没有独立只读查询 Controller/Service/VO，低代码页面只能通过自定义 API 或代码页面读取库存视图。
- 详情设计器目前只能预览关系页签，不能把数量台账余额/流水配置成详情区块。

## 3. 功能点

- [x] **FOREACH 动作步骤**：新增 `FOREACH` 白名单步骤，支持 `collectionPath`、`itemAlias`、`indexAlias`、`steps`，逐行执行子步骤并汇总结果。
- [x] **循环上下文解析**：动作步骤字段映射支持 `${item.xxx}`、`${index}`，循环内可读取父级 record/form/context。
- [x] **选择器多字段搜索**：选择器关键词支持多个字段 OR 查询，不暴露 SQL 给用户。
- [x] **选择器动态过滤**：选择器 `searchParams` 支持从 formData、record、routeQuery、context 解析表达式，运行态字段和子表选择器都可使用。
- [x] **数量台账只读查询**：新增余额/流水查询协议，支持按 accountCode、itemCode、dimensionKey、sourceObjectCode、sourceRecordId、sourceDetailId 过滤。
- [x] **详情数量区块运行协议**：低代码运行配置可声明数量台账详情区块，运行态通过通用接口加载数据。
- [x] **采购仓储验收样例文档**：用平台配置描述采购入库、出库扣减、调拨转移、供应商报价维护和仓库详情区块，不生成业务硬编码代码。

## 4. 业务规则

- 平台服务、接口、表结构和前端组件命名必须通用，不能出现采购仓储专用命名。
- 动作循环步骤只能嵌套执行已有白名单步骤，不允许任意脚本执行。
- 循环步骤默认同事务；任一子步骤失败时按父步骤 `rollbackOnFailure` 控制回滚。
- 数量台账查询是只读接口，写入仍只能通过动作引擎或内部服务。
- 选择器查询必须继续复用业务对象权限、租户隔离、数据权限、字段白名单和 Long ID 字符串化。
- 所有新增 SQL 写 Mapper XML，不在 Service 层拼复杂 SQL。
- 端到端验收时采购仓储只能作为配置样例，不写死对象编码。

## 5. 数据变更

本轮 P0 不新增业务表。数量台账复用 `ai_business_quantity_balance`、`ai_business_quantity_ledger`、`ai_business_quantity_lock`。

新增权限迁移脚本 `V1.0.89__add_business_quantity_query_permission.sql`，写入 `ai:businessQuantity:view` 只读权限，具备 `NOT EXISTS` 防重复保护，`tenant_id=1`。

如后续需要沉淀详情区块模板，可另起变更新增平台配置表或扩展现有 page/view schema，不在本轮新增表。

## 6. 接口变更

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/business/quantity/query/balance` | POST | 查询通用数量余额 |
| 新增 | `/ai/business/quantity/query/ledger` | POST | 查询通用数量流水 |
| 新增 | `/ai/business/quantity/query/lock` | POST | 查询通用数量锁定记录 |
| 扩展 | `/ai/business/selector/query` | POST | 支持多字段关键词和动态过滤 |
| 扩展 | `/ai/business/action/execute` | POST | 支持 `FOREACH` 动作步骤 |

## 7. 影响范围

- 后端：`forge-plugin-generator` 的动作执行、选择器、数量台账 Mapper/Service/Controller。
- 前端：`AiRecordSelectorModal`、`AiFormItem`、`ChildTableEditor`、动作设计器、详情设计器和运行态详情区块。
- 测试：动作循环、选择器多字段搜索/动态过滤、数量查询权限和分页。

## 8. 风险与关注点

- `FOREACH` 容易被滥用成脚本引擎，必须只允许白名单步骤，并限制递归深度。
- 循环执行数量动作必须保证幂等键稳定且按明细行区分，避免审批回调重复入库或重复扣减。
- 多字段 OR 搜索必须通过动态 CRUD/Mapper 白名单实现，不能把字段名或 SQL 片段直接交给用户。
- 数量台账查询可能暴露业务敏感库存，需要走权限和字段边界，不能开放任意租户数据。
- 详情区块若范围过大容易变成页面搭建器重写，本轮只做运行协议和最小配置入口。

## 9. 测试策略

- `BusinessActionForeachStepExecutorTest`：覆盖集合不存在、空集合、逐行成功、子步骤失败回滚、循环变量映射和稳定幂等键。
- `BusinessRecordSelectorServiceTest`：覆盖多字段关键词、动态过滤解析、非法字段过滤、权限拒绝。
- `BusinessQuantityQueryServiceTest`：覆盖余额、流水、锁定查询条件和分页。
- 前端构建：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
- 后端验证：插件编译和定向单测必须实际执行，使用 `-Pdev,enable-tests`。

## 10. 验收样例

采购仓储原型应能通过低代码配置表达：

- 采购单审批通过后遍历采购明细，逐行对目标仓库或项目现场仓执行数量入账。
- 出库申请提交或审批通过后遍历出库明细，对当前仓库逐行锁定并最终扣减。
- 调拨申请审批通过后遍历调拨明细，从调出仓库向调入仓库逐行转移。
- 采购单/供应商新增时通过记录选择器选择物料并批量带入明细字段。
- 仓库详情展示数量余额列表、采购记录、出库记录、调拨记录和流水。

## 11. 执行日志

| 时间 | 事项 | 状态 | 备注 |
|------|------|------|------|
| 2026-07-03 | 创建 SDD 变更 | in_progress | 从采购仓储原型评估收敛为通用业务交易闭环能力 |
| 2026-07-03 | 完成 P0 平台能力实现 | completed | `FOREACH` 动作、选择器多字段搜索/动态过滤、数量台账只读查询和权限/API |
| 2026-07-03 | 完成 P1 前端配置与详情区块 | completed | 动作循环模板、选择器过滤入口、子表选择器配置、数量展开/详情区块和验收文档 |
| 2026-07-03 | 补齐详情区块发布运行态贯通 | completed | `detail.quantityPanels` 发布为 `options.detailPanels`，真实运行页、预览页和低代码页面块均传入 `AiCrudPage` |
| 2026-07-03 | 补充运行配置单测 | completed | 新增 `LowcodeRuntimeConfigBuilderTest` 覆盖详情数量区块发布到运行态 options |
| 2026-07-03 | 修复设计器配置保真和菜单停用等回归 | completed | 保留字典/选择器/自动编号元数据，合并发布入口，停用同步隐藏菜单，修复 Delete 误清绑定和表格全局对齐 |
| 2026-07-04 | 修复表单校验和关系选择器配置体验 | completed | 常用校验不再被 schema 归一化清空；关系子表选择器改为结构化选择配置；运行态按 recordSelector 元数据发布选择器组件 |
