# 部署指南

> 本指南覆盖 Docker 容器化部署、传统手动部署、Nginx 反向代理配置三种方式，帮你把 Forge Admin 推上生产环境。

---

## 一、部署架构总览

Forge Admin 生产环境由以下服务组成：

```
                    ┌─────────────┐
                    │   浏览器/H5   │
                    └──────┬──────┘
                           │ HTTP
                    ┌──────▼──────┐
                    │    Nginx    │  ← 静态资源 + 反向代理
                    │  (端口 80)   │
                    └──┬───┬───┬──┘
            ┌─────────┘   │   └─────────┐
            │             │             │
     /forge/ 静态   /forge-api/    /forge-api/api/flow/
            │             │             │
     ┌──────▼──────┐ ┌────▼─────┐ ┌─────▼──────┐
     │  前端 dist  │ │  Admin   │ │   Flow     │
     │  (Vue 3)   │ │ (8580)   │ │  (8081)    │
     └─────────────┘ └────┬─────┘ └─────┬──────┘
                          │             │
                   ┌──────┴─────┐ ┌─────┴──────┐
                   │   MySQL    │ │   Redis    │
                   │  (3306)    │ │  (6379)    │
                   └────────────┘ └────────────┘
```

| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx | 80 | 前端静态资源 + API 反向代理 |
| forge-admin | 8580 | 后端主服务 |
| forge-flow | 8081 | 流程服务（可选） |
| MySQL | 3306 | 主数据库 |
| Redis | 6379 | 缓存与会话存储 |

---

## 二、Docker 容器化部署（推荐）

### 2.1 前置条件

- Docker 20.10+
- Docker Compose 2.0+

```bash
# 验证
docker --version
docker compose version
```

### 2.2 配置环境变量

```bash
cd forge-admin/docker
cp .env.example .env
```

编辑 `.env` 文件，修改密码和端口：

```env
# MySQL 配置
MYSQL_PASSWORD=your_secure_password
MYSQL_DB=forge_admin
MYSQL_PORT=3306

# Redis 配置
REDIS_PASSWORD=your_secure_redis_password
REDIS_PORT=6379

# 后端服务端口
ADMIN_PORT=8580
FLOW_PORT=8081

# Nginx 端口
NGINX_PORT=80
```

::: warning 安全提示
生产环境请使用强密码（至少 16 位，包含大小写字母、数字、特殊字符）。`.env` 文件不要提交到 Git 仓库。
:::

### 2.3 一键启动

```bash
cd forge-admin/docker
docker compose up -d
```

Docker Compose 会自动完成以下步骤：

1. 拉取 MySQL 8.0 和 Redis 7 镜像
2. 构建 forge-admin、forge-flow、forge-ui 三个镜像
3. 创建 Docker 网络和数据卷
4. 按依赖顺序启动所有服务

