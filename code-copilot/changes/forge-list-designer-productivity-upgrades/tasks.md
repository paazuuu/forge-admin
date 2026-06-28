# 任务清单：forge-list-designer-productivity-upgrades
> status: apply
> spec: `code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md`

## 本轮任务

- [x] 分析列表设计器与表单设计器可复用能力边界。
- [x] 列表设计器增加会话级撤销/重做和快捷键。
- [x] 压缩列表设计器头部区域，并收纳低频操作。
- [x] 自由布局画布增加拖拽落点反馈，并把移动/缩放合并为单次提交。
- [x] 结构化列表配置改为左预览、右字段配置工作区，保留大窗口编辑入口。
- [x] 自由布局区块属性配置从抽屉改为固定右侧配置面板。
- [x] 自由布局区块缩放改为八方向锚点，支持上下左右和四角缩放。
- [x] 自由布局增加左侧区块库、右侧配置区块收起展开。
- [x] 自由布局区块增加顶部拖动手柄和右上角更多操作。
- [x] 清理选中态重复边框，实心化缩放锚点。
- [x] 优化自由布局画布、区块库、右侧配置区块的视觉层次。
- [x] 左侧区块库补充 `AiCrudPage`、`AiTable`、`AiForm` 系统组件区块。
- [x] 自由布局拖动命中其它区块时支持位置交换。
- [x] 修正更多操作可见性、画布边界、属性 label 竖直居中和分组标题标记遮挡问题。
- [x] 自由画布改为列表设计默认主模式，结构化配置改为快速配置辅助模式。
- [x] 自由画布列宽跟随中间工作区自适应，修复右侧空白区域不可拖拽/缩放问题。
- [x] 自由画布移动过程改为视觉跟手和落点色块预览，松手后再提交 schema。
- [x] 自由画布增加“清空当前画布”功能，区别于“重置默认”。
- [x] 修复空画布被同步逻辑自动回填默认区块的问题，逐个删除到最后也能保持空画布。
- [x] 区块库增加搜索，支持按名称、描述、组件 key 过滤。
- [x] 自由画布增加源码查看弹窗，展示当前布局 JSON 和选中区块 JSON。
- [x] 优化配置字段弹层样式，避免字段设置下拉超出抽屉。
- [x] 左侧“区块库”改为“页面组件”，组件库定位从固定业务区块调整为通用页面组件。
- [x] 右侧属性面板增加通用外观配置：宽高、背景、边框、圆角、阴影、内边距、自定义 style。
- [x] 新增通用布局组件：卡片、Tabs、横向/竖向分隔线、留白占位。
- [x] 卡片和 Tabs 支持承载子组件，拖入容器后按 children 渲染。
- [x] 页面组件库增加“返回上一页”组件，详情页默认内置返回入口。
- [x] 自由布局支持页面级多画布，默认包含列表页和详情页，并支持新增空白页面。
- [x] 多页面管理补充新增、复制、删除自定义页、页面重命名、页面编码、类型、路径、说明和入参配置。
- [x] 默认详情页允许删除，并通过 `removedPageKeys` 防止保存/刷新后自动补回。
- [x] 默认详情页改为页面标题、返回上一页和 `detail-info` 只读详情信息组件，不再默认使用 `AiForm`。
- [x] 当前页面支持重置布局和清空布局，重置/清空只作用于当前页面。
- [x] 页面编码变更时同步更新事件里的目标页面引用。
- [x] 右侧属性面板为每个页面组件增加通用事件配置，事件统一写入 `props.events`。
- [x] 通用事件配置补充目标页面选择，支持页面跳转类动作引用页面资源。
- [x] 左侧导航树增加节点选择联动目标配置，可生成 `nodeSelect -> filterBlock` 事件。
- [x] 左侧页面组件改为两列布局展示。
- [x] 页面组件默认样式去掉边框和圆角，保留右侧显式外观配置能力。
- [x] 页面组件库扩充页面标题、详情信息、提示面板、按钮、按钮组、标签列表、步骤条、时间线、空状态等常用组件。
- [x] `AiCrudPage`、`AiTable`、`AiForm` 右侧属性补充结构化配置，覆盖显示项、布局、接口、搜索、表格、弹窗、工具栏和事件说明，不再要求手写接口 JSON 或回调方法名。
- [x] 系统组件画布预览移除额外说明头，直接渲染真实组件内容。
- [x] 页面组件外观区分设计画布显示框和运行时 CSS 尺寸，默认 `width: 100%`，支持 `100% / auto / 固定 px` 宽度模式。
- [x] 设计画布宽度改为布局级可配置，默认 1366，可在工具栏选择 1200/1366/1440/1600/1920 或手输宽度。
- [x] 自由画布拖拽、缩放和右侧位置尺寸编辑改为 px 交互，`gridX/gridY/gridW/gridH` 仅保留兼容同步；预览态不使用设计画布宽度，按预览容器计算 `100%` 宽度。
- [x] 新建/重置列表页默认生成 `AiCrudPage`，按当前模型字段初始化，作为可直接调整的默认页面。
- [x] 页面路径 UI 文案调整为路由片段/页面标识，说明当前多页面由统一业务对象运行页承载。
- [x] 画布模块 hover 保留区域边框和阴影反馈，移除组件名称浮标。
- [x] 优化多页面切换区视觉和交互，隐藏横向滚动条，增加页面类型颜色提示和操作说明。
- [x] 修复顶部预览被表单设计器事件拦截的问题；列表预览改为本地全屏弹窗，不保存、不校验、不调预览接口。
- [x] 字段列配置增加链接文本、文字颜色、点击跳转目标页面等无代码配置，并在设计器预览中渲染链接样式。
- [x] 字段列跳转配置补充参数名和取值字段，默认 `id = row.id`。
- [x] 详情页页面配置补充详情接口、请求方式和详情数据字段。
- [x] 自由画布字段配置改为带标签的两列配置，补充跳转参数说明，避免下拉超出抽屉。
- [x] 快速配置改名为 CRUD 配置，明确只配置默认 `AiCrudPage`，并将字段、列属性同步到 `listGridLayout` 和 `pages.list.gridLayout`。
- [x] 保存前按当前画布规范化同步 `zones/viewSchema`，避免发布后应用页面继续使用旧结构化配置。
- [x] CRUD 配置去掉遮挡式字段弹窗，改为右侧内联配置区，配置属性、字段配置和树表模板配置统一在同一侧面板完成。
- [x] CRUD 配置左侧预览改为复用 `GridBlockRenderer` 的 `AiCrudPage` 渲染逻辑，不再手拼一套查询表格预览。
- [x] 列表模板入口从“左侧导航”调整为“标准 CRUD 列表 / 左树右表模板”，避免用户误解为系统侧边菜单配置。
- [x] CRUD 列配置补齐文字颜色、点击动作、目标页面、参数名和取值字段，与自由画布列配置使用同一套字段 schema。
- [x] 树区块命名从“左侧导航树”调整为“筛选树”，树表模板说明明确为“树节点筛选右侧列表”。
- [x] CRUD 配置工作区改为 flex 布局，右侧配置面板增加“展开配置/收起配置”和整体滚动条，避免配置项过多时无法滚动。
- [x] 左树右表模板增加主区块重排修复，发现 CRUD 主区仍压到树区域时自动按模板恢复为左侧筛选树、右侧 CRUD。
- [x] CRUD 配置工作区取消固定视口高度，改为跟随列表设计主体的真实左右 flex 分栏，预览区和配置区各自滚动。
- [x] 切换标准 CRUD / 左树右表模板时重建标准布局骨架，只继承字段、排序、列样式和树配置，避免旧 px 坐标继续遮盖筛选树。
- [x] 小分辨率下压缩列表设计器顶部区域，页面设置改为按需展开，画布内部去掉固定最小高度。
- [x] 自由画布增加设计期缩放，支持 50% / 67% / 75% / 90% / 100% / 125%，拖拽、移动和缩放坐标按缩放比例换算。
- [x] 左树右表刷新恢复优先读取 `pages.list.gridLayout`，避免刷新后误判为标准 CRUD 或树区块空白。
- [x] 优化筛选树区块预览样式，增加标题、副标题、搜索占位、节点层级和字段映射说明。
- [x] 预览弹窗内容改为全高 flex 布局并保留内部滚动，避免预览内容溢出弹窗。
- [x] CRUD 配置右侧面板支持收起，收起后保留窄条入口可再次展开。
- [x] 自由画布移动、缩放和位置尺寸输入增加区块重叠拦截，避免一个区块覆盖住另一个区块。
- [x] 页面操作按钮简化文案并补充图标：新增、复制、重置、清空、删除、设置。
- [x] AiCrudPage 的表单与弹窗配置区补充说明，明确新增/编辑弹窗使用表单设计字段和 AIForm 渲染。
- [x] 页面设计“设置”移入页面操作区，标题区只保留页面切换说明，避免设置和收起关系混乱。
- [x] CRUD 配置补齐 AiCrudPage 常用属性：基础显示、接口、搜索表格、弹窗工具栏和事件配置，并同步写入默认 CRUD 区块。
- [x] 优化 CRUD 配置和右侧组件属性折叠面板样式，提升分组边界、标题层级和表单可读性。
- [x] 修复 CRUD 配置收起按钮不可见问题，改回面板头部明确可见操作。
- [x] 修复 CRUD 配置模式顶部预览仍读取旧 zones 的问题，预览优先使用当前 `pages.list.gridLayout/listGridLayout`。
- [x] 左树右表“生成联动事件”自动选择当前业务列表组件作为目标，避免写死不存在的 `block_table`。
- [x] CRUD 配置补充真实接口预览开关、字段配置入口、字典标签预览和自定义列表操作按钮配置。
- [x] 记录运行态承接边界：多页面保存在 `pageSchema.pages[]`，统一业务对象路由后续需要解释 `targetPageKey` 和页面参数完成内部导航。
- [x] 更新多画布设计判断：首期采用 `pageSchema.pages[]` 承载列表页、详情页和自定义页面，后续补运行态动作目标。
- [x] 修复 CRUD 快速配置“配置查询字段 / 配置列表字段”点击后无明显反馈，切换激活态并自动滚动到字段配置区域。
- [x] CRUD 快速配置左树右表模式的内嵌预览同步展示筛选树和右侧 AiCrudPage，不再只在弹窗预览里看到树。
- [x] CRUD 快速配置接口区按当前业务配置推导默认 `/ai/crud/{configKey}` 接口并回显到输入框和真实接口预览。
- [x] 发布运行态同步列级文字颜色、点击跳转、目标页面、参数名和取值字段，运行页列渲染支持颜色和跳详情。
- [x] 预览弹窗补充只读画布滚动容器高度和 overflow 约束，避免内容纵向溢出后没有滚动条。
- [x] 修复从列表设计切换到发布或直接发布时未强制同步列表草稿的问题，发布前会规范化并写回最新 `pageSchema/viewSchema`。
- [x] 列表布局保存时同步完整 `pageSchema` 到设计器草稿，避免应用页继续读取旧发布结构。
- [x] 优化 CRUD 快速配置预览卡片和左树右表滚动，预览区改为 flex 包裹，左树右表内部保留横向/纵向滚动。
- [x] 修复发布 JSON 中 `AiCrudPage.props.fieldSettings` 已存在但运行态列配置未读取的问题，运行态编译器会从自由画布区块读取列颜色、跳转、默认排序和自定义操作。
- [x] 应用运行页 `/ai/crud-page/:configKey` 支持渲染已发布的 `listGridLayout` 只读画布，主 `AiCrudPage` 区块复用真实运行态 `crudProps`，避免设计器布局在应用页丢失。
- [x] 优化列表预览弹窗和只读画布样式，修复弹窗宽高溢出、设计态网格背景混入预览、预览背景和内容背景不一致的问题。
- [x] 运行页从发布 `pageSchema.listGridLayout.items[].props.fieldSettings` 兜底合并列颜色、点击跳转，避免旧 `columnsSchema` 未重新编译时列样式不生效。
- [x] 只读/运行态画布保留发布设计宽度，不再按当前容器压缩导致 1366 设计宽度变成约 1108。
- [x] 列表设计器保存协议补齐 `pages[]` 与 `removedPageKeys` 后端承接，避免详情页清空、删除内置页后保存又被默认布局补回。
- [x] 详情页本地预览改为预览当前选中页面，不再固定预览列表页。
- [x] 左树右表模板切换保留自定义区块并把落在树区域的区块右移；保存/刷新时不再自动补回已删除的树区块。
- [x] 应用运行页按 `pageKey` 选择 `pages[].gridLayout`，`pageKey=detail&id=...` 不再回到列表页旧渲染。
- [x] 详情页运行态按记录 ID 加载详情数据并传给 `detail-info` 区块，详情画布字段显示真实记录值。
- [x] 运行态只读画布去掉设计网格、设计背景和额外 padding，宽度按父容器 100% 计算，不再强制使用 1366 设计宽。
- [x] 返回上一页组件改为运行态真实返回，直接打开详情页时兜底回到列表查询参数。
- [x] 详情页显式空布局不再被默认详情组件回填，删除默认组件后保存/刷新应保持删除状态。
- [x] 页面组件外观配置拆出内边距/外边距输入，支持 CSS 间距值。
- [x] 筛选树区块增加展开/收起控制，运行态树节点展开状态受控。
- [x] 修复 CRUD 快速配置左树右表预览右侧 CRUD 显示不全和滚动受限问题。
- [x] CRUD 快速配置和自由画布右侧配置组件增加配置项搜索。
- [x] AiCrudPage 增加可视化提交前参数处理规则，并在运行页编译为 `beforeSubmit`。
- [x] 设计态 AiCrudPage 新增/编辑弹窗预览优先使用当前对象完整字段集合。
- [x] 回调参数处理抽成共享编辑器，CRUD 快速配置和自由画布 AiCrudPage 属性统一复用。
- [x] 回调规则扩展为统一 `crudHookRules`，覆盖加载前、搜索前、表单/详情渲染前、提交前和构建提交数据后。
- [x] 统一两处配置搜索样式，并压缩 CRUD 快速配置面板布局，避免输入框溢出。
- [x] 执行定向 eslint 和前端构建。
- [x] 左右页面组件/配置组件收起后释放画布空间，仅保留边缘悬浮展开按钮。
- [x] AiCrudPage 工具栏开关改为逐项“显示/隐藏”配置，并补充按钮作用说明。

