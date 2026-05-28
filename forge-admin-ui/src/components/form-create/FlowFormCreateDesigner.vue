<template>
  <div class="flow-form-create-designer">
    <div class="designer-toolbar">
      <div>
        <div class="designer-title">
          FormCreate 动态表单
        </div>
        <div class="designer-subtitle">
          右侧基础配置中的“字段”就是变量/业务字段 ID，可按业务模型字段维护
        </div>
      </div>
      <n-space size="small">
        <n-button size="small" @click="handleReset">
          清空
        </n-button>
        <n-button size="small" type="primary" @click="handleSave">
          保存配置
        </n-button>
      </n-space>
    </div>
    <div class="designer-shell">
      <FcDesigner
        ref="designerRef"
        :height="height"
        :config="designerConfig"
        @create="queueFlush"
        @copy="queueFlush"
        @delete="queueFlush"
        @drag="queueFlush"
        @paste-rule="queueFlush"
        @sort-up="queueFlush"
        @sort-down="queueFlush"
        @change-field="queueFlush"
      />
    </div>
  </div>
</template>

<script setup>
import FcDesigner from '@form-create/designer'
import { getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { buildDefaultFormOptions, cloneValue, installFormCreate, normalizeFormCreateRules } from './formCreateBridge'

const props = defineProps({
  modelValue: {
    type: [Array, String],
    default: () => [],
  },
  height: {
    type: String,
    default: '680px',
  },
})

const emit = defineEmits(['update:modelValue', 'save'])

installFormCreate(getCurrentInstance()?.appContext?.app)

const designerRef = ref(null)
const syncingDesigner = ref(false)
const flushTimer = ref(null)
const localRulesSnapshot = ref('')

const designerConfig = {
  showAi: false,
  showInputData: false,
  showLanguage: false,
  showDevice: false,
  showComponentName: false,
  fieldReadonly: false,
  showSaveBtn: false,
  showPreviewBtn: true,
  showJsonPreview: true,
  exitConfirm: false,
  formOptions: buildDefaultFormOptions(),
}

watch(
  () => props.modelValue,
  () => loadDesignerRules(),
  { deep: true },
)

onMounted(() => loadDesignerRules())

onBeforeUnmount(() => {
  if (flushTimer.value)
    window.clearTimeout(flushTimer.value)
})

function loadDesignerRules() {
  nextTick(() => {
    if (!designerRef.value)
      return
    const rules = normalizeFormCreateRules(props.modelValue)
    const snapshot = JSON.stringify(rules)
    if (snapshot === localRulesSnapshot.value)
      return
    syncingDesigner.value = true
    designerRef.value.setRule(cloneValue(rules))
    designerRef.value.setOption(buildDefaultFormOptions())
    localRulesSnapshot.value = snapshot
    nextTick(() => {
      syncingDesigner.value = false
    })
  })
}

function queueFlush() {
  if (syncingDesigner.value)
    return
  if (flushTimer.value)
    window.clearTimeout(flushTimer.value)
  flushTimer.value = window.setTimeout(() => flushDesigner(), 160)
}

function flushDesigner() {
  if (!designerRef.value)
    return []
  const rules = designerRef.value.getRule?.() || []
  const clonedRules = cloneValue(rules) || []
  localRulesSnapshot.value = JSON.stringify(clonedRules)
  emit('update:modelValue', clonedRules)
  return clonedRules
}

function handleReset() {
  if (!designerRef.value)
    return
  designerRef.value.setRule([])
  flushDesigner()
}

function handleSave() {
  const rules = flushDesigner()
  emit('save', rules)
}

function getRules() {
  return flushDesigner()
}

defineExpose({
  getRules,
})
</script>

<style scoped>
.flow-form-create-designer {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #d7dde7;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.designer-toolbar {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #f8fafc;
}

.designer-title {
  font-size: 14px;
  font-weight: 700;
  color: #172033;
}

.designer-subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: #667085;
}

.designer-shell {
  flex: 1;
  min-height: 0;
  background: #f6f8fb;
}

:deep(.fc-designer) {
  --fc-primary: #2563eb;
  border: 0;
}
</style>
