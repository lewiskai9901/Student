<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        title="用户总数"
        :value="total"
        :icon="Users"
        subtitle="系统用户"
        color="blue"
      />
      <StatCard
        title="启用用户"
        :value="enabledCount"
        :icon="UserCheck"
        subtitle="正常使用"
        color="emerald"
      />
      <StatCard
        title="禁用用户"
        :value="disabledCount"
        :icon="UserX"
        subtitle="已禁用"
        color="rose"
      />
      <StatCard
        title="今日登录"
        :value="todayLoginCount"
        :icon="LogIn"
        subtitle="今天活跃"
        color="purple"
      />
    </div>

    <!-- 搜索和操作栏 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-end gap-4">
        <div class="w-44">
          <label class="mb-1 block text-sm text-gray-600">用户名</label>
          <input
            v-model="queryParams.username"
            type="text"
            placeholder="请输入用户名"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="w-44">
          <label class="mb-1 block text-sm text-gray-600">姓名</label>
          <input
            v-model="queryParams.realName"
            type="text"
            placeholder="请输入姓名"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="w-44">
          <label class="mb-1 block text-sm text-gray-600">手机号</label>
          <input
            v-model="queryParams.phone"
            type="text"
            placeholder="请输入手机号"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="w-32">
          <label class="mb-1 block text-sm text-gray-600">状态</label>
          <select
            v-model="queryParams.status"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option :value="undefined">全部</option>
            <option :value="1">启用</option>
            <option :value="2">禁用</option>
          </select>
        </div>
        <div class="flex gap-2">
          <button
            @click="handleQuery"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          >
            <Search class="h-4 w-4" />
            查询
          </button>
          <button
            @click="resetQuery"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            <RotateCcw class="h-4 w-4" />
            重置
          </button>
        </div>
        <div class="ml-auto">
          <button
            @click="handleAdd"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          >
            <Plus class="h-4 w-4" />
            新增用户
          </button>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-2">
          <span class="font-medium text-gray-900">用户列表</span>
          <span class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600">{{ total }} 条</span>
        </div>
        <div v-if="selectedIds.length > 0" class="flex items-center gap-2">
          <span class="text-sm text-gray-500">已选 {{ selectedIds.length }} 项</span>
          <button
            @click="handleBatchDelete"
            class="inline-flex items-center gap-1 rounded bg-red-50 px-2.5 py-1 text-sm text-red-600 hover:bg-red-100"
          >
            <Trash2 class="h-3.5 w-3.5" />
            删除
          </button>
        </div>
      </div>

      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="w-10 px-4 py-3">
              <input
                type="checkbox"
                :checked="isAllSelected"
                :indeterminate="isIndeterminate"
                @change="handleSelectAll"
                class="h-4 w-4 rounded border-gray-300"
              />
            </th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">用户名</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">姓名</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">手机号</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">角色</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">部门</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">最后登录</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">操作</th>
          </tr>
        </thead>
        <tbody v-if="loading">
          <tr>
            <td colspan="9" class="py-16 text-center">
              <Loader2 class="mx-auto h-8 w-8 animate-spin text-blue-600" />
              <div class="mt-2 text-sm text-gray-500">加载中...</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="userList.length === 0">
          <tr>
            <td colspan="9" class="py-16 text-center">
              <Users class="mx-auto h-12 w-12 text-gray-300" />
              <div class="mt-2 text-sm text-gray-500">暂无数据</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr
            v-for="row in userList"
            :key="row.id"
            class="border-b border-gray-100 hover:bg-gray-50"
          >
            <td class="px-4 py-3">
              <input
                type="checkbox"
                :checked="selectedIds.includes(row.id)"
                @change="handleSelectRow(row)"
                class="h-4 w-4 rounded border-gray-300"
              />
            </td>
            <td class="px-4 py-3 font-medium text-gray-900">{{ row.username }}</td>
            <td class="px-4 py-3 text-gray-700">{{ row.realName || '-' }}</td>
            <td class="px-4 py-3 text-gray-600">{{ row.phone || '-' }}</td>
            <td class="px-4 py-3">
              <template v-if="row.roleNames && row.roleNames.length">
                <span
                  v-for="(role, index) in row.roleNames"
                  :key="index"
                  class="mr-1 rounded bg-blue-50 px-1.5 py-0.5 text-xs text-blue-600"
                >{{ role }}</span>
              </template>
              <span v-else class="text-gray-400">-</span>
            </td>
            <td class="px-4 py-3 text-gray-600">{{ row.departmentName || '-' }}</td>
            <td class="px-4 py-3 text-center">
              <button
                @click="handleStatusChange(row)"
                :class="[
                  'relative inline-flex h-5 w-9 items-center rounded-full transition-colors',
                  row.status === 1 ? 'bg-blue-600' : 'bg-gray-300'
                ]"
              >
                <span
                  :class="[
                    'inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform',
                    row.status === 1 ? 'translate-x-4' : 'translate-x-0.5'
                  ]"
                />
              </button>
            </td>
            <td class="px-4 py-3 text-sm text-gray-500">{{ row.lastLoginTime || '-' }}</td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-center gap-1">
                <button
                  @click="handleEdit(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-blue-50 hover:text-blue-600"
                  title="编辑"
                >
                  <Pencil class="h-4 w-4" />
                </button>
                <button
                  @click="handleAssignRoles(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-green-50 hover:text-green-600"
                  title="分配角色"
                >
                  <Shield class="h-4 w-4" />
                </button>
                <button
                  @click="handleDataScope(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-purple-50 hover:text-purple-600"
                  title="数据范围"
                >
                  <Database class="h-4 w-4" />
                </button>
                <button
                  @click="handleResetPassword(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-amber-50 hover:text-amber-600"
                  title="重置密码"
                >
                  <KeyRound class="h-4 w-4" />
                </button>
                <button
                  @click="handleDelete(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-red-50 hover:text-red-600"
                  title="删除"
                >
                  <Trash2 class="h-4 w-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <div class="text-sm text-gray-500">
          共 {{ total }} 条，第 {{ queryParams.pageNum }} / {{ Math.ceil(total / queryParams.pageSize) || 1 }} 页
        </div>
        <div class="flex items-center gap-2">
          <select
            v-model="queryParams.pageSize"
            @change="loadUserList"
            class="pagination-select"
          >
            <option v-for="size in [10, 20, 50, 100]" :key="size" :value="size">{{ size }}条/页</option>
          </select>
          <div class="flex gap-1">
            <button
              @click="queryParams.pageNum = 1; loadUserList()"
              :disabled="queryParams.pageNum <= 1"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronsLeft class="h-4 w-4" />
            </button>
            <button
              @click="queryParams.pageNum--; loadUserList()"
              :disabled="queryParams.pageNum <= 1"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronLeft class="h-4 w-4" />
            </button>
            <button
              @click="queryParams.pageNum++; loadUserList()"
              :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronRight class="h-4 w-4" />
            </button>
            <button
              @click="queryParams.pageNum = Math.ceil(total / queryParams.pageSize); loadUserList()"
              :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronsRight class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="dialogVisible = false"></div>
          <div class="relative w-full max-w-xl rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">{{ dialogTitle }}</h3>
              <button @click="dialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="max-h-[60vh] overflow-y-auto p-6">
              <div class="grid grid-cols-2 gap-4">
                <div class="col-span-2">
                  <label class="mb-1 block text-sm text-gray-700">
                    用户名 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.username"
                    type="text"
                    placeholder="请输入用户名"
                    :disabled="isEdit"
                    :class="[
                      'h-9 w-full rounded-lg border px-3 text-sm focus:outline-none',
                      isEdit ? 'border-gray-200 bg-gray-100 text-gray-500' : 'border-gray-300 focus:border-blue-500'
                    ]"
                  />
                </div>
                <div v-if="!isEdit" class="col-span-2">
                  <label class="mb-1 block text-sm text-gray-700">
                    密码 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.password"
                    type="password"
                    placeholder="请输入密码（6-20位）"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">
                    姓名 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.realName"
                    type="text"
                    placeholder="请输入姓名"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">工号</label>
                  <input
                    v-model="formData.employeeNo"
                    type="text"
                    placeholder="请输入工号"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">
                    手机号 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.phone"
                    type="text"
                    placeholder="请输入手机号"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">性别</label>
                  <div class="flex h-9 items-center gap-4">
                    <label class="flex items-center gap-1.5">
                      <input v-model="formData.gender" type="radio" :value="1" class="h-4 w-4" />
                      <span class="text-sm text-gray-700">男</span>
                    </label>
                    <label class="flex items-center gap-1.5">
                      <input v-model="formData.gender" type="radio" :value="2" class="h-4 w-4" />
                      <span class="text-sm text-gray-700">女</span>
                    </label>
                  </div>
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">状态</label>
                  <div class="flex h-9 items-center gap-4">
                    <label class="flex items-center gap-1.5">
                      <input v-model="formData.status" type="radio" :value="1" class="h-4 w-4" />
                      <span class="text-sm text-gray-700">启用</span>
                    </label>
                    <label class="flex items-center gap-1.5">
                      <input v-model="formData.status" type="radio" :value="2" class="h-4 w-4" />
                      <span class="text-sm text-gray-700">禁用</span>
                    </label>
                  </div>
                </div>
              </div>
            </div>
            <div class="flex justify-end gap-3 border-t border-gray-200 px-6 py-4">
              <button
                @click="dialogVisible = false"
                class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                @click="handleSubmit"
                :disabled="submitLoading"
                class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
                确定
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 分配角色对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="roleDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="roleDialogVisible = false"></div>
          <div class="relative w-full max-w-md rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">分配角色</h3>
              <button @click="roleDialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="p-6">
              <div class="mb-4 rounded-lg bg-blue-50 p-3 text-sm text-blue-700">
                请勾选需要分配给该用户的角色
              </div>
              <div class="max-h-80 space-y-2 overflow-y-auto">
                <label
                  v-for="role in allRoles"
                  :key="role.id"
                  class="flex cursor-pointer items-center justify-between rounded-lg border p-3 hover:bg-gray-50"
                  :class="selectedRoleIds.includes(role.id) ? 'border-blue-500 bg-blue-50' : 'border-gray-200'"
                >
                  <div class="flex items-center gap-3">
                    <input
                      type="checkbox"
                      :value="role.id"
                      v-model="selectedRoleIds"
                      class="h-4 w-4 rounded border-gray-300"
                    />
                    <span class="font-medium text-gray-900">{{ role.roleName }}</span>
                  </div>
                  <span v-if="role.roleCode" class="text-xs text-gray-500">{{ role.roleCode }}</span>
                </label>
              </div>
            </div>
            <div class="flex justify-end gap-3 border-t border-gray-200 px-6 py-4">
              <button
                @click="roleDialogVisible = false"
                class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                @click="handleRoleSubmit"
                :disabled="roleSubmitLoading"
                class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                <Loader2 v-if="roleSubmitLoading" class="h-4 w-4 animate-spin" />
                确定
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 数据范围对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="dataScopeDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="dataScopeDialogVisible = false"></div>
          <div class="relative w-full max-w-2xl rounded-xl bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <div>
                <h3 class="text-lg font-semibold text-gray-900">管理数据范围</h3>
                <p class="text-sm text-gray-500 mt-0.5">用户: {{ currentUserForScope?.realName || currentUserForScope?.username }}</p>
              </div>
              <button @click="dataScopeDialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="p-6">
              <UserDataScopeManager
                v-if="currentUserForScope"
                :user-id="currentUserForScope.id"
              />
            </div>
            <div class="flex justify-end border-t border-gray-200 px-6 py-4">
              <button
                @click="dataScopeDialogVisible = false"
                class="h-9 rounded-lg bg-gray-100 px-4 text-sm font-medium text-gray-700 hover:bg-gray-200"
              >
                关闭
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Users,
  UserCheck,
  UserX,
  LogIn,
  Plus,
  Search,
  RotateCcw,
  Pencil,
  Trash2,
  Shield,
  KeyRound,
  Database,
  X,
  Loader2,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight
} from 'lucide-vue-next'
import UserDataScopeManager from '@/components/user/UserDataScopeManager.vue'
import StatCard from '@/components/design-system/cards/StatCard.vue'
import {
  getUserPage,
  createUser,
  updateUser,
  deleteUser,
  batchDeleteUsers,
  resetUserPassword,
  updateUserStatus,
  getUserRoles,
  assignUserRoles,
  type User,
  type UserQueryParams,
  type UserFormData
} from '@/api/user'
import { getAllRoles, type Role } from '@/api/role'
import { useConfigStore } from '@/stores/config'

