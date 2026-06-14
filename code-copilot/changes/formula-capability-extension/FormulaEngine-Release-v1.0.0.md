# ForgeAdmin 公式引擎 V1.0.0 交付文档

> 版本: v1.0.0 | 日期: 2026-06-12 | 状态: READY FOR RELEASE
> 分支: feature/formula-capability-extension | 提交: e8883cd7

---

## 1. 项目概述

### 为什么开发公式引擎

ForgeAdmin 已具备 BusinessObject、LowcodeModel、Runtime、动态CRUD、对象关系等能力，但缺少统一的公式计算体系。以下场景需要业务代码手写实现：

| 场景 | 当前方式 | 问题 |
|------|---------|------|
| 金额自动计算 (`total = price * quantity`) | 前端/后端业务代码 | 重复编码，易出错 |
| 主从表汇总 (`SUM(order.amount)`) | 手动查询从表再计算 | 代码分散，维护困难 |
| 条件字段赋值 (`IF amount>1000 THEN VIP`) | 硬编码判断 | 规则变更需发版 |
| 公式间依赖和嵌套 | 手动管理执行顺序 | 循环依赖无检测 |

### 业务价值

| 维度 | 价值 |
|------|------|
| **效率提升** | 字段值通过配置公式自动计算，无需手写代码 |
| **数据一致性** | 金额计算、主从表汇总自动维护，消除手工维护风险 |
| **灵活扩展** | 公式配置可视化，业务规则变更无需发版 |
| **安全可靠** | 发布时自动检测循环依赖、嵌套深度、字段缺失 |

---

## 2. 整体架构设计

### 公式引擎在 ForgeAdmin 中的位置

```
ForgeAdmin
├── forge-admin-ui (Vue 3 + Naive UI)
│   └── BusinessFieldPropertyPanel.vue  ← 公式配置界面 (CALC/AGGREGATE/CONDITIONAL)
│
├── forge-server
│   ├── FormulaController              ← REST API (/formula/validate|preview|dependency|functions)
│   ├── service/formula/
│   │   ├── FormulaPublishValidator    ← 发布时校验拦截器
│   │   ├── FormulaValidationService   ← 公式校验服务 (语法 + DAG + 循环)
│   │   ├── FormulaExecutionEngine     ← 公式执行引擎 (拓扑排序 → 逐字段执行)
│   │   ├── AbstractFormulaRuntime     ← 运行时基类 (STORED/VIRTUAL 模板方法)
│   │   ├── StoredFormulaRuntime       ← 保存时计算并持久化
│   │   ├── VirtualFormulaRuntime      ← 查询时动态计算
│   │   └── DbAggregateDataProvider    ← 聚合公式数据提供者
│   └── domain/formula/
│       ├── FormulaConfig              ← 公式配置领域模型
│       ├── AggregateConfig            ← 聚合配置
│       ├── ConditionConfig            ← 条件配置
│       ├── AggregateEngine            ← 聚合计算引擎
│       ├── AggregateFunctionExecutor  ← SUM/COUNT/AVG/MAX/MIN 执行器
│       └── FormulaDependencyAnalyzer  ← DAG 拓扑排序 + 循环检测
```

### 架构图

