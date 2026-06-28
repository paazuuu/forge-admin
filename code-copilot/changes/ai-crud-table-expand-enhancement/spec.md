# AiCrudPage 表格增强与通用展开面板
> status: propose
> created: 2026-06-27
> complexity: 🔴复杂

## 1. 背景与目标
AiCrudPage 当前已承载低代码运行态、动态 CRUD 页面和页面模板渲染。列表区需要补齐更接近飞书多维表格的表格体验：列宽配置、列宽拖拽、固定头部、固定列，以及最重要的行展开能力。

行展开不能只理解为“展开一个子表”。业务上常见展开内容包括子表、描述信息、只读表单、多个分组、多个子表 Tab、自定义组件和插槽。目标是新增一个通用 `expandConfig` 协议，让用户在设计器里用低门槛配置展开内容，运行态按同一协议加载数据和渲染。

完成后的效果：
- 列宽、固定列、横向滚动、固定头部和列宽拖拽具备统一配置入口。
- 展开面板支持 `table`、`descriptions`、`form`、`tabs`、`custom` 等内容类型，不局限于表格。
- 子表展开可按当前行字段映射参数，懒加载子表数据并缓存。
- 设计器提供“展开面板”配置入口，用户优先通过选择子表/字段/组件完成配置，而不是手写 JSON。
- 运行态兼容现有 AiCrudPage 页面，没有配置展开时不改变原行为。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路
- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`
  - 模板中通过 `<AiTable>` 渲染主列表，传入 `tableColumns`、`dataSource`、`paginationConfig`、`computedMaxHeight`、`computedScrollX` 和 `tableProps`。
  - `tableColumns` 负责把 `props.columns` 转为运行态列，并补默认操作列。
  - `computedScrollX` 会按列 `width/minWidth` 自动计算横向滚动宽度。
  - `computedMaxHeight` 默认返回 `calc(100vh - 280px)`，已经为固定表头提供基础。
- `forge-admin-ui/src/components/ai-form/AiTable.vue`
  - 是 `n-data-table` 的封装，主表通过 `v-bind="$attrs"` 透传部分 Naive UI 属性。
  - 内部再次转换列，当前保留了 `width/minWidth/maxWidth/fixed/ellipsis/sorter/filter` 等列属性。
  - 当前未显式管理展开行、列宽拖拽后的宽度状态和展开内容渲染。
- `forge-admin-ui/src/components/ai-form/AiCrudPageProps.js`
  - 已定义 `columns`、`maxHeight`、`scrollX`、`tableProps`、`childrenConfig` 等 props。
  - `childrenConfig` 当前用于新增/编辑/详情中的 `ChildTableEditor`，不是列表行展开能力。
- `forge-admin-ui/src/components/page-templates/MasterDetailCrudTemplate.vue`
  - 主子表模板把 `childrenConfig` 注入 AiCrudPage，用于表单/详情里的子表编辑。
  - 可复用子表配置来源，但不能直接等同于列表展开面板。
- `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`
  - 构造运行态 `crudProps`，已经传递 `maxHeight`、`scrollX`、`tableSize`、`bordered`、`striped`、`hideSelection` 等列表配置。
- `forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`
  - AiCrudPage 属性面板已有“查询与列表字段”配置区，包含字段排序、列宽、对齐、固定、最大高度、横向宽度等语义。
  - 适合作为新增“展开面板”配置入口。

### 2.2 现有实现
- 列宽：列配置已可带 `width/minWidth/maxWidth`，AiTable 会传给 Naive UI。
- 固定列：列配置已可带 `fixed`，默认操作列固定在右侧。
- 固定头部：AiCrudPage 默认传 `maxHeight`，Naive UI 在有 `max-height` 时可固定表头。
- 横向滚动：AiCrudPage 支持 `scrollX`，未传时按列宽自动计算。
- 主子表：已有 `childrenConfig + ChildTableEditor`，但用于表单区域，不用于主表行展开。

### 2.3 发现与风险
- 展开行属于数据加载和渲染协议，不能只作为 `tableProps` 透传给 `n-data-table`，否则低代码设计器、运行态、预览和生成器无法统一。
- `childrenConfig` 可以作为“推荐子表来源”，但展开面板要支持描述、表单、Tabs 和自定义组件，不能命名成 `childTableExpandConfig`。
- 列宽拖拽分为设计态和运行态两种语义：设计态应写回 schema；运行态可先本地状态，后续再保存到用户偏好。
- 固定列依赖列宽和 `scrollX`，设计器需要引导或自动补默认宽度。

## 3. 功能点
- [ ] 表格基础增强：支持列宽、最小宽、最大宽、固定列、固定头部、横向滚动的统一配置和运行态映射。
- [ ] 列宽拖拽：支持通过 `tableProps.resizable` 开启列宽拖拽，拖拽后更新运行态列宽状态；设计态可写回 schema。
- [ ] 展开开关：支持 `expandConfig.enabled` 开启主表行展开。
- [ ] 展开数据源：支持按当前行映射参数懒加载 API 数据，也支持直接读取当前行嵌套字段。
- [ ] 展开内容类型：支持 `table`、`descriptions`、`form`、`tabs`、`custom`。
- [ ] 子表展开：支持从 `childrenConfig` 或低代码模型关系中选择子表，自动生成展开表格列和参数映射。
- [ ] 描述展开：支持选择字段，以多列描述信息展示当前行详情或远程详情。
- [ ] 表单展开：支持复用 AiForm 只读渲染字段，适合复杂字段布局。
- [ ] Tabs 展开：支持一个展开区域内配置多个面板，例如“明细 / 流程 / 附件 / 日志”。
- [ ] 自定义展开：支持插槽或组件 key，给复杂业务保留逃生口。
- [ ] 设计器入口：在 AiCrudPage 属性面板新增“展开面板”配置，提供常用预设，不要求用户手写 JSON。
- [ ] 预览与发布：低代码预览、运行页和动态 CRUD 页面都按同一 `expandConfig` 渲染。

## 4. 业务规则
- `expandConfig` 是 AiCrudPage 的一级配置，不放入普通列配置中。
- 未配置 `expandConfig.enabled=true` 时，现有列表行为不变。
- 展开面板默认懒加载，首次展开请求数据；`cache=true` 时同一行重复展开复用缓存。
- 展开数据请求参数通过表达式映射当前行，例如 `orderId: row.id`、`tenantId: row.tenantId`。
- 展开内容支持多类型，但第一期必须优先保证 `table`、`descriptions`、`tabs` 可用。
- `table` 展开内的子表默认不显示工具栏，不显示多选，不启用行操作，除非显式配置。
- 固定列必须具备明确宽度；未配置时运行态可自动补默认宽度并在设计器提示。
- 列宽拖拽不能破坏列显示/隐藏、列排序和自定义查询结果展示。

### 4.1 推荐配置协议

```js
expandConfig: {
  enabled: true,
  trigger: 'icon', // icon | row | both
  lazy: true,
  cache: true,
  defaultExpanded: false,
  rowExpandable: {
    type: 'expression',
    expression: 'row.hasDetail != false'
  },
  panels: [
    {
      key: 'items',
      title: '明细',
      type: 'table',
      dataSource: {
        type: 'api',
        api: 'get@/api/order/item/page',
        method: 'get',
        paramsMap: {
          orderId: 'row.id'
        },
        dataField: 'records',
        totalField: 'total'
      },
      table: {
        rowKey: 'id',
        columns: [
          { prop: 'productName', label: '商品名称', width: 180 },
          { prop: 'quantity', label: '数量', width: 100 },
          { prop: 'amount', label: '金额', width: 120 }
        ],
        pagination: false,
        maxHeight: 320
      }
    },
    {
      key: 'summary',
      title: '概览',
      type: 'descriptions',
      dataSource: {
        type: 'row'
      },
      descriptions: {
        columns: 3,
        fields: [
          { field: 'customerName', label: '客户' },
          { field: 'statusName', label: '状态' },
          { field: 'remark', label: '备注', span: 3 }
        ]
      }
    }
  ],
  layout: {
    mode: 'tabs', // single | tabs | stack
    density: 'compact',
    padding: 12
  }
}
```

### 4.2 面板类型

| type | 展示形式 | 数据来源 | 典型场景 |
|------|----------|----------|----------|
| `table` | 内嵌表格 | API / row 字段 / 子表关系 | 订单明细、项目成员、附件列表 |
| `descriptions` | 描述列表 | row / detail API | 飞书式展开概览、关键字段补充 |
| `form` | 只读 AiForm | row / detail API | 复杂字段分组、布局化详情 |
| `tabs` | 多 Tab 容器 | 子 panels | 多个子表或“明细+日志+流程” |
| `custom` | 插槽/组件 | row + dataSource | 特殊业务卡片、图表、时间线 |

## 5. 数据变更
| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 无 | - | - | 本变更优先使用现有页面 schema/config JSON 扩展，不新增数据库表字段。若后续运行态列宽需按用户持久化，可另开用户偏好配置变更。 |

## 6. 接口变更
| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 复用 | 子表/详情接口 | GET/POST | 展开数据源复用已有 CRUD/detail/list 接口，通过 `paramsMap` 传当前行参数。 |
| 无强制新增 | - | - | AiCrudPage 不强制后端新增统一展开接口。低代码运行态如需子表数据，可由发布流程生成或复用已有运行态 API。 |

## 7. 影响范围
- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`
- `forge-admin-ui/src/components/ai-form/AiCrudPageProps.js`
- `forge-admin-ui/src/components/ai-form/AiTable.vue`
- 新增 `forge-admin-ui/src/components/ai-form/AiCrudRowExpand.vue`（建议）
- 新增 `forge-admin-ui/src/components/ai-form/expand-renderers/*`（建议）
- `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`
- `forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`
- `forge-admin-ui/src/components/lowcode-builder/preview/LowcodePreviewPane.vue`
- `forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue`
- 低代码 schema 编译、预览、发布链路中 AiCrudPage props 透传逻辑

