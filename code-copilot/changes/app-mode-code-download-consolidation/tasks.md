# 任务清单：app-mode-code-download-consolidation
> status: completed
> created: 2026-06-14
> 拆分顺序：契约与迁移 → 后端应用模式 → 应用代码接口 → 代码生成模板 → 前端应用管理 → 旧入口下线 → 术语清理 → 验证
> 原则：先完成 spec/tasks 再编码；不回滚无关工作区改动；查询 SQL 写 Mapper XML；分页参数使用 `pageNum/pageSize`；内置数据 `tenant_id=1`；用户可见文案不出现 CRUD。

## 阶段总览

| 阶段 | 目标 | 包含任务 | 交付结果 |
|------|------|----------|----------|
| Phase 0 | 规格冻结 | Task 0 | 确认接口前缀、旧入口兼容策略和术语表 |
| Phase 1 | 后端模型 | Task 1-2 | 应用模式字段、options 解析、权限和迁移 |
| Phase 2 | 代码能力 | Task 3-5 | 应用管理代码预览、下载、业务接口生成、完整 zip |
| Phase 3 | 前端应用管理 | Task 6-8 | 应用模式选择、代码面板、应用总览操作收敛 |
| Phase 4 | 旧入口和术语 | Task 9-10 | 旧配置入口下线，高级配置隐藏技术入口 |
| Phase 5 | 验证收尾 | Task 11-12 | 自动化验证、执行日志、文档回填 |

## 任务总览

| Task | 阶段 | 名称 | 状态 | 优先级 |
|------|------|------|------|--------|
| Task 0 | Phase 0 | 规格确认与实现边界冻结 | completed | P0 |
| Task 1 | Phase 1 | 应用模式后端字段与 options 映射 | completed | P0 |
| Task 2 | Phase 1 | 字典、菜单和权限迁移脚本 | completed | P0 |
| Task 3 | Phase 2 | 应用管理代码预览/下载后端接口 | completed | P0 |
| Task 4 | Phase 2 | 业务专属接口路径解析与校验 | completed | P0 |
| Task 5 | Phase 2 | 代码生成模板标准化与完整 zip | completed | P0 |
| Task 6 | Phase 3 | 前端 API 迁移到应用管理 | completed | P0 |
| Task 7 | Phase 3 | 应用编辑抽屉新增使用模式 | completed | P0 |
| Task 8 | Phase 3 | 应用总览入口操作和代码面板整合 | completed | P0 |
| Task 9 | Phase 4 | 旧 `/ai/crud-config` 入口下线 | completed | P0 |
| Task 10 | Phase 4 | 高级配置和终端用户术语清理 | completed | P0 |
| Task 11 | Phase 5 | 自动化测试与构建验证 | completed | P0 |
| Task 12 | Phase 5 | 变更文档、执行日志和验收回填 | completed | P1 |

---

## Phase 0：规格冻结

### Task 0: 规格确认与实现边界冻结

**目标**: 在编码前确认本阶段只做“应用管理收敛 + 双模式 + 下载代码业务接口 + 旧入口下线”，不重写动态运行时。

**涉及文件**:
- `code-copilot/changes/app-mode-code-download-consolidation/spec.md`
- `code-copilot/changes/app-mode-code-download-consolidation/tasks.md`

**执行步骤**:
- [ ] 确认下载代码模式默认业务接口前缀：`/{suiteCode-kebab}/{objectCode-kebab}`。
- [ ] 确认旧 `/ai/crud-config/codegen/download/{configKey}` 的后端兼容策略：短期保留但加权限和废弃提示，前端不再调用。
- [ ] 确认生成 Controller 本次保持 Forge 生成器安全调用风格，详情、更新和删除继续使用 POST。
- [ ] 确认用户可见术语表：业务域、业务单元、访问入口、在线运行、下载代码、功能代码、代码包设置。
- [ ] 将确认结果回填到 `spec.md` 第 11 章。

**验收标准**:
- 待确认项有明确结论。
- 后续任务不再出现“是否要做”的开放项。

---

## Phase 1：后端模型

### Task 1: 应用模式后端字段与 options 映射

