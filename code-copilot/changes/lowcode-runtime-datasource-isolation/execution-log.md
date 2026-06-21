# 低代码运行时数据源隔离改造执行日志

## 2026-06-21 阶段一/二/三部分验证

变更范围：

- 拆分并更新 `tasks.md`。
- 新增运行数据源相关 Flyway 字段、实体/DTO 字段、Mapper XML 映射。
- 数据源管理接口支持 `usageScope` 过滤，保存/更新默认运行控制字段，更新/删除后清理动态连接池。
- 新增低代码运行时数据源协议、解析器、JDBC 模板提供器和 MySQL/PostgreSQL/Oracle 方言骨架。
- 模型导入和模型保存回填运行数据源、主键、租户/审计/逻辑删除策略。
- 发布和版本表冗余运行数据源、主键和策略字段。

验证命令与结果：

- `git diff --check -- code-copilot/changes/lowcode-runtime-datasource-isolation forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper forge-server/db/migration/V1.0.75__add_lowcode_runtime_datasource_fields.sql`
  - 结果：通过，无空白错误。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 结果：失败，原因是当前 shell Java 版本不支持 target 17，报 `无效的目标发行版: 17`。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 结果：通过，`forge-plugin-system`、`forge-plugin-generator` 及依赖模块编译成功。
- 第二次同 Java 17 命令复跑：
  - 结果：通过，导入/发布服务新增注入和字段回填代码编译成功。

警告：

- Maven 编译存在既有 deprecation / unchecked warning，未阻断。

跳过项：

- 未启动后端服务，原因是本轮未完成 DDL 和动态 CRUD 运行态路由，接口级验证留到路由改造后执行。
- 未执行前端构建，原因是本轮没有改前端。

服务清理：

- 本轮未启动任何长期运行服务，无需清理 PID。

## 2026-06-21 动态 CRUD、租户默认业务数据源与前端适配验证

变更范围：

- 动态 CRUD 支持运行时自定义单字段主键：Controller 路径 ID 改为字符串接收，Service/Repository 使用运行上下文中的主键列执行详情、更新、删除和插入主键回填。
- 动态 CRUD Repository 查询、分页、别名引用、租户条件、审计字段、逻辑删除和表元数据缓存按运行数据源上下文隔离。
- 写入路径校验数据源只读、禁止写入和非自增主键必填约束。
- 聚合公式刷新链路补运行数据源上下文；非 Long 主键刷新仍按后续任务处理。
- `forge-business` 新增租户默认业务数据源解析器、上下文、JDBC 模板提供器和执行器；显式上下文优先，租户默认配置次之，未配置时回退主库。
- 数据源管理前端展示用途、只读、写入、DDL 和风险等级，高风险新数据源默认只读、禁止写入、禁止 DDL。
- 低代码模型导入/模型管理按 `LOWCODE_RUNTIME` 过滤数据源，业务对象向导按 `TENANT_BUSINESS` 过滤数据源。
- 租户管理前端支持查看和配置默认业务数据源；未配置时展示并提交为主库回退。

验证命令与结果：

- `git diff --check -- code-copilot/changes/lowcode-runtime-datasource-isolation forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator forge-server/forge-framework/forge-plugin-parent/forge-plugin-system forge-server/forge-business/forge-business-core forge-server/db/migration/V1.0.75__add_lowcode_runtime_datasource_fields.sql forge-admin-ui/src/views/generator/datasource.vue forge-admin-ui/src/views/system/tenant.vue forge-admin-ui/src/api/lowcode-crud.js forge-admin-ui/src/api/business-app.js forge-admin-ui/src/components/lowcode-builder/model/LowcodeModelImportModal.vue forge-admin-ui/src/views/ai/lowcode-models.vue forge-admin-ui/src/views/app-center/components/BusinessObjectWizardDrawer.vue`
  - 结果：通过，无空白错误。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 结果：通过，`forge-plugin-generator` 及依赖模块编译成功。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core -am compile -DskipTests`
  - 结果：通过，`forge-business-core` 及依赖模块编译成功。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/system/tenant.vue src/views/generator/datasource.vue src/api/lowcode-crud.js src/api/business-app.js src/components/lowcode-builder/model/LowcodeModelImportModal.vue src/views/ai/lowcode-models.vue src/views/app-center/components/BusinessObjectWizardDrawer.vue --fix`
  - 结果：通过，本轮触达的前端文件 lint 通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，Vite build 成功，耗时约 51 秒。

警告：

