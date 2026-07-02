import { describe, expect, it } from 'vitest'
import { createForgeFieldTemplateComponent } from '../../forge-form-designer/designerLayoutFactory'
import { buildAutoFieldAssets } from '../autoFieldRegistry'
import {
  getDesignerComponent,
  insertDesignerComponent,
  normalizeFormDesignerSchema,
  updateDesignerComponent,
} from '../formDesignerSchema'

describe('formDesignerSchema', () => {
  it('keeps template component ids unique after editing the first component field binding', () => {
    let schema = normalizeFormDesignerSchema({
      formKey: 'test_form',
      formName: '测试表单',
      layout: { gridColumns: 2 },
      components: [],
    })

    const first = createForgeFieldTemplateComponent({ componentKey: 'input', label: '输入框' }, schema)
    schema = insertDesignerComponent(schema, { parentId: '', index: 0 }, first)
    const firstId = schema.components[0].id

    schema = updateDesignerComponent(schema, firstId, {
      label: '客户名称',
      fieldBinding: {
        ...schema.components[0].fieldBinding,
        fieldCode: 'customerName',
        columnName: 'customer_name',
      },
    })

    const second = createForgeFieldTemplateComponent({ componentKey: 'input', label: '输入框' }, schema)
    expect(second.id).not.toBe(firstId)

    schema = insertDesignerComponent(schema, { parentId: '', index: 1 }, second)
    schema = updateDesignerComponent(schema, second.id, { label: '第二个输入框' })

    expect(getDesignerComponent(schema, firstId).label).toBe('客户名称')
    expect(getDesignerComponent(schema, second.id).label).toBe('第二个输入框')
  })

  it('deduplicates repeated explicit component ids during normalization', () => {
    const schema = normalizeFormDesignerSchema({
      components: [
        {
          id: 'cmp_duplicate',
          componentKey: 'input',
          label: '字段一',
          fieldBinding: { fieldCode: 'fieldOne' },
        },
        {
          id: 'cmp_duplicate',
          componentKey: 'input',
          label: '字段二',
          fieldBinding: { fieldCode: 'fieldTwo' },
        },
      ],
    })

    expect(schema.components.map(item => item.id)).toEqual(['cmp_duplicate', 'cmp_duplicate_2'])
  })

  it('creates field assets for newly dragged bound field components', () => {
    const { fields, createdFields } = buildAutoFieldAssets({
      components: [
        {
          id: 'cmp_input',
          componentKey: 'input',
          label: '编码',
          fieldBinding: {
            mode: 'field',
            fieldCode: 'code',
            columnName: 'code',
            createIfMissing: true,
          },
        },
      ],
    }, [])

    expect(createdFields).toHaveLength(1)
    expect(fields[0]).toMatchObject({
      fieldName: '编码',
      fieldCode: 'code',
      columnName: 'code',
      fieldType: 'TEXT',
      dataType: 'varchar',
      componentType: 'input',
    })
  })
})
