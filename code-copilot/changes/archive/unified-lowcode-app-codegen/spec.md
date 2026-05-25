# 低代码应用管理与代码生成闭环整合
> status: propose
> created: 2026-05-25
> complexity: 🔴复杂

## 1. 背景与目标

当前 Forge 的低代码与代码生成能力已经形成多条入口：

- `低代码应用管理`：围绕业务领域、数据模型、应用搭建、预览、发布运行。
- `模型设计`：维护 `ai_lowcode_model`，支持模型字段、DDL 预览和应用选择模型。
- `AI 表单生成 / 应用配置管理`：围绕 `ai_crud_config` 直接生成 `searchSchema/columnsSchema/editSchema/apiConfig`，并支持高级 JSON 编辑和代码下载。
- `旧代码生成 / 表模型管理`：围绕 `gen_table/gen_table_column` 导入数据库表、配置字段、预览代码、下载代码。

这些能力底层都服务于同一个目标：用户描述需求后，平台生成业务模型、应用页面和可运行/可二次开发的代码。但目前入口分散，业务用户需要理解“模型设计、JSON 配置、表模型、代码生成、应用发布”等多个概念，开发者也需要在 `ai_crud_config` 与 `gen_table` 两套模型之间切换。

本次目标是整合为一条应用交付主链路，并保留模型管理作为独立资产入口：

`用户需求输入 → AI 业务模型自动生成 → 模型确认/设计 → 应用设计 → 预览发布 → 代码预览/下载 → 二次开发`

完成后应达到：

- 用户侧保留“应用管理/应用开发”作为应用设计、发布和代码生成主入口，不再暴露纯 JSON 配置管理和旧表模型管理作为主要工作台。
- 模型管理保留现有独立入口，作为统一模型资产中心；模型可以被应用引用，也可以作为不参与应用设计的独立业务模型存在。
- 数据模型设计成为唯一模型资产能力，吸收旧 `generator/table` 的表导入、字段配置、AI Schema 生成能力。
- 代码生成迁移到应用管理，基于低代码应用配置、数据模型、页面设计和模板文件生成前后端代码包，保留直接下载 ZIP 的能力。
- AI 表单生成迁移为“AI 应用生成”，不再以纯 JSON CRUD 配置为最终产物，而是生成低代码模型和应用草稿。
- 保留历史接口和旧数据的兼容读取能力，避免已发布应用和已有代码下载入口立即失效。

## 2. 代码现状（Research Findings）

### 2.1 低代码应用与模型入口

- `forge-admin-ui/src/views/ai/lowcode-apps.vue`：低代码应用列表页，按业务领域组织应用，支持新建应用、进入搭建器、打开运行态、迁移领域、导入/导出配置、删除。
- `forge-admin-ui/src/views/ai/lowcode-models.vue`：数据模型设计页，调用 `lowcodeModelPage/detail/create/update/delete`，使用 `LowcodeModelDesigner` 编辑 `modelSchema`，支持 `lowcodeDdlPreview` 预览建表 DDL。
- `forge-admin-ui/src/views/ai/lowcode-builder.vue`：应用搭建器，选择领域和模型，维护应用基础信息、页面配置、预览和发布。
- `forge-admin-ui/src/api/lowcode-crud.js`：低代码 API 已包含 app/domain/model 接口，也已经临时暴露 `genTablePage/genTableColumnList/genDatasourceEnabled`，说明模型设计与旧代码生成已有局部交叉。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/LowcodeAppController.java`：提供 `/ai/lowcode/app/page`、详情、草稿、预览、发布、版本、回滚、迁移领域。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/LowcodeModelController.java`：提供 `/ai/lowcode/model/page/list/{id}`、新增修改、启停、删除、校验、DDL 预览。

### 2.2 旧 AI CRUD 配置与纯 JSON 入口

