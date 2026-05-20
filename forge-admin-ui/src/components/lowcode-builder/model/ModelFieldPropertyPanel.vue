<template>
  <div class="property-panel">
    <div class="panel-title">
      字段属性
    </div>
    <n-empty v-if="!field" description="请选择左侧字段" />
    <n-form v-else label-placement="top" size="small" class="field-form">
      <n-grid :cols="2" :x-gap="12">
        <n-form-item-gi label="中文名称">
          <n-input
            :value="field.label"
            placeholder="例如：合同名称"
            @update:value="updateFieldProp('label', $event)"
          />
        </n-form-item-gi>
        <n-form-item-gi label="字段名">
          <n-input
            :value="field.field"
            placeholder="camelCase"
            @update:value="handleFieldNameChange"
          />
        </n-form-item-gi>
      </n-grid>

      <n-form-item label="数据库列名">
        <n-input
          :value="field.columnName"
          placeholder="lower_snake_case"
          @update:value="updateFieldProp('columnName', $event)"
        />
      </n-form-item>

      <n-grid :cols="2" :x-gap="12">
        <n-form-item-gi label="数据类型">
          <FieldTypeSelect
            :value="field.dataType"
            @update:value="updateFieldProp('dataType', $event)"
          />
        </n-form-item-gi>
        <n-form-item-gi label="控件类型">
          <n-select
            :value="field.componentType"
            :options="componentTypeOptions"
            size="small"
            @update:value="updateFieldProp('componentType', $event)"
          />
        </n-form-item-gi>
      </n-grid>

      <n-grid :cols="2" :x-gap="12">
        <n-form-item-gi label="长度">
          <n-input-number
            :value="field.length"
            :min="1"
            :max="2048"
            style="width: 100%"
            @update:value="updateFieldProp('length', $event)"
          />
        </n-form-item-gi>
        <n-form-item-gi label="小数位">
          <n-input-number
            :value="field.precision"
            :min="0"
            :max="12"
            style="width: 100%"
            @update:value="updateFieldProp('precision', $event)"
          />
        </n-form-item-gi>
      </n-grid>

      <n-divider>业务能力</n-divider>
      <div class="switch-grid">
        <div class="switch-item">
          <span>必填</span>
          <n-switch
            :value="field.required"
            size="small"
            @update:value="updateFieldProp('required', $event)"
          />
        </div>
        <div class="switch-item">
          <span>查询</span>
          <n-switch
            :value="field.searchable"
            size="small"
            @update:value="updateFieldProp('searchable', $event)"
          />
        </div>
        <div class="switch-item">
          <span>列表</span>
          <n-switch
            :value="field.listVisible"
            size="small"
            @update:value="updateFieldProp('listVisible', $event)"
          />
        </div>
        <div class="switch-item">
          <span>表单</span>
          <n-switch
            :value="field.formVisible"
            size="small"
            @update:value="updateFieldProp('formVisible', $event)"
          />
        </div>
      </div>

      <n-grid :cols="2" :x-gap="12">
        <n-form-item-gi label="查询方式">
          <n-select
            :value="field.queryType"
            :options="queryTypeOptions"
            size="small"
            @update:value="updateFieldProp('queryType', $event)"
          />
        </n-form-item-gi>
        <n-form-item-gi label="列宽">
          <n-input-number
            :value="field.width"
            :min="80"
            :max="520"
            style="width: 100%"
            @update:value="updateFieldProp('width', $event)"
          />
        </n-form-item-gi>
      </n-grid>

      <n-divider>字典与安全</n-divider>
      <n-form-item label="字典类型">
        <DictTypeSelect
          :value="field.dictType"
          :fields="fields"
          @update:value="updateFieldProp('dictType', $event)"
        />
      </n-form-item>
      <n-grid :cols="2" :x-gap="12">
        <n-form-item-gi label="敏感类型">
          <n-select
            :value="field.sensitiveType"
            :options="sensitiveTypeOptions"
            size="small"
            @update:value="updateFieldProp('sensitiveType', $event)"
          />
        </n-form-item-gi>
        <n-form-item-gi label="加密算法">
          <n-select
            :value="field.encryptAlgorithm"
            clearable
            size="small"
            :options="encryptOptions"
            @update:value="updateFieldProp('encryptAlgorithm', $event || '')"
          />
        </n-form-item-gi>
      </n-grid>

      <n-form-item label="备注">
        <n-input
          :value="field.remark"
          type="textarea"
          :rows="2"
          placeholder="字段说明，可为空"
          @update:value="updateFieldProp('remark', $event)"
        />
      </n-form-item>
    </n-form>
  </div>
</template>

<script setup>
import DictTypeSelect from '../shared/DictTypeSelect.vue'
import FieldTypeSelect from '../shared/FieldTypeSelect.vue'
import {
  camelToSnake,
  componentTypeOptions,
  normalizeFieldName,
  queryTypeOptions,
  sensitiveTypeOptions,
} from './model-schema'

const props = defineProps({
  field: {
    type: Object,
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:field'])

const encryptOptions = [
  { label: '不加密', value: '' },
  { label: 'SM4', value: 'SM4' },
  { label: 'AES', value: 'AES' },
]

function handleFieldNameChange(value) {
  if (!props.field)
    return
  const nextField = normalizeFieldName(value)
  patchField({
    field: nextField,
    columnName: camelToSnake(nextField),
  })
}

function updateFieldProp(key, value) {
  patchField({ [key]: value })
}

function patchField(patch) {
  if (!props.field)
    return
  emit('update:field', {
    ...props.field,
    ...patch,
  })
}
</script>

<style scoped>
.property-panel {
  height: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.panel-title {
  height: 42px;
  display: flex;
  align-items: center;
  padding: 0 14px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.field-form {
  padding: 14px;
  max-height: calc(100% - 42px);
  overflow: auto;
}

.switch-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.switch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  color: #475569;
}
</style>
