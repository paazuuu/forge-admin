# 全局异步 Loading 锁交互执行记录

## 2026-07-08 初始记录

- 已读取项目规则、长期记忆和自动化测试标准。
- 已梳理现有入口：Axios 拦截器、`postEncrypt`、路由守卫、`NUpload action`、原生 `fetch`。
- 结论：可以统一处理，但上传和直接 `fetch` 需要显式接入；后台缩略图/静默预取需要保留跳过能力。

## 2026-07-08 实施与验证

- 变更范围：
  - 新增 `forge-admin-ui/src/composables/useGlobalLoading.js`，统一管理全局 Loading 引用计数、文案、`managedFetch`、请求接入和跳过开关。
  - 新增 `forge-admin-ui/src/components/common/GlobalLoadingOverlay.vue`，提供全屏遮罩、滚动锁定和交互事件拦截。
  - 修改 `App.vue`、Axios 拦截器、路由守卫、上传组件、用户触发下载和 SSE/流式生成入口。
- 验证命令：
  - `git diff --check`
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - `ulimit -n 65535 && source ~/.nvm/nvm.sh && nvm use v20.19.0 && CHOKIDAR_USEPOLLING=true pnpm --dir forge-admin-ui exec vite --host 127.0.0.1 --port 5173`
  - `curl -I http://127.0.0.1:5173`
- 验证结果：
  - `git diff --check` 通过，无空白错误。
  - 前端生产构建通过，耗时约 1m 28s；修正 `downloadFile` 失败分支后已二次复跑通过。
  - Vite dev server 已启动，访问地址 `http://127.0.0.1:5173/`，`curl -I` 返回 `HTTP/1.1 200 OK`。
- 非阻断警告：
  - `UserSelectModal` 组件命名冲突。
  - 既有 CSS `//` 注释警告。
  - 既有动态导入与静态导入混用、chunk size 提示。
- 跳过项：
  - 本批只改前端，不执行 Maven。
  - 未做浏览器点击自动化验证；生产构建和本地服务可访问已覆盖语法、打包和服务启动链路。
