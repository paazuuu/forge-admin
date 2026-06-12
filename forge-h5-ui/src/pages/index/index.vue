<template>
  <view class="home-page">
    <view class="page-glow page-glow-blue" />
    <view class="page-glow page-glow-indigo" />
    <view class="grid-layer" />

    <view class="home-content">
      <view class="home-header animate-in">
        <view class="user-block" @click="goMine">
          <view class="avatar-wrap">
            <AiAuthImage class="avatar-image" :src="rawAvatarUrl" :fallback="DEFAULT_AVATAR_URL" mode="aspectFill" />
          </view>
          <view class="user-copy">
            <text class="hello-title">Hi, {{ authStore.displayName }}</text>
            <text class="hello-subtitle">欢迎回来，继续探索 Forge H5</text>
          </view>
        </view>
        <button class="bell-button" @click="goMessages">
          <view class="icon-mask bell-icon" :style="iconMask('/static/icons/ai-icon/bell.svg', '#475569')" />
          <view v-if="unreadCount > 0" class="bell-badge">
            <text>{{ unreadCount > 99 ? '99+' : unreadCount }}</text>
          </view>
        </button>
      </view>

      <view class="feature-card animate-in delay-1">
        <view class="feature-inner">
          <view class="feature-top">
            <view class="feature-title-block">
              <text class="feature-label">服务概览</text>
              <text class="feature-desc">同步 {{ lastSyncText }}</text>
            </view>
            <button class="feature-refresh" @click="refreshWorkspace">
              <AiIcon icon="/static/icons/ai-icon/refresh-cw.svg" color="#ffffff" size="sm" />
            </button>
          </view>
          <view class="feature-metrics">
            <view class="metric-item">
              <text class="metric-value">{{ backendMenuCount }}</text>
              <text class="metric-label">菜单</text>
            </view>
            <view class="metric-item">
              <text class="metric-value">{{ unreadCount }}</text>
              <text class="metric-label">未读</text>
            </view>
            <view class="metric-item">
              <text class="metric-value">{{ permissionCount }}</text>
              <text class="metric-label">权限</text>
            </view>
            <view class="metric-item metric-status">
              <text class="metric-value">{{ syncStatusText }}</text>
              <text class="metric-label">状态</text>
            </view>
          </view>
        </view>
      </view>

      <view class="shortcut-grid animate-in delay-2">
        <view
          v-for="item in menuItems"
          :key="item.key"
          class="shortcut-item"
          @click="handleShortcut(item)"
        >
          <view class="shortcut-icon" :class="item.bgClass">
            <AiIcon :icon="item.icon" :color="item.color" size="lg" />
          </view>
          <text class="shortcut-label">{{ item.label }}</text>
        </view>
      </view>

      <view class="workbench-panel animate-in delay-3">
        <view class="workbench-head">
          <view>
            <text class="section-title">今日工作台</text>
            <text class="section-subtitle">{{ authStore.userInfo?.deptName || authStore.userInfo?.tenantName || '移动端' }}</text>
          </view>
          <button class="section-link" @click="openMenuSheet">
            <text>全部应用</text>
            <view class="icon-mask arrow-icon" :style="iconMask('/static/icons/ai-icon/arrow-right.svg', '#2563eb')" />
          </button>
        </view>
        <view class="workbench-grid">
          <view
            v-for="item in workbenchItems"
            :key="item.key"
            class="workbench-card"
            :class="item.tone"
            @click="handleWorkbench(item)"
          >
            <view class="workbench-icon">
              <AiIcon :icon="item.icon" :color="item.color" size="md" />
            </view>
            <view class="workbench-copy">
              <text class="workbench-title">{{ item.label }}</text>
              <text class="workbench-desc">{{ item.desc }}</text>
            </view>
            <text v-if="item.badge" class="workbench-badge">{{ item.badge }}</text>
          </view>
        </view>
      </view>

      <view class="feed-section animate-in delay-4">
        <view class="section-head">
          <text class="section-title">最新提醒</text>
          <button class="section-link" @click="goMessages">
            <text>查看全部</text>
            <view class="icon-mask arrow-icon" :style="iconMask('/static/icons/ai-icon/arrow-right.svg', '#2563eb')" />
          </button>
        </view>

        <view class="message-list">
          <view
            v-for="message in messages"
            :key="message.id"
            class="message-card"
            @click="openMessage(message)"
          >
            <view class="message-icon" :class="message.bgClass">
              <view class="icon-mask" :style="iconMask(message.icon, message.color)" />
              <view v-if="message.unread" class="message-dot" />
            </view>
            <view class="message-main">
              <view class="message-title-row">
                <text class="message-title">{{ message.title }}</text>
                <text class="message-time">{{ message.time }}</text>
              </view>
              <text class="message-desc">{{ message.desc }}</text>
            </view>
          </view>
          <view v-if="!messages.length" class="message-empty-card">
            <AiIcon icon="/static/icons/ai-icon/check-circle.svg" color="#10b981" size="md" />
            <text>暂无新提醒</text>
          </view>
        </view>
      </view>
    </view>

    <AiPopupSheet
      v-model="menuSheetVisible"
      :scroll="false"
      :show-handle="false"
      max-height="96vh"
      body-max-height="calc(96vh - 160rpx - env(safe-area-inset-bottom))"
      title="全部应用"
      description="按模块浏览已授权的 H5 菜单"
    >
      <view class="menu-search-bar">
        <AiIcon icon="/static/icons/ai-icon/search.svg" color="#64748b" size="sm" />
        <input
          v-model="menuSearchKeyword"
          class="menu-search-input"
          placeholder="搜索菜单"
          placeholder-class="menu-search-placeholder"
          confirm-type="search"
        />
        <button v-if="menuSearchKeyword" class="menu-search-clear" @click.stop="clearMenuSearch">
          <AiIcon icon="/static/icons/ai-icon/x.svg" color="#94a3b8" size="sm" />
        </button>
      </view>

      <view class="menu-browser">
        <scroll-view class="menu-group-pane" scroll-y :show-scrollbar="false">
          <view
            v-for="group in filteredMenuGroups"
            :key="group.key"
            class="menu-group-item"
            :class="{ active: group.key === activeMenuGroupKey }"
            @click="selectMenuGroup(group.key)"
          >
            <text class="menu-group-name">{{ group.label }}</text>
          </view>
        </scroll-view>

        <scroll-view class="menu-list-pane" scroll-y :show-scrollbar="false">
          <view class="menu-list-head">
            <text class="menu-list-title">{{ activeMenuGroup.label }}</text>
          </view>

          <view v-if="activeMenuGroup.items.length" class="menu-list-grid">
            <view
              v-for="item in activeMenuGroup.items"
              :key="item.key"
              class="menu-list-card"
              @click="openMenuEntry(item)"
            >
              <view class="menu-list-icon" :style="menuIconTileStyle(item)">
                <AiIcon :icon="item.icon" :color="item.color" size="lg" />
              </view>
              <view class="menu-list-copy">
                <text class="menu-list-name">{{ item.label }}</text>
              </view>
            </view>
          </view>

          <view v-else class="menu-empty">
            <view class="menu-empty-icon">
              <AiIcon icon="/static/icons/ai-icon/inbox.svg" color="#94a3b8" size="lg" />
            </view>
            <text class="menu-empty-title">{{ menuSearchKeyword ? '暂无匹配' : '暂无菜单' }}</text>
            <text class="menu-empty-desc">{{ menuSearchKeyword ? '换个关键词再试试' : '该模块下还没有可访问的 H5 页面' }}</text>
          </view>
        </scroll-view>
      </view>
    </AiPopupSheet>

    <AiTabBar active="home" />
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AiAuthImage from '@/components/AiAuthImage.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import AiTabBar from '@/components/AiTabBar.vue'
import api from '@/api'
import { useAuthStore } from '@/store'
import { ensureLogin } from '@/utils/auth-guard'
import { resolveStaticUrl } from '@/utils/assets'
import { DEFAULT_AVATAR_URL } from '@/utils/file'
import { toast } from '@/utils/notify'

