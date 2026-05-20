<template>
  <div class="field-table">
    <div class="table-head">
      <span>字段</span>
      <span>数据库列</span>
      <span>类型</span>
      <span>显示</span>
      <span>操作</span>
    </div>
    <draggable
      :model-value="fields"
      item-key="field"
      handle=".drag-handle"
      animation="160"
      class="table-body"
      @update:model-value="$emit('update:fields', $event)"
    >
      <template #item="{ element, index }">
        <div class="field-row" :class="{ active: selectedIndex === index }" @click="$emit('select', index)">
          <div class="field-main">
            <n-button text size="tiny" class="drag-handle" @click.stop>
              <template #icon>
                <n-icon><ReorderFourOutline /></n-icon>
              </template>
            </n-button>
            <div class="field-edit-stack">
              <n-input
                :value="element.label"
                size="small"
                placeholder="字段名称"
                @click.stop
                @update:value="updateField(index, { label: $event })"
              />
              <n-input
                :value="element.field"
                size="small"
                placeholder="fieldName"
                @click.stop
                @update:value="handleFieldNameChange(index, $event)"
              />
            </div>
          </div>
          <div>
            <n-input
              :value="element.columnName"
              size="small"
              placeholder="column_name"
              @click.stop
              @update:value="updateField(index, { columnName: $event })"
            />
          </div>
          <div class="field-edit-stack">
            <n-select
              :value="element.dataType"
              size="small"
              :options="dataTypeOptions"
              @click.stop
              @update:value="updateField(index, { dataType: $event })"
            />
            <n-select
              :value="element.componentType"
              size="small"
              :options="componentTypeOptions"
              @click.stop
              @update:value="updateField(index, { componentType: $event })"
            />
          </div>
          <div class="field-flags">
            <n-checkbox
              :checked="element.searchable"
              size="small"
              @click.stop
              @update:checked="updateField(index, { searchable: $event })"
            >
              查
            </n-checkbox>
            <n-checkbox
              :checked="element.listVisible"
              size="small"
              @click.stop
              @update:checked="updateField(index, { listVisible: $event })"
            >
              列
            </n-checkbox>
            <n-checkbox
              :checked="element.formVisible"
              size="small"
              @click.stop
              @update:checked="updateField(index, { formVisible: $event })"
            >
              表
            </n-checkbox>
          </div>
          <div class="field-actions" @click.stop>
            <n-button text size="tiny" class="text-primary" @click="$emit('copy', index)">
              复制
            </n-button>
            <n-popconfirm @positive-click="$emit('remove', index)">
              <template #trigger>
                <n-button text size="tiny" class="text-error">
                  删除
                </n-button>
              </template>
              删除字段后会同步移除页面引用，确认继续？
            </n-popconfirm>
          </div>
        </div>
      </template>
    </draggable>
  </div>
</template>

<script setup>
import { ReorderFourOutline } from '@vicons/ionicons5'
import draggable from 'vuedraggable'
import { camelToSnake, componentTypeOptions, dataTypeOptions, normalizeFieldName } from './model-schema'

const props = defineProps({
  fields: {
    type: Array,
    default: () => [],
  },
  selectedIndex: {
    type: Number,
    default: -1,
  },
})

const emit = defineEmits(['update:fields', 'select', 'copy', 'remove'])

function updateField(index, patch) {
  const fields = props.fields.map((field, fieldIndex) => fieldIndex === index ? { ...field, ...patch } : field)
  emit('update:fields', fields)
}

function handleFieldNameChange(index, value) {
  const nextField = normalizeFieldName(value)
  updateField(index, {
    field: nextField,
    columnName: camelToSnake(nextField),
  })
}
</script>

<style scoped>
.field-table {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.table-head,
.field-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.25fr) minmax(150px, 0.9fr) minmax(150px, 0.9fr) 150px 96px;
  align-items: center;
  gap: 12px;
}

.table-head {
  height: 38px;
  padding: 0 14px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;
}

.table-body {
  min-height: 280px;
}

.field-row {
  min-height: 76px;
  padding: 8px 14px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
}

.field-row:last-child {
  border-bottom: 0;
}

.field-row:hover,
.field-row.active {
  background: #f8fbff;
}

.field-main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.field-edit-stack {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.field-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.field-flags {
  display: grid;
  grid-template-columns: repeat(3, auto);
  align-items: center;
  gap: 6px;
}
</style>
