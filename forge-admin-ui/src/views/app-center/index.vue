<template>
  <div class="app-center-page">
    <section class="app-center-layout">
      <aside class="suite-nav">
        <div class="suite-nav-head">
          <div>
            <strong>业务域</strong>
            <span>{{ suites.length }} 个业务域</span>
          </div>
          <n-button
            class="suite-create-btn"
            secondary
            type="primary"
            size="small"
            @click="openSuiteEditor(null)"
          >
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
            新建业务域
          </n-button>
        </div>

        <div class="suite-pill-list">
          <template v-if="showSuiteSkeleton">
            <n-skeleton v-for="idx in 6" :key="idx" height="32px" :sharp="false" />
          </template>
          <template v-else>
            <div
              class="suite-pill all-suite"
              :class="{ active: !suiteCode }"
              role="button"
              tabindex="0"
              @click="selectSuite(null)"
              @keydown.enter.prevent="selectSuite(null)"
              @keydown.space.prevent="selectSuite(null)"
            >
              <span class="suite-pill-icon all">
                <n-icon><GridOutline /></n-icon>
              </span>
              <span class="suite-pill-copy">
                <strong>全部业务域</strong>
                <small>{{ suiteObjectTotal }} 对象 · {{ suiteAppTotal }} 入口</small>
              </span>
            </div>

            <div
              v-for="suiteRow in suiteTreeRows"
              :key="suiteRow.suite.id || suiteRow.suite.suiteCode"
              class="suite-pill"
              :class="{
                'active': suiteCode === suiteRow.suite.suiteCode,
                'child': suiteRow.level > 0,
                'has-children': suiteRow.hasChildren,
              }"
              :style="{ '--suite-indent': `${suiteRow.level * 16}px` }"
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
                :aria-label="isSuiteExpanded(suiteRow.suite) ? '收起子业务域' : '展开子业务域'"
                @click.stop="toggleSuiteExpanded(suiteRow.suite)"
              >
                <n-icon>
                  <ChevronDownOutline v-if="isSuiteExpanded(suiteRow.suite)" />
                  <ChevronForwardOutline v-else />
                </n-icon>
              </button>
              <span v-else class="suite-tree-spacer" />
              <span class="suite-pill-icon" :class="{ 'has-icon': suiteRow.suite.icon }">
                <IconRenderer v-if="suiteRow.suite.icon" :icon="suiteRow.suite.icon" :size="18" />
                <template v-else>{{ suiteInitial(suiteRow.suite) }}</template>
              </span>
              <span class="suite-pill-copy">
                <strong>{{ suiteRow.suite.suiteName || suiteRow.suite.suiteCode }}</strong>
                <small>{{ suitePillMetaText(suiteRow.suite) }}</small>
              </span>
              <span class="suite-pill-actions" @click.stop>
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
              </span>
            </div>
          </template>
        </div>
      </aside>

      <main class="workspace">
        <section class="workspace-toolbar">
          <AppFilterBar
            v-model:keyword="keyword"
            v-model:suite-code="suiteCode"
            v-model:app-type="appType"
            v-model:object-type="objectType"
            :suites="suites"
            :show-suite="false"
            @search="loadWorkspace"
            @refresh="loadWorkspace"
          />
        </section>

        <section class="workspace-content">
          <div class="content-head">
            <div>
              <h3>业务对象分组</h3>
              <p>按主对象归集明细、引用对象和访问入口。</p>
            </div>
            <div class="content-head-actions">
              <span>{{ objectGroupTotal }} 个分组</span>
              <n-dropdown trigger="click" :options="createOptions" @select="handleCreateSelect">
                <n-button type="primary" size="small">
                  <template #icon>
                    <n-icon><AddOutline /></n-icon>
                  </template>
                  新建
                </n-button>
              </n-dropdown>
            </div>
          </div>

          <div v-if="showWorkspaceSkeleton" class="object-table-skeleton">
            <div v-for="idx in 4" :key="idx" class="object-group-skeleton">
              <n-skeleton height="40px" :sharp="false" />
              <n-skeleton text :repeat="3" />
            </div>
          </div>
          <BusinessObjectTable
            v-else-if="pagedObjectGroups.length"
            :groups="pagedObjectGroups"
            :show-suite="!suiteCode"
            @open-object="openObject"
            @edit-object="openObjectEditor"
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
          <n-empty v-else description="当前筛选下暂无业务对象或访问入口" />

          <div v-if="objectGroupTotal > groupPagination.pageSize" class="card-pagination">
            <n-pagination
              v-model:page="groupPagination.page"
              v-model:page-size="groupPagination.pageSize"
              :item-count="objectGroupTotal"
              :page-sizes="groupPageSizeOptions"
              show-size-picker
              @update:page-size="handleGroupPageSizeChange"
            />
          </div>
        </section>
      </main>
    </section>

    <AppEntryWizard
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
    <BusinessObjectEditorDrawer
      v-model:show="objectEditorVisible"
      :object="editingObject"
      :suites="suites"
      @saved="handleObjectEdited"
    />
    <SuiteEditorDrawer
      v-model:show="suiteEditorVisible"
      :suite="editingSuite"
      :suites="suites"
      @saved="handleSuiteSaved"
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
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  businessAppDetail,
  businessAppList,
  businessAppOpenInfo,
  businessObjectDetail,
  businessObjectList,
  businessObjectRelations,
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
import BusinessObjectTable from './components/BusinessObjectTable.vue'

