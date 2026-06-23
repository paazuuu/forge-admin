<template>
  <div class="icon-selector">
    <n-button v-if="trigger" ghost :disabled="disabled" style="width: 100%" @click="open">
      <template v-if="modelValue">
        <IconRenderer :icon="modelValue" custom-class="icon-preview" />
        <span class="icon-text">{{ formatDisplayName(modelValue) }}</span>
      </template>
      <span v-else>选择图标</span>
    </n-button>

    <n-drawer v-model:show="showDrawer" :width="600" placement="right">
      <n-drawer-content closable>
        <template #header>
          <div class="drawer-header">
            <span class="drawer-title">选择图标</span>
            <n-button
              v-if="modelValue"
              style="margin-right: 30px"
              type="error"
              secondary
              size="small"
              @click="clearSelection"
            >
              <template #icon>
                <i class="i-material-symbols:delete-outline" />
              </template>
              清除
            </n-button>
          </div>
        </template>

        <div class="icon-selector-popover">
          <div class="icon-selector-header">
            <n-input
              v-model:value="searchText"
              placeholder="搜索图标"
              clearable
              style="margin-bottom: 12px"
            />
            <n-tabs v-model:value="activeTab" type="segment" size="small">
              <n-tab v-if="currentImagePreview" name="currentImage">
                当前图片
              </n-tab>
              <n-tab name="ionicons">
                Ionicons
              </n-tab>
              <n-tab name="local">
                本地图标
              </n-tab>
              <n-tab name="localImages">
                本地图片
              </n-tab>
              <n-tab name="uploadImage">
                上传图片
              </n-tab>
            </n-tabs>
          </div>

          <div ref="contentRef" class="icon-selector-content">
            <div v-if="activeTab === 'currentImage'" class="current-image-panel">
              <div class="current-image-preview selected">
                <IconRenderer :icon="modelValue" :size="44" />
                <div class="current-image-meta">
                  <div class="current-image-title">
                    当前图片图标
                  </div>
                  <div class="current-image-name">
                    {{ formatDisplayName(modelValue) }}
                  </div>
                </div>
              </div>
            </div>

            <div v-if="activeTab === 'ionicons'" class="icon-grid">
              <div
                v-for="icon in filteredIonicons"
                :key="icon"
                class="icon-item"
                :class="{ selected: isIconSelected(icon, 'ionicons') }"
                @click="selectIcon(icon)"
              >
                <component :is="getIoniconComponent(icon)" class="icon" />
                <div class="icon-name">
                  {{ formatIconName(icon) }}
                </div>
              </div>
              <div v-if="filteredIonicons.length === 0" class="no-icons">
                未找到匹配的图标
              </div>
            </div>

            <div v-if="activeTab === 'local'" class="icon-grid">
              <div
                v-for="icon in filteredLocalIcons"
                :key="icon"
                class="icon-item"
                :class="{ selected: isIconSelected(icon, 'local') }"
                @click="selectIcon(icon)"
              >
                <i :class="icon" class="text-20" />
                <div class="icon-name">
                  {{ icon }}
                </div>
              </div>
              <div v-if="filteredLocalIcons.length === 0" class="no-icons">
                未找到匹配的图标
              </div>
            </div>

            <div v-if="activeTab === 'localImages'" class="icon-grid">
              <div
                v-for="icon in filteredLocalImageIcons"
                :key="icon.value"
                class="icon-item"
                :class="{ selected: isIconSelected(icon.value, 'localImages') }"
                @click="selectLocalImageIcon(icon.value)"
              >
                <IconRenderer :icon="icon.value" :size="32" />
                <div class="icon-name">
                  {{ icon.displayName }}
                </div>
              </div>
              <div v-if="filteredLocalImageIcons.length === 0" class="no-icons">
                未找到匹配的图片图标
              </div>
            </div>

            <div v-if="activeTab === 'uploadImage'" class="upload-image-panel">
              <ImageUpload
                :model-value="uploadImageValue"
                :limit="1"
                :file-size="2"
                :file-type="['png', 'jpg', 'jpeg', 'webp', 'gif', 'svg']"
                business-type="menu-icon"
                :show-tip="true"
                value-type="string"
                @success="handleUploadSuccess"
                @update:model-value="handleUploadValueChange"
              />
            </div>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import * as ionicons from '@vicons/ionicons5'
