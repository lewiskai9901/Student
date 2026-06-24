<template>
  <div class="space-y-1.5 rounded border border-indigo-200 bg-indigo-50/40 p-2">
    <div class="flex items-center gap-1 text-[11px] font-medium text-indigo-700">
      <Workflow class="h-3 w-3" /> 多级关系链：我 → … → 数据
    </div>

    <!-- 中间跳 -->
    <div v-for="(h, i) in hops" :key="i" class="flex flex-wrap items-center gap-1.5 pl-3 text-[11px]">
      <span class="text-gray-500">{{ i === 0 ? '我' : '上一级' }}</span>
      <!-- 场所→组织: 引擎按场所归属(effective_org_unit_id)自动投影, 不读关系 → 不让选关系(否则误导) -->
      <template v-if="isPlaceToOrg(i)">
        <span class="text-gray-500">所属（按场所归属自动）的</span>
      </template>
      <template v-else>
        <span class="text-gray-500">经</span>
        <el-select
          :model-value="h.relations"
          multiple
          collapse-tags
          size="small"
          placeholder="选关系"
          style="width: 200px"
          :disabled="disabled"
          @update:model-value="(v: any) => setHopRelations(i, v)"
        >
          <el-option v-for="r in relsFor(i, h.toType)" :key="r.relationCode" :label="r.relationName || r.relationCode" :value="r.relationCode" />
        </el-select>
        <el-select
          v-if="(h.relations?.length || 0) > 1"
          :model-value="h.combine"
          size="small"
          style="width: 92px"
          :disabled="disabled"
          @update:model-value="(v: any) => setHopField(i, 'combine', v)"
        >
          <el-option label="满足任一" value="OR" />
          <el-option label="同时满足" value="AND" />
        </el-select>
        <span class="text-gray-500">关系的</span>
      </template>
      <el-select
        :model-value="h.toType"
        size="small"
        style="width: 92px"
        :disabled="disabled"
        @update:model-value="(v: any) => setHopType(i, v)"
      >
        <el-option label="组织" value="org_unit" />
        <el-option label="场所" value="place" />
        <el-option label="用户" value="user" />
      </el-select>
      <el-checkbox
        v-if="h.toType === 'org_unit'"
        :model-value="!!h.subtree"
        size="small"
        :disabled="disabled"
        @update:model-value="(v: any) => setHopField(i, 'subtree', !!v)"
      >
        <span class="text-[11px]">含下级</span>
      </el-checkbox>
      <button
        class="flex h-5 w-5 items-center justify-center rounded text-gray-400 hover:bg-red-50 hover:text-red-500 disabled:opacity-40"
        :disabled="disabled || hops.length <= 1"
        :title="hops.length <= 1 ? '至少保留一跳' : '删除该跳'"
        @click="removeHop(i)"
      >
        <X class="h-3 w-3" />
      </button>
    </div>
    <button
      class="flex items-center gap-1 pl-3 text-[11px] text-indigo-600 hover:underline disabled:opacity-40"
      :disabled="disabled"
      @click="addHop"
    >
      <Plus class="h-3 w-3" /> 加一跳
    </button>

    <!-- 终端锚点 -->
    <div class="flex items-center gap-1.5 border-t border-indigo-100 pl-3 pt-1.5 text-[11px]">
      <span class="text-gray-500">数据经</span>
      <el-select
        :model-value="modelValue.relation"
        size="small"
        placeholder="终端锚点"
        style="width: 160px"
        :disabled="disabled"
        @update:model-value="(v: any) => setTerminal(v)"
      >
        <el-option v-for="t in terminalOptions" :key="t.code" :label="t.label" :value="t.code" />
      </el-select>
      <!-- [完成项2] 成员图终端: 可选数据↔组织的成员关系 (属于/负责) -->
      <template v-if="terminalIsSubjectGraph">
        <span class="text-gray-500">·数据与组织</span>
        <el-select
          :model-value="membershipArray"
          multiple
          collapse-tags
          size="small"
          style="width: 180px"
          :disabled="disabled"
          @update:model-value="(v: any) => setMembership(v as string[])"
        >
          <el-option v-for="m in membershipOptions" :key="m.relationCode" :label="m.relationName || m.relationCode" :value="m.relationCode" />
        </el-select>
        <span class="text-gray-500">关系{{ membershipArray.length > 1 ? '(且)' : '' }}</span>
      </template>
      <span v-else class="text-gray-500">挂到末级实体</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { X, Plus, Workflow } from 'lucide-vue-next'
import { dataPermissionApi, type ResourceRelationOption } from '@/api/access'
import { relationTypeApi, type RelationTypeDef } from '@/api/relationType'
import type { RelationGrant, ChainHop } from '@/types/access'

const props = defineProps<{
  moduleCode: string
  /** 当前 grant (hops 非空 = 链; relation = 终端锚点) */
  modelValue: RelationGrant
  disabled?: boolean
}>()
const emit = defineEmits<{ 'update:model-value': [RelationGrant] }>()

