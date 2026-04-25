<template>
  <div class="flex h-full flex-col overflow-y-auto bg-gray-50 p-4">
    <!-- Empty state -->
    <div v-if="!type && !isNew" class="flex h-full items-center justify-center text-center">
      <div class="text-gray-400">
        <Eye class="mx-auto mb-2 h-8 w-8 text-gray-300" />
        <div class="text-sm">从左侧选择类型</div>
        <div class="mt-1 text-xs text-gray-300">查看用量和依赖</div>
      </div>
    </div>

    <div v-else-if="isNew" class="rounded-md bg-white p-4">
      <div class="mb-2 flex items-center gap-2 text-sm font-medium text-gray-700">
        <Sparkles class="h-4 w-4 text-blue-500" />
        新建类型指引
      </div>
      <ul class="space-y-2 text-xs text-gray-600">
        <li class="flex gap-2"><span class="text-gray-400">1.</span>选择实体类型 (组织/场所/用户), 决定这个类型归属哪棵树</li>
        <li class="flex gap-2"><span class="text-gray-400">2.</span>编码全大写 + 下划线, 创建后不可改</li>
        <li class="flex gap-2"><span class="text-gray-400">3.</span>分类可选, 决定默认启用的 feature 集合</li>
        <li class="flex gap-2"><span class="text-gray-400">4.</span>父子关系可在创建后补齐</li>
      </ul>
    </div>

    <template v-else>
      <!-- 插件信息 -->
      <div class="mb-3 rounded-md bg-white p-3 shadow-sm">
        <div class="mb-2 text-[10px] font-medium uppercase tracking-wide text-gray-500">
          来源
        </div>
        <div class="flex items-center justify-between">
          <span class="text-sm font-medium text-gray-700">
            {{ type!.isPluginRegistered ? '插件注册' : '管理员自创' }}
          </span>
          <span class="rounded-full px-2 py-0.5 text-[10px] font-medium"
                :class="type!.isPluginRegistered ? 'bg-blue-100 text-blue-700' : 'bg-amber-100 text-amber-700'">
            {{ type!.industry || 'CUSTOM' }}
          </span>
        </div>
        <div v-if="type!.pluginClass" class="mt-2 overflow-x-auto font-mono text-[10px] text-gray-400">
          {{ type!.pluginClass }}
        </div>
        <div v-if="isPluginDisabled"
             class="mt-2 rounded bg-orange-50 px-2 py-1.5 text-[11px] text-orange-700">
          <AlertTriangle class="mr-1 inline h-3 w-3" />
          所属插件已禁用, 本类型级联失效
        </div>
      </div>

      <!-- 管理员覆写 -->
      <div v-if="type!.isPluginRegistered && overriddenList.length > 0"
           class="mb-3 rounded-md border border-indigo-200 bg-indigo-50 p-3">
        <div class="mb-2 flex items-center gap-1 text-[10px] font-medium uppercase tracking-wide text-indigo-700">
          <PenLine class="h-3 w-3" />
          管理员覆写 ({{ overriddenList.length }})
        </div>
        <div class="mb-2 text-[11px] text-indigo-700">
          以下字段插件重启时不会被覆盖, 以管理员配置为准:
        </div>
        <div class="space-y-1">
          <div v-for="f in overriddenList" :key="f"
               class="flex items-center justify-between rounded bg-white px-2 py-1 text-xs">
            <span class="font-mono text-[11px] text-gray-700">{{ f }}</span>
            <button
              class="text-[10px] text-blue-600 hover:underline disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="isPluginDisabled"
              @click="$emit('reset-field', type!, f)">
              恢复
            </button>
          </div>
        </div>
        <button
          class="mt-2 h-6 w-full rounded bg-indigo-600 text-[11px] text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="isPluginDisabled"
          @click="$emit('reset-all', type!)">
          全部恢复为插件默认
        </button>
      </div>

      <!-- 用量 -->
      <div class="mb-3 rounded-md bg-white p-3 shadow-sm">
        <div class="mb-2 text-[10px] font-medium uppercase tracking-wide text-gray-500">
          使用情况
        </div>
        <div v-if="usageLoading" class="flex items-center gap-2 text-xs text-gray-400">
          <Loader2 class="h-3 w-3 animate-spin" />
          查询中...
        </div>
        <div v-else class="space-y-1.5">
          <div class="flex items-center justify-between text-xs">
            <span class="text-gray-600">实例数</span>
            <span class="font-mono text-sm font-medium" :class="usageCount > 0 ? 'text-gray-800' : 'text-gray-400'">
              {{ usageCount }} 条
            </span>
          </div>
          <div class="flex items-center justify-between text-xs">
            <span class="text-gray-600">子类型</span>
            <span class="font-mono text-sm" :class="childTypes.length > 0 ? 'text-gray-800' : 'text-gray-400'">
              {{ childTypes.length }} 个
            </span>
          </div>
          <div class="flex items-center justify-between text-xs">
            <span class="text-gray-600">扩展字段</span>
            <span class="font-mono text-sm" :class="fieldCount > 0 ? 'text-gray-800' : 'text-gray-400'">
              {{ fieldCount }} 个
            </span>
          </div>
        </div>
      </div>

      <!-- 子类型展开列表 -->
      <div v-if="childTypes.length" class="mb-3 rounded-md bg-white p-3 shadow-sm">
        <div class="mb-2 text-[10px] font-medium uppercase tracking-wide text-gray-500">
          子类型 ({{ childTypes.length }})
        </div>
        <div class="space-y-1">
          <button v-for="c in childTypes" :key="c.typeCode"
                  class="flex w-full items-center gap-2 rounded px-2 py-1 text-xs hover:bg-blue-50"
                  @click="$emit('select', c)">
            <ArrowDownRight class="h-3 w-3 flex-shrink-0 text-gray-400" />
            <span class="flex-1 truncate text-gray-700">{{ c.typeName }}</span>
            <span class="font-mono text-[10px] text-gray-400">{{ c.typeCode }}</span>
          </button>
        </div>
      </div>

      <!-- 危险区域 -->
      <div v-if="!type!.isPluginRegistered" class="mt-auto rounded-md border border-red-200 bg-red-50 p-3">
        <div class="mb-2 flex items-center gap-1.5 text-[10px] font-medium uppercase tracking-wide text-red-700">
          <AlertTriangle class="h-3 w-3" />
          危险区域
        </div>
        <div class="mb-2 text-xs text-red-700">
          删除此类型
          <template v-if="usageCount > 0 || childTypes.length > 0">
            将影响
            <b v-if="usageCount > 0">{{ usageCount }} 个实例</b>
            <b v-if="usageCount > 0 && childTypes.length > 0">、</b>
            <b v-if="childTypes.length > 0">{{ childTypes.length }} 个子类型</b>
            <span class="block mt-1 text-[10px]">建议先迁移数据再删除</span>
          </template>
          <template v-else>不会影响任何数据, 可安全删除</template>
        </div>
        <button class="h-7 w-full rounded bg-red-600 text-xs font-medium text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="isPluginDisabled"
                @click="$emit('delete', type!)">
          删除此类型
        </button>
      </div>
      <div v-else class="mt-auto rounded-md bg-blue-50 p-3 text-xs text-blue-700">
        <Info class="mr-1 inline h-3 w-3" />
        插件注册类型不可删除, 只能通过插件禁用级联停用
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  Eye, Sparkles, AlertTriangle, Loader2, ArrowDownRight, Info, PenLine,
} from 'lucide-vue-next'
import type { EntityTypeConfig } from '@/api/entityType'

