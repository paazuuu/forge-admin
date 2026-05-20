# 任务清单：visual-lowcode-crud-builder
> status: propose
> created: 2026-05-19
> 拆分顺序：数据模型 → 协议工具 → 发布服务 → 运行时能力 → 前端搭建器 → AI 接入 → 验证

## 前置条件
- [x] 已确认现有动态 CRUD 运行时入口为 `/ai/crud-page/:configKey` + `/ai/crud/{configKey}`。
- [x] 已确认 `AiCrudPage` 已支持搜索、列表、表单、导入按钮、导出按钮、自定义查询等基础能力。
- [x] 已确认项目已有 Naive UI 版 `FormDesigner` 雏形和 `vuedraggable` 依赖。
- [x] 已调研 `form-create-designer`：MIT，Vue3 版本主要面向 Element Plus / Ant Design Vue / Vant。
- [x] 已确认第一版支持受控在线创建新表，需独立权限和发布确认。
- [x] 已确认第一版聚焦单表 CRUD；树形表按树形单表扩展，暂不做主子表运行时。
- [x] 已确认第一版不新增第三方表单设计器运行时依赖，采用 Naive UI 自研。
- [x] 已确认发布菜单默认挂在 `AI管理` 目录。

## 任务总览

| Task | 名称 | 状态 | 优先级 |
|------|------|------|--------|
| Task 1 | 数据库迁移与实体扩展 | completed | P0 |
| Task 2 | 低代码协议与转换服务 | completed | P0 |
| Task 3 | 草稿、发布、版本与回滚服务 | completed | P0 |
| Task 4 | 安全 DDL 预览与执行 | completed | P0 |
| Task 5 | 低代码后端 API | completed | P0 |
| Task 6 | 动态 CRUD 导入导出 | completed | P1 |
| Task 7 | 前端 API 与工作台入口 | completed | P0 |
| Task 8 | 可视化数据模型设计器 | completed | P0 |
| Task 9 | 拖拽式页面搭建器 | completed | P0 |
| Task 10 | 实时预览与发布面板 | completed | P0 |
| Task 11 | AI 生成协议升级 | partial | P1 |
| Task 12 | 菜单资源、路由与入口收敛 | completed | P0 |
| Task 13 | 验证、文档与测试 Spec | completed | P0 |
| Task 14 | 树形单表拓展与 Excel 配置兼容 | completed | P1 |

---

## Task 1: 数据库迁移与实体扩展

**目标**: 为低代码草稿、页面协议、发布状态和版本快照建立持久化基础。

**状态**: completed

**涉及文件**:
- `forge/db/migration/V1.0.4__add_visual_lowcode_crud_builder.sql` — 新增 Flyway 脚本，扩展 `ai_crud_config` 并创建 `ai_crud_config_version`。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiCrudConfig.java` — 新增低代码字段。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiCrudConfigVersion.java` — 新增版本实体。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/AiCrudConfigDTO.java` — 补充低代码字段。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/AiCrudConfigRenderVO.java` — 补充页面协议和发布状态只读字段。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/AiCrudConfigVersionMapper.java` — 新增版本 Mapper。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudConfigVersionMapper.xml` — 新增版本查询 SQL。

**关键签名**:
```java
public class AiCrudConfigVersion extends TenantEntity {
    private Long id;
    private Long configId;
    private String configKey;
    private Integer versionNo;
    private String versionType;
    private String modelSchema;
    private String pageSchema;
    private String searchSchema;
    private String columnsSchema;
    private String editSchema;
    private String apiConfig;
    private String options;
    private String publishSnapshot;
    private String remark;
}
```

**验收标准**:
- 迁移脚本可重复执行，不修改 `V1.0.0__baseline.sql`。
- 新增内置菜单和权限资源使用 `tenant_id=1`，并通过 `NOT EXISTS` 防重复。
- `ai_crud_config_version` 包含标准审计字段和必要索引。