```
┌──────────────────────────────────────────────────────────┐
│                    前端设计器 (Vue 3)                      │
│  BusinessFieldPropertyPanel                              │
│  ├── CALC UI       (表达式 + 依赖字段)                    │
│  ├── AGGREGATE UI  (聚合函数 + 关联对象 + 过滤)            │
│  └── CONDITIONAL UI (条件 + trueValue + falseValue)       │
└──────────────────────┬───────────────────────────────────┘
                       │ formulaConfig JSON
                       ▼
┌──────────────────────────────────────────────────────────┐
│              发布校验 (FormulaPublishValidator)            │
│  ├── 依赖字段存在性                                        │
│  ├── 公式语法校验                                          │
│  ├── DAG 循环依赖检测                                       │
│  ├── 嵌套深度校验 (max 3)                                   │
│  ├── AGGREGATE 配置完整性                                   │
│  └── CONDITIONAL 配置完整性                                 │
└──────────────────────┬───────────────────────────────────┘
                       │ LowcodeFieldSchema.formulaConfig
                       ▼
┌──────────────────────────────────────────────────────────┐
│               持久化 (GenTableColumn + DB)                 │
│  ALTER TABLE gen_table_column ADD COLUMN formula_config   │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│                   Runtime 执行                             │
│  ┌──────────────────────────────────────────────────┐    │
│  │  AbstractFormulaRuntime                           │    │
│  │  ├── StoredFormulaRuntime  (保存时执行)            │    │
│  │  └── VirtualFormulaRuntime (查询时执行)            │    │
│  │       │                                           │    │
│  │       ▼                                           │    │
│  │  FormulaExecutionEngine                           │    │
│  │  ├── DependencyAnalyzer.analyze()  → 拓扑排序      │    │
│  │  ├── 按拓扑序逐字段执行:                            │    │
│  │  │   ├── CALC        → ExpressionExecutor         │    │
│  │  │   ├── AGGREGATE   → AggregateEngine            │    │
│  │  │   │   └── DbAggregateDataProvider → DB查询      │    │
│  │  │   └── CONDITIONAL → 条件求值 → 三元分支          │    │
│  │  └── 结果回写 record                               │    │
│  └──────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────┘
```

---

## 3. 功能清单

| 功能 | 状态 | 说明 |
|------|------|------|
| CALC 公式 | ✅ | 字段值由表达式计算，如 `total = price * quantity` |
| AGGREGATE 公式 | ✅ | 主表字段聚合从表数据，支持 SUM/COUNT/AVG/MAX/MIN |
| CONDITIONAL 公式 | ✅ | 条件表达式 + trueValue/falseValue 三元分支 |
| STORED 模式 | ✅ | 保存时计算并持久化到数据库 |
| VIRTUAL 模式 | ✅ | 查询时实时计算，不存储 |
| 公式预览 | ✅ | `POST /formula/preview` 输入样例值预览结果 |
| 语法校验 | ✅ | 前端 + API + 发布三层校验 |
| 依赖分析 | ✅ | DAG 拓扑排序，自动提取表达式变量 |
| 循环依赖检测 | ✅ | Kahn 算法检测环，发布时拦截 |
| 嵌套深度限制 | ✅ | max 3 层，超限发布拦截 |
| 发布校验 | ✅ | 统一入口，CALC/AGGREGATE/CONDITIONAL 全覆盖 |
| AGGREGATE 过滤 | ✅ | 支持 Aviator 过滤表达式，如 `amount > 0` |
| 聚合关联映射 | ✅ | 通过 AiBusinessObjectRelation 解析 relationCode |
| 错误处理 | ✅ | 异常降级 + ErrorResult，禁止静默失败 |
| Aviator 函数库 | ✅ | 18+ 内置函数 (math/string/collection/date) |

---

## 4. 数据模型设计

### FormulaConfig 结构

