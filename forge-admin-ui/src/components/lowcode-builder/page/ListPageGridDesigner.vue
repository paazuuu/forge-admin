<template>
  <div class="list-grid-designer">
    <aside class="palette-panel">
      <div class="panel-title">
        区块库
      </div>
      <div class="panel-desc">
        拖拽到右侧画布，或点击直接追加
      </div>
      <div class="palette-groups">
        <section v-for="group in groupedBlocks" :key="group.key" class="palette-group">
          <div class="group-title">
            {{ group.title }}
          </div>
          <div class="palette-list">
            <button
              v-for="item in group.items"
              :key="item.blockType"
              class="palette-item"
              type="button"
              draggable="true"
              :disabled="isBlockDisabled(item)"
              :title="isBlockDisabled(item) ? '该区块已存在或不适用当前布局' : item.desc"
              @dragstart="handlePaletteDragStart($event, item)"
              @click="handlePaletteClick(item)"
            >
              <span class="item-title">{{ item.title }}</span>
              <span class="item-desc">{{ item.desc }}</span>
            </button>
          </div>
        </section>
      </div>
    </aside>

    <main class="canvas-panel" @dragover.prevent @drop="handleCanvasDrop">
      <div class="canvas-toolbar">
        <div class="toolbar-info">
          <strong>列表页面 · {{ layoutTitle }}</strong>
          <span>{{ blocks.length }} 个区块 · {{ totalRows }} 行 × 12 列</span>
        </div>
        <n-space size="small">
          <n-button size="small" @click="resetLayout">
            重置默认
          </n-button>
          <n-button
            size="small"
            type="primary"
            secondary
            :disabled="!selectedBlock"
            @click="removeBlock(selectedBlockId)"
          >
            删除选中区块
          </n-button>
        </n-space>
      </div>

      <div class="canvas-scroll">
        <div
          ref="canvasRef"
          class="canvas-grid"
          :style="canvasStyle"
          @click.self="selectedBlockId = null"
        >
          <!-- Grid background -->
          <div
            v-for="row in totalRows"
            :key="`row-${row}`"
            class="grid-row"
            :style="{ top: `${(row - 1) * (rowHeight + gap)}px`, height: `${rowHeight}px` }"
          />
          <div
            v-for="col in 12"
            :key="`col-${col}`"
            class="grid-col"
            :style="{ left: `${(col - 1) * (colWidth + gap)}px`, width: `${colWidth}px` }"
          />

          <!-- Blocks -->
          <div
            v-for="block in blocks"
            :key="block.id"
            class="grid-item"
            :class="{ selected: block.id === selectedBlockId }"
            :style="resolveBlockStyle(block)"
            @mousedown.stop="startMove(block, $event)"
            @click.stop="selectedBlockId = block.id"
          >
            <GridBlockRenderer
              :block="block"
              :fields="fields"
              :selected="block.id === selectedBlockId"
            />
            <div class="resize-handle" @mousedown.stop="startResize(block, $event)" />
          </div>
        </div>
      </div>
    </main>

    <aside class="property-panel">
      <div v-if="!selectedBlock" class="property-empty">
        <p>选中画布上的区块以编辑属性</p>
      </div>
      <div v-else class="property-body">
        <div class="prop-head">
          <div>
            <div class="prop-title">
              {{ selectedBlockMeta?.title || selectedBlock.blockType }}
            </div>
            <div class="prop-meta">
              {{ selectedBlock.gridW }} 列 × {{ selectedBlock.gridH }} 行
            </div>
          </div>
        </div>

        <n-form size="small" label-placement="top" :show-feedback="false">
          <n-form-item label="区块标题">
            <n-input
              :value="selectedBlock.label"
              @update:value="patchBlock(selectedBlock.id, { label: $event })"
            />
          </n-form-item>

          <n-form-item label="网格位置（列 / 行 / 宽 / 高）">
            <div class="grid-pos-editor">
              <div class="pos-cell">
                <span class="pos-label">列 X</span>
                <n-input-number
                  :value="selectedBlock.gridX"
                  :min="0"
                  :max="11"
                  size="small"
                  class="pos-input"
                  @update:value="patchBlock(selectedBlock.id, { gridX: $event ?? 0 })"
                />
              </div>
              <div class="pos-cell">
                <span class="pos-label">行 Y</span>
                <n-input-number
                  :value="selectedBlock.gridY"
                  :min="0"
                  size="small"
                  class="pos-input"
                  @update:value="patchBlock(selectedBlock.id, { gridY: $event ?? 0 })"
                />
              </div>
              <div class="pos-cell">
                <span class="pos-label">宽 W</span>
                <n-input-number
                  :value="selectedBlock.gridW"
                  :min="1"
                  :max="12"
                  size="small"
                  class="pos-input"
                  @update:value="patchBlock(selectedBlock.id, { gridW: $event ?? 1 })"
                />
              </div>
              <div class="pos-cell">
                <span class="pos-label">高 H</span>
                <n-input-number
                  :value="selectedBlock.gridH"
                  :min="1"
                  size="small"
                  class="pos-input"
                  @update:value="patchBlock(selectedBlock.id, { gridH: $event ?? 1 })"
                />
              </div>
            </div>
          </n-form-item>

          <!-- Search / Table 字段配置 -->
          <template v-if="['search-form', 'data-table'].includes(selectedBlock.blockType)">
            <n-form-item :label="selectedBlock.blockType === 'search-form' ? '查询字段' : '展示列'">
              <n-button size="small" type="primary" secondary @click="fieldDrawerOpen = true">
                配置字段（{{ selectedBlock.fieldRefs?.length || 0 }}/{{ fields.length }}）
              </n-button>
            </n-form-item>
            <n-form-item v-if="selectedBlock.blockType === 'search-form'" label="可折叠">
              <n-switch
                :value="!!selectedBlock.props?.collapsible"
                size="small"
                @update:value="patchBlockProps(selectedBlock.id, { collapsible: $event })"
              />
            </n-form-item>
          </template>

          <!-- Toolbar 动作 -->
          <template v-if="selectedBlock.blockType === 'toolbar'">
            <n-form-item label="按钮">
              <n-checkbox-group
                :value="selectedBlock.props?.actions || []"
                @update:value="patchBlockProps(selectedBlock.id, { actions: $event })"
              >
                <n-space vertical size="small">
                  <n-checkbox value="add" label="新增" />
                  <n-checkbox value="import" label="导入" />
                  <n-checkbox value="export" label="导出" />
                  <n-checkbox value="batch-delete" label="批量删除" />
                  <n-checkbox value="custom-query" label="自定义查询" />
                </n-space>
              </n-checkbox-group>
            </n-form-item>
            <n-divider>自定义操作按钮</n-divider>
            <div class="custom-action-editor">
              <div class="custom-action-head">
                <span>名称</span>
                <span>编码</span>
                <span>位置</span>
                <span>样式</span>
                <span>交互</span>
                <span>目标地址</span>
              </div>
              <div
                v-for="(action, idx) in (selectedBlock.props?.customActions || [])"
                :key="action.key || idx"
                class="custom-action-row"
              >
                <n-input
                  :value="action.label"
                  size="small"
                  placeholder="按钮名称"
                  @update:value="updateCustomAction(idx, { label: $event })"
                />
                <n-input
                  :value="action.key"
                  size="small"
                  placeholder="唯一编码"
                  @update:value="updateCustomAction(idx, { key: normalizeActionKey($event) })"
                />
                <n-select
                  :value="action.position || 'toolbar'"
                  size="small"
                  :options="actionPositionOptions"
                  @update:value="updateCustomAction(idx, { position: $event })"
                />
                <n-select
                  :value="action.type || 'default'"
                  size="small"
                  :options="actionTypeOptions"
                  @update:value="updateCustomAction(idx, { type: $event })"
                />
                <n-select
                  :value="action.actionType || 'route'"
                  size="small"
                  :options="actionBehaviorOptions"
                  @update:value="updateCustomAction(idx, { actionType: $event })"
                />
                <n-input
                  :value="action.routePath"
                  size="small"
                  :disabled="(action.actionType || 'route') === 'refresh'"
                  :placeholder="actionPathPlaceholder(action)"
                  @update:value="updateCustomAction(idx, { routePath: $event })"
                />
                <n-button size="tiny" quaternary type="error" @click="removeCustomAction(idx)">
                  删
                </n-button>
                <n-select
                  :value="action.openTarget || '_self'"
                  size="small"
                  :disabled="(action.actionType || 'route') === 'refresh'"
                  :options="actionOpenTargetOptions"
                  @update:value="updateCustomAction(idx, { openTarget: $event })"
                />
                <n-input
                  :value="action.confirmText"
                  size="small"
                  placeholder="确认提示，可用 :id"
                  @update:value="updateCustomAction(idx, { confirmText: $event })"
                />
              </div>
              <n-button size="small" dashed block @click="addCustomAction">
                + 添加自定义按钮
              </n-button>
            </div>
          </template>

          <!-- Tree panel -->
          <template v-if="selectedBlock.blockType === 'tree-panel'">
            <n-form-item label="树标题">
              <n-input
                :value="selectedBlock.props?.treeTitle"
                @update:value="patchBlockProps(selectedBlock.id, { treeTitle: $event })"
              />
            </n-form-item>
            <n-form-item label="父级字段">
              <n-select
                :value="selectedBlock.props?.parentField"
                :options="fieldOptions"
                clearable
                @update:value="patchBlockProps(selectedBlock.id, { parentField: $event })"
              />
            </n-form-item>
            <n-form-item label="显示字段">
              <n-select
                :value="selectedBlock.props?.labelField"
                :options="fieldOptions"
                clearable
                @update:value="patchBlockProps(selectedBlock.id, { labelField: $event })"
              />
            </n-form-item>
          </template>

          <!-- Stats strip -->
          <template v-if="selectedBlock.blockType === 'stats-strip'">
            <n-form-item label="指标项">
              <div class="metrics-editor">
                <div
                  v-for="(metric, idx) in (selectedBlock.props?.metrics || [])"
                  :key="idx"
                  class="metric-row"
                >
                  <n-input
                    :value="metric.label"
                    placeholder="标签"
                    size="small"
                    @update:value="updateMetric(idx, { label: $event })"
                  />
                  <n-input
                    :value="metric.value"
                    placeholder="数值"
                    size="small"
                    @update:value="updateMetric(idx, { value: $event })"
                  />
                  <n-input
                    :value="metric.trend"
                    placeholder="+5%"
                    size="small"
                    @update:value="updateMetric(idx, { trend: $event })"
                  />
                  <n-button size="tiny" quaternary @click="removeMetric(idx)">
                    删
                  </n-button>
                </div>
                <n-button size="small" dashed block @click="addMetric">
                  + 添加指标
                </n-button>
              </div>
            </n-form-item>
          </template>

          <!-- Custom html -->
          <template v-if="selectedBlock.blockType === 'custom-html'">
            <n-form-item label="标题">
              <n-input
                :value="selectedBlock.props?.title"
                @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
              />
            </n-form-item>
            <n-form-item label="正文">
              <n-input
                :value="selectedBlock.props?.content"
                type="textarea"
                :rows="4"
                @update:value="patchBlockProps(selectedBlock.id, { content: $event })"
              />
            </n-form-item>
          </template>

          <!-- Sub table tabs -->
          <template v-if="selectedBlock.blockType === 'sub-table-tabs'">
            <n-form-item label="Tab 项">
              <div class="metrics-editor">
                <div
                  v-for="(tab, idx) in (selectedBlock.props?.tabs || [])"
                  :key="idx"
                  class="metric-row"
                >
                  <n-input
                    :value="tab.title"
                    placeholder="标题"
                    size="small"
                    @update:value="updateTab(idx, { title: $event })"
                  />
                  <n-input
                    :value="tab.key"
                    placeholder="key"
                    size="small"
                    @update:value="updateTab(idx, { key: $event })"
                  />
                  <n-button size="tiny" quaternary @click="removeTab(idx)">
                    删
                  </n-button>
                </div>
                <n-button size="small" dashed block @click="addTab">
                  + 添加 Tab
                </n-button>
              </div>
            </n-form-item>
          </template>

          <!-- Section divider -->
          <template v-if="selectedBlock.blockType === 'section-divider'">
            <n-form-item label="标题">
              <n-input
                :value="selectedBlock.props?.title"
                @update:value="patchBlockProps(selectedBlock.id, { title: $event })"
              />
            </n-form-item>
          </template>
        </n-form>
      </div>
    </aside>

    <n-drawer v-model:show="fieldDrawerOpen" :width="480" placement="right">
      <n-drawer-content :title="`配置字段 · ${selectedBlockMeta?.title || ''}`" closable>
        <div v-if="selectedBlock" class="field-config">
          <div class="field-config-section">
            <div class="section-title">
              已选字段 ({{ selectedBlock.fieldRefs?.length || 0 }})
            </div>
            <draggable
              :model-value="selectedFieldsList"
              item-key="field"
              handle=".f-handle"
              :animation="160"
              class="selected-list"
              @update:model-value="handleSelectedReorder"
            >
              <template #item="{ element }">
                <div class="selected-row">
                  <span class="f-handle">☰</span>
                  <span class="f-name">
                    {{ element.label || element.field }}
                    <small v-if="element.sourceLabel || element.modelName">{{ element.sourceLabel || element.modelName }}</small>
                  </span>
                  <span class="f-code">{{ element.field }}</span>
                  <button type="button" class="f-remove" @click="toggleField(element.field, false)">
                    移除
                  </button>
                </div>
              </template>
            </draggable>
            <div v-if="!(selectedBlock.fieldRefs?.length)" class="empty">
              当前没有选择字段
            </div>
          </div>
          <div class="field-config-section">
            <div class="section-title">
              可选字段
            </div>
            <div class="available-list">
              <button
                v-for="field in availableFields"
                :key="field.field"
                type="button"
                class="available-item"
                @click="toggleField(field.field, true)"
              >
                <span>{{ field.label || field.field }}</span>
                <small v-if="field.sourceLabel || field.modelName">{{ field.sourceLabel || field.modelName }}</small>
              </button>
              <span v-if="!availableFields.length" class="empty">所有字段已选择</span>
            </div>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import draggable from 'vuedraggable'
