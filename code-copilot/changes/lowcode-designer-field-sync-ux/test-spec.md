# 测试计划：lowcode-designer-field-sync-ux

## 本轮增量验证

### P0 验证

- 聚焦 ESLint：验证本次修改的 Vue/JS 文件语法、未使用变量和基础风格。
- 前端生产构建：验证 Vite 构建链路和路由懒加载编译。
- `git diff --check`：验证补丁无尾随空白等格式错误。
- Vite HTTP 路由检查：验证对象设计器路由能返回前端入口。

### P1 验证

- 字段引用重命名工具冒烟：尝试在 Node 环境直接导入工具函数并验证 `customerLevel -> customerGrade` 替换。

## 跳过项

- 浏览器点击级验证：当前 Codex in-app browser 返回 `Browser is not available: iab`，无法通过内置浏览器操作页面。
- 后端接口验证：本轮未修改后端接口，字段保存最终一致性沿用既有后端 `BusinessFieldDesignService.updateField()` 的引用替换逻辑。

## 2026-06-13 追加验证：默认字段编码与已有字段名称

### P0 验证

- 聚焦 ESLint：验证 `formCreateToForge.js`、`forgeToFormCreate.js` 的语法和未使用变量。
- 前端生产构建：验证真实 Vite 打包链路能解析本轮转换逻辑。
- `git diff --check`：验证本轮补丁无尾随空白等格式错误。

### P1 验证

- 尝试使用 Node / Vite SSR 做转换函数冒烟，覆盖 `rule.field = input` 和已有字段 `customerLevel` 场景。

### 跳过或降级项

- 转换函数直接冒烟受 Vite 省略后缀导入、`tsx` IPC socket 限制以及 SSR 下既有 `sm-crypto` CommonJS 命名导出问题影响，未作为通过依据。
