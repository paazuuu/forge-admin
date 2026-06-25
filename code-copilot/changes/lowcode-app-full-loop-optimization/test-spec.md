# 测试计划：lowcode-app-full-loop-optimization

> created: 2026-06-02
> scope: Phase 9 增量验证，复用 `spec.md`、`tasks.md`、`execution-log.md` 的既有验证基线。

## 1. 既有验证基线

- Phase 8 已完成后端主应用 package、前端 build、Flyway/dev 库、商机流程发起和核心接口验证。
- Phase 9 Task 23-25 已完成插件模块编译、前端 build 和 `git diff --check`。
- Phase 9 Task 26-29 已完成插件模块编译、前端 build 和 `git diff --check`。

## 2. 本轮增量范围：Task 30-33

- Task 30：流程绑定面板与触发器 `START_FLOW` 动作复用变量映射编辑器和标题模板编辑器。
- Task 31：单据运行态返回自动“发起流程”按钮，`AiCrudPage` 渲染并调用 `/ai/business/flow/start`。
- Task 32：触发器新增前置业务对象选择、对象上下文 query 默认值、保存前阻断空对象。
- Task 33：对象设计器单据闭环步骤条、发布检查和 readiness 增加入口/流程/触发器缺口项。

## 3. 必跑验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 后端插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 构建通过，无新增阻断错误 |
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |

## 4. 条件增强验证

- 如本地后端、Flow 服务和 dev 数据库已可用，补充验证：
  - `GET /ai/business/document/{objectCode}/{recordId}/runtime` 返回 `runtimeActions`。
  - `POST /ai/business/flow/start` 可由运行态按钮 payload `{objectCode, recordId}` 发起。
  - `GET /ai/business/flow/model/{modelKey}/variables?objectCode=...` 返回候选变量和映射建议。
  - `GET /ai/business/trigger/page?pageNum=1&pageSize=10&objectCode=...` 可按对象进入触发器页。
- 如前端 dev server 可用，补充浏览器验证：
  - 主流程配置页变量下拉、推荐映射和标题模板预览。
  - 触发器新增未选对象时保存按钮阻断。
  - 对象设计器步骤条能跳转到触发器配置。

## 5. 本轮跳过标准

- 未启动后端、Flow 服务、前端 dev server 或数据库时，接口和浏览器点击验证记录为跳过，不写作通过。
- 前端构建中的既有 UnoCSS 图标加载、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，如未新增阻断，记录为非阻断警告。

## 6. Phase 10 Task 34 增量验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 后端插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 构建通过，无新增阻断错误 |
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P1 | 任务中心代码审查 | 检查 `BusinessTriggerSchedulerService`、`ScheduleConfig` | 扫描入口使用 `@ScheduledJob` 注册到任务中心，Quartz 显式开启集群配置，Redisson 锁作为兜底 |

条件增强验证：

- 后端和数据库可用时，新增或启用 `triggerType=SCHEDULE`、`eventType=SCHEDULED_DUE`、`eventCondition.schedule.dueField` 的触发器，确认扫描器按到期字段小批量读取记录。
- 后端启动后检查 `sys_job_config`：存在 `job_name=lowcodeBusinessTriggerScanJob`、`job_group=LOWCODE`、`cron_expression=0 0/5 * * * ?`。
- 检查 `ai_business_trigger_log`：同一 `trigger_id + record_id + SCHEDULED_DUE` 当天只出现一次 `SUCCESS/TODO` 结果。
- 验证未配置 `dueField` 的定时触发器只输出跳过日志，不读取业务表。
- 多实例环境可用时，同时启动两个 admin 实例，确认 Quartz JDBC 集群只触发一个节点执行 `LOWCODE.lowcodeBusinessTriggerScanJob`。
- Redis/Redisson 可用时，确认手动并发触发时只有一个执行方抢到 `forge:business-trigger:schedule:scan`；Redis/Redisson 不可用时，确认日志说明仅依赖任务中心集群调度和日志去重。

## 7. Phase 10 BUG 修正增量验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

本轮聚焦编号规则格式、应用入口文案、单据填报 form-only、发布 loading、发起主流程按钮和触发器动作配置简化。未启动后端、Flow 服务、前端 dev server 或数据库时，接口联调和浏览器点击验证记录为跳过。

