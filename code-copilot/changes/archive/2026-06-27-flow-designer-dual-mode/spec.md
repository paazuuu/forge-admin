# 流程模型双设计器模式改造
> status: done
> created: 2026-06-21
> complexity: 🟡中等

## 1. 背景与目标
当前流程模型设计页已替换为钉钉式审批流程设计器，适合人员审批、条件分支、抄送和表单权限等低门槛审批场景。但用户反馈该模式会限制通用业务工作流，旧 BPMN.js 专业设计器能力不应被移除。

本次改造目标：
- 保留现有钉钉式审批流程设计器。
- 参考提交 `9a2049b5^` 恢复原 `components/bpmn` BPMN.js 专业设计器。
- 新增流程模型设计器类型，让用户创建模型时选择“审批流程”或“业务流程”。
- 设计页按模型类型加载不同设计器，避免用审批流能力限制复杂 BPMN 工作流。

## 2. 命名决策
- **审批流程**：面向业务人员配置人员审批、条件分支、抄送、表单权限和审批策略。
- **业务流程**：面向实施/开发人员配置完整 BPMN 工作流，支持服务任务、事件、子流程、复杂网关等。

避免使用“专业 BPMN”作为主要入口文案，减少技术化；在说明文字中保留 BPMN 作为能力说明。

## 3. 功能点
- [x] `sys_flow_model` 增加 `designer_type` 字段，默认 `approval`，兼容历史模型。
- [x] `FlowModel` 实体增加 `designerType`。
- [x] 流程模型新建弹窗增加设计器类型选择。
- [x] 流程模型卡片展示设计器类型。
- [x] 流程设计页根据 `designerType` 渲染：
  - `approval`：现有 `DingFlowDesigner`。
  - `business`：恢复的 BPMN.js `FlowModeler` + `NodePropertiesPanel`。
- [x] 编辑已有模型时允许识别并加载对应设计器；已发布模型仍遵循现有只读/保存约束。

## 4. 业务规则
- 设计器类型是模型创建时的流程建模方式，不复用 `flow_type`，避免与请假、报销、采购等业务类型冲突。
- 历史数据无 `designer_type` 时默认按 `approval` 处理，保证已有审批流程不受影响。
- 本轮不做“审批流程 ↔ 业务流程”的无损互转。创建后可编辑元数据，但不提供随意切换，以免复杂 BPMN 无法还原成审批树。
- 两套设计器最终都保存 `bpmnXml`，部署链路继续复用现有 Flowable 部署逻辑。

## 5. 数据变更
| 操作 | 表名 | 字段 | 说明 |
|------|------|------|------|
| 新增列 | `sys_flow_model` | `designer_type varchar(32) NOT NULL DEFAULT 'approval'` | `approval` 审批流程，`business` 业务流程 |

## 6. 接口变更
| 接口 | 方法 | 变更内容 |
|------|------|----------|
| `/api/flow/model` | POST/PUT | 请求体和响应体透传 `designerType` |
| `/api/flow/model/page` | GET | 列表记录返回 `designerType` |
| `/api/flow/model/{id}` | GET | 详情返回 `designerType` |

## 7. 影响范围
- 流程模型列表与新建模型弹窗。
- 流程模型设计页。
- 前端依赖恢复 `bpmn-js` 相关包。
- Flow 插件模型实体和 Flyway 迁移。

## 8. 风险与关注点
- 旧 BPMN.js 组件来自历史提交，恢复后需通过前端构建验证其依赖和 API 兼容性。
- BPMN.js 属性面板与当前审批流程表单权限增强不是同一套交互，本轮只恢复专业模式，不强行合并审批流新功能。
- 若本地依赖缓存不完整，恢复 `bpmn-js` 依赖可能需要执行 pnpm 安装。

## 9. 测试策略
- 前端 ESLint 覆盖新增/恢复入口文件。
- 前端构建验证 BPMN.js 依赖可解析。
- 后端 flow 插件编译验证实体和迁移无编译影响。
- `git diff --check` 检查空白错误。

## 归档记录（HARD-GATE）
- **状态**：done
- **归档时间**：2026-06-27
- **归档人**：yaomd（批量归档）
- **归档路径**：code-copilot/changes/archive/2026-06-27-flow-designer-dual-mode/
- **判定依据**：任务清单全部完成，execution-log 验证通过（编译/构建/lint 闭环）。
