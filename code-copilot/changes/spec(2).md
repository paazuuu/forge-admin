---
title: 组织上下文角色权限改造
version: 0.1
date_created: 2026-07-06
last_updated: 2026-07-06
status: archived
change_id: org-scoped-role-permission
complexity: 复杂
---

# 组织上下文角色权限改造

> status: archived
> created: 2026-07-06
> complexity: 复杂

## 0. 当前假设与推荐结论

- 数据中心是第一层上下文，登录页选择数据中心、右上角现有 `TenantSwitcher` 切换数据中心，决定 `tenantId`、数据中心隔离、数据中心配置和数据中心边界。
- 组织是第二层上下文，必须存在于当前数据中心内，代表集团公司、子公司或部门工作身份，决定当前角色、菜单、按钮、API 权限和数据权限。
- 保留现有数据中心切换能力，不用组织切换替代数据中心切换；新增组织切换器应放在数据中心切换旁边或用户信息附近，并明确命名为“当前组织”。
- 用户希望同一个账号可以加入多个组织，不需要为不同组织重复新建账号；同时每个组织仍可以创建和管理自己范围内的账号。
- 用户在不同组织内可以拥有不同角色和权限。权限计算必须以“当前组织上下文”为边界，不能把 A 组织角色泄漏到 B 组织。
- 推荐模型为三层关系：
  - `sys_user_org`：用户和组织的成员关系。
  - `sys_role_org`：角色在哪些组织可用或可被分配。
  - `sys_user_org_role`：用户在某个组织下被授予哪些角色，这是实际授权关系。
- 推荐不要把“角色绑定多组织”直接当成用户授权。角色绑定组织只表示角色适用范围；真正的授权必须落在“用户 + 组织 + 角色”三元关系上。
- 登录或切换数据中心后，在当前 `tenantId` 内确定 `activeOrgId`。默认进入当前数据中心内主组织，切换组织后重新计算 `roleIds`、`roleKeys`、`permissions`、`apiPermissions` 和数据权限上下文。
- `activeOrgId`、`mainOrgId`、`orgIds` 只在当前 `tenantId` 内有效；切换数据中心后必须重新加载该数据中心下可用组织、默认组织、组织角色和权限。
- 用户已确认全部按推荐执行，当前 Apply、测试、自审、修复后复审已完成；本轮复审发现的持久化/初始化展示文案残留已补齐并通过复审，归档确认已收到。

## 1. 背景与目标

当前用户测试发现“一个用户只能在一个组织”。代码事实显示后端已有多组织成员关系基础，直接限制来自前端用户组织弹窗只保留第一个组织；更大的权限缺口在角色侧：现有 `sys_user_role` 是用户和角色的全局关系，不带组织上下文，无法表达“同一用户在不同组织拥有不同权限”。

目标是把系统权限从“用户在数据中心内拥有一组全局角色”升级为“用户在当前组织内拥有一组组织角色”，并保持数据中心隔离、组织数据权限和普通组织管理员自主管理能力。

本次不重构登录页数据中心选择、数据中心切换和 `sys_user_tenant` 模型，而是在现有数据中心上下文内补齐组织工作身份、组织角色授权和组织数据权限。

### 成功标准

- 登录页和现有右上角数据中心切换仍以 `tenantId` 作为第一层数据中心上下文。
- 用户登录或切换数据中心后，只加载该数据中心下的组织、默认组织、组织角色和权限。
- 同一个用户账号可以绑定多个组织，并有且仅有一个主组织作为默认进入组织。
- 用户切换组织后，菜单、按钮、API 权限和数据权限按当前组织下的角色重新计算。
- 同一用户在组织 A 可拥有角色 X，在组织 B 可拥有角色 Y，两个组织权限互不串权。
- 角色可以配置适用组织范围，普通组织管理员只能把自己可管理范围内的角色授予当前组织成员。
- 组织管理员新建账号时，账号默认加入当前组织，可继续绑定更多组织和组织内角色。
- 现有单组织用户、管理员、数据中心管理员、历史用户角色数据有明确兼容迁移方案。

