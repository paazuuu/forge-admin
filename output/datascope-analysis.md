# forge-starter-datascope 模块技术分析报告


> 📅 生成日期：2026-05-26


---


# 第 1 章：模块概览与核心架构

## 1.1 模块定位

`forge-starter-datascope` 是 Forge 框架中的数据权限控制模块，基于 **MyBatis-Plus InnerInterceptor** 机制实现。其核心职责是在 SQL 执行前自动改写 WHERE 条件，根据不同用户的角色数据范围（组织、行政区划、自定义机构等）透明地追加权限过滤子句。

### 模块坐标

| 项 | 值 |
|---|---|
| Maven Artifact | `com.mdframe.forge:forge-starter-datascope` |
| 父模块 | `forge-starter-parent` |
| 核心依赖 | mybatis-plus-spring-boot3-starter、hutool-all、jsqlparser |
| 自动配置入口 | `DataScopeAutoConfiguration` |

## 1.2 文件清单与职责矩阵

模块源代码共 16 个 Java 文件，按职责分为 6 层：

### 自动配置层

| 文件 | 职责 |
|---|---|
| `config/DataScopeAutoConfiguration.java` | Spring Boot 自动配置入口，声明 `DataScopeInterceptor` Bean |
| `config/DataScopeProperties.java` | 配置属性映射（`forge.datascope.enabled`、`print-sql`） |
| `config/DataScopeIgnore.java` | `@DataScopeIgnore` 注解定义（标于类/方法级，标记跳过数据权限） |

> **关键设计**：`DataScopeAutoConfiguration` 声明 `@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)`，确保在 ORM 配置前加载，只提供 Bean 而不自行注册到 `MybatisPlusInterceptor`，由 `MybatisPlusConfig` 通过 `@Autowired List<InnerInterceptor>` 统一注入。

### 拦截器层

| 文件 | 职责 |
|---|---|
| `handler/DataScopeInterceptor.java` | 核心拦截器，实现 `InnerInterceptor.beforeQuery()`，负责 SQL 权限改写 |

### 上下文层

| 文件 | 职责 |
|---|---|
| `context/DataScopeContext.java` | 数据权限上下文 POJO，封装用户ID、组织ID、角色ID、权限范围、行政区划等 |
| `context/DataScopeContextHolder.java` | ThreadLocal 持有者，控制"跳过权限"标记 |

### 服务层

| 文件 | 职责 |
|---|---|
| `service/IDataScopeService.java` | 数据权限服务接口 |
| `service/impl/DataScopeServiceImpl.java` | 实现：获取用户权限上下文、查询配置、组织树展开、缓存管理 |

### 实体/枚举/持久层

| 文件 | 职责 |
|---|---|
| `entity/SysDataScopeConfig.java` | Mapper 方法级权限配置实体（对应表 `sys_data_scope_config`） |
| `entity/SysRoleDataScope.java` | 角色-自定义组织关联实体（对应表 `sys_role_data_scope`） |
| `enums/DataScopeType.java` | 7种数据权限范围枚举（ALL/SELF/ORG/ORG_AND_CHILD/CUSTOM/TENANT_ALL/REGION） |
| `mapper/SysDataScopeConfigMapper.java` | 配置表 Mapper |
| `mapper/DataScopeRoleMapper.java` | 角色 Mapper（查询最小 data_scope） |
| `mapper/DataScopeOrgMapper.java` | 组织 Mapper（查询子孙组织） |
| `mapper/SysRoleDataScopeMapper.java` | 角色-自定义组织 Mapper |

### 控制器层

| 文件 | 职责 |
|---|---|
| `controller/DataScopeController.java` | REST API：刷新缓存（`POST /datascope/refreshCache`） |

### 配置文件

| 文件 | 职责 |
|---|---|
| `application-datascope-example.yml` | 配置示例：enabled、print-sql、enable-api |
| `META-INF/spring/...AutoConfiguration.imports` | Spring Boot 3.x 自动配置索引 |

## 1.3 组件协作关系

```
用户请求 → Controller → Service → Mapper
                  ↓
         MybatisPlusInterceptor
                  ↓
    ┌─────────────┼──────────────┐
    │             │               │
DataScope   TenantLine     Pagination
Interceptor  Interceptor    Interceptor
    │
    ├→ DataScopeContextHolder.checkSkip()
    ├→ IDataScopeService.getCurrentUserDataScope()
    │     ├→ 超级管理员 → ALL（跳过）
    │     ├→ 租户管理员 → TENANT_ALL
    │     └→ 普通用户 → roleMapper.selectMinDataScope()
    │                   → 构建 DataScopeContext
    ├→ IDataScopeService.getDataScopeConfig(mapperId)
    │     └→ Caffeine 缓存 → DB 查询（sys_data_scope_config）
    ├→ DataScopeType 映射
    └→ JSQLParser 解析原 SQL → 追加 WHERE 条件 → 修改 BoundSql
```

