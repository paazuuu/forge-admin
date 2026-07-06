# 任务拆分 — 组织上下文角色权限改造
> 拆分顺序：数据模型 → 接口协议 → 底层实现 → 上层编排 → 入口层 → 旁路回归
> 每个任务控制在可独立提交的原子变更内
> Commit message 格式：`[org-scoped-role-permission] <中文简述>`
> archive 状态：Task 1-11 已实施；Task 12 已完成静态检查、后端编译和前端构建；补充完成 `create_dept` 当前组织写入修正。Review 后已修复服务层权限兜底、旧角色加人接口绕过、角色加人直接组织筛选、业务消息组织上下文和超级管理员显式组织绑定边界。接口/数据库实测待可用运行环境补充。

## Review 后修复记录

- [x] 服务层兜底：`SysUserServiceImpl` 增加用户/组织/角色管理权限兜底；`SysOrgServiceImpl` 增加组织读写权限兜底；`SysRoleServiceImpl` 增加角色管理权限兜底。`@ApiPermissionIgnore` 继续遵循既有策略。
- [x] 旧接口绕过：`SysRoleServiceImpl#addUsersToRole` 不再写 `sys_user_role`，统一返回旧接口废弃错误，要求走组织角色授权。
- [x] 角色页加人：`UserSelectPanel` 增加 `directOrgOnly`，角色页锁定授权组织时只查询直接加入该组织的用户，避免选中子组织成员后保存失败。
- [x] 业务消息：按角色接收人支持从业务记录/表单/上下文解析 `activeOrgId/orgId/deptId/createDept`，并向查询传入业务 `tenantId + orgId`，Session 仅作为兜底。
- [x] 超级管理员组织切换：直接查询 `sys_user_org` 判断真实绑定关系；有显式绑定时，`/system/org/current/options` 只返回绑定组织，`/system/org/switch` 只允许切换绑定组织；仅无任何绑定的历史超级管理员账号保留全量组织兜底。
- [x] 验证：后端编译、前端构建、Flyway 占位符扫描和 `git diff --check` 已通过；服务级接口/数据库实测仍待可控本地 MySQL/Redis 环境。

## 前置条件

- [x] 用户确认 `spec.md` 作为本轮 `/apply` 执行依据。
- [x] 执行 `git status --short`，确认不在 `master` 分支直接编码，并识别已有用户改动。
- [x] Flyway 按实际迁移目录排序继续：当前最新为 `V1.0.0`，本变更脚本名固定为 `V1.0.1__add_org_scoped_role_permission.sql`。
- [x] 编码前读取 `code-copilot/rules/automated-testing-standard.md`、当前 `test-spec.md` 和 `execution-log.md`。

## Task 1: 数据库迁移与实体基础
> 状态：completed

- **目标**：新增组织角色授权和角色适用组织数据模型，并迁移历史全局角色数据。
- **涉及文件**：
  - 新增：`forge-server/db/migration/V1.0.1__add_org_scoped_role_permission.sql`
  - 新增：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysRoleOrg.java`
  - 新增：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysUserOrgRole.java`
  - 新增：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/mapper/SysRoleOrgMapper.java`
  - 新增：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/mapper/SysUserOrgRoleMapper.java`
- **关键签名**:
  ```java
  @TableName("sys_role_org")
  public class SysRoleOrg extends TenantEntity { }

  @TableName("sys_user_org_role")
  public class SysUserOrgRole extends TenantEntity { }

  public interface SysRoleOrgMapper extends BaseMapper<SysRoleOrg> { }

  public interface SysUserOrgRoleMapper extends BaseMapper<SysUserOrgRole> { }
  ```
