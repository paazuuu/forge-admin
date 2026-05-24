# 从配置到运行时：Forge Admin 的动态 API 配置管理是怎么做的

> 问题：同一个接口，今天要加认证、明天要加加密、后天要限流，这些行为散落在拦截器、过滤器、注解里，改一次牵一发动全身，怎么集中管理和动态刷新？

## 1. 这个问题在企业后台里为什么常见

在企业后台开发中，API 行为控制的需求随着业务发展不断变化。一个看似简单的接口可能涉及多种横切关注点：

- **认证鉴权**：哪些接口需要登录？哪些接口可以匿名访问？
- **报文加解密**：敏感数据传输是否需要加密？
- **租户隔离**：是否自动追加 `tenant_id` 条件？
- **限流保护**：是否开启接口限流？
- **脱敏处理**：哪些字段需要脱敏后返回？

传统做法有三种：

| 做法 | 代码形式 | 问题 |
|------|----------|------|
| 硬编码 | 拦截器里写 `if (path.contains("/public")) return true` | 条件散落，难以维护 |
| 注解标注 | 每个 Controller 方法加 `@ApiEncrypt`、`@SaCheckPermission` | 接口多了注解满天飞，修改要改源码 |
| 配置文件 | YAML 里写 `auth.excludePaths: /login,/register` | 无法运行时动态调整，重启才能生效 |

真实场景更复杂：

- 某接口上线时允许匿名访问，运营后发现数据泄露风险，紧急要求加认证
- 某接口原本不限流，用户量暴涨后触发雪崩，需要立即开启限流
- 新增租户后，部分接口需要关闭租户隔离（如公共查询接口）
- 不同模块的接口可能有不同的默认策略

**核心痛点**：API 行为配置散落在代码各处，无法集中管理、无法运行时动态刷新、无法细粒度控制。

## 2. Forge Admin 是怎么解决的

Forge Admin 提供了 `forge-starter-api-config` 模块，实现"配置驱动 + 两级缓存 + 事件刷新"的动态 API 配置管理。

### 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                     API 配置管理架构                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────┐        ┌───────────────┐                    │
│  │  sys_api_config│        │  拦截器链      │                    │
│  │  (数据库表)    │───────>│  AuthInterceptor│                   │
│  └───────────────┘        │  CryptoInterceptor│                 │
│         │                 │  TenantInterceptor│                 │
│         ▼                 │  LimitInterceptor │                 │
│  ┌───────────────┐        └───────────────┘                    │
│  │ ApiConfigManager│              │                             │
│  │ (核心决策引擎)  │              ▼                             │
│  │  - Ant路径匹配  │        ┌───────────────┐                    │
│  │  - 两级缓存    │        │ ThreadLocal    │                    │
│  │  - 事件刷新    │        │ (请求上下文)   │                    │
│  └───────────────┘        └───────────────┘                    │
│         │                         │                             │
│         ▼                         ▼                             │
│  ┌───────────────┐        ┌───────────────┐                    │
│  │ Caffeine (L1) │        │ 业务Service    │                    │
│  │ Redis (L2)    │        │ (调用决策API)  │                    │
│  └───────────────┘        └───────────────┘                    │
│                                                                 │
│  配置变更 ─────> ApiConfigRefreshEvent ─────> 异步刷新缓存       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 核心能力表

| 能力 | 描述 | 优势 |
|------|------|------|
| 集中配置 | 所有接口行为存储在 `sys_api_config` 表 | 可通过后台页面管理，无需改代码 |
| Ant路径匹配 | 支持 `/api/user/**`、`/api/{id}` 等模式 | 精确匹配和模糊匹配结合 |
| 两级缓存 | Caffeine 本地缓存 + Redis 分布式缓存 | 高性能 + 集群一致性 |
| 事件刷新 | 配置变更发布事件，异步刷新缓存 | 运行时生效，无需重启 |
| ThreadLocal 上下文 | 请求级配置缓存，避免重复查询 | 拦截器和业务层共享配置 |

### 模块结构

