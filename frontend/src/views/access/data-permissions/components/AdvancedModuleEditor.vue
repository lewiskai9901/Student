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
                <el-select
                  v-if="module.typeEntity"
                  :model-value="getTypeFilter(module.code)"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  clearable
                  size="small"
                  placeholder="全部类型"
                  :disabled="module.pluginEnabled === false"
                  style="width: 132px"
                  title="按类型过滤 (与组织范围 AND 组合); 不选=不限类型"
                  @update:model-value="(v: any) => onTypeFilterChange(module.code, v as string[])"
                >
                  <el-option
                    v-for="t in (typeOptions[module.typeEntity] || [])"
                    :key="t.code"
                    :label="t.name"
                    :value="t.code"
                  />
                </el-select>
                <button
                  class="flex h-7 items-center gap-1 rounded px-2 text-[11px] font-medium transition"
                  :class="
                    expandedModule === module.code
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-50 text-gray-500 hover:bg-blue-50 hover:text-blue-600'
                  "
                  :title="'展开高级范围生成器 (组织锚点 / 关系过滤 / 类型过滤)'"
                  @click="toggleExpand(module.code)"
                >
                  <span v-if="getScope(module.code) === 'CUSTOM'">{{ getCustomCount(module.code) }} 项</span>
                  <span v-else-if="axisSummary(module)">{{ axisSummary(module) }}</span>
                  <span v-else>展开</span>
                  <ChevronDown
                    class="h-3 w-3 transition-transform"
                    :class="{ 'rotate-180': expandedModule === module.code }"
                  />
                </button>
              </div>
            </div>

            <!-- ▸ 展开: 可组合三轴范围生成器 (设计 §6) -->
            <div
              v-if="expandedModule === module.code"
              class="space-y-3 border-t border-gray-100 bg-gray-50/40 p-3"
            >
              <!-- ① 组织锚点 -->
              <div class="space-y-1.5">
                <div class="flex items-center gap-1.5 text-[11px] font-semibold text-gray-600">
                  <span class="inline-flex h-4 w-4 items-center justify-center rounded-full bg-blue-100 text-[9px] text-blue-600">1</span>
                  组织锚点
                </div>
                <div class="flex flex-wrap items-center gap-2 pl-5">
                  <span class="text-[11px] text-gray-500">从</span>
                  <el-select
                    :model-value="getOrgAnchor(module.code)"
                    size="small"
                    :disabled="module.pluginEnabled === false"
                    style="width: 168px"
                    @update:model-value="(v: any) => onOrgAnchorChange(module.code, v as string)"
                  >
                    <el-option label="全部组织" value="ALL" />
                    <el-option label="不锚定组织 (仅本人)" value="SELF" />
                    <el-option label="我的主属组织" value="PRIMARY_ORG" />
                    <el-option label="按关系派生的组织" value="RELATION" />
                    <el-option label="指定组织" value="CUSTOM_ORG" />
                  </el-select>
                  <!-- RELATION: 选关系 -->
                  <template v-if="getOrgAnchor(module.code) === 'RELATION'">
                    <span class="text-[11px] text-gray-500">我</span>
                    <el-select
                      :model-value="getAnchorParam(module.code)"
                      size="small"
                      placeholder="选择关系"
                      :disabled="module.pluginEnabled === false"
                      style="width: 140px"
                      @update:model-value="(v: any) => onAnchorParamChange(module.code, v as string)"
                    >
                      <el-option
                        v-for="r in orgAnchorRelations"
                        :key="r.relationCode"
                        :label="r.relationName"
                        :value="r.relationCode"
                      />
                    </el-select>
                    <span class="text-[11px] text-gray-500">的组织</span>
                  </template>
                  <!-- 含下级 子树 (锚定到组织时才有意义) -->
                  <el-checkbox
                    v-if="['PRIMARY_ORG', 'RELATION', 'CUSTOM_ORG'].includes(getOrgAnchor(module.code))"
                    :model-value="getIncludeSubtree(module.code)"
                    size="small"
                    :disabled="module.pluginEnabled === false"
                    @update:model-value="(v: any) => onIncludeSubtreeChange(module.code, !!v)"
                  >
                    <span class="text-[11px]">含下级</span>
                  </el-checkbox>
                </div>
                <!-- CUSTOM_ORG: 组织树选择器 -->
                <div v-if="getOrgAnchor(module.code) === 'CUSTOM_ORG'" class="pl-5">
                  <CustomScopeTreePicker
                    :org-ids="getCustomOrgIds(module.code)"
                    @update:org-ids="v => onCustomOrgIdsChange(module.code, v)"
                  />
                </div>
              </div>

              <!-- ② 关系过滤 (仅 relationFilterable 资源) -->
              <div v-if="module.relationFilterable" class="space-y-1.5">
                <div class="flex items-center gap-1.5 text-[11px] font-semibold text-gray-600">
                  <span class="inline-flex h-4 w-4 items-center justify-center rounded-full bg-amber-100 text-[9px] text-amber-600">2</span>
                  关系过滤
                  <span class="font-normal text-gray-400">— 按主体与组织的关系再次收窄</span>
                </div>
                <div class="flex flex-wrap items-center gap-3 pl-5">
                  <el-radio-group
                    :model-value="getRelFilterMode(module.code)"
                    size="small"
                    :disabled="module.pluginEnabled === false"
                    @update:model-value="(v: any) => onRelFilterModeChange(module.code, v as string)"
                  >
                    <el-radio-button value="NONE">不限</el-radio-button>
                    <el-radio-button value="INCLUDE">仅</el-radio-button>
                    <el-radio-button value="EXCLUDE">排除</el-radio-button>
                  </el-radio-group>
                  <el-select
                    v-if="getRelFilterMode(module.code) !== 'NONE'"
                    :model-value="getRelFilterValues(module.code)"
                    multiple
                    collapse-tags
                    collapse-tags-tooltip
                    size="small"
                    placeholder="选择关系"
                    :disabled="module.pluginEnabled === false"
                    style="width: 200px"
                    @update:model-value="(v: any) => onRelFilterValuesChange(module.code, v as string[])"
                  >
                    <el-option
                      v-for="r in subjectFilterRelations(module)"
                      :key="r.relationCode"
                      :label="r.relationName"
                      :value="r.relationCode"
                    />
                  </el-select>
                  <span class="text-[11px] text-gray-400">关系的主体</span>
                </div>
              </div>

              <!-- ③ 类型过滤 (仅 typeEntity 资源) — 与上方紧凑控件等价, 展开态完整展示 -->
              <div v-if="module.typeEntity" class="space-y-1.5">
                <div class="flex items-center gap-1.5 text-[11px] font-semibold text-gray-600">
                  <span class="inline-flex h-4 w-4 items-center justify-center rounded-full bg-emerald-100 text-[9px] text-emerald-600">3</span>
                  类型过滤
                  <span class="font-normal text-gray-400">— 不选=不限类型</span>
                </div>
                <div class="pl-5">
                  <el-select
                    :model-value="getTypeFilter(module.code)"
                    multiple
                    collapse-tags
                    collapse-tags-tooltip
                    clearable
                    size="small"
                    placeholder="全部类型"
                    :disabled="module.pluginEnabled === false"
                    style="width: 260px"
                    @update:model-value="(v: any) => onTypeFilterChange(module.code, v as string[])"
                  >
                    <el-option
                      v-for="t in (typeOptions[module.typeEntity] || [])"
                      :key="t.code"
                      :label="t.name"
                      :value="t.code"
                    />
                  </el-select>
                </div>
              </div>
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
import { ref, computed, watch } from 'vue'
import { AlertTriangle, Settings, ChevronDown, Lock, Sparkles } from 'lucide-vue-next'
import CustomScopeTreePicker from './CustomScopeTreePicker.vue'
import type { ModulePermission, ScopeItem, DataScopeOption } from '@/types/access'
import { entityTypeApi } from '@/api/entityType'
import { relationTypeApi, type RelationTypeDef } from '@/api/relationType'

