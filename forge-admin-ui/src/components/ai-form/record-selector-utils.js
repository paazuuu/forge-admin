export function normalizeSelectorMappings(mappings = []) {
  if (!mappings)
    return {}
  if (!Array.isArray(mappings) && typeof mappings === 'object')
    return { ...mappings }
  return (Array.isArray(mappings) ? mappings : []).reduce((result, item) => {
    if (!item || typeof item !== 'object')
      return result
    const sourceField = String(item.sourceField || item.source || '').trim()
    const targetField = String(item.targetField || item.target || '').trim()
    if (sourceField && targetField)
      result[sourceField] = targetField
    return result
  }, {})
}

export function applyRecordFieldMappings(record = {}, mappings = {}) {
  const source = extractSelectorRawRecord(record)
  const normalizedMappings = normalizeSelectorMappings(mappings)
  const result = {}
  Object.entries(normalizedMappings).forEach(([sourceField, targetField]) => {
    result[targetField] = readPath(source, sourceField)
  })
  return result
}

export function extractSelectorRawRecord(record = {}) {
  return record && typeof record === 'object'
    ? { ...(record._raw || {}), ...record }
    : {}
}

export function readPath(source = {}, path = '') {
  const keys = String(path || '').split('.').filter(Boolean)
  let cursor = source
  for (const key of keys) {
    if (!cursor || typeof cursor !== 'object')
      return undefined
    cursor = cursor[key]
  }
  return cursor
}
