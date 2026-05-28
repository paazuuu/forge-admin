# 多租户后台怎么做数据隔离？从 tenant_id 到拦截器的完整链路

> 本文深入探讨 Forge Admin 如何通过 `forge-starter-tenant` 模块实现多租户数据隔离，核心解决"租户隔离如何避免业务代码散落判断"的问题。

---

## 1. 这个问题在企业后台里为什么常见

想象这样一个场景：你正在开发一个 SaaS 后台系统，需要服务多个租户（公司/组织）。每个租户的数据必须严格隔离——A 公司不能看到 B 公司的客户信息，C 部门不能访问 D 部门的审批记录。

传统做法是在每个业务查询中手动添加 `WHERE tenant_id = ?`：

```java
// 业务代码中到处散落的租户判断
public List<Customer> getCustomers(Long tenantId) {
    return customerMapper.selectList(
        new QueryWrapper<Customer>()
            .eq("tenant_id", tenantId)  // 每个查询都要手动加
            .eq("status", "ACTIVE")
    );
}

public void updateOrder(Long tenantId, Long orderId, Order order) {
    order.setTenantId(tenantId);  // 每个更新都要手动设置
    orderMapper.updateById(order);
}
```

这种做法有四个明显问题：

1. **代码重复**：每个查询都要手动添加租户条件
2. **容易遗漏**：忘记加租户条件会导致数据泄露
3. **维护困难**：业务逻辑和租户逻辑耦合
4. **扩展性差**：切换多租户策略（如独立数据库）需要重构所有业务代码

Forge Admin 的解决方案是：**将租户隔离从业务逻辑中解耦，通过拦截器和上下文自动处理**。

---

## 2. Forge Admin 是怎么解决的

`forge-starter-tenant` 模块实现了从请求到数据库的完整链路：

```
用户请求 → Web拦截器 → 租户上下文 → MyBatis拦截器 → SQL自动注入
```

### 2.1 核心模块概览

| 模块 | 职责 | 关键文件 |
|------|------|----------|
| **Web层** | 从请求提取租户ID | `TenantInterceptor.java` |
| **上下文层** | 线程安全存储租户ID | `TenantContextHolder.java` |
| **拦截器层** | SQL自动注入租户条件 | `TenantLineInnerInterceptor.java` |
| **处理器层** | 租户策略实现 | `DefaultTenantLineHandler.java` |
| **检测层** | 自动识别表结构 | `TenantTableChecker.java` |
| **切面层** | 忽略租户注解处理 | `IgnoreTenantAspect.java` |

### 2.2 设计原则

1. **透明化**：业务代码无需感知租户存在
2. **自动化**：SQL自动注入租户条件
3. **可配置**：支持多种忽略策略
4. **高性能**：线程本地存储，无额外IO

---

## 3. 核心数据结构与配置协议

### 3.1 租户上下文：线程安全的存储

```java
public class TenantContextHolder {
    // 使用 TransmittableThreadLocal 支持线程池
    private static final TransmittableThreadLocal<Long> TENANT_ID_HOLDER = 
        new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<Boolean> IGNORE_TENANT = 
        new TransmittableThreadLocal<>();
    
    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }
    
    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }
    
    // 临时忽略租户过滤
    public static <T> T executeIgnore(Supplier<T> supplier) {
        Boolean oldIgnore = IGNORE_TENANT.get();
        try {
            IGNORE_TENANT.set(true);
            return supplier.get();
        } finally {
            IGNORE_TENANT.set(oldIgnore);
        }
    }
}
```

### 3.2 配置协议：YAML 配置

```yaml
forge:
  tenant:
    enabled: true                    # 是否启用多租户
    column: tenant_id               # 租户字段名（默认）
    strict-mode: false              # 严格模式：无租户ID时是否抛异常
    auto-detect-tenant-column: true # 自动检测表结构
    ignore-tables:                  # 手动忽略的表
      - sys_tenant                  # 租户表本身
      - sys_config                  # 系统配置
      - sys_dict_*                  # 字典表
```

### 3.3 忽略租户注解

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreTenant {
    boolean value() default true;
}

// 使用示例
@RestController
@IgnoreTenant  // 整个控制器忽略租户过滤
public class SystemController {
    
    @GetMapping("/config")
    @IgnoreTenant  // 或单个方法忽略
    public Result<?> getConfig() {
        // 这里查询 sys_config 表不会添加 tenant_id 条件
        return Result.ok(configService.list());
    }
}
```

### 3.4 数据库规范

所有需要租户隔离的表必须包含 `tenant_id` 字段：

```sql
-- 标准表结构
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    -- ... 其他字段
    
    INDEX idx_tenant_id (tenant_id),  -- 必须为 tenant_id 建索引
    INDEX idx_tenant_username (tenant_id, username)  -- 推荐联合索引
) COMMENT='用户表';
```

---

## 4. 核心实现链路

### 4.1 第一步：Web 拦截器提取租户ID

```java
@Component
public class TenantInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) {
        // 1. 检查是否需要忽略租户
        if (shouldIgnoreTenant(handler)) {
            TenantContextHolder.setIgnore(true);
            return true;
        }
        