```
forge-starter-api-config/
├── config/
│   ├── ApiConfigAutoConfiguration.java     # 自动配置（引入即生效）
│   └── ApiConfigProperties.java            # 属性配置（缓存大小、过期时间）
├── domain/
│   ├── entity/SysApiConfig.java            # 数据库实体
│   ├── dto/ApiConfigInfo.java              # 缓存和传输 DTO
│   ├── event/ApiConfigRefreshEvent.java    # 配置刷新事件
│   └── dto/ApiConfigQuery.java             # 查询参数
├── service/
│   ├── IApiConfigManager.java              # 核心决策引擎接口
│   ├── impl/ApiConfigManagerImpl.java      # 实现类（缓存 + 匹配 + 刷新）
│   ├── ISysApiConfigService.java           # CRUD 服务接口
│   └── impl/SysApiConfigServiceImpl.java   # CRUD 实现
├── context/
│   └── ApiConfigContextHolder.java         # ThreadLocal 上下文持有者
├── listener/
│   └── ApiConfigRefreshListener.java       # 事件监听器
├── controller/
│   ├── SysApiConfigController.java         # CRUD 接口
│   └── ApiConfigManageController.java      # 管理接口（刷新缓存）
├── mapper/
│   └── SysApiConfigMapper.java             # MyBatis Mapper
└── registry/
    ├── ApiConfigScanner.java               # 接口扫描器
    └── ApiConfigAutoRegistrar.java         # 自动注册器
```

## 3. 核心数据结构 / 配置协议

### 3.1 数据库表：sys_api_config

```sql
CREATE TABLE sys_api_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    api_name        VARCHAR(100) COMMENT '接口名称',
    api_code        VARCHAR(100) COMMENT '接口编码（程序引用）',
    req_method      VARCHAR(10) COMMENT '请求方式（GET/POST/PUT/DELETE/ALL）',
    url_path        VARCHAR(500) COMMENT '接口路径（支持Ant风格）',
    api_version     VARCHAR(20) COMMENT '接口版本号',
    module_code     VARCHAR(50) COMMENT '所属模块',
    service_id      VARCHAR(50) COMMENT '微服务ID',
    auth_flag       TINYINT DEFAULT 1 COMMENT '是否需要认证（1-需要, 0-不需要）',
    encrypt_flag    TINYINT DEFAULT 0 COMMENT '是否需要加解密（1-需要, 0-不需要）',
    tenant_flag     TINYINT DEFAULT 1 COMMENT '是否启用租户隔离（1-启用, 0-不启用）',
    limit_flag      TINYINT DEFAULT 0 COMMENT '是否开启限流（1-开启, 0-关闭）',
    sensitive_fields VARCHAR(500) COMMENT '需脱敏字段（JSON数组）',
    status          TINYINT DEFAULT 1 COMMENT '状态（1-正常, 0-停用）',
    remark          VARCHAR(500) COMMENT '备注说明',
    create_by       BIGINT COMMENT '创建人',
    create_time     DATETIME COMMENT '创建时间',
    update_by       BIGINT COMMENT '更新人',
    update_time     DATETIME COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API配置管理表';
```

### 3.2 核心字段说明

| 字段 | 类型 | 示例 | 用途 |
|------|------|------|------|
| `api_name` | String | "查询用户信息" | 人工可读的接口名称，便于管理 |
| `api_code` | String | "user_query" | 程序引用编码，可用于业务逻辑判断 |
| `req_method` | String | "GET" / "ALL" | 请求方式，`ALL` 表示所有方法 |
| `url_path` | String | `/api/user/**` | Ant 风格路径，支持 `*`、`**`、`{变量}` |
| `auth_flag` | Integer | 1 | 0=不需要认证，1=需要认证 |
| `encrypt_flag` | Integer | 0 | 0=不需要加解密，1=需要 |
| `tenant_flag` | Integer | 1 | 0=不启用租户隔离，1=启用 |
| `limit_flag` | Integer | 0 | 0=不限流，1=开启限流 |
| `sensitive_fields` | String | `["phone","id_card"]` | JSON 数组，指定脱敏字段 |

### 3.3 配置示例

**精确匹配配置**：
```json
{
  "apiName": "登录接口",
  "apiCode": "auth_login",
  "reqMethod": "POST",
  "urlPath": "/auth/login",
  "authFlag": 0,
  "encryptFlag": 1,
  "tenantFlag": 0,
  "limitFlag": 1,
  "moduleCode": "auth"
}
```

