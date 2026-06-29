<template>
  <div class="flow-variable-mapping-editor">
    <div class="mapping-guide">
      <div>
        <strong>业务字段 -> 流程变量</strong>
        <span>左边选择单据字段，右边选择流程模型里使用的变量。只有条件分支、审批人表达式或标题模板需要用到的字段才需要映射。</span>
      </div>
      <n-tag size="small" :type="suggestions.length ? 'info' : 'default'" :bordered="false">
        {{ suggestions.length ? `${suggestions.length} 条推荐` : '无推荐' }}
      </n-tag>
    </div>

    <div class="mapping-toolbar">
      <n-space size="small">
        <n-button size="small" secondary :disabled="!suggestions.length" @click="applySuggestions">
          应用推荐映射
        </n-button>
        <n-button size="small" dashed @click="addMapping">
          添加映射
        </n-button>
      </n-space>
    </div>

    <div v-if="mappings.length" class="mapping-list">
      <div v-for="(mapping, index) in mappings" :key="mapping.clientKey" class="mapping-row">
        <n-select
          :value="mapping.formField"
          :options="fieldOptions"
          clearable
          filterable
          placeholder="单据字段"
          @update:value="value => updateMapping(index, { formField: value, label: fieldLabel(value) })"
        />
        <span>→</span>
        <n-select
          :value="mapping.flowVariable"
          :options="variableOptions"
          :loading="loading"
          clearable
          filterable
          tag
          placeholder="流程变量"
          @update:value="value => updateMapping(index, { flowVariable: value || '' })"
        />
        <n-button quaternary circle size="small" @click="removeMapping(index)">
          <template #icon>
            <n-icon><TrashOutline /></n-icon>
          </template>
        </n-button>
      </div>
    </div>
    <n-empty v-else description="暂无变量映射" />

    <n-alert v-if="warnings.length" type="warning" :bordered="false" class="mapping-warning">
      {{ warnings.join('；') }}
    </n-alert>
  </div>
</template>

<script setup>
import { TrashOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  fieldOptions: {
    type: Array,
    default: () => [],
  },
  variableOptions: {
    type: Array,
    default: () => [],
  },
  suggestions: {
    type: Array,
    default: () => [],
  },
  warnings: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])
const message = useMessage()

const mappings = computed(() => normalizeMappings(props.modelValue))

function addMapping() {
  emit('update:modelValue', [
    ...mappings.value,
    createMapping({ formField: null, flowVariable: '', label: '' }),
  ])
}

function updateMapping(index, patch) {
  const next = mappings.value.map((item, itemIndex) => itemIndex === index
    ? { ...item, ...patch }
    : item)
  emit('update:modelValue', next)
}

function removeMapping(index) {
  emit('update:modelValue', mappings.value.filter((_, itemIndex) => itemIndex !== index))
}

function applySuggestions() {
  if (!props.suggestions.length)
    return
  const next = [...mappings.value]
  let applied = 0
  props.suggestions.forEach((suggestion) => {
    if (!suggestion?.formField || !suggestion?.flowVariable)
      return
    const existing = next.find(item => item.formField === suggestion.formField)
    if (existing) {
      if (!existing.flowVariable) {
        existing.flowVariable = suggestion.flowVariable
        existing.label = suggestion.fieldLabel || fieldLabel(suggestion.formField)
        applied += 1
      }
      return
    }
    next.push(createMapping({
      formField: suggestion.formField,
      flowVariable: suggestion.flowVariable,
      label: suggestion.fieldLabel || fieldLabel(suggestion.formField),
    }))
    applied += 1
  })
  if (!applied) {
    message.info('没有可补充的空映射')
    return
  }
  emit('update:modelValue', next)
  message.success(`已应用 ${applied} 条推荐映射`)
}

function normalizeMappings(list = []) {
  return (Array.isArray(list) ? list : []).map(item => createMapping({
    ...item,
    formField: item.formField || item.field || null,
    flowVariable: item.flowVariable || item.variable || '',
    label: item.label || fieldLabel(item.formField || item.field),
    clientKey: item.clientKey,
  }))
}

function createMapping(values = {}) {
  return {
    clientKey: values.clientKey || `mapping_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    formField: values.formField || null,
    flowVariable: values.flowVariable || '',
    label: values.label || '',
  }
}

function fieldLabel(code) {
  if (!code)
    return ''
  return props.fieldOptions.find(item => item.value === code)?.label || code
}
</script>

<style scoped>
.flow-variable-mapping-editor {
  display: grid;
  gap: 10px;
  width: 100%;
}

.mapping-guide {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
  padding: 10px 12px;
}

.mapping-guide strong {
  display: block;
  color: #0f172a;
  font-size: 13px;
  line-height: 18px;
}

.mapping-guide span {
  display: block;
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.mapping-toolbar {
  display: flex;
  justify-content: flex-end;
}

.mapping-list {
  display: grid;
  gap: 8px;
}

.mapping-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 24px minmax(180px, 1fr) 32px;
  gap: 8px;
  align-items: center;
}

.mapping-row span {
  color: #64748b;
  font-size: 12px;
  text-align: center;
}

.mapping-warning {
  margin-top: 2px;
}

@media (max-width: 720px) {
  .mapping-row {
    grid-template-columns: 1fr;
  }
}
</style>
