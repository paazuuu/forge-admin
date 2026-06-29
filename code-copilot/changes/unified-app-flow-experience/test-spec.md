# 单测 Spec — 业务应用与流程模块体验整合
> status: applied-partial-validation
> created: 2026-06-29

## 0. 测试原则

- **Red/Green TDD**：测试先 Red 再 Green，跳过 Red 的测试无法证明有效。
- **First Run the Tests**：开始前先跑已有测试套件（`forge-plugin-generator`、`forge-admin-ui` vitest），了解框架和基线。
- **展示工作**：必须展示 `mvn test` / `pnpm test` 的实际输出，禁止"测试通过"等无证据声明。
- **增量复用**：复用 `unified-business-flow-app-config` 的现有测试，本轮只补差异（字段权限消费、列表中文名渲染、卡片选择器、反查接口）。
- **试金石**：以采购单审批样例（`SamplePurchaseOrder*`）作为端到端用例锚点。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit 版本 | JUnit 5 (Jupiter) |
| Mock 框架 | Mockito 5 |
| 后端测试位置 | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/.../businessapp/` |
| 后端已有测试 | `BusinessFlowServiceTest`、`BusinessFlowAppConfigServiceTest`、`BusinessObjectReadinessServiceTest`（待跑命令确认） |
| 前端测试框架 | Vitest + @vue/test-utils（待跑命令确认） |
| 前端测试位置 | `forge-admin-ui/src/components/ai-form/__tests__/` 等（如不存在则首次建立） |

> 任务 0：执行 `cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator test -DskipITs` 与 `cd forge-admin-ui && pnpm test --run`，确认基线绿灯，写入 §4 历史验证基线表。

## 2. 覆盖范围

### P0 — 核心业务逻辑（必须覆盖）

#### 类名: AiForm.vue（前端组件）

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|
| 渲染 | 未传 `field-permissions` | schema=[a,b,c] | — | 三个字段都渲染，editable=true |
| 渲染 | `visible=false` | schema=[a,b,c]，perm a.visible=false | — | 只渲染 b,c |
| 渲染 | `editable=false` | schema=[a,b]，perm a.editable=false | — | a 渲染但 disabled=true |
| 渲染 | `required=true` | schema=[a]，perm a.required=true | — | a 必填红星，提交校验不通过 |
| 渲染 | 三态混合 | schema=[a,b,c]，a 不可见、b 只读、c 必填 | — | a 隐藏、b 灰、c 红星 |
| 渲染 | schema 已有 required=true，perm required=false | — | — | required 取 OR（schema 兜底） |

#### 类名: getRowDisplayTitle（前端工具）

| 方法 | 场景 | 输入 | 预期结果 |
|------|------|------|---------|
| getRowDisplayTitle | 有 businessSummary | `{businessSummary:'采购单 PO001'}` | '采购单 PO001' |
| getRowDisplayTitle | 仅有 processName | `{processName:'采购单审批'}` | '采购单审批' |
| getRowDisplayTitle | 仅有 taskName | `{taskName:'部门负责人审批'}` | '部门负责人审批' |
| getRowDisplayTitle | 仅有 key | `{processDefinitionKey:'sample_purchase_order'}` | 'sample_purchase_order' |
| getRowDisplayTitle | 全空 | `{}` | '-' |

#### 类名: BusinessFlowService（后端）

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|
| listBusinessBindingsByModelKey | 0 绑定 | modelKey=`other_flow` | bindingMapper.selectList → 空 | 空数组 |
| listBusinessBindingsByModelKey | 1 绑定 | modelKey=`sample_purchase_order` | mapper 返回 1 条 | 1 个 VO，含 objectCode/objectName/codeApp |
| listBusinessBindingsByModelKey | N 绑定 | — | mapper 返回 3 条 | 3 个 VO 按 objectCode 升序 |
| getFormAssets | 字段数与预览填充 | objectCode=`sample_purchase_order` | Provider 返回 12 字段 | VO.fieldCount=12，fieldPreview 长度=5 |
| getFormAssets | internal 字段过滤 | — | Provider 字段含 `id`(internal=true) | VO.fields 不含 internal=true 字段（默认） |
| buildTaskFormContext | businessSummary 回填 | 任务 ID + 业务记录 | Provider buildSummary → "采购单 PO001" | VO.businessSummary='采购单 PO001' |

#### 类名: BusinessCodeFormProviderRegistry（后端）

| 方法 | 场景 | 预期结果 |
|------|------|---------|
| listAssets | 字段含 internal 标志 | 旧 Provider（不重载 field()）字段 internal=false |
| listAssets | 新 Provider 显式 internal | SamplePurchaseOrderCodeFormProvider 的 `id` 字段 internal=true |

### P1 — 数据访问层 / 控制器

#### 类名: BusinessFlowController（后端）

| 接口 | 场景 | 预期 |
|------|------|------|
| GET /flow/model/{modelKey}/business-bindings | 已绑定 | 200，返回数组 |
| GET /flow/model/{modelKey}/business-bindings | 未绑定 | 200，空数组 |
| GET /flow/model/{modelKey}/business-bindings | modelKey 为空 | 400 |
| GET /flow/form-assets/{objectCode} | 含 internal 字段 | 默认返回 fields 不含 internal |
| GET /flow/form-assets/{objectCode}?includeInternal=true | 含 internal 字段 | 返回完整字段（含 internal）|

### P2 — 入口层 / 服务层

#### 类名: todo.vue / done.vue（前端集成）

| 场景 | 步骤 | 预期 |
|------|------|------|
| 待办列表中文渲染 | mock 接口返回 processName='采购单审批' | 列表 #title 显示中文 |
| 待办抽屉无技术标签 | mock 业务托管表单上下文 | 不出现"代码业务"tag |
| 待办字段权限生效 | mock fieldPermissions 含 visible=false | 对应字段不渲染 |
| 已办强制只读 | mock done.vue 渲染 | 所有字段 disabled |

#### 类名: ApproverConfig.vue + BusinessFlowFormAssetSelect.vue（前端集成）

| 场景 | 步骤 | 预期 |
|------|------|------|
| 节点资产改卡片 | 打开节点抽屉 → 表单权限 tab | 渲染卡片而非下拉 |
| 卡片选中切换字段权限矩阵 | 选中代码 Provider 资产 | 字段矩阵展示 Provider fields |

### 不测试（明确列出原因）

- form-create 引擎 (`FlowFormCreateRenderer.vue`) 字段权限消费：已在 `unified-business-flow-app-config` 覆盖，本轮不重复。
- 业务运行时 `AiCrudPage.vue` 中的 `<AiForm>`：不传 `field-permissions`，行为保持，不在本轮回归。
- 流程引擎自身（Flowable）：本变更不修改引擎。
- 菜单分组的 SSR / 权限边界：Task 8 只做前端可见性，权限模型如未扩展则不覆盖后端权限单测。

## 3. 执行计划

- [ ] **Step 0**：运行已有后端测试 + 前端测试，确认基线，记入 §4。
  ```
  cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator test -DskipITs
  cd forge-admin-ui && pnpm test --run
  ```
- [ ] **Step 1**：Task 1 / 2 / 3（P0）— 写测试先 Red：
  - `AiForm` 字段权限消费用例（Vitest）
  - `getRowDisplayTitle` 工具函数用例
  - `BusinessCodeFormField` `internal` 字段标志单测
- [ ] **Step 2**：实现 Task 1 / 2 / 3 → 让上述用例 Green → 跑全量后端 + 前端测试。
- [ ] **Step 3**：Task 4 / 5（P1）— 反查接口与上下文注入：
  - `BusinessFlowController#listBusinessBindingsByModelKey` Controller + Service 双层用例。
