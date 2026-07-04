-- Preserve procurement/warehouse field runtime metadata after designer save/publish.
-- Java now bridges legacy runtime schemas into page schema; this repairs already persisted samples.

SET @pw_warehouse_selector := JSON_OBJECT(
  'targetLabelField', 'warehouseName',
  'recordSelector', JSON_OBJECT(
    'suiteCode', 'PROCUREMENT_WAREHOUSE',
    'objectCode', 'PW_WAREHOUSE',
    'businessObjectCode', 'PW_WAREHOUSE',
    'targetObjectCode', 'PW_WAREHOUSE',
    'title', '选择仓库',
    'displayFields', JSON_ARRAY('warehouseCode:仓库编号', 'warehouseName:仓库名称', 'warehouseType:类型', 'projectName:关联项目'),
    'keywordFields', JSON_ARRAY('warehouseCode', 'warehouseName', 'projectName'),
    'searchParams', JSON_OBJECT('status', 'ENABLED'),
    'fieldMappings', JSON_ARRAY(
      JSON_OBJECT('sourceField', 'id', 'targetField', 'warehouseId'),
      JSON_OBJECT('sourceField', 'warehouseName', 'targetField', 'warehouseName')
    ),
    'valueField', 'id',
    'labelField', 'warehouseName',
    'targetLabelField', 'warehouseName'
  )
);
SET @pw_supplier_selector := JSON_OBJECT(
  'targetLabelField', 'supplierName',
  'recordSelector', JSON_OBJECT(
    'suiteCode', 'PROCUREMENT_WAREHOUSE',
    'objectCode', 'PW_SUPPLIER',
    'businessObjectCode', 'PW_SUPPLIER',
    'targetObjectCode', 'PW_SUPPLIER',
    'title', '选择供应商',
    'displayFields', JSON_ARRAY('supplierCode:供应商编号', 'supplierName:供应商名称', 'contactName:联系人', 'contactPhone:联系电话'),
    'keywordFields', JSON_ARRAY('supplierCode', 'supplierName', 'contactName'),
    'searchParams', JSON_OBJECT('status', 'ENABLED'),
    'fieldMappings', JSON_ARRAY(
      JSON_OBJECT('sourceField', 'id', 'targetField', 'supplierId'),
      JSON_OBJECT('sourceField', 'supplierName', 'targetField', 'supplierName'),
      JSON_OBJECT('sourceField', 'contactName', 'targetField', 'supplierContact'),
      JSON_OBJECT('sourceField', 'contactPhone', 'targetField', 'supplierPhone')
    ),
    'valueField', 'id',
    'labelField', 'supplierName',
    'targetLabelField', 'supplierName'
  )
);
SET @pw_from_warehouse_selector := JSON_OBJECT(
  'targetLabelField', 'fromWarehouseName',
  'recordSelector', JSON_OBJECT(
    'suiteCode', 'PROCUREMENT_WAREHOUSE',
    'objectCode', 'PW_WAREHOUSE',
    'businessObjectCode', 'PW_WAREHOUSE',
    'targetObjectCode', 'PW_WAREHOUSE',
    'title', '选择调出仓库',
    'displayFields', JSON_ARRAY('warehouseCode:仓库编号', 'warehouseName:仓库名称', 'warehouseType:类型', 'projectName:关联项目'),
    'keywordFields', JSON_ARRAY('warehouseCode', 'warehouseName', 'projectName'),
    'searchParams', JSON_OBJECT('status', 'ENABLED'),
    'fieldMappings', JSON_ARRAY(
      JSON_OBJECT('sourceField', 'id', 'targetField', 'fromWarehouseId'),
      JSON_OBJECT('sourceField', 'warehouseName', 'targetField', 'fromWarehouseName')
    ),
    'valueField', 'id',
    'labelField', 'warehouseName',
    'targetLabelField', 'fromWarehouseName'
  )
);
SET @pw_to_warehouse_selector := JSON_OBJECT(
  'targetLabelField', 'toWarehouseName',
  'recordSelector', JSON_OBJECT(
    'suiteCode', 'PROCUREMENT_WAREHOUSE',
    'objectCode', 'PW_WAREHOUSE',
    'businessObjectCode', 'PW_WAREHOUSE',
    'targetObjectCode', 'PW_WAREHOUSE',
    'title', '选择调入仓库',
    'displayFields', JSON_ARRAY('warehouseCode:仓库编号', 'warehouseName:仓库名称', 'warehouseType:类型', 'projectName:关联项目'),
    'keywordFields', JSON_ARRAY('warehouseCode', 'warehouseName', 'projectName'),
    'searchParams', JSON_OBJECT('status', 'ENABLED'),
    'fieldMappings', JSON_ARRAY(
      JSON_OBJECT('sourceField', 'id', 'targetField', 'toWarehouseId'),
      JSON_OBJECT('sourceField', 'warehouseName', 'targetField', 'toWarehouseName')
    ),
    'valueField', 'id',
    'labelField', 'warehouseName',
    'targetLabelField', 'toWarehouseName'
  )
);
SET @pw_common_status_props := JSON_OBJECT('dictType', 'pw_common_status');
SET @pw_order_status_props := JSON_OBJECT('dictType', 'pw_order_status');
SET @pw_warehouse_type_props := JSON_OBJECT('dictType', 'pw_warehouse_type');
SET @pw_common_status_render := JSON_OBJECT('type', 'dictTag', 'dictType', 'pw_common_status');
SET @pw_order_status_render := JSON_OBJECT('type', 'dictTag', 'dictType', 'pw_order_status');
SET @pw_warehouse_type_render := JSON_OBJECT('type', 'dictTag', 'dictType', 'pw_warehouse_type');
SET @pw_gen_supplier := JSON_OBJECT('enabled', true, 'mode', 'CODE_RULE', 'ruleCode', 'supplier_code', 'trigger', 'ON_CREATE', 'fillPolicy', 'EMPTY_ONLY');
SET @pw_gen_warehouse := JSON_OBJECT('enabled', true, 'mode', 'CODE_RULE', 'ruleCode', 'warehouse_code', 'trigger', 'ON_CREATE', 'fillPolicy', 'EMPTY_ONLY');
SET @pw_gen_material := JSON_OBJECT('enabled', true, 'mode', 'CODE_RULE', 'ruleCode', 'material_code', 'trigger', 'ON_CREATE', 'fillPolicy', 'EMPTY_ONLY');
SET @pw_gen_purchase := JSON_OBJECT('enabled', true, 'mode', 'CODE_RULE', 'ruleCode', 'purchase_no', 'trigger', 'ON_CREATE', 'fillPolicy', 'EMPTY_ONLY');
SET @pw_gen_outbound := JSON_OBJECT('enabled', true, 'mode', 'CODE_RULE', 'ruleCode', 'outbound_no', 'trigger', 'ON_CREATE', 'fillPolicy', 'EMPTY_ONLY');
SET @pw_gen_transfer := JSON_OBJECT('enabled', true, 'mode', 'CODE_RULE', 'ruleCode', 'transfer_no', 'trigger', 'ON_CREATE', 'fillPolicy', 'EMPTY_ONLY');

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field')), '.field', '.label'), '目标仓库',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field')), '.field', '.type'), 'recordSelector',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field')), '.field', '.componentType'), 'recordSelector',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field')), '.field', '.required'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field')), '.field', '.props'), @pw_warehouse_selector
    ),
    page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(
        JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'),
        JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key')
      )), '.zoneKey', '.props.fieldSettings.warehouseId'), '.key', '.props.fieldSettings.warehouseId'),
      JSON_OBJECT('label', '目标仓库', 'componentType', 'recordSelector', 'type', 'recordSelector', 'required', true, 'props', @pw_warehouse_selector)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_purchase_order'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field') IS NOT NULL
  AND COALESCE(
    JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'),
    JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key')
  ) IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'supplierId', NULL, '$[*].field')), '.field', '.label'), '供应商',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'supplierId', NULL, '$[*].field')), '.field', '.type'), 'recordSelector',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'supplierId', NULL, '$[*].field')), '.field', '.componentType'), 'recordSelector',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'supplierId', NULL, '$[*].field')), '.field', '.props'), @pw_supplier_selector
    ),
    page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(
        JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'),
        JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key')
      )), '.zoneKey', '.props.fieldSettings.supplierId'), '.key', '.props.fieldSettings.supplierId'),
      JSON_OBJECT('label', '供应商', 'componentType', 'recordSelector', 'type', 'recordSelector', 'props', @pw_supplier_selector)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_purchase_order'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'supplierId', NULL, '$[*].field') IS NOT NULL
  AND COALESCE(
    JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'),
    JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key')
  ) IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field')), '.field', '.label'), '所属仓库',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field')), '.field', '.type'), 'recordSelector',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field')), '.field', '.componentType'), 'recordSelector',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field')), '.field', '.required'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field')), '.field', '.props'), @pw_warehouse_selector
    ),
    page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(
        JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'),
        JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key')
      )), '.zoneKey', '.props.fieldSettings.warehouseId'), '.key', '.props.fieldSettings.warehouseId'),
      JSON_OBJECT('label', '所属仓库', 'componentType', 'recordSelector', 'type', 'recordSelector', 'required', true, 'props', @pw_warehouse_selector)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_outbound_order'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseId', NULL, '$[*].field') IS NOT NULL
  AND COALESCE(
    JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'),
    JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key')
  ) IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'fromWarehouseId', NULL, '$[*].field')), '.field', '.type'), 'recordSelector',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'fromWarehouseId', NULL, '$[*].field')), '.field', '.componentType'), 'recordSelector',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'fromWarehouseId', NULL, '$[*].field')), '.field', '.props'), @pw_from_warehouse_selector,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'toWarehouseId', NULL, '$[*].field')), '.field', '.type'), 'recordSelector',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'toWarehouseId', NULL, '$[*].field')), '.field', '.componentType'), 'recordSelector',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'toWarehouseId', NULL, '$[*].field')), '.field', '.props'), @pw_to_warehouse_selector
    ),
    page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(
        JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'),
        JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key')
      )), '.zoneKey', '.props.fieldSettings.fromWarehouseId'), '.key', '.props.fieldSettings.fromWarehouseId'),
      JSON_OBJECT('label', '调出仓库', 'componentType', 'recordSelector', 'type', 'recordSelector', 'required', true, 'props', @pw_from_warehouse_selector),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(
        JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'),
        JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key')
      )), '.zoneKey', '.props.fieldSettings.toWarehouseId'), '.key', '.props.fieldSettings.toWarehouseId'),
      JSON_OBJECT('label', '调入仓库', 'componentType', 'recordSelector', 'type', 'recordSelector', 'required', true, 'props', @pw_to_warehouse_selector)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_transfer_order'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'fromWarehouseId', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'toWarehouseId', NULL, '$[*].field') IS NOT NULL
  AND COALESCE(
    JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'),
    JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key')
  ) IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.dictType'), 'pw_order_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.props'), @pw_order_status_props,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.defaultValue'), 'DRAFT'
    ),
    search_schema = JSON_SET(
      CAST(search_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.dictType'), 'pw_order_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field')), '.field', '.props'), @pw_order_status_props
    ),
    columns_schema = JSON_SET(
      CAST(columns_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].prop')), '.prop', '.render'), @pw_order_status_render
    ),
    page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key'))), '.zoneKey', '.props.fieldSettings.orderStatus'), '.key', '.props.fieldSettings.orderStatus'),
      JSON_OBJECT('componentType', 'select', 'type', 'select', 'dictType', 'pw_order_status', 'defaultValue', 'DRAFT', 'props', @pw_order_status_props),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'search', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'search', NULL, '$.zones[*].key'))), '.zoneKey', '.props.fieldSettings.orderStatus'), '.key', '.props.fieldSettings.orderStatus'),
      JSON_OBJECT('componentType', 'select', 'type', 'select', 'dictType', 'pw_order_status', 'queryType', 'eq', 'props', @pw_order_status_props),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'table', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'table', NULL, '$.zones[*].key'))), '.zoneKey', '.props.fieldSettings.orderStatus'), '.key', '.props.fieldSettings.orderStatus'),
      JSON_OBJECT('componentType', 'select', 'dictType', 'pw_order_status', 'renderType', 'dictTag', 'props', @pw_order_status_props)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key IN ('pw_purchase_order', 'pw_outbound_order', 'pw_transfer_order')
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'orderStatus', NULL, '$[*].prop') IS NOT NULL
  AND COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key')) IS NOT NULL
  AND COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'search', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'search', NULL, '$.zones[*].key')) IS NOT NULL
  AND COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'table', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'table', NULL, '$.zones[*].key')) IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.props'), @pw_common_status_props,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.defaultValue'), 'ENABLED'
    ),
    search_schema = JSON_SET(
      CAST(search_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'status', NULL, '$[*].field')), '.field', '.props'), @pw_common_status_props
    ),
    columns_schema = JSON_SET(
      CAST(columns_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'status', NULL, '$[*].prop')), '.prop', '.render'), @pw_common_status_render
    ),
    page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key'))), '.zoneKey', '.props.fieldSettings.status'), '.key', '.props.fieldSettings.status'),
      JSON_OBJECT('componentType', 'select', 'type', 'select', 'dictType', 'pw_common_status', 'defaultValue', 'ENABLED', 'props', @pw_common_status_props),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'search', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'search', NULL, '$.zones[*].key'))), '.zoneKey', '.props.fieldSettings.status'), '.key', '.props.fieldSettings.status'),
      JSON_OBJECT('componentType', 'select', 'type', 'select', 'dictType', 'pw_common_status', 'queryType', 'eq', 'props', @pw_common_status_props),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'table', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'table', NULL, '$.zones[*].key'))), '.zoneKey', '.props.fieldSettings.status'), '.key', '.props.fieldSettings.status'),
      JSON_OBJECT('componentType', 'select', 'dictType', 'pw_common_status', 'renderType', 'dictTag', 'props', @pw_common_status_props)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key IN ('pw_material', 'pw_supplier', 'pw_supplier_material', 'pw_warehouse')
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'status', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'status', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'status', NULL, '$[*].prop') IS NOT NULL
  AND COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key')) IS NOT NULL
  AND COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'search', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'search', NULL, '$.zones[*].key')) IS NOT NULL
  AND COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'table', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'table', NULL, '$.zones[*].key')) IS NOT NULL;

