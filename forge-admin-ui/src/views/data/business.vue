<template>
  <div class="business-studio">
    <section class="business-hero">
      <div class="hero-main">
        <p class="hero-kicker">
          Business Semantics
        </p>
        <h1 class="hero-title">
          业务定义管理台
        </h1>
        <p class="hero-description">
          把业务目标、指标口径和可用数据集绑定为一份 AI 可读的业务语义，让大屏生成从“描述驱动”升级为“数据资产驱动”。
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

    <section class="business-panel">
      <div class="panel-toolbar">
        <div>
          <p class="panel-kicker">
            AI Ready Assets
          </p>
          <h3>业务定义清单</h3>
        </div>
        <div class="toolbar-actions">
          <NInput
            v-model:value="queryForm.businessName"
            clearable
            placeholder="搜索业务名称或编码"
            @keydown.enter="applySearch"
          >
            <template #prefix>
              <i class="i-material-symbols:search-rounded" />
            </template>
          </NInput>
          <NSelect
            v-model:value="queryForm.status"
            clearable
            placeholder="状态"
            :options="statusOptions"
          />
          <NButton type="primary" @click="applySearch">
            搜索
          </NButton>
          <NButton @click="handleReset">
            重置
          </NButton>
          <NButton type="primary" secondary @click="handleOpenAddBusiness">
            新增业务定义
          </NButton>
        </div>
      </div>

      <AiCrudPage
        ref="crudRef"
        class="business-crud"
        :api-config="{
          list: 'get@/data/business/page',
          detail: 'get@/data/business/:id',
          add: 'post@/data/business',
          update: 'put@/data/business',
          delete: 'delete@/data/business/:id',
        }"
        :show-search="false"
        :hide-toolbar="true"
        :columns="tableColumns"
        :edit-schema="editSchema"
        :before-render-form="beforeRenderForm"
        :before-render-detail="beforeRenderDetail"
        :before-submit="beforeSubmit"
        :load-detail-on-edit="true"
        row-key="id"
        :bordered="false"
        :striped="false"
        :scroll-x="1480"
        max-height="calc(100vh - 250px)"
        :edit-grid-cols="12"
        edit-label-placement="top"
        edit-form-class="data-business-edit-form"
        modal-width="min(1180px, calc(100vw - 32px))"
        add-button-text="新增业务定义"
        @load-list-success="handleBusinessLoadSuccess"
      >
        <template #form-businessGuide="{ formData }">
          <div class="business-guide">
            <div class="business-guide__main">
              <div class="business-guide__eyebrow">
                Business Context
              </div>
              <div class="business-guide__title">
                {{ formData.businessName || '先定义业务，再绑定可消费的数据集' }}
              </div>
              <div class="business-guide__desc">
                AI 生成大屏时会读取这里的业务定义、指标口径、分析维度和绑定数据集字段，优先生成可自动查询的数据组件。
              </div>
            </div>
            <div class="business-guide__facts">
              <div class="guide-fact">
                <span>业务编码</span>
                <strong>{{ formData.businessCode || '待填写' }}</strong>
              </div>
              <div class="guide-fact">
                <span>绑定数据集</span>
                <strong>{{ (formData.datasets || []).filter(item => item.datasetId).length }} 个</strong>
              </div>
              <div class="guide-fact">
                <span>状态</span>
                <strong>{{ formData.status === 0 ? '禁用' : '启用' }}</strong>
              </div>
            </div>
            <div class="business-readiness-panel" :class="`is-${getBusinessReadiness(formData).level}`">
              <div class="business-readiness-main">
                <span>AI 准备度</span>
                <strong>{{ getBusinessReadiness(formData).score }} / {{ getBusinessReadiness(formData).label }}</strong>
              </div>
              <div class="business-readiness-bar">
                <span :style="{ width: `${getBusinessReadiness(formData).score}%` }" />
              </div>
              <div class="business-readiness-tip">
                {{ getBusinessReadiness(formData).suggestions[0] || '业务语义和数据集绑定已满足 AI 生成要求' }}
              </div>
            </div>
            <div class="business-template-panel">
              <div class="business-template-head">
                <span>模板填充</span>
                <small>快速生成 AI 可读业务语义</small>
              </div>
              <div class="business-template-list">
                <NButton
                  v-for="template in businessTemplates"
                  :key="template.key"
                  size="small"
                  secondary
                  @click="applyBusinessTemplate(formData, template)"
                >
                  {{ template.name }}
                </NButton>
              </div>
            </div>
          </div>
        </template>

        <template #form-datasets="{ value, updateValue }">
          <div class="dataset-binding-panel">
            <div class="dataset-binding-toolbar">
              <div>
                <div class="dataset-binding-title">
                  绑定数据集
                </div>
                <div class="dataset-binding-desc">
                  选择已发布数据集，并补充用途说明。AI 会按这些数据集字段优先生成动态数据组件。
                </div>
              </div>
              <NButton type="primary" secondary @click="addDatasetBinding(value, updateValue)">
                <template #icon>
                  <i class="i-material-symbols:add-rounded" />
                </template>
                添加数据集
              </NButton>
            </div>

            <n-empty
              v-if="!value || value.length === 0"
              description="暂未绑定数据集"
              size="small"
            />
            <n-data-table
              v-else
              :columns="datasetBindingColumns(updateValue)"
              :data="getDatasetBindingRows(value)"
              :pagination="false"
              :scroll-x="980"
              size="small"
              striped
            />
          </div>
        </template>
      </AiCrudPage>
    </section>
  </div>
