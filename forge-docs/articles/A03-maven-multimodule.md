# Maven 多模块项目如何避免越写越乱？Forge Admin 的模块边界实践

> 多模块工程依赖如何分层？

## 1. 这个问题在企业后台里为什么常见

多模块项目的理想状态是：层次清晰，依赖单向，每个模块职责明确。

现实往往是另一个样子。

**典型症状一：版本号地狱**

项目做了半年，你发现 `sa-token` 在用户模块引的是 1.38.0，在消息模块引的是 1.36.0，在流程模块引的是 1.35.0。三个版本同时存在 classpath 里，运行时偶尔抛 `NoSuchMethodError`，但你也不知道是哪个 jar 覆盖了哪个 jar。

更可怕的是，你要升级一个库——比如 MyBatis-Plus 从 3.5.5 升到 3.5.7——你需要在 15 个 POM 文件里逐个改版本号。漏一个，版本不一致；全改了，又不知道会不会有兼容问题。

**典型症状二：循环依赖**

A 模块依赖 B，B 模块依赖 C，某天 C 模块的新功能需要调用 A 模块的接口——Maven 直接报 `The projects in the reactor contain a cyclic reference`。你只能把共同代码抽到第四个模块，一周过去了。

**典型症状三：不知道改了什么**

根 POM 通过 `<dependencyManagement>` 升了一个三方库的版本号。两周后生产环境出现一个诡异 bug，你花了两天排查，最后发现是这个库的 patch 版本改了序列化行为。你甚至不知道这个库被哪些模块间接依赖了。

**典型症状四：新增模块成本高**

产品提了一个新需求，你需要新建一个 Maven 模块。你得决定：

- 父 POM 是谁？
- 版本号写在哪？
- 依赖哪些模块？依赖了会不会引入循环？
- 谁依赖这个新模块？改了别人的 POM 会不会影响打包？

这些问题回答不清楚，新模块要么耦合过深，要么孤悬在外。

---

## 2. Forge Admin 的模块拓扑

Forge Admin 有 **35+ 个 Maven 模块**，但依赖关系非常干净。核心在于 4 层单向分层：

```
                        ┌─────────────────┐
                        │   forge (根POM)   │  ← 统一版本号、全局插件
                        └────────┬────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              ▼                  ▼                  ▼
   ┌─────────────────┐  ┌──────────────┐  ┌─────────────────┐
   │ forge-admin-    │  │ forge-       │  │ forge-report/   │
   │ server (入口)    │  │ framework/   │  │ flow/app/biz    │
   └─────────────────┘  └──────┬───────┘  └─────────────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
   ┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐
   │ forge-dependencies│ │starter-parent│ │ plugin-parent    │
   │ (BOM 版本中心)    │ │ (20个技术模块)│ │ (8个业务插件)     │
   └──────────────────┘ └──────────────┘ └──────────────────┘
```

**核心规则只有一条：依赖方向永远从上到下，绝不允许反向。**

| 层级 | 角色 | 例子 | 可以依赖 |
|------|------|------|---------|
| 根 POM | 全局管家 | `forge/pom.xml` | 无（顶层） |
| BOM | 版本中心 | `forge-dependencies` | 外部 BOM |
| 框架层 | 可复用能力 | `forge-starter-*` / `forge-plugin-*` | BOM + 同级 Starter |
| 应用层 | 入口组装 | `forge-admin-server` | 框架层 + 外部依赖 |

应用层可以依赖框架层，框架层可以依赖 BOM。反过来不行。

---

## 3. 核心机制：BOM 统一版本管理

### 3.1 版本号零散落

Forge Admin 里，你在任何子模块的 POM 中都**看不到一个 `<version>` 标签**。所有版本号集中在 `forge-dependencies` 这一个文件里。

`forge-dependencies/pom.xml`（534 行）完成了三件事：

**第一，导入外部 BOM**

