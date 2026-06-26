# 执行日志：forge-list-designer-productivity-upgrades

## 2026-06-26 CRUD 字段快捷配置补充

### 变更范围

- `ListPageGridDesigner.vue` 的 AiCrudPage 右侧属性面板新增“字段快捷配置”折叠区。
- 字段快捷配置按当前对象字段列出字段名、字段编码，并提供搜索、导入、导出三个开关。
- 开关写入当前 CRUD 区块 `props.fieldSettings[field]`，并同步维护 `searchFields`、`importFields`、`exportFields` 字段列表。

### 命令与结果

- 当前 shell 命令通道异常：`pwd`、`date`、`pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue` 均直接返回 137，未能完成自动化验证。

### 跳过项

- 待命令通道恢复后补跑定向 eslint、`git diff --check` 和前端构建。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 23:05:03 CST

### 变更范围

- 修复 `RuntimeRulesEditor` 中“数据来源”下拉选择后仍回到旧值的问题。
- 原因是 `patchFirstCondition` 合并条件时最后又用旧 `conditions[0].source` 覆盖了本次选择的新 `source`。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/shared/RuntimeRulesEditor.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/shared/RuntimeRulesEditor.vue`：通过。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 22:58:57 CST

### 变更范围

- `RuntimeRulesEditor` 增加“数据来源”配置，支持当前记录/详情、当前行数据、当前表单数据、URL 查询参数、URL 路由参数和当前用户。
- `runtime-rules.js` 补充 `query/params` 来源解析，URL 查询参数直接按参数名取值，不要求用户写 `query.xxx`。
- `GridBlockRenderer`、发布运行页 `crud-page.vue`、`AiForm`、`AiFormLayoutNodes`、`AiFormItem` 统一注入 `route.query/route.params`。
- 表单规则上下文补齐 `row`，优先取 `context.currentRow`。列表操作列打开编辑/详情弹窗后，弹窗表单可以按当前行字段控制显示隐藏、只读、禁用、必填和样式。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/shared/runtime-rules.js src/components/lowcode-builder/shared/RuntimeRulesEditor.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/ai/crud-page.vue src/components/ai-form/AiForm.vue src/components/ai-form/AiFormLayoutNodes.vue src/components/ai-form/AiFormItem.vue`：通过，0 error；保留既有 warning。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/shared/runtime-rules.js forge-admin-ui/src/components/lowcode-builder/shared/RuntimeRulesEditor.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/components/ai-form/AiForm.vue forge-admin-ui/src/components/ai-form/AiFormLayoutNodes.vue forge-admin-ui/src/components/ai-form/AiFormItem.vue`：通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。

### 警告

- `AiForm.vue` 保留既有 `vue/no-required-prop-with-default` warning。
- 构建保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态和静态导入、部分 chunk 体积较大。

### 跳过项

- 浏览器手工验证待补：本轮未启动 Vite/后端服务。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 22:36:26 CST

### 变更范围

- 新增 `runtime-rules.js` 作为低代码运行规则唯一共享解析器，统一承接字段、模块和区块的显示隐藏、只读、禁用、必填、颜色和样式规则。
- 新增 `RuntimeRulesEditor.vue`，列表设计器和表单设计器右侧属性面板复用同一个规则编辑组件。
- 新增 `FieldValueRenderer.vue`，列表列、详情字段和表单只读展示统一支持文本、字典标签、状态 Tag、链接、金额、颜色规则和旧 render 配置兼容。
- `AiForm`、`AiFormLayoutNodes`、`AiFormItem` 接入运行规则上下文，表单新增/编辑/详情中的字段和模块可按状态或动态数据控制显示、隐藏、只读、禁用和必填。
- `GridBlockRenderer` 接入运行规则和统一字段渲染，详情信息区块、只读画布区块和表格列展示使用同一套渲染器。
- 应用运行页 `crud-page.vue` 接入统一字段渲染，发布后列表列的字典标签、颜色和链接跳转能力不再单独散落实现。
- 列表设计器和表单设计器“数据来源”里的请求方式、响应路径改为各占一行。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/shared/runtime-rules.js src/components/lowcode-builder/shared/FieldValueRenderer.vue src/components/lowcode-builder/shared/RuntimeRulesEditor.vue src/components/ai-form/AiForm.vue src/components/ai-form/AiFormItem.vue src/components/ai-form/AiFormLayoutNodes.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/ai/crud-page.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过，0 error；保留既有 warning。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/shared/runtime-rules.js forge-admin-ui/src/components/lowcode-builder/shared/FieldValueRenderer.vue forge-admin-ui/src/components/lowcode-builder/shared/RuntimeRulesEditor.vue forge-admin-ui/src/components/ai-form/AiForm.vue forge-admin-ui/src/components/ai-form/AiFormItem.vue forge-admin-ui/src/components/ai-form/AiFormLayoutNodes.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。

### 警告

- `AiForm.vue` 保留既有 `vue/no-required-prop-with-default` warning。
- `ListPageGridDesigner.vue` 保留既有 `vue/singleline-html-element-content-newline` warning，均为现有按钮单行文本格式提示。
- 构建保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态和静态导入、部分 chunk 体积较大。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务，运行规则组合需在真实设计器页面补充验收。
- 未执行后端编译：本轮只改前端 Vue 设计器、表单/列表运行态和 code-copilot 记录。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 17:58:00 CST Naive 页面组件扩展与预览编辑修正

### 变更范围

- `page-widget-schema.js` 新增共享页面组件：日历、代码、倒计时、描述、公示、列表、日志、数值动画、面包屑、菜单、分页和面板分隔。
- `PageWidgetRenderer.vue` 统一使用 Naive UI 组件渲染上述新增组件，并补齐 JSON 数据解析、布局约束和滚动约束。
- 富文本和 Markdown 的预览编辑状态改为只受组件自身 `readonly` 控制，列表预览弹窗的只读画布状态不再强行禁用编辑器。
- `page-schema.js` 移除旧手写描述列表来源，描述组件改为共享 Naive `NDescriptions`。
- `ListPageGridDesigner.vue` 左侧组件分组补充导航组件，右侧属性面板补齐新增 Naive 组件的基础配置入口。
- `ForgePropertyPanel.vue` 补齐表单设计器新增 Naive 组件属性入口。
- `formDesignerSchema.js` 将新增 Naive 页面组件标记为虚拟组件和全行组件，避免进入业务字段注册、DDL 和字段绑定校验。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/shared/PageWidgetRenderer.vue src/components/lowcode-builder/shared/page-widget-schema.js src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/form-first/formDesignerSchema.js src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/components/ai-form/AiFormItem.vue`：通过；保留既有 `vue/singleline-html-element-content-newline` warning。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/shared/PageWidgetRenderer.vue forge-admin-ui/src/components/lowcode-builder/shared/page-widget-schema.js forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入、部分 chunk 体积较大。
- 新增组件通过共享 `PageWidgetRenderer` 接入，未做浏览器手工交互截图；复杂组件如日历、菜单、分隔面板后续可按实际业务需求继续扩展事件和数据源配置。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 组件和 code-copilot 记录。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 22:09:36 CST

