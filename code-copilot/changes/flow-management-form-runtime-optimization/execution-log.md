# 执行记录 — 流程管理表单运行态与业务入口优化

## 2026-06-06 10:10 +0800 阶段验证

### 变更范围

- 新增流程表单版本、入口、表单实例、组织填报批次相关实体、Mapper/XML、Service、Controller 和 Flyway 脚本。
- 扩展流程表单分页、发布版本、字段目录接口。
- 增加 `PROCESS_ONLY` 入口提交运行态，保存表单实例快照并发起流程。
- 增加待办创建后的站内信推送和 `MessageService.sendIfAbsent` 幂等入口。
- 前端增加流程入口管理、入口运行填报页、组织填报批次页，补动态路由和 API。

### 执行命令与结果

1. 后端编译

```bash
cd forge
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-flow/forge-flow-server -am compile -DskipTests
```

结果：通过。Reactor 29 个模块全部 `SUCCESS`，输出 `BUILD SUCCESS`。

2. 前端构建

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过。Vite build 完成，输出 `✓ built in 1m 5s`，新增 `entry`、`fillBatch` 等 chunk 正常产出。

3. 空白检查

```bash
git diff --check -- forge-admin-ui/src/router/index.js forge-admin-ui/src/api/flow.js forge/db/migration/V1.0.57__add_flow_form_runtime_entry.sql
```

结果：通过。命令无输出，退出码 0。

### 警告项

- 前端构建存在一批既有 UnoCSS 图标加载告警，以及既有 CSS `//` 注释压缩告警；未阻断构建，未见新增页面导致的编译错误。
- 本地未启动 MySQL/Redis 和后端服务，本轮未执行 Flyway 实跑、登录后 curl 接口联调或浏览器点击验证。

### 跳过项

- `mvn test`：本轮为阶段实现验证，先按变更面执行目标模块编译。
- Playwright/UI 截图：需要可用后端数据和登录态，本轮未启动完整前后端服务。

### 服务清理

- 本轮未启动长期运行服务，无需停止进程。

## 2026-06-06 20:30 +0800 流程动态表单字段间距配置

### 变更范围

- 流程动态表单规则统一补默认 `wrap.style.marginBottom=20px`，预览、发起测试、待办办理等运行态会按该间距渲染。
- 流程表单设计器右侧“表单配置”增加“字段行间距（px）”配置，统一写入各字段 `wrap.style.marginBottom`。
- 字段属性面板不注入行间距配置，低代码表单设计器默认行为保持不变。

### 执行命令与结果

1. 空白检查

```bash
git diff --check -- forge-admin-ui/src/components/form-create/formCreateBridge.js forge-admin-ui/src/components/form-create/FlowFormCreateDesigner.vue forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js
```

结果：通过。命令无输出，退出码 0。

2. 配置扫描

```bash
rg -n "DEFAULT_FORM_ITEM_GAP|字段行间距|marginBottom|enableFormItemSpacing|formCreateWrap>style>marginBottom" forge-admin-ui/src/components/form-create forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js
```

结果：通过。默认值和表单级配置均已落地，字段属性扩展残留已清理。

3. 前端构建

```bash
/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build"
```

结果：通过。输出 `✓ built in 1m 5s`。

### 警告项

- 前端构建仍存在既有 UnoCSS 图标加载告警。
- 前端构建仍存在既有 CSS `//` 注释压缩告警。
- 前端构建仍存在既有 store 动静态混合导入 chunk 告警。
- 上述警告未阻断构建，本轮未处理。

### 跳过项

- 未启动浏览器实测右侧表单配置面板；本轮通过源码路径确认配置统一写入 `rule.wrap.style.marginBottom`，并通过构建验证。

### 服务清理

- 本轮未启动长期运行服务，无需停止进程。

## 2026-06-06 20:55 +0800 表单级行间距配置纠偏

### 变更范围

- 将流程动态表单“字段行间距（px）”从字段属性面板移到 fcDesigner 右侧“表单配置”Tab。
- 保存、预览刷新和表单配置变更时统一读取表单级行间距，并批量写入所有规则的 `wrap.style.marginBottom`。
- 清理 `forgeBusinessComponents.js` 中 `enableFormItemSpacing` 和 `formCreateWrap>style>marginBottom` 字段属性扩展，避免业务组件属性中继续出现“下方间距”。

### 执行命令与结果

1. 空白检查