**模糊匹配配置**：
```json
{
  "apiName": "公共查询接口",
  "apiCode": "public_query",
  "reqMethod": "GET",
  "urlPath": "/api/public/**",
  "authFlag": 0,
  "encryptFlag": 0,
  "tenantFlag": 0,
  "moduleCode": "common"
}
```

### 3.4 DTO：ApiConfigInfo

```java
@Data
public class ApiConfigInfo implements Serializable {
    private Long id;
    private String apiName;
    private String apiCode;
    private String reqMethod;
    private String urlPath;
    private String apiVersion;
    private String moduleCode;
    private String serviceId;
    
    // 行为标志位（Boolean 类型，便于判断）
    private Boolean needAuth;
    private Boolean needEncrypt;
    private Boolean needTenant;
    private Boolean needLimit;
    
    private List<String> sensitiveFields;
    private Boolean enabled;
    private String remark;
    private Long cacheTime;  // 缓存时间戳
    
    // 构建缓存 Key
    public String buildCacheKey() {
        return urlPath + ":" + reqMethod;
    }
    
    // 从实体转换
    public static ApiConfigInfo fromEntity(SysApiConfig entity) {
        ApiConfigInfo info = new ApiConfigInfo();
        info.setNeedAuth(entity.getAuthFlag() == 1);
        info.setNeedEncrypt(entity.getEncryptFlag() == 1);
        // ... 其他字段转换
        return info;
    }
}
```

### 3.5 配置属性：ApiConfigProperties

```yaml
forge:
  api-config:
    enabled: true                    # 是否启用 API 配置管理
    auto-register: true              # 是否自动注册接口
    cache-warm-up: true              # 是否预热缓存
    scan-packages:                   # 扫描的包路径
      - com.mdframe.forge
    cache:
      local:
        max-size: 1000               # 本地缓存最大容量
        expire-minutes: 10           # 本地缓存过期时间（分钟）
      redis:
        enabled: true                # 是否启用 Redis 缓存
        expire-seconds: 1800         # Redis 缓存过期时间（秒）
        key-prefix: "api:config:"    # Redis Key 前缀
```

## 4. 核心实现链路

### 4.1 启动阶段：自动配置和缓存预热

**入口**：`ApiConfigAutoConfiguration.java`

```java
@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "forge.api-config", name = "enabled", havingValue = "true")
@EnableAsync
public class ApiConfigAutoConfiguration {
    
    @Bean
    public ApiConfigScanner apiConfigScanner(RequestMappingHandlerMapping mapping) {
        return new ApiConfigScanner(mapping);  // 扫描已注册的接口
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "forge.api-config", name = "auto-register")
    public ApiConfigAutoRegistrar apiConfigAutoRegistrar(
            ApiConfigScanner scanner,
            SysApiConfigMapper mapper,
            ApiConfigProperties props,
            IApiConfigManager manager) {
        return new ApiConfigAutoRegistrar(scanner, mapper, props, manager);
    }
    
    @Bean
    public ApiConfigRefreshListener apiConfigRefreshListener(IApiConfigManager manager) {
        return new ApiConfigRefreshListener(manager);  // 配置刷新监听器
    }
}
```

**缓存预热**：`ApiConfigManagerImpl.warmUpCache()`

```java
@Override
public void warmUpCache() {
    log.info("开始预热API配置缓存...");
    long startTime = System.currentTimeMillis();
    
    List<SysApiConfig> configs = apiConfigMapper.selectAllEnabled();
    for (SysApiConfig entity : configs) {
        ApiConfigInfo config = ApiConfigInfo.fromEntity(entity);
        String cacheKey = buildCacheKey(entity.getUrlPath(), entity.getReqMethod());
        
        // 写入 L2 缓存（Redis）
        putToRedis(cacheKey, config);
        
        // 写入 L1 缓存（Caffeine）
        localCache.put(cacheKey, config);
    }
    
    // 初始化全量配置列表缓存（用于 Ant 匹配）
    allEnabledConfigsCache = configs.stream()
            .map(ApiConfigInfo::fromEntity)
            .collect(Collectors.toList());
    
    log.info("API配置缓存预热完成，共{}条配置，耗时{}ms", configs.size(), elapsed);
}
```

### 4.2 请求阶段：配置获取和上下文设置

**拦截器链调用**：假设有 `ApiConfigInterceptor`

