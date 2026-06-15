<template>
  <n-modal
    v-model:show="visible"
    :title="panelTitle"
    preset="card"
    style="width: min(1500px, calc(100vw - 48px))"
    :mask-closable="false"
  >
    <div class="app-code-workbench">
      <section class="code-toolbar">
        <div class="toolbar-meta">
          <n-tag :bordered="false" type="info">
            下载代码
          </n-tag>
          <span>{{ app?.suiteName || app?.suiteCode || '业务域' }}</span>
          <span>/</span>
          <span>{{ app?.objectName || app?.objectCode || '业务单元' }}</span>
        </div>
        <n-space>
          <n-button secondary :loading="saving" @click="saveOptions">
            <template #icon>
              <n-icon><SaveOutline /></n-icon>
            </template>
            保存设置
          </n-button>
          <n-button secondary :loading="previewing" @click="previewCode">
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
            刷新预览
          </n-button>
          <n-button type="primary" :loading="downloading" @click="downloadCode">
            <template #icon>
              <n-icon><DownloadOutline /></n-icon>
            </template>
            下载代码
          </n-button>
        </n-space>
      </section>

      <n-collapse :default-expanded-names="['settings']" class="settings-collapse">
        <n-collapse-item title="代码包设置" name="settings">
          <n-form label-placement="top" :model="form">
            <n-grid :cols="4" :x-gap="12" :y-gap="4" responsive="screen">
              <n-form-item-gi label="来源版本">
                <n-select v-model:value="form.sourceType" :options="sourceTypeOptions" />
              </n-form-item-gi>
              <n-form-item-gi v-if="form.sourceType === 'VERSION'" label="版本 ID">
                <n-input-number v-model:value="form.versionId" :show-button="false" placeholder="输入版本 ID" />
              </n-form-item-gi>
              <n-form-item-gi label="业务接口前缀" :span="form.sourceType === 'VERSION' ? 2 : 3">
                <n-input v-model:value="form.businessApiBase" placeholder="/crm/customer" />
              </n-form-item-gi>
              <n-form-item-gi label="Java 基础包名">
                <n-input v-model:value="form.domainPackage" placeholder="com.mdframe.forge" />
              </n-form-item-gi>
              <n-form-item-gi label="模块名">
                <n-input v-model:value="form.moduleName" placeholder="crm" />
              </n-form-item-gi>
              <n-form-item-gi label="作者">
                <n-input v-model:value="form.author" placeholder="Forge Generator" />
              </n-form-item-gi>
              <n-form-item-gi label="前端输出路径">
                <n-input v-model:value="form.frontendBasePath" placeholder="frontend/src/views" />
              </n-form-item-gi>
            </n-grid>
            <n-form-item label="包含内容">
              <n-space>
                <n-checkbox v-model:checked="form.includeSql">
                  SQL
                </n-checkbox>
                <n-checkbox v-model:checked="form.includeMenuSql">
                  菜单 SQL
                </n-checkbox>
                <n-checkbox v-model:checked="form.includeDictSql">
                  字典 SQL
                </n-checkbox>
              </n-space>
            </n-form-item>
          </n-form>
        </n-collapse-item>
      </n-collapse>

      <n-spin :show="previewing || loadingOptions">
        <div class="preview-container">
          <aside class="file-list">
            <div class="file-list-header">
              <span>目录</span>
              <span>{{ fileCount }} 个文件</span>
            </div>
            <n-tree
              block-line
              expand-on-click
              :data="fileTree"
              :expanded-keys="expandedKeys"
              :selected-keys="selectedKeys"
              :render-label="renderTreeLabel"
              @update:expanded-keys="keys => expandedKeys = keys"
              @update:selected-keys="handleTreeSelect"
            />
          </aside>

          <section class="code-viewer">
            <div class="code-header">
              <span class="file-name">{{ selectedFile || '请选择文件' }}</span>
              <n-button size="small" :disabled="!selectedFile" @click="copySelectedFile">
                <template #icon>
                  <n-icon><CopyOutline /></n-icon>
                </template>
                复制
              </n-button>
            </div>
            <div v-if="selectedFile" ref="editorContainer" class="editor-container" />
            <n-empty v-else class="preview-empty" description="暂无可预览文件" />
          </section>
        </div>
      </n-spin>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="visible = false">
          关闭
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { java } from '@codemirror/lang-java'
import { javascript } from '@codemirror/lang-javascript'
import { sql } from '@codemirror/lang-sql'
import { xml } from '@codemirror/lang-xml'
import { oneDark } from '@codemirror/theme-one-dark'
import { CopyOutline, DownloadOutline, RefreshOutline, SaveOutline } from '@vicons/ionicons5'
import { basicSetup, EditorView } from 'codemirror'
import { useMessage } from 'naive-ui'
import { computed, h, nextTick, onUnmounted, reactive, ref, watch } from 'vue'
import {
  businessAppCodeOptions,
  businessAppCodePreview,
  businessDownloadAppCode,
  businessSaveAppCodeOptions,
} from '@/api/business-app'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  app: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:show'])
const message = useMessage()
const saving = ref(false)
const previewing = ref(false)
const downloading = ref(false)
const loadingOptions = ref(false)
const fileMap = ref({})
const fileTree = ref([])
const expandedKeys = ref([])
const selectedFile = ref('')
const editorContainer = ref(null)
const form = reactive(defaultForm())
let editorView = null

