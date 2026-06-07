<template>
  <view class="home-page">
    <view class="page-glow page-glow-blue" />
    <view class="page-glow page-glow-indigo" />
    <view class="grid-layer" />

    <view class="home-content">
      <view class="home-header animate-in">
        <view class="user-block" @click="goMine">
          <view class="avatar-wrap">
            <image class="avatar-image" :src="avatarUrl" mode="aspectFit" @error="handleAvatarError" />
          </view>
          <view class="user-copy">
            <text class="hello-title">Hi, {{ authStore.displayName }}</text>
            <text class="hello-subtitle">欢迎回来，继续探索 Forge H5</text>
          </view>
        </view>
        <button class="bell-button" @click="refreshWorkspace">
          <view class="icon-mask bell-icon" :style="iconMask('/static/icons/ai-icon/bell.svg', '#475569')" />
          <view class="bell-dot" />
        </button>
      </view>

      <view class="feature-card animate-in delay-1">
        <view class="feature-orb feature-orb-one" />
        <view class="feature-orb feature-orb-two" />
        <view class="feature-inner">
          <view class="feature-top">
            <text class="feature-label">服务概览</text>
            <text class="feature-chip">{{ syncStatusText }}</text>
          </view>
          <view class="feature-main">
            <view class="feature-metrics">
              <view class="metric-item">
                <text class="metric-value">{{ backendMenuCount }}</text>
                <text class="metric-label">菜单</text>
              </view>
              <view class="metric-divider" />
              <view class="metric-item">
                <text class="metric-value">{{ permissionCount }}</text>
                <text class="metric-label">权限</text>
              </view>
              <view class="metric-divider" />
              <view class="metric-item">
                <text class="metric-value">{{ userClientLabel }}</text>
                <text class="metric-label">客户端</text>
              </view>
            </view>
            <text class="feature-desc">最近同步：{{ lastSyncText }}</text>
          </view>
          <view class="feature-actions">
            <button class="feature-primary" @click="refreshWorkspace">刷新信息</button>
            <button class="feature-secondary" @click="goMine">个人中心</button>
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
            <view class="icon-mask" :style="iconMask(item.icon, item.color)" />
          </view>
          <text class="shortcut-label">{{ item.label }}</text>
        </view>
      </view>

      <view class="feed-section animate-in delay-3">
        <view class="section-head">
          <text class="section-title">最近消息</text>
          <button class="section-link" @click="refreshWorkspace">
            <text>查看全部</text>
            <view class="icon-mask arrow-icon" :style="iconMask('/static/icons/ai-icon/arrow-right.svg', '#2563eb')" />
          </button>
        </view>

        <view class="message-list">
          <view
            v-for="message in messages"
            :key="message.id"
            class="message-card"
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
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store'
import { ensureLogin } from '@/utils/auth-guard'
import { toast } from '@/utils/notify'
import logoUrl from '@/static/logo.png'

const authStore = useAuthStore()

const userClientLabel = (import.meta.env.VITE_USER_CLIENT || 'app').toUpperCase()
const lastSyncAt = ref('')

const avatarLoadFailed = ref(false)
const rawAvatarUrl = computed(() => authStore.userInfo?.avatar || '')
const avatarUrl = computed(() => {
  if (!rawAvatarUrl.value || avatarLoadFailed.value) {
    return logoUrl
  }
  return rawAvatarUrl.value
})

watch(rawAvatarUrl, () => {
  avatarLoadFailed.value = false
})

const handleAvatarError = () => {
  avatarLoadFailed.value = true
}

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

const menuItems = computed(() => {
  const backendItems = flattenMenus(authStore.menus)
  const sourceItems = backendItems.length ? backendItems : fallbackMenuItems
  return [componentDemoItem, ...sourceItems].slice(0, 8)
})

const backendMenuCount = computed(() => flattenMenus(authStore.menus).length)
const permissionCount = computed(() => Array.isArray(authStore.permissions) ? authStore.permissions.length : 0)
const syncStatusText = computed(() => backendMenuCount.value ? '已接入' : '待接入')
const lastSyncText = computed(() => lastSyncAt.value || '未同步')

