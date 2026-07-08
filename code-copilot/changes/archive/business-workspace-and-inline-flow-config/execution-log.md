# 变更日志 — 我的工作台 + 节点就地配置 + 渲染引擎收敛

> 记录决策、踩坑和知识发现。知识飞轮的输入。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
| --- | --- | --- | --- |
| 2026-06-29 | propose | 用户在第二轮 spec 后反馈"还有结构性问题没解" | 引出三大问题：我的工作台 / 应用中心节点就地配置 / 双渲染引擎收敛 |
| 2026-06-29 | propose | 创建变更目录 `code-copilot/changes/business-workspace-and-inline-flow-config/`，生成 `spec.md` / `tasks.md` / `test-spec.md` / `execution-log.md` | 与 `unified-app-flow-experience` 拆开，独立迭代 |
| 2026-06-29 | propose | HARD-GATE 六项确认完成（保持并列 / 支持网关 / 分批离线迁移 / 本轮清理 form-create / summary 默认 + 偏好 / 仅 4 项 + summary） | 调整：新增 B8 网关业务化就地配置 + C7 form-create 完全清理（含前置硬条件 + 回滚预案 + 7 天观察期） |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
| --- | --- | --- | --- |
| 三个子迭代独立交付 | A / B / C 任一可单独发布 | 一锤子全做完 | 用户可按业务节奏分批；任一阻塞不影响其他 |
| 统一渲染引擎 | AiForm | 引入第三方表单引擎 | 自研 AiForm 已能覆盖 90% 场景；不引入外部依赖 |
| schema 迁移落点 | 新列 `form_schema`，旧列 `form_json` 保留兜底 | 原地改写 | 灰度安全；可降级；可重入 |
| 节点配置数据所有权 | 应用中心就地保存共享 BPMN 节点属性 | 新建"应用中心节点配置表" | 避免双写，数据所有权清晰 |
| 并发编辑保护 | 乐观锁（updateTime） | 悲观锁 / 拒绝双端 | 用户体验更好；冲突很少；UI 兜底 |
| 顶部导航 | 应用中心 / 工作台 / 能力中心 三级，**保持并列**（用户 2026-06-29 确认） | 应用中心内嵌"工作台 Tab"；工作台收编应用中心 | IA 干净；应用中心 = 配置入口 / 工作台 = 日常事务 |
| 网关就地编辑 | 本轮支持 exclusiveGateway + inclusiveGateway 业务化就地编辑（用户确认） | 第一版不支持，所有网关只能进完整设计器 | 同意/驳回路由是业务高频场景，分离体验割裂；用 isInlineEditable 判定复杂度兜底 |
| form-create 下线节奏 | 本轮完成代码清理（用户确认）；保留 form_json 列一个大版本作兜底 | 仅 deprecate 不删；下一轮单独清理 | 用户明确要求一次性收口；前置硬条件 + 回滚预案 + 7 天观察期降低风险 |
| 工作台默认页 | summary，用户偏好可记忆 | 直接到 todo 列表 | 聚合首页提供决策入口，列表深处难发现；用 ai_user_setting 落地 |
| 路由兼容期 | 保留 2 个大版本 | 立刻 redirect | 老链接 / 通知 / 邮件需要迁移时间 |
| 应用中心节点配置入口 | 弹窗打开真实流程设计器 | 应用中心自建节点配置抽屉 | 用户明确要求在流程图节点上下文中配置，避免两套节点配置入口割裂 |
| 代码应用字段面板 | 应用中心不展示“表单字段”面板 | 只读字段目录 | 用户认为字段来自表单/Provider，不应在应用设计里再出现字段配置心智 |
| 业务字段变量 | 自动注入全部业务字段 | 人工维护变量映射 | 流程设计节点条件会从表单字段里选字段，人工映射是重复配置 |
| 节点表单权限主数据 | BPMN 节点配置为准，应用中心不再保存 `nodeForms` | 应用中心保存一份 nodeForms 副本 | 避免低代码应用保存后覆盖流程设计器字段权限 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 沉淀？ |
| --- | --- | --- | --- |
| （propose 阶段无踩坑，apply 时补充） | — | — | — |

## 知识发现

> 每个 task 后实时记录，/archive 时逐条确认沉淀到 `code-copilot/memory/` 或 `knowledge/`

