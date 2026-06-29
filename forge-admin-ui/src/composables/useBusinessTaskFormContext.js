import { computed, ref, unref } from 'vue'
import {
  businessTaskFormContext,
  businessTaskFormReadonlyContext,
  saveBusinessTaskFormContext,
} from '@/api/business-app'
import { createFieldPermissionMap, normalizeFieldPermissions } from '@/utils/field-permissions'

export function useBusinessTaskFormContext(defaultQuery = {}, options = {}) {
  const context = ref(null)
  const loading = ref(false)
  const saving = ref(false)
  const error = ref('')
  const readonly = computed(() => unref(options.readonly) === true)

  const recordData = computed(() => context.value?.recordData || {})
  const fields = computed(() => Array.isArray(context.value?.fields) ? context.value.fields : [])
  const fieldPermissions = computed(() => normalizeFieldPermissions(context.value?.fieldPermissions, { readOnly: readonly.value }))

  const permissionMap = computed(() => createFieldPermissionMap(fieldPermissions.value, { readOnly: readonly.value }))

  async function load(query = {}) {
    const params = compactParams({ ...defaultQuery, ...query })
    if (!hasContextQuery(params)) {
      context.value = null
      return null
    }
    loading.value = true
    error.value = ''
    try {
      const api = readonly.value ? businessTaskFormReadonlyContext : businessTaskFormContext
      const res = await api(params)
      if (res.code !== 200)
        throw new Error(res.message || '业务表单上下文加载失败')
      context.value = res.data || null
      return context.value
    }
    catch (e) {
      error.value = e?.message || '业务表单上下文加载失败'
      throw e
    }
    finally {
      loading.value = false
    }
  }

  async function save(data = {}, overrides = {}) {
    if (!context.value)
      throw new Error('请先加载业务表单上下文')
    saving.value = true
    error.value = ''
    try {
      const payload = compactParams({
        taskId: context.value.taskId,
        businessKey: context.value.businessKey,
        processInstanceId: context.value.processInstanceId,
        processDefKey: context.value.processDefKey,
        taskDefKey: context.value.taskDefKey,
        objectCode: context.value.objectCode,
        recordId: context.value.recordId,
        formKey: context.value.formKey,
        ...overrides,
        data,
      })
      const res = await saveBusinessTaskFormContext(payload)
      if (res.code !== 200)
        throw new Error(res.message || '业务字段保存失败')
      context.value = res.data || context.value
      return context.value
    }
    catch (e) {
      error.value = e?.message || '业务字段保存失败'
      throw e
    }
    finally {
      saving.value = false
    }
  }

  function fieldPermission(field) {
    const key = String(field || '').trim()
    return key ? permissionMap.value.get(key) : null
  }

  function canShowField(field) {
    const permission = fieldPermission(field)
    return permission ? permission.visible !== false : true
  }

  function canEditField(field) {
    if (readonly.value)
      return false
    const permission = fieldPermission(field)
    return permission ? permission.editable !== false && permission.visible !== false : true
  }

  function isRequiredField(field) {
    return fieldPermission(field)?.required === true
  }

  return {
    context,
    recordData,
    fields,
    fieldPermissions,
    loading,
    saving,
    error,
    load,
    save,
    fieldPermission,
    canShowField,
    canEditField,
    isRequiredField,
  }
}

function compactParams(source = {}) {
  const result = {}
  Object.entries(source).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '')
      result[key] = value
  })
  return result
}

function hasContextQuery(query = {}) {
  return Boolean(query.taskId || query.processInstanceId || query.businessKey || (query.objectCode && query.recordId))
}
