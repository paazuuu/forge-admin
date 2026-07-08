# 任务拆分 — 采购仓储低代码应用配置落地
> status: apply
> created: 2026-07-03
> 拆分顺序：SDD 边界 → 基础运行表 → 低代码资产 → 主子表/选择器 → 流程动作 → 演示数据 → 验证

## 前置条件

- [x] 已确认使用项目渐进式 SDD，不使用 superpowers 开发模式。
- [x] 已确认必须完全低代码模式，不新增采购仓储专用 Vue 页面或 Java Controller。
- [x] 已确认采购仓储作为业务应用 seed 可以出现采购/仓储对象名，但平台能力代码不能硬编码采购逻辑。

## 阶段总览

| 阶段 | 目标 | 任务 | 交付结果 |
|------|------|------|----------|
| Phase 1 | 应用骨架 | Task 1-4 | 运行表、低代码模型、业务对象、应用入口、基础 CRUD |
| Phase 2 | 主子表与选择器 | Task 5-6 | 供应商报价、采购/出库/调拨明细内嵌编辑，记录选择器批量带入 |
| Phase 3 | 流程与动作 | Task 7-8 | 审批回调执行入库、锁定/扣减/释放、调拨转移 |
| Phase 4 | 详情与演示数据 | Task 9-10 | 仓库/物料/采购详情区块和演示数据 |
| Phase 5 | 验证与对照 | Task 11-12 | 构建、SQL 校验、原型对照验收 |

## 任务清单

| Task | 名称 | 状态 | 优先级 |
|------|------|------|--------|
| Task 1 | SDD 变更文档初始化 | completed | P0 |
| Task 2 | 采购仓储基础运行表 Flyway | completed | P0 |
| Task 3 | 低代码模型与业务对象 seed | completed | P0 |
| Task 4 | 基础 CRUD 运行配置与应用入口 | completed | P0 |
| Task 5 | 主子表运行配置增强 | completed | P0 |
| Task 6 | 记录选择器配置 | partial | P1 |
| Task 7 | 采购/出库/调拨流程绑定 | completed | P0 |
| Task 8 | 数量台账动作配置 | completed | P0 |
| Task 9 | 详情数量区块和关联区块 | completed | P1 |
| Task 10 | 原型演示数据 seed | completed | P1 |
| Task 11 | 自动化验证和构建 | completed | P0 |
| Task 12 | 原型对照验收文档 | completed | P1 |
| Task 13 | 用户反馈问题修复 | completed | P0 |
| Task 14 | 二次反馈修复：设计器可见性、子表 ID、流程 key、字典保存 | completed | P0 |

## Task 1: SDD 变更文档初始化

**目标**：建立本变更的规格、任务、测试规格和执行日志。

**涉及文件**：
- `code-copilot/changes/lowcode-procurement-warehouse-app/spec.md`
- `code-copilot/changes/lowcode-procurement-warehouse-app/tasks.md`
- `code-copilot/changes/lowcode-procurement-warehouse-app/test-spec.md`
- `code-copilot/changes/lowcode-procurement-warehouse-app/execution-log.md`

**验收标准**：
- [x] 规格明确“完全低代码模式”边界。
- [x] 任务拆分符合渐进式 SDD。
- [x] 测试规格记录后续每阶段必跑项。

## Task 2: 采购仓储基础运行表 Flyway

**目标**：创建采购仓储低代码运行表，先支持基础 CRUD 和后续动作配置。

**涉及文件**：
- `forge-server/db/migration/V1.0.90__seed_procurement_warehouse_lowcode_app.sql` — 新增运行表 DDL。

**关键表**：
- `pw_material`
- `pw_supplier`
- `pw_supplier_material`
- `pw_warehouse`
- `pw_purchase_order`
- `pw_purchase_order_item`
- `pw_outbound_order`
- `pw_outbound_order_item`
- `pw_transfer_order`
- `pw_transfer_order_item`

**验收标准**：
- [x] 所有表包含 `id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`, `del_flag`。
- [x] 金额字段使用 `bigint`，单位分。
- [x] 业务编码字段具备租户内唯一索引。
- [x] 脚本使用 `CREATE TABLE IF NOT EXISTS`。

## Task 3: 低代码模型与业务对象 seed

**目标**：将运行表映射为低代码模型和应用中心业务对象。

