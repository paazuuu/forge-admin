# 旧入口退场计划 — 低代码应用管理与代码生成闭环整合

## 首期策略

- 菜单隐藏旧 `generator/table`、`generator/template`、`ai/crud-config`、`ai/crud-generator`、`ai/page-template` 入口。
- 保留旧路由和后端接口，避免历史配置、已发布页面和已有下载入口立即失效。
- 数据源管理继续保留，作为模型设计从数据库表导入的底层能力入口。
- 新功能只扩展 `/ai/lowcode/**`，不再扩展 `/generator/**` 和纯 JSON CRUD 配置入口。

## 兼容范围

- `/ai/crud-config/codegen/download/{configKey}` 保留，低代码配置委托应用级代码生成服务。
- `/generator/preview/{tableName}`、`/generator/download/{tableName}`、`/generator/importTable/**` 保留旧接口兼容。
- `GenTable` 和 `GenTableColumn` 旧数据不删除、不批量迁移，也不作为模型设计导入来源。

## 后续条件

- 确认生产环境没有普通用户依赖旧菜单后，可评估删除前端旧页面入口。
- 确认历史代码下载使用量归零后，再评估删除旧 `/ai/crud-config/codegen/**` 代理入口。
- 删除旧接口前必须新增迁移说明，并保留至少一个版本周期的兼容提示。
