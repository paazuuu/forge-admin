# 执行日志 — 低代码平台通用业务能力底座补齐
> status: review_fix_completed
> created: 2026-07-02

## 2026-07-02 SDD 提案创建

- 用户明确要求：平台能力必须做通用功能，不针对采购仓储单一需求，按 SDD 方式开发。
- 已创建：
  - `code-copilot/changes/lowcode-platform-capability-foundation/spec.md`
  - `code-copilot/changes/lowcode-platform-capability-foundation/tasks.md`
  - `code-copilot/changes/lowcode-platform-capability-foundation/test-spec.md`
- 当前未进入编码，未执行构建或测试。
- 下一步：用户确认 HARD-GATE 后进入 `/apply`，建议第一轮只实现 Phase 1「通用动作执行引擎」。

## 2026-07-02 Apply 启动

- 用户执行：`apply lowcode-platform-capability-foundation`，并要求及时更新任务状态。
- 已确认第一轮编码范围：Phase 1「通用动作执行引擎」。
- 范围控制：本轮不实现选择器、流程回调、领域动作 SPI、数量台账和提醒增强。
- 已读取自动化测试标准和本变更 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`，后续验证按增量范围记录。
- 当前 Task 状态：Task 1 已切换为 `in_progress`。

## 2026-07-02 Task 1 完成

- 新增 Flyway 脚本：`forge-server/db/migration/V1.0.87__add_business_action_execution_log.sql`。
- 新增动作协议 DTO/VO：执行请求、步骤配置、日志查询、执行结果、步骤结果。
- 新增动作执行日志实体和 Mapper XML，日志查询支持对象、记录、动作、状态和链路 ID 过滤。
- 任务状态：Task 1 已完成，Task 2「通用动作执行服务」进入 `in_progress`。

## 2026-07-02 Task 2/3 完成

- 新增 `BusinessActionExecutionService`，统一负责动作解析、动作权限校验、幂等命中、步骤编排、事务执行和动作日志写入。
- 新增四类白名单步骤执行器：`UPDATE_FIELD`、`CREATE_RECORD`、`START_FLOW`、`SEND_MESSAGE`。
- 新增 `/ai/business/action/execute`、`/ai/business/action/preview`、`/ai/business/action/logs` 接口。
- 前端 API 文件已新增 `executeBusinessAction`、`previewBusinessAction`、`businessActionLogs`。
- 任务状态：Task 2、Task 3 已完成，Task 4「前端动作设计和运行态执行」进入 `in_progress`。

## 2026-07-02 Task 4 与验证完成

- `BusinessActionDesigner.vue`：新增 `COMMAND` 动作类型，支持动作表单 Schema 和动作步骤配置保存为通用协议。
- `BusinessListDesigner.vue`：新增 `COMMAND` 设计态/运行态转换，发布后的列表行操作可透传通用动作配置。
- `AiCrudPage.vue`：运行态支持 `COMMAND` 动作；有 `formSchema` 时弹窗采集参数，无表单时直接执行；复用 action loading key 防重复点击；成功后按 `successBehavior` 刷新或返回。
- 验证命令：
  - `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：首次失败，原因是 shell 未指定 Java 17，报 `无效的目标发行版: 17`。
  - `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：通过；仅有项目既有 chunk warning 和 CSS `//` 注释 warning。
  - `git diff --check`：通过。
  - `rg -n '\$\{[^}]+\}' forge-server/db/migration`：无输出，确认本轮迁移未引入 Flyway placeholder。
- 任务状态：Phase 1 的 Task 1-4 已完成；后续 Phase 2-6 仍按原任务拆分保持 `pending`。

## 2026-07-02 Phase 2 启动

- 用户要求继续推进。
- 当前进入 Phase 2，先实现 Task 5「通用记录选择器后端协议」。
- 任务状态：Task 5 已切换为 `in_progress`。

## 2026-07-02 Task 5/6 完成与验证

