# 低代码运行时数据源隔离改造
> status: propose
> created: 2026-06-21
> complexity: 🔴复杂

## 1. 背景与目标

当前 ForgeAdmin 已经具备低代码应用中心、业务对象设计器、模型导入、发布运行和动态 CRUD 能力。用户希望在已有成熟第三方业务系统已长期生产运行的前提下，用 ForgeAdmin 承接新的低代码二开功能，两套系统平行运行，ForgeAdmin 尽量零侵入旧系统。

现有尝试的目标场景可以拆成三类：

1. `/ai/crud` 动态低代码应用：根据动态页面配置中的业务对象或数据模型，读写该业务对象绑定的数据源。
2. `forge-business` 手写复杂业务模块：根据当前租户绑定的默认业务数据源执行复杂业务逻辑。
3. Forge 平台能力：用户、租户、菜单、权限、字典、低代码元数据、系统配置等继续使用 `application.yml` 配置的主数据源。

目前低代码模型导入已经可以从旧系统数据源读取表结构，但后续 DDL 同步和运行态 CRUD 仍使用 Forge 主数据源，导致：

- 数据源连接的是旧系统，低代码应用发布时却把表同步创建到 `forge_admin`。
- 页面运行时新增、修改、删除的数据也落到 `forge_admin`。
- 低代码应用中心的数据源和业务模块的租户默认数据源缺少清晰边界。

本变更目标：

- 将低代码应用中心的数据源与业务模块数据源分离。
- 低代码应用运行时以“业务对象/模型绑定数据源”为准，不再默认写入 Forge 主库。
- `forge-business` 手写业务模块可按“租户默认业务数据源”切换。
- 平台功能始终走主数据源，低代码运行数据和平台元数据物理隔离。
- 发布、DDL、运行、导入导出、就绪度检查都能感知目标数据源，避免误写库。

### 1.1 核心术语

| 术语 | 含义 | 数据来源 |
|------|------|----------|
| 平台主数据源 | ForgeAdmin 自身运行库，保存系统用户、权限、菜单、低代码元数据等 | `spring.datasource.dynamic.datasource.master` |
| 数据源注册表 | Forge 管理的外部数据库连接配置 | `gen_datasource` |
| 低代码运行数据源 | 低代码业务对象发布后，动态 CRUD 实际读写的业务数据源 | `ai_lowcode_model` / `ai_crud_config` 绑定 |
| 租户默认业务数据源 | 手写 `forge-business` 模块按当前租户解析的默认业务库 | `sys_tenant` 绑定 |
| 业务对象绑定数据源 | 某个低代码模型或对象自己的运行数据源，优先级高于租户默认业务数据源 | 模型/应用发布配置 |

### 1.2 范围

本期必须完成：

- 低代码模型、发布配置、DDL 和动态 CRUD 运行态贯通业务对象绑定数据源。
- 应用中心展示和维护低代码对象的运行数据源。
- 发布检查基于目标数据源执行表结构校验。
- `forge-business` 提供租户默认业务数据源解析能力和配置入口。
- 兼容历史低代码应用，未绑定运行数据源的应用继续走主库。

本期不做：

- 不把平台元数据迁移到外部业务库。
- 不做跨数据源 SQL Join。
- 不做跨数据源强事务。
- 不自动修改旧系统表结构，除非数据源允许 DDL 且用户二次确认。
- 不让前端运行时请求临时指定数据源。
- 不重写 `AiCrudPage` 和动态 CRUD 的产品形态。

## 2. 代码现状（Research Findings）

### 2.1 低代码模型导入已经记录来源数据源

`LowcodeModelImportService#previewDbTableModel` 会根据请求的数据源读取表结构，并把数据源信息写入 `LowcodeModelSchema.sourceTable`：

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeModelImportService.java`
- 方法：`previewDbTableModel`
- 关键行为：
  - `schema.setTableMode("EXISTING")`
  - `schema.setTableName(context.table().getTableName())`
  - `schema.setSourceTable(buildSourceTable(context.datasource(), context.table()))`

`LowcodeSourceTableRef` 已有字段：

- `datasourceId`
- `datasourceCode`
- `datasourceName`
- `dbType`
- `tableName`
- `tableComment`

结论：模型导入阶段已经具备“来自哪个数据源”的元数据基础，但后续发布和运行链路没有继续使用。

### 2.2 低代码 DDL 固定使用主库 JdbcTemplate

`LowcodeDdlRepository` 当前注入的是 Spring 主上下文中的 `JdbcTemplate`：

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDdlRepository.java`
- 字段：`private final JdbcTemplate jdbcTemplate`
- 方法：`tableExists`、`listColumns`、`listColumnMetadata`、`hasAutoIncrementPrimaryId`、`executeDdl`

这些方法都使用 `DATABASE()` 检查当前连接所在库，因此实际检查和执行的是 Forge 主数据源。

结论：即使模型来自旧系统数据源，在线建表、字段同步和发布校验仍会落到 `forge_admin`。

### 2.3 动态 CRUD 固定使用主库 NamedParameterJdbcTemplate

`DynamicCrudRepository` 当前注入的是单一 `NamedParameterJdbcTemplate`：

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudRepository.java`
- 字段：`private final NamedParameterJdbcTemplate namedJdbcTemplate`
- 查询、插入、更新、删除均直接调用该模板。

关键方法：

- `selectPage`
- `selectById`
- `insertReturningId`
- `updateById`
- `deleteById`
- `tableExists`
- `getColumnMapping`
- `getTableColumns`

结论：动态 CRUD 的运行时数据读写没有数据源上下文，天然写入主数据源。

### 2.4 发布校验只检查主库表结构

`LowcodePublishService#publish` 发布时调用：

- `ensureTableReady(modelSchema, dto)`
- `policyService.validatePublishedPolicies(modelSchema, ddlService.listColumns(modelSchema.getTableName()))`
- `ddlService.executeCreateTable(modelSchema)`

