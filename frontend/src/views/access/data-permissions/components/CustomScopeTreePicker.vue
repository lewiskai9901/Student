<template>
  <div class="rounded-lg border border-gray-200 bg-gray-50/50 p-3">
    <div class="mb-2 flex items-center justify-between">
      <span class="text-xs text-gray-600">选择可访问的数据范围</span>
      <div class="flex items-center gap-1">
        <button class="action-btn" @click="expandAll">全部展开</button>
        <button class="action-btn" @click="collapseAll">全部折叠</button>
        <button class="action-btn" @click="clearAll">清空</button>
      </div>
    </div>

    <div
      class="max-h-60 overflow-y-auto rounded-md border border-gray-200 bg-white"
      :class="{ 'opacity-60': loading }"
    >
      <div v-if="loading" class="flex items-center justify-center py-6">
        <Loader2 class="h-5 w-5 animate-spin text-gray-400" />
      </div>
      <div v-else-if="treeData.length === 0" class="py-6 text-center text-xs text-gray-400">
        暂无数据
      </div>
      <div v-else class="p-2">
        <div v-for="org in treeData" :key="'org-' + org.id" class="mb-1 last:mb-0">
          <div class="flex items-center gap-1.5 rounded px-1.5 py-1 hover:bg-gray-50">
            <button
              class="flex h-4 w-4 items-center justify-center text-gray-400 hover:text-gray-600"
              @click="toggle('org', org.id)"
            >
              <ChevronDown
                v-if="org.children?.length || org.grades?.length"
                class="h-3.5 w-3.5 transition-transform"
                :class="{ '-rotate-90': !isExpanded('org', org.id) }"
              />
            </button>
            <input
              type="checkbox"
              :checked="orgIds.includes(org.id)"
              class="h-3.5 w-3.5"
              @change="toggleOrg(org.id)"
            />
            <Building2 class="h-3.5 w-3.5 text-blue-500" />
            <span class="text-xs font-medium text-gray-700">{{ org.name }}</span>
            <span class="text-[10px] text-gray-400">组织</span>
          </div>

          <div
            v-if="isExpanded('org', org.id)"
            class="ml-5 border-l border-gray-200 pl-2"
          >
            <template v-if="org.children?.length">
              <div v-for="child in org.children" :key="'org-' + child.id" class="mt-0.5">
                <div class="flex items-center gap-1.5 rounded px-1.5 py-1 hover:bg-gray-50">
                  <div class="h-4 w-4"></div>
                  <input
                    type="checkbox"
                    :checked="orgIds.includes(child.id)"
                    class="h-3.5 w-3.5"
                    @change="toggleOrg(child.id)"
                  />
                  <Building2 class="h-3.5 w-3.5 text-blue-400" />
                  <span class="text-xs text-gray-600">{{ child.name }}</span>
                </div>
              </div>
            </template>

            <template v-if="org.grades?.length">
              <div v-for="grade in org.grades" :key="'grade-' + grade.id" class="mt-0.5">
                <div class="flex items-center gap-1.5 rounded px-1.5 py-1 hover:bg-gray-50">
                  <button
                    class="flex h-4 w-4 items-center justify-center text-gray-400 hover:text-gray-600"
                    @click="toggle('grade', grade.id)"
                  >
                    <ChevronDown
                      v-if="grade.classes?.length"
                      class="h-3.5 w-3.5 transition-transform"
                      :class="{ '-rotate-90': !isExpanded('grade', grade.id) }"
                    />
                  </button>
                  <input
                    type="checkbox"
                    :checked="gradeIds.includes(grade.id)"
                    class="h-3.5 w-3.5"
                    @change="toggleGrade(grade.id)"
                  />
                  <GraduationCap class="h-3.5 w-3.5 text-green-500" />
                  <span class="text-xs text-gray-600">{{ grade.name }}</span>
                  <span class="text-[10px] text-gray-400">年级</span>
                </div>
                <div
                  v-if="isExpanded('grade', grade.id) && grade.classes?.length"
                  class="ml-5 border-l border-gray-200 pl-2"
                >
                  <div v-for="cls in grade.classes" :key="'class-' + cls.id" class="mt-0.5">
                    <div class="flex items-center gap-1.5 rounded px-1.5 py-1 hover:bg-gray-50">
                      <div class="h-4 w-4"></div>
                      <input
                        type="checkbox"
                        :checked="classIds.includes(cls.id)"
                        class="h-3.5 w-3.5"
                        @change="toggleClass(cls.id)"
                      />
                      <Users class="h-3.5 w-3.5 text-orange-500" />
                      <span class="text-xs text-gray-600">{{ cls.name }}</span>
                      <span class="text-[10px] text-gray-400">班级</span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计 -->
    <div class="mt-2 flex items-center justify-between rounded bg-blue-50 px-2 py-1.5">
      <div class="flex items-center gap-3 text-[11px]">
        <span class="flex items-center gap-1">
          <Building2 class="h-3 w-3 text-blue-500" />
          <span class="text-gray-600">组织:</span>
          <span class="font-semibold text-blue-600">{{ orgIds.length }}</span>
        </span>
        <span class="flex items-center gap-1">
          <GraduationCap class="h-3 w-3 text-green-500" />
          <span class="text-gray-600">年级:</span>
          <span class="font-semibold text-green-600">{{ gradeIds.length }}</span>
        </span>
        <span class="flex items-center gap-1">
          <Users class="h-3 w-3 text-orange-500" />
          <span class="text-gray-600">班级:</span>
          <span class="font-semibold text-orange-600">{{ classIds.length }}</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, watch, onMounted } from 'vue'