const hops = computed<ChainHop[]>(() =>
  props.modelValue.hops && props.modelValue.hops.length
    ? props.modelValue.hops
    : [{ relations: [], combine: 'OR', toType: 'org_unit', subtree: false }]
)

function emitGrant(nextHops: ChainHop[], terminal?: string) {
  emit('update:model-value', {
    ...props.modelValue,
    subject: 'SELF',
    hops: nextHops,
    relation: terminal ?? props.modelValue.relation,
  })
}

// ── 关系字典 (链边) + 终端锚点 (resource_relations) ──
const allRelations = ref<RelationTypeDef[]>([])
const resRels = ref<ResourceRelationOption[]>([])
relationTypeApi.list().then(r => (allRelations.value = r || [])).catch(() => (allRelations.value = []))
watch(
  () => props.moduleCode,
  async code => {
    try {
      resRels.value = (await dataPermissionApi.getResourceRelations(code)) || []
    } catch {
      resRels.value = []
    }
  },
  { immediate: true }
)

/** 第 i 跳的起点实体类型: 跳 0 = 我(user); 跳 k = 上一跳 toType。 */
function prevType(i: number): string {
  return i === 0 ? 'user' : hops.value[i - 1].toType
}
/** 场所→组织跳: 引擎走 effective_org_unit_id 投影忽略关系 → UI 不让选关系。 */
function isPlaceToOrg(i: number): boolean {
  return prevType(i) === 'place' && hops.value[i]?.toType === 'org_unit'
}
/** 第 i 跳可选关系: fromType=起点, toType=该跳到达类型。 */
function relsFor(i: number, toType: string): RelationTypeDef[] {
  const from = prevType(i).toUpperCase()
  const to = (toType || '').toUpperCase()
  return allRelations.value.filter(
    r => (r.fromType || '').toUpperCase() === from && (r.toType || '').toUpperCase() === to
  )
}

const TERM_LABEL: Record<string, string> = { owner_org: '组织归属', creator: '我创建' }
/** 终端锚点候选: 列锚/成员图 (owner_org/creator 等; PROVIDER/RECORD 不入链终端)。 */
const terminalOptions = computed(() =>
  resRels.value
    .filter(r => r.storageKind === 'COLUMN' || r.storageKind === 'SUBJECT_GRAPH')
    .map(r => ({ code: r.relationCode, label: TERM_LABEL[r.relationCode] || r.relationCode }))
)

// [完成项2] 选中终端是否成员图 (SUBJECT_GRAPH) → 显示"数据↔组织成员关系"下拉 (属于/负责)
const terminalIsSubjectGraph = computed(
  () => resRels.value.find(r => r.relationCode === props.modelValue.relation)?.storageKind === 'SUBJECT_GRAPH'
)
/** 数据↔组织成员关系候选 = 用户↔组织关系 (member 属于 / responsible_for 负责 / admin …)。 */
const membershipOptions = computed<RelationTypeDef[]>(() =>
  allRelations.value.filter(
    r => (r.fromType || '').toUpperCase() === 'USER' && (r.toType || '').toUpperCase() === 'ORG_UNIT'
  )
)
/** 当前终端成员关系集 (subjectParam 逗号串 → 数组; 空→[member])。 */
const membershipArray = computed<string[]>(() => {
  const p = props.modelValue.subjectParam
  return p ? p.split(',').map(s => s.trim()).filter(Boolean) : ['member']
})
/** 多选 → subjectParam 逗号串 (多个 = AND 交集"属于且负责"); 空→member。 */
function setMembership(arr: string[]) {
  const v = arr && arr.length ? arr.join(',') : 'member'
  emit('update:model-value', { ...props.modelValue, subject: 'SELF', subjectParam: v })
}

// ── 编辑 ──
function setHopRelations(i: number, v: string[]) {
  const next = hops.value.map((h, idx) => (idx === i ? { ...h, relations: v } : h))
  emitGrant(next)
}
function setHopField(i: number, field: 'combine' | 'subtree', v: any) {
  const next = hops.value.map((h, idx) => (idx === i ? { ...h, [field]: v } : h))
  emitGrant(next)
}
function setHopType(i: number, v: string) {
  // 改到达类型 → 该跳已选关系作废 (fromType→新 toType 重选); 后续跳起点变, 也清其关系
  const next = hops.value.map((h, idx) => {
    if (idx === i) return { ...h, toType: v as ChainHop['toType'], relations: [] }
    if (idx === i + 1) return { ...h, relations: [] }
    return h
  })
  emitGrant(next)
}
function addHop() {
  emitGrant([...hops.value, { relations: [], combine: 'OR', toType: 'org_unit', subtree: false }])
}
function removeHop(i: number) {
  if (hops.value.length <= 1) return
  emitGrant(hops.value.filter((_, idx) => idx !== i))
}
function setTerminal(v: string) {
  emitGrant(hops.value, v)
}
</script>
