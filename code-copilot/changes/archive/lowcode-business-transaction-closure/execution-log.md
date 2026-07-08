# 执行日志 — 低代码业务交易闭环能力补齐
> status: apply_completed
> created: 2026-07-03

## 2026-07-03 SDD 初始化

- 用户确认按 SDD 开始推进。
- 创建变更目录：`code-copilot/changes/lowcode-business-transaction-closure/`。
- 范围收敛：平台级业务交易闭环能力，不硬编码采购仓储。
- P0 能力：`FOREACH` 动作步骤、选择器多字段搜索/动态过滤、数量台账只读查询。

## 验证记录

### 2026-07-03 P0 平台能力实现与验证

**变更范围**

- 后端动作引擎：新增 `FOREACH` 动作步骤、循环作用域变量、嵌套子步骤执行入口。
- 后端选择器：多字段关键词归一为私有 OR-like 条件，字段仍走动态 CRUD 白名单；过滤内部字段和空值。
- 前端选择器：运行态解析 `searchParams` 中的 `${formData.xxx}`、`${record.xxx}`、`${row.xxx}`、`${query.xxx}`，子表选择器可读取父表单数据。
- 数量台账：新增余额/流水/锁定只读查询 DTO/VO/Mapper XML/Service/Controller/API 和 `ai:businessQuantity:view` 权限脚本。

**执行命令与结果**

1. 后端插件编译：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

结果：首次失败于 `ForeachActionStepExecutor` lambda 捕获循环变量 `i`；修复为 `itemIndex` 后复跑通过，最终 `BUILD SUCCESS`。

2. `FOREACH` 定向单测：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessActionForeachStepExecutorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：首次因 Mockito inline MockMaker 无法 attach 当前 Homebrew JDK 失败；测试改为无 Mockito 本地 fake 后复跑通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。

3. 选择器归一逻辑单测：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessRecordSelectorSearchParamsTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。

4. 数量台账查询服务单测：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessQuantityQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。

5. 本轮组合单测：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessActionForeachStepExecutorTest,BusinessRecordSelectorSearchParamsTest,BusinessQuantityQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。

6. 前端构建：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过，`✓ built in 2m 34s`。

7. 静态检查：

```bash
git diff --check
rg -n '\$\{[^}]+\}' forge-server/db/migration
```

结果：`git diff --check` 无输出；Flyway `${...}` 扫描无输出。

**警告项**

- 后端编译存在既有 `BusinessFlowService` deprecated API 和 `BusinessObjectDesignerService` unchecked 警告，不阻断。
- 前端构建存在既有 `UserSelectModal` 命名冲突提示、CSS `//` 注释 minify 警告、动态/静态导入同模块 chunk 提示，不阻断。
- 单测输出 `commons-logging.jar` 与 `spring-jcl` 发现提示，不阻断。

**跳过项**

- 未启动本地 MySQL/Redis/后端服务做真实接口联调；本轮先完成平台 P0 编译、单测和前端构建闭环。
- 未做采购仓储端到端配置验收文档和详情数量区块 UI，保留为 Task 10-13 的 P1 后续项。

**服务清理**

- 本轮未启动长期运行的后端、前端开发服务或数据库服务，无需清理 PID。

### 2026-07-04 表单校验、关系选择器配置和运行态选择器兜底验证

**变更范围**

- 表单设计：修复常用校验 `preset/pattern/message` 在 `formDesignerSchema` 归一化时被丢弃的问题，并在属性面板选择常用校验时同步写回字段资产。
- 关系与级联：将子表选择器从手写文本配置改成候选对象、展示字段、搜索字段、字段映射和筛选条件的结构化选择式配置；旧 `displayFields/keywordFields/fieldMappings/searchParams` 协议兼容读取和保存。
- 运行配置：`LowcodeRuntimeConfigBuilder` 在字段或页面配置带有 `recordSelector/selector` 元数据时，优先发布为 `recordSelector` 组件，并把选择器配置下发到运行时 props，避免仓库/供应商等字段退化成 ID 输入框。
- 单测：扩展 `LowcodeRuntimeConfigBuilderTest` 覆盖 bigint 字段携带 recordSelector 元数据时发布为选择器组件。

**执行命令与结果**

