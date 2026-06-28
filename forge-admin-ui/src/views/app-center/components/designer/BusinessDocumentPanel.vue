<template>
  <div class="business-document-panel">
    <div class="document-head">
      <div>
        <h3>单据设置</h3>
        <p>维护单据字段、编号生成和状态生命周期，主流程统一在流程与自动化中配置。</p>
      </div>
      <n-space align="center" size="small">
        <n-tag :type="form.documentEnabled ? 'success' : 'default'" :bordered="false">
          {{ form.documentEnabled ? '已启用' : '未启用' }}
        </n-tag>
        <n-button size="small" secondary :loading="loading" @click="loadConfig">
          刷新
        </n-button>
        <n-button size="small" type="primary" :loading="saving" @click="saveConfig">
          保存单据
        </n-button>
      </n-space>
    </div>

    <div class="document-body">
      <main class="document-main">
        <div class="document-rail">
          <div :class="{ active: form.documentEnabled }">
            <span>01</span>
            <strong>单据模式</strong>
            <em>{{ form.documentEnabled ? '运行态已启用' : '普通数据管理' }}</em>
          </div>
          <div :class="{ active: !!form.options.documentNoField && !!form.noRuleTemplate }">
            <span>02</span>
            <strong>编号生成</strong>
            <em>{{ form.options.documentNoField || '待选择字段' }}</em>
          </div>
          <div :class="{ active: !!form.statusField && statusValueOptions.length }">
            <span>03</span>
            <strong>状态字典</strong>
            <em>{{ statusValueOptions.length ? `${statusValueOptions.length} 个可选值` : '待绑定选项' }}</em>
          </div>
          <div :class="{ active: !!form.mainFlowSummary?.configured }">
            <span>04</span>
            <strong>主流程</strong>
            <em>{{ form.mainFlowSummary?.flowModelName || form.mainFlowSummary?.flowModelKey || '未配置' }}</em>
          </div>
        </div>

        <n-spin :show="loading">
          <div class="document-config-grid">
            <section class="document-section">
              <div class="section-head">
                <div>
                  <h4>基础配置</h4>
                  <p>启用后运行态会按单据生命周期处理状态、流程和权限。</p>
                </div>
                <n-switch v-model:value="form.documentEnabled" @update:value="handleDocumentEnabledChange" />
              </div>

              <n-form label-placement="top" size="small" :show-feedback="false">
                <n-grid :cols="2" :x-gap="14" :y-gap="8" responsive="screen">
                  <n-form-item-gi label="单据名称">
                    <n-input
                      v-model:value="form.documentName"
                      :disabled="!form.documentEnabled"
                      :placeholder="defaultDocumentName"
                      @update:value="markDirty"
                    />
                  </n-form-item-gi>
                  <n-form-item-gi label="状态字段">
                    <n-select
                      v-model:value="form.statusField"
                      :disabled="!form.documentEnabled"
                      :options="statusFieldOptions"
                      clearable
                      filterable
                      placeholder="选择带字典或选项的状态字段"
                      @update:value="handleStatusFieldChange"
                    />
                  </n-form-item-gi>
                  <n-form-item-gi label="发起人字段">
                    <n-select
                      v-model:value="form.starterField"
                      :disabled="!form.documentEnabled"
                      :options="fieldOptions"
                      clearable
                      filterable
                      placeholder="选择记录发起人字段"
                      @update:value="markDirty"
                    />
                  </n-form-item-gi>
                  <n-form-item-gi label="负责人字段">
                    <n-select
                      v-model:value="form.ownerField"
                      :disabled="!form.documentEnabled"
                      :options="fieldOptions"
                      clearable
                      filterable
                      placeholder="选择负责人字段"
                      @update:value="markDirty"
                    />
                  </n-form-item-gi>
                </n-grid>
              </n-form>

              <div class="field-hints">
                <span>状态字段必须绑定字典或选项，状态映射只能从这些值中选择。</span>
                <span>发起人和负责人用于流程发起、待办归属和消息接收。</span>
              </div>
            </section>

            <section class="document-section">
              <div class="section-head">
                <div>
                  <h4>编号生成</h4>
                  <p>选择编号写入字段并维护生成规则；真实新增时由后端生成，不由用户手填。</p>
                </div>
              </div>
              <n-form label-placement="top" size="small" :show-feedback="false">
                <n-form-item label="编号字段">
                  <n-select
                    v-model:value="form.options.documentNoField"
                    :disabled="!form.documentEnabled"
                    :options="documentNoFieldOptions"
                    clearable
                    filterable
                    placeholder="选择申请单号/单据编号字段"
                    @update:value="markDirty"
                  />
                </n-form-item>
              </n-form>
              <DocumentNoRuleEditor
                v-model="form.noRuleTemplate"
                :disabled="!form.documentEnabled"
                :suite-code="effectiveSuiteCode"
                :object-code="effectiveObjectCode"
                :field-options="fieldOptions"
                @preview="handleNoRulePreview"
                @update:model-value="markDirty"
              />
            </section>
          </div>

          <section class="document-section">
            <div class="section-head">
              <div>
                <h4>状态映射</h4>
                <p>将标准单据状态映射到状态字段的字典值，并控制该状态下能否编辑、删除或发起主流程。</p>
              </div>
            </div>
            <DocumentStatusMappingTable
              :rows="form.statusMappingRows"
              :disabled="!form.documentEnabled"
              :status-field="form.statusField"
              :fields="fields"
              :status-value-options="statusValueOptions"
              :loading-options="statusOptionsLoading"
              @update:rows="updateStatusRows"
            />
          </section>

          <section class="document-section">
            <div class="section-head">
              <div>
                <h4>详情页流程展示</h4>
                <p>记录已关联流程实例时，详情页以 Tab 展示业务数据和流程进度。</p>
              </div>
            </div>
            <div class="display-switches">
              <label>
                <span>流程时间轴</span>
                <n-switch
                  v-model:value="form.options.detailFlowTimelineVisible"
                  :disabled="!form.documentEnabled"
                  @update:value="markDirty"
                />
              </label>
              <label>
                <span>流程图</span>
                <n-switch
                  v-model:value="form.options.detailFlowDiagramVisible"
                  :disabled="!form.documentEnabled"
                  @update:value="markDirty"
                />
              </label>
            </div>
          </section>
        </n-spin>
      </main>

      <aside class="document-side">
        <DocumentConfigSummary
          :form="form"
          :no-rule-preview="noRulePreview"
          @configure-flow="$emit('configureFlow')"
        />
      </aside>
    </div>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { businessDocumentConfig, saveBusinessDocumentConfig } from '@/api/business-app'
