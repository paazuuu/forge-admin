# 测试策略 — unified-business-flow-app-config
> status: executed
> created: 2026-06-28

## 1. 测试目标

验证统一业务流程应用配置能覆盖三类场景：

- 低代码业务对象：单据状态字段、流程绑定、节点表单、字段权限、发布检查。
- 代码优先复杂业务：采购单审批代码表单资产、节点字段权限、Provider 保存、已办只读。
- 应用设计体验：业务流程配置内完成流程关联和变量映射，并一键打开真实流程设计器；审批办理、表单资产和字段权限在流程节点抽屉中配置。
- 独立流程：未绑定业务应用时，流程设计器原有动态表单和表单 URL 能力不受影响。

## 2. 后端验证

### 2.1 统一配置 Facade

- `GET /ai/business/flow-app/config/{objectCode}` 返回对象信息、单据配置、流程绑定、表单资产。
- `PUT /ai/business/flow-app/config/{objectCode}` 保存后再次查询一致。
- 单据保存失败或流程绑定保存失败时，事务不留下半配置。

### 2.2 表单资产目录

- 低代码表单资产包含 `formMode=BUSINESS_OBJECT_FORM`、`formKey`、`fields`。
- 代码表单资产包含 `formMode=BUSINESS_CODE_FORM`、`providerKey`、`formKey`、`fields`、`supportsSave`。
- Provider 不存在或资产不存在时，发布检查能提示。

### 2.3 字段权限

- 可写字段提交后落库。
- 不可写字段出现在请求体中也被过滤。
- 必填字段缺失返回业务错误。
- `task-form-context/readonly` 强制所有字段只读。

## 3. 前端验证

### 3.1 统一配置页

- 能从业务对象设计器进入统一“业务流程配置”页。
- 旧 `panel=document`、`panel=automation` 参数仍能打开统一页并定位对应分区。
- 保存按钮一次提交单据与流程配置。

### 3.2 表单资产选择器

- 低代码表单按表单名称展示。
- 采购单审批代码表单按“采购单审批表单”展示。
- 外部地址位于开发者高级配置，不作为普通默认入口。

### 3.3 流程设计器入口与节点配置

- 应用设计的“业务流程配置”内提供打开流程设计器入口，并携带业务对象上下文。
- 选中人工节点后，右侧节点配置展示 `审批设置 / 表单权限 / 高级设置` 页签。
- `表单权限` 页签能选择当前节点使用的业务表单资产。
- `表单权限` 页签使用字段权限矩阵，能按字段配置可见、可编辑、必填。
- 保存流程后写入 BPMN 节点 `formKey/formFieldPermissions`，待办表单优先按该权限生效。
- 未绑定流程仍能配置原有流程表单。
- 条件线仍能通过“同意通过 / 驳回修改 / 业务字段”配置，不要求普通用户手写表达式。

## 4. 采购单审批验收

测试流程：

1. 初始化采购单审批测试流程。
2. 新增采购单并提交审批。
3. 部门负责人节点打开待办，确认“上传清单”字段可见可编辑。
4. 工程部经理节点打开待办，确认“上传清单”不可编辑，工程经理意见可编辑。
5. 会签节点多人办理，确认会签意见按节点权限可编辑。
6. 任一审批节点驳回到申请人修改。
7. 申请人修改业务字段后重提。
8. 流程通过后，在我的已办打开详情，确认表单内容只读可见。

## 5. 自动化命令

后端目标编译：

```bash
cd forge-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-business/forge-business-core -am -DskipTests compile
```

前端验证：

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint src/api/business-app.js src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue src/views/app-center/components/designer/BusinessFlowFormAssetSelect.vue src/components/bpmn/NodePropertiesPanel.vue src/components/flow-designer/DingFlowDesigner.vue
pnpm build
```

通用检查：

```bash
git diff --check
```

## 6. 本轮提案阶段结果

- 仅创建 SDD 文档，未执行代码验证。
- 编码实现完成后按本文件做增量验证，并把结果追加到 `execution-log.md`。

## 7. 2026-06-28 Apply 阶段结果

- 后端目标编译通过：
  - `forge-plugin-generator` 及依赖模块 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile`
  - `forge-business-core` 及依赖模块 `mvn -pl forge-business/forge-business-core -am -DskipTests compile`
- 前端目标 ESLint 通过，覆盖统一配置 API、对象设计器、统一配置面板、资产选择器、两个流程设计器表单提示链路和应用中心入口卡片。
- 前端 `pnpm build` 通过；仍有既有 Vite 动态导入、CSS `//` 注释和大 chunk warning，不阻断本轮变更。
- `git diff --check` 通过。
- 未执行服务级接口联调和采购单全流程人工验收；原因是本轮按用户要求先完成改造，最终由用户统一验证，本地未启动后端、前端、MySQL、Redis 和流程服务。

