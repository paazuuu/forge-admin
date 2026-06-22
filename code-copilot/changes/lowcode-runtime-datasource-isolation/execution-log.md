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

## 2026-06-22 数据权限控制面元数据主库快照验证

执行时间：2026-06-22 16:38:50 CST

变更范围：

- `forge-starter-datascope` 将 `sys_data_scope_config`、角色数据范围、自定义组织、组织层级、行政区划父子关系统一作为平台控制面元数据，从 `forge.datascope.metadata-datasource`（默认 `master`）预加载为内存快照。
- 业务 Mapper 查询期间只读取内存快照，不再在租户业务数据源中访问 `sys_data_scope_config`、`sys_role`、`sys_org`、`sys_region_code` 等平台表。
- REGION 数据权限不再生成 `IN (SELECT code FROM sys_region_code ...)`，改为从主库快照解析出业务库可执行的字符串等值或 `IN (...)` 条件；自定义 SQL 新增 `#{regionCodes}` 占位符。
- 新增 `forge.datascope.metadata-datasource` 和 `forge.datascope.default-config-tenant-id` 配置项。
- 为长期维护性，将本轮新增的数据权限快照查询下沉到 Mapper XML，服务层不再使用 `LambdaQueryWrapper` 构建这批查询。

验证命令与结果：

- `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin mvn -pl forge-framework/forge-starter-parent/forge-starter-datascope -am compile -DskipTests`
  - 结果：通过，`forge-starter-datascope` 及依赖模块编译成功，新增 Mapper XML 已复制到 `target/classes`。
- `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin mvn -pl forge-admin-server -am compile -DskipTests`
  - 结果：通过，admin 入口聚合 `forge-starter-datascope`、`forge-starter-tenant`、dynamic-datasource 和 `forge-business-core` 编译成功。
- `git diff --check -- forge-server/forge-framework/forge-starter-parent/forge-starter-datascope code-copilot/changes/lowcode-runtime-datasource-isolation code-copilot/memory/decisions.md code-copilot/memory/pitfalls.md`
  - 结果：通过，无空白错误。
- `git diff --check --no-index /dev/null <新增 Mapper XML 或 Java 文件>`
  - 结果：新增 `DataScope*Mapper.xml`、`Sys*Mapper.xml` 和 `DataScopeRegionMapper.java` 检查无输出；返回码 1 为 no-index 新文件差异的预期结果。

警告：

- Maven 编译仍存在既有 deprecation / unchecked warning，集中在 `DataScopeInterceptor`、generator 和 admin 既有类，未阻断本轮验证。

跳过项：

- 未启动后端服务和未执行 curl/API 验证，原因是本地未准备可用的租户业务数据源和数据权限业务 Mapper 验证数据。
- 未执行真实多库集成测试；后续需准备 `master` + 租户业务库，调用带数据权限配置的业务 Mapper，确认业务 SQL 命中业务库且不再出现 `Table '<业务库>.sys_data_scope_config' doesn't exist`。

服务清理：

- 本轮未启动任何长期运行服务，无需清理 PID。

## 2026-06-22 业务数据源调试前端页面验证

执行时间：2026-06-22 15:31 CST

变更范围：

- 新增 `forge-admin-ui/src/views/business/datasource-debug.vue`，提供租户选择、路由检测、写入测试记录、读取最近记录和 dsKey/当前数据库展示。
- 新增 `forge-server/db/migration/V1.0.77__add_business_datasource_debug_menu.sql`，注册“业务数据源调试”菜单和 `/business/datasource-demo/*` 调试接口 API 资源，并给 admin / 数据源管理相关角色授权。
- 更新 `tasks.md` 和 `test-spec.md`，把调试台作为租户业务数据源路由改造的前端验证入口。

