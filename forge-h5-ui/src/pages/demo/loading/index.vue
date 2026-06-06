<template>
  <view class="loading-demo-page">
    <view class="page-glow page-glow-blue" />
    <view class="page-glow page-glow-pink" />
    <view class="grid-layer" />

    <view class="demo-content">
      <view class="demo-header animate-in">
        <button class="back-button" @click="goBack">
          <view class="icon-mask" :style="iconMask('/static/icons/ai-icon/chevron-left.svg', '#475569')" />
        </button>
        <view class="header-copy">
          <text class="header-title">加载演示</text>
          <text class="header-subtitle">Loading component preview</text>
        </view>
      </view>

      <view class="preview-card animate-in delay-1">
        <view class="preview-panel">
          <Loading
            visible
            :text="previewText"
            :type="currentType"
            :theme="currentTheme"
            :size="currentSize"
          />
        </view>
      </view>

      <view class="control-card animate-in delay-2">
        <view class="control-section">
          <text class="control-title">动画类型</text>
          <view class="segmented">
            <button
              v-for="item in typeOptions"
              :key="item.value"
              class="segment-button"
              :class="{ active: currentType === item.value }"
              @click="currentType = item.value"
            >
              {{ item.label }}
            </button>
          </view>
        </view>

        <view class="control-section">
          <text class="control-title">主题</text>
          <view class="segmented">
            <button
              v-for="item in themeOptions"
              :key="item.value"
              class="segment-button"
              :class="{ active: currentTheme === item.value }"
              @click="currentTheme = item.value"
            >
              {{ item.label }}
            </button>
          </view>
        </view>

        <view class="control-section">
          <text class="control-title">尺寸</text>
          <view class="segmented">
            <button
              v-for="item in sizeOptions"
              :key="item.value"
              class="segment-button"
              :class="{ active: currentSize === item.value }"
              @click="currentSize = item.value"
            >
              {{ item.label }}
            </button>
          </view>
        </view>
      </view>

      <view class="action-grid animate-in delay-3">
        <button class="action-button primary" @click="showFullscreenLoading">
          <view class="icon-mask button-icon" :style="iconMask('/static/icons/ai-icon/maximize.svg', '#ffffff')" />
          <text>全屏演示</text>
        </button>
        <button class="action-button secondary" @click="showServiceLoading">
          <view class="icon-mask button-icon" :style="iconMask('/static/icons/ai-icon/zap.svg', '#2563eb')" />
          <text>服务调用</text>
        </button>
      </view>
    </view>

    <Loading
      :visible="fullscreenVisible"
      text="正在加载..."
      :type="currentType"
      full-screen
      blur
      theme="brand"
      size="md"
      :z-index="12000"
    />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import Loading from '@/components/Loading.vue'
import { loadingService } from '@/directives'
import { toast } from '@/utils/notify'

const currentType = ref('brand')
const currentTheme = ref('brand')
const currentSize = ref('md')
const fullscreenVisible = ref(false)

const typeOptions = [
  { label: '品牌', value: 'brand' },
  { label: '圆环', value: 'spinner' },
  { label: '圆点', value: 'dots' },
]

const themeOptions = [
  { label: '品牌', value: 'brand' },
  { label: '浅色', value: 'light' },
  { label: '深色', value: 'dark' },
]

const sizeOptions = [
  { label: '小', value: 'sm' },
  { label: '中', value: 'md' },
  { label: '大', value: 'lg' },
]

const previewText = computed(() => {
  const typeLabel = typeOptions.find(item => item.value === currentType.value)?.label || ''
  return `${typeLabel}加载中...`
})

function iconMask(icon, color) {
  return {
    backgroundColor: color,
    WebkitMask: `url(${icon}) center / contain no-repeat`,
    mask: `url(${icon}) center / contain no-repeat`,
  }
}

function goBack() {
  uni.navigateBack({
    fail: () => uni.switchTab({ url: '/pages/index/index' }),
  })
}

