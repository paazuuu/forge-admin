# 任务拆分 — 低代码平台通用业务能力底座补齐
> status: review_fix_completed
> created: 2026-07-02
> 拆分顺序：动作协议 → 动作执行 → 前端动作 → 选择器/主子表 → 流程回调 → 领域动作 SPI → 数量台账 → 提醒规则 → 验证归档
> 原则：每个任务可独立提交；所有平台能力必须通用；采购仓储只作为验收样例；查询 SQL 写 Mapper XML；内置数据 `tenant_id=1`；Flyway 脚本必须防重复。

## 前置条件

- [x] 已确认第一轮编码范围。建议先做 Phase 1「通用动作执行引擎」。
- [x] 已确认不在平台代码中硬编码采购、仓库、供应商、物料等业务对象。
- [x] 已确认流程审批仍统一走 Flowable，不新建审批引擎。
- [x] 已确认高风险副作用必须经过动作日志、权限和幂等控制。

## 阶段总览

| 阶段 | 目标 | 包含任务 | 交付结果 |
|------|------|----------|----------|
| Phase 1 | 动作底座 | Task 1-4 | 统一动作协议、执行服务、日志和按钮运行 |
| Phase 2 | 选择器与主子表 | Task 5-7 | 通用记录选择器、批量带入子表、服务端校验 |
| Phase 3 | 流程与触发器融合 | Task 8-9 | 流程结果和触发器统一复用动作引擎 |
| Phase 4 | 领域动作 SPI | Task 10 | 可插拔领域动作能力 |
| Phase 5 | 数量台账 | Task 11-13 | 通用数量余额、流水、锁定、转移 |
| Phase 6 | 提醒增强 | Task 14 | 分层提醒规则 |
| Phase 7 | 验证归档 | Task 15-16 | 测试、执行日志、审查准备 |

## 任务总览

| Task | 阶段 | 名称 | 状态 | 优先级 |
|------|------|------|------|--------|
| Task 1 | Phase 1 | 动作协议 DTO/VO 与执行日志表 | completed | P0 |
| Task 2 | Phase 1 | 通用动作执行服务 | completed | P0 |
| Task 3 | Phase 1 | 动作执行接口与权限 | completed | P0 |
| Task 4 | Phase 1 | 前端动作设计和运行态执行 | completed | P0 |
| Task 5 | Phase 2 | 通用记录选择器后端协议 | completed | P1 |
| Task 6 | Phase 2 | 通用记录选择器前端组件 | completed | P1 |
| Task 7 | Phase 2 | 主子表映射和行级合并增强 | completed | P1 |
| Task 8 | Phase 3 | 流程回调绑定动作 | completed | P0 |
| Task 9 | Phase 3 | 触发器动作迁移到动作引擎 | completed | P1 |
| Task 10 | Phase 4 | 领域动作 SPI | completed | P0 |
| Task 11 | Phase 5 | 通用数量台账数据模型 | completed | P0 |
| Task 12 | Phase 5 | 通用数量台账服务 | completed | P0 |
| Task 13 | Phase 5 | 数量台账接入动作步骤 | completed | P0 |
| Task 14 | Phase 6 | 分层提醒规则增强 | completed | P2 |
| Task 15 | Phase 7 | 自动化测试和构建验证 | completed | P0 |
| Task 16 | Phase 7 | SDD 日志、审查和归档准备 | completed | P0 |
| Review Fix | Review | 审查问题修复与回归验证 | completed | P0 |

---

## Phase 1：动作底座

### Task 1: 动作协议 DTO/VO 与执行日志表

**目标**: 定义通用动作执行输入、输出和执行日志，不绑定具体业务场景。

**涉及文件**:
- `forge-server/db/migration/V1.0.xx__add_business_action_execution_log.sql` — 新增动作执行日志表和权限资源。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessActionExecuteDTO.java` — 新增动作执行请求 DTO。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessActionStepDTO.java` — 新增动作步骤 DTO。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessActionExecuteResultVO.java` — 新增动作执行结果 VO。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessActionExecutionLog.java` — 新增执行日志实体。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessActionExecutionLogMapper.java` — 新增 Mapper。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessActionExecutionLogMapper.xml` — 新增日志分页查询 XML。

**关键签名**:
```java
public class BusinessActionExecuteDTO {
    private String objectCode;
    private String recordId;
    private String actionCode;
    private Map<String, Object> formData;
    private Map<String, Object> context;
    private String idempotencyKey;
}
```