const sourceTypeOptions = [
  { label: '当前草稿', value: 'DRAFT' },
  { label: '已发布版本', value: 'PUBLISHED' },
  { label: '指定版本', value: 'VERSION' },
]

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value),
})
const panelTitle = computed(() => `${props.app?.appName || '访问入口'}功能代码`)
const fileCount = computed(() => Object.keys(fileMap.value).length)
const selectedKeys = computed(() => selectedFile.value ? [selectedFile.value] : [])

watch(() => props.show, async (value) => {
  if (value) {
    await openWorkbench()
    return
  }
  destroyEditor()
})

watch(() => props.app?.id, async () => {
  if (props.show)
    await openWorkbench()
})

async function openWorkbench() {
  resetWorkbench()
  await loadOptions()
  await previewCode()
}

function resetWorkbench() {
  Object.assign(form, defaultForm())
  fileMap.value = {}
  fileTree.value = []
  expandedKeys.value = []
  selectedFile.value = ''
  destroyEditor()
}

async function loadOptions() {
  if (!props.app?.id)
    return
  loadingOptions.value = true
  try {
    const res = await businessAppCodeOptions(props.app.id)
    Object.assign(form, defaultForm(), res.data || {})
  }
  finally {
    loadingOptions.value = false
  }
}

async function saveOptions() {
  if (!props.app?.id)
    return
  saving.value = true
  try {
    await businessSaveAppCodeOptions(props.app.id, buildPayload())
    message.success('代码包设置已保存')
    await previewCode()
  }
  finally {
    saving.value = false
  }
}

async function previewCode() {
  if (!props.app?.id)
    return
  previewing.value = true
  try {
    const res = await businessAppCodePreview(props.app.id, buildPayload())
    const files = res.data?.files || {}
    applyPreviewFiles(files)
  }
  finally {
    previewing.value = false
  }
}

