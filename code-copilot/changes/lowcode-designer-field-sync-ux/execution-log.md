# 执行日志：lowcode-designer-field-sync-ux

## 2026-06-13

- 创建变更 Spec 与任务清单。

## 2026-06-13 验证记录

### 变更范围

- 前端低代码业务对象设计器：
  - 表单设计器字段 ID 可输入。
  - 字段资产保存后前端本地同步表单、页面、视图字段引用。
  - 表单设计器 dirty 等价判断。
  - 字段资产配置由抽屉改为页面内嵌属性面板。

### 执行命令与结果

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint ...`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`✓ built in 2m 26s`。
  - 警告：存在既有 CSS `//` 注释格式警告；`src/store/index.js` 同时被动态和静态导入导致 chunk 警告。本轮未修改相关文件。
- `git diff --check -- <本轮相关文件>`
  - 结果：通过。
- `curl -I 'http://127.0.0.1:5173/app-center/object/demo/designer?objectId=1'`
  - 结果：HTTP 200，Vite 能返回前端入口。
- `curl -s 'http://127.0.0.1:5173/app-center/object/demo/designer?objectId=1'`
  - 结果：返回 `index.html`，包含 `/src/main.js` 入口。

### 非阻断失败与跳过项

- Node 工具函数冒烟：
  - 命令：`node --input-type=module -e "<动态导入 fieldReferenceUtils/viewSchema>"`
  - 结果：失败，原因是源码使用 Vite 扩展省略导入，裸 Node ESM 无法解析 `./formCreateToForge`。
  - 判定：非阻断，Vite build 已覆盖真实打包解析。
- 浏览器点击级验证：
  - 内置浏览器初始化返回 `Browser is not available: iab`。
  - 已降级为 Vite HTTP 路由检查，未覆盖登录后字段配置点击流程。

### 服务清理

- 首次启动 Vite dev server 因 `EMFILE: too many open files, watch` 退出。
- 使用 `ulimit -n 65535 && CHOKIDAR_USEPOLLING=true pnpm --dir forge-admin-ui exec vite --host 127.0.0.1 --port 5173` 成功启动本轮临时 Vite 服务。
- 验证后已通过 `Ctrl-C` 停止该 Vite 服务。

## 2026-06-13 追加验证记录：默认字段编码与已有字段名称

### 变更范围

- 修复 form-create 默认 `rule.field = input`、`select` 等设计器内部字段名被当作业务字段编码，导致保存时报“数据库列名重复: input”的问题。
- 修复绑定已有字段资产后，表单组件标题未优先带出字段资产名称的问题。

### 执行命令与结果

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/form-first/formCreateToForge.js src/views/app-center/components/designer/form-first/forgeToFormCreate.js`
  - 结果：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/form-first/formCreateToForge.js forge-admin-ui/src/views/app-center/components/designer/form-first/forgeToFormCreate.js code-copilot/changes/lowcode-designer-field-sync-ux/spec.md code-copilot/changes/lowcode-designer-field-sync-ux/tasks.md code-copilot/changes/lowcode-designer-field-sync-ux/test-spec.md code-copilot/changes/lowcode-designer-field-sync-ux/execution-log.md`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`✓ built in 47.69s`。
  - 警告：仍存在既有 CSS `//` 注释格式警告，以及 `src/store/index.js` 同时动态/静态导入导致的 chunk 警告；本轮未修改相关文件。

### 非阻断失败与跳过项

- `pnpm --dir forge-admin-ui exec esno --help`
  - 结果：失败，原因是 `tsx` 在当前沙箱内创建本地 IPC socket 时返回 `EPERM`。
- `node --experimental-specifier-resolution=node --input-type=module -e "<直接导入 formCreateToForge.js>"`
  - 结果：失败，原因是 Node 原生 ESM 无法解析项目中 Vite 风格的省略后缀导入。
- `node --input-type=module -e "<Vite SSR ssrLoadModule formCreateToForge.js>"`
  - 结果：失败，原因是 Vite SSR 会连带加载 `@/utils` 加密模块，既有 `sm-crypto` CommonJS 命名导出在 SSR 下不兼容；该问题不影响生产构建。

### 服务清理

- 本轮未启动长期服务。
- 前端构建命令已正常退出，无需额外清理进程。
