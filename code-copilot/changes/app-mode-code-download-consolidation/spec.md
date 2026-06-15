# 应用模式与代码下载能力收敛
> status: implemented
> created: 2026-06-14
> complexity: 🔴复杂

## 1. 背景与目标

当前低代码能力存在两条用户链路：

- 业务用户从【应用总览 / 应用管理】进入业务域、业务单元和访问入口。
- 技术配置仍散落在 `/ai/crud-config`、低代码搭建器、模型资产等入口中，且代码预览、代码下载、运行配置等能力会暴露 CRUD、Schema、configKey 等技术名词。

本次改造目标是把低代码应用的主链路统一收敛到【应用管理】，并把应用配置拆成两种使用模式：

- `DYNAMIC_RENDER`：在线运行模式，保留现有在线搭建、发布和动态渲染能力。
- `CODE_DOWNLOAD`：下载代码模式，面向简单单表数据管理等基础场景，用户在应用内预览、下载完整代码包后导入本地工程二次开发，不再依赖在线动态渲染。

完成后，终端用户不再看到独立“CRUD 配置”入口，不再在普通业务界面看到 CRUD、低代码搭建器、模型资产、Schema、configKey 等技术词；代码下载模式生成的代码包必须使用当前应用对应的业务专属接口，禁止继续使用 `/ai/crud/` 通用运行时接口。

## 2. 现状调研

### 2.1 后端入口

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/AiCrudConfigController.java`
  - 旧入口为 `@RequestMapping("/ai/crud-config")`。
  - 当前提供配置分页、详情、按 key 查询、渲染配置、创建、修改、删除、AI 生成、从表生成、`/codegen/download/{configKey}` 下载代码包。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/LowcodeAppController.java`
  - 已提供低代码应用维度代码能力：`GET /ai/lowcode/app/{id}/code/preview`、`GET /ai/lowcode/app/{id}/code/download`、`GET/PUT /ai/lowcode/app/{id}/code/options`。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessAppController.java`
  - 当前只提供应用入口分页、列表、详情、打开信息、创建、修改、启停、删除、同步已发布配置。
  - 尚未提供应用管理维度的代码预览、代码下载和代码包配置接口。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/DynamicCrudController.java`
  - 在线动态运行入口为 `/ai/crud/{configKey}`，动态渲染模式仍依赖该运行时。

### 2.2 后端代码生成链路

- `LowcodeCodegenService` 负责根据低代码应用草稿、发布版本或历史版本解析生成配置，并委托 `AiCrudCodegenService` 输出预览文件或 zip。
- `AiCrudCodegenService` 通过 `CodegenStrategy` 分发到 `VelocityCodegenStrategy`。
- `VelocityCodegenStrategy` 已能生成后端 Controller、Service、Mapper、Mapper XML、DTO、Query、前端页面、前端 API、菜单 SQL、字典 SQL 和原始配置 JSON。
- `VelocityCodegenStrategy#resolveApiBase` 已有兜底逻辑：当发现 `/ai/crud/{configKey}` 或 `/rest/` 时，会降级为 `/{configKey}`。但这只是模板层兜底，不等于应用维度的业务专属接口契约。
- 当前模板 `templates/vm/controller.java.vm` 和 `templates/vm/ai-crud/index.vue.vm` 使用 `getById/add/edit/remove` 风格接口，其中详情、更新、删除按既有安全约束使用 POST，不能改成 PUT/DELETE。

### 2.3 应用管理前端

- `forge-admin-ui/src/views/app-center/index.vue`
  - 当前“应用总览”已按业务域左侧导航、右侧业务单元和访问入口卡片展示。
  - 左侧在业务域增多时已有搜索，但还需要更明确的“业务域导航”定位，避免把大量应用直接堆在左侧。
  - 右侧对象、入口和重复按钮需要继续归并，核心操作应聚合到业务单元卡片和入口更多菜单。
- `forge-admin-ui/src/views/app-center/components/AppEditorDrawer.vue`
  - 当前支持挂载位置、打开方式、关联业务对象、业务对象打开方式、发布配置、菜单同步等配置。
  - 尚未提供“在线运行 / 下载代码”应用模式选择。