## 8. 风险与关注点
- 展开行请求可能造成 N+1 接口压力，必须懒加载并支持缓存。
- 展开面板内嵌表格再嵌套复杂组件时，表格高度、横向滚动和固定列容易产生布局冲突。
- 自定义查询、列筛选和列拖拽都操作列集合，需要明确列宽状态合并顺序。
- 卡片模式下是否展示展开能力需单独定义；第一期建议仅表格模式支持展开。
- 如果展开子表涉及权限和多租户，后端仍必须按现有鉴权、数据权限和租户规则处理，前端只负责参数传递。
- 不涉及资金、状态流转、权限放开。

## 8.5 测试策略
- **测试范围**：前端组件单测/构建、设计器 schema 转换、运行态手工/E2E 验收。
- **覆盖率目标**：核心配置归一化函数、参数映射函数、展开数据加载函数需要单测覆盖；组件渲染通过 E2E 覆盖主要场景。
- **独立 Test Spec**：是，见 `test-spec.md`。

## 9. 待澄清
- [ ] 运行态列宽拖拽是否需要第一期持久化到用户偏好，还是只在当前页面会话生效。
- [ ] 展开面板第一期是否允许编辑子表，还是只读查看。建议第一期只读，编辑仍走详情/编辑弹窗里的 `ChildTableEditor`。
- [ ] `custom` 类型组件来源是否限制在已注册组件白名单，避免任意组件 key 带来安全和维护风险。

## 10. 技术决策
- 展开能力命名为 `expandConfig`，不是 `childTableConfig`，避免限制在子表。
- 展开内容采用 `panels[]`，即使单面板也走同一结构，方便后续 tabs/stack 扩展。
- 子表展开优先使用懒加载，默认缓存每行展开结果。
- 第一优先级支持 `table`、`descriptions`、`tabs`，第二优先级支持 `form`、`custom`。
- `childrenConfig` 只作为子表展开的配置来源之一，不替代 `expandConfig`。

## 11. 执行日志
| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| 方案沉淀 | 已完成 | `spec.md` | 本轮只记录方案，不改业务代码。 |

## 12. 审查结论
待实现后审查。

## 13. 确认记录（HARD-GATE）
- **确认时间**：待确认
- **确认人**：待确认