- [ ] **关键词**: `workspace top-nav IA` — 工作台作为顶级导航与应用中心并列，徽标驱动用户感知，避免被埋进二级菜单
- [ ] **关键词**: `workspace default page user preference` — `ai_user_setting.workspace.default_page` 记录用户偏好，默认 summary，接口异常回退 summary 不白屏
- [ ] **关键词**: `flow node inline config drawer` — 节点就地配置抽屉组件抽取，与 BPMN 画布解耦，可跨页面复用
- [ ] **关键词**: `BPMN node optimistic lock` — 节点配置乐观锁基于 updateTime，冲突时 409 + UI 确认覆盖
- [ ] **关键词**: `gateway inline edit` — exclusiveGateway / inclusiveGateway 业务化就地编辑；`isInlineEditable` 判定 = 字段+简单运算符+字面值 且 ≤5 分支；BPMN 表达式作为高级回显
- [ ] **关键词**: `gateway business-bpmn round-trip` — 网关业务化 ↔ BPMN 表达式双向转换；保存后立即回读对比，偏差立即报错
- [ ] **关键词**: `gateway preset` — 同意/驳回/退回/终止 4 个快捷预设；用户一键应用避免手写表达式
- [ ] **关键词**: `form-create -> AiForm adapter` — 适配层支持 10 种基础控件 1:1 映射，未知控件通过 unknownFields 兜底
- [ ] **关键词**: `schema migration log table` — `ai_flow_schema_migration_log` 表记录每个节点的迁移状态，支持重试 + dry-run
- [ ] **关键词**: `feature flag flow.form.engine` — 双引擎切换通过 feature flag，租户级灰度，C7 完成后移除
- [ ] **关键词**: `form-create purge precheck` — C7 前置硬条件自动化检查：迁移 100% + flag 24h 稳定 + 监控指标正常，三者全绿才能 purge
- [ ] **关键词**: `form-create rollback rebuild` — `FormSchemaRebuildFromFormJsonJob` 演练从 form_json 重建 form_schema，C7 上线前必跑一次
- [ ] **关键词**: `form_json column retention` — C7 清理代码后 form_json 列保留一个大版本作兜底，下下轮变更才 DROP COLUMN

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
| --- | --- | --- | --- |
| 工作台接口路径 | `GET /ai/workspace/summary`、`GET /ai/workspace/todo-count` | 现有流程前端和 flow server 均使用 `/api/flow/*` 风格，本轮新增为 `GET /api/workspace/summary`、`GET /api/workspace/todo-count` | 保持现有 API 前缀一致；后续如统一 `/ai/*`，再加兼容转发或调整前端 API |
| 用户偏好默认页 | 新增 `/ai/user/setting/workspace-default-page`，支持 summary/todo 偏好 | 本轮未实现，`/workspace` 固定重定向 `/workspace/summary` | 保留为 A3 剩余项，下一小步补后端 setting 和前端 fallback |
| 菜单迁移 | 隐藏旧 `/flow/todo` 等菜单入口 | 本轮未改数据库菜单资源，新增 `/workspace/*` 已登录白名单和工作台内侧栏 | A5 单独执行，避免无 Flyway/菜单资源脚本的半迁移 |
| 应用中心工作台入口 | 早期 A2 计划显示“我的工作台”按钮和徽标 | 用户反馈与外部工作台菜单重复，本轮在应用中心隐藏工作台按钮和 todo-count 轮询 | 保留 `/workspace/*` 路由和外部菜单，应用中心顶部只保留应用中心/能力中心 |
| 应用中心节点就地配置 | 早期 B1-B8 计划自建节点抽屉/步骤条/网关配置 | 用户要求点击节点配置直接弹真实流程设计器页面 | B1-B8 暂停，改为 `BusinessFlowBindingPanel` 内嵌 `flow/design.vue` |
| 流程变量映射 | 早期页面提供“让流程认识业务字段”配置 | 用户认为无需配置 | 移除 UI；前端自动生成同名映射，后端启动时兜底注入业务字段 |

> /apply 阶段如发现偏差，按此格式记录并同步更新 spec.md。

## 代码质量备忘

