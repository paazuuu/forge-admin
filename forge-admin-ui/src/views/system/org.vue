<template>
  <div class="system-org-page">
    <!-- 组织列表 -->
    <div class="org-content">
      <AiCrudPage
        ref="crudRef"
        api="/system/org"
        :api-config="{
          list: 'get@/system/org/lazyTree',
          detail: 'post@/system/org/getById',
          add: 'post@/system/org/add',
          update: 'post@/system/org/edit',
          delete: 'post@/system/org/remove?id=:id',
        }"
        :search-schema="searchSchema"
        :columns="tableColumns"
        :edit-schema="editSchema"
        :before-render-list="beforeRenderList"
        :before-submit="beforeSubmit"
        row-key="id"
        :edit-grid-cols="2"
        modal-width="900px"
        :show-pagination="false"
        :lazy="false"
        :hide-batch-delete="true"
        add-button-text="新增组织"
        :table-props="{
          expandedRowKeys: expandedKeys,
          onUpdateExpandedRowKeys: handleExpandedKeysUpdate,
          onLoad: handleLoad,
        }"
      >
        <!-- 自定义工具栏 -->
        <template #toolbar-end>
          <n-button @click="toggleExpandLoaded">
            <template #icon>
              <i :class="expandLoaded ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
            </template>
            {{ expandLoaded ? '折叠已加载' : '展开已加载' }}
          </n-button>
        </template>
      </AiCrudPage>
    </div>
  </div>
</template>

