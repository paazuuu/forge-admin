<template>
  <div class="app-center-page">
    <header class="page-head">
      <div class="page-title-block">
        <h1>应用总览</h1>
        <p>按业务域查看业务单元、访问入口和底座能力。</p>
      </div>
      <n-space class="head-actions" :wrap="true">
        <n-button secondary @click="router.push('/app-center/engines')">
          <template #icon>
            <n-icon><HardwareChipOutline /></n-icon>
          </template>
          能力中心
        </n-button>
        <n-dropdown trigger="click" :options="createOptions" @select="handleCreateSelect">
          <n-button type="primary">
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
            新建
          </n-button>
        </n-dropdown>
      </n-space>
    </header>

    <section class="app-center-layout">
      <aside class="suite-nav">
        <div class="suite-nav-head">
          <div>
            <strong>业务域</strong>
            <span>{{ suites.length }} 个业务域</span>
          </div>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button quaternary circle size="small" @click="loadAll">
                <template #icon>
                  <n-icon><RefreshOutline /></n-icon>
                </template>
              </n-button>
            </template>
            刷新
          </n-tooltip>
        </div>

        <n-input
          v-model:value="suiteKeyword"
          clearable
          size="small"
          class="suite-search"
          placeholder="搜索业务域"
        >
          <template #prefix>
            <n-icon><SearchOutline /></n-icon>
          </template>
        </n-input>

        <n-spin :show="loadingSuites">
          <div class="suite-nav-list">
            <button
              class="suite-nav-item"
              :class="{ active: !suiteCode }"
              type="button"
              @click="selectSuite(null)"
            >
              <span class="suite-mark all">
                <n-icon><GridOutline /></n-icon>
              </span>
              <span class="suite-nav-copy">
                <strong>全部业务域</strong>
                <small>{{ suiteObjectTotal }} 个业务单元 · {{ suiteAppTotal }} 个入口</small>
              </span>
            </button>

            <button
              v-for="suite in filteredSuites"
              :key="suite.id"
              class="suite-nav-item"
              :class="{ active: suiteCode === suite.suiteCode }"
              type="button"
              @click="selectSuite(suite)"
            >
              <span class="suite-mark" :class="{ 'has-icon': suite.icon }">
                <IconRenderer v-if="suite.icon" :icon="suite.icon" :size="22" />
                <template v-else>{{ suiteInitial(suite) }}</template>
              </span>
              <span class="suite-nav-copy">
                <strong>{{ suite.suiteName || suite.suiteCode }}</strong>
                <small>{{ suite.objectCount || 0 }} 个业务单元 · {{ suite.appCount || 0 }} 个入口</small>
              </span>
            </button>

            <n-empty
              v-if="suiteKeyword && !filteredSuites.length && !loadingSuites"
              size="small"
              description="没有匹配的业务域"
            />
          </div>
        </n-spin>
      </aside>

      <main class="workspace">
        <section class="workspace-head">
          <div class="selected-suite-title">
            <span class="suite-mark large" :class="{ 'has-icon': activeSuite?.icon }">
              <IconRenderer v-if="activeSuite?.icon" :icon="activeSuite.icon" :size="24" />
              <template v-else>{{ activeSuiteInitial }}</template>
            </span>
            <div>
              <h2>{{ activeSuiteName }}</h2>
              <p>{{ activeSuiteDescription }}</p>
            </div>
          </div>
          <n-space class="workspace-actions" :wrap="true">
            <n-dropdown
              trigger="click"
              :disabled="!activeSuite"
              :options="suiteActionOptions"
              @select="handleSuiteAction"
            >
              <n-button secondary :disabled="!activeSuite">
                <template #icon>
                  <n-icon><EllipsisVertical /></n-icon>
                </template>
                业务域操作
              </n-button>
            </n-dropdown>
          </n-space>
        </section>

        <section class="metric-grid">
          <div v-for="metric in metrics" :key="metric.label" class="metric-item">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </div>
        </section>

        <section class="workspace-toolbar">
          <AppFilterBar
            v-model:keyword="keyword"
            v-model:suite-code="suiteCode"
            v-model:app-type="appType"
            :suites="suites"
            :show-suite="false"
            @search="loadWorkspace"
            @refresh="loadWorkspace"
          />
        </section>

        <section class="workspace-content">
          <div class="content-head">
            <div>
              <h3>业务单元与访问入口</h3>
              <p>先按业务单元归集，再处理对应入口；独立入口会单独归类。</p>
            </div>
            <span>{{ businessUnitTotal }} 个业务单元</span>
          </div>

          <n-spin :show="workspaceLoading">
            <div v-if="pagedBusinessUnits.length" class="unit-grid">
              <BusinessUnitCard
                v-for="unit in pagedBusinessUnits"
                :key="unit.key"
                :unit="unit"
                :show-suite="!suiteCode"
                @open-object="openObject"
                @design-object="openObjectDesigner"
                @stats-object="openObjectStats"
                @toggle-object="toggleObject"
                @delete-object="deleteObject"
                @open-app="openApp"
                @config-app="openEditor"
                @toggle-app="toggleApp"
                @delete-app="deleteApp"
                @create-app="createAppForObject"
              />
            </div>
            <n-empty v-else-if="!workspaceLoading" description="当前筛选下暂无业务单元或访问入口" />
          </n-spin>

          <div v-if="businessUnitTotal > unitPagination.pageSize" class="card-pagination">
            <n-pagination
              v-model:page="unitPagination.page"
              v-model:page-size="unitPagination.pageSize"
              :item-count="businessUnitTotal"
              :page-sizes="unitPageSizeOptions"
              show-size-picker
              @update:page-size="handleUnitPageSizeChange"
            />
          </div>
        </section>
      </main>
    </section>

    <AppEditorDrawer
      v-model:show="editorVisible"
      :app="editingApp"
      :suites="suites"
      @saved="loadAll"
    />
    <BusinessObjectWizardDrawer
      v-model:show="objectWizardVisible"
      :suites="suites"
      :default-suite-code="suiteCode"
      @saved="handleObjectSaved"
    />
    <SuiteEditorDrawer
      v-model:show="suiteEditorVisible"
      :suite="editingSuite"
      @saved="handleSuiteSaved"
    />
    <BusinessObjectDesignerPage
      v-if="designerVisible"
      :key="designerMountKey"
      embedded
      :embedded-object-code="designingObject?.objectCode || ''"
      :embedded-object-id="designingObject?.id || null"
      :embedded-suite-code="designingObject?.suiteCode || suiteCode || ''"
      :initial-panel="designerPanel"
      @saved="loadAll"
      @close="closeObjectDesigner"
    />
  </div>
