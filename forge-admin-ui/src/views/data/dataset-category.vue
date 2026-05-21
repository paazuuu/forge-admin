<template>
  <div class="dataset-category-page">
    <section class="category-hero">
      <div class="hero-main">
        <p class="hero-kicker">
          Taxonomy Configuration
        </p>
        <h1 class="hero-title">
          数据集分类管理
        </h1>
        <p class="hero-description">
          统一维护数据集分类编码、层级和状态，为数据集归档、筛选和后续业务分域提供稳定的分类体系。
        </p>
        <div class="hero-actions">
          <NButton @click="goToDatasetPage">
            查看数据集
          </NButton>
          <NButton quaternary @click="handleRefresh">
            刷新
          </NButton>
          <NButton type="primary" @click="handleAddRoot">
            新增根分类
          </NButton>
        </div>
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

    <section class="category-panel">
      <div class="panel-toolbar">
        <div>
          <p class="panel-kicker">
            Tree Workspace
          </p>
          <h3>分类树配置台</h3>
        </div>
        <div class="toolbar-actions">
          <n-input
            v-model:value="queryForm.keyword"
            clearable
            placeholder="搜索分类名称或编码"
            @keydown.enter="applyFilters"
          >
            <template #prefix>
              <i class="i-material-symbols:search-rounded" />
            </template>
          </n-input>
          <n-select
            v-model:value="queryForm.status"
            clearable
            placeholder="状态"
            :options="statusOptions"
          />
          <NButton type="primary" @click="applyFilters">
            筛选
          </NButton>
          <NButton @click="handleReset">
            重置
          </NButton>
          <NButton quaternary @click="toggleExpandAll">
            {{ expandAll ? '折叠到一级' : '展开全部' }}
          </NButton>
        </div>
      </div>

      <n-data-table
        class="category-table"
        :columns="tableColumns"
        :data="filteredCategoryTree"
        :loading="loading"
        :pagination="false"
        :row-key="row => row.id"
        :bordered="false"
        max-height="calc(100vh - 250px)"
        :expandable="{
          expandedRowKeys,
          onUpdateExpandedRowKeys: handleExpandedChange,
        }"
        striped
      />
    </section>

    <n-modal
      v-model:show="modalVisible"
      preset="card"
      :title="modalTitle"
      style="width: 560px"
      :mask-closable="false"
    >
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="top">
        <n-form-item label="父分类" path="parentId">
          <n-tree-select
            v-model:value="formData.parentId"
            clearable
            default-expand-all
            placeholder="不选择则创建为根分类"
            :options="categoryTreeSelectOptions"
          />
        </n-form-item>
        <n-form-item label="分类编码" path="categoryCode">
          <n-input v-model:value="formData.categoryCode" placeholder="如 sales_assets" />
        </n-form-item>
        <n-form-item label="分类名称" path="categoryName">
          <n-input v-model:value="formData.categoryName" placeholder="请输入分类名称" />
        </n-form-item>
        <div class="form-grid">
          <n-form-item label="排序" path="sortOrder">
            <n-input-number v-model:value="formData.sortOrder" :min="0" style="width: 100%" />
          </n-form-item>
          <n-form-item label="状态" path="status">
            <n-radio-group v-model:value="formData.status">
              <NSpace>
                <n-radio :value="1">
                  启用
                </n-radio>
                <n-radio :value="0">
                  禁用
                </n-radio>
              </NSpace>
            </n-radio-group>
          </n-form-item>
        </div>
        <n-form-item label="描述" path="description">
          <n-input
            v-model:value="formData.description"
            type="textarea"
            :rows="4"
            placeholder="描述当前分类的业务归属、适用范围或命名规则"
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <div class="modal-footer">
          <NButton @click="modalVisible = false">
            取消
          </NButton>
          <NButton type="primary" :loading="submitLoading" @click="handleSubmit">
            保存分类
          </NButton>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NButton, NSpace, NTag } from 'naive-ui'
