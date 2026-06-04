<template>
  <div ref="pageRef" class="system-menu-page">
    <div class="menu-workbench-header">
      <div class="header-main">
        <div class="header-title">
          <span class="title-mark" />
          <div>
            <h2>菜单管理</h2>
            <p>按客户端、目录节点和资源类型聚焦维护菜单、按钮与 API 权限</p>
          </div>
        </div>

        <div class="header-actions">
          <NButton size="small" :loading="loading" @click="loadResourceTree">
            <template #icon>
              <i class="i-material-symbols:refresh" />
            </template>
            刷新
          </NButton>
          <NButton size="small" type="primary" @click="handleAddRoot">
            <template #icon>
              <i class="i-material-symbols:add" />
            </template>
            新增顶级资源
          </NButton>
        </div>
      </div>

      <div class="client-tabs-container">
        <n-tabs type="line" size="small" :value="currentClientCode" @update:value="handleClientTabChange">
          <n-tab-pane name="" tab="全部">
            <template #tab>
              <span class="tab-label">全部</span>
            </template>
          </n-tab-pane>
          <n-tab-pane v-for="client in clientList" :key="client.clientCode" :name="client.clientCode">
            <template #tab>
              <span class="tab-label">{{ client.clientName }}</span>
            </template>
          </n-tab-pane>
        </n-tabs>
      </div>

      <div class="metric-strip">
        <div v-for="item in resourceMetrics" :key="item.key" class="metric-item">
          <span class="metric-value">{{ item.value }}</span>
          <span class="metric-label">{{ item.label }}</span>
        </div>
      </div>
    </div>

    <div class="menu-workbench-body">
      <aside class="tree-pane">
        <div class="pane-head">
          <div>
            <span class="pane-title">资源结构</span>
            <span class="pane-subtitle">目录 / 菜单</span>
          </div>
          <div class="tree-head-actions">
            <NButton quaternary size="tiny" @click="expandNavigationTree">
              <template #icon>
                <i class="i-material-symbols:unfold-more" />
              </template>
              展开
            </NButton>
            <NButton quaternary size="tiny" @click="collapseNavigationTree">
              <template #icon>
                <i class="i-material-symbols:unfold-less" />
              </template>
              折叠
            </NButton>
          </div>
        </div>

        <n-input
          v-model:value="treeKeyword"
          size="small"
          clearable
          placeholder="筛选左侧结构"
          class="tree-search"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>

        <div class="tree-scroll">
          <n-spin :show="loading">
            <n-tree
              block-line
              selectable
              :data="navigationTreeData"
              :expanded-keys="navigationExpandedKeys"
              :selected-keys="[selectedResourceId]"
              :render-label="renderNavigationLabel"
              key-field="key"
              label-field="label"
              children-field="children"
              @update:selected-keys="handleNavigationSelect"
              @update:expanded-keys="keys => navigationExpandedKeys = keys"
            />
          </n-spin>
        </div>
      </aside>

      <main class="list-pane">
        <div class="list-toolbar">
          <div class="list-context">
            <span class="context-title">{{ currentContextTitle }}</span>
            <span class="context-subtitle">{{ displayRows.length }} 项资源</span>
          </div>

          <div class="list-filters">
            <n-input
              v-model:value="resourceKeyword"
              size="small"
              clearable
              placeholder="搜索名称 / 路由 / 权限 / API"
              class="keyword-input"
            >
              <template #prefix>
                <i class="i-material-symbols:search" />
              </template>
            </n-input>
            <n-select
              v-model:value="resourceTypeFilter"
              size="small"
              clearable
              placeholder="类型"
              :options="resourceTypeFilterOptions"
              class="type-filter"
            />
            <n-select
              v-model:value="visibleFilter"
              size="small"
              clearable
              placeholder="状态"
              :options="visibleFilterOptions"
              class="visible-filter"
            />
          </div>
        </div>

        <div class="resource-list-scroll" :style="{ maxHeight: `${tableMaxHeight}px` }">
          <n-spin :show="loading">
            <div v-if="displayRows.length === 0" class="resource-list-empty">
              <i class="i-material-symbols:database-off-outline" />
              <span>暂无资源</span>
            </div>

            <div
              v-for="row in displayRows"
              :key="row.id"
              class="resource-list-row"
              :class="{ 'is-active': activeResource?.id === row.id }"
              @click="selectedRow = row"
            >
              <div class="resource-list-main">
                <span
                  class="resource-level-spacer"
                  :class="{ 'has-level': getDisplayLevel(row) > 0 }"
                  :style="{ width: `${getDisplayLevel(row) * 18}px` }"
                />
                <span class="resource-icon-shell">
                  <IconRenderer
                    v-if="getRenderableIcon(row)"
                    :icon="getRenderableIcon(row)"
                    :font-size="15"
                    custom-style="color: var(--primary-color, #4C6EF5)"
                  />
                  <i v-else :class="getResourceTypeConfig(row.resourceType).icon" />
                </span>

                <div class="resource-list-copy">
                  <div class="resource-title-line">
                    <span class="resource-name">{{ row.resourceName || '-' }}</span>
                    <span class="resource-type-badge">
                      <span
                        class="resource-type-dot"
                        :style="{ backgroundColor: getResourceTypeConfig(row.resourceType).color || '#adb5bd' }"
                      />
                      {{ getResourceTypeText(row.resourceType) }}
                    </span>
                    <span v-if="!currentClientCode" class="resource-client-chip">
                      {{ getClientDisplayName(row.clientCode) }}
                    </span>
                  </div>
                  <div class="resource-meta-line">
                    {{ getResourceSubtitle(row) }}
                  </div>
                </div>
              </div>

              <div class="resource-route-summary">
                <span>{{ getPrimaryRouteText(row) }}</span>
                <small v-if="getSecondaryRouteText(row)">{{ getSecondaryRouteText(row) }}</small>
              </div>

              <div class="resource-list-sort">
                <NInputNumber
                  v-if="row._editingSort"
                  :value="row.sort"
                  :min="0"
                  :show-button="false"
                  size="small"
                  class="inline-sort-input"
                  @click.stop
                  @update:value="value => handleSortCommit(row, value)"
                  @blur="row._editingSort = false"
                />
                <button v-else class="sort-chip" @click.stop="row._editingSort = true">
                  {{ row.sort ?? 0 }}
                </button>
              </div>
              <div class="resource-list-status">
                <NTooltip trigger="hover">
                  <template #trigger>
                    <NButton
                      quaternary
                      circle
                      size="tiny"
                      class="table-icon-action visibility-icon-button"
                      :class="Number(row.visible) === 1 ? 'is-visible' : 'is-hidden'"
                      @click.stop="handleInlineUpdate(row, 'visible', Number(row.visible) === 1 ? 0 : 1)"
                    >
                      <template #icon>
                        <i :class="Number(row.visible) === 1 ? 'i-material-symbols:visibility' : 'i-material-symbols:visibility-off'" />
                      </template>
                    </NButton>
                  </template>
                  {{ Number(row.visible) === 1 ? '点击隐藏' : '点击显示' }}
                </NTooltip>
              </div>
              <div class="resource-list-actions">
                <NTooltip trigger="hover">
                  <template #trigger>
                    <NButton quaternary circle size="tiny" type="primary" class="table-icon-action" @click.stop="handleEdit(row)">
                      <template #icon>
                        <i class="i-material-symbols:edit-outline" />
                      </template>
                    </NButton>
                  </template>
                  编辑
                </NTooltip>
                <NDropdown
                  trigger="click"
                  :options="getMoreActionOptions(row)"
                  @select="key => handleMoreAction(key, row)"
                >
                  <NTooltip trigger="hover">
                    <template #trigger>
                      <NButton quaternary circle size="tiny" class="table-icon-action" @click.stop>
                        <template #icon>
                          <i class="i-material-symbols:more-horiz" />
                        </template>
                      </NButton>
                    </template>
                    更多操作
                  </NTooltip>
                </NDropdown>
              </div>
            </div>
          </n-spin>
        </div>
      </main>

      <aside class="detail-pane">
        <template v-if="activeResource">
          <div class="detail-head">
            <div class="detail-title-row">
              <IconRenderer
                v-if="getRenderableIcon(activeResource)"
                :icon="getRenderableIcon(activeResource)"
                :font-size="18"
                custom-style="color: var(--primary-color, #4c6ef5)"
              />
              <div class="detail-title-text">
                <span>{{ activeResource.resourceName }}</span>
                <small>{{ getResourceTypeText(activeResource.resourceType) }}</small>
              </div>
            </div>
            <NTag :type="activeResource.visible === 1 ? 'success' : 'default'" size="small" :bordered="false">
              {{ activeResource.visible === 1 ? '显示' : '隐藏' }}
            </NTag>
          </div>

          <div class="detail-actions">
            <NButton size="small" type="primary" @click="handleEdit(activeResource)">
              编辑
            </NButton>
            <NButton size="small" @click="handleAdd(activeResource)">
              新增子项
            </NButton>
          </div>

          <div class="detail-section">
            <span class="section-label">基础信息</span>
            <dl class="detail-grid">
              <div>
                <dt>客户端</dt>
                <dd>{{ getClientDisplayName(activeResource.clientCode) }}</dd>
              </div>
              <div>
                <dt>排序</dt>
                <dd>{{ activeResource.sort ?? 0 }}</dd>
              </div>
              <div>
                <dt>路由</dt>
                <dd>{{ activeResource.path || '-' }}</dd>
              </div>
              <div>
                <dt>组件</dt>
                <dd>{{ activeResource.component || '-' }}</dd>
              </div>
              <div>
                <dt>权限标识</dt>
                <dd>{{ activeResource.perms || '-' }}</dd>
              </div>
              <div>
                <dt>API</dt>
                <dd>{{ activeResource.apiMethod || '-' }} {{ activeResource.apiUrl || '' }}</dd>
              </div>
            </dl>
          </div>

          <div class="detail-section">
            <span class="section-label">子资源概览</span>
            <div class="child-summary">
              <span>目录/菜单 {{ activeChildSummary.menu }}</span>
              <span>按钮 {{ activeChildSummary.button }}</span>
              <span>API {{ activeChildSummary.api }}</span>
            </div>
          </div>

          <div v-if="activeResource.remark" class="detail-section">
            <span class="section-label">备注</span>
            <p class="remark-text">
              {{ activeResource.remark }}
            </p>
          </div>
        </template>

        <div v-else class="detail-empty">
          <i class="i-material-symbols:ads-click" />
          <span>选择左侧节点或中间列表项查看详情</span>
        </div>
      </aside>
    </div>

    <n-drawer v-model:show="drawerVisible" :width="drawerWidth" placement="right" :trap-focus="false">
      <n-drawer-content :title="drawerTitle" closable>
        <AiForm
          ref="formRef"
          v-model:value="formData"
          :schema="editSchema"
          :grid-cols="2"
          :show-actions="false"
          label-placement="left"
          label-width="96"
          size="small"
        >
          <template #icon="{ value, updateValue }">
            <n-tabs
              type="line"
              size="small"
              animated
              :value="formIconTab"
              @update:value="handleFormIconTabChange"
            >
              <n-tab-pane name="font" tab="字体图标">
                <div class="icon-selector-container">
                  <IconSelector :model-value="getFontIconValue(value)" @update:model-value="updateValue" />
                  <n-input
                    :value="getFontIconValue(value)"
                    placeholder="或手动输入图标名称，如 i-mdi-home"
                    clearable
                    @update:value="updateValue"
                  >
                    <template #prefix>
                      <IconRenderer v-if="getFontIconValue(value)" :icon="getFontIconValue(value)" />
                    </template>
                  </n-input>
                </div>
              </n-tab-pane>
              <n-tab-pane name="image" tab="图片图标">
                <div class="icon-upload-container">
                  <ImageUpload
                    :model-value="getImageIconValue(value)"
                    :limit="1"
                    :file-size="2"
                    :file-type="['png', 'jpg', 'jpeg', 'webp', 'gif', 'svg']"
                    business-type="menu-icon"
                    :show-tip="true"
                    value-type="string"
                    @success="fileData => updateValue(fileData?.fileId || fileData?.filePath || '')"
                    @update:model-value="updateValue"
                  />
                </div>
              </n-tab-pane>
            </n-tabs>
          </template>

          <template #path="{ value, updateValue, formData: slotFormData }">
            <div class="route-select-field">
              <NAutoComplete
                :value="value"
                :options="getAvailableRouteOptions(slotFormData, value)"
                clearable
                placeholder="输入或选择页面路由，如 /system/user"
                :render-label="renderRouteOptionLabel"
                @update:value="routePath => handleRoutePathChange(routePath, updateValue, slotFormData)"
              />
              <div class="route-select-hint">
                可搜索已有页面路由
              </div>
              <div v-if="slotFormData?.component" class="route-select-hint">
                组件路径：{{ slotFormData.component }}
              </div>
            </div>
          </template>

          <template #component="{ value, updateValue, formData: slotFormData }">
            <div class="route-select-field">
              <NAutoComplete
                :value="normalizeComponentValue(value)"
                :options="getAvailableComponentOptions(slotFormData, value)"
                clearable
                placeholder="输入或选择组件路径，如 system/user"
                :render-label="renderComponentOptionLabel"
                @update:value="component => handleComponentPathChange(component, updateValue, slotFormData)"
              />
              <div class="route-select-hint">
                可搜索已有组件路径
              </div>
            </div>
          </template>
        </AiForm>

        <template #footer>
          <n-space justify="end">
            <NButton @click="drawerVisible = false">
              取消
            </NButton>
            <NButton type="primary" :loading="submitLoading" @click="handleDrawerSubmit">
              保存
            </NButton>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <IconSelector
      ref="tableIconSelectorRef"
      :trigger="false"
      :model-value="tableIconValue"
      @update:model-value="handleTableIconSelected"
    />
  </div>