</template>

<script setup>
import {
  AddOutline,
  EllipsisVertical,
  GridOutline,
  HardwareChipOutline,
  RefreshOutline,
  SearchOutline,
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  businessAppDetail,
  businessAppList,
  businessAppOpenInfo,
  businessObjectList,
  businessSuiteSummary,
  deleteBusinessApp,
  deleteBusinessObject,
  deleteBusinessSuite,
  updateBusinessAppStatus,
  updateBusinessObjectStatus,
  updateBusinessSuiteStatus,
} from '@/api/business-app'
import IconRenderer from '@/components/IconRenderer.vue'
import AppEditorDrawer from './components/AppEditorDrawer.vue'
import AppFilterBar from './components/AppFilterBar.vue'
import BusinessObjectWizardDrawer from './components/BusinessObjectWizardDrawer.vue'
import BusinessUnitCard from './components/BusinessUnitCard.vue'
import SuiteEditorDrawer from './components/SuiteEditorDrawer.vue'
import BusinessObjectDesignerPage from './object-designer.[objectCode].vue'

const router = useRouter()
const message = useMessage()

const keyword = ref('')
const suiteKeyword = ref('')
const suiteCode = ref(null)
const appType = ref(null)
const suites = ref([])
const objects = ref([])
const apps = ref([])
const loadingSuites = ref(false)
const loadingObjects = ref(false)
const loadingApps = ref(false)
const editorVisible = ref(false)
const editingApp = ref(null)
const objectWizardVisible = ref(false)
const suiteEditorVisible = ref(false)
const editingSuite = ref(null)
const designerVisible = ref(false)
const designingObject = ref(null)
const designerPanel = ref('form')
const unitPageSizeOptions = [8, 16, 32, 48]
const unitPagination = ref({
  page: 1,
  pageSize: 8,
})