```bash
git diff --check -- forge-admin-ui/src/components/form-create/FlowFormCreateDesigner.vue forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js code-copilot/changes/flow-management-form-runtime-optimization/spec.md code-copilot/changes/flow-management-form-runtime-optimization/tasks.md code-copilot/changes/flow-management-form-runtime-optimization/test-spec.md code-copilot/changes/flow-management-form-runtime-optimization/execution-log.md
```

结果：通过。命令无输出，退出码 0。

2. 字段级配置残留扫描

```bash
rg -n "enableFormItemSpacing|formCreateWrap>style>marginBottom|title: '下方间距'|右侧属性支持|按字段设置" forge-admin-ui/src/components/form-create forge-admin-ui/src/views/app-center/components/designer/form-first code-copilot/changes/flow-management-form-runtime-optimization
```

结果：通过。代码中无字段级行间距配置残留，仅执行记录保留历史命令文本。

3. 前端构建

```bash
/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build"
```

结果：通过。输出 `✓ built in 2m 9s`。

### 警告项

- 前端构建仍存在既有 UnoCSS 动态图标加载告警，未阻断构建。
- 前端构建仍存在既有 CSS `//` 注释压缩告警，未阻断构建。
- 前端构建仍存在既有 store 动静态混合导入 chunk 告警，未阻断构建。

### 跳过项

- 未启动浏览器实测 fcDesigner 表单配置 Tab；本轮以源码路径、残留扫描和前端构建验证为准。

### 服务清理

- 本轮未启动长期运行服务，无需停止进程。

## 2026-06-06 20:04 +0800 流程表单设计器业务组件对齐

### 变更范围

- `FlowFormCreateDesigner` 复用低代码 `installForgeBusinessComponents`，流程表单设计器左侧增加“业务组件”分组，包含系统字典选择、行政区划、组织选择、人员选择、文件/图片上传和引用对象等组件。
- 流程设计器注册业务组件时关闭低代码字段基础规则覆盖，保留流程表单字段作为流程变量自由编辑。
- `FlowFormCreateRenderer` 复用 `hydrateForgeBusinessPreviewRules`，在预览、发起测试、待办办理等运行渲染场景加载字典、组织树、人员列表等选项。
- `installForgeBusinessComponents` 增加可选配置参数，保持低代码设计器默认行为不变。

### 执行命令与结果

1. 空白检查

```bash
git diff --check -- forge-admin-ui/src/components/form-create/FlowFormCreateDesigner.vue forge-admin-ui/src/components/form-create/FlowFormCreateRenderer.vue forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js
```

结果：通过。命令无输出，退出码 0。

2. 业务组件引用扫描

```bash
rg -n "业务组件|字典选择|组织选择|人员选择|hydrateForgeBusinessPreviewRules|installForgeBusinessComponents" forge-admin-ui/src/components/form-create/FlowFormCreateDesigner.vue forge-admin-ui/src/components/form-create/FlowFormCreateRenderer.vue forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js
```

结果：通过。流程表单设计器和渲染器均已复用低代码业务组件注册/选项补齐逻辑。

3. 前端构建

```bash
/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build"
```

结果：通过。输出 `✓ built in 1m 20s`。

### 警告项

- 前端构建仍存在既有 UnoCSS 图标加载告警。
- 前端构建仍存在既有 CSS `//` 注释压缩告警。
- 前端构建仍存在既有 store 动静态混合导入 chunk 告警。
- 上述警告未阻断构建，本轮未处理。

### 跳过项

- 未启动浏览器实测组件面板和远程选项加载；本轮以构建和静态扫描验证，后续联调需在登录态下确认字典、组织、人员接口权限。

### 服务清理

- 本轮未启动长期运行服务，无需停止进程。

## 2026-06-06 19:48 +0800 流程模型测试按钮收敛

### 变更范围

- 移除流程模型卡片 footer 中外露的“发起测试”按钮，减少卡片操作区占用。
- 保留已部署模型更多操作中的“发起测试”和原有测试发起弹窗能力。
- 同步修正文档中“卡片和更多操作”的描述为“更多操作”。

### 执行命令与结果

1. 静态扫描

```bash
rg -n "发起测试|startTest" forge-admin-ui/src/views/flow/model.vue code-copilot/changes/flow-management-form-runtime-optimization/tasks.md code-copilot/changes/flow-management-form-runtime-optimization/test-spec.md code-copilot/changes/flow-management-form-runtime-optimization/execution-log.md
```

