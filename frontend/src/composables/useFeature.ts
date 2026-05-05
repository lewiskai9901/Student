/**
 * Phase 2 — 前端特征开关 (Feature Flags) composable.
 *
 * 单一来源, 全前端通过 hasFeature(code) 判定某个行业插件 / 子模块是否启用.
 * 桥接 pluginsStore (运行时由 router/bootstrap.ts 加载后写入).
 *
 * 用法:
 * <pre>
 *   import { hasFeature } from '@/composables/useFeature'
 *   if (hasFeature('EDU')) { ... }
 *   if (hasFeature('education.dormitory')) { ... }
 * </pre>
 *
 * 行业大码 (EDU/HEALTH 等) 由 pluginsStore 直接判定;
 * 子特征 (education.dormitory) 走"启用大码 + 内置假设"双判 — 简单场景够用,
 * 复杂分支以后再扩到 PluginConfigSchema.
 */
import { computed } from 'vue'
import { usePluginsStore } from '@/stores/plugins'

// 大码到子特征的内置映射 — 当大码启用时, 这些子特征默认开
const BUILTIN_SUB_FEATURES: Record<string, string[]> = {
  EDU: [
    'education',           // 别名
    'education.student',
    'education.class',
    'education.dormitory',
    'education.academic',
    'education.teaching',
    'education.grade',
    'education.calendar',
  ],
  HEALTH: [
    'healthcare',
    'healthcare.patient',
    'healthcare.ward',
    'healthcare.bed',
  ],
}

function normalizeCode(code: string): string {
  return code.trim().toUpperCase()
}

/**
 * 判定特征是否启用. 接受大码 (EDU/HEALTH/CORE) 或子特征 (education.dormitory).
 *
 * - CORE 永远 true (通用核心)
 * - 大码: 直接查 pluginsStore.loadedCodes
 * - 子特征: 解析为 ROOT.code → 查内置映射
 */
export function hasFeature(code: string | null | undefined): boolean {
  if (!code) return false
  const normalized = normalizeCode(code)
  if (normalized === 'CORE') return true

  const store = usePluginsStore()

  // 直接是大码 (EDU/HEALTH)
  if (store.isLoaded(normalized)) return true

  // 子特征: education.dormitory → root EDU
  const lower = code.trim().toLowerCase()
  for (const [root, subs] of Object.entries(BUILTIN_SUB_FEATURES)) {
    if (subs.includes(lower)) {
      return store.isLoaded(root)
    }
  }

  return false
}

/** 反向: 特征是否禁用. */
export function isFeatureDisabled(code: string | null | undefined): boolean {
  return !hasFeature(code)
}

/**
 * 响应式 composable — 在 setup() 中使用, pluginsStore 变化自动重算.
 */
export function useFeature(code: string) {
  const enabled = computed(() => hasFeature(code))
  const disabled = computed(() => !enabled.value)
  return { enabled, disabled }
}

/**
 * 多个特征 ANY (任一启用即 true).
 */
export function useAnyFeature(codes: string[]) {
  const enabled = computed(() => codes.some(c => hasFeature(c)))
  return { enabled, disabled: computed(() => !enabled.value) }
}

/**
 * 多个特征 ALL (全部启用才 true).
 */
export function useAllFeatures(codes: string[]) {
  const enabled = computed(() => codes.every(c => hasFeature(c)))
  return { enabled, disabled: computed(() => !enabled.value) }
}
