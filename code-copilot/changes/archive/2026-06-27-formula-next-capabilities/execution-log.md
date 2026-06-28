# 执行日志：formula-next-capabilities

## 2026-06-13 21:14:25 CST - Task 1 执行日志库表与持久化模型

### 变更范围

- 新增公式执行日志 Flyway 脚本 `V1.0.69__add_formula_observability_and_function_market.sql`。
- 新增 `AiFormulaExecutionLog`、`FormulaExecutionLogMapper`、`FormulaExecutionLogMapper.xml`。
- 新增 `FormulaExecutionLogQueryDTO`、`FormulaExecutionLogResponse`、`FormulaExecutionLogService`。
- 新增 `FormulaExecutionLogServiceTest`。

### 执行命令与结果

- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaExecutionLogServiceTest`
  - 失败，默认 Java 版本不支持 target 17：`无效的目标发行版: 17`。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaExecutionLogServiceTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 但父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `mvn -q -pl forge-framework/forge-plugin-parent/forge-plugin-generator dependency:build-classpath -Dmdep.outputFile=/private/tmp/forge_generator_cp.txt -DincludeScope=test`
  - 成功生成临时 classpath。
- `javac -cp "...$(cat /private/tmp/forge_generator_cp.txt)" /private/tmp/FormulaExecutionLogServiceSmoke.java`
  - 编译通过。
- `java -cp "/private/tmp:...$(cat /private/tmp/forge_generator_cp.txt)" FormulaExecutionLogServiceSmoke`
  - 输出 `FormulaExecutionLogServiceSmoke PASS`。

### 结论

- Task 1 目标模块编译通过。
- Service 行为通过 smoke 验证：保存成功日志、默认失败状态、手机号脱敏、分页映射、详情租户过滤均通过。
- SQL 人工检查通过：`CREATE TABLE IF NOT EXISTS`，索引内联，`tenant_id` 默认 `1`，无依赖字段顺序的插入。

### 跳过项

- 未启动后端服务和数据库；Task 1 不包含查询 API，接口验证留到 Task 4。
- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已记录限制并用临时 runner 补充行为验证。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 13:01:30 CST - Task 19 启动装配修复：FormulaFunctionMarketService 构造器

### 问题

- 启动时报 `FormulaFunctionMarketService: No default constructor found`。
- 根因是 `FormulaFunctionMarketService` 同时存在 Lombok `@RequiredArgsConstructor` 生成构造器和手写内存构造器，Spring 面对多个构造器时没有明确的注入构造器，导致 bean 实例化失败。

### 变更范围

- 修改 `FormulaFunctionMarketService`，移除 `@RequiredArgsConstructor`，显式声明 `FormulaFunctionMapper + FormulaFunctionRegistry` 构造器并标注 `@Autowired`。
- 保留 `FormulaFunctionMarketService(FormulaFunctionRegistry)` 作为测试/内存模式构造器，不参与 Spring 自动装配。

### 执行命令与结果

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaFunctionMarketServiceTest,FormulaValidationServiceTest,FormulaControllerTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为 JUnit/Surefire 通过证据。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaFunctionMarketServiceTest.java FormulaValidationServiceTest.java FormulaControllerTest.java`
  - 测试源码编译通过，仅有既有弃用 API 提示。
- `rg -n "[ \t]+$" FormulaFunctionMarketService.java`
  - 无输出，未发现行尾空白。
- `git diff --check -- FormulaFunctionMarketService.java`
  - 无输出，diff 空白检查通过。

### 结论

- `FormulaFunctionMarketService` 的 Spring 注入构造器已明确，启动链路中 `FormulaValidationService -> FormulaFunctionMarketService` 不再依赖默认构造器推断。

### 跳过项

- 本轮未启动后端 admin 服务和数据库；真实 Spring Boot 启动仍需在具备本地 MySQL/Redis 配置的环境复测。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 10:31:00 CST - Task 18 函数市场与动态函数选择前端

### 变更范围

- 修改 `forge-admin-ui/src/api/formula.js`，新增函数市场分页、详情、安装、启用、禁用 API。
- 新增 `FormulaFunctionBrowser.vue`，从 `/api/ai/business/formula/functions` 动态读取可用函数，支持搜索、分类筛选和插入函数调用。
- 新增 `FormulaExpressionEditor.vue`，承载表达式 textarea、字段变量、基础 token 和动态函数浏览器。
- 新增 `FormulaFunctionMarket.vue`，通过居中弹窗提供函数市场分页、详情、安装、启用、禁用。
- 修改 `FormulaConfigPanel.vue`，移除静态函数下拉，接入 `FormulaExpressionEditor`。
- 修改 `BusinessFieldPropertyPanel.vue`，保存公式 payload 时自动生成 `functionRefs`。
- 修改 `BusinessObjectDesignerShell.vue` 和 `object-designer.[objectCode].vue`，在更多菜单增加“函数市场”入口并挂载弹窗。

### 执行命令与结果

- `rg -n "[ \\t]+$" <Task 18 相关前端文件>`
  - 无输出，未发现行尾空白。
- `git diff --check -- <Task 18 相关前端文件>`
  - 无输出，未发现 diff 空白错误。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/api/formula.js src/views/app-center/components/designer/formula/FormulaFunctionBrowser.vue src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue src/views/app-center/components/designer/formula/FormulaFunctionMarket.vue src/views/app-center/components/designer/formula/FormulaConfigPanel.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/views/app-center/components/designer/BusinessObjectDesignerShell.vue 'src/views/app-center/object-designer.[objectCode].vue'`
  - 首次失败：`object-designer.[objectCode].vue` 导入排序不符合 `perfectionist/sort-imports`。
  - 修正导入顺序后复跑通过，无错误。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 构建成功，输出 `✓ built in 1m 26s`。
  - 警告项：既有 CSS `//` 注释触发 esbuild `js-comment-in-css` warning；`src/store/index.js` 既被动态导入又被静态导入触发 chunk warning；均非本轮新增阻断。

### 结论

- Task 18 前端静态质量和生产构建通过。
- 表达式编辑器函数列表已从后端 `/functions` 动态读取，函数市场入口在对象设计器更多菜单中打开居中弹窗。
- 字段公式保存 payload 会携带 `functionRefs`，为 Task 17 的禁用函数发布校验提供前端协议数据。

### 跳过项

- 未启动后端 admin 服务和数据库，未覆盖函数市场真实 HTTP、鉴权、操作日志入库和禁用后编辑器刷新联调。
- 未启动 Vite dev server 做浏览器点击截图；本轮以 ESLint 和生产构建作为前端增量验证闭环。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 10:13:00 CST - Task 17 函数市场后端 API

### 变更范围

- 新增 `FormulaFunctionMarketQueryDTO`、`FormulaFunctionInstallRequest`、`FormulaFunctionMarketResponse`。
- 新增 `FormulaFunctionMarketService`，支持函数市场分页、详情、安装、启用、禁用、已启用函数列表和函数引用市场状态校验。
- 修改 `FormulaFunctionMapper` / `FormulaFunctionMapper.xml`，新增市场查询 SQL、已启用安装函数查询和安装状态 upsert。
- 修改 `FormulaController`，新增 `/function-market/page`、`/function-market/{functionCode}`、`/function-market/install`、`/enable`、`/disable`，安装/启停接口记录 `@OperationLog`。
- 修改 `FormulaValidationService`，Spring 注入路径下通过市场服务校验函数安装/启用状态，禁用函数阻断公式校验。
- 修改 `V1.0.69__add_formula_observability_and_function_market.sql`，补齐全部内置函数 seed，避免 `/functions` 从数据库读取后缺少注册中心函数。
- 新增 `FormulaFunctionMarketServiceTest`，扩展 `FormulaControllerTest`、`FormulaValidationServiceTest`。

### 执行命令与结果

- `rg -n "new FormulaController\\(" forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java -g '*.java'`
  - 仅发现 `FormulaControllerTest` 一处直接构造器调用，已同步改为注入 `FormulaFunctionMarketService`。
- `rg -n "[ \\t]+$" <Task 17 相关 Java/XML/SQL 文件>`
  - 无输出，未发现行尾空白。
