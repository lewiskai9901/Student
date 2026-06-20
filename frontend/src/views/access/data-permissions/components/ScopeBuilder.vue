<template>
  <div class="space-y-3">
    <!-- ① 看哪些组织的数据 -->
    <div class="space-y-1.5">
      <div class="flex items-center gap-1.5 text-[11px] font-semibold text-gray-600">
        <span
          class="inline-flex h-4 w-4 items-center justify-center rounded-full bg-blue-100 text-[9px] text-blue-600"
          >1</span
        >
        看哪些组织的数据
      </div>
      <p class="pl-5 text-[11px] text-gray-400">这个角色能看到哪些组织范围内的数据</p>
      <div class="flex flex-wrap items-center gap-2 pl-5">
        <span class="text-[11px] text-gray-500">从</span>
        <el-select
          :model-value="orgAnchor"
          size="small"
          :disabled="disabled"
          style="width: 188px"
          @update:model-value="(v: any) => onOrgAnchorChange(v as string)"
        >
          <el-option
            v-if="anchorOffered('ALL')"
            label="全部（不限组织）"
            value="ALL"
            title="不限组织，可看到系统里的全部数据"
          />
          <el-option
            v-if="anchorOffered('SELF')"
            label="仅本人（只看自己负责的）"
            value="SELF"
            title="只看与自己直接相关的数据"
          />
          <el-option
            v-if="anchorOffered('PRIMARY_ORG')"
            label="本组织"
            value="PRIMARY_ORG"
            title="只看自己所在组织范围内的数据（即与该组织有成员/归属关系的数据）"
          />
          <el-option
            v-if="anchorOffered('RELATION')"
            label="我负责的组织（按关系）"
            value="RELATION"
            title="例如：我管理的组织 → 只看我作为管理者的那些组织"
          />
          <el-option
            v-if="anchorOffered('CUSTOM_ORG')"
            label="指定组织"
            value="CUSTOM_ORG"
            title="手动勾选要看的组织"
          />
        </el-select>
        <!-- RELATION: 选关系 -->
        <template v-if="orgAnchor === 'RELATION'">
          <span class="text-[11px] text-gray-500">我</span>
          <el-select
            :model-value="anchorParam"
            size="small"
            placeholder="选择关系"
            :disabled="disabled"
            style="width: 140px"
            @update:model-value="(v: any) => onAnchorParamChange(v as string)"
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
        <!-- 含下级组织 子树 (锚定到组织时才有意义, 且 allowedScopes 允许时) -->
        <el-checkbox
          v-if="subtreeRelevant"
          :model-value="includeSubtree"
          size="small"
          :disabled="disabled"
          title="勾选后，该组织下面的子组织数据也一并能看到"
          @update:model-value="(v: any) => onIncludeSubtreeChange(!!v)"
        >
          <span class="text-[11px]">含下级组织</span>
        </el-checkbox>
      </div>
      <!-- CUSTOM_ORG: 组织树选择器 -->
      <div v-if="orgAnchor === 'CUSTOM_ORG'" class="pl-5">
        <CustomScopeTreePicker
          :org-ids="customOrgIds"
          @update:org-ids="v => onCustomOrgIdsChange(v)"
        />
      </div>
    </div>

    <!-- ② 关系过滤 (仅 relationFilterable 资源, 且非 axisOnlyOrg) -->
    <div v-if="showRelationFilter" class="space-y-1.5">
      <div class="flex items-center gap-1.5 text-[11px] font-semibold text-gray-600">
        <span
          class="inline-flex h-4 w-4 items-center justify-center rounded-full bg-amber-100 text-[9px] text-amber-600"
          >2</span
        >
        按与组织的关系再筛选（可选）
        <span class="font-normal text-gray-400">— 在上面的组织范围里，再按人与组织的关系筛一遍</span>
      </div>
      <div class="flex flex-wrap items-center gap-3 pl-5">
        <el-radio-group
          :model-value="relFilterMode"
          size="small"
          :disabled="disabled"
          @update:model-value="(v: any) => onRelFilterModeChange(v as string)"
        >
          <el-radio-button value="NONE" title="不按关系筛选">不限</el-radio-button>
          <el-radio-button value="INCLUDE" title="只看有指定关系的人">仅</el-radio-button>
          <el-radio-button value="EXCLUDE" title="把有指定关系的人排除掉">排除</el-radio-button>
        </el-radio-group>
        <el-select
          v-if="relFilterMode !== 'NONE'"
          :model-value="relFilterValues"
          multiple
          collapse-tags
          collapse-tags-tooltip
          size="small"
          placeholder="选择关系"
          :disabled="disabled"
          style="width: 200px"
          @update:model-value="(v: any) => onRelFilterValuesChange(v as string[])"
        >
          <el-option
            v-for="r in subjectFilterRelations"
            :key="r.relationCode"
            :label="r.relationName"
            :value="r.relationCode"
          />
        </el-select>
        <span v-if="relFilterMode !== 'NONE'" class="text-[11px] text-gray-400">
          {{ relFilterMode === 'EXCLUDE' ? '关系的人（排除这些人）' : '关系的人（只看这些人）' }}
        </span>
      </div>
    </div>

    <!-- ③ 类型过滤 (仅 typeEntity 资源, 且非 axisOnlyOrg) -->
    <div v-if="showTypeFilter" class="space-y-1.5">
      <div class="flex items-center gap-1.5 text-[11px] font-semibold text-gray-600">
        <span
          class="inline-flex h-4 w-4 items-center justify-center rounded-full bg-emerald-100 text-[9px] text-emerald-600"
          >3</span
        >
        限定类型（可选）
        <span class="font-normal text-gray-400">— 不选=不限类型</span>
      </div>
      <div class="pl-5">
        <el-select
          :model-value="typeFilter"
          multiple
          collapse-tags
          collapse-tags-tooltip
          clearable
          size="small"
          placeholder="全部类型"
          :disabled="disabled"
          style="width: 260px"
          @update:model-value="(v: any) => onTypeFilterChange(v as string[])"
        >
          <el-option
            v-for="t in typeOptions"
            :key="t.code"
            :label="t.name"
            :value="t.code"
          />
        </el-select>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import CustomScopeTreePicker from './CustomScopeTreePicker.vue'