```java
public class BusinessActionStepDTO {
    private String stepCode;
    private String stepName;
    private String stepType;
    private Map<String, Object> stepConfig;
    private Integer sortOrder;
    private Boolean rollbackOnFailure;
}
```

**验收标准**:
- [x] Flyway 脚本可重复执行。
- [x] 执行日志支持按对象、记录、动作、状态查询。
- [x] 权限资源包含 `ai:businessAction:execute` 和 `ai:businessAction:log`。

### Task 2: 通用动作执行服务

**目标**: 实现统一动作执行器，首期支持更新字段、创建记录、发起流程、发送消息四类白名单步骤。

**涉及文件**:
- `BusinessActionExecutionService.java` — 新增动作执行编排服务。
- `BusinessActionStepExecutor.java` — 新增步骤执行接口。
- `UpdateFieldActionStepExecutor.java` — 新增更新字段步骤。
- `CreateRecordActionStepExecutor.java` — 新增创建记录步骤。
- `StartFlowActionStepExecutor.java` — 新增发起流程步骤。
- `SendMessageActionStepExecutor.java` — 新增发送消息步骤。
- `BusinessObjectActionService.java` — 新增按 `objectCode + actionCode` 解析动作配置方法。

**关键签名**:
```java
public BusinessActionExecuteResultVO execute(BusinessActionExecuteDTO dto);
```

```java
public interface BusinessActionStepExecutor {
    String supportType();
    BusinessActionStepResult execute(BusinessActionExecutionContext context, BusinessActionStepDTO step);
}
```

**验收标准**:
- [x] 动作不存在时返回明确业务异常。
- [x] 无权限时拒绝执行。
- [x] 任一步骤失败时按配置回滚事务并写失败日志。
- [x] `UPDATE_FIELD` 和 `CREATE_RECORD` 复用 `DynamicCrudService` 的内部写入方法。

### Task 3: 动作执行接口与权限

**目标**: 对外提供动作执行、动作预览和动作日志查询接口。

**涉及文件**:
- `BusinessActionExecutionController.java` — 新增 REST Controller。
- `BusinessPermissionService.java` — 增加动作权限判定复用。
- `business-app.js` — 增加前端 API。

**关键签名**:
```java
@PostMapping("/execute")
@SaCheckPermission("ai:businessAction:execute")
public RespInfo<BusinessActionExecuteResultVO> execute(@RequestBody BusinessActionExecuteDTO dto) { }
```

```java
@GetMapping("/logs")
@SaCheckPermission("ai:businessAction:log")
public RespInfo<Page<AiBusinessActionExecutionLog>> logs(BusinessActionLogQueryDTO query, PageQuery pageQuery) { }
```

**验收标准**:
- [x] 接口统一返回 `RespInfo.success(data)`。
- [x] 日志接口分页参数使用 `pageNum/pageSize`。
- [x] 无权限执行动作时前端能拿到明确错误。

### Task 4: 前端动作设计和运行态执行

**目标**: 在对象动作设计器和运行态列表/详情按钮中支持 `COMMAND` 类型动作。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue` — 增加 `COMMAND` 动作类型、动作表单 Schema、步骤配置摘要。
- `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue` — 同步 `COMMAND` 到列表运行态 action。
- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` — 运行态执行 `COMMAND`，支持表单弹窗、确认、loading、成功刷新。
- `forge-admin-ui/src/api/business-app.js` — 增加动作执行 API。

**关键行为**:
- `actionType=COMMAND` 且存在 `actionConfig.formSchema` 时先打开动作弹窗。
- 提交时调用 `/ai/business/action/execute`。
- 成功后按 `successBehavior` 执行刷新列表、刷新详情、关闭弹窗或返回。

**验收标准**:
- [x] 行按钮可配置为 `COMMAND` 并执行。
- [x] 重复点击被 loading key 拦截。
- [x] 弹窗表单提交失败时不关闭弹窗并展示错误。

---

## Phase 2：选择器与主子表

### Task 5: 通用记录选择器后端协议

**目标**: 提供按业务对象运行配置查询选择器数据的通用接口。

**涉及文件**:
- `BusinessRecordSelectorController.java` — 新增选择器接口。
- `BusinessRecordSelectorService.java` — 新增选择器查询服务。
- `BusinessRecordSelectorQueryDTO.java` — 查询请求。
- `BusinessRecordSelectorResultVO.java` — 查询结果。