**涉及文件**：
- `forge-server/db/migration/V1.0.90__seed_procurement_warehouse_lowcode_app.sql` — 追加 `ai_lowcode_domain`、`ai_lowcode_model`、`ai_business_suite`、`ai_business_object` seed。

**验收标准**：
- [x] 新增领域 `PROCUREMENT_WAREHOUSE`。
- [x] 新增套件 `PROCUREMENT_WAREHOUSE`。
- [x] 每个模型都有 `model_schema`，字段编码使用 lowerCamel，数据库列名使用 lower_snake。
- [x] 业务对象关联 `model_id`、`model_code`、`config_key`。

## Task 4: 基础 CRUD 运行配置与应用入口

**目标**：让采购仓储主要对象可以通过低代码运行页访问。

**涉及文件**：
- `forge-server/db/migration/V1.0.90__seed_procurement_warehouse_lowcode_app.sql` — 追加 `ai_crud_config`、`ai_business_app` seed。

**验收标准**：
- [x] 至少物料、供应商、仓库、采购单、出库单、调拨单具备应用入口。
- [x] 入口使用 `entry_mode=RUNTIME`，`config_key` 指向对应 `ai_crud_config`。
- [x] `api_config` 使用 `/ai/crud/{configKey}` 标准接口。
- [x] `publish_status=PUBLISHED`。

## Task 5: 主子表运行配置增强

**目标**：供应商、采购单、出库单、调拨单支持内嵌明细编辑。

**涉及文件**：
- `forge-server/db/migration/V1.0.90__seed_procurement_warehouse_lowcode_app.sql`

**验收标准**：
- [x] `page_schema.layoutType=master-detail-crud`。
- [x] `modelRefs` 声明主模型和明细模型。
- [x] 明细关系 `saveMode=merge`。
- [x] 运行配置 `options.masterDetailConfig.children` 可被 `AiCrudPage` 消费。
- [x] `PW_SUPPLIER_MATERIAL` 具备隐藏运行配置，可作为采购明细选择器数据源。

## Task 6: 记录选择器配置

**目标**：明细行可以选择物料、供应商报价或库存候选记录并映射字段。

**涉及文件**：
- `forge-server/db/migration/V1.0.90__seed_procurement_warehouse_lowcode_app.sql`

**验收标准**：
- [x] 供应商报价明细从物料选择器批量带入。
- [x] 采购明细从供应商报价选择器批量带入，支持按主表 `supplierId` 透传过滤条件。
- [x] 出库明细选择器透传当前 `warehouseId` 上下文。
- [x] 调拨明细选择器透传当前 `fromWarehouseId` 上下文。
- [ ] 出库/调拨明细按真实库存余额过滤候选记录，待通用库存余额选择器能力接入。

## Task 7: 采购/出库/调拨流程绑定

**目标**：将三类单据配置为 Flowable 流程驱动。

**涉及文件**：
- `forge-server/db/migration/V1.0.91__seed_procurement_warehouse_flow_actions.sql`

**验收标准**：
- [x] 采购单支持提交审批。
- [x] 出库单支持提交审批。
- [x] 调拨单支持提交审批。
- [x] 流程变量映射包含单据 ID、申请人、金额/数量摘要和状态。
- [ ] 本地端到端发起 Flowable 流程未实跑，待服务和数据库环境验收。

## Task 8: 数量台账动作配置

**目标**：审批结果通过通用动作引擎写数量台账。

**涉及文件**：
- `forge-server/db/migration/V1.0.91__seed_procurement_warehouse_flow_actions.sql`

**验收标准**：
- [x] 采购审批通过执行 `FOREACH + QUANTITY/INBOUND`。
- [x] 出库提交或审批中执行锁定，审批通过执行扣减，驳回释放。
- [x] 调拨审批通过执行 `FOREACH + QUANTITY/TRANSFER`。
- [x] 幂等键包含对象编码、记录 ID、明细 ID 和动作编码。
- [ ] 本地端到端扣减、释放、转移未实跑，待服务和数据库环境验收。

## Task 9: 详情数量区块和关联区块

**目标**：还原原型中的仓库详情、物料详情和采购详情多区块信息。

**涉及文件**：
- `forge-server/db/migration/V1.0.92__seed_procurement_warehouse_detail_panels_demo_data.sql`

**验收标准**：
- [x] 仓库详情展示库存余额、流水、锁定、采购记录、出库记录、调拨记录。
- [x] 物料详情展示供应商报价、库存信息、近 3 次出入库记录。
- [x] 采购详情展示基本信息、供应商信息、采购明细和附件占位。