```xml
<dependencyManagement>
    <dependencies>
        <!-- 继承 Spring Boot 官方版本管理 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.2.9</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-bom</artifactId>
            <version>5.8.31</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Spring Boot 官方 BOM 已经管理了 200+ 个三方库的版本，你不需要再手写。

**第二，覆盖需要锁定的三方库版本**

```xml
<!-- forge-dependencies 中覆盖/补充的三方库版本 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot3-starter</artifactId>
    <version>1.38.0</version>
</dependency>
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.7</version>
</dependency>
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.34.1</version>
</dependency>
<!-- ... 共 30+ 个三方库 -->
```

**第三，统一内部模块版本**

```xml
<!-- 所有内部模块版本 = ${revision} = 1.0.0 -->
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-core</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-orm</artifactId>
    <version>${revision}</version>
</dependency>
<!-- 20 个 Starter + 8 个 Plugin 全部在此统一版本 -->
```

### 3.2 子模块怎么引依赖

有了 BOM，子模块的 POM 极简——只声明 GAV 坐标，版本号自动继承：

**最简 Starter（`forge-starter-orm/pom.xml`，40 行）：**

```xml
<parent>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-parent</artifactId>
</parent>

<dependencies>
    <!-- 内部依赖：无版本号 -->
    <dependency>
        <groupId>com.mdframe.forge</groupId>
        <artifactId>forge-starter-core</artifactId>
    </dependency>
    <!-- 三方依赖：无版本号，版本来自 BOM -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>dynamic-datasource-spring-boot3-starter</artifactId>
    </dependency>
</dependencies>
```

**主应用入口（`forge-admin-server/pom.xml`，153 行）：**

```xml
<dependencies>
    <!-- 业务插件：按需引入 -->
    <dependency><groupId>com.mdframe.forge</groupId>
        <artifactId>forge-plugin-system</artifactId></dependency>
    <dependency><groupId>com.mdframe.forge</groupId>
        <artifactId>forge-plugin-flow</artifactId></dependency>
    <dependency><groupId>com.mdframe.forge</groupId>
        <artifactId>forge-plugin-message</artifactId></dependency>

    <!-- 技术启动器：按需引入 -->
    <dependency><groupId>com.mdframe.forge</groupId>
        <artifactId>forge-starter-web</artifactId></dependency>
    <dependency><groupId>com.mdframe.forge</groupId>
        <artifactId>forge-starter-auth</artifactId></dependency>

    <!-- 外部依赖：无版本号 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>
</dependencies>
```

**整个项目，版本号只需要改一个文件：`forge-dependencies/pom.xml`。** 你要升级 MyBatis-Plus？改一处，35 个模块全部生效。

### 3.3 版本号传递链路

```
forge-dependencies/pom.xml
  └─ <dependencyManagement>
       ├─ spring-boot-dependencies:3.2.9  (import scope) → 200+ 个三方库版本
       ├─ hutool-bom:5.8.31               (import scope) → hutool 全系版本
       ├─ flowable-bom:7.0.1              (import scope) → flowable 全系版本
       ├─ sa-token-*:1.38.0               (显式覆盖)
       ├─ mybatis-plus-*:3.5.7            (显式覆盖)
       ├─ redisson-*:3.34.1               (显式覆盖)
       ├─ ... 30+ 个三方库覆盖 ...
       └─ forge-starter-*:${revision}     (内部模块，20个)
          forge-plugin-*:${revision}      (内部模块，8个)

根 POM
  └─ <dependencyManagement>
       ├─ spring-boot-dependencies:3.5.13  ← 实际生效的 Spring Boot 版本
       └─ forge-dependencies:1.0.0         ← 导入上述所有版本

任何子模块声明依赖时：
  <dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <!-- 版本号由根 POM → forge-dependencies 自动提供 -->
  </dependency>
```

---

## 4. 模块边界：什么能依赖什么

### 4.1 Starter 之间的依赖规则

Starter 只依赖比自己更"底层"的 Starter，形成单向链：

```
forge-starter-core        ← 最底层：工具类、异常、统一响应
    ↑
