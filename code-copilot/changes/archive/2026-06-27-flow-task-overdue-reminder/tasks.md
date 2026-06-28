# 任务拆分 — 流程任务逾期提醒
> 拆分顺序：数据模型 → BPMN 配置协议 → 后端扫描发送 → 前端配置入口 → 测试验证
> 每个任务尽量控制在 3-5 个核心文件内，避免把流程设计器、消息模块和扫描服务混在一个提交里。

## 前置条件

- [x] 已确认审批节点当前配置入口为 `ApproverConfig.vue`。
- [x] 已确认 BPMN UserTask parser/writer 已支持 `flowable:dueDate`。
- [x] 已确认消息模块支持 `templateCode`、`params`、`channel` 和 `sendIfAbsent`。
- [x] 已确认现有 `FlowTimeoutServiceImpl` 定时扫描未启用且通知发送为 TODO。
- [x] 进入 `/apply` 前确认迁移版本号，当前使用 `V1.0.74`。

## Task 1: 数据迁移与实体模型

- **状态**: 已完成
- **目标**: 为逾期提醒记录、旧节点配置 fallback、默认模板和字典补齐数据库基础。
- **涉及文件**:
  - `forge-server/db/migration/V1.0.74__add_flow_task_overdue_reminder.sql` — 新增提醒记录表、扩展 `sys_flow_node_config`、写入字典、默认模板和消息业务类型。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowOverdueReminderRecord.java` — 新增提醒记录实体。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowNodeConfig.java` — 增加逾期提醒 fallback 字段。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/mapper/FlowOverdueReminderRecordMapper.java` — 新增 Mapper。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowOverdueReminderRecordMapper.xml` — 新增按唯一键查询/更新发送结果 SQL。
- **关键签名**:
  ```java
  public class FlowOverdueReminderRecord {
      private String id;
      private Long tenantId;
      private String taskId;
      private String processInstanceId;
      private String processDefKey;
      private String taskDefKey;
      private String reminderKey;
      private String channel;
      private String templateCode;
      private String receiverUserIds;
      private Long messageId;
      private Integer sendStatus;
      private LocalDateTime sendTime;
      private String errorMessage;
  }
  ```
- **验收标准**:
  - SQL 具备 `IF NOT EXISTS` / `information_schema` 防重复保护。
  - 所有新增内置数据 `tenant_id=1`。
  - 提醒记录表唯一键能约束同一 `tenant_id + reminder_key + channel`。

## Task 2: BPMN 逾期提醒配置协议

- **状态**: 已完成
- **目标**: 让审批节点逾期提醒配置能随流程 XML 保存、导入和回显。
- **涉及文件**:
  - `forge-admin-ui/src/components/flow-designer/constants/default-configs.js` — 审批节点默认配置新增逾期提醒字段。
  - `forge-admin-ui/src/components/flow-designer/converter/user-task-parser.js` — 解析 `flowable:overdueReminder*` 属性。
  - `forge-admin-ui/src/components/flow-designer/converter/user-task-writer.js` — 写出 `flowable:overdueReminder*` 属性，并兼容天/小时处理时限。
  - `forge-admin-ui/src/components/flow-designer/converter/__tests__/user-task-parser-permissions.spec.js` — 增加逾期提醒解析测试。
  - `forge-admin-ui/src/components/flow-designer/converter/__tests__/json-to-bpmn.spec.js` — 增加逾期提醒写出测试。
- **关键签名**:
  ```js
  const overdueReminderDefaults = {
    dueDateDays: 0,
    dueDateHours: 0,
    overdueReminderEnabled: false,
    overdueReminderTemplateCode: 'FLOW_TASK_OVERDUE',
    overdueReminderChannels: ['WEB'],
    overdueReminderRepeatMode: 'once',
    overdueReminderIntervalMinutes: 1440,
    overdueReminderMaxTimes: 1,
  }
  ```
