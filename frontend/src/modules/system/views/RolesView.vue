<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        title="角色总数"
        :value="total"
        :icon="Shield"
        subtitle="权限角色"
        color="blue"
      />
      <StatCard
        title="启用角色"
        :value="enabledCount"
        :icon="ShieldCheck"
        subtitle="正常使用"
        color="emerald"
      />
      <StatCard
        title="禁用角色"
        :value="disabledCount"
        :icon="ShieldOff"
        subtitle="已禁用"
        color="rose"
      />
      <StatCard
        title="系统角色"
        :value="systemRoleCount"
        :icon="ShieldAlert"
        subtitle="内置角色"
        color="purple"
      />
    </div>

    <!-- 搜索和操作栏 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-end gap-4">
        <div class="w-44">
          <label class="mb-1 block text-sm text-gray-600">角色名称</label>
          <input
            v-model="queryParams.roleName"
            type="text"
            placeholder="请输入角色名称"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="w-44">
          <label class="mb-1 block text-sm text-gray-600">角色编码</label>
          <input
            v-model="queryParams.roleCode"
            type="text"
            placeholder="请输入角色编码"
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
            <option :value="0">禁用</option>
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
            新增角色
          </button>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-2">
          <span class="font-medium text-gray-900">角色列表</span>
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
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">角色名称</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">角色编码</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">描述</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">排序</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">创建时间</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">操作</th>
          </tr>
        </thead>
        <tbody v-if="loading">
          <tr>
            <td colspan="8" class="py-16 text-center">
              <Loader2 class="mx-auto h-8 w-8 animate-spin text-blue-600" />
              <div class="mt-2 text-sm text-gray-500">加载中...</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="roleList.length === 0">
          <tr>
            <td colspan="8" class="py-16 text-center">
              <Shield class="mx-auto h-12 w-12 text-gray-300" />
              <div class="mt-2 text-sm text-gray-500">暂无数据</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr
            v-for="row in roleList"
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
            <td class="px-4 py-3">
              <div class="flex items-center gap-2">
                <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-100">
                  <Shield class="h-4 w-4 text-blue-600" />
                </div>
                <span class="font-medium text-gray-900">{{ row.roleName }}</span>
              </div>
            </td>
            <td class="px-4 py-3">
              <span class="rounded bg-gray-100 px-2 py-0.5 font-mono text-xs text-gray-600">{{ row.roleCode }}</span>
            </td>
            <td class="max-w-xs truncate px-4 py-3 text-gray-600">{{ row.description || '-' }}</td>
            <td class="px-4 py-3 text-center text-gray-600">{{ row.level ?? 0 }}</td>
            <td class="px-4 py-3 text-center">
              <span
                :class="[
                  'rounded-full px-2 py-0.5 text-xs font-medium',
                  row.isEnabled !== false ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                ]"
              >{{ row.isEnabled !== false ? '启用' : '禁用' }}</span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-500">{{ row.createdAt || '-' }}</td>
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
                  @click="handleAssignPermissions(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-green-50 hover:text-green-600"
                  title="分配权限"
                >
                  <Lock class="h-4 w-4" />
                </button>
                <button
                  @click="handleDataPermissions(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-purple-50 hover:text-purple-600"
                  title="数据权限"
                >
                  <Settings class="h-4 w-4" />
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
            @change="loadRoleList"
            class="pagination-select"
          >
            <option v-for="size in [10, 20, 50, 100]" :key="size" :value="size">{{ size }}条/页</option>
          </select>
          <div class="flex gap-1">
            <button
              @click="queryParams.pageNum = 1; loadRoleList()"
              :disabled="queryParams.pageNum <= 1"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronsLeft class="h-4 w-4" />
            </button>
            <button
              @click="queryParams.pageNum--; loadRoleList()"
              :disabled="queryParams.pageNum <= 1"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronLeft class="h-4 w-4" />
            </button>
            <button
              @click="queryParams.pageNum++; loadRoleList()"
              :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronRight class="h-4 w-4" />
            </button>
            <button
              @click="queryParams.pageNum = Math.ceil(total / queryParams.pageSize); loadRoleList()"
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
          <div class="relative w-full max-w-md rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">{{ dialogTitle }}</h3>
              <button @click="dialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="p-6">
              <div class="space-y-4">
                <div>
                  <label class="mb-1 block text-sm text-gray-700">
                    角色名称 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.roleName"
                    type="text"
                    placeholder="请输入角色名称"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">
                    角色编码 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.roleCode"
                    type="text"
                    placeholder="请输入角色编码"
                    :disabled="isEdit"
                    :class="[
                      'h-9 w-full rounded-lg border px-3 font-mono text-sm focus:outline-none',
                      isEdit ? 'border-gray-200 bg-gray-100 text-gray-500' : 'border-gray-300 focus:border-blue-500'
                    ]"
                  />
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">角色描述</label>
                  <textarea
                    v-model="formData.description"
                    rows="3"
                    placeholder="请输入角色描述"
                    class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                  ></textarea>
                </div>
                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">排序</label>
                    <input
                      v-model.number="formData.sortOrder"
                      type="number"
                      min="0"
                      class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    />
                  </div>
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">状态</label>
                    <div class="flex h-9 items-center gap-4">
                      <label class="flex items-center gap-1.5">
                        <input v-model="formData.status" type="radio" :value="1" class="h-4 w-4" />
                        <span class="text-sm text-gray-700">启用</span>
                      </label>
                      <label class="flex items-center gap-1.5">
                        <input v-model="formData.status" type="radio" :value="0" class="h-4 w-4" />
                        <span class="text-sm text-gray-700">禁用</span>
                      </label>
                    </div>
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

    <!-- 分配权限对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="permissionDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="permissionDialogVisible = false"></div>
          <div class="relative w-full max-w-3xl rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">分配权限</h3>
              <button @click="permissionDialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="p-6">
              <div class="mb-4 flex gap-2">
                <button
                  @click="handleExpandAll"
                  class="inline-flex h-8 items-center gap-1.5 rounded border border-gray-300 px-3 text-sm text-gray-700 hover:bg-gray-50"
                >
                  <FolderOpen class="h-4 w-4" />
                  展开全部
                </button>
                <button
                  @click="handleCollapseAll"
                  class="inline-flex h-8 items-center gap-1.5 rounded border border-gray-300 px-3 text-sm text-gray-700 hover:bg-gray-50"
                >
                  <Folder class="h-4 w-4" />
                  折叠全部
                </button>
                <button
                  @click="handleCheckAll"
                  class="inline-flex h-8 items-center gap-1.5 rounded border border-gray-300 px-3 text-sm text-gray-700 hover:bg-gray-50"
                >
                  <CheckSquare class="h-4 w-4" />
                  全选
                </button>
                <button
                  @click="handleUncheckAll"
                  class="inline-flex h-8 items-center gap-1.5 rounded border border-gray-300 px-3 text-sm text-gray-700 hover:bg-gray-50"
                >
                  <Square class="h-4 w-4" />
                  取消全选
                </button>
              </div>
              <div class="max-h-96 overflow-y-auto rounded-lg border border-gray-200 bg-gray-50 p-4">
                <div v-for="module in permissionModules" :key="module.code" class="mb-3 last:mb-0">
                  <div
                    @click="toggleModule(module.code)"
                    class="flex cursor-pointer items-center justify-between rounded-lg bg-white p-3 hover:bg-gray-50"
                  >
                    <div class="flex items-center gap-2">
                      <ChevronRight
                        class="h-4 w-4 text-gray-400 transition-transform"
                        :class="{ 'rotate-90': expandedModules.includes(module.code) }"
                      />
                      <span class="font-medium text-gray-900">{{ module.name }}</span>
                      <span class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500">{{ module.count }}</span>
                    </div>
                    <label class="flex items-center gap-2" @click.stop>
                      <input
                        type="checkbox"
                        :checked="module.checked"
                        @change="handleModuleCheck(module)"
                        class="h-4 w-4 rounded border-gray-300"
                      />
                      <span class="text-sm text-gray-600">全选</span>
                    </label>
                  </div>
                  <div v-show="expandedModules.includes(module.code)" class="mt-2 space-y-1 rounded-lg bg-white p-3">
                    <template v-for="permission in module.permissions" :key="permission.id">
                      <div class="flex items-center gap-2">
                        <input
                          type="checkbox"
                          :checked="selectedPermissionIds.includes(permission.id)"
                          @change="handlePermissionCheck(permission.id)"
                          class="h-4 w-4 rounded border-gray-300"
                        />
                        <span class="text-gray-900">{{ permission.permissionName }}</span>
                      </div>
                      <div v-if="permission.children && permission.children.length" class="ml-6 space-y-1">
                        <div
                          v-for="child in permission.children"
                          :key="child.id"
                          class="flex items-center gap-2"
                        >
                          <input
                            type="checkbox"
                            :checked="selectedPermissionIds.includes(child.id)"
                            @change="handlePermissionCheck(child.id)"
                            class="h-4 w-4 rounded border-gray-300"
                          />
                          <span class="text-gray-700">{{ child.permissionName }}</span>
                          <span class="rounded bg-gray-100 px-1 py-0.5 text-xs text-gray-500">{{ child.permissionCode }}</span>
                        </div>
                      </div>
                    </template>
                  </div>
                </div>
              </div>
            </div>
            <div class="flex items-center justify-between border-t border-gray-200 px-6 py-4">
              <span class="text-sm text-gray-600">已选 <span class="font-medium text-blue-600">{{ selectedPermissionIds.length }}</span> 项</span>
              <div class="flex gap-3">
                <button
                  @click="permissionDialogVisible = false"
                  class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  取消
                </button>
                <button
                  @click="handlePermissionSubmit"
                  :disabled="permissionSubmitLoading"
                  class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  <Loader2 v-if="permissionSubmitLoading" class="h-4 w-4 animate-spin" />
                  确定
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 数据权限配置对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="dataPermissionDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="dataPermissionDialogVisible = false"></div>
          <div class="relative w-full max-w-lg rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">配置数据权限 - {{ currentRoleName }}</h3>
              <button @click="dataPermissionDialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="max-h-96 overflow-y-auto p-6">
              <div class="space-y-4">
                <div v-for="perm in dataPermissions" :key="perm.moduleCode" class="rounded-lg border border-gray-200 p-4">
                  <div class="flex items-center justify-between">
                    <span class="font-medium text-gray-900">{{ perm.moduleName }}</span>
                    <select
                      v-model="perm.dataScope"
                      class="h-9 w-40 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    >
                      <option v-for="opt in DATA_SCOPE_OPTIONS" :key="opt.value" :value="opt.value">
                        {{ opt.label }}
                      </option>
                    </select>
                  </div>
                </div>
                <div v-if="dataPermissions.length === 0" class="py-8 text-center text-gray-500">
                  加载中...
                </div>
              </div>
            </div>
            <div class="flex items-center justify-end gap-3 border-t border-gray-200 px-6 py-4">
              <button
                @click="dataPermissionDialogVisible = false"
                class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                @click="handleDataPermissionSubmit"
                :disabled="dataPermissionSubmitLoading"
                class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                <Loader2 v-if="dataPermissionSubmitLoading" class="h-4 w-4 animate-spin" />
                保存
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
  Shield,
  ShieldCheck,
  ShieldOff,
  ShieldAlert,
  Plus,
  Search,
  RotateCcw,
  Pencil,
  Trash2,
  Lock,
  X,
  Loader2,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  FolderOpen,
  Folder,
  CheckSquare,
  Square,
  Users,
  Settings,
  BarChart3,
  GraduationCap,
  Building2,
  BookOpen,
  Home
} from 'lucide-vue-next'
// V2 DDD API
import {
  getRolesPage,
  createRole,
  updateRole,
  deleteRole,
  batchDeleteRoles,
  getRolePermissionIds,
  setRolePermissions,
  getPermissionTree,
  enableRole,
  disableRole,
  DATA_SCOPE_OPTIONS,
  getRoleDataPermissions,
  saveRoleDataPermissions,
  type RoleResponse,
  type RoleDataPermission
} from '@/api/v2/access'
import type {
  CreateRoleRequest,
  UpdateRoleRequest,
  RoleQueryParams
} from '@/types/v2/access'

