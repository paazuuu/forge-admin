# Phase 8 执行日志：lowcode-app-full-loop-optimization

> 执行时间：2026-06-02 08:00 CST
> 范围：Phase 8 验证归档，覆盖后端构建、前端构建、Flyway、核心接口、商机主流程和文档回填。

## 1. 代码与迁移补丁

- 修复 `V1.0.51__seed_crm_opportunity_document_flow.sql`、`V1.0.52__seed_leave_document_flow_demo.sql` 中内嵌模板的 Flyway 占位符冲突，将 seed JSON 模板从 `${field}` 改为 `{field}`。
- `BusinessFlowService`、`BusinessTriggerExecutor`、`MessageTemplateEngine` 已兼容 `{field}` 与历史 `${field}` 两种模板变量格式。
- 新增 `V1.0.53__align_flow_template_logic_delete_column.sql`，为 `sys_flow_template` 补齐 `del_flag` 逻辑删除字段，匹配 `FlowTemplate` 实体。
- 新增 `V1.0.54__patch_opportunity_flow_dept_manager_mapping.sql`，为 CRM 商机默认流程绑定和自动发起流程触发器补齐 `createBy -> deptManager` 变量映射。

## 2. Flyway 与流程部署

- dev 库 `forge_schema_history` 已执行到 `1.0.54`，最新脚本 `patch opportunity flow dept manager mapping` 执行成功。
- dev 库原有 `sys_flow_template(template_key=leave_multi)`，但缺少对应 Flowable 模型和已部署流程定义；Phase 8 已通过流程服务创建并部署模型。
- `leave_multi` 模型 ID：`e7d55f0a4087ec5dc0189784a00204ad`。
- `leave_multi` 部署 ID：`dcbda981-5e14-11f1-a0fd-d67ed5f8e875`。
- `act_re_procdef` 已存在 `leave_multi` version `1`。

## 3. 商机主流程验证

- 调用 `/ai/business/flow/start`，请求 `{"objectCode":"OPPORTUNITY","recordId":10}`，返回 `code=200`。
- 流程实例 ID：`602ad5c2-5e15-11f1-a0fd-d67ed5f8e875`。
- 单据流程关联 ID：`2061597550728736769`。
- `crm_opportunity.id=10` 的 `document_status` 已回写为 `IN_PROCESS`。
- `ai_business_flow_instance_link` 已写入 `OPPORTUNITY:10`，流程状态 `RUNNING`。
- `sys_flow_business` 已写入 `OPPORTUNITY:10`，状态 `running`。
- `sys_flow_task` 已生成 `部门经理审批` 待办，任务 ID `602b2410-5e15-11f1-a0fd-d67ed5f8e875`，assignee `1`。

## 4. 接口验证

以下接口均通过 `X-Inner-Call: true` 验证，返回 `code=200`：

- `/ai/business/document/config/1910000000000000104`：单据配置已启用，默认流程 `leave_multi`。
- `/ai/business/document/OPPORTUNITY/10/runtime`：返回 `documentStatus=IN_PROCESS`、`flowStatus=RUNNING`，可用动作包含 `START_FLOW`、`VIEW_FLOW`。
- `/ai/business/flow/status/OPPORTUNITY/10`：返回 `RUNNING`。
- `/ai/business/trigger/page?pageNum=1&pageSize=10`：返回总数 `5`。
- `/ai/business/trigger/scenario-templates`：返回模板数 `5`。
- `/ai/business/stats/crm_opportunity/metrics`：返回指标 `TOTAL`、`TODAY`、`MONTH`。

## 5. 构建验证

- `git diff --check`：通过。
- 后端：`mvn -pl forge-admin-server -am package -DskipTests`：`BUILD SUCCESS`。
- 前端：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：构建通过。
- 前端构建仅保留既有警告：UnoCSS 图标加载、CSS `//` 注释、动态/静态导入混用和 chunk size 提示。

## 6. 结论

Phase 8 验证通过。数据库迁移、流程定义部署、商机单据手动发起流程、运行态状态查询、触发器模板、触发器分页和业务指标接口均已完成最小闭环验证。

---

# Phase 9 增量执行日志：Task 23-25

> 执行时间：2026-06-02 20:34 CST
> 范围：动态应用菜单归属、open-info 协议、菜单选中态、应用入口运行态打开模式和填报入口。

## 1. 本轮改动

- 后端 `BusinessAppOpenInfoVO` 增加 `targetRoute`、`runtimeOpenMode`、`menuResourceId`、`activeMenuKey`。
- 后端 `BusinessAppDTO`、`BusinessAppVO` 增加 `runtimeOpenMode`，VO 额外回显 `menuResourceId`。
- `BusinessAppService` 复用 `ai_business_app.options` 保存 `runtimeOpenMode`，RUNTIME 应用菜单同步到最终运行页路径，并保留原菜单资源 ID。
- `BusinessAppOpenService` 生成带菜单归属和打开模式 query 的 `targetRoute`。
- 前端菜单 activeKey 优先读取 `menuKey/menuResourceId/appId`，动态运行页 Tab/浏览器标题支持按菜单 key 或 query title 恢复。
- 应用入口抽屉增加 `LIST`、`CREATE_FORM`、`DETAIL` 运行态打开模式。
- 运行页支持 `mode=create` 自动打开新增表单，新增保存成功后移除 `mode=create` 回到列表。

## 2. 验证命令

- `git diff --check`：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：构建通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端或前端服务，未执行浏览器点击验证；原因是本轮先完成 Task 23-25 代码与构建验证，未要求联调现有 dev 数据菜单。

## 4. 结论

Task 23-25 增量验证通过。动态应用入口协议、菜单选中态恢复和填报入口打开模式已完成代码闭环。

---

# Phase 9 增量执行日志：Task 26-29

> 执行时间：2026-06-02 21:05 CST
> 范围：单据编号规则、状态映射、单据设置面板、主流程合并兼容、流程变量候选项和自动映射建议。

## 1. 本轮改动

- 后端新增单据编号变量接口和编号预览接口：`/ai/business/document/no-rule/tokens`、`/ai/business/document/no-rule/preview`。
- 单据配置协议扩展 `noRuleTemplate`、`statusMappingRows`、`statusActionPolicy`、`noRulePreview`、`mainFlowSummary`，并保持旧 `statusMapping` 兼容。
- 单据配置保存时校验编号未知变量，结构化状态映射写入 `options.statusMappingRows`，运行态标准状态映射继续写入 `status_mapping`。
- 主流程事实来源合并为 `ai_business_binding(binding_type=FLOW)`；历史 `default_flow_key` 只做兼容读取和保存后快照回写。
- 发布检查改为读取主流程摘要，提示未配置主流程、变量映射缺失等具体缺口。
- 新增流程变量解析器，解析内置变量、BPMN 表达式/审批人属性、流程动态表单字段和业务对象字段，并给出映射建议。
- 前端单据设置面板重构为基础配置、编号规则编辑器、状态映射表和发布摘要；移除独立默认流程下拉。
- 流程绑定面板接入变量候选项，下拉选择流程变量，并支持只补空项的推荐映射应用。

## 2. 验证命令

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：构建通过。
- `git diff --check`：通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未新增 Flyway：未改表结构，历史主流程通过读取兼容和保存回写渐进迁移。
- 本轮未启动后端、流程服务或前端 dev server，未执行浏览器点击和真实接口联调；原因是本轮先完成 Task 26-29 代码与构建验证，未要求连接现有 dev 数据逐项试跑。

## 4. 结论

Task 26-29 增量验证通过。编号规则、结构化状态映射、单据设置面板、主流程合并兼容和流程变量候选项已完成代码闭环。

---

# Phase 9 增量执行日志：Task 30-33

> 执行时间：2026-06-02 21:44 CST
> 范围：流程绑定面板和标题模板体验、运行态自动发起流程按钮、触发器前置对象、单据闭环步骤条、发布检查和验证回填。

## 1. 本轮改动