const authStore = useAuthStore()

const userClientLabel = (import.meta.env.VITE_USER_CLIENT || 'app').toUpperCase()
const lastSyncAt = ref('')
const unreadCount = ref(0)
const latestMessages = ref([])
const menuSheetVisible = ref(false)
const activeMenuGroupKey = ref('')
const menuSearchKeyword = ref('')

const rawAvatarUrl = computed(() => authStore.userInfo?.avatar || '')

const fallbackMenuItems = [
  {
    key: 'account',
    label: '账户',
    icon: '/static/icons/ai-icon/pocket.svg',
    color: '#2563eb',
    bgClass: 'bg-blue',
  },
  {
    key: 'cards',
    label: '卡包',
    icon: '/static/icons/ai-icon/credit-card.svg',
    color: '#4f46e5',
    bgClass: 'bg-indigo',
  },
  {
    key: 'analytics',
    label: '数据',
    icon: '/static/icons/ai-icon/pie-chart.svg',
    color: '#7c3aed',
    bgClass: 'bg-purple',
  },
  {
    key: 'service',
    label: '服务',
    icon: '/static/icons/ai-icon/zap.svg',
    color: '#d97706',
    bgClass: 'bg-amber',
  },
]

const componentDemoItem = {
  key: 'component-demo',
  label: '组件演示',
  icon: '/static/icons/ai-icon/loader.svg',
  color: '#2563eb',
  bgClass: 'bg-blue',
}

const moreMenuItem = {
  key: 'more',
  label: '更多',
  icon: '/static/icons/ai-icon/grid.svg',
  color: '#0f766e',
  bgClass: 'bg-teal',
  isMore: true,
}