**验证**:
```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

**结果**:
- 生成器模块及依赖模块编译通过。
- 输出存在既有 javac warning：`-source 17` 未设置系统模块路径、`GenTableServiceImpl` 过时 API、`SchemaGenerator` unchecked 操作。

---

## Task 2: 低代码协议与转换服务

**目标**: 将业务模型和页面协议转换为现有 `AiCrudPage` 运行时配置。

**状态**: completed

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeModelSchema.java` — 新增模型协议 DTO。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeFieldSchema.java` — 新增字段协议 DTO。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodePageSchema.java` — 新增页面协议 DTO。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeRuntimeConfig.java` — 新增运行时配置 DTO。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeSchemaValidator.java` — 新增协议校验。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java` — 新增协议转换。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiCrudConfigService.java` — `buildRenderVO` 合并低代码协议转换结果。

**关键签名**:
```java
public void validateModel(LowcodeModelSchema modelSchema);
public void validatePage(LowcodePageSchema pageSchema, LowcodeModelSchema modelSchema);
public LowcodeRuntimeConfig buildRuntimeConfig(String configKey, LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema);
```

**验收标准**:
- 字段名、表名、列名、数据类型、查询类型、组件类型均有白名单校验。
- `pageSchema` 中引用不存在的字段时返回明确业务异常。
- 生成的 `apiConfig` 全部使用 `:id` 占位符。
- 字典字段生成 `dictConfig/transConfig`，敏感字段生成 `desensitizeConfig/encryptConfig`。

**验证**:
```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -DskipTests=false
```

**结果**:
- 已完成低代码单表 `modelSchema/pageSchema` DTO、协议校验器和运行时配置转换器。
- `AiCrudConfigService#getRenderConfig` 已兼容低代码协议：存在 `modelSchema/pageSchema` 时转换为 `AiCrudPage` 配置，否则保持旧配置路径。
- 实际验证执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`，编译通过；单元测试将在测试规格阶段补齐。

---

## Task 3: 草稿、发布、版本与回滚服务

**目标**: 建立草稿和发布分离机制，一键发布时生成版本快照并注册菜单。

**状态**: completed

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeAppDraftDTO.java` — 新增草稿保存请求。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodePublishDTO.java` — 新增发布请求。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/lowcode/LowcodeAppDetailVO.java` — 新增应用详情响应。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/lowcode/LowcodeVersionVO.java` — 新增版本响应。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeAppService.java` — 新增应用草稿和详情服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodePublishService.java` — 新增发布、回滚服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiCrudConfigService.java` — 创建配置时不再强制作为低代码草稿注册菜单；保留旧入口兼容。

**关键签名**:
```java
public Long saveDraft(LowcodeAppDraftDTO dto);
public LowcodeAppDetailVO getDetail(Long id);
public AiCrudConfigRenderVO preview(Long id, LowcodeAppDraftDTO draft);
public Long publish(Long id, LowcodePublishDTO dto);
public void rollback(Long id, Long versionId);
public List<LowcodeVersionVO> listVersions(Long id);
```

**验收标准**:
- 保存草稿不会注册菜单。
- 首次发布注册菜单，再次发布更新菜单名称和排序。
- 每次发布都写入 `ai_crud_config_version`。
- 回滚只能选择同一 `configId` 下的历史版本。
- 已授权菜单删除仍沿用 `hasRolePermission` 防护。

**验证**:
```bash
mvn -pl forge-admin-server -am compile -DskipTests
```

**结果**:
- 新增 `LowcodeAppService` 与 `LowcodePublishService`，支持草稿保存、草稿预览、发布、版本列表和回滚。
- 草稿保存不注册菜单；正式运行页只读取发布时固化的运行时配置，避免草稿变更提前影响线上页面。
- 首次发布注册菜单，默认挂载到 `/ai` 的 AI 管理目录；再次发布更新菜单名称和排序。
- 发布与回滚均写入 `ai_crud_config_version` 快照。
- `forge-admin-server` 聚合编译通过。

---

## Task 4: 安全 DDL 预览与执行

**目标**: 支持单表业务模型从可视化配置生成 DDL，并在发布时受控在线建表。

