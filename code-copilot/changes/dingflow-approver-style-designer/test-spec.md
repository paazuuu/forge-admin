# 测试计划：dingflow-approver-style-designer

> 本文件记录当前变更的自动化验证基线。详细执行证据以 `execution-log.md` 为准。

## 1. 基线范围

- 变更类型：前端 Vue 流程设计器、画布交互、流程查看器、BPMN XML 与 flowJson 转换。
- 核心风险：BPMN 双向转换、节点卡片渲染、画布布局、添加节点交互、配置抽屉保存、查看器状态展示。
- 后端范围：后端 Flowable 接口零改动，本变更不执行 Maven 验证。

## 2. 必跑验证

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
cd forge-admin-ui
NODE_ENV=development pnpm vitest run src/components/flow-designer
pnpm build
```

## 3. 本轮增量验证（2026-06-20）

- 变更范围：流程设计器样式优化，涉及 `NodeCard`、`FlowCanvas`、添加节点按钮/菜单、分支标签、连线颜色、画布布局尺寸、自动居中，以及 `vite.config.js` 过期 `optimizeDeps.include` 清理。
- 必跑命令：
  - `pnpm exec eslint` 针对本轮触达文件。
  - `NODE_ENV=development pnpm vitest run src/components/flow-designer`。
  - `pnpm build`。
- 浏览器验证：尝试启动 Vite dev server；当前执行环境禁止监听本地端口，无法完成浏览器截图，记录在 `execution-log.md`。

## 4. 本轮增量验证（2026-06-20 发起节点配置收口）

- 变更范围：发起节点配置面板、开始节点卡片、StartEvent BPMN 导入/导出规则。
- 验证重点：
  - 发起人变量固定为 `initiator`，配置面板不再提供可编辑输入。
  - StartEvent 不再读写 `formKey/formJson/formUrl`，流程表单回到流程模型全局表单配置。
  - 旧 XML/JSON 中的自定义 initiator 和开始节点表单属性会被归一化，不污染保存后的 BPMN。
- 必跑命令：
  - `pnpm exec eslint` 针对本轮触达文件。
  - `NODE_ENV=development pnpm vitest run` 针对转换器、设计器和节点卡片测试。
  - `NODE_ENV=development pnpm vitest run src/components/flow-designer`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
  - `git diff --check`。
- 浏览器验证：本轮未启动 dev server；此前同环境启动 Vite 监听端口失败，本轮以组件测试和构建验证为准。

## 5. 本轮增量验证（2026-06-20 设计页整体布局优化）

- 变更范围：`src/views/flow/design.vue`，把顶部流程属性/表单配置/说明配置区收口到右侧固定面板。
- 验证重点：
  - 画布区域不再被顶部配置区挤占，右侧面板固定占位。
  - 右侧面板分为“流程设计”和“更多设置”两大入口。
  - “更多设置”采用树形导航 + 详情内容区，流程属性、表单配置、说明和 AI 助手可切换。
  - 发起表单提示仍指向流程级全局表单，不回退到发起节点表单配置。
- 必跑命令：
  - `pnpm exec eslint src/views/flow/design.vue`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
  - `git diff --check`。
- 浏览器验证：本轮布局改动适合浏览器截图验证；若本地 Vite 仍因当前执行环境禁止监听端口失败，则记录为跳过项。

## 6. 本轮增量验证（2026-06-20 顶部主 Tab 与设置页二次收口）

- 变更范围：`src/views/flow/design.vue`，根据用户反馈把“流程设计 / 更多设置”从右侧面板移到页面顶部。
- 验证重点：
  - 顶部不再直接展示流程名称、流程编码和部署状态。
  - “流程设计”模式只显示流程画布，不再显示额外说明/辅助面板。
  - “更多设置”模式展示设置详情，右侧保留树形设置导航。
  - 流程名称、流程编码、部署状态移动到“更多设置 / 流程属性”。
- 必跑命令：
  - `pnpm exec eslint src/views/flow/design.vue`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
  - `git diff --check`。
- 浏览器验证：继续尝试启动 Vite；当前执行环境禁止监听 `127.0.0.1:5173`，记录在 `execution-log.md`。

## 7. 本轮增量验证（2026-06-20 属性面板参考图样式优化）

- 变更范围：`src/components/flow-designer/panel/*` 节点属性抽屉与审批人配置面板。
- 验证重点：
  - 属性抽屉改为轻量头部，表单项采用参考图式纵向标签和统一控件高度。
  - 审批人节点页签收口为“审批人设置 / 表单权限 / 审批后操作”。
  - 节点名称进入“审批人设置”页签，会签配置合并到同一页签。
  - 表单权限、操作权限、监听器仍保留配置入口，不丢失原能力。
- 必跑命令：
  - `pnpm exec eslint` 针对本轮触达的 panel 文件。
  - `NODE_ENV=development pnpm vitest run src/components/flow-designer/__tests__/DingFlowDesigner.spec.js`。
  - `NODE_ENV=development pnpm vitest run src/components/flow-designer`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
  - `git diff --check`。
- 浏览器验证：继续尝试启动 Vite；当前执行环境禁止监听 `127.0.0.1:5173`，记录在 `execution-log.md`。

## 8. 本轮增量验证（2026-06-20 审批人/监听器配置修正）

- 变更范围：`src/components/flow-designer/panel/ApproverAssigneeForm.vue`、`src/components/flow-designer/panel/ListenerConfig.vue`。
- 验证重点：
  - 指定人员、候选人员改为复用系统用户选择弹窗，支持按组织架构筛选，不再暴露 `${user_xxx}` 表达式输入。
  - 候选角色改为从 `/system/role/page` 角色列表选择，不再手输角色编码。
  - SPEL 改为从 `/api/flow/spelTemplate/list` 模板下拉选择，选择后自动维护内部表达式。
  - 监听器配置从横向挤压布局改为分块表单，事件/类型两列，具体实现值独占整行。
- 必跑命令：
  - `pnpm exec eslint src/components/flow-designer/panel/ApproverAssigneeForm.vue src/components/flow-designer/panel/ListenerConfig.vue`。
  - `NODE_ENV=development pnpm vitest run src/components/flow-designer/__tests__/DingFlowDesigner.spec.js`。
  - `NODE_ENV=development pnpm vitest run src/components/flow-designer`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
  - `git diff --check`。
- 浏览器验证：本轮 Vite dev server 可启动到 `http://127.0.0.1:3000/`，HTTP 冒烟 200；当前环境缺少 Python/Node Playwright 包且不安装新依赖，未执行截图级自动化验证。

## 9. 本轮增量验证（2026-06-20 运行态流程图查看器适配）

- 变更范围：`DingFlowViewer` 运行态数据适配，以及待办、已办、我发起、流程监控、低代码流程详情页面的查看器引用。
- 验证重点：
  - `diagram-info` 后端返回的 `ProcessDiagramInfo.nodes` 能被新查看器识别，不再只依赖旧的 `nodeInstanceList`。
  - 节点状态大小写和别名归一化后能驱动新卡片状态高亮。
  - 流程整体状态支持后端小写 `running/completed/terminated`。
  - 页面模板显式使用 `DingFlowViewer`，避免旧组件名误导。
  - 查看器在折叠面板、弹窗和流程详情内有稳定最小高度，不被压扁。
- 必跑命令：
  - `pnpm exec eslint` 针对本轮触达的 viewer 和业务入口文件。
  - `NODE_ENV=development pnpm vitest run src/components/flow-designer/viewer/__tests__/DingFlowViewer.spec.js`。
  - `NODE_ENV=development pnpm vitest run src/components/flow-designer`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
  - `git diff --check`。
- 浏览器验证：启动 Vite dev server 到 `http://127.0.0.1:3000/` 并执行 HTTP 冒烟 200；当前环境未安装 Playwright/Puppeteer，未执行截图级自动化验证。

## 10. 本轮增量验证（2026-06-20 查看器视野与卡片密度优化）

- 变更范围：`FlowCanvas`、`DingFlowViewer`、`NodeCard`、运行态布局引擎及对应测试。
- 验证重点：
  - 运行态流程图只读时仍允许平移、滚轮移动和工具栏缩放，解决长流程看不全。
  - `DingFlowViewer` 加载流程后按画布边界自动适配可视区。
  - 节点卡片标题、状态、审批人摘要分层展示，避免“部门经理审批 / 审批中 / 审批人”挤在一行或贴得过近。
  - 节点卡片高度与布局引擎高度一致，避免卡片变高后节点/连线重叠。
- 必跑命令：
  - `pnpm exec eslint` 针对本轮触达的 canvas、viewer、nodes 文件。
  - `NODE_ENV=development pnpm vitest run` 针对 `FlowCanvas`、`node-cards`、`DingFlowViewer`。
  - `NODE_ENV=development pnpm vitest run src/components/flow-designer`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
  - `git diff --check`。
- 浏览器验证：启动 Vite dev server 到 `http://127.0.0.1:3000/` 并执行 HTTP 冒烟 200；当前环境未安装 Playwright/Puppeteer，未执行截图级自动化验证。

## 11. 本轮增量验证（2026-06-20 节点详情面板宽版优化）

- 变更范围：`src/components/flow-designer/viewer/NodeDetailPopover.vue` 及查看器相关测试。
- 验证重点：
  - 节点详情从窄气泡改为宽版详情面板，默认宽度约 480px。
  - 处理人、开始时间、完成时间、审批结果使用分区/两列布局。
  - 审批意见独立展示，长文本可换行。
  - 详情面板根据视口宽度和高度做边界保护，避免超出屏幕。
- 必跑命令：
  - `pnpm exec eslint` 针对 `NodeDetailPopover.vue`、`NodeDetailPopover.spec.js` 和查看器文件。
  - `NODE_ENV=development pnpm vitest run` 针对 `NodeDetailPopover` 和 `DingFlowViewer`。
  - `NODE_ENV=development pnpm vitest run src/components/flow-designer`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
  - `git diff --check`。
- 浏览器验证：启动 Vite dev server 到 `http://127.0.0.1:3000/` 并执行 HTTP 冒烟 200；当前环境未安装 Playwright/Puppeteer，未执行截图级自动化验证。

## 12. 本轮增量验证（2026-06-20 待办已办详情页布局优化）

- 变更范围：`src/components/flow/FlowTaskDetailShell.vue`、`src/views/flow/todo.vue`、`src/views/flow/done.vue`、`src/views/flow/started.vue`。
- 验证重点：
  - 待办处理详情默认全屏展开，避免业务内容、流程图和审批操作被压缩在窄弹窗里。
  - 详情页改为顶部状态栏 + 左侧业务详情/处理区 + 右侧审批记录双栏结构。
  - 待办页去掉“基本信息 / 审批进度 / 流程图”Tab 切换，审批记录在右侧常驻展示。
  - 已办和我发起详情复用同一布局，审批结果、意见、签名、撤回操作分区展示。
  - 流程图降级为左侧折叠辅助区，不再抢占审批记录主视图。
- 必跑命令：
  - `pnpm exec eslint src/components/flow/FlowTaskDetailShell.vue src/views/flow/todo.vue src/views/flow/done.vue src/views/flow/started.vue`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
  - `git diff --check`。
- 浏览器验证：启动 Vite dev server 到 `http://127.0.0.1:3000/` 并执行 HTTP 冒烟 200；当前环境未安装 Python Playwright，未执行截图级自动化验证。

## 13. 本轮增量验证（2026-06-20 审批记录面板去 AI 化）

- 变更范围：`src/components/flow/FlowTaskDetailShell.vue` 右侧审批记录区。
- 验证重点：
  - 审批记录不再复用通用渐变卡片时间线，改为专用扁平记录流。
  - 节点图标从渐变圆点改为小方形业务状态图标，连线更细更克制。
  - 每条记录聚焦“节点 / 处理结果 / 处理人 / 时间 / 意见 / 签名”，减少装饰感。
  - 保持待办、已办、我发起三个详情入口复用同一审批记录布局。
- 必跑命令：
  - `pnpm exec eslint src/components/flow/FlowTaskDetailShell.vue`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
  - `git diff --check`。
- 浏览器验证：本轮未重新启动 dev server；以前序 HTTP 冒烟和构建验证为准。当前环境未安装 Python Playwright，未执行截图级自动化验证。

## 14. 本轮增量验证（2026-06-20 流程任务列表页去 AI 化）

- 变更范围：`src/components/flow/FlowStats.vue`、`src/components/flow/FlowTaskCardList.vue`、`src/views/flow/todo.vue`、`src/views/flow/done.vue`、`src/views/flow/started.vue`、`src/views/flow/cc.vue`。
- 验证重点：
  - 待办、已办、我发起、我抄送列表从表格式布局切换为参考图风格的审批任务条目。
  - 顶部统计入口从渐变卡片改为克制页签条，减少装饰和纵向占用。
  - 批量选择、清空、搜索、筛选、刷新和分页统一收口到 `FlowTaskCardList`。
  - 抄送页保留“抄送给我的 / 我发送的”二级切换，并与新列表风格保持一致。
  - 待办页仍通过“去审批”进入详情，不做列表批量同意/驳回，避免绕过表单、意见、签名和审批策略校验。
- 必跑命令：
  - `git diff --check`。
  - `pnpm exec eslint src/components/flow/FlowStats.vue src/components/flow/FlowTaskCardList.vue src/views/flow/todo.vue src/views/flow/done.vue src/views/flow/started.vue src/views/flow/cc.vue`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
- 浏览器验证：启动 Vite dev server 到 `http://127.0.0.1:3000/`，首页和本轮动态模块 HTTP 冒烟均返回 200；当前环境未安装 Playwright，未执行截图级自动化验证。

## 15. 本轮增量验证（2026-06-20 列表密度与待办快捷操作修正）

- 变更范围：`src/components/flow/FlowTaskCardList.vue`、`src/views/flow/todo.vue`、`src/views/flow/done.vue`、`src/views/flow/started.vue`、`src/views/flow/cc.vue`。
- 验证重点：
  - 列表页面移除顶部跨页面统计区，不再展示“已办任务 / 发起的流程 / 抄送我的”等无关计数。
  - 页面级左右 padding 从 20px 收紧到 12px，任务条目高度、内边距和卡片间距同步压缩。
  - 待办列表增加单条“驳回 / 同意”和选中后的批量“驳回 / 同意”。
  - 快捷审批复用现有 `approveTask` / `rejectTask` API，候选任务先签收；需要动态表单、外部表单或手写签名的任务提示进入详情处理。
  - 已办、我发起、抄送列表保留当前页必要提示，去掉跨页统计请求。
- 必跑命令：
  - `git diff --check`。
  - `pnpm exec eslint src/components/flow/FlowTaskCardList.vue src/views/flow/todo.vue src/views/flow/done.vue src/views/flow/started.vue src/views/flow/cc.vue`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
- 浏览器验证：启动 Vite dev server 到 `http://127.0.0.1:3000/`，首页以及 `todo.vue`、`done.vue`、`started.vue`、`cc.vue` 动态模块 HTTP 冒烟均返回 200；当前环境未安装 Playwright，未执行截图级自动化验证。

## 16. 本轮增量验证（2026-06-20 列表页面宽度适配）

- 变更范围：`src/components/flow/FlowTaskCardList.vue`、`src/views/flow/todo.vue`、`src/views/flow/done.vue`、`src/views/flow/started.vue`、`src/views/flow/cc.vue`。
- 验证重点：
  - 四个列表页横向 padding 清零，页面内容按当前容器宽度铺开。
  - `FlowTaskCardList` 设置 `width: 100%`，避免共享列表组件自身产生二次收缩。
  - 任务条目内部横向 padding、列间距和元信息间距继续压缩，减少左侧复选框和右侧操作区留白。
- 必跑命令：
  - `git diff --check`。
  - `pnpm exec eslint src/components/flow/FlowTaskCardList.vue src/views/flow/todo.vue src/views/flow/done.vue src/views/flow/started.vue src/views/flow/cc.vue`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
- 浏览器验证：本轮未启动 dev server；以前序 HTTP 冒烟和本轮构建验证为准。当前环境未安装 Playwright，未执行截图级自动化验证。

## 17. 本轮增量验证（2026-06-20 父级布局留白修正）

- 变更范围：`src/utils/flow-task-layout.js`、`src/layouts/top-menu/index.vue`、`src/layouts/top-side-menu/index.vue`、`src/layouts/full/index.vue`、`src/layouts/simple/index.vue`、`src/layouts/bento/index.vue`、`src/layouts/immersive/index.vue`、`src/layouts/nexus/index.vue`。
- 验证重点：
  - 默认 `top-menu` 布局下，流程任务列表不再被外层 `p-12` 内容区压窄。
  - `top-side-menu`、`full`、`simple`、`bento`、`immersive`、`nexus` 布局切换时，同一批流程任务列表路由也取消外层内容 padding。
  - flush 逻辑只对 `/flow/todo`、`/flow/done`、`/flow/started`、`/flow/cc` 生效，不影响其它后台页面通用间距。
  - 路由判断收口到共享工具，避免每个布局重复维护路径常量。
- 必跑命令：
  - `git diff --check`。
  - `pnpm exec eslint src/utils/flow-task-layout.js src/layouts/top-menu/index.vue src/layouts/top-side-menu/index.vue src/layouts/full/index.vue src/layouts/simple/index.vue src/layouts/bento/index.vue src/layouts/immersive/index.vue src/layouts/nexus/index.vue`。
  - `pnpm exec eslint src/components/flow/FlowTaskCardList.vue src/views/flow/todo.vue src/views/flow/done.vue src/views/flow/started.vue src/views/flow/cc.vue`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
- 浏览器验证：启动 Vite dev server 到 `http://127.0.0.1:3000/`，`/flow/todo`、`top-menu` 布局模块、新增工具模块和 `todo.vue` 动态模块 HTTP 冒烟均返回 200；当前环境未执行截图级自动化验证。

## 18. 本轮增量验证（2026-06-20 列表不溢出满宽修正）

- 变更范围：`src/components/flow/FlowTaskCardList.vue`。
- 验证重点：
  - 撤销列表容器超过父容器的左右外扩，避免任务列表横向溢出页面。
  - 列表组件自身保持 `width: 100%`，只占当前内容区可用宽度。
  - Naive UI loading 容器、任务栈和任务行显式 `width: 100%`，避免内部中间层收缩。
  - 工具栏和任务行保留低 padding，保持可用宽度但不产生横向滚动。
- 必跑命令：
  - `git diff --check`。
  - `pnpm exec eslint src/components/flow/FlowTaskCardList.vue src/views/flow/todo.vue src/views/flow/done.vue src/views/flow/started.vue src/views/flow/cc.vue`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
- 浏览器验证：本轮未重新启动 dev server；以前序 HTTP 冒烟和本轮构建验证为准。当前环境未执行截图级自动化验证。

## 19. 本轮增量验证（2026-06-20 已办和发起列表动作弱化）

- 变更范围：`src/components/flow/FlowTaskCardList.vue`、`src/views/flow/done.vue`、`src/views/flow/started.vue`。
- 验证重点：
  - 已办列表右侧“查看详情”从 Naive 小号按钮改为行内“详情 + 箭头”动作。
  - 我发起列表右侧“查看进度”从 Naive 小号按钮改为行内“进度 + 箭头”动作。
  - 行内动作透明背景、无边框，只在 hover 时出现浅色底，避免和任务条目里的主操作按钮抢层级。
- 必跑命令：
  - `git diff --check`。
  - `pnpm exec eslint src/components/flow/FlowTaskCardList.vue src/views/flow/done.vue src/views/flow/started.vue`。
- 浏览器验证：本轮仅做模板与 CSS 小范围修正，未重新启动 dev server；完整构建沿用前序验证。

## 20. 本轮增量验证（2026-06-20 列表动作统一右对齐）

- 变更范围：`src/components/flow/FlowTaskCardList.vue`、`src/views/flow/todo.vue`。
- 验证重点：
  - 列表行右侧动作区统一 `justify-content: flex-end`，已办和发起列表单动作保持行尾对齐。
  - 待办列表的“签收 / 驳回 / 同意 / 审批”从 Naive 小按钮改为同款轻量行内动作。
  - 待办动作通过 `info / danger / success / primary` 颜色区分，保留业务操作辨识度。
  - 窄屏下动作区独占一行并右对齐，避免换行后左贴边。
- 已执行命令：
  - `git diff --check`。
  - `pnpm exec eslint src/components/flow/FlowTaskCardList.vue src/views/flow/todo.vue src/views/flow/done.vue src/views/flow/started.vue`。
- 跳过项：本轮仅改模板与 CSS，未重新执行长构建；前序列表样式调整已完成构建验证。

## 19. 本轮增量验证（2026-06-20 已办与发起列表行内动作优化）

- 变更范围：`src/components/flow/FlowTaskCardList.vue`、`src/views/flow/done.vue`、`src/views/flow/started.vue`。
- 验证重点：
  - 已办列表右侧“查看详情”从 Naive 次级按钮改为轻量“详情 + 箭头”行内动作。
  - 我发起的列表右侧“查看进度”从 Naive 次级按钮改为轻量“进度 + 箭头”行内动作。
  - 新动作去掉边框和填充底色，仅保留 hover 浅色反馈，避免与待办的同意/驳回主操作混淆。
- 已执行命令：
  - `git diff --check`。
  - `pnpm exec eslint src/components/flow/FlowTaskCardList.vue src/views/flow/done.vue src/views/flow/started.vue`。
- 跳过项：本轮仅小范围模板和 CSS 调整，且前序构建已通过；未重新执行长构建。

## 21. 本轮增量验证（2026-06-20 查看流程图节点图标放大）

- 变更范围：`src/components/flow-designer/nodes/NodeCard.vue`。
- 验证重点：
  - 只读流程查看器节点图标从 42px 放大到 52px，解决待办、已办、我发起详情页查看流程图时图标看不清的问题。
  - 只读态图标字体单独提升到 30px，并同步调整标题行对齐、间距、圆角和阴影，避免图标变大后挤压节点名称和状态。
  - 调整仅作用于 `readonly` 节点卡片，不影响流程设计器编辑态节点删除、选中和 hover 行为。
- 已执行命令：
  - `git diff --check`。
  - `pnpm exec eslint src/components/flow-designer/nodes/NodeCard.vue src/components/flow-designer/viewer/DingFlowViewer.vue`。
- 跳过项：本轮为只读态 CSS 微调，未重新执行长构建；前序流程设计器与列表样式调整已通过完整构建。

## 22. 本轮增量验证（2026-06-20 查看流程图隐藏配置摘要与详情瘦身）

- 变更范围：`src/components/flow-designer/nodes/NodeCard.vue`、`src/components/flow-designer/viewer/NodeDetailPopover.vue`、相关单测。
- 验证重点：
  - 流程图查看态 `readonly` 节点不再显示 `subtitle`、默认插槽和 `title-extra`，避免暴露 SPEL、表达式、会签等流程配置内容。
  - 节点详情弹层从宽版卡片网格改为 360px 紧凑信息列表，只展示运行态处理人、开始/完成时间、结果和审批意见。
  - 新增单测覆盖“readonly 隐藏配置摘要”和“紧凑详情布局”。
- 已执行命令：
  - `git diff --check`。
  - `pnpm exec eslint src/components/flow-designer/nodes/NodeCard.vue src/components/flow-designer/viewer/NodeDetailPopover.vue src/components/flow-designer/nodes/__tests__/node-cards.spec.js src/components/flow-designer/viewer/__tests__/NodeDetailPopover.spec.js src/components/flow-designer/viewer/DingFlowViewer.vue`。
  - `pnpm vitest run src/components/flow-designer/nodes/__tests__/node-cards.spec.js src/components/flow-designer/viewer/__tests__/NodeDetailPopover.spec.js`。
- 跳过项：本轮为查看态模板、CSS 和单测小范围改动，未重新执行长构建；前序流程设计器改造已通过完整构建。

## 23. 本轮增量验证（2026-06-20 流程模型列表按钮去 AI 化）

- 变更范围：`src/views/flow/model.vue`。
- 验证重点：
  - 顶部工具区从 Naive 默认主按钮收敛为轻量“查询 / 清空 / 新增模型”工具按钮，新增模型保留主操作但降低装饰感。
  - 模型卡片右侧“设计 / 部署 / 实例 / 更多”从默认小按钮改为行内轻量操作和图标更多按钮，减少高饱和按钮块。
  - 空状态入口复用新的主操作按钮；移动端工具按钮独占一行并自适应宽度，避免挤压筛选项。
- 已执行命令：
  - `git diff --check`。
  - `pnpm exec eslint src/views/flow/model.vue`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
- 警告与跳过项：构建通过；仍有既有 CSS `//padding` 注释 warning 和 `src/store/index.js` 动静态混合导入 warning，本轮未引入。未启动 dev server，未执行截图级自动化验证。

## 24. 本轮增量验证（2026-06-20 流程监控行操作按钮统一）

- 变更范围：`src/views/flow/monitor.vue`。
- 验证重点：
  - 流程实例监控主表的“详情”从 Naive text 按钮改为轻量行内操作，并增加右箭头。
  - “更多”从文字按钮改为 28px 图标按钮，与流程模型列表页的更多操作风格保持一致。
  - 保留原有下拉菜单能力：流程图、变量、错误日志、管理、删除流程数据。
  - 删除中 disabled 状态仍可用，避免批量删除时触发详情或更多菜单。
- 已执行命令：
  - `git diff --check`。
  - `pnpm exec eslint src/views/flow/monitor.vue`。
- 跳过项：本轮为单页 render 函数和 CSS 小范围样式调整；前一轮已完成 `pnpm build`，本轮未重复执行长构建，未启动 dev server 和截图验证。

## 25. 本轮增量验证（2026-06-20 流程监控按钮按已办页样式二次统一）

- 变更范围：`src/views/flow/monitor.vue`。
- 验证重点：
  - 流程监控主表“详情 / 更多”按钮对齐已办页 `task-row-link-action` 视觉：青绿色文字、透明底、浅青 hover、14px 加粗文字。
  - “详情”保留右箭头，“更多”改为文字加下拉图标，避免只有灰色图标导致操作辨识度不足。
  - 操作区保持右对齐，disabled 状态仍降低透明度并禁止点击。
- 已执行命令：
  - `git diff --check`。
  - `pnpm exec eslint src/views/flow/monitor.vue`。
- 跳过项：本轮为流程监控单页按钮样式微调，前一轮已完成 `pnpm build`，本轮未重复执行长构建，未启动 dev server 和截图验证。

## 26. 本轮增量验证（2026-06-20 流程监控表格 render 按钮样式命中修正）

- 变更范围：`src/views/flow/monitor.vue`。
- 验证重点：
  - `NDataTable` 列 `render` 生成的按钮通过 `:deep(.monitor-row-link-action)` 匹配，避免 scoped 样式只编译为带 scope 属性的选择器后无法命中表格内部 DOM。
  - 颜色使用 `#177c7d !important`，文字和图标通过 `currentColor` 继承，覆盖默认灰色按钮文本。
  - hover 状态继续保持浅青底和深青文字，disabled 状态保持半透明。
- 已执行命令：
  - `git diff --check`。
  - `pnpm exec eslint src/views/flow/monitor.vue`。
- 跳过项：本轮只修正 CSS 选择器命中问题和颜色覆盖，未重复执行长构建，未启动 dev server 和截图验证。

## 27. 本轮增量验证（2026-06-20 待办列表回退恢复）

- 变更范围：`src/views/flow/todo.vue`。
- 验证重点：
  - 从 `dist/assets/todo-B8Vb9fog.js` 中残留的新版待办页面产物恢复源码结构。
  - 待办入口重新使用 `FlowTaskCardList`，恢复任务条目式列表、搜索、分类/状态筛选、刷新、分页和选中态。
  - 恢复单条“签收 / 驳回 / 同意 / 审批”和批量“驳回 / 同意”快捷操作。
  - 快捷操作会先签收候选任务；需要动态表单、外部表单或手写签名的任务会跳过并提示进入详情处理。
  - 待办详情恢复为 `FlowTaskDetailShell` 全屏展开，保留动态表单、外部表单、签名、转办和流程图能力。
- 已执行命令：
  - `pnpm exec eslint src/views/flow/todo.vue`。
  - `git diff --check`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
- 警告与跳过项：构建通过；仍有既有 CSS `//padding` 注释 warning 和 `src/store/index.js` 动静态混合导入 warning，本轮未引入。未启动 dev server 和截图验证。

## 28. 本轮增量验证（2026-06-20 首页待办任务列表去 AI 化）

- 变更范围：`src/views/home/index.vue`。
- 验证重点：
  - 首页 `/home` 待办任务从装饰化卡片行改为更像工作台的扁平任务列表。
  - 列表行信息层级调整为“状态 / 标题与业务元信息 / 时间与处理动作”，减少渐变、阴影和横向漂移效果。
  - “查看全部”收口为右上角轻量“全部”入口，单条任务使用“处理 + 箭头”行内动作。
  - 从首页进入待办详情时传递 `taskId` 查询参数，匹配待办页当前详情打开逻辑。
  - 移动端和暗色模式保留可读性与点击区域。
- 已执行命令：
  - `pnpm exec eslint src/views/home/index.vue`。
  - `git diff --check`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
- 警告与跳过项：构建通过；仍有既有 CSS `//padding` 注释 warning 和 `src/store/index.js` 动静态混合导入 warning，本轮未引入。未启动 dev server 和截图验证。

## 29. 本轮增量验证（2026-06-20 我的待办状态颜色调整）

- 变更范围：`src/views/flow/todo.vue`。
- 验证重点：
  - 我的待办列表状态标签不再复用通用 `default/info` 灰蓝色。
  - 待签收状态改为琥珀色，待处理状态改为青绿色，增强辨识度。
  - 审批详情头部状态图标同步使用待办页专用状态色。
  - 已办、我发起、抄送等其它列表仍使用各自原有状态类，不被本轮颜色调整影响。
- 已执行命令：
  - `pnpm exec eslint src/views/flow/todo.vue`。
  - `git diff --check`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
- 警告与跳过项：构建通过；仍有既有 CSS `//padding` 注释 warning 和 `src/store/index.js` 动静态混合导入 warning，本轮未引入。未启动 dev server 和截图验证。

## 30. 本轮增量验证（2026-06-20 审批详情流程名称展示）

- 变更范围：`src/views/flow/todo.vue`、`FlowTask` 实体、`FlowTaskMapper`、`FlowTaskServiceImpl`、`FlowTaskMapper.xml`。
- 验证重点：
  - 待办审批详情不再把 `businessType` / `leave_multi` 当作“流程分类”展示。
  - 任务列表和详情接口通过 `sys_flow_model.model_name` 返回 `processName`。
  - 详情字段改为“流程名称”，优先展示 `processName`，异常数据再回退到原有字段。
  - `selectByTaskId` 从注解 SQL 迁移到 Mapper XML，符合项目复杂查询统一写 XML 的约定。
- 已执行命令：
  - `pnpm exec eslint src/views/flow/todo.vue`。
  - `git diff --check`。
  - `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am compile -DskipTests`。
  - `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。
- 警告与跳过项：首次未指定 Java 17 的 Maven 编译因当前 shell JDK 不支持 target 17 失败；按项目标准指定 Java 17 后编译通过。前端构建通过，仍有既有 CSS `//padding` 注释 warning 和 `src/store/index.js` 动静态混合导入 warning，本轮未引入。未启动 dev server 和截图验证。
