# 表格横向拖拽与表头固定优化
> status: apply
> created: 2026-07-04
> complexity: 🟡中等

## 1. 背景与目标

现有后台表格横向滚动依赖底部滚动条，数据较少或表格未占满可视高度时仍需移动到表格底部才能左右查看列；纵向滚动时表头会离开可视区域，查看多列多行数据时字段含义容易混淆。

目标是在不逐页改造业务表格的前提下，为 Forge 管理端 Naive UI 表格提供通用体验增强：

- 鼠标悬浮表格区域后，可在表格内容区域按住横向拖拽滚动。
- 表格纵向滚动时，表头固定在表格可视区域顶部。
- 优先覆盖 `AiCrudPage` / `AiTable`，并兼容直接使用 `n-data-table` 的页面。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` 使用 `AiTable` 渲染低代码和通用 CRUD 列表，向下传递 `max-height` 与 `scroll-x`。
- `forge-admin-ui/src/components/ai-form/AiTable.vue` 是 `n-data-table` 的主要封装点，负责工具栏、列配置、分页和表格/卡片模式切换。
- `forge-admin-ui/src/directives/index.js` 已经统一注册 `copy/loading/preview/watermark` 等全局指令，可接入表格增强指令。
- `forge-admin-ui/src/styles/theme.css` 和 `forge-admin-ui/src/styles/global.css` 已有全局表格视觉样式，可补充粘性表头和拖拽状态样式。

### 2.2 现有实现

- `AiTable.vue` 的 `n-data-table` 当前没有横向拖拽逻辑，只依赖浏览器/Naive UI 默认滚动条。
- `AiCrudPage.vue` 仅在局部样式中让 `.n-data-table-wrapper` `overflow: auto`，没有表头粘性策略。
- 部分流程、系统、数据页面直接使用 `n-data-table`，仅改 `AiTable` 不能覆盖全部场景。

### 2.3 发现与风险

- Naive UI 表格内部 DOM 存在固定列、表体滚动容器、分页和工具栏，拖拽事件必须避开输入框、按钮、链接、下拉等交互元素。
- 表头粘性应限制在表格滚动容器内，不能固定到整个页面顶部覆盖导航。
- 全局样式要兼容暗色模式、固定列和已有局部表格样式。

## 3. 功能点

- [x] **横向拖拽滚动**：鼠标在表格 body/header 区域按下并横向拖动，驱动表格滚动容器 `scrollLeft`，不需要操作底部滚动条。
- [x] **表头固定悬浮**：表头单元格在表格垂直滚动容器中 `sticky top: 0`，始终停留在当前表格顶部。
- [x] **交互保护**：点击按钮、链接、输入、选择器、复选框、展开图标等元素时不触发拖拽滚动。
- [x] **通用覆盖**：`AiTable` 显式绑定增强指令；直接使用 `n-data-table` 的页面通过全局注册后的指令可复用。

## 4. 业务规则

- 仅调整表格交互，不改变数据查询、分页、排序、列配置和权限逻辑。
- 表格横向拖拽只响应鼠标主键，不拦截触控滚动和键盘导航。
- 拖拽距离超过阈值后才阻止点击，避免误伤行内操作。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
| --- | --- | --- | --- |
| 无 | 无 | 无 | 纯前端交互优化 |

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
| --- | --- | --- | --- |
| 无 | 无 | 无 | 纯前端交互优化 |

## 7. 影响范围

- `forge-admin-ui/src/components/ai-form/AiTable.vue`
- `forge-admin-ui/src/directives/index.js`
- `forge-admin-ui/src/directives/modules/tableScrollEnhance.js`
- `forge-admin-ui/src/styles/theme.css`

## 8. 风险与关注点

- 固定列场景下 z-index 需要高于普通表头但低于弹窗浮层。
- 拖拽状态不能影响表格行内按钮、链接、输入框、复选框、下拉等交互。
- 表头 sticky 需要兼容表格内部横向滚动和最大高度滚动。

## 8.5 测试策略

- **测试范围**：静态校验、前端构建、浏览器交互抽查。
- **覆盖率目标**：至少覆盖 `AiCrudPage` 列表页和一个直接 `n-data-table` 页面。
- **独立 Test Spec**：是。

## 9. 待澄清

- 无。

## 10. 技术决策

- 使用 Vue 指令封装横向拖拽能力，避免在每个页面复制事件处理。
- `AiTable` 默认启用该指令，保持主链路自动受益。
- 表头固定采用全局 CSS 约束在 `.n-data-table` 内，减少逐组件配置。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
| --- | --- | --- | --- |
| Task 0 | completed | `spec.md`, `tasks.md`, `test-spec.md`, `execution-log.md` | 建立变更记录 |
| Task 1 | completed | `tableScrollEnhance.js`, `directives/index.js` | 新增横向拖拽滚动指令，并全局自动增强直接使用的 `n-data-table` |
| Task 2 | completed | `AiTable.vue` | `AiTable` 默认启用拖拽滚动，保留 `dragScroll` 关闭入口 |
| Task 3 | completed | `theme.css` | 补充表头 sticky、固定列表头层级和拖拽光标样式 |
| Task 4 | completed | `test-spec.md`, `execution-log.md` | 已完成构建、指令烟测和 diff 空白检查 |
| 回归修复 | completed | `AiCrudPage.vue`, `AiTable.vue`, `tableScrollEnhance.js`, `theme.css` | 少量数据不启用默认内部纵向滚动；挂载/更新后刷新固定列状态；无纵向溢出时隐藏竖向 scrollbar rail；操作列默认右固定 |

## 12. 审查结论

已完成实现与回归修复验证。`pnpm --dir forge-admin-ui build` 成功；jsdom 指令烟测验证横向拖拽会同步 body/header，按钮区域不会触发拖拽；回归烟测验证无纵向溢出时隐藏竖向滚动 rail、纵向溢出时恢复；`git diff --check` 通过。构建中存在既有动态导入、CSS 注释和 chunk size 类 warning，未阻断本次变更。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-04
- **确认人**：用户
- **确认内容**：用户提出表格横向滚动操作繁琐、表头无固定悬浮的问题，并要求优化横向拖拽滚动和表头固定悬浮。