import GridBlockRenderer from './GridBlockRenderer.vue'
import {
  createDefaultListGridLayout,
  createGridBlock,
  LIST_PAGE_GRID_COLS,
  isPageFieldVisible,
  listPageBlockCatalog,
  resolveListPageBlockMeta,
  syncGridLayoutWithModel,
} from './page-schema'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  modelSchema: {
    type: Object,
    default: () => ({}),
  },
  layoutType: {
    type: String,
    default: 'simple-crud',
  },
})

const emit = defineEmits(['update:modelValue'])

const colWidth = 78
const rowHeight = 32
const gap = 8
const actionPositionOptions = [
  { label: '工具栏', value: 'toolbar' },
  { label: '行操作列', value: 'row' },
]
const actionTypeOptions = [
  { label: '默认', value: 'default' },
  { label: '主要', value: 'primary' },
  { label: '信息', value: 'info' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '危险', value: 'error' },
]
const actionBehaviorOptions = [
  { label: '站内跳转', value: 'route' },
  { label: '外部链接', value: 'external' },
  { label: '刷新列表', value: 'refresh' },
]
const actionOpenTargetOptions = [
  { label: '当前页', value: '_self' },
  { label: '新窗口', value: '_blank' },
]

const canvasRef = ref(null)
const selectedBlockId = ref(null)
const fieldDrawerOpen = ref(false)

