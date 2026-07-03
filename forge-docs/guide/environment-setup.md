# 环境搭建指南

> 面向新手的保姆级环境搭建教程，覆盖 Windows / macOS / Linux 三大平台，包含常见踩坑排查。

---

## 一、环境总览

Forge Admin 的开发环境由以下几部分组成：

| 组件 | 版本要求 | 用途 |
|------|---------|------|
| JDK | 17+ | 后端 Java 运行环境 |
| Maven | 3.8+ | 后端构建工具 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 6.0+ | 缓存与会话存储 |
| Node.js | 20.19+ | 前端运行环境 |
| pnpm | 8+ | 前端包管理器 |
| Git | 2.30+ | 版本控制 |

::: tip 一键检查
将以下命令保存为 `check-env.sh`，随时验证环境是否就绪：

```bash
#!/bin/bash
echo "=== JDK ===" && java -version 2>&1 | head -1
echo "=== Maven ===" && mvn -version 2>&1 | head -1
echo "=== MySQL ===" && mysql --version 2>&1
echo "=== Redis ===" && redis-cli --version 2>&1
echo "=== Node.js ===" && node -v 2>&1
echo "=== pnpm ===" && pnpm -v 2>&1
echo "=== Git ===" && git --version 2>&1
```
:::

---

## 二、JDK 17 安装

### 2.1 Windows

1. 下载 JDK 17 安装包（推荐 Eclipse Temurin）：
   - 下载地址：https://adoptium.net/temurin/releases/?version=17
   - 选择 `Windows x64 .msi` 安装包

2. 运行安装程序，勾选 **Set JAVA_HOME variable** 和 **Add to PATH**

3. 验证安装：
   ```powershell
   java -version
   # 输出应包含: openjdk version "17.x.x"
   ```

4. 如需手动配置环境变量：
   ```
   JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot
   Path 中添加: %JAVA_HOME%\bin
   ```

### 2.2 macOS

```bash
# 使用 Homebrew 安装
brew install openjdk@17

# 配置环境变量
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
source ~/.zshrc

# 验证
java -version
```

### 2.3 Linux (Ubuntu/Debian)

```bash
# 安装 Eclipse Temurin JDK 17
apt update
apt install -y temurin-17-jdk

# 或使用 SDKMAN 安装
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.13-tem

# 验证
java -version
```

::: warning 常见踩坑
- **JDK 版本过低**：Forge Admin 使用 Spring Boot 3.x，必须 JDK 17+，JDK 8/11 无法编译
- **多版本冲突**：如果系统已有 JDK 8，使用 `update-alternatives --config java` 切换默认版本
- **JAVA_HOME 未设置**：Maven 依赖 JAVA_HOME，未设置会报错
:::

---

## 三、Maven 安装

### 3.1 Windows

1. 下载 Maven：https://maven.apache.org/download.cgi（选 `apache-maven-3.9.x-bin.zip`）
2. 解压到 `C:\apache-maven-3.9.x`
3. 配置环境变量：
   ```
   MAVEN_HOME = C:\apache-maven-3.9.x
   Path 中添加: %MAVEN_HOME%\bin
   ```
4. 验证：`mvn -version`

### 3.2 macOS

```bash
brew install maven
mvn -version
```

### 3.3 Linux

```bash
apt install -y maven
# 或
sdk install maven

mvn -version
```

### 3.3 配置国内镜像（推荐）

在国内开发环境，配置阿里云 Maven 镜像可大幅提升依赖下载速度：

编辑 `~/.m2/settings.xml`（不存在则创建）：

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <name>Aliyun Maven Mirror</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

---

## 四、MySQL 8.0 安装

### 4.1 Windows

1. 下载 MySQL Installer：https://dev.mysql.com/downloads/installer/
2. 选择 **Server only** 安装类型
3. 设置 root 密码（请记住此密码）
4. 配置为 Windows 服务，开机自启
5. 验证：`mysql -u root -p`

