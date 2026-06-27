---
title: 公式能力二期扩展
version: 1.0
date_created: 2026-06-13
last_updated: 2026-06-14
tags: ['formula', 'lookup', 'cross-object', 'debugger', 'function-market', 'condition-rule']
status: done
complexity: high
base_change: code-copilot/changes/formula-capability-extension/spec.md
---

# 背景

公式能力 V1 已建立字段级公式体系，覆盖 CALC、AGGREGATE、CONDITIONAL、STORED/VIRTUAL、公式预览、语法校验、DAG 依赖分析、循环依赖检测和基础函数列表。下一阶段需要从“单对象字段计算”升级到“对象图公式 + 可观测性 + 可配置函数生态”，支撑更复杂的低代码业务规则。

本次变更规划以下能力：

- LOOKUP 公式
- 跨对象公式
- 自定义函数
- 公式调试器
- 执行日志
- Dependency 可视化
- 函数市场
- 条件规则设计器

## 1. 目标与成功标准

### 用户价值

- 业务人员可以通过 LOOKUP / 跨对象公式读取关联对象字段，减少冗余字段和手工同步。
- 设计人员可以通过调试器、执行日志和依赖图快速定位公式错误。
- 平台管理员可以治理函数能力，按需启用内置函数、自定义函数和市场函数。
- 普通配置人员可以通过条件规则设计器生成条件公式，减少手写表达式门槛。

### 业务目标

- 将公式能力从“字段计算”扩展为“业务对象关系图计算”。
- 为复杂公式上线前后提供可解释、可追踪、可审计的运维闭环。
- 为后续 AI 公式生成、规则推荐、函数模板市场打基础。

### 成功判定

- 公式执行失败时，可以在执行日志中看到输入、输出、耗时、错误原因和 traceId。
- 公式调试器可以展示样例数据、依赖字段值、执行顺序和每一步结果。
- 依赖图可以展示对象内和跨对象依赖，并高亮循环路径。
- 字段可以配置 LOOKUP 公式读取一跳关联对象字段。
- 跨对象公式支持受控的一跳路径表达式，并在发布时进行对象图依赖校验。
- 函数列表不再硬编码，函数市场可查询、安装、启用、禁用函数。
- 条件规则设计器可以生成 Aviator 表达式，并与公式配置双向同步。

## 2. 代码现状

### 2.1 已有能力

| 能力 | 现状 |
|------|------|
| 公式领域模型 | `FormulaConfig`, `AggregateConfig`, `ConditionConfig`, `FormulaType`, `FormulaMode` |
| 表达式执行 | `ExpressionExecutor`, `FormulaExecutionEngine` |
| 聚合公式 | `AggregateEngine`, `DbAggregateDataProvider` |
| 依赖分析 | `FormulaDependencyAnalyzer`, `DependencyAnalysisResult` |
| 校验服务 | `FormulaValidationService`, `FormulaPublishValidator` |
| REST API | `/api/ai/business/formula/validate`, `/preview`, `/dependency`, `/functions` |
| 前端配置 | `BusinessFieldPropertyPanel.vue` 内嵌公式配置、校验和预览 |
| 前端 API | `forge-admin-ui/src/api/formula.js` |

### 2.2 当前限制

- `/formula/functions` 返回静态内置函数列表，无法治理函数状态、版本和市场来源。
- `/formula/dependency` 返回字段依赖信息，但没有图形化节点/边结构，也不覆盖跨对象依赖。
- `ExecutionResult` 只返回当前执行结果，不持久化执行日志，不支持运行后审计。
- 公式预览只能做整体结果预览，不能展示每个依赖字段的执行过程。
- V1 明确不支持跨对象公式引用。
- 条件公式仍以表达式输入为主，普通用户配置门槛较高。

## 3. 分阶段范围

### Phase 1: 可观测性与调试底座

优先级：P0

包含：

- 执行日志
- 公式调试器
- Dependency 可视化

目标：

- 先让公式“可解释、可追踪、可排错”，再扩展 LOOKUP 和跨对象引用。
- 所有后续公式能力必须接入同一套日志、调试和依赖图结构。

### Phase 2: LOOKUP 与跨对象公式

优先级：P1