## 1.4 与 MyBatis-Plus 拦截器的集成机制

模块采用**提供者-消费者模式**实现拦截器注入，这是一种松耦合的注册机制：

1. **提供方**（`DataScopeAutoConfiguration`）：声明 `@Bean DataScopeInterceptor`
2. **消费方**（`MybatisPlusConfig`）：通过 `@Autowired(required = false) List<InnerInterceptor>` 自动收集所有自定义拦截器
3. **注册顺序**：
   ```java
   // MybatisPlusConfig.mybatisPlusInterceptor()
   // 1. 先添加其他模块注册的拦截器（含 DataScope 和 Tenant）
   if (innerInterceptors != null && !innerInterceptors.isEmpty()) {
       innerInterceptors.forEach(interceptor.addInnerInterceptor);
   }
   // 2. 分页插件
   interceptor.addInnerInterceptor(paginationInnerInterceptor());
   // 3. 乐观锁插件
   interceptor.addInnerInterceptor(optimisticLockerInnerInterceptor());
   ```

拦截器的执行顺序由 `MybatisPlusInterceptor` 内部的 `interceptors` 列表决定——先添加的先执行。因此 DataScope 和 Tenant 拦截器都在分页插件之前执行，确保权限过滤发生在分页统计之前。

> **优先级约束**：`DataScopeAutoConfiguration` 设置 `@AutoConfigureOrder(HIGHEST_PRECEDENCE)`，确保在 TenantConfiguration 之前加载。但实际拦截器执行顺序由 `addInnerInterceptor()` 的调用顺序决定——由于 Java 的 Field Injection 不保证 List 中元素顺序，生产环境建议通过 `@Order` 注解或在 `MybatisPlusConfig` 中对 List 排序来显式控制 DataScope 和 Tenant 的执行顺序。



---


# 第 2 章：数据权限实现技术链路

## 2.1 数据权限上下文（DataScopeContext）

`DataScopeContext` 是拦截器和权限查询服务之间的数据契约，封装了一次请求中所有必要的数据权限信息：

| 字段 | 类型 | 来源 | 用途 |
|---|---|---|---|
| `userId` | Long | `LoginUser.getUserId()` | SELF 权限：`user_id = #{userId}` |
| `orgIds` | List\<Long\> | `LoginUser.getOrgIds()` | ORG/ORG_AND_CHILD：`org_id IN (...)` |
| `roleIds` | List\<Long\> | `LoginUser.getRoleIds()` | 查询最小 data_scope、自定义组织 |
| `minDataScope` | Integer | `roleMapper.selectMinDataScope()` | 决定 DataScopeType |
| `customOrgIds` | Set\<Long\> | `roleDataScopeMapper.selectOrgIdsByRoleIds()` | CUSTOM：`org_id IN (...)` |
| `tenantId` | Long | `LoginUser.getTenantId()` | TENANT_ALL：`tenant_id = #{tenantId}` |
| `regionCode` | String | `LoginUser.getRegionCode()` | REGION：行政区划过滤 |
| `regionLevel` | Integer | `LoginUser.getRegionLevel()` | REGION：判断是否省级（全放行） |
| `regionAncestors` | String | `LoginUser.getRegionAncestors()` | REGION：自定义SQL占位符替换 |

**构建流程**（`DataScopeServiceImpl.getCurrentUserDataScope()`）：

```
StpUtil.isLogin()? → No → return null
         ↓ Yes
LoginUser loginUser = SessionHelper.getLoginUser()
         ↓
    ┌────┼────┐
    ↓    ↓     ↓
  isAdmin?  isTenantAdmin?  普通用户
    ↓         ↓              ↓
  ALL     TENANT_ALL    roleMapper.selectMinDataScope(roleIds)
                           ↓
                     minDataScope == CUSTOM?
                           ↓ Yes
                     roleDataScopeMapper.selectOrgIdsByRoleIds()
                           ↓
                     构建 DataScopeContext
```

### 权限跳过机制（DataScopeContextHolder）

