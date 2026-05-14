<template>
  <div class="connection-studio">
    <section class="studio-hero">
      <div class="hero-main">
        <p class="hero-kicker">
          Data Source Registry
        </p>
        <h1 class="hero-title">
          数据连接管理台
        </h1>
        <p class="hero-description">
          统一管理分析侧数据库连接、校验链路和元数据探查入口。连接资产在这里完成接入、验证与维护，再供数据集建模复用。
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

    <section class="connection-panel">
      <div class="panel-toolbar">
        <div>
          <p class="panel-kicker">
            Access Inventory
          </p>
          <h3>连接清单</h3>
        </div>
        <div class="toolbar-actions">
          <n-input
            v-model:value="queryForm.connectionName"
            clearable
            placeholder="搜索连接名称"
            @keydown.enter="applySearch"
          >
            <template #prefix>
              <i class="i-material-symbols:search-rounded" />
            </template>
          </n-input>
          <n-select
            v-model:value="queryForm.dbType"
            clearable
            placeholder="数据库类型"
            :options="dbTypeOptions"
          />
          <n-select
            v-model:value="queryForm.status"
            clearable
            placeholder="状态"
            :options="statusOptions"
          />
          <n-button type="primary" @click="applySearch">
            搜索
          </n-button>
          <n-button @click="handleReset">
            重置
          </n-button>
          <n-button type="primary" secondary @click="handleOpenAddConnection">
            新增连接
          </n-button>
        </div>
      </div>

      <AiCrudPage
        ref="crudRef"
        class="connection-crud"
        :api-config="{
          list: 'get@/data/connection/page',
          detail: 'get@/data/connection/:id',
          add: 'post@/data/connection',
          update: 'put@/data/connection',
          delete: 'delete@/data/connection/:id',
        }"
        :show-search="false"
        :hide-toolbar="true"
        :columns="tableColumns"
        :edit-schema="editSchema"
        :before-render-form="beforeRenderForm"
        :before-render-detail="beforeRenderDetail"
        :before-submit="beforeSubmit"
        row-key="id"
        :bordered="false"
        :striped="false"
        :scroll-x="1420"
        max-height="calc(100vh - 360px)"
        :edit-grid-cols="12"
        edit-label-placement="top"
        edit-form-class="data-connection-edit-form"
        modal-width="min(1120px, calc(100vw - 32px))"
        add-button-text="新增数据连接"
        @load-list-success="handleConnectionLoadSuccess"
        @modal-close="handleModalClose"
      >
        <template #form-connectionOverview="{ formData }">
          <div class="connection-guide-grid">
            <div class="connection-guide-card">
              <div class="guide-label">
                连接标识
              </div>
              <div class="guide-value">
                {{ formData.connectionName || '待命名连接' }}
              </div>
              <div class="guide-note">
                {{ formData.connectionCode || '建议使用业务域_环境 的编码方式' }}
              </div>
            </div>
            <div class="connection-guide-card">
              <div class="guide-label">
                数据库类型
              </div>
              <div class="guide-value">
                {{ getDbTypeLabel(formData.dbType) }}
              </div>
              <div class="guide-note">
                {{ getDriverShortName(formData.driverClassName) || '驱动将随数据库类型自动填充' }}
              </div>
            </div>
            <div class="connection-guide-card">
              <div class="guide-label">
                访问入口
              </div>
              <div class="guide-value">
                {{ getJdbcEndpoint(formData.jdbcUrl) || '待配置 JDBC 地址' }}
              </div>
              <div class="guide-note">
                {{ getJdbcDatabaseName(formData.jdbcUrl) || formData.schemaName || '可补充 schema 便于表探查' }}
              </div>
            </div>
            <div class="connection-guide-card">
              <div class="guide-label">
                当前状态
              </div>
              <div class="guide-value">
                {{ getStatusLabel(formData.status ?? 1) }}
              </div>
              <div class="guide-note">
                启用状态下才允许数据集继续引用该连接
              </div>
            </div>
          </div>
        </template>

        <template #form-passwordHint>
          <div class="connection-inline-tip">
            编辑已有连接时，密码留空会沿用原密码；若要校验当前草稿连接，请填写密码后执行“即时测试”。
          </div>
        </template>

        <template #form-connectionProbe="{ formData }">
          <div class="connection-probe-bar">
            <n-button
              type="primary"
              secondary
              :loading="connectionProbeLoading"
              @click="handleProbeConnection(formData)"
            >
              即时测试连接
            </n-button>
            <NText depth="3">
              {{ currentEditingConnection?.id && !formData.password ? '未填写密码时，将校验已保存连接。' : '草稿测试不会保存数据。' }}
            </NText>
          </div>
        </template>
      </AiCrudPage>
    </section>

    <n-modal
      v-model:show="tableModalVisible"
      preset="card"
      :title="tableModalTitle"
      style="width: 980px"
      :segmented="{ content: 'soft' }"
    >
      <div class="modal-toolbar">
        <div class="modal-toolbar-info">
          <div class="modal-toolbar-title">
            表清单
          </div>
          <div class="modal-toolbar-desc">
            快速检视当前连接下可用的数据表与注释信息
          </div>
        </div>
        <n-space>
          <n-input
            v-model:value="tableKeyword"
            clearable
            placeholder="按表名或注释搜索"
            style="width: 260px"
            @keyup.enter="loadConnectionTables"
          />
          <n-button type="primary" :loading="tableLoading" @click="loadConnectionTables">
            查询
          </n-button>
        </n-space>
      </div>
      <n-data-table
        :columns="connectionTableColumns"
        :data="connectionTables"
        :loading="tableLoading"
        :pagination="{ pageSize: 10 }"
        size="small"
        striped
      />
    </n-modal>

    <n-modal
      v-model:show="fieldModalVisible"
      preset="card"
      :title="fieldModalTitle"
      style="width: 900px"
      :segmented="{ content: 'soft' }"
    >
      <n-data-table
        :columns="fieldColumns"
        :data="fieldRows"
        :loading="fieldLoading"
        :pagination="{ pageSize: 10 }"
        size="small"
        striped
      />
    </n-modal>
  </div>
