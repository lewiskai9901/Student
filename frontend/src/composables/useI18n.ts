/**
 * Phase 5 — 轻量 i18n composable (无 vue-i18n 依赖).
 *
 * 提供 t(key) / locale / setLocale 与 vue-i18n 同名 API. 切换 locale 时
 * 写 localStorage + reactive ref 触发组件重渲染.
 *
 * 用法:
 * <pre>
 *   const { t, locale, setLocale } = useI18n()
 *   <p>{{ t('common.confirm') }}</p>
 *   <button @click="setLocale('en-US')">EN</button>
 * </pre>
 *
 * 如未来需 vue-i18n 高级特性 (复数/datetime fmt/SFC inline messages),
 * 此 API 可平滑替换 — 核心 t() 签名一致.
 */
import { ref, computed } from 'vue'
import zhCN from '@/locales/zh-CN'
import enUS from '@/locales/en-US'

export type Locale = 'zh-CN' | 'en-US'

const STORAGE_KEY = 'app-locale'
const DEFAULT_LOCALE: Locale = 'zh-CN'

const messages: Record<Locale, Record<string, unknown>> = {
  'zh-CN': zhCN,
  'en-US': enUS,
}

const locale = ref<Locale>(loadInitialLocale())

function loadInitialLocale(): Locale {
  if (typeof window === 'undefined') return DEFAULT_LOCALE
  const saved = window.localStorage?.getItem(STORAGE_KEY)
  if (saved === 'zh-CN' || saved === 'en-US') return saved
  // 浏览器默认语言
  const nav = (typeof navigator !== 'undefined' && navigator.language) || ''
  if (nav.startsWith('en')) return 'en-US'
  return DEFAULT_LOCALE
}

function setLocale(l: Locale) {
  locale.value = l
  if (typeof window !== 'undefined') {
    window.localStorage?.setItem(STORAGE_KEY, l)
  }
}

/** 按 'a.b.c' 路径取值, 未找到回退到 zh-CN, 仍未找到返回 key 本身. */
function lookup(table: Record<string, unknown>, path: string): string | null {
  const parts = path.split('.')
  let cur: unknown = table
  for (const p of parts) {
    if (cur && typeof cur === 'object' && p in (cur as Record<string, unknown>)) {
      cur = (cur as Record<string, unknown>)[p]
    } else {
      return null
    }
  }
  return typeof cur === 'string' ? cur : null
}

/** 模板替换 {name} 等占位符. */
function format(s: string, params?: Record<string, string | number>): string {
  if (!params) return s
  return s.replace(/\{(\w+)\}/g, (_, k: string) =>
    String(params[k] ?? `{${k}}`))
}

/**
 * t(key, params?) — 翻译函数. 缺当前 locale 自动回退到 zh-CN.
 */
function t(key: string, params?: Record<string, string | number>): string {
  // computed 包装确保响应式 — 不过手动函数也要每次读 locale.value
  const tableForLocale = messages[locale.value] || messages[DEFAULT_LOCALE]
  const found = lookup(tableForLocale, key)
  if (found != null) return format(found, params)
  if (locale.value !== DEFAULT_LOCALE) {
    const fallback = lookup(messages[DEFAULT_LOCALE], key)
    if (fallback != null) return format(fallback, params)
  }
  return key
}

export function useI18n() {
  return {
    t,
    locale,
    setLocale,
    availableLocales: Object.keys(messages) as Locale[],
    isReady: computed(() => true),
  }
}

export const i18n = { t, get locale() { return locale.value }, setLocale }
