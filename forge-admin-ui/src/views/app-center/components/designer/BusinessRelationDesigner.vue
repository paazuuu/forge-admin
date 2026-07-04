<template>
  <div class="business-relation-designer">
    <div class="relation-designer-head">
      <div>
        <h3>关系与级联</h3>
        <p>维护对象关系、字段联动和运行态过滤规则。</p>
      </div>
      <n-space size="small">
        <n-button size="small" secondary @click="loadRelations">
          刷新
        </n-button>
        <n-button v-if="activeTab === 'relations'" size="small" secondary @click="addRelation">
          新增关系
        </n-button>
        <n-button v-if="activeTab === 'linkage'" size="small" secondary @click="addLinkageRule">
          新增级联
        </n-button>
        <n-button size="small" type="primary" :loading="saving" @click="saveRelations">
          保存配置
        </n-button>
      </n-space>
    </div>

    <n-tabs v-model:value="activeTab" type="line" class="relation-tabs">
      <n-tab-pane name="relations" tab="对象关系">
        <div class="relation-designer-body">
          <main class="relation-list-pane">
            <n-spin :show="loading">
              <div v-if="localRelations.length" class="relation-card-list">
                <section v-for="(relation, index) in localRelations" :key="relation.clientKey" class="relation-card">
                  <header class="relation-card-head">
                    <div>
                      <strong>{{ relation.relationName || relationLabel(relation) }}</strong>
                      <p>{{ relationSentence(relation) }}</p>
                    </div>
                    <n-space size="small">
                      <n-tag size="small" :type="relation.status === 0 ? 'default' : 'success'" :bordered="false">
                        {{ relation.status === 0 ? '停用' : '启用' }}
                      </n-tag>
                      <n-tag v-if="relation.inlineEditEnabled" size="small" type="info" :bordered="false">
                        编辑内嵌
                      </n-tag>
                      <n-tag v-if="relation.inlineCreateEnabled" size="small" type="success" :bordered="false">
                        新增内嵌
                      </n-tag>
                      <n-popconfirm @positive-click="removeRelation(index)">
                        <template #trigger>
                          <n-button quaternary circle size="small">
                            <template #icon>
                              <n-icon><TrashOutline /></n-icon>
                            </template>
                          </n-button>
                        </template>
                        确认移除该关系？
                      </n-popconfirm>
                    </n-space>
                  </header>

                  <n-form label-placement="top" :show-feedback="false" size="small" class="relation-form">
                    <n-grid :cols="3" :x-gap="12" :y-gap="4" responsive="screen">
                      <n-form-item-gi label="业务关系">
                        <n-select
                          v-model:value="relation.relationType"
                          :options="relationTypeOptions"
                          @update:value="value => updateRelationType(relation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="目标对象">
                        <n-select
                          v-model:value="relation.targetObjectCode"
                          :options="targetObjectOptions"
                          filterable
                          placeholder="选择目标对象"
                          @update:value="value => updateTargetObject(relation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="页面显示名称">
                        <n-input v-model:value="relation.relationName" placeholder="例如：客户有多个联系人" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi :label="sourceFieldLabel(relation)">
                        <n-select
                          v-model:value="relation.sourceFieldCode"
                          :options="sourceFieldOptions"
                          clearable
                          filterable
                          :placeholder="sourceFieldPlaceholder(relation)"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi :label="targetFieldLabel(relation)">
                        <n-select
                          v-model:value="relation.targetFieldCode"
                          :options="targetFieldOptions(relation)"
                          :loading="targetFieldLoadingMap[relation.targetObjectCode]"
                          clearable
                          filterable
                          :placeholder="targetFieldPlaceholder(relation)"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi :label="displayFieldLabel(relation)">
                        <n-select
                          v-model:value="relation.displayField"
                          :options="targetDisplayFieldOptions(relation)"
                          :loading="targetFieldLoadingMap[relation.targetObjectCode]"
                          clearable
                          filterable
                          placeholder="选择列表中显示的名称字段"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="详情页签">
                        <n-input v-model:value="relation.detailTabTitle" placeholder="例如：联系人" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi label="详情展示">
                        <n-switch
                          :value="relation.showInDetail !== false"
                          @update:value="value => updateShowInDetail(relation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="新增表单维护">
                        <n-switch
                          :value="relation.inlineCreateEnabled === true"
                          :disabled="!canInlineEdit(relation)"
                          @update:value="value => updateInlineCreate(relation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="编辑表单维护">
                        <n-switch
                          :value="relation.inlineEditEnabled === true"
                          :disabled="!canInlineEdit(relation)"
                          @update:value="value => updateInlineEdit(relation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="子表保存模式">
                        <n-select
                          v-model:value="relation.saveMode"
                          :options="childSaveModeOptions"
                          :disabled="!canInlineEdit(relation)"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="启用状态">
                        <n-switch
                          :value="relation.status !== 0"
                          @update:value="value => updateStatus(relation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="排序">
                        <n-input-number v-model:value="relation.sortOrder" :min="0" style="width: 100%" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi label="默认筛选">
                        <n-input v-model:value="relation.defaultFilter" placeholder="可选，运行态自动带入" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi v-if="canInlineEdit(relation)" :span="3" label="子表选择器">
                        <section class="relation-selector-config">
                          <div class="relation-selector-config-head">
                            <div>
                              <strong>{{ relation.selectorEnabled ? '已启用选择器按钮' : '未启用选择器按钮' }}</strong>
                              <span>从候选对象批量选择记录并按映射写入子表行。</span>
                            </div>
                            <n-switch
                              :value="relation.selectorEnabled === true"
                              @update:value="value => updateRelationSelectorEnabled(relation, value)"
                            />
                          </div>
                          <template v-if="relation.selectorEnabled">
                            <n-grid :cols="3" :x-gap="12" :y-gap="4" responsive="screen">
                              <n-form-item-gi label="候选对象">
                                <n-select
                                  v-model:value="relation.selectorObjectCode"
                                  :options="targetObjectOptions"
                                  clearable
                                  filterable
                                  placeholder="选择弹窗里查询的对象"
                                  @update:value="value => updateSelectorObject(relation, value)"
                                />
                              </n-form-item-gi>
                              <n-form-item-gi label="按钮文案">
                                <n-input v-model:value="relation.selectorButtonText" placeholder="选择记录" @update:value="markDirty" />
                              </n-form-item-gi>
                              <n-form-item-gi label="选择器标题">
                                <n-input v-model:value="relation.selectorTitle" placeholder="可为空" @update:value="markDirty" />
                              </n-form-item-gi>
                            </n-grid>
                            <div class="selector-preset-bar">
                              <div>
                                <strong>选择器配置</strong>
                                <span>选择字段后会自动生成弹窗列、查询条件和写入子表的映射。</span>
                              </div>
                              <n-button size="tiny" secondary @click="applySelectorDefaults(relation)">
                                智能补齐
                              </n-button>
                            </div>
                            <n-grid :cols="2" :x-gap="12" :y-gap="4" responsive="screen">
                              <n-form-item-gi label="弹窗展示字段">
                                <n-select
                                  v-model:value="relation.selectorDisplayFields"
                                  :options="selectorCandidateFieldOptions(relation)"
                                  :loading="targetFieldLoadingMap[selectorCandidateObjectCode(relation)]"
                                  multiple
                                  clearable
                                  filterable
                                  placeholder="选择用户在弹窗里看到的字段"
                                  @update:value="markDirty"
                                />
                              </n-form-item-gi>
                              <n-form-item-gi label="关键词搜索字段">
                                <n-select
                                  v-model:value="relation.selectorKeywordFields"
                                  :options="selectorCandidateFieldOptions(relation)"
                                  :loading="targetFieldLoadingMap[selectorCandidateObjectCode(relation)]"
                                  multiple
                                  clearable
                                  filterable
                                  placeholder="选择编号、名称等可搜索字段"
                                  @update:value="markDirty"
                                />
                              </n-form-item-gi>
                            </n-grid>
                            <section class="selector-structured-panel">
                              <div class="selector-structured-head">
                                <div>
                                  <strong>选中后写入子表</strong>
                                  <span>左侧是候选对象字段，右侧是子表字段。</span>
                                </div>
                                <n-button size="tiny" secondary @click="addSelectorMapping(relation)">
                                  <template #icon>
                                    <n-icon><AddOutline /></n-icon>
                                  </template>
                                  添加映射
                                </n-button>
                              </div>
                              <div v-if="relation.selectorMappings.length" class="selector-mapping-list">
                                <div v-for="(mapping, mappingIndex) in relation.selectorMappings" :key="mapping.clientKey" class="selector-mapping-row">
                                  <n-select
                                    v-model:value="mapping.sourceField"
                                    :options="selectorCandidateFieldOptions(relation, mapping.sourceField)"
                                    :loading="targetFieldLoadingMap[selectorCandidateObjectCode(relation)]"
                                    clearable
                                    filterable
                                    placeholder="候选字段"
                                    @update:value="markDirty"
                                  />
                                  <span class="selector-mapping-arrow">写入</span>
                                  <n-select
                                    v-model:value="mapping.targetField"
                                    :options="selectorTargetFieldOptions(relation, mapping.targetField)"
                                    :loading="targetFieldLoadingMap[relation.targetObjectCode]"
                                    clearable
                                    filterable
                                    placeholder="子表字段"
                                    @update:value="markDirty"
                                  />
                                  <n-button quaternary circle size="small" @click="removeSelectorMapping(relation, mappingIndex)">
                                    <template #icon>
                                      <n-icon><TrashOutline /></n-icon>
                                    </template>
                                  </n-button>
                                </div>
                              </div>
                              <n-empty v-else size="small" description="还没有字段映射，点击添加映射" />
                            </section>
                            <section class="selector-structured-panel">
                              <div class="selector-structured-head">
                                <div>
                                  <strong>筛选候选记录</strong>
                                  <span>用于按当前表单、记录或固定值过滤弹窗数据。</span>
                                </div>
                                <n-button size="tiny" secondary @click="addSelectorSearchParam(relation)">
                                  <template #icon>
                                    <n-icon><AddOutline /></n-icon>
                                  </template>
                                  添加筛选
                                </n-button>
                              </div>
                              <div v-if="relation.selectorSearchParams.length" class="selector-filter-list">
                                <div v-for="(param, paramIndex) in relation.selectorSearchParams" :key="param.clientKey" class="selector-filter-row">
                                  <n-select
                                    v-model:value="param.paramKey"
                                    :options="selectorCandidateFieldOptions(relation, param.paramKey)"
                                    :loading="targetFieldLoadingMap[selectorCandidateObjectCode(relation)]"
                                    clearable
                                    filterable
                                    placeholder="候选对象筛选字段"
                                    @update:value="markDirty"
                                  />
                                  <n-select
                                    v-model:value="param.sourceType"
                                    :options="selectorFilterSourceOptions"
                                    @update:value="value => updateSelectorSearchParamSource(param, value)"
                                  />
                                  <n-select
                                    v-if="param.sourceType !== 'static'"
                                    v-model:value="param.sourceField"
                                    :options="selectorContextFieldOptions(param)"
                                    clearable
                                    filterable
                                    placeholder="选择来源字段"
                                    @update:value="markDirty"
                                  />
                                  <n-input
                                    v-else
                                    v-model:value="param.staticValue"
                                    clearable
                                    placeholder="固定值"
                                    @update:value="markDirty"
                                  />
                                  <n-button quaternary circle size="small" @click="removeSelectorSearchParam(relation, paramIndex)">
                                    <template #icon>
                                      <n-icon><TrashOutline /></n-icon>
                                    </template>
                                  </n-button>
                                </div>
                              </div>
                              <n-empty v-else size="small" description="未设置筛选条件，弹窗展示全部候选记录" />
                            </section>
                          </template>
                        </section>
                      </n-form-item-gi>
                      <n-form-item-gi :span="3" label="说明">
                        <n-input
                          v-model:value="relation.description"
                          type="textarea"
                          :rows="3"
                          placeholder="描述这条业务关系的使用场景"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                    </n-grid>
                  </n-form>
                </section>
              </div>
              <n-empty v-else-if="!loading" description="暂无关系配置" />
            </n-spin>
          </main>

          <aside class="relation-summary-pane">
            <section>
              <h4>业务句子</h4>
              <p>关系会以业务语言进入详情页签和发布检查。</p>
              <div class="sentence-list">
                <span v-for="relation in localRelations" :key="relation.clientKey">
                  {{ relationSentence(relation) }}
                </span>
              </div>
            </section>
            <section>
              <h4>发布关注</h4>
              <ul>
                <li>目标对象必须存在。</li>
                <li>本对象匹配字段必须存在。</li>
                <li>目标对象所属字段必须从字段列表选择。</li>
                <li>目标对象未发布时会在发布检查中提示。</li>
                <li>需要在新增或编辑表单维护关联数据时，关系类型应使用“拥有多条”或“明细”。</li>
              </ul>
            </section>
          </aside>
        </div>
      </n-tab-pane>

      <n-tab-pane name="linkage" tab="级联规则">
        <div class="relation-designer-body">
          <main class="relation-list-pane">
            <div v-if="localLinkage.rules.length" class="relation-card-list">
              <section v-for="(rule, index) in localLinkage.rules" :key="rule.ruleId" class="relation-card linkage-card">
                <header class="relation-card-head">
                  <div>
                    <strong>{{ linkageRuleLabel(rule) }}</strong>
                    <p>{{ linkageRuleSentence(rule) }}</p>
                  </div>
                  <n-space size="small">
                    <n-tag size="small" :type="rule.enabled === false ? 'default' : 'success'" :bordered="false">
                      {{ rule.enabled === false ? '停用' : '启用' }}
                    </n-tag>
                    <n-popconfirm @positive-click="removeLinkageRule(index)">
                      <template #trigger>
                        <n-button quaternary circle size="small">
                          <template #icon>
                            <n-icon><TrashOutline /></n-icon>
                          </template>
                        </n-button>
                      </template>
                      确认移除该级联规则？
                    </n-popconfirm>
                  </n-space>
                </header>

                <n-form label-placement="top" :show-feedback="false" size="small" class="relation-form">
                  <n-grid :cols="3" :x-gap="12" :y-gap="4" responsive="screen">
                    <n-form-item-gi label="规则类型">
                      <n-select
                        v-model:value="rule.type"
                        :options="linkageTypeOptions"
                        @update:value="value => updateLinkageType(rule, value)"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi label="上级字段">
                      <n-select
                        v-model:value="rule.sourceField"
                        :options="sourceFieldOptions"
                        clearable
                        filterable
                        placeholder="选择控制字段"
                        @update:value="value => updateLinkageSource(rule, value)"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi label="目标字段">
                      <n-select
                        v-model:value="rule.targetField"
                        :options="linkageTargetFieldOptions(rule)"
                        clearable
                        filterable
                        placeholder="选择被过滤字段"
                        @update:value="value => updateLinkageTarget(rule, value)"
                      />
                    </n-form-item-gi>

                    <template v-if="rule.dataSourceType === 'dict'">
                      <n-form-item-gi label="上级字典类型">
                        <n-input v-model:value="rule.dictConfig.sourceDictType" placeholder="例如：sys_dict_type_a" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi label="目标字典类型">
                        <n-input v-model:value="rule.dictConfig.targetDictType" placeholder="例如：sys_dict_type_b" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi v-if="rule.type === 'linkedDict'" label="关联字典类型">
                        <n-input v-model:value="rule.dictConfig.linkedDictType" placeholder="匹配 linked_dict_type" @update:value="markDirty" />
                      </n-form-item-gi>
                    </template>

                    <template v-else>
                      <n-form-item-gi label="请求参数名">
                        <n-input v-model:value="rule.remoteConfig.paramName" placeholder="例如：orgId / parentId" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi label="远程接口">
                        <n-input v-model:value="rule.remoteConfig.url" placeholder="get@/api/xxx/options" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi label="请求方式">
                        <n-select v-model:value="rule.remoteConfig.method" :options="methodOptions" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi v-if="rule.type === 'objectReference'" label="目标对象">
                        <n-select
                          v-model:value="rule.objectConfig.targetObjectCode"
                          :options="targetObjectOptions"
                          clearable
                          filterable
                          placeholder="选择目标对象"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                    </template>

                    <n-form-item-gi label="上级为空">
                      <n-select
                        v-model:value="rule.emptyStrategy"
                        :options="emptyStrategyOptions"
                        @update:value="markDirty"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi label="上级变化清空">
                      <n-switch
                        :value="rule.clearOnSourceChange !== false"
                        @update:value="value => updateRuleClearOnChange(rule, value)"
                      />
                    </n-form-item-gi>
                    <n-form-item-gi label="启用状态">
                      <n-switch
                        :value="rule.enabled !== false"
                        @update:value="value => updateRuleEnabled(rule, value)"
                      />
                    </n-form-item-gi>
                  </n-grid>
                </n-form>
              </section>
            </div>
            <n-empty v-else description="暂无级联规则" />
          </main>

          <aside class="relation-summary-pane">
            <section>
              <h4>规则摘要</h4>
              <div class="metric-list">
                <span>全部 {{ localLinkage.rules.length }}</span>
                <span>字典 {{ linkageStats.dict }}</span>
                <span>远程 {{ linkageStats.remote }}</span>
                <span>停用 {{ linkageStats.disabled }}</span>
              </div>
            </section>
            <section>
              <h4>发布关注</h4>
              <ul>
                <li>上级字段和目标字段必须存在。</li>
                <li>字典级联必须配置目标字典类型。</li>
                <li>远程级联必须配置请求参数名和接口。</li>
                <li>保存后规则会同步到目标字段运行属性。</li>
              </ul>
            </section>
          </aside>
        </div>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>

