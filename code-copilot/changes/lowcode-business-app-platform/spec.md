# 低代码业务应用平台重构
> status: propose
> created: 2026-05-27
> complexity: 🔴复杂

## 1. 背景与目标

当前低代码模块已经具备业务领域、数据模型、应用搭建、预览发布、AI 生成、代码预览和运行态 CRUD 能力，但整体产品形态仍偏“开发工具”和“低代码配置后台”。对于 CRM、合同、项目、采购、服务工单等明确业务场景，业务人员更希望看到的是“可直接启用和扩展的业务应用”，而不是先理解模型、页面协议、JSON 配置、发布版本和代码生成。

用户当前诉求可以概括为：

- 做 CRM 时，只需要先看到和 CRM 有关的应用与能力。
- 业务建设应从客户、联系人、线索、商机、合同、回款等业务对象开始，而不是先理解应用卡片、页面配置或代码生成。
- 对象关系、主子明细、触发器、导入、导出和报表应挂在业务对象上，成为业务人员能直接理解和维护的能力。
- 后续需要大屏看板、移动端 H5、嵌入式页面时，可以快速接入新应用。
- 前端业务界面需要高度个性化，平台只保留统一底座和可复用引擎。
- 平台核心能力应沉淀为流程、审批、报表、权限、消息等引擎。
- 应用层以单据和业务对象为中心，移动端和外部平台接入作为可选扩展。

本次重构目标是将现有“低代码搭建器中心”调整为“业务应用平台中心”：

`基础底座 → 核心引擎 → 业务套件 → 业务对象 → 应用入口 → 渠道接入 → 外部扩展`

第一阶段不重写低代码运行时，不另起独立工程，而是在现有 Forge 工程内新增业务化应用平台模块。现有 `ai_crud_config`、`ai_lowcode_model`、`ai_lowcode_domain` 和动态 CRUD 运行链路继续保留；新模块先在产品入口、应用类型、能力挂接协议和 CRM 样板上完成业务化重构。

REBUILD 示例的关键借鉴点是“业务对象驱动”：先定义订单实体，再通过引用字段建立客户和订单关系，将订单明细作为依附主订单的明细实体，然后配置列表/表单布局、触发器、报表导出和数据导入。Forge 不应照搬成纯实体管理系统，但需要把“实体 → 关系 → 明细 → 布局 → 自动化 → 导入导出/报表”作为业务建模主线。

## 2. 产品定位

### 2.1 新定位

Forge 的低代码能力不再直接对业务人员表达为“页面搭建器”或“配置中心”，而是表达为“企业应用装配平台”。

平台对业务人员提供：

- 应用中心：按 CRM、合同、项目、采购、工单等业务套件组织业务对象、业务流程、业务看板、移动入口和集成入口。
- 业务对象中心：以客户、联系人、线索、商机、合同、回款等业务对象作为建模和配置主入口。
- 引擎中心：流程、审批、报表、权限、消息等能力按需挂接到业务对象或应用入口。
- 移动端中心：H5、移动待办、移动审批、移动业务入口。
- 集成中心：标准接口、事件推送、企微、飞书、钉钉等第三方连接。
- 平台设置：组织、用户、权限、菜单、字典、文件、系统参数等基础管理。

平台对实施和开发人员保留：

- 低代码模型设计。
- 应用搭建器。
- JSON 配置诊断。
- 代码预览与下载。
- 数据源和模板等开发者工具。

### 2.2 低代码的新边界

低代码继续存在，但不作为所有业务建设的唯一主入口。它在新产品结构中的定位是：

- 负责业务对象/实体、字段、引用关系、主子明细关系等底层元数据配置。
- 负责单据模型、字段、表单规则、导入导出等元数据配置。
- 负责流程、审批、报表、权限、消息等引擎能力的挂接配置。
- 负责触发器、导入、导出、报表模板等对象级能力的配置和运行协议。
- 负责标准后台类页面的快速生成和原型验证。
- 为个性化前端提供标准接口、权限、消息、流程和数据运行能力。

低代码不强制承担：

- 所有业务前端最终页面形态。
- 所有业务系统的唯一入口。
- 高度定制 CRM、经营看板、移动 H5 的最终体验设计。

## 3. 代码现状（Research Findings）

### 3.1 现有低代码应用入口

- `forge-admin-ui/src/views/ai/lowcode-apps.vue`：当前低代码应用列表页，按业务领域组织应用，集成 AI 智能开发、数据模型设计、导入配置、新建应用、应用卡片、代码预览等能力。
- `forge-admin-ui/src/views/ai/lowcode-builder.vue`：当前低代码应用搭建器，负责应用基础信息、模型选择、页面配置、预览、保存草稿、代码预览、下载代码和发布上线。
- `forge-admin-ui/src/views/ai/lowcode-models.vue`：当前数据模型设计页，维护 `ai_lowcode_model.model_schema`。
- `forge-admin-ui/src/views/ai/crud-page.vue`：当前动态低代码运行页，通过 `configKey` 渲染运行态 CRUD。

### 3.2 现有后端低代码能力

- `LowcodeAppController`：提供低代码应用分页、详情、草稿、预览、发布、版本、回滚、迁移领域等接口。
- `LowcodeModelController`：提供低代码模型分页、详情、创建、更新、启停、删除、DDL 预览等接口。
- `AiCrudConfigController`：提供 `ai_crud_config` 配置管理、运行渲染、AI 生成和代码下载等能力。
- `DynamicCrudController`：提供 `/ai/crud/{configKey}` 动态 CRUD 运行接口。
- `AiCrudConfigGenerateService` 和 `LowcodeRuntimeConfigBuilder`：已经能将模型和页面协议转换为运行态 `AiCrudConfig`。

### 3.3 现有架构优势

- 认证、权限、租户、菜单、字典、文件、消息、流程等底座能力已经存在。
- 现有动态 CRUD 运行时可以继续承载标准单据类应用。
- 现有模型协议和页面协议可作为业务对象和标准应用入口的底层配置来源。
- 现有代码生成能力可继续服务开发者和交付团队。

