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

## preset

| preset | 说明 |
| --- | --- |
| `minimal-admin` | 最小后台：系统权限、租户、文件、基础认证和管理端前端 |
| `data-app` | 数据应用：`minimal-admin` + 数据资产/数据集模块 |
| `ai-report` | AI 数据大屏：`data-app` + AI、外部接口代理、报表服务和报表前端 |
| `full` | 全量能力：保留当前 Forge 后端所有子模块和两个前端工程 |

## 当前边界

1. 后端 Maven 子模块、starter、plugin 会按 preset 裁剪。
2. `forge-admin-ui` 第一版整体复制，不做菜单和页面级裁剪。
3. 数据库脚本第一版整体复制，后续应继续按 module catalog 拆分 migration/seed。
4. Java 包名会从 `com.mdframe.forge` 移动并替换为 `--base-package`。
5. Maven `groupId` 会替换为 `--group-id`，artifactId 会按 `--artifact-prefix` 重命名。

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
