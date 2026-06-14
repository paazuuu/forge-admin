# 公式能力扩展 — 执行日志

> change: formula-capability-extension
> created: 2026-06-12
> status: apply (Phase 5B-2 完成)

---

## 2026-06-12 | 项目初始化 & Phase 1 启动

| 时间 | 阶段 | 操作 | 结果 |
|------|------|------|------|
| 2026-06-12 | 初始化 | 创建 spec 文档目录结构 | docs/changes/ → code-copilot/changes/ 迁移 |
| 2026-06-12 | Spec | 生成 spec.md | code-copilot/changes/formula-capability-extension/spec.md |
| 2026-06-12 | Spec | 生成 tasks.md | code-copilot/changes/formula-capability-extension/tasks.md |
| 2026-06-12 | Phase 1 | 调整计划：FormulaDependencyAnalyzer 提前到第一阶段 | 第一阶段仅做领域模型和依赖分析器 |
| 2026-06-12 | Phase 1 | FormulaConfig 从 DTO 调整为领域模型 | 纯领域模型，不含持久化注解 |
| 2026-06-12 | Phase 1 | 第一阶段禁止项明确 | 禁止 Aviator/CRUD Hook/PublishService/前端/API |

---

## 2026-06-12 | Phase 1 实现完成

| 时间 | 任务 | 产出 | 说明 |
|------|------|------|------|
| 2026-06-12 | T1.1-T1.7 | 7 个源文件 + 1 个测试文件 | FormulaType/Mode, AggregateFunction, FormulaConfig, AggregateConfig, FormulaDependencyAnalyzer |
| 2026-06-12 | 测试修复 #1 | @Tag("dev") 注解添加 | Maven surefire 默认不运行带标签的测试，增加 `<groups>dev</groups>` 后 Tests run: 0 |
| 2026-06-12 | 测试修复 #2 | 测试发现配置 | 添加 surefire `<groups>dev</groups>` 配置解决 |
| 2026-06-12 | 测试修复 #3 | 深度校验逻辑 | depthExactly3 测试：间接嵌套 depth=3 应通过，需修正 computeDepth 使用 maxDepth 比较 |
| 2026-06-12 | 测试修复 #4 | buildPrereqAdjacency 方向修正 | 边方向应为 B→A（B 先计算，A 后计算），确保 Kahn 排序正确 |
| 2026-06-12 | 测试修复 #5 | computeDepth 方向修正 | 改为使用 FormulaConfig.getDependsOn() 方向计算深度 |
| 2026-06-12 | 测试修复 #6 | findCycleFrom 方向修正 | 转换为 dependsOn 邻接表后再做 DFS 环检测 |
| 2026-06-12 | Phase 1 完成 | 14/14 测试通过 | 0 失败 0 错误 |

---

## 2026-06-12 | Phase 2A 表达式解析与校验

| 时间 | 任务 | 产出 | 说明 |
|------|------|------|------|
| 2026-06-12 | T2A.1 | AviatorAdapter | 封装 Aviator 编译/执行，错误转 FormulaExecutionException |
| 2026-06-12 | T2A.2 | ExpressionParser | 解析表达式提取变量，语法校验，依赖交叉检查 |
| 2026-06-12 | T2A.3 | FormulaValidationService | 整合三层校验：语法/依赖一致性/DAG分析 |
| 2026-06-12 | Phase 2A 完成 | 全部测试通过 | 公式校验链路打通 |

---

## 2026-06-12 | Phase 3A 执行引擎

| 时间 | 任务 | 产出 | 说明 |
|------|------|------|------|
| 2026-06-12 | T3A.1 | ExpressionExecutor | Aviator 表达式执行封装，支持变量绑定 |
| 2026-06-12 | T3A.2 | FormulaExecutionEngine | 按 DAG 拓扑顺序执行公式，支持 3 层嵌套 |
| 2026-06-12 | T3A.3 | ExecutionResult | 执行结果模型：value/errors/elapsedMs |
| 2026-06-12 | T3A.4 | FormulaErrorHandler | 策略模式错误处理，支持降级 |
| 2026-06-12 | Phase 3A 完成 | 全部测试通过 | 禁止 AGGREGATE 静默跳过已修复 |

---

## 2026-06-12 | Phase 3B 聚合引擎

| 时间 | 任务 | 产出 | 说明 |
|------|------|------|------|
| 2026-06-12 | T3B.1 | AggregateFunctionExecutor | 支持 SUM/COUNT/AVG/MAX/MIN |
| 2026-06-12 | T3B.2 | AggregateEngine | 编排：获取数据→过滤→聚合→返回 |
| 2026-06-12 | T3B.3 | AggregateResult | 结果模型：value/totalRowCount/matchedRowCount/errors |
| 2026-06-12 | T3B.4 | AggregateValidation | 校验 aggregate config 完整性 + filter 语法 |
| 2026-06-12 | Phase 3B 完成 | 130 tests 全部通过 | |