- Task 5：新增 `BusinessRecordSelectorController`、`BusinessRecordSelectorService`、`BusinessRecordSelectorQueryDTO`、`BusinessRecordSelectorResultVO`，选择器查询通过业务对象运行配置调用 `DynamicCrudService.selectPage`，复用字段白名单、运行数据源、租户/数据权限、解密/脱敏/公式读取链路；返回记录中的 Long ID 和 `_raw` Long 值统一字符串化。
- Task 6：新增 `AiRecordSelectorModal.vue` 和 `record-selector-utils.js`；`AiFormItem.vue` 支持 `recordSelector` 字段单选回填与字段映射；`ChildTableEditor.vue` 支持按选择器多选批量追加子表行。
- Task 6 配置入口：`BusinessFieldPropertyPanel.vue` 新增 `RECORD_SELECTOR` 字段类型和记录选择器配置区；`ForgeFieldShelf.vue` 新增“记录选择器”组件；`BusinessFormDesigner.vue`、`ForgePropertyPanel.vue`、`formDesignerSchema.js`、`autoFieldRegistry.js`、`BusinessObjectDesignerService.java`、`BusinessFieldSchemaService.java`、`LowcodeRuntimeConfigBuilder.java`、`LowcodeSchemaValidator.java` 已补齐组件类型映射和校验。
- 修复项：`AiFormItem.vue` 中新增选择器显示文本误用了不存在的 `valueText`，已改为现有 `normalizeDisplayText`。
- 验证命令：
  - `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：通过；仅有既有 deprecated/unchecked 编译提示。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：通过；仅有项目既有 CSS `//` 注释和动态/静态 import chunk warning。
  - `git diff --check`：通过。
  - `rg -n '\$\{[^}]+\}' forge-server/db/migration`：无输出，未引入 Flyway placeholder。
  - `rg -n 'valueText\(' forge-admin-ui/src/components/ai-form/AiFormItem.vue forge-admin-ui/src/components/page-templates/ChildTableEditor.vue forge-admin-ui/src/components/ai-form/AiRecordSelectorModal.vue`：无输出，确认未保留未定义调用。
- 跳过项：
  - 未启动后端服务做真实接口联调，本轮先以插件编译和前端构建验证增量代码；本地数据库状态未作为前提。
  - 未实现 Task 7 的服务端主子表 `merge` 保存模式；本轮只完成选择器批量追加子表行，行级合并保存仍需单独改 `DynamicCrudService` 并保持默认 replace 行为兼容。
- 任务状态：Task 5、Task 6 已完成；Task 7 保持 `pending`。

## 2026-07-02 Task 7 完成与验证

- 后端主子表保存增强：
  - `DynamicCrudService` 新增子表 `saveMode=merge` 处理分支，默认仍保持 `replace` 全量替换，避免改变既有行为。
  - merge 模式支持子表行新增、按 `id` 修改、按 `_deleted/__deleted` 删除。
  - 更新/删除已有子行前校验该行属于当前主记录，防止跨主表记录篡改子表。
  - 子表写入继续走字段白名单和动态表字段映射；新增/修改时补充必填与配置化 `min/max` 数值范围校验。
  - 修复 `DynamicCrudService` 中未定义的 `mapValue(ref.getProps())` 调用，改为复用现有 `firstMap`。
- 配置链路：
  - `BusinessRelationDesigner.vue` 已支持“子表保存模式”配置。
  - `BusinessObjectDesignerService` 将关系 `relationConfig.saveMode` 同步到 `pageSchema.modelRefs[].props.saveMode`。
  - `LowcodeRuntimeConfigBuilder` 将 `saveMode` 输出到 `masterDetailConfig.children[]`。
- 前端运行态：
  - `ChildTableEditor.vue` 在 merge 模式下对已有行删除改为隐藏并提交 `_deleted: true`。
  - 新增未保存行仍直接移除，避免产生无效删除 payload。
  - `getValue()` 保留 `_deleted` 行给后端，表格展示和校验跳过已删除行。
- 验证命令：
  - `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：通过；仅有既有 deprecated/unchecked 编译提示。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：通过；仅有项目既有 CSS `//` 注释、动态/静态 import chunk warning 和 `UserSelectModal` 命名冲突 warning。
  - `git diff --check`：通过。
  - `rg -n '\$\{[^}]+\}' forge-server/db/migration`：无输出，未引入 Flyway placeholder。
- 跳过项：
  - 未启动后端服务做真实主子表保存接口联调，本轮先以插件编译、前端构建和静态检查验证；本地数据库状态未作为前提。
