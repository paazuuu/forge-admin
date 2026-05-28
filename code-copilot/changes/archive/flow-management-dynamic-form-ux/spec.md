# 流程管理动态表单与体验优化 Spec

## 背景

当前流程管理已有流程模型、BPMN 设计器、流程表单和待办审批能力，但动态表单链路不完整：

- 流程模型可配置 `formJson/formKey/formUrl`，待办页主要使用外置表单或普通审批意见表单，动态表单未在办理区形成可用闭环。
- 用户任务节点属性面板只能维护 `formKey/formJson` 文本，业务用户无法在节点内在线设计专属动态表单。
- 流程表单和流程模型设计页仍使用旧版自研表单设计器，无法复用已接入的 `@form-create/designer` 能力。
- 流程模型分页未按创建人隔离，普通用户可看到其他用户创建的模型。
- 流程图查看和设计器视觉偏“AI 风”，大量渐变/发光/自定义 SVG 渲染不够正式，且流程图详情接口默认生成 Base64 图片导致加载慢。
- 节点属性面板 Tab 横向内容可发现性差，用户容易误以为后面没有更多配置。

## 目标

1. 流程模型管理分页只查询当前登录用户创建的模型。
2. 流程设计和表单管理统一使用 form-create 设计器保存动态表单 JSON。
3. 待办办理区支持节点动态表单渲染和提交，表单数据作为流程变量随审批动作提交。
4. 用户任务节点属性面板支持在线设计节点专属动态表单，并保存为 BPMN 节点 `flowable:formJson`。
5. 流程图查看接口默认不再生成 Base64 图片，前端使用 BPMN XML 渲染，减少加载耗时。
6. BPMN 设计器和流程图查看样式调整为正式企业后台风格，去掉紫色渐变、过度阴影和动效。
7. 节点属性面板 Tab 增强横向滚动提示、左右切换和当前页签提示。

## 非目标

- 不新增业务模型字段绑定表结构。
- 不改 Flowable 流程实例、任务和审批核心流转语义。
- 不迁移历史动态表单数据，只做旧 schema 到 form-create rule 的前端兼容转换。

## 方案

### 后端

- `FlowModelService.pageFlowModel` 获取当前用户名并传入 Mapper。
- `FlowModelMapper.selectModelPage` 增加 `createBy` 条件。
- `FlowTaskController.getProcessDiagramInfo` 增加 `includeImage` 参数，默认 `false`。
- `FlowTaskService.getProcessDiagramInfo` 保留兼容方法，新增带 `includeImage` 的实现，只有显式要求时才生成 Base64 图片。

### 前端

- 新增 form-create 公共适配工具，统一安装 `ElementPlus`、`@form-create/element-ui`，并提供旧 schema 与 form-create rule 互转兼容。
- 新增 form-create 设计器封装，用于流程模型设计页和流程表单管理页。
- 新增 form-create 运行时渲染组件，用于表单预览和待办动态表单办理。
- `flow/design.vue` 和 `flow/form.vue` 替换旧 `FormDesigner/FormPreview`。
- `NodePropertiesPanel.vue` 在用户任务动态表单配置中提供引用已有表单、在线设计、预览和高级 JSON 折叠配置；在线设计结果写入当前 BPMN 节点 `flowable:formJson`。
- `flow/todo.vue` 对 `taskFormInfo.formType === 'dynamic'` 时显示动态表单，并在审批动作提交时合并动态表单变量。
- `ProcessDiagramViewer.vue` 增加 `compact` 和 `includeImage` 参数，默认走 BPMN XML 渲染并使用正式样式。
- `FlowModeler.vue` 移除高成本自定义 SVG 渲染和动态点阵背景，改用 CSS 静态网格和默认 BPMN 图形。
- `Minimap.vue` 默认折叠，展开后再生成 SVG 缩略图。
- `NodePropertiesPanel.vue` 增加 Tab 左右滚动按钮、末尾渐隐提示和当前 Tab 位置提示。

## 验收标准

- 流程模型分页接口只返回 `create_by = 当前用户名` 的模型。
- 流程设计页可打开 form-create 设计器、保存到 `sys_flow_model.form_json`，重新进入可回显。
- 用户任务节点属性面板可打开 form-create 设计器，保存后当前节点包含 `flowable:formJson`，待办办理时优先渲染节点专属表单。
- 表单管理页可用 form-create 设计器编辑 `sys_flow_form.form_schema`，可预览。
- 待办页遇到动态表单节点时显示动态表单，审批通过时校验表单并提交变量。
- 打开流程图不再默认请求/返回 `diagramBase64`，加载明显变快。
- BPMN 设计器视觉正式、简洁，无紫色渐变/发光边框。
- 属性面板 Tab 后续选项可被明显发现并可一键左右滚动。