`DataScopeContextHolder` 基于 ThreadLocal 提供两种跳过模式：

1. **主动标记跳过**：`DataScopeContextHolder.skipDataScope()` — 适合后台任务、定时任务等无用户上下文场景
2. **执行块跳过**：`DataScopeContextHolder.executeWithoutDataScope(() -> {...})` — 适合需要在无权限限制下执行特定查询的子任务

## 2.2 权限范围枚举（DataScopeType）

`DataScopeType` 定义了 7 种权限范围，通过 `getByRoleDataScope()` 实现双向兼容的历史编码映射：

| 枚举值 | Code | 当前字典语义 | 旧枚举语义 | 触发的 SQL 条件 |
|---|---|---|---|---|
| `ALL` | 1 | 全部数据 | 全部数据 | 不追加条件 |
| `SELF` | 2 | - | 个人数据 | `user_id = #{userId}` |
| `ORG` | 3 | 本组织 | 本组织 | `org_id IN (...)` |
| `ORG_AND_CHILD` | 4 | 本组织及子组织 | 本组织及子组织 | `org_id IN (所有子孙组织)` |
| `CUSTOM` | 5 | - | 自定义 | `org_id IN (自定义组织)` |
| `TENANT_ALL` | 6 | 本租户 | 本租户 | `tenant_id = #{tenantId}` |
| `REGION` | 7 | 本行政区划 | 本行政区划 | `area_code = #{regionCode} OR area_code IN (下级区划)` |

**兼容映射逻辑**（`getByRoleDataScope()`）：

```java
public static DataScopeType getByRoleDataScope(Integer code, boolean hasCustomOrgIds) {
    return switch (code) {
        case 1 -> ALL;
        case 2 -> TENANT_ALL;       // 当前字典：2=本租户
        case 3 -> ORG;
        case 4 -> ORG_AND_CHILD;
        case 5 -> hasCustomOrgIds ? CUSTOM : SELF;  // 关键兼容点
        case 6 -> TENANT_ALL;       // 旧枚举：6=本租户
        case 7 -> REGION;
        default -> getByCode(code);
    };
}
```

Code=5 的处理是核心兼容点：新字典中 5=个人数据（SELF），旧字典中 5=自定义（CUSTOM）。通过检查 `hasCustomOrgIds` 来区分——如果角色配置了自定义组织ID则为 CUSTOM，否则视为 SELF。

## 2.3 拦截器工作流程（DataScopeInterceptor）

`DataScopeInterceptor` 实现 `InnerInterceptor` 接口，完整工作流程如下：

### 步骤 1：检查跳过标记

```java
if (DataScopeContextHolder.isSkip()) {
    return;  // 后台任务场景
}
```

### 步骤 2：过滤内部 Mapper

```java
if (mapperId.startsWith("com.mdframe.forge.starter.datascope.mapper.")) {
    return;  // 避免递归拦截自身的数据权限查询
}
```

### 步骤 3：处理分页 Count 查询

MyBatis-Plus 分页插件在执行 `page()` 时会产生 `_mpCount` 后缀的方法，需要去掉后缀获取原方法名：

```java
if (mapperId.endsWith("_mpCount") || mapperId.endsWith("_COUNT")) {
    actualMapperId = mapperId.replaceAll("(_mpCount|_COUNT)$", "");
}
```

### 步骤 4：查询权限配置（含缓存）

```java
SysDataScopeConfig config = dataScopeService.getDataScopeConfig(actualMapperId);
if (config == null || config.getEnabled() == 0) {
    return;  // 未配置或已禁用 → 放行
}
```

### 步骤 5：获取用户权限上下文

```java
DataScopeContext context = dataScopeService.getCurrentUserDataScope();
if (context == null || context.getUserId() == null) {
    return;  // 未登录或后台任务
}
```

### 步骤 6：判定权限类型并决策

```java
DataScopeType scopeType = DataScopeType.getByRoleDataScope(
    context.getMinDataScope(), 
    context.getCustomOrgIds() != null && !context.getCustomOrgIds().isEmpty()
);

// 全部数据 → 放行
if (scopeType == DataScopeType.ALL) return;

// 省级行政区划 → 等同全部，放行
if (scopeType == DataScopeType.REGION && Integer.valueOf(1).equals(context.getRegionLevel())) return;
```

### 步骤 7：SQL 改写

```java
String originalSql = boundSql.getSql();
String modifiedSql = buildDataScopeSql(originalSql, config, context, scopeType);

PLUGIN_UTILS.MPBoundSql mpBoundSql = PLUGIN_UTILS.mpBoundSql(boundSql);
mpBoundSql.sql(modifiedSql);
```

