# 低代码应用开发业务领域化重设计任务计划
> status: phase_5r_g_page_preview_feedback_done
> current_phase: phase_5r_g_page_preview_feedback
> last_updated: 2026-05-21
> execution_rule: 每次只实现一个阶段；阶段开始前把该阶段状态改为 `in_progress`，阶段完成后改为 `done` 并补充实际改动文件与验证结果。

## 1. Spec 分析结论

- 本变更是跨数据库、后端领域模型、低代码应用发布链路、前端信息架构和 AI 生成上下文的复杂改造，不适合一次性全量实现。
- 最小安全路径是先建立领域主数据和接口，再让低代码应用绑定领域，最后改前端入口和 AI 生成；运行时 `/ai/crud-page/{configKey}` 与 `/ai/crud/{configKey}` 保持不变。
- 最高风险点是权限资源脚本、发布菜单父级、旧版 `modelSchema` 兼容、历史未归属应用、领域停用对新建/迁移入口的影响。
- 第一版只存储和展示 `relations/children` 协议，不改变动态 CRUD 的单表查询能力；领域字段模板第一版只做新建模型初始化，不批量同步历史应用。
- 所有新增查询 SQL 必须落在 Mapper XML；分页参数继续使用 `pageNum/pageSize`；业务内置数据和权限资源 `tenant_id` 使用 `1`。

## 2. 阶段状态总览

| 阶段 | 状态 | 目标 | 阶段完成口径 |
|------|------|------|--------------|
| 0. 规划与门禁 | done | 完成 spec 阅读、风险识别和阶段拆分 | `tasks.md` 已创建，未写实现代码 |
| Gate. 人工确认 | done | 补齐 spec HARD-GATE 与待澄清项 | `spec.md` 第 14 节已填写确认记录 |
| 1. 数据库与领域后端核心 | done | 新增领域表、领域 CRUD/树/默认规则/工作台接口 | 后端编译通过，接口路径完整 |
| 2. 应用领域绑定与模型协议兼容 | done | 低代码应用绑定领域，支持通用业务域和迁移，`modelSchema` 升级 v2 | 旧应用可读，新草稿必须绑定领域 |
| 3. 发布菜单与版本快照领域化 | done | 发布菜单按领域父级挂载，版本保留领域快照 | 发布/回滚不丢领域信息 |
| 4. 领域工作台与应用列表前端 | done | 低代码首页改成领域优先，提供领域工作台和迁移入口 | 前端构建通过，应用列表可按领域过滤 |
| 5. 搭建器领域优先改造 | done | 新建流程先选领域和业务对象，默认值按领域规则生成 | 草稿保存带领域和对象信息 |
| 5R. 数据模型/应用页面拆分纠偏 | done | 数据模型独立设计，应用页面选择多个模型并绑定字段 | 应用设计器不再维护模型本体，多模型字段引用可保存 |
| 5R-F. 交互反馈纠偏 | done | 修正领域树、模型关系选择、应用入口和菜单重复 | 前端交互顺序符合“领域 -> 模型 -> 应用页面” |
| 5R-G. 页面设计与预览纠偏 | done | 修正发布菜单父级、表单详情合并、字段配置弹窗和真实运行态预览入口 | 父级菜单树选择；表单/详情共用；查询/列表字段弹窗编辑；已发布应用可运行态预览 |
| 6. AI 生成领域上下文注入 | pending | AI CRUD 生成支持 `domainId` 并注入领域上下文 | 流式生成请求与提示词包含领域信息 |
| 7. 测试、文档与收口 | pending | 补充 test-spec、核心测试和执行日志 | 编译/构建/测试结果记录完整 |

## 3. Gate：人工确认

**状态：done**

进入实现前先补齐 `code-copilot/changes/lowcode-business-domain-redesign/spec.md` 第 14 节确认记录。确认内容必须覆盖：

- 允许新增业务领域管理菜单、领域工作台菜单、启停和迁移按钮权限。
- 历史低代码应用先进入“通用业务域”，不做破坏性自动重命名。
- 领域菜单父级按领域自动创建目录。
- 领域字段模板第一版先不实现后续批量同步到已创建应用。
- `relations` 第一版按自动生成 ER 图方向处理，不改变动态 CRUD 查询。

