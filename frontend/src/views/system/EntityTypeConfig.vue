<template>
  <div class="flex h-[calc(100vh-64px)] flex-col bg-gray-100">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-5 py-3">
      <div>
        <h1 class="flex items-center gap-2 text-base font-semibold text-gray-800">
          <Layers class="h-4 w-4 text-blue-500" />
          类型配置
          <span class="text-xs font-normal text-gray-400">统一管理组织/场所/用户类型</span>
        </h1>
        <div class="mt-1 flex items-center gap-3 text-[11px] text-gray-500">
          <span>组织 <b class="text-gray-700">{{ countByEntity.ORG_UNIT }}</b></span>
          <span class="text-gray-300">|</span>
          <span>场所 <b class="text-gray-700">{{ countByEntity.PLACE }}</b></span>
          <span class="text-gray-300">|</span>
          <span>用户 <b class="text-gray-700">{{ countByEntity.USER }}</b></span>
          <span class="text-gray-300">|</span>
          <span>自定义 <b class="text-amber-600">{{ customCount }}</b></span>
          <span v-if="pluginDisabledCount > 0" class="text-gray-300">|</span>
          <span v-if="pluginDisabledCount > 0">
            插件禁用级联 <b class="text-orange-600">{{ pluginDisabledCount }}</b>
          </span>
        </div>
      </div>
      <button
        class="flex h-8 items-center gap-1 rounded-md border border-gray-200 bg-white px-3 text-xs text-gray-600 hover:bg-gray-50"
        @click="loadAll"
      >
        <RefreshCw class="h-3.5 w-3.5" :class="loading ? 'animate-spin' : ''" />
        刷新
      </button>
    </div>

    <!-- 3-column IDE layout -->
    <div class="flex min-h-0 flex-1">
      <!-- Left: explorer -->
      <div class="w-[260px] flex-shrink-0 border-r border-gray-200 bg-white">
        <TypeExplorer
          :types="allTypes"
          :loading="loading"
          :selected-id="selectedId"
          @select="onSelect"
          @create="onCreate"
        />
      </div>

      <!-- Middle: editor -->
      <div class="min-w-0 flex-1 bg-white">
        <TypeEditor
          v-if="selectedType || isNew"
          :type="selectedType"
          :is-new="isNew"
          :all-types="allTypes"
          :category-map="categoryMap"
          @saved="onSaved"
          @cancel="onEditorCancel"
          @select="onSelect"
        />
        <div v-else class="flex h-full flex-col items-center justify-center text-gray-400">
          <Layers class="mb-3 h-12 w-12 text-gray-200" />
          <div class="text-sm">从左侧选一个类型查看/编辑</div>
          <div class="mt-1 text-xs text-gray-300">或点底部 "+ 新建类型"</div>
        </div>
      </div>

      <!-- Right: usage -->
      <div class="w-[300px] flex-shrink-0 border-l border-gray-200 bg-gray-50">
        <TypeUsagePanel
          :type="selectedType"
          :is-new="isNew"
          :all-types="allTypes"
          @select="onSelect"
          @delete="onDelete"
          @reset-field="onResetField"
          @reset-all="onResetAll"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Layers, RefreshCw } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { entityTypeApi, type EntityTypeConfig, type CategoryOption } from '@/api/entityType'
import { http } from '@/utils/request'
import TypeExplorer from './entity-types/TypeExplorer.vue'
import TypeEditor from './entity-types/TypeEditor.vue'
import TypeUsagePanel from './entity-types/TypeUsagePanel.vue'

const route = useRoute()
const router = useRouter()

const allTypes = ref<EntityTypeConfig[]>([])
const categoryMap = ref<Record<string, CategoryOption[]>>({ ORG_UNIT: [], PLACE: [], USER: [] })
const loading = ref(false)
const isNew = ref(false)

const selectedId = computed<string | null>({
  get() {
    const raw = route.query.type
    if (raw == null || raw === '') return null
    return String(raw)
  },
  set(v) {
    router.replace({ query: { ...route.query, type: v ?? undefined } })
  },
})

const selectedType = computed<EntityTypeConfig | null>(() => {
  if (isNew.value) return null
  const id = selectedId.value
  if (!id) return null
  return allTypes.value.find(t => String(t.id) === id) || null
})

