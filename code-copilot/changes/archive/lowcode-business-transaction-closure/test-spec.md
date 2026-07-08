# 测试 Spec — 低代码业务交易闭环能力补齐
> status: apply_completed
> created: 2026-07-03

## 1. 测试原则

- 按 `code-copilot/rules/automated-testing-standard.md` 执行增量验证。
- 核心后端逻辑必须有定向单测，且 Maven 日志必须显示实际 `Tests run`。
- 前端本轮涉及运行态组件和设计器时执行 `pnpm build`。
- 不把采购仓储业务名写入平台单测类名，采购仓储只进入验收文档。

## 2. P0 单测范围

### `ForeachActionStepExecutor`

| 场景 | 输入 | 预期 |
|------|------|------|
| 集合为空 | `collectionPath=formData.items`，items 为空 | 返回 SUCCESS，执行 0 行 |
| 集合不存在 | `collectionPath=formData.items`，items 缺失 | 返回 SUCCESS，执行 0 行 |
| 两行成功 | items 两行，子步骤 Mock 成功 | 返回两组行结果，子步骤执行 2 次 |
| 子步骤失败 | 第二行子步骤抛异常 | 父步骤失败并触发事务回滚 |
| 循环变量 | 子步骤映射 `${item.quantity}`、`${index}` | 子步骤收到当前行数量和索引 |
| 深度限制 | `FOREACH` 中嵌套超过允许深度 | 抛业务异常 |

### `BusinessRecordSelectorService`

| 场景 | 输入 | 预期 |
|------|------|------|
| 单字段关键词 | `keywordFields=["name"]` | 兼容旧查询 |
| 多字段关键词 | `keywordFields=["code","name"]` | 按 OR 查询 |
| 非法字段 | `keywordFields=["tenant_id"]` | 字段被过滤或拒绝 |
| 动态过滤 | `searchParams={"warehouseId":"${formData.warehouseId}"}` | 解析为实际仓库 ID |
| 无权限 | 目标对象无 VIEW 权限 | 抛业务异常 |

> 本轮后端关键词归一逻辑使用无 Mockito 的 `BusinessRecordSelectorSearchParamsTest` 覆盖；运行态动态过滤由 `record-selector-utils.js` 与前端构建覆盖。既有 `BusinessRecordSelectorServiceTest` 依赖 Mockito inline，本地 JDK agent attach 受限，未纳入本轮组合命令。

### `BusinessQuantityQueryService`

| 场景 | 输入 | 预期 |
|------|------|------|
| 余额查询 | accountCode + itemCode | 返回余额分页 |
| 流水查询 | sourceObjectCode + sourceRecordId | 返回来源流水 |
| 锁定查询 | lockStatus=LOCKED | 返回锁定记录 |
| 空条件 | 无查询条件 | 仍按租户分页，不跨租户 |

## 3. 前端验证范围

- `BusinessActionDesigner.vue`：循环明细步骤模板可插入并格式化。
- `AiRecordSelectorModal.vue`：多字段搜索仍能显示列、分页、选择记录。
- `AiFormItem.vue`：动态过滤参数从表单数据解析后提交。
- `ChildTableEditor.vue`：子表选择器能读取父表单数据，批量追加不破坏 merge。
- 详情数量区块：加载中、错误、空数据、正常数据四种状态可渲染。

## 4. 执行命令

后端编译：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

后端定向单测：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessActionForeachStepExecutorTest,BusinessRecordSelectorServiceTest,BusinessQuantityQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

本轮实际执行的无 Mockito 组合单测：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessActionForeachStepExecutorTest,BusinessRecordSelectorSearchParamsTest,BusinessQuantityQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