## 4. 阶段 1：数据库与领域后端核心

**状态：done**

**目标**

建立业务领域主表和后端基础接口，先不改前端入口，也不强制低代码应用绑定领域。

**计划改动文件**

- Create: `forge/db/migration/V1.0.7__add_lowcode_business_domain.sql`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiLowcodeDomain.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeDomainDTO.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeDomainStatusDTO.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeDomainSchema.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/lowcode/LowcodeDomainVO.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/lowcode/LowcodeDomainTreeVO.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/lowcode/LowcodeDomainWorkspaceVO.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/AiLowcodeDomainMapper.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiLowcodeDomainMapper.xml`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDomainService.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/LowcodeDomainController.java`

**任务清单**

- [x] 新增 Flyway 脚本，创建 `ai_lowcode_domain`，并为 `ai_crud_config`、`ai_crud_config_version` 增加 `domain_id/domain_code/object_code/object_name` 字段和必要索引。
- [x] Flyway 脚本初始化业务领域管理菜单、领域工作台菜单、启停/迁移按钮权限；所有 `sys_resource` 插入必须使用 `tenant_id=1` 和 `NOT EXISTS`。
- [x] 实现领域实体、DTO、VO 和 `domain_schema` JSON DTO，字段覆盖 spec 中 AI 上下文、命名规则、默认规则、字段模板、字典推荐、安全策略。
- [x] 实现 `AiLowcodeDomainMapper.xml`：分页、树、详情、同租户编码唯一校验、同父级名称唯一校验、工作台统计查询。
- [x] 实现 `LowcodeDomainService`：编码格式校验、默认值补齐、启停规则、存在应用时禁止物理删除。
- [x] 实现 `/ai/lowcode/domain/page`、`/tree`、`/{id}`、`POST /`、`PUT /`、`/{id}/status`、`/{id}/workspace`、`/{id}/defaults`。

**验证**

- Run: `cd forge && mvn -pl forge-admin-server -am compile -DskipTests`
- 重点检查：分页参数为 `pageNum/pageSize`；新增查询 SQL 全在 XML；迁移脚本防重复。

## 5. 阶段 2：应用领域绑定与模型协议兼容

**状态：done**

**目标**

让低代码应用具备领域归属和业务对象标识，同时兼容旧版无领域 `modelSchema`。历史应用按澄清意见归入“通用业务域”。

**计划改动文件**

- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiCrudConfig.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiCrudConfigVersion.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudConfigMapper.xml`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudConfigVersionMapper.xml`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/AiCrudConfigMapper.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeAppDraftDTO.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeModelSchema.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeDomainRef.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeObjectSchema.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeRelationSchema.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodePolicySchema.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/lowcode/LowcodeAppDetailVO.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeMoveDomainDTO.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeAppService.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeSchemaValidator.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/LowcodeAppController.java`
- Create: `forge/db/migration/V1.0.8__fix_lowcode_domain_workspace_route.sql`

**任务清单**

- [x] 实体、Mapper ResultMap 和 Base_Columns 补齐领域字段，避免新增列无法回显。
- [x] `/ai/lowcode/app/page` 增加 `domainId/domainCode/generalDomain` 过滤，SQL 继续写在 `AiCrudConfigMapper.xml`。
- [x] `LowcodeAppDraftDTO` 增加 `domainId/objectCode/objectName`；新增草稿绑定启用领域，旧前端未传领域时自动归入通用业务域。
- [x] `LowcodeModelSchema` 增加 `schemaVersion/domain/object/relations/policies`，读取旧版 schema 时用配置表字段补齐运行时对象，仍无领域时补齐为通用业务域。
- [x] 增加 `/ai/lowcode/app/{id}/move-domain`，历史和草稿可迁移到启用领域，停用领域禁止迁入。
- [x] `LowcodeAppDetailVO` 回显领域和业务对象信息，前端后续可直接展示。
- [x] 不新增手动前端路由；通过 `V1.0.8` 将阶段 1 的领域工作台菜单修正到现有 `/ai/lowcode-apps`，避免 404。

**验证**

- Run: `cd forge && mvn -pl forge-admin-server -am compile -DskipTests`
- 重点检查：旧版 `modelSchema` 仍能打开详情和预览；通用业务域过滤可查到历史应用。

## 6. 阶段 3：发布菜单与版本快照领域化

**状态：done**

**目标**

发布时优先使用领域菜单父级，版本记录保留领域归属快照，回滚后不丢领域信息。

**计划改动文件**

- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodePublishDTO.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodePublishService.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/MenuRegisterAdapter.java`
- Modify: `forge/forge-admin-server/src/main/java/com/mdframe/forge/admin/bridge/MenuRegisterAdapterImpl.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/lowcode/LowcodeVersionVO.java`

