import { request } from '@/utils'

export default {
  getPublicKey: () => request({ url: '/crypto/public-key', method: 'get', needToken: false }),
  login: data => request({ url: '/auth/login', method: 'post', data, needToken: false }),
  logout: () => request({ url: '/auth/logout', method: 'post', needTip: false }),
  getUserInfo: () => request({ url: '/auth/userInfo', method: 'get' }),
  getTenantConfig: tenantId => request({
    url: '/system/tenant/userTenantConfig',
    method: 'post',
    data: { id: tenantId },
  }),
}