</template>

<script setup>
import { NButton, NInput, NInputNumber, NProgress, NSelect, NSwitch, NTag } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { deleteDataBusiness } from '@/api/data/business'
import { getDataDatasetList } from '@/api/data/dataset'

defineOptions({ name: 'DataBusiness' })

const crudRef = ref(null)
const datasetOptions = ref([])
const datasetOptionMap = computed(() => new Map(datasetOptions.value.map(item => [item.value, item])))

const queryForm = reactive({
  businessName: '',
  status: null,
})

const businessStats = reactive({
  total: 0,
  active: 0,
  disabled: 0,
  datasets: 0,
})

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

const statCards = computed(() => [
  {
    key: 'total',
    label: '筛选结果',
    value: businessStats.total,
    note: '匹配当前筛选条件的业务定义总数',
  },
  {
    key: 'active',
    label: '当前页启用',
    value: businessStats.active,
    note: '可在 report-ui AI 大屏生成时选择使用',
  },
  {
    key: 'disabled',
    label: '当前页禁用',
    value: businessStats.disabled,
    note: '不会作为 AI 生成上下文使用',
  },
  {
    key: 'datasets',
    label: '当前页绑定',
    value: businessStats.datasets,
    note: '当前页业务定义绑定的数据集总数',
  },
])