</template>

<script setup>
import { NTag, NText } from 'naive-ui'
import { computed, h, reactive, ref } from 'vue'
import {
  deleteDataConnection,
  getDataConnectionFields,
  getDataConnectionTables,
  testDataConnection,
  testDataConnectionTemp,
} from '@/api/data/connection'
import { AiCrudPage } from '@/components/ai-form'

defineOptions({ name: 'DataConnection' })

const crudRef = ref(null)
const tableModalVisible = ref(false)
const tableModalTitle = ref('数据表')
const tableLoading = ref(false)
const tableKeyword = ref('')
const connectionTables = ref([])
const currentConnection = ref(null)
const currentEditingConnection = ref(null)
const connectionProbeLoading = ref(false)
const fieldModalVisible = ref(false)
const fieldModalTitle = ref('字段列表')
const fieldLoading = ref(false)
const fieldRows = ref([])

const queryForm = reactive({
  connectionName: '',
  dbType: null,
  status: null,
})

const connectionStats = reactive({
  total: 0,
  enabled: 0,
  disabled: 0,
  engineKinds: 0,
})

const dbTypeOptions = [
  { label: 'MySQL', value: 'MYSQL' },
  { label: 'Oracle', value: 'ORACLE' },
  { label: 'PostgreSQL', value: 'POSTGRESQL' },
  { label: 'SQLServer', value: 'SQLSERVER' },
]

const driverClassMap = {
  MYSQL: 'com.mysql.cj.jdbc.Driver',
  ORACLE: 'oracle.jdbc.OracleDriver',
  POSTGRESQL: 'org.postgresql.Driver',
  SQLSERVER: 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
}

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

