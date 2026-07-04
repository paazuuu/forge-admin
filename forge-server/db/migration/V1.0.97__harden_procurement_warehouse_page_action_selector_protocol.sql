-- Harden procurement/warehouse page, action and selector protocol.
-- Platform fixes are in Java; this only repairs persisted sample metadata.

UPDATE ai_crud_config
SET page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(
        JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'toolbar', NULL, '$.zones[*].zoneKey')),
        '.zoneKey',
        '.fieldRefs'
      ),
      JSON_ARRAY()
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'toolbar', NULL, '$.zones[*].zoneKey') IS NOT NULL;

UPDATE ai_crud_config
SET page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(
        JSON_UNQUOTE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'toolbar', NULL, '$.zones[*].key')),
        '.key',
        '.fieldRefs'
      ),
      JSON_ARRAY()
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'toolbar', NULL, '$.zones[*].key') IS NOT NULL;

UPDATE ai_crud_config
SET options = JSON_SET(
      COALESCE(CAST(options AS JSON), JSON_OBJECT()),
      '$.rowActions[0].actionCode', 'submit_purchase_approval',
      '$.rowActions[0].suiteCode', 'PROCUREMENT_WAREHOUSE',
      '$.rowActions[0].objectCode', 'PW_PURCHASE_ORDER',
      '$.rowActions[0].businessObjectCode', 'PW_PURCHASE_ORDER',
      '$.rowActions[0].targetObjectCode', 'PW_PURCHASE_ORDER'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_purchase_order';

UPDATE ai_crud_config
SET options = JSON_SET(
      COALESCE(CAST(options AS JSON), JSON_OBJECT()),
      '$.rowActions[0].actionCode', 'submit_outbound_approval',
      '$.rowActions[0].suiteCode', 'PROCUREMENT_WAREHOUSE',
      '$.rowActions[0].objectCode', 'PW_OUTBOUND_ORDER',
      '$.rowActions[0].businessObjectCode', 'PW_OUTBOUND_ORDER',
      '$.rowActions[0].targetObjectCode', 'PW_OUTBOUND_ORDER'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_outbound_order';

UPDATE ai_crud_config
SET options = JSON_SET(
      COALESCE(CAST(options AS JSON), JSON_OBJECT()),
      '$.rowActions[0].actionCode', 'submit_transfer_approval',
      '$.rowActions[0].suiteCode', 'PROCUREMENT_WAREHOUSE',
      '$.rowActions[0].objectCode', 'PW_TRANSFER_ORDER',
      '$.rowActions[0].businessObjectCode', 'PW_TRANSFER_ORDER',
      '$.rowActions[0].targetObjectCode', 'PW_TRANSFER_ORDER'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_transfer_order';

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field')), '.field', '.props.recordSelector.businessObjectCode'), 'PW_WAREHOUSE',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field')), '.field', '.props.recordSelector.targetObjectCode'), 'PW_WAREHOUSE'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key IN ('pw_purchase_order', 'pw_outbound_order')
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'supplierId', NULL, '$[*].field')), '.field', '.props.recordSelector.businessObjectCode'), 'PW_SUPPLIER',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'supplierId', NULL, '$[*].field')), '.field', '.props.recordSelector.targetObjectCode'), 'PW_SUPPLIER'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key IN ('pw_supplier_material', 'pw_purchase_order')
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'supplierId', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'materialId', NULL, '$[*].field')), '.field', '.props.recordSelector.businessObjectCode'), 'PW_MATERIAL',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'materialId', NULL, '$[*].field')), '.field', '.props.recordSelector.targetObjectCode'), 'PW_MATERIAL'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_supplier_material'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'materialId', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'fromWarehouseId', NULL, '$[*].field')), '.field', '.props.recordSelector.businessObjectCode'), 'PW_WAREHOUSE',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'fromWarehouseId', NULL, '$[*].field')), '.field', '.props.recordSelector.targetObjectCode'), 'PW_WAREHOUSE'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_transfer_order'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'fromWarehouseId', NULL, '$[*].field') IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'toWarehouseId', NULL, '$[*].field')), '.field', '.props.recordSelector.businessObjectCode'), 'PW_WAREHOUSE',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'toWarehouseId', NULL, '$[*].field')), '.field', '.props.recordSelector.targetObjectCode'), 'PW_WAREHOUSE'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_transfer_order'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'toWarehouseId', NULL, '$[*].field') IS NOT NULL;