结果：通过。`flow/model.vue` 中“发起测试”只保留在更多操作、弹窗和相关处理函数中，卡片 footer 已无外露按钮。

2. 空白检查

```bash
git diff --check -- forge-admin-ui/src/views/flow/model.vue code-copilot/changes/flow-management-form-runtime-optimization/tasks.md code-copilot/changes/flow-management-form-runtime-optimization/test-spec.md code-copilot/changes/flow-management-form-runtime-optimization/execution-log.md
```

结果：通过。命令无输出，退出码 0。

3. 前端构建

```bash
/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build"
```

结果：通过。输出 `✓ built in 1m 11s`。

### 警告项

- 前端构建仍存在既有 UnoCSS 图标加载告警。
- 前端构建仍存在既有 CSS `//` 注释压缩告警。
- 前端构建仍存在既有 store 动静态混合导入 chunk 告警。
- 上述警告未阻断构建，本轮未处理。

### 跳过项

- 未启动浏览器做点击验证；本轮为按钮位置收敛，已用模板构建和静态扫描确认入口保留在更多操作中。

### 服务清理

- 本轮未启动长期运行服务，无需停止进程。

## 2026-06-06 18:47 +0800 入口边界与表单设计器增量验证

### 变更范围

- 流程模型页撤掉“入口配置”动作，不再挂载 `FlowEntryConfigModal`。
- 移除隐藏的 `/flow/entry-runtime/:entryCode` 手工路由和错误入口运行页，避免未闭环入口配置继续暴露。
- 表单设计器弹窗内容区改为固定全高，`FlowFormCreateDesigner` 默认跟随容器高度。
- `FlowFormCreateDesigner` 显式使用 `zh-cn` locale，并在规则归一化时把历史英文组件标题如 `Select` 转为中文默认标题。
- 同步修正 spec/tasks/test 中“流程模型页入口配置”的旧产品结论。

### 执行命令与结果

1. 空白检查

```bash
git diff --check -- forge-admin-ui/src/components/form-create/FlowFormCreateDesigner.vue forge-admin-ui/src/components/form-create/formCreateBridge.js forge-admin-ui/src/views/flow/design.vue forge-admin-ui/src/views/flow/model.vue forge-admin-ui/src/router/index.js code-copilot/changes/flow-management-form-runtime-optimization/spec.md code-copilot/changes/flow-management-form-runtime-optimization/tasks.md code-copilot/changes/flow-management-form-runtime-optimization/test-spec.md code-copilot/changes/flow-management-form-runtime-optimization/execution-log.md
```

结果：通过。命令无输出，退出码 0。

2. 入口配置引用扫描

```bash
rg "FlowEntryConfigModal|entry-runtime|入口配置收敛到流程模型页|流程模型页提供.*入口配置|模型页入口配置组件" forge-admin-ui/src code-copilot/changes/flow-management-form-runtime-optimization -n
```

结果：通过。前端代码无引用；仅任务说明和历史执行日志中保留删除/历史记录文本。

3. 前端构建

```bash
/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build"
```

结果：通过。输出 `✓ built in 54.18s`。

### 警告项

- 前端构建仍存在既有 UnoCSS 图标加载告警。
- 前端构建仍存在既有 CSS `//` 注释压缩告警。
- 前端构建仍存在既有 store 动静态混合导入 chunk 告警。
- 上述警告未阻断构建，本轮未处理。

### 跳过项

- 未启动 Vite/浏览器做 Playwright 截图；本轮以 Vue 编译、路由导入和静态引用扫描作为前端最小闭环。
- 未执行后端编译；本轮只调整前端产品暴露和文档。

### 服务清理

- 本轮未启动长期运行服务，无需停止进程。

## 2026-06-06 模型页测试发起与设计器配置区增量验证

### 变更范围

- 流程模型页在已部署模型的更多操作中保留“发起测试”，用于按当前模型级动态表单填写变量并启动测试流程；卡片 footer 不外露该按钮，减少操作区占用。
- 发起测试弹窗增加模型摘要、测试口径提示、动态表单渲染和“我发起的”跳转；提交时使用 `FLOW_MODEL_TEST` 和 `FLOW_TEST:` 业务键前缀，避免与正式业务入口混淆。
- 流程设计器顶部流程配置由默认折叠条改为常驻配置工作区，分为“流程属性 / 表单配置 / 说明”，表单配置直接展示表单类型、已有表单、设计表单、预览和配置状态。

