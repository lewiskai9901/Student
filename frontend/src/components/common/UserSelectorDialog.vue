<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="640px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', $event)"
  >
    <!-- 顶部筛选条 -->
    <div class="mb-3 flex items-center gap-3">
      <el-input
        v-model="keyword"
        placeholder="搜索姓名/用户名"
        size="small"
        clearable
        class="!w-48"
      >
        <template #prefix><Search class="h-3.5 w-3.5 text-gray-400" /></template>
      </el-input>
      <el-select v-model="filterUserType" placeholder="用户类型" size="small" clearable class="!w-32">
        <el-option
          v-for="(name, code) in userTypeNameMap"
          :key="code"
          :label="name"
          :value="code"
        />
      </el-select>
      <el-radio-group v-model="filterAssignment" size="small">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="assigned">已分配</el-radio-button>
        <el-radio-button value="unassigned">未分配</el-radio-button>
      </el-radio-group>
      <span class="ml-auto text-xs text-gray-400">共 {{ filteredUsers.length }} 人</span>
    </div>

    <!-- 用户列表 -->
    <div v-if="loading" class="flex items-center justify-center py-16">
      <Loader2 class="h-5 w-5 animate-spin text-gray-400" />
      <span class="ml-2 text-sm text-gray-500">加载中...</span>
    </div>
    <div v-else>
      <!-- 表头 -->
      <div class="flex items-center border-b border-gray-200 px-3 py-2 text-xs font-medium text-gray-500">
        <span class="w-8"></span>
        <span class="w-36">姓名</span>
        <span class="w-32">用户名</span>
        <span class="w-28">用户类型</span>
        <span class="flex-1">归属组织</span>
      </div>

      <!-- 列表 -->
      <div class="max-h-[320px] overflow-y-auto">
        <div
          v-for="u in filteredUsers"
          :key="u.id"
          class="flex cursor-pointer items-center border-b border-gray-50 px-3 py-2 transition-colors hover:bg-gray-50"
          :class="{ 'bg-blue-50 hover:bg-blue-50': isSelected(u) }"
          @click="toggleSelect(u)"
        >
          <span class="w-8">
            <input
              type="checkbox"
              :checked="isSelected(u)"
              class="h-3.5 w-3.5 rounded border-gray-300 text-blue-600"
              @click.stop
              @change="toggleSelect(u)"
            />
          </span>
          <span class="w-36 truncate text-sm font-medium text-gray-900">{{ u.realName }}</span>
          <span class="w-32 truncate text-sm text-gray-500">{{ u.username }}</span>
          <span class="w-28 truncate text-xs text-gray-500">{{ userTypeNameMap[u.userType || ''] || u.userType || '-' }}</span>
          <span class="flex-1 truncate text-xs" :class="u.primaryOrgUnitId ? 'text-gray-500' : 'text-gray-300'">
            {{ u.primaryOrgUnitName || u.orgUnitName || '(未分配)' }}
          </span>
        </div>
        <div v-if="filteredUsers.length === 0" class="py-12 text-center text-sm text-gray-400">
          无匹配用户
        </div>
      </div>

      <!-- 已选 -->
      <div v-if="selectedUsers.length > 0" class="mt-2 rounded-lg bg-blue-50 px-3 py-2">
        <div class="flex items-center justify-between">
          <span class="text-xs text-blue-700">
            已选 {{ selectedUsers.length }} 人：{{ selectedUsers.map(u => u.realName).join('、') }}
          </span>
          <button
            class="text-xs text-red-500 hover:text-red-700"
            @click="selectedUsers = []"
          >清空</button>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button @click="emit('update:visible', false)">取消</el-button>
        <el-button
          type="primary"
          :disabled="selectedUsers.length === 0"
          @click="handleConfirm"
        >
          确认选择 ({{ selectedUsers.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Loader2, Search } from 'lucide-vue-next'
import type { SimpleUser } from '@/types/user'
import { getSimpleUserList } from '@/api/user'
import { getEnabledUserTypes } from '@/api/userType'

interface Props {
  visible: boolean
  multiple?: boolean
  excludeUserIds?: (string | number)[]
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  multiple: false,
  excludeUserIds: () => [],
  title: '选择用户',
})

const emit = defineEmits<{
  'update:visible': [v: boolean]
  confirm: [users: SimpleUser[]]
}>()

const loading = ref(false)
const allUsers = ref<SimpleUser[]>([])
const selectedUsers = ref<SimpleUser[]>([])

// Filters
const keyword = ref('')
const filterUserType = ref('')
const filterAssignment = ref<'all' | 'assigned' | 'unassigned'>('all')

// User type map
const userTypeNameMap = ref<Record<string, string>>({})

const loadData = async () => {
  loading.value = true
  try {
    const [users, types] = await Promise.all([
      getSimpleUserList(),
      getEnabledUserTypes(),
    ])
    allUsers.value = users
    const map: Record<string, string> = {}
    for (const t of types) map[t.typeCode] = t.typeName
    userTypeNameMap.value = map
  } catch (e) {
    console.error('Failed to load users', e)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.visible,
  (vis) => {
    if (vis) {
      selectedUsers.value = []
      keyword.value = ''
      filterUserType.value = ''
      filterAssignment.value = 'all'
      if (allUsers.value.length === 0) loadData()
    }
  }
)

const excludeSet = computed(() => new Set(props.excludeUserIds.map(String)))

const filteredUsers = computed(() => {
  return allUsers.value.filter(u => {
    if (excludeSet.value.has(String(u.id))) return false
    if (keyword.value) {
      const kw = keyword.value.toLowerCase()
      if (!u.realName.toLowerCase().includes(kw) && !u.username.toLowerCase().includes(kw)) return false
    }
    if (filterUserType.value && u.userType !== filterUserType.value) return false
    if (filterAssignment.value === 'assigned' && !u.primaryOrgUnitId) return false
    if (filterAssignment.value === 'unassigned' && u.primaryOrgUnitId) return false
    return true
  })
})

const isSelected = (u: SimpleUser) => selectedUsers.value.some(s => String(s.id) === String(u.id))

const toggleSelect = (u: SimpleUser) => {
  const idx = selectedUsers.value.findIndex(s => String(s.id) === String(u.id))
  if (idx >= 0) {
    selectedUsers.value.splice(idx, 1)
  } else {
    if (props.multiple) {
      selectedUsers.value.push(u)
    } else {
      selectedUsers.value = [u]
    }
  }
}

const handleConfirm = () => {
  emit('confirm', [...selectedUsers.value])
  emit('update:visible', false)
}
</script>
