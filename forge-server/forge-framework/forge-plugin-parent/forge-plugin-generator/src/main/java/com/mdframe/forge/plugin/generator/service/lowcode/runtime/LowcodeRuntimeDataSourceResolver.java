package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAuditStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeLogicDeleteStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeDatasourceSnapshot;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeSourceTableRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTenantStrategy;
import com.mdframe.forge.plugin.generator.mapper.GenDatasourceMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 低代码运行时数据源解析器。
 */
@Service
@RequiredArgsConstructor
public class LowcodeRuntimeDataSourceResolver {

    private static final String DEFAULT_DB_TYPE = "MySQL";
    private static final String DEFAULT_PRIMARY_KEY = "id";
    private static final String TENANT_MODE_FORGE = "FORGE_TENANT_ID";
    private static final String AUDIT_MODE_FORGE_COLUMNS = "FORGE_COLUMNS";
    private static final String LOGIC_MODE_DEL_FLAG = "DEL_FLAG";

    private final ObjectMapper objectMapper;
    private final GenDatasourceMapper datasourceMapper;

    public LowcodeRuntimeDataSourceContext resolve(LowcodeModelSchema modelSchema) {
        if (modelSchema == null) {
            throw new BusinessException("低代码模型协议不能为空");
        }
        LowcodeRuntimeDatasourceSnapshot snapshot = resolveModelSnapshot(modelSchema);
        String tableName = StringUtils.firstNonBlank(
            snapshot == null ? null : snapshot.getTableName(),
            modelSchema.getTableName(),
            modelSchema.getSourceTable() == null ? null : modelSchema.getSourceTable().getTableName()
        );
        LowcodeRuntimeDataSourceContext context = resolveBySnapshot(snapshot, tableName, modelSchema.getTableMode());
        context.setPrimaryKey(defaultPrimaryKey(modelSchema.getPrimaryKey()));
        context.setTenantStrategy(defaultTenantStrategy(modelSchema.getTenantStrategy()));
        context.setAuditStrategy(defaultAuditStrategy(modelSchema.getAuditStrategy()));
        context.setLogicDeleteStrategy(defaultLogicDeleteStrategy(modelSchema.getLogicDeleteStrategy()));
        return context;
    }

    public LowcodeRuntimeDataSourceContext resolve(AiCrudConfig config) {
        if (config == null) {
            throw new BusinessException("低代码应用配置不能为空");
        }
        LowcodeModelSchema modelSchema = readModelSchema(config);
        LowcodeRuntimeDatasourceSnapshot snapshot = readJson(
            config.getRuntimeDatasourceSnapshot(), LowcodeRuntimeDatasourceSnapshot.class, "runtimeDatasourceSnapshot");
        if (snapshot == null && modelSchema != null) {
            snapshot = resolveModelSnapshot(modelSchema);
        }
        if (snapshot == null && (config.getRuntimeDatasourceId() != null
            || StringUtils.isNotBlank(config.getRuntimeDatasourceCode()))) {
            snapshot = new LowcodeRuntimeDatasourceSnapshot();
            snapshot.setDatasourceId(config.getRuntimeDatasourceId());
            snapshot.setDatasourceCode(config.getRuntimeDatasourceCode());
        }
        String tableName = StringUtils.firstNonBlank(
            config.getRuntimeTableName(),
            snapshot == null ? null : snapshot.getTableName(),
            config.getTableName(),
            modelSchema == null ? null : modelSchema.getTableName()
        );
        LowcodeRuntimeDataSourceContext context = resolveBySnapshot(snapshot, tableName,
            modelSchema == null ? null : modelSchema.getTableMode());
        context.setPrimaryKey(defaultPrimaryKey(resolveConfigPrimaryKey(config, modelSchema)));
        context.setTenantStrategy(defaultTenantStrategy(readJson(
            config.getTenantStrategy(), LowcodeTenantStrategy.class, "tenantStrategy",
            modelSchema == null ? null : modelSchema.getTenantStrategy())));
        context.setAuditStrategy(defaultAuditStrategy(readJson(
            config.getAuditStrategy(), LowcodeAuditStrategy.class, "auditStrategy",
            modelSchema == null ? null : modelSchema.getAuditStrategy())));
        context.setLogicDeleteStrategy(defaultLogicDeleteStrategy(readJson(
            config.getLogicDeleteStrategy(), LowcodeLogicDeleteStrategy.class, "logicDeleteStrategy",
            modelSchema == null ? null : modelSchema.getLogicDeleteStrategy())));
        return context;
    }