**状态**: completed

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDdlService.java` — 新增 DDL 生成和校验服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDdlRepository.java` — 新增受控 DDL 执行仓储。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeDdlPreviewDTO.java` — 新增 DDL 预览请求。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/lowcode/LowcodeDdlPreviewVO.java` — 新增 DDL 预览响应。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodePublishService.java` — 发布时按 `deployMode` 调用 DDL。

**关键签名**:
```java
public LowcodeDdlPreviewVO previewCreateTable(LowcodeModelSchema modelSchema);
public void executeCreateTable(LowcodeModelSchema modelSchema);
public boolean tableExists(String tableName);
```

**验收标准**:
- 仅允许 `CREATE TABLE IF NOT EXISTS` 和受控 `ALTER TABLE ADD COLUMN`。
- 表名、字段名、索引名全部通过白名单。
- 默认不执行 DDL；只有发布请求 `deployMode=ONLINE_CREATE_TABLE` 且当前用户有 `ai:lowcode:deploy-ddl` 权限时执行。
- 第一版不允许在线删除表、删除字段、重命名字段或修改字段类型。
- DDL 操作必须记录 `@OperationLog`。

**验证**:
```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -DskipTests=false
```

**结果**:
- 新增 `LowcodeDdlService` 与 `LowcodeDdlRepository`，根据单表 `modelSchema` 生成 `CREATE TABLE IF NOT EXISTS` 或受控 `ALTER TABLE ADD COLUMN`。
- 在线 DDL 发布必须使用 `deployMode=ONLINE_CREATE_TABLE`、二次确认并具备 `ai:lowcode:deploy-ddl` 权限。
- 已有表仅允许追加缺失字段，不删除、不重命名、不修改字段类型。
- DDL 执行后清理动态 CRUD 表结构缓存，避免追加字段后运行时仍读旧列集合。
- 实际验证执行 `mvn -pl forge-admin-server -am compile -DskipTests`，编译通过；单元测试将在测试规格阶段补齐。

---

## Task 5: 低代码后端 API

**目标**: 对前端搭建器提供完整草稿、预览、发布、版本和模型校验接口。

**状态**: completed

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/LowcodeAppController.java` — 新增应用接口。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/LowcodeModelController.java` — 新增模型校验和 DDL 预览接口。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/AiCrudConfigMapper.java` — 新增低代码分页方法。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudConfigMapper.xml` — 新增分页 SQL，避免管理查询继续用 Service `LambdaQueryWrapper`。

**关键签名**:
```java
@GetMapping("/page")
public RespInfo<Page<LowcodeAppDetailVO>> page(PageQuery pageQuery, String keyword, String publishStatus);

@PostMapping("/draft")
public RespInfo<Long> saveDraft(@RequestBody LowcodeAppDraftDTO dto);

@PostMapping("/{id}/publish")
public RespInfo<Long> publish(@PathVariable Long id, @RequestBody LowcodePublishDTO dto);
```

**验收标准**:
- Controller 统一使用 `@ApiEncrypt` / `@ApiDecrypt`。
- 分页参数使用 `PageQuery` 的 `pageNum/pageSize`。
- 发布、回滚、DDL 预览和草稿保存有操作日志。
- 查询 SQL 写在 Mapper XML 中。

**验证**:
```bash
mvn -pl forge-admin-server -am compile -DskipTests
```

**结果**:
- 新增 `LowcodeAppController` 和 `LowcodeModelController`。
- 新增 `/ai/lowcode/app/page`、草稿保存、详情、预览、发布、版本、回滚接口。
- 新增 `/ai/lowcode/model/validate` 和 `/ai/lowcode/model/ddl/preview` 接口。
- 新增 `AiCrudConfigMapper.xml` 低代码分页查询 SQL，避免低代码管理分页继续写在 Service `LambdaQueryWrapper`。
- Controller 统一使用 `@ApiDecrypt` / `@ApiEncrypt`，关键操作补充 `@OperationLog`。

---

## Task 6: 动态 CRUD 导入导出

**目标**: 让低代码页面的批量导入、数据导出、导入模板成为真实可用能力。

**状态**: completed

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/DynamicCrudController.java` — 新增导入、导出、模板接口。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudExcelService.java` — 新增 Excel 模板、导入、导出服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java` — 暴露字段白名单和批量写入能力。
- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` — 导出接口按 blob 下载处理，补齐文件名。
- `forge-admin-ui/src/views/ai/crud-page.vue` — 从渲染配置注入导入导出 URL。

**关键签名**:
```java
public void importExcel(String configKey, MultipartFile file);
public void exportExcel(String configKey, DynamicCrudQuery query, HttpServletResponse response);
public void downloadImportTemplate(String configKey, HttpServletResponse response);
```

**验收标准**:
- 导入只写入 `editSchema` 允许字段。
- 导出只导出 `columnsSchema` 可见字段，敏感字段按脱敏规则处理。
- 导入模板使用中文字段名，隐藏审计字段。
- 前端点击导出会真实下载文件，不只是弹出“导出成功”。
- 字典导入支持按字典标签反查字典值，同时兼容直接填写字典值。
- 已有 `sys_excel_export_config/sys_excel_column_config` 继续用于固定 Service Bean 导出场景；动态 CRUD 第一版复用 EasyExcel 和列配置语义，不直接依赖固定 `query_method`。

**验证**:
```bash
mvn -pl forge-admin-server -am compile -DskipTests
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**结果**:
- 新增 `/ai/crud/{configKey}/import`、`/export`、`/import-template`。
- 导出按 `columnsSchema` 输出当前搜索条件下的数据，读取链路复用解密、字典翻译、脱敏处理。
- 导入按 `editSchema` 模板写入，校验必填字段，字典字段支持“标签 -> 值”转换，校验失败时不写入任何数据。
- AiCrudPage 导入改为自定义上传请求，补齐认证、防重放头并避免 multipart 被请求体加密；导出/模板按 blob 文件流下载。

