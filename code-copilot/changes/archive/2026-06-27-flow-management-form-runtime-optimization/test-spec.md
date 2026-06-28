# 测试计划 — 流程管理表单运行态与业务入口优化

> 创建时间：2026-06-06
> 测试策略：按 `code-copilot/rules/automated-testing-standard.md` 做增量验证。

## 1. 本轮验证范围

- 后端 Flow 插件和 `forge-flow-server`：流程表单版本、入口、运行态、组织填报批次、任务表单快照、待办站内信推送相关 Java/XML 编译。
- 前端流程管理：`flow/form`、`flow/model` 全屏流程设计弹窗、模型级表单设计器、BPMN 字段目录和手工动态路由清理。
- 前端流程模型：已部署模型的“发起测试”弹窗，按模型级动态表单收集变量后发起流程，并跳转“我发起的”查看流转结果。
- 前端流程表单：流程表单设计器复用低代码业务组件，字典、组织、人员等组件在设计器和运行渲染器中都能加载选项。
- 前端流程表单：动态表单字段默认下方间距为 20px，设计器右侧“表单配置”支持统一设置字段行间距。
- Flyway 脚本：`V1.0.57__add_flow_form_runtime_entry.sql` 新增表、字典和按钮权限；`V1.0.58__remove_standalone_flow_entry_menus.sql` 清理误注册的独立入口/组织填报菜单。

## 2. P0 必跑

| 验证项 | 命令 | 期望 |
|--------|------|------|
| 后端编译 | `cd forge && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-flow/forge-flow-server -am compile -DskipTests` | Reactor `BUILD SUCCESS` |
| 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | Vite build 成功，流程模型页不再引用入口配置组件或入口运行页 |
| 空白检查 | `git diff --check -- forge-admin-ui/src/components/form-create/FlowFormCreateDesigner.vue forge-admin-ui/src/components/form-create/formCreateBridge.js forge-admin-ui/src/views/flow/design.vue forge-admin-ui/src/views/flow/model.vue forge-admin-ui/src/router/index.js code-copilot/changes/flow-management-form-runtime-optimization/spec.md code-copilot/changes/flow-management-form-runtime-optimization/tasks.md code-copilot/changes/flow-management-form-runtime-optimization/test-spec.md code-copilot/changes/flow-management-form-runtime-optimization/execution-log.md` | 无输出，退出码 0 |

## 3. P1 条件验证

| 验证项 | 条件 | 当前处理 |
|--------|------|----------|
| Flyway 实跑 | 本地 MySQL/Redis/dev profile 可用 | 本轮未启动数据库，暂未执行迁移实跑 |
| 接口联调 | `forge-flow-server`、主应用鉴权和测试流程定义可用 | 本轮以编译和前端构建为阶段验证，未启动服务做 curl |
| 浏览器交互 | 前后端服务和测试数据可用 | 本轮未做 Playwright，前端 build 已覆盖 Vue 编译和路由导入 |

## 4. 风险回归点

- `PROCESS_ONLY` 必须保存 `sys_flow_form_instance` 快照并永久保留。
- `BUSINESS_OBJECT/HYBRID` 已接入 `FlowBusinessObjectRuntimeAdapterImpl` 和 `DynamicCrudService`，后续联调需重点验证业务对象 `configKey`、字段映射和 `ai_business_flow_instance_link` 关联。
- 组织批量填报不再作为流程管理独立页面；若具体业务应用启用该场景，后续联调需验证无负责人组织跳过告警、重复发布防重和驳回后重新提交。
- 待办站内信幂等依赖 `bizType + bizKey` 查询，后续可补数据库唯一键强化并发幂等。
- BPMN 字段目录已覆盖审批人、候选用户、候选组和流转条件；高级消息模板字段引用仍依赖手写 `${field}`。
- 流程设计器和任务详情已改为弹窗；本轮通过前端构建验证模板，未做浏览器截图交互验证。
- 模型页“入口配置”已撤掉；业务入口展示位置改由低代码应用、业务对象或后续流程发起中心承载。
- 模型页“发起测试”只用于设计验证，测试业务键固定使用 `FLOW_TEST:` 前缀，不能作为正式业务入口配置复用。
- 流程表单设计器共享低代码业务组件注册；后续浏览器联调需重点验证字典类型下拉、组织树、人员选择在不同接口权限下的加载表现。

## 9. 2026-06-06 流程表单设计器业务组件对齐验证范围

- 前端：流程表单设计器注册低代码业务组件分组，保留流程字段自由编辑。
- 前端：流程表单运行渲染器复用业务组件选项补齐逻辑，支持发起测试、预览、待办办理时加载系统字典、组织树和人员列表。
- 前端：流程动态表单字段默认行间距和右侧“表单配置”中的统一字段行间距配置。
- 必跑命令：`git diff --check` 和 `pnpm --dir forge-admin-ui build`。

## 10. 2026-06-06 表单级行间距纠偏验证范围

- 前端：流程动态表单“字段行间距（px）”必须出现在 fcDesigner 右侧“表单配置”Tab，不再出现在字段属性面板。
- 前端：保存、预览刷新和配置变更时统一把表单级行间距写入所有规则的 `wrap.style.marginBottom`。
- 静态扫描：不得再存在 `enableFormItemSpacing` 或 `formCreateWrap>style>marginBottom` 字段属性扩展代码。
- 必跑命令：`git diff --check`、残留引用 `rg` 扫描和 `pnpm --dir forge-admin-ui build`。

## 8. 2026-06-06 模型页测试发起和设计器配置区增量验证范围

- 前端：流程模型页已部署模型的更多操作增加“发起测试”，弹窗渲染模型级动态表单，提交后调用流程启动接口。
- 前端：流程设计器顶部配置区取消默认折叠，拆成“流程属性 / 表单配置 / 说明”三块，表单配置常驻展示设计、选择和预览动作。
- 必跑命令：`git diff --check` 和 `pnpm --dir forge-admin-ui build`。

## 7. 2026-06-06 入口边界和表单设计器增量验证范围

- 前端：流程模型页移除“入口配置”动作、隐藏入口运行路由和错误运行页；流程表单设计器弹窗高度与设计器高度保持一致。
- 前端：`FlowFormCreateDesigner` 显式使用中文 locale，并归一化历史英文组件标题，避免动态表单选择器显示 `Select`。
- 文档：同步修正 spec/tasks/test 里“模型页入口配置”的旧产品结论。
- 必跑命令：`git diff --check` 和 `pnpm --dir forge-admin-ui build`。

## 5. 2026-06-06 增量验证范围

- 后端：`BUSINESS_OBJECT/HYBRID` 落表适配器、批量填报发布、`batchItemId` 提交回写、消息推送相关编译。
- 前端：BPMN 节点字段目录选择、模型设计全屏弹窗、待办/已办/我发起/抄送详情弹窗、批量填报发布按钮。
- 静态检查：本轮相关前端文件和变更文档的 `git diff --check`。

## 6. 2026-06-06 启动装配增量验证范围

- 后端：`forge-flow-server` 引入 `forge-plugin-generator` 后的 Spring bean 装配和可执行 jar 启动。
- 必跑命令：`mvn -pl forge-flow/forge-flow-server -am package -DskipTests`。
- 启动验证：`java -jar forge-flow/forge-flow-server/target/forge-flow-server.jar --server.port=0`，需要 dev MySQL/Redis 可连接。
- 验证重点：`MenuRegisterAdapter`、`AiClientAdapter`、`FlowClient` 相关装配错误不再出现，日志出现 `Started ForgeFlowApplication`。
