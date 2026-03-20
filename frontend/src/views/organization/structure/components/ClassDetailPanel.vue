<template>
  <div class="space-y-4">
    <!-- Header Card -->
    <div class="rounded-xl border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
        <div class="flex items-center gap-3">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-lg"
            :style="typeIconStyle"
          >
            <Users class="h-5 w-5" />
          </div>
          <div>
            <h2 class="text-lg font-semibold text-gray-900">{{ node.unitName }}</h2>
            <div class="flex items-center gap-2 text-xs text-gray-500">
              <code class="rounded bg-gray-100 px-1.5 py-0.5 font-mono">{{ node.unitCode }}</code>
              <span class="text-gray-300">|</span>
              <span
                class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium"
                :style="typeBadgeStyle"
              >
                班级
              </span>
              <span class="text-gray-300">|</span>
              <span :class="statusClass">{{ statusLabel }}</span>
              <template v-if="node.headTeacherName">
                <span class="text-gray-300">|</span>
                <span class="flex items-center gap-1">
                  <User class="h-3 w-3" />{{ node.headTeacherName }}
                </span>
              </template>
              <template v-if="node.enrollmentYear">
                <span class="text-gray-300">|</span>
                <span>{{ node.enrollmentYear }}级</span>
              </template>
              <template v-if="node.studentCount != null">
                <span class="text-gray-300">|</span>
                <span>{{ node.studentCount }}人</span>
              </template>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <button
            class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            @click="emit('edit', node)"
          >
            <Pencil class="h-4 w-4" />
            编辑
          </button>
          <button
            v-if="node.status === 'ACTIVE'"
            class="inline-flex items-center gap-1.5 rounded-lg border border-orange-200 bg-orange-50 px-3 py-2 text-sm font-medium text-orange-700 transition-colors hover:bg-orange-100"
            @click="emit('toggleStatus', node)"
          >
            <Ban class="h-4 w-4" />
            冻结
          </button>
          <button
            v-else-if="node.status === 'FROZEN'"
            class="inline-flex items-center gap-1.5 rounded-lg border border-green-200 bg-green-50 px-3 py-2 text-sm font-medium text-green-700 transition-colors hover:bg-green-100"
            @click="emit('toggleStatus', node)"
          >
            <Check class="h-4 w-4" />
            解冻
          </button>
          <button
            class="inline-flex items-center gap-1.5 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-red-700 transition-colors hover:bg-red-100"
            @click="emit('delete', node)"
          >
            <Trash2 class="h-4 w-4" />
            删除
          </button>
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="rounded-xl border border-gray-200 bg-white">
      <div class="flex border-b border-gray-200">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="[
            'relative px-5 py-3 text-sm font-medium transition-colors',
            activeTab === tab.key
              ? 'text-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          ]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span
            v-if="tab.count !== undefined"
            class="ml-1.5 rounded-full bg-gray-100 px-1.5 py-0.5 text-[10px] font-medium text-gray-600"
          >{{ tab.count }}</span>
          <div
            v-if="activeTab === tab.key"
            class="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600"
          ></div>
        </button>
      </div>

      <!-- Tab: 学生列表 -->
      <div v-if="activeTab === 'students'">
        <div class="flex items-center justify-between border-b border-gray-50 px-6 py-3">
          <span class="text-xs text-gray-500">共 {{ students.length }} 名学生</span>
        </div>
        <div v-if="studentsLoading" class="flex items-center justify-center py-10">
          <Loader2 class="h-5 w-5 animate-spin text-gray-400" />
          <span class="ml-2 text-sm text-gray-500">加载中...</span>
        </div>
        <div v-else-if="students.length > 0" class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-100 bg-gray-50/60">
                <th class="px-6 py-2.5 text-left text-xs font-medium text-gray-500">学号</th>
                <th class="px-6 py-2.5 text-left text-xs font-medium text-gray-500">姓名</th>
                <th class="px-6 py-2.5 text-left text-xs font-medium text-gray-500">性别</th>
                <th class="px-6 py-2.5 text-center text-xs font-medium text-gray-500">状态</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-50">
              <tr
                v-for="s in students"
                :key="s.id"
                class="transition-colors hover:bg-gray-50"
              >
                <td class="px-6 py-3">
                  <code class="rounded bg-gray-100 px-1.5 py-0.5 font-mono text-xs text-gray-600">
                    {{ s.studentCode || '-' }}
                  </code>
                </td>
                <td class="px-6 py-3">
                  <span class="text-sm font-medium text-gray-900">{{ s.name || s.realName || '-' }}</span>
                </td>
                <td class="px-6 py-3">
                  <span class="text-sm text-gray-600">{{ s.gender === 1 ? '男' : s.gender === 2 ? '女' : '-' }}</span>
                </td>
                <td class="px-6 py-3 text-center">
                  <span
                    :class="[
                      'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium',
                      s.status === 1 ? 'bg-green-50 text-green-700' : 'bg-gray-100 text-gray-500'
                    ]"
                  >
                    {{ s.status === 1 ? '在读' : '其他' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="flex flex-col items-center py-10">
          <Users class="h-10 w-10 text-gray-300" />
          <p class="mt-2 text-sm text-gray-500">暂无学生</p>
        </div>
      </div>

      <!-- Tab: 成员用户 (teachers/staff) -->
      <div v-if="activeTab === 'members'">
        <div class="flex items-center justify-between border-b border-gray-50 px-6 py-3">
          <span class="text-xs text-gray-500">共 {{ members.length }} 名成员</span>
        </div>
        <div v-if="membersLoading" class="flex items-center justify-center py-10">
          <Loader2 class="h-5 w-5 animate-spin text-gray-400" />
          <span class="ml-2 text-sm text-gray-500">加载中...</span>
        </div>
        <div v-else-if="members.length > 0" class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-100 bg-gray-50/60">
                <th class="px-6 py-2.5 text-left text-xs font-medium text-gray-500">姓名</th>
                <th class="px-6 py-2.5 text-left text-xs font-medium text-gray-500">职务</th>
                <th class="px-6 py-2.5 text-left text-xs font-medium text-gray-500">关系类型</th>
                <th class="px-6 py-2.5 text-center text-xs font-medium text-gray-500">领导</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-50">
              <tr
                v-for="m in members"
                :key="m.id"
                class="transition-colors hover:bg-gray-50"
              >
                <td class="px-6 py-3">
                  <div class="flex items-center gap-2">
                    <div class="flex h-7 w-7 items-center justify-center rounded-full bg-blue-50 text-blue-600">
                      <User class="h-3.5 w-3.5" />
                    </div>
                    <span class="text-sm font-medium text-gray-900">{{ m.metadata?.realName || `用户#${m.subjectId}` }}</span>
                  </div>
                </td>
                <td class="px-6 py-3">
                  <span class="text-sm text-gray-600">{{ m.metadata?.positionTitle || '-' }}</span>
                </td>
                <td class="px-6 py-3">
                  <span class="inline-flex rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700">
                    {{ RelationLabels[m.relation] || m.relation }}
                  </span>
                </td>
                <td class="px-6 py-3 text-center">
                  <CheckCircle v-if="m.metadata?.isLeader" class="mx-auto h-4 w-4 text-amber-500" />
                  <span v-else class="text-gray-300">-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="flex flex-col items-center py-10">
          <User class="h-10 w-10 text-gray-300" />
          <p class="mt-2 text-sm text-gray-500">暂无成员</p>
        </div>
      </div>

      <!-- Tab: 基本信息 -->
      <div v-if="activeTab === 'info'" class="grid grid-cols-2 gap-x-8 gap-y-4 px-6 py-5 lg:grid-cols-3">
        <div>
          <dt class="text-xs font-medium text-gray-500">班级编码</dt>
          <dd class="mt-1 text-sm font-medium text-gray-900">
            <code class="rounded bg-gray-100 px-2 py-0.5 font-mono text-xs">{{ node.unitCode }}</code>
          </dd>
        </div>
        <div>
          <dt class="text-xs font-medium text-gray-500">班级状态</dt>
          <dd class="mt-1">
            <span :class="statusBadgeClass">{{ statusLabel }}</span>
          </dd>
        </div>
        <div>
          <dt class="text-xs font-medium text-gray-500">班主任</dt>
          <dd class="mt-1 flex items-center gap-1.5 text-sm text-gray-900">
            <User class="h-4 w-4 text-gray-400" />
            {{ node.headTeacherName || '未指定' }}
          </dd>
        </div>
        <div>
          <dt class="text-xs font-medium text-gray-500">入学年份</dt>
          <dd class="mt-1 text-sm text-gray-900">{{ node.enrollmentYear || '-' }}</dd>
        </div>
        <div>
          <dt class="text-xs font-medium text-gray-500">学生人数</dt>
          <dd class="mt-1 text-sm text-gray-900">{{ node.studentCount ?? 0 }} / {{ node.standardSize ?? 45 }}</dd>
        </div>
        <div>
          <dt class="text-xs font-medium text-gray-500">组织状态</dt>
          <dd class="mt-1">
            <span
              :class="[
                'inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium',
                node.status === 'ACTIVE' ? 'bg-green-50 text-green-700' :
                node.status === 'FROZEN' ? 'bg-orange-50 text-orange-700' :
                'bg-red-50 text-red-700'
              ]"
            >
              <CheckCircle v-if="node.status === 'ACTIVE'" class="h-3 w-3" />
              <XCircle v-else class="h-3 w-3" />
              {{ node.statusLabel || node.status }}
            </span>
          </dd>
        </div>
        <div>
          <dt class="text-xs font-medium text-gray-500">上级组织</dt>
          <dd class="mt-1 text-sm text-gray-900">{{ parentName || '顶级组织' }}</dd>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  Users,
  User,
  Pencil,
  Trash2,
  Ban,
  Check,
  CheckCircle,
  XCircle,
  Loader2
} from 'lucide-vue-next'
import type { DepartmentResponse } from '@/api/organization'
import { getClassStudents } from '@/api/organization'
import type { AccessRelation } from '@/types/accessRelation'
import { accessRelationApi } from '@/api/accessRelation'
import { RelationLabels } from '@/types/accessRelation'

