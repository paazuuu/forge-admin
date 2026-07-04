-- Finalize procurement/warehouse low-code runtime closure.
-- Platform code stays generic; procurement/warehouse details live in seed data only.

SET @sql := (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE pw_supplier_material MODIFY COLUMN material_id bigint DEFAULT NULL COMMENT ''物料ID''',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'pw_supplier_material'
    AND column_name = 'material_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE ai_crud_config
SET search_schema = JSON_SET(
      COALESCE(CAST(search_schema AS JSON), JSON_ARRAY()),
      '$[4].type', 'select',
      '$[4].dictType', 'pw_common_status'
    ),
    columns_schema = JSON_SET(
      COALESCE(CAST(columns_schema AS JSON), JSON_ARRAY()),
      '$[7].render', JSON_OBJECT('type', 'dictTag', 'dictType', 'pw_common_status')
    ),
    edit_schema = JSON_SET(
      COALESCE(CAST(edit_schema AS JSON), JSON_ARRAY()),
      '$[2]', JSON_OBJECT(
        'field', 'materialId',
        'label', '物料',
        'type', 'recordSelector',
        'required', true,
        'props', JSON_OBJECT(
          'targetLabelField', 'materialName',
          'recordSelector', JSON_OBJECT(
            'suiteCode', 'PROCUREMENT_WAREHOUSE',
            'objectCode', 'PW_MATERIAL',
            'businessObjectCode', 'PW_MATERIAL',
            'targetObjectCode', 'PW_MATERIAL',
            'title', '选择物料',
            'displayFields', JSON_ARRAY('materialCode:物料编号', 'materialName:物料名称', 'specModel:规格型号', 'unit:单位', 'referencePriceCent:参考单价(分)'),
            'keywordFields', JSON_ARRAY('materialCode', 'materialName', 'specModel'),
            'searchParams', JSON_OBJECT('status', 'ENABLED'),
            'fieldMappings', JSON_ARRAY(
              JSON_OBJECT('sourceField', 'id', 'targetField', 'materialId'),
              JSON_OBJECT('sourceField', 'materialCode', 'targetField', 'materialCode'),
              JSON_OBJECT('sourceField', 'materialName', 'targetField', 'materialName'),
              JSON_OBJECT('sourceField', 'specModel', 'targetField', 'specModel'),
              JSON_OBJECT('sourceField', 'unit', 'targetField', 'unit'),
              JSON_OBJECT('sourceField', 'referencePriceCent', 'targetField', 'quotePriceCent')
            ),
            'valueField', 'id',
            'labelField', 'materialName',
            'targetLabelField', 'materialName'
          )
        )
      ),
      '$[10].type', 'select',
      '$[10].componentType', 'select',
      '$[10].dictType', 'pw_common_status',
      '$[10].defaultValue', 'ENABLED'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_supplier_material';

UPDATE ai_crud_config
SET options = JSON_SET(
      COALESCE(CAST(options AS JSON), JSON_OBJECT()),
      '$.masterDetailConfig.children[0].recordSelector.objectCode', 'PW_MATERIAL',
      '$.masterDetailConfig.children[0].recordSelector.businessObjectCode', 'PW_MATERIAL',
      '$.masterDetailConfig.children[0].recordSelector.targetObjectCode', 'PW_MATERIAL',
      '$.masterDetailConfig.children[0].fields[0].formVisible', false,
      '$.masterDetailConfig.children[0].fields[0].required', false,
      '$.masterDetailConfig.children[0].fields[0].hidden', true,
      '$.masterDetailConfig.children[0].fields[0].showInChildEditor', false,
      '$.masterDetailConfig.children[0].fields[8].type', 'select',
      '$.masterDetailConfig.children[0].fields[8].componentType', 'select',
      '$.masterDetailConfig.children[0].fields[8].dictType', 'pw_common_status',
      '$.masterDetailConfig.children[0].fields[8].defaultValue', 'ENABLED'
    ),
    page_schema = JSON_SET(
      COALESCE(CAST(page_schema AS JSON), JSON_OBJECT()),
      '$.modelRefs[1].props.recordSelector.objectCode', 'PW_MATERIAL',
      '$.modelRefs[1].props.recordSelector.businessObjectCode', 'PW_MATERIAL',
      '$.modelRefs[1].props.recordSelector.targetObjectCode', 'PW_MATERIAL',
      '$.modelRefs[1].fields[0].formVisible', false,
      '$.modelRefs[1].fields[0].required', false,
      '$.modelRefs[1].fields[0].hidden', true,
      '$.modelRefs[1].fields[0].showInChildEditor', false,
      '$.modelRefs[1].fields[8].type', 'select',
      '$.modelRefs[1].fields[8].componentType', 'select',
      '$.modelRefs[1].fields[8].dictType', 'pw_common_status',
      '$.modelRefs[1].fields[8].defaultValue', 'ENABLED'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'pw_supplier';

DROP TEMPORARY TABLE IF EXISTS tmp_pw_flow_model_seed;
CREATE TEMPORARY TABLE tmp_pw_flow_model_seed AS
SELECT seed.object_code,
       seed.flow_key,
       seed.flow_name,
       seed.binding_name,
       seed.title_template,
       CONCAT(
         '<?xml version="1.0" encoding="UTF-8"?>\n',
         '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef">\n',
         '  <bpmn:process id="', seed.flow_key, '" name="', seed.flow_name, '" isExecutable="true">\n',
         '    <bpmn:startEvent id="startEvent" name="开始" flowable:initiator="initiator" />\n',
         '    <bpmn:sequenceFlow id="flow_start_to_approve" sourceRef="startEvent" targetRef="userTask_approve" />\n',
         '    <bpmn:userTask id="userTask_approve" name="审批" flowable:assignee="1" />\n',
         '    <bpmn:sequenceFlow id="flow_approve_to_end" sourceRef="userTask_approve" targetRef="endEvent" />\n',
         '    <bpmn:endEvent id="endEvent" name="结束" />\n',
         '  </bpmn:process>\n',
         '  <bpmndi:BPMNDiagram id="BPMNDiagram_', seed.flow_key, '">\n',
         '    <bpmndi:BPMNPlane id="BPMNPlane_', seed.flow_key, '" bpmnElement="', seed.flow_key, '">\n',
         '      <bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent"><dc:Bounds x="160" y="160" width="36" height="36" /></bpmndi:BPMNShape>\n',
         '      <bpmndi:BPMNShape id="userTask_approve_di" bpmnElement="userTask_approve"><dc:Bounds x="280" y="138" width="110" height="80" /></bpmndi:BPMNShape>\n',
         '      <bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent"><dc:Bounds x="470" y="160" width="36" height="36" /></bpmndi:BPMNShape>\n',
         '      <bpmndi:BPMNEdge id="flow_start_to_approve_di" bpmnElement="flow_start_to_approve"><di:waypoint x="196" y="178" /><di:waypoint x="280" y="178" /></bpmndi:BPMNEdge>\n',
         '      <bpmndi:BPMNEdge id="flow_approve_to_end_di" bpmnElement="flow_approve_to_end"><di:waypoint x="390" y="178" /><di:waypoint x="470" y="178" /></bpmndi:BPMNEdge>\n',
         '    </bpmndi:BPMNPlane>\n',
         '  </bpmndi:BPMNDiagram>\n',
         '</bpmn:definitions>\n'
       ) AS bpmn_xml
FROM (
  SELECT 'PW_PURCHASE_ORDER' object_code, 'pw_purchase_approval' flow_key, '采购单审批流程' flow_name, '采购单审批流程' binding_name, '采购单审批' title_template
  UNION ALL SELECT 'PW_OUTBOUND_ORDER', 'pw_outbound_approval', '出库单审批流程', '出库单审批流程', '出库单审批'
  UNION ALL SELECT 'PW_TRANSFER_ORDER', 'pw_transfer_approval', '调拨单审批流程', '调拨单审批流程', '调拨单审批'
) seed;

INSERT INTO sys_flow_model (id, tenant_id, model_key, model_name, description, category, flow_type, form_type,
                            form_json, version, status, create_by, create_time, update_time, del_flag, bpmn_xml)
SELECT flow_key, 1, flow_key, flow_name, CONCAT(flow_name, '，采购仓储低代码样例流程'), 'PROCUREMENT_WAREHOUSE',
       'approval', 'dynamic', '{}', 1, 0, 'admin', NOW(), NOW(), 0, bpmn_xml
FROM tmp_pw_flow_model_seed seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_flow_model m
  WHERE m.tenant_id = 1
    AND m.model_key = seed.flow_key
);

UPDATE sys_flow_model m
JOIN tmp_pw_flow_model_seed seed ON seed.flow_key = m.model_key
SET m.model_name = seed.flow_name,
    m.description = CONCAT(seed.flow_name, '，采购仓储低代码样例流程'),
    m.category = 'PROCUREMENT_WAREHOUSE',
    m.flow_type = 'approval',
    m.form_type = 'dynamic',
    m.bpmn_xml = IF(m.bpmn_xml IS NULL OR m.bpmn_xml = '', seed.bpmn_xml, m.bpmn_xml),
    m.del_flag = 0,
    m.update_time = NOW()
WHERE m.tenant_id = 1;

UPDATE ai_business_document_config c
JOIN tmp_pw_flow_model_seed seed ON seed.object_code = c.object_code
SET c.default_flow_key = seed.flow_key,
    c.options = JSON_SET(
      COALESCE(CAST(c.options AS JSON), JSON_OBJECT()),
      '$.flowModelKey', seed.flow_key,
      '$.flowModelName', seed.flow_name,
      '$.showStartFlowAction', false,
      '$.hideStartFlowAction', true
    ),
    c.update_by = 1,
    c.update_time = NOW()
WHERE c.tenant_id = 1
  AND c.suite_code = 'PROCUREMENT_WAREHOUSE';

UPDATE ai_business_binding b
JOIN tmp_pw_flow_model_seed seed ON seed.object_code = b.target_code AND seed.flow_key = b.binding_key
SET b.binding_key = seed.flow_key,
    b.binding_name = seed.binding_name,
    b.binding_config = JSON_SET(
      COALESCE(CAST(b.binding_config AS JSON), JSON_OBJECT()),
      '$.flowModelKey', seed.flow_key,
      '$.flowModelName', seed.flow_name,
      '$.titleTemplate', seed.title_template,
      '$.startMode', 'ACTION_ONLY',
      '$.options.lowcodeApp', true,
      '$.options.scenario', 'procurement_warehouse'
    ),
    b.status = 1,
    b.update_by = 1,
    b.update_time = NOW()
WHERE b.tenant_id = 1
  AND b.target_type = 'OBJECT'
  AND b.binding_type = 'FLOW';

UPDATE ai_business_binding b
JOIN tmp_pw_flow_model_seed seed ON seed.object_code = b.target_code
LEFT JOIN ai_business_binding existing
  ON existing.tenant_id = b.tenant_id
 AND existing.target_type = b.target_type
 AND existing.target_code = b.target_code
 AND existing.binding_type = b.binding_type
 AND existing.binding_key = seed.flow_key
 AND existing.id <> b.id
SET b.binding_key = seed.flow_key,
    b.binding_name = seed.binding_name,
    b.binding_config = JSON_SET(
      COALESCE(CAST(b.binding_config AS JSON), JSON_OBJECT()),
      '$.flowModelKey', seed.flow_key,
      '$.flowModelName', seed.flow_name,
      '$.titleTemplate', seed.title_template,
      '$.startMode', 'ACTION_ONLY',
      '$.options.lowcodeApp', true,
      '$.options.scenario', 'procurement_warehouse'
    ),
    b.status = 1,
    b.update_by = 1,
    b.update_time = NOW()
WHERE b.tenant_id = 1
  AND b.target_type = 'OBJECT'
  AND b.binding_type = 'FLOW'
  AND b.binding_key = 'leave_multi'
  AND existing.id IS NULL;

UPDATE ai_business_binding b
JOIN tmp_pw_flow_model_seed seed ON seed.object_code = b.target_code
JOIN ai_business_binding existing
  ON existing.tenant_id = b.tenant_id
 AND existing.target_type = b.target_type
 AND existing.target_code = b.target_code
 AND existing.binding_type = b.binding_type
 AND existing.binding_key = seed.flow_key
 AND existing.id <> b.id
SET b.status = 0,
    b.description = '已替换为采购仓储业务流程模型绑定',
    b.update_by = 1,
    b.update_time = NOW()
WHERE b.tenant_id = 1
  AND b.target_type = 'OBJECT'
  AND b.binding_type = 'FLOW'
  AND b.binding_key = 'leave_multi';

INSERT INTO ai_business_binding (id, tenant_id, target_type, target_id, target_code, binding_type, binding_key,
                                 binding_name, binding_config, description, status, sort_order,
                                 create_by, create_time, create_dept, update_by, update_time)
SELECT 1910000000000009500 + ROW_NUMBER() OVER (ORDER BY seed.object_code),
       1, 'OBJECT', o.id, seed.object_code, 'FLOW', seed.flow_key, seed.binding_name,
       JSON_OBJECT(
         'flowModelKey', seed.flow_key,
         'flowModelName', seed.flow_name,
         'titleTemplate', seed.title_template,
         'startMode', 'ACTION_ONLY',
         'options', JSON_OBJECT('lowcodeApp', true, 'scenario', 'procurement_warehouse')
       ),
       CONCAT(seed.binding_name, '，绑定采购仓储业务流程模型'), 1, 10,
       1, NOW(), 1, 1, NOW()
FROM tmp_pw_flow_model_seed seed
JOIN ai_business_object o
  ON o.tenant_id = 1
 AND o.suite_code = 'PROCUREMENT_WAREHOUSE'
 AND o.object_code = seed.object_code
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_binding b
  WHERE b.tenant_id = 1
    AND b.target_type = 'OBJECT'
    AND b.target_code = seed.object_code
    AND b.binding_type = 'FLOW'
    AND b.binding_key = seed.flow_key
);

DROP TEMPORARY TABLE IF EXISTS tmp_pw_relation_seed;
CREATE TEMPORARY TABLE tmp_pw_relation_seed (
  id BIGINT NOT NULL PRIMARY KEY,
  source_object_code VARCHAR(64) NOT NULL,
  target_object_code VARCHAR(64) NOT NULL,
  relation_type VARCHAR(32) NOT NULL,
  relation_name VARCHAR(128) NOT NULL,
  source_field_code VARCHAR(64) NOT NULL,
  target_field_code VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL,
  relation_config JSON NOT NULL,
  description VARCHAR(500) NOT NULL
);

INSERT INTO tmp_pw_relation_seed (id, source_object_code, target_object_code, relation_type, relation_name,
                                  source_field_code, target_field_code, sort_order, relation_config, description)
VALUES
  (1910000000000009501, 'PW_SUPPLIER', 'PW_SUPPLIER_MATERIAL', 'DETAIL', '供应商报价',
   'id', 'supplierId', 10,
   JSON_OBJECT(
     'detailTabTitle', '供应商报价',
     'showInDetail', true,
     'inlineCreateEnabled', true,
     'inlineEditEnabled', true,
     'saveMode', 'merge',
     'recordSelector', JSON_OBJECT(
       'suiteCode', 'PROCUREMENT_WAREHOUSE',
       'objectCode', 'PW_MATERIAL',
       'businessObjectCode', 'PW_MATERIAL',
       'targetObjectCode', 'PW_MATERIAL',
       'title', '选择物料',
       'buttonText', '选择物料',
       'displayFields', JSON_ARRAY('materialCode:物料编号', 'materialName:物料名称', 'specModel:规格型号', 'unit:单位', 'referencePriceCent:参考单价(分)'),
       'keywordFields', JSON_ARRAY('materialCode', 'materialName', 'specModel'),
       'searchParams', JSON_OBJECT('status', 'ENABLED'),
       'fieldMappings', JSON_ARRAY(
         JSON_OBJECT('sourceField', 'id', 'targetField', 'materialId'),
         JSON_OBJECT('sourceField', 'materialCode', 'targetField', 'materialCode'),
         JSON_OBJECT('sourceField', 'materialName', 'targetField', 'materialName'),
         JSON_OBJECT('sourceField', 'specModel', 'targetField', 'specModel'),
         JSON_OBJECT('sourceField', 'unit', 'targetField', 'unit'),
         JSON_OBJECT('sourceField', 'referencePriceCent', 'targetField', 'quotePriceCent')
       )
     )
   ),
   '供应商与报价明细的主子关系'),
  (1910000000000009502, 'PW_PURCHASE_ORDER', 'PW_PURCHASE_ORDER_ITEM', 'DETAIL', '采购明细',
   'id', 'purchaseId', 20,
   JSON_OBJECT(
     'detailTabTitle', '采购明细',
     'showInDetail', true,
     'inlineCreateEnabled', true,
     'inlineEditEnabled', true,
     'saveMode', 'merge',
     'recordSelector', JSON_OBJECT(
       'suiteCode', 'PROCUREMENT_WAREHOUSE',
       'objectCode', 'PW_SUPPLIER_MATERIAL',
       'businessObjectCode', 'PW_SUPPLIER_MATERIAL',
       'targetObjectCode', 'PW_SUPPLIER_MATERIAL',
       'title', '选择供应商报价',
       'buttonText', '选择报价',
       'displayFields', JSON_ARRAY('supplierName:供应商', 'materialCode:物料编号', 'materialName:物料名称', 'quotePriceCent:报价(分)', 'effectiveDate:有效期'),
       'keywordFields', JSON_ARRAY('materialCode', 'materialName', 'supplierName'),
       'searchParams', JSON_OBJECT('supplierId', CONCAT('$', '{formData.supplierId}'), 'status', 'ENABLED'),
       'fieldMappings', JSON_ARRAY(
         JSON_OBJECT('sourceField', 'materialId', 'targetField', 'materialId'),
         JSON_OBJECT('sourceField', 'materialCode', 'targetField', 'materialCode'),
         JSON_OBJECT('sourceField', 'materialName', 'targetField', 'materialName'),
         JSON_OBJECT('sourceField', 'specModel', 'targetField', 'specModel'),
         JSON_OBJECT('sourceField', 'unit', 'targetField', 'unit'),
         JSON_OBJECT('sourceField', 'lastPriceCent', 'targetField', 'costPriceCent'),
         JSON_OBJECT('sourceField', 'quotePriceCent', 'targetField', 'dealPriceCent')
       )
     )
   ),
   '采购单与采购明细的主子关系'),
  (1910000000000009503, 'PW_OUTBOUND_ORDER', 'PW_OUTBOUND_ORDER_ITEM', 'DETAIL', '出库明细',
   'id', 'outboundId', 30,
   JSON_OBJECT(
     'detailTabTitle', '出库明细',
     'showInDetail', true,
     'inlineCreateEnabled', true,
     'inlineEditEnabled', true,
     'saveMode', 'merge',
     'recordSelector', JSON_OBJECT(
       'suiteCode', 'PROCUREMENT_WAREHOUSE',
       'objectCode', 'PW_MATERIAL',
       'businessObjectCode', 'PW_MATERIAL',
       'targetObjectCode', 'PW_MATERIAL',
       'title', '选择出库物料',
       'buttonText', '选择物料',
       'displayFields', JSON_ARRAY('materialCode:物料编号', 'materialName:物料名称', 'specModel:规格型号', 'unit:单位'),
       'keywordFields', JSON_ARRAY('materialCode', 'materialName', 'specModel'),
       'searchParams', JSON_OBJECT('status', 'ENABLED', 'warehouseId', CONCAT('$', '{formData.warehouseId}')),
       'fieldMappings', JSON_ARRAY(
         JSON_OBJECT('sourceField', 'id', 'targetField', 'materialId'),
         JSON_OBJECT('sourceField', 'materialCode', 'targetField', 'materialCode'),
         JSON_OBJECT('sourceField', 'materialName', 'targetField', 'materialName'),
         JSON_OBJECT('sourceField', 'specModel', 'targetField', 'specModel'),
         JSON_OBJECT('sourceField', 'unit', 'targetField', 'unit')
       )
     )
   ),
   '出库单与出库明细的主子关系'),
  (1910000000000009504, 'PW_TRANSFER_ORDER', 'PW_TRANSFER_ORDER_ITEM', 'DETAIL', '调拨明细',
   'id', 'transferId', 40,
   JSON_OBJECT(
     'detailTabTitle', '调拨明细',
     'showInDetail', true,
     'inlineCreateEnabled', true,
     'inlineEditEnabled', true,
     'saveMode', 'merge',
     'recordSelector', JSON_OBJECT(
       'suiteCode', 'PROCUREMENT_WAREHOUSE',
       'objectCode', 'PW_MATERIAL',
       'businessObjectCode', 'PW_MATERIAL',
       'targetObjectCode', 'PW_MATERIAL',
       'title', '选择调拨物料',
       'buttonText', '选择物料',
       'displayFields', JSON_ARRAY('materialCode:物料编号', 'materialName:物料名称', 'specModel:规格型号', 'unit:单位'),
       'keywordFields', JSON_ARRAY('materialCode', 'materialName', 'specModel'),
       'searchParams', JSON_OBJECT('status', 'ENABLED', 'fromWarehouseId', CONCAT('$', '{formData.fromWarehouseId}')),
       'fieldMappings', JSON_ARRAY(
         JSON_OBJECT('sourceField', 'id', 'targetField', 'materialId'),
         JSON_OBJECT('sourceField', 'materialCode', 'targetField', 'materialCode'),
         JSON_OBJECT('sourceField', 'materialName', 'targetField', 'materialName'),
         JSON_OBJECT('sourceField', 'specModel', 'targetField', 'specModel'),
         JSON_OBJECT('sourceField', 'unit', 'targetField', 'unit')
       )
     )
   ),
   '调拨单与调拨明细的主子关系');

INSERT INTO ai_business_object_relation (id, tenant_id, suite_code, source_object_code, target_object_code,
                                         relation_type, relation_name, source_field_code, target_field_code,
                                         relation_config, description, status, sort_order,
                                         create_by, create_time, create_dept, update_by, update_time)
SELECT seed.id, 1, 'PROCUREMENT_WAREHOUSE', seed.source_object_code, seed.target_object_code,
       seed.relation_type, seed.relation_name, seed.source_field_code, seed.target_field_code,
       seed.relation_config, seed.description, 1, seed.sort_order,
       1, NOW(), 1, 1, NOW()
FROM tmp_pw_relation_seed seed
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_object_relation r
  WHERE r.tenant_id = 1
    AND r.suite_code = 'PROCUREMENT_WAREHOUSE'
    AND r.source_object_code = seed.source_object_code
    AND r.target_object_code = seed.target_object_code
    AND r.relation_type = seed.relation_type
    AND r.relation_name = seed.relation_name
);

UPDATE ai_business_object_relation r
JOIN tmp_pw_relation_seed seed
  ON seed.source_object_code = r.source_object_code
 AND seed.target_object_code = r.target_object_code
 AND seed.relation_type = r.relation_type
 AND seed.relation_name = r.relation_name
SET r.source_field_code = seed.source_field_code,
    r.target_field_code = seed.target_field_code,
    r.relation_config = seed.relation_config,
    r.description = seed.description,
    r.status = 1,
    r.sort_order = seed.sort_order,
    r.update_by = 1,
    r.update_time = NOW()
WHERE r.tenant_id = 1
  AND r.suite_code = 'PROCUREMENT_WAREHOUSE';

DROP TEMPORARY TABLE IF EXISTS tmp_pw_relation_seed;
DROP TEMPORARY TABLE IF EXISTS tmp_pw_flow_model_seed;
