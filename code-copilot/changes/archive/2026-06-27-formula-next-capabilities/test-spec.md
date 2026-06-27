# 测试计划：formula-next-capabilities

## 本轮增量验证（Task 1）

### 变更范围

- 新增公式执行日志表 `ai_formula_execution_log`。
- 新增执行日志实体、DTO、Mapper XML、Service。
- 新增 `FormulaExecutionLogServiceTest` 覆盖日志落库、脱敏、分页查询和详情查询。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 公式执行日志服务单测 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaExecutionLogServiceTest` | 单测通过，目标模块测试编译通过 |
| 公式执行日志服务行为 smoke | `javac ... /private/tmp/FormulaExecutionLogServiceSmoke.java` + `java ... FormulaExecutionLogServiceSmoke` | 输出 `FormulaExecutionLogServiceSmoke PASS` |
| SQL 防重复检查 | 人工检查 `V1.0.69__add_formula_observability_and_function_market.sql` | 使用 `CREATE TABLE IF NOT EXISTS`，索引内联，`tenant_id` 默认 `1` |

### 跳过项

- 本轮不启动后端服务，不验证接口；执行日志查询 API 属于 Task 4。
- 本轮不验证 LOOKUP、跨对象公式和函数市场表；这些属于后续任务。
- Maven 单测目标受父 POM 固定 `maven-compiler-plugin <skip>true</skip>` 与 `maven-surefire-plugin <skipTests>true</skipTests>` 影响，命令行参数无法触发实际 testCompile/surefire，本轮用临时 Java runner 覆盖 Service 行为。

## 本轮增量验证（Task 2）

### 变更范围

- 新增公式执行 Trace 领域对象：`FormulaExecutionTrace`、`FormulaExecutionStep`、`FormulaTraceOptions`。
- `ExecutionResult` 增加可空 `trace` 字段，保持默认构建不带 trace。
- 新增 Trace 与 `ExecutionResult` 测试文件。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| Trace 数据结构单测目标 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaExecutionTraceTest,ExecutionResultTest` | 目标模块主代码编译通过 |
| Trace 数据结构行为 smoke | `javac ... /private/tmp/FormulaExecutionTraceSmoke.java` + `java ... FormulaExecutionTraceSmoke` | 输出 `FormulaExecutionTraceSmoke PASS` |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用临时 Java runner 验证不可变集合、debug options、失败 trace 和 `ExecutionResult` 默认兼容行为。

## 本轮增量验证（Task 3）

### 变更范围

- `FormulaExecutionEngine` 新增带 `FormulaTraceOptions` 的重载执行方法。
- `FormulaErrorHandler` 支持带 traceId 的错误日志。
- `AbstractFormulaRuntime` 接入 trace 执行、失败日志记录和 debug 成功日志记录。
- `StoredFormulaRuntime`、`VirtualFormulaRuntime` 增加可注入 `FormulaExecutionLogService` 的构造器。
- 新增 `FormulaExecutionEngineTraceTest`、`FormulaRuntimeTraceLoggingTest`。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| Runtime trace 目标命令 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaExecutionEngineTest,StoredFormulaRuntimeTest,VirtualFormulaRuntimeTest` | 目标模块主代码编译通过 |
| Runtime trace 行为 smoke | `javac ... /private/tmp/FormulaRuntimeTraceSmoke.java` + `java ... FormulaRuntimeTraceSmoke` | 输出 `FormulaRuntimeTraceSmoke PASS`，错误日志包含 `traceId=` |
| 本轮新增测试源码编译 | `javac -d /private/tmp/forge_formula_test_classes ... FormulaExecutionEngineTraceTest.java FormulaRuntimeTraceLoggingTest.java ...` | 编译通过 |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用临时 Java runner 验证引擎 trace、runtime 失败落日志、debug 成功落日志和虚拟公式 debug 日志。

## 本轮增量验证（Task 4）

### 变更范围

- `FormulaController` 新增执行日志分页和详情接口。
- 新增 `FormulaExecutionLogDetailResponse`。
- 扩展 `FormulaControllerTest`，覆盖日志查询参数绑定和详情返回。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| Controller 目标命令 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaControllerTest -DskipTests=false -Dmaven.test.skip=false` | 目标模块主代码编译通过 |
| Controller 测试源码编译 | `javac -d /private/tmp/forge_formula_test_classes ... FormulaControllerTest.java` | 测试源码编译通过 |
| Controller 日志接口 smoke | `javac ... /private/tmp/FormulaControllerLogSmoke.java` + `java ... FormulaControllerLogSmoke` | 输出 `FormulaControllerLogSmoke PASS` |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用临时 Java runner 验证分页过滤、traceId 查询、详情返回和脱敏快照透传。
- 本轮不启动后端服务和数据库；真实 HTTP/API 权限接入留到后续调试器、前端组件和安全加固任务统一验证。

## 本轮增量验证（Task 5）

### 变更范围

