<template>
  <div v-if="normalizedChildren.length" class="child-table-editor">
    <n-tabs type="line" animated>
      <n-tab-pane
        v-for="child in normalizedChildren"
        :key="resolveChildKey(child)"
        :name="resolveChildKey(child)"
        :tab="child.tabTitle || child.relationName || child.modelName || child.modelCode || child.tableName"
      >
        <div class="child-table-panel">
          <div class="child-table-head">
            <div class="child-table-title">
              {{ child.tabTitle || child.relationName || child.modelName || child.modelCode || '子表明细' }}
            </div>
            <n-space v-if="!props.readonly" size="small">
              <n-button v-if="hasRecordSelector(child)" size="small" secondary @click="openRecordSelector(child)">
                {{ resolveSelectorButtonText(child) }}
              </n-button>
              <n-button size="small" type="primary" secondary @click="addRow(child)">
                {{ resolveAddButtonText(child) }}
              </n-button>
            </n-space>
          </div>

          <div class="child-table-scroll">
            <table class="child-edit-table">
              <thead>
                <tr>
                  <th
                    v-for="field in child.fields"
                    :key="field.field"
                    :style="{ width: resolveColumnWidth(field) }"
                  >
                    <span>{{ field.label || field.field }}</span>
                    <em v-if="field.required">*</em>
                  </th>
                  <th v-if="!props.readonly" class="action-col">
                    操作
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="{ row, rowIndex } in visibleRowsFor(child)"
                  :key="row.__rowKey"
                >
                  <td v-for="field in child.fields" :key="field.field">
                    <AiFormItem
                      v-if="useRuntimeCell(field)"
                      class="child-runtime-cell"
                      :field="toRuntimeCellField(field)"
                      :value="row[field.field]"
                      :form-data="row"
                      :context="buildRuntimeCellContext(child, rowIndex)"
                      @update:value="updateCell(child, rowIndex, field, $event)"
                    />
                    <n-input
                      v-else-if="field.type === 'textarea'"
                      type="textarea"
                      :value="row[field.field]"
                      :placeholder="field.props?.placeholder || `请输入${field.label || field.field}`"
                      :disabled="props.readonly || field.disabled || field.readonly"
                      :autosize="{ minRows: 1, maxRows: 3 }"
                      v-bind="field.props"
                      @update:value="updateCell(child, rowIndex, field, $event)"
                    />
                    <n-input-number
                      v-else-if="field.type === 'number' || field.type === 'inputNumber'"
                      :value="row[field.field]"
                      :placeholder="field.props?.placeholder || `请输入${field.label || field.field}`"
                      :disabled="props.readonly || field.disabled || field.readonly"
                      :precision="field.props?.precision ?? field.precision"
                      style="width: 100%"
                      v-bind="field.props"
                      @update:value="updateCell(child, rowIndex, field, $event)"
                    />
                    <n-select
                      v-else-if="field.type === 'select'"
                      :value="row[field.field]"
                      :placeholder="field.props?.placeholder || `请选择${field.label || field.field}`"
                      :disabled="props.readonly || field.disabled || field.readonly"
                      :options="field.props?.options || field.options || []"
                      clearable
                      filterable
                      v-bind="field.props"
                      @update:value="updateCell(child, rowIndex, field, $event)"
                    />
                    <UserSelectPicker
                      v-else-if="field.type === 'userSelect'"
                      :model-value="row[field.field]"
                      :label-value="resolveUserLabel(row, field)"
                      :placeholder="field.props?.placeholder || `请选择${field.label || field.field}`"
                      :disabled="props.readonly || field.disabled || field.readonly"
                      :multiple="field.multiple"
                      :clearable="field.clearable !== false"
                      v-bind="field.props"
                      @update:model-value="updateCell(child, rowIndex, field, $event)"
                      @update:label-value="updateCellLabel(child, rowIndex, field, $event)"
                    />
                    <n-date-picker
                      v-else-if="field.type === 'date' || field.type === 'datetime'"
                      :value="row[field.field]"
                      :type="field.type === 'datetime' ? 'datetime' : 'date'"
                      :placeholder="field.props?.placeholder || `请选择${field.label || field.field}`"
                      :disabled="props.readonly || field.disabled || field.readonly"
                      style="width: 100%"
                      v-bind="field.props"
                      :format="field.props?.format || (field.type === 'datetime' ? 'yyyy-MM-dd HH:mm:ss' : 'yyyy-MM-dd')"
                      :value-format="field.props?.valueFormat || (field.type === 'datetime' ? 'yyyy-MM-dd HH:mm:ss' : 'yyyy-MM-dd')"
                      @update:value="updateCell(child, rowIndex, field, $event)"
                    />
                    <n-switch
                      v-else-if="field.type === 'switch'"
                      :value="row[field.field]"
                      :disabled="props.readonly || field.disabled || field.readonly"
                      v-bind="field.props"
                      :checked-value="field.props?.checkedValue ?? field.checkedValue ?? true"
                      :unchecked-value="field.props?.uncheckedValue ?? field.uncheckedValue ?? false"
                      @update:value="updateCell(child, rowIndex, field, $event)"
                    />
                    <n-input
                      v-else
                      :value="row[field.field]"
                      :placeholder="field.props?.placeholder || `请输入${field.label || field.field}`"
                      :disabled="props.readonly || field.disabled || field.readonly"
                      clearable
                      v-bind="field.props"
                      @update:value="updateCell(child, rowIndex, field, $event)"
                    />
                  </td>
                  <td v-if="!props.readonly" class="action-col">
                    <n-button text type="error" size="small" @click="removeRow(child, rowIndex)">
                      删除
                    </n-button>
                  </td>
                </tr>
                <tr v-if="!visibleRowsFor(child).length">
                  <td :colspan="props.readonly ? child.fields.length : child.fields.length + 1" class="empty-cell">
                    <n-empty size="small" description="暂无明细" />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </n-tab-pane>
    </n-tabs>

    <AiRecordSelectorModal
      v-model:show="selectorVisible"
      :title="activeSelectorTitle"
      :suite-code="activeSelectorConfig.suiteCode"
      :object-code="activeSelectorConfig.objectCode"
      :multiple="true"
      :display-fields="activeSelectorConfig.displayFields"
      :keyword-fields="activeSelectorConfig.keywordFields"
      :field-mappings="activeSelectorConfig.fieldMappings"
      :search-params="activeSelectorConfig.searchParams"
      @confirm="handleSelectorConfirm"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import AiRecordSelectorModal from '@/components/ai-form/AiRecordSelectorModal.vue'
