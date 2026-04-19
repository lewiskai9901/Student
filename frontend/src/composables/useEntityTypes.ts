import { ref, computed, type Ref } from 'vue'
import { http } from '@/utils/request'

/**
 * useEntityTypes — 运行时消费 entity_type_configs 的 composable.
 *
 * 替代前端硬编码的 `ROOM_TYPE_CONFIG` / `USER_TYPE_CONFIG` 等映射表,
 * 让 icon/color/label/features 这些 UI 元数据完全由后端插件声明驱动.
 *
 * 用法:
 *   const { typeOptions, getByCode, loading } = useEntityTypes('PLACE')
 *   // typeOptions: [{ value: 'DORM_ROOM', label: '宿舍', icon: 'bed', color: '#0d9488' }, ...]
 *   const icon = getByCode('DORM_ROOM')?.uiConfig?.icon
 */

export interface EntityTypeConfig {
  id: number
  entityType: string           // 'USER' | 'ORG_UNIT' | 'PLACE'
  typeCode: string             // 'STUDENT' | 'DORM_ROOM' | ...
  typeName: string
  category?: string
  parentTypeCode?: string
  metadataSchema?: any
  features?: Record<string, boolean>
  uiConfig?: { icon?: string; color?: string; [k: string]: any }
  isPluginRegistered?: number
  pluginClass?: string
  industry?: string
}

/** 内存缓存,避免重复请求 */
const cache = new Map<string, Ref<EntityTypeConfig[]>>()
const loadingMap = new Map<string, Ref<boolean>>()

export function useEntityTypes(entityType: 'USER' | 'ORG_UNIT' | 'PLACE') {
  if (!cache.has(entityType)) {
    const data = ref<EntityTypeConfig[]>([])
    const loading = ref(false)
    cache.set(entityType, data)
    loadingMap.set(entityType, loading)
    loadConfigs(entityType, data, loading)
  }
  const data = cache.get(entityType)!
  const loading = loadingMap.get(entityType)!

  const typeOptions = computed(() => data.value.map(t => ({
    value: t.typeCode,
    label: t.typeName,
    icon: t.uiConfig?.icon,
    color: t.uiConfig?.color
  })))

  function getByCode(typeCode: string): EntityTypeConfig | undefined {
    return data.value.find(t => t.typeCode === typeCode)
  }

  function getIcon(typeCode: string): string | undefined {
    return getByCode(typeCode)?.uiConfig?.icon
  }

  function getColor(typeCode: string): string | undefined {
    return getByCode(typeCode)?.uiConfig?.color
  }

  function getLabel(typeCode: string): string {
    return getByCode(typeCode)?.typeName || typeCode
  }

  function hasFeature(typeCode: string, featureKey: string): boolean {
    const cfg = getByCode(typeCode)
    const raw = cfg?.features
    if (!raw) return false
    const feat: any = typeof raw === 'string' ? (() => { try { return JSON.parse(raw) } catch { return {} } })() : raw
    return !!feat?.[featureKey]
  }

  async function refresh() {
    await loadConfigs(entityType, data, loading)
  }

  return {
    data,
    loading,
    typeOptions,
    getByCode,
    getIcon,
    getColor,
    getLabel,
    hasFeature,
    refresh
  }
}

async function loadConfigs(entityType: string, data: Ref<EntityTypeConfig[]>, loading: Ref<boolean>) {
  loading.value = true
  try {
    const resp: any = await http.get('/entity-type-configs', { params: { entityType } })
    const list = Array.isArray(resp) ? resp : (resp?.records || resp?.list || [])
    data.value = list.map((r: any) => ({
      ...r,
      features: typeof r.features === 'string' ? (() => { try { return JSON.parse(r.features) } catch { return {} } })() : (r.features || {}),
      uiConfig: typeof r.uiConfig === 'string' ? (() => { try { return JSON.parse(r.uiConfig) } catch { return {} } })() : (r.uiConfig || {})
    }))
  } catch (err) {
    console.error('[useEntityTypes] 加载失败', entityType, err)
    data.value = []
  } finally {
    loading.value = false
  }
}
