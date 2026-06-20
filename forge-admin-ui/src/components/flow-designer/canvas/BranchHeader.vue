<script setup>
/**
 * BranchHeader — 分支首部条件徽章
 *
 * 用法：在每条分支线的首端（紧贴网关下方）渲染一个带 condition 文本的胶囊，
 * 标识 "if (days > 3)" / "默认" 等。
 *
 * Props:
 *   - edge       branchEdge（含 condition / isDefault / branchId）
 *   - position   { x, y } 画布坐标
 *
 * Events:
 *   - click  点击徽章 → 父组件可打开条件配置抽屉
 */
import { computed } from 'vue'

const props = defineProps({
  edge: { type: Object, required: true },
  position: { type: Object, required: true },
})

defineEmits(['click'])

const labelText = computed(() => {
  if (props.edge?.isDefault)
    return '默认'
  const t = String(props.edge?.condition || '')
  if (!t)
    return '配置条件'
  return t.length > 18 ? `${t.slice(0, 18)}…` : t
})

const colorClass = computed(() => {
  if (props.edge?.isDefault)
    return 'is-default'
  if (props.edge?.condition)
    return 'is-configured'
  return 'is-empty'
})
</script>

<template>
  <div
    class="branch-header text-xs absolute z-10 flex cursor-pointer items-center px-2.5 py-1"
    :class="colorClass"
    :style="{
      left: `${position.x}px`,
      top: `${position.y}px`,
      transform: 'translate(-50%, -50%)',
    }"
    @click.stop="$emit('click', edge)"
  >
    {{ labelText }}
  </div>
</template>

<style scoped>
.branch-header {
  max-width: 160px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: box-shadow 160ms ease;
}

.branch-header:hover {
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
}

.branch-header.is-default {
  border-color: rgba(245, 158, 11, 0.32);
  background: #fff8ed;
  color: #b45309;
}

.branch-header.is-configured {
  border-color: rgba(53, 109, 255, 0.24);
  background: #f2f6ff;
  color: #245bdb;
}

.branch-header.is-empty {
  border-style: dashed;
  color: #94a3b8;
}

@media (prefers-reduced-motion: reduce) {
  .branch-header {
    transition: none;
  }

  .branch-header:hover {
    box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
  }
}
</style>
