package com.mdframe.forge.business.core.purchase.provider;

import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderTaskSaveDTO;
import com.mdframe.forge.business.core.purchase.service.SamplePurchaseOrderService;
import com.mdframe.forge.business.core.purchase.vo.SamplePurchaseOrderVO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessCodeFormProvider;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购单审批测试代码表单 Provider。
 */
@Component
@RequiredArgsConstructor
public class SamplePurchaseOrderCodeFormProvider implements BusinessCodeFormProvider {

    private static final String PROVIDER_KEY = "samplePurchaseOrder";

    private final SamplePurchaseOrderService purchaseOrderService;

    @Override
    public String providerKey() {
        return PROVIDER_KEY;
    }

    @Override
    public String providerName() {
        return "采购单审批代码表单";
    }

    @Override
    public List<Map<String, Object>> formAssets(String objectCode) {
        if (StringUtils.hasText(objectCode)
                && !SamplePurchaseOrderService.BUSINESS_TYPE.equalsIgnoreCase(objectCode)) {
            return List.of();
        }
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("formKey", "sample_purchase_order_approval_form");
        asset.put("formName", "采购单审批表单");
        asset.put("formMode", "BUSINESS_CODE_FORM");
        asset.put("providerKey", PROVIDER_KEY);
        asset.put("formUrl", "/business/purchase-order-test");
        asset.put("description", "代码实现的采购单审批表单，支持按流程节点控制字段。");
        return List.of(asset);
    }

    @Override
    public BusinessTaskFormContextVO buildContext(BusinessTaskFormContextQueryDTO query,
                                                  Map<String, Object> formRef,
                                                  List<Map<String, Object>> fieldPermissions) {
        Long recordId = query.getRecordId() != null ? query.getRecordId() : parseRecordId(query.getBusinessKey());
        SamplePurchaseOrderVO detail = recordId == null ? null : purchaseOrderService.detail(recordId);
        BusinessTaskFormContextVO vo = new BusinessTaskFormContextVO();
        vo.setConfigured(detail != null);
        vo.setFormType("business-code");
        vo.setTaskId(query.getTaskId());
        vo.setBusinessKey(query.getBusinessKey());
        vo.setProcessInstanceId(query.getProcessInstanceId());
        vo.setProcessDefKey(query.getProcessDefKey());
        vo.setTaskDefKey(query.getTaskDefKey());
        vo.setObjectCode(SamplePurchaseOrderService.BUSINESS_TYPE);
        vo.setRecordId(recordId);
        vo.setFormKey("sample_purchase_order_approval_form");
        vo.setFormName("采购单审批表单");
        vo.setProviderKey(PROVIDER_KEY);
        vo.setFormUrl("/business/purchase-order-test");
        vo.setFormRef(formRef == null ? new LinkedHashMap<>() : formRef);
        vo.setFieldPermissions(fieldPermissions == null ? List.of() : fieldPermissions);
        vo.setFields(buildFields());
        if (detail != null) {
            vo.setRecordData(toRecordData(detail));
        } else {
            vo.getWarnings().add("未找到采购单业务记录");
        }
        return vo;
    }

    @Override
    public BusinessTaskFormContextVO saveContext(BusinessTaskFormSaveDTO dto,
                                                 Map<String, Object> formRef,
                                                 List<Map<String, Object>> fieldPermissions) {
        SamplePurchaseOrderTaskSaveDTO saveDTO = new SamplePurchaseOrderTaskSaveDTO();
        saveDTO.setId(dto.getRecordId());
        saveDTO.setBusinessKey(dto.getBusinessKey());
        saveDTO.setTaskId(dto.getTaskId());
        saveDTO.setTaskDefKey(dto.getTaskDefKey());
        Map<String, Object> data = dto.getData() == null ? Map.of() : dto.getData();
        saveDTO.setTitle(text(data.get("title")));
        saveDTO.setSupplierName(text(data.get("supplierName")));
        saveDTO.setAmountCent(longValue(data.get("amountCent")));
        saveDTO.setPurchaseItems(text(data.get("purchaseItems")));
        saveDTO.setArrivalListFileIds(text(data.get("arrivalListFileIds")));
        saveDTO.setApplicantModifyRemark(text(data.get("applicantModifyRemark")));
        saveDTO.setDeptLeaderRemark(text(data.get("deptLeaderRemark")));
        saveDTO.setEngineeringManagerRemark(text(data.get("engineeringManagerRemark")));
        saveDTO.setCountersignRemark(text(data.get("countersignRemark")));
        SamplePurchaseOrderVO detail = purchaseOrderService.saveTaskFields(saveDTO);

        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(dto.getTaskId());
        query.setBusinessKey(dto.getBusinessKey());
        query.setProcessInstanceId(dto.getProcessInstanceId());
        query.setProcessDefKey(dto.getProcessDefKey());
        query.setTaskDefKey(dto.getTaskDefKey());
        query.setObjectCode(dto.getObjectCode());
        query.setRecordId(detail.getId());
        query.setFormKey(dto.getFormKey());
        return buildContext(query, formRef, fieldPermissions);
    }

    private List<Map<String, Object>> buildFields() {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field("title", "采购主题", "input"));
        fields.add(field("supplierName", "供应商", "input"));
        fields.add(field("amountCent", "采购金额(分)", "input-number"));
        fields.add(field("purchaseItems", "采购明细", "textarea"));
        fields.add(field("arrivalListFileIds", "上传清单", "input"));
        fields.add(field("applicantModifyRemark", "申请人修改说明", "textarea"));
        fields.add(field("deptLeaderRemark", "部门负责人意见", "textarea"));
        fields.add(field("engineeringManagerRemark", "工程部经理意见", "textarea"));
        fields.add(field("countersignRemark", "会签意见", "textarea"));
        return fields;
    }

    private Map<String, Object> field(String field, String label, String type) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("field", field);
        item.put("label", label);
        item.put("type", type);
        item.put("visible", true);
        item.put("writable", true);
        return item;
    }

    private Map<String, Object> toRecordData(SamplePurchaseOrderVO detail) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", detail.getId());
        data.put("orderNo", detail.getOrderNo());
        data.put("title", detail.getTitle());
        data.put("supplierName", detail.getSupplierName());
        data.put("amountCent", detail.getAmountCent());
        data.put("purchaseItems", detail.getPurchaseItems());
        data.put("arrivalListFileIds", detail.getArrivalListFileIds());
        data.put("applicantModifyRemark", detail.getApplicantModifyRemark());
        data.put("deptLeaderRemark", detail.getDeptLeaderRemark());
        data.put("engineeringManagerRemark", detail.getEngineeringManagerRemark());
        data.put("countersignRemark", detail.getCountersignRemark());
        return data;
    }

    private Long parseRecordId(String businessKey) {
        if (!StringUtils.hasText(businessKey)) {
            return null;
        }
        int index = businessKey.lastIndexOf(':');
        if (index < 0 || index == businessKey.length() - 1) {
            return null;
        }
        return longValue(businessKey.substring(index + 1));
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