- **执行步骤**：
  - [ ] 创建两个新表，字段包含 `id`, `tenant_id`, `role_id`, `org_id`, `user_id`（仅 `sys_user_org_role`）, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`。
  - [ ] 为 `sys_role_org` 增加 `uk_role_org(tenant_id, role_id, org_id)` 和 `idx_org_role(tenant_id, org_id, role_id)`。
  - [ ] 为 `sys_user_org_role` 增加 `uk_user_org_role(tenant_id, user_id, org_id, role_id)`, `idx_user_org(tenant_id, user_id, org_id)`, `idx_org_role(tenant_id, org_id, role_id)`。
  - [ ] 迁移角色适用组织：把每个数据中心内已有角色绑定到该数据中心下已有组织，使用 `WHERE NOT EXISTS` 防重复。
  - [ ] 迁移用户组织角色：把 `sys_user_role` 复制到该用户已绑定的所有组织，使用 `WHERE NOT EXISTS` 防重复。
  - [ ] 执行静态检查：`rg -n '\$\{[^}]+\}' forge-server/db/migration` 应无输出。

## Task 2: 登录态增加当前组织上下文
> 状态：completed

- **目标**：让 `LoginUser` 能表达当前组织，并支持在用户信息刷新时保留有效当前组织。
- **涉及文件**：
  - 修改：`forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/session/LoginUser.java`
  - 修改：`forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/session/SessionHelper.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/IUserLoadService.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImpl.java`
  - 修改：`forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/controller/AuthController.java`
- **关键签名**:
  ```java
  private Long activeOrgId;
  private String activeOrgName;

  public static Long getActiveOrgId();

  LoginUser loadUserByUserId(Long userId, Long tenantId, Long preferredActiveOrgId);
  LoginUser loadUserByUsername(String username, Long tenantId, Long preferredActiveOrgId);
  ```
- **执行步骤**：
  - [ ] `LoginUser` 新增 `activeOrgId`、`activeOrgName`。
  - [ ] `SessionHelper` 新增 `getActiveOrgId()` 和 `getActiveOrgName()`。
  - [ ] `IUserLoadService` 增加带 `preferredActiveOrgId` 的重载，旧方法委托到新方法。
  - [ ] `UserLoadServiceImpl#loadUserOrgs` 加载当前数据中心组织后，按“preferredActiveOrgId 有效 → 主组织 → 第一个组织”确定当前组织。
  - [ ] `UserLoadServiceImpl#loadUserRegion` 改为优先使用 `activeOrgId` 对应组织行政区划，再回退用户自身行政区划。
  - [ ] `AuthController#getUserInfo` 重建用户信息时传入旧 Session 的 `activeOrgId`，避免刷新覆盖组织切换结果。

## Task 3: 组织上下文角色加载
> 状态：completed

- **目标**：登录态的 `roleIds/roleKeys/permissions/apiPermissions` 按当前组织授权计算。
- **涉及文件**：
  - 修改：`UserLoadServiceImpl.java`
  - 新增：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserOrgRoleMapper.xml`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysResourceMapper.xml`
- **关键签名**:
  ```java
  private void loadUserRoles(LoginUser loginUser);

  List<Long> selectActiveRoleIdsByUserOrg(@Param("tenantId") Long tenantId,
                                          @Param("userId") Long userId,
                                          @Param("orgId") Long orgId);
  ```
- **执行步骤**：
  - [ ] 普通用户角色查询改为 `tenant_id + user_id + active_org_id` 查询 `sys_user_org_role`。
  - [ ] 查询角色时过滤 `role_status = 1`。
  - [ ] 查询角色时校验角色仍适用于当前组织：存在 `sys_role_org(tenant_id, role_id, activeOrgId)`。
  - [ ] 当前组织无角色时设置空 `roleIds/roleKeys/permissions/apiPermissions`，不得回退其他组织角色。
  - [ ] 系统管理员和数据中心管理员保持现有特权边界，但仍返回当前组织展示字段。
  - [ ] 确认 `loadUserPermissions` 和 `loadApiPermissions` 不再需要直接感知组织，只消费已经收窄后的 `roleIds`。

## Task 4: 当前组织切换接口
> 状态：completed

