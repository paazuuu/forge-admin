<template>
  <div class="system-post-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/post"
      :api-config="{
        list: 'get@/system/post/page',
        detail: 'post@/system/post/getById',
        add: 'post@/system/post/add',
        update: 'post@/system/post/edit',
        delete: 'post@/system/post/removeBatch',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="id"
      :edit-grid-cols="2"
      modal-width="800px"
      add-button-text="新增岗位"
      :hide-selection="false"
    />
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'

defineOptions({ name: 'SystemPost' })

const POST_TYPE_DICT = 'sys_post_type'
const NORMAL_DISABLE_DICT = 'sys_normal_disable'

const crudRef = ref(null)
const orgOptions = ref([])

const { dict } = useDict(POST_TYPE_DICT, NORMAL_DISABLE_DICT)

const postTypeOptions = computed(() => toNumberOptions(dict.value[POST_TYPE_DICT]))
const postStatusOptions = computed(() => toNumberOptions(dict.value[NORMAL_DISABLE_DICT]))

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'postName',
    label: '岗位名称',
    type: 'input',
    props: {
      placeholder: '请输入岗位名称',
    },
  },
  {
    field: 'postCode',
    label: '岗位编码',
    type: 'input',
    props: {
      placeholder: '请输入岗位编码',
    },
  },
  {
    field: 'postType',
    label: '岗位类型',
    type: 'select',
    props: {
      placeholder: '请选择岗位类型',
      options: postTypeOptions.value,
    },
  },
  {
    field: 'postStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: postStatusOptions.value,
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'postCode',
    label: '岗位编码',
    width: 150,
  },
  {
    prop: 'postName',
    label: '岗位名称',
    width: 180,
  },
  {
    prop: 'postType',
    label: '岗位类型',
    width: 120,
    render: (row) => {
      return h(DictTag, { dictType: POST_TYPE_DICT, value: row.postType, size: 'small' })
    },
  },
  {
    prop: 'sort',
    label: '排序',
    width: 80,
  },
  {
    prop: 'postStatus',
    label: '状态',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: NORMAL_DISABLE_DICT, value: row.postStatus, size: 'small' })
    },
  },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 150,
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
  // 基础信息
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'postCode',
    label: '岗位编码',
    type: 'input',
    rules: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
    props: {
      placeholder: '请输入岗位编码',
    },
  },
  {
    field: 'postName',
    label: '岗位名称',
    type: 'input',
    rules: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入岗位名称',
    },
  },
  {
    field: 'orgId',
    label: '所属组织',
    type: 'treeSelect',
    required: true,
    rules: [{ required: true, type: 'number', message: '请选择所属组织', trigger: 'change' }],
    props: {
      placeholder: '请选择所属组织',
      clearable: true,
      filterable: true,
      defaultExpandAll: true,
    },
    options: () => orgOptions.value,
  },
  {
    field: 'postType',
    label: '岗位类型',
    type: 'radio',
    defaultValue: 2,
    rules: [{ required: true, type: 'number', message: '请选择岗位类型', trigger: 'change' }],
    props: {
      options: postTypeOptions.value,
    },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'input-number',
    defaultValue: 0,
    props: {
      placeholder: '排序值',
      min: 0,
    },
  },

  // 状态配置
  {
    type: 'divider',
    label: '状态配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'postStatus',
    label: '岗位状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: postStatusOptions.value,
    },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注',
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

// 组件挂载时加载组织选项
onMounted(() => {
  loadOrgOptions()
})

// 加载组织选项
async function loadOrgOptions() {
  try {
    const res = await request.get('/system/org/tree')
    if (res.code === 200) {
      const convertToTreeSelect = (list) => {
        return list.map(item => ({
          label: item.orgName,
          value: item.id,
          key: item.id,
          children: item.children && item.children.length > 0
            ? convertToTreeSelect(item.children)
            : undefined,
        }))
      }
      orgOptions.value = convertToTreeSelect(res.data || [])
    }
  }
  catch (error) {
    console.error('加载组织选项失败:', error)
  }
}

// 编辑
async function handleEdit(row) {
  // 加载组织选项
  await loadOrgOptions()

  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除岗位"${row.postName}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/post/remove', null, { params: { id: row.id } })
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error('删除失败')
      }
    },
  })
}
</script>

<style scoped>
.system-post-page {
  height: 100%;
}
</style>
