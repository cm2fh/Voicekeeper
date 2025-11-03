import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/Home.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/card-library',
      name: 'cardLibrary',
      component: () => import('@/views/CardLibrary.vue'),
      meta: { requiresAuth: true }
    },
    // 以下页面已废弃，功能集成到AI对话中
    // {
    //   path: '/voice-clone',
    //   name: 'voiceClone',
    //   component: () => import('@/views/VoiceClone.vue'),
    //   meta: { requiresAuth: true }
    // },
    // {
    //   path: '/create-card',
    //   name: 'createCard',
    //   component: () => import('@/views/CreateCard.vue'),
    //   meta: { requiresAuth: true }
    // },
    {
      path: '/card/:id',
      name: 'cardDetail',
      component: () => import('@/views/CardDetail.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 如果路由需要认证
  if (to.meta.requiresAuth) {
    // 检查是否已登录
    if (userStore.isLogin) {
      next()
    } else {
      // 尝试获取用户信息
      const user = await userStore.fetchUserInfo()
      if (user) {
        next()
      } else {
        // 未登录，跳转到登录页
        next({ name: 'login', query: { redirect: to.fullPath } })
      }
    }
  } else {
    // 如果已登录访问登录页，跳转到首页
    if (to.name === 'login' && userStore.isLogin) {
      next({ name: 'home' })
    } else {
      next()
    }
  }
})

export default router