</template>

<script setup>
import {
  NAutoComplete,
  NButton,
  NDropdown,
  NInputNumber,
  NTag,
  NTooltip,
} from 'naive-ui'
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import api from '@/api'
import { AiForm } from '@/components/ai-form'
import IconRenderer from '@/components/IconRenderer.vue'
import IconSelector from '@/components/IconSelector.vue'
import ImageUpload from '@/components/image-upload/index.vue'
import { useDict } from '@/composables'
import { usePermissionStore, useUserStore } from '@/store'
import { request } from '@/utils'
import { getMenuRouteOptions } from '@/utils/menu-route-options'

defineOptions({ name: 'SystemMenu' })

const { dict } = useDict('sys_resource_type', 'sys_show_hide', 'sys_req_method', 'sys_link_open_target')

const resourceTypeOptions = computed(() => dict.value.sys_resource_type || [])
const visibleOptions = computed(() => dict.value.sys_show_hide || [])
const apiMethodOptions = computed(() => dict.value.sys_req_method || [])
const openTargetOptions = computed(() => dict.value.sys_link_open_target || [])

const permissionStore = usePermissionStore()
const userStore = useUserStore()

const currentUserClientCode = computed(() => userStore.userInfo?.userClient || 'pc')
const routeOptions = getMenuRouteOptions()

