# 单测 Spec — 采购仓储低代码应用配置落地
> status: apply
> created: 2026-07-03

## 0. 测试原则

- **增量验证**：每轮只验证本轮变更，但必须复用 `spec.md`、`tasks.md` 和 `execution-log.md`。
- **低代码优先**：本变更不新增采购仓储专用服务类，第一阶段主要验证 Flyway SQL、运行配置 JSON 和构建。
- **高风险动作单独验证**：后续涉及数量台账动作时必须补动作执行或端到端验收记录。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit 版本 | JUnit 5（后续动作服务单测使用） |
| Mock 框架 | Mockito / 本地 fake（按既有测试情况选择） |
| 前端验证 | `pnpm --dir forge-admin-ui build` |
| 后端验证 | Maven 插件编译 / 定向单测 |

## 2. 覆盖范围

### P0 — 第一阶段基础资产

| 范围 | 场景 | 预期结果 |
|------|------|----------|
| Flyway SQL | 运行表 DDL | 表包含租户、审计、逻辑删除字段，脚本可重复执行 |
| Flyway SQL | seed 防重复 | 所有 `INSERT` 带 `NOT EXISTS` 或唯一键保护 |
| 运行配置 | `ai_crud_config` JSON | `api_config` 指向 `/ai/crud/{configKey}`，`publish_status=PUBLISHED` |
| 应用入口 | `ai_business_app` | `entry_mode=RUNTIME`，`config_key` 有对应配置 |

### P1 — 主子表与选择器

| 范围 | 场景 | 预期结果 |
|------|------|----------|
| 页面协议 | `master-detail-crud` | 主子表配置能生成 `masterDetailConfig` |
| 选择器 | 多字段搜索/动态过滤 | 物料、库存候选能按配置查询 |

### P2 — 流程动作与数量台账

| 范围 | 场景 | 预期结果 |
|------|------|----------|
| 动作配置 | `FOREACH + QUANTITY` | 每行明细写入稳定幂等键 |
| 流程回调 | 审批通过/驳回 | 正确执行入库、扣减、释放、转移 |

### 不测试

- 第一阶段不做真实 MySQL/Redis 联调，除非本地服务已可用。
- 第一阶段不做像素级 UI 自动截图，先以配置结构和构建为准。

## 3. 执行计划

