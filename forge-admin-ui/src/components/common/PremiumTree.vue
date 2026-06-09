<template>
  <div class="premium-tree" role="tree">
    <PremiumTreeNode
      v-for="node in data"
      :key="getNodeKey(node)"
      :node="node"
      :level="0"
      :selected-key="selectedKey"
      :expanded-key-set="expandedKeySet"
      :key-field="keyField"
      :label-field="labelField"
      :children-field="childrenField"
      :get-node-icon="getNodeIcon"
      :get-node-meta="getNodeMeta"
      :get-node-subtitle="getNodeSubtitle"
      :get-node-tone="getNodeTone"
      @select="handleSelect"
      @toggle="handleToggle"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import PremiumTreeNode from './PremiumTreeNode.vue'

defineOptions({ name: 'PremiumTree' })

const props = defineProps({
  data: {
    type: Array,
    default: () => [],
  },
  selectedKeys: {
    type: Array,
    default: () => [],
  },
  expandedKeys: {
    type: Array,
    default: () => [],
  },
  keyField: {
    type: String,
    default: 'key',
  },
  labelField: {
    type: String,
    default: 'label',
  },
  childrenField: {
    type: String,
    default: 'children',
  },
  getNodeIcon: {
    type: Function,
    default: null,
  },
  getNodeMeta: {
    type: Function,
    default: null,
  },
  getNodeSubtitle: {
    type: Function,
    default: null,
  },
  getNodeTone: {
    type: Function,
    default: null,
  },
})

const emit = defineEmits(['update:selected-keys', 'update:expanded-keys', 'select'])

const selectedKey = computed(() => props.selectedKeys?.[0])
const expandedKeySet = computed(() => new Set(props.expandedKeys || []))

function getNodeKey(node = {}) {
  return node[props.keyField] ?? node.key ?? node.id
}

function handleSelect(node) {
  const key = getNodeKey(node)
  emit('update:selected-keys', key === undefined ? [] : [key], node)
  emit('select', node)
}

function handleToggle(node, nextExpanded) {
  const key = getNodeKey(node)
  if (key === undefined)
    return

  const keys = new Set(props.expandedKeys || [])
  const shouldExpand = typeof nextExpanded === 'boolean' ? nextExpanded : !keys.has(key)
  if (shouldExpand)
    keys.add(key)
  else
    keys.delete(key)

  emit('update:expanded-keys', Array.from(keys), node)
}
</script>

<style scoped>
.premium-tree {
  width: 100%;
  padding: 2px 0;
}
</style>
