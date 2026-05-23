<template>
  <div class="model-workbench-page">
    <aside class="model-nav">
      <section class="nav-block tree-block">
        <div class="nav-head">
          <div>
            <h2>业务域与数据模型</h2>
            <span>{{ flatDomains.length }} 个业务域</span>
          </div>
          <n-button size="small" type="primary" @click="openDomainEditor">
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
            新建业务域
          </n-button>
        </div>
        <n-input
          v-model:value="domainKeyword"
          clearable
          size="small"
          placeholder="搜索领域"
          @keyup.enter="loadDomains"
          @clear="loadDomains"
        >
          <template #prefix>
            <n-icon><SearchOutline /></n-icon>
          </template>
        </n-input>
        <n-spin :show="domainLoading">
          <div class="asset-tree">
            <template v-for="domain in visibleDomains" :key="domain.id">
              <button
                type="button"
                class="tree-domain"
                :class="{ active: selectedDomainId === domain.id, disabled: domain.status === 'DISABLED' }"
                :style="{ '--indent': domain.level }"
                @click="selectDomain(domain)"
              >
                <span class="node-indent" />
                <span
                  class="node-toggle"
                  :class="{ empty: !hasChildren(domain) }"
                  @click.stop="toggleExpanded(domain)"
                >
                  <n-icon v-if="hasChildren(domain)">
                    <ChevronDownOutline v-if="isExpanded(domain.id)" />
                    <ChevronForwardOutline v-else />
                  </n-icon>
                </span>
                <span class="tree-glyph">
                  <n-icon><FolderOpenOutline /></n-icon>
                </span>
                <span class="node-main">
                  <strong>{{ domain.domainName }}</strong>
                  <code>{{ domain.domainCode }}</code>
                </span>
                <n-tag
                  v-if="domain.status === 'DISABLED'"
                  size="small"
                  type="warning"
                  :bordered="false"
                >
                  停用
                </n-tag>
              </button>
            </template>
          </div>
        </n-spin>
      </section>

      <section class="nav-block model-list-block">
        <div class="nav-head compact">
          <div>
            <h2>当前领域数据模型</h2>
            <span>{{ selectedDomain?.domainName || '未选择业务域' }}</span>
          </div>
        </div>
        <n-input
          v-model:value="modelKeyword"
          clearable
          size="small"
          placeholder="搜索当前领域模型"
          :disabled="!selectedDomain"
          @keyup.enter="loadModels"
          @clear="loadModels"
        />
        <n-spin :show="modelLoading">
          <div v-if="models.length" class="model-list">
            <button
              v-for="model in models"
              :key="model.id"
              type="button"
              class="tree-model"
              :class="{ active: currentModel.id === model.id }"
              @click="openModel(model)"
            >
              <span class="tree-glyph">
                <n-icon><DocumentTextOutline /></n-icon>
              </span>
              <span class="node-main">
                <strong>{{ model.modelName }}</strong>
                <code>{{ model.modelCode }}</code>
              </span>
              <n-tag size="small" :type="model.status === 'ENABLED' ? 'success' : 'warning'" :bordered="false">
                {{ dict.lowcode_model_status?.find(d => d.value === model.status)?.label || '启用' }}
              </n-tag>
            </button>
          </div>
          <n-empty
            v-else
            size="small"
            :description="selectedDomain ? '该领域暂无模型' : '请先选择业务域'"
            class="model-empty"
          />
        </n-spin>
      </section>
    </aside>

    <main class="model-main">
      <header class="page-header">
        <div>
          <div class="header-kicker">
            数据模型设计
          </div>
          <h1>{{ currentModel.modelName || '新建数据模型' }}</h1>
          <p>{{ selectedDomain ? `${selectedDomain.domainName} / ${currentModel.modelCode || '未设置编码'}` : '先选择业务领域，再维护领域下的数据模型。' }}</p>
        </div>
        <n-space>
          <n-button :disabled="!canCreateModel" type="primary" secondary @click="createModel">
            新建模型
          </n-button>
          <n-button :disabled="!selectedDomain || !isEditingModel" @click="triggerModelImport">
            导入配置
          </n-button>
          <n-button :disabled="!selectedDomain || !isEditingModel || !currentModel.modelName" @click="exportCurrentModel">
            导出配置
          </n-button>
          <n-button @click="loadModels">
            刷新
          </n-button>
          <n-button :loading="ddlLoading" :disabled="!selectedDomain || !isEditingModel" @click="previewDdl">
            DDL 预览
          </n-button>
          <n-button :loading="saving" :disabled="!selectedDomain || !isEditingModel" @click="saveModel(false)">
            保存配置
          </n-button>
          <n-button :loading="syncSaving" type="primary" :disabled="!selectedDomain || !isEditingModel" @click="saveModel(true)">
            保存并同步表结构
          </n-button>
        </n-space>
      </header>

      <section v-if="!selectedDomain" class="empty-panel">
        <div class="empty-icon">
          <n-icon><FolderOpenOutline /></n-icon>
        </div>
        <h2>先选择业务领域</h2>
        <p>数据模型是领域资产，不等同于应用。一个业务领域下可以沉淀多个模型，后续创建应用时再选择一个或多个模型来设计页面。</p>
      </section>

      <section v-else-if="selectedDomain && !isEditingModel" class="domain-overview">
        <div class="overview-hero">
            <div>
              <div class="hero-code">
                {{ selectedDomain.domainCode }}
              </div>
              <h1>{{ selectedDomain.domainName }}</h1>
              <p>{{ selectedDomain.domainDesc || `${selectedDomain.domainName} 业务领域，管理该领域下的所有数据模型。` }}</p>
            </div>
            <div class="hero-actions">
              <div class="hero-metrics">
                <div class="hero-metric">
                  <strong>{{ models.length }}</strong>
                  <span>数据模型</span>
                </div>
                <div class="hero-metric">
                  <strong>{{ enabledModels.length }}</strong>
                  <span>启用模型</span>
                </div>
              </div>
              <n-button type="primary" ghost @click="editSelectedDomain">
                编辑领域
              </n-button>
            </div>
          </div>

          <LowcodeErDiagram
            title="领域 ER 图"
            :subtitle="`${selectedDomain.domainName} 下全部数据模型关系，可拖拽调整并下载 SVG`"
            :models="models"
            :download-file-name="`${selectedDomain.domainCode || 'domain'}-er.svg`"
            empty-text="该业务域暂无数据模型"
          />

          <div class="domain-model-list">
            <div class="model-list-head">
              <div>
                <div class="section-title">
                  领域数据模型
                </div>
                <p>当前领域下已维护的数据模型清单</p>
              </div>
              <n-button :disabled="!canCreateModel" type="primary" secondary @click="createModel">
                新建模型
              </n-button>
            </div>
            <div v-if="models.length" class="model-card-grid">
              <button
                v-for="model in models"
                :key="model.id"
                type="button"
                class="model-card"
                @click="openModel(model)"
              >
                <div class="model-card-head">
                  <strong>{{ model.modelName }}</strong>
                  <n-tag size="small" :type="model.status === 'ENABLED' ? 'success' : 'warning'" :bordered="false">
                    {{ dict.lowcode_model_status?.find(d => d.value === model.status)?.label || model.status }}
                  </n-tag>
                </div>
                <div class="model-card-body">
                  <code>{{ model.modelCode }}</code>
                  <span>{{ (model.modelSchema?.fields || []).length }} 个字段</span>
                  <span v-if="model.masterData">· 主数据</span>
                  <span v-if="model.tenantEnabled">· 多租户</span>
                </div>
                <div v-if="model.modelDesc" class="model-card-desc">
                  {{ model.modelDesc }}
                </div>
              </button>
            </div>
            <n-empty v-else size="small" description="该领域暂无数据模型" class="model-empty">
              <template #extra>
                <n-button type="primary" :disabled="!canCreateModel" @click="createModel">
                  新建模型
                </n-button>
              </template>
            </n-empty>
          </div>
        </section>

        <section v-else class="model-editor">
          <section class="basic-panel">
            <div class="section-title">
              模型基本信息
            </div>
            <n-form label-placement="top" size="small" class="basic-form" :show-feedback="false">
              <n-form-item label="模型名称">
                <n-input
                  v-model:value="currentModel.modelName"
                  placeholder="例如：客户档案"
                  @update:value="syncModelIdentity"
                />
              </n-form-item>
              <n-form-item label="模型编码 / 数据表名">
                <n-input
                  :value="currentModel.modelCode"
                  placeholder="tf_f_order"
                  @update:value="updateModelCode"
                />
              </n-form-item>
              <n-form-item label="所属业务域">
                <n-select
                  :value="currentModel.domainId"
                  :options="domainOptions"
                  disabled
                />
              </n-form-item>
              <n-form-item label="启用状态">
                <n-select v-model:value="currentModel.status" :options="statusOptions" />
              </n-form-item>
              <n-form-item label="多租户">
                <n-switch v-model:value="currentModel.tenantEnabled" />
              </n-form-item>
              <n-form-item label="主数据标识">
                <n-switch v-model:value="currentModel.masterData" />
              </n-form-item>
              <n-form-item class="span-2" label="绑定数据源">
                <n-select
                  v-model:value="selectedDatasourceId"
                  clearable
                  filterable
                  :loading="datasourceLoading"
                  :options="datasourceOptions"
                  placeholder="选择模型所属数据源"
                  @update:value="handleDatasourceChange"
                />
              </n-form-item>
              <n-form-item class="span-2" label="描述">
                <n-input
                  v-model:value="currentModel.modelDesc"
                  type="textarea"
                  :autosize="{ minRows: 2, maxRows: 4 }"
                  placeholder="描述模型承载的数据范围、口径和业务边界"
                />
              </n-form-item>
              <n-form-item class="span-2" label="同步已有表模型">
                <div class="table-sync-control">
                  <n-select
                    v-model:value="selectedTableModelId"
                    clearable
                    filterable
                    :loading="tableModelLoading"
                    :options="tableModelOptions"
                    :disabled="!selectedDatasourceId"
                    placeholder="先选数据源，再选择已导入的表模型"
                    @update:value="handleTableModelSelect"
                  />
                  <n-button
                    :loading="syncingTable"
                    :disabled="!selectedTableModelId"
                    @click="syncFromSelectedTableModel"
                  >
                    同步字段
                  </n-button>
                </div>
              </n-form-item>
            </n-form>
          </section>

          <section v-if="ddlPreview" class="ddl-panel">
            <div class="section-title">
              DDL 预览
            </div>
            <n-alert
              v-for="warning in ddlPreview.warnings"
              :key="warning"
              type="warning"
              :bordered="false"
              class="ddl-warning"
            >
              {{ warning }}
            </n-alert>
            <pre v-for="ddl in ddlPreview.ddlStatements" :key="ddl" class="ddl-code">{{ ddl }}</pre>
            <n-empty v-if="!ddlPreview.ddlStatements?.length" description="当前没有需要执行的 DDL" />
          </section>

          <LowcodeModelDesigner
            v-if="currentModel.modelSchema"
            v-model="currentModel.modelSchema"
            :domain="selectedDomain"
            :data-models="models"
            :show-basic-tab="false"
            class="designer-panel"
          />
        </section>
    </main>
    <DomainEditorDrawer
      v-model:show="domainEditorVisible"
      :domain="editingDomain"
      :domains="domains"
      @saved="handleDomainSaved"
    />
    <input
      ref="modelImportInputRef"
      class="hidden-file-input"
      type="file"
      accept="application/json,.json"
      @change="handleModelImportFile"
    >
  </div>