const loading = ref(false)
const submitLoading = ref(false)
const permissionSubmitLoading = ref(false)
const dataPermissionSubmitLoading = ref(false)
const dialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const dataPermissionDialogVisible = ref(false)
const isEdit = ref(false)
const currentRoleName = ref('')
const dataPermissions = ref<RoleDataPermission[]>([])
const selectedIds = ref<number[]>([])
const currentRoleId = ref<number>()
const expandedModules = ref<string[]>([])

// 查询参数 - V2 类型
interface LocalRoleQueryParams extends RoleQueryParams {
  pageNum: number
  pageSize: number
  roleName?: string
  roleCode?: string
  status?: number
}

const queryParams = reactive<LocalRoleQueryParams>({
  pageNum: 1,
  pageSize: 10
})

const roleList = ref<RoleResponse[]>([])
const total = ref(0)
const permissionTree = ref<any[]>([])
const selectedPermissionIds = ref<number[]>([])

// 统计数据 - 适配 V2 RoleResponse (isEnabled: boolean)
const enabledCount = computed(() => roleList.value.filter(r => r.isEnabled !== false).length)
const disabledCount = computed(() => roleList.value.filter(r => r.isEnabled === false).length)
const systemRoleCount = computed(() => roleList.value.filter(r => r.isSystem === true).length)

