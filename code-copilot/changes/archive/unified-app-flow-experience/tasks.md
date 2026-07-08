# 任务拆分 — 业务应用与流程模块体验整合

> 拆分顺序：数据契约 → 后端 API → 共享组件 → 入口页面 → 菜单与运行时
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> P0/P1/P2 分阶段交付，P0 可单独发布

## 前置条件

- [x] 用户确认 `spec.md` §9 三个待澄清项的结论。
- [x] 用户确认是否同意 P0 单独发布（不等 P1/P2）。
- [ ] 本地能跑通 `forge-admin-server` + `forge-flow` + `forge-admin-ui`，并能进入采购单审批样例的待办列表。（本轮未启动服务做端到端验证）

---

## P0 — 用户感知最强的三项（建议本周交付）

### Task 1: 业务表单字段权限前端生效（F6）

- **执行状态**: completed（2026-06-29 /apply）。`AiForm` 已消费 `fieldPermissions`，todo/done 业务托管表单已接入，已办强制只读。

- **目标**：让节点字段权限矩阵（visible/editable/required）在 `<AiForm>` 渲染时立即生效，覆盖采购单审批待办场景。
- **涉及文件**:
    - `forge-admin-ui/src/components/ai-form/AiForm.vue` — 新增 `fieldPermissions` prop 与字段处理逻辑。
    - `forge-admin-ui/src/components/ai-form/types.ts`（如存在）— 补充类型定义。
    - `forge-admin-ui/src/views/flow/todo.vue:256-265` — 业务托管表单分支传入 `:field-permissions`。
    - `forge-admin-ui/src/views/flow/done.vue` — 已办分支强制 `editable=false`（封装一个 `toReadonlyPermissions(permissions)` 工具或直接覆盖）。
- **关键签名**:
  ```ts
  // AiForm.vue props
  fieldPermissions?: Array<{
    fieldCode: string;
    visible?: boolean;     // 默认 true
    editable?: boolean;    // 默认 true
    required?: boolean;    // 默认 false（不强制覆盖 schema 必填）
  }>;
  ```
  ```ts
  // 内部计算
  const visibleSchema = computed(() => schema.filter(f => permMap.value[f.code]?.visible !== false));
  const fieldDisabled = (code: string) => permMap.value[code]?.editable === false;
  const fieldRequired = (code: string) => !!permMap.value[code]?.required || !!schemaRequired[code];
  ```
- **验收**:
  - 采购单审批节点配置"申请人修改"节点的 `needDate` 字段为 `visible=true, editable=true, required=true`，"部门负责人审批"节点为 `visible=true, editable=false, required=false`。
  - 待办里"申请人修改"节点 `needDate` 可编辑、必填红星；"部门负责人审批"节点 `needDate` 灰显、无红星。
  - 已办 `needDate` 一律灰显。
  - `<AiForm>` 未传 `field-permissions` 时行为不变（业务运行时回归验证）。

### Task 2: 待办列表中文流程名渲染（F1）

- **执行状态**: completed（2026-06-29 /apply）。已新增 `processDisplay.js`，todo/done/started/cc 统一使用业务摘要与中文流程名回退链。

- **目标**：列表 `#title` 永远显示中文（业务摘要 / processName），不再出现 `sample_purchase_order` 等英文 key。
- **涉及文件**:
    - `forge-admin-ui/src/views/flow/todo.vue:70-72` — `#title` 改写回退链。
    - `forge-admin-ui/src/views/flow/done.vue` — 同上。
    - `forge-admin-ui/src/views/flow/started.vue` — 同上。
    - `forge-admin-ui/src/views/flow/cc.vue` — 同上。
    - 新增 `forge-admin-ui/src/views/flow/utils/processDisplay.js` — 抽取 `getRowDisplayTitle(row)` 工具，避免四处重复。
- **关键签名**:
  ```js
  export function getRowDisplayTitle(row) {
    return row.businessSummary
      || row.businessObjectName
      || row.processName
      || row.processTitle
      || row.modelName
      || row.taskName
      || row.processDefinitionName
      || row.processDefinitionKey
      || '-';
  }
  ```
- **验收**:
  - 列表 `#title` 在采购单审批样例显示"采购单审批"（或业务摘要"采购单审批 · PO20260629001"），不出现 `sample_purchase_order`。
  - 老历史任务（仅有 `processDefinitionKey`）仍能显示 key 兜底。