const configStore = useConfigStore()
const loading = ref(false)
const submitLoading = ref(false)
const roleSubmitLoading = ref(false)
const dialogVisible = ref(false)
const roleDialogVisible = ref(false)
const dataScopeDialogVisible = ref(false)
const isEdit = ref(false)
const selectedIds = ref<number[]>([])
const currentUserId = ref<number>()
const currentUserForScope = ref<User | null>(null)

const queryParams = reactive<UserQueryParams>({
  pageNum: 1,
  pageSize: configStore.defaultPageSize
})

const userList = ref<User[]>([])
const total = ref(0)
const allRoles = ref<Role[]>([])
const selectedRoleIds = ref<number[]>([])

// 统计数据
const enabledCount = computed(() => userList.value.filter(u => u.status === 1).length)
const disabledCount = computed(() => userList.value.filter(u => u.status === 2).length)
const todayLoginCount = ref(0)

const formData = reactive<UserFormData>({
  username: '',
  realName: '',
  password: '',
  phone: '',
  employeeNo: '',
  gender: 1,
  status: 1
})

const dialogTitle = computed(() => (isEdit.value ? '编辑用户' : '新增用户'))

const isAllSelected = computed(() => {
  return userList.value.length > 0 && selectedIds.value.length === userList.value.length
})

