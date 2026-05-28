# 实现总结：visual-lowcode-crud-builder
> updated: 2026-05-19

## 已完成范围

- 第一版聚焦单表 CRUD，支持“创建新业务表”和“绑定已有表”；树形表按“树形单表”扩展实现。
- 前端采用 Naive UI 自研搭建器，不引入 `form-create-designer`、Element Plus 或 Ant Design Vue 运行时依赖。
- 完整链路为：可视化数据模型设计 → 拖拽式页面搭建 → 实时预览 → 一键发布。
- 发布菜单默认挂载到 `AI管理`，草稿不注册菜单。
- 在线 DDL 受控执行：仅允许 `CREATE TABLE IF NOT EXISTS` 和 `ALTER TABLE ADD COLUMN`，并要求 `ai:lowcode:deploy-ddl` 权限和二次确认。

## 后端改动

- 扩展 `ai_crud_config`，新增 `model_schema/page_schema/publish_status/draft_version/published_version` 等低代码字段。
- 新增 `ai_crud_config_version` 保存发布和回滚快照。
- 新增低代码协议 DTO、校验器和运行时转换器，将 `modelSchema/pageSchema` 转换为现有 `AiCrudPage` 配置。
- 新增 `appType/treeConfig` 协议和 `/ai/crud/{configKey}/tree` 运行时接口，支持左树右表模板；`MASTER_DETAIL` 协议预留但运行时阻断。
- 新增草稿、预览、发布、版本列表、回滚服务。
- 新增 DDL 预览与执行服务，DDL 后清理动态 CRUD 表结构缓存。
- 动态 CRUD 运行时阻止未发布低代码应用直接访问。
- 新增动态 CRUD Excel 模板、导入、导出服务；导入按 `editSchema` 白名单写入，字典字段支持标签反查字典值，导出按 `columnsSchema` 输出并复用脱敏展示链路。
- 动态导出兼容后台 Excel 列配置表：按 `configKey` 读取列配置并覆盖导出列顺序、表头和字典类型。
- AI 生成接口已兼容 `modelSchema/pageSchema/options/layoutType` 返回；AI 降级时规则引擎同步生成基础低代码协议。
- 修正 AI 配置生成提示和规则生成器中的 `:id` 占位符。

## 前端改动

- 新增 `/ai/lowcode-apps` 低代码应用列表。
- 新增 `/ai/lowcode-builder/:id?` 四步搭建器。
- 新增模型设计器：字段类型、长度、精度、必填、搜索、列表、表单、字典、敏感类型、加密策略；支持标准单表/树形单表选择，树形单表自动补齐 `parentId` 父级字段。
- 新增页面搭建器：查询区、数据表格、编辑表单、详情区，以及导入、导出、批量删除、自定义查询开关；页面模板支持标准单表和左树右表切换。
- 新增预览和发布面板：DDL 预览、在线建表确认、版本列表、回滚。
- 运行页 `crud-page.vue` 接入低代码发布态 `options`，映射导入导出、批量删除、自定义查询和弹窗布局配置。
- `AiCrudPage` 导入改为自定义上传请求，导出和模板下载按 blob 文件流处理，避免只提示成功但不下载文件。
- 旧 `crud-config.vue` 主入口收敛到低代码应用，高级 JSON 配置保留给技术人员。

## 保留兼容

- 旧 AI CRUD 配置、JSON 编辑、代码包下载仍保留。
- 旧动态运行页 `/ai/crud-page/:configKey` 不变。
- 旧配置驱动运行时继续读取 `searchSchema/columnsSchema/editSchema/apiConfig`。
- 主子表暂未开放运行时，但 `modelSchema.children` 已预留，后续可按同一协议扩展。

## 后续迭代

- 完整升级 AI SSE 前端交互：展示 `modelSchema/pageSchema` 阶段，并支持字段/组件局部优化后应用到搭建器。
- 主子表运行时：补充主表/子表 DDL、事务写入、子表列表编辑器和发布校验。
- 补充 Playwright 交互回归，覆盖模型编辑、拖拽搭建、发布和回滚。