---

## 2026-06-12 | Phase 1-3B 架构审查

| 时间 | 审查项 | 结论 |
|------|--------|------|
| 2026-06-12 | FormulaConfig 扩展性 | ✅ 通过 |
| 2026-06-12 | AggregateConfig 兼容性 | ✅ 通过 |
| 2026-06-12 | FormulaExecutionEngine 可扩展性 | ✅ 通过 |
| 2026-06-12 | Phase4 集成风险 | ⚠️ 需要在 review 后确认 |
| 2026-06-12 | 审查报告 R1 | FormulaExecutionEngine 不允许静默跳过 AGGREGATE → 已修复 |

---

## 2026-06-12 | Phase 4A Runtime 接入

| 时间 | 任务 | 产出 | 说明 |
|------|------|------|------|
| 2026-06-12 | T4A.1 | FormulaRuntimeContext | 运行时上下文：tenantId/suiteCode/sourceObjectCode/fieldValues |
| 2026-06-12 | T4A.2 | DbAggregateDataProvider | DB 数据提供者，通过 AiBusinessObjectRelation 映射 |
| 2026-06-12 | T4A.3 | FormulaPublishValidator | 发布时公式校验拦截器 |
| 2026-06-12 | T4A.4 | AggregateDataProvider 接口 | 数据源抽象接口 |
| 2026-06-12 | Phase 4A 完成 | 157 tests 全部通过 | 不假设 relationCode = targetObjectCode |

---

## 2026-06-12 | Phase 4B 持久化接入

| 时间 | 任务 | 产出 | 说明 |
|------|------|------|------|
| 2026-06-12 | T4B.1 | LowcodeFieldSchema.formulaConfig | 字段 Schema 增加 formulaConfig 字段 |
| 2026-06-12 | T4B.2 | GenTableColumn.formulaConfig | 数据库实体增加 formula_config 列 |
| 2026-06-12 | T4B.3 | DB Migration V1.0.67 | gen_table_column 增加 formula_config TEXT |
| 2026-06-12 | T4B.4 | StoredFormulaRuntime | STORED 模式：保存时计算并持久化 |
| 2026-06-12 | T4B.5 | VirtualFormulaRuntime | VIRTUAL 模式：读取时动态计算 |
| 2026-06-12 | Phase 4B 完成 | 169 tests 全部通过 | |

---

## 2026-06-12 | 技术债修复 R2

| 时间 | 修复项 | 说明 |
|------|--------|------|
| 2026-06-12 | I1: FormulaExecutionEngine 强制注入 DataProvider | 构造器注入改为强制参数 |
| 2026-06-12 | I2: AggregateEngine 接入 FormulaRuntimeContext | 支持运行时上下文传递 |
| 2026-06-12 | I3: 抽取 AbstractFormulaRuntime | 消除 Stored/Virtual 88% 代码重复 |
| 2026-06-12 | R2 完成 | 169 tests 全部通过 |

---

## 2026-06-12 | Phase 5A Formula API

| 时间 | 任务 | 产出 | 说明 |
|------|------|------|------|
| 2026-06-12 | T5A.1 | FormulaController | 4 个 REST 端点 |
| 2026-06-12 | T5A.2 | POST /api/ai/business/formula/validate | 公式语法校验 |
| 2026-06-12 | T5A.3 | POST /api/ai/business/formula/preview | 公式预览计算 |
| 2026-06-12 | T5A.4 | POST /api/ai/business/formula/dependency | 依赖分析 |
| 2026-06-12 | T5A.5 | GET /api/ai/business/formula/functions | 可用函数列表 |
| 2026-06-12 | T5A.6 | 8 个 DTO | 独立 DTO，OpenAPI 注解 |
| 2026-06-12 | Phase 5A 完成 | 177 tests 全部通过 | |

---

## 2026-06-12 | Phase 5B-1 CALC 公式配置 UI

| 时间 | 任务 | 产出 | 说明 |
|------|------|------|------|
| 2026-06-12 | 前端调研 | 前端目录结构 & 改造点 | BusinessFieldPropertyPanel.vue 增加公式面板 |
| 2026-06-12 | UI 实现 | 公式配置折叠面板 | FormulaType 选择 / FormulaMode 切换 / 表达式输入 / 依赖字段选择 |
| 2026-06-12 | 校验集成 | 前端调用 POST /formula/validate | 语法校验 + 结果展示 |
| 2026-06-12 | 预览集成 | 前端调用 POST /formula/preview | 样例值输入 + 计算结果展示 |
| 2026-06-12 | 保存链路 | formulaConfig → LowcodeFieldSchema | 公式配置保存到字段 Schema |
| 2026-06-12 | Phase 5B-1 完成 | allFields 数据适配 | |

