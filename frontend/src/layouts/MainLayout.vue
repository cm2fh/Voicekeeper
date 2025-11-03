<template>
  <div class="main-layout">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-content">
        <!-- Logo -->
        <div class="logo">
          <h1 class="logo-text">VoiceKeeper</h1>
        </div>

        <!-- 导航菜单 -->
        <nav class="nav-menu">
          <router-link to="/" class="nav-item" active-class="active">
            <el-icon><HomeFilled /></el-icon>
            <span>AI助手</span>
          </router-link>
          <router-link to="/card-library" class="nav-item" active-class="active">
            <el-icon><Collection /></el-icon>
            <span>我的卡片</span>
          </router-link>
        </nav>

        <!-- 用户信息 -->
        <div class="user-section">
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="36" :src="userStore.userInfo?.avatar">
                {{ userStore.username?.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="username">{{ userStore.username }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-icon><User /></el-icon>
                  <span>{{ userStore.username }}</span>
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- 页面内容插槽 -->
    <main class="layout-content">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import {
  HomeFilled,
  Collection,
  ArrowDown,
  User,
  SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

// 退出登录
const handleLogout = async () => {
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 var(--spacing-xl);
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
}

.logo-text {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  background: linear-gradient(135deg, var(--color-primary), #ff6b9d);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0;
  cursor: pointer;
}

/* 导航菜单 */
.nav-menu {
  display: flex;
  gap: var(--spacing-sm);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-lg);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  transition: all var(--transition-base);
}

.nav-item:hover {
  background: var(--color-bg);
  color: var(--color-primary);
}

.nav-item.active {
  background: linear-gradient(135deg, rgba(255, 154, 98, 0.1), rgba(255, 107, 154, 0.1));
  color: var(--color-primary);
}

/* 用户信息 */
.user-section {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-base);
}

.user-info:hover {
  background: var(--color-bg);
}

.username {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.arrow {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

/* 内容区域 */
.layout-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>

