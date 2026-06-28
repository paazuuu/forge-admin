# 任务清单：formula-next-capabilities
> status: proposed
> created: 2026-06-13
> spec: `code-copilot/changes/formula-next-capabilities/spec.md`
> base: `code-copilot/changes/formula-capability-extension/`

## 任务总览

| Task | 名称 | 状态 | 优先级 | 阶段 |
|------|------|------|--------|------|
| Task 1 | 执行日志库表与持久化模型 | completed | P0 | Phase 1 |
| Task 2 | 公式执行 Trace 数据结构 | completed | P0 | Phase 1 |
| Task 3 | 运行时接入 Trace 与执行日志 | completed | P0 | Phase 1 |
| Task 4 | 执行日志查询 API | completed | P0 | Phase 1 |
| Task 5 | 公式调试器后端 API | completed | P0 | Phase 1 |
| Task 6 | Dependency 图后端 API | completed | P0 | Phase 1 |
| Task 7 | 公式可观测性前端组件 | completed | P0 | Phase 1 |
| Task 8 | LOOKUP / 跨对象公式领域模型扩展 | completed | P1 | Phase 2 |
| Task 9 | LOOKUP 公式解析与执行 | completed | P1 | Phase 2 |
| Task 10 | 一跳跨对象路径解析与批量预取 | completed | P1 | Phase 2 |
| Task 11 | 跨对象发布校验与重算策略 | completed | P1 | Phase 2 |
| Task 12 | LOOKUP / 跨对象公式前端配置 | completed | P1 | Phase 2 |
| Task 13 | 条件规则 AST 与编译 API | completed | P2 | Phase 3 |
| Task 14 | 条件规则设计器前端组件 | completed | P2 | Phase 3 |
| Task 15 | 函数注册表库表与持久化模型 | completed | P3 | Phase 4 |
| Task 16 | 函数注册中心与表达式引擎集成 | completed | P3 | Phase 4 |
| Task 17 | 函数市场后端 API | completed | P3 | Phase 4 |
| Task 18 | 函数市场与动态函数选择前端 | completed | P3 | Phase 4 |
| Task 19 | 安全、权限、脱敏与配置开关 | completed | P0 | Hardening |
| Task 20 | 自动化测试与执行日志回填 | completed | P0 | Validation |

## 阶段门禁

- Phase 1 完成前，不开始 LOOKUP / 跨对象运行时代码，避免复杂公式缺少追踪能力。
- Phase 2 首期只支持一跳关系，不实现多跳路径。
- Phase 4 首期只支持 Java Bean 注册函数，不开放脚本函数。
- 所有查询 SQL 必须写在 Mapper XML 中，禁止在 Service 层拼复杂查询。
- 所有 Flyway 新增表和内置数据必须具备防重复保护，`tenant_id` 使用 `1`。

## Task 1：执行日志库表与持久化模型

**涉及文件**

- Create: `forge-server/db/migration/V1.0.69__add_formula_observability_and_function_market.sql`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiFormulaExecutionLog.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/FormulaExecutionLogMapper.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/FormulaExecutionLogMapper.xml`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaExecutionLogQueryDTO.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaExecutionLogResponse.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaExecutionLogService.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaExecutionLogServiceTest.java`

**执行要点**

- 新增 `ai_formula_execution_log` 表，字段覆盖 Spec 6.5。
- `input_snapshot` 使用 JSON 或 TEXT，写入前必须走脱敏工具。
- Mapper XML 提供 `selectFormulaExecutionLogPage` 和 `selectFormulaExecutionLogById`。
- 查询参数使用 `pageNum` + `pageSize`。
- Service 提供 `record(FormulaExecutionLogResponse log)`、`page(FormulaExecutionLogQueryDTO query)`、`detail(Long id)`。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaExecutionLogServiceTest
```

**验收标准**

- Flyway 脚本可重复校验，不依赖字段顺序插入。
- 日志 Service 能保存成功日志和失败日志。
- 日志查询支持 objectCode、recordId、fieldCode、success、traceId、时间范围筛选。

**执行记录**

- 2026-06-13 完成 `ai_formula_execution_log` 表、实体、Mapper/XML、查询 DTO、响应 DTO、Service 与单测文件。
- Maven 目标模块编译通过；父 POM 固定跳过测试，实际 Service 行为通过 `/private/tmp/FormulaExecutionLogServiceSmoke.java` 临时 runner 验证。

## Task 2：公式执行 Trace 数据结构

**涉及文件**

- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/FormulaExecutionTrace.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/FormulaExecutionStep.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/FormulaTraceOptions.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/ExecutionResult.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/domain/formula/FormulaExecutionTraceTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/domain/formula/ExecutionResultTest.java`

**执行要点**