<script setup>
import { AddOutline, TrashOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import {
  businessObjectDesigner,
  businessObjectList,
  businessObjectRelations,
  saveBusinessObjectDesigner,
} from '@/api/business-app'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import {
  applyLinkageSchemaToFields,
  createLinkageSchemaFromFields,
  normalizeLinkageSchema,
  validateLinkageSchema,
} from './form-first/linkageSchema'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  suiteCode: {
    type: String,
    default: '',
  },
  objectCode: {
    type: String,
    default: '',
  },
  objectName: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  linkageSchema: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['updated', 'dirtyChange', 'fieldsUpdated', 'update:linkageSchema'])

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const activeTab = ref('relations')
const businessObjects = ref([])
const localRelations = ref([])
const localLinkage = ref(normalizeLinkageSchema())
const targetFieldsMap = ref({})
const targetFieldLoadingMap = ref({})
const relationsLoaded = ref(false)
const relationDirty = ref(false)
let resettingLinkage = false

const relationTypeOptions = [
  { label: '属于目标对象', value: 'REFERENCE' },
  { label: '拥有多条目标记录', value: 'CHILD_LIST' },
  { label: '包含明细记录', value: 'DETAIL' },
  { label: '当前对象与目标对象多对多关联', value: 'MANY_TO_MANY' },
]