import { getDictData } from '@/composables/useDict'
import DocumentConfigSummary from './DocumentConfigSummary.vue'
import DocumentNoRuleEditor from './DocumentNoRuleEditor.vue'
import DocumentStatusMappingTable from './DocumentStatusMappingTable.vue'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  suiteCode: {
    type: String,
    default: '',
  },
  objectCode: {
    type: String,
    default: '',
  },
  objectName: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  initialConfig: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['dirtyChange', 'saved', 'loaded', 'configureFlow'])

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const noRulePreview = ref(null)
const statusOptionsLoading = ref(false)
const statusValueOptions = ref([])
const form = reactive(createDefaultConfig())
let statusOptionLoadSeq = 0

const defaultDocumentName = computed(() => `${props.objectName || '业务单元'}单据`)
const effectiveSuiteCode = computed(() => props.suiteCode || form.suiteCode || '')
const effectiveObjectCode = computed(() => props.objectCode || form.objectCode || '')
const activeFields = computed(() => props.fields.filter(field => fieldCode(field) && !isInactiveField(field)))
const fieldMap = computed(() => new Map(activeFields.value.map(field => [fieldCode(field), field])))
const fieldOptions = computed(() => activeFields.value.map(field => ({
  label: `${field.fieldName || field.label || fieldCode(field)}（${fieldCode(field)}）`,
  value: fieldCode(field),
})))
const statusFieldOptions = computed(() => activeFields.value.map((field) => {
  const hasOptions = hasFieldSelectableValues(field)
  const label = `${field.fieldName || field.label || fieldCode(field)}（${fieldCode(field)}）`
  return {
    label: hasOptions ? label : `${label} - 未配置选项`,
    value: fieldCode(field),
    disabled: !hasOptions,
  }
}))
const selectedStatusField = computed(() => fieldMap.value.get(form.statusField) || null)
const documentNoFieldOptions = computed(() => {
  const inferred = inferDocumentNoField()
  return activeFields.value
    .filter(isDocumentNoCandidate)
    .map(field => ({
      label: `${field.fieldName || field.label || fieldCode(field)}（${fieldCode(field)}）${fieldCode(field) === inferred ? ' - 推荐' : ''}`,
      value: fieldCode(field),
    }))
})