### 变更范围

- 列表设计器右侧动态展示组件“数据来源”配置从压缩多列改为纵向说明式配置。
- 表单设计器右侧共享页面组件“数据来源”配置同步改为同一套表达。
- “当前详情/表单数据”和“远程接口”按来源类型条件显示不同字段，避免用户看到无关输入项。
- 字段映射改为中文标签单列展示，减少窄面板下拉和输入框展示不全的问题。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过；保留 `ListPageGridDesigner.vue` 既有按钮单行文本 warning。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过，耗时约 1m44s。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。
- 构建保留 chunk size warning，不阻断本轮变更。

### 跳过项

- 未启动 Vite 和浏览器截图验证：本轮先以静态检查和生产构建覆盖，具体面板展示宽度需在本地设计器页面中手工确认。
- 未执行后端编译：本轮只改前端 Vue 设计器属性面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 17:10:00 CST 列表预览和组件库重复修正

### 变更范围

- `PageWidgetRenderer.vue` 中条形码改为用外部 `jsbarcode` 渲染当前组件自己的 SVG，避开 `vue3-barcode` 内部固定 `document.querySelector('.vue3-barcode-element')` 导致主画布和预览弹窗多实例互相抢占的问题。
- `PageWidgetRenderer.vue` 补齐富文本、Markdown、二维码、条形码容器的 `min-width: 0`、`max-width: 100%` 和 overflow 约束，避免内部编辑器撑开父容器。
- `page-schema.js` 移除旧二维码/条形码 catalog/default props，只保留共享 `pageWidgetCatalog` 作为列表组件库来源。
- `BusinessListDesigner.vue` 修复列表预览弹窗只读设计器使用 `width: max-content`、`overflow: visible` 导致富文本/Markdown 预览横向无限扩大问题，改为弹窗固定在视口内、内部滚动。
- `package.json` / `pnpm-lock.yaml` 新增直接依赖 `jsbarcode`。

### 命令与结果

- `pnpm --dir forge-admin-ui add jsbarcode`：通过。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/shared/PageWidgetRenderer.vue src/components/lowcode-builder/shared/page-widget-schema.js src/components/lowcode-builder/page/page-schema.js src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过；保留既有 `vue/singleline-html-element-content-newline` warning。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。
- `git diff --check -- forge-admin-ui/package.json forge-admin-ui/pnpm-lock.yaml forge-admin-ui/src/components/lowcode-builder/shared/PageWidgetRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`：通过。
- `rg -n "blockType: 'barcode'|blockType: 'qrcode'|title: '条形码'|title: '二维码'" forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/shared/page-widget-schema.js`：确认二维码/条形码只在共享 `page-widget-schema.js` 中定义。

### 警告

- `vue3-barcode` 仍保留在依赖中，但其组件实现对多实例不可靠；本轮运行渲染改为直接使用外部 `jsbarcode` 引擎。
- 构建保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入、部分 chunk 体积较大。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 组件、依赖和 code-copilot 记录。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 16:42:00 CST 表单运行态页面组件预览修正

### 变更范围