```java
public class ApiConfigInterceptor implements HandlerInterceptor {
    
    @Autowired
    private IApiConfigManager apiConfigManager;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String urlPath = request.getRequestURI();
        String method = request.getMethod();
        
        // 获取 API 配置
        ApiConfigInfo config = apiConfigManager.getApiConfig(urlPath, method);
        
        // 设置到 ThreadLocal 上下文
        ApiConfigContextHolder.setConfig(config);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清除上下文
        ApiConfigContextHolder.clear();
    }
}
```

**核心决策逻辑**：`ApiConfigManagerImpl.getApiConfig()`

```java
@Override
public ApiConfigInfo getApiConfig(String urlPath, String method) {
    if (!configProperties.isEnabled()) {
        return null;
    }
    
    // 使用 Ant 路径匹配查找最匹配的配置
    ApiConfigInfo config = findBestMatchConfig(urlPath, method);
    
    if (config != null) {
        log.debug("API配置匹配成功: {} {} -> {}", method, urlPath, config.getUrlPath());
    }
    
    return config;
}

private ApiConfigInfo findBestMatchConfig(String urlPath, String method) {
    ApiConfigInfo bestMatch = null;
    int bestScore = -1;
    
    for (ApiConfigInfo config : allEnabledConfigsCache) {
        // 检查请求方法是否匹配
        if (!method.equalsIgnoreCase(config.getReqMethod())) {
            continue;
        }
        
        // 使用 Ant 路径匹配
        if (matcher.match(config.getUrlPath(), urlPath)) {
            // 计算匹配分数：路径越精确，分数越高
            int score = calculateMatchScore(urlPath, config.getUrlPath());
            if (score > bestScore) {
                bestScore = score;
                bestMatch = config;
            }
        }
    }
    
    return bestMatch;
}

private int calculateMatchScore(String requestPath, String configPath) {
    // 精确匹配：完全相等 -> 100分
    if (requestPath.equals(configPath)) {
        return 100;
    }
    
    // 通配符匹配：根据匹配段数计算分数
    if (configPath.contains("*") || configPath.contains("**")) {
        int matchCount = calculateMatchSegments(requestPath, configPath);
        return matchCount * 20;
    }
    
    // 前缀匹配 -> 50分
    if (requestPath.startsWith(configPath)) {
        return 50;
    }
    
    return 0;
}
```

**上下文使用**：`ApiConfigContextHolder`

```java
public class ApiConfigContextHolder {
    
    private static final ThreadLocal<ApiConfigInfo> CONTEXT_HOLDER = new ThreadLocal<>();
    
    public static void setConfig(ApiConfigInfo config) {
        CONTEXT_HOLDER.set(config);
    }
    
    public static ApiConfigInfo getConfig() {
        return CONTEXT_HOLDER.get();
    }
    
    // 快捷判断方法
    public static boolean needAuth() {
        ApiConfigInfo config = CONTEXT_HOLDER.get();
        return config != null && config.getNeedAuth();
    }
    
    public static boolean needEncrypt() {
        ApiConfigInfo config = CONTEXT_HOLDER.get();
        return config != null && config.getNeedEncrypt();
    }
    
    public static boolean needTenant() {
        ApiConfigInfo config = CONTEXT_HOLDER.get();
        return config != null && config.getNeedTenant();
    }
    
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
```

### 4.3 配置变更：事件发布和缓存刷新

**Controller 发布事件**：`SysApiConfigController`

```java
@PostMapping("/edit")
public RespInfo<Void> edit(@RequestBody SysApiConfig config) {
    boolean result = apiConfigService.updateConfig(config);
    
    if (result) {
        // 发布刷新事件（异步处理）
        eventPublisher.publishEvent(new ApiConfigRefreshEvent(this,
                ApiConfigRefreshEvent.RefreshType.SINGLE,
                config.getId(),
                "修改API配置"));
    }
    
    return result ? RespInfo.success() : RespInfo.error("修改失败");
}
```

**事件监听器**：`ApiConfigRefreshListener`

