<template>
  <div class="premium-tree-node" role="none">
    <span
      v-if="level > 0"
      class="tree-guide tree-guide-vertical"
      :style="guideVerticalStyle"
    />
    <div
      class="premium-tree-row"
      :class="[
        `tone-${nodeTone}`,
        {
          'is-selected': isSelected,
          'has-children': hasChildren,
          'is-expanded': isExpanded,
        },
      ]"
      :style="{ paddingLeft: `${level * 20 + 4}px` }"
      role="treeitem"
      :aria-selected="isSelected"
      :aria-expanded="hasChildren ? isExpanded : undefined"
      tabindex="0"
      @click="handleSelect"
      @keydown.enter.prevent="handleSelect"
      @keydown.space.prevent="handleSelect"
      @keydown.right.prevent="handleArrowRight"
      @keydown.left.prevent="handleArrowLeft"
    >
      <button
        class="premium-tree-switcher"
        :class="{ 'is-visible': hasChildren }"
        type="button"
        :aria-label="isExpanded ? '折叠节点' : '展开节点'"
        :tabindex="hasChildren ? 0 : -1"
        @click.stop="handleToggle"
      >
        <i v-if="hasChildren" class="i-material-symbols:chevron-right-rounded" />
      </button>

      <input
        v-if="checkable"
        class="premium-tree-checkbox"
        type="checkbox"
        :checked="isChecked"
        :indeterminate="isIndeterminate"
        :aria-checked="isIndeterminate ? 'mixed' : isChecked"
        @click.stop
        @change="handleCheckChange"
      >

      <span class="premium-tree-icon">
        <i :class="nodeIcon" />
      </span>

      <span class="premium-tree-copy">
        <span class="premium-tree-title">{{ nodeLabel }}</span>
        <small v-if="nodeSubtitle" class="premium-tree-subtitle">{{ nodeSubtitle }}</small>
      </span>

      <span v-if="nodeMeta" class="premium-tree-meta">
        <span v-if="nodeMeta.label">{{ nodeMeta.label }}</span>
        <strong>{{ nodeMeta.value }}</strong>
      </span>
    </div>

    <Transition name="premium-tree-children">
      <div v-if="hasChildren && isExpanded" class="premium-tree-children" role="group">
        <div class="premium-tree-children-inner">
          <PremiumTreeNode
            v-for="child in nodeChildren"
            :key="getNodeKey(child)"
            :node="child"
            :level="level + 1"
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
            @select="$emit('select', $event)"
            @check="(treeNode, checked) => $emit('check', treeNode, checked)"
            @toggle="(treeNode, nextExpanded) => $emit('toggle', treeNode, nextExpanded)"
          />
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({ name: 'PremiumTreeNode' })