- `AiFormItem.vue` 增加虚拟页面组件统一分流：当字段 `componentKey/type` 属于共享页面组件且 `fieldBinding.mode === 'virtual'` 时，直接复用 `PageWidgetRenderer`。
- 修复表单预览弹窗和应用页把二维码等页面组件当普通 `NInput` 渲染的问题，避免 `props.size` 等页面组件配置透传给 Naive 输入框触发 `suffix.replace is not a function`。
- 运行态富文本、Markdown、穿梭框更新时按组件语义回写表单值；二维码、条形码、水印、HTML、Vue 组件等展示组件保持只读展示语义。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiFormItem.vue src/components/lowcode-builder/shared/PageWidgetRenderer.vue src/components/lowcode-builder/shared/page-widget-schema.js src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/page-schema.js src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/form-first/formDesignerSchema.js`：通过；保留既有 `vue/singleline-html-element-content-newline` warning。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。
- `git diff --check -- forge-admin-ui/src/components/ai-form/AiFormItem.vue forge-admin-ui/src/components/lowcode-builder/shared/PageWidgetRenderer.vue forge-admin-ui/src/components/lowcode-builder/shared/page-widget-schema.js code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`：通过。

### 警告

- 本轮修复覆盖的是 `AiForm` 运行态路径；设计画布路径此前已复用 `PageWidgetRenderer`。
- 构建保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入、部分 chunk 体积较大。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 组件和 code-copilot 记录。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 16:20:00 CST 外部组件运行时补丁

### 变更范围

- `PageWidgetRenderer.vue` 为 `@kangc/v-md-editor` vuepress 主题显式注入 `Prism`，修复表单/列表预览中 `Cannot read properties of undefined (reading 'languages')` 的运行时异常。
- `package.json` / `pnpm-lock.yaml` 新增直接依赖 `prismjs`，避免依赖树变化时 Markdown 主题缺少高亮语言对象。
- `page-widget-schema.js` 将二维码、条形码纳入共享页面组件 key、catalog 和默认 props，表单与列表预览统一走外部组件渲染。
- `ForgePropertyPanel.vue`、`formDesignerSchema.js` 补齐二维码/条形码表单属性和虚拟组件标记，避免预览组件参与业务字段注册。

### 命令与结果

- `pnpm --dir forge-admin-ui add prismjs`：通过。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/shared/PageWidgetRenderer.vue src/components/lowcode-builder/shared/page-widget-schema.js src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/form-first/formDesignerSchema.js`：通过；保留既有 `vue/singleline-html-element-content-newline` warning。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。
- `git diff --check -- forge-admin-ui/package.json forge-admin-ui/pnpm-lock.yaml forge-admin-ui/src/components/lowcode-builder/shared/PageWidgetRenderer.vue forge-admin-ui/src/components/lowcode-builder/shared/page-widget-schema.js forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`：通过。

### 警告

- `@kangc/v-md-editor` vuepress 主题需要 Prism 对象，缺失时会在组件更新阶段抛出 `languages` undefined，并级联触发 Vue runtime 的 `emitsOptions/type` null 错误。
- 构建保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入、部分 chunk 体积较大；外部富文本/Markdown/二维码/条形码组件会继续增加 `PageWidgetRenderer` chunk 体积。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 组件、依赖和 code-copilot 记录。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 16:05:00 CST 外部组件接入修正

### 变更范围

- `package.json` / `pnpm-lock.yaml` 新增外部组件依赖：`@wangeditor/editor`、`@wangeditor/editor-for-vue@5.1.12`、`@kangc/v-md-editor`、`qrcode-vue3`、`vue3-barcode`。
- `PageWidgetRenderer.vue` 中富文本改为 WangEditor Vue3 组件，Markdown 改为 `@kangc/v-md-editor`，水印继续使用真实 `n-watermark` 并按 Naive UI API 透传属性。
- `GridBlockRenderer.vue` 中水印改为 `n-watermark`，二维码改为 `qrcode-vue3`，条形码改为 `vue3-barcode`，移除手写二维码/条形码占位绘制逻辑。
- `page-widget-schema.js`、`page-schema.js`、`ListPageGridDesigner.vue`、`ForgePropertyPanel.vue` 补齐外部组件默认 props 和结构化属性配置。

### 命令与结果

