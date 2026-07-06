# 组织上下文角色权限改造
> status: apply
> created: 2026-07-06
> complexity: 🔴复杂
> change_id: org-scoped-role-permission

## 1. 背景与目标

当前系统已经具备数据中心（技术字段仍为 `tenantId` / `tenant_id`）隔离、用户多租户绑定、组织树、角色资源、按钮/API 权限和数据权限拦截能力。用户测试发现“一个用户只能在一个组织”，直接原因是前端用户组织弹窗只保留一个主组织；更深层的问题是角色授权仍是 `tenant_id + user_id + role_id` 的全局关系，不能表达“同一账号在不同组织拥有不同角色和数据权限”。

本变更把权限模型升级为两层上下文：

1. 数据中心是第一层上下文，由登录页选择和顶部数据中心切换器决定，继续使用现有 `tenantId`、`sys_user_tenant` 和 `/system/tenant/switch` 链路。
2. 组织是第二层上下文，必须属于当前数据中心。当前组织决定用户当前可用角色、菜单、按钮、API 权限和数据权限。

完成后应达到：

- 同一个账号可以加入同一数据中心下多个组织，并有一个主组织作为默认进入组织。
- 同一账号在组织 A 可以拥有角色 R1，在组织 B 可以拥有角色 R2，切换组织后权限互不串权。
- 角色可以配置适用组织范围。普通组织管理员只能给当前组织成员分配自己可管理、且目标组织可用的角色。
- `ORG` 数据权限只代表当前组织，`ORG_AND_CHILD` 只代表当前组织及其子组织，不再代表用户绑定的所有组织。
- 保留历史 `sys_user_role` 作为迁移和兼容来源，新权限计算以 `sys_user_org_role` 为准。

## 2. 代码现状（Research Findings）

### 2.1 数据中心与登录上下文

- 登录态由 `UserLoadServiceImpl#buildLoginUser` 构建，先校验数据中心成员，再加载角色、组织、按钮权限、API 权限和行政区划。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImpl.java`。
- 顶部数据中心切换调用 `SysTenantServiceImpl#switchTenant`，会重新加载指定数据中心下的 `LoginUser` 并写回 Session。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysTenantServiceImpl.java`。
- `/auth/userInfo` 每次会按当前 Session 的 `tenantId` 重新从 DB 构建 `LoginUser`，再写回 Session。路径：`forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/controller/AuthController.java`，方法：`getUserInfo`。
- 前端 `TenantSwitcher.vue` 只处理数据中心切换，切换后清理登录相关状态并重新进入首页。路径：`forge-admin-ui/src/layouts/components/TenantSwitcher.vue`。

结论：数据中心上下文链路可复用，但组织上下文需要在 `LoginUser` 重建时保留当前组织，否则 `/auth/userInfo` 会把组织切换结果覆盖回默认组织。

### 2.2 用户组织关系