- 新增 `TemplateVariableEditor.vue` 和 `FlowVariableMappingEditor.vue`，主流程配置和触发器 `START_FLOW` 动作复用同一套变量插入、标题预览、变量候选和推荐映射体验。
- 单据运行态 `BusinessDocumentRuntimeVO` 返回 `runtimeActions`，后端按主流程配置、发起方式、状态、进行中流程和权限生成“发起流程”按钮及禁用原因。
- `crud-page.vue` 为运行页当前页记录加载单据运行态，`AiCrudPage.vue` 合并自动行操作并执行 `START_FLOW`，成功后刷新列表。
- 触发器页面支持 `objectCode` query 上下文，新增触发器先选业务对象，切换对象后清理旧字段条件和动作配置。
- 对象设计器新增单据闭环步骤条，单据保存和主流程保存后能引导进入下一步；触发器配置从对象设计器携带当前对象。
- 发布检查和 readiness 增加应用入口、菜单资源、运行打开模式、编号规则、状态映射、主流程完整度、自动按钮和触发器缺口提示。
- 修复单据已启用但主流程摘要为空时，运行态自动按钮和发布检查可能出现空指针的边界。

## 2. 验证命令

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：构建通过。
- `git diff --check`：通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端、Flow 服务、前端 dev server 或数据库，未执行接口联调和浏览器点击验证；原因是本轮按增量范围完成代码编译和前端生产构建验证，未要求连接现有 dev 数据逐项试跑。
- 本轮未启动任何服务，无需停止服务。

## 4. 结论

Task 30-33 增量验证通过。流程配置体验、运行态自动发起流程按钮、触发器对象前置和单据闭环步骤条已完成代码闭环，并已补齐本轮 `test-spec.md`、`tasks.md`、`spec.md` 和执行日志。

---

# Phase 10 增量执行日志：Task 34

> 执行时间：2026-06-02 22:55 CST
> 范围：定时触发扫描器、到期提醒配置、集群部署防重复和验证回填。

## 1. 本轮改动

- 新增 `BusinessTriggerSchedulerService`，使用低频 `@Scheduled` 统一扫描启用的 `SCHEDULE/SCHEDULED` 触发器，默认 5 分钟一次。
- 针对集群部署补齐 Redisson 分布式锁：全局扫描锁防止多个节点同时扫，记录级执行锁防止同一到期记录重复执行。
- 定时触发动作由 scheduler 调用同步单条执行入口，确保记录级锁覆盖动作执行和触发器日志写入。
- 定时扫描只按配置的 `schedule.dueField` 区间读取候选记录，字段必须经过运行态字段白名单校验，单批默认 50、最大 200。
- 新增 `SCHEDULED_DUE` 事件类型，保存时兼容历史 `SCHEDULED` 并归一为 `SCHEDULE`。
- 增加同日 `SUCCESS/TODO` 日志去重、扫描时间回写和前端定时参数配置区。
- `TriggerConditionBuilder` 保留 `eventCondition.schedule`，避免编辑条件时丢失定时参数。

## 2. 验证命令

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：构建通过。
- `git diff --check`：通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、数据库、Redis 或两个 admin 实例，未执行真实扫描落库、多实例抢锁和浏览器点击验证；原因是本轮按增量范围完成代码编译、前端构建和静态补丁验证，集群验证需要可用的 Redis/数据库和双实例运行环境。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

Task 34 增量验证通过。定时触发已从配置占位补齐为低频扫描执行能力，并已针对集群部署问题增加 Redisson 全局扫描锁、记录级执行锁和同日日志去重；真实多实例抢锁和落库去重验证仍需在联调环境补跑。

---

# Phase 10 修正执行日志：Task 34 接入系统任务调度中心

> 执行时间：2026-06-03 07:40 CST
> 范围：将低代码定时触发从 Spring 本地定时迁移到系统任务调度中心，并补齐 Quartz 集群配置。

## 1. 本轮改动

- `BusinessTriggerSchedulerService` 不再使用 Spring `@Scheduled`，改用 `@ScheduledJob` 注册到系统任务调度中心。
- 新任务名：`lowcodeBusinessTriggerScanJob`，分组：`LOWCODE`，默认 cron：`0 0/5 * * * ?`。
- `forge-plugin-generator` 新增 `forge-starter-job` 依赖，只依赖任务注解，不直接耦合 `forge-plugin-job` 实现。
- `ScheduleConfig` 显式写入 `org.quartz.jobStore.isClustered`，并从 `forge.job` 读取线程数、集群 checkin 间隔、misfire 阈值和表前缀。
- `JobProperties` 新增 `threadPoolSize`、`clustered`、`clusterCheckinInterval`、`misfireThreshold`、`tablePrefix` 配置项。
- 保留 Redisson 全局扫描锁、记录级锁和同日日志去重，作为任务中心手动触发、补偿执行或配置缺失时的并发兜底。

## 2. 验证命令

- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests`：`BUILD SUCCESS`。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 本轮未启动后端服务、数据库、Quartz 双实例或任务中心页面，未验证 `sys_job_config` 自动注册、Quartz 集群单点触发和真实扫描落库；原因是本轮为架构修正和编译验证，联调验证需要可用数据库和多实例环境。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

Task 34 已改为复用系统自带任务调度能力。定时触发扫描由任务中心统一调度，默认 5 分钟一次，支持后台启停和 cron 调整；Quartz 集群负责主防重，Redisson 锁和业务日志去重继续作为兜底防线。

---

# Phase 10 BUG 修正执行日志：低代码应用功能问题

> 执行时间：2026-06-03 16:22 CST
> 范围：编号规则示例格式、应用入口打开方式文案、单据填报 form-only、发布 loading、发起主流程按钮和触发器动作配置简化。

## 1. 本轮改动

- 编号规则模板统一为 `${}` 格式，后端兼容历史 `{yyyyMMdd}` / `{seq4}` 并归一为 `${yyyyMMdd}` / `${seq:4}`。
- 应用入口把“运行态页面 / 内部路由”统一改为“业务对象页面 / 系统已有页面”，`CREATE_FORM` 说明改为“单据填报，直接显示表单，不显示列表”。
- 运行页 `CREATE_FORM` 改为 `formOnly` 模式，进入页面直接展示填报表单，提交成功显示结果页，不再先渲染列表再弹窗。
- 对象设计发布按钮和发布检查按钮补齐 `loading`，发布中禁用按钮并显示加载态。
- 自定义操作和运行态按钮统一为“发起主流程”，按钮只配置名称、位置、权限和提示，不再重复选择流程。
- 触发器 `START_FLOW` 动作默认使用“流程与自动化”中的主流程，触发器面板不再展示流程模型、标题模板和变量映射；后端执行时空流程 key / 空标题回落到主流程绑定。

## 2. 验证命令

- `git diff --check`：通过。
- 首次执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests`：失败，原因是默认 JDK 不支持 target 17，报 `无效的目标发行版: 17`。
- 使用项目指定 Java 17 复跑：`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、Flow 服务、前端 dev server 或数据库，未执行真实接口联调和浏览器点击验证；原因是本轮按用户反馈完成代码修正和构建验证，联调需要可用 dev 数据和服务链路。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

本轮 BUG 修正的后端插件编译、前端生产构建和补丁空白检查均已通过。触发器和自定义操作的发起流程配置已收敛为主流程复用，单据填报入口已改为独立表单页。

---

# Phase 10 BUG 跟进执行日志：入口模式与自定义操作显示

> 执行时间：2026-06-03 16:57 CST
> 范围：对象类型与入口打开方式说明、父级菜单回填、自定义操作注入运行态列表、单据设置解释和操作列兼容识别。

## 1. 本轮改动

- 明确产品口径：对象类型只描述数据结构，入口打开方式描述使用场景；单表对象不强制只能“单据填报”，需要列表行操作时选择“列表管理”。
- 应用入口详情 VO 新增 `adminMenuParentId`、`adminMenuSyncEnabled`、`suiteAsMenuParent`、`menuSort`，前端编辑器优先使用详情字段并兼容旧 `options.adminMenu`。
- 菜单同步时保存 `originalParentId`，避免“套件作为父级目录”时实际挂载父级和配置父级混用导致回填失败。
- 发布业务对象时将 `designerOptions.actions` 转为运行态 `customActions` 注入 table zone，运行态配置生成 `rowActions/toolbarActions` 后能进入列表操作列。
- `crud-page.vue` 和 `AiCrudPage.vue` 兼容 `action/actions/operation/operations` 操作列命名；没有操作列但存在行操作时主动创建“操作”列。
- 单据设置补充状态字段、发起人/负责人和主流程的职责说明，状态映射表头改为“允许编辑 / 允许删除 / 允许发起”。

## 2. 验证命令

- `git diff --check`：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-job -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、Flow 服务、前端 dev server 或数据库，未执行真实接口联调和浏览器点击验证；原因是本轮按用户反馈完成代码修正和构建验证，联调需要可用 dev 数据和服务链路。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

用户反馈跟进项的后端插件编译、前端生产构建和补丁空白检查均已通过。入口打开方式已从对象类型中解耦，父级菜单回填字段已拍平，自定义操作按钮已补齐发布注入和运行态操作列渲染兼容。

---

# Phase 10 BUG 跟进执行日志：非单据对象发起主流程

> 执行时间：2026-06-03 17:08 CST
> 范围：修复未启用单据模式的业务对象点击“发起主流程”时报“业务对象未启用单据模式，无法发起主流程”。

## 1. 本轮改动

- `BusinessFlowService.startDocumentFlowInternal` 改为双路径：启用单据模式时保留单据状态和动作权限校验；未启用单据模式时按对象已发布运行配置读取记录，并使用“流程与自动化”的主流程绑定发起流程。
- 新增 `AiCrudConfigMapper.selectPublishedByObjectCode`，从已发布 LOWCODE 运行配置中解析非单据对象的 `configKey`。
- 流程发起后只有单据对象才回写单据状态为 `IN_PROCESS`；普通业务对象只写流程实例关联，不强制要求状态字段。
- 流程回调不再因为对象未启用单据模式而失败；非单据对象回调会更新流程实例状态，并在存在运行配置时继续发布 `FLOW_APPROVED/FLOW_REJECTED/FLOW_CANCELED` 业务事件。

## 2. 验证命令

- `git diff --check`：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 本轮只改后端 Java 和 Mapper XML，未重跑前端生产构建。
- 本轮未启动后端服务、Flow 服务或数据库，未执行真实流程发起和回调联调；原因是本轮按错误来源完成后端逻辑修复和编译验证，联调需要可用运行配置、流程模型和记录数据。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

非单据业务对象现在也可以通过“发起主流程”自定义操作走对象主流程绑定。单据模式仍保留状态字段、状态映射和动作权限校验；普通对象不再被“未启用单据模式”拦截。

---

# Phase 10 BUG 跟进执行日志：发布检查与回显一致性

> 执行时间：2026-06-03 17:33 CST
> 范围：修复应用入口发布检查误报、父级菜单回显、主流程绑定读取和旧审批兼容状态。

## 1. 本轮改动

- `selectRuntimeAppByObject` 调整排序：优先启用、已绑定运行配置、已同步菜单资源、已配置打开模式、最近更新的运行态入口，避免发布检查读到历史旧入口后误报“菜单资源未同步”。
- 发布检查不再因为 `runtimeOpenMode` 缺省报警；缺省按 `LIST` 处理，仅在存在非法值时提示“运行打开模式不合法”。
- 菜单同步写入 `options.adminMenu` 时将 `menuResourceId`、`parentId`、`originalParentId` 字符串化，避免前端 `JSON.parse` 雪花 Long 后精度丢失。
- `MenuParentSelect` 和应用入口编辑器保留菜单 ID 字符串，不再强转 Number；入口编辑器保存时优先保留详情接口拍平的 `menuResourceId`。
- 主流程绑定查询优先启用且有模型的绑定，并兼容历史 `APPROVAL` 绑定，避免旧空绑定抢先命中后提示“请先配置主流程”。
- 旧审批兼容运行态在对象未启用完整单据状态能力但已配置主流程时，不再返回“未启用单据模式”作为不可发起原因。

## 2. 验证命令

- `git diff --check`：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、Flow 服务、前端 dev server 或数据库，未执行真实发布检查接口、菜单同步落库和浏览器回显验证；原因是本轮按用户反馈完成读取/回显口径修正和构建验证，联调需要可用业务对象、应用入口、菜单资源和流程模型数据。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

发布检查现在会优先读取真正可用的运行态入口，缺省打开模式不再误报；菜单父级和资源 ID 避免 Long 精度丢失；主流程摘要和发起兼容层能识别新 `FLOW` 与历史 `APPROVAL` 绑定。

---

# Phase 10 BUG 跟进执行日志：运行态按钮、主流程诊断和套件目录回显

> 执行时间：2026-06-03 20:06 CST
> 范围：修复 `/ai/crud-page` 头部重复“新增”、主流程发起失败诊断日志、应用入口“套件作为父级目录”实际挂载目录回显。

## 1. 本轮改动

- `/ai/crud-page` 运行态过滤 `add/create/new/新增/新建` 这类无目标路由的标准新增 toolbar action，避免和 `AiCrudPage` 内置新增按钮重复。
- 业务对象自定义操作不再默认预置标准 CRUD 动作；发布运行态时跳过历史保存的标准新增、编辑、详情、删除动作，避免老数据继续生成重复按钮。
- 保存主流程绑定时，已有 `FLOW` 绑定会同步修正 `targetType/targetCode/bindingType` 并置为启用，避免页面保存后仍因停用绑定提示“请先配置主流程”。
- 流程发起解析主流程失败时增加诊断日志，输出租户、对象、记录、运行配置、单据配置、绑定状态、候选 `flowModelKey` 来源和 `binding_config` 截断预览。
- 应用入口菜单同步保存 `adminMenu.actualParentId/suiteMenuResourceId`；入口编辑抽屉在勾选“套件作为父级目录”后只读回显实际生成的套件目录，父级选择框继续表示套件目录的上级。

## 2. 验证命令

- `git diff --check`：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、Flow 服务、前端 dev server 或数据库，未执行真实流程发起日志、菜单同步落库和浏览器回显验证；原因是本轮按用户反馈完成代码修正和构建验证，联调需要可用业务对象、菜单资源、流程模型和记录数据。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

运行态重复新增按钮已从前端渲染和发布数据两侧兜底去重；主流程保存会重新启用已停用的 `FLOW` 绑定，发起失败时可通过新增日志定位绑定/配置来源；入口配置能回显自动生成的套件目录实际挂载位置。

---

# Phase 10 BUG 跟进执行日志：主流程对象标识归一和入口菜单持久化

> 执行时间：2026-06-04 14:20 CST
> 范围：修复低代码流程发起使用 `configKey` 查不到主流程绑定、应用入口勾选“套件作为父级目录”后菜单资源和实际父级回显不稳定。

## 1. 本轮定位

- 用户日志显示前端发起流程时传入 `objectCode=hr_leave_application`，但主流程绑定日志按该值查不到记录。
- 排查数据口径后确认：业务对象标准 `objectCode` 为 `LEAVE_APPLICATION`，运行配置 `configKey` 为 `hr_leave_application`；单据配置和流程绑定均落在 `LEAVE_APPLICATION` 下。
- 旧运行配置存在 `ai_crud_config.object_code=hr_leave_application` 的历史不一致数据，导致后端把 `configKey` 当业务对象编码使用时，主流程绑定读取落空。
- 入口菜单同步已经能生成菜单，但菜单资源 ID、套件目录 ID、实际挂载父级只存在 `options.adminMenu` 深层结构时，编辑入口详情没有稳定拍平回传，前端保存时也可能丢失。

## 2. 本轮改动

- `BusinessFlowService` 增加流程启动上下文解析：先按请求值查运行配置、单据配置和业务对象，再归一出标准业务对象 `objectCode` 与运行态 `configKey`。
- 流程发起、运行态查询、流程绑定查询和保存统一使用标准业务对象编码查 `ai_business_binding`、构造 `businessKey`、写流程实例关联和流程变量。
- 运行态数据读取继续使用解析后的 `configKey` 调用动态 CRUD，避免改成标准对象编码后读不到低代码运行配置。
- 主流程绑定查询增加候选对象编码集合，兼容标准 `objectCode`、原始请求值和历史 `APPROVAL` 绑定；失败日志输出候选对象编码和绑定状态。
- 新增 `selectByConfigKey`、`selectPublishedByObjectCodeOrConfigKey` 等 Mapper XML 查询，避免在 Service 中拼查询逻辑。
- `BusinessAppVO` 拍平返回 `activeMenuKey`、`adminMenuActualParentId`、`suiteMenuResourceId`。
- `AppEditorDrawer.vue` 保存入口配置时保留 `menuResourceId`、`activeMenuKey`、`actualParentId`、`suiteMenuResourceId`，避免“套件作为父级目录”编辑后回显丢失。
- 菜单 ID 暂不新增 `ai_business_app` 物理列，继续存放在 `ai_business_app.options.adminMenu.menuResourceId`；本轮通过 VO 拍平和前端保存兜底解决回显问题。

## 3. 验证命令

- `xmllint --noout --nonet forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudConfigMapper.xml forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessDocumentConfigMapper.xml forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessObjectMapper.xml forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessAppMapper.xml`：通过。
- `git diff --check`：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests`：`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过，输出 `built in 1m 17s`。

## 4. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、Flow 服务、前端 dev server 或数据库，未执行真实 `/ai/business/flow/start`、菜单同步落库和浏览器编辑回显验证；原因是本轮按用户反馈完成后端归一逻辑、前端回显字段和构建验证，联调需要可用流程模型、记录数据和菜单资源。
- 本轮未启动任何长期服务，无需停止服务。

## 5. 结论

流程发起不再依赖前端传入的值刚好等于业务对象标准编码；`hr_leave_application` 会先归一到 `LEAVE_APPLICATION` 查主流程绑定，同时仍用 `hr_leave_application` 读取运行态记录。入口菜单资源 ID 继续存储在 `ai_business_app.options.adminMenu`，通过详情 VO 拍平和保存保留解决“套件作为父级目录”后的回显问题。

---

# Phase 10 BUG 跟进执行日志：动态页发起流程和详情流程进度

> 执行时间：2026-06-04 15:20 CST
> 范围：动态页发起主流程交互、流程任务 assignee 用户 ID 归一、单据详情流程进度 Tab 配置。

## 1. 本轮改动

- 动态运行页“发起主流程”改为使用内置 `window.$dialog.warning` 确认弹窗，发起中显示全局 loading message，并按行禁用按钮显示“发起中...”。
- `FlowTaskEventListener` 创建、分配、完成任务时统一把 Flowable 原始 assignee/owner 归一为用户 ID；事件 payload 也使用归一后的 `sys_flow_task.assignee`，避免继续向业务监听方传姓名。
- 单据配置面板新增详情页流程展示开关：流程时间轴、流程图，默认开启并存入单据配置 `options`。
- 单据运行态 VO 返回详情页流程展示配置；动态详情页默认使用弹窗，不再用抽屉，并按 Tab 展示“业务数据 / 流程进度”。
- 新增 `AiCrudFlowDetail`，复用现有流程时间轴和 BPMN 流程图组件；未发起流程时在流程进度 Tab 显示空状态。

## 2. 验证命令

- `git diff --check`：通过。
- `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests`：`BUILD SUCCESS`，总耗时 `22.090 s`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过，输出 `built in 1m 13s`。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、Flow 服务、前端 dev server 或数据库，未执行真实 `/ai/business/flow/start` 落库、`sys_flow_task.assignee` 数据库检查和浏览器点击验证；原因是本轮先按用户反馈完成代码修正和编译构建验证，联调需要可用流程模型、记录数据和服务链路。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

本轮动态页发起主流程的确认与 loading 体验、流程任务 assignee 用户 ID 归一、以及详情页流程进度 Tab 配置已经通过后端主应用编译、前端生产构建和补丁空白检查。真实流程发起和落库校验需要在可用 dev 数据与服务链路下补充。

---

# Phase 10 BUG 跟进执行日志：详情流程配置对象标识归一

> 执行时间：2026-06-04 16:47 CST
> 范围：修复详情页流程展示开关已打开，但动态详情仍不展示流程进度/流程图。

## 1. 本轮定位

- 动态详情页调用 `/ai/business/document/{objectCode}/{recordId}/runtime` 时，前端传入值仍可能是运行配置 `configKey` 或历史 `ai_crud_config.object_code`。
- 单据配置和流程实例关联按标准业务对象 `objectCode` 保存；`BusinessDocumentRuntimeService` 之前只按请求值直接查单据配置，导致 `options.detailFlowTimelineVisible/detailFlowDiagramVisible` 读不到。
- 同一个问题还会导致流程实例关联用错误的 `businessKey` 查询，例如用 `configKey:recordId` 查不到按标准 `objectCode:recordId` 写入的流程关联。

## 2. 本轮改动

- `BusinessDocumentRuntimeService` 增加运行态上下文解析：按请求值查发布运行配置、单据配置和业务对象，再归一出标准业务对象 `objectCode`。
- 单据运行态统一用标准 `objectCode` 构造 `businessKey`、查询流程实例关联、解析可用动作；继续使用单据配置中的 `configKey` 读取动态 CRUD 记录数据。
- 前端 `crud-page.vue` 解析业务对象编码时，优先使用 `businessObjectCode/options.businessObjectCode/modelSchema.objectCode`，最后才回退到历史 `cfg.objectCode/options.objectCode/configKey`。

## 3. 验证命令

- `git diff --check`：通过。
- `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`，总耗时 `16.282 s`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过，输出 `built in 1m 8s`。

## 4. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、Flow 服务、前端 dev server 或数据库，未执行真实详情接口返回检查和浏览器流程图显示验证；原因是本轮先修正对象标识归一和构建验证，联调需要可用记录、流程实例和服务链路。
- 本轮未启动任何长期服务，无需停止服务。

## 5. 结论

详情页流程展示配置读取现在不再依赖前端传入值刚好等于标准业务对象编码；传入 `configKey` 或历史运行配置对象编码时，也会归一到标准 `objectCode` 读取单据配置开关并查询流程实例关联。

---

# Phase 10 BUG 跟进执行日志：发起流程重复实例幂等

> 执行时间：2026-06-04 17:07 CST
> 范围：修复动态页点击“发起主流程”后创建两个流程实例、两个待办的问题。

## 1. 本轮定位

- 动态页操作按钮处理里，配置动作会先 `emit('custom-action')`，再进入内置 `START_FLOW` 分支；如果运行态外层也监听 `custom-action` 发起流程，同一次点击会走两条发起路径。
- 后端 `BusinessFlowService.startDocumentFlowInternal` 之前是先查 `selectRunningByBusinessKey`，再调用 Flowable 启动流程，最后插入 `ai_business_flow_instance_link`。两个近同时请求都可能在插入提交前通过运行中实例检查，导致各自启动一个 Flowable 实例并生成两条待办。

## 2. 本轮改动

- `AiCrudPage` 对内置 `START_FLOW` 先执行 `startFlowAction` 并立即返回，不再向外抛 `custom-action`，避免同一按钮事件被运行态外层重复消费。
- 发起流程前继续使用内置确认弹窗和行级 loading，重复点击同一行同一动作时提示“流程正在发起，请稍候”。
- `BusinessFlowService` 按 `tenantId + canonical businessKey` 获取流程发起锁；锁内重新执行单据权限校验、运行中流程检查、流程模型解析、Flowable 启动和实例关联插入。
- 优先使用 Redisson 分布式锁并依赖 watchdog 续期，Redisson 不可用时使用本地 `ReentrantLock` 做单实例兜底；事务同步开启时延迟到事务完成后释放锁，避免下一请求在实例关联提交前通过检查。

## 3. 验证命令

- `git diff --check`：通过。
- `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：最终复跑 `BUILD SUCCESS`，总耗时 `10.562 s`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过，输出 `built in 1m 16s`。