interface Props {
  node: DepartmentResponse
  treeData: DepartmentResponse[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  refresh: []
  edit: [node: DepartmentResponse]
  delete: [node: DepartmentResponse]
  toggleStatus: [node: DepartmentResponse]
}>()

// ==================== Tab state ====================
const activeTab = ref<'students' | 'members' | 'info'>('students')

// ==================== Style helpers ====================
// GROUP category color (red) for class/team nodes
const OrgCategoryColorMap: Record<string, string> = {
  ROOT: '#3b82f6', BRANCH: '#8b5cf6', FUNCTIONAL: '#10b981',
  GROUP: '#ef4444', CONTAINER: '#f59e0b',
}
const typeColor = computed(() => OrgCategoryColorMap[props.node.category || ''] || '#3B82F6')

const typeIconStyle = computed(() => ({
  backgroundColor: `${typeColor.value}18`,
  color: typeColor.value
}))

const typeBadgeStyle = computed(() => ({
  backgroundColor: `${typeColor.value}12`,
  color: typeColor.value
}))

const statusLabel = computed(() => {
  const s = props.node.classStatus
  if (s === 'ACTIVE') return '在读中'
  if (s === 'GRADUATED') return '已毕业'
  if (s === 'DISSOLVED') return '已撤销'
  return '筹建中'
})

const statusClass = computed(() => {
  const s = props.node.classStatus
  if (s === 'ACTIVE') return 'text-green-600'
  if (s === 'GRADUATED') return 'text-orange-600'
  if (s === 'DISSOLVED') return 'text-red-500'
  return 'text-gray-500'
})

const statusBadgeClass = computed(() => {
  const s = props.node.classStatus
  const base = 'inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium'
  if (s === 'ACTIVE') return `${base} bg-green-50 text-green-700`
  if (s === 'GRADUATED') return `${base} bg-orange-50 text-orange-700`
  if (s === 'DISSOLVED') return `${base} bg-red-50 text-red-700`
  return `${base} bg-gray-100 text-gray-600`
})

// Find parent name
const findParentName = (nodes: DepartmentResponse[], parentId: number | null): string => {
  if (!parentId) return ''
  for (const n of nodes) {
    if (n.id === parentId) return n.unitName
    if (n.children) {
      const found = findParentName(n.children, parentId)
      if (found) return found
    }
  }
  return ''
}

const parentName = computed(() => findParentName(props.treeData, props.node.parentId))

// ==================== Students data ====================
const students = ref<any[]>([])
const studentsLoading = ref(false)

const loadStudents = async () => {
  studentsLoading.value = true
  try {
    students.value = await getClassStudents(props.node.id) || []
  } catch {
    students.value = []
  } finally {
    studentsLoading.value = false
  }
}

// ==================== Members data ====================
const members = ref<AccessRelation[]>([])
const membersLoading = ref(false)

const loadMembers = async () => {
  membersLoading.value = true
  try {
    const all = await accessRelationApi.getByResource('org_unit', props.node.id)
    members.value = all.filter(r => r.subjectType === 'user')
  } catch {
    members.value = []
  } finally {
    membersLoading.value = false
  }
}

// ==================== Tabs ====================
const tabs = computed(() => [
  { key: 'students' as const, label: '学生列表', count: students.value.length },
  { key: 'members' as const, label: '教师成员', count: members.value.length },
  { key: 'info' as const, label: '基本信息', count: undefined }
])

// ==================== Load data on node change ====================
watch(
  () => props.node.id,
  () => {
    students.value = []
    members.value = []
    activeTab.value = 'students'
    loadStudents()
    loadMembers()
  },
  { immediate: true }
)
</script>
