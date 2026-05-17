<template>
  <CollapseItem name="CRUD 表格" :expanded="true">
    <SettingItemBox name="接口">
      <SettingItem name="标题">
        <n-input v-model:value="optionData.title" size="small" />
      </SettingItem>
      <SettingItem name="副标题">
        <n-input v-model:value="optionData.subtitle" size="small" />
      </SettingItem>
      <SettingItem name="列表地址">
        <n-input v-model:value="optionData.api.listUrl" size="small" placeholder="/forge-report-api/xxx/page" />
      </SettingItem>
      <SettingItem name="请求方式">
        <n-select v-model:value="optionData.api.method" size="small" :options="methodOptions" />
      </SettingItem>
      <SettingItem name="数据路径">
        <n-input v-model:value="optionData.api.dataPath" size="small" />
      </SettingItem>
      <SettingItem name="总数路径">
        <n-input v-model:value="optionData.api.totalPath" size="small" />
      </SettingItem>
      <SettingItem name="接口预览">
        <n-button size="tiny" secondary type="primary" @click="openEditor('apiTest')">测试接口</n-button>
      </SettingItem>
    </SettingItemBox>

    <SettingItemBox name="可视化配置">
      <SettingItem name="查询条件">
        <div class="setting-line">
          <n-button size="tiny" secondary type="primary" @click="openEditor('searchFields')">配置</n-button>
          <span class="setting-count">{{ optionData.searchFields?.length || 0 }} 项</span>
        </div>
      </SettingItem>
      <SettingItem name="表格列">
        <div class="setting-line">
          <n-button size="tiny" secondary type="primary" @click="openEditor('columns')">配置</n-button>
          <span class="setting-count">{{ optionData.columns?.length || 0 }} 列</span>
        </div>
      </SettingItem>
      <SettingItem name="行操作">
        <div class="setting-line">
          <n-button size="tiny" secondary type="primary" @click="openEditor('actions')">配置</n-button>
          <span class="setting-count">{{ optionData.actions?.length || 0 }} 个</span>
        </div>
      </SettingItem>
      <SettingItem name="上下文参数">
        <div class="setting-line">
          <n-button size="tiny" secondary type="primary" @click="openEditor('contextParamMap')">配置</n-button>
          <span class="setting-count">{{ recordCount(optionData.contextParamMap) }} 个</span>
        </div>
      </SettingItem>
      <SettingItem name="配置源码">
        <n-button size="tiny" secondary @click="openEditor('json')">查看 JSON</n-button>
      </SettingItem>
    </SettingItemBox>

    <SettingItemBox name="显示">
      <SettingItem name="每页条数">
        <n-input-number v-model:value="optionData.pageSize" size="small" :min="1" :max="100" />
      </SettingItem>
      <SettingItem name="工具栏">
        <n-switch v-model:value="optionData.showToolbar" size="small" />
      </SettingItem>
      <SettingItem name="查询区">
        <n-switch v-model:value="optionData.showSearch" size="small" />
      </SettingItem>
      <SettingItem name="序号列">
        <n-switch v-model:value="optionData.showIndex" size="small" />
      </SettingItem>
      <SettingItem name="操作列">
        <n-switch v-model:value="optionData.showActions" size="small" />
      </SettingItem>
    </SettingItemBox>

    <SettingItemBox name="样式">
      <SettingItem name="主色">
        <n-color-picker v-model:value="optionData.style.accentColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="文字">
        <n-color-picker v-model:value="optionData.style.textColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="面板">
        <n-color-picker v-model:value="optionData.style.panelColor" size="small" :modes="['hex', 'rgb']" />
      </SettingItem>
      <SettingItem name="字号">
        <n-input-number v-model:value="optionData.style.fontSize" size="small" :min="10" :max="24" />
      </SettingItem>
    </SettingItemBox>
  </CollapseItem>

  <n-modal
    v-model:show="editorVisible"
    preset="card"
    :title="editorTitle"
    :bordered="false"
    :segmented="{ content: true, footer: true }"
    class="crud-config-modal"
  >
    <template v-if="editorMode === 'searchFields'">
      <div class="modal-toolbar">
        <span>配置查询区字段，字段名会作为接口查询参数传递。</span>
        <n-button size="small" type="primary" @click="addSearchField">新增条件</n-button>
      </div>
      <div class="config-list">
        <div v-for="(field, index) in optionData.searchFields" :key="index" class="config-card">
          <div class="config-card__head">
            <strong>查询条件 {{ index + 1 }}</strong>
            <div class="config-actions">
              <n-button size="tiny" quaternary :disabled="index === 0" @click="moveItem(optionData.searchFields, index, -1)">
                上移
              </n-button>
              <n-button
                size="tiny"
                quaternary
                :disabled="index === optionData.searchFields.length - 1"
                @click="moveItem(optionData.searchFields, index, 1)"
              >
                下移
              </n-button>
              <n-button size="tiny" quaternary type="error" @click="removeItem(optionData.searchFields, index)">
                删除
              </n-button>
            </div>
          </div>
          <n-grid :cols="2" :x-gap="12" :y-gap="10" responsive="screen">
            <n-grid-item>
              <div class="field-label">显示名称</div>
              <n-input v-model:value="field.label" size="small" placeholder="如：关键词" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">字段名</div>
              <n-input v-model:value="field.field" size="small" placeholder="如：keyword" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">控件类型</div>
              <n-select v-model:value="field.type" size="small" :options="fieldTypeOptions" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">占位提示</div>
              <n-input v-model:value="field.placeholder" size="small" placeholder="输入框提示" />
            </n-grid-item>
            <n-grid-item v-if="field.type === 'dateRange' || field.type === 'numberRange'">
              <div class="field-label">开始参数名</div>
              <n-input v-model:value="field.startKey" size="small" placeholder="默认自动生成" />
            </n-grid-item>
            <n-grid-item v-if="field.type === 'dateRange' || field.type === 'numberRange'">
              <div class="field-label">结束参数名</div>
              <n-input v-model:value="field.endKey" size="small" placeholder="默认自动生成" />
            </n-grid-item>
            <n-grid-item v-if="isOptionField(field.type)">
              <div class="field-label">字典类型</div>
              <n-input v-model:value="field.dictType" size="small" placeholder="sys_xxx" />
            </n-grid-item>
          </n-grid>
          <div v-if="isOptionField(field.type)" class="nested-editor">
            <div class="nested-editor__head">
              <span>固定选项</span>
              <n-button size="tiny" secondary @click="addOption(field)">新增选项</n-button>
            </div>
            <div v-for="(item, optionIndex) in field.options || []" :key="optionIndex" class="option-row">
              <n-input v-model:value="item.label" size="small" placeholder="显示文本" />
              <n-input v-model:value="item.value" size="small" placeholder="实际值" />
              <n-button size="tiny" quaternary type="error" @click="removeItem(field.options, optionIndex)">删除</n-button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="editorMode === 'columns'">
      <div class="modal-toolbar">
        <span>配置表格列。枚举列可绑定字典，也可直接维护固定选项。</span>
        <n-button size="small" type="primary" @click="addColumn">新增列</n-button>
      </div>
      <div class="config-list">
        <div v-for="(column, index) in optionData.columns" :key="index" class="config-card">
          <div class="config-card__head">
            <strong>表格列 {{ index + 1 }}</strong>
            <div class="config-actions">
              <n-button size="tiny" quaternary :disabled="index === 0" @click="moveItem(optionData.columns, index, -1)">
                上移
              </n-button>
              <n-button
                size="tiny"
                quaternary
                :disabled="index === optionData.columns.length - 1"
                @click="moveItem(optionData.columns, index, 1)"
              >
                下移
              </n-button>
              <n-button size="tiny" quaternary type="error" @click="removeItem(optionData.columns, index)">
                删除
              </n-button>
            </div>
          </div>
          <n-grid :cols="2" :x-gap="12" :y-gap="10" responsive="screen">
            <n-grid-item>
              <div class="field-label">列标题</div>
              <n-input v-model:value="column.title" size="small" placeholder="如：名称" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">字段名</div>
              <n-input v-model:value="column.key" size="small" placeholder="如：name" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">列类型</div>
              <n-select v-model:value="column.type" size="small" :options="columnTypeOptions" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">列宽</div>
              <n-input-number v-model:value="column.width" size="small" :min="60" :max="600" clearable />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">对齐</div>
              <n-select v-model:value="column.align" size="small" :options="alignOptions" />
            </n-grid-item>
            <n-grid-item v-if="column.type === 'dict'">
              <div class="field-label">字典类型</div>
              <n-input v-model:value="column.dictType" size="small" placeholder="sys_xxx" />
            </n-grid-item>
            <n-grid-item v-if="column.type === 'money'">
              <div class="field-label">金额单位</div>
              <n-select v-model:value="column.moneyUnit" size="small" :options="moneyUnitOptions" />
            </n-grid-item>
            <n-grid-item v-if="column.type === 'link'">
              <div class="field-label">链接模板</div>
              <n-input v-model:value="column.urlTemplate" size="small" placeholder="/detail?id=${id}" />
            </n-grid-item>
            <n-grid-item v-if="column.type === 'link'">
              <div class="field-label">打开方式</div>
              <n-select v-model:value="column.openTarget" size="small" :options="openTargetOptions" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">文本省略</div>
              <n-switch v-model:value="column.ellipsis" size="small" />
            </n-grid-item>
          </n-grid>
          <div v-if="column.type === 'dict'" class="nested-editor">
            <div class="nested-editor__head">
              <span>固定枚举</span>
              <n-button size="tiny" secondary @click="addOption(column)">新增枚举</n-button>
            </div>
            <div v-for="(item, optionIndex) in column.options || []" :key="optionIndex" class="option-row">
              <n-input v-model:value="item.label" size="small" placeholder="显示文本" />
              <n-input v-model:value="item.value" size="small" placeholder="实际值" />
              <n-button size="tiny" quaternary type="error" @click="removeItem(column.options, optionIndex)">删除</n-button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="editorMode === 'actions'">
      <div class="modal-toolbar">
        <span>配置每行右侧操作。下钻和弹窗参数通过“参数映射”传给目标页面。</span>
        <n-button size="small" type="primary" @click="addAction">新增操作</n-button>
      </div>
      <div class="config-list">
        <div v-for="(action, index) in optionData.actions" :key="index" class="config-card">
          <div class="config-card__head">
            <strong>行操作 {{ index + 1 }}</strong>
            <div class="config-actions">
              <n-button size="tiny" quaternary :disabled="index === 0" @click="moveItem(optionData.actions, index, -1)">
                上移
              </n-button>
              <n-button
                size="tiny"
                quaternary
                :disabled="index === optionData.actions.length - 1"
                @click="moveItem(optionData.actions, index, 1)"
              >
                下移
              </n-button>
              <n-button size="tiny" quaternary type="error" @click="removeItem(optionData.actions, index)">
                删除
              </n-button>
            </div>
          </div>
          <n-grid :cols="2" :x-gap="12" :y-gap="10" responsive="screen">
            <n-grid-item>
              <div class="field-label">按钮文字</div>
              <n-input v-model:value="action.label" size="small" placeholder="如：查看" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">操作类型</div>
              <n-select v-model:value="action.type" size="small" :options="actionTypeOptions" />
            </n-grid-item>
            <n-grid-item v-if="action.type === 'goPage' || action.type === 'openModal'">
              <div class="field-label">{{ action.type === 'openModal' ? '目标弹窗' : '目标页面' }}</div>
              <n-select
                v-model:value="action.targetPageId"
                size="small"
                filterable
                clearable
                placeholder="选择项目页面"
                :options="action.type === 'openModal' ? modalPageOptions : normalPageOptions"
              />
            </n-grid-item>
            <n-grid-item v-if="action.type === 'link' || action.type === 'request'">
              <div class="field-label">{{ action.type === 'link' ? '链接地址' : '接口地址' }}</div>
              <n-input v-model:value="action.url" size="small" placeholder="/forge-report-api/xxx/{id}" />
            </n-grid-item>
            <n-grid-item v-if="action.type === 'request'">
              <div class="field-label">请求方式</div>
              <n-select v-model:value="action.method" size="small" :options="requestMethodOptions" />
            </n-grid-item>
            <n-grid-item v-if="action.type === 'link'">
              <div class="field-label">打开方式</div>
              <n-select v-model:value="action.openTarget" size="small" :options="openTargetOptions" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">按钮样式</div>
              <n-select v-model:value="action.style" size="small" :options="actionStyleOptions" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">执行后</div>
              <n-select v-model:value="action.afterAction" size="small" :options="afterActionOptions" />
            </n-grid-item>
            <n-grid-item v-if="action.afterAction === 'goPage' || action.afterAction === 'openModal'">
              <div class="field-label">执行后目标</div>
              <n-select
                v-model:value="action.afterTargetPageId"
                size="small"
                filterable
                clearable
                :options="action.afterAction === 'openModal' ? modalPageOptions : normalPageOptions"
              />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">确认操作</div>
              <n-switch v-model:value="action.confirm" size="small" />
            </n-grid-item>
            <n-grid-item v-if="action.confirm">
              <div class="field-label">确认文案</div>
              <n-input v-model:value="action.confirmText" size="small" placeholder="确认执行该操作？" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">显示条件</div>
              <n-input v-model:value="action.visibleWhen" size="small" placeholder="如 status === '1'" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">禁用条件</div>
              <n-input v-model:value="action.disabledWhen" size="small" placeholder="如 status === '0'" />
            </n-grid-item>
          </n-grid>
          <div class="nested-editor">
            <div class="nested-editor__head">
              <span>参数映射</span>
              <n-button size="tiny" secondary @click="addRecordEntry(action, 'paramMap')">新增映射</n-button>
            </div>
            <div v-for="(entry, entryIndex) in recordEntries(action.paramMap)" :key="entryIndex" class="option-row">
              <n-input
                :value="entry[0]"
                size="small"
                placeholder="目标参数，如 id"
                @update:value="setRecordEntry(action, 'paramMap', entryIndex, 'key', $event)"
              />
              <n-input
                :value="entry[1]"
                size="small"
                placeholder="行字段，如 id"
                @update:value="setRecordEntry(action, 'paramMap', entryIndex, 'value', $event)"
              />
              <n-button size="tiny" quaternary type="error" @click="removeRecordEntry(action, 'paramMap', entryIndex)">
                删除
              </n-button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="editorMode === 'contextParamMap'">
      <div class="modal-toolbar">
        <span>把弹窗或下钻页面收到的上下文参数带入列表查询。</span>
        <n-button size="small" type="primary" @click="addRecordEntry(optionData, 'contextParamMap')">新增参数</n-button>
      </div>
      <div class="config-card">
        <div class="option-row option-row--header">
          <span>接口参数名</span>
          <span>上下文字段名</span>
          <span></span>
        </div>
        <div v-for="(entry, entryIndex) in recordEntries(optionData.contextParamMap)" :key="entryIndex" class="option-row">
          <n-input
            :value="entry[0]"
            size="small"
            placeholder="接口参数，如 projectId"
            @update:value="setRecordEntry(optionData, 'contextParamMap', entryIndex, 'key', $event)"
          />
          <n-input
            :value="entry[1]"
            size="small"
            placeholder="上下文字段，如 id"
            @update:value="setRecordEntry(optionData, 'contextParamMap', entryIndex, 'value', $event)"
          />
          <n-button size="tiny" quaternary type="error" @click="removeRecordEntry(optionData, 'contextParamMap', entryIndex)">
            删除
          </n-button>
        </div>
      </div>
    </template>

    <template v-else-if="editorMode === 'apiTest'">
      <div class="modal-toolbar">
        <span>按当前列表接口和解析路径拉取第一页数据。</span>
        <n-button size="small" type="primary" :loading="apiTesting" @click="testApi">重新测试</n-button>
      </div>
      <div v-if="apiTestError" class="api-test-error">{{ apiTestError }}</div>
      <div v-else class="api-test-result">
        <div class="api-test-stat">
          <span>解析条数：{{ apiTestRows.length }}</span>
          <span>解析总数：{{ apiTestTotal }}</span>
        </div>
        <n-input :value="apiTestPreview" type="textarea" readonly :autosize="{ minRows: 14, maxRows: 24 }" />
      </div>
    </template>

    <template v-else>
      <div class="json-viewer">
        <div class="modal-toolbar">
          <span>当前配置的只读 JSON，方便复制排查。</span>
          <n-button size="small" secondary @click="copyJson">复制 JSON</n-button>
        </div>
        <n-input :value="jsonPreview" type="textarea" readonly :autosize="{ minRows: 18, maxRows: 28 }" />
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { computed, PropType, ref } from 'vue'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { get, post } from '@/api/http'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { option } from './config'