### 执行命令与结果

1. 空白检查

```bash
git diff --check -- forge-admin-ui/src/views/flow/model.vue forge-admin-ui/src/views/flow/design.vue
```

结果：通过。命令无输出，退出码 0。

2. 前端构建

```bash
/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build"
```

结果：通过。输出 `✓ built in 55.81s`。

### 警告项

- 前端构建仍存在既有 UnoCSS 图标加载告警。
- 前端构建仍存在既有 CSS `//` 注释压缩告警。
- 前端构建仍存在既有 store 动静态混合导入 chunk 告警。
- 上述警告未阻断构建，本轮未处理。

### 跳过项

- 未启动后端服务和浏览器做真实发起流程联调；本轮以 Vue 构建和静态检查确认入口、弹窗和设计器配置区可编译。

### 服务清理

- 本轮未启动长期运行服务，无需停止进程。

## 2026-06-06 17:41 +0800 产品口径纠偏验证

### 变更范围

- 流程入口配置从独立 `flow/entry` 页面收敛到流程模型页的“入口配置”弹窗。
- 删除前端独立 `flow/entry.vue` 和 `flow/fillBatch.vue`，组织填报批次不再作为流程管理内置页面暴露。
- 运行填报页返回按钮改为返回流程模型。
- `V1.0.57` 不再注册“流程入口”“组织填报批次”独立菜单；新增 `V1.0.58` 清理已落库的误注册菜单和组织填报按钮资源。
- Spec/Tasks/Test Spec 已同步“组织填报是业务场景扩展，不是流程管理独立菜单”的口径。

### 执行命令与结果

1. 空白检查

```bash
git diff --check -- \
  forge-admin-ui/src/views/flow/model.vue \
  'forge-admin-ui/src/views/flow/entry-runtime.[entryCode].vue' \
  forge/db/migration/V1.0.57__add_flow_form_runtime_entry.sql \
  forge/db/migration/V1.0.58__remove_standalone_flow_entry_menus.sql \
  code-copilot/changes/flow-management-form-runtime-optimization/spec.md \
  code-copilot/changes/flow-management-form-runtime-optimization/tasks.md \
  code-copilot/changes/flow-management-form-runtime-optimization/test-spec.md
```

结果：通过。命令无输出，退出码 0。

2. 前端构建

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过，输出 `✓ built in 50.54s`。构建产出包含 `FlowEntryConfigModal` chunk。

### 警告项

- 前端构建仍存在既有 UnoCSS 图标加载告警、既有 CSS `//` 注释压缩告警，以及 store 动静态混合导入 chunk 告警；未阻断构建。

### 跳过项

- 本轮未启动后端服务、未执行 Flyway 实跑和浏览器交互验证；变更集中在前端入口收敛、SQL 菜单纠偏和文档口径同步。

### 服务清理

- 本轮未启动长期运行服务，无需停止进程。

## 2026-06-06 Flyway 校验修复

### 变更范围

- 恢复 `V1.0.57__add_flow_form_runtime_entry.sql` 中已经执行过的菜单和按钮资源内容，避免 Flyway checksum mismatch。
- 保持 `V1.0.58__remove_standalone_flow_entry_menus.sql` 承担菜单纠偏，符合“已执行迁移脚本不再修改”的规则。

### 执行命令与结果

```bash
git diff --check -- \
  forge/db/migration/V1.0.57__add_flow_form_runtime_entry.sql \
  forge/db/migration/V1.0.58__remove_standalone_flow_entry_menus.sql
```

结果：通过。命令无输出，退出码 0。

2. Flyway checksum 计算

```bash
java -classpath /private/tmp:/Users/yaomindong/.m2/repository/org/flywaydb/flyway-core/10.20.1/flyway-core-10.20.1.jar \
  FlywayChecksum forge/db/migration/V1.0.57__add_flow_form_runtime_entry.sql
```

结果：输出 `-1204205122`，与数据库已执行记录一致。

### 跳过项

- 未执行 Flyway 实跑；本轮修复目标是恢复已执行脚本内容并让后续启动继续执行 `V1.0.58`。

## 2026-06-06 21:35 +0800 Flyway 低版本补脚本历史缺口修复

### 问题