</template>

<script setup>
import {
  AddOutline,
  ChevronDownOutline,
  ChevronForwardOutline,
  DocumentTextOutline,
  FolderOpenOutline,
  SearchOutline,
} from '@vicons/ionicons5'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  genDatasourceEnabled,
  genTableColumnList,
  genTablePage,
  lowcodeCreateModel,
  lowcodeDdlPreview,
  lowcodeDomainDefaults,
  lowcodeDomainTree,
  lowcodeModelDetail,
  lowcodeModelPage,
  lowcodeUpdateModel,
} from '@/api/lowcode-crud'
import DomainEditorDrawer from '@/components/lowcode-builder/domain/DomainEditorDrawer.vue'
import LowcodeErDiagram from '@/components/lowcode-builder/model/LowcodeErDiagram.vue'
import LowcodeModelDesigner from '@/components/lowcode-builder/model/LowcodeModelDesigner.vue'
import {
  cloneSchema,
  createDefaultField,
  createDefaultModelSchema,
  ensureSystemFields,
  isAuditField,
  normalizeFieldName,
  normalizeObjectCode,
  normalizeTableName,
} from '@/components/lowcode-builder/model/model-schema'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'AiLowcodeModels' })

const { dict } = useDict('lowcode_model_status')

const domainLoading = ref(false)
const modelLoading = ref(false)
const saving = ref(false)
const syncSaving = ref(false)
const ddlLoading = ref(false)
const domains = ref([])
const models = ref([])
const selectedDomainId = ref(null)
const selectedDomain = ref(null)
const domainKeyword = ref('')
const modelKeyword = ref('')
const domainEditorVisible = ref(false)
const editingDomain = ref(null)
const expandedDomainIds = ref(new Set())
const modelImportInputRef = ref(null)

