# 新增页面操作审计日志并优化页面视觉样式
> status: applied
> created: 2026-07-07
> complexity: 🟡中等

## 1. 背景与目标
当前操作日志只记录基础接口请求信息，缺少页面维度、操作人真实姓名、操作内容摘要、前后数据快照/差异展示和导出配置。需要在复用现有 `sys_operation_log` 与 `@OperationLog` 的基础上增强页面审计能力，并把 `/system/operation-log` 页面改造成更适合审计筛选和详情核查的视觉体验。

完成后可验证结果：
- 页面请求自动带上当前页面路径和页面标题，后台对变更类操作写入操作日志。
- 操作日志记录账号、操作人、时间、页面、类型、内容、IP、请求/响应和差异快照字段。
- `/system/operation-log` 支持按账号、操作人、页面、类型、状态、IP、URL、内容、时间筛选，支持导出。
- 详情弹窗以审计视角展示用户、页面、请求、数据差异和环境信息，页面样式更清晰紧凑。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路
- `forge-admin-ui/src/utils/http/interceptors.js:reqResolve` 是前端统一请求拦截器，当前已写入 `traceId`、`Authorization`、防重放头，但未写入当前页面路径/标题。
- `forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/aspect/OperationLogAspect.java:around` 拦截 Controller 请求，构造 `OperationLogInfo` 并异步保存。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SystemLogServiceImpl.java:saveOperationLog` 将 `OperationLogInfo` 转为 `SysOperationLog` 入库。
- `forge-admin-ui/src/views/system/operation-log.vue` 是现有操作日志页面，使用 `AiCrudPage` 和自定义详情弹窗。

### 2.2 现有实现
- `sys_operation_log` 已有字段：`tenant_id`, `user_id`, `username`, `operation_module`, `operation_type`, `operation_desc`, `request_method`, `request_url`, `request_params`, `response_result`, `error_msg`, `operation_status`, `operation_ip`, `operation_location`, `user_agent`, `execute_time`, `operation_time`。
- `SysOperationLogController.page` 当前直接使用 `LambdaQueryWrapper` 写查询条件，不符合 `AGENTS.md` 第 5.1 条“查询 SQL 必须写 XML”。
- 通用 Excel 导出由 `GenericExportController` + `DynamicExportEngine` 提供，导出配置来自 `sys_excel_export_config` 和 `sys_excel_column_config`。
- `AiCrudPage` 已支持 `showExport` 和 `apiConfig.export`，可复用 `POST /api/excel/export/{configKey}`。

### 2.3 发现与风险
- 通用切面无法自动知道所有业务表的数据库变更前快照。必须提供审计快照字段，并通过上下文扩展支持业务代码后续主动设置；默认从请求参数/响应结果生成可追溯快照，避免阻塞当前全局页面审计上线。
- 部分接口未显式 `@OperationLog`，当前切面会根据 API 配置兜底。页面维度应在切面统一补齐，不要求逐个 Controller 改造。
- `sys_operation_type` 历史字典可能存在 `INSERT` 而非 `ADD`，本次迁移补齐 `ADD`，避免新增操作标签不显示。

## 3. 功能点
- [x] **SDD 文档**：建立本变更 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- [x] **日志字段增强**：为 `sys_operation_log` 增加页面、操作人、操作内容、前后快照和差异字段。
- [x] **前端页面头注入**：统一请求头写入 `X-Page-Path`、`X-Page-Title`。
- [x] **后端切面采集**：`OperationLogAspect` 读取页面头，填充审计字段，并提供默认操作内容/快照兜底。
- [x] **查询与导出**：新增 `SysOperationLogService`、Query DTO 和 Mapper XML 查询，支持导出列表。
- [x] **页面视觉优化**：重做 `/system/operation-log` 列表字段、筛选项、详情弹窗和导出入口。
- [x] **操作类型归一**：统一历史 `INSERT` 与现行 `ADD` 新增类型，避免页面“操作类型”下拉展示两个“新增”。

## 4. 业务规则
- 操作账号使用 `username`，操作人使用 `operator_name`，从 `sys_user.real_name` 补齐；为空时回退用户名。
- 操作页面同时记录页面路径 `operation_page` 和页面标题 `operation_page_title`。
- 操作内容优先使用 `@OperationLog.desc`，无注解时使用 API 配置名称；再拼接请求方法和 URL 形成可读摘要。
- 操作日志默认只记录变更类操作：`ADD`、`UPDATE`、`DELETE`、`IMPORT`、`EXPORT`、`OTHER`，以及未显式标注但 HTTP 方法为 `POST`、`PUT`、`PATCH`、`DELETE` 的接口。
- 查询类操作默认不落库：显式 `OperationType.QUERY`、API 配置识别为 `QUERY`、未显式标注且 HTTP 方法为 `GET`、`HEAD`、`OPTIONS` 的接口直接放行；`/page`、`/list`、`/tree`、`/detail`、`/getById`、`/options`、`/profile`、`/query` 等 POST 查询兼容接口也按查询放行。
- `before_data`、`after_data`、`diff_data` 支持审计上下文主动写入；未主动写入时：
  - 新增/修改/删除/导入/导出类操作以请求参数作为提交快照，以响应结果作为结果快照。
- 加密请求体被切面替换为 `[DECRYPTED_REQUEST_BODY_OMITTED]` 时，不允许把该占位符写入 `after_data`；需要精确快照的业务接口通过 `OperationAuditContext` 主动写入脱敏后的前后数据。
- 用户管理主操作（新增、修改、删除、批量删除、状态、解锁、重置密码、个人资料）主动写入脱敏快照和字段 diff；密码值不进入请求参数和快照。
- 操作类型以 `OperationType.ADD` 的 `ADD` 作为新增操作标准值；历史 `INSERT` 日志和字典值迁移归一到 `ADD`。页面展示时将 `INSERT` 兼容映射为 `ADD`，筛选 `ADD` 时兼容查询迁移前的 `INSERT` 数据。
- 操作日志表属于审计日志，普通行级删除不在本需求新增；历史留存清理可由专用任务按策略物理清理。
- 页面视觉遵循后台审计工具风格：克制、密集、可扫描，不做营销式 Hero 或大面积装饰。

## 5. 数据变更
| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 修改 | `sys_operation_log` | `operator_name` | 操作人真实姓名 |
| 修改 | `sys_operation_log` | `operation_page`, `operation_page_title` | 页面路径和页面标题 |
| 修改 | `sys_operation_log` | `operation_content` | 操作内容摘要 |
| 修改 | `sys_operation_log` | `before_data`, `after_data`, `diff_data` | 前后快照与差异 |
| 修改 | `sys_operation_log` | `idx_operation_page_time`, `idx_operator_name` | 查询优化 |
| 新增/补齐 | `sys_dict_data` | `sys_operation_type=ADD` | 兼容 `OperationType.ADD` |
| 修改 | `sys_operation_log`, `sys_dict_data` | `INSERT` → `ADD` | 统一新增操作类型，避免 `INSERT/ADD` 同时显示为两个“新增” |
| 新增 | `sys_excel_export_config`, `sys_excel_column_config` | `sys_operation_log_export` | 操作日志导出配置 |
| 修改 | `sys_resource` | 操作日志菜单名称/权限 | 菜单展示为页面操作审计日志并补导出权限 |

## 6. 接口变更
| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 修改 | `/system/operationLog/page` | GET | 增加页面、操作人、操作内容筛选，后端改为 XML 查询 |
| 修改 | `/system/operationLog/{id}` | GET | 返回新增审计字段 |
| 复用 | `/api/excel/export/sys_operation_log_export` | POST | 导出筛选后的操作日志 |

## 7. 影响范围
- 后端：`forge-starter-log`、`forge-plugin-system`、Flyway 迁移脚本。
- 前端：统一请求拦截器、`/system/operation-log` 页面。
- 数据：`sys_operation_log` 增加字段；历史数据字段为空，不影响既有查询。

## 8. 风险与关注点
- ⚠️ 审计日志可能包含请求参数和响应结果，切面已有 `@ApiDecrypt` 请求体省略逻辑；本次继续沿用截断和脱敏边界，避免扩大敏感数据暴露面。
- ⚠️ 通用切面无法对所有业务表自动生成数据库级前后差异；本次通过字段和上下文扩展提供能力，默认记录请求/响应快照，后续高风险业务可主动写入精确差异。
- ⚠️ 导出数据需受现有登录和权限体系保护，复用通用 Excel 导出接口和菜单权限配置。

## 8.5 测试策略
- **测试范围**：SQL 迁移静态检查、后端模块编译、前端构建、页面字段/导出配置静态校验。
- **覆盖率目标**：本轮以编译和关键链路静态验证为主；若本地数据库和服务可用，再做登录后页面/导出联调。
- **独立 Test Spec**：是。

## 9. 待澄清
- [x] 用户已明确“按照 SDD 开发，我睡觉了”，本轮不阻塞等待确认。

## 10. 技术决策
- 复用现有 `sys_operation_log`，不新建 `page_audit_log` 表，避免日志链路分叉。
- 前端统一请求头注入页面元信息，不逐页手工传参。
- 查询 SQL 迁移到 `SysOperationLogMapper.xml`，修正当前 Controller 内查询问题。
- 操作日志导出复用平台动态 Excel 能力。
- 切面在解析注解/API 配置后先判断是否为查询类操作，查询直接执行业务方法并清理审计上下文，不创建 `traceId`，不异步保存日志。

## 11. 执行日志
| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| SDD 文档 | 已完成 | `spec.md`, `tasks.md`, `test-spec.md`, `execution-log.md` | 本轮创建 |
| Task 1 | 已完成 | `V1.0.14__enhance_operation_log_page_audit.sql` | 新增审计字段、索引、字典、导出配置和资源权限 |
| Task 2 | 已完成 | `OperationAuditContext.java`, `OperationLogInfo.java`, `OperationLogAspect.java` | 统一采集页面头、操作内容和审计快照 |
| Task 3 | 已完成 | `SysOperationLogQuery.java`, `ISysOperationLogService.java`, `SysOperationLogServiceImpl.java`, `SysOperationLogMapper.java`, `SysOperationLogMapper.xml`, `SysOperationLogController.java` | 查询迁移到 XML SQL 并支持导出 |
| Task 4 | 已完成 | `interceptors.js` | 所有请求自动携带页面路径和页面标题 |
| Task 5 | 已完成 | `operation-log.vue` | 审计列表、筛选、导出和详情弹窗视觉优化 |
| Task 6 | 已完成 | `test-spec.md`, `execution-log.md` | 后端编译、前端构建和静态校验通过 |
| 增量修正 | 已完成 | `OperationLogAspect.java`, `spec.md`, `tasks.md`, `test-spec.md`, `execution-log.md` | 查询类操作不再落库，只记录变更类操作 |
| 增量修正 | 已完成 | `interceptors.js`, `OperationLogAspect.java`, `SysUserController.java`, `spec.md`, `tasks.md`, `test-spec.md`, `execution-log.md` | 修正页面标题/模块兜底和用户管理快照，避免加密体占位符进入数据快照 |
| 增量修正 | 已完成 | `operation-log.vue`, `SysOperationLogMapper.xml`, `V1.0.15__normalize_operation_type_add_dict.sql`, `spec.md`, `tasks.md`, `test-spec.md`, `execution-log.md` | 修复操作类型两个“新增”，统一 `INSERT` 与 `ADD` |

## 12. 审查结论
已完成实现与验证。`git diff --check` 通过；Flyway 占位符扫描无 `${...}`；`SysOperationLogMapper.xml` 通过 `xmllint --noout`；`forge-admin-server` 依赖编译通过；`pnpm --dir forge-admin-ui build` 通过。前端构建中存在既有动态导入、CSS `//` 注释和 chunk size 类 warning，未阻断本次变更。未启动后端和真实数据库做页面联调，原因是本轮以代码实现、迁移脚本和构建验证为主，未引入本地服务启动。

