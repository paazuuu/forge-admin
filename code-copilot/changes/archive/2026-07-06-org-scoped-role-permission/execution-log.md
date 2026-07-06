# 变更日志 — 组织上下文角色权限改造

> 记录决策、踩坑和知识发现。知识飞轮的输入。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-07-06 | propose | 根据 `code-copilot/changes/spec(2).md` 和当前代码影响面重新生成 SDD 文档 | 生成 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md` |
| 2026-07-06 | verify | 执行文档轻量检查 | `git diff --check -- code-copilot/changes/org-scoped-role-permission` 无输出；`rg -n "TBD|TODO|V<next>|可选新增|待定|占位"` 仅命中正常的“SQL 占位符”说明 |
| 2026-07-06 | apply | 实施组织上下文角色权限改造 | 新增组织角色授权表、当前组织登录态、组织切换接口、用户组织角色授权、角色适用组织、数据权限收窄、消息/流程/数据集旁路适配、前端组织切换器和用户/角色页面适配 |
| 2026-07-06 | verify | 执行增量验证 | 后端编译、前端构建、Flyway 占位符扫描、`git diff --check` 均通过；未启动服务和数据库做接口实测 |
| 2026-07-06 | apply | 补充当前组织写入链路 | `InjectionMetaObjectHandler` 自动填充 `create_dept` 改为优先使用 `LoginUser.activeOrgId`，避免多组织用户新建数据落到第一个绑定组织 |
| 2026-07-06 | verify | 复跑后端编译和静态检查 | 后端编译、Flyway 占位符扫描、`git diff --check` 通过；本地服务级验证仍因 MySQL/Redis 环境不可用跳过 |
| 2026-07-06 | fix | 修复 review 问题 | 补服务层权限兜底；阻断旧 `/system/role/{roleId}/addUsers` 写旧表；角色页加人只查询授权组织直接成员；消息按角色接收人支持业务 `tenantId + orgId` 上下文 |
| 2026-07-06 | verify | 执行 review 修复后增量验证 | 后端编译、前端构建、Flyway 占位符扫描、`git diff --check` 通过；未启动后端和数据库做接口实测 |
| 2026-07-06 | fix | 修复超级管理员组织选项显示全部组织 | `SysOrgServiceImpl#selectCurrentUserOrgOptions` 改为有绑定组织时只返回绑定组织；`switchCurrentOrg` 同步限制有绑定组织的超级管理员只能切换绑定组织；无绑定历史超管保留全量兜底 |
| 2026-07-06 | verify | 执行超级管理员组织选项边界修复验证 | 后端编译和 `git diff --check` 通过；本轮未改前端，未重复跑前端构建 |
| 2026-07-06 | fix | 细化超级管理员组织绑定判断 | `SysOrgServiceImpl` 不再依赖登录态 `orgIds` 判断真实绑定，改为直接查询 `sys_user_org`，避免历史超管全量兜底 `orgIds` 被误当成真实绑定 |
| 2026-07-06 | verify | 执行显式组织绑定边界二次验证 | 后端编译、Flyway 占位符扫描和 `git diff --check` 通过；本轮未改前端，未启动服务写库 |
| 2026-07-06 | fix | 收敛组织绑定查询到 Mapper XML | 新增 `SysUserOrgMapper.xml` 查询当前用户显式绑定组织；新增 `SysOrgMapper.selectEnabledOrgsByIds` 查询启用组织，服务层不再新增直接查询拼装 |
| 2026-07-06 | verify | 执行 Mapper XML 收敛后验证 | 后端编译、Flyway 占位符扫描和 `git diff --check` 通过；本轮未改前端，未启动服务写库 |
| 2026-07-06 | archive | 归档前知识沉淀 | 新增 `tech-org-scoped-role-permission.md`，更新 `knowledge/index.md`，补充 `memory/decisions.md` 第 15 条和 `memory/pitfalls.md` 第 102 条 |
| 2026-07-06 | archive | 归档状态回填 | `spec.md` 状态改为 done，补充归档路径、知识沉淀和服务级验证缺口说明；`tasks.md`、`test-spec.md` 同步归档状态 |
| 2026-07-06 | archive | 移动变更目录 | 已移动到 `code-copilot/changes/archive/2026-07-06-org-scoped-role-permission/` |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
|------|------|------------|------|
| 用户角色授权模型 | 新增 `sys_user_org_role` | 给 `sys_user_role` 加 `org_id` | 保留旧接口和迁移窗口，降低破坏面 |
| 角色组织范围 | 新增 `sys_role_org` | 每个组织复制一份角色 | 角色仍作为数据中心内权限模板，资源绑定复用 |
| 当前组织状态 | Session 态 `activeOrgId` | 新增跨登录持久化字段 | 本期避免额外状态同步，重新登录默认主组织 |
| 数据权限组织列表 | `DataScopeContext.orgIds=[activeOrgId]` | 继续使用用户全部组织 | 防止 `ORG` 数据范围放大 |
| 历史角色迁移 | 复制旧 `sys_user_role` 到用户全部已绑定组织 | 只复制到主组织 | 保持升级后权限不收窄，后续人工治理 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 沉淀？ |
|------|------|----------|--------|
| 放开多组织后 `ORG` 数据权限可能扩大 | 现有 `DataScopeServiceImpl` 把 `LoginUser.orgIds` 全量写入数据权限上下文 | 当前组织上下文只写入 `[activeOrgId]` | 待实现后按结果决定是否写入 memory |
| `/auth/userInfo` 可能覆盖组织切换结果 | 当前接口会重新从 DB 构建 `LoginUser` | 重建时传入旧 Session 的 `activeOrgId` | 待实现后按结果决定是否写入 memory |
| Flyway 版本口径需统一 | 实际迁移目录 `forge-server/db/migration/` 只有 baseline，`db/backup` 是归档目录 | 按实际迁移目录排序继续，本变更使用 `V1.0.1` | 已按用户要求更新 |
| 前端角色授权旧入口可能绕过组织维度 | 角色页原“添加用户/移除用户”走全局角色接口 | 新 UI 改为先选授权组织，再调用 `/system/user/{id}/org-roles` 追加或移除当前角色 | 可作为后续 review 重点 |