const childSaveModeOptions = [
  { label: '全量替换', value: 'replace' },
  { label: '行级合并', value: 'merge' },
]

const selectorFilterSourceOptions = [
  { label: '固定值', value: 'static' },
  { label: '当前表单字段', value: 'formData' },
  { label: '当前记录字段', value: 'record' },
  { label: '当前行字段', value: 'row' },
  { label: '路由查询参数', value: 'query' },
  { label: '路由路径参数', value: 'params' },
]

const linkageTypeOptions = [
  { label: '字典父子(parent_dict_code)', value: 'parentDictCode' },
  { label: '关联字典(linked_dict_type/value)', value: 'linkedDict' },
  { label: '远程参数过滤', value: 'remoteParam' },
  { label: '组织范围过滤', value: 'orgScope' },
  { label: '对象引用过滤', value: 'objectReference' },
]

const emptyStrategyOptions = [
  { label: '显示空选项', value: 'empty' },
  { label: '显示全部选项', value: 'all' },
  { label: '禁用目标字段', value: 'disabled' },
]

const methodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
]

const sourceFieldOptions = computed(() => {
  const fields = (props.fields || []).map(toPageField).filter(field => field.field && !isInactiveField(field))
  return fields.map(field => ({
    label: businessFieldOptionLabel(field),
    value: field.field,
  }))
})

const targetObjectOptions = computed(() => {
  const objects = (businessObjects.value || []).filter(item => item.objectCode !== props.objectCode)
  return objects.map(item => ({
    label: `${item.objectName || item.objectCode}（${item.objectCode}）`,
    value: item.objectCode,
  }))
})

const objectNameMap = computed(() => new Map((businessObjects.value || []).map(item => [item.objectCode, item.objectName || item.objectCode])))
const fieldMap = computed(() => new Map((props.fields || []).map(field => [field.fieldCode || field.field, toPageField(field)]).filter(([fieldCode]) => fieldCode)))
const linkageStats = computed(() => {
  const rules = localLinkage.value?.rules || []
  return {
    dict: rules.filter(rule => rule.dataSourceType === 'dict').length,
    remote: rules.filter(rule => ['remote', 'org', 'object'].includes(rule.dataSourceType)).length,
    disabled: rules.filter(rule => rule.enabled === false).length,
  }
})

watch(() => props.objectId, () => {
  loadRelations()
}, { immediate: true })

watch(() => props.suiteCode, () => {
  loadBusinessObjects()
}, { immediate: true })

watch(
  () => [props.linkageSchema, props.fields],
  () => resetLinkageSchema(),
  { immediate: true, deep: true },
)

onMounted(() => {
  loadBusinessObjects()
})

async function loadRelations() {
  if (!props.objectId) {
    localRelations.value = []
    relationsLoaded.value = true
    relationDirty.value = false
    return
  }
  loading.value = true
  relationsLoaded.value = false
  relationDirty.value = false
  try {
    const res = await businessObjectRelations(props.objectId)
    localRelations.value = (res.data || [])
      .filter(relation => !props.objectCode || !relation.sourceObjectCode || relation.sourceObjectCode === props.objectCode)
      .map(normalizeRelation)
    relationsLoaded.value = true
    relationDirty.value = false
    emit('updated', localRelations.value)
    localRelations.value.forEach((relation) => {
      loadTargetFields(relation.targetObjectCode)
      loadTargetFields(selectorCandidateObjectCode(relation))
    })
  }
  catch (error) {
    message.error(error?.message || '关系配置加载失败')
  }
  finally {
    loading.value = false
  }
}

async function loadBusinessObjects() {
  try {
    const res = await businessObjectList({
      suiteCode: props.suiteCode || undefined,
    })
    businessObjects.value = res.data || []
    localRelations.value.forEach((relation) => {
      loadTargetFields(relation.targetObjectCode)
      loadTargetFields(selectorCandidateObjectCode(relation))
    })
  }
  catch {
    businessObjects.value = []
  }
}

function resetLinkageSchema() {
  resettingLinkage = true
  try {
    const next = createLinkageSchemaFromFields(props.fields || [], props.linkageSchema || {})
    if (!isSameSchema(next, localLinkage.value))
      localLinkage.value = cloneSchema(next)
  }
  finally {
    resettingLinkage = false
  }
}

async function addRelation() {
  const target = targetObjectOptions.value[0]
  if (!target) {
    message.warning('当前套件没有可关联的目标对象')
    return
  }
  const targetName = objectNameMap.value.get(target.value) || target.label
  await loadTargetFields(target.value)
  const relationType = inferDefaultRelationType(target.value)
  const sourceFieldCode = firstSourceField(relationType, target.value)
  const targetFieldCode = firstTargetField(target.value, relationType)
  const relation = {
    clientKey: createClientKey(),
    relationType,
    targetObjectCode: target.value,
    relationName: relationLabel({ relationType, targetObjectCode: target.value }),
    sourceFieldCode,
    targetFieldCode,
    displayField: firstDisplayField(target.value, targetFieldCode),
    detailTabTitle: targetName,
    showInDetail: true,
    inlineCreateEnabled: canInlineEdit({ relationType }),
    inlineEditEnabled: canInlineEdit({ relationType }),
    defaultFilter: '',
    selectorEnabled: false,
    selectorSuiteCode: props.suiteCode || '',
    selectorObjectCode: target.value,
    selectorTitle: '',
    selectorButtonText: '选择记录',
    selectorDisplayFields: [],
    selectorKeywordFields: [],
    selectorMappings: [],
    selectorSearchParams: [],
    description: '',
    status: 1,
    sortOrder: localRelations.value.length * 10 + 10,
  }
  localRelations.value.push(relation)
  markRelationDirty()
}

function addLinkageRule() {
  const source = sourceFieldOptions.value[0]
  const target = sourceFieldOptions.value.find(item => item.value !== source?.value) || sourceFieldOptions.value[1]
  if (!source || !target) {
    message.warning('至少需要两个可用字段才能配置级联')
    return
  }
  const rule = normalizeLinkageRuleDraft({
    ruleId: createLinkageRuleId(source.value, target.value),
    type: inferLinkageType(target.value),
    sourceField: source.value,
    targetField: target.value,
  })
  localLinkage.value = normalizeLinkageSchema({
    ...localLinkage.value,
    rules: [
      ...(localLinkage.value.rules || []),
      rule,
    ],
  })
  syncLinkageModel(true)
  activeTab.value = 'linkage'
}