- `forge-admin-ui/src/views/ai/crud-config.vue`：应用配置管理页，面向 `ai_crud_config`，提供“高级 JSON”“AI 生成”“预览”“下载”等操作。
- `forge-admin-ui/src/views/ai/crud-generator.vue`：AI 辅助生成页，使用流式对话生成 `searchSchema/columnsSchema/editSchema/apiConfig/createTableSql`，仍以纯 JSON 配置为核心产物。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/AiCrudConfigController.java`：提供 `/ai/crud-config/page`、CRUD、`/render/{configKey}`、`/ai/generate`、`/ai/generateFromTable`、`/codegen/download/{configKey}`。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiCrudConfigGenerateService.java`：AI 生成服务已能解析 `modelSchema/pageSchema`，并通过 `LowcodeRuntimeConfigBuilder` 补齐运行时配置，是迁移到低代码协议的重要基础。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/CrudGeneratorController.java` 与 `CrudGeneratorStreamService.java`：提供 `/ai/crud-generator/stream-generate` SSE，当前请求 DTO `StreamGenerateRequest` 仍围绕 `configKey/tableName/description/existingSearchSchema/...`。

### 2.3 旧代码生成与表模型入口

- `forge-admin-ui/src/views/generator/table.vue`：旧表模型管理页，调用 `/generator/list` 展示已导入表模型，提供列配置、代码预览、代码下载。
- `forge-admin-ui/src/views/generator/components/CodePreviewModal.vue`：代码预览弹窗，调用 `/generator/preview/{tableName}`，可复用于应用代码预览。
- `forge-admin-ui/src/views/generator/components/ImportTableModal.vue`：数据库表导入弹窗，调用 `/generator/datasource/enabled`、`/generator/datasource/{id}/tables`、`/generator/importTable/{datasourceId}`。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/GenController.java`：提供旧代码生成主接口，包括 `/generator/list`、`/importTable`、`/edit`、`/preview/{tableName}`、`/download/{tableName}`、`/executeSql`、`/ai/nlToSchema`、`/ai/importSchema`。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/GenTable.java` 与 `GenTableColumn.java`：旧代码生成表模型实体，仍承载导入表结构和字段配置。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/util/VelocityUtils.java`：旧代码生成模板上下文工具，围绕 `GenTable/GenTableColumn` 构建前后端代码所需变量。

### 2.4 代码下载与模板生成能力

- `AiCrudConfigController#downloadCode` 已提供 `/ai/crud-config/codegen/download/{configKey}`，输出 `{configKey}-code.zip`。
- `AiCrudCodegenService#generateZip` 根据 `configKey` 读取 `AiCrudConfig`，再按 `AiPageTemplate.codegenType` 选择 `CodegenStrategy` 生成 ZIP。
- `VelocityCodegenStrategy#generate` 已能基于 `AiCrudConfig` 和模板生成后端 Entity/Mapper/XML/Service/Controller/DTO/Query、SQL、前端 `index.vue/api.js`、配置 JSON。
- `GenController#download` 和 `GenController#preview` 基于 `GenTable` 生成旧 ZIP 和预览代码。

### 2.5 菜单与数据现状

- `forge/db/migration/V1.0.4__add_visual_lowcode_crud_builder.sql` 为 `ai_crud_config` 增加低代码字段，并初始化低代码应用、搭建器、发布、在线 DDL 等菜单。
- `forge/db/migration/V1.0.9__add_lowcode_data_model.sql` 新增低代码数据模型菜单和模型表能力。
- `forge/db/migration/V1.0.11__reparent_lowcode_menus.sql` 已将低代码菜单统一挂到原“AI 代码生成/AI 低代码”目录，并将“数据模型设计”指向 `/ai/lowcode-models`。

### 2.6 发现与风险

- 现有 `ai_crud_config` 仍是运行时事实来源，不能直接删除；应用管理应继续写入它，但不再把纯 JSON 编辑暴露为主流程。
- 旧 `GenTable` 和新 `LowcodeModelSchema` 字段语义接近但不等价，本期不把 `GenTable` 作为新模型导入来源，避免引入额外适配复杂度。
- 代码生成模板目前依赖数据库字段元数据和 `GenTableColumn`，应用级代码生成若直接复用 `VelocityCodegenStrategy`，必须先补齐从 `LowcodeModelSchema/pageSchema` 到模板上下文的转换。
- 菜单收敛涉及权限资源变更，必须通过 Flyway 迁移脚本更新 `sys_resource`，并保留旧路由兼容一段时间。
- AI 自动建表和代码生成涉及 DDL、权限菜单、后端代码输出，必须明确人工确认点，不能让 AI 直接执行高风险变更。

## 3. 功能点

### 3.1 信息架构整合

