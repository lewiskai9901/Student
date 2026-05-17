/**
 * Phase 5 — useTheme composable.
 *
 * 全前端单一入口管理 light/dark 主题. 写 <html data-theme="dark"> + persist 到 localStorage.
 * 支持 'system' 跟随系统主题.
 *
 * 用法:
 * <pre>
 *   import { useTheme } from '@/composables/useTheme'
 *   const { theme, isDark, toggle, setTheme } = useTheme()
 *   <button @click="toggle">{{ isDark ? '☀' : '🌙' }}</button>
 * </pre>
 */
import { ref, computed, onMounted, watch } from 'vue'
import { safeLocalStorage } from '@/utils/safeStorage'

type Theme = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'app-theme'
const DOC_ATTR = 'data-theme'

// 单例 ref — 全前端共享
const theme = ref<Theme>(loadInitialTheme())
const systemPrefersDark = ref(prefersDarkInitial())

function loadInitialTheme(): Theme {
  if (typeof window === 'undefined') return 'light'
  // K1: 走 safeLocalStorage 处理 Safari 隐私模式
  const saved = safeLocalStorage.getItem(STORAGE_KEY)
  if (saved === 'dark' || saved === 'light' || saved === 'system') return saved
  return 'system'
}

function prefersDarkInitial(): boolean {
  if (typeof window === 'undefined' || !window.matchMedia) return false
  try {
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    return !!(mq && mq.matches)
  } catch {
    return false
  }
}

const isDark = computed<boolean>(() => {
  if (theme.value === 'dark') return true
  if (theme.value === 'light') return false
  return systemPrefersDark.value
})

/** 应用 data-theme 到 <html>. */
function applyTheme() {
  if (typeof document === 'undefined') return
  if (isDark.value) {
    document.documentElement.setAttribute(DOC_ATTR, 'dark')
  } else {
    document.documentElement.removeAttribute(DOC_ATTR)
  }
}

function setTheme(t: Theme) {
  theme.value = t
  // K1: 失败不影响内存切换
  safeLocalStorage.setItem(STORAGE_KEY, t)
  applyTheme()
}

function toggle() {
  setTheme(isDark.value ? 'light' : 'dark')
}

/** 在应用启动时调用一次 (App.vue onMounted). 监听系统主题变化. */
function initTheme() {
  if (typeof window === 'undefined') return
  applyTheme()
  if (!window.matchMedia) return
  let mql: MediaQueryList
  try { mql = window.matchMedia('(prefers-color-scheme: dark)') }
  catch { return }
  if (!mql) return
  const handler = (e: MediaQueryListEvent) => {
    systemPrefersDark.value = e.matches
    if (theme.value === 'system') applyTheme()
  }
  if (typeof mql.addEventListener === 'function') {
    mql.addEventListener('change', handler)
  } else if (typeof (mql as MediaQueryList & { addListener?: (l: (e: MediaQueryListEvent) => void) => void }).addListener === 'function') {
    (mql as MediaQueryList & { addListener: (l: (e: MediaQueryListEvent) => void) => void }).addListener(handler)
  }
}

watch(isDark, () => applyTheme())

export function useTheme() {
  onMounted(initTheme)
  return { theme, isDark, setTheme, toggle, initTheme }
}

/** 静态 API (App.vue setup 早期调用). */
export const themeApi = {
  init: initTheme,
  toggle,
  setTheme,
  get isDark() { return isDark.value },
  get current() { return theme.value },
}
