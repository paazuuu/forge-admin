# 测试计划：business-flow-lowcode-integration

## 本轮增量验证：2026-06-28

### 变更范围

- 后端流程绑定协议增加 `businessBinding`。
- 保存流程绑定时自动从低代码发布配置和单据配置补齐业务表、主键、租户字段、状态字段和负责人字段。
- 流程绑定查询和旧兼容配置输出回显 `businessBinding`。
- 流程发起和回调时通过 `businessBinding.statusField` 回写低代码业务表流程状态。

### P0 验证

1. 后端 generator 插件及依赖模块编译。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

2. Git diff 空白检查。

```bash
git diff --check
```

## 本轮补充验证：2026-06-28 会签部署与分支图形重排

### 变更范围

- 修复流程设计器导出多实例人工节点时丢失 `flowable:collection` / `flowable:elementVariable` 的问题；缺失集合时兜底写出 `loopCardinality`，避免 Flowable 部署报 `flowable-multi-instance-missing-collection`。
- 采购单样例 `purchase_countersign` 会签节点往返转换后继续保留 `${countersignUserList}` 和 `assignee`。
- 画布布局层将条件/并行/包容网关按 44px 菱形锚点计算，连线端点连接到真实菱形边界，避免视觉断线。
- 设计器关闭 SVG 连线层的重复条件标签，分支标签改为按连线路径放置并做碰撞避让，避免“条件已设 / 默认”和加号互相遮挡。
- 补充复杂驳回回路组件单测，锁定分支标签不与加号或彼此重叠。

### P0 验证

1. 流程设计器与 BPMN 转换定向单测。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec vitest run \
  src/components/flow-designer/converter/__tests__/layout-algorithm.spec.js \
  src/components/flow-designer/canvas/__tests__/layout-engine.spec.js \
  src/components/flow-designer/canvas/__tests__/EdgePath.spec.js \
  src/components/flow-designer/converter/__tests__/user-task-parser-multi-instance.spec.js \
  src/components/flow-designer/converter/__tests__/json-to-bpmn.spec.js \
  src/components/flow-designer/converter/__tests__/roundtrip.spec.js \
  src/components/flow-designer/__tests__/DingFlowDesigner.spec.js \
  src/components/flow-designer/nodes/__tests__/node-cards.spec.js
```

2. 采购单样例 BPMN 往返转换检查。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
node --input-type=module <inline-check-script>
```

3. 前端生产构建。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

4. Git diff 空白检查。

```bash
git diff --check
```

### 跳过项

- 未启动 Admin/Flow 做真实部署联调：用户要求最后统一验证；本轮通过样例 BPMN 往返转换确认会签节点导出的 Flowable 多实例必要属性存在。
- 未做浏览器截图：流程配置页依赖登录态和后端模型数据；本轮通过画布坐标、组件 DOM 位置和生产构建覆盖展示层回归。

### 跳过项

- 未启动后端服务做接口验证：本轮只落协议保存/回显和默认补齐逻辑，未改 Controller 路径和数据库结构。
- 未做前端构建：本轮未改前端页面。

## 本轮增量验证：2026-06-28 前端业务绑定面板

### 变更范围

- `BusinessFlowBindingPanel.vue` 增加业务记录绑定配置区。
- 保存流程绑定时提交 `businessBinding`。
- 流程绑定回显时归一化旧配置，并为低代码对象推断状态字段、标题字段和负责人字段。
- 运行摘要展示业务绑定模式、业务表和状态字段。

### P0 验证

1. 前端生产构建。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

2. 后端 generator 插件及依赖模块编译。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

3. Git diff 空白检查。

```bash
git diff --check
```

### 跳过项

- 不启动 Vite 做浏览器截图：本轮 UI 改动为配置项表单和保存协议，先以生产构建覆盖模板/脚本合法性。
- 不启动后端服务做接口联调：本轮沿用已存在 `PUT /ai/business/flow/binding/{objectCode}`，无 Controller 路径变更。

## 本轮增量验证：2026-06-28 节点表单资产与字段权限

### 变更范围

- 后端流程绑定协议增加 `nodeForms` 保存、归一化和回显。
- 后端新增 `GET /ai/business/flow/form-assets/{objectCode}`，从业务对象 `formDesignerSchema` 读取低代码表单资产和字段目录。
- 流程变量候选接口同时返回 BPMN 人工节点清单，供节点表单策略自动生成节点行。
- `BusinessFlowBindingPanel.vue` 增加“节点表单策略”，支持低代码表单资产下拉、代码适配器表单编码、外部表单 URL 和节点字段可见/可编辑/必填配置。