验证命令与结果：

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/business/datasource-debug.vue --fix`
  - 结果：通过，`datasource-debug.vue` targeted eslint 无错误。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，Vite build 成功，产物中包含 `dist/assets/datasource-debug-*.js`。
- `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin mvn -pl forge-admin-server -am compile -DskipTests`
  - 结果：通过，`forge-admin-server` 及依赖模块编译成功。
- `git diff --check -- code-copilot/changes/lowcode-runtime-datasource-isolation forge-admin-ui/src/views/business/datasource-debug.vue forge-server/db/migration/V1.0.77__add_business_datasource_debug_menu.sql forge-server/forge-framework/forge-starter-parent/forge-starter-tenant forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/datasource forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/demo forge-server/forge-business/forge-business-core/src/main/resources/mapper/business/BusinessDatasourceDemoMapper.xml`
  - 结果：通过，已跟踪文件无空白错误。
- `git diff --check --no-index /dev/null forge-admin-ui/src/views/business/datasource-debug.vue`
  - 结果：无输出，新增 Vue 文件无空白错误；`--no-index` 对新增文件返回码 1 属于正常差异返回。
- `git diff --check --no-index /dev/null forge-server/db/migration/V1.0.77__add_business_datasource_debug_menu.sql`
  - 结果：无输出，新增 Flyway 脚本无空白错误；`--no-index` 对新增文件返回码 1 属于正常差异返回。
- `ulimit -n 65535 && source ~/.nvm/nvm.sh && nvm use v20.19.0 && CHOKIDAR_USEPOLLING=true pnpm --dir forge-admin-ui exec vite --host 127.0.0.1 --port 5173`
  - 结果：通过，Vite dev server 启动成功，访问地址 `http://127.0.0.1:5173/`。
- `curl -I http://127.0.0.1:5173`
  - 结果：通过，返回 `HTTP/1.1 200 OK`。

警告：

- Maven 编译仍存在既有 deprecation / unchecked warning，未阻断。
- 前端 build 仍存在既有 CSS `//` 注释 minify warning、`UserSelectModal` 组件命名冲突提示，以及 `src/store/index.js` 同时动态/静态导入导致的 chunk 提示，未阻断。
- 首次直接启动 Vite 触发本机 watcher 限制 `EMFILE: too many open files`，已通过提高 `ulimit` 并开启 `CHOKIDAR_USEPOLLING=true` 解决。

跳过项：

- 未启动后端服务做接口实操验证，原因是真实路由验证需要 Flyway 执行菜单脚本、登录 admin，并配置至少一个租户的 `default_business_datasource_code` 指向已注册 dynamic-datasource dsKey。
- 未执行业务接口 curl 验证，原因同上；后续联调可直接在页面选择租户后点“检测路由 / 写入测试记录 / 读取记录”完成手工验证。

服务清理：

- 本轮启动了前端 Vite dev server，并保留运行用于页面验证；工具会话 ID：`26129`，访问地址：`http://127.0.0.1:5173/`。

## 2026-06-22 异步日志租户上下文修复验证

执行时间：2026-06-22 15:09 CST

变更范围：

- `SystemLogServiceImpl` 补全操作日志/登录日志用户信息时，通过 `TenantContextHolder.executeIgnore` 跨租户查询 `SysUser`，避免异步线程无租户上下文时被租户条件过滤。
- `SystemLogServiceImpl` 对 `SysUser` 查询结果增加 null 保护，查不到用户时不再调用 `getUsername()`。
- `TenantBusinessDataSourceTaskDecorator` 捕获、传播并恢复 `TenantContextHolder.isIgnore()` 状态，避免异步任务清掉系统级忽略租户标记。
- `TenantBusinessDataSourceTaskDecoratorTest` 增加忽略租户标记传播/恢复断言。
- `test-spec.md` 和 `pitfalls.md` 记录本轮回归范围与踩坑。

验证命令与结果：

- `git diff --check -- forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SystemLogServiceImpl.java forge-server/forge-framework/forge-starter-parent/forge-starter-tenant/src/main/java/com/mdframe/forge/starter/tenant/datasource/TenantBusinessDataSourceTaskDecorator.java forge-server/forge-business/forge-business-core/src/test/java/com/mdframe/forge/business/core/datasource/TenantBusinessDataSourceTaskDecoratorTest.java code-copilot/changes/lowcode-runtime-datasource-isolation/test-spec.md code-copilot/memory/pitfalls.md`
  - 结果：通过，无空白错误。
- `git diff --check --no-index /dev/null forge-server/forge-framework/forge-starter-parent/forge-starter-tenant/src/main/java/com/mdframe/forge/starter/tenant/datasource/TenantBusinessDataSourceTaskDecorator.java`
  - 结果：输出为空，无空白错误；退出码 1 为 `--no-index` 对比 `/dev/null` 的正常差异退出码。
- `git diff --check --no-index /dev/null forge-server/forge-business/forge-business-core/src/test/java/com/mdframe/forge/business/core/datasource/TenantBusinessDataSourceTaskDecoratorTest.java`
  - 结果：输出为空，无空白错误；退出码 1 为 `--no-index` 对比 `/dev/null` 的正常差异退出码。
