# 执行日志 — 采购仓储低代码应用配置落地
> status: apply
> created: 2026-07-03

## 2026-07-03 SDD 初始化

- 用户确认开始 `lowcode-procurement-warehouse-app` 开发。
- 用户明确要求使用项目渐进式 SDD 开发流程，不使用 superpowers 开发模式。
- 创建变更目录：`code-copilot/changes/lowcode-procurement-warehouse-app/`。
- 第一阶段范围收敛为低代码应用骨架：运行表、低代码领域/模型、业务对象、基础运行配置和应用入口。

## 验证记录

### 2026-07-03 Phase 1 低代码应用骨架

**变更范围**

- 新增 Flyway 迁移 `V1.0.90__seed_procurement_warehouse_lowcode_app.sql`。
- 创建 10 张采购仓储低代码运行表：物料、供应商、供应商报价、仓库、采购单/明细、出库单/明细、调拨单/明细。
- Seed 采购仓储低代码领域 `PROCUREMENT_WAREHOUSE`、业务套件、10 个低代码模型、10 个业务对象。
- Seed 6 个基础 CRUD 运行配置和应用入口：物料、供应商、仓库、采购、出库、调拨。

**执行命令与结果**

- `git diff --no-index --check /dev/null forge-server/db/migration/V1.0.90__seed_procurement_warehouse_lowcode_app.sql`
  - 结果：通过，无空白错误输出。
  - 备注：该命令与 `/dev/null` 对比新增文件，退出码 1 表示存在文件差异；无输出表示 `--check` 未发现空白错误。
- `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.90__seed_procurement_warehouse_lowcode_app.sql`
  - 结果：通过，无输出。
  - 备注：选择器上下文表达式在 SQL 中使用 `CONCAT('$', '{formData.xxx}')`，避免被 Flyway 当作 placeholder。
- `node -e "const fs=require('fs'); const s=fs.readFileSync(process.argv[1],'utf8'); const q=(s.match(/[']/g)||[]).length; const l=(s.match(/\\(/g)||[]).length; const r=(s.match(/\\)/g)||[]).length; console.log('single_quotes='+q); console.log('left_parens='+l); console.log('right_parens='+r); process.exit((q % 2 || l !== r) ? 1 : 0)" forge-server/db/migration/V1.0.90__seed_procurement_warehouse_lowcode_app.sql`
  - 结果：通过，`single_quotes=8758`、`left_parens=723`、`right_parens=723`。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 工作目录：`forge-server`
  - 结果：通过，`BUILD SUCCESS`。
  - 警告：既有 deprecated/unchecked 编译警告，不阻断。

### 2026-07-03 Phase 2 主子表与记录选择器

**变更范围**

- 新增隐藏运行配置 `pw_supplier_material`，让 `PW_SUPPLIER_MATERIAL` 可作为通用记录选择器数据源。
- 将供应商、采购单、出库单、调拨单运行配置升级为 `master-detail-crud`。
- 在 `page_schema.modelRefs` 中声明主模型、明细模型和一对多关系。
- 在 `options.masterDetailConfig.children` 中配置明细表、`saveMode=merge` 和 `ChildTableEditor` 可消费字段。
- 配置供应商报价选择物料、采购明细选择供应商报价、出库/调拨明细选择物料的记录选择器。
- 采购、出库、调拨主表仓库/供应商字段改为记录选择器配置，选择后回填 ID 和名称字段。

**执行命令与结果**

- `rg -n "config_key = 'pw_supplier_material'|layoutType', 'master-detail-crud'|objectCode', 'PW_SUPPLIER_MATERIAL'|saveMode', 'merge'|recordSelector|CONCAT\('\$', '\{formData" forge-server/db/migration/V1.0.90__seed_procurement_warehouse_lowcode_app.sql`
  - 结果：通过，命中供应商报价配置、主子表布局、`merge` 保存和记录选择器配置。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 工作目录：`forge-server`
  - 结果：通过，`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`built in 1m 39s`。
  - 警告：既有 Vite dynamic/static import chunk 提示、CSS `//` 注释 warning、`UserSelectModal` 命名冲突提示，不阻断。

**跳过项**