### 3.4 当前主要问题

- 产品入口偏技术视角，业务人员需要理解过多低代码概念。
- 应用入口先行，缺少“业务对象/实体”这一层核心建模语言。
- 应用、模型、页面、发布、代码生成被压在一个低代码工作台里，边界较重。
- CRM 这类业务场景缺少“开箱即用的业务应用中心”表达。
- 主子表仍偏页面模板概念，没有上升为业务对象之间的基础关系类型。
- 导入、导出、报表、触发器偏系统配置，没有挂到客户、合同、商机等业务对象上。
- 看板、H5、外部平台接入当前更像附加功能，而不是标准应用类型。
- 业务前端个性化与通用 CRUD 页面生成之间存在天然张力。

## 4. 总体方案

### 4.1 工程归属

本次不新建独立工程，继续基于现有 Forge 工程演进。

原因：

- 现有工程已经具备所需底座能力，重新起工程会重复建设认证、权限、租户、组织、消息、文件、菜单和流程能力。
- 当前阶段产品形态仍需验证，直接拆独立工程会放大成本和边界不确定性。
- 在现有工程内逻辑隔离，可以快速复用底座并降低重构风险。

推荐做法：

- 后端优先在 `forge-plugin-generator` 内新增业务应用平台相关服务，或新增 `forge-plugin-app` 作为业务应用平台插件。
- 前端新增 `src/views/app-center/` 页面目录，不直接重写 `src/views/ai/lowcode-apps.vue`。
- 数据库第一阶段可复用现有低代码表承载底层模型和运行态配置，业务化主数据优先通过轻量扩展表表达，避免继续堆入 `options` JSON。
- 旧低代码入口保留给开发者和历史兼容，业务人员使用新“应用中心”入口。

### 4.2 新信息架构

业务建模主线调整为：

`业务套件 → 业务对象/实体 → 对象关系 → 对象能力 → 应用入口`

| 层级 | 示例 | 说明 |
|------|------|------|
| 业务套件 | CRM | 组织一组业务对象、流程、看板、移动入口和集成能力 |
| 业务对象/实体 | 客户、联系人、线索、商机、合同、回款 | 业务人员理解和维护的核心对象，也是低代码模型的业务化表达 |
| 对象关系 | 客户-联系人、客户-商机、商机-合同、合同-回款、合同-合同明细 | 通过引用、主子明细、关联列表等方式表达对象之间的业务关系 |
| 对象能力 | 列表、表单、详情、导入、导出、报表、触发器、审批、消息、权限 | 挂在对象上的标准能力，避免业务人员到系统配置里寻找 |
| 应用入口 | PC 后台、H5、看板、嵌入页、第三方推送 | 面向不同渠道和场景的入口，不取代业务对象本身 |

新增业务化导航结构：

| 一级入口 | 面向对象 | 说明 |
|----------|----------|------|
| 应用中心 | 业务人员、实施人员 | 展示 CRM、合同、项目、采购、工单等业务套件、业务对象和应用入口 |
| 引擎中心 | 产品负责人、实施顾问 | 管理流程、审批、报表、权限、消息等通用能力 |
| 移动端中心 | 移动场景负责人 | 管理 H5、移动待办、移动审批、移动业务入口 |
| 集成中心 | 集成实施人员 | 管理开放接口、事件推送、企微、飞书、钉钉 |
| 平台设置 | 系统管理员 | 管理组织、用户、菜单、字典、文件和系统参数 |
| 开发者工具 | 平台开发人员 | 保留模型设计、低代码搭建器、JSON 配置、代码生成等能力 |

### 4.3 应用类型

新增标准应用类型：

应用类型只表达入口和渠道形态，不替代业务对象/实体。客户管理、合同管理这类标准后台入口可以关联到业务对象；销售看板、移动拜访、第三方推送则作为面向场景的应用入口存在。

| 应用类型 | 编码 | 说明 |
|----------|------|------|
| 业务应用 | `BUSINESS` | 客户管理、合同管理、商机管理、工单管理等标准对象入口 |
| 嵌入应用 | `EMBEDDED` | 大屏、BI、外部系统页面、第三方页面嵌入 |
| 移动应用 | `MOBILE` | H5 页面、移动审批、移动待办、移动业务入口 |
| 集成应用 | `INTEGRATION` | 开放接口、Webhook、企微、飞书、钉钉推送配置 |

### 4.4 业务对象 / 实体模型

业务对象/实体是本次重构的核心建模语言，位于业务套件和应用入口之间。

| 概念 | 示例 | 产品表达 |
|------|------|----------|
| 业务套件 | CRM | 一组业务对象和业务场景的集合 |
| 业务对象/实体 | 客户、联系人、线索、商机、合同、回款 | 业务人员看到和维护的核心对象 |
| 引用关系 | 商机关联客户、合同关联合同主体 | 业务化表达为“关联客户”“关联合同”，不暴露外键概念 |
| 明细关系 | 合同-合同明细、订单-订单明细 | 明细实体依附主实体，是对象关系类型，不只是页面模板 |
| 关联列表 | 客户下的联系人、客户下的商机 | 在详情页呈现下级对象或关联对象 |
| 对象布局 | 列表布局、表单布局、详情布局 | 围绕对象配置字段展示和交互 |
| 对象自动化 | 金额汇总、逾期提醒、阶段变化通知 | 轻量业务触发器，不等同于 Flowable 审批流程 |
| 对象数据能力 | 导入、导出、报表模板 | 挂在对象上，例如“导入客户”“导出合同”“合同报表” |

第一阶段可以继续复用 `ai_lowcode_model` 承载底层模型 Schema，但产品文案和导航必须把“模型”表达为“业务对象/实体”。如果新增表结构，建议使用 `ai_business_object` 显式承载业务对象，避免 `ai_business_app` 同时承担对象和入口两种职责。

### 4.5 能力挂接模型

业务应用和业务对象不直接内置所有能力，而是通过标准挂接协议按需启用。挂接目标应支持业务套件、业务对象和应用入口三类对象。

