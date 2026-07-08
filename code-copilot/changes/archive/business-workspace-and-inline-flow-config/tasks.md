# 任务拆分 — 我的工作台 + 节点就地配置 + 渲染引擎收敛

> 拆分顺序：数据模型 → 接口协议 → 底层实现 → 上层编排 → 入口层
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 子迭代 A / B / C 互相独立，任一可单独发布
> 建议发布顺序：A（工作台） → B（节点就地） → C（渲染引擎收敛）

## 前置条件

- [ ] 第二轮变更 `unified-app-flow-experience` P0（AiForm 字段权限 / 列表中文名 / 移除技术标签）已落地，本变更基于其底座。
- [ ] 用户确认 `spec.md` §9 六个待澄清项的结论。
- [ ] 用户确认子迭代发布顺序与节奏。
- [ ] 本地能跑通 admin + flow + ui，并能进入采购单审批样例与至少一个独立流程审批样例。

---

## 子迭代 A — 我的工作台产品概念（建议 1-2 周交付）

### Task A1: 工作台路由与目录骨架

- **目标**：新建 `/workspace/*` 路由组与 `views/workspace/` 目录，保留旧 `/flow/*` 路由作为兼容。
- **涉及文件**:
    - `forge-admin-ui/src/router/index.js` — 增加 `/workspace` 路由组（summary / todo / done / started / cc）。
    - 新建 `forge-admin-ui/src/views/workspace/index.vue` — 工作台壳（左侧菜单 + 右侧 router-view）。
    - 新建 `forge-admin-ui/src/views/workspace/summary.vue` — 聚合首页占位。
    - 新建 `forge-admin-ui/src/views/workspace/todo.vue` / `done.vue` / `started.vue` / `cc.vue` — 占位 `<script setup>` 导入旧 `/flow/*` 同名页面，保持功能不变。
- **关键签名**:
  ```js
  {
    path: '/workspace',
    component: () => import('@/views/workspace/index.vue'),
    children: [
      { path: '', redirect: '/workspace/summary' },
      { path: 'summary', component: () => import('@/views/workspace/summary.vue') },
      { path: 'todo', component: () => import('@/views/workspace/todo.vue') },
      ...
    ]
  }
  ```
- **验收**:
  - 访问 `/workspace/todo` 与 `/flow/todo` 行为一致（功能不丢）。
  - 工作台壳有左侧菜单（4 项 + 首页）。
  - 旧路由 `/flow/todo` 仍可访问（兼容）。

### Task A2: 顶部导航三级重组

- **目标**：顶部增加"我的工作台"按钮，且仅对非 `superadmin` 用户隐藏"能力中心"。
- **涉及文件**:
    - `forge-admin-ui/src/layout/components/AppHeader.vue`（或实际顶部组件，待启动时再核对）— 增加 `<button>` 我的工作台。
    - `forge-admin-ui/src/views/app-center/index.vue:9` — 复用同一组件抽取顶部到布局。
    - 权限判断：`useUserRoleStore()` 检查 `superadmin`。
- **关键签名**:
  ```vue
  <nav class="app-top-nav">
    <NButton text @click="goto('/app-center')">应用中心</NButton>
    <NButton text @click="goto('/workspace')">
      我的工作台
      <NBadge v-if="todoCount > 0" :value="todoCount" :max="99" />
    </NButton>
    <NButton v-if="isSuperAdmin" text @click="goto('/app-center/engines')">能力中心</NButton>
  </nav>
  ```
- **验收**:
  - 顶部出现三个按钮；普通用户看不到"能力中心"。
  - "我的工作台"按钮显示徽标（暂以静态数字占位，等 A3 接入接口）。
- **依赖**: A1。

### Task A3: 工作台聚合首页 + 后端接口

- **目标**：`/workspace/summary` 展示 4 个聚合卡片；新增后端聚合接口与徽标轮询接口。
- **涉及文件**:
    - 新增 `forge-server/.../controller/WorkspaceController.java` — `GET /workspace/summary`、`GET /workspace/todo-count`。
    - 新增 `forge-server/.../service/WorkspaceService.java` — 聚合查询（待办数 / 本周已办 / 我发起进行中 / 抄送我未读）。
    - 新增 `forge-admin-ui/src/api/workspace.js` — 前端 API。
    - `forge-admin-ui/src/views/workspace/summary.vue` — 渲染 4 个 `<n-card>` 卡片。
    - `forge-admin-ui/src/layout/components/AppHeader.vue` — 接入徽标接口，30s 轮询。