- `FormulaExecutionTrace` 保存 `traceId`、`executionPlan`、`steps`、`elapsedMs`、`errors`。
- `FormulaExecutionStep` 保存 `fieldCode`、`formulaType`、`expression`、`input`、`output`、`elapsedMs`、`success`、`errorMessage`、`metadata`。
- `FormulaTraceOptions` 支持 `enabled`、`includeInputSnapshot`、`includeOutputValue`、`debugMode`。
- `ExecutionResult` 保持 V1 兼容，新增可空 `trace` 字段。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaExecutionTraceTest,ExecutionResultTest
```

**验收标准**

- 不开启 trace 时，V1 `ExecutionResult` 行为不变。
- 开启 trace 时，结果中包含执行计划和步骤明细。
- `ExecutionResult` 旧单测继续通过。

**执行记录**

- 2026-06-13 完成 `FormulaExecutionTrace`、`FormulaExecutionStep`、`FormulaTraceOptions`，并为 `ExecutionResult` 增加可空 `trace` 字段。
- Maven 目标模块编译通过；父 POM 固定跳过测试，实际 Trace 行为通过 `/private/tmp/FormulaExecutionTraceSmoke.java` 临时 runner 验证。

## Task 3：运行时接入 Trace 与执行日志

**涉及文件**

- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaExecutionEngine.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/AbstractFormulaRuntime.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/StoredFormulaRuntime.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/VirtualFormulaRuntime.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/FormulaErrorHandler.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaExecutionEngineTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/StoredFormulaRuntimeTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/VirtualFormulaRuntimeTest.java`

**执行要点**

- 为 `FormulaExecutionEngine.execute` 增加带 `FormulaTraceOptions` 的重载方法。
- CALC、AGGREGATE、CONDITIONAL 每个字段执行前后写入 `FormulaExecutionStep`。
- Runtime 层在失败或 debugMode 时调用 `FormulaExecutionLogService.record`。
- ERROR 日志输出 `traceId`，便于从应用日志反查业务日志。
- 成功日志默认不全量记录，只在 debugMode 或配置开启时记录。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaExecutionEngineTest,StoredFormulaRuntimeTest,VirtualFormulaRuntimeTest
```

**验收标准**

- 公式异常时日志里有 traceId。
- debugMode 执行成功也能落执行日志。
- 不开启日志时保存和查询性能路径不额外落库。

**执行记录**

- 2026-06-13 完成 `FormulaExecutionEngine.execute(..., FormulaTraceOptions)`、运行时失败/调试日志接入和 traceId 错误日志。
- Maven 目标模块编译通过；父 POM 固定跳过测试，实际运行时行为通过 `/private/tmp/FormulaRuntimeTraceSmoke.java` 临时 runner 验证。

## Task 4：执行日志查询 API

**涉及文件**

- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/FormulaController.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaExecutionLogDetailResponse.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/controller/FormulaControllerTest.java`

**执行要点**

- 新增 `GET /api/ai/business/formula/log/page`。
- 新增 `GET /api/ai/business/formula/log/{id}`。
- Controller 参数统一 `pageNum` + `pageSize`。
- 返回 `RespInfo.success(data)`。
- 敏感字段只返回脱敏后快照。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaControllerTest
```

**验收标准**

- 分页接口能按 traceId 精确查询。
- 详情接口返回 steps 或 inputSnapshot 时不泄露敏感值。

**执行记录**

- 2026-06-13 完成 `GET /api/ai/business/formula/log/page` 与 `GET /api/ai/business/formula/log/{id}`。
- 新增 `FormulaExecutionLogDetailResponse`，Controller 统一使用 `pageNum` + `pageSize` 绑定查询参数。
- Maven 目标模块编译通过；父 POM 固定跳过测试，实际 Controller 行为通过 `/private/tmp/FormulaControllerLogSmoke.java` 临时 runner 验证。

## Task 5：公式调试器后端 API

**涉及文件**

- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaDebugRequest.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaDebugResponse.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaDebugService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/FormulaController.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaDebugServiceTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/controller/FormulaControllerTest.java`

**执行要点**

- 新增 `POST /api/ai/business/formula/debug`。
- 支持调试单字段 `fieldCode`，也支持调试当前对象全部公式。
- 输入 `sampleValues`，输出 `traceId`、`executionPlan`、`steps`、`errors`。
- 调试执行默认记录执行日志。
- LOOKUP / 跨对象尚未实现时，调试器必须保持 CALC / AGGREGATE / CONDITIONAL 可用。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaDebugServiceTest,FormulaControllerTest
```

**验收标准**

- 调试 `price * quantity` 返回每一步输入和输出。
- 条件公式能显示 true/false 分支结果。
- 调试失败也返回 traceId。

**执行记录**

- 2026-06-13 完成 `POST /api/ai/business/formula/debug`、`FormulaDebugRequest`、`FormulaDebugResponse` 与 `FormulaDebugService`。
- 单字段调试会自动选择目标字段及公式依赖闭包；全量调试执行请求内全部公式。
- 调试执行默认使用 debug trace，并将每个 step 写入执行日志服务；敏感字段脱敏继续复用 `FormulaExecutionLogService`。
- 条件公式 trace step metadata 增加 `conditionResult`、`conditionMatched`、`trueValue`、`falseValue`，前端可直接展示分支命中。
- Maven 目标模块编译通过；父 POM 固定跳过测试，实际调试行为通过 `/private/tmp/FormulaDebugServiceSmoke.java` 临时 runner 验证。

## Task 6：Dependency 图后端 API

**涉及文件**

- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaDependencyGraphRequest.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaDependencyGraphResponse.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaDependencyGraphNode.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaDependencyGraphEdge.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaDependencyGraphService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/FormulaController.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaDependencyGraphServiceTest.java`

**执行要点**

- 新增 `POST /api/ai/business/formula/dependency/graph`。
- 将当前 `DependencyAnalysisResult` 转换为 nodes / edges。
- 节点类型支持 FIELD、FORMULA、OBJECT、FUNCTION、RELATION。
- 边类型支持 DEPENDS_ON、LOOKUP、CROSS_OBJECT、AGGREGATE、FUNCTION_CALL。
- 循环依赖返回 `cyclePath`。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaDependencyGraphServiceTest
```

**验收标准**

- A 依赖 B、C 时返回 3 个节点和 2 条 DEPENDS_ON 边。
- 发生循环时 `hasCycle = true` 且 `cyclePath` 不为空。

**执行记录**

- 2026-06-13 完成 `POST /api/ai/business/formula/dependency/graph`。
- 新增图结构 DTO：`FormulaDependencyGraphRequest`、`FormulaDependencyGraphResponse`、`FormulaDependencyGraphNode`、`FormulaDependencyGraphEdge`。
- 新增 `FormulaDependencyGraphService`，将现有 DAG 分析结果转换为 nodes / edges，支持 FIELD、FORMULA、RELATION 节点和 DEPENDS_ON、AGGREGATE 边。
- Maven 目标模块编译通过；父 POM 固定跳过测试，实际图结构行为通过 `/private/tmp/FormulaDependencyGraphSmoke.java` 临时 runner 验证。

## Task 7：公式可观测性前端组件

**涉及文件**

- Modify: `forge-admin-ui/src/api/formula.js`
- Create: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaDebuggerPanel.vue`
- Create: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExecutionLogDrawer.vue`
- Create: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaDependencyGraph.vue`
- Create: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaTraceSteps.vue`
- Modify: `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue`
- Modify: `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue`
- Modify: `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`

**执行要点**

- `formula.js` 新增 `debugFormula`、`getFormulaLogPage`、`getFormulaLogDetail`、`getDependencyGraph`。
- 调试器支持输入样例值、执行调试、展示 steps。
- 执行日志抽屉支持按字段和 traceId 查询。
- 依赖图组件先用 SVG / div 结构实现基础节点边，后续再接图形库。
- 对象设计器增加“公式工具”入口，默认带 objectCode。

**验证命令**

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/api/formula.js src/views/app-center/components/designer/formula/*.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/views/app-center/object-designer.[objectCode].vue
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

**验收标准**

- 字段属性面板可打开调试器。
- 调试成功后能看到 executionPlan 和 steps。
- 依赖图能显示字段节点和依赖边。
- 日志抽屉可分页展示日志列表。

**执行记录**

- 2026-06-13 完成 `formula.js` 调试、日志、依赖图 API 封装。
- 新增公式调试器、执行日志、依赖图和 Trace 步骤组件；执行日志按用户偏好使用居中弹窗，不使用右侧抽屉。
- 字段属性面板新增“调试 / 执行日志 / 依赖图”入口，并将未保存的当前公式配置合并进调试和依赖图请求。
- 对象设计页通过 `BusinessFieldManager` 向属性面板透传 `objectCode`，用于调试请求和日志筛选。
- 前端生产构建通过；本地 Vite + Playwright 冒烟验证前端入口可正常加载，因未启动后端服务，真实公式接口交互留到服务联调验证。

## Task 8：LOOKUP / 跨对象公式领域模型扩展

**涉及文件**

- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/FormulaType.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/FormulaConfig.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/LookupConfig.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/CrossObjectConfig.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/CrossObjectRecomputeMode.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessFieldDTO.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessFieldVO.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/domain/formula/FormulaConfigTest.java`

**执行要点**

