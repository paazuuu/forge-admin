# 执行日志 — unified-business-flow-app-config

## 2026-06-28 SDD 提案

### 本轮动作

- 创建 `spec.md`，明确统一业务流程应用配置的背景、现状、功能点、业务规则、接口和风险。
- 创建 `tasks.md`，拆分后端 Facade、前端统一面板、表单资产选择器、流程设计器所有权提示、发布检查和采购单样例验收任务。
- 创建 `test-spec.md`，记录后端、前端和采购单审批验收策略。
- 未修改业务代码。

### 验证结果

- 本轮为提案阶段，仅做文档落地，未执行编译和前端构建。
- 后续进入 `/apply unified-business-flow-app-config` 后按 `test-spec.md` 执行增量验证。

### 风险记录

- 统一配置会触及状态流转和字段权限，编码阶段必须验证后端字段权限兜底。
- 标准 BPMN 与钉钉样式设计器都有表单权限入口，编码阶段必须同时兼容。

## 2026-06-28 Apply 实现与验证

### 本轮动作

- 新增后端统一配置 Facade：`GET/PUT /ai/business/flow-app/config/{objectCode}`，事务保存单据配置和流程绑定。
- 前端业务对象设计器合并入口为“业务流程配置”，旧 `panel=document`、`panel=automation` 继续映射到新面板。
- 节点表单配置改为选择业务表单资产，支持低代码表单、代码表单 Provider 和开发者外部地址。
- 代码表单 Provider 资产补齐 `fields/fieldCatalog/providerKey/providerName/supportsSave/formMode`，采购单审批表单暴露字段目录。
- `BUSINESS_CODE_FORM` 保存前按节点字段权限过滤提交数据；代码优先业务没有低代码 `configKey` 时允许进入 Provider 表单上下文。
- 标准 BPMN 与钉钉样式设计器增加业务表单所有权提示，绑定业务应用时隐藏流程设计器里的节点业务表单主配置。
- 应用中心卡片和自定义操作提示文案收口到“业务流程配置”。

### 验证命令与结果

```bash
cd forge-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile
```

- 结果：通过，`BUILD SUCCESS`。
- 警告：既有 `GenTableServiceImpl` deprecation、`BusinessObjectDesignerService` unchecked warning，不阻断。

```bash
cd forge-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-business/forge-business-core -am -DskipTests compile
```

- 结果：通过，`BUILD SUCCESS`。
- 警告：同上游依赖编译 warning，不阻断。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint src/api/business-app.js 'src/views/app-center/object-designer.[objectCode].vue' src/views/app-center/components/BusinessUnitCard.vue src/views/app-center/components/ObjectCard.vue src/views/app-center/components/designer/BusinessActionDesigner.vue src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue src/views/app-center/components/designer/BusinessFlowFormAssetSelect.vue src/views/app-center/components/designer/BusinessFlowBindingPanel.vue src/views/app-center/components/designer/BusinessDocumentPanel.vue src/views/app-center/components/designer/BusinessObjectDesignerShell.vue src/components/bpmn/NodePropertiesPanel.vue src/components/flow-designer/DingFlowDesigner.vue src/components/flow-designer/panel/NodeConfigDrawer.vue src/components/flow-designer/panel/ApproverConfig.vue src/components/flow-designer/panel/FormPermissionConfig.vue
```

- 结果：通过。
- 修复记录：首次运行发现 `BusinessFlowAppConfigPanel.vue` 的 `open-trigger/open-publish` emit 命名不符合 camelCase，已改为 `openTrigger/openPublish` 后复跑通过。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

- 结果：通过，`✓ built in 1m 25s`。
- 警告：既有 `UserSelectModal` 组件命名冲突、动态导入与静态导入混用、CSS `//` 注释、chunk 体积提示；本轮未新增阻断项。

```bash
git diff --check
```

- 结果：通过。

### 跳过项

- 未启动后端、前端、MySQL、Redis 或 Flow 服务做接口联调。
- 未执行采购单审批“提交 -> 部门负责人 -> 工程经理 -> 会签 -> 驳回修改 -> 重提 -> 已办只读”全流程人工验收；用户计划最后统一验证。
- 未新增采购单 `ai_business_binding.nodeForms` 内置 Flyway 绑定脚本，本轮只补齐代码表单资产和运行时兼容。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-29 业务流程配置顶部布局收敛