- 新增公式调试请求/响应 DTO：`FormulaDebugRequest`、`FormulaDebugResponse`。
- 新增 `FormulaDebugService`，封装 debug trace、单字段依赖闭包、调试日志记录。
- `FormulaController` 新增 `POST /api/ai/business/formula/debug`。
- `FormulaExecutionEngine` 的 CONDITIONAL trace step metadata 增加条件结果和分支值。
- 新增 `FormulaDebugServiceTest`，扩展 `FormulaControllerTest`。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| Debug API 目标命令 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaDebugServiceTest,FormulaControllerTest -DskipTests=false -Dmaven.test.skip=false` | 目标模块主代码编译通过 |
| Debug 测试源码编译 | `javac -d /private/tmp/forge_formula_test_classes ... FormulaDebugServiceTest.java FormulaControllerTest.java` | 测试源码编译通过 |
| Debug 行为 smoke | `javac ... /private/tmp/FormulaDebugServiceSmoke.java` + `java ... FormulaDebugServiceSmoke` | 输出 `FormulaDebugServiceSmoke PASS` |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用临时 Java runner 验证调试服务关键行为。
- 本轮不启动后端服务和数据库；真实 HTTP、权限和前端交互留到 Task 7 与 Task 19 验证。
- LOOKUP / 跨对象公式尚未实现，调试器本轮只覆盖 CALC / AGGREGATE / CONDITIONAL。

## 本轮增量验证（Task 6）

### 变更范围

- 新增依赖图请求/响应 DTO：`FormulaDependencyGraphRequest`、`FormulaDependencyGraphResponse`、`FormulaDependencyGraphNode`、`FormulaDependencyGraphEdge`。
- 新增 `FormulaDependencyGraphService`，将公式 DAG 转换为 nodes / edges。
- `FormulaController` 新增 `POST /api/ai/business/formula/dependency/graph`。
- 新增 `FormulaDependencyGraphServiceTest`，扩展 `FormulaControllerTest`。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| Dependency Graph 目标命令 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaDependencyGraphServiceTest,FormulaControllerTest -DskipTests=false -Dmaven.test.skip=false` | 目标模块主代码编译通过 |
| Dependency Graph 测试源码编译 | `javac -d /private/tmp/forge_formula_test_classes ... FormulaDependencyGraphServiceTest.java FormulaControllerTest.java` | 测试源码编译通过 |
| Dependency Graph 行为 smoke | `javac ... /private/tmp/FormulaDependencyGraphSmoke.java` + `java ... FormulaDependencyGraphSmoke` | 输出 `FormulaDependencyGraphSmoke PASS` |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用临时 Java runner 验证图结构关键行为。
- 本轮不启动后端服务和数据库；真实 HTTP 和前端图展示留到 Task 7 验证。
- LOOKUP / 跨对象公式尚未实现，本轮图结构只输出对象内依赖和聚合关系边。

## 本轮增量验证（Task 7）

### 变更范围

- `formula.js` 新增调试、日志查询、依赖图 API 封装。
- 新增 `FormulaDebuggerPanel`、`FormulaExecutionLogDrawer`、`FormulaDependencyGraph`、`FormulaTraceSteps`。
- `BusinessFieldPropertyPanel` 新增公式可观测性入口，并将当前未保存公式配置合并给调试器和依赖图。
- `BusinessFieldManager` 与对象设计页补充 `objectCode` 透传。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" forge-admin-ui/src/api/formula.js forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' forge-admin-ui/src/views/app-center/components/designer/formula` | 无输出 |
| Git diff 空白检查 | `git diff --check -- forge-admin-ui/src/api/formula.js forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' forge-admin-ui/src/views/app-center/components/designer/formula` | 无输出 |
| 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建成功 |
| 页面加载冒烟 | 启动 Vite 后执行 `python3 /private/tmp/forge_formula_ui_smoke.py` | 输出 `forge_formula_ui_smoke PASS` |

### 跳过项

- 本轮未启动后端 admin 服务和数据库，公式调试、执行日志、依赖图真实接口点击联调未覆盖。
- Playwright 冒烟验证停在登录页；登录配置、三方登录、验证码接口因后端未启动返回 500，已作为本轮环境边界排除。

## 本轮增量验证（Task 8）

### 变更范围