const menuToneList = [
  { icon: '/static/icons/ai-icon/pocket.svg', color: '#2563eb', bgClass: 'bg-blue' },
  { icon: '/static/icons/ai-icon/credit-card.svg', color: '#4f46e5', bgClass: 'bg-indigo' },
  { icon: '/static/icons/ai-icon/pie-chart.svg', color: '#7c3aed', bgClass: 'bg-purple' },
  { icon: '/static/icons/ai-icon/zap.svg', color: '#d97706', bgClass: 'bg-amber' },
  { icon: '/static/icons/ai-icon/message-square.svg', color: '#10b981', bgClass: 'bg-emerald' },
  { icon: '/static/icons/ai-icon/briefcase.svg', color: '#0f766e', bgClass: 'bg-teal' },
]

const menuItems = computed(() => {
  const backendItems = flattenMenus(authStore.menus)
  const sourceItems = backendItems.length ? backendItems : fallbackMenuItems
  return [componentDemoItem, ...sourceItems].slice(0, 7).concat(moreMenuItem)
})

const menuGroups = computed(() => buildMenuGroups(authStore.menus))
const filteredMenuGroups = computed(() => {
  const keyword = normalizeSearchText(menuSearchKeyword.value)
  if (!keyword) {
    return menuGroups.value
  }

  return menuGroups.value
    .map((group) => {
      const groupMatched = normalizeSearchText(group.label).includes(keyword)
      const items = group.items.filter((item) => {
        return groupMatched
          || normalizeSearchText(item.label).includes(keyword)
          || normalizeSearchText(item.path).includes(keyword)
          || normalizeSearchText(item.component).includes(keyword)
      })
      return { ...group, items }
    })
    .filter(group => group.items.length)
})
const activeMenuGroup = computed(() => {
  return filteredMenuGroups.value.find(group => group.key === activeMenuGroupKey.value) || filteredMenuGroups.value[0] || {
    key: 'empty',
    label: '全部应用',
    items: [],
  }
})
const backendMenuCount = computed(() => flattenMenus(authStore.menus).length)
const permissionCount = computed(() => Array.isArray(authStore.permissions) ? authStore.permissions.length : 0)
const syncStatusText = computed(() => backendMenuCount.value ? '已接入' : '待接入')
const lastSyncText = computed(() => lastSyncAt.value || '未同步')
const todoBadgeText = computed(() => unreadCount.value > 0 ? `${unreadCount.value}` : '')
const workbenchItems = computed(() => [
  {
    key: 'todo',
    label: '流程待办',
    desc: '审批任务',
    icon: '/static/icons/ai-icon/check-square.svg',
    color: '#0f766e',
    tone: 'tone-teal',
  },
  {
    key: 'messages',
    label: '消息中心',
    desc: unreadCount.value ? '有未读提醒' : '暂无未读',
    icon: '/static/icons/ai-icon/bell.svg',
    color: '#2563eb',
    tone: 'tone-blue',
    badge: todoBadgeText.value,
  },
  {
    key: 'apps',
    label: '全部应用',
    desc: `${backendMenuCount.value || 0} 个入口`,
    icon: '/static/icons/ai-icon/grid.svg',
    color: '#7c3aed',
    tone: 'tone-purple',
  },
  {
    key: 'profile',
    label: '资料维护',
    desc: authStore.userInfo?.phone ? '信息完整' : '补全账号',
    icon: '/static/icons/ai-icon/user.svg',
    color: '#d97706',
    tone: 'tone-amber',
  },
])
const fallbackMessages = computed(() => [
  {
    id: 'fallback-system',
    title: '系统更新',
    desc: 'Forge H5 模板已准备好登录、鉴权和用户端基础页面。',
    time: '刚刚',
    icon: '/static/icons/ai-icon/activity.svg',
    color: '#3b82f6',
    bgClass: 'bg-blue',
    unread: true,
  },
  {
    id: 'fallback-login',
    title: '登录成功',
    desc: `${authStore.displayName} 已通过 ${userClientLabel} 客户端完成安全登录。`,
    time: '2小时前',
    icon: '/static/icons/ai-icon/credit-card.svg',
    color: '#10b981',
    bgClass: 'bg-emerald',
    unread: false,
  },
  {
    id: 'fallback-welcome',
    title: '欢迎使用 Forge',
    desc: '这里可以继续接入订单、权益、消息、服务等用户端 H5 功能。',
    time: '1天前',
    icon: '/static/icons/ai-icon/message-square.svg',
    color: '#8b5cf6',
    bgClass: 'bg-purple',
    unread: false,
  },
])

const messages = computed(() => {
  return latestMessages.value.length ? latestMessages.value.slice(0, 2) : fallbackMessages.value.slice(0, 2)
})

function flattenMenus(menus = []) {
  const result = []

  function walk(list = []) {
    sortMenus(list)
      .filter(isVisibleMenu)
      .forEach((menu) => {
        const children = Array.isArray(menu.children) ? menu.children : []
        if (children.length) {
          walk(children)
          return
        }
        result.push(normalizeMenuEntry(menu, result.length))
      })
  }

  walk(menus)
  return result
}