const statCards = computed(() => [
  {
    key: 'total',
    label: '筛选结果',
    value: connectionStats.total,
    note: '匹配当前搜索条件的数据连接总数',
  },
  {
    key: 'enabled',
    label: '当前页启用',
    value: connectionStats.enabled,
    note: '可供数据集建模继续引用的连接数',
  },
  {
    key: 'disabled',
    label: '当前页禁用',
    value: connectionStats.disabled,
    note: '已保留但暂不可继续消费的连接数',
  },
  {
    key: 'engines',
    label: '引擎覆盖',
    value: connectionStats.engineKinds,
    note: '当前页覆盖的数据库类型数量',
  },
])

const tableColumns = computed(() => [
  {
    prop: 'connectionName',
    label: '连接资产',
    width: 300,
    render: row => h('div', { class: 'connection-name-card' }, [
      h('div', { class: 'connection-name-row' }, [
        h('div', { class: 'connection-name' }, row.connectionName),
        h(NTag, {
          size: 'small',
          bordered: false,
          type: getDbTypeTagType(row.dbType),
        }, { default: () => getDbTypeLabel(row.dbType) }),
      ]),
      h('div', { class: 'connection-code' }, row.connectionCode),
      h('div', { class: 'connection-desc' }, row.description || '暂无描述'),
    ]),
  },
  {
    prop: 'driverClassName',
    label: '驱动与模式',
    width: 250,
    render: row => h('div', { class: 'connection-meta-card' }, [
      h('div', { class: 'connection-meta-primary' }, getDriverShortName(row.driverClassName) || '未设置驱动'),
      h('div', { class: 'connection-meta-secondary' }, row.driverClassName || '待自动填充'),
      h('div', { class: 'connection-meta-tertiary' }, row.schemaName || '未指定 schema'),
    ]),
  },
  {
    prop: 'jdbcUrl',
    label: '访问入口',
    width: 320,
    render: row => h('div', { class: 'connection-endpoint-card' }, [
      h('div', { class: 'connection-endpoint-primary' }, getJdbcEndpoint(row.jdbcUrl) || '待配置地址'),
      h('div', { class: 'connection-endpoint-secondary' }, getJdbcDatabaseName(row.jdbcUrl) || row.jdbcUrl || '-'),
      h('div', { class: 'connection-endpoint-tertiary' }, `账号：${row.username || '-'}`),
    ]),
  },
  {
    prop: 'status',
    label: '状态',
    width: 110,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: row.status === 1 ? 'success' : 'default',
    }, { default: () => getStatusLabel(row.status) }),
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    width: 180,
  },
  {
    prop: 'action',
    label: '操作',
    width: 260,
    fixed: 'right',
    maxActionButtons: 4,
    actions: [
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '测试连接', key: 'test', type: 'info', onClick: handleTest },
      { label: '查看表', key: 'tables', type: 'info', onClick: handleViewTables },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

const connectionTableColumns = [
  {
    title: '数据表',
    key: 'tableName',
    width: 300,
    render: row => h('div', { class: 'table-name-card' }, [
      h('div', { class: 'table-name' }, row.tableName),
      h('div', { class: 'table-comment' }, row.tableComment || '暂无注释'),
    ]),
  },
  {
    title: '表类型',
    key: 'tableType',
    width: 120,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: 'info',
    }, { default: () => row.tableType || 'TABLE' }),
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: row => h('a', {
      class: 'text-primary cursor-pointer hover:text-primary-hover',
      onClick: () => handleViewFields(row),
    }, '查看字段'),
  },
]