```java
@Component
public class ApiConfigRefreshListener {
    
    private final IApiConfigManager apiConfigManager;
    
    @Async
    @EventListener
    public void onApiConfigRefresh(ApiConfigRefreshEvent event) {
        log.info("收到API配置刷新事件: type={}, reason={}", 
                event.getRefreshType(), event.getReason());
        
        switch (event.getRefreshType()) {
            case SINGLE:
                // 刷新单个接口配置
                if (event.getConfigId() != null) {
                    apiConfigManager.refreshApiConfigById(event.getConfigId());
                }
                break;
            
            case ALL:
                // 刷新所有接口配置
                apiConfigManager.refreshAllApiConfig();
                break;
            
            case MODULE:
                // 刷新指定模块的配置
                apiConfigManager.refreshApiConfigByModule(event.getModuleCode());
                break;
        }
    }
}
```

**缓存刷新实现**：`ApiConfigManagerImpl`

```java
@Override
public void refreshApiConfigById(Long configId) {
    SysApiConfig config = apiConfigMapper.selectById(configId);
    
    if (config != null) {
        String cacheKey = buildCacheKey(config.getUrlPath(), config.getReqMethod());
        
        // 清除 L1 缓存
        localCache.invalidate(cacheKey);
        
        // 清除 L2 缓存（Redis）
        deleteFromRedis(cacheKey);
        
        // 重新加载配置到全量缓存
        reloadAllEnabledConfigsCache();
        
        log.info("刷新API配置缓存: id={}, key={}", configId, cacheKey);
    }
}

@Override
public void refreshAllApiConfig() {
    // 清除 L1 缓存
    localCache.invalidateAll();
    
    // 清除 L2 缓存（Redis）
    clearAllFromRedis();
    
    // 重新预热缓存
    warmUpCache();
    
    log.info("刷新所有API配置缓存");
}
```

### 4.4 完整链路图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         API 配置请求链路                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  HTTP Request                                                       │
│  (GET /api/user/123)                                                │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────┐                                               │
│  │ ApiConfigInterceptor│                                            │
│  │  - 获取 urlPath/method                                           │
│  │  - 调用 apiConfigManager.getApiConfig()                          │
│  │  - 设置到 ThreadLocal                                            │
│  └──────────────────┘                                               │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────┐                                               │
│  │ IApiConfigManager │                                               │
│  │  - 查询 L1 缓存（Caffeine）                                       │
│  │  - Ant 路径匹配查找最匹配配置                                     │
│  │  - 返回 ApiConfigInfo                                            │
│  └──────────────────┘                                               │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────┐                                               │
│  │ 后续拦截器链      │                                               │
│  │  - AuthInterceptor: 根据 needAuth 决定是否拦截                    │
│  │  - CryptoInterceptor: 根据 needEncrypt 决定是否加解密             │
│  │  - TenantInterceptor: 根据 needTenant 决定是否追加租户条件        │
│  └──────────────────┘                                               │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────┐                                               │
│  │ Controller       │                                               │
│  │  - 业务逻辑处理                                                  │
│  │  - 可通过 ApiConfigContextHolder 获取配置                        │
│  └──────────────────┘                                               │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────┐                                               │
│  │ afterCompletion  │                                               │
│  │  - ApiConfigContextHolder.clear()                               │
│  └──────────────────┘                                               │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                         配置变更链路                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  用户修改配置（后台页面）                                             │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────┐                                               │
│  │ SysApiConfigController.edit()                                    │
│  │  - 更新数据库                                                    │
│  │  - 发布 ApiConfigRefreshEvent                                    │
│  └──────────────────┘                                               │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────┐                                               │
│  │ ApiConfigRefreshListener                                         │
│  │  @Async @EventListener                                           │
│  │  - 异步处理事件                                                  │
│  └──────────────────┘                                               │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────┐                                               │
│  │ IApiConfigManager.refreshApiConfigById()                         │
│  │  - 清除 L1 缓存                                                  │
│  │  - 清除 L2 缓存                                                  │
│  │  - 重新加载配置                                                  │
│  └──────────────────┘                                               │
│       │                                                             │
│       ▼                                                             │
│  下一次请求生效（无需重启）                                           │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 5. 关键取舍和坑

### 5.1 为什么用两级缓存而不是只用 Redis？

**问题**：如果只用 Redis，每次请求都要网络调用，性能损耗明显。

**取舍**：
- **L1（Caffeine）**：本地内存缓存，毫秒级响应，适合高频访问的配置
- **L2（Redis）**：分布式缓存，保证集群一致性，适合配置同步

**架构优势**：
```
请求 -> L1 缓存（命中则返回） -> L2 缓存（命中则写入 L1） -> 数据库
```

