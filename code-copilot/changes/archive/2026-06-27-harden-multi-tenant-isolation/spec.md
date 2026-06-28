# Harden Multi-Tenant Isolation
> status: done

## 背景

当前租户隔离存在几类风险：

- 部分 `sys_` 业务表缺少 `tenant_id` 或被误放入租户忽略表，导致租户数据无法被 SQL 拦截器稳定隔离。
- 流程、文件、公告关系等业务数据应随租户隔离，但旧表结构没有统一补齐。
- `sys_resource`、页面模板、区域码等平台定义数据不适合按租户复制，需要通过角色授权或未来租户资源包控制可见性。
- 数据权限在用户没有组织或范围为空时必须失败关闭，不能退化成全量查询。

## 设计原则

1. 租户业务数据表必须包含 `tenant_id`，并从 `ignoreTables` 中移除。
2. 平台定义表保持全局，不按当前租户 SQL 隔离，但管理端操作需要服务端权限兜底。
3. `sys_resource` 作为全局菜单/权限定义，租户内实际可见菜单由 `sys_role_resource.tenant_id` 控制。
4. 历史 `tenant_id IS NULL` 或 `tenant_id = 0` 数据统一回填到默认租户 `1`。
5. 无组织、无自定义范围、无行政区划等数据权限空范围必须拼接恒假条件。

## 本阶段范围

- 收敛租户忽略表默认配置。
- 为流程、文件、公告关系表补齐 `tenant_id` 迁移。
- 补齐对应 Java 实体字段。
- 强化关键联表查询的同租户关联。
- 将全局 AI 页面模板显式列为平台定义表，并限制模板维护只能由超级管理员执行。

## 后续阶段

- 继续审查 `sys_job_*`、`sys_excel_*`、`sys_file_storage_config` 等平台配置表的管理权限。
- 设计可选的 `sys_tenant_resource` 或租户资源包表，用于控制租户启用模块，而不是复制 `sys_resource`。
- 增加租户隔离回归用例：普通租户、租户管理员、超级管理员、无组织用户、多租户用户切换。

## 归档记录（HARD-GATE）
- **状态**：done
- **归档时间**：2026-06-27
- **归档人**：yaomd（批量归档）
- **归档路径**：code-copilot/changes/archive/2026-06-27-harden-multi-tenant-isolation/
- **判定依据**：任务清单全部完成，execution-log 验证通过（编译/构建/lint 闭环）。
