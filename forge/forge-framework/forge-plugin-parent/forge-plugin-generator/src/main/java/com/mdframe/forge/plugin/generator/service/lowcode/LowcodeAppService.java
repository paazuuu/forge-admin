package com.mdframe.forge.plugin.generator.service.lowcode;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigRenderVO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAppDraftDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeAppDetailVO;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 低代码应用草稿与详情服务。
 */
@Service
@RequiredArgsConstructor
public class LowcodeAppService {

    private static final Pattern CONFIG_KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");

    private final ObjectMapper objectMapper;
    private final AiCrudConfigService configService;
    private final AiCrudConfigMapper configMapper;
    private final LowcodeSchemaValidator schemaValidator;

    public Page<LowcodeAppDetailVO> page(PageQuery pageQuery, String keyword, String publishStatus) {
        Page<AiCrudConfig> configPage = configMapper.selectLowcodePage(
                new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()),
                resolveTenantId(),
                StringUtils.trimToNull(keyword),
                StringUtils.trimToNull(publishStatus));
        Page<LowcodeAppDetailVO> result = new Page<>(configPage.getCurrent(), configPage.getSize(), configPage.getTotal());
        result.setRecords(configPage.getRecords().stream().map(this::toDetailVO).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(LowcodeAppDraftDTO dto) {
        if (dto == null) {
            throw new BusinessException("草稿不能为空");
        }
        AiCrudConfig config = dto.getId() == null ? createDraft(dto) : updateDraft(dto);
        return config.getId();
    }

    public LowcodeAppDetailVO getDetail(Long id) {
        return toDetailVO(requireConfig(id));
    }

    public AiCrudConfigRenderVO preview(Long id, LowcodeAppDraftDTO draft) {
        AiCrudConfig existing = id == null ? null : requireConfig(id);
        String configKey = resolveConfigKey(existing, draft);
        LowcodeModelSchema modelSchema = resolveModelSchema(existing, draft);
        LowcodePageSchema pageSchema = resolvePageSchema(existing, draft, modelSchema);
        schemaValidator.validatePage(pageSchema, modelSchema);

        AiCrudConfig previewConfig = new AiCrudConfig();
        previewConfig.setConfigKey(configKey);
        previewConfig.setTableName(modelSchema.getTableName());
        previewConfig.setTableComment(modelSchema.getBusinessName());
        previewConfig.setAppName(resolveAppName(existing, draft, modelSchema));
        previewConfig.setMode("CONFIG");
        previewConfig.setBuildMode("LOWCODE");
        previewConfig.setStatus("0");
        previewConfig.setPublishStatus("DRAFT");
        previewConfig.setLayoutType(StringUtils.defaultIfBlank(pageSchema.getLayoutType(), "simple-crud"));
        previewConfig.setModelSchema(writeJson(modelSchema, "modelSchema"));
        previewConfig.setPageSchema(writeJson(pageSchema, "pageSchema"));
        return configService.buildRenderConfig(previewConfig);
    }

    AiCrudConfig requireConfig(Long id) {
        if (id == null) {
            throw new BusinessException("低代码应用ID不能为空");
        }
        AiCrudConfig config = configService.getById(id);
        if (config == null || !"LOWCODE".equals(config.getBuildMode())) {
            throw new BusinessException("低代码应用不存在");
        }
        return config;
    }

    LowcodeModelSchema readModelSchema(AiCrudConfig config) {
        return readJson(config.getModelSchema(), LowcodeModelSchema.class, "modelSchema");
    }

    LowcodePageSchema readPageSchema(AiCrudConfig config) {
        return readJson(config.getPageSchema(), LowcodePageSchema.class, "pageSchema");
    }

    LowcodePageSchema buildDefaultPageSchema(LowcodeModelSchema modelSchema) {
        LowcodePageSchema pageSchema = new LowcodePageSchema();
        boolean treeApp = "TREE".equals(StringUtils.defaultIfBlank(modelSchema.getAppType(), "SINGLE").toUpperCase(Locale.ROOT));
        pageSchema.setLayoutType(treeApp ? "tree-crud" : "simple-crud");
        pageSchema.getZones().add(buildZone("search", "search-form", true,
                modelSchema.getFields().stream()
                        .filter(field -> Boolean.TRUE.equals(field.getSearchable()))
                        .map(LowcodeFieldSchema::getField)
                        .toList()));
        pageSchema.getZones().add(buildZone("table", "data-table", true,
                modelSchema.getFields().stream()
                        .filter(field -> field.getListVisible() == null || Boolean.TRUE.equals(field.getListVisible()))
                        .map(LowcodeFieldSchema::getField)
                        .toList()));
        if (treeApp) {
            LowcodePageZone tableZone = pageSchema.getZones().stream()
                    .filter(zone -> "table".equals(zone.getZoneKey()))
                    .findFirst()
                    .orElse(null);
            if (tableZone != null) {
                tableZone.getProps().put("treeConfig", buildDefaultTreeConfig(modelSchema));
            }
        }
        pageSchema.getZones().add(buildZone("edit", "edit-form", true,
                modelSchema.getFields().stream()
                        .filter(field -> field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible()))
                        .map(LowcodeFieldSchema::getField)
                        .toList()));
        return pageSchema;
    }

