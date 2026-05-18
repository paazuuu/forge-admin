<template>
  <div ref="pageRef" class="system-menu-page">
    <!-- 客户端切换 Tab -->
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

    <AiCrudPage
      ref="crudRef"
      api="/system/resource"
      :api-config="{
        list: 'get@/system/resource/tree',
        detail: 'post@/system/resource/getById',
        add: 'post@/system/resource/add',
        update: 'post@/system/resource/edit',
        delete: 'post@/system/resource/remove',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-render-list="beforeRenderList"
      :before-submit="beforeSubmit"
      :before-render-form="beforeRenderForm"
      :public-params="publicParams"
      :max-height="tableMaxHeight"
      row-key="id"
      :edit-grid-cols="2"
      modal-width="800px"
      :show-pagination="false"
      :lazy="false"
      add-button-text="新增资源"
      :table-props="{
        indent: 24,
        expandOnClick: true,
        expandedRowKeys: expandedKeys,
        onUpdateExpandedRowKeys: handleExpandedKeysUpdate,
        rowProps,
        virtualScroll: true,
        minRowHeight: 38,
      }"
      @submit-success="handleSubmitSuccess"
      @add="handleToolbarAdd"
      @load-list-success="handleListLoaded"
      @load-list-error="handleListLoaded"
    >
      <!-- 自定义工具栏 -->
      <template #toolbar-end>
        <n-button size="small" @click="toggleExpandAll">
          <template #icon>
            <i :class="expandAll ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
          </template>
          {{ expandAll ? '折叠全部' : '展开全部' }}
        </n-button>
      </template>

      <!-- 自定义图标列 -->
      <template #table-icon="{ row }">
        <div class="inline-edit-cell" @click.stop>
          <div class="inline-edit-preview" @click="openTableIconSelector(row)">
            <IconRenderer v-if="row.icon" :key="row.icon" :icon="row.icon" :font-size="16" />
            <span v-else class="text-xs text-gray-400">选择</span>
          </div>
        </div>
      </template>

      <!-- 自定义排序列 -->
      <template #table-sort="{ row }">
        <div class="inline-edit-cell" @click.stop>
          <div class="inline-edit-preview" @click="row._editingSort = true">
            <span class="sort-value">{{ row.sort }}</span>
          </div>
          <NInputNumber
            v-if="row._editingSort"
            :value="row.sort"
            :min="0"
            :show-button="false"
            size="small"
            style="width: 80px"
            @update:value="(value) => { handleInlineUpdate(row, 'sort', value); row._editingSort = false }"
            @blur="row._editingSort = false"
          />
        </div>
      </template>

      <!-- 自定义表单 - 图标选择 -->
      <template #form-icon="{ value, updateValue }">
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
                placeholder="或手动输入图标名称（如: i-mdi-home）"
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

      <template #form-path="{ value, updateValue, formData }">
        <div class="route-select-field">
          <NAutoComplete
            :value="value"
            :options="getAvailableRouteOptions(formData, value)"
            clearable
            placeholder="输入或选择页面路由，如 /system/user"
            :render-label="renderRouteOptionLabel"
            @update:value="routePath => handleRoutePathChange(routePath, updateValue, formData)"
          />
          <div class="route-select-hint">
            可搜索已有页面路由
          </div>
          <div v-if="formData?.component" class="route-select-hint" style="margin-top: 2px">
            组件路径：{{ formData.component }}
          </div>
        </div>
      </template>

      <template #form-component="{ value, updateValue, formData }">
        <div class="route-select-field">
          <NAutoComplete
            :value="normalizeComponentValue(value)"
            :options="getAvailableComponentOptions(formData, value)"
            clearable
            placeholder="输入或选择组件路径，如 system/user"
            :render-label="renderComponentOptionLabel"
            @update:value="component => handleComponentPathChange(component, updateValue, formData)"
          />
          <div class="route-select-hint">
            可搜索已有组件路径
          </div>
        </div>
      </template>
    </AiCrudPage>

    <IconSelector
      ref="tableIconSelectorRef"
      :trigger="false"
      :model-value="tableIconValue"
      @update:model-value="handleTableIconSelected"
    />

    <div v-if="menuInitialLoading" class="menu-loading-skeleton">
      <div class="skeleton-toolbar">
        <span v-for="item in 4" :key="item" />
      </div>
      <div class="skeleton-table">
        <div v-for="row in 9" :key="row" class="skeleton-row">
          <span class="skeleton-cell w-lg" />
          <span class="skeleton-cell w-sm" />
          <span class="skeleton-cell w-md" />
          <span class="skeleton-cell w-sm" />
          <span class="skeleton-cell w-lg" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { NAutoComplete, NInputNumber, NTag } from 'naive-ui'
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import api from '@/api'
import { AiCrudPage } from '@/components/ai-form'
import IconRenderer from '@/components/IconRenderer.vue'
import IconSelector from '@/components/IconSelector.vue'
import ImageUpload from '@/components/image-upload/index.vue'
import { usePermissionStore, useUserStore } from '@/store'
import { request } from '@/utils'
import { getMenuRouteOptions } from '@/utils/menu-route-options'

