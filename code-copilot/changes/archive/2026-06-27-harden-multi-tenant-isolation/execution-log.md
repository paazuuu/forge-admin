# Execution Log

## 2026-06-04 10:52:42 CST

变更范围：

- 多租户忽略表默认配置。
- V1.0.56 租户隔离迁移脚本。
- 流程、文件、公告关系实体和 Mapper XML。
- `ai_page_template` 平台全局模板维护权限。

执行结果：

- `git diff --check`：通过。
- 只读结构查询：开发库 `ai_` 表除 `ai_page_template` 外均已有 `tenant_id`；`sys_` 缺口与 V1.0.56 覆盖范围一致。
- 只读历史数据查询：发现 `sys_dict_data`、`sys_dict_type`、`sys_flow_form`、`sys_login_log`、`sys_notice`、`sys_operation_log`、`sys_role`、`sys_role_resource`、`sys_user_social` 存在 `tenant_id IS NULL OR 0`，已在迁移中回填到 `1`。
- 只读唯一索引查询：`sys_flow_model.model_key`、`sys_flow_business.uk_business_key`、`sys_flow_category.category_code`、`sys_flow_form.form_key`、`sys_flow_template.template_key`、`sys_flow_statistics.uk_process_date` 与迁移脚本匹配。
- `xmllint --noout --nonet`：`SysFileGroupMapper.xml`、`SysNoticeReadRecordMapper.xml`、`SysUserMapper.xml`、`SysRoleMapper.xml`、`SysUserTenantMapper.xml` 均通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests`：通过，`BUILD SUCCESS`。

警告和跳过项：

- 本机 `127.0.0.1:3407` MySQL 未启动，`127.0.0.1:3306` root 无密码访问被拒绝；未在本轮实跑 Flyway 迁移。
- Maven 编译存在既有 Lombok Builder 默认值和过时 API 警告，不阻断本轮变更。
- 本轮未启动后端或前端服务，无需清理 PID。

## 2026-06-04 11:12:45 CST

变更范围：

- 平台全局配置入口权限收口。
- `SessionHelper` 新增超级管理员断言。
- 定时任务、任务日志、API配置、系统配置分组、配置刷新、Excel配置、文件存储配置、客户端配置、缓存、监控等管理入口增加超级管理员兜底。
- 行政区划保留业务读取接口，新增/修改/删除/刷新缓存限制超级管理员。
- 文件存储默认配置接口改为只返回上传组件需要的非敏感字段，新增 `/system/storage/config/options` 供文件列表选择存储配置。
- `/system/client/list` 保留给菜单和角色页面使用，但仅返回客户端下拉需要的字段。

执行结果：

- `git diff --check`：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests`：通过，`BUILD SUCCESS`。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：通过，`built in 55.10s`。

警告和跳过项：

- 前端构建存在既有 UnoCSS 图标加载失败、CSS `//` 注释、动态/静态混用 chunk 提示，不阻断本轮变更。
- Maven 编译存在既有 Lombok Builder 默认值、过时 API 和 unchecked 警告，不阻断本轮变更。
- 本轮未启动后端服务，未用真实 Token 验证超级管理员/租户管理员接口返回差异；留到接口联调阶段执行。
- 本机可丢弃 MySQL 沙箱仍不可用，Flyway 迁移实跑继续保留为待办。
- 本轮没有启动常驻服务，无需清理 PID。

## 2026-06-04 11:50:29 CST

变更范围：

- 修复 `V1.0.56__harden_tenant_isolation_boundaries.sql` 在开发库执行失败的问题。
- 失败根因：历史字典数据同时存在 tenant 0 和 tenant 1 的同名字典类型，`UPDATE sys_dict_type SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0` 触发唯一键 `sys_dict_type.uk_tenant_dict_type`，具体冲突为 `sys_notice_status`、`sys_notice_type`。
- 迁移脚本新增前置去重：先删除 tenant 0/null 中已经在 tenant 1 存在的重复 `sys_dict_type` 和 `sys_dict_data`，再归一化到默认租户 1。

执行结果：

- 开发库 Flyway 失败记录已在本轮前置修复中删除：`forge_schema_history` 当前 1.0.55 为最新成功版本，未保留失败的 1.0.56 行。
- 启动命令误跑 Maven 根 POM 一次，失败原因为根 POM 无 `mainClass`，未进入应用启动也未触发 Flyway。
- 正确启动命令：在 `forge/forge-admin-server` 执行 `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn org.springframework.boot:spring-boot-maven-plugin:3.5.13:run -Dspring-boot.run.profiles=dev -DskipTests`。
- Flyway 实跑结果：`Successfully applied 1 migration to schema forge_admin_new, now at version v1.0.56`，应用随后成功启动到 `Started ForgeAdminApplication`。
- 迁移后 `forge_schema_history`：`version=1.0.56`、`description=harden tenant isolation boundaries`、`success=1`、`execution_time=16827`。
- 报错字典核验：`sys_notice_status` 仅保留 `dict_id=44, tenant_id=1`；`sys_notice_type` 仅保留 `dict_id=43, tenant_id=1`。
- 目标列核验：`sys_file_group`、`sys_file_metadata`、`sys_notice_org`、`sys_notice_read_record`、`sys_flow_model`、`sys_flow_business`、`sys_flow_category`、`sys_flow_cc`、`sys_flow_comment`、`sys_flow_condition_item`、`sys_flow_condition_rule`、`sys_flow_error_log`、`sys_flow_node_config`、`sys_flow_approval_level`、`sys_flow_statistics`、`sys_flow_task`、`sys_flow_template` 均已具备 `tenant_id`。
- 目标索引核验：文件、公告关系、流程查询索引和流程租户内唯一索引已落库，包括 `uk_flow_model_tenant_key`、`uk_flow_business_tenant_key`、`uk_flow_category_tenant_code`、`uk_flow_form_tenant_key`、`uk_flow_template_tenant_key`、`uk_flow_statistics_tenant_date`。
- 核心旧值核验：`sys_user`、`sys_role`、`sys_role_resource`、`sys_user_role`、`sys_user_org`、`sys_dict_type`、`sys_dict_data`、`sys_notice`、`sys_file_group`、`sys_file_metadata`、`sys_notice_read_record`、`sys_flow_model`、`sys_flow_business`、`sys_flow_category`、`sys_flow_form`、`sys_flow_template` 的 `tenant_id IS NULL OR tenant_id = 0` 均为 0；`sys_notice_org` 当前为空表。
- `git diff --check`：通过。

警告和跳过项：

- `sys_flow_node_operation` 在当前开发库不存在，迁移脚本按 `information_schema` 防护分支跳过。
- 后端启动期间出现既有警告：Flyway 部分相对 migration 路径不存在、MacOS Netty DNS native 缺失、Commons Logging 冲突提示、启动期无租户上下文导致部分租户查询追加 `tenant_id = NULL`、未知配置分组 `security`；均未阻断启动或迁移。
- 后端启动后 Quartz 示例任务 `DEFAULT.simpleTask` 执行并写入少量 `sys_job_log`；未做清理，避免额外破坏性数据操作。
- 本轮启动的后端进程 PID 73653 已通过 Ctrl-C 正常停止，日志显示 Quartz、Hikari、DynamicRoutingDataSource 均完成 shutdown；`jps -l` 仅显示自身进程，无后端 Java 进程残留。