const pageRef = ref(null)
const formRef = ref(null)
const clientList = ref([])
const currentClientCode = ref(currentUserClientCode.value)
const loading = ref(false)
const submitLoading = ref(false)
const allResources = ref([])
const selectedResourceId = ref(0)
const selectedRow = ref(null)
const navigationExpandedKeys = ref([0])
const treeKeyword = ref('')
const resourceKeyword = ref('')
const resourceTypeFilter = ref(null)
const visibleFilter = ref(null)
const parentResourceOptions = ref([{ label: '顶级资源', value: 0, key: 0 }])
const pendingParentId = ref(null)
const pendingClientCode = ref(null)
const drawerVisible = ref(false)
const drawerMode = ref('add')
const formData = ref({})
const formIconTab = ref('font')
const tableIconSelectorRef = ref(null)
const tableIconEditRow = ref(null)
const tableIconValue = ref('')
const tableMaxHeight = ref(520)
let pageResizeObserver = null
let tableHeightFrame = null

const publicParams = computed(() => {
  if (currentClientCode.value)
    return { clientCode: currentClientCode.value }
  return {}
})

const drawerTitle = computed(() => drawerMode.value === 'edit' ? '编辑资源' : '新增资源')
const drawerWidth = computed(() => Math.min(860, Math.max(640, Math.floor(window.innerWidth * 0.58))))

const clientCodeOptions = computed(() => {
  return clientList.value.map(client => ({
    label: client.clientName,
    value: client.clientCode,
  }))
})

const resourceTypeFilterOptions = computed(() => {
  const options = resourceTypeOptions.value?.length
    ? resourceTypeOptions.value
    : [
        { label: '目录', value: 1 },
        { label: '菜单', value: 2 },
        { label: '按钮', value: 3 },
        { label: 'API', value: 4 },
      ]
  return options.map(item => ({ label: item.label, value: Number(item.value) }))
})

const visibleFilterOptions = computed(() => {
  const options = visibleOptions.value?.length
    ? visibleOptions.value
    : [
        { label: '显示', value: 1 },
        { label: '隐藏', value: 0 },
      ]
  return options.map(item => ({ label: item.label, value: Number(item.value) }))
})

const flatResources = computed(() => flattenResourceTree(allResources.value))

const currentNode = computed(() => {
  if (!selectedResourceId.value)
    return null
  return flatResources.value.find(item => item.id === selectedResourceId.value) || null
})

const activeResource = computed(() => selectedRow.value || currentNode.value)

const currentContextTitle = computed(() => {
  if (resourceKeyword.value || resourceTypeFilter.value !== null || visibleFilter.value !== null)
    return currentNode.value ? `${currentNode.value.resourceName} 下的匹配资源` : '全部匹配资源'
  return currentNode.value ? `${currentNode.value.resourceName} 下级资源` : '全部资源层级'
})

const activeChildSummary = computed(() => {
  const children = activeResource.value?.children || []
  return {
    menu: children.filter(item => Number(item.resourceType) === 1 || Number(item.resourceType) === 2).length,
    button: children.filter(item => Number(item.resourceType) === 3).length,
    api: children.filter(item => Number(item.resourceType) === 4).length,
  }
})

const resourceMetrics = computed(() => {
  const rows = flatResources.value
  return [
    { key: 'total', label: '全部资源', value: rows.length },
    { key: 'menu', label: '目录 / 菜单', value: rows.filter(item => Number(item.resourceType) === 1 || Number(item.resourceType) === 2).length },
    { key: 'button', label: '按钮权限', value: rows.filter(item => Number(item.resourceType) === 3).length },
    { key: 'api', label: 'API 权限', value: rows.filter(item => Number(item.resourceType) === 4).length },
    { key: 'hidden', label: '隐藏项', value: rows.filter(item => Number(item.visible) !== 1).length },
  ]
})

const navigationTreeData = computed(() => {
  const keyword = treeKeyword.value.trim().toLowerCase()
  const children = buildNavigationTree(allResources.value, keyword)
  return [
    {
      key: 0,
      id: 0,
      label: '全部资源',
      resourceName: '全部资源',
      resourceType: 0,
      children,
    },
  ]
})

const displayRows = computed(() => {
  const hasFilter = !!resourceKeyword.value.trim() || resourceTypeFilter.value !== null || visibleFilter.value !== null
  const baseRows = getContextRows()

  if (!hasFilter)
    return baseRows

  return baseRows.filter(row => matchesResourceFilter(row))
})

const typeStyleMap = {
  1: { text: '目录', icon: 'i-material-symbols:folder-outline', color: '#4C6EF5', bg: '#EDF2FF', fontWeight: '600' },
  2: { text: '菜单', icon: 'i-material-symbols:menu', color: '#40C057', bg: '#EBFBEE', fontWeight: '500' },
  3: { text: '按钮', icon: 'i-material-symbols:smart-button', color: '#FD7E14', bg: '#FFF4E6', fontWeight: '400' },
  4: { text: 'API', icon: 'i-material-symbols:api', color: '#FA5252', bg: '#FFF5F5', fontWeight: '400' },
}

watch(currentClientCode, async () => {
  selectedResourceId.value = 0
  selectedRow.value = null
  pendingParentId.value = null
  pendingClientCode.value = null
  await loadResourceTree()
})

onMounted(async () => {
  setupMenuPageLayout()
  await loadClientList()
  await loadResourceTree()
})

onBeforeUnmount(() => {
  pageResizeObserver?.disconnect?.()
  pageResizeObserver = null
  if (tableHeightFrame) {
    cancelAnimationFrame(tableHeightFrame)
    tableHeightFrame = null
  }
  pageRef.value?.closest?.('.nexus-page')?.classList.remove('menu-page-host-no-scroll')
})

function setupMenuPageLayout() {
  nextTick(() => {
    const pageEl = pageRef.value
    if (!pageEl)
      return

    pageEl.closest?.('.nexus-page')?.classList.add('menu-page-host-no-scroll')
    pageResizeObserver?.disconnect?.()
    pageResizeObserver = new ResizeObserver(() => scheduleTableHeightUpdate())
    pageResizeObserver.observe(pageEl)
    scheduleTableHeightUpdate()
  })
}

function scheduleTableHeightUpdate() {
  if (tableHeightFrame)
    cancelAnimationFrame(tableHeightFrame)
  tableHeightFrame = requestAnimationFrame(updateTableHeight)
}

