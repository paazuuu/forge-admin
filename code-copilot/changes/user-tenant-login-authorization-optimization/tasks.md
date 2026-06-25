# 任务拆分 — 用户租户登录与授权优化
> 拆分顺序：接口协议 → 底层实现 → 前端入口 → 验证

## 前置条件
- [x] 确认现有 `sys_user_tenant` 支持用户多租户关系。
- [x] 确认登录请求 `LoginRequest.tenantId` 已存在。

## Task 1: 登录租户配置公开接口
- [x] 已完成
- **目标**: 登录前可选择租户，并按所选租户返回登录页展示配置；租户 logo/favicon 通过白名单公开图片接口渲染。
- **涉及文件**:
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/domain/LoginTenantOption.java` — 新增租户选项响应对象。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/domain/LoginConfigResult.java` — 增加租户展示字段。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/service/IAuthService.java` — 增加租户配置接口签名。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/controller/AuthController.java` — 暴露 `/auth/tenant/options` 和 `tenantId` 参数。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SystemAuthServiceImpl.java` — 查询启用租户并合并登录配置。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/LoginTenantAssetController.java` — 只允许读取启用租户配置中引用的 logo/favicon 文件。
- **关键签名**:
  ```java
  LoginConfigResult getLoginConfig(String userClient, Long tenantId);
  List<LoginTenantOption> listLoginTenantOptions();
  ```

## Task 2: 用户批量授权与批量加入租户
- [x] 已完成
- **目标**: 用户管理支持对选中用户批量赋角色、批量加入租户。
- **涉及文件**:
  - `BatchUserRoleBindDTO.java` — 新增用户批量角色绑定 DTO。
  - `BatchUserTenantBindDTO.java` — 新增用户批量租户绑定 DTO。
  - `ISysUserService.java` — 增加批量方法。
  - `SysUserServiceImpl.java` — 复用 `bindUserRoles`、`upsertUserTenant` 实现批量逻辑。
  - `SysUserController.java` — 新增 `/batch/roles`、`/batch/tenants`。
- **关键签名**:
  ```java
  boolean batchBindUserRoles(BatchUserRoleBindDTO dto);
  boolean batchBindUserTenant(BatchUserTenantBindDTO dto);
  ```

## Task 3: 前端登录页租户选择
- [x] 已完成
- **目标**: 登录页加载租户列表、切换租户时刷新登录配置和三方登录配置，登录请求提交 `tenantId`。
- **涉及文件**:
  - `forge-admin-ui/src/views/login/api.js`
  - `forge-admin-ui/src/views/login/index.vue`
- **关键行为**:
  - `onMounted` 先加载租户选项，再加载当前租户配置。
  - `watch(selectedTenantId)` 刷新登录配置、验证码和三方平台。
  - 登录参数追加 `tenantId`。

## Task 4: 前端用户管理批量操作和回显修复
- [x] 已完成
- **目标**: 用户管理页增加批量授权/加入租户按钮，支持 `tenantIds` 多选、`tenantId` 默认租户单选、列表多租户名称展示和用户类型中文回显。
- **涉及文件**:
  - `forge-admin-ui/src/views/system/user.vue`
- **关键行为**:
  - 工具栏读取 `crudRef.getSelectedKeys()`。
  - 批量授权弹窗选择目标租户和角色。
  - 批量加入租户弹窗选择目标租户和成员类型。
  - `beforeRenderUserForm` / `beforeRenderUserDetail` 规范化 `tenantIds`、`tenantId`、`userType`。
  - `tenantIds` 变更时自动校正默认租户，默认租户选项只来自已选租户。
  - 列表 `tenantName` 按多个租户标签渲染。

## Task 5: 验证与记录
- [x] 已完成
- **目标**: 按自动化测试标准记录命令和结果。
- **涉及文件**:
  - `code-copilot/changes/user-tenant-login-authorization-optimization/test-spec.md`
  - `code-copilot/changes/user-tenant-login-authorization-optimization/execution-log.md`
- **关键命令**:
  ```bash
  mvn -q -pl forge-framework/forge-plugin-parent/forge-plugin-system -am compile -DskipTests
  pnpm --dir forge-admin-ui build
  ```
