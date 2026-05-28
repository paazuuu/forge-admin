# Forge-Starter-Tenant 多租户数据隔离分析报告

## 模块概述

`forge-starter-tenant` 是一个基于 MyBatis-Plus 多租户插件的完整多租户解决方案，实现了从请求拦截到 SQL 自动注入的完整链路，核心目标是**避免业务代码中散落租户判断逻辑**。

## 模块文件清单

### 核心文件
```
forge-starter-tenant/
├── src/main/java/com/mdframe/forge/starter/tenant/
│   ├── aspect/
│   │   └── IgnoreTenantAspect.java          # 租户忽略注解切面
│   ├── config/
│   │   ├── TenantAutoConfiguration.java     # 自动配置类
│   │   └── TenantProperties.java           # 配置属性类
│   ├── context/
│   │   └── TenantContextHolder.java        # 租户上下文持有者
│   ├── core/
│   │   └── TenantEntity.java               # 租户实体基类
│   ├── handler/
│   │   ├── DefaultTenantLineHandler.java   # 租户处理器
│   │   └── TenantTableChecker.java         # 表结构检测器
│   ├── interceptor/
│   │   └── TenantInterceptor.java          # Web拦截器
│   └── util/
│       └── TenantUtil.java                 # 工具类
├── src/main/resources/
│   ├── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   ├── sql/tenant_migration.sql           # 数据库迁移脚本
│   └── tenant-config-example.yml          # 配置示例
├── TENANT_USAGE.md                        # 使用文档
└── pom.xml
```

### 依赖文件
```
forge-starter-core/src/main/java/com/mdframe/forge/starter/core/annotation/tenant/IgnoreTenant.java
forge-starter-orm/src/main/java/com/mdframe/forge/starter/orm/config/MybatisPlusConfig.java
```

## 核心类职责说明

### 1. TenantInterceptor - 租户Web拦截器
**职责**：从请求中提取租户ID并设置到上下文
- 优先级：在认证拦截器之后执行（order=10）
- 租户ID来源：
  1. 优先从 `SessionHelper.getTenantId()` 获取（需要 forge-starter-auth）
  2. 备选从请求头 `X-Tenant-Id` 获取
- 忽略机制：
  1. 检查 `@IgnoreTenant` 注解
  2. 检查 API 配置中的 `needTenant` 标志
  3. 设置 `TenantContextHolder.setIgnore(true)`
- 生命周期：请求结束后自动清除上下文

### 2. TenantContextHolder - 租户上下文持有者
**职责**：线程安全的租户ID存储和传递
- 实现：使用 `TransmittableThreadLocal` 支持线程池场景
- 核心字段：
  - `TENANT_ID_HOLDER`：存储当前线程的租户ID
  - `IGNORE_TENANT`：标记是否忽略租户过滤
- 关键方法：
  - `executeIgnore()`：临时忽略租户执行操作
  - `executeWithTenant()`：使用指定租户ID执行操作
  - `setIgnore()`：设置忽略标记

### 3. DefaultTenantLineHandler - 租户处理器
**职责**：实现 MyBatis-Plus 的 `TenantLineHandler` 接口
- 核心方法：
  - `getTenantId()`：从 `TenantContextHolder` 获取租户ID
  - `getTenantIdColumn()`：返回配置的租户字段名（默认 `tenant_id`）
  - `ignoreTable()`：判断表是否需要忽略租户过滤
- 忽略表判断优先级：
  1. 上下文设置了忽略标记
  2. 手动配置的忽略表列表
  3. 手动添加到缓存的忽略表
  4. 自动检测（通过 `TenantTableChecker`）

### 4. TenantTableChecker - 表结构检测器
**职责**：自动扫描数据库表结构，检测哪些表包含租户字段
- 实现：`SmartInitializingSingleton`，在单例Bean初始化完成后执行
- 机制：
  1. 扫描所有表，检查是否包含 `tenant_id` 字段
  2. 缓存包含/不包含租户字段的表集合
  3. 支持实时查询和缓存刷新
