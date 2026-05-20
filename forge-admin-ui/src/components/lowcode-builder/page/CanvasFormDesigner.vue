<template>
  <div class="canvas-designer">
    <div class="canvas-toolbar">
      <div>
        <div class="canvas-title">
          {{ zoneTitle }}
        </div>
        <div class="canvas-subtitle">
          Leafer Canvas · {{ canvasItems.length }} 个组件 · {{ canvas.width }} × {{ canvas.height }}
        </div>
      </div>
      <n-space size="small" align="center">
        <NSwitch
          :value="zone?.enabled !== false"
          size="small"
          @update:value="handleEnabledChange"
        />
        <NInputNumber
          :value="canvas.width"
          :min="720"
          :max="2000"
          :step="40"
          size="small"
          class="size-input"
          @update:value="updateCanvasSize('width', $event)"
        />
        <span class="size-divider">×</span>
        <NInputNumber
          :value="canvas.height"
          :min="360"
          :max="1800"
          :step="40"
          size="small"
          class="size-input"
          @update:value="updateCanvasSize('height', $event)"
        />
        <NButton size="small" @click="autoArrange">
          自动排列
        </NButton>
      </n-space>
    </div>

    <div
      ref="scrollRef"
      class="canvas-scroll"
      @dragover.prevent
      @drop="handleDrop"
    >
      <div
        ref="boardRef"
        class="canvas-board"
        :style="boardStyle"
      >
        <div class="canvas-grid" />
        <div ref="leaferViewRef" class="leafer-layer" />
        <div class="dom-layer">
          <div
            v-for="canvasItem in overlayItems"
            :key="canvasItem.id"
            class="canvas-dom-item"
            :class="{ selected: canvasItem.id === selectedItemId }"
            :style="resolveItemStyle(canvasItem)"
          >
            <div class="item-title">
              <span>{{ canvasItem.label || resolveComponentTitle(canvasItem.componentKey) }}</span>
              <span class="item-key">{{ canvasItem.componentKey }}</span>
            </div>

            <div v-if="isButtonComponent(canvasItem.componentKey)" class="button-preview">
              <NButton
                :type="canvasItem.componentKey === 'save-button' || canvasItem.componentKey === 'add-button' ? 'primary' : 'default'"
                size="small"
                @click="showActionMessage(canvasItem.label)"
              >
                {{ canvasItem.label }}
              </NButton>
            </div>

            <div v-else-if="canvasItem.componentKey === 'query-set'" class="query-set-preview">
              <div class="query-set-head">
                <span>查询集字段</span>
                <span>{{ resolveItemFields(canvasItem).length }} 项</span>
              </div>
              <div class="query-field-list">
                <span
                  v-for="queryField in resolveItemFields(canvasItem).slice(0, 8)"
                  :key="queryField.field"
                  class="query-chip"
                >
                  {{ queryField.label || queryField.field }}
                </span>
                <span v-if="resolveItemFields(canvasItem).length > 8" class="query-more">
                  +{{ resolveItemFields(canvasItem).length - 8 }}
                </span>
              </div>
            </div>

            <div v-else-if="canvasItem.componentKey === 'data-table'" class="table-preview">
              <div class="table-head">
                <span
                  v-for="tableField in resolveItemFields(canvasItem).slice(0, 5)"
                  :key="tableField.field"
                >
                  {{ tableField.label || tableField.field }}
                </span>
              </div>
              <div class="table-row">
                <span
                  v-for="rowField in resolveItemFields(canvasItem).slice(0, 5)"
                  :key="rowField.field"
                >
                  {{ resolveSampleValue(rowField) }}
                </span>
              </div>
              <div class="table-foot">
                共 {{ resolveItemFields(canvasItem).length }} 列 · 支持列选择和顺序配置
              </div>
            </div>

            <div v-else-if="canvasItem.componentKey === 'detail-field'" class="detail-preview">
              <span class="detail-label">{{ canvasItem.label }}</span>
              <span class="detail-value">{{ resolveSampleValue(resolveItemField(canvasItem)) }}</span>
            </div>

            <div v-else class="form-control-preview">
              <label :style="{ width: `${Number(canvasItem.style?.labelWidth || 86)}px` }">
                {{ canvasItem.label }}
              </label>
              <div class="control-editor" @mousedown.stop>
                <CanvasPreviewControl
                  v-model:value="demoModel[canvasItem.fieldRef || canvasItem.id]"
                  :field="resolveItemField(canvasItem)"
                  :item="canvasItem"
                  :options="resolveOptions(resolveItemField(canvasItem))"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { NButton, NDatePicker, NInput, NInputNumber, NSelect, NSwitch, NUpload } from 'naive-ui'
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef, watch } from 'vue'
import { getDictData } from '@/composables/useDict'
import {
  createCanvasItem,
  patchZoneCanvas,
  resolveCanvasComponent,
  resolveDefaultFieldComponentKey,
  resolveZoneTitle,
} from './page-schema'

