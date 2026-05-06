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
import { installErrorReporter } from './utils/errorReporter'

// 引入设计系统令牌 (最高优先级)
import '@/styles/design-tokens.css'
// Inspection 模块设计令牌 — Audit Console 美学
import '@/styles/inspection-tokens.css'
// 引入 Tailwind CSS 全局样式
import '@/assets/styles/globals.css'
// 保留原有样式（暂时兼容）
import '@/assets/styles/index.scss'

const app = createApp(App)

// P3 客户端错误上报 (Vue + window.onerror + unhandledrejection)
installErrorReporter(app)

app.use(createPinia())

const authStore = useAuthStore()

async function bootstrap() {
  // initAuth 现在是 async 且自愈: token 有效但 user_info 丢失时 fetch /me 补齐
  await authStore.initAuth()
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