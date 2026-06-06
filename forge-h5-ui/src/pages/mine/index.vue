<template>
  <view class="mine-page">
    <view class="page-glow page-glow-blue" />
    <view class="page-glow page-glow-pink" />
    <view class="grid-layer" />

    <view class="mine-content">
      <view class="profile-card animate-in">
        <view class="profile-shape profile-shape-blue" />
        <view class="profile-shape profile-shape-pink" />

        <view class="avatar-frame">
          <view class="avatar-inner">
            <image class="avatar-image" :src="avatarUrl || '/static/logo.png'" mode="aspectFit" />
          </view>
        </view>

        <view class="profile-copy">
          <text class="profile-name">{{ authStore.displayName }}</text>
          <view class="member-badge">
            <view class="icon-mask badge-icon" :style="iconMask('/static/icons/ai-icon/award.svg', '#d97706')" />
            <text>高级会员</text>
          </view>
        </view>

        <view class="stats-grid">
          <view class="stat-item">
            <text class="stat-value">12</text>
            <text class="stat-label">优惠券</text>
          </view>
          <view class="stat-item stat-middle">
            <text class="stat-value">4.5k</text>
            <text class="stat-label">积分</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">2</text>
            <text class="stat-label">卡包</text>
          </view>
        </view>
      </view>

      <view class="menu-groups">
        <view
          v-for="(group, groupIndex) in menuGroups"
          :key="groupIndex"
          class="menu-group animate-in"
          :class="`delay-${groupIndex + 1}`"
        >
          <view
            v-for="item in group.items"
            :key="item.key"
            class="menu-row"
            @click="handleMenu(item)"
          >
            <view class="menu-icon" :class="item.bgClass">
              <view class="icon-mask" :style="iconMask(item.icon, item.color)" />
            </view>
            <view class="menu-main">
              <text class="menu-label">{{ item.label }}</text>
            </view>
            <view class="icon-mask chevron-icon" :style="iconMask('/static/icons/ai-icon/chevron-right.svg', '#94a3b8')" />
          </view>
        </view>
      </view>

      <view class="logout-wrap animate-in delay-3">
        <button class="logout-button" @click="handleLogout">
          <view class="icon-mask logout-icon" :style="iconMask('/static/icons/ai-icon/log-out.svg', '#ef4444')" />
          <text>退出登录</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store'
import { ensureLogin } from '@/utils/auth-guard'
import { showConfirmDialog } from '@/utils/dialog'
import { toast } from '@/utils/notify'

const authStore = useAuthStore()
const userInfo = computed(() => authStore.userInfo || {})
const avatarUrl = computed(() => userInfo.value.avatar || '')

const menuGroups = computed(() => [
  {
    items: [
      {
        key: 'profile',
        icon: '/static/icons/ai-icon/user.svg',
        label: '个人信息',
        color: '#3b82f6',
        bgClass: 'bg-blue',
      },
      {
        key: 'security',
        icon: '/static/icons/ai-icon/shield.svg',
        label: '安全中心',
        color: '#6366f1',
        bgClass: 'bg-indigo',
      },
      {
        key: 'notice',
        icon: '/static/icons/ai-icon/bell.svg',
        label: '通知设置',
        color: '#8b5cf6',
        bgClass: 'bg-purple',
      },
    ],
  },
  {
    items: [
      {
        key: 'help',
        icon: '/static/icons/ai-icon/help-circle.svg',
        label: '帮助与支持',
        color: '#10b981',
        bgClass: 'bg-emerald',
      },
      {
        key: 'settings',
        icon: '/static/icons/ai-icon/settings.svg',
        label: '通用设置',
        color: '#64748b',
        bgClass: 'bg-slate',
      },
    ],
  },
])

onShow(async () => {
  const ok = await ensureLogin({ redirect: '/pages/mine/index' })
  if (!ok) {
    return
  }
  if (!authStore.menus.length && !authStore.permissions.length) {
    await authStore.fetchAccessSnapshot()
  }
})

function iconMask(icon, color) {
  return {
    backgroundColor: color,
    WebkitMask: `url(${icon}) center / contain no-repeat`,
    mask: `url(${icon}) center / contain no-repeat`,
  }
}

function handleMenu(item) {
  if (item.key === 'profile') {
    refreshUser()
    return
  }
  toast(`${item.label}待接入`, { type: 'info' })
}

async function refreshUser() {
  try {
    await authStore.fetchUserInfo()
    await authStore.fetchAccessSnapshot()
    toast('已刷新', { type: 'success' })
  }
  catch (error) {
    console.error('刷新用户信息失败:', error)
  }
}

async function handleLogout() {
  const confirmed = await showConfirmDialog({
    title: '退出登录',
    description: '确认退出当前账号？退出后需要重新登录。',
    icon: 'warning',
    confirmText: '退出登录',
    cancelText: '取消',
    isDestructive: true,
  })
  if (!confirmed) {
    return
  }
  await authStore.logout()
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<style lang="scss" scoped>
.mine-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
  background: #f8fafc;
}

.mine-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 8%, rgba(147, 197, 253, 0.34), transparent 32%),
    radial-gradient(circle at 86% 34%, rgba(199, 210, 254, 0.3), transparent 34%),
    radial-gradient(circle at 26% 84%, rgba(251, 207, 232, 0.24), transparent 32%);
}

.grid-layer {
  position: absolute;
  inset: 0;
  opacity: 0.16;
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

.page-glow-pink {
  right: -180rpx;
  bottom: 160rpx;
  background: rgba(251, 207, 232, 0.36);
  animation: floatGlow 15s ease-in-out infinite reverse;
}

.mine-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 48rpx;
  padding: 56rpx 28rpx 132rpx;
  box-sizing: border-box;
}