const props = defineProps({
  zone: {
    type: Object,
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  selectedItemId: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:zone', 'selectItem'])

const CanvasPreviewControl = defineComponent({
  name: 'CanvasPreviewControl',
  props: {
    value: {
      type: [String, Number, Boolean, Array, Object],
      default: null,
    },
    field: {
      type: Object,
      default: null,
    },
    item: {
      type: Object,
      required: true,
    },
    options: {
      type: Array,
      default: () => [],
    },
  },
  emits: ['update:value'],
  setup(controlProps, { emit }) {
    const updateValue = value => emit('update:value', value)
    return () => {
      const field = controlProps.field || {}
      const item = controlProps.item || {}
      const label = item.label || field.label || '字段'
      const placeholder = item.props?.placeholder || `请输入${label}`
      const selectPlaceholder = item.props?.placeholder || `请选择${label}`
      const componentKey = item.componentKey || resolveDefaultFieldComponentKey(field)
      const componentType = field.componentType

      if (componentKey === 'field-textarea' || componentType === 'textarea') {
        return h(NInput, {
          value: controlProps.value,
          type: 'textarea',
          rows: 2,
          placeholder,
          onUpdateValue: updateValue,
        })
      }
      if (componentKey === 'field-number' || componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field.dataType)) {
        return h(NInputNumber, {
          value: controlProps.value,
          placeholder,
          showButton: false,
          style: 'width: 100%',
          onUpdateValue: updateValue,
        })
      }
      if (componentKey === 'field-date' || componentType === 'date' || field.dataType === 'date') {
        return h(NDatePicker, {
          value: controlProps.value,
          type: 'date',
          placeholder: selectPlaceholder,
          style: 'width: 100%',
          onUpdateValue: updateValue,
        })
      }
      if (componentKey === 'field-datetime' || componentType === 'datetime' || field.dataType === 'datetime') {
        return h(NDatePicker, {
          value: controlProps.value,
          type: 'datetime',
          placeholder: selectPlaceholder,
          style: 'width: 100%',
          onUpdateValue: updateValue,
        })
      }
      if (componentKey === 'field-switch' || componentType === 'switch') {
        return h(NSwitch, {
          value: controlProps.value,
          checkedValue: 1,
          uncheckedValue: 0,
          onUpdateValue: updateValue,
        })
      }
      if (componentKey === 'field-upload' || componentType === 'fileUpload' || componentType === 'imageUpload') {
        return h(NUpload, {
          defaultUpload: false,
          showFileList: false,
          accept: componentType === 'imageUpload' ? 'image/*' : undefined,
        }, {
          default: () => h(NButton, { size: 'small' }, { default: () => componentType === 'imageUpload' ? '选择图片' : '选择文件' }),
        })
      }
      if (componentKey === 'field-select' || field.dictType || ['select', 'radio', 'checkbox'].includes(componentType)) {
        return h(NSelect, {
          value: controlProps.value,
          options: controlProps.options,
          multiple: componentType === 'checkbox',
          clearable: true,
          filterable: true,
          placeholder: selectPlaceholder,
          onUpdateValue: updateValue,
        })
      }
      return h(NInput, {
        value: controlProps.value,
        placeholder,
        onUpdateValue: updateValue,
      })
    }
  },
})

const scrollRef = ref(null)
const boardRef = ref(null)
const leaferViewRef = ref(null)
const leaferApp = shallowRef(null)
const leaferApi = shallowRef(null)
const liveLayouts = ref({})
const demoModel = reactive({})
const dictOptionsMap = ref({})
const shapeMap = new Map()
let resizeObserver
let transformTimer = null

const fieldMap = computed(() => new Map(props.fields.map(field => [field.field, field])))
const zoneTitle = computed(() => resolveZoneTitle(props.zone?.zoneKey))
const canvas = computed(() => props.zone?.props?.canvas || { width: 1040, height: 460, snap: 8, items: [] })
const canvasItems = computed(() => canvas.value.items || [])
const overlayItems = computed(() => {
  return canvasItems.value
    .map(item => ({
      ...item,
      ...(liveLayouts.value[item.id] || {}),
    }))
    .sort((a, b) => Number(a.zIndex || 0) - Number(b.zIndex || 0))
})
const boardStyle = computed(() => ({
  width: `${canvas.value.width || 1040}px`,
  height: `${canvas.value.height || 460}px`,
}))
const usedFields = computed(() => {
  const refs = new Set()
  canvasItems.value.forEach((item) => {
    if (item.fieldRef)
      refs.add(item.fieldRef)
    ;(item.fieldRefs || item.props?.fieldRefs || []).forEach(ref => refs.add(ref))
  })
  return Array.from(refs).map(ref => fieldMap.value.get(ref)).filter(Boolean)
})
const dictTypes = computed(() => Array.from(new Set(usedFields.value.map(field => field.dictType).filter(Boolean))))

watch(
  () => props.zone?.props?.canvas,
  () => nextTick(renderLeaferItems),
  { deep: true },
)

watch(
  () => props.selectedItemId,
  () => nextTick(syncEditorSelection),
)

watch(usedFields, initDemoModel, { immediate: true })
watch(dictTypes, loadDictOptions, { immediate: true })

onMounted(async () => {
  await initLeafer()
  resizeObserver = new ResizeObserver(() => resizeLeafer())
  if (boardRef.value)
    resizeObserver.observe(boardRef.value)
})

onBeforeUnmount(() => {
  if (transformTimer)
    window.clearTimeout(transformTimer)
  resizeObserver?.disconnect()
  leaferApp.value?.destroy?.()
  leaferApp.value = null
  shapeMap.clear()
})

async function initLeafer() {
  if (!leaferViewRef.value || leaferApp.value)
    return
  const api = await import('leafer-editor')
  leaferApi.value = api
  const app = new api.App({
    view: leaferViewRef.value,
    width: canvas.value.width,
    height: canvas.value.height,
    editor: {
      select: 'tap',
      moveable: true,
      resizeable: true,
      rotateable: false,
      skewable: false,
      lockRatio: false,
      stroke: '#2563eb',
      pointFill: '#ffffff',
      pointSize: 7,
      hover: true,
      hoverStyle: {
        stroke: '#2563eb',
        strokeWidth: 1.5,
      },
    },
  })

  leaferApp.value = app
  app.editor?.on?.(api.EditorEvent.SELECT, handleEditorSelect)
  app.editor?.on?.(api.EditorMoveEvent.MOVE, handleEditorTransform)
  app.editor?.on?.(api.EditorScaleEvent.SCALE, handleEditorTransform)
  renderLeaferItems()
}

function renderLeaferItems() {
  const app = leaferApp.value
  const api = leaferApi.value
  if (!app || !api)
    return

  const layer = app.tree || app
  layer.clear?.()
  shapeMap.clear()

  overlayItems.value.forEach((item) => {
    const rect = new api.Rect({
      id: item.id,
      x: Number(item.x || 0),
      y: Number(item.y || 0),
      width: Number(item.w || 120),
      height: Number(item.h || 48),
      fill: item.id === props.selectedItemId ? '#eff6ff' : item.style?.fill || '#ffffff',
      stroke: item.id === props.selectedItemId ? '#2563eb' : item.style?.stroke || '#cbd5e1',
      strokeWidth: item.id === props.selectedItemId ? 2 : 1,
      cornerRadius: Number(item.style?.radius || 6),
      draggable: !item.locked,
      editable: !item.locked,
      cursor: item.locked ? 'default' : 'move',
      data: { itemId: item.id },
    })
    rect.on(api.PointerEvent.TAP, () => {
      emit('selectItem', item.id)
      app.editor?.select?.(rect)
    })
    rect.on(api.DragEvent.DRAG, () => {
      liveLayouts.value = {
        ...liveLayouts.value,
        [item.id]: readShapeLayout(rect, item),
      }
    })
    rect.on(api.DragEvent.END, () => {
      commitShapeLayout(rect, item)
    })
    shapeMap.set(item.id, rect)
    layer.add(rect)
  })

  resizeLeafer()
  syncEditorSelection()
}

function handleEditorSelect(event) {
  const target = Array.isArray(event.value) ? event.value[0] : event.value
  const itemId = target?.data?.itemId || target?.id
  if (itemId)
    emit('selectItem', itemId)
}

function handleEditorTransform(event) {
  const target = event.target || leaferApp.value?.editor?.getItem?.()
  const itemId = target?.data?.itemId || target?.id
  const item = canvasItems.value.find(canvasItem => canvasItem.id === itemId)
  if (!target || !item)
    return
  liveLayouts.value = {
    ...liveLayouts.value,
    [item.id]: readShapeLayout(target, item),
  }
  if (transformTimer)
    window.clearTimeout(transformTimer)
  transformTimer = window.setTimeout(() => commitShapeLayout(target, item), 120)
}

function syncEditorSelection() {
  const app = leaferApp.value
  if (!app?.editor)
    return
  const shape = props.selectedItemId ? shapeMap.get(props.selectedItemId) : null
  if (shape)
    app.editor.select(shape)
  else
    app.editor.cancel?.()
}

function resizeLeafer() {
  leaferApp.value?.resize?.({
    width: canvas.value.width || 1040,
    height: canvas.value.height || 460,
  })
}

function readShapeLayout(shape, item) {
  const scaleX = Number(shape.scaleX || 1)
  const scaleY = Number(shape.scaleY || 1)
  return {
    x: Math.max(0, snapValue(shape.x || 0)),
    y: Math.max(0, snapValue(shape.y || 0)),
    w: Math.max(48, snapValue((shape.width || item.w || 120) * scaleX)),
    h: Math.max(32, snapValue((shape.height || item.h || 48) * scaleY)),
  }
}

function commitShapeLayout(shape, item) {
  const layout = readShapeLayout(shape, item)
  liveLayouts.value = {
    ...liveLayouts.value,
    [item.id]: layout,
  }
  updateCanvasItems(canvasItems.value.map(canvasItem => canvasItem.id === item.id
    ? { ...canvasItem, ...layout }
    : canvasItem))
  nextTick(() => {
    const nextLayouts = { ...liveLayouts.value }
    delete nextLayouts[item.id]
    liveLayouts.value = nextLayouts
  })
}

function handleDrop(event) {
  const raw = event.dataTransfer.getData('application/x-lowcode-component')
  if (!raw || !props.zone)
    return
  const payload = JSON.parse(raw)
  const rect = boardRef.value.getBoundingClientRect()
  const x = snapValue(event.clientX - rect.left)
  const y = snapValue(event.clientY - rect.top)
  const item = createCanvasItem(payload, {
    zoneKey: props.zone.zoneKey,
    fields: props.fields,
    x: Math.max(0, x),
    y: Math.max(0, y),
    zIndex: resolveNextZIndex(),
  })
  updateCanvasItems([...canvasItems.value, item])
  emit('selectItem', item.id)
}

function updateCanvasSize(key, value) {
  if (!props.zone || !value)
    return
  emit('update:zone', patchZoneCanvas(props.zone, {
    ...canvas.value,
    [key]: Number(value),
  }))
}

function updateCanvasItems(items) {
  if (!props.zone)
    return
  emit('update:zone', patchZoneCanvas(props.zone, {
    ...canvas.value,
    items,
  }))
}

function handleEnabledChange(value) {
  if (!props.zone)
    return
  emit('update:zone', {
    ...props.zone,
    enabled: value,
  })
}

function autoArrange() {
  const nextItems = canvasItems.value.map((item, index) => {
    if (isButtonComponent(item.componentKey)) {
      const buttonIndex = canvasItems.value.slice(0, index).filter(prev => isButtonComponent(prev.componentKey)).length
      return {
        ...item,
        x: 32 + buttonIndex * 116,
        y: 28,
        zIndex: index + 1,
      }
    }
    const fieldIndex = canvasItems.value.slice(0, index).filter(prev => !isButtonComponent(prev.componentKey)).length
    return {
      ...item,
      x: 32 + (fieldIndex % 3) * 308,
      y: 88 + Math.floor(fieldIndex / 3) * 86,
      zIndex: index + 1,
    }
  })
  updateCanvasItems(nextItems)
}

function resolveNextZIndex() {
  return Math.max(0, ...canvasItems.value.map(item => Number(item.zIndex || 0))) + 1
}

function resolveItemStyle(item) {
  return {
    left: `${Number(item.x || 0)}px`,
    top: `${Number(item.y || 0)}px`,
    width: `${Number(item.w || 120)}px`,
    height: `${Number(item.h || 48)}px`,
    zIndex: Number(item.zIndex || 1) + 5,
    borderRadius: `${Number(item.style?.radius || 6)}px`,
  }
}

function resolveComponentTitle(componentKey) {
  return resolveCanvasComponent(componentKey)?.title || componentKey
}

function resolveItemField(item) {
  return fieldMap.value.get(item.fieldRef) || {
    field: item.fieldRef || item.id,
    label: item.label,
    componentType: item.componentKey?.replace('field-', '') || 'input',
  }
}

function resolveItemFields(item) {
  const refs = item.fieldRefs?.length ? item.fieldRefs : item.props?.fieldRefs || []
  if (refs.length)
    return refs.map(ref => fieldMap.value.get(ref)).filter(Boolean)
  if (item.fieldRef && fieldMap.value.get(item.fieldRef))
    return [fieldMap.value.get(item.fieldRef)]
  return []
}

function isButtonComponent(componentKey) {
  return ['import-button', 'export-button', 'custom-query', 'add-button', 'save-button', 'reset-button'].includes(componentKey)
}

function initDemoModel() {
  usedFields.value.forEach((field) => {
    if (demoModel[field.field] !== undefined)
      return
    if (field.componentType === 'checkbox') {
      demoModel[field.field] = []
    }
    else if (field.componentType === 'switch') {
      demoModel[field.field] = 1
    }
    else {
      demoModel[field.field] = null
    }
  })
}

async function loadDictOptions(types) {
  const nextMap = { ...dictOptionsMap.value }
  for (const type of types || []) {
    if (!nextMap[type])
      nextMap[type] = await getDictData(type)
  }
  dictOptionsMap.value = nextMap
}

function resolveOptions(field) {
  if (field?.dictType && dictOptionsMap.value[field.dictType]?.length)
    return dictOptionsMap.value[field.dictType]
  return [
    { label: '选项一', value: '1' },
    { label: '选项二', value: '2' },
  ]
}

function resolveSampleValue(field) {
  if (!field)
    return '示例值'
  if (field.dictType)
    return resolveOptions(field)[0]?.label || '字典值'
  if (field.componentType === 'switch' || field.dataType === 'tinyint')
    return '是'
  if (field.componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field.dataType))
    return field.dataType === 'decimal' ? '128.00' : '128'
  if (field.componentType === 'date' || field.dataType === 'date')
    return '2026-05-20'
  if (field.componentType === 'datetime' || field.dataType === 'datetime')
    return '2026-05-20 09:30:00'
  return field.label ? `${field.label}示例` : '示例数据'
}

function showActionMessage(label) {
  window.$message?.info(`${label}为画布交互预览，发布后接入运行时接口`)
}

function snapValue(value) {
  const snap = Number(canvas.value.snap || 8)
  return Math.round(Number(value || 0) / snap) * snap
}
</script>

<style scoped>
.canvas-designer {
  min-height: 640px;
  display: grid;
  grid-template-rows: 52px minmax(0, 1fr);
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #f8fafc;
  overflow: hidden;
}

.canvas-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 14px;
  background: #ffffff;
  border-bottom: 1px solid #dbe3ee;
}

