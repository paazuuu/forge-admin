<script setup>
/**
 * NodeConfigDrawer — 节点配置抽屉容器
 *
 * 职责：
 * - 右侧滑出抽屉（Naive UI NDrawer）
 * - 顶部：轻量节点信息 + 关闭按钮
 * - 中部：根据 nodeType 调度对应配置 Tab（通过 panel/index 注册的 RENDERER_MAP）
 * - 底部：取消 / 保存按钮（保存时触发 emit('save', patch)）
 *
 * Props:
 *   - visible        v-model:visible
 *   - node           当前编辑节点（外层 useFlowDesigner.getNode 取）
 *   - outgoingEdges  当前节点出边，网关配置分支条件时使用
 *   - nodes          当前流程节点数组，用于网关配置显示下游节点名称
 *   - readonly       只读模式（查看器场景）
 *   - width          抽屉宽度，默认 520
 *
 * Events:
 *   - update:visible (boolean)
 *   - save           (patch: object)  外层 useFlowDesigner.updateNode(node.id, patch)
 *   - update:edge    (edgeId, patch)  外层 useFlowDesigner.updateEdge(edgeId, patch)
 *
 * 实现策略：
 * - 内部维护 draftNode（深拷贝 props.node），用户修改不直接影响外层；点击 "保存" 才 emit
 * - 取消按钮 / 关闭按钮：丢弃 draft，emit('update:visible', false)
 * - 节点 name 编辑通过 BasicConfig 组件；审批人节点为了贴近配置页签体验，基础属性内嵌到“审批人设置”
 */
import { computed, ref, watch } from 'vue'
import BasicConfig from '../panel/BasicConfig.vue'
import { CONFIG_RENDERER_MAP } from '../panel/config-renderer-map.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  node: { type: Object, default: null },
  outgoingEdges: { type: Array, default: () => [] },
  nodes: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false },
  width: { type: Number, default: 520 },
})

const emit = defineEmits(['update:visible', 'save', 'update:edge'])

const draftNode = ref(null)

watch(
  () => [props.visible, props.node],
  ([v, n]) => {
    if (v && n)
      draftNode.value = cloneDeep(n)
    else if (!v)
      draftNode.value = null
  },
  { immediate: true },
)

const ConfigComponent = computed(() => {
  if (!draftNode.value)
    return null
  return CONFIG_RENDERER_MAP[draftNode.value.nodeType] || null
})

const configExtraProps = computed(() => {
  if (!['condition', 'parallel', 'inclusive'].includes(draftNode.value?.nodeType))
    return {}
  return {
    outgoingEdges: props.outgoingEdges,
    nodes: props.nodes,
  }
})

const useIntegratedBasicConfig = computed(() => draftNode.value?.nodeType === 'approver')

const headerIcon = computed(() => {
  const map = {
    start: 'i-mdi-flag-variant-outline',
    end: 'i-mdi-flag-checkered',
    approver: 'i-mdi-account-check',
    carbonCopy: 'i-mdi-email-outline',
    condition: 'i-mdi-source-branch',
    parallel: 'i-mdi-call-split',
    inclusive: 'i-mdi-set-merge',
    service: 'i-mdi-cog-outline',
    script: 'i-mdi-code-tags',
    subProcess: 'i-mdi-sitemap-outline',
    callActivity: 'i-mdi-phone-forward-outline',
    advanced: 'i-mdi-shield-alert-outline',
  }
  return map[draftNode.value?.nodeType] || 'i-mdi-square-outline'
})

function handleClose() {
  emit('update:visible', false)
}

function handleSave() {
  if (!draftNode.value)
    return
  const patch = {
    name: draftNode.value.name,
    config: draftNode.value.config,
  }
  emit('save', patch, draftNode.value.id)
  emit('update:visible', false)
}

function updateConfig(partial) {
  if (!draftNode.value)
    return
  draftNode.value.config = { ...draftNode.value.config, ...partial }
}

function updateNode(nextNode) {
  draftNode.value = nextNode
}

function updateEdge(edgeId, patch) {
  emit('update:edge', edgeId, patch)
}

