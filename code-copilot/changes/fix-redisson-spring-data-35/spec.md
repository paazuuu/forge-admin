# 修复登录 Redisson Spring Data 适配死循环
> status: apply
> created: 2026-06-24
> complexity: 🟢简单

## 1. 背景与目标

`/auth/login` 登录时报 `StackOverflowError`。初步诊断为 Spring Boot 3.5.13 带来的 Spring Data Redis 3.5 接口变更，与当前 Redisson 3.34.1 依赖的 `redisson-spring-data-33` 不兼容。

Spring Data Redis 3.5 的 `RedisKeyCommands` 新增 `pExpire(byte[], Expiration, ExpirationOptions)` 签名；旧版 Redisson Spring Data 适配层未实现该签名时，会触发接口 default 方法与 Redisson 旧方法之间的递归调用，最终栈溢出。

本变更目标：

- 将 Forge 后端 Redisson 管理版本升级到包含 Spring Data Redis 3.5 适配层的版本。
- 确保 `forge-starter-cache` 不再解析到 `redisson-spring-data-33`。
- 保持变更范围仅限依赖版本管理，不改登录业务逻辑。

## 2. 范围

本期修改：

- `forge-server/pom.xml`
- `forge-server/forge-framework/forge-dependencies/pom.xml`

本期不做：

- 不调整 Spring Boot 版本。
- 不改 Redis、Sa-Token 或登录 Controller/Service 逻辑。
- 不启动依赖本地数据库和 Redis 的完整登录链路。

## 3. 方案

将 `redisson.version` 从 `3.34.1` 升级到 `3.50.0`。该版本的 `redisson-spring-boot-starter` 解析到 `redisson-spring-data-35`，与 Spring Data Redis 3.5.x 的接口签名匹配。

## 4. 风险与回滚

风险：

- Redisson 版本跨度较大，需要关注运行时 Redis 配置兼容性和锁行为。

回滚：

- 如验证发现新的 Redisson 运行时兼容问题，可回退本次两个 POM 中的 `redisson.version`，再选择其他包含 `redisson-spring-data-35` 的 Redisson 版本重试。
