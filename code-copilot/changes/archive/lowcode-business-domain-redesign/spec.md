# 低代码应用开发业务领域化重设计
> status: apply
> created: 2026-05-20
> complexity: 🔴复杂

## 1. 背景与目标

当前低代码 CRUD 已经形成“数据模型设计 → 页面搭建 → 预览 → 发布”的闭环，但顶层抽象仍是单个低代码应用。用户进入系统后只能看到应用列表，再围绕某张表配置字段、页面和菜单；平台缺少“业务领域”的概念，导致以下问题：

- 多个应用之间没有业务归属，销售、采购、库存、合同等能力只能混在同一个低代码应用列表中。
- 字段、字典、安全策略、表名前缀、菜单父级、AI 生成上下文无法按业务领域沉淀复用。
- 业务人员设计应用时需要从技术对象开始，例如 `configKey`、`tableName`、字段类型，而不是先定义业务域、业务对象和业务流程。
- 后续扩展主子表、跨表关系、领域指标、流程绑定、应用市场时，缺少稳定的上层组织模型。

目标是把低代码应用开发从“应用/表驱动”调整为“业务领域驱动”：

`业务领域 → 数据模型设计 → 应用页面设计 → 预览发布 → 运行时页面`

完成后应达到：

- 低代码首页按业务领域组织应用，用户先进入“业务域工作台”；数据模型作为业务领域下的独立资产维护，应用页面创建时再选择一个或多个已有数据模型。
- 每个领域可以配置命名规则、表名前缀、菜单父级、默认模板、通用字段、常用字典、安全策略和 AI 上下文。
- 低代码应用必须归属于一个领域，发布菜单、配置键、表名、权限标识优先从领域规则推导，减少业务人员填写技术字段。
- `modelSchema` 从“单表字段协议”升级为“领域内数据模型协议”，模型只维护基础信息、字段、关系和校验规则；查询/列表/表单/排序等页面行为进入 `pageSchema`。
- 运行时继续复用现有 `AiCrudPage`、`TreeCrudTemplate`、动态 CRUD、发布版本和菜单注册链路，不重做渲染内核。

## 2. 代码现状（Research Findings）

### 2.1 当前低代码入口与链路

- `forge-admin-ui/src/views/ai/lowcode-apps.vue`：当前低代码首页是应用卡片列表，支持按关键词和发布状态筛选；无领域、分类、业务模块或应用分组。
- `forge-admin-ui/src/views/ai/lowcode-builder.vue`：当前搭建器为四步流程，顶部只配置应用名称、配置键、菜单名称；未选择业务领域，也没有从领域规则推导默认值。
- `forge-admin-ui/src/api/lowcode-crud.js`：前端低代码 API 只有 app/model 维度接口，例如 `/ai/lowcode/app/page`、`/ai/lowcode/app/draft`、`/ai/lowcode/model/validate`，没有 domain 维度接口。
- `forge-admin-ui/src/components/lowcode-builder/model/model-schema.js`：默认模型只有 `appType/tableMode/tableName/businessName/treeConfig/fields`，字段默认值和命名规则是全局写死，不支持按领域差异化。

### 2.2 后端现有实现