包含：

- LOOKUP 公式
- 一跳跨对象公式
- 跨对象依赖校验
- 跨对象公式日志与调试接入

目标：

- 在对象关系配置明确的前提下读取关联对象字段。
- 发布时发现跨对象循环依赖和非法路径。
- 先支持一跳关系，暂不开放任意深度路径。

### Phase 3: 条件规则设计器

优先级：P2

包含：

- 条件规则 JSON AST
- 表达式生成器
- 条件公式 UI 改造
- 调试器展示条件节点命中情况

目标：

- 普通配置人员不手写表达式即可配置条件公式。
- 保留表达式模式，支持高级用户继续手写。

### Phase 4: 自定义函数与函数市场

优先级：P3

包含：

- 函数注册中心
- 自定义函数元数据
- 函数市场
- 函数安装、启用、禁用、版本管理

目标：

- 将公式函数从静态列表升级为可治理、可扩展、可审计的函数生态。
- 第一版只支持 Java Bean 注册和受控配置，不开放任意脚本执行。

## 4. 非目标

- 不在本阶段开放任意 SQL 函数。
- 不允许用户函数访问文件、网络、系统命令或环境变量。
- 不支持无限层级跨对象路径；首期只支持一跳，后续再评估多跳。
- 不改变 V1 公式配置兼容性；已有 CALC / AGGREGATE / CONDITIONAL 配置必须继续可用。
- 不替换 Aviator 表达式引擎。
- 不把公式引擎改造成通用工作流引擎。
- 不在本阶段实现 AI 自动生成公式。

## 5. Requirements, Constraints & Guidelines

### 5.1 执行日志

- **REQ-LOG-001**: 每次公式运行必须可选记录执行日志，包含 objectCode、recordId、fieldCode、formulaType、mode、expression、inputSnapshot、outputValue、success、errorMessage、elapsedMs、traceId。
- **REQ-LOG-002**: 公式异常必须记录 ERROR 级日志和业务执行日志，不能静默吞掉。
- **REQ-LOG-003**: 日志记录必须支持按 objectCode、recordId、fieldCode、success、traceId、时间范围分页查询。
- **REQ-LOG-004**: 日志中涉及手机号、身份证、银行卡、Token、AK/SK 等敏感值必须脱敏。
- **REQ-LOG-005**: 日志开关必须可配置，默认记录失败日志和调试日志，不强制记录所有成功日志。

### 5.2 公式调试器

- **REQ-DBG-001**: 调试器支持输入 sampleValues 并运行单个字段公式或当前对象全部公式。
- **REQ-DBG-002**: 调试结果必须返回 executionPlan、steps、contextBefore、contextAfter、result、errors。
- **REQ-DBG-003**: 每一步必须显示字段编码、公式类型、表达式、输入变量、输出值、耗时和错误。
- **REQ-DBG-004**: CONDITIONAL 公式调试时必须展示条件表达式结果和 true/false 分支选择。
- **REQ-DBG-005**: LOOKUP / 跨对象公式调试时必须展示关联对象、关联字段、查询条件和返回字段。

### 5.3 Dependency 可视化

- **REQ-DAG-001**: 依赖分析接口必须返回图结构 nodes / edges。
- **REQ-DAG-002**: 节点类型至少包含 FIELD、FORMULA、OBJECT、FUNCTION、RELATION。
- **REQ-DAG-003**: 边类型至少包含 DEPENDS_ON、LOOKUP、CROSS_OBJECT、AGGREGATE、FUNCTION_CALL。
- **REQ-DAG-004**: 检测到循环依赖时必须返回 cyclePath，并在前端高亮。
- **REQ-DAG-005**: 图结构必须兼容对象内依赖和跨对象依赖。

### 5.4 LOOKUP 公式

- **REQ-LOOKUP-001**: `FormulaType` 新增 LOOKUP。
- **REQ-LOOKUP-002**: LOOKUP 公式必须基于已配置的业务对象关系或显式 lookup 配置，禁止自由拼接表名。
- **REQ-LOOKUP-003**: LOOKUP 支持根据当前对象字段值查询目标对象字段值。
- **REQ-LOOKUP-004**: LOOKUP 首期支持一跳关系，不支持任意多跳。
- **REQ-LOOKUP-005**: LOOKUP 查询必须遵守租户隔离和数据权限规则。
- **REQ-LOOKUP-006**: LOOKUP 失败时按公式错误处理策略降级，并写入执行日志。