**任务清单**

- [x] 发布菜单父级优先级实现为：发布请求显式 `menuParentId` > 领域默认/自动创建领域菜单父级 > 现有低代码默认父级。
- [x] `MenuRegisterAdapter` 支持更新已有菜单的父级；实现类更新 `SysResource.parentId/resourceName/sort`。
- [x] 发布版本写入 `domain_id/domain_code/object_code/object_name`，`publishSnapshot` 同步包含领域、对象和菜单父级信息。
- [x] 回滚时从版本和快照恢复领域、对象、菜单配置；旧版本缺失字段时沿用当前配置或默认兜底。
- [x] 发布在线建表继续沿用现有 DDL 权限和二次确认，不因领域表名前缀绕过校验。

**验证**

- Run: `cd forge && mvn -pl forge-admin-server -am compile -DskipTests`
- 重点检查：领域未配置菜单父级时仍回落到现有默认父级；已有菜单更新后父级可变更。

## 7. 阶段 4：领域工作台与应用列表前端

**状态：done**

**目标**

把低代码首页从单纯应用卡片列表改为领域优先的信息架构，并提供领域管理、工作台和应用迁移入口。

**计划改动文件**

- Modify: `forge-admin-ui/src/api/lowcode-crud.js`
- Create: `forge-admin-ui/src/components/lowcode-builder/domain/DomainTreePanel.vue`
- Create: `forge-admin-ui/src/components/lowcode-builder/domain/DomainWorkspacePane.vue`
- Create: `forge-admin-ui/src/components/lowcode-builder/domain/DomainEditorDrawer.vue`
- Create: `forge-admin-ui/src/components/lowcode-builder/domain/MoveDomainModal.vue`
- Modify: `forge-admin-ui/src/views/ai/lowcode-apps.vue`

**任务清单**

- [x] 在 API 文件中补齐 domain page/tree/detail/create/update/status/workspace/defaults 和 app move-domain 方法。
- [x] 低代码首页增加左侧领域树，主区域按当前领域加载工作台与应用列表。
- [x] 保留关键词、发布状态筛选，选择领域时额外传入 `domainId`。
- [x] 领域管理抽屉支持新增、编辑、启停、默认规则和 AI 上下文维护。
- [x] 应用卡片展示领域名称、业务对象编码和发布状态；历史应用提供迁移入口。
- [x] 页面控件使用 Naive UI 与项目现有后台风格，按钮操作颜色遵守项目后台操作约定。

**验证**

- Run: `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm build`
- 重点检查：低代码首页在无领域、只有未归属、有多领域三种状态下都能正常展示。

## 8. 阶段 5：搭建器领域优先改造

**状态：done**

**目标**

新建低代码应用时先选择业务领域和业务对象，`configKey/tableName/menuName` 默认从领域规则推导。

**计划改动文件**

