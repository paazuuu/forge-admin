# AI 编程 Skill 使用教程

> 本教程介绍 Forge Admin 项目内置的 AI 编程 Skill 体系，帮助你利用 AI 编程助手（如 Claude Code、Cursor、OpenClaw 等）高效完成业务开发。涵盖 CRUD 代码生成和业务流程开发两大核心 Skill。

---

## 一、Skill 体系总览

Forge Admin 在 `.agents/skills/` 目录下内置了三个 AI 编程 Skill，为 AI 编程助手提供项目特定的开发规范和代码生成模板：

```
.agents/skills/
├── forge-codegen-crud/              ← 简单业务 CRUD 代码生成
│   ├── SKILL.md                     ← Skill 主文件（规范+流程）
│   └── references/                  ← 详细参考文档
│       ├── single-table-crud.md     ← 单表 CRUD 代码模板
│       ├── sql-seeds.md             ← SQL 种子数据模板
│       └── validation-checklist.md  ← 验证清单
├── forge-business-flow-development/ ← 业务流程开发
│   ├── SKILL.md                     ← Skill 主文件（规范+流程）
│   └── references/                  ← 详细参考文档
│       ├── code-first-workflow.md   ← 代码优先工作流
│       ├── lowcode-workflow.md      ← 低代码工作流
│       ├── purchase-order-reference.md ← 采购单示例
│       ├── status-and-callbacks.md  ← 状态机与回调
│       ├── bpmn-configuration.md    ← BPMN 节点配置
│       ├── sql-templates.md         ← SQL 模板
│       └── validation-checklist.md  ← 验证清单
└── ui-ux-pro-max/                   ← UI/UX 设计智能
    ├── SKILL.md                     ← Skill 主文件
    └── scripts/                     ← 设计系统搜索工具
```

### 什么是 Skill？

Skill 是一组结构化的开发规范文档，告诉 AI 编程助手：

1. **项目约定**：包名结构、命名规范、技术选型
2. **代码模板**：Entity、Controller、Service、Mapper、Vue 页面的标准写法
3. **SQL 规范**：Flyway 迁移脚本、字典种子、菜单权限种子
4. **验证清单**：生成代码后的检查项
5. **禁止事项**：不可违反的红线规则

### Skill vs 普通文档

| 对比项 | 普通文档 | Skill |
|--------|---------|-------|
| 阅读对象 | 人类开发者 | AI 编程助手 |
| 结构 | 自由格式 | YAML front matter + 结构化 Markdown |
| 触发方式 | 手动查阅 | AI 根据任务自动匹配 |
| 内容 | 概念说明 | 规范 + 模板 + 红线 + 验证清单 |
| 引用 | 链接跳转 | AI 按需加载 references 子文档 |

### 支持的 AI 编程助手

| 工具 | 兼容性 | 说明 |
|------|--------|------|
| Claude Code (Anthropic) | ✅ 原生支持 | 自动读取 `.agents/skills/` 目录 |
| Cursor | ✅ 支持 | 通过 `.cursorrules` 或手动引用 |
| OpenClaw | ✅ 支持 | 自动读取 skill 列表 |
| GitHub Copilot Chat | ⚠️ 需手动引用 | 在对话中粘贴 SKILL.md 内容 |
| Windsurf / 其他 | ⚠️ 需手动引用 | 将 SKILL.md 作为上下文提供 |

---

## 二、forge-codegen-crud — 简单业务 CRUD 代码生成

### 2.1 适用场景

当你需要开发一个**不涉及审批流程**的简单业务管理页面时使用，例如：

- 物料管理（单表增删改查）
- 供应商管理（主子表）
- 仓库管理（单表 + 树形）
- 任何标准 CRUD 模块

### 2.2 Skill 核心规范

#### 红线规则（Non-Negotiables）

| 规则 | 说明 | 原因 |
|------|------|------|
| 使用 Flyway 脚本 | 所有 DDL 和内置数据通过 `forge/db/migration/` | 保证数据库版本可控 |
| 租户 ID = 1 | 所有内置数据使用 `tenant_id = 1` | 项目不支持 tenant_id=0 |
| 查询 SQL 放 Mapper XML | 禁止 Service 层 LambdaQueryWrapper 查询链 | 项目规范，便于 SQL 优化 |
| POST 安全路由 | detail/create/update/delete 全部用 POST | 网关和安全策略要求 |
| 使用字典组件 | `DictSelect`、`DictTag`、`useDict()` | 禁止前端硬编码选项 |
| AiCrudPage | 前端页面使用 AiCrudPage 组件 | 配置式开发，不手写 CRUD |