        // 2. 提取租户ID（优先级从高到低）
        Long tenantId = null;
        
        // 2.1 从登录会话获取（需要 forge-starter-auth）
        if (SessionHelper.isLogin()) {
            tenantId = SessionHelper.getTenantId();
        }
        
        // 2.2 从请求头获取（API调用场景）
        if (tenantId == null) {
            String tenantHeader = request.getHeader("X-Tenant-Id");
            if (StringUtils.hasText(tenantHeader)) {
                tenantId = Long.parseLong(tenantHeader);
            }
        }
        
        // 2.3 从请求参数获取（兼容旧系统）
        if (tenantId == null) {
            String tenantParam = request.getParameter("tenantId");
            if (StringUtils.hasText(tenantParam)) {
                tenantId = Long.parseLong(tenantParam);
            }
        }
        
        // 3. 设置到上下文
        if (tenantId != null) {
            TenantContextHolder.setTenantId(tenantId);
        } else if (tenantProperties.isStrictMode()) {
            // 严格模式：无租户ID时抛出异常
            throw new TenantNotFoundException("租户ID不能为空");
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                              HttpServletResponse response, 
                              Object handler, 
                              Exception ex) {
        // 请求结束后清理上下文，防止内存泄漏
        TenantContextHolder.clear();
    }
}
```

### 4.2 第二步：MyBatis 拦截器自动注入 SQL

```java
@Component
public class DefaultTenantLineHandler implements TenantLineHandler {
    
    @Override
    public Expression getTenantId() {
        // 从上下文获取租户ID
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            // 无租户ID时返回默认值（如0）
            return new LongValue(0L);
        }
        return new LongValue(tenantId);
    }
    
    @Override
    public String getTenantIdColumn() {
        // 返回配置的租户字段名
        return tenantProperties.getColumn();
    }
    
    @Override
    public boolean ignoreTable(String tableName) {
        // 五级忽略策略（优先级从高到低）
        
        // 1. 上下文标记忽略
        if (Boolean.TRUE.equals(TenantContextHolder.isIgnore())) {
            return true;
        }
        
        // 2. 手动配置的忽略表
        if (tenantProperties.getIgnoreTables().contains(tableName)) {
            return true;
        }
        
        // 3. 通配符匹配（如 sys_dict_*）
        for (String pattern : tenantProperties.getIgnoreTables()) {
            if (pattern.contains("*") && tableName.matches(
                pattern.replace("*", ".*"))) {
                return true;
            }
        }
        
        // 4. 自动检测结果
        if (tenantTableChecker != null && 
            !tenantTableChecker.hasTenantColumn(tableName)) {
            return true;
        }
        
        // 5. 默认不忽略
        return false;
    }
}
```

### 4.3 第三步：SQL 自动注入效果

```sql
-- 业务代码中的查询
List<User> users = userMapper.selectList(
    new QueryWrapper<User>().eq("status", "ACTIVE")
);

-- 实际执行的 SQL（自动注入 tenant_id 条件）
SELECT * FROM sys_user 
WHERE status = 'ACTIVE' 
  AND tenant_id = 1001;  -- 自动添加

-- 插入操作同样自动设置
INSERT INTO sys_user (username, tenant_id) 
VALUES ('admin', 1001);  -- 自动设置 tenant_id
```

### 4.4 第四步：自动表结构检测

```java
@Component
@ConditionalOnProperty(prefix = "forge.tenant", 
                      name = "auto-detect-tenant-column", 
                      havingValue = "true")
public class TenantTableChecker implements SmartInitializingSingleton {
    
    private final Map<String, Boolean> tableCache = new ConcurrentHashMap<>();
    
    @Override
    public void afterSingletonsInstantiated() {
        // 应用启动时扫描所有表结构
        List<String> allTables = getDatabaseTables();
        
        for (String table : allTables) {
            boolean hasTenantColumn = checkTableHasColumn(table, "tenant_id");
            tableCache.put(table, hasTenantColumn);
        }
    }
    
