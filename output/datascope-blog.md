# 数据权限为什么必须写在 Mapper XML？DataScope 拦截器的工程取舍

> 本文深入分析 Forge Admin 如何通过 `forge-starter-datascope` 模块实现组织、角色、区域等多维度数据权限，核心解答"为什么选择在 Mapper XML 层实现"这一关键工程决策。

---

## 1. 这个问题在企业后台里为什么常见

假设你正在开发一个企业后台系统，不同角色的用户需要看到不同的数据：

- **部门经理**：只能看到本部门的数据
- **区域总监**：能看到本区域所有部门的数据  
- **超级管理员**：能看到全公司的数据
- **跨组织协作**：A部门经理可能需要查看B部门的项目数据（需授权）

传统实现方式通常有三种，各有痛点：

### 方案一：Controller 层硬编码（最差实践）

```java
@GetMapping("/orders")
public Result<List<Order>> getOrders() {
    Long userId = getCurrentUserId();
    User user = userService.getById(userId);
    
    List<Order> orders;
    if (user.isSuperAdmin()) {
        orders = orderService.list();  // 全部数据
    } else if (user.isDeptManager()) {
        orders = orderService.listByDept(user.getDeptId());  // 本部门
    } else {
        orders = orderService.listByUser(userId);  // 个人数据
    }
    
    return Result.ok(orders);
}
```

**问题**：权限逻辑散落各处，每个接口都要重复判断，维护成本高。

### 方案二：Service 层统一处理（中等实践）

```java
@Service
public class OrderService {
    
    public List<Order> list() {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        
        // 权限判断逻辑
        DataScopeContext context = getDataScopeContext();
        if (context.isDeptScope()) {
            wrapper.eq("dept_id", context.getDeptId());
        } else if (context.isUserScope()) {
            wrapper.eq("create_by", context.getUserId());
        }
        // ... 其他条件
        
        return orderMapper.selectList(wrapper);
    }
}
```

**问题**：SQL 与业务代码耦合，复杂 JOIN 查询难以处理，分页统计困难。

### 方案三：AOP 或拦截器（当前方案）

```java
// 只需一个注解
@GetMapping("/orders")
@DataScope(deptAlias = "o", deptField = "dept_id")
public Result<Page<Order>> getOrders(PageParam param) {
    // 业务代码完全不用关心权限
    return Result.ok(orderService.page(param));
}
```

**问题**：需要解决 SQL 自动改写、分页兼容、性能优化等复杂问题。

Forge Admin 选择了方案三，并通过 `forge-starter-datascope` 模块实现了完整的解决方案。

---

## 2. Forge Admin 是怎么解决的

`forge-starter-datascope` 的核心设计是：**在 MyBatis 执行 SQL 前，通过拦截器自动改写 WHERE 条件，透明地追加数据权限过滤**。

### 2.1 整体架构

```
用户请求 → Controller → Service → Mapper
                            ↓
                   MybatisPlusInterceptor
                            ↓
              ┌─────────────┼──────────────┐
              │             │               │
        DataScope     TenantLine     Pagination
       Interceptor   Interceptor    Interceptor
              │
              ├→ 1. 检查是否需要跳过权限
              ├→ 2. 查询当前用户的数据权限上下文
              ├→ 3. 获取当前 Mapper 方法的权限配置
              ├→ 4. 根据权限类型构建 SQL 条件
              └→ 5. 使用 JSQLParser 改写原 SQL
```

### 2.2 七种数据权限范围

模块定义了七种标准权限范围，覆盖了企业应用常见场景：

| 权限类型 | 代码 | 说明 | SQL 条件示例 |
|---------|------|------|-------------|
| **ALL** | 1 | 全部数据 | 无附加条件 |
| **SELF** | 2 | 个人数据 | `user_id = 123` |
| **ORG** | 3 | 本组织 | `org_id IN (101, 102)` |
| **ORG_AND_CHILD** | 4 | 本组织及子组织 | `org_id IN (101, 102, 103, 104)` |
| **CUSTOM** | 5 | 自定义组织 | `org_id IN (自定义组织ID列表)` |
| **TENANT_ALL** | 6 | 本租户 | `tenant_id = 1001` |
| **REGION** | 7 | 本行政区划 | `area_code = '440300' OR area_code IN (子区划)` |