- `AiCrudConfig` 当前承载低代码应用草稿和运行时配置，字段包括 `configKey/tableName/appName/buildMode/publishStatus/modelSchema/pageSchema/menuParentId/menuResourceId`，没有 `domainId/domainCode/domainName/objectCode` 等领域归属字段。
- `LowcodeAppController` 当前接口全部挂在 `/ai/lowcode/app` 下，面向单个应用草稿、预览、发布、版本和回滚，没有领域工作台、领域详情、领域下应用分页等接口。
- `LowcodeAppService#page` 调用 `AiCrudConfigMapper.selectLowcodePage`，只按租户、`build_mode=LOWCODE`、关键词和发布状态查询，无法按业务领域过滤。
- `LowcodeAppService#createDraft` 保存草稿时由前端直接传入 `configKey`、`menuName`、`menuSort`；缺少领域默认菜单父级、命名前缀和对象编码校验。
- `LowcodePublishService#registerOrUpdateMenu` 当前发布时默认使用配置中的 `menuParentId`，为空时落到默认低代码父菜单；没有按领域挂载到领域菜单目录。
- `LowcodeModelSchema` 当前注释为“单表低代码数据模型协议”，只包含 `appType/tableMode/tableName/businessName/treeConfig/fields/children`，主子表也只是预留，缺少业务域、业务对象、对象关系和领域规则引用。
- `AiCrudConfigMapper.xml#selectLowcodePage` 已按 AGENTS 约定将查询 SQL 写在 XML 中，后续新增领域查询也应继续放在 Mapper XML 中。
- `V1.0.4__add_visual_lowcode_crud_builder.sql` 已为 `ai_crud_config` 增加低代码草稿、发布、模型协议和页面协议字段，并初始化“低代码应用/低代码搭建器/发布/在线DDL”菜单权限；本次新表和字段应新增 Flyway 脚本，不能修改已执行迁移。

### 2.3 参考方案结论

参考 `docs/低代码 CRUD 前端架构全景报告.md`，现有架构已经具备两条建设路径：

- AI 生成路径：自然语言生成 `searchSchema/columns/editSchema/apiConfig`，保存为 CRUD 配置。
- 可视化搭建路径：`LowcodeModelDesigner → LowcodePageBuilder → Preview → Publish`，最终同样转换为 `AiCrudPage` 运行时配置。

该方案的缺口主要集中在自由画布、操作列配置、详情页配置、条件联动、模板扩展和菜单权限配置。业务领域化重设计不替代这些能力，而是新增顶层组织模型，使这些能力后续可以按领域复用和治理。

### 2.4 发现与风险

- 当前低代码应用直接落在 `ai_crud_config`，如果只增加一个 `domain_id` 字段，仍无法承载领域规则、通用字段、AI 上下文和领域菜单等能力；需要新增领域主表。
- 领域会影响菜单挂载和权限资源，属于权限影响范围，进入 `/apply` 前必须人工确认。
- 领域会影响表名、配置键和菜单路径生成规则，必须保证已发布应用兼容，不允许强制迁移历史应用导致运行时路径失效。
- 当前动态 CRUD 仍以单个 `configKey` 为运行时入口，本期不改变 `/ai/crud-page/:configKey` 和 `/ai/crud/{configKey}`，避免扩大运行时风险。

## 3. 功能点

- [ ] 新增业务领域管理：支持领域分页、树形/列表展示、新增、编辑、启停、详情查询。
- [ ] 新增数据模型设计：业务领域下维护多个数据模型，模型基础信息包含模型名称、模型编码、所属业务域、描述、启用状态、多租户、主数据标识。
- [ ] 数据模型字段设计只维护字段本体属性，例如字段名称、字段编码、备注、数据类型、长度、小数位、必填、默认值、字典、安全、数据库映射、关系和校验规则。
- [ ] 新增应用页面设计：应用归属于业务领域，可选择一个或多个数据模型作为数据源，并指定主模型用于当前单表运行时兼容。
- [ ] 页面元素可绑定不同数据模型的字段；查询字段、列表字段、编辑表单字段、查询方式和排序方式在页面设计阶段维护，不进入模型基础设计。
- [ ] 新增领域工作台：进入某个领域后展示领域说明、应用列表、业务对象概览、最近发布版本和快捷创建入口。
- [ ] 低代码应用新建流程改为先选领域，再选择已有数据模型并创建应用页面；应用必须绑定 `domainId`。
- [ ] 领域支持默认规则：表名前缀、配置键前缀、菜单父级、默认页面模板、默认应用类型、默认建表模式、默认字段库。
- [ ] 领域支持 AI 上下文：业务描述、业务术语、常用对象、字段命名偏好、业务约束和生成注意事项。
- [ ] 领域支持通用字段模板：例如区域编码、组织、状态、负责人、附件、备注；创建新业务对象时可一键引入。
- [ ] 领域支持字典和安全策略推荐：按领域维护常用字典类型、敏感字段类型、加密字段策略，模型设计器选择字段时自动推荐。
- [ ] `LowcodeModelSchema` 扩展为领域内业务对象协议，新增 `domain/object/relations/policies` 信息，兼容旧版模型协议。
- [ ] 低代码应用列表支持按领域筛选和分组展示；保留原关键词、发布状态筛选。
- [ ] 发布低代码应用时，菜单默认挂载到领域配置的菜单父级；领域未配置时才回落到现有默认低代码父菜单。
- [ ] AI CRUD 生成支持选择业务领域，提示词注入领域上下文，生成结果落入同一套领域化 `modelSchema/pageSchema`。
- [ ] 历史低代码应用兼容：未绑定领域的应用展示在“未归属”分组，可手动迁移到某个领域。

