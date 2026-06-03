<template>
  <view class="ai-loading" v-if="visible">
    <view class="loading-spinner"></view>
    <text class="loading-text">{{text}}</text>
  </view>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  text: {
    type: String,
    default: '加载中...'
  },
  visible: {
    type: Boolean,
    default: true
  }
})
</script>

<style lang="scss" scoped>
.ai-loading {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.7);
  padding: 20px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 9999;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-text {
  color: #fff;
  margin-top: 10px;
  font-size: 14px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