```json
{
  "type": "CALC",
  "mode": "STORED",
  "expression": "price * quantity",
  "dependsOn": ["price", "quantity"],
  "aggregate": null,
  "condition": null
}
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | String | 是 | 公式类型: `CALC` / `AGGREGATE` / `CONDITIONAL` |
| `mode` | String | 是 | 计算模式: `STORED` (保存持久化) / `VIRTUAL` (查询计算) |
| `expression` | String | CALC时必填 | Aviator 表达式 |
| `dependsOn` | String[] | 否 | 依赖字段名列表，用于 DAG 分析和循环检测 |
| `aggregate` | Object | AGGREGATE时必填 | 聚合配置 `{ function, relationCode, targetField, filter }` |
| `condition` | Object | CONDITIONAL时必填 | 条件配置 `{ expression, trueValue, falseValue }` |

### AggregateConfig

```json
{
  "function": "SUM",
  "relationCode": "42",
  "targetField": "amount",
  "filter": "status == 'ACTIVE'"
}
```

### ConditionConfig

```json
{
  "expression": "amount > 1000",
  "trueValue": "VIP",
  "falseValue": "NORMAL"
}
```

### 持久化位置

| 表 | 字段 | 类型 | 说明 |
|----|------|------|------|
| `gen_table_column` | `formula_config` | TEXT | JSON 格式公式配置 (V1.0.67 Migration) |
| `ai_lowcode_model` | `modelSchema` JSON | - | 字段 schema 中 `LowcodeFieldSchema.formulaConfig` 节点 |

---

## 5. CALC 公式设计

### 支持能力

| 场景 | 表达式示例 | 说明 |
|------|-----------|------|
| 金额计算 | `price * quantity` | 单价 × 数量 |
| 税额计算 | `amount * taxRate / 100` | 含税率计算 |
| 折扣计算 | `originalPrice * (1 - discount)` | 折后价 |
| 数学运算 | `math.round(amount * 1.1)` | Aviator 内置函数 |
| 字符串拼接 | `prefix + '-' + code` | 字段拼接 |

### 执行流程

```
用户保存记录
  ↓
StoredFormulaRuntime.calculate(records, modelSchema, context)
  ↓
提取 STORED 模式 FormulaConfig
  ↓
FormulaExecutionEngine.execute(formulaMap, rowContext)
  ↓
DependencyAnalyzer.analyze() → 拓扑排序
  ↓
按拓扑序逐字段: ExpressionExecutor.execute("price * quantity", context)
  ↓
结果回写 record["total"] = 300
  ↓
持久化到数据库
```

### 嵌套公式示例

```
A = price * quantity
B = A * taxRate
C = B + shipping
```

`FormulaDependencyAnalyzer` 检测: A→B→C 合法 (depth=3)，C→A 循环则拦截。

---

## 6. AGGREGATE 公式设计

### 支持函数

| 函数 | 说明 | 示例 |
|------|------|------|
| SUM | 求和 | `SUM(order.amount)` |
| COUNT | 计数 | `COUNT(order.id)` |
| AVG | 平均值 | `AVG(order.amount)` |
| MAX | 最大值 | `MAX(order.price)` |
| MIN | 最小值 | `MIN(order.price)` |

### AGGREGATE 执行流程

```
FormulaExecutionEngine.executeAggregate()
  ↓
AggregateConfig: { function: "SUM", relationCode: "42", targetField: "amount", filter: "amount > 0" }
  ↓
AggregateEngine.execute(aggConfig, context)
  ↓
DbAggregateDataProvider.getDetailRecords("42", context)
  ├── relationCode → Long.parseLong("42") → relationId=42
  ├── BusinessObjectRelationMapper → AiBusinessObjectRelation
  ├── BusinessObjectMapper → AiBusinessObject (目标对象)
  ├── AiLowcodeModelMapper → 物理表名
  └── DetailRecordFetcher → 查询从表记录
  ↓
AggregateFunctionExecutor.execute(SUM, "amount", rows)
  ├── 遍历 rows 累加 amount
  └── filter 过滤 (Aviator 表达式)
  ↓
AggregateResult: { value: 350, totalRowCount: 5, matchedRowCount: 3 }
  ↓
回写主表字段
```

### 关键设计点

- 不假设 `relationCode == targetObjectCode`
- 通过 `AiBusinessObjectRelation.sourceFieldCode` 确定 JOIN 字段
- `filter` 为空则全量聚合
- 目标字段全为 null 时抛出 `AggregateExecutionException` (禁止静默返回 0)

---

## 7. CONDITIONAL 公式设计

### 支持能力

| 场景 | 配置 | 输入 | 输出 |
|------|------|------|------|
| 客户等级 | `amount > 1000`, `"VIP"`, `"NORMAL"` | amount=1200 | VIP |
| 客户等级 | `amount > 1000`, `"VIP"`, `"NORMAL"` | amount=500 | NORMAL |
| 布尔标记 | `status == "ACTIVE"`, `1`, `0` | status=ACTIVE | 1 |
| 数字计算 | `price > 100`, `price * 0.9`, `price` | price=200 | 180 |

### 执行流程

```
FormulaExecutionEngine.executeConditional()
  ↓
