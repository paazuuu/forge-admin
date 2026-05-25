# 动态低代码页面数据权限适配
> status: apply
> created: 2026-05-24
> complexity: 🔴复杂

## 1. 背景与目标
动态生成页面当前已经支持单表、树形单表、左树右表、自定义查询、导入导出等能力，但运行时数据查询主要走 `NamedParameterJdbcTemplate`，不会触发系统现有基于 MyBatis Mapper 的 `DataScopeInterceptor`。这会导致低代码页面即使有租户隔离，也不能自动跟随当前系统角色的数据范围。

目标：
- 动态页面的数据权限与系统角色数据范围保持一致，不新增一套独立角色权限体系。
- 页面设计阶段只配置“权限字段映射”，例如归属组织字段、创建人字段、区划字段；实际可见范围由当前登录人的角色数据权限决定。
- 动态 CRUD 的列表、树形列表、自定义查询、导出、详情、编辑、删除、批量删除都必须由后端强制追加数据权限条件。
- 查询条件中的组织树、左树右表选择节点等业务筛选条件，与系统数据权限取交集，不能扩大用户可见范围。
- 默认兼容历史低代码页面：未启用系统数据权限时继续只做租户隔离和逻辑删除过滤。

## 2. 代码现状（Research Findings）
### 2.1 系统数据权限链路
- `forge/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/handler/DataScopeInterceptor.java`：当前数据权限核心拦截器，只在 MyBatis `beforeQuery` 中根据 `mapperId` 查 `sys_data_scope_config` 并改写 SQL。
- `forge/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/service/impl/DataScopeServiceImpl.java`：通过当前登录用户、角色、组织、自定义组织、行政区划构建 `DataScopeContext`。
- `forge/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/enums/DataScopeType.java`：定义 `ALL/SELF/ORG/ORG_AND_CHILD/CUSTOM/TENANT_ALL/REGION` 等后端可执行数据范围。
- `forge/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/entity/SysDataScopeConfig.java`：Mapper 级权限字段配置，包含用户字段、组织字段、租户字段、区划字段和表别名。
- `forge-admin-ui/src/views/system/dataScopeConfig.vue`：系统侧数据权限配置管理页面，面向 Mapper 方法，不适合直接配置每个动态 CRUD 页面。

### 2.2 动态 CRUD 运行时链路
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/DynamicCrudController.java`：动态页面统一入口 `/ai/crud/{configKey}`，包含分页、树、详情、新增、修改、删除、导入、导出。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java`：动态 CRUD 业务编排，负责读取配置、字段白名单、查询条件扩展、读链路解密/翻译/脱敏。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudRepository.java`：动态 SQL 仓储，使用 `NamedParameterJdbcTemplate`，当前 `appendBaseQueryConditions` 只追加租户和逻辑删除条件。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudExcelService.java`：导出通过 `DynamicCrudService#selectExportRows` 获取数据，后续只要查询链路统一加权限，导出可复用。
- `forge-admin-ui/src/views/ai/crud-page.vue`：动态渲染页，根据 `configKey` 拉取运行时配置并渲染 `AiCrudPage` 或页面模板。

### 2.3 低代码模型协议现状
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodeModelSchema.java`：模型协议已有 `policies` 字段，可继续承载数据权限策略。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/lowcode/LowcodePolicySchema.java`：当前只有 `dataScope/regionField/tenantField/logicDeleteField` 等基础字段，缺少用户字段、组织字段、自定义组织字段映射。
- `forge-admin-ui/src/components/lowcode-builder/model/LowcodeModelDesigner.vue`：模型设计器已有“数据范围”配置，但当前选项只有 `TENANT/REGION/DEPT`，不能表达“跟随系统角色数据权限”。
- `forge-admin-ui/src/components/lowcode-builder/model/model-schema.js`：默认模型已内置 `tenantId/createBy/createDept` 等系统字段，适合用作数据权限默认字段映射。

### 2.4 发现与风险
- 动态 CRUD 不走 MyBatis Mapper，单纯在 `sys_data_scope_config` 里配置 `mapper_method` 不会对 `/ai/crud/{configKey}` 生效。
- 动态页面现有租户过滤在 `DynamicCrudRepository` 内部兜底，但没有组织、本人、行政区划范围过滤。
- 数据权限必须覆盖写操作。只过滤列表会导致用户通过详情、编辑、删除接口访问或修改越权数据。
- 当前角色数据范围字典和后端 `DataScopeType` 存在口径不一致迹象，动态适配层不能直接散落使用数字常量，必须先归一化为可执行范围。
- 左树右表、组织树查询、自定义查询、树形单表都可能引入额外业务筛选条件，这些条件只能缩小结果集，不能替代系统权限条件。

