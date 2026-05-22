# Spring Boot 3 后台框架的自动配置设计：少写配置，多做组合

> Starter 如何做到引入即生效？

## 1. 这个问题在企业后台里为什么常见

你有没有遇到这种体验？

**你接手了一个"脚手架"项目，光是配环境就花了半天。**

项目 README 里写着："请将以下配置类复制到你的项目中"——然后贴了 3 个 `@Configuration` 类，加起来 200 行。你照着复制了，启动报错。

排查半天，发现是线程池配置类依赖了一个 Redis 连接工厂，但你还没配 Redis。你把 Redis 配好，又发现文件存储配置类依赖了 OSS 的 SDK，而你用的是 MinIO。你把 OSS 配置删掉，结果全局异常处理器又报错了——因为它引用了文件模块的一个工具类。

**这就是没有自动配置的后果：模块之间是「硬连线」的。**

再看另一个极端：你引入了一个「工作流 Starter」，pom.xml 加一行依赖，启动项目——居然什么反应都没有。翻了半天文档，发现还需要在 `application.yml` 里写 8 个配置项，还得手动加一个 `@EnableWorkflow` 注解到启动类上，再注入一个 `WorkflowClient` Bean。

**这叫「引入」，不叫「引入即生效」。**

真正的「引入即生效」应该是：加一行 Maven 依赖，启动，功能就在那里了。默认配置直接能跑，需要调整时再配 yml，想替换实现时自己写个 Bean 覆盖。

Forge Admin 的 21 个 Starter 和 6 个 Plugin 全部遵循这个原则。这篇文章就来拆解它是怎么做到的。

---

## 2. Forge Admin 是怎么解决的

先看一个最直观的效果对比：

**没有自动配置之前**，你需要：
```
1. 在启动类加 @EnableTenant + @EnableDatascope + @EnableCache + ...
2. 手动写 3 个 @Configuration 类注册 Bean
3. 手动写 yml 配置（不配就报错）
4. 如果不需要某个功能，得逐个删掉注解和配置类
```

**有了自动配置之后**，你只需：
```
1. pom.xml 加 <dependency>forge-starter-tenant</dependency>
2. 启动。完毕。
3. 想关掉？application.yml 一行：forge.tenant.enabled: false
4. 想定制？写个自己的 @Bean，框架自动让步
```

整个链路如下：

```
引入 Maven 依赖
  → Spring Boot 扫描 jar 内的 AutoConfiguration.imports 文件
    → 加载声明的配置类（@AutoConfiguration）
      → 评估条件注解（@ConditionalOnProperty、@ConditionalOnMissingBean…）
        → 条件满足 → @Bean 方法执行，组件注册到容器
        → 条件不满足 → 跳过
```

Forge Admin 目前有 18 个模块通过这种方式自动装配：

| Starter | 核心能力 | 装配方式 |
|---------|---------|---------|
| forge-starter-core | 异常处理、JSON 序列化 | `@AutoConfiguration` |
| forge-starter-web | Undertow + Actuator 聚合 | 纯 POM（无 Java 代码） |
| forge-starter-auth | Sa-Token 登录 + 接口权限 | `@Configuration` + 拦截器 |
| forge-starter-orm | MyBatis-Plus + 分页 + 拦截器组合 | `@Configuration` + `@MapperScan` |
| forge-starter-cache | Redis + Redisson 分布式锁 | `@AutoConfiguration` |
| forge-starter-tenant | 多租户数据隔离 | `@AutoConfiguration` + `@ConditionalOnProperty` |
| forge-starter-datascope | 数据权限 SQL 改写 | `@AutoConfiguration` + `@ConditionalOnProperty` |
| forge-starter-crypto | API 加解密 + 防重放 | `@AutoConfiguration` + `@ConditionalOnBean` |
| forge-starter-idempotent | 幂等控制 | `@AutoConfiguration` + 策略模式 |
| forge-starter-id | 雪花算法分布式 ID | `@AutoConfiguration` |
| forge-starter-excel | EasyExcel 导入导出 | `@AutoConfiguration` |
| forge-starter-file | OSS/MinIO/本地文件 | `@AutoConfiguration` |
| forge-starter-message | 站内信/邮件/短信 | `@AutoConfiguration` |
| forge-starter-social | 社交登录 | `@AutoConfiguration` |
| forge-starter-trans | 分布式事务 | `@AutoConfiguration` |
| forge-starter-websocket | WebSocket | `@Configuration` |
| forge-starter-api-config | API 行为动态配置 | `@AutoConfiguration` |
| forge-starter-config | 配置动态刷新 | `@AutoConfiguration` + `spring.factories` |

