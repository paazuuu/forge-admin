<template>
  <div class="dataset-studio">
    <section class="studio-hero">
      <div class="hero-main">
        <p class="hero-kicker">
          Data Asset Workspace
        </p>
        <h1 class="hero-title">
          数据集资产管理台
        </h1>
        <p class="hero-description">
          用分类树组织业务域，用发布流转控制可用性。已发布数据集只读，先下架再修改，保证下游报表与分析消费稳定。
        </p>
      </div>

      <div class="hero-stats">
        <div v-for="card in statCards" :key="card.key" class="hero-stat-card">
          <div class="hero-stat-label">
            {{ card.label }}
          </div>
          <div class="hero-stat-value">
            {{ card.value }}
          </div>
          <div class="hero-stat-note">
            {{ card.note }}
          </div>
        </div>
      </div>
    </section>

    <div class="dataset-workspace">
      <aside class="workspace-sidebar">
        <div class="sidebar-head">
          <div>
            <p class="panel-kicker">
              Taxonomy
            </p>
            <h3>数据集分类</h3>
          </div>
          <n-button type="primary" secondary @click="goToCategoryManage">
            分类管理
          </n-button>
        </div>

        <div class="sidebar-shortcuts">
          <button
            class="scope-chip"
            :class="{ active: activeCategoryScope === 'all' }"
            type="button"
            @click="selectAllCategories"
          >
            全部数据集
          </button>
          <button
            class="scope-chip"
            :class="{ active: activeCategoryScope === 'uncategorized' }"
            type="button"
            @click="selectUncategorized"
          >
            未分类
          </button>
        </div>

        <n-input
          v-model:value="categoryKeyword"
          placeholder="搜索分类名称或编码"
          clearable
          class="category-search"
        >
          <template #prefix>
            <i class="i-material-symbols:search-rounded" />
          </template>
        </n-input>

        <div class="category-tree-shell">
          <n-empty v-if="categoryTreeNodes.length === 0" description="暂无分类，请前往分类管理页配置" size="small" />
          <n-tree
            v-else
            block-line
            :data="categoryTreeNodes"
            :default-expand-all="true"
            :selected-keys="selectedTreeKeys"
            @update:selected-keys="handleCategoryTreeSelect"
          />
        </div>

        <div v-if="selectedCategoryNode" class="category-detail-card">
          <div class="category-detail-top">
            <div>
              <div class="category-detail-name">
                {{ selectedCategoryNode.categoryName }}
              </div>
              <div class="category-detail-code">
                {{ selectedCategoryNode.categoryCode }}
              </div>
            </div>
            <NTag size="small" :type="selectedCategoryNode.status === 1 ? 'success' : 'default'" :bordered="false">
              {{ selectedCategoryNode.status === 1 ? '启用' : '停用' }}
            </NTag>
          </div>
          <p class="category-detail-desc">
            {{ selectedCategoryNode.description || '当前分类暂无补充说明。' }}
          </p>
        </div>
      </aside>

      <section class="workspace-main">
        <div class="main-toolbar">
          <div class="toolbar-title-row">
            <div>
              <p class="panel-kicker">
                Asset Inventory
              </p>
              <h3>数据集列表</h3>
            </div>
            <div class="toolbar-title-meta">
              <span class="toolbar-scope">{{ activeCategoryScopeLabel }}</span>
              <n-button @click="handleResetFilters">
                重置筛选
              </n-button>
              <n-button type="primary" @click="handleAddDataset">
                新增数据集
              </n-button>
            </div>
          </div>

          <div class="toolbar-filters">
            <n-input
              v-model:value="queryForm.datasetName"
              class="toolbar-filter toolbar-filter--keyword"
              clearable
              placeholder="搜索数据集名称"
              @keydown.enter="applySearch"
            >
              <template #prefix>
                <i class="i-material-symbols:search-rounded" />
              </template>
            </n-input>
            <n-select
              v-model:value="queryForm.connectionId"
              class="toolbar-filter"
              clearable
              filterable
              placeholder="数据连接"
              :options="connectionOptions"
            />
            <n-select
              v-model:value="queryForm.datasetType"
              class="toolbar-filter"
              clearable
              placeholder="数据集类型"
              :options="datasetTypeOptions"
            />
            <n-select
              v-model:value="queryForm.publishStatus"
              class="toolbar-filter"
              clearable
              placeholder="发布状态"
              :options="publishStatusOptions"
            />
            <n-button type="primary" @click="applySearch">
              搜索
            </n-button>
          </div>
        </div>

        <AiCrudPage
          ref="crudRef"
          class="dataset-crud"
          :api-config="{
            list: 'get@/data/dataset/page',
            detail: 'get@/data/dataset/:id',
            add: 'post@/data/dataset',
            update: 'put@/data/dataset',
            delete: 'delete@/data/dataset/:id',
          }"
          :show-search="false"
          :hide-toolbar="true"
          :columns="tableColumns"
          :edit-schema="editSchema"
          :before-render-form="beforeRenderForm"
          :before-render-detail="beforeRenderDetail"
          :before-submit="beforeSubmit"
          :hide-modal-footer="isFormReadOnly"
          row-key="id"
          :load-detail-on-edit="true"
          :striped="false"
          :bordered="false"
          :scroll-x="1580"
          max-height="calc(100vh - 420px)"
          :edit-grid-cols="12"
          edit-label-placement="top"
          edit-form-class="data-dataset-edit-form"
          modal-type="modal"
          modal-width="min(1320px, calc(100vw - 32px))"
          add-button-text="新增数据集"
          @load-list-success="handleDatasetLoadSuccess"
          @modal-close="handleDatasetModalClose"
        >
          <template #form-publishReadonlyAlert>
            <n-alert type="warning" :show-icon="false">
              当前数据集已发布，内容处于只读状态。如需修改，请先在列表中执行“下架”。
            </n-alert>
          </template>

          <template #form-datasetOverview="{ formData }">
            <div class="dataset-guide-grid">
              <div class="dataset-guide-card">
                <div class="guide-label">
                  资产识别
                </div>
                <div class="guide-value">
                  {{ formData.datasetName || '待命名数据集' }}
                </div>
                <div class="guide-note">
                  {{ formData.datasetCode || '建议使用业务域_主题_粒度 的编码方式' }}
                </div>
              </div>
              <div class="dataset-guide-card">
                <div class="guide-label">
                  建模模式
                </div>
                <div class="guide-value">
                  {{ getDatasetTypeLabel(formData.datasetType || 'TABLE') }}
                </div>
                <div class="guide-note">
                  {{ getDatasetSourceSummary(formData) }}
                </div>
              </div>
              <div class="dataset-guide-card">
                <div class="guide-label">
                  业务分类
                </div>
                <div class="guide-value">
                  {{ getCategoryName(formData.categoryId) || '未分类' }}
                </div>
                <div class="guide-note">
                  {{ getCategoryGuideNote(formData.categoryId) }}
                </div>
              </div>
              <div class="dataset-guide-card">
                <div class="guide-label">
                  发布状态
                </div>
                <div class="guide-value">
                  {{ getPublishStatusLabel(formData.publishStatus ?? 0) }}
                </div>
                <div class="guide-note">
                  {{ getPublishStatusGuide(formData.publishStatus) }}
                </div>
              </div>
            </div>
          </template>

          <template #form-sqlText="{ value, updateValue }">
            <SqlEditor
              :value="value"
              :readonly="isFormReadOnly"
              placeholder="SELECT id, name FROM table_name WHERE status = 1"
              @update:value="updateValue"
            />
          </template>

          <template #form-sqlPreviewAction="{ formData }">
            <div class="sql-preview-action">
              <n-button
                type="primary"
                secondary
                :disabled="isFormReadOnly"
                :loading="sqlPreviewLoading"
                @click="handlePreviewSql(formData)"
              >
                预览SQL
              </n-button>
              <n-text depth="3">
                仅执行并展示前 10 条数据，用于校验 SQL 语句
              </n-text>
            </div>
          </template>

          <template #form-sourceGuide="{ formData }">
            <div class="dataset-inline-guide">
              <div class="inline-guide-main">
                <div class="inline-guide-title">
                  来源选择建议
                </div>
                <div class="inline-guide-desc">
                  {{ getDatasetSourceGuide(formData) }}
                </div>
              </div>
              <div class="inline-guide-pills">
                <span class="guide-pill">
                  {{ formData.connectionId ? getConnectionName(formData.connectionId) : '待选数据连接' }}
                </span>
                <span class="guide-pill guide-pill--muted">
                  {{ formData.datasetType === 'SQL' ? 'SQL 查询模式' : (formData.tableName || '待选数据表') }}
                </span>
              </div>
            </div>
          </template>

          <template #form-paramGuide="{ formData }">
            <div class="dataset-inline-guide dataset-inline-guide--soft">
              <div class="inline-guide-main">
                <div class="inline-guide-title">
                  条件定义建议
                </div>
                <div class="inline-guide-desc">
                  {{ getDatasetParamGuide(formData) }}
                </div>
              </div>
              <div class="inline-guide-pills">
                <span class="guide-pill">
                  {{ formData.datasetType === 'SQL' ? `SQL 参数 ${getSqlParamCount(formData.sqlText)} 个` : (formData.tableName ? '已绑定数据表字段' : '待绑定数据表字段') }}
                </span>
                <span class="guide-pill guide-pill--muted">
                  {{ formData.datasetType === 'SQL' ? '参数名需与 SQL 中的 :param 保持一致' : '每个条件都需要映射数据表字段' }}
                </span>
              </div>
            </div>
          </template>

          <template #form-paramSchemaJson="{ value, updateValue, formData }">
            <DatasetParamSchemaEditor
              :model-value="value || []"
              :readonly="isFormReadOnly"
              :dataset-type="formData.datasetType"
              :connection-id="formData.connectionId"
              :table-name="formData.tableName"
              :sql-text="formData.sqlText"
              @update:model-value="updateValue"
            />
          </template>

          <template #form-settingGuide="{ formData }">
            <div class="dataset-runtime-strip">
              <div class="runtime-chip">
                <span>最大行数</span>
                <strong>{{ formData.maxRows || 1000 }}</strong>
              </div>
              <div class="runtime-chip">
                <span>超时</span>
                <strong>{{ formData.timeoutSeconds || 15 }}s</strong>
              </div>
              <div class="runtime-note">
                {{ formData.datasetType === 'SQL' ? '复杂 SQL 建议收紧返回行数和超时时间，避免拖慢报表查询。' : '单表数据集建议保持轻量，先同步字段再逐步补充筛选条件。' }}
              </div>
            </div>
          </template>
        </AiCrudPage>
      </section>
    </div>

    <n-modal
      v-model:show="fieldModalVisible"
      preset="card"
      :title="fieldModalTitle"
      style="width: 960px"
      :segmented="{ content: 'soft' }"
    >
      <n-data-table
        :columns="fieldColumns"
        :data="fieldRows"
        :loading="fieldLoading"
        :pagination="{ pageSize: 10 }"
        size="small"
      />
    </n-modal>

    <n-modal
      v-model:show="sqlPreviewVisible"
      preset="card"
      title="SQL预览结果"
      style="width: 1000px"
      :segmented="{ content: 'soft' }"
    >
      <n-data-table
        :columns="sqlPreviewColumns"
        :data="sqlPreviewRows"
        :loading="sqlPreviewLoading"
        :pagination="{ pageSize: 10 }"
        :scroll-x="sqlPreviewScrollX"
        size="small"
      />
    </n-modal>
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDataConnectionList, getDataConnectionTables } from '@/api/data/connection'
import {
  deleteDataDataset,
  getDataDatasetCategoryTree,
  offlineDataDataset,
  publishDataDataset,
  syncDataDatasetFields,
} from '@/api/data/dataset'
import { AiCrudPage } from '@/components/ai-form'
import DatasetParamSchemaEditor from '@/components/data/DatasetParamSchemaEditor.vue'
import SqlEditor from '@/components/SqlEditor.vue'
import { request } from '@/utils'