相关文件：

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodePublishService.java`

结论：发布阶段只知道表名，不知道表在哪个数据源里；发布成功后的 `ai_crud_config` 也没有冗余运行数据源字段。

### 2.5 动态 CRUD 假设业务表具备 Forge 标准字段

`DynamicCrudRepository` 会根据表字段自动追加部分条件和审计字段：

- 查询时：如果表存在 `tenant_id`，追加租户条件；如果存在 `del_flag`，追加逻辑删除条件。
- 插入时：如果表存在 `tenant_id`、`create_by`、`create_time`、`create_dept`、`update_by`、`update_time`，自动填充。
- 更新和删除时：当前 `appendTenantCondition` 直接追加 `AND tenant_id = :tenantId`，未先判断目标表是否存在 `tenant_id`。

相关文件：

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudRepository.java`
- 方法：`appendBaseQueryConditions`、`appendTenantCondition`、`fillInsertAuditFields`、`fillUpdateAuditFields`

结论：旧系统表若没有 Forge 标准审计字段，部分操作会报错或无法满足零侵入诉求；需要显式建模租户、审计、逻辑删除策略。

### 2.6 数据源管理已有连接池工具

`DynamicDataSourceUtil` 已支持按 `GenDatasource` 创建并缓存 Hikari 数据源：

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/util/DynamicDataSourceUtil.java`
- 方法：`getConnection`、`testConnection`、`removeDataSource`、`clearAll`

`GenDatasourceServiceImpl` 已使用该工具读取指定数据源表结构：

- `selectDbTableList`
- `selectDbTableByName`
- `selectDbTableColumnsByName`

结论：可复用现有数据源注册表和连接创建能力，但需要封装成 Spring 可注入的运行时 JDBC 模板提供器，避免业务代码散落静态工具调用。

### 2.7 租户表当前没有默认业务数据源字段

`SysTenant` 当前保存租户名称、联系人、状态、主题配置等字段，没有业务数据源绑定字段：

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysTenant.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysTenantMapper.xml`

结论：`forge-business` 按租户默认业务数据源切换需要新增租户配置字段和解析器。

### 2.8 项目已有 dynamic-datasource 依赖

`forge-starter-orm` 已依赖：

- `com.baomidou:dynamic-datasource-spring-boot3-starter`

本地配置中主数据源为：

- `spring.datasource.dynamic.primary=master`
- `spring.datasource.dynamic.datasource.master`

结论：工程基础已支持 dynamic-datasource 生态。低代码动态 CRUD 由于本身是动态 SQL/JdbcTemplate 场景，可以继续由低代码运行上下文选择目标 JDBC 模板；但 `forge-business` 手写业务模块不能采用独立 JDBC 模板路由，必须接入 dynamic-datasource，让 MyBatis-Plus Mapper/XML 在同一个 ORM 链路内完成数据源切换。

官方 dynamic-datasource 使用模型参考：https://baomidou.com/guides/dynamic-datasource/ 。本变更按以下边界设计：

- 平台主库仍是 `spring.datasource.dynamic.primary=master`。
- 业务模块显式进入租户业务数据源时，通过 dynamic-datasource 的上下文选择数据源；方法级声明优先于类级声明。
- 数据源不存在时是否严格失败由 `spring.datasource.dynamic.strict` 和业务侧全局开关共同控制；业务模块默认要求解析不到租户数据源时回退 `master`，但解析到已禁用/不可用数据源时失败。
- 运行期不从低代码 `gen_datasource` 注册业务库；`forge-business` 只校验并切换租户表中配置的 dynamic-datasource dsKey，不再维护一套绕开 MyBatis-Plus 的 `JdbcTemplate` 路由。

### 2.9 纠偏前 `forge-business` 多数据源实现偏离目标

纠偏前 `forge-business-core` 已新增：

- `BusinessTenantDataSourceResolver`
- `BusinessDataSourceContextHolder`
- `BusinessDataSourceExecutor`
- `BusinessJdbcTemplateProvider`

现有实现存在三个关键问题：

1. `BusinessJdbcTemplateProvider` 复用低代码 `RuntimeJdbcTemplateProvider`，只能影响手写 `JdbcTemplate`/`NamedParameterJdbcTemplate`，不能影响业务侧 MyBatis-Plus Mapper/XML。后续 `forge-business` 真实复杂业务如果按项目规范走 Mapper/XML，仍会落到当前 dynamic-datasource 默认主库。
2. `BusinessDataSourceContextHolder` 是独立 ThreadLocal，和 dynamic-datasource 的上下文不是同一个体系。即使 Service 外层设置了该上下文，MyBatis-Plus 获取连接时也不会读取它。
3. 异步任务、调度任务、事件监听任务不会自动继承该 ThreadLocal；如果异步逻辑里执行业务 Mapper，会丢失租户和数据源上下文，最终回到主库或错误数据源。

结论：阶段五的 `forge-business` 方案需要纠偏。保留 `sys_tenant.default_business_datasource_id/code` 作为“租户选择哪个数据源”的配置，其中 `default_business_datasource_code` 必须对应 baomidou dynamic-datasource 已配置的数据源名称；新增全局开关、AOP/执行器和异步上下文传播，确保业务侧 ORM 层仍由 MyBatis-Plus 控制。

## 3. 功能点

### 3.1 数据源注册表能力增强

- [ ] `gen_datasource` 增加运行用途配置，用于区分低代码运行、租户业务模块、开发者导入等场景。
- [ ] 数据源支持配置是否允许运行时写入。
- [ ] 数据源支持配置是否允许低代码执行 DDL。
- [ ] 数据源列表接口支持按用途筛选，应用中心只展示允许低代码运行的数据源，租户配置只展示允许业务模块使用的数据源。
- [ ] 旧系统生产库或高风险数据源默认开启只读模式，默认禁止 DDL；只有管理员显式解除只读并二次确认后才允许写入。
- [ ] 密码继续加密存储，前端详情和列表不得返回明文密码。
- [ ] 编辑数据源后必须清理对应运行时连接池和表元数据缓存。

建议用途枚举：

| 编码 | 说明 |
|------|------|
| `LOWCODE_RUNTIME` | 允许低代码应用运行时读写 |
| `TENANT_BUSINESS` | 允许作为租户默认业务数据源 |
| `DEVELOPER_IMPORT` | 仅用于开发者导入表结构或代码生成 |
| `BOTH` | 兼容历史数据，可同时用于低代码和租户业务 |