- [ ] **Step 4**：Task 6 / 7（P1）— 卡片选择器 + 代码应用面板：
  - `BusinessFlowFormAssetVO` 新字段后端单测。
  - 前端集成测试 `ApproverConfig.vue` 卡片渲染。
- [ ] **Step 5**：Task 8（P2）— 菜单弱化只跑前端冒烟（unit 不强求）。
- [ ] **Step 6**：Task 9 — 端到端：本地启动 admin + flow + ui，跑完整采购单审批流程，截图 / 录屏写入 `execution-log.md`。
- [ ] **Step 7**：跑全量测试套件，确认无回归；提交前 `git diff --check`。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-06-29 | 后端 forge-plugin-generator 编译基线 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 | 首次未指定 JDK 17 时失败，报 `无效的目标发行版: 17`；切换 JDK 17 后 `BUILD SUCCESS`。 |
| 2026-06-29 | 前端 forge-admin-ui 构建基线 | `/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build"` | 通过 | 构建耗时约 3m46s；仅有既有 dynamic/static import chunking 和 CSS `//` 注释类 warning。 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-06-29 | 全部实现文件 | 空白/冲突检查 | `git diff --check` | 通过 | 无空白错误。 |
| 2026-06-29 | 后端 Task 3/4/6/7 | Maven 编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests` | 通过 | 仅有既有 deprecation/unchecked warning；未跑 JUnit。 |
| 2026-06-29 | 前端 Task 1/2/5/6/7/8 | 生产构建 | `/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build"` | 通过 | 仅有既有构建 warning；未跑 Vitest。 |
| 2026-06-29 | 路由迁移 | 旧 URL 残留搜索 | `rg "/app-center/object-designer|object-designer/sample_purchase_order" forge-admin-ui/src forge-server -g '*.*'` | 通过 | 仅剩 `router/index.js` 对旧组件文件的兼容 import；未发现旧 URL 使用。 |
| 2026-06-29 | Task 8 菜单隐藏 | 代码核对 | `rg "requireRole|流程库管理" forge-admin-ui/src forge-server -g '*.*'` | 部分通过 | 当前菜单处理链只支持后端资源的 `visible/menuStatus`，未实现 `requireRole`；本轮只完成页面 Banner。 |
| 2026-06-29 | 端到端采购单审批 | 本地服务联调 | 未执行 | 跳过 | 未启动 `forge-admin-server` / `forge-flow` / `forge-admin-ui`，需后续 `/test` 执行完整链路。 |

