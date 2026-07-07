<template>
  <div id="top-tab">
    <n-tabs
      :value="tabStore.activeTab"
      :closable="hasClosableTabs"
      type="card"
      size="small"
      @close="(path) => tabStore.removeTab(path)"
    >
      <n-tab
        v-for="item in tabStore.tabs"
        :key="item.path"
        :name="item.path"
        :closable="isTabClosable(item)"
        @click="handleItemClick(item.path)"
        @contextmenu.prevent="handleContextMenu($event, item)"
      >
        {{ item.title }}
      </n-tab>
    </n-tabs>

    <ContextMenu
      v-if="contextMenuOption.show"
      v-model:show="contextMenuOption.show"
      :current-path="contextMenuOption.currentPath"
      :x="contextMenuOption.x"
      :y="contextMenuOption.y"
    />
  </div>
</template>

<script setup>
import { useMenu } from '@/composables'
import { useTabStore } from '@/store'
import ContextMenu from './ContextMenu.vue'

const tabStore = useTabStore()
const { handleMenuSelect: baseHandleMenuSelect } = useMenu()

const contextMenuOption = reactive({
  show: false,
  x: 0,
  y: 0,
  currentPath: '',
})

const hasClosableTabs = computed(() => tabStore.tabs.some(isTabClosable))

function isTabClosable(tab) {
  if (tab?.closable === false)
    return false
  if (tab?.forceClosable)
    return true
  return tabStore.tabs.length > 1
}

function handleItemClick(path) {
  tabStore.setActiveTab(path)
  baseHandleMenuSelect(undefined, path)
}

function showContextMenu() {
  contextMenuOption.show = true
}
function hideContextMenu() {
  contextMenuOption.show = false
}
function setContextMenu(x, y, currentPath) {
  Object.assign(contextMenuOption, { x, y, currentPath })
}

// 右击菜单
async function handleContextMenu(e, tagItem) {
  const { clientX, clientY } = e
  hideContextMenu()
  setContextMenu(clientX, clientY, tagItem.path)
  await nextTick()
  showContextMenu()
}
</script>

<style scoped>
#top-tab {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  height: 38px;
  overflow: hidden;
  --forge-tab-height: 30px;
  --forge-tab-gap: 5px;
  --forge-tab-text: var(--text-secondary, #4e5969);
  --forge-tab-muted: var(--text-tertiary, #86909c);
  --forge-tab-active-bg: color-mix(in srgb, var(--primary-color, #4242f7) 9%, transparent);
  --forge-tab-hover-bg: color-mix(in srgb, var(--text-primary, #1d2129) 5%, transparent);
}

#top-tab :deep(.n-tabs) {
  width: 100%;
  min-width: 0;
  height: 38px;
  overflow: hidden;
}

#top-tab :deep(.n-tabs-nav) {
  height: 38px;
  min-height: 38px;
  border: 0 !important;
  background: transparent !important;
  line-height: 1;
}

#top-tab :deep(.n-tabs-nav::before),
#top-tab :deep(.n-tabs-nav::after) {
  display: none !important;
}

#top-tab :deep(.n-tabs-nav-scroll-wrapper),
#top-tab :deep(.n-tabs-nav-scroll-content),
#top-tab :deep(.n-tabs-nav-scroll-content > div) {
  height: 38px;
}

#top-tab :deep(.n-tabs-nav-scroll-content) {
  display: flex;
  align-items: center;
  gap: var(--forge-tab-gap);
}

#top-tab :deep(.n-tabs-tab-wrapper) {
  display: flex;
  align-items: center;
  height: 38px;
}

#top-tab :deep(.n-tabs-tab) {
  position: relative;
  display: inline-flex;
  align-items: center;
  height: var(--forge-tab-height) !important;
  min-height: var(--forge-tab-height) !important;
  padding: 0 12px !important;
  border: 0 !important;
  border-radius: 3px !important;
  background: transparent !important;
  color: var(--forge-tab-text) !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  line-height: var(--forge-tab-height) !important;
  box-shadow: none !important;
  transition:
    background-color 0.16s ease,
    color 0.16s ease !important;
}

#top-tab :deep(.n-tabs-tab::after) {
  position: absolute;
  right: 8px;
  bottom: 0;
  left: 8px;
  height: 2px;
  background: var(--primary-color, #4242f7);
  content: '';
  opacity: 0;
  transform: scaleX(0.35);
  transition:
    opacity 0.16s ease,
    transform 0.16s ease;
}

#top-tab :deep(.n-tabs-tab:hover) {
  background: var(--forge-tab-hover-bg) !important;
  color: var(--text-primary, #1d2129) !important;
}

#top-tab :deep(.n-tabs-tab.n-tabs-tab--active) {
  background: var(--forge-tab-active-bg) !important;
  color: var(--primary-color, #4242f7) !important;
  font-weight: 600 !important;
}

#top-tab :deep(.n-tabs-tab.n-tabs-tab--active::after) {
  opacity: 1;
  transform: scaleX(1);
}

#top-tab :deep(.n-tabs-tab__label) {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

#top-tab :deep(.n-tabs-tab__close) {
  width: 18px;
  height: 18px;
  margin-left: 8px;
  border-radius: 3px;
  color: var(--forge-tab-muted);
  font-size: 12px !important;
  transition:
    background-color 0.16s ease,
    color 0.16s ease;
}

#top-tab :deep(.n-tabs-tab__close .n-icon),
#top-tab :deep(.n-tabs-tab__close svg) {
  width: 12px !important;
  height: 12px !important;
  font-size: 12px !important;
}

#top-tab :deep(.n-tabs-tab__close:hover) {
  background: color-mix(in srgb, var(--text-primary, #1d2129) 9%, transparent);
  color: var(--text-secondary, #4e5969);
}

#top-tab :deep(.n-tabs-tab-pad),
#top-tab :deep(.n-tabs-tab-pane) {
  display: none;
}

.dark #top-tab {
  --forge-tab-active-bg: color-mix(in srgb, var(--primary-color, #6a7dff) 18%, transparent);
  --forge-tab-hover-bg: rgba(255, 255, 255, 0.06);
}
</style>