1. 静态检查：

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessRelationDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilderTest.java
```

结果：通过，无输出。

2. 后端定向单测：

```bash
env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin /usr/local/apache-maven-3.9.3/bin/mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=LowcodeRuntimeConfigBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

3. 前端构建：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过，`✓ built in 2m 35s`。

**警告项**

- 后端编译仍有既有 deprecated API 和 unchecked 提示，不阻断。
- 前端构建仍有既有 `UserSelectModal` 命名冲突提示、CSS `//` 注释 minify 警告、动态/静态导入同模块 chunk 提示，不阻断。

**跳过项**

- 未启动本地后端、数据库和浏览器做真实采购单新增联调；本轮覆盖了运行配置生成单测、前端生产构建和静态检查。当前数据库如仍保留旧发布配置，需要重启服务执行 Flyway 后重新发布/刷新运行配置再复测。

**服务清理**

- 本轮未启动长期运行的后端、前端开发服务或数据库服务，无需清理 PID。

### 2026-07-04 发布入口位置修复验证

**变更范围**

- 恢复对象设计器右上角“发布”按钮在发布检查面板中的显示。
- 移除发布检查卡片里的重复“发布对象”按钮，只保留“同步数据表结构”确认和“打开应用”。
- 右上角发布会读取发布检查面板当前 `syncTable` 勾选值。

**执行命令与结果**

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过，`✓ built in 2m 26s`。

**警告项**

- 前端构建仍存在既有 `UserSelectModal` 命名冲突提示、CSS `//` 注释 minify 警告、动态/静态导入同模块 chunk 提示，不阻断。

**服务清理**

- 本轮未启动长期运行的前端开发服务，无需清理 PID。

### 2026-07-03 设计器配置保真与菜单停用回归修复验证

**变更范围**

- 后端设计器保存：重建字段前后保留已有 `dictType`、`basicProps.generation`、`basicProps.recordSelector`、引用字段、公式配置和业务组件类型；同时把旧 `edit_schema/search_schema/columns_schema` 桥接回 `page_schema.fieldSettings`，避免保存设计器后状态下拉、仓库/供应商选择器、自动编号消失。
- 存量配置修复：新增 `V1.0.98__preserve_procurement_warehouse_designer_field_runtime_metadata.sql`，修复采购仓储样例的仓库/供应商选择器、状态/仓库类型字典、列表渲染和编号字段生成配置。
- 前端设计器：合并对象设计器顶部发布和发布面板内发布入口，修复 Delete/Backspace 在字段绑定配置输入区误触设计器删除，字段绑定选择器禁止清空。
- 菜单状态：业务域/入口停用时同步 `sys_resource.visible/menu_status`，启用时恢复目录和启用应用菜单。
- 表格对齐：列表设计器新增表格块全局对齐配置，运行态列配置和预览渲染均使用全局默认值。

**执行命令与结果**

1. 前端构建：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过，`✓ built in 1m 46s`。

2. 定向组合单测：

```bash
env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin /usr/local/apache-maven-3.9.3/bin/mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessObjectDesignerPageSchemaTest,BusinessActionExecutionServiceTest,LowcodeRuntimeConfigBuilderTest,BusinessRecordSelectorSearchParamsTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：通过，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。

3. admin 聚合编译：

```bash
env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin /usr/local/apache-maven-3.9.3/bin/mvn -pl forge-admin-server -am compile -DskipTests
```

结果：通过，`BUILD SUCCESS`，总耗时 `13.779 s`。

4. 静态检查：

```bash
git diff --check
rg -n '\$\{[^}]+\}' forge-server/db/migration
```

结果：`git diff --check` 无输出；Flyway `${...}` 扫描无输出。

**警告项**

- 前端构建仍存在既有 `UserSelectModal` 命名冲突提示、CSS `//` 注释 minify 警告、动态/静态导入同模块 chunk 提示，不阻断。
- 后端编译仍存在既有 deprecated API 和 unchecked 警告，不阻断。
- 单测输出 `commons-logging.jar` 与 `spring-jcl` 发现提示，不阻断。

**跳过项**

- 未直接连接本地/远程业务库执行 `V1.0.98`；脚本会在 `forge-admin-server` 下次启动时由 Flyway 执行。当前服务如已启动，需要重启 admin 服务后再复测采购仓储新增/发布页面。
- 未做真实浏览器端采购仓储增删改流程联调；本轮用前端生产构建、admin 聚合编译、定向单测和 Flyway 占位符静态扫描覆盖。