---

## Task 7: 前端 API 与工作台入口

**目标**: 新增低代码工作台页面和前端 API 封装。

**状态**: completed

**涉及文件**:
- `forge-admin-ui/src/api/lowcode-crud.js` — 新增低代码 API。
- `forge-admin-ui/src/views/ai/lowcode-builder.vue` — 新增四步工作台主页面。
- `forge-admin-ui/src/views/ai/lowcode-builder.css` — 新增工作台样式。
- `forge-admin-ui/src/views/ai/lowcode-apps.vue` — 新增低代码应用列表。
- `forge-admin-ui/src/router/index.js` — 补充静态兼容路由 `/ai/lowcode-builder/:id?`。

**关键签名**:
```js
export function lowcodeAppPage(params) {}
export function lowcodeAppDetail(id) {}
export function lowcodeSaveDraft(data) {}
export function lowcodePreview(id, data) {}
export function lowcodePublish(id, data) {}
export function lowcodeVersions(id) {}
export function lowcodeRollback(id, versionId) {}
export function lowcodeValidateModel(data) {}
export function lowcodeDdlPreview(data) {}
```

**验收标准**:
- 工作台首屏直接是可操作搭建界面，不做营销式介绍页。
- 顶部步骤清晰显示“数据模型、页面搭建、实时预览、发布上线”。
- 草稿自动保存和手动保存都有明确状态反馈。
- 页面不要求用户直接编辑 JSON。

**验证**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**结果**:
- 新增 `src/api/lowcode-crud.js`，封装低代码应用、模型校验、DDL 预览、发布和回滚接口。
- 新增 `src/views/ai/lowcode-apps.vue` 应用管理入口和 `src/views/ai/lowcode-builder.vue` 四步搭建器。
- 新增手动路由 `/ai/lowcode-builder/:id?`，兼容创建和编辑同一工作台。
- 前端生产构建通过。

---

## Task 8: 可视化数据模型设计器

**目标**: 让业务人员通过表格和属性面板完成单表数据模型配置。

**状态**: completed

**涉及文件**:
- `forge-admin-ui/src/components/lowcode-builder/model/LowcodeModelDesigner.vue` — 新增模型设计器容器。
- `forge-admin-ui/src/components/lowcode-builder/model/ModelFieldTable.vue` — 新增字段列表和排序。
- `forge-admin-ui/src/components/lowcode-builder/model/ModelFieldPropertyPanel.vue` — 新增字段属性面板。
- `forge-admin-ui/src/components/lowcode-builder/model/model-schema.js` — 新增前端协议默认值和校验辅助。
- `forge-admin-ui/src/components/lowcode-builder/shared/FieldTypeSelect.vue` — 新增字段类型选择器。
- `forge-admin-ui/src/components/lowcode-builder/shared/DictTypeSelect.vue` — 新增字典类型选择器，复用系统字典接口。