| 能力类型 | 编码 | 示例 |
|----------|------|------|
| 流程能力 | `FLOW` | 商机推进流程、合同流转流程 |
| 审批能力 | `APPROVAL` | 合同审批、折扣审批、回款审批 |
| 报表能力 | `REPORT` | 销售看板、客户增长分析、回款分析 |
| 权限能力 | `PERMISSION` | 区域可见、部门可见、个人可见 |
| 消息能力 | `MESSAGE` | 到期提醒、审批通知、商机跟进提醒 |
| 触发器能力 | `TRIGGER` | 合同金额自动汇总、阶段变更提醒、逾期待办生成 |
| 数据导入能力 | `IMPORT` | 导入客户、导入联系人、导入合同明细 |
| 数据导出能力 | `EXPORT` | 导出客户、导出合同、导出回款记录 |
| 移动能力 | `MOBILE` | H5 待办、移动客户拜访 |
| 集成能力 | `INTEGRATION` | 企微通知、飞书待办、钉钉消息 |

## 5. 功能点

### 5.1 应用中心

- [ ] 新增业务化“应用中心”菜单。
- [ ] 应用中心按业务套件分组展示，进入套件后优先看到业务对象、业务流程、业务看板、移动入口和集成入口。
- [ ] 支持按应用类型筛选：业务应用、嵌入应用、移动应用、集成应用。
- [ ] 支持按业务套件筛选，例如 CRM、合同、项目、采购、服务。
- [ ] 支持业务对象卡片展示：对象名称、业务说明、对象类型、接入能力、最近更新。
- [ ] 支持应用入口卡片展示：入口名称、业务说明、应用类型、启用状态、接入能力、最近更新。
- [ ] 支持应用入口打开、配置、启停、复制；导入、导出优先作为对象级能力进入。
- [ ] 标准业务应用可继续打开现有 `/ai/crud-page/{configKey}` 运行页。
- [ ] 嵌入应用可打开内部路由、外部 URL 或 iframe 页面。
- [ ] 移动应用可配置 H5 入口和移动端可见范围。
- [ ] 集成应用可进入集成配置详情。

### 5.2 业务对象中心

- [ ] 新增业务对象/实体配置视图，支持在业务套件下维护客户、合同、商机等对象。
- [ ] 支持配置对象基本信息：对象名称、对象编码、对象类型、所属套件、业务说明、图标、启用状态。
- [ ] 支持对象关联底层低代码模型；第一阶段可复用 `ai_lowcode_model`，但产品侧展示为业务对象/实体。
- [ ] 支持引用关系配置，例如“商机关联客户”“合同关联商机”“回款关联合同”。
- [ ] 支持明细关系配置，例如“合同-合同明细”“订单-订单明细”，明细实体依附主实体。
- [ ] 支持对象详情中的关联列表，例如客户下的联系人、客户下的商机、合同下的回款记录。
- [ ] 支持对象布局配置入口：列表布局、表单布局、详情布局。
- [ ] 支持对象级导入、导出、报表模板入口，例如“导入客户”“导出合同”“客户报表”。
- [ ] 支持对象级触发器入口，例如金额汇总、阶段变化提醒、逾期待办生成。
- [ ] JSON、Schema、表名、字段名等技术配置仅在开发者模式展示。

### 5.3 CRM 样板应用

- [ ] 内置 CRM 业务套件分组。
- [ ] CRM 一期包含客户、联系人、线索、商机、合同、合同明细、回款、跟进记录、销售任务等业务对象。
- [ ] CRM 业务对象使用业务语言描述，不展示底层表名和 JSON 配置。
- [ ] CRM 对象关系至少覆盖客户-联系人、客户-商机、商机-合同、合同-合同明细、合同-回款、客户-跟进记录。
- [ ] CRM 单据和对象支持关联审批、消息、权限、报表、触发器、导入和导出能力。
- [ ] CRM 后续可增量接入销售看板、经营大屏、移动拜访和第三方通知。

### 5.4 引擎中心

- [ ] 新增“引擎中心”业务入口。
- [ ] 展示流程引擎、审批引擎、报表引擎、权限引擎、消息引擎。
- [ ] 每个引擎展示可用状态、已接入业务对象/应用数量、最近配置。
- [ ] 第一阶段不重构引擎底层，只提供统一入口和对象/应用挂接视图。
- [ ] 后续逐步将流程、审批、报表、权限、消息配置标准化为应用能力。

### 5.5 嵌入应用

- [ ] 支持新增嵌入应用。
- [ ] 支持嵌入模式：内部路由、iframe、外部打开。
- [ ] 支持配置标题、图标、访问地址、打开方式、权限标识。
- [ ] 支持看板类应用作为嵌入应用接入。
- [ ] 嵌入应用必须经过权限控制，不能绕过平台登录和菜单权限。

### 5.6 移动应用

- [ ] 支持登记移动 H5 应用入口。
- [ ] 支持配置移动端可见范围、入口图标、入口名称、目标地址。
- [ ] 支持移动待办、移动审批、移动业务入口作为应用类型。
- [ ] 第一阶段只完成入口管理，不强制建设完整移动端容器。

### 5.7 集成应用

- [ ] 支持登记标准接口类应用。
- [ ] 支持登记事件推送和 Webhook 应用。
- [ ] 支持企微、飞书、钉钉作为第三方平台类型。
- [ ] 第一阶段只完成集成应用定义和入口管理，具体推送通道后续分阶段实现。

### 5.8 开发者工具收敛

- [ ] 现有低代码应用管理、模型设计、搭建器、JSON 配置、代码生成逐步移动到开发者工具。
- [ ] 普通业务用户默认不展示纯 JSON 配置入口。
- [ ] 历史路由继续保留，避免已发布应用和开发者入口失效。
- [ ] `ai_crud_config` 继续作为运行时事实来源，不直接删除。

## 6. 业务规则