## 3. 功能点
- [x] 新增动态数据权限适配服务，复用 `IDataScopeService#getCurrentUserDataScope()` 获取当前登录人的系统数据权限上下文。
- [x] 扩展 `LowcodePolicySchema`，支持 `SYSTEM_DATA_SCOPE` 或 `FOLLOW_SYSTEM` 策略，并配置 `userField/orgField/regionField/tenantField` 等字段映射。
- [x] 模型设计器新增“跟随系统数据权限”选项，并提供权限字段映射设置；默认值从系统字段自动推导：`createBy/create_by`、`orgId/org_id`、`createDept/create_dept`、`tenantId/tenant_id`。
- [x] 动态分页、关联分页、自定义查询、树形查询、详情查询、导出查询统一追加数据权限条件。
- [x] 动态编辑、删除、批量删除统一追加数据权限条件；无权限时返回业务异常或影响行数为 0，不能静默越权成功。
- [x] 组织树查询条件和左树右表节点筛选支持“选父级查询子级”，并与数据权限条件取交集。
- [x] 树形单表支持补齐导航祖先节点，但补齐节点只能用于展示；编辑、删除、添加下级仍以节点本身是否满足数据权限为准。
- [x] 自定义查询组件保存/加载的查询条件继续按字段白名单执行，并自动叠加同一套动态数据权限。
- [x] Excel 导出复用动态查询权限结果，关联字段翻译、组织名称翻译、脱敏顺序保持现有读链路规则。

## 4. 业务规则
- 动态页面默认策略为 `TENANT`：只做租户隔离和逻辑删除过滤，兼容历史配置。
- 选择 `FOLLOW_SYSTEM` 后，数据范围由当前用户角色决定，页面配置只负责字段映射。
- 超级管理员 `ALL` 不追加组织/本人/区划限制，但仍保留租户拦截体系已有规则。
- 租户管理员或本租户数据范围只限制 `tenantField`。
- 本人数据范围使用 `userField`，默认 `create_by`。
- 本组织、本组织及子组织、自定义组织范围使用 `orgField`，优先业务归属组织字段 `org_id`；未配置时降级为 `create_dept`。
- 行政区划范围使用 `regionField`，规则与现有系统保持一致：省级视为全部，市级及以下匹配本级及下级区划。
- 业务查询条件和系统权限条件必须同时生效。例如用户选择组织 A 查询，当前角色只能看组织 B，则结果为 A 与 B 的交集。
- 前端只负责配置和展示，后端必须强制追加权限条件，不能信任前端传参。
- 未配置必要权限字段时，`FOLLOW_SYSTEM` 发布或运行时应明确报错，不能自动放大为全部数据。

## 5. 数据变更
| 操作 | 表名/协议 | 字段/索引 | 说明 |
|------|-----------|-----------|------|
| 修改 | `model_schema.policies` | `dataScope` | 增加 `FOLLOW_SYSTEM` 或 `SYSTEM_DATA_SCOPE`，表示跟随系统角色数据权限 |
| 修改 | `model_schema.policies` | `userField/userColumn` | 本人数据范围字段映射，默认 `createBy/create_by` |
| 修改 | `model_schema.policies` | `orgField/orgColumn` | 组织数据范围字段映射，默认优先 `orgId/org_id`，否则 `createDept/create_dept` |
| 修改 | `model_schema.policies` | `regionField/regionColumn` | 行政区划数据范围字段映射 |
| 修改 | `model_schema.policies` | `tenantField/tenantColumn` | 租户字段映射，默认 `tenantId/tenant_id` |
| 不新增 | 业务表 | - | 第一阶段不新增系统表；权限策略随低代码模型协议保存 |