function buildMenuGroups(menus = []) {
  const groups = []
  const topLevelItems = []

  sortMenus(menus)
    .filter(isVisibleMenu)
    .forEach((menu) => {
      const children = Array.isArray(menu.children) ? menu.children : []
      const childItems = collectMenuEntries(children, groups.length)
      if (childItems.length) {
        groups.push({
          key: `group-${menu.id || menu.resourceName || groups.length}`,
          label: menu.resourceName || menu.title || menu.name || '未命名模块',
          items: childItems,
        })
        return
      }

      if (isNavigableMenu(menu)) {
        topLevelItems.push(normalizeMenuEntry(menu, topLevelItems.length))
      }
    })

  if (topLevelItems.length) {
    groups.unshift({
      key: 'quick',
      label: '常用',
      items: topLevelItems,
    })
  }

  if (!groups.length) {
    groups.push({
      key: 'template',
      label: '模板',
      items: [componentDemoItem, ...fallbackMenuItems],
    })
  }

  return groups
}

function collectMenuEntries(list = [], offset = 0) {
  const result = []

  function walk(children = []) {
    sortMenus(children)
      .filter(isVisibleMenu)
      .forEach((menu) => {
        const nextChildren = Array.isArray(menu.children) ? menu.children : []
        if (nextChildren.length) {
          walk(nextChildren)
          return
        }
        if (isNavigableMenu(menu)) {
          result.push(normalizeMenuEntry(menu, offset + result.length))
        }
      })
  }

  walk(list)
  return result
}

function sortMenus(list = []) {
  return [...list].sort((a, b) => Number(a?.sort || 0) - Number(b?.sort || 0))
}

function isVisibleMenu(menu) {
  return menu && menu.visible !== 0 && menu.menuStatus !== 0
}

function isNavigableMenu(menu) {
  return !!(menu?.path || menu?.component || menu?.isExternal === 1)
}

function normalizeMenuEntry(menu, index = 0) {
  const tone = menuToneList[index % menuToneList.length]
  return {
    ...tone,
    key: menu.id || menu.path || menu.resourceName,
    label: menu.resourceName || menu.title || menu.name || '未命名',
    path: menu.path,
    component: menu.component,
    icon: normalizeMenuIcon(menu.icon, tone.icon),
    external: menu.isExternal === 1,
    fromBackend: true,
  }
}

function normalizeMenuIcon(icon, fallbackIcon) {
  const iconValue = String(icon || '').trim()
  return iconValue || fallbackIcon
}

function normalizeSearchText(value) {
  return String(value || '').trim().toLowerCase()
}

watch(filteredMenuGroups, (groups) => {
  if (!groups.some(group => group.key === activeMenuGroupKey.value)) {
    activeMenuGroupKey.value = groups[0]?.key || ''
  }
}, { immediate: true })

onShow(async () => {
  hideNativeTabBar()
  const ok = await ensureLogin({ redirect: '/pages/index/index' })
  if (!ok) {
    return
  }
  await refreshWorkspace({ silent: true })
})

function hideNativeTabBar() {
  if (typeof uni === 'undefined' || typeof uni.hideTabBar !== 'function') {
    return
  }
  uni.hideTabBar({
    animation: false,
    fail: () => {},
  })
}

function iconMask(icon, color) {
  const url = resolveStaticUrl(icon)
  return {
    backgroundColor: color,
    WebkitMask: `url(${url}) center / contain no-repeat`,
    mask: `url(${url}) center / contain no-repeat`,
  }
}

function menuIconTileStyle(item) {
  const color = item?.color || '#2563eb'
  return {
    background: `linear-gradient(135deg, ${hexToRgba(color, 0.16)}, ${hexToRgba(color, 0.07)})`,
    borderColor: hexToRgba(color, 0.18),
    boxShadow: `0 8rpx 18rpx ${hexToRgba(color, 0.08)}`,
  }
}

