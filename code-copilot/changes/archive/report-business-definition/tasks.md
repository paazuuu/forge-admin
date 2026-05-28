# 任务清单：report-business-definition
> status: apply
> created: 2026-05-17

## 任务总览

| Task | 名称 | 状态 |
|------|------|------|
| Task 1 | 后端业务定义模型与 SQL | completed |
| Task 2 | 后端业务定义 CRUD 与 AI 上下文接口 | completed |
| Task 3 | admin-ui 业务定义管理页 | completed |
| Task 4 | report-ui AI 业务定义选择与请求模型 | completed |
| Task 5 | AI 提示词和组件数据集自动绑定 | completed |
| Task 6 | 编译与构建验证 | completed |

## 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 1 | completed | `forge-plugin-data` entity/mapper/xml/sql | 新增业务定义表与业务-数据集绑定表 |
| Task 2 | completed | `DataBusinessDefinitionController`, `DataBusinessDefinitionServiceImpl` | 新增 CRUD、详情、列表、AI 上下文接口 |
| Task 3 | completed | `forge-admin-ui/src/views/data/business.vue`, `src/api/data/business.ts` | 新增业务定义管理页与多数据集绑定表单 |
| Task 4 | completed | `forge-report-ui/src/components/FgAI/AIChatPanel.vue`, `src/api/data/business.ts`, AI 请求类型 | AI 面板新增业务定义选择并发送业务上下文 |
| Task 5 | completed | `aiEngine.ts`, `report-init.sql`, `AiChatService` | 支持组件 `request.datasetId` 自动绑定数据集；提示词加入数据驱动规则 |
| Task 6 | completed | - | 后端 Maven 编译、admin-ui 构建、report-ui 构建均通过 |