## 6. 接口变更
| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 修改 | `/ai/lowcode/app/draft` | POST | 保存草稿时接收扩展后的 `modelSchema.policies` |
| 修改 | `/ai/lowcode/app/{id}/publish` | POST | 发布前校验 `FOLLOW_SYSTEM` 所需字段映射是否存在于模型字段或真实表列 |
| 修改 | `/ai/crud/{configKey}/page` | GET | 查询时自动追加动态数据权限条件 |
| 修改 | `/ai/crud/{configKey}/tree` | GET | 树形数据查询自动追加动态数据权限条件 |
| 修改 | `/ai/crud/{configKey}/{id}` | GET | 详情查询自动追加动态数据权限条件 |
| 修改 | `/ai/crud/{configKey}` | PUT | 更新时自动追加动态数据权限条件 |
| 修改 | `/ai/crud/{configKey}/{id}` | DELETE | 删除时自动追加动态数据权限条件 |
| 修改 | `/ai/crud/{configKey}/export` | POST | 导出时复用带权限的动态查询 |
| 修改 | `/ai/custom-query/{configKey}/execute` | POST | 自定义查询执行自动追加动态数据权限条件 |

## 7. 影响范围
- `forge-plugin-generator`：低代码模型协议、运行时配置构建、动态 CRUD Service/Repository、动态导出、自定义查询。
- `forge-starter-datascope`：复用 `IDataScopeService` 和 `DataScopeContext`；如角色数据范围编码不一致，需要增加兼容归一化方法或修正枚举/字典口径。
- `forge-admin-ui`：低代码模型设计器、默认模型协议、动态页面渲染说明、组织树查询条件。
- 动态 CRUD 所有已发布页面：默认 `TENANT` 保持兼容；启用 `FOLLOW_SYSTEM` 的页面开始按角色数据权限收敛数据。

## 8. 风险与关注点
> ⚠️ 涉及权限变更，必须后端强制执行并重点回归。

- 写接口必须带权限条件，否则存在越权编辑/删除风险。
- 角色数据范围编码不统一会导致权限扩大或缩小，必须在实现前明确统一口径。
- 组织字段映射选错会导致业务数据不可见或越权可见，发布前必须校验字段存在并提示建议字段。
- 树形数据补齐祖先节点容易被误认为有权限操作，前端需要区分“导航补齐节点”和“有权限节点”。
- 多模型 Join 查询只应按主业务表数据权限过滤；子表 Join 条件不能扩大主表可见范围。
- 行政区划下级规则要与当前系统保持一致，避免低代码页面和普通 Mapper 页面结果不一致。

## 8.5 测试策略
- **测试范围**：动态数据权限 SQL 条件生成、单表分页、Join 分页、自定义查询、树形查询、详情、编辑、删除、导出、低代码发布校验、模型设计器字段映射。
- **覆盖率目标**：权限类型 `ALL/TENANT_ALL/SELF/ORG/ORG_AND_CHILD/CUSTOM/REGION` 至少覆盖主要 SQL 生成分支；写操作必须覆盖无权限场景。
- **独立 Test Spec**：是，见 `code-copilot/changes/dynamic-lowcode-data-scope/test-spec.md`。
- **验证命令**：
  - 后端：`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -pl forge-admin-server -am compile -DskipTests`
  - 前端：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build`
  - 前端定向 lint：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/components/ai-form/AiCrudPage.vue src/components/lowcode-builder/model/model-schema.js src/components/lowcode-builder/model/LowcodeModelDesigner.vue src/views/ai/lowcode-builder.vue src/views/ai/lowcode-models.vue`
  - 接口：使用普通角色分别验证本人、本组织、本组织及子组织、行政区划、租户管理员、超级管理员的数据可见范围。
- **验证结果**：
  - 后端编译通过。
  - 前端构建通过，存在既有 UnoCSS missing-icon、CSS `//` 注释、动态导入提示和 chunk-size warning。
  - 前端定向 lint 通过。
  - 角色矩阵接口验收仍需连接真实账号和业务数据后人工回归。

## 9. 待澄清
- [x] 角色数据范围编码最终以当前角色字典为主：`1全部/2本租户/3本组织/4本组织及子组织/5个人/7行政区划`；同时在 `DataScopeType#getByRoleDataScope` 兼容旧后端枚举 `5自定义/6本租户`，避免升级后历史角色被错误解释。
- [x] 自定义组织数据权限继续复用 `sys_role_data_scope`。当角色编码为 `5` 且存在自定义组织 ID 时按 `CUSTOM` 执行；否则按当前字典的“个人”执行。本次不新增独立低代码角色授权入口。
- [x] 树形单表补齐祖先节点使用 `_scopeAncestor=true` 和 `_dataScopeWritable=false` 标识；前端行操作拦截编辑、删除、添加下级，后端写接口仍以动态数据权限条件强制校验。