// 表单数据 - V2 字段
interface RoleFormData {
  roleName: string
  roleCode: string
  description: string
  sortOrder: number
  status: number  // 前端兼容: 1=启用, 0=禁用
}

const formData = reactive<RoleFormData>({
  roleName: '',
  roleCode: '',
  description: '',
  sortOrder: 0,
  status: 1
})

const dialogTitle = computed(() => (isEdit.value ? '编辑角色' : '新增角色'))

const isAllSelected = computed(() => roleList.value.length > 0 && selectedIds.value.length === roleList.value.length)
const isIndeterminate = computed(() => selectedIds.value.length > 0 && selectedIds.value.length < roleList.value.length)

// 模块配置
const moduleConfig: Record<string, { name: string }> = {
  student: { name: '学生管理' },
  system: { name: '系统管理' },
  quantification: { name: '量化管理' },
  major: { name: '专业管理' },
  building: { name: '楼栋管理' },
  classroom: { name: '教室管理' },
  dormitory: { name: '宿舍管理' }
}

interface PermissionModule {
  code: string
  name: string
  count: number
  permissions: any[]
  checked: boolean
}

const permissionModules = computed<PermissionModule[]>(() => {
  if (!permissionTree.value?.length) return []
  const moduleMap = new Map<string, any[]>()
  permissionTree.value.forEach((p: any) => {
    const code = p.permissionCode.split(':')[0]
    if (!moduleMap.has(code)) moduleMap.set(code, [])
    moduleMap.get(code)!.push(p)
  })
  const modules: PermissionModule[] = []
  moduleMap.forEach((permissions, code) => {
    const config = moduleConfig[code] || { name: code }
    const allIds = getAllPermissionIds(permissions)
    modules.push({
      code,
      name: config.name,
      count: allIds.length,
      permissions,
      checked: allIds.every(id => selectedPermissionIds.value.includes(id))
    })
  })
  return modules.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
})

