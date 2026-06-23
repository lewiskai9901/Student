<template>
  <div class="space-y-2">
    <div class="flex items-center gap-1.5 text-[11px] font-semibold text-gray-600">
      <span
        class="inline-flex h-4 w-4 items-center justify-center rounded-full bg-indigo-100 text-[9px] text-indigo-600"
        >∨</span
      >
      显示范围 — 满足<b class="mx-0.5">任一</b>条即可见
      <span class="font-normal text-gray-400">（每条 = 经某关系关联的数据）</span>
    </div>

    <div
      v-for="(g, i) in grants"
      :key="i"
      class="flex flex-wrap items-center gap-2 rounded border border-gray-200 bg-white px-2 py-1.5 pl-5"
    >
      <span class="text-[11px] text-gray-400">经</span>
      <el-select
        :model-value="keyOf(g)"
        size="small"
        placeholder="选择关系"
        :disabled="disabled"
        style="width: 240px"
        @update:model-value="(v: any) => patch(i, v as string)"
      >
        <el-option
          v-for="o in relationOptions"
          :key="o.key"
          :label="o.label"
          :value="o.key"
        />
      </el-select>
      <!-- 含下级: 仅"和我有X关系的组织"有意义 -->
      <el-checkbox
        v-if="keyIsOrg(keyOf(g))"
        :model-value="!!g.subtree"
        size="small"
        :disabled="disabled"
        @update:model-value="(v: any) => toggleSubtree(i, !!v)"
      >
        <span class="text-[11px]">含下级</span>
      </el-checkbox>
      <button
        class="ml-auto flex h-5 w-5 items-center justify-center rounded text-gray-400 hover:bg-red-50 hover:text-red-500 disabled:opacity-40"
        :disabled="disabled || grants.length <= 1"
        :title="grants.length <= 1 ? '至少保留一条' : '删除该条'"
        @click="removeGrant(i)"
      >
        <X class="h-3 w-3" />
      </button>
    </div>

    <button
      class="flex items-center gap-1 pl-5 text-[11px] text-indigo-600 hover:underline disabled:opacity-40"
      :disabled="disabled"
      @click="addGrant"
    >
      <Plus class="h-3 w-3" /> 添加条件
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { X, Plus } from 'lucide-vue-next'
import { dataPermissionApi, type ResourceRelationOption } from '@/api/access'
import { relationTypeApi, type RelationTypeDef } from '@/api/relationType'
import type { RelationGrant } from '@/types/access'
import { grantToKey, keyToGrant, keyIsOrg, ensureGrants, type RelOption } from '../composables/scopeRelation'

const props = defineProps<{
  /** 该资源模块码 (拉取可锚定的资源关系) */
  moduleCode: string
  /** 当前 grant 数组 (一切皆关系; 空=创建者) */
  modelValue: RelationGrant[]
  disabled?: boolean
}>()
const emit = defineEmits<{ 'update:model-value': [RelationGrant[]] }>()

const grants = ref<RelationGrant[]>(ensureGrants(props.modelValue).map(g => ({ ...g })))
watch(
  () => props.modelValue,
  v => {
    if (JSON.stringify(v) !== JSON.stringify(grants.value)) {
      grants.value = ensureGrants(v).map(g => ({ ...g }))
    }
  }
)

function keyOf(g: RelationGrant): string {
  return grantToKey(g)
}

// ── 关系下拉来源: 特殊 + 用户↔组织关系 + 资源关系(PROVIDER/RECORD) + 维度(保全) ──
const orgRels = ref<RelationTypeDef[]>([])
const resRels = ref<ResourceRelationOption[]>([])

async function loadOrgRelations() {
  try {
    const all = (await relationTypeApi.list()) || []
    orgRels.value = all.filter(r => (r.toType || '').toUpperCase() === 'ORG_UNIT')
  } catch {
    orgRels.value = []
  }
}
async function loadResRelations() {
  try {
    const all = (await dataPermissionApi.getResourceRelations(props.moduleCode)) || []
    // 只取 PROVIDER / RECORD_RELATION (owner_org/creator 已由特殊项/org 关系覆盖)
    resRels.value = all.filter(r => r.storageKind === 'PROVIDER' || r.storageKind === 'RECORD_RELATION')
  } catch {
    resRels.value = []
  }
}
loadOrgRelations()
watch(() => props.moduleCode, loadResRelations, { immediate: true })

// 资源关系/维度的人话标签: 行业词不硬编码进核心 (no-industry-vocab 守护)。
// 资源关系名暂用资源关系端点回传的码 (reviewer/inspected…); 维度仅为"保全已配"显示码即可。
const RES_LABEL: Record<string, string> = { reviewer: '复核', inspected: '受检' }

const relationOptions = computed<RelOption[]>(() => {
  const opts: RelOption[] = [
    { key: 'creator', label: '我创建的', kind: 'creator' },
    { key: 'all', label: '全部（不限组织）', kind: 'all' },
    ...orgRels.value.map(r => ({
      key: 'org:' + r.relationCode,
      label: `和我有「${r.relationName || r.relationCode}」关系的组织`,
      kind: 'org' as const,
      code: r.relationCode,
    })),
    ...resRels.value.map(r => ({
      key: 'res:' + r.relationCode,
      label: (RES_LABEL[r.relationCode] || r.relationCode) +
        (r.storageKind === 'PROVIDER' ? '（由插件解析）' : '（指派）'),
      kind: 'res' as const,
      code: r.relationCode,
    })),
  ]
  // 保全当前 grants 里的 PLUGIN_DIM 维度 (下拉源没有也要能显示/回写, 护金标准 BY_CLASS)
  const seen = new Set(opts.map(o => o.key))
  for (const g of grants.value) {
    const k = grantToKey(g)
    if (k.startsWith('dim:') && !seen.has(k)) {
      const dim = g.subjectParam || ''
      opts.push({ key: k, label: '维度：' + dim, kind: 'dim', code: dim })
      seen.add(k)
    }
  }
  return opts
})

// ── 增删改 ───────────────────────────────────────────────────
function emitChange() {
  emit('update:model-value', grants.value.map(g => ({ ...g })))
}
function patch(i: number, key: string) {
  // 保留原 subtree (若新关系仍是 org 类)
  const subtree = !!grants.value[i].subtree
  grants.value[i] = keyToGrant(key, keyIsOrg(key) ? subtree : false)
  emitChange()
}
function toggleSubtree(i: number, v: boolean) {
  grants.value[i] = { ...grants.value[i], subtree: v }
  emitChange()
}
function addGrant() {
  grants.value.push({ relation: 'creator', subject: 'SELF' })
  emitChange()
}
function removeGrant(i: number) {
  if (grants.value.length <= 1) return
  grants.value.splice(i, 1)
  emitChange()
}
</script>
