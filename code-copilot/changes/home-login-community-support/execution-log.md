# 执行记录

## 2026-06-28

- 变更文件：
  - `forge-admin-ui/src/views/login/index.vue`
  - `forge-admin-ui/src/views/home/index.vue`
  - `forge-admin-ui/src/assets/images/forge-wechat-group.png`
  - `forge-admin-ui/src/assets/images/forge-wechat-support.png`
- 登录页：
  - 移除动态背景装饰节点、扫光、流光边框、装饰圆点。
  - 将背景改为静态渐变和轻量网格。
  - 去掉登录卡片、品牌区、表单项和验证码弹窗的入场动画。
- 首页：
  - 在统计卡片下方新增“ForgeAdmin 社区”面板。
  - 展示交流群和维护支持两个二维码。
  - 二维码使用 `n-image` 支持点击放大预览。
  - 调大二维码区域，桌面端单图高度 260px，窄屏 320px。
  - 收款文案使用“支持维护”“随心支持”“请维护者喝杯咖啡，支持持续优化”。
- 验证：
  - `pnpm --dir forge-admin-ui exec eslint src/views/login/index.vue src/views/home/index.vue` 通过。
  - `git diff -- forge-admin-ui/src/views/home/index.vue forge-admin-ui/src/views/login/index.vue --check` 通过。
  - `pnpm --dir forge-admin-ui build` 通过；保留项目已有构建警告：`UserSelectModal` 命名冲突、CSS `//` 注释、部分动态/静态导入 chunk 提示。

## 2026-06-28 登录页专业化修正

- 用户反馈登录页去掉动效后“普通、不专业、不吸引人”。
- 调整方向：
  - 不恢复高成本持续动画。
  - 增强静态产品主视觉和企业级控制台质感。
- 变更：
  - 左侧品牌区从普通 Logo + 卖点列表，改为产品封面式结构。
  - 新增平台标识、微内核中后台定位、主标题“从数据模型到业务应用 / 一次搭建，持续演进”。
  - 新增静态控制台视觉块，表达数据建模、页面生成、流程编排、权限审计。
  - 卖点改为更贴近 ForgeAdmin 的“低代码交付闭环、权限与租户隔离、微内核插件架构”。
  - 右侧表单标题改为“登录 ForgeAdmin”，增加“安全登录”标识和“租户隔离 / 权限审计 / 会话保护”信任信息。
  - 移除不再使用的 `title` / `tenantPageTitle` 变量和 `getDefaultPageTitle` 导入。
- 验证：
  - `pnpm --dir forge-admin-ui exec eslint src/views/login/index.vue` 通过。
  - `git diff -- forge-admin-ui/src/views/login/index.vue --check` 通过。
  - `pnpm --dir forge-admin-ui build` 通过；保留项目已有构建警告。

## 2026-06-28 登录页降噪与小屏适配

- 用户反馈：
  - 右侧颜色太深、文字太多。
  - 尽量不要有英文。
  - 右侧信息过乱，左右布局都占满且不够紧凑。
  - 希望左侧大一点、右侧小一点，并适配小屏幕。
- 变更：
  - 删除右侧“安全登录”“租户隔离 / 权限审计 / 会话保护”等弱价值说明。
  - 右侧标题改为“登录系统”，默认提示改为“请输入账号密码”。
  - 左侧删除英文标签，能力视觉标题改为中文“应用搭建”。
  - 左侧品牌区从深色背景改为浅色产品区，整体视觉更轻。
  - 桌面端列比例改为左侧 62%、右侧 38%。
  - 小屏断点提升到 960px 以下隐藏左侧视觉，仅展示紧凑登录表单。
  - 移除 Google Fonts 外部字体加载，改用系统中文字体栈。
  - 登录容器允许竖向滚动，避免小屏验证码或第三方登录区域溢出。
- 验证：
  - `pnpm --dir forge-admin-ui exec eslint src/views/login/index.vue` 通过。
  - `git diff --check -- forge-admin-ui/src/views/login/index.vue` 通过。
  - `pnpm --dir forge-admin-ui build` 通过；保留项目已有构建警告：`UserSelectModal` 命名冲突、CSS `//` 注释、部分动态/静态导入 chunk 提示。

## 2026-06-28 登录页全屏分栏修正

- 用户反馈：
  - `login-card` 不要再居中卡片化，直接平铺。
  - `login-brand` 和 `login-form-wrapper` 应占满屏幕，避免右侧表单显示不全。
  - 左侧底部轮播在卡片布局下看不见。
- 变更：
  - `login-card` 改为全屏容器，取消 `max-width`、圆角、边框和阴影。
  - `login-brand` 与 `login-form-wrapper` 设置为视口高度，左右直接铺满。
  - 桌面分栏调整为左侧 60%、右侧 40%。
  - 右侧表单区保持独立竖向滚动，避免验证码/三方登录等内容溢出。
  - 小屏继续隐藏左侧，仅保留全屏登录表单区。