- **关键签名**:
  ```java
  @GetMapping("/workspace/summary")
  public R<WorkspaceSummaryVO> summary();

  @GetMapping("/workspace/todo-count")
  public R<Long> todoCount();

  public class WorkspaceSummaryVO {
    private Long todoCount;        // 我的待办
    private Long doneWeekCount;    // 本周已办
    private Long startedRunningCount; // 我发起的进行中
    private Long ccUnreadCount;    // 抄送我未读
  }
  ```
- **验收**:
  - 进入 `/workspace/summary` 显示 4 个聚合数。
  - 顶部徽标 30s 自动刷新；登出登陆后立即重新拉取。
  - 接口性能：单用户 4 个聚合数响应 < 200ms。

### Task A4: 应用中心 → 工作台跳转提示

- **目标**：用户在应用中心提交业务单后，顶部出现 toast "已提交 · 查看流转"，点击直达 `/workspace/started`。
- **涉及文件**:
    - `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` — 提交成功后触发全局事件 `business-submit-success`。
    - `forge-admin-ui/src/layout/components/AppHeader.vue` — 监听事件并 toast。
    - 或采用 Naive UI `useMessage()` 全局调用。
- **关键签名**:
  ```js
  // AiCrudPage.vue 提交成功后
  emitGlobal('business-submit-success', { businessKey, summary, processInstanceId });

  // AppHeader.vue
  on('business-submit-success', (payload) => {
    message.success({
      content: `${payload.summary} 已提交`,
      action: () => router.push('/workspace/started'),
      actionText: '查看流转'
    });
  });
  ```
- **验收**:
  - 采购单提交成功后顶部出现"已提交 · 查看流转"，点击跳工作台。
  - 失败时不显示 toast。

### Task A5: 菜单数据迁移 + 旧入口下线

- **目标**：左侧菜单不再显示 `/flow/todo` 等待办入口（这些迁到工作台）；`/flow/model` `/flow/form` `/flow/template` `/flow/monitor` 已在第二轮 spec 弱化。
- **涉及文件**:
    - 菜单数据源（DB 种子或前端配置）— 删除/隐藏 `/flow/todo` `/flow/done` `/flow/started` `/flow/cc`。
    - 应用中心顶部"我的工作台"按钮成为唯一入口。
- **验收**:
  - 左侧菜单不再出现"我的待办"等入口。
  - 直接访问 `/flow/todo` 仍正常（兼容期）。

### 子迭代 A 本轮执行状态（2026-06-29 /apply）

- **A1 已完成**：新增 `/workspace` 手写父子路由、`views/workspace/` 工作台壳、summary/todo/done/started/cc 页面；旧 `/flow/*` 路由保持不变。
- **A2 已完成**：新增统一 `BusinessTopNav`，应用中心 / 工作台 / 能力中心并列；能力中心仅超级管理员可见；工作台按钮接入待办徽标。
- **A3 核心已完成**：新增 `/api/workspace/summary`、`/api/workspace/todo-count`，工作台首页展示 4 个聚合数；前端构建和后端编译通过。
- **A3 剩余**：`/ai/user/setting/workspace-default-page` 用户偏好默认页未实现，仍按 `/workspace -> /workspace/summary` 固定默认。
- **A4 未执行**：应用中心提交成功 toast 仍待后续小步处理。
- **A5 未执行**：菜单数据迁移 / 旧入口隐藏未执行；本轮通过权限守卫白名单允许 `/workspace/*` 已登录访问。
- **Spec 偏差记录**：本轮接口按现有流程 API 风格落在 `/api/workspace/*`，未采用 spec 表中的 `/ai/workspace/*`。

---

## 子迭代 B — 应用中心节点就地配置（建议 2-3 周交付）

### Task B1: 抽取共享节点配置组件

