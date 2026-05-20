<template>
  <div class="structured-designer">
    <section class="surface-section">
      <div class="section-head">
        <div>
          <div class="section-title">
            查询集
          </div>
          <div class="section-desc">
            查询、重置、展开收起固定在查询集内；这里只调整字段和顺序。
          </div>
        </div>
        <n-space size="small" align="center">
          <NSwitch
            :value="searchZone?.enabled !== false"
            size="small"
            @update:value="patchZone('search', { enabled: $event })"
          />
          <NButton size="small" @click="resetSearchFields">
            恢复默认
          </NButton>
        </n-space>
      </div>

      <div class="query-surface">
        <div class="query-grid">
          <n-form-item
            v-for="queryField in searchFields"
            :key="queryField.field"
            :label="queryField.label || queryField.field"
            :show-feedback="false"
          >
            <ComponentPreviewControl
              :field="queryField"
              :options="resolveOptions(queryField)"
              disabled
            />
          </n-form-item>
        </div>
        <div class="query-actions">
          <NButton type="primary" size="small" disabled>
            查询
          </NButton>
          <NButton size="small" disabled>
            重置
          </NButton>
          <NButton text type="primary" size="small" disabled>
            条件收起
          </NButton>
          <NButton v-if="tableZone?.props?.enableCustomQuery" text size="small" disabled>
            自定义查询
          </NButton>
        </div>
      </div>

      <FieldOrderEditor
        title="查询字段"
        empty-text="当前没有查询字段"
        :fields="fields"
        :selected-refs="searchZone?.fieldRefs || []"
        :filter="candidate => candidate.searchable"
        @update="updateZoneRefs('search', $event)"
      />
    </section>

    <section class="surface-section">
      <div class="section-head">
        <div>
          <div class="section-title">
            列表页
          </div>
          <div class="section-desc">
            新增、导出是独立工具按钮；表格列只配置字段顺序和显示范围。
          </div>
        </div>
        <n-space size="small" align="center">
          <NSwitch
            :value="tableZone?.enabled !== false"
            size="small"
            @update:value="patchZone('table', { enabled: $event })"
          />
          <NButton size="small" @click="resetTableFields">
            恢复默认
          </NButton>
        </n-space>
      </div>

      <div class="table-toolbar">
        <n-space size="small">
          <NButton type="primary" size="small" disabled>
            新增
          </NButton>
          <NButton v-if="tableZone?.props?.showExport" size="small" disabled>
            导出
          </NButton>
          <NButton v-if="tableZone?.props?.showImport" size="small" disabled>
            导入
          </NButton>
        </n-space>
        <n-space size="small" align="center">
          <span class="toggle-label">导入</span>
          <NSwitch
            :value="!!tableZone?.props?.showImport"
            size="small"
            @update:value="updateTableProp('showImport', $event)"
          />
          <span class="toggle-label">导出</span>
          <NSwitch
            :value="!!tableZone?.props?.showExport"
            size="small"
            @update:value="updateTableProp('showExport', $event)"
          />
          <span class="toggle-label">自定义查询</span>
          <NSwitch
            :value="tableZone?.props?.enableCustomQuery !== false"
            size="small"
            @update:value="updateTableProp('enableCustomQuery', $event)"
          />
        </n-space>
      </div>

      <n-data-table
        :columns="tableColumns"
        :data="sampleRows"
        :bordered="false"
        size="small"
        class="preview-table"
      />

      <FieldOrderEditor
        title="列表字段"
        empty-text="当前没有列表字段"
        :fields="fields"
        :selected-refs="tableZone?.fieldRefs || []"
        :filter="candidate => candidate.listVisible !== false"
        @update="updateZoneRefs('table', $event)"
      />

      <template v-if="layoutType === 'tree-crud'">
        <n-divider>树形导航</n-divider>
        <div class="tree-grid">
          <n-form-item label="树标题">
            <NInput
              :value="treeConfig.treeTitle"
              placeholder="例如：组织架构"
              @update:value="updateTreeConfig('treeTitle', $event)"
            />
          </n-form-item>
          <n-form-item label="父级字段">
            <NSelect
              :value="treeConfig.parentField"
              :options="fieldOptions"
              placeholder="请选择父级字段"
              @update:value="updateTreeConfig('parentField', $event)"
            />
          </n-form-item>
          <n-form-item label="显示字段">
            <NSelect
              :value="treeConfig.labelField"
              :options="fieldOptions"
              placeholder="请选择树节点显示字段"
              @update:value="updateTreeConfig('labelField', $event)"
            />
          </n-form-item>
        </div>
      </template>
    </section>

    <section class="surface-section">
      <div class="section-head">
        <div>
          <div class="section-title">
            查询详情页
          </div>
          <div class="section-desc">
            详情页按字段顺序展示，只调整是否启用和字段顺序。
          </div>
        </div>
        <NSwitch
          :value="detailZone?.enabled !== false"
          size="small"
          @update:value="patchZone('detail', { enabled: $event })"
        />
      </div>

      <n-descriptions :column="2" bordered size="small" class="detail-preview">
        <n-descriptions-item
          v-for="detailField in detailFields"
          :key="detailField.field"
          :label="detailField.label || detailField.field"
        >
          {{ resolveSampleValue(detailField) }}
        </n-descriptions-item>
      </n-descriptions>

      <FieldOrderEditor
        title="详情字段"
        empty-text="当前没有详情字段"
        :fields="fields"
        :selected-refs="detailZone?.fieldRefs || []"
        :filter="candidate => candidate.formVisible !== false"
        @update="updateZoneRefs('detail', $event)"
      />
    </section>
  </div>
