# 测试计划：app-mode-code-download-consolidation
> created: 2026-06-14
> scope: 应用模式、应用管理代码下载、旧入口下线、应用总览术语与交互

## P0 验证范围

- 后端编译：覆盖 `forge-plugin-generator` 及依赖模块，验证新增 Controller、Service、DTO、VO、模板上下文和迁移引用可编译。
- 前端静态检查：覆盖本次改动的 `business-app.js`、路由守卫、应用管理页面和新增 `AppCodePanel.vue`。
- 前端构建：验证 Vue、自动路由、自动导入和 Vite 打包链路。
- 术语扫描：验证应用管理主链路不再出现用户可见的 CRUD、业务套件、业务对象、应用入口等旧文案。
- 路径扫描：验证代码生成模板不包含 `/ai/crud/`，下载代码模式只在后端校验和重写逻辑中引用旧路径。
- Diff 检查：验证无尾随空格和补丁格式问题。

## P1 验证范围

- 旧入口兼容：确认 `/ai/crud-config` 页面只保留迁移提示，后端旧管理接口加权限，`render/{configKey}` 保持可用。
- 代码包完整性：确认模板输出后端 Controller/Service/Mapper/DTO/Query、前端 API/Page、SQL、配置 JSON 和 README。
- 构建警告归因：记录非本次阻断的 Vite CSS 注释和分包警告。

## 本轮未执行

- 未启动本地后端服务和数据库执行真实接口调用；本轮改动已通过目标模块编译，服务级接口验证需要本地 MySQL/Redis 和已发布应用数据。
- 未执行真实 zip 下载并解压校验；当前通过模板扫描、后端生成前校验和编译验证覆盖静态风险。
