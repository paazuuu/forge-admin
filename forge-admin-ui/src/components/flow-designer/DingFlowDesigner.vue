<script setup>
/**
 * DingFlowDesigner — 钉钉样式流程设计器主组件
 *
 * 对外接口与原 FlowModeler.vue 1:1 兼容：
 *   Props: xml / readonly
 *   Events: change(xml) / ready / importStart(@import-start) / importEnd(@import-end)
 *   Methods: setXML / getXML / reset / undo / redo
 *
 * pitfalls #7 防回环：v-model 风格父组件回写 props.xml 时，比对 lastEmittedXml 跳过。
 * pitfalls #8 BPMNPlane 修复：JSON→XML 时 convertJsonToBpmn 总写真实 process id。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  AddNodeButton,
  BranchAddButton,
  BranchHeader,
  EdgeLayer,
  FlowCanvas,
  layoutFlow,
  MergeNode,
  NodeContextMenu,
  NodeRenderer,
} from './canvas/index.js'
import { useFlowDesigner, useFlowHistory } from './composables/index.js'
import { convertBpmnToJson, convertJsonToBpmn } from './converter/index.js'
import { NodeConfigDrawer } from './panel/index.js'

const props = defineProps({
  xml: { type: String, default: '' },
  readonly: { type: Boolean, default: false },
  formFieldCatalog: { type: Array, default: () => [] },
  processConfig: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['change', 'ready', 'importStart', 'importEnd'])

const designer = useFlowDesigner()
const history = useFlowHistory(designer.flowJson, { maxStack: 50 })
const canvasRef = ref(null)
const isImporting = ref(false)
const lastEmittedXml = ref('')

const drawerVisible = ref(false)
const drawerNodeId = ref(null)
const drawerFocusEdgeId = ref(null)
const drawerNode = computed(() => designer.getNode(drawerNodeId.value))
const drawerOutgoingEdges = computed(() => drawerNodeId.value ? designer.getOutgoingEdges(drawerNodeId.value) : [])

const contextMenu = ref({ visible: false, position: { x: 0, y: 0 }, node: null })

// layoutResult 用 computed 同步求值，避免 ref+watch 异步时序导致新增节点
// 先用旧位置渲染（position=undefined → 节点堆叠在左上角）的问题
const layoutResult = computed(() => layoutFlow(designer.flowJson.value))

watch(
  () => props.xml,
  async (xml) => {
    if (xml && xml === lastEmittedXml.value)
      return
    if (xml)
      await importXml(xml)
    else
      designer.reset()
  },
  { immediate: true },
)

watch(
  () => props.processConfig,
  (config) => {
    applyProcessConfig(config)
  },
  { immediate: true, deep: true },
)

function normalizeProcessConfig(config = {}) {
  const mode = ['firstOnly', 'consecutive', 'none'].includes(config.autoApprovalMode)
    ? config.autoApprovalMode
    : 'none'
  return {
    allowSubmitterWithdraw: config.allowSubmitterWithdraw !== false,
    autoApprovalMode: mode,
  }
}

function applyProcessConfig(config = {}) {
  const normalized = normalizeProcessConfig(config)
  const current = designer.flowJson.value.config || {}
  if (
    current.allowSubmitterWithdraw === normalized.allowSubmitterWithdraw
    && current.autoApprovalMode === normalized.autoApprovalMode
  ) {
    return
  }
  designer.flowJson.value = {
    ...designer.flowJson.value,
    config: {
      ...current,
      ...normalized,
    },
  }
}

async function importXml(xml) {
  isImporting.value = true
  emit('importStart')
  try {
    const json = convertBpmnToJson(xml)
    designer.loadJson(json)
    history.clear()
    await nextTick()
    autoFitToScreen()
    emit('importEnd', true, null)
  }
  catch (err) {
    console.error('[DingFlowDesigner] importXml failed', err)
    emit('importEnd', false, err)
  }
  finally {
    isImporting.value = false
  }
}

let emitTimer = null
function scheduleEmit() {
  if (props.readonly || isImporting.value)
    return
  clearTimeout(emitTimer)
  emitTimer = setTimeout(() => {
    try {
      const xml = convertJsonToBpmn(designer.flowJson.value)
      lastEmittedXml.value = xml
      emit('change', xml)
    }
    catch (err) {
      console.warn('[DingFlowDesigner] convertJsonToBpmn failed', err)
    }
  }, 200)
}

watch(designer.flowJson, scheduleEmit)

function handleNodeClick(node, options = {}) {
  if (!node)
    return
  designer.selectedNodeId.value = node.id
  drawerNodeId.value = node.id
  drawerFocusEdgeId.value = options.focusEdgeId || null
  drawerVisible.value = true
}

function handleBranchHeaderClick(edge) {
  const gatewayNode = designer.getNode(edge.source)
  handleNodeClick(gatewayNode, { focusEdgeId: edge.id })
}

function handleDrawerVisibleUpdate(visible) {
  drawerVisible.value = visible
  if (!visible)
    drawerFocusEdgeId.value = null
}

function handleNodeDelete(node) {
  if (props.readonly)
    return
  history.snapshot()
  try {
    designer.deleteNode(node.id)
  }
  catch (e) {
    console.warn(e)
  }
}

function handleContextMenu({ event, node }) {
  contextMenu.value = {
    visible: true,
    position: { x: event.clientX, y: event.clientY },
    node,
  }
}

function handleContextAction(actionId, node) {
  contextMenu.value.visible = false
  if (!node)
    return
  switch (actionId) {
    case 'edit':
      handleNodeClick(node)
      break
    case 'copy':
      history.snapshot()
      try {
        designer.copyNode(node.id)
      }
      catch (e) {
        console.warn(e)
      }
      break
    case 'move-up':
      history.snapshot()
      try {
        designer.moveNodeUp(node.id)
      }
      catch (e) {
        console.warn(e)
      }
      break
    case 'move-down':
      history.snapshot()
      try {
        designer.moveNodeDown(node.id)
      }
      catch (e) {
        console.warn(e)
      }
      break
    case 'delete':
      handleNodeDelete(node)
      break
  }
}

function handleAddAfter(afterNodeId, type) {
  if (props.readonly)
    return
  history.snapshot()
  try {
    const newId = designer.addNode(afterNodeId, type)
    designer.selectedNodeId.value = newId
    drawerNodeId.value = newId
    drawerFocusEdgeId.value = null
    drawerVisible.value = true
  }
  catch (e) {
    console.warn(e)
  }
}

function handleDrawerSave(patch, nodeId) {
  history.snapshot()
  try {
    designer.updateNode(nodeId, patch)
  }
  catch (e) {
    console.warn(e)
  }
}

function handleDrawerEdgeUpdate(edgeId, patch) {
  try {
    designer.updateEdge(edgeId, patch)
  }
  catch (e) {
    console.warn(e)
  }
}

function handleAddBranch(gatewayId) {
  if (props.readonly)
    return
  history.snapshot()
  try {
    const result = designer.addBranch(gatewayId)
    designer.selectedNodeId.value = gatewayId
    drawerNodeId.value = gatewayId
    drawerFocusEdgeId.value = result.edgeId
    drawerVisible.value = true
  }
  catch (e) {
    console.warn(e)
  }
}

const addButtonPositions = computed(() => {
  const list = []
  for (const node of designer.flowJson.value.nodes) {
    if (['condition', 'parallel', 'inclusive', 'end'].includes(node.nodeType))
      continue
    const pos = layoutResult.value.nodePositions.get(node.id)
    if (!pos)
      continue
    list.push({
      id: node.id,
      position: { x: pos.x + pos.width / 2, y: pos.y + pos.height + 25 },
    })
  }
  return list
})

const branchHeaders = computed(() => {
  const out = []
  for (const node of designer.flowJson.value.nodes) {
    if (!['condition', 'parallel', 'inclusive'].includes(node.nodeType))
      continue
    const gwPos = layoutResult.value.nodePositions.get(node.id)
    if (!gwPos)
      continue
    for (const edge of designer.getOutgoingEdges(node.id)) {
      const targetPos = layoutResult.value.nodePositions.get(edge.target)
      if (!targetPos)
        continue
      out.push({
        edge,
        position: {
          x: targetPos.x + targetPos.width / 2,
          y: (gwPos.y + gwPos.height + targetPos.y) / 2,
        },
      })
    }
  }
  return out
})

const branchAddButtons = computed(() => {
  const out = []
  for (const node of designer.flowJson.value.nodes) {
    if (node.nodeType !== 'condition')
      continue
    const gwPos = layoutResult.value.nodePositions.get(node.id)
    if (!gwPos)
      continue
    const outgoing = designer.getOutgoingEdges(node.id)
    const targetPositions = outgoing
      .map(edge => layoutResult.value.nodePositions.get(edge.target))
      .filter(Boolean)
    if (!targetPositions.length)
      continue
    out.push({
      gatewayId: node.id,
      position: {
        x: gwPos.x + gwPos.width / 2 + 52,
        y: gwPos.y + gwPos.height + 14,
      },
    })
  }
  return out
})

const mergeMarkers = computed(() => {
  const out = []
  for (const node of designer.flowJson.value.nodes) {
    if (!node.config?.mergeNode)
      continue
    const pos = layoutResult.value.nodePositions.get(node.id)
    if (!pos)
      continue
    out.push({ id: node.id, position: { x: pos.x + pos.width / 2, y: pos.y - 12 } })
  }
  return out
})

async function setXML(xml) {
  await importXml(xml)
}

function getXML(_formatted = false) {
  return convertJsonToBpmn({
    ...designer.flowJson.value,
    config: {
      ...(designer.flowJson.value.config || {}),
      ...normalizeProcessConfig(props.processConfig),
    },
  })
}

function reset() {
  designer.reset()
  history.clear()
}

function undo() {
  history.undo()
}

function redo() {
  history.redo()
}

defineExpose({
  setXML,
  getXML,
  reset,
  undo,
  redo,
  designer,
  history,
})

/** 自动居中：把画布内容 fit 到视口中央 */
function autoFitToScreen() {
  const canvas = canvasRef.value
  if (!canvas?.containerRef)
    return
  const container = canvas.containerRef
  const b = layoutResult.value.canvasBounds
  if (!b || b.maxX <= b.minX || b.maxY <= b.minY)
    return
  const contentW = b.maxX - b.minX
  const contentH = b.maxY - b.minY
  canvas.fitToScreen(contentW, contentH, container.clientWidth, container.clientHeight, 72, b.minX, b.minY)
}

