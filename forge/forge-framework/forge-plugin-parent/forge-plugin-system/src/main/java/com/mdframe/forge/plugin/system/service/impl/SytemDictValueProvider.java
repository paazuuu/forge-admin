package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.mapper.SysFileMetadataMapper;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.service.ISysDictDataService;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class SytemDictValueProvider implements DictValueProvider {

    private final ISysDictDataService sysDictDataService;
    private final SysOrgMapper sysOrgMapper;
    private final SysUserMapper sysUserMapper;
    private final SysFileMetadataMapper sysFileMetadataMapper;

    private static final long CACHE_TTL_MS = 30 * 60 * 1000L;

    static class DictCacheEntry {
        final Map<String, String> valueLabelMap;
        final long timestamp;

        DictCacheEntry(Map<String, String> valueLabelMap) {
            this.valueLabelMap = valueLabelMap;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    private final ConcurrentHashMap<String, DictCacheEntry> dictCache = new ConcurrentHashMap<>();

    @Override
    public String getLabel(String dictType, String key) {
        if (dictType == null || key == null) {
            return null;
        }
        Map<String, String> valueLabelMap = getOrLoadDictMap(dictType);
        return valueLabelMap.get(key);
    }

    @Override
    public String getOrgName(String orgId) {
        if (orgId == null || orgId.isBlank()) {
            return null;
        }
        try {
            Long id = Long.valueOf(orgId);
            SysOrg org = sysOrgMapper.selectById(id);
            return org != null ? org.getOrgName() : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Map<String, String> batchGetOrgNames(List<String> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = orgIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(id -> {
                    try { return Long.valueOf(id); } catch (NumberFormatException e) { return null; }
                })
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysOrg> orgs = sysOrgMapper.selectBatchIds(ids);
        Map<String, String> result = new LinkedHashMap<>();
        for (SysOrg org : orgs) {
            result.put(String.valueOf(org.getId()), org.getOrgName());
        }
        return result;
    }

    @Override
    public String getUserName(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            Long id = Long.valueOf(userId);
            SysUser user = sysUserMapper.selectById(id);
            return user != null ? user.getRealName() : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Map<String, String> batchGetUserNames(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = userIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(id -> {
                    try { return Long.valueOf(id); } catch (NumberFormatException e) { return null; }
                })
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(ids);
        Map<String, String> result = new LinkedHashMap<>();
        for (SysUser user : users) {
            result.put(String.valueOf(user.getId()), user.getRealName());
        }
        return result;
    }

    @Override
    public String getFileUrl(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        try {
            SysFileMetadata metadata = sysFileMetadataMapper.selectOne(
                    new LambdaQueryWrapper<SysFileMetadata>().eq(SysFileMetadata::getFileId, fileId));
            return metadata != null ? metadata.getAccessUrl() : null;
        } catch (Exception e) {
            log.warn("[DictValueProvider] 获取文件URL失败, fileId={}", fileId, e);
            return null;
        }
    }

    @Override
    public Map<String, String> batchGetFileUrls(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> distinctIds = fileIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysFileMetadata> metadatas = sysFileMetadataMapper.selectList(
                new LambdaQueryWrapper<SysFileMetadata>().in(SysFileMetadata::getFileId, distinctIds));
        Map<String, String> result = new LinkedHashMap<>();
        for (SysFileMetadata metadata : metadatas) {
            result.put(metadata.getFileId(), metadata.getAccessUrl());
        }
        return result;
    }

    @Override
    public String getFileName(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        try {
            SysFileMetadata metadata = sysFileMetadataMapper.selectOne(
                    new LambdaQueryWrapper<SysFileMetadata>().eq(SysFileMetadata::getFileId, fileId));
            return metadata != null ? metadata.getOriginalName() : null;
        } catch (Exception e) {
            log.warn("[DictValueProvider] 获取文件名失败, fileId={}", fileId, e);
            return null;
        }
    }

    @Override
    public Map<String, String> batchGetFileNames(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> distinctIds = fileIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysFileMetadata> metadatas = sysFileMetadataMapper.selectList(
                new LambdaQueryWrapper<SysFileMetadata>().in(SysFileMetadata::getFileId, distinctIds));
        Map<String, String> result = new LinkedHashMap<>();
        for (SysFileMetadata metadata : metadatas) {
            result.put(metadata.getFileId(), metadata.getOriginalName());
        }
        return result;
    }

    private Map<String, String> getOrLoadDictMap(String dictType) {
        DictCacheEntry entry = dictCache.get(dictType);
        if (entry != null && !entry.isExpired()) {
            return entry.valueLabelMap;
        }
        Map<String, String> valueLabelMap = loadDictMapFromDb(dictType);
        dictCache.put(dictType, new DictCacheEntry(valueLabelMap));
        return valueLabelMap;
    }

    private Map<String, String> loadDictMapFromDb(String dictType) {
        List<SysDictData> dictDataList = sysDictDataService.selectDictDataByType(dictType);
        Map<String, String> map = new ConcurrentHashMap<>(dictDataList.size() * 2);
        for (SysDictData dictData : dictDataList) {
            map.put(dictData.getDictValue(), dictData.getDictLabel());
        }
        return map;
    }

    public void clearCache() {
        dictCache.clear();
        log.info("字典翻译缓存已全部清除");
    }

    public void clearCache(String dictType) {
        dictCache.remove(dictType);
        log.info("字典翻译缓存已清除: {}", dictType);
    }
}