const businessTemplates = [
  {
    key: 'sales_overview',
    name: '销售经营',
    businessCode: 'sales_overview',
    businessName: '销售经营分析',
    businessDesc: '面向销售经营管理场景，围绕销售额、订单、客户、渠道、区域和商品等核心对象，监控经营规模、增长趋势、结构贡献和异常波动。',
    analysisGoal: '快速识别销售增长来源、区域/渠道贡献差异、订单转化变化和重点商品表现，为经营复盘、资源投放和销售目标达成提供依据。',
    metricDefinition: '核心指标包括销售额、订单量、客单价、支付转化率、退款金额、活跃客户数、同比增速和环比增速。金额类指标按元展示，趋势类指标按日/月聚合。',
    dimensionDefinition: '按时间、区域、渠道、门店、商品类目、客户类型、销售组织等维度分析，可组合展示趋势、占比、排行和明细。',
    usageGuide: '优先使用销售汇总数据集生成 KPI、趋势和结构图；使用订单明细或商品排行数据集生成 TopN、滚动表格和异常列表。无法映射字段时再使用静态模拟数据。',
  },
  {
    key: 'operation_monitor',
    name: '运营监控',
    businessCode: 'operation_monitor',
    businessName: '业务运营监控',
    businessDesc: '面向日常运营指挥场景，描述业务运行状态、关键过程指标、实时告警、任务处置和资源使用情况，支持管理人员快速判断当前运营健康度。',
    analysisGoal: '监控核心业务链路是否稳定，发现流量、订单、服务、告警和任务处理中的异常变化，辅助运营调度和问题定位。',
    metricDefinition: '核心指标包括访问量、办理量、完成率、超时率、告警数、待处理数、平均处理时长和资源利用率。实时类指标按最新批次展示，趋势类指标按时间序列展示。',
    dimensionDefinition: '按时间、业务类型、区域、组织、状态、优先级、处理人和渠道等维度分析。',
    usageGuide: '概览区使用汇总数据集；趋势区使用按时间聚合的数据集；告警、任务和事件使用明细数据集生成列表或时间线。',
  },
  {
    key: 'finance_analysis',
    name: '财务分析',
    businessCode: 'finance_analysis',
    businessName: '财务经营分析',
    businessDesc: '面向财务经营分析场景，围绕收入、成本、利润、预算、回款和费用等核心财务对象，展示经营质量、预算执行和风险变化。',
    analysisGoal: '分析收入利润走势、预算执行偏差、成本费用结构、回款风险和部门/项目贡献，帮助管理层掌握经营质量。',
    metricDefinition: '核心指标包括营业收入、成本、毛利、毛利率、净利润、预算完成率、应收账款、回款率和费用率。金额字段统一按元或万元展示，比例字段按百分比展示。',
    dimensionDefinition: '按期间、组织、项目、客户、产品线、费用类型、区域和会计科目等维度分析。',
    usageGuide: '财务汇总数据集适合 KPI、趋势和预算对比；费用明细、应收明细适合排行、结构占比和风险列表。',
  },
  {
    key: 'equipment_monitor',
    name: '设备监控',
    businessCode: 'equipment_monitor',
    businessName: '设备运行监控',
    businessDesc: '面向设备、产线、能源或物联网监控场景，描述设备运行状态、产能效率、告警事件、维护工单和能耗指标。',
    analysisGoal: '实时掌握设备在线状态、运行效率、异常告警、产线负荷和能耗变化，支撑生产调度、故障定位和维护决策。',
    metricDefinition: '核心指标包括在线设备数、运行率、故障数、告警数、产量、OEE、能耗、停机时长和维修完成率。状态类指标展示当前值，趋势类指标按小时/日聚合。',
    dimensionDefinition: '按设备、产线、车间、区域、状态、告警等级、时间和维护类型等维度分析。',
    usageGuide: '设备状态数据集适合状态标签、分布和地图；生产/能耗数据集适合趋势和对比；告警/工单明细适合滚动列表和时间线。',
  },
]

const tableColumns = computed(() => [
  {
    prop: 'businessName',
    label: '业务定义',
    width: 340,
    render: row => h('div', { class: 'business-name-card' }, [
      h('div', { class: 'business-name-row' }, [
        h('div', { class: 'business-name' }, row.businessName),
        h(NTag, {
          size: 'small',
          bordered: false,
          type: row.status === 1 ? 'success' : 'default',
        }, { default: () => row.status === 1 ? '启用' : '禁用' }),
      ]),
      h('div', { class: 'business-code' }, row.businessCode),
      h('div', { class: 'business-desc' }, row.businessDesc || '暂无业务定义描述'),
    ]),
  },
  {
    prop: 'analysisGoal',
    label: '分析目标',
    width: 300,
    ellipsis: true,
    render: row => row.analysisGoal || '-',
  },
  {
    prop: 'datasetNames',
    label: '绑定数据集',
    width: 240,
    render: row => renderDatasetSummary(row),
  },
  {
    prop: 'aiReadiness',
    label: 'AI准备度',
    width: 170,
    render: row => renderReadiness(row),
  },
  {
    prop: 'metricDefinition',
    label: '指标口径',
    width: 300,
    ellipsis: true,
    render: row => row.metricDefinition || '-',
  },
  { prop: 'updateTime', label: '更新时间', width: 170 },
  {
    prop: 'action',
    label: '操作',
    width: 180,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEditBusiness },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDeleteBusiness },
    ],
  },
])