const currentModel = reactive(createEmptyModel())

const datasourceLoading = ref(false)
const datasources = ref([])
const selectedDatasourceId = ref(null)
const tableModels = ref([])
const tableModelLoading = ref(false)
const selectedTableModelId = ref(null)
const syncingTable = ref(false)
const ddlPreview = ref(null)
const isEditingModel = ref(false)
const datasourceOptions = computed(() => datasources.value.map(item => ({
  label: `${item.datasourceName}${item.isDefault === 1 ? '（默认）' : ''} / ${item.dbType || '-'}`,
  value: item.datasourceId,
})))
const datasourceMap = computed(() => new Map(datasources.value.map(item => [item.datasourceId, item])))
const filteredTableModels = computed(() => {
  if (!selectedDatasourceId.value)
    return []
  return tableModels.value.filter((item) => {
    if (item.datasourceId === null || item.datasourceId === undefined)
      return true
    return Number(item.datasourceId) === Number(selectedDatasourceId.value)
  })
})
const tableModelOptions = computed(() => filteredTableModels.value.map(item => ({
  label: `${item.tableComment || item.tableName}（${item.tableName}）`,
  value: item.tableId,
})))

const flatDomains = computed(() => flattenDomains(domains.value))
const visibleDomains = computed(() => flattenVisibleDomains(domains.value))
const domainOptions = computed(() => flatDomains.value.map(domain => ({
  label: domain.domainName,
  value: domain.id,
})))
const canCreateModel = computed(() => selectedDomain.value?.status === 'ENABLED')
const statusOptions = computed(() => dict.value.lowcode_model_status || [])
const enabledModels = computed(() => models.value.filter(model => model.status === 'ENABLED'))

onMounted(async () => {
  await Promise.all([loadDomains(), loadDatasources()])
})

watch(
  domains,
  (items) => {
    const next = new Set(expandedDomainIds.value)
    ;(items || []).forEach((domain) => {
      if (domain.children?.length)
        next.add(domain.id)
    })
    expandedDomainIds.value = next
  },
  { deep: true },
)

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

async function selectDomain(domain) {
  selectedDomainId.value = domain.id
  const res = await lowcodeDomainDefaults(domain.id)
  selectedDomain.value = res.data || domain
  clearModelState()
  isEditingModel.value = false
  await Promise.all([loadModels(), loadTableModels()])
}

async function loadDatasources() {
  datasourceLoading.value = true
  try {
    const res = await genDatasourceEnabled()
    datasources.value = res.data || []
    ensureDefaultDatasourceSelected()
  }
  catch (e) {
    datasources.value = []
    console.warn('加载数据源失败:', e)
  }
  finally {
    datasourceLoading.value = false
  }
}

function ensureDefaultDatasourceSelected() {
  if (selectedDatasourceId.value || !datasources.value.length)
    return
  const defaultDatasource = datasources.value.find(item => item.isDefault === 1) || datasources.value[0]
  selectedDatasourceId.value = defaultDatasource?.datasourceId || null
}

async function loadTableModels() {
  tableModelLoading.value = true
  try {
    const res = await genTablePage({ pageNum: 1, pageSize: 500 })
    tableModels.value = res.data?.records || res.data?.list || res.data || []
    if (selectedTableModelId.value && !filteredTableModels.value.some(item => item.tableId === selectedTableModelId.value))
      selectedTableModelId.value = null
  }
  catch (e) {
    tableModels.value = []
    console.warn('加载已有表模型失败:', e)
  }
  finally {
    tableModelLoading.value = false
  }
}

