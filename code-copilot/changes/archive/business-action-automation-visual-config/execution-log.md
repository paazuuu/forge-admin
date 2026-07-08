# Execution Log

## 2026-07-04 初始记录

- 创建 SDD 变更：`business-action-automation-visual-config`。
- 范围：对象设计器自动化动作配置前端体验。
- 尚未执行验证，待实现完成后追加。

## 2026-07-04 实现与验证

- 变更范围：
  - `BusinessActionDesigner.vue` 改为业务自动化配置界面。
  - `BusinessObjectDesignerShell.vue` 左侧入口改为“自动化动作”。
  - `object-designer.[objectCode].vue` 传入 `documentConfig` 用于识别流程回调场景。
- 执行命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint 'src/views/app-center/object-designer.[objectCode].vue' src/views/app-center/components/designer/BusinessObjectDesignerShell.vue src/views/app-center/components/designer/BusinessActionDesigner.vue`
  - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
  - 结果：通过，Vite 输出既有 chunk/dynamic import 和 CSS `//` 注释警告，未阻断。
  - `git diff --check -- code-copilot/changes/business-action-automation-visual-config forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue'`
  - 结果：通过。
- 未启动本地服务，无需清理 PID。

## 2026-07-04 业务标签体验修正

- 变更范围：
  - 普通配置界面隐藏 `item`、`index` 等执行器别名。
  - 明细集合下拉只显示业务关系名称，不显示 `record.children.*`。
  - 字段下拉只显示“单据字段 · 字段名”“明细字段 · 字段名”，不显示 `${...}` 或 `item.xxx` 路径。
  - 参数文案从“账户 / 仓库、物品 / 资源、明细ID”调整为通用数量域文案：“数量账户、数量对象、来源明细、备用对象字段”。
- 执行命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint src/views/app-center/components/designer/BusinessActionDesigner.vue`
  - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
  - 结果：通过，仍存在项目既有 Vite chunk/dynamic import 和 CSS `//` 注释警告，未阻断。
  - `git diff --check -- code-copilot/changes/business-action-automation-visual-config forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue`
  - 结果：通过。

## 2026-07-04 历史路径值显示修正

- 变更范围：
  - `BusinessActionDesigner.vue` 字段候选补充读取对象关系和 `childrenConfig`，避免子表字段元数据缺失。
  - 对已保存的历史路径值增加显示翻译兜底，`item.materialId`、`item.materialCode` 这类保存值不再作为普通界面展示文案。
  - `object-designer.[objectCode].vue` 向自动化动作面板传入 `draft.relations`。
- 执行命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint 'src/views/app-center/object-designer.[objectCode].vue' src/views/app-center/components/designer/BusinessActionDesigner.vue`
  - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
  - 结果：通过，仍存在项目既有 Vite chunk/dynamic import 和 CSS `//` 注释警告，未阻断。

## 2026-07-04 关系配置整合修正

- 变更范围：
  - `BusinessActionDesigner.vue` 将“处理明细”改为引用“关系与级联”中已有明细关系，自动反查关系名称、目标对象和字段元数据。
  - 自动化动作面板通过 `suiteCode` 加载目标对象设计字段，关系配置成为明细字段和选择器回填的唯一来源。
  - 普通字段下拉取消自由输入，避免用户看到或输入 `item.xxx`、`record.children.*` 这类内部表达式；未识别字段显示中文维护提示。
  - `object-designer.[objectCode].vue` 向自动化动作面板传入 `suiteCode`。
  - SDD 规格补充“关系与级联为唯一来源，自动化动作只引用关系”的产品边界。
- 执行命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint src/views/app-center/components/designer/BusinessActionDesigner.vue 'src/views/app-center/object-designer.[objectCode].vue'`
  - 结果：通过；补充提示样式后复跑仍通过。
  - `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' code-copilot/changes/business-action-automation-visual-config/spec.md code-copilot/changes/business-action-automation-visual-config/tasks.md`
  - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
  - 结果：通过；补充提示样式后复跑仍通过，仍存在项目既有 Vite chunk/dynamic import 和 CSS `//` 注释警告，未阻断。

## 2026-07-04 入口归属修正

- 变更范围：
  - 普通对象设计器左侧不再展示“自动化动作”独立一级入口，避免被理解为关系之外的第二套配置。
  - 单据闭环配置增加“审批后处理（可选）”入口，点击后进入原业务处理配置能力。
  - 代码应用白名单场景保留“业务处理”入口，用于兼容现有代码应用设计。
  - SDD 规格与任务同步更新入口归属。
- 执行命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint src/views/app-center/components/designer/BusinessObjectDesignerShell.vue src/views/app-center/components/designer/BusinessActionDesigner.vue 'src/views/app-center/object-designer.[objectCode].vue'`
  - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
  - 结果：通过，仍存在项目既有 Vite chunk/dynamic import 和 CSS `//` 注释警告，未阻断。

## 2026-07-04 发布检查动作误报修正

- 变更范围：
  - `BusinessObjectPublishService` 发布运行态页面时不再把带步骤的 `COMMAND` 业务处理动作注入为列表自定义按钮。
  - 发布检查自定义操作时区分站内跳转、外链、接口调用、刷新、流程启动、触发器和业务处理动作；`COMMAND` 不再要求目标地址，也不再按按钮参数映射校验 `params.name`。
  - 对象设计器在“业务流程配置”面板也显示单据闭环步骤，方便直接进入“审批后处理（可选）”。
  - 保留入口权限码提示为普通 WARN，当前未改为阻断项。
