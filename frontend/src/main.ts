// 引入 Element Plus 样式
import 'element-plus/dist/index.css'

// 引入全局样式（顺序很重要：变量 → 动画 → 主题 → 组件样式）
import './assets/styles/variables.css'
import './assets/styles/animations.css'
import './assets/styles/theme.css'
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistence from 'pinia-plugin-persistedstate'

import App from './App.vue'
import router from './router'

const pinia = createPinia()
pinia.use(piniaPluginPersistence)

const app = createApp(App)

app.use(pinia)
app.use(router)

app.mount('#app')