type EditorMode = 'searchFields' | 'columns' | 'actions' | 'contextParamMap' | 'apiTest' | 'json'
type RecordSide = 'key' | 'value'

const props = defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

const editorVisible = ref(false)
const editorMode = ref<EditorMode>('searchFields')
const chartEditStore = useChartEditStore()
const apiTesting = ref(false)
const apiTestRows = ref<any[]>([])
const apiTestTotal = ref(0)
const apiTestError = ref('')

const methodOptions = [
  { label: 'GET', value: 'get' },
  { label: 'POST', value: 'post' }
]

const requestMethodOptions = [
  { label: 'GET', value: 'get' },
  { label: 'POST', value: 'post' },
  { label: 'PUT', value: 'put' },
  { label: 'DELETE', value: 'delete' }
]

const fieldTypeOptions = [
  { label: '输入框', value: 'input' },
  { label: '下拉选择', value: 'select' },
  { label: '日期', value: 'date' },
  { label: '日期范围', value: 'dateRange' },
  { label: '数字', value: 'number' },
  { label: '数字范围', value: 'numberRange' },
  { label: '多选', value: 'multiSelect' },
  { label: '单选', value: 'radio' },
  { label: '开关', value: 'switch' }
]

const columnTypeOptions = [
  { label: '文本', value: 'text' },
  { label: '枚举标签', value: 'dict' },
  { label: '日期', value: 'date' },
  { label: '金额', value: 'money' },
  { label: '图片', value: 'image' },
  { label: '链接', value: 'link' },
  { label: '进度条', value: 'progress' },
  { label: '开关状态', value: 'switch' }
]