const countByEntity = computed(() => ({
  ORG_UNIT: allTypes.value.filter(t => t.entityType === 'ORG_UNIT').length,
  PLACE:    allTypes.value.filter(t => t.entityType === 'PLACE').length,
  USER:     allTypes.value.filter(t => t.entityType === 'USER').length,
}))

const customCount = computed(() => allTypes.value.filter(t => !t.isPluginRegistered).length)

const pluginDisabledCount = computed(() =>
  allTypes.value.filter(t => t.pluginEnabled === false || t.pluginEnabled === 0).length
)

async function loadAll() {
  loading.value = true
  try {
    const types: Array<'ORG_UNIT' | 'PLACE' | 'USER'> = ['ORG_UNIT', 'PLACE', 'USER']
    const results = await Promise.all(types.map(t => entityTypeApi.list(t, true)))
    const merged: EntityTypeConfig[] = []
    for (const r of results) {
      const list = ((r as any).data || r || []) as EntityTypeConfig[]
      merged.push(...list)
    }
    allTypes.value = merged
  } catch {
    allTypes.value = []
    ElMessage.error('加载类型失败')
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  const types: Array<'ORG_UNIT' | 'PLACE' | 'USER'> = ['ORG_UNIT', 'PLACE', 'USER']
  const results = await Promise.allSettled(types.map(t => entityTypeApi.getCategories(t)))
  const map: Record<string, CategoryOption[]> = { ORG_UNIT: [], PLACE: [], USER: [] }
  results.forEach((r, i) => {
    if (r.status === 'fulfilled') {
      const v: any = r.value
      map[types[i]] = (Array.isArray(v) ? v : (v?.data || [])) as CategoryOption[]
    }
  })
  categoryMap.value = map
}

function onSelect(t: EntityTypeConfig) {
  isNew.value = false
  selectedId.value = String(t.id)
}

function onCreate() {
  isNew.value = true
  selectedId.value = null
}

function onEditorCancel() {
  isNew.value = false
}

async function onSaved(t: EntityTypeConfig) {
  isNew.value = false
  await loadAll()
  if (t?.id) selectedId.value = String(t.id)
}

async function onResetField(t: EntityTypeConfig, field: string) {
  try {
    await ElMessageBox.confirm(
      `恢复字段 "${field}" 为插件默认值?\n当前管理员修改会丢失。`,
      '恢复确认', { type: 'warning' }
    )
    await entityTypeApi.resetField(t.id, field)
    ElMessage.success(`字段 ${field} 已恢复`)
    await loadAll()
  } catch (e: any) {
    if (e === 'cancel' || e?.message === 'cancel') return
    ElMessage.error('恢复失败')
  }
}

async function onResetAll(t: EntityTypeConfig) {
  const raw = t.overriddenFields
  const list: string[] = raw
    ? (typeof raw === 'string' ? JSON.parse(raw) : raw) as string[]
    : []
  if (list.length === 0) return
  try {
    await ElMessageBox.confirm(
      `恢复所有 ${list.length} 个覆写字段为插件默认?\n所有管理员修改将丢失。`,
      '批量恢复', { type: 'warning', confirmButtonText: '全部恢复', cancelButtonText: '取消' }
    )
    for (const f of list) {
      await entityTypeApi.resetField(t.id, f)
    }
    ElMessage.success(`已恢复 ${list.length} 个字段`)
    await loadAll()
  } catch (e: any) {
    if (e === 'cancel' || e?.message === 'cancel') return
    ElMessage.error('批量恢复失败')
  }
}

async function onDelete(t: EntityTypeConfig) {
  if (t.isPluginRegistered) { ElMessage.warning('插件注册的类型不能删除'); return }
  try {
    await ElMessageBox.confirm(
      `确定删除类型 "${t.typeName}" (${t.typeCode}) ?\n此操作不可恢复。`,
      '删除确认', { type: 'warning' }
    )
    await http.delete(`/entity-type-configs/${t.id}`)
    ElMessage.success('已删除')
    selectedId.value = null
    await loadAll()
  } catch (e: any) {
    if (e === 'cancel' || e?.message === 'cancel') return
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadAll()
  loadCategories()
})
</script>