    public LowcodeRuntimeDatasourceSnapshot buildSnapshot(GenDatasource datasource, String tableName, String tableMode) {
        if (datasource == null) {
            return null;
        }
        LowcodeRuntimeDatasourceSnapshot snapshot = new LowcodeRuntimeDatasourceSnapshot();
        snapshot.setDatasourceId(datasource.getDatasourceId());
        snapshot.setDatasourceCode(datasource.getDatasourceCode());
        snapshot.setDatasourceName(datasource.getDatasourceName());
        snapshot.setDbType(StringUtils.defaultIfBlank(datasource.getDbType(), DEFAULT_DB_TYPE));
        snapshot.setTableName(tableName);
        snapshot.setTableMode(tableMode);
        snapshot.setUsageScope(datasource.getUsageScope());
        snapshot.setReadonly(Integer.valueOf(1).equals(datasource.getReadonly()));
        snapshot.setAllowWrite(!Boolean.TRUE.equals(snapshot.getReadonly())
            && Integer.valueOf(1).equals(datasource.getAllowRuntimeWrite()));
        snapshot.setAllowDdl(!Boolean.TRUE.equals(snapshot.getReadonly())
            && Integer.valueOf(1).equals(datasource.getAllowRuntimeDdl()));
        snapshot.setRiskLevel(datasource.getRiskLevel());
        return snapshot;
    }

    private LowcodeRuntimeDatasourceSnapshot resolveModelSnapshot(LowcodeModelSchema modelSchema) {
        if (modelSchema.getRuntimeDatasource() != null) {
            return modelSchema.getRuntimeDatasource();
        }
        LowcodeSourceTableRef sourceTable = modelSchema.getSourceTable();
        if (sourceTable == null || (sourceTable.getDatasourceId() == null
            && StringUtils.isBlank(sourceTable.getDatasourceCode()))) {
            return null;
        }
        LowcodeRuntimeDatasourceSnapshot snapshot = new LowcodeRuntimeDatasourceSnapshot();
        snapshot.setDatasourceId(sourceTable.getDatasourceId());
        snapshot.setDatasourceCode(sourceTable.getDatasourceCode());
        snapshot.setDatasourceName(sourceTable.getDatasourceName());
        snapshot.setDbType(sourceTable.getDbType());
        snapshot.setTableName(StringUtils.firstNonBlank(sourceTable.getTableName(), modelSchema.getTableName()));
        snapshot.setTableMode(StringUtils.defaultIfBlank(modelSchema.getTableMode(), "EXISTING"));
        return snapshot;
    }

    private LowcodeRuntimeDataSourceContext resolveBySnapshot(LowcodeRuntimeDatasourceSnapshot snapshot,
                                                             String tableName,
                                                             String tableMode) {
        if (snapshot == null || (snapshot.getDatasourceId() == null
            && StringUtils.isBlank(snapshot.getDatasourceCode()))) {
            LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContext.master(tableName);
            context.setTableMode(tableMode);
            context.setSnapshot(null);
            return context;
        }
        GenDatasource datasource = findDatasource(snapshot);
        if (datasource == null) {
            throw new BusinessException("运行数据源不存在: " + StringUtils.firstNonBlank(
                snapshot.getDatasourceCode(),
                snapshot.getDatasourceId() == null ? null : String.valueOf(snapshot.getDatasourceId())));
        }
        if (Integer.valueOf(0).equals(datasource.getIsEnabled())) {
            throw new BusinessException("运行数据源已禁用: " + datasource.getDatasourceName());
        }
        String effectiveTableName = StringUtils.firstNonBlank(tableName, snapshot.getTableName());
        LowcodeRuntimeDatasourceSnapshot freshSnapshot = buildSnapshot(datasource, effectiveTableName,
            StringUtils.firstNonBlank(tableMode, snapshot.getTableMode()));

        LowcodeRuntimeDataSourceContext context = new LowcodeRuntimeDataSourceContext();
        context.setMaster(false);
        context.setDatasourceId(datasource.getDatasourceId());
        context.setDatasourceCode(datasource.getDatasourceCode());
        context.setDatasourceName(datasource.getDatasourceName());
        context.setDbType(StringUtils.defaultIfBlank(datasource.getDbType(), DEFAULT_DB_TYPE));
        context.setTableName(effectiveTableName);
        context.setTableMode(freshSnapshot.getTableMode());
        context.setAllowWrite(Boolean.TRUE.equals(freshSnapshot.getAllowWrite()));
        context.setAllowDdl(Boolean.TRUE.equals(freshSnapshot.getAllowDdl()));
        context.setReadonly(Boolean.TRUE.equals(freshSnapshot.getReadonly()));
        context.setRiskLevel(datasource.getRiskLevel());
        context.setSnapshot(freshSnapshot);
        return context;
    }