export interface ModuleGroupItem {
  code: string
  name: string
  industry: string
  pluginEnabled: boolean
  /** 本模块支持的 scope 代码数组; null/undefined 表示默认全集 */
  allowedScopes?: string[] | null
  /** 类型过滤字段(后端 SQL 用); 非空表示该资源支持"按类型过滤" */
  typeField?: string | null
  /** 类型选项来源实体 USER/PLACE/ORG_UNIT — 决定类型多选的候选项 */
  typeEntity?: string | null
  /** 轴② 关系过滤能力: true 表示该资源支持"按主体关系过滤"(如 user 资源排除管理者) */
  relationFilterable?: boolean
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
  // preset 是快捷路径: 选定后派生隐含三轴, 让展开视图与之一致 (用户随后可在展开里覆盖)
  const patch: Partial<ModulePermission> = {
    scopeCode,
    orgAnchor: presetToAnchor(scopeCode) as ModulePermission['orgAnchor'],
    includeSubtree: scopeCode === 'DEPARTMENT_AND_BELOW',
  }
  if (scopeCode !== 'CUSTOM') {
    patch.scopeItems = []
    patch.customOrgIds = []
  } else {
    expandedModule.value = code
  }
  updateModulePermission(code, patch)
}

// ── 类型过滤(闸2/2b): 候选类型按 typeEntity 懒加载, 与组织范围 AND 组合 ──
const typeOptions = ref<Record<string, { code: string; name: string }[]>>({})