---

## 3. 核心机制：`AutoConfiguration.imports`

Spring Boot 3.x 用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件替代了老版本的 `spring.factories`。

**文件格式**非常简单，每行一个全限定类名：

```
# forge-starter-tenant 的注册文件
com.mdframe.forge.starter.tenant.config.TenantAutoConfiguration
```

Spring Boot 启动时会扫描 classpath 下所有 jar 包中的这个文件，汇总出一个自动配置类清单，然后：
1. 按 `@AutoConfigureOrder` 的值排序（值越小越先加载）
2. 逐个评估条件注解（`@ConditionalOnProperty` 等）
3. 条件满足的类执行 `@Bean` 方法，注册组件

来看一个完整的自动配置类长什么样。

### 3.1 标准模板：TenantAutoConfiguration

`forge-starter-tenant/src/main/java/…/TenantAutoConfiguration.java`：

```java
@Slf4j
@AutoConfiguration                                         // 1️⃣ 标记为自动配置类
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)        // 2️⃣ 控制在 ORM 之前加载
@EnableConfigurationProperties(TenantProperties.class)       // 3️⃣ 绑定 yml 配置
@ConditionalOnProperty(prefix = "forge.tenant", name = "enabled",
    havingValue = "true", matchIfMissing = true)             // 4️⃣ 默认启用，可关闭
@RequiredArgsConstructor
public class TenantAutoConfiguration implements WebMvcConfigurer {

    @Bean
    @ConditionalOnMissingBean                                // 5️⃣ 你写自己的 Bean 就能覆盖
    public DefaultTenantLineHandler tenantLineHandler(
            @Autowired(required = false) TenantTableChecker tenantTableChecker) {
        return new DefaultTenantLineHandler(tenantTableChecker);
    }

    @Bean
    @ConditionalOnMissingBean(name = "tenantLineInnerInterceptor")
    public TenantLineInnerInterceptor tenantLineInnerInterceptor(
            DefaultTenantLineHandler tenantLineHandler) {
        return new TenantLineInnerInterceptor(tenantLineHandler);
    }

    // 自动检测数据库租户字段（子功能开关）
    @Bean
    @ConditionalOnProperty(prefix = "forge.tenant",
        name = "auto-detect-tenant-column", havingValue = "true", matchIfMissing = true)
    public TenantTableChecker tenantTableChecker(DataSource dataSource) { … }

    // 注册租户切面
    @Bean
    @ConditionalOnMissingBean
    public IgnoreTenantAspect ignoreTenantAspect() { … }

    // 注册请求拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) { … }
}
```

这 5 个注解构成了 Forge Admin 自动配置的标准姿势，下面逐一展开。

### 3.2 `@ConditionalOnMissingBean`：用户优先

这是「少写配置」的核心。框架提供默认实现，但用户声明同名 Bean 就能覆盖。

```java
// 框架默认：
@Bean
@ConditionalOnMissingBean
public DefaultTenantLineHandler tenantLineHandler() {
    return new DefaultTenantLineHandler();
}

// 你的项目里如果要定制：
@Bean
public TenantLineHandler myCustomHandler() {
    return new MyCustomTenantLineHandler();  // 框架的 DefaultTenantLineHandler 就不会创建
}
```