- 优势：无需手动配置 `ignore-tables`，自动适配表结构

### 5. IgnoreTenantAspect - 忽略租户切面
**职责**：处理 `@IgnoreTenant` 注解
- 拦截标记了 `@IgnoreTenant` 注解的方法
- 使用 `TenantContextHolder.executeIgnore()` 包装方法执行
- 优先级较高（`@Order(1)`），确保在其他切面之前执行

### 6. TenantAutoConfiguration - 自动配置类
**职责**：Spring Boot 自动配置
- 条件：`forge.tenant.enabled=true`（默认开启）
- 注册的Bean：
  1. `DefaultTenantLineHandler`：租户处理器
  2. `TenantLineInnerInterceptor`：MyBatis-Plus 租户拦截器
  3. `TenantInterceptor`：Web拦截器
  4. `IgnoreTenantAspect`：注解切面
  5. `TenantTableChecker`：表检测器（条件开启）
- Web配置：注册 `TenantInterceptor` 到拦截器链

### 7. TenantProperties - 配置属性
**职责**：多租户配置参数
```yaml
forge:
  tenant:
    enabled: true                    # 是否启用
    column: tenant_id               # 租户字段名
    strict-mode: false              # 严格模式
    auto-detect-tenant-column: true # 自动检测租户字段
    ignore-tables:                  # 手动忽略的表
      - sys_tenant
      - sys_config
      # ... 其他系统表
```

### 8. TenantEntity - 租户实体基类
**职责**：提供租户字段的实体基类
```java
public class TenantEntity extends BaseEntity {
    private Long tenantId;  // 租户编号
}
```

### 9. @IgnoreTenant - 忽略租户注解
**职责**：标记不需要租户隔离的类或方法
```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreTenant {
    boolean value() default true;
}
```

## 完整链路分析

### 请求 → 拦截器 → 上下文 → SQL注入 完整链路

```
1. 用户请求到达
   ↓
2. TenantInterceptor.preHandle()
   ├── 检查 @IgnoreTenant 注解
   ├── 检查 API 配置 needTenant 标志
   ├── 从 SessionHelper 获取租户ID
   ├── 从请求头 X-Tenant-Id 获取（备选）
   └── 设置 TenantContextHolder.setTenantId(tenantId)
   ↓
3. 业务逻辑执行
   ├── 调用 Mapper 方法
   ├── MyBatis-Plus 拦截 SQL
   └── TenantLineInnerInterceptor 处理
   ↓
4. DefaultTenantLineHandler.ignoreTable()
   ├── 检查 TenantContextHolder.isIgnore()
   ├── 检查配置的 ignore-tables
   ├── 检查自动检测结果
   └── 决定是否添加租户条件
   ↓
5. DefaultTenantLineHandler.getTenantId()
   ├── 从 TenantContextHolder.getTenantId()
   └── 转换为 SQL 表达式
   ↓
6. SQL 自动注入
   原始: SELECT * FROM sys_user WHERE username = 'admin'
   注入后: SELECT * FROM sys_user WHERE username = 'admin' AND tenant_id = 1001
   ↓
7. 请求结束
   └── TenantInterceptor.afterCompletion() 清除上下文
```

### 数据库规范

#### 表设计规范
1. **租户字段名**：`tenant_id`（可配置）
2. **字段类型**：`BIGINT`（对应 Java `Long`）
3. **索引要求**：必须为 `tenant_id` 添加索引
4. **联合索引**：将 `tenant_id` 放在索引最前面
5. **默认值**：建议设置默认值（如 `DEFAULT 0`）

