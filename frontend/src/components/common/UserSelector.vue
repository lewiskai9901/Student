<template>
  <div class="user-selector">
    <div class="flex gap-4">
      <!-- 左侧：部门树 -->
      <div class="w-1/3 border border-gray-200 rounded-lg bg-white">
        <div class="p-3 border-b border-gray-200">
          <input
            v-model="departmentSearch"
            type="text"
            placeholder="搜索部门..."
            class="w-full h-8 px-3 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div class="h-64 overflow-y-auto p-2">
          <div
            v-for="dept in filteredDepartments"
            :key="dept.id"
            class="cursor-pointer"
          >
            <DepartmentTreeNode
              :department="dept"
              :selected-id="selectedDepartmentId"
              :expanded-ids="expandedDeptIds"
              @select="selectDepartment"
              @toggle="toggleDepartment"
            />
          </div>
          <div v-if="filteredDepartments.length === 0" class="text-center py-4 text-sm text-gray-400">
            暂无部门数据
          </div>
        </div>
      </div>

      <!-- 右侧：用户列表 -->
      <div class="w-2/3 border border-gray-200 rounded-lg bg-white">
        <div class="p-3 border-b border-gray-200 flex items-center gap-2">
          <input
            v-model="userSearch"
            type="text"
            placeholder="搜索用户..."
            class="flex-1 h-8 px-3 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <label v-if="selectedDepartmentId" class="flex items-center gap-1 text-sm text-gray-600">
            <input
              v-model="includeChildren"
              type="checkbox"
              class="rounded border-gray-300"
            />
            包含子部门
          </label>
        </div>
        <div class="h-64 overflow-y-auto">
          <div v-if="loading" class="flex items-center justify-center h-full">
            <span class="text-sm text-gray-400">加载中...</span>
          </div>
          <div v-else-if="userList.length === 0" class="flex items-center justify-center h-full">
            <span class="text-sm text-gray-400">
              {{ selectedDepartmentId ? '该部门暂无用户' : '请先选择部门' }}
            </span>
          </div>
          <div v-else class="divide-y divide-gray-100">
            <label
              v-for="user in userList"
              :key="user.id"
              class="flex items-center px-4 py-2 hover:bg-gray-50 cursor-pointer"
              :class="{ 'bg-blue-50': isSelected(user.id) }"
            >
              <input
                v-if="multiple"
                type="checkbox"
                :checked="isSelected(user.id)"
                :disabled="!isSelected(user.id) && maxSelect && selectedUsers.length >= maxSelect"
                class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                @change="toggleUser(user)"
              />
              <input
                v-else
                type="radio"
                :checked="isSelected(user.id)"
                name="user-selector"
                class="h-4 w-4 border-gray-300 text-blue-600 focus:ring-blue-500"
                @change="selectUser(user)"
              />
              <div class="ml-3 flex-1">
                <span class="text-sm font-medium text-gray-900">{{ user.realName }}</span>
                <span class="ml-2 text-xs text-gray-500">{{ user.departmentName || '未分配部门' }}</span>
              </div>
            </label>
          </div>
        </div>
      </div>
    </div>

    <!-- 已选用户 -->
    <div v-if="selectedUsers.length > 0" class="mt-3 p-3 bg-gray-50 rounded-lg">
      <div class="flex items-center justify-between mb-2">
        <span class="text-sm font-medium text-gray-700">
          已选择 ({{ selectedUsers.length }}{{ maxSelect ? `/${maxSelect}` : '' }})
        </span>
        <button
          type="button"
          class="text-xs text-red-600 hover:text-red-800"
          @click="clearAll"
        >
          清空
        </button>
      </div>
      <div class="flex flex-wrap gap-2">
        <span
          v-for="user in selectedUsers"
          :key="user.id"
          class="inline-flex items-center gap-1 px-2 py-1 bg-blue-100 text-blue-800 rounded text-sm"
        >
          {{ user.realName }}
          <button
            type="button"
            class="hover:text-blue-600"
            @click="removeUser(user)"
          >
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { getDepartmentTree, type DepartmentResponse } from '@/api/v2/organization'
import { getUsersByDepartment, getUsersWithDepartments, type User } from '@/api/v2/user'
import DepartmentTreeNode from './DepartmentTreeNode.vue'

interface SelectedUser {
  id: string | number
  realName: string
  departmentName?: string
}

const props = withDefaults(defineProps<{
  modelValue: string | string[] | null
  multiple?: boolean
  maxSelect?: number
  excludeIds?: string[]
}>(), {
  multiple: false,
  maxSelect: 0,
  excludeIds: () => []
})

const emit = defineEmits<{
  'update:modelValue': [value: string | string[] | null]
  'change': [users: SelectedUser[]]
}>()