function handleDatasourceChange(datasourceId) {
  selectedDatasourceId.value = datasourceId || null
  if (selectedTableModelId.value && !filteredTableModels.value.some(item => item.tableId === selectedTableModelId.value))
    selectedTableModelId.value = null
  syncSourceTableDatasource()
}

async function loadModels() {
  if (!selectedDomainId.value) {
    models.value = []
    return
  }
  modelLoading.value = true
  try {
    const res = await lowcodeModelPage({
      pageNum: 1,
      pageSize: 100,
      domainId: selectedDomainId.value,
      keyword: modelKeyword.value || undefined,
    })
    models.value = res.data?.records || []
  }
  finally {
    modelLoading.value = false
  }
}

function createModel() {
  if (!canCreateModel.value) {
    window.$message?.warning('请先选择启用状态的业务领域')
    return
  }
  isEditingModel.value = true
  resetCurrentModel()
}

async function openModel(model) {
  isEditingModel.value = true
  const res = await lowcodeModelDetail(model.id)
  applyModel(res.data)
}

async function saveModel(syncDdl = false) {
  if (!selectedDomain.value) {
    window.$message?.warning('请先选择业务领域')
    return
  }
  if (!currentModel.modelSchema) {
    window.$message?.warning('模型配置无效，请新建或选择数据模型')
    return
  }
  if (!currentModel.modelName) {
    window.$message?.warning('请填写模型名称')
    return
  }
  if (!currentModel.modelCode) {
    window.$message?.warning('请填写模型编码 / 数据表名')
    return
  }
  if (syncDdl) {
    const confirmed = await confirmSyncDdl()
    if (!confirmed)
      return
  }
  syncModelIdentity()
  currentModel.modelSchema.fields = ensureSystemFields(currentModel.modelSchema.fields || [], true)
  if (syncDdl)
    syncSaving.value = true
  else
    saving.value = true
  try {
    const payload = {
      id: currentModel.id,
      domainId: currentModel.domainId,
      modelCode: currentModel.modelCode,
      modelName: currentModel.modelName,
      modelDesc: currentModel.modelDesc,
      status: currentModel.status,
      tenantEnabled: currentModel.tenantEnabled,
      masterData: currentModel.masterData,
      modelSchema: cloneSchema(currentModel.modelSchema),
      syncDdl,
      confirmSyncDdl: syncDdl,
    }
    const res = currentModel.id ? await lowcodeUpdateModel(payload) : await lowcodeCreateModel(payload)
    currentModel.id = res.data || currentModel.id
    window.$message?.success(syncDdl ? '模型已保存，表结构已同步' : '模型配置已保存')
    ddlPreview.value = null
    await loadModels()
  }
  catch (e) {
    window.$message?.error(e?.message || '保存模型失败')
  }
  finally {
    saving.value = false
    syncSaving.value = false
  }
}

function confirmSyncDdl() {
  return new Promise((resolve) => {
    if (!window.$dialog) {
      resolve(false)
      return
    }
    window.$dialog.warning({
      title: '确认同步表结构',
      content: '确认保存模型配置并同步表结构到数据库？已有表只会追加缺失业务字段。',
      positiveText: '确认同步',
      negativeText: '取消',
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false),
      onClose: () => resolve(false),
    })
  })
}

function triggerModelImport() {
  if (!selectedDomain.value) {
    window.$message?.warning('请先选择业务领域')
    return
  }
  modelImportInputRef.value?.click()
}

function exportCurrentModel() {
  syncModelIdentity()
  downloadJson({
    type: 'LOWCODE_MODEL_CONFIG',
    version: 1,
    exportedAt: new Date().toISOString(),
    model: {
      id: null,
      domainCode: currentModel.domainCode,
      domainName: currentModel.domainName,
      modelCode: currentModel.modelCode,
      modelName: currentModel.modelName,
      modelDesc: currentModel.modelDesc,
      status: currentModel.status,
      tenantEnabled: currentModel.tenantEnabled,
      masterData: currentModel.masterData,
      modelSchema: cloneSchema(currentModel.modelSchema),
    },
  }, `${currentModel.modelCode || 'model'}-model-config.json`)
}

async function handleModelImportFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file)
    return
  try {
    const payload = JSON.parse(await file.text())
    const model = payload.model || payload
    if (!model?.modelSchema) {
      window.$message?.warning('模型配置文件格式不正确')
      return
    }
    applyImportedModel(model)
    window.$message?.success('模型配置已导入，请检查后保存')
  }
  catch (e) {
    window.$message?.error(e?.message || '导入模型配置失败')
  }
}

function applyImportedModel(model) {
  isEditingModel.value = true
  const schema = cloneSchema(model.modelSchema || {})
  currentModel.id = null
  currentModel.domainId = selectedDomain.value?.id || null
  currentModel.domainCode = selectedDomain.value?.domainCode || ''
  currentModel.domainName = selectedDomain.value?.domainName || ''
  currentModel.modelCode = normalizeObjectCode(model.modelCode || schema.object?.code || schema.tableName || '')
  currentModel.modelName = model.modelName || schema.object?.name || schema.businessName || ''
  currentModel.modelDesc = model.modelDesc || schema.object?.description || ''
  currentModel.status = model.status || 'ENABLED'
  currentModel.tenantEnabled = model.tenantEnabled !== false
  currentModel.masterData = Boolean(model.masterData)
  currentModel.modelSchema = schema
  currentModel.modelSchema.fields = ensureSystemFields(currentModel.modelSchema.fields || [], currentModel.tenantEnabled)
  applySourceTableSelection(schema.sourceTable)
  syncModelIdentity()
  ddlPreview.value = null
}