#### 生成的 CRUD 接口契约

| 操作 | HTTP 方法 | 路径 | 说明 |
|------|----------|------|------|
| 分页查询 | GET | `/page` | 列表分页 |
| 详情查询 | POST | `/getById` | 用 @RequestParam 接收 id |
| 新增 | POST | `/add` | @RequestBody DTO |
| 修改 | POST | `/edit` | @RequestBody DTO |
| 删除 | POST | `/remove/{id}` | @PathVariable |
| 批量删除 | POST | `/removeBatch` | @RequestBody Long[] |

> ⚠️ 禁止生成 `PUT` 或 `DELETE` 接口。

### 2.3 使用步骤

#### 第一步：确认需求

在向 AI 提需求前，先整理好以下信息：

```
模块名称：物料管理
业务表名：biz_material
路由路径：/biz/material
菜单父级：/biz
权限前缀：biz:material

字段清单：
| 字段名 | 中文名 | 数据库类型 | Java类型 | 前端组件 | 必填 | 可搜 | 可列 | 可编 | 可导 |
|--------|--------|-----------|---------|---------|------|------|------|------|------|
| material_name | 物料名称 | varchar(128) | String | Input | ✅ | ✅ | ✅ | ✅ | ✅ |
| material_code | 物料编码 | varchar(64) | String | Input | ✅ | ✅ | ✅ | ✅ | ✅ |
| category | 类别 | varchar(32) | String | DictSelect | ✅ | ✅ | ✅ | ✅ | ✅ |
| specification | 规格型号 | varchar(128) | String | Input | | | ✅ | ✅ | ✅ |
| unit | 单位 | varchar(16) | String | Input | ✅ | | ✅ | ✅ | ✅ |
| status | 状态 | varchar(16) | String | DictSelect | ✅ | ✅ | ✅ | ✅ | ✅ |
| remark | 备注 | varchar(500) | String | Textarea | | | | ✅ | |

字典：
- biz_material_category（物料类别）：原材料、半成品、成品
- 复用 sys_enable_disable（启用/禁用）

导入导出：需要
```

#### 第二步：向 AI 提需求

在 AI 编程助手中输入：

```
请使用 forge-codegen-crud skill 生成物料管理模块的完整代码。

模块信息：
- 模块名称：物料管理
- 业务表名：biz_material
- 路由路径：/biz/material
- 菜单父级：/biz
- 权限前缀：biz:material

字段清单：
（粘贴上面的字段表）

字典：
- biz_material_category：原材料、半成品、成品
- 复用 sys_enable_disable

需要导入导出功能。
```

AI 会自动读取 `.agents/skills/forge-codegen-crud/SKILL.md` 及其 references，按项目规范生成代码。

#### 第三步：确认生成产物

AI 应生成以下文件：

**后端 Java：**

```
forge-framework/forge-plugin-parent/forge-plugin-<module>/
├── src/main/java/com/mdframe/forge/plugin/<module>/
│   ├── controller/BizMaterialController.java
│   ├── domain/entity/BizMaterial.java
│   ├── dto/BizMaterialDTO.java
│   ├── dto/BizMaterialQuery.java
│   ├── mapper/BizMaterialMapper.java
│   ├── service/BizMaterialService.java
│   ├── service/impl/BizMaterialServiceImpl.java
│   └── vo/BizMaterialVO.java
└── src/main/resources/mapper/BizMaterialMapper.xml
```

**前端 Vue：**

```
forge-admin-ui/src/views/biz/material/index.vue
```

**Flyway SQL：**

```
forge-server/db/migration/V1.0.XX__add_biz_material.sql
```

包含：
- 建表 DDL（含索引）
- 字典种子（sys_dict_type + sys_dict_data）
- Excel 导入导出配置（sys_excel_export_config + sys_excel_column_config）
- 菜单和按钮权限（sys_resource）