.profile-card {
  position: relative;
  display: flex;
  overflow: hidden;
  flex-direction: column;
  align-items: center;
  margin-top: 8rpx;
  padding: 48rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.82);
  border-radius: 64rpx;
  background: rgba(255, 255, 255, 0.66);
  box-shadow: 0 20rpx 60rpx rgba(15, 23, 42, 0.05);
  backdrop-filter: blur(28rpx);
}

.profile-shape {
  position: absolute;
  border-radius: 999rpx;
  pointer-events: none;
}

.profile-shape-blue {
  top: -84rpx;
  right: -80rpx;
  width: 260rpx;
  height: 260rpx;
  background: rgba(191, 219, 254, 0.5);
  filter: blur(36rpx);
}

.profile-shape-pink {
  bottom: -76rpx;
  left: -58rpx;
  width: 220rpx;
  height: 220rpx;
  background: rgba(251, 207, 232, 0.52);
  filter: blur(32rpx);
}

.avatar-frame {
  position: relative;
  z-index: 1;
  width: 156rpx;
  height: 156rpx;
  padding: 8rpx;
  border-radius: 48rpx;
  background: linear-gradient(135deg, #3b82f6, #4f46e5);
  box-shadow: 0 16rpx 38rpx rgba(59, 130, 246, 0.28);
}

.avatar-inner {
  width: 100%;
  height: 100%;
  overflow: hidden;
  border: 4rpx solid #ffffff;
  border-radius: 38rpx;
  background: linear-gradient(135deg, #dbeafe, #e0e7ff);
}

.avatar-image {
  width: 100%;
  height: 100%;
  padding: 26rpx;
  box-sizing: border-box;
}

.profile-copy {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 26rpx;
}

.profile-name,
.member-badge text,
.stat-value,
.stat-label,
.menu-label,
.menu-desc,
.logout-button text {
  display: block;
}

.profile-name {
  max-width: 560rpx;
  overflow: hidden;
  color: #1e293b;
  font-size: 42rpx;
  font-weight: 950;
  line-height: 1.18;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-badge {
  display: flex;
  align-items: center;
  gap: 8rpx;
  max-width: 560rpx;
  margin-top: 16rpx;
  padding: 8rpx 18rpx;
  border: 1rpx solid rgba(251, 191, 36, 0.42);
  border-radius: 16rpx;
  background: linear-gradient(90deg, #fef3c7, #fef9c3);
  box-shadow: 0 6rpx 16rpx rgba(217, 119, 6, 0.08);
}

.member-badge text {
  overflow: hidden;
  color: #b45309;
  font-size: 22rpx;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.badge-icon {
  width: 28rpx;
  height: 28rpx;
  flex: 0 0 28rpx;
}

.stats-grid {
  position: relative;
  z-index: 1;
  display: grid;
  width: 100%;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  margin-top: 48rpx;
  padding-top: 48rpx;
  border-top: 1rpx solid rgba(203, 213, 225, 0.52);
}

.stat-item {
  min-width: 0;
  text-align: center;
}

.stat-middle {
  border-right: 1rpx solid rgba(203, 213, 225, 0.52);
  border-left: 1rpx solid rgba(203, 213, 225, 0.52);
}

.stat-value {
  overflow: hidden;
  color: #1e293b;
  font-size: 36rpx;
  font-weight: 950;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stat-label {
  margin-top: 8rpx;
  color: #64748b;
  font-size: 22rpx;
  font-weight: 800;
}

.menu-groups {
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

.menu-group {
  overflow: hidden;
  border: 1rpx solid rgba(255, 255, 255, 0.82);
  border-radius: 48rpx;
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 8rpx 28rpx rgba(15, 23, 42, 0.04);
  backdrop-filter: blur(24rpx);
}

.menu-row {
  display: flex;
  align-items: center;
  gap: 32rpx;
  padding: 32rpx;
  border-bottom: 1rpx solid rgba(226, 232, 240, 0.62);
}

.menu-row:last-child {
  border-bottom: 0;
}

.menu-icon {
  position: relative;
  display: flex;
  width: 80rpx;
  height: 80rpx;
  flex: 0 0 80rpx;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 24rpx;
}

.menu-icon::before {
  content: '';
  position: absolute;
  inset: 0;
}

.bg-blue::before {
  background: #eff6ff;
}

.bg-indigo::before {
  background: #eef2ff;
}

.bg-purple::before {
  background: #f5f3ff;
}

.bg-emerald::before {
  background: #ecfdf5;
}

.bg-slate::before {
  background: #f1f5f9;
}

.icon-mask {
  position: relative;
  z-index: 1;
  width: 40rpx;
  height: 40rpx;
}

.menu-main {
  min-width: 0;
  flex: 1;
}

.menu-label {
  overflow: hidden;
  color: #334155;
  font-size: 29rpx;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-desc {
  overflow: hidden;
  margin-top: 6rpx;
  color: #94a3b8;
  font-size: 23rpx;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chevron-icon {
  width: 34rpx;
  height: 34rpx;
  flex: 0 0 34rpx;
}

.logout-wrap {
  margin-top: 32rpx;
}

.logout-button {
  display: flex;
  height: 92rpx;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  padding: 0;
  border: 1rpx solid rgba(254, 202, 202, 0.9);
  border-radius: 40rpx;
  color: #ef4444;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 8rpx 28rpx rgba(239, 68, 68, 0.06);
  backdrop-filter: blur(18rpx);
}

.logout-button::after {
  border: 0;
}

.logout-button text {
  color: #ef4444;
  font-size: 29rpx;
  font-weight: 900;
}

.logout-icon {
  width: 38rpx;
  height: 38rpx;
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