## 2026-06-25 页面组件库扩展需求

- [x] 扩展页面组件库：手写签名、富文本框、穿梭框、分步表单、Vue 组件占位、HTML 标签、标题、段落、统计数值、链接、文字提示、水印、音频播放器、视频播放器、头像框、条形码、内嵌页面、二维码、Markdown、盒子布局、间距和描述列表。
- [x] 新增组件统一接入 `listPageBlockCatalog`、`createGridBlock`、`GridBlockRenderer` 和 `ListPageGridDesigner` 右侧属性面板，保持左侧两列组件卡片、中间真实预览、右侧属性/样式/交互的现有风格。
- [x] 每个新增组件保留通用外观 `props.style` 和通用事件 `props.events`，并补充结构化属性配置，避免普通用户手写 JSON。
- [x] 富文本、Markdown、HTML、内嵌页面、Vue 组件和媒体类组件先按安全受控渲染实现，涉及脚本执行、任意组件加载和外链 iframe 的能力默认禁用或白名单化。
- [x] 二维码、条形码、手写签名优先在不新增依赖前提下完成设计态/运行态占位和结构化 schema；如需真实生成或采集，再单独评估依赖和安全边界。
- [x] 栅格布局拖入子组件后自动根据子组件内容和数量撑高，避免内容裁切；用户手动固定高度时不被自动逻辑反复覆盖。
- [x] 执行定向 eslint、`git diff --check` 和前端构建验证。

