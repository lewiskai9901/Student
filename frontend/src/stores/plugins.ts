import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * Phase 6.4: 响应式追踪已加载的插件, 让 MainLayout 菜单能在 addRoute 后
 * 自动刷新 — 取代原来只依赖 router.getRoutes() (非响应式) 的方案.
 */
export const usePluginsStore = defineStore('plugins', () => {
  // 已通过 router.addRoute 注册的行业码 (不含 CORE — CORE 永远存在)
  const loadedCodes = ref<Set<string>>(new Set())

  // 每次加载触发一次, menuList computed 依赖此值
  const loadVersion = ref(0)

  const markLoaded = (code: string) => {
    if (loadedCodes.value.has(code)) return false
    loadedCodes.value.add(code)
    loadVersion.value++ // 触发 computed 重算
    return true
  }

  const markUnloaded = (code: string) => {
    if (!loadedCodes.value.has(code)) return false
    loadedCodes.value.delete(code)
    loadVersion.value++
    return true
  }

  const isLoaded = (code: string) => loadedCodes.value.has(code)

  const codes = computed(() => Array.from(loadedCodes.value))

  return {
    loadedCodes,
    loadVersion,
    codes,
    markLoaded,
    markUnloaded,
    isLoaded
  }
})