const localLayout = ref(syncGridLayoutWithModel(
  props.modelValue || createDefaultListGridLayout(props.modelSchema, { layoutType: props.layoutType }),
  props.modelSchema,
))

const blocks = computed(() => localLayout.value.items || [])
const totalRows = computed(() => {
  const max = blocks.value.reduce((acc, b) => Math.max(acc, b.gridY + b.gridH), 0)
  return Math.max(20, max + 4)
})
const canvasStyle = computed(() => ({
  width: `${LIST_PAGE_GRID_COLS * colWidth + (LIST_PAGE_GRID_COLS - 1) * gap + 24}px`,
  minHeight: `${totalRows.value * (rowHeight + gap) + 24}px`,
}))

const selectedBlock = computed(() => blocks.value.find(b => b.id === selectedBlockId.value) || null)
const layoutTitle = computed(() => {
  if (props.layoutType === 'tree-crud')
    return '左树右表'
  if (props.layoutType === 'master-detail-crud')
    return '主子表'
  return '标准单表'
})
const selectedBlockMeta = computed(() => selectedBlock.value ? resolveListPageBlockMeta(selectedBlock.value.blockType) : null)

const fieldOptions = computed(() => props.fields
  .filter(field => isPageFieldVisible(field, 'table'))
  .map(f => ({
  label: f.sourceLabel || f.modelName ? `${f.label || f.field} · ${f.sourceLabel || f.modelName}` : (f.label ? `${f.label}（${f.field}）` : f.field),
  value: f.field,
})))

