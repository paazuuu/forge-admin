# 从零搭一个企业后台，为什么我把能力拆成 Starter 和 Plugin？

> 微内核插件化到底解决什么问题？

## 1. 这个问题在企业后台里为什么常见

你有没有经历过这样的场景？

**场景一：半年后，代码已经分不清边界**

刚开始搭后台时，一切都很清爽。用户、角色、菜单放在一个模块，日志、缓存、文件放在另一个模块。半年过去，需求迭代了几十轮：

- 用户模块加了「登录日志」和「操作日志」的查询功能，日志代码散落在用户 Service 里；
- 角色模块要支持「导出角色用户」，导出逻辑写在角色 Controller 里；
- 文件模块要「鉴权访问」，鉴权判断散落在十几个 Controller 里；
- 多租户上线后，几乎每个列表接口都加了 `tenant_id` 条件……

这时你再想「把日志能力单独抽出去」或者「把文件鉴权统一收口」，改动已经牵一发而动全身。

**场景二：新项目要复用旧项目的「通用能力」**

公司决定做一个新后台，你负责技术选型。旧项目里：

- 登录认证用 Sa-Token，封装了一套 Token 刷新、踢人下线、会话管理的逻辑；
- Excel 导入导出用 EasyExcel，封装了一套列配置、字典翻译、脱敏、分批写入的模板；
- 多数据源配置用 dynamic-datasource，封装了一套读写分离、数据源切换的拦截器。

你满怀信心把这些代码复制到新项目，却发现：

- 老项目的日志配置写在了 `application.yml` 里，新项目的日志格式不一样；
- 老项目的 Excel 模板用了一套自定义注解，新项目的实体类结构不同；
- 老项目的数据源切换用了一个 `@DataSource` 注解，新项目不想用这个注解了。

你花了三天时间「适配」这些代码，还不如从头写一套。

**场景三：想「裁剪」掉不需要的能力**

老板说：「这个项目不需要工作流，也不需要消息通知，把这两块代码去掉。」

你打开项目，发现：

- 工作流代码散落在用户模块、角色模块、审批模块、定时任务模块里；
- 消息通知代码散落在登录模块、操作日志模块、文件模块里；
- 这些模块之间有大量交叉依赖。

你想「去掉工作流」，结果去掉后项目启动报错——因为用户模块依赖了一个 `WorkflowService`，而这个 Service 在工作流模块里。

---

这些问题的根源是什么？**能力边界模糊，模块之间耦合，通用能力没有沉淀。**

企业后台的长期演进，最怕的就是「代码越写越乱」和「能力没法复用」。

## 2. Forge Admin 是怎么解决的

Forge Admin 用 **微内核 + Starter + Plugin** 三层架构来解决这个问题。

<!-- 
  📸 图片位置 1：架构分层示意图
  内容建议：三层架构的层次关系图，从下到上依次为：
  - 底层：forge-dependencies（依赖版本管理）
  - 中层：forge-starter-parent（技术 Starter） + forge-plugin-parent（业务 Plugin）
  - 顶层：forge-admin-server（主应用聚合）
  用不同颜色区分 Starter 和 Plugin，箭头表示依赖关系（Plugin → Starter，主应用 → Plugin）
-->

### 2.1 整体结构