function updateTableHeight() {
  tableHeightFrame = null
  const pageEl = pageRef.value
  if (!pageEl)
    return
  const bodyEl = pageEl.querySelector('.menu-workbench-body')
  const toolbarEl = pageEl.querySelector('.list-toolbar')
  const bodyHeight = bodyEl?.clientHeight || 560
  const toolbarHeight = toolbarEl?.offsetHeight || 56
  tableMaxHeight.value = Math.max(320, bodyHeight - toolbarHeight - 14)
}

async function loadClientList() {
  try {
    const res = await request.get('/system/client/list')
    if (res.code === 200) {
      clientList.value = res.data || []

      if (!currentClientCode.value) {
        const userClient = currentUserClientCode.value
        const matchedClient = clientList.value.find(client => client.clientCode === userClient)
        if (matchedClient)
          currentClientCode.value = userClient
      }
    }
  }
  catch (error) {
    console.error('加载客户端列表失败:', error)
  }
}

async function loadResourceTree() {
  loading.value = true
  try {
    const res = await request.get('/system/resource/tree', { params: publicParams.value })
    const list = normalizeListResponse(res)
    allResources.value = Array.isArray(list) ? list : []
    syncParentResourceOptions(allResources.value)
    expandNavigationTree()
    keepSelectionAvailable()
    await nextTick()
    scheduleTableHeightUpdate()
  }
  catch (error) {
    console.error('加载资源树失败:', error)
    window.$message?.error('加载资源树失败')
  }
  finally {
    loading.value = false
  }
}

function normalizeListResponse(res) {
  if (Array.isArray(res))
    return res
  if (Array.isArray(res?.data))
    return res.data
  if (Array.isArray(res?.data?.records))
    return res.data.records
  if (Array.isArray(res?.data?.list))
    return res.data.list
  return []
}

function keepSelectionAvailable() {
  if (!selectedResourceId.value)
    return
  const exists = flatResources.value.some(item => item.id === selectedResourceId.value)
  if (!exists) {
    selectedResourceId.value = 0
    selectedRow.value = null
  }
}

function handleClientTabChange(clientCode) {
  currentClientCode.value = clientCode
}

function flattenResourceTree(list = []) {
  const result = []
  const walk = (items, parent = null, level = 0) => {
    items.forEach((item) => {
      const normalized = { ...item, parent, level }
      result.push(normalized)
      if (item.children?.length)
        walk(item.children, normalized, level + 1)
    })
  }
  walk(Array.isArray(list) ? list : [])
  return result
}

function buildNavigationTree(list = [], keyword = '') {
  return (Array.isArray(list) ? list : [])
    .map((item) => {
      const type = Number(item.resourceType)
      const children = buildNavigationTree(item.children || [], keyword)
      const isNavigationNode = type === 1 || type === 2
      const matched = !keyword || String(item.resourceName || '').toLowerCase().includes(keyword) || String(item.path || '').toLowerCase().includes(keyword)

      if (!isNavigationNode && children.length === 0)
        return null
      if (keyword && !matched && children.length === 0)
        return null

      return {
        ...item,
        key: item.id,
        label: item.resourceName,
        children,
      }
    })
    .filter(Boolean)
}

function expandNavigationTree() {
  const keys = [0]
  const walk = (items = []) => {
    items.forEach((item) => {
      keys.push(item.id)
      if (item.children?.length)
        walk(item.children)
    })
  }
  walk(allResources.value.filter(item => Number(item.resourceType) === 1 || Number(item.resourceType) === 2 || item.children?.length))
  navigationExpandedKeys.value = keys
}

function collapseNavigationTree() {
  navigationExpandedKeys.value = [0]
}

function handleNavigationSelect(keys) {
  selectedResourceId.value = keys?.[0] ?? 0
  selectedRow.value = selectedResourceId.value
    ? flatResources.value.find(item => item.id === selectedResourceId.value) || null
    : null
  scheduleTableHeightUpdate()
}

function renderNavigationLabel({ option }) {
  const typeConfig = typeStyleMap[option.resourceType] || { text: '全部', icon: 'i-material-symbols:account-tree' }
  return h('div', { class: ['nav-tree-label', `type-${option.resourceType || 0}`] }, [
    h('span', { class: 'nav-tree-icon-shell' }, [
      h('i', { class: typeConfig.icon }),
    ]),
    h('span', { class: 'nav-tree-name' }, option.label),
    option.resourceType
      ? h('span', { class: 'nav-tree-count' }, [
          h('span', '下级'),
          h('strong', String(option.children?.length || 0)),
        ])
      : null,
  ])
}

function getContextRows() {
  if (!currentNode.value)
    return flatResources.value
  return flattenResourceTree(currentNode.value.children || [])
}

function matchesResourceFilter(row) {
  const keyword = resourceKeyword.value.trim().toLowerCase()
  const typeMatched = resourceTypeFilter.value === null || Number(row.resourceType) === Number(resourceTypeFilter.value)
  const visibleMatched = visibleFilter.value === null || Number(row.visible) === Number(visibleFilter.value)
  const keywordMatched = !keyword || [
    row.resourceName,
    row.path,
    row.component,
    row.perms,
    row.apiUrl,
    row.remark,
  ].some(value => String(value || '').toLowerCase().includes(keyword))

  return typeMatched && visibleMatched && keywordMatched
}

function getDisplayLevel(row) {
  const level = Number(row.level || 0)
  if (!Number.isFinite(level) || level < 0)
    return 0
  return Math.min(level, 5)
}

function getResourceTypeConfig(type) {
  return typeStyleMap[Number(type)] || { text: '未知', icon: 'i-material-symbols:radio-button-unchecked', color: '#adb5bd' }
}

function getRenderableIcon(row) {
  const icon = String(row?.icon || '').trim()
  if (!icon)
    return ''

  if (isImageIconValue(icon) || icon.startsWith('i-') || icon.startsWith('ionicons5:') || icon.startsWith('local:') || icon.startsWith('local-image:'))
    return icon

  if (icon === 'ProfileOutlined')
    return 'i-material-symbols:person-outline'

  return ''
}

async function handleSortCommit(row, value) {
  row._editingSort = false
  await handleInlineUpdate(row, 'sort', value ?? 0)
}

function getMoreActionOptions(row) {
  return [
    { label: '新增子项', key: 'add' },
    { label: '更换图标', key: 'icon', disabled: Number(row.resourceType) > 2 },
    { label: '复制路由', key: 'copyPath', disabled: !row.path },
    { label: '复制权限', key: 'copyPerms', disabled: !row.perms },
    { label: '删除', key: 'delete' },
  ]
}

function getResourceSubtitle(row) {
  const parts = []
  if (row.parent?.resourceName)
    parts.push(`上级：${row.parent.resourceName}`)
  else
    parts.push('顶级资源')

  const childCount = row.children?.length || 0
  if (childCount)
    parts.push(`子项 ${childCount}`)

  return parts.join(' · ')
}

function getPrimaryRouteText(row) {
  if (Number(row.resourceType) === 4)
    return row.apiUrl || '-'
  if (Number(row.resourceType) === 3)
    return row.perms || '-'
  return row.path || '-'
}

function getSecondaryRouteText(row) {
  if (Number(row.resourceType) === 4)
    return row.apiMethod || ''
  if (Number(row.resourceType) === 3)
    return ''
  return row.component || ''
}

function getResourceTypeText(type) {
  return typeStyleMap[Number(type)]?.text || '未知'
}

