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
          <div class="suite-nav-actions">
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-button quaternary circle size="small" :disabled="!hasExpandableSuites" @click="expandAllSuites">
                  <template #icon>
                    <n-icon><ChevronDownOutline /></n-icon>
                  </template>
                </n-button>
              </template>
              展开全部
            </n-tooltip>
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-button quaternary circle size="small" :disabled="!hasExpandableSuites" @click="collapseAllSuites">
                  <template #icon>
                    <n-icon><ChevronForwardOutline /></n-icon>
                  </template>
                </n-button>
              </template>
              收起全部
            </n-tooltip>
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

        <n-spin :show="loadingSuites && !bootstrapping">
          <div class="suite-nav-list">
            <template v-if="bootstrapping">
              <div v-for="idx in 5" :key="idx" class="suite-skeleton-row">
                <n-skeleton circle :width="38" :height="38" />
                <div>
                  <n-skeleton text :width="idx === 1 ? '62%' : '78%'" />
                  <n-skeleton text :width="idx === 1 ? '82%' : '58%'" />
                </div>
              </div>
            </template>
            <template v-else>
              <div
                class="suite-nav-item"
                :class="{ active: !suiteCode }"
                role="button"
                tabindex="0"
                @click="selectSuite(null)"
                @keydown.enter.prevent="selectSuite(null)"
                @keydown.space.prevent="selectSuite(null)"
              >
                <span class="suite-tree-control" />
                <span class="suite-mark all">
                  <n-icon><GridOutline /></n-icon>
                </span>
                <span class="suite-nav-copy">
                  <strong>全部业务域</strong>
                  <small>{{ suiteObjectTotal }} 个业务单元 · {{ suiteAppTotal }} 个入口</small>
                </span>
              </div>

              <div
                v-for="suiteRow in filteredSuiteRows"
                :key="suiteRow.suite.id"
                class="suite-nav-item"
                :class="{
                  'active': suiteCode === suiteRow.suite.suiteCode,
                  'child': suiteRow.level > 0,
                  'has-children': suiteRow.hasChildren,
                }"
                :style="{ '--suite-indent': `${suiteRow.level * 18}px` }"
                role="button"
                tabindex="0"
                @click="selectSuite(suiteRow.suite)"
                @keydown.enter.prevent="selectSuite(suiteRow.suite)"
                @keydown.space.prevent="selectSuite(suiteRow.suite)"
              >
                <button
                  v-if="suiteRow.hasChildren"
                  class="suite-tree-toggle"
                  type="button"
                  :aria-label="isSuiteTreeExpanded(suiteRow.suite) ? '收起子业务域' : '展开子业务域'"
                  @click.stop="toggleSuiteExpanded(suiteRow.suite)"
                  @keydown.enter.stop.prevent="toggleSuiteExpanded(suiteRow.suite)"
                  @keydown.space.stop.prevent="toggleSuiteExpanded(suiteRow.suite)"
                >
                  <n-icon>
                    <ChevronDownOutline v-if="isSuiteTreeExpanded(suiteRow.suite)" />
                    <ChevronForwardOutline v-else />
                  </n-icon>
                </button>
                <span v-else class="suite-tree-control" />
                <span class="suite-mark" :class="{ 'has-icon': suiteRow.suite.icon }">
                  <IconRenderer v-if="suiteRow.suite.icon" :icon="suiteRow.suite.icon" :size="22" />
                  <template v-else>{{ suiteInitial(suiteRow.suite) }}</template>
                </span>
                <span class="suite-nav-copy">
                  <strong>{{ suiteRow.suite.suiteName || suiteRow.suite.suiteCode }}</strong>
                  <small>{{ suiteMetaText(suiteRow) }}</small>
                </span>
                <div class="suite-item-actions" @click.stop>
                  <n-dropdown
                    trigger="click"
                    :options="getSuiteActionOptions(suiteRow.suite)"
                    @select="key => handleSuiteAction(key, suiteRow.suite)"
                  >
                    <n-button quaternary circle size="small" class="suite-item-more" aria-label="业务域操作">
                      <template #icon>
                        <n-icon><EllipsisVertical /></n-icon>
                      </template>
                    </n-button>
                  </n-dropdown>
                </div>
              </div>

              <n-empty
                v-if="suiteKeyword && !filteredSuiteRows.length && !loadingSuites"
                size="small"
                description="没有匹配的业务域"
              />
            </template>
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
        </section>

        <section class="metric-grid">
          <template v-if="bootstrapping">
            <div v-for="idx in 4" :key="idx" class="metric-item metric-skeleton">
              <n-skeleton text :width="idx % 2 ? '46%' : '58%'" />
              <n-skeleton text :width="idx % 2 ? '34%' : '42%'" />
            </div>
          </template>
          <template v-else>
            <div v-for="metric in metrics" :key="metric.label" class="metric-item">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
            </div>
          </template>
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

          <n-spin :show="workspaceLoading && !bootstrapping">
            <div v-if="bootstrapping" class="unit-grid">
              <div v-for="idx in 6" :key="idx" class="unit-skeleton-card">
                <div class="unit-skeleton-head">
                  <n-skeleton circle :width="34" :height="34" />
                  <div>
                    <n-skeleton text :width="idx % 2 ? '72%' : '58%'" />
                    <n-skeleton text :width="idx % 2 ? '48%' : '64%'" />
                  </div>
                </div>
                <n-skeleton text :repeat="2" />
                <div class="unit-skeleton-actions">
                  <n-skeleton text width="28%" />
                  <n-skeleton text width="24%" />
                  <n-skeleton text width="20%" />
                </div>
              </div>
            </div>
            <div v-else-if="pagedBusinessUnits.length" class="unit-grid">
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
                @code-app="openCodePanel"
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
    <AppCodePanel
      v-model:show="codePanelVisible"
      :app="codingApp"
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
      :suites="suites"
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
  ChevronDownOutline,
  ChevronForwardOutline,
  EllipsisVertical,
  GridOutline,
  HardwareChipOutline,
  RefreshOutline,
  SearchOutline,
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
import AppFilterBar from './components/AppFilterBar.vue'
import BusinessUnitCard from './components/BusinessUnitCard.vue'