- 应用中心面向业务人员，页面文案必须使用业务语言，避免暴露 `configKey`、JSON、Schema 等技术概念作为主信息。
- 一个业务对象必须归属一个业务套件或业务分类；未分类对象进入“未分类对象”分组。
- 一个应用入口必须归属一个业务套件；标准业务应用应优先关联到一个业务对象。
- 业务对象是建模主资产，应用入口是访问和场景入口，二者不能混作同一个概念。
- 对象关系必须使用业务语言表达，例如“关联客户”“关联合同”“合同明细”，避免让业务人员理解外键、Join、Schema 等技术概念。
- 主子表必须作为业务对象关系类型维护，不只作为页面模板或代码生成类型维护。
- 业务对象可以绑定一个或多个能力引擎，但第一阶段只要求保存挂接配置，不要求所有引擎完成深度联动。
- 触发器属于轻量业务自动化，用于字段计算、状态提醒、待办生成等场景；复杂长流程仍使用 Flowable 审批/流程能力。
- 导入、导出和报表模板应优先挂在业务对象上，而不是只作为系统级 Excel 配置入口。
- 标准单据类业务应用优先复用现有动态 CRUD 运行时。
- 高度个性化前端应用可以作为嵌入应用或独立业务前端接入平台底座。
- 低代码搭建器继续作为开发者能力存在，但不作为业务用户主入口。
- 嵌入应用和外部链接必须经过权限判断，禁止配置无权限控制的敏感入口。
- 第三方平台推送不得硬编码企业密钥、Token、Webhook Secret。
- CRM 样板应用只作为首期产品样板，不限制后续接入其他业务套件。

## 7. 数据变更

第一阶段建议控制表结构变更，但数据模型要避免把“业务套件、业务对象、应用入口、能力挂接”都压进 `ai_business_app`。推荐分层如下：

| 表名 | 定位 |
|------|------|
| `ai_business_suite` | 业务套件，例如 CRM、合同、项目、采购、服务 |
| `ai_business_object` | 业务对象/实体，例如客户、联系人、商机、合同、回款 |
| `ai_business_object_relation` | 对象关系，例如引用关系、明细关系、关联列表 |
| `ai_business_app` | 应用入口，例如客户管理、销售看板、移动拜访、第三方推送 |
| `ai_business_binding` | 能力挂接，例如审批、报表、消息、触发器、导入、导出 |

如果第一阶段不新增全部表，可以先复用 `ai_lowcode_domain` 承载业务套件、复用 `ai_lowcode_model` 承载业务对象，但产品文案、接口 VO 和前端页面必须表达为“业务套件”和“业务对象/实体”。

### 7.1 复用表

| 表名 | 用途 |
|------|------|
| `ai_crud_config` | 继续保存标准业务应用运行时配置 |
| `ai_lowcode_model` | 继续保存业务数据模型；第一阶段可作为业务对象底层模型 |
| `ai_lowcode_domain` | 继续保存业务领域；第一阶段可作为业务套件基础 |
| `ai_crud_config_version` | 继续保存发布版本 |
| `sys_resource` | 保存新增菜单、按钮和应用入口权限 |
| `sys_dict_type/sys_dict_data` | 保存应用类型、接入类型、引擎类型等字典 |

### 7.2 新增或扩展建议

优先方案：新增业务化扩展表，避免继续把所有业务化属性塞进 `ai_crud_config.options`。

| 操作 | 表名 | 说明 |
|------|------|------|
| 新增 | `ai_business_suite` | 业务套件主表，承载 CRM、合同、项目等套件 |
| 新增 | `ai_business_object` | 业务对象/实体表，承载客户、合同、商机等对象 |
| 新增 | `ai_business_object_relation` | 对象关系表，承载引用、明细、关联列表等关系 |
| 新增 | `ai_business_app` | 应用入口表，承载 PC 后台、H5、看板、嵌入页、集成入口 |
| 新增 | `ai_business_binding` | 能力挂接表，承载流程、审批、报表、权限、消息、触发器、导入、导出、移动、集成绑定 |
| 新增 | `ai_business_app_version` | 可选，应用入口配置版本；第一阶段可复用 `ai_crud_config_version` |

### 7.3 `ai_business_suite` 建议字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `tenant_id` | bigint | 租户 ID，默认 1 |
| `suite_code` | varchar | 套件编码，例如 CRM |
| `suite_name` | varchar | 套件名称，例如 CRM |
| `icon` | varchar | 套件图标 |
| `description` | varchar | 业务说明 |
| `status` | int | 状态：1 启用，0 禁用 |
| `sort_order` | int | 排序 |
| `options` | json | 扩展配置 |
| 标准字段 | - | `create_by/create_time/create_dept/update_by/update_time` |

### 7.4 `ai_business_object` 建议字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `tenant_id` | bigint | 租户 ID，默认 1 |
| `suite_code` | varchar | 所属业务套件编码 |
| `object_code` | varchar | 业务对象编码，例如 CUSTOMER、CONTRACT |
| `object_name` | varchar | 业务对象名称，例如 客户、合同 |
| `object_type` | varchar | 对象类型：MASTER/DETAIL/LOOKUP/TRANSACTION |
| `model_id` | bigint | 关联 `ai_lowcode_model.id`，可为空 |
| `model_code` | varchar | 关联底层模型编码，可为空 |
| `display_field` | varchar | 默认展示字段，例如客户名称、合同名称 |
| `icon` | varchar | 对象图标 |
| `description` | varchar | 业务说明 |
| `status` | int | 状态：1 启用，0 禁用 |
| `sort_order` | int | 排序 |
| `options` | json | 扩展配置 |
| 标准字段 | - | `create_by/create_time/create_dept/update_by/update_time` |

### 7.5 `ai_business_object_relation` 建议字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `tenant_id` | bigint | 租户 ID，默认 1 |
| `suite_code` | varchar | 所属业务套件编码 |
| `source_object_code` | varchar | 来源对象编码 |
| `target_object_code` | varchar | 目标对象编码 |
| `relation_type` | varchar | REFERENCE/DETAIL/CHILD_LIST/MANY_TO_MANY |
| `relation_name` | varchar | 关系名称，例如 关联客户、合同明细 |
| `source_field_code` | varchar | 来源对象字段编码，可为空 |
| `target_field_code` | varchar | 目标对象字段编码，可为空 |
| `relation_config` | json | 关系配置 |
| `status` | int | 状态：1 启用，0 禁用 |
| 标准字段 | - | `create_by/create_time/create_dept/update_by/update_time` |