## 8. Phase 10 BUG 用户反馈跟进验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

本轮聚焦对象类型与入口打开方式关系说明、父级菜单回填、自定义操作发布到运行态列表、单据设置说明文案和操作列兼容识别。未启动后端、Flow 服务、前端 dev server 或数据库时，接口联调和浏览器点击验证记录为跳过。

## 9. Phase 10 BUG 非单据对象发起主流程验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |

条件增强验证：

- 对未启用单据模式但已发布运行配置、已配置主流程绑定的对象，调用 `POST /ai/business/flow/start`，期望不再返回“业务对象未启用单据模式”，并写入流程实例关联。
- 对启用单据模式的对象，仍验证状态字段、状态映射、权限和重复运行流程的限制。
- 对非单据对象流程回调，期望更新流程实例状态；如存在运行配置，继续发布流程结果业务事件。

## 10. Phase 10 BUG 发布检查与回显一致性验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

本轮聚焦应用入口发布检查误报、菜单父级回显、菜单资源 ID 精度、主流程绑定读取优先级和旧审批兼容运行态。未启动后端、Flow 服务、前端 dev server 或数据库时，菜单同步落库、发布检查接口和浏览器回显验证记录为跳过。

## 11. Phase 10 BUG 运行态按钮、主流程诊断和套件目录回显验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

本轮聚焦 `/ai/crud-page` 头部重复“新增”按钮、主流程发起失败日志诊断和“套件作为父级目录”后的实际挂载目录回显。未启动后端、Flow 服务、前端 dev server 或数据库时，真实流程发起日志、菜单同步落库和浏览器回显验证记录为跳过。

## 12. Phase 10 BUG 主流程对象标识归一和入口菜单持久化验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | Mapper XML 语法检查 | `xmllint --noout --nonet forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudConfigMapper.xml forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessDocumentConfigMapper.xml forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessObjectMapper.xml forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessAppMapper.xml` | XML 语法通过 |
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端主应用编译 | `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

本轮聚焦前端发起流程传入 `configKey` 时，后端应归一到业务对象 `objectCode` 再查主流程绑定；应用入口勾选“套件作为父级目录”后，应保留菜单资源 ID、实际父级目录和套件目录 ID，编辑入口时稳定回显。未启动后端、Flow 服务、前端 dev server 或数据库时，真实流程发起、菜单同步落库和浏览器回显验证记录为跳过。

## 13. Phase 10 BUG 动态页发起流程和详情流程进度验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端主应用编译 | `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

本轮聚焦动态页“发起主流程”的确认弹窗、发起中 loading、`sys_flow_task.assignee` 用户 ID 归一、单据详情流程时间轴/流程图配置和详情默认弹窗 + Tab 展示。未启动后端、Flow 服务、前端 dev server 或数据库时，真实流程发起落库和浏览器点击验证记录为跳过。

## 14. Phase 10 BUG 详情流程配置对象标识归一验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

本轮聚焦动态详情运行态接口收到 `configKey` 或历史 `ai_crud_config.object_code` 时，应归一到标准业务对象 `objectCode` 读取 `ai_business_document_config.options` 中的流程时间轴/流程图开关，并用标准 `objectCode:recordId` 查询流程实例关联。未启动后端、Flow 服务、前端 dev server 或数据库时，真实详情接口和浏览器流程图显示验证记录为跳过。

## 15. Phase 10 BUG 发起流程重复实例幂等验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

条件增强验证：

- 前端 dev server 可用时，在动态页点击一次“发起主流程”，确认只弹出一个内置确认框，确认后按钮进入“发起中...”并禁用，不再触发外层 `custom-action` 重复发起。
- 后端、Flow 服务和数据库可用时，对同一 `{objectCode, recordId}` 连续或并发调用两次 `POST /ai/business/flow/start`，期望只创建一个运行中 `ai_business_flow_instance_link` 和一个 Flowable 流程实例；第二次返回“当前单据已有流转中的流程”或重复提交提示。
- 检查 `sys_flow_task`，同一业务单据本轮发起后只生成一条待办链路，`assignee` 继续保存用户 ID。
- Redis/Redisson 可用时，确认同一业务单据的流程发起锁使用 `forge:business-flow:start:{tenantId}:{businessKey}` 串行化；Redisson 不可用时，本地单实例使用内存锁兜底。

