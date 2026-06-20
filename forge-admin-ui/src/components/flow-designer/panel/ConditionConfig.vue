<script setup>
/**
 * ConditionConfig — 网关条件配置
 *
 * 显示该网关所有出边的 condition / isDefault：
 * - 用户在此选择哪条边作为默认（互斥单选）
 * - 编辑每条边的 condition 表达式
 *
 * 因为 ConditionConfig 涉及网关的所有出边而不仅是节点本身的 config，本组件接收
 * outgoingEdges + onEdgeUpdate 作为额外 props（注入式 — 由 NodeConfigDrawer 父组件传入）。
 *
 * Props:
 *   - node                  网关节点
 *   - outgoingEdges         网关的出边数组
 *   - nodes                 当前流程节点数组，用于把目标节点 ID 翻译成节点名称
 *   - readonly
 *
 * Events:
 *   - update:edge(edgeId, patch)
 *   - update:config(patch)   仅写 defaultFlowId 到节点 config，便于 markBranches 兼容
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  outgoingEdges: { type: Array, default: () => [] },
  nodes: { type: Array, default: () => [] },
  readonly: Boolean,
})

const emit = defineEmits(['update:edge', 'update:config'])

const edges = computed(() => props.outgoingEdges || [])
const nodeNameMap = computed(() => new Map((props.nodes || []).map(node => [node.id, node.name || '未命名节点'])))

function getBranchTitle(index) {
  return `分支 ${index + 1}`
}

function getTargetName(edge) {
  return nodeNameMap.value.get(edge.target) || '未命名节点'
}

function setDefault(edgeId) {
  if (props.readonly)
    return
  for (const e of edges.value)
    emit('update:edge', e.id, { isDefault: e.id === edgeId, condition: e.id === edgeId ? '' : e.condition })
  emit('update:config', { defaultFlowId: edgeId })
}

function updateCondition(edgeId, value) {
  emit('update:edge', edgeId, { condition: value, conditionType: 'expression' })
}
</script>

<template>
  <div class="space-y-3">
    <div class="text-sm text-gray-600">
      该网关共 {{ edges.length }} 条分支
    </div>
    <div v-if="edges.length === 0" class="text-xs rounded bg-gray-50 px-3 py-2 text-gray-400">
      暂无分支，请先在画布上添加下游节点
    </div>
    <div
      v-for="(e, index) in edges"
      :key="e.id"
      class="border border-gray-100 rounded p-3 space-y-2"
    >
      <div class="flex items-center justify-between">
        <div class="min-w-0">
          <div class="text-sm text-gray-800 font-medium">
            {{ getBranchTitle(index) }}
          </div>
          <div class="text-xs mt-1 truncate text-gray-500">
            下游节点：{{ getTargetName(e) }}
          </div>
        </div>
        <n-tag v-if="e.isDefault" type="warning" size="small">
          默认分支
        </n-tag>
      </div>

      <n-form-item label="条件表达式" label-placement="top" :show-feedback="false">
        <n-input
          :value="e.condition"
          :disabled="readonly || e.isDefault"
          placeholder="${days > 3}"
          @update:value="updateCondition(e.id, $event)"
        />
      </n-form-item>
      <div class="flex items-center gap-2">
        <n-radio
          :checked="e.isDefault"
          :disabled="readonly"
          @click="setDefault(e.id)"
        >
          作为默认分支
        </n-radio>
      </div>
    </div>
  </div>
</template>
