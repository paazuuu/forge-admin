# Forge项目决策记录

> 记录项目级架构和产品决策，避免后续变更重复讨论。

## 1. 低代码应用与代码生成统一主链路

**记录日期**: 2026-05-25

低代码应用、AI 应用生成和代码生成统一收敛到“应用管理/应用开发”主入口。用户从需求描述生成模型和应用草稿后，必须确认才保存；应用级代码预览和 ZIP 下载默认使用已保存草稿，发布版本作为可选来源。

模型管理继续保留独立入口，因为模型是领域资产，可以不参与应用设计。模型导入直接读取数据源表结构生成 `ai_lowcode_model.model_schema`，不从旧 `GenTable` 选择；旧 `GenTable` 数据保留但新流程忽略。

数据源管理保留为开发者菜单；模板管理菜单去掉。首期代码生成支持单表/单主模型，主子表、左树右表和树形单表作为后续扩展。

## 2. AI Agent 配置来源

**记录日期**: 2026-05-25

Forge 的 AI Agent 角色提示词必须优先配置在 `ai_agent.system_prompt`，可维护上下文、输出协议和规则必须放在 `ai_context_config`，代码只传 `agentCode`、用户输入和运行时变量。低代码业务系统生成使用 `lowcode_system_generator`，禁止把完整业务 Prompt 长期硬编码在 Java Service 中；Java 里只保留规则降级和协议归一化逻辑。

## 3. 表单优先业务对象设计器使用 fcDesigner 作为首期画布

**记录日期**: 2026-05-31

低代码业务对象设计器后续主链路调整为“表单优先”：普通用户默认先设计最终表单，平台再维护字段注册表、视图投影、级联规则和运行态配置。

首期不从零自研完整表单画布，优先复用系统已集成的 `fcDesigner` / form-create 能力。`fcDesigner` 负责拖拽画布、组件排序、基础属性编辑和预览，Forge 负责业务组件适配、字段绑定、FormDesignerSchema、ViewSchema、LinkageSchema、发布检查和运行态编译。

form-create rule/options 只作为设计器可编辑表示，不能成为 Forge 运行时唯一事实来源。保存和发布必须通过 Forge Adapter 转换为 `FormDesignerSchema + FieldRegistry + ViewSchema + LinkageSchema`，发布运行态继续编译到 `AiCrudPage`、`AiForm`、`DynamicCrudController` 和 `LowcodeRuntimeConfigBuilder`。

## 4. 审批场景统一归入 Flowable 流程引擎

**记录日期**: 2026-06-02

低代码应用不再建设独立“审批引擎”。审批是业务单据绑定 Flowable 流程后的使用场景，发起、待办、结果回写、消息通知和触发器联动都围绕流程实例完成。

内置示例和 seed 数据必须部署真实 Flowable 流程定义，不能用模拟审批实例 ID 代替。业务对象的流程绑定和自动触发器需要显式维护业务字段到流程变量的映射，确保 BPMN 节点表达式需要的变量在启动时已经存在。

## 5. 低代码定时触发接入系统任务调度中心

**记录日期**: 2026-06-02

低代码业务触发器的 `SCHEDULE` 定时能力不为每个触发器创建独立 Quartz Job，也不默认做秒级扫描。平台只注册一个 `LOWCODE.lowcodeBusinessTriggerScanJob` 到系统任务调度中心，默认每 5 分钟扫描一次启用的到期提醒触发器，后台任务中心可统一启停、改 cron 和查看日志。

集群部署下主防重依赖 Quartz JDBC 集群调度，`forge.job.clustered` 默认应开启；Redis/Redisson 全局扫描锁、记录级执行锁和同日日志去重作为手动触发、补偿执行或任务配置缺失时的兜底防线。

## 6. 登录验证码配置解析规则

**记录日期**: 2026-06-07

登录验证码配置采用“全局默认 + 客户端覆盖”的单一解析规则。系统登录配置维护全局默认验证码开关和默认验证码类型；`sys_client.captcha_type` 只作为客户端覆盖项，空值表示继承全局配置。

登录页获取 `/auth/loginConfig` 时必须传入当前 `userClient`，后端登录配置接口和登录校验策略必须共用同一个解析器返回最终生效配置，避免前端展示验证码类型和后端校验验证码类型不一致。

## 7. 代码生成模板更新和删除必须使用 POST

**记录日期**: 2026-06-14

Forge 代码生成模板不能按通用 REST 风格生成 `PUT` 更新或 `DELETE` 删除接口。出于项目安全策略和网关兼容要求，生成 Controller、前端 API 和 `AiCrudPage.apiConfig` 必须保持既有 POST 风格：

- 详情：`POST /getById`
- 新增：`POST /add`
- 更新：`POST /edit`
- 删除：`POST /remove/{id}`
- 批量删除：`POST /removeBatch`

应用管理下载代码模式可以把接口前缀替换为业务专属 `businessApiBase`，但不能把更新、删除改成 `PUT` / `DELETE`。

## 8. 长期记忆统一归集到 code-copilot/memory

**记录日期**: 2026-06-14

项目决策、踩坑记录、用户偏好三类长期记忆从 `.opencode/memory/` 迁移到 `code-copilot/memory/`，后续只维护 code-copilot 下的权威文件：

- 项目决策：`code-copilot/memory/decisions.md`
- 踩坑记录：`code-copilot/memory/pitfalls.md`
- 用户偏好：`code-copilot/memory/preferences.md`

`AGENTS.md`、`code-copilot/AGENTS.md`、`code-copilot/agents/*.md` 和变更模板必须指向上述权威路径；`.opencode/memory/` 不再作为 Forge 项目长期记忆维护位置，`code-copilot/knowledge/` 只保留专题技术知识材料。

## 9. 数据权限控制面元数据固定在平台主库

**记录日期**: 2026-06-22

租户业务数据源切换只影响业务 Mapper 查询的主业务表。数据权限控制面元数据，包括 `sys_data_scope_config`、`sys_role`、`sys_role_data_scope`、`sys_org` 和 `sys_region_code`，必须固定由 Forge 平台主库提供，不能要求租户业务库复制这些平台表。

`forge-starter-datascope` 运行时应先从 `forge.datascope.metadata-datasource`（默认 `master`）加载控制面快照，再在业务 SQL 拦截时只读取内存快照。行政区划权限等需要平台字典/树数据的规则，必须提前解析成业务库可执行的字面量条件，禁止在业务库 SQL 中拼接 `sys_region_code` 等平台表子查询。

## 10. 低代码运行规则和字段展示统一走共享封装

**记录日期**: 2026-06-25

列表设计器、表单设计器、发布运行页和详情页中，涉及组件/字段/模块的显示隐藏、只读、禁用、必填、颜色和样式控制，统一通过 `forge-admin-ui/src/components/lowcode-builder/shared/runtime-rules.js` 解析。

字段展示不再在列表列、详情字段、表单只读态中分别实现。文本、字典标签、状态 Tag、链接、金额和颜色规则统一走 `FieldValueRenderer.vue`；运行规则配置入口统一走 `RuntimeRulesEditor.vue`。后续新增动态展示组件或复杂字段渲染时必须优先扩展这三个共享封装，避免列表和表单再次分叉。

运行规则的数据来源统一使用 `source` 表达：`record` 表示当前记录/详情，`row` 表示当前行数据，`formData` 表示当前表单数据，`query` 表示 URL 查询参数，`params` 表示路由参数，`user` 表示当前用户。列表行操作打开的编辑/详情弹窗必须把当前行放入 `context.currentRow`，运行规则上下文再统一映射为 `row`。