async function previewDdl() {
  if (!selectedDomain.value) {
    window.$message?.warning('请先选择业务领域')
    return
  }
  if (!currentModel.modelSchema) {
    window.$message?.warning('模型配置无效，请新建或选择数据模型')
    return
  }
  syncModelIdentity()
  currentModel.modelSchema.fields = ensureSystemFields(currentModel.modelSchema.fields || [], true)
  ddlLoading.value = true
  try {
    const res = await lowcodeDdlPreview(cloneSchema(currentModel.modelSchema))
    ddlPreview.value = res.data
  }
  catch (e) {
    window.$message?.error(e?.message || 'DDL 预览失败')
  }
  finally {
    ddlLoading.value = false
  }
}

function clearModelState() {
  currentModel.id = null
  currentModel.domainId = selectedDomain.value?.id || null
  currentModel.domainCode = selectedDomain.value?.domainCode || ''
  currentModel.domainName = selectedDomain.value?.domainName || ''
  currentModel.modelCode = ''
  currentModel.modelName = ''
  currentModel.modelDesc = ''
  currentModel.status = 'ENABLED'
  currentModel.tenantEnabled = true
  currentModel.masterData = false
  currentModel.modelSchema = null
  selectedDatasourceId.value = null
  selectedTableModelId.value = null
  ddlPreview.value = null
}

function resetCurrentModel() {
  applyModel(createEmptyModel(selectedDomain.value))
}

function applyModel(model = {}) {
  currentModel.id = model.id || null
  currentModel.domainId = model.domainId || selectedDomain.value?.id || null
  currentModel.domainCode = model.domainCode || selectedDomain.value?.domainCode || ''
  currentModel.domainName = model.domainName || selectedDomain.value?.domainName || ''
  currentModel.modelCode = model.modelCode || ''
  currentModel.modelName = model.modelName || ''
  currentModel.modelDesc = model.modelDesc || ''
  currentModel.status = model.status || 'ENABLED'
  currentModel.tenantEnabled = model.tenantEnabled !== false
  currentModel.masterData = Boolean(model.masterData)
  currentModel.modelSchema = cloneSchema(model.modelSchema || createModelSchema(selectedDomain.value))
  applySourceTableSelection(currentModel.modelSchema.sourceTable)
  syncModelIdentity()
}

function updateModelCode(value) {
  currentModel.modelCode = normalizeObjectCode(value)
  syncTableNameFromModelCode()
  syncModelIdentity()
}

function syncTableNameFromModelCode() {
  if (!currentModel.modelSchema)
    currentModel.modelSchema = createModelSchema(selectedDomain.value)
  currentModel.modelSchema.tableName = normalizeTableName(currentModel.modelCode)
}

function handleTableModelSelect(tableId) {
  selectedTableModelId.value = tableId
  const table = tableModels.value.find(item => item.tableId === tableId)
  if (table?.datasourceId && Number(table.datasourceId) !== Number(selectedDatasourceId.value))
    selectedDatasourceId.value = table.datasourceId
}

function applySourceTableSelection(sourceTable = {}) {
  selectedDatasourceId.value = sourceTable?.datasourceId || null
  selectedTableModelId.value = sourceTable?.tableId || null
  ensureDefaultDatasourceSelected()
}

function syncSourceTableDatasource() {
  if (!currentModel.modelSchema)
    currentModel.modelSchema = createModelSchema(selectedDomain.value)
  if (!selectedDatasourceId.value || currentModel.modelSchema.tableMode !== 'EXISTING')
    return
  const currentSource = currentModel.modelSchema.sourceTable || {}
  currentModel.modelSchema.sourceTable = {
    ...currentSource,
    ...buildDatasourceRef(selectedDatasourceId.value),
  }
}

function buildSourceTableRef(table = {}) {
  return {
    tableId: table.tableId,
    tableName: table.tableName,
    tableComment: table.tableComment || '',
    ...buildDatasourceRef(table.datasourceId || selectedDatasourceId.value),
  }
}

function buildDatasourceRef(datasourceId) {
  const datasource = datasourceMap.value.get(datasourceId) || {}
  return {
    datasourceId: datasource.datasourceId || datasourceId || null,
    datasourceCode: datasource.datasourceCode || '',
    datasourceName: datasource.datasourceName || '',
    dbType: datasource.dbType || '',
  }
}

async function syncFromSelectedTableModel() {
  if (!selectedTableModelId.value)
    return
  const table = tableModels.value.find(item => item.tableId === selectedTableModelId.value)
  if (!table) {
    window.$message?.warning('未找到所选表模型')
    return
  }
  if (!selectedDatasourceId.value) {
    window.$message?.warning('请先选择数据源')
    return
  }
  syncingTable.value = true
  try {
    const res = await genTableColumnList(selectedTableModelId.value)
    const columns = Array.isArray(res.data) ? res.data : res.data?.records || []
    if (!columns.length) {
      window.$message?.warning('该表暂无字段')
      return
    }
    if (!currentModel.modelSchema)
      currentModel.modelSchema = createModelSchema(selectedDomain.value)
    if (table.tableName) {
      currentModel.modelCode = normalizeObjectCode(table.tableName)
      currentModel.modelSchema.tableName = normalizeTableName(table.tableName)
    }
    if (table.tableComment) {
      currentModel.modelName = table.tableComment
      currentModel.modelDesc = table.tableComment
    }
    currentModel.modelSchema.tableMode = 'EXISTING'
    currentModel.modelSchema.sourceTable = buildSourceTableRef(table)
    currentModel.modelSchema.fields = ensureSystemFields(
      columns.map(col => mapTableColumnToField(col)),
      true,
    )
    syncModelIdentity()
    window.$message?.success(`已同步 ${currentModel.modelSchema.fields.length} 个字段`)
  }
  catch (e) {
    window.$message?.error(e?.message || '同步表字段失败')
  }
  finally {
    syncingTable.value = false
  }
}