defineOptions({ name: 'DataDataset' })

const router = useRouter()
const crudRef = ref(null)
const connectionOptions = ref([])
const categoryTree = ref([])
const categoryKeyword = ref('')
const tableOptions = ref([])
const tableLoading = ref(false)
const loadedTableConnectionId = ref(null)
const loadingTableConnectionId = ref(null)
const fieldModalVisible = ref(false)
const fieldLoading = ref(false)
const fieldModalTitle = ref('字段列表')
const fieldRows = ref([])
const sqlPreviewVisible = ref(false)
const sqlPreviewLoading = ref(false)
const sqlPreviewColumns = ref([])
const sqlPreviewRows = ref([])
const sqlPreviewScrollX = ref(0)
const activeCategoryScope = ref('all')
const selectedCategoryId = ref(null)
const currentFormMode = ref('edit')
const currentEditingDataset = ref(null)

const queryForm = reactive({
  datasetName: '',
  connectionId: null,
  datasetType: null,
  publishStatus: null,
})

const datasetStats = reactive({
  total: 0,
  published: 0,
  editable: 0,
})

const datasetTypeOptions = [
  { label: '单表数据集', value: 'TABLE' },
  { label: 'SQL数据集', value: 'SQL' },
]

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

const publishStatusOptions = [
  { label: '未发布', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已下架', value: 2 },
]

const supportedParamOperators = ['=', '!=', '>', '>=', '<', '<=', 'LIKE']

const isFormReadOnly = computed(() => currentFormMode.value === 'view' || currentEditingDataset.value?.publishStatus === 1)
const selectedCategoryNode = computed(() => findCategoryById(categoryTree.value, selectedCategoryId.value))
const selectedTreeKeys = computed(() => activeCategoryScope.value === 'category' && selectedCategoryId.value ? [selectedCategoryId.value] : [])

const activeCategoryScopeLabel = computed(() => {
  if (activeCategoryScope.value === 'uncategorized') {
    return '当前范围：未分类数据集'
  }
  if (activeCategoryScope.value === 'category' && selectedCategoryNode.value) {
    return `当前范围：${selectedCategoryNode.value.categoryName}`
  }
  return '当前范围：全部数据集'
})

const statCards = computed(() => [
  {
    key: 'total',
    label: '筛选结果',
    value: datasetStats.total,
    note: '匹配当前分类与搜索条件的数据集总数',
  },
  {
    key: 'published',
    label: '当前页已发布',
    value: datasetStats.published,
    note: '可被报表和运行时直接消费的数据集',
  },
  {
    key: 'editable',
    label: '当前页可编辑',
    value: datasetStats.editable,
    note: '未发布或已下架，允许继续调整结构',
  },
  {
    key: 'category',
    label: '分类总数',
    value: countTreeNodes(categoryTree.value),
    note: '用于业务划分的数据集分类节点数量',
  },
])

const categoryTreeNodes = computed(() => buildCategoryTreeNodes(filterCategoryTree(categoryTree.value, categoryKeyword.value)))
const categoryTreeSelectOptions = computed(() => buildCategorySelectOptions(categoryTree.value))

const tableColumns = computed(() => [
  {
    prop: 'datasetName',
    label: '数据集资产',
    width: 310,
    render: row => h('div', { class: 'asset-name-card' }, [
      h('div', { class: 'asset-name-row' }, [
        h('div', { class: 'asset-name' }, row.datasetName),
        h(NTag, {
          size: 'small',
          bordered: false,
          type: row.datasetType === 'TABLE' ? 'info' : 'warning',
        }, { default: () => row.datasetType === 'TABLE' ? '单表' : 'SQL' }),
      ]),
      h('div', { class: 'asset-code' }, row.datasetCode),
      h('div', { class: 'asset-desc' }, row.description || '暂无描述'),
    ]),
  },
  {
    prop: 'categoryName',
    label: '业务分类',
    width: 180,
    render: row => h('div', { class: 'asset-category' }, [
      h('div', { class: 'asset-category-name' }, row.categoryName || '未分类'),
      h('div', { class: 'asset-category-code' }, row.categoryCode || '暂未归档'),
    ]),
  },
  {
    prop: 'connectionId',
    label: '数据来源',
    width: 260,
    render: row => h('div', { class: 'asset-source' }, [
      h('div', { class: 'asset-source-name' }, row.connectionName || getConnectionName(row.connectionId)),
      h('div', { class: 'asset-source-detail' }, row.datasetType === 'TABLE'
        ? `表：${row.tableName || '-'}`
        : 'SQL 查询模式'),
    ]),
  },
  {
    prop: 'publishStatus',
    label: '发布状态',
    width: 110,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: getPublishStatusTagType(row.publishStatus),
    }, { default: () => getPublishStatusLabel(row.publishStatus) }),
  },
  {
    prop: 'status',
    label: '可用状态',
    width: 110,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: row.status === 1 ? 'success' : 'default',
    }, { default: () => row.status === 1 ? '启用' : '禁用' }),
  },
  { prop: 'maxRows', label: '最大行数', width: 100 },
  { prop: 'updateTime', label: '更新时间', width: 170 },
  {
    prop: 'action',
    label: '操作',
    width: 320,
    fixed: 'right',
    maxActionButtons: 3,
    actions: [
      { label: '编辑', key: 'edit', type: 'primary', visible: row => row.publishStatus !== 1, onClick: handleEdit },
      { label: '查看', key: 'view', type: 'primary', visible: row => row.publishStatus === 1, onClick: handleViewDataset },
      { label: '发布', key: 'publish', type: 'success', visible: row => row.publishStatus !== 1, onClick: handlePublishDataset },
      { label: '下架', key: 'offline', type: 'warning', visible: row => row.publishStatus === 1, onClick: handleOfflineDataset },
      { label: '查看字段', key: 'fields', type: 'info', onClick: handleViewFields },
      { label: '同步字段', key: 'sync', type: 'info', visible: row => row.publishStatus !== 1, onClick: handleSyncFields },
      { label: '删除', key: 'delete', type: 'error', visible: row => row.publishStatus !== 1, onClick: handleDelete },
    ],
  },
])