## 4. 业务规则

- 业务领域编码 `domainCode` 必须小写字母开头，仅允许小写字母、数字、下划线，长度 2-48。
- 同一租户下 `domainCode` 唯一；领域名称同一父级下唯一。
- 领域允许树形结构，但第一版只要求两层展示：一级领域 + 二级业务模块；更深层级可存储但 UI 不强调。
- 领域状态为 `ENABLED` 时可创建应用；`DISABLED` 时禁止新建应用，已发布应用仍可运行。
- 低代码应用必须绑定领域；历史数据可临时绑定到系统内置“未归属领域”。
- 数据模型必须归属于一个业务领域；一个业务领域下可以维护多个数据模型。
- 数据模型不是应用，不强制创建运行时菜单；不是每个模型都需要创建应用。
- 一个应用可以引用多个数据模型；当前动态 CRUD 运行时仍以主模型生成单表配置，非主模型字段先作为页面设计引用和后续扩展快照保存。
- 模型基础信息只包含模型名称、模型编码、所属业务域、描述、启用状态、多租户、主数据标识；业务对象基本信息不再混入模型基础信息表单。
- 查询、列表、表单、查询方式和排序方式属于应用页面设计配置，不属于数据模型字段本体。
- 字段长度、小数位、必填、默认值、数据类型属于数据模型字段设计配置。
- 领域默认表名前缀必须小写下划线，建议以 `biz_` 开头；自动创建表时生成 `领域前缀 + 对象编码`。
- 领域默认配置键前缀必须小写下划线，生成规则为 `domainCode + '_' + objectCode`，仍需满足现有 `configKey` 规则。
- 业务对象编码 `objectCode` 在同一领域内唯一，长度 2-48，只允许小写字母、数字、下划线。
- 领域通用字段不能覆盖审计字段：`id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`。
- 字典字段仍必须绑定系统字典，禁止在领域字段模板中硬编码选项。
- 图片字段仍存储 fileId，运行时列表必须使用 `AuthImage` 或现有文件访问归一化方案。
- 发布菜单默认挂载到领域菜单父级；如果领域未配置菜单父级，则回落到现有低代码默认父级。
- 领域删除采用逻辑停用/禁用；存在低代码应用时禁止物理删除。
- 本期不改变动态 CRUD 运行时 URL：发布后页面仍为 `/ai/crud-page/{configKey}`。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增 | `ai_lowcode_domain` | `id`, `tenant_id`, `parent_id`, `domain_code`, `domain_name`, `domain_desc`, `icon`, `sort`, `status`, `menu_parent_id`, `table_prefix`, `config_key_prefix`, `default_app_type`, `default_layout_type`, `default_table_mode`, `domain_schema`, 审计字段 | 业务领域主表，保存领域规则和 AI 上下文 |
| 新增 | `ai_lowcode_model` | `id`, `tenant_id`, `domain_id`, `domain_code`, `model_code`, `model_name`, `model_desc`, `status`, `tenant_enabled`, `master_data`, `model_schema`, 审计字段 | 业务领域下的数据模型主数据，独立于应用页面 |
| 修改 | `ai_crud_config` | `domain_id`, `domain_code`, `object_code`, `object_name` | 低代码应用绑定领域和业务对象标识 |
| 修改 | `ai_crud_config_version` | `domain_id`, `domain_code`, `object_code`, `object_name` | 发布版本保留领域归属快照 |
| 新增/更新 | `sys_resource` | 业务领域管理菜单、领域工作台菜单、领域启停/迁移按钮权限 | 初始化资源脚本必须 `tenant_id=1` 且 `NOT EXISTS` 防重复 |