const alignOptions = [
  { label: '居左', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '居右', value: 'right' }
]

const actionTypeOptions = [
  { label: '跳转页面', value: 'goPage' },
  { label: '打开弹窗', value: 'openModal' },
  { label: '关闭弹窗', value: 'closeModal' },
  { label: '打开链接', value: 'link' },
  { label: '调用接口', value: 'request' }
]

const openTargetOptions = [
  { label: '当前页', value: '_self' },
  { label: '新窗口', value: '_blank' }
]

const actionStyleOptions = [
  { label: '主要', value: 'primary' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '危险', value: 'error' },
  { label: '信息', value: 'info' }
]

const moneyUnitOptions = [
  { label: '元', value: 'yuan' },
  { label: '分', value: 'cent' }
]

const afterActionOptions = [
  { label: '不处理', value: 'none' },
  { label: '刷新列表', value: 'refresh' },
  { label: '关闭弹窗', value: 'closeModal' },
  { label: '跳转页面', value: 'goPage' },
  { label: '打开弹窗', value: 'openModal' }
]

const normalPageOptions = computed(() =>
  chartEditStore.getProjectPages
    .filter(page => page.pageType !== 'modal')
    .map(page => ({
      label: `${page.name}${page.id === chartEditStore.getHomePageId ? '（首页）' : ''}`,
      value: page.id
    }))
)