ConditionConfig: { expression: "amount > 1000", trueValue: "VIP", falseValue: "NORMAL" }
  ↓
ExpressionExecutor.execute("amount > 1000", context) → true
  ↓
isTruthy(true) → true
  ↓
value = trueValue = "VIP"
  ↓
回写 record["level"] = "VIP"
```

### 错误处理

- condition.expression 语法错误 → `FormulaExecutionException` → 回退 `falseValue`
- condition 为 null → ErrorResult: "missing condition config"
- trueValue/falseValue 为 null → 正常返回 null (合法值)

---

## 8. Runtime 执行流程

```
┌─────────────┐
│  用户操作     │
│ Save/Query   │
└──────┬──────┘
       ▼
┌─────────────────────────────────────────┐
│  AbstractFormulaRuntime.calculate()      │
│  ├── StoredFormulaRuntime (STORED模式)   │
│  └── VirtualFormulaRuntime (VIRTUAL模式) │
└──────┬──────────────────────────────────┘
       ▼
┌─────────────────────────────────────────┐
│  extractFormulas(modelSchema)            │
│  从 LowcodeFieldSchema.formulaConfig     │
│  按模式过滤 STORED / VIRTUAL              │
└──────┬──────────────────────────────────┘
       ▼
┌─────────────────────────────────────────┐
│  对每条 record:                           │
│  ┌───────────────────────────────────┐  │
│  │ FormulaExecutionEngine.execute()  │  │
│  │  ├── DependencyAnalyzer.analyze() │  │
│  │  │   ├── buildPrereqAdjacency()   │  │
│  │  │   ├── computeIndegree()        │  │
│  │  │   ├── kahnSort()              │  │
│  │  │   └── computeDepth()          │  │
│  │  ├── 循环检测 → 拦截              │  │
│  │  └── 按拓扑序执行每个公式:          │  │
│  │      ├── CALC        → expr()    │  │
│  │      ├── AGGREGATE   → 聚合查询   │  │
│  │      └── CONDITIONAL → 条件分支   │  │
│  └───────────────────────────────────┘  │
│  结果回写 record[fieldName] = value       │
└──────┬──────────────────────────────────┘
       ▼
┌─────────────┐
│  返回 records │
│  (公式字段已填充)│
└─────────────┘
```

---

## 9. 发布校验机制

### FormulaPublishValidator 校验链路

```
FormulaPublishValidator.validate(LowcodeModelSchema)
  │
  ├── 1. extractFormulas(modelSchema)
  │     从字段 schema 提取所有 FormulaConfig
  │
  ├── 2. 依赖字段存在性校验 (UAT-05)
  │     FormulaDependencyAnalyzer.validateDependencyFields()
  │     每个 dependsOn 字段必须在 schema 或公式字段中存在
  │     缺失 → ValidationResult.addError()
  │
  ├── 3. 公式语法 + DAG 校验
  │     FormulaValidationService.validate(formulaMap)
  │     ├── Aviator 表达式语法解析
  │     ├── 依赖一致性交叉检查
  │     ├── DAG 循环依赖检测 (Kahn)
  │     └── 嵌套深度校验 (max 3)
  │
  └── 4. AGGREGATE/CONDITIONAL 配置完整性
        validateAggregateFormulas(formulaMap)
        ├── AggregateValidation.validateFormula()
        │   ├── relationCode 非空
        │   ├── function 非空
        │   ├── COUNT 以外: targetField 非空
        │   └── filter 语法校验
        └── ConditionConfig
            └── expression 非空