- 未实跑 Flyway 入库迁移：本轮没有启动本地 MySQL/Redis/后端服务，先以 SQL 静态检查和构建验证为准。
- 未做端到端扣库存验收：流程绑定和数量台账动作属于后续 Task 7-8。

**服务清理**

- 本轮没有启动长期运行的本地服务，无需清理 PID。

### 2026-07-03 物料新增编号为空修复

**变更范围**

- 修复低代码运行态新增记录时，字段配置未显式带 `generation` 或迁移未生效时，`materialCode=null` 直接进入 `INSERT` 导致 `Column 'material_code' cannot be null` 的问题。
- `DynamicCrudService` 在显式自动编号配置为空时，增加平台级约定兜底：字段名以 `Code` / `No` 结尾、列名以 `_code` / `_no` 结尾，或字段标签包含“编号/单号”时，按列名作为编码规则编码调用 `CodeRuleService.generate`。
- 兜底只在当前字段无值时填充，并把 camel/snake 别名同时加入可写字段，保证最终 `materialCode` 和 `material_code` 都能被写入。
- 如果编码规则不存在，捕获 `BusinessException` 后跳过，避免普通业务 code 字段被误伤。
- 如果字段已经显式配置过 `generation`，包括 `enabled=false`，则尊重显式配置，不再走约定兜底。
- 新增 `DynamicCrudServiceAutoGenerationTest`，覆盖空编号生成、已有编号不覆盖、显式关闭 generation 不兜底三个场景。

**执行命令与结果**

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=DynamicCrudServiceAutoGenerationTest test`
  - 首次结果：失败，原因是新增测试使用 Mockito mock 时，本机 Homebrew JDK 17 无法初始化 Mockito inline ByteBuddy mock maker，自附加 agent 失败；不是业务断言失败。
  - 处理：将新增测试改为内部轻量 fake，不依赖 Mockito。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=DynamicCrudServiceAutoGenerationTest test`
  - 结果：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/DynamicCrudServiceAutoGenerationTest.java`
  - 结果：通过，无空白错误输出。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 工作目录：`forge-server`
  - 结果：通过，`BUILD SUCCESS`，总耗时 `12.886 s`。
  - 警告：既有 deprecated/unchecked 编译警告，不阻断。

**跳过项**

- 未实跑新增物料接口：本轮没有启动 MySQL/Redis/后端服务；已通过服务私有方法单测覆盖插入前自动编号填充路径。

**服务清理**

- 本轮没有启动长期运行的本地服务，无需清理 PID。

### 2026-07-03 Phase 3 流程绑定与数量动作

**变更范围**

- 新增 Flyway 迁移 `V1.0.91__seed_procurement_warehouse_flow_actions.sql`。
- 三类单据写入 `ai_business_document_config`，启用单据模式，状态字段为 `orderStatus`。
- 三类单据写入 `ai_business_binding`，绑定复用流程 `leave_multi`，`startMode=ACTION_ONLY`。
- 流程回调动作：
  - 采购 `APPROVED` -> `inbound_purchase_stock`
  - 出库 `APPROVED` -> `commit_outbound_stock`
  - 出库 `REJECTED` -> `release_outbound_stock`
  - 调拨 `APPROVED` -> `transfer_stock`
- 三类单据写入 `ai_business_object.designer_options.actions`，动作步骤全部使用通用 `START_FLOW`、`FOREACH`、`DOMAIN_ACTION/QUANTITY`。
- 三类运行页写入 `ai_crud_config.options.rowActions`，行按钮使用 `COMMAND` 调用通用业务动作接口。

**执行命令与结果**

- `git diff --no-index --check /dev/null forge-server/db/migration/V1.0.91__seed_procurement_warehouse_flow_actions.sql`
  - 结果：通过，无空白错误输出。
  - 备注：该命令与 `/dev/null` 对比新增文件，退出码 1 表示存在文件差异；无输出表示 `--check` 未发现空白错误。
- `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.91__seed_procurement_warehouse_flow_actions.sql`
  - 结果：通过，无输出。
  - 备注：动作表达式使用 `CONCAT('$', '{...}')`，避免 Flyway placeholder。
- `node -e "const fs=require('fs'); const s=fs.readFileSync(process.argv[1],'utf8'); const q=(s.match(/[']/g)||[]).length; const l=(s.match(/\\(/g)||[]).length; const r=(s.match(/\\)/g)||[]).length; console.log('single_quotes='+q); console.log('left_parens='+l); console.log('right_parens='+r); process.exit((q % 2 || l !== r) ? 1 : 0)" forge-server/db/migration/V1.0.91__seed_procurement_warehouse_flow_actions.sql`
  - 结果：通过，`single_quotes=1524`、`left_parens=166`、`right_parens=166`。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 工作目录：`forge-server`
  - 结果：通过，`BUILD SUCCESS`。
  - 警告：既有 deprecated/unchecked 编译警告，不阻断。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`built in 2m 20s`。
  - 警告：既有 Vite dynamic/static import chunk 提示、CSS `//` 注释 warning、`UserSelectModal` 命名冲突提示，不阻断。