- `pnpm --dir forge-admin-ui add @wangeditor/editor @wangeditor/editor-for-vue @kangc/v-md-editor@next qrcode-vue3 vue3-barcode`：通过；`vue3-barcode-qrcode` 在当前 registry 返回 404，因此使用 `qrcode-vue3` / `vue3-barcode` 两个外部生成组件替代。
- `pnpm --dir forge-admin-ui add @wangeditor/editor-for-vue@5.1.12`：通过；修正默认安装到 Vue2 peer 版本的问题。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/shared/PageWidgetRenderer.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/shared/page-widget-schema.js src/components/lowcode-builder/page/page-schema.js src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过；保留既有 `vue/singleline-html-element-content-newline` warning。
- `git diff --check -- forge-admin-ui/package.json forge-admin-ui/pnpm-lock.yaml forge-admin-ui/src/components/lowcode-builder/shared/PageWidgetRenderer.vue forge-admin-ui/src/components/lowcode-builder/shared/page-widget-schema.js forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。

### 警告

- `vue3-barcode-qrcode` 当前公开 registry 404，未能安装该精确包名。
- `vue3-barcode` 声明的间接 peer `@vitejs/plugin-vue@1.10.2` 期望 Vite 2，当前项目 Vite 7；生产构建已通过。
- 构建保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入、部分 chunk 体积较大；外部富文本/Markdown 组件使 `PageWidgetRenderer` chunk 明显增大。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 组件、依赖和 code-copilot 记录。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 15:10:00 CST Vue 组件 runtime-only 预览修正

### 变更范围

- 修复 `PageWidgetRenderer.vue` 中 Vue 组件预览依赖运行时 `compile()` 的问题，避免 runtime-only Vue 构建触发 `Runtime compilation is not supported` 和后续 `__vnode` 异常。
- Vue 组件预览改为安全模板插值 + Props 展示 + 代码视图，不在设计器内执行用户输入的 `template/script`。
- 列表设计器和表单属性面板将 Vue 组件预览模式文案调整为 `Props 模板预览`，与实际安全边界一致。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/shared/PageWidgetRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过；保留既有 `vue/singleline-html-element-content-newline` warning。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/shared/PageWidgetRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。

### 警告

- 定向 ESLint 保留既有/格式类 `vue/singleline-html-element-content-newline` warning，不阻断构建。
- 构建保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入、部分 chunk 体积较大。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器共享组件和 code-copilot 记录。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 14:41:25 CST 组件可用性补充修正

### 变更范围

- `shared/page-widget-schema.js` 将水印、穿梭框纳入共享组件 catalog/default props；穿梭框默认保留静态选项和远程接口 `optionSource` 结构。
- `shared/PageWidgetRenderer.vue` 补齐富文本工具栏编辑与内容回写、水印渲染、穿梭框静态/远程选项加载、Vue template 动态预览。
- 列表设计器 `GridBlockRenderer` / `ListPageGridDesigner` 接通共享组件更新事件，并补齐穿梭框远程接口配置项。
- 表单设计器 `ForgeFieldShelf` / `designerLayoutFactory` / `ForgeFormCanvasNode` / `ForgePropertyPanel` 复用共享组件，并补齐水印、Vue 动态预览属性入口；穿梭框按字段组件接入，支持业务字段绑定和远程接口选项。
- `AiFormItem.vue` 远程选项加载支持 `paramsText`，穿梭框动态接口参数配置可在运行态生效。
- `formDesignerSchema.js` 将水印等展示组件作为虚拟组件处理，避免进入字段注册和 DDL，同时保留穿梭框字段组件能力。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiFormItem.vue src/components/lowcode-builder/shared/page-widget-schema.js src/components/lowcode-builder/shared/PageWidgetRenderer.vue src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js src/views/app-center/components/designer/form-first/formDesignerSchema.js`：通过；保留既有 `vue/singleline-html-element-content-newline` warning。
- `git diff --check -- forge-admin-ui/src/components/ai-form/AiFormItem.vue forge-admin-ui/src/components/lowcode-builder/shared/page-widget-schema.js forge-admin-ui/src/components/lowcode-builder/shared/PageWidgetRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js`：通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过；二次构建覆盖字段型穿梭框和 `AiFormItem` 参数解析补丁。

### 警告

- 首次按偏好执行 `nvm use v20.19.0` 返回 N/A，本轮后续直接使用当前可用 Node 执行 pnpm 命令。
- 定向 ESLint 保留既有/格式类 `vue/singleline-html-element-content-newline` warning，不阻断构建。
- 构建保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入、部分 chunk 体积较大。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器、共享组件和 code-copilot 记录。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 页面组件真实能力与表单复用修正

### 变更范围

- 抽取 `shared/page-widget-schema.js` 和 `shared/PageWidgetRenderer.vue`，统一维护富文本、Markdown、HTML 标签、Vue 组件的默认配置、安全渲染和预览逻辑。
- 列表设计器 `GridBlockRenderer` 改为复用共享 `PageWidgetRenderer`，不再在列表内维护一套富文本/Markdown/HTML/Vue 预览实现。
- 表单设计器左侧组件库接入共享页面组件，`designerLayoutFactory` 使用共享默认配置创建组件，`ForgeFormCanvasNode` 使用共享渲染器展示组件。
- 表单属性面板为共享组件补充基础代码配置入口；列表右侧属性面板为富文本、Markdown、HTML 和 Vue 组件补充源码/模板/脚本/样式/props 等配置。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/shared/page-widget-schema.js src/components/lowcode-builder/shared/PageWidgetRenderer.vue src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js src/views/app-center/components/designer/form-first/formDesignerSchema.js`：通过；保留单行模板换行 warning。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/shared/page-widget-schema.js forge-admin-ui/src/components/lowcode-builder/shared/PageWidgetRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。

### 警告

- 定向 ESLint 保留既有/格式类 `vue/singleline-html-element-content-newline` warning，不阻断构建。
- 构建保留项目既有 warning：CSS 中存在 `//` 注释、部分 chunk 体积较大、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器和 code-copilot 记录。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 页面组件库扩展

### 变更范围