**关键签名**:
```java
public BusinessRecordSelectorResultVO query(BusinessRecordSelectorQueryDTO dto, PageQuery pageQuery);
```

**验收标准**:
- [x] 选择器查询复用动态 CRUD 查询和数据权限。
- [x] 返回值中的 Long ID 以字符串形式给前端。
- [x] 不暴露表名和 SQL 给普通用户。

### Task 6: 通用记录选择器前端组件

**目标**: 支持表单字段和子表行通过弹窗选择其他对象记录。

**涉及文件**:
- `forge-admin-ui/src/components/ai-form/AiRecordSelectorModal.vue` — 新增选择器弹窗。
- `forge-admin-ui/src/components/ai-form/record-selector-utils.js` — 字段映射工具。
- `forge-admin-ui/src/components/page-templates/ChildTableEditor.vue` — 支持选择记录批量追加子表。
- `BusinessFormDesigner.vue` — 选择器配置入口。

**验收标准**:
- [x] 支持单选写入当前表单字段。
- [x] 支持多选批量追加到子表。
- [x] 字段映射失败时有明确提示。

### Task 7: 主子表映射和行级合并增强

**目标**: 主子表运行态支持稳定行级更新和服务端校验。

**涉及文件**:
- `DynamicCrudService.java` — 增加 `merge` 模式处理子表。
- `ChildTableEditor.vue` — 保留行 ID、支持 `_deleted` 标记。
- `LowcodeRuntimeConfigBuilder.java` — 输出子表保存模式配置。

**验收标准**:
- 子表更新支持新增、修改、删除单行，不强制全量删除重插。
- 服务端校验必填、数值范围和字段存在性。

---

## Phase 3：流程与触发器融合

### Task 8: 流程回调绑定动作

**目标**: 流程通过、驳回、取消后可以执行配置动作编码。

**涉及文件**:
- `BusinessFlowService.java` — 流程结果处理后调用动作引擎。
- `BusinessFlowBindingPanel.vue` — 增加通过/驳回/取消动作选择。
- `BusinessFlowAppConfigPanel.vue` — 展示回调动作状态。

**验收标准**:
- 审批通过后能执行通用动作。
- 动作失败写日志并在流程运行态提示业务回调异常。

### Task 9: 触发器动作迁移到动作引擎

**目标**: 新触发器统一配置动作编码，旧触发器配置兼容执行。

**涉及文件**:
- `BusinessTriggerExecutor.java` — 新配置优先调用 `BusinessActionExecutionService`。
- `TriggerActionConfigPanel.vue` — 触发器动作配置支持选择动作编码。
- `trigger.vue` — 场景模板保留，但落到动作编码。

**验收标准**:
- 旧 `CREATE_RECORD`、`UPDATE_FIELD` 触发器仍能执行。
- 新触发器可以复用对象动作。

---

## Phase 4：领域动作 SPI

### Task 10: 领域动作 SPI

**目标**: 为库存、资金、合同状态等高风险领域动作提供可插拔接口。

**涉及文件**:
- `BusinessDomainActionExecutor.java` — 新增 SPI。
- `BusinessDomainActionRegistry.java` — 新增领域动作注册中心。
- `DomainActionStepExecutor.java` — 动作步骤接入领域动作。

**关键签名**:
```java
public interface BusinessDomainActionExecutor {
    String actionType();
    BusinessActionStepResult execute(BusinessActionExecutionContext context, Map<String, Object> config);
}
```

**验收标准**:
- [x] 未注册领域动作返回明确异常。
- [x] 领域动作执行结果进入动作日志。

---

## Phase 5：通用数量台账

### Task 11: 通用数量台账数据模型

**目标**: 建立通用数量余额、流水和锁定表，不使用采购仓储命名。

**涉及文件**:
- `V1.0.xx__add_business_quantity_ledger.sql`
- `AiBusinessQuantityBalance.java`
- `AiBusinessQuantityLedger.java`
- `AiBusinessQuantityLock.java`
- 对应 Mapper 和 XML。

**验收标准**:
- [x] 唯一索引能防止同一维度重复余额。
- [x] 流水含 `idempotency_key` 唯一约束。

### Task 12: 通用数量台账服务

**目标**: 实现入账、锁定、释放、扣减、转移、幂等和防负数。