#### 第四步：验证

对照 `validation-checklist.md` 检查：

- [ ] Flyway 文件名使用下一个未用版本号
- [ ] 业务表包含 id、tenant_id、create_by、create_time 等系统字段
- [ ] 内置数据使用 tenant_id = 1
- [ ] 每条 INSERT 有 NOT EXISTS 保护
- [ ] Controller 使用 POST 安全路由
- [ ] 分页参数使用 pageNum + pageSize
- [ ] 查询 SQL 在 Mapper XML 中
- [ ] 前端使用 AiCrudPage
- [ ] 字典字段使用 useDict() + DictTag
- [ ] api-config 使用 `:id` 占位符

### 2.4 实战示例：生成供应商管理模块

```
请使用 forge-codegen-crud skill 生成供应商管理模块。

模块信息：
- 模块名称：供应商管理
- 业务表名：biz_supplier
- 路由路径：/biz/supplier
- 菜单父级：/biz
- 权限前缀：biz:supplier
- 页面模式：主子表（供应商主表 + 供应商品报价子表）

主表字段：
- supplier_name 供应商名称 varchar(128) String Input 必填 可搜 可列 可编 可导
- supplier_code 供应商编码 varchar(64) String Input 必填 可搜 可列 可编 可导
- contact_name 联系人 varchar(64) String Input 可列 可编 可导
- contact_phone 联系电话 varchar(20) String Input 可列 可编 可导
- status 状态 varchar(16) String DictSelect 必填 可搜 可列 可编 可导（复用 sys_enable_disable）
- remark 备注 varchar(500) String Textarea 可编 可导

子表字段（biz_supplier_quote）：
- material_id 物料ID bigint Long RecordSelector 必填 可列 可编
- quote_price 报价(分) bigint Long InputNumber 必填 可列 可编
- effective_date 有效日期 date LocalDate DatePicker 必填 可列 可编
- status 状态 varchar(16) String DictSelect 必填 可列 可编（复用 sys_enable_disable）

字典：复用 sys_enable_disable
导入导出：仅主表需要
```

---

## 三、forge-business-flow-development — 业务流程开发

### 3.1 适用场景

当你需要开发一个**涉及审批流程**的业务模块时使用，例如：

- 采购审批（代码优先模式）
- 合同审批（代码优先模式）
- 请假审批（低代码模式）
- 报销审批（低代码模式）

### 3.2 两种开发模式

Skill 提供两种工作流路径，根据业务复杂度选择：

| 模式 | 适用场景 | 特点 | 参考文档 |
|------|---------|------|---------|
| **代码优先**（Code-First） | 复杂业务逻辑、自定义表单、多节点审批 | 编写 Java Service、自定义 Controller、FlowDefinition | `references/code-first-workflow.md` |
| **低代码**（Low-Code） | 简单审批、标准 CRUD + 流程 | 配置驱动、无自定义 Java、使用 AiCrudPage | `references/lowcode-workflow.md` |

**选择建议：**

```
是否需要自定义 Java Service 逻辑？
  ├── 是 → 代码优先模式
  └── 否 → 低代码模式

是否需要自定义业务表和复杂查询？
  ├── 是 → 代码优先模式
  └── 否 → 低代码模式

是否需要多级审批、会签、驳回修改等复杂流程？
  ├── 是 → 代码优先模式
  └── 否 → 低代码模式（简单直线审批）
```

### 3.3 代码优先模式

#### 核心概念

| 概念 | 说明 | 示例 |
|------|------|------|
| Object Code | 业务对象编码 | `sample_purchase_order` |
| Model Key | 流程模型 Key | `sample_purchase_order_approval` |
| Business Key | 业务键（对象编码:记录ID） | `sample_purchase_order:123456` |
| Provider Key | 表单提供者 Key | `samplePurchaseOrder` |
| Form Key | 表单资产 Key | `sample_purchase_order_approval_form` |

#### 状态机

