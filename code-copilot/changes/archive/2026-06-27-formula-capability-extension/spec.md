---
title: 公式能力扩展
version: 1.0
date_created: 2026-06-12
last_updated: 2026-06-12
tags: ['formula', 'business-object', 'computation', 'core']
status: done
complexity: 🜶 复杂
---

# 背景

ForgeAdmin 已具备 BusinessObject、LowcodeModel、Runtime、动态CRUD、对象关系能力，但缺少统一公式体系，无法支持金额自动计算、主从表汇总、条件表达式计算、自动字段赋值等场景。本次变更建立字段级公式能力，使业务对象的字段可通过公式自动计算值，减少手动赋值和数据不一致风险。

## 1. 目标与成功标准

- **用户价值**: 业务对象字段值可通过配置公式自动计算，无需手写代码
- **业务目标**: 消除金额计算、主从表汇总的手工维护，降低数据不一致风险
- **成功判定**:
  - 字段设计器中可配置公式（类型、表达式、计算模式）
  - 配置公式后保存/查询时字段值自动计算
  - 从表变更时主表聚合字段自动更新
  - 发布时循环依赖和嵌套深度校验通过

## 2. 代码现状

### 2.1 关键入口

| 文件 | 说明 |
|------|------|
| orge-plugin-generator/.../domain/entity/AiBusinessObject.java | 业务对象实体，含 objectType、options(JSON) |
| orge-plugin-generator/.../domain/entity/AiBusinessObjectRelation.java | 对象关系，支持 REFERENCE/DETAIL/CHILD_LIST |
| orge-plugin-generator/.../domain/entity/AiLowcodeModel.java | 数据模型，modelSchema 存储字段定义 JSON |
| orge-plugin-generator/.../domain/entity/GenTableColumn.java | 字段定义实体，含 javaField、columnType、validateRule |
| orge-plugin-generator/.../service/businessapp/BusinessObjectPublishService.java | 对象发布服务 |
| orge-plugin-generator/.../service/businessapp/BusinessObjectCreateService.java | 业务对象创建服务 |

### 2.2 已知限制

- GenTableColumn 无公式配置字段，AiLowcodeModel.modelSchema 中无公式定义
- 运行时动态CRUD 无公式计算截入点
- 金额计算、主从表汇总全靠业务代码手写

## 3. 变更范围

### 会修改

- orge-plugin-generator: 字段定义实体、发布服务、DTO/VO 扩展
- orge-plugin-generator: 新增公式服务、控制器、Aviator 表达式引擎集成
- orge-app-server / orge-admin-server: 动态CRUD 入口增加公式计算截入
- orge-admin-ui: 字段设计器中新增公式配置界面
- orge-dependencies: 新增 Aviator 表达式引擎依赖

### 不会修改

- 条件引擎、审批表达式、流程表达式
- AI公式生成
- forge-flow 流程引擎
- forge-report-ui、forge-h5-ui
- 跨对象公式引用（V1 不支持）

## 4. Requirements, Constraints & Guidelines

### 功能需求

- **REQ-001 (计算公式)**: 字段值由同对象其他字段表达式计算得出，如 	otal = unit_price * quantity
- **REQ-002 (聚合公式)**: 主表字段值由从表记录聚合计算，如 	otal_amount = SUM(detail.amount)
- **REQ-003 (条件公式)**: 字段值根据条件表达式动态赋值，如 discount = IF(amount > 1000, 0.1, 0)
- **REQ-004 (VIRTUAL 模式)**: 读取时动态计算，不存储到数据库
- **REQ-005 (STORED 模式)**: 保存时计算并持久化，读取时返回存储值
- **REQ-006 (公式配置)**: 字段定义中新增 ormula_config JSON，支持公式类型、表达式、计算模式、依赖字段
- **REQ-007 (循环依赖检测)**: 发布时使用 DAG 拓扑排序检测循环引用
- **REQ-008 (公式错误处理)**: 计算失败时记录 ERROR 日志并降级处理
- **REQ-009 (公式嵌套)**: V1 支持最大嵌套深度 3 层，发布时 DAG 分析 + 循环检测

### 约束条件

- **CON-001**: 公式字段不支持用户直接编辑，值由系统计算
- **CON-002**: 金额类公式计算结果使用 long 类型（单位：分）
- **CON-003**: STORED 公式在 Insert/Update 时触发重算
- **CON-004**: VIRTUAL 公式在查询记录时实时计算，不存储
- **CON-005**: 聚合公式依赖于对象关系 (DETAIL/CHILD_LIST)，无关系时报错
- **CON-006**: 聚合公式采用自动重算模式，不采用手动或定时触发
- **CON-007**: STORED 聚合公式采用自动维护模式，禁止脏数据
- **CON-008**: 公式嵌套最大深度 3 层
- **CON-009**: V1 不支持跨对象公式