const fieldColumns = [
  { title: '字段名', key: 'fieldName', width: 160 },
  { title: '显示名', key: 'fieldLabel', width: 160 },
  { title: '来源列', key: 'sourceColumn', width: 140 },
  { title: '数据库类型', key: 'dbType', width: 120 },
  { title: '标准类型', key: 'dataType', width: 100 },
  {
    title: '字段角色',
    key: 'fieldRole',
    width: 100,
    render: row => row.fieldRole === 'MEASURE' ? '指标' : '维度',
  },
  {
    title: '可筛选',
    key: 'queryEnabled',
    width: 80,
    render: row => row.queryEnabled === 1 ? '是' : '否',
  },
  {
    title: '可展示',
    key: 'displayEnabled',
    width: 80,
    render: row => row.displayEnabled === 1 ? '是' : '否',
  },
]

const editSchema = computed(() => [
  {
    field: 'publishReadonlyAlert',
    label: '',
    type: 'slot',
    slotName: 'publishReadonlyAlert',
    span: 12,
    showFeedback: false,
    vIf: () => isFormReadOnly.value,
  },
  {
    field: 'datasetOverview',
    label: '',
    type: 'slot',
    slotName: 'datasetOverview',
    span: 12,
    showFeedback: false,
  },
  {
    field: '__sectionBasic',
    label: '基础信息',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'dataset-form-divider' },
  },
  {
    field: 'datasetCode',
    label: '数据集编码',
    type: 'input',
    span: 3,
    disabled: () => isFormReadOnly.value,
    rules: [{ required: true, message: '请输入数据集编码', trigger: 'blur' }],
    props: { placeholder: '请输入数据集编码' },
  },
  {
    field: 'datasetName',
    label: '数据集名称',
    type: 'input',
    span: 4,
    disabled: () => isFormReadOnly.value,
    rules: [{ required: true, message: '请输入数据集名称', trigger: 'blur' }],
    props: { placeholder: '请输入数据集名称' },
  },
  {
    field: 'categoryId',
    label: '业务分类',
    type: 'treeSelect',
    span: 5,
    disabled: () => isFormReadOnly.value,
    props: {
      options: categoryTreeSelectOptions.value,
      clearable: true,
      defaultExpandAll: true,
      placeholder: '请选择业务分类',
    },
  },
  {
    field: 'status',
    label: '可用状态',
    type: 'radio',
    span: 4,
    disabled: () => isFormReadOnly.value,
    defaultValue: 1,
    props: { options: statusOptions },
  },
  {
    field: '__sectionSource',
    label: '数据来源',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'dataset-form-divider' },
  },
  {
    field: 'connectionId',
    label: '数据连接',
    type: 'select',
    span: 4,
    disabled: () => isFormReadOnly.value,
    props: { placeholder: '请选择数据连接', options: connectionOptions.value, filterable: true },
    onChange: ({ value, formData }) => handleConnectionChange(value, formData),
  },
  {
    field: 'datasetType',
    label: '数据集类型',
    type: 'radio',
    span: 3,
    disabled: ({ context }) => isFormReadOnly.value || context?.isEdit,
    defaultValue: 'TABLE',
    rules: [{ required: true }],
    props: { options: datasetTypeOptions },
    onChange: ({ value, formData }) => handleDatasetTypeChange(value, formData),
  },
  {
    field: 'tableName',
    label: '数据表',
    type: 'select',
    span: 5,
    disabled: () => isFormReadOnly.value,
    props: {
      placeholder: '请先选择数据连接，再选择数据表',
      options: tableOptions.value,
      loading: tableLoading.value,
      filterable: true,
      clearable: true,
    },
    vIf: formData => formData.datasetType === 'TABLE',
  },
  {
    field: 'sqlText',
    label: '查询SQL',
    type: 'slot',
    slotName: 'sqlText',
    span: 12,
    rules: [{ required: true, message: '请输入查询SQL', trigger: 'blur' }],
    vIf: formData => formData.datasetType === 'SQL',
  },
  {
    field: 'sqlPreviewAction',
    label: '',
    type: 'slot',
    slotName: 'sqlPreviewAction',
    span: 12,
    showFeedback: false,
    vIf: formData => formData.datasetType === 'SQL',
  },
  {
    field: 'sourceGuide',
    label: '',
    type: 'slot',
    slotName: 'sourceGuide',
    span: 12,
    showFeedback: false,
  },
  {
    field: '__sectionParam',
    label: '查询条件定义',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'dataset-form-divider' },
  },
  {
    field: 'paramGuide',
    label: '',
    type: 'slot',
    slotName: 'paramGuide',
    span: 12,
    showFeedback: false,
  },
  {
    field: 'paramSchemaJson',
    label: '查询条件',
    type: 'slot',
    slotName: 'paramSchemaJson',
    span: 12,
    showFeedback: false,
  },
  {
    field: '__sectionSetting',
    label: '执行设置',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'dataset-form-divider' },
  },
  {
    field: 'settingGuide',
    label: '',
    type: 'slot',
    slotName: 'settingGuide',
    span: 12,
    showFeedback: false,
  },
  {
    field: 'maxRows',
    label: '最大返回行数',
    type: 'number',
    span: 3,
    disabled: () => isFormReadOnly.value,
    defaultValue: 1000,
    props: { placeholder: '请输入最大返回行数', min: 1, max: 10000 },
  },
  {
    field: 'timeoutSeconds',
    label: '查询超时(秒)',
    type: 'number',
    span: 3,
    disabled: () => isFormReadOnly.value,
    defaultValue: 15,
    props: { placeholder: '请输入超时时间', min: 1, max: 300 },
  },
  {
    field: 'description',
    label: '描述',
    type: 'textarea',
    span: 6,
    disabled: () => isFormReadOnly.value,
    props: { placeholder: '请输入描述', rows: 3 },
  },
])

