package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowBindingDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowCallbackDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowResubmitDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowBindingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowRuntimeVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 业务流程服务。
 * <p>
 * 负责业务对象与流程引擎的动态集成：
 * - 读取 ai_business_binding (binding_type=FLOW) 中的流程绑定配置
 * - 从业务记录动态发起流程（不依赖硬编码注解）
 * - 处理流程回调，更新业务记录状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessFlowService {

    private static final String FLOW_START_LOCK_PREFIX = "forge:business-flow:start:";
    private static final long FLOW_START_LOCK_WAIT_SECONDS = 5L;

    @Autowired(required = false)
    private FlowClient flowClient;

    private final BusinessBindingMapper bindingMapper;
    private final BusinessFlowInstanceLinkMapper flowInstanceLinkMapper;
    private final AiCrudConfigMapper crudConfigMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessDocumentConfigService documentConfigService;
    private final BusinessDocumentRuntimeService documentRuntimeService;
    private final DynamicCrudService dynamicCrudService;
    private final BusinessFlowVariableResolver variableResolver;
    private final BusinessCodeFormProviderRegistry codeFormProviderRegistry;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final Map<String, ReentrantLock> localFlowStartLocks = new ConcurrentHashMap<>();

    /**
     * 从业务记录发起流程
     *
     * @param objectCode 业务对象编码
     * @param recordId   业务记录ID
     * @param recordData 业务记录数据
     * @return 流程发起结果
     */
    public JSONObject startFlow(String objectCode, String recordId, Map<String, Object> recordData) {
        if (flowClient == null) {
            throw new RuntimeException("流程服务未配置，无法发起主流程");
        }
        Long tenantId = resolveTenantId();

        // 1. 查询该对象的 FLOW 绑定配置
        AiBusinessBinding flowBinding = bindingMapper.selectBindingByTypeAndCode(
                tenantId, "OBJECT", objectCode, "FLOW");

        if (flowBinding == null || flowBinding.getBindingConfig() == null) {
            throw new RuntimeException("业务对象 [" + objectCode + "] 未配置流程绑定");
        }

        JSONObject bindingConfig = readBindingConfig(flowBinding.getBindingConfig());
        String flowModelKey = resolveFlowModelKey(bindingConfig);

        if (flowModelKey == null || flowModelKey.isBlank()) {
            throw new RuntimeException("流程绑定配置中缺少 flowModelKey");
        }

        // 2. 构建流程变量
        Map<String, Object> flowVariables = buildFlowVariables(bindingConfig, recordData);

        // 3. 构建业务Key和标题
        String businessKey = objectCode + ":" + recordId;
        String title = buildFlowTitle(bindingConfig, recordData, objectCode);

        // 4. 发起流程
        Long userId = resolveUserId();
        String userName = resolveUsername();

        FlowResult<String> result = flowClient.startProcess(
                flowModelKey, businessKey, title,
                flowVariables, String.valueOf(userId), userName, null, null);

        if (!result.isSuccess()) {
            throw new RuntimeException("流程发起失败: " + result.getMsg());
        }

        JSONObject response = new JSONObject();
        response.put("flowModelKey", flowModelKey);
        response.put("businessKey", businessKey);
        response.put("processInstanceId", result.getData());
        response.put("status", "STARTED");
        return response;
    }

    /**
     * 查询业务对象的流程绑定配置
     */
    public BusinessFlowBindingVO getFlowBinding(String objectCode) {
        Long tenantId = resolveTenantId();
        String canonicalObjectCode = resolveCanonicalObjectCode(tenantId, objectCode);
        AiBusinessBinding binding = selectMainFlowBindingForConfig(tenantId, canonicalObjectCode, objectCode);

        if (binding == null) {
            AiBusinessDocumentConfig documentConfig = resolveEnabledDocumentConfig(tenantId, canonicalObjectCode,
                    resolvePublishedRuntimeConfig(tenantId, objectCode));
            if (documentConfig == null || StringUtils.isBlank(documentConfig.getDefaultFlowKey())) {
                return null;
            }
            return legacyDocumentFlowToVO(canonicalObjectCode, documentConfig);
        }
        return toVO(canonicalObjectCode, binding);
    }

    /**
     * 查询流程模型变量候选项和字段映射建议。
     */
    public Map<String, Object> getVariableCandidates(String modelKey, String objectCode) {
        if (StringUtils.isBlank(modelKey)) {
            throw new BusinessException("流程模型Key不能为空");
        }
        return variableResolver.resolve(modelKey, objectCode);
    }

    /**
     * 查询业务对象可供流程节点绑定的表单资产。
     */
    public Map<String, Object> getFormAssets(String objectCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("objectCode", objectCode);
        result.put("formAssets", List.of());
        result.put("warnings", List.of());
        if (StringUtils.isBlank(objectCode)) {
            return result;
        }

        List<String> warnings = new ArrayList<>();
        BusinessObjectQueryDTO query = new BusinessObjectQueryDTO();
        query.setObjectCode(objectCode);
        List<BusinessObjectVO> objects = businessObjectMapper.selectObjectList(resolveTenantId(), query);
        if (objects == null || objects.isEmpty()) {
            warnings.add("业务对象不存在或无权限访问: " + objectCode);
            result.put("warnings", warnings);
            return result;
        }

        BusinessObjectVO object = objects.get(0);
        JSONObject designerOptions = readJsonObject(object.getDesignerOptions());
        JSONObject formSchema = readNestedObject(designerOptions.get("formDesignerSchema"));
        List<Map<String, Object>> assets = new ArrayList<>(collectBusinessFormAssets(object, formSchema));
        assets.addAll(codeFormProviderRegistry.listAssets(object.getObjectCode()));
        if (assets.isEmpty()) {
            warnings.add("业务对象尚未配置低代码表单资产");
        }
        result.put("objectId", object.getId());
        result.put("objectCode", object.getObjectCode());
        result.put("objectName", object.getObjectName());
        result.put("formAssets", assets);
        result.put("warnings", warnings);
        return result;
    }

    /**
     * 查询待办任务对应的业务表单上下文。
     */
    public BusinessTaskFormContextVO getTaskFormContext(BusinessTaskFormContextQueryDTO query) {
        BusinessTaskFormContextQueryDTO effectiveQuery = query == null ? new BusinessTaskFormContextQueryDTO() : query;
        validateTaskAccess(effectiveQuery, false);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(effectiveQuery, false);
        return buildTaskFormContext(effectiveQuery, runtime);
    }

    /**
     * 查询历史/已办场景下的业务表单上下文，只用于只读展示，不校验运行中待办任务身份。
     */
    public BusinessTaskFormContextVO getTaskFormReadonlyContext(BusinessTaskFormContextQueryDTO query) {
        BusinessTaskFormContextQueryDTO effectiveQuery = query == null ? new BusinessTaskFormContextQueryDTO() : query;
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(effectiveQuery, false);
        BusinessTaskFormContextVO context = buildTaskFormContext(effectiveQuery, runtime);
        makeBusinessTaskFormReadonly(context);
        return context;
    }

    /**
     * 保存待办任务允许编辑的业务字段，并返回最新上下文。
     */
    public BusinessTaskFormContextVO saveTaskFormContext(BusinessTaskFormSaveDTO dto) {
        if (dto == null) {
            throw new BusinessException("业务待办表单参数不能为空");
        }
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(dto.getTaskId());
        query.setBusinessKey(dto.getBusinessKey());
        query.setProcessInstanceId(dto.getProcessInstanceId());
        query.setProcessDefKey(dto.getProcessDefKey());
        query.setTaskDefKey(dto.getTaskDefKey());
        query.setObjectCode(dto.getObjectCode());
        query.setRecordId(dto.getRecordId());
        query.setFormKey(dto.getFormKey());

        validateTaskAccess(query, true);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(query, true);
        JSONObject nodeForm = findNodeForm(runtime.bindingConfig(), query.getTaskDefKey());
        if (nodeForm == null || nodeForm.isEmpty()) {
            throw new BusinessException("当前流程节点未配置业务表单权限");
        }
        String formMode = normalizeNodeFormMode(nodeForm.getString("formMode"));
        if ("BUSINESS_CODE_FORM".equals(formMode)) {
            return saveBusinessCodeFormContext(dto, nodeForm);
        }
        if (!"BUSINESS_OBJECT_FORM".equals(formMode)) {
            throw new BusinessException("当前节点不是平台可保存的业务表单，不能通过平台保存业务字段");
        }
        List<Map<String, Object>> permissions = normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
        Set<String> writableFields = collectPermissionFields(permissions, "writable", true);
        if (writableFields.isEmpty()) {
            throw new BusinessException("当前节点没有可编辑业务字段");
        }

        Map<String, Object> input = dto.getData() == null ? Map.of() : dto.getData();
        Map<String, Object> updateData = new LinkedHashMap<>();
        for (String field : writableFields) {
            if (input.containsKey(field)) {
                updateData.put(field, input.get(field));
            }
        }
        validateRequiredTaskFields(permissions, updateData, input);
        if (updateData.isEmpty()) {
            throw new BusinessException("未提交可编辑业务字段");
        }

        dynamicCrudService.updateInternalFieldsById(runtime.configKey(), runtime.recordId(), updateData);
        return buildTaskFormContext(query, runtime);
    }

    /**
     * 驳回修改后重提。复杂代码业务可先在业务页保存主数据，再调用该接口完成修改节点。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO resubmit(BusinessFlowResubmitDTO dto) {
        if (dto == null) {
            throw new BusinessException("重提参数不能为空");
        }
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(dto.getTaskId());
        query.setBusinessKey(dto.getBusinessKey());
        query.setProcessInstanceId(dto.getProcessInstanceId());
        query.setProcessDefKey(dto.getProcessDefKey());
        query.setTaskDefKey(dto.getTaskDefKey());

        validateTaskAccess(query, true);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(query, true);
        Map<String, Object> variables = dto.getVariables() == null ? Map.of() : dto.getVariables();
        FlowResult<Void> result = flowClient.approve(
                query.getTaskId(),
                String.valueOf(resolveUserId()),
                StringUtils.defaultIfBlank(dto.getComment(), "修改后重提"),
                variables);
        if (result == null || !result.isSuccess()) {
            throw new BusinessException(result == null ? "重提失败" : StringUtils.defaultIfBlank(result.getMsg(), "重提失败"));
        }

        AiBusinessFlowInstanceLink link = findRuntimeLink(resolveTenantId(), query.getProcessInstanceId(), runtime.businessKey());
        if (link == null) {
            BusinessFlowRuntimeVO vo = new BusinessFlowRuntimeVO();
            vo.setObjectCode(runtime.objectCode());
            vo.setRecordId(runtime.recordId());
            vo.setBusinessKey(runtime.businessKey());
            vo.setProcessInstanceId(query.getProcessInstanceId());
            vo.setFlowStatus("IN_PROCESS");
            vo.setMessage("已重提");
            return vo;
        }

        AiBusinessDocumentConfig documentConfig = documentConfigService.selectEnabledByObjectCode(
                link.getTenantId(), link.getObjectCode());
        AiCrudConfig runtimeConfig = documentConfig == null
                ? resolvePublishedRuntimeConfig(link.getTenantId(), link.getObjectCode())
                : null;
        AiBusinessBinding binding = selectMainFlowBindingForConfig(link.getTenantId(), link.getObjectCode());
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(bindingConfig, link.getTenantId(), link.getObjectCode());
        updateBusinessFlowStatus(documentConfig, runtimeConfig, bindingConfig, link.getRecordId(), "IN_PROCESS");

        link.setFlowStatus("IN_PROCESS");
        link.setResult(null);
        link.setEndTime(null);
        if (!variables.isEmpty()) {
            link.setVariablesSnapshot(JSON.toJSONString(variables));
        }
        flowInstanceLinkMapper.updateById(link);
        return toRuntimeVO(link, "已重提");
    }

    private void validateTaskAccess(BusinessTaskFormContextQueryDTO query, boolean writeRequired) {
        String taskId = StringUtils.trimToNull(query.getTaskId());
        if (taskId == null) {
            throw new BusinessException("任务ID不能为空");
        }
        query.setTaskId(taskId);
        if (flowClient == null) {
            throw new BusinessException("流程服务未配置，无法校验任务身份");
        }
        Long currentUserId = resolveUserId();
        if (currentUserId == null) {
            throw new BusinessException("当前登录用户不能为空");
        }

        Map<String, Object> task = loadFlowTaskDetail(taskId);
        Integer status = readIntegerValue(task.get("status"));
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("当前任务已处理，不能访问待办业务表单");
        }

        String currentUser = String.valueOf(currentUserId);
        String assignee = StringUtils.trimToNull(textValue(task.get("assignee")));
        boolean claimedByCurrentUser = StringUtils.equals(assignee, currentUser);
        boolean unclaimedCandidate = StringUtils.isBlank(assignee)
                && (csvContains(task.get("candidateUsers"), currentUser)
                || StringUtils.isNotBlank(textValue(task.get("candidateGroups"))));

        if (!claimedByCurrentUser && !unclaimedCandidate) {
            throw new BusinessException("无权访问当前任务业务表单");
        }
        if (writeRequired && !claimedByCurrentUser) {
            throw new BusinessException("请先签收任务后再保存业务字段");
        }

        assertTaskFieldMatches(query.getProcessInstanceId(), task.get("processInstanceId"), "流程实例");
        assertTaskFieldMatches(query.getBusinessKey(), task.get("businessKey"), "业务Key");
        assertTaskFieldMatches(query.getTaskDefKey(), task.get("taskDefKey"), "任务节点");
        assertTaskFieldMatches(query.getProcessDefKey(), task.get("processDefKey"), "流程定义");

        if (StringUtils.isBlank(query.getProcessInstanceId())) {
            query.setProcessInstanceId(StringUtils.trimToNull(textValue(task.get("processInstanceId"))));
        }
        if (StringUtils.isBlank(query.getBusinessKey())) {
            query.setBusinessKey(StringUtils.trimToNull(textValue(task.get("businessKey"))));
        }
        if (StringUtils.isBlank(query.getTaskDefKey())) {
            query.setTaskDefKey(StringUtils.trimToNull(textValue(task.get("taskDefKey"))));
        }
        if (StringUtils.isBlank(query.getProcessDefKey())) {
            query.setProcessDefKey(StringUtils.trimToNull(textValue(task.get("processDefKey"))));
        }
    }

    private Map<String, Object> loadFlowTaskDetail(String taskId) {
        try {
            FlowResult<Map<String, Object>> result = flowClient.getTaskDetail(taskId);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                String message = result == null ? null : result.getMsg();
                throw new BusinessException(StringUtils.defaultIfBlank(message, "任务不存在或无权访问"));
            }
            return result.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("业务待办表单任务身份校验失败: taskId={}, error={}", taskId, e.getMessage());
            throw new BusinessException("任务身份校验失败，请稍后重试");
        }
    }

    private void assertTaskFieldMatches(String requestedValue, Object actualValue, String label) {
        String requested = StringUtils.trimToNull(requestedValue);
        String actual = StringUtils.trimToNull(textValue(actualValue));
        if (requested != null && actual != null && !StringUtils.equals(requested, actual)) {
            throw new BusinessException(label + "与当前任务不匹配");
        }
    }

    private boolean csvContains(Object csvValue, String expected) {
        String csv = StringUtils.trimToNull(textValue(csvValue));
        if (csv == null || StringUtils.isBlank(expected)) {
            return false;
        }
        String[] parts = csv.split(",");
        for (String part : parts) {
            if (StringUtils.equals(StringUtils.trimToEmpty(part), expected)) {
                return true;
            }
        }
        return false;
    }

    private Integer readIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = StringUtils.trimToNull(String.valueOf(value));
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BusinessTaskFormContextVO buildTaskFormContext(BusinessTaskFormContextQueryDTO query,
                                                           TaskFormRuntimeContext runtime) {
        BusinessTaskFormContextVO vo = new BusinessTaskFormContextVO();
        vo.setTaskId(StringUtils.trimToNull(query.getTaskId()));
        vo.setBusinessKey(runtime.businessKey());
        vo.setProcessInstanceId(StringUtils.trimToNull(query.getProcessInstanceId()));
        vo.setProcessDefKey(StringUtils.trimToNull(query.getProcessDefKey()));
        vo.setTaskDefKey(StringUtils.trimToNull(query.getTaskDefKey()));
        vo.setObjectCode(runtime.objectCode());
        vo.setRecordId(runtime.recordId());
        vo.setConfigKey(runtime.configKey());
        vo.setFormType("none");

        if (StringUtils.isBlank(runtime.objectCode()) || runtime.recordId() == null || StringUtils.isBlank(runtime.configKey())) {
            vo.getWarnings().add("未解析到业务对象、记录或运行配置");
            return vo;
        }

        JSONObject nodeForm = findNodeForm(runtime.bindingConfig(), query.getTaskDefKey());
        if (nodeForm == null || nodeForm.isEmpty()) {
            vo.getWarnings().add("当前节点未配置业务表单策略");
            return vo;
        }
        String formMode = normalizeNodeFormMode(nodeForm.getString("formMode"));
        if (!"BUSINESS_OBJECT_FORM".equals(formMode)) {
            if ("BUSINESS_CODE_FORM".equals(formMode)) {
                return buildBusinessCodeFormContext(query, nodeForm, runtime);
            }
            vo.setFormType(formMode);
            vo.setFormKey(StringUtils.trimToNull(nodeForm.getString("formKey")));
            vo.setFormName(StringUtils.trimToNull(nodeForm.getString("formName")));
            vo.setProviderKey(StringUtils.trimToNull(nodeForm.getString("providerKey")));
            vo.setFormUrl(StringUtils.trimToNull(nodeForm.getString("formUrl")));
            vo.setEditMode(normalizeNodeEditMode(nodeForm.getString("editMode")));
            vo.setFormRef(readNestedObject(nodeForm.get("formRef")));
            vo.getWarnings().add("当前节点表单类型暂不由低代码业务表单渲染: " + formMode);
            return vo;
        }

        String formKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(query.getFormKey()),
                StringUtils.trimToNull(nodeForm.getString("formKey")));
        BusinessObjectVO object = queryBusinessObject(resolveTenantId(), runtime.objectCode());
        JSONObject formSchema = resolveBusinessFormSchema(object, formKey);
        if (formSchema.isEmpty()) {
            vo.getWarnings().add("未找到节点引用的低代码表单资产: " + formKey);
            return vo;
        }

        List<Map<String, Object>> fieldCatalog = collectBusinessFormFieldCatalog(formSchema);
        List<Map<String, Object>> permissions = normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
        List<Map<String, Object>> fields = buildTaskFormFields(fieldCatalog, permissions);
        Map<String, Object> recordData = dynamicCrudService.selectById(runtime.configKey(), runtime.recordId());
        Map<String, Object> visibleRecordData = filterVisibleRecordData(recordData, fields);

        vo.setConfigured(true);
        vo.setFormType("business-object");
        vo.setFormKey(StringUtils.firstNonBlank(formKey, formSchema.getString("formKey")));
        vo.setFormName(StringUtils.defaultIfBlank(nodeForm.getString("formName"), formSchema.getString("formName")));
        vo.setViewKey(StringUtils.defaultIfBlank(nodeForm.getString("viewKey"), "default"));
        vo.setEditMode(normalizeNodeEditMode(nodeForm.getString("editMode")));
        vo.setFormRef(readNestedObject(nodeForm.get("formRef")));
        vo.setFieldPermissions(permissions);
        vo.setFields(fields);
        vo.setRecordData(visibleRecordData);
        if (fields.isEmpty()) {
            vo.getWarnings().add("当前业务表单没有可展示字段");
        }
        return vo;
    }

    private void makeBusinessTaskFormReadonly(BusinessTaskFormContextVO context) {
        if (context == null) {
            return;
        }
        if (context.getFields() != null) {
            for (Map<String, Object> field : context.getFields()) {
                if (field == null) {
                    continue;
                }
                field.put("writable", false);
                field.put("readonly", true);
                field.put("disabled", true);
            }
        }
        if (context.getFieldPermissions() != null) {
            for (Map<String, Object> permission : context.getFieldPermissions()) {
                if (permission == null) {
                    continue;
                }
                permission.put("writable", false);
                permission.put("readonly", true);
                permission.put("disabled", true);
            }
        }
    }

    private BusinessTaskFormContextVO buildBusinessCodeFormContext(BusinessTaskFormContextQueryDTO query,
                                                                   JSONObject nodeForm,
                                                                   TaskFormRuntimeContext runtime) {
        JSONObject formRef = readNestedObject(nodeForm.get("formRef"));
        String providerKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("providerKey")),
                StringUtils.trimToNull(formRef.getString("providerKey")));
        List<Map<String, Object>> permissions = normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
        BusinessTaskFormContextVO fallback = buildBusinessCodeFormFallback(query, nodeForm, runtime, formRef, permissions, providerKey);
        if (StringUtils.isBlank(providerKey)) {
            fallback.getWarnings().add("当前代码表单缺少 providerKey，无法加载业务表单");
            return fallback;
        }
        return codeFormProviderRegistry.find(providerKey)
                .map(provider -> mergeBusinessCodeFormBase(provider.buildContext(query, new LinkedHashMap<>(formRef), permissions),
                        fallback))
                .orElseGet(() -> {
                    fallback.getWarnings().add("代码表单Provider未注册: " + providerKey);
                    return fallback;
                });
    }

    private BusinessTaskFormContextVO saveBusinessCodeFormContext(BusinessTaskFormSaveDTO dto, JSONObject nodeForm) {
        JSONObject formRef = readNestedObject(nodeForm.get("formRef"));
        String providerKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("providerKey")),
                StringUtils.trimToNull(formRef.getString("providerKey")));
        if (StringUtils.isBlank(providerKey)) {
            throw new BusinessException("当前代码表单缺少 providerKey，无法保存业务字段");
        }
        List<Map<String, Object>> permissions = normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
        return codeFormProviderRegistry.require(providerKey)
                .saveContext(dto, new LinkedHashMap<>(formRef), permissions);
    }

    private BusinessTaskFormContextVO buildBusinessCodeFormFallback(BusinessTaskFormContextQueryDTO query,
                                                                   JSONObject nodeForm,
                                                                   TaskFormRuntimeContext runtime,
                                                                   JSONObject formRef,
                                                                   List<Map<String, Object>> permissions,
                                                                   String providerKey) {
        BusinessTaskFormContextVO vo = new BusinessTaskFormContextVO();
        vo.setConfigured(true);
        vo.setFormType("business-code");
        vo.setTaskId(StringUtils.trimToNull(query.getTaskId()));
        vo.setBusinessKey(runtime.businessKey());
        vo.setProcessInstanceId(StringUtils.trimToNull(query.getProcessInstanceId()));
        vo.setProcessDefKey(StringUtils.trimToNull(query.getProcessDefKey()));
        vo.setTaskDefKey(StringUtils.trimToNull(query.getTaskDefKey()));
        vo.setObjectCode(runtime.objectCode());
        vo.setRecordId(runtime.recordId());
        vo.setConfigKey(runtime.configKey());
        vo.setFormKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("formKey")),
                StringUtils.trimToNull(formRef.getString("formKey"))));
        vo.setFormName(StringUtils.trimToNull(nodeForm.getString("formName")));
        vo.setProviderKey(providerKey);
        vo.setFormUrl(StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("formUrl")),
                StringUtils.trimToNull(formRef.getString("formUrl"))));
        vo.setViewKey(StringUtils.defaultIfBlank(nodeForm.getString("viewKey"), "default"));
        vo.setEditMode(normalizeNodeEditMode(nodeForm.getString("editMode")));
        vo.setFormRef(new LinkedHashMap<>(formRef));
        vo.setFieldPermissions(permissions);
        return vo;
    }

    private BusinessTaskFormContextVO mergeBusinessCodeFormBase(BusinessTaskFormContextVO source,
                                                               BusinessTaskFormContextVO fallback) {
        if (source == null) {
            return fallback;
        }
        if (source.getConfigured() == null) {
            source.setConfigured(true);
        }
        if (StringUtils.isBlank(source.getFormType())) {
            source.setFormType("business-code");
        }
        if (StringUtils.isBlank(source.getTaskId())) {
            source.setTaskId(fallback.getTaskId());
        }
        if (StringUtils.isBlank(source.getBusinessKey())) {
            source.setBusinessKey(fallback.getBusinessKey());
        }
        if (StringUtils.isBlank(source.getProcessInstanceId())) {
            source.setProcessInstanceId(fallback.getProcessInstanceId());
        }
        if (StringUtils.isBlank(source.getProcessDefKey())) {
            source.setProcessDefKey(fallback.getProcessDefKey());
        }
        if (StringUtils.isBlank(source.getTaskDefKey())) {
            source.setTaskDefKey(fallback.getTaskDefKey());
        }
        if (StringUtils.isBlank(source.getObjectCode())) {
            source.setObjectCode(fallback.getObjectCode());
        }
        if (source.getRecordId() == null) {
            source.setRecordId(fallback.getRecordId());
        }
        if (StringUtils.isBlank(source.getConfigKey())) {
            source.setConfigKey(fallback.getConfigKey());
        }
        if (StringUtils.isBlank(source.getFormKey())) {
            source.setFormKey(fallback.getFormKey());
        }
        if (StringUtils.isBlank(source.getFormName())) {
            source.setFormName(fallback.getFormName());
        }
        if (StringUtils.isBlank(source.getProviderKey())) {
            source.setProviderKey(fallback.getProviderKey());
        }
        if (StringUtils.isBlank(source.getFormUrl())) {
            source.setFormUrl(fallback.getFormUrl());
        }
        if (StringUtils.isBlank(source.getViewKey())) {
            source.setViewKey(fallback.getViewKey());
        }
        if (StringUtils.isBlank(source.getEditMode())) {
            source.setEditMode(fallback.getEditMode());
        }
        if (source.getFormRef() == null || source.getFormRef().isEmpty()) {
            source.setFormRef(fallback.getFormRef());
        }
        if (source.getFieldPermissions() == null || source.getFieldPermissions().isEmpty()) {
            source.setFieldPermissions(fallback.getFieldPermissions());
        }
        return source;
    }

    private TaskFormRuntimeContext resolveTaskFormRuntimeContext(BusinessTaskFormContextQueryDTO query, boolean strict) {
        Long tenantId = resolveTenantId();
        AiBusinessFlowInstanceLink link = null;
        if (StringUtils.isNotBlank(query.getProcessInstanceId())) {
            link = flowInstanceLinkMapper.selectByProcessInstanceId(tenantId, query.getProcessInstanceId());
        }
        if (link == null && StringUtils.isNotBlank(query.getBusinessKey())) {
            link = flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, query.getBusinessKey());
        }

        String objectCode = StringUtils.firstNonBlank(
                link == null ? null : link.getObjectCode(),
                StringUtils.trimToNull(query.getObjectCode()),
                parseBusinessKeyObjectCode(query.getBusinessKey()));
        Long recordId = link == null || link.getRecordId() == null
                ? query.getRecordId()
                : link.getRecordId();
        if (recordId == null) {
            recordId = parseBusinessKeyRecordId(query.getBusinessKey());
        }
        String businessKey = StringUtils.firstNonBlank(
                link == null ? null : link.getBusinessKey(),
                StringUtils.trimToNull(query.getBusinessKey()),
                objectCode != null && recordId != null ? buildBusinessKey(objectCode, recordId) : null);

        if (StringUtils.isBlank(objectCode) || recordId == null) {
            if (strict) {
                throw new BusinessException("未解析到业务对象或记录ID");
            }
            return new TaskFormRuntimeContext(null, null, businessKey, null, null);
        }

        String canonicalObjectCode = resolveCanonicalObjectCode(tenantId, objectCode);
        AiCrudConfig runtimeConfig = resolvePublishedRuntimeConfig(tenantId, canonicalObjectCode);
        AiBusinessDocumentConfig documentConfig = resolveEnabledDocumentConfig(tenantId, canonicalObjectCode, runtimeConfig);
        String configKey = documentConfig != null ? documentConfig.getConfigKey() : runtimeConfig == null ? null : runtimeConfig.getConfigKey();
        AiBusinessBinding binding = selectMainFlowBindingForConfig(tenantId, canonicalObjectCode, objectCode);
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(bindingConfig, tenantId, canonicalObjectCode);

        if (StringUtils.isBlank(configKey) && strict) {
            throw new BusinessException("业务对象缺少已发布运行配置，无法保存待办业务字段");
        }
        return new TaskFormRuntimeContext(canonicalObjectCode, recordId, businessKey, configKey, bindingConfig);
    }

    private BusinessObjectVO queryBusinessObject(Long tenantId, String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return null;
        }
        BusinessObjectQueryDTO query = new BusinessObjectQueryDTO();
        query.setObjectCode(objectCode);
        List<BusinessObjectVO> objects = businessObjectMapper.selectObjectList(tenantId, query);
        return objects == null || objects.isEmpty() ? null : objects.get(0);
    }

    private JSONObject findNodeForm(JSONObject bindingConfig, String taskDefKey) {
        JSONArray nodeForms = bindingConfig == null ? null : bindingConfig.getJSONArray("nodeForms");
        if (nodeForms == null || nodeForms.isEmpty() || StringUtils.isBlank(taskDefKey)) {
            return new JSONObject();
        }
        for (int i = 0; i < nodeForms.size(); i++) {
            JSONObject nodeForm = nodeForms.getJSONObject(i);
            if (nodeForm != null && taskDefKey.equals(nodeForm.getString("taskDefKey"))) {
                return nodeForm;
            }
        }
        return new JSONObject();
    }

    private JSONObject resolveBusinessFormSchema(BusinessObjectVO object, String formKey) {
        if (object == null) {
            return new JSONObject();
        }
        JSONObject designerOptions = readJsonObject(object.getDesignerOptions());
        JSONObject formSchema = readNestedObject(designerOptions.get("formDesignerSchema"));
        if (formSchema.isEmpty()) {
            return new JSONObject();
        }
        String targetFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formKey),
                StringUtils.trimToNull(formSchema.getString("defaultFormKey")),
                StringUtils.trimToNull(formSchema.getString("formKey")));

        JSONObject byForms = findFormSchemaInArray(readNestedArray(formSchema.get("forms")), targetFormKey);
        if (!byForms.isEmpty()) {
            return byForms;
        }
        JSONObject settings = readNestedObject(formSchema.get("settings"));
        JSONObject byAssets = findFormSchemaInArray(readNestedArray(settings.get("formAssets")), targetFormKey);
        if (!byAssets.isEmpty()) {
            return byAssets;
        }
        String rootFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formSchema.getString("formKey")),
                StringUtils.trimToNull(formSchema.getString("defaultFormKey")));
        if (StringUtils.isBlank(targetFormKey) || StringUtils.equals(targetFormKey, rootFormKey)) {
            return formSchema;
        }
        return new JSONObject();
    }

    private JSONObject findFormSchemaInArray(JSONArray forms, String formKey) {
        if (forms == null || forms.isEmpty() || StringUtils.isBlank(formKey)) {
            return new JSONObject();
        }
        for (int i = 0; i < forms.size(); i++) {
            JSONObject form = forms.getJSONObject(i);
            if (form == null) {
                continue;
            }
            JSONObject schema = readNestedObject(form.get("schema"));
            JSONObject candidate = schema.isEmpty() ? form : schema;
            String candidateKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(form.getString("formKey")),
                    StringUtils.trimToNull(candidate.getString("formKey")),
                    StringUtils.trimToNull(candidate.getString("defaultFormKey")));
            if (StringUtils.equals(formKey, candidateKey)) {
                return candidate;
            }
        }
        return new JSONObject();
    }

    private List<Map<String, Object>> buildTaskFormFields(List<Map<String, Object>> fieldCatalog,
                                                          List<Map<String, Object>> permissions) {
        Map<String, Map<String, Object>> permissionMap = new LinkedHashMap<>();
        for (Map<String, Object> permission : permissions) {
            String field = StringUtils.trimToNull(textValue(permission.get("field")));
            if (field != null) {
                permissionMap.put(field, permission);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> field : fieldCatalog) {
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(field.get("field"))),
                    StringUtils.trimToNull(textValue(field.get("fieldCode"))));
            if (fieldCode == null) {
                continue;
            }
            Map<String, Object> permission = permissionMap.get(fieldCode);
            boolean readable = permission == null || readBooleanValue(permission.get("readable"), true);
            if (!readable) {
                continue;
            }
            boolean writable = permission != null && readBooleanValue(permission.get("writable"), false);
            boolean required = writable && permission != null && readBooleanValue(permission.get("required"), false);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", fieldCode);
            item.put("label", StringUtils.defaultIfBlank(textValue(field.get("label")), fieldCode));
            item.put("type", normalizeTaskFormFieldType(textValue(field.get("componentType"))));
            item.put("componentType", StringUtils.trimToEmpty(textValue(field.get("componentType"))));
            item.put("dataType", StringUtils.trimToEmpty(textValue(field.get("dataType"))));
            putIfText(item, "dictType", field.get("dictType"));
            item.put("readable", true);
            item.put("writable", writable);
            item.put("required", required);
            item.put("readonly", !writable);
            item.put("disabled", !writable);
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("disabled", !writable);
            if (item.get("dictType") != null) {
                props.put("dictType", item.get("dictType"));
            }
            item.put("props", props);
            result.add(item);
        }
        return result;
    }

    private String normalizeTaskFormFieldType(String componentType) {
        String type = StringUtils.defaultIfBlank(componentType, "input").trim();
        return switch (type) {
            case "textarea" -> "textarea";
            case "inputNumber", "integer", "decimal", "money", "number" -> "number";
            case "dictSelect" -> "dictSelect";
            case "select", "radio", "checkbox", "date", "datetime", "switch", "imageUpload", "fileUpload" -> type;
            case "upload" -> "fileUpload";
            default -> "input";
        };
    }

    private Map<String, Object> filterVisibleRecordData(Map<String, Object> recordData, List<Map<String, Object>> fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (recordData == null || fields == null) {
            return result;
        }
        for (Map<String, Object> field : fields) {
            String fieldCode = StringUtils.trimToNull(textValue(field.get("field")));
            if (fieldCode != null) {
                result.put(fieldCode, readRecordValue(recordData, fieldCode));
            }
        }
        return result;
    }

    private Set<String> collectPermissionFields(List<Map<String, Object>> permissions, String permissionKey, boolean expected) {
        Set<String> result = new LinkedHashSet<>();
        if (permissions == null) {
            return result;
        }
        for (Map<String, Object> permission : permissions) {
            if (readBooleanValue(permission.get(permissionKey), false) == expected) {
                String field = StringUtils.trimToNull(textValue(permission.get("field")));
                if (field != null) {
                    result.add(field);
                }
            }
        }
        return result;
    }

    private void validateRequiredTaskFields(List<Map<String, Object>> permissions,
                                            Map<String, Object> updateData,
                                            Map<String, Object> input) {
        if (permissions == null) {
            return;
        }
        for (Map<String, Object> permission : permissions) {
            boolean required = readBooleanValue(permission.get("required"), false);
            boolean writable = readBooleanValue(permission.get("writable"), false);
            if (!required || !writable) {
                continue;
            }
            String field = StringUtils.trimToNull(textValue(permission.get("field")));
            if (field == null) {
                continue;
            }
            if (!input.containsKey(field)) {
                throw new BusinessException("请填写必填字段: " + field);
            }
            Object value = updateData.get(field);
            if (value == null || StringUtils.isBlank(String.valueOf(value))) {
                throw new BusinessException("请填写必填字段: " + field);
            }
        }
    }

    private String parseBusinessKeyObjectCode(String businessKey) {
        if (StringUtils.isBlank(businessKey) || !businessKey.contains(":")) {
            return null;
        }
        return StringUtils.trimToNull(businessKey.split(":", 2)[0]);
    }

    private Long parseBusinessKeyRecordId(String businessKey) {
        if (StringUtils.isBlank(businessKey) || !businessKey.contains(":")) {
            return null;
        }
        String value = StringUtils.trimToNull(businessKey.split(":", 2)[1]);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 旧触发器路径兼容返回，保留 config 包装结构。
     */
    public JSONObject getFlowBindingLegacy(String objectCode) {
        BusinessFlowBindingVO binding = getFlowBinding(objectCode);
        if (binding == null) {
            return null;
        }
        JSONObject result = new JSONObject();
        result.put("bindingId", binding.getBindingId());
        result.put("bindingName", StringUtils.defaultIfBlank(binding.getFlowModelName(), binding.getFlowModelKey()));
        result.put("objectCode", binding.getObjectCode());
        result.put("status", binding.getStatus());
        result.put("config", toConfigJson(binding));
        return result;
    }

    /**
     * 保存流程绑定配置
     */
    public void saveFlowBinding(String objectCode, BusinessFlowBindingDTO dto) {
        if (dto == null) {
            throw new BusinessException("流程绑定配置不能为空");
        }
        Long tenantId = resolveTenantId();
        String canonicalObjectCode = resolveCanonicalObjectCode(tenantId, objectCode);
        JSONObject config = normalizeBindingConfig(dto);
        ensureBusinessBinding(config, tenantId, canonicalObjectCode);
        String flowModelKey = config.getString("flowModelKey");
        if (StringUtils.isBlank(flowModelKey)) {
            throw new BusinessException("流程模型Key不能为空");
        }
        AiBusinessBinding existing = bindingMapper.selectBindingByTypeAndCode(
                tenantId, "OBJECT", canonicalObjectCode, "FLOW");

        if (existing != null) {
            existing.setTargetType("OBJECT");
            existing.setTargetCode(canonicalObjectCode);
            existing.setBindingType("FLOW");
            existing.setBindingConfig(config.toJSONString());
            existing.setBindingKey(flowModelKey);
            existing.setBindingName(resolveBindingName(config));
            existing.setStatus(1);
            bindingMapper.updateById(existing);
            log.info("[低代码流程绑定] 更新主流程绑定: tenantId={}, objectCode={}, bindingId={}, flowModelKey={}",
                    tenantId, objectCode, existing.getId(), flowModelKey);
        } else {
            AiBusinessBinding binding = new AiBusinessBinding();
            binding.setTenantId(tenantId);
            binding.setTargetType("OBJECT");
            binding.setTargetCode(canonicalObjectCode);
            binding.setBindingType("FLOW");
            binding.setBindingKey(flowModelKey);
            binding.setBindingName(resolveBindingName(config));
            binding.setBindingConfig(config.toJSONString());
            binding.setStatus(1);
            binding.setSortOrder(0);
            bindingMapper.insert(binding);
            log.info("[低代码流程绑定] 创建主流程绑定: tenantId={}, objectCode={}, bindingId={}, flowModelKey={}",
                    tenantId, objectCode, binding.getId(), flowModelKey);
        }
        documentConfigService.syncDefaultFlowKeyByObjectCode(tenantId, canonicalObjectCode, flowModelKey);
    }

    /**
     * 旧触发器路径保存兼容，读取 field/variable 后只落 formField/flowVariable。
     */
    public void saveFlowBinding(String objectCode, JSONObject config) {
        saveFlowBinding(objectCode, toDTO(config));
    }

    /**
     * 手动按钮发起单据流程。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startDocumentFlow(BusinessFlowStartDTO dto) {
        Long tenantId = resolveTenantId();
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> startDocumentFlowInternal(dto, true, null, null, tenantId));
    }

    /**
     * 旧审批入口兼容发起。接口层仍有旧权限校验，这里不再重复要求新流程按钮权限。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startDocumentFlowForCompatibility(BusinessFlowStartDTO dto) {
        Long tenantId = resolveTenantId();
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> startDocumentFlowInternal(dto, false, null, null, tenantId));
    }

    /**
     * 由触发器调用的流程发起（内部方法）。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startFlowFromTrigger(String flowModelKey, String businessKey, String title,
                                                      Long userId, String userName, JSONObject variables) {
        return startFlowFromTrigger(flowModelKey, businessKey, title, userId, userName, resolveTenantId(), variables);
    }

    /**
     * 由触发器调用的流程发起（内部方法）。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startFlowFromTrigger(String flowModelKey, String businessKey, String title,
                                                      Long userId, String userName, Long tenantId, JSONObject variables) {
        BusinessKeyParts parts = parseBusinessKey(businessKey);
        BusinessFlowStartDTO dto = new BusinessFlowStartDTO();
        dto.setObjectCode(parts.objectCode());
        dto.setRecordId(parts.recordId());
        dto.setFlowModelKey(flowModelKey);
        dto.setTitle(title);
        if (variables != null) {
            dto.setVariables(new LinkedHashMap<>(variables));
        }
        Long effectiveTenantId = tenantId != null ? tenantId : resolveTenantId();
        return TenantContextHolder.executeWithTenant(effectiveTenantId,
                () -> startDocumentFlowInternal(dto, false, userId, userName, effectiveTenantId));
    }

    /**
     * 查询单据流程状态。
     */
    public BusinessFlowRuntimeVO getFlowStatus(String objectCode, Long recordId) {
        if (StringUtils.isBlank(objectCode)) {
            throw new BusinessException("业务对象编码不能为空");
        }
        if (recordId == null) {
            throw new BusinessException("记录ID不能为空");
        }
        String canonicalObjectCode = resolveCanonicalObjectCode(resolveTenantId(), objectCode);
        String businessKey = buildBusinessKey(canonicalObjectCode, recordId);
        AiBusinessFlowInstanceLink link = flowInstanceLinkMapper.selectLatestByBusinessKey(resolveTenantId(), businessKey);
        if (link == null) {
            BusinessFlowRuntimeVO vo = new BusinessFlowRuntimeVO();
            vo.setObjectCode(canonicalObjectCode);
            vo.setRecordId(recordId);
            vo.setBusinessKey(businessKey);
            vo.setFlowStatus("NOT_STARTED");
            vo.setMessage("尚未发起主流程");
            return vo;
        }
        return toRuntimeVO(link, null);
    }

    /**
     * 处理流程引擎回调，按流程结果回写单据状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleFlowCallback(BusinessFlowCallbackDTO dto) {
        if (dto == null || (StringUtils.isBlank(dto.getProcessInstanceId()) && StringUtils.isBlank(dto.getBusinessKey()))) {
            throw new BusinessException("流程回调缺少流程实例ID或业务Key");
        }
        Long tenantId = dto.getTenantId() != null ? dto.getTenantId() : resolveTenantId();
        AiBusinessFlowInstanceLink link = findCallbackLink(tenantId, dto);
        if (link == null) {
            throw new BusinessException("未找到流程实例关联");
        }
        Long effectiveTenantId = link.getTenantId() != null ? link.getTenantId() : tenantId;
        TenantContextHolder.executeWithTenant(effectiveTenantId, () -> handleFlowCallbackInternal(link, dto));
    }

    private BusinessFlowRuntimeVO startDocumentFlowInternal(BusinessFlowStartDTO dto,
                                                            boolean checkPermission,
                                                            Long starterUserId,
                                                            String starterUserName,
                                                            Long tenantId) {
        if (dto == null) {
            throw new BusinessException("发起主流程参数不能为空");
        }
        if (StringUtils.isBlank(dto.getObjectCode())) {
            throw new BusinessException("业务对象编码不能为空");
        }
        if (dto.getRecordId() == null) {
            throw new BusinessException("请先保存记录后再发起主流程");
        }
        if (flowClient == null) {
            throw new BusinessException("流程服务未配置，无法发起主流程");
        }

        FlowStartContext startContext = resolveFlowStartContext(tenantId, dto.getObjectCode());
        AiBusinessDocumentConfig documentConfig = startContext.documentConfig();
        AiCrudConfig runtimeConfig = startContext.runtimeConfig();
        String objectCode = startContext.objectCode();
        String configKey = startContext.configKey();
        Map<String, Object> recordData = dynamicCrudService.selectById(configKey, dto.getRecordId());
        if (recordData == null) {
            throw new BusinessException("记录不存在或无权限访问");
        }

        String businessKey = buildBusinessKey(objectCode, dto.getRecordId());
        return executeWithFlowStartLock(tenantId, businessKey, () -> startDocumentFlowLocked(
                dto, checkPermission, starterUserId, starterUserName, tenantId, documentConfig,
                runtimeConfig, objectCode, configKey, recordData, businessKey, startContext.requestedObjectCode()));
    }

    private BusinessFlowRuntimeVO startDocumentFlowLocked(BusinessFlowStartDTO dto,
                                                          boolean checkPermission,
                                                          Long starterUserId,
                                                          String starterUserName,
                                                          Long tenantId,
                                                          AiBusinessDocumentConfig documentConfig,
                                                          AiCrudConfig runtimeConfig,
                                                          String objectCode,
                                                          String configKey,
                                                          Map<String, Object> recordData,
                                                          String businessKey,
                                                          String requestedObjectCode) {
        if (documentConfig != null) {
            documentRuntimeService.validateStartAllowed(objectCode, dto.getRecordId(), checkPermission);
        }
        AiBusinessFlowInstanceLink runningLink = flowInstanceLinkMapper.selectRunningByBusinessKey(tenantId, businessKey);
        if (runningLink != null) {
            return toRuntimeVO(runningLink, "当前单据已有流转中的流程");
        }

        AiBusinessBinding binding = selectFlowBindingForStart(tenantId, objectCode, requestedObjectCode);
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(bindingConfig, tenantId, objectCode);
        String dtoFlowModelKey = StringUtils.trimToNull(dto.getFlowModelKey());
        String bindingConfigFlowModelKey = resolveFlowModelKey(bindingConfig);
        String bindingKey = binding == null ? null : StringUtils.trimToNull(binding.getBindingKey());
        String documentDefaultFlowKey = documentConfig == null ? null : StringUtils.trimToNull(documentConfig.getDefaultFlowKey());
        String flowModelKey = StringUtils.firstNonBlank(
                dtoFlowModelKey,
                bindingConfigFlowModelKey,
                bindingKey,
                documentDefaultFlowKey);
        if (StringUtils.isBlank(flowModelKey)) {
            log.warn("[低代码流程启动] 主流程解析失败: tenantId={}, objectCode={}, recordId={}, checkPermission={}, " +
                            "configKey={}, documentConfigId={}, documentEnabled={}, documentDefaultFlowKey={}, " +
                            "runtimeConfigId={}, runtimeConfigKey={}, binding={}, dtoFlowModelKey={}, " +
                            "bindingConfigFlowModelKey={}, bindingConfigPreview={}",
                    tenantId, objectCode, dto.getRecordId(), checkPermission, configKey,
                    documentConfig == null ? null : documentConfig.getId(),
                    documentConfig == null ? null : documentConfig.getDocumentEnabled(),
                    documentDefaultFlowKey,
                    runtimeConfig == null ? null : runtimeConfig.getId(),
                    runtimeConfig == null ? null : runtimeConfig.getConfigKey(),
                    describeBinding(binding), dtoFlowModelKey, bindingConfigFlowModelKey, previewBindingConfig(bindingConfig));
            throw new BusinessException("请先在流程与自动化中配置主流程");
        }
        log.info("[低代码流程启动] 主流程解析成功: tenantId={}, objectCode={}, recordId={}, configKey={}, " +
                        "flowModelKey={}, bindingId={}, bindingType={}",
                tenantId, objectCode, dto.getRecordId(), configKey, flowModelKey,
                binding == null ? null : binding.getId(), binding == null ? null : binding.getBindingType());

        Map<String, Object> flowVariables = buildFlowVariables(bindingConfig, recordData);
        if (dto.getVariables() != null) {
            flowVariables.putAll(dto.getVariables());
        }
        flowVariables.putIfAbsent("objectCode", objectCode);
        flowVariables.putIfAbsent("configKey", configKey);
        flowVariables.putIfAbsent("recordId", dto.getRecordId());
        flowVariables.putIfAbsent("businessKey", businessKey);

        String title = StringUtils.defaultIfBlank(dto.getTitle(), buildFlowTitle(bindingConfig, recordData, objectCode));
        Long userId = starterUserId != null ? starterUserId : resolveUserId();
        String userName = StringUtils.defaultIfBlank(starterUserName, resolveUsername());
        FlowResult<String> result = flowClient.startProcess(
                flowModelKey, businessKey, title, flowVariables,
                userId == null ? null : String.valueOf(userId), userName, null, null);
        if (result == null || !result.isSuccess() || StringUtils.isBlank(result.getData())) {
            throw new BusinessException("流程发起失败: " + (result == null ? "无返回结果" : result.getMsg()));
        }

        AiBusinessFlowInstanceLink link = new AiBusinessFlowInstanceLink();
        link.setTenantId(tenantId);
        link.setObjectCode(objectCode);
        link.setRecordId(dto.getRecordId());
        link.setBusinessKey(businessKey);
        link.setFlowModelKey(flowModelKey);
        link.setProcessInstanceId(result.getData());
        link.setFlowStatus("RUNNING");
        link.setStartUserId(userId);
        link.setStartTime(LocalDateTime.now());
        link.setVariablesSnapshot(JSON.toJSONString(flowVariables));
        flowInstanceLinkMapper.insert(link);

        updateBusinessFlowStatus(documentConfig, runtimeConfig, bindingConfig, dto.getRecordId(), "IN_PROCESS");
        return toRuntimeVO(link, "流程已发起");
    }

    private BusinessFlowRuntimeVO executeWithFlowStartLock(Long tenantId,
                                                           String businessKey,
                                                           Supplier<BusinessFlowRuntimeVO> supplier) {
        String lockKey = buildFlowStartLockKey(tenantId, businessKey);
        FlowStartLockHandle lockHandle = acquireFlowStartLock(lockKey);
        boolean unlockInFinally = true;
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                unlockInFinally = false;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        unlockFlowStartLock(lockHandle);
                    }
                });
            }
            return supplier.get();
        } finally {
            if (unlockInFinally) {
                unlockFlowStartLock(lockHandle);
            }
        }
    }

    private FlowStartLockHandle acquireFlowStartLock(String lockKey) {
        RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
        if (redissonClient != null) {
            RLock lock = redissonClient.getLock(lockKey);
            try {
                if (!lock.tryLock(FLOW_START_LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
                    throw new BusinessException("流程正在发起，请勿重复提交");
                }
                return new FlowStartLockHandle(lockKey, lock, null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("流程发起锁等待被中断，请稍后重试");
            }
        }

        ReentrantLock localLock = localFlowStartLocks.computeIfAbsent(lockKey, key -> new ReentrantLock());
        try {
            if (!localLock.tryLock(FLOW_START_LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new BusinessException("流程正在发起，请勿重复提交");
            }
            return new FlowStartLockHandle(lockKey, null, localLock);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("流程发起锁等待被中断，请稍后重试");
        }
    }

    private void unlockFlowStartLock(FlowStartLockHandle lockHandle) {
        if (lockHandle == null) {
            return;
        }
        RLock redissonLock = lockHandle.redissonLock();
        if (redissonLock != null) {
            try {
                if (redissonLock.isHeldByCurrentThread()) {
                    redissonLock.unlock();
                }
            } catch (Exception e) {
                log.warn("[低代码流程启动] 释放流程发起分布式锁失败: lockKey={}, error={}",
                        lockHandle.lockKey(), e.getMessage());
            }
            return;
        }

        ReentrantLock localLock = lockHandle.localLock();
        if (localLock != null && localLock.isHeldByCurrentThread()) {
            localLock.unlock();
            if (!localLock.isLocked() && !localLock.hasQueuedThreads()) {
                localFlowStartLocks.remove(lockHandle.lockKey(), localLock);
            }
        }
    }

    private String buildFlowStartLockKey(Long tenantId, String businessKey) {
        return FLOW_START_LOCK_PREFIX
                + safeLockToken(tenantId) + ":"
                + safeLockToken(businessKey);
    }

    private String safeLockToken(Object value) {
        if (value == null) {
            return "null";
        }
        return String.valueOf(value).replaceAll("[^A-Za-z0-9:_-]", "_");
    }

    private void handleFlowCallbackInternal(AiBusinessFlowInstanceLink link, BusinessFlowCallbackDTO dto) {
        if (isEndedLink(link)) {
            log.info("流程回调已处理，跳过重复回调: processInstanceId={}, result={}",
                    link.getProcessInstanceId(), link.getResult());
            return;
        }
        AiBusinessDocumentConfig documentConfig = documentConfigService.selectEnabledByObjectCode(
                link.getTenantId(), link.getObjectCode());
        AiCrudConfig runtimeConfig = documentConfig == null
                ? resolvePublishedRuntimeConfig(link.getTenantId(), link.getObjectCode())
                : null;
        AiBusinessBinding binding = selectMainFlowBindingForConfig(link.getTenantId(), link.getObjectCode());
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(bindingConfig, link.getTenantId(), link.getObjectCode());
        String configKey = documentConfig != null ? documentConfig.getConfigKey() : runtimeConfig == null ? null : runtimeConfig.getConfigKey();
        Map<String, Object> previousData = StringUtils.isBlank(configKey)
                ? null
                : dynamicCrudService.selectById(configKey, link.getRecordId());
        String result = normalizeCallbackResult(dto);
        updateBusinessFlowStatus(documentConfig, runtimeConfig, bindingConfig, link.getRecordId(), result);

        link.setFlowStatus(result);
        link.setResult(result);
        link.setEndTime(LocalDateTime.now());
        if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
            link.setVariablesSnapshot(JSON.toJSONString(dto.getVariables()));
        }
        flowInstanceLinkMapper.updateById(link);

        Map<String, Object> currentData = StringUtils.isBlank(configKey)
                ? null
                : dynamicCrudService.selectById(configKey, link.getRecordId());
        if (documentConfig != null) {
            publishFlowResultEvent(link, documentConfig, result, previousData, currentData, dto);
        } else if (runtimeConfig != null) {
            publishFlowResultEvent(link, runtimeConfig, result, previousData, currentData, dto);
        }
    }

    private String resolveStartConfigKey(AiBusinessDocumentConfig documentConfig, AiCrudConfig runtimeConfig) {
        if (documentConfig != null) {
            if (StringUtils.isBlank(documentConfig.getConfigKey())) {
                throw new BusinessException("单据缺少发布配置，无法发起主流程");
            }
            return documentConfig.getConfigKey();
        }
        if (runtimeConfig == null || StringUtils.isBlank(runtimeConfig.getConfigKey())) {
            throw new BusinessException("业务对象缺少已发布运行配置，无法发起主流程");
        }
        return runtimeConfig.getConfigKey();
    }

    private AiBusinessFlowInstanceLink findCallbackLink(Long tenantId, BusinessFlowCallbackDTO dto) {
        if (StringUtils.isNotBlank(dto.getProcessInstanceId())) {
            AiBusinessFlowInstanceLink link = flowInstanceLinkMapper.selectByProcessInstanceId(
                    tenantId, dto.getProcessInstanceId());
            if (link != null) {
                return link;
            }
        }
        if (StringUtils.isNotBlank(dto.getBusinessKey())) {
            return flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, dto.getBusinessKey());
        }
        return null;
    }

    private AiBusinessFlowInstanceLink findRuntimeLink(Long tenantId, String processInstanceId, String businessKey) {
        if (StringUtils.isNotBlank(processInstanceId)) {
            AiBusinessFlowInstanceLink link = flowInstanceLinkMapper.selectByProcessInstanceId(tenantId, processInstanceId);
            if (link != null) {
                return link;
            }
        }
        if (StringUtils.isNotBlank(businessKey)) {
            return flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, businessKey);
        }
        return null;
    }

    private void updateDocumentStatus(AiBusinessDocumentConfig config, Long recordId, String statusKey) {
        if (StringUtils.isBlank(config.getStatusField())) {
            throw new BusinessException("单据状态字段未配置");
        }
        if (StringUtils.isBlank(config.getConfigKey())) {
            throw new BusinessException("单据缺少动态运行配置，无法更新状态");
        }
        String statusValue = resolveDocumentStatusValue(config, statusKey);
        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put(config.getStatusField(), statusValue);
        dynamicCrudService.updateInternalFieldsById(config.getConfigKey(), recordId, updateData);
    }

    private void updateBusinessFlowStatus(AiBusinessDocumentConfig documentConfig,
                                          AiCrudConfig runtimeConfig,
                                          JSONObject bindingConfig,
                                          Long recordId,
                                          String statusKey) {
        if (documentConfig != null) {
            updateDocumentStatus(documentConfig, recordId, statusKey);
            return;
        }
        BusinessFlowBindingDTO.BusinessBindingDTO businessBinding = toBusinessBindingDTO(
                bindingConfig == null ? null : bindingConfig.getJSONObject("businessBinding"));
        if (businessBinding == null || StringUtils.isBlank(businessBinding.getStatusField())) {
            return;
        }
        String mode = normalizeBusinessBindingMode(businessBinding.getMode());
        if ("ADAPTER".equals(mode)) {
            log.debug("[低代码流程状态] Adapter 模式跳过平台直接回写: recordId={}, status={}", recordId, statusKey);
            return;
        }
        if (runtimeConfig == null || StringUtils.isBlank(runtimeConfig.getConfigKey())) {
            throw new BusinessException("业务表绑定缺少低代码运行配置，无法更新流程状态");
        }
        validateBusinessBindingRuntimeTable(businessBinding, runtimeConfig);
        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put(businessBinding.getStatusField(), resolveBusinessBindingStatusValue(bindingConfig, statusKey));
        dynamicCrudService.updateInternalFieldsById(runtimeConfig.getConfigKey(), recordId, updateData);
    }

    private String resolveBusinessBindingStatusValue(JSONObject bindingConfig, String statusKey) {
        JSONObject document = bindingConfig == null ? null : bindingConfig.getJSONObject("document");
        JSONObject statusMapping = document == null ? null : document.getJSONObject("statusMapping");
        if (statusMapping != null) {
            return StringUtils.defaultIfBlank(statusMapping.getString(statusKey), statusKey);
        }
        return statusKey;
    }

    private void validateBusinessBindingRuntimeTable(BusinessFlowBindingDTO.BusinessBindingDTO businessBinding,
                                                     AiCrudConfig runtimeConfig) {
        String bindingTable = StringUtils.trimToNull(businessBinding.getTableName());
        if (bindingTable == null) {
            return;
        }
        String runtimeTable = StringUtils.firstNonBlank(runtimeConfig.getRuntimeTableName(), runtimeConfig.getTableName());
        if (StringUtils.isNotBlank(runtimeTable) && !bindingTable.equalsIgnoreCase(runtimeTable)) {
            throw new BusinessException("业务表绑定与发布运行表不一致，禁止直接回写状态");
        }
    }

    private String resolveDocumentStatusValue(AiBusinessDocumentConfig config, String statusKey) {
        Map<String, String> statusMapping = documentConfigService.toVO(config).getStatusMapping();
        return StringUtils.defaultIfBlank(statusMapping.get(statusKey), statusKey);
    }

    private void publishFlowResultEvent(AiBusinessFlowInstanceLink link,
                                        AiBusinessDocumentConfig config,
                                        String result,
                                        Map<String, Object> previousData,
                                        Map<String, Object> currentData,
                                        BusinessFlowCallbackDTO dto) {
        String eventType = switch (result) {
            case "APPROVED" -> BusinessEvent.FLOW_APPROVED;
            case "REJECTED" -> BusinessEvent.FLOW_REJECTED;
            case "CANCELED" -> BusinessEvent.FLOW_CANCELED;
            default -> null;
        };
        if (eventType == null) {
            return;
        }
        BusinessEvent event = BusinessEvent.builder()
                .eventType(eventType)
                .suiteCode(config.getSuiteCode())
                .objectCode(link.getObjectCode())
                .configKey(config.getConfigKey())
                .recordId(String.valueOf(link.getRecordId()))
                .recordData(currentData)
                .previousData(previousData)
                .operatorId(dto.getOperatorId() != null ? dto.getOperatorId() : link.getStartUserId())
                .operatorName(resolveUsername())
                .tenantId(link.getTenantId())
                .build();
        applicationEventPublisher.publishEvent(event);
    }

    private void publishFlowResultEvent(AiBusinessFlowInstanceLink link,
                                        AiCrudConfig config,
                                        String result,
                                        Map<String, Object> previousData,
                                        Map<String, Object> currentData,
                                        BusinessFlowCallbackDTO dto) {
        String eventType = switch (result) {
            case "APPROVED" -> BusinessEvent.FLOW_APPROVED;
            case "REJECTED" -> BusinessEvent.FLOW_REJECTED;
            case "CANCELED" -> BusinessEvent.FLOW_CANCELED;
            default -> null;
        };
        if (eventType == null) {
            return;
        }
        BusinessEvent event = BusinessEvent.builder()
                .eventType(eventType)
                .objectCode(link.getObjectCode())
                .configKey(config.getConfigKey())
                .recordId(String.valueOf(link.getRecordId()))
                .recordData(currentData)
                .previousData(previousData)
                .operatorId(dto.getOperatorId() != null ? dto.getOperatorId() : link.getStartUserId())
                .operatorName(resolveUsername())
                .tenantId(link.getTenantId())
                .build();
        applicationEventPublisher.publishEvent(event);
    }

    private AiCrudConfig resolvePublishedRuntimeConfig(Long tenantId, String objectCodeOrConfigKey) {
        if (StringUtils.isBlank(objectCodeOrConfigKey)) {
            return null;
        }
        return crudConfigMapper.selectPublishedByObjectCodeOrConfigKey(
                tenantId != null ? tenantId : resolveTenantId(), objectCodeOrConfigKey);
    }

    private String normalizeCallbackResult(BusinessFlowCallbackDTO dto) {
        String value = StringUtils.firstNonBlank(dto.getResult(), dto.getFlowStatus());
        if (StringUtils.isBlank(value)) {
            throw new BusinessException("流程回调缺少结果状态");
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.contains("COMPLETED") || normalized.contains("APPROVED") || "APPROVE".equals(normalized)) {
            return "APPROVED";
        }
        if (normalized.contains("REJECT")) {
            return "REJECTED";
        }
        if (normalized.contains("CANCEL") || normalized.contains("WITHDRAW")) {
            return "CANCELED";
        }
        throw new BusinessException("不支持的流程回调结果: " + value);
    }

    private boolean isEndedLink(AiBusinessFlowInstanceLink link) {
        return link.getEndTime() != null
                || "APPROVED".equalsIgnoreCase(link.getResult())
                || "REJECTED".equalsIgnoreCase(link.getResult())
                || "CANCELED".equalsIgnoreCase(link.getResult());
    }

    private AiBusinessBinding selectFlowBindingForStart(Long tenantId, String objectCode, String... fallbackCodes) {
        BindingLookupResult result = selectMainFlowBinding(tenantId, objectCode, fallbackCodes);
        if (isBindingEnabled(result.binding())) {
            if ("APPROVAL".equalsIgnoreCase(result.binding().getBindingType())) {
                log.info("[低代码流程启动] 使用历史审批绑定作为主流程: tenantId={}, objectCode={}, binding={}",
                        tenantId, result.matchedObjectCode(), describeBinding(result.binding()));
            }
            return result.binding();
        }
        if (result.binding() != null) {
            log.warn("[低代码流程启动] 未找到启用的主流程绑定: tenantId={}, objectCodes={}, binding={}",
                    tenantId, result.candidates(), describeBinding(result.binding()));
        } else {
            log.warn("[低代码流程启动] 未找到主流程绑定记录: tenantId={}, objectCodes={}", tenantId, result.candidates());
        }
        return null;
    }

    private AiBusinessBinding selectMainFlowBindingForConfig(Long tenantId, String objectCode, String... fallbackCodes) {
        return selectMainFlowBinding(tenantId, objectCode, fallbackCodes).binding();
    }

    private BindingLookupResult selectMainFlowBinding(Long tenantId, String objectCode, String... fallbackCodes) {
        List<String> candidates = objectCodeCandidates(objectCode, fallbackCodes);
        AiBusinessBinding firstDisabled = null;
        String disabledObjectCode = null;
        for (String candidate : candidates) {
            AiBusinessBinding binding = bindingMapper.selectBindingByTypeAndCode(tenantId, "OBJECT", candidate, "FLOW");
            if (isBindingEnabled(binding)) {
                return new BindingLookupResult(binding, candidate, candidates);
            }
            if (firstDisabled == null && binding != null) {
                firstDisabled = binding;
                disabledObjectCode = candidate;
            }
            AiBusinessBinding legacyApprovalBinding = bindingMapper.selectBindingByTypeAndCode(
                    tenantId, "OBJECT", candidate, "APPROVAL");
            if (isBindingEnabled(legacyApprovalBinding)) {
                return new BindingLookupResult(legacyApprovalBinding, candidate, candidates);
            }
            if (firstDisabled == null && legacyApprovalBinding != null) {
                firstDisabled = legacyApprovalBinding;
                disabledObjectCode = candidate;
            }
        }
        return new BindingLookupResult(firstDisabled, disabledObjectCode, candidates);
    }

    private List<String> objectCodeCandidates(String objectCode, String... fallbackCodes) {
        Set<String> candidates = new LinkedHashSet<>();
        addObjectCodeCandidate(candidates, objectCode);
        if (fallbackCodes != null) {
            for (String fallbackCode : fallbackCodes) {
                addObjectCodeCandidate(candidates, fallbackCode);
            }
        }
        return new ArrayList<>(candidates);
    }

    private void addObjectCodeCandidate(Set<String> candidates, String objectCode) {
        String normalized = StringUtils.trimToNull(objectCode);
        if (StringUtils.isNotBlank(normalized)) {
            candidates.add(normalized);
        }
    }

    private FlowStartContext resolveFlowStartContext(Long tenantId, String objectCodeOrConfigKey) {
        String requestedObjectCode = StringUtils.trimToNull(objectCodeOrConfigKey);
        AiCrudConfig runtimeConfig = resolvePublishedRuntimeConfig(tenantId, requestedObjectCode);
        AiBusinessDocumentConfig documentConfig = resolveEnabledDocumentConfig(tenantId, requestedObjectCode, runtimeConfig);
        AiBusinessObject businessObject = resolveBusinessObject(tenantId, requestedObjectCode, runtimeConfig, documentConfig);
        String canonicalObjectCode = StringUtils.firstNonBlank(
                documentConfig == null ? null : documentConfig.getObjectCode(),
                businessObject == null ? null : businessObject.getObjectCode(),
                runtimeConfig == null ? null : runtimeConfig.getObjectCode(),
                requestedObjectCode);

        if (documentConfig == null && !StringUtils.equals(canonicalObjectCode, requestedObjectCode)) {
            documentConfig = resolveEnabledDocumentConfig(tenantId, canonicalObjectCode, runtimeConfig);
        }
        if (runtimeConfig == null) {
            runtimeConfig = resolvePublishedRuntimeConfig(tenantId, StringUtils.firstNonBlank(
                    documentConfig == null ? null : documentConfig.getConfigKey(),
                    businessObject == null ? null : businessObject.getConfigKey(),
                    canonicalObjectCode));
        }
        String configKey = resolveStartConfigKey(documentConfig, runtimeConfig);
        return new FlowStartContext(requestedObjectCode, canonicalObjectCode, configKey, documentConfig, runtimeConfig);
    }

    private String resolveCanonicalObjectCode(Long tenantId, String objectCodeOrConfigKey) {
        if (StringUtils.isBlank(objectCodeOrConfigKey)) {
            return objectCodeOrConfigKey;
        }
        AiCrudConfig runtimeConfig = resolvePublishedRuntimeConfig(tenantId, objectCodeOrConfigKey);
        AiBusinessDocumentConfig documentConfig = resolveEnabledDocumentConfig(tenantId, objectCodeOrConfigKey, runtimeConfig);
        AiBusinessObject businessObject = resolveBusinessObject(tenantId, objectCodeOrConfigKey, runtimeConfig, documentConfig);
        return StringUtils.firstNonBlank(
                documentConfig == null ? null : documentConfig.getObjectCode(),
                businessObject == null ? null : businessObject.getObjectCode(),
                runtimeConfig == null ? null : runtimeConfig.getObjectCode(),
                StringUtils.trimToNull(objectCodeOrConfigKey));
    }

    private AiBusinessDocumentConfig resolveEnabledDocumentConfig(Long tenantId, String objectCodeOrConfigKey,
                                                                  AiCrudConfig runtimeConfig) {
        Long effectiveTenantId = tenantId != null ? tenantId : resolveTenantId();
        AiBusinessDocumentConfig config = documentConfigService.selectEnabledByObjectCode(effectiveTenantId, objectCodeOrConfigKey);
        if (config != null) {
            return config;
        }
        config = documentConfigService.selectEnabledByConfigKey(effectiveTenantId, objectCodeOrConfigKey);
        if (config != null || runtimeConfig == null) {
            return config;
        }
        config = documentConfigService.selectEnabledByConfigKey(effectiveTenantId, runtimeConfig.getConfigKey());
        if (config != null) {
            return config;
        }
        return documentConfigService.selectEnabledByObjectCode(effectiveTenantId, runtimeConfig.getObjectCode());
    }

    private AiBusinessObject resolveBusinessObject(Long tenantId, String objectCodeOrConfigKey,
                                                   AiCrudConfig runtimeConfig,
                                                   AiBusinessDocumentConfig documentConfig) {
        Long effectiveTenantId = tenantId != null ? tenantId : resolveTenantId();
        AiBusinessObject object = null;
        if (documentConfig != null && StringUtils.isNotBlank(documentConfig.getConfigKey())) {
            object = businessObjectMapper.selectByConfigKey(effectiveTenantId, documentConfig.getConfigKey());
        }
        if (object == null && runtimeConfig != null && StringUtils.isNotBlank(runtimeConfig.getConfigKey())) {
            object = businessObjectMapper.selectByConfigKey(effectiveTenantId, runtimeConfig.getConfigKey());
        }
        if (object == null && StringUtils.isNotBlank(objectCodeOrConfigKey)) {
            object = businessObjectMapper.selectByConfigKey(effectiveTenantId, objectCodeOrConfigKey);
        }
        return object;
    }

    private boolean isBindingEnabled(AiBusinessBinding binding) {
        return binding != null && !Integer.valueOf(0).equals(binding.getStatus());
    }

    private String describeBinding(AiBusinessBinding binding) {
        if (binding == null) {
            return "null";
        }
        return "id=" + binding.getId()
                + ", type=" + binding.getBindingType()
                + ", status=" + binding.getStatus()
                + ", targetCode=" + binding.getTargetCode()
                + ", bindingKey=" + binding.getBindingKey()
                + ", bindingName=" + binding.getBindingName()
                + ", configBlank=" + StringUtils.isBlank(binding.getBindingConfig());
    }

    private String previewBindingConfig(JSONObject bindingConfig) {
        if (bindingConfig == null || bindingConfig.isEmpty()) {
            return "{}";
        }
        return StringUtils.left(bindingConfig.toJSONString(), 400);
    }

    private BusinessFlowRuntimeVO toRuntimeVO(AiBusinessFlowInstanceLink link, String message) {
        BusinessFlowRuntimeVO vo = new BusinessFlowRuntimeVO();
        vo.setLinkId(link.getId());
        vo.setObjectCode(link.getObjectCode());
        vo.setRecordId(link.getRecordId());
        vo.setBusinessKey(link.getBusinessKey());
        vo.setFlowModelKey(link.getFlowModelKey());
        vo.setProcessInstanceId(link.getProcessInstanceId());
        vo.setFlowStatus(link.getFlowStatus());
        vo.setResult(link.getResult());
        vo.setStartTime(link.getStartTime());
        vo.setEndTime(link.getEndTime());
        vo.setMessage(message);
        return vo;
    }

    private String buildBusinessKey(String objectCode, Long recordId) {
        return objectCode + ":" + recordId;
    }

    private BusinessKeyParts parseBusinessKey(String businessKey) {
        if (StringUtils.isBlank(businessKey) || !businessKey.contains(":")) {
            throw new BusinessException("业务Key格式错误，应为 objectCode:recordId");
        }
        String[] parts = businessKey.split(":", 2);
        if (StringUtils.isBlank(parts[0]) || StringUtils.isBlank(parts[1])) {
            throw new BusinessException("业务Key格式错误，应为 objectCode:recordId");
        }
        try {
            return new BusinessKeyParts(parts[0], Long.valueOf(parts[1]));
        } catch (NumberFormatException e) {
            throw new BusinessException("业务Key中的记录ID必须是数字");
        }
    }

    /**
     * 查询流程状态
     */
    public JSONObject getFlowStatus(String businessKey) {
        try {
            FlowResult<Map<String, Object>> result = flowClient.getProcessStatus(businessKey);
            if (result.isSuccess()) {
                JSONObject status = new JSONObject();
                status.put("businessKey", businessKey);
                status.put("data", result.getData());
                return status;
            }
        } catch (Exception e) {
            log.debug("查询流程状态失败: businessKey={}", businessKey);
        }
        return null;
    }

    /**
     * 构建流程变量
     */
    private Map<String, Object> buildFlowVariables(JSONObject bindingConfig, Map<String, Object> recordData) {
        Map<String, Object> variables = new HashMap<>();
        JSONArray variableMapping = bindingConfig.getJSONArray("variableMapping");

        if (variableMapping != null && recordData != null) {
            for (int i = 0; i < variableMapping.size(); i++) {
                JSONObject mapping = variableMapping.getJSONObject(i);
                String formField = StringUtils.defaultIfBlank(mapping.getString("formField"), mapping.getString("field"));
                String flowVariable = StringUtils.defaultIfBlank(mapping.getString("flowVariable"), mapping.getString("variable"));
                Object value = readRecordValue(recordData, formField);
                if (value != null && StringUtils.isNotBlank(flowVariable)) {
                    variables.put(flowVariable, value);
                }
            }
        }

        return variables;
    }

    /**
     * 构建流程标题
     */
    private String buildFlowTitle(JSONObject bindingConfig, Map<String, Object> recordData, String objectCode) {
        String titleTemplate = bindingConfig.getString("titleTemplate");
        if (titleTemplate != null && !titleTemplate.isBlank() && recordData != null) {
            // 兼容历史 ${fieldName} 与 Flyway 友好的 {fieldName} 模板。
            for (Map.Entry<String, Object> entry : recordData.entrySet()) {
                titleTemplate = replaceTemplateValue(titleTemplate, entry.getKey(), entry.getValue());
            }
            return titleTemplate;
        }
        return objectCode + " 审批申请";
    }

    private String replaceTemplateValue(String template, String key, Object value) {
        if (StringUtils.isBlank(template) || StringUtils.isBlank(key)) {
            return template;
        }
        String text = value != null ? String.valueOf(value) : "";
        String result = replaceTemplateToken(template, key, text);
        result = replaceTemplateToken(result, snakeToCamel(key), text);
        return replaceTemplateToken(result, camelToSnake(key), text);
    }

    private String replaceTemplateToken(String template, String key, String value) {
        if (StringUtils.isBlank(key)) {
            return template;
        }
        return template.replace("${" + key + "}", value)
                .replace("{" + key + "}", value);
    }

    private BusinessFlowBindingVO toVO(String objectCode, AiBusinessBinding binding) {
        JSONObject config = readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(config, binding.getTenantId(), objectCode);
        BusinessFlowBindingVO vo = new BusinessFlowBindingVO();
        vo.setBindingId(binding.getId());
        vo.setObjectCode(objectCode);
        vo.setFlowModelKey(StringUtils.defaultIfBlank(resolveFlowModelKey(config), binding.getBindingKey()));
        vo.setFlowModelName(StringUtils.defaultIfBlank(config.getString("flowModelName"), binding.getBindingName()));
        vo.setTitleTemplate(config.getString("titleTemplate"));
        vo.setStartMode(normalizeStartMode(config.getString("startMode")));
        vo.setBusinessBinding(toBusinessBindingDTO(config.getJSONObject("businessBinding")));
        vo.setVariableMapping(normalizeVariableMapping(config.getJSONArray("variableMapping")));
        vo.setNodeForms(normalizeNodeForms(readMapList(config.getJSONArray("nodeForms"))));
        vo.setConditionFlows(readMapList(config.getJSONArray("conditionFlows")));
        vo.setOptions(readOptions(config.getJSONObject("options")));
        vo.setStatus(binding.getStatus());
        enrichBindingSummary(vo, "AI_BUSINESS_BINDING");
        return vo;
    }

    private BusinessFlowBindingVO legacyDocumentFlowToVO(String objectCode, AiBusinessDocumentConfig documentConfig) {
        BusinessFlowBindingVO vo = new BusinessFlowBindingVO();
        vo.setObjectCode(objectCode);
        vo.setFlowModelKey(documentConfig.getDefaultFlowKey());
        vo.setFlowModelName(documentConfig.getDefaultFlowKey());
        vo.setStartMode("MANUAL");
        vo.setBusinessBinding(defaultBusinessBinding(null, documentConfig));
        vo.setStatus(1);
        vo.setCompatibilitySource("DOCUMENT_DEFAULT_FLOW");
        vo.setComplete(false);
        vo.setGaps(List.of("历史默认流程缺少变量映射，请在流程与自动化中保存一次主流程"));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("configured", true);
        summary.put("flowModelKey", documentConfig.getDefaultFlowKey());
        summary.put("flowModelName", documentConfig.getDefaultFlowKey());
        summary.put("startMode", "MANUAL");
        summary.put("businessBinding", vo.getBusinessBinding());
        summary.put("variableMappingCount", 0);
        summary.put("complete", false);
        summary.put("gaps", vo.getGaps());
        summary.put("compatibilitySource", "DOCUMENT_DEFAULT_FLOW");
        vo.setMainFlowSummary(summary);
        return vo;
    }

    private void enrichBindingSummary(BusinessFlowBindingVO vo, String compatibilitySource) {
        List<String> gaps = new ArrayList<>();
        if (StringUtils.isBlank(vo.getFlowModelKey())) {
            gaps.add("未配置主流程");
        }
        if (StringUtils.isBlank(vo.getStartMode())) {
            gaps.add("发起方式未配置");
        }
        if (vo.getVariableMapping() == null || vo.getVariableMapping().isEmpty()) {
            gaps.add("变量映射缺失");
        }
        boolean complete = gaps.isEmpty();
        vo.setComplete(complete);
        vo.setGaps(gaps);
        vo.setCompatibilitySource(compatibilitySource);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("configured", StringUtils.isNotBlank(vo.getFlowModelKey()));
        summary.put("bindingId", vo.getBindingId());
        summary.put("flowModelKey", vo.getFlowModelKey());
        summary.put("flowModelName", vo.getFlowModelName());
        summary.put("startMode", vo.getStartMode());
        summary.put("businessBinding", vo.getBusinessBinding());
        summary.put("variableMappingCount", vo.getVariableMapping() == null ? 0 : vo.getVariableMapping().size());
        summary.put("complete", complete);
        summary.put("gaps", gaps);
        summary.put("compatibilitySource", compatibilitySource);
        vo.setMainFlowSummary(summary);
    }

    private BusinessFlowBindingDTO toDTO(JSONObject config) {
        JSONObject source = config == null ? new JSONObject() : config;
        BusinessFlowBindingDTO dto = new BusinessFlowBindingDTO();
        dto.setFlowModelKey(resolveFlowModelKey(source));
        dto.setFlowModelName(source.getString("flowModelName"));
        dto.setTitleTemplate(source.getString("titleTemplate"));
        dto.setStartMode(normalizeStartMode(source.getString("startMode")));
        dto.setBusinessBinding(toBusinessBindingDTO(source.getJSONObject("businessBinding")));
        dto.setVariableMapping(normalizeVariableMapping(source.getJSONArray("variableMapping")));
        dto.setNodeForms(normalizeNodeForms(readMapList(source.getJSONArray("nodeForms"))));
        dto.setConditionFlows(readMapList(source.getJSONArray("conditionFlows")));
        dto.setOptions(readOptions(source.getJSONObject("options")));
        return dto;
    }

    private JSONObject normalizeBindingConfig(BusinessFlowBindingDTO dto) {
        JSONObject config = new JSONObject();
        config.put("flowModelKey", StringUtils.trimToNull(dto.getFlowModelKey()));
        config.put("flowModelName", StringUtils.trimToNull(dto.getFlowModelName()));
        config.put("titleTemplate", StringUtils.trimToNull(dto.getTitleTemplate()));
        config.put("startMode", normalizeStartMode(dto.getStartMode()));
        JSONObject businessBinding = normalizeBusinessBinding(dto.getBusinessBinding());
        if (!businessBinding.isEmpty()) {
            config.put("businessBinding", businessBinding);
        }
        JSONArray variableMapping = new JSONArray();
        if (dto.getVariableMapping() != null) {
            for (BusinessFlowBindingDTO.VariableMappingDTO item : dto.getVariableMapping()) {
                if (item == null || StringUtils.isBlank(item.getFormField()) || StringUtils.isBlank(item.getFlowVariable())) {
                    continue;
                }
                JSONObject mapping = new JSONObject();
                mapping.put("formField", item.getFormField().trim());
                mapping.put("flowVariable", item.getFlowVariable().trim());
                mapping.put("label", StringUtils.trimToNull(item.getLabel()));
                variableMapping.add(mapping);
            }
        }
        config.put("variableMapping", variableMapping);
        config.put("nodeForms", normalizeNodeForms(dto.getNodeForms()));
        config.put("conditionFlows", dto.getConditionFlows() == null ? new ArrayList<>() : dto.getConditionFlows());
        config.put("options", dto.getOptions() == null ? new LinkedHashMap<>() : dto.getOptions());
        return config;
    }

    private void ensureBusinessBinding(JSONObject config, Long tenantId, String objectCode) {
        if (config == null) {
            return;
        }
        AiCrudConfig runtimeConfig = resolvePublishedRuntimeConfig(tenantId, objectCode);
        AiBusinessDocumentConfig documentConfig = resolveEnabledDocumentConfig(tenantId, objectCode, runtimeConfig);
        JSONObject defaults = normalizeBusinessBinding(defaultBusinessBinding(runtimeConfig, documentConfig));
        JSONObject current = config.getJSONObject("businessBinding");
        if (current == null || current.isEmpty()) {
            if (!defaults.isEmpty()) {
                config.put("businessBinding", defaults);
            }
            return;
        }
        mergeBusinessBindingDefaults(current, defaults);
        config.put("businessBinding", current);
    }

    private BusinessFlowBindingDTO.BusinessBindingDTO defaultBusinessBinding(AiCrudConfig runtimeConfig,
                                                                            AiBusinessDocumentConfig documentConfig) {
        BusinessFlowBindingDTO.BusinessBindingDTO binding = new BusinessFlowBindingDTO.BusinessBindingDTO();
        binding.setMode("LOWCODE_OBJECT");
        if (runtimeConfig != null) {
            binding.setTableName(StringUtils.firstNonBlank(runtimeConfig.getRuntimeTableName(), runtimeConfig.getTableName()));
            binding.setPrimaryKeyField(StringUtils.firstNonBlank(
                    runtimeConfig.getPrimaryKeyField(),
                    runtimeConfig.getPrimaryKeyColumn(),
                    "id"));
        } else {
            binding.setPrimaryKeyField("id");
        }
        binding.setTenantField("tenant_id");
        if (documentConfig != null) {
            binding.setStatusField(StringUtils.trimToNull(documentConfig.getStatusField()));
            binding.setOwnerField(StringUtils.trimToNull(documentConfig.getOwnerField()));
        }
        return binding;
    }

    private JSONObject normalizeBusinessBinding(BusinessFlowBindingDTO.BusinessBindingDTO binding) {
        JSONObject result = new JSONObject();
        if (binding == null) {
            return result;
        }
        putText(result, "mode", normalizeBusinessBindingMode(binding.getMode()));
        putText(result, "tableName", binding.getTableName());
        putText(result, "primaryKeyField", binding.getPrimaryKeyField());
        putText(result, "tenantField", binding.getTenantField());
        putText(result, "statusField", binding.getStatusField());
        putText(result, "titleField", binding.getTitleField());
        putText(result, "ownerField", binding.getOwnerField());
        return result;
    }

    private BusinessFlowBindingDTO.BusinessBindingDTO toBusinessBindingDTO(JSONObject source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        BusinessFlowBindingDTO.BusinessBindingDTO binding = new BusinessFlowBindingDTO.BusinessBindingDTO();
        binding.setMode(normalizeBusinessBindingMode(source.getString("mode")));
        binding.setTableName(StringUtils.trimToNull(source.getString("tableName")));
        binding.setPrimaryKeyField(StringUtils.trimToNull(source.getString("primaryKeyField")));
        binding.setTenantField(StringUtils.trimToNull(source.getString("tenantField")));
        binding.setStatusField(StringUtils.trimToNull(source.getString("statusField")));
        binding.setTitleField(StringUtils.trimToNull(source.getString("titleField")));
        binding.setOwnerField(StringUtils.trimToNull(source.getString("ownerField")));
        return binding;
    }

    private void mergeBusinessBindingDefaults(JSONObject current, JSONObject defaults) {
        if (current == null || defaults == null || defaults.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.isBlank(current.getString(key)) && entry.getValue() != null) {
                current.put(key, entry.getValue());
            }
        }
    }

    private String normalizeBusinessBindingMode(String mode) {
        String normalized = StringUtils.defaultIfBlank(mode, "LOWCODE_OBJECT").trim().toUpperCase();
        if ("BUSINESS_TABLE".equals(normalized) || "ADAPTER".equals(normalized)) {
            return normalized;
        }
        return "LOWCODE_OBJECT";
    }

    private void putText(JSONObject target, String key, String value) {
        String text = StringUtils.trimToNull(value);
        if (text != null) {
            target.put(key, text);
        }
    }

    private List<Map<String, Object>> collectBusinessFormAssets(BusinessObjectVO object, JSONObject formSchema) {
        if (object == null || formSchema == null || formSchema.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        appendBusinessFormAsset(result, seen, object, formSchema, "default");

        JSONArray forms = readNestedArray(formSchema.get("forms"));
        for (int i = 0; i < forms.size(); i++) {
            JSONObject form = forms.getJSONObject(i);
            JSONObject schema = readNestedObject(form.get("schema"));
            appendBusinessFormAsset(result, seen, object, schema.isEmpty() ? form : schema, "form");
        }

        JSONObject settings = readNestedObject(formSchema.get("settings"));
        JSONArray formAssets = readNestedArray(settings.get("formAssets"));
        for (int i = 0; i < formAssets.size(); i++) {
            JSONObject asset = formAssets.getJSONObject(i);
            JSONObject schema = readNestedObject(asset.get("schema"));
            appendBusinessFormAsset(result, seen, object, schema.isEmpty() ? asset : schema, "asset");
        }
        return result;
    }

    private void appendBusinessFormAsset(List<Map<String, Object>> result,
                                         Set<String> seen,
                                         BusinessObjectVO object,
                                         JSONObject schema,
                                         String source) {
        if (schema == null || schema.isEmpty()) {
            return;
        }
        String formKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(schema.getString("formKey")),
                StringUtils.trimToNull(schema.getString("defaultFormKey")));
        if (formKey == null || !seen.add(formKey)) {
            return;
        }
        String formName = StringUtils.defaultIfBlank(schema.getString("formName"), object.getObjectName() + "表单");
        List<Map<String, Object>> fieldCatalog = collectBusinessFormFieldCatalog(schema);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "BUSINESS_OBJECT_FORM");
        item.put("objectCode", object.getObjectCode());
        item.put("objectName", object.getObjectName());
        item.put("formKey", formKey);
        item.put("formName", formName);
        item.put("viewKey", "default");
        item.put("source", source);
        item.put("fieldCatalog", fieldCatalog);
        result.add(item);
    }

    private List<Map<String, Object>> collectBusinessFormFieldCatalog(JSONObject schema) {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        collectBusinessFormFieldComponents(readNestedArray(schema.get("components")), result, seen);
        return result;
    }

    private void collectBusinessFormFieldComponents(JSONArray components,
                                                    List<Map<String, Object>> result,
                                                    Set<String> seen) {
        if (components == null) {
            return;
        }
        for (int i = 0; i < components.size(); i++) {
            JSONObject component = components.getJSONObject(i);
            if (component == null) {
                continue;
            }
            JSONObject binding = readNestedObject(component.get("fieldBinding"));
            JSONObject props = readNestedObject(component.get("props"));
            String field = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(binding.getString("fieldCode")),
                    StringUtils.trimToNull(component.getString("field")),
                    StringUtils.trimToNull(props.getString("field")));
            if (field != null && seen.add(field)) {
                JSONObject validation = readNestedObject(component.get("validation"));
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("field", field);
                item.put("fieldCode", field);
                item.put("label", StringUtils.firstNonBlank(
                        StringUtils.trimToNull(component.getString("label")),
                        StringUtils.trimToNull(props.getString("label")),
                        StringUtils.trimToNull(props.getString("title")),
                        field));
                item.put("componentType", StringUtils.trimToEmpty(component.getString("componentKey")));
                item.put("dataType", StringUtils.trimToEmpty(binding.getString("dataType")));
                item.put("dictType", StringUtils.trimToEmpty(props.getString("dictType")));
                item.put("required", readBooleanValue(validation.get("required"), false));
                result.add(item);
            }
            collectBusinessFormFieldComponents(readNestedArray(component.get("children")), result, seen);
        }
    }

    private JSONObject readJsonObject(String json) {
        if (StringUtils.isBlank(json)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private JSONObject readNestedObject(Object value) {
        if (value == null) {
            return new JSONObject();
        }
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        if (value instanceof Map<?, ?> || value instanceof String) {
            try {
                String text = value instanceof String stringValue ? stringValue : JSON.toJSONString(value);
                return StringUtils.isBlank(text) ? new JSONObject() : JSON.parseObject(text);
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    private JSONArray readNestedArray(Object value) {
        if (value == null) {
            return new JSONArray();
        }
        if (value instanceof JSONArray jsonArray) {
            return jsonArray;
        }
        if (value instanceof List<?> || value instanceof String) {
            try {
                String text = value instanceof String stringValue ? stringValue : JSON.toJSONString(value);
                return StringUtils.isBlank(text) ? new JSONArray() : JSON.parseArray(text);
            } catch (Exception e) {
                return new JSONArray();
            }
        }
        return new JSONArray();
    }

    private void putIfText(Map<String, Object> target, String key, Object value) {
        String text = StringUtils.trimToNull(value == null ? null : String.valueOf(value));
        if (text != null && !"null".equalsIgnoreCase(text)) {
            target.put(key, text);
        }
    }

    private String normalizeStartMode(String startMode) {
        String normalized = StringUtils.defaultIfBlank(startMode, "MANUAL").trim().toUpperCase();
        if ("MANUAL_AND_TRIGGER".equals(normalized) || "MANUAL_TRIGGER".equals(normalized) || "BOTH".equals(normalized)) {
            return "BOTH";
        }
        if ("AUTO".equals(normalized) || "AUTOMATIC".equals(normalized)) {
            return "TRIGGER";
        }
        if ("TRIGGER".equals(normalized)) {
            return "TRIGGER";
        }
        return "MANUAL";
    }

    private List<BusinessFlowBindingDTO.VariableMappingDTO> normalizeVariableMapping(JSONArray variableMapping) {
        List<BusinessFlowBindingDTO.VariableMappingDTO> result = new ArrayList<>();
        if (variableMapping == null) {
            return result;
        }
        for (int i = 0; i < variableMapping.size(); i++) {
            JSONObject mapping = variableMapping.getJSONObject(i);
            if (mapping == null) {
                continue;
            }
            String formField = StringUtils.defaultIfBlank(mapping.getString("formField"), mapping.getString("field"));
            String flowVariable = StringUtils.defaultIfBlank(mapping.getString("flowVariable"), mapping.getString("variable"));
            if (StringUtils.isBlank(formField) || StringUtils.isBlank(flowVariable)) {
                continue;
            }
            BusinessFlowBindingDTO.VariableMappingDTO item = new BusinessFlowBindingDTO.VariableMappingDTO();
            item.setFormField(formField.trim());
            item.setFlowVariable(flowVariable.trim());
            item.setLabel(StringUtils.trimToNull(mapping.getString("label")));
            result.add(item);
        }
        return result;
    }

    private JSONObject readBindingConfig(String bindingConfig) {
        if (StringUtils.isBlank(bindingConfig)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(bindingConfig);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private String resolveFlowModelKey(JSONObject config) {
        if (config == null) {
            return null;
        }
        return StringUtils.firstNonBlank(
                config.getString("flowModelKey"),
                config.getString("flowKey"),
                config.getString("processDefinitionKey"),
                config.getString("modelKey")
        );
    }

    private String resolveBindingName(JSONObject config) {
        return StringUtils.defaultIfBlank(config.getString("flowModelName"), config.getString("flowModelKey") + " 流程");
    }

    private JSONObject toConfigJson(BusinessFlowBindingVO binding) {
        JSONObject config = new JSONObject();
        config.put("flowModelKey", binding.getFlowModelKey());
        config.put("flowModelName", binding.getFlowModelName());
        config.put("titleTemplate", binding.getTitleTemplate());
        config.put("startMode", binding.getStartMode());
        JSONObject businessBinding = normalizeBusinessBinding(binding.getBusinessBinding());
        if (!businessBinding.isEmpty()) {
            config.put("businessBinding", businessBinding);
        }
        config.put("variableMapping", binding.getVariableMapping());
        config.put("nodeForms", binding.getNodeForms());
        config.put("conditionFlows", binding.getConditionFlows());
        config.put("options", binding.getOptions());
        return config;
    }

    private List<Map<String, Object>> normalizeNodeForms(List<Map<String, Object>> nodeForms) {
        if (nodeForms == null || nodeForms.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> source : nodeForms) {
            if (source == null) {
                continue;
            }
            String taskDefKey = StringUtils.trimToNull(textValue(source.get("taskDefKey")));
            if (taskDefKey == null || !seen.add(taskDefKey)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            putIfText(item, "taskDefKey", taskDefKey);
            putIfText(item, "taskName", source.get("taskName"));
            String formMode = normalizeNodeFormMode(textValue(source.get("formMode")));
            putIfText(item, "formMode", formMode);
            putIfText(item, "formKey", source.get("formKey"));
            putIfText(item, "formName", source.get("formName"));
            putIfText(item, "providerKey", source.get("providerKey"));
            putIfText(item, "formUrl", source.get("formUrl"));
            putIfText(item, "viewKey", source.get("viewKey"));
            putIfText(item, "editMode", normalizeNodeEditMode(textValue(source.get("editMode"))));
            Object formRef = source.get("formRef");
            if (formRef instanceof Map<?, ?> || formRef instanceof JSONObject) {
                item.put("formRef", readNestedObject(formRef));
            }
            List<Map<String, Object>> fieldPermissions = normalizeFieldPermissions(source.get("fieldPermissions"));
            if (fieldPermissions.isEmpty()) {
                fieldPermissions = normalizeFieldSelections(source);
            }
            if (!fieldPermissions.isEmpty()) {
                item.put("fieldPermissions", fieldPermissions);
            }
            result.add(item);
        }
        return result;
    }

    private String normalizeNodeFormMode(String formMode) {
        String normalized = StringUtils.defaultIfBlank(formMode, "BUSINESS_OBJECT_FORM").trim().toUpperCase();
        if ("BUSINESS_CODE_FORM".equals(normalized) || "EXTERNAL".equals(normalized)) {
            return normalized;
        }
        return "BUSINESS_OBJECT_FORM";
    }

    private String normalizeNodeEditMode(String editMode) {
        String normalized = StringUtils.defaultIfBlank(editMode, "READONLY").trim().toUpperCase();
        if ("EDITABLE".equals(normalized) || "MODIFY_RESUBMIT".equals(normalized)) {
            return normalized;
        }
        return "READONLY";
    }

    private List<Map<String, Object>> normalizeFieldPermissions(Object permissions) {
        if (permissions instanceof Map<?, ?> || permissions instanceof JSONObject) {
            JSONObject object = readNestedObject(permissions);
            JSONArray fields = readNestedArray(object.get("fields"));
            if (!fields.isEmpty()) {
                return normalizeFieldPermissions(fields);
            }
            return buildFieldPermissionsFromSelections(
                    readFieldSet(object, "visibleFields", "visible", "readableFields", "readable"),
                    readFieldSet(object, "writableFields", "writable"),
                    readFieldSet(object, "requiredFields", "required"));
        }
        JSONArray array = readNestedArray(permissions);
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject source = array.getJSONObject(i);
            if (source == null) {
                continue;
            }
            String field = StringUtils.trimToNull(source.getString("field"));
            if (field == null || !seen.add(field)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", field);
            putIfText(item, "label", source.getString("label"));
            item.put("readable", readBooleanValue(source.get("readable"), true));
            item.put("writable", readBooleanValue(source.get("writable"), false));
            item.put("required", readBooleanValue(source.get("required"), false));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> normalizeFieldSelections(Map<String, Object> source) {
        return buildFieldPermissionsFromSelections(
                readFieldSet(source, "visibleFields", "visible", "readableFields", "readable"),
                readFieldSet(source, "writableFields", "writable"),
                readFieldSet(source, "requiredFields", "required"));
    }

    private List<Map<String, Object>> buildFieldPermissionsFromSelections(Set<String> visible,
                                                                          Set<String> writable,
                                                                          Set<String> required) {
        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(visible);
        fields.addAll(writable);
        fields.addAll(required);
        if (fields.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String field : fields) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", field);
            boolean readable = visible.isEmpty() || visible.contains(field);
            boolean editable = readable && writable.contains(field);
            item.put("readable", readable);
            item.put("writable", editable);
            item.put("required", editable && required.contains(field));
            result.add(item);
        }
        return result;
    }

    private Set<String> readFieldSet(Map<String, Object> source, String... keys) {
        Set<String> result = new LinkedHashSet<>();
        if (source == null || keys == null) {
            return result;
        }
        for (String key : keys) {
            Object value = source.get(key);
            if (value == null) {
                continue;
            }
            JSONArray array = readNestedArray(value);
            for (int i = 0; i < array.size(); i++) {
                String field = StringUtils.trimToNull(array.getString(i));
                if (field != null) {
                    result.add(field);
                }
            }
        }
        return result;
    }

    private String textValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text) ? null : text;
    }

    private boolean readBooleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        if (StringUtils.isBlank(text)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }

    private List<Map<String, Object>> readMapList(JSONArray array) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (array == null) {
            return result;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item != null) {
                result.add(new LinkedHashMap<>(item));
            }
        }
        return result;
    }

    private Map<String, Object> readOptions(JSONObject options) {
        if (options == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(options);
    }

    private Object readRecordValue(Map<String, Object> recordData, String field) {
        if (recordData == null || StringUtils.isBlank(field)) {
            return null;
        }
        if (recordData.containsKey(field)) {
            return recordData.get(field);
        }
        String camelField = snakeToCamel(field);
        if (recordData.containsKey(camelField)) {
            return recordData.get(camelField);
        }
        String snakeField = camelToSnake(field);
        if (recordData.containsKey(snakeField)) {
            return recordData.get(snakeField);
        }
        return null;
    }

    private String snakeToCamel(String value) {
        if (StringUtils.isBlank(value) || !value.contains("_")) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    private String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                result.append('_').append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
        return tenantId != null ? tenantId : 1L;
    }

    private Long resolveUserId() {
        try {
            return SessionHelper.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveUsername() {
        try {
            return SessionHelper.getUsername();
        } catch (Exception e) {
            return null;
        }
    }

    private record FlowStartContext(String requestedObjectCode,
                                    String objectCode,
                                    String configKey,
                                    AiBusinessDocumentConfig documentConfig,
                                    AiCrudConfig runtimeConfig) {
    }

    private record BindingLookupResult(AiBusinessBinding binding,
                                       String matchedObjectCode,
                                       List<String> candidates) {
    }

    private record FlowStartLockHandle(String lockKey,
                                       RLock redissonLock,
                                       ReentrantLock localLock) {
    }

    private record BusinessKeyParts(String objectCode, Long recordId) {
    }

    private record TaskFormRuntimeContext(String objectCode,
                                          Long recordId,
                                          String businessKey,
                                          String configKey,
                                          JSONObject bindingConfig) {
    }
}