- 左侧页面组件库新增手写签名、富文本框、穿梭框、分步表单、Vue 组件占位、HTML 标签、标题、段落、统计数值、链接、文字提示、水印、音频播放器、视频播放器、头像框、条形码、内嵌页面、二维码、Markdown、盒子布局、间距和描述列表。
- `page-schema.js` 补齐新增组件 catalog、默认 props、默认 fieldRefs 和最小高度规则。
- `GridBlockRenderer.vue` 补齐新增组件设计态/运行态预览；签名复用现有 `SignaturePad`，二维码/条形码使用无新增依赖的安全预览占位。
- `ListPageGridDesigner.vue` 补齐左侧分组、子组件可选项、右侧结构化属性配置和栅格布局子组件自动撑高逻辑。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过；保留单行模板换行 warning。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。

### 警告

- 定向 ESLint 保留既有/格式类 `vue/singleline-html-element-content-newline` warning，不阻断构建。
- 构建保留项目既有 warning：CSS 中存在 `//` 注释、部分 chunk 体积较大、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器和 code-copilot 记录。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-21 12:20:52 CST

### 变更范围

- 列表自由画布顶部摘要去掉“行 x 列”信息，只保留区块数、设计宽度、预览形态和缩放比例。
- `list-page-switch` 展开态增加面板间距，避免页面配置、页面入参贴住上方页签。
- `page-config-row` 改为浅底卡片式配置面板，输入区统一宽度和视觉层级。
- `page-param-row` 改为轻量参数面板，参数项使用紧凑条目样式，删除参数改为图标按钮。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过；`nvm use v20.19.0` 本机返回 N/A，实际使用当前 Node `v20.20.0`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器布局和 scoped 样式。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-21 12:12:25 CST

### 变更范围

- 视图控制从常驻表单面板改为底部图标控件：预览视口图标弹出预览形态、设计宽度和自定义宽度；缩放按钮弹出缩放选择和加减按钮。
- 自由画布缩放 stage 改为 flex 居中，拖放、缩放和切换设计宽度后画布主体保持在中间。
- `list-designer-head` 压缩为紧凑单行，撤销/重做改为图标按钮，去掉重复的布局状态标签。
- `list-page-switch` 压缩为页面标题、页面 tabs、图标操作同一行，新增/设置/复制/重置/清空/删除改为图标按钮。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器布局与缩放交互。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-21 12:00:09 CST

### 变更范围

- 列表自由画布顶部工具条只保留源码、专注画布、清空和重置默认等主操作。
- 设计宽度、预览形态、缩放控制迁移到画布内部右上角浮动视图控制区，降低中间画布顶部拥挤。
- 列表自由画布支持 `Ctrl/⌘ + 滚轮` 缩放，普通滚轮仍保留滚动画布行为。
- 工具栏状态摘要显示当前预览形态、设计宽度和缩放比例。

### 命令与结果

- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue`：通过。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器布局与缩放交互。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 18:48:00 CST

### 变更范围

- 修复专注画布模式下左右展开 rail 按钮仍被渲染，导致 grid 自动布局残留占位的问题。
- 专注画布下 `canvas-zoom-stage` 改为按内容宽度居中，不再因为 `min-width: 100%` 让真实画布贴左。
- 进入专注画布时自动把横向滚动位置调整到中间，避免大屏下内容挤在左侧。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过；实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮为单文件布局修复。
- 未执行前端全量构建：本轮增量执行定向 eslint 和空白检查。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 18:46:06 CST

### 变更范围

- 移除列表设计工具栏内重复的“保存列表”按钮。
- 保留 `BusinessListDesigner.saveLayout()` 暴露方法，右上角全局“保存”在列表页仍调用同一保存逻辑。
- 列表页保存入口统一为右上角“保存”，减少同屏两个保存按钮的语义混淆。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessListDesigner.vue`：通过；本机无精确 `v20.19.0`，实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。

### 跳过项

- 未执行前端全量构建：本轮只移除重复按钮并保留原保存方法，已做定向 lint 与空白检查。
- 未执行后端编译：本轮只改前端 Vue 模板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 18:40:41 CST

### 变更范围

- 单按钮组件的“按钮配置”拆分为“基础样式”和“按钮状态”，基础样式配置按钮文本、类型和尺寸。
- 按钮状态改为逐行开关并补充说明：次要按钮、撑满宽度、禁用状态、加载状态。
- 修复设计器预览中单按钮被写死 `disabled` 的问题，只有打开“禁用状态”时才禁用；打开“加载状态”和“撑满宽度”会在画布预览中立即可见。
- 单按钮默认 schema 补齐 `size`、`disabled`、`loading` 字段。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/page-schema.js`：通过；本机无精确 `v20.19.0`，实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js`：通过。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器和页面 schema。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 18:36:59 CST

### 变更范围

- 修复“专注画布”复用左右面板收起状态的问题：专注状态改为独立 `canvasFocusMode`，不再修改 `paletteCollapsed` / `propertyCollapsed`。
- 专注画布时左侧“页面组件”和右侧“配置组件”直接不渲染，避免响应式布局下跑到画布上方/下方。
- 退出专注后保留进入专注前的左右面板展开/收起状态。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过；本机无精确 `v20.19.0`，实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 18:34:05 CST

### 变更范围