function cloneDeep(v) {
  return JSON.parse(JSON.stringify(v))
}
</script>

<template>
  <n-drawer
    :show="visible"
    :width="width"
    placement="right"
    :mask-closable="!readonly"
    @update:show="$emit('update:visible', $event)"
  >
    <n-drawer-content
      class="node-config-drawer"
      :native-scrollbar="false"
      :closable="false"
    >
      <div v-if="draftNode" class="node-config-shell">
        <div class="node-config-header">
          <div class="node-config-title">
            <span class="node-config-icon">
              <i :class="headerIcon" />
            </span>
            <span>{{ draftNode.name || (readonly ? '查看节点' : '配置节点') }}</span>
          </div>
          <button type="button" class="node-config-close" @click="handleClose">
            <i class="i-material-symbols:close" />
          </button>
        </div>

        <BasicConfig
          v-if="!useIntegratedBasicConfig"
          v-model:node="draftNode"
          :readonly="readonly"
        />
        <component
          :is="ConfigComponent"
          v-if="ConfigComponent"
          :node="draftNode"
          v-bind="configExtraProps"
          :readonly="readonly"
          @update:config="updateConfig"
          @update:node="updateNode"
          @update:edge="updateEdge"
        />
        <div v-else class="node-config-empty">
          该节点类型暂无可配置项。
        </div>
      </div>
      <div v-else class="node-config-empty">
        请先选择一个节点。
      </div>

      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button @click="handleClose">
            {{ readonly ? '关闭' : '取消' }}
          </n-button>
          <n-button v-if="!readonly" type="primary" @click="handleSave">
            保存
          </n-button>
        </div>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<style scoped>
:deep(.node-config-drawer .n-drawer-body-content-wrapper) {
  padding: 0;
}

:deep(.node-config-drawer .n-drawer-footer) {
  padding: 12px 18px;
  border-top: 1px solid #eef2f7;
  background: #fff;
}

.node-config-shell {
  min-height: 100%;
  background: #fff;
}

.node-config-header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  border-bottom: 1px solid #eef2f7;
  background: #fff;
}

.node-config-title {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.node-config-icon {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #e6fffb;
  color: #14b8a6;
  font-size: 17px;
}

.node-config-close {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transition:
    background 150ms ease,
    color 150ms ease;
}

.node-config-close:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.node-config-empty {
  margin: 18px;
  border-radius: 8px;
  padding: 16px;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 13px;
}

:deep(.n-tabs .n-tabs-nav) {
  padding: 0 18px;
  border-bottom: 1px solid #eef2f7;
}

:deep(.n-tabs .n-tabs-tab) {
  min-height: 46px;
  padding: 0 14px;
  color: #64748b;
  font-size: 14px;
  font-weight: 600;
}

:deep(.n-tabs .n-tabs-tab.n-tabs-tab--active) {
  color: #14b8a6;
}

:deep(.n-tabs .n-tabs-bar) {
  background-color: #14b8a6;
}

:deep(.n-tabs .n-tab-pane) {
  padding: 18px 26px 24px;
}

:deep(.n-form-item) {
  margin-bottom: 18px;
}

:deep(.n-form-item .n-form-item-label) {
  min-height: 24px;
  padding: 0 0 7px;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

:deep(.n-form-item .n-form-item-label__asterisk) {
  color: #ef4444;
}

:deep(.n-input),
:deep(.n-base-selection),
:deep(.n-input-number) {
  width: 100%;
}

:deep(.n-input .n-input-wrapper),
:deep(.n-base-selection .n-base-selection-label) {
  min-height: 42px;
}

:deep(.n-input__input-el),
:deep(.n-base-selection-input),
:deep(.n-base-selection-placeholder) {
  font-size: 14px;
}

:deep(.config-section-block) {
  margin-top: 18px;
}

:deep(.config-section-title) {
  margin-bottom: 10px;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

:deep(.config-hint) {
  border-radius: 8px;
  padding: 10px 12px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 640px) {
  :deep(.n-tabs .n-tabs-nav) {
    padding: 0 12px;
  }

  :deep(.n-tabs .n-tab-pane) {
    padding: 14px 16px 18px;
  }
}
</style>
