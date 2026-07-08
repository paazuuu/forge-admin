# 任务拆分 — 新增页面操作审计日志并优化页面视觉样式
> 拆分顺序：数据模型 → 接口协议 → 底层实现 → 上层编排 → 入口层
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 每个任务必须精确到文件路径和函数签名

## 前置条件
- [x] 已读取根目录 `AGENTS.md`、`code-copilot/AGENTS.md` 和三类记忆文件。
- [x] 已读取项目级 `.agents/skills/forge-codegen-crud/SKILL.md`、`.agents/skills/ui-ux-pro-max/SKILL.md` 及 CRUD 引用文件。
- [x] 已确认后端真实目录为 `forge-server/`，迁移目录为 `forge-server/db/migration/`。

## Task 1: SDD 基线与迁移脚本
> 状态：completed。

- **目标**: 建立变更文档并补齐操作日志表字段、导出配置和资源权限。
- **涉及文件**:
  - `code-copilot/changes/page-operation-audit-visual-optimization/spec.md` — 新增需求规格。
  - `code-copilot/changes/page-operation-audit-visual-optimization/tasks.md` — 新增任务拆分。
  - `code-copilot/changes/page-operation-audit-visual-optimization/test-spec.md` — 新增测试规格。
  - `code-copilot/changes/page-operation-audit-visual-optimization/execution-log.md` — 新增执行记录。
  - `forge-server/db/migration/V1.0.14__enhance_operation_log_page_audit.sql` — 新增字段、索引、字典、Excel 导出配置、资源补丁。
- **关键签名/SQL**:
  ```sql
  ALTER TABLE sys_operation_log ADD COLUMN operator_name varchar(100) DEFAULT NULL COMMENT '操作人姓名';
  ALTER TABLE sys_operation_log ADD COLUMN operation_page varchar(500) DEFAULT NULL COMMENT '操作页面路径';
  ALTER TABLE sys_operation_log ADD COLUMN operation_page_title varchar(200) DEFAULT NULL COMMENT '操作页面标题';
  ALTER TABLE sys_operation_log ADD COLUMN operation_content varchar(1000) DEFAULT NULL COMMENT '操作内容';
  ALTER TABLE sys_operation_log ADD COLUMN before_data mediumtext COMMENT '操作前数据';
  ALTER TABLE sys_operation_log ADD COLUMN after_data mediumtext COMMENT '操作后数据';
  ALTER TABLE sys_operation_log ADD COLUMN diff_data mediumtext COMMENT '操作数据差异';
  ```

## Task 2: 后端审计上下文与切面增强
> 状态：completed。

- **目标**: 统一采集页面元信息、操作内容和数据快照字段。
- **涉及文件**:
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/context/OperationAuditContext.java` — 新增线程上下文。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/domain/OperationLogInfo.java` — 增加审计字段。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/aspect/OperationLogAspect.java` — 读取页面请求头并填充审计字段。
- **关键签名**:
  ```java
  public final class OperationAuditContext {
      public static void setBeforeData(Object beforeData) { }
      public static void setAfterData(Object afterData) { }
      public static void setDiffData(Object diffData) { }
      public static Snapshot snapshot() { }
      public static void clear() { }
  }
  ```

## Task 3: 操作日志查询服务与 XML SQL
> 状态：completed。

- **目标**: 将日志查询迁移到 Service + Mapper XML，并支持导出查询。
- **涉及文件**:
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/dto/SysOperationLogQuery.java` — 新增查询 DTO。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/ISysOperationLogService.java` — 新增服务接口。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysOperationLogServiceImpl.java` — 新增服务实现与导出方法。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/mapper/SysOperationLogMapper.java` — 增加 XML 方法。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysOperationLogMapper.xml` — 新增显式字段 SQL。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/SysOperationLogController.java` — 改用 Service。
- **关键签名**:
  ```java
  Page<SysOperationLog> page(PageQuery pageQuery, SysOperationLogQuery query);
  SysOperationLog detail(Long id);
  List<SysOperationLog> selectExportList(SysOperationLogQuery query);
  ```

## Task 4: 前端页面元信息注入
> 状态：completed。

- **目标**: 所有请求自动带当前页面路径和标题，供后端审计。
- **涉及文件**:
  - `forge-admin-ui/src/utils/http/interceptors.js` — 在请求头写入 `X-Page-Path`、`X-Page-Title`。
- **关键签名**:
  ```javascript
  function resolvePageAuditHeaders() {
    return {
      'X-Page-Path': currentPath,
      'X-Page-Title': encodeURIComponent(pageTitle),
    }
  }
  ```

## Task 5: 操作日志页面视觉与交互优化
> 状态：completed。

- **目标**: 页面支持新增筛选和导出，详情弹窗按审计信息分区展示。
- **涉及文件**:
  - `forge-admin-ui/src/views/system/operation-log.vue` — 优化列表、搜索、导出、详情弹窗和样式。
- **关键配置**:
  ```javascript
  apiConfig: {
    list: 'get@/system/operationLog/page',
    detail: 'get@/system/operationLog/:id',
    export: 'post@/api/excel/export/sys_operation_log_export',
  }
  ```

## Task 6: 验证与记录
> 状态：completed。

- **目标**: 执行必要编译构建并记录结果。
- **涉及文件**:
  - `code-copilot/changes/page-operation-audit-visual-optimization/test-spec.md` — 更新验证结果。
  - `code-copilot/changes/page-operation-audit-visual-optimization/execution-log.md` — 追加命令、结果、跳过项。
- **命令**:
  ```bash
  env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -q -pl forge-framework/forge-plugin-parent/forge-plugin-system -am compile -DskipTests
  ```
  ```bash
  source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
  ```

## Task 7: 查询类操作不落审计日志
> 状态：completed。

- **目标**: 默认只记录变更类操作，避免列表、详情、字典等查询请求刷爆审计日志。
- **涉及文件**:
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/aspect/OperationLogAspect.java` — 增加 `shouldSkipOperationLog`、`isReadOnlyMethod`、`resolveDefaultOperationType`，查询直接放行。
  - `code-copilot/changes/page-operation-audit-visual-optimization/spec.md` — 补充变更类操作审计规则。
  - `code-copilot/changes/page-operation-audit-visual-optimization/test-spec.md` — 记录增量验证范围。
  - `code-copilot/changes/page-operation-audit-visual-optimization/execution-log.md` — 追加验证结果。
