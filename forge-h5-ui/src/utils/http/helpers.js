import { showConfirmDialog } from '@/utils/dialog'
import { toast } from '@/utils/notify'

let isConfirming = false
const LOGIN_PAGE = '/pages/login/index'

function redirectToLogin() {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  const redirect = current?.route ? `/${current.route}` : ''
  const query = redirect ? `?redirect=${encodeURIComponent(redirect)}` : ''
  uni.reLaunch({ url: `${LOGIN_PAGE}${query}` })
}

export function resolveResError(code, message, needTip = true) {
  switch (code) {
    case '-8': // 令牌无效
    case 401:
      if (isConfirming || !needTip) return
      isConfirming = true
      showConfirmDialog({
        title: '提示',
        description: message || '登录已过期，是否重新登录？',
        icon: 'warning',
        confirmText: '重新登录',
        cancelText: '取消',
      })
        .then((confirmed) => {
          isConfirming = false
          if (confirmed) {
            redirectToLogin()
          }
        })
        .catch(() => {
          isConfirming = false
        })
      return false
    case 11007:
    case 11008:
      if (isConfirming || !needTip) return
      isConfirming = true
      showConfirmDialog({
        title: '提示',
        description: `${message}，是否重新登录？`,
        icon: 'warning',
        confirmText: '重新登录',
        cancelText: '取消',
      })
        .then((confirmed) => {
          isConfirming = false
          if (confirmed) {
            redirectToLogin()
          }
        })
        .catch(() => {
          isConfirming = false
        })
      return false
    case 403:
      message = message || '请求被拒绝'
      break
    case 404:
      message = message || '请求资源或接口不存在'
      break
    case 500:
      message = message || '服务器发生异常'
      break
    default:
      message = message ?? `【${code}】: 未知异常!`
      break
  }
  if (needTip) {
    toast(message, { type: code === 403 ? 'warning' : 'error', duration: 2200 })
  }
  return message
}