---

## 2026-06-12 | UAT 验收

| UAT | 场景 | 结果 |
|-----|------|------|
| UAT-01 | STORED 公式：amount = price * quantity | ✅ PASS |
| UAT-02 | VIRTUAL 公式：amount = price * quantity | ✅ PASS |
| UAT-03 | 三层语法校验：price ** | ✅ PASS |
| UAT-04 | 循环依赖：A = B+1, B = A+1 | ✅ PASS |
| UAT-05 | 删除依赖字段 quantity → 发布阻断 | ⚠️ PARTIAL (GAP) |

---

## 2026-06-12 | UAT-05 修复

| 时间 | 修复 | 说明 |
|------|------|------|
| 2026-06-12 | FormulaDependencyAnalyzer.validateDependencyFields() | 新增静态方法，校验 dependsOn 字段存在性 |
| 2026-06-12 | FormulaPublishValidator.collectFieldNames() | 收集 Schema 中所有字段名 |
| 2026-06-12 | FormulaPublishValidatorTest.MissingDependencies | 新增 4 个测试：单缺失/多缺失/全存在/公式依赖公式 |
| 2026-06-12 | 测试回归修复 | validCalc 和 chainedFormulas 测试 schema 补全缺失字段 |
| 2026-06-12 | UAT-05 修复完成 | 181 tests 全部通过 |

---

## 2026-06-12 | Phase 5B-2 AGGREGATE 公式配置 UI

| 时间 | 任务 | 产出 | 说明 |
|------|------|------|------|
| 2026-06-12 | UI 实现 | AGGREGATE 公式面板 | 聚合函数选择(SUM/COUNT/AVG/MAX/MIN) + 关联对象 + 目标字段 + 过滤条件 |
| 2026-06-12 | 关联对象选择 | aggregateRelationOptions computed | 从 allFields 中筛选 REFERENCE 类型字段 |
| 2026-06-12 | 保存链路 | buildFormulaConfigPayload | AGGREGATE 类型写入 config.aggregate |
| 2026-06-12 | 表单状态 | createFieldForm / onFormulaTypeChange | 初始化和重置 aggregate 字段 |
| 2026-06-12 | Phase 5B-2 完成 | 181 tests 全部通过 | 仅修改 BusinessFieldPropertyPanel.vue |

---

## 2026-06-12 | Phase 5B-2 UAT 验收

| UAT | 场景 | 结果 |
|-----|------|------|
| UAT-AGG-01 | SUM 聚合：totalAmount = SUM(order.amount) | ✅ PASS (后端+UI) |
| UAT-AGG-02 | COUNT 聚合：orderCount = COUNT(order.id) | ✅ PASS (后端+UI) |
| UAT-AGG-03 | AVG 聚合：avgAmount = AVG(order.amount) | ✅ PASS (后端+UI) |
| UAT-AGG-04 | 过滤条件：filter status == 'ACTIVE' | ✅ PASS (后端+UI) |
| UAT-AGG-05 | 关联对象缺失 | ⚠️ PARTIAL — AggregateValidation 存在但未集成到发布链路 |
| UAT-AGG-06 | 目标字段缺失 | ⚠️ PARTIAL — 同上 |

---

## 当前状态总结

### 测试统计
| 里程碑 | 测试数 |
|--------|--------|
| Phase 1 完成 | 14 |
| Phase 1-3B | 130 |
| Phase 4A | 157 |
| Phase 4B | 169 |
| Phase 5A | 177 |
| UAT-05 修复 | 181 |
| Phase 5B-2 | 181 |

### 已实现功能
- ✅ CALC 公式配置 (STORED/VIRTUAL)
- ✅ AGGREGATE 公式配置 (SUM/COUNT/AVG/MAX/MIN + 过滤)
- ✅ 表达式语法校验 (前端 + API + 发布三层)
- ✅ 公式预览计算
- ✅ 循环依赖检测
- ✅ 嵌套深度校验 (max 3)
- ✅ 依赖字段缺失检测 (发布阻断)
- ✅ formulaConfig 持久化到 LowcodeFieldSchema / GenTableColumn
- ✅ StoredFormulaRuntime / VirtualFormulaRuntime
- ✅ AbstractFormulaRuntime (消除重复)
- ✅ FormulaController (4 REST 端点)
- ✅ DbAggregateDataProvider (通过 AiBusinessObjectRelation 映射)