### P0 验证

1. 后端 generator 插件及依赖模块编译。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

2. 前端生产构建。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

3. Git diff 空白检查。

```bash
git diff --check
```

### 跳过项

- 不启动后端服务做接口联调：本轮无数据库结构变更，且本地服务依赖 Flow/Admin 多服务同时启动，先以目标模块编译覆盖接口签名、DTO 和 JSON 归一化合法性。
- 不启动 Vite 做浏览器截图：本轮前端为配置表单和协议保存，生产构建可覆盖模板、脚本和组件导入合法性。

## 本轮增量验证：2026-06-28 待办业务表单运行态

### 变更范围

- 后端新增业务待办表单上下文查询和保存 DTO/VO。
- `BusinessFlowController` 新增 `GET/PUT /ai/business/flow/task-form-context`。
- `BusinessFlowService` 支持通过流程实例关联或 `businessKey` 解析业务记录、读取节点 `nodeForms`、输出低代码表单字段权限和保存可编辑字段。
- `business-app.js` 增加待办业务表单上下文 API。
- `todo.vue` 在待办审批详情中渲染低代码业务表单，审批“同意”前校验并保存节点授权字段，快捷同意遇到业务可写字段时提示进入详情办理。

### P0 验证

1. 后端 generator 插件及依赖模块编译。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

2. 前端生产构建。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

3. Git diff 空白检查。

```bash
git diff --check
```

### 跳过项

- 不启动 Admin/Flow 服务做接口联调：本轮无数据库结构变更，完整链路需要已发布低代码对象、已部署流程模型、待办任务和登录态数据，本地当前不具备稳定验收数据。
- 不启动 Vite 做浏览器截图：本轮待办页改动通过生产构建覆盖模板、脚本、组件导入和类型解析；后续有真实流程样例后再补浏览器级交互验收。

## 本轮增量验证：2026-06-28 任务身份校验、代码表单 Provider 与重提接口

### 变更范围

- `FlowClient` 增加任务详情查询能力，业务待办表单上下文按 `taskId` 校验任务状态、当前办理人、流程实例、业务 Key 和节点 Key。
- `BusinessTaskFormContextQueryDTO`、`BusinessTaskFormSaveDTO`、`BusinessTaskFormContextVO` 补充 `taskId`，保存业务字段和重提必须由当前已签收办理人执行。
- 新增 `BusinessCodeFormProvider`、`BusinessCodeFormProviderRegistry`，代码优先复杂业务可通过 Spring Bean 注册代码表单资产、上下文和保存逻辑。
- `BUSINESS_CODE_FORM` 待办上下文支持 Provider 委派；未注册 Provider 时返回明确 warning，并支持从待办打开业务页处理。
- 新增 `POST /ai/business/flow/resubmit`，复杂业务页保存主数据后可复用原流程实例完成修改节点重提。
- `todo.vue` 对代码表单和存在可写业务字段的任务禁止快捷同意，要求进入详情或业务页办理。

### P0 验证

1. 后端 generator 插件及依赖模块编译。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

2. 前端生产构建。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

3. Git diff 空白检查。

```bash
git diff --check
```

### 跳过项

- 不继续做低代码应用/设计器整体改造：按本轮用户要求，低代码应用完整链路改造暂缓，只保留已完成的流程配置和待办运行态桥接。
- 不启动 Admin/Flow 服务做接口联调：完整重提链路需要已部署流程模型、驳回到修改节点的待办任务、登录态和业务样例数据，本地当前不具备稳定验收数据。
- 不新增采购合同示例 Provider：本轮先落平台扩展点和运行协议，具体业务示例留给后续业务模块接入时实现。

## 本轮增量验证：2026-06-28 流程完成事件变量快照兜底

### 变更范围

- `FlowTaskEventListener#handleProcessCompleted` 发布完成事件前合并当前执行变量、运行时变量和历史变量，避免 `PROCESS_COMPLETED` 事件缺失最后一次审批提交变量。
- `FlowTaskEventListener#handleTaskCompleted` 读取任务变量失败时继续通过流程实例和历史变量兜底。
- `FlowInstanceServiceImpl#getProcessVariables` 支持流程结束后按历史变量读取。
- `FlowMonitorServiceImpl#getProcessVariables` 与监控 Controller 行为对齐，运行时优先、历史变量兜底。