import AiFormItem from '@/components/ai-form/AiFormItem.vue'
import { applyRecordFieldMappings } from '@/components/ai-form/record-selector-utils'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'

const props = defineProps({
  value: {
    type: Object,
    default: () => ({}),
  },
  childrenConfig: {
    type: Array,
    default: () => [],
  },
  readonly: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:value'])

const localValue = ref({})
const selectorVisible = ref(false)
const activeSelectorChild = ref(null)

const normalizedChildren = computed(() => (props.childrenConfig || [])
  .map(child => ({
    ...child,
    fields: (child.fields || []).filter(field => field && field.field),
  }))
  .filter(child => child.fields.length))

watch(
  () => props.value,
  (value) => {
    localValue.value = normalizeInputValue(value)
  },
  { immediate: true, deep: true },
)

function resolveChildKey(child) {
  return child.key || child.modelCode || child.tableName || 'children'
}

function resolveAddButtonText(child) {
  const title = child.tabTitle || child.relationName || child.modelName || '关联数据'
  return `新增${title}`
}

const activeSelectorConfig = computed(() => normalizeRecordSelectorConfig(activeSelectorChild.value))
const activeSelectorTitle = computed(() => activeSelectorConfig.value.title || `选择${activeSelectorChild.value?.modelName || activeSelectorChild.value?.tabTitle || '记录'}`)

function hasRecordSelector(child) {
  return Boolean(normalizeRecordSelectorConfig(child).objectCode)
}

function resolveSelectorButtonText(child) {
  return normalizeRecordSelectorConfig(child).buttonText || '选择记录'
}

function rowsFor(child) {
  const key = resolveChildKey(child)
  return Array.isArray(localValue.value[key]) ? localValue.value[key] : []
}

function visibleRowsFor(child) {
  return rowsFor(child)
    .map((row, rowIndex) => ({ row, rowIndex }))
    .filter(item => !isDeletedRow(item.row))
}

function addRow(child) {
  const key = resolveChildKey(child)
  localValue.value = {
    ...localValue.value,
    [key]: [...rowsFor(child), createEmptyRow(child)],
  }
  commit()
}

function openRecordSelector(child) {
  activeSelectorChild.value = child
  selectorVisible.value = true
}

function handleSelectorConfirm({ rows = [], mappings = {} } = {}) {
  const child = activeSelectorChild.value
  if (!child || !rows.length)
    return
  const key = resolveChildKey(child)
  const nextRows = rows.map(row => ({
    ...createEmptyRow(child),
    ...applyRecordFieldMappings(row, mappings || activeSelectorConfig.value.fieldMappings),
  }))
  localValue.value = {
    ...localValue.value,
    [key]: [...rowsFor(child), ...nextRows],
  }
  commit()
}

function removeRow(child, rowIndex) {
  const key = resolveChildKey(child)
  const rows = rowsFor(child)
  const row = rows[rowIndex]
  if (isMergeSaveMode(child) && hasPersistedRowId(row)) {
    localValue.value = {
      ...localValue.value,
      [key]: rows.map((item, index) => index === rowIndex ? { ...item, _deleted: true } : item),
    }
    commit()
    return
  }
  localValue.value = {
    ...localValue.value,
    [key]: rows.filter((_row, index) => index !== rowIndex),
  }
  commit()
}

function updateCell(child, rowIndex, field, value) {
  updateRow(child, rowIndex, { [field.field]: value })
}

function updateCellLabel(child, rowIndex, field, value) {
  const labelField = resolveUserLabelField(field)
  if (!labelField)
    return
  updateRow(child, rowIndex, {
    [labelField]: Array.isArray(value) ? value.join(',') : value || undefined,
  })
}

function updateRow(child, rowIndex, patch) {
  const key = resolveChildKey(child)
  const rows = rowsFor(child).map((row, index) => {
    if (index !== rowIndex)
      return row
    return applyRowPatch(row, patch)
  })
  localValue.value = {
    ...localValue.value,
    [key]: rows,
  }
  commit()
}

function useRuntimeCell(field = {}) {
  if (field.type === 'select') {
    return Boolean(field.dictType || field.props?.dictType || field.optionSource || field.props?.optionSource)
  }
  return [
    'dictSelect',
    'orgTreeSelect',
    'regionTreeSelect',
    'objectReference',
    'fileUpload',
    'imageUpload',
    'cascader',
    'treeSelect',
    'customSelect',
    'radio',
    'checkbox',
  ].includes(field.type)
}

function toRuntimeCellField(field = {}) {
  return {
    ...field,
    disabled: props.readonly || field.disabled || field.readonly,
    readonly: props.readonly || field.readonly,
    showLabel: false,
    showFeedback: false,
    size: field.size || 'small',
    props: {
      ...(field.props || {}),
      size: field.props?.size || field.size || 'small',
    },
  }
}

function buildRuntimeCellContext(child, rowIndex) {
  return {
    schema: child.fields || [],
    allSchema: child.fields || [],
    patchFormData: patch => updateRow(child, rowIndex, patch),
  }
}

function applyRowPatch(row, patch) {
  const next = { ...row }
  Object.entries(patch || {}).forEach(([key, value]) => {
    if (value === undefined)
      delete next[key]
    else
      next[key] = value
  })
  return next
}

function resolveUserLabel(row, field) {
  return row?.[resolveUserLabelField(field)] || ''
}

function resolveUserLabelField(field) {
  return field?.props?.targetField || field?.targetField || `${field?.field || ''}Name`
}

function createEmptyRow(child) {
  const row = {
    __rowKey: `row_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
  }
  ;(child.fields || []).forEach((field) => {
    row[field.field] = field.defaultValue ?? null
  })
  return row
}

function normalizeRecordSelectorConfig(child = {}) {
  const config = {
    ...(child.recordSelector || {}),
    ...(child.selector || {}),
  }
  return {
    ...config,
    suiteCode: config.suiteCode || child.suiteCode || '',
    objectCode: config.objectCode || child.targetObjectCode || child.objectCode || '',
    title: config.title || config.selectorTitle || '',
    buttonText: config.buttonText || '',
    displayFields: Array.isArray(config.displayFields) ? config.displayFields : [],
    keywordFields: Array.isArray(config.keywordFields) ? config.keywordFields : [],
    fieldMappings: config.fieldMappings || config.mappings || [],
    searchParams: config.searchParams || {},
  }
}

function normalizeInputValue(value) {
  const source = value && typeof value === 'object' ? value : {}
  const result = {}
  normalizedChildren.value.forEach((child) => {
    const key = resolveChildKey(child)
    result[key] = (Array.isArray(source[key]) ? source[key] : []).map(row => ({
      __rowKey: row.__rowKey || `row_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      ...row,
    }))
  })
  return result
}

