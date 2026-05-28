# 通用 CRUD 异步导出
> status: apply
> created: 2026-05-26
> complexity: 🔴复杂

## 1. 背景与目标
现有 `AiCrudPage` 已提供通用导出按钮，动态 CRUD 运行时也提供 `/ai/crud/{configKey}/export`，但当前链路是一次性查询全部导出数据并直接写响应。当数据量较大时，请求容易超时，后端也存在一次性构造数据集合导致 OOM 的风险。

本次目标是在通用 CRUD 页面增加智能导出能力：后端先按当前查询条件统计数据量，小数据仍同步下载；超过系统参数阈值时自动转为异步导出任务。异步任务后台分页查询、流式写 Excel、上传到文件服务，前端提供任务进度查询和文件下载入口。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路
- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue#handleExport`：当前导出按钮始终按 blob 响应下载文件，没有识别异步任务 JSON。
- `forge-admin-ui/src/views/ai/crud-page.vue#crudProps`：动态低代码 CRUD 会把 `cfg.apiConfig.export` 透传给 `AiCrudPage`。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/DynamicCrudController.java#exportExcel`：动态 CRUD 导出入口为 `POST /ai/crud/{configKey}/export`。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudExcelService.java#exportExcel`：当前读取最多 `MAX_EXPORT_ROWS` 行后一次性构造 `List<List<Object>>` 写 Excel。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java#selectExportRows`：动态导出复用字段白名单、解密、字典翻译和脱敏链路。
- `forge/forge-framework/forge-starter-parent/forge-starter-file/src/main/java/com/mdframe/forge/starter/file/core/FileManager.java`：文件中心已有上传、下载、元数据持久化能力，但当前 `FileManager` 只暴露 `MultipartFile` 上传入口。
- `forge/forge-framework/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysConfig.java`：系统参数表 `sys_config` 已存在，可保存导出阈值和批量大小。

### 2.2 现有实现
- 动态 CRUD 导出已支持按 `columnsSchema` 生成表头，并可读取 `sys_excel_column_config` 覆盖列顺序、表头和字典类型。
- 动态查询层使用 `NamedParameterJdbcTemplate` + 表名/字段白名单拼接 SQL，不适合普通 Mapper XML，但新增任务分页查询应在 Mapper XML 中实现。
- Starter Excel 里已有 `AsyncExportServiceImpl`，但该实现使用 `ConcurrentHashMap` 保存任务、`ByteArrayOutputStream` 缓存完整文件、写本地临时文件后再下载，不满足本次“持久任务、文件服务、避免 OOM”的要求。

### 2.3 发现与风险
- 异步线程不能依赖当前 HTTP 会话上下文，需要在提交任务时捕获租户、用户和数据权限上下文，并在后台执行时恢复租户上下文。
- 动态 CRUD 若启用 FOLLOW_SYSTEM 数据权限，异步导出必须沿用提交人当时的数据权限，不能因为后台线程丢失登录态而扩大数据范围。
- 导出文件必须先上传文件服务，任务表只保存 `file_id` 等元数据，前端使用现有 `/api/file/download/{fileId}` 链路下载。
- 批量写 Excel 必须使用分页查询和 EasyExcel writer 分批写入，禁止把完整文件或完整数据集放入内存。

## 3. 功能点
- [x] 新增系统参数：异步阈值、导出批量大小、任务文件保留时间。
- [x] 动态 CRUD 导出支持智能判断：总数 `<= threshold` 同步下载，`> threshold` 返回异步任务信息。
- [x] 新增导出任务表，持久化任务状态、总数、已导出数、进度、文件 ID、错误信息、过期时间。
- [x] 异步导出后台分页查询并分批写入 Excel，完成后上传到文件服务。
- [x] 前端 `AiCrudPage` 支持识别异步导出响应，打开导出任务抽屉，轮询进度并提供下载按钮。
- [x] 前端工具栏提供“导出任务”入口，用户可查看当前 CRUD 配置下的历史任务。

## 4. 业务规则
- 默认异步阈值为 `5000` 条，配置键：`sys.export.async.threshold`。
- 默认导出批量大小为 `1000` 条，配置键：`sys.export.batch.size`。
- 默认导出文件保留 `24` 小时，配置键：`sys.export.file.keepHours`。
- 同步导出仍直接返回 Excel 文件，不额外创建任务。
- 异步任务仅展示当前登录用户提交的任务；管理员也不在通用入口直接看其他用户任务。
- 任务状态：`PENDING`、`RUNNING`、`SUCCESS`、`FAILED`。
- 导出条件与当前搜索条件一致，继续走字段白名单、租户过滤、数据权限、解密、字典翻译和脱敏处理。