- [ ] “应用管理/应用开发”作为应用设计、AI 创建应用、发布运行和代码输出主入口。
- [ ] “模型管理/模型设计”保留现有独立入口，承载统一模型资产、数据库表导入和 AI 生成模型。
- [ ] 应用管理内提供模型选择、关联和快捷新建入口，但不要求所有模型都必须归属或绑定到某个应用。
- [ ] `generator/table` 表模型管理从菜单隐藏；数据库表导入、字段初始化迁移到模型设计，代码预览/下载迁移到应用管理，不再从旧表模型页发起。
- [ ] `ai/crud-config` 应用配置管理从菜单隐藏或仅保留开发者诊断入口，普通用户不能再新增/编辑纯 JSON 配置。
- [ ] `ai/crud-generator` 不再作为独立“AI 表单生成”入口，能力迁移为应用管理内的“AI 新建应用/AI 优化应用”。

### 3.2 统一模型设计

- [ ] 数据模型设计成为唯一模型资产，底层使用 `ai_lowcode_model.model_schema` 保存。
- [ ] 支持从数据库表导入为低代码模型：选择数据源 → 选择表 → 解析字段 → 生成 `LowcodeModelSchema` → 用户确认保存。
- [ ] 模型设计导入不从旧 `GenTable` 选择，直接读取数据源对应的数据表结构；`GenTable` 旧数据在新流程中忽略。
- [ ] 支持 AI 从自然语言生成模型：输入需求描述后生成业务对象、字段、关系、字典、安全策略和表名建议。
- [ ] 旧字段列配置能力迁移到 `LowcodeModelDesigner`：字段名、列名、数据类型、长度、小数位、必填、默认值、字典、脱敏、加密、导入导出、显示规则。
- [ ] 旧 `generator/column` 的字段推荐能力迁移到模型设计器，输入当前字段列表和领域上下文后返回字段优化建议。

### 3.3 AI 应用生成闭环

- [ ] 应用管理新增“AI 生成业务系统”入口。
- [ ] 用户只输入完整业务需求；业务领域、数据模型和应用模板由 AI Agent 自动规划，不要求用户提前选择领域或模板。
- [ ] 若需求命中已有启用业务领域则复用；否则返回待确认的新领域草稿，确认后再保存。
- [ ] 后端按领域上下文、现有模型、字段模板、字典策略、安全策略构建 Prompt。
- [ ] AI 输出低代码协议，不再以纯 JSON CRUD 配置为主产物：
  - `domainSuggestion`
  - `domains[]`
  - `models[]`
  - `apps[]`
  - `steps[]`
  - `decisions[]`
  - `appDraft`（兼容旧单应用调用）
  - `modelSchema`
  - `pageSchema`
  - `ddlPreview`
  - `generationNotes`
- [ ] 用户确认后保存数据模型和应用草稿，进入应用设计器继续调整。
- [ ] 生成过程以 SSE 展示“需求理解 → 领域划分 → 模型生成 → 页面生成 → 协议校验”的可解释进度；前端展示决策摘要，不暴露不可审计的内部推理链。
- [ ] 支持 AI 优化现有应用：基于当前模型和页面草稿提出字段、布局、查询、列表、详情、权限、字典建议。
- [ ] AI 生成失败时使用规则引擎降级，至少能从表结构或模型字段生成基础应用草稿。

### 3.4 应用设计与运行

- [ ] 应用设计器继续维护页面布局、查询条件、列表列、表单、详情、操作列、导入导出和发布配置。
- [ ] 应用设计器可从模型资产引入字段，模型字段变更后支持“同步到应用草稿”并显示差异。
- [ ] 运行时仍使用 `/ai/crud-page/{configKey}` 和 `/ai/crud/{configKey}`，不破坏已发布链接。
- [ ] 发布流程仍生成运行时 `AiCrudConfig`、版本快照、菜单资源和动态 CRUD 配置。

### 3.5 应用级代码预览与下载

- [ ] 应用管理列表和应用设计器均提供“代码预览”和“下载代码”。
- [ ] 代码生成源支持：
  - 当前已保存草稿 `DRAFT`，作为默认来源
  - 已发布版本 `PUBLISHED`
  - 指定历史版本 `VERSION`