- **目标**：把 `NodeConfigDrawer` + `ApproverConfig` + `FormPermissionConfig` 抽成无 BPMN 画布依赖的 `FlowNodeInlineConfig.vue`。
- **涉及文件**:
    - `forge-admin-ui/src/components/flow-designer/panel/NodeConfigDrawer.vue` — 抽出"无画布"模式。
    - 新建 `forge-admin-ui/src/components/flow-inline/FlowNodeInlineConfig.vue` — 抽屉壳，输入 `modelKey + nodeId + businessObjectCode`，输出节点配置变更事件 `update:nodeConfig`。
    - 内部仍引用 `ApproverConfig.vue` / `FormPermissionConfig.vue` / `PermissionConfig.vue`。
- **关键签名**:
  ```vue
  <FlowNodeInlineConfig
    v-model:show="drawerShow"
    :model-key="modelKey"
    :node-id="selectedNodeId"
    :business-object-code="objectCode"
    @save="onNodeConfigSave"
    @cancel="onCancel"
  />
  ```
- **验收**:
  - `FlowNodeInlineConfig` 可独立挂载，不依赖 BPMN 画布。
  - 流程设计器 `NodeConfigDrawer` 行为不变（向后兼容）。

### Task B2: 节点拓扑步骤条组件

- **目标**：新建 `FlowNodeStepList.vue` 步骤条，展示人工节点序列。
- **涉及文件**:
    - 新建 `forge-admin-ui/src/components/flow-inline/FlowNodeStepList.vue` — 垂直时间线 / 步骤条，长流程支持滚动 + 搜索。
    - 新增后端接口 `GET /ai/business/flow/model/{modelKey}/inline-nodes` 返回人工节点摘要。
    - 后端 `BusinessFlowService.listInlineNodes(modelKey)`。
- **关键签名**:
  ```java
  public List<InlineNodeSummaryVO> listInlineNodes(String modelKey);

  public class InlineNodeSummaryVO {
    private String nodeId;
    private String nodeName;
    private String nodeType;       // userTask only
    private String formAssetName;  // 当前绑定表单中文名
    private Integer assigneeCount;
    private Boolean configured;    // 是否已完成配置
  }
  ```
- **验收**:
  - 步骤条按 BPMN 节点顺序展示人工任务。
  - 自动节点 / 网关在步骤条中不出现。
  - 长流程支持滚动（容器高度限制 + 内部滚动）。

### Task B3: 节点就地配置 GET/PUT 接口

- **目标**：提供节点配置详情查询 + 保存接口（含并发校验）。
- **涉及文件**:
    - 新增 `forge-server/.../controller/BusinessFlowNodeController.java` — `GET/PUT /business/flow/node/{modelKey}/{nodeId}/config`。
    - `BusinessFlowService.getNodeConfig(modelKey, nodeId)` / `saveNodeConfig(modelKey, nodeId, dto)`。
    - `ai_flow_model_node.update_time` 索引（如缺）。
- **关键签名**:
  ```java
  public class FlowNodeConfigDTO {
    private String approverConfig;
    private String formAssetKey;
    private List<FieldPermissionDTO> fieldPermissions;
    private String advancedConfig;
    private Long expectedUpdateTime;  // 乐观锁
  }

  @PutMapping("/business/flow/node/{modelKey}/{nodeId}/config")
  public R<FlowNodeConfigVO> save(@PathVariable String modelKey,
                                  @PathVariable String nodeId,
                                  @Valid @RequestBody FlowNodeConfigDTO dto);
  // 409 Conflict 当 expectedUpdateTime 不匹配
  ```
- **验收**:
  - GET 返回当前节点配置 + `updateTime`。
  - PUT 成功更新且 `updateTime` 递增。
  - PUT 携带过期 `expectedUpdateTime` 返回 409。

### Task B4: 应用中心嵌入步骤条 + 就地抽屉

- **目标**：`BusinessFlowBindingPanel.vue` 节点列表替换为步骤条，选中后右侧弹 `FlowNodeInlineConfig`。
- **涉及文件**:
    - `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue` — 节点列表区替换为 `<FlowNodeStepList>` + `<FlowNodeInlineConfig>`。
    - 加载步骤条数据：调用 `/inline-nodes`。
    - "打开完整流程设计器"按钮位置上移到 tab 顶部右侧。