- **目标**：提供当前数据中心内组织切换能力，返回重算后的登录态。
- **涉及文件**：
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/SysOrgController.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/ISysOrgService.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysOrgServiceImpl.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysTenantServiceImpl.java`
- **关键签名**:
  ```java
  List<SysOrgTreeVO> selectCurrentUserOrgOptions();

  LoginUser switchCurrentOrg(Long orgId);
  ```
- **执行步骤**：
  - [ ] 新增 `GET /system/org/current/options`，只返回当前数据中心下当前用户已加入组织。
  - [ ] 新增 `POST /system/org/switch?orgId=`，校验组织属于当前数据中心且用户已加入。
  - [ ] 切换组织时调用 `IUserLoadService.loadUserByUserId(userId, tenantId, orgId)` 重建权限并写回 Session。
  - [ ] 切换组织不改变 `tenantId` 和 `TenantContextHolder`。
  - [ ] 修改 `SysTenantServiceImpl#switchTenant`，切换数据中心后不复用旧数据中心的 `activeOrgId`，按新数据中心默认组织重建。

## Task 5: 用户组织绑定联动组织角色清理
> 状态：completed

- **目标**：用户组织关系变更时保证组织角色授权不残留。
- **涉及文件**：
  - 修改：`SysUserServiceImpl.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/ISysUserService.java`
  - 新增：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/vo/UserOrgBindingVO.java`
  - 新增：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/vo/UserOrgRoleSummaryVO.java`
- **关键签名**:
  ```java
  List<UserOrgBindingVO> selectUserOrgBindings(Long userId, Long tenantId);

  boolean bindUserOrgs(Long userId, List<Long> orgIds, Long mainOrgId, Long requestedTenantId);
  ```
- **执行步骤**：
  - [ ] `bindUserOrgs` 删除组织关系前，先删除 `sys_user_org_role` 中该用户、该数据中心、被移除组织的授权。
  - [ ] `unbindUserOrg` 删除单个组织时同步删除对应组织角色授权。
  - [ ] `removeTenantUser` 和 `bindUserTenants` 移除数据中心成员时同步删除 `sys_user_org_role`。
  - [ ] 新增 `GET /system/user/{userId}/org-bindings` 返回组织名称、是否主组织、角色数量和角色名称摘要。
  - [ ] 当前登录用户组织关系变化时，同步更新 Session 中 `orgIds/mainOrgId/activeOrgId/activeOrgName`。

## Task 6: 用户组织角色授权接口
> 状态：completed

