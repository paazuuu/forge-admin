import { defineStore } from 'pinia'
import api from '@/api'
import { rsaEncrypt } from '@/utils/crypto/rsa'

function getToken(data = {}) {
  return data.accessToken || data.token
}

function getDisplayName(userInfo) {
  return userInfo?.realName || userInfo?.nickName || userInfo?.username || '用户'
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: '',
    tokenType: 'Bearer',
    expiresIn: null,
    userInfo: null,
  }),
  getters: {
    isLogin: state => !!state.accessToken,
    displayName: state => getDisplayName(state.userInfo),
  },
  actions: {
    setToken(data = {}) {
      const token = getToken(data)
      if (!token) {
        return
      }
      this.accessToken = token
      this.tokenType = data.tokenType || 'Bearer'
      this.expiresIn = data.expiresIn || null
    },
    setUserInfo(userInfo) {
      this.userInfo = userInfo || null
    },
    resetAuth() {
      this.accessToken = ''
      this.tokenType = 'Bearer'
      this.expiresIn = null
      this.userInfo = null
    },
    async encryptPassword(password) {
      try {
        const res = await api.getPublicKey()
        const publicKey = res?.data?.publicKey
        return publicKey ? rsaEncrypt(password, publicKey) : password
      }
      catch (error) {
        console.warn('密码 RSA 加密失败，使用明文降级:', error)
        return password
      }
    },
    async login(form) {
      const password = await this.encryptPassword(form.password)
      const payload = {
        username: form.username,
        password,
        tenantId: form.tenantId || undefined,
        authType: 'password',
        userClient: import.meta.env.VITE_USER_CLIENT || 'h5',
        appId: import.meta.env.VITE_APP_ID || undefined,
        appSecret: import.meta.env.VITE_APP_SECRET || undefined,
      }
      const res = await api.login(payload)
      this.setToken(res.data || {})
      await this.fetchUserInfo()
      return res
    },
    async fetchUserInfo() {
      if (!this.accessToken) {
        return null
      }
      const res = await api.getUserInfo()
      this.setUserInfo(res.data || null)
      return this.userInfo
    },
    async logout() {
      try {
        if (this.accessToken) {
          await api.logout()
        }
      }
      finally {
        this.resetAuth()
      }
    },
  },
  persist: {
    key: `${import.meta.env.VITE_TENANT || 'default'}_auth`,
    pick: ['accessToken', 'tokenType', 'expiresIn', 'userInfo'],
  },
})