## 知识发现

- [x] **关键词**: 组织上下文权限 — `sys_user_org` 已支持多组织，真正缺口是角色授权缺少组织维度。
- [x] **关键词**: 数据权限组织范围 — 当前 `DataScopeContext.orgIds` 使用用户全部组织，是多组织放开后的核心越权风险。
- [x] **关键词**: 权限旁路 — 消息、流程、数据集存在直接读取 `sys_user_role` 或 `LoginUser.orgIds` 的链路，必须纳入改造。

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
|--------|-----------|----------|----------|
| 迁移版本 | 按实际迁移目录排序继续 | 当前实际目录 `forge-server/db/migration/` 只有 `V1.0.0__baseline.sql` | 本变更使用 `V1.0.1__add_org_scoped_role_permission.sql` |
| 用户组织能力 | 需求要求支持用户多组织 | 后端确有批量绑定，前端仍单选提交 | 任务拆分中单独安排前端多选与主组织 |
| 当前组织持久化 | 需求要求当前组织参与权限计算 | 当前没有跨登录保存字段 | 本期定义为 Session 态保留，重新登录默认主组织 |
| 前端组织授权组件 | tasks 初版计划新增 `UserOrgRolePanel.vue` / `RoleOrgScopePanel.vue` | 为降低页面改造和组件拆分成本，直接在 `system/user.vue`、`system/role.vue` 内复用现有弹窗和 `PremiumTree` | 不新增组件文件，但行为满足组织角色授权和角色适用组织配置 |