- 验证：
  - `pnpm --dir forge-admin-ui exec eslint src/views/login/index.vue` 通过。
  - `git diff --check -- forge-admin-ui/src/views/login/index.vue` 通过。
  - `pnpm --dir forge-admin-ui build` 通过；保留项目已有构建警告。

## 2026-06-28 登录页 Banner 可见性修正

- 用户反馈：
  - `banner` 跑到左侧最下面，基本看不见。
  - 轮播区域宽度太小。
- 变更：
  - 左侧内容从 `space-between` 改为正常自上而下布局。
  - 去掉 `banner` 的 `margin-top: auto`，让轮播紧跟在标题下方。
  - 将 `banner` 宽度从 `min(480px, 100%)` 扩大到 `min(680px, 100%)`。
  - 轮播高度改为 `clamp(300px, 44vh, 430px)`，在不同屏幕高度下保持可见。
  - 堆叠预览卡片改为容器相对宽高，避免在宽屏下显得过小。
  - 增加低高度桌面屏适配，收紧 Logo 与标题间距。
- 验证：
  - `pnpm --dir forge-admin-ui exec eslint src/views/login/index.vue` 通过。
  - `git diff --check -- forge-admin-ui/src/views/login/index.vue` 通过。
  - `pnpm --dir forge-admin-ui build` 通过；保留项目已有构建警告。

## 2026-06-28 登录页轮播居中与品牌区修正

- 用户反馈：
  - `login-brand` 占比仍偏大，需要再小一点。
  - `n-carousel`、轮播内容和指示器没有居中。
  - 指示器位置偏上，轮播高度偏矮。
  - Logo 旁边希望直接露出 `ForgeAdmin`。
- 变更：
  - 桌面分栏调整为左侧 52%、右侧 48%，右侧最小宽度提升到 420px。
  - Logo 旁主标题改为 `ForgeAdmin`。
  - 左侧内容容器限制到 720px 并整体居中。
  - 轮播高度提升到 `clamp(430px, 62vh, 640px)`，低高度桌面兜底为 380px。
  - 对 Naive Carousel 内部 `.n-carousel__slides`、`.n-carousel__slide`、`.n-carousel__dots` 增加宽高和居中规则。
  - 指示器改为底部 `left: 50%` + `translateX(-50%)` 居中。
  - 轮播预览卡片放大并下移，避免视觉区域显得矮和靠上。
- 验证：
  - `pnpm --dir forge-admin-ui exec eslint src/views/login/index.vue` 通过。
  - `git diff --check -- forge-admin-ui/src/views/login/index.vue` 通过。
  - `pnpm --dir forge-admin-ui build` 通过；保留项目已有构建警告。

## 2026-06-28 登录页轮播箭头与 Logo 简化

- 用户反馈：
  - 去掉 `max-width: 720px` 限制。
  - 轮播需要类似 Arco 的左右箭头。
  - Logo 区域应简洁，只保留 Logo 和 `ForgeAdmin`，不要多余说明。
  - `stack-preview` 里面内容太少，需要更像后台界面。
- 变更：
  - 移除 `.brand-content`、`.stack-carousel`、`.carousel-item` 的 `max-width: 720px`。
  - Logo 区改为 `img + ForgeAdmin` 的简单横向布局，删除“微内核中后台框架”等副文案。
  - `n-carousel` 启用 `show-arrow`，通过 `#arrow` 插槽渲染 `arco-carousel-arrow-left` 和 `arco-carousel-arrow-right`。
  - 为箭头增加半透明圆形按钮样式和 hover 状态。
  - 扩充三个轮播预览卡片：列表页增加更多栅格和表格行，生成链路增加指标块和更多行，权限治理增加权限块和更多行。
- 验证：
  - `pnpm --dir forge-admin-ui exec eslint src/views/login/index.vue` 通过。
  - `git diff --check -- forge-admin-ui/src/views/login/index.vue` 通过。
  - `pnpm --dir forge-admin-ui build` 通过；保留项目已有构建警告。

## 2026-06-28 登录页 Dots 居中与右侧增强

- 用户反馈：
  - 默认 `.n-carousel__dots` 仍未居中。
  - 左侧仍偏宽。
  - 右侧登录按钮希望稍高一点。
  - 右侧可以稍微丰富，但不要复杂。