forge-starter-orm         ← 依赖 core，提供 MyBatis-Plus + 数据源
    ↑
forge-starter-cache       ← 依赖 core，提供 Redis + Redisson
    ↑
forge-starter-tenant      ← 依赖 core + orm(optional) + cache，提供多租户
    ↑
forge-starter-datascope   ← 依赖 core + orm，提供数据权限
    ↑
forge-starter-crypto      ← 依赖 core + cache，提供加解密
    ↑
forge-starter-auth        ← 依赖 core + cache，提供认证授权
    ↑
forge-starter-excel       ← 依赖 core + orm，提供导入导出
    ↑
forge-starter-file        ← 依赖 core + orm，提供文件存储
```

**关键约束**：
- `forge-starter-tenant` 对 ORM 的依赖设为 `optional=true`，因为理论上租户隔离可以不依赖 MyBatis
- 所有 Starter 都可以依赖 `forge-starter-core`，它是唯一没有上级依赖的模块
- 跨 Starter 协调逻辑上提到 Plugin 或 Controller 层

### 4.2 Plugin 聚合 Starter

Plugin（业务插件）可以依赖任意 Starter，但不能反向：

`forge-plugin-system/pom.xml` 是聚合的典范：

```xml
<dependencies>
    <!-- 它几乎依赖了所有 Starter -->
    forge-starter-core, forge-starter-orm, forge-starter-auth,
    forge-starter-log, forge-starter-trans, forge-starter-excel,
    forge-starter-file, forge-starter-datascope, forge-starter-tenant,
    forge-starter-crypto, forge-starter-social, forge-starter-message
</dependencies>
```

Plugin 是真正的"业务承载体"——它把通用技术能力（Starter）组装成完整的业务功能（用户管理、角色管理、菜单管理等）。

### 4.3 应用层只做组装

`forge-admin-server` 的 POM 不包含任何业务代码，只做一件事：**声明需要的 Plugin 和 Starter，Spring Boot 启动**。

```xml
<dependencies>
    <!-- 宣布：我需要这些能力 -->
    forge-plugin-system, forge-plugin-flow, forge-plugin-message,
    forge-plugin-generator, forge-plugin-ai, forge-plugin-data,
    forge-starter-web, forge-starter-auth, forge-starter-crypto,
    forge-starter-config, forge-starter-api-config, forge-starter-social
</dependencies>
```

想裁剪？注释掉不需要的插件依赖即可。没有任何一个模块会因为缺少依赖而无法编译——因为所有模块之间的依赖都是在 POM 中显式声明的，不存在隐式依赖。

---

## 5. 关键取舍和踩坑

### 5.1 为什么不把所有模块平铺在根 POM 下？

很多多模块项目把所有子模块都列在根 POM 的 `<modules>` 下，形成一个扁平列表：

```xml
<!-- ❌ 不推荐 -->
<modules>
    <module>forge-starter-core</module>
    <module>forge-starter-orm</module>
    <module>forge-starter-cache</module>
    <module>forge-plugin-system</module>
    ... 35 个模块
</modules>
```

Forge Admin 选择**嵌套聚合**：根 POM → `forge-framework` → `starter-parent` / `plugin-parent` → 具体模块。

这种设计的好处：

1. **逻辑分组**：一眼看出哪些是 Starter（技术能力），哪些是 Plugin（业务功能）
2. **独立构建**：可以 `mvn install -pl forge-framework/forge-starter-parent` 只构建 Starter
3. **依赖隔离**：Plugin 的 POM 不会意外依赖到应用层的东西

### 5.2 为什么 forge-dependencies 是一个独立模块？

很多项目会把 `<dependencyManagement>` 直接写在根 POM 里。Forge Admin 把它抽成了独立模块 `forge-dependencies`，原因有三：

1. **可复用**：如果将来有其他外部项目想依赖 Forge 的版本管理，可以直接 import 这个 BOM
2. **职责单一**：根 POM 管构建插件和聚合，forge-dependencies 只管版本号
3. **可独立发布**：BOM 可以单独发版，不绑定根 POM 的版本

### 5.3 `optional = true` 的使用哲学

`forge-starter-tenant` 对 ORM 的依赖标记为 `optional`：

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-orm</artifactId>
    <optional>true</optional>
</dependency>
```

