<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">专业管理</h1>
      <p class="mt-1 text-sm text-gray-500">管理专业信息及其培养方向（层次、学制）</p>
    </div>

    <!-- 统计卡片 - 设计系统 -->
    <div class="mb-6 grid grid-cols-4 gap-4">
      <StatCard title="专业总数" :value="total" :icon="GraduationCap" subtitle="所有专业" :trend="10.5" color="blue" />
      <StatCard title="已启用" :value="stats.enabled" :icon="CheckCircle" subtitle="活跃专业" :trend="3.2" color="emerald" />
      <StatCard title="已禁用" :value="stats.disabled" :icon="XCircle" subtitle="停用专业" :trend="-5.0" color="cyan" />
      <StatCard title="培养方向" :value="stats.directionCount" :icon="Compass" subtitle="专业方向" :trend="15.8" color="purple" />
    </div>

    <!-- 搜索区域 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-600">专业名称</label>
          <input
            v-model="searchForm.majorName"
            type="text"
            placeholder="请输入专业名称"
            class="h-9 w-44 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-600">专业编码</label>
          <input
            v-model="searchForm.majorCode"
            type="text"
            placeholder="请输入专业编码"
            class="h-9 w-36 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-600">所属部门</label>
          <select
            v-model="searchForm.departmentId"
            class="h-9 w-40 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option :value="null">全部部门</option>
            <option v-for="item in departmentList" :key="item.id" :value="item.id">
              {{ item.deptName }}
            </option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-600">状态</label>
          <select
            v-model="searchForm.status"
            class="h-9 w-24 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option :value="null">全部</option>
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </div>
        <div class="flex gap-2">
          <button
            @click="handleSearch"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          >
            <Search class="h-4 w-4" />
            查询
          </button>
          <button
            @click="handleReset"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            <RotateCcw class="h-4 w-4" />
            重置
          </button>
          <button
            v-if="hasPermission('major:add')"
            @click="handleAddMajor"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-emerald-600 px-4 text-sm font-medium text-white hover:bg-emerald-700"
          >
            <Plus class="h-4 w-4" />
            新增专业
          </button>
        </div>
      </div>
    </div>

    <!-- 数据列表 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <span class="text-sm text-gray-500">共 <span class="font-medium text-gray-900">{{ total }}</span> 个专业</span>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="flex items-center justify-center py-12">
        <Loader2 class="h-6 w-6 animate-spin text-blue-600" />
        <span class="ml-2 text-gray-500">加载中...</span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="majorList.length === 0" class="py-12 text-center">
        <GraduationCap class="mx-auto h-10 w-10 text-gray-300" />
        <p class="mt-2 text-gray-400">暂无专业数据</p>
      </div>

      <!-- 专业列表（带展开的方向子列表） -->
      <div v-else class="divide-y divide-gray-100">
        <div v-for="major in majorList" :key="major.id">
          <!-- 专业行 -->
          <div
            class="flex items-center justify-between px-4 py-4 hover:bg-gray-50 cursor-pointer"
            @click="toggleExpand(major.id)"
          >
            <div class="flex items-center gap-3">
              <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-100">
                <GraduationCap class="h-4 w-4 text-blue-600" />
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <span class="font-medium text-gray-900">{{ major.majorName }}</span>
                  <span class="rounded bg-gray-100 px-2 py-0.5 font-mono text-xs text-gray-600">
                    {{ major.majorCode }}
                  </span>
                  <span :class="['rounded px-2 py-0.5 text-xs font-medium', major.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600']">
                    {{ major.status === 1 ? '启用' : '禁用' }}
                  </span>
                </div>
                <div class="mt-1 flex items-center gap-3 text-sm text-gray-500">
                  <span v-if="major.departmentName">{{ major.departmentName }}</span>
                </div>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <span class="rounded bg-purple-100 px-2 py-0.5 text-xs font-medium text-purple-700">
                {{ getDirectionCount(major.id) }}个方向
              </span>
              <ChevronDown
                :class="['h-5 w-5 text-gray-400 transition-transform', expandedMajors.includes(major.id) ? 'rotate-180' : '']"
              />
              <button
                v-if="hasPermission('major:edit')"
                @click.stop="handleEditMajor(major)"
                class="inline-flex items-center gap-1 rounded px-2 py-1 text-xs text-blue-600 hover:bg-blue-50"
              >
                <Pencil class="h-3.5 w-3.5" />
                编辑
              </button>
              <button
                v-if="hasPermission('major:delete')"
                @click.stop="handleDeleteMajor(major)"
                class="inline-flex items-center gap-1 rounded px-2 py-1 text-xs text-red-600 hover:bg-red-50"
              >
                <Trash2 class="h-3.5 w-3.5" />
                删除
              </button>
            </div>
          </div>

          <!-- 专业方向子列表（展开时显示） -->
          <Transition name="expand">
            <div v-if="expandedMajors.includes(major.id)" class="border-t border-gray-100 bg-gray-50/50">
              <!-- 方向列表头部 -->
              <div class="flex items-center justify-between border-b border-gray-100 px-6 py-2">
                <span class="text-sm text-gray-500">培养方向列表</span>
                <button
                  v-if="hasPermission('major:direction:add')"
                  @click="handleAddDirection(major)"
                  class="inline-flex items-center gap-1 rounded px-2 py-1 text-xs text-emerald-600 hover:bg-emerald-50"
                >
                  <Plus class="h-3.5 w-3.5" />
                  添加方向
                </button>
              </div>

              <!-- 方向列表 -->
              <div v-if="getDirections(major.id).length > 0" class="divide-y divide-gray-100">
                <div
                  v-for="direction in getDirections(major.id)"
                  :key="direction.id"
                  class="flex items-center justify-between px-6 py-3 hover:bg-white"
                >
                  <div class="flex items-center gap-3">
                    <Compass class="h-4 w-4 text-purple-500" />
                    <div>
                      <div class="flex items-center gap-2">
                        <span class="font-medium text-gray-700">{{ direction.directionName }}</span>
                        <span class="rounded bg-gray-100 px-1.5 py-0.5 font-mono text-xs text-gray-500">
                          {{ direction.directionCode }}
                        </span>
                      </div>
                      <div class="mt-1 flex items-center gap-2 text-xs text-gray-500">
                        <span class="inline-flex items-center gap-1 rounded bg-blue-50 px-1.5 py-0.5 text-blue-600">
                          <Award class="h-3 w-3" />
                          {{ getLevelDisplay(direction) }}
                        </span>
                        <span class="inline-flex items-center gap-1 rounded bg-amber-50 px-1.5 py-0.5 text-amber-600">
                          <Clock class="h-3 w-3" />
                          {{ getYearsDisplay(direction) }}
                        </span>
                        <span v-if="direction.isSegmented === 1" class="rounded bg-purple-50 px-1.5 py-0.5 text-purple-600">
                          分段注册
                        </span>
                      </div>
                    </div>
                  </div>
                  <div class="flex items-center gap-1">
                    <button
                      v-if="hasPermission('major:direction:edit')"
                      @click="handleEditDirection(direction)"
                      class="inline-flex items-center gap-1 rounded px-2 py-1 text-xs text-blue-600 hover:bg-blue-50"
                    >
                      <Pencil class="h-3 w-3" />
                      编辑
                    </button>
                    <button
                      v-if="hasPermission('major:direction:delete')"
                      @click="handleDeleteDirection(direction)"
                      class="inline-flex items-center gap-1 rounded px-2 py-1 text-xs text-red-600 hover:bg-red-50"
                    >
                      <Trash2 class="h-3 w-3" />
                      删除
                    </button>
                  </div>
                </div>
              </div>
              <div v-else class="px-6 py-4 text-center text-sm text-gray-400">
                暂无培养方向，点击"添加方向"创建
              </div>
            </div>
          </Transition>
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
              @click="handlePageChange(1)"
              :disabled="pagination.pageNum === 1"
              class="flex h-8 w-8 items-center justify-center rounded border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <ChevronsLeft class="h-4 w-4" />
            </button>
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
            <button
              @click="handlePageChange(totalPages)"
              :disabled="pagination.pageNum >= totalPages"
              class="flex h-8 w-8 items-center justify-center rounded border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <ChevronsRight class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 专业新增/编辑对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="majorDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="majorDialogVisible = false"></div>
          <div class="relative w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
            <div class="mb-6 flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">{{ majorEditMode === 'add' ? '新增专业' : '编辑专业' }}</h3>
              <button @click="majorDialogVisible = false" class="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
                <X class="h-5 w-5" />
              </button>
            </div>

            <form @submit.prevent="handleMajorSubmit" class="space-y-4">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  专业名称 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="majorForm.majorName"
                  type="text"
                  required
                  placeholder="请输入专业名称"
                  class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                />
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  专业编码 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="majorForm.majorCode"
                  type="text"
                  required
                  placeholder="请输入专业编码"
                  class="h-9 w-full rounded-lg border border-gray-300 px-3 font-mono text-sm focus:border-blue-500 focus:outline-none"
                />
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  所属部门 <span class="text-red-500">*</span>
                </label>
                <select
                  v-model="majorForm.departmentId"
                  required
                  class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                >
                  <option :value="null" disabled>请选择部门</option>
                  <option v-for="item in departmentList" :key="item.id" :value="item.id">
                    {{ item.deptName }}
                  </option>
                </select>
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">状态</label>
                <div class="flex items-center gap-6">
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input type="radio" v-model="majorForm.status" :value="1" class="h-4 w-4 text-blue-600" />
                    <span class="text-sm text-gray-700">启用</span>
                  </label>
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input type="radio" v-model="majorForm.status" :value="0" class="h-4 w-4 text-blue-600" />
                    <span class="text-sm text-gray-700">禁用</span>
                  </label>
                </div>
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">专业描述</label>
                <textarea
                  v-model="majorForm.description"
                  rows="3"
                  placeholder="请输入专业描述"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                ></textarea>
              </div>

              <div class="flex justify-end gap-3 border-t border-gray-200 pt-4">
                <button
                  type="button"
                  @click="majorDialogVisible = false"
                  class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  取消
                </button>
                <button
                  type="submit"
                  :disabled="submitLoading"
                  class="inline-flex h-9 items-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
                  确定
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
          <div class="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-lg bg-white p-6 shadow-xl">
            <div class="mb-6 flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">{{ directionEditMode === 'add' ? '新增培养方向' : '编辑培养方向' }}</h3>
              <button @click="directionDialogVisible = false" class="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
                <X class="h-5 w-5" />
              </button>
            </div>

            <form @submit.prevent="handleDirectionSubmit" class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">
                    所属专业
                  </label>
                  <input
                    type="text"
                    :value="currentMajorName"
                    disabled
                    class="h-9 w-full rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm text-gray-500"
                  />
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">
                    方向名称 <span class="text-red-500">*</span>
                  </label>
                  <div class="flex gap-2">
                    <input
                      v-model="directionForm.directionName"
                      type="text"
                      required
                      placeholder="如: 高级工方向"
                      class="h-9 flex-1 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    />
                    <button
                      type="button"
                      @click="autoFillDirectionName"
                      class="h-9 whitespace-nowrap rounded-lg border border-blue-300 bg-blue-50 px-3 text-xs font-medium text-blue-600 hover:bg-blue-100"
                      title="根据层次和学制自动生成名称"
                    >
                      <Wand2 class="h-3.5 w-3.5 inline mr-1" />
                      自动填充
                    </button>
                  </div>
                </div>
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  方向编码 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="directionForm.directionCode"
                  type="text"
                  required
                  placeholder="如: CN_ADV_01"
                  class="h-9 w-full rounded-lg border border-gray-300 px-3 font-mono text-sm focus:border-blue-500 focus:outline-none"
                />
              </div>

              <!-- 分段注册开关 -->
              <div class="rounded-lg border border-gray-200 p-4">
                <div class="flex items-center justify-between">
                  <div>
                    <label class="text-sm font-medium text-gray-700">分段注册</label>
                    <p class="text-xs text-gray-500">如3+2模式，学生分两阶段完成不同层次的培养</p>
                  </div>
                  <label class="relative inline-flex cursor-pointer items-center">
                    <input
                      type="checkbox"
                      v-model="directionForm.isSegmented"
                      :true-value="1"
                      :false-value="0"
                      class="peer sr-only"
                    />
                    <div class="peer h-6 w-11 rounded-full bg-gray-200 after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:border-gray-300 after:bg-white after:transition-all after:content-[''] peer-checked:bg-blue-600 peer-checked:after:translate-x-full peer-checked:after:border-white peer-focus:outline-none"></div>
                  </label>
                </div>
              </div>

              <!-- 非分段模式：单一层次和学制 -->
              <div v-if="!directionForm.isSegmented" class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">
                    层次 <span class="text-red-500">*</span>
                  </label>
                  <select
                    v-model="directionForm.level"
                    required
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  >
                    <option value="">请选择层次</option>
                    <option value="中级工">中级工</option>
                    <option value="高级工">高级工</option>
                    <option value="预备技师">预备技师</option>
                    <option value="技师">技师</option>
                  </select>
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">
                    学制(年) <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model.number="directionForm.years"
                    type="number"
                    min="1"
                    max="10"
                    required
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
              </div>

              <!-- 分段模式：两阶段层次和学制 -->
              <div v-else class="space-y-4">
                <div class="rounded-lg border border-blue-100 bg-blue-50/50 p-4">
                  <h4 class="mb-3 text-sm font-medium text-blue-700">第一阶段</h4>
                  <div class="grid grid-cols-2 gap-4">
                    <div>
                      <label class="mb-1.5 block text-sm text-gray-600">层次</label>
                      <select
                        v-model="directionForm.phase1Level"
                        required
                        class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                      >
                        <option value="">请选择</option>
                        <option value="中级工">中级工</option>
                        <option value="高级工">高级工</option>
                        <option value="预备技师">预备技师</option>
                        <option value="技师">技师</option>
                      </select>
                    </div>
                    <div>
                      <label class="mb-1.5 block text-sm text-gray-600">年数</label>
                      <input
                        v-model.number="directionForm.phase1Years"
                        type="number"
                        min="1"
                        max="5"
                        required
                        class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                      />
                    </div>
                  </div>
                </div>

                <div class="rounded-lg border border-purple-100 bg-purple-50/50 p-4">
                  <h4 class="mb-3 text-sm font-medium text-purple-700">第二阶段</h4>
                  <div class="grid grid-cols-2 gap-4">
                    <div>
                      <label class="mb-1.5 block text-sm text-gray-600">层次</label>
                      <select
                        v-model="directionForm.phase2Level"
                        required
                        class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                      >
                        <option value="">请选择</option>
                        <option value="中级工">中级工</option>
                        <option value="高级工">高级工</option>
                        <option value="预备技师">预备技师</option>
                        <option value="技师">技师</option>
                      </select>
                    </div>
                    <div>
                      <label class="mb-1.5 block text-sm text-gray-600">年数</label>
                      <input
                        v-model.number="directionForm.phase2Years"
                        type="number"
                        min="1"
                        max="5"
                        required
                        class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">备注</label>
                <textarea
                  v-model="directionForm.remarks"
                  rows="2"
                  placeholder="请输入备注"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                ></textarea>
              </div>

              <div class="flex justify-end gap-3 border-t border-gray-200 pt-4">
                <button
                  type="button"
                  @click="directionDialogVisible = false"
                  class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  取消
                </button>
                <button
                  type="submit"
                  :disabled="submitLoading"
                  class="inline-flex h-9 items-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
                  确定
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
  RotateCcw,
  Pencil,
  Trash2,
  X,
  Loader2,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  ChevronDown,
  CheckCircle,
  XCircle,
  Compass,
  Award,
  Clock,
  Wand2
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { getMajorList, addMajor, updateMajor, deleteMajor } from '@/api/v2/major'
import {
  getDirectionsByMajor,
  addMajorDirection,
  updateMajorDirection,
  deleteMajorDirection,
  type MajorDirection
} from '@/api/v2/majorDirection'
import { getDepartmentList } from '@/api/v2/organization'
import type { Major, MajorQueryParams, MajorFormData } from '@/types/major'

