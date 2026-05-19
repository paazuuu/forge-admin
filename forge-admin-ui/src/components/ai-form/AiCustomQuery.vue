<template>
  <div class="ai-custom-query">
    <n-space :size="8" align="center">
      <n-button
        size="small"
        strong
        secondary
        :type="queryActive ? 'primary' : 'default'"
        @click="openDrawer"
      >
        <template #icon>
          <n-icon><SearchOutline /></n-icon>
        </template>
        自定义查询
      </n-button>
      <n-button
        v-if="queryActive"
        size="small"
        quaternary
        @click="handleClear"
      >
        清除
      </n-button>
    </n-space>

    <n-drawer
      v-model:show="drawerVisible"
      :width="860"
      placement="right"
      :mask-closable="false"
    >
      <n-drawer-content title="自定义查询" :closable="true">
        <div class="query-head">
          <n-select
            v-model:value="selectedSchemeId"
            :options="schemeOptions"
            :loading="schemeLoading"
            clearable
            filterable
            placeholder="选择查询方案"
            class="scheme-select"
            @update:value="handleSchemeSelect"
          />
          <n-button size="small" secondary @click="loadSchemes">
            刷新
          </n-button>
          <n-button size="small" type="primary" @click="handleApply">
            <template #icon>
              <n-icon><SearchOutline /></n-icon>
            </template>
            执行查询
          </n-button>
        </div>

        <n-tabs v-model:value="activeTab" type="line" animated>
          <n-tab-pane name="conditions" tab="查询条件">
            <div class="condition-list">
              <div
                v-for="(condition, index) in conditions"
                :key="condition.uid"
                class="condition-row"
              >
                <n-select
                  v-if="index > 0"
                  v-model:value="condition.relation"
                  :options="relationOptions"
                  class="relation-select"
                />
                <div v-else class="relation-placeholder">
                  当
                </div>

                <n-select
                  v-model:value="condition.field"
                  :options="queryFieldOptions"
                  filterable
                  placeholder="字段"
                  class="field-select"
                  @update:value="resetConditionValue(condition)"
                />

                <n-select
                  v-model:value="condition.operator"
                  :options="operatorOptions"
                  class="operator-select"
                  @update:value="resetConditionValue(condition)"
                />

                <template v-if="requiresValue(condition.operator)">
                  <n-input
                    v-if="condition.operator !== 'between'"
                    v-model:value="condition.value"
                    clearable
                    placeholder="值"
                    class="value-input"
                  />
                  <n-input-group v-else class="range-input">
                    <n-input
                      v-model:value="condition.value"
                      clearable
                      placeholder="起始值"
                    />
                    <n-input
                      v-model:value="condition.valueEnd"
                      clearable
                      placeholder="结束值"
                    />
                  </n-input-group>
                </template>
                <div v-else class="empty-value" />

                <n-button
                  quaternary
                  circle
                  :disabled="conditions.length === 1"
                  @click="removeCondition(index)"
                >
                  <template #icon>
                    <n-icon><TrashOutline /></n-icon>
                  </template>
                </n-button>
              </div>
            </div>

            <n-button size="small" dashed class="add-condition" @click="addCondition">
              <template #icon>
                <n-icon><AddOutline /></n-icon>
              </template>
              添加条件
            </n-button>
          </n-tab-pane>

          <n-tab-pane name="columns" tab="结果展示">
            <div class="display-grid">
              <div class="display-section">
                <div class="section-title">
                  展示字段
                </div>
                <n-checkbox-group v-model:value="selectedFields">
                  <n-grid :cols="2" :x-gap="12" :y-gap="10" responsive="screen">
                    <n-grid-item
                      v-for="field in fieldOptions"
                      :key="field.value"
                    >
                      <n-checkbox :value="field.value">
                        {{ field.label }}
                      </n-checkbox>
                    </n-grid-item>
                  </n-grid>
                </n-checkbox-group>
              </div>

              <div class="display-section narrow">
                <div class="section-title">
                  展示方式
                </div>
                <n-radio-group v-model:value="currentRenderMode">
                  <n-radio-button value="table">
                    列表
                  </n-radio-button>
                  <n-radio-button value="card">
                    卡片
                  </n-radio-button>
                </n-radio-group>

                <div class="section-title with-gap">
                  排序
                </div>
                <n-space vertical :size="10">
                  <n-select
                    v-model:value="orderByColumn"
                    :options="fieldOptions"
                    clearable
                    filterable
                    placeholder="排序字段"
                  />
                  <n-radio-group v-model:value="isAsc">
                    <n-radio-button value="asc">
                      升序
                    </n-radio-button>
                    <n-radio-button value="desc">
                      降序
                    </n-radio-button>
                  </n-radio-group>
                </n-space>
              </div>
            </div>
          </n-tab-pane>

          <n-tab-pane name="schemes" tab="常用方案">
            <n-spin :show="schemeLoading">
              <div v-if="schemes.length > 0" class="scheme-list">
                <div
                  v-for="scheme in schemes"
                  :key="scheme.id"
                  class="scheme-item"
                  :class="{ active: selectedSchemeId === scheme.id }"
                  @click="applySchemeToBuilder(scheme)"
                >
                  <div class="scheme-meta">
                    <div class="scheme-title">
                      {{ scheme.schemeName }}
                      <n-tag v-if="scheme.isDefault === 1" size="tiny" type="success">
                        默认
                      </n-tag>
                    </div>
                    <div class="scheme-sub">
                      {{ scheme.renderMode === 'card' ? '卡片' : '列表' }} · {{ (scheme.fields || []).length }} 字段
                    </div>
                  </div>
                  <n-space :size="6" @click.stop>
                    <n-button size="tiny" @click="applySchemeAndRun(scheme)">
                      应用
                    </n-button>
                    <n-button size="tiny" tertiary type="error" @click="deleteScheme(scheme)">
                      删除
                    </n-button>
                  </n-space>
                </div>
              </div>
              <n-empty v-else description="暂无查询方案" />
            </n-spin>
          </n-tab-pane>
        </n-tabs>

        <template #footer>
          <n-space justify="space-between">
            <n-space>
              <n-button @click="openSaveModal('create')">
                <template #icon>
                  <n-icon><SaveOutline /></n-icon>
                </template>
                保存方案
              </n-button>
              <n-button
                :disabled="!selectedSchemeId"
                @click="openSaveModal('update')"
              >
                更新方案
              </n-button>
            </n-space>
            <n-space>
              <n-button @click="drawerVisible = false">
                关闭
              </n-button>
              <n-button type="primary" @click="handleApply">
                执行查询
              </n-button>
            </n-space>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <n-modal
      v-model:show="saveModalVisible"
      preset="card"
      :title="saveMode === 'update' ? '更新查询方案' : '保存查询方案'"
      style="width: 460px"
      :mask-closable="false"
    >
      <n-form
        ref="saveFormRef"
        :model="saveForm"
        :rules="saveRules"
        label-placement="left"
        label-width="92"
      >
        <n-form-item label="方案名称" path="schemeName">
          <n-input v-model:value="saveForm.schemeName" clearable placeholder="请输入方案名称" />
        </n-form-item>
        <n-form-item label="设为默认">
          <n-switch v-model:value="saveForm.isDefault" />
        </n-form-item>
        <n-form-item label="备注">
          <n-input
            v-model:value="saveForm.remark"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="请输入备注"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="saveModalVisible = false">
            取消
          </n-button>
          <n-button type="primary" :loading="saving" @click="saveScheme">
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { AddOutline, SaveOutline, SearchOutline, TrashOutline } from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'
import {
  customQuerySchemeAdd,
  customQuerySchemeDelete,
  customQuerySchemeList,
  customQuerySchemeUpdate,
} from '@/api/ai'