- Modify: `forge-admin-ui/src/views/ai/lowcode-builder.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/model/model-schema.js`
- Modify: `forge-admin-ui/src/components/lowcode-builder/model/LowcodeModelDesigner.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/model/ModelFieldTable.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/model/ModelFieldPropertyPanel.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/publish/PublishPanel.vue`
- Modify: `forge-admin-ui/src/views/ai/lowcode-apps.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/domain/DomainWorkspacePane.vue`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeAppDraftDTO.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodePublishDTO.java`

**任务清单**

- [x] 顶部配置改为“领域 / 业务对象 / 应用名称 / 发布菜单”，保留高级模式手动调整 `configKey/tableName/menuParentId`。
- [x] 从领域默认规则生成 `objectCode`、`tableName`、`configKey` 和 `menuName`；已有应用加载时不重新覆盖人工值。
- [x] 模型设计器增加“引入领域字段模板”，过滤审计字段，避免覆盖 `id/tenant_id/create_by/create_time/create_dept/update_by/update_time`。
- [x] 字段编辑时根据领域字典推荐和安全策略推荐默认 `dictType/sensitiveType/encryptAlgorithm`，字典选项仍来自系统字典。
- [x] 保存草稿和发布请求带上 `domainId/domainCode/domainName/objectCode/objectName`。

**验证**

- Run: `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint --fix src/views/ai/lowcode-builder.vue src/views/ai/lowcode-apps.vue src/components/lowcode-builder/domain/DomainWorkspacePane.vue src/components/lowcode-builder/model/LowcodeModelDesigner.vue src/components/lowcode-builder/model/ModelFieldTable.vue src/components/lowcode-builder/model/ModelFieldPropertyPanel.vue src/components/lowcode-builder/model/model-schema.js src/components/lowcode-builder/publish/PublishPanel.vue`
- Run: `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
- Run: `cd forge && mvn -pl forge-admin-server -am compile -DskipTests`
- 结果：ESLint 通过；前端构建通过（4GB 堆偶发 OOM，8GB 堆通过；存在项目既有 UnoCSS 图标和 chunk 体积告警）；后端 admin 编译通过。
- 重点检查：低代码搭建器必须先选择业务领域；停用领域不可新建；编辑旧应用不覆盖 `configKey/tableName`；字段模板过滤审计字段；图片字段未改变 fileId 存储和运行时渲染方案。

## 8R. 阶段 5R：数据模型/应用页面拆分纠偏

**状态：done**

**目标**

按澄清后的关系重构交互：业务领域下沉淀多个独立数据模型，应用页面创建时选择一个或多个已有数据模型，页面元素再绑定到不同模型的字段。应用设计器不再维护模型名称、模型编码、字段、关联关系和校验规则等模型本体信息。

**计划改动文件**

- Modify: `forge-admin-ui/src/views/ai/lowcode-builder.vue`
- Create: `forge-admin-ui/src/views/ai/lowcode-models.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/model/LowcodeModelDesigner.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/model/ModelFieldTable.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/model/ModelFieldPropertyPanel.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/model/model-schema.js`
- Modify: `forge-admin-ui/src/components/lowcode-builder/page/ComponentPalette.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/page/page-schema.js`
- Modify: `forge-admin-ui/src/components/lowcode-builder/page/LowcodePageBuilder.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/preview/LowcodePreviewPane.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/publish/PublishPanel.vue`
- Create: `forge/db/migration/V1.0.9__add_lowcode_data_model.sql`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiLowcodeModel.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeDataModelDTO.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeDataModelStatusDTO.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/lowcode/LowcodeDataModelVO.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/AiLowcodeModelMapper.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiLowcodeModelMapper.xml`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/LowcodeModelController.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDataModelService.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodePageSchema.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodePageModelRef.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeSchemaValidator.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeAppService.java`

**任务清单**