### 本轮动作

- 继续收敛 `BusinessFlowAppConfigPanel.vue` 顶部区域：移除内层“对象名 · 业务流程配置”标题、重复刷新/保存按钮和旧配置链路摘要，只保留紧凑的“配置项”分段切换。
- 普通业务对象进入“业务流程配置”时，父级 `closureSteps` 不再同时展示“单据闭环配置”侧栏，避免同一状态在侧栏和面板顶部重复出现。
- 保留外层设计器统一标题、保存、刷新入口；内层面板只负责单据规则、流程配置、触发器、发布检查的业务配置内容。
- 检索确认当前源码中已无 `配置链路`、`未配置状态字段`、`未选择流程模型`、`单据规则 · 主流程` 等旧堆叠文案。

### 验证命令与结果

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue src/views/app-center/components/designer/BusinessObjectDesignerShell.vue 'src/views/app-center/object-designer.[objectCode].vue'
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

- 结果：通过，`✓ built in 1m 13s`。
- 警告：仍为既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释、chunk 体积提示；本轮未新增阻断项。

```bash
git diff --check
```

- 结果：通过。

### 跳过项

- 本轮仅调整前端布局与父级显示逻辑，未重复执行 Maven 编译。
- 未启动 Vite/后端服务做浏览器截图；原因是用户计划最后统一验证，本轮已完成 lint/build 和源码旧文案检索。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-28 配置页交互修正

### 本轮动作

- 按列表设计器的工作台风格重构 `BusinessFlowAppConfigPanel.vue`：保留顶部标题工具栏，新增顶部步骤切换条，移除不可感知的左侧滚动锚点。
- 步骤点击从 `scrollIntoView` 改为真实分区切换：低代码对象在“单据规则 / 流程配置”之间切换，触发器和发布检查作为明确跳转命令。
- 代码应用模式直接进入“流程配置”，不再显示低代码单据规则。
- `BusinessFlowBindingPanel.vue` 在代码应用模式下隐藏“低代码业务表/状态字段”配置，替换为“代码适配器”接入说明和业务编码/主键/状态回写摘要。

### 验证命令与结果

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue src/views/app-center/components/designer/BusinessFlowBindingPanel.vue src/views/app-center/components/designer/BusinessObjectDesignerShell.vue 'src/views/app-center/object-designer.[objectCode].vue' src/views/business/purchase-order-test.vue
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

- 结果：通过，`✓ built in 1m 54s`。
- 警告：仍为既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释、chunk 体积提示；本轮未新增阻断项。

```bash
git diff --check
```

- 结果：通过。

### 跳过项

- 本轮未改后端代码，未重复执行 Maven 编译；上一轮 `forge-plugin-generator` 和 `forge-business-core` 编译已通过。
- 未启动浏览器做截图验收；用户反馈后已按交互和布局问题直接修正，最终由用户统一验证。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-29 参考业务配置中心样式重构

### 本轮动作

- 参考用户提供的“业务配置中心”截图，重构 `BusinessFlowAppConfigPanel.vue` 的页面结构。
- 顶部由小胶囊按钮改为“业务配置中心 > 当前配置页”面包屑标题和下划线页签。
- 配置进度改为横向四步卡片：单据规则展示“基础配置 / 编号生成 / 状态字典 / 主流程”，流程配置展示“默认流程 / 业务记录绑定 / 变量映射 / 节点表单”。
- 内容区继续复用既有单据规则和流程配置能力，但外层统一为参考图的浅灰工作区、白色卡片、8px 圆角、两列卡片布局和响应式收缩规则。
- 告警条移动到内容区内，避免无告警时占据主布局行高。

### 验证命令与结果

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

- 结果：通过，`✓ built in 1m 25s`。
- 警告：仍为既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释、chunk 体积提示；本轮未新增阻断项。

```bash
git diff --check
```

- 结果：通过。

### 跳过项