function removeRelation(index) {
  localRelations.value.splice(index, 1)
  markRelationDirty()
}

function removeLinkageRule(index) {
  const rules = [...(localLinkage.value.rules || [])]
  rules.splice(index, 1)
  localLinkage.value = normalizeLinkageSchema({
    ...localLinkage.value,
    rules,
  })
  syncLinkageModel(true)
}

function updateRelationType(relation, value) {
  relation.relationType = value
  if (!canInlineEdit(relation)) {
    relation.inlineCreateEnabled = false
    relation.inlineEditEnabled = false
  }
  relation.sourceFieldCode = firstSourceField(value, relation.targetObjectCode, relation.sourceFieldCode)
  relation.targetFieldCode = firstTargetField(relation.targetObjectCode, value, relation.targetFieldCode)
  if (!relation.displayField)
    relation.displayField = firstDisplayField(relation.targetObjectCode, relation.targetFieldCode)
  relation.relationName = relationLabel(relation)
  markDirty()
}

async function updateTargetObject(relation, value) {
  relation.targetObjectCode = value
  await loadTargetFields(value)
  const targetName = objectNameMap.value.get(value) || value
  relation.detailTabTitle = relation.detailTabTitle || targetName
  relation.relationName = relationLabel(relation)
  relation.sourceFieldCode = firstSourceField(relation.relationType, value, relation.sourceFieldCode)
  relation.targetFieldCode = firstTargetField(value, relation.relationType)
  relation.displayField = firstDisplayField(value, relation.targetFieldCode)
  if (!relation.selectorObjectCode)
    relation.selectorObjectCode = value || ''
  if (relation.selectorEnabled)
    await applySelectorDefaults(relation, false)
  markDirty()
}

function updateStatus(relation, value) {
  relation.status = value ? 1 : 0
  markDirty()
}

function updateShowInDetail(relation, value) {
  relation.showInDetail = !!value
  markDirty()
}

function updateInlineEdit(relation, value) {
  relation.inlineEditEnabled = canInlineEdit(relation) && !!value
  markDirty()
}

function updateInlineCreate(relation, value) {
  relation.inlineCreateEnabled = canInlineEdit(relation) && !!value
  markDirty()
}

function updateRelationSelectorEnabled(relation, value) {
  relation.selectorEnabled = canInlineEdit(relation) && !!value
  if (relation.selectorEnabled && !relation.selectorObjectCode)
    relation.selectorObjectCode = relation.targetObjectCode || ''
  if (relation.selectorEnabled)
    applySelectorDefaults(relation, false)
  markDirty()
}

async function updateSelectorObject(relation, value) {
  relation.selectorObjectCode = value || ''
  await loadTargetFields(selectorCandidateObjectCode(relation))
  await applySelectorDefaults(relation, false)
  markDirty()
}

function updateSelectorSearchParamSource(param, value) {
  param.sourceType = value || 'static'
  if (param.sourceType === 'static')
    param.sourceField = ''
  else
    param.staticValue = ''
  markDirty()
}

function addSelectorMapping(relation) {
  relation.selectorMappings = [
    ...(relation.selectorMappings || []),
    createSelectorMappingRow(),
  ]
  markDirty()
}

function removeSelectorMapping(relation, index) {
  relation.selectorMappings.splice(index, 1)
  markDirty()
}

function addSelectorSearchParam(relation) {
  relation.selectorSearchParams = [
    ...(relation.selectorSearchParams || []),
    createSelectorSearchParamRow(),
  ]
  markDirty()
}

function removeSelectorSearchParam(relation, index) {
  relation.selectorSearchParams.splice(index, 1)
  markDirty()
}

function updateLinkageType(rule, value) {
  const next = normalizeLinkageRuleDraft({
    ...rule,
    type: value,
    matchMode: value,
    dataSourceType: resolveLinkageDataSourceType(value),
  })
  Object.assign(rule, next)
  fillRuleFieldDefaults(rule)
  syncLinkageModel(true)
}

function updateLinkageSource(rule, value) {
  rule.sourceField = value || ''
  fillRuleFieldDefaults(rule)
  syncLinkageModel(true)
}

function updateLinkageTarget(rule, value) {
  rule.targetField = value || ''
  fillRuleFieldDefaults(rule)
  syncLinkageModel(true)
}

function updateRuleClearOnChange(rule, value) {
  rule.clearOnSourceChange = !!value
  syncLinkageModel(true)
}

function updateRuleEnabled(rule, value) {
  rule.enabled = !!value
  syncLinkageModel(true)
}

async function saveRelations() {
  if (!props.objectId)
    return
  const shouldSaveRelations = activeTab.value === 'relations' || relationDirty.value
  if (shouldSaveRelations) {
    if (!relationsLoaded.value) {
      message.warning('关系配置还没有加载完成，请刷新后再保存')
      return
    }
    const invalidRelation = localRelations.value.find(relation => !relation.targetObjectCode || !relation.sourceFieldCode || !relation.targetFieldCode)
    if (invalidRelation) {
      message.warning(`请先补全「${invalidRelation.relationName || relationLabel(invalidRelation)}」的关联对象和匹配字段`)
      return
    }
    const missingDisplayRelation = localRelations.value.find(relation => relation.relationType === 'REFERENCE' && !relation.displayField)
    if (missingDisplayRelation) {
      message.warning(`请先为「${missingDisplayRelation.relationName || relationLabel(missingDisplayRelation)}」选择目标对象回显字段`)
      return
    }
    const invalidSelectorRelation = localRelations.value.find(relation => relation.selectorEnabled && !relation.selectorObjectCode)
    if (invalidSelectorRelation) {
      message.warning(`请先为「${invalidSelectorRelation.relationName || relationLabel(invalidSelectorRelation)}」选择子表选择器候选对象`)
      return
    }
  }
  saving.value = true
  try {
    const linkageSchema = normalizeLinkageSchema(localLinkage.value)
    const validation = validateLinkageSchema(linkageSchema, sourceFieldOptions.value.map(item => item.value))
    if (!validation.valid) {
      activeTab.value = 'linkage'
      message.warning(validation.errors[0]?.message || '请先修复级联规则')
      return
    }
    const fields = applyLinkageSchemaToFields(props.fields || [], linkageSchema)
    const designerPayload = {
      fields: fields.map(toFieldPayload),
      linkageSchema,
    }
    if (shouldSaveRelations)
      designerPayload.relations = localRelations.value.map(toRelationPayload)
    await saveBusinessObjectDesigner(props.objectId, designerPayload)
    message.success('关系与级联配置已保存')
    relationDirty.value = false
    emit('fieldsUpdated', fields)
    emit('update:linkageSchema', linkageSchema)
    emit('dirtyChange', false)
    await loadRelations()
  }
  finally {
    saving.value = false
  }
}

function normalizeRelation(relation = {}) {
  const config = parseRelationConfig(relation.relationConfig)
  const selector = normalizeRelationSelector(config.recordSelector || config.selector)
  return {
    ...relation,
    clientKey: relation.id || createClientKey(),
    relationType: relation.relationType || 'REFERENCE',
    relationName: relation.relationName || relationLabel(relation),
    targetObjectCode: relation.targetObjectCode || '',
    sourceFieldCode: relation.sourceFieldCode || '',
    targetFieldCode: relation.targetFieldCode || '',
    detailTabTitle: config.detailTabTitle || relation.relationName || relation.targetObjectName || relation.targetObjectCode || '',
    showInDetail: config.showInDetail !== false,
    inlineCreateEnabled: canInlineEdit(relation) && config.inlineCreateEnabled !== false && config.inlineCreateEnabled !== 'false',
    inlineEditEnabled: canInlineEdit(relation) && config.inlineEditEnabled !== false && config.inlineEditEnabled !== 'false',
    saveMode: normalizeChildSaveMode(config.saveMode),
    defaultFilter: config.defaultFilter || '',
    displayField: config.displayField || '',
    selectorEnabled: Boolean(selector.objectCode),
    selectorSuiteCode: selector.suiteCode,
    selectorObjectCode: selector.objectCode,
    selectorTitle: selector.title,
    selectorButtonText: selector.buttonText,
    selectorDisplayFields: normalizeDisplayFieldCodes(selector.displayFields),
    selectorKeywordFields: normalizeTextList(selector.keywordFields),
    selectorMappings: createSelectorMappingRows(selector.fieldMappings),
    selectorSearchParams: createSelectorSearchParamRows(selector.searchParams),
    selectorDisplayFieldsText: listToLines(selector.displayFields),
    selectorKeywordFieldsText: listToLines(selector.keywordFields),
    selectorMappingsText: mappingsToLines(selector.fieldMappings),
    selectorSearchParamsText: searchParamsToLines(selector.searchParams),
    status: relation.status ?? 1,
    sortOrder: relation.sortOrder ?? 0,
  }
}