const props = defineProps({
  configKey: {
    type: String,
    required: true,
  },
  columns: {
    type: Array,
    default: () => [],
  },
  searchSchema: {
    type: Array,
    default: () => [],
  },
  editSchema: {
    type: Array,
    default: () => [],
  },
  renderMode: {
    type: String,
    default: 'table',
  },
})

const emit = defineEmits(['apply', 'clear'])

const relationOptions = [
  { label: '并且', value: 'AND' },
  { label: '或者', value: 'OR' },
]

const operatorOptions = [
  { label: '等于', value: 'eq' },
  { label: '不等于', value: 'ne' },
  { label: '包含', value: 'like' },
  { label: '大于', value: 'gt' },
  { label: '大于等于', value: 'ge' },
  { label: '小于', value: 'lt' },
  { label: '小于等于', value: 'le' },
  { label: '属于', value: 'in' },
  { label: '区间', value: 'between' },
  { label: '为空', value: 'is_null' },
  { label: '不为空', value: 'is_not_null' },
]

const drawerVisible = ref(false)
const activeTab = ref('conditions')
const queryActive = ref(false)
const schemeLoading = ref(false)
const saving = ref(false)
const schemes = ref([])
const selectedSchemeId = ref(null)
const conditions = ref([createCondition()])
const selectedFields = ref([])
const fieldsInitialized = ref(false)
const currentRenderMode = ref(props.renderMode || 'table')
const orderByColumn = ref(null)
const isAsc = ref('desc')
const saveModalVisible = ref(false)
const saveMode = ref('create')
const saveFormRef = ref(null)
const saveForm = ref({
  schemeName: '',
  isDefault: false,
  remark: '',
})