<script setup>
import { computed, h, nextTick, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'

defineOptions({ name: 'SystemOrg' })

const { dict } = useDict('sys_org_type', 'sys_normal_disable')

const crudRef = ref(null)
const expandLoaded = ref(false)
const expandedKeys = ref([])
const parentOrgOptions = ref([{ label: '顶级组织', value: 0, key: 0 }])
const searchRegionOptions = ref([])
const editRegionOptions = ref([])
const defaultParentId = ref(0)

const searchSchema = computed(() => [
  {
    field: 'orgName',
    label: '组织名称',
    type: 'input',
    props: {
      placeholder: '请输入组织名称',
    },
  },
  {
    field: 'regionCode',
    label: '行政区划',
    type: 'treeSelect',
    props: {
      placeholder: '请选择行政区划',
      clearable: true,
      filterable: true,
    },
    options: () => searchRegionOptions.value,
  },
  {
    field: 'orgType',
    label: '组织类型',
    type: 'select',
    props: {
      placeholder: '请选择组织类型',
      options: dict.value.sys_org_type || [],
    },
  },
  {
    field: 'orgStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: dict.value.sys_normal_disable || [],
    },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'orgName',
    label: '组织名称',
    width: 200,
  },
  {
    prop: 'orgType',
    label: '组织类型',
    width: 100,
    render: (row) => {
      return h(DictTag, { options: dict.value.sys_org_type, value: row.orgType })
    },
  },
  {
    prop: 'leaderName',
    label: '负责人',
    width: 120,
  },
  {
    prop: 'phone',
    label: '联系电话',
    width: 130,
  },
  {
    prop: 'address',
    label: '地址',
    width: 200,
  },
  {
    prop: 'regionCode',
    label: '行政区划',
    width: 150,
    render: (row) => {
      if (!row.regionCode)
        return '-'
      const name = findRegionName(searchRegionOptions.value, row.regionCode)
      return name || row.regionCode
    },
  },
  {
    prop: 'sort',
    label: '排序',
    width: 80,
  },
  {
    prop: 'orgStatus',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(DictTag, { options: dict.value.sys_normal_disable, value: row.orgStatus })
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
    width: 150,
    fixed: 'right',
    actions: [
      { label: '新增', key: 'add', type: 'primary', onClick: handleAdd },
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

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
    field: 'parentId',
    label: '上级组织',
    type: 'treeSelect',
    defaultValue: defaultParentId.value,
    props: {
      placeholder: '请选择上级组织',
      clearable: true,
      filterable: true,
      defaultExpandAll: true,
    },
    options: () => parentOrgOptions.value,
  },
  {
    field: 'orgName',
    label: '组织名称',
    type: 'input',
    rules: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入组织名称',
    },
  },
  {
    field: 'orgType',
    label: '组织类型',
    type: 'radio',
    defaultValue: 2,
    rules: [{ required: true, type: 'number', message: '请选择组织类型', trigger: 'change' }],
    props: {
      options: dict.value.sys_org_type || [],
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
  {
    type: 'divider',
    label: '负责人信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'leaderName',
    label: '负责人',
    type: 'input',
    props: {
      placeholder: '请输入负责人姓名',
    },
  },
  {
    field: 'phone',
    label: '联系电话',
    type: 'input',
    rules: [
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
    ],
    props: {
      placeholder: '请输入联系电话',
    },
  },
  {
    field: 'address',
    label: '地址',
    type: 'input',
    span: 2,
    props: {
      placeholder: '请输入组织地址',
    },
  },
  {
    field: 'regionCode',
    label: '行政区划',
    type: 'treeSelect',
    props: {
      placeholder: '请选择行政区划',
      clearable: true,
      filterable: true,
    },
    options: () => editRegionOptions.value,
  },
  {
    type: 'divider',
    label: '状态配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'orgStatus',
    label: '组织状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: dict.value.sys_normal_disable || [],
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

onMounted(() => {
  loadParentOrgOptions()
})

async function loadParentOrgOptions() {
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
      parentOrgOptions.value = [
        { label: '顶级组织', value: 0, key: 0 },
        ...convertToTreeSelect(res.data || []),
      ]

      await loadRegionOptions()
    }
  }
  catch (error) {
    console.error('加载上级组织选项失败:', error)
  }
}

async function loadRegionOptions() {
  try {
    const res = await request.get('/system/region/treeAll', { params: { rootCode: '150000', dataRight: true } })
    if (res.code === 200) {
      const data = res.data || []
      searchRegionOptions.value = convertRegionToTreeSelect(data, false)
      editRegionOptions.value = convertRegionToTreeSelect(data, true)
    }
  }
  catch (error) {
    console.error('加载行政区划选项失败:', error)
  }
}

function convertRegionToTreeSelect(list, virtualDisabled = true) {
  return list.map((item) => {
    const node = {
      label: item.name,
      value: item.code,
      key: item.code,
    }
    if (virtualDisabled && item.code && item.code.endsWith('ALL')) {
      node.disabled = true
    }
    if (item.children && item.children.length > 0) {
      node.children = convertRegionToTreeSelect(item.children, virtualDisabled)
    }
    return node
  })
}

function findRegionName(options, code) {
  for (const item of options) {
    if (item.value === code)
      return item.label
    if (item.children) {
      const name = findRegionName(item.children, code)
      if (name)
        return name
    }
  }
  return null
}

function beforeRenderList(list) {
  return list.map(item => ({
    ...item,
    isLeaf: !item.hasChildren,
  }))
}

function handleLoad(node) {
  return new Promise((resolve) => {
    request.get(`/system/org/children/${node.id}`)
      .then((res) => {
        if (res.code === 200) {
          node.children = (res.data || []).map(item => ({
            ...item,
            isLeaf: !item.hasChildren,
          }))
        }
        resolve()
      })
      .catch(() => {
        resolve()
      })
  })
}

function handleExpandedKeysUpdate(keys) {
  expandedKeys.value = keys
}

function toggleExpandLoaded() {
  expandLoaded.value = !expandLoaded.value
  if (expandLoaded.value) {
    const tableData = crudRef.value?.getTableData() || []
    expandedKeys.value = getLoadedKeys(tableData)
  }
  else {
    expandedKeys.value = []
  }
}

function getLoadedKeys(list, keys = []) {
  list.forEach((item) => {
    keys.push(item.id)
    if (item.children && item.children.length > 0) {
      getLoadedKeys(item.children, keys)
    }
  })
  return keys
}

async function handleAdd(row) {
  await loadParentOrgOptions()

  defaultParentId.value = row ? row.id : 0

  crudRef.value?.showAdd()

  await nextTick()
  await nextTick()

  defaultParentId.value = 0
}

async function handleEdit(row) {
  await loadParentOrgOptions()
  crudRef.value?.showEdit(row)
}

function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除组织"${row.orgName}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/org/remove', null, { params: { id: row.id } })
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
.system-org-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.org-content {
  flex: 1;
  min-height: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.org-content :deep(.ai-crud-page) {
  height: 100%;
}

.dark .org-content {
  background: #0f172a !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}
</style>