const fieldColumns = [
  { title: '字段名', key: 'columnName', width: 200 },
  { title: '字段类型', key: 'columnType', width: 180 },
  { title: '字段注释', key: 'columnComment' },
  {
    title: '可空',
    key: 'nullable',
    width: 90,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: row.nullable ? 'default' : 'warning',
    }, { default: () => row.nullable ? '是' : '否' }),
  },
  {
    title: '主键',
    key: 'primaryKey',
    width: 90,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: row.primaryKey ? 'success' : 'default',
    }, { default: () => row.primaryKey ? '是' : '否' }),
  },
]

const editSchema = computed(() => [
  {
    field: 'connectionOverview',
    label: '',
    type: 'slot',
    slotName: 'connectionOverview',
    span: 12,
    showFeedback: false,
  },
  {
    field: '__sectionIdentity',
    label: '连接标识',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'connection-form-divider' },
  },
  {
    field: 'connectionCode',
    label: '连接编码',
    type: 'input',
    span: 3,
    rules: [{ required: true, message: '请输入连接编码', trigger: 'blur' }],
    props: { placeholder: '请输入连接编码' },
  },
  {
    field: 'connectionName',
    label: '连接名称',
    type: 'input',
    span: 4,
    rules: [{ required: true, message: '请输入连接名称', trigger: 'blur' }],
    props: { placeholder: '请输入连接名称' },
  },
  {
    field: 'dbType',
    label: '数据库类型',
    type: 'select',
    span: 3,
    defaultValue: 'MYSQL',
    rules: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
    props: {
      placeholder: '请选择数据库类型',
      options: dbTypeOptions,
      onUpdateValue: (value, formData) => {
        if (driverClassMap[value]) {
          formData.driverClassName = driverClassMap[value]
        }
      },
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    span: 2,
    defaultValue: 1,
    props: { options: statusOptions },
  },
  {
    field: '__sectionAccess',
    label: '访问配置',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'connection-form-divider' },
  },
  {
    field: 'jdbcUrl',
    label: 'JDBC连接地址',
    type: 'input',
    span: 12,
    rules: [{ required: true, message: '请输入JDBC连接地址', trigger: 'blur' }],
    props: { placeholder: 'jdbc:mysql://localhost:3306/database' },
  },
  {
    field: 'driverClassName',
    label: '驱动类名',
    type: 'input',
    span: 5,
    defaultValue: 'com.mysql.cj.jdbc.Driver',
    rules: [{ required: true, message: '请输入驱动类名', trigger: 'blur' }],
    props: { placeholder: '请输入驱动类名' },
  },
  {
    field: 'schemaName',
    label: '模式名',
    type: 'input',
    span: 3,
    props: { placeholder: '数据库名/模式名' },
  },
  {
    field: 'username',
    label: '用户名',
    type: 'input',
    span: 2,
    rules: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    props: { placeholder: '请输入用户名' },
  },
  {
    field: 'password',
    label: '密码',
    type: 'input',
    span: 2,
    props: { type: 'password', placeholder: '编辑时留空则不修改', showPasswordOn: 'click' },
  },
  {
    field: 'passwordHint',
    label: '',
    type: 'slot',
    slotName: 'passwordHint',
    span: 12,
    showFeedback: false,
  },
  {
    field: '__sectionVerify',
    label: '验证与观测',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'connection-form-divider' },
  },
  {
    field: 'testSql',
    label: '测试SQL',
    type: 'input',
    span: 12,
    defaultValue: 'SELECT 1',
    props: { placeholder: '请输入测试SQL，如 SELECT 1' },
  },
  {
    field: 'connectionProbe',
    label: '',
    type: 'slot',
    slotName: 'connectionProbe',
    span: 12,
    showFeedback: false,
  },
  {
    field: '__sectionGovernance',
    label: '治理信息',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'connection-form-divider' },
  },
  {
    field: 'description',
    label: '描述',
    type: 'textarea',
    span: 12,
    props: { placeholder: '请输入描述', rows: 4 },
  },
])

function buildSearchParams() {
  return {
    connectionName: queryForm.connectionName?.trim() || undefined,
    dbType: queryForm.dbType || undefined,
    status: queryForm.status ?? undefined,
  }
}

