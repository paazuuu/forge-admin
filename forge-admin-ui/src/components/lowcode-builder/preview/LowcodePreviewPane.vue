<template>
  <div class="preview-pane">
    <div class="preview-toolbar">
      <div>
        <div class="preview-title">
          {{ modelSchema.businessName || '业务应用预览' }}
        </div>
        <div class="preview-desc">
          {{ modelSchema.tableName }} · {{ visibleColumns.length }} 个列表字段
        </div>
      </div>
      <n-button :loading="loading" @click="refreshPreview">
        刷新预览
      </n-button>
    </div>

    <n-alert v-if="errorMsg" type="error" :bordered="false" class="preview-alert">
      {{ errorMsg }}
    </n-alert>

    <div class="preview-surface">
      <div class="preview-band runtime-surface">
        <div class="band-title">
          {{ pageSchema.layoutType === 'tree-crud' ? '左树右表运行态' : '列表页面运行态' }}
        </div>
        <div class="runtime-layout" :class="{ tree: pageSchema.layoutType === 'tree-crud' }">
          <aside v-if="pageSchema.layoutType === 'tree-crud'" class="tree-preview">
            <div class="tree-title">
              {{ tableZone?.props?.treeConfig?.treeTitle || `${modelSchema.businessName || '业务'}树` }}
            </div>
            <div class="tree-node active">
              全部
            </div>
            <div class="tree-node">
              示例节点一
            </div>
            <div class="tree-node">
              示例节点二
            </div>
          </aside>
          <div class="runtime-main">
            <div v-if="searchFields.length" class="query-set-preview">
              <div class="search-grid">
                <n-form-item
                  v-for="field in searchFields"
                  :key="field.field"
                  :label="field.label"
                  :show-feedback="false"
                >
                  <n-input v-if="!field.dictType" disabled :placeholder="`请输入${field.label}`" />
                  <n-select v-else disabled placeholder="请选择" />
                </n-form-item>
              </div>
              <div class="query-actions">
                <n-button size="small" type="primary" disabled>
                  查询
                </n-button>
                <n-button size="small" disabled>
                  重置
                </n-button>
                <n-button size="small" text type="primary" disabled>
                  条件收起
                </n-button>
                <n-button v-if="tableZone?.props?.enableCustomQuery !== false" size="small" text disabled>
                  自定义查询
                </n-button>
              </div>
            </div>
            <div class="table-actions">
              <n-space>
                <n-button size="small" type="primary" disabled>
                  新增
                </n-button>
                <n-button v-if="tableZone?.props?.showExport" size="small" disabled>
                  数据导出
                </n-button>
                <n-button v-if="tableZone?.props?.showImport" size="small" disabled>
                  批量导入
                </n-button>
              </n-space>
            </div>
            <n-data-table
              :columns="tableColumns"
              :data="sampleRows"
              :bordered="false"
              size="small"
            />
          </div>
        </div>
      </div>

      <div v-if="formFields.length" class="preview-band">
        <div class="band-title">
          数据录入表单
        </div>
        <div class="form-grid">
          <n-form-item
            v-for="field in formFields"
            :key="field.field"
            :label="field.label"
            :required="field.required"
          >
            <n-input v-if="['input', 'textarea'].includes(field.componentType)" disabled :placeholder="`请输入${field.label}`" />
            <n-input-number v-else-if="field.componentType === 'number'" disabled style="width: 100%" />
            <n-select v-else-if="field.dictType || ['select', 'radio', 'checkbox'].includes(field.componentType)" disabled placeholder="请选择" />
            <n-date-picker v-else-if="['date', 'datetime'].includes(field.componentType)" disabled style="width: 100%" />
            <n-input v-else disabled :placeholder="field.componentType" />
          </n-form-item>
        </div>
      </div>

      <div v-if="detailFields.length" class="preview-band">
        <div class="band-title">
          查询详情页
        </div>
        <n-descriptions :column="2" bordered size="small">
          <n-descriptions-item
            v-for="field in detailFields"
            :key="field.field"
            :label="field.label"
          >
            {{ field.label }}示例
          </n-descriptions-item>
        </n-descriptions>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { lowcodePreview } from '@/api/lowcode-crud'
import { resolveFieldRefsFromFormCreateRules } from '@/components/lowcode-builder/page/page-schema'

const props = defineProps({
  appId: {
    type: [String, Number],
    default: null,
  },
  draft: {
    type: Object,
    required: true,
  },
})

const loading = ref(false)
const errorMsg = ref('')

const modelSchema = computed(() => props.draft.modelSchema || { fields: [] })
const pageSchema = computed(() => props.draft.pageSchema || { zones: [] })
const tableZone = computed(() => pageSchema.value.zones?.find(zone => zone.zoneKey === 'table'))
const detailZone = computed(() => pageSchema.value.zones?.find(zone => zone.zoneKey === 'detail'))