watch(() => props.objectId, () => {
  loadConfig()
}, { immediate: true })

watch(() => props.initialConfig, (value) => {
  if (!props.objectId && value)
    assignConfig(value)
}, { deep: true })

watch(selectedStatusField, async (field) => {
  const seq = ++statusOptionLoadSeq
  statusOptionsLoading.value = true
  try {
    const options = await resolveStatusValueOptions(field)
    if (seq === statusOptionLoadSeq)
      statusValueOptions.value = options
  }
  finally {
    if (seq === statusOptionLoadSeq)
      statusOptionsLoading.value = false
  }
}, { immediate: true })

watch(activeFields, () => {
  ensureDocumentNoField()
}, { deep: true, immediate: true })

async function loadConfig() {
  if (!props.objectId) {
    assignConfig(props.initialConfig || createDefaultConfig())
    return
  }
  loading.value = true
  try {
    const configRes = await businessDocumentConfig(props.objectId)
    assignConfig(configRes.data || props.initialConfig || createDefaultConfig())
    emit('loaded', { ...form })
    emit('dirtyChange', false)
  }
  finally {
    loading.value = false
  }
}

async function saveConfig() {
  if (!props.objectId)
    return
  if (form.documentEnabled && !form.statusField) {
    message.warning('启用单据模式后必须选择状态字段')
    return
  }
  if (form.documentEnabled && form.statusField && !statusValueOptions.value.length) {
    message.warning('状态字段未配置字典或选项，不能保存状态映射')
    return
  }
  ensureDocumentNoField()
  if (form.documentEnabled && form.noRuleTemplate && !form.options.documentNoField) {
    message.warning('配置编号规则后必须选择编号字段')
    return
  }
  if (noRulePreview.value?.valid === false) {
    message.warning('编号规则存在错误，请先修正预览提示')
    return
  }
  saving.value = true
  try {
    await saveBusinessDocumentConfig(props.objectId, buildPayload())
    message.success('单据设置已保存')
    emit('dirtyChange', false)
    emit('saved', { ...form })
    await loadConfig()
  }
  finally {
    saving.value = false
  }
}

function buildPayload() {
  const statusMappingRows = normalizeStatusRows(form.statusMappingRows)
  return {
    documentEnabled: !!form.documentEnabled,
    documentName: form.documentName || defaultDocumentName.value,
    documentNoRule: form.noRuleTemplate || '',
    noRuleTemplate: form.noRuleTemplate || '',
    statusField: form.statusField || '',
    starterField: form.starterField || '',
    ownerField: form.ownerField || '',
    defaultFlowKey: form.defaultFlowKey || form.mainFlowSummary?.flowModelKey || '',
    statusMapping: statusMappingFromRows(statusMappingRows),
    statusMappingRows,
    statusActionPolicy: form.statusActionPolicy || {},
    options: {
      ...(form.options || {}),
      documentNoField: form.options?.documentNoField || '',
    },
  }
}

function assignConfig(value = {}) {
  const options = {
    ...defaultDocumentOptions(),
    ...(value.options || {}),
  }
  options.documentNoField = options.documentNoField || value.documentNoField || ''
  options.detailFlowTimelineVisible = readBoolean(options.detailFlowTimelineVisible, true)
  options.detailFlowDiagramVisible = readBoolean(options.detailFlowDiagramVisible, true)
  Object.assign(form, {
    ...createDefaultConfig(),
    ...value,
    documentEnabled: readBoolean(value.documentEnabled, false),
    noRuleTemplate: value.noRuleTemplate || value.documentNoRule || '',
    statusMappingRows: normalizeStatusRows(value.statusMappingRows || rowsFromLegacyMapping(value.statusMapping || {})),
    statusActionPolicy: { ...(value.statusActionPolicy || {}) },
    mainFlowSummary: { ...(value.mainFlowSummary || {}) },
    options,
  })
  ensureDocumentNoField()
  noRulePreview.value = value.noRulePreview || null
}