## 4. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、Flow 服务、前端 dev server 或数据库，未执行真实并发 `/ai/business/flow/start`、流程实例落库、`sys_flow_task` 待办数量和浏览器点击验证；原因是本轮先收口重复提交代码路径和构建验证，真实并发联调需要可用流程模型、业务记录和服务链路。
- 本轮未启动任何长期服务，无需停止服务。

## 5. 结论

动态页内置发起流程现在不会再被 `custom-action` 重复消费；后端同一业务单据的流程发起在事务完成前按业务 Key 串行化，第二个请求会在锁释放后读到已运行实例并返回已有流程，不再同时创建两个 Flowable 实例。

---

# Phase 10 BUG 跟进执行日志：Flow 服务 businessKey 唯一键幂等

> 执行时间：2026-06-04 17:29 CST
> 范围：修复 Flow 服务 `/api/flow/instance/start/{modelKey}` 重试时撞 `sys_flow_business.uk_flow_business_tenant_key` 的问题。

## 1. 本轮定位

- 用户日志显示 Flow 服务在启动 `leave_multi` 时插入 `sys_flow_business`，`business_key=LEAVE_APPLICATION:5`，数据库返回 `Duplicate entry '1-LEAVE_APPLICATION:5' for key 'sys_flow_business.uk_flow_business_tenant_key'`。
- 这说明 `sys_flow_business` 已经有同一租户同一业务 Key 的流程业务记录；即使 admin 侧已做低代码发起锁，Flow 服务自身仍需要按 `tenant_id + business_key` 做幂等。
- 还有一种重试场景是 admin 侧 `ai_business_flow_instance_link` 没写入或事务回滚，但 Flow 服务已成功创建 `sys_flow_business` 和 Flowable 实例；再次发起时 Flow 服务应返回已有流程实例 ID，让 admin 侧补齐关联，而不是抛数据库唯一键异常。

