# 全局异步 Loading 锁交互任务

## Task 1：SDD 基线

- [x] 创建 `spec.md`，明确统一处理范围和边界。
- [x] 创建 `tasks.md`，拆分平台级改动。
- [x] 创建 `test-spec.md`，定义前端验证范围。
- [x] 创建 `execution-log.md`，记录执行过程。

## Task 2：全局 Loading Manager

- [x] 新增 `forge-admin-ui/src/composables/useGlobalLoading.js`。
- [x] 支持引用计数、token 结束、全局状态、请求文案推导。
- [x] 支持 `skipGlobalLoading`、`globalLoading === false`、`globalLoadingText`。
- [x] 支持 `withGlobalLoading` 和 `managedFetch`。

## Task 3：根级遮罩组件

- [x] 新增 `forge-admin-ui/src/components/common/GlobalLoadingOverlay.vue`。
- [x] 在 `App.vue` 中挂载遮罩。
- [x] 遮罩激活时全屏覆盖、禁用滚动、拦截点击/键盘/滚轮交互。

## Task 4：统一请求接入

- [x] 在 `forge-admin-ui/src/utils/http/interceptors.js` 接入全局 Loading。
- [x] 响应成功、业务失败、网络错误、超时、请求拦截错误都关闭 Loading。
- [x] 错误提示在 Loading 关闭后展示。
- [x] 路由守卫接入全局 Loading，保留现有 loading bar。

## Task 5：特殊异步入口接入

- [x] 文件上传组件接入全局 Loading。
- [x] 图片上传组件接入全局 Loading。
- [x] 用户触发型下载 fetch 接入 `managedFetch`。
- [x] SSE/流式生成 fetch 接入手动 token，完成/失败/取消后结束。

## Task 6：验证与回填

- [x] 执行 `git diff --check`。
- [x] 执行前端生产构建。
- [x] 更新 `execution-log.md`。
- [x] 回填任务完成状态。