### 7.6 `ai_business_app` 建议字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `tenant_id` | bigint | 租户 ID，默认 1 |
| `app_code` | varchar | 应用入口编码，唯一 |
| `app_name` | varchar | 应用入口名称 |
| `app_type` | varchar | 应用类型：BUSINESS/EMBEDDED/MOBILE/INTEGRATION |
| `suite_code` | varchar | 业务套件编码，例如 CRM |
| `object_code` | varchar | 关联业务对象编码，非对象入口可为空 |
| `entry_mode` | varchar | 入口模式：RUNTIME/ROUTE/IFRAME/EXTERNAL/H5/API |
| `entry_url` | varchar | 应用入口地址 |
| `config_key` | varchar | 关联现有 `ai_crud_config.config_key`，非标准应用可为空 |
| `icon` | varchar | 应用图标 |
| `description` | varchar | 业务说明 |
| `status` | int | 状态：1 启用，0 禁用 |
| `sort_order` | int | 排序 |
| `options` | json | 扩展配置 |
| 标准字段 | - | `create_by/create_time/create_dept/update_by/update_time` |

### 7.7 `ai_business_binding` 建议字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `tenant_id` | bigint | 租户 ID，默认 1 |
| `target_type` | varchar | 挂接目标类型：SUITE/OBJECT/APP |
| `target_id` | bigint | 挂接目标 ID |
| `target_code` | varchar | 挂接目标编码，便于跨环境迁移 |
| `binding_type` | varchar | FLOW/APPROVAL/REPORT/PERMISSION/MESSAGE/TRIGGER/IMPORT/EXPORT/MOBILE/INTEGRATION |
| `binding_key` | varchar | 关联外部能力的业务键 |
| `binding_name` | varchar | 挂接名称 |
| `binding_config` | json | 挂接配置 |
| `status` | int | 状态：1 启用，0 禁用 |
| 标准字段 | - | `create_by/create_time/create_dept/update_by/update_time` |

### 7.8 字典建议

| 字典类型 | 说明 | 建议值 |
|----------|------|--------|
| `ai_business_suite` | 业务套件 | CRM、CONTRACT、PROJECT、PURCHASE、SERVICE |
| `ai_business_object_type` | 业务对象类型 | MASTER、DETAIL、LOOKUP、TRANSACTION |
| `ai_business_relation_type` | 对象关系类型 | REFERENCE、DETAIL、CHILD_LIST、MANY_TO_MANY |
| `ai_business_app_type` | 业务应用类型 | BUSINESS、EMBEDDED、MOBILE、INTEGRATION |
| `ai_business_app_entry_mode` | 应用入口模式 | RUNTIME、ROUTE、IFRAME、EXTERNAL、H5、API |
| `ai_business_binding_type` | 能力挂接类型 | FLOW、APPROVAL、REPORT、PERMISSION、MESSAGE、TRIGGER、IMPORT、EXPORT、MOBILE、INTEGRATION |

## 8. 接口变更

### 8.1 新增业务套件接口

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/business/suite/page` | GET | 分页查询业务套件 |
| 新增 | `/ai/business/suite/list` | GET | 查询业务套件列表 |
| 新增 | `/ai/business/suite/{id}` | GET | 查询业务套件详情 |
| 新增 | `/ai/business/suite` | POST | 新增业务套件 |
| 新增 | `/ai/business/suite` | PUT | 修改业务套件 |
| 新增 | `/ai/business/suite/{id}` | DELETE | 删除业务套件 |
| 新增 | `/ai/business/suite/summary` | GET | 查询业务套件对象数量、应用数量、启用数量、最近更新 |

### 8.2 新增业务对象接口

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/business/object/page` | GET | 分页查询业务对象 |
| 新增 | `/ai/business/object/list` | GET | 查询业务对象列表，用于套件详情和对象选择 |
| 新增 | `/ai/business/object/{id}` | GET | 查询业务对象详情 |
| 新增 | `/ai/business/object` | POST | 新增业务对象 |
| 新增 | `/ai/business/object` | PUT | 修改业务对象 |
| 新增 | `/ai/business/object/{id}` | DELETE | 删除业务对象 |
| 新增 | `/ai/business/object/{id}/status` | PUT | 启停业务对象 |
| 新增 | `/ai/business/object/{id}/runtime-info` | GET | 获取对象对应运行态配置、入口和权限信息 |

### 8.3 新增对象关系接口

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/business/object/{objectId}/relations` | GET | 查询对象关系 |
| 新增 | `/ai/business/object/{objectId}/relations` | POST | 保存对象关系 |
| 新增 | `/ai/business/object/{objectId}/relations/{relationId}` | DELETE | 删除对象关系 |

### 8.4 新增应用入口接口

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/business/app/page` | GET | 分页查询应用入口 |
| 新增 | `/ai/business/app/list` | GET | 查询应用入口列表，用于卡片和入口 |
| 新增 | `/ai/business/app/{id}` | GET | 查询应用入口详情 |
| 新增 | `/ai/business/app` | POST | 新增应用入口 |
| 新增 | `/ai/business/app` | PUT | 修改应用入口 |
| 新增 | `/ai/business/app/{id}` | DELETE | 删除应用入口 |
| 新增 | `/ai/business/app/{id}/status` | PUT | 启停应用入口 |
| 新增 | `/ai/business/app/{id}/open-info` | GET | 获取应用打开方式和权限校验结果 |

### 8.5 新增能力挂接接口

| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 新增 | `/ai/business/binding/list` | GET | 按 `targetType` 和 `targetId` 查询挂接能力 |
| 新增 | `/ai/business/binding` | POST | 新增能力挂接 |
| 新增 | `/ai/business/binding` | PUT | 修改能力挂接 |
| 新增 | `/ai/business/binding/{id}` | DELETE | 删除能力挂接 |
| 新增 | `/ai/business/binding/batch-save` | POST | 批量保存对象或应用挂接能力 |