## 2.4 SQL 改写引擎

`buildDataScopeSql()` 方法使用 **JSQLParser** 作为 SQL 解析和改写引擎：

```java
private String buildDataScopeSql(String originalSql, SysDataScopeConfig config, 
        DataScopeContext context, DataScopeType scopeType) throws Exception {
    
    // 1. 解析 SQL 为 AST
    Statement statement = CCJSqlParserUtil.parse(originalSql);
    if (!(statement instanceof Select)) return originalSql;  // 非 SELECT 不处理
    
    // 2. 定位 WHERE 子句位置
    Select select = (Select) statement;
    PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
    Expression where = plainSelect.getWhere();
    
    // 3. 构建权限条件表达式
    Expression dataScopeCondition = buildDataScopeCondition(config, context, scopeType);
    
    // 4. 追加或替换 WHERE
    if (where != null) {
        plainSelect.setWhere(new AndExpression(where, dataScopeCondition));
    } else {
        plainSelect.setWhere(dataScopeCondition);
    }
    
    // 5. 序列化回 SQL 字符串
    return select.toString();
}
```

**条件构造分支**（`buildDataScopeCondition()`）：

| scopeType | 条件表达式 |
|---|---|
| SELF | `tableAlias.userIdColumn = context.userId` |
| ORG | `tableAlias.orgIdColumn IN (orgIds)` |
| ORG_AND_CHILD | `tableAlias.orgIdColumn IN (allOrgIds)`（递归展开组织树） |
| CUSTOM | `tableAlias.orgIdColumn IN (customOrgIds)` |
| TENANT_ALL | `tableAlias.tenantIdColumn = context.tenantId` |
| REGION | `area_code = regionCode OR area_code IN (SELECT code FROM sys_region_code WHERE parent_code = regionCode)` |

## 2.5 关键架构决策

整个链路中有两个关键决策节点：

1. **权限配置查询早于权限上下文计算**：先查 mapper 是否有配置，再计算用户权限。避免"每个无配置的普通查询都白白计算一次角色数据范围"——这是性能优化的第一道关口。

2. **minDataScope 取所有角色中的最小值**：data_scope 字段遵循"值越小权限越大"的约定（1=全部 > 2=本租户 > 3=本组织 > 4=本组织及子组织 > 5=个人）。取 MIN(data_scope) 即取用户所有角色中权限最大的那个，保证权限的"最大包容"原则。



---


# 第 3 章：XML SQL 规则与复杂场景处理

## 3.1 权限配置实体（SysDataScopeConfig）

`SysDataScopeConfig` 是 Mapper 方法级权限配置的核心实体，对应数据库表 `sys_data_scope_config`。每条记录定义了一个 Mapper 方法的数据权限改写规则：

| 字段 | 类型 | 说明 |
|---|---|---|
| `mapperId` | String | Mapper 方法全限定名（如 `com.xxx.mapper.OrderMapper.selectList`） |
| `tableAlias` | String | 主表别名（如 `o`），用于构造 `o.org_id IN (...)` |
| `userIdColumn` | String | 用户ID列名（如 `create_by`），SELF 权限使用 |
| `orgIdColumn` | String | 组织ID列名（如 `org_id`），ORG/ORG_AND_CHILD/CUSTOM 使用 |
| `tenantIdColumn` | String | 租户ID列名（如 `tenant_id`），TENANT_ALL 使用 |
| `regionCodeColumn` | String | 行政区划代码列名（如 `area_code`），REGION 使用 |
| `userRegionColumn` | String | 用户行政区划列名（如 `user_area_code`），REGION 使用 |
| `userTableAlias` | String | 用户表别名（如 `u`），REGION 使用 |
| `enabled` | Integer | 是否启用（0=禁用，1=启用） |

### 简单字段模式 vs 复杂 SQL 模式

模块通过字段值的格式自动区分两种模式：

- **简单字段模式**：列名不包含 `<sql>` 标记，直接拼接 `tableAlias.columnName = value`
- **复杂 SQL 模式**：列名以 `<sql>` 开头，如 `<sql>o.org_id IN (SELECT org_id FROM ...)</sql>`，直接使用 SQL 模板