function getClientDisplayName(clientCode) {
  return clientList.value.find(item => item.clientCode === clientCode)?.clientName || clientCode || '-'
}

function getSsoTargetClientOptions(formValue) {
  const currentClient = formValue?.clientCode || pendingClientCode.value || currentClientCode.value
  return clientList.value
    .filter(client => client.clientCode && client.clientCode !== currentClient)
    .map(client => ({
      label: client.clientName,
      value: client.clientCode,
    }))
}

function handleMoreAction(key, row) {
  if (key === 'add')
    handleAdd(row)
  else if (key === 'icon')
    openTableIconSelector(row)
  else if (key === 'delete')
    handleDelete(row)
  else if (key === 'copyPath')
    copyText(row.path, '路由已复制')
  else if (key === 'copyPerms')
    copyText(row.perms, '权限标识已复制')
}

async function copyText(text, message) {
  if (!text)
    return
  try {
    await navigator.clipboard?.writeText(text)
    window.$message?.success(message)
  }
  catch {
    window.$message?.warning('当前浏览器不支持自动复制')
  }
}

function syncParentResourceOptions(list = allResources.value) {
  const convertToTreeSelect = (items = []) => {
    return items.map(item => ({
      label: item.resourceName,
      value: item.id,
      key: item.id,
      children: item.children && item.children.length > 0
        ? convertToTreeSelect(item.children)
        : undefined,
    }))
  }
  parentResourceOptions.value = [
    { label: '顶级资源', value: 0, key: 0 },
    ...convertToTreeSelect(Array.isArray(list) ? list : []),
  ]
}

function handleAddRoot() {
  handleAdd(null)
}

function handleAdd(row) {
  syncParentResourceOptions()
  pendingParentId.value = row?.id ?? selectedResourceId.value ?? 0
  pendingClientCode.value = row?.clientCode || currentNode.value?.clientCode || currentClientCode.value || null
  drawerMode.value = 'add'
  formData.value = beforeRenderForm(null)
  drawerVisible.value = true
  nextTick(() => formRef.value?.restoreValidation?.())
}

async function handleEdit(row) {
  syncParentResourceOptions()
  drawerMode.value = 'edit'
  const detail = await loadResourceDetail(row)
  formData.value = beforeRenderForm({ ...row, ...detail })
  drawerVisible.value = true
  selectedRow.value = row
  nextTick(() => formRef.value?.restoreValidation?.())
}

async function loadResourceDetail(row) {
  try {
    const res = await request.post('/system/resource/getById', null, { params: { id: row.id } })
    return res?.data || {}
  }
  catch (error) {
    console.warn('加载资源详情失败，使用列表数据:', error)
    return {}
  }
}

function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除「${row.resourceName}」吗？删除后将无法恢复。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        loading.value = true
        const res = await request.post('/system/resource/remove', null, { params: { id: row.id } })
        if (res.code === 200) {
          window.$message.success('删除成功')
          if (selectedRow.value?.id === row.id)
            selectedRow.value = null
          await refreshSystemMenu()
          await loadResourceTree()
        }
      }
      catch (error) {
        console.error('删除资源失败:', error)
        window.$message.error('删除失败')
      }
      finally {
        loading.value = false
      }
    },
  })
}

async function handleDrawerSubmit() {
  try {
    await formRef.value?.validate()
    const normalized = beforeSubmit(formRef.value?.getFormData?.() || formData.value)
    if (normalized === false)
      return

    submitLoading.value = true
    const apiUrl = drawerMode.value === 'edit' ? '/system/resource/edit' : '/system/resource/add'
    const res = await request.post(apiUrl, normalized)
    if (res.code === 200) {
      window.$message.success('保存成功')
      drawerVisible.value = false
      await refreshSystemMenu()
      await loadResourceTree()
      selectSavedResource(normalized)
    }
    else {
      window.$message.error(res.msg || '保存失败')
    }
  }
  catch (error) {
    console.error('保存资源失败:', error)
    window.$message.error('保存失败')
  }
  finally {
    submitLoading.value = false
  }
}

function selectSavedResource(resource) {
  if (!resource?.id)
    return
  const saved = flatResources.value.find(item => item.id === resource.id)
  if (saved) {
    selectedResourceId.value = saved.parentId || 0
    selectedRow.value = saved
  }
}

async function handleInlineUpdate(row, field, value) {
  const previousValue = row[field]
  try {
    row[field] = value
    const res = await request.post('/system/resource/edit', {
      id: row.id,
      [field]: value,
    })
    if (res.code === 200) {
      window.$message.success('更新成功')
      await refreshSystemMenu()
      await loadResourceTree()
    }
    else {
      row[field] = previousValue
      window.$message.error(res.msg || '更新失败')
    }
  }
  catch (error) {
    row[field] = previousValue
    console.error('内联更新失败:', error)
    window.$message.error('更新失败')
  }
}

async function refreshSystemMenu() {
  try {
    const res = await api.getMenu()
    if (res.code === 200 && res.data)
      permissionStore.setMenuData(res.data)
  }
  catch (error) {
    console.error('刷新系统菜单失败:', error)
  }
}

function beforeRenderForm(data) {
  formIconTab.value = isImageIconValue(data?.icon) ? 'image' : 'font'

  if (!data) {
    const parentId = pendingParentId.value !== null ? pendingParentId.value : 0
    const clientCode = pendingClientCode.value || currentClientCode.value || 'pc'
    pendingParentId.value = null
    pendingClientCode.value = null
    return { parentId, clientCode, resourceType: 2, sort: 0, visible: 1, menuStatus: 1, ssoEnabled: 0, ssoTargetClient: '', openTarget: '_self' }
  }

  pendingParentId.value = null
  pendingClientCode.value = null
  return {
    ...data,
    ssoEnabled: data.ssoEnabled ?? 0,
    ssoTargetClient: data.ssoTargetClient || '',
    openTarget: data.openTarget || '_self',
  }
}

function beforeSubmit(sourceData) {
  const resourceType = sourceData.resourceType !== undefined ? Number(sourceData.resourceType) : sourceData.resourceType
  const ssoEnabled = resourceType === 2 ? Number(sourceData.ssoEnabled || 0) : 0
  const ssoTargetClient = ssoEnabled === 1 ? (sourceData.ssoTargetClient || '').trim() : ''
  const openTarget = resourceType === 2 && ssoEnabled === 1 ? (sourceData.openTarget || '_self') : '_self'

  if (resourceType === 2 && ssoEnabled === 1) {
    if (!ssoTargetClient) {
      window.$message.error('请选择目标子系统')
      return false
    }
    if (ssoTargetClient === sourceData.clientCode) {
      window.$message.error('目标子系统不能与当前客户端相同')
      return false
    }
  }

  return {
    ...sourceData,
    path: normalizeRouteInput(sourceData.path),
    component: normalizeComponentValue(sourceData.component),
    resourceType,
    ssoEnabled,
    ssoTargetClient,
    openTarget,
  }
}

function isImageIconValue(value) {
  if (!value || typeof value !== 'string')
    return false

  const iconValue = value.trim()
  if (!iconValue)
    return false
  if (iconValue.startsWith('local-image:'))
    return false

  return iconValue.startsWith('http://')
    || iconValue.startsWith('https://')
    || iconValue.startsWith('data:')
    || iconValue.startsWith('blob:')
    || iconValue.startsWith('/api/file/')
    || iconValue.startsWith('forge-file://')
    || /^[a-f0-9]{32}$/i.test(iconValue)
    || /\.(?:png|jpe?g|webp|gif|svg|avif)(?:\?.*)?$/i.test(iconValue)
}