function toRelationPayload(relation = {}) {
  return {
    id: relation.id,
    suiteCode: props.suiteCode,
    sourceObjectCode: props.objectCode,
    targetObjectCode: relation.targetObjectCode,
    relationType: relation.relationType,
    relationName: relation.relationName || relationLabel(relation),
    sourceFieldCode: relation.sourceFieldCode,
    targetFieldCode: relation.targetFieldCode,
    relationConfig: buildRelationConfig(relation),
    description: relation.description,
    status: relation.status ?? 1,
    sortOrder: relation.sortOrder ?? 0,
  }
}

function buildRelationConfig(relation) {
  const config = {}
  if (relation.detailTabTitle)
    config.detailTabTitle = relation.detailTabTitle
  config.showInDetail = relation.showInDetail !== false
  config.inlineCreateEnabled = canInlineEdit(relation) && relation.inlineCreateEnabled === true
  config.inlineEditEnabled = canInlineEdit(relation) && relation.inlineEditEnabled === true
  config.saveMode = normalizeChildSaveMode(relation.saveMode)
  if (relation.defaultFilter)
    config.defaultFilter = relation.defaultFilter
  if (relation.displayField)
    config.displayField = relation.displayField
  const selector = buildRelationSelectorConfig(relation)
  if (selector)
    config.recordSelector = selector
  return Object.keys(config).length ? JSON.stringify(config) : ''
}

function parseRelationConfig(value) {
  if (!value)
    return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' ? parsed : {}
  }
  catch {
    return {
      defaultFilter: value,
    }
  }
}

function normalizeRelationSelector(value) {
  const config = value && typeof value === 'object' ? value : {}
  return {
    suiteCode: String(config.suiteCode || '').trim(),
    objectCode: String(config.objectCode || '').trim(),
    title: String(config.title || config.selectorTitle || '').trim(),
    buttonText: String(config.buttonText || '').trim(),
    displayFields: normalizeTextList(config.displayFields),
    keywordFields: normalizeTextList(config.keywordFields),
    fieldMappings: normalizeMappingList(config.fieldMappings || config.mappings),
    searchParams: normalizeSearchParams(config.searchParams),
  }
}

function normalizeDisplayFieldCodes(value) {
  return normalizeTextList(value)
    .map(item => String(item || '').split(/\s*[:：]\s*/, 1)[0].trim())
    .filter(Boolean)
}

function createSelectorMappingRow(source = {}) {
  return {
    clientKey: createClientKey(),
    sourceField: String(source.sourceField || source.source || '').trim(),
    targetField: String(source.targetField || source.target || '').trim(),
  }
}

function createSelectorMappingRows(value) {
  return normalizeMappingList(value).map(createSelectorMappingRow)
}

function createSelectorSearchParamRow(source = {}) {
  const row = normalizeSelectorSearchParamRow(source)
  return {
    clientKey: createClientKey(),
    ...row,
  }
}

function createSelectorSearchParamRows(value) {
  return Object.entries(normalizeSearchParams(value)).map(([key, item]) => createSelectorSearchParamRow({
    paramKey: key,
    value: item,
  }))
}

function normalizeSelectorSearchParamRow(source = {}) {
  const paramKey = String(source.paramKey || source.key || '').trim()
  const sourceType = String(source.sourceType || '').trim()
  if (sourceType) {
    return {
      paramKey,
      sourceType,
      sourceField: String(source.sourceField || '').trim(),
      staticValue: source.staticValue ?? '',
    }
  }
  return {
    paramKey,
    ...parseSelectorParamValue(source.value ?? source.staticValue),
  }
}

function parseSelectorParamValue(value) {
  const textValue = String(value ?? '').trim()
  const matched = textValue.match(/^\$\{(formData|form|record|row|query|routeQuery|params)\.([^}]+)\}$/)
  if (!matched) {
    return {
      sourceType: 'static',
      sourceField: '',
      staticValue: textValue,
    }
  }
  const sourceTypeAlias = {
    form: 'formData',
    routeQuery: 'query',
  }
  return {
    sourceType: sourceTypeAlias[matched[1]] || matched[1],
    sourceField: matched[2],
    staticValue: '',
  }
}

function buildRelationSelectorConfig(relation = {}) {
  if (!relation.selectorEnabled)
    return null
  const selector = normalizeRelationSelector({
    suiteCode: relation.selectorSuiteCode || props.suiteCode,
    objectCode: relation.selectorObjectCode || relation.targetObjectCode,
    title: relation.selectorTitle,
    buttonText: relation.selectorButtonText,
    displayFields: buildSelectorDisplayFields(relation),
    keywordFields: normalizeTextList(relation.selectorKeywordFields?.length ? relation.selectorKeywordFields : relation.selectorKeywordFieldsText),
    fieldMappings: buildSelectorMappings(relation),
    searchParams: buildSelectorSearchParams(relation),
  })
  if (!selector.objectCode)
    return null
  const result = {
    objectCode: selector.objectCode,
  }
  if (selector.suiteCode)
    result.suiteCode = selector.suiteCode
  if (selector.title)
    result.title = selector.title
  if (selector.buttonText)
    result.buttonText = selector.buttonText
  if (selector.displayFields.length)
    result.displayFields = selector.displayFields
  if (selector.keywordFields.length)
    result.keywordFields = selector.keywordFields
  if (selector.fieldMappings.length)
    result.fieldMappings = selector.fieldMappings
  if (Object.keys(selector.searchParams).length)
    result.searchParams = selector.searchParams
  return result
}

function buildSelectorDisplayFields(relation = {}) {
  const codes = normalizeDisplayFieldCodes((relation.selectorDisplayFields || []).length
    ? relation.selectorDisplayFields
    : relation.selectorDisplayFieldsText)
  if (!codes.length)
    return []
  const fieldMap = new Map((targetFieldsMap.value[selectorCandidateObjectCode(relation)] || []).map(field => [field.field, field]))
  return codes.map((fieldCode) => {
    const field = fieldMap.get(fieldCode)
    const label = field ? businessFieldLabel(field) : ''
    return label && label !== fieldCode ? `${fieldCode}:${label}` : fieldCode
  })
}

function buildSelectorMappings(relation = {}) {
  const rows = Array.isArray(relation.selectorMappings) && relation.selectorMappings.length
    ? relation.selectorMappings
    : createSelectorMappingRows(relation.selectorMappingsText)
  return rows
    .map(row => ({
      sourceField: String(row.sourceField || '').trim(),
      targetField: String(row.targetField || '').trim(),
    }))
    .filter(row => row.sourceField && row.targetField)
}

function buildSelectorSearchParams(relation = {}) {
  const rows = Array.isArray(relation.selectorSearchParams) && relation.selectorSearchParams.length
    ? relation.selectorSearchParams
    : createSelectorSearchParamRows(relation.selectorSearchParamsText)
  return rows.reduce((result, row) => {
    const paramKey = String(row.paramKey || '').trim()
    if (!paramKey)
      return result
    const sourceType = row.sourceType || 'static'
    if (sourceType === 'static') {
      if (row.staticValue !== undefined && row.staticValue !== null && String(row.staticValue).trim() !== '')
        result[paramKey] = row.staticValue
      return result
    }
    const sourceField = String(row.sourceField || '').trim()
    if (sourceField)
      result[paramKey] = `\${${sourceType}.${sourceField}}`
    return result
  }, {})
}

function normalizeTextList(value) {
  if (Array.isArray(value))
    return value.map(item => String(item || '').trim()).filter(Boolean)
  if (typeof value === 'string')
    return parseTextList(value)
  return []
}

function parseTextList(value) {
  return String(value || '')
    .split(/[\n,，]/)
    .map(item => item.trim())
    .filter(Boolean)
}

function normalizeMappingList(value) {
  if (Array.isArray(value)) {
    return value
      .map(item => ({
        sourceField: String(item?.sourceField || item?.source || '').trim(),
        targetField: String(item?.targetField || item?.target || '').trim(),
      }))
      .filter(item => item.sourceField && item.targetField)
  }
  if (value && typeof value === 'object') {
    return Object.entries(value)
      .map(([sourceField, targetField]) => ({
        sourceField: String(sourceField || '').trim(),
        targetField: String(targetField || '').trim(),
      }))
      .filter(item => item.sourceField && item.targetField)
  }
  if (typeof value === 'string')
    return parseMappingLines(value)
  return []
}

