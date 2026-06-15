# 公式能力扩展 — 执行日志

## 2026-06-12 22:24 增量验证

### 变更范围

- 迁移公式领域模型、公式执行/校验/发布校验、公式 REST API、字段设计器公式配置 UI。
- 补齐 `gen_table_column.formula_config` Flyway 迁移脚本，当前仓库版本使用 `V1.0.68` 避免与既有 `V1.0.67` 冲突。
- 本轮额外修复运行链路：
  - 更新时先合并数据库旧值再计算 STORED 公式，避免部分字段更新导致公式算空。
  - 读取统一执行 VIRTUAL 公式，覆盖列表、详情、导出、树形和自定义查询。
  - 从表新增/更新/删除后按对象关系刷新父表 STORED 聚合公式。
  - 聚合查询按低代码模型 schema 将字段编码解析为真实列名，并为明细行补充 camel/snake 别名。

### 命令与结果

- `git diff --check`
  - 结果：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 结果：BUILD SUCCESS。
  - 警告：`GenTableServiceImpl` 使用过时 API；`BusinessObjectDesignerService` 存在 unchecked/unsafe 操作。均为既有编译警告，未阻断。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dmaven.compiler.skip=false -Dsurefire.skipTests=false -DskipTests=false -Dmaven.test.skip=false -Dtest='*Formula*,*Aggregate*,AviatorAdapterTest,ExpressionExecutorTest,ExpressionParserTest,ExecutionResultTest' test`
  - 结果：BUILD SUCCESS，但未执行测试。
  - 原因：`forge-server/pom.xml` 中 `maven-compiler-plugin` 配置了 `<skip>true</skip>`，`maven-surefire-plugin` 配置了 `<skipTests>true</skipTests>`，命令行参数未能覆盖，输出显示 `Not compiling test sources` 与 `Tests are skipped`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：构建通过，`vite build` 完成。
  - 警告：既有 CSS `//` 注释 minify 警告；`src/store/index.js` 同时动态/静态导入导致 chunk 提示。均未阻断。

### 跳过项

- 未连接真实 MySQL 执行 Flyway 实跑和接口级主从聚合回写验证；本轮以迁移编译、前端构建、静态脚本防重复和代码链路补齐为验证范围。

### 服务清理

- 本轮未启动后端或前端 dev server，无需清理进程。

## 2026-06-12 22:31 契约修复后复验

### 变更范围

- 修复聚合公式 `relationCode` 前后端契约：
  - 前端字段属性面板从当前业务对象 `DETAIL/CHILD_LIST` 关系中选择关系，并保存关系 ID。
  - 后端聚合读取和父表刷新兼容旧配置中的目标对象编码/关系名。

### 命令与结果

- `git diff --check`
  - 结果：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 结果：BUILD SUCCESS。
  - 警告：同上一轮，均为既有编译警告。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：构建通过。
  - 警告：同上一轮，均未阻断。

## 2026-06-12 22:50 接手补丁后复验

### 变更范围

- 调整动态 CRUD 读取链路顺序为“解密 -> VIRTUAL 公式 -> 字典翻译 -> 脱敏”，避免虚拟公式使用脱敏值参与计算，也确保公式结果仍可进入翻译/脱敏链路。
- 清理 `FormulaPublishValidator` 合并痕迹中的重复条件块。
- 补强发布校验：无法解析的 `formulaConfig` 现在会作为 `CONFIG` 错误阻断发布，避免缺失聚合/条件配置时被静默跳过。
- 增加 `FormulaPublishValidatorTest.missingAggregateConfig` 用例，覆盖聚合公式缺少 `aggregate` 配置的发布阻断场景。

### 命令与结果

- `git diff --check`
  - 结果：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 结果：BUILD SUCCESS。
  - 警告：`GenTableServiceImpl` 使用过时 API；`BusinessObjectDesignerService` 存在 unchecked/unsafe 操作。均为既有编译警告，未阻断。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：构建通过，`vite build` 完成。
  - 警告：既有 CSS `//` 注释 minify 警告；`src/store/index.js` 同时动态/静态导入导致 chunk 提示。均未阻断。
- `find forge-server/db/migration -maxdepth 1 -name 'V*__*.sql' | sed 's#^.*/##' | sort -V | tail -n 8`
  - 结果：最新版本为 `V1.0.68__add_formula_config_to_gen_table_column.sql`，未与既有 `V1.0.67` 冲突。
- `rg -n "�" ...公式迁移相关文件...`
  - 结果：未命中乱码替换字符。
- `rg -n "@playwright/test|lodash-es|@vueuse/core" forge-admin-ui/package.json forge-admin-ui/vite.config.js`
  - 结果：当前项目已具备 `lodash-es`、`@vueuse/core` 依赖与 Vite 预构建配置；旧仓库提交中的 `@playwright/test` 和 `pnpm-workspace.yaml` 对当前生产构建非必需，本轮未迁入。

### 跳过项

- 未启动真实 MySQL 和后端服务执行接口级保存/回显、STORED/VIRTUAL 公式运行、主从聚合回写验证；本轮仍以代码迁移、编译、前端构建和静态链路审查为范围。
- 未复跑 Maven 单测。上一轮已确认当前 `forge-server/pom.xml` 的 compiler/surefire 配置会跳过测试源码编译与测试执行，命令行覆盖未生效。

### 服务清理