```
forge-framework/                      # 框架层（不依赖具体业务）
├── forge-dependencies/               # 统一依赖版本管理（BOM）
│
├── forge-starter-parent/             # 【技术 Starter】可插拔技术能力
│   ├── forge-starter-core/           # 核心工具类、异常、统一响应
│   ├── forge-starter-web/            # Web 层封装（Undertow + 全局异常处理）
│   ├── forge-starter-auth/           # 认证授权（Sa-Token + 权限注解）
│   ├── forge-starter-orm/            # ORM（MyBatis-Plus + 动态数据源 + 分页）
│   ├── forge-starter-cache/          # 缓存（Redis + Redisson 分布式锁）
│   ├── forge-starter-tenant/         # 多租户（TenantLineInnerInterceptor）
│   ├── forge-starter-datascope/      # 数据权限（DataScopeInterceptor）
│   ├── forge-starter-crypto/         # API 加解密（@ApiEncrypt / @ApiDecrypt）
│   ├── forge-starter-excel/          # Excel 导入导出（EasyExcel）
│   ├── forge-starter-file/           # 文件存储（OSS / RustFS / 本地）
│   ├── forge-starter-log/            # 操作日志（@OperationLog）
│   ├── forge-starter-idempotent/     # 幂等性（注解 + Redisson 分布式锁）
│   ├── forge-starter-id/             # 分布式 ID（雪花算法）
│   ├── forge-starter-config/         # 动态配置刷新（@RefreshScope）
│   ├── forge-starter-trans/          # 分布式事务
│   ├── forge-starter-social/         # 社交登录
│   ├── forge-starter-websocket/      # WebSocket
│   ├── forge-starter-message/        # 消息服务
│   ├── forge-starter-job/            # 任务调度基础设施
│   └── forge-starter-api-config/     # API 行为动态配置
│
├── forge-plugin-parent/              # 【业务插件】可插拔功能模块
│   ├── forge-plugin-system/          # 系统管理（用户/角色/菜单/部门/岗位/租户/字典）
│   ├── forge-plugin-generator/       # 代码生成器（AI 驱动）
│   ├── forge-plugin-job/             # 定时任务（Quartz / SnailJob）
│   ├── forge-plugin-message/         # 消息中心（站内信/邮件/短信）
│   ├── forge-plugin-flow/            # 流程引擎（Flowable）
│   ├── forge-plugin-ai/              # AI 供应商管理
│   ├── forge-plugin-external/        # 外部扩展
│   └── forge-plugin-data/            # 数据模块
│
forge-admin-server/                   # 【主应用】聚合所需插件，对外提供接口
```

### 2.2 核心设计原则

| 层级 | 定位 | 特点 |
|------|------|------|
| **Starter** | 技术能力下沉 | 不依赖业务，引入即生效，可独立二开 |
| **Plugin** | 业务能力沉淀 | 可选组合，按需引入，不依赖主应用 |
| **主应用** | 场景聚合 | 只负责组合 Plugin、配置启动参数 |

用一个比喻来理解：

> Starter 是「插座」，Plugin 是「电器」，主应用是「配电箱」。
> 
> - 插座（Starter）提供稳定的技术能力，谁都可以接入；
> - 电器（Plugin）提供具体的业务功能，可以按需组合；
> - 配电箱（主应用）决定哪些电器接入哪些插座。

### 2.3 解决了什么问题

<!--
  📸 图片位置 2：传统项目 vs Forge Admin 对比图
  内容建议：左右对比图
  左侧（传统项目）：模块边界模糊的示意图，用虚线表示代码散落，红色箭头表示交叉依赖
  右侧（Forge Admin）：清晰的分层结构，Starter 和 Plugin 边界清晰，绿色箭头表示单向依赖
-->

| 问题 | 传统项目 | Forge Admin |
|------|----------|-------------|
| 能力边界模糊 | 用户模块里写日志、文件模块里写鉴权 | Starter 只做技术能力，Plugin 只做业务能力 |
| 能力无法复用 | 复制代码 + 三天适配 | 引入 Starter 即生效，自动配置 |
| 无法裁剪 | 去掉后报错、依赖残留 | Plugin 可选引入，去掉不影响其他模块 |
| 升级困难 | 改一处牵动多处 | Starter 版本升级，Plugin 不受影响 |

## 3. 核心设计：Starter 与 Plugin 的边界

### 3.1 Starter：技术能力下沉

Starter 的职责是「封装技术实现细节，提供声明式使用方式」。

典型 Starter 设计：

| Starter | 封装的技术能力 | 声明式使用方式 |
|---------|---------------|---------------|
| `forge-starter-auth` | Sa-Token 登录态、Token 刷新、踢人下线、权限拦截 | `@SaCheckPermission("system:user:add")` |
| `forge-starter-log` | 操作日志收集、异步写入、字段脱敏 | `@OperationLog(module = "用户管理", action = "新增用户")` |
| `forge-starter-excel` | EasyExcel 列配置、字典翻译、分批写入 | `@ExcelExport(columns = {...}, dictTranslate = true)` |
| `forge-starter-file` | OSS/MinIO/本地存储切换、鉴权 URL 生成 | `FileService.upload(file, "avatar")` |
| `forge-starter-tenant` | 租户拦截器、自动追加 `tenant_id` | 无需注解，SQL 自动改写 |

