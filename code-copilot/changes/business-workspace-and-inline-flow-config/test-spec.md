# 单测 Spec — 我的工作台 + 节点就地配置 + 渲染引擎收敛
> status: propose
> created: 2026-06-29

## 0. 测试原则

- **Red/Green TDD**：测试先 Red 再 Green，跳过 Red 的测试无法证明有效。
- **First Run the Tests**：开始前先跑已有测试套件（后端 `forge-plugin-generator` / `forge-plugin-flow`、前端 vitest），记入基线。
- **展示工作**：必须展示 `mvn test` / `pnpm test` 实际输出，禁止"测试通过"等无证据声明。
- **增量复用**：复用第一轮 `unified-business-flow-app-config` 与第二轮 `unified-app-flow-experience` 已有测试，本轮只补差异。
- **子迭代独立验收**：A / B / C 各自有独立验收清单，互不阻塞。
- **试金石**：采购单审批样例（绑定业务对象）+ 独立请假审批样例（未绑定业务对象）。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit 版本 | JUnit 5 (Jupiter) |
| Mock 框架 | Mockito 5 |
| 后端测试位置 | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/.../` |
| 后端已有测试 | `BusinessFlowServiceTest` / `BusinessFlowAppConfigServiceTest`（待跑命令确认） |
| 前端测试框架 | Vitest + @vue/test-utils（待跑命令确认） |
| 视觉回归 | Playwright + Vitest（如本项目已有则复用，否则纳入 C6 评估） |

> 任务 0：执行
> ```
> cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator test -DskipITs
> cd forge-admin-ui && pnpm test --run
> ```
> 确认基线绿灯，写入 §4。

## 2. 覆盖范围

### 子迭代 A — 工作台

#### P0 — 后端 WorkspaceService

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|
| summary | 普通用户 | userId=1 | 各 mapper 返回固定值 | VO.todoCount=3, doneWeekCount=5, startedRunningCount=2, ccUnreadCount=1 |
| summary | 空数据 | userId=999 | mapper 返回 0 | VO 全 0 |
| todoCount | 普通用户 | userId=1 | mapper count=3 | 3 |
| todoCount | 缓存 | 短时间多次调用 | — | 第二次走缓存（如设计了缓存） |

#### P0 — 前端 workspace 路由 + 顶部导航

| 场景 | 步骤 | 预期 |
|------|------|------|
| 路由兼容 | 访问 `/flow/todo` | 仍可正常加载 |
| 工作台默认页 | 访问 `/workspace` | 重定向到 `/workspace/summary` |
| 顶部导航三项 | superadmin 登录 | 看到三个按钮 |
| 顶部导航两项 | 普通用户登录 | 不显示"能力中心" |
| 徽标更新 | mock todo-count=5 | 顶部徽标显示 5 |
| 徽标轮询 | mock 30s 后 todo-count=10 | 徽标更新为 10 |

#### P1 — 应用中心提交 toast

| 场景 | 步骤 | 预期 |
|------|------|------|
| 提交成功触发 toast | mock AiCrudPage 提交成功事件 | toast 出现 + 含"查看流转"按钮 |
| 提交失败不显示 | mock 提交失败 | toast 不出现 |
| 点击 toast 跳转 | 点击"查看流转" | router 跳 `/workspace/started` |

#### P1 — 工作台默认页用户偏好

| 场景 | 步骤 | 预期 |
|------|------|------|
| 默认偏好 summary | mock 偏好接口返回 summary | `/workspace` 重定向到 `/workspace/summary` |
| 偏好 todo | mock 偏好返回 todo | `/workspace` 重定向到 `/workspace/todo` |
| 偏好接口异常 | mock 偏好接口 500 | 回退 summary，不白屏 |
| 修改偏好 | PUT 偏好 = todo | 下次进入重定向到 todo |

### 子迭代 B — 节点就地配置

#### P0 — 后端节点配置 API

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|
| getNodeConfig | 正常 | modelKey + nodeId | 节点存在 | VO 含审批人 / 表单 / 字段权限 / updateTime |
| getNodeConfig | 节点不存在 | 错误 nodeId | mapper 返回 null | 404 |
| saveNodeConfig | 并发冲突 | expectedUpdateTime=旧值 | 实际 updateTime 已变化 | 409 Conflict |
| saveNodeConfig | 正常保存 | expectedUpdateTime=当前值 | — | 200 + 新 VO，updateTime 已递增 |
| listInlineNodes | 仅返回人工节点 | modelKey=sample_purchase_order | 含 userTask + serviceTask 节点 | 只返回 userTask |
| listInlineNodes | configured 判断 | 节点配置完整 vs 缺审批人 | — | configured 字段对应 true/false |

#### P0 — 前端 FlowNodeInlineConfig 组件

| 场景 | 步骤 | 预期 |
|------|------|------|
| 抽屉打开取配置 | mount + show=true | 调用 GET 节点配置接口 |
| 保存成功 | 编辑后点保存 | 调用 PUT + 触发 save 事件 |
| 409 冲突弹确认 | mock PUT 返回 409 | 弹出确认框"是否覆盖" |
| 覆盖后重试 | 确认覆盖 | 重新 PUT（用新 updateTime） |
| 自动节点只读 | nodeType=serviceTask | 抽屉显示只读说明 + 链接到完整设计器 |

#### P1 — 步骤条 + 应用中心嵌入

| 场景 | 步骤 | 预期 |
|------|------|------|
| 步骤条加载 | mount with modelKey | 调用 inline-nodes 接口 |
| 长流程滚动 | mock 25 节点 | 容器内滚动而非页面滚动 |
| 配置完成度显示 | 节点 configured=true | 绿色对勾 |
| 配置缺失提示 | configured=false | 灰色感叹号 + 悬浮提示具体缺失 |
| 选中节点弹抽屉 | 点击步骤 | drawerShow=true + 节点 ID 透传 |
| 应用中心打开完整设计器 | 点击"高级编辑" | router 跳 `/flow/design` + source=appCenter |
| 步骤条展示网关节点 | mock 含 exclusiveGateway 节点 | 菱形节点出现，标记 inlineEditable |
| tab 视觉骨架 | 进入业务流程 tab | 顶部 4 步骤进度可见 |

#### P0 — 网关业务化就地配置（B8）

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|
| getConfig | 简单网关 | exclusiveGateway 含 3 个出向条件 | — | VO.inlineEditable=true，outgoingConditions 长度=3 |
| getConfig | 复杂网关 | 含复合表达式 | — | VO.inlineEditable=false + reason 说明 |
| saveConfig | 同意预设 | preset=approve | — | BPMN `<conditionExpression>` = `${approvalResult == 'approve'}` |
| saveConfig | 驳回预设 | preset=reject | — | BPMN 表达式 = `${approvalResult == 'reject'}` |
| saveConfig | 自定义条件 | sourceField=amount, op='>', value=10000 | — | BPMN 表达式 = `${amount > 10000}` |
| saveConfig | 并发冲突 | expectedUpdateTime=旧值 | — | 409 Conflict |
| convertToBpmnExpression | 双向转换语义等价 | DTO → BPMN → DTO | — | 来回转换不丢失语义 |
| listPresets | 4 个预设 | — | — | 同意 / 驳回 / 退回 / 终止 都返回 |
| isInlineEditable | 分支 > 5 | 6 个出向条件 | — | false + reason="分支数超过 5" |
| isInlineEditable | 含函数调用 | 表达式 `${calc(a)}` | — | false + reason="表达式含函数调用" |

#### P1 — 网关就地配置 UI

| 场景 | 步骤 | 预期 |
|------|------|------|
| 简单网关可编辑 | mock inlineEditable=true | 抽屉显示业务化条件表单 |
| 复杂网关不可编辑 | inlineEditable=false | 抽屉显示禁用提示 + 跳完整设计器按钮 |
| 应用快捷预设 | 点击"同意路由" | 出向条件填充 approvalResult == 'approve' → 下一节点 |
| BPMN 表达式高级编辑 | 展开"高级设置" | 显示 raw 表达式列表，可手写覆盖 |

### 子迭代 C — 渲染引擎收敛

#### P0 — schema 适配层 formCreateToAiSchema

| 输入控件 | form-create JSON | 预期 AiForm field |
|---|---|---|
| input | `{type:'input', field:'name', title:'姓名'}` | `{code:'name', label:'姓名', type:'string'}` |
| number | `{type:'inputNumber', field:'age'}` | `{code:'age', type:'number'}` |
| select | `{type:'select', options:[{label,value}]}` | `{type:'select', options}` |
| radio | `{type:'radio', options}` | `{type:'radio', options}` |
| checkbox | `{type:'checkbox'}` | `{type:'checkboxGroup'}` |
| date | `{type:'datePicker'}` | `{type:'date'}` |
| datetime | `{type:'datePicker', props:{type:'datetime'}}` | `{type:'datetime'}` |
| switch | `{type:'switch'}` | `{type:'switch'}` |
| upload | `{type:'upload'}` | `{type:'upload'}` |
| 未知 | `{type:'customSignature'}` | unknownFields 数组含此项 + 主 schema 占位 |
| 嵌套 layout | grid 内有 2 个 input | 扁平化为 2 个 field，丢弃 layout（或附 `meta.layout`） |
| validate | `{validate:[{required:true}]}` | `{required: true}` |
| 必填 + max | `{validate:[{required:true, max:50}]}` | `{required:true, maxLength:50}` |

#### P0 — 节点动态表单切换 AiForm

| 场景 | 步骤 | 预期 |
|------|------|------|
| 默认走 AiForm | feature flag = ai | todo.vue 渲染 AiForm + 适配 schema |
| 降级走 form-create | feature flag = formCreate | 渲染 FlowFormCreateRenderer |
| 字段权限三态 (AiForm) | visible/editable/required | 三态正确呈现 |
| 字段权限三态 (form-create) | 同上 | 行为等价 |

#### P0 — schema 迁移工具

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|
| migrateModel | 全部成功 | modelKey=sample | 3 节点都有 form_json | report.success=3, failed=0 |
| migrateModel | 部分失败 | — | 1 节点含未知组件 | report.success=2, failed=1, failedNodeIds 含该节点 |
| migrateModel | 幂等 | 已迁移过的 modelKey | — | 跳过已迁移节点，success=0 |
| migrateModel | dry-run | dryRun=true | — | 不写库，log 表插入 dry_run=true 记录 |
| migrateModel | 批次大小 | batchSize=2 | 5 节点 | 分 3 批处理，每批最大 2 节点 |
| migrateAll | 多模型 | — | 5 个模型 | 累计 report |

#### P0 — C7 form-create 清理前置检查

| 方法 | 场景 | Mock 行为 | 预期结果 |
|------|------|-----------|---------|
| FormCreatePurgePrecheckJob.check | 三项全绿 | 迁移 100% + flag 24h + 指标正常 | canPurge=true, blockReasons=[] |
| FormCreatePurgePrecheckJob.check | 迁移未完成 | 还有 form_json 未迁移 | canPurge=false, blockReasons 含 "migration not fully completed" |
| FormCreatePurgePrecheckJob.check | flag 不足 24h | flag 开启 1h | canPurge=false, blockReasons 含 "feature flag not stable" |
| FormCreatePurgePrecheckJob.check | 错误率超阈值 | mock 错误率 = 1% | canPurge=false, blockReasons 含 "error rate exceeds threshold" |

#### P0 — C7 回滚预案演练

| 方法 | 场景 | 预期 |
|------|------|------|
| FormSchemaRebuildFromFormJsonJob.rebuild | 测试环境单模型 | 从 form_json 重建 form_schema，与 C4 迁移结果 schema 等价 |
| FormSchemaRebuildFromFormJsonJob.rebuildAll | 全量重建 | 所有模型重建成功，无字段丢失 |

#### P1 — 通用流程表单设计器迁移

| 场景 | 步骤 | 预期 |
|------|------|------|
| /flow/form 加载 | 进入页面 | 显示 AiForm 设计器（与业务对象设计器一致） |
| /flow/template 保存 | 编辑 + 保存 | schema 落入 form_schema 列 |

#### P1 — 视觉回归

| 场景 | 步骤 | 预期 |
|------|------|------|
| 采购单待办 vs 业务运行时 | playwright 截图 | 像素差 < 1% |
| 独立流程待办 vs 业务运行时 | playwright 截图 | 像素差 < 1% |
| 字段权限灰显视觉 | mock editable=false | 灰显样式一致 |

### 不测试（明确列出原因）

- BPMN 引擎自身：本变更不修改 Flowable。
- 工作台首页"我的草稿"占位：本轮未实现，下一轮覆盖。
- 复杂网关（事件网关 / 并行网关编辑）：本轮 B8 仅 exclusiveGateway / inclusiveGateway 可编辑。
- 移动端 / App 端：本变更聚焦 Web 后台。
- form-create 卸载后的依赖深度审计：C7 完成后 pnpm/npm 包是否还有间接引用，由依赖分析工具兜底，不纳入单测。

## 3. 执行计划

- [ ] **Step 0**：跑基线
  ```
  cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator test -DskipITs
  cd forge-admin-ui && pnpm test --run
  ```
- [ ] **Step 1（A）**：A1 路由 → A2 顶部导航 → A3 聚合首页 + 徽标 → A4 toast → A5 旧入口下线；每个 Task 写测试先 Red 后 Green。
- [ ] **Step 2（A 发布检查）**：完成 A 发布检查清单（tasks.md）。
- [ ] **Step 3（B）**：B1 抽组件 → B2 步骤条 + 后端摘要 → B3 节点 GET/PUT → B4 应用中心嵌入 → B5 冲突 UI → B6 完成度。
- [ ] **Step 4（B 发布检查）**：双 tab 并发测试 + 长流程滚动测试。
- [ ] **Step 5（C1-C6）**：C1 适配层 → C3 迁移工具（含 dry-run）→ C2 feature flag → C4 通用表单设计器 → C5 deprecated → C6 视觉回归。
- [ ] **Step 6（C1-C6 发布检查）**：feature flag 灰度方案 + 截图回归确认。
- [ ] **Step 7（C7 清理）**：自动化前置检查脚本 → 三项硬条件全绿 → 回滚预案演练 → 删除代码 → 全量构建 + 测试 → 7 天观察期。
- [ ] **Step 8**：端��端联调（采购单 + 独立请假流程）。
- [ ] **Step 9**：跑全量测试，确认无回归；`git diff --check`。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 待填 | 后端 forge-plugin-generator | `mvn -pl ... test` | 待填 | Step 0 |
| 待填 | 后端 forge-plugin-flow | `mvn -pl ... test` | 待填 | Step 0 |
| 待填 | 前端 forge-admin-ui | `pnpm test --run` | 待填 | Step 0 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 待填 | A WorkspaceService | mvn test | `mvn -pl ... -Dtest=WorkspaceServiceTest test` | 待填 | — |
| 待填 | A 工作台路由 | vitest | `pnpm test --run workspace` | 待填 | — |
| 待填 | A 用户偏好默认页 | vitest + mvn test | `pnpm test --run workspaceDefaultPage` + `mvn ... UserSettingServiceTest` | 待填 | — |
| 待填 | B 节点配置 API | mvn test | `mvn -pl ... -Dtest=BusinessFlowNode*Test test` | 待填 | — |
| 待填 | B 共享抽屉组件 | vitest | `pnpm test --run FlowNodeInlineConfig` | 待填 | — |
| 待填 | B 步骤条 | vitest | `pnpm test --run FlowNodeStepList` | 待填 | — |
| 待填 | B8 网关 Service | mvn test | `mvn -pl ... -Dtest=BusinessFlowGateway*Test test` | 待填 | 双向转换关键 |
| 待填 | B8 网关 UI | vitest | `pnpm test --run FlowGatewayInlineConfig` | 待填 | — |
| 待填 | C 适配层 | vitest | `pnpm test --run formCreate` | 待填 | — |
| 待填 | C 迁移工具 | mvn test | `mvn -pl ... -Dtest=FormCreateSchemaMigration*Test test` | 待填 | dry-run 必跑 |
| 待填 | C 视觉回归 | playwright | `pnpm test:visual` | 待填 | 截图基线首次产出 |
| 待填 | C7 前置检查 | mvn test | `mvn -pl ... -Dtest=FormCreatePurgePrecheckJobTest test` | 待填 | 三项硬条件覆盖 |
| 待填 | C7 回滚演练 | mvn test + 测试库 | 测试环境执行 `FormSchemaRebuildFromFormJsonJob` | 待填 | execution-log 记录差异 |
| 待填 | 端到端采购单 | 手动 | — | 待填 | execution-log 录屏 |
| 待填 | 端到端独立流程 | 手动 | — | 待填 | execution-log 录屏 |

## 6. 执行证据

- `execution-log.md`：每个 Task 完成后追加"踩坑 / 决策 / 验证"。
- 关键接口验证：
  - `GET /ai/workspace/summary` → 返回 4 个聚合数。
  - `GET /ai/workspace/todo-count` → 返回数字（轻量）。
  - `GET/PUT /ai/user/setting/workspace-default-page` → 用户偏好读写。
  - `GET /ai/business/flow/node/{modelKey}/{nodeId}/config` → 返回节点配置 + updateTime。
  - `PUT /ai/business/flow/node/{modelKey}/{nodeId}/config` → 200 / 409。
  - `GET /ai/business/flow/model/{modelKey}/inline-nodes` → 人工节点 + 网关节点（含 inlineEditable）。
  - `GET /ai/business/flow/gateway/{modelKey}/{nodeId}/config` → 网关业务化条件 + isInlineEditable。
  - `PUT /ai/business/flow/gateway/{modelKey}/{nodeId}/config` → 保存网关条件，回读校验语义等价。
  - `GET /ai/business/flow/gateway/presets` → 4 个快捷预设。
  - `POST /ai/business/flow/schema/migration/{modelKey}?dryRun=true` → dry-run 报告。
  - `POST /ai/business/flow/schema/migration/{modelKey}` → 正式迁移报告。
- 关键数据库检查：
  - `DESCRIBE ai_flow_model_node;` → 确认 `form_schema` / `gateway_inline_config` 列存在。
  - `SELECT * FROM ai_flow_schema_migration_log WHERE status='failed';` → 失败节点。
  - `SELECT COUNT(*) FROM ai_flow_model_node WHERE form_json IS NOT NULL AND form_schema IS NULL;` → C7 前置必须 = 0。
  - `SELECT * FROM ai_user_setting WHERE setting_key='workspace.default_page';` → 用户偏好。
- 服务启动：
  - `cd forge && mvn -pl forge-admin-server spring-boot:run -Dspring-boot.run.profiles=dev`
  - `cd forge && mvn -pl forge-flow spring-boot:run -Dspring-boot.run.profiles=dev`
  - `cd forge-admin-ui && pnpm dev`
- 端到端手测路径（A+B+C 全部完成后）：
  1. `/login` → 普通业务用户
  2. 顶部出现"应用中心 / 我的工作台"两个按钮（无能力中心）
  3. `/app-center` → 采购 → 采购申请 → 提交一单 → 顶部 toast "已提交 · 查看流转"
  4. 点击 toast → `/workspace/started` → 显示该单
  5. 切换 superadmin → 顶部出现"能力中心"
  6. `/app-center/object/sample_purchase_order/designer` → 业务流程 tab → 顶部 4 步进度条 → 步骤条 → 选中人工节点 → 抽屉就地编辑 → 保存
  7. 选中网关节点 → 抽屉显示业务化条件 + 4 个快捷预设 → 应用"驳回路由" → 保存 → 验证 BPMN `<conditionExpression>` 等价
  8. 同时打开 `/flow/design?id=...` → 验证看到的配置（含网关条件）与应用中心保存的一致
  9. 双 tab 并发编辑同一节点 → 第二次保存 409 → 选择覆盖 → 成功
  10. `/workspace/todo` → 待办列表（AiForm 渲染） → 字段权限三态生效
  11. （C1-C6 完成后）切换 feature flag `flow.form.engine=formCreate` → 重新打开待办 → form-create 渲染 → 切回 ai → 视觉一致
  12. （C7 完成后）feature flag 配置项已移除；form-create 目录不存在；测试环境跑回滚演练 `FormSchemaRebuildFromFormJsonJob` → 重建结果等价
