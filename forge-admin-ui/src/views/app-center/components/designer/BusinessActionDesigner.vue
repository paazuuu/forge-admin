<template>
  <div class="business-action-designer">
    <div class="action-designer-head">
      <div>
        <h3>自定义操作</h3>
        <p>维护工具栏、行操作和详情操作，普通模式只填写业务动作。</p>
      </div>
      <n-space size="small">
        <n-button size="small" secondary @click="loadActions">
          刷新
        </n-button>
        <n-button size="small" secondary @click="addFlowAction">
          添加发起主流程
        </n-button>
        <n-button size="small" secondary @click="addAction('ROW')">
          新增操作
        </n-button>
        <n-button size="small" type="primary" :loading="saving" @click="saveActions">
          保存操作
        </n-button>
      </n-space>
    </div>

    <div class="action-designer-body">
      <main class="action-list-pane">
        <n-spin :show="loading">
          <div v-if="localActions.length" class="action-card-list">
            <section v-for="(action, index) in localActions" :key="action.clientKey" class="action-card">
              <header class="action-card-head">
                <div>
                  <strong>{{ action.actionName || '未命名操作' }}</strong>
                  <p>{{ actionPositionLabel(action.actionPosition) }} · {{ actionTypeLabel(action.actionType) }}</p>
                </div>
                <n-space size="small">
                  <n-tag size="small" :type="action.status === 0 ? 'default' : 'success'" :bordered="false">
                    {{ action.status === 0 ? '停用' : '启用' }}
                  </n-tag>
                  <n-popconfirm @positive-click="removeAction(index)">
                    <template #trigger>
                      <n-button quaternary circle size="small">
                        <template #icon>
                          <n-icon><TrashOutline /></n-icon>
                        </template>
                      </n-button>
                    </template>
                    确认删除该操作？
                  </n-popconfirm>
                </n-space>
              </header>

              <n-form label-placement="top" :show-feedback="false" size="small" class="action-form">
                <n-grid :cols="3" :x-gap="12" :y-gap="4" responsive="screen">
                  <n-form-item-gi label="操作名称">
                    <n-input v-model:value="action.actionName" placeholder="例如：发起主流程" @update:value="value => updateActionName(action, value)" />
                  </n-form-item-gi>
                  <n-form-item-gi label="操作位置">
                    <n-select v-model:value="action.actionPosition" :options="positionOptions" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi label="操作类型">
                    <n-select v-model:value="action.actionType" :options="actionTypeOptions" @update:value="value => updateActionType(action, value)" />
                  </n-form-item-gi>
                  <n-form-item-gi label="权限标识">
                    <n-input v-model:value="action.permission" placeholder="例如：ai:businessObject:publish" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi label="二次确认">
                    <n-switch v-model:value="action.confirmRequired" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi label="启用状态">
                    <n-switch :value="action.status !== 0" @update:value="value => updateStatus(action, value)" />
                  </n-form-item-gi>
                  <n-form-item-gi label="成功提示">
                    <n-input v-model:value="action.successMessage" placeholder="操作成功" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi label="失败提示">
                    <n-input v-model:value="action.failureMessage" placeholder="操作失败" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi label="排序">
                    <n-input-number v-model:value="action.sortOrder" :min="0" style="width: 100%" @update:value="markDirty" />
                  </n-form-item-gi>

                  <template v-if="action.actionType === 'OPEN_PAGE'">
                    <n-form-item-gi :span="formOptions.length ? 2 : 3" label="目标页面">
                      <n-input v-model:value="action.actionConfig.targetPath" placeholder="例如：/ai/crud-page/customer?pageKey=detail&id=:id" @update:value="markDirty" />
                    </n-form-item-gi>
                    <n-form-item-gi v-if="formOptions.length" label="目标表单">
                      <n-select
                        v-model:value="action.actionConfig.targetFormKey"
                        :options="formOptions"
                        clearable
                        filterable
                        placeholder="默认表单"
                        @update:value="markDirty"
                      />
                    </n-form-item-gi>
                  </template>
                  <template v-else-if="action.actionType === 'START_FLOW'">
                    <n-form-item-gi :span="3" label="主流程">
                      <div class="main-flow-action-hint">
                        <strong>使用“流程与自动化”中配置的主流程</strong>
                        <span>这里只维护按钮名称、位置、权限和确认文案。</span>
                      </div>
                    </n-form-item-gi>
                  </template>
                  <n-form-item-gi v-else-if="action.actionType === 'OPEN_EXTERNAL'" :span="3" label="外部链接">
                    <n-input v-model:value="action.actionConfig.url" placeholder="https://example.com" @update:value="markDirty" />
                  </n-form-item-gi>
                  <template v-else-if="action.actionType === 'CALL_API'">
                    <n-form-item-gi :span="3" label="API 调用">
                      <div class="api-action-panel">
                        <n-grid :cols="3" :x-gap="12" :y-gap="4" responsive="screen">
                          <n-form-item-gi label="已登记 API">
                            <n-select
                              v-model:value="action.actionConfig.apiConfigId"
                              :options="apiConfigOptions"
                              :loading="apiConfigLoading"
                              clearable
                              filterable
                              placeholder="可手工填写"
                              @update:value="value => applyApiConfig(action, value)"
                            />
                          </n-form-item-gi>
                          <n-form-item-gi label="请求方式">
                            <n-select v-model:value="action.actionConfig.method" :options="apiMethodOptions" @update:value="markDirty" />
                          </n-form-item-gi>
                          <n-form-item-gi label="成功后">
                            <n-select
                              v-model:value="action.actionConfig.successBehavior"
                              :options="successBehaviorOptions"
                              clearable
                              placeholder="仅提示"
                              @update:value="markDirty"
                            />
                          </n-form-item-gi>
                          <n-form-item-gi :span="2" label="接口地址">
                            <n-input v-model:value="action.actionConfig.url" placeholder="/business/customer/audit/:id" @update:value="markDirty" />
                          </n-form-item-gi>
                          <n-form-item-gi label="能力标识">
                            <n-input v-model:value="action.actionConfig.capabilityCode" placeholder="可选，例如：customer_audit" @update:value="markDirty" />
                          </n-form-item-gi>
                        </n-grid>

                        <div class="api-param-head">
                          <span>参数映射</span>
                          <n-button size="small" secondary @click="addApiParam(action)">
                            <template #icon>
                              <n-icon><AddOutline /></n-icon>
                            </template>
                            添加参数
                          </n-button>
                        </div>
                        <div v-if="action.actionConfig.params?.length" class="api-param-list">
                          <div v-for="(param, paramIndex) in action.actionConfig.params" :key="param.clientKey" class="api-param-row">
                            <n-input v-model:value="param.name" placeholder="参数名" @update:value="markDirty" />
                            <n-select v-model:value="param.target" :options="apiParamTargetOptions" @update:value="markDirty" />
                            <n-select v-model:value="param.sourceType" :options="apiParamSourceOptions" @update:value="markDirty" />
                            <n-select
                              v-if="param.sourceType === 'rowField'"
                              v-model:value="param.sourceField"
                              :options="fieldOptions"
                              clearable
                              filterable
                              placeholder="选择字段"
                              @update:value="markDirty"
                            />
                            <n-select
                              v-else-if="param.sourceType === 'system'"
                              v-model:value="param.sourceField"
                              :options="systemParamOptions"
                              clearable
                              placeholder="系统变量"
                              @update:value="markDirty"
                            />
                            <n-input
                              v-else-if="param.sourceType === 'routeQuery'"
                              v-model:value="param.sourceField"
                              placeholder="路由参数名"
                              @update:value="markDirty"
                            />
                            <n-input
                              v-else
                              v-model:value="param.value"
                              placeholder="固定值，支持 :id / ${field}"
                              @update:value="markDirty"
                            />
                            <n-button quaternary circle size="small" @click="removeApiParam(action, paramIndex)">
                              <template #icon>
                                <n-icon><TrashOutline /></n-icon>
                              </template>
                            </n-button>
                          </div>
                        </div>
                        <n-empty v-else size="small" description="暂无参数映射" />
                      </div>
                    </n-form-item-gi>
                  </template>
                  <n-form-item-gi v-else-if="action.actionType === 'TRIGGER'" :span="3" label="触发器标识">
                    <n-input v-model:value="action.actionConfig.triggerCode" placeholder="例如：customer_notify" @update:value="markDirty" />
                  </n-form-item-gi>
                  <n-form-item-gi v-else :span="3" label="能力标识">
                    <n-input v-model:value="action.actionConfig.capabilityCode" placeholder="填写已封装的业务能力标识" @update:value="markDirty" />
                  </n-form-item-gi>
                </n-grid>
              </n-form>
            </section>
          </div>
          <n-empty v-else-if="!loading" description="暂无自定义操作" />
        </n-spin>
      </main>

      <aside class="action-summary-pane">
        <section>
          <h4>操作分布</h4>
          <div class="action-stat-grid">
            <div v-for="item in actionStats" :key="item.position">
              <span>{{ item.label }}</span>
              <strong>{{ item.count }}</strong>
            </div>
          </div>
        </section>
        <section>
          <h4>发布关注</h4>
          <p>发起主流程按钮会复用当前对象的主流程配置；流程模型、标题和变量映射在流程与自动化里维护。</p>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { AddOutline, TrashOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import {
  businessObjectActions,
  enabledApiConfigs,
  saveBusinessObjectActions,
} from '@/api/business-app'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  formOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['updated', 'dirtyChange'])

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const localActions = ref([])
const apiConfigs = ref([])
const apiConfigLoading = ref(false)
const apiConfigLoaded = ref(false)

