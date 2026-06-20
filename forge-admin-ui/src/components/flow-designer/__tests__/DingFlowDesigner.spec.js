import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DingFlowDesigner from '../DingFlowDesigner.vue'

const DOLLAR = '$'
const SIMPLE_XML = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn">',
  '  <bpmn:process id="Process_T1" name="测试" isExecutable="true">',
  '    <bpmn:startEvent id="S"/>',
  `    <bpmn:userTask id="T1" name="审批" flowable:assignee="${DOLLAR}{initiator}"/>`,
  '    <bpmn:endEvent id="E"/>',
  '    <bpmn:sequenceFlow id="F1" sourceRef="S" targetRef="T1"/>',
  '    <bpmn:sequenceFlow id="F2" sourceRef="T1" targetRef="E"/>',
  '  </bpmn:process>',
  '</bpmn:definitions>',
].join('\n')

// 简化挂载：用 stubs 避免 Naive UI 全局组件未注册导致 warning 失控
const STUBS = {
  'n-drawer': { template: '<div class="n-drawer"><slot /></div>' },
  'n-drawer-content': { template: '<div class="n-drawer-content"><slot /><slot name="header" /><slot name="footer" /></div>' },
  'n-tabs': { template: '<div class="n-tabs"><slot /></div>' },
  'n-tab-pane': { template: '<div class="n-tab-pane"><slot /></div>' },
  'n-form-item': { template: '<div class="n-form-item"><slot /></div>' },
  'n-input': { template: '<input class="n-input" />' },
  'n-input-number': { template: '<input class="n-input-number" />' },
  'n-select': { template: '<select class="n-select"></select>' },
  'n-switch': { template: '<input type="checkbox" class="n-switch" />' },
  'n-button': { template: '<button class="n-button"><slot /></button>' },
  'n-radio': {
    props: ['checked', 'disabled'],
    emits: ['click'],
    template: '<label class="n-radio" @click="$emit(\'click\', $event)"><input type="radio" :checked="checked" :disabled="disabled"><slot /></label>',
  },
  'n-radio-group': { template: '<div class="n-radio-group"><slot /></div>' },
  'n-space': { template: '<div class="n-space"><slot /></div>' },
  'n-checkbox': { template: '<input type="checkbox" class="n-checkbox" /><slot />' },
  'n-divider': { template: '<hr class="n-divider" />' },
  'n-tag': { template: '<span class="n-tag"><slot /></span>' },
}

function mountDesigner(props = {}) {
  return mount(DingFlowDesigner, {
    props,
    global: { stubs: STUBS },
  })
}

describe('dingFlowDesigner - 基础 mount', () => {
  it('空 props 挂载默认 createEmptyFlow（start → end）', () => {
    const w = mountDesigner()
    const exposed = w.vm
    const json = exposed.designer.flowJson.value
    expect(json.nodes.map(n => n.nodeType)).toEqual(['start', 'end'])
    w.unmount()
  })

  it('暴露 setXML / getXML / reset / undo / redo', () => {
    const w = mountDesigner()
    const exposed = w.vm
    expect(typeof exposed.setXML).toBe('function')
    expect(typeof exposed.getXML).toBe('function')
    expect(typeof exposed.reset).toBe('function')
    expect(typeof exposed.undo).toBe('function')
    expect(typeof exposed.redo).toBe('function')
    w.unmount()
  })
})

describe('dingFlowDesigner - props.xml 输入加载', () => {
  it('xml prop 触发 import 后 nodes / edges 完整加载', async () => {
    const w = mountDesigner({ xml: SIMPLE_XML })
    await new Promise(r => setTimeout(r, 50))
    const json = w.vm.designer.flowJson.value
    expect(json.nodes.map(n => n.id)).toEqual(['S', 'T1', 'E'])
    expect(json.edges).toHaveLength(2)
    expect(json.nodes.find(n => n.id === 'T1').config.assignee).toBe(`${DOLLAR}{initiator}`)
    w.unmount()
  })

  it('getXML 返回当前流程的 BPMN XML，包含 process id', async () => {
    const w = mountDesigner({ xml: SIMPLE_XML })
    await new Promise(r => setTimeout(r, 50))
    const xml = w.vm.getXML()
    expect(xml).toContain('Process_T1')
    expect(xml).toContain('userTask')
    expect(xml).toContain(`${DOLLAR}{initiator}`)
    w.unmount()
  })

  it('xML → JSON → XML 往返保留关键节点', async () => {
    const w = mountDesigner({ xml: SIMPLE_XML })
    await new Promise(r => setTimeout(r, 50))
    const xml2 = w.vm.getXML()
    expect(xml2).toContain('startEvent')
    expect(xml2).toContain('userTask')
    expect(xml2).toContain('endEvent')
    expect(xml2).toContain('sequenceFlow')
    expect(xml2).toContain('id="F1"')
    expect(xml2).toContain('id="F2"')
    w.unmount()
  })
})

describe('dingFlowDesigner - reset / setXML', () => {
  it('reset 清空回 start → end 默认状态', async () => {
    const w = mountDesigner({ xml: SIMPLE_XML })
    await new Promise(r => setTimeout(r, 50))
    w.vm.reset()
    const json = w.vm.designer.flowJson.value
    expect(json.nodes.map(n => n.nodeType)).toEqual(['start', 'end'])
    w.unmount()
  })

  it('setXML 多次切换不报错', async () => {
    const w = mountDesigner()
    await w.vm.setXML(SIMPLE_XML)
    expect(w.vm.designer.flowJson.value.nodes.length).toBe(3)
    await w.vm.setXML(SIMPLE_XML)
    expect(w.vm.designer.flowJson.value.nodes.length).toBe(3)
    w.unmount()
  })
})

describe('dingFlowDesigner - readonly', () => {
  it('readonly=true 时不抛错', async () => {
    const w = mountDesigner({ xml: SIMPLE_XML, readonly: true })
    await new Promise(r => setTimeout(r, 50))
    expect(w.vm.designer.flowJson.value.nodes.length).toBe(3)
    w.unmount()
  })
})

describe('dingFlowDesigner - 发起节点配置', () => {
  it('发起人变量固定展示，且不再展示节点表单配置', async () => {
    const w = mountDesigner()
    await w.find('[data-node-type="start"]').trigger('click')
    await w.vm.$nextTick()

    const text = w.text()
    expect(text).toContain('当前登录用户')
    expect(text).toContain('initiator')
    expect(text).toContain('流程表单请在右侧流程信息')
    expect(text).not.toContain('发起人变量')
    expect(text).not.toContain('表单 Key')
    expect(text).not.toContain('表单 URL')
    w.unmount()
  })
})

describe('dingFlowDesigner - 网关分支配置', () => {
  it('新增条件网关后打开抽屉能看到两条可读分支', async () => {
    const w = mountDesigner()
    w.vm.designer.addNode('StartEvent_1', 'condition')
    await w.vm.$nextTick()

    await w.find('[data-node-type="condition"]').trigger('click')
    await w.vm.$nextTick()

    expect(w.text()).toContain('该网关共 2 条分支')
    expect(w.text()).toContain('分支 1')
    expect(w.text()).toContain('下游节点：分支1审批')
    expect(w.text()).not.toContain('→ Node_')
    w.unmount()
  })

  it('网关本身不显示普通添加按钮，分支节点可继续添加', async () => {
    const w = mountDesigner()
    w.vm.designer.addNode('StartEvent_1', 'condition')
    await w.vm.$nextTick()

    expect(w.findAll('.add-node-button-wrap')).toHaveLength(3)
    w.unmount()
  })
})
