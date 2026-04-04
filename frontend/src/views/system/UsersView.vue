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
          <label class="mb-1 block text-sm text-gray-600">账号</label>
          <input
            v-model="queryParams.username"
            type="text"
            placeholder="请输入账号"
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
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">账号</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">姓名</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">手机号</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">用户类型</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">角色</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">组织</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">最后登录</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">操作</th>
          </tr>
        </thead>
        <tbody v-if="loading">
          <tr>
            <td colspan="10" class="py-16 text-center">
              <Loader2 class="mx-auto h-8 w-8 animate-spin text-blue-600" />
              <div class="mt-2 text-sm text-gray-500">加载中...</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="userList.length === 0">
          <tr>
            <td colspan="10" class="py-16 text-center">
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
              <span v-if="getUserTypeName(row.userType)" class="rounded bg-purple-50 px-1.5 py-0.5 text-xs text-purple-600">{{ getUserTypeName(row.userType) }}</span>
              <span v-else class="text-gray-400">-</span>
            </td>
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
            <td class="px-4 py-3 text-gray-600">{{ row.orgUnitName || '-' }}</td>
            <td class="px-4 py-3 text-center">
              <button
                @click="handleStatusChange(row)"
                :class="[
                  'relative inline-flex h-5 w-9 items-center rounded-full transition-colors',
                  normalizeStatus(row.status) === 1 ? 'bg-blue-600' : 'bg-gray-300'
                ]"
              >
                <span
                  :class="[
                    'inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform',
                    normalizeStatus(row.status) === 1 ? 'translate-x-4' : 'translate-x-0.5'
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
                <!-- 数据范围按钮已移除 - 数据权限现在在角色级别配置 -->
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
          <div class="relative w-full max-w-2xl rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">{{ dialogTitle }}</h3>
              <button @click="dialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="max-h-[60vh] overflow-y-auto p-6">
              <div class="grid grid-cols-2 gap-4">
                <!-- 账号信息 -->
                <div class="col-span-2">
                  <label class="mb-1 block text-sm text-gray-700">
                    账号 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.username"
                    type="text"
                    placeholder="字母开头，仅限英文、数字、下划线（3-30位）"
                    :disabled="isEdit"
                    :class="[
                      'h-9 w-full rounded-lg border px-3 text-sm focus:outline-none',
                      isEdit ? 'border-gray-200 bg-gray-100 text-gray-500' : usernameError ? 'border-red-400 focus:border-red-500' : 'border-gray-300 focus:border-blue-500'
                    ]"
                    @input="validateUsername"
                  />
                  <p v-if="usernameError" class="mt-0.5 text-xs text-red-500">{{ usernameError }}</p>
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

                <!-- 基本信息 -->
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
                  <label class="mb-1 block text-sm text-gray-700">手机号</label>
                  <input
                    v-model="formData.phone"
                    type="text"
                    placeholder="请输入11位手机号"
                    :class="[
                      'h-9 w-full rounded-lg border px-3 text-sm focus:outline-none',
                      phoneError ? 'border-red-400 focus:border-red-500' : 'border-gray-300 focus:border-blue-500'
                    ]"
                    @input="validatePhone"
                  />
                  <p v-if="phoneError" class="mt-0.5 text-xs text-red-500">{{ phoneError }}</p>
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">邮箱</label>
                  <input
                    v-model="formData.email"
                    type="email"
                    placeholder="请输入邮箱"
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
                  <label class="mb-1 block text-sm text-gray-700">出生日期</label>
                  <input
                    v-model="formData.birthDate"
                    type="date"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div class="col-span-2">
                  <label class="mb-1 block text-sm text-gray-700">身份证号</label>
                  <input
                    v-model="formData.idCard"
                    type="text"
                    placeholder="请输入身份证号"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>

                <!-- 组织信息 -->
                <div class="col-span-2">
                  <label class="mb-1 block text-sm text-gray-700">
                    用户类型 <span class="text-red-500">*</span>
                  </label>
                  <select
                    v-model="formData.userTypeCode"
                    class="h-9 w-full rounded-lg border px-3 text-sm focus:outline-none"
                    :class="formData.userTypeCode ? 'border-gray-300 focus:border-blue-500' : 'border-red-300 focus:border-red-500'"
                  >
                    <option value="">请选择用户类型</option>
                    <option v-for="ut in userTypes" :key="ut.typeCode" :value="ut.typeCode">
                      {{ ut.typeName }}
                    </option>
                  </select>
                  <div v-if="selectedUserType" class="mt-1 flex gap-2">
                    <span v-if="selectedUserType.features?.requiresOrg" class="text-xs text-orange-500">需关联组织</span>
                    <span v-if="selectedUserType.features?.requiresPlace" class="text-xs text-orange-500">需关联场所</span>
                    <span v-if="selectedUserType.defaultRoleCodes?.length" class="text-xs text-gray-400">默认角色: {{ selectedUserType.defaultRoleCodes.join(', ') }}</span>
                  </div>
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">
                    所属组织
                    <span v-if="selectedUserType?.features?.requiresOrg" class="text-red-500">*</span>
                  </label>
                  <select
                    v-model="formData.orgUnitId"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  >
                    <option :value="undefined">请选择</option>
                    <option v-for="org in flatOrgUnits" :key="org.id" :value="org.id">
                      {{ org.label }}
                    </option>
                  </select>
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">
                    关联场所
                    <span v-if="selectedUserType?.features?.requiresPlace" class="text-red-500">*</span>
                  </label>
                  <select
                    v-model="formData.placeId"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  >
                    <option :value="undefined">请选择场所</option>
                    <option v-for="place in allPlaces" :key="place.placeId" :value="place.placeId">
                      {{ place.placeName }}
                    </option>
                  </select>
                </div>
                <div class="col-span-2">
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
          <div class="relative w-full max-w-3xl rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">分配角色 - {{ currentUserName }}</h3>
              <button @click="roleDialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="max-h-[70vh] overflow-y-auto p-6">
              <!-- 当前角色列表 -->
              <div v-if="currentUserRolesDetailed.length > 0" class="mb-5">
                <h4 class="mb-2 text-sm font-medium text-gray-700">当前角色</h4>
                <table class="w-full text-sm">
                  <thead>
                    <tr class="border-b border-gray-200 bg-gray-50">
                      <th class="px-3 py-2 text-left font-medium text-gray-600">角色名</th>
                      <th class="px-3 py-2 text-left font-medium text-gray-600">作用域</th>
                      <th class="px-3 py-2 text-left font-medium text-gray-600">过期时间</th>
                      <th class="px-3 py-2 text-left font-medium text-gray-600">原因</th>
                      <th class="px-3 py-2 text-center font-medium text-gray-600">状态</th>
                      <th class="px-3 py-2 text-center font-medium text-gray-600">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(ur, idx) in currentUserRolesDetailed"
                      :key="idx"
                      class="border-b border-gray-100"
                      :class="ur.isExpired ? 'opacity-50' : ''"
                    >
                      <td class="px-3 py-2 font-medium text-gray-900">{{ ur.roleName }}</td>
                      <td class="px-3 py-2 text-gray-600">
                        <span v-if="ur.scopeType === 'ALL'" class="rounded bg-blue-50 px-2 py-0.5 text-xs text-blue-600">全局</span>
                        <span v-else class="text-sm">{{ ur.scopeName || '未指定' }}</span>
                      </td>
                      <td class="px-3 py-2 text-gray-500">{{ ur.expiresAt ? ur.expiresAt.substring(0, 10) : '永久' }}</td>
                      <td class="px-3 py-2 text-gray-400 text-xs truncate max-w-[150px]" :title="ur.reason">
                        {{ ur.reason || '-' }}
                      </td>
                      <td class="px-3 py-2 text-center">
                        <span v-if="ur.isExpired" class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500">已过期</span>
                        <span v-else class="rounded bg-green-100 px-1.5 py-0.5 text-xs text-green-600">有效</span>
                      </td>
                      <td class="px-3 py-2 text-center">
                        <button
                          @click="handleRemoveSingleRole(ur)"
                          class="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-500"
                          title="移除"
                        >
                          <Trash2 class="h-3.5 w-3.5" />
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- 添加角色 -->
              <div class="mb-3 rounded-lg bg-blue-50 p-3 text-sm text-blue-700">
                勾选角色并选择作用域：「全局」不限范围，「指定组织」限定到某个组织节点及其子节点
              </div>
              <div class="max-h-72 space-y-2 overflow-y-auto">
                <div
                  v-for="role in allRoles"
                  :key="role.id"
                  class="rounded-lg border p-3"
                  :class="isRoleSelected(role.id) ? 'border-blue-500 bg-blue-50/50' : 'border-gray-200'"
                >
                  <div class="flex flex-wrap items-center gap-3">
                    <input
                      type="checkbox"
                      :checked="isRoleSelected(role.id)"
                      @change="toggleRole(role.id)"
                      class="h-4 w-4 shrink-0 rounded border-gray-300"
                    />
                    <span class="shrink-0 font-medium text-gray-900">{{ role.roleName }}</span>
                    <span v-if="role.roleCode" class="shrink-0 text-xs text-gray-500">{{ role.roleCode }}</span>
                  </div>
                  <!-- 作用域 + 过期 + 原因（固定第二行，避免抖动） -->
                  <div v-if="isRoleSelected(role.id)" class="mt-2 space-y-2 pl-7">
                    <!-- 作用域选择 -->
                    <div class="flex items-center gap-3 text-xs">
                      <span class="text-gray-500 w-14 shrink-0">作用域</span>
                      <label class="flex items-center gap-1 cursor-pointer">
                        <input type="radio" :name="'scope-' + role.id" value="ALL"
                          :checked="getRoleScope(role.id).scopeType === 'ALL'"
                          @change="updateRoleScopeType(role.id, 'ALL')" class="accent-blue-600" />
                        <span>全局</span>
                      </label>
                      <label class="flex items-center gap-1 cursor-pointer">
                        <input type="radio" :name="'scope-' + role.id" value="ORG_UNIT"
                          :checked="getRoleScope(role.id).scopeType === 'ORG_UNIT'"
                          @change="updateRoleScopeType(role.id, 'ORG_UNIT')" class="accent-blue-600" />
                        <span>指定组织</span>
                      </label>
                      <!-- 组织选择始终占位，禁用状态防抖动 -->
                      <select
                        :value="getRoleScope(role.id).scopeId"
                        @change="updateRoleScopeId(role.id, Number(($event.target as HTMLSelectElement).value))"
                        class="h-7 w-48 rounded border border-gray-300 px-2 text-xs"
                        :disabled="getRoleScope(role.id).scopeType !== 'ORG_UNIT'"
                        :class="getRoleScope(role.id).scopeType !== 'ORG_UNIT' ? 'opacity-30' : ''"
                      >
                        <option :value="0">请选择组织</option>
                        <option v-for="org in flatOrgUnits" :key="org.id" :value="org.id">
                          {{ org.label }}
                        </option>
                      </select>
                    </div>
                    <!-- 过期时间和原因 -->
                    <div class="flex flex-wrap items-center gap-3 text-xs">
                    <div class="flex items-center gap-1.5">
                      <label class="text-xs text-gray-500">过期时间</label>
                      <input
                        type="date"
                        :value="getRoleScope(role.id).expiresAt || ''"
                        @input="updateRoleExpiry(role.id, ($event.target as HTMLInputElement).value)"
                        class="h-7 rounded border border-gray-300 px-2 text-xs"
                        placeholder="空=永久"
                      />
                    </div>
                    <div class="flex items-center gap-1.5">
                      <label class="text-xs text-gray-500">授权原因</label>
                      <input
                        type="text"
                        :value="getRoleScope(role.id).reason || ''"
                        @input="updateRoleReason(role.id, ($event.target as HTMLInputElement).value)"
                        class="h-7 w-40 rounded border border-gray-300 px-2 text-xs"
                        placeholder="可选"
                      />
                    </div>
                  </div>
                </div>
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

    <!-- 数据范围对话框已移除 - 数据权限现在在角色级别配置 -->
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
  X,
  Loader2,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  Clock
} from 'lucide-vue-next'
import StatCard from '@/components/design-system/cards/StatCard.vue'
import { getEnabledUserTypes } from '@/api/userType'
import { getOrgUnitTree } from '@/api/organization'
import { universalPlaceApi } from '@/api/universalPlace'
import type { UserType as UserTypeItem } from '@/types/userType'
import type { OrgUnitTreeNode } from '@/types'
// User API
import {
  getUserPage,
  createUser,
  updateUser,
  deleteUser,
  batchDeleteUsers,
  resetPassword,
  updateUserStatus,
  getUserRoleAssignments,
  assignRoles
} from '@/api/user'
import type {
  User,
  UserQueryParams,
  UserFormData,
  UserListItem
} from '@/types/user'
import { getAllRoles, removeUserRoleWithScope } from '@/api/access'
import type { RoleResponse as Role } from '@/api/access'
import { useConfigStore } from '@/stores/config'