**目标**: 应用保存、详情、列表都能读写 `appMode`，历史应用默认在线运行。

**涉及文件**:
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessAppDTO.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessAppVO.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessAppService.java`
- 可选新增：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/constant/BusinessAppMode.java`

**执行步骤**:
- [ ] 新增 `BusinessAppDTO.appMode` 和 `BusinessAppVO.appMode`。
- [ ] 新增常量：
  - `DYNAMIC_RENDER`
  - `CODE_DOWNLOAD`
- [ ] 在 `BusinessAppService` 的 DTO → Entity 保存逻辑中，将 `appMode` 写入 `options.appMode`。
- [ ] 在 Entity → VO 转换逻辑中，从 `options.appMode` 解析 `appMode`。
- [ ] 对 `entryMode != RUNTIME` 的入口保存时移除 `options.appMode`，VO 默认返回 `DYNAMIC_RENDER` 或空值需保持前端可识别。
- [ ] 对 `entryMode = RUNTIME` 且未传 `appMode` 的历史数据默认 `DYNAMIC_RENDER`。
- [ ] 保存 `CODE_DOWNLOAD` 时校验必须有关联 `suiteCode`、`objectCode` 和 `configKey`。

**验收标准**:
- 应用列表、详情接口返回 `appMode`。
- 创建/编辑应用后 `options` 中包含正确 `appMode`。
- 历史 RUNTIME 应用不需要数据迁移即可显示为在线运行。

### Task 2: 字典、菜单和权限迁移脚本

**目标**: 补齐应用模式字典、应用代码权限，并隐藏旧终端用户入口。

**涉及文件**:
- 新增：`forge-server/db/migration/V1.0.70__consolidate_app_codegen_mode.sql`
- 可能同步：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/sql/ai_crud_config.sql`
- 可能同步：`forge-server/forge-admin-server/sql/初始化脚本.sql`

**执行步骤**:
- [ ] 检查 `forge-server/db/migration/` 当前最大版本号，选择下一个版本。
- [ ] 插入字典类型 `ai_business_app_mode`，`tenant_id=1`，带 `NOT EXISTS`。
- [ ] 插入字典项 `DYNAMIC_RENDER`、`CODE_DOWNLOAD`，`tenant_id=1`，带 `NOT EXISTS`。
- [ ] 在应用管理资源下补齐按钮权限：
  - `ai:businessApp:code`
  - `ai:businessApp:codePreview`
  - `ai:businessApp:codeDownload`
- [ ] 将旧菜单资源设置为普通用户不可见或停用：
  - `/ai/crud-config`
  - `/ai/crud-generator`
  - `/ai/lowcode-builder`
  - `/ai/lowcode-models`
- [ ] 兼容历史 `tenant_id=0` 和 `tenant_id=1` 资源条件，但新增内置数据必须用 `tenant_id=1`。
- [ ] 更新初始化 SQL 中用户可见文案，避免新增安装仍暴露旧菜单。

**验收标准**:
- Flyway 脚本可重复执行。
- 新装环境应用管理有代码相关权限。
- 普通菜单中不出现旧配置入口。

---

## Phase 2：代码能力

### Task 3: 应用管理代码预览/下载后端接口

**目标**: 代码能力从低代码应用入口迁移到业务应用入口。

**涉及文件**:
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessAppController.java`
- 新增：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessAppCodegenService.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeCodegenRequest.java`
- 复用：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/lowcode/LowcodeCodePreviewVO.java`

**执行步骤**:
- [ ] 在 `BusinessAppController` 新增：
  - `GET /{id}/code/options`
  - `PUT /{id}/code/options`
  - `GET /{id}/code/preview`
  - `GET /{id}/code/download`
- [ ] 接口增加 `@SaCheckPermission`：
  - options 使用 `ai:businessApp:code`
  - preview 使用 `ai:businessApp:codePreview`
  - download 使用 `ai:businessApp:codeDownload`