function getFontIconValue(value) {
  return isImageIconValue(value) ? '' : (value || '')
}

function getImageIconValue(value) {
  return isImageIconValue(value) ? value : ''
}

function handleFormIconTabChange(tab) {
  formIconTab.value = tab
}

async function openTableIconSelector(row) {
  tableIconEditRow.value = row
  tableIconValue.value = row.icon || ''
  await nextTick()
  tableIconSelectorRef.value?.open?.()
}

async function handleTableIconSelected(value) {
  const row = tableIconEditRow.value
  if (!row)
    return

  tableIconValue.value = value
  tableIconEditRow.value = null
  await handleInlineUpdate(row, 'icon', value)
}

function getUsedRoutePathSet(currentFormData) {
  return new Set(
    flatResources.value
      .filter(row => row.id !== currentFormData?.id)
      .map(row => row.path)
      .filter(Boolean),
  )
}

function matchesRouteKeyword(option, keyword) {
  const value = String(keyword || '').trim().toLowerCase()
  if (!value)
    return true
  return option.path.toLowerCase().includes(value)
    || option.component.toLowerCase().includes(value)
}

function normalizeRouteInput(routePath) {
  const value = String(routePath || '').trim()
  if (!value)
    return ''
  return value.startsWith('/') ? value.replace(/\/+/g, '/') : `/${value.replace(/\/+/g, '/')}`
}

function getAvailableRouteOptions(currentFormData, keyword = '') {
  const usedPathSet = getUsedRoutePathSet(currentFormData)
  const currentPath = currentFormData?.path
  return routeOptions
    .filter(option => option.path === currentPath || !usedPathSet.has(option.path))
    .filter(option => matchesRouteKeyword(option, keyword))
    .map(option => ({
      ...option,
      disabled: option.path !== currentPath && usedPathSet.has(option.path),
      label: option.path,
      value: option.path,
    }))
}

function renderRouteOptionLabel(option) {
  return h('div', { class: 'route-option-label' }, [
    h('span', { class: 'route-option-path' }, option.path),
  ])
}

function normalizeComponentValue(componentPath) {
  const value = String(componentPath || '').trim()
  if (!value)
    return ''
  return value.replace(/^\/+/, '').replace(/\/$/, '')
}

function handleRoutePathChange(routePath, updateValue, currentFormData) {
  const normalizedPath = normalizeRouteInput(routePath)
  if (!currentFormData) {
    updateValue(normalizedPath)
    return
  }

  currentFormData.path = normalizedPath
  autoFillComponentFromRoute(currentFormData)
  updateValue(normalizedPath)
}

function autoFillComponentFromRoute(currentFormData) {
  if (!currentFormData || !currentFormData.path || currentFormData.component)
    return

  const selected = routeOptions.find(option => option.path === currentFormData.path)
  if (selected?.component)
    currentFormData.component = selected.component
}

function getAvailableComponentOptions(_currentFormData, keyword = '') {
  return routeOptions
    .filter(option => matchesRouteKeyword(option, keyword))
    .map(option => ({
      ...option,
      label: option.component,
      value: option.component,
    }))
}

function renderComponentOptionLabel(option) {
  return h('div', { class: 'route-option-label' }, [
    h('span', { class: 'route-option-path' }, option.component),
    h('span', { class: 'route-option-component' }, `页面路由：${option.path}`),
  ])
}

function handleComponentPathChange(componentPath, updateValue, currentFormData) {
  const normalizedComponent = normalizeComponentValue(componentPath)
  updateValue(normalizedComponent)
  if (!currentFormData)
    return

  currentFormData.component = normalizedComponent
  const selected = routeOptions.find(option => option.component === normalizedComponent)
  if (selected?.path && !currentFormData.path)
    currentFormData.path = selected.path
}

const editSchema = computed(() => [
  {
    field: '__basicDivider',
    type: 'divider',
    label: '基础信息',
    props: { titlePlacement: 'left' },
    span: 2,
  },
  {
    field: 'parentId',
    label: '上级资源',
    type: 'treeSelect',
    span: 1,
    defaultValue: 0,
    props: {
      placeholder: '请选择上级资源',
      clearable: true,
      filterable: true,
      defaultExpandAll: false,
      keyField: 'value',
      labelField: 'label',
      childrenField: 'children',
    },
    options: () => parentResourceOptions.value,
  },
  {
    field: 'resourceName',
    label: '资源名称',
    type: 'input',
    span: 1,
    rules: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
    props: { placeholder: '请输入资源名称' },
  },
  {
    field: 'resourceType',
    label: '资源类型',
    type: 'radio',
    span: 2,
    defaultValue: 1,
    rules: [
      {
        required: true,
        message: '请选择资源类型',
        trigger: 'change',
        validator: (_rule, value) => {
          if (value === null || value === undefined || value === '')
            return new Error('请选择资源类型')
          return true
        },
      },
    ],
    props: {
      options: [
        { label: '目录', value: 1 },
        { label: '菜单', value: 2 },
        { label: '按钮', value: 3 },
        { label: 'API', value: 4 },
      ],
    },
  },
  {
    field: 'clientCode',
    label: '客户端',
    type: 'radio',
    span: 2,
    defaultValue: 'pc',
    rules: [
      {
        required: true,
        message: '请选择客户端',
        trigger: 'change',
        validator: (_rule, value) => {
          if (!value)
            return new Error('请选择客户端')
          return true
        },
      },
    ],
    props: { options: clientCodeOptions.value },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'inputNumber',
    span: 1,
    defaultValue: 0,
    props: { placeholder: '排序值', min: 0 },
  },
  {
    field: '__menuDivider',
    type: 'divider',
    label: '目录/菜单配置',
    props: { titlePlacement: 'left' },
    span: 2,
    vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
  },
  {
    field: 'icon',
    label: '图标',
    type: 'slot',
    span: 2,
    slotName: 'icon',
    vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
  },
  {
    field: 'path',
    label: '路由地址',
    type: 'slot',
    slotName: 'path',
    span: 2,
    vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
  },
  {
    field: 'component',
    label: '组件路径',
    type: 'slot',
    span: 2,
    slotName: 'component',
    vIf: currentFormData => currentFormData.resourceType === 2,
  },
  {
    field: 'redirect',
    label: '重定向地址',
    type: 'input',
    span: 1,
    props: { placeholder: '重定向地址' },
    vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
  },
  {
    field: 'isExternal',
    label: '是否外链',
    type: 'radio',
    span: 1,
    defaultValue: 0,
    props: { options: [{ label: '否', value: 0 }, { label: '是', value: 1 }] },
    vIf: currentFormData => currentFormData.resourceType === 2,
  },
  {
    field: 'ssoEnabled',
    label: '启用SSO',
    type: 'switch',
    span: 1,
    defaultValue: 0,
    checkedValue: 1,
    uncheckedValue: 0,
    checkedText: '开启',
    uncheckedText: '关闭',
    vIf: currentFormData => currentFormData.resourceType === 2,
  },
  {
    field: 'ssoTargetClient',
    label: '目标子系统',
    type: 'select',
    span: 1,
    rules: [{ required: true, message: '请选择目标子系统', trigger: 'change' }],
    props: {
      placeholder: '请选择目标子系统',
      clearable: true,
    },
    options: ({ formData: currentFormData }) => getSsoTargetClientOptions(currentFormData),
    vIf: currentFormData => currentFormData.resourceType === 2 && currentFormData.ssoEnabled === 1,
  },
  {
    field: 'openTarget',
    label: '打开方式',
    type: 'radio',
    span: 1,
    defaultValue: '_self',
    props: { options: openTargetOptions.value },
    vIf: currentFormData => currentFormData.resourceType === 2 && currentFormData.ssoEnabled === 1,
  },
  {
    field: 'keepAlive',
    label: '是否缓存',
    type: 'radio',
    span: 1,
    defaultValue: 0,
    props: { options: [{ label: '否', value: 0 }, { label: '是', value: 1 }] },
    vIf: currentFormData => currentFormData.resourceType === 2,
  },
  {
    field: 'alwaysShow',
    label: '总是显示',
    type: 'radio',
    span: 1,
    defaultValue: 0,
    props: { options: [{ label: '否', value: 0 }, { label: '是', value: 1 }] },
    vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
  },
  {
    field: '__permissionDivider',
    type: 'divider',
    label: '按钮/API配置',
    props: { titlePlacement: 'left' },
    span: 2,
    vIf: currentFormData => currentFormData.resourceType === 3 || currentFormData.resourceType === 4,
  },
  {
    field: 'perms',
    label: '权限标识',
    type: 'input',
    span: 1,
    props: { placeholder: 'sys:user:add' },
    vIf: currentFormData => currentFormData.resourceType === 3 || currentFormData.resourceType === 4,
  },
  {
    field: 'apiMethod',
    label: '请求方法',
    type: 'select',
    span: 1,
    defaultValue: 'GET',
    props: { placeholder: '请求方法', options: apiMethodOptions.value },
    vIf: currentFormData => currentFormData.resourceType === 4,
  },
  {
    field: 'apiUrl',
    label: '接口地址',
    type: 'input',
    span: 1,
    props: { placeholder: '/system/user/list' },
    vIf: currentFormData => currentFormData.resourceType === 4,
  },
  {
    field: '__statusDivider',
    type: 'divider',
    label: '状态配置',
    props: { titlePlacement: 'left' },
    span: 2,
  },
  {
    field: 'visible',
    label: '显示状态',
    type: 'radio',
    span: 1,
    defaultValue: 1,
    props: { options: [{ label: '显示', value: 1 }, { label: '隐藏', value: 0 }] },
  },
  {
    field: 'menuStatus',
    label: '菜单状态',
    type: 'radio',
    span: 1,
    defaultValue: 1,
    props: { options: [{ label: '显示', value: 1 }, { label: '隐藏', value: 0 }] },
    vIf: currentFormData => currentFormData.resourceType === 1 || currentFormData.resourceType === 2,
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: { placeholder: '请输入备注', rows: 3 },
  },
])
</script>