- [ ] 代码预览以文件树 + 编辑器方式展示，复用或重构 `CodePreviewModal`。
- [ ] 下载 ZIP 保留直接下载能力，文件名建议 `{configKey}-code.zip`。
- [ ] 代码生成应基于应用配置、模型协议、页面协议和模板文件生成：
  - 后端 Entity / DTO / Query / Mapper / Mapper.xml / Service / ServiceImpl / Controller
  - 前端 API 文件 / Vue 页面
  - SQL 菜单脚本 / 字典脚本 / 可选建表脚本
  - 原始低代码配置 JSON
- [ ] 代码生成参数可配置：Maven groupId、Java 基础包名、代码模块名、作者、是否包含 SQL、是否包含菜单资源、是否包含字典脚本、目标前端路径。
- [ ] 生成代码只下载，不直接写入当前仓库，不自动执行 SQL。

### 3.6 旧能力兼容与退场

- [ ] 旧 `/ai/crud-config/codegen/download/{configKey}` 保留兼容，内部委托新的应用级代码生成服务。
- [ ] 旧 `/generator/preview/{tableName}`、`/generator/download/{tableName}` 保留接口兼容，但菜单隐藏；后续版本再评估删除。
- [ ] 旧 `ai/crud-config` 页面仅允许拥有开发者权限的用户访问，用于排障和历史配置查看，不再作为新增入口。
- [ ] 旧 `ai/crud-generator` 页面隐藏菜单，入口迁移到应用管理；路由可保留跳转到新 AI 应用生成面板。
- [ ] 已有 `GenTable` 数据不删除，但不进入新模型设计导入流程；旧数据可忽略，仅保留旧接口兼容。

## 4. 业务规则

- 应用是面向应用交付的主聚合；页面、发布、运行和代码输出都围绕应用管理组织。
- 模型是独立业务资产，可以被一个或多个应用引用，也可以不参与应用设计。
- 数据模型是唯一业务模型资产；旧 `GenTable` 不作为导入来源，仅保留旧接口兼容或历史数据。
- `ai_crud_config` 仍是运行时配置存储，不作为用户直接编辑的产品概念。
- 应用必须归属业务领域；历史未归属应用展示在“未归属”分组，允许迁移。
- 一个应用至少绑定一个主模型；首期代码生成只支持单表/单主模型。
- 主子表、左树右表、单表树形和多模型代码生成后续支持，首期不纳入生成范围。
- AI 生成的模型和应用必须经过用户确认才保存；DDL 必须二次确认才允许执行。
- 代码生成默认使用已保存应用草稿 `DRAFT`，发布版本 `PUBLISHED` 和历史版本 `VERSION` 作为可选来源；不允许基于浏览器未保存状态直接下载。
- 代码生成包的目标包名按业务领域配置，不使用全局固定包名作为默认值。
- 代码 ZIP 中禁止包含真实密钥、数据库密码、Token、AK/SK。
- 生成 SQL 的业务内置数据 `tenant_id` 必须为 `1`，权限资源插入必须 `NOT EXISTS` 防重复。
- 字典字段必须绑定 `sys_dict_type/sys_dict_data`，禁止生成前端硬编码选项。
- 图片字段仍存储 fileId，生成前端代码必须使用 `AuthImage` 或现有文件访问归一化工具。
- 旧接口兼容期间，新增能力优先走 `/ai/lowcode/**`，不再扩展 `/generator/**` 的业务语义。

## 5. 数据变更

本变更优先复用现有表，减少结构变更。

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 复用 | `ai_lowcode_model` | `model_schema` | 作为唯一模型资产，承载从 AI/数据库表导入后的模型协议 |
| 复用 | `ai_lowcode_domain` | `domain_schema` | 增加 `codegen` 领域配置，保存默认 groupId、Java 基础包名、代码模块名、前端路径等代码生成默认值 |
| 复用 | `ai_crud_config` | `model_schema`, `page_schema`, `options`, `build_mode`, `publish_status` | 作为低代码应用草稿和运行时配置，不再面向用户做纯 JSON 编辑 |
| 复用 | `ai_crud_config_version` | 现有版本字段 | 作为发布版本和代码生成 `VERSION` 来源 |
| 复用 | `gen_table`, `gen_table_column` | 全量保留 | 只作为旧接口兼容和历史数据，不作为新模型设计导入来源 |
| 修改 | `sys_resource` | 菜单/按钮资源 | 通过新 Flyway 脚本隐藏或重定向旧入口，新增应用级代码预览/下载/AI 生成权限 |
| 可选新增 | `sys_dict_type/sys_dict_data` | `ai_prompt_usage_scene` 新场景 | 如现有字典缺少“应用生成/模型生成/代码生成”场景，则新增字典项 |