建议运行控制字段：

| 字段 | 含义 |
|------|------|
| `usage_scope` | 数据源用途范围 |
| `allow_runtime_write` | 是否允许运行时写入 |
| `allow_runtime_ddl` | 是否允许低代码执行 DDL |
| `readonly` | 是否只读 |
| `risk_level` | 风险等级，例如 `LOW`、`MEDIUM`、`HIGH` |

### 3.2 低代码模型绑定运行数据源

- [ ] 新建空白模型时，用户必须选择运行数据源；默认可选择当前平台主库，但要明确标识“平台主库”。
- [ ] 从数据库表导入模型时，默认将导入表的数据源设置为模型运行数据源。
- [ ] 模型编辑页展示：数据源名称、数据源编码、数据库类型、表名、表模式、是否允许 DDL、是否允许写入。
- [ ] 支持修改模型运行数据源，但必须重新执行表结构检查；已发布应用修改数据源时必须提示风险。
- [ ] `LowcodeModelSchema` 增加运行数据源快照，避免数据源名称修改后历史模型显示不一致。

建议模型协议增加：

```json
{
  "runtimeDatasource": {
    "datasourceId": 1,
    "datasourceCode": "legacy_crm",
    "datasourceName": "旧CRM生产库",
    "dbType": "MySQL",
    "tableName": "crm_customer",
    "tableMode": "EXISTING",
    "allowDdl": false,
    "allowWrite": true
  },
  "primaryKey": {
    "field": "id",
    "columnName": "id",
    "dataType": "bigint",
    "autoIncrement": true
  },
  "tenantStrategy": {
    "mode": "FORGE_TENANT_ID",
    "columnName": "tenant_id"
  },
  "auditStrategy": {
    "mode": "FORGE_COLUMNS"
  },
  "logicDeleteStrategy": {
    "mode": "DEL_FLAG",
    "columnName": "del_flag",
    "activeValue": "0",
    "deletedValue": "1"
  }
}
```

策略枚举：

| 策略 | 支持值 | 说明 |
|------|--------|------|
| `tableMode` | `CREATE`、`EXISTING` | `CREATE` 表示在绑定数据源建表；`EXISTING` 表示绑定旧表 |
| `tenantStrategy.mode` | `FORGE_TENANT_ID`、`NONE`、`CUSTOM_COLUMN` | 旧表无租户字段时必须配置 `NONE` 或自定义字段 |
| `auditStrategy.mode` | `FORGE_COLUMNS`、`NONE`、`CUSTOM_MAPPING` | 控制是否写入 `create_by/update_time` 等字段 |
| `logicDeleteStrategy.mode` | `DEL_FLAG`、`NONE`、`CUSTOM_COLUMN` | 控制删除方式 |

### 3.3 发布配置冗余运行数据源

- [ ] 发布时从模型解析运行数据源，写入 `ai_crud_config`。
- [ ] `ai_crud_config` 冗余保存运行数据源 ID、编码、快照、表名、主键策略、租户策略、审计策略、逻辑删除策略。
- [ ] `ai_crud_config_version` 同步保存上述运行时字段，回滚时按版本快照恢复。
- [ ] 动态 CRUD 运行时只读取已发布 `ai_crud_config` 的运行数据源，不依赖浏览器传参。
- [ ] 历史应用没有运行数据源字段时，默认使用主数据源 `master`，保持兼容。

### 3.4 DDL 和发布检查按绑定数据源执行

- [ ] `LowcodeDdlRepository` 改造成按数据源执行，不再固定使用主库 `JdbcTemplate`。
- [ ] `LowcodeDdlService.previewCreateTable`、`executeCreateTable`、`tableExists`、`listColumns`、`hasAutoIncrementPrimaryId` 都必须接收运行数据源上下文。
- [ ] `LowcodePublishService#ensureTableReady` 基于模型绑定数据源校验表结构。
- [ ] `policyService.validatePublishedPolicies` 使用绑定数据源列清单校验权限字段。
- [ ] 新增数据库方言适配层，首期完整支持 MySQL、PostgreSQL、Oracle 的表存在、列清单、主键、自增/序列、分页和受控 DDL 差异。
- [ ] 在线建表只能在 `tableMode=CREATE` 且数据源 `allow_runtime_ddl=1` 时执行。
- [ ] 绑定已有表时默认只做校验，不追加字段；若用户选择同步旧表字段，必须二次确认并校验数据源允许 DDL。
- [ ] DDL 预览中必须展示目标数据源、数据库类型、表名和每条 DDL。

发布部署模式建议：

| 模式 | 说明 |
|------|------|
| `VERIFY_ONLY` | 只校验绑定数据源中表结构是否满足运行条件 |
| `ONLINE_CREATE_TABLE` | 在绑定数据源中新建 Forge 托管表 |
| `SYNC_EXISTING_TABLE` | 对已有表追加缺失字段或索引，必须二次确认 |
| `SKIP_DDL` | 跳过 DDL，但仍做基础表存在校验 |

### 3.5 动态 CRUD 按发布配置切换数据源

- [ ] 新增 `LowcodeRuntimeDataSourceResolver`，根据 `configKey` 或 `AiCrudConfig` 解析运行数据源。
- [ ] 新增 `RuntimeJdbcTemplateProvider`，根据数据源 ID 返回 `JdbcTemplate` 和 `NamedParameterJdbcTemplate`。
- [ ] `DynamicCrudRepository` 所有查询、写入、元数据读取方法都必须使用运行数据源对应的 JDBC 模板。
- [ ] `DynamicCrudService` 读取配置后构建 `DynamicCrudRuntimeContext`，包含数据源、表名、主键、租户、审计、逻辑删除策略。
- [ ] 元数据缓存 key 从 `tableName` 改为 `datasourceId + schemaName + tableName`，避免不同库同名表污染。
- [ ] 插入、更新、删除、查询都按表策略处理租户、审计和逻辑删除字段。
- [ ] 动态导入、导出、异步导出、导入模板全部使用相同运行数据源。
- [ ] 触发器扫描、公式计算、单据编号、业务事件读取和写入动态表时全部复用同一运行上下文。
- [ ] 动态 CRUD 的分页、主键回填、元数据查询必须走数据库方言适配，首期覆盖 MySQL、PostgreSQL、Oracle。
- [ ] 多模型页面只允许同数据源 SQL Join；不同数据源模型关系第一阶段降级为查主表后批量 lookup 或直接禁止发布。