async function ensureTypeOptions(entity: string | null | undefined) {
  if (!entity || typeOptions.value[entity]) return
  // 占位避免并发重复请求
  typeOptions.value = { ...typeOptions.value, [entity]: [] }
  try {
    const list = await entityTypeApi.list(entity)
    typeOptions.value = {
      ...typeOptions.value,
      [entity]: (list || []).map(t => ({ code: t.typeCode, name: t.typeName || t.typeCode })),
    }
  } catch {
    /* 拉取失败 → 保持空, 控件显示"全部类型"占位 */
  }
}

watch(
  () => [props.groupedModules, props.advancedGroupedModules] as const,
  () => {
    const entities = new Set<string>()
    for (const list of Object.values(props.groupedModules)) {
      for (const m of list) if (m.typeEntity) entities.add(m.typeEntity)
    }
    for (const list of Object.values(props.advancedGroupedModules ?? {})) {
      for (const m of list) if (m.typeEntity) entities.add(m.typeEntity)
    }
    entities.forEach(ensureTypeOptions)
  },
  { immediate: true }
)

function getTypeFilter(code: string): string[] {
  return props.modulePermissions.find(p => p.moduleCode === code)?.typeFilter ?? []
}

function onTypeFilterChange(code: string, vals: string[]) {
  updateModulePermission(code, { typeFilter: vals })
}

// ── 关系字典 (轴①锚点 + 轴②过滤): 从 relation_types 数据驱动, 无任何行业硬编码 ──
const allRelations = ref<RelationTypeDef[]>([])

async function loadRelations() {
  try {
    allRelations.value = (await relationTypeApi.list()) || []
  } catch {
    allRelations.value = []
  }
}
loadRelations()

/** 轴① "我[关系]的组织" 候选: 指向组织单元的关系 (如 admin/member 等 user→ORG_UNIT) */
const orgAnchorRelations = computed<RelationTypeDef[]>(() =>
  allRelations.value.filter(r => (r.toType || '').toUpperCase() === 'ORG_UNIT')
)

/** 轴② 主体关系过滤候选: 指向本模块类型实体的关系; 无匹配时回退全部 (后端解释关系码) */
function subjectFilterRelations(module: ModuleGroupItem): RelationTypeDef[] {
  const entity = (module.typeEntity || '').toUpperCase()
  if (!entity) return allRelations.value
  const matched = allRelations.value.filter(r => (r.toType || '').toUpperCase() === entity)
  return matched.length ? matched : allRelations.value
}

// ── 轴① 组织锚点 getter/setter ──
function getOrgAnchor(code: string): string {
  const mp = props.modulePermissions.find(p => p.moduleCode === code)
  if (mp?.orgAnchor) return mp.orgAnchor
  // 无显式轴① → 由 scopeCode (preset) 推导, 保证回显一致
  return presetToAnchor(mp?.scopeCode || getScope(code))
}

/** preset scopeCode → 隐含的组织锚点 (preset 是快捷路径, 展开后映射到三轴) */
function presetToAnchor(scope: string): string {
  switch (scope) {
    case 'ALL':
      return 'ALL'
    case 'SELF':
      return 'SELF'
    case 'DEPARTMENT':
    case 'DEPARTMENT_AND_BELOW':
      return 'PRIMARY_ORG'
    case 'CUSTOM':
      return 'CUSTOM_ORG'
    default:
      return 'SELF'
  }
}

function getAnchorParam(code: string): string {
  return props.modulePermissions.find(p => p.moduleCode === code)?.anchorParam || ''
}

function getIncludeSubtree(code: string): boolean {
  const mp = props.modulePermissions.find(p => p.moduleCode === code)
  if (mp?.includeSubtree != null) return mp.includeSubtree
  // 无显式值 → 从 preset 推导 (DEPARTMENT_AND_BELOW 含子树)
  return (mp?.scopeCode || getScope(code)) === 'DEPARTMENT_AND_BELOW'
}