import type { OrgAnchor, RelationGrant } from '@/types/access'
import { entityTypeApi } from '@/api/entityType'
import { relationTypeApi, type RelationTypeDef } from '@/api/relationType'

/**
 * 单个数据范围 spec 的 ViewModel (三轴可组合).
 * 取自 ModulePermission 的轴字段子集 — ScopeBuilder 只编辑"一个范围",
 * 不关心它挂在哪个模块 / 是默认还是例外.
 */
export interface ScopeSpecVM {
  /** 轴① 组织锚点 */
  orgAnchor?: OrgAnchor
  /** 轴① 锚点参数: RELATION 时=关系码; PLUGIN_DIM 时=维度码 */
  anchorParam?: string
  /** 轴① 是否含锚定组织的下级 (子树) */
  includeSubtree?: boolean
  /** 轴① CUSTOM_ORG 时的组织单元 id 列表 */
  customOrgIds?: (number | string)[]
  /** 轴② 关系过滤-包含: 仅这些关系的主体 */
  subjectRelInclude?: string[]
  /** 轴② 关系过滤-排除: 排除这些关系的主体 (如"排除管理者") */
  subjectRelExclude?: string[]
  /** 轴③ 类型过滤: 类型码集, 与组织范围 AND 组合; 空=不限 */
  typeFilter?: string[]
  /**
   * R3/R4 多锚点授予 (>1 条)。ScopeBuilder 不编辑它 — 仅承载/透传, 由 DataScopeStudio 例外层
   * 以只读多锚点卡渲染。单 grant / 缺省时为 undefined, 走常规三轴。
   */
  relationGrants?: RelationGrant[]
}

/** 资源能力声明 — 决定哪些轴/锚点可用. 来自 M1 模块列表暴露的能力字段. */
export interface ScopeCapabilities {
  /** 轴② 关系过滤可用 (如 user 资源排除管理者) */
  relationFilterable?: boolean
  /** 轴③ 类型选项来源实体 USER/PLACE/ORG_UNIT; 非空表示资源支持按类型过滤 */
  typeEntity?: string | null
  /** 本资源支持的 preset scope 代码; 用于门控轴①锚点/子树. null=全集 */
  allowedScopes?: string[] | null
}