- **依赖**: 后端列表 VO 必须已包含 `processName`（已确认 `FlowTaskMapper.xml` `selectTodoTasks` join 有 `m.model_name AS process_name`）。如 VO/响应缺字段，先在本任务里补回。

### Task 3: 待办详情移除技术标签（F2）

- **执行状态**: completed（2026-06-29 /apply）。todo/done 已移除“代码业务”展示，业务表单上下文补 `businessObjectName/businessSummary`，采购单 Provider 已标记内部/系统字段。

- **目标**：抽屉顶部不再露"代码业务" tag，业务标题用中文业务对象名 + 业务摘要；技术字段（id 等）默认隐藏。
- **涉及文件**:
    - `forge-admin-ui/src/views/flow/todo.vue:249-265` — 移除 `<n-tag>代码业务</n-tag>`；`businessFormTitle` 改用 `businessObjectName + '·' + businessNo` 兜底。
    - `forge-admin-ui/src/views/flow/done.vue` — 同上。
    - 后端 VO 扩展（如需）：`BusinessTaskFormContextVO` 增加 `businessObjectName`、`businessSummary` 字段（如不存在）。
    - `forge-server/.../purchase/provider/SamplePurchaseOrderCodeFormProvider.java:120-180` — 给 `id`、`tenantId`、`createBy`、`updateBy`、`createTime`、`updateTime` 等字段标 `internal=true` / `systemField=true`。
    - `forge-server/.../service/businessapp/BusinessCodeFormField.java` 或 `field(...)` 工厂 — 增加 `internal` / `systemField` 字段。
- **关键签名**:
  ```java
  public static BusinessCodeFormField field(
      String code, String label, FieldType type, boolean required,
      boolean internal, boolean systemField, String description) { ... }

  // 旧重载保留：
  public static BusinessCodeFormField field(String code, String label, FieldType type, boolean required) {
      return field(code, label, type, required, false, false, null);
  }
  ```
  ```vue
  <!-- 移除 -->
  <n-tag v-if="useBusinessCodeForm" ...>代码业务</n-tag>

  <!-- 改为 -->
  <div class="approval-form-title">
    <span>{{ businessObjectName || businessFormTitle }}</span>
    <span v-if="businessSummary" class="form-subtitle">{{ businessSummary }}</span>
  </div>
  ```
- **验收**:
  - 待办详情顶部不出现"代码业务"字样。
  - 表单中字段不出现 `id`、`tenant_id` 等系统字段（除非进入"高级"折叠区）。
  - 标题显示"采购单审批"或"采购申请单 · PO20260629001"。

---

## P1 — 模块整合的"骨架"（建议 1-2 周交付）

### Task 4: 流程模型反查业务绑定接口（F8）

- **执行状态**: completed（2026-06-29 /apply）。新增 `/ai/business/flow/model/{modelKey}/business-bindings`，Mapper XML 查询返回业务对象摘要和回跳入口。

- **目标**：提供 `/ai/business/flow/model/{modelKey}/business-bindings` 接口，返回该流程被哪些业务对象绑定。
- **涉及文件**:
    - `forge-server/.../controller/BusinessFlowController.java` — 新增方法 `listBusinessBindingsByModelKey`。
    - `forge-server/.../service/businessapp/BusinessFlowService.java` — 新增 `listBusinessBindingsByModelKey(String modelKey)`。
    - `forge-server/.../vo/BusinessBindingSummaryVO.java` — 新增简化 VO（objectCode、objectName、suiteName、codeApp、entryRoute）。
    - `forge-admin-ui/src/api/business-app.js` — 新增 `businessFlowModelBindings(modelKey)` 函数。
- **关键签名**:
  ```java
  @GetMapping("/model/{modelKey}/business-bindings")
  public R<List<BusinessBindingSummaryVO>> listBusinessBindingsByModelKey(@PathVariable String modelKey);
  ```
  ```js
  export function businessFlowModelBindings(modelKey) {
    return defHttp.get({ url: `/ai/business/flow/model/${modelKey}/business-bindings` });
  }
  ```
- **验收**:
  - 调用接口返回采购单业务对象绑定信息。
  - 无绑定返回空数组。
  - 接口响应时间 < 200ms（采购单样例数据下）。

### Task 5: 流程模型列表/设计器自动注入业务上下文（F3）

- **执行状态**: completed（2026-06-29 /apply）。`/flow/model` 已展示绑定业务应用并向嵌入式设计器注入业务对象上下文，`design.vue` 已显示业务 Banner 并优先加载业务表单资产。