### 8.6 兼容接口

| 接口 | 兼容策略 |
|------|----------|
| `/ai/lowcode/app/**` | 保留，继续服务现有低代码应用管理和搭建器 |
| `/ai/lowcode/model/**` | 保留，继续服务底层模型设计；在业务页面表达为业务对象/实体 |
| `/ai/crud-page/{configKey}` | 保留，作为标准业务应用运行入口 |
| `/ai/crud/{configKey}/**` | 保留，作为动态 CRUD 数据接口 |
| `/ai/crud-config/**` | 保留给开发者工具和历史兼容，普通业务菜单隐藏 |

## 9. 前端变更

### 9.1 新增页面目录

建议新增：

| 路径 | 说明 |
|------|------|
| `forge-admin-ui/src/views/app-center/index.vue` | 应用中心首页 |
| `forge-admin-ui/src/views/app-center/suite-detail.vue` | 业务套件详情页 |
| `forge-admin-ui/src/views/app-center/object-detail.vue` | 业务对象详情页 |
| `forge-admin-ui/src/views/app-center/components/AppCard.vue` | 应用卡片 |
| `forge-admin-ui/src/views/app-center/components/ObjectCard.vue` | 业务对象卡片 |
| `forge-admin-ui/src/views/app-center/components/AppFilterBar.vue` | 应用筛选区 |
| `forge-admin-ui/src/views/app-center/components/ObjectRelationPanel.vue` | 对象关系面板 |
| `forge-admin-ui/src/views/app-center/components/AppEditorDrawer.vue` | 应用编辑抽屉 |
| `forge-admin-ui/src/views/app-center/components/BusinessBindingPanel.vue` | 对象/应用能力挂接面板 |
| `forge-admin-ui/src/views/app-center/engine-center.vue` | 引擎中心 |
| `forge-admin-ui/src/views/app-center/mobile-center.vue` | 移动端中心 |
| `forge-admin-ui/src/views/app-center/integration-center.vue` | 集成中心 |

### 9.2 页面体验要求

- 应用中心默认展示业务套件和业务对象，不展示模型字段和 JSON 配置。
- CRM 套件详情默认展示客户、联系人、线索、商机、合同、回款等业务对象，应用入口作为对象之外的场景入口展示。
- 业务对象详情建议使用“布局、关系、导入导出、报表、触发器、审批、消息、权限”等页签组织能力。
- CRM 业务对象和应用入口卡片优先展示业务名称、说明、能力标签和启用状态。
- 操作按钮使用业务语言：打开、配置、启用、停用、接入能力。
- 对象关系配置使用“关联客户”“合同明细”“下级联系人”等业务文案。
- 技术字段如 `configKey`、`tableName`、`modelSchema` 只在详情或开发者模式中展示。
- 嵌入应用打开时必须根据 `entryMode` 选择内部路由、iframe、外部窗口或 H5 入口。
- 页面应保持后台业务系统风格，信息密度适中，不做营销式落地页。

### 9.3 旧页面处理

- `src/views/ai/lowcode-apps.vue` 保留，不作为业务用户默认入口。
- `src/views/ai/lowcode-builder.vue` 保留，从应用配置或开发者工具进入。
- `src/views/ai/crud-config.vue` 隐藏普通菜单，仅开发者可见。
- `src/views/ai/crud-generator.vue` 隐藏普通菜单，AI 生成能力后续迁移到应用中心。

## 10. 后端变更

### 10.1 推荐模块

第一阶段可在 `forge-plugin-generator` 内新增业务应用包：

```text
com.mdframe.forge.plugin.generator.businessapp
├── controller
├── service
├── service.impl
├── mapper
├── entity
├── dto
└── vo
```

如后续业务应用平台成为独立产品，再评估抽成 `forge-plugin-app`。

### 10.2 推荐类

| 类 | 说明 |
|----|------|
| `BusinessSuiteController` | 业务套件接口 |
| `BusinessObjectController` | 业务对象接口 |
| `BusinessObjectRelationController` | 对象关系接口 |
| `BusinessAppController` | 应用入口接口 |
| `BusinessBindingController` | 能力挂接接口 |
| `BusinessSuiteService` | 业务套件管理 |
| `BusinessObjectService` | 业务对象管理 |
| `BusinessObjectRelationService` | 对象关系管理 |
| `BusinessAppService` | 应用入口管理 |
| `BusinessBindingService` | 对象/应用能力挂接管理 |
| `BusinessAppOpenService` | 应用打开方式解析和权限校验 |
| `BusinessSuiteMapper.xml` | 业务套件查询 SQL |
| `BusinessObjectMapper.xml` | 业务对象查询 SQL |
| `BusinessObjectRelationMapper.xml` | 对象关系查询 SQL |
| `BusinessAppMapper.xml` | 应用查询 SQL |
| `BusinessBindingMapper.xml` | 能力挂接查询 SQL |

### 10.3 查询规则

- 复杂查询必须写 Mapper XML。
- 分页参数使用 `pageNum` 和 `pageSize`。
- 新增业务表包含租户和标准审计字段。
- 业务内置数据 `tenant_id` 必须为 `1`。
- 菜单和权限资源通过 Flyway 脚本维护，必须 `NOT EXISTS` 防重复。

## 11. 菜单与权限

### 11.1 新增菜单

建议新增一级或二级菜单：

| 菜单 | 路由 | 说明 |
|------|------|------|
| 应用中心 | `/app-center` | 业务套件、业务对象和应用入口主入口 |
| 业务套件详情 | `/app-center/suite/:suiteCode` | 展示套件下的业务对象、流程、看板和入口 |
| 业务对象详情 | `/app-center/object/:objectCode` | 展示对象布局、关系和对象能力 |
| 引擎中心 | `/app-center/engines` | 核心引擎统一入口 |
| 移动端中心 | `/app-center/mobile` | H5 和移动入口 |
| 集成中心 | `/app-center/integration` | 外部集成入口 |

### 11.2 旧菜单处理