loadConnectionOptions()
loadCategoryTree()

async function loadConnectionOptions() {
  try {
    const res = await getDataConnectionList()
    if (res.code === 200 && Array.isArray(res.data)) {
      connectionOptions.value = res.data.map(item => ({
        label: item.connectionName,
        value: item.id,
      }))
    }
  }
  catch (error) {
    console.error('Failed to load connections', error)
  }
}

async function loadCategoryTree(options = {}) {
  const { silent = false } = options
  try {
    const res = await getDataDatasetCategoryTree()
    if (res.code === 200 && Array.isArray(res.data)) {
      categoryTree.value = res.data
      if (activeCategoryScope.value === 'category' && selectedCategoryId.value && !findCategoryById(categoryTree.value, selectedCategoryId.value)) {
        activeCategoryScope.value = 'all'
        selectedCategoryId.value = null
      }
      return true
    }
    if (!silent) {
      window.$message?.error(res.msg || '加载数据集分类失败')
    }
    return false
  }
  catch (error) {
    console.error('Failed to load dataset categories', error)
    if (!silent) {
      window.$message?.error('加载数据集分类失败')
    }
    return false
  }
}

function getConnectionName(connectionId) {
  const connection = connectionOptions.value.find(item => item.value === connectionId)
  return connection?.label || connectionId || '-'
}

function getPublishStatusLabel(status) {
  return publishStatusOptions.find(item => item.value === status)?.label || '未发布'
}

function getDatasetTypeLabel(datasetType) {
  return datasetTypeOptions.find(item => item.value === datasetType)?.label || '单表数据集'
}

function getCategoryName(categoryId) {
  return findCategoryById(categoryTree.value, categoryId)?.categoryName || ''
}

function getCategoryGuideNote(categoryId) {
  if (categoryId) {
    return '分类用于筛选定位和默认关联，不在此页维护层级。'
  }
  if (activeCategoryScope.value === 'category' && selectedCategoryId.value) {
    return '当前从分类视角新建，保存前可继续调整默认带入的分类。'
  }
  return '未分类也可先保存，后续再补充归档。'
}