const AppCodePanel = defineAsyncComponent(() => import('./components/AppCodePanel.vue'))
const AppEntryWizard = defineAsyncComponent(() => import('./components/AppEntryWizard.vue'))
const BusinessObjectEditorDrawer = defineAsyncComponent(() => import('./components/BusinessObjectEditorDrawer.vue'))
const BusinessObjectWizardDrawer = defineAsyncComponent(() => import('./components/BusinessObjectWizardDrawer.vue'))
const SuiteEditorDrawer = defineAsyncComponent(() => import('./components/SuiteEditorDrawer.vue'))

const router = useRouter()
const route = useRoute()
const message = useMessage()

const keyword = ref('')
const suiteCode = ref(null)
const appType = ref(null)
const objectType = ref(null)
const suites = ref([])
const collapsedSuiteIds = ref(new Set())
const objects = ref([])
const apps = ref([])
const relationCache = ref(new Map())
const loadingSuites = ref(false)
const loadingObjects = ref(false)
const loadingApps = ref(false)
const loadingRelations = ref(false)
const bootstrapping = ref(true)
const editorVisible = ref(false)
const editingApp = ref(null)
const codePanelVisible = ref(false)
const codingApp = ref(null)
const objectWizardVisible = ref(false)
const objectEditorVisible = ref(false)
const editingObject = ref(null)
const suiteEditorVisible = ref(false)
const editingSuite = ref(null)
const groupPageSizeOptions = [8, 16, 32, 48]
const groupPagination = ref({
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
const sortedSuites = computed(() => [...suites.value].sort(compareSuites))
const suiteChildrenMap = computed(() => {
  const map = new Map()
  sortedSuites.value.forEach((suite) => {
    const parentKey = normalizeSuiteParentKey(suite)
    if (!map.has(parentKey))
      map.set(parentKey, [])
    map.get(parentKey).push(suite)
  })
  return map
})
const suiteTreeRows = computed(() => flattenSuiteRows('__root__', 0, new Set()))
const selectedSuiteCodes = computed(() => {
  if (!activeSuite.value)
    return suiteCode.value ? [suiteCode.value] : []
  const codes = []
  collectSuiteCodes(activeSuite.value, codes, new Set())
  return codes
})
const suiteObjectTotal = computed(() => suites.value.reduce((sum, item) => sum + Number(item.objectCount || 0), 0))
const suiteAppTotal = computed(() => suites.value.reduce((sum, item) => sum + Number(item.appCount || 0), 0))
const workspaceLoading = computed(() => loadingObjects.value || loadingApps.value || loadingRelations.value)
const showSuiteSkeleton = computed(() => bootstrapping.value || loadingSuites.value)
const showWorkspaceSkeleton = computed(() => bootstrapping.value || workspaceLoading.value)
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
const objectByKey = computed(() => {
  const map = new Map()
  objects.value.forEach((object) => {
    const key = unitKey(object.suiteCode, object.objectCode)
    if (key)
      map.set(key, object)
  })
  return map
})
const objectGroups = computed(() => {
  const groups = []
  const groupedChildKeys = new Set()
  const objectList = [...objects.value].sort(compareObjects)

  objectList.forEach((object) => {
    if (!isGroupLeaderObject(object))
      return
    const group = createObjectGroup(object, { standalone: false, synthetic: false })
    const sourceRelations = relationCache.value.get(String(object.id)) || []
    sourceRelations
      .filter(relation => relationBelongsToSource(relation, object))
      .forEach((relation) => {
        const child = childFromRelation(relation, object)
        if (!child)
          return
        group.children.push(child)
        if (!child.synthetic && child.object?.objectCode) {
          groupedChildKeys.add(unitKey(child.object.suiteCode, child.object.objectCode))
        }
      })
    finalizeGroupStats(group)
    groups.push(group)
  })

  objectList.forEach((object) => {
    const key = unitKey(object.suiteCode, object.objectCode)
    if (!key || groupedChildKeys.has(key) || groups.some(group => group.key === key))
      return
    const group = createObjectGroup(object, { standalone: false, synthetic: false })
    finalizeGroupStats(group)
    groups.push(group)
  })

  const objectKeys = new Set(objectList.map(object => unitKey(object.suiteCode, object.objectCode)).filter(Boolean))
  apps.value.forEach((app) => {
    const key = unitKey(app.suiteCode, app.objectCode)
    if (!key)
      return
    if (!objectKeys.has(key) && !groups.some(group => group.key === key)) {
      const group = createObjectGroup(syntheticObjectFromApp(app), { standalone: false, synthetic: true })
      finalizeGroupStats(group)
      groups.push(group)
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
        groupObject: {
          suiteCode: app.suiteCode,
          suiteName: app.suiteName,
          objectName: app.suiteName ? `${app.suiteName}独立入口` : '独立访问入口',
          description: '未绑定具体业务单元的页面、移动端或接口入口。',
        },
        apps: [],
        synthetic: true,
        standalone: true,
        children: [],
      })
    }
    standaloneGroups.get(key).apps.push(app)
  })

  standaloneGroups.forEach((group) => {
    finalizeGroupStats(group)
    groups.push(group)
  })

  return groups
    .filter(group => !appType.value || group.entryCount > 0)
    .sort(compareGroups)
})
const objectGroupTotal = computed(() => objectGroups.value.length)
const pagedObjectGroups = computed(() => {
  const start = (groupPagination.value.page - 1) * groupPagination.value.pageSize
  return objectGroups.value.slice(start, start + groupPagination.value.pageSize)
})

