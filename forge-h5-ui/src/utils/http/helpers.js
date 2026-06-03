let isConfirming = false
const LOGIN_PAGE = '/pages/login/index'

function redirectToLogin() {
  import('@/store').then(({ useAuthStore }) => {
    useAuthStore().resetAuth()
  })
  uni.reLaunch({ url: LOGIN_PAGE })
}

export function resolveResError(code, message, needTip = true) {
  switch (code) {
    case '-8': // 令牌无效
    case 401:
      if (isConfirming || !needTip) return
      isConfirming = true
      uni.showModal({
        title: '提示',
        content: message || '登录已过期，是否重新登录？',
        confirmText: '重新登录',
        cancelText: '取消',
        success: (res) => {
          isConfirming = false
          if (res.confirm) {
            redirectToLogin()
          }
        },
        fail: () => {
          isConfirming = false
        }
      })
      return false
    case 11007:
    case 11008:
      if (isConfirming || !needTip) return
      isConfirming = true
      uni.showModal({
        title: '提示',
        content: `${message}，是否重新登录？`,
        confirmText: '重新登录',
        cancelText: '取消',
        success: (res) => {
          isConfirming = false
          if (res.confirm) {
            redirectToLogin()
          }
        },
        fail: () => {
          isConfirming = false
        }
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
    uni.showToast({
      title: message,
      icon: 'none',
      duration: 2000
    })
  }
  return message
}