## 2. 技术栈与执行命令

### 技术栈

- 后端：Java 17、Spring Boot 3.2、MyBatis-Plus、Sa-Token、Flyway、MySQL。
- 前端：Vue 3、Naive UI、Pinia、Vite、UnoCSS。
- 权限链路：`forge-plugin-system` 负责用户、角色、组织和资源；`forge-starter-auth` 读取登录态权限；`forge-starter-datascope` 负责数据权限。

### 后续验证命令

- 后端编译：`cd forge-server && D:\nancc\huanjing\apache-maven-3.9.9\bin\mvn.cmd clean compile -DskipTests`
- 后端测试：`cd forge-server && D:\nancc\huanjing\apache-maven-3.9.9\bin\mvn.cmd test -Penable-tests`
- 前端构建：`cd forge-admin-ui && pnpm build`
- 前端检查：`cd forge-admin-ui && pnpm lint:fix`

执行测试、阶段验证或归档前，必须先读取并遵守 `code-copilot/rules/automated-testing-standard.md`，复用本变更的 `test-spec.md` 与 `execution-log.md`。

## 3. 项目结构与边界

- 后端系统插件：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/`
- 权限登录态：`forge-server/forge-framework/forge-starter-parent/forge-starter-core/`
- 数据权限启动器：`forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/`
- 低代码动态数据权限：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/`
- 数据库迁移：`forge-server/db/migration/`
- 前端管理页：`forge-admin-ui/src/views/system/`
- 前端登录态映射：`forge-admin-ui/src/store/helper.js`
- 变更文档：`code-copilot/changes/org-scoped-role-permission/`

## 4. 代码现状

### 4.1 已有能力

- `sys_user_org` 已经是多组织成员表，唯一键为 `(tenant_id, user_id, org_id)`，没有 `user_id` 单唯一；字段包含 `is_main` 主组织标记。出处：`forge-server/db/全量初始化SQL.sql:5733`、`forge-server/db/全量初始化SQL.sql:5741`、`forge-server/db/全量初始化SQL.sql:5744`。
- `SysUserOrg` 实体已映射 `sys_user_org`，字段包含 `tenantId`、`userId`、`orgId`、`isMain`、`createTime`。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysUserOrg.java:14`。
- `UserOrgBindDTO` 已支持 `List<Long> orgIds` 和 `Long mainOrgId`。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/dto/UserOrgBindDTO.java:11`、`:16`、`:21`。
- 用户组织接口已经有批量查询和批量绑定入口：`GET /system/user/{userId}/orgs` 返回组织 ID 列表，`POST /system/user/{userId}/orgs` 接收 `UserOrgBindDTO`。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/SysUserController.java:184`、`:220`。
- `SysUserServiceImpl.bindUserOrgs` 已按数据中心校验组织、删除不再需要的组织、插入新增组织、更新主组织，并同步当前用户组织会话。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysUserServiceImpl.java:476`、`:490`、`:506`、`:519`、`:543`、`:546`。
- 登录态 `LoginUser` 已有 `roleIds`、`roleKeys`、`permissions`、`apiPermissions`、`orgIds`、`mainOrgId`。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/session/LoginUser.java:83`、`:88`、`:93`、`:99`、`:104`、`:109`。
- `UserLoadServiceImpl.loadUserOrgs` 会加载用户所有组织，并设置 `mainOrgId`。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImpl.java:210`、`:217`、`:220`、`:222`。
- 用户列表查询已经能按组织筛选，并聚合展示多个组织名称。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserMapper.xml:99`、`:101`、`:145`、`:146`。
- 前端登录态已经把后端 `orgIds` 与 `mainOrgId` 放入用户信息。出处：`forge-admin-ui/src/store/helper.js:29`、`:30`。
- 登录页已有数据中心选择，`selectedTenantId` 绑定数据中心下拉，登录 payload 携带 `tenantId`，登录后用户信息写入 `tenantId`、`tenantName`、`tenantIds`。出处：`forge-admin-ui/src/views/login/index.vue:124`、`:131`、`:841`、`:847`、`:886`、`:954`。
- 顶部已有 `TenantSwitcher.vue`，基于 `userStore.userInfo.tenantId` 展示当前数据中心，调用 `getCurrentTenantOptions()` 加载可切换数据中心，调用 `switchTenant(tenantId)` 完成数据中心切换。出处：`forge-admin-ui/src/layouts/components/TenantSwitcher.vue`、`forge-admin-ui/src/api/index.js:17`、`:20`。
- 后端已有数据中心切换接口 `POST /system/tenant/switch`，`SysTenantServiceImpl.switchTenant` 会校验数据中心、校验非管理员的用户数据中心绑定、重载指定数据中心下登录态、写回 Session 并设置 `TenantContextHolder`。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/SysTenantController.java`、`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysTenantServiceImpl.java`。