- `FormulaType` 新增 `LOOKUP`。
- `FormulaConfig` 新增 `lookup`、`crossObject`、`rule`、`functionRefs`，并保持 V1 JSON 兼容。
- LOOKUP 类型必须要求 `LookupConfig` 非空。
- CrossObject 配置首期只允许一跳路径。
- DTO / VO 保持 `formulaConfig` JSON 透传，不破坏旧字段。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaConfigTest
```

**验收标准**

- 旧 CALC / AGGREGATE / CONDITIONAL 配置反序列化不报错。
- LOOKUP 缺少 lookup 配置时报明确异常。

**执行记录**

- 2026-06-14 完成 `FormulaType.LOOKUP`、`LookupConfig`、`CrossObjectConfig`、`CrossObjectRecomputeMode`。
- `FormulaConfig` 新增 `lookup`、`crossObject`、`rule`、`functionRefs`，保留 builder 不可变风格并补充 Jackson 反序列化兼容。
- `AggregateConfig`、`ConditionConfig` 补充 Jackson 构造注解，旧 CALC / AGGREGATE / CONDITIONAL JSON 可直接反序列化。
- `AbstractFormulaRuntime`、`FormulaPublishValidator` 支持从字段 `formulaConfig` Map 解析 LOOKUP / crossObject 元数据。
- `BusinessFieldDTO`、`BusinessFieldVO` 注释更新为 CALC / AGGREGATE / CONDITIONAL / LOOKUP JSON 透传。
- Maven 目标模块主代码编译通过；父 POM 固定跳过测试，实际领域行为通过 `/private/tmp/FormulaConfigSmoke.java` 临时 runner 验证。

## Task 9：LOOKUP 公式解析与执行

**涉及文件**

- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaLookupResolver.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/LookupResolveResult.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaExecutionEngine.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaValidationService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/FormulaDependencyAnalyzer.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaDebugRequest.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaDependencyGraphRequest.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaDebugService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaDependencyGraphService.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaLookupResolverTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaExecutionEngineTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaExecutionEngineLookupTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/domain/formula/FormulaDependencyAnalyzerTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaValidationServiceTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaDependencyGraphServiceTest.java`

**执行要点**

- LOOKUP 只能基于 `relationCode` 和对象关系元数据解析目标对象。
- 禁止配置表名、SQL、Mapper 方法名。
- Resolver 根据 `sourceField` 值匹配目标对象 `targetField`，返回 `returnField`。
- 未命中返回 `notFoundValue`。
- 执行步骤 metadata 写入 relationCode、targetObjectCode、sourceField、targetField、returnField。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaLookupResolverTest,FormulaExecutionEngineLookupTest,FormulaDependencyAnalyzerTest,FormulaValidationServiceTest,FormulaDependencyGraphServiceTest
```

**验收标准**

- LOOKUP 命中时返回目标字段值。
- LOOKUP 未命中时返回 notFoundValue。
- 非法关系路径校验失败。

**执行记录**

- 2026-06-14 完成 `FormulaLookupResolver` 与 `LookupResolveResult`，LOOKUP 只接收关系/对象/字段元数据，数据库路径内部解析目标对象模型与物理列，不暴露表名或 SQL 配置。
- `FormulaExecutionEngine` 接入 LOOKUP 执行分支，命中返回目标字段值，未命中按 `notFoundValue` 成功降级，resolver 失败走公式错误处理并在 trace metadata 输出 relationCode、targetObjectCode、sourceField、targetField、returnField、lookupMatched。
- `FormulaValidationService` 支持 LOOKUP 空表达式校验，拦截 path-style relationCode；`FormulaDependencyAnalyzer` 将 LOOKUP sourceField 纳入拓扑排序、深度和字段存在性校验。
- 调试器和依赖图请求 DTO/Service 已补充 lookup 配置解析，依赖图输出 LOOKUP 边。
- `FormulaExecutionEngineTest.java` 是非 UTF-8 旧文件，未做编码转换；新增 `FormulaExecutionEngineLookupTest` 独立覆盖 LOOKUP 引擎行为。
- Maven 目标模块编译通过；父 POM 固定跳过测试，实际 LOOKUP 行为通过 `/private/tmp/FormulaLookupSmoke.java` 临时 runner 验证。

## Task 10：一跳跨对象路径解析与批量预取

**涉及文件**

- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/CrossObjectPath.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaCrossObjectResolver.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaReferenceResolver.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/VirtualFormulaRuntime.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/DetailRecordFetcher.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/DynamicDetailRecordFetcher.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudRepository.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaDebugRequest.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaDependencyGraphRequest.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaDebugService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaDependencyGraphService.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaCrossObjectResolverTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/VirtualFormulaRuntimeTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaDependencyGraphServiceTest.java`

**执行要点**

- 支持路径格式 `relationAlias.fieldCode`，例如 `customer.level`。
- 路径必须解析到对象关系和目标字段。
- VIRTUAL 列表查询时使用批量预取，避免每行单独查目标对象。
- 批量预取必须保留 tenant_id 和数据权限边界。
- 暂不支持 `a.b.c` 多跳路径。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaCrossObjectResolverTest,VirtualFormulaRuntimeTest,FormulaDependencyGraphServiceTest
```

**验收标准**

- 一跳路径能解析并返回目标字段。
- 多跳路径被拒绝并返回明确错误。
- 批量预取测试证明不会按记录数发起 N 次查询。

**执行记录**

- 2026-06-14 完成 `CrossObjectPath`、`FormulaCrossObjectResolver`、`FormulaReferenceResolver`，支持 `relationAlias.fieldCode` 一跳路径解析，多跳和非法路径直接拒绝。
- `VirtualFormulaRuntime` 对包含 `crossObject` 的 VIRTUAL 公式先复制记录、批量预取目标对象值、执行公式，再只把公式结果回写原记录，避免泄漏内部嵌套对象上下文。
- `DetailRecordFetcher` 增加批量读取默认方法，`DynamicDetailRecordFetcher` 使用 `DynamicCrudRepository.selectListByColumnIn` 单次查询目标对象记录，避免列表场景 N+1。
- 调试器和依赖图请求 DTO/Service 已补充 `crossObject` 配置解析；依赖图输出 `CROSS_OBJECT` 边。
- Maven 目标模块编译通过；父 POM 固定跳过测试，实际一跳解析、批量预取、虚拟公式执行和依赖图行为通过 `/private/tmp/FormulaCrossObjectSmoke.java` 临时 runner 验证。

## Task 11：跨对象发布校验与重算策略

**涉及文件**

- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaObjectDependencyAnalyzer.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/CrossObjectRecomputeTaskService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaPublishValidator.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectPublishService.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaObjectDependencyAnalyzerTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaPublishValidatorTest.java`