## 2. 本轮改动

- `FlowInstanceServiceImpl.startProcess` 启动前按当前租户和 `businessKey` 查询 `sys_flow_business`：已有运行中、草稿或 Flowable runtime 仍存在的记录时，直接返回原 `processInstanceId`。
- 对已结束状态 `approved/rejected/canceled/terminated/completed` 的业务 Key，返回明确的不可重复发起错误，避免把历史完成实例误当成新流程。
- Flow 服务同 JVM 内按 `tenantId + businessKey` 使用本地 `ReentrantLock` 串行化启动，事务开启时延迟到事务完成后释放锁；跨实例并发仍由数据库唯一键兜底，捕获 `DuplicateKeyException` 后再查询并复用已有实例或返回“流程正在发起，请稍后重试”。
- 新建 `FlowBusinessMapper.xml`，把 `selectByProcessInstanceId/selectByBusinessKey/selectByBusinessKeyAndTenantId` 查询放到 XML；新增流程业务记录时显式设置 `tenantId`。

## 3. 验证命令

- `xmllint --noout --nonet forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowBusinessMapper.xml`：通过。
- `git diff --check`：通过。
- `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-flow/forge-flow-server -am compile -DskipTests`：最终复跑 `BUILD SUCCESS`，总耗时 `7.581 s`。

## 4. 警告和跳过项

- 本轮未启动 Flow 服务或数据库，未执行真实 `/api/flow/instance/start/leave_multi` 重试、并发发起和 `sys_flow_business/sys_flow_task` 落库检查；原因是本轮先按错误日志修复服务端幂等缺口并完成编译验证。
- 本轮未改前端代码，未重新执行前端构建。
- 本轮未启动任何长期服务，无需停止服务。

## 5. 结论

Flow 服务现在不会把同一租户同一 `businessKey` 的重复启动直接打成数据库唯一键异常；运行中流程会幂等返回已有实例 ID，已结束流程会给出明确的不可重复发起错误。

---

# Phase 10 BUG 跟进执行日志：同一流程实例重复待办

> 执行时间：2026-06-04 18:02 CST
> 范围：修复同一个 `businessKey`、同一个 `processInstanceId` 下生成两条不同 `task_id` 待办的问题。

## 1. 本轮定位

- 用户反馈当前不再是两个流程实例，而是同一个流程实例下生成两条待办，`businessKey` 和 `processInstanceId` 一样，只有 `task_id` 不一样。
- dev 库核查显示 `ACT_RU_TASK` 中同一 `PROC_INST_ID_` 有两个活跃 `deptApprove` 用户任务，`TASK_DEF_KEY_` 和任务名相同。
- 对应部署的 `leave_multi` BPMN XML 同时存在旧连线 `flow1/flow2/flow3` 和设计器生成的新连线 `Flow_0fnqi4c/Flow_09gi1bi/Flow_1bw7xaf`；这些连线的 `sourceRef -> targetRef` 语义重复。
- Flowable 会按 `sourceRef` 计算实际出线，即使节点的 `<outgoing>` 只引用了新线，只要 `<sequenceFlow sourceRef="startEvent" targetRef="deptApprove">` 还存在，都会创建执行路径，所以同一流程实例会产生两个 `deptApprove` 待办。

## 2. 本轮改动

- 新增 `BpmnXmlUtils.normalizeDuplicateSequenceFlows`，基于 DOM 安全解析 BPMN XML，按流程/子流程作用域识别语义完全相同的重复 `sequenceFlow`。
- 清理策略优先保留被 `<incoming>/<outgoing>`、节点 `default` 或 BPMNDI `BPMNEdge` 引用的连线；删除未引用的重复旧线，并同步清理对应 `<incoming>/<outgoing>` 和 `BPMNEdge`。
- `FlowModelServiceImpl` 在创建、更新、导入、复制和部署流程模型时统一执行 BPMN 重复连线规范化；部署时把清洗后的 XML 写回 `FlowModel.bpmnXml`，避免版本历史继续记录坏定义。
- `FlowModelVersionServiceImpl` 在版本回退部署前执行同样的规范化，并把新版本记录和模型当前 XML 都写为清洗后的部署 XML。
- 新增 `BpmnXmlUtilsTest` 覆盖重复旧线清理和不同条件表达式连线不误删；由于父 POM 固定 `skipTests=true` 且 compiler `skip=true`，Maven 单测目标仍被项目配置跳过，本轮用临时 Java smoke runner 直接验证已编译工具类行为。

## 3. 验证命令

- `git diff --check`：通过。
- `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am compile -DskipTests`：`BUILD SUCCESS`，总耗时 `3.569 s`。
- `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-flow/forge-flow-server -am compile -DskipTests`：`BUILD SUCCESS`，总耗时 `4.147 s`。
- 临时 smoke runner：`javac -cp forge/forge-framework/forge-plugin-parent/forge-plugin-flow/target/classes -d /private/tmp /private/tmp/BpmnXmlUtilsSmoke.java` 通过；`java -cp forge/forge-framework/forge-plugin-parent/forge-plugin-flow/target/classes:/private/tmp BpmnXmlUtilsSmoke` 输出 `repairs=3`、`oldDuplicateRemoved=true`、`referencedFlowsKept=true`。

## 4. 警告和跳过项

- 首次 Maven 执行使用系统默认 JDK，失败于 `无效的目标发行版: 17`；切换到项目固定 JDK 17 后编译通过。
- `mvn -Dtest=BpmnXmlUtilsTest test` 被父 POM 的 `maven-compiler-plugin <skip>true</skip>` 和 `maven-surefire-plugin <skipTests>true</skipTests>` 固定配置跳过，未作为通过证据。
- 本轮未启动 Flow 服务和数据库，未重新部署 `leave_multi` 或发起真实 `LEAVE_APPLICATION:5`；原因是本轮先修复 BPMN 定义入口和编译验证，真实运行态需要对现有坏模型做一次保存/部署或数据修复。
- 已经运行中的坏流程实例不会被新部署自动修复；该实例已有两个执行 token/待办，需要终止、清理或人工处理后重新用修复后的定义发起。
- 本轮未启动任何长期服务，无需停止服务。