defineOptions({ name: 'SystemMenu' })

const permissionStore = usePermissionStore()
const userStore = useUserStore()

const currentUserClientCode = computed(() => userStore.userInfo?.userClient || 'pc')

// 客户端列表（从后端动态加载）
const clientList = ref([])
const currentClientCode = ref(currentUserClientCode.value)

// 公共搜索参数（用于 Tab 切换筛选）
const publicParams = computed(() => {
  if (currentClientCode.value) {
    return { clientCode: currentClientCode.value }
  }
  return {}
})

const crudRef = ref(null)
const pageRef = ref(null)
const expandAll = ref(true)
const expandedKeys = ref([])
const parentResourceOptions = ref([{ label: '顶级资源', value: 0, key: 0 }])
const pendingParentId = ref(null)
const pendingClientCode = ref(null)
const formIconTab = ref('font')
const tableIconSelectorRef = ref(null)
const tableIconEditRow = ref(null)
const tableIconValue = ref('')
const tableMaxHeight = ref('calc(100vh - 280px)')
const menuInitialLoading = ref(true)
let pageResizeObserver = null
let tableHeightFrame = null

// 组件挂载时加载上级资源选项和客户端列表
onMounted(() => {
  loadClientList()
  setupMenuPageLayout()
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

// 资源类型选项
const resourceTypeOptions = [
  { label: '目录', value: 1 },
  { label: '菜单', value: 2 },
  { label: '按钮', value: 3 },
  { label: 'API接口', value: 4 },
]

const routeOptions = getMenuRouteOptions()

// 客户端选项（动态从后端加载）
const clientCodeOptions = computed(() => {
  return clientList.value.map(client => ({
    label: client.clientName,
    value: client.clientCode,
  }))
})

function getClientDisplayName(clientCode) {
  return clientList.value.find(item => item.clientCode === clientCode)?.clientName || clientCode || '-'
}

function getSsoTargetClientOptions(formData) {
  const currentClient = formData?.clientCode || pendingClientCode.value || currentClientCode.value
  return clientList.value
    .filter(client => client.clientCode && client.clientCode !== currentClient)
    .map(client => ({
      label: client.clientName,
      value: client.clientCode,
    }))
}

function flattenResourceTree(list = []) {
  const result = []
  const walk = (items) => {
    items.forEach((item) => {
      result.push(item)
      if (item.children?.length)
        walk(item.children)
    })
  }
  walk(Array.isArray(list) ? list : [])
  return result
}

function getUsedRoutePathSet(formData) {
  const rows = flattenResourceTree(crudRef.value?.getTableData?.() || [])
  return new Set(
    rows
      .filter(row => row.id !== formData?.id)
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

function getAvailableRouteOptions(formData, keyword = '') {
  const usedPathSet = getUsedRoutePathSet(formData)
  const currentPath = formData?.path
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

function handleRoutePathChange(routePath, updateValue, formData) {
  const normalizedPath = normalizeRouteInput(routePath)
  if (!formData) {
    updateValue(normalizedPath)
    return
  }

  formData.path = normalizedPath
  autoFillComponentFromRoute(formData)
  updateValue(normalizedPath)
}

function autoFillComponentFromRoute(formData) {
  if (!formData || !formData.path || formData.component)
    return

  const selected = routeOptions.find(option => option.path === formData.path)
  if (selected?.component) {
    formData.component = selected.component
  }
}

function getAvailableComponentOptions(_formData, keyword = '') {
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

function handleComponentPathChange(componentPath, updateValue, formData) {
  const normalizedComponent = normalizeComponentValue(componentPath)
  updateValue(normalizedComponent)
  if (!formData)
    return

  formData.component = normalizedComponent
  const selected = routeOptions.find(option => option.component === normalizedComponent)
  if (selected?.path && !formData.path) {
    formData.path = selected.path
  }
}

function syncParentResourceOptions(list = crudRef.value?.getTableData?.() || []) {
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

// 加载客户端列表
async function loadClientList() {
  try {
    const res = await request.get('/system/client/list')
    if (res.code === 200) {
      clientList.value = res.data || []

      if (!currentClientCode.value) {
        const userClient = currentUserClientCode.value
        const matchedClient = clientList.value.find(client => client.clientCode === userClient)
        if (matchedClient) {
          currentClientCode.value = userClient
        }
      }
    }
  }
  catch (error) {
    console.error('加载客户端列表失败:', error)
  }
}

// 客户端 Tab 切换
function handleClientTabChange(clientCode) {
  menuInitialLoading.value = true
  currentClientCode.value = clientCode
  // Tab 切换时清空 pendingClientCode，避免残留值影响新增表单
  pendingClientCode.value = null
  scheduleTableHeightUpdate()
}

// 显示状态选项
const visibleOptions = [
  { label: '显示', value: 1 },
  { label: '隐藏', value: 0 },
]

// API请求方法选项
const apiMethodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
]

const openTargetOptions = [
  { label: '当前页', value: '_self' },
  { label: '新窗口', value: '_blank' },
]

function getOpenTargetDisplayName(openTarget) {
  return openTargetOptions.find(item => item.value === openTarget)?.label || '当前页'
}

function isImageIconValue(value) {
  if (!value || typeof value !== 'string')
    return false

  const iconValue = value.trim()
  if (!iconValue)
    return false
  if (iconValue.startsWith('local-image:'))
    return false

  if (iconValue.startsWith('http://')
    || iconValue.startsWith('https://')
    || iconValue.startsWith('data:')
    || iconValue.startsWith('blob:')
    || iconValue.startsWith('/api/file/')
    || iconValue.startsWith('forge-file://')
    || /^[a-f0-9]{32}$/i.test(iconValue)
    || /\.(?:png|jpe?g|webp|gif|svg|avif)(?:\?.*)?$/i.test(iconValue)) {
    return true
  }

  return false
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

function setupMenuPageLayout() {
  nextTick(() => {
    const pageEl = pageRef.value
    if (!pageEl)
      return

    pageEl.closest?.('.nexus-page')?.classList.add('menu-page-host-no-scroll')
    pageResizeObserver?.disconnect?.()
    pageResizeObserver = new ResizeObserver(() => scheduleTableHeightUpdate())
    pageResizeObserver.observe(pageEl)

    const searchEl = pageEl.querySelector('.ai-crud-search')
    const toolbarEl = pageEl.querySelector('.ai-table-toolbar')
    if (searchEl)
      pageResizeObserver.observe(searchEl)
    if (toolbarEl)
      pageResizeObserver.observe(toolbarEl)

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

  const tabsHeight = pageEl.querySelector('.client-tabs-container')?.offsetHeight || 0
  const searchHeight = pageEl.querySelector('.ai-crud-search')?.offsetHeight || 0
  const toolbarHeight = pageEl.querySelector('.ai-table-toolbar')?.offsetHeight || 0
  const availableHeight = pageEl.clientHeight - tabsHeight - searchHeight - toolbarHeight - 2
  tableMaxHeight.value = Math.max(260, availableHeight)
}

function handleListLoaded(payload = {}) {
  menuInitialLoading.value = false
  syncParentResourceOptions(payload.list || [])
  nextTick(() => {
    setupMenuPageLayout()
    scheduleTableHeightUpdate()
  })
}

// 搜索表单配置
const searchSchema = [
  {
    field: 'resourceName',
    label: '资源名称',
    type: 'input',
    props: {
      placeholder: '请输入资源名称',
    },
  },
  {
    field: 'resourceType',
    label: '资源类型',
    type: 'select',
    props: {
      placeholder: '请选择资源类型',
      options: resourceTypeOptions,
    },
  },
  {
    field: 'visible',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: visibleOptions,
    },
  },
]

// 资源类型样式配置
const typeStyleMap = {
  1: { text: '目录', icon: 'i-material-symbols:folder-outline', color: '#4C6EF5', bg: '#EDF2FF', fontWeight: '600' },
  2: { text: '菜单', icon: 'i-material-symbols:menu', color: '#40C057', bg: '#EBFBEE', fontWeight: '500' },
  3: { text: '按钮', icon: 'i-material-symbols:smart-button-outline', color: '#FD7E14', bg: '#FFF4E6', fontWeight: '400' },
  4: { text: 'API', icon: 'i-material-symbols:api', color: '#FA5252', bg: '#FFF5F5', fontWeight: '400' },
}

// 客户端样式配置
const clientStyleMap = {
  pc: { text: 'PC端', type: 'info' },
  app: { text: 'APP', type: 'success' },
  h5: { text: 'H5', type: 'warning' },
}

// 行样式 — 根据资源类型添加 class
function rowProps(row) {
  return {
    class: `resource-type-${row.resourceType}`,
  }
}

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'resourceName',
    label: '资源名称',
    width: 180,
    fixed: 'left',
    render: (row) => {
      const style = typeStyleMap[row.resourceType] || { fontWeight: '400' }
      return h('div', { class: 'flex items-center', style: { fontWeight: style.fontWeight } }, [
        row.icon
          ? h(IconRenderer, {
              icon: row.icon,
              fontSize: '16',
              customStyle: row.resourceType <= 2 ? { color: 'var(--primary-color, #4C6EF5)' } : { color: '#999' },
            })
          : null,
        h('span', { style: { marginLeft: row.icon ? '8px' : '0' } }, row.resourceName),
      ])
    },
  },
  {
    prop: 'clientCode',
    label: '客户端',
    width: 100,
    render: (row) => {
      const config = clientStyleMap[row.clientCode] || { text: row.clientCode, type: 'default' }
      return h(NTag, { type: config.type, size: 'small', bordered: false }, { default: () => config.text })
    },
  },
  {
    prop: 'resourceType',
    label: '类型',
    width: 90,
    render: (row) => {
      const config = typeStyleMap[row.resourceType]
      if (!config)
        return h('span', '未知')
      return h('span', {
        class: 'resource-type-badge',
        style: {
          color: config.color,
          backgroundColor: config.bg,
          borderColor: config.color,
        },
      }, [
        h('i', { class: config.icon, style: { fontSize: '12px', marginRight: '4px' } }),
        config.text,
      ])
    },
  },
  {
    prop: 'icon',
    label: '图标',
    width: 120,
    _slot: 'icon',
  },
  {
    prop: 'path',
    label: '路由地址',
    width: 180,
  },
  {
    prop: 'ssoEnabled',
    label: 'SSO',
    width: 240,
    render: (row) => {
      if (row.resourceType !== 2) {
        return h('span', { class: 'text-gray-400' }, '-')
      }

      if (Number(row.ssoEnabled) !== 1) {
        return h(NTag, { size: 'small', bordered: false }, { default: () => '关闭' })
      }

      return h('div', { class: 'flex items-center', style: { gap: '8px' } }, [
        h(NTag, { type: 'success', size: 'small', bordered: false }, { default: () => '开启' }),
        h('span', { class: 'text-xs text-gray-500' }, getClientDisplayName(row.ssoTargetClient)),
        h('span', { class: 'text-xs text-gray-400' }, `/${getOpenTargetDisplayName(row.openTarget || '_self')}`),
      ])
    },
  },
  {
    prop: 'sort',
    label: '排序',
    width: 90,
    _slot: 'sort',
  },
  {
    prop: 'visible',
    label: '状态',
    width: 80,
    render: (row) => {
      const isVisible = row.visible === 1
      return h('span', {
        class: ['visibility-toggle', isVisible ? 'is-visible' : 'is-hidden'],
        onClick: (e) => {
          e.stopPropagation()
          handleInlineUpdate(row, 'visible', isVisible ? 0 : 1)
        },
      }, [
        h('i', {
          class: isVisible ? 'i-material-symbols:visibility' : 'i-material-symbols:visibility-off',
          style: { fontSize: '14px', marginRight: '2px' },
        }),
        isVisible ? '显示' : '隐藏',
      ])
    },
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    actions: [
      { label: '新增子项', key: 'add', type: 'primary', onClick: handleAdd },
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置 - 优化布局和验证规则
const editSchema = computed(() => [
  // 基础信息分组
  {
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
        validator: (rule, value) => {
          if (value === null || value === undefined || value === '') {
            return new Error('请选择资源类型')
          }
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
        validator: (rule, value) => {
          if (!value) {
            return new Error('请选择客户端')
          }
          return true
        },
      },
    ],
    props: { options: clientCodeOptions.value },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'input-number',
    span: 1,
    defaultValue: 0,
    props: { placeholder: '排序值', min: 0 },
  },

  // 目录和菜单配置
  {
    type: 'divider',
    label: '目录/菜单配置',
    props: { titlePlacement: 'left' },
    span: 2,
    vIf: formData => formData.resourceType === 1 || formData.resourceType === 2,
  },
  {
    field: 'icon',
    label: '图标',
    type: 'slot',
    span: 2,
    slotName: 'icon',
    vIf: formData => formData.resourceType === 1 || formData.resourceType === 2,
  },
  {
    field: 'path',
    label: '路由地址',
    type: 'slot',
    slotName: 'path',
    span: 2,
    vIf: formData => formData.resourceType === 1 || formData.resourceType === 2,
  },
  {
    field: 'component',
    label: '组件路径',
    type: 'slot',
    span: 2,
    slotName: 'component',
    vIf: formData => formData.resourceType === 2,
  },
  {
    field: 'redirect',
    label: '重定向地址',
    type: 'input',
    span: 1,
    props: { placeholder: '重定向地址' },
    vIf: formData => formData.resourceType === 1 || formData.resourceType === 2,
  },
  {
    field: 'isExternal',
    label: '是否外链',
    type: 'radio',
    span: 1,
    defaultValue: 0,
    props: { options: [{ label: '否', value: 0 }, { label: '是', value: 1 }] },
    vIf: formData => formData.resourceType === 2,
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
    vIf: formData => formData.resourceType === 2,
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
    options: ({ formData }) => getSsoTargetClientOptions(formData),
    vIf: formData => formData.resourceType === 2 && formData.ssoEnabled === 1,
  },
  {
    field: 'openTarget',
    label: '打开方式',
    type: 'radio',
    span: 1,
    defaultValue: '_self',
    props: { options: openTargetOptions },
    vIf: formData => formData.resourceType === 2 && formData.ssoEnabled === 1,
  },
  {
    field: 'keepAlive',
    label: '是否缓存',
    type: 'radio',
    span: 1,
    defaultValue: 0,
    props: { options: [{ label: '否', value: 0 }, { label: '是', value: 1 }] },
    vIf: formData => formData.resourceType === 2,
  },
  {
    field: 'alwaysShow',
    label: '总是显示',
    type: 'radio',
    span: 1,
    defaultValue: 0,
    props: { options: [{ label: '否', value: 0 }, { label: '是', value: 1 }] },
    vIf: formData => formData.resourceType === 1 || formData.resourceType === 2,
  },

  // 按钮和API配置
  {
    type: 'divider',
    label: '按钮/API配置',
    props: { titlePlacement: 'left' },
    span: 2,
    vIf: formData => formData.resourceType === 3 || formData.resourceType === 4,
  },
  {
    field: 'perms',
    label: '权限标识',
    type: 'input',
    span: 1,
    props: { placeholder: 'sys:user:add' },
    vIf: formData => formData.resourceType === 3 || formData.resourceType === 4,
  },
  {
    field: 'apiMethod',
    label: '请求方法',
    type: 'select',
    span: 1,
    defaultValue: 'GET',
    props: { placeholder: '请求方法', options: apiMethodOptions },
    vIf: formData => formData.resourceType === 4,
  },
  {
    field: 'apiUrl',
    label: '接口地址',
    type: 'input',
    span: 1,
    props: { placeholder: '/system/user/list' },
    vIf: formData => formData.resourceType === 4,
  },

  // 状态配置
  {
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
    vIf: formData => formData.resourceType === 1 || formData.resourceType === 2,
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: { placeholder: '请输入备注', rows: 3 },
  },
])

// 获取所有节点的 key（用于展开/收起）
function getAllKeys(list, keys = []) {
  list.forEach((item) => {
    keys.push(item.id)
    if (item.children && item.children.length > 0) {
      getAllKeys(item.children, keys)
    }
  })
  return keys
}

// 列表数据渲染前处理
function beforeRenderList(list) {
  if (expandAll.value) {
    expandedKeys.value = getAllKeys(list)
  }
  return list
}

// 表单渲染前处理
function beforeRenderForm(data) {
  formIconTab.value = isImageIconValue(data?.icon) ? 'image' : 'font'

  if (!data) {
    // 新增时设置默认值
    const parentId = pendingParentId.value !== null ? pendingParentId.value : 0
    // 优先使用 pendingClientCode（新增子项时带入父级），其次使用当前 Tab 的 clientCode
    const clientCode = pendingClientCode.value || currentClientCode.value || 'pc'
    // 清空临时值
    pendingParentId.value = null
    pendingClientCode.value = null
    return { parentId, clientCode, ssoEnabled: 0, ssoTargetClient: '', openTarget: '_self' }
  }
  // 编辑时清空临时值
  pendingParentId.value = null
  pendingClientCode.value = null
  return {
    ...data,
    ssoEnabled: data.ssoEnabled ?? 0,
    ssoTargetClient: data.ssoTargetClient || '',
    openTarget: data.openTarget || '_self',
  }
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

// 表单提交前处理
function beforeSubmit(formData) {
  const resourceType = formData.resourceType !== undefined ? Number(formData.resourceType) : formData.resourceType
  const ssoEnabled = resourceType === 2 ? Number(formData.ssoEnabled || 0) : 0
  const ssoTargetClient = ssoEnabled === 1 ? (formData.ssoTargetClient || '').trim() : ''
  const openTarget = resourceType === 2 && ssoEnabled === 1 ? (formData.openTarget || '_self') : '_self'

  if (resourceType === 2 && ssoEnabled === 1) {
    if (!ssoTargetClient) {
      window.$message.error('请选择目标子系统')
      return false
    }
    if (ssoTargetClient === formData.clientCode) {
      window.$message.error('目标子系统不能与当前客户端相同')
      return false
    }
  }

  return {
    ...formData,
    path: normalizeRouteInput(formData.path),
    component: normalizeComponentValue(formData.component),
    resourceType,
    ssoEnabled,
    ssoTargetClient,
    openTarget,
  }
}

// 提交成功后
async function handleSubmitSuccess() {
  menuInitialLoading.value = true
  await refreshSystemMenu()
  scheduleTableHeightUpdate()
}

// 刷新系统菜单
async function refreshSystemMenu() {
  try {
    const res = await api.getMenu()
    if (res.code === 200 && res.data) {
      permissionStore.setMenuData(res.data)
    }
  }
  catch (error) {
    console.error('刷新系统菜单失败:', error)
  }
}

// 内联更新
async function handleInlineUpdate(row, field, value) {
  try {
    menuInitialLoading.value = true
    row[field] = value
    const res = await request.post('/system/resource/edit', {
      id: row.id,
      [field]: value,
    })
    if (res.code === 200) {
      window.$message.success('更新成功')
      await refreshSystemMenu()
    }
    else {
      window.$message.error(res.msg || '更新失败')
    }
  }
  catch (error) {
    console.error('内联更新失败:', error)
    window.$message.error('更新失败')
  }
  finally {
    menuInitialLoading.value = false
    nextTick(scheduleTableHeightUpdate)
  }
}

// 展开/折叠所有
function toggleExpandAll() {
  menuInitialLoading.value = true
  expandAll.value = !expandAll.value
  if (expandAll.value) {
    const tableData = crudRef.value?.getTableData() || []
    expandedKeys.value = getAllKeys(tableData)
  }
  else {
    expandedKeys.value = []
  }
  nextTick(() => {
    menuInitialLoading.value = false
    scheduleTableHeightUpdate()
  })
}

// 处理表格展开状态更新
function handleExpandedKeysUpdate(keys) {
  expandedKeys.value = keys
  const tableData = crudRef.value?.getTableData() || []
  const allKeys = getAllKeys(tableData)
  expandAll.value = keys.length === allKeys.length
}

// 工具栏新增按钮点击（AiCrudPage 内部按钮触发）
function handleToolbarAdd() {
  // 设置当前 Tab 的 clientCode，beforeRenderForm 会使用
  pendingClientCode.value = currentClientCode.value || null
  scheduleTableHeightUpdate()
}

// 新增子资源（操作列按钮触发）
async function handleAdd(row) {
  syncParentResourceOptions()
  if (row) {
    // 新增子项：带入父级的 clientCode
    pendingParentId.value = row.id
    pendingClientCode.value = row.clientCode
  }
  else {
    // 顶级新增：带入当前 Tab 的 clientCode
    pendingParentId.value = null
    pendingClientCode.value = currentClientCode.value || null
  }
  crudRef.value?.showAdd()
  scheduleTableHeightUpdate()
}

// 编辑
async function handleEdit(row) {
  syncParentResourceOptions()
  crudRef.value?.showEdit(row)
  scheduleTableHeightUpdate()
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: '确定要删除该资源吗？删除后将无法恢复！',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        menuInitialLoading.value = true
        const res = await request.post('/system/resource/remove', null, { params: { id: row.id } })
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
          await refreshSystemMenu()
        }
      }
      catch (error) {
        console.error('删除资源失败:', error)
        window.$message.error('删除失败')
      }
      finally {
        menuInitialLoading.value = false
        nextTick(scheduleTableHeightUpdate)
      }
    },
  })
}
</script>

<style scoped>
.system-menu-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

:global(.nexus-page.menu-page-host-no-scroll) {
  overflow: hidden !important;
}

:global(.nexus-page.menu-page-host-no-scroll) > .system-menu-page {
  min-height: 0;
}

.client-tabs-container {
  flex-shrink: 0;
}

.system-menu-page :deep(.ai-crud-page) {
  min-height: 0;
}

.system-menu-page :deep(.ai-crud-main) {
  overflow: hidden !important;
}

.system-menu-page :deep(.ai-crud-table) {
  overflow: hidden;
}

.system-menu-page :deep(.ai-table-wrapper) {
  min-height: 0;
}

.system-menu-page :deep(.n-data-table) {
  min-height: 0;
}

.menu-loading-skeleton {
  position: absolute;
  inset: 38px 0 0;
  z-index: 6;
  padding: 10px 16px 16px;
  background: var(--bg-primary);
  pointer-events: none;
}

.skeleton-toolbar {
  display: flex;
  gap: 8px;
  height: 42px;
  align-items: center;
  margin-bottom: 8px;
}

.skeleton-toolbar span,
.skeleton-cell {
  display: inline-block;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--bg-secondary) 25%, var(--bg-tertiary, #f1f3f5) 37%, var(--bg-secondary) 63%);
  background-size: 400% 100%;
  animation: menu-skeleton-shimmer 1.2s ease-in-out infinite;
}

.skeleton-toolbar span {
  width: 82px;
  height: 28px;
}

.skeleton-table {
  border: 1px solid var(--border-light);
  border-radius: 6px;
  overflow: hidden;
}

.skeleton-row {
  display: grid;
  grid-template-columns: 1.6fr 0.55fr 1fr 0.55fr 1.4fr;
  gap: 18px;
  align-items: center;
  height: 42px;
  padding: 0 16px;
  border-bottom: 1px solid var(--border-light);
}

.skeleton-row:last-child {
  border-bottom: none;
}

.skeleton-cell {
  height: 14px;
}

.skeleton-cell.w-lg {
  width: 78%;
}

.skeleton-cell.w-md {
  width: 62%;
}

.skeleton-cell.w-sm {
  width: 44%;
}

@keyframes menu-skeleton-shimmer {
  0% {
    background-position: 100% 0;
  }

  100% {
    background-position: 0 0;
  }
}

/* 行内编辑 */
.inline-edit-cell {
  display: flex;
  align-items: center;
}

.inline-edit-preview {
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: background 0.2s;
  min-height: 24px;
  display: flex;
  align-items: center;
}

.inline-edit-preview:hover {
  background: var(--n-color-hover, #f5f5f5);
}

.sort-value {
  font-size: 13px;
  min-width: 24px;
  text-align: center;
}

/* 资源类型标签 */
.resource-type-badge {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  line-height: 1;
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid;
  white-space: nowrap;
}

/* 可见状态切换 */
.visibility-toggle {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s;
  user-select: none;
}

.visibility-toggle.is-visible {
  color: #40c057;
}

.visibility-toggle.is-visible:hover {
  background: #ebfbee;
}

.visibility-toggle.is-hidden {
  color: #868e96;
}

.visibility-toggle.is-hidden:hover {
  background: #f8f9fa;
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

/* 行背景区分类型 */
:deep(.resource-type-1) {
  background-color: #f8f9ff !important;
}

:deep(.resource-type-1:hover) {
  background-color: #edf2ff !important;
}

:deep(.resource-type-2) {
  background-color: #f8fff8 !important;
}

:deep(.resource-type-2:hover) {
  background-color: #ebfbee !important;
}

.icon-selector-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.icon-upload-container {
  padding: 8px 0;
}

.client-tabs-container {
  background: #fff;
  padding: 8px 16px 0;
  border-radius: 4px;
  margin-bottom: 8px;
}

.tab-label {
  font-size: 14px;
}

/* 弹窗表单样式优化 */
.system-menu-page :deep(.n-form) {
  --n-feedback-padding: 2px 0 0;
}

.system-menu-page :deep(.n-form-item-blank) {
  min-height: auto;
}

.system-menu-page :deep(.n-divider) {
  margin: 0 0 8px 0;
}

.system-menu-page :deep(.n-divider:not(:first-child)) {
  margin-top: 12px;
}

.system-menu-page :deep(.n-radio-group) {
  flex-wrap: wrap;
  gap: 4px 16px;
}
</style>
