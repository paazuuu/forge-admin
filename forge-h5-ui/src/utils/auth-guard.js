import { useAuthStore } from '@/store'

export const LOGIN_PAGE = '/pages/login/index'
export const HOME_PAGE = '/pages/index/index'

function buildLoginUrl(redirect) {
  if (!redirect) {
    return LOGIN_PAGE
  }
  return `${LOGIN_PAGE}?redirect=${encodeURIComponent(redirect)}`
}

export function getCurrentRoutePath() {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  if (!current?.route) {
    return HOME_PAGE
  }
  return `/${current.route}`
}

export function toLogin(redirect = getCurrentRoutePath()) {
  const url = buildLoginUrl(redirect)
  uni.reLaunch({ url })
}

export async function ensureLogin(options = {}) {
  const { redirect = getCurrentRoutePath(), loadUser = true } = options
  const authStore = useAuthStore()
  if (!authStore.isLogin) {
    toLogin(redirect)
    return false
  }

  if (loadUser && !authStore.userInfo) {
    try {
      await authStore.fetchUserInfo()
    }
    catch (error) {
      authStore.resetAuth()
      toLogin(redirect)
      return false
    }
  }

  return true
}