- 列表设计器左侧“页面组件”和右侧“配置组件”收起后不再保留 42px 侧栏列，中间画布直接释放并占用对应空间。
- 左右面板展开入口改为画布边缘悬浮小箭头，避免收起后看起来仍有一个独立左右模块。
- AiCrudPage “工具栏与导入导出”配置从混合 checkbox 改为逐行显示开关，开关打开即显示对应顶部工具栏按钮，关闭即隐藏。
- 工具栏开关补充说明文案，明确“新增、批量删除、导入、导出、自定义查询、导出任务入口”分别控制什么，以及接口/工具栏总开关关闭时为什么看不到变化。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过；本机无精确 `v20.19.0`，实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。

### 警告

- 未执行前端全量构建：本轮只改 `ListPageGridDesigner.vue` 的模板、状态映射和 scoped 样式，已做定向 eslint 与空白检查。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 18:35:00 CST

### 变更范围

- 筛选树收起态从“筛选树 + 树标题”简化为单个竖向“树”标识，完整标题仅保留在 hover title，降低画布占用和视觉噪声。
- 页面组件面板取消原生 `disabled` 卡片：已存在的唯一组件显示“已在画布中”，点击后选中并滚动到画布中的已有组件。
- 当前布局不支持的组件显示“当前布局不可用”，点击时给出信息提示，避免看起来像按钮坏了。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过；实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮为小范围交互和样式调整。
- 未执行前端全量构建：上一轮已完成 `pnpm build`，本轮增量执行定向 eslint 和空白检查。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 18:17:52 CST

### 变更范围

- 移除全局 `n-tree` 选中节点左侧指示条，避免所有树组件被统一插入额外竖线。
- 修复筛选树运行态节点内容短时居中问题：树节点与内容区强制撑满宽度并左对齐。
- 筛选树整块展开/收起按钮改为贴右边缘的窄按钮；节点展开/收起独立为“节点层级”工具条，减少标题区混乱。
- `AiCrudPage` 的“添加下级”行操作改为显式 `enableTreeAddChild` 开关，左树右表筛选布局默认不显示。
- 自由画布增加“专注画布”模式，一键隐藏左右配置面板，释放中间画布宽度。
- 画布新增拖拽边缘自动滚动，并在移动/缩放区块时把滚动距离计入位移计算，避免拖到可视区外时元素跟丢。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/ai-form/AiCrudPage.vue src/components/ai-form/AiCrudPageProps.js src/views/ai/crud-page.vue`：通过；本机无精确 `v20.19.0`，实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/ai-form/AiCrudPage.vue forge-admin-ui/src/components/ai-form/AiCrudPageProps.js forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/styles/global.css`：通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。

### 警告

- 首次按偏好执行 `nvm use v20.19.0` 返回 N/A；本机安装的是 `v20.19.5` 和 `v20.20.0`，本轮使用 `v20.19.5` 验证。
- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器和运行态 CRUD 行操作逻辑。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 17:47:18 CST

### 变更范围

- 修复默认参数编辑器点击“新增”后空参数行被父级 props 回刷清空的问题。
- 新增空参数行只保留在编辑器本地 UI，不再立即向父级写入空 `{}`，避免触发 `AiCrudPage.publicParams/publicQuery` 监听导致中间画布列表刷新。
- 自由画布和 CRUD 快速配置的默认参数更新入口增加相同值保护，避免重复写入相同配置。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/CrudDefaultParamsEditor.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/CrudDefaultParamsEditor.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 默认参数编辑器和父级更新保护。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 17:40:49 CST

### 变更范围

- 新增共享 `CrudDefaultParamsEditor`，自由画布 `配置组件` 和 `CRUD 快速配置` 统一使用同一套默认参数配置。
- AiCrudPage 增加 `formDefaultValues` 与 `submitDefaultParams`，并复用已有 `publicParams/publicQuery`：列表查询、URL query、打开表单默认值、提交固定参数四类配置分开维护。
- 新增/编辑打开表单时合入 `formDefaultValues`；编辑场景已有记录值优先，不被默认值覆盖。
- 新增/编辑提交前合入 `submitDefaultParams`，再执行 `beforeSubmit` 回调，允许高级回调继续覆盖固定参数。
- 发布运行页、列表设计器预览、自由画布单区块预览同步传递默认参数，避免设计态和发布态分叉。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiCrudPage.vue src/components/ai-form/AiCrudPageProps.js src/components/lowcode-builder/page/CrudDefaultParamsEditor.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/page-schema.js src/views/app-center/components/designer/BusinessListDesigner.vue src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/ai-form/AiCrudPage.vue forge-admin-ui/src/components/ai-form/AiCrudPageProps.js forge-admin-ui/src/components/lowcode-builder/page/CrudDefaultParamsEditor.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 组件、设计器配置和运行态 props。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 17:23:51 CST

### 变更范围

- 修复回调规则编辑器“新增规则”看似无反应的问题：编辑态归一化保留空白新行，避免新增后立即被空规则过滤逻辑删除。
- 列表设计器编辑态和预览弹窗的 `AiCrudPage` 运行 props 补齐 `crudHookRules/beforeSubmitRules` 编译结果，提交前、搜索前、加载前等参数处理与发布页保持一致。
- 保持运行态执行逻辑只执行已填写目标字段的规则，避免空白编辑行影响真实请求参数。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/CrudHookRulesEditor.vue src/components/lowcode-builder/page/crud-hook-rules.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/CrudHookRulesEditor.vue forge-admin-ui/src/components/lowcode-builder/page/crud-hook-rules.js forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器预览和共享回调规则编辑器。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-19 21:29:05 CST

