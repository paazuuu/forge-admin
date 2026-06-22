# Tenant Business Datasource Routing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `forge-business` tenant business datasource routing work through baomidou dynamic-datasource so MyBatis-Plus Mapper/XML queries hit the current tenant's configured business database.

**Architecture:** Keep lowcode dynamic CRUD on its existing runtime JDBC-template context. Replace `forge-business`'s main routing path with a dynamic-datasource adapter: resolve the current tenant to a configured dynamic-datasource `dsKey`, validate that the key exists in `DynamicRoutingDataSource`, push/pop the dsKey around annotated business service methods, and provide an executor/task decorator for async or no-login contexts.

**Implementation boundary after review:** Generic tenant business datasource contracts and context helpers live in `forge-starter-tenant`; sys_tenant/sys_config-backed resolution lives in `forge-plugin-system`. `forge-business-core` only contains a small demo endpoint for manual verification. Business tenant datasource routing does not listen to or dynamically register lowcode `gen_datasource` entries.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, baomidou dynamic-datasource 4.3.1, Forge `sys_tenant`/`gen_datasource`/`sys_config`.

---

### Task 1: Datasource Routing Core

**Files:**
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-tenant/src/main/java/com/mdframe/forge/starter/tenant/datasource/TenantBusinessDataSource.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-tenant/src/main/java/com/mdframe/forge/starter/tenant/datasource/BusinessDataSourceProperties.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-tenant/src/main/java/com/mdframe/forge/starter/tenant/datasource/TenantBusinessDataSourceInfo.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-tenant/src/main/java/com/mdframe/forge/starter/tenant/datasource/TenantBusinessDataSourceResolver.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/datasource/SysTenantBusinessDataSourceResolver.java`

- [x] **Step 1: Define the annotation and properties**

Create `@TenantBusinessDataSource` for service method/class usage and `BusinessDataSourceProperties` with `enabled=false` as the configuration default.

- [x] **Step 2: Change resolver output**

Make the resolver return `TenantBusinessDataSourceInfo` with `dsKey`, datasource metadata, and master fallback instead of lowcode `LowcodeRuntimeDataSourceContext`.

### Task 2: Dynamic-Datasource Key Validation

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/datasource/SysTenantBusinessDataSourceResolver.java`

- [x] **Step 1: Validate configured dsKey**

Read `sys_tenant.default_business_datasource_code` as the business datasource dsKey and verify it exists in `DynamicRoutingDataSource#getDataSources()`.

- [x] **Step 2: Keep lowcode datasource separate**

Do not listen to `gen_datasource` changes and do not dynamically register lowcode datasource rows for business tenant routing.

### Task 3: AOP And Executor

**Files:**
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-tenant/src/main/java/com/mdframe/forge/starter/tenant/datasource/TenantBusinessDataSourceAspect.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-tenant/src/main/java/com/mdframe/forge/starter/tenant/datasource/TenantBusinessDataSourceExecutor.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-tenant/src/main/java/com/mdframe/forge/starter/tenant/datasource/TenantBusinessDataSourceTaskDecorator.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-tenant/src/main/java/com/mdframe/forge/starter/tenant/datasource/TenantBusinessDataSourceTaskDecoratorPostProcessor.java`

- [x] **Step 1: Add AOP route**

Before annotated methods, resolve the current tenant datasource and push it with `DynamicDataSourceContextHolder.push(dsKey)`; always call `poll()` in finally.

- [x] **Step 2: Add explicit tenant executor**

Support `execute(Long tenantId, Supplier<T>)` for scheduled jobs and message consumers without login context.

- [x] **Step 3: Add task decorator**

Capture tenant id and current dynamic datasource key when tasks are submitted, restore them inside async execution, and clear them after execution.

### Task 4: Verification And Docs

**Files:**
- Modify: `code-copilot/changes/lowcode-runtime-datasource-isolation/tasks.md`
- Modify: `code-copilot/changes/lowcode-runtime-datasource-isolation/test-spec.md`
- Modify: `code-copilot/changes/lowcode-runtime-datasource-isolation/execution-log.md`

- [x] **Step 1: Update task status**

Mark implemented core routing tasks complete and leave integration tests pending if no external test databases are available.

- [x] **Step 2: Run verification**

Run `git diff --check` and `cd forge-server && JAVA_HOME=... mvn -pl forge-admin-server -am compile -DskipTests`.

### Task 5: Business Demo Endpoint

**Files:**
- Create: `forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/demo/**`
- Create: `forge-server/forge-business/forge-business-core/src/main/resources/mapper/business/BusinessDatasourceDemoMapper.xml`
- Modify: `forge-server/forge-admin-server/pom.xml`

- [x] **Step 1: Add a mapper-backed demo**

Expose `/business/datasource-demo/current`, `/business/datasource-demo/prepare`, and `/business/datasource-demo/list`. The demo uses MyBatis Mapper/XML, creates `business_datasource_demo` in the routed business database, inserts a record, and returns the current database name plus dynamic-datasource key.

- [x] **Step 2: Include demo in admin runtime**

Add `forge-business-core` to `forge-admin-server` so the demo controller is available when the admin server is started.
