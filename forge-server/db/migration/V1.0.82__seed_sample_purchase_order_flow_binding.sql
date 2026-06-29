-- Seed the sample purchase order code-business flow binding.
-- The runtime owner of node forms is ai_business_binding.binding_config.nodeForms.

INSERT INTO ai_business_binding (id, tenant_id, target_type, target_id, target_code, binding_type, binding_key,
                                 binding_name, binding_config, description, status, sort_order, create_by,
                                 create_time, create_dept, update_by, update_time)
SELECT 1910000000000008201, 1, 'OBJECT', NULL, 'sample_purchase_order', 'FLOW', 'sample_purchase_order_approval',
       '采购单审批测试流程',
       '{
         "flowModelKey":"sample_purchase_order_approval",
         "flowModelName":"采购单审批测试流程",
         "titleTemplate":"采购单审批-{orderNo}",
         "startMode":"MANUAL",
         "businessBinding":{
           "mode":"ADAPTER",
           "primaryKeyField":"id",
           "tenantField":"tenant_id",
           "titleField":"title",
           "statusField":"status",
           "ownerField":"applicantId"
         },
         "variableMapping":[
           {"formField":"businessKey","flowVariable":"businessKey","label":"业务Key"},
           {"formField":"orderNo","flowVariable":"orderNo","label":"采购单号"},
           {"formField":"title","flowVariable":"title","label":"采购主题"},
           {"formField":"amountCent","flowVariable":"amountCent","label":"采购金额分"},
           {"formField":"deptLeaderId","flowVariable":"deptLeaderId","label":"部门负责人"},
           {"formField":"engineeringManagerId","flowVariable":"engineeringManagerId","label":"工程部经理"},
           {"formField":"countersignUserIds","flowVariable":"countersignUserList","label":"会签人员"}
         ],
         "nodeForms":[
           {
             "taskDefKey":"dept_leader_approve",
             "taskName":"部门负责人审批",
             "formMode":"BUSINESS_CODE_FORM",
             "formKey":"sample_purchase_order_approval_form",
             "formName":"采购单审批表单",
             "providerKey":"samplePurchaseOrder",
             "editMode":"EDITABLE",
             "formRef":{"type":"BUSINESS_CODE_FORM","objectCode":"sample_purchase_order","providerKey":"samplePurchaseOrder","formKey":"sample_purchase_order_approval_form"},
             "fieldPermissions":[
               {"field":"arrivalListFileIds","label":"上传清单","readable":true,"writable":true,"required":false},
               {"field":"deptLeaderRemark","label":"部门负责人意见","readable":true,"writable":true,"required":false}
             ]
           },
           {
             "taskDefKey":"engineering_manager_approve",
             "taskName":"工程部经理审批",
             "formMode":"BUSINESS_CODE_FORM",
             "formKey":"sample_purchase_order_approval_form",
             "formName":"采购单审批表单",
             "providerKey":"samplePurchaseOrder",
             "editMode":"EDITABLE",
             "formRef":{"type":"BUSINESS_CODE_FORM","objectCode":"sample_purchase_order","providerKey":"samplePurchaseOrder","formKey":"sample_purchase_order_approval_form"},
             "fieldPermissions":[
               {"field":"engineeringManagerRemark","label":"工程部经理意见","readable":true,"writable":true,"required":false}
             ]
           },
           {
             "taskDefKey":"purchase_countersign",
             "taskName":"采购会签",
             "formMode":"BUSINESS_CODE_FORM",
             "formKey":"sample_purchase_order_approval_form",
             "formName":"采购单审批表单",
             "providerKey":"samplePurchaseOrder",
             "editMode":"EDITABLE",
             "formRef":{"type":"BUSINESS_CODE_FORM","objectCode":"sample_purchase_order","providerKey":"samplePurchaseOrder","formKey":"sample_purchase_order_approval_form"},
             "fieldPermissions":[
               {"field":"countersignRemark","label":"会签意见","readable":true,"writable":true,"required":false}
             ]
           },
           {
             "taskDefKey":"applicant_modify",
             "taskName":"申请人修改",
             "formMode":"BUSINESS_CODE_FORM",
             "formKey":"sample_purchase_order_approval_form",
             "formName":"采购单审批表单",
             "providerKey":"samplePurchaseOrder",
             "editMode":"MODIFY_RESUBMIT",
             "formRef":{"type":"BUSINESS_CODE_FORM","objectCode":"sample_purchase_order","providerKey":"samplePurchaseOrder","formKey":"sample_purchase_order_approval_form"},
             "fieldPermissions":[
               {"field":"title","label":"采购主题","readable":true,"writable":true,"required":true},
               {"field":"supplierName","label":"供应商","readable":true,"writable":true,"required":true},
               {"field":"amountCent","label":"采购金额分","readable":true,"writable":true,"required":true},
               {"field":"purchaseItems","label":"采购明细","readable":true,"writable":true,"required":false},
               {"field":"applicantModifyRemark","label":"申请人修改说明","readable":true,"writable":true,"required":false}
             ]
           }
         ],
         "conditionFlows":[],
         "options":{"sample":true,"codeApp":true,"businessKeyPattern":"sample_purchase_order:{recordId}"}
       }',
       '采购单代码业务默认流程绑定，节点表单和字段权限由业务流程配置维护', 1, 1, 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_binding
  WHERE tenant_id = 1
    AND target_type = 'OBJECT'
    AND target_code = 'sample_purchase_order'
    AND binding_type = 'FLOW'
    AND binding_key = 'sample_purchase_order_approval'
);
