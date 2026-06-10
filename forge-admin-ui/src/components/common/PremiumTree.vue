<template>
  <div class="premium-tree" role="tree">
    <PremiumTreeNode
      v-for="node in data"
      :key="getNodeKey(node)"
      :node="node"
      :level="0"
      :selected-key="selectedKey"
      :expanded-key-set="expandedKeySet"
      :checked-key-set="checkedKeySet"
      :indeterminate-key-set="indeterminateKeySet"
      :key-field="keyField"
      :label-field="labelField"
      :children-field="childrenField"
      :checkable="checkable"
      :get-node-icon="getNodeIcon"
      :get-node-meta="getNodeMeta"
      :get-node-subtitle="getNodeSubtitle"
      :get-node-tone="getNodeTone"
      :show-meta="showMeta"
      :show-subtitle="showSubtitle"
      @select="handleSelect"
      @check="handleCheck"
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
  checkedKeys: {
    type: Array,
    default: () => [],
  },
  checkable: {
    type: Boolean,
    default: false,
  },
  cascade: {
    type: Boolean,
    default: true,
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
  showMeta: {
    type: Boolean,
    default: false,
  },
  showSubtitle: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:selected-keys', 'update:expanded-keys', 'update:checked-keys', 'select', 'check'])

const selectedKey = computed(() => props.selectedKeys?.[0])
const expandedKeySet = computed(() => new Set(props.expandedKeys || []))
const checkedKeySet = computed(() => new Set(props.checkedKeys || []))
const indeterminateKeySet = computed(() => {
  if (!props.checkable || !props.cascade)
    return new Set()

  const result = new Set()
  collectIndeterminateKeys(props.data || [], checkedKeySet.value, result)
  return result
})

function getNodeKey(node = {}) {
  return node[props.keyField] ?? node.key ?? node.id
}

function handleSelect(node) {
  const key = getNodeKey(node)
  emit('update:selected-keys', key === undefined ? [] : [key], node)
  emit('select', node)
}

function handleCheck(node, checked) {
  const keys = new Set(props.checkedKeys || [])
  const targetKeys = props.cascade ? collectNodeKeys(node) : [getNodeKey(node)]

  targetKeys.forEach((key) => {
    if (key === undefined)
      return
    if (checked)
      keys.add(key)
    else
      keys.delete(key)
  })

  const nextKeys = Array.from(keys)
  emit('update:checked-keys', nextKeys, node)
  emit('check', nextKeys, node)
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

function collectNodeKeys(node = {}) {
  const keys = []
  const walk = (item) => {
    const key = getNodeKey(item)
    if (key !== undefined)
      keys.push(key)
    const children = item?.[props.childrenField] || []
    children.forEach(walk)
  }
  walk(node)
  return keys
}

function collectIndeterminateKeys(nodes = [], checkedSet, result) {
  nodes.forEach((node) => {
    const key = getNodeKey(node)
    const children = node?.[props.childrenField] || []
    if (!children.length)
      return

    collectIndeterminateKeys(children, checkedSet, result)

    const childKeys = children.map(child => getNodeKey(child)).filter(keyValue => keyValue !== undefined)
    const hasCheckedChild = childKeys.some(childKey => checkedSet.has(childKey))
    const hasIndeterminateChild = childKeys.some(childKey => result.has(childKey))
    const allChildrenChecked = childKeys.length > 0 && childKeys.every(childKey => checkedSet.has(childKey))

    if (key !== undefined && !checkedSet.has(key) && !allChildrenChecked && (hasCheckedChild || hasIndeterminateChild))
      result.add(key)
  })
}
</script>

<style scoped>
.premium-tree {
  width: 100%;
  padding: 2px 0;
}
</style>
