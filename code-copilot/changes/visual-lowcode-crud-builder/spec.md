# AI 低代码 CRUD 可视化搭建体系
> status: propose
> created: 2026-05-19
> complexity: 🔴复杂

## 1. 背景与目标
当前 AI CRUD 模块已经具备自然语言生成配置、配置管理、动态 CRUD 渲染和代码包下载能力，但主要面向技术人员：用户仍需要理解表名、字段名、JSON Schema、接口配置、菜单注册和建表 SQL。新目标是将这些技术细节封装为业务人员可理解的“业务对象、页面组件、字段属性、发布配置”，形成零代码全链路：

`可视化数据模型设计 → 拖拽式页面搭建 → 实时效果预览 → 一键在线部署发布`

完成后应达到：
- 业务人员不需要手写代码和 JSON，即可创建一个完整业务管理应用。
- 平台内置业务组件库可拖拽拼装查询筛选区、数据录入表单、数据展示列表、批量导入、数据导出、批量删除、自定义查询等常见业务能力。
- AI 继续作为辅助能力，但生成结果进入可视化模型和页面协议，而不是让用户直接编辑技术配置。
- 运行时继续复用现有 `AiCrudPage`、动态 CRUD、字典、脱敏、加解密、菜单注册能力，避免重做已成熟链路。
- 第一版聚焦单表 CRUD 应用，支持受控在线创建新业务表；主子表、流程编排和跨表聚合暂不纳入第一版。

