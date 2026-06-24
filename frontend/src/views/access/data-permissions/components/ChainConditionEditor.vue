<template>
  <div class="space-y-1.5 rounded border border-indigo-200 bg-indigo-50/40 p-2">
    <div class="flex items-center gap-1 text-[11px] font-medium text-indigo-700">
      <Workflow class="h-3 w-3" /> 多级关系链（点节点 / 连线编辑）
    </div>

    <!-- 可视化流图: 我 ─边→ 实体 ─边→ … ─终端→ 数据 -->
    <div class="flex flex-wrap items-center gap-1 pl-1">
      <!-- 起点 -->
      <span class="inline-flex items-center rounded-full bg-indigo-600 px-2 py-0.5 text-[11px] font-medium text-white">我</span>

      <template v-for="(h, i) in hops" :key="i">
        <!-- 边 i: prevType --[关系/方向]--> h.toType -->
        <el-popover trigger="click" :width="290" placement="bottom" :disabled="disabled">
          <template #reference>
            <button type="button" class="inline-flex items-center gap-0.5 rounded border border-indigo-300 bg-white px-1.5 py-0.5 text-[11px] text-indigo-700 hover:bg-indigo-100">
              <span>{{ edgeLabel(i) }}</span>
              <span class="text-indigo-400">→</span>
            </button>
          </template>
          <div class="space-y-2 text-[12px]">
            <div class="font-medium text-gray-700">第 {{ i + 1 }} 跳</div>
            <template v-if="isProjectionEdge(i)">
              <div class="rounded bg-amber-50 p-1.5 text-[11px] text-amber-700">
                场所与组织按「归属」自动关联（无需选关系）。
              </div>
            </template>
            <template v-else>
              <div>
                <div class="mb-0.5 text-gray-400">方向</div>
                <el-radio-group :model-value="h.direction || 'FORWARD'" size="small" :disabled="disabled" @update:model-value="(v: any) => setDir(i, v)">
                  <el-radio-button value="FORWARD">正向</el-radio-button>
                  <el-radio-button value="REVERSE">反向</el-radio-button>
                </el-radio-group>
                <div class="mt-0.5 text-[10px] text-gray-400">{{ (h.direction === 'REVERSE') ? '倒读关系：从到达实体反查' : '正读关系：我/上一级 → 到达实体' }}</div>
              </div>
              <div>
                <div class="mb-0.5 text-gray-400">关系</div>
                <el-select :model-value="h.relations" multiple collapse-tags size="small" placeholder="选关系" style="width: 100%" :disabled="disabled" @update:model-value="(v: any) => setHopRelations(i, v)">
                  <el-option v-for="r in relsForDir(i)" :key="r.relationCode" :label="optionLabel(r, h)" :value="r.relationCode" />
                </el-select>
              </div>
              <div v-if="(h.relations?.length || 0) > 1">
                <div class="mb-0.5 text-gray-400">多关系组合</div>
                <el-select :model-value="h.combine" size="small" style="width: 100%" :disabled="disabled" @update:model-value="(v: any) => setHopField(i, 'combine', v)">
                  <el-option label="满足任一 (OR)" value="OR" />
                  <el-option label="同时满足 (AND)" value="AND" />
                </el-select>
              </div>
            </template>
            <el-checkbox v-if="h.toType === 'org_unit'" :model-value="!!h.subtree" size="small" :disabled="disabled" @update:model-value="(v: any) => setHopField(i, 'subtree', !!v)">
              <span class="text-[11px]">含下级（子树）</span>
            </el-checkbox>
            <button type="button" class="text-[11px] text-red-500 hover:underline disabled:opacity-40" :disabled="disabled || hops.length <= 1" @click="removeHop(i)">删除该跳</button>
          </div>
        </el-popover>

        <!-- 节点: 到达实体 -->
        <el-popover trigger="click" :width="180" placement="bottom" :disabled="disabled">
          <template #reference>
            <button type="button" class="inline-flex items-center gap-0.5 rounded-full border border-gray-300 bg-white px-2 py-0.5 text-[11px] text-gray-700 hover:bg-gray-100">
              {{ typeLabel(h.toType) }}
              <span v-if="h.toType === 'org_unit' && h.subtree" class="text-[10px] text-indigo-500">+下级</span>
            </button>
          </template>
          <div class="space-y-1 text-[12px]">
            <div class="text-gray-400">实体类型</div>
            <el-select :model-value="h.toType" size="small" style="width: 100%" :disabled="disabled" @update:model-value="(v: any) => setHopType(i, v)">
              <el-option label="组织" value="org_unit" />
              <el-option label="场所" value="place" />
              <el-option label="用户" value="user" />
            </el-select>
          </div>
        </el-popover>
      </template>

      <!-- 加一跳 -->
      <button type="button" class="inline-flex h-5 w-5 items-center justify-center rounded-full border border-dashed border-indigo-400 text-indigo-500 hover:bg-indigo-100 disabled:opacity-40" :disabled="disabled" title="加一跳" @click="addHop">
        <Plus class="h-3 w-3" />
      </button>

      <!-- 终端边 -->
      <el-popover trigger="click" :width="300" placement="bottom" :disabled="disabled">
        <template #reference>
          <button type="button" class="inline-flex items-center gap-0.5 rounded border border-emerald-300 bg-white px-1.5 py-0.5 text-[11px] text-emerald-700 hover:bg-emerald-100">
            <span>{{ terminalLabel }}</span>
            <span class="text-emerald-400">→</span>
          </button>
        </template>
        <div class="space-y-2 text-[12px]">
          <div class="font-medium text-gray-700">数据怎么挂到链末实体</div>
          <el-select :model-value="modelValue.relation" size="small" placeholder="终端锚点" style="width: 100%" :disabled="disabled" @update:model-value="(v: any) => setTerminal(v)">
            <el-option v-for="t in terminalOptions" :key="t.code" :label="t.label" :value="t.code" />
          </el-select>
          <template v-if="terminalIsSubjectGraph">
            <div class="text-gray-400">数据与组织的成员关系</div>
            <el-select :model-value="membershipArray" multiple collapse-tags size="small" style="width: 100%" :disabled="disabled" @update:model-value="(v: any) => setMembership(v as string[])">
              <el-option v-for="m in membershipOptions" :key="m.relationCode" :label="m.relationName || m.relationCode" :value="m.relationCode" />
            </el-select>
            <div class="text-[10px] text-gray-400">{{ membershipArray.length > 1 ? '多选 = 同时满足(且)' : '' }}</div>
          </template>
        </div>
      </el-popover>

      <!-- 数据节点 -->
      <span class="inline-flex items-center rounded bg-emerald-600 px-2 py-0.5 text-[11px] font-medium text-white">《数据》</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Plus, Workflow } from 'lucide-vue-next'
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
    : [{ relations: [], combine: 'OR', toType: 'org_unit', subtree: false, direction: 'FORWARD' }]
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