Forge Admin 在所有核心 Bean 上都加了 `@ConditionalOnMissingBean`（项目中共 24+ 处），这意味着**你不需要反向依赖、不需要继承、不需要重写任何东西，声明同名 Bean 即可覆盖**。

### 3.3 `@ConditionalOnProperty`：功能开关

通过 yml 一行配置控制整个模块：

```yaml
# 关闭多租户
forge.tenant.enabled: false

# 关闭数据权限
forge.datascope.enabled: false

# 关闭幂等控制
forge.idempotent.enabled: false
```

关键细节：所有开关的默认值都是 `matchIfMissing = true`。也就是说**不配就等于启用**，这才是「引入即生效」的承诺。

```java
@ConditionalOnProperty(prefix = "forge.tenant", name = "enabled",
    havingValue = "true", matchIfMissing = true)  // ← 默认启用
```

### 3.4 `@ConditionalOnBean`：级联依赖

当功能 B 依赖功能 A 时才生效。最典型的例子是 `forge-starter-crypto` 的防重放过滤器：

```java
// 第一层：Redis 缓存可用，才创建 Token 缓存
@Bean
@ConditionalOnBean(ICacheService.class)
public ReplayTokenCache replayTokenCache(ICacheService cacheService) { … }

// 第二层：Token 缓存存在 + 配置打开，才注册过滤器
@Bean
@ConditionalOnBean(ReplayTokenCache.class)
@ConditionalOnProperty(prefix = "forge.crypto", name = "enableReplayProtection", havingValue = "true")
public FilterRegistrationBean<ReplayAttackFilter> replayAttackFilter(
        ReplayTokenCache replayTokenCache) { … }
```

如果没配 Redis（没有 `ICacheService`），防重放功能**静默不生效**，不会报错，不会影响主流程。这叫「优雅降级」。

### 3.5 `@ConditionalOnClass`：可选依赖

某依赖 jar 在 classpath 才创建对应 Bean：

```java
// forge-starter-idempotent
@Bean
@ConditionalOnMissingBean
@ConditionalOnClass(RedissonClient.class)  // 有 Redisson 才启用分布式锁模式
public LockManager lockManager(RedissonClient redissonClient) {
    return new LockManager(redissonClient);
}
```

如果你的项目只引入了 `redisson-spring-boot-starter`，幂等模块自动获得分布式锁能力；没引入，就用本地锁。零配置。

---

## 4. 核心设计：拦截器组合

`forge-starter-orm` 的 `MybatisPlusConfig` 是整个框架**最精妙的一行代码**：

```java
@EnableTransactionManagement(proxyTargetClass = true)
@MapperScan("${mybatis-plus.mapperPackage}")
public class MybatisPlusConfig {

    @Autowired(required = false)
    private List<InnerInterceptor> innerInterceptors;  // 🔑 就这一行

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 先注册租户、数据权限等外部拦截器
        if (innerInterceptors != null && !innerInterceptors.isEmpty()) {
            innerInterceptors.forEach(interceptor::addInnerInterceptor);
        }
        // 再注册分页
        interceptor.addInnerInterceptor(paginationInnerInterceptor());
        // 最后注册乐观锁
        interceptor.addInnerInterceptor(optimisticLockerInnerInterceptor());
        return interceptor;
    }
}
```

**ORM 模块不需要知道有哪些拦截器**。`TenantLineInnerInterceptor`（租户拦截器）是 `forge-starter-tenant` 注册的 Bean，`DataScopeInterceptor`（数据权限拦截器）是 `forge-starter-datascope` 注册的 Bean。Spring 自动把它们收集到 `List<InnerInterceptor>` 里，注入给 ORM 模块。

这意味着：**新增一个拦截器模块，不需要改 ORM 的一行代码**。这就是「多做组合」的真正含义。

整个拦截器链路：