const AppCodePanel = defineAsyncComponent(() => import('./components/AppCodePanel.vue'))
const AppEditorDrawer = defineAsyncComponent(() => import('./components/AppEditorDrawer.vue'))
const BusinessObjectWizardDrawer = defineAsyncComponent(() => import('./components/BusinessObjectWizardDrawer.vue'))
const SuiteEditorDrawer = defineAsyncComponent(() => import('./components/SuiteEditorDrawer.vue'))
const BusinessObjectDesignerPage = defineAsyncComponent(() => import('./object-designer.[objectCode].vue'))

const router = useRouter()
const route = useRoute()
const message = useMessage()

const keyword = ref('')
const suiteKeyword = ref('')
const suiteCode = ref(null)
const appType = ref(null)
const suites = ref([])
const collapsedSuiteIds = ref(new Set())
const objects = ref([])
const apps = ref([])
const loadingSuites = ref(false)
const loadingObjects = ref(false)
const loadingApps = ref(false)
const bootstrapping = ref(true)
const editorVisible = ref(false)
const editingApp = ref(null)
const codePanelVisible = ref(false)
const codingApp = ref(null)
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
const suiteById = computed(() => {
  const map = new Map()
  suites.value.forEach((suite) => {
    if (suite?.id != null)
      map.set(String(suite.id), suite)
  })
  return map
})
const suiteChildrenMap = computed(() => {
  const map = new Map()
  const sortedSuites = [...suites.value].sort(compareSuites)
  sortedSuites.forEach((suite) => {
    const parentKey = normalizeSuiteParentKey(suite)
    if (!map.has(parentKey))
      map.set(parentKey, [])
    map.get(parentKey).push(suite)
  })
  return map
})
const suiteTreeRows = computed(() => flattenSuiteRows('__root__', 0, new Set()))
const expandableSuiteIds = computed(() => suites.value
  .filter((suite) => {
    if (suite?.id == null)
      return false
    return (suiteChildrenMap.value.get(String(suite.id)) || []).length > 0
  })
  .map(suite => String(suite.id)))