```java
// DataScopeInterceptor.buildColumnCondition()
private Expression buildColumnCondition(String column, String tableAlias, 
        String value, boolean isInCondition) {
    if (column.startsWith("<sql>")) {
        // 复杂 SQL 模式：提取 SQL 模板，替换占位符
        String sqlTemplate = column.replace("<sql>", "").replace("</sql>", "");
        return CCJSqlParserUtil.parseExpression(sqlTemplate);
    }
    // 简单字段模式
    String fullColumn = tableAlias != null ? tableAlias + "." + column : column;
    if (isInCondition) {
        return CCJSqlParserUtil.parseExpression(fullColumn + " IN (" + value + ")");
    }
    return CCJSqlParserUtil.parseExpression(fullColumn + " = " + value);
}
```

## 3.2 复杂 SQL 场景处理

### 3.2.1 JOIN 查询

模块通过 `tableAlias` 字段指定权限条件应追加到哪个表上。对于多表 JOIN 查询，只需在配置中指定正确的表别名：

```sql
-- 原始 SQL
SELECT o.*, u.name FROM t_order o 
LEFT JOIN t_user u ON o.user_id = u.id

-- 配置：tableAlias = "o", orgIdColumn = "org_id"
-- 改写后（ORG 权限）
SELECT o.*, u.name FROM t_order o 
LEFT JOIN t_user u ON o.user_id = u.id 
WHERE o.org_id IN (101, 102, 103)
```

### 3.2.2 子查询

JSQLParser 能正确解析子查询的 AST 结构，权限条件追加到最外层 WHERE：

```sql
-- 原始 SQL
SELECT * FROM t_order WHERE id IN (SELECT order_id FROM t_order_item WHERE price > 100)

-- 改写后（SELF 权限，tableAlias = "o"）
SELECT * FROM t_order o WHERE o.create_by = 12345 
AND id IN (SELECT order_id FROM t_order_item WHERE price > 100)
```

### 3.2.3 UNION 查询

对于 UNION 查询，JSQLParser 将每个 SELECT 分支解析为独立的 `PlainSelect`。模块当前实现仅处理 `SelectBody` 为 `PlainSelect` 的情况，UNION 查询的 `SelectBody` 为 `SetOperationList`，会被跳过。这是模块的一个已知限制——UNION 查询需要手动在 SQL 中处理权限条件。

### 3.2.4 分页 Count 查询

MyBatis-Plus 分页插件在执行 `page()` 时会生成 `_mpCount` 后缀的 Count 查询。模块通过去掉后缀获取原始 mapperId 来查询配置，确保 Count 查询和实际查询使用相同的权限条件：

```java
if (mapperId.endsWith("_mpCount") || mapperId.endsWith("_COUNT")) {
    actualMapperId = mapperId.replaceAll("(_mpCount|_COUNT)$", "");
}
```

## 3.3 行政区划权限（REGION）

REGION 权限是模块中最复杂的权限类型，支持三级行政区划过滤：

### 配置要求

REGION 权限需要额外配置 `regionCodeColumn`、`userRegionColumn` 和 `userTableAlias` 三个字段，用于关联用户表和业务表：

```sql
-- 配置示例
-- regionCodeColumn: area_code
-- userRegionColumn: region_code  
-- userTableAlias: u
-- 生成的条件：
-- area_code = '440300' 
-- OR area_code IN (SELECT code FROM sys_region_code WHERE parent_code = '440300')
```

### 实现机制

```java
private Expression buildRegionCondition(SysDataScopeConfig config, DataScopeContext context) {
    // 1. 构建本级条件
    Expression selfCondition = CCJSqlParserUtil.parseExpression(
        config.getTableAlias() + "." + config.getRegionCodeColumn() + " = '" + context.getRegionCode() + "'"
    );
    
    // 2. 构建下级条件（子查询）
    String subQuery = "SELECT code FROM sys_region_code WHERE parent_code = '" + context.getRegionCode() + "'";
    Expression childCondition = CCJSqlParserUtil.parseExpression(
        config.getTableAlias() + "." + config.getRegionCodeColumn() + " IN (" + subQuery + ")"
    );
    
    // 3. OR 连接
    return new OrExpression(selfCondition, childCondition);
}
```

### 省级特殊处理

当 `regionLevel == 1`（省级）时，拦截器直接放行，等同于 ALL 权限——省级用户通常需要查看全省数据，无需追加过滤条件。

## 3.4 自定义组织权限（CUSTOM）

CUSTOM 权限通过 `sys_role_data_scope` 表存储角色与组织的多对多关系：