```
MybatisPlusInterceptor 初始化
  ├─ 注入 List<InnerInterceptor>
  │   ├─ TenantLineInnerInterceptor（来自 forge-starter-tenant，按 order=1 执行）
  │   └─ DataScopeInterceptor（来自 forge-starter-datascope，按 order=2 执行）
  ├─ PaginationInnerInterceptor（ORM 自身的分页拦截器）
  └─ OptimisticLockerInnerInterceptor（ORM 自身的乐观锁拦截器）
```

---

## 5. 关键取舍和坑

### 5.1 为什么不是每个模块都用 `@AutoConfiguration`？

`forge-starter-auth` 用了 `@Configuration` + `@ComponentScan`，而不是 `@AutoConfiguration`。原因是 Sa-Token 的拦截器注册逻辑复杂，需要通过 `@ComponentScan` 扫描 `ApiPermissionInterceptor` 等组件，放在 `AutoConfiguration.imports` 里反而不自然。

`forge-starter-web` 甚至连 Java 代码都没有，纯靠 POM 聚合依赖（Undertow + Actuator + Jackson）。因为 Web 容器不需要任何自定义 Bean，引入 Spring Boot 的 starter 即可。

**选择标准**：
- 需要注册自定义 Bean → `@AutoConfiguration` + `AutoConfiguration.imports`
- 需要扫描包内组件 → `@Configuration` + `@ComponentScan`
- 只需要聚合第三方依赖 → 纯 POM

### 5.2 为什么 `matchIfMissing = true`？

默认启用。这是为了兑现「引入即生效」的承诺。用户加了依赖就应该能用，不需要额外配置。如果需要关掉，用户自然会去查文档配 `enabled: false`。

### 5.3 常见踩坑

| 问题 | 原因 | 解决 |
|------|------|------|
| 自动配置类不生效 | 没写 `AutoConfiguration.imports` 或路径错误 | 检查文件路径和类名 |
| 开关属性不生效 | kebab-case 转 camelCase 规则不熟 | `enable-replay-protection` → `enableReplayProtection` |
| 用户 Bean 没覆盖默认 | 忘了加 `@ConditionalOnMissingBean` | 框架侧加注解 |
| 拦截器顺序不对 | `@AutoConfigureOrder` 值不当 | 租户 order 需小于 ORM |
| `@Autowired(required=false)` 为 null | 依赖的 Starter 没引入 | 确认 pom.xml 依赖完整 |

---

## 6. 如何二开：新增一个 Starter

假设你要新增 `forge-starter-notification`（消息通知），标准 5 步：

**第一步**：创建模块目录，写 pom.xml 引入必要依赖。

**第二步**：定义配置属性类：
```java
@ConfigurationProperties(prefix = "forge.notification")
@Data
public class NotificationProperties {
    private boolean enabled = true;
    private String channel = "inbox";  // inbox, email, sms
}
```

**第三步**：写自动配置类：
```java
@AutoConfiguration
@EnableConfigurationProperties(NotificationProperties.class)
@ConditionalOnProperty(prefix = "forge.notification", name = "enabled",
    havingValue = "true", matchIfMissing = true)
public class NotificationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NotificationService notificationService(NotificationProperties props) {
        return new DefaultNotificationService(props);
    }
}
```

**第四步**：创建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，写入一行：
```
com.mdframe.forge.starter.notification.config.NotificationAutoConfiguration
```

**第五步**：在 `forge-starter-parent/pom.xml` 的 `<modules>` 中添加模块声明。

完成。用户加一行 Maven 依赖，启动即可用。想关闭就 `forge.notification.enabled: false`。

---

## 7. 体验入口和下一篇预告

Forge Admin 的 18 个自动配置模块全部遵循这套设计，你可以打开项目亲自验证：

- 在线演示：后台管理／大屏设计器
- 默认账号：admin / 123456
- Gitee：ForgeLab/forge-admin
- GitHub：yaomindong1996/forge-admin
- 文档站：Forge Admin Docs

**下一篇预告**：A03｜Maven 多模块项目如何避免越写越乱？Forge Admin 的模块边界实践——拆开 `forge-dependencies`、父子 POM 和依赖分层策略。