- **目标**：流程模型列表显示绑定业务应用；进入设计器自动注入 `businessObjectCode`，业务上下文 Banner 常驻。
- **涉及文件**:
    - `forge-admin-ui/src/views/flow/model.vue:747` — `handleDesign(row)` 调用反查接口（或预加载到列表数据），注入 `businessObjectCode`。
    - `forge-admin-ui/src/views/flow/model.vue` 列表 — 新增"绑定业务应用"列。
    - `forge-admin-ui/src/views/flow/design.vue:714-715` — `businessObjectCode` 兜底读 props；新增顶部 Banner 组件。
    - 新增 `forge-admin-ui/src/views/flow/components/BusinessContextBanner.vue` — 显示业务对象中文名 + 返回业务应用按钮。
- **关键签名**:
  ```vue
  <!-- model.vue -->
  <FlowDesignPage
    :model-id="currentDesignModelId"
    :business-object-code="currentDesignBindings?.[0]?.objectCode"
    :code-app="currentDesignBindings?.[0]?.codeApp"
  />
  ```
  ```vue
  <!-- design.vue -->
  <BusinessContextBanner
    v-if="businessContextActive"
    :object-name="businessObjectName"
    :object-code="businessObjectCode"
    @back="goBackToAppCenter"
  />
  ```
- **验收**:
  - `/flow/model` 列表"采购单审批"行显示绑定"采购申请"。
  - 点击设计进入后，画布顶部 Banner 显示"当前流程绑定：采购申请"。
  - 节点资产分支走业务侧资产（不再是 `formOptions`）。
- **依赖**: Task 4。

### Task 6: 节点表单资产卡片化（F4）

- **执行状态**: completed（2026-06-29 /apply）。`ApproverConfig` 已复用 `BusinessFlowFormAssetSelect`，资产接口补充 `sourceType/fieldCount/fieldPreview`。

- **目标**：流程设计器节点抽屉"表单权限"页签的资产选择，从下拉换成卡片库，复用 `BusinessFlowFormAssetSelect.vue`。
- **涉及文件**:
    - `forge-admin-ui/src/components/flow-designer/panel/ApproverConfig.vue:131-144` — `<n-select>` 替换为 `<BusinessFlowFormAssetSelect>`。
    - `forge-admin-ui/src/components/flow-designer/panel/NodeConfigDrawer.vue:42,73` — 透传 `assetCards`（卡片数据：含来源标签、字段数、字段预览）。
    - `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowFormAssetSelect.vue` — 增加"来源标签"、"字段数"、"字段预览"展示（如尚未支持）。
    - 后端：`BusinessFlowFormAssetVO` — 补 `sourceType`（`businessObject` / `codeProvider` / `external`）、`fieldCount`、`fieldPreview`。
    - `forge-server/.../service/businessapp/BusinessFlowService.java#getFormAssets` — 计算字段数与预览。
- **关键签名**:
  ```java
  public class BusinessFlowFormAssetVO {
    private String formKey;
    private String formName;
    private String sourceType;   // businessObject | codeProvider | external
    private Integer fieldCount;
    private List<String> fieldPreview;  // 前 5 个字段 label
    private List<BusinessFlowFormFieldVO> fields;  // 完整字段（含 internal/systemField）
  }
  ```
- **验收**:
  - 节点"表单权限"页签显示卡片列表（不是下拉）。
  - 卡片显示"采购单审批表单 · 代码 Provider · 12 个字段 · 字段：单号/申请人/部门/金额/需求日期"。
  - 选中后字段权限矩阵展示对应字段。

### Task 7: 代码应用表单字段只读面板（F7）

- **执行状态**: completed（2026-06-29 /apply）。代码应用设计器已开放“表单字段”只读面板，复用表单资产与流程绑定接口展示 Provider 字段和节点权限矩阵。

- **目标**：业务对象设计器对代码应用启用"表单字段"面板，展示 Provider 字段目录 + 节点字段权限矩阵（只读）。
- **涉及文件**:
    - `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue:240-258` — `form` 面板对 `isCodeApp` 启用，标题改"表单字段"。
    - 新增 `forge-admin-ui/src/views/app-center/components/designer/BusinessFormFieldsReadonlyPanel.vue` — 字段列表 + 节点权限切换。
    - `forge-admin-ui/src/api/business-app.js` — 字段目录、节点权限矩阵接口已存在则复用，否则新增。
    - 后端：必要时新增 `GET /ai/business/flow/form-assets/{objectCode}/fields-with-permissions?nodeId={nodeId}` —— 一次返回字段 + 当前节点权限矩阵。
