import { defaultThemeConfig } from '@/config/theme.config'
import { useTenantStore } from '@/store'
import { resolveRenderableFileUrl } from '@/utils/file'
import { normalizePageTitle, setDocumentTitle } from '@/utils/page-title'

export async function applyTenantConfig(tenantConfig, appStore) {
  const tenantStore = useTenantStore()

  appStore.resetAccountState()
  if (!tenantConfig)
    return

  if (tenantConfig.systemLayout) {
    appStore.setLayout(tenantConfig.systemLayout)
  }

  const themeConfigObj = tenantStore.themeConfig
  if (themeConfigObj) {
    const primaryColor = tenantConfig.systemTheme || themeConfigObj.primaryColor || defaultThemeConfig.primaryColor
    appStore.setThemeConfig({
      primaryColor,
      header: {
        ...defaultThemeConfig.header,
        ...themeConfigObj.header,
      },
      headerDark: {
        ...defaultThemeConfig.headerDark,
        ...themeConfigObj.headerDark,
      },
      topMenu: {
        ...defaultThemeConfig.topMenu,
        ...themeConfigObj.topMenu,
      },
      topMenuDark: {
        ...defaultThemeConfig.topMenuDark,
        ...themeConfigObj.topMenuDark,
      },
      sideMenu: {
        ...defaultThemeConfig.sideMenu,
        ...themeConfigObj.sideMenu,
      },
      sideMenuDark: {
        ...defaultThemeConfig.sideMenuDark,
        ...themeConfigObj.sideMenuDark,
      },
    })
  }
  else if (tenantConfig.systemTheme) {
    appStore.setPrimaryColor(tenantConfig.systemTheme)
    appStore.setThemeColor(tenantConfig.systemTheme)
  }

  const pageBaseTitle = normalizePageTitle(tenantConfig.browserTitle) || normalizePageTitle(tenantConfig.systemName)
  if (pageBaseTitle) {
    setDocumentTitle(pageBaseTitle)
  }

  if (tenantConfig.browserIcon) {
    const link = document.querySelector('link[rel*=\'icon\']') || document.createElement('link')
    link.type = 'image/x-icon'
    link.rel = 'shortcut icon'
    try {
      link.href = await resolveRenderableFileUrl(tenantConfig.browserIcon) || tenantConfig.browserIcon
    }
    catch {
      link.href = tenantConfig.browserIcon
    }
    document.getElementsByTagName('head')[0].appendChild(link)
  }
}