- 变更：
  - 轮播移除默认 `dot-type`，通过 `#dots` 插槽自定义 `arco-carousel-dots` 和 `arco-carousel-dot`。
  - 自定义 dots 使用 `left: 0; right: 0; justify-content: center` 固定底部居中，避免 Naive 默认定位影响。
  - 桌面分栏调整为左侧 48%、右侧 52%，右侧最小宽度提升到 440px。
  - 登录按钮高度从 46px 调整到 52px。
  - 登录按钮下方新增轻量信息条：安全登录、租户隔离、操作审计。
  - 新增信息条图标使用项目已有 `shield`、`layers`、`check-circle`，避免未知图标类。
- 验证：
  - `pnpm --dir forge-admin-ui exec eslint src/views/login/index.vue` 通过。
  - `git diff --check -- forge-admin-ui/src/views/login/index.vue` 通过。
  - `pnpm --dir forge-admin-ui build` 通过；保留项目已有构建警告。

## 2026-06-28 登录页轮播图片替换与遮挡修正

- 用户反馈：
  - 轮播图片需要替换为新的蓝色平台能力图。
  - `carousel-copy` 文案被图片盖住。
- 变更：
  - 将 `/Users/yaominliang/Downloads/nanobanana-preview-original-2026-06-28T03-13-43-752Z.png` 复制为 `forge-admin-ui/src/assets/images/login-carousel-platform.png`。
  - 三个登录页轮播项统一使用 `login-carousel-platform.png`，去掉手写堆叠预览卡片。
  - 轮播图改为绝对定位在标题下方，使用 `top`、`max-height` 和 `object-fit: contain` 控制尺寸，避免覆盖标题和副标题。
  - 增加低高度屏幕下的图片尺寸兜底，保证小屏下文案和图片都在可视区域内。
- 验证：
  - `pnpm --dir forge-admin-ui exec eslint src/views/login/index.vue` 通过。
  - `git diff --check -- forge-admin-ui/src/views/login/index.vue forge-admin-ui/src/assets/images/login-carousel-platform.png` 通过。
  - `pnpm --dir forge-admin-ui build` 通过；保留项目已有构建警告。

## 2026-06-28 登录页左侧文案层级精简

- 用户反馈：
  - “让后台系统更快交付 / 建模、页面、流程、权限统一管理”和下方轮播文案挤在一起，信息显得乱。
- 变更：
  - 删除左侧品牌区大标题和副标题，仅保留 `ForgeAdmin` Logo 与轮播能力标题。
  - 调整 Logo 与轮播之间的间距，让左侧视觉层级从品牌名直接过渡到能力轮播。
  - 清理不再使用的 `.brand-title` 样式。
- 验证：
  - `pnpm --dir forge-admin-ui exec eslint src/views/login/index.vue` 通过。
  - `git diff --check -- forge-admin-ui/src/views/login/index.vue` 通过。

## 2026-06-28 登录页 Banner 位置与小屏社交登录修正

- 用户反馈：
  - 左侧 `banner` 位置偏上，希望更靠中间。
  - 小屏幕下“其他登录方式”看不见。
- 变更：
  - 左侧 `.brand-content` 改为垂直居中布局，让 Logo 和轮播整体更靠屏幕中部。
  - 小屏下登录卡片取消内部隐藏溢出，改为页面自然滚动，避免底部内容被截断。
  - 小屏下隐藏登录按钮下方的信任信息条，减少弱价值内容占用高度。
  - 小屏下压缩表单项、记住我、社交登录区域的上下间距，让“其他登录方式”更容易露出。
- 验证：
  - `pnpm --dir forge-admin-ui exec eslint src/views/login/index.vue` 通过。
  - `git diff --check -- forge-admin-ui/src/views/login/index.vue` 通过。

## 2026-06-28 登录页 Logo 左上角固定

- 用户反馈：
  - `logo-lockup` 不要跟随左侧内容居中，需要放回页面左上角。
- 变更：
  - 将 `.logo-lockup` 改为绝对定位到左侧品牌区域左上角。
  - Logo 脱离普通文档流，不再影响 `banner` 的垂直居中位置。
- 验证：
  - `pnpm --dir forge-admin-ui exec eslint src/views/login/index.vue` 通过。
  - `git diff --check -- forge-admin-ui/src/views/login/index.vue` 通过。

## 2026-06-28 登录页表单控件圆角收敛

- 用户反馈：
  - 登录页输入框和按钮圆角需要改成 `2px`。
- 变更：
  - 将登录页用户名、密码、验证码输入框的 Naive UI 外层和状态边框圆角统一改为 `2px`。
  - 将租户选择框、登录按钮、短信验证码按钮、滑块验证触发按钮、社交登录按钮圆角统一改为 `2px`。
  - 登录按钮内部高光层和登录区信任信息条同步改为 `2px`，保持视觉一致。
- 验证：
  - `pnpm --dir forge-admin-ui exec eslint src/views/login/index.vue` 通过。
  - `git diff --check -- forge-admin-ui/src/views/login/index.vue` 通过。