**执行要点**

- 发布时将跨对象公式转为对象图依赖。
- 检测 A 对象依赖 B、B 对象反向依赖 A 的循环。
- STORED 跨对象公式首期写入待重算任务，不做深层同步级联。
- 待重算任务必须具备幂等键：objectCode + recordId + fieldCode + dependencyTrace。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaObjectDependencyAnalyzerTest,FormulaPublishValidatorTest
```

**验收标准**

- 非法对象关系发布阻断。
- 跨对象循环依赖发布阻断。
- STORED 跨对象公式能生成待重算任务。

**执行记录**

- 2026-06-14 完成 `FormulaObjectDependencyAnalyzer`，发布期可将跨对象公式转换为对象图边并检测对象级循环依赖。
- 2026-06-14 完成 `CrossObjectRecomputeTaskService`，为 STORED 跨对象公式生成 `PENDING` 待重算任务，幂等键格式为 `objectCode:recordId:fieldCode:dependencyTrace`。
- `FormulaPublishValidator` 新增对象图上下文校验重载，保留旧 `validate(modelSchema)` 兼容入口。
- `BusinessObjectPublishService` 在发布检查中接入对象图校验，并在发布成功后为当前对象生成跨对象公式待重算任务。
- Maven 目标模块编译通过；父 POM 固定跳过测试，实际对象依赖、发布阻断和重算任务行为通过 `/private/tmp/FormulaObjectDependencySmoke.java` 临时 runner 验证。

## Task 12：LOOKUP / 跨对象公式前端配置

**涉及文件**

- Modify: `forge-admin-ui/src/api/formula.js`
- Create: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue`
- Create: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaLookupPanel.vue`
- Create: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaCrossObjectPanel.vue`
- Modify: `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue`

**执行要点**

- 将公式配置区从 `BusinessFieldPropertyPanel.vue` 中拆到 `FormulaConfigPanel.vue`。
- 公式类型下拉新增 LOOKUP。
- LOOKUP 面板支持选择 relationCode、targetObjectCode、sourceField、targetField、returnField、notFoundValue。
- 跨对象面板首期支持一跳路径选择，不允许用户手写任意多跳路径。
- 保存 payload 兼容 V1 formulaConfig。