### P0 验证

1. 后端 flow 插件及依赖模块编译。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am compile -DskipTests
```

2. Flow 监听器测试尝试执行。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -Dtest=FlowTaskEventListenerTest -DskipTests=false -Dmaven.test.skip=false test
```

3. Git diff 空白检查。

```bash
git diff --check
```

### 跳过项

- 未启动 Flow/Admin 做真实流程回调联调：需要可完成的流程实例、最后节点提交变量和业务侧 `@FlowCallback` 样例，留待统一验证。
- `FlowEventSubscriber` 仍是 Redis Pub/Sub 即时消费，业务回调异常后只记录日志；本轮先消除变量缺失导致的回调失败，持久化事件重试/死信队列需单独设计。

## 本轮增量验证：2026-06-28 采购单审批测试样例

### 变更范围

- 新增代码优先业务样例 `sample_purchase_order`，用于验证手写业务模块接入流程。
- 新增采购单分页、详情、新增、编辑、删除、提交审批、待办节点字段保存和流程初始化接口。
- 新增采购单审批 BPMN：部门负责人审批、工程部经理审批、会签、申请人修改、驳回修改、申请人终止、审批通过。
- 新增代码表单 Provider，向业务表单资产目录注册“采购单审批表单”。
- 新增前端测试页 `/business/purchase-order-test`，同一个组件支持列表工作台和 Flow 外置待办审批表单。
- 新增 Flyway 脚本，创建测试业务表、状态字典、总经理角色、菜单和按钮权限。

### P0 验证

1. 后端业务模块与 FlowClient 编译。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-business/forge-business-core,forge-flow/forge-flow-client -am compile -DskipTests
```

2. 前端生产构建。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

3. Git diff 空白检查。

```bash
git diff --check
```

### 跳过项

- 未启动 Admin/Flow 服务做真实流程接口联调：用户要求最后统一验证，本轮先完成样例代码和构建验证。
- 未对低代码应用设计器继续改造：按用户要求，低代码应用改造暂缓。

## 本轮增量验证：2026-06-28 钉钉流程图分支布局修复

### 变更范围

- 修复流程图布局算法把“驳回修改后重提”的回退入口误当成普通汇合点，导致首个审批节点被排到未访问节点补齐列的问题。
- 对没有即时汇合点的审批结果网关，按“默认分支继续主线、非默认分支侧向展开”排布，覆盖采购单审批的通过/驳回场景。
- 将条件/并行/包容网关从普通任务卡视觉调整为轻量分支锚点，降低与审批办理节点混淆。
- 补充驳回重提回路布局单测。

### P0 验证

1. 流程设计器定向单测。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec vitest run \
  src/components/flow-designer/converter/__tests__/layout-algorithm.spec.js \
  src/components/flow-designer/canvas/__tests__/layout-engine.spec.js \
  src/components/flow-designer/nodes/__tests__/node-cards.spec.js \
  src/components/flow-designer/canvas/__tests__/NodeRenderer.spec.js \
  src/components/flow-designer/__tests__/DingFlowDesigner.spec.js
```

2. 前端生产构建。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

3. Git diff 空白检查。

```bash
git diff --check
```

### 跳过项

- 未启动 Vite/Playwright 截图：项目当前没有独立 Storybook/Playwright 入口，流程配置页依赖登录态和后端流程模型数据；本轮用坐标级布局单测、组件单测和生产构建覆盖。

## 本轮补充验证：2026-06-28 钉钉流程图标签与加号避让

### 变更范围

- 分支条件标签从目标节点中心线移到网关与目标节点之间的分支线中段，避免和目标节点下方的普通添加按钮重叠。
- 垂直默认分支标签向左避让，避免和居中的添加分支按钮重叠。
- 添加分支按钮从网关右侧偏移改为网关中心线定位，并根据下游目标选择合适的分支线 y 坐标。
- 补充 `DingFlowDesigner` 组件单测，锁定“添加分支按钮居中、默认标签避开按钮”的布局约束。

### P0 验证

1. 流程设计器定位相关定向单测。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec vitest run \
  src/components/flow-designer/__tests__/DingFlowDesigner.spec.js \
  src/components/flow-designer/canvas/__tests__/AddNodeButton.spec.js \
  src/components/flow-designer/converter/__tests__/layout-algorithm.spec.js \
  src/components/flow-designer/canvas/__tests__/layout-engine.spec.js
