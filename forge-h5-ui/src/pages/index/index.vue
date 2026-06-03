<template>
  <view class="home-page">
    <view class="top-shell">
      <view>
        <text class="eyebrow">FORGE H5</text>
        <text class="title">{{ greeting }}</text>
      </view>
      <button v-if="authStore.isLogin" class="ghost-button" @click="handleLogout">退出</button>
    </view>

    <view v-if="authStore.isLogin" class="profile-card">
      <view class="avatar">
        <text>{{ avatarText }}</text>
      </view>
      <view class="profile-main">
        <text class="profile-name">{{ authStore.displayName }}</text>
        <text class="profile-meta">{{ authStore.userInfo?.username || '-' }}</text>
      </view>
      <view class="status-pill">
        <text>已登录</text>
      </view>
    </view>

    <view v-else class="guest-card">
      <text class="guest-title">还未登录</text>
      <text class="guest-desc">登录后可以访问移动端业务工作台和个人信息。</text>
      <button class="primary-button" @click="goLogin">去登录</button>
    </view>

    <view class="metric-grid">
      <view class="metric-card">
        <text class="metric-label">服务</text>
        <text class="metric-value">App Server</text>
      </view>
      <view class="metric-card">
        <text class="metric-label">客户端</text>
        <text class="metric-value">{{ userClient }}</text>
      </view>
    </view>

    <view class="action-list">
      <view class="action-item" @click="refreshUser">
        <view>
          <text class="action-title">刷新用户信息</text>
          <text class="action-desc">调用 /auth/userInfo 验证 token 和用户态</text>
        </view>
        <text class="action-arrow">›</text>
      </view>
      <view class="action-item disabled">
        <view>
          <text class="action-title">业务功能入口</text>
          <text class="action-desc">等待 app-server 增加 H5 专属业务接口</text>
        </view>
        <text class="action-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store'

const authStore = useAuthStore()
const userClient = import.meta.env.VITE_USER_CLIENT || 'h5'

const greeting = computed(() => {
  return authStore.isLogin ? '移动工作台' : '欢迎使用移动端'
})

const avatarText = computed(() => {
  const name = authStore.displayName || 'U'
  return name.slice(0, 1).toUpperCase()
})

onShow(async () => {
  if (!authStore.isLogin) {
    return
  }
  if (!authStore.userInfo) {
    await authStore.fetchUserInfo()
  }
})

function goLogin() {
  uni.navigateTo({ url: '/pages/login/index' })
}

async function refreshUser() {
  if (!authStore.isLogin) {
    goLogin()
    return
  }
  try {
    await authStore.fetchUserInfo()
    uni.showToast({ title: '已刷新', icon: 'success' })
  }
  catch (error) {
    console.error('刷新用户信息失败:', error)
  }
}

async function handleLogout() {
  await authStore.logout()
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  padding: 72rpx 32rpx 48rpx;
  box-sizing: border-box;
  background:
    linear-gradient(145deg, rgba(209, 39, 35, 0.12), rgba(255, 255, 255, 0) 44%),
    #f4f6f9;
}

.top-shell {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
  margin-bottom: 32rpx;
}

.eyebrow {
  display: block;
  margin-bottom: 8rpx;
  color: #d12723;
  font-size: 22rpx;
  font-weight: 800;
  letter-spacing: 0;
}

.title {
  display: block;
  color: #151923;
  font-size: 46rpx;
  font-weight: 900;
}

.ghost-button {
  min-width: 116rpx;
  height: 60rpx;
  padding: 0 22rpx;
  border: 1rpx solid #e2e6ee;
  border-radius: 18rpx;
  color: #5f6673;
  font-size: 24rpx;
  line-height: 60rpx;
  background: rgba(255, 255, 255, 0.82);
}

.profile-card,
.guest-card {
  border-radius: 30rpx;
  background: #ffffff;
  box-shadow: 0 22rpx 58rpx rgba(31, 41, 55, 0.1);
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 22rpx;
  padding: 30rpx;
}

.avatar {
  width: 92rpx;
  height: 92rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 28rpx;
  color: #ffffff;
  font-size: 38rpx;
  font-weight: 800;
  background: linear-gradient(135deg, #1f2937, #d12723);
}

.profile-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.profile-name {
  color: #111827;
  font-size: 32rpx;
  font-weight: 800;
}

.profile-meta {
  color: #7a8190;
  font-size: 24rpx;
}

.status-pill {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  color: #16794c;
  font-size: 22rpx;
  background: #e9f8ef;
}

.guest-card {
  padding: 34rpx;
}

.guest-title {
  display: block;
  color: #111827;
  font-size: 34rpx;
  font-weight: 800;
}

.guest-desc {
  display: block;
  margin: 14rpx 0 30rpx;
  color: #6b7280;
  font-size: 26rpx;
  line-height: 1.6;
}

.primary-button {
  height: 78rpx;
  border: 0;
  border-radius: 22rpx;
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 78rpx;
  background: #d12723;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
  margin: 26rpx 0;
}

.metric-card {
  padding: 26rpx;
  border-radius: 24rpx;
  background: #ffffff;
}

.metric-label,
.action-desc {
  color: #8a93a3;
  font-size: 23rpx;
}

.metric-value {
  display: block;
  margin-top: 10rpx;
  color: #171b24;
  font-size: 30rpx;
  font-weight: 800;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.action-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  padding: 28rpx 30rpx;
  border-radius: 24rpx;
  background: #ffffff;
}

.action-item.disabled {
  opacity: 0.58;
}

.action-title {
  display: block;
  margin-bottom: 8rpx;
  color: #111827;
  font-size: 29rpx;
  font-weight: 700;
}

.action-arrow {
  color: #b3bac6;
  font-size: 48rpx;
  line-height: 1;
}
</style>