const getAllPermissionIds = (permissions: any[]): number[] => {
  const ids: number[] = []
  const traverse = (perms: any[]) => {
    perms.forEach(p => {
      ids.push(p.id)
      if (p.children?.length) traverse(p.children)
    })
  }
  traverse(permissions)
  return ids
}

const loadRoleList = async () => {
  loading.value = true
  try {
    const res = await getRolesPage({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      roleType: queryParams.roleType
    })
    roleList.value = res.records
    total.value = res.total
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const loadPermissionTree = async () => {
  try {
    permissionTree.value = await getPermissionTree()
  } catch (error) {
    console.error('加载权限树失败:', error)
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  loadRoleList()
}

const resetQuery = () => {
  Object.assign(queryParams, { pageNum: 1, pageSize: 10, roleName: undefined, roleCode: undefined, status: undefined })
  loadRoleList()
}

const handleSelectAll = (e: Event) => {
  selectedIds.value = (e.target as HTMLInputElement).checked ? roleList.value.map(r => r.id) : []
}

const handleSelectRow = (row: RoleResponse) => {
  const idx = selectedIds.value.indexOf(row.id)
  idx > -1 ? selectedIds.value.splice(idx, 1) : selectedIds.value.push(row.id)
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formData, { roleName: '', roleCode: '', description: '', sortOrder: 0, status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: RoleResponse) => {
  isEdit.value = true
  currentRoleId.value = row.id
  Object.assign(formData, {
    roleName: row.roleName,
    roleCode: row.roleCode,
    description: row.description || '',
    sortOrder: row.level || 0,
    status: row.isEnabled !== false ? 1 : 0
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.roleName || !formData.roleCode) {
    ElMessage.error('请填写必填项')
    return
  }
  try {
    submitLoading.value = true
    if (isEdit.value && currentRoleId.value) {
      // V2 更新请求
      const updateData: UpdateRoleRequest = {
        roleName: formData.roleName,
        description: formData.description,
        level: formData.sortOrder
      }
      await updateRole(currentRoleId.value, updateData)
      // 处理启用/禁用状态
      if (formData.status === 1) {
        await enableRole(currentRoleId.value)
      } else {
        await disableRole(currentRoleId.value)
      }
      ElMessage.success('更新成功')
    } else {
      // V2 创建请求
      const createData: CreateRoleRequest = {
        roleCode: formData.roleCode,
        roleName: formData.roleName,
        description: formData.description,
        level: formData.sortOrder
      }
      await createRole(createData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadRoleList()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: RoleResponse) => {
  try {
    await ElMessageBox.confirm(`确定删除角色"${row.roleName}"吗?`, '删除确认', { type: 'warning' })
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    loadRoleList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的${selectedIds.value.length}个角色吗?`, '批量删除', { type: 'warning' })
    await batchDeleteRoles(selectedIds.value)
    ElMessage.success('删除成功')
    selectedIds.value = []
    loadRoleList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const handleAssignPermissions = async (row: RoleResponse) => {
  try {
    currentRoleId.value = row.id
    // V2 API 获取角色权限ID
    const ids = await getRolePermissionIds(row.id)
    selectedPermissionIds.value = ids
    permissionDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载权限失败')
  }
}

const toggleModule = (code: string) => {
  const idx = expandedModules.value.indexOf(code)
  idx > -1 ? expandedModules.value.splice(idx, 1) : expandedModules.value.push(code)
}

const handleExpandAll = () => { expandedModules.value = permissionModules.value.map(m => m.code) }
const handleCollapseAll = () => { expandedModules.value = [] }
const handleCheckAll = () => {
  const allIds: number[] = []
  permissionModules.value.forEach(m => allIds.push(...getAllPermissionIds(m.permissions)))
  selectedPermissionIds.value = allIds
}
const handleUncheckAll = () => { selectedPermissionIds.value = [] }

const handleModuleCheck = (module: PermissionModule) => {
  const allIds = getAllPermissionIds(module.permissions)
  if (module.checked) {
    selectedPermissionIds.value = selectedPermissionIds.value.filter(id => !allIds.includes(id))
  } else {
    selectedPermissionIds.value.push(...allIds.filter(id => !selectedPermissionIds.value.includes(id)))
  }
}

const handlePermissionCheck = (id: number) => {
  const idx = selectedPermissionIds.value.indexOf(id)
  idx > -1 ? selectedPermissionIds.value.splice(idx, 1) : selectedPermissionIds.value.push(id)
}

const handlePermissionSubmit = async () => {
  if (!currentRoleId.value) return
  try {
    permissionSubmitLoading.value = true
    // V2 API 设置角色权限
    await setRolePermissions(currentRoleId.value, selectedPermissionIds.value)
    ElMessage.success('权限分配成功')
    permissionDialogVisible.value = false
    loadRoleList()
  } catch (error: any) {
    ElMessage.error(error.message || '分配失败')
  } finally {
    permissionSubmitLoading.value = false
  }
}

// 数据权限相关
const handleDataPermissions = async (row: RoleResponse) => {
  try {
    currentRoleId.value = row.id
    currentRoleName.value = row.roleName
    const permissions = await getRoleDataPermissions(row.id)
    dataPermissions.value = permissions
    dataPermissionDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载数据权限失败')
  }
}

const handleDataPermissionSubmit = async () => {
  if (!currentRoleId.value) return
  try {
    dataPermissionSubmitLoading.value = true
    await saveRoleDataPermissions(currentRoleId.value, dataPermissions.value)
    ElMessage.success('数据权限保存成功')
    dataPermissionDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    dataPermissionSubmitLoading.value = false
  }
}

onMounted(() => {
  loadRoleList()
  loadPermissionTree()
})
</script>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
