# Forge 框架产品化建设方案

## 背景

当前 Forge 项目已经具备后台管理、数据集、业务定义、AI 大屏生成、权限租户、插件化模块等基础能力。后续不建议分散去做很多具体业务系统，而应继续深耕“AI + 数据应用生成”，先把框架打磨成能快速生成业务系统的底座。

同时，随着项目持续演进，当前遇到两个效率瓶颈：

1. 每次修改数据库表结构或初始化数据后，需要手工导出 SQL 同步到开源社区，维护成本高且容易遗漏。
2. 新项目需要使用这套框架时，目前倾向于全量复制代码并手工修改项目名、数据库名、包名等配置，容易产生分叉和维护困难。

因此，建议将 Forge 从“项目代码仓库”逐步升级为“可发布、可初始化、可同步的框架产品”。

核心方向：

> 继续深耕“AI + 数据应用生成”，不要分散去做很多业务系统；先把 Forge 打磨成能快速生成业务系统的底座。

工程化支撑：

> 数据库变更版本化 + 初始化数据标准化 + 项目模板化 + 初始化脚本自动化。

---

# 一、产品方向：深耕 AI + 数据应用生成

## 1. 战略判断

Forge 后续不应定位成普通后台管理框架，也不建议分散去做 CRM、OA、进销存、工单、设备管理等大量具体业务系统。更优的方向是：

> Forge = AI 驱动的企业数据应用生成平台。

也就是围绕已有的后台管理、数据集、业务定义、大屏设计器、AI 生成、权限租户能力，继续打磨一条完整链路：

```text
业务定义 → 数据集绑定 → AI 生成 → 结果校验 → 应用画布 → 保存版本 → 发布访问
```

这条路线的价值在于：Forge 不是直接交付一个固定业务系统，而是成为快速生成业务系统、数据大屏、分析报表和管理后台的底座。

---

## 2. 可以衍生的系统方向

基于当前框架，未来可以衍生以下几类系统。

### 2.1 AI 数据大屏平台

这是当前最顺的方向。

继续完善：

- 业务定义。
- 数据集绑定。
- AI 生成大屏。
- 生成记录。
- 大屏版本管理。
- 发布分享。
- 模板市场。

目标是包装成：

> 用自然语言生成企业驾驶舱。

---

### 2.2 低代码后台管理系统生成器

基于现有 `AiCrudPage`、后端 CRUD、数据库表配置，可以继续发展为后台系统生成器。

能力包括：

- 输入表结构，生成 CRUD 页面。
- 输入业务描述，生成表结构。
- 自动生成菜单。
- 自动生成权限。
- 自动生成前端页面。
- 自动生成后端接口。
- 自动生成 SQL。

这条路线适合做 SaaS、私有化交付或项目快速启动工具。

---

### 2.3 企业数据资产管理平台

围绕业务定义、数据集、字段语义和指标口径继续深耕，可以形成企业数据资产底座。

模块包括：

- 数据目录。
- 指标管理。
- 字段语义。
- 数据集用途说明。
- 指标口径管理。
- 数据血缘。
- AI 查询推荐。

该方向可以成为 AI 大屏生成和 AI 报表生成的基础。

---

### 2.4 AI 报表 / 分析助手

从“生成大屏”扩展到“问数、生成图表、生成报表、解释指标异常”。

典型场景：

```text
用户输入：分析本月销售下滑原因。
系统自动选择数据集，生成图表，输出分析结论。
```

这条路线比纯大屏更贴近业务用户，也更容易形成持续使用场景。

---

### 2.5 多租户企业应用平台

利用现有租户、权限、插件体系，可以衍生 CRM、OA、进销存、项目管理、工单系统、设备管理等系统。

但该方向不建议作为当前主线。原因是具体业务系统容易分散精力，也容易让 Forge 退化成普通后台框架。

更建议的做法是：

> 不直接深耕大量具体业务系统，而是深耕能快速生成这些系统的底座能力。

---

## 3. 推荐深耕模块

### 第一优先：AI 大屏生成闭环

