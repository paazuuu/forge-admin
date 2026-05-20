package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.service.ISysDictDataService;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @date 2025/11/28
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SytemDictValueProvider implements DictValueProvider {
    
    private final ISysDictDataService sysDictDataService;
    
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