<style scoped>
.system-menu-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  gap: 10px;
}

:global(.nexus-page.menu-page-host-no-scroll) {
  overflow: hidden !important;
}

:global(.nexus-page.menu-page-host-no-scroll) > .system-menu-page {
  min-height: 0;
}

.menu-workbench-header {
  flex-shrink: 0;
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: 6px;
  padding: 12px 14px 8px;
}

.header-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.header-title {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.title-mark {
  width: 4px;
  height: 34px;
  border-radius: 3px;
  background: linear-gradient(180deg, #2f6fed, #12b886);
  margin-top: 2px;
}

.header-title h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 17px;
  line-height: 1.35;
  font-weight: 650;
  letter-spacing: 0;
}

.header-title p {
  margin: 3px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.client-tabs-container {
  margin-top: 8px;
}

.tab-label {
  font-size: 13px;
}

.metric-strip {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid var(--border-light);
}

.metric-item {
  display: flex;
  align-items: baseline;
  gap: 5px;
  min-width: 0;
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: 0;
}

.metric-value {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1;
  font-weight: 650;
}

.metric-label {
  color: var(--text-tertiary);
  font-size: 12px;
  white-space: nowrap;
}

.menu-workbench-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(220px, 260px) minmax(520px, 1fr) minmax(280px, 340px);
  gap: 10px;
}

.tree-pane,
.list-pane,
.detail-pane {
  min-height: 0;
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  overflow: hidden;
}

.tree-pane,
.detail-pane {
  display: flex;
  flex-direction: column;
}

.pane-head,
.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 12px;
  background: #fbfcfe;
  border-bottom: 1px solid var(--border-light);
}

.pane-title,
.context-title {
  display: block;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.25;
  font-weight: 650;
}

.pane-subtitle,
.context-subtitle {
  display: block;
  margin-top: 2px;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.2;
}

.tree-search {
  margin: 8px 10px 6px;
  width: calc(100% - 24px);
}

.tree-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0 6px 8px;
}

.list-pane {
  min-width: 0;
}

.list-toolbar {
  align-items: flex-start;
  min-height: 54px;
}

.list-context {
  min-width: 150px;
  padding-top: 2px;
}

.list-filters {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
}

.keyword-input {
  width: 240px;
}

.type-filter {
  width: 104px;
}

.visible-filter {
  width: 96px;
}

.detail-pane {
  padding: 0;
  background: #fbfcfe;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 14px 12px;
  background: linear-gradient(180deg, #f8fbff 0%, #fff 100%);
  border-bottom: 1px solid var(--border-light);
}

.detail-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.detail-title-text {
  min-width: 0;
}

.detail-title-text span {
  display: block;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.3;
  font-weight: 650;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-title-text small {
  display: block;
  margin-top: 2px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.detail-actions {
  display: flex;
  gap: 8px;
  padding: 10px 14px;
  background: #fff;
  border-bottom: 1px solid var(--border-light);
}

.detail-section {
  margin: 10px;
  padding: 12px;
  background: #fff;
  border: 1px solid #edf1f5;
  border-radius: 6px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 3%);
}

.detail-section + .detail-section {
  margin-top: 0;
}

.detail-section:last-child {
  border-bottom: 1px solid var(--border-light);
}

.section-label {
  display: block;
  margin-bottom: 10px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 650;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
  margin: 0;
}

.detail-grid div {
  min-width: 0;
}

.detail-grid dt {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.2;
}

.detail-grid dd {
  margin: 4px 0 0;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.35;
  word-break: break-all;
}

.detail-grid div:nth-child(n + 3) {
  grid-column: 1 / -1;
}

.child-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.child-summary span {
  padding: 7px 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
}

.remark-text {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.55;
  word-break: break-word;
}

.detail-empty {
  flex: 1;
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--text-tertiary);
  font-size: 13px;
  text-align: center;
  padding: 24px;
}

.detail-empty i {
  font-size: 28px;
  color: var(--text-disabled, #adb5bd);
}

.resource-list-scroll {
  overflow: auto;
}

.resource-list-scroll :deep(.n-spin-content) {
  min-height: 100%;
  align-items: stretch !important;
  justify-content: flex-start !important;
}

.resource-list-empty {
  height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-tertiary);
  font-size: 13px;
}

.resource-list-empty i {
  font-size: 26px;
  color: #adb5bd;
}

.resource-list-row {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(180px, 0.8fr) 58px 44px 66px;
  gap: 10px;
  align-items: center;
  min-height: 46px;
  padding: 6px 10px;
  border-bottom: 1px solid #edf1f5;
  cursor: pointer;
  transition: background 0.16s ease;
}

.resource-list-row:hover {
  background: #f8fbff;
}

.resource-list-row.is-active {
  background: #eef5ff;
}

.resource-list-main {
  display: flex;
  align-items: center;
  min-width: 0;
}

.resource-level-spacer {
  position: relative;
  display: inline-flex;
  flex-shrink: 0;
  height: 28px;
}

.resource-level-spacer.has-level::before {
  content: '';
  position: absolute;
  right: 13px;
  top: -14px;
  bottom: -14px;
  width: 1px;
  background: #e2e8f0;
}

.resource-level-spacer.has-level::after {
  content: '';
  position: absolute;
  right: 4px;
  top: 50%;
  width: 10px;
  height: 1px;
  background: #d4dce7;
}

.resource-icon-shell {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 8px;
  border-radius: 4px;
  background: #f4f7fb;
  color: var(--text-tertiary);
}

.resource-icon-shell i {
  font-size: 15px;
}

.resource-list-copy {
  min-width: 0;
  flex: 1;
}

.resource-title-line {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.resource-meta-line {
  margin-top: 2px;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 550;
}

.resource-type-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1;
  padding: 0;
  border: 0;
  background: transparent;
  white-space: nowrap;
}

.resource-type-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  flex-shrink: 0;
}