const positionOptions = [
  { label: '工具栏', value: 'TOOLBAR' },
  { label: '行操作', value: 'ROW' },
  { label: '详情页', value: 'DETAIL' },
]

const actionTypeOptions = [
  { label: '打开页面', value: 'OPEN_PAGE' },
  { label: '调用 API', value: 'CALL_API' },
  { label: '发起主流程', value: 'START_FLOW' },
  { label: '执行触发器', value: 'TRIGGER' },
  { label: '打开外部链接', value: 'OPEN_EXTERNAL' },
]

const apiMethodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'POST 加密', value: 'POST_ENCRYPT' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
  { label: 'PATCH', value: 'PATCH' },
]

const apiParamTargetOptions = [
  { label: 'Path', value: 'path' },
  { label: 'Query', value: 'query' },
  { label: 'Body', value: 'body' },
  { label: 'Header', value: 'header' },
]

const apiParamSourceOptions = [
  { label: '当前行字段', value: 'rowField' },
  { label: '路由参数', value: 'routeQuery' },
  { label: '固定值', value: 'static' },
  { label: '系统变量', value: 'system' },
]

const systemParamOptions = [
  { label: '当前时间', value: 'now' },
  { label: '当前日期', value: 'today' },
  { label: '租户ID', value: 'tenantId' },
  { label: '勾选行ID', value: 'selectedIds' },
]