- **关键签名**:
  ```vue
  <FlowNodeStepList
    :model-key="modelKey"
    :selected-node-id="selectedNodeId"
    @select="onSelectNode"
  />
  <FlowNodeInlineConfig
    v-model:show="drawerShow"
    :model-key="modelKey"
    :node-id="selectedNodeId"
    :business-object-code="objectCode"
    @save="onSaved"
  />
  ```
- **验收**:
  - 应用中心"业务流程"tab 不再显示原节点列表，而是步骤条。
  - 选中节点 → 抽屉打开 → 编辑 → 保存 → 步骤条刷新。
  - 编辑自动节点时显示只读说明 + "请在流程设计器中编辑"。
- **依赖**: B1, B2, B3。

### Task B5: 并发编辑冲突 UI

- **目标**：保存返回 409 时弹确认框"该节点已被他人修改，是否覆盖"。
- **涉及文件**:
    - `FlowNodeInlineConfig.vue` — catch 409 → `n-dialog` 提示。
    - 同步流程设计器 `NodeConfigDrawer.vue` 实现一致 UI。
- **验收**:
  - 两个浏览器同时开同一节点 → A 保存成功 → B 保存提示 409 → B 选择覆盖后再次保存成功。

### Task B6: 配置完成度可视化

- **目标**：节点步骤条上显示每个节点是否"已完成配置"（绿色对勾 / 灰色感叹号），帮助管理员快速判断流程是否就绪。
- **涉及文件**:
    - 后端 `InlineNodeSummaryVO.configured` 字段（已在 B2 包含）。
    - `BusinessFlowService.judgeNodeConfigured(node)` — 判断逻辑：审批人非空 + 表单资产非空（网关：出向条件全部已配置）。
    - 前端 `FlowNodeStepList.vue` 渲染状态徽标。
- **验收**:
  - 完整配置的节点显示绿色对勾。
  - 缺审批人 / 缺表单 / 网关出向条件未配 的节点显示灰色感叹号 + 悬浮提示具体缺什么。

### Task B7: 应用中心"业务流程"tab 重排（视觉骨架）

- **目标**：tab 顶部展示步骤"基本配置 → 节点配置 → 变量映射 → 发布检查"，让业务管理员明确流程配置的完成度。
- **涉及文件**:
    - `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue` — 顶部加 `n-steps` 步骤条壳。
    - `BusinessFlowAppConfigPanel.vue` — 子 tab 重排与可视化关联。
- **验收**:
  - tab 顶部展示 4 步进度条，当前步骤高亮，已完成步骤绿色对勾。
  - 步骤可点击跳转到对应区块（页内锚点）。

### Task B8: 网关节点业务化就地配置（用户确认本轮支持）

- **目标**：步骤条同时展示网关节点（exclusiveGateway / inclusiveGateway），点击后抽屉提供业务化条件配置 + 快捷预设。
- **涉及文件**:
    - 后端：
      - `forge-server/.../controller/BusinessFlowGatewayController.java` — 新增 `GET/PUT /business/flow/gateway/{modelKey}/{nodeId}/config`、`GET /business/flow/gateway/presets`。
      - `forge-server/.../service/businessapp/BusinessFlowGatewayService.java` — 网关业务化条件 ↔ BPMN `<conditionExpression>` 双向转换；`isInlineEditable(node)` 判定。
      - `forge-server/.../vo/GatewayInlineConfigVO.java`：包含 `outgoingConditions[] = {sourceField, operator, value, targetNodeId, presetType?}`、`bpmnExpressions[]`（高级回显）、`inlineEditable`、`reason`（不可编辑原因）。
      - 网关条件预设种子：同意 / 驳回 / 退回上一步 / 终止流程 四个。
    - 前端：
      - 新建 `forge-admin-ui/src/components/flow-inline/FlowGatewayInlineConfig.vue` — 网关业务化条件配置抽屉。
      - `forge-admin-ui/src/components/flow-inline/FlowNodeStepList.vue` — 步骤条增加网关节点渲染（菱形节点 + 出向条件预览）。
      - `forge-admin-ui/src/components/flow-inline/FlowNodeInlineConfig.vue` — 根据 nodeType 路由到 `FlowGatewayInlineConfig` 或人工节点配置。
      - `BusinessFlowBindingPanel.vue` 嵌入网关编辑。
    - 数据库：Flyway `V1.0.88__add_flow_model_node_gateway_inline_config.sql`（缓存列，非权威）。
