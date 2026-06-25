# 用户租户登录与授权优化
> status: apply
> created: 2026-06-25
> complexity: 🟡中等

## 1. 背景与目标
用户管理当前支持单用户授权和单用户租户绑定，但缺少批量操作入口；登录页不能在登录前选择租户并应用对应租户展示配置；用户编辑表单需要支持“所属租户”多选并保持“默认租户”为单值；用户类型在部分回显场景显示数字；登录页未登录态无法渲染租户自定义文件 logo。完成后应支持在用户管理批量给用户分配角色、批量加入目标租户，登录前选择租户并按所选租户渲染登录页配置，用户列表展示多个租户名称，用户编辑表单字段稳定回显中文。

## 2. 代码现状（Research Findings）
### 2.1 相关入口与链路
- `forge-admin-ui/src/views/system/user.vue` 使用 `AiCrudPage` 渲染用户管理，已有单用户授权弹窗、单用户租户绑定弹窗。
- `forge-server/.../SysUserController.java` 已有 `POST /system/user/{userId}/roles` 和 `POST /system/user/{userId}/tenants` 单用户接口。
- `forge-server/.../SysUserServiceImpl.java` 已有 `syncUserRoles`、`bindUserTenants`、`upsertUserTenant` 等复用能力。
- `forge-admin-ui/src/views/login/index.vue` 登录页只按 `userClient` 加载 `/auth/loginConfig`，未传租户。
- `forge-server/.../AuthController.java` 的 `GET /auth/loginConfig` 仅接收 `userClient`。

### 2.2 现有实现
- `LoginRequest` 已包含 `tenantId`，认证策略 `UsernamePasswordAuthStrategy`、`UsernamePasswordCaptchaAuthStrategy` 已把 `request.getTenantId()` 传给用户加载服务。
- `sys_user_tenant` 已由 `V1.0.55__add_user_tenant_membership.sql` 建立，具备用户多租户绑定模型。
- `AiCrudPage` 暴露 `getSelectedKeys()` 和 `clearSelection()`，可用于用户页批量操作。

### 2.3 发现与风险
- 批量授权必须复用现有角色可管理范围校验，避免租户管理员越权。
- 登录页租户配置读取不能依赖需要登录态的 `/system/tenant/userTenantConfig`。
- 用户多租户关系存在时，编辑表单仍只能编辑默认/当前上下文租户，不能把 `tenantIds` 数组塞给 `tenantId` 单选字段。

## 3. 功能点
- [x] 用户管理选中多名用户后，可批量分配当前操作租户下角色。
- [x] 用户管理选中多名用户后，可批量加入一个目标租户，并指定成员类型。
- [x] 登录页加载公开启用租户列表，登录前选择租户，登录配置和展示配置按所选租户生效。
- [x] 用户编辑时 `tenantIds` 支持多选，`tenantId` 作为默认租户保持单值；用户类型按字典中文回显。
- [x] 用户列表租户名称展示多个租户。
- [x] 登录页租户 logo/favicon 使用白名单公开图片接口渲染，不放开通用文件下载权限。

## 4. 业务规则
- 批量授权不允许操作当前登录用户。
- 超级管理员可批量加入任意启用租户；非超级管理员仍只能操作当前租户内普通用户。
- 批量授权的角色必须属于操作租户，非超级管理员只能分配自己拥有的角色。
- 登录页租户列表仅返回启用租户，不暴露敏感字段。

## 5. 数据变更
| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 无 | - | - | 复用 `sys_user_tenant`、`sys_user_role` |

## 6. 接口变更
| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 新增 | `/auth/tenant/options` | GET | 公开返回启用租户选项 |
| 修改 | `/auth/loginConfig` | GET | 增加可选 `tenantId`，返回租户展示配置 |
| 新增 | `/system/user/batch/roles` | POST | 批量给用户绑定角色 |
| 新增 | `/system/user/batch/tenants` | POST | 批量把用户加入目标租户 |
| 新增 | `/auth/tenant/assets/{tenantId}/{assetType}` | GET | 登录页只读访问启用租户配置中引用的 logo/favicon |

## 7. 影响范围
- 登录页品牌展示、验证码加载、登录参数。
- 用户管理页工具栏、授权弹窗、租户绑定弹窗。
- 系统用户服务、认证服务接口。

## 8. 风险与关注点
> ⚠️ 涉及权限变更。必须保留现有角色租户校验、当前用户自操作保护和租户管理员权限边界。

## 8.5 测试策略
- **测试范围**：系统插件编译、前端构建、关键 XML/接口签名检查。
- **覆盖率目标**：本轮以编译与构建验证为主，不新增持久化表结构。
- **独立 Test Spec**：是。

## 9. 待澄清
- [x] 无阻塞项，按现有 `sys_user_tenant` 多租户模型实现。

## 10. 技术决策
- 新增批量接口只做薄封装，内部复用现有单用户绑定逻辑，减少权限规则分叉。
- 登录页新增 `/auth/tenant/options`，避免未登录态调用 `/system/tenant/*`。

## 11. 执行日志
| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 1 | done | `AuthController.java`, `IAuthService.java`, `LoginConfigResult.java`, `LoginTenantOption.java`, `SystemAuthServiceImpl.java`, `SaTokenConfig.java` | 新增登录租户选项和按租户读取登录配置 |
| Task 2 | done | `SysUserController.java`, `ISysUserService.java`, `SysUserServiceImpl.java`, `BatchUserRoleBindDTO.java`, `BatchUserTenantBindDTO.java` | 批量授权采用追加语义，批量加入租户仅超级管理员可操作 |
| Task 3 | done | `login/api.js`, `login/index.vue`, `login/callback.vue`, `tenant-config.js`, `crypto-config.js` | 登录前选择租户并刷新验证码、三方登录和租户展示配置 |
| Task 4 | done | `system/user.vue` | 用户批量操作入口、租户单选回显、用户类型字典回显 |
| Task 5 | done | `test-spec.md`, `execution-log.md` | 后端模块编译和前端生产构建通过 |
| Follow-up | done | `LoginTenantAssetController.java`, `tenant-config.js`, `login/index.vue`, `system/user.vue`, `SysUserMapper.xml`, `SysUserServiceImpl.java` | 编辑所属租户改为多选、列表聚合多租户名、登录 logo/favicon 走安全白名单图片接口 |

## 12. 审查结论

## 13. 确认记录（HARD-GATE）
- **确认时间**：2026-06-25
- **确认人**：用户直接提出优化需求
