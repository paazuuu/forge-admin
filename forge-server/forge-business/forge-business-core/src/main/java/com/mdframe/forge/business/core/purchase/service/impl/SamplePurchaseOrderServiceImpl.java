package com.mdframe.forge.business.core.purchase.service.impl;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.business.core.purchase.domain.SamplePurchaseOrder;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderDTO;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderQuery;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderSubmitDTO;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderTaskSaveDTO;
import com.mdframe.forge.business.core.purchase.mapper.SamplePurchaseOrderMapper;
import com.mdframe.forge.business.core.purchase.service.SamplePurchaseOrderService;
import com.mdframe.forge.business.core.purchase.support.SamplePurchaseOrderFlowBpmn;
import com.mdframe.forge.business.core.purchase.vo.SamplePurchaseOrderFlowInitVO;
import com.mdframe.forge.business.core.purchase.vo.SamplePurchaseOrderVO;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.flow.client.annotation.FlowBind;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 采购单审批测试服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FlowBind(modelKey = SamplePurchaseOrderService.MODEL_KEY, businessType = SamplePurchaseOrderService.BUSINESS_TYPE)
public class SamplePurchaseOrderServiceImpl extends ServiceImpl<SamplePurchaseOrderMapper, SamplePurchaseOrder>
        implements SamplePurchaseOrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final FlowClient flowClient;
    private final IdentifierGenerator identifierGenerator;

    @Override
    public IPage<SamplePurchaseOrderVO> page(PageQuery pageQuery, SamplePurchaseOrderQuery query) {
        Page<SamplePurchaseOrderVO> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        return getBaseMapper().selectPage(page, resolveTenantId(), query);
    }

    @Override
    public SamplePurchaseOrderVO detail(Long id) {
        if (id == null) {
            throw new BusinessException("采购单ID不能为空");
        }
        SamplePurchaseOrderVO detail = getBaseMapper().selectDetail(resolveTenantId(), id);
        if (detail == null) {
            throw new BusinessException("采购单不存在");
        }
        return detail;
    }

    @Override
    public SamplePurchaseOrderVO detailByBusinessKey(String businessKey) {
        if (!StringUtils.hasText(businessKey)) {
            throw new BusinessException("采购单业务Key不能为空");
        }
        SamplePurchaseOrder entity = getBaseMapper().selectByBusinessKey(resolveTenantId(), businessKey.trim());
        if (entity == null) {
            throw new BusinessException("采购单不存在");
        }
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SamplePurchaseOrderDTO dto) {
        validateBase(dto);
        SamplePurchaseOrder entity = new SamplePurchaseOrder();
        entity.setId(identifierGenerator.nextId(entity).longValue());
        entity.setTenantId(resolveTenantId());
        applyBaseFields(entity, dto);
        entity.setStatus(STATUS_DRAFT);
        entity.setApplicantId(SessionHelper.getUserId());
        entity.setApplicantName(resolveUsername());
        entity.setApplicantDeptId(SessionHelper.getMainOrgId());
        entity.setApplicantDeptName(null);
        if (!StringUtils.hasText(entity.getOrderNo())) {
            entity.setOrderNo(generateOrderNo(entity.getId()));
        }
        save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SamplePurchaseOrderDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("采购单ID不能为空");
        }
        validateBase(dto);
        SamplePurchaseOrder entity = requireEntity(dto.getId());
        if (!STATUS_DRAFT.equals(entity.getStatus()) && !STATUS_NEED_MODIFY.equals(entity.getStatus())) {
            throw new BusinessException("只有草稿或待修改采购单允许编辑");
        }
        applyBaseFields(entity, dto);
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SamplePurchaseOrder entity = requireEntity(id);
        if (!List.of(STATUS_DRAFT, STATUS_REJECTED, STATUS_CANCELED).contains(entity.getStatus())) {
            throw new BusinessException("当前状态不允许删除");
        }
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submit(Long id, SamplePurchaseOrderSubmitDTO dto) {
        SamplePurchaseOrder entity = requireEntity(id);
        if (!STATUS_DRAFT.equals(entity.getStatus())) {
            throw new BusinessException("只有草稿采购单允许发起流程");
        }
        validateSubmit(dto);
        ensureFlowModel();

        String businessKey = buildBusinessKey(entity.getId());
        List<String> countersignUserList = dto.getCountersignUserIds().stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .toList();
        List<String> ccRoleKeys = normalizeRoleKeys(dto.getCcRoleKeys());
        String userId = SessionHelper.getUserId() == null ? null : String.valueOf(SessionHelper.getUserId());
        String userName = resolveUsername();

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("businessKey", businessKey);
        variables.put("objectCode", BUSINESS_TYPE);
        variables.put("recordId", entity.getId());
        variables.put("purchaseOrderId", entity.getId());
        variables.put("orderNo", entity.getOrderNo());
        variables.put("title", entity.getTitle());
        variables.put("amountCent", entity.getAmountCent());
        variables.put("initiator", userId);
        variables.put("deptLeaderId", String.valueOf(dto.getDeptLeaderId()));
        variables.put("engineeringManagerId", String.valueOf(dto.getEngineeringManagerId()));
        variables.put("countersignUserList", countersignUserList);
        variables.put("ccRoleKeys", ccRoleKeys);

        FlowResult<String> result = flowClient.startProcess(
                MODEL_KEY,
                businessKey,
                BUSINESS_TYPE,
                "采购单审批-" + entity.getOrderNo(),
                variables,
                userId,
                userName,
                SessionHelper.getMainOrgId() == null ? null : String.valueOf(SessionHelper.getMainOrgId()),
                null);
        if (result == null || !result.isSuccess() || !StringUtils.hasText(result.getData())) {
            throw new BusinessException("流程发起失败: " + (result == null ? "无返回结果" : result.getMsg()));
        }

        entity.setBusinessKey(businessKey);
        entity.setProcessInstanceId(result.getData());
        entity.setDeptLeaderId(dto.getDeptLeaderId());
        entity.setEngineeringManagerId(dto.getEngineeringManagerId());
        entity.setCountersignUserIds(joinLongs(dto.getCountersignUserIds()));
        entity.setCcRoleKeys(String.join(",", ccRoleKeys));
        entity.setStatus(STATUS_IN_PROCESS);
        updateById(entity);
        return result.getData();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SamplePurchaseOrderVO saveTaskFields(SamplePurchaseOrderTaskSaveDTO dto) {
        if (dto == null) {
            throw new BusinessException("保存参数不能为空");
        }
        SamplePurchaseOrder entity = resolveTaskEntity(dto);
        String taskDefKey = StringUtils.hasText(dto.getTaskDefKey()) ? dto.getTaskDefKey() : "";
        switch (taskDefKey) {
            case "applicant_modify" -> {
                if (StringUtils.hasText(dto.getTitle())) {
                    entity.setTitle(dto.getTitle().trim());
                }
                if (StringUtils.hasText(dto.getSupplierName())) {
                    entity.setSupplierName(dto.getSupplierName().trim());
                }
                if (dto.getAmountCent() != null) {
                    if (dto.getAmountCent() <= 0) {
                        throw new BusinessException("采购金额必须大于0");
                    }
                    entity.setAmountCent(dto.getAmountCent());
                }
                if (dto.getNeedDate() != null) {
                    entity.setNeedDate(dto.getNeedDate());
                }
                entity.setPurchaseItems(dto.getPurchaseItems());
                entity.setApplicantModifyRemark(dto.getApplicantModifyRemark());
            }
            case "dept_leader_approve" -> {
                entity.setDeptLeaderRemark(dto.getDeptLeaderRemark());
                entity.setArrivalListFileIds(dto.getArrivalListFileIds());
            }
            case "engineering_manager_approve" -> entity.setEngineeringManagerRemark(dto.getEngineeringManagerRemark());
            case "purchase_countersign" -> entity.setCountersignRemark(dto.getCountersignRemark());
            default -> throw new BusinessException("当前节点不允许保存采购单字段: " + taskDefKey);
        }
        updateById(entity);
        return detail(entity.getId());
    }

    @Override
    public SamplePurchaseOrderFlowInitVO ensureFlowModel() {
        String bpmnXml = SamplePurchaseOrderFlowBpmn.build();
        FlowResult<Map<String, Object>> current = flowClient.getModelByKey(MODEL_KEY);
        Map<String, Object> model = current == null ? null : current.getData();
        boolean created = model == null || model.get("id") == null;
        boolean shouldDeploy = created;

        if (created) {
            FlowResult<Map<String, Object>> createdResult = flowClient.createModel(buildFlowModelPayload(null, bpmnXml));
            if (createdResult == null || !createdResult.isSuccess() || createdResult.getData() == null) {
                throw new BusinessException("创建采购单测试流程失败: "
                        + (createdResult == null ? "无返回结果" : createdResult.getMsg()));
            }
            model = createdResult.getData();
        } else {
            String existingBpmnXml = Objects.toString(model.get("bpmnXml"), "");
            Integer status = toInteger(model.get("status"));
            shouldDeploy = status == null || status != 1 || !existingBpmnXml.equals(bpmnXml);
            if (!existingBpmnXml.equals(bpmnXml)) {
                FlowResult<Map<String, Object>> updatedResult = flowClient.updateModel(buildFlowModelPayload(model.get("id"), bpmnXml));
                if (updatedResult == null || !updatedResult.isSuccess() || updatedResult.getData() == null) {
                    throw new BusinessException("更新采购单测试流程失败: "
                            + (updatedResult == null ? "无返回结果" : updatedResult.getMsg()));
                }
                model = updatedResult.getData();
            }
        }

        String deploymentId = Objects.toString(model.get("deploymentId"), null);
        if (shouldDeploy) {
            FlowResult<String> deployedResult = flowClient.deployModel(Objects.toString(model.get("id"), ""));
            if (deployedResult == null || !deployedResult.isSuccess()) {
                throw new BusinessException("发布采购单测试流程失败: "
                        + (deployedResult == null ? "无返回结果" : deployedResult.getMsg()));
            }
            deploymentId = deployedResult.getData();
        }

        SamplePurchaseOrderFlowInitVO vo = new SamplePurchaseOrderFlowInitVO();
        vo.setModelKey(MODEL_KEY);
        vo.setModelId(Objects.toString(model.get("id"), null));
        vo.setDeploymentId(deploymentId);
        vo.setStatus(shouldDeploy ? 1 : toInteger(model.get("status")));
        vo.setMessage(shouldDeploy ? "采购单测试流程已初始化并发布" : "采购单测试流程已存在，无需重复发布");
        return vo;
    }

    @FlowCallback(on = {
            FlowCallback.ON_TASK_COMPLETED,
            FlowCallback.ON_COMPLETED,
            FlowCallback.ON_REJECTED,
            FlowCallback.ON_CANCELED
    })
    @Transactional(rollbackFor = Exception.class)
    public void handleFlowEvent(FlowEventContext context) {
        if (context == null || !StringUtils.hasText(context.getBusinessKey())) {
            return;
        }
        SamplePurchaseOrder entity = getBaseMapper().selectByBusinessKey(
                resolveTenantId(context.getTenantId()), context.getBusinessKey());
        if (entity == null) {
            log.warn("采购单流程回调未找到业务记录: event={}, businessKey={}",
                    context.getEvent(), context.getBusinessKey());
            return;
        }
        if (FlowCallback.ON_TASK_COMPLETED.equals(context.getEvent())) {
            handleTaskCompleted(entity, context);
            return;
        }
        if (FlowCallback.ON_COMPLETED.equals(context.getEvent())) {
            copyFinishVariables(entity, context.getVariables());
            entity.setStatus(STATUS_APPROVED);
            updateById(entity);
            return;
        }
        if (FlowCallback.ON_REJECTED.equals(context.getEvent())) {
            entity.setStatus(STATUS_REJECTED);
            entity.setRejectReason(firstText(context.getLastComment(), context.getComment(), entity.getRejectReason()));
            updateById(entity);
            return;
        }
        if (FlowCallback.ON_CANCELED.equals(context.getEvent())) {
            entity.setStatus(STATUS_CANCELED);
            entity.setRejectReason(firstText(context.getLastComment(), context.getComment(), entity.getRejectReason()));
            updateById(entity);
        }
    }

    private void handleTaskCompleted(SamplePurchaseOrder entity, FlowEventContext context) {
        Map<String, Object> variables = context.getVariables();
        String approvalResult = variables == null ? null : Objects.toString(variables.get("approvalResult"), null);
        copyFinishVariables(entity, variables);
        if ("reject".equalsIgnoreCase(approvalResult)) {
            if ("applicant_modify".equals(context.getTaskDefKey())) {
                entity.setStatus(STATUS_REJECTED);
            } else {
                entity.setStatus(STATUS_NEED_MODIFY);
            }
            entity.setRejectReason(firstText(context.getComment(), context.getLastComment(), entity.getRejectReason()));
            updateById(entity);
            return;
        }
        if ("applicant_modify".equals(context.getTaskDefKey())) {
            entity.setStatus(STATUS_IN_PROCESS);
            updateById(entity);
        }
    }

    private void copyFinishVariables(SamplePurchaseOrder entity, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return;
        }
        Object arrivalListFileIds = variables.get("arrivalListFileIds");
        if (arrivalListFileIds != null) {
            entity.setArrivalListFileIds(String.valueOf(arrivalListFileIds));
        }
    }

    private Map<String, Object> buildFlowModelPayload(Object id, String bpmnXml) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (id != null) {
            payload.put("id", id);
        }
        payload.put("tenantId", resolveTenantId());
        payload.put("modelKey", MODEL_KEY);
        payload.put("modelName", "采购单审批测试流程");
        payload.put("description", "用于验证业务模块与流程模块联动：部门负责人、工程部经理、会签、驳回修改、抄送。");
        payload.put("category", "purchase");
        payload.put("flowType", "purchase");
        payload.put("designerType", "approval");
        payload.put("formType", "none");
        payload.put("formId", null);
        payload.put("notifyType", "redis");
        payload.put("bpmnXml", bpmnXml);
        return payload;
    }

    private void validateBase(SamplePurchaseOrderDTO dto) {
        if (dto == null) {
            throw new BusinessException("采购单参数不能为空");
        }
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new BusinessException("采购主题不能为空");
        }
        if (!StringUtils.hasText(dto.getSupplierName())) {
            throw new BusinessException("供应商不能为空");
        }
        if (dto.getAmountCent() == null || dto.getAmountCent() <= 0) {
            throw new BusinessException("采购金额必须大于0");
        }
    }

    private void validateSubmit(SamplePurchaseOrderSubmitDTO dto) {
        if (dto == null) {
            throw new BusinessException("审批配置不能为空");
        }
        if (dto.getDeptLeaderId() == null) {
            throw new BusinessException("请选择部门负责人");
        }
        if (dto.getEngineeringManagerId() == null) {
            throw new BusinessException("请选择工程部经理");
        }
        if (dto.getCountersignUserIds() == null || dto.getCountersignUserIds().stream().filter(Objects::nonNull).distinct().count() < 2) {
            throw new BusinessException("会签节点至少选择2名人员");
        }
    }

    private void applyBaseFields(SamplePurchaseOrder entity, SamplePurchaseOrderDTO dto) {
        BeanUtils.copyProperties(dto, entity, "id", "status", "businessKey", "processInstanceId");
        if (StringUtils.hasText(dto.getTitle())) {
            entity.setTitle(dto.getTitle().trim());
        }
        if (StringUtils.hasText(dto.getSupplierName())) {
            entity.setSupplierName(dto.getSupplierName().trim());
        }
    }

    private SamplePurchaseOrder resolveTaskEntity(SamplePurchaseOrderTaskSaveDTO dto) {
        if (dto.getId() != null) {
            return requireEntity(dto.getId());
        }
        if (StringUtils.hasText(dto.getBusinessKey())) {
            SamplePurchaseOrder entity = getBaseMapper().selectByBusinessKey(resolveTenantId(), dto.getBusinessKey());
            if (entity != null) {
                return entity;
            }
        }
        throw new BusinessException("未找到待办关联的采购单");
    }

    private SamplePurchaseOrder requireEntity(Long id) {
        if (id == null) {
            throw new BusinessException("采购单ID不能为空");
        }
        SamplePurchaseOrder entity = getById(id);
        if (entity == null || !Objects.equals(entity.getTenantId(), resolveTenantId())) {
            throw new BusinessException("采购单不存在");
        }
        return entity;
    }

    private String buildBusinessKey(Long id) {
        return BUSINESS_TYPE + ":" + id;
    }

    private String generateOrderNo(Long id) {
        String suffix = String.valueOf(id);
        if (suffix.length() > 6) {
            suffix = suffix.substring(suffix.length() - 6);
        }
        return "PO" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + suffix;
    }

    private String resolveUsername() {
        return StringUtils.hasText(SessionHelper.getUsername()) ? SessionHelper.getUsername() : "system";
    }

    private Long resolveTenantId() {
        return resolveTenantId(null);
    }

    private Long resolveTenantId(Long eventTenantId) {
        if (eventTenantId != null) {
            return eventTenantId;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        tenantId = SessionHelper.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private List<String> normalizeRoleKeys(List<String> roleKeys) {
        List<String> source = roleKeys == null || roleKeys.isEmpty() ? List.of("admin") : roleKeys;
        Set<String> result = new LinkedHashSet<>();
        for (String roleKey : source) {
            if (StringUtils.hasText(roleKey)) {
                result.add(roleKey.trim());
            }
        }
        if (result.isEmpty()) {
            result.add("admin");
        }
        return new ArrayList<>(result);
    }

    private String joinLongs(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstText(String... values) {
        return Arrays.stream(values)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }
}