本轮聚焦动态页 START_FLOW 事件双路径和后端流程发起竞态。未启动后端、Flow 服务、前端 dev server 或数据库时，真实并发发起和落库检查记录为跳过。

## 16. Phase 10 BUG Flow 服务 businessKey 唯一键幂等验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | Mapper XML 语法检查 | `xmllint --noout --nonet forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowBusinessMapper.xml` | XML 语法通过 |
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | Flow 服务编译 | `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-flow/forge-flow-server -am compile -DskipTests` | `BUILD SUCCESS` |

条件增强验证：

- Flow 服务和数据库可用时，对已存在运行中 `sys_flow_business(tenant_id=1,business_key=LEAVE_APPLICATION:5)` 的业务 Key 再调用 `POST /api/flow/instance/start/leave_multi`，期望返回已有 `processInstanceId`，不再抛 `DuplicateKeyException`。
- 对同一业务 Key 并发调用 Flow 服务启动接口，期望同一 JVM 内通过本地锁串行化；跨实例并发时唯一键冲突被转换为已有实例复用或“流程正在发起，请稍后重试”的业务提示。
- 对已结束状态 `approved/rejected/canceled/terminated/completed` 的业务 Key 重复发起，期望返回“业务流程已存在且不可重复发起”，不把已结束实例误当新流程。
- 检查 `sys_flow_business.tenant_id` 新插入时显式写入当前租户或默认租户 `1`，查询按 `tenant_id + business_key` 匹配。

本轮聚焦 Flow 服务 `sys_flow_business.uk_flow_business_tenant_key` 唯一键冲突。未启动 Flow 服务和数据库时，真实接口重试与并发落库验证记录为跳过。

## 17. Phase 10 BUG 同一流程实例重复待办验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | BPMN 重复连线 smoke 验证 | `javac -cp forge/forge-framework/forge-plugin-parent/forge-plugin-flow/target/classes -d /private/tmp /private/tmp/BpmnXmlUtilsSmoke.java && java -cp forge/forge-framework/forge-plugin-parent/forge-plugin-flow/target/classes:/private/tmp BpmnXmlUtilsSmoke` | 旧重复 `flow1/flow2/flow3` 被移除，保留设计器引用的 `Flow_*` 连线 |
| P0 | Flow 插件编译 | `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | Flow 服务编译 | `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-flow/forge-flow-server -am compile -DskipTests` | `BUILD SUCCESS` |

条件增强验证：

- 对已存在重复 `sequenceFlow(sourceRef,targetRef)` 的流程模型保存草稿、导入、复制、部署和版本回退，期望后端自动清理语义完全相同的重复连线，并在日志输出保留/删除的连线 ID。
- 部署修复后的 `leave_multi` 后，重新发起 `LEAVE_APPLICATION:5`，期望 Flowable 只创建一个 `deptApprove` 活动任务。
- 对已运行的坏流程实例，确认需要终止/清理后重新发起；新部署不会改变已经创建的运行时 token 和待办。

本轮聚焦同一个 `processInstanceId` 下生成两条不同 `task_id` 的待办。该现象来自 BPMN 定义中存在重复语义连线，Flowable 会对每条出线创建执行路径；不是 `sys_flow_task` 监听器重复 insert，也不是前端重复调用。

## 18. Phase 10 BUG 单据设置体验、状态映射和编号规则落地验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

条件增强验证：

- 前端 dev server 可用时，进入对象设计器单据设置，确认布局收敛为基础配置、编号生成、状态映射和详情页流程展示区域，状态映射值只能从状态字段字典/选项选择。
- 后端和数据库可用时，保存启用单据模式、编号规则和编号字段后，通过动态新增接口创建记录，期望编号字段由后端生成并写入，客户端传入编号被覆盖。
- 打开动态新增/编辑表单时，期望编号字段只读或禁用，表单行间距较旧版本收紧，用户选择/组织选择清除按钮垂直居中。

本轮聚焦单据设置页面视觉结构、动态表单间距、用户/组织选择组件清除按钮对齐、状态映射候选值约束，以及单据编号规则在运行态新增链路自动写入。未启动后端服务、前端 dev server 或数据库时，真实页面点击和动态新增落库验证记录为跳过。

## 19. Phase 10 BUG 应用入口套件菜单自父级验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端主应用编译 | `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

