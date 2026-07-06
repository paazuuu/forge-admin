<template>
  <TopMenuBar
    class="top-menu"
    :items="menuOptions"
    :active-key="activeKey"
    dropdown
    @select="handleMenuSelect"
  />
</template>

<script setup>
import { computed, h } from 'vue'
import { useRoute } from 'vue-router'
import IconRenderer from '@/components/IconRenderer.vue'
import { useMenu } from '@/composables'
import TopMenuBar from '@/layouts/components/TopMenuBar.vue'
import { usePermissionStore } from '@/store'
import { findMenuIdByPath, processTopMenus } from '@/utils/menu-utils'

const route = useRoute()
const permissionStore = usePermissionStore()

const { handleMenuSelect: baseHandleMenuSelect } = useMenu()

function renderMenuIcon(icon) {
  if (!icon)
    return undefined
  if (typeof icon === 'function')
    return icon
  if (typeof icon === 'string' && icon.trim() && icon !== '-1') {
    return () => h(IconRenderer, { icon, fontSize: 16 })
  }
  return undefined
}

// Process menu data for top menu with dropdown
const menuOptions = computed(() => {
  const menus = permissionStore.menus || []
  const topMenus = processTopMenus(menus)

  return topMenus.map((item) => {
    const children = item.children ? processDropdownMenuData(item.children) : []
    return {
      ...item,
      key: item.id,
      label: item.label || item.name,
      children: children.length ? children : null,
    }
  })
})

function processDropdownMenuData(menuItems, parentLabels = []) {
  if (!menuItems || !Array.isArray(menuItems)) {
    return []
  }

  return menuItems.flatMap((item) => {
    const label = item.name || item.label || ''
    const children = item.children?.length
      ? processDropdownMenuData(item.children, [...parentLabels, label])
      : []

    if (item.type === 'module') {
      return children
    }

    const menuItem = {
      key: item.key || String(item.id),
      label: parentLabels.length ? `${parentLabels.join(' / ')} / ${label}` : label,
      icon: renderMenuIcon(item.icon),
    }

    if (item.path) {
      menuItem.path = item.path
    }

    return item.path ? [menuItem] : children
  })
}

// Active menu key based on route
const activeKey = computed(() => {
  return findMenuIdByPath(menuOptions.value, route.path) || route.path
})

// Wrapper for menu select to integrate with base composable
function handleMenuSelect(item) {
  baseHandleMenuSelect(item.key, item.path)
}
</script>