    private Map<String, Object> buildDefaultTreeConfig(LowcodeModelSchema modelSchema) {
        String parentField = modelSchema.getTreeConfig() != null
                ? modelSchema.getTreeConfig().getParentField()
                : null;
        String labelField = modelSchema.getTreeConfig() != null
                ? modelSchema.getTreeConfig().getLabelField()
                : null;
        parentField = StringUtils.defaultIfBlank(parentField, modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getField)
                .filter(field -> "parentId".equals(field) || "pid".equals(field) || "parentCode".equals(field))
                .findFirst()
                .orElse("parentId"));
        labelField = StringUtils.defaultIfBlank(labelField, modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getField)
                .filter(field -> "name".equals(field) || "title".equals(field) || "label".equals(field))
                .findFirst()
                .orElseGet(() -> modelSchema.getFields().isEmpty() ? "name" : modelSchema.getFields().get(0).getField()));
        return Map.of(
                "keyField", "id",
                "parentField", parentField,
                "labelField", labelField,
                "childrenField", "children",
                "treeTitle", StringUtils.defaultIfBlank(modelSchema.getBusinessName(), "树形导航")
        );
    }

    String writeJson(Object value, String fieldName) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(fieldName + "序列化失败");
        }
    }

    private AiCrudConfig createDraft(LowcodeAppDraftDTO dto) {
        validateConfigKey(dto.getConfigKey());
        if (configService.getByConfigKey(dto.getConfigKey()) != null) {
            throw new BusinessException("configKey已存在: " + dto.getConfigKey());
        }
        LowcodeModelSchema modelSchema = resolveModelSchema(null, dto);
        LowcodePageSchema pageSchema = resolvePageSchema(null, dto, modelSchema);
        schemaValidator.validatePage(pageSchema, modelSchema);

        AiCrudConfig config = new AiCrudConfig();
        config.setTenantId(resolveTenantId());
        config.setConfigKey(dto.getConfigKey());
        config.setTableName(modelSchema.getTableName());
        config.setTableComment(modelSchema.getBusinessName());
        config.setAppName(resolveAppName(null, dto, modelSchema));
        config.setMenuName(StringUtils.defaultIfBlank(dto.getMenuName(), config.getAppName()));
        config.setMenuParentId(dto.getMenuParentId());
        config.setMenuSort(dto.getMenuSort() != null ? dto.getMenuSort() : 0);
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setStatus("0");
        config.setPublishStatus("DRAFT");
        config.setDraftVersion(1);
        config.setPublishedVersion(0);
        config.setLayoutType(StringUtils.defaultIfBlank(pageSchema.getLayoutType(), "simple-crud"));
        config.setModelSchema(writeJson(modelSchema, "modelSchema"));
        config.setPageSchema(writeJson(pageSchema, "pageSchema"));
        configService.save(config);
        return config;
    }

    private AiCrudConfig updateDraft(LowcodeAppDraftDTO dto) {
        AiCrudConfig config = requireConfig(dto.getId());
        if (StringUtils.isNotBlank(dto.getConfigKey()) && !dto.getConfigKey().equals(config.getConfigKey())) {
            throw new BusinessException("configKey创建后不允许修改");
        }
        LowcodeModelSchema modelSchema = resolveModelSchema(config, dto);
        LowcodePageSchema pageSchema = resolvePageSchema(config, dto, modelSchema);
        schemaValidator.validatePage(pageSchema, modelSchema);

        config.setTableName(modelSchema.getTableName());
        config.setTableComment(modelSchema.getBusinessName());
        config.setAppName(resolveAppName(config, dto, modelSchema));
        if (dto.getMenuName() != null) {
            config.setMenuName(dto.getMenuName());
        }
        if (dto.getMenuParentId() != null) {
            config.setMenuParentId(dto.getMenuParentId());
        }
        if (dto.getMenuSort() != null) {
            config.setMenuSort(dto.getMenuSort());
        }
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setStatus("0");
        if (StringUtils.isBlank(config.getPublishStatus())) {
            config.setPublishStatus("DRAFT");
        }
        config.setDraftVersion((config.getDraftVersion() == null ? 0 : config.getDraftVersion()) + 1);
        config.setLayoutType(StringUtils.defaultIfBlank(pageSchema.getLayoutType(), "simple-crud"));
        config.setModelSchema(writeJson(modelSchema, "modelSchema"));
        config.setPageSchema(writeJson(pageSchema, "pageSchema"));
        configService.updateById(config);
        return config;
    }

    private LowcodeModelSchema resolveModelSchema(AiCrudConfig existing, LowcodeAppDraftDTO draft) {
        if (draft != null && draft.getModelSchema() != null) {
            return draft.getModelSchema();
        }
        if (existing != null && StringUtils.isNotBlank(existing.getModelSchema())) {
            return readModelSchema(existing);
        }
        throw new BusinessException("数据模型不能为空");
    }

    private LowcodePageSchema resolvePageSchema(AiCrudConfig existing, LowcodeAppDraftDTO draft,
                                                LowcodeModelSchema modelSchema) {
        if (draft != null && draft.getPageSchema() != null) {
            return draft.getPageSchema();
        }
        if (existing != null && StringUtils.isNotBlank(existing.getPageSchema())) {
            return readPageSchema(existing);
        }
        return buildDefaultPageSchema(modelSchema);
    }

    private String resolveConfigKey(AiCrudConfig existing, LowcodeAppDraftDTO draft) {
        String configKey = draft != null ? StringUtils.trimToNull(draft.getConfigKey()) : null;
        if (configKey == null && existing != null) {
            configKey = existing.getConfigKey();
        }
        validateConfigKey(configKey);
        return configKey;
    }

    private String resolveAppName(AiCrudConfig existing, LowcodeAppDraftDTO draft, LowcodeModelSchema modelSchema) {
        if (draft != null && StringUtils.isNotBlank(draft.getAppName())) {
            return draft.getAppName();
        }
        if (existing != null && StringUtils.isNotBlank(existing.getAppName())) {
            return existing.getAppName();
        }
        return StringUtils.defaultIfBlank(modelSchema.getBusinessName(), modelSchema.getTableName());
    }

    private void validateConfigKey(String configKey) {
        if (StringUtils.isBlank(configKey) || !CONFIG_KEY_PATTERN.matcher(configKey).matches()) {
            throw new BusinessException("configKey格式不正确（小写字母开头，仅含小写字母+数字+下划线，2-64字符）");
        }
    }

    private LowcodePageZone buildZone(String zoneKey, String componentKey, boolean enabled, List<String> fieldRefs) {
        LowcodePageZone zone = new LowcodePageZone();
        zone.setZoneKey(zoneKey);
        zone.setComponentKey(componentKey);
        zone.setEnabled(enabled);
        zone.setFieldRefs(fieldRefs);
        return zone;
    }

    private LowcodeAppDetailVO toDetailVO(AiCrudConfig config) {
        LowcodeAppDetailVO vo = new LowcodeAppDetailVO();
        vo.setId(config.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setTableName(config.getTableName());
        vo.setTableComment(config.getTableComment());
        vo.setAppName(config.getAppName());
        vo.setMode(config.getMode());
        vo.setBuildMode(config.getBuildMode());
        vo.setStatus(config.getStatus());
        vo.setPublishStatus(config.getPublishStatus());
        vo.setMenuName(config.getMenuName());
        vo.setMenuParentId(config.getMenuParentId());
        vo.setMenuSort(config.getMenuSort());
        vo.setMenuResourceId(config.getMenuResourceId());
        vo.setLayoutType(config.getLayoutType());
        vo.setModelSchema(readJsonObject(config.getModelSchema()));
        vo.setPageSchema(readJsonObject(config.getPageSchema()));
        vo.setDraftVersion(config.getDraftVersion());
        vo.setPublishedVersion(config.getPublishedVersion());
        vo.setPublishTime(config.getPublishTime());
        vo.setPublishBy(config.getPublishBy());
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }

    private Object readJsonObject(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return readJson(json, Object.class, "JSON");
    }

    private <T> T readJson(String json, Class<T> type, String fieldName) {
        if (StringUtils.isBlank(json)) {
            throw new BusinessException(fieldName + "不能为空");
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new BusinessException(fieldName + "格式不正确");
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