const editSchema = computed(() => [
  {
    field: 'businessGuide',
    label: '',
    type: 'slot',
    slotName: 'businessGuide',
    span: 12,
    showFeedback: false,
  },
  {
    field: 'businessCode',
    label: '业务编码',
    type: 'input',
    span: 4,
    rules: [{ required: true, message: '请输入业务编码', trigger: 'blur' }],
    props: { placeholder: '如 sales_overview' },
  },
  {
    field: 'businessName',
    label: '业务名称',
    type: 'input',
    span: 4,
    rules: [{ required: true, message: '请输入业务名称', trigger: 'blur' }],
    props: { placeholder: '如销售经营分析' },
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    span: 4,
    defaultValue: 1,
    props: { options: statusOptions },
  },
  {
    field: 'businessDesc',
    label: '业务定义描述',
    type: 'textarea',
    span: 12,
    rules: [{ required: true, message: '请输入业务定义描述', trigger: 'blur' }],
    props: {
      placeholder: '描述业务对象、业务边界、核心管理动作和数据使用范围',
      rows: 3,
    },
  },
  {
    field: 'analysisGoal',
    label: '分析目标',
    type: 'textarea',
    span: 6,
    props: {
      placeholder: '例如：监控销售额、订单量、转化率、区域贡献和异常波动',
      rows: 3,
    },
  },
  {
    field: 'metricDefinition',
    label: '指标口径',
    type: 'textarea',
    span: 6,
    props: {
      placeholder: '定义核心指标、单位、聚合方式和展示口径',
      rows: 3,
    },
  },
  {
    field: 'dimensionDefinition',
    label: '分析维度',
    type: 'textarea',
    span: 6,
    props: {
      placeholder: '例如：按时间、区域、渠道、客户、产品、组织等维度分析',
      rows: 3,
    },
  },
  {
    field: 'usageGuide',
    label: 'AI 使用建议',
    type: 'textarea',
    span: 6,
    props: {
      placeholder: '告诉 AI 哪些数据集适合做概览指标、趋势、排行、表格或地图',
      rows: 3,
    },
  },
  {
    field: 'datasets',
    label: '',
    type: 'slot',
    slotName: 'datasets',
    span: 12,
    showFeedback: false,
  },
])

onMounted(loadDatasetOptions)

async function loadDatasetOptions() {
  try {
    const res = await getDataDatasetList()
    const records = res.data || []
    datasetOptions.value = records.map(item => ({
      label: `${item.datasetName}${item.datasetCode ? ` (${item.datasetCode})` : ''}`,
      value: item.id,
      datasetName: item.datasetName,
      datasetCode: item.datasetCode,
      datasetType: item.datasetType,
    }))
  }
  catch (error) {
    window.$message?.error(error?.message || '加载数据集失败')
  }
}

function applySearch() {
  crudRef.value?.search({
    businessName: queryForm.businessName || undefined,
    status: queryForm.status ?? undefined,
  })
}

function handleReset() {
  queryForm.businessName = ''
  queryForm.status = null
  crudRef.value?.search({})
}

function handleOpenAddBusiness() {
  crudRef.value?.showAdd()
}

function hasText(value) {
  return !!String(value || '').trim()
}

function getDatasetCount(source) {
  if (Array.isArray(source?.datasets)) {
    return source.datasets.filter(item => item.datasetId).length
  }
  return source?.datasetCount || 0
}

