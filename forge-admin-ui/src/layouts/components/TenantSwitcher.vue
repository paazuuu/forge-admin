<template>
  <n-dropdown
    trigger="click"
    :options="dropdownOptions"
    :disabled="dropdownOptions.length === 0"
    @select="handleSelect"
  >
    <div class="tenant-switcher" :class="{ 'is-single': switchableTenantCount <= 1 }">
      <i class="i-material-symbols:domain-rounded tenant-icon" />
      <span class="tenant-name">{{ currentTenantName }}</span>
      <i v-if="switchableTenantCount > 1" class="i-material-symbols:expand-more-rounded tenant-arrow" />
    </div>
  </n-dropdown>
</template>

<script setup>
import api from '@/api'
import { useAuthStore, useUserStore } from '@/store'

const router = useRouter()
const userStore = useUserStore()
const authStore = useAuthStore()

const tenantOptions = ref([])
const switchingTenant = ref(false)

const currentTenantId = computed(() => userStore.userInfo?.tenantId)
const currentTenantName = computed(() => {
  const current = tenantOptions.value.find(item => item.tenantId === currentTenantId.value)
  return current?.tenantName || userStore.userInfo?.tenantName || '当前租户'
})
const displayTenantOptions = computed(() => {
  if (tenantOptions.value.length > 0) {
    return tenantOptions.value
  }
  if (!currentTenantId.value) {
    return []
  }
  return [{
    tenantId: currentTenantId.value,
    tenantName: userStore.userInfo?.tenantName || '当前租户',
  }]
})
const switchableTenantCount = computed(() => displayTenantOptions.value.length)
const dropdownOptions = computed(() => displayTenantOptions.value.map(item => ({
  label: item.tenantName || `租户 ${item.tenantId}`,
  key: `tenant:${item.tenantId}`,
  disabled: switchingTenant.value || item.tenantId === currentTenantId.value,
  icon: () => h('i', {
    class: item.tenantId === currentTenantId.value
      ? 'i-material-symbols:check-circle-rounded text-14 text-success'
      : 'i-material-symbols:corporate-fare-rounded text-14',
  }),
})))

async function loadTenantOptions() {
  if (!userStore.userInfo)
    return
  try {
    const res = await api.getCurrentTenantOptions()
    if (res.code === 200) {
      tenantOptions.value = res.data || []
    }
  }
  catch (error) {
    console.error('load tenant options error', error)
  }
}

function handleSelect(key) {
  if (!key?.startsWith('tenant:'))
    return
  handleTenantSwitch(Number(key.replace('tenant:', '')))
}

async function handleTenantSwitch(tenantId) {
  if (!tenantId || tenantId === currentTenantId.value)
    return
  try {
    switchingTenant.value = true
    const res = await api.switchTenant(tenantId)
    if (res.code === 200) {
      authStore.resetLoginState({ resetAuth: false })
      $message.success('租户已切换')
      await router.replace({ path: '/', query: { tenantSwitch: Date.now() } })
    }
  }
  catch {
    $message.error('切换租户失败')
  }
  finally {
    switchingTenant.value = false
  }
}

watch(() => userStore.userInfo?.tenantId, () => {
  loadTenantOptions()
}, { immediate: true })
</script>

<style scoped>
.tenant-switcher {
  height: 32px;
  max-width: 220px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-base);
}

.tenant-switcher:hover {
  border-color: var(--border-default);
  color: var(--text-primary);
}

.tenant-switcher.is-single {
  cursor: default;
}

.tenant-icon,
.tenant-arrow {
  flex-shrink: 0;
  font-size: 16px;
}

.tenant-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
</style>