const successBehaviorOptions = [
  { label: '刷新列表', value: 'refreshList' },
  { label: '返回上一页', value: 'goBack' },
]

const actionStats = computed(() => positionOptions.map(item => ({
  position: item.value,
  label: item.label,
  count: localActions.value.filter(action => action.actionPosition === item.value && action.status !== 0).length,
})))

const apiConfigOptions = computed(() => apiConfigs.value.map(item => ({
  label: `${item.apiName || item.apiCode || item.urlPath} · ${item.reqMethod || 'GET'} ${item.urlPath || ''}`,
  value: String(item.id || item.apiCode || item.urlPath),
})))

const fieldOptions = computed(() => (Array.isArray(props.fields) ? props.fields : [])
  .map((field) => {
    const value = field.field || field.fieldCode || field.fieldName
    if (!value)
      return null
    return {
      label: `${field.label || field.fieldName || value}（${value}）`,
      value,
    }
  })
  .filter(Boolean))

onMounted(() => {
  loadEnabledApiConfigs()
})

watch(() => props.objectId, () => {
  loadActions()
}, { immediate: true })

async function loadActions() {
  if (!props.objectId) {
    localActions.value = []
    return
  }
  loading.value = true
  try {
    const res = await businessObjectActions(props.objectId)
    localActions.value = (res.data || []).map(normalizeAction)
    emit('updated', localActions.value)
  }
  finally {
    loading.value = false
  }
}