function getPublishStatusGuide(status) {
  if (status === 1) {
    return '当前内容只读，如需调整结构或 SQL，请先下架。'
  }
  if (status === 2) {
    return '已下架，可继续修改并重新发布。'
  }
  return '草稿状态，可继续完善后发布。'
}

function getDatasetSourceSummary(formData) {
  if (!formData?.connectionId) {
    return '待选择数据连接'
  }
  if (formData.datasetType === 'SQL') {
    return `连接：${getConnectionName(formData.connectionId)}`
  }
  return formData.tableName ? `表：${formData.tableName}` : `连接：${getConnectionName(formData.connectionId)}`
}

function getDatasetSourceGuide(formData) {
  if (formData?.datasetType === 'SQL') {
    return 'SQL 数据集适合多表关联、预聚合和复杂过滤，保存前建议先执行 SQL 预览。'
  }
  return '单表数据集适合标准明细表和维表建模，字段同步会按所选数据表结构生成。'
}

function getDatasetParamGuide(formData) {
  if (formData?.datasetType === 'SQL') {
    const paramCount = getSqlParamCount(formData?.sqlText)
    return paramCount > 0
      ? `当前 SQL 已识别 ${paramCount} 个命名参数，条件参数名需要与 SQL 中的 :param 完全一致。`
      : '先在 SQL 中写入 :paramName，再回到这里定义参数类型、默认值和是否必填。'
  }
  return '单表模式下每个查询条件都要映射到具体数据表字段，便于运行时安全拼装过滤条件。'
}

function getSqlParamCount(sqlText) {
  return extractSqlParamNames(sqlText).length
}

function extractSqlParamNames(sqlText) {
  if (!sqlText) {
    return []
  }

  const matches = sqlText.matchAll(/:([a-z_]\w*)/gi)
  return [...new Set(Array.from(matches, match => match[1]))]
}

function getPublishStatusTagType(status) {
  if (status === 1) {
    return 'success'
  }
  if (status === 2) {
    return 'warning'
  }
  return 'default'
}

function buildCategoryTreeNodes(tree) {
  return tree.map(item => ({
    key: item.id,
    label: item.status === 1 ? item.categoryName : `${item.categoryName} · 停用`,
    children: item.children?.length ? buildCategoryTreeNodes(item.children) : undefined,
  }))
}

function buildCategorySelectOptions(tree) {
  return tree.map(item => ({
    label: item.status === 1 ? item.categoryName : `${item.categoryName}（停用）`,
    value: item.id,
    key: item.id,
    children: item.children?.length ? buildCategorySelectOptions(item.children) : undefined,
  }))
}

function filterCategoryTree(tree, keyword) {
  const normalizedKeyword = keyword?.trim().toLowerCase()
  if (!normalizedKeyword) {
    return tree
  }

  return tree
    .map((item) => {
      const children = filterCategoryTree(item.children || [], keyword)
      const matched = item.categoryName?.toLowerCase().includes(normalizedKeyword)
        || item.categoryCode?.toLowerCase().includes(normalizedKeyword)
      if (!matched && children.length === 0) {
        return null
      }
      return {
        ...item,
        children,
      }
    })
    .filter(Boolean)
}

function findCategoryById(tree, id) {
  if (!id) {
    return null
  }
  for (const item of tree || []) {
    if (item.id === id) {
      return item
    }
    const child = findCategoryById(item.children, id)
    if (child) {
      return child
    }
  }
  return null
}

function countTreeNodes(tree) {
  return (tree || []).reduce((total, item) => total + 1 + countTreeNodes(item.children), 0)
}

function handleCategoryTreeSelect(keys) {
  const nextId = Array.isArray(keys) && keys.length > 0 ? keys[0] : null
  if (!nextId) {
    return
  }
  selectedCategoryId.value = nextId
  activeCategoryScope.value = 'category'
  applySearch()
}

function selectAllCategories() {
  activeCategoryScope.value = 'all'
  selectedCategoryId.value = null
  applySearch()
}

function selectUncategorized() {
  activeCategoryScope.value = 'uncategorized'
  selectedCategoryId.value = null
  applySearch()
}

function buildSearchParams() {
  return {
    datasetName: queryForm.datasetName?.trim() || undefined,
    connectionId: queryForm.connectionId || undefined,
    datasetType: queryForm.datasetType || undefined,
    publishStatus: queryForm.publishStatus ?? undefined,
    categoryId: activeCategoryScope.value === 'category' ? selectedCategoryId.value : undefined,
    uncategorized: activeCategoryScope.value === 'uncategorized' ? true : undefined,
  }
}

function applySearch() {
  crudRef.value?.search(buildSearchParams())
}

function handleResetFilters() {
  queryForm.datasetName = ''
  queryForm.connectionId = null
  queryForm.datasetType = null
  queryForm.publishStatus = null
  activeCategoryScope.value = 'all'
  selectedCategoryId.value = null
  crudRef.value?.search({})
}

function handleDatasetLoadSuccess({ list, total }) {
  datasetStats.total = total || 0
  datasetStats.published = (list || []).filter(item => item.publishStatus === 1).length
  datasetStats.editable = (list || []).filter(item => item.publishStatus !== 1).length
}

function handleAddDataset() {
  currentFormMode.value = 'add'
  currentEditingDataset.value = null
  crudRef.value?.showAdd()
}

function handleEdit(row) {
  currentFormMode.value = 'edit'
  currentEditingDataset.value = row
  crudRef.value?.showEdit({ ...row, __modalTitle: '编辑数据集' })
}

function handleViewDataset(row) {
  currentFormMode.value = 'view'
  currentEditingDataset.value = row
  crudRef.value?.showEdit({ ...row, __modalTitle: '查看数据集' })
}

function handleDatasetModalClose() {
  currentFormMode.value = 'edit'
  currentEditingDataset.value = null
}

function prepareDatasetFormData(sourceData = {}, options = {}) {
  const { applyScopeDefault = false } = options
  const nextFormData = {
    datasetType: 'TABLE',
    status: 1,
    maxRows: 1000,
    timeoutSeconds: 15,
    ...sourceData,
  }

  if (applyScopeDefault && !nextFormData.categoryId && activeCategoryScope.value === 'category' && selectedCategoryId.value) {
    nextFormData.categoryId = selectedCategoryId.value
  }

  nextFormData.paramSchemaJson = parseParamSchemaFormValue(nextFormData.paramSchemaJson)
  return nextFormData
}

