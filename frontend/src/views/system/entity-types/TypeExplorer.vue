<template>
  <div class="flex h-full flex-col bg-white">
    <!-- 搜索 + 筛选 -->
    <div class="border-b border-gray-200 p-3">
      <div class="relative">
        <Search class="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <input
          v-model="keyword"
          type="text"
          placeholder="搜类型名 / 编码"
          class="h-8 w-full rounded-md border border-gray-200 pl-8 pr-2 text-xs focus:border-blue-500 focus:outline-none"
        />
      </div>
      <div class="mt-2 flex flex-wrap items-center gap-2 text-[11px]">
        <label class="flex items-center gap-1 text-gray-500 cursor-pointer">
          <input type="checkbox" v-model="onlyCustom" class="h-3 w-3" />
          只看自定义
        </label>
        <label class="flex items-center gap-1 text-gray-500 cursor-pointer">
          <input type="checkbox" v-model="onlyDisabled" class="h-3 w-3" />
          只看禁用
        </label>
      </div>
    </div>

    <!-- 分组树 -->
    <div class="flex-1 overflow-y-auto">
      <div v-if="loading" class="flex h-32 items-center justify-center text-sm text-gray-400">
        <Loader2 class="h-5 w-5 animate-spin" />
      </div>
      <template v-else>
        <div v-for="g in groups" :key="g.industry"
             class="border-b border-gray-100 last:border-b-0">
          <!-- industry group header -->
          <button
            class="flex w-full items-center gap-2 px-3 py-2 text-left text-xs font-medium text-gray-600 hover:bg-gray-50"
            @click="toggleIndustry(g.industry)"
          >
            <ChevronDown class="h-3 w-3 transition-transform"
                         :class="{ '-rotate-90': !isIndustryExpanded(g.industry) }" />
            <span class="inline-block h-2 w-2 rounded-full"
                  :style="{ background: industryColor(g.industry) }"></span>
            <span>{{ industryLabel(g.industry) }}</span>
            <span class="ml-auto rounded bg-gray-100 px-1.5 py-0.5 text-[10px] text-gray-500">
              {{ g.total }}
            </span>
          </button>

          <!-- entity-type subgroups -->
          <div v-if="isIndustryExpanded(g.industry)" class="pb-1">
            <div v-for="et in ENTITY_ORDER" :key="et">
              <div v-if="g.byEntity[et].length" class="">
                <button
                  class="flex w-full items-center gap-2 px-5 py-1.5 text-left text-[11px] text-gray-500 hover:bg-gray-50"
                  @click="toggleSubgroup(g.industry, et)"
                >
                  <ChevronDown class="h-3 w-3 transition-transform"
                               :class="{ '-rotate-90': !isSubgroupExpanded(g.industry, et) }" />
                  <component :is="entityIcon(et)" class="h-3 w-3" :class="entityIconColor(et)" />
                  <span>{{ ENTITY_LABELS[et] }}</span>
                  <span class="ml-auto text-[10px] text-gray-400">{{ g.byEntity[et].length }}</span>
                </button>
                <div v-if="isSubgroupExpanded(g.industry, et)">
                  <button
                    v-for="t in g.byEntity[et]" :key="t.typeCode"
                    class="group flex w-full items-center gap-2 border-l-2 px-7 py-1.5 text-left text-xs hover:bg-blue-50/50"
                    :class="[
                      isSelected(t) ? 'border-blue-500 bg-blue-50 text-blue-700' : 'border-transparent text-gray-700',
                      isPluginDisabled(t) ? 'opacity-50' : '',
                    ]"
                    @click="$emit('select', t)"
                  >
                    <div class="min-w-0 flex-1 truncate">
                      <div class="truncate font-medium">{{ t.typeName }}</div>
                      <div class="truncate font-mono text-[10px] text-gray-400">{{ t.typeCode }}</div>
                    </div>
                    <span v-if="hasOverride(t)"
                          class="flex-shrink-0 rounded-full bg-indigo-100 px-1 py-0.5 text-[9px] font-semibold text-indigo-700"
                          title="管理员覆写: 插件重启不会覆盖">编辑</span>
                    <span v-if="!t.isPluginRegistered"
                          class="flex-shrink-0 rounded-full bg-amber-100 px-1 py-0.5 text-[9px] text-amber-700"
                          title="管理员自创">自</span>
                    <span v-if="isPluginDisabled(t)"
                          class="flex-shrink-0 rounded-full bg-orange-100 px-1 py-0.5 text-[9px] text-orange-600"
                          title="插件已禁用">禁</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="groups.length === 0" class="py-10 text-center text-sm text-gray-400">
          {{ keyword ? '没有匹配的类型' : '暂无类型配置' }}
        </div>
      </template>
    </div>

    <!-- 底部：新建 -->
    <div class="border-t border-gray-200 p-2">
      <button
        class="flex h-8 w-full items-center justify-center gap-1.5 rounded-md bg-blue-600 text-xs font-medium text-white hover:bg-blue-700"
        @click="$emit('create')"
      >
        <Plus class="h-3.5 w-3.5" />
        新建类型
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Search, Loader2, ChevronDown, Plus, Building2, MapPin, User } from 'lucide-vue-next'
import type { EntityTypeConfig } from '@/api/entityType'

