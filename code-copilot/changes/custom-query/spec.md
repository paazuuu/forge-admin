# 自定义查询组件

## 目标

为配置化 CRUD 页面提供通用的自定义查询能力。用户可以按工作需要组合查询条件、选择结果字段和展示方式，并保存为个人常用查询方案，后续快速调用。

## 范围

- 前端在 `AiCrudPage` 基础组件上新增可选的自定义查询入口。
- 后端基于现有 `AiCrudConfig` 元数据执行动态查询，禁止开放任意 SQL。
- 查询方案按租户、用户、`configKey` 隔离保存。
- 动态 CRUD 页面 `/ai/crud-page/:configKey` 默认启用自定义查询。

## 功能需求

1. 用户可在查询面板中添加多条条件，条件包含字段、操作符、值和连接关系。
2. 用户可选择结果展示字段，字段顺序按选择顺序生效。
3. 用户可选择结果展示方式：列表或卡片。
4. 用户可保存、更新、删除和设为默认查询方案。
5. 用户可加载已有方案并立即执行查询。
6. 未启用自定义查询时，原有 `AiCrudPage` 行为不变。

## 后端约束

- 自定义查询只允许作用于 `AiCrudConfig.mode = CONFIG` 且启用的配置。
- 表名来自 `AiCrudConfig.tableName`，字段只能来自配置表实际列和 `columnsSchema/searchSchema/editSchema` 中的字段。
- 所有动态 SQL 必须使用白名单字段和命名参数绑定。
- 查询自动叠加租户条件和逻辑删除条件，与现有 `DynamicCrudRepository` 行为一致。
- 查询结果保持现有动态 CRUD 的字段驼峰转换、解密、字典翻译和脱敏顺序。

## 数据库

新增表 `ai_custom_query_scheme`：

- `id`
- `tenant_id`
- `config_key`
- `scheme_name`
- `conditions_json`
- `columns_json`
- `sort_json`
- `display_json`
- `is_default`
- 标准审计字段：`create_by`, `create_time`, `create_dept`, `update_by`, `update_time`
- `remark`

## 接口

- `POST /ai/custom-query/{configKey}/execute`
- `GET /ai/custom-query/{configKey}/scheme/list`
- `GET /ai/custom-query/{configKey}/scheme/{id}`
- `POST /ai/custom-query/{configKey}/scheme`
- `PUT /ai/custom-query/{configKey}/scheme`
- `DELETE /ai/custom-query/{configKey}/scheme/{id}`

## 验证

- 后端至少运行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`。
- 前端至少运行 `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build` 或能说明无法运行的原因。