interface Props {
  type: EntityTypeConfig | null
  isNew: boolean
  allTypes: EntityTypeConfig[]
}
const props = defineProps<Props>()
defineEmits<{
  select: [t: EntityTypeConfig]
  delete: [t: EntityTypeConfig]
  'reset-field': [t: EntityTypeConfig, field: string]
  'reset-all': [t: EntityTypeConfig]
}>()

const overriddenList = computed<string[]>(() => {
  const raw = props.type?.overriddenFields
  if (!raw) return []
  try {
    const arr = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
})

const usageCount = ref(0)
const usageLoading = ref(false)

const isPluginDisabled = computed(() => {
  const t = props.type
  if (!t) return false
  return t.pluginEnabled === false || t.pluginEnabled === 0
})

const childTypes = computed(() => {
  const t = props.type
  if (!t) return []
  return props.allTypes.filter(x =>
    x.entityType === t.entityType && x.parentTypeCode === t.typeCode
  )
})

const fieldCount = computed(() => {
  const t = props.type
  if (!t) return 0
  const schema = typeof t.metadataSchema === 'string'
    ? JSON.parse(t.metadataSchema || '{"fields":[]}')
    : t.metadataSchema || { fields: [] }
  return (schema.fields || []).length
})

watch(() => props.type?.typeCode, async () => {
  const t = props.type
  if (!t) { usageCount.value = 0; return }
  usageLoading.value = true
  try {
    // Query instance count based on entity type
    const r = await fetch(`/api/entity-type-configs/${t.id}/usage-count`, {
      headers: { Authorization: 'Bearer ' + (localStorage.getItem('access_token')
        || sessionStorage.getItem('access_token') || '') },
    })
    if (r.ok) {
      const j = await r.json()
      usageCount.value = Number(j.data ?? j.count ?? 0)
    } else {
      // endpoint may not exist yet — fallback to 0
      usageCount.value = 0
    }
  } catch {
    usageCount.value = 0
  } finally {
    usageLoading.value = false
  }
}, { immediate: true })
</script>