function applySearch() {
  crudRef.value?.search(buildSearchParams())
}

function handleReset() {
  queryForm.connectionName = ''
  queryForm.dbType = null
  queryForm.status = null
  crudRef.value?.search({})
}

function handleConnectionLoadSuccess({ list, total }) {
  connectionStats.total = total || 0
  connectionStats.enabled = (list || []).filter(item => item.status === 1).length
  connectionStats.disabled = (list || []).filter(item => item.status !== 1).length
  connectionStats.engineKinds = new Set((list || []).map(item => item.dbType).filter(Boolean)).size
}

function handleOpenAddConnection() {
  currentEditingConnection.value = null
  crudRef.value?.showAdd()
}

function beforeRenderForm(formData) {
  currentEditingConnection.value = null
  return {
    dbType: 'MYSQL',
    driverClassName: driverClassMap.MYSQL,
    status: 1,
    testSql: 'SELECT 1',
    ...(formData || {}),
  }
}

function beforeRenderDetail(detailData) {
  currentEditingConnection.value = detailData ? { ...detailData } : null
  return {
    ...(detailData || {}),
    password: '',
  }
}

function beforeSubmit(formData) {
  if (!formData.password) {
    delete formData.password
  }

  delete formData.connectionOverview
  delete formData.passwordHint
  delete formData.connectionProbe
  delete formData.__sectionIdentity
  delete formData.__sectionAccess
  delete formData.__sectionVerify
  delete formData.__sectionGovernance

  return formData
}

function handleModalClose() {
  currentEditingConnection.value = null
  connectionProbeLoading.value = false
}

function handleEdit(row) {
  currentEditingConnection.value = row
  crudRef.value?.showEdit({ ...row, password: '' })
}

function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除数据连接“${row.connectionName}”吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await deleteDataConnection(row.id)
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error('删除失败')
      }
    },
  })
}

async function handleTest(row) {
  try {
    window.$message.loading('正在测试连接...', { duration: 0, key: 'testConn' })
    const res = await testDataConnection(row.id)
    if (res.code === 200 && res.data) {
      window.$message.success('连接成功', { key: 'testConn' })
    }
    else {
      window.$message.error('连接失败', { key: 'testConn' })
    }
  }
  catch {
    window.$message.error('连接测试失败', { key: 'testConn' })
  }
}

async function handleProbeConnection(formData) {
  if (!formData.dbType || !formData.driverClassName || !formData.jdbcUrl || !formData.username) {
    window.$message?.warning('请先完善数据库类型、驱动、地址和用户名')
    return
  }

  connectionProbeLoading.value = true
  try {
    let res
    if (currentEditingConnection.value?.id && !formData.password) {
      res = await testDataConnection(currentEditingConnection.value.id)
    }
    else {
      if (!formData.password) {
        window.$message?.warning('请先输入密码后再测试草稿连接')
        return
      }
      res = await testDataConnectionTemp({
        dbType: formData.dbType,
        driverClassName: formData.driverClassName,
        jdbcUrl: formData.jdbcUrl,
        username: formData.username,
        password: formData.password,
        testSql: formData.testSql || 'SELECT 1',
      })
    }

    if (res.code === 200 && res.data) {
      window.$message?.success('连接测试通过')
    }
    else {
      window.$message?.error(res.msg || '连接测试失败')
    }
  }
  catch (error) {
    window.$message?.error(error?.message || '连接测试失败')
  }
  finally {
    connectionProbeLoading.value = false
  }
}

async function handleViewTables(row) {
  currentConnection.value = row
  tableModalTitle.value = `数据表 - ${row.connectionName}`
  tableKeyword.value = ''
  tableModalVisible.value = true
  await loadConnectionTables()
}