## 2026-06-25 页面组件真实能力与表单复用修正

- [x] 抽取共享低代码页面组件能力，统一维护通用组件 catalog、默认 props、渲染器和属性编辑模型，列表设计器与表单设计器只做适配。
- [x] Vue 组件改为可配置代码：组件名、模板、脚本、样式、props JSON、示例数据和安全模式；画布中展示代码预览和受控渲染结果。
- [x] Markdown 组件改为源码编辑 + Markdown 解析预览，支持标题、段落、列表、引用、代码块、链接等常见语法。
- [x] 富文本框改为工具栏式富文本编辑器体验，支持加粗、斜体、标题、列表、引用、链接、对齐、清除格式和源码模式。
- [x] HTML 标签组件改为真实标签配置，支持标签名、属性、文本内容、HTML 内容、安全渲染、语义角色和样式类名。
- [x] 右侧属性面板按组件类型补齐丰富配置，避免只提供标题/内容等少量字段。
- [x] 将上述共享组件同步接入表单设计器左侧组件库、画布渲染和属性配置，不能复制粘贴列表设计器实现。
- [x] 执行定向 eslint、`git diff --check` 和前端构建验证。

## 2026-06-25 组件可用性补充修正

- [x] 将水印、穿梭框纳入共享页面组件 catalog/default props，避免列表和表单两套实现分叉。
- [x] 富文本画布编辑支持工具栏命令和内容回写，列表/表单设计器都能保存编辑后的 HTML。
- [x] 穿梭框支持静态选项和远程接口数据源配置，远程配置包含接口、方法、参数、列表路径、label/value/disabled 字段映射。
- [x] Vue 组件支持安全模板预览、Props 模板预览和代码视图；当前项目使用 Vue runtime-only 构建，设计器不在浏览器运行时编译或执行 template/script。
- [x] 表单设计器补齐水印、穿梭框属性入口，并将这些组件标记为虚拟组件，避免参与业务字段注册和 DDL。
- [x] 表单设计器中的穿梭框按字段组件接入，支持绑定业务字段、静态选项和远程接口选项，不再只有虚拟页面组件能力。
- [x] 执行定向 eslint、`git diff --check` 和前端构建验证。