```

2. 前端生产构建。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

3. Git diff 空白检查。

```bash
git diff --check
```

## 本轮增量验证：2026-06-28 采购单详情 Snowflake ID 精度修复

### 变更范围

- 修复采购单测试页从流程待办进入外部表单时，前端将后端雪花 ID 转成 JS `Number` 导致精度丢失，进而 `POST /business/sample-purchase-order/getById` 查不到采购单的问题。
- 采购单详情接口增加 `businessKey` 查询入口，待办表单优先通过 `businessKey` 定位记录，只有缺失业务键时才用字符串形式的 `id` 兜底。
- 采购单前端 API 对详情、提交、删除等 ID 参数统一按字符串传递，列表详情/编辑优先使用行上的 `businessKey`。
- 用户选择器返回的审批人 ID、会签人 ID 保持字符串传给后端，由 Jackson 反序列化为 `Long`，避免用户 ID 精度风险。

### P0 验证

1. 后端业务模块与 FlowClient 编译。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-business/forge-business-core,forge-flow/forge-flow-client -am compile -DskipTests
```

2. 前端生产构建。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

3. 空白检查和 ID 精度风险搜索。

```bash
git diff --check
rg -n "Number\((props|row|record|detail|currentSubmitRow|.*Id)|Number\(.*\.id|Number\(.*recordId|Number\(.*businessKey|Number\(.*purchaseOrderId" \
  forge-admin-ui/src/views/business/purchase-order-test.vue \
  forge-admin-ui/src/api/business/purchase-order-test.js
```

### 跳过项

- 未启动 Admin/Flow 服务做真实待办接口联调：本地当前没有稳定的已部署流程实例、采购单样例数据和待办任务，用户要求最后统一验证；本轮通过构建和静态检查确认协议、类型和前端调用合法。

## 本轮增量验证：2026-06-28 已办表单只读查看与业务化条件配置

### 变更范围

- 待办外部采购单表单中“转办 / 同意 / 驳回修改”按钮统一为大尺寸按钮，并设置稳定最小宽度。
- `FlowTaskController` 新增 `GET /api/flow/task/form`，用于已办、抄送、历史查看等无运行中任务场景读取只读表单信息。
- `FlowTaskMapper/FlowTaskServiceImpl` 增加 `selectByIdOrTaskId` 兜底，已办页传 Flowable `taskId` 或 `sys_flow_task.id` 都能解析到对应历史任务和节点表单策略。
- `BusinessFlowController` 新增 `GET /ai/business/flow/task-form-context/readonly`，用于已办页读取业务对象/代码表单只读上下文，不触发待办办理人校验和保存能力。
- 已办详情页新增“表单内容”区块，支持只读展示外部业务表单、节点动态表单、低代码业务对象表单和代码业务表单入口。
- 普通 BPMN 节点属性面板的流转条件从“手写表达式”调整为业务人员可选的审批结果分支：同意通过、驳回修改、退回上一步、终止流程、按业务字段判断；表达式/脚本保留在“开发者高级配置（可选）”折叠面板中。
- 仿钉钉流程设计器的网关分支配置同步业务化，支持“审批结果 / 业务字段 / 开发者高级”三种模式；选择“驳回修改”自动写入 `${approvalResult == 'reject'}`，已有该表达式的分支标签回显为“驳回修改”。

### P0 验证

1. 后端 flow server 与 generator 插件编译。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-flow/forge-flow-server,forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

2. 前端生产构建。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

3. 仿钉钉流程设计器条件配置定向单测。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec vitest run \
  src/components/flow-designer/panel/__tests__/ConditionConfig.spec.js \
  src/components/flow-designer/__tests__/DingFlowDesigner.spec.js
```

4. Git diff 空白检查。

```bash
git diff --check
```

### 跳过项

- 未启动 Admin/Flow 服务做真实已办详情联调：完整验证需要已完成采购单流程实例和当前用户已办任务；本轮先通过目标模块编译和前端构建验证接口、模板和打包合法性。

## 本轮增量验证：2026-06-28 抄送节点配置驱动

### 变更范围

- 仿钉钉流程设计器 `CarbonCopyConfig.vue` 把抄送人从手工输入用户 ID 改为复用 `UserSelectPicker` 选择用户。
- 普通 BPMN `NodePropertiesPanel.vue` 的服务任务可勾选“作为抄送节点”，并通过用户选择弹窗配置抄送人。
- 抄送节点 BPMN 导出写入 `flowable:type="cc"`、`flowable:candidateUsers`、`flowable:candidateUserNames`，未配置自定义实现时自动挂 `flowable:delegateExpression="${flowCcNodeDelegate}"`。
- BPMN 导入可回显抄送人配置；画布卡片可展示抄送人或抄送角色摘要。
- 后端新增 `FlowCcNodeDelegate`，流程流转到抄送 serviceTask 时按节点配置发送抄送。

### P0 验证

1. 抄送节点导入导出和卡片摘要定向单测。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec vitest run \
  src/components/flow-designer/converter/__tests__/json-to-bpmn.spec.js \
  src/components/flow-designer/converter/__tests__/bpmn-to-json-multi.spec.js \
  src/components/flow-designer/nodes/__tests__/node-cards.spec.js
```

