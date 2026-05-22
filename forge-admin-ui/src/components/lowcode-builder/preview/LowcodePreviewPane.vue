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
      <n-space size="small">
        <n-button :loading="loading" @click="refreshPreview">
          后端校验
        </n-button>
        <n-button
          :loading="runtimeLoading"
          :disabled="!isPublished"
          type="primary"
          secondary
          @click="toggleRuntimePreview"
        >
          {{ showRuntimePreview ? '配置预览' : '真实运行预览' }}
        </n-button>
        <n-button :disabled="!isPublished" @click="openRuntimePage">
          打开运行页
        </n-button>
      </n-space>
    </div>

    <n-alert v-if="errorMsg" type="error" :bordered="false" class="preview-alert">
      {{ errorMsg }}
    </n-alert>
    <n-alert v-if="runtimeError" type="error" :bordered="false" class="preview-alert">
      {{ runtimeError }}
    </n-alert>
    <n-alert v-if="!isPublished" type="info" :bordered="false" class="preview-alert">
      当前展示的是草稿配置预览。保存并发布后，可在这里切换到真实运行预览，加载数据库数据并验证新增、编辑、删除等交互动作。
    </n-alert>

    <div v-if="showRuntimePreview" class="runtime-crud-preview">
      <div class="runtime-preview-head">
        <div>
          <div class="band-title">
            真实运行态预览
          </div>
          <p>当前区域连接已发布运行接口，页面动作会按正式低代码 CRUD 执行。</p>
        </div>
        <n-tag type="success" :bordered="false">
          已发布
        </n-tag>
      </div>
      <AiCrudPage v-bind="runtimeCrudProps" />
    </div>

    <div v-else class="preview-surface">
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
                <n-button
                  v-for="action in customToolbarActions"
                  :key="action.key"
                  size="small"
                  :type="action.type === 'default' ? undefined : action.type"
                  disabled
                >
                  {{ action.label }}
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

      <div v-if="formFields.length || childFieldGroups.length" class="preview-band">
        <div class="band-title">
          表单与详情
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
        <div v-if="childFieldGroups.length" class="child-preview-groups">
          <div v-for="group in childFieldGroups" :key="group.key" class="child-preview-group">
            <div class="child-preview-title">
              {{ group.name }}
            </div>
            <div class="child-preview-table">
              <span v-for="field in group.fields" :key="field.field">
                {{ field.label || field.field }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, h, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { crudConfigRender } from '@/api/ai'
import { lowcodePreview } from '@/api/lowcode-crud'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import DictTag from '@/components/DictTag.vue'
import {
  buildPageDesignModelSchema,
  isPageFieldVisible,
  resolveFieldRefsFromFormCreateRules,
} from '@/components/lowcode-builder/page/page-schema'
import { getDictData } from '@/composables/useDict'

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

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const runtimeLoading = ref(false)
const runtimeError = ref('')
const runtimeConfig = ref(null)
const previewMode = ref('draft')
const dictCache = ref({})

const pageSchema = computed(() => props.draft.pageSchema || { zones: [] })
const modelSchema = computed(() => {
  return buildPageDesignModelSchema(props.draft.modelSchema || { fields: [] }, pageSchema.value.modelRefs || [])
})
const tableZone = computed(() => pageSchema.value.zones?.find(zone => zone.zoneKey === 'table'))
const isPublished = computed(() => props.draft.publishStatus === 'PUBLISHED')
const showRuntimePreview = computed(() => Boolean(previewMode.value === 'runtime' && runtimeConfig.value))

const fieldMap = computed(() => new Map((modelSchema.value.fields || []).map(field => [field.field, field])))

const searchFields = computed(() => resolveFields('search', field => field.searchable))
const visibleColumns = computed(() => resolveFields('table', field => field.listVisible !== false))
const editFields = computed(() => resolveFields('edit', field => field.formVisible !== false))
const primaryModelCode = computed(() => pageSchema.value.primaryModelCode
  || pageSchema.value.modelRefs?.find(ref => ref.primary)?.modelCode
  || props.draft.modelSchema?.object?.code
  || '')
const formFields = computed(() => editFields.value.filter(field => isPrimaryFormField(field)))
const childFieldGroups = computed(() => {
  if (pageSchema.value.layoutType !== 'master-detail-crud')
    return []
  const groupMap = new Map()
  editFields.value.filter(field => !isPrimaryFormField(field)).forEach((field) => {
    const key = field.modelCode || 'children'
    if (!groupMap.has(key)) {
      groupMap.set(key, {
        key,
        name: field.modelName || key,
        fields: [],
      })
    }
    groupMap.get(key).fields.push(field)
  })
  return Array.from(groupMap.values())
})
const customToolbarActions = computed(() => resolveCustomActions('toolbar'))
const customRowActions = computed(() => resolveCustomActions('row'))

const tableColumns = computed(() => visibleColumns.value.map(field => ({
  key: field.field,
  title: field.label || field.field,
  minWidth: field.width || 140,
})).concat({
  key: 'actions',
  title: '操作',
  width: Math.max(140, (2 + customRowActions.value.length) * 54),
  fixed: 'right',
  render: row => renderDraftActions(row),
}))

const sampleRows = computed(() => {
  return Array.from({ length: 3 }).map((_, index) => {
    const row = { id: index + 1, actions: '编辑 / 删除' }
    visibleColumns.value.forEach((field) => {
      row[field.field] = resolveSampleValue(field, index)
    })
    return row
  })
})

const runtimeCrudProps = computed(() => buildRuntimeCrudProps(runtimeConfig.value))

watch(
  () => [props.draft.configKey, props.draft.publishStatus],
  () => {
    runtimeConfig.value = null
    runtimeError.value = ''
    previewMode.value = 'draft'
  },
)

function resolveFields(zoneKey, fallback) {
  const zone = pageSchema.value.zones?.find(item => item.zoneKey === zoneKey)
  const formCreateRefs = zoneKey === 'edit'
    ? resolveFieldRefsFromFormCreateRules(zone?.props?.formCreateRule, new Set(fieldMap.value.keys()))
    : []
  if (formCreateRefs.length)
    return formCreateRefs.map(ref => fieldMap.value.get(ref)).filter(field => field && isPageFieldVisible(field, zoneKey))
  if (zone?.fieldRefs?.length) {
    return uniqueRefs(zone.fieldRefs).map(ref => fieldMap.value.get(ref)).filter(field => field && isPageFieldVisible(field, zoneKey))
  }
  const canvasRefs = resolveCanvasRefs(zone, zoneKey)
  if (canvasRefs.length) {
    return canvasRefs.map(ref => fieldMap.value.get(ref)).filter(field => field && isPageFieldVisible(field, zoneKey))
  }
  return (modelSchema.value.fields || []).filter(field => fallback(field) && isPageFieldVisible(field, zoneKey))
}

function isPrimaryFormField(field) {
  if (pageSchema.value.layoutType !== 'master-detail-crud')
    return true
  return !field.modelCode || field.modelCode === primaryModelCode.value
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

function resolveCustomActions(position) {
  return (tableZone.value?.props?.customActions || [])
    .filter(action => (action.position || 'toolbar') === position)
}

function renderDraftActions(row) {
  const actions = [
    { key: 'edit', label: '编辑', type: 'primary' },
    { key: 'delete', label: '删除', type: 'error' },
    ...customRowActions.value,
  ]
  return h('div', { class: 'draft-action-column' }, actions.map((action, index) => [
    index > 0 ? h('span', { class: 'draft-action-divider' }, ' | ') : null,
    h('span', { class: ['draft-action-link', `type-${action.type || 'default'}`] }, action.label),
  ]).flat().filter(Boolean))
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
    window.$message?.success('草稿配置校验通过')
  }
  catch (e) {
    errorMsg.value = e?.message || '预览失败'
  }
  finally {
    loading.value = false
  }
}

async function toggleRuntimePreview() {
  if (showRuntimePreview.value) {
    previewMode.value = 'draft'
    return
  }
  await loadRuntimePreview()
}

async function loadRuntimePreview() {
  if (!isPublished.value) {
    window.$message?.warning('应用发布后才能加载真实运行态')
    return
  }
  if (!props.draft.configKey) {
    window.$message?.warning('缺少应用编码，无法加载运行态')
    return
  }
  runtimeLoading.value = true
  runtimeError.value = ''
  try {
    const res = await crudConfigRender(props.draft.configKey)
    runtimeConfig.value = res.data
    await preloadDicts(runtimeConfig.value)
    previewMode.value = 'runtime'
  }
  catch (e) {
    runtimeError.value = e?.message || '真实运行态预览加载失败'
  }
  finally {
    runtimeLoading.value = false
  }
}

function openRuntimePage() {
  if (!isPublished.value || !props.draft.configKey)
    return
  const route = router.resolve(`/ai/crud-page/${props.draft.configKey}`)
  window.open(route.href, '_blank')
}

function buildRuntimeCrudProps(cfg) {
  if (!cfg)
    return {}
  const options = cfg.options || {}
  return {
    searchSchema: transformFields(cfg.searchSchema),
    columns: transformColumns(cfg.columnsSchema, cfg.transConfig),
    editSchema: transformFields(cfg.editSchema),
    childrenConfig: transformChildrenConfig(options.masterDetailConfig?.children || []),
    apiConfig: cfg.apiConfig || {},
    options,
    rowKey: cfg.rowKey || 'id',
    modalType: options.modalType || cfg.modalType || 'drawer',
    modalWidth: options.modalWidth || cfg.modalWidth || '800px',
    editGridCols: options.editGridCols || cfg.editGridCols || 1,
    searchGridCols: options.searchGridCols || cfg.searchGridCols || 4,
    hideBatchDelete: !!options.hideBatchDelete,
    showImport: !!options.showImport,
    showExport: !!options.showExport,
    importApi: extractApiUrl(cfg.apiConfig?.import),
    exportApi: cfg.apiConfig?.export || '',
    importTemplateUrl: extractApiUrl(cfg.apiConfig?.importTemplate),
    enableCustomQuery: options.enableCustomQuery !== false,
    customQueryConfigKey: cfg.configKey,
    toolbarActions: options.toolbarActions || [],
  }
}

function transformColumns(columns, transConfig) {
  const transMap = {}
  if (transConfig && typeof transConfig === 'object') {
    for (const [field, conf] of Object.entries(transConfig))
      transMap[field] = conf.targetField || `${field}Name`
  }
  return (columns || []).map((col) => {
    const key = col.prop || col.key || col.dataIndex
    const nextCol = { ...col, prop: key }
    if (transMap[key]) {
      const targetField = transMap[key]
      nextCol.render = row => row[targetField] ?? row[key]
      return nextCol
    }
    if (col.render && typeof col.render === 'object' && col.render.type === 'dictTag') {
      nextCol.render = row => h(DictTag, {
        dictType: col.render.dictType,
        value: row[key],
        size: 'small',
      })
    }
    return nextCol
  })
}

function transformFields(fields) {
  return (fields || []).map((field) => {
    const nextField = { ...field }
    if (field.dictType && ['select', 'radio', 'checkbox'].includes(field.type)) {
      nextField.props = {
        ...(nextField.props || {}),
        options: dictCache.value[field.dictType] || [],
      }
    }
    if (['date', 'datetime', 'time'].includes(field.type?.toLowerCase?.() || '')) {
      nextField.props = {
        ...(nextField.props || {}),
        format: 'yyyy-MM-dd HH:mm:ss',
        valueFormat: 'yyyy-MM-dd HH:mm:ss',
      }
    }
    return nextField
  })
}

function transformChildrenConfig(children = []) {
  return (children || []).map(child => ({
    ...child,
    fields: transformFields(child.fields || []),
  }))
}

async function preloadDicts(cfg) {
  const types = new Set()
  ;(cfg?.columnsSchema || []).forEach((col) => {
    if (col.render?.dictType)
      types.add(col.render.dictType)
  })
  ;[...(cfg?.searchSchema || []), ...(cfg?.editSchema || [])].forEach((field) => {
    if (field.dictType)
      types.add(field.dictType)
  })
  ;(cfg?.options?.masterDetailConfig?.children || []).forEach((child) => {
    ;(child.fields || []).forEach((field) => {
      if (field.dictType)
        types.add(field.dictType)
    })
  })
  for (const type of types) {
    if (!dictCache.value[type]) {
      try {
        dictCache.value[type] = await getDictData(type)
      }
      catch (e) {
        console.warn(`[lowcode-preview] 加载字典 ${type} 失败`, e)
      }
    }
  }
}

function extractApiUrl(apiConfigValue) {
  if (!apiConfigValue)
    return ''
  const parts = String(apiConfigValue).split('@')
  return parts.length > 1 ? parts.slice(1).join('@') : apiConfigValue
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

.runtime-crud-preview {
  display: grid;
  gap: 12px;
  min-height: 620px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.runtime-preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eef2f7;
}

.runtime-preview-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
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

.draft-action-column {
  white-space: nowrap;
}

.draft-action-link {
  color: #2563eb;
  cursor: default;
  font-size: 12px;
}

.draft-action-link.type-error,
.draft-action-link.type-danger {
  color: #dc2626;
}

.draft-action-link.type-warning {
  color: #d97706;
}

.draft-action-link.type-success {
  color: #16a34a;
}

.draft-action-divider {
  color: #cbd5e1;
}

.search-grid,
.form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.child-preview-groups {
  display: grid;
  gap: 10px;
  margin-top: 14px;
  border-top: 1px solid #eef2f7;
  padding-top: 12px;
}

.child-preview-group {
  display: grid;
  gap: 8px;
}

.child-preview-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.child-preview-table {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  padding: 10px;
}

.child-preview-table span {
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  background: #fff;
  color: #475569;
  font-size: 12px;
  padding: 5px 8px;
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
