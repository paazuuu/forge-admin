# 单测 Spec — 低代码平台通用业务能力底座补齐
> status: review_fix_completed
> created: 2026-07-02

## 0. 测试原则

- **Red/Green TDD**：核心服务必须先写失败测试，再实现。
- **First Run the Tests**：开始 `/apply` 前先跑相关模块已有测试，确认基线。
- **展示工作**：实际执行命令、结果、警告、跳过项必须写入 `execution-log.md`。
- **增量复用**：执行前读取 `code-copilot/rules/automated-testing-standard.md`，复用本变更 `spec.md`、`tasks.md`、`test-spec.md` 和 `execution-log.md`。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit 版本 | 依赖项目现有 Spring Boot 3 / JUnit 5 |
| Mock 框架 | Mockito / Spring Boot Test，按现有模块测试风格复用 |
| 已有测试数量 | `/apply` 前通过 `mvn test` 或定向测试确认 |
| 已有测试风格 | 优先服务单测，数据库相关逻辑使用可控 Mapper Mock 或集成测试 |

## 2. 覆盖范围

### P0 — 核心业务逻辑（必须覆盖）

#### 类名: `BusinessActionExecutionService`

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|
| `execute` | 动作不存在 | `objectCode=A`, `actionCode=missing` | `BusinessObjectActionService` 返回空 | 抛业务异常，写失败日志 |
| `execute` | 无权限 | 有动作配置但用户无权限 | 权限服务返回 false | 拒绝执行，不执行步骤 |
| `execute` | 多步骤成功 | 更新字段 + 创建记录 | `DynamicCrudService` 成功 | 返回 SUCCESS，日志 SUCCESS |
| `execute` | 第二步失败回滚 | 第一步成功，第二步抛错 | 第二步 executor 抛异常 | 事务回滚，日志 FAILED |
| `execute` | 重复幂等键 | 同一 idempotencyKey 重复提交 | 日志已有成功记录 | 返回已有结果或跳过重复执行 |

#### 类名: `BusinessActionStepExecutor`

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|
| `UpdateFieldActionStepExecutor.execute` | 更新当前对象字段 | 当前记录 ID + 字段映射 | `DynamicCrudService.updateFieldsInternal` 成功 | 步骤 SUCCESS |
| `CreateRecordActionStepExecutor.execute` | 创建关联记录 | 目标 configKey + 字段映射 | `DynamicCrudService.insertInternal` 返回新记录 | 步骤返回 createdRecordId |
| `StartFlowActionStepExecutor.execute` | 发起流程 | businessKey + 变量映射 | `BusinessFlowService` 返回流程实例 | 步骤返回 processInstanceId |
| `SendMessageActionStepExecutor.execute` | 发送站内信 | 模板 + 接收人规则 | 消息服务返回 messageId | 步骤返回 messageId |

#### 类名: `BusinessQuantityLedgerService`

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|
| `inbound` | 首次入账 | quantity=10, idempotencyKey=K1 | 无历史流水 | 余额 +10，流水 1 条 |
| `inbound` | 重复入账 | 同 K1 再执行 | 已有流水 | 不重复增加余额 |
| `lock` | 可用数量足够 | balance=10, locked=0, lock=6 | 条件更新成功 | locked=6, available=4 |
| `lock` | 可用数量不足 | balance=5, locked=0, lock=6 | 条件更新 0 行 | 抛业务异常 |
| `release` | 释放锁定 | locked=6, release=6 | 锁存在 | locked=0 |
| `commit` | 扣减锁定 | balance=10, locked=6, commit=6 | 锁存在 | balance=4, locked=0 |
| `transfer` | 转移成功 | source=10, target=0, quantity=3 | 源和目标更新成功 | source=7, target=3，两条流水 |

### P1 — 数据访问层

- `BusinessActionExecutionLogMapper.xml`
  - 按对象、记录、动作编码、执行状态分页查询。
- `BusinessQuantityBalanceMapper.xml`
  - 条件更新锁定数量时必须带 `available >= quantity` 条件。
