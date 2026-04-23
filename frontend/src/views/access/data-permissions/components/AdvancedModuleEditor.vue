<template>
  <div class="space-y-4">
    <div class="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-700">
      <div class="flex items-start gap-2">
        <AlertTriangle class="mt-0.5 h-3.5 w-3.5 flex-shrink-0" />
        <div>
          高级模式: 为每个模块单独设置 scope. 适合 99% 以外的精细化需求 — 一般用场景模板即可.
          <br />
          <span class="text-amber-600">修改后不会自动反映到上方场景模板, 保存时以此处配置为准.</span>
        </div>
      </div>
    </div>

    <!-- 过滤规则提示 -->
    <div
      v-if="filterMeta?.filtered"
      class="flex items-start gap-2 rounded-md border border-blue-200 bg-blue-50 px-3 py-2 text-xs text-blue-700"
    >
      <Sparkles class="mt-0.5 h-3.5 w-3.5 flex-shrink-0" />
      <div class="flex-1">
        按 <b>{{ filterMeta.filterRule }}</b> 智能筛选 —
        常用 <b>{{ filterMeta.totalRelevant }}</b> 个模块,
        其他 <b>{{ filterMeta.totalAdvanced }}</b> 个跨行业模块折叠在下方.
      </div>
    </div>

    <!-- 快捷: 批量设置 -->
    <div class="flex flex-wrap items-center gap-2 rounded-md bg-gray-50 px-3 py-2">
      <span class="text-xs text-gray-500">批量设置:</span>
      <button
        v-for="opt in quickScopes"
        :key="opt.code"
        class="rounded-full border border-gray-200 bg-white px-2.5 py-0.5 text-[11px] font-medium text-gray-600 hover:border-blue-300 hover:bg-blue-50 hover:text-blue-600"
        @click="$emit('batch-set', opt.code)"
      >
        {{ opt.label }}
      </button>
    </div>

    <!-- 相关模块 (主展示) -->
    <section>
      <div v-if="hasAdvanced" class="mb-2 flex items-center gap-2 border-b border-gray-200 pb-1">
        <Sparkles class="h-3 w-3 text-blue-500" />
        <span class="text-xs font-semibold text-gray-700">相关模块</span>
        <span class="rounded bg-blue-100 px-1.5 py-0.5 text-[10px] font-medium text-blue-700">
          {{ relevantCount }}
        </span>
        <span class="text-[11px] text-gray-400">— 根据角色行业/权限自动筛出</span>
      </div>

      <template v-for="(modules, industry) in groupedModules" :key="industry">
        <div class="mt-2 space-y-2">
          <div class="flex items-center gap-2 pb-1">
            <span
              class="inline-block h-2 w-2 rounded-full"
              :style="{ background: industryColor(industry as string) }"
            ></span>
            <span class="text-xs font-medium text-gray-700">{{ industryLabel(industry as string) }}</span>
            <span class="rounded bg-gray-100 px-1.5 py-0.5 text-[10px] text-gray-500">
              {{ modules.length }}
            </span>
          </div>

          <div
            v-for="module in modules"
            :key="module.code"
            class="rounded-md border bg-white"
            :class="{
              'ring-1 ring-blue-400': expandedModule === module.code,
              'opacity-60 border-gray-200 bg-gray-50': module.pluginEnabled === false,
              'border-gray-200': module.pluginEnabled !== false,
            }"
          >
            <div class="flex items-center justify-between gap-2 px-3 py-2">
              <div class="flex min-w-0 items-center gap-2">
                <Settings class="h-3.5 w-3.5 flex-shrink-0 text-gray-400" />
                <span class="truncate text-sm font-medium text-gray-900">{{ module.name }}</span>
                <span
                  v-if="module.pluginEnabled === false"
                  class="flex-shrink-0 rounded-full bg-gray-100 px-1.5 py-0.5 text-[10px] text-gray-500"
                >
                  <Lock class="inline h-2.5 w-2.5" /> 禁
                </span>
              </div>
              <div class="flex items-center gap-2">
                <select
                  v-if="availableScopesFor(module).length > 1"
                  :value="getScope(module.code)"
                  :disabled="module.pluginEnabled === false"
                  class="h-7 rounded border border-gray-200 bg-white px-2 text-xs text-gray-700 focus:border-blue-500 focus:outline-none disabled:bg-gray-100"
                  @change="onScopeChange(module.code, ($event.target as HTMLSelectElement).value)"
                  :title="scopeCountHint(module)"
                >
                  <option v-for="s in availableScopesFor(module)" :key="s.scopeCode" :value="s.scopeCode">
                    {{ s.scopeName }}
                  </option>
                </select>
                <span
                  v-else
                  class="flex h-7 items-center gap-1 rounded border border-gray-200 bg-gray-50 px-2 text-[11px] text-gray-600"
                  :title="'此模块仅支持单一范围'"
                >
                  <Lock class="h-3 w-3 text-gray-400" />
                  {{ availableScopesFor(module)[0]?.scopeName || '—' }}
                </span>
                <button
                  v-if="getScope(module.code) === 'CUSTOM'"
                  class="flex h-7 items-center gap-1 rounded px-2 text-[11px] font-medium transition"
                  :class="
                    expandedModule === module.code
                      ? 'bg-blue-600 text-white'
                      : 'bg-blue-50 text-blue-600 hover:bg-blue-100'
                  "
                  @click="toggleExpand(module.code)"
                >
                  <span>{{ getCustomCount(module.code) }} 项</span>
                  <ChevronDown
                    class="h-3 w-3 transition-transform"
                    :class="{ 'rotate-180': expandedModule === module.code }"
                  />
                </button>
              </div>
            </div>

            <div
              v-if="expandedModule === module.code && getScope(module.code) === 'CUSTOM'"
              class="border-t border-gray-100 p-3"
            >
              <CustomScopeTreePicker
                :org-ids="getScopeItems(module.code, 'ORG_UNIT')"
                :grade-ids="getScopeItems(module.code, 'GRADE')"
                :class-ids="getScopeItems(module.code, 'CLASS')"
                @update:org-ids="v => onScopeItemsChange(module.code, 'ORG_UNIT', v)"
                @update:grade-ids="v => onScopeItemsChange(module.code, 'GRADE', v)"
                @update:class-ids="v => onScopeItemsChange(module.code, 'CLASS', v)"
              />
            </div>
          </div>
        </div>
      </template>
    </section>

    <!-- 其他模块 (折叠) -->
    <section v-if="hasAdvanced" class="mt-4 border-t border-dashed border-gray-300 pt-3">
      <button
        class="flex w-full items-center justify-between rounded-md bg-gray-50 px-3 py-2 text-xs font-medium text-gray-600 hover:bg-gray-100"
        @click="showAdvanced = !showAdvanced"
      >
        <span class="flex items-center gap-2">
          <ChevronDown
            class="h-3 w-3 transition-transform"
            :class="{ 'rotate-180': showAdvanced }"
          />
          <span>其他模块</span>
          <span class="rounded bg-gray-200 px-1.5 py-0.5 text-[10px] text-gray-600">
            {{ advancedCount }}
          </span>
        </span>
        <span class="text-[11px] text-gray-400">此角色通常无需配置, 跨行业高级场景可展开</span>
      </button>

      <template v-if="showAdvanced">
        <template v-for="(modules, industry) in advancedGroupedModules" :key="'adv-' + industry">
          <div class="mt-3 space-y-2">
            <div class="flex items-center gap-2 pb-1">
              <span
                class="inline-block h-2 w-2 rounded-full opacity-60"
                :style="{ background: industryColor(industry as string) }"
              ></span>
              <span class="text-xs font-medium text-gray-500">{{ industryLabel(industry as string) }}</span>
              <span class="rounded bg-gray-100 px-1.5 py-0.5 text-[10px] text-gray-400">
                {{ modules.length }}
              </span>
            </div>

            <div
              v-for="module in modules"
              :key="'adv-' + module.code"
              class="rounded-md border border-gray-200 bg-gray-50/40"
            >
              <div class="flex items-center justify-between gap-2 px-3 py-2">
                <div class="flex min-w-0 items-center gap-2">
                  <Settings class="h-3.5 w-3.5 flex-shrink-0 text-gray-300" />
                  <span class="truncate text-sm text-gray-600">{{ module.name }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <select
                    v-if="availableScopesFor(module).length > 1"
                    :value="getScope(module.code)"
                    :disabled="module.pluginEnabled === false"
                    class="h-7 rounded border border-gray-200 bg-white px-2 text-xs text-gray-600 focus:border-blue-500 focus:outline-none disabled:bg-gray-100"
                    @change="onScopeChange(module.code, ($event.target as HTMLSelectElement).value)"
                    :title="scopeCountHint(module)"
                  >
                    <option v-for="s in availableScopesFor(module)" :key="s.scopeCode" :value="s.scopeCode">
                      {{ s.scopeName }}
                    </option>
                  </select>
                  <span
                    v-else
                    class="flex h-7 items-center gap-1 rounded border border-gray-200 bg-gray-50 px-2 text-[11px] text-gray-500"
                  >
                    <Lock class="h-3 w-3 text-gray-400" />
                    {{ availableScopesFor(module)[0]?.scopeName || '—' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { AlertTriangle, Settings, ChevronDown, Lock, Sparkles } from 'lucide-vue-next'
import CustomScopeTreePicker from './CustomScopeTreePicker.vue'
import type { ModulePermission, ScopeItem, DataScopeOption } from '@/types/access'

export interface ModuleGroupItem {
  code: string
  name: string
  industry: string
  pluginEnabled: boolean
  /** 本模块支持的 scope 代码数组; null/undefined 表示默认全集 */
  allowedScopes?: string[] | null
}

interface FilterMeta {
  filtered: boolean
  filterRule?: string
  totalRelevant?: number
  totalAdvanced?: number
  roleIndustry?: string
  rolePermModules?: string[]
}

interface Props {
  groupedModules: Record<string, ModuleGroupItem[]>
  advancedGroupedModules?: Record<string, ModuleGroupItem[]>
  filterMeta?: FilterMeta
  modulePermissions: ModulePermission[]
  dataScopeOptions: DataScopeOption[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:module-permissions': [value: ModulePermission[]]
  'batch-set': [code: string]
}>()

const expandedModule = ref<string>('')
const showAdvanced = ref(false)

const relevantCount = computed(() =>
  Object.values(props.groupedModules).reduce((s, l) => s + l.length, 0)
)
const advancedCount = computed(() =>
  Object.values(props.advancedGroupedModules ?? {}).reduce((s, l) => s + l.length, 0)
)
const hasAdvanced = computed(() => advancedCount.value > 0)

const INDUSTRY_LABELS: Record<string, string> = {
  CORE: '通用核心',
  EDU: '教育行业',
  HEALTH: '医疗行业',
  CARE: '养老行业',
  CUSTOM: '自定义',
}
function industryLabel(code: string) {
  return INDUSTRY_LABELS[code] || code
}
function industryColor(code: string) {
  return (
    { CORE: '#2563eb', EDU: '#d97706', HEALTH: '#be185d', CARE: '#059669', CUSTOM: '#6b7280' } as Record<
      string,
      string
    >
  )[code] || '#6b7280'
}

const quickScopes = [
  { code: 'ALL', label: '全部数据' },
  { code: 'DEPARTMENT_AND_BELOW', label: '本部门及以下' },
  { code: 'DEPARTMENT', label: '仅本部门' },
  { code: 'SELF', label: '仅本人' },
]

/**
 * 按模块的 allowedScopes 过滤出可选项.
 * null/空 = 返回全部选项 (兼容旧模块).
 */
function availableScopesFor(module: ModuleGroupItem): DataScopeOption[] {
  const allowed = module.allowedScopes
  if (!allowed || allowed.length === 0) {
    return props.dataScopeOptions
  }
  return props.dataScopeOptions.filter(s => allowed.includes(s.scopeCode))
}

function scopeCountHint(module: ModuleGroupItem): string {
  const n = availableScopesFor(module).length
  const total = props.dataScopeOptions.length
  if (n === total) return ''
  return `此模块支持 ${n} / ${total} 种范围`
}

function getScope(code: string): string {
  return props.modulePermissions.find(p => p.moduleCode === code)?.scopeCode || 'SELF'
}

function getCustomCount(code: string): number {
  return props.modulePermissions.find(p => p.moduleCode === code)?.scopeItems?.length || 0
}

function getScopeItems(code: string, type: string): (number | string)[] {
  const mp = props.modulePermissions.find(p => p.moduleCode === code)
  return mp?.scopeItems?.filter(i => i.itemTypeCode === type).map(i => i.scopeId) || []
}

function updateModulePermission(code: string, patch: Partial<ModulePermission>) {
  const next = [...props.modulePermissions]
  const idx = next.findIndex(p => p.moduleCode === code)
  if (idx > -1) {
    next[idx] = { ...next[idx], ...patch }
  } else {
    next.push({ moduleCode: code, scopeCode: 'SELF', ...patch } as ModulePermission)
  }
  emit('update:module-permissions', next)
}

function onScopeChange(code: string, scopeCode: string) {
  const patch: Partial<ModulePermission> = { scopeCode }
  if (scopeCode !== 'CUSTOM') {
    patch.scopeItems = []
    if (expandedModule.value === code) expandedModule.value = ''
  } else {
    expandedModule.value = code
  }
  updateModulePermission(code, patch)
}

function onScopeItemsChange(
  code: string,
  type: 'ORG_UNIT' | 'GRADE' | 'CLASS',
  ids: (number | string)[]
) {
  const mp = props.modulePermissions.find(p => p.moduleCode === code)
  const others = (mp?.scopeItems || []).filter(i => i.itemTypeCode !== type)
  const added: ScopeItem[] = ids.map(id => ({
    itemTypeCode: type,
    scopeId: id,
    scopeName: '',
    includeChildren: type === 'ORG_UNIT',
  }))
  updateModulePermission(code, { scopeItems: [...others, ...added] })
}

function toggleExpand(code: string) {
  expandedModule.value = expandedModule.value === code ? '' : code
}
</script>