async function loadEnabledApiConfigs() {
  if (apiConfigLoaded.value || apiConfigLoading.value)
    return
  apiConfigLoading.value = true
  try {
    const res = await enabledApiConfigs()
    apiConfigs.value = Array.isArray(res.data) ? res.data : []
    apiConfigLoaded.value = true
  }
  catch (error) {
    apiConfigs.value = []
    apiConfigLoaded.value = true
    console.warn('[BusinessActionDesigner] API配置不可用，已切换为手工输入模式', error?.message || error)
  }
  finally {
    apiConfigLoading.value = false
  }
}

function addAction(position = 'ROW') {
  localActions.value.push(normalizeAction({
    actionCode: `custom_${Date.now()}`,
    actionName: '自定义操作',
    actionPosition: position,
    actionType: 'OPEN_PAGE',
    confirmRequired: false,
    status: 1,
    sortOrder: localActions.value.length * 10 + 10,
    actionConfig: {},
  }))
  emit('dirtyChange', true)
}

function addFlowAction() {
  localActions.value.push(normalizeAction({
    actionCode: `start_flow_${Date.now()}`,
    actionName: '发起主流程',
    actionPosition: 'ROW',
    actionType: 'START_FLOW',
    confirmRequired: true,
    successMessage: '流程已发起',
    failureMessage: '流程发起失败',
    status: 1,
    sortOrder: localActions.value.length * 10 + 10,
    actionConfig: { useMainFlow: true },
  }))
  emit('dirtyChange', true)
}

function removeAction(index) {
  localActions.value.splice(index, 1)
  emit('dirtyChange', true)
}

function updateActionName(action, value) {
  action.actionName = value
  if (!action.actionCode || /^custom_\d+/.test(action.actionCode))
    action.actionCode = normalizeActionCode(value) || action.actionCode
  markDirty()
}

function updateStatus(action, value) {
  action.status = value ? 1 : 0
  markDirty()
}

async function saveActions() {
  if (!props.objectId)
    return
  const invalidApiAction = localActions.value.find(action => action.status !== 0 && isInvalidApiAction(action))
  if (invalidApiAction) {
    message.warning(`请为“${invalidApiAction.actionName || '自定义操作'}”配置接口地址或能力标识`)
    return
  }
  saving.value = true
  try {
    await saveBusinessObjectActions(props.objectId, localActions.value.map(toActionPayload))
    message.success('自定义操作已保存')
    emit('dirtyChange', false)
    await loadActions()
  }
  finally {
    saving.value = false
  }
}