- 本轮仅调整前端页面结构和样式，未重复执行 Maven 编译。
- 未启动浏览器做截图验收；原因是本轮已按用户提供截图做结构对齐并完成 lint/build，最终由用户统一验证。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-29 业务流程配置页签与内嵌布局修正

### 本轮动作

- `BusinessFlowAppConfigPanel.vue` 顶部页签只保留“单据规则 / 流程配置”；移除“自动化触发器”和“发布检查”页签，避免用户点击触发器直接跳出当前配置页面。
- 统一内嵌单据规则、流程配置的内容宽度为 `1180px`，背景、卡片边框、圆角、阴影、间距改为同一套后台工作区样式。
- 单据规则内部保留状态进度条，基础配置和编号生成按规整两列展示，小屏自动单列。
- 流程配置内部将“默认流程 / 业务记录绑定”按两列展示，“变量映射 / 节点表单策略”横向铺满，减少卡片无规律堆叠。

### 验证命令与结果

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

- 结果：通过，`✓ built in 1m 20s`。
- 警告：仍为既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释、chunk 体积提示；本轮未新增阻断项。

```bash
git diff --check
```

- 结果：通过。

### 跳过项

- 本轮仅调整前端页面布局与本地页签，不涉及后端逻辑，未重复执行 Maven 编译。
- 未启动浏览器做截图验收；用户计划最后统一验证，本轮已完成 lint/build。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-28 配置页重构与采购申请接入补充验证

### 本轮动作

- 重构 `BusinessFlowAppConfigPanel.vue`：由旧面板堆叠改为工作台布局，包含顶部工具栏、指标摘要、左侧步骤导航和统一配置区。
- 统一配置面板支持代码应用模式：隐藏低代码单据规则，展示代码应用接入状态，只维护流程绑定和节点表单策略。
- 后端 `BusinessFlowAppConfigService` 支持代码应用 fallback：低代码业务对象不存在但存在代码表单资产时，仍返回统一配置结构并允许保存流程绑定。
- 采购单审批代码表单资产增加 `appName/objectName/businessName`，配置页展示为“采购申请”。
- 采购单审批测试页新增“业务流程配置”按钮，跳转到代码应用统一配置页。
- 对象设计器支持代码应用虚拟上下文，代码应用场景只展示“业务流程配置”导航，并隐藏预览/发布按钮。
- `BusinessFlowBindingPanel` 增加 `codeApp` 模式：默认 `ADAPTER` 接入，节点表单默认 `BUSINESS_CODE_FORM`，唯一代码表单资产自动填充到人工节点。

### 验证命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue src/views/app-center/components/designer/BusinessFlowBindingPanel.vue src/views/app-center/components/designer/BusinessObjectDesignerShell.vue 'src/views/app-center/object-designer.[objectCode].vue' src/views/business/purchase-order-test.vue
```

- 结果：通过。
- 修复记录：首次运行发现 `BusinessFlowAppConfigPanel.vue` 的 `else` 换行风格和 `purchase-order-test.vue` 导入顺序不符合项目规则，已修复并复跑通过。

```bash
cd forge-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile
```

- 结果：通过，`BUILD SUCCESS`。
- 警告：既有 deprecation / unchecked warning，不阻断。

```bash
cd forge-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-business/forge-business-core -am -DskipTests compile
```

- 结果：通过，`BUILD SUCCESS`。
- 警告：同上游依赖编译 warning，不阻断。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

- 结果：通过，`✓ built in 1m 40s`。
- 警告：既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释、chunk 体积提示；本轮未新增阻断项。

```bash
git diff --check
```

- 结果：通过。

### 跳过项

- 未启动后端、前端、MySQL、Redis 或 Flow 服务做接口联调。
- 未执行浏览器截图验证；原因是本轮未启动完整前后端服务，采购单全流程由用户最后统一验证。
- 未执行采购单审批“提交 -> 部门负责人 -> 工程经理 -> 会签 -> 驳回修改 -> 重提 -> 已办只读”全流程人工验收。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-29 业务流程配置步骤联动修正

### 本轮动作

- 修复 `BusinessFlowAppConfigPanel.vue` 四步卡片选中态不完整的问题：`编号生成`、`状态字典`、`业务记录绑定`、`变量映射`、`节点表单` 现在都会根据当前步骤同步高亮。
- 下方工作区新增当前配置项标题栏，直接展示当前页签、步骤标题和说明，避免顶部时间轴和下面内容看起来不对应。
- 统一配置页继续隐藏旧 `BusinessDocumentPanel` 内部 `document-rail`，只保留外层一套步骤。
- 单据规则阶段通过 CSS 只展示对应单据卡片；流程配置阶段只展示对应流程卡片，`主流程` 会切换到流程配置的默认流程卡片。

### 验证命令与结果

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue
```

