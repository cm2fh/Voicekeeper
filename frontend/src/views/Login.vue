<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="background-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card glass">
      <!-- Logo 和标题 -->
      <div class="header">
        <h1 class="title">VoiceKeeper</h1>
        <p class="subtitle">留住声音，留住温暖</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 底部提示 -->
      <div class="footer">
        <span class="hint">还没有账号？</span>
        <el-button type="text" @click="showRegister = true">立即注册</el-button>
      </div>

      <!-- 快速登录提示 -->
      <div class="quick-login-hint">
        <el-divider>测试账号</el-divider>
        <p class="test-account">
          用户名: <span class="highlight">testuser</span> 
          密码: <span class="highlight">testuser</span>
        </p>
      </div>
    </div>

    <!-- 注册对话框 -->
    <el-dialog
      v-model="showRegister"
      title="注册账号"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="checkPassword">
          <el-input
            v-model="registerForm.checkPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
            clearable
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">
          注册
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { register as registerApi } from '@/api/user'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 登录表单
const loginFormRef = ref<FormInstance>()
const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const loading = ref(false)

// 登录处理
const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true
        await userStore.login(loginForm.username, loginForm.password)
        
        ElMessage.success('登录成功！')
        
        // 跳转到原来要访问的页面或首页
        const redirect = (route.query.redirect as string) || '/'
        router.push(redirect)
      } catch (error: any) {
        ElMessage.error(error.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 注册相关
const showRegister = ref(false)
const registerFormRef = ref<FormInstance>()
const registerForm = reactive({
  username: '',
  password: '',
  checkPassword: ''
})

const validateCheckPassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  checkPassword: [
    { required: true, validator: validateCheckPassword, trigger: 'blur' }
  ]
}

const registerLoading = ref(false)

// 注册处理
const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        registerLoading.value = true
        await registerApi(registerForm)
        
        ElMessage.success('注册成功！请登录')
        showRegister.value = false
        
        // 自动填充用户名
        loginForm.username = registerForm.username
        loginForm.password = ''
        
        // 清空注册表单
        registerForm.username = ''
        registerForm.password = ''
        registerForm.checkPassword = ''
      } catch (error: any) {
        ElMessage.error(error.message || '注册失败')
      } finally {
        registerLoading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  /* 温暖渐变背景：从浅橙到深橙+粉 */
  background: linear-gradient(135deg, #FFE5D9 0%, #FFB4A2 40%, #FF9A62 100%);
}

/* 背景装饰 */
.background-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  animation: float 20s infinite ease-in-out;
}

.circle-1 {
  width: 400px;
  height: 400px;
  top: -200px;
  left: -200px;
  background: rgba(255, 180, 162, 0.2);
  animation-delay: 0s;
}

.circle-2 {
  width: 300px;
  height: 300px;
  bottom: -150px;
  right: -150px;
  background: rgba(255, 215, 139, 0.15);
  animation-delay: 5s;
}

.circle-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  right: 5%;
  background: rgba(255, 255, 255, 0.2);
  animation-delay: 10s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
    opacity: 0.6;
  }
  50% {
    transform: translate(30px, 30px) scale(1.15);
    opacity: 0.8;
  }
}

/* 登录卡片 */
.login-card {
  position: relative;
  width: 420px;
  padding: var(--spacing-2xl) var(--spacing-xl);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(255, 154, 98, 0.15), 
              0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.6);
  z-index: 1;
}

/* 头部 */
.header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
}

.title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  background: linear-gradient(135deg, #FF6B35 0%, #FF9A62 50%, #FFB488 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: var(--spacing-sm);
  text-shadow: 0 2px 10px rgba(255, 154, 98, 0.2);
  letter-spacing: 1px;
}

.subtitle {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin: 0;
}

/* 登录表单 */
.login-form {
  margin-top: var(--spacing-xl);
}

.login-btn {
  width: 100%;
  background: linear-gradient(135deg, #FF9A62 0%, #FF7A45 100%);
  border: none;
  height: 44px;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  border-radius: var(--radius-md);
  transition: all var(--transition-base);
  box-shadow: 0 4px 15px rgba(255, 154, 98, 0.3);
}

.login-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(255, 122, 69, 0.4);
  background: linear-gradient(135deg, #FFB488 0%, #FF9A62 100%);
}

.login-btn:active {
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(255, 154, 98, 0.3);
}

/* 底部 */
.footer {
  text-align: center;
  margin-top: var(--spacing-lg);
  color: var(--color-text-secondary);
}

.hint {
  font-size: var(--font-size-sm);
}

/* 快速登录提示 */
.quick-login-hint {
  margin-top: var(--spacing-xl);
  padding-top: var(--spacing-lg);
}

.test-account {
  text-align: center;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: var(--spacing-md) 0 0;
}

.highlight {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  font-family: 'Courier New', monospace;
}

/* 响应式 */
@media (max-width: 768px) {
  .login-card {
    width: 90%;
    margin: var(--spacing-lg);
  }
}
</style>