- [x] 应用设计器左侧第二栏从“领域模型”改为“应用页面”，数据模型设计入口跳转到独立 `lowcode-models` 页面。
- [x] 应用基础信息只维护应用名称、应用编码、所属业务域、发布菜单等应用属性，不再维护模型基础信息。
- [x] 应用数据源支持选择多个已启用数据模型，并指定一个主模型用于当前单表运行时兼容。
- [x] 页面设计器字段池合并所选模型字段，字段标签显示来源模型，页面区域和画布组件可保存多模型字段引用。
- [x] 后端 `pageSchema` 增加应用引用模型快照，页面校验允许多模型字段引用；运行时转换只使用主模型字段，避免破坏现有动态 CRUD。
- [x] 取消低代码应用在同一领域内按业务对象编码唯一的限制，避免一个模型只能创建一个应用。
- [x] 数据模型设计页改为“业务域树 + 模型子节点”的树形信息架构，不再用模型卡片堆叠展示。
- [x] 字段设计 tab 去掉触发器；字段表格全宽优先，长度/小数位/必填/默认值等字段本体配置在字段设计区维护。
- [x] 字段编码输入改为输入时不强制规范化、失焦再规范化，避免行内编辑时焦点跳出。
- [x] 审计字段在前端保存前和后端保存前双侧过滤；领域通用字段校验覆盖 camel 与 snake 审计字段。
- [x] 查询字段、列表字段、编辑表单字段、查询方式、排序方式移动到页面设计阶段维护；模型字段表格不再展示查询/列表/表单开关。
- [x] 字段属性面板只保留数据库映射、主键/系统字段、字典与安全策略等补充配置，避免与字段表格重复。

**验证**

- Run: `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint --fix src/views/ai/lowcode-models.vue src/components/lowcode-builder/model/LowcodeModelDesigner.vue src/components/lowcode-builder/model/ModelFieldTable.vue src/components/lowcode-builder/model/ModelFieldPropertyPanel.vue src/components/lowcode-builder/model/model-schema.js src/components/lowcode-builder/page/ComponentPalette.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/preview/LowcodePreviewPane.vue`
- Run: `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
- Run: `cd forge && mvn -pl forge-admin-server -am compile -DskipTests`
- 结果：ESLint 通过，仅保留 `StructuredListPageDesigner.vue` 既有单文件多组件 warning；前端生产构建通过，存在项目既有 UnoCSS 图标、CSS 注释和 chunk 体积 warning；后端 admin 编译通过。

## 8S. 阶段 5R-F：交互反馈纠偏

**状态：done**

**目标**

按最新反馈继续纠偏交互和菜单：关系配置不再手输目标对象编码；领域树可展开/收起；低代码应用页不再展示占空间的领域工作台概览；应用页面设计器不再出现左侧业务领域/应用页面侧栏；菜单只保留统一的“低代码应用”入口。

**计划改动文件**

- Modify: `forge-admin-ui/src/views/ai/lowcode-apps.vue`
- Modify: `forge-admin-ui/src/views/ai/lowcode-builder.vue`
- Modify: `forge-admin-ui/src/views/ai/lowcode-models.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/domain/DomainTreePanel.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/model/LowcodeModelDesigner.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/shared/DictTypeSelect.vue`
- Create: `forge/db/migration/V1.0.10__dedupe_lowcode_app_menu.sql`

**任务清单**

- [x] 模型关联配置的目标模型改为从当前领域数据模型中下拉选择；目标字段和展示字段随目标模型联动。
- [x] 字典与安全区的“字典类型 / 新增字典”改为固定网格对齐，修正控件高度不协调。
- [x] 低代码应用页和数据模型设计页的业务领域树支持展开/收起。
- [x] 低代码应用页移除领域工作台大块概览内容，保留业务领域树、应用列表、筛选和新建应用入口。
- [x] 新建应用入口在低代码应用页持续可见；未选领域时给出明确提示，停用领域仍禁止新建。
- [x] 应用页面设计器去掉左侧业务领域/应用页面侧栏和残留领域抽屉，改为单栏页面设计工作台。
- [x] 新增 `V1.0.10` 菜单迁移：把 `ai:lowcode:domain:list` 资源改名为“低代码应用”，删除旧 `ai:lowcode:list` 与临时 `ai:lowcode:domain:workspace` 资源，并迁移旧角色授权和发布按钮权限。

**验证**

- Run: `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint --fix src/views/ai/lowcode-apps.vue src/components/lowcode-builder/domain/DomainTreePanel.vue src/views/ai/lowcode-models.vue src/components/lowcode-builder/model/LowcodeModelDesigner.vue src/components/lowcode-builder/shared/DictTypeSelect.vue src/views/ai/lowcode-builder.vue`
- Run: `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
- Run: `cd forge && mvn -pl forge-admin-server -am compile -DskipTests`
- Run: `curl -I http://127.0.0.1:5173/ai/lowcode-apps`
- 结果：ESLint 通过；前端构建通过，存在项目既有 UnoCSS 图标、CSS 注释和 chunk 体积告警；后端 admin 编译通过；本地前端服务 `127.0.0.1:5173` 返回 `200 OK`。

