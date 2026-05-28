# 业务定义驱动 AI 大屏生成
> status: apply
> created: 2026-05-17
> complexity: 高

## 1. 背景与目标

现有 AI 大屏主要根据用户输入的需求描述生成组件和静态示例数据，缺少业务定义和数据集元数据约束，导致生成结果不是数据驱动的。用户已经在数据资产中准备好数据连接、数据集和字段语义后，希望先定义一个“业务定义”，把业务目标、指标口径、分析场景和多个数据集绑定起来，再让 `forge-report-ui` 的 AI 大屏生成基于该业务定义生成，并尽量自动绑定到对应数据集。

目标：
- 在数据资产管理中新增“业务定义”，支持描述业务基本定义、分析目标、指标口径和使用建议。
- 一个业务定义可绑定多个已发布数据集。
- `forge-report-ui` AI 生成大屏时可选择业务定义。
- 后端提示词增加业务定义和数据集字段元数据上下文。
- AI 返回组件时可声明数据集绑定；前端应用到画布时自动写入组件 `request` 数据集配置。
- 无法绑定数据集的组件仍允许保留静态数据。

## 2. 范围

### 本期包含
- 后端新增业务定义表、绑定表、CRUD、详情和 AI 上下文接口。
- admin-ui 新增业务定义管理页。
- report-ui AI 助手新增业务定义选择。
- AI 请求模型新增 `businessDefinitionId` 和 `businessContext`。
- AI 提示词模板新增业务定义数据上下文和数据集绑定输出规则。
- AI 引擎应用组件时识别 `request` 字段并自动绑定数据集。

### 本期不包含
- 自动生成 SQL。
- 复杂指标血缘、指标计算引擎和多表语义建模。
- 数据集运行时自动聚合改造。
- 业务定义审批流。

## 3. 数据模型

新增表：
- `ai_report_data_business_definition`
  - 业务定义主表，保存编码、名称、业务描述、目标、指标口径、分析维度、使用建议、状态。
- `ai_report_data_business_dataset`
  - 业务定义与数据集多对多绑定表，保存排序、用途说明和主数据集标识。

## 4. 接口

- `GET /data/business/page`
- `GET /data/business/list`
- `GET /data/business/{id}`
- `POST /data/business`
- `PUT /data/business`
- `DELETE /data/business/{id}`
- `GET /data/business/{id}/ai-context`

## 5. AI 生成规则

业务上下文包含：
- 业务定义：名称、编码、业务描述、分析目标、指标口径、分析维度、使用建议。
- 绑定数据集：数据集 ID、编码、名称、类型、描述、参数定义。
- 字段元数据：字段名、显示名、类型、维度/指标角色、默认聚合、单位、维度绑定。

AI 组件输出可带：

```json
{
  "request": {
    "datasetId": 123,
    "datasetName": "订单销售数据集",
    "datasetFields": ["order_date", "amount"],
    "datasetMapping": {
      "mode": "auto",
      "fieldMap": {
        "category": "order_date",
        "value": "amount"
      },
      "syncHeader": true
    }
  }
}
```

前端应用规则：
- `request.datasetId` 有值时，将组件请求模式设置为 `DATASET`。
- 继承默认 request 配置，只覆盖数据集相关字段。
- AI 同时返回静态 `option.dataset` 时保留为兜底预览数据，运行时由数据集请求刷新。
- 未返回 `request` 或无法匹配数据集时按静态数据处理。

## 6. 风险

- 不涉及资金、权限变更和状态流转。
- 新增接口复用数据集现有访问能力，仅向 AI 上下文暴露当前用户可查询的已发布数据集。
- 提示词中的字段元数据需要控制长度，避免超出模型上下文。