**坑点**：
- L1 和 L2 的过期时间要合理设置（L1 过期时间 < L2）
- 集群环境下，配置变更必须清除所有节点的 L1 缓存（通过 Redis Pub/Sub 或事件广播）

### 5.2 为什么用 Ant 路径匹配而不是精确匹配？

**问题**：接口数量多时，每个接口都配置一次太繁琐；有些接口有路径参数（如 `/api/user/{id}`），精确匹配无法覆盖。

**Ant 匹配规则**：
| 模式 | 匹配示例 |
|------|----------|
| `/api/user/*` | `/api/user/123`、`/api/user/profile` |
| `/api/user/**` | `/api/user/123/profile`、`/api/user/123/roles` |
| `/api/user/{id}` | `/api/user/123`（Spring MVC 路径参数） |
| `/api/public/**` | 所有 `/api/public/` 下的路径 |

**匹配分数机制**：
- 精确匹配：100 分（优先级最高）
- 通配符匹配：根据匹配段数计算分数（20 分/段）
- 前缀匹配：50 分

**坑点**：
- 配置顺序不重要，但路径精确度决定优先级
- `/api/user/**` 和 `/api/user/*` 同时配置时，后者更精确，优先匹配

### 5.3 为什么用事件刷新而不是直接刷新缓存？

**问题**：配置变更可能涉及多条缓存、多个节点，直接刷新会导致阻塞请求处理。

**事件驱动优势**：
- `@Async` 异步处理，不影响请求性能
- 事件广播可扩展到集群（通过 Redis Pub/Sub）
- 配置变更审计日志可在事件监听器中统一记录

**坑点**：
- 异步刷新有短暂延迟（通常 < 100ms），极端情况下可能有几次请求使用旧配置
- 如果 Redis 不可用，事件广播可能失败，需增加降级逻辑

### 5.4 ThreadLocal 上下文的坑

**问题**：ThreadLocal 在异步线程池、线程复用场景下可能污染。

**解决方案**：
- 拦截器 `afterCompletion` 必须调用 `clear()`
- 异步任务（如 `@Async` 方法）不应依赖 ThreadLocal，应从 `IApiConfigManager` 直接获取

**坑点**：
- 不要在 Service 层长时间持有 ThreadLocal 配置（可能在后续拦截器中已清除）
- 测试环境需注意 ThreadLocal 泄漏（单元测试后未清除）

### 5.5 配置优先级的坑

**优先级规则**：数据库配置 > 注解配置 > 系统默认值

**坑点**：
- 如果数据库配置和注解配置冲突，数据库配置优先，可能覆盖注解效果
- 新增接口时，如果未配置数据库记录，会使用注解或默认值
- 配置状态 `status=0`（停用）的记录不参与匹配，但数据库中仍存在

### 5.6 缓存预热 vs 懒加载

**预热优势**：
- 启动时一次性加载所有配置，避免首次请求延迟
- 缓存统计信息准确（命中率、加载次数）

**懒加载优势**：
- 启动速度更快
- 只缓存实际访问的配置

**取舍**：配置数量 < 1000 时，预热更合适；配置数量巨大时，懒加载更合适。

### 5.7 常见错误配置示例

**错误 1**：`urlPath` 未考虑 Spring MVC 路径参数

```json
// ❌ 错误：无法匹配 /api/user/123
"urlPath": "/api/user/:id"

// ✅ 正确：Spring MVC 路径参数用 Ant 风格
"urlPath": "/api/user/*"
```

**错误 2**：`reqMethod` 大小写不一致

```json
// ❌ 错误：大小写不一致可能导致匹配失败
"reqMethod": "get"

// ✅ 正确：统一使用大写
"reqMethod": "GET"
```

**错误 3**：`sensitive_fields` 格式错误

```json
// ❌ 错误：非 JSON 数组格式
"sensitiveFields": "phone,id_card"

// ✅ 正确：JSON 数组格式
"sensitiveFields": "[\"phone\",\"id_card\"]"
```

## 6. 如何二开

### 6.1 新增一个 API 配置

**步骤**：

1. **通过后台页面添加**（推荐）
   - 访问"系统管理 > API 配置管理"
   - 新增配置，填写接口名称、路径、行为标志位
   - 保存后自动触发缓存刷新

**【截图占位：API 配置管理页面 - 新增配置表单】**

