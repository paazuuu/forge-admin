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
- [x] 新增数据库方言层：MySQL、PostgreSQL、Oracle 的 schema 解析、表/列/主键元数据、分页、主键回填、受控 DDL。

## 阶段三：模型、发布与 DDL 链路

- [x] 改造 `LowcodeDdlRepository/LowcodeDdlService`，DDL 预览、执行和发布校验按绑定数据源运行。
- [x] 改造 `LowcodeDataModelService/LowcodeModelImportService`，模型保存和导入时回填运行数据源字段。
- [x] 改造 `LowcodePublishService`，发布时冗余运行数据源、主键和策略到 `ai_crud_config` 与版本表。

## 阶段四：动态 CRUD 运行态

- [x] 改造 `DynamicCrudRepository`，所有查询、写入、元数据缓存按运行上下文和数据源隔离。
- [x] 改造 `DynamicCrudService/Controller`，支持自定义单字段主键、只读数据源、租户/审计/逻辑删除策略。
- [x] 改造导入导出、异步导出、触发器、公式和单据编号链路，复用动态 CRUD 运行上下文。（导入导出经 `DynamicCrudService` 运行上下文；触发器事件/定时扫描/关联记录动作按运行主键解析；聚合公式刷新支持自定义单字段主键；单据编号在运行上下文内随插入链路执行。流程审批实例 `Long recordId` 属于后续表结构协议改造。）

## 阶段五：租户默认业务数据源

- [x] 新增租户默认业务数据源解析器，`forge-business` 显式业务数据源上下文未配置时回退主库。
- [x] 扩展租户管理接口和前端，支持查看和配置默认业务数据源。

## 阶段六：前端适配

- [x] 扩展数据源管理前端，展示用途、只读、允许写入、允许 DDL、风险等级。
- [x] 扩展应用中心/模型设计器前端，按低代码运行和租户业务用途过滤可选数据源。
- [x] 扩展发布目标库展示和就绪度检查，明确显示目标数据库、只读/DDL/写入风险。

## 阶段七：验证与验收

- [ ] 编写后端单元测试：运行数据源解析、方言、元数据缓存、自定义主键、租户默认业务数据源回退。
- [ ] 编写集成测试方案：主库 + MySQL/PostgreSQL/Oracle 外部库的动态 CRUD 读写验证。
- [x] 执行后端增量编译、前端 lint/build，并按自动化测试标准更新 `execution-log.md`。