</template>

<script setup>
import { NButton, NDatePicker, NInput, NInputNumber, NSelect, NSwitch, NUpload } from 'naive-ui'
import { computed, defineComponent, h } from 'vue'
import draggable from 'vuedraggable'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  layoutType: {
    type: String,
    default: 'simple-crud',
  },
})

const emit = defineEmits(['update:modelValue'])

const ComponentPreviewControl = defineComponent({
  name: 'ComponentPreviewControl',
  props: {
    field: {
      type: Object,
      required: true,
    },
    options: {
      type: Array,
      default: () => [],
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  setup(controlProps) {
    return () => {
      const field = controlProps.field
      const label = field.label || field.field
      const componentType = field.componentType
      if (componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field.dataType)) {
        return h(NInputNumber, {
          disabled: controlProps.disabled,
          placeholder: `请输入${label}`,
          showButton: false,
          style: 'width: 100%',
        })
      }
      if (['date', 'datetime'].includes(componentType) || ['date', 'datetime'].includes(field.dataType)) {
        return h(NDatePicker, {
          disabled: controlProps.disabled,
          type: componentType === 'datetime' || field.dataType === 'datetime' ? 'datetime' : 'date',
          placeholder: `请选择${label}`,
          style: 'width: 100%',
        })
      }
      if (field.dictType || ['select', 'radio', 'checkbox'].includes(componentType)) {
        return h(NSelect, {
          disabled: controlProps.disabled,
          options: controlProps.options,
          placeholder: `请选择${label}`,
          clearable: true,
        })
      }
      if (['fileUpload', 'imageUpload', 'upload'].includes(componentType)) {
        return h(NUpload, {
          disabled: controlProps.disabled,
          defaultUpload: false,
          showFileList: false,
          accept: componentType === 'imageUpload' ? 'image/*' : undefined,
        }, {
          default: () => h(NButton, { size: 'small', disabled: controlProps.disabled }, { default: () => componentType === 'imageUpload' ? '选择图片' : '选择文件' }),
        })
      }
      return h(NInput, {
        disabled: controlProps.disabled,
        type: componentType === 'textarea' ? 'textarea' : 'text',
        rows: componentType === 'textarea' ? 2 : undefined,
        placeholder: `请输入${label}`,
      })
    }
  },
})

const FieldOrderEditor = defineComponent({
  name: 'FieldOrderEditor',
  props: {
    title: {
      type: String,
      required: true,
    },
    emptyText: {
      type: String,
      required: true,
    },
    fields: {
      type: Array,
      default: () => [],
    },
    selectedRefs: {
      type: Array,
      default: () => [],
    },
    filter: {
      type: Function,
      required: true,
    },
  },
  emits: ['update'],
  setup(editorProps, { emit }) {
    const fieldMap = computed(() => new Map(editorProps.fields.map(field => [field.field, field])))
    const selectedRows = computed(() => editorProps.selectedRefs.map(ref => fieldMap.value.get(ref)).filter(Boolean))
    const availableRows = computed(() => editorProps.fields.filter((field) => {
      return editorProps.filter(field) && !editorProps.selectedRefs.includes(field.field)
    }))
    const updateRows = rows => emit('update', rows.map(row => row.field))
    const remove = field => emit('update', editorProps.selectedRefs.filter(ref => ref !== field))
    const add = field => emit('update', [...editorProps.selectedRefs, field])
    return () => h('div', { class: 'field-editor' }, [
      h('div', { class: 'field-editor-title' }, editorProps.title),
      selectedRows.value.length
        ? h(draggable, {
            'modelValue': selectedRows.value,
            'itemKey': 'field',
            'handle': '.field-handle',
            'animation': 160,
            'class': 'selected-field-list',
            'onUpdate:modelValue': updateRows,
          }, {
            item: ({ element }) => h('div', { class: 'selected-field-row' }, [
              h('span', { class: 'field-handle' }, '☰'),
              h('span', { class: 'field-name' }, element.label || element.field),
              h('span', { class: 'field-code' }, element.field),
              h('button', { type: 'button', onClick: () => remove(element.field) }, '移除'),
            ]),
          })
        : h('div', { class: 'field-empty' }, editorProps.emptyText),
      availableRows.value.length
        ? h('div', { class: 'available-field-list' }, availableRows.value.map(field => h('button', {
            key: field.field,
            type: 'button',
            onClick: () => add(field.field),
          }, `${field.label || field.field}`)))
        : null,
    ])
  },
})

const fieldMap = computed(() => new Map(props.fields.map(field => [field.field, field])))
const searchZone = computed(() => findZone('search'))
const tableZone = computed(() => findZone('table'))
const detailZone = computed(() => findZone('detail'))
const fieldOptions = computed(() => props.fields.map(field => ({
  label: field.label ? `${field.label}（${field.field}）` : field.field,
  value: field.field,
})))
const searchFields = computed(() => resolveFields(searchZone.value, field => field.searchable))
const tableFields = computed(() => resolveFields(tableZone.value, field => field.listVisible !== false))
const detailFields = computed(() => detailZone.value?.enabled === false ? [] : resolveFields(detailZone.value, field => field.formVisible !== false))
const treeConfig = computed(() => tableZone.value?.props?.treeConfig || {})
const tableColumns = computed(() => [
  ...tableFields.value.map(field => ({
    key: field.field,
    title: field.label || field.field,
    minWidth: field.width || 140,
    ellipsis: { tooltip: true },
  })),
  { key: 'actions', title: '操作', width: 140, fixed: 'right' },
])
const sampleRows = computed(() => {
  return Array.from({ length: 3 }).map((_, index) => {
    const row = { id: index + 1, actions: '编辑 / 删除' }
    tableFields.value.forEach((field) => {
      row[field.field] = resolveSampleValue(field, index)
    })
    return row
  })
})

function findZone(zoneKey) {
  return props.modelValue.zones?.find(zone => zone.zoneKey === zoneKey) || null
}

function resolveFields(zone, fallback) {
  if (!zone || zone.enabled === false)
    return []
  const refs = zone.fieldRefs?.length
    ? zone.fieldRefs
    : props.fields.filter(fallback).map(field => field.field)
  return refs.map(ref => fieldMap.value.get(ref)).filter(Boolean)
}

function updateZoneRefs(zoneKey, refs) {
  patchZone(zoneKey, { fieldRefs: refs })
}

function updateTableProp(key, value) {
  patchZone('table', {
    props: {
      ...(tableZone.value?.props || {}),
      [key]: value,
    },
  })
}

function updateTreeConfig(key, value) {
  patchZone('table', {
    props: {
      ...(tableZone.value?.props || {}),
      treeConfig: {
        ...(treeConfig.value || {}),
        [key]: value,
      },
    },
  })
}

function patchZone(zoneKey, patch) {
  const zones = (props.modelValue.zones || []).map((zone) => {
    if (zone.zoneKey !== zoneKey)
      return zone
    return {
      ...zone,
      ...patch,
      props: patch.props ? { ...(zone.props || {}), ...patch.props } : zone.props || {},
    }
  })
  emit('update:modelValue', { ...props.modelValue, zones })
}

function resetSearchFields() {
  updateZoneRefs('search', props.fields.filter(field => field.searchable).map(field => field.field))
}

function resetTableFields() {
  updateZoneRefs('table', props.fields.filter(field => field.listVisible !== false).map(field => field.field))
}

function resolveOptions(field) {
  if (field?.dictType)
    return [{ label: `${field.dictType}字典项`, value: '' }]
  return [
    { label: '选项一', value: '1' },
    { label: '选项二', value: '2' },
  ]
}

function resolveSampleValue(field, index = 0) {
  if (field.dictType)
    return '字典值'
  if (field.componentType === 'switch' || field.dataType === 'tinyint')
    return index % 2 === 0 ? '是' : '否'
  if (field.componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field.dataType))
    return field.dataType === 'decimal' ? `${128 + index}.00` : String(128 + index)
  if (field.componentType === 'date' || field.dataType === 'date')
    return '2026-05-20'
  if (field.componentType === 'datetime' || field.dataType === 'datetime')
    return '2026-05-20 09:30:00'
  return field.label ? `${field.label}示例${index + 1}` : `示例${index + 1}`
}
</script>