- 首次结果：失败，原因是新增 `activeStageMeta` 时引用 `activeStages` 的定义顺序不符合 `no-use-before-define`。
- 修复后复跑结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

- 结果：通过，`✓ built in 1m 16s`。
- 警告：仍为既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释和 chunk 体积提示；本轮未新增阻断项。

```bash
git diff --check
```

- 结果：通过。

### 跳过项

- 本轮只调整前端页面结构、选中态和样式映射，未重复执行 Maven 编译。
- 未启动浏览器做截图验收；用户计划最后统一验证，本轮已完成 lint/build。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-29 代码业务运行时接入修正

### 本轮动作

- 采购单代码表单 Provider 改为只提供表单资产、字段目录、记录上下文和保存能力；节点级可写/必填权限归业务流程配置维护。
- 采购单样例 BPMN 移除所有人工节点上的 `flowable:formUrl` 和 `flowable:formFieldPermissions`，避免流程模型和业务流程配置双写。
- 新增 `V1.0.82__seed_sample_purchase_order_flow_binding.sql`，为 `sample_purchase_order` 内置默认 `ai_business_binding`，节点表单配置落到 `binding_config.nodeForms`。
- `BusinessFlowService` 支持纯代码业务无低代码 `configKey` 时加载代码 Provider，上下文和保存都按节点字段权限过滤。
- 待办/已办页把代码业务纳入统一业务表单上下文渲染；外部采购单页面只保留为“打开完整业务页”的辅助入口。
- 待办快速审批校验增加业务配置接管判断，业务上下文存在时不再先按旧 BPMN dynamic/external 表单拦截。

### 验证命令与结果

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
git diff --check
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project/forge-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile
```

- 结果：通过，`BUILD SUCCESS`。
- 警告：既有 deprecation / unchecked warning，不阻断。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project/forge-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-business/forge-business-core -am -DskipTests compile
```

- 结果：通过，`BUILD SUCCESS`。
- 警告：同上游依赖编译 warning，不阻断。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm --dir forge-admin-ui exec eslint src/api/business-app.js src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue src/views/app-center/components/designer/BusinessFlowBindingPanel.vue src/views/app-center/components/designer/BusinessFlowFormAssetSelect.vue src/views/app-center/components/designer/BusinessObjectDesignerShell.vue 'src/views/app-center/object-designer.[objectCode].vue' src/views/business/purchase-order-test.vue src/views/flow/todo.vue src/views/flow/done.vue
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

- 结果：通过，`✓ built in 2m 12s`。
- 警告：仍为既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释、chunk 体积提示；本轮未新增阻断项。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
rg -n "formUrl|formFieldPermissions|/business/purchase-order-test" forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/support/SamplePurchaseOrderFlowBpmn.java forge-server/db/migration/V1.0.82__seed_sample_purchase_order_flow_binding.sql
rg -n "\$\{" forge-server/db/migration/V1.0.82__seed_sample_purchase_order_flow_binding.sql
```

- 结果：均无匹配，符合预期。BPMN 中不再内嵌业务表单 URL/字段权限，新增 SQL 不含 Flyway placeholder。

### 跳过项

- 未启动后端、前端、MySQL、Redis 或 Flow 服务做接口联调。
- 未执行采购单审批“提交 -> 部门负责人 -> 工程经理 -> 会签 -> 驳回修改 -> 重提 -> 已办只读”完整人工验收；原因是本轮完成代码接入和静态/构建验证，完整链路依赖本地运行环境。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-29 采购单样例应用中心入口修正

### 本轮动作

- 新增 `V1.0.83__seed_sample_purchase_order_app_center_entry.sql`，把采购单审批测试写入应用中心元数据：`采购` 业务域、`采购申请` 业务对象、`采购单审批测试` 应用入口。
- 应用入口使用 `ROUTE` 模式打开 `/business/purchase-order-test`，避免代码业务被低代码 `RUNTIME/configKey` 校验拦截。
- 采购申请业务对象写入 `designer_options={"codeApp":true,...}`，应用中心卡片识别后默认展示“业务流程配置”。
- `BusinessUnitCard.vue` 和 `ObjectCard.vue` 识别 `options/designerOptions.codeApp=true` 后，主设计按钮直接进入 `flow-app`。
- `object-designer.[objectCode].vue` 增加代码型业务对象兜底切换，避免从应用中心进入后停在低代码表单设计页。

### 验证命令与结果

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
git diff --check
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
rg --files forge-server/db/migration | rg "V1\.0\.83"
rg -n "\$\{|tenant_id = 0|tenant_id,.*0|formUrl|formFieldPermissions" forge-server/db/migration/V1.0.83__seed_sample_purchase_order_app_center_entry.sql
```