const configStore = useConfigStore()
const loading = ref(false)
const submitLoading = ref(false)
const roleSubmitLoading = ref(false)
const dialogVisible = ref(false)
const roleDialogVisible = ref(false)
// dataScopeDialogVisible 已移除 - 数据权限现在在角色级别配置
const isEdit = ref(false)
const selectedIds = ref<(number | string)[]>([])
const currentUserId = ref<number | string>()
const usernameError = ref('')
const phoneError = ref('')

const queryParams = reactive<UserQueryParams>({
  pageNum: 1,
  pageSize: configStore.defaultPageSize
})

const userList = ref<UserListItem[]>([])
const total = ref(0)
const allRoles = ref<Role[]>([])
// 角色分配（含作用域、过期时间、原因）
interface RoleAssignmentLocal {
  roleId: string | number
  scopeType: string
  scopeId: number | string
  expiresAt?: string
  reason?: string
}
const roleAssignments = ref<RoleAssignmentLocal[]>([])
const currentUserName = ref('')

// 当前用户的角色详细信息（用于显示表格）
interface UserRoleDetailed {
  roleId: string | number
  roleName: string
  roleCode?: string
  scopeType: string
  scopeId: number | string
  scopeName?: string
  expiresAt?: string
  isExpired: boolean
  assignedAt?: string
  reason?: string
}
const currentUserRolesDetailed = ref<UserRoleDetailed[]>([])
const userTypes = ref<UserTypeItem[]>([])
const orgTree = ref<OrgUnitTreeNode[]>([])
// 全量场所列表（独立于组织）
interface PlaceItem { placeId: number; placeName: string }
const allPlaces = ref<PlaceItem[]>([])