## 2026-06-25 外部组件接入修正

- [x] 富文本组件改为使用 `@wangeditor/editor` + `@wangeditor/editor-for-vue`，移除手写 `contenteditable/document.execCommand` 编辑逻辑。
- [x] Markdown 组件改为使用 `@kangc/v-md-editor`，移除手写 Markdown 解析预览逻辑。
- [x] 修复 `@kangc/v-md-editor` vuepress 主题缺少 Prism 初始化导致 `Cannot read properties of undefined (reading 'languages')` 的运行时异常。
- [x] 水印组件按 Naive UI `n-watermark` API 接入并补齐列表/表单属性配置，覆盖文本、字体、图片、旋转、间隔、偏移、全屏和跨边界等参数。
- [x] 二维码/条形码改为外部组件渲染；公开 registry 中 `vue3-barcode-qrcode` 返回 404，本轮使用 `qrcode-vue3` 和 `vue3-barcode` 落地，未再手写生成逻辑。
- [x] 二维码/条形码纳入共享页面组件 catalog/default props/renderer，并标记为表单虚拟组件，列表设计器和表单设计器预览都走同一套外部组件渲染。
- [x] `AiFormItem` 运行态补齐虚拟页面组件统一分流，预览弹窗和应用页不再把二维码、条形码、富文本、Markdown、水印、HTML、Vue 组件等展示组件兜底渲染为 `NInput`。
- [x] 修复 `vue3-barcode` 多实例 querySelector 导致预览弹窗条形码不显示的问题，改为用外部 `jsbarcode` 渲染当前组件 SVG。
- [x] 列表组件库移除旧二维码/条形码 catalog/default props，只保留共享页面组件来源，避免左侧组件重复。
- [x] 修复列表预览弹窗 `width: max-content` 和 `overflow: visible` 导致富文本/Markdown 预览横向无限撑宽的问题。
- [x] 富文本和 Markdown 在列表预览弹窗中不再被只读预览态锁死，可按组件自身 `readonly` 属性决定是否编辑。
- [x] 共享页面组件新增 Naive UI 组件：日历、代码、倒计时、描述、公示、列表、日志、数值动画、面包屑、菜单、分页和面板分隔，列表设计器与表单设计器共用同一套 catalog/default props/renderer。
- [x] 表单设计器将上述 Naive 页面组件全部标记为虚拟组件，避免参与业务字段注册和 DDL。
- [x] 执行定向 eslint、`git diff --check` 和前端构建验证。