const createOptions = [
  {
    label: '新建业务域',
    key: 'suite',
  },
  {
    label: '新建业务单元',
    key: 'object',
  },
  {
    label: '新建访问入口',
    key: 'app',
  },
]

const activeSuite = computed(() => suites.value.find(item => item.suiteCode === suiteCode.value) || null)
const filteredSuites = computed(() => {
  const word = suiteKeyword.value.trim().toLowerCase()
  if (!word)
    return suites.value
  return suites.value.filter((item) => {
    const name = `${item.suiteName || ''} ${item.suiteCode || ''} ${item.description || ''}`.toLowerCase()
    return name.includes(word)
  })
})
const suiteObjectTotal = computed(() => suites.value.reduce((sum, item) => sum + Number(item.objectCount || 0), 0))
const suiteAppTotal = computed(() => suites.value.reduce((sum, item) => sum + Number(item.appCount || 0), 0))
const objectTotal = computed(() => objects.value.length)
const appTotal = computed(() => apps.value.length)
const enabledAppCount = computed(() => apps.value.filter(item => item.status === 1).length)
const openableAppCount = computed(() => apps.value.filter(isAppOpenable).length)
const workspaceLoading = computed(() => loadingObjects.value || loadingApps.value)
const activeSuiteName = computed(() => activeSuite.value?.suiteName || activeSuite.value?.suiteCode || '全部业务域')
const activeSuiteDescription = computed(() => {
  if (activeSuite.value?.description)
    return activeSuite.value.description
  if (activeSuite.value)
    return '查看当前业务域下的业务单元、入口和运行能力。'
  return '跨业务域聚合业务单元和访问入口，适合快速定位可用应用。'
})
const activeSuiteInitial = computed(() => activeSuite.value ? suiteInitial(activeSuite.value) : '全')
const designerMountKey = computed(() => `${designingObject.value?.objectCode || 'object'}_${designerPanel.value}`)
const metrics = computed(() => [
  { label: '业务单元', value: objectTotal.value },
  { label: '访问入口', value: appTotal.value },
  { label: '可直接打开', value: openableAppCount.value },
  { label: '已启用入口', value: enabledAppCount.value },
])
const suiteActionOptions = computed(() => [
  {
    label: '进入业务域',
    key: 'open',
  },
  {
    label: '编辑信息',
    key: 'edit',
  },
  {
    label: suiteStatusActionText(activeSuite.value),
    key: 'toggle',
  },
  {
    type: 'divider',
    key: 'divider',
  },
  {
    label: '删除业务域',
    key: 'delete',
  },
])
const appGroups = computed(() => {
  const groups = new Map()
  apps.value.forEach((app) => {
    const key = unitKey(app.suiteCode, app.objectCode)
    if (!key)
      return
    if (!groups.has(key))
      groups.set(key, [])
    groups.get(key).push(app)
  })
  return groups
})
const businessUnits = computed(() => {
  const units = new Map()
  objects.value.forEach((object) => {
    const key = unitKey(object.suiteCode, object.objectCode)
    if (!key)
      return
    units.set(key, {
      key,
      object,
      apps: appGroups.value.get(key) || [],
      synthetic: false,
      standalone: false,
    })
  })
  apps.value.forEach((app) => {
    const key = unitKey(app.suiteCode, app.objectCode)
    if (!key)
      return
    if (!units.has(key)) {
      units.set(key, {
        key,
        object: syntheticObjectFromApp(app),
        apps: appGroups.value.get(key) || [],
        synthetic: true,
        standalone: false,
      })
    }
  })

  const standaloneGroups = new Map()
  apps.value.forEach((app) => {
    if (unitKey(app.suiteCode, app.objectCode))
      return
    const key = `standalone:${app.suiteCode || 'all'}`
    if (!standaloneGroups.has(key)) {
      standaloneGroups.set(key, {
        key,
        object: {
          suiteCode: app.suiteCode,
          suiteName: app.suiteName,
          objectName: app.suiteName ? `${app.suiteName}独立入口` : '独立访问入口',
          description: '未绑定具体业务单元的页面、移动端或接口入口。',
        },
        apps: [],
        synthetic: true,
        standalone: true,
      })
    }
    standaloneGroups.get(key).apps.push(app)
  })

  return [...units.values(), ...standaloneGroups.values()]
    .filter(unit => !appType.value || unit.apps.length > 0)
    .sort(compareUnits)
})
const businessUnitTotal = computed(() => businessUnits.value.length)
const pagedBusinessUnits = computed(() => {
  const start = (unitPagination.value.page - 1) * unitPagination.value.pageSize
  return businessUnits.value.slice(start, start + unitPagination.value.pageSize)
})