// 当前选中的用户类型
const selectedUserType = computed<UserTypeItem | null>(() => {
  if (!formData.userTypeCode) return null
  return userTypes.value.find(ut => ut.typeCode === formData.userTypeCode) || null
})

// 根据 typeCode 获取用户类型名称
const getUserTypeName = (typeCode?: string): string => {
  if (!typeCode) return ''
  const ut = userTypes.value.find(t => t.typeCode === typeCode)
  return ut?.typeName || typeCode
}

// 将组织树扁平化为列表（带缩进标记）
interface FlatOrgItem { id: number; label: string }
const flatOrgUnits = computed<FlatOrgItem[]>(() => {
  const result: FlatOrgItem[] = []
  const flatten = (nodes: OrgUnitTreeNode[], depth: number) => {
    for (const node of nodes) {
      const prefix = '\u00A0\u00A0'.repeat(depth)
      result.push({ id: node.id, label: prefix + (node.unitName || node.label || '') })
      if (node.children?.length) flatten(node.children, depth + 1)
    }
  }
  flatten(orgTree.value, 0)
  return result
})

// 将 status 统一为数字：1=启用, 2=禁用
const normalizeStatus = (status: any): number => {
  if (status === '启用' || status === 1) return 1
  if (status === '禁用' || status === 2) return 2
  return Number(status) || 1
}