- 任务状态：Task 7 已完成；Task 8「流程回调绑定动作」进入 `in_progress`。

## 2026-07-02 Task 8 完成与验证

- 后端流程回调：
  - `BusinessFlowService` 在流程结果回调状态回写、实例关联更新后，按流程结果读取 `bindingConfig.options.callbackActions`。
  - 支持 `APPROVED`、`REJECTED`、`CANCELED` 三类结果绑定对象动作编码。
  - 回调动作复用 `BusinessActionExecutionService`，动作步骤、事务、幂等和执行日志都走通用动作引擎。
  - 幂等键格式为 `flowCallback:{processInstanceId|businessKey}:{result}:{actionCode}`，防止流程重复回调重复执行业务副作用。
  - 动作失败会抛出明确业务异常，回调调用方可感知并重试；动作失败日志由动作引擎记录。
- 前端流程绑定：
  - `BusinessFlowBindingPanel.vue` 新增“流程结果动作”配置区，可为通过、驳回、取消分别选择对象动作。
  - 动作候选来自当前业务对象动作列表，仅展示 `COMMAND` 或具备步骤配置的动作。
  - 配置保存到 `options.callbackActions`，运行摘要展示已配置回调动作数量。
  - `BusinessFlowAppConfigPanel.vue` 将 `objectId` 传给流程绑定面板，用于加载对象动作。
- 验证命令：
  - `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：通过；仅有既有 deprecated/unchecked 编译提示。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：通过；仅有项目既有 CSS `//` 注释、动态/静态 import chunk warning 和 `UserSelectModal` 命名冲突 warning。
  - `git diff --check`：通过。
  - `rg -n '\$\{[^}]+\}' forge-server/db/migration`：无输出，未引入 Flyway placeholder。
- 跳过项：
  - 未启动 Flowable 和后端服务做真实流程回调联调，本轮先以编译和构建验证；本地流程服务、数据库状态未作为前提。
- 任务状态：Task 8 已完成；Task 9「触发器动作迁移到动作引擎」进入 `in_progress`。

## 2026-07-02 Task 9 完成与验证

- 后端触发器：
  - `BusinessTriggerExecutor` 注入 `BusinessActionExecutionService`。
  - 新增触发器动作优先路径：当 `actionType` 为 `BUSINESS_ACTION`、`ACTION`、`COMMAND`，或 `actionConfig` 中存在 `actionCode/businessActionCode/commandActionCode` 时，统一调用动作执行引擎。
  - 动作请求携带对象编码、记录 ID、事件上下文和幂等键，动作结果回写触发器日志的 `correlationId` 与 `actionResult`。
  - 旧 `START_FLOW`、`SEND_MESSAGE`、`CREATE_RECORD`、`UPDATE_FIELD`、`WEBHOOK` 保持原兼容执行路径。
- 前端触发器配置：
  - `trigger.vue` 增加“执行对象动作”类型，并按当前业务对象加载可执行对象动作选项。
  - `TriggerActionConfigPanel.vue` 支持 `BUSINESS_ACTION`，保存标准 `actionCode` 配置。
- 验证命令：
  - `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：通过；仅有既有 deprecated/unchecked 编译提示。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：通过；仅有项目既有 CSS `//` 注释、动态/静态 import chunk warning 和 `UserSelectModal` 命名冲突 warning。
  - `git diff --check`：通过。
  - `rg -n '\$\{[^}]+\}' forge-server/db/migration`：无输出，未引入 Flyway placeholder。
- 跳过项：
  - 未启动后端服务做真实事件触发联调，本轮先以编译、构建和静态检查验证；本地数据库状态未作为前提。
- 任务状态：Task 9 已完成；Task 10「领域动作 SPI」进入 `in_progress`。

## 2026-07-02 Task 10 完成

- 新增 `BusinessDomainActionExecutor`：定义领域动作 SPI，领域能力通过 `actionType()` 注册。
- 新增 `BusinessDomainActionRegistry`：启动时收集领域动作执行器，重复注册或空 `actionType` 明确失败；运行时未注册动作返回 `未注册领域动作`。
- 新增 `DomainActionStepExecutor`：动作步骤类型 `DOMAIN_ACTION` 接入领域动作注册中心；支持从 `params` 和字段映射中解析参数，支持 `${field}` 从当前记录/表单/上下文取值。
- 任务状态：Task 10 已完成；Task 11「通用数量台账数据模型」进入实现。

