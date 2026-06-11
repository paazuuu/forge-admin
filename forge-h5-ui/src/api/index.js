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
  updateUserProfile: data => request({
    url: '/system/user/updateProfile',
    method: 'post',
    data,
    encrypt: true,
  }),
  changePassword: data => request({
    url: '/auth/changePassword',
    method: 'post',
    params: {
      oldPassword: data?.oldPassword,
      newPassword: data?.newPassword,
    },
  }),
  getTodoTasks: (params = {}) => request({
    url: '/api/flow/task/todo',
    method: 'get',
    params,
    encrypt: true,
    needTip: false,
  }),
  getFlowTaskDetail: taskId => request({
    url: `/api/flow/task/${taskId}`,
    method: 'get',
    encrypt: true,
    needTip: false,
  }),
  getFlowTaskForm: taskId => request({
    url: `/api/flow/task/form/${taskId}`,
    method: 'get',
    encrypt: true,
    needTip: false,
  }),
  getFlowTaskHistory: processInstanceId => request({
    url: `/api/flow/task/history/${processInstanceId}`,
    method: 'get',
    encrypt: true,
    needTip: false,
  }),
  approveFlowTask: data => request({
    url: '/api/flow/task/approve',
    method: 'post',
    data,
    encrypt: true,
  }),
  rejectFlowTask: data => request({
    url: '/api/flow/task/reject',
    method: 'post',
    data,
    encrypt: true,
  }),
  getMessagePage: (params = {}) => {
    const {
      pageNum = 1,
      pageSize = 10,
      ...query
    } = params || {}
    return request({
      url: '/api/message/page',
      method: 'post',
      params: { pageNum, pageSize },
      data: query,
      encrypt: true,
      needTip: false,
    })
  },
  getMessageDetail: id => request({
    url: `/api/message/${id}`,
    method: 'get',
    encrypt: true,
    needTip: false,
  }),
  getUnreadMessageCount: () => request({
    url: '/api/message/unread/count',
    method: 'get',
    encrypt: true,
    needTip: false,
  }),
  markMessageRead: id => request({
    url: `/api/message/${id}/read`,
    method: 'post',
    data: {},
    encrypt: true,
    needTip: false,
  }),
  markAllMessagesRead: () => request({
    url: '/api/message/read/all',
    method: 'post',
    data: {},
    encrypt: true,
    needTip: false,
  }),
  getEnabledMessageBizTypes: () => request({
    url: '/api/message/bizType/list/enabled',
    method: 'get',
    encrypt: true,
    needTip: false,
  }),
  getTenantConfig: tenantId => request({
    url: '/system/tenant/userTenantConfig',
    method: 'post',
    data: { id: tenantId },
  }),
}
