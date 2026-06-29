export function normalizeFieldPermissions(source, options = {}) {
  const list = parsePermissionList(source)
  if (!Array.isArray(list))
    return []

  return list
    .map((item) => {
      if (!item || typeof item !== 'object')
        return null
      const field = String(item.field || item.fieldCode || item.code || item.name || '').trim()
      if (!field)
        return null
      const readable = readPermissionBoolean(item.readable, readPermissionBoolean(item.visible, true))
      const writable = readable && !options.readOnly && readPermissionBoolean(item.writable, readPermissionBoolean(item.editable, true))
      return {
        ...item,
        field,
        fieldCode: field,
        visible: readable,
        editable: writable,
        readable,
        writable,
        required: writable && readPermissionBoolean(item.required, false),
      }
    })
    .filter(Boolean)
}

export function createFieldPermissionMap(source, options = {}) {
  const map = new Map()
  for (const item of normalizeFieldPermissions(source, options))
    map.set(item.field, item)
  return map
}

export function pickFirstNonEmptyFieldPermissions(sources = [], options = {}) {
  const list = Array.isArray(sources) ? sources : [sources]
  for (const source of list) {
    const permissions = normalizeFieldPermissions(source, options)
    if (permissions.length)
      return permissions
  }
  return []
}

function parsePermissionList(source) {
  if (typeof source === 'string') {
    const text = source.trim()
    if (!text)
      return []
    try {
      return parsePermissionList(JSON.parse(text))
    }
    catch {
      return []
    }
  }
  if (Array.isArray(source))
    return source
  if (source && typeof source === 'object') {
    if (Array.isArray(source.fields))
      return source.fields
    return []
  }
  return []
}

function readPermissionBoolean(value, fallback) {
  if (value === undefined || value === null || value === '')
    return fallback
  if (typeof value === 'boolean')
    return value
  if (typeof value === 'number')
    return value !== 0
  const text = String(value).trim().toLowerCase()
  if (['true', '1', 'yes', 'y'].includes(text))
    return true
  if (['false', '0', 'no', 'n'].includes(text))
    return false
  return fallback
}
