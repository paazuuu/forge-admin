# 任务清单：lowcode-business-app-platform
> status: propose
> created: 2026-05-27
> 拆分顺序：决策基线 → 数据模型 → 后端协议 → 兼容迁移 → 前端入口 → CRM 样板 → 验证归档
> 原则：每个任务应可独立提交；查询 SQL 写 Mapper XML；分页参数使用 `pageNum/pageSize`；内置数据 `tenant_id=1`；Flyway 脚本必须防重复。

## 前置条件

- [x] 已完成 `spec.md` 业务对象/实体模型补充。
- [x] 已确认第一阶段不重写动态 CRUD 运行时。
- [x] 已确认标准业务应用入口继续复用 `/ai/crud-page/{configKey}`。
- [ ] 确认第一阶段采用新增业务表方案：`ai_business_suite`、`ai_business_object`、`ai_business_object_relation`、`ai_business_app`、`ai_business_binding`。
- [ ] 确认 CRM 样板第一阶段以配置初始化为主，不强制一次性创建所有真实 CRM 业务表。

## 阶段总览

| 阶段 | 目标 | 包含任务 | 交付结果 |
|------|------|----------|----------|
| Phase 0 | 决策基线 | Task 0 | 冻结第一阶段边界和命名 |
| Phase 1 | 数据模型 | Task 1-3 | 表结构、字典、菜单、CRM 初始化数据 |
| Phase 2 | 后端基础能力 | Task 4-10 | 业务套件、业务对象、对象关系、应用入口、能力挂接 API |
| Phase 3 | 兼容迁移 | Task 11-12 | 低代码旧数据映射到新业务化模型 |
| Phase 4 | 前端入口 | Task 13-18 | 应用中心、套件详情、对象详情、引擎/移动/集成入口 |
| Phase 5 | CRM 样板闭环 | Task 19 | CRM 对象、关系、能力、入口可视化闭环 |
| Phase 6 | 验证归档 | Task 20-21 | 编译、构建、验收和文档回填 |

## 任务总览

| Task | 阶段 | 名称 | 状态 | 优先级 |
|------|------|------|------|--------|
| Task 0 | Phase 0 | 决策基线与命名冻结 | pending | P0 |
| Task 1 | Phase 1 | 业务平台表结构迁移 | pending | P0 |
| Task 2 | Phase 1 | 字典、菜单、权限资源迁移 | pending | P0 |
| Task 3 | Phase 1 | CRM 样板初始化数据迁移 | pending | P0 |
| Task 4 | Phase 2 | 业务套件实体、Mapper、查询接口 | pending | P0 |
| Task 5 | Phase 2 | 业务对象实体、Mapper、查询接口 | pending | P0 |
| Task 6 | Phase 2 | 对象关系实体、Mapper、保存接口 | pending | P0 |
| Task 7 | Phase 2 | 应用入口实体、Mapper、打开信息接口 | pending | P0 |
| Task 8 | Phase 2 | 能力挂接实体、Mapper、批量保存接口 | pending | P0 |
| Task 9 | Phase 2 | 后端 DTO/VO 与接口协议统一 | pending | P0 |
| Task 10 | Phase 2 | 后端权限、状态、删除规则补齐 | pending | P1 |
| Task 11 | Phase 3 | 低代码领域和模型兼容映射 | pending | P0 |
| Task 12 | Phase 3 | 已发布低代码应用入口兼容映射 | pending | P0 |
| Task 13 | Phase 4 | 前端业务平台 API 客户端 | pending | P0 |
| Task 14 | Phase 4 | 应用中心首页 | pending | P0 |
| Task 15 | Phase 4 | 业务套件详情页 | pending | P0 |
| Task 16 | Phase 4 | 业务对象详情页和对象关系面板 | pending | P0 |
| Task 17 | Phase 4 | 应用入口编辑与打开行为 | pending | P1 |
| Task 18 | Phase 4 | 引擎中心、移动端中心、集成中心壳页面 | pending | P1 |
| Task 19 | Phase 5 | CRM 样板端到端验收 | pending | P0 |
| Task 20 | Phase 6 | 后端编译与前端构建验证 | pending | P0 |
| Task 21 | Phase 6 | 文档、变更记录和验收回填 | pending | P1 |

