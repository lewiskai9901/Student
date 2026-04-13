<script setup lang="ts">
/**
 * InspectorPoolManager - Inspector pool management
 *
 * Manages the list of inspectors assigned to a project.
 * Supports adding new inspectors via a user selector dialog
 * and removing existing ones.
 */
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Trash2, Search } from 'lucide-vue-next'
import { InspectorRoleConfig } from '@/types/insp/enums'

interface InspectorEntry {
  userId: number
  userName: string
  role: string
}

const props = defineProps<{
  modelValue: InspectorEntry[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: InspectorEntry[]]
}>()

// ---------- Dialog State ----------

const dialogVisible = ref(false)
const searchKeyword = ref('')
const userSearchResults = ref<{ id: number; name: string }[]>([])
const searchLoading = ref(false)
const selectedUserId = ref<number | null>(null)
const selectedUserName = ref('')
const selectedRole = ref<string>('INSPECTOR')

// ---------- Computed ----------

const inspectors = computed(() => props.modelValue ?? [])

const roleOptions = computed(() =>
  Object.entries(InspectorRoleConfig).map(([key, cfg]) => ({
    value: key,
    label: cfg.label,
  }))
)

const inspectorCount = computed(() => inspectors.value.length)
const leadCount = computed(() => inspectors.value.filter(i => i.role === 'LEAD').length)

// ---------- Add Inspector ----------

function openAddDialog() {
  searchKeyword.value = ''
  userSearchResults.value = []
  selectedUserId.value = null
  selectedUserName.value = ''
  selectedRole.value = 'INSPECTOR'
  dialogVisible.value = true
}

async function searchUsers() {
  if (!searchKeyword.value.trim()) return
  searchLoading.value = true
  try {
    // In production, call userApi.search(searchKeyword.value)
    const { default: request } = await import('@/utils/request')
    const res = await request.get('/api/users', {
      params: { keyword: searchKeyword.value, size: 20 },
    })
    const records = res.data?.records ?? res.records ?? []
    userSearchResults.value = records.map((u: any) => ({
      id: u.id,
      name: u.realName || u.username || `ID:${u.id}`,
    }))
  } catch {
    userSearchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

function selectUser(user: { id: number; name: string }) {
  selectedUserId.value = user.id
  selectedUserName.value = user.name
}

function confirmAdd() {
  if (!selectedUserId.value || !selectedUserName.value) {
    ElMessage.warning('请先选择用户')
    return
  }
  // Check for duplicates
  const exists = inspectors.value.some(i => i.userId === selectedUserId.value)
  if (exists) {
    ElMessage.warning('该用户已在检查员列表中')
    return
  }

  const updated = [
    ...inspectors.value,
    {
      userId: selectedUserId.value,
      userName: selectedUserName.value,
      role: selectedRole.value,
    },
  ]
  emit('update:modelValue', updated)
  dialogVisible.value = false
}

// ---------- Remove Inspector ----------

async function removeInspector(index: number) {
  const inspector = inspectors.value[index]
  try {
    await ElMessageBox.confirm(
      `确定移除检查员「${inspector.userName}」？`,
      '确认移除',
      { type: 'warning' },
    )
    const updated = [...inspectors.value]
    updated.splice(index, 1)
    emit('update:modelValue', updated)
  } catch {
    // cancelled
  }
}

// ---------- Update Role ----------

function updateRole(index: number, role: string) {
  const updated = [...inspectors.value]
  updated[index] = { ...updated[index], role }
  emit('update:modelValue', updated)
}
</script>

<template>
  <div class="inspector-pool-manager">
    <!-- Header -->
    <div class="flex items-center justify-between mb-3">
      <div class="flex items-center gap-2">
        <span class="text-sm font-medium text-gray-700">检查员列表</span>
        <el-tag size="small" type="info">{{ inspectorCount }} 人</el-tag>
        <el-tag v-if="leadCount > 0" size="small" type="warning">
          {{ leadCount }} 组长
        </el-tag>
      </div>
      <el-button type="primary" size="small" @click="openAddDialog">
        <Plus class="w-3.5 h-3.5 mr-1" />添加
      </el-button>
    </div>

    <!-- Inspector Table -->
    <el-table
      :data="inspectors"
      stripe
      size="small"
      empty-text="暂无检查员，请点击上方按钮添加"
    >
      <el-table-column prop="userName" label="姓名" min-width="120" />
      <el-table-column label="角色" width="140">
        <template #default="{ row, $index }">
          <el-select
            :model-value="row.role"
            size="small"
            @update:model-value="(val: string) => updateRole($index, val)"
          >
            <el-option
              v-for="opt in roleOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column label="操作" width="70" align="center">
        <template #default="{ $index }">
          <el-button
            link
            type="danger"
            size="small"
            @click="removeInspector($index)"
          >
            <Trash2 class="w-3.5 h-3.5" />
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add Inspector Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="添加检查员"
      width="480px"
      :close-on-click-modal="false"
    >
      <!-- Search -->
      <div class="flex items-center gap-2 mb-3">
        <el-input
          v-model="searchKeyword"
          placeholder="输入姓名搜索用户"
          clearable
          @keyup.enter="searchUsers"
        >
          <template #prefix>
            <Search class="w-4 h-4 text-gray-400" />
          </template>
        </el-input>
        <el-button type="primary" :loading="searchLoading" @click="searchUsers">
          搜索
        </el-button>
      </div>

      <!-- Search Results -->
      <div
        class="border border-gray-200 rounded-md overflow-auto mb-3"
        style="max-height: 200px"
      >
        <div
          v-if="userSearchResults.length === 0"
          class="py-6 text-center text-sm text-gray-400"
        >
          {{ searchKeyword ? '未找到匹配用户' : '请输入关键词搜索' }}
        </div>
        <div
          v-for="user in userSearchResults"
          :key="user.id"
          class="flex items-center justify-between px-3 py-2 cursor-pointer transition hover:bg-blue-50"
          :class="{ 'bg-blue-50 font-medium': selectedUserId === user.id }"
          @click="selectUser(user)"
        >
          <span class="text-sm">{{ user.name }}</span>
          <span class="text-xs text-gray-400">ID: {{ user.id }}</span>
        </div>
      </div>

      <!-- Role Select -->
      <div class="flex items-center gap-3">
        <label class="text-sm text-gray-600 shrink-0">角色：</label>
        <el-select v-model="selectedRole" class="flex-1">
          <el-option
            v-for="opt in roleOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </div>

      <!-- Selected Preview -->
      <div
        v-if="selectedUserId"
        class="mt-3 rounded-md bg-blue-50 px-3 py-2 text-sm text-blue-600"
      >
        已选择：{{ selectedUserName }} (ID: {{ selectedUserId }})
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedUserId" @click="confirmAdd">
          确定添加
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