```
                  ┌──────────────┐
                  │    DRAFT     │ ← 新建/草稿
                  └──────┬───────┘
                         │ submit
                         ▼
                  ┌──────────────┐
           ┌─────→│  IN_PROCESS  │←─────────┐
           │      └──────┬───────┘          │
           │             │                  │
           │     ┌───────┴───────┐          │ resubmit
           │     │               │          │
           │     ▼ approve       ▼ reject   │
           │  ┌──────┐    ┌──────────────┐  │
           │  │APPROVED│   │ NEED_MODIFY  │──┘
           │  └──────┘    └──────┬───────┘
           │                      │ terminate
           │                      ▼
           │               ┌──────────────┐
           │               │  REJECTED    │
           │               └──────────────┘
           │
           └ cancel/withdraw
                  ▼
           ┌──────────────┐
           │   CANCELED   │
           └──────────────┘
```

#### 生成的文件清单

```
forge-server/forge-business/forge-business-core/
├── src/main/java/.../<domain>/
│   ├── controller/<Business>Controller.java        ← REST 接口
│   ├── domain/<Business>.java                       ← 实体类（含流程字段）
│   ├── dto/<Business>DTO.java                       ← 数据传输对象
│   ├── dto/<Business>Query.java                     ← 查询条件
│   ├── dto/<Business>SubmitDTO.java                 ← 提交审批 DTO
│   ├── dto/<Business>TaskSaveDTO.java               ← 任务保存 DTO
│   ├── mapper/<Business>Mapper.java                 ← Mapper 接口
│   ├── provider/<Business>CodeFormProvider.java     ← 表单提供者
│   ├── service/<Business>Service.java               ← Service 接口
│   ├── service/impl/<Business>ServiceImpl.java      ← Service 实现（含回调）
│   ├── support/<Business>FlowDefinition.java        ← 流程常量定义
│   ├── support/<Business>FlowBpmn.java              ← BPMN 模型构建
│   └── vo/<Business>VO.java                         ← 视图对象
├── src/main/resources/mapper/business/
│   └── <Business>Mapper.xml                         ← Mapper XML
└── forge-server/db/migration/
    └── V1.0.XX__add_<business>_flow.sql             ← 建表+字典+菜单+流程绑定
```

#### 向 AI 提需求示例

```
请使用 forge-business-flow-development skill，采用代码优先模式开发合同审批模块。

业务需求：
- 模块名称：合同审批
- Object Code：contract_approval
- 业务表：biz_contract
- 审批流程：部门负责人 → 法务审核 → 总经理审批
- 驳回：法务审核可驳回给申请人修改
- 会签：无
- 字段：合同编号、合同名称、合同金额(分)、对方单位、合同类型、附件、备注
- 审批人：部门负责人（发起人所在部门）、法务负责人（固定角色）、总经理（固定角色）

请生成完整的后端代码、SQL 迁移脚本和前端页面。
```

#### 关键实现要点

**1. FlowDefinition 常量类**

```java
public class ContractFlowDefinition {
    public static final String OBJECT_CODE = "contract_approval";
    public static final String MODEL_KEY = "contract_approval_flow";
    public static final String BUSINESS_TYPE = "contract_approval";
    public static final String PROVIDER_KEY = "contractApproval";
    public static final String FORM_KEY = "contract_approval_form";

    // 节点 Key
    public static final String NODE_DEPT_LEADER = "dept_leader_approve";
    public static final String NODE_LEGAL_REVIEW = "legal_review";
    public static final String NODE_GM_APPROVE = "gm_approve";
    public static final String NODE_APPLICANT_MODIFY = "applicant_modify";

    // 状态
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_IN_PROCESS = "IN_PROCESS";
    public static final String STATUS_NEED_MODIFY = "NEED_MODIFY";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELED = "CANCELED";
}
```

**2. 流程回调**

```java
@FlowBind(modelKey = MODEL_KEY, businessType = BUSINESS_TYPE)
public class ContractServiceImpl implements ContractService {

    @FlowCallback(on = {
        FlowCallback.ON_TASK_CREATED,
        FlowCallback.ON_TASK_COMPLETED,
        FlowCallback.ON_COMPLETED,
        FlowCallback.ON_REJECTED,
        FlowCallback.ON_CANCELED
    })
    @Transactional(rollbackFor = Exception.class)
    public void handleFlowEvent(FlowEventContext context) {
        // 根据 context 的事件类型，幂等更新业务状态
    }
}
```

