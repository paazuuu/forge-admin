<template>
  <view class="ai-tabs">
    <view class="ai-tabs-header">
      <view 
        v-for="(tab, index) in tabs" 
        :key="index"
        class="ai-tabs-tab"
        :class="{ 'ai-tabs-tab--active': activeIndex === index }"
        @click="handleTabClick(index)"
      >
        <text class="ai-tabs-tab-text">{{tab.label || tab}}</text>
      </view>
    </view>
    <view class="ai-tabs-content">
      <slot></slot>
    </view>
  </view>
</template>

<script setup>
import { ref, provide } from 'vue'

const props = defineProps({
  tabs: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const activeIndex = ref(props.modelValue)

const handleTabClick = (index) => {
  activeIndex.value = index
  emit('update:modelValue', index)
  emit('change', index)
}

provide('activeIndex', activeIndex)
</script>

<style lang="scss" scoped>
.ai-tabs {
  &-header {
    display: flex;
    background: #fff;
    border-bottom: 1px solid #eee;
  }
  
  &-tab {
    flex: 1;
    text-align: center;
    padding: 12px 0;
    position: relative;
    
    &--active {
      color: var(--primary-color, #d12723FF);
      
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 40rpx;
        height: 4rpx;
        background: var(--primary-color, #d12723FF);
      }
    }
  }
  
  &-tab-text {
    font-size: 14px;
    color: #666;
    
    .ai-tabs-tab--active & {
      color: var(--primary-color, #d12723FF);
      font-weight: 500;
    }
  }
}
</style>
