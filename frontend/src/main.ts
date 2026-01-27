import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'

// 引入设计系统令牌 (最高优先级)
import '@/styles/design-tokens.css'
// 引入 Tailwind CSS 全局样式
import '@/assets/styles/globals.css'
// 保留原有样式（暂时兼容）
import '@/assets/styles/index.scss'

const app = createApp(App)

app.use(createPinia())

// 初始化认证状态
const authStore = useAuthStore()
authStore.initAuth()

app.use(router)
app.use(ElementPlus, {
  locale: zhCn
})

app.mount('#app')