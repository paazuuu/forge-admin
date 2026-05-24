package com.mdframe.forge.plugin.generator.service.lowcode;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeDomain;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDataModelDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeObjectSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePolicySchema;
import com.mdframe.forge.plugin.generator.mapper.AiLowcodeModelMapper;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDataModelVO;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 低代码数据模型主数据服务。
 */
@Service
@RequiredArgsConstructor
public class LowcodeDataModelService extends ServiceImpl<AiLowcodeModelMapper, AiLowcodeModel> {

    private static final Pattern MODEL_CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,47}$");
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String DDL_PERMISSION = "ai:lowcode:deploy-ddl";
    private static final Set<String> AUDIT_FIELDS = Set.of(
            "id", "tenantId", "createBy", "createTime", "createDept", "updateBy", "updateTime", "delFlag"
    );
    private static final Set<String> AUDIT_COLUMNS = Set.of(
            "id", "tenant_id", "create_by", "create_time", "create_dept", "update_by", "update_time", "del_flag"
    );

    private final ObjectMapper objectMapper;
    private final LowcodeDomainService domainService;
    private final LowcodeSchemaValidator schemaValidator;
    private final LowcodeDdlService ddlService;
    private final GenTableColumnMapper genTableColumnMapper;
    private final LowcodePolicyService policyService;

    public Page<LowcodeDataModelVO> page(PageQuery pageQuery, Long domainId, String keyword,
                                         String status, Boolean masterData) {
        Long tenantId = resolveTenantId();
        Page<AiLowcodeModel> modelPage = baseMapper.selectModelPage(
                new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()),
                tenantId,
                domainId,
                StringUtils.trimToNull(keyword),
                StringUtils.trimToNull(status),
                masterData);
        Page<LowcodeDataModelVO> result = new Page<>(modelPage.getCurrent(), modelPage.getSize(), modelPage.getTotal());
        result.setRecords(modelPage.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    public List<LowcodeDataModelVO> list(Long domainId, String keyword, String status) {
        return baseMapper.selectModelList(
                        resolveTenantId(),
                        domainId,
                        StringUtils.trimToNull(keyword),
                        StringUtils.trimToNull(status))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public LowcodeDataModelVO getDetail(Long id) {
        return toVO(requireModel(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveModel(LowcodeDataModelDTO dto) {
        if (dto == null) {
            throw new BusinessException("数据模型不能为空");
        }
        AiLowcodeModel model = dto.getId() == null ? new AiLowcodeModel() : requireModel(dto.getId());
        copyDtoToEntity(dto, model);
        if (dto.getId() == null) {
            save(model);
        } else {
            updateById(model);
        }
        syncGenTableColumnRequired(readModelSchema(model.getModelSchema()));
        if (Boolean.TRUE.equals(dto.getSyncDdl())) {
            syncTableStructure(model, dto);
        }
        return model.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        AiLowcodeModel model = requireModel(id);
        validateStatus(status);
        model.setStatus(status);
        updateById(model);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiLowcodeModel model = requireModel(id);
        removeById(model.getId());
    }

    public AiLowcodeModel requireModel(Long id) {
        if (id == null) {
            throw new BusinessException("数据模型ID不能为空");
        }
        AiLowcodeModel model = baseMapper.selectModelById(resolveTenantId(), id);
        if (model == null) {
            throw new BusinessException("数据模型不存在");
        }
        return model;
    }

    private void copyDtoToEntity(LowcodeDataModelDTO dto, AiLowcodeModel model) {
        AiLowcodeDomain domain = domainService.requireEnabledDomain(dto.getDomainId());
        String modelCode = StringUtils.trimToNull(dto.getModelCode());
        String modelName = StringUtils.trimToNull(dto.getModelName());
        if (StringUtils.isBlank(modelCode) || !MODEL_CODE_PATTERN.matcher(modelCode).matches()) {
            throw new BusinessException("模型编码格式不正确（小写字母开头，仅含小写字母+数字+下划线，2-48字符）");
        }
        if (StringUtils.isBlank(modelName)) {
            throw new BusinessException("模型名称不能为空");
        }
        String status = StringUtils.defaultIfBlank(dto.getStatus(), STATUS_ENABLED);
        validateStatus(status);
        Long excludeId = dto.getId();
        if (baseMapper.countByCode(resolveTenantId(), domain.getId(), modelCode, excludeId) > 0) {
            throw new BusinessException("同一业务领域下模型编码已存在: " + modelCode);
        }

        LowcodeModelSchema modelSchema = normalizeModelSchema(dto.getModelSchema(), domain, modelCode, modelName);
        schemaValidator.validateModel(modelSchema);

        model.setTenantId(resolveTenantId());
        model.setDomainId(domain.getId());
        model.setDomainCode(domain.getDomainCode());
        model.setModelCode(modelCode);
        model.setModelName(modelName);
        model.setModelDesc(StringUtils.trimToNull(dto.getModelDesc()));
        model.setStatus(status);
        model.setTenantEnabled(dto.getTenantEnabled() == null || dto.getTenantEnabled());
        model.setMasterData(Boolean.TRUE.equals(dto.getMasterData()));
        model.setModelSchema(writeModelSchema(modelSchema));
    }

    private LowcodeModelSchema normalizeModelSchema(LowcodeModelSchema schema, AiLowcodeDomain domain,
                                                    String modelCode, String modelName) {
        LowcodeModelSchema result = schema == null ? new LowcodeModelSchema() : schema;
        result.setSchemaVersion(2);
        LowcodeDomainRef domainRef = result.getDomain() == null ? new LowcodeDomainRef() : result.getDomain();
        domainRef.setId(domain.getId());
        domainRef.setCode(domain.getDomainCode());
        domainRef.setName(domain.getDomainName());
        result.setDomain(domainRef);

        LowcodeObjectSchema object = result.getObject() == null ? new LowcodeObjectSchema() : result.getObject();
        object.setCode(modelCode);
        object.setName(modelName);
        result.setObject(object);
        result.setBusinessName(modelName);
        if (StringUtils.isBlank(result.getAppType())) {
            result.setAppType("SINGLE");
        }
        if (StringUtils.isBlank(result.getTableMode())) {
            result.setTableMode("CREATE");
        } else {
            result.setTableMode(result.getTableMode().toUpperCase());
        }
        if (StringUtils.isBlank(result.getTableName())) {
            result.setTableName(StringUtils.defaultIfBlank(domain.getTablePrefix(), "biz_") + modelCode);
        }
        normalizePolicies(result);
        result.setFields(normalizeModelFields(result.getFields()));
        if (result.getIndexes() == null) {
            result.setIndexes(new ArrayList<>());
        }
        return result;
    }

    private List<LowcodeFieldSchema> normalizeModelFields(List<LowcodeFieldSchema> fields) {
        List<LowcodeFieldSchema> result = new ArrayList<>();
        result.add(systemField("id", "id", "ID", "bigint", "number", true, true, true, true, true, false, 100,
                "自增主键，系统生成"));
        if (fields != null) {
            fields.stream()
                    .filter(field -> field != null && !isAuditField(field))
                    .forEach(result::add);
        }
        result.add(systemField("tenantId", "tenant_id", "租户ID", "bigint", "number", true, false, true, false,
                false, false, 120, "租户隔离字段，系统写入"));
        result.add(systemField("createBy", "create_by", "创建人", "bigint", "number", false, false, true, false,
                false, false, 120, "审计字段，系统写入"));
        result.add(systemField("createTime", "create_time", "创建时间", "datetime", "datetime", true, false, true, false,
                true, false, 180, "审计字段，系统写入"));
        result.add(systemField("createDept", "create_dept", "创建部门", "bigint", "number", false, false, true, false,
                false, false, 120, "审计字段，系统写入"));
        result.add(systemField("updateBy", "update_by", "更新人", "bigint", "number", false, false, true, false,
                false, false, 120, "审计字段，系统写入"));
        result.add(systemField("updateTime", "update_time", "更新时间", "datetime", "datetime", true, false, true, false,
                true, false, 180, "审计字段，系统写入"));
        result.add(systemField("delFlag", "del_flag", "删除标志", "char", "input", true, false, true, false,
                false, false, 100, "逻辑删除字段，系统维护"));
        return result;
    }

    private LowcodeFieldSchema systemField(String field, String columnName, String label, String dataType,
                                           String componentType, boolean required, boolean primaryKey,
                                           boolean readonly, boolean searchable, boolean listVisible,
                                           boolean formVisible, int width, String remark) {
        LowcodeFieldSchema schema = new LowcodeFieldSchema();
        schema.setField(field);
        schema.setColumnName(columnName);
        schema.setLabel(label);
        schema.setDataType(dataType);
        schema.setLength("char".equals(dataType) ? 1 : null);
        schema.setPrecision(null);
        schema.setRequired(required);
        schema.setSearchable(searchable);
        schema.setListVisible(listVisible);
        schema.setFormVisible(formVisible);
        schema.setComponentType(componentType);
        schema.setQueryType("eq");
        schema.setSensitiveType("NONE");
        schema.setPrimaryKey(primaryKey);
        schema.setSystemField(true);
        schema.setReadonly(readonly);
        schema.setAutoIncrement(primaryKey);
        schema.setSortable("id".equals(field) || field.endsWith("Time"));
        schema.setWidth(width);
        schema.setRemark(remark);
        return schema;
    }

    private void normalizePolicies(LowcodeModelSchema schema) {
        policyService.normalizeModelSchema(schema);
    }

    private boolean isAuditField(LowcodeFieldSchema field) {
        if (field == null) {
            return false;
        }
        return AUDIT_FIELDS.contains(field.getField()) || AUDIT_COLUMNS.contains(field.getColumnName());
    }

    private void syncTableStructure(AiLowcodeModel model, LowcodeDataModelDTO dto) {
        if (!Boolean.TRUE.equals(dto.getConfirmSyncDdl())) {
            throw new BusinessException("同步表结构需要二次确认");
        }
        if (!SessionHelper.hasPermission(DDL_PERMISSION)) {
            throw new BusinessException("缺少同步表结构权限: " + DDL_PERMISSION);
        }
        ddlService.executeCreateTable(readModelSchema(model.getModelSchema()));
    }

    private void syncGenTableColumnRequired(LowcodeModelSchema modelSchema) {
        Long sourceTableId = resolveSourceTableId(modelSchema);
        if (modelSchema == null
                || (sourceTableId == null && StringUtils.isBlank(modelSchema.getTableName()))
                || modelSchema.getFields() == null) {
            return;
        }
        Set<String> syncedColumns = new HashSet<>();
        List<Map<String, Object>> columns = new ArrayList<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (field == null || isAuditField(field) || StringUtils.isBlank(field.getColumnName())) {
                continue;
            }
            if (!syncedColumns.add(field.getColumnName())) {
                continue;
            }
            Map<String, Object> column = new LinkedHashMap<>();
            column.put("columnName", field.getColumnName());
            column.put("required", Boolean.TRUE.equals(field.getRequired()) ? 1 : 0);
            columns.add(column);
        }
        if (!columns.isEmpty()) {
            genTableColumnMapper.updateRequiredByTableRef(sourceTableId, modelSchema.getTableName(), columns);
        }
    }

    private Long resolveSourceTableId(LowcodeModelSchema modelSchema) {
        if (modelSchema == null || modelSchema.getSourceTable() == null) {
            return null;
        }
        return modelSchema.getSourceTable().getTableId();
    }

    private LowcodeDataModelVO toVO(AiLowcodeModel model) {
        LowcodeDataModelVO vo = new LowcodeDataModelVO();
        vo.setId(model.getId());
        vo.setDomainId(model.getDomainId());
        vo.setDomainCode(model.getDomainCode());
        AiLowcodeDomain domain = domainService.getByCode(model.getDomainCode());
        vo.setDomainName(domain == null ? model.getDomainCode() : domain.getDomainName());
        vo.setModelCode(model.getModelCode());
        vo.setModelName(model.getModelName());
        vo.setModelDesc(model.getModelDesc());
        vo.setStatus(model.getStatus());
        vo.setTenantEnabled(model.getTenantEnabled());
        vo.setMasterData(model.getMasterData());
        LowcodeModelSchema modelSchema = readModelSchema(model.getModelSchema());
        if (domain != null) {
            modelSchema = normalizeModelSchema(modelSchema, domain, model.getModelCode(), model.getModelName());
        }
        vo.setModelSchema(modelSchema);
        vo.setCreateTime(model.getCreateTime());
        vo.setUpdateTime(model.getUpdateTime());
        return vo;
    }

    private LowcodeModelSchema readModelSchema(String json) {
        if (StringUtils.isBlank(json)) {
            return new LowcodeModelSchema();
        }
        try {
            return objectMapper.readValue(json, LowcodeModelSchema.class);
        } catch (Exception e) {
            throw new BusinessException("模型协议格式不正确");
        }
    }

    private String writeModelSchema(LowcodeModelSchema schema) {
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            throw new BusinessException("模型协议序列化失败");
        }
    }

    private void validateStatus(String status) {
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException("数据模型状态不正确");
        }
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}