function normalizeAction(action = {}) {
  return {
    ...action,
    clientKey: action.actionCode || `action_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    actionCode: action.actionCode || normalizeActionCode(action.actionName) || `custom_${Date.now()}`,
    actionName: action.actionName || '自定义操作',
    actionPosition: normalizePosition(action.actionPosition),
    actionType: normalizeActionType(action.actionType),
    confirmRequired: Boolean(action.confirmRequired),
    successMessage: action.successMessage || '',
    failureMessage: action.failureMessage || '',
    status: action.status ?? 1,
    sortOrder: action.sortOrder ?? 0,
    actionConfig: normalizeActionConfig(action.actionType, action.actionConfig),
  }
}

function toActionPayload(action = {}) {
  return {
    actionCode: normalizeActionCode(action.actionCode) || normalizeActionCode(action.actionName),
    actionName: action.actionName,
    actionPosition: normalizePosition(action.actionPosition),
    actionType: normalizeActionType(action.actionType),
    permission: action.permission,
    confirmRequired: Boolean(action.confirmRequired),
    successMessage: action.successMessage,
    failureMessage: action.failureMessage,
    status: action.status ?? 1,
    sortOrder: action.sortOrder ?? 0,
    actionConfig: normalizeActionConfigForPayload(action.actionType, action.actionConfig),
  }
}

function normalizePosition(value) {
  const normalized = String(value || 'ROW').replace('-', '_').toUpperCase()
  return positionOptions.some(item => item.value === normalized) ? normalized : 'ROW'
}

function normalizeActionType(value) {
  const normalized = String(value || 'OPEN_PAGE')
    .replace(/([a-z])([A-Z])/g, '$1_$2')
    .replace('-', '_')
    .toUpperCase()
  if (normalized === 'START_APPROVAL')
    return 'START_FLOW'
  return actionTypeOptions.some(item => item.value === normalized) ? normalized : 'OPEN_PAGE'
}

function normalizeActionConfig(actionType, config = {}) {
  const source = typeof config === 'string' ? safeParseJson(config) : { ...(config || {}) }
  const normalizedType = normalizeActionType(actionType)
  if (normalizedType === 'CALL_API')
    return normalizeApiActionConfig(source)
  if (normalizedType !== 'START_FLOW')
    return source
  return {
    useMainFlow: true,
  }
}

function normalizeActionConfigForPayload(actionType, config = {}) {
  const source = normalizeActionConfig(actionType, config)
  const normalizedType = normalizeActionType(actionType)
  if (normalizedType === 'CALL_API')
    return normalizeApiActionConfigForPayload(source)
  if (normalizedType !== 'START_FLOW')
    return source
  return {
    useMainFlow: true,
  }
}

function updateActionType(action, value) {
  action.actionType = normalizeActionType(value)
  action.actionConfig = normalizeActionConfig(action.actionType, action.actionConfig)
  if (action.actionType === 'CALL_API')
    loadEnabledApiConfigs()
  markDirty()
}

function normalizeApiActionConfig(config = {}) {
  const params = Array.isArray(config.params)
    ? config.params
    : Array.isArray(config.paramMappings)
      ? config.paramMappings
      : []
  return {
    ...config,
    apiConfigId: config.apiConfigId === undefined || config.apiConfigId === null ? null : String(config.apiConfigId),
    apiCode: String(config.apiCode || '').trim(),
    apiName: String(config.apiName || '').trim(),
    method: normalizeApiMethod(config.method || config.reqMethod || config.apiMethod || 'POST'),
    url: String(config.url || config.apiUrl || config.urlPath || config.path || '').trim(),
    capabilityCode: String(config.capabilityCode || '').trim(),
    successBehavior: String(config.successBehavior || '').trim() || null,
    params: params.map(normalizeApiParam).filter(Boolean),
  }
}

function normalizeApiActionConfigForPayload(config = {}) {
  const normalized = normalizeApiActionConfig(config)
  return {
    ...normalized,
    params: normalized.params
      .filter(param => param.name && hasApiParamSource(param))
      .map(({ clientKey, ...param }) => param),
  }
}

function normalizeApiMethod(value) {
  const method = String(value || 'POST')
    .replace('-', '_')
    .toUpperCase()
  return apiMethodOptions.some(item => item.value === method) ? method : 'POST'
}

function normalizeApiParam(param = {}) {
  const sourceType = ['rowField', 'routeQuery', 'static', 'system'].includes(param.sourceType) ? param.sourceType : 'rowField'
  const target = ['path', 'query', 'body', 'header'].includes(param.target) ? param.target : ''
  return {
    clientKey: param.clientKey || `param_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    name: String(param.name || '').trim(),
    target,
    sourceType,
    sourceField: String(param.sourceField || '').trim(),
    value: param.value === undefined || param.value === null ? '' : String(param.value),
  }
}

function hasApiParamSource(param = {}) {
  if (param.sourceType === 'static')
    return param.value !== ''
  return Boolean(param.sourceField)
}

function isInvalidApiAction(action = {}) {
  if (normalizeActionType(action.actionType) !== 'CALL_API')
    return false
  const config = normalizeApiActionConfig(action.actionConfig)
  return !config.url && !config.capabilityCode
}

