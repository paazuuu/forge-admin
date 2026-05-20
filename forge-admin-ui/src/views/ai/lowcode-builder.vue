<template>
  <div class="lowcode-builder-page">
    <div class="builder-header">
      <div class="header-main">
        <n-button quaternary @click="router.push('/ai/lowcode-apps')">
          应用列表
        </n-button>
        <div class="title-block">
          <h2>{{ draft.appName || draft.modelSchema.businessName || '低代码应用搭建器' }}</h2>
          <p>{{ draft.configKey || '保存草稿后生成应用ID' }}</p>
        </div>
      </div>
      <n-space>
        <n-button :loading="saving" @click="saveDraft">
          保存草稿
        </n-button>
        <n-button type="primary" @click="currentStep = 4">
          发布上线
        </n-button>
      </n-space>
    </div>

    <div class="top-config">
      <n-steps :current="currentStep" @update:current="currentStep = $event">
        <n-step title="数据模型" description="设计单表字段" />
        <n-step title="页面搭建" description="配置页面结构" />
        <n-step title="实时预览" description="模拟运行时效果" />
        <n-step title="发布上线" description="建表与菜单发布" />
      </n-steps>

      <n-form label-placement="left" label-width="72" size="small" class="config-form">
        <n-form-item label="应用名称">
          <n-input v-model:value="draft.appName" placeholder="例如：合同管理" />
        </n-form-item>
        <n-form-item label="配置键">
          <n-input
            v-model:value="draft.configKey"
            :disabled="!!appId"
            placeholder="contract_manage"
          />
        </n-form-item>
        <n-form-item label="菜单名称">
          <n-input v-model:value="draft.menuName" placeholder="默认同应用名称" />
        </n-form-item>
      </n-form>
    </div>

    <div class="builder-shell">
      <main class="builder-content">
        <LowcodeModelDesigner
          v-show="currentStep === 1"
          v-model="draft.modelSchema"
          @validated="syncPageSchema"
        />
        <LowcodePageBuilder
          v-show="currentStep === 2"
          v-model="draft.pageSchema"
          :model-schema="draft.modelSchema"
        />
        <LowcodePreviewPane
          v-show="currentStep === 3"
          :app-id="appId"
          :draft="draft"
        />
        <PublishPanel
          v-show="currentStep === 4"
          :app-id="appId"
          :draft="draft"
          @published="reloadDetail"
          @rolled-back="reloadDetail"
        />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { lowcodeAppDetail, lowcodeSaveDraft } from '@/api/lowcode-crud'
import LowcodeModelDesigner from '@/components/lowcode-builder/model/LowcodeModelDesigner.vue'
import { cloneSchema, createDefaultModelSchema, normalizeTableName } from '@/components/lowcode-builder/model/model-schema'
import LowcodePageBuilder from '@/components/lowcode-builder/page/LowcodePageBuilder.vue'
import { createDefaultPageSchema, syncPageSchemaWithModel } from '@/components/lowcode-builder/page/page-schema'
import LowcodePreviewPane from '@/components/lowcode-builder/preview/LowcodePreviewPane.vue'
import PublishPanel from '@/components/lowcode-builder/publish/PublishPanel.vue'

defineOptions({ name: 'AiLowcodeBuilder' })

const route = useRoute()
const router = useRouter()
const currentStep = ref(1)
const saving = ref(false)

const appId = computed(() => route.params.id || null)

const defaultModel = createDefaultModelSchema()
const draft = reactive({
  id: null,
  configKey: '',
  appName: '',
  menuName: '',
  menuSort: 0,
  modelSchema: defaultModel,
  pageSchema: createDefaultPageSchema(defaultModel),
})

watch(
  () => draft.appName,
  (value) => {
    if (value && !draft.modelSchema.businessName)
      draft.modelSchema.businessName = value
  },
)

watch(
  () => draft.modelSchema.businessName,
  (value) => {
    if (value && !draft.appName)
      draft.appName = value
    if (value && !draft.menuName)
      draft.menuName = value
  },
)

watch(
  () => draft.modelSchema.tableName,
  (value) => {
    const normalized = normalizeTableName(value)
    if (normalized !== value)
      draft.modelSchema.tableName = normalized
  },
)

watch(
  () => draft.modelSchema.appType,
  (value, oldValue) => {
    if (value === 'TREE') {
      draft.pageSchema.layoutType = 'tree-crud'
      syncPageSchema()
    }
    else if (oldValue === 'TREE' && draft.pageSchema.layoutType === 'tree-crud') {
      draft.pageSchema.layoutType = 'simple-crud'
      syncPageSchema()
    }
  },
)

onMounted(() => {
  if (appId.value)
    reloadDetail()
})

async function reloadDetail() {
  if (!appId.value)
    return
  const res = await lowcodeAppDetail(appId.value)
  const detail = res.data || {}
  draft.id = detail.id
  draft.configKey = detail.configKey || ''
  draft.appName = detail.appName || detail.tableComment || ''
  draft.menuName = detail.menuName || draft.appName
  draft.menuSort = detail.menuSort || 0
  draft.modelSchema = cloneSchema(detail.modelSchema || createDefaultModelSchema())
  draft.pageSchema = syncPageSchemaWithModel(detail.pageSchema || createDefaultPageSchema(draft.modelSchema), draft.modelSchema)
}

function syncPageSchema() {
  draft.pageSchema = syncPageSchemaWithModel(draft.pageSchema, draft.modelSchema)
}

async function saveDraft() {
  if (!draft.configKey) {
    window.$message?.warning('请先填写配置键')
    return
  }
  saving.value = true
  try {
    syncPageSchema()
    const res = await lowcodeSaveDraft({
      id: draft.id,
      configKey: draft.configKey,
      appName: draft.appName || draft.modelSchema.businessName,
      menuName: draft.menuName || draft.appName || draft.modelSchema.businessName,
      menuSort: draft.menuSort,
      modelSchema: draft.modelSchema,
      pageSchema: draft.pageSchema,
    })
    const id = res.data
    window.$message?.success('草稿已保存')
    if (!appId.value && id)
      router.replace(`/ai/lowcode-builder/${id}`)
    else
      reloadDetail()
  }
  catch (e) {
    window.$message?.error(e?.message || '保存草稿失败')
  }
  finally {
    saving.value = false
  }
}
</script>

<style scoped>
.lowcode-builder-page {
  min-height: 100%;
  background: #f8fafc;
}

.builder-header {
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.header-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.title-block h2 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.title-block p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.builder-shell {
  padding: 16px;
}

.top-config {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 520px;
  gap: 18px;
  align-items: start;
  margin: 16px 16px 0;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.config-form {
  display: grid;
  gap: 2px;
}

.builder-content {
  min-width: 0;
}

@media (max-width: 1180px) {
  .top-config {
    grid-template-columns: 1fr;
  }
}
</style>