import icons from 'isme:icons'
import { computed, nextTick, ref, watch } from 'vue'
import ImageUpload from '@/components/image-upload/index.vue'
import { getLocalImageIconName, isLocalImageIcon, localImageIcons } from '@/utils/local-image-icons'
import IconRenderer from './IconRenderer.vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  trigger: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const showDrawer = ref(false)
const searchText = ref('')
const activeTab = ref('ionicons')
const contentRef = ref(null)

// 获取所有 Ionicons 图标名称
const ioniconNames = Object.keys(ionicons)

// 过滤 Ionicons 图标
const filteredIonicons = computed(() => {
  if (!searchText.value)
    return ioniconNames
  return ioniconNames.filter(name =>
    name.toLowerCase().includes(searchText.value.toLowerCase()),
  )
})

// 过滤本地图标
const filteredLocalIcons = computed(() => {
  if (!searchText.value)
    return icons
  return icons.filter((name) => {
    // 对于带目录前缀的图标名称（如 feather:activity），只搜索文件名部分
    const displayName = name.includes(':') ? name.split(':')[1] : name
    return displayName.toLowerCase().includes(searchText.value.toLowerCase())
  })
})

const filteredLocalImageIcons = computed(() => {
  if (!searchText.value)
    return localImageIcons
  return localImageIcons.filter((icon) => {
    const keyword = searchText.value.toLowerCase()
    return icon.name.toLowerCase().includes(keyword)
      || icon.displayName.toLowerCase().includes(keyword)
  })
})

const currentImagePreview = computed(() => {
  const value = props.modelValue?.trim?.() || ''
  if (!value || isLocalImageIcon(value))
    return false
  return isImageIconValue(value)
})

const uploadImageValue = computed(() => currentImagePreview.value ? props.modelValue : '')

// 获取 Ionicons 图标组件
function getIoniconComponent(name) {
  return ionicons[name]
}

// 格式化图标名称显示
function formatIconName(name) {
  // 对于带目录前缀的图标名称（如 feather:activity），只显示文件名部分
  if (name.includes(':')) {
    return name.split(':')[1]
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .replace(/-/g, ' ')
      .replace(/^./, str => str.toUpperCase())
  }

  return name
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/-/g, ' ')
    .replace(/^./, str => str.toUpperCase())
}

// 选择图标
function selectIcon(iconName) {
  // 根据当前标签页添加对应的前缀
  let iconWithPrefix = ''
  if (activeTab.value === 'ionicons') {
    iconWithPrefix = `ionicons5:${iconName}`
  }
  else if (activeTab.value === 'local') {
    iconWithPrefix = `local:${iconName}`
  }
  emit('update:modelValue', iconWithPrefix)
  showDrawer.value = false // 选择后关闭抽屉
}

function selectLocalImageIcon(iconValue) {
  emit('update:modelValue', iconValue)
  showDrawer.value = false
}

function normalizeUploadedIconValue(value) {
  if (!value)
    return ''
  if (typeof value === 'string')
    return value
  return value.fileId || value.filePath || value.accessUrl || ''
}

function handleUploadSuccess(fileData) {
  const value = normalizeUploadedIconValue(fileData)
  if (!value)
    return
  emit('update:modelValue', value)
  showDrawer.value = false
}

function handleUploadValueChange(value) {
  const nextValue = normalizeUploadedIconValue(value)
  emit('update:modelValue', nextValue)
}