---

## Phase 0：决策基线

### Task 0: 决策基线与命名冻结

**目标**: 在编码前冻结第一阶段数据表、接口前缀、CRM 初始化策略和菜单层级，避免实现阶段反复返工。

**涉及文件**:
- `code-copilot/changes/lowcode-business-app-platform/spec.md` — 如决策变化，回填“待澄清”和“技术决策”。
- `code-copilot/changes/lowcode-business-app-platform/tasks.md` — 同步任务状态和拆分。

**决策输出**:
- 第一阶段采用新增业务表：`ai_business_suite`、`ai_business_object`、`ai_business_object_relation`、`ai_business_app`、`ai_business_binding`。
- 接口前缀固定为 `/ai/business/**`。
- `ai_business_app` 只表示应用入口，不表示业务对象。
- CRM 样板第一阶段初始化业务套件、对象、关系、能力挂接、应用入口；真实业务表按后续 CRUD 配置或业务模块补齐。
- 应用中心作为业务入口，旧低代码应用、模型设计、配置诊断移动到开发者工具或隐藏普通菜单。

**验收标准**:
- `spec.md` 和 `tasks.md` 对 `ai_business_app` 定位一致。
- 后续任务不再出现“业务应用主表承载业务对象”的表达。

---

## Phase 1：数据模型

### Task 1: 业务平台表结构迁移

**目标**: 新增业务套件、业务对象、对象关系、应用入口、能力挂接 5 张表。

**涉及文件**:
- `forge/db/migration/V1.0.26__add_business_app_platform_tables.sql` — 新增表、索引和字段注释。

**关键表**:
- `ai_business_suite`
- `ai_business_object`
- `ai_business_object_relation`
- `ai_business_app`
- `ai_business_binding`

