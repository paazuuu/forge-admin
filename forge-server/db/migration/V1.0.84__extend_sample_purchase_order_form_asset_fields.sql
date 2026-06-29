-- Extend sample purchase order node field permissions with needDate for applicant modification.

UPDATE ai_business_binding
SET binding_config = REPLACE(
    binding_config,
    '{"field":"purchaseItems","label":"采购明细","readable":true,"writable":true,"required":false},
               {"field":"applicantModifyRemark","label":"申请人修改说明","readable":true,"writable":true,"required":false}',
    '{"field":"purchaseItems","label":"采购明细","readable":true,"writable":true,"required":false},
               {"field":"needDate","label":"期望到货日期","readable":true,"writable":true,"required":false},
               {"field":"applicantModifyRemark","label":"申请人修改说明","readable":true,"writable":true,"required":false}'
  ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND target_type = 'OBJECT'
  AND target_code = 'sample_purchase_order'
  AND binding_type = 'FLOW'
  AND binding_key = 'sample_purchase_order_approval'
  AND binding_config LIKE '%"taskDefKey":"applicant_modify"%'
  AND binding_config NOT LIKE '%"field":"needDate"%';
