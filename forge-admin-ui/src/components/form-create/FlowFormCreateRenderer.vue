<template>
  <div class="flow-form-create-renderer">
    <component
      :is="FormCreateComponent"
      v-if="rules.length"
      v-model:api="formApi"
      v-model="innerValue"
      :rule="rules"
      :option="mergedOptions"
      :disabled="readOnly"
      @change="handleChange"
    />
    <n-empty v-else size="small" description="暂无动态表单配置" />
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, ref, watch } from 'vue'
import { hydrateForgeBusinessPreviewRules } from '@/views/app-center/components/designer/form-first/forgeBusinessComponents'
import {
  buildDefaultFormOptions,
  cloneValue,
  getFormCreateComponent,
  installFormCreate,
  normalizeFormCreateOptions,
  normalizeFormCreateRules,
} from './formCreateBridge'

const props = defineProps({
  schema: {
    type: [Array, String],
    default: () => [],
  },
  modelValue: {
    type: Object,
    default: () => ({}),
  },
  options: {
    type: [Object, String],
    default: () => ({}),
  },
  readOnly: {
    type: Boolean,
    default: false,
  },
  fieldPermissions: {
    type: [Array, String],
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'change'])

installFormCreate(getCurrentInstance()?.appContext?.app)

const FormCreateComponent = getFormCreateComponent()
const formApi = ref(null)
const innerValue = ref(cloneValue(props.modelValue) || {})
const hydratedRules = ref([])
let hydrateSeq = 0

const fieldPermissionMap = computed(() => {
  const map = new Map()
  for (const item of normalizeFieldPermissions(props.fieldPermissions)) {
    if (item.field)
      map.set(item.field, item)
  }
  return map
})
const rawRules = computed(() => applyFieldPermissions(
  normalizeFormCreateRules(props.schema),
  fieldPermissionMap.value,
))
const rules = computed(() => hydratedRules.value)
const mergedOptions = computed(() => ({
  ...buildDefaultFormOptions(),
  ...normalizeFormCreateOptions(props.options),
}))

watch(
  () => props.modelValue,
  (value) => {
    innerValue.value = cloneValue(value) || {}
  },
  { deep: true },
)

watch(
  rawRules,
  () => hydrateRules(),
  { immediate: true, deep: true },
)

function handleChange() {
  emit('update:modelValue', cloneValue(innerValue.value) || {})
  emit('change', cloneValue(innerValue.value) || {})
}

function normalizeFieldPermissions(source) {
  let list = source
  if (typeof source === 'string' && source.trim()) {
    try {
      list = JSON.parse(source)
    }
    catch {
      list = []
    }
  }
  if (!Array.isArray(list))
    return []
  return list.map(item => ({
    field: String(item?.field || '').trim(),
    readable: item?.readable !== false,
    writable: item?.writable !== false,
    required: item?.required === true,
  }))
}

function applyFieldPermissions(rules, permissionMap) {
  if (!permissionMap.size)
    return rules
  return applyFieldPermissionsToList(rules, permissionMap)
}

function applyFieldPermissionsToList(rules, permissionMap) {
  return (rules || [])
    .map(rule => applyFieldPermissionToRule(rule, permissionMap))
    .filter(Boolean)
}

function applyFieldPermissionToRule(rule, permissionMap) {
  if (!rule || typeof rule !== 'object')
    return rule

  const next = cloneValue(rule)
  const field = String(next.field || next.props?.field || '').trim()
  const permission = field ? permissionMap.get(field) : null

  if (permission?.readable === false)
    return null

  if (permission) {
    if (permission.writable === false) {
      next.props = { ...(next.props || {}), disabled: true }
      next.disabled = true
    }
    if (permission.required === true)
      next.validate = ensureRequiredRule(next.validate, next.title || field)
  }

  if (Array.isArray(next.children))
    next.children = applyFieldPermissionsToList(next.children, permissionMap)

  return next
}

function ensureRequiredRule(validate, title) {
  const list = Array.isArray(validate) ? [...validate] : []
  if (!list.some(item => item?.required))
    list.unshift({ required: true, message: `请输入${title || '该字段'}`, trigger: 'blur' })
  return list
}

async function hydrateRules() {
  const seq = ++hydrateSeq
  const normalizedRules = cloneValue(rawRules.value) || []
  hydratedRules.value = normalizedRules
  const nextRules = await hydrateForgeBusinessPreviewRules(normalizedRules)
  if (seq !== hydrateSeq)
    return
  hydratedRules.value = nextRules
}

async function validate() {
  if (!rules.value.length)
    return true
  await formApi.value?.validate?.()
  return true
}

function getData() {
  return cloneValue(formApi.value?.formData?.() || innerValue.value) || {}
}

async function submit() {
  await validate()
  return getData()
}

defineExpose({
  validate,
  getData,
  submit,
})
</script>

<style scoped>
.flow-form-create-renderer {
  width: 100%;
}

:deep(.form-create) {
  margin: 0;
}

:deep(.el-form-item:last-child) {
  margin-bottom: 0;
}
</style>