- 本轮未启动后端或前端 dev server，无需清理进程。

## 2026-06-13 09:45 公式面板样式对齐后复验

### 变更范围

- 将字段属性面板中的公式配置从普通折叠表单调整为独立“公式设置”卡片，视觉结构对齐旧工程截图：
  - 顶部开关、公式名称、公式类型、表达式编辑区。
  - 表达式底部增加“字段 / 函数 / 数字 / 字符串”快捷 token。
  - 右侧增加“字段变量 / fx 函数”插入按钮。
  - 增加“实时 / 保存时 / 手动”触发方式分段控件，其中手动仅保留视觉占位，当前后端协议仍只支持 VIRTUAL/STORED。
  - 增加“保存公式 / 校验语法 / 预览计算”操作区。
- 公式配置继续写入现有 `formulaConfig`，新增 `name` 和 `triggerMode` 展示配置，不改变后端公式执行协议。

### 命令与结果

- `git diff --check`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：构建通过，`vite build` 完成。
  - 警告：既有 CSS `//` 注释 minify 警告；`src/store/index.js` 同时动态/静态导入导致 chunk 提示。均未阻断。

### 服务清理

- 本轮未启动后端或前端 dev server，无需清理进程。

## 2026-06-13 10:49 公式面板视觉细节修复后复验

### 变更范围

- 继续按用户截图修正字段属性面板中的“公式设置”卡片：
  - 公式卡片改为更接近截图的灰底、8px 圆角、紧凑间距。
  - 表单标签、输入框高度、表达式编辑器高度和底部工具条密度重新调整。
  - “字段变量 / fx 函数”按钮改为截图中的灰色工具按钮质感。
  - “实时 / 保存时 / 手动”触发方式改为自定义分段按钮，避免 Naive primary 按钮变成大面积蓝底。
  - 主操作区保留“保存公式 / 校验语法”，将“预览计算”弱化为文本入口，避免破坏截图中的两按钮布局。
  - 语法错误反馈支持多行错误展示，重复错误不会产生 Vue key 冲突。

### 命令与结果

- `git diff --check`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：构建通过，`vite build` 完成。
  - 警告：既有 CSS `//` 注释 minify 警告；`src/store/index.js` 同时动态/静态导入导致 chunk 提示。均未阻断。
- 临时启动 `pnpm --dir forge-admin-ui exec vite --host 127.0.0.1 --port 3022 --strictPort true` 并确认前端可访问；后端 `8580` 已在运行。
  - 结果：前端 dev server 正常启动。
  - 说明：后续尝试用 headless Chrome + CDP 做页面级截图检查，但当前 Node REPL 工具无法访问本机 `9223` CDP 端口，未完成截图采集。

### 服务清理

- 已清理临时前端 Vite 进程和 headless Chrome 进程，复查无残留。

## 2026-06-13 14:27 编辑表单公式回显修复后复验

### 变更范围

- 字段属性面板中，非聚合公式的“依赖字段”改为按表达式变量自动同步；表达式清空时同步清空依赖，避免旧依赖残留。
- 动态 CRUD 编辑/新增表单中，所有非聚合公式字段均参与表单内计算并回填目标字段，不再只计算 `REALTIME/VIRTUAL` 公式。
- 运行态公式预览请求会按表单字段元数据将数字字段的字符串值转换为 Number，避免后端公式引擎把数字字符串当作普通字符串。
- 动态页和低代码真实运行态预览在旧发布配置缺少 `editSchema.formulaConfig` 时，从 `modelSchema.fields` 反补 `formulaConfig`、数据类型和只读/禁用态。

### 命令与结果

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiCrudPage.vue src/views/ai/crud-page.vue src/components/lowcode-builder/preview/LowcodePreviewPane.vue src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue`
  - 结果：通过，无输出。
- `git diff --check`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：构建通过，`vite build` 完成。
  - 警告：既有 CSS `//` 注释 minify 警告；`src/store/index.js` 同时动态/静态导入导致 chunk 提示。均未阻断。

### 跳过项

- 未启动真实后端和数据库做 `/ai/crud-config/render/{configKey}`、编辑弹窗和公式预览接口联动验证；本轮以用户反馈的前端配置同步、运行态计算触发和旧配置兼容为增量修复范围。
- 未复跑后端 Maven 编译；本轮未改 Java/SQL 文件。

### 服务清理

- 本轮未启动后端或前端 dev server，无需清理进程。

## 2026-06-13 14:41 运行态公式数字字符串修复后复验

### 变更范围

- 修复动态编辑表单公式预览入参中数字字符串未转换的问题。
- 运行态公式依赖值现在会结合字段元数据、字段名和表达式运算符判断是否应转为 Number；`quantity * price` 中 `"11"`、`"1212"` 会在请求 `/api/ai/business/formula/preview` 前转换为数字。

### 命令与结果

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiCrudPage.vue`
  - 结果：通过，无输出。
- `git diff --check`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 >/dev/null && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：构建通过，`vite build` 完成。
  - 警告：既有 CSS `//` 注释 minify 警告；`src/store/index.js` 同时动态/静态导入导致 chunk 提示。均未阻断。

### 跳过项

- 未启动后端做真实浏览器联调；本轮为前端入参类型修复，已通过 lint/build 验证。

### 服务清理

- 本轮未启动后端或前端 dev server，无需清理进程。