**服务清理**

- 本轮未启动长期运行的后端、前端开发服务或数据库服务，无需清理 PID。

### 2026-07-03 采购仓储发布协议二次收口验证

**变更范围**

- 设计器内部组件白名单和字段默认值映射补齐 `input-number/inputnumber`，避免保存/迁移旧表单组件时没有命中数字字段默认值。
- `BusinessObjectDesignerPageSchemaTest` 新增 `input-number` 默认映射断言，和发布校验、动作执行对象编码、运行配置动作元数据用例一起复跑。

**执行命令与结果**

1. 静态检查：

```bash
git diff --check
rg -n '\$\{[^}]+\}' forge-server/db/migration
```

结果：`git diff --check` 无输出；Flyway `${...}` 扫描无输出。

2. 定向单测：

```bash
env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin /usr/local/apache-maven-3.9.3/bin/mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessObjectDesignerPageSchemaTest,BusinessActionExecutionServiceTest,LowcodeRuntimeConfigBuilderTest,BusinessRecordSelectorSearchParamsTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：通过，`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。

3. 后端插件编译：

```bash
env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin /usr/local/apache-maven-3.9.3/bin/mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

结果：通过，`BUILD SUCCESS`，总耗时 `13.846 s`。

**警告项**

- 后端编译仍有既有 deprecated API 和 unchecked 警告，不阻断。
- 单测仍输出 `commons-logging.jar` 与 `spring-jcl` 发现提示，不阻断。

**跳过项**

- 本轮没有改动前端源码，未重复执行前端构建。
- 未直接连接业务库执行 `V1.0.97`；需要重启 `forge-admin-server` 或手动执行迁移后，线上/本地数据库里的采购仓储样例配置才会被修复。

**服务清理**

- 本轮未启动长期运行的后端、前端开发服务或数据库服务，无需清理 PID。

### 2026-07-03 采购仓储发布和动作协议补修验证

**变更范围**

- 页面协议校验：`toolbar.fieldRefs` 作为工具栏动作配置不再按数据字段校验，避免供应商/采购单发布被子表字段脏引用阻断。
- 控件兼容：后端低代码校验器接受 `input-number/inputNumber/inputnumber`，运行态继续归一为 `number`。
- 页面旧协议：`LowcodePageZone` 支持历史 `key/type` JSON 别名，避免旧 seed 的 zones 反序列化成空区域。
- 动作协议：`BusinessActionExecuteDTO` 兼容 `businessObjectCode/targetObjectCode/targetEntityCode/refObjectCode/targetCode` 等别名，并从 `context.row._runtimeObjectCode` 兜底；发布出的 COMMAND 动作保留 `actionCode/suiteCode/objectCode/businessObjectCode`。
- 迁移脚本：新增 `V1.0.97__harden_procurement_warehouse_page_action_selector_protocol.sql`，清理采购仓储样例已持久化的工具栏脏字段引用，并补齐运行态按钮和关键选择器对象编码别名。

**执行命令与结果**

1. 定向单测：

```bash
env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin /usr/local/apache-maven-3.9.3/bin/mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessObjectDesignerPageSchemaTest,BusinessActionExecutionServiceTest,LowcodeRuntimeConfigBuilderTest,BusinessRecordSelectorSearchParamsTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：通过，`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`。

2. 后端插件编译：