function showFullscreenLoading() {
  fullscreenVisible.value = true
  setTimeout(() => {
    fullscreenVisible.value = false
    toast('全屏加载演示完成', { type: 'success' })
  }, 1800)
}

function showServiceLoading() {
  const loading = loadingService.show({
    text: '服务加载中...',
    type: currentType.value,
    theme: 'brand',
    size: currentSize.value,
  })
  setTimeout(() => {
    loading?.close?.()
    toast('服务调用演示完成', { type: 'success' })
  }, 1800)
}
</script>

<style lang="scss" scoped>
.loading-demo-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: #f8fafc;
}

.loading-demo-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 10%, rgba(147, 197, 253, 0.32), transparent 34%),
    radial-gradient(circle at 86% 28%, rgba(199, 210, 254, 0.28), transparent 34%),
    radial-gradient(circle at 28% 82%, rgba(251, 207, 232, 0.22), transparent 32%);
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
}

.page-glow-pink {
  right: -180rpx;
  bottom: 120rpx;
  background: rgba(251, 207, 232, 0.34);
}

.demo-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 28rpx;
  padding: 56rpx 28rpx 72rpx;
  box-sizing: border-box;
}

.demo-header {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.back-button {
  display: flex;
  width: 76rpx;
  height: 76rpx;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1rpx solid rgba(255, 255, 255, 0.86);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(20rpx);
}

.back-button::after,
.segment-button::after,
.action-button::after {
  border: 0;
}

.icon-mask {
  width: 34rpx;
  height: 34rpx;
}

.header-copy {
  min-width: 0;
  flex: 1;
}

.header-title,
.header-subtitle,
.control-title,
.action-button text {
  display: block;
}

.header-title {
  color: #1e293b;
  font-size: 42rpx;
  font-weight: 950;
  line-height: 1.15;
}

.header-subtitle {
  margin-top: 6rpx;
  color: #64748b;
  font-size: 24rpx;
  font-weight: 700;
}

.preview-card,
.control-card {
  border: 1rpx solid rgba(255, 255, 255, 0.82);
  border-radius: 40rpx;
  background: rgba(255, 255, 255, 0.66);
  box-shadow: 0 12rpx 36rpx rgba(15, 23, 42, 0.05);
  backdrop-filter: blur(24rpx);
}

.preview-card {
  padding: 32rpx;
}

.preview-panel {
  display: flex;
  min-height: 360rpx;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(226, 232, 240, 0.7);
  border-radius: 32rpx;
  background:
    radial-gradient(circle at 50% 28%, rgba(219, 234, 254, 0.72), transparent 42%),
    rgba(248, 250, 252, 0.72);
}

.control-card {
  display: flex;
  flex-direction: column;
  gap: 28rpx;
  padding: 30rpx;
}

.control-section {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.control-title {
  color: #334155;
  font-size: 27rpx;
  font-weight: 900;
}

.segmented {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
  padding: 8rpx;
  border-radius: 24rpx;
  background: rgba(241, 245, 249, 0.78);
}

.segment-button {
  height: 72rpx;
  padding: 0;
  border-radius: 18rpx;
  color: #64748b;
  font-size: 25rpx;
  font-weight: 850;
  line-height: 72rpx;
  background: transparent;
}

.segment-button.active {
  color: #2563eb;
  background: #ffffff;
  box-shadow: 0 6rpx 18rpx rgba(37, 99, 235, 0.12);
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.action-button {
  display: flex;
  height: 92rpx;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  padding: 0 10px;
  border-radius: 28rpx;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 92rpx;
}

.action-button.primary {
  color: #ffffff;
  background: linear-gradient(90deg, #2563eb, #4f46e5);
  box-shadow: 0 12rpx 28rpx rgba(37, 99, 235, 0.22);
}

.action-button.secondary {
  color: #2563eb;
  border: 1rpx solid rgba(191, 219, 254, 0.78);
  background: rgba(255, 255, 255, 0.78);
}

.button-icon {
  width: 34rpx;
  height: 34rpx;
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
</style>
