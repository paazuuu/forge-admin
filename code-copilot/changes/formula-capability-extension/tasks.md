# 公式能力扩展 — 开发任务

> created: 2026-06-12 | complexity: 🜶 复杂 | status: apply (Phase 1 ✅, Phase 2-5 ⏳)

---

## Phase 1: 领域模型 + 依赖分析器（～6h）

> ⚠️ 本阶段不引入 Aviator、不修改 CRUD Hook、不修改 PublishService、不做前端、不暴露 API

### T1.1 创建 FormulaType 枚举
- **路径**: orge-plugin-generator/.../domain/formula/FormulaType.java
- **值**: CALC (计算公式), AGGREGATE (聚合公式), CONDITIONAL (条件公式)
- **验证**: 编译通过
- **预计**: 0.25h

### T1.2 创建 FormulaMode 枚举
- **路径**: orge-plugin-generator/.../domain/formula/FormulaMode.java
- **值**: VIRTUAL (读取时实时计算), STORED (保存时计算并持久化)
- **验证**: 编译通过
- **预计**: 0.25h

### T1.3 创建 AggregateFunction 枚举
- **路径**: orge-plugin-generator/.../domain/formula/AggregateFunction.java
- **值**: SUM, COUNT, AVG, MAX, MIN
- **验证**: 编译通过
- **预计**: 0.25h

### T1.4 创建 FormulaConfig 领域模型
- **路径**: orge-plugin-generator/.../domain/formula/FormulaConfig.java
- **设计**: 纯领域模型（非 DTO），不含持久化注解
- **字段**:
  - 	ype: FormulaType
  - mode: FormulaMode  
  - expression: String (Aviator 表达式，Phase 2 才执行)
  - dependsOn: List\<String\> (依赖字段名列表)
  - ggregate: AggregateConfig (聚合配置，type=AGGREGATE 时必填)
  - condition: String (条件表达式，type=CONDITIONAL 时必填)
- **方法**: isVirtual(), isStored(), isAggregate(), isCalc(), isConditional()
- **验证**: 编译通过，自描述性 API 正确
- **预计**: 1h

### T1.5 创建 AggregateConfig 领域模型
- **路径**: orge-plugin-generator/.../domain/formula/AggregateConfig.java
- **字段**:
  - unction: AggregateFunction
  - elationCode: String (对象关系编码，定位从表)
  - 	argetField: String (从表中需要聚合的字段)
  - ilter: String (可选，Aviator 过滤条件，Phase 2 才执行)
- **方法**: getFunctionName(), hasFilter()
- **验证**: 编译通过
- **预计**: 0.5h

### T1.6 实现 FormulaDependencyAnalyzer
- **路径**: orge-plugin-generator/.../domain/formula/FormulaDependencyAnalyzer.java
- **输入**: List<FormulaConfig> (一个对象的所有公式字段)
- **功能**:
  - nalyzeDag() → 构建依赖 DAG (Map<String, Set<String>>)
  - detectCycle() → 拓扑排序检测循环依赖
  - computeDepth() → 计算每个公式的嵌套深度
  - alidateDepth(int maxDepth=3) → 校验最大嵌套深度
  - getTopologicalOrder() → 返回拓扑排序后的计算公式执行顺序
- **错误信息**: 检测到循环时返回涉及的字段名列表
- **验证**: 编译通过
- **预计**: 3h

### T1.7 编写 CycleDetect 单元测试
- **路径**: orge-plugin-generator/.../domain/formula/FormulaDependencyAnalyzerTest.java
- **覆盖**:
  - 无环 DAG → 通过，返回正确拓扑序
  - 简单环 A→B→A → 检测失败
  - 间接环 A→B→C→A → 检测失败
  - 自引用 A→A → 检测失败
  - 嵌套深度=3 → 通过
  - 嵌套深度=4 → 拦截
  - 空公式列表 → 通过
  - 单公式无依赖 → 通过
  - 聚合公式依赖关系正确解析
- **覆盖率**: 90%+
- **预计**: 2h

---

## Phase 2: 表达式引擎集成（～6h）

### T2.1 引入 Aviator 依赖
- **范围**: orge-dependencies/pom.xml
- **预计**: 0.5h

### T2.2 实现 FormulaEngine
- **预计**: 4h

### T2.3 实现公式验证服务
- **预计**: 1.5h

---

## Phase 3: 业务集成（～11h）

### T3.1 扩展 GenTableColumn + DB Migration
- **预计**: 1h

### T3.2 发布服务集成公式校验
- **预计**: 2h

### T3.3 动态CRUD 截入公式计算
- **预计**: 3h

### T3.4 实现聚合公式引擎
- **预计**: 3h

### T3.5 从表联动聚合重算
- **预计**: 2h

---

## Phase 4: 接口与前端（～8.5h）

### T4.1 公式配置 REST API
- **预计**: 2h

### T4.2 前端公式配置界面
- **预计**: 5h

### T4.3 前端公式字段只读展示
- **预计**: 1.5h

---

## Phase 5: 测试（～6h）

### T5.1 FormulaEngine 单元测试
- **覆盖率**: 85%+
- **预计**: 2h

### T5.2 聚合公式测试
- **预计**: 1.5h

### T5.3 集成测试
- **预计**: 2.5h

---

## 工时估算

| Phase | 内容 | 预计 |
|-------|------|------|
| Phase 1 | 领域模型 + 依赖分析器 | ~8h |
| Phase 2 | 表达式引擎集成 | ~6h |
| Phase 3 | 业务集成 | ~11h |
| Phase 4 | 接口与前端 | ~8.5h |
| Phase 5 | 测试 | ~6h |
| **合计** | | **~39.5h** |

## Phase 1 明确禁止项

> 第一阶段仅创建领域模型、枚举和分析器，明确禁止以下操作：

| 禁止项 | 原因 |
|--------|------|
| ❌ Aviator 依赖 | Phase 2 引入 |
| ❌ CRUD Hook | Phase 3 实现 |
| ❌ PublishService 修改 | Phase 3 实现 |
| ❌ 前端界面 | Phase 4 实现 |
| ❌ REST API | Phase 4 实现 |
| ❌ DB Migration | Phase 3 实现 |
| ❌ GenTableColumn 修改 | Phase 3 实现 |

