# Lowcode Business Transaction Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Status:** completed on 2026-07-03. P0 backend/runtime work and P1 frontend configuration/detail quantity panels are implemented and verified.

**Goal:** Build the platform-level transaction closure capabilities needed for fully low-code procurement and warehouse workflows without hardcoding procurement-specific business objects.

**Architecture:** Extend the existing low-code action engine with a `FOREACH` step that executes existing whitelisted child steps against collection items. Enhance record selectors to support multi-field search and context-driven filters, then expose quantity ledger read models through guarded query APIs that can be consumed by low-code detail panels.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus XML mappers, Vue 3.5, Naive UI, Vite 7, pnpm.

---

## Files To Create Or Modify

- Create `ForeachActionStepExecutor.java`: executes collection-based child steps using existing step executors.
- Modify `BusinessActionExecutionContext.java`: stores scoped variables such as `item` and `index`.
- Modify `BusinessActionStepConfigHelper.java`: resolves `${item.xxx}` and context expressions consistently.
- Modify `BusinessActionExecutionService.java`: exposes child-step execution safely to `FOREACH` or extracts a reusable step runner.
- Modify `BusinessActionDesigner.vue`: adds a loop-step JSON template button.
- Modify `BusinessRecordSelectorService.java`: supports multi-field search and expression-filter normalization.
- Modify `AiRecordSelectorModal.vue`, `AiFormItem.vue`, `ChildTableEditor.vue`, `record-selector-utils.js`: resolves dynamic `searchParams` from runtime context before querying.
- Create `BusinessQuantityQueryDTO.java` and quantity query VO classes.
- Modify quantity mapper XML files: add explicit-field select queries.
- Create `BusinessQuantityQueryService.java` and `BusinessQuantityQueryController.java`.
- Modify `business-app.js`: add quantity query API wrappers.
- Create tests for `FOREACH`, selector search/filtering, and quantity query service.

## Task 1: Action Context Scoped Variables

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessActionExecutionContext.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessActionStepConfigHelper.java`

- [x] **Step 1: Add scoped variable storage**

Add this field and helper methods to `BusinessActionExecutionContext`:

```java
private Map<String, Object> scopedVariables = new LinkedHashMap<>();

public Object getScopedVariable(String key) {
    return scopedVariables == null ? null : scopedVariables.get(key);
}

public Map<String, Object> getScopedVariables() {
    return scopedVariables == null ? new LinkedHashMap<>() : scopedVariables;
}

public void setScopedVariables(Map<String, Object> scopedVariables) {
    this.scopedVariables = scopedVariables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(scopedVariables);
}
```

- [x] **Step 2: Resolve scoped paths**

In `BusinessActionStepConfigHelper.resolvePath`, before form/record fallback, resolve the first path segment from `context.getScopedVariables()`:

```java
String firstSegment = field.contains(".") ? field.substring(0, field.indexOf('.')) : field;
if (context.getScopedVariables().containsKey(firstSegment)) {
    Object scoped = context.getScopedVariables().get(firstSegment);
    String rest = field.equals(firstSegment) ? "" : field.substring(firstSegment.length() + 1);
    return StringUtils.isBlank(rest) ? scoped : readPathFromObject(scoped, rest);
}
```

Add `readPathFromObject(Object source, String path)` that supports `Map<?, ?>` values.

- [x] **Step 3: Compile**

Run:

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

Expected: compile succeeds.

## Task 2: FOREACH Action Step

**Files:**
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/ForeachActionStepExecutor.java`
- Modify: `BusinessActionExecutionService.java`
- Test: `BusinessActionForeachStepExecutorTest.java`

- [x] **Step 1: Extract step execution method**

Expose a package-private method on `BusinessActionExecutionService`:

```java
List<BusinessActionStepResultVO> executeNestedSteps(BusinessActionExecutionContext context, List<BusinessActionStepDTO> steps, int depth) {
    if (depth > 2) {
        throw new BusinessException("FOREACH 动作步骤最多嵌套 2 层");
    }
    return executeSteps(context, steps).stepResults();
}
```

- [x] **Step 2: Implement `ForeachActionStepExecutor`**