const groupedBlocks = computed(() => {
  const groups = [
    { key: 'data', title: '数据区块', items: [] },
    { key: 'action', title: '动作区块', items: [] },
    { key: 'extra', title: '辅助区块', items: [] },
  ]
  for (const item of listPageBlockCatalog) {
    const target = groups.find(g => g.key === item.group) || groups[2]
    target.items.push(item)
  }
  return groups
})

const fieldMap = computed(() => new Map(props.fields.map(f => [f.field, f])))
const selectedFieldsList = computed(() => (selectedBlock.value?.fieldRefs || [])
  .map(ref => fieldMap.value.get(ref))
  .filter(Boolean))
const availableFields = computed(() => {
  const set = new Set(selectedBlock.value?.fieldRefs || [])
  return props.fields.filter(f => isPageFieldVisible(f, selectedBlockZoneKey.value) && !set.has(f.field))
})
const selectedBlockZoneKey = computed(() => selectedBlock.value?.blockType === 'search-form' ? 'search' : 'table')

watch(
  () => props.modelValue,
  (value) => {
    const next = syncGridLayoutWithModel(value, props.modelSchema)
    if (JSON.stringify(next) !== JSON.stringify(localLayout.value))
      localLayout.value = next
  },
  { deep: true },
)

watch(
  () => props.modelSchema,
  () => {
    localLayout.value = syncGridLayoutWithModel(localLayout.value, props.modelSchema)
  },
  { deep: true },
)