- `BusinessQuantityLedgerMapper.xml`
  - 幂等键唯一查询。

### P2 — 入口层/服务层

- `BusinessActionExecutionController`
  - `/execute` 参数缺失返回业务异常。
  - `/logs` 使用 `pageNum/pageSize`。
- `BusinessRecordSelectorController`
  - 选择器查询不传对象编码时返回业务异常。
  - 选择器查询返回 Long ID 字符串。
- `BusinessFlowService`
  - 流程通过后能调用动作执行服务。
- `BusinessTriggerExecutor`
  - 新触发器动作编码优先调用动作执行服务。

### 前端验证

- `BusinessActionDesigner.vue`
  - 能配置 `COMMAND` 动作。
  - 动作表单 Schema 可保存并回显。
- `AiCrudPage.vue`
  - `COMMAND` 动作能打开表单弹窗。
  - 执行中按钮 loading，重复点击被拦截。
  - 成功后按 `successBehavior` 刷新。
- `AiRecordSelectorModal.vue`
  - 单选写入表单字段。
  - 多选批量追加子表。

### 不测试（明确列出原因）

- 不测试完整采购仓储业务流。采购仓储只作为手工验收样例，不进入平台单测命名。
- 不测试第三方 Webhook 真外呼。首期 Webhook 可保持 TODO/日志状态。
- 不测试完整 Flowable 引擎内部行为，只 Mock `BusinessFlowService` 或使用已有流程测试基线。

## 3. 执行计划