## 8T. 阶段 5R-G：页面设计与预览纠偏

**状态：done**

**目标**

按最新反馈修正应用发布、页面设计和预览交互：发布菜单父级从菜单树选择；查询详情页与表单页合并；查询字段、列表字段在弹窗中配置；表单配置按钮有明确反馈；已发布应用支持真实运行态预览入口。

**计划改动文件**

- Create: `forge-admin-ui/src/components/lowcode-builder/shared/MenuParentSelect.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/publish/PublishPanel.vue`
- Modify: `forge-admin-ui/src/views/ai/lowcode-builder.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/domain/DomainEditorDrawer.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/page/LowcodePageBuilder.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/page/page-schema.js`
- Modify: `forge-admin-ui/src/components/lowcode-builder/preview/LowcodePreviewPane.vue`

**任务清单**

- [x] 发布配置、应用基础配置、领域默认规则中的菜单父级改为 `MenuParentSelect`，从 `/system/resource/tree` 加载目录/菜单树，不再手输父级 ID。
- [x] 页面设计器将“编辑表单”改为“表单与详情”，表单字段作为详情展示字段来源；列表设计区移除单独“查询详情页/详情字段”配置。
- [x] 查询字段、列表字段改为摘要行加弹窗配置；弹窗内支持字段增删、排序、查询方式、列表排序，避免字段过多挤压主设计区。
- [x] “应用表单配置”按钮调用后给出成功/未加载提示，避免点击无反馈。
- [x] 实时预览区区分草稿配置预览和真实运行态预览：草稿只做配置预览/后端校验；已发布应用可加载 `crudConfigRender` 并内嵌 `AiCrudPage` 访问真实运行接口，也可打开 `/ai/crud-page/{configKey}`。
- [x] 明确未发布草稿不能直接跑真实 CRUD 数据和动作，避免把样例预览误认为真实运行。

**验证**

- Run: `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint --fix src/components/lowcode-builder/shared/MenuParentSelect.vue src/components/lowcode-builder/publish/PublishPanel.vue src/views/ai/lowcode-builder.vue src/components/lowcode-builder/domain/DomainEditorDrawer.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/LowcodePageBuilder.vue src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/preview/LowcodePreviewPane.vue`
- Run: `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
- Run: `curl -I http://127.0.0.1:5173/ai/lowcode-builder`
- 结果：ESLint 通过，仅保留 `StructuredListPageDesigner.vue` 既有单文件多组件 warning；前端构建通过，存在项目既有 UnoCSS 图标、CSS 注释和 chunk 体积告警；本地前端服务 `127.0.0.1:5173/ai/lowcode-builder` 返回 `200 OK`。

## 8U. 阶段 5R-H：低代码菜单归位

**状态：done**

**目标**

按最新反馈调整菜单信息架构：低代码应用不再作为“AI智能平台”下的散落入口；复用原“AI代码生成”顶层目录并重命名为“AI低代码”，其下提供“模型设计”和“应用开发”两个低代码入口。

**计划改动文件**

- Create: `forge/db/migration/V1.0.11__reparent_lowcode_menus.sql`
- Modify: `code-copilot/changes/lowcode-business-domain-redesign/tasks.md`

**任务清单**