interface Props {
  modelValue: ScopeSpecVM
  capabilities?: ScopeCapabilities
  /** true 时只渲染轴① (给"默认范围"用) */
  axisOnlyOrg?: boolean
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  capabilities: () => ({}),
  axisOnlyOrg: false,
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: ScopeSpecVM]
}>()

// ──────────────────────────────────────────────────────────
// 轴① 组织锚点 — 派生 getter
// ──────────────────────────────────────────────────────────
const orgAnchor = computed<string>(() => props.modelValue.orgAnchor || 'SELF')
const anchorParam = computed<string>(() => props.modelValue.anchorParam || '')
const includeSubtree = computed<boolean>(() => !!props.modelValue.includeSubtree)
const customOrgIds = computed<(number | string)[]>(() => props.modelValue.customOrgIds || [])

/**
 * allowedScopes (preset 代码) → 该资源可用的锚点/子树映射.
 * null/空 → 全部锚点可用. 否则按 preset 代码推导可见锚点集合.
 */
const ANCHOR_GATE: Record<
  string,
  { anchors: OrgAnchor[]; subtree: boolean }
> = {
  ALL: { anchors: ['ALL'], subtree: false },
  SELF: { anchors: ['SELF'], subtree: false },
  DEPARTMENT: { anchors: ['PRIMARY_ORG'], subtree: false },
  DEPARTMENT_AND_BELOW: { anchors: ['PRIMARY_ORG'], subtree: true },
  MANAGED_ORGS: { anchors: ['RELATION'], subtree: false },
  MANAGED_ORGS_AND_BELOW: { anchors: ['RELATION'], subtree: true },
  CUSTOM: { anchors: ['CUSTOM_ORG'], subtree: true },
}

const ALL_ANCHORS: OrgAnchor[] = ['ALL', 'SELF', 'PRIMARY_ORG', 'RELATION', 'CUSTOM_ORG']

/** 当前资源允许出现的锚点集合 (按 allowedScopes 门控; null=全部). */
const offeredAnchors = computed<Set<OrgAnchor>>(() => {
  const allowed = props.capabilities.allowedScopes
  if (!allowed || allowed.length === 0) return new Set(ALL_ANCHORS)
  const set = new Set<OrgAnchor>()
  for (const code of allowed) {
    const gate = ANCHOR_GATE[code]
    if (gate) gate.anchors.forEach(a => set.add(a))
  }
  // 兜底: 若 allowedScopes 全是未知码, 不至于啥都不给 — 至少给 SELF
  if (set.size === 0) set.add('SELF')
  return set
})

function anchorOffered(a: OrgAnchor): boolean {
  // 当前值始终有对应选项 — 否则 el-select 命中不到 option 会退显原始枚举值(如 "PRIMARY_ORG"),
  // 即使该锚点被 allowedScopes 门控掉(常见: 既有配置的锚点不在资源 allowed_scopes 内)。
  return offeredAnchors.value.has(a) || orgAnchor.value === a
}

/** 含下级(子树)是否相关: 锚定到组织 + allowedScopes 允许子树 (或无门控). */
const subtreeRelevant = computed<boolean>(() => {
  if (!['PRIMARY_ORG', 'RELATION', 'CUSTOM_ORG'].includes(orgAnchor.value)) return false
  const allowed = props.capabilities.allowedScopes
  if (!allowed || allowed.length === 0) return true
  // 任一 allowed preset 声明 subtree=true 即提供"含下级"开关
  return allowed.some(code => ANCHOR_GATE[code]?.subtree)
})

function emitPatch(patch: Partial<ScopeSpecVM>) {
  emit('update:modelValue', { ...props.modelValue, ...patch })
}

function onOrgAnchorChange(anchor: string) {
  const patch: Partial<ScopeSpecVM> = { orgAnchor: anchor as OrgAnchor }
  if (anchor !== 'RELATION' && anchor !== 'PLUGIN_DIM') patch.anchorParam = undefined
  if (anchor !== 'CUSTOM_ORG') patch.customOrgIds = []
  if (anchor === 'SELF' || anchor === 'ALL') patch.includeSubtree = false
  emitPatch(patch)
}

function onAnchorParamChange(param: string) {
  emitPatch({ anchorParam: param })
}

