# Forge 项目装配器第一版

第一版用于从当前 Forge 仓库生成一个客户项目源码，支持项目名、包名、Maven 坐标和部分模块裁剪。

## 命令

```bash
pnpm forge:create -- ../smart-factory \
  --preset ai-report \
  --project-name smart-factory \
  --display-name 智慧工厂管理平台 \
  --base-package com.company.smartfactory \
  --group-id com.company.smartfactory \
  --database-name smart_factory
```

如果目标目录非空，加 `--force` 覆盖。
需要在某个 preset 基础上额外带模块时，用 `--include`，例如：

```bash
pnpm forge:create -- ../smart-factory \
  --preset minimal-admin \
  --include business-core \
  --project-name smart-factory \
  --display-name 智慧工厂管理平台 \
  --base-package com.company.smartfactory \
  --group-id com.company.smartfactory \
  --database-name smart_factory
```

如果客户项目名较长，可以让父级工程保持完整名称、子模块使用短前缀：

```bash
pnpm forge:create -- ../nmg-lt-psi-system \
  --preset minimal-admin \
  --project-name nmg-lt-psi-system \
  --display-name 内蒙古商品进销存管理系统 \
  --base-package com.asiainfo.nm.psi \
  --group-id com.asiainfo.nm.psi \
  --database-name psi_admin \
  --strip-module-prefix nmg-lt
```

上面命令会保留 `nmg-lt-psi-system-server` 作为后端根工程目录和父级 artifact，子模块 artifact 会使用 `psi-system-admin-server`、`psi-system-framework` 这种短名称。也可以直接用 `--module-artifact-prefix psi-system` 指定子模块前缀。

## preset

| preset | 说明 |
| --- | --- |
| `minimal-admin` | 最小后台：系统权限、租户、文件、基础认证、代码生成和管理端前端 |
| `data-app` | 数据应用：`minimal-admin` + 数据资产/数据集模块 |
| `ai-report` | AI 数据大屏：`data-app` + AI、外部接口代理、报表服务和报表前端 |
| `full` | 全量能力：保留当前 Forge 后端所有子模块和两个前端工程 |

## 当前边界

1. 后端 Maven 子模块、starter、plugin 会按 preset 裁剪。
2. `forge-admin-ui` 第一版整体复制，不做菜单和页面级裁剪。
3. 生成结果会在后端根工程输出 `db/manifest.json` 和 `db/module/*.sql`，按本次选择的模块收集模块 SQL 资源。
4. Java 包名会从 `com.mdframe.forge` 移动并替换为 `--base-package`。
5. Maven `groupId` 会替换为 `--group-id`，artifactId 会按 `--artifact-prefix` 重命名。
6. 通过 `--include business-core` 带业务模块时，生成器会同时给 admin server 增加业务模块依赖。
7. 数据库初始化统一使用后端根工程的 `scripts/db/init-db.sh`；独立 npx CLI 后续也调用这个脚本执行初始化。

## 数据库脚本

生成工程根目录会包含：

| 路径 | 说明 |
| --- | --- |
| `db/manifest.json` | 本次选中模块对应 SQL 文件清单 |
| `db/module/*.sql` | 已完成项目名、包名、数据库名替换后的模块 SQL 文件 |
| `scripts/db/init-db.sh` | 创建数据库并执行初始化 SQL 的统一脚本 |

执行示例：

```bash
cd smart-factory-server
bash scripts/db/init-db.sh \
  --host 127.0.0.1 \
  --port 3306 \
  --database smart_factory \
  --user root \
  --password your_password
```

如需额外执行 `db/module/*.sql`，追加 `--with-module`。当前历史大脚本 `forge-admin-server/sql/初始化脚本.sql` 还没有拆成 core/module 两层，默认初始化仍会先执行该大脚本，避免基础表缺失。

## 生成后验证

```bash
cd ../smart-factory/smart-factory-server
mvn -pl smart-factory-admin-server -am compile -DskipTests
```

```bash
cd ../smart-factory/smart-factory-admin-ui
pnpm install
pnpm build
```