- **目标**：提供按组织授权用户角色的主接口，替代旧全局用户角色授权入口。
- **涉及文件**：
  - 修改：`SysUserController.java`
  - 修改：`ISysUserService.java`
  - 修改：`SysUserServiceImpl.java`
  - 新增：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/dto/UserOrgRoleBindDTO.java`
- **关键签名**:
  ```java
  List<Long> selectUserOrgRoleIds(Long userId, Long orgId, Long tenantId);

  boolean bindUserOrgRoles(Long userId, Long orgId, List<Long> roleIds, Long tenantId);
  ```
- **执行步骤**：
  - [ ] 新增 `GET /system/user/{userId}/org-roles?orgId=&tenantId=`。
  - [ ] 新增 `POST /system/user/{userId}/org-roles`，请求体包含 `orgId`、`roleIds`。
  - [ ] 校验用户属于目标数据中心和目标组织。
  - [ ] 校验角色属于目标数据中心、启用、适用于目标组织。
  - [ ] 普通组织管理员只能分配自己当前组织拥有且数据范围不越级的角色。
  - [ ] 旧 `/system/user/{userId}/roles` 保留兼容，不作为新前端主入口。

## Task 7: 角色适用组织配置
> 状态：completed

- **目标**：角色管理支持维护适用组织范围，并限制角色可见和可分配范围。
- **涉及文件**：
  - 修改：`SysRoleController.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/ISysRoleService.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysRoleServiceImpl.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/dto/SysRoleDTO.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysRoleMapper.xml`
- **关键签名**:
  ```java
  List<Long> selectRoleOrgIds(Long roleId);

  boolean bindRoleOrgs(Long roleId, List<Long> orgIds);
  ```
- **执行步骤**：
  - [ ] `SysRoleDTO` 增加 `List<Long> orgIds`，用于角色新增/编辑时可选保存适用组织。
  - [ ] 新增 `GET /system/role/{roleId}/orgs` 和 `POST /system/role/{roleId}/orgs`。
  - [ ] 保存适用组织时校验组织属于角色数据中心。
  - [ ] 角色适用组织被移除时，删除该角色在被移除组织下的 `sys_user_org_role` 授权。
  - [ ] `selectRoleUsers` 支持可选 `orgId`，按 `sys_user_org_role` 查询用户。
  - [ ] `countUsersByRole` 改为统计 `sys_user_org_role`，防止删除仍被组织授权使用的角色。

## Task 8: 数据权限主链路收窄
> 状态：completed

- **目标**：MyBatis 数据权限和低代码动态数据权限都按当前组织执行。
- **涉及文件**：
  - 修改：`forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/context/DataScopeContext.java`
  - 修改：`forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/service/impl/DataScopeServiceImpl.java`
  - 修改：`forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/java/com/mdframe/forge/starter/datascope/handler/DataScopeInterceptor.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicDataScopeService.java`
- **关键签名**:
  ```java
  private Long activeOrgId;

  public DataScopeContext getCurrentUserDataScope();
  ```
- **执行步骤**：
  - [ ] `DataScopeContext` 增加 `activeOrgId`，保留 `orgIds` 兼容现有 SQL 占位符。
  - [ ] `DataScopeServiceImpl` 普通用户上下文设置 `activeOrgId`，并把 `orgIds` 设置为只包含当前组织的单元素列表。
  - [ ] `ORG` 使用当前组织；当前组织为空时强制无数据。
  - [ ] `ORG_AND_CHILD` 只展开当前组织及子组织。
  - [ ] `CUSTOM` 只基于当前组织有效角色的自定义组织范围。
  - [ ] `DynamicDataScopeService` 复用收窄后的 `DataScopeContext`，不再自行扩大到全部组织。

## Task 9: 数据集、消息和流程旁路适配
> 状态：completed

- **目标**：消除直接读取全局 `sys_user_role` 或全部组织导致的旁路串权。
- **涉及文件**：
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/impl/DataDatasetAccessServiceImpl.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/impl/DataDatasetRowScopeServiceImpl.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessMessageChannelMapper.xml`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessMessageChannelService.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowInstanceServiceImpl.java`
  - 修改：`forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowOrgIntegrationServiceImpl.java`
- **关键签名**:
  ```java
  List<Long> selectUserIdsByRoleIds(Long tenantId, Long orgId, List<Long> roleIds);

  List<Long> selectUserOrgRoleIds(Long userId, Long orgId, Long tenantId);
  ```
- **执行步骤**：
  - [ ] 数据集 ACL 使用当前组织和当前组织角色匹配 `ROLE/ORG` 主体。
  - [ ] 数据集行级权限的 `ORG_AND_CHILD` 只展开当前组织。
  - [ ] 消息按角色接收人查询改为 `sys_user_org_role`，并传入当前组织或业务明确组织。
  - [ ] 流程启动变量 `startUserRoleIds` 改为当前组织角色。
  - [ ] 流程启动变量保留 `startUserOrgIds` 兼容旧表达式，同时新增 `startUserActiveOrgId`。
  - [ ] 流程角色判断必须带 `tenantId + activeOrgId`，不能只按 `userId` 查询所有角色。

## Task 10: 前端状态和当前组织切换器
> 状态：completed

- **目标**：前端展示当前组织并在切换后刷新权限、菜单和路由缓存。
- **涉及文件**：
  - 修改：`forge-admin-ui/src/store/helper.js`
  - 修改：`forge-admin-ui/src/store/modules/user.js`
  - 修改：`forge-admin-ui/src/api/index.js`
  - 新增：`forge-admin-ui/src/layouts/components/OrgSwitcher.vue`
  - 修改：`forge-admin-ui/src/layouts/components/index.js`
  - 修改：`forge-admin-ui/src/layouts/normal/header/index.vue`
  - 修改：`forge-admin-ui/src/layouts/immersive/header/index.vue`
  - 修改：`forge-admin-ui/src/layouts/top-menu/index.vue`
  - 修改：`forge-admin-ui/src/layouts/top-side-menu/index.vue`
  - 修改：`forge-admin-ui/src/layouts/full/header/index.vue`
  - 修改：`forge-admin-ui/src/layouts/nexus/header/index.vue`
- **关键签名**:
  ```js
  getCurrentOrgOptions: () => request.get('/system/org/current/options')
  switchOrg: orgId => request.post('/system/org/switch', null, { params: { orgId } })
  ```
- **执行步骤**：
  - [ ] `helper.js` 映射 `activeOrgId/activeOrgName`。
  - [ ] `user.js` 增加 `activeOrgId`、`activeOrgName` getters。
  - [ ] 新增 `OrgSwitcher.vue`，单组织用户隐藏或禁用，多组织用户可切换。
  - [ ] 组织切换成功后调用 `authStore.resetLoginState({ resetAuth: false })`，保留 token。
  - [ ] 组织切换后重新进入首页并触发菜单重新加载。
  - [ ] 顶部同时出现数据中心和当前组织时，文案明确区分“数据中心”和“当前组织”。

## Task 11: 前端用户和角色管理适配
> 状态：completed

- **目标**：用户管理支持多组织绑定和组织内角色授权，角色管理支持适用组织配置。
- **涉及文件**：
  - 修改：`forge-admin-ui/src/views/system/user.vue`
  - 修改：`forge-admin-ui/src/views/system/role.vue`
  - 新增：`forge-admin-ui/src/views/system/components/UserOrgRolePanel.vue`
  - 新增：`forge-admin-ui/src/views/system/components/RoleOrgScopePanel.vue`
- **关键接口**:
  ```text
  GET  /system/user/{userId}/org-bindings
  GET  /system/user/{userId}/org-roles?orgId=
  POST /system/user/{userId}/org-roles
  GET  /system/role/{roleId}/orgs
  POST /system/role/{roleId}/orgs
  ```
- **执行步骤**：
  - [ ] 用户组织弹窗从单选改为多选，并提供主组织选择。
  - [ ] 用户组织弹窗保存 `orgIds + mainOrgId`，重新打开能回显多个组织。
  - [ ] 用户角色授权弹窗增加组织选择，按组织加载可分配角色和已授权角色。
  - [ ] 用户新增时普通组织管理员默认绑定当前组织，默认角色授权写入当前组织。
  - [ ] 用户编辑表单中的全局 `roleIds` 不再作为新主入口，避免误导。
  - [ ] 角色管理新增适用组织入口，保存后角色只能分配给适用组织。
  - [ ] 前端仍保留旧接口兼容显示，但新操作走组织角色接口。

## Task 12: 验证、审查和文档回填
> 状态：partially completed（静态检查、编译、构建已通过；服务级接口/数据库实测待补）

- **目标**：按自动化测试标准完成权限改造的最小闭环验证。
- **涉及文件**：
  - 修改：`code-copilot/changes/org-scoped-role-permission/test-spec.md`
  - 修改：`code-copilot/changes/org-scoped-role-permission/execution-log.md`
  - 修改：`code-copilot/changes/org-scoped-role-permission/spec.md`
  - 修改：`code-copilot/changes/org-scoped-role-permission/tasks.md`
- **执行步骤**：
  - [x] 执行 `git diff --check`。
  - [x] 执行后端编译：`mvn -pl forge-admin-server -am compile -DskipTests`。
- [ ] 执行数据权限模块定向测试或补充测试后运行对应 Maven test。（本轮未补单测，记录为待补）
- [x] 执行前端构建：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`。
- [x] 补充检查自动填充链路：`create_dept` 优先使用 `LoginUser.activeOrgId`，避免多组织下新数据落到非当前组织。
- [ ] 启动后端后通过接口验证登录、切组织、菜单、API 权限、`ORG` 和 `ORG_AND_CHILD` 数据权限。（本轮未启动服务和数据库；本地 3407 MySQL 未运行，3306 root 无密码访问被拒，Redis 探测受沙箱限制且提权未获批）
- [x] 把命令、结果、警告、跳过项和服务清理情况追加到 `execution-log.md`。
- [x] 回填 `tasks.md` 任务状态和 `spec.md` 审查结论。