## 8. 2026-06-28 配置页重构与采购申请接入增量验证

- 覆盖范围：
  - 业务流程配置页工作台式布局重构。
  - 采购申请代码应用进入统一业务流程配置。
  - 代码应用无低代码对象时的后端统一配置 fallback。
  - 代码应用模式下默认 `ADAPTER` 接入和代码表单资产默认选择。
- 后端目标编译通过：
  - `forge-plugin-generator` 及依赖模块 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile`
  - `forge-business-core` 及依赖模块 `mvn -pl forge-business/forge-business-core -am -DskipTests compile`
- 前端目标 ESLint 通过，覆盖：
  - `BusinessFlowAppConfigPanel.vue`
  - `BusinessFlowBindingPanel.vue`
  - `BusinessObjectDesignerShell.vue`
  - `object-designer.[objectCode].vue`
  - `purchase-order-test.vue`
- 前端 `pnpm build` 通过；仍有既有 Vite 动态导入、CSS `//` 注释和大 chunk warning，不阻断本轮变更。
- `git diff --check` 通过。
- 未启动本地服务做浏览器截图和采购单全流程人工验收；原因是用户计划最后统一验证，本轮未启动后端、前端、MySQL、Redis 和流程服务。

## 9. 2026-06-28 配置页交互修正增量验证

- 覆盖范围：
  - 移除业务流程配置页左侧滚动锚点，改为顶部步骤切换。
  - 对齐列表设计器的标题工具栏、切换条和工作区结构。
  - 代码应用模式隐藏低代码业务表/状态字段配置，只展示代码适配器摘要。
- 前端目标 ESLint 通过，覆盖：
  - `BusinessFlowAppConfigPanel.vue`
  - `BusinessFlowBindingPanel.vue`
  - `BusinessObjectDesignerShell.vue`
  - `object-designer.[objectCode].vue`
  - `purchase-order-test.vue`
- 前端 `pnpm build` 通过；仍有既有 Vite 动态导入、CSS `//` 注释和大 chunk warning，不阻断本轮变更。
- `git diff --check` 通过。
- 本轮未改后端，未重复执行 Maven 编译；上一轮后端目标模块编译已通过。

## 10. 2026-06-29 顶部布局收敛增量验证

- 覆盖范围：
  - 移除业务流程配置内层重复标题、刷新/保存按钮和旧配置链路摘要。
  - 父级对象设计器在业务流程配置面板下隐藏“单据闭环配置”侧栏，避免导航和状态摘要重复。
  - 检索确认旧堆叠文案不再存在。
- 前端目标 ESLint 通过，覆盖：
  - `BusinessFlowAppConfigPanel.vue`
  - `BusinessObjectDesignerShell.vue`
  - `object-designer.[objectCode].vue`
- 前端 `pnpm build` 通过；仍有既有 Vite 动态导入、CSS `//` 注释和大 chunk warning，不阻断本轮变更。
- `git diff --check` 通过。
- 本轮只改前端布局展示，未重复执行 Maven 编译。

## 11. 2026-06-29 页签和内嵌卡片布局增量验证

- 覆盖范围：
  - 业务流程配置顶部只保留“单据规则 / 流程配置”两个本地页签。
  - 移除自动化触发器跳转页签和发布检查页签。
  - 内嵌单据规则、流程配置统一内容宽度、卡片样式和两列布局规则。
- 前端目标 ESLint 通过，覆盖：
  - `BusinessFlowAppConfigPanel.vue`
- 前端 `pnpm build` 通过；仍有既有 Vite 动态导入、CSS `//` 注释和大 chunk warning，不阻断本轮变更。
- `git diff --check` 通过。
- 本轮只改前端布局展示，未重复执行 Maven 编译。

## 12. 2026-06-29 参考业务配置中心样式增量验证

- 覆盖范围：
  - 业务流程配置顶部改为面包屑标题和下划线页签。
  - 单据规则和流程配置分别展示四步横向卡片。
  - 内容区统一为参考图风格的浅灰工作区、白色卡片和两列布局。
  - 告警条移动到内容区内，避免影响主 grid 高度。
- 前端目标 ESLint 通过，覆盖：
  - `BusinessFlowAppConfigPanel.vue`
- 前端 `pnpm build` 通过；仍有既有 Vite 动态导入、CSS `//` 注释和大 chunk warning，不阻断本轮变更。
- `git diff --check` 通过。
- 本轮只改前端布局展示，未重复执行 Maven 编译。

## 13. 2026-06-29 步骤与内容对应关系增量验证

- 覆盖范围：
  - 四步卡片的 `active` 态补齐，避免点击非第一步后顶部仍显示未选中。
  - 下方工作区新增当前配置项标题，展示当前页签、步骤标题和说明。
  - 单据规则步骤只显示对应单据卡片；流程配置步骤只显示对应流程卡片。
  - 旧单据内部时间轴继续在统一配置页隐藏，避免出现两套步骤。
