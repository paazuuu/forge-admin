-- Procurement/Warehouse low-code document flow and quantity actions.
-- All behavior is configured through generic business document, flow binding and action protocols.

SET @pw_purchase_object_id := (
  SELECT id
  FROM ai_business_object
  WHERE tenant_id = 1
    AND suite_code = 'PROCUREMENT_WAREHOUSE'
    AND object_code = 'PW_PURCHASE_ORDER'
  LIMIT 1
);

SET @pw_outbound_object_id := (
  SELECT id
  FROM ai_business_object
  WHERE tenant_id = 1
    AND suite_code = 'PROCUREMENT_WAREHOUSE'
    AND object_code = 'PW_OUTBOUND_ORDER'
  LIMIT 1
);

SET @pw_transfer_object_id := (
  SELECT id
  FROM ai_business_object
  WHERE tenant_id = 1
    AND suite_code = 'PROCUREMENT_WAREHOUSE'
    AND object_code = 'PW_TRANSFER_ORDER'
  LIMIT 1
);

INSERT INTO ai_business_document_config (id, tenant_id, object_id, suite_code, object_code, config_key,
                                         document_name, document_no_rule, document_enabled, status_field,
                                         starter_field, owner_field, default_flow_key, status_mapping, options,
                                         create_by, create_time, create_dept, update_by, update_time)
SELECT seed.id, 1, seed.object_id, 'PROCUREMENT_WAREHOUSE', seed.object_code, seed.config_key,
       seed.document_name, seed.document_no_rule, 1, 'orderStatus',
       'createBy', 'createBy', 'leave_multi',
       JSON_OBJECT(
         'DRAFT', 'DRAFT',
         'SUBMITTED', 'SUBMITTED',
         'IN_PROCESS', 'IN_PROCESS',
         'APPROVED', 'APPROVED',
         'REJECTED', 'REJECTED',
         'CANCELED', 'CANCELED',
         'CLOSED', 'CLOSED'
       ),
       JSON_OBJECT(
         'lowcodeApp', true,
         'scenario', 'procurement_warehouse',
         'quantityManaged', seed.quantity_managed,
         'submitActionCode', seed.submit_action_code
       ),
       1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000009101 AS id, @pw_purchase_object_id AS object_id,
         'PW_PURCHASE_ORDER' AS object_code, 'pw_purchase_order' AS config_key,
         '采购单据' AS document_name, 'PO-{yyyyMMdd}-{seq4}' AS document_no_rule,
         true AS quantity_managed, 'submit_purchase_approval' AS submit_action_code
  UNION ALL
  SELECT 1910000000000009102, @pw_outbound_object_id,
         'PW_OUTBOUND_ORDER', 'pw_outbound_order',
         '出库单据', 'OUT-{yyyyMMdd}-{seq4}',
         true, 'submit_outbound_approval'
  UNION ALL
  SELECT 1910000000000009103, @pw_transfer_object_id,
         'PW_TRANSFER_ORDER', 'pw_transfer_order',
         '调拨单据', 'TRF-{yyyyMMdd}-{seq4}',
         true, 'submit_transfer_approval'
) seed
WHERE seed.object_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM ai_business_document_config c
    WHERE c.tenant_id = 1
      AND c.object_code = seed.object_code
  );

INSERT INTO ai_business_binding (id, tenant_id, target_type, target_id, target_code, binding_type, binding_key,
                                 binding_name, binding_config, description, status, sort_order, create_by,
                                 create_time, create_dept, update_by, update_time)