这是当前最有差异化的能力。

需要继续补齐：

- AI 会话历史。
- 生成记录管理。
- 大屏版本管理。
- 生成结果校验。
- 数据集字段自动绑定。
- 失败重试。
- 局部修复。
- 一键应用。
- 预览发布。
- 大屏模板库。
- 生成 Prompt 模板管理。

目标是形成完整闭环：

```text
业务定义 → 数据集绑定 → AI 生成 → 校验修复 → 应用画布 → 保存版本 → 发布访问
```

---

### 第二优先：业务定义 / 数据语义中心

这是 AI 能否稳定生成有价值大屏的关键。

需要继续补齐：

- 业务对象定义。
- 指标口径管理。
- 维度管理。
- 数据集用途说明。
- 主数据集配置。
- 字段语义标签。
- 指标与字段映射。
- AI 准备度评分。
- 业务模板沉淀。

后续所有 AI 生成能力都应依赖这层语义中心。

---

### 第三优先：CRUD / 应用生成能力

把当前框架从“写代码框架”升级为“生成系统框架”。

需要继续补齐：

- 根据数据库表生成 CRUD 页面。
- 根据业务描述生成表结构。
- 自动生成菜单资源。
- 自动生成接口权限。
- 自动生成后端 Controller / Service / Mapper。
- 自动生成前端 AiCrudPage 配置。
- 支持代码预览。
- 支持差异对比。
- 支持确认后生成。

该模块成熟后，Forge 可以衍生出大量行业系统，但核心能力仍然是“生成”。

---

### 第四优先：插件市场 / 模板市场

等核心生成能力稳定后，再建设模板和插件生态。

可以沉淀：

- 大屏模板。
- CRUD 模板。
- 行业业务模板。
- 指标模板。
- 数据集模板。
- AI Prompt 模板。
- 组件模板。

模板市场的目标是提升复用率，让 Forge 从单项目能力变成可复制能力。

---

## 4. 阶段建议

### 短期 1-2 个月

聚焦打磨 AI 数据应用生成主线：

1. 打磨 AI 大屏生成闭环。
2. 补齐业务定义和数据集语义。
3. 完善生成记录、版本、发布。
4. 做 1-2 个行业 Demo，例如销售经营分析、设备运维分析、项目管理驾驶舱。

### 中期 3-6 个月

扩展生成能力：

1. AI CRUD 生成。
2. AI 报表生成。
3. AI 问数。
4. 模板市场。
5. 多租户私有化交付能力。

### 长期 6 个月以上

产品化为企业数据应用生成平台：

1. 形成 Forge CLI。
2. 支持项目模板初始化。
3. 支持插件安装和升级。
4. 支持行业模板市场。
5. 支持私有化交付和社区版协同升级。

---

# 二、数据库变更与开源社区同步方案

## 1. 当前问题

目前每次修改表结构或系统基础数据后，需要手工导出表结构和数据，再同步到开源社区版本。该方式存在以下问题：

- 操作重复，效率低。
- 容易漏导字段、索引、菜单、权限、字典等数据。
- 不容易区分“系统必须数据”和“本地测试数据”。
- 开源用户升级数据库困难。
- 历史版本缺少可追踪的迁移记录。
- 容易误导出敏感数据或无关业务数据。

## 2. 目标方案

建立一套标准的数据库发布机制：

```text
数据库结构变更 → migration 脚本
系统基础数据 → required seed 脚本
演示数据 → demo seed 脚本
社区同步 → 自动导出脚本
```

最终目标：

> 开源用户拉取项目后，可以通过一条命令完成数据库初始化或升级。

例如：

```bash
pnpm forge:init-db
```

或：

```bash
docker compose up
```

启动时自动完成表结构和基础数据初始化。

---

## 3. 表结构使用迁移脚本管理

建议引入 **Flyway** 作为数据库版本迁移工具。

目录建议：

```text
forge/
  db/
    migration/
      V1.0.0__init_schema.sql
      V1.0.1__add_ai_dashboard_generate_record.sql
      V1.0.2__alter_data_business_add_ai_fields.sql
```