function commit() {
  emit('update:value', getEditorValue())
}

function getValue() {
  const result = {}
  normalizedChildren.value.forEach((child) => {
    const key = resolveChildKey(child)
    result[key] = rowsFor(child)
      .filter(row => isDeletedRow(row) || !isEmptyRow(row, child.fields))
      .map(row => stripInternalFields(row))
  })
  return result
}

function getEditorValue() {
  const result = {}
  normalizedChildren.value.forEach((child) => {
    const key = resolveChildKey(child)
    result[key] = rowsFor(child).map(row => ({ ...row }))
  })
  return result
}

function stripInternalFields(row) {
  const result = {}
  Object.entries(row || {}).forEach(([key, value]) => {
    if (key !== '__rowKey')
      result[key] = value
  })
  return result
}

function isEmptyRow(row, fields) {
  if (isDeletedRow(row))
    return false
  return !(fields || []).some(field => !isEmptyValue(row?.[field.field]))
}

function isMergeSaveMode(child) {
  return String(child?.saveMode || '').toLowerCase() === 'merge'
}

function hasPersistedRowId(row) {
  const id = row?.id ?? row?.ID
  return id !== null && id !== undefined && String(id).trim() !== ''
}

function isDeletedRow(row) {
  const value = row?._deleted ?? row?.__deleted
  if (typeof value === 'boolean')
    return value
  return ['true', '1', 'yes', 'y'].includes(String(value || '').trim().toLowerCase())
}