.canvas-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.canvas-subtitle {
  margin-top: 2px;
  font-size: 11px;
  color: #64748b;
}

.size-input {
  width: 92px;
}

.size-divider {
  color: #94a3b8;
}

.canvas-scroll {
  min-height: 588px;
  overflow: auto;
  padding: 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.8), rgba(241, 245, 249, 0.95)), #f1f5f9;
}

.canvas-board {
  position: relative;
  margin: 0 auto;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.12);
  overflow: hidden;
}

.canvas-grid,
.leafer-layer,
.dom-layer {
  position: absolute;
  inset: 0;
}

.canvas-grid {
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.18) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.18) 1px, transparent 1px);
  background-size: 16px 16px;
  pointer-events: none;
}

.leafer-layer {
  z-index: 1;
}

.dom-layer {
  z-index: 2;
  pointer-events: none;
}

.canvas-dom-item {
  position: absolute;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 9px 10px;
  pointer-events: none;
  color: #0f172a;
  min-width: 0;
}

.canvas-dom-item.selected .item-title {
  color: #1d4ed8;
}

.item-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 16px;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

.item-title span:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-key {
  flex: none;
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  font-weight: 500;
}

.button-preview {
  display: flex;
  align-items: center;
  height: 100%;
  pointer-events: auto;
}

.query-set-preview,
.table-preview {
  display: grid;
  gap: 8px;
  min-height: 0;
}

.query-set-head,
.table-foot {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
  color: #64748b;
}

.query-field-list {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.query-chip,
.query-more {
  max-width: 112px;
  padding: 3px 7px;
  border: 1px solid #dbe3ee;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  border-left: 1px solid #e5e7eb;
  border-top: 1px solid #e5e7eb;
}

.table-head span,
.table-row span {
  min-height: 32px;
  padding: 7px 8px;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
}

.table-head span {
  background: #f8fafc;
  color: #334155;
  font-weight: 700;
}

.table-row span {
  color: #64748b;
}

.detail-preview {
  display: grid;
  grid-template-columns: minmax(72px, 34%) minmax(0, 1fr);
  align-items: center;
  min-height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 5px;
  overflow: hidden;
}

.detail-label,
.detail-value {
  padding: 7px 9px;
  font-size: 12px;
}

.detail-label {
  background: #f8fafc;
  color: #64748b;
}

.detail-value {
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-control-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.form-control-preview label {
  flex: none;
  color: #475569;
  font-size: 12px;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.control-editor {
  flex: 1;
  min-width: 0;
  pointer-events: auto;
}
</style>