## 2026-07-02 Task 11-13 完成

- 数据模型：
  - 新增 Flyway 脚本 `V1.0.88__add_business_quantity_ledger.sql`。
  - 新增 `ai_business_quantity_balance`、`ai_business_quantity_ledger`、`ai_business_quantity_lock` 三张通用数量表。
  - 余额唯一维度为 `tenant_id + account_code + item_code + dimension_key`。
  - 流水和锁定表均包含 `idempotency_key` 唯一约束，避免重复入账、重复锁定或重复扣减。
  - 新增权限资源 `ai:businessQuantity:operate`，内置数据 `tenant_id=1`，脚本无 Flyway `${...}` placeholder。
- 后端服务：
  - 新增数量实体、DTO、VO、Mapper 和 XML。
  - `BusinessQuantityLedgerService` 支持 `inbound`、`lock`、`release`、`commit`、`transfer`。
  - 数量锁定、释放、扣减和转移均通过 Mapper XML 条件更新防止可用数量或锁定数量变为负数。
  - 重复幂等键命中时直接返回已有流水结果，不重复写余额。
  - 转移动作在同一事务中完成源端扣减、目标端入账和流水写入。
- 动作接入：
  - 新增 `BusinessQuantityDomainActionExecutor`，注册为领域动作 `QUANTITY`。
  - `BusinessActionDesigner.vue` 的通用动作步骤区新增“数量台账步骤”模板插入和格式化入口，减少从空 JSON 手写步骤。
  - 新增 `BusinessQuantityLedgerServiceTest` 覆盖入账幂等、锁定不足、转移成功三个核心分支；受根 POM 固定跳过测试影响，测试类未被 Maven 实际执行，见验证记录。
- 任务状态：Task 11、Task 12、Task 13 已完成；Task 14「分层提醒规则增强」进入实现。

## 2026-07-02 Task 14 完成

- 后端定时触发器：
  - `BusinessTriggerSchedulerService` 支持 `schedule.tierRules` / `schedule.reminderRules`。
  - 扫描窗口按基础 `lookAheadDays` 和分层规则最大 `lookAheadDays` 取最大值。
  - 每条候选记录按到期日期、指标字段、最小值、最大值和提前天数匹配分层规则。
  - 匹配到的规则写入事件上下文：`reminderRuleCode`、`reminderRuleName`、`reminderReceiverRule`、`reminderLookAheadDays`、`reminderMetricField`、`reminderMetricValue`。
  - 仍复用现有触发器日志按同一触发器、同一记录、同一天去重；多规则命中同一记录时当前实现会合并为一次提醒。
- 后端消息动作：
  - `BusinessTriggerExecutor` 的 `SEND_MESSAGE` 动作优先读取事件上下文中的 `reminderReceiverRule`，分层规则可覆盖默认接收人规则。
- 前端配置：
  - `trigger.vue` 的定时触发器普通模式新增“分层提醒”配置区。
  - 每条规则可配置规则名称、指标字段、最小值、最大值、提前天数和接收人规则。
- 任务状态：Task 14 已完成；Task 15「自动化测试和构建验证」进入执行。

## 2026-07-02 Task 15 验证完成

- 后端编译：
  - `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：首次失败，原因是 `BusinessTriggerSchedulerService` 漏 import `JSONArray`；补充 import 后重跑通过。
  - 通过时仅有既有 deprecated/unchecked 编译提示。
- 前端构建：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：通过。
  - warning 为既有 CSS `//` 注释、动态/静态 import chunk warning、`UserSelectModal` 命名冲突 warning。
- 数量台账单测：
  - `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessQuantityLedgerServiceTest test`：未执行，缺少 `-am` 导致内部模块依赖解析到远程仓库并因 401 失败。
  - `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -Dtest=BusinessQuantityLedgerServiceTest -DskipTests=false -Dmaven.test.skip=false -Dsurefire.skipTests=false test`：构建通过，但根 POM 固定配置 compiler/surefire skip，日志显示 `Not compiling test sources`、`Tests are skipped`，测试类未实际执行。
