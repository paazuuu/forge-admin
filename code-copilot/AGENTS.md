# code-copilot/AGENTS.md
> code-copilot 目录工作指引。根目录 `AGENTS.md` 仍是最高优先级规则。

## 1. 适用范围

本文件适用于 `code-copilot/` 下的规则、变更、Agent prompt 和知识文档维护。

优先级从高到低：
1. 根目录 `AGENTS.md`
2. 当前变更目录 `code-copilot/changes/[变更名]/spec.md`
3. 当前变更目录 `tasks.md`、`execution-log.md`、`test-spec.md`
4. `code-copilot/rules/` 下的专项规则
5. 本文件

## 2. 记忆文件迁移位置

以下三类长期记忆统一迁移并维护在 `code-copilot/memory/`，不要再在 `.opencode/memory/` 下维护同名副本：

| 类型 | 权威文件 |
|------|----------|
| 项目决策 | `code-copilot/memory/decisions.md` |
| 踩坑记录 | `code-copilot/memory/pitfalls.md` |
| 用户偏好 | `code-copilot/memory/preferences.md` |

每次新会话、执行 `/test`、阶段验证、Review 修复或归档前验收时，必须先读取这三个文件，再读取当前变更的 spec/tasks/log。

## 3. 写入规则

- 架构、产品、技术路线等长期选择写入 `code-copilot/memory/decisions.md`。
- 可复用问题、根因、规避方式写入 `code-copilot/memory/pitfalls.md`。
- 用户明确偏好、环境偏好、验证偏好写入 `code-copilot/memory/preferences.md`。
- `code-copilot/knowledge/` 只保留专题知识材料，不再承载上述三类会话记忆。

## 4. Agent Prompt 同步

修改 `code-copilot/agents/*.md` 时，涉及启动上下文、知识沉淀、验证前置读取的描述，都必须指向 `code-copilot/memory/decisions.md`、`code-copilot/memory/pitfalls.md`、`code-copilot/memory/preferences.md`。