// 统计数据
const enabledCount = computed(() => userList.value.filter(u => normalizeStatus(u.status) === 1).length)
const disabledCount = computed(() => userList.value.filter(u => normalizeStatus(u.status) === 2).length)
const todayLoginCount = ref(0)

const formData = reactive<UserFormData & { placeId?: number }>({
  username: '',
  realName: '',
  password: '',
  phone: '',
  email: '',
  employeeNo: '',
  gender: 1,
  birthDate: '',
  idCard: '',
  userTypeCode: '',
  orgUnitId: undefined,
  placeId: undefined,
  status: 1
})

const validateUsername = () => {
  const val = formData.username
  if (!val) {
    usernameError.value = ''
    return
  }
  if (!/^[a-zA-Z]/.test(val)) {
    usernameError.value = '必须以英文字母开头'
  } else if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(val)) {
    usernameError.value = '只能包含英文字母、数字和下划线'
  } else if (val.length < 3) {
    usernameError.value = '至少3个字符'
  } else if (val.length > 30) {
    usernameError.value = '最多30个字符'
  } else {
    usernameError.value = ''
  }
}

const validatePhone = () => {
  const val = formData.phone
  if (!val) {
    phoneError.value = ''
    return
  }
  if (!/^1[3-9]\d{9}$/.test(val)) {
    phoneError.value = '请输入正确的11位手机号'
  } else {
    phoneError.value = ''
  }
}

