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
                :checked="isRoleSelected(row.id)"
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
                <div v-if="isEdit">
                  <label class="mb-1 block text-sm text-gray-700">角色编码</label>
                  <input
                    :value="formData.roleCode"
                    type="text"
                    disabled
                    class="h-9 w-full rounded-lg border border-gray-200 bg-gray-100 px-3 font-mono text-sm text-gray-500"
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
                          :checked="isPermissionSelected(permission.id)"
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
                            :checked="isPermissionSelected(child.id)"
                            @change="handlePermissionCheck(child.id)"
                            class="h-4 w-4 rounded border-gray-300"
                          />
                          <span class="text-gray-700">{{ child.permissionName }}</span>
                          
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

    <!-- 数据权限配置对话框 V3 - 卡片式设计 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="dataPermissionDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="dataPermissionDialogVisible = false"></div>
          <div class="relative w-full max-w-3xl rounded-xl bg-white shadow-2xl">
            <!-- 头部 -->
            <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
              <div class="flex items-center gap-3">
                <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-blue-500 to-blue-600">
                  <Shield class="h-5 w-5 text-white" />
                </div>
                <div>
                  <h3 class="text-lg font-semibold text-gray-900">数据权限配置</h3>
                  <p class="text-sm text-gray-500">{{ currentRoleName }}</p>
                </div>
              </div>
              <button @click="dataPermissionDialogVisible = false" class="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
                <X class="h-5 w-5" />
              </button>
            </div>

            <!-- 快捷操作 -->
            <div class="flex items-center gap-2 border-b border-gray-100 bg-gray-50/50 px-6 py-3">
              <span class="text-sm text-gray-500">快捷设置：</span>
              <button
                v-for="opt in quickScopeOptions"
                :key="opt.code"
                @click="batchSetScope(opt.code)"
                class="rounded-full border border-gray-200 bg-white px-3 py-1 text-xs font-medium text-gray-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-600"
              >
                {{ opt.label }}
              </button>
            </div>

            <!-- 模块卡片列表 -->
            <div class="max-h-[500px] overflow-y-auto p-6">
              <div v-for="(modules, domain) in groupedModules" :key="domain" class="mb-6 last:mb-0">
                <!-- 领域标题 -->
                <div class="mb-3 flex items-center gap-2">
                  <component :is="getDomainIcon(domain)" class="h-4 w-4 text-gray-400" />
                  <span class="text-sm font-medium text-gray-500">{{ getDomainLabel(domain) }}</span>
                </div>

                <!-- 模块卡片 -->
                <div class="space-y-3">
                  <div
                    v-for="module in modules"
                    :key="module.code"
                    class="rounded-lg border border-gray-200 bg-white transition-shadow hover:shadow-sm"
                    :class="{ 'ring-2 ring-blue-500 ring-offset-1': expandedCustomModule === module.code }"
                  >
                    <!-- 卡片头部 -->
                    <div class="flex items-center justify-between px-4 py-3">
                      <div class="flex items-center gap-3">
                        <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-gray-100">
                          <component :is="getModuleIcon(module.code)" class="h-4 w-4 text-gray-500" />
                        </div>
                        <span class="font-medium text-gray-900">{{ module.name }}</span>
                      </div>

                      <!-- 范围选择下拉 -->
                      <div class="flex items-center gap-2">
                        <select
                          :value="getModuleScope(module.code)"
                          @change="handleScopeChange(module.code, ($event.target as HTMLSelectElement).value)"
                          class="h-9 rounded-lg border border-gray-200 bg-white px-3 pr-8 text-sm font-medium text-gray-700 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                        >
                          <option v-for="scope in dataScopeOptions" :key="scope.scopeCode" :value="scope.scopeCode">
                            {{ scope.scopeName }}
                          </option>
                        </select>

                        <!-- 自定义范围展开按钮 -->
                        <button
                          v-if="getModuleScope(module.code) === 'CUSTOM'"
                          @click="toggleCustomScopeInline(module.code)"
                          class="flex h-9 items-center gap-1.5 rounded-lg px-3 text-sm font-medium transition"
                          :class="expandedCustomModule === module.code
                            ? 'bg-blue-600 text-white'
                            : 'bg-blue-50 text-blue-600 hover:bg-blue-100'"
                        >
                          <span>已选 {{ getCustomScopeCount(module.code) }} 项</span>
                          <ChevronDown
                            class="h-4 w-4 transition-transform"
                            :class="{ 'rotate-180': expandedCustomModule === module.code }"
                          />
                        </button>
                      </div>
                    </div>

                    <!-- 自定义范围选择器（展开时显示） -->
                    <Transition name="expand">
                      <div v-if="expandedCustomModule === module.code && getModuleScope(module.code) === 'CUSTOM'" class="border-t border-gray-100 bg-gray-50/50 p-4">
                        <!-- 统一树形选择器 -->
                        <div class="mb-3 flex items-center justify-between">
                          <span class="text-sm text-gray-600">选择可访问的数据范围</span>
                          <div class="flex items-center gap-2">
                            <button @click="handleExpandAllScope" class="rounded border border-gray-200 bg-white px-2 py-1 text-xs text-gray-600 hover:bg-gray-50">
                              全部展开
                            </button>
                            <button @click="handleCollapseAllScope" class="rounded border border-gray-200 bg-white px-2 py-1 text-xs text-gray-600 hover:bg-gray-50">
                              全部折叠
                            </button>
                            <button @click="handleClearScope" class="rounded border border-gray-200 bg-white px-2 py-1 text-xs text-gray-600 hover:bg-gray-50">
                              清空选择
                            </button>
                          </div>
                        </div>

                        <!-- 树形选择区域 -->
                        <div class="max-h-64 overflow-y-auto rounded-lg border border-gray-200 bg-white">
                          <div v-if="unifiedTreeLoading" class="flex items-center justify-center py-8">
                            <Loader2 class="h-6 w-6 animate-spin text-gray-400" />
                          </div>
                          <div v-else-if="unifiedTreeData.length === 0" class="py-8 text-center text-sm text-gray-500">
                            暂无数据
                          </div>
                          <div v-else class="p-3">
                            <!-- 统一树形结构 -->
                            <div v-for="org in unifiedTreeData" :key="'org-' + org.id" class="mb-2 last:mb-0">
                              <!-- 组织单元节点 -->
                              <div class="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-gray-50">
                                <button
                                  @click="toggleUnifiedNode('org', org.id)"
                                  class="flex h-5 w-5 items-center justify-center rounded text-gray-400 hover:bg-gray-200"
                                >
                                  <ChevronDown
                                    v-if="org.children?.length || org.grades?.length"
                                    class="h-4 w-4 transition-transform"
                                    :class="{ '-rotate-90': !isUnifiedNodeExpanded('org', org.id) }"
                                  />
                                </button>
                                <input
                                  type="checkbox"
                                  :checked="isNodeSelected('org', org.id)"
                                  @change="toggleNodeSelection('org', org.id)"
                                  class="h-4 w-4 rounded border-gray-300 text-blue-600"
                                />
                                <Building2 class="h-4 w-4 text-blue-500" />
                                <span class="text-sm font-medium text-gray-700">{{ org.name }}</span>
                                <span class="text-xs text-gray-400">组织单元</span>
                              </div>

                              <!-- 子组织单元和年级 -->
                              <div v-if="isUnifiedNodeExpanded('org', org.id)" class="ml-7 border-l border-gray-200 pl-3">
                                <!-- 子组织单元 -->
                                <template v-if="org.children?.length">
                                  <div v-for="child in org.children" :key="'org-' + child.id" class="mt-1">
                                    <div class="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-gray-50">
                                      <button
                                        @click="toggleUnifiedNode('org', child.id)"
                                        class="flex h-5 w-5 items-center justify-center rounded text-gray-400 hover:bg-gray-200"
                                      >
                                        <ChevronDown
                                          v-if="child.children?.length"
                                          class="h-4 w-4 transition-transform"
                                          :class="{ '-rotate-90': !isUnifiedNodeExpanded('org', child.id) }"
                                        />
                                      </button>
                                      <input
                                        type="checkbox"
                                        :checked="isNodeSelected('org', child.id)"
                                        @change="toggleNodeSelection('org', child.id)"
                                        class="h-4 w-4 rounded border-gray-300 text-blue-600"
                                      />
                                      <Building2 class="h-4 w-4 text-blue-400" />
                                      <span class="text-sm text-gray-600">{{ child.name }}</span>
                                    </div>
                                  </div>
                                </template>

                                <!-- 年级节点 -->
                                <template v-if="org.grades?.length">
                                  <div v-for="grade in org.grades" :key="'grade-' + grade.id" class="mt-1">
                                    <div class="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-gray-50">
                                      <button
                                        @click="toggleUnifiedNode('grade', grade.id)"
                                        class="flex h-5 w-5 items-center justify-center rounded text-gray-400 hover:bg-gray-200"
                                      >
                                        <ChevronDown
                                          v-if="grade.classes?.length"
                                          class="h-4 w-4 transition-transform"
                                          :class="{ '-rotate-90': !isUnifiedNodeExpanded('grade', grade.id) }"
                                        />
                                      </button>
                                      <input
                                        type="checkbox"
                                        :checked="isNodeSelected('grade', grade.id)"
                                        @change="toggleNodeSelection('grade', grade.id)"
                                        class="h-4 w-4 rounded border-gray-300 text-green-600"
                                      />
                                      <GraduationCap class="h-4 w-4 text-green-500" />
                                      <span class="text-sm text-gray-600">{{ grade.name }}</span>
                                      <span class="text-xs text-gray-400">年级</span>
                                    </div>

                                    <!-- 班级节点 -->
                                    <div v-if="isUnifiedNodeExpanded('grade', grade.id) && grade.classes?.length" class="ml-7 border-l border-gray-200 pl-3">
                                      <div v-for="cls in grade.classes" :key="'class-' + cls.id" class="mt-1">
                                        <div class="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-gray-50">
                                          <div class="h-5 w-5"></div>
                                          <input
                                            type="checkbox"
                                            :checked="isNodeSelected('class', cls.id)"
                                            @change="toggleNodeSelection('class', cls.id)"
                                            class="h-4 w-4 rounded border-gray-300 text-orange-600"
                                          />
                                          <Users class="h-4 w-4 text-orange-500" />
                                          <span class="text-sm text-gray-600">{{ cls.name }}</span>
                                          <span class="text-xs text-gray-400">班级</span>
                                        </div>
                                      </div>
                                    </div>
                                  </div>
                                </template>
                              </div>
                            </div>
                          </div>
                        </div>

                        <!-- 选择统计 -->
                        <div class="mt-3 flex items-center justify-between rounded-lg bg-blue-50 px-3 py-2">
                          <div class="flex items-center gap-4 text-xs">
                            <span class="flex items-center gap-1">
                              <Building2 class="h-3.5 w-3.5 text-blue-500" />
                              <span class="text-gray-600">组织单元:</span>
                              <span class="font-semibold text-blue-600">{{ selectedOrgUnitIds.length }}</span>
                            </span>
                            <span class="flex items-center gap-1">
                              <GraduationCap class="h-3.5 w-3.5 text-green-500" />
                              <span class="text-gray-600">年级:</span>
                              <span class="font-semibold text-green-600">{{ selectedGradeIds.length }}</span>
                            </span>
                            <span class="flex items-center gap-1">
                              <Users class="h-3.5 w-3.5 text-orange-500" />
                              <span class="text-gray-600">班级:</span>
                              <span class="font-semibold text-orange-600">{{ selectedClassIds.length }}</span>
                            </span>
                          </div>
                          <button
                            @click="confirmInlineCustomScope"
                            class="rounded-lg bg-blue-600 px-4 py-1.5 text-xs font-medium text-white hover:bg-blue-700"
                          >
                            确认选择
                          </button>
                        </div>
                      </div>
                    </Transition>
                  </div>
                </div>
              </div>

              <!-- 空状态 -->
              <div v-if="Object.keys(groupedModules).length === 0" class="py-12 text-center">
                <Loader2 class="mx-auto h-8 w-8 animate-spin text-gray-300" />
                <p class="mt-3 text-sm text-gray-500">加载中...</p>
              </div>
            </div>

            <!-- 底部操作栏 -->
            <div class="flex items-center justify-end gap-3 border-t border-gray-100 bg-gray-50/50 px-6 py-4">
              <button
                @click="dataPermissionDialogVisible = false"
                class="h-10 rounded-lg border border-gray-200 bg-white px-5 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                @click="handleDataPermissionSubmit"
                :disabled="dataPermissionSubmitLoading"
                class="inline-flex h-10 items-center gap-2 rounded-lg bg-blue-600 px-5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                <Loader2 v-if="dataPermissionSubmitLoading" class="h-4 w-4 animate-spin" />
                保存配置
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 自定义范围选择对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="customScopeDialogVisible" class="fixed inset-0 z-[60] flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="customScopeDialogVisible = false"></div>
          <div class="relative w-full max-w-lg rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">选择可访问的组织单元</h3>
              <button @click="customScopeDialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="p-6">
              <p class="mb-4 text-sm text-gray-500">
                请选择该角色可以访问的组织单元（组织/年级/班级）。
              </p>
              <!-- 快捷操作 -->
              <div class="mb-3 flex gap-2">
                <button
                  @click="handleExpandAllOrg"
                  class="inline-flex h-7 items-center gap-1 rounded border border-gray-300 px-2 text-xs text-gray-600 hover:bg-gray-50"
                >
                  <FolderOpen class="h-3.5 w-3.5" />
                  展开全部
                </button>
                <button
                  @click="handleCollapseAllOrg"
                  class="inline-flex h-7 items-center gap-1 rounded border border-gray-300 px-2 text-xs text-gray-600 hover:bg-gray-50"
                >
                  <Folder class="h-3.5 w-3.5" />
                  折叠全部
                </button>
                <button
                  @click="handleSelectAllOrg"
                  class="inline-flex h-7 items-center gap-1 rounded border border-gray-300 px-2 text-xs text-gray-600 hover:bg-gray-50"
                >
                  <CheckSquare class="h-3.5 w-3.5" />
                  全选
                </button>
                <button
                  @click="handleClearAllOrg"
                  class="inline-flex h-7 items-center gap-1 rounded border border-gray-300 px-2 text-xs text-gray-600 hover:bg-gray-50"
                >
                  <Square class="h-3.5 w-3.5" />
                  清空
                </button>
              </div>
              <!-- 组织树选择器 -->
              <div class="max-h-72 overflow-y-auto rounded-lg border border-gray-200 bg-gray-50 p-3">
                <div v-if="orgTreeLoading" class="py-8 text-center">
                  <Loader2 class="mx-auto h-6 w-6 animate-spin text-gray-400" />
                  <p class="mt-2 text-sm text-gray-500">加载组织架构...</p>
                </div>
                <div v-else-if="orgTreeData.length === 0" class="py-8 text-center text-gray-500">
                  <Building2 class="mx-auto h-8 w-8 text-gray-300" />
                  <p class="mt-2 text-sm">暂无组织架构数据</p>
                </div>
                <div v-else>
                  <OrgTreeNode
                    v-for="node in orgTreeData"
                    :key="node.id"
                    :node="node"
                    :selected-ids="selectedOrgUnitIds"
                    :expanded-keys="expandedOrgKeys"
                    @toggle="toggleOrgNode"
                    @check="handleOrgNodeCheck"
                  />
                </div>
              </div>
              <div class="mt-3 text-sm text-gray-500">
                已选择 <span class="font-medium text-blue-600">{{ selectedOrgUnitIds.length }}</span> 个组织单元
              </div>
            </div>
            <div class="flex items-center justify-end gap-3 border-t border-gray-200 px-6 py-4">
              <button
                @click="customScopeDialogVisible = false"
                class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                @click="confirmCustomScope"
                class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
              >
                确定
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
  Home,
  ChevronDown
} from 'lucide-vue-next'
// DDD API
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
  dataPermissionApi,
  type RoleResponse
} from '@/api/access'
import { getOrgUnitTree, getAllGrades, getAllClasses, type Grade, type SchoolClass } from '@/api/organization'
import type { OrgUnitTreeNode } from '@/types'
import OrgTreeNode from '@/components/common/OrgTreeNode.vue'
import type {
  CreateRoleRequest,
  UpdateRoleRequest,
  RoleQueryParams,
  RolePermissionConfig,
  ModulePermission,
  DataScopeOption,
  DataModuleDTO,
  ScopeItem
} from '@/types/access'