### 5.1 菜单迁移规则

- “应用管理/应用开发”保留为主菜单，组件指向 `ai/lowcode-apps`。
- “模型管理/模型设计”保留现有独立菜单，普通用户可继续维护不参与应用设计的模型资产。
- “低代码搭建器”保留隐藏路由 `/ai/lowcode-builder/:id?`，通过应用管理进入。
- “AI 辅助生成 / AI 表单生成”菜单隐藏或重定向到应用管理的 AI 生成面板。
- “应用配置管理 / 高级 JSON”菜单隐藏，只给开发者权限可见。
- “代码生成表管理”菜单隐藏，表导入迁移到模型设计。
- “数据源管理”保留为开发者菜单。
- “模板管理”菜单去掉；应用级代码生成继续使用后端模板文件或内置策略，不提供独立模板管理入口。

## 6. 接口变更

### 6.1 新增应用级接口

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/lowcode/app/ai/stream-generate` | POST SSE | 从用户需求生成模型建议和应用草稿，替代旧 `/ai/crud-generator/stream-generate` 用户入口 |
| 新增 | `/ai/lowcode/app/{id}/ai/refine` | POST SSE | 基于已有应用草稿进行 AI 优化 |
| 新增 | `/ai/lowcode/app/{id}/code/preview` | GET | 基于应用草稿/发布版本预览代码文件 Map |
| 新增 | `/ai/lowcode/app/{id}/code/download` | GET | 下载应用代码 ZIP |
| 新增 | `/ai/lowcode/app/{id}/code/options` | GET/PUT | 查询/保存应用代码生成参数，存入 `options.codegen` |

### 6.2 新增模型导入与迁移接口

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/lowcode/model/import-db-table` | POST | 从数据源表结构生成并保存低代码模型 |
| 新增 | `/ai/lowcode/model/preview-db-table` | POST | 从数据源表结构预览模型，不保存 |
| 新增 | `/ai/lowcode/model/ai/stream-generate` | POST SSE | 从自然语言生成模型协议 |
| 新增 | `/ai/lowcode/model/{id}/ai/recommend-fields` | POST | 字段推荐/优化，迁移旧 `recommendColumns` 能力 |

### 6.3 修改现有接口

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 修改 | `/ai/lowcode/app/page` | GET | 增加 `sourceType`、`hasCodegen`、`modelId` 过滤；返回代码生成状态摘要 |
| 修改 | `/ai/lowcode/app/draft` | POST | 支持保存 `options.codegen` 和 AI 生成来源元数据 |
| 修改 | `/ai/lowcode/app/{id}` | GET | 返回模型快照、代码生成选项、可下载状态 |
| 修改 | `/ai/lowcode/model/page` | GET | 增加 `importSource`、`tableName` 过滤，展示是否由数据源表或 AI 生成 |

### 6.4 兼容但不再扩展的接口

| 接口 | 兼容策略 |
|------|----------|
| `/ai/crud-config/codegen/download/{configKey}` | 保留，内部按 `configKey` 查应用并委托应用级代码生成；找不到低代码应用时走旧逻辑 |
| `/ai/crud-generator/stream-generate` | 保留，前端入口隐藏；后续只用于历史页面或开发者调试 |
| `/generator/preview/{tableName}` | 保留旧表模型预览 |
| `/generator/download/{tableName}` | 保留旧表模型下载 |
| `/generator/importTable/**` | 保留底层能力，但新前端从 `/ai/lowcode/model/import-db-table` 进入 |

## 7. 技术方案

### 7.1 后端服务分层

- 新增 `LowcodeAiGenerateService`：统一 AI 需求生成、模型生成、应用生成和应用优化。
- 新增 `LowcodeCodegenService`：应用级代码预览/下载入口，封装草稿/发布/版本来源选择。
- 新增 `LowcodeModelImportService`：从数据库表和 AI Schema 转换为 `LowcodeModelSchema`；不从旧 `GenTable` 导入。
- 新增 `LowcodeCodegenContextBuilder`：把 `LowcodeModelSchema + LowcodePageSchema + AiCrudConfig` 转换为模板引擎上下文，替代直接依赖 `GenTableColumn`。
- 保留 `AiCrudCodegenService`，但逐步改造成底层策略服务；应用级接口不直接暴露 `configKey` 下载。
- 保留 `VelocityCodegenStrategy`，第一期优先通过上下文适配复用模板，不重写全部模板。

