<template>
  <n-dropdown
    trigger="click"
    :options="dropdownOptions"
    :disabled="dropdownOptions.length === 0 || switchingOrg"
    @select="handleSelect"
  >
    <div class="org-switcher" :class="{ 'is-single': switchableOrgCount <= 1 }">
      <i class="i-material-symbols:account-tree-rounded org-icon" />
      <span class="org-label">当前组织</span>
      <span class="org-name">{{ currentOrgName }}</span>
      <i v-if="switchableOrgCount > 1" class="i-material-symbols:expand-more-rounded org-arrow" />
    </div>
  </n-dropdown>
</template>

<script setup>
import api from '@/api'
import { useAuthStore, useUserStore } from '@/store'

const router = useRouter()
const userStore = useUserStore()
const authStore = useAuthStore()

const orgOptions = ref([])
const switchingOrg = ref(false)

const currentOrgId = computed(() => userStore.activeOrgId || userStore.userInfo?.activeOrgId)
const flatOrgOptions = computed(() => flattenOrgOptions(orgOptions.value))
const displayOrgOptions = computed(() => {
  if (flatOrgOptions.value.length > 0) {
    return flatOrgOptions.value
  }
  if (!currentOrgId.value) {
    return []
  }
  return [{
    id: currentOrgId.value,
    orgName: userStore.activeOrgName || userStore.userInfo?.activeOrgName || '当前组织',
  }]
})
const switchableOrgCount = computed(() => displayOrgOptions.value.length)
const currentOrgName = computed(() => {
  const current = displayOrgOptions.value.find(item => Number(item.id) === Number(currentOrgId.value))
  return current?.orgName || userStore.activeOrgName || userStore.userInfo?.activeOrgName || '未选择组织'
})
const dropdownOptions = computed(() => displayOrgOptions.value.map(item => ({
  label: item.pathLabel || item.orgName || `组织 ${item.id}`,
  key: `org:${item.id}`,
  disabled: switchingOrg.value || Number(item.id) === Number(currentOrgId.value),
  icon: () => h('i', {
    class: Number(item.id) === Number(currentOrgId.value)
      ? 'i-material-symbols:check-circle-rounded text-14 text-success'
      : 'i-material-symbols:account-tree-rounded text-14',
  }),
})))

async function loadOrgOptions() {
  if (!userStore.userInfo)
    return
  try {
    const res = await api.getCurrentOrgOptions()
    if (res.code === 200) {
      orgOptions.value = res.data || []
    }
  }
  catch (error) {
    console.error('load org options error', error)
  }
}

function flattenOrgOptions(list = [], parentNames = []) {
  return (list || []).flatMap((item) => {
    const pathNames = [...parentNames, item.orgName || item.name || String(item.id)]
    const current = {
      ...item,
      pathLabel: pathNames.join(' / '),
    }
    const children = flattenOrgOptions(item.children || [], pathNames)
    return [current, ...children]
  })
}

function handleSelect(key) {
  if (!key?.startsWith('org:'))
    return
  handleOrgSwitch(Number(key.replace('org:', '')))
}

async function handleOrgSwitch(orgId) {
  if (!orgId || Number(orgId) === Number(currentOrgId.value))
    return
  try {
    switchingOrg.value = true
    const res = await api.switchOrg(orgId)
    if (res.code === 200) {
      authStore.resetLoginState({ resetAuth: false })
      window.$message?.success('当前组织已切换')
      await router.replace({ path: '/', query: { orgSwitch: Date.now() } })
    }
  }
  catch {
    window.$message?.error('切换组织失败')
  }
  finally {
    switchingOrg.value = false
  }
}

watch(() => [userStore.userInfo?.tenantId, userStore.userInfo?.activeOrgId], () => {
  loadOrgOptions()
}, { immediate: true })
</script>

<style scoped>
.org-switcher {
  height: 32px;
  max-width: 260px;
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
  margin-right: 8px;
}

.org-switcher:hover {
  border-color: var(--border-default);
  color: var(--text-primary);
}

.org-switcher.is-single {
  cursor: default;
}

.org-icon,
.org-arrow {
  flex-shrink: 0;
  font-size: 16px;
}

.org-label {
  flex-shrink: 0;
  color: var(--text-tertiary);
  font-size: 12px;
}

.org-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

@media (max-width: 900px) {
  .org-label {
    display: none;
  }
}
</style>