2. **通过数据库脚本添加**（适合批量导入）
   ```sql
   INSERT INTO sys_api_config (
       api_name, api_code, req_method, url_path, 
       auth_flag, encrypt_flag, tenant_flag, limit_flag, 
       module_code, status, tenant_id
   ) VALUES (
       '查询用户列表', 'user_list', 'GET', '/api/user/page',
       1, 0, 1, 0, 'system', 1, 1
   );
   ```
   
   - 注意：`tenant_id` 必须为 `1`（默认租户）
   - 插入后需手动刷新缓存（调用 `/system/apiConfig/refreshAll`）

3. **通过 API 接口添加**
   ```bash
   curl -X POST http://localhost:8580/system/apiConfig/add \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "apiName": "查询用户列表",
       "apiCode": "user_list",
       "reqMethod": "GET",
       "urlPath": "/api/user/page",
       "authFlag": 1,
       "encryptFlag": 0,
       "tenantFlag": 1,
       "moduleCode": "system"
     }'
   ```

### 6.2 扩展拦截器使用 API 配置

**示例**：在 `CryptoInterceptor` 中使用配置决定是否加解密

```java
@Component
public class CryptoInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从 ThreadLocal 获取配置
        if (ApiConfigContextHolder.needEncrypt()) {
            // 标记请求需要解密
            request.setAttribute("needEncrypt", true);
        }
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // 从 ThreadLocal 获取配置
        if (ApiConfigContextHolder.needEncrypt()) {
            // 标记响应需要加密
            response.setHeader("X-Need-Encrypt", "true");
        }
    }
}
```

### 6.3 新增一个行为标志位

**步骤**：

1. **修改数据库表**
   ```sql
   ALTER TABLE sys_api_config ADD COLUMN audit_flag TINYINT DEFAULT 0 COMMENT '是否记录审计日志（1-开启, 0-关闭）';
   ```

2. **修改实体类**
   ```java
   @Data
   public class SysApiConfig {
       // 新增字段
       @TransField(dictType = "yes_no")
       private Integer auditFlag;
       
       @TableField(exist = false)
       private String auditFlagName;
   }
   ```

3. **修改 DTO**
   ```java
   @Data
   public class ApiConfigInfo {
       private Boolean needAudit;
       
       public static ApiConfigInfo fromEntity(SysApiConfig entity) {
           info.setNeedAudit(entity.getAuditFlag() != null && entity.getAuditFlag() == 1);
           return info;
       }
   }
   ```

4. **修改 ThreadLocal 上下文**
   ```java
   public static boolean needAudit() {
       ApiConfigInfo config = CONTEXT_HOLDER.get();
       return config != null && config.getNeedAudit();
   }
   ```

5. **修改拦截器**
   ```java
   if (ApiConfigContextHolder.needAudit()) {
       auditLogger.log(request, response);
   }
   ```

### 6.4 自定义缓存策略

**修改配置属性**：

```yaml
forge:
  api-config:
    cache:
      local:
        max-size: 5000              # 增大本地缓存容量
        expire-minutes: 30          # 增长过期时间
      redis:
        enabled: false              # 禁用 Redis 缓存（单机场景）
```

**自定义缓存实现**：

替换 `ApiConfigManagerImpl` 的缓存逻辑，例如使用 Guava Cache：

```java
private final Cache<String, ApiConfigInfo> guavaCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .recordStats()
        .build();
```

### 6.5 接入前端页面

**前端页面示例**（使用 AiCrudPage）

**【截图占位：前端 API 配置管理页面 - 使用 AiCrudPage 组件】**