- 执行命令：
  - `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 结果：失败；当前 shell 默认 Java 8，报 `无效的目标发行版: 17`。
  - `export JAVA_HOME=$(/usr/libexec/java_home -v 17) && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint 'src/views/app-center/object-designer.[objectCode].vue' src/views/app-center/components/designer/BusinessObjectDesignerShell.vue src/views/app-center/components/designer/BusinessActionDesigner.vue`
  - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
  - 结果：通过，仍存在项目既有 Vite chunk/dynamic import 和 CSS `//` 注释警告，未阻断。

## 2026-07-04 关系与级联入口整合

- 变更范围：
  - `BusinessRelationDesigner.vue` 按原型方向改为左侧步骤轨道 + 右侧业务配置卡片，替代原 tab + 折叠表单堆叠。
  - 审批后数量同步整合进当前关系配置，不再从“单据闭环配置”进入单独动作页。
  - 数量处理字段改为通用业务文案：归属字段（主表）、对象字段（明细）、备用对象字段（明细）、数量字段（明细），字段选项仍来自对象设计，不写死采购、物料、仓库。
  - 父设计器把 `relations` 面板纳入脏数据状态，顶部保存继续调用关系页自己的保存方法。
  - SDD 规格和任务同步为“关系与级联是唯一入口”的最终产品口径。
- 执行命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint src/views/app-center/components/designer/BusinessRelationDesigner.vue`
  - 结果：首次发现空 `span` 与 CSS 换行格式问题，修复后通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint src/views/app-center/components/designer/BusinessRelationDesigner.vue 'src/views/app-center/object-designer.[objectCode].vue' src/views/app-center/components/designer/BusinessObjectDesignerShell.vue src/views/app-center/components/designer/BusinessActionDesigner.vue`
  - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
  - 结果：通过，仍存在项目既有 Vite dynamic import 和 CSS `//` 注释警告，未阻断。
  - `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectPublishService.java code-copilot/changes/business-action-automation-visual-config`
  - 结果：通过。
- 追加检查：
  - 兼容动作页残留的“库存账户/物品资源”文案改为“归属字段/对象字段”。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint src/views/app-center/components/designer/BusinessActionDesigner.vue src/views/app-center/components/designer/BusinessRelationDesigner.vue 'src/views/app-center/object-designer.[objectCode].vue'`
  - 结果：通过。
  - `rg -n "库存账户|物品资源|数量对象|账户 / 仓库|物品 / 资源|审批后处理（可选）" ...`
  - 结果：无匹配。

## 2026-07-05 查询与操作入口体验修正

- 变更范围：
  - `AiCrudPage.vue` 搜索区使用搜索专用 schema，去掉 `showCount/showWordLimit`，避免查询条件输入框显示 `0/200`。
  - `BusinessListDesigner.vue` 增加显眼的“自定义操作”入口条，直接显示操作数量和预览，并提供“新增顶部按钮 / 新增行操作 / 配置全部操作”。
  - `ListPageGridDesigner.vue` 暴露 `openCustomActionManager` 和 `createAndEditCustomAction`，供列表设计器顶部入口直接打开操作配置弹窗。
  - `BusinessRelationDesigner.vue` 将关系类型收敛为“包含多个明细”，旧关系类型在设计器内按 `DETAIL` 处理。
- 执行命令：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint src/components/ai-form/AiCrudPage.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/BusinessRelationDesigner.vue`
  - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
  - 结果：通过，仍存在项目既有 Vite dynamic import 和 CSS `//` 注释警告，未阻断。
  - `git diff --check -- forge-admin-ui/src/components/ai-form/AiCrudPage.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue code-copilot/changes/business-action-automation-visual-config`
  - 结果：通过。
  - `rg -n "属于目标对象|拥有多条目标记录|多对多关联|库存账户|物品资源|show-count=\"field.showCount\"" ...`
  - 结果：无匹配。

## 2026-07-05 业务流程配置精简

- 变更范围：
  - `BusinessFlowAppConfigPanel.vue` 将普通对象流程配置阶段精简为“基础配置 / 状态字典 / 主流程”，去掉“编号生成”阶段，顶部配置项调整为“单据状态 / 流程绑定”。
  - `BusinessDocumentPanel.vue` 删除编号生成配置区，单据基础配置发起人固定展示为“创建人 createBy”，不再提供发起人字段选择和负责人字段。
  - `DocumentConfigSummary.vue` 发布摘要和保存检查去掉编号规则检查，摘要展示发起人为创建人。
  - `BusinessFlowBindingPanel.vue` 业务记录绑定去掉负责人字段，不再自动推断负责人。
  - `ListPageGridDesigner.vue` 删除最后一个自定义操作时保留配置弹窗空状态和添加入口，避免下方配置区看起来消失。
- 执行命令：
  - `rg -n "number-rule|编号生成|负责人字段|inferOwnerField|noRulePreview|selectedDocument|codeRuleOptions|documentNoFieldOptions|previewDocumentCodeRule|effectiveSuiteCode|effectiveObjectCode" ...`
  - 结果：无匹配。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue src/views/app-center/components/designer/BusinessDocumentPanel.vue src/views/app-center/components/designer/DocumentConfigSummary.vue src/views/app-center/components/designer/BusinessFlowBindingPanel.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`
  - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
  - 结果：通过，仍存在项目既有 Vite dynamic import 和 CSS `//` 注释警告，未阻断。
  - `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessDocumentPanel.vue forge-admin-ui/src/views/app-center/components/designer/DocumentConfigSummary.vue forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue code-copilot/changes/business-action-automation-visual-config`
  - 结果：通过。