- `git diff --check -- <Task 17 相关 Java/XML/SQL 文件>`
  - 无输出，未发现 diff 空白错误。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaFunctionMarketServiceTest,FormulaControllerTest,FormulaValidationServiceTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-server/.../target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaFunctionMarketServiceTest.java FormulaControllerTest.java FormulaValidationServiceTest.java`
  - 编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-server/.../target/classes:$(cat /private/tmp/forge_generator_cp.txt)" /private/tmp/FormulaFunctionMarketSmoke.java`
  - 首次失败：`FormulaValidationResult` 包名误写为 `domain.formula`，已修正为 `service.formula`。
  - 复跑编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... java -cp "/private/tmp/forge_formula_test_classes:forge-server/.../target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaFunctionMarketSmoke`
  - 输出 `FormulaFunctionMarketSmoke PASS`。

### 结论

- Task 17 目标模块主代码编译通过。
- 测试源码编译覆盖 `FormulaFunctionMarketServiceTest`、`FormulaControllerTest`、`FormulaValidationServiceTest`。
- Smoke 覆盖：函数市场分页和详情；安装状态 upsert 显式生成 ID；禁用函数后从可选函数列表移除；禁用函数引用阻断公式校验；重新安装启用后校验恢复；Controller 市场接口返回 `RespInfo.success(data)`。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已用测试源码编译和临时 runner 补充行为验证。
- 未启动后端服务和数据库；真实 HTTP 鉴权、操作日志入库、Flyway 实跑和真实 MyBatis 分页 SQL 留到 Task 19-20 总体验证。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 09:40:41 CST - Task 15 函数注册表库表与持久化模型

### 变更范围

- 修改 `V1.0.69__add_formula_observability_and_function_market.sql`，新增 `ai_formula_function`、`ai_formula_function_version`、`ai_formula_function_install` 表和内置函数 seed。
- 新增 `AiFormulaFunction`、`AiFormulaFunctionVersion`、`AiFormulaFunctionInstall`。
- 新增 `FormulaFunctionMapper` 和 `FormulaFunctionMapper.xml`，提供函数分页、详情、版本和安装状态查询。
- 新增 `FormulaFunctionPersistenceTest`。

### 执行命令与结果

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaFunctionPersistenceTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaFunctionPersistenceTest.java`
  - 测试源码编译通过。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" /private/tmp/FormulaFunctionPersistenceSmoke.java`
  - smoke runner 编译通过。
- `JAVA_HOME=... PATH=... java -cp "/private/tmp/forge_formula_test_classes:forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaFunctionPersistenceSmoke`
  - 输出 `FormulaFunctionPersistenceSmoke PASS`。
- `rg -n "[ \t]+$" <Task 15 相关 SQL/Java/XML 文件>`
  - 无输出，未发现行尾空白。
- `git diff --check -- <Task 15 相关 SQL/Java/XML 文件>`
  - 无输出，diff 空白检查通过。

### 结论

- Task 15 目标模块编译通过。
- 行为 smoke 覆盖：函数元数据实体可读写；版本实现类型与 Bean 名称可读写；租户安装启用状态可读写；Mapper 具备分页方法签名；迁移脚本包含三张函数表、seed 防重复和内置 Bean 标识；Mapper XML 包含分页、详情、版本和安装查询。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已用测试源码编译和临时 runner 补充行为验证。
- 未启动后端服务和数据库；真实 Flyway 执行、函数分页 SQL 和租户安装状态查询留到后续服务级验证。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 09:56:05 CST - Task 16 函数注册中心与表达式引擎集成

### 变更范围

- 新增 `FormulaFunctionDefinition`，定义函数编码、分类、参数 schema、返回类型、实现 Bean、方法、状态和超时配置。
- 新增 `FormulaBuiltinFunctionProvider`，提供 math、string、seq、date 内置函数 Java Bean 实现。
- 新增 `FormulaFunctionInvoker`，通过 Java Bean 方法调用函数并校验参数数量、参数类型、返回类型和执行耗时。
- 新增 `FormulaFunctionRegistry`，统一函数发现、Aviator 注册、表达式函数引用校验和 `/functions` 响应转换。
- 修改 `ExpressionExecutor`、`FormulaValidationService`、`FormulaExecutionEngine` 和 `FormulaController` 接入注册中心。
- 新增 `FormulaFunctionRegistryTest`，扩展 `ExpressionExecutorTest`、`FormulaValidationServiceTest`、`FormulaControllerTest`。

### 执行命令与结果

- `JAVA_HOME=... PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaFunctionRegistryTest,ExpressionExecutorTest,FormulaValidationServiceTest -DskipTests=false -Dmaven.test.skip=false`
  - 首次失败：`FormulaExecutionEngine` 漏保留二参构造器；`FormulaFunctionRegistry` 覆盖了 Aviator 当前版本不存在的简短 varargs 签名。
  - 修复后复跑通过，`BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaFunctionRegistryTest.java ExpressionExecutorTest.java FormulaValidationServiceTest.java FormulaControllerTest.java /private/tmp/FormulaFunctionRegistrySmoke.java`
  - 测试源码与 smoke runner 编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=... PATH=... java -cp "/private/tmp/forge_formula_test_classes:forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaFunctionRegistrySmoke`
  - 输出 `FormulaFunctionRegistrySmoke PASS`。
  - 覆盖注册函数执行、参数数量错误、参数类型错误、禁用函数校验、Controller 函数列表。
- `rg -n "[ \t]+$" <Task 16 相关 Java 文件>`
  - 无输出，未发现行尾空白。
- `git diff --check -- <Task 16 相关 Java 文件>`
  - 无输出，diff 空白检查通过。

### 结论

- Task 16 目标模块编译通过。
- 行为 smoke 覆盖：启用函数可在表达式中执行；禁用函数不能通过公式校验；参数数量和类型错误返回明确异常；`GET /functions` 的数据源已从 Controller 硬编码迁移到函数注册中心。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已用测试源码编译和临时 runner 补充行为验证。
- 未启动后端服务和数据库；真实 HTTP、租户安装状态和市场函数管理留到 Task 17/18。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 08:45:00 CST - Task 11 跨对象发布校验与重算策略

### 变更范围

- 新增 `FormulaObjectDependencyAnalyzer`，将跨对象公式转换为对象图依赖边，校验关系、源字段、目标字段和 returnField，并检测对象级循环依赖。
- 新增 `CrossObjectRecomputeTaskService`，为 STORED 跨对象公式生成 `PENDING` 待重算任务和 `objectCode:recordId:fieldCode:dependencyTrace` 幂等键。
- 修改 `FormulaPublishValidator`，新增对象图上下文校验重载，保留旧 `validate(modelSchema)` 兼容入口。
- 修改 `BusinessObjectPublishService`，发布检查接入对象图校验，发布成功后为当前对象生成跨对象待重算任务。
- 新增 `FormulaObjectDependencyAnalyzerTest`，扩展 `FormulaPublishValidatorTest` 和 `BusinessObjectPublishServiceFormulaTest`。

### 执行命令与结果

- `cat code-copilot/rules/automated-testing-standard.md`
  - 已读取自动化测试与验证标准，并复用当前 `spec.md`、`tasks.md`、`test-spec.md` 和 `execution-log.md` 执行增量验证。
- `rg -n "[ \t]+$" <Task 11 相关 Java 文件>`
  - 无输出，未发现行尾空白。
- `git diff --check -- <Task 11 相关 Java 文件>`
  - 无输出，diff 空白检查通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaObjectDependencyAnalyzerTest,FormulaPublishValidatorTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaObjectDependencyAnalyzerTest.java FormulaPublishValidatorTest.java BusinessObjectPublishServiceFormulaTest.java`
  - 测试源码编译通过。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" /private/tmp/FormulaObjectDependencySmoke.java`
  - smoke runner 编译通过。
- `JAVA_HOME=... PATH=... java -cp "/private/tmp/forge_formula_test_classes:forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaObjectDependencySmoke`
  - 输出 `FormulaObjectDependencySmoke PASS`。

### 结论

- Task 11 目标模块编译通过。
- 行为 smoke 覆盖：合法跨对象公式生成对象依赖边；`order -> customer -> order` 对象级循环被检测；缺失关系发布校验被阻断；STORED 跨对象公式生成 `PENDING` 重算任务并包含幂等键。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已用测试源码编译和临时 runner 补充行为验证。
- 未启动后端服务和数据库；真实发布检查 HTTP 调用、跨对象重算任务持久化和运行态重算消费留到后续服务级验证。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 09:40:00 CST - Task 12 LOOKUP / 跨对象公式前端配置