### 7.2 前端页面整合

- `lowcode-apps.vue`：应用管理主工作台，新增 AI 创建应用、代码预览、下载代码、选择/关联模型入口。
- `lowcode-builder.vue`：应用设计器新增“代码输出”区域，可选择草稿/发布版本预览和下载。
- `lowcode-models.vue`：保留为独立模型管理入口，新增从数据库表导入、AI 生成模型。
- `lowcode-crud.js`：新增应用级 AI、代码生成、模型导入接口。
- 复用/迁移 `CodePreviewModal.vue`：改造为可接收 `files` 或应用 ID，避免只绑定 `tableName`。
- 复用/迁移 `ImportTableModal.vue`：将结果落到 `ai_lowcode_model`，不再只创建 `gen_table`。
- 隐藏 `crud-config.vue`、`crud-generator.vue`、`generator/table.vue` 普通菜单入口；路由保留兼容。

### 7.3 低代码协议与代码生成

代码生成源统一为：

```text
AiCrudConfig
  ├─ modelSchema: LowcodeModelSchema
  ├─ pageSchema: LowcodePageSchema
  ├─ searchSchema / columnsSchema / editSchema / apiConfig
  ├─ dictConfig / desensitizeConfig / encryptConfig / transConfig
  └─ options.codegen
```

生成服务必须支持两种上下文：

- **运行时配置上下文**：用于现有 `AiCrudPage` 动态运行。
- **代码模板上下文**：用于生成可复制到业务模块的 Java/Vue/SQL 文件。

模型导入只读取数据源表结构：

```text
数据源 → 数据库表结构
  → LowcodeModelImportService
  → LowcodeModelSchema
  → LowcodeDataModel
```

旧 `GenTable/GenTableColumn` 不进入新模型导入流程，仅保留旧接口兼容。

### 7.4 AI 生成流程

```text
用户需求描述
  → 选择领域/AI模型
  → LowcodeAiGenerateService 构建 Prompt
  → AI 输出模型和应用协议
  → LowcodeSchemaValidator 校验
  → LowcodeRuntimeConfigBuilder 生成运行时配置
  → 用户确认保存模型和应用草稿
  → 进入应用设计器
  → 预览/发布/代码下载
```

失败降级：

- AI 不可用：基于用户输入生成最小空模型草稿，引导用户手动补字段。
- AI 输出 JSON 解析失败：返回可读错误和原始片段，不保存。
- 模型校验失败：展示字段级错误，允许用户修正后再保存。
- 代码生成失败：返回模板文件、字段、配置键等定位信息，不影响应用运行。

## 8. 影响范围

### 8.1 前端

- `forge-admin-ui/src/views/ai/lowcode-apps.vue`
- `forge-admin-ui/src/views/ai/lowcode-builder.vue`
- `forge-admin-ui/src/views/ai/lowcode-models.vue`
- `forge-admin-ui/src/api/lowcode-crud.js`
- `forge-admin-ui/src/components/lowcode-builder/**`
- `forge-admin-ui/src/views/generator/components/CodePreviewModal.vue`
- `forge-admin-ui/src/views/generator/components/ImportTableModal.vue`
- `forge-admin-ui/src/views/ai/crud-config.vue`
- `forge-admin-ui/src/views/ai/crud-generator.vue`
- `forge-admin-ui/src/views/generator/table.vue`

### 8.2 后端

- `LowcodeAppController` / `LowcodeAppService`
- `LowcodeModelController` / `LowcodeDataModelService`
- `LowcodeRuntimeConfigBuilder`
- `LowcodeSchemaValidator`
- `AiCrudCodegenService`
- `VelocityCodegenStrategy`
- `AiCrudConfigGenerateService`
- `CrudGeneratorStreamService`
- `GenController`
- `GenTableServiceImpl`
- `VelocityUtils`

### 8.3 数据与权限

- Flyway 菜单迁移脚本
- `sys_resource` 菜单和按钮权限
- `ai_prompt_usage_scene` 字典项（如需要）
- 旧 `gen_table/gen_table_column` 只读兼容与迁移