**涉及文件**:
- `BusinessQuantityLedgerService.java`
- `BusinessQuantityOperationDTO.java`
- `BusinessQuantityOperationResultVO.java`

**关键签名**:
```java
public BusinessQuantityOperationResultVO inbound(BusinessQuantityOperationDTO dto);
public BusinessQuantityOperationResultVO lock(BusinessQuantityOperationDTO dto);
public BusinessQuantityOperationResultVO release(BusinessQuantityOperationDTO dto);
public BusinessQuantityOperationResultVO commit(BusinessQuantityOperationDTO dto);
public BusinessQuantityOperationResultVO transfer(BusinessQuantityOperationDTO dto);
```

**验收标准**:
- [x] 重复幂等键不会重复入账或扣减。
- [x] 可用数量不足时锁定失败。
- [x] 转移动作源端扣减和目标端入账同事务。

### Task 13: 数量台账接入动作步骤

**目标**: 动作引擎支持通过 `DOMAIN_ACTION` 调用数量台账。

**涉及文件**:
- `BusinessQuantityDomainActionExecutor.java`
- `BusinessActionDesigner.vue`
- `BusinessActionExecutionService.java`

**验收标准**:
- [x] 动作步骤可从当前记录和表单参数路径读取数量项；子表行级路径可通过动作步骤参数显式映射。
- [x] 采购仓储可作为验收样例配置“审批通过后入账”“提交后锁定”“驳回释放”“通过扣减/转移”，但平台代码不包含采购仓储对象名。

---

## Phase 6：提醒增强

### Task 14: 分层提醒规则增强

**目标**: 在现有定时触发器基础上支持金额/数量/日期分层提醒。

**涉及文件**:
- `BusinessTriggerSchedulerService.java`
- `BusinessTriggerExecutor.java`
- `trigger.vue`

**验收标准**:
- [x] 可按数值区间选择提前天数和提醒对象。
- [x] 同一触发器同一记录同一天去重；同一记录多规则并发提醒仍按当前触发器日志粒度合并。

---

## Phase 7：验证归档

### Task 15: 自动化测试和构建验证

**目标**: 按 `test-spec.md` 执行增量验证。

**涉及文件**:
- `code-copilot/changes/lowcode-platform-capability-foundation/test-spec.md`
- `code-copilot/changes/lowcode-platform-capability-foundation/execution-log.md`

**验收标准**:
- [x] 已新增数量台账核心服务单测。
- [x] 后端编译通过；根 POM 固定 skip 已补 `enable-tests` profile，本轮定向单测已实际执行。
- [x] 前端构建通过。
- [x] 执行日志记录命令、结果、警告和跳过项。

### Task 16: SDD 日志、审查和归档准备

**目标**: 进入 review 前补齐 SDD 记录。

**涉及文件**:
- `spec.md`
- `tasks.md`
- `execution-log.md`

**验收标准**:
- [x] 所有已完成任务状态回填。
- [x] 风险和遗留项明确。
- [x] `/review lowcode-platform-capability-foundation` 审查问题已修复并记录验证结果。

---

## Review 修复

### Review Fix: 审查问题修复与回归验证

**目标**: 修复审查发现的动作幂等并发、选择器权限/字段泄漏、数量台账幂等和测试未实际执行问题。

**涉及文件**:
- `BusinessActionExecutionService.java`、`BusinessActionExecutionLogMapper.java`、`BusinessActionExecutionLogMapper.xml`、`V1.0.87__add_business_action_execution_log.sql`
- `BusinessRecordSelectorService.java`
- `BusinessQuantityLedgerService.java`、`BusinessQuantityDomainActionExecutor.java`、`BusinessQuantityOperationResultVO.java`
- `BusinessTriggerExecutor.java`
- `forge-server/pom.xml`
- `BusinessActionExecutionServiceTest.java`、`BusinessRecordSelectorServiceTest.java`、`BusinessQuantityLedgerServiceTest.java`

**验收标准**:
- [x] 动作相同幂等键并发执行时先写 `RUNNING` 预占日志，重复请求不会执行步骤。
- [x] 选择器必须具备目标对象 `VIEW` 权限，响应不暴露 `configKey`，`_raw` 只保留选择器允许字段。
- [x] 数量台账强制稳定幂等键；转移流水拆分源端和目标端两条记录。
- [x] Maven 定向测试通过，8 个测试全部实际执行。
- [x] SQL placeholder、历史方法名和格式静态检查通过。