const saveRules = {
  schemeName: [
    { required: true, message: '请输入方案名称', trigger: ['input', 'blur'] },
  ],
}

const fieldOptions = computed(() => {
  const map = new Map()
  ;[...(props.columns || [])].forEach((item) => {
    const field = normalizeField(item)
    if (field && !map.has(field.value)) {
      map.set(field.value, field)
    }
  })
  return Array.from(map.values())
})

const queryFieldOptions = computed(() => {
  const map = new Map()
  ;[...(props.columns || []), ...(props.searchSchema || []), ...(props.editSchema || [])].forEach((item) => {
    const field = normalizeField(item)
    if (field && !map.has(field.value)) {
      map.set(field.value, field)
    }
  })
  return Array.from(map.values())
})

const schemeOptions = computed(() => {
  return schemes.value.map(item => ({
    label: `${item.schemeName}${item.isDefault === 1 ? '（默认）' : ''}`,
    value: item.id,
  }))
})

watch(
  () => props.renderMode,
  (mode) => {
    currentRenderMode.value = mode || 'table'
  },
)

watch(
  fieldOptions,
  (options) => {
    if (fieldsInitialized.value || options.length === 0) {
      return
    }
    selectedFields.value = options.map(item => item.value)
    fieldsInitialized.value = true
  },
  { immediate: true },
)

function normalizeField(item) {
  const value = item?.prop || item?.key || item?.dataIndex || item?.field
  if (!value || value === 'action' || value === 'actions') {
    return null
  }
  return {
    label: item.label || item.title || item.fieldLabel || value,
    value,
  }
}

function createCondition() {
  return {
    uid: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    relation: 'AND',
    field: null,
    operator: 'like',
    value: null,
    valueEnd: null,
  }
}

function openDrawer() {
  drawerVisible.value = true
  loadSchemes()
}

async function loadSchemes() {
  if (!props.configKey) {
    return
  }
  schemeLoading.value = true
  try {
    const res = await customQuerySchemeList(props.configKey)
    schemes.value = res.data || []
  }
  catch (error) {
    console.error('加载查询方案失败:', error)
    window.$message?.error('加载查询方案失败')
  }
  finally {
    schemeLoading.value = false
  }
}

function handleSchemeSelect(id) {
  const scheme = schemes.value.find(item => item.id === id)
  if (scheme) {
    applySchemeToBuilder(scheme)
  }
}

function applySchemeToBuilder(scheme) {
  selectedSchemeId.value = scheme.id
  conditions.value = normalizeConditions(scheme.conditions)
  selectedFields.value = normalizeFields(scheme.fields)
  currentRenderMode.value = scheme.renderMode || 'table'
  orderByColumn.value = scheme.orderByColumn || null
  isAsc.value = scheme.isAsc || 'desc'
  saveForm.value = {
    schemeName: scheme.schemeName || '',
    isDefault: scheme.isDefault === 1,
    remark: scheme.remark || '',
  }
  activeTab.value = 'conditions'
}

function applySchemeAndRun(scheme) {
  applySchemeToBuilder(scheme)
  handleApply()
}

function normalizeConditions(items) {
  if (!Array.isArray(items) || items.length === 0) {
    return [createCondition()]
  }
  return items.map(item => ({
    uid: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    relation: item.relation || 'AND',
    field: item.field || null,
    operator: item.operator || 'like',
    value: item.value ?? null,
    valueEnd: item.valueEnd ?? null,
  }))
}

function normalizeFields(fields) {
  const validFields = new Set(fieldOptions.value.map(item => item.value))
  const values = (fields || []).filter(field => validFields.has(field))
  return values.length > 0 ? values : fieldOptions.value.map(item => item.value)
}

function addCondition() {
  conditions.value.push(createCondition())
}

function removeCondition(index) {
  if (conditions.value.length <= 1) {
    return
  }
  conditions.value.splice(index, 1)
}

function resetConditionValue(condition) {
  condition.value = null
  condition.valueEnd = null
}

function requiresValue(operator) {
  return !['is_null', 'is_not_null'].includes(operator)
}

function buildConditions() {
  return conditions.value
    .filter((condition) => {
      if (!condition.field || !condition.operator) {
        return false
      }
      if (!requiresValue(condition.operator)) {
        return true
      }
      if (condition.operator === 'between') {
        return hasValue(condition.value) && hasValue(condition.valueEnd)
      }
      return hasValue(condition.value)
    })
    .map((condition, index) => ({
      relation: index === 0 ? 'AND' : (condition.relation || 'AND'),
      field: condition.field,
      operator: condition.operator,
      value: condition.value,
      valueEnd: condition.operator === 'between' ? condition.valueEnd : null,
    }))
}

function hasValue(value) {
  if (Array.isArray(value)) {
    return value.length > 0
  }
  return value !== null && value !== undefined && String(value).trim() !== ''
}