<style scoped>
.structured-designer {
  display: grid;
  gap: 14px;
}

.surface-section {
  padding: 16px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
}

.section-head,
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.section-head {
  margin-bottom: 14px;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.section-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.query-surface {
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.query-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.query-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
}

.table-toolbar {
  margin-bottom: 10px;
}

.toggle-label {
  font-size: 12px;
  color: #475569;
}

.preview-table {
  margin-bottom: 14px;
}

.tree-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.detail-preview {
  margin-bottom: 14px;
}

:deep(.field-editor) {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #eef2f7;
}

:deep(.field-editor-title) {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

:deep(.selected-field-list),
:deep(.available-field-list) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.selected-field-row) {
  display: grid;
  grid-template-columns: 16px auto auto auto;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 8px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
}

:deep(.field-handle) {
  color: #94a3b8;
  cursor: grab;
}

:deep(.field-name) {
  color: #0f172a;
  font-weight: 600;
}

:deep(.field-code) {
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

:deep(.selected-field-row button),
:deep(.available-field-list button) {
  border: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font-size: 12px;
}

:deep(.field-empty) {
  color: #94a3b8;
  font-size: 12px;
}

:deep(.available-field-list) {
  margin-top: 10px;
}

:deep(.available-field-list button) {
  min-height: 30px;
  padding: 0 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
}

@media (max-width: 1280px) {
  .query-grid,
  .tree-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