- 后端启动在 `flywayInitializer` 失败，表面链路表现为 `jobAutoRegistrar -> jobScheduler -> flywayInitializer` 依赖创建失败。
- 根因是开发库 `forge_schema_history` 已有 `1.0.57/1.0.58`，但本地解析到的 `V1.0.55`、`V1.0.56` 未在历史表中记录，Flyway 默认 `outOfOrder=false` 时拒绝启动。

### 处理

- 只读确认历史表：`1.0.54 -> 1.0.57 -> 1.0.58`，缺少 `1.0.55/1.0.56`。
- 只读确认 `sys_user_tenant`、`sys_file_group.tenant_id`、`sys_flow_model.tenant_id`、`idx_sys_flow_task_tenant` 等结构已存在，脚本具备重复执行保护。
- 使用临时 Flyway runner 打开 `outOfOrder=true`，只执行 Flyway `migrate()`，不启动 Spring/Quartz，正式补跑 `1.0.55` 和 `1.0.56`。
- 迁移结果：`Successfully applied 2 migrations`，`migrationsExecuted=2`。
- 迁移后历史表：`1.0.57`、`1.0.58`、`1.0.55`、`1.0.56` 均 `success=1`，其中 `1.0.55/1.0.56` 以 out-of-order 方式记录在后续 installed_rank。
- 默认 Flyway validate 结果：`Successfully validated 60 migrations`，`validationSuccessful=true`，`invalidMigrations=0`。

### 警告项

- `V1.0.55` 中 `CREATE TABLE IF NOT EXISTS sys_user_tenant` 执行时 MySQL 输出表已存在警告，未阻断迁移。
- Flyway 输出 out-of-order 模式不可复现性警告；本轮仅用于修复既有开发库历史缺口，后续新增迁移仍必须按最新版本顺延。

## 2026-06-06 18:32 +0800 流程设计弹窗全屏修复

### 变更范围

- 修复流程模型页打开设计器时弹窗高度过低、未真正全屏的问题。
- `flow/model.vue` 中设计弹窗根节点增加 `100vw/100vh` 内联尺寸。
- 覆盖 Naive Modal 根节点样式，内部 `flow-design-modal-shell` 使用完整视口尺寸并通过 flex 把高度传给 `design.vue` 的 `.model-design-page`。

### 执行命令与结果

1. 空白检查

```bash
git diff --check -- forge-admin-ui/src/views/flow/model.vue
```

结果：通过。命令无输出，退出码 0。

2. 前端构建

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过，输出 `✓ built in 59.62s`。

### 警告项

- 前端构建仍存在既有 UnoCSS 图标加载告警、既有 CSS `//` 注释压缩告警，以及 store 动静态混合导入 chunk 告警；未阻断构建。
- 沙箱阻止执行 `ps` 枚举本地 Vite 进程，未做浏览器截图验证。

## 2026-06-06 17:02 +0800 启动装配修复验证

### 变更范围

- 修复 `forge-flow-server` 引入 `forge-plugin-generator` 后的独立服务启动装配问题。
- 新增 flow server 专用 `FlowMenuRegisterAdapter`，用于满足 generator 菜单注册桥接依赖；菜单注册仍由 admin server 的真实实现负责。
- 新增 flow server 专用 `FlowAiClientAdapter`，用于满足 generator AI 生成桥接依赖；flow server 中 AI 生成调用返回 fallback/空流。
- `forge-flow-server` 显式引入 `forge-flow-client`，解决 generator 业务流程服务反射 `FlowClient` 时 optional 依赖不传递导致的 `NoClassDefFoundError`。

### 执行命令与结果

1. 后端打包

```bash
cd forge
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-flow/forge-flow-server -am package -DskipTests
```

结果：通过。Reactor 31 个模块全部 `SUCCESS`，输出 `BUILD SUCCESS`，`forge-flow-server` 编译 24 个源文件并重新打包。

2. 启动包内容检查

```bash
cd forge
jar tf forge-flow/forge-flow-server/target/forge-flow-server.jar | rg 'forge-flow-client|FlowClient.class|FlowAiClientAdapter|FlowMenuRegisterAdapter'
```

结果：通过。启动包包含 `FlowMenuRegisterAdapter.class`、`FlowAiClientAdapter.class` 和 `BOOT-INF/lib/forge-flow-client-1.0.0.jar`。

3. flow server 启动验证