const modalPageOptions = computed(() =>
  chartEditStore.getProjectPages
    .filter(page => page.pageType === 'modal')
    .map(page => ({
      label: page.name,
      value: page.id
    }))
)

const editorTitle = computed(() => {
  const titleMap: Record<EditorMode, string> = {
    searchFields: '配置查询条件',
    columns: '配置表格列',
    actions: '配置行操作',
    contextParamMap: '配置上下文参数',
    apiTest: '接口测试预览',
    json: '查看 CRUD 配置 JSON'
  }
  return titleMap[editorMode.value]
})

const jsonPreview = computed(() =>
  JSON.stringify(
    {
      api: props.optionData.api,
      contextParamMap: props.optionData.contextParamMap,
      searchFields: props.optionData.searchFields,
      columns: props.optionData.columns,
      actions: props.optionData.actions,
      staticRows: props.optionData.staticRows
    },
    null,
    2
  )
)

const openEditor = (mode: EditorMode) => {
  editorMode.value = mode
  editorVisible.value = true
  if (mode === 'apiTest') testApi()
}

const getByPath = (target: any, path?: string) => {
  if (!path) return target
  return path.split('.').reduce((current, key) => current?.[key], target)
}

const normalizeRows = (payload: any) => {
  const data = props.optionData.api.dataPath ? getByPath(payload, props.optionData.api.dataPath) : payload?.data
  if (Array.isArray(data)) return data
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload)) return payload
  return []
}