- `forge-admin-ui/src/views/app-center/components/BusinessUnitCard.vue`
  - 当前已经把业务对象和访问入口归并到同一卡片。
  - 尚未展示代码下载模式入口，也未提供代码预览 / 下载 / 代码包设置动作。
- `forge-admin-ui/src/views/app-center/components/designer/BusinessAdvancedConfig.vue`
  - 当前开发者模式仍暴露“模型资产”“低代码搭建器”“CRUD 配置”，API 配置中直接展示 `/ai/crud/{configKey}`。
  - 本次需要面向终端用户隐藏这些入口和术语。
- `forge-admin-ui/src/api/business-app.js`
  - 当前有业务套件、业务对象、应用入口相关 API。
  - 当前仍存在 `syncPublishedCrudConfigs`、`dynamicCrudImportTemplate`、`dynamicCrudExport`、`dynamicCrudImport` 等命名和通用路径。
- `forge-admin-ui/src/api/lowcode-crud.js`
  - 已有低代码应用代码预览和下载前端方法，但入口不在应用管理主链路。

### 2.4 菜单与初始化数据

- `forge-server/db/migration/V1.0.19__unify_lowcode_app_codegen_menus.sql`、`V1.0.34__normalize_lowcode_developer_menus.sql`、`V1.0.41__add_business_object_designer_menu_permissions.sql` 已多次尝试收敛开发者菜单。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/sql/ai_crud_config.sql` 和部分初始化脚本仍包含 `/ai/crud-config`、`CRUD配置`、`CRUD生成器` 等旧资源和文案。
- 需要新增迁移脚本，最终保证终端用户菜单不可见，并为应用管理代码能力补充按钮权限。

## 3. 范围

### 3.1 本次必须完成

- 废弃 `/ai/crud-config` 独立配置入口：菜单、路由入口、页面跳转和普通用户可见按钮全部下线。
- 将代码预览、代码下载、代码包配置统一迁移到【应用管理】模块内。
- 应用配置新增应用模式：
  - 在线运行：保留现有动态页面能力。
  - 下载代码：应用不打开动态运行页，只提供代码配置、预览和下载。
- 下载代码模式必须生成业务专属接口路径，不能生成 `/ai/crud/`。
- 下载代码模式输出 zip 必须包含该应用完整功能代码。
- 普通用户界面隐藏 CRUD、模型资产、低代码搭建器、CRUD 配置、Schema、configKey 等技术词。
- 高级配置中下线模型资产、低代码搭建器、原配置入口等终端用户不可见入口。
- 应用总览布局继续按“业务域导航 + 业务单元卡片 + 访问入口”组织，业务对象和应用入口整合展示。
- 右侧重复操作收敛为：
  - 业务单元操作：详情、数据设计、流程与自动化、启停、删除。
  - 访问入口操作：打开、配置、代码、启停、删除。
- 用户可见业务名词统一：
  - `业务套件` → `业务域`
  - `业务对象` → `业务单元`
  - `应用入口` → `访问入口`
  - `CRUD` → `数据管理`、`业务页面`、`功能代码`
  - `代码生成配置` → `代码包设置`

### 3.2 本次不做

- 不重写动态渲染运行时。
- 不删除 `/ai/crud/{configKey}` 内部动态运行接口；在线运行模式仍可继续使用。
- 不一次性迁移所有历史文档中的 CRUD 字样，只处理终端用户可见页面、菜单、接口命名和本次变更代码。
- 不实现主子表、左树右表、树表等复杂下载模式的新能力；复杂场景继续走在线运行或后续扩展。
- 不改变已有用户数据的 `ai_crud_config` 表结构和历史配置含义。
- 不删除模型资产和低代码搭建器后端能力，只从终端用户界面和菜单中隐藏。

## 4. 核心设计

### 4.1 应用模式

应用模式只对 `entryMode = RUNTIME` 的业务类访问入口生效。

| 模式值 | 用户文案 | 行为 |
|--------|----------|------|
| `DYNAMIC_RENDER` | 在线运行 | 保持当前在线搭建、发布、动态页面打开能力 |
| `CODE_DOWNLOAD` | 下载代码 | 不打开动态页面，提供代码包设置、代码预览、下载完整代码 |

存储策略：

- 首期不新增 `ai_business_app` 表字段，应用模式写入 `ai_business_app.options.appMode`。
- `BusinessAppDTO` 和 `BusinessAppVO` 增加 `appMode` 字段，Service 负责从 `options` 解析和回写。
- 历史应用默认 `DYNAMIC_RENDER`，保证现有入口行为不变。
- 非 `RUNTIME` 入口忽略 `appMode`，保存时移除无效模式。

### 4.2 下载代码模式约束

选择 `CODE_DOWNLOAD` 时必须满足：

- 访问入口已关联业务域和业务单元。
- `configKey` 可解析到可用运行配置。
- 应用状态启用后，卡片主按钮默认进入代码面板，不打开 `/ai/crud-page/{configKey}`。
- 代码包设置中可配置 `sourceType`、`versionId`、`domainPackage`、`moduleName`、`author`、SQL 包含项、前端输出路径。
- 代码包 API 路径默认按业务域和业务单元生成：`/{suiteCode-kebab}/{objectCode-kebab}`。
- 允许实施人员在代码包设置中覆盖业务接口前缀，但必须通过校验：
  - 必须以 `/` 开头。
  - 不能以 `/ai/crud/`、`/ai/crud-config`、`/ai/lowcode/`、`/rest/` 开头。
  - 不能包含 `{configKey}`、`crud` 等面向旧运行时的标识。
- 生成的前端 API 和页面必须使用该业务接口前缀。
- 生成的后端 Controller 必须使用 Forge 代码生成器约定的安全调用风格：
  - `GET /page`
  - `GET /list`
  - `POST /getById`
  - `POST /add`
  - `POST /edit`
  - `POST /remove/{id}`
  - 批量删除继续使用 `POST /removeBatch`，禁止生成 PUT/DELETE。

### 4.3 应用管理代码接口

新增接口统一挂在应用管理：

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/ai/business/app/{id}/code/options` | GET | 查询代码包设置 | `ai:businessApp:code` |
| `/ai/business/app/{id}/code/options` | PUT | 保存代码包设置 | `ai:businessApp:code` |
| `/ai/business/app/{id}/code/preview` | GET | 预览代码文件列表和内容 | `ai:businessApp:codePreview` |
| `/ai/business/app/{id}/code/download` | GET | 下载完整 zip | `ai:businessApp:codeDownload` |