### 2.3 核心模块组成

```
forge-starter-datascope/
├── config/
│   ├── DataScopeAutoConfiguration.java  # 自动配置
│   ├── DataScopeProperties.java         # 配置属性
│   └── DataScopeIgnore.java             # @DataScopeIgnore 注解
├── interceptor/
│   └── DataScopeInterceptor.java        # 核心拦截器
├── context/
│   ├── DataScopeContext.java            # 权限上下文
│   └── DataScopeContextHolder.java      # 上下文持有者
├── service/
│   ├── IDataScopeService.java           # 服务接口
│   └── impl/DataScopeServiceImpl.java   # 服务实现
├── enums/
│   └── DataScopeType.java               # 权限类型枚举
└── entity/                              # 数据库实体
    ├── SysDataScopeConfig.java          # Mapper 权限配置
    └── SysRoleDataScope.java            # 角色-自定义组织关联
```

---

## 3. 核心数据结构与配置协议

### 3.1 权限配置表（sys_data_scope_config）

这是模块的核心配置表，采用 **Mapper 方法级** 的配置粒度：

```sql
CREATE TABLE sys_data_scope_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mapper_id VARCHAR(500) NOT NULL COMMENT 'Mapper方法全限定名',
    table_alias VARCHAR(50) COMMENT '表别名',
    user_id_column VARCHAR(50) COMMENT '用户ID字段名',
    org_id_column VARCHAR(50) COMMENT '组织ID字段名',
    tenant_id_column VARCHAR(50) COMMENT '租户ID字段名',
    region_code_column VARCHAR(50) COMMENT '行政区划代码字段名',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    UNIQUE KEY uk_mapper (mapper_id)
);
```

**配置示例**：

| mapper_id | table_alias | org_id_column | 说明 |
|-----------|-------------|---------------|------|
| `com.example.mapper.OrderMapper.selectList` | `o` | `dept_id` | 订单列表按部门过滤 |
| `com.example.mapper.UserMapper.selectPage` | `u` | `org_id` | 用户列表按组织过滤 |
| `com.example.mapper.ReportMapper.getRegionStats` | `r` | `area_code` | 报表按行政区划过滤 |

### 3.2 权限上下文（DataScopeContext）

拦截器通过 `DataScopeContext` 获取当前用户的权限信息：

```java
public class DataScopeContext {
    private Long userId;           // 当前用户ID
    private List<Long> orgIds;     // 用户所属组织ID列表
    private List<Long> roleIds;    // 用户角色ID列表
    private Integer minDataScope;  // 最小数据权限值（值越小权限越大）
    private Set<Long> customOrgIds; // 自定义组织ID集合
    private Long tenantId;         // 租户ID
    private String regionCode;     // 行政区划代码
    private Integer regionLevel;   // 行政区划级别
    private String regionAncestors; // 行政区划祖先路径
}
```

### 3.3 权限计算规则

**关键规则**：用户拥有多个角色时，取 **最小 data_scope 值**（值越小权限越大）

```sql
-- 用户角色表 sys_user_role
user_id | role_id
--------|--------
1001    | 1      -- 角色1: data_scope = 3 (本组织)
1001    | 2      -- 角色2: data_scope = 4 (本组织及子组织)

-- 最终权限: MIN(3, 4) = 3 (本组织)
```

**特殊处理**：当 data_scope = 5 时，需要检查是否有自定义组织配置：

```java
// DataScopeType.getByRoleDataScope()
public static DataScopeType getByRoleDataScope(Integer code, boolean hasCustomOrgIds) {
    return switch (code) {
        case 5 -> hasCustomOrgIds ? CUSTOM : SELF;  // 关键兼容点
        // ... 其他 case
    };
}
```

---

## 4. 核心实现链路

### 4.1 第一步：拦截器入口