function updateStatusRows(rows) {
  form.statusMappingRows = normalizeStatusRows(rows)
  markDirty()
}

function handleNoRulePreview(preview) {
  noRulePreview.value = preview
}

function handleDocumentEnabledChange() {
  ensureDocumentNoField()
  markDirty()
}

function handleStatusFieldChange() {
  markDirty()
}

function normalizeStatusRows(rows = []) {
  const defaults = defaultStatusRows()
  const byKey = new Map(defaults.map(row => [row.standardStatus, { ...row }]))
  for (const row of rows || []) {
    if (!row?.standardStatus)
      continue
    const key = String(row.standardStatus).toUpperCase()
    byKey.set(key, { ...(byKey.get(key) || {}), ...row, standardStatus: key })
  }
  return Array.from(byKey.values())
}

function rowsFromLegacyMapping(mapping = {}) {
  return defaultStatusRows().map(row => ({
    ...row,
    statusValue: mapping[row.standardStatus] || row.statusValue,
  }))
}

function statusMappingFromRows(rows = []) {
  return rows.reduce((result, row) => {
    if (row.standardStatus && row.statusValue)
      result[row.standardStatus] = row.statusValue
    return result
  }, {})
}

function defaultStatusRows() {
  return [
    row('DRAFT', '草稿', 'DRAFT', '草稿', 'default', true, true, true),
    row('SUBMITTED', '已提交', 'SUBMITTED', '已提交', 'info', false, false, false),
    row('IN_PROCESS', '流程中', 'IN_PROCESS', '流程中', 'warning', false, false, false),
    row('APPROVED', '已通过', 'APPROVED', '已通过', 'success', false, false, false),
    row('REJECTED', '已驳回', 'REJECTED', '已驳回', 'error', true, false, true),
    row('CANCELED', '已撤回', 'CANCELED', '已撤回', 'default', true, false, true),
    row('CLOSED', '已关闭', 'CLOSED', '已关闭', 'default', false, false, false),
  ]
}

function row(standardStatus, standardLabel, statusValue, displayName, tagType, allowEdit, allowDelete, allowStartFlow) {
  return { standardStatus, standardLabel, statusValue, displayName, tagType, allowEdit, allowDelete, allowStartFlow }
}

function createDefaultConfig() {
  return {
    documentEnabled: false,
    documentName: '',
    documentNoRule: '',
    noRuleTemplate: '',
    statusField: '',
    starterField: '',
    ownerField: '',
    defaultFlowKey: '',
    statusMappingRows: defaultStatusRows(),
    statusActionPolicy: {},
    mainFlowSummary: {},
    options: defaultDocumentOptions(),
  }
}

function defaultDocumentOptions() {
  return {
    documentNoField: '',
    detailFlowTimelineVisible: true,
    detailFlowDiagramVisible: true,
  }
}

function ensureDocumentNoField() {
  if (!form.documentEnabled || form.options.documentNoField)
    return
  form.options.documentNoField = inferDocumentNoField()
}

function inferDocumentNoField() {
  const candidates = activeFields.value
    .filter(field => !isInactiveField(field))
    .map(field => ({
      field,
      code: fieldCode(field),
      label: String(field.fieldName || field.label || ''),
    }))
    .filter(item => item.code)
  const exact = candidates.find(item => ['documentNo', 'document_no', 'applicationNo', 'application_no', 'billNo', 'bill_no'].includes(item.code))
  if (exact)
    return exact.code
  const byLabel = candidates.find(item => /申请单号|单据编号|单号|编号/.test(item.label))
  if (byLabel)
    return byLabel.code
  const byCode = candidates.find(item => /(?:^|_)(?:no|code)$|No$|Code$/.test(item.code))
  return byCode?.code || ''
}

function isDocumentNoCandidate(field = {}) {
  const code = fieldCode(field)
  const label = String(field.fieldName || field.label || '')
  if (!code)
    return false
  if (/申请单号|单据编号|单号|编号|编码/.test(label))
    return true
  if (/(?:^|_)(?:no|code)$|No$|Code$/.test(code))
    return true
  const type = String(field.dataType || field.fieldType || '').toLowerCase()
  const componentType = String(field.componentType || field.type || '').toLowerCase()
  return ['varchar', 'char', 'text', 'string'].includes(type) || ['input', 'textarea'].includes(componentType)
}

