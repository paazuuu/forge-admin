<template>
  <n-drawer v-model:show="drawerShow" :width="680" placement="right">
    <n-drawer-content :title="form.id ? '编辑业务领域' : '新增业务领域'">
      <n-form label-placement="left" label-width="104" size="small">
        <div class="form-section">
          <div class="section-title">
            基础信息
          </div>
          <n-form-item label="父级领域">
            <n-select
              v-model:value="form.parentId"
              clearable
              filterable
              :options="parentOptions"
              placeholder="不选则为顶级领域"
            />
          </n-form-item>
          <n-form-item label="领域编码">
            <n-input v-model:value="form.domainCode" :disabled="!!form.id" placeholder="sales_contract" />
          </n-form-item>
          <n-form-item label="领域名称">
            <n-input v-model:value="form.domainName" placeholder="销售合同域" />
          </n-form-item>
          <n-form-item label="领域说明">
            <n-input v-model:value="form.domainDesc" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
          </n-form-item>
          <n-form-item label="启用状态">
            <n-switch v-model:value="form.status" checked-value="ENABLED" unchecked-value="DISABLED" />
          </n-form-item>
        </div>

        <div class="form-section">
          <div class="section-title">
            默认规则
          </div>
          <div class="two-col">
            <n-form-item label="表名前缀">
              <n-input v-model:value="form.tablePrefix" placeholder="biz_contract_" />
            </n-form-item>
            <n-form-item label="配置键前缀">
              <n-input v-model:value="form.configKeyPrefix" placeholder="contract_" />
            </n-form-item>
            <n-form-item label="应用类型">
              <n-select v-model:value="form.defaultAppType" :options="appTypeOptions" />
            </n-form-item>
            <n-form-item label="页面模板">
              <n-input v-model:value="form.defaultLayoutType" placeholder="simple-crud" />
            </n-form-item>
            <n-form-item label="菜单父级">
              <MenuParentSelect v-model:value="form.menuParentId" />
            </n-form-item>
          </div>
        </div>

        <div class="form-section">
          <div class="section-title">
            AI 上下文
          </div>
          <n-form-item label="业务描述">
            <n-input v-model:value="schema.aiContext.description" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
          </n-form-item>
          <n-form-item label="业务术语">
            <n-input v-model:value="termsText" type="textarea" placeholder="每行一个术语" :autosize="{ minRows: 2, maxRows: 4 }" />
          </n-form-item>
          <n-form-item label="业务约束">
            <n-input v-model:value="constraintsText" type="textarea" placeholder="每行一条约束" :autosize="{ minRows: 2, maxRows: 4 }" />
          </n-form-item>
        </div>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="drawerShow = false">
            取消
          </n-button>
          <n-button type="primary" :loading="saving" @click="save">
            保存领域
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { lowcodeCreateDomain, lowcodeUpdateDomain } from '@/api/lowcode-crud'
import { cloneSchema } from '../model/model-schema'
import MenuParentSelect from '../shared/MenuParentSelect.vue'

defineOptions({ name: 'DomainEditorDrawer' })

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  domain: {
    type: Object,
    default: null,
  },
  domains: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:show', 'saved'])

const saving = ref(false)
const termsText = ref('')
const constraintsText = ref('')
const form = reactive(createBlankForm())
const schema = reactive(createBlankSchema())

const drawerShow = computed({
  get: () => props.show,
  set: value => emit('update:show', value),
})

const appTypeOptions = [
  { label: '单表应用', value: 'SINGLE' },
  { label: '树形单表', value: 'TREE' },
  { label: '主子表预留', value: 'MASTER_DETAIL' },
]

const parentOptions = computed(() => flattenDomains(props.domains)
  .filter(item => item.id !== form.id)
  .map(item => ({
    label: `${'  '.repeat(item.level)}${item.domainName} (${item.domainCode})`,
    value: item.id,
  })))

watch(
  () => props.show,
  (visible) => {
    if (visible)
      resetForm(props.domain)
  },
)

function createBlankForm() {
  return {
    id: null,
    parentId: null,
    domainCode: '',
    domainName: '',
    domainDesc: '',
    icon: '',
    sort: 0,
    status: 'ENABLED',
    menuParentId: null,
    tablePrefix: '',
    configKeyPrefix: '',
    defaultAppType: 'SINGLE',
    defaultLayoutType: 'simple-crud',
  }
}

function createBlankSchema() {
  return {
    aiContext: {
      description: '',
      terms: [],
      commonObjects: [],
      fieldNamingPreference: 'lowerCamel',
      constraints: [],
      generationNotes: [],
    },
    naming: {
      tablePrefix: '',
      configKeyPrefix: '',
      objectCodeStyle: 'lower_snake',
    },
    defaults: {
      appType: 'SINGLE',
      layoutType: 'simple-crud',
      menuParentId: null,
    },
    fieldTemplates: [],
    dictRecommendations: [],
    securityPolicies: [],
  }
}

function resetForm(domain) {
  Object.assign(form, createBlankForm(), domain || {})
  delete form.defaultTableMode
  const nextSchema = cloneSchema(domain?.domainSchema || createBlankSchema())
  if (nextSchema.defaults)
    delete nextSchema.defaults.tableMode
  if (nextSchema.naming)
    delete nextSchema.naming.tableMode
  Object.assign(schema, createBlankSchema(), nextSchema)
  termsText.value = (schema.aiContext.terms || []).join('\n')
  constraintsText.value = (schema.aiContext.constraints || []).join('\n')
}

async function save() {
  if (!form.domainCode || !form.domainName) {
    window.$message?.warning('请填写领域编码和领域名称')
    return
  }
  saving.value = true
  try {
    schema.aiContext.terms = splitLines(termsText.value)
    schema.aiContext.constraints = splitLines(constraintsText.value)
    schema.naming.tablePrefix = form.tablePrefix
    schema.naming.configKeyPrefix = form.configKeyPrefix
    schema.defaults.appType = form.defaultAppType
    schema.defaults.layoutType = form.defaultLayoutType
    schema.defaults.menuParentId = form.menuParentId

    const payload = {
      id: form.id,
      parentId: form.parentId || 0,
      domainCode: form.domainCode,
      domainName: form.domainName,
      domainDesc: form.domainDesc,
      icon: form.icon,
      sort: form.sort,
      status: form.status,
      menuParentId: form.menuParentId,
      tablePrefix: form.tablePrefix,
      configKeyPrefix: form.configKeyPrefix,
      defaultAppType: form.defaultAppType,
      defaultLayoutType: form.defaultLayoutType,
      domainSchema: cloneSchema(schema),
    }
    if (form.id)
      await lowcodeUpdateDomain(payload)
    else
      await lowcodeCreateDomain(payload)
    window.$message?.success('业务领域已保存')
    emit('saved')
    drawerShow.value = false
  }
  catch (e) {
    window.$message?.error(e?.message || '保存业务领域失败')
  }
  finally {
    saving.value = false
  }
}

function splitLines(value) {
  return String(value || '')
    .split(/\n|,/)
    .map(item => item.trim())
    .filter(Boolean)
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
</script>

<style scoped>
.form-section {
  border-bottom: 1px solid #eef2f7;
  margin-bottom: 16px;
  padding-bottom: 8px;
}

.section-title {
  margin-bottom: 12px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.two-col {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  column-gap: 12px;
}

@media (max-width: 720px) {
  .two-col {
    grid-template-columns: 1fr;
  }
}
</style>