watch(
  localLayout,
  (value) => {
    emit('update:modelValue', JSON.parse(JSON.stringify(value)))
  },
  { deep: true },
)

function resolveBlockStyle(block) {
  return {
    left: `${block.gridX * (colWidth + gap)}px`,
    top: `${block.gridY * (rowHeight + gap)}px`,
    width: `${block.gridW * colWidth + (block.gridW - 1) * gap}px`,
    height: `${block.gridH * rowHeight + (block.gridH - 1) * gap}px`,
  }
}

function isBlockDisabled(item) {
  if (item.unique && blocks.value.some(b => b.blockType === item.blockType))
    return true
  if (item.onlyFor && !item.onlyFor.includes(props.layoutType))
    return true
  return false
}

function handlePaletteDragStart(event, item) {
  if (isBlockDisabled(item)) {
    event.preventDefault()
    return
  }
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/x-list-block', item.blockType)
}

function handlePaletteClick(item) {
  if (isBlockDisabled(item))
    return
  appendBlock(item.blockType)
}

function handleCanvasDrop(event) {
  const blockType = event.dataTransfer?.getData('application/x-list-block')
  if (!blockType)
    return
  const meta = resolveListPageBlockMeta(blockType)
  if (!meta)
    return
  const cell = pixelToCell(event.clientX, event.clientY)
  appendBlock(blockType, cell)
}