- `FlowNodeInlineConfig.vue` / `FlowGatewayInlineConfig.vue` 方案已被本轮用户纠偏暂停；应用中心应打开真实流程设计器，节点配置继续归 BPMN 节点抽屉。
- `formCreateToAiSchema` 适配层建议放在 `components/ai-form/adapters/`，并独立单测；未知字段必须 raise 警告日志（一次性）便于排查。
- `WorkspaceService` 聚合接口注意数据库查询合并（避免 4 次独立 count），用单条 SQL 或缓存兜底。
- 顶部徽标轮询 30s 建议用 `useIntervalFn`（VueUse），页面隐藏时暂停。
- 迁移工具 `FormCreateSchemaMigrationJob` 必须支持 dry-run 模式（先打印迁移计划，不写库）+ batchSize 参数；运维使用安全。
- 步骤条对长流程（>20 节点）滚动 + 搜索 UX 要在 B2 完成时确认；可参考 GitLab CI 流水线视觉。
- 应用中心 + 流程设计器双入口编辑节点，文档里必须明确"以最后保存为准"，避免用户误解为"两个独立配置"。
- 网关 `convertToBpmnExpression` / `parseBpmnExpression` 必须互逆；建议加 round-trip 属性测试（property-based test）。
- C7 清理前的备份 tag (`pre-form-create-purge`) 必须打在 main 分支上，且包含 lockfile；回滚时可整体回退。
- C7 清理后建议运行依赖分析工具（如 `pnpm why @form-create/element-ui`）确认无残留间接引用。
- 工作台偏好接口异常时回退 summary 必须前端兜底，不可阻塞页面加载；后端接口可异步重试。

## Apply 执行记录

### 2026-06-29 — 子迭代 A 工作台底座（A1/A2/A3 核心）

**变更范围**

- 后端：新增 `WorkspaceController`、`WorkspaceService`、`WorkspaceSummaryVO`，并在 `FlowTaskMapper.xml` / `FlowCcMapper.xml` 增加工作台统计 SQL。
- 前端：新增 `BusinessTopNav`、`api/workspace.js`、`views/workspace/*`；应用中心和能力中心接入顶部三项导航；权限守卫允许已登录用户访问 `/workspace/*`。
- 路由：手写 `/workspace` 父子路由，Vite 自动路由排除 `views/workspace`，旧 `/flow/todo` `/flow/done` `/flow/started` `/flow/cc` 保留。

**执行命令与结果**

| 命令 | 结果 | 备注 |
| --- | --- | --- |
| `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow test -DskipITs` | 通过 | Maven 显示 `BUILD SUCCESS`，但该模块测试被 POM 配置跳过：`Tests are skipped`。 |
| `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm test --run` | 失败 | 项目 `test` 脚本已是 `vitest run`，额外 `--run` 被 pnpm 判定为未知参数。 |
| `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm test` | 通过 | `40 passed (40)`，`360 passed (360)`；存在既有 Vue/测试 mock 警告，不阻断。 |
| `mvn -pl forge-flow/forge-flow-server -am compile -DskipTests` | 失败 | 当前 shell Java 版本不支持 target 17：`无效的目标发行版: 17`。 |
| `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-flow/forge-flow-server -am compile -DskipTests` | 通过 | Reactor 到 `forge-flow-server` 全部 `BUILD SUCCESS`。 |
| `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build` | 通过 | `✓ built in 1m 48s`；存在既有动态导入 chunk 和 CSS `//` 注释 warning。 |
| `git diff --check` | 通过 | 无空白错误。 |
| `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec vite --host 127.0.0.1 --port 5174 --strictPort` | 已启动 | 本轮前端 dev server：`http://127.0.0.1:5174/`，PID `6596`。 |

**跳过项**

- 未启动本地后端 / 前端服务，未做浏览器点击验证；本轮以编译、单测、生产构建确认可集成性。
- 未执行真实接口 curl；需要本地数据库、Redis 和登录 token。
- A4 提交成功 toast、A5 菜单数据迁移、用户偏好默认页接口未纳入本轮。

**服务清理**

- 本轮启动的前端 dev server 保留运行，便于用户验证：`http://127.0.0.1:5174/`，PID `6596`。未启动后端服务。

### 2026-06-29 — 用户纠偏修复（6 项反馈 + 3000 workspace 代理）

**变更范围**