实现策略：

- 新增 `BusinessAppCodegenService`，以业务应用为入口解析应用、运行配置、应用模式和业务接口前缀。
- 复用 `LowcodeCodegenRequest`，新增 `businessApiBase` 字段；也可以新建 `BusinessAppCodegenRequest` 继承/组合现有字段。
- 复用 `AiCrudCodegenService` 和 `VelocityCodegenStrategy`，但输入给模板前必须把 `apiConfig` 改写为业务专属接口。
- 输出文件沿用 `LowcodeCodePreviewVO` 或新增轻量 VO，避免重复定义文件预览结构。

### 4.4 旧入口下线策略

`/ai/crud-config` 相关能力处理如下：

- 菜单和页面入口对终端用户全部隐藏。
- 前端 `/ai/crud-config.vue` 不再作为配置页使用，访问时提示“该配置入口已迁移到应用管理”，并提供跳转到 `/app-center`。
- `AiCrudConfigController` 保留 `/render/{configKey}` 给动态渲染模式内部使用。
- `page/detail/by-key/create/update/delete/ai-generate/generateFromTable/codegen-download` 不再作为普通用户功能入口。
- 旧 `/ai/crud-config/codegen/download/{configKey}` 标记废弃；前端不再调用。后端可短期保留兼容，但必须增加权限或迁移提示，并在日志中提示使用应用管理代码下载接口。
- `sync-published-crud-configs` 等内部方法需要在 UI 文案改名为“同步已发布应用”，避免 CRUD 字样出现在按钮或提示中。