- 结果：版本文件唯一；第二条命令无匹配，符合预期。新增 SQL 不含 Flyway placeholder、错误租户插入或旧表单配置。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/BusinessUnitCard.vue src/views/app-center/components/ObjectCard.vue 'src/views/app-center/object-designer.[objectCode].vue'
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

- 结果：通过，`✓ built in 2m 24s`。
- 警告：仍为既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释和 chunk 体积提示；本轮未新增阻断项。

### 跳过项

- 本轮未改 Java 代码，未重复执行 Maven 编译。
- 未启动后端、前端、MySQL、Redis 或 Flow 服务做页面点击和迁移实跑验收；应用中心显示效果依赖本地 Flyway 执行 `V1.0.83` 后刷新页面。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-29 字段目录补全与节点权限矩阵修正

### 本轮动作

- 补全 `SamplePurchaseOrderCodeFormProvider` 的代码表单字段资产，字段目录从仅业务可编辑字段扩展为完整单据字段，补齐采购单号、状态、申请人、部门、业务 Key、流程实例、审批人、会签人、驳回原因和创建/更新时间等只读字段。
- `BusinessTaskFormContextVO.recordData` 同步补齐上述字段，避免字段目录有字段但待办/已办上下文没有值。
- `SamplePurchaseOrderTaskSaveDTO` 和 `SamplePurchaseOrderServiceImpl` 增加 `needDate` 保存，支持申请人修改节点配置并保存期望到货日期。
- 新增 `V1.0.84__extend_sample_purchase_order_form_asset_fields.sql`，对已经存在的采购单默认流程绑定补充申请人修改节点的 `needDate` 权限。
- `BusinessFlowBindingPanel.vue` 的节点表单策略从“可见字段 / 可编辑字段 / 必填字段”三个多选框改为字段权限矩阵，按行展示字段名、字段编码、可见、可编辑、必填，接近用户参考图的表单权限体验。
- 权限矩阵保留原后端数据结构：`visibleFields/writableFields/requiredFields` 仍会归一化为 `fieldPermissions`，只读字段可见但不能配置可编辑或必填。

### 验证命令与结果

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
git diff --check
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
rg -n "\$\{|tenant_id = 0|tenant_id,.*0" forge-server/db/migration/V1.0.84__extend_sample_purchase_order_form_asset_fields.sql
```

- 结果：无匹配，符合预期。新增 SQL 不含 Flyway placeholder 或错误租户写入。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project/forge-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-business/forge-business-core -am -DskipTests compile
```

