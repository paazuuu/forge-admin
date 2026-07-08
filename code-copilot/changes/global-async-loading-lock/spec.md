# 全局异步 Loading 锁交互

## 背景

当前前端存在局部 `loading`、按钮 `:loading`、路由进度条和上传组件内部进度等多套异步反馈。不同页面覆盖不一致，用户快速连续点击时仍可能产生重复提交、重复删除、重复导出或重复下发请求。

## 目标

建立平台级全局 Loading 能力：

- 所有默认业务请求进入统一全屏遮罩，遮罩期间锁定点击、滚动、键盘等页面交互。
- 并行请求采用引用计数，所有请求结束后才解除遮罩。
- 请求成功、业务失败、网络错误、超时、拦截器抛错都必须关闭遮罩。
- 错误提示必须在遮罩关闭后展示，避免错误弹窗被遮罩覆盖。
- 路由跳转、页面初始化、分页筛选、弹窗加载、批量处理、导入导出、上传等异步链路统一接入。
- 保留显式豁免能力，允许文件缩略图、后台静默预取等非用户操作请求不锁全屏。

## 范围

### 本批纳入

- Axios 统一请求实例：`request`、`noPrefixRequest`、`mockRequest`。
- `postEncrypt` 加密请求：复用 `request`，无需单独改页面。
- 路由跳转过程：路由守卫开始/完成/异常接入全局 Loading。
- Naive `NUpload action` 文件上传、图片上传：补充统一 Loading 计数。
- 原生 `fetch` 的用户触发型下载和 SSE/流式请求：接入同一套 manager。
- 根组件全屏遮罩：全页面覆盖、禁用滚动和点击。

### 本批不强制纳入

- 纯展示型图片 blob 拉取、文件缩略图回显等后台渲染请求默认不锁全屏，避免列表/详情打开时大量缩略图请求造成页面长时间不可操作。
- WebSocket 收发不纳入本次 Loading 计数，除非后续定义为明确用户触发任务。

## 设计

### 全局 Loading Manager

新增 `useGlobalLoading`，内部维护 active token 列表：

- `startGlobalLoading(options)` 返回 token。
- `finishGlobalLoading(token)` 按 token 结束。
- `withGlobalLoading(fn, options)` 包装 Promise。
- `managedFetch(input, init, options)` 包装原生 fetch。
- `resolveRequestLoadingText(config)` 根据请求方法和 URL 推导提示文案。
- `skipGlobalLoading/globalLoading === false` 显式跳过。
- `globalLoadingText` 覆盖默认文案。

### Axios 接入

在请求拦截器入口立即 `start`，避免用户连续点击同一按钮时第二次点击进入。响应成功、业务失败、网络失败、请求拦截失败均 `finish`。

### 路由接入

路由 `beforeEach` 开始全局 Loading，`afterEach/onError` 结束。保留现有 `$loadingBar` 顶部进度反馈。

### 上传接入

`NUpload action` 不经过 Axios，需要在 `on-before-upload` 校验成功后开始 Loading，在 `on-finish/on-error` 结束。

### 原生 fetch 接入

用户触发型 `fetch` 使用 `managedFetch` 或手动 token 包裹。SSE 流式生成使用手动 token，完成、错误、取消时结束。

## 验收标准

- 快速连续点击按钮时，首个请求开始后出现全屏遮罩并阻止后续点击。
- 多个接口并行时，任一接口先返回不会提前解除遮罩。
- 接口失败、超时或业务错误时遮罩关闭，随后展示错误提示。
- 路由跳转期间展示“页面加载中，请稍候...”，菜单重复点击被遮罩拦截。
- 文件上传展示“文件上传中，请稍候...”，上传完成/失败后解除。
- 导出/下载展示“文件导出处理中，请稍候...”或“文件下载处理中，请稍候...”。
- 前端构建通过。

## 风险与边界

- 全局遮罩会改变所有业务请求的交互节奏，局部频繁请求页面可能感觉更“重”。通过 `skipGlobalLoading` 对后台静默请求做豁免。
- SSE 流式请求如果全程锁定，用户不能中途操作页面。当前按需求锁定到完成/失败/取消，后续如需取消按钮可设计遮罩内取消能力。
