<template>
  <n-dropdown :options="dropdownOptions" @select="handleSelect">
    <div id="user-dropdown" class="flex cursor-pointer items-center">
      <n-avatar
        v-if="avatarSrc"
        round
        :size="28"
        :src="avatarSrc"
        @error="handleAvatarError"
      />
      <n-avatar
        v-else
        round
        :size="28"
        :style="{ backgroundColor: 'var(--primary-500)', fontSize: '12px' }"
      >
        {{ avatarText }}
      </n-avatar>
      <div v-if="userStore.userInfo || userStore.staffInfo" class="ml-8 flex-col flex-shrink-0 items-center">
        <span class="text-14">{{ userStore.realName || userStore.staffInfo?.staffName }}</span>
      </div>
    </div>
  </n-dropdown>
</template>

<script setup>
import api from '@/api'
import { defaultThemeConfig } from '@/config/theme.config.js'
import { useAuthStore, useUserStore } from '@/store'
import { resolveRenderableFileUrl } from '@/utils/file'

const router = useRouter()
const userStore = useUserStore()
const authStore = useAuthStore()

const avatarSrc = ref('')
const tenantOptions = ref([])
const switchingTenant = ref(false)
const avatarText = computed(() => {
  const name = userStore.realName || userStore.username
  return name ? name.charAt(0) : 'U'
})

const dropdownOptions = computed(() => {
  const baseOptions = []
  if (tenantOptions.value.length > 1) {
    baseOptions.push({
      label: '切换租户',
      key: 'tenant-switch',
      icon: () => h('i', { class: 'i-material-symbols:domain-rounded text-14' }),
      children: tenantOptions.value.map(item => ({
        label: item.tenantName || `租户 ${item.tenantId}`,
        key: `tenant:${item.tenantId}`,
        disabled: switchingTenant.value || item.tenantId === userStore.userInfo?.tenantId,
        icon: () => h('i', {
          class: item.tenantId === userStore.userInfo?.tenantId
            ? 'i-material-symbols:check-circle-rounded text-14 text-success'
            : 'i-material-symbols:corporate-fare-rounded text-14',
        }),
      })),
    })
  }
  baseOptions.push(
    {
      label: '个人资料',
      key: 'profile',
      icon: () => h('i', { class: 'i-material-symbols:person-outline text-14' }),
    },
    {
      label: '退出登录',
      key: 'logout',
      icon: () => h('i', { class: 'i-mdi:exit-to-app text-14' }),
    },
  )
  return baseOptions
})

async function loadAvatar() {
  const avatar = userStore.avatar
  if (!avatar) {
    avatarSrc.value = ''
    return
  }
  try {
    avatarSrc.value = await resolveRenderableFileUrl(avatar)
  }
  catch {
    avatarSrc.value = ''
  }
}

function handleAvatarError() {
  avatarSrc.value = ''
}

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
  if (key?.startsWith('tenant:')) {
    handleTenantSwitch(Number(key.replace('tenant:', '')))
    return
  }
  switch (key) {
    case 'profile':
      router.push('/profile')
      break
    case 'logout':
      $dialog.confirm({
        'title': '提示',
        'type': 'info',
        'content': '确认退出？',
        'positive-button-props': {
          color: defaultThemeConfig.primaryColor,
        },
        async confirm() {
          try {
            await api.logout()
          }
          catch {
            console.error('logout error')
          }
          authStore.logout()
          $message.success('已退出登录')
        },
      })
      break
  }
}

async function handleTenantSwitch(tenantId) {
  if (!tenantId || tenantId === userStore.userInfo?.tenantId)
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

watch(() => userStore.avatar, () => {
  loadAvatar()
}, { immediate: true })

watch(() => userStore.userInfo?.tenantId, () => {
  loadTenantOptions()
}, { immediate: true })
</script>
