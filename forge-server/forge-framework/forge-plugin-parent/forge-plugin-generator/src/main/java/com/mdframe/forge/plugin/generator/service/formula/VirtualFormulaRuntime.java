package com.mdframe.forge.plugin.generator.service.formula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaMode;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaTraceOptions;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Executes VIRTUAL-mode formulas during record query.
 * <p>
 * Extends {@link AbstractFormulaRuntime} with VIRTUAL mode filtering.
 * Formula results are appended to the record in memory only (not persisted).
 */
@Component
public class VirtualFormulaRuntime extends AbstractFormulaRuntime {

    private final FormulaCrossObjectResolver crossObjectResolver;

    public VirtualFormulaRuntime(FormulaExecutionEngine executionEngine, ObjectMapper objectMapper) {
        super(executionEngine, objectMapper, "VIRTUAL");
        this.crossObjectResolver = null;
    }

    public VirtualFormulaRuntime(FormulaExecutionEngine executionEngine,
                                 ObjectMapper objectMapper,
                                 FormulaExecutionLogService executionLogService) {
        super(executionEngine, objectMapper, "VIRTUAL", executionLogService);
        this.crossObjectResolver = null;
    }

    public VirtualFormulaRuntime(FormulaExecutionEngine executionEngine,
                                 ObjectMapper objectMapper,
                                 FormulaExecutionLogService executionLogService,
                                 FormulaCrossObjectResolver crossObjectResolver) {
        super(executionEngine, objectMapper, "VIRTUAL", executionLogService);
        this.crossObjectResolver = Objects.requireNonNull(crossObjectResolver);
    }

    @Autowired
    public VirtualFormulaRuntime(FormulaExecutionEngine executionEngine,
                                 ObjectMapper objectMapper,
                                 FormulaExecutionLogService executionLogService,
                                 FormulaCrossObjectResolver crossObjectResolver,
                                 FormulaRuntimeProperties runtimeProperties) {
        super(executionEngine, objectMapper, "VIRTUAL", executionLogService, runtimeProperties);
        this.crossObjectResolver = Objects.requireNonNull(crossObjectResolver);
    }

    @Override
    protected Map<String, FormulaConfig> extractFormulas(LowcodeModelSchema modelSchema) {
        return extractByMode(modelSchema, FormulaMode.VIRTUAL);
    }

    @Override
    public List<Map<String, Object>> calculate(List<Map<String, Object>> records,
                                               LowcodeModelSchema modelSchema,
                                               FormulaRuntimeContext context,
                                               FormulaTraceOptions traceOptions) {
        if (records == null || records.isEmpty()) {
            return records != null ? records : List.of();
        }
        Map<String, FormulaConfig> formulas = extractFormulas(modelSchema);
        if (crossObjectResolver == null || !crossObjectResolver.hasCrossObject(formulas)) {
            return super.calculate(records, modelSchema, context, traceOptions);
        }

        List<Map<String, Object>> workingRecords = copyRecords(records);
        crossObjectResolver.prefetch(formulas, workingRecords, context);
        List<Map<String, Object>> calculated = super.calculate(workingRecords, modelSchema, context, traceOptions);
        copyFormulaValues(calculated, records, formulas.keySet());
        return records;
    }

    private List<Map<String, Object>> copyRecords(List<Map<String, Object>> records) {
        List<Map<String, Object>> copies = new ArrayList<>(records.size());
        for (Map<String, Object> record : records) {
            copies.add(record == null ? new LinkedHashMap<>() : new LinkedHashMap<>(record));
        }
        return copies;
    }

    private void copyFormulaValues(List<Map<String, Object>> sourceRecords,
                                   List<Map<String, Object>> targetRecords,
                                   Set<String> formulaFields) {
        int size = Math.min(sourceRecords.size(), targetRecords.size());
        for (int i = 0; i < size; i++) {
            Map<String, Object> source = sourceRecords.get(i);
            Map<String, Object> target = targetRecords.get(i);
            if (source == null || target == null) {
                continue;
            }
            for (String field : formulaFields) {
                if (source.containsKey(field)) {
                    target.put(field, source.get(field));
                }
            }
        }
    }
}
