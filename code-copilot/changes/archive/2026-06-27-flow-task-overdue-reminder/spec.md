# 流程任务逾期提醒
> status: done
> created: 2026-06-21
> complexity: 🟡中等

## 1. 背景与目标

现有流程管理已经支持待办任务、手动催办、任务创建站内信和消息模板，但缺少“审批节点到期后自动提醒当前审批人”的闭环。业务管理员希望在流程设计时为审批节点设置处理时限和逾期提醒策略，并能直接关联消息模块的模板和推送方式。

做完后的效果：

- 流程设计器的审批节点可配置处理时限、是否启用逾期提醒、消息模板、推送渠道和重复提醒策略。
- 任务到达处理时限后，后台定时扫描仍处于待办/已签收状态的任务。
- 系统按当前办理人、候选人或候选组解析审批人，并通过消息模块推送逾期提醒。
- 消息模板变量可引用任务名称、流程名称、发起人、截止时间、逾期时长和待办跳转地址。
- 同一任务、同一提醒批次、同一渠道幂等发送，避免集群扫描或重试导致重复提醒。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- `forge-admin-ui/src/components/flow-designer/panel/ApproverConfig.vue:52` 至 `106` 是审批节点配置入口，目前只有审批人设置、表单权限、审批权限和扩展配置页签，没有逾期提醒页签。
- `forge-admin-ui/src/components/flow-designer/constants/default-configs.js:20` 至 `50` 定义审批节点默认配置，当前只有 `dueDate` 天数字段，没有提醒模板、渠道或重复策略。
- `forge-admin-ui/src/components/flow-designer/converter/user-task-parser.js:190` 至 `203` 已能从 BPMN `flowable:dueDate` 解析天数。
- `forge-admin-ui/src/components/flow-designer/converter/user-task-writer.js:112` 至 `116` 已能把 `dueDate` 写为 `flowable:dueDate="P{N}D"`。
- `forge-admin-ui/src/api/message.js:59` 至 `90` 已提供消息模板分页、详情、新增、更新和删除 API，逾期提醒配置可复用模板分页接口选择模板。
- `forge-admin-ui/src/api/flow.js:103` 至 `106` 目前只提供手动催办接口 `remindTask`。

### 2.2 现有实现

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowTask.java:104` 至 `107` 已有 `dueDate` 字段，可存储 Flowable 任务截止时间。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java:572` 至 `589` 创建 `sys_flow_task` 时会把 Flowable `task.getDueDate()` 写入 `FlowTask.dueDate`。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java:623` 至 `648` 已在任务创建时调用 `MessageService.sendIfAbsent` 发送待办站内信，且能解析处理人、候选人和候选组。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java:1178` 至 `1229` 已有手动催办逻辑，但只支持已签收处理人，未覆盖候选人/候选组，也没有模板和渠道配置。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowNodeConfig.java:99` 至 `123` 已有旧版节点超时字段 `dueDateDays/dueDateHours/timeoutAction/timeoutNotifyUsers`。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTimeoutServiceImpl.java:40` 至 `43` 的定时扫描 `@Scheduled` 处于注释状态。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTimeoutServiceImpl.java:166` 至 `213` 的 `sendTimeoutNotification` 只有 email/sms/system 的 TODO 日志，没有真正调用消息模块。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/java/com/mdframe/forge/plugin/message/domain/dto/MessageSendRequestDTO.java:25` 至 `57` 已支持模板编码、模板参数和单一发送渠道。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/java/com/mdframe/forge/plugin/message/service/impl/MessageServiceImpl.java:117` 至 `141` 会根据 `templateCode` 渲染模板并使用模板默认渠道兜底。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/java/com/mdframe/forge/plugin/message/service/MessageService.java:21` 至 `24` 已有按 `bizType + bizKey` 幂等发送接口，但底层当前只有普通索引，不足以完全抵御集群并发重复。

### 2.3 发现与风险

- 新钉钉式流程设计器主要把节点配置写入 BPMN `flowable:*` 属性；旧 `sys_flow_node_config` 不一定与 BPMN 同步。逾期提醒运行时必须优先读取 BPMN 节点属性，并兼容旧节点配置作为 fallback。
- 现有 `FlowTimeoutServiceImpl` 使用 Flowable 活动任务全量扫描，且定时任务未启用。新增实现应改为从 `sys_flow_task` 按 `due_date <= now` 查询待办任务，SQL 写在 Mapper XML 中。
- 手动催办只处理 `assignee`，逾期提醒必须覆盖候选用户和候选组，否则未签收任务不会提醒审批人。
- 消息模块 `sendIfAbsent` 没有数据库唯一约束，本次需要新增流程侧提醒记录表或唯一键，保证重复扫描不会重复推送。
- 涉及流程任务状态判断，但本次不自动通过、驳回或终结流程，只发送提醒消息，避免引入流程状态流转风险。

