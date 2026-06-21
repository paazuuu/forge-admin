<script setup>
/**
 * FormPermissionConfig — 表单字段权限
 *
 * 由流程全局动态表单字段目录生成，不再手工输入字段名。
 * config.formFieldPermissions: [{ field, label, readable, writable, required }]
 */
import { computed } from 'vue'

const props = defineProps({
  config: { type: Object, required: true },
  formFieldCatalog: { type: Array, default: () => [] },
  readonly: Boolean,
})

const emit = defineEmits(['update:config'])

const savedPermissions = computed(() => {
  const map = new Map()
  const source = Array.isArray(props.config?.formFieldPermissions) ? props.config.formFieldPermissions : []
  for (const item of source) {
    const normalized = normalizePermission(item)
    if (normalized.field)
      map.set(normalized.field, normalized)
  }
  return map
})

const formFields = computed(() => {
  const map = new Map()
  for (const item of props.formFieldCatalog || []) {
    const field = String(item?.field || item?.fieldName || item?.name || item?.key || '').trim()
    if (!field || map.has(field))
      continue
    map.set(field, {
      field,
      label: item?.label || item?.title || field,
      componentType: item?.componentType || item?.type || '',
      dataType: item?.dataType || '',
      sourceRequired: item?.required === true,
    })
  }
  return Array.from(map.values())
})

const rows = computed(() => {
  const output = formFields.value.map((field) => {
    const saved = savedPermissions.value.get(field.field)
    return {
      ...field,
      ...normalizePermission(saved || field),
    }
  })

  for (const saved of savedPermissions.value.values()) {
    if (!output.some(row => row.field === saved.field)) {
      output.push({
        ...saved,
        sourceRequired: false,
        componentType: '',
        dataType: '',
        stale: true,
      })
    }
  }
  return output
})

const configuredCount = computed(() => {
  return rows.value.filter(row => row.readable === false || row.writable === false || row.required === true).length
})

function normalizePermission(item = {}) {
  return {
    field: String(item.field || '').trim(),
    label: String(item.label || item.field || '').trim(),
    readable: item.readable !== false,
    writable: item.writable !== false,
    required: item.required === true,
  }
}

function update(field, patch) {
  if (props.readonly)
    return
  const nextRows = rows.value.map((row) => {
    if (row.field !== field)
      return row
    const next = { ...row, ...patch }
    if (next.readable === false) {
      next.writable = false
      next.required = false
    }
    if (next.writable === true || next.required === true)
      next.readable = true
    if (next.required === true)
      next.writable = true
    return next
  })
  emit('update:config', {
    formFieldPermissions: nextRows.map(normalizePermission).filter(item => item.field),
  })
}
</script>

<template>
  <div class="form-permission-config">
    <div class="form-permission-head">
      <div>
        <div class="form-permission-title">
          表单字段权限
        </div>
        <div class="form-permission-desc">
          按流程全局表单字段控制当前审批节点的可见、可编辑和必填。
        </div>
      </div>
      <n-tag size="small" :type="configuredCount > 0 ? 'info' : 'default'" :bordered="false">
        {{ configuredCount > 0 ? `${configuredCount} 项已调整` : '默认全量可写' }}
      </n-tag>
    </div>

    <div v-if="rows.length === 0" class="form-permission-empty">
      <i class="i-material-symbols:dynamic-form" />
      <div>
        <div class="form-permission-empty-title">
          暂无可配置字段
        </div>
        <div class="form-permission-empty-desc">
          请先在“更多设置 / 表单配置”中配置动态表单，保存后这里会自动列出字段。
        </div>
      </div>
    </div>

    <div v-else class="form-permission-table">
      <div class="form-permission-row is-head">
        <span>字段</span>
        <span>可见</span>
        <span>可编辑</span>
        <span>必填</span>
      </div>
      <div
        v-for="row in rows"
        :key="row.field"
        class="form-permission-row"
        :class="{ stale: row.stale }"
      >
        <div class="form-field-cell">
          <div class="form-field-label">
            {{ row.label || row.field }}
            <n-tag v-if="row.sourceRequired" size="tiny" type="warning" :bordered="false">
              表单必填
            </n-tag>
            <n-tag v-if="row.stale" size="tiny" type="default" :bordered="false">
              历史字段
            </n-tag>
          </div>
          <div class="form-field-code">
            {{ row.field }}
          </div>
        </div>
        <n-checkbox
          data-test="permission-readable"
          :checked="row.readable"
          :disabled="readonly"
          @update:checked="update(row.field, { readable: $event })"
        />
        <n-checkbox
          data-test="permission-writable"
          :checked="row.writable"
          :disabled="readonly || !row.readable"
          @update:checked="update(row.field, { writable: $event })"
        />
        <n-checkbox
          data-test="permission-required"
          :checked="row.required"
          :disabled="readonly || !row.readable"
          @update:checked="update(row.field, { required: $event })"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.form-permission-config {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-permission-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.form-permission-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.form-permission-desc {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.form-permission-empty {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  padding: 14px;
  color: #64748b;
}

.form-permission-empty i {
  margin-top: 2px;
  color: #2563eb;
  font-size: 20px;
}

.form-permission-empty-title {
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.form-permission-empty-desc {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
}

.form-permission-table {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.form-permission-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 64px 64px 64px;
  align-items: center;
  gap: 8px;
  min-height: 54px;
  padding: 8px 12px;
  background: #fff;
  border-top: 1px solid #eef2f7;
}

.form-permission-row:first-child {
  border-top: none;
}

.form-permission-row.is-head {
  min-height: 36px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.form-permission-row:not(.is-head):hover {
  background: #f8fafc;
}

.form-permission-row.stale {
  background: #fffbeb;
}

.form-field-cell {
  min-width: 0;
}

.form-field-label {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.form-field-code {
  margin-top: 3px;
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  word-break: break-all;
}
</style>