## 2026-06-25 组件面板体验补充

- [x] 表单设计器左侧组件库按组件语义分配图标，二维码、条形码、富文本、Markdown、日历、菜单、倒计时等新增组件不再大量复用同一个图标。
- [x] 列表设计器左侧页面组件面板增加组件统计，展示总数、搜索匹配数和分组数。
- [x] 列表设计器组件分组标题补充分组内组件数量，搜索过滤后能直观看到每组剩余数量。
- [x] 执行定向 eslint 和 `git diff --check` 验证。

## 2026-06-25 右侧配置面板样式统一

- [x] 列表设计器右侧配置面板调整为“标题头部、搜索、配置类型 Tab、滚动内容”的顺序，和表单设计器右侧面板保持一致。
- [x] 统一列表/表单右侧面板的头部高度、背景、分割线、收起按钮、Tab、折叠区块、表单项和输入控件视觉密度。
- [x] 记录后续能力缺口：Naive UI 新增组件需要补齐官方 Props/Slots/Events 的结构化配置，并支持从页面详情接口、已有列表接口或自定义接口取数，避免只靠手写 JSON 或写死示例数据。
- [x] 执行定向 eslint 和 `git diff --check` 验证。

## 2026-06-25 左侧组件面板样式统一

- [x] 列表设计器左侧页面组件卡片补充语义图标，和表单设计器左侧组件卡片保持“图标 + 主体信息”的展示结构。
- [x] 表单设计器左侧组件面板补充统计胶囊，展示组件总数、搜索匹配数、分组数或字段资产数量。
- [x] 统一列表/表单左侧面板的头部、搜索框、分组标题、计数胶囊、分段 Tab、组件卡片和字段列表视觉密度。
- [x] 执行定向 eslint 和 `git diff --check` 验证。

## 2026-06-25 新增组件可配置能力补齐

- [x] 列表设计器左侧页面组件图标进一步按组件语义细分，Naive UI 新增组件不再大量走同一个兜底图标。
- [x] 共享页面组件默认 props 增加 `dataBinding`，支持静态配置、当前详情/表单上下文和远程接口三类数据来源。
- [x] 共享 `PageWidgetRenderer` 支持按 `dataBinding` 渲染描述、列表、日志、代码、数值动画、面包屑、菜单、分页、公示、日历和倒计时等组件数据。
- [x] 列表设计器和表单设计器右侧属性面板补充新增组件的数据来源配置，支持接口地址、请求方法、请求参数、响应路径和字段映射。
- [x] 列表/表单右侧“数据来源”配置改为说明 + 条件字段 + 中文字段映射，修复下拉和输入框在窄面板里展示不全、含义不清的问题。
- [x] `detail-info` 详情信息区块补充数据来源配置，支持当前详情数据、当前详情字段路径和远程详情接口，并在运行态按配置取值。
- [ ] 后续逐组件补齐 Naive UI 官方 Props/Slots/Events 的完整结构化配置；本轮优先补齐业务可用的数据来源链路。
- [x] 执行定向 eslint、`git diff --check` 和前端构建验证。

## 2026-06-25 运行规则与字段展示统一封装