const normalizeTotal = (payload: any, rows: any[]) => {
  const value = props.optionData.api.totalPath ? getByPath(payload, props.optionData.api.totalPath) : undefined
  return Number(value ?? payload?.data?.total ?? payload?.total ?? rows.length)
}

const apiTestPreview = computed(() => JSON.stringify(apiTestRows.value.slice(0, 5), null, 2))

const testApi = async () => {
  apiTestError.value = ''
  apiTestRows.value = []
  apiTestTotal.value = 0
  if (!props.optionData.api.listUrl) {
    apiTestError.value = '请先配置列表地址'
    return
  }
  apiTesting.value = true
  try {
    const params = {
      [props.optionData.api.pageNumKey || 'pageNum']: 1,
      [props.optionData.api.pageSizeKey || 'pageSize']: props.optionData.pageSize || 10
    }
    const method = props.optionData.api.method === 'post' ? post : get
    const res = await method(props.optionData.api.listUrl, params)
    const rows = normalizeRows(res)
    apiTestRows.value = rows
    apiTestTotal.value = normalizeTotal(res, rows)
  } catch (error: any) {
    apiTestError.value = error?.message || '接口测试失败'
  } finally {
    apiTesting.value = false
  }
}

const copyJson = async () => {
  await navigator.clipboard?.writeText(jsonPreview.value)
  window['$message']?.success('已复制 JSON')
}