```bash
env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin /usr/local/apache-maven-3.9.3/bin/mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

结果：通过，`BUILD SUCCESS`，总耗时 `11.503 s`。

3. 静态检查：

```bash
git diff --check
rg -n '\$\{[^}]+\}' forge-server/db/migration
```

结果：`git diff --check` 无输出；Flyway `${...}` 扫描无输出。

**警告项**

- 后端编译仍有既有 deprecated API 和 unchecked 警告，不阻断。
- 单测仍输出 `commons-logging.jar` 与 `spring-jcl` 发现提示，不阻断。

**跳过项**

- 本轮未重复执行前端构建；没有改动前端源码，风险面集中在后端发布/动作协议和 Flyway JSON 修复。
- 未连接本地/远程业务库执行 `V1.0.97`；脚本会在 `forge-admin-server` 下次启动时由 Flyway 执行。已完成占位符静态扫描。
- 未启动后端服务做采购仓储真实页面联调；本轮覆盖发布校验器、动作执行解析和运行配置构建的定向单测。

**服务清理**

- 本轮未启动长期运行的后端、前端开发服务或数据库服务，无需清理 PID。

### 2026-07-03 详情数量区块运行配置单测补强

**变更范围**

- 新增 `LowcodeRuntimeConfigBuilderTest`，使用真实 `LowcodeRuntimeConfigBuilder`、`LowcodeSchemaValidator`、`LowcodePolicyService` 构造最小低代码模型。
- 覆盖 `detail` zone props 中的 `quantityPanels` 发布为运行态 `options.detailPanels`。

**执行命令与结果**

1. 新增单测：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=LowcodeRuntimeConfigBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

2. 扩展组合单测：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessActionForeachStepExecutorTest,BusinessRecordSelectorSearchParamsTest,BusinessQuantityQueryServiceTest,LowcodeRuntimeConfigBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：通过，`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`。

**警告项**

- 单测仍输出 `commons-logging.jar` 与 `spring-jcl` 发现提示，不阻断。
- 新增单测首次编译测试源码时出现既有 deprecated/unchecked 测试源码提示，不阻断。

**跳过项**

- 本轮为单测补强，未重复执行前端构建和后端插件编译；上一轮同代码面已通过，且本轮 Maven test 已完成 testCompile。

**服务清理**

- 本轮未启动长期运行的后端、前端开发服务或数据库服务，无需清理 PID。

### 2026-07-03 详情数量区块发布运行态贯通补齐

**变更范围**

- 后端运行配置：`LowcodeRuntimeConfigBuilder` 从详情 zone props 读取 `quantityPanels`，发布为通用 `options.detailPanels`。
- 前端真实运行页：`views/ai/crud-page.vue` 将 `options.detailPanels` 传入 `AiCrudPage`。
- 前端预览/页面块：`LowcodePreviewPane.vue` 和 `GridBlockRenderer.vue` 将运行态 `detailPanels` 透传给 `AiCrudPage`。
- 前端详情渲染：`AiCrudPage` 详情模式在主表单和子表之后渲染 `AiCrudRowExpand`，复用通用数量面板 renderer。

**执行命令与结果**

1. 静态检查：

```bash
git diff --check
rg -n '\$\{[^}]+\}' forge-server/db/migration
```

结果：`git diff --check` 无输出；Flyway `${...}` 扫描无输出。

2. 后端插件编译：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

结果：通过，`BUILD SUCCESS`，总耗时 `38.350 s`。

3. 前端构建：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过，`✓ built in 2m 54s`。

4. 定向组合单测：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessActionForeachStepExecutorTest,BusinessRecordSelectorSearchParamsTest,BusinessQuantityQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。

**警告项**

- 前端构建仍存在既有 `UserSelectModal` 命名冲突提示、CSS `//` 注释 minify 警告、动态/静态导入同模块 chunk 提示，不阻断。
- 后端编译仍存在既有 deprecated API 和 unchecked 警告，不阻断。
- 单测输出 `commons-logging.jar` 与 `spring-jcl` 发现提示，不阻断。

**跳过项**

- 未启动本地 MySQL/Redis/后端服务做真实采购仓储端到端联调；本轮补齐的是设计器配置到发布运行态的协议贯通，已通过前端构建、后端编译和定向单测覆盖。
- 未做 Playwright 截图验证；本轮 UI 复用既有 `AiCrudRowExpand`/数量面板组件，主要风险在 prop 贯通和模板语法，已由生产构建覆盖。

**服务清理**

- 本轮未启动长期运行的后端、前端开发服务或数据库服务，无需清理 PID。

### 2026-07-03 P1 前端配置入口与详情区块验证

**变更范围**

- 前端动作设计器：新增“循环明细步骤”模板，生成 `FOREACH + DOMAIN_ACTION/QUANTITY` 可编辑步骤 JSON。
- 前端选择器配置：字段属性面板新增 `searchParams` 过滤参数入口；关系设计器新增子表选择器按钮、候选对象、显示字段、搜索字段、字段映射和过滤参数配置。
- 前端详情/展开：`expandConfig.panels[]` 支持 `quantity-balance`、`quantity-ledger`、`quantity-lock`；详情设计器新增数量区块配置；行展开堆叠模式补错误重试。
- 后端运行配置：关系配置中的 `recordSelector` 透传到模型引用 props 和运行态 `childrenConfig`。
- 文档：新增 `procurement-warehouse-acceptance.md`，说明采购仓储通过平台配置验收。