- [x] 新增共享 `runtime-rules.js`，统一解析组件/字段/区块的运行规则，覆盖显示隐藏、只读、禁用、必填、文字颜色、背景色、样式类和内联样式。
- [x] 新增共享 `RuntimeRulesEditor.vue`，列表设计器和表单设计器右侧属性面板复用同一套运行规则配置入口，避免两边重复造配置。
- [x] 新增共享 `FieldValueRenderer.vue`，统一承接文本、字典标签、状态 Tag、链接、金额和颜色规则渲染，列表列、详情字段和表单只读展示不再各写一套。
- [x] `AiForm`、`AiFormLayoutNodes`、`AiFormItem` 接入运行规则，上下文包含当前表单数据、当前记录和外部 context，支持表单新增/编辑中按状态或动态数据控制字段/模块显示隐藏、只读和禁用。
- [x] `GridBlockRenderer` 接入运行规则和统一字段渲染，支持详情页字段、详情区块和列表画布区块按运行数据控制展示，并支持字典/Tag/颜色等复杂显示。
- [x] 应用运行页 `crud-page.vue` 接入统一字段渲染，发布后的列表列继续支持字典标签、颜色、链接跳转和旧 render 配置兼容。
- [x] 列表设计器和表单设计器“数据来源”中的“请求方式”“响应路径”改为各占一行，减少窄面板下输入项展示不全的问题。
- [x] 运行规则配置补充“数据来源”，支持当前记录/详情、当前行数据、当前表单数据、URL 查询参数、URL 路由参数和当前用户。
- [x] 表单新增/编辑/详情弹窗上下文补齐 `currentRow` 到 `row`，列表操作点开弹窗后可按当前行字段控制显示隐藏、只读、禁用和样式。
- [x] 列表画布、详情画布、发布运行页和表单运行态统一注入 `route.query/route.params`，进入详情页后可按 URL 参数控制组件规则。
- [x] 执行定向 eslint、`git diff --check` 和前端构建验证。

## 2026-06-26 CRUD 字段快捷配置补充

- [x] 在列表设计器 CRUD 组件右侧属性面板新增“字段快捷配置”折叠区。
- [x] 字段快捷配置按当前对象字段展示字段名和字段编码。
- [x] 字段快捷配置补充“搜索”“导入”“导出”三类开关，方便在 CRUD 组件内直接配置查询条件和 Excel 字段范围。
- [x] 快捷配置写入当前 CRUD 区块 `props.fieldSettings`，并同步维护 `searchFields`、`importFields`、`exportFields` 字段列表，避免只改 UI 不落 schema。
- [x] 命令通道恢复后已补跑定向 eslint、`git diff --check` 和前端构建验证。

## 2026-06-26 App Center 首屏资源优化

- [x] `app-center/index.vue` 中对象设计页改为 `defineAsyncComponent`，避免进入应用中心首页时同步加载完整对象设计器。
- [x] `suite.[suiteCode].vue`、`object.[objectCode].vue` 中对象设计页和非首屏抽屉/面板改为异步组件。
- [x] `object-designer.[objectCode].vue` 中字段、表单、列表、关系、文书、流程、权限、发布、开发者配置等设计面板改为按 active panel 懒加载，只保留设计器 shell 同步加载。
- [x] 新增 `DesignerAsyncLoader.vue`，对象设计器异步面板加载时展示类似 Figma 的细分进度条和加载提示。
- [x] 表单设计、列表设计等对象设计器面板统一使用异步组件 loading 配置，首次进入不再出现无反馈白屏。
- [x] `BusinessObjectDesignerShell.vue` 顶层配置加载遮罩也统一使用 `DesignerAsyncLoader`，避免原 `n-spin` 覆盖在上方导致进度条被挤到下面不可见。
- [x] 顶层配置加载期间不渲染下面的 panel slot，避免配置 loading 和表单/列表异步面板 loading 同时出现两个进度条。
- [x] `PageWidgetRenderer.vue` 中 WangEditor、VMdEditor、Prism/vuepress theme、QRCode、JsBarcode 由同步 import 改为按组件类型动态加载。
- [x] 富文本、Markdown、二维码、条形码相关样式和依赖拆为独立异步 chunk，减少 app-center 首屏同步资源。
- [x] 执行定向 eslint、`git diff --check` 和前端构建验证。

## 2026-06-26 列表画布交互修正

- [x] 列表设计器 `AiCrudPage` 区块预览改为严格服从外层区块高度，内部内容滚动，不再因 CRUD 内部最小高度把区块底部撑出一截。
- [x] 降低 `AiCrudPage` 区块最小高度归一化限制，用户在右侧改高度或拖拽缩放后不再被自动拉回大高度。
- [x] 列表画布拖拽时命中非容器组件不再显示投放预览，释放时直接拒绝投放，避免普通组件被误当成可嵌套容器。
- [x] 命中非容器组件时增加红色不可投放提示，明确说明“该组件不支持嵌套”，避免用户误以为拖拽失效。
- [x] 画布内已有组件拖拽命中非容器组件时同样显示不可投放提示，并在释放时取消移动。
- [x] 列表设计器中间画布改为浅灰蓝设计底色和细网格，区块默认显示浅边界，白色组件放上去更容易看清范围；只读/运行态仍保持白底。
- [x] 弱化中间画布标线：移除密集网格和横向行线，仅保留极淡列参考线，降低视觉噪音。
- [x] 新增组件或嵌套组件移出到画布时，如果目标框会压住已有区块，自动落到画布底部空白处，并同步最终 `gridX/gridY/gridW/gridH` 和 style 坐标。
- [x] 执行定向 eslint、`git diff --check` 和前端构建验证。