const fieldMap = computed(() => new Map((modelSchema.value.fields || []).map(field => [field.field, field])))

const searchFields = computed(() => resolveFields('search', field => field.searchable))
const visibleColumns = computed(() => resolveFields('table', field => field.listVisible !== false))
const formFields = computed(() => resolveFields('edit', field => field.formVisible !== false))
const detailFields = computed(() => detailZone.value?.enabled === false ? [] : resolveFields('detail', field => field.formVisible !== false))

const tableColumns = computed(() => visibleColumns.value.map(field => ({
  key: field.field,
  title: field.label || field.field,
  minWidth: field.width || 140,
})).concat({ key: 'actions', title: '操作', width: 140, fixed: 'right' }))

const sampleRows = computed(() => {
  return Array.from({ length: 3 }).map((_, index) => {
    const row = { id: index + 1, actions: '编辑 / 删除' }
    visibleColumns.value.forEach((field) => {
      row[field.field] = resolveSampleValue(field, index)
    })
    return row
  })
})

function resolveFields(zoneKey, fallback) {
  const zone = pageSchema.value.zones?.find(item => item.zoneKey === zoneKey)
  const formCreateRefs = zoneKey === 'edit'
    ? resolveFieldRefsFromFormCreateRules(zone?.props?.formCreateRule, new Set(fieldMap.value.keys()))
    : []
  if (formCreateRefs.length)
    return formCreateRefs.map(ref => fieldMap.value.get(ref)).filter(Boolean)
  if (zone?.fieldRefs?.length) {
    return uniqueRefs(zone.fieldRefs).map(ref => fieldMap.value.get(ref)).filter(Boolean)
  }
  const canvasRefs = resolveCanvasRefs(zone, zoneKey)
  if (canvasRefs.length) {
    return canvasRefs.map(ref => fieldMap.value.get(ref)).filter(Boolean)
  }
  return (modelSchema.value.fields || []).filter(fallback)
}

function resolveCanvasRefs(zone, zoneKey) {
  const items = [...(zone?.props?.canvas?.items || [])].sort((a, b) => Number(a.zIndex || 0) - Number(b.zIndex || 0))
  const primary = zoneKey === 'table'
    ? items.find(item => item.componentKey === 'data-table')
    : zoneKey === 'search'
      ? items.find(item => item.componentKey === 'query-set')
      : null
  if (primary) {
    const refs = uniqueRefs(primary.fieldRefs || primary.props?.fieldRefs || [])
    if (refs.length)
      return refs
  }
  const refs = []
  items.forEach((item) => {
    if (item.fieldRef)
      refs.push(item.fieldRef)
    refs.push(...(item.fieldRefs || item.props?.fieldRefs || []))
  })
  return uniqueRefs(refs)
}

function uniqueRefs(refs) {
  return Array.from(new Set((refs || []).filter(Boolean)))
}

function resolveSampleValue(field, index = 0) {
  if (!field)
    return '示例值'
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

async function refreshPreview() {
  if (!props.appId) {
    window.$message?.info('新应用保存草稿后可执行后端预览校验')
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await lowcodePreview(props.appId, props.draft)
    window.$message?.success('预览配置校验通过')
  }
  catch (e) {
    errorMsg.value = e?.message || '预览失败'
  }
  finally {
    loading.value = false
  }
}
</script>

<style scoped>
.preview-pane {
  display: grid;
  gap: 14px;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.preview-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.preview-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.preview-alert {
  border-radius: 8px;
}

.preview-surface {
  display: grid;
  gap: 14px;
}

.preview-band {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.runtime-surface {
  padding: 0;
  overflow: hidden;
}

.runtime-surface .band-title {
  margin: 0;
  padding: 14px 16px;
  border-bottom: 1px solid #eef2f7;
}

.runtime-layout {
  display: grid;
  gap: 0;
}

.runtime-layout.tree {
  grid-template-columns: 240px minmax(0, 1fr);
}

.tree-preview {
  padding: 14px;
  border-right: 1px solid #eef2f7;
  background: #f8fafc;
}

.tree-title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.tree-node {
  min-height: 32px;
  display: flex;
  align-items: center;
  padding: 0 10px;
  border-radius: 6px;
  color: #475569;
  font-size: 12px;
}

.tree-node.active {
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 700;
}

.runtime-main {
  padding: 14px;
}

.query-set-preview {
  margin-bottom: 12px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.query-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.band-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
}

.table-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.table-actions .band-title {
  margin-bottom: 0;
}

.search-grid,
.form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 1180px) {
  .runtime-layout.tree {
    grid-template-columns: 1fr;
  }

  .tree-preview {
    border-right: 0;
    border-bottom: 1px solid #eef2f7;
  }

  .search-grid,
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
