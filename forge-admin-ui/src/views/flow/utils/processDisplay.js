export function getRowDisplayTitle(row = {}) {
  return firstText(
    row.businessSummary,
    row.businessObjectName,
    row.processName,
    row.processTitle,
    row.modelName,
    row.processDefinitionName,
    row.taskName,
    row.title,
    row.processDefinitionKey,
    row.processDefKey,
    '-',
  )
}

export function getBusinessFormDisplayTitle(context = {}, fallback = '业务表单') {
  const objectName = firstText(context.businessObjectName, context.objectName, context.appName, context.businessName)
  const summary = firstText(context.businessSummary, context.summary)
  if (objectName && summary && !summary.includes(objectName))
    return `${objectName} · ${summary}`
  return firstText(summary, objectName, context.formName, fallback)
}

export function firstText(...values) {
  for (const value of values) {
    const text = String(Array.isArray(value) ? value[0] || '' : value ?? '').trim()
    if (text)
      return text
  }
  return ''
}
