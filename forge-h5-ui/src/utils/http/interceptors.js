import { resolveResError } from './helpers'
import { useAuthStore } from '@/store'

export function setupInterceptors(axiosInstance) {
  const SUCCESS_CODES = [0, 200]
  const AUTH_EXPIRED_CODES = ['-8', -8, 401, 11007, 11008]

  function resetAuthIfExpired(code) {
    if (AUTH_EXPIRED_CODES.includes(code)) {
      useAuthStore().resetAuth()
    }
  }

  /**
   * 响应成功拦截器
   */
  function resResolve(response) {
    const { data, status, config, statusText, headers } = response

    // 处理JSON响应
    if (headers['content-type']?.includes('json')) {
      // 检查新的响应格式 (respCode)
      if (data && data.respCode === '0000') {
        return Promise.resolve(data)
      }

      // 兼容旧的响应格式
      if (SUCCESS_CODES.includes(data?.code) || SUCCESS_CODES.includes(data?.respCode)) {
        return Promise.resolve(data)
      }

      // 业务错误响应
      const code = data?.code ?? data?.respCode ?? status
      const message = data?.message ?? data?.msg ?? data?.respDesc ?? statusText
      const needTip = config?.needTip !== false

      return Promise.reject({
        code,
        message,
        error: data ?? response,
        isBusinessError: true,
        needTip,
      })
    }

    // 非JSON响应直接返回
    return Promise.resolve(data ?? response)
  }

  /**
   * 响应失败拦截器 - 统一错误处理入口
   */
  async function resReject(error) {
    // 1. 处理已经在 resResolve 中标记了 skipErrorHandler 的错误
    if (error?.skipErrorHandler) {
      return Promise.reject(error)
    }

    // 2. 处理网络错误（没有 response）
    if (!error || !error.response) {
      // 如果是业务错误（从 resResolve 传来的）
      if (error?.isBusinessError) {
        const { code, message, needTip = true } = error
        resetAuthIfExpired(code)
        const finalMessage = resolveResError(code, message, needTip)
        return Promise.reject({ code, message: finalMessage, error: error.error })
      }

      // 网络错误或其他错误
      const code = error?.code || 'NETWORK_ERROR'
      const message = error?.message || '网络连接失败，请检查您的网络'
      resolveResError(code, message, true)
      return Promise.reject({ code, message, error })
    }

    // 3. 处理HTTP错误响应
    const { data, status, config } = error.response
    const code = data?.code ?? data?.respCode ?? status
    const message = data?.message ?? data?.msg ?? data?.respDesc ?? error.message
    const needTip = config?.needTip !== false
    resetAuthIfExpired(code)
    const finalMessage = resolveResError(code, message, needTip)
    return Promise.reject({
      code,
      message: finalMessage,
      error: error.response?.data || error.response,
    })
  }

  axiosInstance.interceptors.request.use(reqResolve, reqReject)
  axiosInstance.interceptors.response.use(resResolve, resReject)
}

/**
 * 请求拦截器
 */
function reqResolve(config) {
  const authStore = useAuthStore()

  // 设置默认headers
  config.headers = config.headers || {}

  // 生成traceid: 时间戳+5位随机数
  const timestamp = Date.now()
  const random = Math.floor(10000 + Math.random() * 90000)
  config.headers.traceId = `${timestamp}${random}`

  if (config.needToken !== false && authStore.accessToken) {
    config.headers.Authorization = `${authStore.tokenType || 'Bearer'} ${authStore.accessToken}`
  }

  return config
}

function reqReject(error) {
  return Promise.reject(error)
}