import { computed, h, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  createDataDatasetCategory,
  deleteDataDatasetCategory,
  getDataDatasetCategoryTree,
  updateDataDatasetCategory,
} from '@/api/data/dataset'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'DataDatasetCategory' })

const { dict } = useDict('sys_enable_disable')

const router = useRouter()
const loading = ref(false)
const categoryTree = ref([])
const expandedRowKeys = ref([])
const expandAll = ref(false)
const modalVisible = ref(false)
const modalTitle = ref('新增分类')
const submitLoading = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const queryForm = reactive({
  keyword: '',
  status: null,
})

const formData = reactive({
  id: null,
  parentId: null,
  categoryCode: '',
  categoryName: '',
  sortOrder: 0,
  status: 1,
  description: '',
})

const statusOptions = computed(() => dict.value.sys_enable_disable || [])

const formRules = {
  categoryCode: {
    required: true,
    message: '请输入分类编码',
    trigger: ['blur', 'input'],
  },
  categoryName: {
    required: true,
    message: '请输入分类名称',
    trigger: ['blur', 'input'],
  },
}

const allCategoryNodes = computed(() => flattenTree(categoryTree.value))
const filteredCategoryTree = computed(() => filterCategoryTree(categoryTree.value, queryForm.keyword, queryForm.status))
const filteredCategoryNodes = computed(() => flattenTree(filteredCategoryTree.value))
const categoryTreeSelectOptions = computed(() => {
  const blockedIds = new Set()
  if (editingId.value) {
    const currentNode = findCategoryById(categoryTree.value, editingId.value)
    collectCategoryIds(currentNode, blockedIds)
  }
  return buildCategorySelectOptions(categoryTree.value, blockedIds)
})

const statCards = computed(() => [
  {
    key: 'total',
    label: '分类总数',
    value: allCategoryNodes.value.length,
    note: '当前租户下的全部数据集分类节点',
  },
  {
    key: 'active',
    label: '启用分类',
    value: allCategoryNodes.value.filter(item => item.status === 1).length,
    note: '可供数据集正常关联使用的分类数量',
  },
  {
    key: 'disabled',
    label: '停用分类',
    value: allCategoryNodes.value.filter(item => item.status !== 1).length,
    note: '已保留但暂不建议继续关联的数据分类',
  },
  {
    key: 'visible',
    label: '当前筛选',
    value: filteredCategoryNodes.value.length,
    note: '匹配当前条件并在树表中展示的分类节点',
  },
])

const tableColumns = computed(() => [
  {
    title: '分类名称',
    key: 'categoryName',
    width: 380,
    render: (row) => {
      const depth = Math.max((row.level || 1) - 1, 0)
      return h('div', {
        class: ['category-name-card', row.parentId ? 'is-child' : 'is-root'],
        style: {
          '--category-depth': depth,
          '--category-depth-px': `${depth * 28}px`,
          '--category-card-width': `calc(100% - ${depth * 28}px)`,
        },
      }, [
        h('div', { class: 'category-name-row' }, [
          h('div', { class: 'category-name' }, row.categoryName),
          row.children?.length
            ? h('span', { class: 'category-child-count' }, `${row.children.length} 个子类`)
            : null,
          h(NTag, {
            size: 'small',
            bordered: false,
            type: 'info',
          }, { default: () => `L${row.level || 1}` }),
        ]),
        h('div', { class: 'category-description' }, row.description || '暂无描述'),
      ])
    },
  },
  {
    title: '分类编码',
    key: 'categoryCode',
    width: 220,
    render: row => h('span', { class: 'category-code' }, row.categoryCode),
  },
  {
    title: '排序',
    key: 'sortOrder',
    width: 100,
  },
  {
    title: '状态',
    key: 'status',
    width: 120,
    render: row => h(DictTag, {
      dictType: 'sys_enable_disable',
      dictValue: String(row.status),
      size: 'small',
    }),
  },
  {
    title: '更新时间',
    key: 'updateTime',
    width: 170,
  },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    render: row => h(NSpace, { size: 10 }, {
      default: () => [
        h(NButton, {
          size: 'small',
          text: true,
          type: 'primary',
          onClick: () => handleEdit(row),
        }, { default: () => '编辑' }),
        h(NButton, {
          size: 'small',
          text: true,
          type: 'info',
          onClick: () => handleAddChild(row),
        }, { default: () => '新增子级' }),
        h(NButton, {
          size: 'small',
          text: true,
          type: 'error',
          onClick: () => handleDelete(row),
        }, { default: () => '删除' }),
      ],
    }),
  },
])