## 5. 结论

当前“同一流程实例两条待办”的重复点不在 `sys_flow_task` 插入逻辑，而在已部署 BPMN 定义中存在重复语义连线。现在后端会在模型保存、导入、复制、部署和版本回退时从源头清理这类重复 `sequenceFlow`，避免 Flowable 在同一个开始节点或任务节点上创建两条并行执行路径。

---

# Phase 10 BUG 跟进执行日志：单据设置体验、状态映射和编号规则落地

> 执行时间：2026-06-04 20:45 CST
> 范围：修复单据设置页面视觉混乱、动态表单行距过宽、用户/组织选择清除按钮错位、状态映射自由输入、编号规则未写入申请单号字段。

## 1. 本轮改动

- `BusinessDocumentPanel` 重构为紧凑后台配置布局，按“单据模式 / 编号生成 / 状态字典 / 主流程”展示配置进度，并把基础配置、编号生成、状态映射、详情页流程展示分区整理。
- 状态字段只允许选择带字典或本地选项的字段；`DocumentStatusMappingTable` 的字段值改为下拉选择，展示名随选项自动回填，不再让用户自由输入状态值。
- 单据配置新增 `options.documentNoField`，保存时校验编号规则必须绑定编号字段；回显时可从 `applicationNo/application_no/documentNo/document_no` 等字段自动推断。
- 动态 CRUD 新增记录前读取已启用单据配置，使用 `forge-starter-id` 的业务序列按编号规则生成单据号，并覆盖客户端传入的编号字段值，兼容 camelCase/snake_case 字段别名。
- 运行态配置回显时把编号字段写入 `options.documentNoField`，并将对应编辑表单项置为只读/禁用、占位提示“系统自动生成”。
- 动态编辑表单默认行距从 16 收紧到 8，横向间距默认 12；`AiCrudPage` 增加运行态表单 class 以便统一压缩 Naive 表单反馈间距。
- `UserSelectPicker` 和 `AiFormItem` 增加选择/清除按钮居中样式，修复用户选择、组织选择清除叉号与输入框错位。

## 2. 验证命令

- `git diff --check`：通过。
- 首次执行 `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：失败，原因是 generator 插件使用 `ISequenceService` 但未声明 `forge-starter-id` 编译依赖。
- 补充 `forge-plugin-generator/pom.xml` 对 `forge-starter-id` 的依赖后复跑同一后端编译命令：`BUILD SUCCESS`，总耗时 `35.731 s`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过，输出 `built in 1m 55s`。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 当前会话没有可用浏览器自动化工具暴露，且未启动后端服务、前端 dev server 或数据库；未执行真实对象设计器点击、动态新增落库和表单视觉截图验证。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

本轮已经从配置端和运行态两侧收口：单据状态映射值来自状态字段的字典/选项，编号规则必须绑定编号字段，动态新增时后端统一生成并写入申请单号/单据编号字段，运行态表单不再要求用户手填编号。相关前端构建、后端插件编译和补丁空白检查已通过。

---

# Phase 10 BUG 跟进执行日志：应用入口套件菜单自父级

> 执行时间：2026-06-04 21:10 CST
> 范围：修复应用入口勾选“同步为菜单 + 套件作为父级目录”后，保存会把套件目录 `sys_resource.parent_id` 写成自身 ID，导致入口菜单不渲染。

## 1. 本轮定位

- 应用入口保存时，`adminMenu.parentId/originalParentId` 表示“套件目录的上级”；`adminMenu.actualParentId/suiteMenuResourceId` 表示后端生成或复用的“套件目录自身 ID”。
- 旧回显/保存链路可能把 `actualParentId/suiteMenuResourceId` 回填进 `adminMenuParentId`，下一次保存时后端又把它当作套件目录上级传给 `resolveOrCreateBusinessSuiteParentId`。
- 菜单适配器会按套件目录 perms 复用已有 `sys_resource` 并更新 `parent_id`，因此传入父级等于套件目录自身时，会把 `parent_id` 更新成自己的 `id`。

## 2. 本轮改动

- `BusinessAppService` 在同步管理端菜单前归一化套件目录上级：当 `originalParentId/parentId` 等于 `suiteMenuResourceId`、`actualParentId` 或应用菜单自身 ID 时，按顶级挂载处理并清空持久化的原始父级。
- `BusinessAppService.enrichAppVO` 对历史污染配置做同样归一，避免编辑抽屉再次把“实际挂载目录”自身展示为“套件目录上级”。
- `MenuRegisterAdapterImpl` 增加菜单自父级兜底防护：更新应用菜单或复用套件目录时，如果 `parentId == resourceId`，自动挂载到顶级 `0` 并输出 warning 日志。
- `AppEditorDrawer` 回显和保存时区分“套件目录上级”和“实际挂载目录”，保存前过滤掉 `actualParentId/suiteMenuResourceId/menuResourceId`，不再写入 `parentId/originalParentId`。

## 3. 验证命令

- `git diff --check`：通过。
- `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests`：`BUILD SUCCESS`，总耗时 `12.327 s`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过，输出 `built in 1m`。

## 4. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、前端 dev server 或数据库，未执行真实应用入口保存、`sys_resource.parent_id` 落库检查和浏览器菜单回显验证；原因是本轮先修复保存链路和自引用兜底，真实落库需要可用 dev 服务与对应入口数据。
- 本轮未启动任何长期服务，无需停止服务。

## 5. 结论

应用入口保存链路现在不会再把套件目录自身 ID 当作上级目录保存；即使旧配置已经污染，重新保存时也会被后端归一并由菜单适配器兜底挂载到顶级，避免 `sys_resource.parent_id = id` 导致菜单树渲染失败。

---

# Phase 10 BUG 跟进执行日志：表单/列表设计器和运行态交互

> 执行时间：2026-06-05 06:22 CST
> 范围：修复列表字段批量对齐/行间距、表单设计器字段 ID 选择、未保存字段跨面板保存、动态页发起流程页面级 loading、对象入口导入导出启用判断。

## 1. 本轮改动

- `StructuredListPageDesigner` 和 `ListPageGridDesigner` 增加“全部列对齐”和“行间距”配置；全局对齐会同步写入所有已选列表字段的 `fieldSettings.align`，行间距写入列表区 `rowGap` 并作用于设计器预览。
- `LowcodeRuntimeConfigBuilder` 从列表区读取 `rowGap` 下发为 `options.tableRowGap`；`crud-page.vue` 在运行态转换为 `AiTable` 的 `rowProps` 行高样式。
- `BusinessFormCreateDesigner` 安装业务组件规则时传入字段资产；form-create 右侧基础配置的字段 ID 改为已有字段下拉，业务组件和常用基础组件属性里也提供“组件字段ID”选择。
- `formCreateToForge` 优先读取字段下拉选择值，清理内部 `fieldBinding` 属性，不把它下发为运行态组件 props；同时把 form-create 默认生成的 `F...` 随机字段名视为临时值，未选择字段时按标题生成可读字段编码。
- `BusinessFormDesigner` 暴露 `syncDesignerDraft`，用于只 flush 画布和自动补齐字段，不直接调用后端。
- `object-designer.[objectCode].vue` 在从表单设计切换到单据/流程/操作等面板前同步表单草稿；保存这些面板前如存在待持久化设计器草稿，先静默保存且不 reload 当前面板，避免“页面区域引用了不存在的字段”。
- `AiCrudPage` 发起主流程改为当前页面遮罩 loading，行操作只禁用不显示“发起中...”按钮态。
- `/app-center/object` 的导入模板、导入、导出按钮改为只依赖运行配置 `configKey`，不再被 `canOpen` 阻断。

## 2. 验证命令

- `git diff --check`：通过。
- `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`，总耗时 `10.771 s`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过，输出 `built in 1m 5s`。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮初次验证未新启动后端服务、前端 dev server 或数据库，未执行真实对象设计器点击、单据配置保存、运行态导入导出接口和动态页发起流程浏览器验证；收尾阶段复用已有 Vite dev server 做了入口和模块加载验证，见第 5 节。
- 本轮未启动任何长期服务，无需停止服务。

## 4. 结论

本轮从设计器和运行态两端修复了字段配置与保存链路：列表字段可批量对齐并设置行间距，表单组件字段 ID 可从已有字段选择，未保存的新字段在切换到单据设置保存前会先同步并静默持久化；动态页发起流程 loading 改为页面级遮罩，对象入口导入导出不再被运行页可打开状态误拦截。

## 5. 收尾验证补充

- `rg -n "^## 50|form-create 随机字段|页面区域引用了不存在的字段" code-copilot/memory/pitfalls.md`：确认本轮踩坑记录已追加到第 50 条。
- `git diff --check`：通过。
- `curl -I 'http://[::1]:3001'`：已有 `forge-admin-ui` Vite dev server 返回 `HTTP/1.1 200 OK`。
- `curl -I 'http://localhost:3001/src/views/app-center/components/designer/BusinessDocumentPanel.vue'`：返回 `HTTP/1.1 200 OK`，用户反馈的 `BusinessDocumentPanel.vue` HMR 404 在当前服务上未复现。
- 首次执行 `playwright screenshot --full-page 'http://localhost:3001' /private/tmp/forge-admin-ui-home.png` 被 macOS 沙箱拦截 Chromium MachPort 权限；提升权限后重跑成功。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && playwright screenshot --wait-for-timeout 5000 --full-page 'http://localhost:3001' /private/tmp/forge-admin-ui-home-wait.png`：成功截图，等待后进入登录页，未出现白屏或模块加载失败。