### 3.6 旧系统表零侵入适配

- [ ] 首期必须支持单字段自定义主键，不强制旧表必须叫 `id`。
- [ ] 支持字符串、数字等单字段主键，主键字段和列名从模型或发布配置读取；复合主键第一阶段不支持发布为可写动态 CRUD。
- [ ] `AiCrudPage` 运行配置下发 `rowKey`，表格详情、编辑、删除按配置主键取值。
- [ ] 动态 CRUD 后端 `/{id}` 路径参数改为字符串接收，按主键类型转换。
- [ ] 旧表无 `tenant_id` 时，`tenantStrategy=NONE` 不追加租户条件。
- [ ] 旧表无 `del_flag` 时，`logicDeleteStrategy=NONE` 执行物理删除；若数据源标记高风险，默认禁止物理删除。
- [ ] 旧表无审计字段时，`auditStrategy=NONE` 不写入 Forge 审计字段。
- [ ] 旧表字段类型、必填、长度和主键不满足运行要求时，发布检查必须给出阻断原因。

### 3.7 租户默认业务数据源（`forge-business` + MyBatis-Plus）

阶段五改造目标调整为：`forge-business` 业务侧仍按项目标准使用 MyBatis-Plus Mapper/XML，不引导业务模块改用 `JdbcTemplate`。数据源切换由 baomidou dynamic-datasource 接管，业务代码只声明“当前方法需要进入租户默认业务数据源”。

#### 3.7.1 全局开关

- [x] 新增 `forge.business.datasource.enabled` 配置项，默认 `false`，用于灰度控制租户业务数据源路由能力。
- [x] 新增系统配置项 `business.datasource.tenant-routing-enabled`，运行期可关闭租户业务数据源路由；关闭后即使租户配置了默认业务数据源，`forge-business` 显式声明方法也必须走 `master`。
- [x] 全局开关关闭时记录 debug 日志，不能报错，便于线上快速回退。
- [x] 全局开关开启后，只有显式注解或显式执行器包裹的业务逻辑进入租户业务数据源；普通平台、系统、低代码元数据接口不受影响。

#### 3.7.2 dynamic-datasource 解析与校验

- [x] 保留 `sys_tenant.default_business_datasource_id` / `default_business_datasource_code`，只负责记录“当前租户默认使用哪个业务数据源”。
- [x] 新增 `TenantBusinessDataSourceResolver`，按 `TenantContextHolder` / `SessionHelper` 获取当前租户，读取 `sys_tenant.default_business_datasource_code`。
- [x] `default_business_datasource_code` 只表示业务租户要使用的 dynamic-datasource dsKey，不复用低代码 `gen_datasource` 连接配置。
- [x] Resolver 校验 dsKey 已存在于 `DynamicRoutingDataSource#getDataSources()`；不存在时抛业务异常，避免静默写错库。
- [x] 租户未配置默认业务数据源时回退 `master`；租户配置了未注册的 dsKey 时失败。

#### 3.7.3 MyBatis-Plus 路由方式

- [x] 新增 `@TenantBusinessDataSource` 注解，建议标在 `forge-business` Service 方法或类上，不建议标在 Mapper 上。
- [x] 新增 AOP 切面：进入注解方法前解析当前租户的数据源 key，并调用 dynamic-datasource 上下文 `push(dsKey)`；finally 中必须 `poll()` 清理，避免线程复用串库。
- [x] 切面顺序必须早于 `@Transactional`，确保事务开启前 dynamic-datasource 已经选中正确数据源。
- [x] 如果方法已经在平台主库事务中，禁止中途切换到租户业务数据源；需要拆分事务边界，避免同一事务内跨库。
- [x] 业务侧 Mapper 查询、XML SQL、分页和 MyBatis-Plus 插件链都走同一个 dynamic-datasource DataSource，不再通过 `BusinessJdbcTemplateProvider` 执行主路径。

建议开发模型：

```java
@TenantBusinessDataSource
public Page<CustomerVO> selectCustomerPage(Page<CustomerVO> page, CustomerQuery query) {
    return customerMapper.selectCustomerPage(page, query);
}
```

或用于需要显式指定租户的后台任务：

```java
tenantBusinessDataSourceExecutor.execute(tenantId, () -> {
    return customerMapper.selectCustomerPage(page, query);
});
```

#### 3.7.4 异步场景

- [x] 不能依赖普通 ThreadLocal 自动进入异步线程；`@Async`、线程池、事件监听、定时任务必须显式传播租户和数据源上下文。
- [x] 新增 `TenantBusinessDataSourceTaskDecorator`，在提交任务时捕获 `tenantId` 和当前 dynamic-datasource key，在异步线程执行前重新设置，finally 中清理。
- [x] `forge.business.datasource.enabled=true` 时对系统级线程池统一配置 TaskDecorator；对手动创建的异步任务必须使用 `TenantBusinessDataSourceExecutor.execute(tenantId, action)`，禁止直接调用业务 Mapper。
- [x] 定时任务、消息消费、补偿任务没有登录态时必须传入明确 `tenantId`；缺少租户上下文时只允许走 `master` 或直接失败，不能猜测租户。
- [x] 异步任务不能跨线程复用未提交事务；业务库写入和平台日志/消息记录按最终一致性处理。

#### 3.7.5 当前实现处理

- [x] 废弃 `BusinessJdbcTemplateProvider` 作为业务模块主路径；如保留，只能用于少量非 Mapper 的工具型 SQL，并且必须显式标注不参与业务 ORM 主链路。
- [x] `BusinessDataSourceContextHolder` 不再作为最终数据源选择依据；最终连接选择必须进入 dynamic-datasource 上下文。
- [x] `SysTenantBusinessDataSourceResolver` 不再返回低代码 `LowcodeRuntimeDataSourceContext`，应返回业务数据源 key、数据源摘要和开关状态。