function getCustomOrgIds(code: string): (number | string)[] {
  const mp = props.modulePermissions.find(p => p.moduleCode === code)
  if (mp?.customOrgIds?.length) return mp.customOrgIds
  // 回退: 兼容旧 CUSTOM scopeItems
  return mp?.scopeItems?.map(i => i.scopeId) || []
}

function onOrgAnchorChange(code: string, anchor: string) {
  const patch: Partial<ModulePermission> = { orgAnchor: anchor as ModulePermission['orgAnchor'] }
  // 维持 scopeCode (preset) 与轴① 大致一致, 后端有轴①时优先用轴①
  if (anchor === 'ALL') patch.scopeCode = 'ALL'
  else if (anchor === 'SELF') patch.scopeCode = 'SELF'
  else if (anchor === 'CUSTOM_ORG') patch.scopeCode = 'CUSTOM'
  else patch.scopeCode = 'DEPARTMENT'
  if (anchor !== 'RELATION' && anchor !== 'PLUGIN_DIM') patch.anchorParam = undefined
  if (anchor !== 'CUSTOM_ORG') patch.customOrgIds = []
  if (anchor === 'SELF' || anchor === 'ALL') patch.includeSubtree = false
  updateModulePermission(code, patch)
}

function onAnchorParamChange(code: string, param: string) {
  updateModulePermission(code, { anchorParam: param })
}

function onIncludeSubtreeChange(code: string, val: boolean) {
  updateModulePermission(code, { includeSubtree: val })
}

function onCustomOrgIdsChange(code: string, ids: (number | string)[]) {
  // 同时维持 scopeItems (preset CUSTOM 向后兼容) 与 customOrgIds (轴①)
  const items: ScopeItem[] = ids.map(id => ({
    itemTypeCode: 'ORG_UNIT',
    scopeId: String(id),
    scopeName: '',
    includeChildren: true,
  }))
  updateModulePermission(code, { customOrgIds: ids, scopeItems: items, orgAnchor: 'CUSTOM_ORG', scopeCode: 'CUSTOM' })
}

// ── 轴② 关系过滤 (include / exclude 互斥三态) ──
function getRelFilterMode(code: string): 'NONE' | 'INCLUDE' | 'EXCLUDE' {
  const mp = props.modulePermissions.find(p => p.moduleCode === code)
  if (mp?.subjectRelExclude?.length) return 'EXCLUDE'
  if (mp?.subjectRelInclude?.length) return 'INCLUDE'
  return 'NONE'
}

function getRelFilterValues(code: string): string[] {
  const mp = props.modulePermissions.find(p => p.moduleCode === code)
  if (mp?.subjectRelExclude?.length) return mp.subjectRelExclude
  if (mp?.subjectRelInclude?.length) return mp.subjectRelInclude
  return []
}

function onRelFilterModeChange(code: string, mode: string) {
  if (mode === 'NONE') {
    updateModulePermission(code, { subjectRelInclude: [], subjectRelExclude: [] })
  } else if (mode === 'INCLUDE') {
    const vals = getRelFilterValues(code)
    updateModulePermission(code, { subjectRelInclude: vals, subjectRelExclude: [] })
  } else {
    const vals = getRelFilterValues(code)
    updateModulePermission(code, { subjectRelExclude: vals, subjectRelInclude: [] })
  }
}

function onRelFilterValuesChange(code: string, vals: string[]) {
  const mode = getRelFilterMode(code)
  if (mode === 'INCLUDE') updateModulePermission(code, { subjectRelInclude: vals, subjectRelExclude: [] })
  else if (mode === 'EXCLUDE') updateModulePermission(code, { subjectRelExclude: vals, subjectRelInclude: [] })
}

/** 行头摘要徽标: 显示已配置的高级轴数量 (有别于 preset 默认) */
function axisSummary(module: ModuleGroupItem): string {
  const mp = props.modulePermissions.find(p => p.moduleCode === module.code)
  if (!mp) return ''
  const parts: string[] = []
  if (mp.typeFilter?.length) parts.push(`类型${mp.typeFilter.length}`)
  if (mp.subjectRelInclude?.length || mp.subjectRelExclude?.length) parts.push('关系')
  if (mp.orgAnchor === 'RELATION' && mp.anchorParam) parts.push('锚点')
  return parts.join('·')
}

function toggleExpand(code: string) {
  expandedModule.value = expandedModule.value === code ? '' : code
}
</script>