**关键设计原则**：

1. **不依赖业务**：Starter 不知道「用户」、「角色」、「订单」是什么，只知道「认证」、「日志」、「文件」、「租户」。
2. **引入即生效**：通过 Spring Boot AutoConfiguration，只要 Maven 引入，功能自动开启。
3. **可配置开关**：敏感能力（如加解密、数据权限）可以通过配置关闭，不影响其他模块。

### 3.2 Plugin：业务能力沉淀

Plugin 的职责是「提供完整的业务模块，可独立运行、可组合部署」。

典型 Plugin 设计：

| Plugin | 业务能力 | 包含内容 |
|--------|----------|----------|
| `forge-plugin-system` | 系统管理 | 用户、角色、菜单、部门、岗位、租户、字典的 CRUD + 权限 |
| `forge-plugin-flow` | 流程管理 | 流程模型、流程发起、待办审批、流程时间轴 |
| `forge-plugin-generator` | 代码生成 | 表导入、字段配置、模板管理、代码预览与下载 |
| `forge-plugin-message` | 消息中心 | 站内信、系统通知、消息模板、发送记录 |

**关键设计原则**：

1. **业务闭环**：一个 Plugin 包含该业务领域的 Controller、Service、Mapper、DTO、VO、前端页面。
2. **可选组合**：主应用可以只引入 `forge-plugin-system`，不引入 `forge-plugin-flow`。
3. **依赖 Starter**：Plugin 依赖 Starter 的技术能力，但不依赖其他 Plugin（避免循环依赖）。

### 3.3 边界划分标准

怎么判断一个能力应该放在 Starter 还是 Plugin？

| 判断标准 | Starter | Plugin |
|----------|---------|--------|
| 是否依赖具体业务表？ | 不依赖（如：日志表、文件表是技术表） | 依赖（如：用户表、角色表是业务表） |
| 是否需要配置后生效？ | 自动生效（AutoConfiguration） | 需要数据初始化（菜单、字典、权限） |
| 是否可以被其他项目复用？ | 可以（纯技术能力） | 需要适配（业务领域不同） |
| 升级是否影响业务代码？ | 不影响（接口稳定） | 可能影响（表结构、字段变更） |

## 4. 核心实现链路

### 4.1 从请求到响应的完整链路

<!--
  📸 图片位置 3：请求处理链路图（核心配图）
  内容建议：从 HTTP 请求到数据库响应的完整链路，标注每一层归属：
  - 前端请求 → Controller (Plugin 层，蓝色标注)
  - Controller → Service (Plugin 层，蓝色标注)
  - Service → Mapper (Plugin 层，蓝色标注)
  - Mapper → TenantInterceptor (Starter 层，绿色标注)
  - TenantInterceptor → DataScopeInterceptor (Starter 层，绿色标注)
  - DataScopeInterceptor → 数据库
  - 数据库 → 统一响应封装 (Starter 层，绿色标注)
  - 响应 → 前端
  用不同颜色区分 Plugin 和 Starter，清晰展示两层协作关系
-->

一个典型的「新增用户」请求链路：

```
Controller (forge-plugin-system)
    ↓ 调用
Service (forge-plugin-system)
    ↓ 调用
Mapper (forge-plugin-system)
    ↓ 被 MyBatis-Plus 拦截
TenantLineInnerInterceptor (forge-starter-tenant)
    ↓ 自动追加 WHERE tenant_id = ?
DataScopeInterceptor (forge-starter-datascope)
    ↓ 按角色追加数据权限条件
    ↓ 执行 SQL
数据库
    ↓ 返回结果
统一响应封装 (forge-starter-core)
    ↓ 返回前端
```

**关键点**：

- Controller、Service、Mapper 在 Plugin 里，负责业务编排；
- TenantInterceptor、DataScopeInterceptor 在 Starter 里，负责技术拦截；
- 两层之间通过「接口」隔离，Plugin 不依赖 Starter 的实现细节。