- `SysUserOrg` 已映射 `sys_user_org`，字段包含 `tenantId`、`userId`、`orgId`、`isMain`。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysUserOrg.java`。
- `SysUserController` 已提供 `GET /system/user/{userId}/orgs` 和 `POST /system/user/{userId}/orgs`。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/SysUserController.java`。
- `SysUserServiceImpl#bindUserOrgs` 已能按数据中心批量保存用户组织并设置主组织。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysUserServiceImpl.java`。
- 前端用户组织弹窗 `PremiumTree` 仍使用单选 `selected-keys="mainOrgId ? [mainOrgId] : []"`，读取接口后只取第一个组织，保存时提交 `orgIds: [mainOrgId]`。路径：`forge-admin-ui/src/views/system/user.vue`。

结论：后端多组织成员关系基础已存在，前端和组织角色联动还未完成。删除用户组织关系时，需要同步清理该组织下的组织角色授权。

### 2.3 用户角色关系

- `SysUserRole` 当前只映射 `sys_user_role`，字段为 `tenantId`、`userId`、`roleId`，没有 `orgId`。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysUserRole.java`。
- `UserLoadServiceImpl#loadUserRoles` 按 `userId + tenantId` 查询所有 `sys_user_role`，并把启用角色写入 `LoginUser.roleIds/roleKeys`。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImpl.java`。
- `SysUserServiceImpl#syncUserRoles` 按 `userId + tenantId` 删除和插入全局角色，普通管理员只能分配自己拥有的角色。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysUserServiceImpl.java`。
- 用户新增/编辑 DTO 只有 `roleIds`，没有 `orgIds/mainOrgId/orgRoleBindings`。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/dto/SysUserDTO.java`。

结论：必须新增 `sys_user_org_role` 作为真实授权关系。旧 `sys_user_role` 只能作为迁移来源或旧接口兼容来源，不能继续参与新权限计算。

### 2.4 角色适用组织

- `SysRole` 只有角色基础字段和 `dataScope`，没有适用组织字段。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysRole.java`。
- `SysRoleDTO` 没有 `orgIds`。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/dto/SysRoleDTO.java`。
- `SysRoleMapper.xml#selectRoleUsers` 和 `countUsersByRole` 都直接查询 `sys_user_role`。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysRoleMapper.xml`。

结论：需要新增 `sys_role_org` 表示角色在哪些组织可被分配。角色绑定组织不是用户授权，用户授权仍必须落到 `sys_user_org_role`。

### 2.5 菜单、按钮和 API 权限

- 登录时按钮权限由 `UserLoadServiceImpl#loadUserPermissions` 读取 `LoginUser.roleIds` 对应的 `sys_role_resource`。
- 登录时 API 权限由 `UserLoadServiceImpl#loadApiPermissions` 读取同一批角色资源，并缓存到 `LoginUser.apiPermissions`。
- 菜单树由 `SysResourceServiceImpl#getUserResources` 基于 `LoginUser.roleIds` 查询角色资源。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysResourceServiceImpl.java`。
- API 拦截器 `ApiPermissionInterceptor` 通过 `PermissionServiceImpl#getCurrentUserApiPermissions` 从 Session 的 `LoginUser.apiPermissions` 判断接口权限。路径：`forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/interceptor/ApiPermissionInterceptor.java`。

结论：只要 `LoginUser.roleIds` 在登录、数据中心切换和组织切换时按 `activeOrgId` 重算，菜单、按钮和 API 权限主链路可复用。但所有直接读取 `sys_user_role` 的旁路必须同步改造。

### 2.6 数据权限

- `LoginUser` 当前只有 `orgIds/mainOrgId`，没有 `activeOrgId/activeOrgName`。路径：`forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/session/LoginUser.java`。
- `DataScopeServiceImpl#getCurrentUserDataScope` 用 `LoginUser.roleIds` 算最小数据范围，并把 `LoginUser.orgIds` 写入 `DataScopeContext.orgIds`。路径：`forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/service/impl/DataScopeServiceImpl.java`。
- `DataScopeInterceptor#buildDataScopeCondition` 的 `ORG` 直接使用 `context.orgIds`，`ORG_AND_CHILD` 使用 `dataScopeService.getOrgAndChildIds(context.orgIds)`。路径：`forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/handler/DataScopeInterceptor.java`。
- 低代码动态 CRUD 通过 `DynamicDataScopeService` 自行读取 `DataScopeContext` 拼接组织条件，不走 MyBatis 拦截器。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicDataScopeService.java`。
- 数据集 ACL 和行级权限也直接使用 `LoginUser.roleIds/orgIds`。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/impl/DataDatasetAccessServiceImpl.java`、`DataDatasetRowScopeServiceImpl.java`。

结论：数据权限必须统一收窄到当前组织上下文。否则放开多组织后，`ORG` 会扩大到用户所有组织，形成越权。

### 2.7 旁路读取 `sys_user_role` 的模块

