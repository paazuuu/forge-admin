<template>
  <view
    class="ai-icon"
    :class="[`ai-icon--${size}`, { 'ai-icon--tile': tile }]"
    :style="iconStyle"
  />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: {
    type: String,
    default: ''
  },
  src: {
    type: String,
    default: ''
  },
  color: {
    type: String,
    default: '#475569'
  },
  size: {
    type: String,
    default: 'md',
    validator: value => ['sm', 'md', 'lg', 'xl'].includes(value)
  },
  tile: {
    type: Boolean,
    default: false
  }
})

const iconUrl = computed(() => props.src || (props.name ? `/static/icons/ai-icon/${props.name}.svg` : ''))

const iconStyle = computed(() => {
  if (!iconUrl.value) {
    return {}
  }
  return {
    backgroundColor: props.color,
    WebkitMask: `url(${iconUrl.value}) center / contain no-repeat`,
    mask: `url(${iconUrl.value}) center / contain no-repeat`,
  }
})
</script>

<style lang="scss" scoped>
.ai-icon {
  display: inline-block;
  flex-shrink: 0;
}

.ai-icon--sm {
  width: 28rpx;
  height: 28rpx;
}

.ai-icon--md {
  width: 38rpx;
  height: 38rpx;
}

.ai-icon--lg {
  width: 48rpx;
  height: 48rpx;
}

.ai-icon--xl {
  width: 64rpx;
  height: 64rpx;
}

.ai-icon--tile {
  border-radius: 18rpx;
}
</style>