const dialogTitle = computed(() => (isEdit.value ? '编辑用户' : '新增用户'))

// 加载全量场所列表（独立于组织）
const loadAllPlaces = async () => {
  try {
    const tree = await universalPlaceApi.getTree()
    const result: PlaceItem[] = []
    const flatten = (nodes: any[], depth: number) => {
      for (const n of nodes) {
        const prefix = '\u00A0\u00A0'.repeat(depth)
        result.push({ placeId: n.id, placeName: prefix + (n.placeName || n.name || '') })
        if (n.children?.length) flatten(n.children, depth + 1)
      }
    }
    flatten(tree, 0)
    allPlaces.value = result
  } catch (error) {
    console.error('加载场所列表失败:', error)
  }
}

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
    // 统一 status 为数字格式
    userList.value = (res.records || []).map(u => ({ ...u, status: normalizeStatus(u.status) }))
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

const loadUserTypes = async () => {
  try {
    userTypes.value = await getEnabledUserTypes()
  } catch (error) {
    console.error('加载用户类型失败:', error)
  }
}

const loadOrgTree = async () => {
  try {
    orgTree.value = await getOrgUnitTree()
  } catch (error) {
    console.error('加载组织树失败:', error)
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
    selectedIds.value = userList.value.map(item => item.id)
  } else {
    selectedIds.value = []
  }
}

const handleSelectRow = (row: UserListItem) => {
  const id = row.id
  const index = selectedIds.value.indexOf(id)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
  } else {
    selectedIds.value.push(id)
  }
}

