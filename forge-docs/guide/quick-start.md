# 快速入门

> 5 分钟内把 Forge Admin 跑起来，体验完整的后台管理系统。

---

## 一、环境准备

在开始之前，请确保你的开发环境已安装以下软件：

| 环境 | 版本要求 | 验证命令 |
|------|---------|---------|
| JDK | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| MySQL | 8.0+ | `mysql --version` |
| Redis | 6.0+ | `redis-cli ping` |
| Node.js | 20.19+ | `node -v` |
| pnpm | 8+ | `pnpm -v` |

::: tip 未安装？
请参考 [环境搭建指南](./environment-setup.md) 获取各平台的详细安装步骤。
:::

---

## 二、获取代码

```bash
git clone https://gitee.com/ForgeLab/forge-admin.git
cd forge-admin
```

仓库结构一览：

```
forge-admin/
├── forge-server/          # 后端工程（Spring Boot 3.x + JDK 17）
├── forge-admin-ui/        # 前端工程（Vue 3 + Vite + Naive UI）
├── forge-report-ui/       # AI 大屏前端
├── forge-docs/            # VitePress 文档站
├── docker/                # Docker 部署文件
└── images/                # 项目截图
```

---

## 三、初始化数据库

### 3.1 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS forge_admin
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 3.2 执行初始化脚本

Forge Admin 提供了一键初始化脚本，会自动执行历史 SQL、Flyway 迁移脚本和必需的种子数据：

```bash
bash forge-server/scripts/db/init-db.sh \
  --host 127.0.0.1 \
  --port 3306 \
  --database forge_admin \
  --user root \
  --password your_password
```

如需导入演示数据，追加 `--with-demo` 参数：

```bash
bash forge-server/scripts/db/init-db.sh \
  --database forge_admin \
  --user root \
  --password your_password \
  --with-demo
```

::: warning Flyway 自动迁移
除了手动执行初始化脚本外，后端服务首次启动时 Flyway 也会自动执行 `forge-server/db/migration/` 下的迁移脚本。你可以通过以下 SQL 查看迁移状态：

```sql
SELECT installed_rank, version, description, success
FROM forge_schema_history
ORDER BY installed_rank DESC;
```
:::

---

## 四、启动后端

### 4.1 准备配置文件

```bash
cp forge-server/forge-admin-server/src/main/resources/application-dev.example.yml \
   forge-server/forge-admin-server/src/main/resources/application-dev.yml
```

编辑 `application-dev.yml`，修改 MySQL 和 Redis 连接信息为你本地的配置：

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/forge_admin?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&autoReconnect=true&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true
          username: root
          password: 'your_password'  # ← 改成你的 MySQL 密码

spring.data:
  redis:
    host: localhost
    port: 6379
    password: 'your_redis_password'  # ← 改成你的 Redis 密码
```

::: warning 配置安全
`application-dev.yml` 包含本地敏感信息，已在 `.gitignore` 中忽略，不会提交到仓库。请勿将真实密码提交到代码仓库。
:::

### 4.2 启动主服务

```bash
cd forge-server/forge-admin-server
mvn spring-boot:run
```

启动成功后，后端服务运行在 `http://localhost:8580`。

![后端启动成功](https://gitee.com/ForgeLab/forge-admin/raw/main/images/dashboard.png)

> 💡 如果看到 `Started ForgeAdminApplication` 日志，说明后端已就绪。

### 4.3 （可选）启动流程服务

如果你的业务需要工作流审批功能，额外启动流程服务：

```bash
cd forge-server/forge-flow/forge-flow-server
mvn spring-boot:run
```

流程服务运行在 `http://localhost:8081`。

---

## 五、启动前端

### 5.1 安装依赖

```bash
cd forge-admin-ui
pnpm install
```

### 5.2 启动开发服务器

```bash
pnpm dev
```

启动后访问 `http://localhost:3000/forge/login`，使用默认账号登录：

| 账号 | 密码 |
|------|------|
| `admin` | `123456` |

![登录页面](https://gitee.com/ForgeLab/forge-admin/raw/main/images/%E7%99%BB%E5%BD%95%E9%A1%B5.png)

---

## 六、验证启动

登录成功后，你应该能看到：

1. **工作台首页**：展示在线用户、今日登录、总用户数等统计信息
2. **左侧菜单**：系统管理、AI 大屏等功能模块
3. **用户管理**：组织架构树 + 用户列表

![工作台首页](https://gitee.com/ForgeLab/forge-admin/raw/main/images/dashboard.png)

如果以上页面都能正常访问，恭喜你 🎉 Forge Admin 已经成功运行！

---

## 七、常见问题

### Q1: 后端启动报数据库连接失败？

1. 检查 `application-dev.yml` 中的 MySQL 地址、端口、用户名、密码
2. 确认 MySQL 服务已启动：`systemctl status mysql`
3. 确认数据库 `forge_admin` 已创建
4. 如果使用云数据库，检查白名单是否放行了本机 IP

### Q2: 后端启动报 Redis 连接失败？

1. 检查 `application-dev.yml` 中的 Redis 地址和密码
2. 确认 Redis 服务已启动：`redis-cli ping` 应返回 `PONG`
3. 如果 Redis 无密码，将 `password` 配置项留空或删除

### Q3: 前端启动后页面白屏？

1. 检查浏览器控制台是否有报错
2. 确认 Node.js 版本 ≥ 20.19：`node -v`
3. 删除 `node_modules` 重新安装：`rm -rf node_modules && pnpm install`

### Q4: 前端请求接口报 404 或跨域错误？

1. 检查 `forge-admin-ui/.env.development` 中的 `VITE_HTTP_PROXY_TARGET` 是否指向后端地址
2. 确认后端服务已启动且端口为 8580
3. 检查浏览器 Network 面板，请求路径是否包含 `/dev-api` 前缀

### Q5: Flyway 迁移报错？

1. 检查 `FORGE_FLYWAY_ENABLED` 环境变量是否被设置
2. 确认数据库版本为 MySQL 8.0+
3. 查看 `forge_schema_history` 表中是否有失败的迁移记录
4. 如需重新执行，删除对应迁移记录后重启

---

## 八、下一步

- 📖 [环境搭建指南](./environment-setup.md) — 各平台详细安装步骤与踩坑
- 🔐 [权限体系完整配置指南](./permission-guide.md) — RBAC 权限模型全链路配置
- 🛠️ [二次开发实战教程](./development-tutorial.md) — 从零开发一个业务模块
- 🚀 [部署指南](./deployment-guide.md) — Docker 容器化与 Nginx 生产部署
- 📝 [SDD 工作流](./sdd-workflow.md) — Spec 驱动开发方法论

::: tip 在线体验
不想本地搭建？可以直接访问在线演示环境：
http://www.dlforgelab.com:8084/forge/login （账号 `admin` / `123456`）
:::