### 编写约定

- **GUD-001**: 表达式引擎使用 Aviator，支持算术、逻辑、三元、内置函数
- **GUD-002**: 公式配置 JSON 格式统一：{ type, expression, mode, dependsOn, maxDepth }
- **GUD-003**: 公式服务注入点采用统一拦截器模式，不侵入具体 CRUD 业务代码

### 风险

- **RISK-001 (金额一致性)**: STORED 聚合公式必须与从表同步更新，避免主从表金额不一致
- **RISK-002 (循环依赖)**: 公式间循环引用必须在发布时拦截
- **RISK-003 (性能)**: 聚合公式 N+1 查询需优化；从表批量操作时主表重算频率需控制
- **RISK-004 (并发)**: STORED 公式并发写入时可能导致主表值过时
- **RISK-005 (兼容性)**: 已有字段增加公式后历史数据需重算迁移
- **RISK-006 (回滚)**: 公式配置回滚后已存储计算值需明确清理策略

## 5. Interfaces & Data Contracts

### 数据变更

| 表 | 字段 | 说明 |
|---|---|---|
| gen_table_column | ormula_config TEXT | 新增公式配置 JSON |
| i_lowcode_model | modelSchema JSON | 字段 schema 中新增 formula 节点 |

### API 变更

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/ai/business/formula/validate | 验证公式表达式语法 |
| POST | /api/ai/business/formula/preview | 预览公式计算结果 |
| POST | /api/ai/business/object/{objectCode}/publish | 发布时增加 DAG + 循环检测 + 深度校验 |
| POST/PUT/GET | 动态CRUD入口 | 新增/更新/查询时公式计算截入 |
| POST/PUT/DELETE | 动态CRUD入口（从表） | 从表变更后触发主表聚合重算 |

## 6. 测试与验证策略

- **公式解析测试**: Aviator 表达式语法解析正确性
- **计算模式测试**: STORED 保存持久化、VIRTUAL 查询实时计算
- **聚合重算测试**: 从表 CRUD 后主表字段自动更新
- **循环依赖测试**: DAG 拓扑排序拦截循环引用
- **嵌套深度测试**: 超过 3 层嵌套时发布拦截
- **错误处理测试**: 公式异常时日志记录 + 降级处理
- **覆盖率**: 核心公式引擎 85%+，公式服务 80%+

## 7. 技术决策

| 决策项 | 方案 | 放弃方案 |
|--------|------|---------|
| 表达式引擎 | Aviator | MVEL（体积大）、自研（成本高） |
| 存储策略 | 字段扩展 JSON | 独立公式表（复杂度高） |
| 聚合触发 | 自动重算 | 手动触发、定时触发 |
| 循环检测 | DAG 拓扑排序 | — |
| 跨对象 | V1 不支持 | — |
| 嵌套深度 | 最大 3 层 | — |

## 8. 待澄清项

全部 8 项已确认，无待澄清项。

## 9. 验收标准

- **AC-001**: Given 字段配置 STORED 计算公式 	otal = price * qty, When 保存记录且 price=100, qty=3, Then total 持久化为 300
- **AC-002**: Given 字段配置 VIRTUAL 计算公式 	otal = price * qty, When 查询记录且 price=100, qty=3, Then total 返回 300 且不存储
- **AC-003**: Given 主表字段配置聚合公式 SUM(detail.amount), When 从表新增一条 amount=50 的记录, Then 主表字段自动更新
- **AC-004**: Given 表单字段 A 引用 B, B 引用 C, C 引用 A, When 发布时, Then 检测循环依赖并禁止发布
- **AC-005**: Given 公式嵌套深度 4 层, When 发布时, Then 拦截并提示超限
- **AC-006**: Given 公式表达式语法错误, When 调用 validate 接口, Then 返回具体错误位置和原因
- **AC-007**: Given 配置公式的字段, When 用户在表单中编辑该字段, Then 字段不可编辑（只读）
- **AC-008**: Given 公式计算时抛出异常, When 查询/保存, Then 记录 ERROR 日志，公式字段保持原值或置 null

## 归档记录（HARD-GATE）
- **状态**：done
- **归档时间**：2026-06-27
- **归档人**：yaomd（批量归档）
- **归档路径**：code-copilot/changes/archive/2026-06-27-formula-capability-extension/
- **判定依据**：任务清单全部完成，execution-log 验证通过（编译/构建/lint 闭环）。