- Maven 编译存在既有 deprecation / unchecked warning，未阻断。
- 前端 build 存在既有 CSS `//` 注释 minify warning、`UserSelectModal` 组件命名冲突提示，以及 `src/store/index.js` 同时动态/静态导入导致的 chunk 提示，未阻断。

跳过项：

- 未启动后端服务和未执行 curl 接口验证，原因是本轮没有可用的本地外部业务库连接信息，且真实多数据源联调需要准备 MySQL/PostgreSQL/Oracle 验证库。
- 未执行全量 `pnpm lint:fix`，原因是该命令会扫描并可能自动修复大量非本变更文件；本轮改为对触达文件执行 targeted eslint。
- 未补后端单元测试和多库集成测试，已在 `tasks.md` 保留为后续验收项。

服务清理：

- 本轮未启动任何长期运行服务，无需清理 PID。

## 2026-06-21 DDL 与动态 CRUD 运行时路由验证

变更范围：

- `LowcodeDdlRepository` 改为通过运行时上下文选择 `JdbcTemplate` 和数据库方言，表/列/索引/主键元数据查询不再固定主库。
- `LowcodeDdlService` 预览、执行、发布校验按模型绑定运行数据源执行；已有表主键要求调整为单字段主键，不再强制 `id` 自增。
- 业务对象发布检查和字段设计列状态检查改为基于模型运行数据源。
- 新增 `LowcodeRuntimeDataSourceContextHolder`，`DynamicCrudRepository` SQL 执行点按当前运行上下文选择 `NamedParameterJdbcTemplate`。
- 动态 CRUD 表结构缓存改为 `datasourceId + tableName` 维度，避免同名表跨库串缓存。
- 动态 CRUD 主路径入口绑定运行数据源上下文，并对只读/禁止写入数据源拦截新增、更新、删除。

验证命令与结果：

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 结果：通过，`forge-plugin-generator` 及依赖模块编译成功。
- `git diff --check -- code-copilot/changes/lowcode-runtime-datasource-isolation forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper forge-server/db/migration/V1.0.75__add_lowcode_runtime_datasource_fields.sql`
  - 结果：通过，无空白错误。

警告：

- Maven 编译仍存在既有 deprecation / unchecked warning，未阻断。

跳过项：

- 未启动后端服务，原因是本轮尚未完成自定义主键端到端、租户默认业务数据源和前端适配。
- 未执行前端构建，原因是本轮未改前端。

服务清理：

- 本轮未启动任何长期运行服务，无需清理 PID。

## 2026-06-21 发布检查、触发器事件与公式聚合补全验证

变更范围：

- `BusinessObjectPublishService` 新增运行数据源发布检查项：目标库、运行表、运行主键、只读、写入、DDL 和高风险提示；表结构检查结合数据源 `allowDdl` 和 DDL preview `executable` 决定阻断/警告。
- `BusinessEventPublisher`、`BusinessTriggerSchedulerService`、`BusinessTriggerExecutor` 按动态 CRUD 发布配置解析运行主键，触发器创建/更新关联记录不再强制 `id` 或 `Long`。
- `StoredAggregateRefreshService` 和 `DynamicCrudService` 的聚合公式刷新链路支持任意单字段主键，并在父对象运行数据源上下文内查询和回写。
- `BusinessPublishChecklist.vue` 新增发布目标库摘要面板，直接展示后端 `DATASOURCE_*` 就绪度检查和风险标签。

验证命令与结果：

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessPublishChecklist.vue --fix`
  - 结果：通过，发布检查组件 targeted eslint 无错误。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 结果：通过，`forge-plugin-generator` 及依赖模块编译成功。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，Vite build 成功，耗时约 54 秒。
- `git diff --check -- code-copilot/changes/lowcode-runtime-datasource-isolation forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectPublishService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessEventPublisher.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerExecutor.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerSchedulerService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/StoredAggregateRefreshService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java forge-admin-ui/src/views/app-center/components/designer/BusinessPublishChecklist.vue`
  - 结果：通过，无空白错误。

警告：

- Maven 编译仍存在既有 deprecation / unchecked warning，未阻断。
- 前端 build 仍存在既有 CSS `//` 注释 minify warning、`UserSelectModal` 组件命名冲突提示，以及 `src/store/index.js` 同时动态/静态导入导致的 chunk 提示，未阻断。

跳过项：

- 未启动后端服务和未执行 curl 接口验证，原因是本地没有准备 MySQL/PostgreSQL/Oracle 外部业务库连接和带自定义主键的测试对象。
- 流程审批实例链路仍使用 `Long recordId`，涉及 `ai_business_flow_instance_link` 表字段和接口协议迁移，已作为后续改造项记录，不在本轮小补丁中扩大修改面。