**验证命令**

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/*.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue src/api/formula.js
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

**验收标准**

- 能在字段属性面板选择 LOOKUP 类型并保存配置。
- 旧 CALC / AGGREGATE / CONDITIONAL 表单回显正常。
- 多跳路径不能在 UI 中配置。

**执行记录**

- 2026-06-14 完成公式配置区拆分，新增 `FormulaConfigPanel.vue` 承载公式基础配置、表达式校验、预览、调试、执行日志和依赖图入口。
- 2026-06-14 新增 `FormulaLookupPanel.vue`，支持 relationCode、targetObjectCode、sourceField、targetField、returnField、notFoundValue 配置，并在 LOOKUP payload 中保持 V1 兼容。
- 2026-06-14 新增 `FormulaCrossObjectPanel.vue`，通过关系选择生成一跳 `relationCode.returnField` 路径，不提供自由多跳输入。
- `BusinessFieldPropertyPanel.vue` 改为引用公式配置组件，旧 CALC / AGGREGATE / CONDITIONAL 配置仍可回显，LOOKUP 和跨对象配置会同步到调试器、依赖图请求。
- 前端 eslint、生产构建、行尾空白和 `git diff --check` 均通过；本地 preview 冒烟因当前沙箱禁止监听 `127.0.0.1:4174` 未执行到浏览器步骤，已记录为环境边界。

## Task 13：条件规则 AST 与编译 API

**涉及文件**

- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/ConditionRuleNode.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/ConditionRuleOperator.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/ConditionRuleCompiler.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/ConditionRuleCompileRequest.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/ConditionRuleCompileResponse.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/FormulaController.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/ConditionRuleCompilerTest.java`

**执行要点**

- 支持 AND / OR 分组。
- 支持 EQ、NE、GT、GTE、LT、LTE、IN、NOT_IN、CONTAINS、STARTS_WITH、ENDS_WITH、IS_NULL、NOT_NULL。
- 编译结果输出 Aviator 表达式。
- `POST /api/ai/business/formula/rule/compile` 返回表达式。
- `POST /api/ai/business/formula/rule/validate` 校验字段、操作符和值类型。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=ConditionRuleCompilerTest,FormulaControllerTest
```

**验收标准**

- AST `amount > 1000 AND status = ACTIVE` 编译为 `amount > 1000 && status == 'ACTIVE'`。
- 空分组、非法操作符、字段缺失均返回明确错误。

**执行记录**

- 2026-06-14 新增 `ConditionRuleNode`、`ConditionRuleOperator`，支持 AND / OR 分组与 EQ、NE、GT、GTE、LT、LTE、IN、NOT_IN、CONTAINS、STARTS_WITH、ENDS_WITH、IS_NULL、NOT_NULL 条件操作符。
- 2026-06-14 新增 `ConditionRuleCompiler`，将规则 AST 编译为受控 Aviator 表达式，并校验字段、操作符、集合值和基础值类型。
- 2026-06-14 新增 `ConditionRuleCompileRequest`、`ConditionRuleCompileResponse`，Controller 增加 `POST /api/ai/business/formula/rule/compile` 和 `/rule/validate`。
- 新增 `ConditionRuleCompilerTest`，扩展 `FormulaControllerTest` 覆盖 compile / validate 路由。
- Maven 目标模块主代码编译通过；父 POM 固定跳过测试，实际编译器和 Controller 行为通过 `/private/tmp/ConditionRuleCompilerSmoke.java` 临时 runner 验证。

## Task 14：条件规则设计器前端组件

**涉及文件**

- Modify: `forge-admin-ui/src/api/formula.js`
- Create: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConditionRuleDesigner.vue`
- Create: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConditionRuleNode.vue`
- Modify: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaConfigPanel.vue`
- Modify: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaDebuggerPanel.vue`

**执行要点**

- 条件公式支持“表达式模式”和“规则设计模式”切换。
- 规则设计器支持新增分组、新增条件、删除节点、切换 AND/OR。
- 保存时以 rule JSON 为主，expression 为编译快照。
- 调试器展示每个规则节点是否命中。

**验证命令**

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/*.vue src/api/formula.js
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

**验收标准**

- UI 可生成 `amount > 1000 && status == 'ACTIVE'`。
- 表达式模式切回规则模式时，已保存 rule JSON 可正常回显。
- 调试结果能看到规则节点命中状态。

**执行记录**

- 2026-06-14 新增 `FormulaConditionRuleNode.vue`，支持递归 AND / OR 分组、条件节点新增/删除、字段/操作符/值编辑。
- 2026-06-14 新增 `FormulaConditionRuleDesigner.vue`，规则变更时本地生成表达式快照，并调用 `/rule/compile` 做后端校验。
- `FormulaConfigPanel.vue` 的 CONDITIONAL 配置改为“表达式 / 规则设计”模式切换，规则模式保存 `rule` JSON 和编译后的 expression 快照。
- `BusinessFieldPropertyPanel.vue` 支持旧表达式模式回显、rule JSON 回显、规则编译错误纳入本地校验和保存 payload。
- `FormulaDebuggerPanel.vue` 调试请求透传 `rule`，并根据调试上下文展示规则分组和叶子条件命中状态。
- 前端 eslint、生产构建、行尾空白和 `git diff --check` 均通过；本地浏览器点击冒烟因当前沙箱端口监听限制未执行，仍沿用 Task 12 记录的环境边界。

## Task 15：函数注册表库表与持久化模型

**涉及文件**

- Modify: `forge-server/db/migration/V1.0.69__add_formula_observability_and_function_market.sql`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiFormulaFunction.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiFormulaFunctionVersion.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiFormulaFunctionInstall.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/FormulaFunctionMapper.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/FormulaFunctionMapper.xml`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaFunctionPersistenceTest.java`

**执行要点**

- 新增 `ai_formula_function`、`ai_formula_function_version`、`ai_formula_function_install`。
- 内置函数 seed 数据使用 `INSERT ... SELECT ... WHERE NOT EXISTS`。
- `tenant_id` 使用 `1`。
- Mapper XML 提供函数分页、详情、安装状态查询。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaFunctionPersistenceTest
```

**验收标准**

- 内置函数重复执行迁移不会重复插入。
- 函数分页可按 category、sourceType、status 查询。

**执行记录**

- 2026-06-14 完成 `ai_formula_function`、`ai_formula_function_version`、`ai_formula_function_install` 表结构，迁移脚本具备 `CREATE TABLE IF NOT EXISTS` 和 seed `WHERE NOT EXISTS` 防重复保护，内置数据 `tenant_id` 使用 `1`。
- 新增 `AiFormulaFunction`、`AiFormulaFunctionVersion`、`AiFormulaFunctionInstall` 实体和 `FormulaFunctionMapper` / XML，提供分页、详情、版本和安装状态查询。
- 新增 `FormulaFunctionPersistenceTest`，覆盖函数元数据、版本元数据、安装状态和 Mapper 方法签名。
- Maven 目标模块主代码编译通过；父 POM 固定跳过测试，测试源码编译和 `/private/tmp/FormulaFunctionPersistenceSmoke.java` 临时 runner 验证通过。

## Task 16：函数注册中心与表达式引擎集成

**涉及文件**

- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaFunctionRegistry.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaFunctionDefinition.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaFunctionInvoker.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaBuiltinFunctionProvider.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/formula/ExpressionExecutor.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaValidationService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaExecutionEngine.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/FormulaController.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaFunctionRegistryTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/domain/formula/ExpressionExecutorTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaValidationServiceTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/controller/FormulaControllerTest.java`

**执行要点**

- 将 Controller 中硬编码函数列表迁移到函数注册中心。
- 首期函数实现只允许 Java Bean 注册。
- 函数调用校验 argumentSchema 和 returnType。
- 函数执行设置超时限制。
- 禁用函数在校验和发布阶段阻断。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaFunctionRegistryTest,ExpressionExecutorTest,FormulaValidationServiceTest
```

**验收标准**

- 已启用函数可在表达式中调用。
- 禁用函数不能通过公式校验。
- 函数参数数量或类型错误时返回明确错误。

**执行记录**

- 2026-06-14 新增 `FormulaFunctionDefinition`、`FormulaBuiltinFunctionProvider`、`FormulaFunctionInvoker`、`FormulaFunctionRegistry`，内置函数和后续市场函数统一走 Java Bean 注册路径。
- `FormulaController.functions()` 已从硬编码列表切换为 `FormulaFunctionRegistry.listEnabledResponses()`。
- `ExpressionExecutor` 在编译/执行前安装注册函数；`FormulaValidationService` 在表达式校验中阻断禁用函数、未支持实现和受管命名空间未注册函数；`FormulaExecutionEngine` 的 Spring 注入路径复用注册中心。
- 新增/扩展测试覆盖函数列表、注册函数执行、参数数量/类型错误、禁用函数校验和 Controller 函数列表。
- Maven 目标模块主代码编译通过；父 POM 固定跳过测试，测试源码编译和 `/private/tmp/FormulaFunctionRegistrySmoke.java` 临时 runner 验证通过。

## Task 17：函数市场后端 API

**涉及文件**

- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaFunctionMarketQueryDTO.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/formula/FormulaFunctionInstallRequest.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaFunctionMarketService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/FormulaController.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaFunctionMarketServiceTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/controller/FormulaControllerTest.java`

**执行要点**

- 新增函数市场分页、详情、安装接口。
- 新增函数启用、禁用接口。
- 安装、启用、禁用记录操作日志。
- 未安装或禁用的市场函数不出现在公式编辑器可选函数中。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaFunctionMarketServiceTest,FormulaControllerTest
```

**验收标准**

- 函数可安装、启用、禁用。
- 禁用后公式发布校验阻断引用该函数的新配置。

**执行记录**

- 2026-06-14 完成函数市场查询、详情、安装、启用、禁用后端 API，`FormulaController.functions()` 切换为市场服务返回已安装且启用的函数。
- 新增 `FormulaFunctionMarketQueryDTO`、`FormulaFunctionInstallRequest`、`FormulaFunctionMarketResponse`、`FormulaFunctionMarketService`，Mapper XML 提供市场分页、详情、已启用函数列表和安装状态 upsert。
- 安装/启用/禁用接口补充 `@OperationLog`；XML upsert 前由 Service 显式生成 `IdWorker` 主键，避免 `ASSIGN_ID` 在手写 XML insert 中不生效。
- `FormulaValidationService` 在 Spring 注入路径下使用 `FormulaFunctionMarketService.validateFunctionReferences()`，禁用或未启用的市场函数会阻断公式校验。
- `V1.0.69__add_formula_observability_and_function_market.sql` 已补齐注册中心全部内置函数种子，保持数据库市场函数和表达式引擎能力一致。
- Maven 目标模块主代码编译通过；父 POM 固定跳过测试，测试源码编译和 `/private/tmp/FormulaFunctionMarketSmoke.java` 临时 runner 验证通过。

## Task 18：函数市场与动态函数选择前端

**涉及文件**

- Modify: `forge-admin-ui/src/api/formula.js`
- Create: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaFunctionMarket.vue`
- Create: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaFunctionBrowser.vue`
- Modify: `forge-admin-ui/src/views/app-center/components/designer/formula/FormulaExpressionEditor.vue`
- Modify: `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`

**执行要点**

- `getFormulaFunctions` 改为从后端注册表读取。
- 函数浏览器支持分类筛选、搜索、插入表达式。
- 函数市场支持分页、详情、安装、启用、禁用。
- 对象设计器增加函数市场入口。

**验证命令**

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/formula/*.vue src/views/app-center/object-designer.[objectCode].vue src/api/formula.js
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

**验收标准**

- 公式编辑器函数下拉来自后端。
- 函数市场可安装并启用函数。
- 禁用函数后不再出现在可插入列表中。

**执行记录**

- 2026-06-14 完成公式函数前端 API、动态函数浏览器、表达式编辑器抽取和函数市场弹窗。
- `FormulaExpressionEditor` 取代 `FormulaConfigPanel` 中的静态函数下拉；`FormulaFunctionBrowser` 从 `/api/ai/business/formula/functions` 动态加载可插入函数并支持搜索、分类筛选和插入表达式。
- `FormulaFunctionMarket` 通过居中弹窗提供分页、搜索、分类筛选、详情、安装、启用、禁用，不使用右侧抽屉。
- 对象设计器右上角“更多”菜单新增“函数市场”入口；字段公式 payload 自动生成 `functionRefs`，用于后端发布校验识别函数市场状态。
- 前端 ESLint、生产构建、行尾空白检查和 `git diff --check` 通过；构建仅保留既有 CSS `//` 注释和 chunk 混合导入警告。

## Task 19：安全、权限、脱敏与配置开关

**涉及文件**

- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaValueMasker.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaRuntimeProperties.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/FormulaController.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaExecutionLogService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaLookupResolver.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/formula/FormulaCrossObjectResolver.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/formula/FormulaValueMaskerTest.java`

**执行要点**

- 执行日志脱敏手机号、身份证、银行卡、Token、AK/SK。
- 调试器、执行日志、函数市场接口增加权限注解或复用对象设计权限校验。
- 函数执行超时、日志成功采样率、debug 日志开关纳入配置。
- LOOKUP / 跨对象查询禁止直接使用表名或 SQL。

**验证命令**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Dtest=FormulaValueMaskerTest,FormulaControllerTest
```

**验收标准**

- 日志详情中敏感值已脱敏。
- 无设计权限用户不能访问调试器和函数市场管理接口。
- 函数超时返回明确错误并写入执行日志。

**执行记录**

- 2026-06-14 新增 `FormulaValueMasker`，执行日志写入统一脱敏手机号、身份证、银行卡、Token、AK/SK 等敏感值。
- 2026-06-14 新增 `FormulaRuntimeProperties`，将执行日志总开关、失败/成功/debug 日志开关、debug 输入/输出快照和函数 timeout 纳入 `forge.formula.runtime` 配置。
- `FormulaController` 增加 `@SaCheckPermission("ai:businessObject:design")`，公式调试、执行日志、依赖图、规则编译和函数市场接口统一复用对象设计权限。
- `FormulaLookupResolver`、`FormulaCrossObjectResolver` 和 `FormulaValidationService` 补齐配置校验，拒绝 SQL 片段、非法字段编码、路径式 LOOKUP relationCode，并要求 LOOKUP 目标/返回字段解析到目标模型字段资产或 `id`。
- 新增/扩展 `FormulaValueMaskerTest`、`FormulaExecutionLogServiceTest`、`FormulaFunctionRegistryTest`、`FormulaValidationServiceTest`，并通过临时 runner `/private/tmp/FormulaSecurityHardeningSmoke.java` 验证脱敏、timeout 和非法配置阻断。

## Task 20：自动化测试与执行日志回填

**涉及文件**

- Create: `code-copilot/changes/formula-next-capabilities/test-spec.md`
- Create: `code-copilot/changes/formula-next-capabilities/execution-log.md`
- Modify: `code-copilot/changes/formula-next-capabilities/tasks.md`
- Read: `code-copilot/rules/automated-testing-standard.md`

**执行要点**

- 按阶段维护测试计划，不等全量完成后再补日志。
- 每个 Phase 完成后追加 execution-log。
- 后端至少执行 generator 模块相关单测。
- 前端至少执行 ESLint 和 build。
- 若浏览器工具不可用，记录降级验证方式。

**推荐验证矩阵**

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
git diff --check -- code-copilot/changes/formula-next-capabilities forge-server forge-admin-ui
```

**验收标准**

- `test-spec.md` 覆盖后端、前端、数据库和跳过项。
- `execution-log.md` 记录命令、结果、警告、失败项和服务清理情况。
- `tasks.md` 状态随实现推进更新。

**执行记录**

- 2026-06-14 已按 `code-copilot/rules/automated-testing-standard.md` 复用当前 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md` 执行 Task 19 增量验证。
- `test-spec.md` 已追加 Task 19/20 验证矩阵，覆盖后端编译、测试源码编译、smoke runner、空白检查和跳过项。
- `execution-log.md` 已追加 Task 19/20 执行记录，记录 Maven 父 POM 跳过 Surefire、测试源码编译、smoke 输出、空白检查和服务清理情况。

## 工时估算

| 阶段 | 任务 | 预计 |
|------|------|------|
| Phase 1 | Task 1-7 | 4-6 天 |
| Phase 2 | Task 8-12 | 5-8 天 |
| Phase 3 | Task 13-14 | 2-3 天 |
| Phase 4 | Task 15-18 | 4-6 天 |
| Hardening / Validation | Task 19-20 | 2-3 天 |
| 合计 | Task 1-20 | 17-26 天 |

## 推荐落地顺序

1. Task 1-6：先完成后端可观测性闭环。
2. Task 7：补齐前端调试、日志和依赖图入口。
3. Task 8-12：上线 LOOKUP 和一跳跨对象公式。
4. Task 13-14：上线条件规则设计器。
5. Task 15-18：上线函数注册中心和函数市场。
6. Task 19-20：贯穿每个阶段持续执行，不应留到最后一次性处理。
