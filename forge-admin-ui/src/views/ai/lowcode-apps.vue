<template>
  <div class="lowcode-domain-page">
    <DomainTreePanel
      v-model:keyword="domainKeyword"
      :domains="domains"
      :selected-domain-id="selectedDomainId"
      :loading="domainLoading"
      @select="selectDomain"
      @search="loadDomains"
      @refresh="refreshDomains"
      @create="openDomainEditor(null)"
    />

    <main class="domain-main">
      <div class="main-toolbar">
        <div>
          <h1>{{ pageTitle }}</h1>
          <p>{{ pageSubtitle }}</p>
        </div>
        <n-space>
          <n-button @click="router.push('/ai/lowcode-models')">
            数据模型设计
          </n-button>
          <n-button @click="refreshAll">
            刷新
          </n-button>
          <n-button type="primary" :disabled="selectedDomain?.status === 'DISABLED'" @click="createApp(selectedDomain)">
            新建应用
          </n-button>
        </n-space>
      </div>

      <section class="apps-board">
        <div class="apps-board-head">
          <div>
            <h2>低代码应用</h2>
            <p>{{ appScopeText }}</p>
          </div>
          <div class="filter-strip">
            <n-input
              v-model:value="keyword"
              clearable
              placeholder="搜索应用 / 表名 / configKey"
              @update:value="handleSearch"
            />
            <n-select
              v-model:value="publishStatus"
              clearable
              placeholder="发布状态"
              :options="statusOptions"
              @update:value="handleSearch"
            />
          </div>
        </div>

        <n-spin :show="loading">
          <div v-if="apps.length" class="apps-grid">
            <article v-for="app in apps" :key="app.id" class="app-card">
              <div class="app-topline">
                <n-tag size="small" :type="app.publishStatus === 'PUBLISHED' ? 'success' : 'warning'" :bordered="false">
                  {{ statusLabel(app.publishStatus) }}
                </n-tag>
                <span>{{ formatTime(app.updateTime) }}</span>
              </div>
              <div class="app-title-row">
                <div class="app-mark">
                  {{ appInitial(app) }}
                </div>
                <div>
                  <h3>{{ app.appName || app.tableComment || app.configKey }}</h3>
                  <p>{{ app.objectName || app.tableComment || '未命名对象' }}</p>
                </div>
              </div>
              <div class="app-code-grid">
                <div>
                  <span>领域</span>
                  <code>{{ app.domainName || app.domainCode || '-' }}</code>
                </div>
                <div>
                  <span>对象编码</span>
                  <code>{{ app.objectCode || '-' }}</code>
                </div>
                <div>
                  <span>配置键</span>
                  <code>{{ app.configKey }}</code>
                </div>
                <div>
                  <span>数据表</span>
                  <code>{{ app.tableName || '-' }}</code>
                </div>
              </div>
              <div class="app-version-row">
                <span>草稿 v{{ app.draftVersion || 0 }}</span>
                <span>发布 v{{ app.publishedVersion || 0 }}</span>
              </div>
              <div class="app-actions">
                <n-button size="small" type="primary" @click="openBuilder(app.id)">
                  搭建
                </n-button>
                <n-button size="small" :disabled="app.publishStatus !== 'PUBLISHED'" @click="openRuntime(app.configKey)">
                  打开
                </n-button>
                <n-button size="small" @click="openMoveDomain(app)">
                  迁移
                </n-button>
                <n-button size="small" type="error" secondary @click="deleteApp(app)">
                  删除
                </n-button>
              </div>
            </article>
          </div>
          <n-empty v-else-if="!loading" description="当前筛选下暂无低代码应用">
            <template #extra>
              <n-button type="primary" :disabled="selectedDomain?.status === 'DISABLED'" @click="createApp(selectedDomain)">
                新建应用
              </n-button>
            </template>
          </n-empty>
        </n-spin>

        <div v-if="total > 0" class="apps-pagination">
          <n-pagination
            v-model:page="pageNum"
            v-model:page-size="pageSize"
            :item-count="total"
            :page-sizes="[9, 18, 36]"
            show-size-picker
            @update:page="loadApps"
            @update:page-size="handlePageSizeChange"
          />
        </div>
      </section>
    </main>

    <DomainEditorDrawer
      v-model:show="domainEditorVisible"
      :domain="editingDomain"
      :domains="domains"
      @saved="handleDomainSaved"
    />

    <MoveDomainModal
      v-model:show="moveVisible"
      :app="movingApp"
      :domains="domains"
      @moved="handleMoved"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  lowcodeAppPage,
  lowcodeDeleteApp,
  lowcodeDomainTree,
} from '@/api/lowcode-crud'
import DomainEditorDrawer from '@/components/lowcode-builder/domain/DomainEditorDrawer.vue'
import DomainTreePanel from '@/components/lowcode-builder/domain/DomainTreePanel.vue'
import MoveDomainModal from '@/components/lowcode-builder/domain/MoveDomainModal.vue'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'AiLowcodeApps' })

const { dict } = useDict('lowcode_app_publish_status')

const router = useRouter()
const domainLoading = ref(false)
const loading = ref(false)
const domains = ref([])
const selectedDomainId = ref(null)
const selectedDomain = ref(null)
const apps = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(9)
const keyword = ref('')
const domainKeyword = ref('')
const publishStatus = ref(null)
const domainEditorVisible = ref(false)
const editingDomain = ref(null)
const moveVisible = ref(false)
const movingApp = ref(null)

const statusOptions = computed(() => dict.value.lowcode_app_publish_status || [])