```java
@Component
public class DataScopeInterceptor implements InnerInterceptor {
    
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, 
                          Object parameter, RowBounds rowBounds, 
                          ResultHandler resultHandler, BoundSql boundSql) {
        
        // 1. 检查跳过标记（后台任务等场景）
        if (DataScopeContextHolder.isSkip()) {
            return;
        }
        
        // 2. 获取当前 Mapper 方法ID
        String mapperId = ms.getId();
        
        // 3. 处理分页 Count 查询（去掉 _mpCount 后缀）
        if (mapperId.endsWith("_mpCount")) {
            mapperId = mapperId.replace("_mpCount", "");
        }
        
        // 4. 查询权限配置（带缓存）
        SysDataScopeConfig config = dataScopeService.getDataScopeConfig(mapperId);
        if (config == null || config.getEnabled() == 0) {
            return;  // 未配置或已禁用
        }
        
        // 5. 获取用户权限上下文
        DataScopeContext context = dataScopeService.getCurrentUserDataScope();
        if (context == null) {
            return;  // 未登录或后台任务
        }
        
        // 6. 确定权限类型
        DataScopeType scopeType = DataScopeType.getByRoleDataScope(
            context.getMinDataScope(),
            !CollectionUtils.isEmpty(context.getCustomOrgIds())
        );
        
        // 7. 根据权限类型改写 SQL
        String originalSql = boundSql.getSql();
        String modifiedSql = buildDataScopeSql(originalSql, config, context, scopeType);
        
        // 8. 替换 BoundSql 中的 SQL
        PLUGIN_UTILS.MPBoundSql mpBoundSql = PLUGIN_UTILS.mpBoundSql(boundSql);
        mpBoundSql.sql(modifiedSql);
    }
}
```

### 4.2 第二步：SQL 改写引擎

模块使用 **JSQLParser** 作为 SQL 解析和改写引擎：

```java
private String buildDataScopeSql(String originalSql, SysDataScopeConfig config,
                               DataScopeContext context, DataScopeType scopeType) {
    
    // 1. 解析 SQL 为抽象语法树（AST）
    Statement statement = CCJSqlParserUtil.parse(originalSql);
    if (!(statement instanceof Select)) {
        return originalSql;  // 只处理 SELECT 查询
    }
    
    // 2. 获取 SELECT 主体
    Select select = (Select) statement;
    PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
    
    // 3. 构建权限条件表达式
    Expression dataScopeCondition = buildDataScopeCondition(config, context, scopeType);
    
    // 4. 追加到 WHERE 子句
    Expression where = plainSelect.getWhere();
    if (where != null) {
        // 原 WHERE 条件 AND 权限条件
        plainSelect.setWhere(new AndExpression(where, dataScopeCondition));
    } else {
        plainSelect.setWhere(dataScopeCondition);
    }
    
    // 5. 序列化回 SQL 字符串
    return select.toString();
}
```

### 4.3 第三步：条件构建策略

根据不同的权限类型，构建不同的 SQL 条件：

```java
private Expression buildDataScopeCondition(SysDataScopeConfig config,
                                         DataScopeContext context,
                                         DataScopeType scopeType) {
    switch (scopeType) {
        case SELF:
            // user_id = 123
            return buildSimpleCondition(config.getTableAlias(), 
                                      config.getUserIdColumn(), 
                                      context.getUserId());
            
        case ORG:
            // org_id IN (101, 102)
            return buildInCondition(config.getTableAlias(),
                                  config.getOrgIdColumn(),
                                  context.getOrgIds());
            
        case ORG_AND_CHILD:
            // org_id IN (101, 102, 103, 104...) 包含所有子孙组织
            List<Long> allOrgIds = expandOrgTree(context.getOrgIds());
            return buildInCondition(config.getTableAlias(),
                                  config.getOrgIdColumn(),
                                  allOrgIds);
            
        case CUSTOM:
            // org_id IN (自定义组织列表)
            return buildInCondition(config.getTableAlias(),
                                  config.getOrgIdColumn(),
                                  context.getCustomOrgIds());
            
        case TENANT_ALL:
            // tenant_id = 1001
            return buildSimpleCondition(config.getTableAlias(),
                                      config.getTenantIdColumn(),
                                      context.getTenantId());
            
        case REGION:
            // area_code = '440300' OR area_code IN (子区划)
            return buildRegionCondition(config, context);
            
        case ALL:
        default:
            return null;  // 无附加条件
    }
}
```