watch([keyword, suiteCode, appType], () => {
  unitPagination.value.page = 1
  loadWorkspace()
})

watch(businessUnitTotal, (total) => {
  const maxPage = Math.max(1, Math.ceil(total / unitPagination.value.pageSize))
  if (unitPagination.value.page > maxPage)
    unitPagination.value.page = maxPage
})

onMounted(loadAll)

async function loadAll() {
  await loadSuites()
  await loadWorkspace()
}

async function loadWorkspace() {
  await Promise.all([loadObjects(), loadApps()])
}

async function loadSuites() {
  loadingSuites.value = true
  try {
    const res = await businessSuiteSummary()
    suites.value = res.data || []
  }
  finally {
    loadingSuites.value = false
  }
}

async function loadObjects() {
  loadingObjects.value = true
  try {
    const res = await businessObjectList({
      keyword: keyword.value,
      suiteCode: suiteCode.value,
    })
    objects.value = res.data || []
  }
  finally {
    loadingObjects.value = false
  }
}

async function loadApps() {
  loadingApps.value = true
  try {
    const res = await businessAppList({
      keyword: keyword.value,
      suiteCode: suiteCode.value,
      appType: appType.value,
    })
    apps.value = res.data || []
  }
  finally {
    loadingApps.value = false
  }
}

function suiteInitial(suite) {
  const text = String(suite?.suiteName || suite?.suiteCode || 'A').trim()
  if (/^[A-Z0-9]{2,4}$/.test(text))
    return text.slice(0, 3)
  return text.slice(0, 2).toUpperCase()
}

function suiteStatusActionText(suite) {
  return suite?.status === 0 ? '启用业务域' : '停用业务域'
}

function unitKey(currentSuiteCode, objectCode) {
  if (!currentSuiteCode || !objectCode)
    return ''
  return `${currentSuiteCode}::${objectCode}`
}

function syntheticObjectFromApp(app) {
  return {
    suiteCode: app.suiteCode,
    suiteName: app.suiteName,
    objectCode: app.objectCode,
    objectName: app.objectName || app.objectCode,
    description: '由访问入口关联的业务单元。',
    status: app.status,
  }
}