watch([keyword, suiteCode, appType, objectType], () => {
  groupPagination.value.page = 1
  loadWorkspace()
})

watch(objectGroupTotal, (total) => {
  const maxPage = Math.max(1, Math.ceil(total / groupPagination.value.pageSize))
  if (groupPagination.value.page > maxPage)
    groupPagination.value.page = maxPage
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
  relationCache.value = new Map()
  await loadSuites()
  await loadWorkspace()
}

async function loadWorkspace() {
  await Promise.all([loadObjects(), loadApps()])
  await loadGroupRelations()
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
      objectType: objectType.value,
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

async function loadGroupRelations() {
  const candidates = objects.value.filter((object) => {
    if (!object?.id || !isGroupLeaderObject(object))
      return false
    return Number(object.relationCount || 0) > 0
  })
  const missingCandidates = candidates.filter(object => !relationCache.value.has(String(object.id)))
  if (!missingCandidates.length)
    return

  loadingRelations.value = true
  try {
    const nextCache = new Map(relationCache.value)
    const results = await Promise.all(missingCandidates.map(async (object) => {
      try {
        const res = await businessObjectRelations(object.id)
        return [String(object.id), res.data || []]
      }
      catch (error) {
        console.error('加载业务对象关系失败:', error)
        return [String(object.id), []]
      }
    }))
    results.forEach(([objectId, relations]) => {
      nextCache.set(objectId, relations)
    })
    relationCache.value = nextCache
  }
  finally {
    loadingRelations.value = false
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
    const childRows = hasChildren && isSuiteExpanded(suite)
      ? flattenSuiteRows(suiteKey, level + 1, nextVisited)
      : []
    return [{
      suite,
      level,
      hasChildren,
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

function suitePillMetaText(suite) {
  const childCount = suite?.id == null ? 0 : countSuiteDescendants(String(suite.id), new Set([String(suite.id)]))
  const childText = childCount ? ` · ${childCount} 子域` : ''
  return `${suite.objectCount || 0} 对象 · ${suite.appCount || 0} 入口${childText}`
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

function isGroupLeaderObject(object) {
  return ['MASTER', 'TRANSACTION'].includes(String(object?.objectType || '').toUpperCase())
}

function createObjectGroup(object, options = {}) {
  const key = unitKey(object?.suiteCode, object?.objectCode) || `standalone:${object?.suiteCode || 'all'}`
  return {
    key,
    groupObject: object,
    children: [],
    apps: appGroups.value.get(key) || [],
    entryCount: 0,
    synthetic: Boolean(options.synthetic),
    standalone: Boolean(options.standalone),
  }
}

function finalizeGroupStats(group) {
  group.entryCount = (group.apps || []).length + (group.children || []).reduce((sum, child) => {
    return sum + (child.apps || []).length
  }, 0)
  return group
}

function relationBelongsToSource(relation, object) {
  const relationSuiteCode = relation?.suiteCode || object?.suiteCode
  return relationSuiteCode === object?.suiteCode
    && relation?.sourceObjectCode === object?.objectCode
    && relation?.targetObjectCode
}

function childFromRelation(relation, sourceObject) {
  const targetKey = unitKey(relation.suiteCode || sourceObject.suiteCode, relation.targetObjectCode)
  if (!targetKey || targetKey === unitKey(sourceObject.suiteCode, sourceObject.objectCode))
    return null
  const targetObject = objectByKey.value.get(targetKey)
  if (targetObject) {
    if (!objectMatchesType(targetObject))
      return null
    return {
      object: targetObject,
      relation,
      apps: appGroups.value.get(targetKey) || [],
      synthetic: false,
    }
  }
  const syntheticObject = syntheticObjectFromRelation(relation, sourceObject)
  if (!objectMatchesType(syntheticObject))
    return null
  return {
    object: syntheticObject,
    relation,
    apps: appGroups.value.get(targetKey) || [],
    synthetic: true,
  }
}

function syntheticObjectFromRelation(relation, sourceObject) {
  return {
    suiteCode: relation.suiteCode || sourceObject.suiteCode,
    suiteName: sourceObject.suiteName,
    objectCode: relation.targetObjectCode,
    objectName: relation.targetObjectName || relation.targetObjectCode,
    objectType: relation.relationType === 'REFERENCE' ? 'LOOKUP' : 'DETAIL',
    description: relation.description || relation.relationName || '关系目标对象尚未进入当前筛选结果。',
    status: relation.status,
  }
}

function compareGroups(a, b) {
  if (a.standalone !== b.standalone)
    return a.standalone ? 1 : -1
  return compareObjects(a.groupObject, b.groupObject)
}

function objectMatchesType(object) {
  if (!objectType.value)
    return true
  return String(object?.objectType || '').toUpperCase() === String(objectType.value).toUpperCase()
}

function compareObjects(a, b) {
  if (!suiteCode.value) {
    const suiteCompare = String(a?.suiteName || a?.suiteCode || '')
      .localeCompare(String(b?.suiteName || b?.suiteCode || ''), 'zh-CN')
    if (suiteCompare !== 0)
      return suiteCompare
  }
  const sortCompare = Number(a?.sortOrder || 0) - Number(b?.sortOrder || 0)
  if (sortCompare !== 0)
    return sortCompare
  return String(a?.objectName || a?.objectCode || '')
    .localeCompare(String(b?.objectName || b?.objectCode || ''), 'zh-CN')
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

function handleGroupPageSizeChange(pageSize) {
  groupPagination.value.pageSize = pageSize
  groupPagination.value.page = 1
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
  openRouteInNewTab({
    name: 'BusinessObjectDesigner',
    params: { objectCode: object.objectCode },
    query: {
      suiteCode: object.suiteCode || suiteCode.value || undefined,
      objectId: object.id,
      panel: panel || 'form',
      returnTo: route.fullPath,
    },
  })
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

async function openObjectEditor(object) {
  if (!object?.id)
    return
  try {
    const res = await businessObjectDetail(object.id)
    editingObject.value = { ...object, ...(res.data || {}) }
  }
  catch {
    editingObject.value = { ...object }
  }
  objectEditorVisible.value = true
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

async function handleObjectEdited(payload) {
  suiteCode.value = payload?.suiteCode || suiteCode.value
  await loadAll()
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
  openRouteInNewTab(info.targetUrl)
}

function openRouteInNewTab(location) {
  if (!location)
    return
  if (typeof location === 'string' && /^https?:\/\//i.test(location)) {
    window.open(location, '_blank', 'noopener,noreferrer')
    return
  }
  const target = router.resolve(location)
  window.open(target.href, '_blank', 'noopener,noreferrer')
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

async function applyObjectStatus(object, nextStatus) {
  await updateBusinessObjectStatus(object.id, nextStatus)
  message.success(nextStatus === 0 ? '业务单元已停用' : '业务单元已启用')
  await loadAll()
}

async function toggleObject(object) {
  if (!object?.id)
    return
  const nextStatus = object.status === 1 ? 0 : 1
  if (nextStatus === 0) {
    window.$dialog?.warning({
      title: '停用业务单元',
      content: `确定停用“${object.objectName || object.objectCode}”吗？停用后关联访问入口和流程配置仍保留，但用户不应继续进入该业务单元办理新业务。`,
      positiveText: '停用',
      negativeText: '取消',
      onPositiveClick: () => applyObjectStatus(object, nextStatus),
    })
    return
  }
  await applyObjectStatus(object, nextStatus)
}

async function applySuiteStatus(suite, nextStatus) {
  await updateBusinessSuiteStatus(suite.id, nextStatus)
  message.success(nextStatus === 0 ? '业务域已停用' : '业务域已启用')
  await loadAll()
}

async function toggleSuite(suite) {
  if (!suite?.id)
    return
  const nextStatus = suite.status === 1 ? 0 : 1
  if (nextStatus === 0) {
    window.$dialog?.warning({
      title: '停用业务域',
      content: `确定停用“${suite.suiteName || suite.suiteCode}”吗？停用后该业务域下的业务单元和访问入口配置仍会保留。`,
      positiveText: '停用',
      negativeText: '取消',
      onPositiveClick: () => applySuiteStatus(suite, nextStatus),
    })
    return
  }
  await applySuiteStatus(suite, nextStatus)
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

.content-head h3 {
  margin: 0;
  color: var(--n-text-color, #111827);
  font-weight: 700;
  letter-spacing: 0;
}

.content-head h3 {
  font-size: 16px;
}

.content-head p {
  margin: 3px 0 0;
  color: var(--n-text-color-2, #6b7280);
  font-size: 12px;
  line-height: 1.45;
}

.app-center-layout {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.suite-nav,
.workspace-toolbar,
.workspace-content {
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 8px;
  background: var(--n-color, #fff);
}

.suite-nav {
  position: sticky;
  top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 232px;
  height: calc(100vh - 40px);
  max-height: calc(100vh - 40px);
  overflow: hidden;
  padding: 10px;
}

.suite-nav-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: flex-start;
  flex: 0 0 auto;
}

.suite-nav-head > div {
  min-width: 0;
}

.suite-nav-head strong,
.suite-nav-head span {
  display: block;
}

.suite-nav-head strong {
  color: var(--n-text-color, #111827);
  font-size: 14px;
}

.suite-nav-head span {
  margin-top: 2px;
  color: var(--n-text-color-2, #6b7280);
  font-size: 12px;
}

.suite-create-btn {
  flex: 0 0 auto;
}

.suite-nav :deep(.n-spin-container),
.suite-nav :deep(.n-spin-content) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.suite-pill-list {
  display: grid;
  flex: 1 1 auto;
  align-content: start;
  gap: 4px;
  min-height: 0;
  max-height: 100%;
  overflow-y: auto;
  padding-right: 2px;
}

.suite-pill {
  position: relative;
  display: grid;
  grid-template-columns: 18px 24px minmax(0, 1fr) 26px;
  gap: 7px;
  align-items: center;
  min-height: 36px;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: var(--n-text-color, #111827);
  padding: 4px 5px 4px calc(5px + var(--suite-indent, 0px));
  text-align: left;
  transition:
    background 160ms ease,
    border-color 160ms ease,
    color 160ms ease;
}

.suite-pill.all-suite {
  grid-template-columns: 24px minmax(0, 1fr);
  padding-left: 5px;
}

.suite-pill.child::before {
  position: absolute;
  top: -4px;
  bottom: -4px;
  left: calc(14px + var(--suite-indent, 0px));
  width: 1px;
  background: #dbe4f0;
  content: '';
  pointer-events: none;
}

.suite-pill.child::after {
  position: absolute;
  top: 18px;
  left: calc(14px + var(--suite-indent, 0px));
  width: 10px;
  height: 1px;
  background: #dbe4f0;
  content: '';
  pointer-events: none;
}

.suite-pill:hover {
  background: rgb(0 0 0 / 4%);
}

.suite-pill.active {
  border-color: #18a058;
  background: #18a058;
  color: #fff;
}

.suite-tree-toggle,
.suite-tree-spacer {
  position: relative;
  z-index: 1;
  display: grid;
  width: 18px;
  height: 24px;
  place-items: center;
}

.suite-tree-toggle {
  cursor: pointer;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: inherit;
  padding: 0;
}

.suite-tree-toggle:hover {
  background: rgb(0 0 0 / 7%);
}

.suite-pill.active .suite-tree-toggle:hover {
  background: rgb(255 255 255 / 18%);
}

.suite-pill-icon {
  position: relative;
  z-index: 1;
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 5px;
  background: #eef6ff;
  color: #2563eb;
  font-size: 10px;
  font-weight: 700;
}

.suite-pill-icon.all {
  background: #ecfdf5;
  color: #15803d;
  font-size: 15px;
}

.suite-pill.active .suite-pill-icon {
  background: rgb(255 255 255 / 18%);
  color: #fff;
}

.suite-pill-copy {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.suite-pill-copy strong,
.suite-pill-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suite-pill-copy strong {
  font-size: 12px;
  line-height: 1.25;
}

.suite-pill-copy small {
  margin-top: 1px;
  color: var(--n-text-color-3, #8a94a6);
  font-size: 10px;
}

.suite-pill.active .suite-pill-copy small {
  color: rgb(255 255 255 / 78%);
}

.suite-pill-actions {
  position: relative;
  z-index: 1;
  display: flex;
  width: 26px;
  height: 26px;
  align-items: center;
  justify-content: center;
  opacity: 0;
  pointer-events: none;
  transition: opacity 160ms ease;
}

.suite-pill:hover .suite-pill-actions,
.suite-pill.active .suite-pill-actions {
  opacity: 1;
  pointer-events: auto;
}

.suite-item-more {
  color: inherit;
}

.workspace {
  display: grid;
  min-width: 0;
  gap: 10px;
}

.workspace-toolbar {
  background: var(--n-color, #fff);
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

.content-head-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
  align-items: center;
}

.content-head-actions span {
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

.object-table-skeleton {
  display: grid;
  gap: 10px;
}

.object-group-skeleton {
  display: grid;
  gap: 9px;
  border: 1px solid var(--n-border-color, #eef2f7);
  border-radius: 8px;
  background: var(--n-color, #fff);
  padding: 10px;
}

.card-pagination {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 980px) {
  .app-center-layout {
    grid-template-columns: 208px minmax(0, 1fr);
  }

  .suite-nav {
    width: 208px;
  }
}

@media (max-width: 860px) {
  .app-center-layout {
    grid-template-columns: 1fr;
  }

  .suite-nav {
    position: static;
    width: auto;
    height: 260px;
    max-height: 260px;
  }
}

@media (max-width: 560px) {
  .app-center-page {
    padding: 10px;
  }

  .content-head {
    display: grid;
  }

  .content-head-actions {
    justify-content: space-between;
  }
}
</style>