- 低代码业务消息按角色找接收人时查询 `sys_user_role`。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessMessageChannelMapper.xml`。
- 流程启动自动注入 `startUserRoleIds` 和 `startUserOrgIds`，当前取的是全局角色和全部组织。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowInstanceServiceImpl.java`。
- 流程组织集成服务按 `SysUserRole` 判断用户角色编码，未带数据中心和组织上下文。路径：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowOrgIntegrationServiceImpl.java`。

结论：必须排查并迁移所有旁路，否则会出现 UI 和接口主链路正确、消息/流程/数据集仍按全局角色运行的隐性串权。

### 2.8 数据库迁移目录状态

- 实际迁移目录 `forge-server/db/migration/` 当前只有 `V1.0.0__baseline.sql`。
- `forge-server/db/backup/` 是归档目录，不参与本次 Flyway 最新编号判断。
- 本变更 Flyway 版本号只按实际迁移目录排序继续，不使用外部提供文档或归档目录中的编号。

结论：当前实际迁移目录最新 Flyway 编号为 `V1.0.0`，本变更迁移脚本使用 `V1.0.1__add_org_scoped_role_permission.sql`。禁止修改已执行历史脚本。

## 3. 功能点

- [x] 用户组织绑定从单选升级为多选，支持唯一主组织。
- [x] 新增当前组织上下文，登录、切换数据中心和切换组织后按组织重算权限。
- [x] 新增角色适用组织配置，控制角色可在哪些组织被分配。
- [x] 用户角色授权从全局角色升级为组织内角色，授权关系写入 `sys_user_org_role`。
- [x] 菜单、按钮、API 权限按当前组织下有效角色计算。
- [x] 数据权限按当前组织计算，`ORG` 和 `ORG_AND_CHILD` 不再使用用户全部组织。
- [x] 普通组织管理员新建账号默认加入当前组织，只能授权可管理且目标组织可用的角色。
- [x] 历史 `sys_user_role` 数据迁移到组织角色授权，保持历史用户不因升级空菜单。
- [x] 消息、流程、数据集等旁路按当前组织角色和当前组织重算。
- [x] 前端新增“当前组织”切换器，并在切换后刷新用户信息、菜单、权限和标签页。

## 4. 业务规则

- 用户必须先处于某个数据中心 `tenantId`，再处于该数据中心内某个当前组织 `activeOrgId`。
- `activeOrgId`、`mainOrgId`、`orgIds` 只在当前 `tenantId` 内有效，不能跨数据中心复用。
- 当前组织必须来自用户在当前数据中心下绑定的组织列表。
- 登录和切换数据中心时默认组织选择顺序为：当前 Session 中仍有效的 `activeOrgId`、主组织、组织列表第一个。
- 本期不新增跨登录持久化的“上次组织”。刷新页面通过 Session 保留当前组织；重新登录和切换数据中心按默认组织规则进入。
- 当前组织下没有角色时，普通用户只能拥有最小登录能力，不继承其他组织角色。
- 角色适用组织只表示角色可分配范围，不等同于用户授权。
- 用户组织角色授权必须同时满足：用户属于目标组织、角色属于当前数据中心、角色启用、角色适用于目标组织。
- 删除用户组织成员关系时，必须删除该用户在该组织下的 `sys_user_org_role`。
- 删除、禁用角色或移除角色适用组织后，该角色不得继续参与对应组织的权限计算。
- `ALL` 和 `TENANT` 数据范围角色不得由普通组织管理员越权分配。
- 产品展示统一使用“数据中心”；技术标识继续保留 `tenant` 命名，避免大范围破坏现有代码。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增 | `sys_role_org` | `id`, `tenant_id`, `role_id`, `org_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time` | 角色适用组织范围 |
| 新增 | `sys_role_org` | 唯一键 `uk_role_org(tenant_id, role_id, org_id)` | 防止重复绑定 |
| 新增 | `sys_role_org` | 索引 `idx_org_role(tenant_id, org_id, role_id)` | 按组织加载可分配角色 |
| 新增 | `sys_user_org_role` | `id`, `tenant_id`, `user_id`, `org_id`, `role_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time` | 用户在组织内的真实角色授权 |
| 新增 | `sys_user_org_role` | 唯一键 `uk_user_org_role(tenant_id, user_id, org_id, role_id)` | 防止重复授权 |
| 新增 | `sys_user_org_role` | 索引 `idx_user_org(tenant_id, user_id, org_id)`, `idx_org_role(tenant_id, org_id, role_id)` | 支持登录加载和角色反查 |
| 迁移 | `sys_role_org` | 从当前数据中心内组织补角色适用范围 | 历史角色默认对当前数据中心内所有组织可用，避免迁移后无法授权 |
| 迁移 | `sys_user_org_role` | 从 `sys_user_role` 复制到用户所有已绑定组织 | 保持当前行为不收窄，后续由管理员按组织回收 |

迁移脚本要求：

- 写入 `forge-server/db/migration/`。
- 版本号按实际迁移目录当前最新 Flyway 编号取下一号：`V1.0.1__add_org_scoped_role_permission.sql`。
- 使用 `CREATE TABLE IF NOT EXISTS`、`INSERT ... SELECT ... WHERE NOT EXISTS` 或等效防重复保护。
- 所有内置业务数据 `tenant_id` 使用 `1`，禁止写 `0`。
- 禁止提交真实密码、Token、AK/SK 或生产敏感数据。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 保留增强 | `/system/tenant/switch` | POST | 切换数据中心后重新加载该数据中心下组织，选择默认当前组织并重算权限 |
| 新增 | `/system/org/current/options` | GET | 查询当前数据中心下当前用户可切换组织，返回当前组织标识 |
| 新增 | `/system/org/switch` | POST | 请求参数 `orgId`，切换当前组织并返回重算后的 `LoginUser` |
| 新增 | `/system/user/{userId}/org-bindings` | GET | 返回用户组织详情、主组织、每个组织的角色摘要 |
| 兼容增强 | `/system/user/{userId}/orgs` | GET | 保留返回组织 ID 列表，旧前端兼容 |
| 兼容增强 | `/system/user/{userId}/orgs` | POST | 保存多组织和主组织，清理移除组织下的组织角色 |
| 新增 | `/system/user/{userId}/org-roles` | GET | 按 `orgId` 查询用户在该组织下角色 |
| 新增 | `/system/user/{userId}/org-roles` | POST | 按 `orgId + roleIds` 保存用户组织角色 |
| 新增 | `/system/role/{roleId}/orgs` | GET | 查询角色适用组织 ID 列表 |
| 新增 | `/system/role/{roleId}/orgs` | POST | 保存角色适用组织 |
| 兼容保留 | `/system/user/{userId}/roles` | GET/POST | 作为旧接口保留；新 UI 不再作为主入口 |
| 兼容保留 | `/system/role/{roleId}/users` | GET | 新增可选 `orgId` 参数；未传时按角色在当前数据中心下全部组织授权汇总 |

`LoginUser` 新增字段：

- `activeOrgId`：当前组织 ID。
- `activeOrgName`：当前组织名称。
- `orgIds`：当前数据中心下可加入组织集合，供切换器使用。
- `mainOrgId`：当前数据中心下默认组织。
- 不新增 `activeOrgIds`，避免把多个组织误当成当前权限范围。

## 7. 影响范围

- 系统管理：用户、角色、组织、资源、数据中心切换、在线用户显示。
- 权限链路：登录态加载、菜单、按钮权限、API 权限拦截、Sa-Token 角色接口。
- 数据权限：MyBatis 数据权限拦截器、低代码动态数据权限、数据集 ACL、数据集行级权限。
- 业务旁路：低代码业务消息接收人、流程启动变量、流程组织角色判断。
- 前端体验：用户组织弹窗、用户授权弹窗、角色适用组织配置、顶部当前组织切换器、个人资料组织展示。
- 数据迁移：历史 `sys_user_role`、历史单组织用户、普通组织管理员、数据中心管理员和超级管理员。

## 8. 风险与关注点

- ⚠️ 本变更属于权限变更，必须重点防止组织间角色、菜单、API 和数据权限串权。
- ⚠️ 如果旧 `sys_user_role` 继续参与普通权限计算，组织切换仍会泄漏全局角色。
- ⚠️ 如果数据权限仍使用 `LoginUser.orgIds`，放开多组织后 `ORG` 会扩大到用户所有组织。
- ⚠️ `/auth/userInfo` 会重建 `LoginUser`，组织切换后的 `activeOrgId` 必须在刷新用户信息时保留。
- ⚠️ 前端切换组织后必须刷新菜单和权限缓存，否则会出现菜单可见但接口拒绝，或接口可访问但菜单不可见。
- ⚠️ 角色适用组织为空的语义必须收紧：系统管理员可维护，普通组织管理员不可分配。
- ⚠️ 迁移策略按“复制到所有已绑定组织”保持不收窄，但上线后需要人工回收不应存在的组织角色。
- ⚠️ 消息、流程、数据集等旁路直接查 `sys_user_role`，必须纳入验收，不能只验证登录菜单。

## 8.5 测试策略

- **测试范围**：后端权限加载、组织切换、组织角色授权、角色适用组织、数据权限、低代码动态数据权限、数据集权限、前端核心交互。
- **覆盖率目标**：P0 覆盖权限计算和数据范围；P1 覆盖前端构建与关键接口；P2 覆盖消息、流程、数据集旁路回归。
- **独立 Test Spec**：是，见 `code-copilot/changes/org-scoped-role-permission/test-spec.md`。

## 9. 待澄清

- [x] 进入 `/apply` 前需确认本 SDD 版本为执行依据。
- [x] Flyway 编号按实际迁移目录排序继续：当前最新为 `V1.0.0`，本变更使用 `V1.0.1__add_org_scoped_role_permission.sql`。

## 10. 技术决策

- 选择新增 `sys_user_org_role`，不直接给 `sys_user_role` 加 `org_id`。原因是保留迁移窗口和旧接口兼容，降低一次性破坏面。
- 选择新增 `sys_role_org` 表示角色适用组织，不复制角色本体。原因是角色仍是数据中心内权限模板，资源绑定继续复用 `sys_role_resource`。
- 选择 `activeOrgId` 作为唯一当前组织上下文。原因是多组织成员身份不代表一次请求应拥有所有组织权限。
- 选择 Session 态保存当前组织，本期不新增跨登录持久化上次组织。原因是避免额外表结构和状态同步复杂度；重新登录默认主组织符合现有主组织语义。
- 选择让 `DataScopeContext.orgIds` 在运行时只承载 `[activeOrgId]`。原因是保留已有拦截器接口，同时从源头避免组织范围放大。
- 选择迁移历史 `sys_user_role` 到用户所有已绑定组织。原因是保持升级后行为不收窄，不让历史用户突然空菜单，后续再由管理员按组织治理。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| 建档 | completed | `spec.md`, `tasks.md`, `test-spec.md`, `execution-log.md` | 根据 `spec(2).md` 和当前代码影响面重新生成 SDD 提案 |
| Task 1-9 | completed | 后端 Java/XML/SQL 见 git diff | 完成数据模型、登录态、组织切换、组织角色授权、角色适用组织、数据权限和旁路适配 |
| Task 10 | completed | `forge-admin-ui/src/layouts/components/OrgSwitcher.vue` 等 | 当前组织切换器接入 normal/immersive/top/full/nexus 等布局 |
| Task 11 | completed | `forge-admin-ui/src/views/system/user.vue`, `role.vue`, `UserSelectPanel.vue` | 用户多组织/组织角色授权、角色适用组织和角色用户组织维度操作已接入；未新增独立 panel 组件，直接复用现有页面弹窗 |
| Task 12 | partially completed | `test-spec.md`, `execution-log.md`, `tasks.md`, `spec.md`, `InjectionMetaObjectHandler.java` | 后端编译、前端构建、Flyway 占位符扫描、`git diff --check` 通过；补充 `create_dept` 优先写当前组织；服务级接口/数据库实测待补 |
| Review 修复 | completed | `SysUserServiceImpl.java`, `SysOrgServiceImpl.java`, `SysRoleServiceImpl.java`, `SysUserMapper.xml`, `SysUserQuery.java`, `UserSelectPanel.vue`, `role.vue`, `BusinessMessageChannelService.java`, `SendMessageActionStepExecutor.java`, `BusinessTriggerExecutor.java` | 修复服务层权限兜底、旧角色加人接口绕过、角色页直接组织用户筛选和业务消息角色接收人组织上下文；后端编译、前端构建和静态检查通过 |
| 超级管理员组织选项边界修复 | completed | `SysOrgServiceImpl.java`, `SysOrgMapper.java`, `SysOrgMapper.xml`, `SysUserOrgMapper.java`, `SysUserOrgMapper.xml`, `test-spec.md`, `execution-log.md` | `/system/org/current/options` 和 `/system/org/switch` 直接按 `sys_user_org` 显式绑定组织收窄；仅无任何显式绑定的历史超级管理员账号保留当前数据中心全量组织兜底；前端仍保持扁平下拉展示；新增查询落到 Mapper XML |

## 12. 审查结论

当前已进入 apply 并完成代码实施。自审重点：

- 已覆盖原始需求中的数据中心/组织双上下文。
- 已覆盖当前系统中直接影响权限和数据权限的主链路。
- 已把直接读取 `sys_user_role` 的旁路模块纳入影响范围。
- 已按实际迁移目录最新 Flyway 编号确定本变更迁移脚本版本。
- 新前端主操作已改走 `sys_user_org_role`：用户授权按组织保存，角色用户添加/移除也按授权组织调用 `/system/user/{id}/org-roles`。
- 旧 `sys_user_role` 仍保留为迁移/兼容写入与清理来源，后续 review 需重点确认没有新增普通权限读取路径回退旧表。
- 自动填充链路已调整为优先使用 `LoginUser.activeOrgId` 写入 `create_dept`，避免切换组织后新数据写到非当前组织。
- Review 后补充服务层权限兜底：`@ApiPermissionIgnore` 继续遵循既有策略，服务层按用户/组织/角色管理权限限制普通用户访问；超级管理员和数据中心管理员保持现有能力。
- 旧 `/system/role/{roleId}/addUsers` 已阻断，不再只写 `sys_user_role`；新前端主流程继续按授权组织写 `sys_user_org_role`。
- 角色页添加用户时已按授权组织直接成员过滤，避免选择子组织成员后被 `用户未加入目标组织` 拒绝。
- 业务消息按角色接收人已支持从业务记录/表单/上下文传入 `tenantId + orgId`，Session `activeOrgId` 仅作为兜底。
- 超级管理员存在 `sys_user_org` 显式绑定时，组织切换选项和切换校验按真实绑定组织收窄；登录态历史兜底产生的全量 `orgIds` 不再作为可切换组织依据。无显式绑定的历史超级管理员账号继续保留当前数据中心全量组织兜底，避免升级后无法进入组织上下文。
- 前端组织切换器保持扁平下拉展示；后端即使返回树形结构，前端也只压平成路径文案，不改为树控件。
- 本轮未完成真实接口/数据库验证：当前 dev 配置指向远端 MySQL，直接启动会触发 Flyway 写库；本地 3407 MySQL 未运行，3306 root 无密码访问被拒，Redis 探测受沙箱限制且提权未获批。不能替代 `/test org-scoped-role-permission` 的服务级验收。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-06
- **确认人**：用户通过 `apply org-scoped-role-permission` 确认进入实施
- **进入 Apply 条件**：已满足；本轮已实施并完成最小静态验证闭环。
