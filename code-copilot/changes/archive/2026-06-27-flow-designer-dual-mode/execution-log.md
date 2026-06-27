# 执行日志 — 流程模型双设计器模式改造

## 2026-06-21

### SDD 基线
- 触发原因：用户要求按照 SDD 模型改造，并参考提交 `9a2049b5` 恢复原 BPMN 设计器。
- 发现：
  - `9a2049b5` 是引入钉钉式 `flow-designer` 并删除旧 `components/bpmn` 的提交。
  - 旧 BPMN.js 设计器应从 `9a2049b5^` 恢复。
  - 旧依赖包括 `bpmn-js`、`bpmn-js-properties-panel`、`dagre`、`diagram-js`、`inherits-browser`、`tiny-svg`。
- 当前状态：已完成实现与增量验证。

### 实现记录
- 恢复 `forge-admin-ui/src/components/bpmn` 目录，并补回 BPMN.js 相关依赖。
- 新增 `sys_flow_model.designer_type` Flyway 迁移，后端 `FlowModel` 增加 `designerType`，创建/更新默认归一为 `approval`，复制模型继承原类型，导入 BPMN 默认 `business`。
- 流程模型列表/新建弹窗增加“审批流程 / 业务流程”选择和展示。
- 设计页根据 `designerType` 渲染：
  - `approval`：现有 `DingFlowDesigner`。
  - `business`：恢复的 `FlowModeler`，选中 BPMN 元素后右侧停靠 `NodePropertiesPanel`。
- 业务流程下隐藏审批专属的全局“审批设置”入口，避免和通用 BPMN 流程混淆。