.resource-client-chip {
  flex-shrink: 0;
  max-width: 74px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-tertiary);
  font-size: 11px;
}

.resource-route-summary {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.resource-route-summary span {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-route-summary small {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-list-sort,
.resource-list-status,
.resource-list-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
}

.resource-list-actions {
  gap: 2px;
}

.inline-sort-input {
  width: 54px;
}

.sort-chip {
  height: 24px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
}

.sort-chip {
  min-width: 30px;
  padding: 0 6px;
  color: var(--text-secondary);
}

.sort-chip:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.table-icon-action {
  width: 24px;
  height: 24px;
}

.table-icon-action i {
  font-size: 15px;
}

.visibility-icon-button.is-visible {
  color: #2f9e44;
}

.visibility-icon-button.is-hidden {
  color: #868e96;
}

.tree-pane :deep(.nav-tree-label) {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-width: 0;
  height: 26px;
  line-height: 26px;
}

.tree-pane :deep(.nav-tree-icon-shell) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 5px;
  background: #f1f5f9 !important;
  color: #64748b !important;
  flex-shrink: 0;
}

.tree-pane :deep(.nav-tree-label i) {
  font-size: 14px;
  line-height: 1;
}

.tree-pane :deep(.nav-tree-label.type-0 .nav-tree-icon-shell) {
  background: #f1f5f9 !important;
  color: #64748b !important;
}

.tree-pane :deep(.nav-tree-label.type-1 .nav-tree-icon-shell) {
  background: #eaf2ff !important;
  color: #3b82f6 !important;
}

.tree-pane :deep(.nav-tree-label.type-2 .nav-tree-icon-shell) {
  background: #eaf8ef !important;
  color: #16a34a !important;
}

.tree-pane :deep(.nav-tree-label.type-3 .nav-tree-icon-shell) {
  background: #fff1e6 !important;
  color: #ea580c !important;
}

.tree-pane :deep(.nav-tree-label.type-4 .nav-tree-icon-shell) {
  background: #feeceb !important;
  color: #dc2626 !important;
}

.tree-pane :deep(.nav-tree-name) {
  display: block;
  min-width: 0;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 26px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-pane :deep(.nav-tree-count) {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 46px;
  padding: 1px 7px;
  border-radius: 999px;
  background: #eff6ff !important;
  border: 1px solid #93c5fd !important;
  color: #1d4ed8 !important;
  font-size: 11px;
  line-height: 16px;
  font-weight: 600;
  justify-content: center;
}

.tree-pane :deep(.nav-tree-count strong) {
  color: #1e3a8a !important;
  font-weight: 750;
  font-size: 12px;
}

.tree-pane :deep(.n-tree-node-content) {
  min-height: 28px;
  height: 28px;
  align-items: center;
  border-radius: 4px;
  padding-left: 4px;
}

.tree-pane :deep(.n-tree-node-content__text) {
  display: flex;
  align-items: center;
  min-width: 0;
  flex: 1;
  height: 28px;
  padding-left: 3px;
}

.tree-pane :deep(.n-tree-node-switcher) {
  height: 28px;
  align-items: center;
  margin-right: 3px;
}

.tree-pane :deep(.n-tree-node-indent) {
  width: 15px;
}

.tree-pane :deep(.n-tree-node-content:hover) {
  background: #f6f8fb;
}

.tree-pane :deep(.n-tree-node--selected .n-tree-node-content) {
  background: #edf4ff;
}

.tree-pane :deep(.n-tree-node--selected > .n-tree-node-content::before) {
  display: none !important;
  content: none !important;
}

.tree-pane :deep(.n-tree-node--selected .nav-tree-icon-shell),
.tree-pane :deep(.n-tree-node--selected .nav-tree-name) {
  color: var(--primary-color, #2f6fed);
}

.tree-pane :deep(.n-tree-node--selected .nav-tree-count) {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #1d4ed8;
}

.tree-pane :deep(.n-tree-node--selected .nav-tree-count strong) {
  color: #1d4ed8;
}

.list-pane :deep(.n-data-table-th) {
  height: 34px;
  background: #fbfcfe;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.list-pane :deep(.n-data-table-td) {
  height: 42px;
}

.list-pane :deep(.n-data-table-tr:hover .n-data-table-td) {
  background: #f7faff;
}

.icon-selector-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.icon-upload-container {
  padding: 8px 0;
}

.route-select-field {
  width: 100%;
}

.route-select-hint {
  margin-top: 6px;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.4;
}

.route-option-label {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.25;
}

.route-option-path {
  color: var(--text-primary);
  font-size: 13px;
}

.route-option-component {
  color: var(--text-tertiary);
  font-size: 12px;
}

:deep(.is-active-row td) {
  background: #eef5ff !important;
}

:deep(.n-data-table) {
  min-height: 0;
}

:deep(.n-form) {
  --n-feedback-padding: 2px 0 0;
}

:deep(.n-form-item-blank) {
  min-height: auto;
}

:deep(.n-divider) {
  margin: 0 0 8px 0;
}

:deep(.n-divider:not(:first-child)) {
  margin-top: 12px;
}

:deep(.n-radio-group) {
  flex-wrap: wrap;
  gap: 4px 16px;
}

@media (max-width: 1280px) {
  .menu-workbench-body {
    grid-template-columns: 230px minmax(460px, 1fr);
  }

  .detail-pane {
    display: none;
  }
}

@media (max-width: 900px) {
  .menu-workbench-header {
    padding: 10px;
  }

  .header-main,
  .list-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions,
  .list-filters {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .metric-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .menu-workbench-body {
    grid-template-columns: 1fr;
    overflow: auto;
  }

  .tree-pane {
    min-height: 260px;
  }

  .keyword-input {
    width: min(100%, 280px);
  }
}
</style>