- **BPMN 属性约定**:
  ```xml
  flowable:dueDate="P1DT2H"
  flowable:overdueReminderEnabled="true"
  flowable:overdueReminderTemplateCode="FLOW_TASK_OVERDUE"
  flowable:overdueReminderChannels="WEB,EMAIL"
  flowable:overdueReminderRepeatMode="once"
  flowable:overdueReminderIntervalMinutes="1440"
  flowable:overdueReminderMaxTimes="1"
  ```
- **验收标准**:
  - XML 导入后配置能完整回显到审批节点配置。
  - JSON 保存后导出的 BPMN XML 包含逾期提醒扩展属性。
  - 未配置逾期提醒的历史流程保持默认不提醒。

## Task 3: 后端逾期任务查询与配置解析

- **状态**: 已完成
- **目标**: 从 `sys_flow_task` 找到逾期待办任务，并读取 BPMN 或旧节点配置中的提醒规则。
- **涉及文件**:
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/mapper/FlowTaskMapper.java` — 新增逾期任务查询方法。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowTaskMapper.xml` — 新增 `selectOverduePendingTasks` XML SQL。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/dto/FlowOverdueReminderConfig.java` — 新增运行时提醒配置 DTO。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowOverdueReminderConfigResolver.java` — 新增 BPMN/旧配置解析器。
- **关键签名**:
  ```java
  IPage<FlowTask> selectOverduePendingTasks(Page<FlowTask> page,
                                            @Param("now") LocalDateTime now);

  public FlowOverdueReminderConfig resolve(FlowTask task);
  ```
- **验收标准**:
  - 查询只返回 `status IN (0, 1)` 且 `due_date <= now` 的任务。
  - 配置解析优先 BPMN UserTask 属性，旧 `sys_flow_node_config` 作为 fallback。
  - 未启用提醒、无模板、无渠道或无截止时间时跳过。

## Task 4: 审批人解析与消息发送

- **状态**: 已完成
- **目标**: 复用消息模块对逾期任务审批人推送模板消息，并保证幂等。
- **涉及文件**:
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/FlowTaskReceiverResolver.java` — 新增任务接收人解析服务。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskReceiverResolverImpl.java` — 从 assignee/candidateUsers/candidateGroups 解析用户。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java` — 改用接收人解析服务发送任务创建站内信，减少重复逻辑。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowOverdueReminderServiceImpl.java` — 新增消息构造、发送、记录更新。
- **关键签名**:
  ```java
  Set<Long> resolveReceivers(FlowTask task);

  public void sendOverdueReminder(FlowTask task, FlowOverdueReminderConfig config);
  ```
- **消息参数**:
  ```java
  Map.of(
      "taskId", task.getTaskId(),
      "taskName", task.getTaskName(),
      "taskTitle", task.getTitle(),
      "processName", task.getProcessName(),
      "startUserName", task.getStartUserName(),
      "dueDate", formatDateTime(task.getDueDate()),
      "overdueMinutes", overdueMinutes,
      "jumpUrl", "/flow/todo?taskId=" + task.getTaskId()
  )
  ```
- **验收标准**:
  - 已签收任务提醒 `assignee`。
  - 未签收任务提醒候选用户；候选用户为空时尝试候选组展开。
  - 多渠道配置按渠道分别调用消息模块，失败只记录不阻断扫描。
  - 同一任务同一提醒 key 同一渠道不会重复发送。

## Task 5: 定时扫描与补偿接口

- **状态**: 已完成
- **目标**: 启用逾期提醒扫描，并提供可选手动补偿入口。
- **涉及文件**:
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/FlowOverdueReminderService.java` — 新增扫描服务接口。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowOverdueReminderServiceImpl.java` — 实现分页扫描、重复策略、最大次数判断。
  - `forge-server/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowTaskController.java` — 可选新增手动扫描接口。
  - `forge-server/forge-flow/forge-flow-server/src/main/resources/application.yml` — 增加扫描开关、间隔和每批数量默认值。
- **关键签名**:
  ```java
  void scanAndSendOverdueReminders();

  @PostMapping("/overdue-reminder/scan")
  public RespInfo<Void> scanOverdueReminders()
  ```