- **关键规则**:
  ```java
  OperationType.QUERY -> 不记录
  GET / HEAD / OPTIONS -> 默认不记录
  ADD / UPDATE / DELETE / IMPORT / EXPORT / OTHER -> 记录
  POST / PUT / PATCH / DELETE -> 未显式标注时默认记录为 UPDATE 或 DELETE
  ```
- **验收标准**:
  - `/system/operationLog/page`、详情、普通列表查询不新增审计日志。
  - 新增、修改、删除、导入、导出继续新增审计日志。
  - 后端主应用依赖编译通过。

## Task 8: 页面标题、模块和快照展示修复
> 状态：completed。

- **目标**: 修复审计详情中页面显示为系统名、模块为空、加密请求体占位符进入数据快照的问题。
- **涉及文件**:
  - `forge-admin-ui/src/utils/http/interceptors.js` — 页面标题优先从菜单/页签解析，基础系统名不作为审计页面标题。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/aspect/OperationLogAspect.java` — 模块为空时用有效页面标题兜底；POST 查询类 URL 不落库；加密请求体占位符不作为 `afterData`。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/SysUserController.java` — 用户管理主操作补 `@OperationLog` 和脱敏前后快照/diff，重置密码不保存请求参数。
  - `code-copilot/changes/page-operation-audit-visual-optimization/spec.md` — 补充展示修复规则。
  - `code-copilot/changes/page-operation-audit-visual-optimization/test-spec.md` — 补充增量验证范围。
  - `code-copilot/changes/page-operation-audit-visual-optimization/execution-log.md` — 追加验证结果。
- **关键规则**:
  ```text
  页面标题：菜单/页签标题 > document.title，基础系统名不作为审计页面
  操作模块：注解/API配置 > 有效页面标题兜底
  数据快照：[DECRYPTED_REQUEST_BODY_OMITTED] 不进入 after_data
  用户管理：主操作写入脱敏 before_data / after_data / diff_data
  ```
- **验收标准**:
  - `/system/user` 操作日志详情中“操作页面”显示用户管理页面标题，不显示基础系统名。
  - 未配置模块的接口不再显示 `-`，可从页面标题兜底。
  - 用户编辑等加密请求不再在“操作后”展示 `{ "dto": "[DECRYPTED_REQUEST_BODY_OMITTED]" }`。
  - 后端主应用依赖编译和前端构建通过。

## Task 9: 操作类型“新增”重复展示修复
> 状态：completed。

- **目标**: 统一历史 `INSERT` 与现行 `ADD` 新增类型，避免操作日志页面筛选项展示两个“新增”。
- **涉及文件**:
  - `forge-admin-ui/src/views/system/operation-log.vue` — 操作类型字典选项归一化，搜索下拉只展示一个 `ADD=新增`；标签回显保留 `INSERT` 兼容别名。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysOperationLogMapper.xml` — 筛选 `ADD` 时兼容查询历史 `INSERT` 日志。
  - `forge-server/db/migration/V1.0.15__normalize_operation_type_add_dict.sql` — 历史日志 `INSERT` 归一为 `ADD`，字典存在 `ADD` 时禁用旧 `INSERT`，不存在 `ADD` 时才将 `INSERT` 转为 `ADD`。
  - `code-copilot/changes/page-operation-audit-visual-optimization/spec.md` — 补充操作类型归一规则。
  - `code-copilot/changes/page-operation-audit-visual-optimization/test-spec.md` — 补充增量验证范围。
  - `code-copilot/changes/page-operation-audit-visual-optimization/execution-log.md` — 追加验证结果。
- **关键规则**:
  ```text
  页面展示：INSERT -> ADD，只保留一个“新增”选项
  标签回显：ADD / INSERT 都显示“新增”
  查询兼容：operationType=ADD 同时匹配 ADD 和 INSERT
  数据迁移：sys_operation_log.operation_type 从 INSERT 归一为 ADD
  字典迁移：优先保留/启用 ADD；有 ADD 时禁用 INSERT，避免唯一键冲突
  ```
- **验收标准**:
  - `/system/operation-log` 搜索表单“操作类型”下拉只出现一个“新增”。
  - 历史 `INSERT` 日志在迁移前后都能正常显示为“新增”。
  - 选择“新增”筛选时兼容历史 `INSERT` 和现行 `ADD` 数据。
  - Flyway 脚本不触发 `uk_tenant_dict_data(tenant_id, dict_type, dict_value)` 唯一键冲突。
  - 后端主应用依赖编译和前端构建通过。
