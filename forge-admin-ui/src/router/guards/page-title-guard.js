import { usePermissionStore, useTabStore } from '@/store'

const baseTitle = import.meta.env.VITE_TITLE

function findTitleFromAllMenus(allMenus, targetPath) {
  if (!Array.isArray(allMenus))
    return null
  const found = allMenus.find(menu => menu.path === targetPath)
  return found?.label || found?.name || found?.meta?.title || null
}

export function createPageTitleGuard(router) {
  router.afterEach((to) => {
    const permissionStore = usePermissionStore()
    const tabStore = useTabStore()
    let pageTitle = to.meta?.title
    if (to.path?.startsWith('/ai/crud-page/')) {
      const dynamicTitle = tabStore.tabs.find(tab => tab.path === to.fullPath || tab.key === to.fullPath)?.title
      if (dynamicTitle && dynamicTitle !== 'CRUD页面')
        pageTitle = dynamicTitle
      else if (!pageTitle || pageTitle === 'CRUD页面')
        pageTitle = findTitleFromAllMenus(permissionStore.allMenus, to.path) || pageTitle
    }
    if (pageTitle) {
      document.title = `${pageTitle} | ${baseTitle}`
    }
    else {
      document.title = baseTitle
    }
  })
}
