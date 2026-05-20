export const tableModeOptions = [
  { label: '创建新业务表', value: 'CREATE' },
  { label: '绑定已有表', value: 'EXISTING' },
]

export const appTypeOptions = [
  { label: '单表应用', value: 'SINGLE' },
  { label: '树形单表', value: 'TREE' },
]

export const dataTypeOptions = [
  { label: '短文本', value: 'varchar' },
  { label: '长文本', value: 'text' },
  { label: '整数', value: 'int' },
  { label: '长整数', value: 'bigint' },
  { label: '小数', value: 'decimal' },
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '开关值', value: 'tinyint' },
]

export const componentTypeOptions = [
  { label: '单行输入', value: 'input' },
  { label: '多行文本', value: 'textarea' },
  { label: '数字输入', value: 'number' },
  { label: '下拉选择', value: 'select' },
  { label: '单选', value: 'radio' },
  { label: '多选', value: 'checkbox' },
  { label: '开关', value: 'switch' },
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '图片上传', value: 'imageUpload' },
  { label: '文件上传', value: 'fileUpload' },
]

export const queryTypeOptions = [
  { label: '等于', value: 'eq' },
  { label: '包含', value: 'like' },
  { label: '大于等于', value: 'ge' },
  { label: '小于等于', value: 'le' },
  { label: '区间', value: 'between' },
  { label: '多值', value: 'in' },
]

export const sensitiveTypeOptions = [
  { label: '不敏感', value: 'NONE' },
  { label: '手机号', value: 'PHONE' },
  { label: '身份证', value: 'ID_CARD' },
  { label: '邮箱', value: 'EMAIL' },
  { label: '银行卡', value: 'BANK_CARD' },
  { label: '姓名', value: 'NAME' },
  { label: '地址', value: 'ADDRESS' },
]

export function createDefaultModelSchema() {
  return {
    appType: 'SINGLE',
    tableMode: 'CREATE',
    tableName: 'biz_lowcode_demo',
    businessName: '低代码应用',
    treeConfig: {
      keyField: 'id',
      parentField: 'parentId',
      labelField: 'name',
      childrenField: 'children',
      treeTitle: '树形导航',
    },
    fields: [
      createDefaultField('name', '名称'),
      {
        ...createDefaultField('status', '状态'),
        dataType: 'varchar',
        length: 32,
        componentType: 'select',
        dictType: 'common_status',
        queryType: 'eq',
      },
    ],
  }
}

export function createDefaultField(field = 'fieldName', label = '字段名称') {
  return {
    field,
    columnName: camelToSnake(field),
    label,
    dataType: 'varchar',
    length: 128,
    precision: 2,
    required: false,
    defaultValue: null,
    searchable: false,
    listVisible: true,
    formVisible: true,
    componentType: 'input',
    queryType: 'like',
    dictType: '',
    sensitiveType: 'NONE',
    encryptAlgorithm: '',
    sortable: false,
    width: 160,
    remark: '',
  }
}

export function normalizeFieldName(value) {
  const cleaned = String(value || '')
    .replace(/[^a-zA-Z0-9_]/g, ' ')
    .replace(/[_\s]+([a-zA-Z0-9])/g, (_, char) => char.toUpperCase())
    .replace(/^[^a-zA-Z]+/, '')

  if (!cleaned)
    return 'fieldName'
  return cleaned.charAt(0).toLowerCase() + cleaned.slice(1)
}

export function normalizeTableName(value) {
  const cleaned = String(value || '')
    .trim()
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/[^a-zA-Z0-9_]/g, '_')
    .replace(/_+/g, '_')
    .toLowerCase()
    .replace(/^[^a-z]+/, '')

  return cleaned || 'biz_lowcode_demo'
}

export function camelToSnake(value) {
  return String(value || '')
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/[^a-zA-Z0-9_]/g, '_')
    .replace(/_+/g, '_')
    .toLowerCase()
}

export function cloneSchema(value) {
  return JSON.parse(JSON.stringify(value))
}

export function isSameSchema(left, right) {
  return JSON.stringify(left || null) === JSON.stringify(right || null)
}

export function createFieldFromIndex(index) {
  return createDefaultField(`field${index}`, `业务字段${index}`)
}