**服务清理**:
本轮未新启动长期服务，仅复用已有 PID 24518 的 `forge-admin-ui` Vite dev server，未停止用户已有进程。

---

# Phase 10 BUG 跟进执行日志：表单字段 ID 保存和字段资产名称保护

> 执行时间：2026-06-05 07:05 CST
> 范围：修复表单设计器选择已有字段 ID 后保存回显丢失，以及字段资产被同步成“未命名字段”的问题。

## 1. 本轮定位

- form-create 右侧属性面板存在多个字段写入位置：基础配置会改 `rule.field/name`，业务组件属性可能改 `props.fieldBinding.fieldCode`。旧转换逻辑优先读取旧 `_forge.fieldBinding` 或旧 props，导致用户选择的新字段可能被覆盖。
- 保存表单草稿时，自动字段资产的基准使用了 `modelSchema.fields`。该结构主要服务页面模型，常只有 `field/label`，不一定包含完整 `fieldName/fieldCode`；同步回字段资产后会导致字段列表显示“未命名字段”。

## 2. 本轮改动

- `forgeToFormCreate` 下发规则时，把当前 `fieldBinding.fieldCode` 同步写入 `props.fieldCode` 和 `props.fieldBinding.fieldCode`，保证字段选择框有稳定回显值。
- `formCreateToForge` 回收规则时同时兼容 `rule.field/name`、`props.fieldCode`、`props.fieldBinding.fieldCode` 和根级 `fieldBinding.fieldCode`；当旧绑定与新选择不一致时，优先保留用户刚修改的字段。
- `formCreateToForge` 对已存在字段设置 `createIfMissing=false`，并使用已有字段的 `columnName`，避免误创建新字段或覆盖字段资产。
- `BusinessFormDesigner` 自动字段资产基准改为优先使用真实 `props.fields`，只在没有字段资产时才回退 `modelSchema.fields`。
- `BusinessFormDesigner` 保存表单后用完整字段资产刷新父层本地字段，避免本地继续拿旧字段列表。
- `object-designer` 保存字段 payload 时对 `fieldName/fieldCode` 增加兜底，避免空字段名写回后端。

## 3. 验证命令

- `git diff --check`：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过，输出 `✓ built in 1m 3s`。

## 4. 警告和跳过项

- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务和数据库，未执行真实对象设计器保存、重新加载详情和字段资产落库检查；原因是本轮改动为前端转换和本地 payload 修复，真实落库回显需要可用后端数据。
- 本轮未新启动长期服务，无需停止服务。

## 5. 结论

表单组件字段 ID 的选择值现在会在 form-create 规则和 Forge 表单 Schema 之间稳定往返；保存字段资产时也会优先使用真实字段资产而不是页面模型字段，避免已有字段名称被清空后显示为“未命名字段”。

---

# Phase 10 BUG 跟进执行日志：对象设计保存后入口菜单同步和字段资产保存

> 执行时间：2026-06-05 07:44 CST
> 范围：修复对象设计器保存后应用入口菜单未重新同步，导致返回应用入口时看不到实际挂载目录；修复字段资产修改后全局保存仍提示“请先选择需要保存的字段”。

## 1. 本轮定位

- 应用入口的管理端菜单 path、组件、实际套件目录和菜单资源 ID 只在入口自身保存时同步。对象设计器保存后可能更新业务对象 `configKey` 或运行态配置，但已关联该对象的运行态入口没有重新执行菜单同步，导致入口回显和管理端菜单仍引用旧配置。
- 字段资产全局保存会调用字段管理组件的 `saveSelectedField()`。旧逻辑在没有选中字段或属性面板关闭时直接提示“请先选择需要保存的字段”，阻断对象设计器整体保存；而实际场景下用户可能已经在属性面板里改过字段，或当前字段资产面板没有待保存字段。

## 2. 本轮改动

- `BusinessAppMapper` 新增 `selectRuntimeAppsByObject` XML 查询，按租户、套件和对象找出所有 `BUSINESS/RUNTIME` 应用入口。
- `BusinessAppService` 新增 `syncRuntimeAppsForObject`，对象设计保存后刷新关联入口的 `configKey`，并重新执行 `syncManagementMenu`，同步菜单 path、`menuResourceId`、`actualParentId` 和 `suiteMenuResourceId`。
- `BusinessObjectDesignerService.saveDraft` 在保存对象 `configKey` 后调用入口同步，避免只有保存应用入口时菜单才更新。
- `BusinessFieldManager.saveSelectedField` 在属性面板打开时直接保存当前面板 payload；无选中字段且无属性面板待保存时返回成功并提示“字段资产暂无需要保存的字段属性”，不再阻断对象设计器全局保存。

## 3. 验证命令

- `git diff --check`：通过。
- `xmllint --noout --nonet forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessAppMapper.xml`：通过。
- `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`，总耗时 `19.223 s`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过，输出 `✓ built in 7m 31s`。
- 文档和 memory 回填后复跑 `git diff --check`：通过。

## 4. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、前端 dev server 或数据库，未执行真实对象设计器保存、`ai_business_app/options` 落库检查、侧边栏菜单渲染和字段资产浏览器点击验证；原因是本轮先完成代码链路修复和编译构建验证，真实落库需要可用 dev 服务与对应入口数据。
- 本轮未新启动长期服务，无需停止服务。

## 5. 结论

对象设计器保存现在会主动同步关联运行态入口的管理端菜单，不再依赖用户额外打开应用入口再保存；字段资产全局保存也不再把“未选中字段”当成错误阻断整页保存。

---

# Phase 10 BUG 跟进执行日志：查询条件隐藏旧字段引用清理

> 执行时间：2026-06-05 08:59 CST
> 范围：修复字段改名后旧随机字段码残留在查询条件 `viewSchema` 或隐藏查询映射中，导致发布检查报“查询条件引用字段不存在”。

## 1. 本轮定位

- 用户看到的“查询条件引用了不存在字段: frpjmpzgzlc1hfc”来自发布检查的 `viewSchema.search.fields[].fieldCode` 校验，不是普通列表设计器可见的查询条件 `pageSchema.zones.search.fieldRefs`。
- 字段改名时后端递归替换了 `fieldRef`、`field`、`sourceField` 等引用，但漏掉了表单优先视图 schema 使用的 `fieldCode`，因此旧 form-create 随机字段码会留在 `ai_business_object.designer_options.viewSchema.search.fields`。
- 列表/查询自由布局还可能在 `props.fieldSettings[*].queryField` 里保存旧字段，运行态构建时会把这个隐藏映射写入搜索 schema。

