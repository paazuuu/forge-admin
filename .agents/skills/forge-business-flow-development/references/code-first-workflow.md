# Code-First Workflow

Use this path for complex business modules that need custom Java service logic, custom business tables, custom forms, or special status repair. The sample is `sample_purchase_order`.

## Sequence

1. Create the business contract.
   - Object code: lower snake case, for example `contract_payment`.
   - Model key: `<object_code>_approval`.
   - Business key: `<objectCode>:<recordId>`.
   - Status values: `DRAFT`, `IN_PROCESS`, `NEED_MODIFY`, `APPROVED`, `REJECTED`, `CANCELED`.

2. Add the table and dictionary migration.
   - Include `business_key`, `process_instance_id`, approver fields, node remark fields, and reject/cancel reason fields.
   - Include standard audit and tenant fields.
   - Add unique key on `(tenant_id, order_no/code)` and `(tenant_id, business_key)`.
   - Add status dictionary with `sys_dict_type` and `sys_dict_data`.

3. Add backend module files.
   - Entity extends `TenantEntity`.
   - Mapper extends `BaseMapper<Entity>`.
   - Complex queries go in Mapper XML with explicit columns and tenant predicates.
   - Service owns create/update/delete/submit/task-save/status-callback logic.
   - Controller uses `RespInfo`, `@ApiDecrypt`, `@ApiEncrypt`, and operation logs when the page uses encrypted request flow.

4. Add a `FlowDefinition` support class.
   - Keep constants for object code, model key, provider key, form key, node keys, statuses, variables, action names, and field codes.
   - Expose `formAssets(objectCode)`, `fields(recordId)`, `buildTaskFormContext(...)`, `toTaskSaveDTO(...)`, `recordData(...)`, `businessKey(id)`, and `flowModelPayload(...)`.
   - Keep node field permission defaults here only as seed/defaults. Designer-edited BPMN is the source of truth after deployment.

5. Add a `BusinessCodeFormProvider`.
   - Implement `providerKey()`, `providerName()`, `formAssets(objectCode)`, `buildContext(...)`, and `saveContext(...)`.
   - Use `BusinessTaskFormContextVO` for task/done/history forms.
   - In `saveContext`, convert `BusinessTaskFormSaveDTO` to a business DTO and call a business Service method that validates status and node key.
   - Implement `buildSummaries(...)` with batch query to avoid N+1 lookup in todo/done lists.

6. Start the flow from the business Service.
   - Validate only `DRAFT` records can start.
   - Build variables needed by BPMN assignment, conditions, title, and task forms.
   - Call `flowClient.startProcess(modelKey, businessKey, businessType, title, variables, userId, userName, deptId, deptName)`.
   - Save `businessKey`, `processInstanceId`, approver selections, and set status `IN_PROCESS` in the same transaction after successful start.

7. Register callbacks.
   - Annotate the Service with `@FlowBind(modelKey = MODEL_KEY, businessType = BUSINESS_TYPE)`.
   - Add `@FlowCallback(on = { ON_TASK_CREATED, ON_TASK_COMPLETED, ON_COMPLETED, ON_REJECTED, ON_CANCELED })`.
   - Use callbacks to move status and copy finish variables, but make every transition idempotent.
   - Add active-task reconciliation in page/detail queries for known running statuses.

8. Seed App Center and flow binding.
   - Insert/update `ai_business_suite`, `ai_business_object`, `ai_business_app`.
   - Insert `ai_business_binding` with `binding_type='FLOW'`, `flowModelKey`, `titleTemplate`, `businessBinding`, and `variableMapping`.
   - Keep `nodeForms` only as compatibility fallback; new node form config belongs in BPMN node attributes.

9. Add frontend only if needed.
   - Code-first pages can be custom Vue pages when business UX is complex.
   - Use `DictTag` / `useDict` for status.
   - Keep Long IDs as strings when reading row IDs or flow variables.
   - If the page is used as an external task form, read task form context and obey node field permissions.

## Status Gate Defaults

- Create: status `DRAFT`.
- Edit: allow `DRAFT` and `NEED_MODIFY`.
- Delete: allow `DRAFT`, `REJECTED`, `CANCELED`.
- Submit: allow `DRAFT` only.
- Applicant modify task save: require `NEED_MODIFY`, but repair from `IN_PROCESS` when the active task node is applicant modify.
- Approval task save: require `IN_PROCESS`, but repair from `NEED_MODIFY` when the active task node is an approval node.

## File Pattern

Use this layout under `forge-server/forge-business/forge-business-core/src/main/java/.../<domain>/`:

```text
controller/<Business>Controller.java
domain/<Business>.java
dto/<Business>DTO.java
dto/<Business>Query.java
dto/<Business>SubmitDTO.java
dto/<Business>TaskSaveDTO.java
mapper/<Business>Mapper.java
provider/<Business>CodeFormProvider.java
service/<Business>Service.java
service/impl/<Business>ServiceImpl.java
support/<Business>FlowDefinition.java
support/<Business>FlowBpmn.java
vo/<Business>VO.java
vo/<Business>FlowInitVO.java
```

Mapper XML goes under:

```text
forge-server/forge-business/forge-business-core/src/main/resources/mapper/business/<Business>Mapper.xml
```
