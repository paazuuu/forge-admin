<template>
  <div class="form-create-adapter">
    <div class="designer-head">
      <div>
        <div class="designer-title">
          表单与详情页
        </div>
        <div class="designer-meta">
          新增、编辑、详情共用字段 · {{ selectedFieldRefs.length }} 个业务字段
        </div>
      </div>
      <n-space size="small" align="center">
        <n-switch
          :value="zone?.enabled !== false"
          size="small"
          @update:value="updateEnabled"
        />
        <n-button size="small" @click="resetFromModel">
          按模型重置
        </n-button>
        <n-button size="small" type="primary" @click="applyDesignerConfig">
          应用表单配置
        </n-button>
      </n-space>
    </div>

    <div class="designer-body">
      <FcDesigner
        ref="designerRef"
        :height="designerHeight"
        :config="designerConfig"
        @save="handleSave"
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
import formCreate from '@form-create/element-ui'
import ElementPlus from 'element-plus'
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import 'element-plus/dist/index.css'
import '@form-create/element-ui/src/style/index.css'

const props = defineProps({
  zone: {
    type: Object,
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:zone'])

installDesignerPlugins()

const designerRef = ref(null)
const syncingDesigner = ref(false)
const flushTimer = ref(null)
const designerHeight = '640px'
const designerConfig = {
  showAi: false,
  showInputData: false,
  showLanguage: false,
  showDevice: false,
  showComponentName: false,
  showSaveBtn: true,
  showPreviewBtn: true,
  showJsonPreview: true,
  exitConfirm: false,
  formOptions: buildDefaultOptions(),
}

const fieldMap = computed(() => new Map(props.fields.map(field => [field.field, field])))
const selectedFieldRefs = computed(() => {
  const refs = extractFieldRefs(props.zone?.props?.formCreateRule || [], fieldMap.value)
  if (refs.length)
    return refs
  return (props.zone?.fieldRefs || []).filter(ref => fieldMap.value.has(ref))
})

watch(
  () => [props.zone?.zoneKey, props.zone?.props?.formCreateRule, props.fields],
  () => loadDesignerRules(),
  { deep: true },
)

onMounted(() => loadDesignerRules())

onBeforeUnmount(() => {
  if (flushTimer.value)
    window.clearTimeout(flushTimer.value)
})

function loadDesignerRules() {
  if (!props.zone || props.zone.zoneKey !== 'edit')
    return
  nextTick(() => {
    if (!designerRef.value)
      return
    syncingDesigner.value = true
    const rules = resolveDesignerRules()
    designerRef.value.setRule(cloneValue(rules))
    designerRef.value.setOption(resolveDesignerOptions())
    nextTick(() => {
      syncingDesigner.value = false
    })
  })
}

function resolveDesignerRules() {
  const sourceRules = Array.isArray(props.zone?.props?.formCreateRule)
    ? props.zone.props.formCreateRule
    : []
  const refs = (props.zone?.fieldRefs?.length
    ? props.zone.fieldRefs
    : props.fields.map(field => field.field))
    .filter(ref => fieldMap.value.has(ref))

  if (!sourceRules.length)
    return refs.map(ref => buildRuleFromField(fieldMap.value.get(ref)))

  const normalizedRules = normalizeRules(sourceRules, fieldMap.value)
  const existingRefs = new Set(extractFieldRefs(normalizedRules, fieldMap.value))
  refs.forEach((ref) => {
    if (!existingRefs.has(ref))
      normalizedRules.push(buildRuleFromField(fieldMap.value.get(ref)))
  })
  return normalizedRules
}

function resolveDesignerOptions() {
  return {
    ...buildDefaultOptions(),
    ...(props.zone?.props?.formCreateOptions || {}),
  }
}

function queueFlush() {
  if (syncingDesigner.value)
    return
  if (flushTimer.value)
    window.clearTimeout(flushTimer.value)
  flushTimer.value = window.setTimeout(() => flushDesigner(), 160)
}

function flushDesigner() {
  if (!props.zone || !designerRef.value)
    return false
  const rules = designerRef.value.getRule?.() || []
  const options = designerRef.value.getOptions?.() || designerRef.value.getOption?.() || resolveDesignerOptions()
  const refs = extractFieldRefs(rules, fieldMap.value)
  emit('update:zone', {
    ...props.zone,
    fieldRefs: refs,
    props: {
      ...(props.zone.props || {}),
      formCreateRule: cloneValue(rules),
      formCreateOptions: cloneValue(options),
    },
  })
  return true
}

function applyDesignerConfig() {
  if (flushDesigner())
    window.$message?.success('表单与详情配置已应用')
  else
    window.$message?.warning('表单设计器尚未加载完成')
}

function handleSave() {
  if (flushDesigner())
    window.$message?.success('表单与详情配置已应用')
}

function updateEnabled(value) {
  if (!props.zone)
    return
  emit('update:zone', {
    ...props.zone,
    enabled: value,
  })
}

function resetFromModel() {
  if (!props.zone)
    return
  const refs = props.fields
    .map(field => field.field)
  const rules = refs.map(ref => buildRuleFromField(fieldMap.value.get(ref)))
  emit('update:zone', {
    ...props.zone,
    fieldRefs: refs,
    props: {
      ...(props.zone.props || {}),
      formCreateRule: rules,
      formCreateOptions: buildDefaultOptions(),
    },
  })
  nextTick(loadDesignerRules)
}

function buildRuleFromField(field) {
  const label = field?.label || field?.field || '字段'
  const componentType = resolveRuleType(field)
  const rule = {
    type: componentType,
    field: field.field,
    title: label,
    name: field.field,
    props: buildRuleProps(field, label),
  }
  if (field?.required) {
    rule.validate = [{
      required: true,
      message: buildPlaceholder(field, label),
      trigger: ['blur', 'change'],
    }]
  }
  if (field?.defaultValue !== undefined && field.defaultValue !== null && field.defaultValue !== '')
    rule.value = field.defaultValue
  if (['select', 'radio', 'checkbox'].includes(componentType))
    rule.options = buildOptions(field)
  return rule
}

function resolveRuleType(field = {}) {
  if (field.componentType === 'textarea')
    return 'textarea'
  if (field.componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field.dataType))
    return 'inputNumber'
  if (field.componentType === 'date' || field.dataType === 'date')
    return 'datePicker'
  if (field.componentType === 'datetime' || field.dataType === 'datetime')
    return 'datePicker'
  if (field.componentType === 'time' || field.dataType === 'time')
    return 'timePicker'
  if (field.componentType === 'switch')
    return 'switch'
  if (field.componentType === 'radio')
    return 'radio'
  if (field.componentType === 'checkbox')
    return 'checkbox'
  if (field.dictType || field.componentType === 'select')
    return 'select'
  if (['upload', 'fileUpload', 'imageUpload'].includes(field.componentType))
    return 'upload'
  return 'input'
}

function buildRuleProps(field, label) {
  const placeholder = buildPlaceholder(field, label)
  const props = {
    placeholder,
    clearable: true,
  }
  if (field.componentType === 'textarea') {
    props.type = 'textarea'
    props.rows = 3
    props.showWordLimit = true
  }
  if (field.length && ['input', 'textarea'].includes(resolveRuleType(field)))
    props.maxlength = field.length
  if (resolveRuleType(field) === 'inputNumber') {
    props.controls = false
    if (field.precision !== undefined && field.precision !== null)
      props.precision = field.precision
  }
  if (field.componentType === 'datetime' || field.dataType === 'datetime') {
    props.type = 'datetime'
    props.format = 'YYYY-MM-DD HH:mm:ss'
    props.valueFormat = 'YYYY-MM-DD HH:mm:ss'
  }
  if (field.componentType === 'date' || field.dataType === 'date') {
    props.type = 'date'
    props.format = 'YYYY-MM-DD'
    props.valueFormat = 'YYYY-MM-DD'
  }
  if (field.componentType === 'switch') {
    props.activeValue = 1
    props.inactiveValue = 0
  }
  if (field.componentType === 'imageUpload') {
    props.listType = 'picture-card'
    props.accept = 'image/*'
    props.autoUpload = false
  }
  if (field.componentType === 'fileUpload' || field.componentType === 'upload') {
    props.autoUpload = false
  }
  return props
}

function buildOptions(field) {
  if (field?.dictType) {
    return [
      { label: `${field.dictType}字典项`, value: '' },
    ]
  }
  return [
    { label: '选项一', value: '1' },
    { label: '选项二', value: '2' },
  ]
}

function buildPlaceholder(field, label) {
  const type = resolveRuleType(field)
  if (['select', 'radio', 'checkbox', 'datePicker', 'timePicker', 'upload'].includes(type))
    return `请选择${label}`
  return `请输入${label}`
}

function normalizeRules(rules, fields) {
  return cloneValue(rules).map(rule => normalizeRule(rule, fields)).filter(Boolean)
}

function normalizeRule(rule, fields) {
  if (!rule || typeof rule !== 'object')
    return null
  const field = rule.field && fields.get(rule.field)
  const nextRule = field
    ? {
        ...rule,
        title: field.label || rule.title || field.field,
        name: rule.name || field.field,
        props: {
          ...buildRuleProps(field, field.label || field.field),
          ...(rule.props || {}),
        },
      }
    : { ...rule }
  if (Array.isArray(nextRule.children))
    nextRule.children = nextRule.children.map(child => normalizeRule(child, fields)).filter(Boolean)
  return nextRule
}

function extractFieldRefs(rules, fields) {
  const refs = []
  const walk = (items) => {
    ;(items || []).forEach((rule) => {
      if (!rule || typeof rule !== 'object')
        return
      if (rule.field && fields.has(rule.field) && !refs.includes(rule.field))
        refs.push(rule.field)
      if (Array.isArray(rule.children))
        walk(rule.children)
    })
  }
  walk(rules)
  return refs
}

function buildDefaultOptions() {
  return {
    form: {
      labelPosition: 'right',
      labelWidth: '110px',
      size: 'default',
    },
    submitBtn: false,
    resetBtn: false,
  }
}

function cloneValue(value) {
  return JSON.parse(JSON.stringify(value ?? null))
}

function installDesignerPlugins() {
  const app = getCurrentInstance()?.appContext?.app
  if (!app || app._context.provides.__lowcodeFormCreateInstalled)
    return
  app.use(ElementPlus)
  app.use(formCreate)
  app._context.provides.__lowcodeFormCreateInstalled = true
}
</script>

<style scoped>
.form-create-adapter {
  min-height: 704px;
  display: grid;
  grid-template-rows: 56px minmax(0, 1fr);
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.designer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #ffffff;
}

.designer-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.designer-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.designer-body {
  min-width: 0;
  min-height: 0;
  background: #f8fafc;
}

:deep(.fc-designer) {
  --fc-primary: #2563eb;
  border: 0;
}

:deep(.fc-designer ._fc-l),
:deep(.fc-designer ._fc-r) {
  background: #fff;
}
</style>