#### 迁移脚本示例
```sql
-- 添加租户字段
ALTER TABLE sys_user
ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER id,
ADD INDEX idx_tenant_id (tenant_id);

-- 创建租户表
CREATE TABLE sys_tenant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '租户ID',
    tenant_name VARCHAR(100) NOT NULL COMMENT '租户名称',
    -- ... 其他字段
);
```

#### 系统表分类
1. **需要租户隔离**：用户、角色、组织、业务数据等
2. **不需要租户隔离**：
   - 租户表本身（`sys_tenant`）
   - 系统配置表（`sys_config`、`sys_dict_*`）
   - 文件存储配置（`sys_file_storage_config`）
   - 任务配置（`sys_job_config`）
   - API配置（`sys_api_config`）

## 设计亮点与取舍分析

### 1. 租户ID存储策略
**设计**：使用 `TransmittableThreadLocal` 存储租户ID
- **优点**：
  - 支持线程池场景下的上下文传递
  - 避免每次请求都从Session解析
  - 性能高，无额外IO开销
- **取舍**：
  - 依赖阿里 TTL 库
  - 需要确保上下文及时清理（内存泄漏风险）

### 2. 多租户策略
**设计**：共享表 + 字段隔离（`tenant_id` 字段）
- **优点**：
  - 实现简单，维护成本低
  - 数据集中，便于全局统计
  - 支持跨租户查询（通过 `@IgnoreTenant`）
- **取舍**：
  - 单表数据量大时性能问题
  - 不支持租户级别的数据库备份
  - 所有租户共享数据库连接

### 3. SQL拦截层级
**设计**：MyBatis-Plus 的 `TenantLineInnerInterceptor`
- **实现层级**：在 `StatementHandler` 层拦截
- **优点**：
  - 与 MyBatis-Plus 生态深度集成
  - 支持所有 SQL 操作（SELECT/INSERT/UPDATE/DELETE）
  - 自动处理复杂 SQL（JOIN、子查询）
- **取舍**：
  - 依赖 MyBatis-Plus 版本
  - 无法拦截原生 JDBC 操作
  - 需要配置正确的拦截器顺序

### 4. 忽略租户机制
**设计**：多级忽略策略
1. **注解级**：`@IgnoreTenant` 标记类或方法
2. **配置级**：`ignore-tables` 配置忽略表
3. **自动检测**：`TenantTableChecker` 自动识别
4. **上下文级**：`TenantContextHolder.setIgnore(true)`
5. **API配置级**：通过 `IApiConfigManager` 动态配置

**需要跳过租户过滤的场景**：
- 登录认证（无租户上下文）
- 系统初始化
- 全局配置查询
- 租户管理功能
- 跨租户数据统计

### 5. 自动检测 vs 手动配置
**设计**：支持两种模式，默认开启自动检测
- **自动检测模式**：
  - 优点：无需手动维护忽略表列表
  - 缺点：启动时扫描数据库，增加启动时间
- **手动配置模式**：
  - 优点：启动快，配置明确
  - 缺点：需要手动维护，容易遗漏

### 6. 严格模式 vs 宽松模式
**设计**：通过 `strict-mode` 配置
- **严格模式**（`strict-mode=true`）：
  - 无租户ID时抛出异常
  - 确保数据安全，防止数据泄露
- **宽松模式**（`strict-mode=false`）：
  - 无租户ID时记录警告
  - 兼容旧系统，平滑迁移

## 二开/扩展关键文件

### 1. 自定义租户ID提取策略
**文件**：`DefaultTenantLineHandler.java`
**扩展点**：重写 `getTenantId()` 方法
```java
public class CustomTenantLineHandler extends DefaultTenantLineHandler {
    @Override
    public Expression getTenantId() {
        // 自定义租户ID提取逻辑
        Long tenantId = getTenantIdFromCustomSource();
        return new LongValue(tenantId);
    }
}
```