    public boolean hasTenantColumn(String tableName) {
        return tableCache.getOrDefault(tableName, false);
    }
}
```

---

## 5. 关键取舍和坑

### 5.1 设计取舍一：共享表 vs 独立数据库

**选择**：共享表 + `tenant_id` 字段隔离

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **共享表** | 实现简单，维护成本低，便于全局统计 | 单表数据量大，性能问题 | 中小型 SaaS，租户数量 < 1000 |
| **独立 Schema** | 数据物理隔离，性能好 | 迁移复杂，连接管理复杂 | 中大型 SaaS，租户数据量大 |
| **独立数据库** | 完全隔离，安全性最高 | 成本高，运维复杂 | 金融、医疗等高安全要求 |

Forge Admin 选择共享表方案，因为：
1. 大多数 SaaS 场景租户数据量不大
2. 通过索引优化可以解决性能问题
3. 便于实现跨租户统计和管理功能

### 5.2 设计取舍二：ThreadLocal 存储策略

**选择**：`TransmittableThreadLocal` 而非普通 `ThreadLocal`

```java
// 普通 ThreadLocal 的问题
ThreadLocal<Long> tenantHolder = new ThreadLocal<>();

// 线程池场景下，线程复用会导致租户ID混乱
executorService.submit(() -> {
    tenantHolder.set(1001L);
    // 任务执行中...
    // 线程被回收后，下一个任务可能拿到错误的租户ID
});

// TransmittableThreadLocal 解决方案
TransmittableThreadLocal<Long> tenantHolder = new TransmittableThreadLocal<>();

// 通过包装任务传递上下文
executorService.submit(
    TtlRunnable.get(() -> {
        tenantHolder.set(1001L);
        // 上下文正确传递
    })
);
```

### 5.3 设计取舍三：SQL 拦截层级

**选择**：MyBatis-Plus 的 `StatementHandler` 层拦截

| 拦截层级 | 优点 | 缺点 |
|----------|------|------|
| **Executor** | 拦截早，控制力强 | 无法处理复杂 SQL |
| **StatementHandler** | 支持所有 SQL 操作，自动处理 JOIN/子查询 | 依赖 MyBatis-Plus |
| **ParameterHandler** | 只处理参数，简单 | 无法处理 WHERE 条件 |

Forge Admin 使用 MyBatis-Plus 原生拦截器，因为：
1. 与 MyBatis-Plus 生态深度集成
2. 自动处理复杂 SQL 场景
3. 社区活跃，长期维护

### 5.4 常见坑与解决方案

#### 坑1：分页查询总数错误

```sql
-- 错误：分页插件和租户插件顺序问题
SELECT COUNT(*) FROM sys_user;  -- 没有租户条件

-- 解决方案：正确配置拦截器顺序
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    
    // 1. 租户拦截器（必须先于分页拦截器）
    interceptor.addInnerInterceptor(tenantLineInnerInterceptor());
    
    // 2. 分页拦截器
    interceptor.addInnerInterceptor(paginationInnerInterceptor());
    
    return interceptor;
}
```

#### 坑2：跨租户查询忘记忽略

```java
// 错误：跨租户统计时忘记忽略租户
public Integer getTotalUserCount() {
    // 这里会添加 tenant_id = 当前租户 的条件
    return userMapper.selectCount(new QueryWrapper<>());
}

// 正确：使用 @IgnoreTenant 或 executeIgnore
@IgnoreTenant
public Integer getTotalUserCount() {
    return userMapper.selectCount(new QueryWrapper<>());
}

// 或编程式忽略
public Integer getTotalUserCount() {
    return TenantContextHolder.executeIgnore(() -> 
        userMapper.selectCount(new QueryWrapper<>())
    );
}
```

#### 坑3：批量操作性能问题

```java
// 错误：循环中单条插入
for (User user : userList) {
    userMapper.insert(user);  // 每次都会解析 SQL
}

// 正确：批量插入
userMapper.insertBatch(userList);  // 一次 SQL，性能提升 10x+

// 更优：手动设置租户ID
List<User> users = new ArrayList<>();
for (User user : userList) {
    user.setTenantId(TenantContextHolder.getTenantId());
    users.add(user);
}
userMapper.insertBatch(users);
```

#### 坑4：索引缺失导致性能下降

```sql
-- 错误：没有为 tenant_id 建索引
SELECT * FROM sys_user WHERE tenant_id = 1001 AND status = 'ACTIVE';
-- 全表扫描，性能极差

-- 正确：创建合适索引
ALTER TABLE sys_user 
ADD INDEX idx_tenant_status (tenant_id, status);

-- 最佳实践：联合索引将 tenant_id 放最前面
ALTER TABLE sys_user 
ADD INDEX idx_tenant_username (tenant_id, username),
ADD INDEX idx_tenant_email (tenant_id, email);
```

---

## 6. 如何二开：扩展多租户能力

### 6.1 自定义租户ID来源

```java
@Component
public class JwtTenantInterceptor extends TenantInterceptor {
    
