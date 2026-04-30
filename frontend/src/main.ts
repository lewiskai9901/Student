import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import VueKonva from 'vue-konva'
import App from './App.vue'
import router from './router'
import { loadEnabledPlugins } from './router/bootstrap'
import { useAuthStore } from './stores/auth'

// 引入设计系统令牌 (最高优先级)
import '@/styles/design-tokens.css'
// Inspection 模块设计令牌 — Audit Console 美学
import '@/styles/inspection-tokens.css'
// 引入 Tailwind CSS 全局样式
import '@/assets/styles/globals.css'
// 保留原有样式（暂时兼容）
import '@/assets/styles/index.scss'

const app = createApp(App)

app.use(createPinia())

const authStore = useAuthStore()
authStore.initAuth()

async function bootstrap() {
  // Phase 4A: 已登录则预装启用的插件路由 (未登录时 bootstrap 会静默 401 跳过)
  if (authStore.isAuthenticated) {
    await loadEnabledPlugins(router)
  }
  app.use(router)
  app.use(VueKonva)
  app.use(ElementPlus, { locale: zhCn })
  app.mount('#app')
}

bootstrap()