## 10. 技术决策
- 不为动态页面新增独立角色授权体系，动态页面只声明字段映射，角色范围复用系统现有角色数据权限。
- 不依赖 `DataScopeInterceptor` 处理动态 CRUD，因为动态 CRUD 使用 `NamedParameterJdbcTemplate`，不会触发 MyBatis Mapper 拦截。
- 新增动态数据权限适配层，输出参数化 SQL 条件和参数，不直接拼接用户输入。
- 动态权限条件在仓储层或 Service 统一入口追加，不能散落在控制器或前端。
- Join 查询按主表别名 `t0` 追加主业务表权限条件。
- 导出和自定义查询不单独实现权限规则，统一复用动态查询权限构建。
- 默认策略保留 `TENANT` 兼容历史页面，只有明确选择 `FOLLOW_SYSTEM` 的模型才启用系统角色数据权限。
- `FOLLOW_SYSTEM` 作为低代码协议标准值，历史或外部传入的 `SYSTEM_DATA_SCOPE` 在保存、发布和运行时归一化为 `FOLLOW_SYSTEM`。
- 发布校验不只看模型字段，还通过真实表列二次确认字段映射，防止绑定已有表时字段名和列名不一致导致运行时越权或报错。
- 多套角色数据范围编码集中在 `DataScopeType#getByRoleDataScope` 归一化，动态低代码和原 MyBatis 数据权限拦截器共用同一套解释逻辑。

## 11. 执行日志
| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Spec 确认 | done | `code-copilot/changes/dynamic-lowcode-data-scope/spec.md` | 角色数据范围编码按当前字典为主、旧枚举兼容处理 |
| Task 1: 策略协议与默认值归一化 | done | `LowcodePolicySchema.java`, `LowcodePolicyService.java`, `LowcodeDataModelService.java`, `LowcodeAppService.java`, `LowcodePublishService.java`, `LowcodeRuntimeConfigBuilder.java`, `model-schema.js`, `lowcode-builder.vue`, `lowcode-models.vue` | 统一 `policies` 默认值，`SYSTEM_DATA_SCOPE` 归一化为 `FOLLOW_SYSTEM` |
| Task 2: 发布校验 | done | `LowcodePublishService.java`, `LowcodeDdlService.java`, `LowcodePolicyService.java` | 发布时通过真实表列校验用户、组织、区划、租户字段映射 |
| Task 3: 角色数据范围编码兼容 | done | `DataScopeType.java`, `DataScopeInterceptor.java` | 当前角色字典与旧后端枚举统一归一化，避免历史角色升级后失真 |
| Task 4: 动态运行时数据权限 | done | `DynamicDataScopeService.java`, `DynamicCrudRepository.java`, `DynamicCrudService.java`, `forge-plugin-generator/pom.xml` | 分页、Join、自定义查询、树、详情、导出、编辑、删除统一追加参数化权限条件 |
| Task 5: 树形与组织筛选收敛 | done | `DynamicCrudService.java`, `DynamicCrudRepository.java`, `CustomQueryConditionDTO.java`, `LowcodeTreeConfig.java` | 支持选父级查子级并与系统权限取交集；补齐祖先节点但标记只读 |
| Task 6: 前端配置与运行态保护 | done | `LowcodeModelDesigner.vue`, `model-schema.js`, `AiCrudPage.vue`, `AiCustomQuery.vue`, `crud-page.vue` | 设计器支持跟随系统角色字段映射；运行态阻断补齐祖先节点的写操作 |
| Task 7: 验证 | done | `test-spec.md`、后端 Maven 编译、前端 build、前端定向 eslint | 补充权限矩阵和接口验收用例；编译/构建通过；前端 build 存在既有 warning，不阻断产物生成 |

## 12. 审查结论
编码实现已完成，核心权限条件由后端强制追加，写接口已纳入权限校验，默认 `TENANT` 策略保持历史低代码页面兼容。当前结论为可进入 Review 和角色矩阵接口验收；仍需在具备真实角色、组织、区划和业务数据的环境中完成接口回归。

## 13. 确认记录（HARD-GATE）
- **确认时间**：2026-05-24
- **确认人**：用户当前会话确认
- **确认内容**：动态低代码页面数据权限适配方案；角色数据范围以当前字典口径为主并兼容旧后端枚举；自定义组织继续复用 `sys_role_data_scope`，不新增独立低代码角色权限体系。