**关键要求**:
- 所有业务表包含 `id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`。
- `tenant_id` 默认内置数据使用 `1`。
- 表使用 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`。
- 建议唯一索引：
  - `uk_ai_business_suite_code(tenant_id, suite_code)`
  - `uk_ai_business_object_code(tenant_id, suite_code, object_code)`
  - `uk_ai_business_app_code(tenant_id, app_code)`
  - `uk_ai_business_binding_target(tenant_id, target_type, target_code, binding_type, binding_key)`
- 对象关系至少包含 `source_object_code`、`target_object_code`、`relation_type`、`relation_name`、`relation_config`。

**验收标准**:
- Flyway 脚本可重复执行，建表使用 `CREATE TABLE IF NOT EXISTS`。
- 新增字段与 `spec.md` 第 7 章一致。

---

### Task 2: 字典、菜单、权限资源迁移

**目标**: 初始化业务平台所需字典、应用中心菜单和按钮权限资源。

**涉及文件**:
- `forge/db/migration/V1.0.27__add_business_app_platform_dicts_menus.sql` — 新增字典、菜单、按钮权限资源。

**字典类型**:
- `ai_business_suite`
- `ai_business_object_type`
- `ai_business_relation_type`
- `ai_business_app_type`
- `ai_business_app_entry_mode`
- `ai_business_binding_type`

**菜单与权限**:
- `/app-center`：应用中心
- `/app-center/suite/:suiteCode`：业务套件详情
- `/app-center/object/:objectCode`：业务对象详情
- `/app-center/engines`：引擎中心
- `/app-center/mobile`：移动端中心
- `/app-center/integration`：集成中心
- `ai:businessSuite:list`
- `ai:businessSuite:edit`
- `ai:businessObject:list`
- `ai:businessObject:add`
- `ai:businessObject:edit`
- `ai:businessObject:delete`
- `ai:businessObject:relation`
- `ai:businessBinding:config`
- `ai:businessApp:add`
- `ai:businessApp:edit`
- `ai:businessApp:delete`
- `ai:businessApp:status`
- `ai:businessApp:open`

**验收标准**:
- 所有 `INSERT` 显式写列名。
- 字典和资源插入使用 `INSERT ... SELECT ... WHERE NOT EXISTS`。
- 业务内置数据 `tenant_id=1`。

---

### Task 3: CRM 样板初始化数据迁移

**目标**: 初始化 CRM 业务套件、核心业务对象、对象关系、对象能力挂接和标准应用入口。

**涉及文件**:
- `forge/db/migration/V1.0.28__seed_crm_business_suite.sql` — 初始化 CRM 样板配置。

**初始化对象**:
- 客户：`CUSTOMER`
- 联系人：`CONTACT`
- 线索：`LEAD`
- 商机：`OPPORTUNITY`
- 合同：`CONTRACT`
- 合同明细：`CONTRACT_ITEM`
- 回款：`PAYMENT`
- 跟进记录：`FOLLOW_RECORD`
- 销售任务：`SALES_TASK`

**初始化关系**:
- `CUSTOMER` -> `CONTACT`：`CHILD_LIST`
- `CUSTOMER` -> `OPPORTUNITY`：`CHILD_LIST`
- `OPPORTUNITY` -> `CONTRACT`：`REFERENCE`
- `CONTRACT` -> `CONTRACT_ITEM`：`DETAIL`
- `CONTRACT` -> `PAYMENT`：`CHILD_LIST`
- `CUSTOMER` -> `FOLLOW_RECORD`：`CHILD_LIST`
- `OPPORTUNITY` -> `FOLLOW_RECORD`：`CHILD_LIST`

**初始化能力**:
- `IMPORT`、`EXPORT`：客户、合同、联系人。
- `REPORT`：客户报表、销售漏斗、回款报表。
- `TRIGGER`：合同金额汇总、商机阶段提醒、回款逾期提醒。
- `APPROVAL`：合同审批。
- `MESSAGE`：商机跟进提醒、回款逾期提醒。
- `PERMISSION`：客户、合同、回款对象权限。

**验收标准**:
- CRM 至少有 8 个业务对象和 8 个应用入口。
- 应用入口 `app_type=BUSINESS` 且关联 `object_code`。
- 初始化脚本可重复执行，不产生重复数据。

---

## Phase 2：后端基础能力

### Task 4: 业务套件实体、Mapper、查询接口

**目标**: 提供业务套件分页、列表、详情、汇总接口。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessSuite.java` — 新增实体。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessSuiteMapper.java` — 新增 Mapper。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessSuiteMapper.xml` — 新增分页、列表、汇总 SQL。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessSuiteService.java` — 新增服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessSuiteController.java` — 新增控制器。

**关键签名**:
```java
Page<BusinessSuiteVO> page(Integer pageNum, Integer pageSize, BusinessSuiteQueryDTO query);
List<BusinessSuiteVO> list(BusinessSuiteQueryDTO query);
BusinessSuiteVO detail(Long id);
List<BusinessSuiteSummaryVO> summary();
```

**验收标准**:
- 分页接口使用 `pageNum/pageSize`。
- 查询类 SQL 全部写在 `BusinessSuiteMapper.xml`。
- `/ai/business/suite/summary` 返回对象数量、应用入口数量、启用数量、最近更新时间。

---

### Task 5: 业务对象实体、Mapper、查询接口

**目标**: 提供业务对象分页、列表、详情、启停和运行信息查询。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessObject.java` — 新增实体。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessObjectMapper.java` — 新增 Mapper。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessObjectMapper.xml` — 新增分页、列表、详情 SQL。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectService.java` — 新增服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessObjectController.java` — 新增控制器。

**关键签名**:
```java
Page<BusinessObjectVO> page(Integer pageNum, Integer pageSize, BusinessObjectQueryDTO query);
List<BusinessObjectVO> list(BusinessObjectQueryDTO query);
BusinessObjectVO detail(Long id);
void updateStatus(Long id, Integer status);
BusinessObjectRuntimeInfoVO runtimeInfo(Long id);
```

**验收标准**:
- 业务对象可关联 `ai_lowcode_model.id` 和 `model_code`。
- `runtimeInfo` 返回关联应用入口、`configKey`、动态 CRUD 路由和权限结果。
- 状态停用后，前端不能作为可打开对象展示。

---

### Task 6: 对象关系实体、Mapper、保存接口

**目标**: 支持引用关系、明细关系、关联列表关系的查询和保存。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessObjectRelation.java` — 新增实体。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessObjectRelationMapper.java` — 新增 Mapper。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessObjectRelationMapper.xml` — 新增对象关系查询 SQL。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectRelationService.java` — 新增服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessObjectRelationController.java` — 新增控制器。

