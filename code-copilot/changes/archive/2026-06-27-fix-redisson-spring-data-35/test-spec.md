# 修复登录 Redisson Spring Data 适配死循环测试计划

## 本轮增量验证

变更范围：

- 后端 Maven 依赖版本管理：`redisson.version`。

P0 必跑：

- `git diff --check` 覆盖本轮 POM 和变更文档。
- `mvn -pl forge-framework/forge-starter-parent/forge-starter-cache -am dependency:tree -Dincludes=org.redisson -DskipTests`，确认 Redisson starter 解析到 `redisson-spring-data-35`。
- `mvn -pl forge-admin-server -am compile -DskipTests`，确认 admin 入口依赖聚合编译通过。

P1 条件验证：

- 本地 MySQL、Redis 可用时，启动 `forge-admin-server` 并调用 `/auth/login` 验证登录不再触发 `StackOverflowError`。

跳过说明：

- 本轮根因在依赖二进制兼容性，优先以依赖树和 admin 编译闭环验证；完整登录接口需要本地数据库、Redis 和配置可用。