## 2. 代码现状（Research Findings）
### 2.1 相关入口与链路
- `forge-admin-ui/src/views/ai/crud-generator.vue`：当前 AI CRUD 生成器入口，左侧为对话和历史会话，右侧为配置面板，生成阶段包括元数据、搜索、表格、编辑表单、接口和建表 SQL。
- `forge-admin-ui/src/views/ai/crud-config.vue`：CRUD 配置管理页，当前编辑方式仍以基础信息 + 多个 Schema/JSON 面板为主。
- `forge-admin-ui/src/views/ai/crud-page.vue`：动态渲染页，根据 `configKey` 调用 `/ai/crud-config/render/{configKey}`，转换字典、列渲染和模板组件后渲染 `AiCrudPage`。
- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`：现有通用 CRUD 渲染核心，已支持搜索区、表格、弹窗/抽屉表单、批量删除、导入按钮、导出按钮、自定义查询、列表/卡片模式。
- `forge-admin-ui/src/components/form-designer/FormDesigner.vue`：已有 Naive UI 风格拖拽表单设计器雏形，左侧组件面板、中间画布、右侧属性面板均已存在，但尚未接入 CRUD 配置保存和整页搭建。
- `forge-admin-ui/src/catalog/index.js` 与 `forge-admin-ui/src/components/page-templates/*.vue`：当前页面模板注册机制已有 `simple-crud` 和 `tree-crud`，可以继续作为低代码页面模板和运行时渲染扩展点。

### 2.2 现有后端实现
- `AiCrudConfig` 目前持久化 `configKey/tableName/tableComment/searchSchema/columnsSchema/editSchema/apiConfig/options/dictConfig/desensitizeConfig/encryptConfig/transConfig/layoutType`，缺少业务模型协议、页面布局协议、草稿/发布状态、版本快照。
- `AiCrudConfigService#createConfig` 在创建配置时会立即保存并尝试注册菜单，当前没有“草稿”和“发布”分离。
- `AiCrudConfigService#getRenderConfig` 运行时强制生成动态 CRUD API，使用 `:id` 占位符，能规避 AI 生成 `{id}` 的历史问题。
- `DynamicCrudController` 已提供 `/ai/crud/{configKey}/page`、详情、新增、修改、删除接口，并统一加了 `@ApiEncrypt` / `@ApiDecrypt`。
- `DynamicCrudService` 和 `DynamicCrudRepository` 通过配置驱动动态读写表，已有字段白名单、表名校验、列映射、脱敏、字典翻译和字段加解密处理。
- `CrudGeneratorStreamService` 已支持 SSE 流式阶段输出，并可注入表结构和页面模板约束。
- `AiCrudConfigGenerateService#buildDescriptionPrompt` 仍在提示词示例中使用 `{id}`，与 AiCrudPage `:id` 占位符规则不一致，需要在本次改造中修正。

### 2.3 外部方案调研
- `form-create-designer` 是开源 Vue 低代码表单设计器，仓库说明为 MIT License，支持拖拽、JSON 表单规则、扩展组件、表单验证、布局和 AI 助手等能力。
- 该方案的 Vue3 版本主要面向 Element Plus、Ant Design Vue 和 Vant。本项目主技术栈是 Naive UI，因此直接引入会带来第二套 UI 组件库、样式和打包体积成本。
- 第一版确认采用 Naive UI 自研搭建器路线，优先复用并增强现有 `FormDesigner`，抽象 `FormDesignerAdapter`。后续如确需引入 `form-create-designer`，通过隔离适配器接入，不让 Element Plus/Antd 依赖污染现有 CRUD 运行时。

### 2.4 发现与风险
- 现有能力更像“AI 生成 JSON 配置”，不是“业务应用搭建器”；需要新增业务建模协议和整页布局协议。
- 在线部署若包含自动建表/改表，属于高风险 DDL 操作，必须加权限、SQL 安全校验、操作日志和发布确认。
- 当前动态 CRUD 依赖动态表名和动态列名，无法完全按普通业务 SQL 写 Mapper XML；新增查询类管理接口仍必须写 Mapper XML，动态 DDL/CRUD 仅限经过白名单校验的仓储层。
- 业务数据和系统资源初始化脚本必须使用 `tenant_id=1`，新增迁移脚本必须做 `NOT EXISTS` 和 `information_schema` 防重复保护。
- 发布会创建或更新菜单资源，属于权限影响范围，必须在 Spec 中标注并经人工确认后进入 `/apply`。

## 3. 功能点
- [ ] 新增“低代码应用搭建器”入口，以四步工作台呈现：数据模型、页面搭建、实时预览、发布上线。
- [ ] 数据模型设计器支持新增/编辑字段、字段类型、长度、必填、默认值、字典、枚举、敏感级别、加密策略、是否搜索、是否列表展示、是否表单录入。
- [ ] 数据模型设计器支持两种模式：绑定已有表、创建新业务表。创建新表时生成 DDL 预览，发布时按权限执行。
- [ ] 页面搭建器支持拖拽内置业务组件：查询筛选区、数据表格、编辑表单、详情区、工具栏、批量导入、数据导出、批量删除、自定义查询。
- [ ] 组件属性面板以业务语言配置显示标题、字段来源、控件类型、排序、宽度、校验规则、字典映射、按钮显示、导入导出开关。
- [ ] 实时预览将草稿 `modelSchema + pageSchema` 转换为 `AiCrudPage` 所需运行时配置，不要求保存发布即可预览。
- [ ] 一键发布将草稿转换为正式 `AiCrudConfig`，保存发布版本，必要时执行安全 DDL，注册或更新菜单，启用运行时页面。
- [ ] 支持版本记录和回滚：每次发布保留快照，回滚后恢复配置并更新菜单。
- [ ] AI 生成升级为生成业务模型和页面布局协议，并允许对选中组件或字段做局部优化。
- [ ] 保留现有代码包下载能力，但降级为技术人员入口，业务人员默认只看到可视化搭建和发布。

## 4. 业务规则
- `configKey` 必须小写字母开头，仅允许小写字母、数字、下划线，长度 2-64。
- 业务表名必须小写下划线，禁止使用系统表名前缀和 SQL 保留字。
- 自动创建的新业务表必须包含 `id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`，建议包含 `del_flag`。
- 模型字段不能直接暴露基础审计字段给表单录入。
- 字典字段必须通过字典组件/字典配置驱动，禁止生成硬编码选项。
- 图片字段存储 fileId，列表预览必须使用 `AuthImage` 或运行时文件 URL 归一化方案。
- 页面发布前必须通过模型校验、页面协议校验、API 占位符校验和权限校验。
- 草稿不会注册菜单，不会对业务用户可见。
- 发布后菜单路径统一为 `/ai/crud-page/{configKey}`，组件为 `ai/crud-page`。
- 删除已发布应用前，如果菜单已被角色授权，仍沿用现有规则禁止直接删除。
- 在线 DDL 发布必须具备独立权限，例如 `ai:lowcode:deploy-ddl`，并记录操作日志。
- 第一版支持在线创建新表，不支持业务人员在线删除表；在线改表仅允许受控追加字段，不允许删除/改名字段。
- 第一版仅支持单表 CRUD，所有字段必须来自同一个业务表。
- 发布菜单默认挂载到 `AI管理` 目录；后续可扩展为管理员选择父菜单。
- 发布、回滚、删除属于权限影响操作，进入 `/apply` 前需要人工确认。

## 5. 数据变更
| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 修改 | `ai_crud_config` | `app_name`, `build_mode`, `model_schema`, `page_schema`, `publish_status`, `draft_version`, `published_version`, `publish_time`, `publish_by` | 支持低代码草稿、页面协议和发布状态 |
| 新增 | `ai_crud_config_version` | `config_id`, `config_key`, `version_no`, `version_type`, `model_schema`, `page_schema`, `search_schema`, `columns_schema`, `edit_schema`, `api_config`, `options`, `publish_snapshot`, `remark` | 保存发布/回滚版本快照 |
| 新增/更新 | `sys_resource` | 低代码搭建器菜单、低代码应用管理菜单、发布权限按钮 | 新增脚本必须 `tenant_id=1` 且防重复 |

### 5.1 `modelSchema` 协议
```json
{
  "tableMode": "EXISTING",
  "tableName": "biz_contract",
  "businessName": "合同管理",
  "fields": [
    {
      "field": "contractName",
      "columnName": "contract_name",
      "label": "合同名称",
      "dataType": "varchar",
      "length": 128,
      "required": true,
      "searchable": true,
      "listVisible": true,
      "formVisible": true,
      "componentType": "input",
      "queryType": "like"
    }
  ]
}
```

### 5.2 `pageSchema` 协议
```json
{
  "layoutType": "simple-crud",
  "zones": [
    {
      "zoneKey": "search",
      "componentKey": "search-form",
      "enabled": true,
      "fieldRefs": ["contractName", "status"]
    },
    {
      "zoneKey": "table",
      "componentKey": "data-table",
      "enabled": true,
      "fieldRefs": ["contractName", "amount", "status"],
      "props": {
        "showImport": true,
        "showExport": true,
        "hideBatchDelete": false
      }
    },
    {
      "zoneKey": "edit",
      "componentKey": "edit-form",
      "enabled": true,
      "fieldRefs": ["contractName", "amount", "status"]
    }
  ]
}
```

## 6. 接口变更
| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 新增 | `/ai/lowcode/app/page` | GET | 低代码应用分页，参数使用 `pageNum/pageSize` |
| 新增 | `/ai/lowcode/app/{id}` | GET | 获取应用草稿和发布信息 |
| 新增 | `/ai/lowcode/app/draft` | POST | 新建或保存草稿 |
| 新增 | `/ai/lowcode/app/{id}/preview` | POST | 根据草稿协议返回运行时预览配置 |
| 新增 | `/ai/lowcode/app/{id}/publish` | POST | 发布应用，生成版本、菜单和运行时配置 |
| 新增 | `/ai/lowcode/app/{id}/versions` | GET | 查询发布版本 |
| 新增 | `/ai/lowcode/app/{id}/rollback/{versionId}` | POST | 回滚到指定版本 |
| 新增 | `/ai/lowcode/model/validate` | POST | 校验模型协议 |
| 新增 | `/ai/lowcode/model/ddl/preview` | POST | 生成安全 DDL 预览 |
| 新增 | `/ai/crud/{configKey}/import` | POST | 动态 CRUD 批量导入 |
| 新增 | `/ai/crud/{configKey}/export` | POST | 动态 CRUD 数据导出 |
| 新增 | `/ai/crud/{configKey}/import-template` | GET | 动态 CRUD 导入模板下载 |
| 修改 | `/ai/crud-generator/stream-generate` | POST | 支持输出 `modelSchema` 和 `pageSchema` 阶段 |

## 7. 影响范围
- `forge-plugin-generator`：配置实体、版本表、低代码服务、发布服务、动态 CRUD 导入导出、AI 生成提示词。
- `forge-admin-server`：菜单注册适配器可能需要支持发布态菜单更新和低代码权限资源。
- `forge-admin-ui`：新增低代码搭建器工作台、模型设计器、页面搭建器、预览和发布入口；现有 `crud-generator` 和 `crud-config` 需要调整入口定位。
- `AiCrudPage`：需要补齐由低代码协议驱动的导入导出 URL、图片字段、字典字段和布局配置映射。
- 数据库迁移：新增 Flyway 版本脚本，初始化菜单和按钮权限。

## 8. 风险与关注点
- ⚠️ 涉及权限变更：发布应用会创建或更新菜单资源；删除/回滚会影响菜单可见内容。
- ⚠️ 涉及状态流转：草稿、已发布、已停用、回滚需要明确状态机，禁止随意 set 状态。
- ⚠️ 涉及 DDL：在线创建表必须独立权限、SQL 白名单、安全预览、操作日志和失败回滚说明。
- 不涉及资金变更。
- 直接引入 `form-create-designer` 可能带来第二套 UI 组件库，第一版不直接引入运行时依赖。
- 动态 CRUD 导入导出需要严格限制字段白名单，避免用户上传或导出基础审计字段、加密字段原文。
- 需要避免 AI 输出 `{id}` 占位符、硬编码字典选项、`tenant_id=0` 初始化数据。

## 8.5 测试策略
- **测试范围**：模型协议校验、运行时配置转换、发布/回滚版本、动态 CRUD 导入导出、前端搭建器交互、预览渲染、菜单注册。
- **覆盖率目标**：新增协议转换、DDL 生成、发布状态机、版本回滚、导入导出字段白名单需要覆盖核心分支。
- **独立 Test Spec**：是，进入 `/apply` 后补充 `code-copilot/changes/visual-lowcode-crud-builder/test-spec.md`。
- **验证命令**：
  - 后端：`mvn -pl forge-admin-server -am compile -DskipTests`
  - 前端：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build`
  - 前端交互：启动 `pnpm dev` 后用浏览器验证模型设计、拖拽搭建、预览、发布流程。

## 9. 待澄清
- [x] 第一版支持受控在线创建新表，发布时需要 `ai:lowcode:deploy-ddl` 权限和二次确认。
- [x] 发布菜单第一版默认挂在 `AI管理` 下，先不开放业务人员任意选择父菜单。
- [x] 导入导出第一版实现真实 Excel 能力，复用后端 Excel 能力输出基础模板和数据，排在 P1。
- [x] 第一版聚焦单表 CRUD，不做主子表/明细行。
- [x] 第一版坚持 Naive UI 自研搭建器，不引入 `form-create-designer` 及 Element Plus/Antd 相关运行时依赖。

## 10. 技术决策
- 第一版采用“增强现有 Naive UI 搭建器 + 适配层”的路线，不直接把 `form-create-designer` 作为运行时依赖。
- 低代码协议分为 `modelSchema` 和 `pageSchema`，运行时由后端转换为现有 `searchSchema/columnsSchema/editSchema/options/apiConfig`。
- 草稿和发布分离：草稿只保存协议，发布才写入正式运行时配置并注册菜单。
- 版本快照单独建表，避免 `ai_crud_config` 过度膨胀，也便于回滚审计。
- 继续复用 `/ai/crud/{configKey}` 动态 CRUD 运行时，避免为每个低代码应用生成 Java/Vue 文件。
- 动态页面模板继续使用 `catalog + ai_page_template` 双注册机制。
- 在线建表通过专门 DDL 服务生成和执行，只允许单表 `CREATE TABLE IF NOT EXISTS` 与受控追加字段。

## 11. 执行日志
| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Spec 确认 | done | `code-copilot/changes/visual-lowcode-crud-builder/spec.md`, `code-copilot/changes/visual-lowcode-crud-builder/tasks.md` | 已确认单表、在线建表、Naive UI 自研 |
| Task 1 | done | `forge/db/migration/V1.0.4__add_visual_lowcode_crud_builder.sql`, `AiCrudConfig.java`, `AiCrudConfigVersion.java`, `AiCrudConfigDTO.java`, `AiCrudConfigRenderVO.java`, `AiCrudConfigVersionMapper.java`, `AiCrudConfigVersionMapper.xml`, `AiCrudConfigService.java` | 数据库迁移、实体/DTO/VO、版本 Mapper 与基础渲染字段扩展；生成器模块编译通过 |
| Task 2 | done | `LowcodeModelSchema.java`, `LowcodeFieldSchema.java`, `LowcodePageSchema.java`, `LowcodePageZone.java`, `LowcodeRuntimeConfig.java`, `LowcodeSchemaValidator.java`, `LowcodeRuntimeConfigBuilder.java`, `AiCrudConfigService.java` | 单表协议 DTO、校验器、运行时转换器和渲染链路接入；生成器模块编译通过 |

## 12. 审查结论
待实现后审查。

## 13. 确认记录（HARD-GATE）
- **确认时间**：2026-05-19
- **确认人**：需求方
- **确认内容**：第一版做单表 CRUD，支持受控在线建表，前端采用 Naive UI 自研搭建器。