### 4.5 高级配置与术语屏蔽

普通模式：

- 不展示模型编码、表名、Schema、configKey、运行配置键。
- 不展示模型资产、低代码搭建器、CRUD 配置等跳转。
- 用业务语言展示：业务单元、字段数量、页面区域、发布状态、数据管理能力。

开发者模式：

- 本次仍不提供旧配置入口跳转。
- 可保留必要技术摘要，例如对象编码、数据表名、运行配置，但不直接提供独立配置页。
- API 预览不再展示 `/ai/crud/{configKey}`；若是在线运行模式，展示为“在线运行接口由平台托管”；若是下载代码模式，展示业务接口前缀。

### 4.6 应用总览交互

左侧：

- 左侧保持“业务域导航”，不承载应用列表。
- 业务域增多时通过搜索、滚动、计数和当前选中态定位。
- “全部业务域”保留，用于跨业务域检索。
- 后续可扩展置顶/最近访问，但本次不强制实现。

右侧：

- 以“业务单元卡片”为主，访问入口内嵌到卡片内。
- 独立访问入口归到“独立访问入口”分组，避免无业务单元入口散落。
- 重复按钮收敛到卡片底部和更多菜单。
- 下载代码模式入口在访问入口行上展示“功能代码”状态或动作，不新增单独页面入口。

## 5. 数据与迁移

### 5.1 字典

新增字典类型建议为 `ai_business_app_mode`：

| value | label | 说明 |
|-------|-------|------|
| `DYNAMIC_RENDER` | 在线运行 | 动态渲染模式 |
| `CODE_DOWNLOAD` | 下载代码 | 代码包交付模式 |

脚本要求：

- 放入 `forge-server/db/migration/`。
- 使用当前最大版本之后的新版本号。
- `tenant_id` 必须为 `1`。
- `sys_dict_type` 和 `sys_dict_data` 插入必须具备 `NOT EXISTS` 防重复。

### 5.2 菜单与权限

新增或补齐按钮权限：

- `ai:businessApp:code`
- `ai:businessApp:codePreview`
- `ai:businessApp:codeDownload`

隐藏或停用旧资源：

- `/ai/crud-config`
- `/ai/crud-generator`
- `/ai/lowcode-builder`
- `/ai/lowcode-models`
- 其他终端用户可见的旧配置入口。

迁移脚本必须只调整菜单可见性和状态，不删除历史数据。

### 5.3 应用 options

应用保存时 `options` 结构示例：

```json
{
  "mountTarget": "ADMIN",
  "runtimeOpenMode": "LIST",
  "appMode": "CODE_DOWNLOAD",
  "codegen": {
    "businessApiBase": "/crm/customer",
    "domainPackage": "com.mdframe.forge",
    "moduleName": "crm",
    "author": "Forge Generator",
    "includeSql": true,
    "includeMenuSql": true,
    "includeDictSql": true,
    "frontendBasePath": "frontend/src/views"
  }
}
```

## 6. 代码包要求

下载代码模式 zip 必须包含：

- `backend/src/main/java/.../entity/*.java`
- `backend/src/main/java/.../dto/*DTO.java`
- `backend/src/main/java/.../dto/*Query.java`
- `backend/src/main/java/.../mapper/*Mapper.java`
- `backend/src/main/java/.../service/I*Service.java`
- `backend/src/main/java/.../service/impl/*ServiceImpl.java`
- `backend/src/main/java/.../controller/*Controller.java`
- `backend/src/main/resources/mapper/*Mapper.xml`
- `frontend/src/api/*.js`
- `frontend/src/views/**/index.vue`
- `sql/*_menu.sql`，当 `includeMenuSql=true`
- `sql/*_dict.sql`，当存在字典配置且 `includeDictSql=true`
- `config/*-config.json`
- `README.md`，说明导入步骤、后端包路径、前端路由、接口前缀和 SQL 导入顺序。

代码包验收规则：