function buildPayload() {
  return {
    conditions: buildConditions(),
    fields: selectedFields.value,
    orderByColumn: orderByColumn.value,
    isAsc: isAsc.value,
    renderMode: currentRenderMode.value,
  }
}

function handleApply() {
  if (selectedFields.value.length === 0) {
    window.$message?.warning('请至少选择一个展示字段')
    activeTab.value = 'columns'
    return
  }
  queryActive.value = true
  emit('apply', buildPayload())
  drawerVisible.value = false
}

function handleClear() {
  queryActive.value = false
  selectedSchemeId.value = null
  conditions.value = [createCondition()]
  orderByColumn.value = null
  isAsc.value = 'desc'
  emit('clear')
}

function openSaveModal(mode) {
  if (selectedFields.value.length === 0) {
    window.$message?.warning('请至少选择一个展示字段')
    activeTab.value = 'columns'
    return
  }
  if (mode === 'update' && !selectedSchemeId.value) {
    window.$message?.warning('请先选择要更新的查询方案')
    return
  }
  saveMode.value = mode
  const current = schemes.value.find(item => item.id === selectedSchemeId.value)
  saveForm.value = {
    schemeName: current?.schemeName || saveForm.value.schemeName || '',
    isDefault: current?.isDefault === 1 || saveForm.value.isDefault,
    remark: current?.remark || saveForm.value.remark || '',
  }
  saveModalVisible.value = true
}

async function saveScheme() {
  await saveFormRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      ...buildPayload(),
      schemeName: saveForm.value.schemeName,
      isDefault: saveForm.value.isDefault ? 1 : 0,
      remark: saveForm.value.remark,
    }
    if (saveMode.value === 'update') {
      payload.id = selectedSchemeId.value
      await customQuerySchemeUpdate(props.configKey, payload)
      window.$message?.success('查询方案已更新')
    }
    else {
      const res = await customQuerySchemeAdd(props.configKey, payload)
      selectedSchemeId.value = res.data
      window.$message?.success('查询方案已保存')
    }
    saveModalVisible.value = false
    await loadSchemes()
  }
  catch (error) {
    if (!Array.isArray(error)) {
      console.error('保存查询方案失败:', error)
      window.$message?.error(error?.message || '保存查询方案失败')
    }
  }
  finally {
    saving.value = false
  }
}

function deleteScheme(scheme) {
  window.$dialog?.warning({
    title: '确认删除',
    content: `确定删除查询方案「${scheme.schemeName}」吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await customQuerySchemeDelete(props.configKey, scheme.id)
        if (selectedSchemeId.value === scheme.id) {
          selectedSchemeId.value = null
        }
        window.$message?.success('查询方案已删除')
        await loadSchemes()
      }
      catch (error) {
        console.error('删除查询方案失败:', error)
        window.$message?.error(error?.message || '删除查询方案失败')
      }
    },
  })
}
</script>

<style scoped>
.ai-custom-query {
  display: inline-flex;
  align-items: center;
}

.query-head {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}

.scheme-select {
  flex: 1;
  min-width: 0;
}

.condition-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.condition-row {
  display: grid;
  grid-template-columns: 72px minmax(150px, 1fr) 116px minmax(180px, 1.4fr) 34px;
  gap: 8px;
  align-items: center;
}

.relation-select,
.operator-select {
  width: 100%;
}

.relation-placeholder {
  height: 32px;
  line-height: 32px;
  color: var(--text-secondary);
  text-align: center;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: 6px;
}

.field-select,
.value-input,
.range-input {
  width: 100%;
}

.empty-value {
  height: 32px;
}

.add-condition {
  width: 100%;
  margin-top: 12px;
}

.display-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 24px;
}

.display-section {
  min-width: 0;
}

.display-section.narrow {
  padding-left: 20px;
  border-left: 1px solid var(--border-light);
}

.section-title {
  margin-bottom: 12px;
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--text-primary);
}

.section-title.with-gap {
  margin-top: 24px;
}

.scheme-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.scheme-item {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  cursor: pointer;
  border: 1px solid var(--border-light);
  border-radius: 8px;
}

.scheme-item:hover,
.scheme-item.active {
  border-color: var(--primary-color);
  background: color-mix(in srgb, var(--primary-color, #165dff) 8%, transparent);
}

.scheme-meta {
  min-width: 0;
}

.scheme-title {
  display: flex;
  gap: 8px;
  align-items: center;
  font-weight: 600;
  color: var(--text-primary);
}

.scheme-sub {
  margin-top: 4px;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

@media (max-width: 720px) {
  .condition-row,
  .display-grid {
    grid-template-columns: 1fr;
  }

  .display-section.narrow {
    padding-left: 0;
    border-left: 0;
  }
}
</style>