- [ ] `BusinessAppCodegenService` 读取应用详情，校验应用存在、租户可见、`entryMode=RUNTIME`、`appMode=CODE_DOWNLOAD`。
- [ ] 根据 `configKey` 读取 `AiCrudConfig`，不存在时抛出业务异常“访问入口尚未发布，不能生成代码”。
- [ ] `code/options` 从 `ai_business_app.options.codegen` 读写，不再写入旧配置入口的 UI 状态。
- [ ] `code/preview` 返回文件 Map、文件数量、应用 ID、configKey、sourceType。
- [ ] `code/download` 输出 zip，文件名使用 `{appCode-kebab}-code.zip` 或 `{configKey}-code.zip`。

**验收标准**:
- 前端可通过 `/ai/business/app/{id}/code/preview` 获取代码预览。
- 前端可通过 `/ai/business/app/{id}/code/download` 下载 zip。
- 非下载代码模式调用代码接口返回明确业务错误。

### Task 4: 业务专属接口路径解析与校验

**目标**: 下载代码模式生成前统一覆盖旧通用接口，保证代码包和预览不含 `/ai/crud/`。

**涉及文件**:
- `BusinessAppCodegenService.java`
- `LowcodeCodegenRequest.java`
- 可能修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/codegen/VelocityCodegenStrategy.java`

**执行步骤**:
- [ ] 在 `LowcodeCodegenRequest` 增加 `businessApiBase` 字段，说明“下载代码模式业务接口前缀”。
- [ ] 实现默认接口前缀：
  - `suiteCode` 和 `objectCode` 转小写 kebab 或小写下划线统一格式。
  - 默认形态为 `/{suite}/{object}`，例如 `/crm/customer`。
- [ ] 校验接口前缀：
  - 必须以 `/` 开头。
  - 不能以 `/ai/crud/`、`/ai/crud-config`、`/ai/lowcode/`、`/rest/` 开头。
  - 不能包含 `{configKey}`。
  - 用户可见配置中不能写 `crud`。
- [ ] 在生成配置副本中覆盖 `apiConfig`，更新和删除必须使用 POST：
  - `list`: `get@{base}/page`
  - `detail`: `post@{base}/getById`
  - `add`: `post@{base}/add`
  - `update`: `post@{base}/edit`
  - `delete`: `post@{base}/remove/:id`
  - 导入导出如本次保留，则同样使用 `{base}/import-template`、`{base}/import`、`{base}/export`。
- [ ] 将 `businessApiBase` 同步写入 `options.codegen.businessApiBase`。
- [ ] 在 `VelocityCodegenStrategy` 中优先读取 `options.codegen.businessApiBase`，再读取 `apiConfig.list`，最后才按 `configKey` 兜底。

**验收标准**:
- 用历史 `/ai/crud/{configKey}` 配置下载代码时，生成结果自动变成业务路径。
- 保存非法接口前缀时后端返回业务错误。
- 代码预览内容中搜索不到 `/ai/crud/`。

### Task 5: 代码生成模板标准化与完整 zip

**目标**: 下载代码模式输出完整且符合 Forge 标准的代码包。

**涉及文件**:
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/templates/vm/controller.java.vm`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/templates/vm/ai-crud/api.js.vm`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/templates/vm/ai-crud/index.vue.vm`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/codegen/VelocityCodegenStrategy.java`
- 新增模板：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/templates/vm/README.md.vm`

**执行步骤**:
- [ ] 将生成 Controller 的详情、新增、修改、删除映射调整为 Forge 代码生成器安全调用风格：
  - `POST /getById`
  - `POST /add`
  - `POST /edit`
  - `POST /remove/{id}`
- [ ] 前端 `api.js.vm` 调整为业务专属接口调用，更新和删除继续使用 POST。
- [ ] 前端 `index.vue.vm` 的 `baseApiConfig` 调整为：
  - `list: 'get@${apiBase}/page'`
  - `detail: 'post@${apiBase}/getById'`
  - `add: 'post@${apiBase}/add'`
  - `update: 'post@${apiBase}/edit'`
  - `delete: 'post@${apiBase}/remove/:${pkColumn.javaField}'`
- [ ] 模板注释和用户可见文案使用“数据管理”“业务页面”“功能代码”，不使用 CRUD。
- [ ] 生成 `README.md`，说明：
  - 后端代码放置路径。
  - 前端代码放置路径。
  - SQL 导入顺序。
  - 接口前缀。
  - 二次开发注意事项。