function getBusinessReadiness(source = {}) {
  let score = 0
  const suggestions = []
  const semanticFields = [
    { key: 'businessDesc', score: 14, suggestion: '补充业务定义描述，说明业务边界和管理对象。' },
    { key: 'analysisGoal', score: 12, suggestion: '补充分析目标，让 AI 知道大屏要回答什么问题。' },
    { key: 'metricDefinition', score: 16, suggestion: '补充指标口径，明确单位、聚合和同比环比口径。' },
    { key: 'dimensionDefinition', score: 12, suggestion: '补充分析维度，如时间、区域、渠道、组织等。' },
    { key: 'usageGuide', score: 11, suggestion: '补充 AI 使用建议，说明数据集适合做哪些组件。' },
  ]

  semanticFields.forEach(item => {
    if (hasText(source[item.key])) {
      score += item.score
    }
    else {
      suggestions.push(item.suggestion)
    }
  })

  const datasetCount = getDatasetCount(source)
  if (datasetCount > 0) {
    score += Math.min(24, 14 + datasetCount * 3)
  }
  else {
    suggestions.push('至少绑定一个数据集，否则 AI 大屏只能生成静态组件。')
  }

  if (Array.isArray(source.datasets) && source.datasets.some(item => hasText(item.usageRemark))) {
    score += 6
  }
  else if (datasetCount > 0) {
    suggestions.push('为数据集补充用途说明，可提升动态绑定成功率。')
  }

  if ((source.status ?? 1) === 1) {
    score += 5
  }

  const normalizedScore = Math.min(100, score)
  const level = normalizedScore >= 80 ? 'high' : normalizedScore >= 55 ? 'medium' : 'low'
  const label = level === 'high' ? '准备充分' : level === 'medium' ? '可生成' : '待完善'

  return {
    score: normalizedScore,
    level,
    label,
    suggestions: suggestions.slice(0, 3),
  }
}

function getReadinessColor(level) {
  if (level === 'high') return '#0f766e'
  if (level === 'medium') return '#d97706'
  return '#dc2626'
}

function renderReadiness(row) {
  const readiness = getBusinessReadiness(row)
  return h('div', { class: 'readiness-cell' }, [
    h('div', { class: 'readiness-cell-head' }, [
      h('strong', { style: { color: getReadinessColor(readiness.level) } }, `${readiness.score}`),
      h('span', readiness.label),
    ]),
    h(NProgress, {
      type: 'line',
      percentage: readiness.score,
      height: 6,
      borderRadius: 3,
      fillBorderRadius: 3,
      showIndicator: false,
      color: getReadinessColor(readiness.level),
    }),
  ])
}

function applyBusinessTemplate(formData, template) {
  if (!formData.businessCode) {
    formData.businessCode = template.businessCode
  }
  if (!formData.businessName) {
    formData.businessName = template.businessName
  }
  formData.businessDesc = template.businessDesc
  formData.analysisGoal = template.analysisGoal
  formData.metricDefinition = template.metricDefinition
  formData.dimensionDefinition = template.dimensionDefinition
  formData.usageGuide = template.usageGuide
  window.$message?.success(`已应用“${template.name}”模板`)
}

function handleEditBusiness(row) {
  crudRef.value?.showEdit({ ...row, __modalTitle: '编辑业务定义' })
}

function handleBusinessLoadSuccess({ list, total }) {
  const rows = list || []
  businessStats.total = total || 0
  businessStats.active = rows.filter(item => item.status === 1).length
  businessStats.disabled = rows.filter(item => item.status === 0).length
  businessStats.datasets = rows.reduce((sum, item) => sum + (item.datasetCount || 0), 0)
}

function beforeRenderForm(formData) {
  return normalizeBusinessForm(formData || {})
}

function beforeRenderDetail(detailData) {
  return normalizeBusinessForm(detailData || {})
}

function normalizeBusinessForm(sourceData) {
  return {
    status: 1,
    datasets: normalizeDatasetBindings(sourceData.datasets),
    ...sourceData,
    datasets: normalizeDatasetBindings(sourceData.datasets),
  }
}