- 前端目标 ESLint 通过，覆盖：
  - `BusinessFlowAppConfigPanel.vue`
- 前端 `pnpm build` 通过；仍有既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释、chunk 体积提示，不阻断本轮变更。
- `git diff --check` 通过。
- 本轮只改前端布局和交互状态，未重复执行 Maven 编译。

## 14. 2026-06-29 代码业务运行时接入增量验证

- 覆盖范围：
  - 采购单代码表单资产补充 `fields/fieldCatalog/supportsSave` 和文件上传字段元数据。
  - 采购单样例 BPMN 移除节点 `formUrl/formFieldPermissions` 写死配置。
  - 采购单默认 `nodeForms` 通过 Flyway seed 写入 `ai_business_binding`。
  - 待办/已办页对代码业务和低代码业务统一使用业务表单上下文渲染。
  - 快速审批校验在业务流程配置接管时不再先按旧 BPMN dynamic/external 表单拦截。
  - 后端代码表单保存前按节点可写字段过滤，并校验必填字段。
- 后端目标编译通过：
  - `forge-plugin-generator` 及依赖模块 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile`
  - `forge-business-core` 及依赖模块 `mvn -pl forge-business/forge-business-core -am -DskipTests compile`
- 前端目标 ESLint 通过，覆盖：
  - `business-app.js`
  - `BusinessFlowAppConfigPanel.vue`
  - `BusinessFlowBindingPanel.vue`
  - `BusinessFlowFormAssetSelect.vue`
  - `BusinessObjectDesignerShell.vue`
  - `object-designer.[objectCode].vue`
  - `purchase-order-test.vue`
  - `todo.vue`
  - `done.vue`
- 前端 `pnpm build` 通过，`✓ built in 2m 12s`；仍有既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释、chunk 体积提示，不阻断本轮变更。
- `git diff --check` 通过。
- 静态核查通过：
  - 采购单 BPMN 和新增 SQL 中没有 `formUrl/formFieldPermissions` 旧配置。
  - 新增 SQL 中没有 `${...}` Flyway placeholder 风险。
- 未启动后端、前端、MySQL、Redis 或 Flow 服务做采购单完整人工流转验收。

## 15. 2026-06-29 采购单样例应用中心入口增量验证

- 覆盖范围：
  - 采购单审批测试以 `ai_business_suite / ai_business_object / ai_business_app` 元数据进入应用中心。
  - 应用中心首页业务单元卡片和业务域详情对象卡片识别代码型业务对象，主按钮打开“业务流程配置”。
  - 对象设计器读取代码型业务对象后兜底切换到 `flow-app` 面板。
- 前端目标 ESLint 通过，覆盖：
  - `BusinessUnitCard.vue`
  - `ObjectCard.vue`
  - `object-designer.[objectCode].vue`
- 前端 `pnpm build` 通过，`✓ built in 2m 24s`；仍有既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释、chunk 体积提示，不阻断本轮变更。
- `git diff --check` 通过。
- SQL 静态核查通过：
  - `V1.0.83__seed_sample_purchase_order_app_center_entry.sql` 文件名版本唯一。
  - 新增 SQL 中没有 `${...}` Flyway placeholder、`tenant_id = 0`、错误租户插入或旧 `formUrl/formFieldPermissions` 配置。
- 本轮只新增应用中心 seed 数据和前端入口适配，未重复执行 Maven 编译。
- 未启动后端、前端、MySQL、Redis 或 Flow 服务做应用中心页面人工点击验收；迁移执行和页面刷新需在本地运行环境中完成。

## 16. 2026-06-29 字段目录补全与节点权限矩阵增量验证

- 覆盖范围：
  - 采购单代码表单资产补齐单据字段目录，包含采购单号、状态、申请人、部门、流程实例、审批人、会签人、驳回原因、创建/更新时间等只读字段。
  - `needDate` 接入待办节点保存 DTO 和申请人修改节点保存逻辑。
  - 新增 `V1.0.84__extend_sample_purchase_order_form_asset_fields.sql`，对已存在默认绑定补充申请人修改节点的 `needDate` 字段权限。
  - 节点表单策略从三个多选框改为字段权限矩阵，按字段配置可见、可编辑、必填。
- 后端目标编译通过：
  - `forge-business-core` 及依赖模块 `mvn -pl forge-business/forge-business-core -am -DskipTests compile`
- 前端目标 ESLint 通过，覆盖：
  - `BusinessFlowBindingPanel.vue`
- 前端 `pnpm build` 通过，`✓ built in 2m 12s`；仍有既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释、chunk 体积提示，不阻断本轮变更。
- `git diff --check` 通过。
- SQL 静态核查通过：
  - 新增 SQL 中没有 `${...}` Flyway placeholder 或 `tenant_id=0` 风险。
- 未启动后端、前端、MySQL、Redis 或 Flow 服务做页面点击和迁移实跑验收。

## 17. 2026-06-29 废弃过渡方案增量验证

- 覆盖范围：
  - 曾经尝试在业务流程配置中维护独立节点配置面板。
  - 该方向已废弃，保留本节只作为历史验证记录。
  - `审批设置 / 表单权限 / 高级设置` 页签切换。
  - 字段权限矩阵在节点上下文中保存并回显。
- 本轮已验证：
  - `BusinessFlowBindingPanel.vue` 和 `BusinessFlowAppConfigPanel.vue` 目标 ESLint 通过。
  - 前端 `pnpm build` 通过。
  - Vite dev server 已启动，`curl -I http://127.0.0.1:5173/` 返回 `HTTP/1.1 200 OK`。