function parseMappingLines(value) {
  return String(value || '')
    .split(/\n/)
    .map(item => item.trim())
    .filter(Boolean)
    .map((item) => {
      const parts = item.split(/\s*(?:=>|=|:)\s*/, 2)
      return {
        sourceField: String(parts[0] || '').trim(),
        targetField: String(parts[1] || '').trim(),
      }
    })
    .filter(item => item.sourceField && item.targetField)
}

function normalizeSearchParams(value) {
  if (!value)
    return {}
  if (typeof value === 'string')
    return parseSearchParamLines(value)
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return Object.entries(value).reduce((result, [key, item]) => {
      const paramKey = String(key || '').trim()
      if (paramKey && item !== undefined && item !== null && item !== '')
        result[paramKey] = item
      return result
    }, {})
  }
  return {}
}

function parseSearchParamLines(value) {
  const text = String(value || '').trim()
  if (!text)
    return {}
  if (text.startsWith('{')) {
    try {
      return normalizeSearchParams(JSON.parse(text))
    }
    catch {
      return {}
    }
  }
  return text
    .split(/\n/)
    .map(item => item.trim())
    .filter(Boolean)
    .reduce((result, item) => {
      const parts = item.split(/\s*(?:=>|=|:)\s*/, 2)
      const key = String(parts[0] || '').trim()
      const paramValue = String(parts[1] || '').trim()
      if (key && paramValue)
        result[key] = paramValue
      return result
    }, {})
}

function listToLines(value) {
  return normalizeTextList(value).join('\n')
}

function mappingsToLines(value) {
  return normalizeMappingList(value)
    .map(item => `${item.sourceField}=${item.targetField}`)
    .join('\n')
}

function searchParamsToLines(value) {
  return Object.entries(normalizeSearchParams(value))
    .map(([key, item]) => `${key}=${item}`)
    .join('\n')
}

function normalizeChildSaveMode(value) {
  return String(value || '').toLowerCase() === 'merge' ? 'merge' : 'replace'
}

function normalizeLinkageRuleDraft(rule = {}) {
  const sourceField = rule.sourceField || ''
  const targetField = rule.targetField || ''
  const type = linkageTypeOptions.some(item => item.value === rule.type) ? rule.type : 'linkedDict'
  const sourceFieldConfig = fieldMap.value.get(sourceField) || {}
  const targetFieldConfig = fieldMap.value.get(targetField) || {}
  const dataSourceType = resolveLinkageDataSourceType(type)
  return {
    ruleId: rule.ruleId || createLinkageRuleId(sourceField, targetField),
    type,
    sourceField,
    targetField,
    dataSourceType,
    matchMode: type,
    dictConfig: {
      ...(rule.dictConfig || {}),
      sourceDictType: rule.dictConfig?.sourceDictType || sourceFieldConfig.dictType || '',
      targetDictType: rule.dictConfig?.targetDictType || targetFieldConfig.dictType || '',
      linkedDictType: rule.dictConfig?.linkedDictType || sourceFieldConfig.dictType || '',
      parentValueSource: rule.dictConfig?.parentValueSource || 'sourceValue',
    },
    remoteConfig: {
      ...(rule.remoteConfig || {}),
      url: rule.remoteConfig?.url || targetFieldConfig.basicProps?.optionSource?.api || '',
      method: rule.remoteConfig?.method || 'GET',
      paramName: rule.remoteConfig?.paramName || sourceField || '',
      valuePath: rule.remoteConfig?.valuePath || 'data',
      labelField: rule.remoteConfig?.labelField || 'label',
      valueField: rule.remoteConfig?.valueField || 'value',
    },
    objectConfig: {
      ...(rule.objectConfig || {}),
      targetObjectCode: rule.objectConfig?.targetObjectCode || targetFieldConfig.referenceObjectCode || targetFieldConfig.basicProps?.referenceObjectCode || '',
      displayField: rule.objectConfig?.displayField || targetFieldConfig.referenceDisplayField || targetFieldConfig.basicProps?.referenceDisplayField || '',
    },
    orgConfig: {
      ...(rule.orgConfig || {}),
      paramName: rule.orgConfig?.paramName || rule.remoteConfig?.paramName || sourceField || '',
    },
    condition: { ...(rule.condition || {}) },
    emptyStrategy: ['empty', 'all', 'disabled'].includes(rule.emptyStrategy) ? rule.emptyStrategy : 'empty',
    clearOnSourceChange: rule.clearOnSourceChange !== false,
    enabled: rule.enabled !== false,
  }
}

function fillRuleFieldDefaults(rule) {
  const normalized = normalizeLinkageRuleDraft(rule)
  Object.assign(rule, normalized)
}

function syncLinkageModel(dirty = false) {
  if (resettingLinkage)
    return
  const normalized = normalizeLinkageSchema(localLinkage.value)
  if (!isSameSchema(normalized, localLinkage.value))
    localLinkage.value = normalized
  if (!isSameSchema(normalized, props.linkageSchema))
    emit('update:linkageSchema', cloneSchema(normalized))
  if (dirty) {
    emit('dirtyChange', true)
  }
}

function resolveLinkageDataSourceType(type) {
  if (['parentDictCode', 'linkedDict'].includes(type))
    return 'dict'
  if (type === 'orgScope')
    return 'org'
  if (type === 'objectReference')
    return 'object'
  return 'remote'
}

function createLinkageRuleId(sourceField = '', targetField = '') {
  return `linkage_${sourceField || 'source'}_${targetField || Date.now()}`
    .replace(/\W+/g, '_')
    .replace(/_+/g, '_')
}

function inferLinkageType(targetField) {
  const target = fieldMap.value.get(targetField) || {}
  if (target.componentType === 'objectReference' || target.fieldType === 'REFERENCE')
    return 'objectReference'
  if (target.componentType === 'orgTreeSelect' || ['DEPT', 'ORG'].includes(target.fieldType))
    return 'orgScope'
  if (target.dictType || ['DICT', 'RADIO', 'CHECKBOX'].includes(target.fieldType))
    return 'linkedDict'
  return 'remoteParam'
}

function linkageTargetFieldOptions(rule = {}) {
  return sourceFieldOptions.value.filter(item => item.value !== rule.sourceField)
}

function linkageRuleLabel(rule = {}) {
  const target = fieldLabel(rule.targetField)
  const typeLabel = linkageTypeOptions.find(item => item.value === rule.type)?.label || rule.type
  return `${target || '目标字段'} · ${typeLabel}`
}

function linkageRuleSentence(rule = {}) {
  const source = fieldLabel(rule.sourceField) || '上级字段'
  const target = fieldLabel(rule.targetField) || '目标字段'
  const verbs = {
    parentDictCode: '按父级字典过滤',
    linkedDict: '按关联字典过滤',
    remoteParam: '作为远程参数过滤',
    orgScope: '作为组织范围过滤',
    objectReference: '过滤引用对象',
  }
  return `${source} → ${target}，${verbs[rule.type] || '联动'}`
}

function fieldLabel(fieldCode) {
  if (!fieldCode)
    return ''
  const field = fieldMap.value.get(fieldCode)
  return field ? `${field.label || field.fieldName || fieldCode}（${fieldCode}）` : fieldCode
}

function relationSentence(relation) {
  const source = props.objectName || props.objectCode || '当前对象'
  const target = objectNameMap.value.get(relation.targetObjectCode) || relation.targetObjectName || relation.targetObjectCode || '目标对象'
  const verbs = {
    REFERENCE: '属于',
    CHILD_LIST: '有多个',
    DETAIL: '包含明细',
    MANY_TO_MANY: '关联多个',
  }
  return `${source}${verbs[relation.relationType] || '关联'}${target}`
}

function sourceFieldLabel(relation) {
  return relation?.relationType === 'REFERENCE'
    ? '当前对象中的关联字段'
    : '当前对象匹配字段'
}

function sourceFieldPlaceholder(relation) {
  return relation?.relationType === 'REFERENCE'
    ? '例如：customerId'
    : '通常选择：记录ID'
}

function targetFieldLabel(relation) {
  return relation?.relationType === 'REFERENCE'
    ? '目标对象主键字段'
    : '目标对象里指向本对象的字段'
}

function targetFieldPlaceholder(relation) {
  return relation?.relationType === 'REFERENCE'
    ? '通常选择：记录ID'
    : '选择目标对象中的所属字段'
}