async function beforeRenderForm(formData) {
  const nextFormData = prepareDatasetFormData(formData || {}, {
    applyScopeDefault: !formData,
  })
  const connectionId = nextFormData.connectionId
  const datasetType = nextFormData.datasetType || 'TABLE'
  if (connectionId && datasetType === 'TABLE') {
    await loadTableOptions(connectionId)
  }
  else {
    resetTableOptions()
  }
  return nextFormData
}

async function beforeRenderDetail(detailData) {
  const nextFormData = prepareDatasetFormData(detailData || {})
  currentEditingDataset.value = nextFormData
  const connectionId = nextFormData.connectionId
  const datasetType = nextFormData.datasetType || 'TABLE'
  if (connectionId && datasetType === 'TABLE') {
    await loadTableOptions(connectionId)
  }
  else {
    resetTableOptions()
  }
  return nextFormData
}

async function handleConnectionChange(connectionId, formData) {
  formData.tableName = null
  if (formData.datasetType === 'TABLE') {
    await loadTableOptions(connectionId)
  }
}

async function handleDatasetTypeChange(datasetType, formData) {
  if (datasetType === 'TABLE') {
    formData.sqlText = null
    await loadTableOptions(formData.connectionId)
    return
  }

  formData.tableName = null
}

function resetTableOptions() {
  tableOptions.value = []
  loadedTableConnectionId.value = null
  loadingTableConnectionId.value = null
}

async function loadTableOptions(connectionId) {
  if (!connectionId) {
    resetTableOptions()
    return
  }
  if (loadedTableConnectionId.value === connectionId && tableOptions.value.length > 0) {
    return
  }
  if (tableLoading.value && loadingTableConnectionId.value === connectionId) {
    return
  }

  tableLoading.value = true
  loadingTableConnectionId.value = connectionId
  try {
    const res = await getDataConnectionTables(connectionId)
    if (res.code === 200 && Array.isArray(res.data)) {
      tableOptions.value = res.data.map(table => ({
        label: table.tableComment ? `${table.tableName}（${table.tableComment}）` : table.tableName,
        value: table.tableName,
      }))
      loadedTableConnectionId.value = connectionId
    }
    else {
      resetTableOptions()
    }
  }
  catch (error) {
    console.error('Failed to load tables', error)
    resetTableOptions()
    window.$message?.error('加载数据表失败')
  }
  finally {
    tableLoading.value = false
    loadingTableConnectionId.value = null
  }
}

function beforeSubmit(formData) {
  if (isFormReadOnly.value) {
    return false
  }

  delete formData.publishReadonlyAlert
  delete formData.datasetOverview
  delete formData.__sectionBasic
  delete formData.__sectionSource
  delete formData.__sectionParam
  delete formData.__sectionSetting
  delete formData.sqlPreviewAction
  delete formData.sourceGuide
  delete formData.paramGuide
  delete formData.settingGuide

  if (!formData.connectionId) {
    window.$message?.error('请选择数据连接')
    return false
  }

  if (formData.datasetType === 'TABLE') {
    if (!formData.tableName) {
      window.$message?.error('请选择数据表')
      return false
    }
    formData.sqlText = null
  }
  else if (formData.datasetType === 'SQL') {
    if (!formData.sqlText) {
      window.$message?.error('请输入查询SQL')
      return false
    }
    formData.tableName = null
  }

  const normalizedSchema = normalizeParamSchema(formData.paramSchemaJson, formData.datasetType)
  if (normalizedSchema === null) {
    return false
  }

  formData.paramSchemaJson = normalizedSchema.length > 0
    ? JSON.stringify(normalizedSchema, null, 2)
    : null

  return formData
}

function parseParamSchemaFormValue(value) {
  if (!value) {
    return []
  }
  if (Array.isArray(value)) {
    return value
  }

  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  }
  catch (error) {
    console.error('Failed to parse dataset param schema', error)
    window.$message?.error('查询参数定义格式异常，已按空配置处理')
    return []
  }
}

function normalizeParamSchema(rows, datasetType) {
  if (!rows) {
    return []
  }
  if (!Array.isArray(rows)) {
    window.$message?.error('查询条件配置格式不正确')
    return null
  }

  const normalizedRows = []
  const paramNames = new Set()

  for (const [index, row] of rows.entries()) {
    const paramName = typeof row?.paramName === 'string' ? row.paramName.trim() : ''
    const label = typeof row?.label === 'string' ? row.label.trim() : ''
    const dataType = typeof row?.dataType === 'string' && row.dataType
      ? row.dataType.trim().toUpperCase()
      : 'STRING'
    const operator = typeof row?.operator === 'string' && row.operator
      ? row.operator.trim().toUpperCase()
      : '='
    const fieldName = typeof row?.fieldName === 'string' ? row.fieldName.trim() : ''
    const defaultValue = row?.defaultValue === '' ? null : row?.defaultValue ?? null
    const required = row?.required === true
    const isEmptyRow = !paramName && !label && !fieldName && defaultValue === null && required === false

    if (isEmptyRow) {
      continue
    }
    if (!paramName) {
      window.$message?.error(`第${index + 1}行缺少条件参数名`)
      return null
    }
    if (paramNames.has(paramName)) {
      window.$message?.error(`条件参数名重复：${paramName}`)
      return null
    }
    if (!supportedParamOperators.includes(operator)) {
      window.$message?.error(`第${index + 1}行匹配方式不支持：${operator}`)
      return null
    }
    if (datasetType === 'TABLE' && !fieldName) {
      window.$message?.error(`第${index + 1}行还未选择数据表字段`)
      return null
    }
    if (datasetType === 'SQL' && fieldName) {
      window.$message?.error(`第${index + 1}行不需要配置数据表字段`)
      return null
    }

    paramNames.add(paramName)
    normalizedRows.push({
      paramName,
      label: label || null,
      dataType,
      required,
      defaultValue,
      operator,
      fieldName: fieldName || null,
    })
  }

  return normalizedRows
}

async function handlePreviewSql(formData) {
  if (!formData.connectionId) {
    window.$message?.error('请选择数据连接')
    return
  }
  if (!formData.sqlText) {
    window.$message?.error('请输入查询SQL')
    return
  }

  sqlPreviewVisible.value = true
  sqlPreviewLoading.value = true
  sqlPreviewColumns.value = []
  sqlPreviewRows.value = []
  sqlPreviewScrollX.value = 0

  try {
    const res = await request.post('/data/dataset/preview-sql', {
      connectionId: formData.connectionId,
      sqlText: formData.sqlText,
      maxRows: 10,
    })
    if (res.code === 200) {
      const columns = res.data?.columns || []
      sqlPreviewColumns.value = columns.map(column => ({
        title: column,
        key: column,
        width: 160,
        ellipsis: { tooltip: true },
        render: row => row[column] ?? '',
      }))
      sqlPreviewRows.value = res.data?.rows || []
      sqlPreviewScrollX.value = Math.max(columns.length * 160, 800)
      window.$message?.success(`SQL校验通过，预览 ${sqlPreviewRows.value.length} 条数据`)
    }
    else {
      window.$message?.error(res.msg || 'SQL预览失败')
    }
  }
  catch (error) {
    window.$message?.error(error?.message || 'SQL预览失败')
  }
  finally {
    sqlPreviewLoading.value = false
  }
}