- **关键签名**:
  ```java
  public class GatewayOutgoingConditionDTO {
    private String sourceField;        // 来源字段（如 approvalResult）
    private String operator;           // ==, !=, >, >=, <, <=, contains, in
    private Object value;              // 目标值
    private String targetNodeId;       // 流转目标节点 ID
    private String presetType;         // approve | reject | returnPrev | terminate | custom
  }

  public class GatewayInlineConfigDTO {
    private List<GatewayOutgoingConditionDTO> outgoingConditions;
    private List<String> advancedExpressions;  // BPMN 表达式（高级编辑）
    private Long expectedUpdateTime;
  }

  public class BusinessFlowGatewayService {
    public GatewayInlineConfigVO getConfig(String modelKey, String nodeId);
    public GatewayInlineConfigVO saveConfig(String modelKey, String nodeId, GatewayInlineConfigDTO dto);
    public boolean isInlineEditable(FlowNode gateway);   // 出向条件全部 (字段+运算符+字面值) 且分支 ≤ 5
    public List<GatewayPresetVO> listPresets();
    public String convertToBpmnExpression(GatewayOutgoingConditionDTO cond);  // 业务化 → BPMN
    public GatewayOutgoingConditionDTO parseBpmnExpression(String expr);     // BPMN → 业务化（失败则返回 null）
  }
  ```
- **验收**:
  - 步骤条出现菱形网关节点；点击弹出 `FlowGatewayInlineConfig` 抽屉。
  - 快捷预设"同意路由"一键应用后，出向条件填充为 `approvalResult == 'approve' → 下一节点`。
  - 自定义条件保存后，读 BPMN 文件可见 `<conditionExpression>` 等价表达式。
  - 复杂网关（多分支 + 复合表达式）抽屉显示"该网关结构复杂，请在完整流程设计器中编辑"按钮，禁用就地编辑。
  - 保存后立即回读对比业务化 ↔ BPMN，若偏差立即报错。
- **依赖**: B1（共享抽屉）、B2（步骤条）、B3（节点配置 API 复用部分基础设施）。

---

## 子迭代 C — 渲染引擎收敛到 AiForm（建议 3-4 周交付，含迁移期）

### Task C1: AiForm schema 适配层

- **目标**：新增 `formCreateToAiSchema(formJson)` 适配函数，把 form-create schema 转 AiForm schema。
- **涉及文件**:
    - 新建 `forge-admin-ui/src/components/ai-form/adapters/formCreate.js` — 适配函数。
    - `AiForm.vue` 增加 `:schema-source="'formCreate'"` prop，自动调用适配层。
    - 单元测试覆盖 10 种基础控件迁移。
- **关键签名**:
  ```js
  /**
   * @param {Object} formJson form-create 嵌套 JSON
   * @returns {{schema: AiFormField[], unknownFields: Object[]}}
   */
  export function formCreateToAiSchema(formJson) { ... }
  ```
- **验收**:
  - 10 种基础控件（input/textarea/number/select/radio/checkbox/date/datetime/switch/upload）迁移后 AiForm 渲染等价。
  - 未知字段返回 `unknownFields`，AiForm 在该位置渲染占位 + 提示文案。

### Task C2: 流程节点动态表单切换 AiForm（feature flag）

- **目标**：`todo.vue` 节点动态表单分支增加 feature flag `flow.form.engine`，默认 `ai`，可降级 `formCreate`。
- **涉及文件**:
    - `forge-admin-ui/src/views/flow/todo.vue:297-302` — `<FlowFormCreateRenderer>` 与 `<AiForm + adapter>` 双分支。
    - `forge-admin-ui/src/views/workspace/todo.vue`（A1 已建）— 同步分支。
    - feature flag 来源：`useAppConfig()` 或环境变量。
- **关键签名**:
  ```vue
  <component
    :is="flowFormEngine === 'ai' ? AiForm : FlowFormCreateRenderer"
    v-model="dynamicFormData"
    :schema="adaptedSchema"
    :field-permissions="taskFormInfo.formFieldPermissions"
  />
  ```
- **验收**:
  - 默认 AiForm 渲染节点动态表单。
  - feature flag 切回 form-create 可立即降级。
  - 字段权限三态语义在两种引擎下一致。

