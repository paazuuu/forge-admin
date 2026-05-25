# 实现说明 — 低代码应用管理与代码生成闭环整合

## 已完成

- 应用管理新增 AI 创建应用、代码预览、代码下载入口；AI 结果只作为草稿，用户确认后才保存模型和应用草稿。
- 应用搭建器新增“代码输出”步骤和顶部代码预览/下载操作，默认基于已保存草稿生成代码。
- 模型管理保留独立入口，新增从数据源表结构预览导入模型能力，并移除从旧 `GenTable` 表模型同步字段的页面入口。
- 后端新增低代码 AI 生成、模型导入、应用级代码预览/下载服务；旧 `/ai/crud-config/codegen/download/{configKey}` 继续委托兼容。
- `VelocityCodegenStrategy` 支持从低代码模型协议回退构建字段上下文，应用级代码生成可复用现有模板。
- 应用级代码生成参数已落到模板输出：支持 Maven groupId、Java 基础包名、代码模块名、作者、前端输出根路径，以及菜单/字典 SQL 输出开关；无参预览会保留已保存的 `options.codegen`。
- 业务领域 `domainSchema.codegen` 支持保存领域默认 groupId、Java 基础包名、代码模块名和前端路径；最终 Java 包路径会追加代码模块名，并自动剥离重复模块段。
- 新增 Flyway 脚本 `V1.0.19__unify_lowcode_app_codegen_menus.sql`，保留应用开发、模型设计、数据源管理，隐藏旧表模型、模板、纯 JSON 配置和 AI 表单生成菜单。

## 首期边界

- 首期代码生成按单主模型/单表路径支持；主子表、左树右表、树形单表代码生成只保留协议和后续扩展空间。
- AI 生成服务当前提供规则降级草稿，接口和前端确认链路已就位，后续可替换为真实模型调用和 Prompt 编排。
- 模型导入读取数据源表结构，不读取也不迁移旧 `GenTable` 数据。

## 验证

```bash
cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
  mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-admin-server -am compile -DskipTests
```

```bash
cd forge-admin-ui && source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

两条验证均已通过。前端构建存在项目原有 UnoCSS 图标加载和 CSS `//` 注释警告，不影响本次构建结果。