**关键能力**:
- 新增字段、复制字段、删除字段、拖拽排序。
- 配置字段中文名、字段名、数据库列名、数据类型、控件类型、长度、精度、必填、搜索、列表、表单、字典、敏感级别、加密策略。
- 支持从已有表结构导入字段。
- 支持生成 DDL 预览。
- 第一版不提供主子表、跨表关联和聚合字段配置。

**验收标准**:
- 字段名修改时自动同步列名建议，但允许手动调整。
- 字典字段必须选择或创建 dictType。
- 敏感字段默认带脱敏建议，禁止默认导出原文。
- 模型校验失败时定位到具体字段行。

**验证**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**结果**:
- 新增 `LowcodeModelDesigner`、字段表格、字段属性面板、字段类型和字典类型选择器。
- 支持创建新表/绑定已有表、字段新增/复制/删除/拖拽排序，以及字段名到列名的自动同步。
- 支持字段数据类型、控件类型、长度、必填、查询、列表、表单、字典、敏感类型和加密算法配置。
- 接入后端模型校验接口。

---

## Task 9: 拖拽式页面搭建器

**目标**: 提供业务组件库、画布和属性配置面板，生成 `pageSchema`。

**状态**: completed

**涉及文件**:
- `forge-admin-ui/src/components/lowcode-builder/page/LowcodePageBuilder.vue` — 新增页面搭建器容器。
- `forge-admin-ui/src/components/lowcode-builder/page/ComponentPalette.vue` — 新增业务组件库。
- `forge-admin-ui/src/components/lowcode-builder/page/BuilderCanvas.vue` — 新增拖拽画布。
- `forge-admin-ui/src/components/lowcode-builder/page/BuilderZone.vue` — 新增查询区、表格区、表单区等区域渲染。
- `forge-admin-ui/src/components/lowcode-builder/page/ComponentPropertyPanel.vue` — 新增组件属性配置。
- `forge-admin-ui/src/components/lowcode-builder/page/page-schema.js` — 新增页面协议默认值和转换辅助。
- `forge-admin-ui/src/components/form-designer/FormDesigner.vue` — 增强为可嵌入编辑表单字段顺序和表单属性。

**关键能力**:
- 拖拽启用/排序查询筛选区、数据表格、编辑表单、导入、导出、批量删除、自定义查询。
- 每个区域可选择字段、调整字段顺序和组件属性。
- 表单区域复用现有 `FormDesigner` 交互，不暴露 JSON。

**验收标准**:
- 画布区域尺寸稳定，拖拽和选中不会造成布局跳动。
- 属性面板只展示与当前组件相关的配置。
- 禁用组件后实时预览不再展示对应能力。
- 生成的 `pageSchema` 可被后端转换服务消费。
- 第一版页面所有字段均来自同一个 `modelSchema.fields` 列表。

**验证**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**结果**:
- 新增 `LowcodePageBuilder`、业务组件库、画布、区域组件和参数面板。
- 支持查询筛选区、数据展示列表、数据录入表单、详情区的启用/禁用、字段选择和拖拽排序。
- 表格区域支持导入、导出、批量删除和自定义查询参数配置。
- 页面协议会随数据模型字段变化清理失效引用。

---

## Task 10: 实时预览与发布面板

**目标**: 让用户在发布前看到真实运行效果，并完成一键上线。

**状态**: completed

**涉及文件**:
- `forge-admin-ui/src/components/lowcode-builder/preview/LowcodePreviewPane.vue` — 新增预览面板，渲染 `AiCrudPage`。
- `forge-admin-ui/src/components/lowcode-builder/publish/PublishPanel.vue` — 新增发布配置和确认面板。
- `forge-admin-ui/src/components/lowcode-builder/publish/VersionTimeline.vue` — 新增版本记录和回滚入口。
- `forge-admin-ui/src/views/ai/lowcode-builder.vue` — 接入预览和发布步骤。
- `forge-admin-ui/src/views/ai/crud-page.vue` — 支持低代码发布态配置回显的页面标题和导入导出地址。

**关键能力**:
- 草稿预览不注册菜单。
- 发布前展示模型校验、页面校验、DDL 校验、菜单配置、权限影响。
- 发布后提供打开页面按钮。
- 历史版本可查看、回滚。