### 4.4 第四步：复杂场景处理

#### 场景一：JOIN 查询

```sql
-- 原始 SQL
SELECT o.*, u.name 
FROM t_order o 
LEFT JOIN t_user u ON o.user_id = u.id
WHERE o.status = 'ACTIVE'

-- 配置: table_alias = "o", org_id_column = "dept_id"
-- 用户权限: 部门经理，只能看部门101的数据

-- 改写后 SQL
SELECT o.*, u.name 
FROM t_order o 
LEFT JOIN t_user u ON o.user_id = u.id
WHERE o.status = 'ACTIVE' 
  AND o.dept_id = 101  -- 自动追加
```

#### 场景二：子查询

```sql
-- 原始 SQL
SELECT * FROM t_order 
WHERE id IN (
    SELECT order_id FROM t_order_item WHERE price > 100
)

-- 改写后 (SELF 权限)
SELECT * FROM t_order o
WHERE o.create_by = 12345  -- 自动追加
  AND id IN (
    SELECT order_id FROM t_order_item WHERE price > 100
  )
```

#### 场景三：分页查询

MyBatis-Plus 分页插件会生成 Count 查询，模块自动处理：

```java
// 原始方法: OrderMapper.selectPage
// 生成的 Count 方法: OrderMapper.selectPage_mpCount

// 拦截器处理
if (mapperId.endsWith("_mpCount")) {
    actualMapperId = mapperId.replace("_mpCount", "");  // 还原为原始方法
}
// 使用相同的配置进行权限过滤，确保分页统计准确
```

---

## 5. 关键工程取舍

### 5.1 为什么选择 Mapper XML 层？

这是模块最核心的设计决策，基于三个层面的对比：

| 实现层 | 优点 | 缺点 | Forge Admin 的选择 |
|--------|------|------|-------------------|
| **Controller 层** | 业务语义清晰 | 1. 代码重复<br>2. 易遗漏<br>3. 难以统一维护 | ❌ 否决 |
| **Service 层** | 逻辑集中，可复用 | 1. SQL 与业务耦合<br>2. 复杂查询难处理<br>3. 分页统计困难 | ❌ 否决 |
| **Mapper XML 层** | 1. **SQL 透明化**<br>2. **统一入口**<br>3. **与业务解耦**<br>4. **自动处理分页** | 1. 配置复杂<br>2. 调试困难<br>3. SQL 兼容性问题 | ✅ 选择 |

**核心理由**：
1. **透明化**：业务代码无需感知权限存在，保持简洁
2. **统一性**：所有数据访问都经过相同检查，无遗漏
3. **兼容性**：与 MyBatis-Plus 生态完美集成
4. **性能**：通过缓存和优化，性能开销可控

### 5.2 性能优化策略

#### 两级缓存机制

```java
// 一级缓存：权限配置（30分钟）
private final Cache<String, SysDataScopeConfig> configCache = 
    Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build();

// 二级缓存：组织树展开结果（10分钟）
private final Cache<Long, List<Long>> orgChildCache = 
    Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(500)
        .build();
```

#### 避免 N+1 查询

```java
// 批量查询用户的所有角色权限
public DataScopeContext getCurrentUserDataScope() {
    // 1. 一次查询获取所有角色
    List<Long> roleIds = getCurrentUserRoleIds();
    
    // 2. 一次查询获取最小 data_scope
    Integer minDataScope = roleMapper.selectMinDataScope(roleIds);
    
    // 3. 一次查询获取自定义组织（如果需要）
    Set<Long> customOrgIds = null;
    if (DataScopeType.CUSTOM.getCode().equals(minDataScope)) {
        customOrgIds = roleDataScopeMapper.selectOrgIdsByRoleIds(roleIds);
    }
    
    // 构建上下文
    return DataScopeContext.builder()
        .roleIds(roleIds)
        .minDataScope(minDataScope)
        .customOrgIds(customOrgIds)
        .build();
}
```

### 5.3 与租户权限的优先级