### 增量验证
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint --fix src/views/flow/model.vue src/views/flow/design.vue src/components/bpmn/*.vue src/components/bpmn/*.js vite.config.js`
  - 结果：最终通过。
  - 过程：前两次因命令执行目录与 shell 通配路径不一致未实际匹配文件；修正到 `forge-admin-ui` 目录后发现历史 BPMN 文件 lint 阻断，已修复后通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
  - 结果：通过，Vite built in 49.38s。
  - 警告：`UserSelectModal` 组件名冲突自动导入警告、既有 CSS `//` 注释 minify 警告、`src/store/index.js` 动态/静态导入混用提示、chunk size 提示；均非本轮阻断。
- `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests compile`
  - 结果：最终通过，`forge-plugin-flow` 与依赖 Reactor 模块均 `BUILD SUCCESS`。
  - 过程：追加“更新缺省 designerType 时继承已有类型”兼容逻辑后，第一次复跑因补丁误放到创建逻辑导致 `existing` 未定义而失败；修正到更新逻辑后复跑通过。
- `git diff --check`
  - 结果：通过。

### 跳过项
- 未执行真实数据库迁移：本轮提供 Flyway 脚本，实际由环境启动时执行。
- 未执行浏览器登录态交互验证：本轮未启动本地后端和数据库；已用前端构建覆盖恢复组件和依赖解析。
- 本轮未启动需清理的服务进程。

### 业务流程画布回归修复
- 触发原因：用户反馈 BPMN 业务流程设计器节点过大、布局观感异常，并且新建流程后会显示上一个流程图。
- 修复内容：
  - `FlowModeler` 导入空 XML 或缺少 `BPMNDiagram` 的 XML 时强制使用默认 BPMN 模板，避免沿用旧画布。
  - `FlowModeler` 初始化、外部 `setXML('')`、属性 `xml` 变化、适应屏幕和自动布局后的画布缩放统一限制为最大 100%，避免单节点被 `fit-viewport` 放大。
  - `AutoLayout` 改为按相对位移调用 `modeling.moveShape`，保持 BPMN 节点按 dagre 左到右布局结果移动。
  - `design.vue` 在设计器类型/模型 ID 变化时增加渲染 key，嵌入模式和路由 query 切换时即使 XML 为空也会重新导入；切到新流程时清空模型、表单、字段目录和右侧属性面板状态。
- 增量验证：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint --fix src/views/flow/design.vue src/components/bpmn/FlowModeler.vue src/components/bpmn/AutoLayout.js`
    - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
    - 结果：通过，Vite built in 53.74s。
    - 警告：`UserSelectModal` 组件名冲突自动导入警告、既有 CSS `//` 注释 minify 警告、`src/store/index.js` 动态/静态导入混用提示、chunk size 提示；均非本轮阻断。
  - `git diff --check`
    - 结果：通过。
- 跳过项：未启动本地后端、数据库和浏览器登录态交互验证；本轮问题已通过目标 ESLint、生产构建和空白检查覆盖编译及依赖解析风险。
- 本轮未启动需清理的服务进程。

### 属性面板稳定性与统一外壳
- 触发原因：用户反馈 BPMN 方式点击节点后右侧面板抖动，并询问审批流程和业务流程的右侧属性面板是否可以通用。
- 修复内容：
  - 新增 `FlowPropertyPanelShell` 通用属性面板外壳，统一标题、图标、关闭按钮、滚动区、底部操作区和空状态。
  - 审批流程 `NodeConfigDrawer` 改为复用通用外壳，内部仍保留审批 JSON 节点配置组件。
  - BPMN 业务流程右侧停靠面板改为复用通用外壳，移除 `v-if + Transition` 反复挂载动画，改成稳定停靠显示。
  - BPMN 选中清空增加 80ms 延迟，避免 bpmn-js 切换节点时短暂空选中导致面板闪烁。
  - `FlowModeler` 暴露 `clearSelection()`，关闭 BPMN 属性面板时同步清理画布选中态，避免关闭后点击同一节点无法重新打开。
- 设计说明：
  - 两种模式可共用属性面板外壳；内部配置表单不完全共用，因为审批流程编辑的是 Forge 审批 JSON 节点，业务流程编辑的是 BPMN element / moddle 属性。
- 增量验证：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint --fix src/views/flow/design.vue src/components/bpmn/FlowModeler.vue src/components/flow-designer/panel/NodeConfigDrawer.vue src/components/flow/FlowPropertyPanelShell.vue`
    - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
    - 结果：通过，Vite built in 55.31s。
    - 警告：`UserSelectModal` 组件名冲突自动导入警告、既有 CSS `//` 注释 minify 警告、`src/store/index.js` 动态/静态导入混用提示、chunk size 提示；均非本轮阻断。
  - `git diff --check`
    - 结果：通过。
- 跳过项：未启动本地后端、数据库和浏览器登录态交互验证；本轮前端交互修复已通过目标 ESLint、生产构建和空白检查。
- 本轮未启动需清理的服务进程。

### 属性面板样式与布局统一
- 触发原因：用户要求两种设计模式的属性面板在样式和布局上保持一致，但不修改参数映射。
- 修复内容：
  - `FlowPropertyPanelShell` 增加统一内容容器，并统一 Tab 导航、Tab 内容 padding、表单项间距、label 字重、输入框高度、底部按钮区和空状态样式。
  - `NodeConfigDrawer` 移除旧的本地 Tab/表单样式，避免审批面板覆盖通用外壳样式。
  - BPMN 业务流程属性面板宽度调整为 520px，与审批流程属性抽屉默认宽度一致。
  - BPMN `NodePropertiesPanel` 取消内部滚动和 8px 小 padding，改为复用外壳滚动区，并对 Tab 工具条、Tab 页内容、表单项和输入框密度做统一。
- 边界说明：
  - 本轮只调整样式与布局，不修改审批 JSON 配置、BPMN element/moddle 属性写入、参数映射、`emit` 事件或转换逻辑。
- 增量验证：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm exec eslint --fix src/views/flow/design.vue src/components/bpmn/NodePropertiesPanel.vue src/components/flow-designer/panel/NodeConfigDrawer.vue src/components/flow/FlowPropertyPanelShell.vue`
    - 结果：通过。
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
    - 结果：通过，Vite built in 1m 4s。
    - 警告：`UserSelectModal` 组件名冲突自动导入警告、既有 CSS `//` 注释 minify 警告、`src/store/index.js` 动态/静态导入混用提示、chunk size 提示；均非本轮阻断。
  - `git diff --check`
    - 结果：通过。
- 跳过项：未启动本地后端、数据库和浏览器登录态交互验证；本轮为前端样式布局调整，已通过目标 ESLint、生产构建和空白检查。
- 本轮未启动需清理的服务进程。
