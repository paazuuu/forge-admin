<template>
  <view class="ai-empty">
    <view class="ai-empty__icon">
      <AiIcon :name="icon" color="#94a3b8" size="xl" />
    </view>
    <text class="ai-empty__title">{{ title }}</text>
    <text v-if="description" class="ai-empty__desc">{{ description }}</text>
    <view v-if="$slots.default" class="ai-empty__action">
      <slot />
    </view>
  </view>
</template>

<script setup>
import AiIcon from './AiIcon.vue'

defineProps({
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: 'inbox'
  }
})
</script>

<style lang="scss" scoped>
.ai-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 320rpx;
  padding: 48rpx 32rpx;
  box-sizing: border-box;
  text-align: center;
}

.ai-empty__icon {
  display: flex;
  width: 112rpx;
  height: 112rpx;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(226, 232, 240, 0.86);
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.04);
}

.ai-empty__title {
  display: block;
  margin-top: 24rpx;
  color: #334155;
  font-size: 30rpx;
  font-weight: 900;
}

.ai-empty__desc {
  display: block;
  max-width: 520rpx;
  margin-top: 10rpx;
  color: #64748b;
  font-size: 24rpx;
  font-weight: 600;
  line-height: 1.45;
}

.ai-empty__action {
  margin-top: 28rpx;
}
</style>