- [ ] `VelocityCodegenStrategy` 将 README 加入 zip 文件列表。
- [ ] 保持 Entity、Mapper、Mapper XML、Service、DTO、Query、SQL、前端页面和 API 均在 zip 中。

**验收标准**:
- zip 包文件完整。
- 生成 Controller 和前端页面使用业务路径。
- zip 内不出现 `/ai/crud/`。
- 生成的前端 `apiConfig` 使用冒号占位符，不使用 `{id}`。

---

## Phase 3：前端应用管理

### Task 6: 前端 API 迁移到应用管理

**目标**: 前端代码预览、下载和设置全部调用 `/ai/business/app`。

**涉及文件**:
- `forge-admin-ui/src/api/business-app.js`
- `forge-admin-ui/src/api/lowcode-crud.js`

**执行步骤**:
- [ ] 在 `business-app.js` 新增：
  - `businessAppCodePreview(id, params)`
  - `businessAppCodeOptions(id)`
  - `businessSaveAppCodeOptions(id, data)`
  - `businessDownloadAppCode(id, params)`
- [ ] 下载方法沿用当前 fetch + Authorization + nonce 方式，URL 改成 `/ai/business/app/{id}/code/download`。
- [ ] 将 `syncPublishedCrudConfigs` 对外导出名改为 `syncPublishedApps`，旧函数可保留兼容但不在 UI 使用。
- [ ] 将 `dynamicCrudImportTemplate/export/import` 的用户可见调用点改名或隔离，不在应用管理主界面暴露 CRUD 命名。
- [ ] 保留 `lowcode-crud.js` 中低代码内部能力，但应用管理页面不再依赖它下载代码。

**验收标准**:
- 应用管理代码功能不再调用 `/ai/lowcode/app/{id}/code/*`。
- 应用管理代码功能不再调用 `/ai/crud-config/codegen/download/*`。

### Task 7: 应用编辑抽屉新增使用模式

**目标**: 用户在应用配置里明确选择“在线运行”或“下载代码”。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/AppEditorDrawer.vue`
- 可能使用：`forge-admin-ui/src/components/DictSelect.vue`
- 字典：`ai_business_app_mode`

**执行步骤**:
- [ ] 引入 `useDict('ai_business_app_mode')` 或本地模式元信息。
- [ ] 在 `entryMode=RUNTIME` 时展示“使用模式”分段选择或卡片选择：
  - 在线运行：在线搭建并由平台动态运行。
  - 下载代码：下载完整功能代码后导入本地工程二次开发。
- [ ] `defaultForm()` 增加 `appMode: 'DYNAMIC_RENDER'`。
- [ ] `hydrateOptions()` 从 `props.app.appMode` 或 `options.appMode` 读取模式。
- [ ] `buildOptions()` 将 `appMode` 写入 options；非 RUNTIME 时删除。
- [ ] 选择下载代码模式时，必须要求关联业务单元和发布配置。
- [ ] 文案中不出现 CRUD、configKey；“发布配置”改成更友好的“业务页面配置”或“运行配置”。

**验收标准**:
- 新增/编辑 RUNTIME 入口可保存使用模式。
- 历史入口打开抽屉默认在线运行。
- 下载代码模式校验缺失业务单元和配置时给业务化提示。

### Task 8: 应用总览入口操作和代码面板整合

**目标**: 业务单元与访问入口整合展示，重复按钮收敛，下载代码模式提供应用内代码能力。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/index.vue`
- `forge-admin-ui/src/views/app-center/components/BusinessUnitCard.vue`
- 新增：`forge-admin-ui/src/views/app-center/components/AppCodePanel.vue`
- 可复用或参考：现有低代码代码预览组件/模态框，如存在则优先复用。

**执行步骤**:
- [ ] `BusinessUnitCard` 在访问入口行展示模式标签：
  - 在线运行
  - 下载代码
- [ ] `CODE_DOWNLOAD` 入口主按钮改为打开代码面板，不触发 `open-info` 动态页面。
- [ ] 入口更多菜单增加“功能代码”，仅下载代码模式显示；在线运行模式可隐藏或置灰。
- [ ] `index.vue` 增加 `codePanelVisible`、`codingApp` 状态和打开代码面板方法。
- [ ] `AppCodePanel` 提供：
  - 代码包设置表单。
  - 代码预览文件列表。
  - 文件内容预览。
  - 下载按钮。
  - 错误提示和加载态。