**验收标准**:
- 预览请求失败时展示具体错误，不吞异常。
- 发布按钮在校验未通过时禁用。
- 发布成功后菜单路径可访问动态 CRUD 页面。
- 回滚后预览和正式页面都使用回滚版本。

**验证**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**结果**:
- 新增 `LowcodePreviewPane`，提供查询区、列表区、表单区结构预览，并可调用后端预览校验。
- 新增 `PublishPanel`，支持 DDL 预览、发布方式选择、在线 DDL 二次确认和一键发布。
- 新增 `VersionTimeline`，支持查看版本并触发回滚。
- `crud-page.vue` 已将低代码发布态 `options` 映射为 `AiCrudPage` 运行参数，支持导入、导出、批量删除、自定义查询和弹窗布局开关。
- 前端生产构建通过；构建输出保留项目既有 UnoCSS 图标加载和 chunk 体积 warning。

---

## Task 11: AI 生成协议升级

**目标**: 将 AI CRUD 从直接输出技术 JSON 升级为输出业务模型和页面协议。

**状态**: partial

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/StreamGenerateRequest.java` — 新增 `existingModelSchema/existingPageSchema/selectedComponentKey`。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/CrudGeneratorStreamService.java` — 新增 `modelSchema/pageSchema` 阶段与提示词。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiCrudConfigGenerateService.java` — 修正提示词中 `{id}` 为 `:id`，补充低代码协议输出。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/sql/ai_crud_config.sql` 或新迁移脚本 — 更新 `crud_config_builder` 上下文配置。
- `forge-admin-ui/src/views/ai/crud-generator.vue` — 显示低代码协议生成阶段，支持“应用到模型/页面搭建器”。
- `forge-admin-ui/src/api/crud-generator.js` — 兼容新增 SSE 阶段。

**关键阶段**:
```text
[STAGE:modelSchema]
[STAGE:pageSchema]
[STAGE:searchSchema]
[STAGE:columnsSchema]
[STAGE:editSchema]
```

**验收标准**:
- AI 输出失败时仍可基于表结构规则生成模型草稿。
- 局部优化只修改选中字段或组件，不覆盖用户已调整的整页配置。
- 所有 API 占位符示例均为 `:id`。

**验证**:
```bash
mvn -pl forge-admin-server -am compile -DskipTests
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**当前结果**:
- `AiCrudGenerateResult` 已扩展 `modelSchema/pageSchema/options/layoutType`。
- `AiCrudConfigGenerateService` 已兼容解析 AI 返回的低代码协议，并可通过 `LowcodeRuntimeConfigBuilder` 回填旧运行时 schema。
- AI 降级到表结构规则生成时，会同步返回一份基础 `modelSchema/pageSchema`。
- SSE 阶段展示、选中字段/组件局部优化和“一键应用到低代码搭建器”的前端闭环仍保留为后续迭代。

---

## Task 12: 菜单资源、路由与入口收敛

**目标**: 将旧“CRUD 配置/生成器”入口升级为低代码应用管理和搭建器入口。

**状态**: completed

**涉及文件**:
- `forge/db/migration/V1.0.4__add_visual_lowcode_crud_builder.sql` — 初始化低代码应用管理、低代码搭建器、发布按钮权限、DDL 发布按钮权限。
- `forge-admin-ui/src/views/ai/crud-config.vue` — 调整卡片操作：主操作进入可视化搭建器，技术 JSON 编辑作为高级入口。
- `forge-admin-ui/src/views/ai/crud-generator.vue` — 调整文案为 AI 辅助配置入口，默认引导进入低代码工作台。
- `forge-admin-ui/src/router/index.js` — 确认 `/ai/crud-page/:configKey` 运行时路由不破坏。

**权限建议**:
```text
ai:lowcode:list
ai:lowcode:edit
ai:lowcode:publish
ai:lowcode:rollback
ai:lowcode:deploy-ddl
```

**验收标准**:
- 业务人员入口只展示低代码搭建器和应用管理。
- 技术人员仍可进入原 JSON 配置和代码下载。
- 发布按钮权限和 DDL 权限可独立授权。

**验证**:
```bash
mvn -pl forge-admin-server -am compile -DskipTests
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**结果**:
- Flyway 脚本已初始化低代码应用、搭建器、发布、回滚和在线 DDL 权限资源。
- `/ai/crud-config` 顶部主入口调整为“低代码应用”，低代码配置卡片点击进入可视化搭建器。
- 旧 JSON 抽屉保留为“高级 JSON”，AI 生成器和代码下载仍作为技术人员入口。
- AI 生成页文案调整为“AI 辅助配置”，默认引导业务人员进入低代码工作台。
- 修正 AI 配置相关 `:id` 占位符示例，避免继续生成 `{id}` 历史格式。

