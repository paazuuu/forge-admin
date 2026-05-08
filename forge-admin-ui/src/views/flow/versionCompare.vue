<template>
  <n-modal v-model:show="visible" preset="card" title="版本对比" style="width: 1000px">
    <n-space vertical>
      <n-select v-model:value="version1" :options="versionOptions" placeholder="选择版本1" />
      <n-select v-model:value="version2" :options="versionOptions" placeholder="选择版本2" />
      <n-button type="primary" @click="handleCompare">对比</n-button>
      <n-divider />
      <div v-if="diffResult">
        <n-h3>差异结果</n-h3>
        <n-space vertical>
          <div v-if="diffResult.addedNodes?.length">
            <n-text strong>新增节点：</n-text>
            <n-list>
              <n-list-item v-for="node in diffResult.addedNodes">
                {{ node.name }} ({{ node.id }})
              </n-list-item>
            </n-list>
          </div>
          <div v-if="diffResult.modifiedNodes?.length">
            <n-text strong>修改节点：</n-text>
            <n-list>
              <n-list-item v-for="node in diffResult.modifiedNodes">
                {{ node.oldName }} → {{ node.newName }}
              </n-list-item>
            </n-list>
          </div>
          <div v-if="diffResult.deletedNodes?.length">
            <n-text strong>删除节点：</n-text>
            <n-list>
              <n-list-item v-for="node in diffResult.deletedNodes">
                {{ node.name }} ({{ node.id }})
              </n-list-item>
            </n-list>
          </div>
        </n-space>
      </div>
    </n-space>
  </n-modal>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import versionApi from '@/api/version'

const props = defineProps({
  modelId: String,
})

const visible = ref(true)
const version1 = ref(null)
const version2 = ref(null)
const versionOptions = ref([])
const diffResult = ref(null)

const loadVersionOptions = async () => {
  try {
    const res = await versionApi.getVersionList(props.modelId, 1, 100)
    versionOptions.value = (res.data?.records || []).map(v => ({
      label: `${v.versionName} (${v.publishTime})`,
      value: v.version,
    }))
  } catch (error) {
    console.error('加载版本列表失败', error)
  }
}

const handleCompare = async () => {
  if (!version1.value || !version2.value) {
    return
  }
  try {
    const res = await versionApi.compareVersions({
      modelId: props.modelId,
      version1: version1.value,
      version2: version2.value,
    })
    diffResult.value = res.data
  } catch (error) {
    console.error('版本对比失败', error)
  }
}

onMounted(() => {
  loadVersionOptions()
})
</script>