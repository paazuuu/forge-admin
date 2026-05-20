<template>
  <div class="lowcode-apps-page">
    <div class="apps-header">
      <div>
        <h2>低代码应用</h2>
        <p>单表 CRUD 应用的草稿、发布和在线建表入口</p>
      </div>
      <div class="header-actions">
        <n-input
          v-model:value="keyword"
          clearable
          placeholder="搜索应用 / 表名 / configKey"
          style="width: 260px"
          @update:value="handleSearch"
        />
        <n-select
          v-model:value="publishStatus"
          clearable
          placeholder="发布状态"
          :options="statusOptions"
          style="width: 140px"
          @update:value="handleSearch"
        />
        <n-button type="primary" @click="createApp">
          新建应用
        </n-button>
      </div>
    </div>

    <n-spin :show="loading">
      <div v-if="apps.length" class="apps-grid">
        <div v-for="app in apps" :key="app.id" class="app-card">
          <div class="app-card-head">
            <div>
              <div class="app-name">
                {{ app.appName || app.tableComment || app.configKey }}
              </div>
              <div class="app-key">
                {{ app.configKey }} · {{ app.tableName || '-' }}
              </div>
            </div>
            <n-tag :type="app.publishStatus === 'PUBLISHED' ? 'success' : 'warning'" size="small" :bordered="false">
              {{ statusLabel(app.publishStatus) }}
            </n-tag>
          </div>
          <div class="app-meta">
            <span>草稿 v{{ app.draftVersion || 0 }}</span>
            <span>发布 v{{ app.publishedVersion || 0 }}</span>
            <span>{{ formatTime(app.updateTime) }}</span>
          </div>
          <div class="app-actions">
            <n-button size="small" type="primary" @click="openBuilder(app.id)">
              可视化搭建
            </n-button>
            <n-button size="small" :disabled="app.publishStatus !== 'PUBLISHED'" @click="openRuntime(app.configKey)">
              打开页面
            </n-button>
          </div>
        </div>
      </div>
      <n-empty v-else-if="!loading" description="暂无低代码应用">
        <template #extra>
          <n-button type="primary" @click="createApp">
            新建第一个应用
          </n-button>
        </template>
      </n-empty>
    </n-spin>

    <div v-if="total > 0" class="apps-pagination">
      <n-pagination
        v-model:page="pageNum"
        v-model:page-size="pageSize"
        :item-count="total"
        :page-sizes="[12, 24, 48]"
        show-size-picker
        @update:page="loadApps"
        @update:page-size="loadApps"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { lowcodeAppPage } from '@/api/lowcode-crud'

defineOptions({ name: 'AiLowcodeApps' })

const router = useRouter()
const loading = ref(false)
const apps = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const keyword = ref('')
const publishStatus = ref(null)

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已停用', value: 'STOPPED' },
]

onMounted(loadApps)

async function loadApps() {
  loading.value = true
  try {
    const res = await lowcodeAppPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      publishStatus: publishStatus.value || undefined,
    })
    apps.value = res.data?.records || []
    total.value = res.data?.total || 0
  }
  finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadApps()
}

function createApp() {
  router.push('/ai/lowcode-builder')
}

function openBuilder(id) {
  router.push(`/ai/lowcode-builder/${id}`)
}

function openRuntime(configKey) {
  const route = router.resolve(`/ai/crud-page/${configKey}`)
  window.open(route.href, '_blank')
}

function statusLabel(status) {
  return statusOptions.find(item => item.value === status)?.label || '草稿'
}

function formatTime(value) {
  if (!value)
    return ''
  const date = new Date(value)
  return `${date.getMonth() + 1}-${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.lowcode-apps-page {
  min-height: 100%;
  padding: 18px 22px;
  background: #f8fafc;
}

.apps-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.apps-header h2 {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
}

.apps-header p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}

.app-card {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.app-card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.app-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.app-key {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.app-meta,
.app-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-meta {
  color: #64748b;
  font-size: 12px;
}

.apps-pagination {
  display: flex;
  justify-content: center;
  padding: 18px 0 4px;
}
</style>