const messages = computed(() => [
  {
    id: 1,
    title: '系统更新',
    desc: 'Forge H5 模板已准备好登录、鉴权和用户端基础页面。',
    time: '刚刚',
    icon: '/static/icons/ai-icon/activity.svg',
    color: '#3b82f6',
    bgClass: 'bg-blue',
    unread: true,
  },
  {
    id: 2,
    title: '登录成功',
    desc: `${authStore.displayName} 已通过 ${userClientLabel} 客户端完成安全登录。`,
    time: '2小时前',
    icon: '/static/icons/ai-icon/credit-card.svg',
    color: '#10b981',
    bgClass: 'bg-emerald',
    unread: false,
  },
  {
    id: 3,
    title: '欢迎使用 Forge',
    desc: '这里可以继续接入订单、权益、消息、服务等用户端 H5 功能。',
    time: '1天前',
    icon: '/static/icons/ai-icon/message-square.svg',
    color: '#8b5cf6',
    bgClass: 'bg-purple',
    unread: false,
  },
])

function flattenMenus(menus = []) {
  const result = []
  const toneList = [
    { icon: '/static/icons/ai-icon/pocket.svg', color: '#2563eb', bgClass: 'bg-blue' },
    { icon: '/static/icons/ai-icon/credit-card.svg', color: '#4f46e5', bgClass: 'bg-indigo' },
    { icon: '/static/icons/ai-icon/pie-chart.svg', color: '#7c3aed', bgClass: 'bg-purple' },
    { icon: '/static/icons/ai-icon/zap.svg', color: '#d97706', bgClass: 'bg-amber' },
    { icon: '/static/icons/ai-icon/message-square.svg', color: '#10b981', bgClass: 'bg-emerald' },
  ]

  function walk(list = []) {
    list
      .filter(menu => menu && menu.visible !== 0 && menu.menuStatus !== 0)
      .forEach((menu) => {
        const children = Array.isArray(menu.children) ? menu.children : []
        if (children.length) {
          walk(children)
          return
        }
        const tone = toneList[result.length % toneList.length]
        result.push({
          key: menu.id || menu.path || menu.resourceName,
          label: menu.resourceName || menu.title || menu.name || '未命名',
          path: menu.path,
          component: menu.component,
          external: menu.isExternal === 1,
          fromBackend: true,
          ...tone,
        })
      })
  }

  walk(menus)
  return result
}

onShow(async () => {
  const ok = await ensureLogin({ redirect: '/pages/index/index' })
  if (!ok) {
    return
  }
  await refreshWorkspace({ silent: true })
})

function iconMask(icon, color) {
  return {
    backgroundColor: color,
    WebkitMask: `url(${icon}) center / contain no-repeat`,
    mask: `url(${icon}) center / contain no-repeat`,
  }
}

function goMine() {
  uni.switchTab({ url: '/pages/mine/index' })
}