- 结果：通过，`BUILD SUCCESS`。
- 警告：既有 deprecation / unchecked warning，不阻断。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessFlowBindingPanel.vue
```

- 首次结果：失败，原因是 `setNodeFieldVisible` 新增分支缺少大括号，不符合 `antfu/curly`。
- 修复后复跑结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

- 结果：通过，`✓ built in 2m 12s`。
- 警告：仍为既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释和 chunk 体积提示；本轮未新增阻断项。

### 跳过项

- 未启动后端、前端、MySQL、Redis 或 Flow 服务做页面点击和迁移实跑验收。
- 未执行采购单审批完整人工流转；本轮覆盖字段资产、权限配置 UI 和目标编译/构建。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-29 节点表单与流程设计割裂问题需求修正

### 本轮动作

- 根据用户复盘，修正“节点表单策略”产品方向：不再把节点表单作为脱离流程图的独立配置列表，而是在应用设计内嵌流程设计工作台中，通过选中节点后的配置抽屉维护。
- 更新 `spec.md`：
  - 新增“流程设计工作台 -> 选中节点 -> 节点配置（审批设置 / 表单权限 / 高级设置）”主链路。
  - 明确低代码只配置业务表单资产、流程关联和变量映射，节点级审批策略和字段权限归流程节点配置体验。
  - 保持数据落点不变：业务绑定流程的节点表单策略继续保存到 `BusinessFlowBinding.nodeForms`，不重复写入 BPMN XML。
- 更新 `tasks.md`：
  - 标注 Task 6 的“所有权提示”只是过渡实现。
  - 新增 Task 10“应用设计内嵌流程节点配置工作台”，用于后续落地流程画布、节点配置抽屉和字段权限矩阵。
- 更新 `test-spec.md`：
  - 增加“应用设计内选中流程节点 -> 配置表单权限 -> 保存 -> 待办按权限生效”的体验验收链路。

### 验证命令与结果

- 本轮只调整需求文档和任务拆分，未改业务代码，未执行编译或前端构建。

### 跳过项

- 未启动后端、前端、MySQL、Redis 或 Flow 服务。
- 未做页面截图和流程节点配置点击验收；该项需在 Task 10 实现后执行。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-29 节点配置工作台基础形态实现

### 本轮动作

- `BusinessFlowBindingPanel.vue`：
  - 将“节点表单策略”重构为“流程节点配置工作台”。
  - 左侧展示流程节点轨道，节点来源仍为 `businessFlowVariables(...).userTasks`。
  - 右侧展示当前节点配置，拆分为 `审批设置 / 表单权限 / 高级设置` 三个页签。
  - `表单权限` 页签复用现有字段权限矩阵，并新增字段类型/只读标识列。
  - 保持 `visibleFields/writableFields/requiredFields` 到 `fieldPermissions` 的保存协议不变，后端和数据库结构不变。
- `BusinessFlowAppConfigPanel.vue`：
  - 外层流程步骤文案从“节点表单”改为“节点配置”。
- 本轮没有修改 Java、SQL 或接口协议。

### 验证命令与结果

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null
pnpm --dir forge-admin-ui exec eslint \
  src/views/app-center/components/designer/BusinessFlowBindingPanel.vue \
  src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue
```

- 首次结果：失败，原因是 `transition` CSS 需要按 Prettier 拆行。
- 修复后复跑结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

- 结果：通过，`✓ built in 2m 41s`。
- 警告：仍为既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释和 chunk 体积提示；本轮未新增阻断项。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
git diff --check -- \
  forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue \
  forge-admin-ui/src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
rg -n "[ \t]+$" \
  forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue \
  forge-admin-ui/src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue
```

- 结果：无匹配。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null
pnpm --dir forge-admin-ui dev --host 127.0.0.1
```

- 首次结果：失败，`EMFILE: too many open files, watch`。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
ulimit -n 65535
source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null
CHOKIDAR_USEPOLLING=true pnpm --dir forge-admin-ui exec vite --host 127.0.0.1 --port 5173
```

- 沙箱内首次结果：失败，`listen EPERM: operation not permitted 127.0.0.1:5173`。
- 提权后结果：通过，Vite 输出 `Local: http://127.0.0.1:5173/`。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
curl -I http://127.0.0.1:5173/
```

- 结果：通过，返回 `HTTP/1.1 200 OK`。

### 跳过项

- 未执行 Maven 编译；本轮只改前端 Vue/CSS。
- 未做采购单审批完整页面点击验收；原因是本轮未启动后端、MySQL、Redis 和 Flow 服务，业务配置页需要登录和接口数据。
- 未做 Playwright 截图；当前项目未安装 `playwright` 包，Node REPL 浏览器工具在本会话沙箱元数据下不可用。
- 尚未嵌入真实 BPMN/DingFlow 画布；本轮完成的是节点配置工作台基础形态。

### 服务清理

- 本轮启动的 Vite dev server 保留运行，供用户访问 `http://127.0.0.1:5173/` 验证页面。

