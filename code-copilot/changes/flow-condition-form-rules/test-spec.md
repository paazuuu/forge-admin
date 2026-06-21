# 测试计划 — 流程分支条件表单规则配置优化

## P0 增量验证
- `git diff --check`：检查本轮相关文件无空白错误。
- 目标文件 ESLint：检查并自动修复本轮改动的 Vue/JS/测试文件。
- `ConditionConfig.spec.js`：覆盖字段条件渲染、数值表达式、区间表达式、高级表达式回退、删除最后一条规则后清空条件、聚焦分支只展示当前分支、默认分支可配置条件、设置默认时保留已有条件。
- `form-field-catalog.spec.js`：覆盖统一表单 Schema 递归字段解析、业务组件字段绑定和临时字段过滤。
- `DingFlowDesigner.spec.js`：确认流程设计器原有网关分支配置行为未回归，并覆盖点击分支标签只打开当前分支、点击网关节点恢复全部分支。
- `useFlowDesigner.spec.js`：覆盖条件网关继续添加第三分支、追加分支回到合流节点、默认分支唯一且 `defaultFlowId` 不漂移。
- `DingFlowDesigner.spec.js`：覆盖画布分支区域点击“添加分支”后分支数变为 3，并聚焦新分支配置；确认抽屉内不再展示添加分支入口。
- `EdgePath.spec.js`：覆盖默认分支存在条件时连线标签仍只展示默认标签；网关分支边不在 SVG 层重复展示条件；分支标签只展示摘要。
- `AddNodeButton.spec.js`：覆盖 `BranchAddButton` 定位、点击和 readonly 禁用状态。
- `ConditionConfig.spec.js`：覆盖表单字段表达式重新打开时回显为字段条件模式，并可继续按规则修改表达式。
- `json-to-bpmn.spec.js` / `branch-parser.spec.js`：覆盖 default 边条件可保留在设计器 JSON 中，但导出 BPMN 时不写入 default 边 `conditionExpression`。
- `FormPermissionConfig.spec.js`：覆盖表单字段权限按字段目录渲染、切换可编辑后输出完整权限、关闭可见同步关闭可编辑和必填。
- `json-to-bpmn.spec.js` / `bpmn-to-json-linear.spec.js`：覆盖流程级 `allowSubmitterWithdraw`、`autoApprovalMode` 的 BPMN process 扩展属性写入和解析。
- `user-task-parser-permissions.spec.js`：覆盖 `flowable:formFieldPermissions` 用户任务扩展属性解析。
- 后端 flow 插件模块编译：覆盖 `FlowTaskServiceImpl`、`TaskFormInfo` 的 Java 编译和 Flowable API 调用签名。
- `pnpm build`：确认前端生产构建可通过。

## P1 交互验证
- 启动本地 Vite 预览服务，使用临时预览页挂载 `ConditionConfig`。
- Playwright 打开预览页，确认字段条件 UI 可见，输入金额后表达式预览生成 `${amount == 3000}`。
- 验证结束后停止本轮启动服务并删除临时预览文件。

## 跳过项
- 数据库接口实跑：本轮未启动后端和 MySQL，运行时逻辑通过模块编译覆盖，真实流程实例自动同意仍需联调环境做业务链路验证。
- 全量 E2E：本轮风险集中在流程设计器、BPMN 转换和 flow 插件任务服务，已用组件测试、转换器测试、前端构建和后端模块编译覆盖。
