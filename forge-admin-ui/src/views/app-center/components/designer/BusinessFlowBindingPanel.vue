<template>
  <div class="business-flow-binding-panel">
    <div class="flow-head">
      <div>
        <h3>流程与自动化</h3>
        <p>绑定默认流程，配置标题模板和单据字段到流程变量的映射。</p>
      </div>
      <n-space size="small">
        <n-button size="small" secondary :loading="loading" @click="loadBinding">
          刷新
        </n-button>
        <n-button size="small" type="primary" :loading="saving" @click="saveConfig">
          保存流程
        </n-button>
      </n-space>
    </div>

    <div class="flow-body">
      <main class="flow-main">
        <n-spin :show="loading">
          <section class="flow-card">
            <div class="flow-card-head">
              <div>
                <h4>默认流程</h4>
                <p>手动按钮和触发器可以复用同一套流程绑定。</p>
              </div>
              <n-tag :type="form.flowModelKey ? 'success' : 'warning'" :bordered="false">
                {{ form.flowModelKey ? '已绑定' : '待绑定' }}
              </n-tag>
            </div>
            <n-form label-placement="top" size="small" :show-feedback="false">
              <n-grid :cols="2" :x-gap="14" :y-gap="4" responsive="screen">
                <n-form-item-gi label="流程模型">
                  <n-select
                    v-model:value="form.flowModelKey"
                    :options="flowModelOptions"
                    :loading="flowModelsLoading"
                    clearable
                    filterable
                    placeholder="选择已发布流程模型"
                    @update:value="handleFlowChange"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="谁来发起">
                  <n-select
                    v-model:value="form.startMode"
                    :options="startModeOptions"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi :span="2" label="流程标题模板">
                  <TemplateVariableEditor
                    v-model="form.titleTemplate"
                    :fields="fieldOptions"
                    :variables="variableOptions"
                    placeholder="例如：${opportunityName}-流程"
                    @update:model-value="markDirty"
                  />
                </n-form-item-gi>
              </n-grid>
            </n-form>
          </section>

          <section v-if="!codeApp" class="flow-card">
            <div class="flow-card-head">
              <div>
                <h4>业务记录绑定</h4>
                <p>维护流程实例和业务记录的关联字段，状态回写按这里的字段执行。</p>
              </div>
              <n-tag :type="businessBindingTagType" :bordered="false">
                {{ businessBindingModeLabel }}
              </n-tag>
            </div>
            <n-form label-placement="top" size="small" :show-feedback="false">
              <n-grid :cols="3" :x-gap="14" :y-gap="4" responsive="screen">
                <n-form-item-gi label="接入方式">
                  <n-select
                    v-model:value="form.businessBinding.mode"
                    :options="businessBindingModeOptions"
                    @update:value="handleBusinessBindingModeChange"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="业务表">
                  <n-input
                    v-model:value="form.businessBinding.tableName"
                    :disabled="businessBindingReadonly"
                    placeholder="发布后自动填充"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="主键字段">
                  <n-input
                    v-model:value="form.businessBinding.primaryKeyField"
                    :disabled="businessBindingReadonly"
                    placeholder="id"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="租户字段">
                  <n-input
                    v-model:value="form.businessBinding.tenantField"
                    :disabled="businessBindingReadonly"
                    placeholder="tenant_id"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="流程状态字段">
                  <n-select
                    v-model:value="form.businessBinding.statusField"
                    :options="fieldOptions"
                    :disabled="businessBindingAdapterMode"
                    clearable
                    filterable
                    tag
                    placeholder="选择或输入状态字段"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="标题字段">
                  <n-select
                    v-model:value="form.businessBinding.titleField"
                    :options="fieldOptions"
                    :disabled="businessBindingAdapterMode"
                    clearable
                    filterable
                    tag
                    placeholder="选择或输入标题字段"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
                <n-form-item-gi label="负责人字段">
                  <n-select
                    v-model:value="form.businessBinding.ownerField"
                    :options="fieldOptions"
                    :disabled="businessBindingAdapterMode"
                    clearable
                    filterable
                    tag
                    placeholder="选择或输入负责人字段"
                    @update:value="markDirty"
                  />
                </n-form-item-gi>
              </n-grid>
            </n-form>
            <n-alert
              v-if="businessBindingMessage"
              :type="businessBindingAdapterMode ? 'info' : 'warning'"
              :bordered="false"
              class="business-binding-alert"
            >
              {{ businessBindingMessage }}
            </n-alert>
          </section>

          <section v-else class="flow-card adapter-card">
            <div class="flow-card-head">
              <div>
                <h4>业务记录绑定</h4>
                <p>代码业务由业务模块读取、保存和回写状态，平台只保存流程实例与业务键的关联。</p>
              </div>
              <n-tag type="info" :bordered="false">
                代码适配器
              </n-tag>
            </div>
            <div class="adapter-facts">
              <div>
                <span>业务编码</span>
                <strong>{{ objectCode }}</strong>
              </div>
              <div>
                <span>主键字段</span>
                <strong>{{ form.businessBinding.primaryKeyField || 'id' }}</strong>
              </div>
              <div>
                <span>状态回写</span>
                <strong>业务代码处理</strong>
              </div>
            </div>
          </section>

          <section class="flow-card">
            <div class="flow-card-head">
              <div>
                <h4>变量映射</h4>
                <p>把业务单据字段写入流程变量，供流程条件线、审批人表达式和标题模板使用。</p>
              </div>
            </div>
            <FlowVariableMappingEditor
              v-model="form.variableMapping"
              :field-options="fieldOptions"
              :variable-options="variableOptions"
              :suggestions="mappingSuggestions"
              :warnings="variableWarnings"
              :loading="variablesLoading"
              @update:model-value="markDirty"
            />
          </section>

          <section class="flow-card flow-designer-entry-card">
            <div class="flow-card-head">
              <div>
                <h4>流程节点配置</h4>
                <p>审批办理、节点表单资产和字段权限在流程设计器节点抽屉中配置，运行时读取流程节点配置。</p>
              </div>
              <n-tag :type="userTasks.length ? 'info' : 'default'" :bordered="false">
                {{ userTasks.length ? `${userTasks.length} 个节点` : '未读取节点' }}
              </n-tag>
            </div>

            <n-spin :show="variablesLoading || formAssetsLoading">
              <div class="flow-designer-entry">
                <div class="flow-designer-entry-main">
                  <div class="flow-designer-entry-icon">
                    <i class="i-material-symbols:account-tree-outline" />
                  </div>
                  <div>
                    <strong>在流程设计器中配置节点</strong>
                    <span>打开流程画布，选中审批节点，在节点抽屉的“表单权限”页签配置字段可见、可编辑和必填。</span>
                  </div>
                </div>
                <n-button
                  type="primary"
                  secondary
                  :disabled="!selectedFlowModelId"
                  @click="openFlowDesigner"
                >
                  打开流程设计器
                </n-button>
              </div>

              <div class="flow-designer-facts">
                <div>
                  <span>当前流程</span>
                  <strong>{{ form.flowModelName || form.flowModelKey || '-' }}</strong>
                </div>
                <div>
                  <span>人工节点</span>
                  <strong>{{ userTasks.length }} 个</strong>
                </div>
                <div>
                  <span>业务字段目录</span>
                  <strong>{{ formAssets.length }} 个资产 · {{ fieldOptions.length }} 字段</strong>
                </div>
              </div>
            </n-spin>

            <n-alert v-if="formAssetWarnings.length" type="warning" :bordered="false" class="node-form-warning">
              {{ formAssetWarnings.join('；') }}
            </n-alert>
          </section>
        </n-spin>
      </main>

      <aside class="flow-side">
        <section>
          <h4>运行摘要</h4>
          <div class="flow-facts">
            <div>
              <span>流程模型</span>
              <strong>{{ form.flowModelName || form.flowModelKey || '-' }}</strong>
            </div>
            <div>
              <span>发起方式</span>
              <strong>{{ startModeLabel }}</strong>
            </div>
            <div>
              <span>变量映射</span>
              <strong>{{ validMappingCount }} 项</strong>
            </div>
            <div>
              <span>业务绑定</span>
              <strong>{{ businessBindingSummary }}</strong>
            </div>
            <div>
              <span>状态字段</span>
              <strong>{{ form.businessBinding.statusField || '-' }}</strong>
            </div>
            <div>
              <span>流程节点</span>
              <strong>{{ userTasks.length }} 个</strong>
            </div>
            <div>
              <span>变量候选</span>
              <strong>{{ variableOptions.length }} 项</strong>
            </div>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { businessFlowBinding, businessFlowFormAssets, businessFlowVariables, saveBusinessFlowBinding } from '@/api/business-app'
