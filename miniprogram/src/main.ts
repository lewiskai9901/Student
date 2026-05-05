import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { dispatcher } from './core/plugin/dispatcher'
import { allPlugins } from './plugins'

export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())

  for (const p of allPlugins) {
    dispatcher.register(p)
  }

  return { app }
}