**执行命令与结果**

1. 前端构建：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过，`✓ built in 2m 17s`。

2. 后端插件编译：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

结果：通过，`BUILD SUCCESS`。

3. 定向组合单测：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessActionForeachStepExecutorTest,BusinessRecordSelectorSearchParamsTest,BusinessQuantityQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。

4. 静态检查：

```bash
git diff --check
rg -n '\$\{[^}]+\}' forge-server/db/migration
```

结果：`git diff --check` 无输出；Flyway `${...}` 扫描无输出。

**警告项**

- 前端构建仍存在既有 `UserSelectModal` 命名冲突提示、CSS `//` 注释 minify 警告、动态/静态导入同模块 chunk 提示，不阻断。
- 后端编译仍存在既有 deprecated API 和 unchecked 警告，不阻断。
- 单测输出 `commons-logging.jar` 与 `spring-jcl` 发现提示，不阻断。

**跳过项**

- 未启动本地 MySQL/Redis/后端服务做真实采购仓储端到端联调；本轮以平台通用能力编译、单测、前端构建和配置验收文档为准。
- 未做 Playwright 截图验证；本轮 UI 是既有后台设计器配置项扩展，已通过生产构建覆盖模板和脚本语法。

**服务清理**

- 本轮未启动长期运行的后端、前端开发服务或数据库服务，无需清理 PID。

### 2026-07-03 采购仓储运行闭环问题修复验证

**变更范围**

- 后端设计器保存：保存前规范化 `pageSchema.zones`，丢弃空区域，兼容 `list/data-table/form/edit-form/detail-view/table-toolbar` 等历史别名，避免 `不支持的页面区域: null`。
- 后端字段协议：`BusinessFieldSchemaService` 从 `basicProps/advancedProps` 读取 `dictType`，并兼容选择器对象编码别名，避免已有字典字段保存时报“字典字段必须配置字典类型”。
- 选择器协议：前端选择器配置和弹窗提交 `objectCode/businessObjectCode/targetObjectCode/targetEntityCode/refObjectCode/targetCode` 等别名，后端 DTO 和解析逻辑同步兜底，避免“选择器缺少业务对象编码”。
- 迁移脚本：新增 `V1.0.96__harden_procurement_warehouse_designer_runtime_config.sql`，按字段名修复采购仓储样例的状态字典、供应商报价子表状态下拉、选择器编码别名和 `pw_supplier_material.material_id` 可空。

**执行命令与结果**

1. 静态检查：

```bash
git diff --check
rg -n '\$\{[^}]+\}' forge-server/db/migration
```

结果：`git diff --check` 无输出；Flyway `${...}` 扫描无输出。

2. 后端插件编译：

```bash
env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin /usr/local/apache-maven-3.9.3/bin/mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

结果：通过，`BUILD SUCCESS`，总耗时 `12.325 s`。

3. 定向单测：

```bash
env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:/usr/local/apache-maven-3.9.3/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin /usr/local/apache-maven-3.9.3/bin/mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessFieldSchemaServiceTest,BusinessRecordSelectorSearchParamsTest,BusinessObjectDesignerPageSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：通过，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。

清理新增单测未使用 helper 后，补充复跑 `BusinessObjectDesignerPageSchemaTest`，通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

4. 前端构建：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过，`✓ built in 1m 28s`。

**警告项**

- 后端编译仍存在既有 deprecated API 和 unchecked 警告，不阻断。
- 单测仍存在既有 deprecated/unchecked 测试源码提示，不阻断。
- 前端构建仍存在既有 `UserSelectModal` 命名冲突提示、CSS `//` 注释 minify 警告、动态/静态导入同模块 chunk 提示，不阻断。

**跳过项**

- 未直接连接本地/远程业务库执行 `V1.0.96`；脚本会在 `forge-admin-server` 下次启动时由 Flyway 执行。如当前服务已经启动，需要重启后再复测采购仓储页面。
- 未做真实浏览器端采购仓储增删改流程联调；本轮覆盖后端编译、定向单测、前端生产构建和 Flyway 占位符静态扫描。

**服务清理**

- 本轮未启动长期运行的后端、前端开发服务或数据库服务，无需清理 PID。