const TYPE_LABEL: Record<string, string> = { user: '用户', org_unit: '组织', place: '场所' }
function typeLabel(t: string): string {
  return TYPE_LABEL[t] || t
}

/** 第 i 跳的起点实体类型: 跳 0 = 我(user); 跳 k = 上一跳 toType。 */
function prevType(i: number): string {
  return i === 0 ? 'user' : hops.value[i - 1].toType
}
/** 场所↔组织跳 (任一方向): 引擎走 effective_org_unit_id 投影, 忽略关系 → 不让选关系。 */
function isProjectionEdge(i: number): boolean {
  const from = prevType(i)
  const to = hops.value[i]?.toType
  return (from === 'place' && to === 'org_unit') || (from === 'org_unit' && to === 'place')
}
/** 第 i 跳按当前方向的可选关系: 正向 from=起点,to=到达; 反向 from=到达,to=起点 (倒读边)。 */
function relsForDir(i: number): RelationTypeDef[] {
  const h = hops.value[i]
  const reverse = h.direction === 'REVERSE'
  const from = (reverse ? h.toType : prevType(i)).toUpperCase()
  const to = (reverse ? prevType(i) : h.toType).toUpperCase()
  return allRelations.value.filter(
    r => (r.fromType || '').toUpperCase() === from && (r.toType || '').toUpperCase() === to
  )
}
/** 选项名: 正向用 relationName; 反向用 reverseName (倒读名), 缺省回退 relationName。 */
function optionLabel(r: RelationTypeDef, h: ChainHop): string {
  return h.direction === 'REVERSE' ? (r.reverseName || r.relationName || r.relationCode) : (r.relationName || r.relationCode)
}
/** 边 chip 文字: 投影边显归属/下辖; 否则当前选中关系的方向感知名 (join), 空显占位。 */
function edgeLabel(i: number): string {
  const h = hops.value[i]
  if (isProjectionEdge(i)) {
    return prevType(i) === 'place' ? '归属组织' : '下辖场所'
  }
  const sel = h.relations || []
  if (!sel.length) return '选关系'
  const defs = relsForDir(i)
  const names = sel.map(code => {
    const d = defs.find(x => x.relationCode === code)
    return d ? optionLabel(d, h) : code
  })
  const joined = names.join(h.combine === 'AND' ? '且' : '/')
  return h.direction === 'REVERSE' ? `${joined}(反)` : joined
}

const TERM_LABEL: Record<string, string> = { owner_org: '组织归属', creator: '我创建' }
/** 终端锚点候选: 列锚/成员图 (owner_org/creator 等; PROVIDER/RECORD 不入链终端)。 */
const terminalOptions = computed(() =>
  resRels.value
    .filter(r => r.storageKind === 'COLUMN' || r.storageKind === 'SUBJECT_GRAPH')
    .map(r => ({ code: r.relationCode, label: TERM_LABEL[r.relationCode] || r.relationCode }))
)
const terminalLabel = computed(() => {
  const code = props.modelValue.relation
  if (!code) return '选终端'
  return TERM_LABEL[code] || code
})

// 选中终端是否成员图 (SUBJECT_GRAPH) → 显示"数据↔组织成员关系"下拉
const terminalIsSubjectGraph = computed(
  () => resRels.value.find(r => r.relationCode === props.modelValue.relation)?.storageKind === 'SUBJECT_GRAPH'
)
/** 数据↔组织成员关系候选 = 用户↔组织关系 (member 属于 / responsible_for 负责 / admin …)。 */
const membershipOptions = computed<RelationTypeDef[]>(() =>
  allRelations.value.filter(
    r => (r.fromType || '').toUpperCase() === 'USER' && (r.toType || '').toUpperCase() === 'ORG_UNIT'
  )
)
const membershipArray = computed<string[]>(() => {
  const p = props.modelValue.subjectParam
  return p ? p.split(',').map(s => s.trim()).filter(Boolean) : ['member']
})
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
/** 改方向 → 候选关系集变 (正/反向边不同) → 清该跳已选关系。 */
function setDir(i: number, v: 'FORWARD' | 'REVERSE') {
  const next = hops.value.map((h, idx) => (idx === i ? { ...h, direction: v, relations: [] } : h))
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
  emitGrant([...hops.value, { relations: [], combine: 'OR', toType: 'org_unit', subtree: false, direction: 'FORWARD' }])
}
function removeHop(i: number) {
  if (hops.value.length <= 1) return
  emitGrant(hops.value.filter((_, idx) => idx !== i))
}
function setTerminal(v: string) {
  emitGrant(hops.value, v)
}
</script>