- 低代码应用管理：移动到开发者工具或隐藏普通业务权限。
- 数据模型设计：移动到开发者工具，实施人员可见。
- 应用配置管理：仅开发者可见。
- AI 表单生成：普通菜单隐藏，能力后续迁移。
- 代码生成表管理：保留兼容，普通菜单隐藏。

### 11.3 权限建议

| 权限标识 | 说明 |
|----------|------|
| `ai:businessApp:list` | 查看应用中心 |
| `ai:businessSuite:list` | 查看业务套件 |
| `ai:businessSuite:edit` | 维护业务套件 |
| `ai:businessObject:list` | 查看业务对象 |
| `ai:businessObject:add` | 新增业务对象 |
| `ai:businessObject:edit` | 修改业务对象 |
| `ai:businessObject:delete` | 删除业务对象 |
| `ai:businessObject:relation` | 配置对象关系 |
| `ai:businessBinding:config` | 配置对象或应用能力挂接 |
| `ai:businessApp:add` | 新增应用入口 |
| `ai:businessApp:edit` | 修改应用入口 |
| `ai:businessApp:delete` | 删除应用入口 |
| `ai:businessApp:status` | 启停应用入口 |
| `ai:businessApp:open` | 打开应用入口 |

## 12. CRM 一期样板

### 12.1 业务对象范围

CRM 一期建议先内置以下业务对象定义：

| 业务对象 | 对象类型 | 说明 |
|------|------|------|
| 客户 | MASTER | 管理客户基本信息、客户分级、所属区域 |
| 联系人 | DETAIL/CHILD_LIST | 管理客户联系人和联系方式，通常依附或关联客户 |
| 线索 | MASTER | 管理销售线索、来源、跟进状态 |
| 商机 | TRANSACTION | 管理商机阶段、预计金额、赢单概率 |
| 合同 | TRANSACTION | 管理合同信息、合同审批和归档 |
| 合同明细 | DETAIL | 管理合同商品、服务、金额等明细项，依附合同 |
| 回款 | DETAIL/TRANSACTION | 管理回款计划、回款记录和逾期提醒，关联合同 |
| 跟进记录 | DETAIL/CHILD_LIST | 管理客户拜访、电话、会议等记录 |
| 销售任务 | TRANSACTION | 管理销售计划、待办和任务完成情况 |

### 12.2 对象关系范围

CRM 一期建议至少内置以下对象关系：

| 关系 | 关系类型 | 说明 |
|------|----------|------|
| 客户-联系人 | CHILD_LIST | 客户详情中展示联系人列表 |
| 客户-商机 | CHILD_LIST | 客户详情中展示相关商机 |
| 商机-合同 | REFERENCE | 合同可关联来源商机 |
| 合同-合同明细 | DETAIL | 合同明细依附合同，支持金额汇总 |
| 合同-回款 | CHILD_LIST | 合同详情中展示回款计划或回款记录 |
| 客户-跟进记录 | CHILD_LIST | 客户详情中展示拜访、电话、会议记录 |
| 商机-跟进记录 | CHILD_LIST | 商机详情中展示推进记录 |

### 12.3 对象能力范围

CRM 一期建议以内置配置方式体现以下对象能力：

| 业务对象 | 对象能力 |
|----------|----------|
| 客户 | 列表、表单、详情、导入、导出、客户报表、权限 |
| 商机 | 列表、表单、详情、阶段变更触发器、消息提醒、销售漏斗报表 |
| 合同 | 列表、表单、详情、合同明细、合同审批、金额汇总触发器、导出 |
| 回款 | 列表、表单、详情、逾期提醒、回款报表 |
| 跟进记录 | 列表、表单、详情、客户或商机关联列表 |

### 12.4 应用入口范围

CRM 一期建议内置以下应用入口定义：

| 应用入口 | 类型 | 关联对象 | 说明 |
|----------|------|----------|------|
| 客户管理 | BUSINESS | 客户 | 打开客户标准后台运行页 |
| 联系人管理 | BUSINESS | 联系人 | 打开联系人标准后台运行页 |
| 线索管理 | BUSINESS | 线索 | 打开线索标准后台运行页 |
| 商机管理 | BUSINESS | 商机 | 打开商机标准后台运行页 |
| 合同管理 | BUSINESS | 合同 | 打开合同标准后台运行页 |
| 回款管理 | BUSINESS | 回款 | 打开回款标准后台运行页 |
| 跟进记录 | BUSINESS | 跟进记录 | 打开跟进记录标准后台运行页 |
| 销售任务 | BUSINESS | 销售任务 | 打开销售任务标准后台运行页 |

### 12.5 后续扩展

CRM 后续扩展应用：

| 应用 | 类型 | 说明 |
|------|------|------|
| 销售看板 | EMBEDDED | 嵌入报表或大屏 |
| 经营分析大屏 | EMBEDDED | 嵌入 BI 或报表引擎页面 |
| 移动客户拜访 | MOBILE | H5 客户拜访和跟进 |
| 企微客户通知 | INTEGRATION | 推送客户提醒到企微 |
| 飞书销售待办 | INTEGRATION | 推送待办到飞书 |
| 钉钉审批提醒 | INTEGRATION | 推送审批消息到钉钉 |

## 13. 迁移策略

### 13.1 第一阶段迁移

- 新增应用中心，不删除旧低代码入口。
- 新增业务套件、业务对象、对象关系、应用入口、能力挂接相关表和应用中心菜单。
- 从现有 `ai_lowcode_domain` 生成业务套件记录。
- 从现有 `ai_lowcode_model` 生成业务对象记录。
- 从现有已发布低代码应用生成应用入口记录，并关联业务对象和 `configKey`。
- 标准业务应用入口通过 `configKey` 继续打开现有运行页。
- CRM 样板先以配置方式初始化业务对象、对象关系、对象能力和应用入口。

### 13.2 第二阶段迁移

- 将旧低代码应用管理移动到开发者工具。
- 将 AI 生成应用能力迁移到应用中心。
- 将对象能力挂接、应用入口挂接和引擎中心打通。
- 将导入、导出、报表模板从系统配置入口逐步补充对象级入口。
- 将轻量触发器沉淀为对象自动化能力。
- 开始沉淀 H5 和嵌入应用标准入口。