- `FormulaType` 新增 `LOOKUP`。
- `FormulaConfig` 新增 `lookup`、`crossObject`、`rule`、`functionRefs`。
- 新增 `LookupConfig`、`CrossObjectConfig`、`CrossObjectRecomputeMode`。
- `AggregateConfig`、`ConditionConfig` 补充 Jackson 构造注解，保证旧配置 JSON 兼容。
- `AbstractFormulaRuntime`、`FormulaPublishValidator` 支持解析 LOOKUP / crossObject 元数据。
- `BusinessFieldDTO`、`BusinessFieldVO` 保持 `formulaConfig` JSON 透传并更新注释。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" <Task 8 相关 Java 文件>` | 无输出 |
| Git diff 空白检查 | `git diff --check -- <Task 8 相关 Java 文件>` | 无输出 |
| FormulaConfig 目标命令 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaConfigTest -DskipTests=false -Dmaven.test.skip=false` | 目标模块主代码编译通过 |
| FormulaConfig 测试源码编译 | `javac -d /private/tmp/forge_formula_test_classes ... FormulaConfigTest.java` | 测试源码编译通过 |
| FormulaConfig 行为 smoke | `javac ... /private/tmp/FormulaConfigSmoke.java` + `java ... FormulaConfigSmoke` | 输出 `FormulaConfigSmoke PASS` |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用临时 Java runner 验证领域模型关键行为。
- 本轮不实现 LOOKUP 查询执行、跨对象批量预取和发布期对象图校验，这些分别属于 Task 9-11。

## 本轮增量验证（Task 9）

### 变更范围

- 新增 `FormulaLookupResolver`、`LookupResolveResult`。
- `FormulaExecutionEngine` 接入 LOOKUP 执行和 trace metadata。
- `FormulaValidationService` 支持 LOOKUP 空表达式、sourceField 依赖提示和非法 path-style relationCode 拦截。
- `FormulaDependencyAnalyzer` 将 LOOKUP sourceField 纳入拓扑排序、深度和字段存在性校验。
- `FormulaDebugRequest`、`FormulaDependencyGraphRequest`、`FormulaDebugService`、`FormulaDependencyGraphService` 支持 lookup 配置解析；依赖图输出 LOOKUP 边。
- 新增 `FormulaLookupResolverTest`、`FormulaExecutionEngineLookupTest`，扩展依赖分析、校验服务和依赖图测试。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" <Task 9 相关 Java 文件>` | 无输出 |
| Git diff 空白检查 | `git diff --check -- <Task 9 相关 Java 文件>` | 无输出 |
| LOOKUP 目标命令 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaLookupResolverTest,FormulaExecutionEngineLookupTest,FormulaDependencyAnalyzerTest,FormulaValidationServiceTest,FormulaDependencyGraphServiceTest -DskipTests=false -Dmaven.test.skip=false` | 目标模块主代码编译通过 |
| LOOKUP 测试源码编译 | `javac -d /private/tmp/forge_formula_test_classes ... FormulaLookupResolverTest.java FormulaExecutionEngineLookupTest.java FormulaDependencyAnalyzerTest.java FormulaValidationServiceTest.java FormulaDependencyGraphServiceTest.java` | 测试源码编译通过 |
| LOOKUP 行为 smoke | `javac ... /private/tmp/FormulaLookupSmoke.java` + `java ... FormulaLookupSmoke` | 输出 `FormulaLookupSmoke PASS` |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用临时 Java runner 验证 LOOKUP 行为。
- 本轮不启动后端服务和数据库；数据库关系解析路径通过主代码编译覆盖，真实对象关系/数据权限联调留到后续服务级验证。
- 跨对象批量预取和发布期对象图校验属于 Task 10-11。

## 本轮增量验证（Task 10）

### 变更范围

- 新增 `CrossObjectPath`、`FormulaCrossObjectResolver`、`FormulaReferenceResolver`。
- `VirtualFormulaRuntime` 对 VIRTUAL 跨对象公式执行批量预取，并只回写公式字段。
- `DetailRecordFetcher`、`DynamicDetailRecordFetcher`、`DynamicCrudRepository` 支持目标对象批量 IN 查询。
- `FormulaDebugRequest`、`FormulaDependencyGraphRequest`、`FormulaDebugService`、`FormulaDependencyGraphService` 支持 `crossObject` 元数据；依赖图输出 `CROSS_OBJECT` 边。
- 新增 `FormulaCrossObjectResolverTest`，扩展 `VirtualFormulaRuntimeTest`、`FormulaDependencyGraphServiceTest`。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" <Task 10 相关 Java 文件>` | 无输出 |
| Git diff 空白检查 | `git diff --check -- <Task 10 相关 Java 文件>` | 无输出 |
| 跨对象目标命令 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaCrossObjectResolverTest,VirtualFormulaRuntimeTest,FormulaDependencyGraphServiceTest -DskipTests=false -Dmaven.test.skip=false` | 目标模块主代码编译通过 |
| 跨对象测试源码编译 | `javac -d /private/tmp/forge_formula_test_classes ... FormulaCrossObjectResolverTest.java VirtualFormulaRuntimeTest.java FormulaDependencyGraphServiceTest.java` | 测试源码编译通过 |
| 跨对象行为 smoke | `javac ... /private/tmp/FormulaCrossObjectSmoke.java` + `java ... FormulaCrossObjectSmoke` | 输出 `FormulaCrossObjectSmoke PASS` |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用临时 Java runner 验证跨对象行为。
- 本轮不启动后端服务和数据库；真实对象关系、动态表数据、租户和数据权限联调留到后续服务级验证。
- STORED 跨对象重算策略和对象图发布校验属于 Task 11。

## 本轮增量验证（Task 11）

### 变更范围

- 新增 `FormulaObjectDependencyAnalyzer`，将跨对象公式转换为对象图依赖边，校验关系、目标对象字段并检测对象级循环依赖。
- 新增 `CrossObjectRecomputeTaskService`，为 STORED 跨对象公式生成 `PENDING` 待重算任务和幂等键。
- `FormulaPublishValidator` 新增对象图上下文校验重载，并保留旧入口兼容。
- `BusinessObjectPublishService` 发布检查接入对象图校验，发布成功后生成当前对象跨对象重算任务。
- 新增 `FormulaObjectDependencyAnalyzerTest`，扩展 `FormulaPublishValidatorTest` 和 `BusinessObjectPublishServiceFormulaTest` 构造器。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" <Task 11 相关 Java 文件>` | 无输出 |
| Git diff 空白检查 | `git diff --check -- <Task 11 相关 Java 文件>` | 无输出 |
| 跨对象发布校验目标命令 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaObjectDependencyAnalyzerTest,FormulaPublishValidatorTest -DskipTests=false -Dmaven.test.skip=false` | 目标模块主代码编译通过 |
| Task 11 测试源码编译 | `javac -d /private/tmp/forge_formula_test_classes ... FormulaObjectDependencyAnalyzerTest.java FormulaPublishValidatorTest.java BusinessObjectPublishServiceFormulaTest.java` | 测试源码编译通过 |
| Task 11 行为 smoke | `javac ... /private/tmp/FormulaObjectDependencySmoke.java` + `java ... FormulaObjectDependencySmoke` | 输出 `FormulaObjectDependencySmoke PASS` |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用临时 Java runner 验证对象图校验和重算任务行为。
- 本轮不启动后端服务和数据库；发布检查真实对象套件加载、持久化重算队列和运行态重算消费留到后续服务级验证。

## 本轮增量验证（Task 12）

### 变更范围

- 新增 `FormulaConfigPanel.vue`，从字段属性面板拆出公式配置区域。
- 新增 `FormulaLookupPanel.vue`，支持 LOOKUP 一跳关联字段配置。
- 新增 `FormulaCrossObjectPanel.vue`，支持一跳跨对象路径选择。
- 修改 `BusinessFieldPropertyPanel.vue`，改为使用公式配置组件并保持原有调试、日志和依赖图入口。
- 修改 `FormulaDebuggerPanel.vue`、`FormulaDependencyGraph.vue`，透传 `lookup` 和 `crossObject` 配置。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaLookupPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaCrossObjectPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaDebuggerPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaDependencyGraph.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/api/formula.js` | 无输出 |
| Git diff 空白检查 | `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaLookupPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaCrossObjectPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaDebuggerPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaDependencyGraph.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/api/formula.js` | 无输出 |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/*.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/api/formula.js` | 无错误 |
| 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建成功 |

### 跳过项

- 本轮未启动后端 admin 服务和数据库，未覆盖字段属性面板真实保存接口。
- 本地 preview 冒烟启动时被当前沙箱拦截端口监听：`listen EPERM: operation not permitted 127.0.0.1:4174`，未作为产品缺陷处理。
- LOOKUP / 跨对象真实运行时已由 Task 9-11 后端 smoke 覆盖，本轮只验证前端配置 payload、构建和静态质量。

## 本轮增量验证（Task 13）

### 变更范围

- 新增条件规则 AST：`ConditionRuleNode`。
- 新增规则操作符枚举：`ConditionRuleOperator`。
- 新增 `ConditionRuleCompiler`，负责编译规则 AST、校验字段和值类型、输出 Aviator 表达式。
- 新增 `ConditionRuleCompileRequest`、`ConditionRuleCompileResponse`。
- `FormulaController` 新增 `/rule/compile` 和 `/rule/validate`。
- 新增 `ConditionRuleCompilerTest`，扩展 `FormulaControllerTest`。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" <Task 13 相关 Java 文件>` | 无输出 |
| Git diff 空白检查 | `git diff --check -- <Task 13 相关 Java 文件>` | 无输出 |
| 条件规则目标命令 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=ConditionRuleCompilerTest,FormulaControllerTest -DskipTests=false -Dmaven.test.skip=false` | 目标模块主代码编译通过 |
| Task 13 测试源码编译 | `javac -d /private/tmp/forge_formula_test_classes ... ConditionRuleCompilerTest.java FormulaControllerTest.java` | 测试源码编译通过 |
| Task 13 行为 smoke | `javac ... /private/tmp/ConditionRuleCompilerSmoke.java` + `java ... ConditionRuleCompilerSmoke` | 输出 `ConditionRuleCompilerSmoke PASS` |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用测试源码编译和临时 Java runner 验证条件规则编译与 Controller 行为。
- 本轮不实现条件规则前端设计器；UI 编辑、编译调用和调试器规则节点命中展示属于 Task 14。

## 本轮增量验证（Task 14）

### 变更范围

- `formula.js` 新增 `compileConditionRule`、`validateConditionRule`。
- 新增 `FormulaConditionRuleNode.vue`，实现递归规则节点编辑。
- 新增 `FormulaConditionRuleDesigner.vue`，实现规则设计、表达式快照和后端编译校验。
- 修改 `FormulaConfigPanel.vue`，CONDITIONAL 支持表达式模式和规则设计模式切换。
- 修改 `BusinessFieldPropertyPanel.vue`，保存 rule JSON、表达式快照和规则校验状态。
- 修改 `FormulaDebuggerPanel.vue`，展示规则节点命中状态。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 前端 eslint | `cd forge-admin-ui && source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/formula/*.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/api/formula.js` | 无错误 |
| 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建成功 |
| 行尾空白检查 | `rg -n "[ \t]+$" <Task 14 相关前端文件>` | 无输出 |
| Git diff 空白检查 | `git diff --check -- <Task 14 相关前端文件>` | 无输出 |

### 跳过项

- 本轮未启动后端 admin 服务和数据库，未覆盖真实字段保存接口和登录后页面点击。
- 本地 preview / dev server 在当前沙箱中无法监听 `127.0.0.1` 端口，浏览器点击冒烟沿用 Task 12 的环境边界记录。
- 规则 AST 编译 API 行为已在 Task 13 后端 smoke 覆盖；本轮重点验证前端构建、静态质量和 payload 集成。

## 本轮增量验证（Task 15）

### 变更范围

- `V1.0.69__add_formula_observability_and_function_market.sql` 新增函数注册表、函数版本表、租户安装表和内置函数 seed。
- 新增 `AiFormulaFunction`、`AiFormulaFunctionVersion`、`AiFormulaFunctionInstall` 实体。
- 新增 `FormulaFunctionMapper` 和 `FormulaFunctionMapper.xml`。
- 新增 `FormulaFunctionPersistenceTest`。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 函数持久化目标命令 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaFunctionPersistenceTest -DskipTests=false -Dmaven.test.skip=false` | 目标模块主代码编译通过 |
| Task 15 测试源码编译 | `javac -d /private/tmp/forge_formula_test_classes ... FormulaFunctionPersistenceTest.java` | 测试源码编译通过 |
| Task 15 行为 smoke | `javac ... /private/tmp/FormulaFunctionPersistenceSmoke.java` + `java ... FormulaFunctionPersistenceSmoke` | 输出 `FormulaFunctionPersistenceSmoke PASS` |
| 行尾空白检查 | `rg -n "[ \t]+$" <Task 15 相关 SQL/Java/XML 文件>` | 无输出 |
| Git diff 空白检查 | `git diff --check -- <Task 15 相关 SQL/Java/XML 文件>` | 无输出 |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用测试源码编译和临时 Java runner 验证函数持久化模型关键行为。
- 本轮未连接真实 MySQL，不执行 Flyway 实跑和分页 SQL 查询；迁移脚本通过文本 smoke 覆盖表结构、seed 防重复和 Mapper XML 关键语句。

## 本轮增量验证（Task 16）

### 变更范围

- 新增 `FormulaFunctionDefinition`、`FormulaBuiltinFunctionProvider`、`FormulaFunctionInvoker`、`FormulaFunctionRegistry`。
- 修改 `ExpressionExecutor`，执行前安装注册函数。
- 修改 `FormulaValidationService`，校验表达式函数引用状态。
- 修改 `FormulaExecutionEngine`，Spring 注入路径复用函数注册中心。
- 修改 `FormulaController`，`/functions` 返回注册中心启用函数。
- 新增 `FormulaFunctionRegistryTest`，扩展 `ExpressionExecutorTest`、`FormulaValidationServiceTest`、`FormulaControllerTest`。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 函数注册中心目标命令 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaFunctionRegistryTest,ExpressionExecutorTest,FormulaValidationServiceTest -DskipTests=false -Dmaven.test.skip=false` | 目标模块主代码编译通过 |
| Task 16 测试源码编译 | `javac -d /private/tmp/forge_formula_test_classes ... FormulaFunctionRegistryTest.java ExpressionExecutorTest.java FormulaValidationServiceTest.java FormulaControllerTest.java` | 测试源码编译通过 |
| Task 16 行为 smoke | `javac ... /private/tmp/FormulaFunctionRegistrySmoke.java` + `java ... FormulaFunctionRegistrySmoke` | 输出 `FormulaFunctionRegistrySmoke PASS` |
| 行尾空白检查 | `rg -n "[ \t]+$" <Task 16 相关 Java 文件>` | 无输出 |
| Git diff 空白检查 | `git diff --check -- <Task 16 相关 Java 文件>` | 无输出 |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用测试源码编译和临时 Java runner 验证函数注册中心关键行为。
- 本轮不启动后端服务和数据库；`/functions` 真实 HTTP 调用和市场安装状态联动留到 Task 17/18。

## 本轮增量验证（Task 17）

### 变更范围

- 新增函数市场分页、详情、安装、启用、禁用后端 API。
- `FormulaFunctionMarketService` 负责市场分页、安装状态 upsert、可用函数列表和函数引用市场状态校验。
- `FormulaController.functions()` 改为读取已安装且启用的市场函数。
- `FormulaValidationService` 在 Spring 注入路径下接入市场状态，禁用或未启用函数会阻断公式校验。
- `V1.0.69__add_formula_observability_and_function_market.sql` 补齐全部内置函数种子。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" <Task 17 相关 Java/XML/SQL 文件>` | 无输出 |
| Git diff 空白检查 | `git diff --check -- <Task 17 相关 Java/XML/SQL 文件>` | 无输出 |
| 函数市场目标命令 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaFunctionMarketServiceTest,FormulaControllerTest,FormulaValidationServiceTest -DskipTests=false -Dmaven.test.skip=false` | 目标模块主代码编译通过 |
| Task 17 测试源码编译 | `javac -d /private/tmp/forge_formula_test_classes ... FormulaFunctionMarketServiceTest.java FormulaControllerTest.java FormulaValidationServiceTest.java` | 编译通过 |
| Task 17 行为 smoke | `javac ... /private/tmp/FormulaFunctionMarketSmoke.java` + `java ... FormulaFunctionMarketSmoke` | 输出 `FormulaFunctionMarketSmoke PASS` |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用临时 Java runner 验证函数市场关键行为。
- 本轮不启动后端服务和数据库；真实 HTTP、鉴权、操作日志入库和 Flyway 实跑留到 Task 19-20 总体验证。

## 本轮增量验证（Task 18）

### 变更范围

- `formula.js` 新增函数市场分页、详情、安装、启用、禁用 API。
- 新增 `FormulaFunctionBrowser.vue`，从后端动态读取可用函数并支持搜索、分类筛选、插入表达式。
- 新增 `FormulaExpressionEditor.vue`，替换 `FormulaConfigPanel.vue` 原静态函数下拉。
- 新增 `FormulaFunctionMarket.vue`，提供函数市场分页、详情、安装、启用、禁用弹窗。
- 修改 `BusinessFieldPropertyPanel.vue`，保存 `formulaConfig.functionRefs`。
- 修改 `BusinessObjectDesignerShell.vue` 和 `object-designer.[objectCode].vue`，增加函数市场入口。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" <Task 18 相关前端文件>` | 无输出 |
| Git diff 空白检查 | `git diff --check -- <Task 18 相关前端文件>` | 无输出 |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/api/formula.js src/views/app-center/components/designer/formula/FormulaFunctionBrowser.vue src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue src/views/app-center/components/designer/formula/FormulaFunctionMarket.vue src/views/app-center/components/designer/formula/FormulaConfigPanel.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/views/app-center/components/designer/BusinessObjectDesignerShell.vue 'src/views/app-center/object-designer.[objectCode].vue'` | 无错误 |
| 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建成功 |

### 跳过项

- 本轮未启动后端 admin 服务和数据库，函数市场真实 HTTP、鉴权、操作日志入库和启停后动态刷新联调留到 Task 19-20。
- 本轮未启动 Vite dev server 做点击截图；生产构建已覆盖新增组件编译、路由 chunk 和 Naive UI 组件解析。

## 本轮增量验证（Task 19 / Task 20）

### 变更范围

- 新增 `FormulaValueMasker`，执行日志统一脱敏手机号、身份证、银行卡、Token、AK/SK 等敏感值。
- 新增 `FormulaRuntimeProperties`，配置执行日志总开关、失败/成功/debug 日志开关、debug 输入/输出快照和函数 timeout。
- `FormulaController` 增加 `@SaCheckPermission("ai:businessObject:design")`，调试器、执行日志、依赖图和函数市场复用对象设计权限。
- `FormulaExecutionLogService`、`FormulaDebugService`、`AbstractFormulaRuntime`、`FormulaFunctionInvoker` 接入运行时配置。
- `FormulaLookupResolver`、`FormulaCrossObjectResolver`、`FormulaValidationService` 增强配置校验，拒绝 SQL 片段和非法字段编码，LOOKUP 返回字段必须来自目标模型字段资产或 `id`。
- 新增/扩展 `FormulaValueMaskerTest`、`FormulaExecutionLogServiceTest`、`FormulaFunctionRegistryTest`、`FormulaValidationServiceTest`。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 安全加固目标命令 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaValueMaskerTest,FormulaExecutionLogServiceTest,FormulaFunctionRegistryTest,FormulaValidationServiceTest -DskipTests=false -Dmaven.test.skip=false` | 目标模块主代码编译通过 |
| Task 19 测试源码编译 | `javac -d /private/tmp/forge_formula_test_classes -cp "forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaValueMaskerTest.java FormulaExecutionLogServiceTest.java FormulaFunctionRegistryTest.java FormulaValidationServiceTest.java /private/tmp/FormulaSecurityHardeningSmoke.java` | 编译通过 |
| Task 19 行为 smoke | `java -cp "/private/tmp/forge_formula_test_classes:forge-framework/forge-plugin-parent/forge-plugin-generator/target/classes:$(cat /private/tmp/forge_generator_cp.txt)" FormulaSecurityHardeningSmoke` | 输出 `FormulaSecurityHardeningSmoke PASS` |
| 行尾空白检查 | `rg -n "[ \t]+$" <Task 19 相关 Java 文件>` | 无输出 |
| Git diff 空白检查 | `git diff --check -- <Task 19 相关 Java 文件>` | 无输出 |

### 跳过项

- Maven 单测仍受父 POM 固定 skip 配置影响，本轮用测试源码编译和临时 Java runner 验证安全加固关键行为。
- 本轮未启动后端 admin 服务和数据库；`@SaCheckPermission` 的真实 HTTP 403/权限资源联调、操作日志入库和 Flyway 实跑留到服务级验收环境覆盖。
- 本轮不启动前端构建；Task 19 仅涉及后端安全加固与变更文档回填，前端函数市场/公式配置构建已在 Task 18 覆盖。

## 本轮增量验证（字段资产与公式配置体验优化）

### 变更范围

- `BusinessFieldManager.vue` 将右侧长属性面板改为字段摘要区，详细属性进入大弹窗工作台。
- `BusinessFieldPropertyPanel.vue` 将字段属性拆分为基础属性、显示与校验、开发者属性、公式与调试标签页。
- `FormulaFunctionBrowser.vue` 从卡片网格改为函数列表 + 详情预览 + 参数 Schema + 明确插入动作。
- `FormulaDependencyGraph.vue` 从节点/边列表改为 SVG 节点连线图，支持缩放、箭头、边标签和循环高亮。
- `FormulaFunctionMarket.vue` 增加“注册函数”入口，首期仅注册 Java Bean 函数元数据，不开放脚本执行。
- `formula.js`、`FormulaController`、`FormulaFunctionMarketService`、`FormulaFunctionMapper.xml` 等补齐自定义函数注册 API、持久化 upsert 和运行时注册同步。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" <本轮相关前后端文件>` | 无输出 |
| Git diff 空白检查 | `git diff --check -- <本轮相关已跟踪文件>` | 无输出 |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/api/formula.js src/views/app-center/components/designer/BusinessFieldManager.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/views/app-center/components/designer/formula/FormulaFunctionBrowser.vue src/views/app-center/components/designer/formula/FormulaDependencyGraph.vue src/views/app-center/components/designer/formula/FormulaFunctionMarket.vue` | 无错误 |
| 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建成功 |
| 后端 generator 编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 编译成功 |

### 跳过项

- 本轮未启动后端 admin 服务和数据库，函数注册接口真实 HTTP、MySQL upsert、操作日志入库和权限拦截需在服务级验收环境覆盖。
- 本轮未启动 Vite dev server 做浏览器截图；已通过 eslint 和生产构建覆盖 Vue 模板、组件解析、chunk 生成和 TypeScript/JS 打包路径。

## 本轮增量验证（表达式工作台与执行日志分页优化）

### 变更范围

- `FormulaExpressionEditor.vue` 从 textarea + 折叠函数浏览器改为字段浏览、表达式编辑、函数树浏览和依赖字段 chips 的三栏工作台。
- 字段浏览支持搜索、按字段类型/分组折叠展示，点击字段自动插入到表达式光标位置并同步依赖字段。
- 函数浏览改为树形分组，点击函数自动插入表达式，函数项直接展示签名、说明和示例。
- `FormulaConfigPanel.vue` 移除旧的依赖字段多选重复配置，条件表达式复用同一套表达式工作台。
- `FormulaExecutionLogDrawer.vue` 执行日志分页增加页大小选择、快速跳页、总量和当前范围显示。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExecutionLogDrawer.vue` | 无输出 |
| Git diff 空白检查 | `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExecutionLogDrawer.vue` | 无输出 |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue src/views/app-center/components/designer/formula/FormulaConfigPanel.vue src/views/app-center/components/designer/formula/FormulaExecutionLogDrawer.vue` | 无错误 |
| 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建成功 |

### 跳过项

- 本轮未启动后端 admin 服务和数据库，函数列表和执行日志分页真实 HTTP 数据联调留到服务级验收环境覆盖。
- 本轮未启动 Vite dev server 做浏览器截图；已通过 eslint 和生产构建覆盖 Vue 模板解析、自动导入、样式处理和打包路径。

## 本轮增量验证（公式与调试一屏化优化）

### 变更范围

- `FormulaConfigPanel.vue` 将公式名称、公式类型、触发方式、保存、校验、预览、调试、日志、依赖入口合并到顶部紧凑命令栏。
- `FormulaConfigPanel.vue` 压缩公式卡片边距、表单控件高度、聚合/条件配置布局，减少“公式与调试”标签页纵向滚动。
- `FormulaExpressionEditor.vue` 新增 `compact` 模式，缩小表达式工作台标题、字段项、函数项、编辑区和依赖 chips 高度。
- 字段浏览和函数树在面板内部滚动，避免把整个公式页撑高。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue` | 无输出 |
| Git diff 空白检查 | `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue` | 无输出 |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/FormulaConfigPanel.vue src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue` | 无错误 |
| 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建成功 |

### 跳过项

- 本轮未启动后端 admin 服务和数据库；仅涉及前端布局密度与组件结构调整。
- 本轮未启动 Vite dev server 做浏览器截图；已通过 eslint 和生产构建覆盖 Vue 模板解析、自动导入、样式处理和打包路径。

## 本轮增量验证（跨对象引用样式修复）

### 变更范围

- `FormulaConfigPanel.vue` 将 `FormulaCrossObjectPanel` 提前到表达式编辑器之前展示。
- 跨对象引用启用后隐藏无意义的禁用表达式编辑器/条件编辑器，避免跨对象引用被大块编辑区挤到下方。
- `FormulaCrossObjectPanel.vue` 增加生成表达式预览，并局部控制表单项高度、边框、背景和层级，避免被父级紧凑样式影响。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaCrossObjectPanel.vue` | 无输出 |
| Git diff 空白检查 | `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaCrossObjectPanel.vue` | 无输出 |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/FormulaConfigPanel.vue src/views/app-center/components/designer/formula/FormulaCrossObjectPanel.vue` | 无错误 |
| 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建成功 |

### 跳过项

- 本轮未启动后端 admin 服务和数据库；仅涉及前端组件布局和样式调整。
- 本轮未启动 Vite dev server 做浏览器截图；已通过 eslint 和生产构建覆盖 Vue 模板解析、自动导入、样式处理和打包路径。

## 本轮增量验证（公式工作台全宽展开修复）

### 变更范围

- `FormulaConfigPanel.vue` 将表达式工作台从 `n-form-item` 中移出，避免 Naive 表单项内容容器限制宽度。
- `FormulaExpressionEditor.vue` 调整三栏网格比例，提升中间表达式区和右侧函数区的最小宽度。
- `BusinessFieldManager.vue` 将字段属性弹窗放宽到接近整屏，并增加可用高度。
- `BusinessFieldPropertyPanel.vue` 明确 tab pane 和 form 容器 `min-width: 0` 与 `width: 100%`，避免内部工作台无法撑开。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue` | 无输出 |
| Git diff 空白检查 | `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue` | 无输出 |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessFieldManager.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/views/app-center/components/designer/formula/FormulaConfigPanel.vue src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue` | 无错误 |
| 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建成功 |

### 跳过项

- 本轮未启动后端 admin 服务和数据库；仅涉及前端布局容器和样式调整。
- 本轮未启动 Vite dev server 做浏览器截图；已通过 eslint 和生产构建覆盖 Vue 模板解析、自动导入、样式处理和打包路径。

## 本轮增量验证（字段浏览面板等高撑满修复）

### 变更范围

- `FormulaExpressionEditor.vue` 为表达式工作台三栏网格增加显式 `align-items: stretch`，避免左侧字段浏览面板按内容高度收缩。
- `FormulaExpressionEditor.vue` 为字段浏览、表达式编辑、函数浏览三栏统一设置 `height: 100%`、`align-self: stretch` 和 `box-sizing: border-box`。
- `FormulaExpressionEditor.vue` 将左侧字段面板单独设为 `auto auto minmax(0, 1fr)` 行结构，字段列表滚动区占满剩余高度，修复截图中左侧白色面板底部未撑满的问题。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue` | 无输出 |
| Git diff 空白检查 | `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue` | 无输出 |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue src/views/app-center/components/designer/formula/FormulaConfigPanel.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/views/app-center/components/designer/BusinessFieldManager.vue` | 无错误 |
| 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建成功 |

### 跳过项

- 本轮未启动后端 admin 服务和数据库；仅涉及前端 CSS 布局约束调整。
- 未完成 Playwright 截图复核；用户在真实页面截图中指出问题后，按截图定位做针对性布局修复，并通过 eslint 和生产构建覆盖静态质量。

## 本轮增量验证（公式设置背景扁平化与字段中文名展示）

### 变更范围

- `FormulaConfigPanel.vue` 去除 `formula-setting-card` 外层灰底、边框、圆角和内边距，公式设置改为铺满公式 tab 的白色工作区，减少“卡片套卡片”的层级感。
- `FormulaExpressionEditor.vue` 去除表达式工作台外壳灰底、边框和内边距，只保留三栏内部面板边界，避免公式设置区域背景看起来没有撑满。
- `BusinessFieldPropertyPanel.vue` 将属性面板 body、tabs pane wrapper、公式 tab form 的背景统一为白色，避免浅灰底从公式区域边缘露出。
- `FormulaExpressionEditor.vue` 字段浏览项改为第一行展示字段中文名，第二行展示字段编码；compact 模式不再隐藏字段中文/编码副信息。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 行尾空白检查 | `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue` | 无输出 |
| Git diff 空白检查 | `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue` | 无输出 |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue src/views/app-center/components/designer/formula/FormulaConfigPanel.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/views/app-center/components/designer/BusinessFieldManager.vue` | 无错误 |
| 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建成功 |

### 跳过项

- 本轮未启动后端 admin 服务和数据库；仅涉及前端布局和字段显示调整。
- 未完成 Playwright 截图复核；本轮通过用户反馈定位问题，并通过 eslint 和生产构建覆盖静态质量。

## 本轮增量验证（移除公式设置卡片 active 类）

### 变更范围

- `FormulaConfigPanel.vue` 移除模板上的 `:class="{ active: formulaEnabled }"`，不再生成 `class="formula-setting-card active"`。
- `FormulaConfigPanel.vue` 将外层类名从 `formula-setting-card` 改为 `formula-setting-workspace`，避免继续保留卡片语义和旧样式覆盖点。
- `FormulaConfigPanel.vue` 删除 `.formula-setting-card.active` 样式块，公式启用状态只用于逻辑展示，不参与外层视觉布局。

### P0 验证

| 验证项 | 命令 | 预期 |
|--------|------|------|
| 旧类名检查 | `rg -n "formula-setting-card|formula-setting-workspace|\\.active" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue` | 只出现 `formula-setting-workspace` |
| 行尾空白检查 | `rg -n "[ \t]+$" forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue` | 无输出 |
| Git diff 空白检查 | `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue` | 无输出 |
| 前端 eslint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/FormulaConfigPanel.vue` | 无错误 |
| 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建成功 |

### 跳过项

- 本轮未启动后端 admin 服务和数据库；仅涉及前端模板类名和样式选择器调整。
