import { request } from '@/utils'

export default {
  getPublicKey: () => request({ url: '/crypto/public-key', method: 'get', needToken: false }),
  getLoginConfig: () => request({ url: '/auth/loginConfig', method: 'get', needToken: false }),
  getCaptcha: () => request({ url: '/auth/captcha', method: 'get', needToken: false }),
  login: data => request({ url: '/auth/login', method: 'post', data, needToken: false, needTip: false }),
  logout: () => request({ url: '/auth/logout', method: 'post', needTip: false }),
  getUserInfo: () => request({ url: '/auth/userInfo', method: 'get' }),
  getCurrentMenu: () => request({ url: '/auth/current/menu', method: 'get' }),
  getCurrentPermissions: () => request({ url: '/auth/current/permissions', method: 'get' }),
  getTenantConfig: tenantId => request({
    url: '/system/tenant/userTenantConfig',
    method: 'post',
    data: { id: tenantId },
  }),
}