### 3.8 应用中心前端适配

- [ ] 业务对象创建向导增加“运行数据源”步骤。
- [ ] 从数据库表导入时，导入结果显示数据源和表名，并自动带入对象运行数据源。
- [ ] 业务对象详情展示运行数据源、表名、表模式、主键、租户策略、审计策略、逻辑删除策略。
- [ ] 发布面板展示目标数据源和 DDL 风险提示。
- [ ] 就绪度检查增加数据源维度：数据源可用、表存在、主键可用、字段匹配、允许写入、允许 DDL。
- [ ] 数据源不可用时，对象卡片和运行入口显示明确错误，不允许打开后才报错。
- [ ] `lowcode-models` 和 `app-center/object-designer` 的 DDL 预览调用目标数据源检查接口。

### 3.9 权限与审计

- [ ] 绑定或修改低代码运行数据源需要独立权限。
- [ ] 在线 DDL 继续使用 `ai:lowcode:deploy-ddl`，并增加数据源级 `allow_runtime_ddl` 校验。
- [ ] 租户默认业务数据源配置需要系统租户管理权限。
- [ ] 所有运行数据源切换、DDL 执行、发布到外部数据源的操作都记录操作日志。
- [ ] 日志中不得输出数据库密码、完整 JDBC URL 中的敏感参数、业务敏感字段值。

## 4. 业务规则

- 平台元数据永远保存在主数据源。
- `/ai/crud/{configKey}` 的数据源只由已发布配置决定，前端不能传入或覆盖数据源。
- 低代码应用优先使用业务对象绑定数据源，不使用租户默认业务数据源兜底，避免同一应用在不同租户下误写不同库。
- `forge-business` 手写业务模块只有显式声明时才按租户默认业务数据源切换。
- `forge-business` 手写业务模块解析不到租户默认业务数据源时，必须回退平台主数据源 `master`。
- `forge-business` 租户业务数据源路由受全局开关控制；开关关闭时所有业务模块显式声明都必须降级到 `master`。
- `forge-business` 业务侧 ORM 主路径必须使用 MyBatis-Plus Mapper/XML，数据源选择必须进入 dynamic-datasource 上下文；禁止把 `BusinessJdbcTemplateProvider` 当作复杂业务模块的主访问方式。
- `@TenantBusinessDataSource` 切面必须早于事务切面执行；已经开启主库事务的方法不得中途切换到租户业务库。
- 异步任务必须显式传播 `tenantId` 和 dynamic-datasource key；没有租户上下文的异步业务 Mapper 调用禁止默认猜测数据源。
- 数据源标记为只读时，低代码运行页只能查询和导出，不能新增、编辑、删除、导入。
- 旧系统生产库或高风险数据源默认只读、默认禁止 DDL；解除只读和开启 DDL 必须有管理员权限和二次确认。
- 外部旧系统生产库默认不允许 DDL；只有数据源配置允许且用户二次确认后才可执行。
- 旧系统表无 Forge 标准字段时，运行策略必须显式配置为 `NONE` 或自定义映射，不能默认追加字段条件。
- 旧系统表支持单字段自定义主键；复合主键第一阶段只能作为只读模型或阻断发布为可写动态 CRUD。
- 首期完整支持 MySQL、PostgreSQL、Oracle；其他数据库只能作为后续方言扩展。
- 跨数据源关系第一阶段不能用 SQL Join；发布时发现多模型跨数据源且页面需要 Join 字段，必须阻断或降级为只读 lookup。
- 不支持跨数据源事务；动态表写入成功后平台事件、消息、触发器按最终一致性处理。
- 历史未绑定运行数据源的低代码应用保持走主库，迁移时由管理员显式绑定。
- 数据源密码、API Key、Token 不得返回前端明文，不得写入运行配置 JSON 快照。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 修改 | `gen_datasource` | `usage_scope varchar(32) DEFAULT 'BOTH'` | 数据源用途范围 |
| 修改 | `gen_datasource` | `allow_runtime_write tinyint DEFAULT 1` | 是否允许低代码运行写入 |
| 修改 | `gen_datasource` | `allow_runtime_ddl tinyint DEFAULT 0` | 是否允许低代码 DDL |
| 修改 | `gen_datasource` | `readonly tinyint DEFAULT 0` | 是否只读 |
| 修改 | `gen_datasource` | `risk_level varchar(16) DEFAULT 'MEDIUM'` | 风险等级 |
| 修改 | `ai_lowcode_model` | `runtime_datasource_id bigint DEFAULT NULL` | 模型运行数据源 ID，便于查询筛选 |
| 修改 | `ai_lowcode_model` | `runtime_datasource_code varchar(64) DEFAULT NULL` | 模型运行数据源编码 |
| 修改 | `ai_lowcode_model` | `runtime_table_name varchar(128) DEFAULT NULL` | 模型运行表名 |
| 修改 | `ai_lowcode_model` | `table_mode varchar(16) DEFAULT NULL` | `CREATE` / `EXISTING` |
| 修改 | `ai_lowcode_model` | `idx_lowcode_model_runtime_ds(tenant_id, runtime_datasource_id, status)` | 按数据源筛选模型 |
| 修改 | `ai_crud_config` | `runtime_datasource_id bigint DEFAULT NULL` | 已发布运行配置实际数据源 ID |
| 修改 | `ai_crud_config` | `runtime_datasource_code varchar(64) DEFAULT NULL` | 已发布运行配置实际数据源编码 |
| 修改 | `ai_crud_config` | `runtime_datasource_snapshot json DEFAULT NULL` | 发布时数据源快照，不含密码 |
| 修改 | `ai_crud_config` | `runtime_table_name varchar(128) DEFAULT NULL` | 运行表名 |
| 修改 | `ai_crud_config` | `primary_key_field varchar(64) DEFAULT 'id'` | 主键字段名 |
| 修改 | `ai_crud_config` | `primary_key_column varchar(64) DEFAULT 'id'` | 主键列名 |
| 修改 | `ai_crud_config` | `primary_key_type varchar(32) DEFAULT 'bigint'` | 主键类型 |
| 修改 | `ai_crud_config` | `tenant_strategy json DEFAULT NULL` | 租户隔离策略 |
| 修改 | `ai_crud_config` | `audit_strategy json DEFAULT NULL` | 审计字段策略 |
| 修改 | `ai_crud_config` | `logic_delete_strategy json DEFAULT NULL` | 逻辑删除策略 |
| 修改 | `ai_crud_config` | `idx_ai_crud_runtime_ds(tenant_id, runtime_datasource_id, publish_status)` | 按运行数据源筛选配置 |
| 修改 | `ai_crud_config_version` | 同步 `ai_crud_config` 运行数据源相关字段 | 发布版本和回滚保持一致 |
| 修改 | `sys_tenant` | `default_business_datasource_id bigint DEFAULT NULL` | 租户默认业务数据源 |
| 修改 | `sys_tenant` | `default_business_datasource_code varchar(64) DEFAULT NULL` | 租户默认业务数据源编码 |
| 新增 | `sys_config` | `business.datasource.tenant-routing-enabled=false` | 租户业务数据源全局开关，关闭时 `forge-business` 统一走 `master` |
| 修改 | `sys_resource` | 新增或补齐权限资源 | 运行数据源绑定、租户业务数据源配置 |

