# 低代码 CRUD 可视化搭建器

低代码 CRUD 搭建器面向业务人员提供单表业务应用的零代码搭建能力。第一版采用 Naive UI 自研界面，复用平台已有 `AiCrudPage`、动态 CRUD、字典、脱敏、加解密、菜单注册和权限体系。树形表按“树形单表”支持，主子表运行时暂未开放。

## 入口

- 应用列表：`/ai/lowcode-apps`
- 应用搭建器：`/ai/lowcode-builder/:id?`
- 运行页面：`/ai/crud-page/:configKey`

旧入口 `/ai/crud-config` 保留为技术人员高级配置入口，主操作已收敛到低代码应用列表。

## 搭建流程

1. 数据模型

   配置应用名称、`configKey`、应用类型、数据表模式、表名和业务字段。标准单表和树形单表都必须来自同一张业务表；主子表、跨表聚合和流程编排暂不开放运行时。

2. 页面搭建

   通过业务组件库启用和配置查询筛选区、数据展示列表、数据录入表单、详情区。字段来源必须来自当前 `modelSchema.fields`。树形单表可选择“左树右表”模板，并配置父级字段和节点显示字段。

3. 实时预览

   预览草稿结构并调用后端校验接口。草稿预览不会注册菜单，也不会对业务用户可见。

4. 发布上线

   发布时将草稿协议转换为正式 `AiCrudPage` 运行配置，写入版本快照，并注册或更新菜单。

## 数据表模式

### 创建新业务表

选择“创建新业务表”时，系统可生成 DDL 预览。发布时如果选择“在线建表/补字段”，必须满足：

- 当前用户具备 `ai:lowcode:deploy-ddl` 权限。
- 已勾选在线 DDL 二次确认。
- DDL 只包含 `CREATE TABLE IF NOT EXISTS` 或 `ALTER TABLE ... ADD COLUMN`。

系统不会在线删除表、删除字段、重命名字段或修改字段类型。

### 绑定已有表

选择“绑定已有表”时，发布默认不执行 DDL。目标表必须已存在，并包含模型中配置的字段。动态 CRUD 运行时会按字段白名单进行读写。

## 协议说明

`modelSchema` 描述业务对象和字段：

```json
{
  "appType": "SINGLE",
  "tableMode": "CREATE",
  "tableName": "biz_contract",
  "businessName": "合同管理",
  "treeConfig": {
    "keyField": "id",
    "parentField": "parentId",
    "labelField": "name",
    "childrenField": "children",
    "treeTitle": "树形导航"
  },
  "fields": [
    {
      "field": "contractName",
      "columnName": "contract_name",
      "label": "合同名称",
      "dataType": "varchar",
      "length": 128,
      "required": true,
      "searchable": true,
      "listVisible": true,
      "formVisible": true,
      "componentType": "input",
      "queryType": "like"
    }
  ]
}
```

树形单表要求：

- `appType` 设置为 `TREE`，或 `pageSchema.layoutType` 设置为 `tree-crud`。
- 模型中必须存在父级字段，默认推荐 `parentId`，数据库列名为 `parent_id`。
- 运行时会自动把父级字段作为隐藏查询/写入字段，业务人员无需在搜索表单或录入表单中手动填写。
- 发布后左侧树调用 `/ai/crud/{configKey}/tree`，右侧列表继续调用 `/ai/crud/{configKey}/page` 并按父级字段过滤。

`pageSchema` 描述页面区域和组件参数：

```json
{
  "layoutType": "simple-crud",
  "zones": [
    {
      "zoneKey": "search",
      "componentKey": "search-form",
      "enabled": true,
      "fieldRefs": ["contractName"]
    },
    {
      "zoneKey": "table",
      "componentKey": "data-table",
      "enabled": true,
      "fieldRefs": ["contractName"],
      "props": {
        "showImport": true,
        "showExport": true,
        "hideBatchDelete": false
      }
    }
  ]
}
```

## 权限

- `ai:lowcode:list`：低代码应用列表。
- `ai:lowcode:edit`：低代码搭建器。
- `ai:lowcode:publish`：发布低代码应用。
- `ai:lowcode:rollback`：回滚历史版本。
- `ai:lowcode:deploy-ddl`：在线建表或追加字段。

## 约束

- `configKey` 必须小写字母开头，仅允许小写字母、数字、下划线。
- 业务表名必须小写下划线，不能使用 `sys_`、`ai_`、`gen_`、`flow_` 等系统保留前缀。
- 基础审计字段不能作为业务字段暴露给表单。
- 字典字段必须配置 `dictType`，运行时使用字典组件渲染。
- API 占位符必须使用 `:id`，不能使用 `{id}`。
- 草稿不会影响线上运行页，发布后运行页读取发布态配置。
- 批量导入仅写入编辑表单允许字段，模板表头使用业务中文名；字典字段可填写字典标签或字典值，后台统一转换为字典值入库。
- 数据导出仅导出列表可见字段，读取链路会继续执行解密、字典翻译和脱敏处理，避免导出敏感明文。
- 如果后台 `sys_excel_column_config` 存在同 `configKey` 的列配置，动态导出会按该配置覆盖列顺序、表头和字典类型。
- 主子表协议字段已预留，但当前选择 `MASTER_DETAIL` 会被后端拦截并提示尚未启用。
