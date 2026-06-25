# 测试计划：forge-list-designer-productivity-upgrades

## 本轮增量验证（2026-06-19）

### 范围

- 左树右表运行态：左侧 `tree-panel` 独立加载树接口，右侧 `AiCrudPage` 保持普通分页接口。
- 左树右表设计态：筛选树属性面板简化为必要字段，去掉容易误导的手动联动事件入口。
- 列表预览弹窗：继续验证内容容器 flex 高度和滚动约束不破坏构建。

### P0 命令

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/views/ai/crud-page.vue`
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/views/ai/crud-page.vue`
- `pnpm --dir forge-admin-ui build`

### 跳过项

- 未启动 Vite 和浏览器截图验证：本轮没有启动前后端服务，先以静态检查和生产构建覆盖；运行时请求链路需在本地服务可用时用真实发布页面复验。

## 本轮增量验证（2026-06-25 14:41:25 CST）

### 范围

- 共享页面组件：富文本编辑回写、穿梭框静态/远程数据源、水印渲染、Vue runtime-only 安全 Props 模板预览。
- 列表设计器：共享组件更新事件写回 `listGridLayout`，穿梭框远程接口结构化配置。
- 表单设计器：共享组件接入左侧组件库、画布渲染、属性配置；穿梭框按字段组件绑定业务字段，水印等展示组件保持虚拟组件不进入字段注册。
- 运行态表单项：`AiFormItem` 支持远程选项 `paramsText`，穿梭框动态接口参数配置可生效。

### P0 命令

- `pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiFormItem.vue src/components/lowcode-builder/shared/page-widget-schema.js src/components/lowcode-builder/shared/PageWidgetRenderer.vue src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js src/views/app-center/components/designer/form-first/formDesignerSchema.js`
- `git diff --check -- forge-admin-ui/src/components/ai-form/AiFormItem.vue forge-admin-ui/src/components/lowcode-builder/shared/page-widget-schema.js forge-admin-ui/src/components/lowcode-builder/shared/PageWidgetRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`

### 跳过项

- 未启动 Vite 和浏览器截图验证：本轮没有启动前后端服务，先以静态检查和生产构建覆盖；远程接口选项加载需在后端可用时用真实接口复验。

## 本轮增量验证（2026-06-25 外部组件接入修正）

### 范围

- 富文本渲染器改为 WangEditor Vue3 组件，继续回写 `props.content`。
- Markdown 渲染器改为 `@kangc/v-md-editor`，源码/预览模式由组件自身承接，并验证 vuepress 主题显式注入 Prism 后不再触发 `languages` undefined。
- 水印使用 `n-watermark` 真实 API，列表和表单属性面板均补齐主要配置项。
- 二维码/条形码使用外部生成组件；`vue3-barcode-qrcode` 当前 registry 404，使用 `qrcode-vue3` / `vue3-barcode` 替代，并覆盖列表/表单共享预览渲染。
- 表单设计器将二维码、条形码、水印、富文本、Markdown 等页面展示组件作为虚拟组件处理，避免参与业务字段注册或 DDL。
- `AiFormItem` 运行态识别虚拟页面组件，表单预览弹窗和应用页应直接复用共享 `PageWidgetRenderer`，不再把页面组件 props 透传给 `NInput`。
- 列表预览弹窗在富文本/Markdown 组件存在时宽度应固定在视口内，横向溢出只发生在内部滚动容器，不允许撑大弹窗。
- 列表左侧页面组件库中二维码、条形码、Markdown、富文本等共享组件应只出现一次，统一来自 `pageWidgetCatalog`。
- 富文本和 Markdown 在列表预览弹窗中应可编辑；是否禁用由组件自身 `readonly` 配置决定。
- 新增 Naive UI 页面组件：日历、代码、倒计时、描述、公示、列表、日志、数值动画、面包屑、菜单、分页、面板分隔，需覆盖列表/表单组件库、画布渲染、预览弹窗和运行态表单虚拟组件分流。

### P0 命令

- `pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiFormItem.vue src/components/lowcode-builder/shared/PageWidgetRenderer.vue src/components/lowcode-builder/shared/page-widget-schema.js src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/page-schema.js src/views/app-center/components/designer/BusinessListDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/form-first/formDesignerSchema.js src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue`
- `git diff --check -- forge-admin-ui/package.json forge-admin-ui/pnpm-lock.yaml forge-admin-ui/src/components/ai-form/AiFormItem.vue forge-admin-ui/src/components/lowcode-builder/shared/PageWidgetRenderer.vue forge-admin-ui/src/components/lowcode-builder/shared/page-widget-schema.js forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`

### 跳过项

- 未启动 Vite 和浏览器截图验证：本轮没有启动前后端服务，先以静态检查和生产构建覆盖；外部组件交互需在浏览器中补充手工验收。

## 本轮增量验证（2026-06-25 组件面板体验补充）

### 范围

- 表单设计器左侧组件库图标：字段组件、布局组件和共享页面组件按语义使用不同图标，减少二维码、条形码、富文本、Markdown、日历、菜单等组件的重复图标。
- 列表设计器左侧页面组件面板：面板头部展示组件总数、搜索匹配数和分组数；每个分组标题展示当前分组组件数量。

### P0 命令

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`

### 跳过项

- 未重新执行生产构建：本轮只调整组件面板展示图标、统计文案和样式，不涉及运行态渲染或依赖变更。
- 未启动 Vite 和浏览器截图验证：本轮没有启动前后端服务，需在本地页面中手工确认图标和统计展示效果。

## 本轮增量验证（2026-06-25 右侧配置面板样式统一）

### 范围

- 列表设计器右侧配置面板：头部、搜索、Tab 顺序、折叠面板、表单项和输入控件样式。
- 表单设计器右侧配置面板：收起按钮、头部按钮视觉和列表设计器保持一致。
- 后续能力记录：Naive UI 新增组件完整 Props/Slots/Events 配置和动态数据源能力不在本轮实现，作为后续结构化能力补齐。

### P0 命令

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`