async function downloadCode() {
  if (!props.app?.id)
    return
  downloading.value = true
  try {
    const blob = await businessDownloadAppCode(props.app.id, buildPayload())
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${props.app.appCode || props.app.configKey || 'app'}-code.zip`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  }
  finally {
    downloading.value = false
  }
}

function applyPreviewFiles(files) {
  fileMap.value = files || {}
  const keys = Object.keys(fileMap.value).sort((a, b) => a.localeCompare(b))
  const treeResult = buildFileTree(keys)
  fileTree.value = treeResult.tree
  expandedKeys.value = treeResult.expandedKeys
  selectedFile.value = keys[0] || ''
  nextTick(() => {
    if (selectedFile.value)
      initEditor(fileMap.value[selectedFile.value], selectedFile.value)
    else
      destroyEditor()
  })
}

function buildFileTree(paths) {
  const root = []
  const nodeMap = new Map()
  const directoryKeys = []
  paths.forEach((path) => {
    const parts = path.split('/').filter(Boolean)
    let currentChildren = root
    let currentPath = ''
    parts.forEach((part, index) => {
      const isFile = index === parts.length - 1
      currentPath = currentPath ? `${currentPath}/${part}` : part
      const key = isFile ? path : `dir:${currentPath}`
      let node = nodeMap.get(key)
      if (!node) {
        node = {
          key,
          label: part,
          type: isFile ? 'file' : 'directory',
          children: isFile ? undefined : [],
        }
        nodeMap.set(key, node)
        currentChildren.push(node)
        if (!isFile)
          directoryKeys.push(key)
      }
      if (!isFile)
        currentChildren = node.children
    })
  })
  sortTree(root)
  return { tree: root, expandedKeys: directoryKeys }
}

function sortTree(nodes) {
  nodes.sort((a, b) => {
    if (a.type !== b.type)
      return a.type === 'directory' ? -1 : 1
    return a.label.localeCompare(b.label)
  })
  nodes.forEach((node) => {
    if (node.children)
      sortTree(node.children)
  })
}

function renderTreeLabel({ option }) {
  return h('span', { class: ['tree-node-label', option.type === 'directory' ? 'is-directory' : 'is-file'] }, [
    h('i', {
      class: option.type === 'directory'
        ? 'i-material-symbols:folder-outline-rounded'
        : 'i-material-symbols:description-outline-rounded',
    }),
    h('span', option.label),
  ])
}

async function handleTreeSelect(keys) {
  const key = keys?.[0]
  if (!key || !fileMap.value[key])
    return
  selectedFile.value = key
  await nextTick()
  initEditor(fileMap.value[key], key)
}

function getLanguageSupport(filename) {
  if (filename.endsWith('.java'))
    return java()
  if (filename.endsWith('.xml'))
    return xml()
  if (filename.endsWith('.vue') || filename.endsWith('.js') || filename.endsWith('.ts'))
    return javascript()
  if (filename.endsWith('.sql'))
    return sql()
  return null
}

function initEditor(code, filename) {
  destroyEditor()
  if (!editorContainer.value)
    return
  const extensions = [
    basicSetup,
    oneDark,
    EditorView.editable.of(false),
  ]
  const languageSupport = getLanguageSupport(filename)
  if (languageSupport)
    extensions.splice(1, 0, languageSupport)
  editorView = new EditorView({
    doc: code || '',
    extensions,
    parent: editorContainer.value,
  })
}

function destroyEditor() {
  if (editorView) {
    editorView.destroy()
    editorView = null
  }
}

function copySelectedFile() {
  const code = fileMap.value[selectedFile.value]
  if (!code)
    return
  navigator.clipboard.writeText(code)
    .then(() => message.success('复制成功'))
    .catch(() => message.error('复制失败'))
}

function buildPayload() {
  return {
    sourceType: form.sourceType || 'DRAFT',
    versionId: form.sourceType === 'VERSION' ? form.versionId : null,
    businessApiBase: form.businessApiBase,
    groupId: form.groupId,
    domainPackage: form.domainPackage,
    moduleName: form.moduleName,
    author: form.author,
    includeSql: form.includeSql,
    includeMenuSql: form.includeMenuSql,
    includeDictSql: form.includeDictSql,
    frontendBasePath: form.frontendBasePath,
  }
}

function defaultForm() {
  return {
    sourceType: 'DRAFT',
    versionId: null,
    businessApiBase: '',
    groupId: '',
    domainPackage: '',
    moduleName: '',
    author: '',
    includeSql: true,
    includeMenuSql: true,
    includeDictSql: true,
    frontendBasePath: 'frontend/src/views',
  }
}

onUnmounted(() => {
  destroyEditor()
})
</script>

<style scoped>
.app-code-workbench {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 12px;
  height: min(78vh, 820px);
  min-height: 560px;
  overflow: hidden;
}

.code-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 10px;
}

.toolbar-meta {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.settings-collapse {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 0 12px;
}

.app-code-workbench :deep(.n-spin-container),
.app-code-workbench :deep(.n-spin-content) {
  height: 100%;
  min-height: 0;
}

.preview-container {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.file-list {
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: auto;
  border-right: 1px solid #e5e7eb;
  background: #fafafa;
}

.file-list-header {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  padding: 10px 12px;
}

.file-list :deep(.n-tree) {
  padding: 8px;
}

.file-list :deep(.tree-node-label) {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}

.file-list :deep(.tree-node-label i) {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 16px;
}

.file-list :deep(.tree-node-label span) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-viewer {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
  background: #0f172a;
}

.code-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #1e293b;
  background: #111827;
  padding: 9px 12px;
}

.file-name {
  overflow: hidden;
  color: #e5e7eb;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.editor-container {
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: auto;
}

.editor-container :deep(.cm-editor) {
  height: 100%;
  min-height: 100%;
  font-size: 13px;
}

.editor-container :deep(.cm-scroller) {
  overflow: auto;
}

.preview-empty {
  align-self: center;
  justify-self: center;
}

@media (max-width: 900px) {
  .app-code-workbench {
    height: min(84vh, 820px);
  }

  .code-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .preview-container {
    grid-template-columns: 1fr;
  }

  .file-list {
    max-height: 220px;
    border-right: 0;
    border-bottom: 1px solid #e5e7eb;
  }
}
</style>