function onIncludeSubtreeChange(val: boolean) {
  emitPatch({ includeSubtree: val })
}

function onCustomOrgIdsChange(ids: (number | string)[]) {
  emitPatch({ customOrgIds: ids, orgAnchor: 'CUSTOM_ORG' })
}

// ──────────────────────────────────────────────────────────
// 关系字典 (轴①锚点候选 + 轴②过滤候选) — 数据驱动, 无行业硬编码
// ──────────────────────────────────────────────────────────
const allRelations = ref<RelationTypeDef[]>([])

async function loadRelations() {
  try {
    allRelations.value = (await relationTypeApi.list()) || []
  } catch {
    allRelations.value = []
  }
}
loadRelations()

/** 轴① "我[关系]的组织" 候选: 指向组织单元的关系 (toType=ORG_UNIT). */
const orgAnchorRelations = computed<RelationTypeDef[]>(() =>
  allRelations.value.filter(r => (r.toType || '').toUpperCase() === 'ORG_UNIT')
)

/** 轴② 主体关系过滤候选: 指向本资源类型实体的关系; 无匹配回退全部 (后端解释关系码). */
const subjectFilterRelations = computed<RelationTypeDef[]>(() => {
  const entity = (props.capabilities.typeEntity || '').toUpperCase()
  if (!entity) return allRelations.value
  const matched = allRelations.value.filter(r => (r.toType || '').toUpperCase() === entity)
  return matched.length ? matched : allRelations.value
})

// ──────────────────────────────────────────────────────────
// 轴② 关系过滤 (include / exclude 互斥三态)
// ──────────────────────────────────────────────────────────
const showRelationFilter = computed(
  () => !props.axisOnlyOrg && !!props.capabilities.relationFilterable
)

const relFilterMode = computed<'NONE' | 'INCLUDE' | 'EXCLUDE'>(() => {
  if (props.modelValue.subjectRelExclude?.length) return 'EXCLUDE'
  if (props.modelValue.subjectRelInclude?.length) return 'INCLUDE'
  return 'NONE'
})

const relFilterValues = computed<string[]>(() => {
  if (props.modelValue.subjectRelExclude?.length) return props.modelValue.subjectRelExclude
  if (props.modelValue.subjectRelInclude?.length) return props.modelValue.subjectRelInclude
  return []
})

function onRelFilterModeChange(mode: string) {
  if (mode === 'NONE') {
    emitPatch({ subjectRelInclude: [], subjectRelExclude: [] })
  } else if (mode === 'INCLUDE') {
    emitPatch({ subjectRelInclude: relFilterValues.value, subjectRelExclude: [] })
  } else {
    emitPatch({ subjectRelExclude: relFilterValues.value, subjectRelInclude: [] })
  }
}

function onRelFilterValuesChange(vals: string[]) {
  if (relFilterMode.value === 'EXCLUDE') {
    emitPatch({ subjectRelExclude: vals, subjectRelInclude: [] })
  } else {
    // INCLUDE (或 NONE→选了值即视作 INCLUDE)
    emitPatch({ subjectRelInclude: vals, subjectRelExclude: [] })
  }
}

// ──────────────────────────────────────────────────────────
// 轴③ 类型过滤 — 候选按 typeEntity 懒加载
// ──────────────────────────────────────────────────────────
const showTypeFilter = computed(() => !props.axisOnlyOrg && !!props.capabilities.typeEntity)

const typeFilter = computed<string[]>(() => props.modelValue.typeFilter || [])
const typeOptions = ref<{ code: string; name: string }[]>([])

async function loadTypeOptions(entity: string | null | undefined) {
  if (!entity) {
    typeOptions.value = []
    return
  }
  try {
    const list = await entityTypeApi.list(entity)
    typeOptions.value = (list || []).map(t => ({
      code: t.typeCode,
      name: t.typeName || t.typeCode,
    }))
  } catch {
    typeOptions.value = []
  }
}

watch(
  () => props.capabilities.typeEntity,
  entity => {
    if (!props.axisOnlyOrg) loadTypeOptions(entity)
  },
  { immediate: true }
)

function onTypeFilterChange(vals: string[]) {
  emitPatch({ typeFilter: vals })
}
</script>