```sql
-- sys_role_data_scope 表结构
role_id | org_id
--------|-------
101     | 1001
101     | 1002
102     | 2001
```

查询逻辑（`SysRoleDataScopeMapper.selectOrgIdsByRoleIds()`）：

```sql
SELECT DISTINCT org_id FROM sys_role_data_scope WHERE role_id IN (#{roleIds})
```

拦截器将查询结果作为 `customOrgIds` 传入上下文，最终生成 `org_id IN (1001, 1002)` 条件。

## 3.5 占位符替换机制

复杂 SQL 模式支持占位符替换，允许在 SQL 模板中引用上下文变量：

| 占位符 | 替换值 | 来源 |
|---|---|---|
| `#{userId}` | 当前用户ID | `context.getUserId()` |
| `#{tenantId}` | 当前租户ID | `context.getTenantId()` |
| `#{regionCode}` | 当前行政区划代码 | `context.getRegionCode()` |
| `#{regionAncestors}` | 行政区划祖先路径 | `context.getRegionAncestors()` |

```java
// DataScopeInterceptor.buildCustomSqlCondition()
private Expression buildCustomSqlCondition(String sqlTemplate, DataScopeContext context) {
    sqlTemplate = sqlTemplate
        .replace("#{userId}", String.valueOf(context.getUserId()))
        .replace("#{tenantId}", String.valueOf(context.getTenantId()))
        .replace("#{regionCode}", context.getRegionCode() != null ? "'" + context.getRegionCode() + "'" : "NULL")
        .replace("#{regionAncestors}", context.getRegionAncestors() != null ? 
            "'" + context.getRegionAncestors() + "'" : "NULL");
    return CCJSqlParserUtil.parseExpression(sqlTemplate);
}
```

## 3.6 plugin-data 模块的补充实现

`plugin-data` 模块提供了另一套数据集行权限实现（`DataDatasetRowScopeServiceImpl`），与 `forge-starter-datascope` 形成互补：

| 维度 | forge-starter-datascope | plugin-data |
|---|---|---|
| 配置粒度 | Mapper 方法级 | 数据集级 |
| 权限来源 | 角色 data_scope 字段 | 数据集行权限配置表 |
| 条件构建 | JSQLParser 解析 + 追加 WHERE | 直接拼接 SQL 条件字符串 |
| 占位符风格 | `#{userId}` | `:__scopeUserId`（命名参数） |
| 行政区划策略 | 本级 + 直接下级 | 本级 / 本级+直接下级 / 本级+所有下级（递归CTE） |

plugin-data 的行政区划实现更灵活，支持三种策略：

```java
// DataDatasetRowScopeStrategyEnum
SELF              → area_code = :__scopeRegionCode
SELF_AND_CHILDREN → area_code = :__scopeRegionCode OR parent_code = :__scopeRegionCode
SELF_AND_DESCENDANTS → 递归 CTE 查询所有后代区划
```

递归 CTE 实现（`DataDatasetRowScopeMapper.xml`）：

```sql
WITH RECURSIVE region_tree AS (
    SELECT code FROM sys_region_code WHERE code = #{regionCode}
    UNION ALL
    SELECT r.code FROM sys_region_code r 
    INNER JOIN region_tree rt ON r.parent_code = rt.code
)
SELECT code FROM region_tree
```



---


# 第 4 章：设计取舍与扩展指南

## 4.1 为什么选择 Mapper XML 层实现数据权限？

### 4.1.1 各层实现方案的对比

| 实现层 | 优点 | 缺点 | 适用场景 |
|---|---|---|---|
| **Controller 层** | 1. 业务语义最清晰<br>2. 可复用业务逻辑<br>3. 权限与业务强绑定 | 1. 代码重复度高<br>2. 难以统一维护<br>3. 易遗漏权限检查 | 简单业务、权限规则简单且不常变化 |
| **Service 层** | 1. 业务逻辑集中<br>2. 可复用权限判断<br>3. 支持复杂业务规则 | 1. SQL 与业务代码耦合<br>2. 难以处理复杂 JOIN 查询<br>3. 分页统计困难 | 中等复杂度业务、权限规则与业务逻辑强相关 |
| **Mapper XML 层** | 1. **SQL 透明化改写**<br>2. **统一入口**<br>3. **与业务代码解耦**<br>4. **自动处理分页统计** | 1. 配置复杂度高<br>2. 调试困难<br>3. 对复杂 SQL 支持有限 | 复杂业务、多表关联、需要统一权限控制 |

### 4.1.2 forge-starter-datascope 的设计决策