**3. 状态修复**

状态修复在三个点执行，防止事件时序漂移：

| 修复点 | 说明 |
|--------|------|
| `ON_TASK_CREATED` 回调 | 申请人修改节点：`IN_PROCESS → NEED_MODIFY`；审批节点：`NEED_MODIFY → IN_PROCESS` |
| 任务表单保存 | 保存前检查并修复状态 |
| 页面/详情查询 | 查询时比对活跃任务节点，修复漂移状态 |

### 3.4 低代码模式

低代码模式不需要编写 Java 代码，通过配置实现审批流程：

1. **创建业务对象**：在应用中心创建业务对象（参考低代码应用管理教程）
2. **配置单据流程**：在设计器中绑定流程模型
3. **配置自动化动作**：在业务处理面板配置审批回调动作
4. **发布**：通过发布检查后发布

> 低代码模式的详细操作请参考[低代码应用管理实战教程](./lowcode-app-management.md)。

### 3.5 参考文档说明

Skill 包含 7 个参考文档，AI 会根据任务按需加载：

| 参考文档 | 何时读取 | 内容 |
|----------|---------|------|
| `code-first-workflow.md` | 代码优先模式 | 文件布局、开发流程、状态门控 |
| `lowcode-workflow.md` | 低代码模式 | 配置流程、平台 API、检查项 |
| `purchase-order-reference.md` | 需要查看完整示例 | 采购单模块的文件清单和行为说明 |
| `status-and-callbacks.md` | 编写回调逻辑 | 状态机、回调契约、修复规则 |
| `bpmn-configuration.md` | 配置 BPMN 节点 | 节点属性、表单权限、变量表达式 |
| `sql-templates.md` | 编写 Flyway 脚本 | 建表、字典、菜单、流程绑定完整模板 |
| `validation-checklist.md` | 测试和最终检查 | 静态检查、编译、流程运行时验证 |

---

## 四、ui-ux-pro-max — UI/UX 设计智能

### 4.1 适用场景

当前端页面需要专业的 UI/UX 设计指导时使用：

- 新建前端页面或组件
- 选择配色方案和字体
- 优化现有页面视觉体验
- 无障碍设计检查

### 4.2 使用方式

```bash
# 生成完整设计系统（必须的第一步）
python3 .agents/skills/ui-ux-pro-max/scripts/search.py "enterprise admin dashboard" --design-system -p "Forge Admin"

# 搜索特定领域
python3 .agents/skills/ui-ux-pro-max/scripts/search.py "data table" --domain ux
python3 .agents/skills/ui-ux-pro-max/scripts/search.py "dark mode" --domain style
```

### 4.3 包含的设计数据库

| 数据库 | 条目数 | 说明 |
|--------|--------|------|
| 风格库 | 67 种 | 玻璃态、新拟态、极简主义等 |
| 配色库 | 96 套 | 按产品类型分类 |
| 字体库 | 57 组 | 标题+正文字体配对 |
| 图表库 | 25 种 | 图表类型选择 |
| UX 准则 | 99 条 | 无障碍、交互、性能 |

---

## 五、Skill 文件结构详解

### 5.1 SKILL.md 格式

每个 Skill 的主文件使用 YAML front matter + Markdown 结构：

```yaml
---
name: skill-name                    # Skill 标识（与目录名一致）
description: 一段描述文字            # AI 根据这段描述判断是否匹配任务
---

# Skill 标题

## Overview
概述...

## Workflow
工作流程...

## Non-Negotiable Rules
红线规则...

## Required References
参考文档列表...
```

### 5.2 description 的作用

`description` 字段是 AI 编程助手判断是否激活该 Skill 的关键。AI 会将用户需求与所有 Skill 的 description 进行语义匹配：

- **用户说**："帮我生成一个物料管理的 CRUD 模块"
- **AI 匹配到** `forge-codegen-crud` 的 description 中的 "CRUD modules"
- **自动读取** SKILL.md 及相关 references

### 5.3 references 子文档

references 目录存放详细参考文档，AI 按需加载，避免一次性加载过多内容：