```

---

## 10. API 文档

### POST /api/ai/business/formula/validate

校验公式表达式语法。

**请求**:
```json
{
  "expression": "price * quantity",
  "type": "CALC",
  "dependsOn": ["price", "quantity"]
}
```

**返回**:
```json
{
  "code": 200,
  "data": {
    "valid": true,
    "variables": ["price", "quantity"],
    "errorMessage": null,
    "dependencyWarnings": []
  }
}
```

---

### POST /api/ai/business/formula/preview

预览公式计算结果。

**请求**:
```json
{
  "expression": "price * quantity",
  "type": "CALC",
  "dependsOn": ["price", "quantity"],
  "sampleValues": { "price": 100, "quantity": 3 }
}
```

**返回**:
```json
{
  "code": 200,
  "data": {
    "success": true,
    "result": 300,
    "elapsedMs": 5
  }
}
```

**CONDITIONAL 预览**:
```json
{
  "expression": "amount > 1000",
  "type": "CONDITIONAL",
  "condition": {
    "expression": "amount > 1000",
    "trueValue": "VIP",
    "falseValue": "NORMAL"
  },
  "sampleValues": { "amount": 1200 }
}
```
→ 返回 `"result": "VIP"`

---

### POST /api/ai/business/formula/dependency

分析公式依赖关系。

**请求**:
```json
{
  "formulas": [
    { "fieldName": "total", "expression": "price * quantity", "dependsOn": ["price", "quantity"] },
    { "fieldName": "grandTotal", "expression": "total * 1.1", "dependsOn": ["total"] }
  ]
}
```

**返回**:
```json
{
  "code": 200,
  "data": {
    "valid": true,
    "hasCycle": false,
    "topologicalOrder": ["price", "quantity", "total", "grandTotal"],
    "depthMap": { "total": 2, "grandTotal": 3 }
  }
}
```

---

### GET /api/ai/business/formula/functions

获取可用 Aviator 函数列表。

**返回**:
```json
{
  "code": 200,
  "data": [
    { "name": "math.abs", "category": "Math", "description": "绝对值", "example": "math.abs(-5) → 5" },
    { "name": "math.round", "category": "Math", "description": "四舍五入", "example": "math.round(3.6) → 4" },
    { "name": "string.length", "category": "String", "description": "字符串长度", "example": "string.length('hello') → 5" }
  ]
}
```

---

## 11. 前端设计

### 修改文件

`forge-admin-ui/src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue`

### 公式类型选择

三种公式类型下拉框: `CALC` / `AGGREGATE` / `CONDITIONAL`

### CALC 配置界面

| 配置项 | 组件 | 说明 |
|--------|------|------|
| 计算模式 | `<n-radio-group>` | STORED / VIRTUAL |
| 表达式 | `<n-input>` textarea | 如 `price * quantity` |
| 依赖字段 | `<n-select>` multiple | 从当前对象字段中选择 |
| 语法验证 | `<n-button>` | 调用 `POST /formula/validate` |
| 预览计算 | `<n-button>` | 输入样例值，调用 `POST /formula/preview` |

### AGGREGATE 配置界面

| 配置项 | 组件 | 说明 |
|--------|------|------|
| 聚合函数 | `<n-select>` | SUM / COUNT / AVG / MAX / MIN |
| 关联对象 | `<n-select>` | 从 REFERENCE 类型字段中选择 |
| 目标字段 | `<n-input>` | 从表字段名，如 `amount` |
| 过滤条件 | `<n-input>` | 可选，如 `status == 'ACTIVE'` |

### CONDITIONAL 配置界面

| 配置项 | 组件 | 说明 |
|--------|------|------|
| 条件表达式 | `<n-input>` | 如 `amount > 1000` |
| 条件成立值 | `<n-input>` | 如 `VIP` |
| 条件不成立值 | `<n-input>` | 如 `NORMAL` |

### 保存结构

```json
{
  "type": "AGGREGATE",
  "mode": "STORED",
  "expression": "",
  "dependsOn": [],
  "aggregate": {
    "function": "SUM",
    "relationCode": "42",
    "targetField": "amount",
    "filter": "amount > 0"
  }
}
```

---

## 12. 错误处理机制

### 异常体系

```
RuntimeException
├── AggregateDataException          ← 数据提供者错误 (R2修复)
├── AggregateExecutionException     ← 聚合执行错误 (R3修复)
├── AggregateValidationException    ← 聚合配置校验错误
├── FormulaExecutionException       ← 公式执行错误
├── FormulaCompileException         ← 公式编译错误
└── FormulaValidationResult         ← 校验结果 (含 errors + warnings)
```

### 错误处理流程

```
公式执行出错
  ↓
