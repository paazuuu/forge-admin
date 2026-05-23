package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.CustomQueryExecuteDTO;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTreeConfig;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.crypto.Encryptor;
import com.mdframe.forge.starter.crypto.crypto.EncryptorFactory;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategy;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategyFactory;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeType;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态CRUD服务
 * 基于DynamicCrudRepository实现，支持配置驱动的通用CRUD操作
 * 
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicCrudService {

    private static final int MAX_EXPORT_ROWS = 10000;
    private static final int MAX_TREE_ROWS = 5000;
    private static final String MASTER_DETAIL_LAYOUT = "master-detail-crud";
    private static final Set<String> IMMUTABLE_WRITE_FIELDS = Set.of(
            "id", "tenantId", "tenant_id", "createBy", "create_by", "createTime", "create_time",
            "createDept", "create_dept", "updateBy", "update_by", "updateTime", "update_time", "delFlag", "del_flag"
    );

    private record RuntimeFieldRef(String fieldName,
                                   String modelCode,
                                   String sourceField,
                                   String columnName,
                                   String tableName,
                                   String tableAlias,
                                   boolean primary) {
    }

    private record RuntimeChildRelation(String modelCode,
                                        String tableName,
                                        String tableAlias,
                                        String childFkColumn,
                                        String mainColumn,
                                        Map<String, RuntimeFieldRef> fields) {
    }

    private record RuntimeJoinContext(Map<String, RuntimeFieldRef> fields,
                                      List<RuntimeChildRelation> childRelations,
                                      List<DynamicCrudRepository.JoinField> selectFields,
                                      List<DynamicCrudRepository.JoinSpec> joins,
                                      Map<String, String> fieldColumnMapping) {
    }

    private final DynamicCrudRepository repository;
    private final AiCrudConfigService configService;
    private final ObjectMapper objectMapper;
    private final DictValueProvider dictValueProvider;
    private final DesensitizeStrategyFactory desensitizeStrategyFactory;
    private final EncryptorFactory encryptorFactory;

    // ==================== 查询操作 ====================

    /**
     * 分页查询
     */
    public Page<Map<String, Object>> selectPage(String configKey, PageQuery pageQuery, DynamicCrudQuery query) {
        // 1. 加载配置
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 2. 获取字段映射
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        
        // 3. 解析搜索配置
        Set<String> allowedSearchFields = DynamicQueryGenerator.extractFieldNames(config.getSearchSchema(), objectMapper);
        Map<String, String> searchTypeMap = DynamicQueryGenerator.extractSearchTypeMap(config.getSearchSchema(), objectMapper);
        
        // 4. 构建搜索条件
        Map<String, Object> searchParams = (query != null) ? query.getSearchParams() : null;

        RuntimeJoinContext joinContext = buildRuntimeJoinContext(config);
        if (joinContext != null && requiresJoinedPageQuery(config, pageQuery, searchParams, joinContext)) {
            Page<Map<String, Object>> page = repository.selectJoinedPage(
                    tableName,
                    buildRuntimeSelectFields(joinContext, DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), true),
                    joinContext.joins(),
                    pageQuery.getPageNum(),
                    pageQuery.getPageSize(),
                    searchParams,
                    allowedSearchFields,
                    searchTypeMap,
                    joinContext.fieldColumnMapping(),
                    buildJoinOrderBy(pageQuery.getOrderByColumn(), pageQuery.getIsAsc(), joinContext)
            );
            applyReadPipeline(page.getRecords(), config);
            return page;
        }
        
        // 5. 构建排序
        String orderBy = DynamicQueryGenerator.buildOrderByClause(
                pageQuery.getOrderByColumn(), pageQuery.getIsAsc(), columnMapping);
        
        // 6. 执行分页查询
        Page<Map<String, Object>> page = repository.selectPage(
                tableName,
                pageQuery.getPageNum(),
                pageQuery.getPageSize(),
                searchParams,
                allowedSearchFields,
                searchTypeMap,
                columnMapping,
                orderBy
        );
        
        // 7. 转换字段名为camelCase
        List<Map<String, Object>> camelCaseRecords = DynamicQueryGenerator.convertListToCamelCase(page.getRecords());
        
        // 8. 读取链路统一先解密，再做翻译和脱敏，避免密文参与展示处理
        applyReadPipeline(camelCaseRecords, config);
        
        page.setRecords(camelCaseRecords);
        return page;
    }

    /**
     * 查询动态导出数据，复用动态 CRUD 的字段白名单、解密、字典翻译和脱敏链路。
     */
    public List<Map<String, Object>> selectExportRows(String configKey,
                                                      DynamicCrudQuery query,
                                                      Integer maxRows) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        Set<String> allowedSearchFields = DynamicQueryGenerator.extractFieldNames(config.getSearchSchema(), objectMapper);
        Map<String, String> searchTypeMap = DynamicQueryGenerator.extractSearchTypeMap(config.getSearchSchema(), objectMapper);
        Map<String, Object> searchParams = query != null ? query.getSearchParams() : null;
        int limit = normalizeExportLimit(maxRows);

        RuntimeJoinContext joinContext = buildRuntimeJoinContext(config);
        if (joinContext != null && requiresJoinedExportQuery(config, searchParams, joinContext)) {
            Page<Map<String, Object>> page = repository.selectJoinedPage(
                    tableName,
                    buildRuntimeSelectFields(joinContext, DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), true),
                    joinContext.joins(),
                    1,
                    limit,
                    searchParams,
                    allowedSearchFields,
                    searchTypeMap,
                    joinContext.fieldColumnMapping(),
                    "t0.id DESC"
            );
            applyReadPipeline(page.getRecords(), config);
            return page.getRecords();
        }

        List<Map<String, Object>> rows = repository.selectList(
                tableName,
                searchParams,
                allowedSearchFields,
                searchTypeMap,
                columnMapping,
                "id DESC",
                limit
        );

        List<Map<String, Object>> camelCaseRows = DynamicQueryGenerator.convertListToCamelCase(rows);
        applyReadPipeline(camelCaseRows, config);
        return camelCaseRows;
    }

    /**
     * 查询树形导航数据，供树形单表模板左侧树使用。
     */
    public List<Map<String, Object>> selectTree(String configKey) {
        AiCrudConfig config = getConfig(configKey);
        LowcodeTreeConfig treeConfig = resolveTreeConfig(config);
        String tableName = StringUtils.defaultIfBlank(treeConfig.getSourceTableName(), config.getTableName());
        List<Map<String, Object>> rows = repository.selectList(
                tableName,
                null,
                Collections.emptySet(),
                Collections.emptyMap(),
                repository.getColumnMapping(tableName),
                "id ASC",
                MAX_TREE_ROWS
        );

        List<Map<String, Object>> camelCaseRows = DynamicQueryGenerator.convertListToCamelCase(rows);
        applyReadPipeline(camelCaseRows, config);
        return buildTree(camelCaseRows, treeConfig);
    }

    /**
     * 自定义分页查询。
     */
    public Page<Map<String, Object>> selectCustomPage(String configKey, CustomQueryExecuteDTO request) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        Set<String> allowedFields = buildAllowedCustomFields(config);

        RuntimeJoinContext joinContext = buildRuntimeJoinContext(config);
        if (joinContext != null) {
            allowedFields.addAll(joinContext.fields().keySet());
            if (requiresJoinedCustomQuery(request, joinContext)) {
                Page<Map<String, Object>> page = repository.selectJoinedCustomPage(
                        tableName,
                        buildRuntimeSelectFields(joinContext, request.getFields(), true),
                        joinContext.joins(),
                        normalizePageNum(request.getPageNum()),
                        normalizePageSize(request.getPageSize()),
                        request.getConditions(),
                        allowedFields,
                        joinContext.fieldColumnMapping(),
                        buildJoinOrderBy(request.getOrderByColumn(), request.getIsAsc(), joinContext)
                );
                applyReadPipeline(page.getRecords(), config);
                return page;
            }
        }

        String orderBy = DynamicQueryGenerator.buildOrderByClause(
                request.getOrderByColumn(), request.getIsAsc(), columnMapping);

        Page<Map<String, Object>> page = repository.selectCustomPage(
                tableName,
                normalizePageNum(request.getPageNum()),
                normalizePageSize(request.getPageSize()),
                request.getFields(),
                request.getConditions(),
                allowedFields,
                columnMapping,
                orderBy
        );

        List<Map<String, Object>> camelCaseRecords = DynamicQueryGenerator.convertListToCamelCase(page.getRecords());
        applyReadPipeline(camelCaseRecords, config);
        page.setRecords(camelCaseRecords);
        return page;
    }

    /**
     * 根据ID查询
     */
    public Map<String, Object> selectById(String configKey, Long id) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();

        RuntimeJoinContext joinContext = buildRuntimeJoinContext(config);
        if (isMasterDetailRuntime(config) && joinContext != null) {
            return selectMasterDetailById(config, id, joinContext);
        }
        if (joinContext != null) {
            Map<String, Object> record = repository.selectJoinedById(tableName, id, joinContext.selectFields(), joinContext.joins());
            if (record == null) {
                return null;
            }
            applyReadPipeline(Collections.singletonList(record), config);
            return record;
        }
        
        Map<String, Object> record = repository.selectById(tableName, id);
        if (record == null) {
            return null;
        }
        
        // 转换为camelCase
        Map<String, Object> camelCaseRecord = DynamicQueryGenerator.convertMapToCamelCase(record);
        
        // 单条读取同样遵循“解密 -> 翻译 -> 脱敏”顺序
        applyReadPipeline(Collections.singletonList(camelCaseRecord), config);
        
        return camelCaseRecord;
    }

    // ==================== 新增操作 ====================

    /**
     * 新增
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert(String configKey, Map<String, Object> data) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 获取字段映射
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        
        // 获取允许写入的字段
        Set<String> allowedFields = DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper);

        RuntimeJoinContext joinContext = buildRuntimeJoinContext(config);
        if (isMasterDetailRuntime(config) && joinContext != null) {
            insertMasterDetailData(config, data, allowedFields, joinContext);
            return;
        }
        if (joinContext != null) {
            insertJoinedData(config, data, allowedFields, joinContext);
            return;
        }
        
        // 过滤并转换字段名
        Map<String, Object> filteredData = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (isImmutableWriteField(entry.getKey())) {
                continue;
            }
            if (allowedFields.contains(entry.getKey())) {
                String columnName = columnMapping.getOrDefault(entry.getKey(), DynamicQueryGenerator.camelToSnake(entry.getKey()));
                filteredData.put(columnName, entry.getValue());
            }
        }
        
        if (filteredData.isEmpty()) {
            throw new BusinessException("没有可写入的字段");
        }
        
        // 应用加密
        applyEncrypt(filteredData, config.getEncryptConfig());
        
        // 执行插入
        repository.insert(tableName, filteredData);
    }

    // ==================== 更新操作 ====================

    /**
     * 更新
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateById(String configKey, Map<String, Object> data) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 获取ID
        Object idValue = resolvePayloadId(data);
        if (idValue == null) {
            throw new BusinessException("更新操作缺少id");
        }
        Long id = Long.valueOf(idValue.toString());
        
        // 获取字段映射
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        
        // 获取允许写入的字段
        Set<String> allowedFields = DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper);

        RuntimeJoinContext joinContext = buildRuntimeJoinContext(config);
        if (isMasterDetailRuntime(config) && joinContext != null) {
            updateMasterDetailData(config, id, data, allowedFields, joinContext);
            return;
        }
        if (joinContext != null) {
            updateJoinedData(config, id, data, allowedFields, joinContext);
            return;
        }
        
        // 过滤并转换字段名
        Map<String, Object> filteredData = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (isImmutableWriteField(key)) {
                continue;
            }
            if (allowedFields.contains(key)) {
                String columnName = columnMapping.getOrDefault(key, DynamicQueryGenerator.camelToSnake(key));
                filteredData.put(columnName, entry.getValue());
            }
        }
        
        if (filteredData.isEmpty()) {
            throw new BusinessException("没有可更新的字段");
        }
        
        // 应用加密
        applyEncrypt(filteredData, config.getEncryptConfig());
        
        // 执行更新
        repository.updateById(tableName, id, filteredData);
    }

    private Map<String, Object> selectMasterDetailById(AiCrudConfig config,
                                                       Long id,
                                                       RuntimeJoinContext joinContext) {
        Map<String, Object> record = repository.selectById(config.getTableName(), id);
        if (record == null) {
            return null;
        }
        Map<String, Object> main = DynamicQueryGenerator.convertMapToCamelCase(record);
        applyReadPipeline(Collections.singletonList(main), config);

        Map<String, Object> children = new LinkedHashMap<>();
        for (RuntimeChildRelation relation : joinContext.childRelations()) {
            Object relationValue = resolveMainRelationValue(relation, record, id, record);
            if (relationValue == null) {
                children.put(relation.modelCode(), List.of());
                continue;
            }
            List<Map<String, Object>> rows = repository.selectListByColumn(
                    relation.tableName(), relation.childFkColumn(), relationValue);
            children.put(relation.modelCode(), DynamicQueryGenerator.convertListToCamelCase(rows));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("main", main);
        result.put("children", children);
        return result;
    }

    private void insertMasterDetailData(AiCrudConfig config,
                                        Map<String, Object> data,
                                        Set<String> allowedFields,
                                        RuntimeJoinContext joinContext) {
        Map<String, Object> mainPayload = extractMainPayload(data);
        Map<String, Object> primaryData = filterPrimaryWriteData(mainPayload, allowedFields, joinContext);
        if (primaryData.isEmpty()) {
            throw new BusinessException("没有可写入的主表字段");
        }

        applyEncrypt(primaryData, config.getEncryptConfig());
        Long mainId = repository.insertReturningId(config.getTableName(), primaryData);
        Map<String, Object> currentMainRecord = null;
        Map<String, Object> childrenPayload = extractChildrenPayload(data);
        for (RuntimeChildRelation relation : joinContext.childRelations()) {
            List<Map<String, Object>> childRows = normalizeChildRows(childrenPayload.get(relation.modelCode()));
            if (childRows.isEmpty()) {
                continue;
            }
            if (currentMainRecord == null && !"id".equals(relation.mainColumn()) && !primaryData.containsKey(relation.mainColumn())) {
                currentMainRecord = repository.selectById(config.getTableName(), mainId);
            }
            Object relationValue = resolveMainRelationValue(relation, primaryData, mainId, currentMainRecord);
            if (relationValue == null) {
                continue;
            }
            for (Map<String, Object> row : childRows) {
                Map<String, Object> childData = filterChildWriteData(row, relation);
                if (!hasWritableChildData(childData)) {
                    continue;
                }
                childData.put(relation.childFkColumn(), relationValue);
                repository.insert(relation.tableName(), childData);
            }
        }
    }

    private void updateMasterDetailData(AiCrudConfig config,
                                        Long id,
                                        Map<String, Object> data,
                                        Set<String> allowedFields,
                                        RuntimeJoinContext joinContext) {
        Map<String, Object> mainPayload = extractMainPayload(data);
        Map<String, Object> primaryData = filterPrimaryWriteData(mainPayload, allowedFields, joinContext);
        if (!primaryData.isEmpty()) {
            applyEncrypt(primaryData, config.getEncryptConfig());
            repository.updateById(config.getTableName(), id, primaryData);
        }

        Map<String, Object> childrenPayload = extractChildrenPayload(data);
        Map<String, Object> currentMainRecord = null;
        boolean childrenChanged = false;
        for (RuntimeChildRelation relation : joinContext.childRelations()) {
            if (!childrenPayload.containsKey(relation.modelCode())) {
                continue;
            }
            if (currentMainRecord == null && !"id".equals(relation.mainColumn()) && !primaryData.containsKey(relation.mainColumn())) {
                currentMainRecord = repository.selectById(config.getTableName(), id);
            }
            Object relationValue = resolveMainRelationValue(relation, primaryData, id, currentMainRecord);
            if (relationValue == null) {
                continue;
            }
            childrenChanged = true;
            repository.deleteByColumn(
                    relation.tableName(),
                    relation.childFkColumn(),
                    relationValue,
                    repository.hasDelFlag(relation.tableName())
            );
            for (Map<String, Object> row : normalizeChildRows(childrenPayload.get(relation.modelCode()))) {
                Map<String, Object> childData = filterChildWriteData(row, relation);
                if (!hasWritableChildData(childData)) {
                    continue;
                }
                childData.put(relation.childFkColumn(), relationValue);
                repository.insert(relation.tableName(), childData);
            }
        }

        if (primaryData.isEmpty() && !childrenChanged) {
            throw new BusinessException("没有可更新的字段");
        }
    }

    private Map<String, Object> filterPrimaryWriteData(Map<String, Object> data,
                                                       Set<String> allowedFields,
                                                       RuntimeJoinContext joinContext) {
        Map<String, Object> primaryData = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            return primaryData;
        }
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (isImmutableWriteField(key) || !allowedFields.contains(key)) {
                continue;
            }
            RuntimeFieldRef fieldRef = joinContext.fields().get(key);
            if (fieldRef == null || !fieldRef.primary()) {
                continue;
            }
            primaryData.put(fieldRef.columnName(), entry.getValue());
        }
        return primaryData;
    }

    private Map<String, Object> filterChildWriteData(Map<String, Object> data, RuntimeChildRelation relation) {
        Map<String, Object> childData = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            return childData;
        }
        for (RuntimeFieldRef fieldRef : relation.fields().values()) {
            if (fieldRef.primary() || isImmutableWriteField(fieldRef.fieldName()) || isImmutableWriteField(fieldRef.sourceField())) {
                continue;
            }
            if (fieldRef.columnName().equals(relation.childFkColumn())) {
                continue;
            }
            Object value = firstPresent(data, fieldRef.sourceField(), fieldRef.fieldName(), fieldRef.columnName());
            if (value != null) {
                childData.put(fieldRef.columnName(), value);
            }
        }
        return childData;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMainPayload(Map<String, Object> data) {
        if (data != null && data.get("main") instanceof Map<?, ?> main) {
            return (Map<String, Object>) main;
        }
        return data == null ? Map.of() : data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractChildrenPayload(Map<String, Object> data) {
        if (data != null && data.get("children") instanceof Map<?, ?> children) {
            return (Map<String, Object>) children;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeChildRows(Object value) {
        if (value instanceof List<?> rows) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object row : rows) {
                if (row instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }
        if (value instanceof Map<?, ?> map) {
            return List.of((Map<String, Object>) map);
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Object resolvePayloadId(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object idValue = data.get("id");
        if (idValue != null) {
            return idValue;
        }
        Object main = data.get("main");
        if (main instanceof Map<?, ?> map) {
            return ((Map<String, Object>) map).get("id");
        }
        return null;
    }

    private Object firstPresent(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (StringUtils.isNotBlank(key) && data.containsKey(key)) {
                return data.get(key);
            }
        }
        return null;
    }

    private void insertJoinedData(AiCrudConfig config,
                                  Map<String, Object> data,
                                  Set<String> allowedFields,
                                  RuntimeJoinContext joinContext) {
        Map<String, Object> primaryData = new LinkedHashMap<>();
        Map<String, Map<String, Object>> childDataMap = new LinkedHashMap<>();
        splitRuntimeWriteData(data, allowedFields, joinContext, primaryData, childDataMap);
        if (primaryData.isEmpty()) {
            throw new BusinessException("没有可写入的主表字段");
        }
        applyEncrypt(primaryData, config.getEncryptConfig());
        Long mainId = repository.insertReturningId(config.getTableName(), primaryData);
        for (RuntimeChildRelation relation : joinContext.childRelations()) {
            Map<String, Object> childData = childDataMap.get(relation.modelCode());
            if (!hasWritableChildData(childData)) {
                continue;
            }
            Object relationValue = resolveMainRelationValue(relation, primaryData, mainId, null);
            if (relationValue == null) {
                continue;
            }
            childData.put(relation.childFkColumn(), relationValue);
            repository.insert(relation.tableName(), childData);
        }
    }

    private void updateJoinedData(AiCrudConfig config,
                                  Long id,
                                  Map<String, Object> data,
                                  Set<String> allowedFields,
                                  RuntimeJoinContext joinContext) {
        Map<String, Object> primaryData = new LinkedHashMap<>();
        Map<String, Map<String, Object>> childDataMap = new LinkedHashMap<>();
        splitRuntimeWriteData(data, allowedFields, joinContext, primaryData, childDataMap);

        if (!primaryData.isEmpty()) {
            applyEncrypt(primaryData, config.getEncryptConfig());
            repository.updateById(config.getTableName(), id, primaryData);
        }

        Map<String, Object> currentMainRecord = null;
        for (RuntimeChildRelation relation : joinContext.childRelations()) {
            Map<String, Object> childData = childDataMap.get(relation.modelCode());
            if (!hasWritableChildData(childData)) {
                continue;
            }
            if (currentMainRecord == null && !"id".equals(relation.mainColumn()) && !primaryData.containsKey(relation.mainColumn())) {
                currentMainRecord = repository.selectById(config.getTableName(), id);
            }
            Object relationValue = resolveMainRelationValue(relation, primaryData, id, currentMainRecord);
            if (relationValue == null) {
                continue;
            }
            childData.put(relation.childFkColumn(), relationValue);
            Long childId = repository.selectFirstIdByColumn(relation.tableName(), relation.childFkColumn(), relationValue);
            if (childId == null) {
                repository.insert(relation.tableName(), childData);
            } else {
                repository.updateById(relation.tableName(), childId, childData);
            }
        }

        if (primaryData.isEmpty() && childDataMap.values().stream().noneMatch(this::hasWritableChildData)) {
            throw new BusinessException("没有可更新的字段");
        }
    }

    private void splitRuntimeWriteData(Map<String, Object> data,
                                       Set<String> allowedFields,
                                       RuntimeJoinContext joinContext,
                                       Map<String, Object> primaryData,
                                       Map<String, Map<String, Object>> childDataMap) {
        if (data == null || data.isEmpty()) {
            return;
        }
        Map<String, RuntimeChildRelation> relationMap = joinContext.childRelations().stream()
                .collect(Collectors.toMap(RuntimeChildRelation::modelCode, relation -> relation, (left, right) -> left, LinkedHashMap::new));
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (isImmutableWriteField(key) || !allowedFields.contains(key)) {
                continue;
            }
            RuntimeFieldRef fieldRef = joinContext.fields().get(key);
            if (fieldRef == null) {
                continue;
            }
            if (fieldRef.primary()) {
                primaryData.put(fieldRef.columnName(), entry.getValue());
                continue;
            }
            RuntimeChildRelation relation = relationMap.get(fieldRef.modelCode());
            if (relation == null) {
                continue;
            }
            childDataMap.computeIfAbsent(fieldRef.modelCode(), ignored -> new LinkedHashMap<>())
                    .put(fieldRef.columnName(), entry.getValue());
        }
    }

    private boolean hasWritableChildData(Map<String, Object> childData) {
        if (childData == null || childData.isEmpty()) {
            return false;
        }
        return childData.values().stream().anyMatch(value -> value != null && !(value instanceof String text && StringUtils.isBlank(text)));
    }

    private Object resolveMainRelationValue(RuntimeChildRelation relation,
                                            Map<String, Object> primaryData,
                                            Long mainId,
                                            Map<String, Object> currentMainRecord) {
        if ("id".equals(relation.mainColumn())) {
            return mainId;
        }
        if (primaryData.containsKey(relation.mainColumn())) {
            return primaryData.get(relation.mainColumn());
        }
        if (currentMainRecord != null) {
            return currentMainRecord.get(relation.mainColumn());
        }
        return null;
    }

    private boolean isImmutableWriteField(String key) {
        return IMMUTABLE_WRITE_FIELDS.contains(key);
    }

    // ==================== 删除操作 ====================

    /**
     * 删除
     */
    public void deleteById(String configKey, Long id) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 判断是否逻辑删除
        boolean logicDelete = repository.hasDelFlag(tableName);
        
        // 执行删除
        repository.deleteById(tableName, id, logicDelete);
    }

    /**
     * 暴露运行时配置给动态导入导出服务，仍统一走发布态校验。
     */
    public AiCrudConfig getRuntimeConfig(String configKey) {
        return getConfig(configKey);
    }

    private RuntimeJoinContext buildRuntimeJoinContext(AiCrudConfig config) {
        if (!"LOWCODE".equals(config.getBuildMode())
                || StringUtils.isBlank(config.getPageSchema())
                || StringUtils.isBlank(config.getModelSchema())) {
            return null;
        }
        LowcodePageSchema pageSchema = readPageSchema(config);
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().size() <= 1) {
            return null;
        }
        LowcodeModelSchema modelSchema = readModelSchema(config);
        if (modelSchema == null) {
            return null;
        }

        LowcodePageModelRef primaryRef = pageSchema.getModelRefs().stream()
                .filter(ref -> Boolean.TRUE.equals(ref.getPrimary()))
                .findFirst()
                .orElse(pageSchema.getModelRefs().get(0));
        String primaryModelCode = StringUtils.defaultIfBlank(pageSchema.getPrimaryModelCode(), primaryRef.getModelCode());
        if (StringUtils.isBlank(primaryModelCode)) {
            primaryModelCode = modelSchema.getObject() == null ? null : modelSchema.getObject().getCode();
        }
        if (StringUtils.isBlank(primaryModelCode)) {
            return null;
        }

        Map<String, RuntimeFieldRef> fields = new LinkedHashMap<>();
        Map<String, String> fieldColumnMapping = new LinkedHashMap<>();
        List<DynamicCrudRepository.JoinField> selectFields = new ArrayList<>();

        addPrimaryRuntimeFields(config.getTableName(), primaryModelCode, fields, fieldColumnMapping, selectFields);

        List<RuntimeChildRelation> childRelations = new ArrayList<>();
        List<DynamicCrudRepository.JoinSpec> joins = new ArrayList<>();
        List<LowcodeRelationSchema> primaryRelations = mergeRelations(modelSchema.getRelations(), primaryRef.getRelations());
        Map<String, String> primaryColumnMapping = repository.getColumnMapping(config.getTableName());
        int aliasIndex = 1;
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            if (StringUtils.isBlank(ref.getTableName()) || !repository.tableExists(ref.getTableName())) {
                log.warn("[DynamicCrudService] 引用模型缺少可用表名，跳过左连接, configKey={}, modelCode={}",
                        config.getConfigKey(), ref.getModelCode());
                continue;
            }
            RuntimeChildRelation relation = buildChildRelation(primaryModelCode, ref, primaryRelations,
                    "t" + aliasIndex, primaryColumnMapping, fields, fieldColumnMapping, selectFields);
            if (relation == null) {
                log.warn("[DynamicCrudService] 未找到引用模型关联关系，跳过左连接, configKey={}, modelCode={}",
                        config.getConfigKey(), ref.getModelCode());
                continue;
            }
            childRelations.add(relation);
            joins.add(new DynamicCrudRepository.JoinSpec(
                    relation.tableName(), relation.tableAlias(), relation.childFkColumn(), relation.mainColumn()));
            aliasIndex++;
        }

        if (childRelations.isEmpty()) {
            return null;
        }
        return new RuntimeJoinContext(fields, childRelations, selectFields, joins, fieldColumnMapping);
    }

    private boolean isMasterDetailRuntime(AiCrudConfig config) {
        return config != null && MASTER_DETAIL_LAYOUT.equals(config.getLayoutType());
    }

    private void addPrimaryRuntimeFields(String tableName,
                                         String modelCode,
                                         Map<String, RuntimeFieldRef> fields,
                                         Map<String, String> fieldColumnMapping,
                                         List<DynamicCrudRepository.JoinField> selectFields) {
        List<String> columns = new ArrayList<>(repository.getTableColumns(tableName));
        columns.sort(Comparator.naturalOrder());
        for (String column : columns) {
            String fieldName = DynamicQueryGenerator.snakeToCamel(column);
            RuntimeFieldRef fieldRef = new RuntimeFieldRef(fieldName, modelCode, fieldName, column, tableName, "t0", true);
            fields.putIfAbsent(fieldName, fieldRef);
            fieldColumnMapping.put(fieldName, "t0." + column);
            selectFields.add(new DynamicCrudRepository.JoinField(fieldName, "t0", column));
        }
    }

    private RuntimeChildRelation buildChildRelation(String primaryModelCode,
                                                    LowcodePageModelRef ref,
                                                    List<LowcodeRelationSchema> primaryRelations,
                                                    String tableAlias,
                                                    Map<String, String> primaryColumnMapping,
                                                    Map<String, RuntimeFieldRef> fields,
                                                    Map<String, String> fieldColumnMapping,
                                                    List<DynamicCrudRepository.JoinField> selectFields) {
        Map<String, String> childColumnMapping = repository.getColumnMapping(ref.getTableName());
        LowcodeRelationSchema relation = findRelationToPrimary(ref.getRelations(), primaryModelCode);
        boolean relationFromPrimary = false;
        if (relation == null) {
            LowcodeRelationSchema primaryRelation = findRelationFromPrimary(primaryRelations, ref.getModelCode());
            LowcodeRelationSchema inferredRelation = inferRelation(primaryModelCode, ref);
            if (shouldPreferInferredChildRelation(primaryRelation, inferredRelation, childColumnMapping)) {
                relation = inferredRelation;
            } else {
                relation = primaryRelation != null ? primaryRelation : inferredRelation;
                relationFromPrimary = primaryRelation != null && relation == primaryRelation;
            }
        }
        if (relation == null) {
            return null;
        }

        String mainField = relationFromPrimary ? relation.getSourceField() : relation.getTargetField();
        String childField = relationFromPrimary ? relation.getTargetField() : relation.getSourceField();
        String mainColumn = resolvePrimaryColumn(mainField, primaryColumnMapping);
        String childColumn = resolveChildColumn(childField, childColumnMapping);
        if (StringUtils.isBlank(mainColumn) || StringUtils.isBlank(childColumn)) {
            return null;
        }

        Map<String, RuntimeFieldRef> childFields = new LinkedHashMap<>();
        for (Map<String, Object> source : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")));
            if (StringUtils.isBlank(sourceField)) {
                continue;
            }
            String fieldName = StringUtils.defaultIfBlank(text(source.get("fieldRef")), safeKey(ref.getModelCode()) + "__" + sourceField);
            String columnName = resolveChildColumn(StringUtils.defaultIfBlank(text(source.get("columnName")), sourceField), childColumnMapping);
            if (StringUtils.isBlank(columnName)) {
                continue;
            }
            RuntimeFieldRef fieldRef = new RuntimeFieldRef(
                    fieldName, ref.getModelCode(), sourceField, columnName, ref.getTableName(), tableAlias, false);
            fields.putIfAbsent(fieldName, fieldRef);
            childFields.put(fieldName, fieldRef);
            fieldColumnMapping.put(fieldName, tableAlias + "." + columnName);
            selectFields.add(new DynamicCrudRepository.JoinField(fieldName, tableAlias, columnName));
        }
        if (childFields.isEmpty()) {
            return null;
        }
        return new RuntimeChildRelation(ref.getModelCode(), ref.getTableName(), tableAlias, childColumn, mainColumn, childFields);
    }

    private boolean shouldPreferInferredChildRelation(LowcodeRelationSchema primaryRelation,
                                                      LowcodeRelationSchema inferredRelation,
                                                      Map<String, String> childColumnMapping) {
        if (primaryRelation == null || inferredRelation == null) {
            return false;
        }
        String configuredChildColumn = resolveChildColumn(primaryRelation.getTargetField(), childColumnMapping);
        String inferredChildColumn = resolveChildColumn(inferredRelation.getSourceField(), childColumnMapping);
        return "id".equals(configuredChildColumn) && StringUtils.isNotBlank(inferredChildColumn)
                && !"id".equals(inferredChildColumn);
    }

    private String resolvePrimaryColumn(String fieldName, Map<String, String> primaryColumnMapping) {
        if (StringUtils.isBlank(fieldName)) {
            return null;
        }
        String column = primaryColumnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName));
        return primaryColumnMapping.containsValue(column) ? column : null;
    }

    private String resolveChildColumn(String fieldName, Map<String, String> childColumnMapping) {
        if (StringUtils.isBlank(fieldName)) {
            return null;
        }
        String column = childColumnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName));
        return childColumnMapping.containsValue(column) ? column : null;
    }

    private LowcodeRelationSchema findRelationFromPrimary(List<LowcodeRelationSchema> relations, String targetModelCode) {
        if (relations == null) {
            return null;
        }
        return relations.stream()
                .filter(relation -> relation != null && targetModelCode.equals(relation.getTargetObjectCode()))
                .findFirst()
                .orElse(null);
    }

    private LowcodeRelationSchema findRelationToPrimary(List<LowcodeRelationSchema> relations, String primaryModelCode) {
        if (relations == null) {
            return null;
        }
        return relations.stream()
                .filter(relation -> relation != null && primaryModelCode.equals(relation.getTargetObjectCode()))
                .findFirst()
                .orElse(null);
    }

    private LowcodeRelationSchema inferRelation(String primaryModelCode, LowcodePageModelRef ref) {
        String expectedCamel = DynamicQueryGenerator.snakeToCamel(primaryModelCode) + "Id";
        String expectedSnake = DynamicQueryGenerator.camelToSnake(primaryModelCode) + "_id";
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = StringUtils.defaultIfBlank(text(field.get("columnName")), sourceField);
            if (expectedCamel.equals(sourceField) || expectedSnake.equals(columnName)) {
                LowcodeRelationSchema relation = new LowcodeRelationSchema();
                relation.setRelationType("ONE_TO_MANY");
                relation.setSourceField(sourceField);
                relation.setTargetObjectCode(primaryModelCode);
                relation.setTargetField("id");
                return relation;
            }
        }
        return null;
    }

    private List<LowcodeRelationSchema> mergeRelations(List<LowcodeRelationSchema> first, List<LowcodeRelationSchema> second) {
        List<LowcodeRelationSchema> result = new ArrayList<>();
        if (first != null) {
            result.addAll(first);
        }
        if (second != null) {
            result.addAll(second);
        }
        return result;
    }

    private String buildJoinOrderBy(String orderByColumn, String isAsc, RuntimeJoinContext joinContext) {
        if (StringUtils.isBlank(orderByColumn)) {
            return "t0.id DESC";
        }
        List<String> columns = Arrays.stream(orderByColumn.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(column -> joinContext.fieldColumnMapping().get(column))
                .filter(StringUtils::isNotBlank)
                .toList();
        if (columns.isEmpty()) {
            return "t0.id DESC";
        }
        String direction = "asc".equalsIgnoreCase(isAsc) ? "ASC" : "DESC";
        return String.join(", ", columns) + " " + direction;
    }

    private boolean requiresJoinedPageQuery(AiCrudConfig config,
                                            PageQuery pageQuery,
                                            Map<String, Object> searchParams,
                                            RuntimeJoinContext joinContext) {
        return containsChildField(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), joinContext)
                || containsActiveChildSearchField(searchParams, joinContext)
                || containsChildField(splitFields(pageQuery.getOrderByColumn()), joinContext);
    }

    private boolean requiresJoinedExportQuery(AiCrudConfig config,
                                              Map<String, Object> searchParams,
                                              RuntimeJoinContext joinContext) {
        return containsChildField(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), joinContext)
                || containsActiveChildSearchField(searchParams, joinContext);
    }

    private boolean requiresJoinedCustomQuery(CustomQueryExecuteDTO request, RuntimeJoinContext joinContext) {
        if (request == null) {
            return false;
        }
        if (containsChildField(request.getFields(), joinContext)) {
            return true;
        }
        if (containsChildField(splitFields(request.getOrderByColumn()), joinContext)) {
            return true;
        }
        if (request.getConditions() == null) {
            return false;
        }
        for (var condition : request.getConditions()) {
            if (condition != null && isChildRuntimeField(condition.getField(), joinContext)) {
                return true;
            }
        }
        return false;
    }

    private List<DynamicCrudRepository.JoinField> buildRuntimeSelectFields(RuntimeJoinContext joinContext,
                                                                           Collection<String> requestedFields,
                                                                           boolean defaultPrimaryFields) {
        LinkedHashSet<String> fieldNames = new LinkedHashSet<>();
        fieldNames.add("id");
        if (requestedFields != null) {
            requestedFields.stream()
                    .filter(StringUtils::isNotBlank)
                    .forEach(fieldNames::add);
        }
        if (fieldNames.size() == 1 && defaultPrimaryFields) {
            joinContext.fields().values().stream()
                    .filter(RuntimeFieldRef::primary)
                    .map(RuntimeFieldRef::fieldName)
                    .forEach(fieldNames::add);
        }

        List<DynamicCrudRepository.JoinField> selectFields = new ArrayList<>();
        for (String fieldName : fieldNames) {
            RuntimeFieldRef fieldRef = joinContext.fields().get(fieldName);
            if (fieldRef == null) {
                continue;
            }
            selectFields.add(new DynamicCrudRepository.JoinField(
                    fieldRef.fieldName(), fieldRef.tableAlias(), fieldRef.columnName()));
        }
        if (selectFields.isEmpty()) {
            joinContext.fields().values().stream()
                    .filter(RuntimeFieldRef::primary)
                    .map(fieldRef -> new DynamicCrudRepository.JoinField(
                            fieldRef.fieldName(), fieldRef.tableAlias(), fieldRef.columnName()))
                    .forEach(selectFields::add);
        }
        return selectFields;
    }

    private boolean containsChildField(Collection<String> fieldNames, RuntimeJoinContext joinContext) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            return false;
        }
        for (String fieldName : fieldNames) {
            if (isChildRuntimeField(fieldName, joinContext)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsActiveChildSearchField(Map<String, Object> searchParams, RuntimeJoinContext joinContext) {
        if (searchParams == null || searchParams.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
            if (hasQueryValue(entry.getValue()) && isChildRuntimeField(entry.getKey(), joinContext)) {
                return true;
            }
        }
        return false;
    }

    private boolean isChildRuntimeField(String fieldName, RuntimeJoinContext joinContext) {
        if (StringUtils.isBlank(fieldName) || joinContext == null) {
            return false;
        }
        RuntimeFieldRef fieldRef = joinContext.fields().get(fieldName);
        return fieldRef != null && !fieldRef.primary();
    }

    private List<String> splitFields(String fields) {
        if (StringUtils.isBlank(fields)) {
            return List.of();
        }
        return Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    private boolean hasQueryValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return StringUtils.isNotBlank(text);
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return true;
    }

    private LowcodePageSchema readPageSchema(AiCrudConfig config) {
        try {
            return objectMapper.readValue(config.getPageSchema(), LowcodePageSchema.class);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析pageSchema失败, configKey={}", config.getConfigKey(), e);
            return null;
        }
    }

    private LowcodeModelSchema readModelSchema(AiCrudConfig config) {
        try {
            return objectMapper.readValue(config.getModelSchema(), LowcodeModelSchema.class);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析modelSchema失败, configKey={}", config.getConfigKey(), e);
            return null;
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String safeKey(String value) {
        String key = StringUtils.defaultIfBlank(value, "model").replaceAll("[^A-Za-z0-9_]", "_");
        return StringUtils.defaultIfBlank(key, "model");
    }

    private void applyReadPipeline(List<Map<String, Object>> rows, AiCrudConfig config) {
        applyDecrypt(rows, config.getEncryptConfig());
        applyDictTranslation(rows, config.getTransConfig());
        applyDesensitize(rows, config.getDesensitizeConfig());
    }

    private LowcodeTreeConfig resolveTreeConfig(AiCrudConfig config) {
        LowcodeTreeConfig treeConfig = new LowcodeTreeConfig();
        applyTreeConfigFromModel(config, treeConfig);
        applyTreeConfigFromOptions(config, treeConfig);
        if (StringUtils.isBlank(treeConfig.getKeyField())) {
            treeConfig.setKeyField("id");
        }
        if (StringUtils.isBlank(treeConfig.getParentField())) {
            treeConfig.setParentField("parentId");
        }
        if (StringUtils.isBlank(treeConfig.getLabelField())) {
            treeConfig.setLabelField("name");
        }
        if (StringUtils.isBlank(treeConfig.getFilterField())) {
            treeConfig.setFilterField(treeConfig.getParentField());
        }
        if (StringUtils.isBlank(treeConfig.getTargetField())) {
            treeConfig.setTargetField(treeConfig.getKeyField());
        }
        if (StringUtils.isBlank(treeConfig.getChildrenField())) {
            treeConfig.setChildrenField("children");
        }
        return treeConfig;
    }

    private void applyTreeConfigFromModel(AiCrudConfig config, LowcodeTreeConfig target) {
        if (StringUtils.isBlank(config.getModelSchema())) {
            return;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(config.getModelSchema()).get("treeConfig");
            applyTreeConfigNode(treeNode, target);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析modelSchema.treeConfig失败, configKey={}", config.getConfigKey(), e);
        }
    }

    private void applyTreeConfigFromOptions(AiCrudConfig config, LowcodeTreeConfig target) {
        if (StringUtils.isBlank(config.getOptions())) {
            return;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(config.getOptions()).get("treeConfig");
            applyTreeConfigNode(treeNode, target);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析options.treeConfig失败, configKey={}", config.getConfigKey(), e);
        }
    }

    private void applyTreeConfigNode(JsonNode treeNode, LowcodeTreeConfig target) {
        if (treeNode == null || !treeNode.isObject()) {
            return;
        }
        if (StringUtils.isNotBlank(text(treeNode, "keyField"))) {
            target.setKeyField(text(treeNode, "keyField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "sourceModelCode"))) {
            target.setSourceModelCode(text(treeNode, "sourceModelCode"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "sourceModelName"))) {
            target.setSourceModelName(text(treeNode, "sourceModelName"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "sourceTableName"))) {
            target.setSourceTableName(text(treeNode, "sourceTableName"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "parentField"))) {
            target.setParentField(text(treeNode, "parentField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "labelField"))) {
            target.setLabelField(text(treeNode, "labelField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "filterField"))) {
            target.setFilterField(text(treeNode, "filterField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "targetField"))) {
            target.setTargetField(text(treeNode, "targetField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "childrenField"))) {
            target.setChildrenField(text(treeNode, "childrenField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "treeTitle"))) {
            target.setTreeTitle(text(treeNode, "treeTitle"));
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildTree(List<Map<String, Object>> rows, LowcodeTreeConfig treeConfig) {
        Map<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        String keyField = treeConfig.getKeyField();
        String parentField = treeConfig.getParentField();
        String labelField = treeConfig.getLabelField();
        String targetField = treeConfig.getTargetField();
        String childrenField = treeConfig.getChildrenField();

        for (Map<String, Object> row : rows) {
            String key = normalizeTreeKey(row.get(keyField));
            if (StringUtils.isBlank(key)) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>(row);
            node.putIfAbsent("key", row.get(keyField));
            node.putIfAbsent("label", resolveTreeLabel(row, labelField, keyField));
            node.putIfAbsent("targetValue", row.get(targetField));
            node.put(childrenField, new ArrayList<Map<String, Object>>());
            nodeMap.put(key, node);
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> node : nodeMap.values()) {
            String key = normalizeTreeKey(node.get(keyField));
            String parentKey = normalizeTreeKey(node.get(parentField));
            Map<String, Object> parent = nodeMap.get(parentKey);
            if (isRootParent(parentKey) || parent == null || key.equals(parentKey)) {
                roots.add(node);
                continue;
            }
            Object children = parent.get(childrenField);
            if (children instanceof List<?> childList) {
                ((List<Map<String, Object>>) childList).add(node);
            }
        }
        return roots;
    }

    private Object resolveTreeLabel(Map<String, Object> row, String labelField, String keyField) {
        List<String> candidates = Arrays.asList(labelField, "label", "name", "title", "treeName", "deptName", "orgName", keyField);
        for (String candidate : candidates) {
            if (StringUtils.isBlank(candidate)) {
                continue;
            }
            Object value = row.get(candidate);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return value;
            }
        }
        return "未命名节点";
    }

    private boolean isRootParent(String parentKey) {
        return StringUtils.isBlank(parentKey) || "0".equals(parentKey) || "null".equalsIgnoreCase(parentKey);
    }

    private String normalizeTreeKey(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.isBlank(text) ? null : text;
    }

    // ==================== 加解密处理 ====================

    /**
     * 应用加密（写入时）
     * encryptConfig格式示例：
     * {
     *   "phone": {"algorithm": "SM4"},
     *   "idCard": {"algorithm": "AES"},
     *   "email": {"algorithm": "SM4"}
     * }
     */
    private void applyEncrypt(Map<String, Object> data, String encryptConfigJson) {
        if (StringUtils.isBlank(encryptConfigJson) || data == null || data.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(encryptConfigJson);
            if (!configNode.isObject()) return;

            for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                String fieldName = entry.getKey(); // camelCase字段名
                JsonNode ruleNode = entry.getValue();

                // 转换为snake_case（数据库列名）
                String snakeFieldName = DynamicQueryGenerator.camelToSnake(fieldName);

                if (!data.containsKey(snakeFieldName) || data.get(snakeFieldName) == null) {
                    continue;
                }

                // 获取加密算法配置
                String algorithm = ruleNode.has("algorithm") ? ruleNode.get("algorithm").asText() : "";
                if (StringUtils.isBlank(algorithm)) {
                    continue;
                }

                // 获取加密器
                Encryptor encryptor = encryptorFactory.getEncryptor(algorithm);
                if (encryptor == null) {
                    log.warn("[DynamicCrudService] 未找到加密器, algorithm={}", algorithm);
                    continue;
                }

                // 加密字段值
                Object value = data.get(snakeFieldName);
                if (value instanceof String) {
                    String plainText = (String) value;
                    if (StringUtils.isNotBlank(plainText)) {
                        String encryptedValue = encryptor.encrypt(plainText);
                        data.put(snakeFieldName, encryptedValue);
                        log.debug("[DynamicCrudService] 加密字段: {}, algorithm: {}", fieldName, algorithm);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 加密处理失败", e);
        }
    }

    /**
     * 应用解密（读取时）
     * encryptConfig格式示例：
     * {
     *   "phone": {"algorithm": "SM4"},
     *   "idCard": {"algorithm": "AES"},
     *   "email": {"algorithm": "SM4"}
     * }
     */
    private void applyDecrypt(List<Map<String, Object>> rows, String encryptConfigJson) {
        if (StringUtils.isBlank(encryptConfigJson) || rows == null || rows.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(encryptConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                    String fieldName = entry.getKey(); // camelCase字段名
                    JsonNode ruleNode = entry.getValue();

                    if (!row.containsKey(fieldName) || row.get(fieldName) == null) {
                        continue;
                    }

                    // 获取加密算法配置
                    String algorithm = ruleNode.has("algorithm") ? ruleNode.get("algorithm").asText() : "";
                    if (StringUtils.isBlank(algorithm)) {
                        continue;
                    }

                    // 获取加密器
                    Encryptor encryptor = encryptorFactory.getEncryptor(algorithm);
                    if (encryptor == null) {
                        log.warn("[DynamicCrudService] 未找到加密器, algorithm={}", algorithm);
                        continue;
                    }

                    // 解密字段值
                    Object value = row.get(fieldName);
                    if (value instanceof String) {
                        String cipherText = (String) value;
                        if (StringUtils.isNotBlank(cipherText)) {
                            try {
                                String decryptedValue = encryptor.decrypt(cipherText);
                                row.put(fieldName, decryptedValue);
                                log.debug("[DynamicCrudService] 解密字段: {}, algorithm: {}", fieldName, algorithm);
                            } catch (Exception decryptException) {
                                log.warn("[DynamicCrudService] 解密字段失败: {}, 可能是明文数据", fieldName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解密处理失败", e);
        }
    }

    // ==================== 脱敏处理 ====================

    /**
     * 应用字段脱敏
     */
    private void applyDesensitize(List<Map<String, Object>> rows, String desensitizeConfigJson) {
        if (StringUtils.isBlank(desensitizeConfigJson) || rows == null || rows.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(desensitizeConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                    String fieldName = entry.getKey(); // camelCase字段名
                    JsonNode ruleNode = entry.getValue();
                    if (!row.containsKey(fieldName) || row.get(fieldName) == null) continue;

                    String typeStr = ruleNode.has("type") ? ruleNode.get("type").asText("CUSTOM") : "CUSTOM";
                    DesensitizeType type = DesensitizeType.valueOf(typeStr);
                    DesensitizeStrategy strategy = desensitizeStrategyFactory.getStrategy(type);
                    if (strategy != null) {
                        String originalValue = String.valueOf(row.get(fieldName));
                        row.put(fieldName, strategy.desensitize(originalValue));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 脱敏处理失败", e);
        }
    }

    // ==================== 字典翻译 ====================

    /**
     * 应用字典翻译
     */
    private void applyDictTranslation(List<Map<String, Object>> rows, String transConfigJson) {
        if (StringUtils.isBlank(transConfigJson) || rows == null || rows.isEmpty() || dictValueProvider == null) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(transConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                    String sourceField = entry.getKey(); // camelCase字段名
                    JsonNode ruleNode = entry.getValue();
                    if (!row.containsKey(sourceField) || row.get(sourceField) == null) continue;

                    String dictType = ruleNode.has("dictType") ? ruleNode.get("dictType").asText() : "";
                    String targetField = ruleNode.has("targetField") ? ruleNode.get("targetField").asText()
                            : sourceField + "Name";
                    if (StringUtils.isBlank(dictType)) continue;

                    String key = String.valueOf(row.get(sourceField));
                    String label = dictValueProvider.getLabel(dictType, key);
                    if (label != null) {
                        row.put(targetField, label);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 字典翻译失败", e);
        }
    }

    // ==================== 配置加载 ====================

    /**
     * 获取配置
     */
    private AiCrudConfig getConfig(String configKey) {
        AiCrudConfig config = configService.getByConfigKey(configKey);
        if (config == null || "1".equals(config.getStatus())) {
            throw new BusinessException("CRUD配置不存在或已停用: " + configKey);
        }
        if (!"CONFIG".equals(config.getMode())) {
            throw new BusinessException("该配置不是配置驱动模式: " + configKey);
        }
        if ("LOWCODE".equals(config.getBuildMode()) && !"PUBLISHED".equals(config.getPublishStatus())) {
            throw new BusinessException("低代码应用尚未发布: " + configKey);
        }
        return config;
    }

    private Set<String> buildAllowedCustomFields(AiCrudConfig config) {
        Set<String> fields = new HashSet<>();
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getSearchSchema(), objectMapper));
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper));
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper));
        fields.add("id");
        return fields;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private int normalizeExportLimit(Integer maxRows) {
        if (maxRows == null || maxRows < 1) {
            return MAX_EXPORT_ROWS;
        }
        return Math.min(maxRows, MAX_EXPORT_ROWS);
    }
}