async function loadConnectionTables() {
  if (!currentConnection.value?.id) {
    return
  }

  tableLoading.value = true
  try {
    const res = await getDataConnectionTables(currentConnection.value.id, tableKeyword.value || undefined)
    if (res.code === 200) {
      connectionTables.value = res.data || []
    }
    else {
      window.$message.error(res.msg || '加载数据表失败')
    }
  }
  catch {
    window.$message.error('加载数据表失败')
  }
  finally {
    tableLoading.value = false
  }
}

async function handleViewFields(row) {
  if (!currentConnection.value?.id) {
    return
  }

  fieldModalTitle.value = `字段列表 - ${row.tableName}`
  fieldModalVisible.value = true
  fieldLoading.value = true
  fieldRows.value = []

  try {
    const res = await getDataConnectionFields(currentConnection.value.id, row.tableName)
    if (res.code === 200) {
      fieldRows.value = res.data || []
    }
    else {
      window.$message.error(res.msg || '加载字段失败')
    }
  }
  catch {
    window.$message.error('加载字段失败')
  }
  finally {
    fieldLoading.value = false
  }
}

function getDbTypeLabel(dbType) {
  return dbTypeOptions.find(item => item.value === dbType)?.label || dbType || '未设置'
}

function getDbTypeTagType(dbType) {
  if (dbType === 'MYSQL') {
    return 'success'
  }
  if (dbType === 'POSTGRESQL') {
    return 'info'
  }
  if (dbType === 'SQLSERVER') {
    return 'warning'
  }
  if (dbType === 'ORACLE') {
    return 'default'
  }
  return 'default'
}

function getStatusLabel(status) {
  return status === 1 ? '启用' : '禁用'
}

function getDriverShortName(driverClassName) {
  if (!driverClassName) {
    return ''
  }
  const parts = driverClassName.split('.')
  return parts[parts.length - 1] || driverClassName
}

function getJdbcEndpoint(jdbcUrl) {
  if (!jdbcUrl) {
    return ''
  }
  const match = jdbcUrl.match(/\/\/([^/?;]+)/)
  return match?.[1] || jdbcUrl
}

function getJdbcDatabaseName(jdbcUrl) {
  if (!jdbcUrl) {
    return ''
  }
  const slashMatch = jdbcUrl.match(/\/\/[^/]+\/([^?;]+)/)
  if (slashMatch?.[1]) {
    return slashMatch[1]
  }
  const serviceMatch = jdbcUrl.match(/@([^:/]+):\d+[:/]([^?;]+)/)
  return serviceMatch?.[2] || ''
}
</script>

<style scoped>
.connection-studio {
  --studio-bg: linear-gradient(180deg, #f5f7fb 0%, #eef4f8 100%);
  --panel-bg: rgb(255 255 255 / 94%);
  --panel-border: rgb(148 163 184 / 16%);
  --panel-shadow: 0 20px 56px rgb(15 23 42 / 9%);
  min-height: 100%;
  padding: 20px;
  background: var(--studio-bg);
}

.studio-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(420px, 0.85fr);
  gap: 18px;
  margin-bottom: 18px;
}

.hero-main,
.hero-stat-card,
.connection-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--panel-border);
  border-radius: 24px;
  background: var(--panel-bg);
  box-shadow: var(--panel-shadow);
  backdrop-filter: blur(16px);
}

.hero-main {
  padding: 30px;
  background:
    radial-gradient(circle at top left, rgb(14 165 233 / 14%), transparent 42%),
    radial-gradient(circle at 88% 24%, rgb(59 130 246 / 16%), transparent 28%),
    linear-gradient(135deg, rgb(255 255 255 / 98%), rgb(247 251 255 / 96%));
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
  line-height: 1.12;
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
  inset: auto -20px -28px auto;
  width: 104px;
  height: 104px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgb(14 165 233 / 14%), rgb(59 130 246 / 0%));
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

.connection-panel {
  padding: 20px;
}

