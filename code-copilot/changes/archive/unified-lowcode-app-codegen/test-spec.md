# 测试说明 — 低代码应用管理与代码生成闭环整合

## 覆盖范围

- 应用管理 AI 创建应用：用户确认前不保存，确认后保存模型和应用草稿。
- 模型管理从数据源表导入：直接读取数据源表结构，不从 `GenTable` 选择。
- 应用级代码预览/下载：默认使用已保存草稿 `DRAFT`，支持 `PUBLISHED` 和 `VERSION`。
- 旧接口兼容：`/ai/crud-config/codegen/download/{configKey}`、`/generator/download/{tableName}` 保留。
- 菜单迁移：保留应用管理、模型管理、数据源管理，隐藏旧表模型、纯 JSON 配置、AI 表单生成、模板管理入口。

## 验证命令

```bash
cd forge && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-admin-server -am compile -DskipTests
```

```bash
cd forge-admin-ui && pnpm build
```

