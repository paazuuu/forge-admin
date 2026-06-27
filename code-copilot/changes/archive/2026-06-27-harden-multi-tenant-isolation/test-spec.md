# Test Spec

## 本轮增量验证

### P0

- `git diff --check`：检查补丁空白和格式。
- 后端编译：`mvn -pl forge-admin-server -am compile -DskipTests`。
- Mapper XML well-formed 检查：`xmllint --noout --nonet`。
- 只读查询开发库表结构：确认 `ai_`、`sys_` 表 `tenant_id` 覆盖情况。
- 只读查询开发库唯一索引：确认 V1.0.56 要替换的流程唯一索引名真实存在。

### P1

- Flyway 迁移实跑：需要可丢弃 MySQL 沙箱。当前本机 3407 未启动，3306 需要本地密码；本轮未直接写远程开发库。
- 前端构建：本轮没有继续改前端代码，沿用既有前端改动，暂不重复执行。

## 本轮增量验证（平台配置权限收口）

### P0

- `git diff --check`：检查补丁空白和格式。
- 后端编译：`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests`。
- 前端构建：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`。

### P1

- 启动后端后用超级管理员、租户管理员分别调用平台配置接口，验证租户管理员返回 403；本轮未启动服务，留到接口联调阶段执行。
- Flyway 迁移实跑仍需要可丢弃 MySQL 沙箱。

## 本轮增量验证（Flyway 1.0.56 失败修复）

### P0

- 远程开发库 Flyway 状态确认：确认 `forge_schema_history` 不存在失败的 1.0.56 记录后，通过后端启动触发 Flyway 正式迁移。
- 后端启动验证：在 `forge/forge-admin-server` 目录执行 `mvn org.springframework.boot:spring-boot-maven-plugin:3.5.13:run -Dspring-boot.run.profiles=dev -DskipTests`，观察 Flyway 和应用启动日志。
- 迁移后数据库核验：确认 `forge_schema_history` 中 1.0.56 `success=1`；确认字典历史 tenant 0 重复行已清理；确认文件、公告关系、流程目标表 `tenant_id` 列和租户索引已落库。
- `git diff --check`：检查补丁空白和格式。

### P1

- 本轮没有重新执行完整 Maven reactor 编译；后端启动过程已完成 `forge-admin-server` 模块编译阶段并成功启动。
- 本轮未清理启动期间 Quartz 示例任务写入的少量 `sys_job_log`，避免对开发库做额外破坏性数据操作。