const authStore = useAuthStore()
const hasPermission = (permission: string) => authStore.hasPermission(permission)

const loading = ref(false)
const submitLoading = ref(false)

// 搜索表单
const searchForm = reactive<MajorQueryParams>({
  majorName: '',
  majorCode: '',
  departmentId: null,
  status: null
})

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 20
})

// 数据
const majorList = ref<Major[]>([])
const total = ref(0)
const departmentList = ref<any[]>([])

// 展开的专业ID列表
const expandedMajors = ref<number[]>([])

// 各专业下的培养方向缓存
const directionsMap = ref<Record<number, MajorDirection[]>>({})

// 计算总页数
const totalPages = computed(() => Math.ceil(total.value / pagination.pageSize) || 1)

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
  departmentId: null,
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
  if (directionForm.isSegmented === 1) {
    // 分段模式: 阶段一学制+阶段二学制+阶段二层次
    if (!directionForm.phase1Years || !directionForm.phase2Years || !directionForm.phase2Level) {
      ElMessage.warning('请先选择两个阶段的层次和学制')
      return
    }
    directionForm.directionName = `${directionForm.phase1Years}+${directionForm.phase2Years}${directionForm.phase2Level}`
  } else {
    // 非分段模式: 学制+年制+层次
    if (!directionForm.years || !directionForm.level) {
      ElMessage.warning('请先选择层次和学制')
      return
    }
    directionForm.directionName = `${directionForm.years}年制${directionForm.level}`
  }
}

