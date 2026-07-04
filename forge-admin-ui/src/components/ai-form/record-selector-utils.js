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

export function resolveSelectorSearchParams(searchParams = {}, context = {}) {
  const result = {}
  Object.entries(searchParams || {}).forEach(([key, value]) => {
    const resolved = resolveSelectorParamValue(value, context)
    if (!isEmptySelectorParam(resolved))
      result[key] = resolved
  })
  return result
}

export function normalizeRecordSelectorConfig(source = {}) {
  const field = source && typeof source === 'object' ? source : {}
  const config = mergeSelectorFragments(field)
  const suiteCode = firstText(
    config.suiteCode,
    config.businessSuiteCode,
    field.suiteCode,
    field.businessSuiteCode,
    field.props?.suiteCode,
    field.props?.businessSuiteCode,
    field.basicProps?.suiteCode,
    field.basicProps?.businessSuiteCode,
  )
  const objectCode = firstText(
    config.objectCode,
    config.businessObjectCode,
    config.targetObjectCode,
    config.targetEntityCode,
    config.candidateObjectCode,
    config.referenceObjectCode,
    config.refObjectCode,
    config.sourceObjectCode,
    config.targetCode,
    field.objectCode,
    field.businessObjectCode,
    field.targetObjectCode,
    field.targetEntityCode,
    field.candidateObjectCode,
    field.referenceObjectCode,
    field.refObjectCode,
    field.sourceObjectCode,
    field.targetCode,
    field.props?.objectCode,
    field.props?.businessObjectCode,
    field.props?.targetObjectCode,
    field.props?.targetEntityCode,
    field.props?.candidateObjectCode,
    field.props?.referenceObjectCode,
    field.props?.refObjectCode,
    field.props?.sourceObjectCode,
    field.props?.targetCode,
    field.basicProps?.objectCode,
    field.basicProps?.businessObjectCode,
    field.basicProps?.targetObjectCode,
    field.basicProps?.targetEntityCode,
    field.basicProps?.candidateObjectCode,
    field.basicProps?.referenceObjectCode,
    field.basicProps?.refObjectCode,
    field.basicProps?.sourceObjectCode,
    field.basicProps?.targetCode,
  )
  return {
    ...config,
    suiteCode,
    objectCode,
    businessObjectCode: firstText(config.businessObjectCode, field.businessObjectCode, field.props?.businessObjectCode, field.basicProps?.businessObjectCode, objectCode),
    targetObjectCode: firstText(config.targetObjectCode, field.targetObjectCode, field.props?.targetObjectCode, field.basicProps?.targetObjectCode, objectCode),
    targetEntityCode: firstText(config.targetEntityCode, field.targetEntityCode, field.props?.targetEntityCode, field.basicProps?.targetEntityCode, objectCode),
    candidateObjectCode: firstText(config.candidateObjectCode, field.candidateObjectCode, field.props?.candidateObjectCode, field.basicProps?.candidateObjectCode),
    referenceObjectCode: firstText(config.referenceObjectCode, field.referenceObjectCode, field.props?.referenceObjectCode, field.basicProps?.referenceObjectCode),
    refObjectCode: firstText(config.refObjectCode, field.refObjectCode, field.props?.refObjectCode, field.basicProps?.refObjectCode),
    sourceObjectCode: firstText(config.sourceObjectCode, field.sourceObjectCode, field.props?.sourceObjectCode, field.basicProps?.sourceObjectCode),
    targetCode: firstText(config.targetCode, field.targetCode, field.props?.targetCode, field.basicProps?.targetCode),
    title: firstText(config.title, config.selectorTitle, field.selectorTitle, field.title),
    buttonText: firstText(config.buttonText, field.buttonText),
    displayFields: firstArray(config.displayFields, field.displayFields, field.props?.displayFields, field.basicProps?.displayFields),
    keywordFields: firstArray(config.keywordFields, field.keywordFields, field.props?.keywordFields, field.basicProps?.keywordFields),
    fieldMappings: config.fieldMappings || config.mappings || field.fieldMappings || field.mappings || field.props?.fieldMappings || field.props?.mappings || field.basicProps?.fieldMappings || field.basicProps?.mappings || [],
    searchParams: config.searchParams || field.searchParams || field.props?.searchParams || field.basicProps?.searchParams || {},
  }
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

function resolveSelectorParamValue(value, context = {}) {
  if (Array.isArray(value))
    return value.map(item => resolveSelectorParamValue(item, context)).filter(item => !isEmptySelectorParam(item))
  if (value && typeof value === 'object') {
    return Object.entries(value).reduce((result, [key, item]) => {
      const resolved = resolveSelectorParamValue(item, context)
      if (!isEmptySelectorParam(resolved))
        result[key] = resolved
      return result
    }, {})
  }
  if (typeof value !== 'string')
    return value
  const text = value.trim()
  const matched = text.match(/^\$\{(.+)\}$/)
  if (!matched)
    return value
  return readSelectorContextPath(context, matched[1])
}

function readSelectorContextPath(context = {}, expression = '') {
  const source = {
    ...(context || {}),
    form: context.form || context.formData || {},
    formData: context.formData || context.form || {},
    record: context.record || context.formData || {},
    row: context.row || {},
    query: context.query || context.route?.query || {},
    params: context.params || context.route?.params || {},
  }
  return readPath(source, expression)
}

function isEmptySelectorParam(value) {
  if (value === null || value === undefined)
    return true
  if (typeof value === 'string')
    return value.trim() === ''
  if (Array.isArray(value))
    return value.length === 0
  if (value && typeof value === 'object')
    return Object.keys(value).length === 0
  return false
}

function mergeSelectorFragments(field = {}) {
  return [
    field.recordSelector,
    field.selector,
    field.selectorConfig,
    field.recordSelectorConfig,
    field.basicProps?.recordSelector,
    field.basicProps?.selector,
    field.basicProps?.selectorConfig,
    field.basicProps?.recordSelectorConfig,
    field.props?.recordSelector,
    field.props?.selector,
    field.props?.selectorConfig,
    field.props?.recordSelectorConfig,
  ].reduce((result, item) => {
    if (item && typeof item === 'object' && !Array.isArray(item))
      return { ...result, ...item }
    return result
  }, {})
}

function firstText(...values) {
  for (const value of values) {
    const text = String(value ?? '').trim()
    if (text)
      return text
  }
  return ''
}

function firstArray(...values) {
  for (const value of values) {
    if (Array.isArray(value))
      return value
    if (typeof value === 'string' && value.trim())
      return value.split(/[,\n]/).map(item => item.trim()).filter(Boolean)
  }
  return []
}
