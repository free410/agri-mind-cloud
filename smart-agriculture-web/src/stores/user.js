import { defineStore } from 'pinia'
import { loginApi, getCurrentUserApi, logoutApi } from '@/api/auth'
import { clearAuth, saveAuth } from '@/utils/auth'
import { getToken, getUserInfo } from '@/utils/storage'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: getUserInfo() || null
  }),
  getters: {
    isLogin: (state) => Boolean(state.token)
  },
  actions: {
    async login(loginForm) {
      const data = await loginApi(loginForm)
      this.token = data.token
      this.userInfo = data.adminInfo || {
        id: data.adminId,
        username: data.username
      }
      saveAuth(this.token, this.userInfo)
      return data
    },
    async fetchCurrentUser() {
      const data = await getCurrentUserApi()
      this.userInfo = data
      saveAuth(this.token, this.userInfo)
      return data
    },
    async logout() {
      try {
        await logoutApi()
      } finally {
        this.resetAuth()
      }
    },
    resetAuth() {
      this.token = ''
      this.userInfo = null
      clearAuth()
    }
  }
})