**跳过项**

- 未实跑 Flowable 流程发起、审批通过、驳回回调：本轮没有启动 MySQL/Redis/后端/Flow 服务。
- 未做真实库存扣减端到端验收：已完成配置和构建验证，需在本地服务环境或测试环境人工验收。

**服务清理**

- 本轮没有启动长期运行的本地服务，无需清理 PID。

### 2026-07-03 Phase 4/5 详情区块、演示数据与原型对照

**变更范围**

- 新增 Flyway 迁移 `V1.0.92__seed_procurement_warehouse_detail_panels_demo_data.sql`。
- 给采购、出库、调拨、供应商报价运行配置补隐藏过滤字段，供详情关联面板调用 `/ai/crud/{configKey}/page` 时使用。
- 给仓库详情配置库存余额、数量流水、数量锁定、采购记录、出库记录、调入/调出记录面板。
- 给物料详情配置供应商报价、库存信息、近 3 次数量流水面板。
- 给采购详情配置供应商信息和附件占位面板，采购明细继续由主子表详情展示。
- Seed 原型演示数据：3 个物料、2 个供应商、2 个仓库、2 张采购单、2 张出库单、1 张调拨单、数量余额/流水/锁定数据。
- 新增 `prototype-acceptance.md`，记录采购、仓储、供应商、物料页面对照结论。

**执行命令与结果**

- `git diff --no-index --check /dev/null forge-server/db/migration/V1.0.92__seed_procurement_warehouse_detail_panels_demo_data.sql`
  - 结果：通过，无空白错误输出。
  - 备注：该命令与 `/dev/null` 对比新增文件，退出码 1 表示存在文件差异；无输出表示 `--check` 未发现空白错误。
- `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.92__seed_procurement_warehouse_detail_panels_demo_data.sql`
  - 结果：通过，无输出。
  - 备注：面板表达式使用 `CONCAT('$', '{row.id}')`。
- `node -e "const fs=require('fs'); const s=fs.readFileSync(process.argv[1],'utf8'); const q=(s.match(/[']/g)||[]).length; const l=(s.match(/\\(/g)||[]).length; const r=(s.match(/\\)/g)||[]).length; console.log('single_quotes='+q); console.log('left_parens='+l); console.log('right_parens='+r); process.exit((q % 2 || l !== r) ? 1 : 0)" forge-server/db/migration/V1.0.92__seed_procurement_warehouse_detail_panels_demo_data.sql`
  - 结果：通过，`single_quotes=1350`、`left_parens=261`、`right_parens=261`。
- `rg -n "detailPanels|quantity-balance|quantity-ledger|quantity-lock|pw-demo|warehouseId|materialId|JSON_SEARCH|pw_supplier_material/page|pw_purchase_order/page|ai_business_quantity_balance" forge-server/db/migration/V1.0.92__seed_procurement_warehouse_detail_panels_demo_data.sql`
  - 结果：通过，命中详情区块、数量区块、关联面板、隐藏过滤字段和 demo 数量数据。
- `rg -n "采购管理|仓储管理|供应商管理|物料管理|暂需后续平台 UI 能力" code-copilot/changes/lowcode-procurement-warehouse-app/prototype-acceptance.md`
  - 结果：通过，关键对照章节齐全。