- **配置建议**:
  ```yaml
  forge:
    flow:
      overdue-reminder:
        enabled: true
        scan-interval-ms: 300000
        batch-size: 200
  ```
- **验收标准**:
  - 默认每 5 分钟扫描一次。
  - 扫描按分页执行，避免一次性加载全部逾期任务。
  - 服务关闭配置时不扫描。
  - 手动扫描接口需要权限控制，不对普通用户开放。

## Task 6: 前端逾期提醒配置页签

- **状态**: 已完成
- **目标**: 在审批节点配置抽屉中提供可用的逾期提醒配置体验。
- **涉及文件**:
  - `forge-admin-ui/src/components/flow-designer/panel/OverdueReminderConfig.vue` — 新增逾期提醒配置组件。
  - `forge-admin-ui/src/components/flow-designer/panel/ApproverConfig.vue` — 增加“逾期提醒”页签并接入组件。
  - `forge-admin-ui/src/api/message.js` — 复用或补充模板查询参数方法。
  - `forge-admin-ui/src/components/flow-designer/panel/__tests__/OverdueReminderConfig.spec.js` — 新增组件测试。
- **界面字段**:
  - 启用逾期提醒：`NSwitch`
  - 处理时限：天/小时 `NInputNumber`
  - 消息模板：`NSelect` 远程加载消息模板
  - 推送渠道：`NSelect multiple`，选项来自 `useDict('sys_message_channel')`
  - 重复策略：`DictSelect` 或字典驱动的 `NSelect`
  - 提醒间隔和最大次数：仅重复策略为按间隔重复时显示
- **验收标准**:
  - 字典异步加载后渠道选项响应式更新。
  - 模板选择只展示启用模板。
  - 未启用提醒时不强制填写模板和渠道。
  - 启用提醒时校验处理时限、模板和渠道。
  - 组件在 520px 抽屉宽度下不横向溢出。

## Task 7: 监控与记录查询（可选增强）

- **状态**: 暂缓（可选增强，本轮未实现）
- **目标**: 支持在流程监控或任务详情中查看逾期提醒发送记录，方便排查。
- **涉及文件**:
  - `forge-server/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowTaskController.java` — 新增任务提醒记录查询接口。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowOverdueReminderRecordMapper.xml` — 新增按 `taskId` 查询记录 SQL。
  - `forge-admin-ui/src/api/flow.js` — 新增查询提醒记录 API。
  - `forge-admin-ui/src/views/flow/monitor.vue` 或 `forge-admin-ui/src/views/flow/todo.vue` — 任务详情中展示提醒记录。
- **关键签名**:
  ```java
  List<FlowOverdueReminderRecord> selectByTaskId(@Param("taskId") String taskId);
  ```
- **验收标准**:
  - 能看到发送渠道、模板、接收人、发送状态、失败原因和发送时间。
  - 查询接口分页参数命名使用 `pageNum/pageSize`。

## Task 8: 验证与日志

- **状态**: 已完成
- **目标**: 按自动化测试标准完成增量验证并记录。
- **涉及文件**:
  - `code-copilot/changes/flow-task-overdue-reminder/execution-log.md` — 记录命令、结果、警告、跳过项和服务清理情况。
  - `code-copilot/changes/flow-task-overdue-reminder/test-spec.md` — 若进入 `/test` 或测试范围扩大，再补独立测试说明。
- **验证命令**:
  ```bash
  cd forge-admin-ui
  source ~/.nvm/nvm.sh && nvm use v20.19.0
  pnpm vitest run src/components/flow-designer
  pnpm build
  ```
  ```bash
  cd forge-server
  mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am test
  mvn clean compile -DskipTests
  ```
- **验收标准**:
  - 前端逾期提醒配置和 BPMN 往返测试通过。
  - 后端逾期扫描、接收人解析和幂等发送单测通过。
  - 编译通过；若因本地数据库/Redis 不可用跳过集成验证，必须在 `execution-log.md` 标注。
