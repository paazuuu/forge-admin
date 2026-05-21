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
        <n-input
          v-model:value="modelKeyword"
          clearable
          size="small"
          placeholder="搜索当前领域模型"
          :disabled="!selectedDomain"
          @keyup.enter="loadModels"
          @clear="loadModels"
        />
        <n-spin :show="domainLoading || modelLoading">
          <div class="asset-tree">
            <div class="tree-action-row">
              <button type="button" class="tree-action" @click="openDomainEditor">
                <n-icon class="tree-icon">
                  <FolderOpenOutline />
                </n-icon>
                <strong>新建业务域</strong>
              </button>
              <n-button size="tiny" type="primary" @click="openDomainEditor">
                +
              </n-button>
            </div>
            <template v-for="domain in visibleDomains" :key="domain.id">
              <button
                type="button"
                class="tree-domain"
                :class="{ active: selectedDomainId === domain.id && !currentModel.id, disabled: domain.status === 'DISABLED' }"
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
              <template v-if="selectedDomainId === domain.id">
                <button
                  v-for="model in models"
                  :key="model.id"
                  type="button"
                  class="tree-model"
                  :class="{ active: currentModel.id === model.id }"
                  @click="openModel(model)"
                >
                  <span class="model-indent" />
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
                <button
                  v-if="canCreateModel"
                  type="button"
                  class="tree-model create"
                  @click="createModel"
                >
                  <span class="model-indent" />
                  <span class="tree-glyph">
                    <n-icon><AddCircleOutline /></n-icon>
                  </span>
                  <span class="node-main">
                    <strong>新建数据模型</strong>
                    <code>{{ domain.domainCode }}</code>
                  </span>
                </button>
                <n-empty v-if="!models.length && !modelLoading" size="small" description="该领域暂无模型" />
              </template>
            </template>
          </div>
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
          <n-button @click="loadModels">
            刷新
          </n-button>
          <n-button :loading="saving" type="primary" :disabled="!selectedDomain" @click="saveModel">
            保存模型
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

      <template v-else>
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
            <n-form-item label="模型编码">
              <n-input
                :value="currentModel.modelCode"
                placeholder="customer_archive"
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
            <n-form-item class="span-2" label="描述">
              <n-input
                v-model:value="currentModel.modelDesc"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 4 }"
                placeholder="描述模型承载的数据范围、口径和业务边界"
              />
            </n-form-item>
            <n-form-item label="数据表">
              <n-input
                :value="currentModel.modelSchema.tableName"
                placeholder="biz_customer_archive"
                @update:value="updateTableName"
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
                  placeholder="选择已导入的表模型"
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

        <LowcodeModelDesigner
          v-model="currentModel.modelSchema"
          :domain="selectedDomain"
          :data-models="models"
          :show-basic-tab="false"
          class="designer-panel"
        />
      </template>
    </main>
    <DomainEditorDrawer
      v-model:show="domainEditorVisible"
      :domain="null"
      :domains="domains"
      @saved="handleDomainSaved"
    />
  </div>
</template>