- **关键签名**:
  ```vue
  <template>
    <div>
      <div class="readonly-banner">
        字段由代码 Provider 提供，编辑请联系开发人员。
      </div>
      <n-select v-model="selectedNodeId" :options="nodeOptions" placeholder="选择流程节点查看字段权限" />
      <n-table>
        <thead><tr>
          <th>字段编码</th><th>字段名</th><th>类型</th><th>必填</th>
          <th>系统字段</th><th>可见</th><th>可编辑</th><th>本节点必填</th>
        </tr></thead>
        <tbody>...</tbody>
      </n-table>
    </div>
  </template>
  ```
- **验收**:
  - 代码应用"采购申请"在应用中心设计器左侧 nav 出现"表单字段"。
  - 进入后看到完整字段目录与每个节点的权限矩阵（只读）。
  - 非代码应用（低代码业务对象）仍使用原"表单设计"面板。

---

## P2 — 整合收口（建议 1 个月内交付）

### Task 8: 通用流程入口弱化与菜单分组（F5）

- **执行状态**: partial（2026-06-29 /apply）。`/flow/form`、`/flow/template` 已加业务应用分流提示；菜单按 `superadmin` 隐藏未落地，因为当前前端菜单处理只支持后端资源树的 `visible/menuStatus`，没有 `requireRole` 字段。

- **目标**：`/flow/form`、`/flow/template`、`/flow/model`、`/flow/monitor` 归入"流程库管理"高级分组，普通用户菜单中隐藏。
- **涉及文件**:
    - `forge-admin-ui/src/views/flow/form.vue`、`template.vue` 头部 — 加 Banner。
    - 菜单数据源（`router/index.js` 或权限菜单种子）— 增加 `requireRole: ['flow_admin','superadmin']`。
    - 顶部"能力中心"页（`engines.vue`）— 增加"流程库管理"入口卡片。
    - 后端权限：如需 `flow_admin` 角色，新增种子（视 §9 待澄清结论）。
- **验收**:
  - 普通业务用户左侧菜单不再显示 `/flow/form` `/flow/template` `/flow/model` `/flow/monitor`。
  - 直接访问 URL 仍可（权限允许时）。
  - "能力中心"页有"流程库管理"卡片，进入后看到全部高级入口。
  - `/flow/form` 顶部 Banner 提示"该列表仅用于未绑定业务应用的独立流程..."。

### Task 9: 采购单端到端验证 + 文档归档

- **执行状态**: partial（2026-06-29 /apply）。已完成后端编译、前端构建和执行日志回填；未启动本地 admin/flow/ui 服务跑完整采购单端到端。

- **目标**：以采购单审批为试金石跑通所有改动，沉淀踩坑到 `code-copilot/memory/`。
- **涉及文件**:
    - `code-copilot/changes/unified-app-flow-experience/execution-log.md` — 记录每个 Task 的踩坑、决策偏差。
    - `code-copilot/changes/unified-app-flow-experience/test-spec.md` — 更新历史验证基线与本轮增量验证表。
    - `code-copilot/memory/pitfalls.md` / `decisions.md` — 沉淀。
- **验收**:
  - 端到端用例：申请人提交 → 部门负责人审批 → 工程经理会签 → 驳回修改 → 申请人改 `needDate` → 重新提交 → 通过 → 已办只读 完整跑通。
  - `/test` 命令产出的报告无 P0/P1 失败。
  - `execution-log.md` 至少 3 条踩坑或决策记录。

---

## 任务依赖图

```
P0:
  Task 1 (AiForm 字段权限) ──┐
  Task 2 (列表中文名)        ├─ 可并行
  Task 3 (移除技术标签)      ┘

P1:
  Task 4 (反查接口)
       └─ Task 5 (业务上下文注入)
  Task 6 (资产卡片化) ── 可独立
  Task 7 (代码应用表单面板) ── 可独立

P2:
  Task 8 (菜单弱化) ── 可独立
  Task 9 (端到端验证) ── 全部完成后
```

## 风险点回顾（详见 spec.md §8）

- 字段权限三态语义必须前后端 + 双引擎一致。
- 已办强制只读。
- 菜单弱化不能让流程库管理员失去入口。
- 旧 Provider 不强制声明 `internal`，默认全展示。