- [ ] 代码面板调用 Task 6 的业务应用 API。
- [ ] 右侧重复操作收敛：
  - 对象设计/看板/添加入口保留在卡片底部。
  - 入口配置/功能代码/启停/删除放入更多菜单。
- [ ] 左侧继续只展示业务域，业务域增多时保持搜索、滚动、计数和选中态，不新增应用堆叠列表。

**验收标准**:
- 下载代码模式入口可在应用总览内完成设置、预览、下载。
- 在线运行模式入口仍正常打开业务页面。
- 业务单元和访问入口在同一卡片内展示。
- 右侧重复按钮明显减少，主要动作路径清晰。

---

## Phase 4：旧入口和术语

### Task 9: 旧 `/ai/crud-config` 入口下线

**目标**: 独立旧配置页不再作为用户功能入口。

**涉及文件**:
- `forge-admin-ui/src/views/ai/crud-config.vue`
- `forge-admin-ui/src/api/ai.js`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/AiCrudConfigController.java`
- 迁移脚本见 Task 2

**执行步骤**:
- [ ] 将 `crud-config.vue` 改为迁移提示页，提示“配置能力已迁移到应用管理”，提供跳转 `/app-center`。
- [ ] 页面文案不出现 CRUD。
- [ ] 移除或隐藏该页内原列表、编辑、AI 生成、代码下载按钮。
- [ ] `AiCrudConfigController` 对非 `/render/{configKey}` 的管理接口增加权限或废弃标记，避免前端隐藏但后端裸露。
- [ ] 旧 `downloadCode(configKey)` 增加废弃日志，建议调用 `/ai/business/app/{id}/code/download`。
- [ ] 保留 `/render/{configKey}` 给动态运行页使用。

**验收标准**:
- 普通用户不会从菜单进入旧配置页。
- 直接访问旧配置页不会看到旧管理功能。
- 动态在线运行页不受影响。

### Task 10: 高级配置和终端用户术语清理

**目标**: 普通用户界面不暴露旧低代码技术入口和 CRUD 词。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/BusinessAdvancedConfig.vue`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js`
- `forge-admin-ui/src/router/guards/tab-guard.js`
- `forge-admin-ui/src/router/guards/page-title-guard.js`
- `forge-admin-ui/src/composables/useMenu.js`
- `forge-admin-ui/src/views/app-center/index.vue`
- `forge-admin-ui/src/views/app-center/components/AppFilterBar.vue`
- `forge-admin-ui/src/views/app-center/components/BusinessUnitCard.vue`
- `forge-admin-ui/src/views/app-center/components/AppEditorDrawer.vue`

**执行步骤**:
- [ ] `BusinessAdvancedConfig` 普通模式仅展示业务摘要，不展示技术入口。
- [ ] `BusinessAdvancedConfig` 开发者模式移除“模型资产”“低代码搭建器”“CRUD 配置”按钮。
- [ ] API 配置页签不展示 `/ai/crud/{configKey}`；在线运行模式展示“平台托管接口”，下载代码模式展示业务专属接口。
- [ ] 将用户可见“业务套件”替换为“业务域”。
- [ ] 将用户可见“业务对象”替换为“业务单元”。
- [ ] 将用户可见“应用入口”替换为“访问入口”。
- [ ] 将用户可见 “CRUD页面”“CRUD渲染页”“CRUD配置”替换为“业务页面”“数据管理”“运行配置”。
- [ ] 搜索前端可见文本：
  - `rg -n "CRUD|crud|业务套件|业务对象|应用入口|低代码搭建器|模型资产|configKey|Schema" forge-admin-ui/src`
  - 对命中的终端用户页面逐项清理；内部变量名可暂不强制改。

**验收标准**:
- 应用中心、应用编辑、高级配置、标签页标题、菜单标题不出现 CRUD。
- 普通用户高级配置看不到模型资产、低代码搭建器、旧配置入口。
- 业务名词更偏用户语言。

---

## Phase 5：验证收尾

### Task 11: 自动化测试与构建验证

**目标**: 按项目标准执行增量验证，记录结果。

**涉及文件**:
- `code-copilot/rules/automated-testing-standard.md`
- `code-copilot/changes/app-mode-code-download-consolidation/test-spec.md`
- `code-copilot/changes/app-mode-code-download-consolidation/execution-log.md`

**执行步骤**:
- [ ] 按 AGENTS 要求先读取 `code-copilot/rules/automated-testing-standard.md`。
- [ ] 创建或更新 `test-spec.md`，只覆盖本次差异：
  - 应用模式保存和详情回显。
  - 代码接口权限和业务错误。
  - 业务接口前缀校验。
  - 代码预览/下载不含 `/ai/crud/`。
  - 旧入口迁移提示。
  - 前端术语扫描。
- [ ] 后端优先运行 generator 模块编译或相关测试：
  - `cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -DskipTests=false`
  - 如果耗时或环境缺失，至少运行 `mvn -pl ... -am compile` 并记录跳过原因。
- [ ] 前端命令前执行：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0`
- [ ] 运行前端 lint 或构建：
  - 优先相关文件 eslint。
  - 必要时 `cd forge-admin-ui && pnpm build`。