**跳过项**

- 未实跑 Flyway 入库迁移：本轮没有启动本地 MySQL/Redis/后端服务。
- 未做浏览器截图验收：本轮新增的是 SQL seed 和 SDD 文档，前端运行时代码未变；已复用 Phase 3 前端构建结果。

**服务清理**

- 本轮没有启动长期运行的本地服务，无需清理 PID。

### 2026-07-03 ChildTableEditor 记录选择器空值修复

**变更范围**

- 修复 `ChildTableEditor.vue` 中 `activeSelectorChild` 初始为 `null` 时，`normalizeRecordSelectorConfig` 直接读取 `recordSelector` 导致页面渲染报错的问题。
- 记录选择器配置归一化新增 `null` / 非对象兜底，并兼容 `basicProps.recordSelector`、`props.recordSelector` 配置来源。

**执行命令与结果**

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`built in 2m 49s`。
  - 警告：既有 Vite dynamic/static import chunk 提示、CSS `//` 注释 warning、`UserSelectModal` 命名冲突提示，不阻断。

**跳过项**

- 未做浏览器端点击回归：本轮先以组件空值路径修复和生产构建验证为准。

**服务清理**

- 本轮没有启动长期运行的本地服务，无需清理 PID。

### 2026-07-03 用户反馈修复：自动编号、字典、选择器、流程入口和校验配置

**变更范围**

- 新增 Flyway 迁移 `V1.0.93__fix_procurement_warehouse_lowcode_runtime_config.sql`。
- 采购仓储内置字典：`pw_common_status`、`pw_warehouse_type`、`pw_order_status`。
- 补齐供应商、仓库、采购、出库、调拨编码规则；物料复用既有 `material_code` 编码规则。
- 物料、供应商、仓库、采购、出库、调拨编号字段改为后端自动生成，表单只读。
- 状态/仓库类型/单据状态改为字典下拉和列表字典标签。
- 子表隐藏 `materialId`、`warehouseId` 等内部 ID 字段，并补齐记录选择器上下文参数。
- 子表列宽按字段类型和业务含义设置最小宽度，避免金额、数量、库存列过窄。
- 采购/出库/调拨运行配置补齐 `businessObjectCode` / `objectCode`，并隐藏默认“发起主流程”动作，只保留自定义“提交审批”动作。
- 字段配置面板开放“最大长度”和“常用校验”，运行态表单支持手机号、邮箱、身份证、银行卡等预设正则校验。
- 低代码运行配置构建补齐字段 `validation`、主子表 `recordSelector` 和详情数量区块配置透传。

**执行命令与结果**

- `git diff --check -- forge-admin-ui/src/utils/validation-presets.js forge-admin-ui/src/components/ai-form/AiForm.vue forge-admin-ui/src/components/page-templates/ChildTableEditor.vue forge-admin-ui/src/components/ai-form/AiCrudPage.vue forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessDocumentRuntimeService.java`
  - 结果：通过，无空白错误输出。
- `git diff --no-index --check /dev/null forge-server/db/migration/V1.0.93__fix_procurement_warehouse_lowcode_runtime_config.sql`
  - 结果：通过，无空白错误输出。
  - 备注：该命令与 `/dev/null` 对比新增文件，退出码 1 表示存在文件差异；无输出表示 `--check` 未发现空白错误。
- `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.93__fix_procurement_warehouse_lowcode_runtime_config.sql`
  - 结果：通过，无输出。
  - 备注：编码规则模板使用 `CONCAT('$', '{...}')`，避免 Flyway placeholder。
- `node -e "const fs=require('fs'); const s=fs.readFileSync(process.argv[1],'utf8'); const q=(s.match(/[']/g)||[]).length; const l=(s.match(/\\(/g)||[]).length; const r=(s.match(/\\)/g)||[]).length; console.log('single_quotes='+q); console.log('left_parens='+l); console.log('right_parens='+r); process.exit((q % 2 || l !== r) ? 1 : 0)" forge-server/db/migration/V1.0.93__fix_procurement_warehouse_lowcode_runtime_config.sql`
  - 结果：通过，`single_quotes=1612`、`left_parens=536`、`right_parens=536`。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 工作目录：`forge-server`
  - 结果：通过，`BUILD SUCCESS`，总耗时 `24.179 s`。
  - 警告：既有 deprecated/unchecked 编译警告，不阻断。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`built in 2m 16s`。
  - 警告：既有 `UserSelectModal` 命名冲突、Vite dynamic/static import chunk 提示、CSS `//` 注释 warning，不阻断。