### 5.1 `ai_lowcode_domain.domain_schema` 协议

```json
{
  "aiContext": {
    "description": "销售合同领域，管理客户合同、回款、附件和状态流转",
    "terms": ["合同", "客户", "回款", "签约主体"],
    "constraints": ["金额字段单位为分", "合同状态必须使用字典 contract_status"]
  },
  "naming": {
    "tablePrefix": "biz_contract_",
    "configKeyPrefix": "contract_",
    "objectCodeStyle": "lower_snake"
  },
  "defaults": {
    "appType": "SINGLE",
    "layoutType": "simple-crud",
    "tableMode": "CREATE",
    "menuParentId": 1001
  },
  "fieldTemplates": [
    {
      "field": "regionCode",
      "columnName": "region_code",
      "label": "所属区划",
      "dataType": "varchar",
      "length": 32,
      "componentType": "regionTreeSelect",
      "searchable": true,
      "listVisible": true,
      "formVisible": true
    }
  ],
  "dictRecommendations": [
    { "fieldPattern": "status", "dictType": "contract_status" }
  ],
  "securityPolicies": [
    { "fieldPattern": "phone", "sensitiveType": "PHONE" },
    { "fieldPattern": "idCard", "sensitiveType": "ID_CARD", "encryptAlgorithm": "SM4" }
  ]
}
```

### 5.2 扩展后的 `modelSchema` 协议

```json
{
  "schemaVersion": 2,
  "domain": {
    "id": 100,
    "code": "contract",
    "name": "合同管理域"
  },
  "object": {
    "code": "contract_archive",
    "name": "合同档案",
    "description": "管理合同主信息、状态、金额和附件"
  },
  "appType": "SINGLE",
  "tableMode": "CREATE",
  "tableName": "biz_contract_archive",
  "businessName": "合同档案",
  "treeConfig": null,
  "fields": [],
  "relations": [
    {
      "relationType": "REFERENCE",
      "targetObjectCode": "customer",
      "sourceField": "customerId",
      "targetField": "id",
      "displayField": "customerName"
    }
  ],
  "policies": {
    "dataScope": "TENANT",
    "regionField": "regionCode",
    "auditEnabled": true
  },
  "children": []
}
```

兼容规则：

- 读取旧版 `modelSchema` 时，如果没有 `schemaVersion/domain/object`，后端按 `AiCrudConfig.domainId/domainCode/objectCode` 补齐运行时对象；仍无领域时归入“未归属”。
- 第一版 `relations` 只做协议存储、ER 图展示和 AI 上下文，不改变动态 CRUD 查询行为。
- 第一版 `children` 仍保持主子表协议预留，运行时不启用。

### 5.3 应用页面 `pageSchema` 引用模型协议

```json
{
  "layoutType": "simple-crud",
  "primaryModelId": 1001,
  "primaryModelCode": "customer",
  "modelRefs": [
    {
      "modelId": 1001,
      "modelCode": "customer",
      "modelName": "客户",
      "primary": true,
      "fields": []
    },
    {
      "modelId": 1002,
      "modelCode": "contact",
      "modelName": "联系人",
      "primary": false,
      "fields": []
    }
  ],
  "zones": []
}
```