2026-07-08 增量修正：按用户确认调整为只记录变更类操作。`OperationType.QUERY` 和只读 HTTP 方法默认不落库；`ADD/UPDATE/DELETE/IMPORT/EXPORT/OTHER` 继续记录。`git diff --check` 和 `mvn -q -pl forge-admin-server -am compile -DskipTests` 已通过。

2026-07-08 Flyway 修复：`sys_dict_data` 补齐 `ADD` 字典的派生表漏写 `dict_label` 别名，导致 `Unknown column 'seed.dict_label' in 'field list'`。已补为 `'新增' dict_label`，并通过 `git diff --check` 与静态别名检查。

2026-07-08 页面审计展示修复：请求头页面标题改为优先从菜单/页签解析，避免只写入基础系统名；后端模块为空时使用有效页面标题兜底；加密请求体占位符不再作为 `after_data`；用户管理主操作补充脱敏前后快照与 diff。`git diff --check`、`mvn -q -pl forge-admin-server -am compile -DskipTests`、`pnpm --dir forge-admin-ui build` 已通过。

2026-07-08 操作类型去重修复：旧基线字典存在 `INSERT=新增`，本变更补齐 `ADD=新增` 后页面会出现两个“新增”。已在前端将 `INSERT` 归一展示为 `ADD`，在 Mapper XML 中让 `operationType=ADD` 兼容历史 `INSERT`，并新增 `V1.0.15__normalize_operation_type_add_dict.sql` 迁移历史日志和字典。迁移脚本避开 `sys_dict_data` 的 `(tenant_id, dict_type, dict_value)` 唯一键冲突：有 `ADD` 时禁用 `INSERT`，无 `ADD` 时才将 `INSERT` 改为 `ADD`。`git diff --check`、Flyway 占位符扫描、`xmllint --noout`、`mvn -q -pl forge-admin-server -am compile -DskipTests`、`pnpm --dir forge-admin-ui build` 已通过；未启动 MySQL 实跑 Flyway。

## 13. 确认记录（HARD-GATE）
- **确认时间**：2026-07-07
- **确认人**：用户本轮明确授权“按照 SDD 开发，我睡觉了”
