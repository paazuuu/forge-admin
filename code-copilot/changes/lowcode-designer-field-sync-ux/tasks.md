# 任务清单：lowcode-designer-field-sync-ux
> status: completed
> created: 2026-06-13
> spec: `code-copilot/changes/lowcode-designer-field-sync-ux/spec.md`

## 任务总览

| Task | 名称 | 状态 | 优先级 |
|------|------|------|--------|
| Task 1 | 允许表单设计器字段 ID 手动输入 | completed | P0 |
| Task 2 | 补齐字段资产与表单设计器双向同步 | completed | P0 |
| Task 3 | 修复无修改时左侧导航切换未保存提示 | completed | P0 |
| Task 4 | 字段配置抽屉改为内嵌属性面板 | completed | P0 |
| Task 5 | 前端校验与执行日志回填 | completed | P0 |
| Task 6 | 修复默认字段编码 input 与已有字段名称回显 | completed | P0 |

## Task 1：允许表单设计器字段 ID 手动输入

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js`

**执行要点**

- 调整基础属性和业务组件属性中的“组件字段ID”规则。
- 下拉保留已有字段选项，同时允许用户输入新字段 ID。
- 占位提示改为“选择已有字段或输入新字段ID”。

**验收标准**

- fcDesigner 属性面板中的字段 ID 可以输入新值。
- 已有字段仍可搜索选择。

## Task 2：补齐字段资产与表单设计器双向同步

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/fieldReferenceUtils.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/viewSchema.js`
- `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`

**执行要点**

- 新增前端字段引用重命名工具，支持替换表单设计 Schema 内的 `fieldBinding.fieldCode`、`fieldBinding.columnName`、组件 `props.fieldCode`、`props.fieldBinding.fieldCode`。
- 新增视图 Schema 字段编码替换工具，覆盖查询、列表、详情字段引用。
- 字段资产保存后识别旧字段编码和新字段编码；发生变化时向对象设计器上报同步信息。
- 对象设计器在本地先同步 `draft.formDesignerSchema`、`draft.pageSchema`、`draft.viewSchema`、`draft.displayField`，随后仍按现有逻辑 reload 服务端设计器。

**验收标准**

- 字段资产修改字段编码后，本地表单绑定和视图引用立即使用新字段编码。
- 保存后重新加载设计器不恢复旧字段编码。

## Task 3：修复无修改时左侧导航切换未保存提示

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormCreateDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`

**执行要点**

- `flushDesigner()` 比较转换后的 schema 与当前 `modelValue`，相同则不触发 dirty。
- `syncDesignerDraft()` 比较字段资产、页面 Schema 和表单 Schema；没有实际差异时返回 `dirty: false`。
- 保留真实拖拽、删除、排序、属性编辑后的 dirty 上报。

**验收标准**

- 进入设计器后不做任何修改，切换左侧导航不提示未保存。
- 修改表单组件后切换导航仍能产生未保存状态。

## Task 4：字段配置抽屉改为内嵌属性面板

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue`

**执行要点**

- 删除 `n-drawer` 包裹。
- 增加 `field-asset-workbench` 左右分栏。
- 右侧直接渲染 `BusinessFieldPropertyPanel`，未选中时展示面板内空状态。
- 小屏幕下分栏改为纵向布局。

**验收标准**

- 字段资产页点击字段后不再弹出抽屉。
- 属性编辑区与字段列表同时可见。

## Task 5：前端校验与执行日志回填

**涉及文件**

- `code-copilot/changes/lowcode-designer-field-sync-ux/execution-log.md`
- `code-copilot/rules/automated-testing-standard.md`

**执行要点**

- 按自动化测试标准读取验证规则。
- 执行本次前端相关的 lint/build 或可用的等价校验。
- 记录命令、结果、警告、跳过项和服务清理情况。

**验收标准**

- 执行日志包含本次校验命令和结果。
- 若无法完成全量校验，说明原因和残余风险。

## Task 6：修复默认字段编码 input 与已有字段名称回显

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/form-first/formCreateToForge.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeToFormCreate.js`

**执行要点**

- 转换 form-create rule 时忽略 `input`、`select`、`dictSelect` 等设计器默认字段名，不再作为业务字段编码或数据库列名。
- 对设计器默认标题“输入框”等生成 `fieldInput1` 形式的稳定字段编码，并避免复用 `input` 作为组件 ID。
- 绑定已有字段资产时，组件标题优先使用字段资产 `fieldName` / `label`。
- 从 Forge Schema 回填到 form-create rule 时，旧组件标题为通用标题或字段编码时使用字段资产名称。

**验收标准**

- 拖入默认输入框后保存，不再生成 `columnName = input` 的新字段。
- 选择已有字段 `customerLevel` 后，表单组件标题同步为字段资产名称。