<script setup>
import {
  AddCircleOutline,
  AddOutline,
  ChevronDownOutline,
  ChevronForwardOutline,
  DocumentTextOutline,
  FolderOpenOutline,
  SearchOutline,
} from '@vicons/ionicons5'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  genTableColumnList,
  genTablePage,
  lowcodeCreateModel,
  lowcodeDomainDefaults,
  lowcodeDomainTree,
  lowcodeModelDetail,
  lowcodeModelPage,
  lowcodeUpdateModel,
} from '@/api/lowcode-crud'
import DomainEditorDrawer from '@/components/lowcode-builder/domain/DomainEditorDrawer.vue'
import LowcodeModelDesigner from '@/components/lowcode-builder/model/LowcodeModelDesigner.vue'
import {
  auditFieldNames,
  cloneSchema,
  createDefaultField,
  createDefaultModelSchema,
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
const domains = ref([])
const models = ref([])
const selectedDomainId = ref(null)
const selectedDomain = ref(null)
const domainKeyword = ref('')
const modelKeyword = ref('')
const domainEditorVisible = ref(false)
const expandedDomainIds = ref(new Set())

const currentModel = reactive(createEmptyModel())

const tableModels = ref([])
const tableModelLoading = ref(false)
const selectedTableModelId = ref(null)
const syncingTable = ref(false)
const tableModelOptions = computed(() => tableModels.value.map(item => ({
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

onMounted(async () => {
  await loadDomains()
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
  resetCurrentModel()
  await Promise.all([loadModels(), loadTableModels()])
}

async function loadTableModels() {
  tableModelLoading.value = true
  try {
    const res = await genTablePage({ pageNum: 1, pageSize: 200 })
    tableModels.value = res.data?.records || res.data?.list || res.data || []
  }
  catch (e) {
    tableModels.value = []
    console.warn('加载已有表模型失败:', e)
  }
  finally {
    tableModelLoading.value = false
  }
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
  resetCurrentModel()
}

async function openModel(model) {
  const res = await lowcodeModelDetail(model.id)
  applyModel(res.data)
}

async function saveModel() {
  if (!selectedDomain.value) {
    window.$message?.warning('请先选择业务领域')
    return
  }
  if (!currentModel.modelName) {
    window.$message?.warning('请填写模型名称')
    return
  }
  if (!currentModel.modelCode) {
    window.$message?.warning('请填写模型编码')
    return
  }
  syncModelIdentity()
  currentModel.modelSchema.fields = (currentModel.modelSchema.fields || []).filter(field => !isAuditField(field))
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
    }
    const res = currentModel.id ? await lowcodeUpdateModel(payload) : await lowcodeCreateModel(payload)
    currentModel.id = res.data || currentModel.id
    window.$message?.success('模型已保存')
    await loadModels()
  }
  catch (e) {
    window.$message?.error(e?.message || '保存模型失败')
  }
  finally {
    saving.value = false
  }
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
  syncModelIdentity()
}

function updateModelCode(value) {
  currentModel.modelCode = normalizeObjectCode(value)
  syncModelIdentity()
}

function updateTableName(value) {
  if (!currentModel.modelSchema)
    currentModel.modelSchema = createModelSchema(selectedDomain.value)
  currentModel.modelSchema.tableName = normalizeTableName(value)
}

function handleTableModelSelect(tableId) {
  selectedTableModelId.value = tableId
}

async function syncFromSelectedTableModel() {
  if (!selectedTableModelId.value)
    return
  const table = tableModels.value.find(item => item.tableId === selectedTableModelId.value)
  if (!table) {
    window.$message?.warning('未找到所选表模型')
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
    if (table.tableName)
      currentModel.modelSchema.tableName = normalizeTableName(table.tableName)
    if (!currentModel.modelDesc && table.tableComment)
      currentModel.modelDesc = table.tableComment
    currentModel.modelSchema.tableMode = 'EXISTING'
    currentModel.modelSchema.sourceTable = {
      tableId: table.tableId,
      tableName: table.tableName,
      tableComment: table.tableComment || '',
    }
    currentModel.modelSchema.fields = columns
      .filter(col => !auditFieldNames.includes(normalizeFieldName(col.javaField || col.columnName)))
      .map(col => mapTableColumnToField(col))
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
  const componentType = mapHtmlTypeToComponent(column.htmlType, dataType)
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
    sortable: false,
    remark: column.columnComment || '',
  }
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
  if (!currentModel.modelSchema.tableName && currentModel.modelCode)
    currentModel.modelSchema.tableName = normalizeTableName(`${selectedDomain.value?.tablePrefix || 'biz_'}${currentModel.modelCode}`)
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
    appType: domain?.defaultAppType || 'SINGLE',
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
  domainEditorVisible.value = true
}

async function handleDomainSaved() {
  domainEditorVisible.value = false
  await loadDomains()
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
  align-self: start;
  gap: 12px;
}

.nav-block,
.page-header,
.basic-panel,
.empty-panel {
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
}

.nav-block {
  display: grid;
  gap: 12px;
  padding: 12px;
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
  max-height: calc(100vh - 190px);
  gap: 4px;
  overflow: auto;
}

.tree-action-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 28px;
  gap: 6px;
  margin-bottom: 4px;
}

.tree-action,
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

.tree-action {
  grid-template-columns: 22px minmax(0, 1fr);
  border-color: #dbeafe;
  background: #f8fbff;
}

.tree-icon {
  color: #2563eb;
  font-size: 16px;
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

@media (max-width: 1160px) {
  .model-workbench-page {
    grid-template-columns: 1fr;
  }

  .model-nav {
    grid-template-columns: 1fr;
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
