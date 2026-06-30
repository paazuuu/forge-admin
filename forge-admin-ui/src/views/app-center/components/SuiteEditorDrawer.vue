<template>
  <n-drawer :show="show" width="620" @update:show="value => emit('update:show', value)">
    <n-drawer-content :title="form.id ? '编辑业务域' : '新建业务域'" closable>
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top">
        <n-grid :cols="2" :x-gap="12">
          <n-form-item-gi label="业务域名称" path="suiteName">
            <n-input v-model:value="form.suiteName" placeholder="例如：合同管理" />
          </n-form-item-gi>
          <n-form-item-gi label="业务域编码" path="suiteCode">
            <n-input
              v-model:value="form.suiteCode"
              :disabled="Boolean(form.id)"
              placeholder="例如：CONTRACT"
              @blur="form.suiteCode = normalizeCode(form.suiteCode)"
            />
          </n-form-item-gi>
        </n-grid>

        <n-form-item label="业务域图标">
          <IconSelector v-model="form.icon" />
        </n-form-item>

        <n-form-item label="上级业务域">
          <n-tree-select
            v-model:value="form.parentId"
            clearable
            filterable
            :options="parentSuiteOptions"
            placeholder="不选择则作为顶级业务域"
          />
        </n-form-item>

        <n-grid :cols="3" :x-gap="12">
          <n-form-item-gi label="启用状态">
            <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0" />
          </n-form-item-gi>
          <n-form-item-gi label="排序">
            <n-input-number v-model:value="form.sortOrder" :min="0" :show-button="false" placeholder="排序" />
          </n-form-item-gi>
          <n-form-item-gi label="创建管理端目录">
            <n-switch v-model:value="form.createMenuDirectory" />
          </n-form-item-gi>
        </n-grid>

        <template v-if="form.createMenuDirectory">
          <n-form-item label="父级菜单或模块">
            <MenuParentSelect v-model:value="form.adminMenuParentId" placeholder="选择业务域目录挂载位置，默认顶级" />
          </n-form-item>
          <n-form-item label="目录排序">
            <n-input-number v-model:value="form.menuSort" :min="0" :show-button="false" placeholder="默认使用业务域排序" />
          </n-form-item>
        </template>

        <n-form-item label="业务说明">
          <n-input
            v-model:value="form.description"
            type="textarea"
            placeholder="说明这个业务域覆盖的业务范围"
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="emit('update:show', false)">
            取消
          </n-button>
          <n-button type="primary" :loading="saving" @click="save">
            保存
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { createBusinessSuite, updateBusinessSuite } from '@/api/business-app'
import IconSelector from '@/components/IconSelector.vue'
import MenuParentSelect from '@/components/lowcode-builder/shared/MenuParentSelect.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  suite: {
    type: Object,
    default: null,
  },
  suites: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:show', 'saved'])
const message = useMessage()
const formRef = ref(null)
const saving = ref(false)
const form = reactive(defaultForm())

const rules = {
  suiteName: { required: true, message: '请输入业务域名称', trigger: 'blur' },
  suiteCode: {
    required: true,
    validator: (_, value) => isValidCode(value),
    message: '业务域编码需以字母开头，仅包含字母、数字和下划线',
    trigger: ['blur', 'input'],
  },
}

const parentSuiteOptions = computed(() => {
  const blockedIds = new Set()
  const childrenMap = buildSuiteChildrenMap()
  if (form.id)
    collectSuiteDescendantIds(String(form.id), blockedIds, childrenMap, new Set())
  return buildParentOptions(childrenMap, '__root__', new Set(), blockedIds)
})

watch(() => props.show, (visible) => {
  if (!visible)
    return
  Object.assign(form, defaultForm(), props.suite || {})
  hydrateOptions()
})

async function save() {
  await formRef.value?.validate()
  const payload = buildPayload()
  if (props.suite?.status === 1 && payload.status === 0) {
    if (!window.$dialog?.warning) {
      await persist(payload)
      return
    }
    window.$dialog.warning({
      title: '停用业务域',
      content: `确定停用“${payload.suiteName || payload.suiteCode}”吗？停用后该业务域下的业务单元和访问入口配置仍会保留。`,
      positiveText: '停用并保存',
      negativeText: '取消',
      onPositiveClick: () => persist(payload),
    })
    return
  }
  await persist(payload)
}

function buildPayload() {
  return {
    id: form.id,
    parentId: normalizeId(form.parentId),
    suiteCode: normalizeCode(form.suiteCode),
    suiteName: form.suiteName.trim(),
    icon: trimToNull(form.icon),
    description: trimToNull(form.description),
    status: form.status,
    sortOrder: Number(form.sortOrder || 0),
    options: buildOptions(),
  }
}