function displayFieldLabel(relation) {
  return relation?.relationType === 'REFERENCE'
    ? '运行态显示字段'
    : '目标对象回显字段'
}

function targetFieldOptions(relation) {
  const fields = targetFieldsMap.value[relation.targetObjectCode] || []
  const options = fields
    .filter(field => field.field && !isInactiveField(field))
    .map(field => ({
      label: businessFieldOptionLabel(field),
      value: field.field,
    }))
  if (relation.targetFieldCode && !options.some(item => item.value === relation.targetFieldCode)) {
    options.unshift({
      label: `已配置字段：${relation.targetFieldCode}`,
      value: relation.targetFieldCode,
    })
  }
  return options
}

function targetDisplayFieldOptions(relation) {
  const fields = targetFieldsMap.value[relation.targetObjectCode] || []
  const options = fields
    .filter(field => field.field && !isInactiveField(field) && !field.systemField && field.field !== relation.targetFieldCode)
    .map(field => ({
      label: businessFieldOptionLabel(field),
      value: field.field,
    }))
  if (relation.displayField && !options.some(item => item.value === relation.displayField)) {
    options.unshift({
      label: `已配置字段：${relation.displayField}`,
      value: relation.displayField,
    })
  }
  return options
}

function selectorCandidateObjectCode(relation = {}) {
  return relation.selectorObjectCode || relation.targetObjectCode || ''
}

function selectorCandidateFieldOptions(relation, currentValue = '') {
  return fieldOptionsForObject(selectorCandidateObjectCode(relation), currentValue)
}

function selectorTargetFieldOptions(relation, currentValue = '') {
  return fieldOptionsForObject(relation.targetObjectCode, currentValue)
}

function selectorContextFieldOptions(param = {}) {
  if (['query', 'params'].includes(param.sourceType)) {
    return [
      { label: 'id', value: 'id' },
      { label: 'recordId', value: 'recordId' },
      { label: 'objectCode', value: 'objectCode' },
    ]
  }
  return sourceFieldOptions.value
}

function fieldOptionsForObject(objectCode, currentValue = '') {
  const fields = targetFieldsMap.value[objectCode] || []
  const options = fields
    .filter(field => field.field && !isInactiveField(field))
    .map(field => ({
      label: businessFieldOptionLabel(field),
      value: field.field,
    }))
  if (currentValue && !options.some(item => item.value === currentValue)) {
    options.unshift({
      label: `已配置字段：${currentValue}`,
      value: currentValue,
    })
  }
  return options
}

async function loadTargetFields(objectCode) {
  if (!objectCode || targetFieldsMap.value[objectCode] || targetFieldLoadingMap.value[objectCode])
    return
  const targetObject = businessObjects.value.find(item => item.objectCode === objectCode)
  if (!targetObject?.id)
    return
  targetFieldLoadingMap.value = {
    ...targetFieldLoadingMap.value,
    [objectCode]: true,
  }
  try {
    const res = await businessObjectDesigner(targetObject.id)
    const fields = res.data?.fields || res.data?.modelSchema?.fields || []
    targetFieldsMap.value = {
      ...targetFieldsMap.value,
      [objectCode]: fields.map(toPageField),
    }
  }
  catch {
    targetFieldsMap.value = {
      ...targetFieldsMap.value,
      [objectCode]: [],
    }
  }
  finally {
    targetFieldLoadingMap.value = {
      ...targetFieldLoadingMap.value,
      [objectCode]: false,
    }
  }
}

function firstTargetField(objectCode, relationType = 'CHILD_LIST', currentValue = '') {
  const fields = targetFieldsMap.value[objectCode] || []
  if (relationType === 'REFERENCE') {
    if (currentValue && currentValue !== 'id' && fields.some(field => field.field === currentValue && !isInactiveField(field)))
      return currentValue
    return fields.find(field => field.field === 'id' && !isInactiveField(field))?.field
      || fields.find(field => field.primaryKey && !isInactiveField(field))?.field
      || fields.find(field => !isInactiveField(field))?.field
      || ''
  }
  const sourceObject = lowerFirst(props.objectCode || '')
  const candidates = [
    `${sourceObject}Id`,
    `${sourceObject}Code`,
    props.objectCode,
    'parentId',
  ].filter(Boolean)
  const activeFields = fields.filter(field => !isInactiveField(field))
  const matched = activeFields.find(field => candidates.includes(field.field))
    || activeFields.find((field) => {
      const label = field.label || ''
      const sourceName = props.objectName || props.objectCode || ''
      return sourceName && label.includes(sourceName)
    })
  if (matched)
    return matched.field
  if (currentValue && currentValue !== 'id' && fields.some(field => field.field === currentValue && !isInactiveField(field)))
    return currentValue
  const fallback = activeFields.find(field => field.field !== 'id' && !field.systemField)
  return fallback?.field || ''
}

function inferDefaultRelationType(targetObjectCode) {
  return findReferenceSourceField(targetObjectCode) ? 'REFERENCE' : 'CHILD_LIST'
}

function firstDisplayField(objectCode, relationField = '') {
  const targetObject = businessObjects.value.find(item => item.objectCode === objectCode)
  const fields = targetFieldsMap.value[objectCode] || []
  const activeFields = fields.filter(field => field.field && !isInactiveField(field) && !field.systemField && field.field !== relationField)
  const configured = targetObject?.displayField
  const matched = activeFields.find(field => configured && field.field === configured)
    || activeFields.find((field) => {
      const fieldName = String(field.field || '').toLowerCase()
      const label = String(field.label || field.fieldName || '')
      return fieldName.includes('name') || label.includes('名称') || label.includes('姓名')
    })
    || activeFields[0]
  return matched?.field || ''
}

async function applySelectorDefaults(relation, force = true) {
  const candidateObjectCode = selectorCandidateObjectCode(relation)
  await loadTargetFields(candidateObjectCode)
  await loadTargetFields(relation.targetObjectCode)
  const candidateFields = selectorActiveFields(candidateObjectCode)
  const targetFields = selectorActiveFields(relation.targetObjectCode)
  if (force || !(relation.selectorDisplayFields || []).length)
    relation.selectorDisplayFields = inferSelectorDisplayFields(candidateFields)
  if (force || !(relation.selectorKeywordFields || []).length)
    relation.selectorKeywordFields = inferSelectorKeywordFields(candidateFields, relation.selectorDisplayFields)
  if (force || !(relation.selectorMappings || []).some(item => item.sourceField && item.targetField))
    relation.selectorMappings = inferSelectorMappings(candidateFields, targetFields)
  if (!relation.selectorButtonText)
    relation.selectorButtonText = '选择记录'
  if (!relation.selectorTitle) {
    const objectName = objectNameMap.value.get(candidateObjectCode) || candidateObjectCode
    relation.selectorTitle = objectName ? `选择${objectName}` : ''
  }
  markDirty()
}

function selectorActiveFields(objectCode) {
  return (targetFieldsMap.value[objectCode] || [])
    .filter(field => field.field && !isInactiveField(field) && !field.systemField)
}

function inferSelectorDisplayFields(fields = []) {
  const preferred = ['code', 'no', 'number', 'name', 'title']
  const matched = fields.filter(field => preferred.some(key => normalizedFieldToken(field).includes(key)))
  return (matched.length ? matched : fields).slice(0, 4).map(field => field.field)
}

function inferSelectorKeywordFields(fields = [], displayFields = []) {
  const displaySet = new Set(displayFields || [])
  const preferred = fields.filter((field) => {
    const token = normalizedFieldToken(field)
    return token.includes('code') || token.includes('no') || token.includes('number')
      || token.includes('name') || token.includes('title') || displaySet.has(field.field)
  })
  return (preferred.length ? preferred : fields).slice(0, 3).map(field => field.field)
}

function inferSelectorMappings(candidateFields = [], targetFields = []) {
  const candidateByField = new Map(candidateFields.map(field => [field.field, field]))
  const candidateByToken = new Map(candidateFields.map(field => [normalizedFieldToken(field), field]))
  const rows = []
  const usedTargets = new Set()
  targetFields.forEach((target) => {
    const source = candidateByField.get(target.field)
      || candidateByToken.get(normalizedFieldToken(target))
      || findSemanticSourceField(candidateFields, target)
    if (source && !usedTargets.has(target.field)) {
      rows.push(createSelectorMappingRow({
        sourceField: source.field,
        targetField: target.field,
      }))
      usedTargets.add(target.field)
    }
  })
  return rows.slice(0, 8)
}

