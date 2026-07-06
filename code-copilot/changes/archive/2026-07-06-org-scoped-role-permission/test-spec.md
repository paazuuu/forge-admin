# 单测 Spec — 组织上下文角色权限改造
> status: done
> created: 2026-07-06

## 0. 测试原则

- **Red/Green TDD**：权限计算、组织切换和数据权限必须先写能失败的测试，再实现通过。
- **First Run the Tests**：开始前先跑现有最小相关测试或编译，记录基线。
- **展示工作**：执行后必须在 `execution-log.md` 记录命令、关键输出、失败原因或通过证据。
- **增量复用**：每次验证前先读取本文件、`execution-log.md`、`spec.md`、`tasks.md` 和 `code-copilot/rules/automated-testing-standard.md`。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| 后端测试 | JUnit 5 / Spring Boot Test / Mockito（按现有模块测试依赖为准） |
| 前端测试 | 以 `pnpm build` 为 P0；如现有 Vitest 覆盖相关组件则补定向用例 |
| 数据库验证 | Flyway 静态检查 + 可用 MySQL 环境下接口/SQL 验证 |
| 权限验证 | 后端服务测试 + 登录后接口验证 |

## 2. 覆盖范围

### P0 — 核心权限计算（必须覆盖）

#### 类名: `UserLoadServiceImpl`

| 方法 | 场景 | 输入 | Mock/数据准备 | 预期结果 |
|------|------|------|---------------|----------|
| `loadUserByUserId(userId, tenantId, preferredActiveOrgId)` | 用户在组织 A/B 分别拥有不同角色，preferred 为 A | `tenantId=T1`, `activeOrgId=A` | `sys_user_org` 有 A/B，`sys_user_org_role` A=R1、B=R2 | `LoginUser.activeOrgId=A`，`roleIds=[R1]`，不包含 R2 |
| 同上 | preferred 不属于用户组织 | `activeOrgId=X` | 用户组织只有 A/B，主组织 B | 当前组织回退 B |
| 同上 | 当前组织无角色 | `activeOrgId=A` | A 无 `sys_user_org_role` | `roleIds/permissions/apiPermissions` 为空，不能继承 B |
| `loadUserByUsername(username, tenantId, preferredActiveOrgId)` | `/auth/userInfo` 刷新保留当前组织 | Session 原 `activeOrgId=A` | A 仍有效 | 刷新后仍是 A |

#### 类名: `SysOrgServiceImpl`

| 方法 | 场景 | 输入 | Mock/数据准备 | 预期结果 |
|------|------|------|---------------|----------|
| `switchCurrentOrg` | 切到当前数据中心已加入组织 | `orgId=A` | 用户属于 A | 返回重算后 `LoginUser`，Session 写入 A |
| `switchCurrentOrg` | 切到未加入组织 | `orgId=X` | 用户不属于 X | 抛业务异常 |
| `switchCurrentOrg` | 切到其他数据中心组织 | `orgId=OtherTenantOrg` | 组织 `tenant_id != currentTenantId` | 抛业务异常 |

#### 类名: `SysUserServiceImpl`

| 方法 | 场景 | 输入 | Mock/数据准备 | 预期结果 |
|------|------|------|---------------|----------|
| `bindUserOrgRoles` | 给组织成员授权适用角色 | `userId=U`, `orgId=A`, `roleIds=[R1]` | U 属于 A，R1 适用于 A | 写入 `sys_user_org_role` |
| `bindUserOrgRoles` | 角色不适用于目标组织 | `orgId=B`, `roleIds=[R1]` | R1 只适用于 A | 拒绝授权 |
| `bindUserOrgs` | 移除组织 | 原 A/B，保存 A | B 下存在角色授权 | 删除 B 的 `sys_user_org_role` |

### P0 — 数据权限（必须覆盖）

#### 类名: `DataScopeServiceImpl`

| 方法 | 场景 | 输入 | Mock/数据准备 | 预期结果 |
|------|------|------|---------------|----------|
| `getCurrentUserDataScope` | `ORG` 数据范围 | `activeOrgId=A`, `orgIds=[A,B]` | 当前角色数据范围 `ORG` | `context.orgIds=[A]` |
| 同上 | `ORG_AND_CHILD` 数据范围 | `activeOrgId=A` | A 子组织 A1/A2 | 拦截器使用 A/A1/A2，不含 B |
| 同上 | 无当前组织 | `activeOrgId=null` | 普通用户角色为 `ORG` | 后续 SQL 条件强制无数据 |