- zip 内任何前端 API、页面、Controller 注释、README 不得出现 `/ai/crud/`。
- 用户可见文案不出现 CRUD。
- Controller 使用业务专属 `@RequestMapping`。
- 前端页面 `apiConfig` 使用业务专属路径。
- 生成代码能独立导入 Forge 工程后按标准接口编译。

## 7. 兼容性

- 历史在线应用默认 `DYNAMIC_RENDER`，打开行为不变。
- 动态渲染模式允许内部继续使用 `/ai/crud-page/{configKey}` 和 `/ai/crud/{configKey}`，但不向普通用户暴露。
- 旧 `AiCrudConfigController#render` 保留，避免动态页面失效。
- 旧代码下载 URL 前端不再调用；后端是否保留短期兼容由实现阶段评估，但必须不再作为主链路。
- 菜单迁移只隐藏旧入口，不物理删除资源，方便回滚。

## 8. 安全与权限

- 应用代码下载必须校验应用查看/代码权限。
- 代码下载模式只能对当前租户可见配置生成代码。
- 代码包设置不可保存敏感密钥、数据库密码、Token、AK/SK。
- 生成 README 不输出本地数据库密码。
- 旧配置入口下线后，不应因前端隐藏但后端无权限导致越权访问；旧管理类接口需要补权限或明确废弃。

## 9. 验收标准

- 应用编辑抽屉可选择“在线运行 / 下载代码”，历史应用默认在线运行。
- 下载代码模式应用卡片不再打开动态运行页，能在应用管理内配置、预览、下载代码。
- 代码预览文件列表完整，下载 zip 包含完整前后端、Mapper、SQL、配置说明。
- 下载代码模式生成代码中没有 `/ai/crud/`。
- `/ai/crud-config` 不再出现在普通用户菜单、应用总览、应用编辑、高级配置和代码下载入口中。
- 高级配置普通模式不显示模型资产、低代码搭建器、CRUD 配置、Schema、configKey。
- 应用总览继续以业务域导航和业务单元卡片承载多应用场景，业务对象和访问入口整合展示。
- 用户可见页面不出现“CRUD”字样。
- 后端编译通过；前端构建或至少相关文件 lint 通过。

## 10. 风险与处理

- 风险：完全关闭 `/ai/crud-config` 可能破坏动态渲染。
  - 处理：保留 `/render/{configKey}`，只下线配置管理和代码下载主入口。
- 风险：历史配置 `apiConfig` 仍带 `/ai/crud/`。
  - 处理：下载代码模式生成前统一覆盖为业务专属 API；动态模式不做强制改写。
- 风险：菜单资源历史脚本使用 `tenant_id=0`。
  - 处理：新增脚本按当前项目规范使用 `tenant_id=1`，并兼容历史数据条件。
- 风险：应用模式存入 options 可能不利于查询。
  - 处理：首期列表无需按模式复杂查询；后续如果需要统计再迁移为显式字段。
- 风险：用户仍直接输入旧路由。
  - 处理：前端旧页面给出迁移提示；后端旧管理接口加权限或废弃响应。

## 11. 实现结论

- 下载代码模式业务接口前缀默认使用 `/{suiteCode-kebab}/{objectCode-kebab}`，本次不统一加 `/api` 前缀；代码包设置允许实施人员覆盖，但后端会拦截 `/ai/crud/`、`/ai/crud-config`、`/ai/lowcode/`、`/rest/` 和包含旧配置标识的路径。
- 旧 `/ai/crud-config/codegen/download/{configKey}` 短期保留管理员兼容能力，已增加权限校验和废弃日志；应用管理前端不再调用该入口。
- 生成 Controller 已改为业务专属路径，并按既有生成器安全约束保持详情、更新、删除走 POST：`/getById`、`/add`、`/edit`、`/remove/{id}`；直接使用已校验的 `businessApiBase` 作为 `@RequestMapping`，确保前后端生成路径一致。
- 应用模式首期写入 `ai_business_app.options.appMode`，历史 `RUNTIME` 访问入口默认 `DYNAMIC_RENDER`，非 `RUNTIME` 入口不保留模式配置。
- 旧配置页前端改为迁移提示页；菜单迁移脚本只隐藏旧入口，不删除历史资源。