```bash
cd forge
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
java -jar forge-flow/forge-flow-server/target/forge-flow-server.jar --server.port=0
```

结果：通过。非沙箱网络下连接 dev MySQL/Redis 成功，日志输出 `Started ForgeFlowApplication in 24.584 seconds`，未再出现 `MenuRegisterAdapter`、`AiClientAdapter` 或 `FlowClient` 装配错误。

### 失败链路与处理

- 首次启动复现原始错误：`AiCrudConfigService` 缺少 `MenuRegisterAdapter`。
- 移除 `FlowMenuRegisterAdapter` 上的 `@ConditionalOnMissingBean` 后，原始错误消失。
- 后续暴露 `AiCrudConfigGenerateService` 缺少 `AiClientAdapter`，已通过 `FlowAiClientAdapter` 补齐。
- 后续暴露 `BusinessFlowVariableResolver` 反射时缺少 `FlowClient`，已通过 flow server 直接依赖 `forge-flow-client` 补齐。

### 警告项

- 启动日志存在 macOS Netty DNS native library fallback 告警，不阻断启动。
- 启动时有若干“当前上下文中没有租户ID”告警，属于服务启动阶段无请求上下文触发的既有日志，不阻断启动。

### 跳过项

- 未执行完整业务接口联调和前端构建；本轮只针对用户反馈的 Spring 启动装配错误做后端最小闭环验证。

### 服务清理

- 本轮启动验证进程 PID `58819` 已停止。
- 复查 `ps -ef | rg 'forge-flow-server.jar|java -jar'`，未发现残留 flow server 进程。

## 2026-06-06 16:08 +0800 增量验证

### 变更范围

- 接入 `BUSINESS_OBJECT/HYBRID` 实际业务对象落表适配器，并在流程启动后写入业务流程实例关联。
- 补齐组织填报批次发布：按目标组织生成明细、默认解析 `sys_org` 负责人、发送站内信，并在 `batchItemId` 提交后回写明细。
- BPMN 节点属性面板接入流程表单字段目录，审批人、候选用户、候选组、流转条件可选择表单字段或系统变量。
- 模型列表设计入口改为全屏弹窗，设计页保留路由兼容并支持嵌入模式。
- 待办、已办、我发起、抄送详情从抽屉调整为弹窗，并补移动端全屏适配。

### 执行命令与结果

1. 前端构建

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过。第一次输出 `✓ built in 1m 17s`；补候选用户/候选组字段变量选择后复跑，输出 `✓ built in 1m 13s`。

2. 后端编译

```bash
cd forge
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-flow/forge-flow-server -am compile -DskipTests
```

结果：通过。Reactor 31 个模块全部 `SUCCESS`，输出 `BUILD SUCCESS`，总耗时约 16.963s。

3. 空白检查

```bash
git diff --check -- \
  forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue \
  forge-admin-ui/src/views/flow/design.vue \
  forge-admin-ui/src/views/flow/model.vue \
  forge-admin-ui/src/views/flow/todo.vue \
  forge-admin-ui/src/views/flow/done.vue \
  forge-admin-ui/src/views/flow/started.vue \
  forge-admin-ui/src/views/flow/cc.vue \
  forge-admin-ui/src/views/flow/entry.vue \
  forge-admin-ui/src/views/flow/fillBatch.vue \
  forge-admin-ui/src/api/flow.js \
  code-copilot/changes/flow-management-form-runtime-optimization/tasks.md \
  code-copilot/changes/flow-management-form-runtime-optimization/spec.md \
  code-copilot/changes/flow-management-form-runtime-optimization/test-spec.md \
  code-copilot/changes/flow-management-form-runtime-optimization/execution-log.md
```

结果：通过。命令无输出，退出码 0。

### 警告项

- 前端构建仍存在既有 UnoCSS 图标加载告警、既有 CSS `//` 注释压缩告警，以及 store 动静态混合导入 chunk 告警；未阻断构建。
- 后端编译存在既有 deprecated / unchecked 编译提示；未阻断编译。

### 跳过项

- 未执行 Flyway 实跑、登录后接口联调和 Playwright 截图：本地未启动 MySQL/Redis、后端服务和前端 dev server，本轮以编译、构建和静态检查作为阶段验证。
- 未执行 `mvn test`：本轮聚焦流程功能增量实现，按测试标准先执行目标模块编译。

### 服务清理

- 本轮未启动长期运行服务，无需停止进程。