function hexToRgba(hex, alpha = 1) {
  const value = String(hex || '').replace('#', '').trim()
  if (!/^[\da-f]{6}$/i.test(value)) {
    return `rgba(37, 99, 235, ${alpha})`
  }
  const r = Number.parseInt(value.slice(0, 2), 16)
  const g = Number.parseInt(value.slice(2, 4), 16)
  const b = Number.parseInt(value.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

function goMine() {
  uni.switchTab({ url: '/pages/mine/index' })
}

function goMessages() {
  uni.navigateTo({ url: '/pages/message/index' })
}

function goTodo() {
  uni.navigateTo({ url: '/pages/todo' })
}

function handleWorkbench(item) {
  const actionMap = {
    todo: goTodo,
    messages: goMessages,
    apps: openMenuSheet,
    profile: goMine,
  }
  actionMap[item.key]?.()
}

function handleShortcut(item) {
  if (item.isMore) {
    openMenuSheet()
    return
  }
  if (item.key === 'component-demo') {
    uni.navigateTo({ url: '/pages/demo/loading/index' })
    return
  }
  if (item.key === 'account') {
    goMine()
    return
  }
  if (item.fromBackend) {
    openBackendMenu(item)
    return
  }
  toast(`${item.label}待接入`, { type: 'info' })
}

function openMenuSheet() {
  menuSearchKeyword.value = ''
  if (!filteredMenuGroups.value.some(group => group.key === activeMenuGroupKey.value)) {
    activeMenuGroupKey.value = filteredMenuGroups.value[0]?.key || ''
  }
  menuSheetVisible.value = true
}

function clearMenuSearch() {
  menuSearchKeyword.value = ''
}

function selectMenuGroup(key) {
  activeMenuGroupKey.value = key
}

function openMenuEntry(item) {
  if (item.isMore) {
    return
  }
  menuSheetVisible.value = false
  handleShortcut(item)
}

function openBackendMenu(item) {
  const path = item.component || item.path
  if (path && path.startsWith('/pages/')) {
    uni.navigateTo({
      url: path,
      fail: () => toast(`${item.label}页面未注册`, { type: 'warning' }),
    })
    return
  }
  toast(`${item.label}页面待接入`, { type: 'info' })
}

function openMessage(message) {
  if (message.fromBackend) {
    uni.navigateTo({ url: `/pages/message/index?id=${message.id}` })
    return
  }
  goMessages()
}

async function refreshWorkspace(options = {}) {
  try {
    await Promise.all([
      authStore.fetchUserInfo(),
      authStore.fetchAccessSnapshot(),
      fetchMessageSummary(),
    ])
    lastSyncAt.value = formatCurrentTime()
    if (!options.silent) {
      toast('已同步', { type: 'success' })
    }
  }
  catch (error) {
    console.error('刷新首页信息失败:', error)
  }
}

async function fetchMessageSummary() {
  try {
    const [countResult, pageResult] = await Promise.allSettled([
      api.getUnreadMessageCount(),
      api.getMessagePage({ pageNum: 1, pageSize: 5 }),
    ])

    if (countResult.status === 'fulfilled') {
      unreadCount.value = normalizeUnreadCount(countResult.value?.data)
    }

    if (pageResult.status === 'fulfilled') {
      const records = normalizeMessageRecords(pageResult.value?.data)
      latestMessages.value = records.map(normalizeHomeMessage)
    }
  }
  catch (error) {
    console.error('加载消息摘要失败:', error)
  }
}

function normalizeUnreadCount(data) {
  if (typeof data === 'number') {
    return data
  }
  return Number(data?.totalCount || data?.unreadCount || data?.count || 0)
}

function normalizeMessageRecords(data) {
  if (Array.isArray(data)) {
    return data
  }
  return data?.records || data?.list || data?.rows || []
}

function normalizeHomeMessage(message) {
  const isApproval = message?.bizType === 'FLOW_TODO'
  const unread = Number(message?.readFlag) === 0
  return {
    id: message.id,
    title: message.title || '消息通知',
    desc: stripHtml(message.content || message.description || '-'),
    time: formatMessageTime(message.createTime || message.receiveTime),
    icon: isApproval ? '/static/icons/ai-icon/check-square.svg' : '/static/icons/ai-icon/message-square.svg',
    color: unread ? '#2563eb' : '#64748b',
    bgClass: isApproval ? 'bg-emerald' : unread ? 'bg-blue' : 'bg-slate',
    unread,
    fromBackend: true,
  }
}

function stripHtml(value) {
  return String(value || '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function formatMessageTime(value) {
  const date = parseMessageDate(value)
  if (!date) {
    return ''
  }
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff >= 0 && diff < 60 * 1000) {
    return '刚刚'
  }
  if (diff >= 0 && diff < 60 * 60 * 1000) {
    return `${Math.max(1, Math.floor(diff / 60000))}分钟前`
  }
  if (diff >= 0 && diff < 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${month}-${day}`
}

function parseMessageDate(value) {
  if (!value) {
    return null
  }
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    return new Date(year, month - 1, day, hour, minute, second)
  }
  const date = new Date(typeof value === 'string' ? value.replace(' ', 'T') : value)
  return Number.isNaN(date.getTime()) ? null : date
}

function formatCurrentTime() {
  const now = new Date()
  const hour = `${now.getHours()}`.padStart(2, '0')
  const minute = `${now.getMinutes()}`.padStart(2, '0')
  return `${hour}:${minute}`
}
</script>

<style lang="scss" scoped>
.home-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
  background: #f8fafc;
}

.home-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 12%, rgba(147, 197, 253, 0.32), transparent 34%),
    radial-gradient(circle at 82% 20%, rgba(199, 210, 254, 0.28), transparent 32%),
    radial-gradient(circle at 48% 70%, rgba(251, 207, 232, 0.22), transparent 36%);
}

.grid-layer {
  position: absolute;
  inset: 0;
  opacity: 0.18;
  pointer-events: none;
  background-image:
    linear-gradient(#e2e8f0 1rpx, transparent 1rpx),
    linear-gradient(90deg, #e2e8f0 1rpx, transparent 1rpx);
  background-size: 80rpx 80rpx;
}

.page-glow {
  position: absolute;
  width: 520rpx;
  height: 520rpx;
  border-radius: 999rpx;
  filter: blur(90rpx);
  pointer-events: none;
}

.page-glow-blue {
  top: -180rpx;
  left: -160rpx;
  background: rgba(147, 197, 253, 0.42);
  animation: floatGlow 13s ease-in-out infinite;
}

.page-glow-indigo {
  right: -180rpx;
  bottom: 120rpx;
  background: rgba(165, 180, 252, 0.36);
  animation: floatGlow 15s ease-in-out infinite reverse;
}

.home-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding: 56rpx 28rpx 190rpx;
  box-sizing: border-box;
}

.home-header,
.user-block,
.feature-top,
.section-head,
.section-link,
.message-card,
.message-title-row {
  display: flex;
  align-items: center;
}

.home-header,
.feature-top,
.section-head,
.message-title-row {
  justify-content: space-between;
}

.user-block {
  min-width: 0;
  flex: 1;
  gap: 22rpx;
}

.avatar-wrap {
  width: 96rpx;
  height: 96rpx;
  flex: 0 0 96rpx;
  overflow: hidden;
  border: 4rpx solid #ffffff;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #dbeafe, #c7d2fe);
  box-shadow: 0 8rpx 22rpx rgba(15, 23, 42, 0.08);
}

.avatar-image {
  width: 100%;
  height: 100%;
}

.user-copy {
  min-width: 0;
  flex: 1;
}

.hello-title,
.hello-subtitle,
.feature-label,
.feature-value,
.feature-desc,
.shortcut-label,
.section-title,
.message-title,
.message-desc,
.message-time {
  display: block;
}

.hello-title {
  overflow: hidden;
  color: #1e293b;
  font-size: 36rpx;
  font-weight: 900;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hello-subtitle {
  margin-top: 8rpx;
  color: #64748b;
  font-size: 26rpx;
  font-weight: 600;
}

.bell-button {
  position: relative;
  width: 80rpx;
  height: 80rpx;
  flex: 0 0 80rpx;
  margin-left: 20rpx;
  padding: 0;
  border: 1rpx solid rgba(255, 255, 255, 0.92);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.66);
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(24rpx);
}

.bell-button::after,
.feature-refresh::after,
.section-link::after {
  border: 0;
}

.bell-icon {
  width: 38rpx;
  height: 38rpx;
  margin: 20rpx auto 0;
}

.bell-badge {
  position: absolute;
  top: 8rpx;
  right: 4rpx;
  display: flex;
  min-width: 32rpx;
  height: 32rpx;
  align-items: center;
  justify-content: center;
  padding: 0 8rpx;
  border: 3rpx solid #ffffff;
  border-radius: 999rpx;
  background: #ef4444;
  box-sizing: border-box;
}

.bell-badge text {
  color: #ffffff;
  font-size: 18rpx;
  font-weight: 950;
  line-height: 1;
}

.feature-card {
  position: relative;
  overflow: hidden;
  border: 1rpx solid rgba(147, 197, 253, 0.32);
  border-radius: 30rpx;
  background:
    radial-gradient(circle at 12% -12%, rgba(191, 219, 254, 0.5), transparent 36%),
    radial-gradient(circle at 96% 18%, rgba(125, 211, 252, 0.28), transparent 38%),
    linear-gradient(135deg, #1d4ed8 0%, #2563eb 52%, #0f766e 100%);
  box-shadow: 0 18rpx 42rpx rgba(37, 99, 235, 0.22);
  backdrop-filter: blur(22rpx);
}

.feature-inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  padding: 24rpx;
  box-sizing: border-box;
}

.feature-label {
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 950;
}

.feature-title-block {
  min-width: 0;
  flex: 1;
}

.feature-refresh {
  display: flex;
  width: 58rpx;
  height: 58rpx;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  border: 1rpx solid rgba(255, 255, 255, 0.34);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
  box-shadow: inset 0 0 0 1rpx rgba(255, 255, 255, 0.08);
}

.feature-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: center;
  gap: 10rpx;
}

.metric-item {
  min-width: 0;
  padding: 14rpx 8rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.14);
  box-shadow: inset 0 0 0 1rpx rgba(255, 255, 255, 0.18);
}

.metric-value,
.metric-label {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-value {
  color: #ffffff;
  font-size: 27rpx;
  font-weight: 950;
  line-height: 1.05;
}

.metric-label {
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 20rpx;
  font-weight: 750;
}

.metric-status .metric-value {
  color: #bbf7d0;
  font-size: 24rpx;
}

.feature-desc {
  display: block;
  margin-top: 6rpx;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.72);
  font-size: 22rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workbench-panel {
  padding: 26rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.86);
  border-radius: 34rpx;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 12rpx 34rpx rgba(15, 23, 42, 0.045);
  backdrop-filter: blur(24rpx);
}

.workbench-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 20rpx;
}

.section-subtitle {
  display: block;
  max-width: 420rpx;
  overflow: hidden;
  margin-top: 6rpx;
  color: #64748b;
  font-size: 23rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workbench-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.workbench-card {
  position: relative;
  display: flex;
  min-height: 118rpx;
  align-items: center;
  gap: 18rpx;
  overflow: hidden;
  padding: 20rpx;
  border: 1rpx solid rgba(226, 232, 240, 0.78);
  border-radius: 26rpx;
  background: #ffffff;
  box-sizing: border-box;
}

.workbench-card::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.72;
}

.tone-blue::before {
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.95), transparent 72%);
}

.tone-teal::before {
  background: linear-gradient(135deg, rgba(204, 251, 241, 0.95), transparent 72%);
}

.tone-purple::before {
  background: linear-gradient(135deg, rgba(243, 232, 255, 0.95), transparent 72%);
}

.tone-amber::before {
  background: linear-gradient(135deg, rgba(254, 243, 199, 0.95), transparent 72%);
}

.workbench-icon {
  position: relative;
  z-index: 1;
  display: flex;
  width: 62rpx;
  height: 62rpx;
  flex: 0 0 62rpx;
  align-items: center;
  justify-content: center;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.78);
}

.workbench-copy {
  position: relative;
  z-index: 1;
  min-width: 0;
  flex: 1;
}

.workbench-title,
.workbench-desc {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workbench-title {
  color: #1e293b;
  font-size: 27rpx;
  font-weight: 900;
}

.workbench-desc {
  margin-top: 8rpx;
  color: #64748b;
  font-size: 22rpx;
  font-weight: 700;
}

.workbench-badge {
  position: absolute;
  top: 12rpx;
  right: 12rpx;
  z-index: 2;
  min-width: 30rpx;
  height: 30rpx;
  padding: 0 8rpx;
  border: 3rpx solid #ffffff;
  border-radius: 999rpx;
  color: #ffffff;
  font-size: 18rpx;
  font-weight: 950;
  line-height: 24rpx;
  text-align: center;
  background: #ef4444;
  box-sizing: border-box;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14rpx;
  padding: 0;
}

.shortcut-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
}

.shortcut-icon {
  position: relative;
  display: flex;
  width: 96rpx;
  height: 96rpx;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1rpx solid rgba(255, 255, 255, 0.86);
  border-radius: 26rpx;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.05);
  backdrop-filter: blur(20rpx);
}

.shortcut-icon::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.72;
}

.bg-blue::before {
  background: rgba(219, 234, 254, 0.86);
}

.bg-indigo::before {
  background: rgba(224, 231, 255, 0.86);
}

.bg-purple::before {
  background: rgba(243, 232, 255, 0.86);
}

.bg-amber::before {
  background: rgba(254, 243, 199, 0.86);
}

.bg-emerald::before {
  background: rgba(209, 250, 229, 0.86);
}

.bg-teal::before {
  background: rgba(204, 251, 241, 0.86);
}

.bg-slate::before {
  background: rgba(241, 245, 249, 0.86);
}

.icon-mask {
  position: relative;
  z-index: 1;
  width: 44rpx;
  height: 44rpx;
}

.shortcut-label {
  max-width: 100%;
  overflow: hidden;
  color: #475569;
  font-size: 24rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feed-section {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.section-head {
  padding: 0 2rpx;
}

.section-title {
  color: #1e293b;
  font-size: 31rpx;
  font-weight: 950;
}

.section-link {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin: 0;
  padding: 0;
  color: #2563eb;
  font-size: 25rpx;
  font-weight: 850;
  line-height: 1;
  background: transparent;
}

.arrow-icon {
  width: 24rpx;
  height: 24rpx;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.message-card {
  gap: 18rpx;
  padding: 20rpx 22rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.82);
  border-radius: 26rpx;
  background: rgba(255, 255, 255, 0.66);
  box-shadow: 0 8rpx 28rpx rgba(15, 23, 42, 0.04);
  backdrop-filter: blur(24rpx);
}

.message-icon {
  position: relative;
  display: flex;
  width: 72rpx;
  height: 72rpx;
  flex: 0 0 72rpx;
  align-items: center;
  justify-content: center;
  overflow: visible;
  border: 1rpx solid rgba(226, 232, 240, 0.86);
  border-radius: 22rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.04);
}

.message-icon::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 22rpx;
  opacity: 0.74;
}

.message-dot {
  position: absolute;
  top: -6rpx;
  right: -6rpx;
  z-index: 2;
  width: 22rpx;
  height: 22rpx;
  border: 4rpx solid #ffffff;
  border-radius: 999rpx;
  background: #ef4444;
}

.message-main {
  min-width: 0;
  flex: 1;
}

.message-title {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: #1e293b;
  font-size: 27rpx;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-time {
  flex: 0 0 auto;
  margin-left: 14rpx;
  color: #94a3b8;
  font-size: 22rpx;
  font-weight: 700;
}

.message-desc {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 6rpx;
  color: #64748b;
  font-size: 23rpx;
  line-height: 1.35;
  text-overflow: ellipsis;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.message-empty-card {
  display: flex;
  min-height: 92rpx;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  border: 1rpx solid rgba(209, 250, 229, 0.9);
  border-radius: 26rpx;
  background: rgba(240, 253, 244, 0.74);
}

.message-empty-card text {
  color: #047857;
  font-size: 25rpx;
  font-weight: 850;
}

.menu-browser {
  display: grid;
  height: calc(96vh - 258rpx - env(safe-area-inset-bottom));
  min-height: 680rpx;
  grid-template-columns: 176rpx minmax(0, 1fr);
  gap: 20rpx;
  overflow: hidden;
}

.menu-search-bar {
  display: flex;
  height: 72rpx;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
  padding: 0 18rpx;
  border: 1rpx solid rgba(226, 232, 240, 0.94);
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 8rpx 22rpx rgba(15, 23, 42, 0.04);
  box-sizing: border-box;
}

.menu-search-input {
  min-width: 0;
  height: 70rpx;
  flex: 1;
  color: #1e293b;
  font-size: 25rpx;
  font-weight: 750;
  line-height: 70rpx;
}

.menu-search-placeholder {
  color: #94a3b8;
  font-weight: 650;
}

.menu-search-clear {
  display: flex;
  width: 44rpx;
  height: 44rpx;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  border-radius: 999rpx;
  background: rgba(241, 245, 249, 0.92);
}

.menu-search-clear::after {
  border: 0;
}

.menu-group-pane,
.menu-list-pane {
  min-height: 0;
  height: 100%;
}

.menu-group-pane {
  border-right: 1rpx solid rgba(226, 232, 240, 0.86);
}

.menu-group-item {
  position: relative;
  display: flex;
  min-height: 64rpx;
  align-items: center;
  justify-content: center;
  margin: 0 14rpx 8rpx 0;
  padding: 10rpx 14rpx;
  border: 1rpx solid transparent;
  border-radius: 18rpx;
  box-sizing: border-box;
}

.menu-group-item.active {
  border-color: rgba(37, 99, 235, 0.18);
  background: #ffffff;
  box-shadow: 0 10rpx 26rpx rgba(37, 99, 235, 0.08);
}

.menu-group-item.active::before {
  content: '';
  position: absolute;
  top: 16rpx;
  bottom: 16rpx;
  left: -1rpx;
  width: 6rpx;
  border-radius: 999rpx;
  background: #2563eb;
}

.menu-group-name,
.menu-list-title,
.menu-list-name,
.menu-empty-title,
.menu-empty-desc {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-group-name {
  color: #334155;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 1.25;
  text-align: center;
}

.menu-group-item.active .menu-group-name {
  color: #1d4ed8;
}

.menu-list-pane {
  padding-right: 2rpx;
  box-sizing: border-box;
}

.menu-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 16rpx;
  padding: 4rpx 2rpx 0;
}

.menu-list-title {
  flex: 1;
  color: #0f172a;
  font-size: 30rpx;
  font-weight: 950;
  line-height: 1.2;
  white-space: nowrap;
}

.menu-list-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
  padding-bottom: 22rpx;
}

.menu-list-card {
  display: flex;
  min-width: 0;
  min-height: 122rpx;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 12rpx 6rpx;
  border: 1rpx solid rgba(226, 232, 240, 0.76);
  border-radius: 22rpx;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 250, 252, 0.78));
  box-shadow: 0 6rpx 18rpx rgba(15, 23, 42, 0.035);
  box-sizing: border-box;
}

.menu-list-card:active {
  border-color: rgba(203, 213, 225, 0.95);
  background: rgba(241, 245, 249, 0.92);
}

.menu-list-icon {
  position: relative;
  display: flex;
  width: 64rpx;
  height: 64rpx;
  flex: 0 0 64rpx;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1rpx solid transparent;
  border-radius: 18rpx;
  box-sizing: border-box;
}

.menu-list-icon .ai-icon {
  position: relative;
  z-index: 1;
}

.menu-list-copy {
  min-width: 0;
  width: 100%;
}

.menu-list-name {
  color: #1e293b;
  font-size: 22rpx;
  font-weight: 900;
  line-height: 1.25;
  text-align: center;
  white-space: nowrap;
}

.menu-empty {
  display: flex;
  min-height: 460rpx;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48rpx 20rpx;
  box-sizing: border-box;
}

.menu-empty-icon {
  display: flex;
  width: 96rpx;
  height: 96rpx;
  align-items: center;
  justify-content: center;
  border-radius: 28rpx;
  background: rgba(241, 245, 249, 0.9);
}

.menu-empty-title {
  margin-top: 22rpx;
  color: #334155;
  font-size: 28rpx;
  font-weight: 900;
}

.menu-empty-desc {
  margin-top: 8rpx;
  color: #94a3b8;
  font-size: 23rpx;
  font-weight: 650;
  text-align: center;
}

.animate-in {
  animation: enterUp 0.58s ease both;
}

.delay-1 {
  animation-delay: 0.08s;
}

.delay-2 {
  animation-delay: 0.16s;
}

.delay-3 {
  animation-delay: 0.24s;
}

.delay-4 {
  animation-delay: 0.32s;
}

@keyframes enterUp {
  from {
    opacity: 0;
    transform: translateY(28rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes floatGlow {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(36rpx, 28rpx, 0) scale(1.08);
  }
}
</style>