### Task C3: schema 迁移工具

- **目标**：实现一次性 + 可重入的 `FormCreateSchemaMigrationJob`，把存量 `form_json` 转换为 `form_schema`。
- **涉及文件**:
    - 新建 `forge-server/.../job/FormCreateSchemaMigrationJob.java`。
    - 新建 `forge-server/.../mapper/FlowSchemaMigrationLogMapper.java` + entity。
    - Flyway: `V1.0.85__add_flow_model_node_form_schema.sql` + `V1.0.86__create_flow_schema_migration_log.sql`。
    - 触发接口：`POST /business/flow/schema/migration/{modelKey}` / `POST /business/flow/schema/migration`。
- **关键签名**:
  ```java
  public class FormCreateSchemaMigrationJob {
    public MigrationReport migrateModel(String modelKey);
    public MigrationReport migrateAll();
  }

  public class MigrationReport {
    private Integer total;
    private Integer success;
    private Integer failed;
    private List<String> failedNodeIds;
  }
  ```
- **验收**:
  - 单个模型迁移：成功 / 失败计数准确。
  - 重复执行不重复迁移已成功节点。
  - 失败节点记录在 `ai_flow_schema_migration_log`，可重试。

### Task C4: 流程通用表单设计器迁移

- **目标**：`/flow/form.vue` 与 `/flow/template.vue` 表单设计器从 form-create 改为业务对象设计器 `form` 面板组件。
- **涉及文件**:
    - `forge-admin-ui/src/views/flow/form.vue` — 表单编辑器组件替换。
    - `forge-admin-ui/src/views/flow/template.vue` — 同上。
    - 复用 `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`（或对应 AiForm 设计器）。
    - 后端保存 schema 写入 `form_schema` 新列。
- **验收**:
  - `/flow/form` 编辑器视觉与业务对象表单设计器一致。
  - 保存后通用流程使用该表单时按 AiForm 渲染。

### Task C5: form-create 标记 deprecated

- **目标**：保留 `FlowFormCreateRenderer.vue` 代码，但加 `@deprecated` 注释 + 构建期 warning。
- **涉及文件**:
    - `forge-admin-ui/src/components/flow-form-create/FlowFormCreateRenderer.vue` — 顶部加 `@deprecated` 注释。
    - 新流程创建时不再允许选择 form-create（UI 移除入口）。
- **验收**:
  - 新建流程的表单设计器只显示 AiForm 设计器。
  - 老流程仍可降级 form-create（feature flag）。

### Task C6: 渲染一致性截图回归

- **目标**：业务待办 / 独立流程待办 / 业务运行时 / 流程节点动态表单 四处视觉对比。
- **涉及文件**:
    - 新增 `forge-admin-ui/tests/visual/` 截图基线（使用 vitest + happy-dom + playwright，按现有项目实际框架）。
    - 端到端用例：采购单审批 + 独立请假流程。
- **验收**:
  - 四处渲染同一表单视觉一致（截图差异 < 1% 像素）。
  - 字段权限三态视觉一致。

### Task C7: form-create 代码完全清理（用户确认本轮完成）

- **前置硬条件**（自动化前置检查脚本必须全绿，缺一不可）：
  1. C4 迁移工具在生产环境跑完，所有租户 `ai_flow_model_node.form_schema` 列填充率 = 100%（不含 dry-run 记录）。
  2. feature flag `flow.form.engine=ai` 在生产全量开启 ≥ 24 小时无回滚。
  3. 监控指标在 24 小时窗口内：
     - 待办表单渲染错误率 < 0.1%
     - 字段权限失效告警 0 条
     - 节点表单加载 P95 < 800ms