### 5.5 跨对象公式

- **REQ-XOBJ-001**: 跨对象公式支持一跳路径表达式，例如 `customer.level`、`owner.realName`。
- **REQ-XOBJ-002**: 跨对象路径必须能解析到已发布对象关系，不能引用不存在的对象、关系或字段。
- **REQ-XOBJ-003**: 发布时必须进行对象图 DAG 校验，检测跨对象循环依赖。
- **REQ-XOBJ-004**: STORED 跨对象公式必须定义重算策略：同步重算、异步队列或手动重算，首期默认异步队列。
- **REQ-XOBJ-005**: 查询时 VIRTUAL 跨对象公式必须避免 N+1 查询，支持批量预取。

### 5.6 自定义函数

- **REQ-FN-001**: 函数必须有唯一 functionCode、displayName、category、description、argumentSchema、returnType、example、status。
- **REQ-FN-002**: 函数实现首期支持 Java Bean 注册，不开放任意脚本。
- **REQ-FN-003**: 函数执行必须有超时限制和异常隔离。
- **REQ-FN-004**: 函数参数必须按 argumentSchema 校验。
- **REQ-FN-005**: 函数调用必须接入公式校验、预览、调试器和执行日志。

### 5.7 函数市场

- **REQ-MKT-001**: 函数市场支持分页、搜索、分类筛选、查看详情。
- **REQ-MKT-002**: 函数支持安装、启用、禁用和版本回滚。
- **REQ-MKT-003**: 市场函数必须标记来源：BUILTIN、SYSTEM、TENANT、MARKET。
- **REQ-MKT-004**: 未启用函数不能被新公式引用；已保存旧公式引用禁用函数时，发布必须给出阻断错误。
- **REQ-MKT-005**: 函数市场操作必须记录操作日志。

### 5.8 条件规则设计器

- **REQ-RULE-001**: 条件规则设计器输出 JSON AST，并同步生成 Aviator 表达式。
- **REQ-RULE-002**: 支持 AND / OR 分组、字段选择、操作符选择和值输入。
- **REQ-RULE-003**: 支持操作符 EQ、NE、GT、GTE、LT、LTE、IN、NOT_IN、CONTAINS、STARTS_WITH、ENDS_WITH、IS_NULL、NOT_NULL。
- **REQ-RULE-004**: 条件公式支持在表达式模式和规则设计模式之间切换。
- **REQ-RULE-005**: 调试器必须展示每个规则节点是否命中。

## 6. 数据模型与配置协议

### 6.1 FormulaConfig 扩展

兼容 V1 配置，新增 lookup、crossObject、rule、functionRefs 元数据。

```json
{
  "type": "LOOKUP",
  "mode": "VIRTUAL",
  "expression": "",
  "dependsOn": ["ownerUserId"],
  "lookup": {
    "relationCode": "customer_owner",
    "targetObjectCode": "sys_user",
    "sourceField": "ownerUserId",
    "targetField": "id",
    "returnField": "realName",
    "notFoundValue": null
  },
  "crossObject": null,
  "rule": null,
  "functionRefs": []
}
```

### 6.2 LOOKUP 配置

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| relationCode | String | 是 | 对象关系编码 |
| targetObjectCode | String | 是 | 目标对象编码 |
| sourceField | String | 是 | 当前对象关联字段 |
| targetField | String | 是 | 目标对象匹配字段 |
| returnField | String | 是 | 返回字段 |
| notFoundValue | Object | 否 | 未命中时返回值 |

### 6.3 跨对象路径配置

```json
{
  "path": "customer.level",
  "relationCode": "order_customer",
  "targetObjectCode": "crm_customer",
  "returnField": "level",
  "recomputeMode": "ASYNC"
}
```

### 6.4 条件规则 AST

```json
{
  "operator": "AND",
  "children": [
    { "field": "amount", "op": "GT", "value": 1000 },
    { "field": "status", "op": "EQ", "value": "ACTIVE" }
  ]
}
```