## 本轮验证记录

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-06 | 前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过 | 仅有既有 chunk 动态/静态导入提示、CSS `//` 注释 minify 警告、组件命名冲突提示 |
| 2026-07-06 | 后端编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests` | 通过 | Reactor `BUILD SUCCESS`，仅有既有 deprecation/unchecked/Lombok builder 默认值警告 |
| 2026-07-06 | Flyway 静态扫描 | `rg -n '\$\{[^}]+\}' forge-server/db/migration` | 通过 | 无输出，避免 Flyway placeholder 误解析 |
| 2026-07-06 | Diff 空白检查 | `git diff --check` | 通过 | 无输出 |
| 2026-07-06 | 旧权限关系排查 | `rg -n 'sys_user_role|SysUserRole|userRoleMapper' forge-server/forge-framework -g '*.java' -g '*.xml'` | 已检查 | 命中主要为 legacy 兼容写入/清理接口、旧 API 和实体 Mapper；新前端主流程已改走 `sys_user_org_role` |
| 2026-07-06 | 当前组织写入链路 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests` | 通过 | 修正 `create_dept` 优先写当前组织后复跑，Reactor `BUILD SUCCESS`；仅既有 deprecation/unchecked/Lombok 警告 |
| 2026-07-06 | 当前组织写入链路 | `git diff --check` | 通过 | 无输出 |
| 2026-07-06 | Flyway 静态扫描 | `rg -n '\$\{[^}]+\}' forge-server/db/migration` | 通过 | 无输出 |
| 2026-07-06 | 本地服务环境探测 | `mysqladmin --protocol=tcp -h127.0.0.1 -P3407 -uroot ping`; `mysqladmin --protocol=tcp -h127.0.0.1 -P3306 -uroot ping`; `redis-cli -h 127.0.0.1 -p 6379 ping` | 未形成服务级验证 | 3407 MySQL 未运行；3306 root 无密码访问被拒；Redis 探测被沙箱限制，提权请求未获批。本轮未启动后端，未修改数据库 |
| 2026-07-06 | Review 修复后端编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests` | 通过 | Reactor `BUILD SUCCESS`，35 个模块成功；仅既有 deprecation/unchecked 警告 |
| 2026-07-06 | Review 修复前端构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过 | Node v20.19.0 可用；Vite `built in 1m 12s`；仅既有 CSS `//` 注释、动态/静态导入和组件命名冲突提示 |
| 2026-07-06 | Review 修复静态检查 | `git diff --check`; `rg -n '\$\{[^}]+\}' forge-server/db/migration` | 通过 | `git diff --check` 无输出；Flyway 扫描无输出 |
| 2026-07-06 | 超级管理员组织选项边界 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests` | 通过 | Reactor `BUILD SUCCESS`，35 个模块成功；仅既有 deprecation/unchecked 警告 |
| 2026-07-06 | 超级管理员组织选项边界 | `git diff --check` | 通过 | 无输出 |
| 2026-07-06 | 超级管理员显式组织绑定边界 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests` | 通过 | Reactor `BUILD SUCCESS`，35 个模块成功；仅既有 deprecation/unchecked 警告 |
| 2026-07-06 | 超级管理员显式组织绑定边界 | `rg -n '\$\{[^}]+\}' forge-server/db/migration` | 通过 | 无输出，未发现 Flyway placeholder 风险 |
| 2026-07-06 | 超级管理员显式组织绑定边界 | `git diff --check` | 通过 | 无输出 |
| 2026-07-06 | 超级管理员组织边界 Mapper XML 收敛 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests` | 通过 | Reactor `BUILD SUCCESS`，35 个模块成功；仅既有 deprecation/unchecked 警告 |
| 2026-07-06 | 超级管理员组织边界 Mapper XML 收敛 | `rg -n '\$\{[^}]+\}' forge-server/db/migration` | 通过 | 无输出，未发现 Flyway placeholder 风险 |
| 2026-07-06 | 超级管理员组织边界 Mapper XML 收敛 | `git diff --check` | 通过 | 无输出 |

## 跳过项

- 未启动后端和数据库做真实登录/切组织/菜单/API/数据权限接口验证；当前 `application-dev.yml` 指向远端 MySQL，直接启动会执行 Flyway 写库，不作为本轮验证方式。本地 3407 MySQL 未运行，3306 root 无密码访问被拒，Redis 探测受沙箱限制且提权未获批，服务级验证待可控本地 MySQL/Redis 环境可用后补跑。
- Review 修复后未新增服务级登录/组织授权/业务消息落库验证；原因同上，本轮只做编译、构建和静态检查，不启动会写库的后端服务。
- 超级管理员组织选项边界修复未启动服务做真实下拉验证；原因同上，当前 dev 库不作为自动验证目标。本轮通过后端编译确认代码可构建。
- 超级管理员显式组织绑定边界二次修正未启动服务做真实下拉验证；原因同上，当前 dev 库不作为自动验证目标。本轮通过后端编译和静态检查确认代码可构建，接口实测待可控本地 MySQL/Redis 环境补跑。
- 未新增 JUnit/Vitest 定向用例；权限改造已通过编译，但 P0 数据权限和组织切换单测仍应在后续 `/test org-scoped-role-permission` 补齐。

## 代码质量备忘

- 查询类复杂 SQL 写入 Mapper XML，避免 Service 层堆 LambdaQueryWrapper 并保持数据权限可审查。
- 所有权限判断必须同时校验 `tenantId` 和 `activeOrgId`。
- 前端不要用隐藏按钮替代后端鉴权。
- 迁移脚本必须具备防重复保护，并避免 Flyway `${...}` 占位符误解析。
