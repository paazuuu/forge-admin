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
        return listAssets(objectCode, false);
    }

    public List<Map<String, Object>> listAssets(String objectCode, boolean includeInternal) {
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
                item.putIfAbsent("formMode", "BUSINESS_CODE_FORM");
                item.putIfAbsent("objectCode", objectCode);
                item.putIfAbsent("providerKey", provider.providerKey());
                item.putIfAbsent("providerName", provider.providerName());
                item.putIfAbsent("supportsSave", true);
                item.putIfAbsent("sourceType", "codeProvider");
                List<Map<String, Object>> fields = normalizePublicFields(
                        item.get("fields") == null ? item.get("fieldCatalog") : item.get("fields"), includeInternal);
                item.put("fields", fields);
                item.put("fieldCatalog", fields);
                item.put("fieldCount", fields.size());
                item.put("fieldPreview", buildFieldPreview(fields));
                result.add(item);
            }
        });
        return result;
    }

    private List<Map<String, Object>> normalizePublicFields(Object source, boolean includeInternal) {
        if (!(source instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object value : list) {
            if (!(value instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            map.forEach((key, fieldValue) -> item.put(String.valueOf(key), fieldValue));
            if (!includeInternal && (readBoolean(item.get("internal")) || readBoolean(item.get("systemField")))) {
                continue;
            }
            item.putIfAbsent("fieldCode", item.get("field"));
            result.add(item);
        }
        return result;
    }

    private List<String> buildFieldPreview(List<Map<String, Object>> fields) {
        List<String> preview = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            Object label = field.get("label");
            Object code = field.get("field");
            String text = StringUtils.trimToNull(label == null ? null : String.valueOf(label));
            if (text == null) {
                text = StringUtils.trimToNull(code == null ? null : String.valueOf(code));
            }
            if (text != null) {
                preview.add(text);
            }
            if (preview.size() >= 5) {
                break;
            }
        }
        return preview;
    }

    private boolean readBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value == null) {
            return false;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }
}