import flowApi from '@/api/flow'
import FlowVariableMappingEditor from './FlowVariableMappingEditor.vue'
import TemplateVariableEditor from './TemplateVariableEditor.vue'

const props = defineProps({
  objectCode: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  initialBinding: {
    type: Object,
    default: null,
  },
  codeApp: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['dirtyChange', 'saved', 'loaded'])

const message = useMessage()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const flowModelsLoading = ref(false)
const variablesLoading = ref(false)
const formAssetsLoading = ref(false)
const flowModelOptions = ref([])
const variableOptions = ref([])
const userTasks = ref([])
const formAssets = ref([])
const flowFieldCandidates = ref([])
const mappingSuggestions = ref([])
const variableWarnings = ref([])
const formAssetWarnings = ref([])
const form = reactive(createDefaultBinding())

const startModeOptions = [
  { label: '用户点击按钮', value: 'MANUAL' },
  { label: '触发器自动发起', value: 'TRIGGER' },
  { label: '按钮和触发器都可以', value: 'BOTH' },
]
const businessBindingModeOptions = computed(() => {
  if (props.codeApp) {
    return [
      { label: '代码适配器', value: 'ADAPTER' },
      { label: '简单业务表', value: 'BUSINESS_TABLE' },
    ]
  }
  return [
    { label: '低代码对象', value: 'LOWCODE_OBJECT' },
    { label: '简单业务表', value: 'BUSINESS_TABLE' },
    { label: '代码适配器', value: 'ADAPTER' },
  ]
})

const fieldOptions = computed(() => {
  const options = []
  const used = new Set()
  const append = (field) => {
    const code = fieldCode(field)
    if (!code || used.has(code) || isInactiveField(field)) {
      return
    }
    used.add(code)
    const label = field.fieldName || field.fieldLabel || field.label || code
    options.push({
      label: `${label}（${code}）`,
      value: code,
    })
  }
  props.fields.forEach(append)
  flowFieldCandidates.value.forEach(append)
  return options
})
const validMappingCount = computed(() => form.variableMapping.filter(item => item.formField && item.flowVariable).length)
const selectedFlowModel = computed(() => flowModelOptions.value.find(item => item.value === form.flowModelKey) || null)
const selectedFlowModelId = computed(() => selectedFlowModel.value?.id || '')
const startModeLabel = computed(() => startModeOptions.find(item => item.value === form.startMode)?.label || '-')
const normalizedBusinessBindingMode = computed(() => normalizeBusinessBindingMode(form.businessBinding.mode))
const businessBindingAdapterMode = computed(() => normalizedBusinessBindingMode.value === 'ADAPTER')
const businessBindingReadonly = computed(() => normalizedBusinessBindingMode.value === 'LOWCODE_OBJECT' || businessBindingAdapterMode.value)
const businessBindingModeLabel = computed(() => businessBindingModeOptions.value.find(item => item.value === normalizedBusinessBindingMode.value)?.label || (props.codeApp ? '代码适配器' : '低代码对象'))
const businessBindingTagType = computed(() => {
  if (businessBindingAdapterMode.value)
    return 'info'
  if (form.businessBinding.statusField)
    return 'success'
  return 'warning'
})
const businessBindingSummary = computed(() => {
  const tableName = form.businessBinding.tableName || '-'
  return `${businessBindingModeLabel.value} · ${tableName}`
})
const businessBindingMessage = computed(() => {
  if (businessBindingAdapterMode.value)
    return '代码适配器接管状态回写和节点表单权限，平台不会直接更新业务表字段。'
  if (normalizedBusinessBindingMode.value === 'BUSINESS_TABLE') {
    if (!form.businessBinding.tableName || !form.businessBinding.primaryKeyField || !form.businessBinding.statusField)
      return '简单业务表需要明确业务表、主键字段和流程状态字段。'
    return ''
  }
  if (!form.businessBinding.statusField)
    return '未选择流程状态字段时，流程仍可发起，但业务列表不会自动显示流程状态。'
  return ''
})
watch(() => props.objectCode, () => {
  loadBinding()
}, { immediate: true })

async function loadBinding() {
  if (!props.objectCode) {
    assignBinding(props.initialBinding || createDefaultBinding())
    return
  }
  loading.value = true
  try {
    const [bindingRes] = await Promise.all([
      businessFlowBinding(props.objectCode),
      loadFlowModels(),
      loadFormAssets(),
    ])
    assignBinding(bindingRes.data || props.initialBinding || createDefaultBinding())
    await loadVariableCandidates()
    emit('loaded', { ...form })
    emit('dirtyChange', false)
  }
  finally {
    loading.value = false
  }
}

async function loadFlowModels() {
  flowModelsLoading.value = true
  try {
    const res = await flowApi.getModelList({ status: 1 })
    flowModelOptions.value = (res.data || []).map(model => ({
      label: `${model.modelName || model.name || model.modelKey || model.key}（${model.modelKey || model.key}）`,
      value: model.modelKey || model.key,
      id: model.id,
      modelName: model.modelName || model.name || model.modelKey || model.key,
    })).filter(item => item.value)
  }
  catch {
    flowModelOptions.value = []
  }
  finally {
    flowModelsLoading.value = false
  }
}

async function loadFormAssets() {
  formAssetsLoading.value = true
  formAssetWarnings.value = []
  try {
    const res = await businessFlowFormAssets(props.objectCode)
    const data = res.data || {}
    formAssets.value = Array.isArray(data.formAssets) ? data.formAssets : []
    formAssetWarnings.value = data.warnings || []
  }
  catch (e) {
    formAssets.value = []
    formAssetWarnings.value = [e.message || '业务表单资产加载失败']
  }
  finally {
    formAssetsLoading.value = false
  }
}

async function saveConfig() {
  if (!props.objectCode)
    return
  if (!validateBeforeSave())
    return
  saving.value = true
  try {
    await saveBusinessFlowBinding(props.objectCode, buildPayload())
    message.success('流程绑定已保存')
    emit('dirtyChange', false)
    emit('saved', { ...form })
    await loadBinding()
  }
  finally {
    saving.value = false
  }
}

function validateBeforeSave() {
  if (!form.flowModelKey) {
    message.warning('请选择流程模型')
    return false
  }
  return true
}

function buildPayload() {
  return {
    flowModelKey: form.flowModelKey || '',
    flowModelName: form.flowModelName || selectedFlowName(),
    titleTemplate: form.titleTemplate || '',
    startMode: form.startMode || 'MANUAL',
    variableMapping: form.variableMapping
      .map(item => ({
        formField: item.formField || '',
        flowVariable: item.flowVariable || '',
        label: item.label || fieldLabel(item.formField),
      }))
      .filter(item => item.formField && item.flowVariable),
    businessBinding: normalizeBusinessBinding(form.businessBinding),
    nodeForms: normalizeNodeFormsForPayload(form.nodeForms),
    conditionFlows: form.conditionFlows || [],
    options: { ...(form.options || {}) },
  }
}

function assignBinding(value = {}) {
  Object.assign(form, {
    ...createDefaultBinding(),
    ...value,
    startMode: normalizeStartMode(value.startMode),
    variableMapping: normalizeMappings(value.variableMapping || []),
    businessBinding: normalizeBusinessBinding(value.businessBinding),
    nodeForms: normalizeNodeForms(value.nodeForms || []),
    conditionFlows: Array.isArray(value.conditionFlows) ? value.conditionFlows : [],
    options: { ...(value.options || {}) },
  })
}

async function applyBindingConfig(value = {}, assetCatalog = null) {
  if (assetCatalog) {
    formAssets.value = Array.isArray(assetCatalog.formAssets) ? assetCatalog.formAssets : []
    formAssetWarnings.value = assetCatalog.warnings || []
  }
  else {
    await loadFormAssets()
  }
  assignBinding(value || createDefaultBinding())
  await Promise.all([
    loadFlowModels(),
    loadVariableCandidates(),
  ])
  emit('loaded', { ...form })
  emit('dirtyChange', false)
}

function normalizeMappings(list = []) {
  return list.map(item => ({
    clientKey: `mapping_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    formField: item.formField || item.field || null,
    flowVariable: item.flowVariable || item.variable || '',
    label: item.label || fieldLabel(item.formField || item.field),
  }))
}

function createDefaultBinding() {
  return {
    flowModelKey: '',
    flowModelName: '',
    titleTemplate: '',
    startMode: 'MANUAL',
    variableMapping: [],
    businessBinding: createDefaultBusinessBinding(),
    nodeForms: [],
    conditionFlows: [],
    options: {},
  }
}

function handleFlowChange(value) {
  form.flowModelName = flowModelOptions.value.find(item => item.value === value)?.modelName || ''
  form.variableMapping = []
  form.nodeForms = []
  loadVariableCandidates()
  markDirty()
}

function handleBusinessBindingModeChange(value) {
  form.businessBinding.mode = normalizeBusinessBindingMode(value)
  if (!form.businessBinding.primaryKeyField)
    form.businessBinding.primaryKeyField = 'id'
  if (!form.businessBinding.tenantField)
    form.businessBinding.tenantField = 'tenant_id'
  if (form.businessBinding.mode === 'LOWCODE_OBJECT')
    fillLowcodeBusinessBindingDefaults()
  markDirty()
}

async function loadVariableCandidates() {
  variableOptions.value = []
  flowFieldCandidates.value = []
  mappingSuggestions.value = []
  variableWarnings.value = []
  if (!form.flowModelKey)
    return
  variablesLoading.value = true
  try {
    const res = await businessFlowVariables(form.flowModelKey, { objectCode: props.objectCode })
    const data = res.data || {}
    variableOptions.value = (data.flowVariables || []).map(item => ({
      label: `${item.displayName || item.variableName}（${item.variableName}）`,
      value: item.variableName,
      source: item.source,
    })).filter(item => item.value)
    flowFieldCandidates.value = normalizeFlowFieldCandidates(data.fieldCandidates || [])
    userTasks.value = normalizeUserTasks(data.userTasks || [])
    mappingSuggestions.value = data.mappingSuggestions || []
    variableWarnings.value = data.warnings || []
  }
  catch (e) {
    flowFieldCandidates.value = []
    userTasks.value = []
    variableWarnings.value = [e.message || '流程变量候选项加载失败']
  }
  finally {
    variablesLoading.value = false
  }
}

function selectedFlowName() {
  return flowModelOptions.value.find(item => item.value === form.flowModelKey)?.modelName || form.flowModelName || ''
}

function normalizeUserTasks(list = []) {
  return (Array.isArray(list) ? list : [])
    .map(item => ({
      taskDefKey: normalizeText(item.taskDefKey || item.id),
      taskName: normalizeText(item.taskName || item.name),
    }))
    .filter(item => item.taskDefKey)
}

function normalizeFlowFieldCandidates(list = []) {
  return (Array.isArray(list) ? list : [])
    .map(item => ({
      fieldCode: normalizeText(item.fieldCode || item.field || item.code),
      fieldLabel: normalizeText(item.fieldLabel || item.label || item.fieldName || item.title),
      dataType: normalizeText(item.dataType || item.fieldType || item.type),
      fieldStatus: normalizeText(item.fieldStatus),
    }))
    .filter(item => item.fieldCode)
}

function normalizeNodeForms(list = []) {
  return (Array.isArray(list) ? list : [])
    .map(normalizeNodeForm)
    .filter(item => item.taskDefKey)
}

function normalizeNodeForm(value = {}) {
  const fieldPermissions = Array.isArray(value.fieldPermissions) ? value.fieldPermissions : []
  return {
    taskDefKey: normalizeText(value.taskDefKey),
    taskName: normalizeText(value.taskName),
    formMode: normalizeText(value.formMode),
    formKey: normalizeText(value.formKey),
    formName: normalizeText(value.formName),
    providerKey: normalizeText(value.providerKey),
    formUrl: normalizeText(value.formUrl),
    viewKey: normalizeText(value.viewKey) || 'default',
    editMode: normalizeText(value.editMode) || 'READONLY',
    visibleFields: normalizeFieldSelection(value.visibleFields || fieldsByPermission(fieldPermissions, 'readable')),
    writableFields: normalizeFieldSelection(value.writableFields || fieldsByPermission(fieldPermissions, 'writable')),
    requiredFields: normalizeFieldSelection(value.requiredFields || fieldsByPermission(fieldPermissions, 'required')),
  }
}

function normalizeNodeFormsForPayload(list = []) {
  return normalizeNodeForms(list)
    .filter(item => item.taskDefKey)
    .map((item) => {
      return {
        taskDefKey: item.taskDefKey,
        taskName: item.taskName,
        formMode: item.formMode || '',
        formKey: item.formKey || '',
        formName: item.formName || '',
        providerKey: item.providerKey || '',
        formUrl: item.formUrl || '',
        viewKey: item.viewKey || 'default',
        editMode: item.editMode || 'READONLY',
        fieldPermissions: buildFieldPermissionsFromSelections(item),
      }
    })
}

function fieldsByPermission(fieldPermissions = [], permission) {
  if (permission === 'readable') {
    const hiddenMode = fieldPermissions.some(item => item.readable === false)
    return hiddenMode
      ? fieldPermissions.filter(item => item.readable !== false).map(item => item.field)
      : []
  }
  return fieldPermissions.filter(item => item[permission] === true).map(item => item.field)
}

function normalizeFieldSelection(value = []) {
  return Array.from(new Set((Array.isArray(value) ? value : [])
    .map(item => normalizeText(item))
    .filter(Boolean)))
}

function buildFieldPermissionsFromSelections(nodeForm) {
  const visible = new Set(nodeForm.visibleFields || [])
  const writable = new Set(nodeForm.writableFields || [])
  const required = new Set(nodeForm.requiredFields || [])
  const fields = new Set([...visible, ...writable, ...required])
  return Array.from(fields).map(field => ({
    field,
    readable: visible.size === 0 || visible.has(field),
    writable: writable.has(field),
    required: required.has(field),
  }))
}

function createDefaultBusinessBinding() {
  return {
    mode: props.codeApp ? 'ADAPTER' : 'LOWCODE_OBJECT',
    tableName: '',
    primaryKeyField: 'id',
    tenantField: 'tenant_id',
    statusField: '',
    titleField: '',
    ownerField: '',
  }
}

function normalizeBusinessBinding(value = {}) {
  const defaults = createDefaultBusinessBinding()
  const next = {
    ...defaults,
    ...(value || {}),
    mode: normalizeBusinessBindingMode(value?.mode),
    tableName: normalizeText(value?.tableName),
    primaryKeyField: normalizeText(value?.primaryKeyField) || defaults.primaryKeyField,
    tenantField: normalizeText(value?.tenantField) || defaults.tenantField,
    statusField: normalizeText(value?.statusField),
    titleField: normalizeText(value?.titleField),
    ownerField: normalizeText(value?.ownerField),
  }
  if (next.mode === 'LOWCODE_OBJECT') {
    next.statusField = next.statusField || inferStatusField()
    next.titleField = next.titleField || inferTitleField()
    next.ownerField = next.ownerField || inferOwnerField()
  }
  return next
}

function fillLowcodeBusinessBindingDefaults() {
  form.businessBinding.statusField = form.businessBinding.statusField || inferStatusField()
  form.businessBinding.titleField = form.businessBinding.titleField || inferTitleField()
  form.businessBinding.ownerField = form.businessBinding.ownerField || inferOwnerField()
}

function normalizeBusinessBindingMode(value) {
  const fallback = props.codeApp ? 'ADAPTER' : 'LOWCODE_OBJECT'
  const normalized = String(value || fallback).trim().toUpperCase()
  return businessBindingModeOptions.value.some(item => item.value === normalized) ? normalized : fallback
}

function normalizeText(value) {
  return String(value || '').trim()
}

function inferStatusField() {
  return inferFieldCode([
    'documentStatus',
    'document_status',
    'approvalStatus',
    'approval_status',
    'flowStatus',
    'flow_status',
    'status',
    'state',
  ], /流程状态|审批状态|单据状态|状态/)
}

function inferTitleField() {
  return inferFieldCode([
    'title',
    'name',
    'subject',
    'documentName',
    'document_name',
    'contractName',
    'contract_name',
  ], /标题|名称|主题|合同名称|单据名称/)
}

function inferOwnerField() {
  return inferFieldCode([
    'ownerId',
    'owner_id',
    'principalId',
    'principal_id',
    'leaderId',
    'leader_id',
    'managerId',
    'manager_id',
  ], /负责人|责任人|经理|主管/)
}

function inferFieldCode(exactCodes = [], labelPattern = null) {
  const fields = props.fields
    .filter(field => fieldCode(field) && !isInactiveField(field))
    .map(field => ({
      code: fieldCode(field),
      label: String(field.fieldName || field.label || ''),
    }))
  const exact = fields.find(field => exactCodes.includes(field.code))
  if (exact)
    return exact.code
  const byLabel = labelPattern ? fields.find(field => labelPattern.test(field.label)) : null
  return byLabel?.code || ''
}

function fieldLabel(code) {
  if (!code)
    return ''
  const option = fieldOptions.value.find(item => item.value === code)
  return option?.label || code
}

function fieldCode(field = {}) {
  return field.fieldCode || field.field || field.code || ''
}

function openFlowDesigner() {
  if (!selectedFlowModelId.value) {
    message.warning('请选择流程模型后再打开流程设计器')
    return
  }
  router.push({
    path: '/flow/design',
    query: {
      id: selectedFlowModelId.value,
      businessObjectCode: props.objectCode,
      codeApp: props.codeApp ? '1' : undefined,
      source: 'appCenter',
    },
  })
}

function normalizeStartMode(value) {
  const normalized = String(value || 'MANUAL').toUpperCase()
  if (['MANUAL_AND_TRIGGER', 'MANUAL_TRIGGER'].includes(normalized))
    return 'BOTH'
  if (['AUTO', 'AUTOMATIC'].includes(normalized))
    return 'TRIGGER'
  return startModeOptions.some(item => item.value === normalized) ? normalized : 'MANUAL'
}

function isInactiveField(field = {}) {
  const status = String(field.fieldStatus || '').toUpperCase()
  return status === 'DISABLED' || status === 'HIDDEN'
}

function markDirty() {
  emit('dirtyChange', true)
}

defineExpose({
  saveConfig,
  loadBinding,
  applyBindingConfig,
  assignBinding,
  buildPayload,
  validateBeforeSave,
})
</script>

<style scoped>
.business-flow-binding-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 100%;
}

.flow-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.flow-head h3,
.flow-card h4,
.flow-side h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.flow-head p,
.flow-card p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.flow-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  min-height: 0;
}

.flow-main {
  min-width: 0;
  overflow: visible;
  background: #f8fafc;
  padding: 14px;
}

.flow-card,
.flow-side section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.flow-card + .flow-card {
  margin-top: 12px;
}

.flow-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.mapping-list {
  display: grid;
  gap: 8px;
}

.variable-warning {
  margin-top: 10px;
}

.business-binding-alert {
  margin-top: 10px;
}

.adapter-card {
  background: #f8fbff;
}

.adapter-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.adapter-facts div {
  min-width: 0;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #fff;
  padding: 10px;
}

.adapter-facts span {
  display: block;
  color: #64748b;
  font-size: 12px;
  line-height: 16px;
}

.adapter-facts strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #111827;
  font-size: 13px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-form-warning {
  margin-top: 10px;
}

.flow-designer-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
  padding: 12px;
}

.flow-designer-entry-main {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.flow-designer-entry-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  border-radius: 8px;
  background: #e0ecff;
  color: #2563eb;
  font-size: 18px;
}

.flow-designer-entry-main strong {
  display: block;
  color: #111827;
  font-size: 14px;
  line-height: 20px;
}

.flow-designer-entry-main span {
  display: block;
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.flow-designer-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.flow-designer-facts div {
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 10px;
}

.flow-designer-facts span {
  display: block;
  color: #64748b;
  font-size: 12px;
  line-height: 16px;
}

.flow-designer-facts strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #111827;
  font-size: 13px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mapping-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 24px minmax(180px, 1fr) 32px;
  gap: 8px;
  align-items: center;
}

.mapping-row span,
.flow-facts span {
  color: #64748b;
  font-size: 12px;
  text-align: center;
}

.flow-side {
  border-left: 1px solid #e5e7eb;
  background: #fbfcfe;
  padding: 12px;
}

.flow-facts {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.flow-facts div {
  min-width: 0;
  border-radius: 6px;
  background: #f1f5f9;
  padding: 10px;
}

.flow-facts strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #111827;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .flow-body {
    grid-template-columns: 1fr;
  }

  .flow-side {
    border-left: 0;
    border-top: 1px solid #e5e7eb;
  }

  .mapping-row {
    grid-template-columns: 1fr;
  }

  .adapter-facts {
    grid-template-columns: 1fr;
  }

  .flow-designer-entry {
    align-items: stretch;
    flex-direction: column;
  }

  .flow-designer-facts {
    grid-template-columns: 1fr;
  }
}
</style>
