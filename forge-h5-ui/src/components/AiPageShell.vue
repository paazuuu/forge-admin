<template>
  <view class="ai-page-shell">
    <view class="ai-page-shell__glow ai-page-shell__glow--blue" />
    <view class="ai-page-shell__glow ai-page-shell__glow--pink" />
    <view v-if="grid" class="ai-page-shell__grid" />
    <view class="ai-page-shell__content" :class="{ 'ai-page-shell__content--safe': safeBottom }">
      <slot />
    </view>
  </view>
</template>

<script setup>
defineProps({
  grid: {
    type: Boolean,
    default: true
  },
  safeBottom: {
    type: Boolean,
    default: true
  }
})
</script>

<style lang="scss" scoped>
.ai-page-shell {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
  background: #f8fafc;
}

.ai-page-shell::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 10%, rgba(147, 197, 253, 0.32), transparent 34%),
    radial-gradient(circle at 86% 28%, rgba(199, 210, 254, 0.28), transparent 34%),
    radial-gradient(circle at 28% 82%, rgba(251, 207, 232, 0.22), transparent 32%);
}

.ai-page-shell__grid {
  position: absolute;
  inset: 0;
  opacity: 0.16;
  pointer-events: none;
  background-image:
    linear-gradient(#e2e8f0 1rpx, transparent 1rpx),
    linear-gradient(90deg, #e2e8f0 1rpx, transparent 1rpx);
  background-size: 80rpx 80rpx;
}

.ai-page-shell__glow {
  position: absolute;
  width: 520rpx;
  height: 520rpx;
  border-radius: 999rpx;
  filter: blur(90rpx);
  pointer-events: none;
}

.ai-page-shell__glow--blue {
  top: -180rpx;
  left: -160rpx;
  background: rgba(147, 197, 253, 0.42);
}

.ai-page-shell__glow--pink {
  right: -180rpx;
  bottom: 120rpx;
  background: rgba(251, 207, 232, 0.34);
}

.ai-page-shell__content {
  position: relative;
  z-index: 1;
  box-sizing: border-box;
  min-height: 100vh;
}

.ai-page-shell__content--safe {
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
