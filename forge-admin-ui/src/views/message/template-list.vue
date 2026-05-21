<template>
  <AiCrudPage
    ref="crudRef"
    api="/api/message/template"
    :api-config="{
      list: 'get@/api/message/template/page',
      detail: 'get@/api/message/template/:id',
      add: 'post@/api/message/template',
      update: 'put@/api/message/template',
      delete: 'delete@/api/message/template/:id',
    }"
    :search-schema="searchSchema"
    :columns="tableColumns"
    :edit-schema="editSchema"
    row-key="id"
    add-button-text="新增模板"
    :load-detail-on-edit="true"
  />
</template>

<script setup>
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'MessageTemplate' })

const MESSAGE_TYPE_DICT = 'sys_message_type'
const MESSAGE_CHANNEL_DICT = 'sys_message_channel'
const ENABLE_DISABLE_DICT = 'sys_enable_disable'

const crudRef = ref(null)

const { dict } = useDict(MESSAGE_TYPE_DICT, MESSAGE_CHANNEL_DICT, ENABLE_DISABLE_DICT)

const messageTypeOptions = computed(() => dict.value[MESSAGE_TYPE_DICT] || [])
const channelOptions = computed(() => dict.value[MESSAGE_CHANNEL_DICT] || [])
const enabledOptions = computed(() => toNumberOptions(dict.value[ENABLE_DISABLE_DICT]))

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'type',
    label: '消息类型',
    type: 'select',
    props: {
      placeholder: '请选择消息类型',
      options: messageTypeOptions.value,
    },
  },
  {
    field: 'keyword',
    label: '关键词',
    type: 'input',
    props: {
      placeholder: '请输入模板编码或名称',
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'templateCode',
    label: '模板编码',
    width: 150,
  },
  {
    prop: 'templateName',
    label: '模板名称',
    width: 150,
  },
  {
    prop: 'type',
    label: '消息类型',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: MESSAGE_TYPE_DICT, value: row.type, size: 'small' })
    },
  },
  {
    prop: 'titleTemplate',
    label: '标题模板',
    width: 200,
  },
  {
    prop: 'defaultChannel',
    label: '默认渠道',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: MESSAGE_CHANNEL_DICT, value: row.defaultChannel, size: 'small' })
    },
  },
  {
    prop: 'enabled',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(DictTag, { dictType: ENABLE_DISABLE_DICT, value: row.enabled, size: 'small' })
    },
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
  },
  {
    prop: 'action',
    label: '操作',
    width: 120,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置
const editSchema = computed(() => [
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'templateCode',
    label: '模板编码',
    type: 'input',
    rules: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
    props: {
      placeholder: '请输入模板编码，全局唯一',
    },
    editDisabled: true,
  },
  {
    field: 'templateName',
    label: '模板名称',
    type: 'input',
    rules: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入模板名称',
    },
  },
  {
    field: 'type',
    label: '消息类型',
    type: 'select',
    defaultValue: 'SYSTEM',
    rules: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
    props: {
      options: messageTypeOptions.value,
    },
  },
  {
    field: 'defaultChannel',
    label: '默认渠道',
    type: 'select',
    defaultValue: 'WEB',
    props: {
      options: channelOptions.value,
    },
  },
  {
    field: 'titleTemplate',
    label: '标题模板',
    type: 'textarea',
    span: 2,
    rules: [{ required: true, message: '请输入标题模板', trigger: 'blur' }],
    props: {
      placeholder: '请输入标题模板，支持变量占位符',
      rows: 2,
    },
  },
  {
    field: 'contentTemplate',
    label: '内容模板',
    type: 'textarea',
    span: 2,
    rules: [{ required: true, message: '请输入内容模板', trigger: 'blur' }],
    props: {
      placeholder: '请输入内容模板，支持变量占位符',
      rows: 6,
    },
  },
  {
    field: 'enabled',
    label: '是否启用',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: enabledOptions.value,
    },
  },
  {
    field: 'remark',
    label: '备注说明',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注说明',
      rows: 3,
    },
  },
])

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

// 编辑
function handleEdit(row) {
  crudRef.value?.handleEdit(row)
}

// 删除
function handleDelete(row) {
  crudRef.value?.handleDelete(row)
}
</script>

<style scoped>
:deep(.n-form-item-label) {
  font-weight: 500;
}
</style>