每次修改数据库结构，不再手工导完整结构，而是新增一个版本脚本。

示例：

```sql
-- V1.0.2__alter_data_business_add_ai_fields.sql
ALTER TABLE data_business
  ADD COLUMN analysis_goal varchar(1000) DEFAULT NULL COMMENT '分析目标',
  ADD COLUMN metric_definition text DEFAULT NULL COMMENT '指标口径说明';
```

### 好处

- 每次数据库变更都有版本记录。
- 新用户可以从 0 初始化完整数据库。
- 老用户可以按版本自动升级。
- 社区版、私有项目版可以复用同一套迁移机制。
- 避免手工导出遗漏字段、索引或表注释。

---

## 4. 初始化数据分层管理

不要把所有数据都当成“初始化数据”导出。建议将数据拆成三类：

```text
db/seed/
  required/       必须初始化数据
  demo/           演示数据
  optional/       可选模块数据
```

建议目录：

```text
forge/
  db/
    seed/
      required/
        R__sys_resource.sql
        R__sys_dict.sql
        R__sys_config.sql
        R__ai_provider_template.sql

      demo/
        D__demo_data_business.sql
        D__demo_dashboard_project.sql
        D__demo_dataset.sql

      optional/
        O__workflow_demo.sql
        O__message_template.sql
```

### 数据分类建议

| 类型 | 是否同步到社区 | 示例 |
|---|---|---|
| 表结构 | 必须 | `CREATE TABLE`、`ALTER TABLE`、索引 |
| 系统基础数据 | 必须 | 菜单、权限、字典、配置、AI 模板 |
| 演示数据 | 可选 | 示例业务定义、示例大屏、示例数据集 |
| 本地测试数据 | 不同步 | 测试用户、测试会话、测试生成记录 |
| 敏感业务数据 | 禁止同步 | 实际客户数据、生产业务数据、API Key |

---

## 5. 社区数据导出脚本

建议新增一套自动导出命令，代替人工导 SQL。

示例命令：

```bash
pnpm forge:export-community-db
```

或：

```bash
./scripts/db/export-community-db.sh
```

该命令负责：

1. 导出社区版需要的表结构变更。
2. 导出白名单表的初始化数据。
3. 过滤本地测试数据。
4. 清理敏感字段。
5. 生成标准 SQL 文件。
6. 输出本次导出摘要。

建议目录：

```text
forge/
  scripts/
    db/
      export-community-schema.js
      export-community-seed.js
      check-sensitive-data.js
      export-community-db.sh
```

---

## 6. 数据导出白名单

只允许导出框架运行所需的系统数据。

建议允许导出的数据表：

```text
sys_resource
sys_dict
sys_config
sys_role
sys_role_resource
ai_provider_template
ai_agent
ai_model
data_business_template
dashboard_template
```

建议禁止导出的数据表：

```text
sys_user
sys_user_role
ai_chat_record
ai_chat_session
ai_dashboard_generate_record
业务数据明细表
实际客户数据表
包含 token / key / password 的表
```

如确实需要导出用户数据，应提供脱敏版本，而不是直接导出本地数据库内容。

---

## 7. 推荐落地结构

最终推荐形成如下结构：

```text
forge/
  db/
    migration/
      V1.0.0__init_schema.sql
      V1.0.1__ai_dashboard_generate_record.sql
      V1.0.2__data_business_ai_fields.sql

    seed/
      required/
        R__sys_resource.sql
        R__sys_dict.sql
        R__ai_provider_template.sql

      demo/
        D__demo_business_definition.sql
        D__demo_dashboard_project.sql
        D__demo_dataset.sql

  scripts/
    db/
      export-community-schema.js
      export-community-seed.js
      check-sensitive-data.js
      export-community-db.sh
```

---

## 8. 数据库同步实施步骤

建议按以下顺序落地：