const hasExpandableSuites = computed(() => expandableSuiteIds.value.length > 0)
const filteredSuiteRows = computed(() => {
  const word = suiteKeyword.value.trim().toLowerCase()
  if (!word)
    return suiteTreeRows.value
  const includedIds = new Set()
  suites.value.forEach((suite) => {
    if (!suiteMatchesKeyword(suite, word))
      return
    let cursor = suite
    const visited = new Set()
    while (cursor?.id != null && !visited.has(String(cursor.id))) {
      const cursorId = String(cursor.id)
      includedIds.add(cursorId)
      visited.add(cursorId)
      cursor = suiteById.value.get(String(cursor.parentId))
    }
  })
  return suiteTreeRows.value.filter(row => includedIds.has(String(row.suite.id)))
})
const selectedSuiteCodes = computed(() => {
  if (!activeSuite.value)
    return suiteCode.value ? [suiteCode.value] : []
  const codes = []
  collectSuiteCodes(activeSuite.value, codes, new Set())
  return codes
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

watch(() => route.query.codeAppId, () => {
  openCodePanelFromQuery()
})

onMounted(async () => {
  try {
    await loadAll()
    await openCodePanelFromQuery()
  }
  finally {
    bootstrapping.value = false
  }
})

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
    collapseAllSuites()
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
      ...workspaceSuiteParams(),
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
      ...workspaceSuiteParams(),
      appType: appType.value,
    })
    apps.value = res.data || []
  }
  finally {
    loadingApps.value = false
  }
}

function workspaceSuiteParams() {
  if (!suiteCode.value)
    return {}
  if (selectedSuiteCodes.value.length > 1)
    return { suiteCodes: selectedSuiteCodes.value.join(',') }
  return { suiteCode: suiteCode.value }
}

function compareSuites(left, right) {
  const sortCompare = Number(left?.sortOrder || 0) - Number(right?.sortOrder || 0)
  if (sortCompare !== 0)
    return sortCompare
  return String(left?.suiteName || left?.suiteCode || '')
    .localeCompare(String(right?.suiteName || right?.suiteCode || ''), 'zh-CN')
}

function normalizeSuiteParentKey(suite) {
  if (!suite?.parentId)
    return '__root__'
  const parentKey = String(suite.parentId)
  return suiteById.value.has(parentKey) ? parentKey : '__root__'
}

function flattenSuiteRows(parentKey, level, visited) {
  const children = suiteChildrenMap.value.get(parentKey) || []
  return children.flatMap((suite) => {
    if (suite?.id == null)
      return []
    const suiteKey = String(suite.id)
    if (visited.has(suiteKey))
      return []
    const nextVisited = new Set(visited)
    nextVisited.add(suiteKey)
    const hasChildren = (suiteChildrenMap.value.get(suiteKey) || []).length > 0
    const childCount = countSuiteDescendants(suiteKey, nextVisited)
    const forceExpanded = Boolean(suiteKeyword.value.trim())
    const childRows = forceExpanded || isSuiteExpanded(suite)
      ? flattenSuiteRows(suiteKey, level + 1, nextVisited)
      : []
    return [{
      suite,
      level,
      hasChildren,
      childCount,
    }, ...childRows]
  })
}

function countSuiteDescendants(parentKey, visited) {
  return (suiteChildrenMap.value.get(parentKey) || []).reduce((count, suite) => {
    if (suite?.id == null)
      return count
    const suiteKey = String(suite.id)
    if (visited.has(suiteKey))
      return count
    const nextVisited = new Set(visited)
    nextVisited.add(suiteKey)
    return count + 1 + countSuiteDescendants(suiteKey, nextVisited)
  }, 0)
}