### 变更范围

- 修复左树右表运行态边界：右侧 `AiCrudPage` 运行 props 清空 `treeConfig` 与 `options.treeConfig`，避免右表误进入树表模式。
- `tree-panel` 运行态继续使用独立 `apiConfig.tree` 加载树数据；树节点选择只通过 `publicParams` 给右侧列表追加过滤字段。
- 简化筛选树属性面板，移除“高级联动 / 生成联动事件”入口，改为说明树接口和右表过滤字段的关系。
- 保留预览弹窗 `.n-card__content` / `.n-card-content` 的全高 flex 与滚动约束。

### 命令与结果

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint ...`：未执行到 eslint，`nvm use v20.19.0` 返回 `N/A: version "N/A" is not yet installed`。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 运行态和设计器配置面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 09:24:00 CST

### 变更范围

- 列表设计器编辑态和预览弹窗的 `AiCrudPage` 改为接入当前 `pageSchema.zones.edit` 编译出的运行态表单配置，新增/编辑弹窗优先使用表单设计器同步出的 `fieldSettings`、`formLayout`、校验规则、组件 props 和布局参数。
- `GridBlockRenderer` 的运行态 CRUD 预览改为合并当前区块自身配置，避免传入统一预览 props 后覆盖自由画布里单个 CRUD 区块的接口、按钮、布局和回调配置。
- 自由画布 `配置组件` 中 AiCrudPage 的基础路径和接口地址在未手动配置时显示按当前模型生成的默认接口。
- 回调规则编辑器按钮显式设置 `attr-type="button"` 并阻止外层表单默认行为；规则更新先规范化并立即写入本地状态，再通知父级。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/CrudHookRulesEditor.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/CrudHookRulesEditor.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器预览和配置面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 09:05:00 CST

### 变更范围

- 修复树模块折叠后内部 `grid-block` 仍按保存的固定宽度渲染的问题：折叠状态强制组件根节点宽度跟随父级 44px。
- 将整块树折叠按钮从模块外沿移回树模块内部右上角，避免被外层 `overflow:hidden` 裁切或被右侧 CRUD 覆盖。
- 调整树标题区布局，给折叠按钮预留空间，并把树节点展开/收起按钮做成更紧凑的横向按钮组。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器树模块样式和折叠宽度。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 08:57:20 CST

### 变更范围

- 将左侧筛选树的“收起面板”改为区块级折叠：折叠后树区块宽度压缩到 44px，并保留竖向侧栏提示。
- 父级画布接收 `treePanelCollapseChange`，在只读预览/运行态下重算同一行右侧区块位置和宽度，让 CRUD 列表真正补满树释放的空间。
- 树节点展开/收起按钮继续保留在树标题内；整块树折叠改为模块边缘箭头按钮，避免与节点展开收起混淆。
- 修复 CRUD 快速配置搜索区域：配置面板改为“头部 + 搜索 + 滚动内容”三行布局，搜索区域有稳定白色背景和分隔阴影。
- 自由画布配置组件搜索区域补齐固定高度、白底和层级，避免遮挡配置内容。
- 预览弹窗内容区、画布容器、滚动区和内部网格统一白底并撑满高度，避免弹窗下半截透明/无背景。
- 回调参数处理编辑器改为单列规则输入，降低窄面板内输入框横向溢出的概率。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/CrudHookRulesEditor.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/CrudHookRulesEditor.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器和预览样式。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 17:16:33 CST

### 变更范围

- 表单设计器左侧组件库补充语义化图标映射，新增共享页面组件和常用字段组件不再大量复用相同图标。
- 列表设计器左侧页面组件面板增加组件统计，展示总数、搜索匹配数、分组数和分组内组件数量。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`：通过。

### 警告

- `ListPageGridDesigner.vue` 保留既有 `vue/singleline-html-element-content-newline` warning，均为现有按钮单行文本格式提示，本轮未引入错误。

### 跳过项

- 未重新执行生产构建：本轮只调整组件面板展示图标、统计文案和样式，不涉及运行态渲染或依赖变更。
- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 17:26:39 CST

### 变更范围

- 列表设计器右侧配置面板调整为和表单设计器一致的结构顺序：标题头部、搜索、配置类型 Tab、滚动内容。
- 列表设计器右侧配置面板补齐 48px 头部高度、分割线、标题截断、折叠区块密度等样式，和表单设计器右侧面板保持一致。
- 表单设计器右侧收起按钮改为和列表设计器一致的轻量边框按钮样式。
- 记录后续能力缺口：Naive UI 新增组件需要补齐官方 Props/Slots/Events 结构化配置，并支持页面详情接口、已有列表接口或自定义接口取数。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`：通过。

### 警告

- `ListPageGridDesigner.vue` 保留既有 `vue/singleline-html-element-content-newline` warning，均为现有按钮单行文本格式提示，本轮未引入错误。

### 跳过项

- 未重新执行生产构建：本轮只调整右侧配置面板展示结构和 CSS，不涉及运行态渲染、依赖或构建配置。
- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 17:38:47 CST

### 变更范围

- 列表设计器左侧页面组件卡片补充语义图标，组件库展示结构改为图标 + 主体信息，和表单设计器左侧组件卡片一致。
- 表单设计器左侧组件面板补充统计胶囊，搜索组件时展示匹配数量，组件库展示当前分组数，字段资产展示字段数量。
- 统一列表/表单左侧面板的头部、搜索框、分组标题圆点、计数胶囊、组件卡片高度、图标底色、hover 反馈和字段列表密度。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`：通过。