- 后续仍需完整验收：
  - 本节记录的是已废弃的过渡方案；后续按第 19 节验证真实流程设计器节点配置。
  - 从应用中心进入采购单审批测试，点击“打开流程设计器”进入真实流程设计器。
  - 选中部门负责人节点后，“表单权限”页签能看到采购单字段目录。
  - 调整 `arrivalListFileIds` 可编辑权限并保存后，`ai_business_binding.binding_config.nodeForms` 中对应节点权限更新。
  - 待办打开部门负责人节点时，上传清单字段可编辑；其它节点仍按各自权限只读或隐藏。
  - 未绑定业务应用的独立流程仍能在流程管理中使用原动态表单和表单 URL 配置。
  - 在流程设计器选中画布节点能驱动同一套节点配置页签。

## 18. 2026-06-29 代码业务字段候选项与变量映射增量验证

- 覆盖范围：
  - `sample_purchase_order` 没有低代码运行 `configKey` 时，变量候选项接口仍能从代码表单 Provider 的 `fields/fieldCatalog` 读取单据字段。
  - 变量映射编辑器左侧“单据字段”选项合并接口返回的 `fieldCandidates`，不再依赖低代码表单字段 props。
  - 变量映射区域展示“业务字段 -> 流程变量”说明，提示只映射条件分支、审批人表达式或标题模板真正用到的字段。
- 本轮已验证：
  - `forge-plugin-generator` 目标模块 Maven compile 通过。
  - `BusinessFlowBindingPanel.vue`、`FlowVariableMappingEditor.vue` 目标 ESLint 通过。
  - 前端 `pnpm build` 通过。
  - `git diff --check` 通过。
  - 相关文件尾随空白扫描无匹配。
  - Vite dev server 仍可访问，`curl -I http://127.0.0.1:5173/` 返回 `HTTP/1.1 200 OK`。
- 后续仍需完整验收：
  - 登录本地应用中心后进入 `sample_purchase_order` 业务流程配置，确认不再出现“业务对象缺少运行配置，无法读取字段候选项”的错误提示。
  - 在变量映射左侧下拉中确认能看到采购单号、采购主题、采购金额、部门负责人、工程部经理、会签人员等代码表单字段。
  - 保存推荐映射后，流程条件线和审批人表达式能读取对应流程变量。

## 19. 2026-06-29 流程设计器节点配置归位增量验证

- 覆盖范围：
  - 应用配置页移除独立节点配置工作台，改为打开流程设计器入口。
  - 流程设计器带 `businessObjectCode` 上下文时加载业务表单资产和字段目录。
  - 审批节点抽屉的“表单权限”页签支持选择节点表单资产，并按所选资产字段配置权限。
  - 后端待办上下文优先读取 Flow 节点 `formKey/formFieldPermissions`，`BusinessFlowBinding.nodeForms` 仅兜底。
  - 采购单样例 BPMN 人工节点内置 `formKey/formFieldPermissions`。
- 本轮已验证：
  - `forge-business-core` 及依赖模块 Maven compile 通过。
  - 目标前端 ESLint 通过，覆盖 `BusinessFlowBindingPanel.vue`、`flow/design.vue`、`DingFlowDesigner.vue`、`NodeConfigDrawer.vue`、`ApproverConfig.vue`、`FormPermissionConfig.vue`、`NodePropertiesPanel.vue`。
  - `git diff --check` 通过。
  - 相关文件尾随空白扫描无匹配。
- 后续仍需完整验收：
  - 登录本地应用中心后进入 `sample_purchase_order` 业务流程配置，点击“打开流程设计器”。
  - 在流程设计器中选中部门负责人节点，确认“表单权限”页签可选择“采购单审批表单”。
  - 修改字段权限并保存流程后，确认 BPMN XML 节点包含更新后的 `flowable:formFieldPermissions`。
  - 打开对应待办，确认运行时按流程节点配置渲染表单字段权限。