详情数量区块发布运行态补充单测：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessActionForeachStepExecutorTest,BusinessRecordSelectorSearchParamsTest,BusinessQuantityQueryServiceTest,LowcodeRuntimeConfigBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test
```

前端构建：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

静态检查：

```bash
git diff --check
rg -n '\$\{[^}]+\}' forge-server/db/migration
```

## 5. 不测试项

- 不启动真实采购仓储业务应用联调，除非本地 MySQL、Redis 和服务状态可用。
- 不测试第三方 Webhook。
- 不测试完整 Flowable 引擎内部行为，本轮只验证动作回调可按配置执行循环步骤。

## 6. 本轮验证记录

- 2026-07-03：后端插件编译通过。
- 2026-07-03：`BusinessActionForeachStepExecutorTest`、`BusinessRecordSelectorSearchParamsTest`、`BusinessQuantityQueryServiceTest` 组合执行通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- 2026-07-03：前端 `pnpm --dir forge-admin-ui build` 通过，存在既有 chunk/CSS 注释告警。
- 2026-07-03：`git diff --check` 通过；`rg -n '\$\{[^}]+\}' forge-server/db/migration` 无输出。
- 2026-07-03：P1 前端配置入口和详情数量区块完成后复跑前端构建，通过，`✓ built in 2m 17s`。
- 2026-07-03：P1 后端关系选择器透传完成后复跑插件编译，通过，`BUILD SUCCESS`。
- 2026-07-03：P1 复跑三组组合单测，通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- 2026-07-03：P1 复跑 `git diff --check` 通过；`rg -n '\$\{[^}]+\}' forge-server/db/migration` 无输出。
- 2026-07-03：补齐详情数量区块发布运行态贯通后复跑前端构建，通过，`✓ built in 2m 54s`。
- 2026-07-03：补齐详情数量区块发布运行态贯通后复跑后端插件编译，通过，`BUILD SUCCESS`。
- 2026-07-03：补齐详情数量区块发布运行态贯通后复跑三组组合单测，通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- 2026-07-03：补齐详情数量区块发布运行态贯通后复跑 `git diff --check` 通过；`rg -n '\$\{[^}]+\}' forge-server/db/migration` 无输出。
- 2026-07-03：新增 `LowcodeRuntimeConfigBuilderTest` 覆盖 `detail.quantityPanels -> options.detailPanels`，单测通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- 2026-07-03：扩展组合单测加入 `LowcodeRuntimeConfigBuilderTest` 后通过，`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`。
- 2026-07-03：修复采购仓储设计器保存、字典字段和选择器编码问题后，后端插件编译通过，`BUILD SUCCESS`。
- 2026-07-03：新增并复跑 `BusinessFieldSchemaServiceTest`、`BusinessRecordSelectorSearchParamsTest`、`BusinessObjectDesignerPageSchemaTest`，通过，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。
- 2026-07-03：前端 `pnpm --dir forge-admin-ui build` 通过，`✓ built in 1m 28s`。
- 2026-07-03：`git diff --check` 通过；`rg -n '\$\{[^}]+\}' forge-server/db/migration` 无输出。
- 2026-07-03：修复页面工具栏脏字段引用、`input-number` 控件校验、动作执行对象编码别名和 COMMAND 动作运行配置后，复跑 `BusinessObjectDesignerPageSchemaTest`、`BusinessActionExecutionServiceTest`、`LowcodeRuntimeConfigBuilderTest`、`BusinessRecordSelectorSearchParamsTest`，通过，`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`。
- 2026-07-03：复跑后端插件级编译 `forge-plugin-generator -am compile -DskipTests`，通过，`BUILD SUCCESS`。
- 2026-07-03：新增 `V1.0.97__harden_procurement_warehouse_page_action_selector_protocol.sql` 后复跑 `git diff --check` 通过；`rg -n '\$\{[^}]+\}' forge-server/db/migration` 无输出。
- 2026-07-03：补齐设计器内部 `input-number/inputnumber` 组件默认映射后，复跑同组定向单测，通过，`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`；复跑后端插件级 `-am compile` 通过，`BUILD SUCCESS`。
- 2026-07-03：修复设计器重建字段时丢失字典、选择器、自动编号元数据，以及发布按钮重复、菜单停用同步、Delete 误清字段绑定和列表全局对齐后，前端构建通过，`✓ built in 1m 46s`。
- 2026-07-03：复跑 `BusinessObjectDesignerPageSchemaTest`、`BusinessActionExecutionServiceTest`、`LowcodeRuntimeConfigBuilderTest`、`BusinessRecordSelectorSearchParamsTest`，通过，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。
- 2026-07-03：复跑 admin 聚合编译 `forge-admin-server -am compile -DskipTests`，通过，`BUILD SUCCESS`，总耗时 `13.779 s`。
- 2026-07-03：新增 `V1.0.98__preserve_procurement_warehouse_designer_field_runtime_metadata.sql` 后复跑 `git diff --check` 通过；`rg -n '\$\{[^}]+\}' forge-server/db/migration` 无输出。
- 2026-07-04：恢复对象设计器右上角发布入口，移除发布检查卡片内重复发布按钮，前端构建通过，`✓ built in 2m 26s`。
- 2026-07-04：修复表单设计常用校验归一化丢失、关系选择器结构化配置和运行态 recordSelector 兜底后，`LowcodeRuntimeConfigBuilderTest` 通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`；前端构建通过，`✓ built in 2m 35s`；本轮相关文件 `git diff --check` 无输出。