// 部门相关
const departments = ref<DepartmentResponse[]>([])
const departmentSearch = ref('')
const selectedDepartmentId = ref<number | string | null>(null)
const expandedDeptIds = ref<Set<number | string>>(new Set())
const includeChildren = ref(false)

// 用户相关
const userList = ref<User[]>([])
const userSearch = ref('')
const selectedUsers = ref<SelectedUser[]>([])
const loading = ref(false)

// 过滤后的部门列表
const filteredDepartments = computed(() => {
  if (!departmentSearch.value) {
    return departments.value
  }
  const keyword = departmentSearch.value.toLowerCase()
  return filterDepartments(departments.value, keyword)
})

// 递归过滤部门
function filterDepartments(depts: DepartmentResponse[], keyword: string): DepartmentResponse[] {
  const result: DepartmentResponse[] = []
  for (const dept of depts) {
    const nameMatch = dept.deptName.toLowerCase().includes(keyword)
    const filteredChildren = dept.children ? filterDepartments(dept.children, keyword) : []

    if (nameMatch || filteredChildren.length > 0) {
      result.push({
        ...dept,
        children: nameMatch ? dept.children : filteredChildren
      })
    }
  }
  return result
}

// 加载部门树
async function loadDepartments() {
  try {
    const data = await getDepartmentTree()
    departments.value = data || []
    // 默认展开第一层
    departments.value.forEach(d => expandedDeptIds.value.add(d.id))
  } catch (error) {
    console.error('加载部门失败', error)
  }
}

// 加载用户列表
async function loadUsers() {
  loading.value = true
  try {
    if (selectedDepartmentId.value) {
      const data = await getUsersByDepartment(
        selectedDepartmentId.value,
        includeChildren.value,
        userSearch.value || undefined
      )
      userList.value = filterExcluded(data)
    } else if (userSearch.value) {
      const data = await getUsersWithDepartments(userSearch.value)
      userList.value = filterExcluded(data)
    } else {
      userList.value = []
    }
  } catch (error) {
    console.error('加载用户失败', error)
    userList.value = []
  } finally {
    loading.value = false
  }
}

// 过滤排除的用户
function filterExcluded(users: User[]): User[] {
  if (!props.excludeIds || props.excludeIds.length === 0) {
    return users
  }
  return users.filter(u => !props.excludeIds.includes(String(u.id)))
}

// 选择部门
function selectDepartment(deptId: number | string) {
  selectedDepartmentId.value = deptId
}

// 切换部门展开状态
function toggleDepartment(deptId: number | string) {
  if (expandedDeptIds.value.has(deptId)) {
    expandedDeptIds.value.delete(deptId)
  } else {
    expandedDeptIds.value.add(deptId)
  }
}

// 检查用户是否已选中
function isSelected(userId: string | number): boolean {
  return selectedUsers.value.some(u => String(u.id) === String(userId))
}

// 选择单个用户（单选模式）
function selectUser(user: User) {
  selectedUsers.value = [{
    id: user.id,
    realName: user.realName,
    departmentName: user.departmentName
  }]
  emitChange()
}

// 切换用户选择（多选模式）
function toggleUser(user: User) {
  const idx = selectedUsers.value.findIndex(u => String(u.id) === String(user.id))
  if (idx >= 0) {
    selectedUsers.value.splice(idx, 1)
  } else {
    if (props.maxSelect && selectedUsers.value.length >= props.maxSelect) {
      return
    }
    selectedUsers.value.push({
      id: user.id,
      realName: user.realName,
      departmentName: user.departmentName
    })
  }
  emitChange()
}

// 移除用户
function removeUser(user: SelectedUser) {
  const idx = selectedUsers.value.findIndex(u => String(u.id) === String(user.id))
  if (idx >= 0) {
    selectedUsers.value.splice(idx, 1)
    emitChange()
  }
}

// 清空所有选择
function clearAll() {
  selectedUsers.value = []
  emitChange()
}

// 发送变更事件
function emitChange() {
  if (props.multiple) {
    emit('update:modelValue', selectedUsers.value.map(u => String(u.id)))
  } else {
    emit('update:modelValue', selectedUsers.value.length > 0 ? String(selectedUsers.value[0].id) : null)
  }
  emit('change', [...selectedUsers.value])
}

// 监听部门选择变化
watch([selectedDepartmentId, includeChildren], () => {
  loadUsers()
})

// 用户搜索防抖
let userSearchTimer: ReturnType<typeof setTimeout>
watch(userSearch, () => {
  clearTimeout(userSearchTimer)
  userSearchTimer = setTimeout(() => {
    loadUsers()
  }, 300)
})

// 初始化
onMounted(() => {
  loadDepartments()
})
</script>

<style scoped>
.user-selector {
  min-width: 600px;
}
</style>