SELECT seed.id, 1, 'OBJECT', seed.object_id, seed.object_code, 'FLOW', 'leave_multi',
       seed.binding_name,
       JSON_OBJECT(
         'flowModelKey', 'leave_multi',
         'flowModelName', '采购仓储通用审批流程',
         'titleTemplate', seed.title_template,
         'startMode', 'ACTION_ONLY',
         'variableMapping', seed.variable_mapping,
         'conditionFlows', JSON_ARRAY(),
         'options', JSON_OBJECT(
           'lowcodeApp', true,
           'scenario', 'procurement_warehouse',
           'businessKeyPattern', CONCAT(seed.object_code, ':{recordId}'),
           'callbackActions', seed.callback_actions
         )
       ),
       seed.description, 1, seed.sort_order, 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000009104 AS id, @pw_purchase_object_id AS object_id,
         'PW_PURCHASE_ORDER' AS object_code,
         '采购单审批流程' AS binding_name,
         '采购单审批' AS title_template,
         JSON_ARRAY(
           JSON_OBJECT('formField', 'purchaseNo', 'flowVariable', 'documentNo', 'label', '采购单号'),
           JSON_OBJECT('formField', 'projectName', 'flowVariable', 'projectName', 'label', '项目名称'),
           JSON_OBJECT('formField', 'createBy', 'flowVariable', 'applicantId', 'label', '申请人ID'),
           JSON_OBJECT('formField', 'orderStatus', 'flowVariable', 'documentStatus', 'label', '单据状态'),
           JSON_OBJECT('formField', 'purchaseAmountCent', 'flowVariable', 'amountCent', 'label', '采购金额分')
         ) AS variable_mapping,
         JSON_OBJECT('APPROVED', 'inbound_purchase_stock', 'approvedActionCode', 'inbound_purchase_stock') AS callback_actions,
         '采购单提交后进入通用审批，通过回调执行采购入库数量动作' AS description,
         1 AS sort_order
  UNION ALL
  SELECT 1910000000000009105, @pw_outbound_object_id,
         'PW_OUTBOUND_ORDER',
         '出库单审批流程',
         '出库单审批',
         JSON_ARRAY(
           JSON_OBJECT('formField', 'outboundNo', 'flowVariable', 'documentNo', 'label', '出库单号'),
           JSON_OBJECT('formField', 'warehouseName', 'flowVariable', 'warehouseName', 'label', '所属仓库'),
           JSON_OBJECT('formField', 'createBy', 'flowVariable', 'applicantId', 'label', '申请人ID'),
           JSON_OBJECT('formField', 'orderStatus', 'flowVariable', 'documentStatus', 'label', '单据状态')
         ),
         JSON_OBJECT('APPROVED', 'commit_outbound_stock', 'approvedActionCode', 'commit_outbound_stock', 'REJECTED', 'release_outbound_stock', 'rejectedActionCode', 'release_outbound_stock'),
         '出库单提交动作先锁定库存，通过回调扣减，驳回回调释放锁定',
         2
  UNION ALL
  SELECT 1910000000000009106, @pw_transfer_object_id,
         'PW_TRANSFER_ORDER',
         '调拨单审批流程',
         '调拨单审批',
         JSON_ARRAY(
           JSON_OBJECT('formField', 'transferNo', 'flowVariable', 'documentNo', 'label', '调拨单号'),
           JSON_OBJECT('formField', 'fromWarehouseName', 'flowVariable', 'fromWarehouseName', 'label', '调出仓库'),
           JSON_OBJECT('formField', 'toWarehouseName', 'flowVariable', 'toWarehouseName', 'label', '调入仓库'),
           JSON_OBJECT('formField', 'createBy', 'flowVariable', 'applicantId', 'label', '申请人ID'),
           JSON_OBJECT('formField', 'orderStatus', 'flowVariable', 'documentStatus', 'label', '单据状态')
         ),
         JSON_OBJECT('APPROVED', 'transfer_stock', 'approvedActionCode', 'transfer_stock'),
         '调拨单通过回调执行通用数量转移动作',
         3
) seed
WHERE seed.object_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM ai_business_binding b
    WHERE b.tenant_id = 1
      AND b.target_type = 'OBJECT'
      AND b.target_code = seed.object_code
      AND b.binding_type = 'FLOW'
      AND b.binding_key = 'leave_multi'
  );