### 4.2 AutoConfiguration 如何让 Starter「引入即生效」

以 `forge-starter-log` 为例：

**文件结构**：

```
forge-starter-log/
├── src/main/java/
│   └── OperationLogAutoConfiguration.java   # 自动配置类
│   └── OperationLogInterceptor.java         # 日志拦截器
│   └── OperationLogProperties.java          # 配置属性
├── src/main/resources/
│   └── META-INF/spring/
│       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

**AutoConfiguration.imports 内容**：

```properties
com.mdframe.forge.starter.log.OperationLogAutoConfiguration
```

**自动配置类**：

```java
@AutoConfiguration
@EnableConfigurationProperties(OperationLogProperties.class)
public class OperationLogAutoConfiguration {
    
    @Bean
    @ConditionalOnProperty(prefix = "forge.log", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OperationLogInterceptor operationLogInterceptor() {
        return new OperationLogInterceptor();
    }
}
```

**效果**：只要 Maven 引入 `forge-starter-log`，Spring Boot 启动时自动加载拦截器，无需手动配置。

### 4.3 Plugin 如何组合到主应用

主应用 `forge-admin-server` 的 pom.xml：

```xml
<dependencies>
    <!-- 技术能力 -->
    <dependency>
        <groupId>com.mdframe.forge</groupId>
        <artifactId>forge-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mdframe.forge</groupId>
        <artifactId>forge-starter-auth</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mdframe.forge</groupId>
        <artifactId>forge-starter-orm</artifactId>
    </dependency>
    <!-- ...其他 Starter -->
    