- [x] 新增 `V1.0.11` 菜单迁移：将原“AI代码生成”目录重命名为“AI低代码”。
- [x] 将 `ai:lowcode:model:list` 菜单移到“AI低代码”下，显示名改为“模型设计”。
- [x] 将 `ai:lowcode:domain:list` 菜单移到“AI低代码”下，显示名改为“应用开发”。
- [x] 将独立 `ai:lowcode:edit` 搭建器菜单隐藏到“应用开发”下，避免继续在“AI智能平台”下显示重复入口。
- [x] 根据现有低代码角色授权补齐“AI低代码”父目录授权，避免菜单树缺父节点导致子菜单不可见。

**验证**

- Run: `rg -n "[ \t]$" forge/db/migration/V1.0.11__reparent_lowcode_menus.sql code-copilot/changes/lowcode-business-domain-redesign/tasks.md`
- Run: `cd forge && mvn -pl forge-admin-server -am compile -DskipTests`
- 结果：尾随空白检查无输出；后端 admin 编译通过。未改前端文件，未执行前端构建。

## 9. 阶段 6：AI 生成领域上下文注入

**状态：pending**

**目标**

AI CRUD 生成支持选择业务领域，提示词注入领域上下文，生成结果落入领域化 schema。

**计划改动文件**

- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/StreamGenerateRequest.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/CrudGeneratorStreamService.java`
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/AiCrudGenerateRequest.java`
- Modify: `forge-admin-ui/src/api/crud-generator.js`
- Modify: `forge-admin-ui/src/composables/useCrudGenerator.js`
- Modify: `forge-admin-ui/src/views/ai/crud-generator.vue`

**任务清单**

- [ ] `StreamGenerateRequest` 增加 `domainId/objectCode/objectName`。
- [ ] 后端生成提示词注入领域描述、术语、命名规则、字段模板、字典推荐和安全策略。
- [ ] 如果领域未启用，生成请求直接返回明确错误；未选择领域时保持现有 AI 生成兼容路径。
- [ ] 前端 AI 生成页增加领域选择，带出默认 `configKey/tableName` 建议。
- [ ] 生成结果转换到 `schemaVersion=2` 的 `modelSchema`，不改变现有 `searchSchema/columnsSchema/editSchema/apiConfig` 输出。

**验证**

- Run: `cd forge && mvn -pl forge-admin-server -am compile -DskipTests`
- Run: `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm build`
- 重点检查：不传 `domainId` 的旧生成流程仍可用；传 `domainId` 时提示词含领域上下文。

## 10. 阶段 7：测试、文档与收口

**状态：pending**

**目标**

补齐测试说明、自动化测试和执行日志，确保实现与 spec 对齐。

**计划改动文件**