### 跳过项

- 未重新执行生产构建：本轮只调整右侧配置面板展示结构和 CSS，不涉及运行态渲染、依赖或构建配置。
- 未启动 Vite 和浏览器截图验证：本轮没有启动前后端服务，需在本地页面中手工确认两边面板视觉一致性。

## 本轮增量验证（2026-06-25 左侧组件面板样式统一）

### 范围

- 列表设计器左侧页面组件面板：组件卡片补充图标，图标、标题、描述、禁用标签和分组统计样式统一。
- 表单设计器左侧字段与布局面板：补充统计胶囊，搜索框、组件库/字段资产 Tab、分组标题、组件卡片和字段列表密度统一。

### P0 命令

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue`
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`

### 跳过项

- 未重新执行生产构建：本轮只调整左侧组件面板模板和 CSS，不涉及运行态渲染、依赖或构建配置。
- 未启动 Vite 和浏览器截图验证：本轮没有启动前后端服务，需在本地页面中手工确认两边左侧面板视觉一致性。

## 本轮增量验证（2026-06-25 新增组件可配置能力补齐）

### 范围

- 列表设计器左侧页面组件图标：新增 Naive UI 组件和共享页面组件按组件语义映射图标，减少重复兜底图标。
- 共享页面组件数据来源：`dataBinding` 默认 schema、设计器属性配置、运行态上下文传递、远程接口加载、响应路径和字段映射。
- 新增 Naive UI 组件渲染：描述、列表、日志、代码、数值动画、面包屑、菜单、分页、公示、日历和倒计时优先使用绑定数据，再回退静态配置。
- 详情信息区块：支持当前详情数据、当前详情字段路径和远程详情接口三类来源。

### P0 命令

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/shared/page-widget-schema.js src/components/lowcode-builder/shared/PageWidgetRenderer.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/ai-form/AiFormItem.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/shared/page-widget-schema.js forge-admin-ui/src/components/lowcode-builder/shared/PageWidgetRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/ai-form/AiFormItem.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`

### 跳过项

- 未启动 Vite 和浏览器截图验证：本轮没有启动前后端服务，远程接口取数和上下文取数需在本地设计器页面中补充手工验收。
- 未执行后端编译：本轮只改前端 Vue 设计器、共享渲染器和 schema。

## 本轮增量验证（2026-06-25 数据来源配置可读性修正）

### 范围

- 列表设计器右侧“数据来源”配置：从三列/四列压缩输入改为说明 + 来源类型 + 条件展示字段，避免窄面板展示不全。
- 表单设计器右侧“数据来源”配置：同步列表设计器表达方式，补充“静态配置 / 当前表单或详情数据 / 远程接口”的用途说明。
- 字段映射改为中文标签单列展示，降低普通用户理解成本。

### P0 命令

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`

### 跳过项

- 未启动 Vite 和浏览器截图验证：本轮先用静态检查和生产构建覆盖，具体展示宽度需在设计器页面中手工确认。

## 本轮增量验证（2026-06-25 运行规则与字段展示统一封装）

### 范围

- 共享运行规则：组件、字段、区块统一支持显示隐藏、只读、禁用、必填、文字颜色、背景色、样式类和内联样式。
- 表单运行态：`AiForm`、`AiFormLayoutNodes`、`AiFormItem` 按当前表单数据和上下文过滤字段/模块，并把只读、禁用和必填状态传给真实控件。
- 列表/详情运行态：`GridBlockRenderer` 和发布运行页 `crud-page.vue` 统一使用 `FieldValueRenderer` 展示字典标签、状态 Tag、链接、金额和颜色规则。
- 设计器属性：列表设计器和表单设计器右侧面板共用 `RuntimeRulesEditor`，新增规则不再分两套实现。
- 数据来源入口：运行规则支持当前记录/详情、当前行数据、当前表单数据、URL 查询参数、URL 路由参数和当前用户。
- 行操作弹窗：`AiCrudPage` 打开编辑/详情弹窗时，当前行通过 `currentRow -> row` 进入表单规则上下文。
- 数据来源表单可读性：请求方式和响应路径各占一行。

### P0 命令

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/shared/runtime-rules.js src/components/lowcode-builder/shared/FieldValueRenderer.vue src/components/lowcode-builder/shared/RuntimeRulesEditor.vue src/components/ai-form/AiForm.vue src/components/ai-form/AiFormItem.vue src/components/ai-form/AiFormLayoutNodes.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/ai/crud-page.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/shared/runtime-rules.js src/components/lowcode-builder/shared/RuntimeRulesEditor.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/ai/crud-page.vue src/components/ai-form/AiForm.vue src/components/ai-form/AiFormLayoutNodes.vue src/components/ai-form/AiFormItem.vue`
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/shared/runtime-rules.js forge-admin-ui/src/components/lowcode-builder/shared/FieldValueRenderer.vue forge-admin-ui/src/components/lowcode-builder/shared/RuntimeRulesEditor.vue forge-admin-ui/src/components/ai-form/AiForm.vue forge-admin-ui/src/components/ai-form/AiFormItem.vue forge-admin-ui/src/components/ai-form/AiFormLayoutNodes.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`

### 跳过项

- 未启动 Vite 和浏览器截图验证：本轮先用静态检查和生产构建覆盖，运行规则组合需要在设计器真实页面中补充手工验收。
- 未执行后端编译：本轮只改前端设计器、表单运行态、列表运行态和 code-copilot 记录。
