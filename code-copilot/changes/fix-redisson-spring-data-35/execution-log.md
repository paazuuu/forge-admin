# 修复登录 Redisson Spring Data 适配死循环执行日志

## 2026-06-24 依赖升级验证

执行时间：2026-06-24 11:02:01 CST

变更范围：

- `forge-server/pom.xml`：`redisson.version` 从 `3.34.1` 升级到 `3.50.0`。
- `forge-server/forge-framework/forge-dependencies/pom.xml`：同步升级 `redisson.version`。
- 新增当前变更的 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- `code-copilot/memory/pitfalls.md` 追加 Redisson/Spring Data Redis 版本不匹配踩坑记录。

验证命令与结果：

- `mvn -pl forge-framework/forge-starter-parent/forge-starter-cache -am dependency:tree -Dincludes=org.redisson -DskipTests`
  - 变更前结果：通过，依赖树显示 `redisson-spring-boot-starter:3.34.1`、`redisson:3.34.1`、`redisson-spring-data-33:3.34.1`。
- `mvn -pl forge-framework/forge-starter-parent/forge-starter-cache -am dependency:tree -Dincludes=org.redisson -DskipTests`
  - 变更后结果：通过，依赖树显示 `redisson-spring-boot-starter:3.50.0`、`redisson:3.50.0`、`redisson-spring-data-35:3.50.0`。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests`
  - 结果：失败，原因不是源码编译错误；Maven 需要下载新 Redisson JAR，但当前沙箱不允许写入 `/Users/yaomindong/.m2/repository/.../*.lastUpdated`，报 `Operation not permitted`。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -s ../tmp-forge-maven-settings.xml -pl forge-admin-server -am compile -DskipTests`
  - 结果：失败，已改用 `/private/tmp/forge-m2` 可写本地仓库，但 Maven/JVM 解析远程仓库域名间歇失败，报 `Unknown host repo.maven.apache.org` / `Unknown host repo.spring.io`，未进入 Java 源码编译阶段。
- `curl -I -L --max-time 20 https://repo.maven.apache.org/maven2/org/redisson/redisson-spring-boot-starter/3.50.0/redisson-spring-boot-starter-3.50.0.jar`
  - 结果：一度返回 `HTTP/2 200`，确认 Central 存在该 JAR；随后 DNS 解析间歇失败，无法稳定下载 JAR 完成编译。
- `git diff --check -- forge-server/pom.xml forge-server/forge-framework/forge-dependencies/pom.xml code-copilot/changes/fix-redisson-spring-data-35/spec.md code-copilot/changes/fix-redisson-spring-data-35/tasks.md code-copilot/changes/fix-redisson-spring-data-35/test-spec.md code-copilot/changes/fix-redisson-spring-data-35/execution-log.md`
  - 结果：通过，无空白错误。

警告：

- 本轮未完成 `forge-admin-server` 编译闭环，阻塞点是 Maven 依赖下载环境，不是依赖树或源码编译错误。

跳过项：

- 未启动后端服务和未执行 `/auth/login` curl 验证，原因是本地编译依赖下载未完成，且未确认当前环境的 MySQL/Redis 可用。

服务清理：

- 本轮未启动任何长期运行服务，无需清理 PID。