生成表达式：

```text
amount > 1000 && status == 'ACTIVE'
```

### 6.5 执行日志表

新增 Flyway 脚本，建议表名：`ai_formula_execution_log`。

核心字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| tenant_id | bigint | 租户 |
| trace_id | varchar(64) | 本次公式执行链路 ID |
| object_code | varchar(64) | 对象编码 |
| record_id | varchar(64) | 记录 ID |
| field_code | varchar(64) | 公式字段编码 |
| formula_type | varchar(32) | 公式类型 |
| formula_mode | varchar(32) | 计算模式 |
| expression | text | 表达式或配置摘要 |
| input_snapshot | json | 输入快照，脱敏后存储 |
| output_value | text | 输出值摘要 |
| success | tinyint | 是否成功 |
| error_message | text | 错误信息 |
| elapsed_ms | bigint | 耗时 |
| create_by | bigint | 创建人 |
| create_time | datetime | 创建时间 |
| create_dept | bigint | 创建部门 |
| update_by | bigint | 更新人 |
| update_time | datetime | 更新时间 |

### 6.6 函数注册表

建议新增：

- `ai_formula_function`
- `ai_formula_function_version`
- `ai_formula_function_install`

函数注册核心字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| function_code | varchar(64) | 函数编码 |
| display_name | varchar(128) | 展示名称 |
| category | varchar(64) | 分类 |
| source_type | varchar(32) | BUILTIN / SYSTEM / TENANT / MARKET |
| argument_schema | json | 参数 schema |
| return_type | varchar(32) | 返回类型 |
| example | text | 示例 |
| status | varchar(32) | ENABLED / DISABLED |

## 7. API 设计

### 7.1 公式调试

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/business/formula/debug` | 调试单公式或当前对象全部公式 |

请求示例：

```json
{
  "objectCode": "crm_order",
  "fieldCode": "totalAmount",
  "recordId": "1001",
  "sampleValues": {
    "price": 100,
    "quantity": 3
  },
  "includeDependencyGraph": true
}
```

响应核心：

```json
{
  "success": true,
  "traceId": "FML-20260613-0001",
  "executionPlan": ["amount", "discount", "totalAmount"],
  "steps": [
    {
      "fieldCode": "totalAmount",
      "formulaType": "CALC",
      "expression": "price * quantity",
      "input": { "price": 100, "quantity": 3 },
      "output": 300,
      "elapsedMs": 4,
      "success": true
    }
  ],
  "errors": []
}
```

### 7.2 执行日志

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai/business/formula/log/page` | 分页查询执行日志 |
| GET | `/api/ai/business/formula/log/{id}` | 查询日志详情 |

查询参数：

- `pageNum`
- `pageSize`
- `objectCode`
- `recordId`
- `fieldCode`
- `success`
- `traceId`
- `beginTime`
- `endTime`

### 7.3 Dependency 图

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/business/formula/dependency/graph` | 返回依赖图 nodes / edges |

响应核心：

```json
{
  "valid": true,
  "hasCycle": false,
  "nodes": [
    { "id": "field:amount", "type": "FIELD", "label": "金额" },
    { "id": "formula:totalAmount", "type": "FORMULA", "label": "总金额" }
  ],
  "edges": [
    { "source": "field:amount", "target": "formula:totalAmount", "type": "DEPENDS_ON" }
  ],
  "cyclePath": []
}
```

### 7.4 函数市场

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai/business/formula/function-market/page` | 函数市场分页 |
| GET | `/api/ai/business/formula/function-market/{functionCode}` | 函数详情 |
| POST | `/api/ai/business/formula/function-market/{functionCode}/install` | 安装函数 |
| PUT | `/api/ai/business/formula/functions/{functionCode}/enable` | 启用函数 |
| PUT | `/api/ai/business/formula/functions/{functionCode}/disable` | 禁用函数 |