function handleShortcut(item) {
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

function openBackendMenu(item) {
  const path = item.component || item.path
  if (path && path.startsWith('/pages/')) {
    uni.navigateTo({ url: path })
    return
  }
  toast(`${item.label}页面待接入`, { type: 'info' })
}

async function refreshWorkspace(options = {}) {
  try {
    await authStore.fetchUserInfo()
    await authStore.fetchAccessSnapshot()
    lastSyncAt.value = formatCurrentTime()
    if (!options.silent) {
      toast('已同步', { type: 'success' })
    }
  }
  catch (error) {
    console.error('刷新首页信息失败:', error)
  }
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
  gap: 36rpx;
  padding: 56rpx 28rpx 132rpx;
  box-sizing: border-box;
}

.home-header,
.user-block,
.feature-top,
.feature-actions,
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
  padding: 18rpx;
  box-sizing: border-box;
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
.feature-primary::after,
.feature-secondary::after,
.section-link::after {
  border: 0;
}

.bell-icon {
  width: 38rpx;
  height: 38rpx;
  margin: 20rpx auto 0;
}

.bell-dot {
  position: absolute;
  top: 20rpx;
  right: 20rpx;
  width: 14rpx;
  height: 14rpx;
  border: 4rpx solid #ffffff;
  border-radius: 999rpx;
  background: #ef4444;
}

.feature-card {
  position: relative;
  overflow: hidden;
  min-height: 320rpx;
  border-radius: 40rpx;
  background: linear-gradient(135deg, #2563eb, #4f46e5 74%, #4338ca);
  box-shadow: 0 28rpx 56rpx rgba(79, 70, 229, 0.28);
}

.feature-orb {
  position: absolute;
  border-radius: 999rpx;
  pointer-events: none;
}

.feature-orb-one {
  top: -120rpx;
  right: -80rpx;
  width: 360rpx;
  height: 360rpx;
  background: rgba(255, 255, 255, 0.12);
  filter: blur(48rpx);
}

.feature-orb-two {
  bottom: -90rpx;
  left: -70rpx;
  width: 260rpx;
  height: 260rpx;
  background: rgba(96, 165, 250, 0.28);
  filter: blur(42rpx);
}

.feature-inner {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 320rpx;
  flex-direction: column;
  justify-content: space-between;
  padding: 44rpx 40rpx 38rpx;
  box-sizing: border-box;
}

.feature-label {
  color: rgba(255, 255, 255, 0.78);
  font-size: 27rpx;
  font-weight: 700;
}

.feature-chip {
  padding: 8rpx 22rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.26);
  border-radius: 999rpx;
  color: #ffffff;
  font-size: 22rpx;
  font-weight: 900;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(16rpx);
}

.feature-main {
  margin-top: 30rpx;
}

.feature-metrics {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 1rpx minmax(0, 1fr) 1rpx minmax(0, 1fr);
  align-items: center;
  gap: 18rpx;
}

.metric-item {
  min-width: 0;
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
  font-size: 46rpx;
  font-weight: 950;
  line-height: 1.05;
  text-shadow: 0 4rpx 14rpx rgba(15, 23, 42, 0.12);
}

.metric-label {
  margin-top: 12rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 22rpx;
  font-weight: 750;
}

.metric-divider {
  width: 1rpx;
  height: 70rpx;
  background: rgba(255, 255, 255, 0.22);
}

.feature-desc {
  display: block;
  margin-top: 24rpx;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.78);
  font-size: 25rpx;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feature-actions {
  gap: 22rpx;
  margin-top: 34rpx;
}

.feature-primary,
.feature-secondary {
  height: 88rpx;
  flex: 1;
  padding: 0;
  border-radius: 28rpx;
  font-size: 28rpx;
  font-weight: 850;
  line-height: 88rpx;
}

.feature-primary {
  color: #2563eb;
  background: #ffffff;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.12);
}

.feature-secondary {
  color: #ffffff;
  border: 1rpx solid rgba(255, 255, 255, 0.34);
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(18rpx);
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18rpx;
  padding: 4rpx 0;
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
  width: 112rpx;
  height: 112rpx;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1rpx solid rgba(255, 255, 255, 0.86);
  border-radius: 28rpx;
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
  gap: 24rpx;
}

.section-head {
  padding: 0 2rpx;
}

.section-title {
  color: #1e293b;
  font-size: 34rpx;
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
  gap: 18rpx;
}

.message-card {
  gap: 22rpx;
  padding: 26rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.82);
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.66);
  box-shadow: 0 8rpx 28rpx rgba(15, 23, 42, 0.04);
  backdrop-filter: blur(24rpx);
}

.message-icon {
  position: relative;
  display: flex;
  width: 92rpx;
  height: 92rpx;
  flex: 0 0 92rpx;
  align-items: center;
  justify-content: center;
  overflow: visible;
  border: 1rpx solid rgba(226, 232, 240, 0.86);
  border-radius: 28rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.04);
}

.message-icon::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 28rpx;
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
  font-size: 29rpx;
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
  margin-top: 10rpx;
  color: #64748b;
  font-size: 25rpx;
  line-height: 1.45;
  text-overflow: ellipsis;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
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