watch(filteredCategoryTree, () => {
  resetExpandedState()
}, { immediate: true })

loadCategoryTree()

async function loadCategoryTree() {
  loading.value = true
  try {
    const res = await getDataDatasetCategoryTree()
    if (res.code === 200) {
      categoryTree.value = Array.isArray(res.data) ? res.data : []
      return true
    }
    window.$message?.error(res.msg || '加载分类失败')
    return false
  }
  catch (error) {
    console.error('Failed to load dataset category tree', error)
    window.$message?.error('加载分类失败')
    return false
  }
  finally {
    loading.value = false
  }
}

function applyFilters() {
  resetExpandedState()
}

function handleReset() {
  queryForm.keyword = ''
  queryForm.status = null
  expandAll.value = false
  applyFilters()
}

function handleExpandedChange(keys) {
  expandedRowKeys.value = keys
  expandAll.value = filteredCategoryNodes.value.length > 0 && keys.length === filteredCategoryNodes.value.length
}

function goToDatasetPage() {
  router.push('/data/dataset')
}

async function handleRefresh() {
  const success = await loadCategoryTree()
  if (success) {
    resetExpandedState()
    window.$message?.success('分类树已刷新')
  }
}

function toggleExpandAll() {
  expandAll.value = !expandAll.value
  resetExpandedState()
}

function resetForm() {
  formData.id = null
  formData.parentId = null
  formData.categoryCode = ''
  formData.categoryName = ''
  formData.sortOrder = 0
  formData.status = 1
  formData.description = ''
  editingId.value = null
  formRef.value?.restoreValidation()
}

function handleAddRoot() {
  resetForm()
  modalTitle.value = '新增根分类'
  modalVisible.value = true
}

function handleAddChild(row) {
  resetForm()
  modalTitle.value = `新增子分类 - ${row.categoryName}`
  formData.parentId = row.id
  modalVisible.value = true
}

function handleEdit(row) {
  resetForm()
  modalTitle.value = `编辑分类 - ${row.categoryName}`
  editingId.value = row.id
  formData.id = row.id
  formData.parentId = row.parentId || null
  formData.categoryCode = row.categoryCode
  formData.categoryName = row.categoryName
  formData.sortOrder = row.sortOrder ?? 0
  formData.status = row.status ?? 1
  formData.description = row.description || ''
  modalVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    const payload = {
      id: formData.id || undefined,
      parentId: formData.parentId || null,
      categoryCode: formData.categoryCode?.trim(),
      categoryName: formData.categoryName?.trim(),
      sortOrder: formData.sortOrder ?? 0,
      status: formData.status ?? 1,
      description: formData.description?.trim() || null,
    }
    const res = editingId.value
      ? await updateDataDatasetCategory(payload)
      : await createDataDatasetCategory(payload)

    if (res.code === 200) {
      window.$message?.success(editingId.value ? '分类更新成功' : '分类创建成功')
      modalVisible.value = false
      await loadCategoryTree()
    }
    else {
      window.$message?.error(res.msg || '分类保存失败')
    }
  }
  catch (error) {
    if (Array.isArray(error)) {
      return
    }
    window.$message?.error(error?.message || '分类保存失败')
  }
  finally {
    submitLoading.value = false
  }
}