数据权限和租户权限都是通过拦截器实现的，执行顺序很重要：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor(
        @Autowired(required = false) List<InnerInterceptor> innerInterceptors) {
    
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    
    // 1. 数据权限拦截器（先执行）
    if (innerInterceptors != null) {
        // DataScopeInterceptor 应该先于 TenantLineInnerInterceptor
        innerInterceptors.forEach(interceptor::addInnerInterceptor);
    }
    
    // 2. 分页拦截器
    interceptor.addInnerInterceptor(paginationInnerInterceptor());
    
    return interceptor;
}
```

**设计原则**：先业务逻辑（数据权限），后技术隔离（租户隔离）。

### 5.4 已知限制与应对

#### 限制一：UNION 查询不支持自动改写

```sql
-- 原始 UNION 查询
SELECT * FROM t_order_2023
UNION ALL
SELECT * FROM t_order_2024

-- 模块无法自动处理，需要手动在 SQL 中处理权限
SELECT * FROM t_order_2023 WHERE dept_id = 101
UNION ALL  
SELECT * FROM t_order_2024 WHERE dept_id = 101
```

**应对**：对于 UNION 查询，建议在业务层处理权限，或使用视图封装。

#### 限制二：存储过程/函数调用不支持

**应对**：存储过程内部需要自行实现权限逻辑。

#### 限制三：动态表名不支持

MyBatis 的动态表名（如 `${tableName}`）在拦截器执行时已替换，可以正常处理。

---

## 6. 如何二开：扩展数据权限能力

### 6.1 新增权限类型

假设需要增加"项目维度"的权限控制：

```java
// 1. 扩展 DataScopeType 枚举
public enum DataScopeType {
    // ... 原有类型
    PROJECT(8, "项目数据"),
    PROJECT_AND_CHILD(9, "项目及子项目");
    
    // 2. 修改 getByRoleDataScope() 方法
    public static DataScopeType getByRoleDataScope(Integer code, 
                                                  boolean hasCustomOrgIds,
                                                  boolean hasProjectAuth) {
        // 新增逻辑
        if (code == 8 && hasProjectAuth) {
            return PROJECT;
        }
        // ... 原有逻辑
    }
}

// 3. 扩展 DataScopeContext
public class DataScopeContext {
    // 新增字段
    private List<Long> projectIds;
    private Set<Long> customProjectIds;
}

// 4. 修改拦截器条件构建
private Expression buildDataScopeCondition(...) {
    switch (scopeType) {
        case PROJECT:
            return buildInCondition(config.getTableAlias(),
                                  config.getProjectIdColumn(),
                                  context.getProjectIds());
        // ... 其他 case
    }
}
```

### 6.2 自定义 SQL 改写逻辑

```java
@Component
public class CustomDataScopeInterceptor extends DataScopeInterceptor {
    
    @Override
    protected Expression buildRegionCondition(SysDataScopeConfig config,
                                            DataScopeContext context) {
        // 自定义行政区划处理逻辑
        if ("RECURSIVE".equals(config.getRegionStrategy())) {
            // 使用递归 CTE 查询所有下级区划
            String cteSql = """
                WITH RECURSIVE region_tree AS (
                    SELECT code FROM sys_region WHERE code = '%s'
                    UNION ALL
                    SELECT r.code FROM sys_region r 
                    INNER JOIN region_tree rt ON r.parent_code = rt.code
                )
                """.formatted(context.getRegionCode());
            
            String condition = config.getTableAlias() + "." + 
                             config.getRegionCodeColumn() + 
                             " IN (SELECT code FROM region_tree)";
            
            return CCJSqlParserUtil.parseExpression(condition);
        }
        
        // 默认实现
        return super.buildRegionCondition(config, context);
    }
}
```

### 6.3 集成外部权限系统

```java
@Component
@Primary  // 替换默认实现
public class ExternalDataScopeService implements IDataScopeService {
    
    @Autowired
    private ExternalAuthClient authClient;
    
