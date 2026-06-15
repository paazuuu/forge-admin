# 任务拆分 — 低代码应用管理与代码生成闭环整合
> 拆分顺序：协议适配 → 后端服务 → 前端入口 → 菜单迁移 → 兼容验证
> 每个任务应可独立提交；本文件为实施计划，不包含业务代码实现

## 前置条件

- [x] 已确认 `spec.md` 第 15 节 HARD-GATE 问题。
- [x] 已确认模型管理保留现有独立入口，普通用户菜单至少保留“应用管理/应用开发”和“模型管理/模型设计”两个主工作入口。
- [x] 已确认旧 `generator/table`、`ai/crud-config`、`ai/crud-generator` 只做接口/路由兼容，不再扩展新流程。
- [x] 已确认应用级代码下载默认来源：已保存草稿 `DRAFT`，`PUBLISHED`/`VERSION` 作为可选项。
- [x] 已确认数据源管理保留、模板管理菜单去掉。
- [x] 已确认模型导入直接读取数据源表结构，不从旧 `GenTable` 选择。
- [x] 已确认代码生成包名按业务领域配置，首期只支持单表/单主模型。
- [x] 已补充 `test-spec.md`，覆盖 AI 生成、模型导入、代码预览/下载、旧接口兼容。

## Task 1: 定义低代码整合 DTO 与转换边界

- **目标**：明确 AI 应用生成、代码生成、模型导入的请求/响应协议，避免前后端继续直接传纯 JSON 配置。
- **涉及文件**：
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeAiAppGenerateRequest.java` — 新增 AI 应用生成请求。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeAiAppGenerateResult.java` — 新增 AI 应用生成结果。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeCodegenRequest.java` — 新增代码预览/下载参数。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/lowcode/LowcodeCodePreviewVO.java` — 新增代码预览结果。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeModelImportRequest.java` — 新增数据源表结构导入请求。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeDomainSchema.java` — 增加 `codegen` 领域默认配置。
- **关键签名**：
  ```java
  public class LowcodeAiAppGenerateRequest {
      private Long domainId;
      private String description;
      private String layoutType;
      private Long providerId;
      private Long modelId;
      private Boolean autoCreateModel;
      private Boolean includeDdl;
  }

  public class LowcodeCodegenRequest {
      private String sourceType; // DRAFT | PUBLISHED | VERSION
      private Long versionId;
      private Long domainId;
      private String groupId; // defaults from business domain config
      private String domainPackage; // Java base package, defaults from business domain config
      private String moduleName;
      private String author;
      private Boolean includeSql;
      private Boolean includeMenuSql;
      private Boolean includeDictSql;
  }

  public class LowcodeDomainSchema {
      private Codegen codegen;

      public static class Codegen {
          private String groupId;
          private String domainPackage;
          private String moduleName;
          private String frontendBasePath;
      }
  }
  ```
- **验证**：
  - DTO 字段命名符合前端 camelCase、后端 Java bean 约定。
  - 不引入数据库字段，仅作为接口协议。

## Task 2: 建立低代码模型导入适配服务

- **目标**：把数据源数据库表结构转换为 `LowcodeModelSchema`，使模型设计成为唯一模型资产；旧 `GenTable/GenTableColumn` 不进入新导入流程。
- **涉及文件**：
  - `LowcodeModelController.java` — 新增导入/预览接口。
  - `LowcodeDataModelService.java` — 增加保存导入模型的服务方法。
  - `LowcodeModelImportService.java` — 新增转换服务。
  - `GenDatasourceServiceImpl.java` / `IGenDatasourceService.java` — 复用数据源表结构查询。
- **关键签名**：
  ```java
  public LowcodeModelSchema previewDbTableModel(LowcodeModelImportRequest request);

  public Long importDbTableModel(LowcodeModelImportRequest request);
  ```
- **规则**：
  - SQL 查询仍写在 Mapper XML 或复用现有 Mapper 方法，禁止在 Service 用复杂 `LambdaQueryWrapper` 拼查询类 SQL。
  - 审计字段不进入业务字段设计：`id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`。
  - 字段字典、脱敏、组件类型根据数据源字段注释、字段命名规则、字典策略和 AI 建议推断。
- **验证**：
  - 从数据库表预览不落库。
  - 从数据库表导入后能在 `/ai/lowcode/model/{id}` 查到模型详情。
  - 新模型导入流程不读取旧 `GenTable` 数据。

