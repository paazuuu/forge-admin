# 单测 Spec — 新增页面操作审计日志并优化页面视觉样式
> status: applied
> created: 2026-07-07

## 0. 测试原则
- **Red/Green TDD**：本轮涉及横切日志与已有页面改造，优先补静态/编译/构建验证；若发现已有可复用单测，再按 Red/Green 增量补充。
- **First Run the Tests**：开始验证前读取 `code-copilot/rules/automated-testing-standard.md`。
- **展示工作**：所有命令、结果、警告和跳过项追加到 `execution-log.md`。
- **增量复用**：本变更新建测试规格，后续验证只追加本轮增量。

## 1. 测试框架
| 项目 | 值 |
|------|-----|
| JUnit 版本 | 待 Maven 检测 |
| Mock 框架 | 待 Maven 检测 |
| 前端测试 | Vite 构建为主，必要时补 Playwright 页面检查 |
| 已有测试风格 | 后续按实际模块检查补充 |

## 2. 覆盖范围

### P0 — 核心业务逻辑（必须覆盖）
| 类名 | 方法 | 场景 | 输入 | 预期结果 |
|------|------|------|------|----------|
| `OperationLogAspect` | `around` | 请求头包含页面路径和标题 | `X-Page-Path`, `X-Page-Title` | `OperationLogInfo` 写入页面字段 |
| `OperationLogAspect` | `around` | 查询类请求 | `OperationType.QUERY` 或 `GET/HEAD/OPTIONS` | 直接放行，不保存审计日志 |
| `OperationLogAspect` | `around` | POST 查询兼容接口 | `/getById`、`/page` 等查询 URL | 识别为 `QUERY` 并直接放行 |
| `OperationLogAspect` | `around` | 变更类请求 | `ADD/UPDATE/DELETE/IMPORT/EXPORT/OTHER` 或 `POST/PUT/PATCH/DELETE` | 保存审计日志 |
| `OperationLogAspect` | `fillAuditSnapshot` | 加密请求体被省略 | `[DECRYPTED_REQUEST_BODY_OMITTED]` | 不写入 `afterData` |
| `SysUserController` | `edit/remove/updateStatus/resetPwd` | 用户管理主操作 | 用户 ID / DTO | 写入脱敏 `beforeData`、`afterData`、`diffData` |
| `interceptors.js` | `resolvePageAuditHeaders` | 页面标题仅为基础系统名 | `/system/user` + 菜单数据 | 优先写入菜单/页签标题 |
| `operation-log.vue` | `normalizeOperationTypeOptions` | 字典同时存在 `INSERT=新增` 和 `ADD=新增` | `sys_operation_type` 字典列表 | 搜索下拉只保留一个 `ADD=新增` |
| `SysOperationLogServiceImpl` | `page` | 多条件筛选 | Query DTO | 调用 XML 分页查询 |
| `SysOperationLogServiceImpl` | `selectExportList` | 导出筛选 | Query DTO | 返回导出列表 |

### P1 — 数据访问层
- `SysOperationLogMapper.xml` 明确列字段，不使用 `SELECT *`。
- 页面、操作人、时间、状态、URL 等筛选条件可组合。
- `operationType=ADD` 筛选兼容历史 `INSERT` 日志。
- `V1.0.15__normalize_operation_type_add_dict.sql` 避免在 `sys_dict_data` 唯一键已存在 `ADD` 时直接把 `INSERT` 改成 `ADD`。

### P2 — 入口层/服务层
- `/system/operationLog/page` 返回新增字段。
- `/system/operationLog/{id}` 返回详情。
- `/api/excel/export/sys_operation_log_export` 可反射调用 `sysOperationLogService.selectExportList`。

### 不测试（明确列出原因）
- 不做真实合规留存策略验证：本需求不新增清理任务。
- 不做所有业务表数据库级 diff 自动采集：通用切面不具备跨业务表自动前后快照能力，本轮提供上下文字段和默认请求/响应快照。

## 3. 执行计划
- [x] Step 1: 读取自动化测试标准。
- [x] Step 2: 静态检查迁移脚本版本、字段、`NOT EXISTS`、显式列。
- [x] Step 3: 后端 system 插件依赖编译。
- [x] Step 4: 前端生产构建。
- [ ] Step 5: 如服务可用，登录后访问 `/system/operation-log` 验证列表、筛选、详情、导出。

## 4. 历史验证基线
| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-07 | 初始调研 | `rg OperationLog/sys_operation_log` | 通过 | 确认已有日志链路 |

## 5. 本轮增量验证
| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-07 | 静态 | 空白/SQL/XML | `git diff --check`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.14__enhance_operation_log_page_audit.sql`; `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysOperationLogMapper.xml` | 通过 | Flyway 占位符扫描无输出 |
| 2026-07-07 | 后端 | 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -q -pl forge-framework/forge-plugin-parent/forge-plugin-system -am compile -DskipTests` | 通过 | - |
| 2026-07-07 | 后端 | 主应用依赖编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -q -pl forge-admin-server -am compile -DskipTests` | 通过 | - |
| 2026-07-07 | 前端 | 构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过 | `v20.20.0` 未安装，已回退到本机已安装的 `v20.19.0`；构建存在既有 warning |
| 2026-07-08 | 后端 | 查询日志过滤增量 | `git diff --check`; `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -q -pl forge-admin-server -am compile -DskipTests` | 通过 | 未启动服务做数据库落库验证 |
| 2026-07-08 | SQL | Flyway 字典补丁别名修复 | `rg -n "seed\\.dict_label|'新增' dict_label|SELECT 1 tenant_id, 2 dict_sort" forge-server/db/migration/V1.0.14__enhance_operation_log_page_audit.sql`; `git diff --check` | 通过 | 本地 3407 MySQL 未启动，最小 SQL 片段实跑跳过 |
| 2026-07-08 | 前后端 | 页面标题/模块/快照修复 | `git diff --check`; `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -q -pl forge-admin-server -am compile -DskipTests`; `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过 | 前端构建仍存在既有动态导入、CSS `//` 注释和 chunk size warning |
| 2026-07-08 | 前后端/SQL | 操作类型新增重复修复 | `git diff --check`; `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.15__normalize_operation_type_add_dict.sql`; `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysOperationLogMapper.xml`; `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -q -pl forge-admin-server -am compile -DskipTests`; `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过 | `rg` 占位符扫描无输出；前端构建仍存在既有 warning；未启动 MySQL 实跑 Flyway |

## 6. 执行证据
- `execution-log.md`：本变更目录执行日志。
- 关键接口：`/system/operationLog/page`、`/system/operationLog/{id}`、`/api/excel/export/sys_operation_log_export`。
- 关键数据库检查：`SHOW COLUMNS FROM sys_operation_log`、Excel 配置 `sys_operation_log_export`。
- 服务启动与停止：按执行情况补充。

## 7. 跳过项
- 后端服务启动、登录后页面联调、真实数据库 Flyway 实跑：本轮未启动本地 MySQL/Redis/后端服务；已用幂等 SQL 静态检查、Mapper XML 检查、后端主应用依赖编译和前端构建覆盖核心风险。