### 4.2 macOS

```bash
brew install mysql@8.0
brew services start mysql@8.0

# 设置 root 密码
mysql_secure_installation

# 验证
mysql -u root -p
```

### 4.3 Linux (Ubuntu/Debian)

```bash
apt update
apt install -y mysql-server-8.0

# 启动服务
systemctl start mysql
systemctl enable mysql

# 设置 root 密码
mysql_secure_installation

# 验证
mysql -u root -p
```

### 4.4 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS forge_admin
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

::: warning 常见踩坑
- **字符集问题**：必须使用 utf8mb4，否则中文和 emoji 会出现乱码
- **认证插件**：MySQL 8.0 默认使用 `caching_sha2_password`，如连接报错可在配置中添加 `allowPublicKeyRetrieval=true`
- **时区问题**：连接串中必须设置 `serverTimezone=GMT%2B8`，否则时间可能不一致
:::

---

## 五、Redis 安装

### 5.1 Windows

Redis 官方不直接支持 Windows，推荐使用以下方式：

**方式一：WSL2（推荐）**

```powershell
# 安装 WSL2 后，在 WSL 中安装 Redis
wsl --install
# 进入 WSL 后：
sudo apt update && sudo apt install -y redis-server
sudo service redis-server start
```

**方式二：Memurai（Redis 兼容替代）**

下载地址：https://www.memurai.com/get-memurai

### 5.2 macOS

```bash
brew install redis
brew services start redis

# 验证
redis-cli ping
# 应返回: PONG
```

### 5.3 Linux

```bash
apt install -y redis-server
systemctl start redis-server
systemctl enable redis-server

# 验证
redis-cli ping
```

### 5.4 设置 Redis 密码（推荐）

```bash
# 方式一：命令行设置（重启后失效）
redis-cli
127.0.0.1:6379> CONFIG SET requirepass "your_redis_password"

# 方式二：修改配置文件（永久生效）
# 编辑 /etc/redis/redis.conf，取消注释并修改：
# requirepass your_redis_password
# 然后重启 Redis 服务
```

::: warning 常见踩坑
- **无密码连接**：Forge Admin 默认配置需要 Redis 密码，如 Redis 未设密码，需在 `application-dev.yml` 中将 password 留空
- **端口冲突**：如 6379 端口被占用，检查是否已有其他 Redis 实例运行
- **Windows WSL2 端口转发**：WSL2 中的 Redis 默认只能从 WSL 内部访问，如需从 Windows 访问需配置端口转发
:::

---

## 六、Node.js 与 pnpm 安装

### 6.1 Windows

1. 下载 Node.js LTS：https://nodejs.org/（选择 20.19+ LTS 版本）
2. 运行安装程序，勾选 **Add to PATH**
3. 验证：`node -v`
4. 安装 pnpm：
   ```powershell
   npm install -g pnpm@8
   pnpm -v
   ```

### 6.2 macOS

```bash
# 使用 nvm 安装（推荐）
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.0/install.sh | bash
source ~/.zshrc
nvm install 20
nvm use 20

# 安装 pnpm
npm install -g pnpm@8

# 验证
node -v
pnpm -v
```

### 6.3 Linux

```bash
# 使用 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.0/install.sh | bash
source ~/.bashrc
nvm install 20
nvm use 20

# 或直接安装
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs

# 安装 pnpm
npm install -g pnpm@8

# 验证
node -v
pnpm -v
```

### 6.4 配置国内镜像（推荐）

```bash
# npm 镜像
npm config set registry https://registry.npmmirror.com

# pnpm 镜像
pnpm config set registry https://registry.npmmirror.com
```

::: warning 常见踩坑
- **Node 版本过低**：Forge Admin 前端使用 Vite 7，要求 Node.js ≥ 20.19
- **pnpm 版本**：请使用 pnpm 8+，不要用 npm 或 yarn 安装依赖
- **node-sass 报错**：如遇到 node-sass 编译失败，确保 Node.js 版本与 node-sass 兼容，或改用 dart-sass
:::