## Task 3: 建立应用级代码生成服务

- **目标**：把代码预览和 ZIP 下载迁移到低代码应用维度，保留旧下载入口兼容。
- **涉及文件**：
  - `LowcodeAppController.java` — 新增 `/code/preview`、`/code/download`、`/code/options`。
  - `LowcodeCodegenService.java` — 新增应用级代码生成服务。
  - `LowcodeCodegenContextBuilder.java` — 新增低代码协议到模板上下文转换。
  - `AiCrudCodegenService.java` — 改为底层委托或保留兼容入口。
  - `VelocityCodegenStrategy.java` — 支持应用级上下文或新的上下文对象。
  - `AiCrudConfigService.java` — 按 appId/configKey/版本加载配置。
- **关键签名**：
  ```java
  public LowcodeCodePreviewVO previewCode(Long appId, LowcodeCodegenRequest request);

  public byte[] downloadCode(Long appId, LowcodeCodegenRequest request);

  public byte[] downloadByConfigKey(String configKey);
  ```
- **规则**：
  - `sourceType` 为空时默认使用已保存草稿 `DRAFT`。
  - 代码生成 groupId、Java 基础包名和代码模块名优先使用请求参数，其次使用业务领域 `domainSchema.codegen`。
  - 最终 Java 包路径为 `domainPackage + "." + moduleName`；若 `domainPackage` 已经以 `moduleName` 结尾，生成前自动剥离重复段。
  - 默认不写入本地文件，只返回预览内容或 ZIP 字节。
  - 代码生成失败不能影响应用草稿和发布版本。
  - 旧 `/ai/crud-config/codegen/download/{configKey}` 继续可用。
- **验证**：
  - 应用草稿能预览文件树。
  - 已发布应用能下载 ZIP。
  - 旧 configKey 下载入口返回相同 ZIP 文件名格式。

## Task 4: 迁移 AI 表单生成到 AI 应用生成

- **目标**：把旧 AI JSON 生成能力改造成“生成模型 + 应用草稿”的闭环。
- **涉及文件**：
  - `LowcodeAppController.java` — 新增 `/ai/stream-generate`、`/{id}/ai/refine`。
  - `LowcodeAiGenerateService.java` — 新增 AI 应用生成编排服务。
  - `CrudGeneratorStreamService.java` — 抽取/复用 SSE chunk 处理和 Prompt 构建能力。
  - `AiCrudConfigGenerateService.java` — 复用低代码协议解析和规则降级能力。
  - `LowcodeRuntimeConfigBuilder.java` — 继续从 `modelSchema/pageSchema` 生成运行时配置。
  - `LowcodeSchemaValidator.java` — 校验 AI 输出模型和页面协议。
- **关键签名**：
  ```java
  public Flux<ServerSentEvent<String>> streamGenerateApp(LowcodeAiAppGenerateRequest request);

  public LowcodeAiAppGenerateResult buildDraftFromAiResult(LowcodeAiAppGenerateResult result);
  ```
- **规则**：
  - 用户只输入业务需求，前端不要求选择业务领域或页面模板。
  - 后端负责自动划分业务领域、生成数据模型、生成应用草稿并选择页面模板。
  - 生成结果必须包含可展示的 `steps[]` 和 `decisions[]`，前端展示决策摘要而不是内部推理链。
  - AI 输出不能直接执行 DDL。
  - AI 输出不能自动发布应用。
  - 用户确认前不保存模型和应用草稿。
- **验证**：
  - 正常生成返回模型建议和应用草稿。
  - AI 返回非法 JSON 时给出错误事件。
  - AI 不可用时规则降级能返回最小草稿。

## Task 5: 前端应用管理主入口整合

- **目标**：把 AI 创建应用、应用设计、代码预览和下载放入应用管理体验，同时提供模型选择、关联和快捷新建能力。
- **涉及文件**：
  - `forge-admin-ui/src/views/ai/lowcode-apps.vue` — 新增 AI 创建应用、代码预览、下载、模型选择/关联入口。
  - `forge-admin-ui/src/views/ai/lowcode-builder.vue` — 增加代码输出面板。
  - `forge-admin-ui/src/api/lowcode-crud.js` — 新增 AI、代码生成、模型导入 API。
  - `forge-admin-ui/src/components/lowcode-builder/code/LowcodeCodePreviewModal.vue` — 新增或从旧 `CodePreviewModal.vue` 抽取。
  - `forge-admin-ui/src/components/lowcode-builder/ai/LowcodeAiGenerateDrawer.vue` — 新增 AI 生成抽屉。