- 静态检查：
  - `git diff --check`：通过。
  - `rg -n '\$\{[^}]+\}' forge-server/db/migration`：无输出，未引入 Flyway placeholder。
- 跳过项：
  - 未启动后端、前端和数据库做真实低代码配置联调。
  - 新增单测受根 POM 固定 skip 配置影响未实际执行。
- 任务状态：Task 15 已完成；Task 16「SDD 日志、审查和归档准备」进入执行。

## 2026-07-02 Task 16 完成

- `tasks.md` 已更新为 `review_ready`，Task 1-16 均已完成。
- `spec.md` 已补齐 Task 10-16 的实际改动文件、验证结果和遗留风险。
- `test-spec.md` 已记录 Phase 4-7 的编译、构建、测试跳过和静态检查结果。
- 当前可进入 `/review lowcode-platform-capability-foundation`。

## 2026-07-03 Review 修复完成

- 修复动作幂等并发问题：
  - `BusinessActionExecutionService` 执行动作前先以独立事务写入 `RUNNING` 预占日志。
  - 相同 `tenant_id + object_code + record_id + action_code + idempotency_key` 的重复请求命中 `RUNNING` 时直接拒绝，不再执行步骤。
  - 幂等冲突使用独立异常分支处理，不再尝试补写失败日志，避免唯一键拦截后产生无意义 warning。
  - `BusinessActionExecutionLogMapper.xml` 的幂等查询改为查询最新日志，不再只查询 `SUCCESS/TODO`。
  - `V1.0.87__add_business_action_execution_log.sql` 将 `record_id` 改为 `NOT NULL DEFAULT ''`，避免 MySQL 唯一索引遇到 `NULL` 后无法防重。
- 修复选择器权限和字段泄漏问题：
  - `BusinessRecordSelectorService` 增加目标对象 `VIEW` 权限校验。
  - 选择器响应不再返回运行态 `configKey`。
  - `_raw` 只投影 `id`、展示字段、关键词字段、搜索参数字段和映射源字段，并排除租户、审计、删除标记和 `_` 开头内部字段。
  - 排序字段只允许使用本次选择器请求字段，避免通过 URL/page 参数绕过字段边界。
- 修复数量台账幂等和转移流水问题：
  - `BusinessQuantityLedgerService` 强制所有数量操作携带稳定幂等键。
  - `BusinessQuantityDomainActionExecutor` 移除随机链路号兜底，改为配置幂等键、动作请求幂等键或稳定业务来源哈希。
  - `BusinessTriggerExecutor` 调用业务动作时生成稳定触发器幂等键。
  - `transfer` 转移写源端 `:source` 和目标端 `:target` 两条流水，结果返回 `targetLedgerId`。
- 修复测试未实际执行问题：
  - `forge-server/pom.xml` 增加 `enable-tests` profile，允许定向开启 testCompile/surefire。
  - 对已有历史坏测试增加临时 testCompile exclude，避免本变更定向测试被无关旧问题阻断。
  - 新增/更新 `BusinessActionExecutionServiceTest`、`BusinessRecordSelectorServiceTest`、`BusinessQuantityLedgerServiceTest`。
- 验证命令：
  - `git diff --check`：通过。
  - `rg -n '\$\{[^}]+\}' forge-server/db/migration`：无输出，未引入 Flyway placeholder。
  - `rg -n 'selectLatestReusableByIdempotencyKey|valueText\(' forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator forge-admin-ui/src/components/ai-form forge-admin-ui/src/components/page-templates`：无输出。
  - `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：通过；仅有既有 deprecated/unchecked 编译提示。
  - `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessQuantityLedgerServiceTest,BusinessActionExecutionServiceTest,BusinessRecordSelectorServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`：通过；8 tests run, 0 failures, 0 errors, 0 skipped。
- 跳过项：
  - 本轮 Review 修复只改后端服务、SQL 和 Maven 测试配置，未触碰前端代码，未重新执行前端构建；前端构建已在 Phase 1-6 多轮通过。
  - 未启动真实后端、前端和数据库做低代码运行态联调；仍需在具备本地库和 Redis 的环境做一次端到端验收。
- 当前状态：Review 发现项已修复，变更状态更新为 `review_fix_completed`。