页面设计规则：

- `modelRefs` 保存应用引用的数据模型快照，页面字段引用以 `fieldRef` 保存，非主模型字段使用 `modelCode__fieldName` 避免重名。
- `zones[].fieldRefs` 和画布组件的 `fieldRef/fieldRefs` 保存页面元素绑定字段。
- 查询字段、列表字段、编辑表单字段、查询方式和排序方式存放在页面区域配置中。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 新增 | `/ai/lowcode/domain/page` | GET | 业务领域分页，参数使用 `pageNum/pageSize` |
| 新增 | `/ai/lowcode/domain/tree` | GET | 业务领域树，用于左侧导航和新建应用选择 |
| 新增 | `/ai/lowcode/domain/{id}` | GET | 业务领域详情，包含 `domainSchema` |
| 新增 | `/ai/lowcode/domain` | POST | 新增业务领域 |
| 新增 | `/ai/lowcode/domain` | PUT | 修改业务领域 |
| 新增 | `/ai/lowcode/domain/{id}/status` | PUT | 启用/停用业务领域 |
| 新增 | `/ai/lowcode/domain/{id}/workspace` | GET | 领域工作台概览：应用数量、发布数量、最近版本、对象概览 |
| 新增 | `/ai/lowcode/domain/{id}/defaults` | GET | 获取领域默认规则，用于新建应用初始化 |
| 新增 | `/ai/lowcode/model/page` | GET | 数据模型分页，按业务领域、关键词、状态、主数据标识过滤 |
| 新增 | `/ai/lowcode/model/list` | GET | 数据模型下拉列表，用于应用页面选择数据源 |
| 新增 | `/ai/lowcode/model/{id}` | GET | 数据模型详情，包含 `modelSchema` |
| 新增 | `/ai/lowcode/model` | POST | 新增数据模型 |
| 新增 | `/ai/lowcode/model` | PUT | 修改数据模型 |
| 新增 | `/ai/lowcode/model/{id}/status` | PUT | 启用/停用数据模型 |
| 修改 | `/ai/lowcode/app/page` | GET | 新增 `domainId/domainCode/unassigned` 过滤条件 |
| 修改 | `/ai/lowcode/app/draft` | POST | 请求体新增 `domainId/objectCode/objectName`，保存草稿时绑定领域 |
| 修改 | `/ai/lowcode/app/{id}/publish` | POST | 发布时按领域菜单父级和命名规则补齐菜单配置 |
| 新增 | `/ai/lowcode/app/{id}/move-domain` | PUT | 将历史或草稿应用迁移到指定领域 |
| 修改 | `/ai/crud-generator/stream-generate` | POST | 支持传入 `domainId`，生成提示词注入领域上下文 |

所有新增查询类 SQL 必须写在 Mapper XML 中，分页参数统一使用 `pageNum/pageSize`。

## 7. 前端设计调整

### 7.1 信息架构

低代码入口调整为领域优先：

```text
低代码开发
├── 业务领域
│   ├── 销售域
│   │   ├── 领域工作台
│   │   ├── 应用列表
│   │   ├── 业务对象
│   │   └── 领域规则
│   ├── 采购域
│   └── 未归属
└── 技术配置入口
    ├── CRUD 配置
    └── AI 生成历史
```

### 7.2 领域工作台

- 左侧为领域树，支持搜索、启用状态筛选。
- 主区域展示领域简介、默认规则、应用卡片、最近发布版本和领域对象关系概览。
- 创建应用按钮固定带入当前领域，避免用户从空白应用开始填写技术字段。

### 7.3 数据模型设计页

