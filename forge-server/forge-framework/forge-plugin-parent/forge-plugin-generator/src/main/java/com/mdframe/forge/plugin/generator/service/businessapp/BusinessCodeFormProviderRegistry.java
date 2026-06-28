package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 代码业务表单 Provider 注册表。
 */
@Component
@RequiredArgsConstructor
public class BusinessCodeFormProviderRegistry {

    private final ObjectProvider<BusinessCodeFormProvider> providers;

    public Optional<BusinessCodeFormProvider> find(String providerKey) {
        if (StringUtils.isBlank(providerKey)) {
            return Optional.empty();
        }
        return providers.orderedStream()
                .filter(provider -> StringUtils.equals(providerKey, provider.providerKey()))
                .findFirst();
    }

    public BusinessCodeFormProvider require(String providerKey) {
        return find(providerKey)
                .orElseThrow(() -> new BusinessException("代码表单Provider未注册: " + providerKey));
    }

    public List<Map<String, Object>> listAssets(String objectCode) {
        List<Map<String, Object>> result = new ArrayList<>();
        providers.orderedStream().forEach(provider -> {
            List<Map<String, Object>> assets = provider.formAssets(objectCode);
            if (assets == null || assets.isEmpty()) {
                return;
            }
            for (Map<String, Object> source : assets) {
                if (source == null) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>(source);
                item.putIfAbsent("type", "BUSINESS_CODE_FORM");
                item.putIfAbsent("providerKey", provider.providerKey());
                item.putIfAbsent("providerName", provider.providerName());
                result.add(item);
            }
        });
        return result;
    }
}