function normalizeDatasetBindings(bindings) {
  if (!Array.isArray(bindings) || bindings.length === 0) {
    return []
  }
  return bindings.map((item, index) => ({
    __key: item.__key || `${item.datasetId || 'dataset'}-${index}-${Date.now()}`,
    id: item.id,
    datasetId: item.datasetId,
    datasetName: item.datasetName,
    datasetCode: item.datasetCode,
    datasetType: item.datasetType,
    description: item.description,
    isPrimary: item.isPrimary ?? (index === 0 ? 1 : 0),
    sort: item.sort ?? index,
    usageRemark: item.usageRemark || '',
  }))
}

function beforeSubmit(formData) {
  delete formData.businessGuide

  if (!formData.businessCode) {
    window.$message?.error('请输入业务编码')
    return false
  }
  if (!formData.businessName) {
    window.$message?.error('请输入业务名称')
    return false
  }
  if (!formData.businessDesc) {
    window.$message?.error('请输入业务定义描述')
    return false
  }
  const datasets = normalizeDatasetBindings(formData.datasets).filter(item => item.datasetId)
  if (!datasets.length) {
    window.$message?.error('请至少绑定一个数据集')
    return false
  }
  const datasetIds = new Set()
  for (const item of datasets) {
    if (datasetIds.has(item.datasetId)) {
      window.$message?.error('绑定数据集不能重复')
      return false
    }
    datasetIds.add(item.datasetId)
  }
  if (!datasets.some(item => item.isPrimary === 1)) {
    datasets[0].isPrimary = 1
  }
  formData.datasets = datasets.map((item, index) => ({
    id: item.id,
    datasetId: item.datasetId,
    isPrimary: item.isPrimary === 1 ? 1 : 0,
    sort: item.sort ?? index,
    usageRemark: item.usageRemark || null,
  }))
  formData.status = formData.status ?? 1
  return formData
}

function addDatasetBinding(value, updateValue) {
  const next = normalizeDatasetBindings(value)
  next.push({
    __key: `dataset-${Date.now()}`,
    datasetId: null,
    isPrimary: next.length === 0 ? 1 : 0,
    sort: next.length,
    usageRemark: '',
  })
  updateValue(next)
}

function getDatasetBindingRows(value) {
  const rows = normalizeDatasetBindings(value)
  rows.forEach((item, index) => {
    item.__owner = rows
    item.__index = index
  })
  return rows
}

function removeDatasetBinding(rowIndex, value, updateValue) {
  const next = normalizeDatasetBindings(value).filter((_, index) => index !== rowIndex)
  if (next.length && !next.some(item => item.isPrimary === 1)) {
    next[0].isPrimary = 1
  }
  updateValue(cleanDatasetBindings(next))
}

function updateDatasetBinding(row, key, value, updateValue) {
  row[key] = value
  updateValue(cleanDatasetBindings(row.__owner))
}

function markPrimaryDataset(row, updateValue) {
  row.__owner.forEach(item => {
    item.isPrimary = item.__key === row.__key ? 1 : 0
  })
  updateValue(cleanDatasetBindings(row.__owner))
}

function cleanDatasetBindings(rows) {
  return rows.map(item => ({
    __key: item.__key,
    id: item.id,
    datasetId: item.datasetId,
    datasetName: item.datasetName,
    datasetCode: item.datasetCode,
    datasetType: item.datasetType,
    description: item.description,
    isPrimary: item.isPrimary,
    sort: item.sort,
    usageRemark: item.usageRemark,
  }))
}

function getDatasetSelectOptions(row) {
  if (!row?.datasetId || datasetOptionMap.value.has(row.datasetId)) {
    return datasetOptions.value
  }
  const label = `${row.datasetName || `数据集 ${row.datasetId}`}${row.datasetCode ? ` (${row.datasetCode})` : ''}`
  return [
    {
      label,
      value: row.datasetId,
      datasetName: row.datasetName,
      datasetCode: row.datasetCode,
      datasetType: row.datasetType,
    },
    ...datasetOptions.value,
  ]
}

