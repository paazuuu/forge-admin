# 低代码运行时数据源隔离改造测试计划

## 本轮增量验证

变更范围：

- 元数据字段：Flyway 脚本、实体、DTO、Mapper XML。
- 数据源管理：用途过滤、默认值、高风险只读、连接池清理。
- 低代码运行时基础：数据源快照、主键/租户/审计/逻辑删除策略 DTO、运行时解析器、JDBC 模板提供器、MySQL/PostgreSQL/Oracle 方言骨架。
- 模型导入与发布：运行数据源、主键和策略回填到 `ai_lowcode_model`、`ai_crud_config`、`ai_crud_config_version`。

P0 必跑：

- `git diff --check` 覆盖本轮文档、Java、Mapper XML、Flyway 脚本。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` 覆盖 generator 及依赖模块编译。

P1 后续补充：

- DDL 仓储/服务按运行数据源路由后，补 MySQL/PostgreSQL/Oracle 元数据读取和受控 DDL 验证。
- Dynamic CRUD 按运行数据源路由后，补自定义单字段主键、只读数据源、租户/审计/逻辑删除策略单测。
- 前端适配完成后，补 `pnpm build` 和关键页面联调验证。

## 本轮增量验证：动态 CRUD、租户默认业务数据源和前端适配

变更范围：

- 动态 CRUD Controller/Service/Repository 自定义单字段主键、只读写入拦截、租户/审计/逻辑删除策略。
- `forge-business` 租户默认业务数据源解析器、上下文、JDBC 模板提供器和主库回退。
- 数据源管理、低代码模型导入、模型管理、业务对象向导和租户管理前端适配。

P0 必跑：

- `git diff --check` 覆盖本轮文档、Java、Mapper XML、Flyway 和前端文件。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`。
- `mvn -pl forge-business/forge-business-core -am compile -DskipTests`。
- targeted eslint 覆盖本轮触达的前端文件。
- `pnpm --dir forge-admin-ui build` 覆盖前端打包链路。

P1 后续补充：

- 为运行数据源解析、方言分页、元数据缓存、自定义主键和租户默认业务数据源回退补单元测试。
- 准备主库 + MySQL/PostgreSQL/Oracle 外部库的动态 CRUD 集成验证数据。
- 发布目标库展示、就绪度检查、导入导出/触发器/单据编号全链路完成后补接口级验证。

## 本轮增量验证：发布检查、触发器事件和公式聚合自定义主键补齐

变更范围：

- 业务事件发布、定时触发扫描、触发器创建关联记录和更新字段动作，按动态 CRUD 运行主键解析记录 ID，不再固定读取 `id` 或转换为 `Long`。
- 聚合公式刷新链路进入父对象运行数据源上下文，刷新入参支持任意单字段主键，主表回写不再固定读取 `id`。
- 业务对象发布检查新增运行数据源维度，展示目标库、运行表、主键、只读、写入、DDL 和高风险提示。
- 发布检查前端摘要区新增“发布目标库”面板，直接展示后端 `DATASOURCE_*` 检查项和风险标签。

P0 必跑：

- `git diff --check` 覆盖本轮 Java、Vue 和当前变更文档。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`。
- targeted eslint 覆盖 `BusinessPublishChecklist.vue`。
- `pnpm --dir forge-admin-ui build` 覆盖发布检查前端打包链路。

P1 后续补充：

- 准备带字符串主键的低代码对象，验证新增/编辑/删除、触发器创建关联记录、定时触发去重和聚合公式回写。
- 准备 MySQL/PostgreSQL/Oracle 外部库，执行发布检查接口，确认目标库、表、主键和 DDL 风险提示符合真实库状态。
- 流程审批实例链路仍使用 `Long recordId`，后续需要单独设计 `ai_business_flow_instance_link.record_id` 字段和接口协议迁移。

## 本轮增量验证：字段绑定列名格式兼容

变更范围：

- 表单设计器字段编码变更时，新建 Forge 托管字段的数据库列名默认由 camelCase 转为 snake_case，例如 `userNick` 生成 `user_nick`。
- 低代码 Schema 校验和受控 DDL 安全标识符校验允许旧系统安全列名使用 camelCase 或下划线前缀，避免导入既有表时误报 `数据库列名格式不正确: userNick`。

P0 必跑：

- `git diff --check` 覆盖 `LowcodeSchemaValidator.java`、`LowcodeDdlService.java`、`ForgePropertyPanel.vue` 和当前变更文档。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` 覆盖后端 Schema/DDL 校验编译。
- targeted eslint 覆盖 `ForgePropertyPanel.vue`。
- `pnpm --dir forge-admin-ui build` 覆盖前端打包链路。

P1 后续补充：

- 增加回归用例：新建字段 `field=userNick` 时默认保存 `columnName=user_nick`。
- 增加回归用例：绑定旧系统已有表且真实列名为 `userNick` 时，Schema 校验允许通过。

## 本轮增量验证：新建业务对象运行数据源选择

变更范围：

- 业务对象新建向导第二步常驻展示“运行数据源”，空白创建、数据库导入和 AI 生成三种模式都必须明确运行目标库。
- 运行数据源列表按 `LOWCODE_RUNTIME` 用途过滤；数据库导入模式复用该运行数据源加载数据表，不再使用租户业务模块数据源口径。
- 新建对象保存时写入 `runtimeDatasourceId` 和对象 `options.runtimeDatasource`；设计器默认模型从对象 options 解析运行数据源，保存草稿时同步写入 `ai_lowcode_model` 和 `ai_crud_config` 的运行数据源冗余字段。

P0 必跑：

- `git diff --check` 覆盖 `BusinessObjectWizardDrawer.vue`、`BusinessObjectDTO.java`、`BusinessObjectDesignerService.java` 和当前变更文档。
- targeted eslint 覆盖 `BusinessObjectWizardDrawer.vue`。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`。
- `pnpm --dir forge-admin-ui build` 覆盖创建向导前端打包链路。

P1 后续补充：

- 启动前后端后验证新建空白对象时能看到“平台主库（默认）”和 `LOWCODE_RUNTIME/BOTH` 数据源。
- 验证数据库导入模式选择某个 `LOWCODE_RUNTIME` 数据源后，只加载该数据源表，并在保存草稿后模型/运行配置保留同一个运行数据源快照。
