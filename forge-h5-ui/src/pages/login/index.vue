<template>
  <view class="login-page">
    <view class="brand-panel">
      <view class="brand-mark">
        <image class="brand-logo" src="/static/logo.png" mode="aspectFit" />
      </view>
      <view class="brand-copy">
        <text class="brand-title">{{ title }}</text>
        <text class="brand-subtitle">移动端业务入口</text>
      </view>
    </view>

    <view class="login-card">
      <view class="section-heading">
        <text class="heading-title">账号登录</text>
        <text class="heading-subtitle">使用 Forge 账号进入 H5 工作台</text>
      </view>

      <view class="form-stack">
        <view class="field">
          <text class="field-label">用户名</text>
          <input
            v-model="form.username"
            class="field-input"
            placeholder="请输入用户名"
            placeholder-class="field-placeholder"
            confirm-type="next"
          />
        </view>

        <view class="field">
          <text class="field-label">密码</text>
          <input
            v-model="form.password"
            class="field-input"
            password
            placeholder="请输入密码"
            placeholder-class="field-placeholder"
            confirm-type="done"
            @confirm="handleLogin"
          />
        </view>
      </view>

      <button class="login-button" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <view class="config-line">
        <text>客户端：{{ userClient }}</text>
        <text>接口：{{ requestPrefix }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { useAuthStore } from '@/store'

export default {
  data() {
    return {
      title: import.meta.env.VITE_TITLE || 'Forge H5',
      userClient: import.meta.env.VITE_USER_CLIENT || 'h5',
      requestPrefix: import.meta.env.VITE_REQUEST_PREFIX || '/',
      loading: false,
      form: {
        username: '',
        password: '',
      },
    }
  },
  onLoad() {
    const authStore = useAuthStore()
    if (authStore.isLogin) {
      uni.reLaunch({ url: '/pages/index/index' })
    }
  },
  methods: {
    async handleLogin() {
      const username = this.form.username.trim()
      const password = this.form.password
      if (!username || !password) {
        uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
        return
      }

      this.loading = true
      try {
        const authStore = useAuthStore()
        await authStore.login({ username, password })
        uni.showToast({ title: '登录成功', icon: 'success' })
        uni.reLaunch({ url: '/pages/index/index' })
      }
      catch (error) {
        console.error('登录失败:', error)
      }
      finally {
        this.loading = false
      }
    },
  },
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  padding: 96rpx 36rpx 48rpx;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 18% 12%, rgba(209, 39, 35, 0.16), transparent 32%),
    linear-gradient(180deg, #fff7f5 0%, #f7f8fb 46%, #eef2f7 100%);
}

.brand-panel {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 56rpx;
}

.brand-mark {
  width: 104rpx;
  height: 104rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 32rpx;
  background: #ffffff;
  box-shadow: 0 20rpx 46rpx rgba(120, 26, 24, 0.12);
}

.brand-logo {
  width: 72rpx;
  height: 72rpx;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.brand-title {
  color: #181a20;
  font-size: 44rpx;
  font-weight: 800;
}

.brand-subtitle {
  color: #6b7280;
  font-size: 26rpx;
}

.login-card {
  padding: 42rpx 34rpx 32rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.72);
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 28rpx 80rpx rgba(30, 38, 58, 0.12);
}

.section-heading {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  margin-bottom: 34rpx;
}

.heading-title {
  color: #111827;
  font-size: 36rpx;
  font-weight: 800;
}

.heading-subtitle {
  color: #7a8190;
  font-size: 25rpx;
}

.form-stack {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.field {
  padding: 22rpx 24rpx;
  border: 1rpx solid #edf0f5;
  border-radius: 22rpx;
  background: #f8fafc;
}

.field-label {
  display: block;
  margin-bottom: 14rpx;
  color: #596171;
  font-size: 24rpx;
}

.field-input {
  width: 100%;
  height: 46rpx;
  color: #111827;
  font-size: 30rpx;
}

.field-placeholder {
  color: #a2a9b6;
}

.login-button {
  height: 92rpx;
  margin-top: 36rpx;
  border-radius: 24rpx;
  border: 0;
  color: #ffffff;
  font-size: 31rpx;
  font-weight: 700;
  line-height: 92rpx;
  background: linear-gradient(135deg, #d12723 0%, #f05a3d 100%);
  box-shadow: 0 20rpx 34rpx rgba(209, 39, 35, 0.22);
}

.login-button[disabled] {
  opacity: 0.72;
}

.config-line {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 24rpx;
  color: #9aa1ad;
  font-size: 22rpx;
}
</style>