- `cd forge-server && env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin mvn -pl forge-admin-server -am compile -DskipTests`
  - 结果：通过，`forge-admin-server` 依赖链编译成功。
- `cd forge-server && env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system,forge-framework/forge-starter-parent/forge-starter-tenant,forge-business/forge-business-core -am clean compile -DskipTests`
  - 结果：通过，`forge-starter-tenant`、`forge-plugin-system`、`forge-business-core` 均重新 javac 编译成功。
- `cd forge-server && env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin mvn -pl forge-business/forge-business-core -am test -Dtest=TenantBusinessDataSourceTaskDecoratorTest -DskipTests=false -Dmaven.test.skip=false -Dmaven.compiler.skip=false`
  - 结果：构建成功，但父 POM 仍输出 `Not compiling test sources` 和 `Tests are skipped`，本轮单测未实际执行。

警告：

- Maven 编译仍存在既有 deprecation / unchecked warning，以及 Lombok `@Builder` 默认值提示，未阻断。
- 父 POM 当前固定跳过 testCompile/surefire，本轮新增的 `TenantBusinessDataSourceTaskDecoratorTest` 只能作为后续测试配置修复后的回归用例，未实际运行。

跳过项：

- 未启动后端服务复现接口日志，原因是本轮已完成代码级修复和编译验证；真实复现需要本地登录态、Redis、数据库和加密会话。
- 未执行 curl 接口验证，原因同上。

服务清理：

- 本轮未启动任何长期运行服务，无需清理 PID。

## 2026-06-22 `forge-business` dynamic-datasource 纠偏实现验证

执行时间：2026-06-22 11:50 CST

变更范围：

- `forge-starter-tenant` 新增通用 `@TenantBusinessDataSource`、`TenantBusinessDataSourceInfo`、`BusinessDataSourceProperties`、`TenantBusinessDataSourceResolver`、`TenantBusinessDataSourceAspect`、`TenantBusinessDataSourceExecutor`、`TenantBusinessDataSourceTaskDecorator` 和线程池 `BeanPostProcessor`。
- `forge-plugin-system` 新增 `SysTenantBusinessDataSourceResolver`，基于 `sys_tenant.default_business_datasource_code` 和 `business.datasource.tenant-routing-enabled` 解析/校验租户业务 dsKey。
- `forge-business-core` 删除 `BusinessTenantDataSourceResolver`，新增 `business/datasource-demo` Mapper/XML 后端验证用例。
- 删除 `BusinessJdbcTemplateProvider`、旧 `BusinessDataSourceExecutor` 和独立 `BusinessDataSourceContextHolder`，业务主链路改为 MyBatis-Plus Mapper/XML + dynamic-datasource。
- 移除业务侧对低代码 `gen_datasource` 的监听和动态注册逻辑；业务租户数据源只选择已存在的 dynamic-datasource dsKey。
- 新增 Flyway 脚本 `V1.0.76__add_business_datasource_routing_config.sql` 初始化 `business.datasource.tenant-routing-enabled=false`。
- `application.yml` 增加 `forge.business.datasource.enabled=false` 和 `tenant-routing-enabled-default=false` 默认配置。
- 新增 `TenantBusinessDataSourceTaskDecoratorTest`，覆盖异步任务租户和 dynamic-datasource 上下文传播/恢复。
- `TenantBusinessDataSourceTaskDecorator` 自动接入受 `forge.business.datasource.enabled=true` 控制，默认关闭时不覆盖系统线程池。
- `forge-business-core` 移除 `forge-plugin-generator` 依赖，避免业务租户数据源和低代码数据源生命周期耦合。
- `forge-business-core` 移除 `forge-plugin-system` 依赖，避免业务模块持有通用租户业务数据源解析逻辑。

验证命令与结果：

- `git diff --check -- forge-server/forge-business/forge-business-core forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/GenDatasourceMapper.xml forge-server/db/migration/V1.0.76__add_business_datasource_routing_config.sql forge-server/forge-admin-server/src/main/resources/application.yml code-copilot/changes/lowcode-runtime-datasource-isolation docs/superpowers/plans/2026-06-22-tenant-business-datasource-routing.md`
  - 结果：通过，无空白错误。
- `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core -am compile -DskipTests`
  - 结果：通过，`forge-plugin-generator`、`forge-business-core` 及依赖模块编译成功。
- `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests`
  - 结果：通过，`forge-starter-tenant` 通用类、`forge-business-core` demo 和 `forge-admin-server` 入口聚合编译成功。