**关键签名**:
```java
List<BusinessObjectRelationVO> listByObject(Long objectId);
void saveRelations(Long objectId, List<BusinessObjectRelationDTO> relations);
void deleteRelation(Long objectId, Long relationId);
```

**验收标准**:
- 支持 `REFERENCE`、`DETAIL`、`CHILD_LIST`、`MANY_TO_MANY` 四类关系。
- 保存关系时校验来源对象和目标对象存在。
- 删除关系只删除当前对象范围内的关系，不能误删其他套件关系。

---

### Task 7: 应用入口实体、Mapper、打开信息接口

**目标**: 支持业务应用、嵌入应用、移动应用、集成应用四类入口，并提供打开信息解析。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessApp.java` — 新增实体。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessAppMapper.java` — 新增 Mapper。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessAppMapper.xml` — 新增分页、列表、详情 SQL。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessAppService.java` — 新增服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessAppOpenService.java` — 新增打开方式解析服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessAppController.java` — 新增控制器。

**关键签名**:
```java
Page<BusinessAppVO> page(Integer pageNum, Integer pageSize, BusinessAppQueryDTO query);
BusinessAppVO detail(Long id);
void updateStatus(Long id, Integer status);
BusinessAppOpenInfoVO openInfo(Long id);
```

**验收标准**:
- `BUSINESS + RUNTIME` 类型入口解析为 `/ai/crud-page/{configKey}`。
- `ROUTE`、`IFRAME`、`EXTERNAL`、`H5`、`API` 按 `entry_mode` 返回明确打开方式。
- 嵌入和外部入口返回前必须完成权限校验结果封装。

---

### Task 8: 能力挂接实体、Mapper、批量保存接口

