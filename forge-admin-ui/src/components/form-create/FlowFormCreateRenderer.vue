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
})

const emit = defineEmits(['update:modelValue', 'change'])

installFormCreate(getCurrentInstance()?.appContext?.app)

const FormCreateComponent = getFormCreateComponent()
const formApi = ref(null)
const innerValue = ref(cloneValue(props.modelValue) || {})

const rules = computed(() => normalizeFormCreateRules(props.schema))
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

function handleChange() {
  emit('update:modelValue', cloneValue(innerValue.value) || {})
  emit('change', cloneValue(innerValue.value) || {})
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