UPDATE ai_crud_config
SET edit_schema = JSON_SET(
      CAST(edit_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.dictType'), 'pw_warehouse_type',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.props'), @pw_warehouse_type_props
    ),
    search_schema = JSON_SET(
      CAST(search_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.type'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.dictType'), 'pw_warehouse_type',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field')), '.field', '.props'), @pw_warehouse_type_props
    ),
    columns_schema = JSON_SET(
      CAST(columns_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].prop')), '.prop', '.render'), @pw_warehouse_type_render
    ),
    page_schema = JSON_SET(
      CAST(page_schema AS JSON),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key'))), '.zoneKey', '.props.fieldSettings.warehouseType'), '.key', '.props.fieldSettings.warehouseType'),
      JSON_OBJECT('componentType', 'select', 'type', 'select', 'dictType', 'pw_warehouse_type', 'props', @pw_warehouse_type_props),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'search', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'search', NULL, '$.zones[*].key'))), '.zoneKey', '.props.fieldSettings.warehouseType'), '.key', '.props.fieldSettings.warehouseType'),
      JSON_OBJECT('componentType', 'select', 'type', 'select', 'dictType', 'pw_warehouse_type', 'queryType', 'eq', 'props', @pw_warehouse_type_props),
      REPLACE(REPLACE(JSON_UNQUOTE(COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'table', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'table', NULL, '$.zones[*].key'))), '.zoneKey', '.props.fieldSettings.warehouseType'), '.key', '.props.fieldSettings.warehouseType'),
      JSON_OBJECT('componentType', 'select', 'dictType', 'pw_warehouse_type', 'renderType', 'dictTag', 'props', @pw_warehouse_type_props)
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_warehouse'
  AND JSON_SEARCH(CAST(edit_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(search_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].field') IS NOT NULL
  AND JSON_SEARCH(CAST(columns_schema AS JSON), 'one', 'warehouseType', NULL, '$[*].prop') IS NOT NULL
  AND COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'edit', NULL, '$.zones[*].key')) IS NOT NULL
  AND COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'search', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'search', NULL, '$.zones[*].key')) IS NOT NULL
  AND COALESCE(JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'table', NULL, '$.zones[*].zoneKey'), JSON_SEARCH(CAST(page_schema AS JSON), 'one', 'table', NULL, '$.zones[*].key')) IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field')), '.field', '.businessFieldType'), 'DICT',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field')), '.field', '.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field')), '.field', '.basicProps.dictType'), 'pw_common_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field')), '.field', '.advancedProps.dictType'), 'pw_common_status'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_material', 'pw_supplier', 'pw_supplier_material', 'pw_warehouse')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'status', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.businessFieldType'), 'DICT',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.dictType'), 'pw_order_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.basicProps.dictType'), 'pw_order_status',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field')), '.field', '.advancedProps.dictType'), 'pw_order_status'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code IN ('pw_purchase_order', 'pw_outbound_order', 'pw_transfer_order')
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'orderStatus', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field')), '.field', '.businessFieldType'), 'DICT',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field')), '.field', '.componentType'), 'select',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field')), '.field', '.dictType'), 'pw_warehouse_type',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field')), '.field', '.basicProps.dictType'), 'pw_warehouse_type',
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field')), '.field', '.advancedProps.dictType'), 'pw_warehouse_type'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_warehouse'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseType', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'supplierCode', NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'supplierCode', NULL, '$.fields[*].field')), '.field', '.readonly'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'supplierCode', NULL, '$.fields[*].field')), '.field', '.basicProps.generation'), @pw_gen_supplier
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_supplier'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'supplierCode', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseCode', NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseCode', NULL, '$.fields[*].field')), '.field', '.readonly'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseCode', NULL, '$.fields[*].field')), '.field', '.basicProps.generation'), @pw_gen_warehouse
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_warehouse'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'warehouseCode', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'materialCode', NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'materialCode', NULL, '$.fields[*].field')), '.field', '.readonly'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'materialCode', NULL, '$.fields[*].field')), '.field', '.basicProps.generation'), @pw_gen_material
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_material'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'materialCode', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'purchaseNo', NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'purchaseNo', NULL, '$.fields[*].field')), '.field', '.readonly'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'purchaseNo', NULL, '$.fields[*].field')), '.field', '.basicProps.generation'), @pw_gen_purchase
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_purchase_order'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'purchaseNo', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'outboundNo', NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'outboundNo', NULL, '$.fields[*].field')), '.field', '.readonly'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'outboundNo', NULL, '$.fields[*].field')), '.field', '.basicProps.generation'), @pw_gen_outbound
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_outbound_order'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'outboundNo', NULL, '$.fields[*].field') IS NOT NULL;

UPDATE ai_lowcode_model
SET model_schema = JSON_SET(
      CAST(model_schema AS JSON),
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'transferNo', NULL, '$.fields[*].field')), '.field', '.required'), false,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'transferNo', NULL, '$.fields[*].field')), '.field', '.readonly'), true,
      REPLACE(JSON_UNQUOTE(JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'transferNo', NULL, '$.fields[*].field')), '.field', '.basicProps.generation'), @pw_gen_transfer
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND domain_code = 'PROCUREMENT_WAREHOUSE'
  AND model_code = 'pw_transfer_order'
  AND JSON_SEARCH(CAST(model_schema AS JSON), 'one', 'transferNo', NULL, '$.fields[*].field') IS NOT NULL;
