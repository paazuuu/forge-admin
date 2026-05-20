export const pageZoneCatalog = [
  {
    zoneKey: 'search',
    componentKey: 'search-form',
    title: '查询页',
    desc: '查询集、重置、自定义筛选条件',
  },
  {
    zoneKey: 'table',
    componentKey: 'data-table',
    title: '列表页',
    desc: '列表列、导入导出、批量操作',
  },
  {
    zoneKey: 'edit',
    componentKey: 'edit-form',
    title: '编辑表单页',
    desc: '新增、编辑、必填校验、自由排版',
  },
  {
    zoneKey: 'detail',
    componentKey: 'detail-panel',
    title: '查询详情页',
    desc: '只读详情字段与业务动作',
  },
]

export const canvasComponentCatalog = [
  {
    group: 'business',
    componentKey: 'query-set',
    title: '查询集',
    desc: '选择查询字段、调整顺序',
    zones: ['search', 'table'],
    defaultWidth: 640,
    defaultHeight: 132,
    multiField: true,
  },
  {
    group: 'business',
    componentKey: 'custom-query',
    title: '自定义查询',
    desc: '高级查询入口',
    zones: ['search', 'table'],
    defaultWidth: 128,
    defaultHeight: 40,
  },
  {
    group: 'business',
    componentKey: 'import-button',
    title: '导入',
    desc: 'Excel 批量导入',
    zones: ['table'],
    defaultWidth: 104,
    defaultHeight: 40,
  },
  {
    group: 'business',
    componentKey: 'export-button',
    title: '导出',
    desc: 'Excel 动态导出',
    zones: ['table'],
    defaultWidth: 104,
    defaultHeight: 40,
  },
  {
    group: 'business',
    componentKey: 'add-button',
    title: '新增',
    desc: '打开新增表单',
    zones: ['table'],
    defaultWidth: 104,
    defaultHeight: 40,
  },
  {
    group: 'business',
    componentKey: 'save-button',
    title: '保存',
    desc: '提交表单数据',
    zones: ['edit'],
    defaultWidth: 104,
    defaultHeight: 40,
  },
  {
    group: 'business',
    componentKey: 'reset-button',
    title: '重置',
    desc: '清空当前表单',
    zones: ['search', 'edit'],
    defaultWidth: 104,
    defaultHeight: 40,
  },
  {
    group: 'data',
    componentKey: 'data-table',
    title: '数据列表',
    desc: '配置展示列和顺序',
    zones: ['table'],
    defaultWidth: 860,
    defaultHeight: 280,
    multiField: true,
  },
  {
    group: 'field',
    componentKey: 'field-input',
    title: '单行输入',
    desc: '文本字段',
    zones: ['search', 'edit'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-textarea',
    title: '多行文本',
    desc: '长文本字段',
    zones: ['edit'],
    defaultWidth: 580,
    defaultHeight: 98,
  },
  {
    group: 'field',
    componentKey: 'field-number',
    title: '数字输入',
    desc: '整数、小数、金额',
    zones: ['search', 'edit'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-select',
    title: '下拉选择',
    desc: '系统字典或枚举',
    zones: ['search', 'edit'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-date',
    title: '日期',
    desc: '日期选择',
    zones: ['search', 'edit'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-datetime',
    title: '日期时间',
    desc: '日期时间选择',
    zones: ['search', 'edit'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-switch',
    title: '开关',
    desc: '启用、禁用类字段',
    zones: ['edit'],
    defaultWidth: 220,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-upload',
    title: '文件上传',
    desc: '附件或图片',
    zones: ['edit'],
    defaultWidth: 300,
    defaultHeight: 72,
  },
  {
    group: 'field',
    componentKey: 'detail-field',
    title: '详情字段',
    desc: '只读字段展示',
    zones: ['detail'],
    defaultWidth: 300,
    defaultHeight: 58,
  },
]

export function createDefaultPageSchema(modelSchema) {
  const fields = modelSchema?.fields || []
  const isTree = modelSchema?.appType === 'TREE'
  const schema = {
    layoutType: isTree ? 'tree-crud' : 'simple-crud',
    zones: [
      {
        zoneKey: 'search',
        componentKey: 'search-form',
        enabled: true,
        fieldRefs: fields.filter(field => field.searchable).map(field => field.field),
        props: {},
      },
      {
        zoneKey: 'table',
        componentKey: 'data-table',
        enabled: true,
        fieldRefs: fields.filter(field => field.listVisible !== false).map(field => field.field),
        props: {
          showImport: true,
          showExport: true,
          hideBatchDelete: false,
          enableCustomQuery: true,
          ...(isTree
            ? {
                treeConfig: {
                  keyField: modelSchema?.treeConfig?.keyField || 'id',
                  parentField: modelSchema?.treeConfig?.parentField || 'parentId',
                  labelField: modelSchema?.treeConfig?.labelField || fields.find(field => field.field === 'name')?.field || fields[0]?.field || '',
                  childrenField: modelSchema?.treeConfig?.childrenField || 'children',
                  treeTitle: modelSchema?.treeConfig?.treeTitle || `${modelSchema?.businessName || '业务'}树`,
                },
              }
            : {}),
        },
      },
      {
        zoneKey: 'edit',
        componentKey: 'edit-form',
        enabled: true,
        fieldRefs: fields.filter(field => field.formVisible !== false).map(field => field.field),
        props: {},
      },
      {
        zoneKey: 'detail',
        componentKey: 'detail-panel',
        enabled: false,
        fieldRefs: fields.filter(field => field.formVisible !== false).map(field => field.field),
        props: {},
      },
    ],
  }
  return syncPageSchemaWithModel(schema, modelSchema)
}

export function syncPageSchemaWithModel(pageSchema, modelSchema) {
  const current = pageSchema || createDefaultPageSchema(modelSchema)
  const fields = modelSchema?.fields || []
  const fieldSet = new Set(fields.map(field => field.field))
  const zones = (current.zones || []).map((zone) => {
    const props = zone.props || {}
    const normalizedZone = {
      ...zone,
      fieldRefs: (zone.fieldRefs || []).filter(field => fieldSet.has(field)),
      props,
    }
    const canvas = normalizeZoneCanvas(normalizedZone, modelSchema)
    const formCreateRefs = normalizedZone.zoneKey === 'edit'
      ? resolveFieldRefsFromFormCreateRules(props.formCreateRule, fieldSet)
      : []
    const explicitRefs = (normalizedZone.fieldRefs || []).filter(field => fieldSet.has(field))
    const fieldRefs = formCreateRefs.length
      ? formCreateRefs
      : explicitRefs.length
        ? explicitRefs
        : resolveFieldRefsFromCanvas(canvas).filter(field => fieldSet.has(field))
    return {
      ...normalizedZone,
      fieldRefs,
      props: {
        ...normalizedZone.props,
        canvas,
      },
    }
  })

  for (const catalog of pageZoneCatalog) {
    if (!zones.some(zone => zone.zoneKey === catalog.zoneKey)) {
      const zone = {
        zoneKey: catalog.zoneKey,
        componentKey: catalog.componentKey,
        enabled: catalog.zoneKey !== 'detail',
        fieldRefs: [],
        props: {},
      }
      const canvas = normalizeZoneCanvas(zone, modelSchema)
      const formCreateRefs = zone.zoneKey === 'edit'
        ? resolveFieldRefsFromFormCreateRules(zone.props?.formCreateRule, fieldSet)
        : []
      zones.push({
        ...zone,
        fieldRefs: formCreateRefs.length
          ? formCreateRefs
          : resolveFieldRefsFromCanvas(canvas).filter(field => fieldSet.has(field)),
        props: {
          canvas,
        },
      })
    }
  }

  return {
    layoutType: current.layoutType || 'simple-crud',
    zones,
  }
}

export function resolveZoneTitle(zoneKey) {
  return pageZoneCatalog.find(item => item.zoneKey === zoneKey)?.title || zoneKey
}

export function resolveCanvasComponent(componentKey) {
  return canvasComponentCatalog.find(item => item.componentKey === componentKey) || null
}

export function resolveDefaultFieldComponentKey(field, zoneKey = 'edit') {
  if (zoneKey === 'detail')
    return 'detail-field'
  if (field?.componentType === 'textarea')
    return 'field-textarea'
  if (field?.componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field?.dataType))
    return 'field-number'
  if (field?.componentType === 'date' || field?.dataType === 'date')
    return 'field-date'
  if (field?.componentType === 'datetime' || field?.dataType === 'datetime')
    return 'field-datetime'
  if (field?.componentType === 'switch')
    return 'field-switch'
  if (field?.componentType === 'fileUpload' || field?.componentType === 'imageUpload')
    return 'field-upload'
  if (field?.dictType || ['select', 'radio', 'checkbox'].includes(field?.componentType))
    return 'field-select'
  return 'field-input'
}

export function createCanvasItem(payload = {}, context = {}) {
  const zoneKey = context.zoneKey || payload.zoneKey || 'edit'
  const fieldMap = new Map((context.fields || []).map(field => [field.field, field]))
  const field = payload.field || fieldMap.get(payload.fieldRef)
  const componentKey = payload.componentKey || resolveDefaultFieldComponentKey(field, zoneKey)
  const component = resolveCanvasComponent(componentKey)
  const width = Number(payload.w || payload.width || component?.defaultWidth || 280)
  const height = Number(payload.h || payload.height || component?.defaultHeight || 64)
  const fieldRefs = normalizeFieldRefs(payload.fieldRefs || payload.props?.fieldRefs || [])

  return {
    id: payload.id || createItemId(zoneKey, componentKey, field?.field),
    componentKey,
    label: payload.label || field?.label || component?.title || '组件',
    fieldRef: payload.fieldRef || field?.field || '',
    fieldRefs,
    x: Number(payload.x ?? context.x ?? 32),
    y: Number(payload.y ?? context.y ?? 32),
    w: width,
    h: height,
    zIndex: Number(payload.zIndex ?? context.zIndex ?? 1),
    locked: !!payload.locked,
    style: {
      labelWidth: 86,
      radius: 6,
      fill: '#ffffff',
      stroke: '#cbd5e1',
      ...payload.style,
    },
    props: {
      placeholder: field?.label ? `请输入${field.label}` : '',
      ...payload.props,
      ...(fieldRefs.length ? { fieldRefs } : {}),
    },
  }
}

export function normalizeZoneCanvas(zone, modelSchema) {
  const fields = modelSchema?.fields || []
  const oldCanvas = zone?.props?.canvas || {}
  const defaultCanvas = createDefaultCanvasForZone(zone?.zoneKey, fields, zone?.fieldRefs, modelSchema)
  const sourceItems = Array.isArray(oldCanvas.items) && oldCanvas.items.length
    ? oldCanvas.items
    : defaultCanvas.items
  const fieldSet = new Set(fields.map(field => field.field))
  const items = sourceItems
    .map(item => normalizeCanvasItem(item, zone?.zoneKey, fields))
    .filter(item => !item.fieldRef || fieldSet.has(item.fieldRef))
    .map((item) => {
      const refs = normalizeFieldRefs(item.fieldRefs || item.props?.fieldRefs || []).filter(ref => fieldSet.has(ref))
      const props = { ...(item.props || {}) }
      if (item.fieldRefs?.length || item.props?.fieldRefs)
        props.fieldRefs = refs
      return {
        ...item,
        fieldRefs: refs,
        props,
      }
    })

  return {
    width: Number(oldCanvas.width || defaultCanvas.width),
    height: Number(oldCanvas.height || defaultCanvas.height),
    snap: Number(oldCanvas.snap || 8),
    items,
  }
}

export function resolveFieldRefsFromCanvas(canvas) {
  const refs = []
  ;(canvas?.items || []).forEach((item) => {
    if (item.fieldRef)
      refs.push(item.fieldRef)
    refs.push(...normalizeFieldRefs(item.fieldRefs || item.props?.fieldRefs || []))
  })
  return Array.from(new Set(refs))
}

export function patchZoneCanvas(zone, canvasPatch = {}) {
  const canvas = {
    ...(zone?.props?.canvas || {}),
    ...canvasPatch,
  }
  const syncedProps = syncZoneBusinessProps(zone, canvas)
  return {
    ...zone,
    fieldRefs: resolveFieldRefsFromCanvas(canvas),
    props: {
      ...(zone?.props || {}),
      ...syncedProps,
      canvas,
    },
  }
}

export function resolveFieldRefsFromFormCreateRules(rules, fieldSet) {
  const refs = []
  const modelFields = fieldSet instanceof Set ? fieldSet : new Set(fieldSet || [])
  const walk = (items) => {
    ;(Array.isArray(items) ? items : []).forEach((rule) => {
      if (!rule || typeof rule !== 'object')
        return
      if (rule.field && modelFields.has(rule.field) && !refs.includes(rule.field))
        refs.push(rule.field)
      if (Array.isArray(rule.children))
        walk(rule.children)
    })
  }
  walk(rules)
  return refs
}

function createDefaultCanvasForZone(zoneKey, allFields, fieldRefs = [], modelSchema) {
  const fields = resolveDefaultFields(zoneKey, allFields, fieldRefs)
  if (zoneKey === 'table') {
    const tableRefs = fields.map(field => field.field)
    return {
      width: 1040,
      height: 460,
      snap: 8,
      items: [
        createCanvasItem({
          id: 'table_action_add',
          componentKey: 'add-button',
          label: '新增',
          x: 32,
          y: 28,
          zIndex: 1,
        }, { zoneKey, fields: allFields }),
        createCanvasItem({
          id: 'table_action_import',
          componentKey: 'import-button',
          label: '导入',
          x: 148,
          y: 28,
          zIndex: 2,
        }, { zoneKey, fields: allFields }),
        createCanvasItem({
          id: 'table_action_export',
          componentKey: 'export-button',
          label: '导出',
          x: 264,
          y: 28,
          zIndex: 3,
        }, { zoneKey, fields: allFields }),
        createCanvasItem({
          id: 'table_action_custom_query',
          componentKey: 'custom-query',
          label: '自定义查询',
          x: 380,
          y: 28,
          zIndex: 4,
        }, { zoneKey, fields: allFields }),
        createCanvasItem({
          id: 'table_data_grid',
          componentKey: 'data-table',
          label: `${modelSchema?.businessName || '业务'}列表`,
          fieldRefs: tableRefs,
          props: { fieldRefs: tableRefs },
          x: 32,
          y: 88,
          w: 940,
          h: 300,
          zIndex: 5,
        }, { zoneKey, fields: allFields }),
      ],
    }
  }

  if (zoneKey === 'search') {
    const searchRefs = fields.map(field => field.field)
    return {
      width: 1040,
      height: 300,
      snap: 8,
      items: [
        createCanvasItem({
          id: 'search_query_set',
          componentKey: 'query-set',
          label: '查询集',
          fieldRefs: searchRefs,
          props: { fieldRefs: searchRefs },
          x: 32,
          y: 36,
          w: 700,
          h: 132,
          zIndex: 1,
        }, { zoneKey, fields: allFields }),
        createCanvasItem({
          id: 'search_action_reset',
          componentKey: 'reset-button',
          label: '重置',
          x: 760,
          y: 82,
          zIndex: 2,
        }, { zoneKey, fields: allFields }),
        createCanvasItem({
          id: 'search_action_custom_query',
          componentKey: 'custom-query',
          label: '自定义查询',
          x: 876,
          y: 82,
          zIndex: 3,
        }, { zoneKey, fields: allFields }),
      ],
    }
  }

  const items = fields.map((field, index) => {
    const componentKey = resolveDefaultFieldComponentKey(field, zoneKey)
    const colCount = zoneKey === 'detail' ? 2 : 3
    const col = index % colCount
    const row = Math.floor(index / colCount)
    const width = componentKey === 'field-textarea' ? 580 : 280
    const height = componentKey === 'field-textarea' ? 98 : componentKey === 'detail-field' ? 58 : 64
    return createCanvasItem({
      id: `${zoneKey}_${safeKey(field.field)}`,
      componentKey,
      label: field.label || field.field,
      fieldRef: field.field,
      x: 32 + col * 308,
      y: 36 + row * (zoneKey === 'detail' ? 74 : 86),
      w: width,
      h: height,
      zIndex: index + 1,
    }, { zoneKey, fields: allFields })
  })

  if (zoneKey === 'edit') {
    const y = 36 + Math.ceil(items.length / 3) * 86
    items.push(
      createCanvasItem({
        id: 'edit_action_save',
        componentKey: 'save-button',
        label: '保存',
        x: 32,
        y,
        zIndex: items.length + 1,
      }, { zoneKey, fields: allFields }),
      createCanvasItem({
        id: 'edit_action_reset',
        componentKey: 'reset-button',
        label: '重置',
        x: 148,
        y,
        zIndex: items.length + 2,
      }, { zoneKey, fields: allFields }),
    )
  }

  return {
    width: 1040,
    height: Math.max(420, Math.max(...items.map(item => item.y + item.h), 0) + 48),
    snap: 8,
    items,
  }
}

function resolveDefaultFields(zoneKey, fields, fieldRefs) {
  const refSet = new Set(fieldRefs || [])
  if (refSet.size)
    return fields.filter(field => refSet.has(field.field))
  if (zoneKey === 'search')
    return fields.filter(field => field.searchable)
  if (zoneKey === 'table')
    return fields.filter(field => field.listVisible !== false)
  return fields.filter(field => field.formVisible !== false)
}

function normalizeCanvasItem(item, zoneKey, fields) {
  return createCanvasItem({
    ...item,
    id: item.id,
  }, {
    zoneKey,
    fields,
    x: item.x,
    y: item.y,
    zIndex: item.zIndex,
  })
}

function normalizeFieldRefs(refs) {
  return Array.from(new Set((Array.isArray(refs) ? refs : []).filter(Boolean)))
}

function syncZoneBusinessProps(zone, canvas) {
  const componentKeys = new Set((canvas?.items || []).map(item => item.componentKey))
  if (zone?.zoneKey !== 'table')
    return {}
  return {
    showImport: componentKeys.has('import-button'),
    showExport: componentKeys.has('export-button'),
    enableCustomQuery: componentKeys.has('custom-query'),
  }
}

function createItemId(zoneKey, componentKey, fieldRef) {
  if (fieldRef)
    return `${zoneKey}_${safeKey(fieldRef)}`
  const suffix = Math.random().toString(36).slice(2, 8)
  return `${zoneKey}_${safeKey(componentKey)}_${Date.now()}_${suffix}`
}

function safeKey(value) {
  return String(value || 'item').replace(/[^\w-]/g, '_')
}