const isOptionField = (type: string) => ['select', 'multiSelect', 'radio'].includes(type)

const addSearchField = () => {
  if (!props.optionData.searchFields) props.optionData.searchFields = []
  props.optionData.searchFields.push({
    label: '新条件',
    field: `field${props.optionData.searchFields.length + 1}`,
    type: 'input',
    placeholder: '请输入'
  })
}

const addColumn = () => {
  if (!props.optionData.columns) props.optionData.columns = []
  props.optionData.columns.push({
    title: '新列',
    key: `field${props.optionData.columns.length + 1}`,
    type: 'text',
    width: 120,
    align: 'left',
    ellipsis: true
  })
}

const addAction = () => {
  if (!props.optionData.actions) props.optionData.actions = []
  props.optionData.actions.push({
    label: '查看',
    type: 'openModal',
    style: 'primary',
    afterAction: 'none',
    paramMap: { id: 'id' }
  })
}

const addOption = (target: { options?: Array<{ label: string; value: string | number }> }) => {
  if (!target.options) target.options = []
  target.options.push({ label: '新选项', value: '' })
}

const removeItem = (list: any[] | undefined, index: number) => {
  list?.splice(index, 1)
}

const moveItem = (list: any[], index: number, direction: -1 | 1) => {
  const nextIndex = index + direction
  if (nextIndex < 0 || nextIndex >= list.length) return
  const [item] = list.splice(index, 1)
  list.splice(nextIndex, 0, item)
}