## 3. 功能点

- [ ] 审批节点配置新增“逾期提醒”页签，支持启用开关、处理时限、提醒模板、推送渠道、重复提醒策略和提醒次数上限。
- [ ] 推送渠道从 `sys_message_channel` 字典读取，禁止前端硬编码 WEB/SMS/EMAIL/PUSH 选项。
- [ ] 消息模板通过消息模块模板分页接口选择，模板必须启用后才能保存为提醒模板。
- [ ] 审批节点提醒配置随 BPMN XML 保存和加载，支持 XML → JSON → XML 往返。
- [ ] 后端扫描逾期未完成任务，并按 BPMN 节点配置或旧节点配置解析提醒策略。
- [ ] 后端解析当前审批人：优先 `assignee`，其次候选用户，最后候选组展开用户。
- [ ] 逾期提醒调用消息模块模板发送，支持站内信、短信、邮件、推送等现有渠道。
- [ ] 同一任务、提醒批次、渠道幂等发送，并记录发送结果。
- [ ] 内置 `FLOW_TASK_OVERDUE` 消息模板和流程逾期提醒业务类型，作为默认模板。

## 4. 业务规则

- 只有审批节点启用逾期提醒且任务有有效截止时间时才发送。
- 任务状态必须仍为待办或已签收，已通过、驳回、撤回、退回、终结的任务不再提醒。
- 提醒对象是当前可处理该任务的审批人：已签收任务提醒签收人；未签收任务提醒候选用户或候选组解析出的用户。
- 候选组无法解析出用户时记录告警和失败记录，不阻断流程执行。
- 默认只在任务首次逾期后提醒一次；如配置重复提醒，按间隔和最大次数发送。
- 消息 `bizType` 使用 `FLOW_TASK_OVERDUE`，`bizKey` 使用 `taskId:reminderKey:channel`。
- 消息跳转地址固定为 `/flow/todo?taskId=:taskId`。
- 本次不实现 `auto_pass` / `auto_reject`，不改变流程实例状态。
- 所有新增内置数据 `tenant_id=1`。
- 所有新增查询 SQL 写在 Mapper XML 中。

## 5. 数据变更

建议新增 Flyway：`forge-server/db/migration/V1.0.74__add_flow_task_overdue_reminder.sql`。若进入 `/apply` 时仓库已有更高版本，需顺延版本号。

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增 | `sys_flow_overdue_reminder_record` | `task_id`, `process_instance_id`, `process_def_key`, `task_def_key`, `reminder_key`, `channel`, `template_code`, `receiver_user_ids`, `message_id`, `send_status`, `send_time`, `error_message` | 逾期提醒发送记录和幂等控制 |
| 新增索引 | `sys_flow_overdue_reminder_record` | `uk_flow_overdue_reminder(tenant_id, reminder_key, channel)` | 防止同一提醒批次同一渠道重复发送 |
| 扩展 | `sys_flow_node_config` | `overdue_reminder_enabled`, `overdue_reminder_template_code`, `overdue_reminder_channels`, `overdue_reminder_repeat_mode`, `overdue_reminder_interval_minutes`, `overdue_reminder_max_times` | 兼容旧节点配置 API 的 fallback |
| 新增 | `sys_dict_type/sys_dict_data` | `sys_flow_overdue_repeat_mode` | 逾期提醒重复策略字典：仅一次、按间隔重复 |
| 新增 | `sys_message_template` | `FLOW_TASK_OVERDUE` | 默认逾期提醒模板，需 `NOT EXISTS` 防重复 |
| 新增/补充 | `sys_message_biz_type` | `FLOW_TASK_OVERDUE` | 消息中心业务类型和跳转地址 |

所有新增业务表必须包含 `id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`，字符集 `utf8mb4`，引擎 `InnoDB`。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 复用 | `/api/message/template/page` | GET | 审批节点配置页选择启用消息模板 |
| 复用 | `/api/flow/model` | POST/PUT | BPMN XML 中携带审批节点逾期提醒扩展属性 |
| 新增 | `/api/flow/task/overdue-reminder/scan` | POST | 管理员手动触发一次逾期扫描，用于调试和补偿，可选实现 |
| 新增 | `/api/flow/task/overdue-reminder/records` | GET | 查询某任务逾期提醒记录，用于监控详情，可选实现 |

核心自动提醒不依赖新增前端业务接口；主要通过后台定时扫描执行。

## 7. 影响范围

- 流程设计器审批节点配置页。
- BPMN UserTask 解析和写出逻辑。
- 流程任务逾期扫描服务。
- 流程任务 Mapper XML 查询。
- 消息模板、消息业务类型和消息发送记录。
- 数据库迁移脚本。
- 流程监控中逾期任务统计和后续排查链路。