---

## 七、Git 安装

### 7.1 各平台安装

| 平台 | 安装方式 |
|------|---------|
| Windows | 下载 https://git-scm.com/download/win |
| macOS | `brew install git` |
| Linux | `apt install -y git` |

### 7.2 配置 Git

```bash
# 配置用户信息（提交代码必须）
git config --global user.name "Your Name"
git config --global user.email "your@email.com"

# 配置默认分支名
git config --global init.defaultBranch main

# 配置中文文件名显示
git config --global core.quotepath false
```

---

## 八、IDE 推荐配置

### 8.1 后端开发（IntelliJ IDEA）

1. 下载 IDEA：https://www.jetbrains.com/idea/（Community 版免费）
2. 安装插件：
   - **Lombok** — 支持 Lombok 注解
   - **MyBatisX** — MyBatis-Plus 代码提示与跳转
   - **EnvFile** — 支持 .env 文件加载
3. 配置 JDK 17：`File → Project Structure → SDKs → Add JDK 17`
4. 导入项目：`File → Open → 选择 forge-admin 根目录`

### 8.2 前端开发（VS Code）

1. 下载 VS Code：https://code.visualstudio.com/
2. 安装插件：
   - **Vue - Official**（原 Volar）— Vue 3 语法支持
   - **UnoCSS** — 原子化 CSS 智能提示
   - **ESLint** — 代码规范检查
3. 打开 `forge-admin-ui` 目录

---

## 九、环境验证清单

完成安装后，逐项验证：

| 序号 | 验证命令 | 期望输出 | 状态 |
|------|---------|---------|------|
| 1 | `java -version` | 包含 `17` | ☐ |
| 2 | `mvn -version` | 包含 `3.8`+ | ☐ |
| 3 | `mysql -u root -p` | 成功连接 | ☐ |
| 4 | `redis-cli ping` | 返回 `PONG` | ☐ |
| 5 | `node -v` | 包含 `v20`+ | ☐ |
| 6 | `pnpm -v` | 包含 `8`+ | ☐ |
| 7 | `git --version` | 包含 `2.30`+ | ☐ |

全部通过后，进入 [快速入门](./quick-start.md) 开始体验！

---

## 十、常见问题汇总

### Q1: Maven 下载依赖很慢？

配置阿里云 Maven 镜像（见 3.3 节），下载速度可从几百 KB/s 提升到几十 MB/s。

### Q2: pnpm install 报权限错误？

```bash
# Linux/macOS
sudo chown -R $(whoami) ~/.pnpm-store
# 或配置 store 目录
pnpm config set store-dir /path/to/pnpm-store
```

### Q3: MySQL 8.0 连接报 Public Key Retrieval 错误？

在 JDBC 连接串中添加 `allowPublicKeyRetrieval=true`：

```
jdbc:mysql://localhost:3306/forge_admin?allowPublicKeyRetrieval=true&...
```

### Q4: Redis 启动后外部无法连接？

1. 检查 `redis.conf` 中 `bind` 配置，默认只允许 `127.0.0.1`
2. 如需外部访问，改为 `bind 0.0.0.0`（注意安全风险）
3. 确认防火墙放行了 6379 端口

### Q5: IDEA 编译报 Lombok 相关错误？

1. 确认已安装 Lombok 插件
2. 开启注解处理：`Settings → Build → Compiler → Annotation Processors → Enable`

### Q6: Windows 下 WSL2 的 Redis 无法从宿主机访问？

```powershell
# 在 WSL 中获取 IP
wsl hostname -I
# 在 Windows 中设置端口转发
netsh interface portproxy add v4tov4 listenport=6379 listenaddress=0.0.0.0 connectport=6379 connectaddress=<WSL_IP>
```