function compareUnits(a, b) {
  if (a.standalone !== b.standalone)
    return a.standalone ? 1 : -1
  if (!suiteCode.value) {
    const suiteCompare = String(a.object?.suiteName || a.object?.suiteCode || '')
      .localeCompare(String(b.object?.suiteName || b.object?.suiteCode || ''), 'zh-CN')
    if (suiteCompare !== 0)
      return suiteCompare
  }
  const sortCompare = Number(a.object?.sortOrder || 0) - Number(b.object?.sortOrder || 0)
  if (sortCompare !== 0)
    return sortCompare
  return String(a.object?.objectName || a.object?.objectCode || '')
    .localeCompare(String(b.object?.objectName || b.object?.objectCode || ''), 'zh-CN')
}

function isAppOpenable(app) {
  if (app.status !== 1)
    return false
  if (app.entryMode === 'RUNTIME')
    return Boolean(app.configKey || app.entryUrl)
  return Boolean(app.entryUrl)
}

function selectSuite(suite) {
  suiteCode.value = suite?.suiteCode || null
}

function handleCreateSelect(key) {
  if (key === 'suite') {
    openSuiteEditor(null)
    return
  }
  if (key === 'object') {
    openObjectWizard()
    return
  }
  if (key === 'app')
    openEditor(null)
}

function handleSuiteAction(key) {
  if (!activeSuite.value)
    return
  if (key === 'open') {
    openSuite(activeSuite.value)
    return
  }
  if (key === 'edit') {
    openSuiteEditor(activeSuite.value)
    return
  }
  if (key === 'toggle') {
    toggleSuite(activeSuite.value)
    return
  }
  if (key === 'delete')
    deleteSuite(activeSuite.value)
}

function openSuite(suite) {
  if (!suite?.suiteCode)
    return
  router.push(`/app-center/suite/${suite.suiteCode}`)
}

function handleUnitPageSizeChange(pageSize) {
  unitPagination.value.pageSize = pageSize
  unitPagination.value.page = 1
}

function openObject(object) {
  if (!object?.objectCode)
    return
  router.push({
    path: `/app-center/object/${object.objectCode}`,
    query: { suiteCode: object.suiteCode },
  })
}

function openObjectDesigner(object, panel = 'form') {
  if (!object?.id || !object?.objectCode)
    return
  designingObject.value = {
    ...object,
    suiteCode: object.suiteCode || suiteCode.value,
  }
  designerPanel.value = panel || 'form'
  designerVisible.value = true
}

async function closeObjectDesigner() {
  designerVisible.value = false
  designingObject.value = null
  await loadAll()
}

async function openEditor(app, object = null) {
  if (app?.id) {
    try {
      const res = await businessAppDetail(app.id)
      editingApp.value = { ...app, ...(res.data || {}) }
    }
    catch {
      editingApp.value = { ...app }
    }
  }
  else if (object?.objectCode) {
    editingApp.value = {
      suiteCode: object.suiteCode || suiteCode.value,
      objectCode: object.objectCode,
      appType: 'BUSINESS',
      entryMode: 'RUNTIME',
      configKey: object.configKey || '',
      appName: object.objectName ? `${object.objectName}入口` : '',
      description: object.objectName ? `打开${object.objectName}的列表或填报页面` : '',
    }
  }
  else if (object?.suiteCode) {
    editingApp.value = { suiteCode: object.suiteCode }
  }
  else {
    editingApp.value = suiteCode.value ? { suiteCode: suiteCode.value } : null
  }
  editorVisible.value = true
}

function createAppForObject(object) {
  openEditor(null, object)
}

function openObjectWizard() {
  objectWizardVisible.value = true
}

function openSuiteEditor(suite) {
  editingSuite.value = suite ? { ...suite } : null
  suiteEditorVisible.value = true
}

async function handleObjectSaved(payload) {
  suiteCode.value = payload?.suiteCode || suiteCode.value
  await loadAll()
  openObjectDesigner(payload, payload?.designerPanel || 'form')
}

async function handleSuiteSaved(payload) {
  suiteCode.value = payload?.suiteCode || suiteCode.value
  await loadAll()
}