条件增强验证：

- 后端和数据库可用时，编辑已勾选“同步为菜单 + 套件作为父级目录”的应用入口并保存，检查套件目录 `sys_resource.parent_id` 仍为原上级或 `0`，不得等于套件目录自己的 `id`。
- 对已经污染为自父级的入口配置再次保存，期望后端把 `adminMenu.parentId/originalParentId` 归一为空，并把套件目录重新挂载到顶级。
- 前端 dev server 可用时，重新打开应用入口编辑抽屉，期望“套件目录上级”不回显“实际挂载目录”自身，“实际挂载目录”仅作为只读展示。

本轮聚焦应用入口保存时把套件目录自身 ID 写回 `originalParentId/parentId`，导致 `sys_resource.parent_id` 变成自身 ID。未启动后端服务、前端 dev server 或数据库时，真实菜单落库和浏览器回显验证记录为跳过。

## 20. Phase 10 BUG 表单/列表设计器和运行态交互验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

条件增强验证：

- 前端 dev server 可用时，在列表设计器结构化/自由布局两种模式下设置“全部列对齐”和“行间距”，期望所有已选列表字段同步为该对齐方式，预览表格行高变化，保存后运行态列表读取 `tableRowGap`。
- 在表单设计器基础配置中选择已有字段作为组件字段 ID，期望保存后 `fieldBinding.fieldCode` 指向已存在字段，不再创建随机 `Fr...` 字段。

## 21. 本轮增量验证：低代码应用列表查询与基础表格默认居中

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | Mapper XML 语法检查 | `xmllint --noout --nonet forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudConfigMapper.xml` | XML 语法通过 |
| P0 | Flyway 索引脚本检查 | `sed -n '1,120p' forge-server/db/migration/V1.0.78__add_lowcode_app_page_summary_index.sql` | 幂等 SQL 脚本完整，无重复创建风险 |
| P0 | 后端插件编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `cd forge-admin-ui && source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 构建通过，无新增阻断错误 |

本轮聚焦低代码应用分页只返回摘要字段、关键词搜索防抖、初始加载并行、分页主路径索引，以及 `AiTable` 默认列对齐改为居中。未启动后端服务、前端 dev server 或数据库时，接口和浏览器点击验证记录为跳过。
- 在新增组件后不点保存，直接切换到单据设置并点保存，期望前端先静默保存表单设计器草稿和自动创建字段，不再触发“页面区域引用了不存在的字段”。
- 动态页点击“发起主流程”后，期望 loading 显示在当前页面遮罩上，行操作按钮只禁用不改成按钮 loading 文案。
- `/app-center/object` 对象入口页导入模板/导入/导出只依赖运行配置 `configKey`，不再被 `canOpen` 阻断。

本轮聚焦表单设计器字段绑定、未保存字段引用、列表字段批量样式、运行态流程 loading 和对象入口导入导出启用判断。未启动后端服务、前端 dev server 或数据库时，真实页面点击、单据配置保存和导入导出接口验证记录为跳过。

## 21. Phase 10 BUG 表单字段 ID 选择保存和字段资产名称保护验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

条件增强验证：

- 前端 dev server 可用时，在表单设计器选中组件，在“组件字段ID”里选择已有字段并保存；切换到其他面板再切回表单设计，期望该组件仍绑定选择的字段。
- 保存后进入“字段资产”，期望字段名称、字段编码、列名继续来自原字段资产，不得变成“未命名字段”。
- 对旧的 form-create 规则做兼容：如果 `rule.field/name` 与 `props.fieldBinding.fieldCode` 不一致，保存时应选择用户刚改的字段，而不是旧 `_forge.fieldBinding`。

本轮聚焦 form-create 属性面板字段选择值、Forge 表单 Schema 转换、自动字段资产基准和父层字段 payload 兜底。未启动后端服务、数据库或执行真实浏览器点击时，真实对象保存回显验证记录为跳过。

## 22. Phase 10 BUG 对象设计保存后入口菜单同步和字段资产保存验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | Mapper XML 语法检查 | `xmllint --noout --nonet forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessAppMapper.xml` | XML 语法通过 |
| P0 | 后端插件编译 | `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