- SKILL.md：主文件，包含核心规范和红线（AI 总是先读这个）
- references/*.md：详细参考（AI 根据任务需要选择读取）

这种设计确保：
1. AI 上下文窗口不被撑爆
2. 不同复杂度的任务只加载必要的文档
3. 文档可以独立维护和更新

---

## 六、最佳实践

### 6.1 提需求的模板

向 AI 提需求时，使用以下模板可以让 Skill 发挥最大效果：

```
请使用 [skill-name] skill 完成 [任务描述]。

模块信息：
- 模块名称：xxx
- 业务表名：xxx
- 路由路径：xxx
- 菜单父级：xxx
- 权限前缀：xxx

字段清单：
| 字段名 | 中文名 | 数据库类型 | Java类型 | 前端组件 | 必填 | 可搜 | 可列 | 可编 | 可导 |
|...|...|...|...|...|...|...|...|...|...|

字典：
- xxx（新增）：值1、值2、值3
- xxx（复用已有）

[流程相关]
- 审批节点：节点1 → 节点2 → 节点3
- 驳回规则：...
- 会签：无/有

[其他]
导入导出：需要/不需要
```

### 6.2 验证生成代码

生成代码后，让 AI 自动执行验证清单：

```
请按照 .agents/skills/forge-codegen-crud/references/validation-checklist.md
对生成的代码进行验证，列出所有通过和未通过的检查项。
```

### 6.3 自定义和扩展 Skill

你可以根据项目需要自定义 Skill：

1. **修改现有 Skill**：直接编辑 `.agents/skills/*/SKILL.md` 或 references 文件
2. **新增 Skill**：在 `.agents/skills/` 下新建目录，创建 SKILL.md
3. **新增参考文档**：在 references/ 下新增 .md 文件，并在 SKILL.md 的引用列表中注册

示例：新增一个"报表开发" Skill

```
.agents/skills/forge-report-development/
├── SKILL.md
└── references/
    ├── report-template.md
    └── sql-patterns.md
```

SKILL.md 的 description 写清楚触发关键词：

```yaml
---
name: forge-report-development
description: Generate Forge report modules with chart integration, data aggregation, and export. Use when creating statistical reports, dashboards, data visualization pages, or analytical summaries.
---
```

### 6.4 团队协作

- Skill 文件纳入 Git 版本控制，团队共享
- 修改 Skill 前先通知团队，避免规范频繁变动
- 新成员加入时，让 AI 助手读取 Skill 即可快速上手项目规范
- Code Review 时可对照 Skill 的 validation-checklist 检查

---

## 七、常见问题

### Q1: AI 没有自动激活 Skill？

1. 检查 SKILL.md 的 `description` 是否包含任务相关关键词
2. 在需求中明确指定 skill 名称（如"请使用 forge-codegen-crud skill"）
3. 确认 AI 编程助手支持 `.agents/skills/` 目录读取

### Q2: 生成的代码不符合项目规范？

1. 检查 Skill 的 Non-Negotiable Rules 是否覆盖了该规范
2. 在需求中强调必须遵守的规则
3. 更新 SKILL.md 补充缺失的规范

### Q3: Skill 和 AGENTS.md 的关系？

- `AGENTS.md` 是项目级总纲，包含全局规范
- Skill 是专项指南，针对特定任务类型（CRUD、流程开发等）
- Skill 中的规则不能与 AGENTS.md 冲突
- AI 会先读 AGENTS.md，再根据任务读取对应 Skill

### Q4: 两个 Skill 可以组合使用吗？

可以。例如开发一个"采购审批模块"，既需要流程审批又需要 CRUD 管理：

```
请使用 forge-business-flow-development skill（代码优先模式）
和 forge-codegen-crud skill，开发采购审批模块。

CRUD 部分：
（字段清单...）

流程部分：
- 审批节点：部门负责人 → 采购经理 → 总经理
- 驳回：采购经理可驳回给申请人修改
```

AI 会同时读取两个 Skill 的规范，生成符合双方要求的代码。

### Q5: 如何更新 Skill 以适配项目变化？

1. 修改 SKILL.md 中的规范和红线
2. 更新 references 中受影响的参考文档
3. 在 Git 提交消息中说明变更原因（如 "更新 Skill：适配新的分页参数规范"）
4. 通知团队成员重新加载或重启 AI 编程助手