    @Override
    protected Long extractTenantId(HttpServletRequest request) {
        // 从 JWT Token 中提取租户ID
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token)) {
            return jwtParser.parseTenantId(token);
        }
        return super.extractTenantId(request);
    }
}
```

### 6.2 支持多租户策略切换

```java
public interface TenantStrategy {
    String getDatabaseKey(Long tenantId);
    String getSchemaName(Long tenantId);
    String getTableName(String originalTable, Long tenantId);
}

@Component
@ConditionalOnProperty(name = "forge.tenant.strategy", 
                      havingValue = "database")
public class DatabaseTenantStrategy implements TenantStrategy {
    
    @Override
    public String getDatabaseKey(Long tenantId) {
        return "tenant_db_" + tenantId;  // 动态数据源
    }
    
    // 使用 AbstractRoutingDataSource 实现动态数据源切换
}
```

### 6.3 租户数据迁移工具

```java
@Component
public class TenantDataMigrator {
    
    public void migrateData(Long sourceTenantId, Long targetTenantId) {
        // 1. 临时忽略租户过滤
        TenantContextHolder.executeIgnore(() -> {
            // 2. 查询源租户数据
            List<User> users = userMapper.selectList(
                new QueryWrapper<User>().eq("tenant_id", sourceTenantId)
            );
            
            // 3. 批量插入到目标租户
            for (User user : users) {
                user.setId(null);  // 清除主键
                user.setTenantId(targetTenantId);
            }
            userMapper.insertBatch(users);
        });
    }
}
```

### 6.4 租户级别的数据统计

```java
@RestController
@RequestMapping("/admin/tenant-stats")
public class TenantStatsController {
    
    @GetMapping("/summary")
    @IgnoreTenant  // 需要跨租户查询
    public Result<Map<Long, TenantStats>> getTenantSummary() {
        Map<Long, TenantStats> stats = new HashMap<>();
        
        // 查询所有租户
        List<Tenant> tenants = tenantMapper.selectList(new QueryWrapper<>());
        
        for (Tenant tenant : tenants) {
            // 为每个租户执行查询
            TenantStats stat = TenantContextHolder.executeWithTenant(
                tenant.getId(),
                () -> {
                    TenantStats s = new TenantStats();
                    s.setUserCount(userMapper.selectCount(new QueryWrapper<>()));
                    s.setOrderCount(orderMapper.selectCount(new QueryWrapper<>()));
                    return s;
                }
            );
            stats.put(tenant.getId(), stat);
        }
        
        return Result.ok(stats);
    }
}
```

---

## 7. 体验入口和下一篇预告

### 体验 Forge Admin 多租户功能

- **在线演示**：[Forge Admin 后台管理](http://81.70.22.48:8084)
- **默认账号**：admin / 123456
- **多租户体验**：登录后查看不同租户的数据隔离效果
- **Gitee**：[ForgeLab/forge-admin](https://gitee.com/ForgeLab/forge-admin)
- **GitHub**：[yaomindong1996/forge-admin](https://github.com/yaomindong1996/forge-admin)
- **文档站**：[Forge Admin Docs](https://forge-admin.yomindong.com)

### 核心代码位置

```bash
forge-starter-tenant/
├── src/main/java/com/mdframe/forge/starter/tenant/
│   ├── interceptor/TenantInterceptor.java      # Web拦截器
│   ├── context/TenantContextHolder.java       # 上下文
│   ├── handler/DefaultTenantLineHandler.java  # 租户处理器
│   └── aspect/IgnoreTenantAspect.java         # 忽略切面
```

### 下一篇预告

下一篇我们将深入探讨 **《数据权限为什么必须写在 Mapper XML？DataScope 拦截器的工程取舍》**，分析 Forge Admin 如何通过 `forge-starter-datascope` 模块实现组织、角色、区域等多维度的数据权限控制，以及为什么选择 MyBatis XML 作为数据权限的改写目标。

---

## 总结

Forge Admin 的 `forge-starter-tenant` 模块通过以下设计实现了优雅的多租户数据隔离：

1. **透明化处理**：业务代码无需关心租户逻辑
2. **完整链路**：请求→拦截器→上下文→SQL注入
3. **灵活配置**：五级忽略策略，支持多种场景
4. **性能优化**：线程本地存储 + 自动检测 + 索引规范
5. **安全可靠**：严格模式防止数据泄露

核心价值：**将租户隔离从业务逻辑中解耦，让开发者专注于业务实现，而不是重复的租户判断代码**。

> 本文基于 Forge Admin 2.x 版本，代码分析来自 `forge-starter-tenant` 模块。实际使用请参考最新文档和示例。
