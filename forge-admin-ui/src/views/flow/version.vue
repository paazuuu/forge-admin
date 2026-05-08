<template>
  <n-modal v-model:show="visible" preset="card" title="版本历史" style="width: 800px">
    <n-data-table
      :columns="columns"
      :data="versionList"
      :pagination="pagination"
      :loading="loading"
    />
  </n-modal>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { NButton, NTag } from 'naive-ui'
import versionApi from '@/api/version'

const props = defineProps({
  modelId: String,
})

const visible = ref(true)
const loading = ref(false)
const versionList = ref([])
const pagination = ref({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  onChange: page => {
    pagination.value.page = page
    loadVersionList()
  },
})

const columns = [
  { title: '版本号', key: 'version' },
  { title: '版本名称', key: 'versionName' },
  {
    title: '版本标记',
    key: 'versionTag',
    render: row => {
      const tagMap = {
        draft: { type: 'default', text: '草稿' },
        test: { type: 'info', text: '测试' },
        release: { type: 'success', text: '正式发布' },
        deprecated: { type: 'warning', text: '已废弃' },
      }
      const tag = tagMap[row.versionTag] || { type: 'default', text: row.versionTag }
      return h(NTag, { type: tag.type }, { default: () => tag.text })
    },
  },
  { title: '变更说明', key: 'changeDescription' },
  { title: '发布人', key: 'publishBy' },
  { title: '发布时间', key: 'publishTime' },
  {
    title: '操作',
    key: 'action',
    render: row => {
      return h('div', { class: 'flex gap-2' }, [
        h(NButton, { size: 'small', type: 'primary', onClick: () => handleDetail(row) }, { default: () => '详情' }),
        h(NButton, { size: 'small', type: 'warning', onClick: () => handleRevert(row) }, { default: () => '回退' }),
        h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' }),
      ])
    },
  },
]

const loadVersionList = async () => {
  loading.value = true
  try {
    const res = await versionApi.getVersionList(props.modelId, pagination.value.page, pagination.value.pageSize)
    versionList.value = res.data?.records || []
    pagination.value.itemCount = res.data?.total || 0
  } catch (error) {
    console.error('加载版本列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleDetail = row => {
  console.log('查看版本详情', row.id)
}

const handleRevert = row => {
  console.log('版本回退', row.version)
}

const handleDelete = async row => {
  try {
    await versionApi.deleteVersion(row.id)
    loadVersionList()
  } catch (error) {
    console.error('删除版本失败', error)
  }
}

onMounted(() => {
  loadVersionList()
})
</script>