function addApiParam(action) {
  action.actionConfig = normalizeApiActionConfig(action.actionConfig)
  action.actionConfig.params.push(normalizeApiParam({
    target: normalizeApiMethod(action.actionConfig.method) === 'GET' ? 'query' : 'body',
    sourceType: 'rowField',
  }))
  markDirty()
}

function removeApiParam(action, index) {
  action.actionConfig.params.splice(index, 1)
  markDirty()
}

function applyApiConfig(action, value) {
  const configId = value === undefined || value === null ? '' : String(value)
  const selected = apiConfigs.value.find(item => String(item.id || item.apiCode || item.urlPath) === configId)
  action.actionConfig.apiConfigId = configId || null
  if (selected) {
    action.actionConfig.apiCode = selected.apiCode || ''
    action.actionConfig.apiName = selected.apiName || ''
    action.actionConfig.method = selected.needEncrypt && String(selected.reqMethod || '').toUpperCase() === 'POST'
      ? 'POST_ENCRYPT'
      : normalizeApiMethod(selected.reqMethod || 'POST')
    action.actionConfig.url = selected.urlPath || ''
  }
  markDirty()
}

function safeParseJson(value) {
  try {
    return JSON.parse(value || '{}')
  }
  catch {
    return {}
  }
}

function normalizeActionCode(value) {
  return String(value || '')
    .replace(/([a-z])([A-Z])/g, '$1_$2')
    .replace(/\W+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '')
    .toLowerCase()
    .slice(0, 64)
}

function actionPositionLabel(value) {
  return positionOptions.find(item => item.value === value)?.label || value || '-'
}

function actionTypeLabel(value) {
  return actionTypeOptions.find(item => item.value === value)?.label || value || '-'
}

function markDirty() {
  emit('dirtyChange', true)
}

defineExpose({
  saveActions,
  loadActions,
})
</script>

<style scoped>
.business-action-designer {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
}

.action-designer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.action-designer-head h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.action-designer-head p,
.action-card-head p,
.action-summary-pane p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.action-designer-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  min-height: 0;
}

.action-list-pane {
  min-width: 0;
  overflow: auto;
  background: #f8fafc;
  padding: 14px;
}

.action-card-list {
  display: grid;
  gap: 12px;
}

.action-card,
.action-summary-pane section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.action-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.action-card-head strong,
.action-summary-pane h4 {
  margin: 0;
  color: #111827;
  font-size: 14px;
}

.action-summary-pane {
  display: grid;
  align-content: start;
  gap: 12px;
  border-left: 1px solid #e5e7eb;
  background: #fbfcfe;
  padding: 12px;
}

.action-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 10px;
}

.action-stat-grid div {
  border-radius: 6px;
  background: #f1f5f9;
  padding: 10px;
  text-align: center;
}

.action-stat-grid span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.action-stat-grid strong {
  display: block;
  margin-top: 4px;
  color: #111827;
  font-size: 18px;
}

.main-flow-action-hint {
  display: grid;
  gap: 4px;
  width: 100%;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
  color: #1e40af;
  padding: 10px 12px;
}

.main-flow-action-hint strong {
  font-size: 13px;
}

.main-flow-action-hint span {
  color: #475569;
  font-size: 12px;
}

.api-action-panel {
  display: grid;
  gap: 12px;
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fbfcfe;
  padding: 12px;
}

.api-action-panel :deep(.n-form-item:last-child) {
  margin-bottom: 0;
}

.api-param-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.api-param-list {
  display: grid;
  gap: 8px;
}

.api-param-row {
  display: grid;
  grid-template-columns: minmax(100px, 1fr) 104px 120px minmax(150px, 1.2fr) 32px;
  gap: 8px;
  align-items: center;
}

@media (max-width: 1100px) {
  .action-designer-body {
    grid-template-columns: 1fr;
  }

  .action-summary-pane {
    border-left: 0;
    border-top: 1px solid #e5e7eb;
  }

  .api-param-row {
    grid-template-columns: 1fr;
  }
}
</style>
