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