onMounted(async () => {
  await nextTick()
  autoFitToScreen()
  emit('ready')
})

onBeforeUnmount(() => {
  clearTimeout(emitTimer)
})
</script>

<template>
  <div class="ding-flow-designer relative h-full w-full bg-white">
    <FlowCanvas ref="canvasRef" :readonly="readonly">
      <template #edges>
        <EdgeLayer
          :edges="designer.flowJson.value.edges"
          :paths="layoutResult.edgePaths"
          :canvas-bounds="layoutResult.canvasBounds"
        />
      </template>
      <template #nodes>
        <NodeRenderer
          v-for="node in designer.flowJson.value.nodes"
          :key="node.id"
          :node="node"
          :position="layoutResult.nodePositions.get(node.id)"
          :selected="designer.selectedNodeId.value === node.id"
          :readonly="readonly"
          :outgoing-count="designer.getOutgoingEdges(node.id).length"
          @click="handleNodeClick"
          @delete="handleNodeDelete"
          @context-menu="handleContextMenu"
        />

        <AddNodeButton
          v-for="btn in addButtonPositions"
          :key="`add-${btn.id}`"
          :position="btn.position"
          :readonly="readonly"
          @select="(type) => handleAddAfter(btn.id, type)"
        />

        <BranchHeader
          v-for="(bh, idx) in branchHeaders"
          :key="`bh-${bh.edge.id}-${idx}`"
          :edge="bh.edge"
          :position="bh.position"
          @click="handleBranchHeaderClick"
        />

        <BranchAddButton
          v-for="btn in branchAddButtons"
          :key="`branch-add-${btn.gatewayId}`"
          :position="btn.position"
          :readonly="readonly"
          @click="handleAddBranch(btn.gatewayId)"
        />

        <MergeNode
          v-for="m in mergeMarkers"
          :key="`merge-${m.id}`"
          :position="m.position"
        />
      </template>
    </FlowCanvas>

    <NodeContextMenu
      :visible="contextMenu.visible"
      :position="contextMenu.position"
      :node="contextMenu.node"
      :readonly="readonly"
      @close="contextMenu.visible = false"
      @action="handleContextAction"
    />

    <NodeConfigDrawer
      :visible="drawerVisible"
      :node="drawerNode"
      :outgoing-edges="drawerOutgoingEdges"
      :nodes="designer.flowJson.value.nodes"
      :form-field-catalog="formFieldCatalog"
      :focus-edge-id="drawerFocusEdgeId"
      :readonly="readonly"
      @update:visible="handleDrawerVisibleUpdate"
      @save="handleDrawerSave"
      @update:edge="handleDrawerEdgeUpdate"
    />
  </div>
</template>