#### 类名: `DynamicDataScopeService`

| 方法 | 场景 | 输入 | Mock/数据准备 | 预期结果 |
|------|------|------|---------------|----------|
| `buildCondition` | 低代码 FOLLOW_SYSTEM + ORG | `context.orgIds=[A]` | 模型配置 `orgColumn=org_id` | SQL 条件只包含 A |
| `buildCondition` | ORG_AND_CHILD | `activeOrgId=A` | A 子组织 A1 | SQL 条件包含 A/A1，不含 B |

### P1 — 数据访问层

| Mapper | 场景 | 输入 | 预期结果 |
|--------|------|------|----------|
| `SysUserOrgRoleMapper` | 按用户当前组织查启用且适用角色 | `tenantId,userId,orgId` | 只返回该组织有效角色 |
| `SysRoleMapper.selectRoleUsers` | 按角色和组织查询用户 | `roleId,orgId` | 只返回该组织授权用户 |
| `BusinessMessageChannelMapper.selectUserIdsByRoleIds` | 按当前组织角色找用户 | `tenantId,orgId,roleIds` | 不返回其他组织用户 |

### P1 — 入口层/接口

| 接口 | 场景 | 请求 | 预期结果 |
|------|------|------|----------|
| `POST /system/org/switch` | 用户切换到组织 A | `orgId=A` | 返回 `activeOrgId=A`，权限为 A 的角色 |
| `GET /system/org/current/options` | 查询可切组织 | 当前登录用户 | 只返回当前数据中心已加入组织 |
| `POST /system/user/{id}/org-roles` | 保存组织角色 | `orgId + roleIds` | 成功后查询回显一致 |
| `POST /system/role/{id}/orgs` | 保存角色适用组织 | `orgIds` | 成功后不适用组织不能分配 |

### P1 — 前端构建和交互

| 页面/组件 | 场景 | 验证 |
|-----------|------|------|
| `OrgSwitcher.vue` | 多组织用户切换组织 | 切换后清理菜单缓存并重新加载首页 |
| `system/user.vue` | 用户绑定多个组织和主组织 | 保存后列表展示多个组织，重新打开回显 |
| `system/user.vue` | 用户在组织内授权角色 | 选择组织 A/B 分别回显不同角色 |
| `system/role.vue` | 角色适用组织配置 | 保存后可回显，并限制用户授权角色列表 |

### P2 — 旁路回归

| 模块 | 场景 | 预期结果 |
|------|------|----------|
| 数据集 ACL | ACL 按 ROLE/ORG 授权 | 只匹配当前组织和当前组织角色 |
| 流程启动变量 | 发起流程时注入角色变量 | `startUserRoleIds` 为当前组织角色 |
| 业务消息 | 角色接收人 | 只发给当前组织或业务指定组织内拥有该角色的用户 |

### 不测试（明确列出原因）

- 不做跨登录“上次组织”持久化测试：本期明确不新增跨登录持久化。
- 不做全量 E2E 回归：本变更风险集中在权限链路，按自动化测试标准优先做最小闭环和关键接口验证。
- 不在无数据库环境强制执行 Flyway 实跑：如果本地没有 MySQL，只记录静态检查和跳过原因。

## 3. 执行计划