- Create: `code-copilot/changes/lowcode-business-domain-redesign/test-spec.md`
- Modify: `code-copilot/changes/lowcode-business-domain-redesign/spec.md`
- Modify: `code-copilot/changes/lowcode-business-domain-redesign/tasks.md`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDomainServiceTest.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeModelSchemaCompatibilityTest.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodePublishDomainMenuTest.java`

**任务清单**

- [ ] `test-spec.md` 覆盖领域 CRUD、领域树、工作台统计、默认规则、应用按领域分页、草稿绑定领域、发布菜单父级、旧 schema 兼容、AI 生成上下文。
- [ ] 补充后端单元测试，优先覆盖编码唯一性、启停状态、未归属应用、菜单父级兜底、版本快照。
- [ ] 执行后端编译、前端构建和可行的测试命令，记录结果到 spec 执行日志。
- [ ] 更新 `tasks.md` 每个阶段实际改动文件和验证结果。
- [ ] Review 前自查：是否有新增查询写在 Service wrapper、是否有 `{id}` 占位符、是否有 `tenant_id=0`、是否硬编码字典选项。

**验证**

- Run: `cd forge && mvn -pl forge-admin-server -am compile -DskipTests`
- Run: `cd forge && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator test`
- Run: `source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm build`

## 11. 执行日志

| 时间 | 阶段 | 状态 | 实际改动文件 | 验证 |
|------|------|------|--------------|------|
| 2026-05-20 | 0. 规划与门禁 | done | `code-copilot/changes/lowcode-business-domain-redesign/tasks.md` | 未执行构建；本阶段仅任务规划 |
| 2026-05-20 | 1. 数据库与领域后端核心 | done | `forge/db/migration/V1.0.7__add_lowcode_business_domain.sql`；`AiLowcodeDomain.java`；`LowcodeDomainDTO.java`；`LowcodeDomainStatusDTO.java`；`LowcodeDomainSchema.java`；`LowcodeDomainVO.java`；`LowcodeDomainTreeVO.java`；`LowcodeDomainWorkspaceVO.java`；`AiLowcodeDomainMapper.java`；`AiLowcodeDomainMapper.xml`；`LowcodeDomainService.java`；`LowcodeDomainController.java` | `cd forge && mvn -pl forge-admin-server -am compile -DskipTests` 通过 |
| 2026-05-20 | 2. 应用领域绑定与模型协议兼容 | done | `AiCrudConfig.java`；`AiCrudConfigVersion.java`；`LowcodeAppDraftDTO.java`；`LowcodeMoveDomainDTO.java`；`LowcodeModelSchema.java`；`LowcodeDomainRef.java`；`LowcodeObjectSchema.java`；`LowcodeRelationSchema.java`；`LowcodePolicySchema.java`；`LowcodeAppDetailVO.java`；`AiCrudConfigMapper.java`；`AiCrudConfigMapper.xml`；`AiCrudConfigVersionMapper.xml`；`LowcodeAppService.java`；`LowcodeAppController.java`；`V1.0.8__fix_lowcode_domain_workspace_route.sql` | `cd forge && mvn -pl forge-admin-server -am compile -DskipTests` 通过；前端手动路由未保留 |
| 2026-05-20 | 3. 发布菜单与版本快照领域化 | done | `LowcodePublishService.java`；`MenuRegisterAdapter.java`；`MenuRegisterAdapterImpl.java`；`LowcodeVersionVO.java`；`SysResourceMapper.java`；`SysResourceMapper.xml` | `cd forge && mvn -pl forge-admin-server -am compile -DskipTests` 通过 |
| 2026-05-20 | 4. 领域工作台与应用列表前端 | done | `lowcode-crud.js`；`lowcode-apps.vue`；`DomainTreePanel.vue`；`DomainWorkspacePane.vue`；`DomainEditorDrawer.vue`；`MoveDomainModal.vue` | `pnpm exec eslint --fix ...` 通过；`NODE_OPTIONS=--max-old-space-size=4096 pnpm build` 通过 |
| 2026-05-21 | 5R-F. 交互反馈纠偏 | done | `lowcode-apps.vue`；`lowcode-builder.vue`；`lowcode-models.vue`；`DomainTreePanel.vue`；`LowcodeModelDesigner.vue`；`DictTypeSelect.vue`；`V1.0.10__dedupe_lowcode_app_menu.sql` | `pnpm exec eslint --fix ...` 通过；`NODE_OPTIONS=--max-old-space-size=8192 pnpm build` 通过；`mvn -pl forge-admin-server -am compile -DskipTests` 通过；`curl -I http://127.0.0.1:5173/ai/lowcode-apps` 返回 `200 OK` |
| 2026-05-21 | 5R-G. 页面设计与预览纠偏 | done | `MenuParentSelect.vue`；`PublishPanel.vue`；`lowcode-builder.vue`；`DomainEditorDrawer.vue`；`StructuredListPageDesigner.vue`；`LowcodePageBuilder.vue`；`FormCreateDesignerAdapter.vue`；`page-schema.js`；`LowcodePreviewPane.vue` | `pnpm exec eslint --fix ...` 通过，仅保留既有单文件多组件 warning；`NODE_OPTIONS=--max-old-space-size=8192 pnpm build` 通过；`curl -I http://127.0.0.1:5173/ai/lowcode-builder` 返回 `200 OK` |
| 2026-05-21 | 5R-H. 低代码菜单归位 | done | `V1.0.11__reparent_lowcode_menus.sql`；`tasks.md` | 尾随空白检查无输出；`mvn -pl forge-admin-server -am compile -DskipTests` 通过 |