服务清理：

- 本轮未启动任何长期运行服务，无需清理 PID。

## 2026-06-21 字段绑定列名 `userNick` 兼容修复验证

执行时间：2026-06-21 21:21:07 CST

变更范围：

- 修复表单设计器字段编码同步逻辑：字段编码仍使用 `userNick` 这类 lowerCamel，默认数据库列名改为 `camelToSnake(fieldCode)`，新建 Forge 托管字段生成 `user_nick`。
- 放宽低代码 Schema 和受控 DDL 的安全列名校验，允许旧系统已有表使用 `userNick` 这类安全 SQL 标识符，避免误报 `数据库列名格式不正确: userNick`。
- 补充当前变更测试计划和踩坑记录，明确字段编码与物理列名的边界。

验证命令与结果：

- `git diff --check -- code-copilot/changes/lowcode-runtime-datasource-isolation/test-spec.md code-copilot/changes/lowcode-runtime-datasource-isolation/execution-log.md code-copilot/memory/pitfalls.md forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeSchemaValidator.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDdlService.java forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
  - 结果：通过，无空白错误。
- `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 结果：通过，`forge-plugin-generator` 及依赖模块编译成功。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue --fix`
  - 结果：通过，`ForgePropertyPanel.vue` targeted eslint 无错误。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，Vite build 成功，耗时约 1 分 1 秒。

警告：

- Maven 编译仍存在既有 deprecation / unchecked warning，未阻断。
- 前端 build 仍存在既有 CSS `//` 注释 minify warning、`UserSelectModal` 组件命名冲突提示，以及 `src/store/index.js` 同时动态/静态导入导致的 chunk 提示，未阻断。

跳过项：

- 未启动后端服务和未执行 curl 接口验证，原因是本轮只修复静态 Schema/DDL 校验和设计器默认列名生成；旧系统真实库 `userNick` 字段的端到端发布验证需要准备外部业务库连接。
- 未新增后端单元测试，已在 `test-spec.md` 记录为 P1 回归用例：新字段默认 `user_nick`，旧表真实列名 `userNick` 可通过 Schema 校验。

服务清理：

- 本轮未启动任何长期运行服务，无需清理 PID。

## 2026-06-21 新建业务对象运行数据源选择补齐验证

执行时间：2026-06-21 21:33:02 CST

变更范围：

- `BusinessObjectWizardDrawer.vue` 在创建方式步骤常驻展示“运行数据源”，空白创建、数据库导入、AI 生成三种模式都能选择低代码运行目标库。
- 数据源列表改为按 `LOWCODE_RUNTIME` 过滤；数据库导入模式复用运行数据源加载数据表，不再使用租户业务模块 `TENANT_BUSINESS` 口径。
- 创建对象请求新增 `runtimeDatasourceId`，并在对象 options 中保存 `runtimeDatasourceId/runtimeDatasource` 快照。
- `BusinessObjectDesignerService` 从业务对象 options 回填默认模型 `runtimeDatasource`，保存草稿时同步写入 `ai_lowcode_model` 与 `ai_crud_config` 的运行数据源字段。

验证命令与结果：

- `git diff --check -- code-copilot/changes/lowcode-runtime-datasource-isolation/test-spec.md code-copilot/changes/lowcode-runtime-datasource-isolation/execution-log.md code-copilot/memory/pitfalls.md forge-admin-ui/src/views/app-center/components/BusinessObjectWizardDrawer.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessObjectDTO.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectDesignerService.java`
  - 结果：通过，无空白错误。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/BusinessObjectWizardDrawer.vue --fix`
  - 结果：通过，创建向导组件 targeted eslint 无错误。
- `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 结果：通过，`forge-plugin-generator` 及依赖模块编译成功。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，Vite build 成功，耗时约 55.88 秒。

警告：

- Maven 编译仍存在既有 deprecation / unchecked warning，未阻断。
- 前端 build 仍存在既有 CSS `//` 注释 minify warning、`UserSelectModal` 组件命名冲突提示，以及 `src/store/index.js` 同时动态/静态导入导致的 chunk 提示，未阻断。

跳过项：

- 未启动前后端服务做浏览器实操验证，原因是本轮已完成构建级验证；真实页面验证需要登录环境和可用 `LOWCODE_RUNTIME` 数据源配置。
- 未执行接口 curl 验证，原因是创建对象接口需要登录态、低代码业务域和数据源种子数据；后续联调时按 test-spec 的 P1 场景补充。

服务清理：

- 本轮未启动任何长期运行服务，无需清理 PID。