function isSuiteExpanded(suite) {
  if (!suite?.id)
    return true
  return !collapsedSuiteIds.value.has(String(suite.id))
}

function isSuiteTreeExpanded(suite) {
  if (suiteKeyword.value.trim())
    return true
  return isSuiteExpanded(suite)
}

function expandAllSuites() {
  collapsedSuiteIds.value = new Set()
}

function collapseAllSuites() {
  collapsedSuiteIds.value = new Set(expandableSuiteIds.value)
}

function toggleSuiteExpanded(suite) {
  if (!suite?.id)
    return
  const suiteId = String(suite.id)
  const next = new Set(collapsedSuiteIds.value)
  if (next.has(suiteId))
    next.delete(suiteId)
  else
    next.add(suiteId)
  collapsedSuiteIds.value = next
}

function suiteMatchesKeyword(suite, word) {
  const name = `${suite?.suiteName || ''} ${suite?.suiteCode || ''} ${suite?.description || ''}`.toLowerCase()
  return name.includes(word)
}

function collectSuiteCodes(suite, codes, visited) {
  if (!suite?.suiteCode || suite?.id == null)
    return
  const suiteKey = String(suite.id)
  if (visited.has(suiteKey))
    return
  visited.add(suiteKey)
  codes.push(suite.suiteCode)
  ;(suiteChildrenMap.value.get(suiteKey) || []).forEach(child => collectSuiteCodes(child, codes, visited))
}