const loading = ref(false)
const submitLoading = ref(false)
const permissionSubmitLoading = ref(false)
const dataPermissionSubmitLoading = ref(false)
const dialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const dataPermissionDialogVisible = ref(false)
const customScopeDialogVisible = ref(false)
const isEdit = ref(false)
const currentRoleName = ref('')
const selectedIds = ref<(string | number)[]>([])
const currentRoleId = ref<string | number>()
const expandedModules = ref<string[]>([])

// 数据权限相关状态
const groupedModules = ref<Record<string, { code: string; name: string; domainCode: string }[]>>({})
const domainNameMap = ref<Record<string, string>>({})
const dataScopeOptions = ref<DataScopeOption[]>([])
const rolePermissionConfig = ref<RolePermissionConfig | null>(null)
const currentCustomScopeModule = ref<string>('')

// 自定义范围相关状态
const orgTreeData = ref<OrgUnitTreeNode[]>([])
const orgTreeLoading = ref(false)
const selectedOrgUnitIds = ref<number[]>([])
const expandedOrgKeys = ref<number[]>([])
const expandedCustomModule = ref<string>('') // 当前展开的自定义范围模块

// 多维度自定义范围状态
const gradesData = ref<Grade[]>([])
const classesData = ref<SchoolClass[]>([])
const gradesLoading = ref(false)
const classesLoading = ref(false)
const selectedGradeIds = ref<number[]>([])
const selectedClassIds = ref<number[]>([])
const classFilterGradeId = ref<number | null>(null) // 班级筛选的年级