const pageTitle = computed(() => '低代码应用')
const pageSubtitle = computed(() => {
  if (!selectedDomain.value)
    return '按业务领域组织低代码应用，统一管理对象、规则和发布入口'
  return selectedDomain.value.domainDesc || `领域编码：${selectedDomain.value.domainCode}`
})
const appScopeText = computed(() => selectedDomain.value ? `当前领域：${selectedDomain.value.domainName}` : '当前展示全部业务领域应用')

onMounted(async () => {
  await loadDomains()
  await loadApps()
})

async function loadDomains() {
  domainLoading.value = true
  try {
    const res = await lowcodeDomainTree({
      keyword: domainKeyword.value || undefined,
    })
    domains.value = res.data || []
  }
  finally {
    domainLoading.value = false
  }
}

async function refreshDomains() {
  await loadDomains()
}

async function selectDomain(domain) {
  pageNum.value = 1
  if (!domain) {
    selectedDomainId.value = null
    selectedDomain.value = null
    await loadApps()
    return
  }
  selectedDomainId.value = domain.id
  selectedDomain.value = domain
  await loadApps()
}

async function loadApps() {
  loading.value = true
  try {
    const res = await lowcodeAppPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      publishStatus: publishStatus.value || undefined,
      domainId: selectedDomainId.value || undefined,
    })
    apps.value = res.data?.records || []
    total.value = res.data?.total || 0
  }
  finally {
    loading.value = false
  }
}

async function refreshAll() {
  await Promise.all([
    loadDomains(),
    loadApps(),
  ])
}

function handleSearch() {
  pageNum.value = 1
  loadApps()
}

function handlePageSizeChange() {
  pageNum.value = 1
  loadApps()
}

function createApp(domain) {
  if (!domain) {
    window.$message?.warning('请先选择业务领域，再新建应用')
    return
  }
  if (domain?.status === 'DISABLED') {
    window.$message?.warning('停用领域不能新建应用')
    return
  }
  router.push({
    path: '/ai/lowcode-builder',
    query: domain
      ? {
          domainId: domain.id,
          domainCode: domain.domainCode,
          domainName: domain.domainName,
        }
      : {},
  })
}

function openBuilder(id) {
  router.push(`/ai/lowcode-builder/${id}`)
}

function openRuntime(configKey) {
  const route = router.resolve(`/ai/crud-page/${configKey}`)
  window.open(route.href, '_blank')
}

function openDomainEditor(domain) {
  editingDomain.value = domain ? { ...domain } : null
  domainEditorVisible.value = true
}

async function handleDomainSaved() {
  await refreshDomains()
}

function openMoveDomain(app) {
  movingApp.value = app
  moveVisible.value = true
}

function deleteApp(app) {
  window.$dialog.warning({
    title: '确认删除应用',
    content: `确定删除低代码应用“${app.appName || app.configKey}”吗？已发布菜单会同步删除，若菜单已被角色授权将无法删除。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await lowcodeDeleteApp(app.id)
        window.$message?.success('应用已删除')
        if (apps.value.length === 1 && pageNum.value > 1)
          pageNum.value -= 1
        await refreshAll()
      }
      catch (error) {
        window.$message?.error(error?.message || '应用删除失败')
      }
    },
  })
}

async function handleMoved() {
  moveVisible.value = false
  movingApp.value = null
  await refreshAll()
}

function statusLabel(status) {
  const item = dict.value.lowcode_app_publish_status?.find(d => d.value === status)
  return item?.label || '草稿'
}

function appInitial(app) {
  const text = app.appName || app.tableComment || app.configKey || '低'
  return text.slice(0, 1).toUpperCase()
}

function formatTime(value) {
  if (!value)
    return ''
  const date = new Date(value)
  return `${date.getMonth() + 1}-${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.lowcode-domain-page {
  display: grid;
  min-height: 100%;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  padding: 16px;
  background: #f3f6fa;
}

.domain-main {
  display: grid;
  min-width: 0;
  gap: 14px;
}

.main-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.main-toolbar h1 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.25;
}

.main-toolbar p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
}

.apps-board {
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.apps-board-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.apps-board-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
}

.apps-board-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.filter-strip {
  display: grid;
  width: min(520px, 50%);
  grid-template-columns: minmax(220px, 1fr) 150px;
  gap: 10px;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.app-card {
  display: grid;
  gap: 12px;
  min-width: 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff, #fbfdff);
  padding: 14px;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.app-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.app-topline,
.app-version-row,
.app-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-topline {
  justify-content: space-between;
  color: #94a3b8;
  font-size: 12px;
}

.app-title-row {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.app-mark {
  display: flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #0f172a;
  color: #86efac;
  font-size: 16px;
  font-weight: 800;
}

.app-title-row h3 {
  overflow: hidden;
  margin: 0;
  color: #0f172a;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-title-row p {
  overflow: hidden;
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-code-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.app-code-grid div {
  display: grid;
  min-width: 0;
  gap: 3px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #f8fafc;
  padding: 8px;
}

.app-code-grid span {
  color: #64748b;
  font-size: 11px;
}

.app-code-grid code {
  overflow: hidden;
  color: #0f172a;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-version-row {
  color: #64748b;
  font-size: 12px;
}

.app-actions {
  justify-content: flex-end;
}

.apps-pagination {
  display: flex;
  justify-content: center;
  padding: 18px 0 2px;
}

@media (max-width: 1380px) {
  .apps-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .lowcode-domain-page {
    grid-template-columns: 1fr;
  }

  .filter-strip {
    width: 100%;
  }
}

@media (max-width: 720px) {
  .lowcode-domain-page {
    padding: 10px;
  }

  .main-toolbar,
  .apps-board-head {
    flex-direction: column;
  }

  .filter-strip,
  .apps-grid {
    grid-template-columns: 1fr;
  }
}
</style>