## 9. 风险与关注点

- ⚠️ **权限资源变更**：菜单隐藏、按钮新增、发布菜单和代码下载权限涉及 `sys_resource`，必须人工审查。
- ⚠️ **DDL 风险**：AI 生成 DDL 和旧 `/generator/executeSql` 不能自动执行，必须二次确认。
- ⚠️ **代码生成安全**：ZIP 不得包含真实密钥、数据库密码、Token、AK/SK。
- ⚠️ **兼容风险**：已发布 `configKey`、运行时 URL 和历史代码下载接口必须可用。
- ⚠️ **模板风险**：旧 Velocity 模板依赖 `GenTableColumn`，迁移时必须建立明确上下文适配，不能在模板里塞低代码 JSON 后临时解析。
- ⚠️ **用户认知风险**：入口收敛后，旧页面跳转和提示必须明确，否则用户可能找不到原“下载代码”能力。

## 10. 测试策略

- **测试范围**：
  - 应用管理 AI 创建应用流程
  - 模型从数据库表导入
  - 旧 `GenTable` 数据不进入新模型导入流程
  - 应用草稿预览/发布
  - 应用级代码预览/下载
  - 旧下载接口兼容
  - 菜单权限和路由可访问性
- **覆盖率目标**：
  - 后端新增服务核心分支单元测试覆盖：成功、AI 失败、校验失败、旧数据兼容、代码生成失败。
  - 前端至少覆盖 API 封装、关键转换函数、主流程手工验证。
- **独立 Test Spec**：建议是。进入 `/apply` 前补充 `code-copilot/changes/unified-lowcode-app-codegen/test-spec.md`。

## 11. 已澄清事项

- [x] 模型管理保留现有独立入口；模型可以不参与应用设计，应用管理只提供模型引用和快捷创建能力。
- [x] 数据源管理保留为开发者菜单；模板管理菜单去掉。
- [x] 应用级代码下载默认使用已保存草稿 `DRAFT`；发布版本 `PUBLISHED` 和历史版本 `VERSION` 作为可选项。
- [x] AI 自动生成后不自动保存；必须由用户确认后保存模型和应用草稿。
- [x] 模型设计导入直接读取数据源对应的数据表，不从旧 `GenTable` 中选取；`GenTable` 表旧数据可以忽略。
- [x] 代码生成包的目标包名按业务领域配置。
- [x] 首期只支持单表/单主模型代码生成；主子表、左树右表、单表树形后续支持。

## 12. 技术决策

- 以低代码应用管理作为应用设计、发布和代码生成主入口，旧 `ai_crud_config` 和 `gen_table` 不再作为用户主工作台。
- 模型管理保留独立入口，统一承载 `ai_lowcode_model` 模型资产；模型不强制绑定应用。
- `ai_crud_config` 继续作为运行时配置存储，避免重写动态 CRUD 内核。
- `ai_lowcode_model` 成为唯一模型资产，旧 `GenTable` 不作为新模型导入来源。
- 代码生成从“表模型生成”升级为“应用配置生成”，但第一期复用 Velocity 模板和现有策略模式。
- 旧接口保留兼容，不在本变更中物理删除表和 Controller。
- 菜单变更全部通过 Flyway 脚本，禁止手改数据库。

## 13. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Spec 草案 | done | `code-copilot/changes/unified-lowcode-app-codegen/spec.md` | 仅设计 Spec，未写实现代码 |

## 14. 审查结论

待 `/review unified-lowcode-app-codegen` 后填写。

## 15. 确认记录（HARD-GATE）

- **确认时间**：2026-05-25
- **确认人**：用户确认
- **已确认事项**：
  - 模型管理保留现有独立入口。
  - 模型允许不参与应用设计，应用管理只引用或快捷创建模型。
  - 数据源管理保留为开发者菜单，模板管理菜单去掉。
  - 应用级代码下载默认使用已保存草稿，发布版本和历史版本作为可选项。
  - AI 生成结果必须用户确认后保存。
  - 新模型设计导入只读取数据源表结构，不从 `GenTable` 选择。
  - 代码生成包目标包名按业务领域配置。
  - 首期只支持单表/单主模型代码生成，主子表、左树右表、单表树形后续支持。
- **进入 /apply 前必须确认**：无，本轮待澄清事项已确认。