### 7.5 条件规则

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/business/formula/rule/compile` | 规则 JSON 编译为表达式 |
| POST | `/api/ai/business/formula/rule/validate` | 校验规则 JSON 和生成表达式 |

## 8. 前端设计

### 8.1 字段属性面板

修改 `BusinessFieldPropertyPanel.vue`：

- 公式类型新增 LOOKUP。
- 公式配置区拆分为组件：
  - `FormulaConfigPanel.vue`
  - `FormulaExpressionEditor.vue`
  - `FormulaLookupPanel.vue`
  - `FormulaCrossObjectPanel.vue`
  - `FormulaConditionRuleDesigner.vue`
  - `FormulaDebuggerPanel.vue`
  - `FormulaDependencyGraph.vue`
  - `FormulaExecutionLogDrawer.vue`

### 8.2 对象设计器入口

在对象设计器中增加公式工具入口：

- 公式调试
- 依赖图
- 执行日志
- 函数市场

这些入口应服务于当前对象上下文，默认带入 objectCode。

### 8.3 交互要求

- 调试器和执行日志不放在字段属性表单内挤占编辑区域，建议使用内嵌面板或弹窗。
- Dependency 图支持缩放、拖拽、节点筛选、循环路径高亮。
- 函数市场以工具型管理界面呈现，避免营销式卡片堆叠。
- 条件规则设计器必须支持键盘和鼠标高频编辑。

## 9. 后端设计

### 9.1 新增领域服务

| 服务 | 责任 |
|------|------|
| `FormulaExecutionLogService` | 执行日志落库、查询、脱敏 |
| `FormulaDebugService` | 构建调试上下文、逐步执行、返回 trace |
| `FormulaDependencyGraphService` | 将依赖分析结果转换为 nodes / edges |
| `FormulaLookupResolver` | 解析 LOOKUP 配置并读取关联对象字段 |
| `FormulaCrossObjectResolver` | 解析一跳跨对象路径和预取数据 |
| `FormulaObjectDependencyAnalyzer` | 发布期将跨对象公式转换为对象图依赖并检测循环 |
| `CrossObjectRecomputeTaskService` | 生成 STORED 跨对象公式待重算任务和幂等键 |
| `FormulaFunctionRegistry` | 注册、查询、启停函数 |
| `FormulaFunctionMarketService` | 市场函数安装和版本管理 |
| `ConditionRuleCompiler` | 规则 JSON 转 Aviator 表达式 |

### 9.2 运行时接入

- `FormulaExecutionEngine` 增加 step trace 输出能力。
- `ExpressionExecutor` 增加函数注册表集成。
- `StoredFormulaRuntime` 和 `VirtualFormulaRuntime` 接入日志服务。
- `FormulaPublishValidator` 增加 LOOKUP、跨对象路径、函数状态、条件规则校验。
- `DbAggregateDataProvider` 的批量查询经验复用到 LOOKUP / 跨对象 VIRTUAL 预取。

### 9.3 数据权限

- LOOKUP / 跨对象查询必须走对象关系元数据和动态 CRUD 查询能力。
- 禁止公式配置直接提交表名、SQL 片段或 Mapper 方法名。
- VIRTUAL 跨对象公式批量预取仍必须保留 tenant_id 和数据权限边界。

## 10. 安全与合规

- 公式日志必须脱敏输入快照和输出值。
- 自定义函数不得访问网络、文件系统、环境变量、系统命令。
- 函数执行必须设置超时时间和最大调用深度。
- 函数市场安装、启用、禁用必须记录操作日志。
- 跨对象公式不得绕过对象关系和权限模型读取数据。
- 调试器只允许有对象设计权限的用户访问。

## 11. 风险

| 风险 | 说明 | 缓解 |
|------|------|------|
| 跨对象 N+1 查询 | 列表查询时每行触发 LOOKUP | VIRTUAL 模式批量预取，限制一跳 |
| 跨对象循环依赖 | A 对象依赖 B，B 又依赖 A | 发布时对象图 DAG 校验 |
| 日志过大 | 成功公式大量执行导致日志膨胀 | 默认仅记录失败和调试日志，成功日志可配置采样 |
| 敏感数据泄露 | inputSnapshot 可能含敏感字段 | 字段敏感标记 + 统一脱敏器 |
| 自定义函数安全 | 函数可能执行危险逻辑 | 首期仅 Java Bean 注册，禁脚本 |
| 函数禁用兼容 | 旧公式引用被禁用函数 | 发布阻断，运行态给出明确错误 |
| 条件规则与表达式不一致 | UI JSON 和表达式双写漂移 | 保存时以 rule JSON 编译表达式，保留 expression 快照 |

## 12. 测试与验证策略

### 12.1 后端单元测试

- `FormulaExecutionLogServiceTest`
- `FormulaDebugServiceTest`
- `FormulaDependencyGraphServiceTest`
- `FormulaLookupResolverTest`
- `FormulaCrossObjectResolverTest`
- `FormulaObjectDependencyAnalyzerTest`
- `FormulaFunctionRegistryTest`
- `ConditionRuleCompilerTest`

### 12.2 后端集成测试

- LOOKUP 公式读取一跳关联对象字段。
- VIRTUAL 跨对象公式列表查询批量预取。
- STORED 跨对象公式依赖对象变更后进入重算队列。
- 自定义函数启用后可调用，禁用后发布阻断。
- 执行失败写入日志并返回 traceId。

### 12.3 前端验证

- 字段属性面板可配置 LOOKUP。
- 调试器展示 executionPlan 和 steps。
- 依赖图展示节点和边，循环路径高亮。
- 函数市场查询、安装、启用、禁用流程可用。
- 条件规则设计器可生成表达式并预览结果。

### 12.4 构建验证

- 后端：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test`
- 前端：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui build`
- 数据库：Flyway 脚本在空库和已有库均可重复校验。

## 13. 验收标准

- **AC-001**: Given 公式执行失败, When 查询执行日志, Then 能看到 objectCode、recordId、fieldCode、expression、inputSnapshot、errorMessage、traceId。
- **AC-002**: Given 字段 A 依赖字段 B 和 C, When 打开依赖图, Then 图中展示 A、B、C 节点和 DEPENDS_ON 边。
- **AC-003**: Given 字段配置 LOOKUP 读取客户负责人姓名, When 当前记录 ownerUserId 有值, Then 公式返回对应用户姓名。
- **AC-004**: Given 跨对象公式引用不存在的关系路径, When 发布对象, Then 发布失败并提示非法路径。
- **AC-005**: Given A 对象公式依赖 B，B 对象公式依赖 A, When 发布, Then 对象图循环依赖校验阻断发布。
- **AC-006**: Given 管理员禁用函数 `tax.amount`, When 公式引用该函数并发布, Then 发布失败并提示函数未启用。
- **AC-007**: Given 条件规则为 amount > 1000 AND status = ACTIVE, When 保存条件公式, Then 系统生成表达式 `amount > 1000 && status == 'ACTIVE'` 并可预览。
- **AC-008**: Given 调试 LOOKUP 公式, When 调试执行, Then step 中展示 relationCode、targetObjectCode、sourceField、targetField 和 returnField。
- **AC-009**: Given 输入快照包含敏感字段, When 查看执行日志, Then 敏感值已脱敏。
- **AC-010**: Given V1 旧公式配置, When 打开字段设计器并发布对象, Then 旧配置继续可用，无需迁移。

## 14. 推荐实施顺序

1. Phase 1: 可观测性与调试底座
2. Phase 2A: LOOKUP 公式
3. Phase 2B: 一跳跨对象公式
4. Phase 3: 条件规则设计器
5. Phase 4A: 函数注册中心
6. Phase 4B: 函数市场

## 15. 待澄清项

- LOOKUP 首期是否只允许基于 `AiBusinessObjectRelation`，还是允许显式 `sourceField -> targetField` 配置。
- STORED 跨对象公式首期是否必须自动重算，还是允许先提供“待重算队列 + 手动重算”。
- 函数市场是否只做本地内置市场，还是需要远程市场源。
- 执行日志保留周期和清理策略。
- 条件规则设计器是否需要复用低代码触发器的现有条件构建器。

## 归档记录（HARD-GATE）
- **状态**：done
- **归档时间**：2026-06-27
- **归档人**：yaomd（批量归档）
- **归档路径**：code-copilot/changes/archive/2026-06-27-formula-next-capabilities/
- **判定依据**：任务清单全部完成，execution-log 验证通过（编译/构建/lint 闭环）。