## 验证记录

- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过，有既有 `vue/one-component-per-file` warning。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/views/app-center/components/designer/BusinessObjectDesignerShell.vue 'src/views/app-center/object-designer.[objectCode].vue'`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue'`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/page-schema.js`：通过。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/page-schema.js`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/page-schema.js`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/views/app-center/components/designer/BusinessObjectDesignerShell.vue 'src/views/app-center/object-designer.[objectCode].vue'`：通过，有既有 `vue/one-component-per-file` warning。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。

## 2026-06-26 组件默认外观与宽度策略修正

- [x] 页面组件默认外观不再自带白底、边框和圆角，右侧样式面板显式配置后才展示对应背景、边框、圆角。
- [x] 共享 Naive 页面组件 shell 改为透明无边框，避免内部白底覆盖区块样式面板配置的背景色。
- [x] 富文本、Markdown、HTML、Vue 组件、水印、面板分隔、二维码、条形码等预览容器去掉默认装饰，条形码/二维码图形背景默认透明。
- [x] 列表设计器新增区块默认宽度策略统一：大多数组件默认填充宽度，按钮、链接、标签、面包屑、分页、二维码、条形码、头像等内容型组件默认内容宽度。
- [x] 列表和表单右侧外观面板的背景色默认展示改为透明，清空输入写回 `transparent`，色块用棋盘格提示透明状态。

## 2026-06-26 CRUD 自定义按钮配置体验优化

- [x] 列表设计器 AiCrudPage 右侧“自定义操作”改为飞书风格列表管理：工具栏自定义按钮和列表行自定义按钮分组展示。
- [x] 自定义按钮支持右侧面板内拖拽排序、行内修改按钮文字、设置图标进入完整配置弹窗。
- [x] 自定义按钮更多菜单支持复制、删除、工具栏/行按钮位置切换。
- [x] 单独 toolbar 区块的自定义按钮配置同步改为可拖拽列表管理，避免和 AiCrudPage 体验分叉。
- [x] 移除基础配置里的重复“自定义操作”管理入口，避免同一能力在多个区域重复出现。
- [x] 修复自定义操作只剩一个按钮时删除要点两次的问题，删到空列表时 activeIndex 置空并关闭配置弹窗。
- [x] 将列表行自定义按钮移动到“查询与列表字段”，工具栏配置区只保留工具栏自定义按钮。
- [x] 修复列表行自定义按钮新增后删除需要点两次的问题，自定义按钮删除、排序、复制统一改为按稳定 `clientKey` 定位。
- [x] 修复外层 `BusinessListDesigner` 回灌 `designerActions` 时丢失 `clientKey`，避免第一个/唯一行按钮第一次删除后被旧数据恢复。
- [x] 自定义按钮更新时同步写回 `listGridLayout`、`pages[].gridLayout` 和 `zones.table.props.customActions`，避免 schema 兜底采集把已删除按钮复活。
- [x] 参考飞书多维表格字段面板，把 AiCrudPage“字段配置”和“列表行按钮”合并为“字段与操作”紧凑面板，支持字段显隐、拖拽排序、快速设置和行按钮管理。
- [x] 字段显隐改为眼睛图标，隐藏字段保留在列表中显示为不可见状态；行按钮改为纯列表行，不再在行内放输入框，提升操作分组层次。

## 2026-06-20 补充处理