function appendBlock(blockType, position) {
  const meta = resolveListPageBlockMeta(blockType)
  if (!meta)
    return
  const cell = position || { gridX: 0, gridY: nextFreeRow() }
  const block = createGridBlock(blockType, props.modelSchema, cell)
  if (!block)
    return
  if (block.gridX + block.gridW > LIST_PAGE_GRID_COLS)
    block.gridX = Math.max(0, LIST_PAGE_GRID_COLS - block.gridW)
  localLayout.value = {
    ...localLayout.value,
    items: [...blocks.value, block],
  }
  selectedBlockId.value = block.id
}

function nextFreeRow() {
  return blocks.value.reduce((acc, b) => Math.max(acc, b.gridY + b.gridH), 0)
}

function pixelToCell(clientX, clientY) {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect)
    return { gridX: 0, gridY: 0 }
  const localX = clientX - rect.left - 12
  const localY = clientY - rect.top - 12
  return {
    gridX: Math.max(0, Math.min(11, Math.floor(localX / (colWidth + gap)))),
    gridY: Math.max(0, Math.floor(localY / (rowHeight + gap))),
  }
}

function patchBlock(id, patch) {
  localLayout.value = {
    ...localLayout.value,
    items: blocks.value.map(b => b.id === id
      ? { ...b, ...patch, gridX: clamp(patch.gridX ?? b.gridX, 0, 11), gridW: clamp(patch.gridW ?? b.gridW, 1, 12) }
      : b),
  }
}

function patchBlockProps(id, patch) {
  localLayout.value = {
    ...localLayout.value,
    items: blocks.value.map(b => b.id === id
      ? { ...b, props: { ...(b.props || {}), ...patch } }
      : b),
  }
}

function removeBlock(id) {
  if (!id)
    return
  localLayout.value = {
    ...localLayout.value,
    items: blocks.value.filter(b => b.id !== id),
  }
  if (selectedBlockId.value === id)
    selectedBlockId.value = null
}

function resetLayout() {
  localLayout.value = createDefaultListGridLayout(props.modelSchema, { layoutType: props.layoutType })
  selectedBlockId.value = null
}

function clamp(value, min, max) {
  const n = Number(value)
  if (!Number.isFinite(n))
    return min
  return Math.min(Math.max(n, min), max)
}