- 左侧按“业务领域 -> 数据模型”树形展示，不使用模型卡片堆叠。
- 选择业务领域后，在主区域维护该领域下的数据模型。
- 模型基础信息只维护：模型名称、模型编码、所属业务域、描述、启用状态、多租户、主数据标识。
- 字段设计区优先展示宽表格，字段编码支持行内稳定输入，长度和小数位在字段设计区维护。
- 字段设计不展示查询/列表/表单/查询方式/排序方式；这些配置进入应用页面设计。
- 模型设计页保留关联配置、校验规则和扩展配置；去掉触发器配置。
- 右侧/下方属性面板只维护字段补充属性，避免重复字段表格中的字段名称、编码、类型、长度和小数位。

### 7.4 应用页面设计页

- 顶部配置从“应用名称/配置键/菜单名称”调整为“领域/应用名称/应用编码/发布菜单”。
- 应用页面设计阶段选择一个或多个数据模型，并指定主模型。
- 页面字段池展示所选模型的字段，字段标签带来源模型。
- 查询集、列表页、编辑表单、详情页分别选择字段并维护顺序。
- 查询方式和排序方式属于页面字段设置，保存到页面区域配置。
- `configKey/menuName` 默认由领域规则和应用名称推导，允许高级模式手动调整。

## 8. 影响范围

- `forge-plugin-generator`：新增领域实体、DTO/VO、Mapper XML、Service、Controller；扩展低代码应用草稿、分页、发布、版本快照和 AI 生成上下文。
- `forge-admin-ui`：新增领域管理页、领域工作台、领域选择器；调整低代码应用列表和搭建器初始化逻辑。
- `AiCrudConfig` / `AiCrudConfigVersion`：新增领域和业务对象归属字段。
- `LowcodeModelSchema` / `LowcodeRuntimeConfigBuilder`：扩展领域对象协议并兼容旧版 schema。
- `MenuRegisterAdapter`：发布菜单默认父级从领域配置读取，保留现有默认父级兜底。
- 数据库迁移：新增 Flyway 脚本，必须使用 `information_schema`、`CREATE TABLE IF NOT EXISTS`、`NOT EXISTS` 防重复。

## 9. 风险与关注点

- ⚠️ 涉及权限变更：新增领域管理菜单和按钮权限，发布菜单父级也会按领域改变。
- ⚠️ 涉及状态流转：领域启停会影响新建应用入口，但不能影响已发布应用运行。
- ⚠️ 涉及 DDL 间接影响：领域表名前缀会影响在线建表生成结果，仍需沿用现有 DDL 白名单、二次确认和 `ai:lowcode:deploy-ddl` 权限。
- ⚠️ 涉及历史数据迁移：已有低代码应用没有领域归属，必须提供未归属分组和手动迁移，不做破坏性自动重命名。
- 不涉及资金变更。
- 不改变动态 CRUD 运行时 URL，避免影响已授权菜单和外部收藏链接。

## 9.5 测试策略

