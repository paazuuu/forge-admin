# 权限体系完整配置指南

> 本教程将以「Forge Admin」在线演示环境为例，手把手带你完成从用户创建、角色配置、菜单授权、按钮权限、API 权限到数据权限的全链路配置。

---

## 一、权限模型总览

Forge Admin 采用 **RBAC（基于角色的访问控制）** 权限模型，核心关系链如下：

```
用户 ──> 角色 ──> 资源（菜单/按钮/API）
                   │
                   ├── 菜单权限：控制侧边栏能看到什么页面
                   ├── 按钮权限：控制页面上能看到什么操作按钮
                   ├── API 权限：控制后端接口能否被调用
                   └── 数据权限：控制查询结果能看到哪些数据
```

### 1.1 核心数据表

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表，存储用户基本信息 |
| `sys_role` | 角色表，定义角色和数据权限范围 |
| `sys_user_role` | 用户-角色关联表（多对多） |
| `sys_resource` | 资源表，存储菜单、按钮、API 三类资源 |
| `sys_role_resource` | 角色-资源关联表（多对多） |

### 1.2 资源类型

`sys_resource.resource_type` 字段定义了四种资源类型：

| 值 | 类型 | 说明 |
|----|------|------|
| 1 | 目录 | 侧边栏的分组目录，如「系统管理」 |
| 2 | 菜单 | 实际页面路由，如「用户管理」 |
| 3 | 按钮 | 页面内的操作按钮，如「新增」「删除」 |
| 4 | API 接口 | 后端接口地址，用于接口级权限控制 |

### 1.3 权限校验流程

```
用户请求 ──> Sa-Token 登录校验 ──> API 权限拦截器 ──> 业务逻辑
                                    │
                                    ├── 检查接口是否配置了 API 资源
                                    ├── 如果配置了，检查用户 API 权限列表
                                    └── 通配符匹配（如 /system/**）
```

前端路由守卫会在页面跳转时检查用户是否有该路由的访问权限，未授权的页面会跳转到 403。

---

## 二、创建用户

### 2.1 进入用户管理

1. 登录系统，使用 `admin` 账号
2. 在左侧菜单中点击 **系统管理 → 用户管理**
3. 页面展示组织架构树 + 用户列表联动视图