function suiteMetaText(suiteRow) {
  const suite = suiteRow?.suite || {}
  const childText = suiteRow?.childCount ? ` · ${suiteRow.childCount} 个子域` : ''
  return `${suite.objectCount || 0} 个业务单元 · ${suite.appCount || 0} 个入口${childText}`
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

function getSuiteActionOptions(suite) {
  return [
    {
      label: '进入业务域',
      key: 'open',
    },
    {
      label: '新增子目录',
      key: 'create-child',
    },
    {
      label: '编辑信息',
      key: 'edit',
    },
    {
      label: suiteStatusActionText(suite),
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
  ]
}

function handleSuiteAction(key, suite = activeSuite.value) {
  if (!suite)
    return
  if (key === 'open') {
    openSuite(suite)
    return
  }
  if (key === 'create-child') {
    openSuiteEditor({ parentId: suite.id })
    return
  }
  if (key === 'edit') {
    openSuiteEditor(suite)
    return
  }
  if (key === 'toggle') {
    toggleSuite(suite)
    return
  }
  if (key === 'delete')
    deleteSuite(suite)
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

async function openCodePanel(app) {
  if (!app?.id)
    return
  try {
    const res = await businessAppDetail(app.id)
    codingApp.value = { ...app, ...(res.data || {}) }
  }
  catch {
    codingApp.value = { ...app }
  }
  codePanelVisible.value = true
}

async function openCodePanelFromQuery() {
  const appId = route.query.codeAppId
  if (!appId)
    return
  const matched = apps.value.find(item => String(item.id) === String(appId))
  await openCodePanel(matched || { id: appId })
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
  if (isCodeDownloadApp(app)) {
    await openCodePanel(app)
    return
  }
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

function isCodeDownloadApp(app) {
  return app?.entryMode === 'RUNTIME' && app?.appMode === 'CODE_DOWNLOAD'
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
    content: `确定删除“${suite.suiteName || suite.suiteCode}”吗？已存在子业务域、业务单元或访问入口的业务域会被后端拦截。`,
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
  padding: 12px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px 14px;
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
  font-size: 20px;
  line-height: 26px;
}

.workspace-head h2 {
  font-size: 18px;
}

.content-head h3 {
  font-size: 16px;
}

.page-title-block p,
.workspace-head p,
.content-head p {
  margin: 3px 0 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.45;
}

.app-center-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 12px;
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
  top: 12px;
  display: grid;
  gap: 10px;
  max-height: calc(100vh - 116px);
  overflow: auto;
  padding: 10px;
}

.suite-nav-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.suite-nav-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 2px;
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
  gap: 4px;
}

.suite-nav-item {
  position: relative;
  display: grid;
  grid-template-columns: 22px 32px minmax(0, 1fr) 28px;
  gap: 7px;
  align-items: center;
  width: 100%;
  min-height: 48px;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 7px;
  background: #f9fafb;
  padding: 6px 7px 6px calc(7px + var(--suite-indent, 0px));
  text-align: left;
  transition:
    background 160ms ease,
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.suite-nav-item.child {
  background: #fbfdff;
}

.suite-nav-item.has-children {
  background: #f8fafc;
}

.suite-nav-item.child::before {
  position: absolute;
  z-index: 0;
  top: -5px;
  bottom: -5px;
  left: calc(17px + var(--suite-indent, 0px));
  width: 1px;
  background: #dbe4f0;
  content: '';
  pointer-events: none;
}

.suite-nav-item.child::after {
  position: absolute;
  z-index: 0;
  top: 23px;
  left: calc(17px + var(--suite-indent, 0px));
  width: 12px;
  height: 1px;
  background: #dbe4f0;
  content: '';
  pointer-events: none;
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

.suite-item-actions {
  position: relative;
  z-index: 1;
  display: flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  opacity: 0;
  pointer-events: none;
  transition: opacity 160ms ease;
}

.suite-nav-item:hover .suite-item-actions,
.suite-nav-item.active .suite-item-actions {
  opacity: 1;
  pointer-events: auto;
}

.suite-item-more {
  color: #64748b;
}

.suite-tree-control,
.suite-tree-toggle {
  position: relative;
  z-index: 1;
  display: grid;
  width: 22px;
  height: 22px;
  place-items: center;
}

.suite-tree-toggle {
  cursor: pointer;
  border: 1px solid #d7e0ec;
  border-radius: 5px;
  background: #fff;
  color: #64748b;
  padding: 0;
  transition:
    border-color 160ms ease,
    color 160ms ease,
    background 160ms ease;
}

.suite-tree-toggle:hover {
  border-color: #2f6feb;
  background: #eff6ff;
  color: #2563eb;
}

.suite-mark {
  position: relative;
  z-index: 1;
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 7px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 12px;
  font-weight: 700;
}

.suite-mark.has-icon {
  background: #f8fafc;
  color: #2563eb;
}

.suite-mark.all {
  background: #ecfdf5;
  color: #15803d;
  font-size: 18px;
}

.suite-mark.large {
  width: 42px;
  height: 42px;
  font-size: 14px;
}

.suite-nav-copy {
  position: relative;
  z-index: 1;
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
  font-size: 13px;
  line-height: 1.25;
}

.suite-nav-copy small {
  margin-top: 2px;
  color: #6b7280;
  font-size: 11px;
}

.workspace {
  display: grid;
  min-width: 0;
  gap: 10px;
}

.workspace-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 12px;
}

.selected-suite-title {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 10px;
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
  min-height: 58px;
  border-right: 1px solid #eef2f7;
  padding: 10px 14px;
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
  margin-top: 4px;
  color: #111827;
  font-size: 20px;
  line-height: 1.1;
}

.workspace-toolbar {
  background: #fbfcfe;
  padding: 10px;
}

.workspace-content {
  display: grid;
  min-width: 0;
  gap: 10px;
  padding: 12px;
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
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 10px;
  align-items: stretch;
}

.suite-skeleton-row {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 9px;
  align-items: center;
  min-height: 56px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #f9fafb;
  padding: 8px;
}

.suite-skeleton-row > div:last-child,
.unit-skeleton-head > div {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.metric-skeleton {
  display: grid;
  align-content: center;
  gap: 6px;
}

.unit-skeleton-card {
  display: grid;
  gap: 10px;
  min-height: 172px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.unit-skeleton-head {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.unit-skeleton-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: auto;
  padding-top: 6px;
  border-top: 1px solid #f1f5f9;
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

  .head-actions {
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