### 变更范围

- 新增 `FormulaConfigPanel.vue`，将公式配置区从 `BusinessFieldPropertyPanel.vue` 拆出，统一承载基础配置、表达式校验、预览、调试、执行日志和依赖图入口。
- 新增 `FormulaLookupPanel.vue`，提供 LOOKUP relationCode、targetObjectCode、sourceField、targetField、returnField、notFoundValue 配置。
- 新增 `FormulaCrossObjectPanel.vue`，提供一跳跨对象路径选择，只生成 `relationCode.returnField`，不暴露多跳自由输入。
- 修改 `BusinessFieldPropertyPanel.vue`，改为引用公式配置组件，并保持旧 CALC / AGGREGATE / CONDITIONAL 配置回显。
- 修改 `FormulaDebuggerPanel.vue`、`FormulaDependencyGraph.vue`，调试和依赖图请求透传 `lookup`、`crossObject` 配置。

### 执行命令与结果

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/*.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/api/formula.js`
  - 通过，无 eslint 错误。
- `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaLookupPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaCrossObjectPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaDebuggerPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaDependencyGraph.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/api/formula.js`
  - 无输出，未发现行尾空白。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaLookupPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaCrossObjectPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaDebuggerPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaDependencyGraph.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/api/formula.js`
  - 无输出，diff 空白检查通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 构建成功，输出 `✓ built in 1m 4s`。
  - 非阻断警告：既有 CSS `//` 注释警告、`src/store/index.js` 动静态混合导入 chunk 警告。
- `python3 /Users/yaomindong/.agents/skills/webapp-testing/scripts/with_server.py --server "...vite preview --host 127.0.0.1 --port 4174" --port 4174 --timeout 90 -- python3 /private/tmp/forge_formula_ui_smoke.py`
  - preview 未在 90 秒内被 helper 识别，helper 已停止服务。
- `/bin/zsh -lc 'source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm --dir forge-admin-ui exec vite preview --host 127.0.0.1 --port 4174'`
  - 当前沙箱禁止监听端口，失败信息为 `listen EPERM: operation not permitted 127.0.0.1:4174`。

### 结论

- Task 12 前端静态质量和生产构建通过。
- 字段属性面板可配置 LOOKUP 类型；LOOKUP payload 保留 `lookup` 元数据；跨对象配置只能从一跳关系选择生成路径，UI 不允许手写多跳路径。
- 旧 CALC / AGGREGATE / CONDITIONAL 继续由 `FormulaConfigPanel.vue` 回显和保存，调试器与依赖图会收到当前未保存的 lookup/crossObject 配置。

### 跳过项

- 未启动后端 admin 服务和数据库，未覆盖字段保存接口真实联调。
- 未完成浏览器页面点击冒烟；当前沙箱禁止本地 preview 监听端口，失败已记录为环境边界。

### 服务清理

- `with_server.py` 已停止其尝试启动的 preview 进程。
- 直接 preview 命令因端口监听权限失败退出，无常驻服务需要清理。

## 2026-06-14 09:15:30 CST - Task 13 条件规则 AST 与编译 API

### 变更范围

- 新增 `ConditionRuleNode`，定义条件规则 JSON AST，支持分组节点和条件节点。
- 新增 `ConditionRuleOperator`，覆盖 AND / OR 和 EQ、NE、GT、GTE、LT、LTE、IN、NOT_IN、CONTAINS、STARTS_WITH、ENDS_WITH、IS_NULL、NOT_NULL。
- 新增 `ConditionRuleCompiler`，将 AST 编译为 Aviator 表达式，并校验字段格式、字段存在性、操作符和值类型。
- 新增 `ConditionRuleCompileRequest`、`ConditionRuleCompileResponse`。
- 修改 `FormulaController`，新增 `POST /api/ai/business/formula/rule/compile` 和 `POST /api/ai/business/formula/rule/validate`。
- 新增 `ConditionRuleCompilerTest`，扩展 `FormulaControllerTest`。

### 执行命令与结果

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=ConditionRuleCompilerTest,FormulaControllerTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `JAVA_HOME=... PATH=... mvn -q -pl forge-framework/forge-plugin-parent/forge-plugin-generator dependency:build-classpath -Dmdep.outputFile=/private/tmp/forge_generator_cp.txt -DincludeScope=test`
  - 成功刷新测试 classpath。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" ConditionRuleCompilerTest.java FormulaControllerTest.java`
  - 测试源码编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" /private/tmp/ConditionRuleCompilerSmoke.java`
  - 首次编译失败，临时 runner 误把 `FormulaExecutionLogService` 从 `domain.formula` 包导入；修正为 `service.formula` 后复跑通过。
- `JAVA_HOME=... PATH=... java -cp "/private/tmp/forge_formula_test_classes:forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" ConditionRuleCompilerSmoke`
  - 输出 `ConditionRuleCompilerSmoke PASS`。
- `rg -n "[ \t]+$" <Task 13 相关 Java 文件>`
  - 无输出，未发现行尾空白。
- `git diff --check -- <Task 13 相关 Java 文件>`
  - 无输出，diff 空白检查通过。

### 结论

- Task 13 目标模块编译通过。
- 行为 smoke 覆盖：`amount > 1000 AND status = ACTIVE` 编译为 `(amount > 1000 && status == 'ACTIVE')`；依赖字段按顺序输出；空分组返回明确错误；Controller `/rule/compile` 返回 `RespInfo.success(data)`。
- 编译器对 IN / NOT_IN 强制集合值，对数值字段强制数字值，避免前端传错类型后生成不稳定表达式。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已用测试源码编译和临时 runner 补充行为验证。
- 未启动后端服务；真实 HTTP 鉴权和前端规则设计器调用留到 Task 14 / Task 19 验证。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 10:05:00 CST - Task 14 条件规则设计器前端组件

### 变更范围

- 修改 `forge-admin-ui/src/api/formula.js`，新增 `compileConditionRule`、`validateConditionRule`。
- 新增 `FormulaConditionRuleNode.vue`，提供递归 AND / OR 分组、条件节点新增/删除、字段/操作符/值编辑。
- 新增 `FormulaConditionRuleDesigner.vue`，规则变更时生成本地表达式快照，并调用后端 `/rule/compile` 校验。
- 修改 `FormulaConfigPanel.vue`，CONDITIONAL 公式新增“表达式 / 规则设计”模式，规则模式不再显示通用表达式大文本框。
- 修改 `BusinessFieldPropertyPanel.vue`，初始化和保存 `formulaConfig.rule`，并将规则编译错误纳入本地校验。
- 修改 `FormulaDebuggerPanel.vue`，调试请求透传 `rule`，并基于调试上下文展示规则分组和条件节点命中状态。

### 执行命令与结果

- `/bin/zsh -lc 'source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm exec eslint src/views/app-center/components/designer/formula/*.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/api/formula.js'`
  - 首次发现 `FormulaConditionRuleDesigner.vue` 中 `compileState` 定义顺序和字符串引号风格问题；修复后复跑通过。
  - 增加 timer 清理后再次复跑通过。
- `/bin/zsh -lc 'source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build'`
  - 第一次构建成功，输出 `✓ built in 58.04s`。
  - 增加 timer 清理后复跑构建成功，输出 `✓ built in 1m 1s`。
  - 非阻断警告：既有 CSS `//` 注释警告、`src/store/index.js` 动静态混合导入 chunk 警告。
- `rg -n "[ \t]+$" <Task 14 相关前端文件>`
  - 无输出，未发现行尾空白。
- `git diff --check -- <Task 14 相关前端文件>`
  - 无输出，diff 空白检查通过。

### 结论

- Task 14 前端静态质量和生产构建通过。
- 条件公式支持表达式模式和规则设计模式；规则模式保存 rule JSON，并把编译后的 Aviator 表达式写入 `condition.expression` 和公式 `expression` 快照。
- 调试器可展示规则整体分组和叶子条件命中状态，便于配置人员排查条件分支。

### 跳过项

- 未启动后端 admin 服务和数据库，未覆盖真实字段保存接口与登录后点击流程。
- 本地浏览器点击冒烟因当前沙箱端口监听限制未执行，沿用 Task 12 记录的 `listen EPERM` 环境边界。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-13 22:19:28 CST - Task 7 公式可观测性前端组件

### 变更范围

- 修改 `forge-admin-ui/src/api/formula.js`，新增 `debugFormula`、`getFormulaLogPage`、`getFormulaLogDetail`、`getDependencyGraph`。
- 新增 `FormulaTraceSteps.vue`、`FormulaDebuggerPanel.vue`、`FormulaExecutionLogDrawer.vue`、`FormulaDependencyGraph.vue`。
- 修改 `BusinessFieldPropertyPanel.vue`，在公式设置区新增“调试 / 执行日志 / 依赖图”入口，使用居中弹窗承载工具，避免右侧抽屉。
- 修改 `BusinessFieldManager.vue` 和 `object-designer.[objectCode].vue`，透传 `objectCode` 给公式工具。

### 执行命令与结果

- `cat code-copilot/rules/automated-testing-standard.md`
  - 已读取自动化测试与验证标准，并按 Task 7 增量范围执行验证。
- `git status --short`
  - 工作区已有多处未提交和未跟踪文件；本轮只处理 Task 7 相关前端文件和变更文档，未回退其他改动。
- `rg -n "[ \t]+$" forge-admin-ui/src/api/formula.js forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' forge-admin-ui/src/views/app-center/components/designer/formula`
  - 无输出，未发现行尾空白。
- `git diff --check -- forge-admin-ui/src/api/formula.js forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' forge-admin-ui/src/views/app-center/components/designer/formula`
  - 无输出，diff 空白检查通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 构建成功，输出 `✓ built in 1m 2s`。
  - 非阻断警告：既有 CSS `//` 注释警告、`store/index.js` 动静态混合导入 chunk 警告、chunk size 信息。
- `python3 /Users/yaomindong/.agents/skills/webapp-testing/scripts/with_server.py --server "...vite --host 127.0.0.1 --port 5173" --port 5173 --timeout 60 -- python3 /private/tmp/forge_formula_ui_smoke.py`
  - 失败，Vite 因 `EMFILE: too many open files, watch` 未启动。
- `python3 /Users/yaomindong/.agents/skills/webapp-testing/scripts/with_server.py --server "...ulimit -n 65535 ... CHOKIDAR_USEPOLLING=true ...vite --host 127.0.0.1 --port 5173" --port 5173 --timeout 90 -- python3 /private/tmp/forge_formula_ui_smoke.py`
  - 失败，helper 未能在超时时间内识别 5173 端口；后续直接启动 Vite 验证服务实际可用。
- `/bin/zsh -lc 'ulimit -n 65535 && source ~/.nvm/nvm.sh && nvm use v20.19.0 && CHOKIDAR_USEPOLLING=true pnpm --dir forge-admin-ui exec vite --host 127.0.0.1 --port 5173'`
  - Vite 成功启动，输出 `Local: http://127.0.0.1:5173/`。
- `python3 /private/tmp/forge_formula_ui_smoke.py`
  - 首次失败，Playwright Chromium 被 macOS 沙箱拦截：`MachPortRendezvousServer ... Permission denied`。
- `python3 /private/tmp/forge_formula_ui_smoke.py`（沙箱外执行）
  - 首次沙箱外执行到页面，但因未启动后端，登录配置、三方登录、验证码接口返回 500。
  - 调整 smoke 脚本排除上述登录页后端缺失错误后复跑成功，输出 `forge_formula_ui_smoke PASS`，页面标题为 `登录页 | 企业级中后台基础框架`。

### 结论

- Task 7 前端生产构建通过。
- 字段属性面板已集成调试器、执行日志和依赖图入口；调试器和依赖图会使用当前面板未保存的公式配置，避免调试旧配置。
- 本地 Vite 服务和客户端入口加载通过；因本轮未启动后端，真实公式接口交互点击未覆盖。

### 跳过项

- 未启动后端 admin 服务、数据库和登录会话，未做真实对象设计器字段页内的接口点击联调。
- 执行日志列表、调试器和依赖图的 API 调用已经在后端 Task 4-6 smoke 中覆盖协议层，本轮只覆盖前端构建和页面加载。

### 服务清理

- 本轮直接启动的 Vite 进程 `81818`、`82301` 已执行 `kill 81818 82301` 停止。
- 进程复查仅剩早于本轮存在的其他 Vite 进程，未清理非本轮启动服务。

## 2026-06-14 07:09:40 CST - Task 8 LOOKUP / 跨对象公式领域模型扩展

### 变更范围

- 修改 `FormulaType`，新增 `LOOKUP`。
- 修改 `FormulaConfig`，新增 `lookup`、`crossObject`、`rule`、`functionRefs`，并补充 Jackson 反序列化构造器。
- 新增 `LookupConfig`、`CrossObjectConfig`、`CrossObjectRecomputeMode`。
- 修改 `AggregateConfig`、`ConditionConfig`，补充 Jackson 构造注解，确保旧 AGGREGATE / CONDITIONAL JSON 可直接反序列化。
- 修改 `AbstractFormulaRuntime`、`FormulaPublishValidator`，支持从字段 `formulaConfig` Map 解析 LOOKUP / crossObject 元数据。
- 修改 `BusinessFieldDTO`、`BusinessFieldVO` 注释，保持 `formulaConfig` JSON 透传。
- 新增 `FormulaConfigTest`。

### 执行命令与结果

- `rg -n "[ \t]+$" <Task 8 相关 Java 文件>`
  - 无输出，未发现行尾空白。
- `git diff --check -- <Task 8 相关 Java 文件>`
  - 无输出，diff 空白检查通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaConfigTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-framework/forge-plugin-parent/forge-plugin-generator dependency:build-classpath -Dmdep.outputFile=/private/tmp/forge_generator_cp.txt -DincludeScope=test`
  - 成功生成临时 classpath。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaConfigTest.java`
  - 测试源码编译通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "..." /private/tmp/FormulaConfigSmoke.java`
  - 编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... java -cp "/private/tmp/forge_formula_test_classes:..." FormulaConfigSmoke`
  - 输出 `FormulaConfigSmoke PASS`。

### 结论

- Task 8 目标模块编译通过。
- 行为 smoke 覆盖：旧 CALC / AGGREGATE / CONDITIONAL JSON 兼容反序列化；LOOKUP 缺少 lookup 配置会抛明确异常；有效 LOOKUP 配置可反序列化；跨对象路径只允许一跳；crossObject 默认 `ASYNC`；运行时 Map 解析能读出 LOOKUP / crossObject 元数据。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已用临时 runner 补充行为验证。
- 本轮不实现 LOOKUP 查询执行、跨对象批量预取和发布期对象图校验，这些分别属于 Task 9-11。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 07:59:20 CST - Task 9 LOOKUP 公式解析与执行

### 变更范围

- 新增 `FormulaLookupResolver`、`LookupResolveResult`。
- 修改 `FormulaExecutionEngine`，接入 LOOKUP 执行分支和 trace metadata。
- 修改 `FormulaValidationService`，允许 LOOKUP 空表达式，校验 lookup 配置并拦截 path-style relationCode。
- 修改 `FormulaDependencyAnalyzer`，将 LOOKUP sourceField 纳入拓扑排序、深度计算和字段存在性校验。
- 修改 `FormulaDebugRequest`、`FormulaDependencyGraphRequest`、`FormulaDebugService`、`FormulaDependencyGraphService`，补充 lookup 配置解析和 LOOKUP 依赖图边。
- 新增 `FormulaLookupResolverTest`、`FormulaExecutionEngineLookupTest`，扩展 `FormulaDependencyAnalyzerTest`、`FormulaValidationServiceTest`、`FormulaDependencyGraphServiceTest`。

### 执行命令与结果

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaLookupResolverTest,FormulaExecutionEngineLookupTest,FormulaDependencyAnalyzerTest,FormulaValidationServiceTest,FormulaDependencyGraphServiceTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -q -pl forge-framework/forge-plugin-parent/forge-plugin-generator dependency:build-classpath -Dmdep.outputFile=/private/tmp/forge_generator_cp.txt -DincludeScope=test`
  - 成功生成临时 classpath。
- `javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" ... NoSuchFile.java`
  - 首次手工编译命令误带不存在的占位文件路径，`javac` 返回 `找不到文件`；已修正文件列表后复跑。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaLookupResolverTest.java FormulaExecutionEngineLookupTest.java FormulaDependencyAnalyzerTest.java FormulaValidationServiceTest.java FormulaDependencyGraphServiceTest.java`
  - 测试源码编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" /private/tmp/FormulaLookupSmoke.java`
  - 编译通过。
- `JAVA_HOME=... PATH=... java -cp "/private/tmp/forge_formula_test_classes:forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaLookupSmoke`
  - 输出 `FormulaLookupSmoke PASS`。
- `rg -n "[ \t]+$" <Task 9 相关 Java 文件>`
  - 首次发现 `FormulaDependencyAnalyzer.java` 旧空白行，已清理；复跑无输出。
- `git diff --check -- <Task 9 相关 Java 文件>`
  - 无输出，diff 空白检查通过。

### 结论

- Task 9 目标模块编译通过。
- 行为 smoke 覆盖：LOOKUP 命中返回目标字段；未命中返回 `notFoundValue` 且不计错误；resolver 异常走降级错误处理；trace metadata 包含 relationCode、targetObjectCode、sourceField、targetField、returnField、lookupMatched；LOOKUP sourceField 参与依赖校验；依赖图输出 LOOKUP 边；path-style relationCode 被校验拦截。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已用测试源码编译和临时 runner 补充行为验证。
- 未启动后端服务和数据库；真实对象关系、动态表数据、租户和数据权限联调留到后续服务级验证。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 08:15:40 CST - Task 10 一跳跨对象路径解析与批量预取

### 变更范围

- 新增 `CrossObjectPath`、`FormulaCrossObjectResolver`、`FormulaReferenceResolver`。
- 修改 `VirtualFormulaRuntime`，为包含 `crossObject` 的 VIRTUAL 公式增加批量预取路径。
- 修改 `DetailRecordFetcher`、`DynamicDetailRecordFetcher`、`DynamicCrudRepository`，支持目标对象按关联键批量 IN 查询。
- 修改 `FormulaDebugRequest`、`FormulaDependencyGraphRequest`、`FormulaDebugService`、`FormulaDependencyGraphService`，支持 `crossObject` 元数据解析和 `CROSS_OBJECT` 依赖图边。
- 新增 `FormulaCrossObjectResolverTest`，扩展 `VirtualFormulaRuntimeTest`、`FormulaDependencyGraphServiceTest`。

### 执行命令与结果

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaCrossObjectResolverTest,VirtualFormulaRuntimeTest,FormulaDependencyGraphServiceTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaCrossObjectResolverTest.java VirtualFormulaRuntimeTest.java FormulaDependencyGraphServiceTest.java`
  - 测试源码编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" /private/tmp/FormulaCrossObjectSmoke.java`
  - 编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=... PATH=... java -cp "/private/tmp/forge_formula_test_classes:forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaCrossObjectSmoke`
  - 首次运行时 smoke 自身断言把单记录 `resolve` 调用误认为批量调用，失败信息为 `batch source size, expected=2, actual=1`；已修正 runner 区分单记录和批量调用后复跑。
  - 复跑输出 `FormulaCrossObjectSmoke PASS`。
- `rg -n "[ \t]+$" <Task 10 相关 Java 文件>`
  - 首次发现 `DynamicCrudRepository.java` 既有空白行，已随本轮触达文件清理；复跑无输出。
- `git diff --check -- <Task 10 相关 Java 文件>`
  - 无输出，diff 空白检查通过。

### 结论

- Task 10 目标模块编译通过。
- 行为 smoke 覆盖：一跳路径解析；多跳路径拒绝；单记录跨对象解析返回目标字段；VIRTUAL 列表公式批量预取并计算 `customer.level`；批量 provider 在列表运行时只调用一次；运行结果不泄漏内部 `customer` 嵌套上下文；依赖图输出 `CROSS_OBJECT` 边。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已用测试源码编译和临时 runner 补充行为验证。
- 未启动后端服务和数据库；真实动态表 IN 查询、租户和数据权限边界留到服务级验证。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-13 21:58:55 CST - Task 6 Dependency 图后端 API

### 变更范围

- 新增 `FormulaDependencyGraphRequest`、`FormulaDependencyGraphResponse`、`FormulaDependencyGraphNode`、`FormulaDependencyGraphEdge`。
- 新增 `FormulaDependencyGraphService`，将现有 DAG 分析结果转换为 nodes / edges。
- 修改 `FormulaController`，新增 `POST /api/ai/business/formula/dependency/graph`。
- 新增 `FormulaDependencyGraphServiceTest`，扩展 `FormulaControllerTest`。

### 执行命令与结果

- `rg -n "new FormulaController\\(" ...`
  - 仅发现 `FormulaControllerTest` 一处直接构造器调用，已同步补充 `FormulaDependencyGraphService`。
- `rg -n "[ \\t]+$" FormulaController.java FormulaDependencyGraphService.java FormulaDependencyGraph*.java FormulaDependencyGraphServiceTest.java FormulaControllerTest.java`
  - 无输出，未发现行尾空白。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaDependencyGraphServiceTest,FormulaControllerTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "...$(cat /private/tmp/forge_generator_cp.txt)" FormulaDependencyGraphServiceTest.java FormulaControllerTest.java`
  - 编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "...$(cat /private/tmp/forge_generator_cp.txt)" /private/tmp/FormulaDependencyGraphSmoke.java`
  - 编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... java -cp "/private/tmp/forge_formula_test_classes:...$(cat /private/tmp/forge_generator_cp.txt)" FormulaDependencyGraphSmoke`
  - 输出 `FormulaDependencyGraphSmoke PASS`。

### 结论

- Task 6 目标模块编译通过。
- 行为 smoke 覆盖：A 依赖 B、C 返回 3 个节点和 2 条 DEPENDS_ON 边；循环依赖返回 `hasCycle=true` 和非空 `cyclePath`；聚合公式返回 RELATION 节点和 AGGREGATE 边；Controller `/dependency/graph` 返回 `RespInfo.success(data)`。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已用临时 runner 补充行为验证。
- 未启动后端服务和数据库；真实 HTTP 和前端图展示留到 Task 7 验证。
- LOOKUP / 跨对象公式尚未实现，本轮图结构只输出对象内依赖和聚合关系边。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-13 21:51:39 CST - Task 5 公式调试器后端 API

### 变更范围

- 新增 `FormulaDebugRequest`、`FormulaDebugResponse`。
- 新增 `FormulaDebugService`，支持单字段依赖闭包和全量公式调试。
- 修改 `FormulaController`，新增 `POST /api/ai/business/formula/debug`。
- 修改 `FormulaExecutionEngine`，为 CONDITIONAL trace step 补充分支 metadata。
- 新增 `FormulaDebugServiceTest`，扩展 `FormulaControllerTest`。

### 执行命令与结果

- `rg -n "new FormulaController\\(" ...`
  - 仅发现 `FormulaControllerTest` 一处直接构造器调用，已同步补充 `FormulaDebugService`。
- `rg -n "[ \\t]+$" FormulaController.java FormulaDebugService.java FormulaExecutionEngine.java FormulaDebugRequest.java FormulaDebugResponse.java FormulaDebugServiceTest.java FormulaControllerTest.java`
  - 无输出，未发现行尾空白。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaDebugServiceTest,FormulaControllerTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "...$(cat /private/tmp/forge_generator_cp.txt)" FormulaDebugServiceTest.java FormulaControllerTest.java`
  - 编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "...$(cat /private/tmp/forge_generator_cp.txt)" /private/tmp/FormulaDebugServiceSmoke.java`
  - 编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... java -cp "/private/tmp/forge_formula_test_classes:...$(cat /private/tmp/forge_generator_cp.txt)" FormulaDebugServiceSmoke`
  - 输出 `FormulaDebugServiceSmoke PASS`。
  - 失败公式路径输出 ERROR 日志且包含 `traceId=FML-...`，符合调试失败可追踪预期。

### 结论

- Task 5 目标模块编译通过。
- 行为 smoke 覆盖：单字段调试自动执行公式依赖闭包；条件公式返回分支 metadata；失败调试返回 traceId；成功和失败调试均写入执行日志服务；Controller `/debug` 返回 `RespInfo.success(data)`。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已用临时 runner 补充行为验证。
- 未启动后端服务和数据库；真实 HTTP、权限和前端交互留到 Task 7 与 Task 19 验证。
- LOOKUP / 跨对象公式尚未实现，本轮调试器只覆盖 CALC / AGGREGATE / CONDITIONAL。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-13 21:39:01 CST - Task 4 执行日志查询 API

### 变更范围

- 修改 `FormulaController`，注入 `FormulaExecutionLogService`。
- 新增 `GET /api/ai/business/formula/log/page`，支持 `pageNum`、`pageSize`、`objectCode`、`recordId`、`fieldCode`、`success`、`traceId`、`beginTime`、`endTime`。
- 新增 `GET /api/ai/business/formula/log/{id}`。
- 新增 `FormulaExecutionLogDetailResponse`。
- 扩展 `FormulaControllerTest`，覆盖执行日志分页和详情接口。

### 执行命令与结果

- `rg -n "new FormulaController\\(" ...`
  - 仅发现 `FormulaControllerTest` 一处直接构造器调用，已同步补充 `FormulaExecutionLogService`。
- `rg -n "[ \\t]+$" FormulaController.java FormulaExecutionLogDetailResponse.java FormulaControllerTest.java`
  - 无输出，未发现行尾空白。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaControllerTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `javac -d /private/tmp/forge_formula_test_classes -cp "...$(cat /private/tmp/forge_generator_cp.txt)" FormulaControllerTest.java`
  - 首次失败，系统默认 JDK 8 无法读取 Java 17 class：`类文件具有错误的版本 61.0, 应为 52.0`。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "...$(cat /private/tmp/forge_generator_cp.txt)" FormulaControllerTest.java`
  - 编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "...$(cat /private/tmp/forge_generator_cp.txt)" /private/tmp/FormulaControllerLogSmoke.java`
  - 编译通过，只有既有弃用 API 提示。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... java -cp "/private/tmp/forge_formula_test_classes:...$(cat /private/tmp/forge_generator_cp.txt)" FormulaControllerLogSmoke`
  - 输出 `FormulaControllerLogSmoke PASS`。

### 结论

- Task 4 目标模块编译通过。
- Controller smoke 覆盖：分页接口按 traceId、objectCode、recordId、fieldCode、success、时间范围组装查询 DTO；详情接口返回扁平详情 DTO；inputSnapshot 透传已脱敏内容。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已用临时 runner 补充行为验证。
- 未启动后端服务和数据库；本轮只完成 Controller 层查询 API，真实 HTTP、鉴权和权限资源接入留到后续前端组件和安全加固任务。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-13 21:31:05 CST - Task 3 运行时接入 Trace 与执行日志

### 变更范围

- 修改 `FormulaExecutionEngine`，新增 `execute(formulaMap, originalValues, FormulaTraceOptions)`，生成 traceId、executionPlan、steps、errors。
- 修改 `FormulaErrorHandler`，支持带 traceId 的 ERROR 日志。
- 修改 `AbstractFormulaRuntime`，默认生成轻量 traceId，失败时记录执行日志，debugMode 成功也记录执行日志。
- 修改 `StoredFormulaRuntime`、`VirtualFormulaRuntime`，增加可注入 `FormulaExecutionLogService` 的构造器，同时保留原两参构造器兼容测试和旧调用。
- 新增 `FormulaExecutionEngineTraceTest`、`FormulaRuntimeTraceLoggingTest`。

### 执行命令与结果

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaExecutionEngineTest,StoredFormulaRuntimeTest,VirtualFormulaRuntimeTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `javac -cp "forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" /private/tmp/FormulaRuntimeTraceSmoke.java`
  - 编译通过，只有既有弃用 API 提示。
- `java -cp "/private/tmp:forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaRuntimeTraceSmoke`
  - 输出 `FormulaRuntimeTraceSmoke PASS`。
  - 执行失败公式时控制台 ERROR 日志包含 `traceId=FML-...`。
- `javac -d /private/tmp/forge_formula_test_classes -cp "forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" ...`
  - 本轮新增测试源码编译通过，只有既有弃用 API 提示。

### 结论

- Task 3 目标模块编译通过。
- 行为 smoke 覆盖：引擎 debug trace 捕获输入/输出；失败 STORED 公式落执行日志并带 traceId；debugMode 成功 STORED/VIRTUAL 公式落执行日志；错误日志包含 traceId。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已记录限制并用临时 runner 补充行为验证。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-13 21:20:10 CST - Task 2 公式执行 Trace 数据结构

### 变更范围

- 新增 `FormulaExecutionTrace`、`FormulaExecutionStep`、`FormulaTraceOptions`。
- 修改 `ExecutionResult`，新增可空 `FormulaExecutionTrace trace`、`getTrace()` 和 `hasTrace()`。
- 新增 `FormulaExecutionTraceTest`、`ExecutionResultTest`。

### 执行命令与结果

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaExecutionTraceTest,ExecutionResultTest -DskipTests=false -Dmaven.test.skip=false`
  - `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 父 POM 固定跳过测试，日志仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为单测通过证据。
- `javac -cp "forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes" /private/tmp/FormulaExecutionTraceSmoke.java`
  - 编译通过。
- `java -cp "/private/tmp:forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes" FormulaExecutionTraceSmoke`
  - 输出 `FormulaExecutionTraceSmoke PASS`。

### 结论

- Task 2 目标模块编译通过。
- Trace 行为通过 smoke 验证：默认 options 禁用、debug options 自动开启输入/输出、step/trace 集合不可变、失败 step 标记 trace 失败、`ExecutionResult` 默认不带 trace 且可显式挂载 trace。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已记录限制并用临时 runner 补充行为验证。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 12:36:06 CST - Task 19 安全、权限、脱敏与配置开关 / Task 20 自动化测试与执行日志回填

### 变更范围

- 新增 `FormulaValueMasker`，统一处理手机号、身份证、银行卡、Token、AK/SK、Secret、Password 等敏感值脱敏。
- 新增 `FormulaRuntimeProperties`，提供 `forge.formula.runtime` 配置项：执行日志总开关、失败/成功/debug 日志开关、debug 输入/输出快照开关和函数 timeout。
- 修改 `FormulaExecutionLogService`，执行日志写入前统一脱敏，并支持执行日志总开关。
- 修改 `FormulaDebugService`、`AbstractFormulaRuntime`、`StoredFormulaRuntime`、`VirtualFormulaRuntime`，调试日志、失败日志和成功日志按运行时配置记录，默认仍只记录失败和 debug 日志。
- 修改 `FormulaFunctionInvoker`，函数执行 timeout 由全局配置和函数定义共同约束。
- 修改 `FormulaController`，类级增加 `@SaCheckPermission("ai:businessObject:design")`，公式调试、执行日志、依赖图、规则编译和函数市场接口复用对象设计权限。
- 修改 `FormulaLookupResolver`、`FormulaCrossObjectResolver`、`FormulaValidationService`，拒绝 SQL 片段、非法字段编码、路径式 LOOKUP relationCode；LOOKUP 目标/返回字段必须来自目标模型字段资产或 `id`。
- 新增 `FormulaValueMaskerTest`，扩展 `FormulaExecutionLogServiceTest`、`FormulaFunctionRegistryTest`、`FormulaValidationServiceTest`。
- 回填 `tasks.md` 和 `test-spec.md` 的 Task 19/20 状态与验证计划。

### 执行命令与结果

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaValueMaskerTest,FormulaExecutionLogServiceTest,FormulaFunctionRegistryTest,FormulaValidationServiceTest -DskipTests=false -Dmaven.test.skip=false`
  - 首次执行 `BUILD SUCCESS`，generator 模块主代码编译通过。
  - Maven 仍显示 `Not compiling test sources` 与 `Tests are skipped`，不作为 JUnit/Surefire 通过证据。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaValueMaskerTest.java FormulaExecutionLogServiceTest.java FormulaFunctionRegistryTest.java FormulaValidationServiceTest.java`
  - 首次失败：`FormulaFunctionInvoker` 缺少 `Map<String,Object> + FormulaRuntimeProperties` 兼容构造器。
  - 已补齐构造器。
- `JAVA_HOME=... PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaValueMaskerTest,FormulaExecutionLogServiceTest,FormulaFunctionRegistryTest,FormulaValidationServiceTest -DskipTests=false -Dmaven.test.skip=false`
  - 复跑 `BUILD SUCCESS`，generator 模块主代码编译通过。
  - 仍受父 POM 固定 skip 限制，不执行真实 JUnit/Surefire。
- `JAVA_HOME=... PATH=... javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaValueMaskerTest.java FormulaExecutionLogServiceTest.java FormulaFunctionRegistryTest.java FormulaValidationServiceTest.java /private/tmp/FormulaSecurityHardeningSmoke.java`
  - 编译通过。
- `JAVA_HOME=... PATH=... java -cp "/private/tmp/forge_formula_test_classes:forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaSecurityHardeningSmoke`
  - 首次失败：smoke 使用 path/returnField 不一致构造 `CrossObjectConfig`，领域构造器已提前阻断，错误为 `returnField must match path target field`。
  - 已调整为 SQL 片段校验场景后复跑。
  - 复跑输出 `FormulaSecurityHardeningSmoke PASS`。
- `rg -n "[ \t]+$" <Task 19 相关 Java 文件>`
  - 无输出，未发现行尾空白。
- `git diff --check -- <Task 19 相关 Java 文件>`
  - 无输出，diff 空白检查通过。

### 结论

- Task 19 目标模块主代码编译通过。
- 测试源码编译覆盖 `FormulaValueMaskerTest`、`FormulaExecutionLogServiceTest`、`FormulaFunctionRegistryTest`、`FormulaValidationServiceTest`。
- Smoke 覆盖：敏感 JSON key 和手机号脱敏；函数 timeout 按全局配置返回明确错误并通过 STORED runtime 写入失败执行日志；LOOKUP SQL 片段被校验阻断；跨对象 SQL 片段被校验阻断；LOOKUP resolver 在 provider 前拒绝路径式 relationCode。
- Task 20 文档回填完成，`tasks.md` 状态已更新为 completed，`test-spec.md` 已追加本轮增量验证矩阵。

### 跳过项

- 未执行真实 JUnit/Surefire；父 POM 固定跳过 testCompile 和 surefire，本轮已用测试源码编译和临时 runner 补充行为验证。
- 未启动后端 admin 服务和数据库；`@SaCheckPermission` 真实 HTTP 权限拦截、函数市场操作日志入库、Flyway 实跑留到服务级验收环境覆盖。
- 未执行前端构建；本轮只涉及后端安全加固和变更文档回填，前端构建已在 Task 18 完成。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 15:50:24 CST - 字段资产与公式配置体验优化

### 变更范围

- 修改 `BusinessFieldManager.vue`，将字段资产右侧长属性面板改为轻量字段摘要区，字段详细配置进入大弹窗工作台。
- 修改 `BusinessFieldPropertyPanel.vue`，将字段属性配置拆为“基础属性 / 显示与校验 / 开发者属性 / 公式与调试”标签页，避免全部配置堆叠在一个纵向面板。
- 修改 `FormulaFunctionBrowser.vue`，改为左侧函数列表、右侧详情预览、参数 Schema、返回类型和明确“插入函数”按钮。
- 修改 `FormulaDependencyGraph.vue`，从节点网格 + 边列表改为 SVG 依赖图，包含节点、箭头连线、边标签、缩放和循环路径高亮。
- 修改 `FormulaFunctionMarket.vue`，增加“注册函数”入口；首期只支持 Java Bean 元数据注册，不开放任意脚本执行。
- 修改 `formula.js`、`FormulaController`、`FormulaFunctionMarketService`、`FormulaExecutionEngine`、`FormulaFunctionMapper.xml` 等，补齐自定义函数注册 API、函数/版本 upsert、已安装函数运行时同步和函数详情字段。

### 执行命令与结果

- `rg -n "NSegmented|n-segmented" forge-admin-ui/src/views/app-center/components/designer forge-admin-ui/src/views/app-center/components/designer/formula`
  - 无输出，未使用当前项目兼容性较弱的 `NSegmented`。
- `rg -n "[ \t]+$" <本轮相关前后端文件>`
  - 无输出，未发现行尾空白。
- `git diff --check -- <本轮相关已跟踪文件>`
  - 无输出，diff 空白检查通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/api/formula.js src/views/app-center/components/designer/BusinessFieldManager.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/views/app-center/components/designer/formula/FormulaFunctionBrowser.vue src/views/app-center/components/designer/formula/FormulaDependencyGraph.vue src/views/app-center/components/designer/formula/FormulaFunctionMarket.vue`
  - 首次失败：`FormulaDependencyGraph.vue` 的 `defineProps` 宏顺序和 CSS background 格式不符合规则，`FormulaFunctionBrowser.vue` 有单行 `<p>` 警告。
  - 已修复后复跑通过，无错误。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - `BUILD SUCCESS`，`forge-plugin-generator` 及依赖模块编译通过。
  - Maven 输出既有 deprecated/unchecked 提示，不阻断。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 字段选择交互调整后最终复跑构建成功，输出 `✓ built in 1m 6s`。
  - Vite 仍输出既有 CSS `//` 注释 warning 和 `src/store/index.js` 动态/静态混合导入 warning，不阻断本轮构建。

### 结论

- 字段资产配置交互已从“右侧长面板”调整为“字段摘要 + 大弹窗配置工作台”。
- 函数浏览器详情可读性提升，函数插入动作与函数浏览动作分离。
- 公式依赖图已按后端 nodes / edges 绘制为真正的 SVG 图形结构。
- 函数市场新增自定义 Java Bean 函数注册链路，后端支持函数主表、版本表、安装表 upsert，并将已安装定义同步到公式运行时注册中心。
- 前端 eslint、前端生产构建、后端 generator 编译均通过。

### 跳过项

- 未启动后端 admin 服务和数据库；函数注册真实 HTTP、MySQL upsert、操作日志入库和权限拦截需在服务级验收环境覆盖。
- 未启动 Vite dev server 做浏览器截图；本轮通过 eslint 和生产构建覆盖 Vue 模板解析、组件解析和打包路径。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 17:38:38 CST - 表达式工作台与执行日志分页优化

### 变更范围

- 修改 `FormulaExpressionEditor.vue`，将表达式配置从 textarea + toolbar + 折叠函数浏览器改为三栏工作台：左侧字段浏览、中间表达式编辑、右侧函数树和函数详情。
- 字段浏览支持搜索和分组折叠，点击字段会插入到表达式当前光标位置，并同步 `formulaDependsOn` 依赖字段 chips。
- 函数浏览支持搜索、树形分类折叠、函数签名/说明/示例直接展示，点击函数自动插入表达式当前光标位置。
- 修改 `FormulaConfigPanel.vue`，移除旧的依赖字段多选重复配置，条件表达式复用同一套表达式工作台。
- 修改 `FormulaExecutionLogDrawer.vue`，执行日志分页从固定 10 条增强为页大小选择、快速跳页、总量和当前范围显示。

### 执行命令与结果

- `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExecutionLogDrawer.vue`
  - 无输出，未发现行尾空白。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExecutionLogDrawer.vue`
  - 无输出，diff 空白检查通过。
  - 备注：当前 `forge-admin-ui/src/views/app-center/components/designer/formula/` 仍整体处于未跟踪状态，`git diff --check` 只对已跟踪 diff 有输出；本轮已额外用 `rg` 覆盖实际文件行尾空白。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue src/views/app-center/components/designer/formula/FormulaConfigPanel.vue src/views/app-center/components/designer/formula/FormulaExecutionLogDrawer.vue`
  - 通过，无 eslint 错误。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 构建成功，输出 `✓ built in 1m 11s`。
  - 非阻断 warning：既有 CSS `//` 注释触发 esbuild `js-comment-in-css` warning；`src/store/index.js` 动态/静态混合导入触发 chunk warning，均非本轮新增阻断。

### 结论

- 表达式配置已调整为参考图方向的工作台布局，字段区、编辑区、函数区职责清晰。
- 字段和函数点击会直接填充表达式输入位置，依赖字段以 chips 形式在编辑器底部展示，不再依赖额外多选框。
- 执行日志分页在 UI 上可见且可操作，支持页大小切换和快速跳页。
- 前端 eslint 和生产构建均通过。

### 跳过项

- 未启动后端 admin 服务和数据库；函数树真实数据、执行日志分页真实 HTTP 返回和权限拦截留到服务级验收环境覆盖。
- 未启动 Vite dev server 做浏览器截图；本轮通过 eslint 和生产构建覆盖 Vue 模板解析、组件自动导入、样式处理和打包路径。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 17:51:18 CST - 公式与调试一屏化优化

### 变更范围

- 修改 `FormulaConfigPanel.vue`，将公式名称、公式类型、触发方式、保存、校验、预览、调试、日志、依赖入口合并为顶部紧凑命令栏。
- 修改 `FormulaConfigPanel.vue`，压缩公式卡片边距、表单控件高度、聚合配置和条件配置布局，减少“公式与调试”标签页纵向滚动。
- 修改 `FormulaExpressionEditor.vue`，新增 `compact` 模式，压缩表达式工作台标题、字段项、函数项、编辑区和依赖 chips 高度。
- 表达式编辑器在紧凑模式下使用面板内滚动的字段列表和函数树，避免撑高整个页面。

### 执行命令与结果

- `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue`
  - 无输出，未发现行尾空白。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue`
  - 无输出，diff 空白检查通过。
  - 备注：当前 `forge-admin-ui/src/views/app-center/components/designer/formula/` 仍整体处于未跟踪状态，`git diff --check` 只对已跟踪 diff 有输出；本轮已额外用 `rg` 覆盖实际文件行尾空白。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/FormulaConfigPanel.vue src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue`
  - 通过，无 eslint 错误。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 构建成功，输出 `✓ built in 56.41s`。
  - 非阻断 warning：既有 CSS `//` 注释触发 esbuild `js-comment-in-css` warning；`src/store/index.js` 动态/静态混合导入触发 chunk warning，均非本轮新增阻断。

### 结论

- “公式与调试”标签页已从纵向堆叠调整为顶部命令栏 + 紧凑表达式工作台，默认视口内可见内容更多。
- 字段和函数列表改为区域内滚动，减少整页滚动。
- 前端 eslint 和生产构建均通过。

### 跳过项

- 未启动后端 admin 服务和数据库；本轮仅涉及前端布局密度与组件结构调整。
- 未启动 Vite dev server 做浏览器截图；本轮通过 eslint 和生产构建覆盖 Vue 模板解析、组件自动导入、样式处理和打包路径。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 18:15:42 CST - 字段浏览面板等高撑满修复

### 变更范围

- 根据用户截图定位 `公式与调试 > 表达式编辑器` 左侧“字段浏览”面板没有跟随中间编辑区撑满高度的问题。
- 修改 `FormulaExpressionEditor.vue`，表达式三栏网格增加显式 `align-items: stretch`。
- 修改 `FormulaExpressionEditor.vue`，字段浏览、表达式编辑、函数浏览三栏统一设置 `height: 100%`、`align-self: stretch` 和 `box-sizing: border-box`。
- 修改 `FormulaExpressionEditor.vue`，左侧字段面板使用独立 `auto auto minmax(0, 1fr)` 行结构，字段列表滚动区占满剩余高度，避免底部露出外层背景。

### 执行命令与结果

- `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue`
  - 无输出，未发现行尾空白。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue`
  - 无输出，diff 空白检查通过。
  - 备注：当前 `forge-admin-ui/src/views/app-center/components/designer/formula/` 仍整体处于未跟踪状态，`git diff --check` 只对已跟踪 diff 有输出；本轮已额外用 `rg` 覆盖实际文件行尾空白。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue src/views/app-center/components/designer/formula/FormulaConfigPanel.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/views/app-center/components/designer/BusinessFieldManager.vue`
  - 通过，无 eslint 错误。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 构建成功，输出 `✓ built in 1m 10s`。
  - 非阻断 warning：既有 CSS `//` 注释触发 esbuild `js-comment-in-css` warning；`src/store/index.js` 动态/静态混合导入触发 chunk warning，均非本轮新增阻断。

### 结论

- 左侧字段浏览面板已改为与表达式编辑区、函数浏览区等高拉伸，字段列表滚动区会填满剩余高度。
- 前端 eslint 和生产构建均通过。

### 跳过项

- 本轮未启动后端 admin 服务和数据库；仅涉及前端 CSS 布局约束调整。
- 未完成 Playwright 截图复核；用户提供真实页面截图后，本轮按截图定位做针对性布局修复。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 18:47:58 CST - 公式设置背景扁平化与字段中文名展示

### 变更范围

- 根据用户反馈继续修复 `公式与调试 > 公式设置` 的外层背景未撑满和层级感问题。
- 修改 `FormulaConfigPanel.vue`，去除 `formula-setting-card` 的灰底、边框、圆角和外层内边距，公式设置改为铺满公式 tab 的白色工作区。
- 修改 `FormulaExpressionEditor.vue`，去除表达式工作台外壳灰底、边框和内边距，只保留字段浏览、表达式编辑、函数浏览三列内部面板边界。
- 修改 `BusinessFieldPropertyPanel.vue`，将属性 body、tabs pane wrapper、公式 tab form 背景统一为白色，避免浅灰底在公式设置边缘露出。
- 修改 `FormulaExpressionEditor.vue`，字段浏览项改为第一行展示字段中文名、第二行展示字段编码，compact 模式不再隐藏字段副信息。

### 执行命令与结果

- `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue`
  - 无输出，未发现行尾空白。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue`
  - 无输出，diff 空白检查通过。
  - 备注：当前 `forge-admin-ui/src/views/app-center/components/designer/formula/` 仍整体处于未跟踪状态，`git diff --check` 只对已跟踪 diff 有输出；本轮已额外用 `rg` 覆盖实际文件行尾空白。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue src/views/app-center/components/designer/formula/FormulaConfigPanel.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/views/app-center/components/designer/BusinessFieldManager.vue`
  - 通过，无 eslint 错误。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 构建成功，输出 `✓ built in 1m 4s`。
  - 非阻断 warning：既有 CSS `//` 注释触发 esbuild `js-comment-in-css` warning；`src/store/index.js` 动态/静态混合导入触发 chunk warning，均非本轮新增阻断。

### 结论

- 公式设置区域已从灰底外层卡片调整为白色铺满工作区，表达式工作台外壳层级已拍平。
- 字段浏览列表现在优先展示字段中文名，并保留字段编码作为第二行信息。
- 前端 eslint 和生产构建均通过。

### 跳过项

- 本轮未启动后端 admin 服务和数据库；仅涉及前端布局和字段展示调整。
- 未完成 Playwright 截图复核；本轮基于用户真实页面反馈做针对性修复。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。

## 2026-06-14 19:15:30 CST - 移除公式设置卡片 active 类

### 变更范围

- 根据用户进一步定位，移除 `FormulaConfigPanel.vue` 模板上的 `:class="{ active: formulaEnabled }"`，不再生成 `class="formula-setting-card active"`。
- 将 `formula-setting-card` 外层类名改为 `formula-setting-workspace`，避免继续保留卡片语义和旧样式覆盖点。
- 删除 `.formula-setting-card.active` 样式块，公式启用状态仅用于开关和 `v-if` 逻辑，不再参与外层视觉布局。

### 执行命令与结果

- `rg -n "formula-setting-card|formula-setting-workspace|\\.active" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue`
  - 仅输出 `formula-setting-workspace`，确认旧类名和 `.active` 样式已移除。
- `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue`
  - 无输出，未发现行尾空白。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue`
  - 无输出，diff 空白检查通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/FormulaConfigPanel.vue`
  - 通过，无 eslint 错误。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 构建成功，输出 `✓ built in 1m 20s`。
  - 非阻断 warning：既有 CSS `//` 注释触发 esbuild `js-comment-in-css` warning；`src/store/index.js` 动态/静态混合导入触发 chunk warning，均非本轮新增阻断。

### 结论

- `formula-setting-card active` 已从 DOM 生成路径和 scoped CSS 中移除。
- 前端 eslint 和生产构建均通过。

### 跳过项

- 本轮未启动后端 admin 服务和数据库；仅涉及前端模板类名和样式选择器调整。

### 服务清理

- 本轮未启动常驻服务，无需停止进程。