**跳过项**

- 未实跑 Flyway 入库迁移：本轮没有启动本地 MySQL/Redis/后端服务。
- 未做浏览器端人工点击验证：本轮已覆盖运行态组件构建和后端编译，端到端流程仍需在服务环境中验收。

**服务清理**

- 本轮没有启动长期运行的本地服务，无需清理 PID。

### 2026-07-03 二次反馈修复：设计器可见性、子表 ID、流程 key、字典保存

**变更范围**

- 常用校验配置选择后同步写入 `preset`、`pattern`、`message`，让用户点击后能直接看到正则和提示文案变化。
- 最大长度配置从折叠的高级位置前移到字段基础配置/新版画布属性面板的“字段约束”区域，并同步字段资产长度。
- 表单设计主画布增加“关系与级联”摘要和配置入口，业务对象设计器左侧导航默认展开，避免用户看不到主子表关系配置。
- 子表编辑器默认隐藏 `id`、`xxxId`、`xxx_id` 等内部主键/外键，除非字段显式声明 `showInChildEditor=true`。
- 新增 Flyway 迁移 `V1.0.94__fix_procurement_warehouse_designer_flow_relation_config.sql`，将采购、出库、调拨流程绑定和运行配置从旧 `leave_multi` 修正为 `pw_purchase_approval`、`pw_outbound_approval`、`pw_transfer_approval`。
- 表单设计保存时保留字段资产已有非空 `dictType`，采购单 `orderStatus` 兜底配置 `pw_order_status`，避免保存时报“字典字段必须配置字典类型”。

**执行命令与结果**

- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue forge-admin-ui/src/components/page-templates/ChildTableEditor.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectDesignerService.java forge-server/db/migration/V1.0.94__fix_procurement_warehouse_designer_flow_relation_config.sql`
  - 结果：通过，无空白错误输出。
- `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.94__fix_procurement_warehouse_designer_flow_relation_config.sql`
  - 结果：通过，无输出。
  - 备注：无 Flyway placeholder 风险。
- `node -e "const fs=require('fs'); const s=fs.readFileSync(process.argv[1],'utf8'); const q=(s.match(/[']/g)||[]).length; const l=(s.match(/\\(/g)||[]).length; const r=(s.match(/\\)/g)||[]).length; console.log('single_quotes='+q); console.log('left_parens='+l); console.log('right_parens='+r); process.exit((q % 2 || l !== r) ? 1 : 0)" forge-server/db/migration/V1.0.94__fix_procurement_warehouse_designer_flow_relation_config.sql`
  - 结果：通过，`single_quotes=994`、`left_parens=478`、`right_parens=478`。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 工作目录：`forge-server`
  - 结果：通过，`BUILD SUCCESS`，总耗时 `14.202 s`。
  - 警告：既有 deprecated/unchecked 编译警告，不阻断。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`built in 1m 54s`。
  - 警告：既有 `UserSelectModal` 命名冲突、Vite dynamic/static import chunk 提示、CSS `//` 注释 warning，不阻断。

**跳过项**

- 未实跑 Flyway 入库迁移：本轮没有启动本地 MySQL/Redis/后端服务。
- 未做浏览器端人工点击验证：本轮已覆盖前端构建、后端编译和迁移脚本静态检查；表单设计器点击链路仍需在服务环境中验收。
- 未实跑 Flowable 流程发起：本轮修正低代码绑定的业务流程 key，不负责部署 Flowable BPMN 定义；目标环境仍需确保 `pw_purchase_approval`、`pw_outbound_approval`、`pw_transfer_approval` 已部署。

**服务清理**

- 本轮没有启动长期运行的本地服务，无需清理 PID。