- [x] 左侧筛选树新增区块级折叠：折叠后树模块变为 44px 侧栏，右侧 CRUD 同行区块自动左移并补满释放宽度。
- [x] 树节点展开/收起保留在树标题内，整块树折叠改为边缘箭头按钮，避免交互含义混淆。
- [x] 修复 CRUD 快速配置搜索区域行高占位、背景和固定展示；自由画布配置搜索保持独立固定区域。
- [x] 预览弹窗内容区、画布滚动区和内部网格统一白底并撑满高度。
- [x] 回调参数处理编辑器改为单列规则输入，降低右侧窄面板溢出风险。
- [x] 修复回调规则新增按钮无反馈：编辑态保留新建空规则行，运行态仍只执行有效规则。
- [x] 列表设计器编辑态和预览弹窗补齐回调规则编译，和发布页使用同一套参数处理逻辑。
- [x] 新增默认参数配置模块，覆盖列表请求参数、URL 公共参数、表单默认值和提交固定参数。
- [x] 自由画布配置组件与 CRUD 快速配置统一复用默认参数编辑器，并同步到预览和发布运行页。
- [x] 执行定向 eslint、空白检查和前端构建。
- [x] 移除全局树选中态左侧指示条，修复树节点内容过短时居中显示的问题。
- [x] 重新整理筛选树区块的整块收起按钮和节点层级展开/收起按钮位置。
- [x] 将 CRUD “添加下级”改为显式 `enableTreeAddChild` 配置，左树右表默认不再自动显示。
- [x] 自由画布新增“专注画布”模式，一键隐藏左右面板释放中间编辑空间。
- [x] 画布拖动新增区块和移动/缩放区块时支持靠近边缘自动滚动，并把滚动距离计入拖拽位移。
- [x] 简化筛选树收起态展示，只保留轻量竖向“树”标识，完整标题改为 hover title。
- [x] 页面组件卡片取消原生 disabled 死状态：已存在的唯一组件点击后定位选中画布已有块，布局不支持时显示明确原因。
- [x] 修复专注画布后左右展开 rail 仍参与布局、画布 stage 贴左导致内容拥挤的问题。
- [x] 修复专注画布复用左右收起状态导致小屏下页面组件/配置组件跑到上下位置的问题。
- [x] 单按钮组件配置拆分基础样式和按钮状态，补充开关说明并修复预览写死禁用导致点击无反馈的问题。
- [x] 移除列表设计工具栏内重复的“保存列表”按钮，统一使用右上角“保存”入口。

## 2026-06-21 补充处理

- [x] 列表自由画布顶部只保留源码、专注、清空、重置等主操作。
- [x] 将设计宽度、预览形态和缩放控制迁移到画布内浮动视图控制区，减少中间画布顶部按钮拥挤。
- [x] 列表自由画布支持 `Ctrl/⌘ + 滚轮` 缩放，并在工具栏信息区显示当前预览形态、设计宽度和缩放比例。
- [x] 将视图控制改为 Figma 式底部图标控件，点击图标弹出宽度、预览形态和缩放输入。
- [x] 自由画布缩放容器改为居中布局，拖放、缩放和切换宽度时画布主体保持在中间。
- [x] 压缩 `list-designer-head` 和 `list-page-switch`，撤销/重做、页面操作改为图标按钮，页面切换区压成单行。
- [x] 画布摘要去掉行列信息，只保留区块数、设计宽度、预览形态和缩放比例。
- [x] 优化页面设置展开区，`page-config-row` 和 `page-param-row` 改为有间距的轻量面板，参数删除改为图标按钮。

## 2026-06-20 验证记录

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/CrudHookRulesEditor.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/CrudHookRulesEditor.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/page-schema.js src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/CrudHookRulesEditor.vue src/components/lowcode-builder/page/crud-hook-rules.js src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/page-schema.js src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/CrudHookRulesEditor.vue forge-admin-ui/src/components/lowcode-builder/page/crud-hook-rules.js forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/views/ai/crud-page.vue`：通过；`nvm use v20.19.0` 本机返回 N/A，实际使用当前可用 pnpm 执行。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/page-schema.js`：通过；`nvm use v20.19.0` 本机返回 N/A，实际使用当前 Node `v20.20.0`。
- `git diff --check -- forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodePageSchema.java`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/views/ai/crud-page.vue src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：首次使用默认 JDK 失败于上游 `forge-starter-file`，原因是当前 JDK 不满足 Java 17 编译。
- `JAVA_HOME=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home PATH=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：通过。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue 'src/views/app-center/object-designer.[objectCode].vue' src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue' forge-admin-ui/src/views/ai/crud-page.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `JAVA_HOME=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home PATH=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：通过。
- `pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java`：通过。
- `JAVA_HOME=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home PATH=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/page-schema.js src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodePageSchema.java`：通过。
- `JAVA_HOME=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home PATH=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/page-schema.js`：通过，有既有 `vue/one-component-per-file` warning。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/page-schema.js src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过，有既有 `vue/one-component-per-file` warning。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/page-schema.js src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过，有既有 `vue/one-component-per-file` warning；本机 `nvm use v20.19.0` 返回 N/A，但当前 shell 仍可执行 pnpm。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/GridBlockRenderer.vue`：通过，有既有 `vue/one-component-per-file` warning。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/page-schema.js`：通过，有既有 `vue/one-component-per-file` warning。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/ai-form/AiCrudPage.vue src/components/ai-form/AiCrudPageProps.js src/views/ai/crud-page.vue`：通过；本机无精确 `v20.19.0`，实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/ai-form/AiCrudPage.vue forge-admin-ui/src/components/ai-form/AiCrudPageProps.js forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/styles/global.css`：通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过；实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过；实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过；实际使用 `v20.19.5`。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/page-schema.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。保留项目既有 warning：CSS 中存在 `//` 注释、`src/store/index.js` 同时动态/静态导入导致 chunk 提示。