- 应用中心代码应用设计器：隐藏“表单字段”面板，仅保留“业务流程配置”。
- 业务流程配置：移除人工变量映射 UI；“打开流程设计器”改为全屏弹窗内嵌真实 `flow/design.vue`。
- 节点配置所有权：应用中心保存时不再写入新的 `nodeForms`，避免覆盖流程设计器节点字段权限。
- 流程变量：前端自动生成业务字段同名映射，后端启动流程时额外把业务记录字段注入为流程变量，并兼容 camel/snake 命名。
- 审批动态表单：待办/已办的节点动态表单改用 `AiForm`，通过 `formCreateToAiSchema` 兼容旧 form-create schema。
- 应用中心顶部：隐藏重复“我的工作台”入口和 todo-count 轮询。
- Vite 代理：新增 `/dev-api/api/workspace` 代理规则，解决 `http://localhost:3000/dev-api/api/workspace/todo-count` 命中前端 404 的问题。

**执行命令与结果**

| 命令 | 结果 | 备注 |
| --- | --- | --- |
| `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/flow/todo.vue src/views/flow/done.vue src/views/flow/design.vue src/views/app-center/components/designer/BusinessFlowBindingPanel.vue src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue 'src/views/app-center/object-designer.[objectCode].vue' src/components/business-top-nav/BusinessTopNav.vue src/components/ai-form/adapters/formCreate.js` | 通过 | 定向覆盖本轮前端改动。 |
| `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 | 覆盖 generator 业务流程服务改动。 |
| `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build` | 通过 | `✓ built in 1m 35s`；存在既有动态导入 chunk 与 CSS warning。 |
| `git diff --check` | 通过 | 无空白错误。 |
| `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec vite --host 127.0.0.1 --port 3000 --strictPort` | 已启动 | `http://127.0.0.1:3000/`，PID `43562`。 |
| `curl -i -s http://127.0.0.1:3000/dev-api/api/workspace/todo-count \| head -n 40` | 通过 | 返回 `HTTP/1.1 200 OK` 和 `{"code":401,"message":"未提供登录凭证"...}`；证明请求已进入后端鉴权链路，不再是前端 404。 |

**跳过项**

- 未执行浏览器点击验证；需要后端服务、登录态和实际流程模型配合验证弹窗内节点配置保存回读。
- 未执行带登录 token 的 workspace 业务数值验证；当前只验证无登录态下代理不再 404。

**服务清理**

- 本段未新启动服务；已有 `http://127.0.0.1:5174/`（PID `6596`）保持运行。
- 已停止 3000 端口旧 Vite 进程（PID `74006`），并重启为 PID `43562`，用于用户继续验证。

### 2026-06-30 — 采购待办表单渲染与字段权限修复

**变更范围**

- 待办审批：`business-code` 表单优先使用其 `formUrl` 指向的业务组件渲染，采购审批走 `/business/purchase-order-test` 专用任务表单，不再被通用 `AiForm` 摊平成字段清单。
- 前端权限：`useBusinessTaskFormContext` 以后台返回的 `fields` 清单作为字段显示/编辑边界，未返回的字段默认不可见不可编辑。
- 后端权限：业务对象和代码表单在存在显式字段权限时，未列入权限的字段默认不可见；无权限配置时保持旧的全量只读兜底。

**执行命令与结果**

| 命令 | 结果 | 备注 |
| --- | --- | --- |
| `git diff --check -- forge-admin-ui/src/views/flow/todo.vue forge-admin-ui/src/composables/useBusinessTaskFormContext.js forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java` | 通过 | 无空白错误。 |
| `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/flow/todo.vue src/composables/useBusinessTaskFormContext.js` | 通过 | 定向覆盖本轮前端改动。 |
| `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 失败 | 当前 shell Java 版本不支持 target 17：`无效的目标发行版: 17`。 |
| `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 | `BUILD SUCCESS`；存在既有 deprecation / unchecked 编译提示，不阻断。 |

**跳过项**

- 未启动前后端做采购流程页面点击验证；需要本地登录态、后端服务和一条处于待办节点的采购单实例。
- 未执行全量 `pnpm build`；本轮只改待办渲染分支和字段权限上下文，已做定向 ESLint 与后端目标模块编译。

**服务清理**

- 本轮未启动新的服务，无需清理。