- [x] Step 1: 读取 `code-copilot/rules/automated-testing-standard.md`。
- [x] Step 2: `git diff --check`。
- [x] Step 3: Flyway placeholder 扫描：`rg -n '\$\{[^}]+\}' forge-server/db/migration`。
- [x] Step 4: 后端插件编译。
- [x] Step 5: 前端构建。
- [x] Step 6: 如新增 Java 动作逻辑，补定向单测并记录 `Tests run`。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-03 | `lowcode-business-transaction-closure` | Maven 定向单测、后端编译、前端构建 | 通过 | 已具备平台底座 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-03 | Phase 1/2 SQL 与配置 | 空白检查 | `git diff --no-index --check /dev/null forge-server/db/migration/V1.0.90__seed_procurement_warehouse_lowcode_app.sql` | 通过，无空白错误输出 | 退出码 1 为 `/dev/null` 对比差异语义，不代表检查失败 |
| 2026-07-03 | Phase 1/2 SQL 与配置 | Flyway placeholder 扫描 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.90__seed_procurement_warehouse_lowcode_app.sql` | 通过，无输出 | 选择器表达式使用 `CONCAT('$', '{formData.xxx}')` 避免 Flyway 占位符 |
| 2026-07-03 | Phase 1/2 SQL 与配置 | SQL 轻量结构检查 | `node -e "const fs=require('fs'); const s=fs.readFileSync(process.argv[1],'utf8'); const q=(s.match(/[']/g)||[]).length; const l=(s.match(/\\(/g)||[]).length; const r=(s.match(/\\)/g)||[]).length; console.log('single_quotes='+q); console.log('left_parens='+l); console.log('right_parens='+r); process.exit((q % 2 || l !== r) ? 1 : 0)" forge-server/db/migration/V1.0.90__seed_procurement_warehouse_lowcode_app.sql` | 通过，`single_quotes=8758`、`left_parens=723`、`right_parens=723` | 未连接 MySQL 实跑迁移 |
| 2026-07-03 | 后端运行态解析链路 | generator 插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过，`BUILD SUCCESS` | 既有 deprecated/unchecked 编译警告 |
| 2026-07-03 | 前端运行态消费链路 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过，`built in 1m 39s` | 既有 Vite chunk 提示、CSS `//` 注释 warning、`UserSelectModal` 命名冲突提示 |
| 2026-07-03 | Phase 3 流程与数量动作 | 空白检查 | `git diff --no-index --check /dev/null forge-server/db/migration/V1.0.91__seed_procurement_warehouse_flow_actions.sql` | 通过，无空白错误输出 | 退出码 1 为 `/dev/null` 对比差异语义，不代表检查失败 |
| 2026-07-03 | Phase 3 流程与数量动作 | Flyway placeholder 扫描 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.91__seed_procurement_warehouse_flow_actions.sql` | 通过，无输出 | 动作表达式使用 `CONCAT('$', '{...}')` |
| 2026-07-03 | Phase 3 流程与数量动作 | SQL 轻量结构检查 | `node -e "const fs=require('fs'); const s=fs.readFileSync(process.argv[1],'utf8'); const q=(s.match(/[']/g)||[]).length; const l=(s.match(/\\(/g)||[]).length; const r=(s.match(/\\)/g)||[]).length; console.log('single_quotes='+q); console.log('left_parens='+l); console.log('right_parens='+r); process.exit((q % 2 || l !== r) ? 1 : 0)" forge-server/db/migration/V1.0.91__seed_procurement_warehouse_flow_actions.sql` | 通过，`single_quotes=1524`、`left_parens=166`、`right_parens=166` | 未连接 MySQL 实跑迁移 |
| 2026-07-03 | Phase 3 后端运行态解析链路 | generator 插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过，`BUILD SUCCESS` | 既有 deprecated/unchecked 编译警告 |
| 2026-07-03 | Phase 3 前端运行态消费链路 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过，`built in 2m 20s` | 既有 Vite chunk 提示、CSS `//` 注释 warning、`UserSelectModal` 命名冲突提示 |
| 2026-07-03 | Phase 4 详情区块与演示数据 | 空白检查 | `git diff --no-index --check /dev/null forge-server/db/migration/V1.0.92__seed_procurement_warehouse_detail_panels_demo_data.sql` | 通过，无空白错误输出 | 退出码 1 为 `/dev/null` 对比差异语义，不代表检查失败 |
| 2026-07-03 | Phase 4 详情区块与演示数据 | Flyway placeholder 扫描 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.92__seed_procurement_warehouse_detail_panels_demo_data.sql` | 通过，无输出 | 面板表达式使用 `CONCAT('$', '{row.id}')` |
| 2026-07-03 | Phase 4 详情区块与演示数据 | SQL 轻量结构检查 | `node -e "const fs=require('fs'); const s=fs.readFileSync(process.argv[1],'utf8'); const q=(s.match(/[']/g)||[]).length; const l=(s.match(/\\(/g)||[]).length; const r=(s.match(/\\)/g)||[]).length; console.log('single_quotes='+q); console.log('left_parens='+l); console.log('right_parens='+r); process.exit((q % 2 || l !== r) ? 1 : 0)" forge-server/db/migration/V1.0.92__seed_procurement_warehouse_detail_panels_demo_data.sql` | 通过，`single_quotes=1350`、`left_parens=261`、`right_parens=261` | 未连接 MySQL 实跑迁移 |
| 2026-07-03 | Phase 5 原型对照 | 文档检查 | `rg -n "采购管理|仓储管理|供应商管理|物料管理|暂需后续平台 UI 能力" code-copilot/changes/lowcode-procurement-warehouse-app/prototype-acceptance.md` | 通过，关键章节齐全 | 无 |
| 2026-07-03 | 用户反馈修复 | 代码空白检查 | `git diff --check -- <本轮相关前后端文件>` | 通过，无输出 | 仅检查本轮相关文件，避免混入其它未提交变更 |
| 2026-07-03 | 用户反馈修复 | V1.0.93 空白检查 | `git diff --no-index --check /dev/null forge-server/db/migration/V1.0.93__fix_procurement_warehouse_lowcode_runtime_config.sql` | 通过，无空白错误输出 | 退出码 1 为 `/dev/null` 对比差异语义，不代表检查失败 |
| 2026-07-03 | 用户反馈修复 | Flyway placeholder 扫描 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.93__fix_procurement_warehouse_lowcode_runtime_config.sql` | 通过，无输出 | 编码规则模板使用 `CONCAT('$', '{...}')` |
| 2026-07-03 | 用户反馈修复 | SQL 轻量结构检查 | `node -e "const fs=require('fs'); const s=fs.readFileSync(process.argv[1],'utf8'); const q=(s.match(/[']/g)||[]).length; const l=(s.match(/\\(/g)||[]).length; const r=(s.match(/\\)/g)||[]).length; console.log('single_quotes='+q); console.log('left_parens='+l); console.log('right_parens='+r); process.exit((q % 2 || l !== r) ? 1 : 0)" forge-server/db/migration/V1.0.93__fix_procurement_warehouse_lowcode_runtime_config.sql` | 通过，`single_quotes=1612`、`left_parens=536`、`right_parens=536` | 未连接 MySQL 实跑迁移 |
| 2026-07-03 | 用户反馈修复 | 后端编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过，`BUILD SUCCESS`，总耗时 `24.179 s` | 既有 deprecated/unchecked 编译警告 |
| 2026-07-03 | 用户反馈修复 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过，`built in 2m 16s` | 既有 `UserSelectModal` 命名冲突、Vite chunk、CSS `//` 注释 warning |
| 2026-07-03 | 物料新增编号为空修复 | 定向单测 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=DynamicCrudServiceAutoGenerationTest test` | 通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` | 首次使用 Mockito mock 时因 ByteBuddy agent 自附加失败，已改为轻量 fake 后通过 |
| 2026-07-03 | 物料新增编号为空修复 | 后端空白检查与编译 | `git diff --check -- DynamicCrudService.java DynamicCrudServiceAutoGenerationTest.java`；`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过，`BUILD SUCCESS`，总耗时 `12.886 s` | 既有 deprecated/unchecked 编译警告 |
| 2026-07-03 | 二次反馈修复 | 代码空白检查 | `git diff --check -- BusinessFieldPropertyPanel.vue ForgePropertyPanel.vue BusinessFormDesigner.vue BusinessObjectDesignerShell.vue ChildTableEditor.vue BusinessObjectDesignerService.java V1.0.94__fix_procurement_warehouse_designer_flow_relation_config.sql` | 通过，无输出 | 仅检查本轮相关文件 |
| 2026-07-03 | 二次反馈修复 | V1.0.94 Flyway placeholder 扫描 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.94__fix_procurement_warehouse_designer_flow_relation_config.sql` | 通过，无输出 | 无 Flyway 占位符风险 |
| 2026-07-03 | 二次反馈修复 | V1.0.94 SQL 轻量结构检查 | `node -e "const fs=require('fs'); const s=fs.readFileSync(process.argv[1],'utf8'); const q=(s.match(/[']/g)||[]).length; const l=(s.match(/\\(/g)||[]).length; const r=(s.match(/\\)/g)||[]).length; console.log('single_quotes='+q); console.log('left_parens='+l); console.log('right_parens='+r); process.exit((q % 2 || l !== r) ? 1 : 0)" forge-server/db/migration/V1.0.94__fix_procurement_warehouse_designer_flow_relation_config.sql` | 通过，`single_quotes=994`、`left_parens=478`、`right_parens=478` | 未连接 MySQL 实跑迁移 |
| 2026-07-03 | 二次反馈修复 | 后端编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过，`BUILD SUCCESS`，总耗时 `14.202 s` | 既有 deprecated/unchecked 编译警告 |
| 2026-07-03 | 二次反馈修复 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过，`built in 1m 54s` | 既有 `UserSelectModal` 命名冲突、Vite dynamic/static import chunk 提示、CSS `//` 注释 warning |

## 6. 执行证据

- `execution-log.md`：记录每轮命令、结果、警告和跳过项。
- 关键接口：`/ai/crud/{configKey}`、`/ai/business/action/execute`、`/ai/business/quantity/query/*`。
- 关键数据库检查：`ai_crud_config`、`ai_business_app`、`ai_lowcode_model`、`pw_*` 运行表。
- 服务启动与停止：如启动本地服务，必须记录 PID 和清理情况。