### 警告

- `ListPageGridDesigner.vue` 保留既有 `vue/singleline-html-element-content-newline` warning，均为现有按钮单行文本格式提示，本轮未引入错误。

### 跳过项

- 未重新执行生产构建：本轮只调整左侧组件面板模板和 CSS，不涉及运行态渲染、依赖或构建配置。
- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-25 18:01:17 CST

### 变更范围

- 列表设计器左侧页面组件图标按组件语义细分，新增 Naive UI 组件不再大量使用兜底图标。
- 共享页面组件默认 props 增加 `dataBinding`，统一描述静态配置、当前详情/表单上下文和远程接口三类数据来源。
- `PageWidgetRenderer` 接入 `dataBinding`，描述、列表、日志、代码、数值动画、面包屑、菜单、分页、公示、日历和倒计时等组件优先使用绑定数据。
- 列表设计器和表单设计器右侧属性面板补充数据来源配置，包括接口地址、请求方法、参数 JSON、响应路径和字段映射。
- `GridBlockRenderer` 为 `detail-info` 增加数据来源配置和远程详情接口加载能力；运行态按当前详情数据、字段路径或远程接口返回值渲染详情字段。
- `AiFormItem` 和 `GridBlockRenderer` 向共享页面组件传递当前 `formData/runtimeRecord`，支持“从当前页面详情/表单数据取值”。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/shared/page-widget-schema.js src/components/lowcode-builder/shared/PageWidgetRenderer.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/ai-form/AiFormItem.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/shared/page-widget-schema.js forge-admin-ui/src/components/lowcode-builder/shared/PageWidgetRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/ai-form/AiFormItem.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/shared/page-widget-schema.js forge-admin-ui/src/components/lowcode-builder/shared/PageWidgetRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/ai-form/AiFormItem.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`：通过。

### 警告

- `ListPageGridDesigner.vue` 保留既有 `vue/singleline-html-element-content-newline` warning，均为现有按钮单行文本格式提示，本轮未引入错误。
- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。
- 构建保留 chunk size warning，不阻断本轮变更。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务，远程接口取数需在真实设计器页面中补充验收。
- 未执行后端编译：本轮只改前端 Vue 设计器、共享渲染器和 schema。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-19 22:42:11 CST

### 变更范围

- 抽出共享 `CrudHookRulesEditor.vue` 和 `crud-hook-rules.js`，`CRUD 快速配置` 与自由画布 `AiCrudPage` 属性面板统一复用同一套回调参数处理配置。
- 回调配置从单一 `beforeSubmitRules` 扩展为统一 `crudHookRules`，覆盖加载列表前、搜索前、打开表单前、打开详情前、提交前、构建提交数据后等对象参数处理场景。
- 运行页 `crud-page.vue` 编译 `crudHookRules` 为真实 `AiCrudPage` hook，并从发布画布的 `AiCrudPage.props` 兜底读取；旧 `beforeSubmitRules` 自动兼容迁移。
- 统一 `CRUD 快速配置` 与自由画布 `配置组件` 搜索样式。
- 简化 `CRUD 快速配置` 面板布局：窄面板表单默认一列、开关两列、复杂行纵向排列，避免输入框横向溢出。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/CrudHookRulesEditor.vue src/components/lowcode-builder/page/crud-hook-rules.js src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/page-schema.js src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/CrudHookRulesEditor.vue forge-admin-ui/src/components/lowcode-builder/page/crud-hook-rules.js forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 运行态和设计器配置面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-19 22:13:19 CST

### 变更范围

- 筛选树区块增加设计态和运行态的展开/收起控制，运行态 `n-tree` 改为受控 `expandedKeys`。
- 修复 CRUD 快速配置中左树右表预览容器：去掉右侧 CRUD 硬最小宽度，补齐 `min-width: 0` 与内部滚动约束，避免右侧内容显示不全。
- `CRUD 快速配置` 增加配置搜索；自由画布右侧 `配置组件` 增加配置搜索。
- 增加可视化 `beforeSubmitRules` 配置，支持提交前设置固定值、复制字段、尾部拼接和清空字段；运行页编译为 `AiCrudPage.beforeSubmit`。
- 设计态 AiCrudPage 新增/编辑预览优先使用当前对象完整字段集合，减少和应用页表单内容不一致的问题。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/page-schema.js src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 运行态和设计器配置面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-19 21:38:58 CST

### 变更范围

- 针对已发布 `listGridLayout.layoutType = tree-crud` 但运行配置顶层 `layoutType` 仍为 `simple-crud` 的场景，运行页改为优先使用当前画布布局类型。
- `crudProps` 的树表判断改为读取有效布局类型，避免左树右表右侧列表继续把 `apiConfig.list` 替换成 `apiConfig.tree`。
- 删除 `BusinessListDesigner.vue` 中残留的 `.n-card-content { background: red; }` 调试样式。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。
