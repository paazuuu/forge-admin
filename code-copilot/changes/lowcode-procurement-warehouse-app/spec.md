# 采购仓储低代码应用配置落地
> status: apply
> created: 2026-07-03
> complexity: 🔴复杂

## 1. 背景与目标

用户希望按原型图通过完全低代码方式 1:1 还原采购仓储模块的业务逻辑和 UI：

- `/Users/yaomindong/Desktop/project/mdframe/shenrong/shenrong-projman/code-copilot/docs/系统原型交互-采购仓储.html`
- `/Users/yaomindong/Desktop/project/mdframe/shenrong/shenrong-projman/code-copilot/docs/系统原型交互.html`

前置平台能力已经通过 `lowcode-platform-capability-foundation` 和 `lowcode-business-transaction-closure` 补齐：通用动作、记录选择器、主子表 merge、流程回调、触发器复用动作、领域动作 SPI、数量台账、数量查询和详情数量区块。

本变更目标不是再写采购专用后端页面，而是沉淀一套可运行的低代码应用资产：

- 采购仓储业务领域、业务套件、业务对象和应用入口。
- 物料、供应商、供应商报价、仓库、采购单、采购明细、出库单、出库明细、调拨单、调拨明细的低代码模型和运行配置。
- 第一阶段先完成基础 CRUD、主子表、列表/表单/详情结构和菜单入口。
- 本轮继续补齐流程回调动作、数量台账动作、详情区块、演示数据和原型对照；像素级 UI 细节增强作为后续平台模板能力。