FormulaExecutionException
  ↓
FormulaErrorHandler.handleError()
  ├── 降级策略: 返回 fallback 值
  ├── 记录 ERROR 日志
  └── 结果写入 ExecutionResult.errors
  ↓
ExecutionResult { success: false, errors: { "fieldName": ["error msg"] } }
```

### 运行时保护 (R1/R2/R3)

| 场景 | 旧行为 | 新行为 |
|------|--------|--------|
| relationCode 不存在 | warn + 返回 emptyList | throw `AggregateDataException` → `AggregateResult.error` |
| targetField 全 null | 返回 0 / null | throw `AggregateExecutionException` |
| AGGREGATE 配置缺失 | 发布成功 | 发布拦截 → ValidationResult.addError() |

---

## 13. 测试报告

### 测试总览

| 指标 | 数值 |
|------|------|
| 测试总数 | **186** |
| 失败 | **0** |
| 错误 | **0** |
| 跳过 | **0** |
| 构建结果 | **BUILD SUCCESS** |

### 测试分布

| 测试类 | 测试数 | 覆盖范围 |
|--------|--------|---------|
| `AggregateEngineTest` | 15 | SUM/COUNT/AVG/MAX/MIN、过滤、空数据、错误处理 |
| `AggregateFunctionExecutorTest` | 12 | 各聚合函数计算正确性 |
| `AggregateValidationTest` | 9 | 配置校验规则 |
| `FormulaDependencyAnalyzerTest` | 14 | DAG/循环/深度/拓扑排序 |
| `FormulaPublishValidatorTest` | 19 | 发布校验全链路 + AGGREGATE/CONDITIONAL 集成 |
| `FormulaExecutionEngineTest` | 8 | CALC/AGGREGATE/CONDITIONAL 执行 |
| `FormulaValidationServiceTest` | 10 | 三层校验 (语法/DAG/循环) |
| `DbAggregateDataProviderTest` | 10 | relationCode 解析 + 异常处理 |
| `StoredFormulaRuntimeTest` | 10 | STORED 模式计算 + 回写 |
| `VirtualFormulaRuntimeTest` | 7 | VIRTUAL 模式计算 |
| `FormulaControllerTest` | 8 | API 端点测试 |
| 其他 | 64 | ExpressionParser/AviatorAdapter/ErrorHandler 等 |

### UAT 验收结果

| 用例 | 场景 | 结果 |
|------|------|------|
| UAT-01 | STORED 公式 `amount = price * quantity` | ✅ PASS |
| UAT-02 | VIRTUAL 公式 | ✅ PASS |
| UAT-03 | 三层语法校验 | ✅ PASS |
| UAT-04 | 循环依赖检测 | ✅ PASS |
| UAT-05 | 删除依赖字段 → 发布阻断 | ✅ PASS |
| UAT-AGG-01~04 | SUM/COUNT/AVG/Filter | ✅ PASS |
| UAT-AGG-05~06 | 缺失 relation/targetField | ✅ PASS |
| AGG-REG-01~05 | R1/R2/R3 回归 | ✅ PASS |

---

## 14. 已解决风险

| 风险 | 描述 | 解决方案 |
|------|------|---------|
| **R1** | AggregateValidation 未集成到发布链路 | `FormulaPublishValidator.validateAggregateFormulas()` 调用 `AggregateValidation.validateFormula()` |
| **R2** | relation 缺失时静默返回空集合 | `DbAggregateDataProvider` 抛出 `AggregateDataException`，`AggregateEngine` 捕获转 `AggregateResult.error` |
| **R3** | targetField 缺失时静默返回 0 | `AggregateFunctionExecutor` 检测 `nonNullCount==0` 时抛出 `AggregateExecutionException` |
| B1 | FormulaConfig.condition 类型不匹配 | 新增 `ConditionConfig` 领域模型 `{ expression, trueValue, falseValue }` |
| B2 | CONDITIONAL 执行返回 boolean | `FormulaExecutionEngine.executeConditional()` 实现三元分支逻辑 |
| B3 | preview() 不支持 CONDITIONAL | `FormulaController.buildFormulaConfig()` 解析 `ConditionPreview` DTO |

---

## 15. 已知限制

| 限制 | 说明 | 影响 | 建议 |
|------|------|------|------|
| trueValue/falseValue 类型 | `ConditionConfig.trueValue/falseValue` 为 `Object` 类型 | JSON 反序列化后数字可能为 `Integer` 而非 `Long` | V1.1 增加类型推断 |
| Dependency API | `/formula/dependency` 未传递 condition 给 DTO | 依赖分析接口不完整展示 CONDITIONAL | V1.1 扩展 `FormulaDependencyRequest` |
| 跨对象公式 | V1 仅支持同对象字段间的公式 | 无法引用其他对象的字段 | V1.1 LOOKUP 公式 |
| 公式嵌套深度 | 硬限制 maxDepth=3 | 复杂计算场景可能不够 | V1.1 可配置化 |
| CONDITIONAL 校验 | 未专门校验 trueValue/falseValue 类型 | 运行时可能类型不匹配 | V1.1 增强校验 |

---

## 16. 后续规划 (V1.1)

| 功能 | 优先级 | 说明 |
|------|--------|------|
| LOOKUP 公式 | 高 | 跨对象字段引用，如 `LOOKUP(order.customer.name)` |
| 公式调试器 | 高 | 执行日志 + 中间值展示 + 断点调试 |
| 自定义函数 | 中 | 业务方注册自定义 Aviator 函数 |
| 执行日志 | 中 | 公式执行时间线 + 错误追踪 |
| CONDITIONAL 类型增强 | 中 | trueValue/falseValue 自动类型推断 |
| 可视化 DAG | 低 | 前端依赖关系图展示 |
| 公式模板库 | 低 | 常用公式模板 (税额/折扣/汇率等) |

---

## 17. 发布说明

### 版本信息

| 属性 | 值 |
|------|-----|
| 版本号 | **v1.0.0** |
| 发布日期 | **2026-06-12** |
| 分支 | `feature/formula-capability-extension` |
| 提交 | `e8883cd7` |
| 状态 | **READY FOR RELEASE** |

### 测试结果

| 指标 | 值 |
|------|-----|
| 测试总数 | 186 |
| 失败 | 0 |
| Blocking Issues | 0 |
| Major Issues | 0 |

### DB Migration

需执行: `forge-server/db/migration/V1.0.67__add_formula_config_to_gen_table_column.sql`

```sql
ALTER TABLE gen_table_column ADD COLUMN formula_config TEXT COMMENT '公式配置 JSON';
```

### 依赖变更

新增 `aviator` 表达式引擎依赖 (forge-dependencies/pom.xml)。

### 安装说明

1. 拉取分支 `feature/formula-capability-extension`
2. 执行 DB Migration `V1.0.67`
3. 编译后端: `mvn clean package -pl forge-server`
4. 编译前端: `cd forge-admin-ui && pnpm build`
5. 部署并验证 `/api/ai/business/formula/functions` 返回 200

### 结论

**建议合并主分支并发布。**