```text
第 1 步：整理当前数据库基线 SQL
第 2 步：引入 Flyway
第 3 步：将表结构变更改为 migration 脚本
第 4 步：拆分 required/demo/optional 初始化数据
第 5 步：建立导出白名单和敏感字段黑名单
第 6 步：编写社区数据导出脚本
第 7 步：在 README 中说明数据库初始化和升级流程
```

---

# 三、新项目快速应用 Forge 框架方案

## 1. 当前问题

现在新项目如果要使用 Forge 框架，通常会倾向于：

- 全量复制当前项目代码。
- 手工修改项目名。
- 手工修改数据库名。
- 手工修改 Java 包名。
- 手工修改 Maven artifactId。
- 手工修改前端标题、Logo、端口、环境变量。

这种方式短期能用，但长期问题明显：

- 多个项目会快速分叉。
- 框架升级难以同步。
- 手工改名容易遗漏。
- 项目之间配置不一致。
- 开源版、模板版、业务项目版难以维护。

---

## 2. 总体目标

建议将 Forge 拆成四层：

```text
forge-framework       框架核心
forge-template        项目模板
forge-cli             初始化工具
business-project      具体业务项目
```

新项目不再手工复制，而是通过模板或命令初始化。

理想命令：

```bash
forge create my-project
```

或：

```bash
pnpm create forge-app my-project
```

---

## 3. 新项目应用模式

### 模式一：全量复制

适合当前马上要交付的新项目。

流程：

1. 复制当前仓库。
2. 删除本地测试数据和无关业务数据。
3. 修改项目名、数据库名、端口、前端标题。
4. 保留框架核心模块。
5. 新建业务模块。

优点：

- 启动最快。
- 不需要额外工具。
- 适合短期交付。

缺点：

- 后续框架升级困难。
- 多项目容易分叉。
- 手工修改容易遗漏。
- 不适合长期产品化。

结论：

> 全量复制只能作为短期过渡方案，不建议长期依赖。

---

### 模式二：Git Template + 初始化脚本

这是当前最推荐的中短期方案。

做法是创建一个模板仓库：

```text
forge-project-template
```

新项目创建流程：

```bash
git clone forge-project-template smart-factory
cd smart-factory
pnpm forge:init
```

初始化脚本交互式询问：

```text
项目英文名？smart-factory
项目中文名？智慧工厂管理平台
后端包名？com.company.smartfactory
数据库名？smart_factory
前端系统标题？智慧工厂管理平台
是否启用 report-ui？Yes
是否启用 AI 模块？Yes
是否初始化 demo 数据？No
```

脚本自动替换：

```text
项目名称
Java 包名
Maven artifactId
数据库名称
Redis key 前缀
Docker 服务名
前端标题
前端 Logo
默认租户名称
默认菜单名称
应用编码
端口配置
.env 配置
```

优点：

- 比全量复制更标准。
- 新项目初始化速度快。
- 项目命名和配置不容易遗漏。
- 后续可以平滑升级为 Forge CLI。

---

### 模式三：Forge CLI

这是长期产品化方案。

最终命令：

```bash
forge create
```

交互式初始化：

```text
? 项目名称: smart-factory
? 中文名称: 智慧工厂管理平台
? 后端包名: com.company.smartfactory
? 数据库名: smart_factory
? 启用模块:
  ✓ admin-ui
  ✓ report-ui
  ✓ ai
  ✓ data-business
  ✓ workflow
  ✓ message
? 是否初始化演示数据: Yes
```

生成项目结构：

```text
smart-factory/
  forge-admin-ui/
  forge-report-ui/
  forge-server/
  docker-compose.yml
  .env
  README.md
```

长期目标是让 Forge 像脚手架一样使用：

```bash
forge create crm-system
forge create iot-platform
forge create sales-dashboard
```

---

## 4. 推荐的新项目模板结构

建议模板仓库保留框架能力，但去掉本地业务数据。

```text
forge-project-template/
  forge-admin-ui/
  forge-report-ui/
  forge/
    forge-framework/
    forge-plugin-parent/
  db/
    migration/
    seed/
  scripts/
    init-project.js
    db/
  .env.example
  docker-compose.yml
  README.md
```

