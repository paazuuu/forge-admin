<template>
  <div class="publish-panel">
    <div class="publish-card">
      <div class="card-title">
        发布配置
      </div>
      <n-form label-placement="left" label-width="92" size="small">
        <n-form-item label="菜单名称">
          <n-input v-model:value="form.menuName" placeholder="发布后的菜单名称" />
        </n-form-item>
        <n-form-item label="菜单排序">
          <n-input-number v-model:value="form.menuSort" :min="0" style="width: 100%" />
        </n-form-item>
        <n-form-item label="建表方式">
          <n-radio-group v-model:value="form.deployMode">
            <n-radio-button value="SKIP_DDL">
              仅发布页面
            </n-radio-button>
            <n-radio-button value="ONLINE_CREATE_TABLE">
              在线建表/补字段
            </n-radio-button>
          </n-radio-group>
        </n-form-item>
        <n-alert v-if="form.deployMode === 'ONLINE_CREATE_TABLE'" type="warning" :bordered="false">
          在线 DDL 只允许创建新表或追加缺失字段，需要 ai:lowcode:deploy-ddl 权限。
        </n-alert>
        <n-checkbox v-if="form.deployMode === 'ONLINE_CREATE_TABLE'" v-model:checked="form.confirmOnlineDdl">
          已确认 DDL 预览结果和发布影响
        </n-checkbox>
        <n-space>
          <n-button :loading="ddlLoading" @click="previewDdl">
            DDL 预览
          </n-button>
          <n-button type="primary" :loading="publishing" :disabled="!appId" @click="publish">
            一键发布
          </n-button>
        </n-space>
      </n-form>
    </div>

    <div class="publish-card">
      <div class="card-title">
        DDL 预览
      </div>
      <n-empty v-if="!ddlPreview" description="点击 DDL 预览生成建表语句" />
      <template v-else>
        <n-alert
          v-for="warning in ddlPreview.warnings"
          :key="warning"
          type="warning"
          :bordered="false"
          class="ddl-warning"
        >
          {{ warning }}
        </n-alert>
        <pre v-for="ddl in ddlPreview.ddlStatements" :key="ddl" class="ddl-code">{{ ddl }}</pre>
      </template>
    </div>

    <VersionTimeline
      :versions="versions"
      :loading="versionLoading"
      @rollback="rollback"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import {
  lowcodeDdlPreview,
  lowcodePublish,
  lowcodeRollback,
  lowcodeVersions,
} from '@/api/lowcode-crud'
import VersionTimeline from './VersionTimeline.vue'

const props = defineProps({
  appId: {
    type: [String, Number],
    default: null,
  },
  draft: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['published', 'rolled-back'])

const form = reactive({
  menuName: '',
  menuSort: 0,
  deployMode: 'SKIP_DDL',
  confirmOnlineDdl: false,
})
const ddlPreview = ref(null)
const ddlLoading = ref(false)
const publishing = ref(false)
const versions = ref([])
const versionLoading = ref(false)

watch(
  () => props.draft,
  (value) => {
    form.menuName = value.menuName || value.appName || value.modelSchema?.businessName || ''
    form.menuSort = value.menuSort || 0
  },
  { immediate: true, deep: true },
)

onMounted(() => {
  loadVersions()
})

async function previewDdl() {
  ddlLoading.value = true
  try {
    const res = await lowcodeDdlPreview(props.draft.modelSchema)
    ddlPreview.value = res.data
  }
  catch (e) {
    window.$message?.error(e?.message || 'DDL 预览失败')
  }
  finally {
    ddlLoading.value = false
  }
}

async function publish() {
  if (!props.appId) {
    window.$message?.warning('请先保存草稿')
    return
  }
  publishing.value = true
  try {
    await lowcodePublish(props.appId, {
      deployMode: form.deployMode,
      confirmOnlineDdl: form.confirmOnlineDdl,
      menuName: form.menuName,
      menuSort: form.menuSort,
      modelSchema: props.draft.modelSchema,
      pageSchema: props.draft.pageSchema,
    })
    window.$message?.success('发布成功')
    await loadVersions()
    emit('published')
  }
  catch (e) {
    window.$message?.error(e?.message || '发布失败')
  }
  finally {
    publishing.value = false
  }
}

async function loadVersions() {
  if (!props.appId)
    return
  versionLoading.value = true
  try {
    const res = await lowcodeVersions(props.appId)
    versions.value = res.data || []
  }
  finally {
    versionLoading.value = false
  }
}

async function rollback(versionId) {
  if (!props.appId)
    return
  await lowcodeRollback(props.appId, versionId)
  window.$message?.success('回滚成功')
  await loadVersions()
  emit('rolled-back')
}
</script>

<style scoped>
.publish-panel {
  display: grid;
  grid-template-columns: minmax(360px, 0.8fr) minmax(0, 1.2fr) 340px;
  gap: 16px;
}

.publish-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.card-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
}

.ddl-warning {
  margin-bottom: 10px;
}

.ddl-code {
  max-height: 360px;
  overflow: auto;
  padding: 12px;
  border-radius: 6px;
  background: #0f172a;
  color: #dbeafe;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

@media (max-width: 1280px) {
  .publish-panel {
    grid-template-columns: 1fr;
  }
}
</style>