- [ ] Step 1: 运行基线检查：`git diff --check`。
- [ ] Step 2: 执行后端最小编译：`mvn -pl forge-admin-server -am compile -DskipTests`。
- [ ] Step 3: 为 `UserLoadServiceImpl`、`SysOrgServiceImpl`、`SysUserServiceImpl`、`DataScopeServiceImpl` 编写 P0 测试，先确认 Red。
- [ ] Step 4: 实现对应功能后确认 P0 测试 Green。
- [ ] Step 5: 执行 Mapper XML 和接口级验证。
- [ ] Step 6: 执行前端构建：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`。
- [ ] Step 7: 启动后端并用真实登录 Token 验证切组织、菜单、API 权限和数据权限。
- [ ] Step 8: 更新 `execution-log.md`、`tasks.md` 和 `spec.md`。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-06 | 建档 | 未执行代码验证 | 未执行 | 本轮仅生成 SDD 文档 |
| 2026-07-06 | Apply 增量验证 | 后端编译 + 前端构建 + 静态检查 | 通过 | 未启动服务/数据库做接口实测 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-06 | SDD 文档生成 | 文档结构检查 | `git diff --check -- code-copilot/changes/org-scoped-role-permission` | 通过 | 建档阶段轻量检查 |
| 2026-07-06 | 组织上下文权限改造 | 后端最小编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests` | 通过 | 仅既有 deprecation/unchecked/Lombok 警告 |
| 2026-07-06 | 前端组织切换/用户角色/角色组织范围 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过 | 仅既有 chunk/CSS/组件命名警告 |
| 2026-07-06 | Flyway 脚本 | 占位符静态扫描 | `rg -n '\$\{[^}]+\}' forge-server/db/migration` | 通过 | 无输出 |
| 2026-07-06 | 全量 diff | 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-06 | 当前组织写入链路 | 自动填充修正后后端最小编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests` | 通过 | `create_dept` 改为优先写 `activeOrgId` 后复跑；仅既有 deprecation/unchecked/Lombok 警告 |
| 2026-07-06 | 服务级验证准备 | 本地 MySQL/Redis 可用性探测 | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping`; `mysqladmin --protocol=tcp -h127.0.0.1 -P3306 -uroot ping`; `redis-cli -h 127.0.0.1 -p 6379 ping` | 跳过接口实测 | 3407 MySQL 未运行；3306 root 无密码访问被拒；Redis 探测受沙箱限制且提权未获批。未启动后端，未写数据库 |
| 2026-07-06 | Flyway/全量 diff | 占位符扫描 + 空白检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration`; `git diff --check` | 通过 | Flyway 扫描无输出；diff 检查无输出 |
| 2026-07-06 | Review 修复：服务权限兜底、旧角色加人阻断、角色加人直接组织筛选、消息业务组织上下文 | 后端最小编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests` | 通过 | Reactor `BUILD SUCCESS`；仅既有 deprecation/unchecked 警告 |
| 2026-07-06 | Review 修复：前端角色用户选择直接组织筛选 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过 | Node v20.19.0 可用；仅既有 CSS `//` 注释、动态/静态导入和组件命名冲突提示 |
| 2026-07-06 | Review 修复后静态检查 | 占位符扫描 + 空白检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration`; `git diff --check` | 通过 | Flyway 扫描无输出；diff 检查无输出 |
| 2026-07-06 | Review 修复后服务级验证 | 接口/数据库实测 | 未启动后端服务 | 跳过 | 本轮未引入新 Flyway；当前 dev 库仍不适合作为自动验证目标，服务级真实登录/组织授权/消息落库待可控本地 MySQL/Redis 环境补跑 |
| 2026-07-06 | 超级管理员组织选项边界修复 | 后端最小编译 + 空白检查 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests`; `git diff --check` | 通过 | 超管有组织绑定时只返回/允许切换绑定组织；仅无绑定超管保留全量兜底。未改前端，未重复跑前端构建 |
| 2026-07-06 | 超级管理员显式组织绑定边界二次修正 | 后端最小编译 + Flyway 占位符扫描 + 空白检查 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests`; `rg -n '\$\{[^}]+\}' forge-server/db/migration`; `git diff --check` | 通过 | options/switch 直接查询 `sys_user_org` 区分真实绑定和历史超管全量兜底；本轮未改前端，扁平下拉展示不变 |
| 2026-07-06 | 超级管理员组织边界 Mapper XML 收敛 | 后端最小编译 + Flyway 占位符扫描 + 空白检查 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests`; `rg -n '\$\{[^}]+\}' forge-server/db/migration`; `git diff --check` | 通过 | 新增 `SysUserOrgMapper.xml` 和 `SysOrgMapper.selectEnabledOrgsByIds`，显式绑定查询不在 Service 层拼接；本轮未改前端 |

## 6. 执行证据

- `execution-log.md`：记录每次验证命令、结果、跳过项、服务启动和清理情况。
- 关键接口：`/auth/login`、`/auth/userInfo`、`/auth/current/menu`、`/system/org/switch`、`/system/user/{id}/org-roles`。
- 关键数据库检查：`sys_role_org`、`sys_user_org_role`、`sys_user_org`、`sys_role`、`sys_role_resource`。
- 服务启动与停止：只停止本轮启动的后端、前端或测试服务。