这意味着：如果你的项目用了 ORM，租户模块自动获得 SQL 拦截能力；如果你用了纯 JDBC 或其他 ORM，租户模块不会强制拉入 MyBatis-Plus。这叫「能力增强，但不绑架」。

### 5.4 常见踩坑

| 问题 | 原因 | 解决 |
|------|------|------|
| 版本号不一致导致 `NoSuchMethodError` | 子模块 POM 里手写了 `<version>` | 删掉，版本号交给 BOM |
| `dependencyManagement` 改了但没生效 | 根 POM 和 BOM 各声明了一份不同版本 | 以根 POM 为准（就近优先），统一版本声明源 |
| 新增模块后 `mvn install` 报找不到 | 忘了在父 POM 的 `<modules>` 中声明 | 在 `starter-parent/pom.xml` 或 `plugin-parent/pom.xml` 添加 `<module>` |
| 循环依赖 | A → B，后来 B → A | 上提公共接口到上一层，或拆出独立模块 |
| 引入一个 Starter 却拉入了不需要的依赖 | 传递依赖过多 | 检查是否需要加 `<optional>true</optional>` |
| `mvn clean install -DskipTests` 太慢 | 每次都全量构建 35 个模块 | 用 `-pl` 指定模块，如 `-pl forge-admin-server -am` |

---

## 6. 如何二开：新增一个模块

假设你要新增 `forge-starter-dingtalk`（钉钉集成），标准流程：

**第一步**：确定归属层级。这是一个技术能力 → 放在 `forge-framework/forge-starter-parent/` 下。

**第二步**：在 `forge-dependencies/pom.xml` 的 `<dependencyManagement>` 中注册：
```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-dingtalk</artifactId>
    <version>${revision}</version>
</dependency>
```

**第三步**：在 `forge-starter-parent/pom.xml` 的 `<modules>` 中声明：
```xml
<module>forge-starter-dingtalk</module>
```

**第四步**：创建模块目录和 POM：
```xml
<parent>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-parent</artifactId>
</parent>
<artifactId>forge-starter-dingtalk</artifactId>

<dependencies>
    <dependency>
        <groupId>com.mdframe.forge</groupId>
        <artifactId>forge-starter-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.dingtalk</groupId>
        <artifactId>dingtalk-sdk</artifactId>
        <!-- 版本号在 forge-dependencies 中统一管理 -->
    </dependency>
</dependencies>
```

**第五步**：在 `forge-admin-server/pom.xml` 中引入（可选，按需）：

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-dingtalk</artifactId>
</dependency>
```

完成。新模块自动集成到整个项目的版本管理和构建体系中，零额外配置。

---

## 7. 体验入口和下一篇预告

Forge Admin 的 35+ 个 Maven 模块全部遵循这套分层规则，你可以打开项目亲自查看：

```bash
# 只构建框架层（跳过应用层）
cd forge && mvn clean install -pl forge-framework -DskipTests

# 只构建 Starter
cd forge && mvn clean install -pl forge-framework/forge-starter-parent -am -DskipTests

# 构建全项目
cd forge && mvn clean install -DskipTests
```

- 在线演示：http://www.dlforgelab.com:8084/forge/login
- 默认账号：admin / 123456
- Gitee：https://gitee.com/ForgeLab/forge-admin
- GitHub：https://github.com/yaomindong1996/forge-admin

**下一篇预告**：A04｜统一响应、异常处理和日志：后台系统最容易被低估的基础设施——拆开 `forge-starter-core`、`forge-starter-web` 和 `GlobalExceptionHandler`。
