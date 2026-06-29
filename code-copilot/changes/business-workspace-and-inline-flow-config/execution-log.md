# 变更日志 — 我的工作台 + 节点就地配置 + 渲染引擎收敛

> 记录决策、踩坑和知识发现。知识飞轮的输入。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
| --- | --- | --- | --- |
| 2026-06-29 | propose | 用户在第二轮 spec 后反馈"还有结构性问题没解" | 引出三大问题：我的工作台 / 应用中心节点就地配置 / 双渲染引擎收敛 |
| 2026-06-29 | propose | 创建变更目录 `code-copilot/changes/business-workspace-and-inline-flow-config/`，生成 `spec.md` / `tasks.md` / `test-spec.md` / `execution-log.md` | 与 `unified-app-flow-experience` 拆开，独立迭代 |
| 2026-06-29 | propose | HARD-GATE 六项确认完成（保持并列 / 支持网关 / 分批离线迁移 / 本轮清理 form-create / summary 默认 + 偏好 / 仅 4 项 + summary） | 调整：新增 B8 网关业务化就地配置 + C7 form-create 完全清理（含前置硬条件 + 回滚预案 + 7 天观察期） |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
| --- | --- | --- | --- |
| 三个子迭代独立交付 | A / B / C 任一可单独发布 | 一锤子全做完 | 用户可按业务节奏分批；任一阻塞不影响其他 |
| 统一渲染引擎 | AiForm | 引入第三方表单引擎 | 自研 AiForm 已能覆盖 90% 场景；不引入外部依赖 |
| schema 迁移落点 | 新列 `form_schema`，旧列 `form_json` 保留兜底 | 原地改写 | 灰度安全；可降级；可重入 |
| 节点配置数据所有权 | 应用中心就地保存共享 BPMN 节点属性 | 新建"应用中心节点配置表" | 避免双写，数据所有权清晰 |
| 并发编辑保护 | 乐观锁（updateTime） | 悲观锁 / 拒绝双端 | 用户体验更好；冲突很少；UI 兜底 |
| 顶部导航 | 应用中心 / 工作台 / 能力中心 三级，**保持并列**（用户 2026-06-29 确认） | 应用中心内嵌"工作台 Tab"；工作台收编应用中心 | IA 干净；应用中心 = 配置入口 / 工作台 = 日常事务 |
| 网关就地编辑 | 本轮支持 exclusiveGateway + inclusiveGateway 业务化就地编辑（用户确认） | 第一版不支持，所有网关只能进完整设计器 | 同意/驳回路由是业务高频场景，分离体验割裂；用 isInlineEditable 判定复杂度兜底 |
| form-create 下线节奏 | 本轮完成代码清理（用户确认）；保留 form_json 列一个大版本作兜底 | 仅 deprecate 不删；下一轮单独清理 | 用户明确要求一次性收口；前置硬条件 + 回滚预案 + 7 天观察期降低风险 |
| 工作台默认页 | summary，用户偏好可记忆 | 直接到 todo 列表 | 聚合首页提供决策入口，列表深处难发现；用 ai_user_setting 落地 |
| 路由兼容期 | 保留 2 个大版本 | 立刻 redirect | 老链接 / 通知 / 邮件需要迁移时间 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 沉淀？ |
| --- | --- | --- | --- |
| （propose 阶段无踩坑，apply 时补充） | — | — | — |

## 知识发现

> 每个 task 后实时记录，/archive 时逐条确认沉淀到 `code-copilot/memory/` 或 `knowledge/`

- [ ] **关键词**: `workspace top-nav IA` — 工作台作为顶级导航与应用中心并列，徽标驱动用户感知，避免被埋进二级菜单
- [ ] **关键词**: `workspace default page user preference` — `ai_user_setting.workspace.default_page` 记录用户偏好，默认 summary，接口异常回退 summary 不白屏
- [ ] **关键词**: `flow node inline config drawer` — 节点就地配置抽屉组件抽取，与 BPMN 画布解耦，可跨页面复用
- [ ] **关键词**: `BPMN node optimistic lock` — 节点配置乐观锁基于 updateTime，冲突时 409 + UI 确认覆盖
- [ ] **关键词**: `gateway inline edit` — exclusiveGateway / inclusiveGateway 业务化就地编辑；`isInlineEditable` 判定 = 字段+简单运算符+字面值 且 ≤5 分支；BPMN 表达式作为高级回显
- [ ] **关键词**: `gateway business-bpmn round-trip` — 网关业务化 ↔ BPMN 表达式双向转换；保存后立即回读对比，偏差立即报错
- [ ] **关键词**: `gateway preset` — 同意/驳回/退回/终止 4 个快捷预设；用户一键应用避免手写表达式
- [ ] **关键词**: `form-create -> AiForm adapter` — 适配层支持 10 种基础控件 1:1 映射，未知控件通过 unknownFields 兜底
- [ ] **关键词**: `schema migration log table` — `ai_flow_schema_migration_log` 表记录每个节点的迁移状态，支持重试 + dry-run
- [ ] **关键词**: `feature flag flow.form.engine` — 双引擎切换通过 feature flag，租户级灰度，C7 完成后移除
- [ ] **关键词**: `form-create purge precheck` — C7 前置硬条件自动化检查：迁移 100% + flag 24h 稳定 + 监控指标正常，三者全绿才能 purge
- [ ] **关键词**: `form-create rollback rebuild` — `FormSchemaRebuildFromFormJsonJob` 演练从 form_json 重建 form_schema，C7 上线前必跑一次
- [ ] **关键词**: `form_json column retention` — C7 清理代码后 form_json 列保留一个大版本作兜底，下下轮变更才 DROP COLUMN

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
| --- | --- | --- | --- |
| — | — | — | — |

> /apply 阶段如发现偏差，按此格式记录并同步更新 spec.md。

## 代码质量备忘

- `FlowNodeInlineConfig.vue` / `FlowGatewayInlineConfig.vue` 必须严格无 BPMN 画布依赖，否则不能在应用中心嵌入；评审时核查 import。
- `formCreateToAiSchema` 适配层建议放在 `components/ai-form/adapters/`，并独立单测；未知字段必须 raise 警告日志（一次性）便于排查。
- `WorkspaceService` 聚合接口注意数据库查询合并（避免 4 次独立 count），用单条 SQL 或缓存兜底。
- 顶部徽标轮询 30s 建议用 `useIntervalFn`（VueUse），页面隐藏时暂停。
- 迁移工具 `FormCreateSchemaMigrationJob` 必须支持 dry-run 模式（先打印迁移计划，不写库）+ batchSize 参数；运维使用安全。
- 步骤条对长流程（>20 节点）滚动 + 搜索 UX 要在 B2 完成时确认；可参考 GitLab CI 流水线视觉。
- 应用中心 + 流程设计器双入口编辑节点，文档里必须明确"以最后保存为准"，避免用户误解为"两个独立配置"。
- 网关 `convertToBpmnExpression` / `parseBpmnExpression` 必须互逆；建议加 round-trip 属性测试（property-based test）。
- C7 清理前的备份 tag (`pre-form-create-purge`) 必须打在 main 分支上，且包含 lockfile；回滚时可整体回退。
- C7 清理后建议运行依赖分析工具（如 `pnpm why @form-create/element-ui`）确认无残留间接引用。
- 工作台偏好接口异常时回退 summary 必须前端兜底，不可阻塞页面加载；后端接口可异步重试。