## 2026-06-29 代码业务字段候选项与变量映射补充修复

### 本轮动作

- `BusinessFlowVariableResolver.java`：
  - `sample_purchase_order` 这类代码业务没有低代码 `configKey` 时，不再直接返回“业务对象缺少运行配置，无法读取字段候选项”。
  - 字段候选项改为从 `BusinessCodeFormProviderRegistry#listAssets(objectCode)` 兜底读取代码表单 Provider 暴露的 `fields/fieldCatalog`。
  - Provider 有字段目录时正常返回 `fieldCandidates`；只有找不到代码表单资产或资产未暴露字段目录时才返回 warning。
- `BusinessFlowBindingPanel.vue`：
  - 变量映射的“业务字段”选项合并 `props.fields` 和变量候选项接口返回的 `fieldCandidates`。
  - 兼容 `fieldCode / field / code` 三种字段编码，支持代码业务没有低代码表单字段 props 的场景。
- `FlowVariableMappingEditor.vue`：
  - 增加“业务字段 -> 流程变量”说明，明确只需要映射条件分支、审批人表达式和标题模板用到的字段。

### 验证命令与结果

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project/forge-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -DskipTests compile
```

- 结果：通过，`BUILD SUCCESS`。
- 警告：仍为既有 deprecated / unchecked 编译提示，不阻断本轮变更。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null
pnpm --dir forge-admin-ui exec eslint \
  src/views/app-center/components/designer/BusinessFlowBindingPanel.vue \
  src/views/app-center/components/designer/FlowVariableMappingEditor.vue
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

- 结果：通过，`✓ built in 2m 18s`。
- 警告：仍为既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释和 chunk 体积提示；本轮未新增阻断项。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
git diff --check -- \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowVariableResolver.java \
  forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue \
  forge-admin-ui/src/views/app-center/components/designer/FlowVariableMappingEditor.vue \
  forge-admin-ui/src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue \
  code-copilot/changes/unified-business-flow-app-config/spec.md \
  code-copilot/changes/unified-business-flow-app-config/tasks.md \
  code-copilot/changes/unified-business-flow-app-config/test-spec.md \
  code-copilot/changes/unified-business-flow-app-config/execution-log.md
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
rg -n "[ \t]+$" \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowVariableResolver.java \
  forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue \
  forge-admin-ui/src/views/app-center/components/designer/FlowVariableMappingEditor.vue \
  forge-admin-ui/src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue \
  code-copilot/changes/unified-business-flow-app-config/spec.md \
  code-copilot/changes/unified-business-flow-app-config/tasks.md \
  code-copilot/changes/unified-business-flow-app-config/test-spec.md \
  code-copilot/changes/unified-business-flow-app-config/execution-log.md
```

- 结果：无匹配。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
curl -I http://127.0.0.1:5173/
```

- 结果：通过，返回 `HTTP/1.1 200 OK`。

### 跳过项

- 未启动后端、MySQL、Redis 和 Flow 服务做页面登录联调；本轮后端以目标模块编译验证，前端以 ESLint/build 和 Vite 可访问性验证。
- 未实跑变量候选项接口；完整验证需要本地后端服务和登录态。

### 服务清理

- 本轮没有新启动常驻服务；沿用前一轮保留的 Vite dev server，继续供用户访问 `http://127.0.0.1:5173/`。

## 2026-06-29 流程设计器节点配置归位修正

### 本轮动作

- 根据用户纠正，废弃“应用配置内维护独立节点配置工作台”的方案。
- `BusinessFlowBindingPanel.vue`：
  - 移除应用侧节点配置面板，改为“流程节点配置”入口卡片。
  - 点击“打开流程设计器”跳转 `/flow/design`，携带 `id/businessObjectCode/codeApp/source=appCenter`。
  - 保留流程关联、业务记录绑定和变量映射；`nodeForms` 仅兼容旧配置，不再作为应用侧编辑入口。
