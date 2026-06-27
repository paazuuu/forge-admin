# 执行日志 — 流程任务逾期提醒

## 2026-06-21 10:37 CST — apply 阶段验证

### 变更范围

- 后端新增流程任务逾期提醒记录表、BPMN/旧节点配置解析、逾期任务扫描、审批人解析、消息模板发送和手动扫描接口。
- 前端新增审批节点“逾期提醒”页签，支持处理时限、消息模板、推送方式、重复策略配置，并支持 BPMN XML 往返。
- 新增默认 `FLOW_TASK_OVERDUE` 消息模板、消息业务类型和 `sys_flow_overdue_repeat_mode` 字典。

### 已执行命令与结果

```bash
git diff --check
```

结果：通过，无空白错误。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm vitest run \
  src/components/flow-designer/converter/__tests__/user-task-parser-permissions.spec.js \
  src/components/flow-designer/converter/__tests__/json-to-bpmn.spec.js \
  src/components/flow-designer/panel/__tests__/OverdueReminderConfig.spec.js
```

结果：通过，3 个测试文件、24 个测试用例通过。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am compile -DskipTests
```

结果：通过，流程插件和 flow-server 编译成功。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过。构建存在既有非阻断警告：CSS `//` 注释、动态/静态 import chunk 提示、`UserSelectModal` 命名冲突提示。

### 跳过项

- 未启动本地后端服务和数据库实跑 Flyway，原因是本轮为代码实现与编译/构建验证，未要求连接本地 MySQL/Redis。
- 未实现可选 Task 7 的流程监控详情页提醒记录展示。

### 服务清理

- 本轮未启动长期运行服务，无需清理 PID。

## 2026-06-21 — Flyway placeholder 修复

### 变更范围

- 修复 `V1.0.74__add_flow_task_overdue_reminder.sql` 中默认消息模板和业务类型跳转地址的 `${...}` 字符串。
- 使用 `CONCAT('...', '$', '{taskName}')` 形式保留运行时模板变量，避免 Flyway 在迁移解析阶段把 `${taskName}`、`${taskId}` 当成配置占位符。

### 已执行命令与结果

```bash
rg -n '\$\{' forge-server/db/migration/V1.0.74__add_flow_task_overdue_reminder.sql
```

结果：无输出，脚本中不再残留完整 Flyway placeholder 形式。

```bash
git diff --check -- forge-server/db/migration/V1.0.74__add_flow_task_overdue_reminder.sql
```

结果：通过，无空白错误。

## 2026-06-21 — 逾期提醒配置 UI 调整

### 变更范围

- 将审批节点“逾期提醒”从独立页签移动到“扩展配置”页签中，和监听器等高级配置归类到一起。
- 调整“处理时限”的天数/小时布局，改为固定网格的标签、输入框、单位三列结构，避免 `天数 0 小时` 一类展示错位。

### 已执行命令与结果

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm vitest run src/components/flow-designer/panel/__tests__/OverdueReminderConfig.spec.js
```

结果：通过，1 个测试文件、3 个测试用例通过。

```bash
git diff --check -- forge-admin-ui/src/components/flow-designer/panel/ApproverConfig.vue forge-admin-ui/src/components/flow-designer/panel/OverdueReminderConfig.vue
```

结果：通过，无空白错误。

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过。仍存在既有非阻断警告：CSS `//` 注释、`UserSelectModal` 命名冲突、动态/静态 import chunk 提示。