模块选择 Mapper XML 层实现，基于以下核心考量：

1. **SQL 透明化**：权限条件自动追加到 WHERE 子句，业务代码无需感知权限逻辑，保持代码简洁。
2. **统一入口**：所有数据访问都经过 MyBatis-Plus 拦截器，确保权限检查无遗漏。
3. **分页兼容性**：在分页插件之前执行，确保 Count 查询和实际查询使用相同的权限条件，避免分页统计错误。
4. **性能优化**：通过缓存权限配置和用户权限范围，减少重复计算。

### 4.1.3 与 MyBatis-Plus 拦截器体系的契合

MyBatis-Plus 的 `InnerInterceptor` 机制提供了 SQL 执行前的钩子，是数据权限实现的理想位置：

- **执行时机**：在 SQL 执行前、参数绑定后，可以安全地修改 SQL 字符串
- **上下文信息**：可以获取 Mapper 方法名、参数、BoundSql 等完整信息
- **链式调用**：支持多个拦截器按顺序执行，便于处理权限与租户的优先级关系

## 4.2 性能优化策略

### 4.2.1 两级缓存机制

模块采用两级缓存减少数据库查询：

| 缓存层 | 缓存对象 | TTL | 刷新机制 |
|---|---|---|---|
| **一级缓存**（Caffeine） | `mapperId → SysDataScopeConfig` | 30 分钟 | 1. 定时过期<br>2. 主动调用 `/datascope/refreshCache` API |
| **二级缓存**（Caffeine） | `orgId → 子孙组织ID列表` | 10 分钟 | 1. 定时过期<br>2. 组织树变更时需手动刷新 |

```java
// DataScopeServiceImpl
private final Cache<String, SysDataScopeConfig> configCache = 
    Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();

private final Cache<Long, List<Long>> orgChildCache = 
    Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
```

### 4.2.2 避免 N+1 查询

模块通过批量查询和缓存避免了常见的 N+1 问题：

1. **角色数据范围**：一次查询获取用户所有角色的最小 data_scope
2. **自定义组织**：一次查询获取用户所有角色的自定义组织ID
3. **组织树展开**：缓存组织ID到子孙组织的映射，避免重复递归查询

### 4.2.3 SQL 解析性能

JSQLParser 的 SQL 解析有一定性能开销。模块通过以下方式优化：

1. **配置检查前置**：先检查 mapper 是否有权限配置，无配置直接返回，避免不必要的 SQL 解析
2. **权限类型判断**：在解析 SQL 前判断权限类型，ALL/TENANT_ALL 等简单类型可快速处理
3. **缓存解析结果**：考虑对常见 SQL 模板的解析结果进行缓存（当前版本未实现）

## 4.3 与租户权限的集成关系

### 4.3.1 优先级安排

数据权限和租户权限都是通过 MyBatis-Plus 拦截器实现的，执行顺序由 `MybatisPlusConfig` 中 `addInnerInterceptor()` 的调用顺序决定：

```java
// 理想执行顺序
1. DataScopeInterceptor    → 数据权限过滤
2. TenantLineInnerInterceptor → 租户隔离
3. PaginationInnerInterceptor → 分页处理
```

**设计原则**：先过滤数据权限，再过滤租户。因为：
- 数据权限是基于用户角色的业务逻辑过滤
- 租户隔离是基于多租户架构的技术隔离
- 业务逻辑应优先于技术隔离

### 4.3.2 配置冲突处理

当数据权限和租户权限配置冲突时，模块采用以下策略：

| 场景 | 处理方式 | 示例 |
|---|---|---|
| **数据权限 ALL + 租户隔离** | 只应用租户隔离 | 超级管理员查看全公司数据，但需限制在本租户内 |
| **数据权限 TENANT_ALL + 租户隔离** | 两者条件一致，可优化合并 | 租户管理员查看本租户数据 |
| **数据权限 ORG + 租户隔离** | 两个条件 AND 连接 | 普通员工只能看到本组织+本租户的数据 |

### 4.3.3 自动配置优先级

通过 `@AutoConfigureOrder` 注解控制自动配置类的加载顺序：

```java
// DataScopeAutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)  // 优先级最高

// TenantAutoConfiguration  
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)  // 优先级低10
```

但注意：自动配置顺序只影响 Bean 的创建顺序，不影响拦截器的执行顺序。拦截器执行顺序由 `MybatisPlusConfig` 控制。

## 4.4 二次开发关键扩展点