### 13.3 第三阶段迁移

- 根据业务稳定度决定是否抽出独立 `forge-plugin-app`。
- 根据产品商业化需要决定是否拆独立前端包或独立工程。
- 将第三方平台连接器标准化为可配置应用。

## 14. 影响范围

- `forge-admin-ui`：新增应用中心、业务套件详情、业务对象详情、引擎中心、移动端中心、集成中心页面；调整菜单入口。
- `forge-plugin-generator`：新增业务套件、业务对象、对象关系、应用入口和能力挂接接口。
- `forge/db/migration`：新增业务套件表、业务对象表、对象关系表、应用入口表、能力挂接表、字典、菜单和权限资源。
- `sys_resource`：新增应用中心菜单和按钮权限，隐藏或迁移旧低代码入口。
- `ai_crud_config`：继续作为标准业务应用运行态配置来源。
- `ai_lowcode_model`：继续作为底层模型资产来源，产品侧表达为业务对象/实体。

## 15. 风险与关注点

- 产品概念风险：如果应用中心仍暴露过多技术字段，会失去业务化重构价值。
- 概念分层风险：业务套件、业务对象、应用入口和能力挂接边界不清，会重新退化成应用卡片管理。
- 迁移风险：旧低代码入口不能直接删除，避免影响已发布应用和开发者链路。
- 权限风险：嵌入应用、外部链接、H5 入口必须统一走权限控制。
- 数据边界风险：业务对象与 `ai_lowcode_model`、应用入口与 `ai_crud_config` 的关系要保持清晰，避免双主数据源冲突。
- 自动化风险：触发器如果第一阶段做得过深，会与 Flowable 流程、消息中心和定时任务边界重叠。
- 范围风险：第一阶段不要同时重构流程、报表、移动端和第三方推送底层，否则周期过大。

## 16. 测试策略

- 应用中心页面可正常分页、筛选、查看业务套件、业务对象和应用入口。
- 业务对象详情可查看对象基础信息、对象关系和对象能力挂接。
- 对象关系可保存引用关系和明细关系，并在详情中正确回显。
- 标准业务应用入口通过 `configKey` 打开现有运行页，历史动态 CRUD 不受影响。
- 嵌入应用按 `entryMode` 正确打开内部路由、iframe 或外部地址。
- 业务对象和应用入口启停后，前端入口和打开接口状态一致。
- 普通业务用户看不到开发者 JSON 配置入口。
- 开发者仍可从开发者工具进入旧低代码应用管理和模型设计。
- 菜单 Flyway 脚本可重复执行，不产生重复资源。
- 后端编译通过，前端新增页面 ESLint 通过。

## 17. 验收标准

- 业务人员进入系统后，默认从“应用中心”看到业务套件和业务对象，而不是低代码配置。
- CRM 样板至少展示 8 个核心业务对象，并能按业务对象方式查看关系和能力。
- CRM 样板至少展示客户-联系人、客户-商机、商机-合同、合同-合同明细、合同-回款等对象关系。
- CRM 样板至少展示 8 个应用入口，并能按业务应用方式打开或配置。
- 应用支持 BUSINESS、EMBEDDED、MOBILE、INTEGRATION 四类类型。
- 标准业务应用继续兼容现有动态 CRUD 运行时。
- 旧低代码搭建器和模型设计未被破坏，可通过开发者入口访问。
- 新增菜单、字典、业务套件、业务对象、对象关系、应用入口和能力挂接初始化数据均通过 Flyway 脚本维护。
- 不引入新工程，不重复建设认证、权限、租户、消息、文件等基础能力。

## 18. 非目标

- 本阶段不重写动态 CRUD 运行时。
- 本阶段不删除 `ai_crud_config`。
- 本阶段不强制拆出独立工程或独立服务。
- 本阶段不完成完整移动端 App 或 H5 容器。
- 本阶段不实现企微、飞书、钉钉完整推送通道，只定义集成应用入口和扩展边界。
- 本阶段不重构 Flowable 流程引擎底层。
- 本阶段不实现完整复杂规则引擎，触发器先定义对象自动化入口和挂接协议。
- 本阶段不把所有 CRM 业务逻辑一次性做成定制代码。

## 19. 待澄清

- CRM 样板是否需要初始化真实业务表，还是先以业务对象、对象关系、应用入口和示例配置形式展示。
- “应用中心”作为一级菜单还是挂在现有 AI/低代码目录下。
- 开发者工具的目标可见角色和权限范围。
- 第一阶段是否新增 `ai_business_object`，还是先复用 `ai_lowcode_model` 并通过 VO 层业务化表达。
- 对象关系第一阶段是否只做配置保存，还是要同步生成运行态主子表/关联列表。
- 触发器第一阶段是否只保存配置，还是要支持少量内置动作，例如字段汇总和消息提醒。
- 嵌入应用是否允许跨域 iframe，是否需要白名单。
- 移动端中心是否已有独立 H5 工程规划。
- 第三方平台集成第一期优先企微、飞书还是钉钉。

## 20. 技术决策

- 继续基于现有 Forge 工程演进，不新建独立工程。
- 新业务化模块先逻辑隔离，后续产品形态稳定后再评估物理拆分。
- 标准业务应用继续复用 `ai_crud_config` 和动态 CRUD 运行态。
- 业务对象/实体是核心建模语言，位于业务套件和应用入口之间。
- `ai_business_app` 定位为应用入口，不再承担业务对象/实体主表职责。
- 新增业务主数据建议采用 `ai_business_suite`、`ai_business_object`、`ai_business_app`、`ai_business_binding` 分层，避免所有业务化属性继续堆入 `options`。
- 第一阶段优先交付业务套件、业务对象、对象关系、应用入口和能力挂接协议，不深改底层引擎。
- 触发器作为轻量对象自动化能力沉淀，复杂长流程仍由 Flowable 承担。
- 旧低代码入口保留兼容，业务用户默认使用新应用中心。