**目标**: 支持业务套件、业务对象、应用入口三类目标的能力挂接。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessBinding.java` — 新增实体。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessBindingMapper.java` — 新增 Mapper。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessBindingMapper.xml` — 新增挂接能力查询 SQL。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessBindingService.java` — 新增服务。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessBindingController.java` — 新增控制器。

**关键签名**:
```java
List<BusinessBindingVO> list(BusinessBindingQueryDTO query);
Long create(BusinessBindingDTO dto);
void update(BusinessBindingDTO dto);
void delete(Long id);
void batchSave(BusinessBindingBatchSaveDTO dto);
```

**验收标准**:
- `target_type` 支持 `SUITE`、`OBJECT`、`APP`。
- `binding_type` 支持 `FLOW`、`APPROVAL`、`REPORT`、`PERMISSION`、`MESSAGE`、`TRIGGER`、`IMPORT`、`EXPORT`、`MOBILE`、`INTEGRATION`。
- 批量保存按 `target_type + target_code + binding_type` 覆盖目标范围内旧配置，不影响其他目标。

---

### Task 9: 后端 DTO/VO 与接口协议统一

**目标**: 建立业务平台 DTO/VO，避免 Controller 直接暴露实体和 JSON 字符串细节。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessSuiteQueryDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessSuiteDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessObjectQueryDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessObjectDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessObjectRelationDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessAppQueryDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessAppDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessBindingQueryDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessBindingDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessBindingBatchSaveDTO.java`
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/*.java`

**关键 VO**:
- `BusinessSuiteVO`
- `BusinessSuiteSummaryVO`
- `BusinessObjectVO`
- `BusinessObjectRuntimeInfoVO`
- `BusinessObjectRelationVO`
- `BusinessAppVO`
- `BusinessAppOpenInfoVO`
- `BusinessBindingVO`

**验收标准**:
- 前端不需要解析实体原始 JSON 字符串即可展示对象能力和入口信息。
- DTO 字段使用业务语言：`suiteCode`、`objectCode`、`relationType`、`appType`、`entryMode`、`bindingType`。
- Controller 返回统一 `RespInfo.success(data)`。

---

### Task 10: 后端权限、状态、删除规则补齐

**目标**: 补齐状态控制、删除保护、权限注解和安全边界。

**涉及文件**:
- `BusinessSuiteController.java` — 增加权限注解。
- `BusinessObjectController.java` — 增加权限注解和删除保护。
- `BusinessObjectRelationController.java` — 增加权限注解。
- `BusinessAppController.java` — 增加权限注解和打开权限校验。
- `BusinessBindingController.java` — 增加权限注解。
- `BusinessAppOpenService.java` — 嵌入、外部、H5 入口打开安全校验。

**关键权限**:
```java
@SaCheckPermission("ai:businessObject:list")
@SaCheckPermission("ai:businessObject:edit")
@SaCheckPermission("ai:businessObject:relation")
@SaCheckPermission("ai:businessBinding:config")
@SaCheckPermission("ai:businessApp:open")
```

**验收标准**:
- 已有关联对象关系的业务对象不能直接硬删除。
- 已有关联业务对象的套件不能直接硬删除。
- 停用应用入口不能返回可打开状态。
- 外部地址、iframe、H5 入口不绕过平台权限校验。

---

## Phase 3：兼容迁移

### Task 11: 低代码领域和模型兼容映射

**目标**: 将现有 `ai_lowcode_domain` 和 `ai_lowcode_model` 业务化表达为套件和对象。

**涉及文件**:
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessBootstrapService.java` — 新增兼容映射服务。
- `BusinessSuiteMapper.xml` — 新增按领域映射查询。
- `BusinessObjectMapper.xml` — 新增按模型映射查询。
- `BusinessSuiteService.java` — 增加领域映射入口。
- `BusinessObjectService.java` — 增加模型映射入口。

**关键签名**:
```java
void syncSuitesFromLowcodeDomains();
void syncObjectsFromLowcodeModels();
```

**验收标准**:
- `ai_lowcode_domain.domain_code` 映射为 `ai_business_suite.suite_code`。
- `ai_lowcode_model.model_code` 映射为 `ai_business_object.object_code`。
- 同步逻辑按编码幂等执行，不覆盖用户后续修改的业务名称和说明。

---

### Task 12: 已发布低代码应用入口兼容映射

**目标**: 将已发布低代码应用映射为 `ai_business_app` 应用入口，并继续通过 `configKey` 打开旧运行页。

**涉及文件**:
- `BusinessBootstrapService.java` — 增加低代码应用入口同步。
- `BusinessAppMapper.xml` — 新增按 `config_key` 查询和 upsert 所需查询。
- `BusinessAppOpenService.java` — 保持 `configKey` 到 `/ai/crud-page/{configKey}` 的解析。

**关键签名**:
```java
void syncAppsFromPublishedCrudConfigs();
BusinessAppOpenInfoVO buildRuntimeOpenInfo(AiBusinessApp app);
```

**验收标准**:
- `ai_crud_config.publish_status='PUBLISHED'` 的配置可生成 `BUSINESS + RUNTIME` 应用入口。
- 旧 `/ai/lowcode/app/**`、`/ai/crud-page/{configKey}`、`/ai/crud/{configKey}/**` 不受影响。
- 业务应用入口不复制动态 CRUD 运行配置，只保存 `config_key` 引用。

---

## Phase 4：前端入口

### Task 13: 前端业务平台 API 客户端

**目标**: 封装业务平台前端 API，供应用中心和详情页复用。

**涉及文件**:
- `forge-admin-ui/src/api/business-app.js` — 新增业务平台 API 方法。

**关键函数**:
```js
export function businessSuitePage(params)
export function businessSuiteList(params)
export function businessSuiteSummary(params)
export function businessObjectPage(params)
export function businessObjectList(params)
export function businessObjectDetail(id)
export function businessObjectRelations(objectId)
export function saveBusinessObjectRelations(objectId, data)
export function businessAppPage(params)
export function businessAppOpenInfo(id)
export function businessBindingList(params)
export function saveBusinessBindings(data)
```

**验收标准**:
- 所有接口路径与 `spec.md` 第 8 章一致。
- URL 占位符在 AiCrudPage 场景继续使用 `:id` 规则；普通 API 函数使用模板字符串路径。

---

### Task 14: 应用中心首页

**目标**: 新增 `/app-center` 首页，按业务套件和应用类型展示业务入口。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/index.vue` — 新增应用中心首页。
- `forge-admin-ui/src/views/app-center/components/AppFilterBar.vue` — 新增筛选条。
- `forge-admin-ui/src/views/app-center/components/ObjectCard.vue` — 新增业务对象卡片。
- `forge-admin-ui/src/views/app-center/components/AppCard.vue` — 新增应用入口卡片。

**关键交互**:
- 按业务套件筛选。
- 按应用类型筛选：`BUSINESS`、`EMBEDDED`、`MOBILE`、`INTEGRATION`。
- 点击业务套件进入 `/app-center/suite/:suiteCode`。
- 点击业务对象进入 `/app-center/object/:objectCode`。
- 点击应用入口调用 `businessAppOpenInfo(id)`。

**验收标准**:
- 首页默认展示业务套件和业务对象，不展示 JSON、Schema、表名。
- 页面文案使用“业务对象”“对象关系”“接入能力”“应用入口”。
- 页面保持后台业务系统风格，不做营销式落地页。

---

### Task 15: 业务套件详情页

**目标**: 展示 CRM 等套件下的业务对象、业务流程、业务看板、移动入口和集成入口。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/suite-detail.vue` — 新增套件详情页。
- `ObjectCard.vue` — 展示套件对象。
- `AppCard.vue` — 展示看板、移动、集成入口。

**关键交互**:
- 展示套件基础信息、对象数量、应用入口数量、最近更新。
- 业务对象区展示客户、联系人、线索、商机、合同、回款等对象。
- 场景入口区展示销售看板、移动拜访、第三方推送等入口。

**验收标准**:
- CRM 套件详情中业务对象是第一视觉。
- 应用入口作为对象之外的场景入口展示。
- 停用对象和停用应用入口有明确状态标签。

---

### Task 16: 业务对象详情页和对象关系面板

**目标**: 为业务对象提供关系、布局、导入导出、报表、触发器、审批、消息、权限等能力页签。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/object-detail.vue` — 新增业务对象详情页。
- `forge-admin-ui/src/views/app-center/components/ObjectRelationPanel.vue` — 新增对象关系面板。
- `forge-admin-ui/src/views/app-center/components/BusinessBindingPanel.vue` — 新增能力挂接面板。

**关键交互**:
- “关系”页签展示 `REFERENCE`、`DETAIL`、`CHILD_LIST`、`MANY_TO_MANY`。
- “能力”页签展示 `FLOW`、`APPROVAL`、`REPORT`、`PERMISSION`、`MESSAGE`、`TRIGGER`、`IMPORT`、`EXPORT`。
- 对象关系配置使用“关联客户”“合同明细”“下级联系人”等业务文案。

**验收标准**:
- 客户对象可看到联系人、商机、跟进记录关系。
- 合同对象可看到合同明细和回款关系。
- 技术字段 `modelSchema`、`tableName`、`configKey` 仅在开发者信息区显示。

---

### Task 17: 应用入口编辑与打开行为

**目标**: 支持新增、编辑、启停和打开应用入口。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/components/AppEditorDrawer.vue` — 新增应用入口编辑抽屉。
- `forge-admin-ui/src/views/app-center/index.vue` — 接入编辑和打开行为。
- `forge-admin-ui/src/views/app-center/suite-detail.vue` — 接入入口编辑和打开行为。

**关键交互**:
- `BUSINESS + RUNTIME` 打开 `/ai/crud-page/{configKey}`。
- `ROUTE` 打开内部路由。
- `IFRAME` 打开平台内嵌页面。
- `EXTERNAL` 新窗口打开。
- `H5` 展示移动入口地址或二维码预留区。
- `API` 进入集成配置详情。

**验收标准**:
- 停用入口不能打开。
- 打开前调用后端 `open-info`，不由前端自行拼接外部敏感入口。
- `configKey` 不作为卡片主视觉字段。

---

### Task 18: 引擎中心、移动端中心、集成中心壳页面

**目标**: 完成三类中心的第一阶段统一入口，不深改底层引擎。

**涉及文件**:
- `forge-admin-ui/src/views/app-center/engine-center.vue` — 新增引擎中心。
- `forge-admin-ui/src/views/app-center/mobile-center.vue` — 新增移动端中心。
- `forge-admin-ui/src/views/app-center/integration-center.vue` — 新增集成中心。

**展示内容**:
- 引擎中心：流程、审批、报表、权限、消息、触发器、导入、导出能力卡片。
- 移动端中心：H5、移动待办、移动审批、移动业务入口。
- 集成中心：开放接口、Webhook、企微、飞书、钉钉。

**验收标准**:
- 每个中心能从应用中心菜单访问。
- 第一阶段只展示入口和挂接概览，不承诺完整推送通道或移动容器。

---

## Phase 5：CRM 样板闭环

### Task 19: CRM 样板端到端验收

**目标**: 验证 CRM 样板从业务套件、业务对象、对象关系、对象能力到应用入口的完整表达。

**涉及文件**:
- `forge/db/migration/V1.0.28__seed_crm_business_suite.sql` — 校验初始化数据。
- `forge-admin-ui/src/views/app-center/index.vue` — 校验 CRM 套件展示。
- `forge-admin-ui/src/views/app-center/suite-detail.vue` — 校验 CRM 对象展示。
- `forge-admin-ui/src/views/app-center/object-detail.vue` — 校验对象关系和能力。

**验收路径**:
1. 进入 `/app-center`。
2. 打开 CRM 套件。
3. 查看客户、联系人、线索、商机、合同、合同明细、回款、跟进记录、销售任务。
4. 打开客户对象，确认客户-联系人、客户-商机、客户-跟进记录关系。
5. 打开合同对象，确认合同-合同明细、合同-回款关系。
6. 打开客户管理应用入口，确认能进入现有动态 CRUD 运行页或得到明确未配置提示。

**验收标准**:
- CRM 至少展示 8 个核心业务对象。
- CRM 至少展示 5 个对象关系。
- CRM 至少展示 8 个应用入口。
- 标准业务入口继续兼容现有动态 CRUD。

---

## Phase 6：验证归档

### Task 20: 后端编译与前端构建验证

**目标**: 完成针对性后端编译和前端构建，确认任务实现不破坏现有链路。

**验证命令**:
```bash
mvn -pl forge-admin-server -am compile -DskipTests
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**验收标准**:
- 后端编译通过。
- 前端构建通过。
- 动态 CRUD 旧路由 `/ai/crud-page/{configKey}` 可访问。
- `/ai/lowcode/app/**`、`/ai/lowcode/model/**` 兼容接口未删除。

---

### Task 21: 文档、变更记录和验收回填

**目标**: 将执行结果、验证命令和剩余风险回填到变更文档。

**涉及文件**:
- `code-copilot/changes/lowcode-business-app-platform/spec.md` — 如实现边界变化，回填技术决策。
- `code-copilot/changes/lowcode-business-app-platform/tasks.md` — 更新任务状态和验证结果。
- `.opencode/memory/decisions.md` — 如形成长期产品/架构决策，追加记录。
- `.opencode/memory/pitfalls.md` — 如遇到可复用踩坑，追加记录。

**验收标准**:
- 已完成任务标记为 `completed`。
- 未完成任务保留明确原因和后续处理方式。
- 编译、构建、手工验收结果有记录。