async function handleViewFields(row) {
  fieldModalTitle.value = `字段列表 - ${row.datasetName}`
  fieldModalVisible.value = true
  fieldLoading.value = true
  fieldRows.value = []

  try {
    const res = await request.get(`/data/dataset/${row.id}`)
    if (res.code === 200) {
      fieldRows.value = res.data?.fields || []
    }
    else {
      window.$message?.error(res.msg || '加载字段失败')
    }
  }
  catch (error) {
    console.error('Failed to load dataset fields', error)
    window.$message?.error('加载字段失败')
  }
  finally {
    fieldLoading.value = false
  }
}

function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除数据集“${row.datasetName}”吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await deleteDataDataset(row.id)
        if (res.code === 200) {
          window.$message?.success('删除成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message?.error(error?.message || '删除失败')
      }
    },
  })
}

async function handleSyncFields(row) {
  try {
    window.$message?.loading('正在同步字段...', { duration: 0, key: 'syncFields' })
    const res = await syncDataDatasetFields(row.id)
    if (res.code === 200) {
      window.$message?.success(`同步成功，共 ${res.data?.length || 0} 个字段`, { key: 'syncFields' })
      fieldRows.value = res.data || []
    }
    else {
      window.$message?.error(res.msg || '同步失败', { key: 'syncFields' })
    }
  }
  catch (error) {
    window.$message?.error(error?.message || '同步字段失败', { key: 'syncFields' })
  }
}

function handlePublishDataset(row) {
  window.$dialog.warning({
    title: '确认发布',
    content: `发布后数据集“${row.datasetName}”将进入只读状态，仅可查看和下架，确认继续吗？`,
    positiveText: '发布',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await publishDataDataset(row.id)
        if (res.code === 200) {
          window.$message?.success('发布成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message?.error(error?.message || '发布失败')
      }
    },
  })
}

function handleOfflineDataset(row) {
  window.$dialog.warning({
    title: '确认下架',
    content: `下架后数据集“${row.datasetName}”将暂停供下游使用，但可以继续修改，确认继续吗？`,
    positiveText: '下架',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await offlineDataDataset(row.id)
        if (res.code === 200) {
          window.$message?.success('下架成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message?.error(error?.message || '下架失败')
      }
    },
  })
}

function goToCategoryManage() {
  router.push('/data/dataset-category')
}
</script>

<style scoped>
.dataset-studio {
  --studio-bg: linear-gradient(180deg, #f4f7fb 0%, #eef3f8 100%);
  --panel-bg: rgb(255 255 255 / 92%);
  --panel-border: rgb(148 163 184 / 16%);
  --panel-shadow: 0 18px 50px rgb(15 23 42 / 10%);
  --panel-radius: 24px;
  min-height: 100%;
  padding: 20px;
  background: var(--studio-bg);
}

.studio-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(420px, 0.8fr);
  gap: 18px;
  margin-bottom: 18px;
}

.hero-main,
.hero-stat-card,
.workspace-sidebar,
.workspace-main {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--panel-border);
  border-radius: var(--panel-radius);
  background: var(--panel-bg);
  box-shadow: var(--panel-shadow);
  backdrop-filter: blur(14px);
}

.hero-main {
  padding: 28px 30px;
  background:
    radial-gradient(circle at top left, rgb(59 130 246 / 18%), transparent 40%),
    radial-gradient(circle at 90% 25%, rgb(14 165 233 / 16%), transparent 28%),
    linear-gradient(135deg, rgb(255 255 255 / 94%), rgb(248 251 255 / 92%));
}