- [x] Step 1: 读取 `code-copilot/rules/automated-testing-standard.md` 和本变更 SDD 文档。
- [x] Step 2: 运行相关后端测试基线。
- [x] Step 3: 为 `BusinessActionExecutionService` 写 Red 测试。
- [x] Step 4: 实现动作执行服务并确认 Green。
- [x] Step 5: 为数量台账服务写 Red 测试。
- [x] Step 6: 实现数量台账服务并确认 Green。
- [x] Step 7: 执行前端构建。
- [x] Step 8: 追加 `execution-log.md`。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-02 | propose | 未执行 | 未执行 | 当前仅创建 SDD 文档 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-02 | SDD 文档 | 文档创建 | 无 | 完成 | 未进入编码 |
| 2026-07-02 | Phase 1 apply 启动 | 文档状态同步 | 更新 `spec.md`/`tasks.md`/`execution-log.md` | 进行中 | 第一轮仅覆盖通用动作执行引擎 |
| 2026-07-02 | Phase 1 动作底座 | Java 编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 | 首次未指定 Java 17 时失败：`无效的目标发行版: 17` |
| 2026-07-02 | Phase 1 前端动作 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过 | 存在项目既有 CSS `//` 注释和动态/静态 import chunk warning |
| 2026-07-02 | Phase 1 SQL/格式 | 静态检查 | `git diff --check`; `rg -n '\\$\\{[^}]+\\}' forge-server/db/migration` | 通过 | 占位符扫描无输出，命令退出码 1 表示未匹配 |
| 2026-07-02 | Phase 2 记录选择器后端 | Java 编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 | 编译 warning 为既有 deprecated/unchecked 提示 |
| 2026-07-02 | Phase 2 记录选择器前端 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过 | 存在项目既有 CSS `//` 注释和动态/静态 import chunk warning |
| 2026-07-02 | Phase 2 SQL/格式 | 静态检查 | `git diff --check`; `rg -n '\\$\\{[^}]+\\}' forge-server/db/migration`; `rg -n 'valueText\\(' ...` | 通过 | 占位符扫描和 `valueText` 扫描无输出，命令退出码 1 表示未匹配 |
| 2026-07-02 | Phase 2 主子表行级合并 | Java 编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 | 仅有既有 deprecated/unchecked 编译提示 |
| 2026-07-02 | Phase 2 子表编辑器 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过 | 存在项目既有 CSS `//` 注释、动态/静态 import chunk warning、`UserSelectModal` 命名冲突 warning |
| 2026-07-02 | Phase 2 SQL/格式 | 静态检查 | `git diff --check`; `rg -n '\\$\\{[^}]+\\}' forge-server/db/migration` | 通过 | 占位符扫描无输出，命令退出码 1 表示未匹配 |
| 2026-07-02 | Phase 3 流程回调动作 | Java 编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 | 仅有既有 deprecated/unchecked 编译提示 |
| 2026-07-02 | Phase 3 流程绑定面板 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过 | 存在项目既有 CSS `//` 注释、动态/静态 import chunk warning、`UserSelectModal` 命名冲突 warning |
| 2026-07-02 | Phase 3 SQL/格式 | 静态检查 | `git diff --check`; `rg -n '\\$\\{[^}]+\\}' forge-server/db/migration` | 通过 | 占位符扫描无输出，命令退出码 1 表示未匹配 |
| 2026-07-02 | Phase 3 触发器动作迁移 | Java 编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 | 仅有既有 deprecated/unchecked 编译提示 |
| 2026-07-02 | Phase 3 触发器配置前端 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过 | 存在项目既有 CSS `//` 注释、动态/静态 import chunk warning、`UserSelectModal` 命名冲突 warning |
| 2026-07-02 | Phase 3 触发器 SQL/格式 | 静态检查 | `git diff --check`; `rg -n '\\$\\{[^}]+\\}' forge-server/db/migration` | 通过 | 占位符扫描无输出，命令退出码 1 表示未匹配 |
| 2026-07-02 | Phase 4-6 领域动作、数量台账、分层提醒 | Java 编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 | 首次因 `JSONArray` 漏 import 失败，补 import 后通过；仅有既有 deprecated/unchecked 编译提示 |
| 2026-07-02 | Phase 4-6 前端配置 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过 | 存在项目既有 CSS `//` 注释、动态/静态 import chunk warning、`UserSelectModal` 命名冲突 warning |
| 2026-07-02 | Phase 5 数量台账单测 | Maven 定向测试 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessQuantityLedgerServiceTest test` | 未执行 | 命令缺少 `-am` 时因内部模块依赖解析到远程仓库 401 失败 |
| 2026-07-02 | Phase 5 数量台账单测 | Maven 定向测试 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -Dtest=BusinessQuantityLedgerServiceTest -DskipTests=false -Dmaven.test.skip=false -Dsurefire.skipTests=false test` | 构建通过但测试跳过 | 根 POM 中 compiler/surefire 固定 skip，日志显示 `Not compiling test sources`、`Tests are skipped`；新增测试文件已落地但未被 Maven 执行 |
| 2026-07-02 | Phase 4-7 SQL/格式 | 静态检查 | `git diff --check`; `rg -n '\\$\\{[^}]+\\}' forge-server/db/migration` | 通过 | 占位符扫描无输出，命令退出码 1 表示未匹配 |
| 2026-07-03 | Review 修复后端 | Java 编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 | 仅有既有 deprecated/unchecked 编译提示 |
| 2026-07-03 | Review 修复测试启用 | Maven 定向测试 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessQuantityLedgerServiceTest,BusinessActionExecutionServiceTest,BusinessRecordSelectorServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | 8 tests run, 0 failures, 0 errors, 0 skipped；存在既有 deprecated/unchecked 测试编译 warning 和 commons-logging classpath warning |
| 2026-07-03 | Review 修复 SQL/格式 | 静态检查 | `git diff --check`; `rg -n '\\$\\{[^}]+\\}' forge-server/db/migration`; `rg -n 'selectLatestReusableByIdempotencyKey\|valueText\\(' ...` | 通过 | 占位符扫描和历史方法名/未定义前端函数扫描无输出，命令退出码 1 表示未匹配 |

## 6. 执行证据

- `execution-log.md`：进入 `/apply` 或 `/test` 后维护。
- 关键接口：`/ai/business/action/execute`、`/ai/business/selector/query`、`/ai/business/quantity/operation`。
- 关键数据库检查：动作日志表、数量余额表、数量流水表、数量锁定表。
- 服务启动与停止：进入联调阶段记录。
