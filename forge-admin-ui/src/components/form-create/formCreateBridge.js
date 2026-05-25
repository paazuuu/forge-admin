import formCreate from '@form-create/element-ui'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@form-create/element-ui/src/style/index.css'

const INSTALL_KEY = '__forgeFormCreateInstalled'

export function installFormCreate(app) {
  if (!app || app._context.provides[INSTALL_KEY])
    return
  app.use(ElementPlus)
  app.use(formCreate)
  app._context.provides[INSTALL_KEY] = true
}

export function getFormCreateComponent() {
  return formCreate.$form()
}

export function normalizeFormCreateRules(source) {
  const rules = parseRules(source)
  return rules.map(rule => normalizeRule(rule)).filter(Boolean)
}

export function normalizeFormCreateOptions(source = {}) {
  const parsed = parseObject(source)
  return {
    ...buildDefaultFormOptions(),
    ...parsed,
    form: {
      ...buildDefaultFormOptions().form,
      ...(parsed.form || {}),
    },
  }
}

export function buildDefaultFormOptions() {
  return {
    form: {
      labelPosition: 'right',
      labelWidth: '110px',
      size: 'default',
    },
    submitBtn: false,
    resetBtn: false,
  }
}

export function cloneValue(value) {
  if (value === undefined)
    return undefined
  return JSON.parse(JSON.stringify(value ?? null))
}

function parseRules(source) {
  if (Array.isArray(source))
    return cloneValue(source)
  if (typeof source === 'string' && source.trim()) {
    try {
      const parsed = JSON.parse(source)
      return Array.isArray(parsed) ? parsed : []
    }
    catch {
      return []
    }
  }
  return []
}

function parseObject(source) {
  if (!source)
    return {}
  if (typeof source === 'string' && source.trim()) {
    try {
      const parsed = JSON.parse(source)
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
    }
    catch {
      return {}
    }
  }
  return typeof source === 'object' && !Array.isArray(source) ? cloneValue(source) : {}
}

function normalizeRule(rule) {
  if (!rule || typeof rule !== 'object')
    return null

  const next = cloneValue(rule)
  const isFormCreateRule = Boolean(next.title || next.validate || next.effect || next.control)

  if (!isFormCreateRule) {
    next.title = next.label || next.field || '字段'
    next.type = normalizeLegacyType(next.type)
    next.props = normalizeLegacyProps(next)
    next.validate = normalizeLegacyValidate(next)
    delete next.label
    delete next.rules
    delete next.required
    delete next.disabled
    delete next.defaultValue
  }
  else {
    next.type = normalizeLegacyType(next.type)
    next.props = normalizeRuleProps(next.props || {})
    if (!next.validate && Array.isArray(next.rules))
      next.validate = normalizeValidateRules(next.rules, next.title)
    delete next.rules
  }

  if (next.value === undefined && rule.defaultValue !== undefined)
    next.value = rule.defaultValue

  if (Array.isArray(next.children))
    next.children = next.children.map(child => normalizeRule(child)).filter(Boolean)

  return next
}

function normalizeLegacyType(type) {
  const typeMap = {
    inputNumber: 'inputNumber',
    input_number: 'inputNumber',
    date: 'datePicker',
    datetime: 'datePicker',
    datePicker: 'datePicker',
    textarea: 'textarea',
    timePicker: 'timePicker',
    treeSelect: 'tree',
    richText: 'input',
    colorPicker: 'input',
  }
  return typeMap[type] || type || 'input'
}

function normalizeLegacyProps(rule) {
  const props = normalizeRuleProps(rule.props || {})
  if (rule.disabled !== undefined)
    props.disabled = rule.disabled
  if (rule.type === 'textarea') {
    props.type = 'textarea'
    props.rows = props.rows || 3
  }
  if (rule.type === 'datePicker' && props.type === 'datetime') {
    props.valueFormat = props.valueFormat || 'YYYY-MM-DD HH:mm:ss'
    props.format = props.format || 'YYYY-MM-DD HH:mm:ss'
  }
  return props
}

function normalizeRuleProps(props) {
  const next = { ...props }
  if (next.maxLength !== undefined && next.maxlength === undefined) {
    next.maxlength = next.maxLength
    delete next.maxLength
  }
  if (next.showCount !== undefined && next.showWordLimit === undefined)
    next.showWordLimit = next.showCount
  if (next.checkedText !== undefined && next.activeText === undefined)
    next.activeText = next.checkedText
  if (next.uncheckedText !== undefined && next.inactiveText === undefined)
    next.inactiveText = next.uncheckedText
  if (next.maxCount !== undefined && next.limit === undefined)
    next.limit = next.maxCount
  return next
}

function normalizeLegacyValidate(rule) {
  const validate = normalizeValidateRules(rule.rules, rule.label || rule.field)
  if (rule.required && !validate.some(item => item.required)) {
    validate.push({
      required: true,
      message: `请填写${rule.label || rule.field || '字段'}`,
      trigger: ['blur', 'change'],
    })
  }
  return validate
}

function normalizeValidateRules(rules, title) {
  if (!Array.isArray(rules))
    return []
  return rules.map(item => ({
    ...item,
    message: item.message || `请填写${title || '字段'}`,
    trigger: item.trigger || ['blur', 'change'],
  }))
}