function mapTableColumnToField(column) {
  const fieldName = normalizeFieldName(column.javaField || column.columnName || 'field')
  const dataType = mapColumnDataType(column.columnType, column.javaType)
  const length = parseColumnLength(column.columnType, dataType)
  const componentType = inferBusinessComponent(column, fieldName, dataType) || mapHtmlTypeToComponent(column.htmlType, dataType)
  return {
    ...createDefaultField(fieldName, column.columnComment || fieldName),
    columnName: column.columnName || camelToSnakeFallback(fieldName),
    dataType,
    length,
    componentType,
    required: column.isRequired === 1 || column.isRequired === '1',
    searchable: column.isQuery === 1 || column.isQuery === '1',
    listVisible: column.isList === 1 || column.isList === '1',
    formVisible: (column.isInsert === 1 || column.isInsert === '1') || (column.isEdit === 1 || column.isEdit === '1'),
    queryType: column.queryType || 'like',
    dictType: column.dictType || '',
    sensitiveType: column.desensitizeType || 'NONE',
    primaryKey: column.isPk === 1 || column.isPk === '1',
    systemField: isAuditField({ field: fieldName, columnName: column.columnName }),
    readonly: isAuditField({ field: fieldName, columnName: column.columnName }),
    autoIncrement: (column.isPk === 1 || column.isPk === '1')
      && String(column.extra || column.columnExtra || '').toLowerCase().includes('auto_increment'),
    sortable: false,
    remark: column.columnComment || '',
  }
}

function inferBusinessComponent(column, fieldName, dataType) {
  const columnName = String(column.columnName || '').toLowerCase()
  const javaField = String(fieldName || column.javaField || '').toLowerCase()
  const comment = String(column.columnComment || '').toLowerCase()
  const text = `${columnName} ${javaField} ${comment}`
  const idLike = columnName.endsWith('_id') || javaField.endsWith('id') || ['int', 'bigint'].includes(dataType)
  if (column.dictType)
    return ''
  if (columnName === 'region_code' || javaField === 'regioncode')
    return 'regionTreeSelect'
  if (idLike && (['org_id', 'dept_id', 'department_id'].includes(columnName)
    || ['orgid', 'deptid', 'departmentid'].includes(javaField)
    || text.includes('组织id') || text.includes('部门id'))) {
    return 'orgTreeSelect'
  }
  if (idLike && (['user_id', 'owner_id', 'manager_id', 'assignee_id'].includes(columnName)
    || ['userid', 'ownerid', 'managerid', 'assigneeid'].includes(javaField)
    || text.includes('用户id') || text.includes('负责人id'))) {
    return 'userSelect'
  }
  if (text.includes('附件') || text.includes('文件'))
    return 'fileUpload'
  if (text.includes('图片') || text.includes('照片') || text.includes('头像'))
    return 'imageUpload'
  return ''
}

function mapColumnDataType(columnType, javaType) {
  const type = String(columnType || '').toLowerCase()
  if (type.startsWith('varchar') || type.startsWith('char'))
    return 'varchar'
  if (type.includes('text'))
    return 'text'
  if (type.startsWith('tinyint'))
    return 'tinyint'
  if (type.startsWith('bigint'))
    return 'bigint'
  if (type.startsWith('int') || type.startsWith('smallint') || type.startsWith('mediumint'))
    return 'int'
  if (type.startsWith('decimal') || type.startsWith('numeric') || type.startsWith('double') || type.startsWith('float'))
    return 'decimal'
  if (type.startsWith('datetime') || type.startsWith('timestamp'))
    return 'datetime'
  if (type.startsWith('date'))
    return 'date'
  if (javaType === 'Long' || javaType === 'Integer')
    return javaType === 'Long' ? 'bigint' : 'int'
  if (javaType === 'BigDecimal' || javaType === 'Double' || javaType === 'Float')
    return 'decimal'
  if (javaType === 'Date' || javaType === 'LocalDateTime')
    return 'datetime'
  if (javaType === 'LocalDate')
    return 'date'
  return 'varchar'
}

function parseColumnLength(columnType, dataType) {
  const match = String(columnType || '').match(/\((\d+)/)
  if (match)
    return Number(match[1])
  if (dataType === 'varchar')
    return 128
  if (dataType === 'text')
    return 0
  if (dataType === 'decimal')
    return 18
  return 0
}

function mapHtmlTypeToComponent(htmlType, dataType) {
  if (!htmlType) {
    if (['int', 'bigint', 'decimal'].includes(dataType))
      return 'number'
    if (dataType === 'date')
      return 'date'
    if (dataType === 'datetime')
      return 'datetime'
    if (dataType === 'tinyint')
      return 'switch'
    return 'input'
  }
  const map = {
    input: 'input',
    textarea: 'textarea',
    select: 'select',
    radio: 'radio',
    checkbox: 'checkbox',
    datetime: 'datetime',
    fileUpload: 'fileUpload',
    imageUpload: 'imageUpload',
    editor: 'textarea',
  }
  return map[htmlType] || 'input'
}

function camelToSnakeFallback(value) {
  return String(value || '')
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W/g, '_')
    .replace(/_+/g, '_')
    .toLowerCase()
}