    @Override
    public DataScopeContext getCurrentUserDataScope() {
        // 从外部权限系统获取权限信息
        ExternalUserAuth auth = authClient.getCurrentUserAuth();
        
        return DataScopeContext.builder()
            .userId(auth.getUserId())
            .orgIds(auth.getAuthorizedOrgs())
            .roleIds(auth.getRoleIds())
            .minDataScope(convertToInternalScope(auth.getDataScope()))
            .build();
    }
    
    private Integer convertToInternalScope(ExternalDataScope externalScope) {
        // 映射外部权限到内部枚举
        return switch (externalScope) {
            case ALL -> DataScopeType.ALL.getCode();
            case DEPARTMENT -> DataScopeType.ORG.getCode();
            // ... 其他映射
        };
    }
}
```

### 6.4 性能监控与调优

```yaml
# application.yml 配置
forge:
  datascope:
    enabled: true
    print-sql: false  # 生产环境关闭
    metrics:
      enabled: true
      slow-threshold-ms: 100      # 慢查询阈值
      cache-stats-interval: 60    # 缓存统计间隔（秒）
      
    # 性能调优参数
    cache:
      config-ttl: 1800           # 配置缓存时间（秒）
      org-tree-ttl: 600          # 组织树缓存时间
      max-size: 1000             # 最大缓存条目数
```

添加监控指标：

```java
@Component
public class DataScopeMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // 拦截器执行时间
    private final Timer interceptorTimer;
    
    // 缓存命中率
    private final FunctionCounter configCacheHits;
    private final FunctionCounter configCacheMisses;
    
    public DataScopeMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.interceptorTimer = Timer.builder("datascope.interceptor.time")
            .description("数据权限拦截器执行时间")
            .register(meterRegistry);
            
        this.configCacheHits = FunctionCounter.builder("datascope.cache.hits")
            .description("配置缓存命中次数")
            .register(meterRegistry);
    }
    
    public void recordInterceptorTime(long nanos) {
        interceptorTimer.record(nanos, TimeUnit.NANOSECONDS);
    }
}
```

---

## 7. 体验入口和下一篇预告

### 体验 Forge Admin 数据权限功能

- **在线演示**：[Forge Admin 后台管理](http://81.70.22.48:8084)
- **默认账号**：admin / 123456
- **数据权限体验**：
  1. 使用不同角色账号登录
  2. 查看同一数据列表的过滤效果
  3. 在系统管理配置数据权限规则
- **Gitee**：[ForgeLab/forge-admin](https://gitee.com/ForgeLab/forge-admin)
- **GitHub**：[yaomindong1996/forge-admin](https://github.com/yaomindong1996/forge-admin)
- **文档站**：[Forge Admin Docs](https://forge-admin.yomindong.com)

### 核心代码位置

```bash
forge-starter-datascope/
├── src/main/java/com/mdframe/forge/starter/datascope/
│   ├── interceptor/DataScopeInterceptor.java      # 核心拦截器
│   ├── service/IDataScopeService.java             # 权限服务接口
│   ├── context/DataScopeContext.java              # 权限上下文
│   └── enums/DataScopeType.java                   # 权限类型枚举
```

### 下一篇预告

下一篇我们将探讨 **《后台接口加解密实践：什么时候该用 @ApiEncrypt 和 @ApiDecrypt》**，分析 Forge Admin 如何通过 `forge-starter-crypto` 模块实现敏感数据的传输保护，包括前端自动加密、后端自动解密、密钥管理、性能优化等完整方案。

---

## 总结

Forge Admin 的 `forge-starter-datascope` 模块通过以下设计实现了优雅的数据权限控制：

1. **透明化改写**：在 Mapper 层自动改写 SQL，业务代码零感知
2. **统一配置**：通过数据库表集中管理权限规则
3. **七种标准类型**：覆盖企业应用常见场景
4. **性能优化**：两级缓存 + 批量查询，性能开销可控
5. **灵活扩展**：支持自定义权限类型和 SQL 改写逻辑

**核心工程价值**：将数据权限从业务逻辑中彻底解耦，通过配置化的方式实现统一、一致、可维护的权限控制，让开发者专注于业务实现，而不是重复的权限判断代码。

> 本文基于 Forge Admin 2.x 版本，代码分析来自 `forge-starter-datascope` 模块。实际使用请参考最新文档和示例。