async function openApp(app) {
  const res = await businessAppOpenInfo(app.id)
  const info = res.data || {}
  if (!info.canOpen) {
    message.warning(info.message || '访问入口暂不可打开')
    return
  }
  if (info.openType === 'EXTERNAL' || info.openType === 'H5') {
    window.open(info.targetUrl, '_blank', 'noopener,noreferrer')
    return
  }
  if (info.openType === 'API') {
    message.info('接口类型入口用于登记能力，不跳转独立页面')
    return
  }
  router.push(info.targetUrl)
}

function openObjectStats(object) {
  if (!object?.objectCode)
    return
  const query = {
    suiteCode: object?.suiteCode || suiteCode.value || undefined,
    objectCode: object?.objectCode || undefined,
  }
  if (object?.configKey)
    query.configKey = object.configKey
  router.push({
    path: '/app-center/stats',
    query,
  })
}

async function toggleApp(app) {
  await updateBusinessAppStatus(app.id, app.status === 1 ? 0 : 1)
  message.success(app.status === 1 ? '访问入口已停用' : '访问入口已启用')
  await loadAll()
}

async function toggleObject(object) {
  if (!object?.id)
    return
  await updateBusinessObjectStatus(object.id, object.status === 1 ? 0 : 1)
  message.success(object.status === 1 ? '业务单元已停用' : '业务单元已启用')
  await loadAll()
}

async function toggleSuite(suite) {
  if (!suite?.id)
    return
  await updateBusinessSuiteStatus(suite.id, suite.status === 1 ? 0 : 1)
  message.success(suite.status === 1 ? '业务域已停用' : '业务域已启用')
  await loadAll()
}

function deleteSuite(suite) {
  if (!suite?.id)
    return
  window.$dialog?.warning({
    title: '删除业务域',
    content: `确定删除“${suite.suiteName || suite.suiteCode}”吗？已存在业务单元或访问入口的业务域会被后端拦截。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteBusinessSuite(suite.id)
      message.success('业务域已删除')
      if (suiteCode.value === suite.suiteCode)
        suiteCode.value = null
      await loadAll()
    },
  })
}

function deleteObject(object) {
  if (!object?.id)
    return
  window.$dialog?.warning({
    title: '删除业务单元',
    content: `确定删除“${object.objectName || object.objectCode}”吗？已关联关系或访问入口的业务单元会被后端拦截。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteBusinessObject(object.id)
      message.success('业务单元已删除')
      await loadAll()
    },
  })
}

