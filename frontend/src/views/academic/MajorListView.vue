<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 页面头部 -->
    <div class="bg-gradient-to-r from-blue-600 to-indigo-600 px-6 py-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-white">专业管理</h1>
          <p class="mt-1 text-blue-100">管理学校专业设置、培养方向及学制配置</p>
        </div>
        <button
          v-if="hasPermission('major:add')"
          @click="handleAddMajor"
          class="inline-flex items-center gap-2 rounded-lg bg-white px-4 py-2.5 text-sm font-medium text-blue-600 shadow-sm hover:bg-blue-50 transition-colors"
        >
          <Plus class="h-4 w-4" />
          新增专业
        </button>
      </div>

      <!-- 统计概览 -->
      <div class="mt-6 grid grid-cols-4 gap-4">
        <div class="rounded-xl bg-white/10 backdrop-blur-sm px-4 py-3">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-white/20">
              <Building2 class="h-5 w-5 text-white" />
            </div>
            <div>
              <div class="text-2xl font-bold text-white">{{ departmentStats.length }}</div>
              <div class="text-sm text-blue-100">系部数量</div>
            </div>
          </div>
        </div>
        <div class="rounded-xl bg-white/10 backdrop-blur-sm px-4 py-3">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-white/20">
              <GraduationCap class="h-5 w-5 text-white" />
            </div>
            <div>
              <div class="text-2xl font-bold text-white">{{ total }}</div>
              <div class="text-sm text-blue-100">专业总数</div>
            </div>
          </div>
        </div>
        <div class="rounded-xl bg-white/10 backdrop-blur-sm px-4 py-3">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-white/20">
              <Compass class="h-5 w-5 text-white" />
            </div>
            <div>
              <div class="text-2xl font-bold text-white">{{ stats.directionCount }}</div>
              <div class="text-sm text-blue-100">培养方向</div>
            </div>
          </div>
        </div>
        <div class="rounded-xl bg-white/10 backdrop-blur-sm px-4 py-3">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-white/20">
              <CheckCircle class="h-5 w-5 text-white" />
            </div>
            <div>
              <div class="text-2xl font-bold text-white">{{ stats.enabled }}</div>
              <div class="text-sm text-blue-100">已启用专业</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="p-6">
      <!-- 筛选栏 -->
      <div class="mb-6 flex flex-wrap items-center gap-4 rounded-xl bg-white p-4 shadow-sm">
        <div class="flex items-center gap-2">
          <Search class="h-4 w-4 text-gray-400" />
          <input
            v-model="searchForm.majorName"
            type="text"
            placeholder="搜索专业名称..."
            class="h-9 w-52 rounded-lg border-0 bg-gray-100 px-3 text-sm placeholder:text-gray-400 focus:bg-white focus:ring-2 focus:ring-blue-500"
            @keyup.enter="handleSearch"
          />
        </div>
        <select
          v-model="searchForm.orgUnitId"
          class="h-9 rounded-lg border-0 bg-gray-100 px-3 pr-8 text-sm focus:bg-white focus:ring-2 focus:ring-blue-500"
        >
          <option :value="null">全部系部</option>
          <option v-for="item in departmentList" :key="item.id" :value="item.id">
            {{ item.deptName }}
          </option>
        </select>
        <select
          v-model="searchForm.status"
          class="h-9 rounded-lg border-0 bg-gray-100 px-3 pr-8 text-sm focus:bg-white focus:ring-2 focus:ring-blue-500"
        >
          <option :value="null">全部状态</option>
          <option :value="1">启用</option>
          <option :value="0">禁用</option>
        </select>
        <div class="flex items-center gap-2">
          <button
            @click="handleSearch"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          >
            查询
          </button>
          <button
            @click="handleReset"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-gray-200 bg-white px-4 text-sm font-medium text-gray-600 hover:bg-gray-50"
          >
            重置
          </button>
        </div>
        <div class="ml-auto flex items-center gap-2">
          <span class="text-sm text-gray-500">视图:</span>
          <button
            @click="viewMode = 'department'"
            :class="['rounded-lg px-3 py-1.5 text-sm transition-colors', viewMode === 'department' ? 'bg-blue-100 text-blue-700' : 'text-gray-500 hover:bg-gray-100']"
          >
            按系部
          </button>
          <button
            @click="viewMode = 'list'"
            :class="['rounded-lg px-3 py-1.5 text-sm transition-colors', viewMode === 'list' ? 'bg-blue-100 text-blue-700' : 'text-gray-500 hover:bg-gray-100']"
          >
            列表
          </button>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="flex items-center justify-center py-20">
        <Loader2 class="h-8 w-8 animate-spin text-blue-600" />
        <span class="ml-3 text-gray-500">加载中...</span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="majorList.length === 0" class="rounded-xl bg-white py-20 text-center shadow-sm">
        <GraduationCap class="mx-auto h-16 w-16 text-gray-200" />
        <p class="mt-4 text-lg text-gray-400">暂无专业数据</p>
        <p class="mt-1 text-sm text-gray-400">点击右上角"新增专业"开始添加</p>
      </div>

      <!-- 按系部分组视图 -->
      <div v-else-if="viewMode === 'department'" class="space-y-8">
        <div v-for="dept in departmentStats" :key="dept.id" class="overflow-hidden rounded-xl bg-white shadow-sm">
          <!-- 系部头部 -->
          <div class="border-b border-gray-100 bg-gradient-to-r from-slate-50 to-gray-50 px-6 py-4">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-100">
                  <Building2 class="h-5 w-5 text-blue-600" />
                </div>
                <div>
                  <h2 class="text-lg font-semibold text-gray-900">{{ dept.name }}</h2>
                  <p class="text-sm text-gray-500">{{ dept.majorCount }} 个专业 · {{ dept.directionCount }} 个培养方向</p>
                </div>
              </div>
              <div class="flex items-center gap-2">
                <span class="rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-700">
                  {{ dept.code }}
                </span>
              </div>
            </div>
          </div>

          <!-- 专业卡片网格 -->
          <div class="p-6">
            <div class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
              <div
                v-for="major in getMajorsByDept(dept.id)"
                :key="major.id"
                class="group relative rounded-xl border border-gray-200 bg-white p-5 transition-all hover:border-blue-300 hover:shadow-md"
              >
                <!-- 状态角标 -->
                <div class="absolute right-3 top-3">
                  <span :class="['inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium', major.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500']">
                    <span :class="['h-1.5 w-1.5 rounded-full', major.status === 1 ? 'bg-green-500' : 'bg-gray-400']"></span>
                    {{ major.status === 1 ? '启用' : '禁用' }}
                  </span>
                </div>

                <!-- 专业信息 -->
                <div class="mb-4">
                  <div class="flex items-start gap-3">
                    <div class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 text-lg font-bold text-white">
                      {{ major.majorName.charAt(0) }}
                    </div>
                    <div class="min-w-0 flex-1">
                      <h3 class="truncate text-base font-semibold text-gray-900">{{ major.majorName }}</h3>
                      <p class="mt-0.5 font-mono text-xs text-gray-400">{{ major.majorCode }}</p>
                    </div>
                  </div>
                </div>

                <!-- 培养方向列表 -->
                <div class="space-y-2">
                  <div class="flex items-center justify-between text-xs text-gray-500">
                    <span>培养方向</span>
                    <button
                      v-if="hasPermission('major:direction:add')"
                      @click="handleAddDirection(major)"
                      class="inline-flex items-center gap-1 text-blue-600 hover:text-blue-700"
                    >
                      <Plus class="h-3 w-3" />
                      添加
                    </button>
                  </div>

                  <div v-if="getDirections(major.id).length > 0" class="space-y-2">
                    <div
                      v-for="direction in getDirections(major.id)"
                      :key="direction.id"
                      class="group/dir relative rounded-lg bg-gray-50 p-3 transition-colors hover:bg-blue-50"
                    >
                      <div class="flex items-center justify-between">
                        <div class="flex items-center gap-2">
                          <Compass class="h-4 w-4 text-purple-500" />
                          <span class="text-sm font-medium text-gray-700">{{ direction.directionName }}</span>
                        </div>
                        <div class="flex items-center gap-1 opacity-0 transition-opacity group-hover/dir:opacity-100">
                          <button
                            v-if="hasPermission('major:direction:edit')"
                            @click="handleEditDirection(direction)"
                            class="rounded p-1 text-gray-400 hover:bg-white hover:text-blue-600"
                          >
                            <Pencil class="h-3.5 w-3.5" />
                          </button>
                          <button
                            v-if="hasPermission('major:direction:delete')"
                            @click="handleDeleteDirection(direction)"
                            class="rounded p-1 text-gray-400 hover:bg-white hover:text-red-600"
                          >
                            <Trash2 class="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </div>
                      <div class="mt-2 flex flex-wrap gap-1.5">
                        <span class="inline-flex items-center gap-1 rounded bg-blue-100 px-2 py-0.5 text-xs text-blue-700">
                          <Award class="h-3 w-3" />
                          {{ getLevelDisplay(direction) }}
                        </span>
                        <span class="inline-flex items-center gap-1 rounded bg-amber-100 px-2 py-0.5 text-xs text-amber-700">
                          <Clock class="h-3 w-3" />
                          {{ getYearsDisplay(direction) }}
                        </span>
                        <span v-if="direction.isSegmented === 1" class="rounded bg-purple-100 px-2 py-0.5 text-xs text-purple-700">
                          分段培养
                        </span>
                      </div>
                    </div>
                  </div>
                  <div v-else class="rounded-lg border-2 border-dashed border-gray-200 py-4 text-center">
                    <p class="text-xs text-gray-400">暂无培养方向</p>
                  </div>
                </div>

                <!-- 操作按钮 -->
                <div class="mt-4 flex items-center justify-end gap-2 border-t border-gray-100 pt-3">
                  <button
                    v-if="hasPermission('major:edit')"
                    @click="handleEditMajor(major)"
                    class="inline-flex items-center gap-1 rounded-lg px-3 py-1.5 text-xs font-medium text-blue-600 hover:bg-blue-50"
                  >
                    <Pencil class="h-3.5 w-3.5" />
                    编辑
                  </button>
                  <button
                    v-if="hasPermission('major:delete')"
                    @click="handleDeleteMajor(major)"
                    class="inline-flex items-center gap-1 rounded-lg px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50"
                  >
                    <Trash2 class="h-3.5 w-3.5" />
                    删除
                  </button>
                </div>
              </div>
            </div>

            <!-- 该系部无专业 -->
            <div v-if="getMajorsByDept(dept.id).length === 0" class="py-8 text-center">
              <p class="text-sm text-gray-400">该系部暂无专业</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 列表视图 -->
      <div v-else class="overflow-hidden rounded-xl bg-white shadow-sm">
        <div class="divide-y divide-gray-100">
          <div v-for="major in majorList" :key="major.id" class="p-4 hover:bg-gray-50">
            <div class="flex items-start justify-between">
              <div class="flex items-start gap-4">
                <div class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 text-lg font-bold text-white">
                  {{ major.majorName.charAt(0) }}
                </div>
                <div>
                  <div class="flex items-center gap-2">
                    <h3 class="font-semibold text-gray-900">{{ major.majorName }}</h3>
                    <span class="rounded bg-gray-100 px-2 py-0.5 font-mono text-xs text-gray-500">{{ major.majorCode }}</span>
                    <span :class="['rounded px-2 py-0.5 text-xs font-medium', major.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500']">
                      {{ major.status === 1 ? '启用' : '禁用' }}
                    </span>
                  </div>
                  <div class="mt-1 flex items-center gap-4 text-sm text-gray-500">
                    <span class="inline-flex items-center gap-1">
                      <Building2 class="h-3.5 w-3.5" />
                      {{ major.orgUnitName || '未分配系部' }}
                    </span>
                    <span class="inline-flex items-center gap-1">
                      <Compass class="h-3.5 w-3.5" />
                      {{ getDirectionCount(major.id) }} 个培养方向
                    </span>
                  </div>
                  <!-- 方向标签 -->
                  <div v-if="getDirections(major.id).length > 0" class="mt-2 flex flex-wrap gap-2">
                    <div
                      v-for="direction in getDirections(major.id)"
                      :key="direction.id"
                      class="inline-flex items-center gap-1.5 rounded-lg bg-gray-100 px-2.5 py-1"
                    >
                      <span class="text-xs font-medium text-gray-700">{{ direction.directionName }}</span>
                      <span class="text-xs text-gray-400">|</span>
                      <span class="text-xs text-blue-600">{{ getLevelDisplay(direction) }}</span>
                      <span class="text-xs text-gray-400">|</span>
                      <span class="text-xs text-amber-600">{{ getYearsDisplay(direction) }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="flex items-center gap-2">
                <button
                  v-if="hasPermission('major:direction:add')"
                  @click="handleAddDirection(major)"
                  class="inline-flex items-center gap-1 rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50"
                >
                  <Plus class="h-3.5 w-3.5" />
                  添加方向
                </button>
                <button
                  v-if="hasPermission('major:edit')"
                  @click="handleEditMajor(major)"
                  class="rounded-lg p-2 text-gray-400 hover:bg-blue-50 hover:text-blue-600"
                >
                  <Pencil class="h-4 w-4" />
                </button>
                <button
                  v-if="hasPermission('major:delete')"
                  @click="handleDeleteMajor(major)"
                  class="rounded-lg p-2 text-gray-400 hover:bg-red-50 hover:text-red-600"
                >
                  <Trash2 class="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
          <div class="text-sm text-gray-500">
            共 <span class="font-medium text-gray-900">{{ total }}</span> 条记录
          </div>
          <div class="flex items-center gap-2">
            <select
              v-model="pagination.pageSize"
              @change="pagination.pageNum = 1; loadMajorList()"
              class="h-8 rounded border border-gray-300 px-2 text-sm focus:border-blue-500 focus:outline-none"
            >
              <option :value="10">10条/页</option>
              <option :value="20">20条/页</option>
              <option :value="50">50条/页</option>
            </select>
            <div class="flex items-center gap-1">
              <button
                @click="handlePageChange(pagination.pageNum - 1)"
                :disabled="pagination.pageNum === 1"
                class="flex h-8 w-8 items-center justify-center rounded border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <ChevronLeft class="h-4 w-4" />
              </button>
              <span class="px-3 text-sm text-gray-600">
                {{ pagination.pageNum }} / {{ totalPages }}
              </span>
              <button
                @click="handlePageChange(pagination.pageNum + 1)"
                :disabled="pagination.pageNum >= totalPages"
                class="flex h-8 w-8 items-center justify-center rounded border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <ChevronRight class="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 专业新增/编辑对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="majorDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="majorDialogVisible = false"></div>
          <div class="relative w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl">
            <div class="mb-6 flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-100">
                  <GraduationCap class="h-5 w-5 text-blue-600" />
                </div>
                <h3 class="text-lg font-semibold text-gray-900">{{ majorEditMode === 'add' ? '新增专业' : '编辑专业' }}</h3>
              </div>
              <button @click="majorDialogVisible = false" class="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
                <X class="h-5 w-5" />
              </button>
            </div>

            <form @submit.prevent="handleMajorSubmit" class="space-y-5">
              <div>
                <label class="mb-2 block text-sm font-medium text-gray-700">
                  专业名称 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="majorForm.majorName"
                  type="text"
                  required
                  placeholder="例如：计算机网络技术"
                  class="h-10 w-full rounded-xl border border-gray-300 px-4 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>

              <div>
                <label class="mb-2 block text-sm font-medium text-gray-700">
                  专业编码 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="majorForm.majorCode"
                  type="text"
                  required
                  placeholder="例如：CS001"
                  class="h-10 w-full rounded-xl border border-gray-300 px-4 font-mono text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>

              <div>
                <label class="mb-2 block text-sm font-medium text-gray-700">
                  所属系部 <span class="text-red-500">*</span>
                </label>
                <select
                  v-model="majorForm.orgUnitId"
                  required
                  class="h-10 w-full rounded-xl border border-gray-300 px-4 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                >
                  <option :value="null" disabled>请选择系部</option>
                  <option v-for="item in departmentList" :key="item.id" :value="item.id">
                    {{ item.deptName }}
                  </option>
                </select>
              </div>

              <div>
                <label class="mb-2 block text-sm font-medium text-gray-700">状态</label>
                <div class="flex items-center gap-4">
                  <label class="flex cursor-pointer items-center gap-2 rounded-lg border border-gray-200 px-4 py-2.5 transition-colors" :class="majorForm.status === 1 ? 'border-blue-500 bg-blue-50' : ''">
                    <input type="radio" v-model="majorForm.status" :value="1" class="h-4 w-4 text-blue-600" />
                    <span class="text-sm text-gray-700">启用</span>
                  </label>
                  <label class="flex cursor-pointer items-center gap-2 rounded-lg border border-gray-200 px-4 py-2.5 transition-colors" :class="majorForm.status === 0 ? 'border-gray-500 bg-gray-50' : ''">
                    <input type="radio" v-model="majorForm.status" :value="0" class="h-4 w-4 text-gray-600" />
                    <span class="text-sm text-gray-700">禁用</span>
                  </label>
                </div>
              </div>

              <div>
                <label class="mb-2 block text-sm font-medium text-gray-700">专业描述</label>
                <textarea
                  v-model="majorForm.description"
                  rows="3"
                  placeholder="请输入专业描述（选填）"
                  class="w-full rounded-xl border border-gray-300 px-4 py-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                ></textarea>
              </div>

              <div class="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  @click="majorDialogVisible = false"
                  class="h-10 rounded-xl border border-gray-300 px-5 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  取消
                </button>
                <button
                  type="submit"
                  :disabled="submitLoading"
                  class="inline-flex h-10 items-center gap-2 rounded-xl bg-blue-600 px-5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
                  {{ majorEditMode === 'add' ? '创建专业' : '保存修改' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 专业方向新增/编辑对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="directionDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="directionDialogVisible = false"></div>
          <div class="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl bg-white p-6 shadow-xl">
            <div class="mb-6 flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-purple-100">
                  <Compass class="h-5 w-5 text-purple-600" />
                </div>
                <div>
                  <h3 class="text-lg font-semibold text-gray-900">{{ directionEditMode === 'add' ? '新增培养方向' : '编辑培养方向' }}</h3>
                  <p class="text-sm text-gray-500">所属专业：{{ currentMajorName }}</p>
                </div>
              </div>
              <button @click="directionDialogVisible = false" class="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
                <X class="h-5 w-5" />
              </button>
            </div>

            <form @submit.prevent="handleDirectionSubmit" class="space-y-5">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-2 block text-sm font-medium text-gray-700">
                    方向名称 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="directionForm.directionName"
                    type="text"
                    required
                    placeholder="例如：3年制高级工"
                    class="h-10 w-full rounded-xl border border-gray-300 px-4 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label class="mb-2 block text-sm font-medium text-gray-700">
                    方向编码 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="directionForm.directionCode"
                    type="text"
                    required
                    placeholder="例如：CN_ADV_3Y"
                    class="h-10 w-full rounded-xl border border-gray-300 px-4 font-mono text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  />
                </div>
              </div>

              <!-- 分段注册开关 -->
              <div class="rounded-xl border border-gray-200 p-4">
                <div class="flex items-center justify-between">
                  <div>
                    <div class="flex items-center gap-2">
                      <label class="text-sm font-medium text-gray-700">分段培养模式</label>
                      <span class="rounded bg-purple-100 px-2 py-0.5 text-xs text-purple-600">如3+2模式</span>
                    </div>
                    <p class="mt-1 text-xs text-gray-500">学生分两阶段完成不同层次的培养</p>
                  </div>
                  <label class="relative inline-flex cursor-pointer items-center">
                    <input
                      type="checkbox"
                      v-model="directionForm.isSegmented"
                      :true-value="1"
                      :false-value="0"
                      class="peer sr-only"
                    />
                    <div class="peer h-6 w-11 rounded-full bg-gray-200 after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:border-gray-300 after:bg-white after:transition-all after:content-[''] peer-checked:bg-purple-600 peer-checked:after:translate-x-full peer-checked:after:border-white peer-focus:outline-none"></div>
                  </label>
                </div>
              </div>

              <!-- 非分段模式：单一层次和学制 -->
              <div v-if="!directionForm.isSegmented" class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-2 block text-sm font-medium text-gray-700">
                    培养层次 <span class="text-red-500">*</span>
                  </label>
                  <select
                    v-model="directionForm.level"
                    required
                    class="h-10 w-full rounded-xl border border-gray-300 px-4 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  >
                    <option value="">请选择层次</option>
                    <option value="中级工">中级工</option>
                    <option value="高级工">高级工</option>
                    <option value="预备技师">预备技师</option>
                    <option value="技师">技师</option>
                  </select>
                </div>
                <div>
                  <label class="mb-2 block text-sm font-medium text-gray-700">
                    学制（年） <span class="text-red-500">*</span>
                  </label>
                  <div class="flex items-center gap-2">
                    <input
                      v-model.number="directionForm.years"
                      type="number"
                      min="1"
                      max="10"
                      required
                      class="h-10 w-full rounded-xl border border-gray-300 px-4 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                    />
                    <span class="text-sm text-gray-500">年</span>
                  </div>
                </div>
              </div>

              <!-- 分段模式：两阶段 -->
              <div v-else class="space-y-4">
                <div class="rounded-xl bg-blue-50 p-4">
                  <h4 class="mb-3 flex items-center gap-2 text-sm font-medium text-blue-700">
                    <span class="flex h-5 w-5 items-center justify-center rounded-full bg-blue-600 text-xs text-white">1</span>
                    第一阶段
                  </h4>
                  <div class="grid grid-cols-2 gap-4">
                    <div>
                      <label class="mb-1.5 block text-sm text-gray-600">培养层次</label>
                      <select
                        v-model="directionForm.phase1Level"
                        required
                        class="h-10 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none"
                      >
                        <option value="">请选择</option>
                        <option value="中级工">中级工</option>
                        <option value="高级工">高级工</option>
                        <option value="预备技师">预备技师</option>
                        <option value="技师">技师</option>
                      </select>
                    </div>
                    <div>
                      <label class="mb-1.5 block text-sm text-gray-600">学制年数</label>
                      <div class="flex items-center gap-2">
                        <input
                          v-model.number="directionForm.phase1Years"
                          type="number"
                          min="1"
                          max="5"
                          required
                          class="h-10 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none"
                        />
                        <span class="text-sm text-gray-500">年</span>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="flex justify-center">
                  <div class="flex items-center gap-2 text-gray-400">
                    <ArrowDown class="h-4 w-4" />
                    <span class="text-xs">升入</span>
                    <ArrowDown class="h-4 w-4" />
                  </div>
                </div>

                <div class="rounded-xl bg-purple-50 p-4">
                  <h4 class="mb-3 flex items-center gap-2 text-sm font-medium text-purple-700">
                    <span class="flex h-5 w-5 items-center justify-center rounded-full bg-purple-600 text-xs text-white">2</span>
                    第二阶段
                  </h4>
                  <div class="grid grid-cols-2 gap-4">
                    <div>
                      <label class="mb-1.5 block text-sm text-gray-600">培养层次</label>
                      <select
                        v-model="directionForm.phase2Level"
                        required
                        class="h-10 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none"
                      >
                        <option value="">请选择</option>
                        <option value="中级工">中级工</option>
                        <option value="高级工">高级工</option>
                        <option value="预备技师">预备技师</option>
                        <option value="技师">技师</option>
                      </select>
                    </div>
                    <div>
                      <label class="mb-1.5 block text-sm text-gray-600">学制年数</label>
                      <div class="flex items-center gap-2">
                        <input
                          v-model.number="directionForm.phase2Years"
                          type="number"
                          min="1"
                          max="5"
                          required
                          class="h-10 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none"
                        />
                        <span class="text-sm text-gray-500">年</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 自动生成名称提示 -->
              <div class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-3">
                <div class="text-sm text-gray-600">
                  <span class="font-medium">预览名称：</span>
                  <span class="text-blue-600">{{ previewDirectionName }}</span>
                </div>
                <button
                  type="button"
                  @click="autoFillDirectionName"
                  class="inline-flex items-center gap-1 rounded-lg bg-blue-100 px-3 py-1.5 text-xs font-medium text-blue-600 hover:bg-blue-200"
                >
                  <Wand2 class="h-3.5 w-3.5" />
                  应用名称
                </button>
              </div>

              <div>
                <label class="mb-2 block text-sm font-medium text-gray-700">备注</label>
                <textarea
                  v-model="directionForm.remarks"
                  rows="2"
                  placeholder="请输入备注（选填）"
                  class="w-full rounded-xl border border-gray-300 px-4 py-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                ></textarea>
              </div>

              <div class="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  @click="directionDialogVisible = false"
                  class="h-10 rounded-xl border border-gray-300 px-5 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  取消
                </button>
                <button
                  type="submit"
                  :disabled="submitLoading"
                  class="inline-flex h-10 items-center gap-2 rounded-xl bg-purple-600 px-5 text-sm font-medium text-white hover:bg-purple-700 disabled:opacity-50"
                >
                  <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
                  {{ directionEditMode === 'add' ? '创建方向' : '保存修改' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  GraduationCap,
  Plus,
  Search,
  Pencil,
  Trash2,
  X,
  Loader2,
  ChevronLeft,
  ChevronRight,
  CheckCircle,
  Compass,
  Award,
  Clock,
  Wand2,
  Building2,
  ArrowDown
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import {
  getMajorList, addMajor, updateMajor, deleteMajor,
  getDirectionsByMajor, addMajorDirection, updateMajorDirection, deleteMajorDirection,
} from '@/api/academic'
import type { Major, MajorQueryParams, MajorFormData, MajorDirection } from '@/types/academic'
import { getDepartmentList } from '@/api/organization'

const authStore = useAuthStore()
const hasPermission = (permission: string) => authStore.hasPermission(permission)

const loading = ref(false)
const submitLoading = ref(false)
const viewMode = ref<'department' | 'list'>('department')

// 搜索表单
const searchForm = reactive<MajorQueryParams>({
  majorName: '',
  majorCode: '',
  orgUnitId: null as string | null,
  status: null
})

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 50
})

// 数据
const majorList = ref<Major[]>([])
const total = ref(0)
const departmentList = ref<any[]>([])

// 各专业下的培养方向缓存
const directionsMap = ref<Record<number, MajorDirection[]>>({})

// 计算总页数
const totalPages = computed(() => Math.ceil(total.value / pagination.pageSize) || 1)

// 按系部统计
const departmentStats = computed(() => {
  const deptMap = new Map<string, { id: string, name: string, code: string, majorCount: number, directionCount: number }>()

  for (const dept of departmentList.value) {
    deptMap.set(dept.id, {
      id: dept.id,
      name: dept.deptName,
      code: dept.deptCode || '',
      majorCount: 0,
      directionCount: 0
    })
  }

  for (const major of majorList.value) {
    if (major.orgUnitId) {
      const dept = deptMap.get(String(major.orgUnitId))
      if (dept) {
        dept.majorCount++
        dept.directionCount += directionsMap.value[major.id]?.length || 0
      }
    }
  }

  return Array.from(deptMap.values()).filter(d => d.majorCount > 0)
})

// 获取某系部下的专业
const getMajorsByDept = (deptId: string) => {
  return majorList.value.filter(m => String(m.orgUnitId) === deptId)
}

// 统计数据
const stats = computed(() => {
  const enabled = majorList.value.filter(m => m.status === 1).length
  const disabled = majorList.value.filter(m => m.status === 0).length
  let directionCount = 0
  Object.values(directionsMap.value).forEach(dirs => {
    directionCount += dirs.length
  })
  return { enabled, disabled, directionCount }
})

// 专业弹窗控制
const majorDialogVisible = ref(false)
const majorEditMode = ref<'add' | 'edit'>('add')
const currentMajorId = ref<number | null>(null)

const majorForm = reactive<MajorFormData>({
  majorName: '',
  majorCode: '',
  orgUnitId: null as string | null,
  description: '',
  status: 1
})

// 专业方向弹窗控制
const directionDialogVisible = ref(false)
const directionEditMode = ref<'add' | 'edit'>('add')
const currentDirectionId = ref<number | null>(null)
const currentMajorName = ref('')

const directionForm = reactive<MajorDirection>({
  majorId: 0,
  directionName: '',
  directionCode: '',
  level: '',
  years: 3,
  isSegmented: 0,
  phase1Level: '',
  phase1Years: 3,
  phase2Level: '',
  phase2Years: 2,
  remarks: ''
})

// 预览方向名称
const previewDirectionName = computed(() => {
  if (directionForm.isSegmented === 1) {
    if (!directionForm.phase1Years || !directionForm.phase2Years || !directionForm.phase2Level) {
      return '请选择阶段信息'
    }
    return `${directionForm.phase1Years}+${directionForm.phase2Years}${directionForm.phase2Level}`
  } else {
    if (!directionForm.years || !directionForm.level) {
      return '请选择层次和学制'
    }
    return `${directionForm.years}年制${directionForm.level}`
  }
})

// 获取层次显示文本
const getLevelDisplay = (direction: MajorDirection) => {
  if (direction.isSegmented === 1) {
    return `${direction.phase1Level} → ${direction.phase2Level}`
  }
  return direction.level
}

// 获取学制显示文本
const getYearsDisplay = (direction: MajorDirection) => {
  if (direction.isSegmented === 1) {
    return `${direction.phase1Years}+${direction.phase2Years}年`
  }
  return `${direction.years}年`
}

// 自动填充方向名称
const autoFillDirectionName = () => {
  directionForm.directionName = previewDirectionName.value
}

// 获取某专业下的方向数量
const getDirectionCount = (majorId: number) => {
  return directionsMap.value[majorId]?.length || 0
}

// 获取某专业下的方向列表
const getDirections = (majorId: number) => {
  return directionsMap.value[majorId] || []
}

// 加载专业列表
const loadMajorList = async () => {
  loading.value = true
  try {
    const response = await getMajorList({
      ...searchForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    majorList.value = response.records || []
    total.value = response.total || 0

    // 加载所有专业的培养方向
    for (const major of majorList.value) {
      await loadDirections(major.id)
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加载专业列表失败')
  } finally {
    loading.value = false
  }
}

// 加载某专业下的培养方向
const loadDirections = async (majorId: number) => {
  try {
    const directions = await getDirectionsByMajor(majorId)
    directionsMap.value[majorId] = directions || []
  } catch (error) {
    console.error('加载培养方向失败:', error)
    directionsMap.value[majorId] = []
  }
}

// 加载组织列表
const loadDepartmentList = async () => {
  try {
    const response = await getDepartmentList()
    const flattenTree = (nodes: any[]): any[] => {
      const result: any[] = []
      for (const node of nodes) {
        result.push({
          // 强制转换为字符串避免JavaScript Long精度丢失
          id: String(node.id),
          deptName: node.unitName,
          deptCode: node.unitCode
        })
        if (node.children && node.children.length > 0) {
          result.push(...flattenTree(node.children))
        }
      }
      return result
    }
    departmentList.value = flattenTree(response || [])
  } catch (error) {
    console.error('加载组织列表失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadMajorList()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    majorName: '',
    majorCode: '',
    orgUnitId: null,
    status: null
  })
  pagination.pageNum = 1
  loadMajorList()
}

// 分页
const handlePageChange = (page: number) => {
  if (page < 1 || page > totalPages.value) return
  pagination.pageNum = page
  loadMajorList()
}

// ===== 专业操作 =====

const handleAddMajor = () => {
  majorEditMode.value = 'add'
  currentMajorId.value = null
  Object.assign(majorForm, {
    majorName: '',
    majorCode: '',
    orgUnitId: null as string | null,
    description: '',
    status: 1
  })
  majorDialogVisible.value = true
}

const handleEditMajor = (row: Major) => {
  majorEditMode.value = 'edit'
  currentMajorId.value = row.id
  Object.assign(majorForm, {
    majorName: row.majorName,
    majorCode: row.majorCode,
    orgUnitId: row.orgUnitId ? String(row.orgUnitId) : null,
    description: row.description || '',
    status: row.status
  })
  majorDialogVisible.value = true
}

const handleDeleteMajor = async (row: Major) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除专业 "${row.majorName}" 吗？该专业下的所有培养方向也将被删除！`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteMajor(row.id)
    ElMessage.success('删除成功')
    delete directionsMap.value[row.id]
    loadMajorList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

const handleMajorSubmit = async () => {
  if (!majorForm.majorName) {
    ElMessage.error('请输入专业名称')
    return
  }
  if (!majorForm.majorCode) {
    ElMessage.error('请输入专业编码')
    return
  }
  if (!majorForm.orgUnitId) {
    ElMessage.error('请选择所属系部')
    return
  }

  submitLoading.value = true
  try {
    if (majorEditMode.value === 'add') {
      await addMajor(majorForm)
      ElMessage.success('新增成功')
    } else {
      await updateMajor(currentMajorId.value!, majorForm)
      ElMessage.success('更新成功')
    }
    majorDialogVisible.value = false
    loadMajorList()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// ===== 培养方向操作 =====

const handleAddDirection = (major: Major) => {
  directionEditMode.value = 'add'
  currentDirectionId.value = null
  currentMajorName.value = major.majorName
  Object.assign(directionForm, {
    majorId: major.id,
    directionName: '',
    directionCode: '',
    level: '',
    years: 3,
    isSegmented: 0,
    phase1Level: '',
    phase1Years: 3,
    phase2Level: '',
    phase2Years: 2,
    remarks: ''
  })
  directionDialogVisible.value = true
}

const handleEditDirection = (direction: MajorDirection) => {
  directionEditMode.value = 'edit'
  currentDirectionId.value = direction.id!
  currentMajorName.value = direction.majorName || ''
  Object.assign(directionForm, {
    majorId: direction.majorId,
    directionName: direction.directionName,
    directionCode: direction.directionCode,
    level: direction.level || '',
    years: direction.years || 3,
    isSegmented: direction.isSegmented || 0,
    phase1Level: direction.phase1Level || '',
    phase1Years: direction.phase1Years || 3,
    phase2Level: direction.phase2Level || '',
    phase2Years: direction.phase2Years || 2,
    remarks: direction.remarks || ''
  })
  directionDialogVisible.value = true
}

const handleDeleteDirection = async (direction: MajorDirection) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除培养方向 "${direction.directionName}" 吗？`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteMajorDirection(direction.id!)
    ElMessage.success('删除成功')
    await loadDirections(direction.majorId)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

const handleDirectionSubmit = async () => {
  if (!directionForm.directionName) {
    ElMessage.error('请输入方向名称')
    return
  }
  if (!directionForm.directionCode) {
    ElMessage.error('请输入方向编码')
    return
  }

  if (directionForm.isSegmented === 1) {
    if (!directionForm.phase1Level || !directionForm.phase2Level) {
      ElMessage.error('请选择两个阶段的层次')
      return
    }
    if (!directionForm.phase1Years || !directionForm.phase2Years) {
      ElMessage.error('请输入两个阶段的年数')
      return
    }
  } else {
    if (!directionForm.level) {
      ElMessage.error('请选择层次')
      return
    }
    if (!directionForm.years) {
      ElMessage.error('请输入学制年数')
      return
    }
  }

  submitLoading.value = true
  try {
    if (directionEditMode.value === 'add') {
      await addMajorDirection(directionForm)
      ElMessage.success('新增成功')
    } else {
      await updateMajorDirection(currentDirectionId.value!, directionForm)
      ElMessage.success('更新成功')
    }
    directionDialogVisible.value = false
    await loadDirections(directionForm.majorId)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadDepartmentList().then(() => {
    loadMajorList()
  })
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