### 2. 自定义忽略表策略
**文件**：`DefaultTenantLineHandler.java`
**扩展点**：重写 `ignoreTable()` 方法
```java
@Override
public boolean ignoreTable(String tableName) {
    // 添加自定义忽略逻辑
    if (isSystemTable(tableName)) {
        return true;
    }
    return super.ignoreTable(tableName);
}
```

### 3. 自定义租户拦截器
**文件**：`TenantInterceptor.java`
**扩展点**：实现 `HandlerInterceptor` 接口
```java
public class CustomTenantInterceptor extends TenantInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 自定义租户ID提取逻辑
        Long tenantId = extractFromJWT(request);
        TenantContextHolder.setTenantId(tenantId);
        return true;
    }
}
```

### 4. 集成其他数据隔离策略
**文件**：`MybatisPlusConfig.java`
**扩展点**：拦截器顺序配置
```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    
    // 1. 租户拦截器（最先执行）
    interceptor.addInnerInterceptor(tenantLineInnerInterceptor());
    
    // 2. 数据权限拦截器
    interceptor.addInnerInterceptor(dataScopeInterceptor());
    
    // 3. 分页拦截器
    interceptor.addInnerInterceptor(paginationInnerInterceptor());
    
    return interceptor;
}
```

### 5. 支持多租户策略切换
**扩展方案**：实现策略模式
```java
public interface TenantStrategy {
    boolean shouldFilter(String tableName);
    String getTenantCondition();
}

public class ColumnTenantStrategy implements TenantStrategy {
    // 字段隔离策略
}

public class SchemaTenantStrategy implements TenantStrategy {
    // Schema隔离策略
}

public class DatabaseTenantStrategy implements TenantStrategy {
    // 数据库隔离策略
}
```

## 性能优化建议

### 1. 索引优化
```sql
-- 基本索引
ALTER TABLE table_name ADD INDEX idx_tenant_id (tenant_id);

-- 联合索引（将tenant_id放在前面）
ALTER TABLE table_name ADD INDEX idx_tenant_user (tenant_id, user_id);
ALTER TABLE table_name ADD INDEX idx_tenant_status (tenant_id, status);

-- 覆盖索引
ALTER TABLE table_name ADD INDEX idx_tenant_cover (tenant_id, col1, col2);
```

### 2. 缓存策略
- 租户信息缓存：减少数据库查询
- 表结构缓存：避免重复扫描
- 忽略表缓存：提高判断速度

### 3. 批量操作优化
```java
// 批量插入时，手动设置租户ID比自动填充更高效
List<User> users = new ArrayList<>();
for (User user : userList) {
    user.setTenantId(tenantId);
    users.add(user);
}
userMapper.insertBatch(users);
```

### 4. 查询优化
```java
// 避免在循环中查询
List<Long> userIds = getUserIdList();
// 错误：在循环中多次查询
for (Long userId : userIds) {
    userMapper.selectById(userId);  // 每次都会添加租户条件
}

// 正确：批量查询
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.in("id", userIds);
List<User> users = userMapper.selectList(wrapper);  // 一次查询，一个租户条件
```

## 安全注意事项

### 1. 租户ID验证
- 验证租户ID是否存在
- 验证用户是否属于该租户
- 防止租户ID伪造

### 2. 数据泄露防护
- 严格模式确保必须有租户ID
- 审计日志记录租户操作
- 定期检查数据隔离情况

### 3. 权限控制
- 租户管理员权限控制
- 跨租户操作权限控制
- 敏感操作二次验证

## 总结

`forge-starter-tenant` 模块通过以下设计实现了优雅的多租户数据隔离：

1. **无侵入性**：业务代码无需关心租户逻辑
2. **自动注入**：SQL 自动添加租户条件
3. **灵活配置**：支持多种忽略策略
4. **性能优化**：线程本地存储 + 自动检测
5. **安全可靠**：严格模式 + 上下文清理

核心价值：**将租户隔离从业务逻辑中解耦，通过拦截器和上下文自动处理，实现透明化的多租户支持**。