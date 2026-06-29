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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
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
    private static final String FORM_KEY = "sample_purchase_order_approval_form";
    private static final String FORM_NAME = "采购单审批表单";
    private static final String FORM_URL = "/business/purchase-order-test";

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
    public String buildSummary(Map<String, Object> recordData) {
        if (recordData == null || recordData.isEmpty()) {
            return null;
        }
        String orderNo = text(recordData.get("orderNo"));
        String applicant = text(recordData.get("applicantName"));
        return buildSummary(orderNo, applicant);
    }

    @Override
    public Map<Long, String> buildSummaries(String objectCode, Collection<Long> recordIds) {
        if (StringUtils.hasText(objectCode)
                && !SamplePurchaseOrderService.BUSINESS_TYPE.equalsIgnoreCase(objectCode)) {
            return Map.of();
        }
        List<SamplePurchaseOrderVO> details = purchaseOrderService.detailsByIds(recordIds);
        if (details == null || details.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new LinkedHashMap<>();
        for (SamplePurchaseOrderVO detail : details) {
            if (detail != null && detail.getId() != null) {
                result.put(detail.getId(), buildSummary(detail));
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> formAssets(String objectCode) {
        if (StringUtils.hasText(objectCode)
                && !SamplePurchaseOrderService.BUSINESS_TYPE.equalsIgnoreCase(objectCode)) {
            return List.of();
        }
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("appName", "采购申请");
        asset.put("objectName", "采购申请");
        asset.put("businessName", "采购申请");
        asset.put("formKey", FORM_KEY);
        asset.put("formName", FORM_NAME);
        asset.put("formMode", "BUSINESS_CODE_FORM");
        asset.put("providerKey", PROVIDER_KEY);
        asset.put("formUrl", FORM_URL);
        asset.put("fields", buildFields());
        asset.put("fieldCatalog", buildFields());
        asset.put("supportsSave", true);
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
        vo.setBusinessObjectName("采购申请");
        vo.setBusinessSummary(detail == null ? null : buildSummary(detail));
        vo.setRecordId(recordId);
        vo.setFormKey(FORM_KEY);
        vo.setFormName(FORM_NAME);
        vo.setProviderKey(PROVIDER_KEY);
        vo.setFormUrl(FORM_URL);
        vo.setFormRef(formRef == null ? new LinkedHashMap<>() : formRef);
        vo.setFieldPermissions(fieldPermissions == null ? List.of() : fieldPermissions);
        vo.setFields(buildFields(recordId));
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
        saveDTO.setNeedDate(localDateValue(data.get("needDate")));
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
        return buildFields(null);
    }

    private List<Map<String, Object>> buildFields(Long recordId) {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field("id", "采购单ID", "number", true, true, true, "系统主键，默认不展示"));
        fields.add(field("orderNo", "采购单号", "input", true));
        fields.add(field("title", "采购主题", "input"));
        fields.add(field("supplierName", "供应商", "input"));
        fields.add(field("amountCent", "采购金额（分）", "number"));
        fields.add(field("purchaseItems", "采购明细", "textarea"));
        fields.add(field("needDate", "期望到货日期", "date"));
        fields.add(field("status", "流程状态", "select", true));
        fields.add(field("applicantId", "申请人ID", "number", true, true, true, "系统用户 ID，默认不展示"));
        fields.add(field("applicantName", "申请人", "input", true));
        fields.add(field("applicantDeptId", "申请部门ID", "number", true, true, true, "组织 ID，默认不展示"));
        fields.add(field("applicantDeptName", "申请部门", "input", true));
        fields.add(field("businessKey", "业务Key", "input", true, true, true, "流程业务键，默认不展示"));
        fields.add(field("processInstanceId", "流程实例ID", "input", true, true, true, "流程实例 ID，默认不展示"));
        fields.add(field("deptLeaderId", "部门负责人ID", "number", true, true, true, "审批人 ID，默认不展示"));
        fields.add(field("deptLeaderName", "部门负责人", "input", true));
        fields.add(field("engineeringManagerId", "工程部经理ID", "number", true, true, true, "审批人 ID，默认不展示"));
        fields.add(field("engineeringManagerName", "工程部经理", "input", true));
        fields.add(field("countersignUserIds", "会签人员ID", "input", true, true, true, "会签用户 ID，默认不展示"));
        fields.add(field("countersignUserNames", "会签人员", "input", true));
        fields.add(field("ccRoleKeys", "抄送角色", "input", true, true, true, "抄送角色编码，默认不展示"));
        Map<String, Object> arrivalFiles = field("arrivalListFileIds", "上传清单", "fileUpload");
        arrivalFiles.put("businessType", SamplePurchaseOrderService.BUSINESS_TYPE);
        arrivalFiles.put("businessId", recordId == null ? null : String.valueOf(recordId));
        arrivalFiles.put("limit", 5);
        arrivalFiles.put("fileSize", 20);
        arrivalFiles.put("uploadButtonText", "上传清单");
        fields.add(arrivalFiles);
        fields.add(field("applicantModifyRemark", "申请人修改说明", "textarea"));
        fields.add(field("deptLeaderRemark", "部门负责人意见", "textarea"));
        fields.add(field("engineeringManagerRemark", "工程部经理意见", "textarea"));
        fields.add(field("countersignRemark", "会签意见", "textarea"));
        fields.add(field("rejectReason", "驳回原因", "textarea", true));
        fields.add(field("remark", "备注", "textarea"));
        fields.add(field("createTime", "创建时间", "datetime", true, true, true, "审计字段，默认不展示"));
        fields.add(field("updateTime", "更新时间", "datetime", true, true, true, "审计字段，默认不展示"));
        return fields;
    }

    private Map<String, Object> field(String field, String label, String type) {
        return field(field, label, type, false);
    }

    private Map<String, Object> field(String field, String label, String type, boolean readonly) {
        return field(field, label, type, readonly, false, false, null);
    }

    private Map<String, Object> field(String field, String label, String type,
                                      boolean readonly, boolean internal,
                                      boolean systemField, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("field", field);
        item.put("fieldCode", field);
        item.put("label", label);
        item.put("type", type);
        item.put("componentType", type);
        item.put("visible", !internal);
        item.put("writable", !readonly);
        item.put("readonly", readonly);
        item.put("internal", internal);
        item.put("systemField", systemField);
        if (StringUtils.hasText(description)) {
            item.put("description", description);
        }
        return item;
    }

    private String buildSummary(SamplePurchaseOrderVO detail) {
        return buildSummary(detail.getOrderNo(), detail.getApplicantName());
    }

    private String buildSummary(String orderNoValue, String applicantValue) {
        String orderNo = StringUtils.hasText(orderNoValue) ? orderNoValue : "未编号";
        String applicant = StringUtils.hasText(applicantValue) ? applicantValue : "未知申请人";
        return "采购申请单 · " + orderNo + " · " + applicant;
    }

    private Map<String, Object> toRecordData(SamplePurchaseOrderVO detail) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", detail.getId());
        data.put("orderNo", detail.getOrderNo());
        data.put("title", detail.getTitle());
        data.put("supplierName", detail.getSupplierName());
        data.put("amountCent", detail.getAmountCent());
        data.put("purchaseItems", detail.getPurchaseItems());
        data.put("needDate", detail.getNeedDate());
        data.put("status", detail.getStatus());
        data.put("applicantId", detail.getApplicantId());
        data.put("applicantName", detail.getApplicantName());
        data.put("applicantDeptId", detail.getApplicantDeptId());
        data.put("applicantDeptName", detail.getApplicantDeptName());
        data.put("businessKey", detail.getBusinessKey());
        data.put("processInstanceId", detail.getProcessInstanceId());
        data.put("deptLeaderId", detail.getDeptLeaderId());
        data.put("deptLeaderName", detail.getDeptLeaderName());
        data.put("engineeringManagerId", detail.getEngineeringManagerId());
        data.put("engineeringManagerName", detail.getEngineeringManagerName());
        data.put("countersignUserIds", detail.getCountersignUserIds());
        data.put("countersignUserNames", detail.getCountersignUserNames());
        data.put("ccRoleKeys", detail.getCcRoleKeys());
        data.put("arrivalListFileIds", detail.getArrivalListFileIds());
        data.put("applicantModifyRemark", detail.getApplicantModifyRemark());
        data.put("deptLeaderRemark", detail.getDeptLeaderRemark());
        data.put("engineeringManagerRemark", detail.getEngineeringManagerRemark());
        data.put("countersignRemark", detail.getCountersignRemark());
        data.put("rejectReason", detail.getRejectReason());
        data.put("remark", detail.getRemark());
        data.put("createTime", detail.getCreateTime());
        data.put("updateTime", detail.getUpdateTime());
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

    private LocalDate localDateValue(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