// V3 统一树形结构状态
interface UnifiedTreeNode {
  id: number
  name: string
  children?: UnifiedTreeNode[]
  grades?: { id: number; name: string; classes?: { id: number; name: string }[] }[]
}
const unifiedTreeData = ref<UnifiedTreeNode[]>([])
const unifiedTreeLoading = ref(false)
const unifiedExpandedNodes = ref<Set<string>>(new Set())

// 快捷设置选项
const quickScopeOptions = [
  { code: 'ALL', label: '全部数据' },
  { code: 'DEPARTMENT_AND_BELOW', label: '本组织及以下' },
  { code: 'SELF', label: '仅本人' }
]

// 查询参数
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
const selectedPermissionIds = ref<(string | number)[]>([])

// 统计数据
const enabledCount = computed(() => roleList.value.filter(r => r.isEnabled !== false).length)
const disabledCount = computed(() => roleList.value.filter(r => r.isEnabled === false).length)
const systemRoleCount = computed(() => roleList.value.filter(r => r.isSystem === true).length)

// 表单数据
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

// 从 groupedModules 构建模块名称映射（替代硬编码 moduleConfig）
const moduleNameMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  for (const modules of Object.values(groupedModules.value)) {
    for (const m of modules) {
      map[m.code] = m.name
    }
  }
  return map
})

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
  const selectedIdStrings = selectedPermissionIds.value.map(String)
  moduleMap.forEach((permissions, code) => {
    const allIds = getAllPermissionIds(permissions)
    modules.push({
      code,
      name: moduleNameMap.value[code] || code,
      count: allIds.length,
      permissions,
      checked: allIds.every(id => selectedIdStrings.includes(String(id)))
    })
  })
  return modules.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
})