async function persist(payload) {
  saving.value = true
  try {
    if (form.id)
      await updateBusinessSuite(payload)
    else
      await createBusinessSuite(payload)
    message.success('业务域已保存')
    emit('saved', payload)
    emit('update:show', false)
  }
  finally {
    saving.value = false
  }
}

function hydrateOptions() {
  let options = {}
  try {
    options = form.options ? JSON.parse(form.options) : {}
  }
  catch {
    options = {}
  }
  const adminMenu = options.adminMenu || {}
  form.createMenuDirectory = normalizeBoolean(adminMenu.syncEnabled ?? options.createMenuDirectory, false)
  form.adminMenuParentId = adminMenu.parentId ?? options.adminMenuParentId ?? null
  form.menuSort = Number(adminMenu.sort ?? options.menuSort ?? form.sortOrder ?? 0)
}

function buildSuiteChildrenMap() {
  const suiteById = new Map()
  props.suites.forEach((suite) => {
    if (suite?.id != null)
      suiteById.set(String(suite.id), suite)
  })
  const map = new Map()
  const sortedSuites = [...props.suites].sort(compareSuites)
  sortedSuites.forEach((suite) => {
    if (!suite?.id)
      return
    const parentKey = suite.parentId && suiteById.has(String(suite.parentId))
      ? String(suite.parentId)
      : '__root__'
    if (!map.has(parentKey))
      map.set(parentKey, [])
    map.get(parentKey).push(suite)
  })
  return map
}

function buildParentOptions(childrenMap, parentKey, visited, blockedIds) {
  return (childrenMap.get(parentKey) || []).flatMap((suite) => {
    const suiteKey = String(suite.id)
    if (visited.has(suiteKey) || blockedIds.has(suiteKey))
      return []
    const nextVisited = new Set(visited)
    nextVisited.add(suiteKey)
    const children = buildParentOptions(childrenMap, suiteKey, nextVisited, blockedIds)
    return [{
      label: suite.suiteName || suite.suiteCode,
      key: suite.id,
      children,
    }]
  })
}

function collectSuiteDescendantIds(parentKey, result, childrenMap, visited) {
  if (visited.has(parentKey))
    return
  visited.add(parentKey)
  result.add(parentKey)
  ;(childrenMap.get(parentKey) || []).forEach((child) => {
    if (child?.id != null)
      collectSuiteDescendantIds(String(child.id), result, childrenMap, visited)
  })
}

function compareSuites(left, right) {
  const sortCompare = Number(left?.sortOrder || 0) - Number(right?.sortOrder || 0)
  if (sortCompare !== 0)
    return sortCompare
  return String(left?.suiteName || left?.suiteCode || '')
    .localeCompare(String(right?.suiteName || right?.suiteCode || ''), 'zh-CN')
}

function buildOptions() {
  let options = {}
  try {
    options = form.options ? JSON.parse(form.options) : {}
  }
  catch {
    options = {}
  }
  if (form.createMenuDirectory) {
    const previousAdminMenu = options.adminMenu || {}
    options.adminMenu = {
      syncEnabled: true,
      parentId: form.adminMenuParentId || null,
      sort: Number(form.menuSort ?? form.sortOrder ?? 0),
    }
    if (previousAdminMenu.menuResourceId)
      options.adminMenu.menuResourceId = previousAdminMenu.menuResourceId
  }
  else {
    delete options.adminMenu
    delete options.adminMenuParentId
    delete options.createMenuDirectory
    delete options.menuSort
  }
  return Object.keys(options).length ? JSON.stringify(options) : null
}

function normalizeId(value) {
  if (value === undefined || value === null || value === '')
    return null
  return value
}

function normalizeCode(value) {
  return String(value || '')
    .trim()
    .replace(/[\s-]+/g, '_')
    .replace(/\W/g, '')
    .toUpperCase()
}

function isValidCode(value) {
  return /^[a-z]\w{1,63}$/i.test(normalizeCode(value))
}

function normalizeBoolean(value, fallback) {
  if (value === undefined || value === null)
    return fallback
  if (typeof value === 'boolean')
    return value
  return String(value) === 'true' || String(value) === '1'
}

function trimToNull(value) {
  const text = String(value || '').trim()
  return text || null
}

function defaultForm() {
  return {
    id: null,
    parentId: null,
    suiteCode: '',
    suiteName: '',
    icon: '',
    description: '',
    status: 1,
    sortOrder: 0,
    options: '',
    createMenuDirectory: false,
    adminMenuParentId: null,
    menuSort: 0,
  }
}
</script>