### 4.4.1 自定义权限类型

如需新增权限类型（如项目维度、客户维度），需修改以下文件：

1. **扩展 `DataScopeType` 枚举**
2. **修改 `DataScopeInterceptor.buildDataScopeCondition()`** 添加新分支
3. **扩展 `DataScopeContext`** 添加新字段
4. **修改 `DataScopeServiceImpl.getCurrentUserDataScope()`** 计算新字段值

### 4.4.2 自定义 SQL 改写逻辑

如需修改 SQL 改写逻辑，可继承 `DataScopeInterceptor` 并重写以下方法：

| 方法 | 作用 | 扩展场景 |
|---|---|---|
| `buildDataScopeSql()` | 主 SQL 改写入口 | 修改 WHERE 条件追加逻辑 |
| `buildDataScopeCondition()` | 构建权限条件表达式 | 新增权限类型的条件构建 |
| `buildColumnCondition()` | 构建列条件 | 修改简单字段/复杂 SQL 的处理逻辑 |
| `buildRegionCondition()` | 构建行政区划条件 | 修改行政区划过滤策略 |

### 4.4.3 集成其他权限框架

如需集成 Spring Security、Shiro 等权限框架，需：

1. **实现自定义 `IDataScopeService`**：从权限框架的上下文中提取用户信息
2. **注册自定义实现**：通过 `@Primary` 或 `@Qualifier` 替换默认的 `DataScopeServiceImpl`
3. **调整缓存策略**：根据权限框架的特性调整缓存时间和刷新机制

### 4.4.4 性能监控与调优

模块提供了 `printSql` 配置项用于调试，生产环境可扩展：

```yaml
forge:
  datascope:
    enabled: true
    print-sql: false  # 生产环境关闭
    enable-api: true
    metrics:
      enabled: true
      slow-query-threshold: 100  # 毫秒
```

可添加以下监控指标：
- 权限配置缓存命中率
- SQL 解析平均耗时
- 权限类型分布统计
- 拦截器执行时间统计

## 4.5 已知限制与注意事项

### 4.5.1 SQL 兼容性限制

1. **UNION 查询**：不支持自动改写，需手动在 SQL 中处理权限条件
2. **存储过程/函数调用**：不支持，需在存储过程内部实现权限逻辑
3. **动态 SQL 片段**：MyBatis 的动态 SQL（`<if>`、`<choose>`）在拦截器执行时已渲染完毕，可正常处理

### 4.5.2 配置管理复杂度

1. **配置分散**：权限配置存储在数据库表中，与代码分离，需额外维护
2. **调试困难**：SQL 改写发生在运行时，错误排查需要查看日志或开启 `print-sql`
3. **版本管理**：配置变更需要数据库迁移脚本，与代码版本需同步

### 4.5.3 性能考量

1. **JSQLParser 开销**：复杂 SQL 的解析和序列化有一定性能损耗
2. **缓存一致性**：权限配置和组织树变更后，需要手动刷新缓存或等待过期
3. **内存占用**：组织树缓存可能较大，需根据实际数据量调整缓存策略

## 4.6 最佳实践建议

1. **配置最小化**：只为需要权限控制的 Mapper 方法配置权限，避免不必要的 SQL 解析
2. **权限类型简化**：尽量使用标准的 ORG/ORG_AND_CHILD/TENANT_ALL 类型，减少自定义逻辑
3. **缓存策略调优**：根据业务特点调整缓存时间，高频变更的数据使用较短 TTL
4. **监控告警**：对权限拦截器的执行时间、缓存命中率等关键指标进行监控
5. **测试覆盖**：编写单元测试验证各种权限类型和 SQL 场景的正确性

## 4.7 总结

`forge-starter-datascope` 模块在 Mapper XML 层实现数据权限，通过 SQL 透明化改写提供了统一、无侵入的权限控制方案。其核心优势在于：

1. **解耦性**：权限逻辑与业务代码完全分离
2. **一致性**：所有数据访问都经过统一的权限检查
3. **兼容性**：与 MyBatis-Plus 生态完美集成
4. **灵活性**：支持从简单字段到复杂 SQL 模板的多场景配置

同时，模块也做出了一些工程取舍：
- **牺牲了部分 SQL 兼容性**（如 UNION 查询）
- **增加了配置管理复杂度**
- **引入了 SQL 解析性能开销**

这些取舍在大多数企业应用场景中是合理的，通过合理的配置和优化，模块能够提供稳定、高效的数据权限控制能力。