function isEmptyValue(value) {
  if (value === null || value === undefined)
    return true
  if (typeof value === 'string')
    return value.trim() === ''
  if (Array.isArray(value))
    return value.length === 0
  return false
}

function validate() {
  for (const child of normalizedChildren.value) {
    const rows = visibleRowsFor(child)
    for (let index = 0; index < rows.length; index++) {
      const { row } = rows[index]
      if (isEmptyRow(row, child.fields))
        continue
      for (const field of child.fields) {
        if (field.required && isEmptyValue(row[field.field])) {
          throw new Error(`${child.modelName || '子表'}第${index + 1}行请填写${field.label || field.field}`)
        }
      }
    }
  }
}

function resolveColumnWidth(field) {
  if (field.width)
    return `${field.width}px`
  if (field.type === 'textarea')
    return '260px'
  if (field.type === 'date' || field.type === 'datetime')
    return '190px'
  return '180px'
}

defineExpose({
  validate,
  getValue,
})
</script>

<style scoped>
.child-table-editor {
  margin-top: 18px;
  border-top: 1px solid #e5e7eb;
  padding-top: 14px;
}

.child-table-panel {
  display: grid;
  gap: 10px;
}

.child-table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.child-table-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}

.child-table-scroll {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.child-edit-table {
  width: 100%;
  min-width: 720px;
  border-collapse: collapse;
  background: #fff;
}

.child-edit-table th {
  height: 38px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  padding: 0 10px;
  text-align: left;
  white-space: nowrap;
}

.child-edit-table th em {
  margin-left: 3px;
  color: #dc2626;
  font-style: normal;
}

.child-edit-table td {
  border-bottom: 1px solid #eef2f7;
  padding: 8px 10px;
  vertical-align: top;
}

.child-runtime-cell {
  width: 100%;
}

.child-runtime-cell :deep(.n-form-item) {
  margin: 0;
}

.child-runtime-cell :deep(.n-form-item-feedback-wrapper) {
  display: none;
  min-height: 0;
}

.child-edit-table tr:last-child td {
  border-bottom: 0;
}

.action-col {
  width: 76px;
  text-align: center;
  white-space: nowrap;
}

.empty-cell {
  padding: 26px 0 !important;
}
</style>