.panel-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-toolbar h3 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
}

.toolbar-actions {
  display: grid;
  grid-template-columns: minmax(220px, 1.2fr) 150px 130px auto auto auto;
  gap: 12px;
  min-width: min(100%, 940px);
}

.connection-name-card,
.connection-meta-card,
.connection-endpoint-card {
  display: grid;
  gap: 6px;
}

.connection-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.connection-name {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.connection-code {
  color: #0f766e;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.connection-desc,
.connection-meta-secondary,
.connection-meta-tertiary,
.connection-endpoint-secondary,
.connection-endpoint-tertiary {
  color: #64748b;
  font-size: 12px;
}

.connection-meta-primary,
.connection-endpoint-primary {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.modal-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.modal-toolbar-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.modal-toolbar-desc {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.table-name-card {
  display: grid;
  gap: 4px;
}

.table-name {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.table-comment {
  color: #64748b;
  font-size: 12px;
}

:deep(.connection-crud .ai-crud-main) {
  background: transparent;
}

:deep(.connection-crud .ai-crud-table) {
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 22px;
  background: rgb(255 255 255 / 0.94);
  box-shadow: 0 16px 36px rgb(15 23 42 / 6%);
}

:deep(.connection-crud .n-data-table-th) {
  background: #f8fafc;
}

:deep(.connection-crud .n-data-table-tr:hover td) {
  background: #fbfdff;
}

:global(.data-connection-edit-form) {
  padding: 4px 2px 0;
}

:global(.data-connection-edit-form .connection-guide-grid) {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

:global(.data-connection-edit-form .connection-guide-card) {
  padding: 14px 16px;
  border: 1px solid #dbe8f5;
  border-radius: 16px;
  background: linear-gradient(180deg, #fbfdff 0%, #f6faff 100%);
}

:global(.data-connection-edit-form .guide-label) {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

:global(.data-connection-edit-form .guide-value) {
  margin-top: 10px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.4;
}

:global(.data-connection-edit-form .guide-note) {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

:global(.data-connection-edit-form .connection-inline-tip) {
  padding: 12px 14px;
  color: #475569;
  font-size: 12px;
  line-height: 1.8;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
}

:global(.data-connection-edit-form .connection-probe-bar) {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 38px;
  padding: 2px 0 4px;
}

:global(.data-connection-edit-form .n-form-item) {
  margin-bottom: 8px;
  padding: 12px;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #fff;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

:global(.data-connection-edit-form .n-form-item:hover) {
  border-color: #cbd8ea;
  box-shadow: 0 2px 8px rgb(15 23 42 / 4%);
}

:global(.data-connection-edit-form .n-form-item-blank) {
  width: 100%;
}

:global(.data-connection-edit-form .n-form-item-label) {
  min-height: 20px;
  margin-bottom: 7px;
  color: #475569;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.3;
}

:global(.data-connection-edit-form .connection-form-divider) {
  margin: 12px 0 8px;
  color: #64748b;
}

:global(.data-connection-edit-form .connection-form-divider::before),
:global(.data-connection-edit-form .connection-form-divider::after) {
  border-top-color: #dbe3ef;
}

:global(.data-connection-edit-form .connection-form-divider .n-divider__title) {
  color: #1e293b;
  font-size: 14px;
  font-weight: 600;
}

:global(.data-connection-edit-form .n-input),
:global(.data-connection-edit-form .n-select) {
  width: 100%;
}

@media (max-width: 1400px) {
  .studio-hero {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    min-width: 100%;
  }
}

@media (max-width: 960px) {
  .connection-studio {
    padding: 14px;
  }

  .hero-stats,
  :global(.data-connection-edit-form .connection-guide-grid),
  .toolbar-actions {
    grid-template-columns: 1fr;
  }

  .panel-toolbar,
  .modal-toolbar,
  :global(.data-connection-edit-form .connection-probe-bar) {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