- `flow/design.vue`、`DingFlowDesigner.vue`、`NodeConfigDrawer.vue`、`ApproverConfig.vue`：
  - 流程设计器带业务对象上下文时加载业务表单资产和字段目录。
  - 审批节点“表单权限”页签新增节点表单资产选择器。
  - 选择资产写入节点 `formKey/formName/formType`，字段权限写入 `formFieldPermissions`，由 BPMN writer 输出到流程节点。
- `BusinessFlowService.java`、`FlowClient.java`：
  - 待办运行时优先读取 Flow 服务任务节点 `formKey/formUrl/formFieldPermissions`。
  - 只有流程节点未配置时才回退 `BusinessFlowBinding.nodeForms`。
- `SamplePurchaseOrderFlowBpmn.java`：
  - 采购单样例人工节点补齐 `flowable:formKey="sample_purchase_order_approval_form"` 和节点 `flowable:formFieldPermissions`。
  - `V1.0.82/V1.0.84` 中的 `nodeForms` 文案修正为历史兼容兜底。
- `spec.md`、`tasks.md`、`test-spec.md`、`decisions.md`：
  - 统一产品边界：应用中心负责流程关联和变量映射，节点配置归流程设计器，运行时以 BPMN 节点配置为准。

### 验证命令与结果

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null
pnpm --dir forge-admin-ui exec eslint \
  src/views/app-center/components/designer/BusinessFlowBindingPanel.vue \
  src/views/flow/design.vue \
  src/components/flow-designer/DingFlowDesigner.vue \
  src/components/flow-designer/panel/NodeConfigDrawer.vue \
  src/components/flow-designer/panel/ApproverConfig.vue \
  src/components/flow-designer/panel/FormPermissionConfig.vue \
  src/components/bpmn/NodePropertiesPanel.vue
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project/forge-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" mvn -pl forge-business/forge-business-core -am -DskipTests compile
```

- 结果：通过，`BUILD SUCCESS`。
- 警告：仍为既有 deprecated / unchecked 编译提示，不阻断本轮变更。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

- 结果：通过，`✓ built in 2m 46s`。
- 警告：仍为既有 `UserSelectModal` 命名冲突、动态导入与静态导入混用、CSS `//` 注释和 chunk 体积提示；本轮未新增阻断项。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
git diff --check
```

- 结果：通过。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
rg -n "[ \t]+$" \
  forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue \
  forge-admin-ui/src/views/flow/design.vue \
  forge-admin-ui/src/components/flow-designer/DingFlowDesigner.vue \
  forge-admin-ui/src/components/flow-designer/panel/NodeConfigDrawer.vue \
  forge-admin-ui/src/components/flow-designer/panel/ApproverConfig.vue \
  forge-admin-ui/src/components/flow-designer/panel/FormPermissionConfig.vue \
  forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue \
  forge-server/forge-flow/forge-flow-client/src/main/java/com/mdframe/forge/flow/client/FlowClient.java \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java \
  forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/support/SamplePurchaseOrderFlowBpmn.java \
  forge-server/db/migration/V1.0.82__seed_sample_purchase_order_flow_binding.sql \
  forge-server/db/migration/V1.0.84__extend_sample_purchase_order_form_asset_fields.sql \
  code-copilot/changes/unified-business-flow-app-config/spec.md \
  code-copilot/changes/unified-business-flow-app-config/tasks.md \
  code-copilot/changes/unified-business-flow-app-config/test-spec.md \
  code-copilot/memory/decisions.md
```

- 结果：无匹配。

```bash
cd /Users/yaomindong/Desktop/project/mdframe/forge-project
curl -I http://127.0.0.1:5173/
```

- 结果：通过，返回 `HTTP/1.1 200 OK`。

### 跳过项

- 未启动后端、MySQL、Redis 和 Flow 服务做浏览器登录联调；本轮已完成目标编译、前端 lint/build 和静态检查。
- 未实跑“保存流程 -> 部署 -> 待办读取节点权限”的完整链路；需要本地运行环境和登录态。

### 服务清理

- 本轮没有新启动常驻服务；沿用前一轮保留的 Vite dev server，继续供用户访问 `http://127.0.0.1:5173/`。