function hasFieldSelectableValues(field = {}) {
  return !!resolveFieldDictType(field) || resolveLocalOptions(field).length > 0
}

async function resolveStatusValueOptions(field = {}) {
  if (!field)
    return []
  const localOptions = normalizeStatusOptions(resolveLocalOptions(field))
  if (localOptions.length)
    return localOptions
  const dictType = resolveFieldDictType(field)
  if (!dictType)
    return []
  const dictOptions = await getDictData(dictType)
  return normalizeStatusOptions(dictOptions)
}

function resolveLocalOptions(field = {}) {
  const sources = [
    field.options,
    field.props?.options,
    field.basicProps?.options,
    field.advancedProps?.options,
  ]
  return sources.find(Array.isArray) || []
}

function resolveFieldDictType(field = {}) {
  return field.dictType || field.props?.dictType || field.basicProps?.dictType || field.advancedProps?.dictType || ''
}

function normalizeStatusOptions(options = []) {
  return options
    .map((option) => {
      const value = option.value ?? option.dictValue
      if (value === null || value === undefined || String(value).trim() === '')
        return null
      return {
        label: String(option.label ?? option.dictLabel ?? value),
        value: String(value),
        tagType: normalizeTagType(option.tagType || option.listClass || option.type),
        raw: option.raw || option,
      }
    })
    .filter(Boolean)
}

function normalizeTagType(value) {
  const text = String(value || '').toLowerCase()
  return ['default', 'info', 'success', 'warning', 'error'].includes(text) ? text : 'default'
}

function fieldCode(field = {}) {
  return field.fieldCode || field.field || ''
}

function isInactiveField(field = {}) {
  const status = String(field.fieldStatus || '').toUpperCase()
  return status === 'DISABLED' || status === 'HIDDEN'
}

function readBoolean(value, defaultValue = false) {
  if (value === null || value === undefined)
    return defaultValue
  if (typeof value === 'boolean')
    return value
  if (typeof value === 'number')
    return value !== 0
  return ['true', '1', 'yes'].includes(String(value).trim().toLowerCase())
}

function markDirty() {
  emit('dirtyChange', true)
}

defineExpose({
  saveConfig,
  loadConfig,
})
</script>

<style scoped>
.business-document-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 100%;
  background: #f4f6f9;
}

.document-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  padding: 14px 16px;
}

.document-head h3,
.document-section h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.document-head p,
.document-section p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.document-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 292px;
  min-height: 0;
}

.document-main {
  min-width: 0;
  overflow: visible;
  padding: 16px;
}

.document-rail {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.document-rail div {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 2px 8px;
  align-items: center;
  border: 1px solid #dde3ec;
  border-radius: 8px;
  background: #fff;
  padding: 10px;
}

.document-rail div.active {
  border-color: #9bbcf7;
  background: #f5f8ff;
}

.document-rail span {
  grid-row: span 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: #e8edf5;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.document-rail .active span {
  background: #2563eb;
  color: #fff;
}

.document-rail strong,
.document-rail em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-rail strong {
  color: #111827;
  font-size: 13px;
}

.document-rail em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.document-config-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 0.9fr);
  gap: 12px;
  margin-bottom: 12px;
}

.document-section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.document-section + .document-section {
  margin-top: 12px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.field-hints {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  margin-top: 10px;
}

.field-hints span {
  border-left: 3px solid #9bbcf7;
  background: #f8fbff;
  color: #475569;
  font-size: 12px;
  line-height: 1.55;
  padding: 4px 8px;
}

.display-switches {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.display-switches label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
  color: #111827;
  font-size: 13px;
  padding: 10px 12px;
}

.document-side {
  min-width: 0;
  overflow: auto;
  border-left: 1px solid #e5e7eb;
  background: #fff;
  padding: 14px;
}

@media (max-width: 1024px) {
  .document-body {
    grid-template-columns: 1fr;
  }

  .document-rail,
  .document-config-grid {
    grid-template-columns: 1fr;
  }

  .document-side {
    border-top: 1px solid #e5e7eb;
    border-left: 0;
  }
}

@media (max-width: 640px) {
  .document-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .display-switches {
    grid-template-columns: 1fr;
  }
}
</style>