const recordEntries = (record?: Record<string, string>) => Object.entries(record || {})

const recordCount = (record?: Record<string, string>) => recordEntries(record).length

const addRecordEntry = (target: any, key: 'paramMap' | 'contextParamMap') => {
  const record = { ...(target[key] || {}) }
  let index = Object.keys(record).length + 1
  let nextKey = `param${index}`
  while (record[nextKey] !== undefined) {
    index += 1
    nextKey = `param${index}`
  }
  record[nextKey] = ''
  target[key] = record
}

const setRecordEntry = (target: any, key: 'paramMap' | 'contextParamMap', index: number, side: RecordSide, value: string) => {
  const entries = recordEntries(target[key])
  const current = entries[index] || ['', '']
  entries[index] = side === 'key' ? [value, current[1]] : [current[0], value]
  target[key] = Object.fromEntries(entries.filter(([entryKey]) => entryKey))
}

const removeRecordEntry = (target: any, key: 'paramMap' | 'contextParamMap', index: number) => {
  const entries = recordEntries(target[key])
  entries.splice(index, 1)
  target[key] = Object.fromEntries(entries)
}
</script>

<style scoped lang="scss">
.setting-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.setting-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.56);
}

:deep(.crud-config-modal) {
  width: min(980px, 92vw);
}

.modal-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 36px;
  margin-bottom: 14px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
}

.config-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: min(64vh, 680px);
  padding-right: 4px;
  overflow: auto;
}

.config-card {
  padding: 14px;
  border: 1px solid rgba(120, 172, 255, 0.18);
  border-radius: 8px;
  background: rgba(13, 20, 38, 0.72);
}

.config-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;

  strong {
    color: rgba(255, 255, 255, 0.88);
    font-size: 13px;
  }
}

.config-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.field-label {
  margin-bottom: 5px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 12px;
}

.nested-editor {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.nested-editor__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 12px;
}

.option-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) 58px;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.option-row--header {
  margin-bottom: 10px;
  color: rgba(255, 255, 255, 0.52);
  font-size: 12px;
}

.json-viewer {
  :deep(.n-input__textarea-el) {
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
    font-size: 12px;
    line-height: 1.6;
  }
}

.api-test-error {
  padding: 16px;
  color: #ff8a8a;
  border: 1px solid rgba(255, 120, 120, 0.24);
  border-radius: 8px;
  background: rgba(80, 18, 26, 0.32);
}

.api-test-stat {
  display: flex;
  gap: 16px;
  margin-bottom: 10px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}

.api-test-result {
  :deep(.n-input__textarea-el) {
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
    font-size: 12px;
    line-height: 1.6;
  }
}
</style>
