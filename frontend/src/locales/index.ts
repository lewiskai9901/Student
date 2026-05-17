/**
 * vue-i18n 入口 — F7 国际化骨架.
 *
 * 设计:
 *   - legacy: false → 使用 Composition API (`useI18n()`).
 *   - globalInjection: true → 模板可直接 `{{ $t('key') }}`.
 *   - fallbackLocale: 'zh-CN' → 缺英文翻译时回退中文.
 *   - localStorage 持久化用户语言选择 (key: app:locale).
 *
 * 用法:
 *   - 模板: `{{ $t('common.confirm') }}` 或 `{{ t('common.confirm') }}` (script setup).
 *   - script: `const { t } = useI18n()`.
 *   - 切换: `import { setLocale } from '@/locales'; setLocale('en-US')`.
 *
 * 后续完整国际化按模块逐步迁移 — 本次只是骨架.
 */
import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'
import { safeLocalStorage } from '@/utils/safeStorage'

const STORAGE_KEY = 'app:locale'

export type Locale = 'zh-CN' | 'en-US'

function readInitial(): Locale {
  // K1: 走 safeLocalStorage (之前手动 try/catch, 统一收敛)
  const saved = safeLocalStorage.getItem(STORAGE_KEY)
  if (saved === 'zh-CN' || saved === 'en-US') return saved
  return 'zh-CN'
}

const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: readInitial(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

/**
 * 切换全局语言 + 持久化到 localStorage.
 */
export function setLocale(l: Locale): void {
  i18n.global.locale.value = l
  // K1: 失败不影响内存切换
  safeLocalStorage.setItem(STORAGE_KEY, l)
}

/**
 * 读取当前 locale (响应式).
 */
export function getLocale(): Locale {
  return i18n.global.locale.value as Locale
}

export const SUPPORTED_LOCALES: readonly Locale[] = ['zh-CN', 'en-US'] as const

export default i18n
