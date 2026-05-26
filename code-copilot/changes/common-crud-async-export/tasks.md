# 任务清单：common-crud-async-export
> status: apply
> created: 2026-05-26
> 拆分顺序：数据模型 → 接口协议 → 底层分页能力 → 异步编排 → 前端入口 → 验证

## 前置条件
- [x] 已确认 `AiCrudPage` 存在通用导出入口。
- [x] 已确认动态 CRUD 导出入口为 `/ai/crud/{configKey}/export`。
- [x] 已确认系统参数表 `sys_config` 已存在。
- [x] 已确认文件中心已有 `/api/file/download/{fileId}` 下载链路。

## 任务总览

| Task | 名称 | 状态 | 优先级 |
|------|------|------|--------|
| Task 1 | 数据库迁移与任务模型 | completed | P0 |
| Task 2 | 动态查询分页与数据权限上下文 | completed | P0 |
| Task 3 | 智能导出与异步 worker | completed | P0 |
| Task 4 | 文件中心流式上传能力 | completed | P0 |
| Task 5 | 前端 AiCrudPage 异步任务 UX | completed | P0 |
| Task 6 | 验证与文档回填 | completed | P1 |

---

## Task 1: 数据库迁移与任务模型

**目标**: 创建异步导出任务持久化表和系统参数初始化。

**涉及文件**:
- `forge/db/migration/V1.0.23__add_common_crud_async_export.sql` — 新增任务表和 `sys_config` 默认参数。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiCrudExportTask.java` — 新增任务实体。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/AiCrudExportTaskMapper.java` — 新增 Mapper。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudExportTaskMapper.xml` — 新增任务分页和详情查询 SQL。

**关键签名**:
```java
Page<AiCrudExportTask> selectTaskPage(Page<AiCrudExportTask> page,
                                      Long tenantId,
                                      Long createBy,
                                      String configKey);
AiCrudExportTask selectTaskById(Long tenantId, Long createBy, Long id);
```

**验收标准**:
- 任务表包含标准审计字段和必要索引。
- `sys_config` 初始化使用 `tenant_id=1`，并做 `NOT EXISTS` 防重复。

**完成状态**: completed

---

## Task 2: 动态查询分页与数据权限上下文

**目标**: 为异步导出提供总数统计、无重复 count 的分页数据读取，以及提交人数据权限上下文复用。

**涉及文件**:
- `DynamicCrudRepository.java` — 新增导出 count、分页 records 查询方法。
- `DynamicCrudService.java` — 新增 `countExportRows`、`selectExportPageRows`。
- `DynamicDataScopeService.java` — 新增显式 `DataScopeContext` 入参重载。

**关键签名**:
```java
public long countExportRows(String configKey, DynamicCrudQuery query, DataScopeContext dataScopeContext);
public List<Map<String, Object>> selectExportPageRows(String configKey,
                                                      DynamicCrudQuery query,
                                                      Integer pageNum,
                                                      Integer pageSize,
                                                      DataScopeContext dataScopeContext);
```

**验收标准**:
- 异步导出分页读取不重复执行 count。
- FOLLOW_SYSTEM 数据权限在异步线程中使用提交时捕获的上下文。

**完成状态**: completed

---

## Task 3: 智能导出与异步 worker

**目标**: 将 `/ai/crud/{configKey}/export` 改为同步/异步自动决策，并实现后台导出。

**涉及文件**:
- `DynamicCrudController.java` — 导出接口返回同步文件或异步任务 JSON，新增任务查询接口。
- `DynamicCrudExcelService.java` — 读取系统参数、提交任务、同步导出和任务查询。
- `DynamicCrudAsyncExportWorker.java` — 新增异步 worker，分页写 Excel、更新进度、上传文件服务。
- `DynamicCrudExportResult.java` — 新增导出提交响应。

**关键签名**:
```java
public DynamicCrudExportResult exportExcel(String configKey, DynamicCrudQuery query, HttpServletResponse response);
@Async
public void executeAsync(Long taskId, String configKey, DynamicCrudQuery query, ExportExecutionContext context);
```

**验收标准**:
- 小数据直接下载 Excel。
- 大数据返回 `async=true` 和 `taskId`。
- 失败任务记录错误信息，成功任务记录 `fileId/fileName/fileSize`。
- 临时文件在 finally 中删除。

**完成状态**: completed

---

## Task 4: 文件中心流式上传能力

**目标**: 支持后台生成文件通过流上传到默认文件存储。

**涉及文件**:
- `FileManager.java` — 新增 `InputStream` 上传重载。
- `forge-plugin-generator/pom.xml` — 增加 `forge-starter-file` 依赖。

**关键签名**:
```java
public FileMetadata upload(InputStream inputStream,
                           String fileName,
                           String contentType,
                           String businessType,
                           String businessId,
                           String storageType,
                           Boolean isPrivate);
```

**验收标准**:
- 上传结果持久化到文件元数据表。
- 导出任务拿到可由前端下载的 `fileId`。
- 已知文件大小的异步导出上传路径传入 `fileSize`，RustFS/COS 不为导出文件整文件 `readAllBytes()`。

**完成状态**: completed

---

## Task 5: 前端 AiCrudPage 异步任务 UX

**目标**: 在通用 CRUD 页面提供异步导出反馈、进度查询和下载入口。

**涉及文件**:
- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` — 识别异步导出 JSON、任务抽屉、轮询、下载按钮。
- `forge-admin-ui/src/components/ai-form/AiCrudPageProps.js` — 新增可选异步导出开关/配置键解析属性。

**关键签名**:
```js
async function handleExport()
async function loadExportTasks()
async function pollExportTask(taskId)
async function handleDownloadExportTask(row)
```

**验收标准**:
- 同步导出保持原行为。
- 异步导出返回后自动打开任务抽屉并轮询当前任务。
- 用户可随时点击“导出任务”查看历史任务和下载成功文件。
- `responseType=blob` 下的异步 JSON 响应可被识别，不会误下载为 Excel。

**完成状态**: completed

---

## Task 6: 验证与文档回填

**目标**: 完成针对性编译、前端校验并回填执行日志。

**验证命令**:
```bash
mvn -pl forge-admin-server -am compile -DskipTests
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**验收标准**:
- 后端编译通过：`forge-plugin-generator` 依赖链与 `forge-admin-server` 依赖链均已通过。
- 前端校验通过：`AiCrudPage.vue` / `AiCrudPageProps.js` ESLint 通过。
- 前端构建通过：使用 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` 构建成功。
- 构建仍输出仓库已有 UnoCSS icon 加载警告、CSS `//` 注释警告和 chunk size 警告，不阻断本次变更。

**完成状态**: completed