function isImageIconValue(value) {
  if (!value || typeof value !== 'string')
    return false

  const iconValue = value.trim()
  if (!iconValue || isLocalImageIcon(iconValue))
    return false

  return iconValue.startsWith('http://')
    || iconValue.startsWith('https://')
    || iconValue.startsWith('data:')
    || iconValue.startsWith('blob:')
    || iconValue.startsWith('/api/file/')
    || iconValue.startsWith('forge-file://')
    || /^[a-f0-9]{32}$/i.test(iconValue)
    || /\.(?:png|jpe?g|webp|gif|svg|avif)(?:\?.*)?$/i.test(iconValue)
}

function getInitialTab() {
  const value = props.modelValue?.trim?.() || ''
  if (!value)
    return 'ionicons'
  if (currentImagePreview.value)
    return 'currentImage'
  if (isLocalImageIcon(value))
    return 'localImages'
  if (value.startsWith('ionicons5:'))
    return 'ionicons'
  return 'local'
}

function scrollSelectedIntoView() {
  const selectedEl = contentRef.value?.querySelector?.('.icon-item.selected')
  selectedEl?.scrollIntoView?.({ block: 'center' })
}

function open() {
  if (props.disabled)
    return
  searchText.value = ''
  activeTab.value = getInitialTab()
  showDrawer.value = true
  nextTick(scrollSelectedIntoView)
}

// 清除选择
function clearSelection() {
  emit('update:modelValue', '')
  searchText.value = ''
  showDrawer.value = false
}

// 判断图标是否被选中
function isIconSelected(iconName, type) {
  if (!props.modelValue)
    return false

  if (type === 'localImages')
    return props.modelValue === iconName

  // 构建完整的图标名称（带前缀）
  const fullIconName = type === 'ionicons'
    ? `ionicons5:${iconName}`
    : `local:${iconName}`

  return props.modelValue === fullIconName
}

// 格式化显示名称（用于按钮显示）
function formatDisplayName(iconValue) {
  if (!iconValue)
    return ''

  if (isLocalImageIcon(iconValue))
    return getLocalImageIconName(iconValue)

  if (isImageIconValue(iconValue)) {
    const filename = iconValue.split(/[\\/]/).pop() || iconValue
    if (/^[a-f0-9]{32}$/i.test(iconValue))
      return '上传图片图标'
    return filename.length > 28 ? `${filename.slice(0, 12)}...${filename.slice(-10)}` : filename
  }

  // 移除前缀，只显示图标名称
  if (iconValue.includes(':')) {
    return iconValue.split(':')[1]
  }

  return iconValue
}

defineExpose({
  open,
})

watch(activeTab, () => {
  if (showDrawer.value)
    nextTick(scrollSelectedIntoView)
})
</script>

<style scoped>
.icon-selector {
  width: 100%;
}

.icon-selector-input {
  display: flex;
  align-items: center;
}

.icon-selector-popover {
  width: 100%;
  padding: 12px;
}

.icon-selector-content {
  max-height: 500px;
  overflow-y: auto;
  padding-right: 4px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}
.icon-grid::before {
  display: none;
}
.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  height: 100px;
}

.icon-item:hover {
  background-color: #f5f5f5;
}

.icon-item.selected {
  background-color: #e6f4ff;
  border: 1px solid #1890ff;
}

.current-image-panel {
  padding: 4px 0;
}

.current-image-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid #1890ff;
  border-radius: 6px;
  background: #e6f4ff;
}

.current-image-title {
  font-size: var(--font-size-base);
  font-weight: 500;
}

.current-image-name {
  margin-top: 4px;
  color: #666;
  font-size: var(--font-size-sm);
  word-break: break-all;
}

.upload-image-panel {
  padding: 4px 0;
}

.icon {
  width: 32px;
  height: 32px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-name {
  font-size: var(--font-size-sm);
  text-align: center;
  word-break: break-word;
  line-height: 1.2;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.icon-preview {
  width: 20px;
  height: 20px;
  margin-right: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.icon-text {
  font-size: var(--font-size-sm);
  color: #666;
}

.no-icons {
  grid-column: 1 / -1;
  text-align: center;
  padding: 20px;
  color: #999;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.drawer-title {
  font-size: 16px;
  font-weight: 500;
}
</style>