function syncModelIdentity() {
  if (!currentModel.modelSchema)
    currentModel.modelSchema = createModelSchema(selectedDomain.value)
  if (!currentModel.modelSchema.object)
    currentModel.modelSchema.object = {}
  if (!currentModel.modelSchema.domain)
    currentModel.modelSchema.domain = {}
  currentModel.modelSchema.schemaVersion = 2
  currentModel.modelSchema.fields = ensureSystemFields(currentModel.modelSchema.fields || [], true)
  if (!currentModel.modelSchema.indexes)
    currentModel.modelSchema.indexes = []
  currentModel.modelSchema.domain = {
    id: selectedDomain.value?.id || currentModel.domainId,
    code: selectedDomain.value?.domainCode || currentModel.domainCode,
    name: selectedDomain.value?.domainName || currentModel.domainName,
  }
  currentModel.modelSchema.object = {
    ...currentModel.modelSchema.object,
    code: currentModel.modelCode,
    name: currentModel.modelName,
  }
  currentModel.modelSchema.businessName = currentModel.modelName
  currentModel.modelSchema.policies = {
    dataScope: 'TENANT',
    regionField: '',
    auditEnabled: true,
    primaryKeyStrategy: 'AUTO_INCREMENT',
    primaryKeyField: 'id',
    tenantField: 'tenantId',
    logicDeleteField: 'delFlag',
    ...(currentModel.modelSchema.policies || {}),
  }
  currentModel.modelSchema.policies.auditEnabled = true
  currentModel.modelSchema.policies.primaryKeyStrategy = 'AUTO_INCREMENT'
  currentModel.modelSchema.policies.primaryKeyField = 'id'
  currentModel.modelSchema.policies.tenantField = 'tenantId'
  currentModel.modelSchema.policies.logicDeleteField = 'delFlag'
  if (!currentModel.modelSchema.tableName && currentModel.modelCode)
    currentModel.modelSchema.tableName = normalizeTableName(currentModel.modelCode)
  if (selectedDatasourceId.value) {
    currentModel.modelSchema.sourceTable = {
      ...(currentModel.modelSchema.sourceTable || {}),
      ...buildDatasourceRef(selectedDatasourceId.value),
      tableName: currentModel.modelSchema.sourceTable?.tableName || currentModel.modelSchema.tableName || '',
      tableComment: currentModel.modelSchema.sourceTable?.tableComment || currentModel.modelName || '',
    }
  }
}

function createEmptyModel(domain = null) {
  return {
    id: null,
    domainId: domain?.id || null,
    domainCode: domain?.domainCode || '',
    domainName: domain?.domainName || '',
    modelCode: '',
    modelName: '',
    modelDesc: '',
    status: 'ENABLED',
    tenantEnabled: true,
    masterData: false,
    modelSchema: createModelSchema(domain),
  }
}

function createModelSchema(domain = null) {
  const modelSchema = createDefaultModelSchema({
    domain: domain
      ? {
          id: domain.id,
          code: domain.domainCode,
          name: domain.domainName,
        }
      : undefined,
    objectCode: '',
    objectName: '',
    tableName: '',
    appType: 'SINGLE',
  })
  modelSchema.object.code = ''
  modelSchema.object.name = ''
  modelSchema.businessName = ''
  modelSchema.tableName = ''
  return modelSchema
}

function flattenDomains(nodes, level = 0) {
  const result = []
  for (const node of nodes || []) {
    result.push({ ...node, level })
    if (node.children?.length)
      result.push(...flattenDomains(node.children, level + 1))
  }
  return result
}

function flattenVisibleDomains(nodes, level = 0) {
  const result = []
  for (const node of nodes || []) {
    result.push({ ...node, level })
    if (node.children?.length && isExpanded(node.id))
      result.push(...flattenVisibleDomains(node.children, level + 1))
  }
  return result
}

function hasChildren(domain) {
  return Boolean(domain?.children?.length)
}

function isExpanded(id) {
  return expandedDomainIds.value.has(id)
}

function toggleExpanded(domain) {
  if (!hasChildren(domain))
    return
  const next = new Set(expandedDomainIds.value)
  if (next.has(domain.id))
    next.delete(domain.id)
  else
    next.add(domain.id)
  expandedDomainIds.value = next
}

function openDomainEditor() {
  editingDomain.value = null
  domainEditorVisible.value = true
}

function editSelectedDomain() {
  if (!selectedDomain.value) {
    window.$message?.warning('请先选择业务领域')
    return
  }
  editingDomain.value = { ...selectedDomain.value }
  domainEditorVisible.value = true
}

async function handleDomainSaved() {
  domainEditorVisible.value = false
  editingDomain.value = null
  await loadDomains()
  if (selectedDomainId.value) {
    const res = await lowcodeDomainDefaults(selectedDomainId.value)
    selectedDomain.value = res.data || selectedDomain.value
  }
}

function downloadJson(payload, filename) {
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.model-workbench-page {
  display: grid;
  min-height: 100%;
  grid-template-columns: 304px minmax(0, 1fr);
  gap: 14px;
  padding: 14px;
  background: #f3f6fa;
}

.model-nav {
  display: grid;
  position: sticky;
  top: 14px;
  max-height: calc(100vh - 28px);
  min-height: 0;
  grid-template-rows: minmax(280px, 1fr) minmax(240px, 0.78fr);
  align-self: start;
  gap: 12px;
}

.nav-block,
.page-header,
.basic-panel,
.ddl-panel,
.empty-panel {
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
}

.nav-block {
  display: grid;
  min-height: 0;
  gap: 12px;
  padding: 12px;
  grid-template-rows: auto auto minmax(0, 1fr);
}

.nav-head,
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.nav-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
}

