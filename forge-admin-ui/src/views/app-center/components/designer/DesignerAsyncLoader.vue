<template>
  <div class="designer-async-loader" :class="{ 'is-overlay': overlay }" role="status" aria-live="polite">
    <div class="loader-copy">
      <strong>{{ title }}</strong>
      <span>{{ description }}</span>
    </div>
    <div class="loader-track">
      <span class="loader-segment segment-a" />
      <span class="loader-segment segment-b" />
      <span class="loader-segment segment-c" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '正在打开设计器',
  },
  description: {
    type: String,
    default: '首次加载需要准备组件资源',
  },
  overlay: {
    type: Boolean,
    default: false,
  },
})
</script>

<style scoped>
.designer-async-loader {
  display: grid;
  gap: 14px;
  width: min(460px, 100%);
  margin: 56px auto;
  padding: 18px 20px;
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.designer-async-loader.is-overlay {
  width: min(520px, calc(100% - 40px));
  margin: 0 auto;
}

.loader-copy {
  display: grid;
  gap: 4px;
}

.loader-copy strong {
  color: #18181b;
  font-size: 14px;
  font-weight: 700;
}

.loader-copy span {
  color: #71717a;
  font-size: 12px;
}

.loader-track {
  position: relative;
  height: 4px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef2ff;
}

.loader-segment {
  position: absolute;
  top: 0;
  left: -28%;
  height: 100%;
  border-radius: inherit;
  animation: loader-slide 1.4s cubic-bezier(0.65, 0, 0.35, 1) infinite;
}

.segment-a {
  width: 28%;
  background: #2563eb;
}

.segment-b {
  width: 18%;
  background: #14b8a6;
  animation-delay: 0.14s;
}

.segment-c {
  width: 12%;
  background: #f59e0b;
  animation-delay: 0.28s;
}

@keyframes loader-slide {
  0% {
    transform: translateX(0);
  }

  100% {
    transform: translateX(520%);
  }
}
</style>