function findSemanticSourceField(candidateFields = [], target = {}) {
  const targetToken = normalizedFieldToken(target)
  const semanticGroups = [
    ['id', 'recordid'],
    ['code', 'no', 'number', 'sn'],
    ['name', 'title', 'label'],
    ['price', 'amount', 'cost', 'fee'],
    ['unit', 'uom'],
    ['spec', 'model', 'type'],
  ]
  const group = semanticGroups.find(items => items.some(item => targetToken.includes(item)))
  if (!group)
    return null
  return candidateFields.find(field => group.some(item => normalizedFieldToken(field).includes(item))) || null
}

function normalizedFieldToken(field = {}) {
  return `${field.field || ''}_${field.label || ''}_${field.fieldName || ''}`
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .toLowerCase()
}

function isInactiveField(field = {}) {
  const status = String(field.fieldStatus || '').toUpperCase()
  return status === 'DISABLED' || status === 'HIDDEN'
}

function businessFieldLabel(field = {}) {
  const fieldName = field.field || ''
  if (fieldName === 'id')
    return '记录ID'
  if (fieldName === 'createBy')
    return '创建人'
  if (fieldName === 'createTime')
    return '创建时间'
  if (fieldName === 'updateBy')
    return '修改人'
  if (fieldName === 'updateTime')
    return '修改时间'
  if (fieldName === 'createDept')
    return '创建部门'
  return field.label || field.fieldName || fieldName
}

function businessFieldOptionLabel(field = {}) {
  const code = field.field || field.fieldCode || ''
  const label = businessFieldLabel(field)
  return label && code && label !== code ? `${label}（${code}）` : label || code
}

function relationLabel(relation) {
  return relationSentence(relation)
}

function canInlineEdit(relation = {}) {
  return ['CHILD_LIST', 'DETAIL'].includes(relation.relationType)
}

function firstSourceField(relationType = 'CHILD_LIST', targetObjectCode = '', currentValue = '') {
  if (relationType === 'REFERENCE') {
    const matched = findReferenceSourceField(targetObjectCode)
    if (matched)
      return matched.value
    if (currentValue && sourceFieldOptions.value.some(item => item.value === currentValue))
      return currentValue
  }
  else {
    const idField = sourceFieldOptions.value.find(item => item.value === 'id')?.value
    if (idField)
      return idField
    if (currentValue && sourceFieldOptions.value.some(item => item.value === currentValue))
      return currentValue
  }
  return sourceFieldOptions.value.find(item => item.value === 'id')?.value || sourceFieldOptions.value[0]?.value || ''
}

function findReferenceSourceField(targetObjectCode = '') {
  const targetObject = businessObjects.value.find(item => item.objectCode === targetObjectCode)
  const targetCode = lowerFirst(targetObjectCode || '')
  const targetName = targetObject?.objectName || ''
  const candidates = [
    `${targetCode}Id`,
    `${targetCode}Code`,
    targetObjectCode,
  ].filter(Boolean)
  return sourceFieldOptions.value.find(item => candidates.includes(item.value))
    || sourceFieldOptions.value.find((item) => {
      const label = item.label || ''
      return targetName && label.includes(targetName)
    })
}

function createClientKey() {
  return `relation_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function lowerFirst(value) {
  if (!value)
    return ''
  const normalized = String(value)
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '')
    .toLowerCase()
  return normalized.replace(/_([a-z0-9])/g, (_, char) => char.toUpperCase())
}

function toPageField(field) {
  return {
    ...field,
    field: field.field || field.fieldCode,
    label: field.label || field.fieldName || field.fieldCode,
    fieldStatus: field.fieldStatus,
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
  }
}

function toFieldPayload(field = {}) {
  return {
    fieldName: field.fieldName || field.label,
    fieldCode: field.fieldCode || field.field,
    columnName: field.columnName,
    fieldType: field.fieldType || field.businessFieldType,
    dataType: field.dataType,
    length: field.length,
    precision: field.precision,
    required: field.required,
    defaultValue: field.defaultValue,
    searchable: field.searchable,
    listVisible: field.listVisible,
    formVisible: field.formVisible,
    importable: field.importable,
    exportable: field.exportable,
    componentType: field.componentType,
    queryType: field.queryType,
    dictType: field.dictType,
    sensitiveType: field.sensitiveType,
    encryptAlgorithm: field.encryptAlgorithm,
    sortable: field.sortable,
    systemField: field.systemField,
    readonly: field.readonly,
    fieldStatus: field.fieldStatus,
    referenceObjectCode: field.referenceObjectCode,
    referenceDisplayField: field.referenceDisplayField,
    placeholder: field.basicProps?.placeholder || field.placeholder || '',
    remark: field.remark,
    sortOrder: field.sortOrder,
    fieldBinding: { ...(field.fieldBinding || {}) },
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
  }
}

function markDirty() {
  if (activeTab.value === 'linkage') {
    syncLinkageModel(true)
    return
  }
  markRelationDirty()
}

function markRelationDirty() {
  relationDirty.value = true
  emit('dirtyChange', true)
}

defineExpose({
  saveRelations,
  loadRelations,
})
</script>

<style scoped>
.business-relation-designer {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.relation-designer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.relation-designer-head h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.relation-designer-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.relation-designer-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.relation-tabs {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  overflow: hidden;
}

.relation-tabs :deep(.n-tabs-nav) {
  padding: 0 16px;
}

.relation-tabs :deep(.n-tabs-pane-wrapper),
.relation-tabs :deep(.n-tab-pane) {
  min-height: 0;
  overflow: hidden;
}

.relation-tabs :deep(.n-tab-pane) {
  height: 100%;
  padding: 0;
}

.relation-list-pane {
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  background: #f8fafc;
  padding: 14px;
}

.relation-list-pane :deep(.n-spin-container),
.relation-list-pane :deep(.n-spin-content) {
  min-height: 100%;
}

.relation-card-list {
  display: grid;
  gap: 12px;
}

.relation-card,
.relation-summary-pane section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.relation-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.relation-card-head > div {
  min-width: 0;
}

.relation-card-head strong,
.relation-summary-pane h4 {
  margin: 0;
  color: #111827;
  font-size: 14px;
  overflow-wrap: anywhere;
}

.relation-card-head p,
.relation-summary-pane p,
.relation-summary-pane li {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.relation-summary-pane {
  display: grid;
  align-content: start;
  gap: 12px;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  border-left: 1px solid #e5e7eb;
  background: #fbfcfe;
  padding: 12px;
}

.sentence-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.sentence-list span {
  display: block;
  border-radius: 4px;
  background: #eef6ff;
  color: #1d4ed8;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
  padding: 6px 8px;
}

.relation-summary-pane ul {
  margin: 10px 0 0;
  padding-left: 18px;
}

.metric-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.metric-list span {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #334155;
  font-size: 12px;
  padding: 8px;
}

.relation-selector-config {
  display: grid;
  gap: 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fbfdff;
  padding: 12px;
}

.relation-selector-config-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.relation-selector-config-head > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.relation-selector-config-head strong {
  color: #111827;
  font-size: 13px;
}

.relation-selector-config-head span {
  color: #64748b;
  font-size: 12px;
}

.selector-preset-bar,
.selector-structured-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 6px;
  background: #f8fafc;
  padding: 8px 10px;
}

.selector-preset-bar > div,
.selector-structured-head > div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.selector-preset-bar strong,
.selector-structured-head strong {
  color: #111827;
  font-size: 13px;
}

.selector-preset-bar span,
.selector-structured-head span {
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
}

.selector-structured-panel {
  display: grid;
  gap: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 10px;
}

.selector-mapping-list,
.selector-filter-list {
  display: grid;
  gap: 8px;
}

.selector-mapping-row,
.selector-filter-row {
  display: grid;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.selector-mapping-row {
  grid-template-columns: minmax(180px, 1fr) auto minmax(180px, 1fr) 32px;
}

.selector-filter-row {
  grid-template-columns: minmax(160px, 1fr) 132px minmax(160px, 1fr) 32px;
}

.selector-mapping-arrow {
  border-radius: 999px;
  background: #eef2ff;
  color: #334155;
  font-size: 12px;
  line-height: 24px;
  text-align: center;
  white-space: nowrap;
  padding: 0 8px;
}

@media (max-width: 1100px) {
  .relation-designer-body {
    grid-template-columns: 1fr;
  }

  .relation-summary-pane {
    border-left: 0;
    border-top: 1px solid #e5e7eb;
  }

  .selector-mapping-row,
  .selector-filter-row {
    grid-template-columns: 1fr;
  }

  .selector-mapping-arrow {
    width: max-content;
  }
}
</style>
