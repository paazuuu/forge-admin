<template>
  <section class="flow-task-card-list">
    <div class="task-list-toolbar">
      <div class="task-list-titlebar">
        <n-checkbox
          v-if="selectable"
          :checked="allCurrentPageSelected"
          :indeterminate="partiallySelected"
          @update:checked="toggleCurrentPage"
        />
        <div class="task-list-title">
          {{ title }}
        </div>
        <span v-if="selectedKeys.length > 0" class="task-list-selected">
          已选 {{ selectedKeys.length }} 条
        </span>
        <button v-if="selectedKeys.length > 0" class="task-list-clear" type="button" @click="clearSelection">
          清空
        </button>
        <slot name="batch-actions" :selected-keys="selectedKeys" />
      </div>

      <div class="task-list-tools">
        <slot name="filters" />
        <n-input
          v-if="showSearch"
          :value="searchValue"
          :placeholder="searchPlaceholder"
          clearable
          class="task-list-search"
          @update:value="emit('update:searchValue', $event)"
          @keydown.enter="emit('search')"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <n-button quaternary class="task-list-icon-btn" title="刷新" aria-label="刷新列表" @click="emit('refresh')">
          <i class="i-material-symbols:refresh" />
        </n-button>
      </div>
    </div>

    <n-spin :show="loading">
      <div v-if="items.length > 0" class="task-card-stack">
        <article
          v-for="item in items"
          :key="getRowKey(item)"
          class="task-card-row"
          :class="{ selected: isSelected(item), unread: isUnread(item) }"
          @click="emit('rowClick', item)"
        >
          <div v-if="selectable" class="task-card-check" @click.stop>
            <n-checkbox :checked="isSelected(item)" @update:checked="checked => toggleRow(item, checked)" />
          </div>

          <div class="task-card-main">
            <div class="task-card-heading">
              <slot name="status" :row="item" />
              <button type="button" class="task-card-title" @click.stop="emit('rowClick', item)">
                <slot name="title" :row="item">
                  {{ item.title || item.taskName || '-' }}
                </slot>
              </button>
            </div>
            <div class="task-card-meta">
              <slot name="meta" :row="item" />
            </div>
            <div v-if="$slots.summary" class="task-card-summary">
              <slot name="summary" :row="item" />
            </div>
          </div>

          <div v-if="$slots.actions" class="task-card-actions" @click.stop>
            <slot name="actions" :row="item" />
          </div>
        </article>
      </div>
      <n-empty v-else class="task-list-empty" :description="emptyText" size="small" />
    </n-spin>

    <div v-if="pagination && pagination.itemCount > 0" class="task-list-pagination">
      <n-pagination
        :page="pagination.page"
        :page-size="pagination.pageSize"
        :item-count="pagination.itemCount"
        :page-sizes="pagination.pageSizes || [10, 20, 50]"
        :show-size-picker="pagination.showSizePicker !== false"
        @update:page="emit('update:page', $event)"
        @update:page-size="emit('update:pageSize', $event)"
      />
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '列表' },
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  pagination: { type: Object, default: null },
  selectedKeys: { type: Array, default: () => [] },
  rowKey: { type: [String, Function], default: 'id' },
  searchValue: { type: String, default: '' },
  searchPlaceholder: { type: String, default: '通过名称搜索' },
  emptyText: { type: String, default: '暂无数据' },
  selectable: { type: Boolean, default: true },
  showSearch: { type: Boolean, default: true },
  unreadKey: { type: String, default: '' },
})

const emit = defineEmits([
  'update:selectedKeys',
  'update:searchValue',
  'update:page',
  'update:pageSize',
  'search',
  'refresh',
  'rowClick',
])

const selectedSet = computed(() => new Set(props.selectedKeys))
const currentPageKeys = computed(() => props.items.map(item => getRowKey(item)).filter(key => key !== undefined && key !== null))
const allCurrentPageSelected = computed(() => currentPageKeys.value.length > 0 && currentPageKeys.value.every(key => selectedSet.value.has(key)))
const partiallySelected = computed(() => currentPageKeys.value.some(key => selectedSet.value.has(key)) && !allCurrentPageSelected.value)

function getRowKey(row) {
  if (typeof props.rowKey === 'function')
    return props.rowKey(row)
  return row?.[props.rowKey]
}

function isSelected(row) {
  return selectedSet.value.has(getRowKey(row))
}

function isUnread(row) {
  return props.unreadKey ? row?.[props.unreadKey] === 0 : false
}

function toggleRow(row, checked) {
  const key = getRowKey(row)
  if (key === undefined || key === null)
    return
  const next = new Set(props.selectedKeys)
  if (checked)
    next.add(key)
  else
    next.delete(key)
  emit('update:selectedKeys', [...next])
}

function toggleCurrentPage(checked) {
  const next = new Set(props.selectedKeys)
  currentPageKeys.value.forEach((key) => {
    if (checked)
      next.add(key)
    else
      next.delete(key)
  })
  emit('update:selectedKeys', [...next])
}

function clearSelection() {
  emit('update:selectedKeys', [])
}
</script>