    <!-- 业务能力 -->
    <dependency>
        <groupId>com.mdframe.forge</groupId>
        <artifactId>forge-plugin-system</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mdframe.forge</groupId>
        <artifactId>forge-plugin-generator</artifactId>
    </dependency>
    <!-- ...其他 Plugin，可选引入 -->
</dependencies>
```

**组合效果**：

- 引入 `forge-plugin-system`：后台具备用户、角色、菜单等管理能力；
- 不引入 `forge-plugin-flow`：后台不具备流程审批能力，但其他功能不受影响；
- 后续需要流程时，只需在 pom.xml 加一行 `<dependency>`，重新启动即可。

## 5. 关键取舍和坑

### 5.1 为什么不让 Plugin 互相依赖？

<!--
  📸 图片位置 6：Plugin 依赖问题对比图
  内容建议：上下对比图
  上方（错误做法）：Plugin 互相依赖导致循环依赖，用红色箭头表示循环关系
  下方（正确做法）：Plugin 通过事件/接口解耦，用绿色箭头表示单向依赖
  标注「循环依赖风险」和「裁剪困难」的问题点
-->

**问题**：如果 `forge-plugin-flow` 需要调用 `forge-plugin-system` 的用户查询接口，是否可以让 Flow Plugin 依赖 System Plugin？

**答案**：不建议。

**原因**：

1. **循环依赖风险**：如果 Flow Plugin 调用 System Plugin，System Plugin 后续也可能需要调用 Flow Plugin（如：用户离职触发流程），导致 Maven 循环依赖。
2. **裁剪困难**：如果 Flow Plugin 强依赖 System Plugin，去掉 System Plugin 后 Flow Plugin 无法启动。
3. **升级耦合**：System Plugin 升级时，Flow Plugin 需要同步适配。

**正确做法**：

- Plugin 之间通过「事件」或「接口」解耦，不直接依赖；
- 跨 Plugin 协调逻辑上提到 Controller 层或主应用层。

### 5.2 Starter 不是万能的

**问题**：是不是所有技术能力都要做成 Starter？

**答案**：不是。

**不适合做成 Starter 的场景**：

1. **业务强相关**：如「用户登录日志」依赖用户表结构，这是业务能力，应该放在 Plugin；
2. **使用频率低**：如「二维码生成」只在某个页面用一次，没必要做成 Starter；
3. **配置过于复杂**：如「报表引擎」配置项太多，做成 Starter 反而增加理解成本。

**适合做成 Starter 的场景**：

1. **跨模块使用**：如「操作日志」几乎所有模块都需要；
2. **配置标准化**：如「Redis 缓存」配置项固定，引入即生效；
3. **版本独立演进**：如「文件存储」从本地到 OSS 到 MinIO，升级不影响业务。

### 5.3 常见坑

| 坑 | 表现 | 解决方案 |
|----|------|----------|
| **Plugin 互相注入 Service** | 启动报错「循环依赖」 | 跨 Plugin 协调上提到 Controller 或主应用 |
| **Starter 配置未生效** | 功能不生效，以为是 Bug | 检查 `AutoConfiguration.imports` 是否正确引入 |
| **裁剪后启动报错** | 去掉 Plugin 后找不到类 | 检查是否有其他 Plugin 依赖被裁剪的 Plugin |
| **版本不一致** | Starter 和 Plugin 版本冲突 | 使用 `forge-dependencies` 统一管理版本 |

## 6. 如何二开

### 6.1 新增一个 Starter

假设你要新增一个「短信发送 Starter」：

**步骤**：

1. 在 `forge-starter-parent` 下新建模块 `forge-starter-sms`；
2. 编写 `SmsAutoConfiguration`，注册到 `AutoConfiguration.imports`；
3. 封装短信发送接口，不依赖具体业务；
4. 在 `forge-dependencies/pom.xml` 中声明版本；
5. 在需要的主应用 pom.xml 中引入。

**关键点**：

- Starter 不应该知道「验证码短信」、「订单通知短信」是什么业务，只提供「发送短信」的技术能力；
- 业务含义（如「验证码」）由 Plugin 或主应用传入参数决定。

### 6.2 新增一个 Plugin

假设你要新增一个「合同管理 Plugin」：

**步骤**：

1. 在 `forge-plugin-parent` 下新建模块 `forge-plugin-contract`；
2. 包含 Controller、Service、Mapper、DTO、VO、Entity；
3. 依赖需要的 Starter（如 `forge-starter-orm`、`forge-starter-log`）；
4. 不依赖其他 Plugin（如 `forge-plugin-flow`），如果需要流程审批，通过事件或接口解耦；
5. 在 `forge-dependencies/pom.xml` 中声明版本；
6. 在主应用 pom.xml 中引入。

**关键点**：

- Plugin 可以有独立的数据库表、菜单、字典、权限；
- Plugin 之间通过事件或接口解耦，不直接依赖。

### 6.3 裁剪不需要的 Plugin

假设你的项目不需要「工作流」：

**步骤**：

1. 在主应用 pom.xml 中去掉 `forge-plugin-flow` 的 `<dependency>`；
2. 检查是否有其他代码依赖 Flow Plugin 的类（如有，需重构解耦）；
3. 重新启动，验证功能正常。

**关键点**：

- Plugin 可选引入，去掉不影响其他 Plugin；
- 如果有依赖残留，说明解耦不彻底，需要重构。

## 7. 体验 Forge Admin

想亲自感受微内核插件化架构？可以这样体验：

**方式一：在线演示**

- 后台管理：http://81.70.22.48:8084/forge/login
- 默认账号：`admin` / `123456`
- 体验路径：登录 → 系统管理 → 菜单管理 → 角色管理 → 用户管理

**方式二：本地启动**

```bash
# 克隆项目
git clone https://gitee.com/ForgeLab/forge-admin.git

# 启动后端
cd forge/forge-admin-server
mvn spring-boot:run

# 启动前端
cd forge-admin-ui
pnpm install && pnpm dev
```

**方式三：阅读源码**

- Starter 源码：`forge/forge-framework/forge-starter-parent/`
- Plugin 源码：`forge/forge-framework/forge-plugin-parent/`
- 架构文档：`forge-docs/guide/architecture.md`

---

**下一篇预告**：

> Spring Boot 3 后台框架的自动配置设计：少写配置，多做组合
> 
> 我们将拆解 `AutoConfiguration.imports`、`@ConditionalOnProperty`、`@EnableConfigurationProperties`，看看 Forge Admin 的 Starter 是如何做到「引入即生效」的。