const props = defineProps({
  node: {
    type: Object,
    required: true,
  },
  level: {
    type: Number,
    default: 0,
  },
  selectedKey: {
    type: [String, Number],
    default: null,
  },
  expandedKeySet: {
    type: Object,
    required: true,
  },
  checkedKeySet: {
    type: Object,
    default: () => new Set(),
  },
  indeterminateKeySet: {
    type: Object,
    default: () => new Set(),
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
  checkable: {
    type: Boolean,
    default: false,
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

const emit = defineEmits(['select', 'toggle', 'check'])

const nodeKey = computed(() => getNodeKey(props.node))
const nodeChildren = computed(() => props.node?.[props.childrenField] || [])
const hasChildren = computed(() => nodeChildren.value.length > 0)
const isExpanded = computed(() => props.expandedKeySet.has(nodeKey.value))
const isSelected = computed(() => props.selectedKey === nodeKey.value)
const isChecked = computed(() => props.checkedKeySet.has(nodeKey.value))
const isIndeterminate = computed(() => props.indeterminateKeySet.has(nodeKey.value))
const nodeLabel = computed(() => props.node?.[props.labelField] || props.node?.label || props.node?.title || '')
const nodeSubtitle = computed(() => {
  if (!props.showSubtitle)
    return ''
  return props.getNodeSubtitle?.(props.node) || props.node?.subtitle || props.node?.path || ''
})
const nodeIcon = computed(() => props.getNodeIcon?.(props.node) || props.node?.icon || 'i-material-symbols:folder-outline-rounded')
const nodeMeta = computed(() => {
  if (!props.showMeta)
    return null
  return props.getNodeMeta?.(props.node) || null
})
const nodeTone = computed(() => props.getNodeTone?.(props.node) || 'default')

const guideLeft = computed(() => `${(props.level - 1) * 20 + 14}px`)
const guideVerticalStyle = computed(() => ({
  left: guideLeft.value,
}))

function getNodeKey(node = {}) {
  return node[props.keyField] ?? node.key ?? node.id
}

function handleSelect() {
  if (props.checkable) {
    emit('check', props.node, !isChecked.value)
    return
  }
  emit('select', props.node)
  if (hasChildren.value && !isExpanded.value)
    emit('toggle', props.node, true)
}

function handleCheckChange(event) {
  emit('check', props.node, event.target.checked)
}

function handleToggle() {
  if (hasChildren.value)
    emit('toggle', props.node)
}

function handleArrowRight() {
  if (hasChildren.value && !isExpanded.value)
    emit('toggle', props.node, true)
}

function handleArrowLeft() {
  if (hasChildren.value && isExpanded.value)
    emit('toggle', props.node, false)
}
</script>

<style scoped>
.premium-tree-node {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.tree-guide {
  position: absolute;
  z-index: 0;
  pointer-events: none;
  background: rgba(148, 163, 184, 0.42);
}

.tree-guide-vertical {
  top: -3px;
  bottom: 15px;
  width: 1px;
}

.premium-tree-row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 30px;
  padding-top: 3px;
  padding-right: 8px;
  padding-bottom: 3px;
  margin: 1px 0;
  border: 1px solid transparent;
  border-radius: 7px;
  color: #475569;
  cursor: pointer;
  outline: none;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.premium-tree-row:hover {
  background: rgba(239, 246, 255, 0.82);
  border-color: rgba(191, 219, 254, 0.78);
  transform: translateX(2px);
}

.premium-tree-row:focus-visible {
  border-color: rgba(47, 111, 237, 0.42);
  box-shadow: 0 0 0 3px rgba(47, 111, 237, 0.12);
}

.premium-tree-row.is-selected {
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.98) 0%, rgba(191, 219, 254, 0.88) 100%);
  border-color: rgba(96, 165, 250, 0.52);
  color: #1d4ed8;
  box-shadow:
    inset 3px 0 0 #2563eb,
    0 6px 16px rgba(37, 99, 235, 0.12);
}

.premium-tree-switcher {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 20px;
  height: 20px;
  padding: 0;
  margin: 0 3px 0 0;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: transparent;
  cursor: default;
}

.premium-tree-switcher.is-visible {
  color: #94a3b8;
  cursor: pointer;
}

.premium-tree-switcher.is-visible:hover {
  background: rgba(148, 163, 184, 0.14);
  color: #475569;
}

.premium-tree-switcher i {
  font-size: 16px;
  transition: transform 0.2s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.premium-tree-row.is-expanded .premium-tree-switcher i {
  transform: rotate(90deg);
}

.premium-tree-checkbox {
  flex: 0 0 auto;
  width: 15px;
  height: 15px;
  margin: 0 8px 0 0;
  accent-color: #2563eb;
  cursor: pointer;
}

.premium-tree-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 24px;
  height: 24px;
  margin-right: 8px;
  border-radius: 6px;
  background: rgba(241, 245, 249, 0.8);
  color: #64748b;
  transition:
    background-color 0.18s ease,
    color 0.18s ease;
}

.premium-tree-icon i {
  font-size: 15px;
  line-height: 1;
}

.premium-tree-copy {
  display: flex;
  align-items: baseline;
  min-width: 0;
  flex: 1;
  gap: 8px;
}

.premium-tree-title {
  min-width: 0;
  overflow: hidden;
  color: #475569;
  font-size: 13px;
  font-weight: 550;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.18s ease;
}

.premium-tree-subtitle {
  min-width: 0;
  overflow: hidden;
  color: #94a3b8;
  font-size: 11px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.premium-tree-meta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  min-width: 46px;
  gap: 4px;
  padding: 1px 7px;
  border: 1px solid rgba(147, 197, 253, 0.9);
  border-radius: 999px;
  background: rgba(239, 246, 255, 0.9);
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 650;
  line-height: 16px;
}

.premium-tree-meta strong {
  color: #1e3a8a;
  font-size: 12px;
  font-weight: 750;
}

.premium-tree-row.is-selected .premium-tree-title {
  color: #1e3a8a;
  font-weight: 700;
}

.premium-tree-row:hover .premium-tree-icon {
  background: rgba(255, 255, 255, 0.9);
}

.premium-tree-row.tone-root .premium-tree-icon {
  background: #f1f5f9;
  color: #64748b;
}

.premium-tree-row.tone-folder .premium-tree-icon {
  background: #eaf2ff;
  color: #3b82f6;
}

.premium-tree-row.tone-menu .premium-tree-icon {
  background: #eaf8ef;
  color: #16a34a;
}

.premium-tree-row.tone-action .premium-tree-icon {
  background: #fff1e6;
  color: #ea580c;
}

.premium-tree-row.tone-api .premium-tree-icon {
  background: #feeceb;
  color: #dc2626;
}

.premium-tree-row.is-selected .premium-tree-icon {
  background: #2563eb;
  color: #fff;
}

.premium-tree-row.is-selected .premium-tree-subtitle {
  color: #3b82f6;
}

.premium-tree-row.is-selected .premium-tree-meta {
  background: #fff;
  border-color: rgba(96, 165, 250, 0.5);
}

.premium-tree-children {
  position: relative;
  overflow: hidden;
}

.premium-tree-children-inner {
  min-height: 0;
  overflow: hidden;
}

.premium-tree-children-enter-active,
.premium-tree-children-leave-active {
  display: grid;
  transition:
    grid-template-rows 0.22s cubic-bezier(0.2, 0.8, 0.2, 1),
    opacity 0.18s ease;
}

.premium-tree-children-enter-from,
.premium-tree-children-leave-to {
  grid-template-rows: 0fr;
  opacity: 0;
}

.premium-tree-children-enter-to,
.premium-tree-children-leave-from {
  grid-template-rows: 1fr;
  opacity: 1;
}

:global(.dark) .premium-tree-row {
  color: #cbd5e1;
}

:global(.dark) .premium-tree-row:hover {
  background: rgba(30, 64, 175, 0.26);
  border-color: rgba(96, 165, 250, 0.28);
}

:global(.dark) .premium-tree-row.is-selected {
  background: linear-gradient(135deg, rgba(30, 64, 175, 0.62) 0%, rgba(29, 78, 216, 0.36) 100%);
  border-color: rgba(96, 165, 250, 0.5);
  color: #dbeafe;
  box-shadow:
    inset 3px 0 0 #60a5fa,
    0 8px 18px rgba(15, 23, 42, 0.28);
}

:global(.dark) .premium-tree-title {
  color: #cbd5e1;
}

:global(.dark) .premium-tree-row.is-selected .premium-tree-title {
  color: #eff6ff;
}

:global(.dark) .premium-tree-subtitle {
  color: #64748b;
}

:global(.dark) .premium-tree-row.is-selected .premium-tree-subtitle {
  color: #bfdbfe;
}

:global(.dark) .tree-guide {
  background: rgba(100, 116, 139, 0.36);
}

@media (prefers-reduced-motion: reduce) {
  .premium-tree-row,
  .premium-tree-switcher i,
  .premium-tree-children-enter-active,
  .premium-tree-children-leave-active {
    transition: none;
  }

  .premium-tree-row:hover {
    transform: none;
  }
}
</style>