function renderDatasetSummary(row) {
  const names = String(row.datasetNames || '')
    .split('、')
    .map(item => item.trim())
    .filter(Boolean)
  if (!names.length) {
    return h('span', { class: 'dataset-empty' }, `${row.datasetCount || 0} 个数据集`)
  }
  return h('div', { class: 'dataset-summary' }, [
    h('div', { class: 'dataset-summary-count' }, `${row.datasetCount || names.length} 个数据集`),
    h('div', { class: 'dataset-summary-tags' }, names.slice(0, 3).map(name =>
      h(NTag, { size: 'small', bordered: false, type: 'info' }, { default: () => name }),
    ).concat(names.length > 3
      ? [h(NTag, { size: 'small', bordered: false }, { default: () => `+${names.length - 3}` })]
      : [])),
  ])
}

function datasetBindingColumns(updateValue) {
  return [
    {
      title: '数据集',
      key: 'datasetId',
      width: 280,
      render: row => h(NSelect, {
        value: row.datasetId,
        options: getDatasetSelectOptions(row),
        filterable: true,
        clearable: true,
        placeholder: '选择已发布数据集',
        onUpdateValue: value => updateDatasetBinding(row, 'datasetId', value, updateValue),
      }),
    },
    {
      title: '主数据集',
      key: 'isPrimary',
      width: 100,
      render: row => h(NSwitch, {
        value: row.isPrimary === 1,
        onUpdateValue: checked => {
          if (checked) {
            markPrimaryDataset(row, updateValue)
          }
        },
      }),
    },
    {
      title: '排序',
      key: 'sort',
      width: 150,
      render: row => h('div', { class: 'sort-editor' }, [
        h('span', { class: 'sort-index' }, `#${(row.sort ?? row.__index ?? 0) + 1}`),
        h(NInputNumber, {
          value: row.sort,
          min: 0,
          size: 'small',
          showButton: false,
          onUpdateValue: value => updateDatasetBinding(row, 'sort', value ?? 0, updateValue),
        }),
      ]),
    },
    {
      title: '用途说明',
      key: 'usageRemark',
      width: 360,
      render: row => h(NInput, {
        value: row.usageRemark,
        size: 'small',
        placeholder: getDatasetRemarkPlaceholder(row.datasetId),
        onUpdateValue: value => updateDatasetBinding(row, 'usageRemark', value, updateValue),
      }),
    },
    {
      title: '操作',
      key: 'action',
      width: 90,
      fixed: 'right',
      render: row => h(NButton, {
        quaternary: true,
        type: 'error',
        onClick: () => removeDatasetBinding(row.__index, row.__owner, updateValue),
      }, {
        default: () => '删除',
      }),
    },
  ]
}

function getDatasetRemarkPlaceholder(datasetId) {
  const option = datasetOptionMap.value.get(datasetId)
  if (!option) {
    return '例如：用于趋势、排行、概览指标或明细表格'
  }
  return `${option.datasetName} 的大屏使用场景`
}

function handleDeleteBusiness(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定删除业务定义“${row.businessName}”吗？删除后 report-ui 将不能再基于该业务定义生成大屏。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await deleteDataBusiness(row.id)
        if (res.code === 200) {
          window.$message?.success('删除成功')
          crudRef.value?.refresh()
        }
        else {
          window.$message?.error(res.msg || '删除失败')
        }
      }
      catch (error) {
        window.$message?.error(error?.message || '删除失败')
      }
    },
  })
}
</script>

<style scoped>
.business-studio {
  min-height: 100%;
  padding: 18px;
  background: linear-gradient(180deg, #f4f7fb 0%, #eef3f8 100%);
  color: #1f2937;
}

.business-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 0.9fr);
  gap: 18px;
  padding: 24px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.hero-kicker,
.panel-kicker {
  margin: 0 0 8px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0;
}

.hero-title {
  margin: 0;
  font-size: 30px;
  line-height: 1.25;
}