const getAllPermissionIds = (permissions: any[]): (string | number)[] => {
  const ids: (string | number)[] = []
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

// 检查角色ID是否已选中（处理字符串/数字混合比较）
const isRoleSelected = (id: string | number): boolean => {
  return selectedIds.value.some(sid => String(sid) === String(id))
}

const handleSelectAll = (e: Event) => {
  selectedIds.value = (e.target as HTMLInputElement).checked ? roleList.value.map(r => r.id) : []
}

const handleSelectRow = (row: RoleResponse) => {
  const idx = selectedIds.value.findIndex(sid => String(sid) === String(row.id))
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
  if (!formData.roleName) {
    ElMessage.error('请填写角色名称')
    return
  }
  try {
    submitLoading.value = true
    if (isEdit.value && currentRoleId.value) {
      // 更新请求
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
      // 创建请求
      const createData: CreateRoleRequest = {
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
    // 获取角色权限ID
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
  const allIdStrings = allIds.map(String)
  if (module.checked) {
    selectedPermissionIds.value = selectedPermissionIds.value.filter(id => !allIdStrings.includes(String(id)))
  } else {
    const selectedIdStrings = selectedPermissionIds.value.map(String)
    selectedPermissionIds.value.push(...allIds.filter(id => !selectedIdStrings.includes(String(id))))
  }
}

// 检查权限ID是否已选中（处理字符串/数字混合比较）
const isPermissionSelected = (id: string | number): boolean => {
  return selectedPermissionIds.value.some(pid => String(pid) === String(id))
}

const handlePermissionCheck = (id: string | number) => {
  const idx = selectedPermissionIds.value.findIndex(pid => String(pid) === String(id))
  idx > -1 ? selectedPermissionIds.value.splice(idx, 1) : selectedPermissionIds.value.push(id)
}

const handlePermissionSubmit = async () => {
  if (!currentRoleId.value) return
  try {
    permissionSubmitLoading.value = true
    // 设置角色权限
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

// ==================== 数据权限相关 ====================

// 获取领域标签（从 API 加载的 domainNameMap 中获取）
const getDomainLabel = (domain: string): string => {
  return domainNameMap.value[domain] || domain
}

// 获取领域图标
const getDomainIcon = (domain: string) => {
  const icons: Record<string, any> = {
    organization: Building2,
    inspection: CheckSquare,
    evaluation: BarChart3,
    task: BookOpen
  }
  return icons[domain] || Folder
}

// 获取模块的当前范围
const getModuleScope = (moduleCode: string): string => {
  if (!rolePermissionConfig.value) return 'SELF'
  const mp = rolePermissionConfig.value.modulePermissions.find(p => p.moduleCode === moduleCode)
  return mp?.scopeCode || 'SELF'
}

// 设置模块的范围
const setModuleScope = (moduleCode: string, scopeCode: string) => {
  if (!rolePermissionConfig.value) return
  const mp = rolePermissionConfig.value.modulePermissions.find(p => p.moduleCode === moduleCode)
  if (mp) {
    mp.scopeCode = scopeCode
    // 如果不是自定义范围，清空范围项
    if (scopeCode !== 'CUSTOM') {
      mp.scopeItems = []
    }
  }
}

// 处理单选按钮范围变更
const handleScopeChange = async (moduleCode: string, scopeCode: string) => {
  setModuleScope(moduleCode, scopeCode)
  // 如果选择自定义范围，自动展开内联选择器
  if (scopeCode === 'CUSTOM') {
    expandedCustomModule.value = moduleCode

    // 构建统一树形结构（内联选择器使用 unifiedTreeData）
    if (unifiedTreeData.value.length === 0) {
      await buildUnifiedTree()
    }

    // 加载当前模块已选中的范围项，按类型拆分到各选中数组
    const scopeItems = getModuleScopeItems(moduleCode)
    selectedOrgUnitIds.value = scopeItems.filter(i => i.itemTypeCode === 'ORG_UNIT').map(i => i.scopeId)
    selectedGradeIds.value = scopeItems.filter(i => i.itemTypeCode === 'GRADE').map(i => i.scopeId)
    selectedClassIds.value = scopeItems.filter(i => i.itemTypeCode === 'CLASS').map(i => i.scopeId)
  } else {
    // 如果切换到其他范围，关闭内联选择器
    if (expandedCustomModule.value === moduleCode) {
      expandedCustomModule.value = ''
    }
  }
}

// 批量设置所有模块的范围
const batchSetScope = (scopeCode: string) => {
  if (!rolePermissionConfig.value) return
  rolePermissionConfig.value.modulePermissions.forEach(mp => {
    mp.scopeCode = scopeCode
    if (scopeCode !== 'CUSTOM') {
      mp.scopeItems = []
    }
  })
  // 关闭任何展开的自定义选择器
  expandedCustomModule.value = ''
  ElMessage.success(`已将所有模块设为"${dataScopeOptions.value.find(s => s.scopeCode === scopeCode)?.scopeName || scopeCode}"`)
}

// 切换内联自定义范围选择器
const toggleCustomScopeInline = async (moduleCode: string) => {
  if (expandedCustomModule.value === moduleCode) {
    // 已展开则折叠
    expandedCustomModule.value = ''
  } else {
    // 展开新的
    expandedCustomModule.value = moduleCode

    // 构建统一树形结构
    if (unifiedTreeData.value.length === 0) {
      await buildUnifiedTree()
    }

    // 加载当前模块已选中的范围项，按类型拆分
    const scopeItems = getModuleScopeItems(moduleCode)
    selectedOrgUnitIds.value = scopeItems.filter(i => i.itemTypeCode === 'ORG_UNIT').map(i => i.scopeId)
    selectedGradeIds.value = scopeItems.filter(i => i.itemTypeCode === 'GRADE').map(i => i.scopeId)
    selectedClassIds.value = scopeItems.filter(i => i.itemTypeCode === 'CLASS').map(i => i.scopeId)
  }
}

// 确认内联自定义范围选择
const confirmInlineCustomScope = () => {
  if (!rolePermissionConfig.value || !expandedCustomModule.value) return
  // 保存选中的范围到当前模块配置（使用 scopeItems）
  const mp = rolePermissionConfig.value.modulePermissions.find(
    p => p.moduleCode === expandedCustomModule.value
  )
  if (mp) {
    const items: ScopeItem[] = []

    selectedOrgUnitIds.value.forEach(id => {
      items.push({ itemTypeCode: 'ORG_UNIT', scopeId: id, scopeName: '', includeChildren: true })
    })
    selectedGradeIds.value.forEach(id => {
      items.push({ itemTypeCode: 'GRADE', scopeId: id, scopeName: '', includeChildren: false })
    })
    selectedClassIds.value.forEach(id => {
      items.push({ itemTypeCode: 'CLASS', scopeId: id, scopeName: '', includeChildren: false })
    })

    mp.scopeItems = items
  }
  const total = selectedOrgUnitIds.value.length + selectedGradeIds.value.length + selectedClassIds.value.length
  ElMessage.success(`已选择 ${total} 个范围项`)
  expandedCustomModule.value = ''
}

// 处理年级选中
const handleGradeCheck = (gradeId: number, checked: boolean) => {
  if (checked) {
    if (!selectedGradeIds.value.includes(gradeId)) {
      selectedGradeIds.value.push(gradeId)
    }
  } else {
    const idx = selectedGradeIds.value.indexOf(gradeId)
    if (idx > -1) selectedGradeIds.value.splice(idx, 1)
  }
}

// 处理班级选中
const handleClassCheck = (classId: number, checked: boolean) => {
  if (checked) {
    if (!selectedClassIds.value.includes(classId)) {
      selectedClassIds.value.push(classId)
    }
  } else {
    const idx = selectedClassIds.value.indexOf(classId)
    if (idx > -1) selectedClassIds.value.splice(idx, 1)
  }
}

// 全选/清空年级
const handleSelectAllGrades = () => {
  selectedGradeIds.value = gradesData.value.map(g => g.id!).filter(id => id !== undefined)
}
const handleClearAllGrades = () => {
  selectedGradeIds.value = []
}

// 全选/清空班级
const handleSelectAllClasses = () => {
  selectedClassIds.value = filteredClasses.value.map(c => c.id!).filter(id => id !== undefined)
}
const handleClearAllClasses = () => {
  selectedClassIds.value = []
}

// 获取模块的自定义范围数量
const getCustomScopeCount = (moduleCode: string): number => {
  if (!rolePermissionConfig.value) return 0
  const mp = rolePermissionConfig.value.modulePermissions.find(p => p.moduleCode === moduleCode)
  return mp?.scopeItems?.length || 0
}

// 获取模块的范围项列表
const getModuleScopeItems = (moduleCode: string): ScopeItem[] => {
  if (!rolePermissionConfig.value) return []
  const mp = rolePermissionConfig.value.modulePermissions.find(p => p.moduleCode === moduleCode)
  return mp?.scopeItems || []
}

// 加载组织架构树
const loadOrgTree = async () => {
  if (orgTreeData.value.length > 0) return // 已加载则跳过
  try {
    orgTreeLoading.value = true
    orgTreeData.value = await getOrgUnitTree()
    // 默认展开第一层
    expandedOrgKeys.value = orgTreeData.value.map(n => n.id)
  } catch (error) {
    console.error('加载组织架构失败:', error)
    ElMessage.error('加载组织架构失败')
  } finally {
    orgTreeLoading.value = false
  }
}

// 加载年级列表
const loadGrades = async () => {
  if (gradesData.value.length > 0) return
  try {
    gradesLoading.value = true
    gradesData.value = await getAllGrades()
  } catch (error) {
    console.error('加载年级失败:', error)
  } finally {
    gradesLoading.value = false
  }
}

// 加载班级列表
const loadClasses = async () => {
  if (classesData.value.length > 0) return
  try {
    classesLoading.value = true
    classesData.value = await getAllClasses()
  } catch (error) {
    console.error('加载班级失败:', error)
  } finally {
    classesLoading.value = false
  }
}

// 筛选后的班级列表
const filteredClasses = computed(() => {
  if (!classFilterGradeId.value) return classesData.value
  return classesData.value.filter(c => c.gradeId === classFilterGradeId.value)
})

// 获取所有组织单元ID（用于全选）
const getAllOrgIds = (nodes: OrgUnitTreeNode[]): number[] => {
  const ids: number[] = []
  const traverse = (list: OrgUnitTreeNode[]) => {
    list.forEach(node => {
      ids.push(node.id)
      if (node.children?.length) traverse(node.children)
    })
  }
  traverse(nodes)
  return ids
}

// 获取所有可展开的节点ID
const getAllExpandableIds = (nodes: OrgUnitTreeNode[]): number[] => {
  const ids: number[] = []
  const traverse = (list: OrgUnitTreeNode[]) => {
    list.forEach(node => {
      if (node.children?.length) {
        ids.push(node.id)
        traverse(node.children)
      }
    })
  }
  traverse(nodes)
  return ids
}

// 打开自定义范围选择对话框
const openCustomScopeDialog = async (moduleCode: string) => {
  currentCustomScopeModule.value = moduleCode
  // 加载组织树
  await loadOrgTree()
  // 加载当前模块已选中的组织单元
  if (rolePermissionConfig.value) {
    const mp = rolePermissionConfig.value.modulePermissions.find(p => p.moduleCode === moduleCode)
    selectedOrgUnitIds.value = mp?.scopeItems
      ?.filter(i => i.itemTypeCode === 'ORG_UNIT')
      .map(i => i.scopeId) || []
  }
  customScopeDialogVisible.value = true
}

// 切换节点展开状态
const toggleOrgNode = (id: number) => {
  const idx = expandedOrgKeys.value.indexOf(id)
  if (idx > -1) {
    expandedOrgKeys.value.splice(idx, 1)
  } else {
    expandedOrgKeys.value.push(id)
  }
}

// 处理节点选中
const handleOrgNodeCheck = (id: number, checked: boolean) => {
  if (checked) {
    if (!selectedOrgUnitIds.value.includes(id)) {
      selectedOrgUnitIds.value.push(id)
    }
  } else {
    const idx = selectedOrgUnitIds.value.indexOf(id)
    if (idx > -1) {
      selectedOrgUnitIds.value.splice(idx, 1)
    }
  }
}

// 展开所有节点
const handleExpandAllOrg = () => {
  expandedOrgKeys.value = getAllExpandableIds(orgTreeData.value)
}

// 折叠所有节点
const handleCollapseAllOrg = () => {
  expandedOrgKeys.value = []
}

// 全选所有节点
const handleSelectAllOrg = () => {
  selectedOrgUnitIds.value = getAllOrgIds(orgTreeData.value)
}

// 清空选择
const handleClearAllOrg = () => {
  selectedOrgUnitIds.value = []
}

// 确认自定义范围选择
const confirmCustomScope = () => {
  if (!rolePermissionConfig.value) return
  // 保存选中的组织单元到当前模块配置
  const mp = rolePermissionConfig.value.modulePermissions.find(
    p => p.moduleCode === currentCustomScopeModule.value
  )
  if (mp) {
    mp.scopeItems = selectedOrgUnitIds.value.map(id => ({
      itemTypeCode: 'ORG_UNIT',
      scopeId: id,
      scopeName: '',
      includeChildren: true
    }))
  }
  customScopeDialogVisible.value = false
  ElMessage.success(`已选择 ${selectedOrgUnitIds.value.length} 个组织单元`)
}

// ==================== V3 统一树形结构相关 ====================

// 构建统一树形结构
const buildUnifiedTree = async () => {
  unifiedTreeLoading.value = true
  try {
    // 并行加载所有数据
    const [orgTree, grades, classes] = await Promise.all([
      getOrgUnitTree(),
      getAllGrades(),
      getAllClasses()
    ])

    // 将年级和班级按年级分组
    const gradeMap = new Map<number, { id: number; name: string; classes: { id: number; name: string }[] }>()
    for (const grade of grades) {
      if (grade.id) {
        gradeMap.set(grade.id, {
          id: grade.id,
          name: grade.gradeName || `${grade.enrollmentYear}级`,
          classes: []
        })
      }
    }

    // 将班级添加到对应年级
    for (const cls of classes) {
      if (cls.gradeId && gradeMap.has(cls.gradeId)) {
        gradeMap.get(cls.gradeId)!.classes.push({
          id: cls.id!,
          name: cls.className || ''
        })
      }
    }

    // 构建统一树
    const buildNode = (org: any): UnifiedTreeNode => {
      const node: UnifiedTreeNode = {
        id: org.id,
        name: org.unitName || org.name || org.orgName,
        children: org.children?.map(buildNode),
        grades: Array.from(gradeMap.values()) // 每个组织单元都显示所有年级
      }
      return node
    }

    unifiedTreeData.value = orgTree.map(buildNode)

    // 默认展开第一层
    for (const org of unifiedTreeData.value) {
      unifiedExpandedNodes.value.add(`org-${org.id}`)
    }
  } catch (error) {
    console.error('构建统一树失败:', error)
  } finally {
    unifiedTreeLoading.value = false
  }
}

// 切换节点展开/折叠
const toggleUnifiedNode = (type: 'org' | 'grade', id: number) => {
  const key = `${type}-${id}`
  if (unifiedExpandedNodes.value.has(key)) {
    unifiedExpandedNodes.value.delete(key)
  } else {
    unifiedExpandedNodes.value.add(key)
  }
}

// 检查节点是否展开
const isUnifiedNodeExpanded = (type: 'org' | 'grade', id: number): boolean => {
  return unifiedExpandedNodes.value.has(`${type}-${id}`)
}

// 检查节点是否选中
const isNodeSelected = (type: 'org' | 'grade' | 'class', id: number): boolean => {
  switch (type) {
    case 'org': return selectedOrgUnitIds.value.includes(id)
    case 'grade': return selectedGradeIds.value.includes(id)
    case 'class': return selectedClassIds.value.includes(id)
  }
}

// 切换节点选中状态
const toggleNodeSelection = (type: 'org' | 'grade' | 'class', id: number) => {
  let arr: number[]
  switch (type) {
    case 'org': arr = selectedOrgUnitIds.value; break
    case 'grade': arr = selectedGradeIds.value; break
    case 'class': arr = selectedClassIds.value; break
  }

  const idx = arr.indexOf(id)
  if (idx === -1) {
    arr.push(id)
  } else {
    arr.splice(idx, 1)
  }
}

// 全部展开
const handleExpandAllScope = () => {
  const expandAll = (nodes: UnifiedTreeNode[]) => {
    for (const node of nodes) {
      unifiedExpandedNodes.value.add(`org-${node.id}`)
      if (node.children) expandAll(node.children)
      if (node.grades) {
        for (const grade of node.grades) {
          unifiedExpandedNodes.value.add(`grade-${grade.id}`)
        }
      }
    }
  }
  expandAll(unifiedTreeData.value)
}

// 全部折叠
const handleCollapseAllScope = () => {
  unifiedExpandedNodes.value.clear()
}

// 清空选择
const handleClearScope = () => {
  selectedOrgUnitIds.value = []
  selectedGradeIds.value = []
  selectedClassIds.value = []
}

// 获取模块图标
const getModuleIcon = (moduleCode: string) => {
  const icons: Record<string, any> = {
    student: Users,
    class: Users,
    grade: GraduationCap,
    inspection: BarChart3,
    template: BookOpen,
    record: BarChart3,
    appeal: Settings,
    department: Building2,
    org_unit: Building2,
    rating: BarChart3,
    task: Settings
  }
  return icons[moduleCode] || Settings
}

// 加载数据权限元数据
const loadDataPermissionMeta = async () => {
  try {
    const [modules, scopes] = await Promise.all([
      dataPermissionApi.getModules(),
      dataPermissionApi.getScopes()
    ])

    // 将扁平的 DataModuleDTO[] 按 domainCode 分组，同时构建领域名称映射
    const grouped: Record<string, { code: string; name: string; domainCode: string }[]> = {}
    const domains: Record<string, string> = {}
    for (const m of modules) {
      if (!grouped[m.domainCode]) {
        grouped[m.domainCode] = []
      }
      grouped[m.domainCode].push({
        code: m.moduleCode,
        name: m.moduleName,
        domainCode: m.domainCode
      })
      if (m.domainName) {
        domains[m.domainCode] = m.domainName
      }
    }
    groupedModules.value = grouped
    domainNameMap.value = domains
    dataScopeOptions.value = scopes
  } catch (error) {
    console.error('加载数据权限元数据失败:', error)
  }
}

// 打开数据权限配置对话框
const handleDataPermissions = async (row: RoleResponse) => {
  try {
    currentRoleId.value = row.id
    currentRoleName.value = row.roleName

    // 确保元数据已加载
    if (Object.keys(groupedModules.value).length === 0) {
      await loadDataPermissionMeta()
    }

    // 加载角色的数据权限配置
    const config = await dataPermissionApi.getConfig(row.id)
    rolePermissionConfig.value = config
    dataPermissionDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载数据权限失败')
  }
}

// 保存数据权限配置
const handleDataPermissionSubmit = async () => {
  if (!currentRoleId.value || !rolePermissionConfig.value) return
  try {
    dataPermissionSubmitLoading.value = true

    await dataPermissionApi.saveConfig(currentRoleId.value, rolePermissionConfig.value)

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
  // 预加载数据权限元数据
  loadDataPermissionMeta()
})
</script>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }

/* 展开动画 */
.expand-enter-active, .expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}
.expand-enter-from, .expand-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}
.expand-enter-to, .expand-leave-from {
  opacity: 1;
  max-height: 500px;
}
</style>