模板中保留：

- 权限租户基础能力。
- 系统菜单和字典。
- AI 插件基础能力。
- 数据集和业务定义模块。
- 大屏设计器模块。
- 标准 CRUD 示例。
- 初始化脚本。

模板中移除：

- 本地测试用户数据。
- 本地 AI 会话记录。
- 本地大屏生成记录。
- 敏感配置。
- 具体客户业务数据。

---

## 5. 初始化脚本职责

建议新增：

```text
scripts/init-project.js
```

该脚本负责：

1. 收集项目基础信息。
2. 替换后端包名。
3. 替换 Maven 坐标。
4. 替换前端系统名称。
5. 替换数据库名称。
6. 生成 `.env` 文件。
7. 生成初始化 SQL 配置。
8. 根据选择启用或禁用模块。
9. 清理模板中的示例缓存和临时文件。

示例命令：

```bash
pnpm forge:init
```

---

## 6. 框架与业务项目解耦

长期建议将项目拆成插件化结构：

```text
forge-core              框架核心
forge-plugin-system     系统权限插件
forge-plugin-ai         AI 插件
forge-plugin-data       数据资产插件
forge-plugin-report     大屏插件
forge-plugin-message    消息插件
project-smart-factory   具体业务项目
```

业务项目只依赖框架插件，不直接修改框架核心。

这样可以实现：

- 框架核心持续升级。
- 业务项目独立开发。
- 插件按需启用。
- 私有项目可以同步开源框架更新。
- 不同业务系统复用同一套基础能力。

---

# 四、推荐执行路线

## 第一阶段：数据库发布机制

优先解决数据库同步问题。

目标：

> 不再手工导出社区 SQL。

任务：

1. 整理当前数据库基线脚本。
2. 引入 Flyway。
3. 将后续表结构变更全部写成 migration。
4. 拆分 required/demo 初始化数据。
5. 建立社区导出白名单。
6. 编写敏感数据检查脚本。
7. 编写社区 SQL 导出脚本。

---

## 第二阶段：项目模板化

目标：

> 新项目不再手工全量复制和改名。

任务：

1. 建立 `forge-project-template` 模板仓库。
2. 提供 `.env.example`。
3. 编写 `scripts/init-project.js`。
4. 支持替换项目名、包名、数据库名、端口、前端标题。
5. 支持选择是否启用 AI、report-ui、demo 数据。
6. 编写新项目初始化文档。

---

## 第三阶段：Forge CLI

目标：

> 将 Forge 产品化为可复用脚手架。

任务：

1. 将初始化脚本封装为 CLI。
2. 支持交互式创建项目。
3. 支持模块选择。
4. 支持数据库初始化。
5. 支持模板升级。
6. 支持插件安装和卸载。

---

# 五、最终建议

当前最优方案不是继续手工导库、复制项目，而是马上建设两个基础设施：

## 1. forge-db-release

负责：

- 数据库版本迁移。
- 初始化数据管理。
- 社区 SQL 导出。
- 敏感数据过滤。
- 演示数据管理。

## 2. forge-project-template

负责：

- 新项目初始化。
- 项目名替换。
- 包名替换。
- 模块选择。
- 环境变量生成。
- demo 数据开关。

最终形成：

```text
数据库不要再手工导，改成 migration + seed。
新项目不要再手工搬，改成 template + init script。
长期再升级成 Forge CLI。
```

---

# 六、优先级清单

建议优先级：

```text
P0：整理数据库基线 SQL
P0：引入 Flyway 或等价迁移机制
P0：拆分 required/demo seed 数据
P1：编写社区数据导出脚本
P1：建立 forge-project-template 模板
P1：编写 init-project 初始化脚本
P2：模块选择和配置裁剪
P2：升级为 forge-cli
P2：框架核心与业务项目彻底解耦
```

一句话总结：

> Forge 后续应该从“一个能运行的项目”升级为“一个可初始化、可升级、可复用、可交付的企业应用生成框架”。