## 2. 本轮改动

- `BusinessFieldDesignService` 在字段改名/删除时同步清理 `designerOptions`，递归处理 `fieldCode` 和 `queryField`，避免旧字段继续留在 `viewSchema` 或隐藏查询映射中。
- `BusinessObjectDesignerService` 读取 `viewSchema` 时按当前模型字段集过滤 search/list/detail 里的历史失效字段，旧数据重新打开设计器时不会再回显脏引用。
- `BusinessObjectPublishService` 发布检查前对 `viewSchema` 做同样过滤，已有脏数据不会再因为用户无法看到的旧字段阻断发布。
- 前端 `viewSchema.js` 增加 `sanitizeViewSchemaFieldRefs`，对象设计器保存 payload 前、字段资产更新后都会按当前字段资产过滤 `viewSchema`。
- `page-schema.js` 清理列表自由布局和查询区的 `fieldSettings`，旧 `queryField` 指向不存在字段时会删除；运行态搜索 schema 构建兜底回退到当前字段。

## 3. 验证命令

- `git diff --check`：通过。
- `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：`BUILD SUCCESS`，总耗时 `18.035 s`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：构建通过，输出 `✓ built in 1m 39s`。

## 4. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 编译提示，未阻断。
- 前端构建保留既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态导入混用和 chunk size 提示，未阻断。
- 本轮未启动后端服务、数据库或前端 dev server，未执行真实对象字段改名落库、发布检查接口和浏览器查询条件回显验证；原因是本轮先完成隐藏 schema 清理链路修复和编译构建验证。
- 本轮未新启动长期服务，无需停止服务。

## 5. 结论

旧字段 `frpjmpzgzlc1hfc` 这类 form-create 临时字段现在会在字段改名/删除、设计器读取、发布检查和前端保存 payload 四个入口被清理或过滤。用户即使在查询条件 UI 找不到该字段，也不会再被隐藏 `viewSchema.search.fields[].fieldCode` 阻断。

---

# 本轮增量执行日志：低代码应用列表查询与基础表格默认居中

> 执行时间：2026-06-25 09:38 CST
> 范围：低代码应用分页摘要查询、关键词搜索防抖、首屏并行加载、列表主路径索引、基础表格默认列居中。

## 1. 本轮改动

- `AiCrudConfigMapper.xml` 的低代码应用分页查询改为摘要字段，新增 `LowcodeAppDetailResultMap`，不再读取 `modelSchema/pageSchema`。
- `LowcodeAppService.page` 直接返回分页 VO，删除列表态 `toDetailVO` 反序列化成本。
- `lowcode-apps.vue` 首屏改为并行加载领域树和应用列表，关键词输入增加 300ms 防抖，分页切换会清理待执行搜索。
- `AiTable.vue` 默认列对齐改为 `center`，并补充 `titleAlign` 透传，保留单列显式对齐覆盖。
- 新增 `V1.0.78__add_lowcode_app_page_summary_index.sql`，为低代码应用列表默认查询补上 `tenant_id + mode + build_mode + update_time + id` 索引。

## 2. 验证命令

- `xmllint --noout --nonet forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiCrudConfigMapper.xml`：通过。
- `sed -n '1,120p' forge-server/db/migration/V1.0.78__add_lowcode_app_page_summary_index.sql`：脚本内容检查通过，幂等创建逻辑完整。
- `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：首次在沙箱内因写 `~/.m2` 失败，重新放开权限后 `BUILD SUCCESS`。
- `cd forge-admin-ui && source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：构建通过。

## 3. 警告和跳过项

- Maven 保留既有 deprecation / unchecked 提示，未阻断。
- 前端构建保留既有 CSS `//` 注释警告、动态/静态导入混用警告和 chunk size 提示，未阻断。
- 本轮未启动后端、前端 dev server 或数据库，未执行浏览器点击验证，也未执行 Flyway 实跑。

## 4. 结论

本轮优化已完成代码和构建验证。低代码应用列表不再携带详情级 JSON 反序列化成本，基础表格默认展示改为居中对齐。

---

# 本轮增量执行日志：动态页面查询重复 DB 优化

> 执行时间：2026-06-25 20:49 CST
> 范围：低代码动态页 `/ai/crud/{configKey}/page` 与业务单据运行态 `/ai/business/document/{objectCode}/runtime/batch` 查询链路的控制面重复 DB 优化。

## 1. 本轮定位

- 用户日志显示一次查询会先调用 `GET /ai/crud/crm_customer/page`，随后调用 `POST /ai/business/document/crm_customer/runtime/batch`；两条请求合计后端约 1.3s，前端体感约 3s。
- 热点重复 DB 主要来自控制面元数据：每个请求都查 `SysResourceMapper.selectConfiguredApiUrls`，低代码运行数据源在同一链路反复 `GenDatasourceMapper.selectById`，动态 CRUD 反复读取 `AiCrudConfigMapper.selectByConfigKey`。
- `runtime/batch` 已经批量读取业务记录和流程实例，但单据配置解析仍会先按 `objectCode=crm_customer` miss，再按 `configKey=crm_customer` 命中，并在 `toVO` 内二次补查运行态配置。
- `DynamicDataScopeService` 在热路径用 info 打印 skip/apply 日志，虽然不是 DB，但会放大查询日志量和 I/O 成本。

## 2. 本轮改动

- `PermissionServiceImpl` 对已配置 API 资源 URL 做 HTTP method 维度 30s 本地缓存；`SysResourceServiceImpl` 在资源保存、更新、删除和资源管理显式增删改后立即清空缓存，避免权限配置长期陈旧。
- `LowcodeRuntimeDataSourceResolver` 对 `GenDatasource` 做 datasourceId / datasourceCode 维度 30s 本地缓存，并返回对象拷贝，避免调用方解密密码或修改字段污染缓存。
- `RuntimeJdbcTemplateProvider` 改为复用 `LowcodeRuntimeDataSourceResolver` 缓存，不再每次构造运行态 `JdbcTemplate` 时重复查 `gen_datasource`。
- `GenDatasourceServiceImpl` 在数据源保存、更新、删除后清理运行数据源缓存和动态数据源连接池；更新时同时清旧 code 和新 code。
- `AiCrudConfigService.getByConfigKey` 增加 10s 短缓存，并在 `save/updateById/removeById` 和配置增删改路径失效；返回配置拷贝，避免动态 CRUD 修改运行表名污染缓存。
- `BusinessDocumentRuntimeService` 优先按发布态 `configKey` 查启用单据配置，再按对象编码兜底；构建运行态 VO 时把已解析的 `AiCrudConfig` 传给 `BusinessDocumentConfigService.toVO`，减少二次配置查询。
- `DynamicDataScopeService` 将热路径 skip/apply 日志从 info 降到 debug，减少普通查询日志 I/O。

## 3. 验证命令

- `git diff --check -- forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/PermissionServiceImpl.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysResourceServiceImpl.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiCrudConfigService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicDataScopeService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessDocumentConfigService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessDocumentRuntimeService.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/impl/GenDatasourceServiceImpl.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/runtime/LowcodeRuntimeDataSourceResolver.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/runtime/RuntimeJdbcTemplateProvider.java`：通过，无输出。
- `cd forge-server && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -q -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-system -am compile -DskipTests`：通过，退出码 0。

## 4. 警告和跳过项

- 本轮未启动后端服务和数据库，未执行真实 `crm_customer` 查询日志前后对比；原因是先完成后端热路径代码优化和模块编译验证。
- 本轮未执行前端构建；原因是本轮没有前端代码变更。
- 本轮未新启动长期服务，无需停止服务。

## 5. 结论

动态页面查询链路已减少控制面重复 DB：权限资源配置、低代码配置、运行数据源解析和运行态 JDBC provider 都有短缓存与配置变更失效；业务单据运行态也减少了无效配置查询和二次运行配置读取。