const handleAdd = () => {
  isEdit.value = false
  usernameError.value = ''
  phoneError.value = ''
  Object.assign(formData, {
    username: '',
    realName: '',
    password: '',
    phone: '',
    email: '',
    employeeNo: '',
    gender: 1,
    birthDate: '',
    idCard: '',
    userTypeCode: '',
    orgUnitId: undefined,
    placeId: undefined,
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = async (row: UserListItem) => {
  if (!row.id) {
    ElMessage.error('用户ID无效')
    return
  }
  isEdit.value = true
  usernameError.value = ''
  phoneError.value = ''
  Object.assign(formData, {
    username: row.username,
    realName: row.realName,
    phone: row.phone || '',
    email: row.email || '',
    employeeNo: row.employeeNo || '',
    gender: row.gender,
    birthDate: row.birthDate ? String(row.birthDate).substring(0, 10) : '',
    idCard: row.idCard || '',
    userTypeCode: row.userType || '',
    orgUnitId: row.orgUnitId || undefined,
    status: normalizeStatus(row.status)
  })
  currentUserId.value = row.id
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.username) {
    ElMessage.error('请输入账号')
    return
  }
  if (!isEdit.value && !/^[a-zA-Z][a-zA-Z0-9_]*$/.test(formData.username)) {
    ElMessage.error('账号只能包含英文字母、数字和下划线，且必须以字母开头')
    return
  }
  if (!isEdit.value && (formData.username.length < 3 || formData.username.length > 30)) {
    ElMessage.error('账号长度须为3-30位')
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
  if (formData.phone && !/^1[3-9]\d{9}$/.test(formData.phone)) {
    ElMessage.error('手机号格式不正确')
    return
  }
  if (usernameError.value || phoneError.value) {
    ElMessage.error(usernameError.value || phoneError.value)
    return
  }
  if (!formData.userTypeCode) {
    ElMessage.error('请选择用户类型')
    return
  }
  if (selectedUserType.value?.features?.requiresOrg && !formData.orgUnitId) {
    ElMessage.error('该用户类型需要关联组织')
    return
  }
  if (selectedUserType.value?.features?.requiresPlace && !formData.placeId) {
    ElMessage.error('该用户类型需要关联场所')
    return
  }
  try {
    submitLoading.value = true
    // 清理空字符串为 undefined，避免后端 @Pattern 对空串校验失败
    const payload = { ...formData } as any
    if (!payload.phone) payload.phone = undefined
    if (!payload.email) payload.email = undefined
    if (!payload.employeeNo) payload.employeeNo = undefined
    if (!payload.idCard) payload.idCard = undefined
    if (!payload.birthDate) payload.birthDate = undefined
    if (isEdit.value && currentUserId.value) {
      await updateUser(currentUserId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createUser(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    queryParams.pageNum = 1
    loadUserList()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: UserListItem) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确定要删除用户"${row.realName}"吗?`, '删除确认', { type: 'warning' })
    await deleteUser(row.id)
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

const handleStatusChange = async (row: UserListItem) => {
  if (!row.id) return
  const currentStatus = normalizeStatus(row.status)
  const newStatus = currentStatus === 1 ? 2 : 1
  try {
    await updateUserStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success('状态已更新')
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
    loadUserList()
  }
}

const handleResetPassword = async (row: UserListItem) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确定要重置用户"${row.realName}"的密码吗?`, '重置密码', { type: 'warning' })
    const newPassword = await resetPassword(row.id)
    ElMessageBox.alert(
      `新密码为: <strong style="font-size:16px;color:#409EFF">${newPassword || 'admin123'}</strong><br/><span style="color:#999;font-size:12px">请妥善保管并通知用户</span>`,
      '密码重置成功',
      { dangerouslyUseHTMLString: true, confirmButtonText: '我已记住' }
    )
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '重置失败')
  }
}

// ==================== 角色 scope 辅助方法 ====================

const isRoleSelected = (roleId: string | number) => {
  return roleAssignments.value.some(a => String(a.roleId) === String(roleId))
}

const getRoleScope = (roleId: string | number) => {
  return roleAssignments.value.find(a => String(a.roleId) === String(roleId)) || { scopeType: 'ALL', scopeId: 0, expiresAt: '', reason: '' }
}

const toggleRole = (roleId: string | number) => {
  const idx = roleAssignments.value.findIndex(a => String(a.roleId) === String(roleId))
  if (idx > -1) {
    roleAssignments.value.splice(idx, 1)
  } else {
    roleAssignments.value.push({ roleId: String(roleId), scopeType: 'ALL', scopeId: 0, expiresAt: '', reason: '' })
  }
}

const updateRoleScopeType = (roleId: string | number, scopeType: string) => {
  const a = roleAssignments.value.find(a => String(a.roleId) === String(roleId))
  if (a) {
    a.scopeType = scopeType
    if (scopeType === 'ALL') a.scopeId = 0
  }
}

const updateRoleScopeId = (roleId: string | number, scopeId: number) => {
  const a = roleAssignments.value.find(a => String(a.roleId) === String(roleId))
  if (a) a.scopeId = scopeId
}

const updateRoleExpiry = (roleId: string | number, expiresAt: string) => {
  const a = roleAssignments.value.find(a => String(a.roleId) === String(roleId))
  if (a) a.expiresAt = expiresAt || undefined
}

const updateRoleReason = (roleId: string | number, reason: string) => {
  const a = roleAssignments.value.find(a => String(a.roleId) === String(roleId))
  if (a) a.reason = reason || undefined
}

// ==================== 角色分配操作 ====================

const handleAssignRoles = async (row: UserListItem) => {
  if (!row.id) return
  try {
    currentUserId.value = row.id
    currentUserName.value = row.realName || row.username || ''
    const existing = await getUserRoleAssignments(row.id)
    const existingArr = Array.isArray(existing) ? existing : []

    // Build detailed roles table
    const now = new Date().toISOString()
    currentUserRolesDetailed.value = existingArr.map((a: any) => {
      const roleName = a.roleName || allRoles.value.find(r => String(r.id) === String(a.roleId || a.id))?.roleName || '未知角色'
      const roleCode = a.roleCode || allRoles.value.find(r => String(r.id) === String(a.roleId || a.id))?.roleCode || ''
      const scopeName = a.scopeName || (a.scopeType === 'ORG_UNIT' ? findOrgName(a.scopeId) : undefined)
      return {
        roleId: String(a.roleId || a.id),
        roleName,
        roleCode,
        scopeType: a.scopeType || 'ALL',
        scopeId: a.scopeId || 0,
        scopeName,
        expiresAt: a.expiresAt,
        isExpired: a.expiresAt ? a.expiresAt < now : false,
        assignedAt: a.assignedAt,
        reason: a.reason
      }
    })

    roleAssignments.value = existingArr.map((a: any) => ({
      roleId: String(a.roleId || a.id),
      scopeType: a.scopeType || 'ALL',
      scopeId: a.scopeId || 0,
      expiresAt: a.expiresAt ? String(a.expiresAt).substring(0, 10) : undefined,
      reason: a.reason || undefined
    }))
    roleDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载角色失败')
  }
}

// 根据 orgId 查找组织名称
const findOrgName = (orgId: number | string): string => {
  if (!orgId || orgId === 0) return ''
  const org = flatOrgUnits.value.find(o => o.id === Number(orgId))
  return org?.label?.trim() || ''
}

// 移除单个角色分配
const handleRemoveSingleRole = async (ur: UserRoleDetailed) => {
  if (!currentUserId.value) return
  try {
    await ElMessageBox.confirm(`确定移除角色"${ur.roleName}"吗？`, '移除确认', { type: 'warning' })
    await removeUserRoleWithScope(currentUserId.value, ur.roleId, ur.scopeType, ur.scopeId)
    ElMessage.success('角色已移除')
    // Reload the role dialog
    const row = userList.value.find(u => u.id === currentUserId.value)
    if (row) await handleAssignRoles(row)
    loadUserList()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '移除失败')
  }
}

const handleRoleSubmit = async () => {
  if (!currentUserId.value) return
  // 校验：ORG_UNIT scope 必须选了具体组织
  const invalid = roleAssignments.value.find(a => a.scopeType === 'ORG_UNIT' && (!a.scopeId || a.scopeId === 0))
  if (invalid) {
    const role = allRoles.value.find(r => String(r.id) === String(invalid.roleId))
    ElMessage.error(`角色"${role?.roleName || invalid.roleId}"选择了指定组织但未选择具体组织`)
    return
  }
  try {
    roleSubmitLoading.value = true
    // Include expiresAt and reason in the assignment payload
    const payload = roleAssignments.value.map(a => ({
      roleId: a.roleId,
      scopeType: a.scopeType || 'ALL',
      scopeId: a.scopeId || 0,
      expiresAt: a.expiresAt || undefined,
      reason: a.reason || undefined
    }))
    await assignRoles(currentUserId.value, payload)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    loadUserList()
  } catch (error: any) {
    ElMessage.error(error.message || '分配失败')
  } finally {
    roleSubmitLoading.value = false
  }
}

// handleDataScope 已移除 - 数据权限现在在角色级别配置

onMounted(() => {
  loadUserList()
  loadAllRoles()
  loadUserTypes()
  loadOrgTree()
  loadAllPlaces()
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
/* v2-force-refresh */
</style>