.hero-description {
  max-width: 720px;
  margin: 12px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.hero-stat-card {
  min-height: 112px;
  padding: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 8px;
  background: #f8fafc;
}

.hero-stat-label {
  color: #64748b;
  font-size: 12px;
}

.hero-stat-value {
  margin-top: 6px;
  color: #0f172a;
  font-size: 28px;
  font-weight: 700;
}

.hero-stat-note {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
}

.business-panel {
  margin-top: 18px;
  padding: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.05);
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-toolbar h3 {
  margin: 0;
  font-size: 18px;
}

.toolbar-actions {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 140px auto auto auto;
  gap: 10px;
  align-items: center;
  min-width: min(760px, 100%);
}

.business-name-card {
  display: grid;
  gap: 5px;
}

.business-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.business-name {
  color: #0f172a;
  font-weight: 700;
}

.business-code {
  color: #0f766e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.business-desc {
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.dataset-summary {
  display: grid;
  gap: 6px;
}

.dataset-summary-count,
.dataset-empty {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.dataset-summary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.readiness-cell {
  display: grid;
  gap: 7px;
}

.readiness-cell-head {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.readiness-cell-head strong {
  font-size: 18px;
}

.readiness-cell-head span {
  color: #64748b;
  font-size: 12px;
}

.sort-editor {
  display: grid;
  grid-template-columns: 42px minmax(72px, 1fr);
  gap: 8px;
  align-items: center;
}

.sort-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 38px;
  height: 26px;
  border-radius: 6px;
  background: #e0f2f1;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.sort-editor :deep(.n-input-number) {
  width: 100%;
}

.sort-editor :deep(.n-input__input-el) {
  text-align: center;
  font-weight: 700;
}

.business-guide,
.dataset-binding-panel {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 8px;
  background: #f8fafc;
}

.business-guide {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.8fr);
  gap: 16px;
  padding: 18px;
}

.business-guide__eyebrow {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.business-guide__title {
  margin-top: 6px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.business-guide__desc {
  margin-top: 8px;
  color: #64748b;
  line-height: 1.6;
}

.business-guide__facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.guide-fact {
  padding: 12px;
  border-radius: 8px;
  background: #fff;
}

.guide-fact span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.guide-fact strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
}

.business-readiness-panel {
  grid-column: 1 / -1;
  display: grid;
  gap: 8px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: #fff;
}

.business-readiness-panel.is-high {
  border-color: rgba(15, 118, 110, 0.2);
  background: #f0fdfa;
}

.business-readiness-panel.is-medium {
  border-color: rgba(217, 119, 6, 0.22);
  background: #fffbeb;
}

.business-readiness-panel.is-low {
  border-color: rgba(220, 38, 38, 0.18);
  background: #fef2f2;
}

.business-readiness-main {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: #64748b;
}

.business-readiness-main strong {
  color: #0f172a;
}

.business-readiness-bar {
  height: 7px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(15, 23, 42, 0.08);
}

.business-readiness-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #0f766e, #22c55e);
}

.business-readiness-panel.is-medium .business-readiness-bar span {
  background: linear-gradient(90deg, #d97706, #facc15);
}

.business-readiness-panel.is-low .business-readiness-bar span {
  background: linear-gradient(90deg, #dc2626, #f97316);
}

.business-readiness-tip {
  color: #64748b;
  font-size: 12px;
}

.business-template-panel {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: minmax(180px, 0.34fr) minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.business-template-head {
  display: grid;
  gap: 4px;
}

.business-template-head span {
  color: #0f172a;
  font-weight: 700;
}

.business-template-head small {
  color: #64748b;
}

.business-template-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.dataset-binding-panel {
  padding: 16px;
}

.dataset-binding-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.dataset-binding-title {
  color: #0f172a;
  font-weight: 700;
}

.dataset-binding-desc {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .business-hero,
  .business-guide {
    grid-template-columns: 1fr;
  }

  .panel-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    grid-template-columns: 1fr 1fr;
    min-width: 0;
  }

  .business-template-panel {
    grid-template-columns: 1fr;
  }

  .business-template-list {
    justify-content: flex-start;
  }
}
</style>
