<template>
  <view class="ai-tag" :class="[`ai-tag--${type}`, `ai-tag--${size}`]" :style="customStyle">
    <slot></slot>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'default'
  },
  size: {
    type: String,
    default: 'medium'
  },
  color: {
    type: String,
    default: ''
  }
})

const customStyle = computed(() => {
  if (props.color) {
    return {
      backgroundColor: props.color,
      borderColor: props.color,
      color: '#fff'
    }
  }
  return {}
})
</script>

<style lang="scss" scoped>
.ai-tag {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1;
  
  &--default {
    background-color: #f5f5f5;
    color: #666;
  }
  
  &--primary {
    background-color: var(--primary-color, #d12723FF);
    color: #fff;
  }
  
  &--success {
    background-color: #52c41a;
    color: #fff;
  }
  
  &--warning {
    background-color: #faad14;
    color: #fff;
  }
  
  &--error {
    background-color: #ff4d4f;
    color: #fff;
  }
  
  &--small {
    padding: 2px 6px;
    font-size: 10px;
  }
  
  &--medium {
    padding: 4px 8px;
    font-size: 12px;
  }
  
  &--large {
    padding: 6px 12px;
    font-size: 14px;
  }
}
</style>