## 6. 执行证据

- `execution-log.md`：每个 Task 完成后追加"踩坑 / 决策 / 验证"。
- 关键接口验证：
  - `GET /ai/business/flow/model/sample_purchase_order/business-bindings` → 返回采购申请绑定。
  - `GET /ai/business/flow/form-assets/sample_purchase_order` → 返回 fields（不含 internal）+ fieldCount。
  - `GET /ai/business/flow/task-form-context?taskId=xxx` → 返回 businessObjectName + businessSummary + fieldPermissions。
- 关键数据库检查：
  - `SELECT object_code, code_app, flow_model_key FROM ai_business_binding WHERE flow_model_key='sample_purchase_order';`
- 服务启动与停止：
  - `cd forge && mvn -pl forge-admin-server spring-boot:run -Dspring-boot.run.profiles=dev`
  - `cd forge && mvn -pl forge-flow spring-boot:run -Dspring-boot.run.profiles=dev`
  - `cd forge-admin-ui && pnpm dev`
- 端到端手测路径：
  1. `/login` → 业务用户
  2. `/app-center` → 采购 → 采购申请 → 提交一单（业务表单视觉应与 P0 修复后的待办视觉一致）
  3. `/flow/todo` → 列表中文流程名 + 业务摘要
  4. 点开 → 顶部无"代码业务"tag → `needDate` 在"申请人修改"节点可编辑/必填，在"部门负责人审批"节点灰显
  5. 同意 → 流转 → 工程经理会签 → 驳回 → 申请人修改 → 重新提交 → 通过
  6. `/flow/done` → 所有字段灰显（只读）
  7. `/app-center/object/sample_purchase_order/designer` → "表单字段"面板 → 选择节点查看权限矩阵（只读）
  8. `/flow/model` → 列表显示绑定业务应用 → 点设计 → 顶部 Banner → 选节点 → 卡片资产选择