数据迁移规则：

- 历史 `gen_datasource` 默认 `usage_scope='BOTH'`，避免升级后列表为空。
- 历史低代码模型如果 `model_schema.sourceTable.datasourceId` 存在，则回填 `ai_lowcode_model.runtime_datasource_id`。
- 历史已发布 `ai_crud_config` 如果 `model_schema.sourceTable.datasourceId` 存在，可选择迁移为运行数据源；默认不自动改变运行库，避免历史应用行为变化。
- 历史已发布 `ai_crud_config` 无运行数据源时，运行时按主数据源处理。
- 所有 Flyway 脚本必须使用 `information_schema` 防重复，内置数据 `tenant_id=1`。

## 6. 接口变更

### 6.1 数据源接口

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 修改 | `/generator/datasource/list` | GET | 返回用途、是否只读、是否允许写入、是否允许 DDL、风险等级 |
| 修改 | `/generator/datasource/enabled` | GET | 支持 `usageScope` 参数 |
| 修改 | `/generator/datasource/add` | POST | 保存数据源用途和运行控制字段 |
| 修改 | `/generator/datasource/edit` | POST | 修改后清理连接池和元数据缓存 |
| 新增 | `/generator/datasource/{id}/runtime-capability` | GET | 返回低代码运行能力摘要 |

### 6.2 低代码模型接口

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 修改 | `/ai/lowcode/model/preview-db-table` | POST | 返回运行数据源快照、主键、租户/审计/删除策略建议 |
| 修改 | `/ai/lowcode/model/import-db-table` | POST | 保存运行数据源字段 |
| 修改 | `/ai/lowcode/model` | POST/PUT | 支持空白模型选择运行数据源 |
| 修改 | `/ai/lowcode/model/{id}` | GET | 返回运行数据源和表结构就绪摘要 |
| 新增 | `/ai/lowcode/model/{id}/ddl/preview` | GET/POST | 基于模型绑定数据源预览 DDL |
| 新增 | `/ai/lowcode/model/{id}/datasource/check` | GET | 检查数据源可用性、表、主键、字段策略 |

### 6.3 应用发布接口

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 修改 | `/ai/lowcode/app/{id}/publish` | POST | 发布时写入运行数据源冗余字段；部署模式按绑定数据源执行 |
| 修改 | `/ai/lowcode/app/{id}` | GET | 返回运行数据源快照、发布数据源风险 |
| 修改 | `/ai/lowcode/app/{id}/versions` | GET | 版本返回运行数据源摘要 |
| 修改 | `/ai/lowcode/app/{id}/rollback/{versionId}` | POST | 回滚恢复版本中的运行数据源字段 |

### 6.4 动态 CRUD 接口

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 修改 | `/ai/crud/{configKey}/page` | GET | 后端按配置解析运行数据源；前端无感 |
| 修改 | `/ai/crud/{configKey}/{id}` | GET | `id` 按配置主键类型解析，不再强制 Long |
| 修改 | `/ai/crud/{configKey}` | POST | 写入绑定数据源，按审计策略填充字段 |
| 修改 | `/ai/crud/{configKey}` | PUT | 更新绑定数据源，按主键策略定位记录 |
| 修改 | `/ai/crud/{configKey}/{id}` | DELETE | 删除绑定数据源记录，按删除策略执行 |
| 修改 | `/ai/crud/{configKey}/import` | POST | 导入写入绑定数据源 |
| 修改 | `/ai/crud/{configKey}/export` | POST | 导出读取绑定数据源 |
| 新增 | `/ai/crud/{configKey}/runtime-info` | GET | 返回运行表、主键、只读、可写、导入导出能力，不返回敏感连接信息 |

### 6.5 租户接口

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 修改 | `/system/tenant/page` | GET | 返回默认业务数据源摘要 |
| 修改 | `/system/tenant/{id}` | GET | 返回默认业务数据源字段 |
| 修改 | `/system/tenant` | POST/PUT | 支持保存默认业务数据源 |
| 新增 | `/system/tenant/{id}/business-datasource/check` | GET | 检查租户默认业务数据源可用性 |
| 新增 | `/system/tenant/business-datasource/config` | GET/PUT | 查询和修改租户业务数据源全局开关 |

## 7. 影响范围

### 7.1 后端

- `forge-plugin-generator`
  - `LowcodeModelImportService`
  - `LowcodeDataModelService`
  - `LowcodeDdlService`
  - `LowcodeDdlRepository`
  - `LowcodePublishService`
  - `LowcodeRuntimeConfigBuilder`
  - `DynamicCrudController`
  - `DynamicCrudService`
  - `DynamicCrudRepository`
  - `DynamicCrudExcelService`
  - `BusinessObjectReadinessService`
  - 触发器、公式、单据编号、业务事件相关服务
- `forge-plugin-system`
  - `SysTenant`
  - `SysTenantDTO`
  - `SysTenantMapper.xml`
  - `SysTenantServiceImpl`
  - `SysTenantBusinessDataSourceResolver`