- **规则**：
  - 应用管理首屏仍是工作台，不做营销页。
  - AI 生成面板只保留需求描述输入；领域和模板由 AI 自动判断。
  - AI 生成过程必须清晰展示步骤、领域草稿、模型草稿、应用草稿和模板选择决策。
  - 确认保存时支持批量保存新领域、多个模型和多个应用草稿；已有领域只复用不重复创建。
  - 小屏幕必须避免横向溢出。
  - 代码下载按钮在未保存草稿时禁用并提示先保存。
- **验证**：
  - `pnpm exec eslint --fix` 指定改动文件。
  - `pnpm build` 通过。
  - 手工验证应用列表、AI 抽屉、代码预览、下载按钮。

## Task 6: 前端模型管理保留并吸收表结构导入能力

- **目标**：保留现有模型管理入口，把数据库表导入、AI Schema 生成迁移到低代码模型设计。
- **涉及文件**：
  - `forge-admin-ui/src/views/ai/lowcode-models.vue` — 新增导入表、AI 生成模型入口。
  - `LowcodeModelDesigner.vue` — 支持导入后的字段确认。
  - `ModelFieldTable.vue` / `ModelFieldPropertyPanel.vue` — 保留字段配置能力。
  - `ImportTableModal.vue` — 抽取为低代码模型导入弹窗或新增 `LowcodeModelImportModal.vue`。
  - `AiSchemaModal.vue` — 迁移为低代码模型 AI 生成入口。
- **规则**：
  - 模型管理保留独立菜单，不作为应用管理的隐藏内页。
  - 模型允许不绑定任何应用。
  - 模型导入直接读取数据源表结构，不从旧 `GenTable` 选择。
  - 导入表结构只生成模型，不直接生成应用。
  - 用户可以从导入后的模型继续创建应用。
- **验证**：
  - 从数据源表导入模型成功。
  - 页面不提供从旧 `GenTable` 选择导入的入口。
  - AI 生成模型后字段可继续编辑。

## Task 7: 菜单和权限迁移

- **目标**：收敛旧入口，保留应用管理和模型管理入口，并保留兼容路由和开发者配置入口。
- **涉及文件**：
  - `forge/db/migration/Vx.x.x__unify_lowcode_app_codegen_menus.sql` — 新增 Flyway 脚本。
  - `forge-admin-ui/src/router/index.js` — 保留隐藏路由或重定向。
- **规则**：
  - `sys_resource` 脚本必须 `NOT EXISTS` 防重复。
  - `tenant_id` 必须为 `1`。
  - 不修改已执行迁移脚本。
  - 普通用户菜单保留 `ai/lowcode-apps` 和 `ai/lowcode-models`。
  - 普通用户菜单隐藏旧 `generator/table`、`ai/crud-config`、`ai/crud-generator`。
  - 数据源管理菜单保留为开发者入口。
  - 模板管理菜单去掉。
- **验证**：
  - 应用管理和模型管理菜单登录后可见。
  - 旧路由直接访问时按权限进入诊断页或重定向新入口。
  - 发布应用菜单不受影响。

## Task 8: 兼容与回归验证

- **目标**：确保整合后旧数据、旧接口、已发布低代码应用不失效。
- **涉及文件**：
  - `code-copilot/changes/unified-lowcode-app-codegen/test-spec.md` — 补充测试说明。
  - 后端新增/修改服务对应测试类。
  - 前端主流程手工验证记录。
- **验证命令**：
  ```bash
  cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
    mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-admin-server -am compile -DskipTests
  ```
  ```bash
  cd forge-admin-ui && source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
  ```
- **回归场景**：
  - 已发布低代码应用仍可通过 `/ai/crud-page/{configKey}` 访问。
  - 旧 `/ai/crud-config/codegen/download/{configKey}` 能下载。
  - 旧 `/generator/download/{tableName}` 能下载。
  - 新应用级 `/ai/lowcode/app/{id}/code/download` 能下载。
  - 模型导入不会创建重复字段或覆盖审计字段。

## 后续归档要求

- [x] 实现完成后补 `implementation-summary.md`。
- [x] 如果旧入口退场策略明确，补充 `deprecation-plan.md`。
- [x] 如果发现可复用迁移经验，写入 `code-copilot/memory/decisions.md` 或 `code-copilot/memory/pitfalls.md`。
