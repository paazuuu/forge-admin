export const cryptoConfig = {
  enabled: true,
  algorithm: 'SM4',
  secretKey: '',
  enableDynamicKey: true,
  excludePaths: ['/auth/captcha', '/auth/login', '/auth/loginConfig', '/crypto/public-key', '/crypto/exchange'],
}

export function matchPath(path, pattern) {
  if (!pattern) {
    return false
  }

  const regexPattern = pattern
    .replace(/\*\*/g, '.*')
    .replace(/\*/g, '[^/]*')
  return new RegExp(`^${regexPattern}$`).test(path)
}

export function shouldEncrypt(url) {
  if (!cryptoConfig.enabled) {
    return false
  }

  const path = (url || '').split('?')[0]
  return !cryptoConfig.excludePaths.some(pattern => matchPath(path, pattern))
}

export function updateCryptoConfig(config) {
  Object.assign(cryptoConfig, config)
}