function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除分类',
    content: `确定删除分类“${row.categoryName}”吗？存在子分类或已关联数据集时将无法删除。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await deleteDataDatasetCategory(row.id)
        if (res.code === 200) {
          window.$message?.success('分类删除成功')
          await loadCategoryTree()
        }
      }
      catch (error) {
        window.$message?.error(error?.message || '分类删除失败')
      }
    },
  })
}

function buildCategorySelectOptions(tree, blockedIds = new Set()) {
  return (tree || [])
    .filter(item => !blockedIds.has(item.id))
    .map(item => ({
      label: item.status === 1 ? item.categoryName : `${item.categoryName}（停用）`,
      value: item.id,
      key: item.id,
      children: item.children?.length ? buildCategorySelectOptions(item.children, blockedIds) : undefined,
    }))
}

function filterCategoryTree(tree, keyword, status) {
  const normalizedKeyword = keyword?.trim().toLowerCase()

  return (tree || [])
    .map((item) => {
      const children = filterCategoryTree(item.children || [], keyword, status)
      const matchedKeyword = !normalizedKeyword
        || item.categoryName?.toLowerCase().includes(normalizedKeyword)
        || item.categoryCode?.toLowerCase().includes(normalizedKeyword)
      const matchedStatus = status === null || status === undefined || item.status === status
      const matchedSelf = matchedKeyword && matchedStatus

      if (!matchedSelf && children.length === 0) {
        return null
      }

      return {
        ...item,
        children,
      }
    })
    .filter(Boolean)
}

function flattenTree(tree) {
  return (tree || []).reduce((result, item) => {
    result.push(item)
    if (item.children?.length) {
      result.push(...flattenTree(item.children))
    }
    return result
  }, [])
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

function collectCategoryIds(node, target) {
  if (!node) {
    return
  }
  target.add(node.id)
  ;(node.children || []).forEach(child => collectCategoryIds(child, target))
}

function resetExpandedState() {
  const hasActiveFilter = Boolean(queryForm.keyword?.trim()) || queryForm.status !== null
  if (expandAll.value || hasActiveFilter) {
    expandedRowKeys.value = filteredCategoryNodes.value.map(item => item.id)
    return
  }
  expandedRowKeys.value = (filteredCategoryTree.value || []).map(item => item.id)
}
</script>

<style scoped>
.dataset-category-page {
  background: #f8fafc;
  min-height: 100%;
  padding: 10px;
}

.category-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(460px, 0.9fr);
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  padding: 12px 16px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.category-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 12px;
  background: #fff;
}

.hero-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  min-width: 0;
  column-gap: 12px;
}

.hero-kicker,
.hero-title,
.hero-description {
  grid-column: 1;
}

.hero-kicker,
.panel-kicker {
  margin: 0 0 4px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0;
  text-transform: none;
}

.hero-title {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.2;
  font-weight: 600;
}

.hero-description {
  overflow: hidden;
  max-width: 720px;
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-actions {
  grid-column: 2;
  grid-row: 1 / span 3;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 0;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.hero-stat-card {
  min-width: 0;
  padding: 8px 10px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 8px;
  background: #fff;
}

.hero-stat-label {
  overflow: hidden;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-stat-value {
  margin-top: 2px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 600;
  line-height: 1;
  font-family: 'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.category-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(460px, 0.9fr);
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  padding: 10px 14px;
  overflow: hidden;
  border: 1px solid var(--panel-border);
  border-radius: 16px;
  background:
    radial-gradient(circle at top left, rgb(20 184 166 / 12%), transparent 36%),
    radial-gradient(circle at 88% 20%, rgb(14 165 233 / 12%), transparent 24%),
    linear-gradient(135deg, rgb(255 255 255 / 98%), rgb(246 251 252 / 96%));
  box-shadow: var(--panel-shadow);
  backdrop-filter: blur(16px);
}

.category-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--panel-border);
  border-radius: 16px;
  background: var(--panel-bg);
  box-shadow: var(--panel-shadow);
  backdrop-filter: blur(16px);
}

.hero-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  min-width: 0;
  column-gap: 12px;
}

.hero-kicker,
.hero-title,
.hero-description {
  grid-column: 1;
}

.hero-kicker,
.panel-kicker {
  margin: 0 0 3px;
  color: #0f766e;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.hero-title {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.18;
}

.hero-description {
  overflow: hidden;
  max-width: 720px;
  margin: 4px 0 0;
  color: #475569;
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-actions {
  grid-column: 2;
  grid-row: 1 / span 3;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 0;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.hero-stat-card {
  min-width: 0;
  padding: 8px 10px;
  border: 1px solid rgb(148 163 184 / 14%);
  border-radius: 10px;
  background: rgb(255 255 255 / 78%);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 80%);
}

.hero-stat-card::after {
  display: none;
}

.hero-stat-label {
  overflow: hidden;
  color: #64748b;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.03em;
  text-overflow: ellipsis;
  text-transform: uppercase;
  white-space: nowrap;
}

.hero-stat-value {
  margin-top: 2px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}

.hero-stat-note {
  display: none;
}

.category-panel {
  padding: 10px;
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.panel-toolbar h3 {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
}

.toolbar-actions {
  display: grid;
  grid-template-columns: minmax(200px, 1.2fr) 120px auto auto auto;
  gap: 6px;
  min-width: min(100%, 740px);
}

.category-name-card {
  display: grid;
  gap: 3px;
  position: relative;
  width: var(--category-card-width, 100%);
  margin-left: var(--category-depth-px, 0);
  padding: 7px 12px 7px 14px;
  border-radius: 8px;
  background: transparent;
  border: 1px solid transparent;
}

.category-name-card::before {
  position: absolute;
  top: 6px;
  bottom: 6px;
  left: 0;
  width: 3px;
  border-radius: 999px;
  background: #e2e8f0;
  content: '';
  opacity: 0;
  transition:
    opacity 0.2s ease,
    background 0.2s ease;
}

.category-name-card.is-root::before {
  opacity: 1;
  background: var(--primary-color, #165dff);
}

.category-name-card.is-child::before {
  opacity: 1;
  background: #cbd5e1;
}

.category-name-card.is-root {
  background: color-mix(in srgb, var(--primary-color, #165dff) 4%, transparent);
}

.category-name-card.is-child {
  background: transparent;
}

.category-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 4px;
  flex-wrap: wrap;
}

.category-name {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.category-child-count {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 500;
  padding: 1px 6px;
  background: #f1f5f9;
  border-radius: 4px;
}

.category-description {
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.4;
  padding-left: 4px;
}

.category-code {
  color: #475569;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.02em;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.category-table) {
  overflow: hidden;
  border: 1px solid #e8ecf1;
  border-radius: 12px;
  background: #fff;
}

:deep(.category-table .n-data-table-th) {
  padding: 9px 14px;
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  background: #f8fafc;
  border-bottom: 1px solid #e8ecf1;
}

:deep(.category-table .n-data-table-td) {
  padding: 8px 14px;
  vertical-align: top;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.category-table .n-data-table-tr:nth-child(even) > .n-data-table-td) {
  background: rgb(248 250 252 / 60%);
}

:deep(.category-table .n-data-table-tr:hover .n-data-table-td) {
  background: rgb(241 245 249 / 80%);
}

:deep(.category-table .n-data-table-expand-icon) {
  margin-right: 6px;
  font-size: 15px;
  color: #94a3b8;
  transition:
    transform 0.2s ease,
    color 0.2s ease;
}

:deep(.category-table .n-data-table-expand-icon:hover) {
  color: #3b82f6;
}

:deep(.category-table .n-data-table-expand-trigger) {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
}

:deep(.category-table .n-data-table-expand-trigger:hover) {
  background: color-mix(in srgb, var(--primary-color, #165dff) 8%, transparent);
}

:deep(.category-table .n-data-table-expand-icon:hover) {
  color: var(--primary-color, #165dff);
}

@media (max-width: 1400px) {
  .category-hero {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .dataset-category-page {
    padding: 12px;
  }

  .toolbar-actions,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .hero-main {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    grid-column: 1;
    grid-row: auto;
    justify-content: flex-start;
    margin-top: 10px;
  }

  .hero-description {
    white-space: normal;
  }

  .hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel-toolbar {
    flex-direction: column;
  }
}
</style>