function deleteApp(app) {
  window.$dialog?.warning({
    title: '删除访问入口',
    content: `确定删除“${app.appName || app.appCode}”吗？删除后不会删除关联的业务单元或运行配置。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteBusinessApp(app.id)
      message.success('访问入口已删除')
      await loadAll()
    },
  })
}
</script>

<style scoped>
.app-center-page {
  min-height: 100%;
  background: #f6f8fb;
  padding: 20px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.head-actions {
  justify-content: flex-end;
  max-width: 620px;
}

.page-title-block h1,
.workspace-head h2,
.content-head h3 {
  margin: 0;
  color: #111827;
  font-weight: 700;
  letter-spacing: 0;
}

.page-title-block h1 {
  font-size: 24px;
}

.workspace-head h2 {
  font-size: 20px;
}

.content-head h3 {
  font-size: 16px;
}

.page-title-block p,
.workspace-head p,
.content-head p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.55;
}

.app-center-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.suite-nav,
.workspace-head,
.metric-grid,
.workspace-toolbar,
.workspace-content {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.suite-nav {
  position: sticky;
  top: 16px;
  display: grid;
  gap: 12px;
  max-height: calc(100vh - 140px);
  overflow: auto;
  padding: 14px;
}

.suite-nav-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.suite-nav-head strong,
.suite-nav-head span {
  display: block;
}

.suite-nav-head strong {
  color: #111827;
  font-size: 15px;
}

.suite-nav-head span {
  margin-top: 2px;
  color: #6b7280;
  font-size: 12px;
}

.suite-search {
  width: 100%;
}

.suite-nav-list {
  display: grid;
  gap: 8px;
}

.suite-nav-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 11px;
  align-items: center;
  width: 100%;
  min-height: 64px;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 8px;
  background: #f9fafb;
  padding: 10px;
  text-align: left;
  transition:
    background 160ms ease,
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.suite-nav-item:hover {
  border-color: #cbd5e1;
  background: #fff;
}

.suite-nav-item.active {
  border-color: #2f6feb;
  background: #f4f8ff;
  box-shadow: inset 3px 0 0 #2f6feb;
}

.suite-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 8px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 13px;
  font-weight: 700;
}

.suite-mark.has-icon {
  background: #f8fafc;
  color: #2563eb;
}

.suite-mark.all {
  background: #ecfdf5;
  color: #15803d;
  font-size: 20px;
}

.suite-mark.large {
  width: 50px;
  height: 50px;
  font-size: 14px;
}

.suite-nav-copy {
  min-width: 0;
}

.suite-nav-copy strong,
.suite-nav-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suite-nav-copy strong {
  color: #111827;
  font-size: 14px;
  line-height: 1.35;
}

.suite-nav-copy small {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.workspace {
  display: grid;
  min-width: 0;
  gap: 14px;
}

.workspace-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 16px;
}

.workspace-actions {
  justify-content: flex-end;
  max-width: 620px;
}

.selected-suite-title {
  display: grid;
  grid-template-columns: 50px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  min-width: 0;
}

.selected-suite-title h2,
.selected-suite-title p {
  overflow: hidden;
  text-overflow: ellipsis;
}

.selected-suite-title h2 {
  white-space: nowrap;
}

.selected-suite-title p {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 0;
  overflow: hidden;
}

.metric-item {
  min-width: 0;
  min-height: 76px;
  border-right: 1px solid #eef2f7;
  padding: 14px 16px;
}

.metric-item:last-child {
  border-right: 0;
}

.metric-item span,
.metric-item strong {
  display: block;
}

.metric-item span {
  color: #6b7280;
  font-size: 12px;
}

.metric-item strong {
  margin-top: 6px;
  color: #111827;
  font-size: 22px;
  line-height: 1.1;
}

.workspace-toolbar {
  background: #fbfcfe;
  padding: 12px;
}

.workspace-content {
  display: grid;
  min-width: 0;
  gap: 14px;
  padding: 14px;
}

.content-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.content-head span {
  flex: 0 0 auto;
  border-radius: 4px;
  background: #eef6ff;
  color: #2563eb;
  font-size: 12px;
  line-height: 24px;
  padding: 0 8px;
}

.workspace-content :deep(.n-spin-content) {
  display: block;
  width: 100%;
}

.unit-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 12px;
  align-items: stretch;
}

.card-pagination {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1180px) {
  .app-center-layout {
    grid-template-columns: 260px minmax(0, 1fr);
  }

  .unit-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-item:nth-child(2) {
    border-right: 0;
  }

  .metric-item:nth-child(-n + 2) {
    border-bottom: 1px solid #eef2f7;
  }
}

@media (max-width: 860px) {
  .page-head,
  .workspace-head {
    display: grid;
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .head-actions,
  .workspace-actions {
    justify-content: flex-start;
    max-width: none;
  }

  .app-center-layout {
    grid-template-columns: 1fr;
  }

  .suite-nav {
    position: static;
    max-height: none;
  }

  .suite-nav-list {
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  }
}

@media (max-width: 560px) {
  .app-center-page {
    padding: 12px;
  }

  .content-head {
    display: grid;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .metric-item,
  .metric-item:nth-child(2) {
    border-right: 0;
  }

  .metric-item:not(:last-child) {
    border-bottom: 1px solid #eef2f7;
  }

  .unit-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