UPDATE ai_business_object o
JOIN (
  SELECT 'PW_PURCHASE_ORDER' AS object_code,
         JSON_ARRAY(
           JSON_OBJECT(
             'actionCode', 'submit_purchase_approval',
             'actionName', '提交审批',
             'actionPosition', 'ROW',
             'actionType', 'COMMAND',
             'permission', 'ai:businessAction:execute',
             'confirmRequired', true,
             'successMessage', '采购审批已提交',
             'failureMessage', '采购审批提交失败',
             'status', 1,
             'sortOrder', 10,
             'actionConfig', JSON_OBJECT(
               'successBehavior', 'refreshList',
               'steps', JSON_ARRAY(
                 JSON_OBJECT(
                   'stepCode', 'start_purchase_flow',
                   'stepName', '发起采购审批',
                   'stepType', 'START_FLOW',
                   'rollbackOnFailure', true,
                   'stepConfig', JSON_OBJECT(
                     'flowModelKey', 'leave_multi',
                     'title', '采购单审批',
                     'fieldMapping', JSON_ARRAY(
                       JSON_OBJECT('sourceField', 'record.main.id', 'targetField', 'documentId'),
                       JSON_OBJECT('sourceField', 'record.main.purchaseNo', 'targetField', 'documentNo'),
                       JSON_OBJECT('sourceField', 'record.main.projectName', 'targetField', 'projectName'),
                       JSON_OBJECT('sourceField', 'record.main.purchaseAmountCent', 'targetField', 'amountCent'),
                       JSON_OBJECT('sourceField', 'record.main.createBy', 'targetField', 'applicantId'),
                       JSON_OBJECT('sourceField', 'record.main.orderStatus', 'targetField', 'documentStatus')
                     ),
                     'staticValues', JSON_OBJECT('deptManager', '1', 'hr', 'admin', 'approvalScene', 'PURCHASE')
                   )
                 )
               )
             )
           ),
           JSON_OBJECT(
             'actionCode', 'inbound_purchase_stock',
             'actionName', '采购入库过账',
             'actionPosition', 'DETAIL',
             'actionType', 'COMMAND',
             'permission', 'ai:businessAction:execute',
             'confirmRequired', true,
             'successMessage', '采购入库已写入数量台账',
             'failureMessage', '采购入库过账失败',
             'status', 1,
             'sortOrder', 90,
             'actionConfig', JSON_OBJECT(
               'successBehavior', 'refreshList',
               'steps', JSON_ARRAY(
                 JSON_OBJECT(
                   'stepCode', 'foreach_purchase_items',
                   'stepName', '逐行入库',
                   'stepType', 'FOREACH',
                   'rollbackOnFailure', true,
                   'stepConfig', JSON_OBJECT(
                     'collectionPath', 'record.children.pw_purchase_order_item',
                     'itemAlias', 'item',
                     'indexAlias', 'index',
                     'steps', JSON_ARRAY(
                       JSON_OBJECT(
                         'stepCode', 'purchase_item_inbound',
                         'stepName', '明细入库',
                         'stepType', 'DOMAIN_ACTION',
                         'rollbackOnFailure', true,
                         'stepConfig', JSON_OBJECT(
                           'actionType', 'QUANTITY',
                           'operationType', 'INBOUND',
                           'params', JSON_OBJECT(
                             'accountCode', CONCAT('$', '{record.main.warehouseId}'),
                             'itemCode', CONCAT('$', '{item.materialId}'),
                             'dimensionKey', '',
                             'quantity', CONCAT('$', '{item.quantity}'),
                             'sourceDetailId', CONCAT('$', '{item.id}'),
                             'remark', '采购审批通过入库'
                           )
                         )
                       )
                     )
                   )
                 )
               )
             )
           )
         ) AS actions
  UNION ALL
  SELECT 'PW_OUTBOUND_ORDER',
         JSON_ARRAY(
           JSON_OBJECT(
             'actionCode', 'submit_outbound_approval',
             'actionName', '提交审批并锁定库存',
             'actionPosition', 'ROW',
             'actionType', 'COMMAND',
             'permission', 'ai:businessAction:execute',
             'confirmRequired', true,
             'successMessage', '出库审批已提交，库存已锁定',
             'failureMessage', '出库审批提交或库存锁定失败',
             'status', 1,
             'sortOrder', 10,
             'actionConfig', JSON_OBJECT(
               'successBehavior', 'refreshList',
               'steps', JSON_ARRAY(
                 JSON_OBJECT(
                   'stepCode', 'foreach_outbound_lock_items',
                   'stepName', '逐行锁定出库数量',
                   'stepType', 'FOREACH',
                   'rollbackOnFailure', true,
                   'stepConfig', JSON_OBJECT(
                     'collectionPath', 'record.children.pw_outbound_order_item',
                     'itemAlias', 'item',
                     'indexAlias', 'index',
                     'steps', JSON_ARRAY(
                       JSON_OBJECT(
                         'stepCode', 'outbound_item_lock',
                         'stepName', '明细锁定',
                         'stepType', 'DOMAIN_ACTION',
                         'rollbackOnFailure', true,
                         'stepConfig', JSON_OBJECT(
                           'actionType', 'QUANTITY',
                           'operationType', 'LOCK',
                           'params', JSON_OBJECT(
                             'accountCode', CONCAT('$', '{record.main.warehouseId}'),
                             'itemCode', CONCAT('$', '{item.materialId}'),
                             'dimensionKey', '',
                             'quantity', CONCAT('$', '{item.outboundQuantity}'),
                             'sourceDetailId', CONCAT('$', '{item.id}'),
                             'lockCode', CONCAT('$', '{item.id}'),
                             'remark', '出库提交审批锁定'
                           )
                         )
                       )
                     )
                   )
                 ),
                 JSON_OBJECT(
                   'stepCode', 'start_outbound_flow',
                   'stepName', '发起出库审批',
                   'stepType', 'START_FLOW',
                   'rollbackOnFailure', true,
                   'stepConfig', JSON_OBJECT(
                     'flowModelKey', 'leave_multi',
                     'title', '出库单审批',
                     'fieldMapping', JSON_ARRAY(
                       JSON_OBJECT('sourceField', 'record.main.id', 'targetField', 'documentId'),
                       JSON_OBJECT('sourceField', 'record.main.outboundNo', 'targetField', 'documentNo'),
                       JSON_OBJECT('sourceField', 'record.main.warehouseName', 'targetField', 'warehouseName'),
                       JSON_OBJECT('sourceField', 'record.main.createBy', 'targetField', 'applicantId'),
                       JSON_OBJECT('sourceField', 'record.main.orderStatus', 'targetField', 'documentStatus')
                     ),
                     'staticValues', JSON_OBJECT('deptManager', '1', 'hr', 'admin', 'approvalScene', 'OUTBOUND')
                   )
                 )
               )
             )
           ),
           JSON_OBJECT(
             'actionCode', 'commit_outbound_stock',
             'actionName', '出库扣减过账',
             'actionPosition', 'DETAIL',
             'actionType', 'COMMAND',
             'permission', 'ai:businessAction:execute',
             'confirmRequired', true,
             'successMessage', '出库扣减已写入数量台账',
             'failureMessage', '出库扣减过账失败',
             'status', 1,
             'sortOrder', 90,
             'actionConfig', JSON_OBJECT(
               'successBehavior', 'refreshList',
               'steps', JSON_ARRAY(
                 JSON_OBJECT(
                   'stepCode', 'foreach_outbound_commit_items',
                   'stepName', '逐行扣减出库数量',
                   'stepType', 'FOREACH',
                   'rollbackOnFailure', true,
                   'stepConfig', JSON_OBJECT(
                     'collectionPath', 'record.children.pw_outbound_order_item',
                     'itemAlias', 'item',
                     'indexAlias', 'index',
                     'steps', JSON_ARRAY(
                       JSON_OBJECT(
                         'stepCode', 'outbound_item_commit',
                         'stepName', '明细扣减',
                         'stepType', 'DOMAIN_ACTION',
                         'rollbackOnFailure', true,
                         'stepConfig', JSON_OBJECT(
                           'actionType', 'QUANTITY',
                           'operationType', 'COMMIT',
                           'params', JSON_OBJECT(
                             'accountCode', CONCAT('$', '{record.main.warehouseId}'),
                             'itemCode', CONCAT('$', '{item.materialId}'),
                             'dimensionKey', '',
                             'quantity', CONCAT('$', '{item.outboundQuantity}'),
                             'sourceDetailId', CONCAT('$', '{item.id}'),
                             'lockCode', CONCAT('$', '{item.id}'),
                             'remark', '出库审批通过扣减'
                           )
                         )
                       )
                     )
                   )
                 )
               )
             )
           ),
           JSON_OBJECT(
             'actionCode', 'release_outbound_stock',
             'actionName', '释放出库锁定',
             'actionPosition', 'DETAIL',
             'actionType', 'COMMAND',
             'permission', 'ai:businessAction:execute',
             'confirmRequired', true,
             'successMessage', '出库锁定已释放',
             'failureMessage', '释放出库锁定失败',
             'status', 1,
             'sortOrder', 100,
             'actionConfig', JSON_OBJECT(
               'successBehavior', 'refreshList',
               'steps', JSON_ARRAY(
                 JSON_OBJECT(
                   'stepCode', 'foreach_outbound_release_items',
                   'stepName', '逐行释放出库锁定',
                   'stepType', 'FOREACH',
                   'rollbackOnFailure', false,
                   'stepConfig', JSON_OBJECT(
                     'collectionPath', 'record.children.pw_outbound_order_item',
                     'itemAlias', 'item',
                     'indexAlias', 'index',
                     'steps', JSON_ARRAY(
                       JSON_OBJECT(
                         'stepCode', 'outbound_item_release',
                         'stepName', '明细释放',
                         'stepType', 'DOMAIN_ACTION',
                         'rollbackOnFailure', false,
                         'stepConfig', JSON_OBJECT(
                           'actionType', 'QUANTITY',
                           'operationType', 'RELEASE',
                           'params', JSON_OBJECT(
                             'accountCode', CONCAT('$', '{record.main.warehouseId}'),
                             'itemCode', CONCAT('$', '{item.materialId}'),
                             'dimensionKey', '',
                             'quantity', CONCAT('$', '{item.outboundQuantity}'),
                             'sourceDetailId', CONCAT('$', '{item.id}'),
                             'lockCode', CONCAT('$', '{item.id}'),
                             'remark', '出库审批驳回释放锁定'
                           )
                         )
                       )
                     )
                   )
                 )
               )
             )
           )
         )
  UNION ALL
  SELECT 'PW_TRANSFER_ORDER',
         JSON_ARRAY(
           JSON_OBJECT(
             'actionCode', 'submit_transfer_approval',
             'actionName', '提交审批',
             'actionPosition', 'ROW',
             'actionType', 'COMMAND',
             'permission', 'ai:businessAction:execute',
             'confirmRequired', true,
             'successMessage', '调拨审批已提交',
             'failureMessage', '调拨审批提交失败',
             'status', 1,
             'sortOrder', 10,
             'actionConfig', JSON_OBJECT(
               'successBehavior', 'refreshList',
               'steps', JSON_ARRAY(
                 JSON_OBJECT(
                   'stepCode', 'start_transfer_flow',
                   'stepName', '发起调拨审批',
                   'stepType', 'START_FLOW',
                   'rollbackOnFailure', true,
                   'stepConfig', JSON_OBJECT(
                     'flowModelKey', 'leave_multi',
                     'title', '调拨单审批',
                     'fieldMapping', JSON_ARRAY(
                       JSON_OBJECT('sourceField', 'record.main.id', 'targetField', 'documentId'),
                       JSON_OBJECT('sourceField', 'record.main.transferNo', 'targetField', 'documentNo'),
                       JSON_OBJECT('sourceField', 'record.main.fromWarehouseName', 'targetField', 'fromWarehouseName'),
                       JSON_OBJECT('sourceField', 'record.main.toWarehouseName', 'targetField', 'toWarehouseName'),
                       JSON_OBJECT('sourceField', 'record.main.createBy', 'targetField', 'applicantId'),
                       JSON_OBJECT('sourceField', 'record.main.orderStatus', 'targetField', 'documentStatus')
                     ),
                     'staticValues', JSON_OBJECT('deptManager', '1', 'hr', 'admin', 'approvalScene', 'TRANSFER')
                   )
                 )
               )
             )
           ),
           JSON_OBJECT(
             'actionCode', 'transfer_stock',
             'actionName', '调拨转移过账',
             'actionPosition', 'DETAIL',
             'actionType', 'COMMAND',
             'permission', 'ai:businessAction:execute',
             'confirmRequired', true,
             'successMessage', '调拨转移已写入数量台账',
             'failureMessage', '调拨转移过账失败',
             'status', 1,
             'sortOrder', 90,
             'actionConfig', JSON_OBJECT(
               'successBehavior', 'refreshList',
               'steps', JSON_ARRAY(
                 JSON_OBJECT(
                   'stepCode', 'foreach_transfer_items',
                   'stepName', '逐行调拨转移',
                   'stepType', 'FOREACH',
                   'rollbackOnFailure', true,
                   'stepConfig', JSON_OBJECT(
                     'collectionPath', 'record.children.pw_transfer_order_item',
                     'itemAlias', 'item',
                     'indexAlias', 'index',
                     'steps', JSON_ARRAY(
                       JSON_OBJECT(
                         'stepCode', 'transfer_item_move',
                         'stepName', '明细转移',
                         'stepType', 'DOMAIN_ACTION',
                         'rollbackOnFailure', true,
                         'stepConfig', JSON_OBJECT(
                           'actionType', 'QUANTITY',
                           'operationType', 'TRANSFER',
                           'params', JSON_OBJECT(
                             'accountCode', CONCAT('$', '{record.main.fromWarehouseId}'),
                             'itemCode', CONCAT('$', '{item.materialId}'),
                             'dimensionKey', '',
                             'targetAccountCode', CONCAT('$', '{record.main.toWarehouseId}'),
                             'targetItemCode', CONCAT('$', '{item.materialId}'),
                             'targetDimensionKey', '',
                             'quantity', CONCAT('$', '{item.transferQuantity}'),
                             'sourceDetailId', CONCAT('$', '{item.id}'),
                             'remark', '调拨审批通过转移'
                           )
                         )
                       )
                     )
                   )
                 )
               )
             )
           )
         )
) seed_actions ON seed_actions.object_code = o.object_code
SET o.designer_options = JSON_SET(
      COALESCE(CAST(o.designer_options AS JSON), JSON_OBJECT()),
      '$.documentManaged', true,
      '$.actions', seed_actions.actions
    ),
    o.update_by = 1,
    o.update_time = NOW()