```vue
<template>
  <AiCrudPage
    :api-config="apiConfig"
    :columns="columns"
    :search-schema="searchSchema"
    :edit-schema="editSchema"
  />
</template>

<script setup>
const apiConfig = {
  pageUrl: 'GET@/system/apiConfig/page',
  addUrl: 'POST@/system/apiConfig/add',
  editUrl: 'POST@/system/apiConfig/edit',
  deleteUrl: 'POST@/system/apiConfig/remove',
  getByIdUrl: 'GET@/system/apiConfig/getById?id=:id'
}

const columns = computed(() => [
  { title: '接口名称', key: 'apiName' },
  { title: '接口编码', key: 'apiCode' },
  { title: '请求方式', key: 'reqMethod' },
  { title: '接口路径', key: 'urlPath' },
  { title: '所属模块', key: 'moduleCode' },
  { title: '认证', key: 'authFlag', render: h => h(DictTag, { dictType: 'yes_no', value: row.authFlag }) },
  { title: '加解密', key: 'encryptFlag', render: h => h(DictTag, { dictType: 'yes_no', value: row.encryptFlag }) },
  { title: '租户隔离', key: 'tenantFlag', render: h => h(DictTag, { dictType: 'yes_no', value: row.tenantFlag }) },
  { title: '限流', key: 'limitFlag', render: h => h(DictTag, { dictType: 'yes_no', value: row.limitFlag }) },
  { title: '状态', key: 'status', render: h => h(DictTag, { dictType: 'enable_disable', value: row.status }) },
])

const searchSchema = computed(() => [
  { field: 'apiName', label: '接口名称', component: 'NInput' },
  { field: 'moduleCode', label: '所属模块', component: 'NInput' },
  { field: 'status', label: '状态', component: 'DictSelect', dictType: 'enable_disable' },
])

const editSchema = computed(() => [
  { field: 'apiName', label: '接口名称', component: 'NInput', required: true },
  { field: 'apiCode', label: '接口编码', component: 'NInput', required: true },
  { field: 'reqMethod', label: '请求方式', component: 'DictSelect', dictType: 'req_method', required: true },
  { field: 'urlPath', label: '接口路径', component: 'NInput', required: true },
  { field: 'moduleCode', label: '所属模块', component: 'NInput' },
  { field: 'authFlag', label: '是否认证', component: 'NSwitch', defaultValue: 1 },
  { field: 'encryptFlag', label: '是否加解密', component: 'NSwitch', defaultValue: 0 },
  { field: 'tenantFlag', label: '是否租户隔离', component: 'NSwitch', defaultValue: 1 },
  { field: 'limitFlag', label: '是否限流', component: 'NSwitch', defaultValue: 0 },
  { field: 'status', label: '状态', component: 'DictSelect', dictType: 'enable_disable', defaultValue: 1 },
])
</script>
```

### 6.6 新增配置管理接口

**示例**：新增"刷新缓存"接口

```java
@RestController
@RequestMapping("/system/apiConfig")
public class ApiConfigManageController {
    
    @Autowired
    private IApiConfigManager apiConfigManager;
    
    /**
     * 刷新所有缓存
     */
    @PostMapping("/refreshAll")
    @OperationLog(module = "API配置管理", type = OperationType.UPDATE, desc = "刷新所有缓存")
    public RespInfo<Void> refreshAll() {
        apiConfigManager.refreshAllApiConfig();
        return RespInfo.success();
    }
    
    /**
     * 刷新指定配置缓存
     */
    @PostMapping("/refreshById")
    @OperationLog(module = "API配置管理", type = OperationType.UPDATE, desc = "刷新指定缓存")
    public RespInfo<Void> refreshById(@RequestParam Long id) {
        apiConfigManager.refreshApiConfigById(id);
        return RespInfo.success();
    }
    
    /**
     * 获取缓存统计信息
     */
    @GetMapping("/cacheStats")
    public RespInfo<String> getCacheStats() {
        return RespInfo.success(apiConfigManager.getCacheStats());
    }
}
```

## 7. 体验入口和下一篇预告

### 体验 Forge Admin

- 在线演示：http://www.dlforgelab.com:8084/forge/login
- 默认账号：admin / 123456
- Gitee：https://gitee.com/ForgeLab/forge-admin
- GitHub：https://github.com/yaomindong1996/forge-admin

### 验证步骤

1. 登录后台，进入"系统管理 > API 配置管理"
2. 新增一条配置，观察缓存刷新日志
3. 修改配置的 `authFlag` 或 `encryptFlag`，验证拦截器行为变化
4. 调用 `/system/apiConfig/cacheStats` 查看缓存命中率

**【截图占位：后台 API 配置管理页面 - 列表展示】**

### 下一篇预告

下一篇我们将继续拆解：**多租户后台怎么做数据隔离？从 tenant_id 到拦截器的完整链路**，介绍 Forge Admin 如何通过 `TenantLineInnerInterceptor` 实现"配置即生效"的租户隔离能力，避免业务代码散落租户判断。