## 5. 数据变更
| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增 | `ai_crud_export_task` | `id`, `tenant_id`, `config_key`, `file_id`, `status`, `total_count`, `exported_count`, `progress`, `query_params`, 标准审计字段 | 持久化动态 CRUD 异步导出任务 |
| 新增 | `sys_config` | `sys.export.async.threshold` | 超过该行数自动异步导出 |
| 新增 | `sys_config` | `sys.export.batch.size` | 异步导出每批查询和写入行数 |
| 新增 | `sys_config` | `sys.export.file.keepHours` | 导出文件过期时间 |

## 6. 接口变更
| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 修改 | `/ai/crud/{configKey}/export` | POST | 自动判断同步/异步；同步写文件流，异步返回任务信息 |
| 新增 | `/ai/crud/{configKey}/export/tasks` | GET | 查询当前用户在该配置下的导出任务 |
| 新增 | `/ai/crud/{configKey}/export/tasks/{taskId}` | GET | 查询单个导出任务进度和文件信息 |

## 7. 影响范围
- `forge-plugin-generator`：动态 CRUD 导出服务、导出任务实体/Mapper、动态查询分页能力、数据权限上下文复用。
- `forge-starter-file`：补充从 `InputStream` 上传到文件服务的管理方法。
- `forge-admin-ui`：`AiCrudPage` 导出按钮、任务抽屉、进度轮询和文件下载。
- `forge/db/migration`：新增任务表和系统参数初始化。

## 8. 风险与关注点
- 不涉及资金和业务状态流转。
- 涉及数据导出权限边界，必须确保异步任务不绕过租户、数据权限和当前用户任务隔离。
- 大文件上传失败时任务必须进入 `FAILED`，临时文件必须在 finally 中删除。
- Excel writer 和文件流必须显式关闭，避免句柄泄漏。

## 8.5 测试策略
- **测试范围**：动态 CRUD 导出同步路径、异步提交、任务查询、进度更新、文件下载入口。
- **覆盖率目标**：本次以编译、前端构建和关键链路手工验证为主。
- **独立 Test Spec**：否。

## 9. 待澄清
- [x] 无阻塞问题；按动态 CRUD 通用导出优先实现，固定 `sys_excel_export_config` 反射导出后续可独立升级。

## 10. 技术决策
- 异步导出聚焦 `/ai/crud/{configKey}/export`，这是低代码/通用 CRUD 当前主链路。
- 任务表命名为 `ai_crud_export_task`，归属生成器/低代码运行时，避免把系统插件反向依赖生成器。
- 文件下载不新增专用下载接口，前端使用现有文件中心 `downloadFile(fileId, fileName)`。

## 11. 执行日志
| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 1 | completed | `forge/db/migration/V1.0.23__add_common_crud_async_export.sql`, `AiCrudExportTask.java`, `AiCrudExportTaskMapper.java`, `AiCrudExportTaskMapper.xml` | 新增异步导出任务表、系统参数和任务查询 SQL |
| Task 2 | completed | `DynamicCrudRepository.java`, `DynamicCrudService.java`, `DynamicDataScopeService.java` | 新增导出 count、分页读取和提交人数据权限上下文复用 |
| Task 3 | completed | `DynamicCrudController.java`, `DynamicCrudExcelService.java`, `DynamicCrudAsyncExportWorker.java`, `DynamicCrudExportResult.java` | 导出接口自动同步/异步决策，后台分批写 Excel 并更新任务进度 |
| Task 4 | completed | `FileManager.java`, `FileStorage.java`, `RustfsFileStorage.java`, `TencentCosFileStorage.java`, `forge-plugin-generator/pom.xml` | 文件中心支持已知大小的流式上传，异步导出上传到文件服务 |
| Task 5 | completed | `AiCrudPage.vue`, `AiCrudPageProps.js` | 通用 CRUD 增加导出任务入口、抽屉、轮询、下载和 Blob JSON 识别 |
| Task 6 | completed | `spec.md`, `tasks.md` | 已完成后端编译、前端 ESLint 和生产构建验证 |

## 12. 审查结论
已按 Spec 完成实现。同步导出保持文件流下载；超过 `sys.export.async.threshold` 时创建持久任务并返回 `async=true/taskId`；异步 worker 按 `sys.export.batch.size` 分批查询和写入，完成后上传文件中心并记录 `fileId/fileSize`。任务查询按当前租户、当前用户和 `configKey` 隔离。

验证记录：
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` 通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -pl forge-admin-server -am compile -DskipTests` 通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/components/ai-form/AiCrudPage.vue src/components/ai-form/AiCrudPageProps.js` 通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` 通过；仍有仓库既有 UnoCSS icon 加载警告、CSS `//` 注释警告和 chunk size 警告。

## 13. 确认记录（HARD-GATE）
- **确认时间**：2026-05-26
- **确认人**：用户已直接要求实现通用 CRUD 异步导出能力。
