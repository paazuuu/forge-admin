# 流程管理动态表单与体验优化 Tasks

- [x] 后端流程模型分页按当前创建人过滤。
- [x] 后端流程图详情接口增加 `includeImage` 开关，默认跳过 Base64 图片生成。
- [x] 新增 form-create 公共适配工具、设计器组件、运行时渲染组件。
- [x] 替换流程模型设计页动态表单设计器与预览能力。
- [x] 替换流程表单管理页动态表单设计器与预览能力。
- [x] 为用户任务节点属性面板补充节点专属动态表单在线设计与预览。
- [x] 放开 form-create 设计器字段 ID 编辑，用于流程变量和业务模型字段映射。
- [x] 待办审批区整合动态表单渲染、校验和变量提交。
- [x] 优化流程图查看组件的正式视觉风格和加载参数。
- [x] 优化 BPMN 设计器正式视觉风格，移除高成本自定义渲染模块。
- [x] 优化小地图默认折叠和按需生成。
- [x] 优化节点属性面板 Tab 可发现性和横向滚动体验。
- [x] 运行前端 lint/build 与后端编译验证。

## 验证记录

- `pnpm exec eslint src/api/flow.js src/components/form-create/formCreateBridge.js src/components/form-create/FlowFormCreateDesigner.vue src/components/form-create/FlowFormCreateRenderer.vue src/components/bpmn/FlowModeler.vue src/components/bpmn/Minimap.vue src/components/bpmn/NodePropertiesPanel.vue src/components/bpmn/ProcessDiagramViewer.vue src/components/bpmn/InteractiveProcessDiagram.vue src/views/flow/design.vue src/views/flow/form.vue src/views/flow/todo.vue` 通过。
- `pnpm exec eslint src/components/bpmn/NodePropertiesPanel.vue src/components/form-create/FlowFormCreateDesigner.vue src/components/form-create/FlowFormCreateRenderer.vue` 通过，用于验证节点级在线设计入口。
- `pnpm exec eslint src/components/form-create/FlowFormCreateDesigner.vue src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue` 通过，用于验证字段 ID 可编辑配置。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` 通过。默认堆内存直接构建曾在 chunk 阶段 OOM；构建仍有既有 UnoCSS 图标缺失与 chunk size 警告。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -pl forge-flow/forge-flow-server -am compile -DskipTests` 通过。默认 Maven 使用 JDK 8，会因 `target 17` 失败。
- `pnpm lint:fix` 全量执行失败，原因是仓库既有全局 lint 问题（Markdown 示例被 lint、旧组件未使用变量、console、重复 key 等）；本次触达文件已单独通过 ESLint。
