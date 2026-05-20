package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfigVersion;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePublishDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigVersionMapper;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeVersionVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 低代码应用发布、版本和回滚服务。
 */
@Service
@RequiredArgsConstructor
public class LowcodePublishService {

    private static final String DEPLOY_SKIP_DDL = "SKIP_DDL";
    private static final String DEPLOY_ONLINE_CREATE_TABLE = "ONLINE_CREATE_TABLE";
    private static final String DDL_PERMISSION = "ai:lowcode:deploy-ddl";

    private final ObjectMapper objectMapper;
    private final AiCrudConfigService configService;
    private final LowcodeAppService appService;
    private final LowcodeRuntimeConfigBuilder runtimeConfigBuilder;
    private final LowcodeSchemaValidator schemaValidator;
    private final LowcodeDdlService ddlService;
    private final MenuRegisterAdapter menuRegisterAdapter;
    private final AiCrudConfigVersionMapper versionMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long id, LowcodePublishDTO dto) {
        AiCrudConfig config = appService.requireConfig(id);
        LowcodeModelSchema modelSchema = resolvePublishModel(config, dto);
        LowcodePageSchema pageSchema = resolvePublishPage(config, dto, modelSchema);
        schemaValidator.validatePage(pageSchema, modelSchema);
        ensureTableReady(modelSchema, dto);

        LowcodeRuntimeConfig runtimeConfig = runtimeConfigBuilder.buildRuntimeConfig(config.getConfigKey(), modelSchema, pageSchema);
        applyRuntimeConfig(config, modelSchema, pageSchema, runtimeConfig);
        applyMenuConfig(config, dto);
        int versionNo = nextVersionNo(config);
        config.setPublishStatus("PUBLISHED");
        config.setPublishedVersion(versionNo);
        config.setPublishTime(LocalDateTime.now());
        config.setPublishBy(SessionHelper.getUserId());

        registerOrUpdateMenu(config);
        configService.updateById(config);
        AiCrudConfigVersion version = createVersion(config, versionNo, "publish",
                dto != null ? dto.getRemark() : null);
        return version.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollback(Long id, Long versionId) {
        AiCrudConfig config = appService.requireConfig(id);
        AiCrudConfigVersion targetVersion = versionMapper.selectVersionById(
                resolveTenantId(config), config.getId(), versionId);
        if (targetVersion == null) {
            throw new BusinessException("版本不存在或不属于当前应用");
        }

        Map<String, Object> snapshot = readSnapshot(targetVersion.getPublishSnapshot());
        LowcodeModelSchema modelSchema = readVersionModel(targetVersion);
        LowcodePageSchema pageSchema = readVersionPage(targetVersion);
        LowcodeRuntimeConfig runtimeConfig = runtimeConfigBuilder.buildRuntimeConfig(config.getConfigKey(), modelSchema, pageSchema);
        applyRuntimeConfig(config, modelSchema, pageSchema, runtimeConfig);
        applyVersionRuntimeFields(config, targetVersion, snapshot, runtimeConfig);
        applySnapshotMenuFields(config, snapshot);

        int versionNo = nextVersionNo(config);
        config.setPublishStatus("PUBLISHED");
        config.setPublishedVersion(versionNo);
        config.setPublishTime(LocalDateTime.now());
        config.setPublishBy(SessionHelper.getUserId());
        registerOrUpdateMenu(config);
        configService.updateById(config);
        createVersion(config, versionNo, "rollback", "回滚到版本 " + targetVersion.getVersionNo());
    }

    public List<LowcodeVersionVO> listVersions(Long id) {
        AiCrudConfig config = appService.requireConfig(id);
        return versionMapper.selectByConfigId(resolveTenantId(config), config.getId()).stream()
                .map(this::toVersionVO)
                .toList();
    }

    private LowcodeModelSchema resolvePublishModel(AiCrudConfig config, LowcodePublishDTO dto) {
        if (dto != null && dto.getModelSchema() != null) {
            return dto.getModelSchema();
        }
        return appService.readModelSchema(config);
    }

    private LowcodePageSchema resolvePublishPage(AiCrudConfig config, LowcodePublishDTO dto,
                                                 LowcodeModelSchema modelSchema) {
        if (dto != null && dto.getPageSchema() != null) {
            return dto.getPageSchema();
        }
        if (StringUtils.isNotBlank(config.getPageSchema())) {
            return appService.readPageSchema(config);
        }
        return appService.buildDefaultPageSchema(modelSchema);
    }

    private void ensureTableReady(LowcodeModelSchema modelSchema, LowcodePublishDTO dto) {
        String deployMode = dto != null && StringUtils.isNotBlank(dto.getDeployMode())
                ? dto.getDeployMode()
                : DEPLOY_SKIP_DDL;
        if (DEPLOY_ONLINE_CREATE_TABLE.equals(deployMode)) {
            if (!Boolean.TRUE.equals(dto.getConfirmOnlineDdl())) {
                throw new BusinessException("在线建表发布需要二次确认");
            }
            if (!SessionHelper.hasPermission(DDL_PERMISSION)) {
                throw new BusinessException("缺少在线建表发布权限: " + DDL_PERMISSION);
            }
            ddlService.executeCreateTable(modelSchema);
            return;
        }
        if (!ddlService.tableExists(modelSchema.getTableName())) {
            throw new BusinessException("数据表不存在，请先由DBA建表，或选择在线建表发布");
        }
    }

    private void applyRuntimeConfig(AiCrudConfig config,
                                    LowcodeModelSchema modelSchema,
                                    LowcodePageSchema pageSchema,
                                    LowcodeRuntimeConfig runtimeConfig) {
        config.setTableName(runtimeConfig.getTableName());
        config.setTableComment(runtimeConfig.getTableComment());
        config.setAppName(StringUtils.defaultIfBlank(config.getAppName(), runtimeConfig.getTableComment()));
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setStatus("0");
        config.setLayoutType(runtimeConfig.getLayoutType());
        config.setModelSchema(appService.writeJson(modelSchema, "modelSchema"));
        config.setPageSchema(appService.writeJson(pageSchema, "pageSchema"));
        config.setSearchSchema(runtimeConfig.getSearchSchema());
        config.setColumnsSchema(runtimeConfig.getColumnsSchema());
        config.setEditSchema(runtimeConfig.getEditSchema());
        config.setApiConfig(runtimeConfig.getApiConfig());
        config.setOptions(runtimeConfig.getOptions());
        config.setDictConfig(runtimeConfig.getDictConfig());
        config.setDesensitizeConfig(runtimeConfig.getDesensitizeConfig());
        config.setEncryptConfig(runtimeConfig.getEncryptConfig());
        config.setTransConfig(runtimeConfig.getTransConfig());
    }

    private void applyMenuConfig(AiCrudConfig config, LowcodePublishDTO dto) {
        if (dto == null) {
            return;
        }
        if (StringUtils.isNotBlank(dto.getMenuName())) {
            config.setMenuName(dto.getMenuName());
        }
        if (dto.getMenuParentId() != null) {
            config.setMenuParentId(dto.getMenuParentId());
        }
        if (dto.getMenuSort() != null) {
            config.setMenuSort(dto.getMenuSort());
        }
    }

    private void registerOrUpdateMenu(AiCrudConfig config) {
        String menuName = StringUtils.defaultIfBlank(config.getMenuName(),
                StringUtils.defaultIfBlank(config.getAppName(), config.getTableComment()));
        Long parentId = config.getMenuParentId() != null
                ? config.getMenuParentId()
                : menuRegisterAdapter.resolveDefaultLowcodeParentId();
        Integer sort = config.getMenuSort() != null ? config.getMenuSort() : 0;

        if (config.getMenuResourceId() == null) {
            Long menuResourceId = menuRegisterAdapter.registerMenu(menuName, parentId, config.getConfigKey(), sort);
            config.setMenuResourceId(menuResourceId);
        } else {
            menuRegisterAdapter.updateMenu(config.getMenuResourceId(), menuName, sort);
        }
        config.setMenuName(menuName);
        config.setMenuParentId(parentId);
        config.setMenuSort(sort);
    }

    private int nextVersionNo(AiCrudConfig config) {
        Integer maxVersionNo = versionMapper.selectMaxVersionNo(resolveTenantId(config), config.getId());
        return (maxVersionNo == null ? 0 : maxVersionNo) + 1;
    }

    private AiCrudConfigVersion createVersion(AiCrudConfig config, Integer versionNo, String versionType, String remark) {
        AiCrudConfigVersion version = new AiCrudConfigVersion();
        version.setTenantId(resolveTenantId(config));
        version.setConfigId(config.getId());
        version.setConfigKey(config.getConfigKey());
        version.setVersionNo(versionNo);
        version.setVersionType(versionType);
        version.setModelSchema(config.getModelSchema());
        version.setPageSchema(config.getPageSchema());
        version.setSearchSchema(config.getSearchSchema());
        version.setColumnsSchema(config.getColumnsSchema());
        version.setEditSchema(config.getEditSchema());
        version.setApiConfig(config.getApiConfig());
        version.setOptions(config.getOptions());
        version.setPublishSnapshot(writeSnapshot(config));
        version.setRemark(StringUtils.defaultIfBlank(remark, "发布低代码应用"));
        versionMapper.insert(version);
        return version;
    }

    private void applyVersionRuntimeFields(AiCrudConfig config,
                                           AiCrudConfigVersion version,
                                           Map<String, Object> snapshot,
                                           LowcodeRuntimeConfig fallback) {
        config.setSearchSchema(StringUtils.defaultIfBlank(version.getSearchSchema(), fallback.getSearchSchema()));
        config.setColumnsSchema(StringUtils.defaultIfBlank(version.getColumnsSchema(), fallback.getColumnsSchema()));
        config.setEditSchema(StringUtils.defaultIfBlank(version.getEditSchema(), fallback.getEditSchema()));
        config.setApiConfig(StringUtils.defaultIfBlank(version.getApiConfig(), fallback.getApiConfig()));
        config.setOptions(StringUtils.defaultIfBlank(version.getOptions(), fallback.getOptions()));
        config.setDictConfig(StringUtils.defaultIfBlank(text(snapshot.get("dictConfig")), fallback.getDictConfig()));
        config.setDesensitizeConfig(StringUtils.defaultIfBlank(text(snapshot.get("desensitizeConfig")), fallback.getDesensitizeConfig()));
        config.setEncryptConfig(StringUtils.defaultIfBlank(text(snapshot.get("encryptConfig")), fallback.getEncryptConfig()));
        config.setTransConfig(StringUtils.defaultIfBlank(text(snapshot.get("transConfig")), fallback.getTransConfig()));
        config.setLayoutType(StringUtils.defaultIfBlank(text(snapshot.get("layoutType")), fallback.getLayoutType()));
        config.setTableName(StringUtils.defaultIfBlank(text(snapshot.get("tableName")), fallback.getTableName()));
        config.setTableComment(StringUtils.defaultIfBlank(text(snapshot.get("tableComment")), fallback.getTableComment()));
        config.setAppName(StringUtils.defaultIfBlank(text(snapshot.get("appName")), config.getTableComment()));
    }

    private void applySnapshotMenuFields(AiCrudConfig config, Map<String, Object> snapshot) {
        config.setMenuName(StringUtils.defaultIfBlank(text(snapshot.get("menuName")),
                StringUtils.defaultIfBlank(config.getAppName(), config.getTableComment())));
        config.setMenuParentId(numberAsLong(snapshot.get("menuParentId"), config.getMenuParentId()));
        config.setMenuSort(numberAsInteger(snapshot.get("menuSort"), config.getMenuSort()));
    }

    private LowcodeModelSchema readVersionModel(AiCrudConfigVersion version) {
        return readJson(version.getModelSchema(), LowcodeModelSchema.class, "版本modelSchema");
    }

    private LowcodePageSchema readVersionPage(AiCrudConfigVersion version) {
        return readJson(version.getPageSchema(), LowcodePageSchema.class, "版本pageSchema");
    }

    private String writeSnapshot(AiCrudConfig config) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("configKey", config.getConfigKey());
        snapshot.put("tableName", config.getTableName());
        snapshot.put("tableComment", config.getTableComment());
        snapshot.put("appName", config.getAppName());
        snapshot.put("layoutType", config.getLayoutType());
        snapshot.put("dictConfig", config.getDictConfig());
        snapshot.put("desensitizeConfig", config.getDesensitizeConfig());
        snapshot.put("encryptConfig", config.getEncryptConfig());
        snapshot.put("transConfig", config.getTransConfig());
        snapshot.put("menuName", config.getMenuName());
        snapshot.put("menuParentId", config.getMenuParentId());
        snapshot.put("menuSort", config.getMenuSort());
        snapshot.put("menuResourceId", config.getMenuResourceId());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new BusinessException("发布快照生成失败");
        }
    }

    private Map<String, Object> readSnapshot(String snapshotJson) {
        if (StringUtils.isBlank(snapshotJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(snapshotJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new BusinessException("版本快照格式不正确");
        }
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

    private LowcodeVersionVO toVersionVO(AiCrudConfigVersion version) {
        LowcodeVersionVO vo = new LowcodeVersionVO();
        vo.setId(version.getId());
        vo.setConfigId(version.getConfigId());
        vo.setConfigKey(version.getConfigKey());
        vo.setVersionNo(version.getVersionNo());
        vo.setVersionType(version.getVersionType());
        vo.setRemark(version.getRemark());
        vo.setCreateTime(version.getCreateTime());
        vo.setCreateBy(version.getCreateBy());
        return vo;
    }

    private Long resolveTenantId(AiCrudConfig config) {
        if (config.getTenantId() != null) {
            return config.getTenantId();
        }
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long numberAsLong(Object value, Long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            return Long.valueOf(text);
        }
        return defaultValue;
    }

    private Integer numberAsInteger(Object value, Integer defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            return Integer.valueOf(text);
        }
        return defaultValue;
    }
}