---

## Task 13: 验证、文档与测试 Spec

**目标**: 完成自动化验证、手动回归和使用说明。

**状态**: completed

**涉及文件**:
- `code-copilot/changes/visual-lowcode-crud-builder/test-spec.md` — 新增测试规格。
- `code-copilot/changes/visual-lowcode-crud-builder/implementation-summary.md` — 新增实现总结。
- `forge-docs/guide/lowcode-crud-builder.md` — 新增低代码搭建器使用说明。
- `.opencode/memory/pitfalls.md` — 如发现新的踩坑，补充记录。

**回归清单**:
- 从零创建单表业务应用并发布。
- 绑定已有表生成应用并发布。
- 草稿预览不产生菜单。
- 发布后菜单可访问，CRUD 增删改查正常。
- 字典字段展示和搜索正常。
- 图片字段使用鉴权图片渲染。
- 导入模板、导入、导出正常（P1 动态 CRUD 导入导出完成后补测）。
- 回滚版本后正式页面生效。
- 无 DDL 权限用户不能在线建表。

**验证**:
```bash
mvn -pl forge-admin-server -am compile -DskipTests
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**结果**:
- 新增 `test-spec.md`，覆盖草稿、预览、在线 DDL、发布、版本、回滚和前端手动回归。
- 新增 `implementation-summary.md`，记录本次后端、前端、兼容策略和后续迭代。
- 新增 `forge-docs/guide/lowcode-crud-builder.md`，沉淀低代码搭建器入口、流程、权限、协议和约束。
- `mvn -pl forge-admin-server -am compile -DskipTests` 通过，保留既有 javac warning。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build` 通过，保留既有 UnoCSS 图标、CSS 注释和 chunk 体积 warning。
- Excel 导入导出已补齐；AI 协议深度升级作为后续迭代。

---

## Task 14: 树形单表拓展与 Excel 配置兼容

**目标**: 在单表低代码基础上补齐树形表扩展点，并让动态导出兼容后台 Excel 列配置语义。

**状态**: completed

**涉及文件**:
- `LowcodeTreeConfig.java`、`LowcodeModelSchema.java` — 新增 `appType/treeConfig/children` 协议字段，主子表仅做协议预留。
- `LowcodeSchemaValidator.java`、`LowcodeRuntimeConfigBuilder.java` — 树形单表校验、运行时 `/tree` API、隐藏父级查询/写入字段和 `options.treeConfig` 生成。
- `DynamicCrudController.java`、`DynamicCrudService.java` — 新增 `/ai/crud/{configKey}/tree`，将动态表数据组装为树。
- `DynamicCrudExcelService.java` — 按 `configKey` 读取 `ExcelConfigProvider` 列配置，覆盖动态导出列顺序、表头和字典类型。
- `TreeCrudTemplate.vue`、`AiForm.vue` — 左树右表使用 `publicParams` 过滤右侧列表，新增时自动带入父级字段，表单支持隐藏字段。
- `LowcodeModelDesigner.vue`、`model-schema.js`、`LowcodePageBuilder.vue`、`ComponentPropertyPanel.vue`、`page-schema.js` — 搭建器支持“树形单表/左树右表”和树配置面板。

**验收标准**:
- 选择树形单表时自动补齐 `parentId` 父级字段。
- 发布树形应用后运行页左侧树从 `/tree` 接口加载，选择节点后右侧列表按父级字段过滤。
- 新增子节点时隐藏写入父级字段，不要求业务人员手填。
- `MASTER_DETAIL` 选择时返回明确异常，不开放半成品运行时。
- 后台 Excel 列配置存在时，动态导出按配置顺序和表头输出。

**验证**:
```bash
mvn -pl forge-admin-server -am compile -DskipTests
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```