条件增强验证：

- 后端和数据库可用时，从对象设计器保存已有关联运行态入口的业务对象，期望 `ai_business_app.config_key/options.adminMenu.path/menuResourceId/actualParentId/suiteMenuResourceId` 被同步刷新，管理端菜单仍能渲染到原挂载目录。
- 前端 dev server 可用时，保存对象设计后返回应用入口，打开入口编辑抽屉，期望“实际挂载目录”继续显示已生成套件目录，“套件目录上级”不被实际目录 ID 污染。
- 在字段资产面板修改字段属性并触发全局保存，期望打开属性面板时保存当前字段；未打开属性面板且无选中字段时不再提示“请先选择需要保存的字段”，整体保存流程继续往下执行。

本轮聚焦对象设计器保存后依赖入口菜单没有重新同步，以及字段资产全局保存误要求选中字段。未启动后端服务、数据库或前端 dev server 时，真实菜单落库、侧边栏渲染和浏览器点击验证记录为跳过。

## 23. Phase 10 BUG 查询条件隐藏旧字段引用清理验证

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | `BUILD SUCCESS` |
| P0 | 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 构建通过，无新增阻断错误 |

条件增强验证：

- 后端和数据库可用时，把业务字段从 `frpjmpzgzlc1hfc` 改为新字段名后保存字段资产，检查 `ai_business_object.designer_options.viewSchema.search.fields[].fieldCode` 不再保留旧字段。
- 对已有脏数据直接执行发布检查，期望 `viewSchema.search/list/detail` 中不存在于模型字段集的历史字段被过滤，不再出现“查询条件引用了不存在字段: frpjmpzgzlc1hfc”的阻断项。
- 前端 dev server 可用时，进入对象设计器保存，期望主设计器 payload 中的 `viewSchema` 会按当前字段资产过滤旧字段；列表自由布局的隐藏 `fieldSettings.queryField` 指向旧字段时会在保存前被清理或回退。

本轮聚焦字段改名/删除后，旧随机字段码残留在 `viewSchema.search.fields[].fieldCode`、列表/查询布局 `fieldSettings.queryField` 等不可见 JSON 配置中，导致用户在查询条件 UI 找不到字段但发布检查仍报错。未启动后端服务、数据库或前端 dev server 时，真实落库和浏览器点击验证记录为跳过。

## 24. 本轮增量验证：动态页面查询重复 DB 优化

| 优先级 | 验证项 | 命令 | 期望 |
|--------|--------|------|------|
| P0 | 补丁空白检查 | `git diff --check -- forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/PermissionServiceImpl.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysResourceServiceImpl.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiCrudConfigService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicDataScopeService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessDocumentConfigService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessDocumentRuntimeService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/impl/GenDatasourceServiceImpl.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/runtime/LowcodeRuntimeDataSourceResolver.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/runtime/RuntimeJdbcTemplateProvider.java` | 无 trailing whitespace / conflict marker |
| P0 | 后端插件编译 | `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -q -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-system -am compile -DskipTests` | 编译通过，退出码 0 |

条件增强验证：

- 后端服务和数据库可用时，登录后连续点击低代码动态页 `/ai/crud/crm_customer/page` 查询，观察日志中同一短时间窗口内 `SysResourceMapper.selectConfiguredApiUrls` 不再每个请求重复查询；资源管理新增/修改/删除 API 权限资源后，缓存应立即失效。
- 查询 `crm_customer` 后触发 `/ai/business/document/crm_customer/runtime/batch`，观察 `GenDatasourceMapper.selectById` 在同一数据源短窗口内不再被运行时解析器和 JDBC provider 反复查询；数据源保存、更新、删除后缓存应立即失效。
- 业务单据运行态批量查询应优先按发布态 `configKey` 查单据配置，并复用已解析的 `AiCrudConfig` 构建 VO，避免 `selectByObjectCode(crm_customer)` 这类必然 miss 和 `toVO` 内二次查询。

本轮聚焦用户反馈“一次查询触发很多次 DB、耗时约 3s”的后端控制面重复查询。未启动后端服务和数据库时，真实请求日志对比记录为跳过；已完成相关模块编译验证。