// 获取某专业下的方向数量
const getDirectionCount = (majorId: number) => {
  return directionsMap.value[majorId]?.length || 0
}

// 获取某专业下的方向列表
const getDirections = (majorId: number) => {
  return directionsMap.value[majorId] || []
}

// 切换展开
const toggleExpand = async (majorId: number) => {
  const index = expandedMajors.value.indexOf(majorId)
  if (index > -1) {
    expandedMajors.value.splice(index, 1)
  } else {
    expandedMajors.value.push(majorId)
    // 如果还没加载过方向数据，则加载
    if (!directionsMap.value[majorId]) {
      await loadDirections(majorId)
    }
  }
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

    // 加载展开专业的方向
    for (const majorId of expandedMajors.value) {
      await loadDirections(majorId)
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

// 加载部门列表
const loadDepartmentList = async () => {
  try {
    const response = await getDepartmentList()
    departmentList.value = response || []
  } catch (error) {
    console.error('加载部门列表失败:', error)
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
    departmentId: null,
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
    departmentId: null,
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
    departmentId: row.departmentId,
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
    // 清除缓存
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
  if (!majorForm.departmentId) {
    ElMessage.error('请选择所属部门')
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
    // 重新加载该专业的方向
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

  // 验证层次和学制
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
    // 重新加载该专业的方向
    await loadDirections(directionForm.majorId)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadMajorList()
  loadDepartmentList()
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

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 500px;
}
</style>