interface Props {
  types: EntityTypeConfig[]
  loading: boolean
  selectedId: LongId | null
}

const props = defineProps<Props>()
defineEmits<{
  select: [t: EntityTypeConfig]
  create: []
}>()

const keyword = ref('')
const onlyCustom = ref(false)
const onlyDisabled = ref(false)

const ENTITY_ORDER = ['ORG_UNIT', 'PLACE', 'USER']
const ENTITY_LABELS: Record<string, string> = {
  ORG_UNIT: '组织类型', PLACE: '场所类型', USER: '用户类型',
}
function entityIcon(et: string) {
  return { ORG_UNIT: Building2, PLACE: MapPin, USER: User }[et] || Building2
}
function entityIconColor(et: string) {
  return { ORG_UNIT: 'text-blue-500', PLACE: 'text-emerald-500', USER: 'text-amber-500' }[et] || 'text-gray-500'
}

const INDUSTRY_LABELS: Record<string, string> = {
  CORE: '通用核心', EDU: '教育行业', HEALTH: '医疗行业', CARE: '养老行业', CUSTOM: '自定义',
}
const INDUSTRY_ORDER = ['CORE', 'EDU', 'HEALTH', 'CARE', 'CUSTOM']
function industryLabel(code: string) { return INDUSTRY_LABELS[code] || code }
function industryColor(code: string) {
  return ({ CORE: '#2563eb', EDU: '#d97706', HEALTH: '#be185d', CARE: '#059669', CUSTOM: '#6b7280' } as Record<string, string>)[code] || '#6b7280'
}

function isPluginDisabled(t: EntityTypeConfig): boolean {
  const v = t?.pluginEnabled
  return v === false || v === 0
}

function hasOverride(t: EntityTypeConfig): boolean {
  if (!t.isPluginRegistered) return false
  const raw = t.overriddenFields
  if (!raw) return false
  try {
    const arr = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(arr) && arr.length > 0
  } catch {
    return false
  }
}

function isSelected(t: EntityTypeConfig): boolean {
  return t.id != null && String(t.id) === String(props.selectedId)
}

const expandedIndustries = ref<Set<string>>(new Set(['CORE', 'EDU', 'HEALTH', 'CARE', 'CUSTOM']))
const expandedSubgroups = ref<Set<string>>(new Set())

function isIndustryExpanded(ind: string) { return expandedIndustries.value.has(ind) }
function toggleIndustry(ind: string) {
  if (expandedIndustries.value.has(ind)) expandedIndustries.value.delete(ind)
  else expandedIndustries.value.add(ind)
}
function subKey(ind: string, et: string) { return ind + ':' + et }
function isSubgroupExpanded(ind: string, et: string) {
  // default: expand if industry has <= 3 entity-type subgroups
  return !expandedSubgroups.value.has('CLOSED:' + subKey(ind, et))
}
function toggleSubgroup(ind: string, et: string) {
  const k = 'CLOSED:' + subKey(ind, et)
  if (expandedSubgroups.value.has(k)) expandedSubgroups.value.delete(k)
  else expandedSubgroups.value.add(k)
}

interface Group {
  industry: string
  total: number
  byEntity: Record<string, EntityTypeConfig[]>
}

const groups = computed<Group[]>(() => {
  const kw = keyword.value.trim().toLowerCase()
  const filtered = props.types.filter(t => {
    if (onlyCustom.value && t.isPluginRegistered) return false
    if (onlyDisabled.value && !isPluginDisabled(t)) return false
    if (kw) {
      const name = (t.typeName || '').toLowerCase()
      const code = (t.typeCode || '').toLowerCase()
      if (!name.includes(kw) && !code.includes(kw)) return false
    }
    return true
  })

  const byInd: Record<string, Record<string, EntityTypeConfig[]>> = {}
  for (const t of filtered) {
    const ind = t.industry || 'CUSTOM'
    if (!byInd[ind]) byInd[ind] = { ORG_UNIT: [], PLACE: [], USER: [] }
    if (byInd[ind][t.entityType]) byInd[ind][t.entityType].push(t)
  }

  const out: Group[] = []
  for (const ind of INDUSTRY_ORDER) {
    if (byInd[ind]) {
      const total = Object.values(byInd[ind]).reduce((s, arr) => s + arr.length, 0)
      if (total > 0) out.push({ industry: ind, total, byEntity: byInd[ind] })
    }
  }
  for (const [ind, v] of Object.entries(byInd)) {
    if (!INDUSTRY_ORDER.includes(ind)) {
      const total = Object.values(v).reduce((s, arr) => s + arr.length, 0)
      if (total > 0) out.push({ industry: ind, total, byEntity: v })
    }
  }
  return out
})
</script>