### 4.2 关键缺口

- 前端用户组织弹窗当前使用 `mainOrgId ? [mainOrgId] : []` 作为选中值，只把接口返回的组织列表取第一个，并提交 `orgIds: [mainOrgId.value]`。这会把后端多组织能力表现成单组织。出处：`forge-admin-ui/src/views/system/user.vue:388`、`:1962`、`:1981`、`:1996`。
- `sys_user_role` 只有 `tenant_id`、`user_id`、`role_id`，没有 `org_id`。当前无法区分用户在不同组织的角色。出处：`forge-server/db/全量初始化SQL.sql:5777`、`:5780`、`:5781`、`:5784`。
- `SysUserRole` 实体也只有用户和角色关系，没有组织字段。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysUserRole.java:14`。
- `SysUserServiceImpl.syncUserRoles` 当前同步的是用户在数据中心内的全局角色集合，删除和插入均基于 `userId + tenantId + roleId`。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysUserServiceImpl.java:1094`、`:1122`、`:1139`。
- `UserLoadServiceImpl.loadUserRoles` 当前按用户和数据中心查询全部 `sys_user_role`，并把所有角色写入登录态。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImpl.java:164`、`:166`、`:174`。
- 权限加载使用登录态全部角色查询资源，未按组织过滤。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImpl.java:238`、`:247`、`:253`；`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysResourceMapper.xml:43`、`:47`。
- 数据权限服务当前从登录态全部角色计算最小数据范围，并把登录态全部组织放入 `DataScopeContext`。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/service/impl/DataScopeServiceImpl.java:107`、`:118`、`:132`、`:133`。
- 数据权限拦截器的 `ORG` 和 `ORG_AND_CHILD` 使用 `context.getOrgIds()`，因此如果上下文仍是用户全部组织，就会扩大当前组织的数据范围。出处：`forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/handler/DataScopeInterceptor.java:203`、`:212`。
- 低代码动态数据权限的 `ORG_AND_CHILD` 同样基于 `context.getOrgIds()`。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicDataScopeService.java:154`。
- `SysUserDTO` 目前只有 `postIds` 和 `roleIds`，没有 `orgIds`、`mainOrgId` 或组织角色绑定结构。出处：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/dto/SysUserDTO.java:104`、`:109`。
- 历史迁移 `V1.0.66__allow_normal_user_org_user_role_management.sql` 已把组织、用户、角色管理资源下放给普通用户类型，说明“组织内自主管理账号和角色”已是既有方向，但需要更细的组织权限边界。出处：`forge-server/db/migration/V1.0.66__allow_normal_user_org_user_role_management.sql:4`、`:9`、`:10`、`:11`。

## 5. 功能点

- [ ] 用户组织绑定从单选升级为多选，支持主组织。
- [ ] 角色管理支持配置适用组织列表。
- [ ] 用户角色授权从全局角色升级为组织内角色。
- [ ] 登录态在现有数据中心上下文下增加当前组织，登录、切换数据中心或切换组织后按对应上下文重算权限。
- [ ] 数据权限按当前组织上下文计算。
- [ ] 组织管理员新建账号默认加入当前组织，并只能分配可管理角色。
- [ ] 旧 `sys_user_role` 数据按兼容策略迁移或映射到组织角色。

## 6. 业务规则

- 用户先处于某个 `tenantId`，再处于该数据中心内某个 `activeOrgId`。
- `activeOrgId`、`mainOrgId`、`orgIds` 均为当前数据中心内语义，不能跨数据中心复用。
- 切换数据中心后必须清空或重置旧数据中心的 `activeOrgId`，重新加载新数据中心下可用组织并选择默认组织。
- 切换组织不改变 `tenantId`，只改变当前数据中心内的工作组织和权限计算结果。
- 用户可以属于同一数据中心下多个组织，但默认组织只能有一个。
- 当前组织必须来自用户已绑定组织列表；用户不能切换到未加入的组织。
- 当前组织下没有角色时，普通用户应只有最小登录能力，不应继承其他组织角色。
- 管理员和数据中心管理员保持现有特权边界，但仍应展示当前组织，避免普通逻辑依赖空组织。
- 角色适用组织决定该角色可在哪些组织被分配；实际用户授权必须写入 `sys_user_org_role`。
- 普通组织管理员只能给目标用户分配自己在当前组织可管理、且目标组织可用的角色。
- 角色数据范围 `ORG` 只代表当前组织；`ORG_AND_CHILD` 只代表当前组织及子组织，不代表用户所有组织。
- 删除用户组织成员关系时，必须同步清理该用户在该组织下的组织角色。
- 删除或禁用角色时，必须保证组织角色授权不再参与权限计算。

## 7. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增 | `sys_role_org` | `id`, `tenant_id`, `role_id`, `org_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time` | 角色适用组织范围 |
| 新增 | `sys_role_org` | 唯一键 `uk_role_org(tenant_id, role_id, org_id)` | 防止同角色重复绑定同组织 |
| 新增 | `sys_user_org_role` | `id`, `tenant_id`, `user_id`, `org_id`, `role_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time` | 用户在组织内的实际角色授权 |
| 新增 | `sys_user_org_role` | 唯一键 `uk_user_org_role(tenant_id, user_id, org_id, role_id)` | 防止重复授权 |
| 新增 | `sys_user_org_role` | 索引 `idx_user_org(tenant_id, user_id, org_id)`, `idx_org_role(tenant_id, org_id, role_id)` | 支持登录加载、组织成员授权和角色反查 |
| 迁移 | `sys_user_role` 到 `sys_user_org_role` | 已确认按推荐 | 把历史全局角色复制到用户已绑定组织，保持现有行为不收窄；后续由管理员按组织回收 |

数据库脚本必须走 `forge-server/db/migration/` Flyway 新版本。当前目录最高已看到 `V1.0.150__hide_invalid_dynamic_crud_entries.sql`，后续实现时建议从 `V1.0.151__add_org_scoped_role_permission.sql` 开始，并使用 `CREATE TABLE IF NOT EXISTS`、`INSERT ... WHERE NOT EXISTS` 或等效防重复保护。

## 8. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 保留并联动增强 | `/system/tenant/switch` | POST | 切换数据中心，返回新数据中心登录态，并重载新数据中心下默认组织上下文 |
| 新增 | `/system/org/switch` | POST | 在当前数据中心内切换当前组织，返回重算后的登录态权限 |
| 新增 | `/system/user/{userId}/org-bindings` | GET | 返回用户组织详情、主组织、每个组织下角色摘要 |
| 兼容增强 | `/system/user/{userId}/orgs` | GET | 保留返回组织 ID 列表，供旧前端兼容 |
| 兼容增强 | `/system/user/{userId}/orgs` | POST | 继续绑定组织列表和主组织，同时清理被移除组织的组织角色 |
| 新增 | `/system/user/{userId}/org-roles` | GET | 按 `orgId` 查询用户在该组织下的角色 |
| 新增 | `/system/user/{userId}/org-roles` | POST | 按 `orgId + roleIds` 保存用户组织角色 |
| 新增 | `/system/role/{roleId}/orgs` | GET | 查询角色适用组织 |
| 新增 | `/system/role/{roleId}/orgs` | POST | 保存角色适用组织 |
| 兼容调整 | `/system/user/{userId}/roles` | POST | 标记为全局旧接口，后续只用于兼容或映射到当前组织，不作为新 UI 主入口 |

### 数据中心与组织上下文契约

已有数据中心上下文：

- `tenantId`：当前数据中心 ID，由登录页选择或 `/system/tenant/switch` 确定。
- `tenantName`：当前数据中心名称，用于顶部数据中心切换器展示。
- `tenantIds`：当前用户可访问数据中心集合，用于数据中心切换候选范围。

`LoginUser` 建议在现有数据中心上下文下新增：

- `activeOrgId`：当前组织 ID。
- `activeOrgName`：当前组织名称。
- `activeOrgId` 只在当前 `tenantId` 下有效，组织切换不改变 `tenantId`。
- `activeOrgIds` 不建议新增；数据权限应只使用当前组织或当前组织子树。
- `orgRoles` 或轻量摘要可选，用于前端组织切换器展示，不参与权限判断。

权限加载流程：

1. 登录页或 `/system/tenant/switch` 先确定 `tenantId`。
2. `UserLoadServiceImpl` 在当前 `tenantId` 下加载用户可用组织 `orgIds` 与主组织 `mainOrgId`。
3. 确定 `activeOrgId`：优先使用当前数据中心内已保存组织，其次主组织，再其次当前数据中心内第一个组织。
4. 使用 `tenantId + userId + activeOrgId` 查询 `sys_user_org_role`。
5. 过滤已禁用角色和不适用于当前组织的角色。
6. 通过有效角色加载 `roleKeys`、按钮权限、API 权限。
7. 数据权限上下文写入 `orgIds = [activeOrgId]`，`ORG_AND_CHILD` 再由数据权限服务展开子组织。

## 9. 影响范围

- 系统管理：用户管理、角色管理、组织管理、登录态加载、菜单权限、API 权限。
- 登录与上下文：保留登录页数据中心选择和 `/system/tenant/switch` 数据中心切换链路，新增数据中心内组织上下文联动。
- 数据权限：普通 SQL 拦截器、低代码动态数据权限、组织树可见范围。
- 前端体验：用户组织弹窗、用户角色授权弹窗、角色适用组织配置、保留顶部数据中心切换器并新增“当前组织”切换器。
- 迁移兼容：历史 `sys_user_role`、历史单组织用户、管理员账号、普通组织管理员账号。
- 可能受影响模块：消息按角色接收、流程发起变量 `startUserRoleIds`、业务动作按角色发送消息等所有读取 `loginUser.roleIds` 或 `sys_user_role` 的链路。

## 10. 风险与关注点

- 权限变更属于高风险变更，必须重点防止组织间权限串权。
- 混淆数据中心与组织会导致跨数据中心组织 ID 误用，必须在接口、登录态和 UI 命名中清晰区分。
- 切换数据中心后如果不清理旧 `activeOrgId`，可能用旧数据中心组织参与新数据中心权限计算。
- 如果旧 `sys_user_role` 继续作为普通权限来源，切换组织后仍可能出现全局角色泄漏。
- 如果迁移只复制到主组织，历史多组织用户在非主组织权限会收窄；如果复制到所有组织，安全边界不比当前更差，但需要后续手动回收。
- `role_org` 空绑定语义必须明确。推荐迁移后角色都显式绑定适用组织；空绑定只作为系统管理员维护状态，不允许普通组织管理员分配。
- 数据权限的 `ALL` 与 `TENANT` 角色必须限制可分配人群，避免普通组织管理员给他人授予数据中心级权限。
- 前端切组织后必须刷新路由、菜单和权限缓存，否则会出现 UI 可见但接口拒绝或接口可访问但菜单不可见的不一致。
- 前端同时出现数据中心切换和组织切换时，必须通过名称、图标和位置区分“数据中心”和“当前组织”，避免用户误操作。
- 需要排查所有直接读 `sys_user_role` 的 Mapper 和服务，避免只改登录链路但消息、流程、数据集等旁路仍按全局角色计算。

## 11. 测试策略

- 后端服务测试：覆盖组织角色授权保存、切组织权限重算、角色适用组织校验、删除组织成员后清理授权。
- 权限测试：覆盖同一用户在两个组织有不同角色，切换组织后菜单权限、API 权限和数据范围不同。
- 数据权限测试：覆盖 `ORG`、`ORG_AND_CHILD`、`CUSTOM`、无组织角色、数据中心管理员、超级管理员。
- 迁移测试：覆盖已有 `sys_user_role` 到 `sys_user_org_role` 的数据迁移，检查唯一键和无组织用户边界。
- 前端验证：覆盖多组织绑定、主组织选择、组织切换器、用户组织角色授权、角色适用组织配置。
- 回归验证：用户、角色、组织页面构建通过；核心登录、菜单加载、接口鉴权不回退。

独立测试计划见 `code-copilot/changes/org-scoped-role-permission/test-spec.md`。

## 12. 技术决策

- 选择新增 `sys_user_org_role`，不直接给 `sys_user_role` 加 `org_id`。原因是可以保留旧接口和迁移窗口，避免一次性破坏所有直接读取 `sys_user_role` 的存量链路。
- 选择新增 `sys_role_org` 表示角色适用组织，不把角色复制成多个组织角色。原因是角色本身仍是数据中心内权限模板，资源绑定仍保持 `sys_role_resource` 一套。
- 选择数据中心作为一级上下文、组织作为二级上下文。原因是系统已有登录页数据中心选择和右上角数据中心切换，组织权限应跟随当前数据中心，而不是替代数据中心。
- 选择 `activeOrgId` 作为权限计算上下文。原因是用户多组织并不等于每次请求都应该拥有所有组织权限。
- 选择复用现有数据中心登录和 `/system/tenant/switch` 链路。本次不重构 `sys_user_tenant`、数据中心切换、客户端登录配置等更大范围能力。
- 选择保留旧 `sys_user_role` 为兼容来源或迁移来源，但新 UI 和新权限计算应以 `sys_user_org_role` 为主。

## 13. 已确认与待归档状态

### 已确认

- 数据中心是登录页和右上角现有切换器的上下文，组织切换不替代数据中心切换。
- 组织切换器作为“当前组织”放在顶部数据中心切换器旁边或用户信息附近，多组织用户显示，单组织用户隐藏。
- 历史 `sys_user_role` 迁移范围按推荐执行：复制到用户所有已绑定组织，保持现有权限不收窄，后续由管理员按组织回收。
- 角色未绑定任何组织时按推荐执行：普通组织管理员不可分配，系统管理员可维护，迁移后尽量显式绑定组织。
- 无角色组织成员按推荐执行：允许存在，但该组织下只有最小登录能力。
- 产品展示术语统一为“数据中心”；技术标识继续保留 `tenantId`、`tenant_id`、`SysTenant` 和 `/system/tenant/*`。

### 复审修复结果

- [x] 持久化低代码配置、初始化脚本或资源示例中仍可能出现旧展示术语，已按“技术标识保留、产品展示改名”的边界补齐修复。
- [x] 新增 `V1.0.154__rename_persisted_tenant_labels_to_data_center.sql`，覆盖 `ai_crud_config`、`ai_crud_config_version`、`ai_lowcode_model`、`sys_api_config`、`sys_role`、`sys_tenant` 的产品可见名称、备注、Schema 和发布快照。
- [x] 初始化类 SQL 和示例资源中的中文展示词已统一为“数据中心”，历史 Flyway 迁移与备份目录保持不改。

## 14. 验收标准

- **AC-001**：Given 用户 U 属于组织 A 和组织 B，When 管理员在 A 给 U 分配角色 R1、在 B 给 U 分配角色 R2，Then U 切换到 A 只拥有 R1 权限，切换到 B 只拥有 R2 权限。
- **AC-002**：Given 用户 U 的角色 R1 数据范围为 `ORG`，When U 当前组织为 A，Then 数据权限只包含 A，不包含 U 绑定的其他组织。
- **AC-003**：Given 用户 U 的角色 R1 数据范围为 `ORG_AND_CHILD`，When U 当前组织为 A，Then 数据权限只包含 A 及 A 子组织，不包含 U 绑定的其他平级组织。
- **AC-004**：Given 普通组织管理员 M 在组织 A，When M 新建用户，Then 新用户默认绑定组织 A，且 M 只能分配 M 在 A 可管理的角色。
- **AC-005**：Given 角色 R 只适用于组织 A，When 管理员尝试把 R 分配给组织 B 下的用户，Then 后端拒绝并返回明确错误。
- **AC-006**：Given 旧用户已有 `sys_user_role`，When 迁移完成并登录，Then 用户原有主组织或已绑定组织权限按确认的迁移策略可用，不出现空菜单或越权。
- **AC-007**：Given 前端用户组织弹窗打开，When 选择多个组织并指定主组织，Then 保存后列表展示多个组织，重新打开仍能回显多个组织。
- **AC-008**：Given 切换组织成功，When 前端刷新菜单和权限缓存，Then 不展示当前组织无权访问的菜单和按钮。
- **AC-009**：Given 用户选择数据中心 T1 登录，When 登录成功，Then 只加载 T1 下组织、默认组织和组织角色权限。
- **AC-010**：Given 用户从数据中心 T1 切换到 T2，When `/system/tenant/switch` 成功，Then 清空或重置 T1 的 `activeOrgId`，并选择 T2 下默认组织重新计算权限。
- **AC-011**：Given 用户在数据中心 T1 内选择组织 A，When 调用 `/system/org/switch` 成功，Then `tenantId` 保持 T1 不变，只重算组织 A 的角色、菜单、按钮、API 权限和数据权限。

## 15. 边界

- Always：所有数据库结构和内置资源变更必须走 Flyway 新脚本。
- Always：涉及查询类 SQL 的复杂查询优先写 Mapper XML，保持数据权限拦截可审查。
- Always：权限计算、组织切换、角色分配必须做数据中心校验和组织成员校验。
- Always：数据中心切换和组织切换必须是两个不同上下文，切换数据中心后重新加载组织上下文。
- Ask First：改变旧 `sys_user_role` 的删除策略、废弃旧接口、收窄历史用户权限。
- Ask First：引入新的前端全局状态方案或额外依赖。
- Never：不为多组织能力复制用户账号，不用前端隐藏替代后端鉴权。
- Never：不用组织切换替代数据中心切换，不跨数据中心复用 `activeOrgId`。
- Never：不允许普通组织管理员分配自己没有或目标组织不可用的角色。
- Never：不修改已经执行过的历史 Flyway 脚本。

## 16. 确认记录

- **确认时间**：2026-07-06
- **确认人**：用户
- **进入 Apply 条件**：用户已确认全部按推荐执行。
- **复审后修复**：产品展示术语统一为“数据中心”，技术标识保持 `tenant` 体系兼容。
- **复审结论**：两阶段复审通过，用户已确认归档。
