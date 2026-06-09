package com.mdframe.forge.plugin.system.listener;

import com.mdframe.forge.plugin.system.service.impl.SytemDictValueProvider;
import com.mdframe.forge.plugin.system.service.ISysDictDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DictChangeEventListener {

    private final ISysDictDataService dictDataService;
    private final SytemDictValueProvider dictValueProvider;

    @EventListener
    public void onDictChange(DictChangeEvent event) {
        for (String dictType : event.getDictTypes()) {
            dictDataService.clearDictDataCache(dictType);
            dictValueProvider.clearCache(dictType);
        }
    }
}