![用户管理页面](https://gitee.com/ForgeLab/forge-admin/raw/main/images/user-management.png)

### 2.2 新增用户

1. 点击页面右上角 **新增** 按钮
2. 在弹出的表单中填写以下信息：
   - **用户名**：登录账号，如 `zhangsan`
   - **姓名**：真实姓名，如 `张三`
   - **密码**：初始密码，建议使用强密码
   - **手机号**：用于短信验证和通知
   - **邮箱**：用于邮件通知
   - **所属组织**：选择用户所属的部门
   - **用户类型**：
     - `系统管理员`（type=0）：拥有全部权限，不受数据权限限制
     - `租户管理员`（type=1）：管理本租户内的用户和配置
     - `普通用户`（type=2）：只能访问被授权的资源

3. 点击 **确定** 保存

### 2.3 用户类型说明

| 用户类型 | userType 值 | 权限特点 |
|----------|------------|---------|
| 系统管理员 | 0 | 超级管理员，`isAdmin()` 返回 true，API 权限含 `/**` 通配符，直接放行所有接口 |
| 租户管理员 | 1 | `isTenantAdmin()` 返回 true，管理本租户资源 |
| 普通用户 | 2 | 严格按照角色绑定的资源进行权限控制 |

::: tip 重要提示
系统管理员（userType=0）在 `PermissionServiceImpl` 中会直接匹配 `/**` 通配符，跳过所有 API 权限检查。请谨慎分配此类型。
:::

---

## 三、配置角色

### 3.1 进入角色管理

1. 在左侧菜单中点击 **系统管理 → 角色管理**
2. 页面展示角色列表，包含角色名称、角色标识、角色类型、数据权限范围等信息

### 3.2 新增角色

1. 点击 **新增** 按钮
2. 填写角色信息：

| 字段 | 说明 | 示例 |
|------|------|------|
| 角色名称 | 租户内唯一的角色名称 | `人事专员` |
| 角色标识 | 权限字符串，用于代码中权限判断 | `hr_staff` |
| 角色类型 | 管理角色/业务角色/审批角色/数据角色 | `业务角色` |
| 数据权限范围 | 决定该角色能看到哪些数据 | `本人数据` |
| 排序 | 数值越小越靠前 | `1` |
| 状态 | 启用/禁用 | `正常` |

3. 点击 **确定** 保存

### 3.3 数据权限范围详解

`SysRole.dataScope` 字段定义了角色的数据可见范围：

| 值 | 类型 | 说明 | 效果 |
|----|------|------|------|
| 1 | 全部数据 | `ALL` | 不追加任何数据过滤条件，看到所有数据 |
| 2 | 本租户数据 | `TENANT_ALL` | 追加 `tenant_id = 当前租户ID` 条件 |
| 3 | 本组织数据 | `ORG` | 追加 `org_id IN (用户所属组织ID)` 条件 |
| 4 | 本组织及子组织 | `ORG_AND_CHILD` | 追加 `org_id IN (用户组织+子组织ID列表)` 条件 |
| 5 | 个人数据 | `SELF` | 追加 `create_by = 当前用户ID` 条件 |
| 7 | 本行政区划 | `REGION` | 按行政区划编码过滤（省级不限，市级及以下过滤本级+下级） |

::: warning 数据权限多角色取最小范围
一个用户绑定了多个角色时，系统会取所有角色中**最严格**的数据权限范围（即范围最小的那个）。例如：角色A是「本组织」，角色B是「全部数据」，最终生效的是「本组织」。
:::

### 3.4 为角色分配资源权限

1. 在角色列表中，点击目标角色行的 **资源授权** 按钮
2. 弹出资源树弹窗，展示所有可分配的资源
3. 勾选该角色可以访问的菜单、按钮和 API 资源
4. 点击 **确定** 保存

资源树结构示例：

```
├── 系统管理（目录）
│   ├── 用户管理（菜单）
│   │   ├── 新增（按钮，perms: sys:user:add）
│   │   ├── 编辑（按钮，perms: sys:user:edit）
│   │   ├── 删除（按钮，perms: sys:user:remove）
│   │   └── 导出（按钮，perms: sys:user:export）
│   ├── 角色管理（菜单）
│   │   ├── 新增（按钮）
│   │   └── ...
│   └── ...
├── AI 大屏（目录）
│   └── ...
```

### 3.5 为角色绑定用户

1. 在角色列表中，点击目标角色行的 **分配用户** 按钮
2. 在弹窗中勾选需要绑定该角色的用户
3. 点击 **确定** 保存

一个用户可以同时绑定多个角色，权限取并集，数据权限取最小范围。

---

## 四、菜单与资源管理

### 4.1 进入菜单管理

1. 在左侧菜单中点击 **系统管理 → 菜单管理**
2. 页面以树形结构展示所有资源

![菜单管理页面](https://gitee.com/ForgeLab/forge-admin/raw/main/images/%E8%8F%9C%E5%8D%95%E7%AE%A1%E7%90%86.png)

### 4.2 新增目录

目录是侧边栏的分组容器，如「系统管理」「AI 大屏」。

1. 点击 **新增** 按钮
2. 资源类型选择 **目录**
3. 填写信息：

| 字段 | 说明 | 示例 |
|------|------|------|
| 资源名称 | 目录显示名称 | `人事管理` |
| 父级资源 | 顶级目录选「根目录」 | `根目录` |
| 排序 | 数值越小越靠前 | `10` |
| 图标 | 目录图标 | `md-people` |
| 显示状态 | 是否在侧边栏显示 | `显示` |

4. 点击 **确定** 保存

### 4.3 新增菜单

菜单是实际的页面路由，对应前端的一个 Vue 组件。

1. 点击 **新增** 按钮
2. 资源类型选择 **菜单**
3. 填写信息：

| 字段 | 说明 | 示例 |
|------|------|------|
| 资源名称 | 菜单显示名称 | `员工列表` |
| 父级资源 | 选择所属目录 | `人事管理` |
| 资源路由 | 前端路由路径 | `/employee/list` |
| 前端组件 | Vue 组件路径 | `employee/index` |
| 权限标识 | 按钮权限用，菜单可留空 | `employee:list` |
| 图标 | 菜单图标 | `md-list` |
| 是否缓存 | 是否keep-alive | `是` |
| 最低用户类型 | 限制访问的用户类型 | `普通用户` |

::: tip 路由与组件映射
前端使用 `unplugin-vue-router` 自动路由，组件路径相对于 `src/views/` 目录。例如 `employee/index` 对应 `src/views/employee/index.vue`。
:::

### 4.4 新增按钮权限

按钮权限控制页面内的操作按钮可见性。

1. 点击 **新增** 按钮
2. 资源类型选择 **按钮**
3. 填写信息：

| 字段 | 说明 | 示例 |
|------|------|------|
| 资源名称 | 按钮显示名称 | `新增员工` |
| 父级资源 | 选择所属菜单 | `员工列表` |
| 权限标识 | **关键字段**，前端用此标识判断按钮显示 | `employee:add` |

4. 点击 **确定** 保存

### 4.5 新增 API 权限

API 权限控制后端接口的访问，是最高级别的权限控制。

1. 点击 **新增** 按钮
2. 资源类型选择 **API 接口**
3. 填写信息：

| 字段 | 说明 | 示例 |
|------|------|------|
| 资源名称 | 接口描述 | `新增员工接口` |
| 父级资源 | 选择所属菜单 | `员工列表` |
| API 请求方法 | HTTP 方法 | `POST` |
| API 接口地址 | 接口路径，支持通配符 | `/employee/add` |

4. 点击 **确定** 保存

::: warning API 权限工作原理
后端 `ApiPermissionInterceptor` 拦截器会在每次请求时：
1. 检查该 URI 是否在 `sys_resource` 中配置了 API 类型资源
2. 如果未配置，直接放行（未配置=不受控）
3. 如果已配置，检查当前用户的 `apiPermissions` 列表
4. 使用 `PathMatcher` 通配符匹配，如 `/system/**` 匹配 `/system/user/add`

所以**只有配置了 API 资源的接口才会被拦截**。建议对所有敏感接口都配置 API 资源。
:::

### 4.6 公开资源与匿名访问

某些接口不需要登录即可访问（如登录接口、验证码接口），可以通过以下方式跳过权限校验：

**方式一：设置公开资源**

在资源管理中，将 `isPublic` 字段设为 `1`，该资源无需权限验证。

**方式二：后端注解**

```java
// 使用 @SaIgnore 注解，跳过 Sa-Token 登录校验和 API 权限校验
@SaIgnore
@GetMapping("/public/info")
public RespInfo<String> publicInfo() {
    return RespInfo.success("公开信息");
}

// 使用 @ApiPermissionIgnore 注解，仅跳过 API 权限校验（仍需登录）
@ApiPermissionIgnore
@GetMapping("/internal/info")
public RespInfo<String> internalInfo() {
    return RespInfo.success("内部信息");
}
```

**方式三：API 配置管理**

在「系统管理 → API 配置管理」中，将接口的 `needAuth` 设为 `false`，该接口可匿名访问。

---

## 五、前端权限控制

### 5.1 路由权限

前端路由守卫 `permission-guard.js` 会在每次路由跳转时检查：

1. 用户是否已登录（Token 校验）
2. 目标路由是否在白名单中
3. 用户是否有该路由的访问权限（与后端资源树匹配）
4. 是否需要强制修改密码

无权限访问的页面会自动跳转到 `/403`。

### 5.2 按钮权限指令

在前端页面中使用 `v-permission` 指令控制按钮显示：

```vue
<template>
  <n-button v-permission="'employee:add'" type="primary">新增</n-button>
  <n-button v-permission="'employee:edit'" type="warning">编辑</n-button>
  <n-button v-permission="'employee:remove'" type="error">删除</n-button>
</template>
```

只有当前用户角色中包含对应 `perms` 标识的资源，按钮才会显示。

### 5.3 权限判断工具函数

```javascript
import { useUserStore } from '@/store'

const userStore = useUserStore()

// 判断是否有某个权限
const canAdd = userStore.hasPermission('employee:add')

// 判断是否是管理员
const isAdmin = userStore.userInfo.userType === 0
```

---

## 六、数据权限配置

### 6.1 角色数据权限

在角色管理中，每个角色都有一个 `dataScope` 字段，定义该角色的数据可见范围（见 [3.3 数据权限范围详解](#_3-3-数据权限范围详解)）。

### 6.2 数据权限配置表

对于更细粒度的数据权限控制，Forge Admin 提供了 `sys_data_scope_config` 配置表，可以按 Mapper 方法级别配置：

1. 进入 **系统管理 → 数据权限配置**
2. 新增数据权限配置，指定：
   - **Mapper 方法**：如 `com.mdframe.forge.employee.mapper.EmployeeMapper.selectPage`
   - **表别名**：如 `e`
   - **用户 ID 字段**：如 `create_by`
   - **组织 ID 字段**：如 `dept_id`
   - **租户 ID 字段**：如 `tenant_id`

![数据权限配置页面](https://gitee.com/ForgeLab/forge-admin/raw/main/images/%E6%95%B0%E6%8D%AE%E6%9D%83%E9%99%90%E9%85%8D%E7%BD%AE.png)

### 6.3 数据权限工作原理

数据权限通过 MyBatis-Plus 的 `InnerInterceptor` 实现，在 SQL 执行前自动改写 WHERE 条件：

```sql
-- 原始 SQL
SELECT * FROM sys_employee WHERE del_flag = 0

-- 用户角色 dataScope=5（本人数据），自动改写为：
SELECT * FROM sys_employee WHERE del_flag = 0 AND create_by = 1

-- 用户角色 dataScope=3（本组织数据），自动改写为：
SELECT * FROM sys_employee WHERE del_flag = 0 AND dept_id IN (100, 101)
```

### 6.4 自定义数据权限

对于复杂的业务场景，支持自定义 SQL 条件：

```java
// 在 SysDataScopeConfig 中配置自定义 SQL
// 使用 <sql> 标签包裹，支持占位符：#{userId}、#{tenantId}、#{orgIds}
String customSql = "<sql>e.dept_id IN (SELECT dept_id FROM sys_user_dept WHERE user_id = #{userId})</sql>";
```

### 6.5 跳过数据权限

某些场景需要跳过数据权限（如后台任务、数据同步）：

```java
// 方式一：DataScopeContextHolder 设置跳过标记
DataScopeContextHolder.setSkip(true);
try {
    // 此查询不会追加数据权限条件
    List<Employee> all = employeeMapper.selectList(null);
} finally {
    DataScopeContextHolder.setSkip(false);
}

// 方式二：@DataScopeIgnore 注解（如有）
```

---

## 七、多租户权限

### 7.1 租户隔离机制

Forge Admin 的多租户隔离基于 MyBatis-Plus 的 `TenantLineInnerInterceptor` 实现：

- 所有继承 `TenantEntity` 的实体类，其表会自动追加 `tenant_id` 条件
- 租户 ID 从 `TenantContextHolder` 中获取，在用户登录时自动设置
- `sys_resource`（资源表）也继承 `TenantEntity`，意味着每个租户可以有自己的资源树

### 7.2 租户管理员权限边界

| 操作 | 系统管理员 | 租户管理员 | 普通用户 |
|------|-----------|-----------|---------|
| 创建租户 | ✅ | ❌ | ❌ |
| 管理本租户用户 | ✅ | ✅ | ❌ |
| 配置本租户角色 | ✅ | ✅ | ❌ |
| 配置本租户菜单 | ✅ | ✅ | ❌ |
| 跨租户操作 | ✅ | ❌ | ❌ |
| 查看全部数据 | ✅ | ❌ | ❌ |

### 7.3 忽略租户隔离

```java
// 方式一：注解
@IgnoreTenant
public List<SysTenant> listAllTenants() {
    return tenantMapper.selectList(null);
}

// 方式二：工具类
List<SysTenant> tenants = TenantContextHolder.executeIgnore(() -> {
    return tenantMapper.selectList(null);
});
```

---

## 八、完整配置示例

### 场景：为「人事部门」配置完整权限

**需求**：人事部门的专员可以管理员工信息，只能看到本部门员工数据，不能删除员工。

**步骤**：

#### 1. 创建角色

| 字段 | 值 |
|------|-----|
| 角色名称 | 人事专员 |
| 角色标识 | hr_staff |
| 角色类型 | 业务角色 |
| 数据权限范围 | 本组织数据（3） |

#### 2. 配置资源权限

勾选以下资源：
- ✅ 人事管理（目录）
  - ✅ 员工列表（菜单）
    - ✅ 新增员工（按钮，perms: `employee:add`）
    - ✅ 编辑员工（按钮，perms: `employee:edit`）
    - ✅ 查看员工（按钮，perms: `employee:list`）
    - ✅ 导出员工（按钮，perms: `employee:export`）
    - ❌ 删除员工（按钮，perms: `employee:remove`）— 不勾选
  - ✅ 员工接口
    - ✅ GET /employee/page（API）
    - ✅ GET /employee/list（API）
    - ✅ POST /employee/add（API）
    - ✅ POST /employee/edit（API）
    - ❌ POST /employee/remove/{id}（API）— 不勾选

#### 3. 配置数据权限

在数据权限配置中新增：
- Mapper 方法：`com.mdframe.forge.employee.mapper.EmployeeMapper.selectPage`
- 表别名：`e`
- 组织字段：`dept_id`

#### 4. 创建用户并分配角色

1. 新增用户 `lisi`（李四），所属组织选择「人事部」
2. 在角色管理中，为「人事专员」角色分配用户 `lisi`

#### 5. 验证

使用 `lisi` 账号登录后：
- ✅ 侧边栏只能看到「人事管理 → 员工列表」
- ✅ 员工列表只显示本部门（人事部）的员工数据
- ✅ 页面上有「新增」「编辑」「导出」按钮，没有「删除」按钮
- ✅ 调用 `/employee/remove/{id}` 接口会返回 403 无权限

---

## 九、常见问题

### Q1: 用户登录后看不到任何菜单？

**排查步骤**：
1. 检查用户是否已绑定角色（用户管理 → 分配角色）
2. 检查角色是否已分配资源权限（角色管理 → 资源授权）
3. 检查菜单资源的 `visible` 和 `menuStatus` 是否为显示状态
4. 检查菜单的 `minUserType` 是否限制了用户类型

### Q2: 用户有菜单权限但接口返回 403？

**排查步骤**：
1. 检查该接口是否在 `sys_resource` 中配置了 API 类型资源
2. 如果配置了，检查角色是否分配了该 API 资源
3. 检查用户的 `apiPermissions` 列表（可在 Redis 中查看 Sa-Token Session）
4. 确认 `forge.auth.enable-api-permission` 配置是否开启

### Q3: 数据权限不生效？

**排查步骤**：
1. 确认 Mapper 方法在 `sys_data_scope_config` 中已配置
2. 检查角色的 `dataScope` 字段值是否正确
3. 检查 `DataScopeContextHolder` 是否设置了跳过标记
4. 查看日志中 `DataScopeInterceptor` 的 SQL 改写记录

### Q4: 超级管理员和普通用户的权限区别？

超级管理员（userType=0）的特殊待遇：
- API 权限列表包含 `/**`，直接放行所有接口
- 数据权限类型为 `ALL`，不追加任何数据过滤条件
- 不受 `minUserType` 限制，可访问所有菜单

### Q5: 同一用户多个角色的数据权限如何计算？

取**最严格**的范围。例如：
- 角色A：dataScope=1（全部数据）
- 角色B：dataScope=5（个人数据）
- 最终生效：dataScope=5（个人数据）

---

## 十、最佳实践

1. **最小权限原则**：只给用户分配必要的角色和权限
2. **API 资源全覆盖**：所有敏感接口都应在资源管理中配置 API 资源
3. **角色按业务划分**：不要创建「万能角色」，按业务场景拆分角色
4. **数据权限从严配置**：默认使用「本组织」或「个人」数据权限，需要更大范围时再单独调整
5. **定期审计**：定期检查角色资源分配，清理不必要的权限
6. **测试验证**：配置完成后，使用测试账号登录验证权限是否符合预期