- [ ] 搜索验证：
  - 生成代码预览内容不含 `/ai/crud/`。
  - 用户可见页面不含 CRUD。
- [ ] 将命令、结果、警告、跳过项和服务清理情况追加到 `execution-log.md`。

**验收标准**:
- 后端编译或测试通过，或明确记录外部依赖阻塞。
- 前端 lint/build 通过，或明确记录既有错误与本次无关。
- 执行日志完整。

### Task 12: 变更文档、执行日志和验收回填

**目标**: 收尾时让 spec/tasks 与真实实现保持一致。

**涉及文件**:
- `code-copilot/changes/app-mode-code-download-consolidation/spec.md`
- `code-copilot/changes/app-mode-code-download-consolidation/tasks.md`
- `code-copilot/changes/app-mode-code-download-consolidation/execution-log.md`
- 可能新增：`code-copilot/changes/app-mode-code-download-consolidation/implementation-summary.md`

**执行步骤**:
- [ ] 每完成一个任务，更新任务状态。
- [ ] 如果实现中调整接口、字段或默认路径，回填 `spec.md`。
- [ ] 记录最终修改文件列表和验证结果。
- [ ] 如发现有价值的踩坑或决策，按 AGENTS 写入 `.opencode/memory/pitfalls.md` 或 `.opencode/memory/decisions.md`。

**验收标准**:
- tasks 状态和实际代码一致。
- spec 无过期接口。
- execution-log 有完整验证链路。

---

## 执行摘要

- 已完成应用模式后端契约：`BusinessAppDTO/VO.appMode`、`BusinessAppMode`、`BusinessAppService` options 映射和下载代码模式校验。
- 已完成应用管理代码接口：`/{id}/code/options`、`/{id}/code/preview`、`/{id}/code/download`，并新增 `BusinessAppCodegenService` 统一解析业务接口前缀、覆盖旧运行接口、校验生成文件不含 `/ai/crud/`。
- 已完成代码生成模板标准化：Controller 使用 `businessApiBase` 和 POST 更新/删除风格，前端 API 与页面使用业务路径，zip 增加 `README.md`。
- 已完成前端应用管理迁移：应用编辑新增“在线运行 / 下载代码”，应用总览和业务域详情页接入 `AppCodePanel`，下载代码模式入口不再打开动态运行页。
- 已完成旧入口下线：旧 `/ai/crud-config` 页面改为迁移提示，旧管理接口加权限，迁移脚本隐藏旧菜单并补齐代码权限。
- 已完成术语清理：应用管理主链路用户可见文案统一为业务域、业务单元、访问入口、功能代码、代码包设置；高级配置隐藏模型资产、低代码搭建器、旧配置入口。
- 已完成验证：后端 generator 模块编译通过、前端改动文件 lint 通过、前端生产构建通过、`git diff --check` 通过，执行证据见 `execution-log.md`。