import { Loader2, ChevronDown, Building2, GraduationCap, Users } from 'lucide-vue-next'
import { getOrgUnitTree, getAllCohorts, getAllClasses } from '@/api/organization'

interface UnifiedNode {
  id: LongId | string
  name: string
  children?: UnifiedNode[]
  grades?: { id: LongId | string; name: string; classes?: { id: LongId | string; name: string }[] }[]
}

interface Props {
  orgIds: (number | string)[]
  gradeIds: (number | string)[]
  classIds: (number | string)[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:orgIds': [value: (number | string)[]]
  'update:gradeIds': [value: (number | string)[]]
  'update:classIds': [value: (number | string)[]]
}>()

const loading = ref(false)
const treeData = ref<UnifiedNode[]>([])
const expanded = ref<Set<string>>(new Set())

function isExpanded(type: 'org' | 'grade', id: LongId | string): boolean {
  return expanded.value.has(`${type}-${id}`)
}

function toggle(type: 'org' | 'grade', id: LongId | string) {
  const key = `${type}-${id}`
  if (expanded.value.has(key)) expanded.value.delete(key)
  else expanded.value.add(key)
}

function toggleOrg(id: LongId | string) {
  const arr = [...props.orgIds]
  const idx = arr.findIndex(x => String(x) === String(id))
  if (idx > -1) arr.splice(idx, 1)
  else arr.push(id)
  emit('update:orgIds', arr)
}

function toggleGrade(id: LongId | string) {
  const arr = [...props.gradeIds]
  const idx = arr.findIndex(x => String(x) === String(id))
  if (idx > -1) arr.splice(idx, 1)
  else arr.push(id)
  emit('update:gradeIds', arr)
}

function toggleClass(id: LongId | string) {
  const arr = [...props.classIds]
  const idx = arr.findIndex(x => String(x) === String(id))
  if (idx > -1) arr.splice(idx, 1)
  else arr.push(id)
  emit('update:classIds', arr)
}

function expandAll() {
  const walk = (nodes: UnifiedNode[]) => {
    for (const n of nodes) {
      expanded.value.add(`org-${n.id}`)
      if (n.children) walk(n.children)
      n.grades?.forEach(g => expanded.value.add(`grade-${g.id}`))
    }
  }
  walk(treeData.value)
}

function collapseAll() {
  expanded.value.clear()
}

function clearAll() {
  emit('update:orgIds', [])
  emit('update:gradeIds', [])
  emit('update:classIds', [])
}

async function buildTree() {
  loading.value = true
  try {
    const [orgTree, grades, classes] = await Promise.all([
      getOrgUnitTree(),
      getAllCohorts(),
      getAllClasses(),
    ])

    const gradeMap = new Map<
      number | string,
      { id: LongId | string; name: string; classes: { id: LongId | string; name: string }[] }
    >()
    for (const g of grades) {
      if (g.id) {
        gradeMap.set(g.id, {
          id: g.id,
          name: g.gradeName || `${g.enrollmentYear}级`,
          classes: [],
        })
      }
    }
    for (const c of classes) {
      if (c.gradeId && gradeMap.has(c.gradeId)) {
        gradeMap.get(c.gradeId)!.classes.push({ id: c.id!, name: c.className || '' })
      }
    }

    const build = (org: any): UnifiedNode => ({
      id: org.id,
      name: org.unitName || org.name || org.orgName,
      children: org.children?.map(build),
      grades: Array.from(gradeMap.values()),
    })

    treeData.value = orgTree.map(build)
    treeData.value.forEach(n => expanded.value.add(`org-${n.id}`))
  } catch (e) {
    console.error('加载树结构失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  buildTree()
})

watch(() => props.orgIds, () => {}, { deep: true })
</script>

<style scoped>
.action-btn {
  padding: 2px 6px;
  font-size: 10px;
  color: #6b7280;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  cursor: pointer;
}
.action-btn:hover {
  background: #f9fafb;
}
</style>