    private GenDatasource findDatasource(LowcodeRuntimeDatasourceSnapshot snapshot) {
        if (snapshot.getDatasourceId() != null) {
            return datasourceMapper.selectById(snapshot.getDatasourceId());
        }
        if (StringUtils.isBlank(snapshot.getDatasourceCode())) {
            return null;
        }
        return datasourceMapper.selectOne(new LambdaQueryWrapper<GenDatasource>()
            .eq(GenDatasource::getDatasourceCode, snapshot.getDatasourceCode())
            .last("LIMIT 1"));
    }

    private LowcodeModelSchema readModelSchema(AiCrudConfig config) {
        if (StringUtils.isBlank(config.getModelSchema())) {
            return null;
        }
        return readJson(config.getModelSchema(), LowcodeModelSchema.class, "modelSchema");
    }

    private LowcodePrimaryKeyStrategy resolveConfigPrimaryKey(AiCrudConfig config, LowcodeModelSchema modelSchema) {
        if (StringUtils.isNotBlank(config.getPrimaryKeyField()) || StringUtils.isNotBlank(config.getPrimaryKeyColumn())) {
            LowcodePrimaryKeyStrategy primaryKey = new LowcodePrimaryKeyStrategy();
            primaryKey.setField(config.getPrimaryKeyField());
            primaryKey.setColumnName(config.getPrimaryKeyColumn());
            primaryKey.setDataType(config.getPrimaryKeyType());
            primaryKey.setAutoIncrement(true);
            return primaryKey;
        }
        return modelSchema == null ? null : modelSchema.getPrimaryKey();
    }

    private LowcodePrimaryKeyStrategy defaultPrimaryKey(LowcodePrimaryKeyStrategy primaryKey) {
        LowcodePrimaryKeyStrategy value = primaryKey == null ? new LowcodePrimaryKeyStrategy() : primaryKey;
        value.setField(StringUtils.defaultIfBlank(value.getField(), DEFAULT_PRIMARY_KEY));
        value.setColumnName(StringUtils.defaultIfBlank(value.getColumnName(), DEFAULT_PRIMARY_KEY));
        value.setDataType(StringUtils.defaultIfBlank(value.getDataType(), "bigint"));
        value.setAutoIncrement(value.getAutoIncrement() == null || value.getAutoIncrement());
        return value;
    }

    private LowcodeTenantStrategy defaultTenantStrategy(LowcodeTenantStrategy tenantStrategy) {
        LowcodeTenantStrategy value = tenantStrategy == null ? new LowcodeTenantStrategy() : tenantStrategy;
        value.setMode(StringUtils.defaultIfBlank(value.getMode(), TENANT_MODE_FORGE));
        value.setColumnName(StringUtils.defaultIfBlank(value.getColumnName(), "tenant_id"));
        return value;
    }

    private LowcodeAuditStrategy defaultAuditStrategy(LowcodeAuditStrategy auditStrategy) {
        LowcodeAuditStrategy value = auditStrategy == null ? new LowcodeAuditStrategy() : auditStrategy;
        value.setMode(StringUtils.defaultIfBlank(value.getMode(), AUDIT_MODE_FORGE_COLUMNS));
        value.setCreateByColumn(StringUtils.defaultIfBlank(value.getCreateByColumn(), "create_by"));
        value.setCreateTimeColumn(StringUtils.defaultIfBlank(value.getCreateTimeColumn(), "create_time"));
        value.setCreateDeptColumn(StringUtils.defaultIfBlank(value.getCreateDeptColumn(), "create_dept"));
        value.setUpdateByColumn(StringUtils.defaultIfBlank(value.getUpdateByColumn(), "update_by"));
        value.setUpdateTimeColumn(StringUtils.defaultIfBlank(value.getUpdateTimeColumn(), "update_time"));
        return value;
    }

    private LowcodeLogicDeleteStrategy defaultLogicDeleteStrategy(LowcodeLogicDeleteStrategy logicDeleteStrategy) {
        LowcodeLogicDeleteStrategy value = logicDeleteStrategy == null ? new LowcodeLogicDeleteStrategy() : logicDeleteStrategy;
        value.setMode(StringUtils.defaultIfBlank(value.getMode(), LOGIC_MODE_DEL_FLAG));
        value.setColumnName(StringUtils.defaultIfBlank(value.getColumnName(), "del_flag"));
        value.setActiveValue(StringUtils.defaultIfBlank(value.getActiveValue(), "0"));
        value.setDeletedValue(StringUtils.defaultIfBlank(value.getDeletedValue(), "1"));
        return value;
    }

    private <T> T readJson(String json, Class<T> type, String fieldName) {
        return readJson(json, type, fieldName, null);
    }

    private <T> T readJson(String json, Class<T> type, String fieldName, T defaultValue) {
        if (StringUtils.isBlank(json)) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new BusinessException(fieldName + "格式不正确");
        }
    }
}