Create a component with `supportType()` returning `FOREACH`. It reads `collectionPath`, `itemAlias`, `indexAlias`, and `steps` from `stepConfig`; resolves the collection through `BusinessActionStepConfigHelper.resolvePath`; for every item it temporarily patches `context.scopedVariables`, calls `actionExecutionService.executeNestedSteps(context, childSteps, depth + 1)`, and restores the old scoped variables in `finally`.

- [x] **Step 3: Add tests**

Create tests covering empty collection, two successful rows, variable mapping, and failure rollback behavior using mock child executors.

- [x] **Step 4: Run targeted tests**

Run:

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Pdev,enable-tests -Dtest=BusinessActionForeachStepExecutorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: tests run and pass.

## Task 3: Selector Multi-Field Search And Dynamic Filters

**Files:**
- Modify: `BusinessRecordSelectorService.java`
- Modify: `AiRecordSelectorModal.vue`
- Modify: `AiFormItem.vue`
- Modify: `ChildTableEditor.vue`
- Modify: `record-selector-utils.js`
- Test: `BusinessRecordSelectorServiceTest.java`

- [x] **Step 1: Add selector filter normalization tests**

Add tests for keyword fields `code,name`, internal field rejection, and dynamic search param resolution.

- [x] **Step 2: Implement backend request normalization**

Change selector service so multi-field keywords are normalized into a structured search object consumed by dynamic CRUD or a selector-specific XML query. Continue rejecting system fields and `_` fields.

- [x] **Step 3: Implement frontend search param resolver**

Add `resolveSelectorSearchParams(searchParams, context)` to `record-selector-utils.js`. It should replace strings like `${formData.warehouseId}`, `${record.id}`, `${row.itemCode}`, `${query.projectId}` with values from the provided runtime context and omit null/empty values.

- [x] **Step 4: Wire runtime contexts**

In `AiFormItem.vue`, pass `{ formData, record: formData, query: route.query }`. In `ChildTableEditor.vue`, pass parent form data through existing `context` or a new prop if required.

- [x] **Step 5: Run tests and frontend build**

Run backend selector tests and `pnpm --dir forge-admin-ui build`.

## Task 4: Quantity Query API

**Files:**
- Create DTO/VO classes under `dto/businessapp` and `vo/businessapp`.
- Modify quantity mapper interfaces and XML files.
- Create `BusinessQuantityQueryService.java`
- Create `BusinessQuantityQueryController.java`
- Modify `business-app.js`
- Test `BusinessQuantityQueryServiceTest.java`

- [x] **Step 1: Add DTO and VO**

Create `BusinessQuantityQueryDTO` with `accountCode`, `itemCode`, `dimensionKey`, `sourceObjectCode`, `sourceRecordId`, `sourceDetailId`, `operationType`, and `lockStatus`.

- [x] **Step 2: Add XML queries**

Add explicit-field `selectBalancePage`, `selectLedgerPage`, and `selectLockPage` mapper queries with tenant and optional filters.

- [x] **Step 3: Add Service and Controller**

Expose `POST /ai/business/quantity/query/balance`, `/ledger`, and `/lock` with `@SaCheckPermission("ai:businessQuantity:view")`.

- [x] **Step 4: Add permission migration**

Create a new Flyway script with `NOT EXISTS` inserts for `ai:businessQuantity:view`.

- [x] **Step 5: Run targeted tests**

Run `BusinessQuantityQueryServiceTest`.

## Task 5: SDD Verification

**Files:**
- Modify: `code-copilot/changes/lowcode-business-transaction-closure/execution-log.md`
- Modify: `tasks.md`, `test-spec.md`, `spec.md`

- [x] **Step 1: Read automated testing standard**

Run:

```bash
cat code-copilot/rules/automated-testing-standard.md
```

- [x] **Step 2: Run compile, tests, build, static checks**

Use the commands in `test-spec.md`.

- [x] **Step 3: Update logs**

Append commands, results, warnings, and skipped items to `execution-log.md`. Update task status and spec execution table.

## Self-Review

- Spec coverage: The implementation covers action loops, selector enhancements, quantity query API, frontend selector/action/detail configuration entries, and verification.
- Placeholder scan: No task uses TBD or unspecified file names.
- Type consistency: Action step names use `FOREACH`, quantity action stays `QUANTITY`, permissions use `ai:businessQuantity:view`.
