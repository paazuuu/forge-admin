# 低代码对象设计器字段同步与交互修复
> status: implemented
> created: 2026-06-13
> scope: `forge-admin-ui`
> related: `code-copilot/changes/form-first-business-object-designer/spec.md`

## 1. 背景

低代码应用构建的业务对象设计器已经采用表单优先模式，表单画布负责生成字段资产，字段资产负责高级维护。当前实际使用中存在三个影响设计效率的问题：

1. 表单设计器新增字段时，“组件字段ID”只能从已有字段中选择，不能直接输入新字段 ID，导致用户无法在拖入组件时明确字段编码。
2. 表单设计与字段资产的双向同步不完整：表单新增字段要沉淀为字段资产，字段资产改名或改字段编码后也应同步回表单设计器的绑定与展示。
3. 切换左侧导航时经常提示未保存，即使用户没有做业务修改，说明初始化、预览补水或内部同步触发了错误的 dirty 状态。
4. 字段配置当前使用右侧抽屉弹层，编辑字段时遮挡上下文，且频繁开关影响效率。

## 2. 目标

- 表单设计器中的“组件字段ID”支持选择已有字段，也支持手动输入新字段 ID。
- 手动输入的新字段 ID 在保存/同步设计草稿时自动沉淀到字段资产，字段名称、列名、类型、组件属性沿用现有自动字段资产规则。
- 字段资产保存后同步更新本地模型、表单 Schema、页面 Schema 和视图 Schema 中的字段引用；服务端保存后的重载仍作为最终一致性来源。
- 表单设计器仅在真实 schema/page/field 变更时上报 dirty，初始化、setRule、预览选项补水不触发未保存提示。
- 字段资产页改为列表 + 右侧内嵌属性面板，不再打开 Naive UI Drawer。

## 3. 非目标

- 不修改后端字段设计器接口协议。
- 不重写 `fcDesigner`。
- 不改变发布链路、运行态 `AiCrudPage` 或动态 CRUD 行为。
- 不新增数据库迁移脚本。
- 不处理字段编码变更后的历史数据迁移策略，仍沿用当前后端字段设计服务的处理。

## 4. 设计方案

### 4.1 字段 ID 输入

`forgeBusinessComponents.js` 中的字段绑定属性规则继续使用 `select` 类型，但开启可创建输入能力。用户可以：

- 从已有字段资产中选择字段 ID。
- 直接输入新字段 ID。

`formCreateToForge.js` 已经会从 `rule.props.fieldBinding.fieldCode`、`rule.props.fieldCode`、`rule.field`、`rule.name` 中解析字段编码；本变更保持该转换链路，只补齐属性规则的输入能力。

### 4.2 双向同步

表单到字段资产：

- `BusinessFormDesigner.syncDesignerDraft()` 继续通过 `buildAutoFieldAssets()` 从 `FormDesignerSchema` 自动生成字段资产。
- 手动输入字段 ID 后，组件 `fieldBinding.fieldCode` 作为稳定字段编码，自动生成字段资产。

字段资产到表单：

- 字段资产保存时，如果字段编码发生变化，前端在本地同步替换表单 Schema、页面 Schema、视图 Schema 的引用。
- 字段名称、提示文案、必填、控件类型等仍由现有字段保存和设计器重载兜底同步。

### 4.3 Dirty 状态

`BusinessFormCreateDesigner.flushDesigner()` 增加结构比较：

- 转换出的 Forge Schema 与当前 `modelValue` 等价时，只刷新内部快照，不触发 `update:modelValue` 和 `dirtyChange(true)`。
- 仅用户真实拖拽、删除、排序、改字段绑定或修改属性后才上报 dirty。

`BusinessFormDesigner.syncDesignerDraft()` 增加比较：

- 当前表单 Schema、字段资产和页面 Schema 均未变化时返回 `dirty: false`。
- 左侧导航切换只同步草稿，不制造未保存状态。

### 4.4 字段配置交互

`BusinessFieldManager.vue` 去掉抽屉，改为内嵌工作区：

- 左侧保持字段列表和筛选工具栏。
- 右侧固定展示 `BusinessFieldPropertyPanel`。
- 未选中字段时在右侧展示空状态。
- 小屏幕下列表与属性面板纵向堆叠。

## 5. 影响范围

- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormCreateDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/fieldReferenceUtils.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeToFormCreate.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/formCreateToForge.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/viewSchema.js`
- `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`

## 6. 验收标准

- 在表单设计器拖入输入组件后，可以在属性面板直接输入 `customerLevel` 作为组件字段 ID。
- 保存表单设计后，字段资产中出现 `customerLevel`，字段编码与数据库列名分别为 `customerLevel`、`customer_level`。
- 在字段资产中把字段编码从 `customerLevel` 改为 `customerGrade` 后，表单设计器组件绑定同步为 `customerGrade`，页面/视图引用不遗留旧字段编码。
- 只进入设计器并切换左侧导航，不弹出“未保存变更”提示。
- 字段资产页选择字段后直接在页面右侧编辑属性，不出现右侧抽屉弹层。
- 拖入 form-create 默认输入组件时，`rule.field = input`、`select` 等设计器内部默认值不能沉淀为业务字段编码或数据库列名；新字段应生成 `fieldInput1` 等稳定业务字段编码。
- 组件绑定已有字段资产后，表单组件标题优先回显字段资产名称，例如选择 `customerLevel` 时标题同步为“客户等级”。
- 前端构建或 ESLint 校验通过；如因既有问题无法全量通过，需在执行日志记录失败点和本次变更无关性。
