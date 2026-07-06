<template>
  <router-link to="/" class="the-title text-14 mr-10 cursor-pointer transition-opacity hover:opacity-80">
    {{ systemName }}
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import { useTenantStore } from '@/store'
import { getDefaultPageTitle } from '@/utils/page-title'

const tenantStore = useTenantStore()

// 优先使用租户配置的系统名称，否则使用默认值
const systemName = computed(() => {
  return tenantStore.systemName || getDefaultPageTitle()
})
</script>

<style scoped>
.the-title {
  color: var(--brand-title-text-color, var(--layout-header-text-color, #ffffff));
}
</style>