- `forge-starter-tenant`
  - `@TenantBusinessDataSource`
  - `TenantBusinessDataSourceInfo`
  - `BusinessDataSourceProperties`
  - `TenantBusinessDataSourceAspect`
  - `TenantBusinessDataSourceResolver`
  - `TenantBusinessDataSourceExecutor`
  - `TenantBusinessDataSourceTaskDecorator`
  - 线程池 `TaskDecorator` 后处理器
- `forge-business-core`
  - `business/datasource-demo` 后端验证用例
  - 删除 `BusinessJdbcTemplateProvider`、`BusinessDataSourceContextHolder`、旧 `BusinessDataSourceExecutor`

### 7.2 前端

- `forge-admin-ui/src/views/generator/datasource.vue`
- `forge-admin-ui/src/views/ai/lowcode-models.vue`
- `forge-admin-ui/src/views/ai/lowcode-apps.vue`
- `forge-admin-ui/src/views/ai/lowcode-builder.vue`
- `forge-admin-ui/src/views/app-center/components/BusinessObjectWizardDrawer.vue`
- `forge-admin-ui/src/views/app-center/object.[objectCode].vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue`
- `forge-admin-ui/src/views/system/tenant.vue`
- `forge-admin-ui/src/api/lowcode-crud.js`

### 7.3 数据库

- 新增 Flyway 脚本位于 `forge-server/db/migration/`。
- 需要兼容历史 `gen_datasource`、`ai_lowcode_model`、`ai_crud_config` 数据。

### 7.4 运行与部署

- 外部业务库连接池数量会增加，需要控制每个数据源 Hikari 池大小。
- 外部业务库不可用时，平台主功能应可正常运行，但对应低代码应用显示不可运行。
- 多数据源场景下日志和错误信息必须能定位 `configKey`、数据源编码和表名。

## 8. 风险与关注点

### 8.1 误写旧系统生产库

风险：低代码应用发布或运行时误写旧系统生产表。

控制：

- 数据源增加只读、允许写入、允许 DDL 开关。
- 发布前显示目标数据源和风险等级。
- 高风险数据源默认只读，默认禁止 DDL 和物理删除；解除只读必须有管理员权限和二次确认。
- 写入、DDL、导入必须有权限和二次确认。

### 8.2 旧系统表结构不符合 Forge 假设

风险：旧表无 `id`、`tenant_id`、`del_flag`、审计字段，导致查询或更新失败。

控制：

- 支持主键、租户、审计、逻辑删除策略。
- 发布检查提前阻断不兼容配置。
- 更新/删除条件只在策略和表字段存在时追加。

### 8.3 跨数据源事务不一致

风险：业务表写入外部库成功，但平台事件、触发器、消息记录失败。

控制：

- 第一阶段不承诺跨数据源强事务。
- 动态 CRUD 主写入和后续业务事件采用最终一致性。
- 失败事件写入平台日志，支持后续补偿。

### 8.4 表元数据缓存污染

风险：不同数据源存在同名表，当前按表名缓存会串数据。

控制：

- 缓存 key 必须包含数据源 ID 和表名。
- 数据源编辑、DDL 执行、模型发布后清理对应缓存。

### 8.5 数据权限和租户隔离弱化

风险：旧系统表无租户字段，Forge 租户切换后看到同一外部表全量数据。

控制：

- `tenantStrategy=NONE` 必须在发布检查中提示风险。
- 可配置 `CUSTOM_COLUMN` 映射旧系统租户字段。
- 高风险对象在应用中心展示“未启用租户隔离”标签。

### 8.6 数据库方言差异

风险：当前 DDL 和元数据查询主要按 MySQL 编写，但首期需要支持 MySQL、PostgreSQL、Oracle，分页、主键回填、表/字段元数据和 DDL 语法存在差异。

控制：

- 新增 `RuntimeDatabaseDialect` 适配层，按 `GenDatasource.dbType` 选择 MySQL、PostgreSQL、Oracle 实现。
- 方言层至少覆盖：当前 schema 解析、表存在检查、列元数据、主键元数据、分页 SQL、插入主键回填、受控 DDL。
- 首期只声明完整支持 MySQL、PostgreSQL、Oracle；其他数据库不进入可写运行范围。

### 8.7 权限变更风险

本变更涉及数据源绑定、DDL、租户默认业务数据源配置，属于权限敏感能力。

控制：

- 新增权限资源必须默认只授予超级管理员或开发者角色。
- 数据源配置、发布到外部库、DDL 执行必须记录操作日志。

### 8.8 `forge-business` ORM 路由失效

风险：业务侧代码按规范使用 MyBatis-Plus Mapper/XML，但数据源切换只发生在自定义 JDBC 模板或独立 ThreadLocal 中，导致 Mapper 仍访问 `master`。

控制：

- `forge-business` 主路径必须通过 dynamic-datasource 选择数据源，不再依赖低代码 `RuntimeJdbcTemplateProvider`。
- `@TenantBusinessDataSource` 切面必须在事务开启前 `push(dsKey)`，并在 finally 中 `poll()`。
- 为业务 Mapper 增加最小集成测试，验证相同 Mapper 在不同租户下访问不同物理库。

### 8.9 异步上下文丢失

风险：`@Async`、线程池、定时任务或事件监听中没有登录态和 ThreadLocal，导致业务 Mapper 回退主库或串到上一次线程残留数据源。

控制：

- 系统线程池统一配置 `TenantBusinessDataSourceTaskDecorator`。
- 后台任务必须显式传入 `tenantId`，由执行器重新解析并设置 dynamic-datasource key。
- 所有上下文设置必须 finally 清理；测试覆盖同一线程连续执行两个租户任务不串库。

## 9. 测试策略