## 8. 风险与关注点

| 风险 | 级别 | 应对措施 |
|------|------|----------|
| 配置保存在 BPMN 但后台读旧节点配置导致不生效 | 🟡中 | 后端优先从 BPMN UserTask 属性读取提醒配置，旧 `sys_flow_node_config` 仅 fallback |
| 集群扫描重复发送消息 | 🟡中 | 新增提醒记录唯一键，发送前先抢占提醒记录 |
| 候选组解析范围过大 | 🟡中 | 复用现有 `FlowOrgIntegrationService`，限制只提醒当前候选组解析出的用户 |
| 短信/邮件渠道配置不可用 | 🟡中 | 消息模块发送失败写记录，不阻断流程；站内信作为默认渠道 |
| 重复提醒过于频繁 | 🟡中 | 默认仅一次；重复提醒必须配置间隔和最大次数，最小间隔建议不低于 30 分钟 |
| 误触发自动审批动作 | 🔴高 | 本次不实现自动通过/自动驳回，只发送消息 |

## 8.5 测试策略

- **测试范围**：
  - 前端逾期提醒配置组件、字典渠道加载、模板选择、保存回显。
  - BPMN parser/writer 对逾期提醒属性的往返。
  - 后端逾期任务 XML 查询、配置解析、审批人解析、幂等记录、消息模板发送。
  - 任务已完成后不发送提醒，候选组解析失败时不阻断流程。
- **覆盖率目标**：覆盖核心分支和幂等逻辑，不要求全量流程引擎端到端。
- **独立 Test Spec**：否，进入 `/apply` 后在当前变更补 `execution-log.md` 记录增量验证；执行 `/test` 时再补独立 `test-spec.md`。

## 9. 待澄清

- 无。默认方案为“逾期后提醒当前审批人”，支持一次提醒和可选重复提醒，不引入自动审批动作。

## 10. 技术决策

- 逾期提醒配置以 BPMN UserTask 扩展属性作为主存储，保持与当前钉钉式流程设计器一致。
- 后端扫描以 `sys_flow_task.due_date` 为主，避免每轮全量查询 Flowable 活动任务。
- 消息发送只通过消息模块 `MessageService`，不在流程模块内直接接短信、邮件或推送 SDK。
- 流程侧新增提醒记录表作为幂等边界，不依赖消息表当前非唯一的 `biz_type/biz_key` 索引。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 1 | 已完成 | `forge-server/db/migration/V1.0.74__add_flow_task_overdue_reminder.sql`、`FlowOverdueReminderRecord.java`、`FlowNodeConfig.java`、`FlowOverdueReminderRecordMapper.java`、`FlowOverdueReminderRecordMapper.xml` | 新增记录表、旧节点 fallback 字段、默认模板、业务类型和重复策略字典 |
| Task 2 | 已完成 | `default-configs.js`、`user-task-parser.js`、`user-task-writer.js`、相关 converter 测试 | BPMN UserTask 支持逾期提醒扩展属性和天/小时处理时限 |
| Task 3 | 已完成 | `FlowTaskMapper.java`、`FlowTaskMapper.xml`、`FlowOverdueReminderConfig.java`、`FlowOverdueReminderConfigResolver.java` | 逾期待办查询和 BPMN 优先、节点配置兜底解析 |
| Task 4 | 已完成 | `FlowTaskReceiverResolver.java`、`FlowTaskReceiverResolverImpl.java`、`FlowTaskEventListener.java`、`FlowOverdueReminderServiceImpl.java` | 解析审批人并通过消息模块幂等发送模板消息 |
| Task 5 | 已完成 | `FlowOverdueReminderService.java`、`FlowOverdueReminderServiceImpl.java`、`FlowTaskController.java`、`application.yml` | 默认 5 分钟扫描，支持手动触发补偿扫描 |
| Task 6 | 已完成 | `OverdueReminderConfig.vue`、`ApproverConfig.vue`、`OverdueReminderConfig.spec.js` | 审批节点配置抽屉新增逾期提醒页签 |
| Task 7 | 未实施 | - | 可选增强，本轮未做监控详情页提醒记录展示 |
| Task 8 | 已完成 | `execution-log.md` | 已记录编译、测试和构建验证证据 |

## 12. 审查结论

待 `/review flow-task-overdue-reminder`。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-06-21
- **确认人**：用户通过 `apply flow-task-overdue-reminder` 进入实现阶段

## 归档记录（HARD-GATE）
- **状态**：done
- **归档时间**：2026-06-27
- **归档人**：yaomd（批量归档）
- **归档路径**：code-copilot/changes/archive/2026-06-27-flow-task-overdue-reminder/
- **判定依据**：任务清单全部完成，execution-log 验证通过（编译/构建/lint 闭环）。
