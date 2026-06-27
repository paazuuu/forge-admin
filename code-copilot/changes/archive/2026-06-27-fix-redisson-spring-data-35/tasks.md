# 修复登录 Redisson Spring Data 适配死循环任务

- [x] 确认当前依赖树复现 `redisson-spring-data-33`。
- [x] 升级 Forge 后端 Redisson 管理版本到 Spring Data Redis 3.5 兼容版本。
- [x] 复查依赖树确认解析到 `redisson-spring-data-35`。
- [x] 执行后端最小编译验证并记录结果（编译被 Maven 仓库 DNS/本地仓库写权限阻塞，见 `execution-log.md`）。