WHERE o.tenant_id = 1
  AND o.suite_code = 'PROCUREMENT_WAREHOUSE';

UPDATE ai_crud_config c
JOIN (
  SELECT 'pw_purchase_order' AS config_key,
         JSON_ARRAY(
           JSON_OBJECT(
             'key', 'submit_purchase_approval',
             'label', '提交审批',
             'position', 'row',
             'actionType', 'COMMAND',
             'buttonType', 'success',
             'suiteCode', 'PROCUREMENT_WAREHOUSE',
             'objectCode', 'PW_PURCHASE_ORDER',
             'confirmText', '确认提交采购审批？',
             'successMessage', '采购审批已提交',
             'successBehavior', 'refreshList'
           )
         ) AS row_actions
  UNION ALL
  SELECT 'pw_outbound_order',
         JSON_ARRAY(
           JSON_OBJECT(
             'key', 'submit_outbound_approval',
             'label', '提交审批并锁定',
             'position', 'row',
             'actionType', 'COMMAND',
             'buttonType', 'success',
             'suiteCode', 'PROCUREMENT_WAREHOUSE',
             'objectCode', 'PW_OUTBOUND_ORDER',
             'confirmText', '确认提交出库审批并锁定库存？',
             'successMessage', '出库审批已提交，库存已锁定',
             'successBehavior', 'refreshList'
           )
         )
  UNION ALL
  SELECT 'pw_transfer_order',
         JSON_ARRAY(
           JSON_OBJECT(
             'key', 'submit_transfer_approval',
             'label', '提交审批',
             'position', 'row',
             'actionType', 'COMMAND',
             'buttonType', 'success',
             'suiteCode', 'PROCUREMENT_WAREHOUSE',
             'objectCode', 'PW_TRANSFER_ORDER',
             'confirmText', '确认提交调拨审批？',
             'successMessage', '调拨审批已提交',
             'successBehavior', 'refreshList'
           )
         )
) seed_runtime_actions ON seed_runtime_actions.config_key = c.config_key
SET c.options = JSON_SET(
      COALESCE(CAST(c.options AS JSON), JSON_OBJECT()),
      '$.rowActions', seed_runtime_actions.row_actions
    ),
    c.publish_time = NOW(),
    c.update_by = 1,
    c.update_time = NOW()
WHERE c.tenant_id = 1
  AND c.domain_code = 'PROCUREMENT_WAREHOUSE';