- **目标**：删除 form-create 引擎相关代码、依赖、feature flag；保留数据兜底列一个大版本。
- **涉及文件**:
    - 前置检查脚本：新增 `forge-server/.../job/FormCreatePurgePrecheckJob.java`，输出 `PrecheckReport`。
    - 前端：删除
      - `forge-admin-ui/src/components/flow-form-create/` 整目录（`FlowFormCreateRenderer.vue` 等）。
      - `forge-admin-ui/src/components/flow-form-create-designer/`（如有）。
      - `forge-admin-ui/src/views/flow/todo.vue` `done.vue` 的 form-create 分支（`<component :is="...">` 双引擎切换简化为只用 AiForm）。
      - `forge-admin-ui/src/views/flow/form.vue` `template.vue` 的 form-create 残余引用。
    - `forge-admin-ui/package.json` 卸载：`@form-create/*` / `form-create` 等依赖；`pnpm install` 锁定新 lockfile。
    - 后端：
      - 删除 feature flag `flow.form.engine` 读写代码（`AppConfigService` 中相关 key）。
      - 删除 form-create 旧端点（如 `/ai/flow/form-create/preview` 等，如存在）。
      - `BusinessFlowService` / `FlowController` 中 `formJson` 字段读取退化为"兜底降级路径"，主路径只用 `form_schema`。
    - 文档：
      - `forge-docs/` 移除 form-create 相关页面。
      - `code-copilot/memory/decisions.md` 沉淀"form-create 完全下线"决策记录。
    - **数据兜底**：`ai_flow_model_node.form_json` 列保留一个大版本，下下轮变更才 DROP COLUMN。
    - 回滚预案脚本：新建 `forge-server/.../job/FormSchemaRebuildFromFormJsonJob.java`，从 `form_json` 即时重建 `form_schema`（演练用）。
- **关键签名**:
  ```java
  public class FormCreatePurgePrecheckJob {
    public PrecheckReport check();
  }

  public class PrecheckReport {
    private boolean migrationFullyCompleted;
    private boolean featureFlagFullyOn24h;
    private MetricsStatus metricsStatus;
    private boolean canPurge;        // 三者都 true 才能 purge
    private List<String> blockReasons;
  }

  public class FormSchemaRebuildFromFormJsonJob {
    public RebuildReport rebuild(String modelKey);  // 用于回滚演练
    public RebuildReport rebuildAll();
  }
  ```
- **验收**:
  - 自动化前置检查脚本在执行 C7 前运行一次，输出 `canPurge=true` 才允许继续。
  - 删除代码后 `pnpm build` 成功，`pnpm test --run` 全绿。
  - 后端 `mvn clean install` 全绿。
  - 测试环境执行"从 form_json 重建"演练一次，所有节点 `form_schema` 与 C4 迁移结果一致。
  - 用户在测试环境完整跑一遍采购单审批 + 独立请假流程，无报错。
  - feature flag `flow.form.engine` 配置项移除后，任何引用此 key 的代码搜索为空。
  - `form_json` 列保留，未 DROP COLUMN（下下轮处理）。
- **发布策略**：作为子迭代 C 的**最后一个 Task** 单独发布；上线后 7 天观察期；任何回滚信号都走"从 form_json 重建" + 临时恢复 feature flag 旁路（提前打 tag 备份代码）。
- **依赖**: C1 / C2 / C3 / C4 / C5 / C6 全部完成且生产稳定运行 24h 以上。

---

## 任务依赖图

```
子迭代 A（工作台）:
  A1 (路由骨架) ──┬─ A2 (顶部导航) ──┬─ A4 (提交toast)
                 └─ A3 (聚合首页 + 徽标 + 用户偏好默认页)
                                    └─ A5 (旧入口下线)

子迭代 B（节点就地）:
  B1 (共享组件) ──┐
  B2 (步骤条 + 后端摘要)──┤
  B3 (节点配置 GET/PUT)──┼─ B4 (应用中心嵌入) ──┬─ B5 (并发冲突 UI)
                          ┘                      ├─ B6 (完成度可视化)
                                                 ├─ B7 (tab 视觉骨架)
                                                 └─ B8 (网关业务化就地配置)

子迭代 C（渲染引擎收敛 + 完全清理）:
  C1 (适配层) ──┬─ C2 (feature flag 切换)
  C3 (迁移工具)─┤
              └─ C4 (通用表单设计器) ──┬─ C5 (deprecated 标记)
                                       └─ C6 (视觉回归) ──→ C7 (form-create 清理)

C7 前置硬条件（自动化检查脚本全绿）:
  ① 全量迁移完成 + ② feature flag 全量开启 24h + ③ 监控指标正常
```

## 子迭代发布检查清单