.hero-kicker,
.panel-kicker {
  margin: 0 0 8px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hero-title {
  margin: 0;
  color: #0f172a;
  font-size: 34px;
  line-height: 1.15;
}

.hero-description {
  max-width: 720px;
  margin: 14px 0 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.8;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.hero-stat-card {
  padding: 22px 20px;
}

.hero-stat-card::after {
  position: absolute;
  inset: auto -18px -24px auto;
  width: 96px;
  height: 96px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgb(14 165 233 / 18%), rgb(37 99 235 / 0%));
  content: '';
}

.hero-stat-label {
  color: #64748b;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-stat-value {
  margin-top: 12px;
  color: #0f172a;
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
}

.hero-stat-note {
  margin-top: 10px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.dataset-workspace {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.workspace-sidebar,
.workspace-main {
  padding: 20px;
}

.sidebar-head,
.toolbar-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.sidebar-head h3,
.toolbar-title-row h3 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
}

.sidebar-shortcuts {
  display: flex;
  gap: 10px;
  margin: 18px 0 14px;
}

.scope-chip {
  padding: 9px 14px;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    color 0.2s ease,
    background 0.2s ease,
    transform 0.2s ease;
}

.scope-chip.active,
.scope-chip:hover {
  color: #0f766e;
  background: #ecfeff;
  border-color: #99f6e4;
  transform: translateY(-1px);
}

.category-search {
  margin-bottom: 14px;
}

.category-tree-shell {
  min-height: 360px;
  max-height: calc(100vh - 520px);
  padding: 12px;
  overflow: auto;
  background: linear-gradient(180deg, #fbfdff 0%, #f8fbff 100%);
  border: 1px solid #e2e8f0;
  border-radius: 18px;
}

.category-detail-card {
  margin-top: 12px;
  padding: 14px 16px;
  background: linear-gradient(135deg, rgb(15 23 42 / 0.94), rgb(30 41 59 / 0.92));
  border-radius: 18px;
}

.category-detail-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.category-detail-name {
  color: #f8fafc;
  font-size: 18px;
  font-weight: 600;
}

.category-detail-code {
  margin-top: 6px;
  color: rgb(186 230 253 / 0.86);
  font-size: 12px;
}

.category-detail-desc {
  margin: 14px 0 0;
  color: rgb(226 232 240 / 0.8);
  font-size: 13px;
  line-height: 1.7;
}

.main-toolbar {
  margin-bottom: 18px;
  padding: 18px 18px 16px;
  background: linear-gradient(180deg, rgb(248 250 252 / 0.86), rgb(255 255 255 / 0.94));
  border: 1px solid #e2e8f0;
  border-radius: 20px;
}

.toolbar-title-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.toolbar-scope {
  padding: 6px 10px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 600;
  background: #ecfeff;
  border: 1px solid #a5f3fc;
  border-radius: 999px;
}

.toolbar-filters {
  display: grid;
  grid-template-columns: minmax(260px, 1.5fr) repeat(3, minmax(150px, 0.8fr)) auto;
  gap: 12px;
  margin-top: 18px;
}

.toolbar-filter {
  min-width: 0;
}

.toolbar-filter--keyword {
  width: 100%;
}

.asset-name-card {
  display: grid;
  gap: 7px;
}

.asset-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.asset-name {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.asset-code {
  color: #0f766e;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.asset-desc {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-category-name,
.asset-source-name {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.asset-category-code,
.asset-source-detail {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.sql-preview-action {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 34px;
  padding: 2px 0 4px;
}

:deep(.dataset-crud .ai-crud-main) {
  background: transparent;
}

:deep(.dataset-crud .ai-crud-table) {
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 22px;
  background: rgb(255 255 255 / 0.92);
  box-shadow: 0 16px 36px rgb(15 23 42 / 6%);
}

:deep(.dataset-crud .n-data-table-th) {
  background: #f8fafc;
}

:deep(.dataset-crud .n-data-table-tr:hover td) {
  background: #fbfdff;
}

:deep(.n-tree .n-tree-node-content) {
  min-height: 38px;
  padding: 0 8px;
  border-radius: 12px;
  transition:
    background 0.18s ease,
    color 0.18s ease;
}

:deep(.n-tree .n-tree-node-content:hover) {
  background: #eef6ff;
}

:deep(.n-tree .n-tree-node--selected > .n-tree-node-content) {
  background: #e0f2fe;
}

:global(.data-dataset-edit-form) {
  padding: 4px 2px 0;
}

:global(.data-dataset-edit-form .dataset-guide-grid) {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

:global(.data-dataset-edit-form .dataset-guide-card) {
  padding: 14px 16px;
  border: 1px solid #dbe8f5;
  border-radius: 16px;
  background: linear-gradient(180deg, #fbfdff 0%, #f6faff 100%);
}

:global(.data-dataset-edit-form .guide-label) {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

:global(.data-dataset-edit-form .guide-value) {
  margin-top: 10px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.4;
}

:global(.data-dataset-edit-form .guide-note) {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

:global(.data-dataset-edit-form .dataset-inline-guide) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  background: linear-gradient(180deg, #f8fbff 0%, #f4f8fc 100%);
  border: 1px solid #d8e6f4;
  border-radius: 14px;
}

:global(.data-dataset-edit-form .dataset-inline-guide--soft) {
  background: linear-gradient(180deg, #fbfdff 0%, #f8fafc 100%);
}

:global(.data-dataset-edit-form .inline-guide-main) {
  min-width: 0;
}

:global(.data-dataset-edit-form .inline-guide-title) {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

:global(.data-dataset-edit-form .inline-guide-desc) {
  margin-top: 6px;
  color: #475569;
  font-size: 12px;
  line-height: 1.75;
}

:global(.data-dataset-edit-form .inline-guide-pills) {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

:global(.data-dataset-edit-form .guide-pill) {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  background: #ecfeff;
  border: 1px solid #a5f3fc;
  border-radius: 999px;
}

:global(.data-dataset-edit-form .guide-pill--muted) {
  color: #475569;
  background: #fff;
  border-color: #dbe3ef;
}

:global(.data-dataset-edit-form .dataset-runtime-strip) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 12px;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
}

:global(.data-dataset-edit-form .runtime-chip) {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  padding: 6px 10px;
  color: #334155;
  font-size: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
}

:global(.data-dataset-edit-form .runtime-chip strong) {
  color: #0f172a;
  font-size: 14px;
}

:global(.data-dataset-edit-form .runtime-note) {
  color: #475569;
  font-size: 12px;
  line-height: 1.7;
}

:global(.data-dataset-edit-form .n-form-item) {
  margin-bottom: 8px;
  padding: 12px;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #fff;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

:global(.data-dataset-edit-form .n-form-item:hover) {
  border-color: #cbd8ea;
  box-shadow: 0 2px 8px rgb(15 23 42 / 4%);
}

:global(.data-dataset-edit-form .n-form-item-blank) {
  width: 100%;
}

:global(.data-dataset-edit-form .n-form-item-label) {
  min-height: 20px;
  margin-bottom: 7px;
  color: #475569;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.3;
}

:global(.data-dataset-edit-form .dataset-form-divider) {
  margin: 12px 0 8px;
  color: #64748b;
}

:global(.data-dataset-edit-form .dataset-form-divider::before),
:global(.data-dataset-edit-form .dataset-form-divider::after) {
  border-top-color: #dbe3ef;
}

:global(.data-dataset-edit-form .dataset-form-divider .n-divider__title) {
  color: #1e293b;
  font-size: 14px;
  font-weight: 600;
}

:global(.data-dataset-edit-form .n-input),
:global(.data-dataset-edit-form .n-input-number),
:global(.data-dataset-edit-form .n-select),
:global(.data-dataset-edit-form .n-tree-select) {
  width: 100%;
}

:global(.data-dataset-edit-form .sql-editor) {
  width: 100%;
}

:global(.data-dataset-edit-form textarea) {
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  line-height: 1.55;
}

:global(.data-dataset-edit-form .n-radio-group .n-space) {
  gap: 8px 18px !important;
}

@media (max-width: 1400px) {
  .studio-hero {
    grid-template-columns: 1fr;
  }

  .dataset-workspace {
    grid-template-columns: 1fr;
  }

  .category-tree-shell {
    max-height: 360px;
  }
}

@media (max-width: 960px) {
  .dataset-studio {
    padding: 14px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .toolbar-filters {
    grid-template-columns: 1fr;
  }

  .toolbar-title-row,
  .sidebar-head {
    flex-direction: column;
  }

  :global(.data-dataset-edit-form .dataset-guide-grid) {
    grid-template-columns: 1fr;
  }

  :global(.data-dataset-edit-form .dataset-inline-guide) {
    flex-direction: column;
    align-items: flex-start;
  }

  :global(.data-dataset-edit-form .inline-guide-pills) {
    justify-content: flex-start;
  }

  .toolbar-title-meta {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
