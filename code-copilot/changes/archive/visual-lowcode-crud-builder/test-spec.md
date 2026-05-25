# 测试规格：visual-lowcode-crud-builder
> created: 2026-05-19
> scope: 单表低代码 CRUD、受控在线建表、Naive UI 自研搭建器

## 1. 自动化验证

### 后端编译
```bash
mvn -pl forge-admin-server -am compile -DskipTests
```

验收：
- `forge-plugin-generator`、`forge-admin-server` 聚合编译通过。
- 新增 Controller、Service、Mapper XML、DTO/VO 无编译错误。

### 前端构建
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

验收：
- 低代码应用列表、四步搭建器、模型设计器、页面搭建器、预览发布面板可参与生产构建。
- `/ai/lowcode-apps`、`/ai/lowcode-builder/:id?`、`/ai/crud-page/:configKey` 路由不冲突。

## 2. 后端接口回归

### 草稿与预览
1. 调用 `POST /ai/lowcode/app/draft` 创建草稿，`modelSchema.tableMode=CREATE`。
2. 调用 `GET /ai/lowcode/app/{id}` 查询草稿详情。
3. 调用 `POST /ai/lowcode/app/{id}/preview` 预览草稿。

验收：
- 草稿保存不注册业务菜单。
- 预览返回 `searchSchema/columnsSchema/editSchema/apiConfig/options`。
- `apiConfig.detail/delete` 使用 `:id` 占位符。

### 在线 DDL
1. 调用 `POST /ai/lowcode/model/ddl/preview`。
2. 使用无 `ai:lowcode:deploy-ddl` 权限账号发布 `deployMode=ONLINE_CREATE_TABLE`。
3. 使用有权限账号发布，且 `confirmOnlineDdl=true`。

验收：
- DDL 预览只出现 `CREATE TABLE IF NOT EXISTS` 或 `ALTER TABLE ... ADD COLUMN`。
- 无权限或未确认时发布失败。
- 已有表只追加缺失字段，不删除、不重命名、不改类型。

### 发布与版本
1. 调用 `POST /ai/lowcode/app/{id}/publish`。
2. 调用 `GET /ai/lowcode/app/{id}/versions`。
3. 调用 `POST /ai/lowcode/app/{id}/rollback/{versionId}`。
4. 访问 `/ai/crud-page/{configKey}`。

验收：
- 首次发布注册菜单，默认挂在 `AI管理`。
- 每次发布和回滚写入 `ai_crud_config_version`。
- 正式运行页只读取发布态运行时配置，未发布草稿不可访问。

### 动态导入导出
1. 调用 `GET /ai/crud/{configKey}/import-template` 下载模板。
2. 按模板上传 `POST /ai/crud/{configKey}/import`，字典字段分别填写字典标签和值。
3. 调用 `POST /ai/crud/{configKey}/export` 导出当前搜索条件下的数据。

验收：
- 模板只包含 `editSchema` 字段，表头为业务中文名。
- 导入只写入表单白名单字段，必填和字典值校验失败时不落库。
- 导出只包含 `columnsSchema` 可见字段，字典字段导出展示值，敏感字段保持脱敏展示。
- 当 `sys_excel_column_config` 存在同 `configKey` 列配置时，导出列顺序、表头、字典类型按后台配置覆盖。

### 树形单表
1. 创建 `appType=TREE` 的低代码草稿，模型包含 `parentId` 和 `name` 字段。
2. 页面模板选择 `tree-crud`，配置 `treeConfig.parentField=parentId`、`labelField=name`。
3. 发布后调用 `GET /ai/crud/{configKey}/tree`。
4. 访问 `/ai/crud-page/{configKey}`，选择左侧节点后查看右侧列表请求参数。

验收：
- `/tree` 返回带 `children` 的树形数组，根节点识别 `parentId=null/0/空`。
- 右侧列表请求包含 `parentId=<选中节点id>`，且该字段不出现在搜索表单中。
- 在选中节点下新增数据时，表单隐藏写入 `parentId`。
- `appType=MASTER_DETAIL` 保存/预览/发布时返回“主子表低代码运行时尚未启用”的明确异常。

## 3. 前端手动回归

### 从零创建单表应用
1. 进入 `/ai/lowcode-apps`，点击“新建应用”。
2. 在“数据模型”配置应用名称、配置键、表名和字段。
3. 在“页面搭建”拖拽/启用查询区、列表区、表单区和详情区。
4. 在“实时预览”执行预览校验。
5. 在“发布上线”查看 DDL，勾选确认并发布。

验收：
- 全程不要求编辑 JSON。
- 字段新增、复制、删除、排序后页面区域字段引用同步清理。
- 发布后“打开页面”能进入动态 CRUD 页面。

### 绑定已有表
1. 数据模型选择“绑定已有表”。
2. 配置与已有业务表一致的字段。
3. 选择“仅发布页面”。

验收：
- 表不存在时发布失败并提示建表方式。
- 表存在时不执行 DDL。

### 旧入口收敛
1. 进入 `/ai/crud-config`。
2. 点击顶部“低代码应用”进入低代码应用列表。
3. 低代码配置卡片点击后进入可视化搭建器。
4. 点击“高级 JSON”仍可打开旧配置抽屉。

验收：
- 业务主路径默认指向低代码搭建器。
- 技术人员仍可进行 JSON 配置、AI 辅助生成和代码下载。

## 4. 暂缓项

以下能力后续单独补测：
- AI 生成直接输出 `modelSchema/pageSchema` 和局部优化协议。
- 主子表完整运行时。