## Task 10: 原型演示数据 seed

**目标**：提供接近原型的数据，方便人工验收。

**涉及文件**：
- `forge-server/db/migration/V1.0.92__seed_procurement_warehouse_detail_panels_demo_data.sql`

**验收标准**：
- [x] 包含原型中的主仓库、项目现场仓、常用物料和供应商。
- [x] 包含采购单、出库单、调拨单样例。
- [x] 数据具备防重复插入保护。

## Task 11: 自动化验证和构建

**目标**：按项目测试规范做增量验证。

**验收标准**：
- [x] 读取 `code-copilot/rules/automated-testing-standard.md`。
- [x] `git diff --check` 通过。
- [x] Flyway `${...}` placeholder 扫描无输出。
- [x] 后端插件编译通过。
- [x] 前端构建通过或记录跳过原因。
- [x] V1.0.91/V1.0.92 SQL 静态结构检查通过。

## Task 12: 原型对照验收文档

**目标**：明确哪些原型内容已通过低代码还原，哪些留到 UI 能力增强。

**涉及文件**：
- `code-copilot/changes/lowcode-procurement-warehouse-app/prototype-acceptance.md`

**验收标准**：
- [x] 覆盖采购管理、仓储管理、供应商管理、物料管理。
- [x] 标注“已还原 / 低代码等价还原 / 暂需后续平台 UI 能力”的差异。

## Task 13: 用户反馈问题修复

**目标**：修复物料、供应商、仓库、采购、出库等纯低代码运行态共性问题，并补齐字段长度和常用校验配置能力。

**涉及文件**：
- `forge-server/db/migration/V1.0.93__fix_procurement_warehouse_lowcode_runtime_config.sql`
- `forge-admin-ui/src/utils/validation-presets.js`
- `forge-admin-ui/src/components/ai-form/AiForm.vue`
- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`
- `forge-admin-ui/src/components/page-templates/ChildTableEditor.vue`
- `forge-admin-ui/src/views/ai/crud-page.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue`
- `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessDocumentRuntimeService.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/DynamicCrudServiceAutoGenerationTest.java`

**验收标准**：
- [x] 物料、供应商、仓库、采购、出库、调拨编号字段使用编码规则后端自动生成。
- [x] 状态、仓库类型、单据状态使用字典配置，列表回显使用字典标签。
- [x] 子表原始 ID 字段在表单中隐藏，记录选择器负责回填 ID 和名称。
- [x] 子表列宽按字段类型设置合理最小宽度，避免数字展示过窄。
- [x] 采购/出库/调拨运行配置补齐业务对象编码，流程只保留自定义提交审批动作入口。
- [x] 字段属性支持最大长度配置和常用校验规则选择，运行态表单消费校验配置。
- [x] 低代码新增记录在自动编号配置缺失或迁移未生效时，后端可按编号字段约定兜底生成，避免非空编码列插入空值。

## Task 14: 二次反馈修复：设计器可见性、子表 ID、流程 key、字典保存

**目标**：针对用户二次反馈，修复常用校验点击无明显变化、最大长度入口不明显、关系配置不可见、子表内部 ID 手填、流程仍指向旧流程、采购单保存字典类型校验失败等问题。

**涉及文件**：
- `forge-server/db/migration/V1.0.94__fix_procurement_warehouse_designer_flow_relation_config.sql`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue`
- `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue`
- `forge-admin-ui/src/components/page-templates/ChildTableEditor.vue`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectDesignerService.java`

**验收标准**：
- [x] 常用校验选择后同步写入 `preset`、`pattern`、`message`，用户能看到正则和提示变化。
- [x] 最大长度在字段基础配置/新版画布属性面板中作为显眼“字段约束”展示，并同步字段资产长度。
- [x] 表单设计主画布展示“关系与级联”摘要和入口，左侧对象设计器导航默认展开。
- [x] 子表编辑器默认隐藏 `id`、`xxxId`、`xxx_id` 等内部主键/外键，除非显式配置 `showInChildEditor=true`。
- [x] 采购、出库、调拨流程绑定改为业务流程 key：`pw_purchase_approval`、`pw_outbound_approval`、`pw_transfer_approval`。
- [x] 表单设计保存时不再用空 `dictType` 覆盖字段资产已有字典类型，采购单 `orderStatus` 兜底配置 `pw_order_status`。