### A 发布前
- [ ] `/workspace/*` 全部路由跑通。
- [ ] 旧 `/flow/*` 兼容路径未破坏。
- [ ] 顶部徽标轮询性能确认（每用户 30s 一次，无显著压库）。
- [ ] 工作台首页聚合数与各子页列表数对得上。

### B 发布前
- [ ] 应用中心节点就地保存与流程设计器保存数据一致（双端读同一份）。
- [ ] 并发冲突测试通过（双 tab 测试）。
- [ ] 步骤条对 >20 节点流程仍可用（滚动 + 搜索）。
- [ ] 自动节点（脚本/服务任务）在步骤条只读展示，进入完整设计器仍能编辑。
- [ ] 网关节点（B8）：4 个快捷预设全部生效；自定义条件 ↔ BPMN 表达式双向转换语义等价；复杂网关引导跳完整设计器。

### C 发布前（C1-C6）
- [ ] schema 迁移工具在测试环境跑过至少 1 个完整流程模型（含 dry-run + 正式迁移）。
- [ ] feature flag 灰度方案 ready（单租户先开 → 观察 24h → 全量）。
- [ ] 截图回归无人工标记的差异。
- [ ] 老 form-create 流程切回降级路径验证通过。

### C7（form-create 完全清理）发布前
- [ ] 自动化前置检查脚本 `FormCreatePurgePrecheckJob` 输出 `canPurge=true`。
- [ ] 全量迁移完成（`SELECT COUNT(*) FROM ai_flow_model_node WHERE form_json IS NOT NULL AND form_schema IS NULL` = 0）。
- [ ] feature flag `flow.form.engine=ai` 已全量开启 ≥ 24h 无回滚。
- [ ] 监控指标在 24h 窗口正常（错误率 / 失效告警 / P95 全部达标）。
- [ ] 回滚预案演练通过：`FormSchemaRebuildFromFormJsonJob` 在测试环境从 `form_json` 重建 `form_schema`，与 C4 迁移结果一致。
- [ ] 代码备份 tag 已打：`git tag pre-form-create-purge`。
- [ ] 上线后 7 天观察期窗口预留。

## 风险点回顾（详见 spec.md §8）

- 导航重组 + 路由兼容必须长期保留旧 URL。
- 节点配置双写竞态以乐观锁兜底，UI 必须明确提示用户。
- 网关业务化 ↔ BPMN 表达式双向转换必须语义等价；保存后立即回读对比。
- schema 迁移精度 + 未知字段处理是渲染引擎收敛的最大不确定性。
- **form-create 完全清理（C7）已纳入本轮范围**，但有严格前置硬条件 + 7 天观察期 + 数据兜底保留一个大版本。

## 2026-06-29 用户纠偏增量任务状态

> 下列任务按用户本轮 6 条反馈执行，覆盖早期 B1-B8“应用中心节点就地配置”方案；节点级配置主入口改为真实流程设计器弹窗。

- [x] **D1 隐藏代码应用“表单字段”面板**：代码应用对象设计器只保留“业务流程配置”，不再展示独立字段目录。
- [x] **D2 应用中心弹窗打开流程设计器**：业务流程面板点击“打开流程设计器”后在全屏弹窗内加载 `flow/design.vue`，关闭后留在应用中心。
- [x] **D3 业务字段变量自动化**：移除人工变量映射 UI，保存时按业务字段生成同名变量映射；后端流程变量注入增加全字段兜底。
- [x] **D4 节点配置所有权回归 BPMN**：应用中心保存不再提交新的 `nodeForms`，避免覆盖流程设计器节点表单和字段权限。
- [x] **D5 动态表单渲染切 AiForm**：待办/已办的节点动态表单从 form-create renderer 切到 `AiForm + formCreateToAiSchema`。
- [x] **D6 隐藏重复工作台入口**：应用中心顶部隐藏“我的工作台”按钮和徽标轮询，外部工作台菜单保持可用。
- [x] **D7 修复 3000 workspace 代理**：Vite 新增 `/dev-api/api/workspace` 代理到 flow 服务；需重启 3000 端口生效。

**后续保留项**：

- [ ] 在浏览器内手测应用中心弹窗设计器：选择节点、配置字段权限、保存、关闭后重新打开验证配置仍在。
- [ ] 有后端 dev 服务和登录 token 后，验证 `/dev-api/api/workspace/todo-count` 不再返回前端 404。
