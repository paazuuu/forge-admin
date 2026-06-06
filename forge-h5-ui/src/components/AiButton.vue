<template>
  <button
    class="ai-button"
    :class="[
      `ai-button--${variant}`,
      `ai-button--${size}`,
      {
        'ai-button--block': block,
        'ai-button--loading': loading,
        'ai-button--disabled': disabled || loading
      }
    ]"
    :disabled="disabled || loading"
    :hover-class="disabled || loading ? 'none' : 'ai-button--hover'"
    @click="handleClick"
  >
    <view v-if="variant === 'primary' && !disabled && !loading" class="ai-button__shine" />
    <view class="ai-button__content">
      <view v-if="loading" class="ai-button__spinner" />
      <view v-else-if="$slots.leftIcon" class="ai-button__icon ai-button__icon--left">
        <slot name="leftIcon" />
      </view>
      <text class="ai-button__text"><slot /></text>
      <view v-if="!loading && $slots.rightIcon" class="ai-button__icon ai-button__icon--right">
        <slot name="rightIcon" />
      </view>
    </view>
  </button>
</template>

<script setup>
const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: value => ['primary', 'secondary', 'outline', 'ghost', 'danger'].includes(value)
  },
  size: {
    type: String,
    default: 'md',
    validator: value => ['sm', 'md', 'lg'].includes(value)
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  block: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const handleClick = (event) => {
  if (props.disabled || props.loading) return
  emit('click', event)
}
</script>

<style lang="scss" scoped>
.ai-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  min-width: 0;
  margin: 0;
  padding: 0 48rpx;
  border: 1px solid transparent;
  overflow: hidden;
  font-weight: 700;
  letter-spacing: 0;
  line-height: 1;
  transition: transform 0.2s ease, box-shadow 0.25s ease, background 0.25s ease, opacity 0.2s ease;
  transform: translateZ(0);
  
  &::after {
    border: 0;
  }

  &--hover {
    transform: scale(0.97);
  }

  &--block {
    width: 100%;
  }

  &--disabled {
    opacity: 0.6;
  }

  &--sm {
    height: 72rpx;
    padding: 0 32rpx;
    border-radius: 24rpx;
    font-size: 26rpx;
  }

  &--md {
    height: 96rpx;
    border-radius: 32rpx;
    font-size: 30rpx;
  }

  &--lg {
    height: 112rpx;
    padding: 0 64rpx;
    border-radius: 40rpx;
    font-size: 32rpx;
  }

  &--primary {
    color: #ffffff;
    background: linear-gradient(135deg, #2563eb 0%, #4f46e5 100%);
    box-shadow: 0 16rpx 40rpx rgba(59, 130, 246, 0.25);
  }

  &--secondary {
    color: #334155;
    background: rgba(255, 255, 255, 0.8);
    border-color: rgba(255, 255, 255, 0.95);
    box-shadow: 0 8rpx 30rpx rgba(15, 23, 42, 0.04);
    backdrop-filter: blur(20rpx);
  }

  &--outline {
    color: #334155;
    background: rgba(255, 255, 255, 0.18);
    border-color: rgba(203, 213, 225, 0.86);
    backdrop-filter: blur(12rpx);
  }

  &--ghost {
    color: #475569;
    background: transparent;
  }

  &--danger {
    color: #ef4444;
    background: rgba(255, 255, 255, 0.82);
    border-color: rgba(254, 202, 202, 0.9);
    box-shadow: 0 8rpx 30rpx rgba(239, 68, 68, 0.06);
    backdrop-filter: blur(20rpx);
  }
}

.ai-button__content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  min-width: 0;
}

.ai-button__text {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-button__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-button__spinner {
  width: 34rpx;
  height: 34rpx;
  border: 4rpx solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: ai-button-spin 0.8s linear infinite;
}

.ai-button__shine {
  position: absolute;
  top: -20%;
  bottom: -20%;
  left: -45%;
  width: 38%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.26), transparent);
  transform: skewX(-18deg);
  animation: ai-button-shine 3.2s ease-in-out infinite;
}

@keyframes ai-button-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes ai-button-shine {
  0%,
  52% {
    transform: translateX(0) skewX(-18deg);
  }
  100% {
    transform: translateX(420%) skewX(-18deg);
  }
}
</style>