- **测试范围**：领域 CRUD、领域树、领域工作台统计、领域默认规则初始化、应用按领域分页、草稿保存绑定领域、发布菜单父级、旧版 `modelSchema` 兼容、AI 生成注入领域上下文。
- **覆盖率目标**：领域编码唯一性、启停状态、新建应用默认值、历史未归属应用、发布版本快照、菜单父级兜底、Mapper XML 查询过滤必须覆盖核心分支。
- **独立 Test Spec**：是，进入 `/apply` 后补充 `code-copilot/changes/lowcode-business-domain-redesign/test-spec.md`。
- **验证命令**：
  - 后端：`mvn -pl forge-admin-server -am compile -DskipTests`
  - 前端：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build`
  - 交互：启动前端后验证领域树、领域工作台、新建应用、保存草稿、发布、打开运行时页面。

## 10. 待澄清

- [ ] 领域是否允许多级树？建议第一版存储支持多级，UI 聚焦两级。
允许
- [ ] 历史低代码应用默认放入“未归属”，还是批量初始化到一个“通用业务域”？
放在通用业务域
- [ ] 领域菜单父级是否由管理员配置，还是自动在“低代码开发”下按领域创建目录？ 
按领域创建目录
- [ ] 领域字段模板第一版是否只做模型初始化，还是支持后续批量同步到已创建应用？
先不实现
- [ ] 领域关系 `relations` 第一版是否需要在 UI 中画 ER 图，还是只在详情中以表格维护？
ER图是自动生成的
## 11. 技术决策

- 新增 `ai_lowcode_domain` 作为领域主表，不把领域规则塞进 `ai_crud_config`，避免应用表继续膨胀。
- `ai_crud_config` 只保存领域归属和业务对象标识：`domain_id/domain_code/object_code/object_name`。
- 领域规则采用 `domain_schema` JSON 存储，便于逐步扩展 AI 上下文、字段模板、字典推荐和安全策略。
- `modelSchema` 升级为 `schemaVersion=2`，但运行时转换器必须兼容旧版无版本 schema。
- 第一版领域关系只用于建模、展示和 AI 上下文，不改变动态 CRUD 的单表查询能力。
- 发布菜单父级优先级：发布请求显式传入 `menuParentId` > 领域默认 `menuParentId` > 现有低代码默认父级。
- 领域停用不下线已发布应用，只限制新建和迁入。
- 所有新增管理查询继续走 Mapper XML，遵守 DataScopeInterceptor 可审查规则。

## 12. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Spec 草案 | done | `code-copilot/changes/lowcode-business-domain-redesign/spec.md` | 仅设计 Spec，未写实现代码 |
| 阶段 1：数据库与领域后端核心 | done | `forge/db/migration/V1.0.7__add_lowcode_business_domain.sql`；低代码领域 Entity/DTO/VO/Mapper XML/Service/Controller | 后端编译通过：`cd forge && mvn -pl forge-admin-server -am compile -DskipTests` |
| 阶段 2：应用领域绑定与模型协议兼容 | done | `ai_crud_config` / `ai_crud_config_version` 领域字段映射；`modelSchema` v2 DTO；草稿保存领域补齐；应用迁移领域接口；`V1.0.8__fix_lowcode_domain_workspace_route.sql` | 后端编译通过；领域工作台 404 改为迁移修正脚本处理，不新增前端手动路由 |
| 阶段 3：发布菜单与版本快照领域化 | done | 发布菜单父级按领域解析和自动创建；已有菜单更新父级；版本行和快照写入领域/对象信息；回滚恢复领域与菜单快照 | 后端编译通过：`cd forge && mvn -pl forge-admin-server -am compile -DskipTests` |
| 阶段 4：领域工作台与应用列表前端 | done | 低代码首页重构为领域树、领域工作台、应用列表和迁移入口；补齐领域 API；新增领域编辑抽屉和迁移弹窗 | 前端 lint 通过；前端构建通过：`NODE_OPTIONS=--max-old-space-size=4096 pnpm build` |
| 阶段 5：搭建器领域优先改造 | done | 搭建器重构为左侧业务领域/领域模型树、中间对象配置和四阶段搭建；模型设计器增加基础信息、字段设计、关联配置、校验规则、触发器、扩展配置；字段属性面板补齐默认值、显示规则、字典和安全推荐；草稿/发布 DTO 接收领域和对象信息 | 前端 ESLint 通过；前端构建通过：`NODE_OPTIONS=--max-old-space-size=8192 pnpm build`；后端编译通过：`cd forge && mvn -pl forge-admin-server -am compile -DskipTests` |

## 13. 审查结论

已确认，可进入 `/apply`。

## 14. 确认记录（HARD-GATE）

- **确认时间**：2026-05-20
- **确认人**：用户
- **确认内容**：
  - 领域树允许多级存储和展示。
  - 历史低代码应用默认放入“通用业务域”，不做破坏性重命名。
  - 领域菜单父级按领域自动创建目录。
  - 领域字段模板第一版先不实现后续批量同步到已创建应用。
  - 领域关系图按自动生成方向处理。
