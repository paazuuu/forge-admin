# 单测 Spec — 表格横向拖拽与表头固定优化
> status: apply
> created: 2026-07-04

## 0. 测试原则

- 本变更是前端交互增强，优先执行 lint/build 与浏览器交互验证。
- 执行验证前读取 `code-copilot/rules/automated-testing-standard.md`。
- 记录命令、结果、警告和跳过项到 `execution-log.md`。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| 前端框架 | Vue 3 + Vite |
| 组件库 | Naive UI |
| 自动化方式 | pnpm build / 可选 Playwright |

## 2. 覆盖范围

### P0 — 核心交互

| 场景 | 输入 | 预期结果 |
|------|------|----------|
| 表格 body 横向拖动 | 按住表格空白/单元格横向移动 | 表格 `scrollLeft` 改变 |
| 行内按钮点击 | 点击按钮/链接/输入/复选框 | 不触发拖拽，不阻断原交互 |
| 表头粘性 | 表格垂直滚动 | 表头停留在表格可视区域顶部 |

### P1 — 回归

| 场景 | 预期结果 |
|------|----------|
| AiCrudPage 列表 | 表格正常加载、分页正常 |
| 直接 n-data-table 页面 | 表格视觉不破坏 |
| 暗色模式 | 表头背景不透明且可读 |

### 不测试

- 后端接口和数据库：本变更不涉及。

## 3. 执行计划

- [x] Step 1: 读取自动化测试标准。
- [x] Step 2: 执行前端构建或 lint。
- [x] Step 3: 项目未安装 Playwright，Node REPL 浏览器工具不可用；本轮使用 jsdom 指令烟测覆盖核心拖拽交互。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 无 | 新变更 | - | - | - |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-04 | 表格拖拽指令、AiTable 接入、全局表头样式 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过 | 存在既有组件命名冲突、动态导入、CSS `//` 注释、chunk size warning，未阻断 |
| 2026-07-04 | 表格拖拽指令 | 指令烟测 | `node --input-type=module -e <jsdom smoke>` | 通过 | 项目未安装 Playwright；使用 jsdom 验证拖拽同步和交互元素保护 |
| 2026-07-04 | 本轮相关文件 | 空白检查 | `git diff --check -- <本轮相关文件>` | 通过 | 无 |
| 2026-07-04 | 操作列初始可见、少量数据隐藏竖向滚动条回归修复 | 指令回归烟测 | `node --input-type=module -e <jsdom regression smoke>` | 通过 | 验证横拖同步、行内按钮保护、无纵向溢出 class、纵向溢出 class 移除 |
| 2026-07-04 | 操作列初始可见、少量数据隐藏竖向滚动条回归修复 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过 | 仍有既有组件命名冲突、动态导入、CSS `//` 注释、chunk size warning，未阻断 |
| 2026-07-04 | 本轮回归修复相关文件 | 空白检查 | `git diff --check -- <本轮相关文件>` | 通过 | 无 |
| 2026-07-04 | 自定义操作列默认右固定补丁 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过 | 仍有既有 warning，未阻断 |
| 2026-07-04 | 表头操作列固定兜底 | 源码烟测 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && node --input-type=module - <<'NODE' ...` | 通过 | 验证操作列默认标题、默认 `fixed: right`、`forge-table-action-column` class 和 CSS 右固定兜底 |
| 2026-07-04 | 表头操作列固定兜底 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过 | 仍有既有组件命名冲突、动态导入、CSS `//` 注释、chunk size warning，未阻断 |

## 6. 执行证据

- `execution-log.md`：已记录本轮构建、烟测、空白检查结果。