2. 后端 Flow 插件和 Flow 服务编译。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am compile -DskipTests

JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-flow/forge-flow-server -am compile -DskipTests
```

3. 前端生产构建。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

4. Git diff 空白检查。

```bash
git diff --check
```

### 跳过项

- 未启动 Admin/Flow 服务做真实抄送发送联调：完整验证需要已部署包含抄送 serviceTask 的流程模型、当前租户用户和抄送接收人数据；本轮先通过 BPMN 转换单测、前端构建和后端编译覆盖配置链路。

## 本轮增量验证：2026-06-28 抄送角色/表达式配置

### 变更范围

- 仿钉钉流程设计器抄送节点支持“指定人员 / 指定角色 / 表达式”三种抄送来源。
- 普通 BPMN 服务任务的“作为抄送节点”配置同步支持人员、角色和表达式。
- 抄送角色配置复用系统角色分页接口；表达式配置可选择返回人员或返回角色。
- `flowable-moddle.json` 补充 `flowable:type`、`flowable:ccReceiverType`、`flowable:ccExpressionTarget` 等属性，保证普通 BPMN 设计器序列化稳定。
- 后端抄送委托支持解析 `${ccUserIds}`、`${ccRoleKeys}`、`${user_123}` 和复杂 Flowable 表达式，并把角色解析为用户。
- “开发者高级配置（可选）”使用真实折叠面板承载服务任务实现配置，默认用户不需要编辑表达式或 Java 类。

### P0 验证

1. 前端定向 ESLint。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint \
  src/components/flow-designer/panel/CarbonCopyConfig.vue \
  src/components/bpmn/NodePropertiesPanel.vue \
  src/components/flow-designer/converter/json-to-bpmn.js \
  src/components/flow-designer/converter/bpmn-to-json.js
```

2. 抄送节点 BPMN 导入导出定向单测。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec vitest run \
  src/components/flow-designer/converter/__tests__/json-to-bpmn.spec.js \
  src/components/flow-designer/converter/__tests__/bpmn-to-json-multi.spec.js
```

3. 后端 Flow 插件编译。

```bash
cd forge-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
PATH="$JAVA_HOME/bin:$PATH" \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am compile -DskipTests
```

4. 前端生产构建。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

5. Git diff 空白检查。

```bash
git diff --check
```

### 跳过项

- 未启动 Admin/Flow 服务做真实角色抄送和表达式抄送联调：完整验证需要已部署流程、系统角色成员、流程变量和登录态数据；用户要求最后统一验证，本轮先以转换单测、前端构建和后端编译覆盖配置链路。

## 本轮补充验证：2026-06-28 抄送配置样式回归

### 变更范围

- 修复抄送来源三段选择错位：不再用 `n-radio-group` 强行 grid 布局，改为自定义三段按钮。
- 修复仿钉钉抄送节点“开发者高级配置”类型选择无法保持的问题：实现类型即使实现值为空也能正常回显。
- 去掉仿钉钉抄送配置中重复的“开发者高级配置”表单标签，折叠区只保留一处标题。
- 普通 BPMN 抄送来源选择同步使用自定义三段按钮，避免两套设计器表现不一致。

### P0 验证

1. 前端定向 ESLint。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint \
  src/components/flow-designer/panel/CarbonCopyConfig.vue \
  src/components/bpmn/NodePropertiesPanel.vue
```

2. 前端生产构建。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

3. Git diff 空白检查。

```bash
git diff --check
```

### 跳过项

- 未启动 Vite 做浏览器截图：本轮是局部样式和 computed 回显修复，先用定向 ESLint、生产构建和用户截图对应代码检查覆盖；真实页面由用户最后统一验证。