![Docker 启动成功示意](https://gitee.com/ForgeLab/forge-admin/raw/main/images/dashboard.png)

### 2.4 查看服务状态

```bash
# 查看所有容器状态
docker compose ps

# 查看后端日志
docker compose logs -f forge-admin

# 查看前端 Nginx 日志
docker compose logs -f forge-ui
```

预期输出：

```
NAME           STATUS     PORTS
forge-mysql    Up         0.0.0.0:3306->3306/tcp
forge-redis    Up         0.0.0.0:6379->6379/tcp
forge-flow     Up         0.0.0.0:8081->8081/tcp
forge-admin    Up         0.0.0.0:8580->8580/tcp
forge-ui       Up         0.0.0.0:80->80/tcp
```

### 2.5 访问验证

浏览器打开 `http://你的服务器IP/forge/login`，使用默认账号 `admin / 123456` 登录。

### 2.6 停止与清理

```bash
# 停止所有服务（保留数据）
docker compose down

# 停止并删除数据卷（⚠️ 会丢失所有数据）
docker compose down -v
```

---

## 三、Docker Compose 编排详解

### 3.1 服务编排结构

项目提供的 `docker/docker-compose.yml` 包含 5 个服务：

```yaml
services:
  mysql:        # MySQL 8.0 数据库
    # 数据持久化到 mysql_data 卷
    # 健康检查：mysqladmin ping

  redis:        # Redis 7 缓存
    # 数据持久化到 redis_data 卷
    # 健康检查：redis-cli ping

  forge-flow:   # 流程服务（先于 admin 启动）
    # 依赖 mysql + redis 健康检查通过
    # JDK 17 运行，256M-512M 内存

  forge-admin:  # 后端主服务
    # 依赖 mysql + redis + forge-flow
    # JDK 17 运行，256M-512M 内存

  forge-ui:     # 前端 Nginx
    # 依赖 forge-admin + forge-flow
    # Nginx 1.25 托管静态资源 + 反向代理
```

### 3.2 Dockerfile 说明

| Dockerfile | 构建阶段 | 产物 |
|------------|---------|------|
| `Dockerfile.admin` | Maven 编译 → JRE 运行 | `forge-admin.jar` |
| `Dockerfile.flow` | Maven 编译 → JRE 运行 | `forge-flow.jar` |
| `Dockerfile.ui` | pnpm 构建 → Nginx 托管 | 静态文件 |

三个 Dockerfile 都采用**多阶段构建**，最终镜像不含编译工具，体积更小更安全。

### 3.3 自定义构建

如需修改构建参数（如 JVM 内存、时区），编辑 `docker-compose.yml` 中对应服务的 `environment`：

```yaml
forge-admin:
  environment:
    JAVA_OPTS: -Xms512m -Xmx1024m  # 增大 JVM 内存
    SPRING_PROFILES_ACTIVE: prod
    TZ: Asia/Shanghai
```

---

## 四、传统手动部署

适用于无法使用 Docker 或需要精细控制的场景。

### 4.1 部署数据库

#### 安装 MySQL 8.0

```bash
# Ubuntu/Debian
apt install -y mysql-server-8.0
systemctl start mysql
systemctl enable mysql
```

#### 创建数据库并初始化

```bash
# 创建数据库
mysql -u root -p <<'SQL'
CREATE DATABASE IF NOT EXISTS forge_admin
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
SQL

# 执行初始化脚本
bash forge-server/scripts/db/init-db.sh \
  --host 127.0.0.1 \
  --port 3306 \
  --database forge_admin \
  --user root \
  --password *** \
  --with-demo
```

#### 安装 Redis

```bash
apt install -y redis-server
systemctl start redis-server
systemctl enable redis-server

# 设置密码
redis-cli CONFIG SET requirepass "your_redis_password"
```

### 4.2 构建并部署后端

#### 编译打包

```bash
cd forge-admin/forge-server
mvn clean package -pl forge-admin -am -DskipTests -Pprod
```

产物：`forge-admin-server/target/forge-admin.jar`

#### 准备配置文件

```bash
mkdir -p /opt/forge-admin/config
cp forge-admin-server/src/main/resources/application.yml /opt/forge-admin/config/
```

创建生产环境配置 `application-prod.yml`：

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:mysql://127.0.0.1:3306/forge_admin?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&autoReconnect=true&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true
          username: root
          password: 'your_secure_password'

spring.data:
  redis:
    host: 127.0.0.1
    port: 6379
    password: 'your_redis_password'
```

#### 创建系统服务

```bash
cat > /etc/systemd/system/forge-admin.service <<'EOF'
[Unit]
Description=Forge Admin Server
After=network.target mysql.service redis-server.service

[Service]
Type=simple
User=forge
WorkingDirectory=/opt/forge-admin
ExecStart=/usr/bin/java -Xms512m -Xmx1024m \
  -Dspring.profiles.active=prod \
  -Dspring.config.additional-location=/opt/forge-admin/config/ \
  -jar /opt/forge-admin/forge-admin.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl start forge-admin
systemctl enable forge-admin
```

#### 验证后端

```bash
# 查看服务状态
systemctl status forge-admin

# 查看日志
journalctl -u forge-admin -f

# 测试接口
curl http://localhost:8580/actuator/health
```

### 4.3 构建并部署前端

#### 编译打包

```bash
cd forge-admin/forge-admin-ui
pnpm install
pnpm build
```

产物：`forge-admin-ui/dist/` 目录

#### 部署到 Nginx

```bash
# 复制到 Nginx 站点目录
mkdir -p /var/www/html/forge
cp -r dist/* /var/www/html/forge/
```

### 4.4 （可选）部署流程服务

```bash
cd forge-admin/forge-server
mvn clean package -pl forge-flow -am -DskipTests -Pprod

# 产物: forge-flow/forge-flow-server/target/forge-flow.jar

# 创建系统服务
cat > /etc/systemd/system/forge-flow.service <<'EOF'
[Unit]
Description=Forge Flow Server
After=network.target mysql.service redis-server.service

[Service]
Type=simple
User=forge
WorkingDirectory=/opt/forge-flow
ExecStart=/usr/bin/java -Xms256m -Xmx512m \
  -Dspring.profiles.active=prod \
  -jar /opt/forge-flow/forge-flow.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl start forge-flow
systemctl enable forge-flow
```

---

## 五、Nginx 反向代理配置

### 5.1 安装 Nginx

```bash
apt install -y nginx
systemctl start nginx
systemctl enable nginx
```

### 5.2 配置站点

创建 `/etc/nginx/conf.d/forge-admin.conf`：

```nginx
server {
    listen 80;
    server_name your-domain.com;  # ← 替换为你的域名或 IP

    # 前端静态资源
    location /forge/ {
        alias   /var/www/html/forge/;
        index  index.html index.htm;
        try_files $uri $uri/ /forge/index.html;
    }

    # 流程服务 API（路径更长，优先匹配）
    location /forge-api/api/flow/ {
        proxy_send_timeout 3000;
        proxy_read_timeout 3000;
        proxy_connect_timeout 3000;
        proxy_pass http://127.0.0.1:8081/api/flow/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 后端主服务 API
    location /forge-api/ {
        proxy_send_timeout 3000;
        proxy_read_timeout 3000;
        proxy_connect_timeout 3000;
        proxy_pass http://127.0.0.1:8580/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 文件上传大小限制
    client_max_body_size 20m;

    # Gzip 压缩
    gzip on;
    gzip_types text/plain application/json application/javascript text/css;
    gzip_min_length 1000;
}
```

### 5.3 验证并重载

```bash
# 检查配置语法
nginx -t

# 重载配置（不中断服务）
nginx -s reload
```

### 5.4 前端构建配置

前端构建时需确保 `.env.production` 配置正确：

```env
# 静态资源目录
VITE_PUBLIC_PATH=/forge

# 路由前缀
VITE_BASE_URL=/forge

# 请求地址前缀（由 Nginx 代理）
VITE_REQUEST_PREFIX=/forge-api
```

::: danger 宝塔面板注意事项
宝塔面板默认有静态资源缓存规则 `location ~ .*\.(js|css)?$`，会覆盖 `alias` 配置导致 assets 404。

**解决方案**：注释掉或删除宝塔默认的 js/css 缓存规则。
配置文件路径通常为：`/www/server/panel/vhost/nginx/xxx.conf`
:::

---

## 六、HTTPS 配置（推荐）

### 6.1 申请 SSL 证书

```bash
# 使用 Let's Encrypt 免费证书
apt install -y certbot python3-certbot-nginx
certbot --nginx -d your-domain.com
```

### 6.2 Nginx HTTPS 配置

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate     /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    # ... 以下与 HTTP 配置相同 ...

    # 前端静态资源
    location /forge/ {
        alias   /var/www/html/forge/;
        try_files $uri $uri/ /forge/index.html;
    }

    # API 反向代理
    location /forge-api/api/flow/ {
        proxy_pass http://127.0.0.1:8081/api/flow/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /forge-api/ {
        proxy_pass http://127.0.0.1:8580/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    client_max_body_size 20m;
}

# HTTP 跳转 HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}
```

### 6.3 自动续期

```bash
# Let's Encrypt 证书 90 天过期，设置自动续期
echo "0 3 * * * certbot renew --quiet --post-hook 'nginx -s reload'" | crontab -
```

---

## 七、Flyway 数据库迁移

### 7.1 迁移机制

Forge Admin 使用 Flyway 管理数据库版本。后端服务启动时自动执行迁移脚本。

迁移脚本位于 `forge-server/db/migration/`，命名规则：

```
V{版本号}__{描述}.sql
```

例如：
- `V1.0.0__baseline.sql` — 基线脚本
- `V1.0.7__add_lowcode_business_domain.sql` — 新增低代码业务域
- `V1.0.80__add_business_suite_hierarchy.sql` — 新增业务套件层级

### 7.2 查看迁移状态

```sql
SELECT installed_rank, version, description, type, success, installed_on
FROM forge_schema_history
ORDER BY installed_rank DESC
LIMIT 10;
```

### 7.3 手动触发迁移

正常情况下服务启动时自动迁移。如需手动执行：

```bash
# 方式一：重启服务（推荐）
systemctl restart forge-admin

# 方式二：使用初始化脚本
bash forge-server/scripts/db/init-db.sh \
  --database forge_admin \
  --user root \
  --password *** \
  --skip-admin-init
```

### 7.4 迁移失败处理

```sql
-- 1. 查看失败的迁移记录
SELECT * FROM forge_schema_history WHERE success = 0;

-- 2. 修复问题后删除失败记录
DELETE FROM forge_schema_history WHERE success = 0;

-- 3. 重启服务，Flyway 会重新执行
```

::: warning 生产环境注意
- 迁移脚本一旦执行成功，不可修改
- 新增迁移脚本版本号必须递增，不可跳号
- 建议在测试环境验证后再应用到生产
:::

---

## 八、日志与监控

### 8.1 日志配置

后端使用 Logback，配置文件 `logback.xml`：

```xml
<!-- 日志输出路径 -->
<property name="LOG_PATH" value="/var/log/forge-admin" />

<!-- 滚动策略：每天一个文件，保留 30 天 -->
<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
    <fileNamePattern>${LOG_PATH}/forge-admin.%d{yyyy-MM-dd}.log</fileNamePattern>
    <maxHistory>30</maxHistory>
</rollingPolicy>
```

### 8.2 查看日志

```bash
# Docker 部署
docker compose logs -f forge-admin
docker compose logs -f forge-flow

# 传统部署
journalctl -u forge-admin -f
tail -f /var/log/forge-admin/forge-admin.log
```

### 8.3 健康检查

```bash
# 后端健康检查
curl http://localhost:8580/actuator/health

# 预期返回
# {"status":"UP"}
```

### 8.4 Docker 容器健康检查

```bash
# 查看容器健康状态
docker inspect --format='{{.State.Health.Status}}' forge-admin
docker inspect --format='{{.State.Health.Status}}' forge-mysql
docker inspect --format='{{.State.Health.Status}}' forge-redis
```

---

## 九、性能调优

### 9.1 JVM 参数

```bash
# 生产环境推荐（4G 内存服务器）
JAVA_OPTS="-Xms1g -Xmx2g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/forge-admin/heapdump.hprof"
```

### 9.2 MySQL 调优

```ini
# /etc/mysql/mysql.conf.d/mysqld.cnf
[mysqld]
innodb_buffer_pool_size = 2G        # 建议物理内存的 50-70%
innodb_log_file_size = 512M
max_connections = 500
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2
```

### 9.3 Redis 调优

```ini
# /etc/redis/redis.conf
maxmemory 512mb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
```

### 9.4 Nginx 调优

```nginx
# /etc/nginx/nginx.conf
worker_processes auto;
worker_connections 1024;

# /etc/nginx/conf.d/forge-admin.conf 中添加
gzip on;
gzip_comp_level 6;
gzip_types text/plain application/json application/javascript text/css image/svg+xml;
gzip_min_length 1000;

# 静态资源缓存
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
    expires 30d;
    add_header Cache-Control "public, immutable";
}
```

---

## 十、备份与恢复

### 10.1 数据库备份

```bash
# 手动备份
mysqldump -u root -p forge_admin > backup_$(date +%Y%m%d).sql

# 自动备份（crontab）
echo "0 2 * * * mysqldump -u root -p*** forge_admin > /backup/forge_admin_$(date +\%Y\%m\%d).sql" | crontab -
```

### 10.2 Docker 数据卷备份

```bash
# 备份 MySQL 数据卷
docker run --rm -v forge_mysql_data:/data -v $(pwd):/backup \
  alpine tar czf /backup/mysql_data_$(date +%Y%m%d).tar.gz /data

# 备份 Redis 数据卷
docker run --rm -v forge_redis_data:/data -v $(pwd):/backup \
  alpine tar czf /backup/redis_data_$(date +%Y%m%d).tar.gz /data
```

### 10.3 恢复

```bash
# 恢复数据库
mysql -u root -p forge_admin < backup_20260703.sql

# 恢复 Docker 数据卷
docker volume rm forge_mysql_data
docker volume create forge_mysql_data
docker run --rm -v forge_mysql_data:/data -v $(pwd):/backup \
  alpine tar xzf /backup/mysql_data_20260703.tar.gz -C /
```

---

## 十一、升级与回滚

### 11.1 Docker 部署升级

```bash
cd forge-admin

# 拉取最新代码
git pull origin main

# 重新构建并启动
cd docker
docker compose up -d --build
```

### 11.2 传统部署升级

```bash
# 1. 拉取代码
cd /opt/forge-admin
git pull origin main

# 2. 重新编译后端
cd forge-server
mvn clean package -pl forge-admin -am -DskipTests -Pprod

# 3. 替换 JAR
cp forge-admin-server/target/forge-admin.jar /opt/forge-admin/forge-admin.jar

# 4. 重新编译前端
cd forge-admin-ui
pnpm install && pnpm build
cp -r dist/* /var/www/html/forge/

# 5. 重启服务
systemctl restart forge-admin
```

### 11.3 回滚

```bash
# 回滚到指定版本
git checkout <previous-commit-hash>

# 重新编译部署
mvn clean package -pl forge-admin -am -DskipTests -Pprod
cp forge-admin-server/target/forge-admin.jar /opt/forge-admin/forge-admin.jar
systemctl restart forge-admin
```

::: tip 数据库回滚
如升级包含数据库迁移，回滚前需手动处理 `forge_schema_history` 表中新增的迁移记录。建议升级前做数据库快照。
:::

---

## 十二、常见问题

### Q1: Docker 构建失败，Maven 下载依赖超时？

在 Docker 构建前配置 Maven 镜像。编辑 `Dockerfile.admin`，在 `RUN mvn` 之前添加：

```dockerfile
COPY settings.xml /root/.m2/settings.xml
```

并提供包含阿里云镜像的 `settings.xml`。

### Q2: Nginx 配置后前端 CSS/JS 404？

检查 `alias` 配置，路径结尾必须加斜杠：

```nginx
# ✅ 正确
location /forge/ {
    alias   /var/www/html/forge/;
}

# ❌ 错误（少斜杠会导致路径拼接错误）
location /forge/ {
    alias   /var/www/html/forge;
}
```

### Q3: 后端启动后接口返回 403？

1. 检查 Sa-Token 配置是否正确
2. 确认 Redis 连接正常（Sa-Token 依赖 Redis 存储 Session）
3. 查看后端日志是否有权限拦截日志

### Q4: Docker 容器间无法通信？

1. 确认所有容器在同一 Docker 网络：`docker network inspect forge_docker_forge-network`
2. 检查 `docker-compose.yml` 中服务名是否正确（容器间用服务名通信）
3. 防火墙未拦截 Docker 内部网络

### Q5: 文件上传报 413 Request Entity Too Large？

Nginx 默认限制 1MB，需在配置中增加：

```nginx
client_max_body_size 20m;
```

同时检查后端 `application.yml` 中的 `spring.servlet.multipart.max-file-size` 配置。

### Q6: 如何修改默认 admin 密码？

登录系统后，进入 **个人中心 → 修改密码**，或通过管理员账号在 **系统管理 → 用户管理** 中重置。

---

## 十三、部署检查清单

| 序号 | 检查项 | 状态 |
|------|--------|------|
| 1 | MySQL 已安装且字符集为 utf8mb4 | ☐ |
| 2 | Redis 已安装且密码已设置 | ☐ |
| 3 | 数据库初始化脚本已执行 | ☐ |
| 4 | 后端服务启动且健康检查通过 | ☐ |
| 5 | 前端已构建并部署到 Nginx | ☐ |
| 6 | Nginx 反向代理配置正确 | ☐ |
| 7 | 浏览器能访问登录页面 | ☐ |
| 8 | admin / 123456 能成功登录 | ☐ |
| 9 | API 请求正常（非 404/403/500） | ☐ |
| 10 | HTTPS 证书已配置（生产环境） | ☐ |
| 11 | 数据库备份计划已设置 | ☐ |
| 12 | 日志轮转配置已生效 | ☐ |

---

## 相关文档

- 🚀 [快速入门](./quick-start.md) — 5 分钟本地运行
- 🔧 [环境搭建指南](./environment-setup.md) — 各平台详细安装步骤
- 🔐 [权限体系完整配置指南](./permission-guide.md) — RBAC 权限配置
- 🛠️ [二次开发实战教程](./development-tutorial.md) — 从零开发业务模块