<style scoped>
.flow-task-card-list {
  box-sizing: border-box;
  display: flex;
  flex: 1;
  width: 100%;
  max-width: none;
  min-height: 0;
  flex-direction: column;
  background: #fff;
}

:deep(.n-spin-container),
:deep(.n-spin-content) {
  width: 100%;
}

.task-list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 0 12px;
}

.task-list-titlebar {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.task-list-title {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
}

.task-list-selected {
  color: #0f766e;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.task-list-clear {
  color: #0f766e;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
}

.task-list-tools {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.task-list-search {
  width: 300px;
}

.task-list-icon-btn {
  width: 44px;
  min-width: 44px;
  height: 42px;
  border: 1px solid #d6e5e5;
  border-radius: 6px;
  color: #3f7474;
  font-size: 22px;
}

:deep(.task-list-icon-btn .n-button__content) {
  font-size: 22px;
  line-height: 1;
}

:deep(.task-list-icon-btn i) {
  font-size: 22px;
  line-height: 1;
}

.task-card-stack {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 12px;
  padding: 0 0 4px;
}

.task-card-row {
  box-sizing: border-box;
  display: grid;
  width: 100%;
  min-height: 88px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  column-gap: 10px;
  border: 1px solid #86cbca;
  border-radius: 5px;
  background: #fff;
  padding: 12px 10px;
  cursor: pointer;
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    box-shadow 160ms ease;
}

.task-card-row:hover,
.task-card-row.selected {
  border-color: #3faaa8;
  box-shadow: 0 2px 10px rgba(15, 118, 110, 0.08);
}

.task-card-row.unread {
  background: #faffff;
}

.task-card-check {
  display: flex;
  align-items: flex-start;
  align-self: stretch;
  padding-top: 4px;
}

.task-card-main {
  min-width: 0;
}

.task-card-heading {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.task-card-title {
  min-width: 0;
  color: #177c7d;
  cursor: pointer;
  font-size: 16px;
  font-weight: 700;
  line-height: 24px;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-card-title:hover {
  color: #0f5f63;
}

.task-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  margin-top: 12px;
  color: #4b5563;
  font-size: 14px;
  line-height: 22px;
}

.task-card-summary {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.task-card-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  justify-self: end;
  gap: 8px;
  min-width: 168px;
  flex-wrap: wrap;
}

:global(.task-row-link-action) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  height: 32px;
  padding: 0 3px 0 8px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #177c7d;
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  transition:
    background-color 160ms ease,
    color 160ms ease;
}

:global(.task-row-link-action:hover) {
  background: #effafa;
  color: #0f5f63;
}

:global(.task-row-link-action.primary) {
  color: #177c7d;
}

:global(.task-row-link-action.info) {
  color: #2563eb;
}

:global(.task-row-link-action.info:hover) {
  background: #eff6ff;
  color: #1d4ed8;
}

:global(.task-row-link-action.success) {
  color: #15803d;
}

:global(.task-row-link-action.success:hover) {
  background: #ecfdf3;
  color: #166534;
}

:global(.task-row-link-action.danger) {
  color: #b42318;
}

:global(.task-row-link-action.danger:hover) {
  background: #fff1f2;
  color: #9f1239;
}

:global(.task-row-link-action i) {
  font-size: 18px;
  line-height: 1;
}

.task-list-empty {
  padding: 56px 0;
}

.task-list-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

:global(.task-status-pill) {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 11px;
  border-radius: 4px;
  background: #fff8e8;
  color: #c07b16;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

:global(.task-list-hint) {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 9px;
  border-radius: 4px;
  background: #f5fbfb;
  color: #2f7777;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

:global(.task-list-hint.urgent) {
  background: #fff7ed;
  color: #c2410c;
}

:global(.task-list-hint.pending) {
  background: #eff6ff;
  color: #2563eb;
}

:global(.task-status-pill.success),
:global(.task-status-pill.read) {
  background: #ecfdf3;
  color: #15803d;
}

:global(.task-status-pill.error),
:global(.task-status-pill.unread) {
  background: #fff1f2;
  color: #be123c;
}

:global(.task-status-pill.info) {
  background: #eff6ff;
  color: #2563eb;
}

:global(.task-status-pill.default) {
  background: #f1f5f9;
  color: #64748b;
}

:global(.task-meta-label) {
  color: #6b7280;
  font-weight: 600;
}

:global(.task-meta-value) {
  color: #374151;
  font-weight: 600;
}

@media (max-width: 900px) {
  .task-list-toolbar {
    align-items: stretch;
    flex-direction: column;
    padding-right: 0;
    padding-left: 0;
  }

  .task-list-tools {
    flex-wrap: wrap;
  }

  .task-list-search {
    width: min(100%, 328px);
  }

  .task-card-row {
    grid-template-columns: auto minmax(0, 1fr);
    row-gap: 14px;
    min-height: 0;
    padding: 12px;
  }

  .task-card-actions {
    grid-column: 1 / -1;
    min-width: 0;
    justify-content: flex-end;
  }
}
</style>