const isIndeterminate = computed(() => {
  return selectedIds.value.length > 0 && selectedIds.value.length < userList.value.length
})

const loadUserList = async () => {
  loading.value = true
  try {
    const res = await getUserPage(queryParams)
    userList.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const loadAllRoles = async () => {
  try {
    allRoles.value = await getAllRoles()
  } catch (error) {
    console.error('加载角色列表失败:', error)
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  loadUserList()
}

const resetQuery = () => {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: configStore.defaultPageSize,
    username: undefined,
    realName: undefined,
    phone: undefined,
    status: undefined
  })
  loadUserList()
}

const handleSelectAll = (event: Event) => {
  const checked = (event.target as HTMLInputElement).checked
  if (checked) {
    selectedIds.value = userList.value.map(item => typeof item.id === 'string' ? parseInt(item.id) : item.id)
  } else {
    selectedIds.value = []
  }
}

const handleSelectRow = (row: User) => {
  const id = typeof row.id === 'string' ? parseInt(row.id) : row.id
  const index = selectedIds.value.indexOf(id)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
  } else {
    selectedIds.value.push(id)
  }
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formData, {
    username: '',
    realName: '',
    password: '',
    phone: '',
    employeeNo: '',
    gender: 1,
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = async (row: User) => {
  if (!row.id) {
    ElMessage.error('用户ID无效')
    return
  }
  isEdit.value = true
  Object.assign(formData, {
    username: row.username,
    realName: row.realName,
    phone: row.phone,
    employeeNo: row.employeeNo,
    gender: row.gender,
    status: row.status
  })
  currentUserId.value = typeof row.id === 'string' ? parseInt(row.id) : row.id
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.username) {
    ElMessage.error('请输入用户名')
    return
  }
  if (!isEdit.value && !formData.password) {
    ElMessage.error('请输入密码')
    return
  }
  if (!formData.realName) {
    ElMessage.error('请输入姓名')
    return
  }
  if (!formData.phone) {
    ElMessage.error('请输入手机号')
    return
  }

  try {
    submitLoading.value = true
    if (isEdit.value && currentUserId.value) {
      await updateUser(currentUserId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createUser(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadUserList()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: User) => {
  if (!row.id) return
  const userId = typeof row.id === 'string' ? parseInt(row.id) : row.id
  try {
    await ElMessageBox.confirm(`确定要删除用户"${row.realName}"吗?`, '删除确认', { type: 'warning' })
    await deleteUser(userId)
    ElMessage.success('删除成功')
    loadUserList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的${selectedIds.value.length}个用户吗?`, '批量删除', { type: 'warning' })
    await batchDeleteUsers(selectedIds.value)
    ElMessage.success('删除成功')
    selectedIds.value = []
    loadUserList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const handleStatusChange = async (row: User) => {
  if (!row.id) return
  const userId = typeof row.id === 'string' ? parseInt(row.id) : row.id
  const newStatus = row.status === 1 ? 2 : 1
  try {
    await updateUserStatus(userId, newStatus)
    row.status = newStatus
    ElMessage.success('状态已更新')
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
    loadUserList()
  }
}

const handleResetPassword = async (row: User) => {
  if (!row.id) return
  const userId = typeof row.id === 'string' ? parseInt(row.id) : row.id
  try {
    await ElMessageBox.confirm(`确定要重置用户"${row.realName}"的密码吗?`, '重置密码', { type: 'warning' })
    await resetUserPassword(userId)
    ElMessage.success('密码重置成功')
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '重置失败')
  }
}

const handleAssignRoles = async (row: User) => {
  if (!row.id) return
  const userId = typeof row.id === 'string' ? parseInt(row.id) : row.id
  try {
    currentUserId.value = userId
    const roleIds = await getUserRoles(userId)
    selectedRoleIds.value = roleIds
    roleDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载角色失败')
  }
}

const handleRoleSubmit = async () => {
  if (!currentUserId.value) return
  try {
    roleSubmitLoading.value = true
    await assignUserRoles(currentUserId.value, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    loadUserList()
  } catch (error: any) {
    ElMessage.error(error.message || '分配失败')
  } finally {
    roleSubmitLoading.value = false
  }
}

const handleDataScope = (row: User) => {
  currentUserForScope.value = row
  dataScopeDialogVisible.value = true
}

onMounted(() => {
  loadUserList()
  loadAllRoles()
})
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
