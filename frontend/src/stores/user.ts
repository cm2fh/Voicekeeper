import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginUserVO } from '@/types/user'
import * as userApi from '@/api/user'

export const useUserStore = defineStore('user', () => {
  // State
  const userInfo = ref<LoginUserVO | null>(null)
  const isLoading = ref(false)

  // Getters
  const isLogin = computed(() => !!userInfo.value)
  const userId = computed(() => userInfo.value?.id)
  const username = computed(() => userInfo.value?.username)
  const isAdmin = computed(() => userInfo.value?.role === 'admin')

  // Actions
  async function fetchUserInfo() {
    try {
      isLoading.value = true
      const res = await userApi.getLoginUser()
      userInfo.value = res.data
      return res.data
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function login(username: string, password: string) {
    try {
      isLoading.value = true
      const res = await userApi.login({ username, password })
      userInfo.value = res.data
      return res.data
    } finally {
      isLoading.value = false
    }
  }

  async function logout() {
    try {
      await userApi.logout()
    } catch (error) {
      console.error('退出登录失败:', error)
    } finally {
      userInfo.value = null
    }
  }

  function clearUser() {
    userInfo.value = null
  }

  return {
    // State
    userInfo,
    isLoading,
    // Getters
    isLogin,
    userId,
    username,
    isAdmin,
    // Actions
    fetchUserInfo,
    login,
    logout,
    clearUser
  }
}, {
  persist: true  // 持久化到 localStorage
})