.nav-head span {
  color: #64748b;
  font-size: 12px;
}

.asset-tree {
  display: grid;
  min-height: 0;
  gap: 4px;
  overflow: auto;
  align-content: start;
}

.tree-domain,
.tree-model {
  display: grid;
  width: 100%;
  min-height: 38px;
  align-items: center;
  gap: 8px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  padding: 5px 8px;
  text-align: left;
}

.tree-domain {
  grid-template-columns: calc(var(--indent, 0) * 16px) 20px 22px minmax(0, 1fr) auto;
}

.tree-model {
  grid-template-columns: 26px 22px minmax(0, 1fr) auto;
}

.tree-domain:hover,
.tree-model:hover,
.tree-domain.active,
.tree-model.active {
  border-color: #93c5fd;
  background: #eff6ff;
}

.tree-model.create {
  border-style: dashed;
  color: #2563eb;
}

.model-list {
  display: grid;
  min-height: 0;
  gap: 4px;
  overflow-y: auto;
  align-content: start;
}

.model-list .tree-model {
  grid-template-columns: 22px minmax(0, 1fr) auto;
  border-color: #e5e7eb;
  background: #fbfdff;
}

.model-list .tree-model.active {
  border-color: #60a5fa;
  background: #eff6ff;
}

.nav-head.compact {
  align-items: flex-start;
}

.model-empty {
  padding: 20px 0;
}

.tree-domain.disabled {
  opacity: 0.68;
}

.node-indent,
.model-indent {
  display: block;
}

.node-toggle {
  display: inline-flex;
  width: 20px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #64748b;
}

.node-toggle:not(.empty):hover {
  background: #e0ecff;
  color: #2563eb;
}

.node-toggle.empty {
  cursor: default;
}

.model-indent {
  width: 26px;
  border-left: 1px solid #cbd5e1;
  justify-self: center;
  min-height: 24px;
}

.tree-glyph {
  display: inline-flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  font-size: 16px;
}

.node-main {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.node-main strong,
.node-main code {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-main strong {
  color: #0f172a;
  font-size: 13px;
}

.node-main code {
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.model-main {
  display: grid;
  min-width: 0;
  gap: 12px;
}

.page-header {
  padding: 14px;
}

.header-kicker {
  color: #2563eb;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
}

.page-header h1 {
  margin: 4px 0 0;
  color: #0f172a;
  font-size: 22px;
}

.page-header p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
}

.basic-panel {
  padding: 14px;
}

.ddl-panel {
  padding: 14px;
}

.section-title {
  margin-bottom: 12px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.basic-form {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px 12px;
}

.basic-form :deep(.n-form-item) {
  margin-bottom: 0;
}

.span-2 {
  grid-column: span 2;
}

.table-sync-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  width: 100%;
  align-items: center;
}

.hidden-file-input {
  display: none;
}

.ddl-warning {
  margin-bottom: 10px;
}

.ddl-code {
  max-height: 320px;
  overflow: auto;
  border-radius: 6px;
  background: #0f172a;
  color: #dbeafe;
  font-size: 12px;
  line-height: 1.6;
  padding: 12px;
  white-space: pre-wrap;
}

.designer-panel {
  min-height: 720px;
}

.empty-panel {
  display: grid;
  min-height: 520px;
  place-items: center;
  align-content: center;
  gap: 12px;
  padding: 40px;
  text-align: center;
}

.empty-icon {
  display: inline-flex;
  width: 60px;
  height: 60px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 28px;
}

.empty-panel h2 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
}

.empty-panel p {
  max-width: 560px;
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.domain-overview {
  display: grid;
  gap: 14px;
}

.overview-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.96), rgba(30, 41, 59, 0.92)), #0f172a;
  color: #f8fafc;
  padding: 18px;
}

.hero-code {
  color: #86efac;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
}

.overview-hero h1 {
  margin: 6px 0 0;
  color: #f8fafc;
  font-size: 22px;
  line-height: 1.25;
}

.overview-hero p {
  max-width: 720px;
  margin: 8px 0 0;
  color: #cbd5e1;
  font-size: 13px;
  line-height: 1.6;
}

.hero-metrics {
  display: flex;
  gap: 16px;
  flex-shrink: 0;
}

.hero-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  flex-shrink: 0;
}

.hero-metric {
  display: grid;
  gap: 3px;
  padding: 10px 18px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
  text-align: center;
}

.hero-metric strong {
  font-size: 26px;
  line-height: 1.1;
  color: #f8fafc;
}

.hero-metric span {
  color: #cbd5e1;
  font-size: 11px;
}

.domain-model-list {
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.model-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.model-list-head .section-title {
  margin-bottom: 3px;
}

.model-list-head p {
  margin: 0;
  color: #64748b;
  font-size: 12px;
}

.model-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.model-card {
  display: grid;
  gap: 8px;
  min-height: 78px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: inherit;
  cursor: pointer;
  padding: 12px;
  text-align: left;
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 180ms ease;
}

.model-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.model-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.model-card-head strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-card-body {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.model-card-body code {
  color: #0f172a;
  font-size: 11px;
}

.model-card-desc {
  overflow: hidden;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.5;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

@media (max-width: 1160px) {
  .model-workbench-page {
    grid-template-columns: 1fr;
  }

  .model-nav {
    grid-template-columns: 1fr;
    position: static;
    max-height: none;
    grid-template-rows: auto;
  }

  .asset-tree,
  .model-list {
    max-height: 320px;
  }

  .basic-form {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .model-workbench-page,
  .model-nav,
  .basic-form {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }
}
</style>
