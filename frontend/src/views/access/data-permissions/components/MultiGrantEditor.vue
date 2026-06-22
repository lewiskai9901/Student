<template>
  <div class="space-y-2">
    <div class="flex items-center gap-1.5 text-[11px] font-semibold text-gray-600">
      <span
        class="inline-flex h-4 w-4 items-center justify-center rounded-full bg-indigo-100 text-[9px] text-indigo-600"
        >∨</span
      >
      多锚点 — 满足<b class="mx-0.5">任一</b>条即可见
      <span class="font-normal text-gray-400">（每行 = 一种"按某关系锚定到某范围"的授予）</span>
    </div>

    <div
      v-for="(g, i) in grants"
      :key="i"
      class="flex flex-wrap items-center gap-2 rounded border border-gray-200 bg-white px-2 py-1.5 pl-5"
    >
      <!-- 关系 -->
      <el-select
        :model-value="g.relation"
        size="small"
        placeholder="关系"
        :disabled="disabled"
        style="width: 130px"
        @update:model-value="(v: any) => patch(i, { relation: v as string })"
      >
        <el-option
          v-for="r in relationOptions"
          :key="r.relationCode"
          :label="relLabel(r.relationCode)"
          :value="r.relationCode"
        />
      </el-select>
      <span class="text-[11px] text-gray-400">→</span>
      <!-- 主体范围 -->
      <el-select
        :model-value="g.subject"
        size="small"
        placeholder="范围"
        :disabled="disabled"
        style="width: 120px"
        @update:model-value="(v: any) => patch(i, { subject: v as SubjectScope })"
      >
        <el-option
          v-for="s in subjectOptions"
          :key="s.value"
          :label="s.label"
          :value="s.value"
        />
      </el-select>
      <!-- 含下级 (锚到组织的范围才有意义) -->
      <el-checkbox
        v-if="subtreeRelevant(g.subject)"
        :model-value="!!g.subtree"
        size="small"
        :disabled="disabled"
        @update:model-value="(v: any) => patch(i, { subtree: !!v })"
      >
        <span class="text-[11px]">含下级</span>
      </el-checkbox>
      <!-- 删除该行 -->
      <button
        class="ml-auto flex h-5 w-5 items-center justify-center rounded text-gray-400 hover:bg-red-50 hover:text-red-500 disabled:opacity-40"
        :disabled="disabled || grants.length <= 1"
        :title="grants.length <= 1 ? '至少保留一条' : '删除该锚点'"
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
      <Plus class="h-3 w-3" /> 添加锚点
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { X, Plus } from 'lucide-vue-next'
import { dataPermissionApi, type ResourceRelationOption } from '@/api/access'
import type { RelationGrant, SubjectScope } from '@/types/access'

const props = defineProps<{
  /** 该例外资源的模块码 (拉取可锚定关系) */
  moduleCode: string
  /** 当前多 grant 数组 */
  modelValue: RelationGrant[]
  disabled?: boolean
}>()
const emit = defineEmits<{ 'update:model-value': [RelationGrant[]] }>()

// 本地副本 (≥1 条保证)
const grants = ref<RelationGrant[]>(
  props.modelValue?.length ? props.modelValue.map(g => ({ ...g })) : [{ relation: 'creator', subject: 'SELF' }]
)
watch(
  () => props.modelValue,
  v => {
    if (v && JSON.stringify(v) !== JSON.stringify(grants.value)) {
      grants.value = v.length ? v.map(g => ({ ...g })) : [{ relation: 'creator', subject: 'SELF' }]
    }
  }
)

// ── 关系选项 (数据驱动: resource_relations) ──────────────────
const relationOptions = ref<ResourceRelationOption[]>([])
async function loadRelations() {
  try {
    relationOptions.value = (await dataPermissionApi.getResourceRelations(props.moduleCode)) || []
  } catch {
    relationOptions.value = []
  }
}
watch(() => props.moduleCode, loadRelations, { immediate: true })

// 关系码 → 可读标签 (通用核心关系; 其余原样显示)
const REL_LABELS: Record<string, string> = {
  creator: '创建者',
  owner_org: '所属组织',
  reviewer: '复核',
  inspected: '受检',
  member: '成员',
  admin: '管理',
}
function relLabel(code: string): string {
  return REL_LABELS[code] || code
}

// ── 主体范围选项 ─────────────────────────────────────────────
const subjectOptions: { value: SubjectScope; label: string }[] = [
  { value: 'SELF', label: '仅本人' },
  { value: 'MY_ORG', label: '本组织' },
  { value: 'ALL', label: '全部' },
]
/** 含下级仅对锚到组织的范围有意义 (MY_ORG)。 */
function subtreeRelevant(subject: SubjectScope): boolean {
  return subject === 'MY_ORG'
}

// ── 增删改 ───────────────────────────────────────────────────
function emitChange() {
  emit('update:model-value', grants.value.map(g => ({ ...g })))
}
function patch(i: number, partial: Partial<RelationGrant>) {
  grants.value[i] = { ...grants.value[i], ...partial }
  // 切到非组织范围时清掉 subtree, 避免脏字段
  if (partial.subject && !subtreeRelevant(partial.subject)) {
    delete grants.value[i].subtree
  }
  emitChange()
}
function addGrant() {
  const firstRel = relationOptions.value[0]?.relationCode || 'creator'
  grants.value.push({ relation: firstRel, subject: 'SELF' })
  emitChange()
}
function removeGrant(i: number) {
  if (grants.value.length <= 1) return
  grants.value.splice(i, 1)
  emitChange()
}
</script>