完成后，用户可以在应用中心看到采购仓储套件，并通过低代码运行页进入采购管理、仓储管理、供应商管理、物料管理等入口，继续在设计器里可视化调整。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- `forge-server/db/migration/V1.0.7__add_lowcode_business_domain.sql`：定义 `ai_lowcode_domain`，低代码领域通过 Flyway seed 初始化。
- `forge-server/db/migration/V1.0.9__add_lowcode_data_model.sql`：定义 `ai_lowcode_model`，模型独立于应用，包含 `model_schema`。
- `forge-server/db/migration/V1.0.26__add_business_app_platform_tables.sql`：定义 `ai_business_suite`、`ai_business_object`、`ai_business_object_relation`、`ai_business_app`。
- `forge-server/db/全量初始化SQL.sql`：`ai_crud_config` 为低代码运行配置表，包含 `search_schema`、`columns_schema`、`edit_schema`、`api_config`、`options`、`model_schema`、`page_schema`。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`：从模型和页面协议生成 `AiCrudPage` 运行配置。
- `forge-admin-ui/src/views/ai/crud-page.vue`：低代码运行页通过 `configKey` 读取发布配置并渲染 `AiCrudPage`。
- `forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue`：对象动作可配置 `COMMAND`、`FOREACH`、`DOMAIN_ACTION/QUANTITY`。
- `forge-admin-ui/src/components/page-templates/ChildTableEditor.vue`：运行态子表支持选择器批量追加和 `merge` 保存。

### 2.2 现有实现

- `lowcode-platform-capability-foundation` 已完成动作底座、记录选择器、流程回调、触发器动作、领域动作 SPI 和数量台账。
- `lowcode-business-transaction-closure` 已完成 `FOREACH` 动作、选择器动态过滤、数量台账查询和详情数量区块。
- 既有 CRM seed（如 `V1.0.32__seed_crm_customer_runtime_link.sql`、`V1.0.37__seed_crm_runtime_crud_configs.sql`）已经使用 Flyway 初始化低代码模型、运行配置和应用入口。
- 现有采购审批样例 `V1.0.83__seed_sample_purchase_order_app_center_entry.sql` 是代码应用，不满足“完全低代码模式”，但可作为应用中心入口组织方式参考。

### 2.3 发现与风险

- 如果只建业务对象、不写 `ai_crud_config`，用户仍需要手工发布才能访问运行页，不能作为可运行模块交付。
- 如果把采购逻辑写成 Vue 页面或 Java Controller，会违背“完全低代码模式”。
- 原型中的视觉 1:1 包含多区块详情、底部操作栏、局部统计和复杂表格排版；低代码运行态可先接近结构与业务逻辑，像素级 UI 需要后续补页面区块样式模板。
- 采购入库、出库扣减、调拨转移涉及库存数量变化，属于高风险状态流转，必须通过通用动作引擎和数量台账，不能靠普通字段更新。

## 3. 功能点

- [x] **低代码应用骨架**：初始化采购仓储低代码领域、业务套件、业务对象、应用入口和基础运行配置。
- [x] **基础模型与运行表**：创建物料、供应商、供应商报价、仓库、采购单/明细、出库单/明细、调拨单/明细运行表，所有表包含标准审计字段和租户字段。
- [x] **基础 CRUD 页面**：物料、供应商、供应商报价、仓库、采购单、出库单、调拨单具备运行配置，主入口全部走 `/ai/crud/{configKey}`。
- [x] **主子表配置**：供应商报价、采购单、出库单、调拨单通过低代码主子表配置维护明细行，明细保存使用 `merge` 模式。
- [x] **记录选择器配置**：供应商报价选择物料，采购明细选择供应商报价，出库/调拨明细选择物料并透传当前仓库上下文；真实库存余额候选过滤留到数量动作联调阶段。
- [x] **流程与动作配置**：采购审批通过后入库；出库审批通过后扣减，驳回释放；调拨审批通过后转移。
- [x] **数量详情区块**：仓库详情展示数量余额、数量流水、数量锁定，以及采购/出库/调拨关联记录。
- [x] **演示数据**：提供贴近原型的物料、供应商、仓库、采购单、出库单、调拨单样例数据。
- [x] **原型对照验收**：逐页对照原型文档，记录已还原、低代码等价还原和暂不支持的 UI 细节。

## 4. 业务规则

- 本变更允许 seed 采购仓储应用资产，但平台服务、组件和接口不能写采购专用逻辑。
- 所有采购仓储业务页面必须通过低代码运行配置访问，不新增采购仓储专用 Vue 页面或 Java Controller。
- 数量变化必须通过 `DOMAIN_ACTION/QUANTITY`，不能直接更新库存字段。
- 采购、出库、调拨的明细动作必须使用 `FOREACH` 逐行执行，幂等键必须包含对象、记录、明细行和动作类型。
- 金额字段存储单位为分，字段类型为 `bigint`。
- 所有 Long ID 前端按字符串处理。
- 运行表 `tenant_id` 必须为 1 默认租户，业务表包含 `id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`, `del_flag`。
- Flyway seed 必须具备 `IF NOT EXISTS` / `NOT EXISTS` 防重复保护。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增 | `pw_material` | 物料编号、名称、规格、单位、预警数量、状态 | 物料主数据运行表 |
| 新增 | `pw_supplier` | 供应商编号、名称、联系人、电话、状态 | 供应商主数据运行表 |
| 新增 | `pw_supplier_material` | 供应商、物料、报价、有效期 | 供应商报价明细运行表 |
| 新增 | `pw_warehouse` | 仓库编号、名称、类型、关联项目、状态 | 仓库/库存账户运行表 |
| 新增 | `pw_purchase_order` | 采购单号、项目、仓库、采购人、金额、状态 | 采购单主表 |
| 新增 | `pw_purchase_order_item` | 采购单、物料、数量、单价、金额 | 采购明细表 |
| 新增 | `pw_outbound_order` | 出库单号、仓库、申请人、状态 | 出库单主表 |
| 新增 | `pw_outbound_order_item` | 出库单、物料、库存数量、出库数量 | 出库明细表 |
| 新增 | `pw_transfer_order` | 调拨单号、调出仓、调入仓、状态 | 调拨单主表 |
| 新增 | `pw_transfer_order_item` | 调拨单、物料、当前库存、调拨数量 | 调拨明细表 |
| 新增/更新 | `ai_lowcode_domain` | `PROCUREMENT_WAREHOUSE` | 采购仓储低代码领域 |
| 新增/更新 | `ai_lowcode_model` | 10 个模型 | 低代码模型协议 |
| 新增/更新 | `ai_business_suite/object/app/relation` | 采购仓储业务套件与入口 | 应用中心资产 |
| 新增/更新 | `ai_crud_config` | 7 个运行配置 + 4 个主子表配置 | 低代码运行页配置；明细对象通过主子表运行 |
| 新增/更新 | `ai_business_document_config` / `ai_business_binding` | 3 类单据 | 采购/出库/调拨流程绑定和回调动作 |
| 新增/更新 | `ai_business_object.designer_options` | 3 类单据动作 | 提交审批、入库、锁定、扣减、释放、转移动作 |
| 新增/更新 | `ai_crud_config.options.detailPanels` | 仓库/物料/采购详情 | 数量区块和关联记录面板 |
| 新增 | `pw_*` / `ai_business_quantity_*` | demo 数据 | 原型验收用物料、供应商、仓库、单据和数量余额 |

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 复用 | `/ai/crud/{configKey}/page` | GET | 采购仓储低代码列表 |
| 复用 | `/ai/crud/{configKey}` | POST/PUT | 采购仓储低代码新增/修改 |
| 复用 | `/ai/crud/{configKey}/{id}` | GET/DELETE | 详情/删除 |
| 复用 | `/ai/business/action/execute` | POST | 后续用于采购入库、出库扣减、调拨转移 |
| 复用 | `/ai/business/selector/query` | POST | 后续用于物料/库存记录选择 |
| 复用 | `/ai/business/quantity/query/*` | POST | 后续用于仓库详情数量区块 |

## 7. 影响范围

- 数据库：新增采购仓储低代码运行表和 seed 数据。
- 应用中心：新增采购仓储业务套件、对象和应用入口。
- 低代码运行态：新增 10 个已发布配置，菜单/入口走 `AiCrudPage`。
- 测试：SQL 防重复、前端构建、后端编译、低代码运行配置解析。

## 8. 风险与关注点

> ⚠️ 本变更涉及采购审批状态、出入库数量变化和调拨转移，属于高风险状态流转能力。

- 已完成流程和数量动作配置，但本地未启动 MySQL/Redis/后端/Flow 服务实跑审批通过、驳回和扣库存链路；上线前必须做端到端人工验收。
- `V1.0.94` 已将采购、出库、调拨从旧的 `leave_multi` 绑定修正为业务流程 key：`pw_purchase_approval`、`pw_outbound_approval`、`pw_transfer_approval`；目标环境仍需在 Flowable 中部署同名 BPMN 流程定义后才能端到端发起。
- 出库提交动作已经配置为先锁定库存，审批通过扣减，驳回释放；如审批流程实例重复回调，数量台账依赖通用幂等键兜底。
- 原型 UI 的像素级还原可能需要后续补低代码页面样式模板，本变更先保证完全低代码模式和业务结构闭环。
- 如果目标环境没有配置独立 `LOWCODE_RUNTIME` 数据源，运行表先落在默认主库；后续可通过低代码发布流程迁移到专用数据源。
- 出库/调拨明细选择器已配置仓库上下文参数，但当前仍使用物料对象作为候选源；真实“按仓库可用库存过滤候选物料”仍需后续补通用库存余额选择器对象。
- 已通过 `V1.0.92` 给仓库和物料详情接入数量余额/流水/锁定展示；“按可用库存选择候选物料”仍建议后续补通用库存余额选择器对象。

## 8.5 测试策略

- **测试范围**：Flyway SQL 语法/防重复、运行表字段规范、低代码运行配置 JSON 结构、前端构建、后端编译。
- **覆盖率目标**：第一阶段以静态验证和构建为主；后续动作/数量流转必须增加服务单测或端到端手工验收。
- **独立 Test Spec**：是。

## 9. 待澄清

- [x] 是否按渐进式 SDD 先做低代码应用骨架，再逐步补动作、流程和数据。结论：是。
- [x] 是否使用 superpowers 开发模式。结论：否，按本项目 SDD 文档推进。

## 10. 技术决策

- 第一阶段使用 Flyway seed 初始化低代码资产，复用 CRM seed 模式。
- 运行入口统一使用 `entry_mode=RUNTIME` 和 `config_key`，不新增采购仓储专用前端页面。
- 主子表在 `page_schema.modelRefs[].props.saveMode=merge` 表达，运行态复用 `ChildTableEditor`。
- 复杂副作用在后续阶段通过 `BusinessActionDesigner` 配置 `FOREACH + DOMAIN_ACTION/QUANTITY`。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| SDD init | completed | `spec.md`, `tasks.md`, `test-spec.md`, `execution-log.md` | 用户确认开始 `lowcode-procurement-warehouse-app` 开发 |
| Task 2 | completed | `V1.0.90__seed_procurement_warehouse_lowcode_app.sql` | 新增 10 张 `pw_*` 采购仓储低代码运行表 |
| Task 3 | completed | `V1.0.90__seed_procurement_warehouse_lowcode_app.sql` | 新增采购仓储低代码领域、套件、10 个模型和业务对象 |
| Task 4 | completed | `V1.0.90__seed_procurement_warehouse_lowcode_app.sql` | 新增物料、供应商、仓库、采购、出库、调拨 6 个基础 CRUD 运行配置和应用入口 |
| Task 5 | completed | `V1.0.90__seed_procurement_warehouse_lowcode_app.sql` | 供应商、采购、出库、调拨升级为 `master-detail-crud`，子表 `saveMode=merge` |
| Task 6 | partial | `V1.0.90__seed_procurement_warehouse_lowcode_app.sql` | 供应商报价/采购明细选择器完成；出库/调拨已透传仓库上下文，真实库存余额候选过滤后续联调 |
| Task 7 | completed | `V1.0.91__seed_procurement_warehouse_flow_actions.sql` | 三类单据绑定 `leave_multi`，运行页新增提交审批行按钮 |
| Task 8 | completed | `V1.0.91__seed_procurement_warehouse_flow_actions.sql` | 配置 `FOREACH + DOMAIN_ACTION/QUANTITY` 入库、锁定、扣减、释放、转移动作 |
| Task 9 | completed | `V1.0.92__seed_procurement_warehouse_detail_panels_demo_data.sql` | 仓库/物料/采购详情区块接入数量和关联记录面板 |
| Task 10 | completed | `V1.0.92__seed_procurement_warehouse_detail_panels_demo_data.sql` | Seed 主仓库、现场仓、物料、供应商、采购/出库/调拨单和数量台账演示数据 |
| Task 11 | completed | `test-spec.md`, `execution-log.md` | 完成 SQL 静态检查、后端 generator 编译、前端构建 |
| Task 12 | completed | `prototype-acceptance.md` | 完成采购、仓储、供应商、物料原型对照 |
| Task 13 | completed | `V1.0.93__fix_procurement_warehouse_lowcode_runtime_config.sql`, `validation-presets.js`, `AiCrudPage.vue`, `AiForm.vue`, `ChildTableEditor.vue`, `crud-page.vue`, `BusinessFieldPropertyPanel.vue`, `ForgePropertyPanel.vue`, `LowcodeRuntimeConfigBuilder.java`, `BusinessDocumentRuntimeService.java`, `DynamicCrudService.java`, `DynamicCrudServiceAutoGenerationTest.java` | 修复自动编号、字典状态、记录选择器业务对象编码、子表列宽、重复流程入口、字段长度和常用校验配置，并补齐运行态自动编号兜底 |
| Task 14 | completed | `V1.0.94__fix_procurement_warehouse_designer_flow_relation_config.sql`, `BusinessFieldPropertyPanel.vue`, `ForgePropertyPanel.vue`, `BusinessFormDesigner.vue`, `BusinessObjectDesignerShell.vue`, `ChildTableEditor.vue`, `BusinessObjectDesignerService.java` | 修复字段约束入口和常用校验同步、关系配置可见性、子表内部 ID 默认隐藏、采购仓储业务流程 key、字典类型保存兜底 |

## 12. 审查结论

采购仓储低代码应用主链路已完成配置层落地：应用入口、主子表、选择器、流程绑定、数量动作、详情区块、演示数据和原型对照均已具备。用户反馈的自动编号、状态字典、记录选择器业务对象编码、子表列宽、重复流程入口、字段长度和常用校验配置已完成平台级修复；二次反馈中的关系配置可见性、子表内部 ID 默认隐藏、旧流程 key 替换、采购单字典保存兜底也已完成。剩余风险是未在本地数据库和 Flow 服务中做审批/扣库存端到端验收，以及按仓库可用库存过滤候选物料仍需后续平台级库存选择器增强。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-03
- **确认人**：用户
- **确认内容**：用户确认可以开始 `lowcode-procurement-warehouse-app` 开发，使用项目渐进式 SDD 开发流程，不使用 superpowers 开发模式。
