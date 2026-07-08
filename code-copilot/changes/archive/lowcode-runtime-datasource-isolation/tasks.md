# 低代码运行时数据源隔离改造任务

## 阶段一：元数据基础与兼容字段

- [x] 形成完整 spec，确认自定义主键、主库回退、高风险只读和 MySQL/PostgreSQL/Oracle 支持范围。
- [x] 新增 Flyway 迁移脚本，扩展 `gen_datasource`、`ai_lowcode_model`、`ai_crud_config`、`ai_crud_config_version`、`sys_tenant` 的运行数据源字段。
- [x] 扩展后端实体和 DTO：`GenDatasource`、`AiLowcodeModel`、`AiCrudConfig`、`AiCrudConfigVersion`、`SysTenant`、`SysTenantDTO`。
- [x] 补齐 MyBatis XML 显式字段映射：`AiLowcodeModelMapper.xml`、`AiCrudConfigMapper.xml`、`AiCrudConfigVersionMapper.xml`、`SysTenantMapper.xml`。
- [x] 扩展数据源管理接口：支持 `usageScope` 过滤、运行能力字段默认值、高风险默认只读、编辑/删除后清理连接池。

## 阶段二：低代码运行时协议

- [x] 新增低代码运行时数据源协议 DTO：运行数据源快照、主键策略、租户策略、审计策略、逻辑删除策略。
- [x] 新增运行时数据源解析器：从 `LowcodeModelSchema` 和 `AiCrudConfig` 解析运行数据源，历史配置回退主库。
- [x] 新增运行时 JDBC 模板提供器：按 `datasourceId` 获取 `JdbcTemplate` / `NamedParameterJdbcTemplate`，兼容主库。
- [x] 新增数据库方言层：MySQL、PostgreSQL、Oracle 的 schema 解析、表/列/主键元数据、分页、主键回填、受控 DDL；Oracle 已补默认测试 SQL、导入元数据 SQL 和受控 DDL 语义。

## 阶段三：模型、发布与 DDL 链路

- [x] 改造 `LowcodeDdlRepository/LowcodeDdlService`，DDL 预览、执行和发布校验按绑定数据源运行。
- [x] 改造 `LowcodeDataModelService/LowcodeModelImportService`，模型保存和导入时回填运行数据源字段。
- [x] 改造 `LowcodePublishService`，发布时冗余运行数据源、主键和策略到 `ai_crud_config` 与版本表。

## 阶段四：动态 CRUD 运行态

- [x] 改造 `DynamicCrudRepository`，所有查询、写入、元数据缓存按运行上下文和数据源隔离。
- [x] 改造 `DynamicCrudService/Controller`，支持自定义单字段主键、只读数据源、租户/审计/逻辑删除策略。
- [x] 改造导入导出、异步导出、触发器、公式和单据编号链路，复用动态 CRUD 运行上下文。（导入导出经 `DynamicCrudService` 运行上下文；触发器事件/定时扫描/关联记录动作按运行主键解析；聚合公式刷新支持自定义单字段主键；单据编号在运行上下文内随插入链路执行。流程审批实例 `Long recordId` 属于后续表结构协议改造。）

## 阶段五：租户默认业务数据源

- [x] 扩展租户管理接口和前端，支持查看和配置默认业务数据源。
- [x] `sys_tenant` 已具备默认业务数据源字段；该字段只负责记录租户选择哪个数据源。
- [x] 废弃当前 `forge-business` JDBC 模板主路径方案：`BusinessJdbcTemplateProvider` / 独立 `BusinessDataSourceContextHolder` 不能作为复杂业务模块多数据源主链路。
- [x] 新增租户业务数据源全局开关：`forge.business.datasource.enabled` + `business.datasource.tenant-routing-enabled`，关闭时所有显式声明业务方法统一走 `master`。
- [x] 接入 baomidou dynamic-datasource：租户表 `default_business_datasource_code` 直接保存已配置 dynamic-datasource dsKey，业务侧只负责校验并切换该 dsKey。
- [x] 新增 `@TenantBusinessDataSource` 注解和 AOP，按当前租户解析 dsKey，进入方法前 `DynamicDataSourceContextHolder.push(dsKey)`，finally 中 `poll()`。
- [x] 确保切面顺序早于 `@Transactional`；禁止在已开启主库事务的方法中途切换到租户业务库。
- [x] 新增系统侧 `SysTenantBusinessDataSourceResolver`：返回业务 dsKey/数据源摘要/开关状态，不再返回低代码 `LowcodeRuntimeDataSourceContext`。
- [x] 新增通用 `TenantBusinessDataSourceExecutor`：支持显式传入 `tenantId`，用于定时任务、消息消费、补偿任务等无登录态场景。
- [x] 新增通用 `TenantBusinessDataSourceTaskDecorator`，并在 `forge.business.datasource.enabled=true` 时接入系统线程池，覆盖 `@Async` 和线程池异步任务的租户与 dynamic-datasource 上下文传播。
- [x] 将通用注解、执行器、解析接口、AOP 和异步上下文能力下沉到 `forge-starter-tenant`；租户配置解析落到 `forge-plugin-system`；`forge-business-core` 仅保留测试 demo。
- [x] 移除业务侧对低代码 `gen_datasource` 的监听和动态注册逻辑；业务租户数据源和低代码数据源不是同一套连接生命周期。
- [x] 新增 `business/datasource-demo` 后端用例，使用 MyBatis Mapper/XML 在租户业务库自动建演示表、插入记录并返回当前数据库和 dsKey，便于手工验证路由。
- [x] 新增前端“业务数据源调试”页面和菜单资源，支持选择租户、检测路由、写入测试记录、查看当前数据库和最近记录。
- [x] 改造数据权限控制面元数据加载：`sys_data_scope_config`、角色、组织、行政区划从平台主库预加载为内存快照，业务库查询期间不再访问 `sys_*` 平台表。
- [ ] 补充 MyBatis-Plus Mapper/XML 路由测试：同一 Mapper 在不同租户下命中不同业务库；全局开关关闭时回退 `master`；异步连续执行两个租户任务不串库。

## 阶段六：前端适配

- [x] 扩展数据源管理前端，展示用途、只读、允许写入、允许 DDL、风险等级。
- [x] 扩展应用中心/模型设计器前端，按低代码运行和租户业务用途过滤可选数据源。
- [x] 扩展发布目标库展示和就绪度检查，明确显示目标数据库、只读/DDL/写入风险。
- [x] 新增业务数据源调试台，降低手工验证 `TenantBusinessDataSourceExecutor` / `@TenantBusinessDataSource` 路由成本。

## 阶段七：验证与验收

- [ ] 编写后端单元测试：运行数据源解析、方言、元数据缓存、自定义主键、租户默认业务数据源回退、全局开关、dynamic-datasource dsKey 解析。（已补 `TenantBusinessDataSourceTaskDecoratorTest`，但当前父 POM 默认跳过 testCompile/surefire，需后续统一调整测试插件配置后执行。）
- [ ] 编写 `forge-business` ORM 路由集成测试：`@TenantBusinessDataSource` + MyBatis-Plus Mapper/XML 命中租户业务库，异步任务上下文不丢失且不串库。
- [ ] 编写集成测试方案：主库 + MySQL/PostgreSQL/Oracle 外部库的动态 CRUD 读写验证；Oracle 需覆盖连接测试、表导入、建表/加列/索引/注释 DDL、分页查询和自增主键回填。
- [x] 执行后端增量编译、前端 lint/build，并按自动化测试标准更新 `execution-log.md`。