### 已知 Gap
- ⚠️ UAT-AGG-05/06: AggregateValidation 未集成到 FormulaPublishValidator
  - AggregateValidation 类存在且功能完整，但 publish 链路未调用

### 未实现 (按计划禁止)
- ❌ CONDITIONAL UI
- ❌ Dependency Graph 可视化
- ❌ 跨对象公式 (V1 不支持)

### 修改文件清单
| 层级 | 文件 | 变更类型 |
|------|------|----------|
| 后端 domain | FormulaType.java | 新增 |
| 后端 domain | FormulaMode.java | 新增 |
| 后端 domain | AggregateFunction.java | 新增 |
| 后端 domain | FormulaConfig.java | 新增 |
| 后端 domain | AggregateConfig.java | 新增 |
| 后端 domain | FormulaDependencyAnalyzer.java | 新增 |
| 后端 domain | AggregateEngine.java | 新增 |
| 后端 domain | AggregateFunctionExecutor.java | 新增 |
| 后端 domain | AggregateResult.java | 新增 |
| 后端 domain | AggregateValidation.java | 新增 |
| 后端 domain | AggregateDataProvider.java | 新增 |
| 后端 domain | DependencyAnalysisResult.java | 新增 |
| 后端 domain | ExpressionExecutor.java | 新增 |
| 后端 domain | ExecutionResult.java | 新增 |
| 后端 domain | FormulaErrorHandler.java | 新增 |
| 后端 service | AviatorAdapter.java | 新增 |
| 后端 service | ExpressionParser.java | 新增 |
| 后端 service | FormulaValidationService.java | 新增 |
| 后端 service | FormulaPublishValidator.java | 新增 |
| 后端 service | FormulaExecutionEngine.java | 新增 |
| 后端 service | DbAggregateDataProvider.java | 新增 |
| 后端 service | FormulaRuntimeContext.java | 新增 |
| 后端 service | StoredFormulaRuntime.java | 新增 |
| 后端 service | VirtualFormulaRuntime.java | 新增 |
| 后端 service | AbstractFormulaRuntime.java | 新增 |
| 后端 controller | FormulaController.java | 新增 |
| 后端 dto | LowcodeFieldSchema.java | 修改 (+formulaConfig) |
| 后端 dto | GenTableColumn.java | 修改 (+formulaConfig) |
| 后端 db | V1.0.67__add_formula_config.sql | 新增 |
| 后端 dependency | pom.xml (aviator) | 修改 |
| 前端 | BusinessFieldPropertyPanel.vue | 修改 (+公式面板 +AGGREGATE UI) |
| 前端 | src/api/formula.js | 新增 |

---

## 下一步

1. **修复 UAT-AGG-05/06 Gap**: 集成 AggregateValidation 到 FormulaPublishValidator
2. **Phase 5B-3**: CONDITIONAL 公式配置 UI
3. **Phase 5C**: Dependency Graph 可视化
---

## 2026-06-12 | R1 修复 — AggregateValidation 集成

| 时间 | 修复 | 说明 |
|------|------|------|
| 2026-06-12 | AggregateDataException | 新增异常类，用于运行时数据提供者错误 |
| 2026-06-12 | DbAggregateDataProvider | 6处 silent empty → throw AggregateDataException |
| 2026-06-12 | AggregateFunctionExecutor | computeSum/computeAvg: all-null targetField → throw |
| 2026-06-12 | AggregateEngine | execute(config,context) 捕获 AggregateDataException → ErrorResult |
| 2026-06-12 | FormulaPublishValidator | 集成 AggregateValidation.validateFormula() |
| 2026-06-12 | FormulaPublishValidator.validateAggregateFormulas() | 新增方法，遍历 AGGREGATE config 合并校验错误 |
| 2026-06-12 | FormulaPublishValidatorTest.AggregateConfigValidation | 新增 5 个测试 |
| 2026-06-12 | DbAggregateDataProviderTest | 7 个 invalid input 测试改为 assertThrows |
| 2026-06-12 | R1 修复完成 | **186 tests, 0 failures, BUILD SUCCESS** |

### 风险状态

| 风险 | 状态 |
|------|------|
| R1: AggregateValidation 未集成 | ✅ 已关闭 |
| R2: 缺失 relation 静默返回空 | ✅ 已关闭 (抛 AggregateDataException) |
| R3: 缺失 targetField 静默跳过 | ✅ 已关闭 (抛 AggregateExecutionException) |