- `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests`
  - 结果：通过；复核业务租户数据源不再监听/注册低代码 `gen_datasource`，`forge-business-core` 移除 `forge-plugin-generator` 依赖后 admin 聚合编译成功。
- `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests`
  - 结果：通过；`SysTenantBusinessDataSourceResolver` 下沉到 `forge-plugin-system`，`forge-business-core` 删除 resolver 和 `forge-plugin-system` 依赖后 admin 聚合编译成功。
- `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-business/forge-business-core -am test -Dtest=TenantBusinessDataSourceTaskDecoratorTest -Dskip=false -DskipTests=false -Dmaven.test.skip=false -Dmaven.compiler.skip=false`
  - 结果：构建成功，但父 POM 固定配置导致 `testCompile` 显示 `Not compiling test sources`、surefire 显示 `Tests are skipped`，测试源未实际执行。

警告：

- Maven 编译仍存在既有 deprecation / unchecked warning，未阻断。
- dynamic-datasource 4.3.1 本地依赖不存在 `DefaultDataSourceCreator`，实现按实际 API 使用 `DataSourceCreator` 列表选择 Hikari/Druid/Basic 创建器。

跳过项：

- 未执行真实 MyBatis-Plus Mapper/XML 多库集成测试，原因是本地未准备 master + 两个租户业务测试库及测试 Mapper/XML 数据集。
- 未启动后端服务和未执行接口 curl，原因是本地未准备可用租户业务数据源；已补 `business/datasource-demo` 用例，真实启用后可用 `POST /business/datasource-demo/prepare?tenantId=<id>&title=...` 和 `GET /business/datasource-demo/list?tenantId=<id>` 手工验证。
- `TenantBusinessDataSourceTaskDecoratorTest` 已提交，但受父 POM 测试跳过配置影响未实际运行；后续需统一调整 Maven 测试插件配置后执行。

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

## 2026-06-22 `forge-business` 多数据源方案文档纠偏

执行时间：2026-06-22 10:46 CST

变更范围：

- 修订 `spec.md`：明确 `forge-business` 多数据源不能以 `BusinessJdbcTemplateProvider` / 独立 ThreadLocal 作为主方案，必须接入 baomidou dynamic-datasource，让 MyBatis-Plus Mapper/XML 透明切换到租户业务库。
- 补充全局开关要求：`forge.business.datasource.enabled` + `business.datasource.tenant-routing-enabled`，关闭时显式声明业务方法统一走 `master`。
- 补充异步场景要求：`@Async`、线程池、事件监听和定时任务必须显式传播 `tenantId` 与 dynamic-datasource key，任务结束后清理上下文。
- 修订 `tasks.md`：阶段五从“已完成”调整为“租户字段已完成，ORM 路由需纠偏”，新增注解、AOP、动态数据源注册、执行器、TaskDecorator 和测试任务。
- 修订 `test-spec.md`：历史 JDBC 模板验证不再作为阶段五验收标准，新增 `forge-business` dynamic-datasource 纠偏验证矩阵。

验证命令与结果：

- `git diff --check -- code-copilot/changes/lowcode-runtime-datasource-isolation/spec.md code-copilot/changes/lowcode-runtime-datasource-isolation/tasks.md code-copilot/changes/lowcode-runtime-datasource-isolation/test-spec.md`
  - 结果：通过，无空白错误。
- `rg -n "forge-business/forge-business-core|Business datasource correction|dynamic-datasource 纠偏|tenant-routing-enabled|BusinessJdbcTemplateProvider|MyBatis-Plus Mapper/XML|TaskDecorator" code-copilot/changes/lowcode-runtime-datasource-isolation/spec.md code-copilot/changes/lowcode-runtime-datasource-isolation/tasks.md code-copilot/changes/lowcode-runtime-datasource-isolation/test-spec.md -S`
  - 结果：通过，关键纠偏要求、全局开关、异步传播和测试矩阵均已落到文档。

警告：

- 本轮只修改变更文档，未改 Java/Vue 代码。
- 官方 dynamic-datasource 页面地址已记录在 `spec.md`；本地浏览和 `curl` 未返回页面正文，本轮按官方公开使用模型和项目已有依赖设计文档。

跳过项：

- 未执行 Maven/前端构建，原因是本轮只修改文档。
- 未启动服务，原因是本轮没有代码和接口变更。

服务清理：

- 本轮未启动任何长期运行服务，无需清理 PID。