- **测试范围**：
  - 单元测试：运行数据源解析、运行上下文构建、租户/审计/逻辑删除策略、主键解析、缓存 key。
  - `forge-business` 单元测试：全局开关、租户默认业务数据源解析、用途校验、dynamic-datasource key 生成、开关关闭回退 `master`。
  - Repository 测试：不同数据源同名表元数据隔离；无 `tenant_id` 表更新删除不追加租户条件。
  - Service 测试：发布检查按绑定数据源执行；历史无数据源应用回退主库。
  - `forge-business` 集成测试：Service 标注 `@TenantBusinessDataSource` 后，MyBatis-Plus Mapper/XML 查询命中租户默认业务库；同一 Mapper 在不同租户下命中不同库。
  - 异步测试：`@Async`/线程池任务显式传入租户后能命中租户业务库，任务结束后 dynamic-datasource 上下文清理干净。
  - 前端测试：新建对象选择数据源、发布面板展示目标数据源、租户配置保存。
  - 集成测试：至少准备主库和外部 MySQL、PostgreSQL、Oracle 测试库，验证 `/ai/crud` 按绑定数据源读写。
- **覆盖率目标**：
  - 新增数据源解析和动态 CRUD 路由核心类行覆盖率不低于 80%。
  - 策略分支覆盖 `FORGE_TENANT_ID`、`NONE`、`DEL_FLAG`、`NONE` 删除策略。
- **独立 Test Spec**：是。进入 `/test` 前需要新增 `test-spec.md`，并按 `code-copilot/rules/automated-testing-standard.md` 记录执行日志。

建议验证命令：

```bash
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test
cd forge-server && mvn -pl forge-business/forge-business-core -am test
cd forge-admin-ui && pnpm exec eslint src/views/generator/datasource.vue src/views/system/tenant.vue src/views/app-center/components/BusinessObjectWizardDrawer.vue
cd forge-admin-ui && pnpm build
```

## 10. 待澄清与确认结论

- [x] 首期支持旧系统非 `id` 主键表的可写 CRUD：支持单字段自定义主键；复合主键第一阶段不支持可写动态 CRUD。
- [x] 租户默认业务数据源未配置时，`forge-business` 模块统一回退平台主数据源 `master`。
- [x] 旧系统生产库需要增加“只读模式默认开启”的产品策略；高风险数据源默认只读、默认禁止 DDL。
- [x] 第一阶段完整支持 MySQL、PostgreSQL、Oracle；其他数据库作为后续方言扩展。
- [x] `forge-business` 多数据源必须基于 dynamic-datasource + MyBatis-Plus ORM 链路，不能以 `JdbcTemplate` 路由作为主方案。
- [x] 租户业务数据源需要全局启停开关；租户表只负责记录默认使用哪个业务数据源。
- [x] 异步任务必须显式传播租户和 dynamic-datasource 上下文，不能依赖普通 ThreadLocal 自动继承。

当前无待澄清项。

## 11. 技术决策

1. 低代码运行数据源以业务对象/模型绑定为准，不受租户默认业务数据源影响。
2. 平台元数据继续保存在主数据源，不迁移到外部业务库。
3. 发布时将运行数据源从模型协议冗余到 `ai_crud_config`，运行时只读发布配置。
4. DDL、发布检查、运行 CRUD、导入导出必须共用同一个运行数据源解析器。
5. 动态 CRUD 仓储层从“单一 `NamedParameterJdbcTemplate`”改为“运行上下文驱动的 JDBC 模板”。
6. 元数据缓存必须按数据源隔离。
7. 旧表零侵入优先，不默认追加 Forge 审计字段、租户字段或逻辑删除字段。
8. 旧系统表首期支持单字段自定义主键；复合主键不支持可写动态 CRUD。
9. 跨数据源 Join 和跨数据源事务不进入首期。
10. 租户默认业务数据源只服务显式声明的 `forge-business` 手写模块，未配置时回退主数据源 `master`。
11. 高风险或旧系统生产数据源默认只读、默认禁止 DDL。
12. 首期完整支持 MySQL、PostgreSQL、Oracle 三类数据库方言。
13. 数据源密码不进入任何运行配置快照。
14. `forge-business` 业务侧 ORM 主路径必须使用 dynamic-datasource 选择数据源，让 MyBatis-Plus Mapper/XML 透明落到租户业务库；`BusinessJdbcTemplateProvider` 不作为复杂业务模块主链路。
15. 租户业务数据源路由必须受全局开关控制，关闭时所有显式声明方法统一走 `master`。
16. `@TenantBusinessDataSource` 切面必须先于事务切面执行；异步任务必须通过 TaskDecorator 或显式执行器传播租户和数据源上下文。

## 12. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Spec | completed | `code-copilot/changes/lowcode-runtime-datasource-isolation/spec.md` | 初版需求和技术方案 |
| Clarification | completed | `code-copilot/changes/lowcode-runtime-datasource-isolation/spec.md` | 补充自定义主键、主库回退、高风险只读、MySQL/PostgreSQL/Oracle 支持范围 |
| Runtime datasource adaptation | completed | `forge-plugin-generator`、`forge-plugin-system`、`forge-business-core`、`forge-admin-ui`、`forge-server/db/migration/V1.0.75__add_lowcode_runtime_datasource_fields.sql` | 已完成元数据、DDL、动态 CRUD、租户默认业务数据源、触发器/公式外围链路和发布目标库展示；流程审批 `Long recordId` 与多库集成测试后续单独处理 |
| Business datasource correction | implemented | `forge-starter-tenant`、`forge-plugin-system`、`forge-business-core`、`forge-server/db/migration/V1.0.76__add_business_datasource_routing_config.sql`、`application.yml`、`spec.md`、`tasks.md`、`test-spec.md` | 已落地 dynamic-datasource + MyBatis-Plus 主链路、全局开关、通用注解/AOP/显式执行器/异步上下文传播、系统侧租户 resolver 和 `business/datasource-demo` 验证用例；业务侧只选择已配置 dsKey，不监听或注册低代码 `gen_datasource`；真实多库 Mapper/XML 集成测试后续补齐 |

## 13. 审查结论

待 review。

## 14. 确认记录（HARD-GATE）

- **确认时间**：2026-06-21
- **确认人**：用户本轮确认
- **确认内容**：首期支持单字段自定义主键；租户默认业务数据源未配置时回退主库；旧系统生产库/高风险数据源默认只读并禁止 DDL；第一阶段完整支持 MySQL、PostgreSQL、Oracle。