// 拖动移动
let moveCtx = null
function startMove(block, event) {
  if (event.button !== 0)
    return
  selectedBlockId.value = block.id
  moveCtx = {
    blockId: block.id,
    startX: event.clientX,
    startY: event.clientY,
    originX: block.gridX,
    originY: block.gridY,
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', endMove)
}
function onMove(event) {
  if (!moveCtx)
    return
  const dx = Math.round((event.clientX - moveCtx.startX) / (colWidth + gap))
  const dy = Math.round((event.clientY - moveCtx.startY) / (rowHeight + gap))
  const block = blocks.value.find(b => b.id === moveCtx.blockId)
  if (!block)
    return
  const nextX = clamp(moveCtx.originX + dx, 0, 12 - block.gridW)
  const nextY = Math.max(0, moveCtx.originY + dy)
  if (block.gridX !== nextX || block.gridY !== nextY)
    patchBlock(block.id, { gridX: nextX, gridY: nextY })
}
function endMove() {
  moveCtx = null
  window.removeEventListener('mousemove', onMove)
  window.removeEventListener('mouseup', endMove)
}

// Resize
let resizeCtx = null
function startResize(block, event) {
  resizeCtx = {
    blockId: block.id,
    startX: event.clientX,
    startY: event.clientY,
    originW: block.gridW,
    originH: block.gridH,
  }
  selectedBlockId.value = block.id
  window.addEventListener('mousemove', onResize)
  window.addEventListener('mouseup', endResize)
}
function onResize(event) {
  if (!resizeCtx)
    return
  const dw = Math.round((event.clientX - resizeCtx.startX) / (colWidth + gap))
  const dh = Math.round((event.clientY - resizeCtx.startY) / (rowHeight + gap))
  const block = blocks.value.find(b => b.id === resizeCtx.blockId)
  if (!block)
    return
  const nextW = clamp(resizeCtx.originW + dw, 1, 12 - block.gridX)
  const nextH = Math.max(1, resizeCtx.originH + dh)
  if (block.gridW !== nextW || block.gridH !== nextH)
    patchBlock(block.id, { gridW: nextW, gridH: nextH })
}
function endResize() {
  resizeCtx = null
  window.removeEventListener('mousemove', onResize)
  window.removeEventListener('mouseup', endResize)
}

onBeforeUnmount(() => {
  endMove()
  endResize()
})

// Field config drawer
function toggleField(fieldName, add) {
  if (!selectedBlock.value)
    return
  const current = selectedBlock.value.fieldRefs || []
  const next = add
    ? [...current, fieldName]
    : current.filter(f => f !== fieldName)
  patchBlock(selectedBlock.value.id, { fieldRefs: Array.from(new Set(next)) })
}

function handleSelectedReorder(rows) {
  if (!selectedBlock.value)
    return
  patchBlock(selectedBlock.value.id, { fieldRefs: rows.map(r => r.field) })
}

// Metric editing
function updateMetric(idx, patch) {
  const list = [...(selectedBlock.value?.props?.metrics || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { metrics: list })
}
function addMetric() {
  const list = [...(selectedBlock.value?.props?.metrics || []), { label: '指标', value: '0', trend: '' }]
  patchBlockProps(selectedBlock.value.id, { metrics: list })
}
function removeMetric(idx) {
  const list = [...(selectedBlock.value?.props?.metrics || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { metrics: list })
}

function addCustomAction() {
  const list = [...(selectedBlock.value?.props?.customActions || [])]
  list.push({
    key: `custom_${Date.now()}`,
    label: '自定义按钮',
    position: 'row',
    type: 'primary',
    actionType: 'route',
    routePath: '',
    openTarget: '_self',
  })
  patchBlockProps(selectedBlock.value.id, { customActions: list })
}

function updateCustomAction(idx, patch) {
  const list = [...(selectedBlock.value?.props?.customActions || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { customActions: list })
}

function removeCustomAction(idx) {
  const list = [...(selectedBlock.value?.props?.customActions || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { customActions: list })
}

function normalizeActionKey(value) {
  return String(value || '')
    .trim()
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W/g, '_')
    .replace(/_+/g, '_')
    .toLowerCase()
    .replace(/^[^a-z]+/, '')
}

function actionPathPlaceholder(action = {}) {
  if ((action.actionType || 'route') === 'external')
    return 'https://example.com/:id'
  if ((action.actionType || 'route') === 'refresh')
    return '刷新列表无需地址'
  return '/ai/xxx/:id'
}

// Tab editing
function updateTab(idx, patch) {
  const list = [...(selectedBlock.value?.props?.tabs || [])]
  list[idx] = { ...list[idx], ...patch }
  patchBlockProps(selectedBlock.value.id, { tabs: list })
}
function addTab() {
  const list = [...(selectedBlock.value?.props?.tabs || []), { key: `tab_${Date.now()}`, title: '新 Tab' }]
  patchBlockProps(selectedBlock.value.id, { tabs: list })
}
function removeTab(idx) {
  const list = [...(selectedBlock.value?.props?.tabs || [])]
  list.splice(idx, 1)
  patchBlockProps(selectedBlock.value.id, { tabs: list })
}
</script>

<style scoped>
.list-grid-designer {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 320px;
  gap: 12px;
  min-height: 704px;
}

.palette-panel,
.canvas-panel,
.property-panel {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.palette-panel {
  padding: 12px;
}

.panel-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.panel-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.palette-groups {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.group-title {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
}

.palette-list {
  display: grid;
  gap: 6px;
}

.palette-item {
  display: grid;
  gap: 2px;
  padding: 8px 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
  text-align: left;
  cursor: grab;
  transition: all 0.15s;
}

.palette-item:hover:not(:disabled) {
  border-color: #2563eb;
  background: #eff6ff;
}

.palette-item:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.item-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.item-desc {
  font-size: 11px;
  color: #64748b;
}

.canvas-panel {
  background: #f3f6fa;
  min-width: 0;
}

.canvas-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.canvas-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.toolbar-info strong {
  font-size: 13px;
  color: #0f172a;
}

.toolbar-info span {
  margin-left: 8px;
  font-size: 12px;
  color: #64748b;
}

.canvas-grid {
  position: relative;
  margin: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 6px;
}

.grid-row,
.grid-col {
  position: absolute;
  pointer-events: none;
}

.grid-row {
  left: 12px;
  right: 12px;
  border-bottom: 1px dashed #eef2f7;
}

.grid-col {
  top: 12px;
  bottom: 12px;
  border-right: 1px dashed #eef2f7;
}

.grid-item {
  position: absolute;
  cursor: move;
  transition: box-shadow 0.15s;
}

.grid-item.selected {
  z-index: 10;
}

.resize-handle {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 16px;
  height: 16px;
  cursor: nwse-resize;
  background: linear-gradient(
    135deg,
    transparent 0 50%,
    #94a3b8 50% 60%,
    transparent 60% 70%,
    #94a3b8 70% 80%,
    transparent 80%
  );
  border-bottom-right-radius: 8px;
}

.property-panel {
  padding: 14px;
  overflow-y: auto;
}

.property-empty {
  display: grid;
  place-items: center;
  height: 100%;
  color: #94a3b8;
  font-size: 13px;
}

.prop-head {
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5e7eb;
}

.prop-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.prop-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.grid-pos-editor {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.pos-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.pos-label {
  font-size: 11px;
  color: #64748b;
}

.pos-input {
  width: 100%;
}

.pos-input :deep(.n-input-number) {
  width: 100%;
}

.pos-input :deep(.n-input__input-el) {
  text-align: center;
}

.metrics-editor {
  display: grid;
  gap: 6px;
}

.custom-action-editor {
  display: grid;
  gap: 6px;
}

.custom-action-head {
  display: grid;
  grid-template-columns: minmax(70px, 1fr) minmax(78px, 1fr) 86px 76px 92px minmax(120px, 1.4fr) auto;
  gap: 4px;
  color: #64748b;
  font-size: 11px;
}

.custom-action-row {
  display: grid;
  grid-template-columns: minmax(70px, 1fr) minmax(78px, 1fr) 86px 76px 92px minmax(120px, 1.4fr) auto;
  gap: 4px;
  align-items: center;
}

.custom-action-row > :nth-child(8) {
  grid-column: 1 / 3;
}

.custom-action-row > :nth-child(9) {
  grid-column: 3 / 8;
}

.metric-row {
  display: grid;
  grid-template-columns: 1fr 1fr 80px auto;
  gap: 4px;
  align-items: center;
}

/* Field drawer */
.field-config {
  display: grid;
  gap: 16px;
}

.field-config-section .section-title {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 8px;
}

.selected-list {
  display: grid;
  gap: 6px;
  max-height: 360px;
  overflow-y: auto;
}

.selected-row {
  display: grid;
  grid-template-columns: 16px minmax(120px, 1fr) minmax(80px, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
}

.f-handle {
  color: #94a3b8;
  cursor: grab;
}

.f-name {
  display: grid;
  gap: 2px;
  color: #0f172a;
  font-weight: 600;
}

.f-name small,
.available-item small {
  color: #64748b;
  font-size: 11px;
  font-weight: 400;
}

.f-code {
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.f-remove {
  border: 0;
  background: transparent;
  color: #ef4444;
  font-size: 12px;
  cursor: pointer;
}

.available-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 220px;
  overflow-y: auto;
}

.available-item {
  display: inline-grid;
  gap: 2px;
  padding: 4px 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
  color: #2563eb;
  font-size: 12px;
  cursor: pointer;
}

.available-item:hover {
  border-color: #2563eb;
  background: #eff6ff;
}

.empty {
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 1280px) {
  .list-grid-designer {
    grid-template-columns: 220px minmax(0, 1fr) 280px;
  }
}
</style>
