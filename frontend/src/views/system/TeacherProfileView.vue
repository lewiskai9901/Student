<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 统计条 -->
    <div class="mb-4 flex items-center gap-4 rounded-lg border border-gray-200 bg-white px-5 py-3">
      <div class="flex items-center gap-1.5">
        <span class="text-sm text-gray-500">在职</span>
        <span class="text-sm font-semibold text-gray-900">{{ statusCounts.active }}</span>
      </div>
      <div class="h-3 w-px bg-gray-200" />
      <div class="flex items-center gap-1.5">
        <span class="text-sm text-gray-500">离职</span>
        <span class="text-sm font-semibold text-gray-900">{{ statusCounts.resigned }}</span>
      </div>
      <div class="h-3 w-px bg-gray-200" />
      <div class="flex items-center gap-1.5">
        <span class="text-sm text-gray-500">退休</span>
        <span class="text-sm font-semibold text-gray-900">{{ statusCounts.retired }}</span>
      </div>
      <div class="h-3 w-px bg-gray-200" />
      <div class="flex items-center gap-1.5">
        <span class="text-sm text-gray-500">总数</span>
        <span class="text-sm font-semibold text-gray-900">{{ total }}</span>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="mb-4 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-end gap-4">
        <div class="w-44">
          <label class="mb-1 block text-sm text-gray-600">姓名/工号</label>
          <input
            v-model="queryParams.keyword"
            type="text"
            placeholder="搜索姓名或工号"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="w-40">
          <label class="mb-1 block text-sm text-gray-600">所属部门</label>
          <select
            v-model="queryParams.orgUnitId"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option :value="undefined">全部</option>
            <option v-for="org in flatOrgUnits" :key="org.id" :value="org.id">{{ org.label }}</option>
          </select>
        </div>
        <div class="w-36">
          <label class="mb-1 block text-sm text-gray-600">职称</label>
          <select
            v-model="queryParams.title"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option :value="undefined">全部</option>
            <option v-for="t in TEACHER_TITLES" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <div class="w-28">
          <label class="mb-1 block text-sm text-gray-600">状态</label>
          <select
            v-model="queryParams.status"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option :value="undefined">全部</option>
            <option :value="1">在职</option>
            <option :value="2">离职</option>
            <option :value="3">退休</option>
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
            新增教师
          </button>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-2">
          <span class="font-medium text-gray-900">教师列表</span>
          <span class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600">{{ total }} 条</span>
        </div>
      </div>

      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">工号</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">姓名</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">职称</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">职称等级</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">所属系部</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">最大周课时</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
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
        <tbody v-else-if="list.length === 0">
          <tr>
            <td colspan="8" class="py-16 text-center">
              <UserX class="mx-auto h-12 w-12 text-gray-300" />
              <div class="mt-2 text-sm text-gray-500">暂无数据</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr
            v-for="row in list"
            :key="row.id"
            class="border-b border-gray-100 hover:bg-gray-50"
          >
            <td class="px-4 py-3 font-medium text-gray-900">{{ row.employeeNo || '-' }}</td>
            <td class="px-4 py-3 text-gray-700">
              <button
                @click="handleDetail(row)"
                class="text-blue-600 hover:text-blue-800 hover:underline"
              >{{ row.realName || row.userName || '-' }}</button>
            </td>
            <td class="px-4 py-3 text-gray-600">{{ row.title || '-' }}</td>
            <td class="px-4 py-3 text-gray-600">{{ row.titleLevel || '-' }}</td>
            <td class="px-4 py-3 text-gray-600">{{ row.orgUnitName || '-' }}</td>
            <td class="px-4 py-3 text-center text-gray-600">{{ row.maxWeeklyHours }}</td>
            <td class="px-4 py-3 text-center">
              <span :class="statusClass(row.status)">{{ TEACHER_STATUS_MAP[row.status] || '未知' }}</span>
            </td>
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
                  @click="handleDetail(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-green-50 hover:text-green-600"
                  title="详情"
                >
                  <Eye class="h-4 w-4" />
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
            @change="loadList"
            class="h-8 rounded border border-gray-300 px-2 text-sm"
          >
            <option v-for="size in [10, 20, 50]" :key="size" :value="size">{{ size }}条/页</option>
          </select>
          <div class="flex gap-1">
            <button
              @click="queryParams.pageNum = 1; loadList()"
              :disabled="queryParams.pageNum <= 1"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronsLeft class="h-4 w-4" />
            </button>
            <button
              @click="queryParams.pageNum--; loadList()"
              :disabled="queryParams.pageNum <= 1"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronLeft class="h-4 w-4" />
            </button>
            <button
              @click="queryParams.pageNum++; loadList()"
              :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)"
              class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronRight class="h-4 w-4" />
            </button>
            <button
              @click="queryParams.pageNum = Math.ceil(total / queryParams.pageSize); loadList()"
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
              <h3 class="text-lg font-medium text-gray-900">{{ isEdit ? '编辑教师' : '新增教师' }}</h3>
              <button @click="dialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="max-h-[65vh] overflow-y-auto p-6">
              <div class="grid grid-cols-2 gap-4">
                <!-- 关联用户 -->
                <div class="col-span-2">
                  <label class="mb-1 block text-sm text-gray-700">关联用户 <span class="text-red-500">*</span></label>
                  <div class="relative">
                    <input
                      v-model="userSearchKeyword"
                      type="text"
                      placeholder="搜索用户（姓名/账号）"
                      :disabled="isEdit"
                      :class="[
                        'h-9 w-full rounded-lg border px-3 text-sm focus:outline-none',
                        isEdit ? 'border-gray-200 bg-gray-100 text-gray-500' : 'border-gray-300 focus:border-blue-500'
                      ]"
                      @input="handleUserSearch"
                      @focus="showUserDropdown = true"
                    />
                    <div
                      v-if="showUserDropdown && userSearchResults.length > 0 && !isEdit"
                      class="absolute z-10 mt-1 max-h-48 w-full overflow-auto rounded-lg border border-gray-200 bg-white shadow-lg"
                    >
                      <button
                        v-for="u in userSearchResults"
                        :key="u.id"
                        class="flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-blue-50"
                        @click="selectUser(u)"
                      >
                        <span class="font-medium text-gray-900">{{ u.realName || u.username }}</span>
                        <span class="text-xs text-gray-400">{{ u.username }}</span>
                      </button>
                    </div>
                  </div>
                  <p v-if="formData.userId" class="mt-1 text-xs text-gray-400">已选: {{ selectedUserLabel }}</p>
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
                  <label class="mb-1 block text-sm text-gray-700">职称</label>
                  <select
                    v-model="formData.title"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  >
                    <option value="">请选择</option>
                    <option v-for="t in TEACHER_TITLES" :key="t" :value="t">{{ t }}</option>
                  </select>
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">职称等级</label>
                  <select
                    v-model="formData.titleLevel"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  >
                    <option value="">请选择</option>
                    <option v-for="l in TITLE_LEVELS" :key="l" :value="l">{{ l }}</option>
                  </select>
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">所属系部</label>
                  <select
                    v-model="formData.orgUnitId"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  >
                    <option :value="undefined">请选择</option>
                    <option v-for="org in flatOrgUnits" :key="org.id" :value="org.id">{{ org.label }}</option>
                  </select>
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">教学组</label>
                  <input
                    v-model="formData.teachingGroup"
                    type="text"
                    placeholder="请输入教学组"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">最大周课时</label>
                  <input
                    v-model.number="formData.maxWeeklyHours"
                    type="number"
                    min="0"
                    max="40"
                    placeholder="0"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">入职日期</label>
                  <input
                    v-model="formData.hireDate"
                    type="date"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label class="mb-1 block text-sm text-gray-700">状态</label>
                  <select
                    v-model="formData.status"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  >
                    <option :value="1">在职</option>
                    <option :value="2">离职</option>
                    <option :value="3">退休</option>
                  </select>
                </div>
                <div class="col-span-2">
                  <label class="mb-1 block text-sm text-gray-700">资质证书</label>
                  <textarea
                    v-model="formData.qualification"
                    rows="2"
                    placeholder="请输入资质证书信息"
                    class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div class="col-span-2">
                  <label class="mb-1 block text-sm text-gray-700">专业特长</label>
                  <div class="flex flex-wrap gap-1.5">
                    <span
                      v-for="(tag, idx) in formData.specialties"
                      :key="idx"
                      class="inline-flex items-center gap-1 rounded bg-blue-50 px-2 py-0.5 text-xs text-blue-600"
                    >
                      {{ tag }}
                      <button @click="removeSpecialty(idx)" class="hover:text-red-500">&times;</button>
                    </span>
                    <input
                      v-model="newSpecialty"
                      type="text"
                      placeholder="输入后回车添加"
                      class="h-7 w-32 rounded border border-dashed border-gray-300 px-2 text-xs focus:border-blue-500 focus:outline-none"
                      @keyup.enter="addSpecialty"
                    />
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

    <!-- 详情抽屉 -->
    <Teleport to="body">
      <Transition name="drawer">
        <div v-if="drawerVisible" class="fixed inset-0 z-50 flex justify-end">
          <div class="fixed inset-0 bg-black/50" @click="drawerVisible = false"></div>
          <div class="relative w-full max-w-xl bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">教师详情</h3>
              <button @click="drawerVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="h-[calc(100vh-65px)] overflow-y-auto">
              <!-- Tab切换 -->
              <div class="flex border-b border-gray-200 px-6">
                <button
                  v-for="tab in ['基本信息', '可授课程']"
                  :key="tab"
                  @click="activeTab = tab"
                  :class="[
                    'border-b-2 px-4 py-3 text-sm font-medium transition-colors',
                    activeTab === tab
                      ? 'border-blue-600 text-blue-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                  ]"
                >{{ tab }}</button>
              </div>

              <!-- 基本信息 -->
              <div v-if="activeTab === '基本信息'" class="space-y-3 p-6">
                <div class="grid grid-cols-2 gap-3">
                  <div v-for="field in detailFields" :key="field.label">
                    <div class="text-xs text-gray-400">{{ field.label }}</div>
                    <div class="text-sm text-gray-900">{{ field.value || '-' }}</div>
                  </div>
                </div>
                <div v-if="detailData?.specialties?.length">
                  <div class="mb-1 text-xs text-gray-400">专业特长</div>
                  <div class="flex flex-wrap gap-1.5">
                    <span
                      v-for="(s, i) in detailData.specialties"
                      :key="i"
                      class="rounded bg-blue-50 px-2 py-0.5 text-xs text-blue-600"
                    >{{ s }}</span>
                  </div>
                </div>
                <div v-if="detailData?.qualification">
                  <div class="mb-1 text-xs text-gray-400">资质证书</div>
                  <div class="whitespace-pre-wrap text-sm text-gray-700">{{ detailData.qualification }}</div>
                </div>
              </div>

              <!-- 可授课程 -->
              <div v-if="activeTab === '可授课程'" class="p-6">
                <div class="mb-3 flex items-center justify-between">
                  <span class="text-sm font-medium text-gray-700">课程列表</span>
                  <button
                    @click="courseDialogVisible = true"
                    class="inline-flex h-8 items-center gap-1 rounded-lg bg-blue-600 px-3 text-xs font-medium text-white hover:bg-blue-700"
                  >
                    <Plus class="h-3.5 w-3.5" />
                    添加课程
                  </button>
                </div>
                <div v-if="courses.length === 0" class="py-8 text-center text-sm text-gray-400">暂无课程</div>
                <table v-else class="w-full text-sm">
                  <thead>
                    <tr class="border-b border-gray-200 bg-gray-50">
                      <th class="px-3 py-2 text-left font-medium text-gray-600">课程代码</th>
                      <th class="px-3 py-2 text-left font-medium text-gray-600">课程名称</th>
                      <th class="px-3 py-2 text-center font-medium text-gray-600">资质等级</th>
                      <th class="px-3 py-2 text-center font-medium text-gray-600">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="c in courses" :key="c.id" class="border-b border-gray-100">
                      <td class="px-3 py-2 text-gray-600">{{ c.courseCode || '-' }}</td>
                      <td class="px-3 py-2 text-gray-900">{{ c.courseName || '-' }}</td>
                      <td class="px-3 py-2 text-center text-gray-600">{{ c.qualificationLevel }}</td>
                      <td class="px-3 py-2 text-center">
                        <button
                          @click="handleRemoveCourse(c)"
                          class="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-500"
                        >
                          <Trash2 class="h-3.5 w-3.5" />
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 添加课程对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="courseDialogVisible" class="fixed inset-0 z-[60] flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="courseDialogVisible = false"></div>
          <div class="relative w-full max-w-md rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">添加可授课程</h3>
              <button @click="courseDialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="p-6 space-y-4">
              <div>
                <label class="mb-1 block text-sm text-gray-700">课程 <span class="text-red-500">*</span></label>
                <select
                  v-model="courseForm.courseId"
                  class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                >
                  <option :value="0">请选择课程</option>
                  <option v-for="c in allCourses" :key="c.id" :value="c.id">{{ c.name }} ({{ c.code }})</option>
                </select>
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-700">资质等级</label>
                <select
                  v-model="courseForm.qualificationLevel"
                  class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                >
                  <option :value="1">1 - 初级</option>
                  <option :value="2">2 - 中级</option>
                  <option :value="3">3 - 高级</option>
                </select>
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-700">备注</label>
                <input
                  v-model="courseForm.remark"
                  type="text"
                  placeholder="可选备注"
                  class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                />
              </div>
            </div>
            <div class="flex justify-end gap-3 border-t border-gray-200 px-6 py-4">
              <button
                @click="courseDialogVisible = false"
                class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                @click="handleAddCourse"
                :disabled="!courseForm.courseId"
                class="inline-flex h-9 items-center rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, RotateCcw, Plus, Pencil, Eye, Trash2, X,
  Loader2, UserX, ChevronsLeft, ChevronLeft, ChevronRight, ChevronsRight
} from 'lucide-vue-next'
import { teacherProfileApi } from '@/api/teacher'
import { getSimpleUserList } from '@/api/user'
import { getOrgUnitTree } from '@/api/organization'
import { courseApi } from '@/api/academic'
import type { TeacherProfile, TeacherCourseQualification } from '@/types/teacher'
import { TEACHER_TITLES, TITLE_LEVELS, TEACHER_STATUS_MAP } from '@/types/teacher'
import type { SimpleUser } from '@/types/user'
import type { OrgUnitTreeNode } from '@/types'

// State
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const courseDialogVisible = ref(false)
const isEdit = ref(false)
const activeTab = ref('基本信息')

const list = ref<TeacherProfile[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  keyword: undefined as string | undefined,
  orgUnitId: undefined as number | undefined,
  title: undefined as string | undefined,
  status: undefined as number | undefined
})

// Status counts
const statusCounts = computed(() => {
  const all = list.value
  return {
    active: all.filter(t => t.status === 1).length,
    resigned: all.filter(t => t.status === 2).length,
    retired: all.filter(t => t.status === 3).length
  }
})

// Org tree
const orgTree = ref<OrgUnitTreeNode[]>([])
interface FlatOrgItem { id: number; label: string }
const flatOrgUnits = computed<FlatOrgItem[]>(() => {
  const result: FlatOrgItem[] = []
  const flatten = (nodes: OrgUnitTreeNode[], depth: number) => {
    for (const node of nodes) {
      const prefix = '\u00A0\u00A0'.repeat(depth)
      result.push({ id: node.id, label: prefix + (node.unitName || node.label || '') })
      if (node.children) flatten(node.children, depth + 1)
    }
  }
  flatten(orgTree.value, 0)
  return result
})

// Form
const editId = ref<number>()
const formData = reactive<Partial<TeacherProfile>>({
  userId: undefined,
  employeeNo: '',
  title: '',
  titleLevel: '',
  orgUnitId: undefined,
  teachingGroup: '',
  maxWeeklyHours: 0,
  qualification: '',
  specialties: [],
  hireDate: '',
  status: 1
})
const newSpecialty = ref('')

// User search
const userSearchKeyword = ref('')
const userSearchResults = ref<SimpleUser[]>([])
const showUserDropdown = ref(false)
const selectedUserLabel = ref('')

let searchTimer: ReturnType<typeof setTimeout> | null = null
const handleUserSearch = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    if (!userSearchKeyword.value || userSearchKeyword.value.length < 1) {
      userSearchResults.value = []
      return
    }
    try {
      userSearchResults.value = await getSimpleUserList(userSearchKeyword.value)
    } catch {
      userSearchResults.value = []
    }
  }, 300)
}

const selectUser = (u: SimpleUser) => {
  formData.userId = Number(u.id)
  userSearchKeyword.value = u.realName || u.username
  selectedUserLabel.value = `${u.realName || ''} (${u.username})`
  showUserDropdown.value = false
}

// Detail
const detailData = ref<TeacherProfile | null>(null)
const courses = ref<TeacherCourseQualification[]>([])

const detailFields = computed(() => {
  const d = detailData.value
  if (!d) return []
  return [
    { label: '工号', value: d.employeeNo },
    { label: '姓名', value: d.realName || d.userName },
    { label: '职称', value: d.title },
    { label: '职称等级', value: d.titleLevel },
    { label: '所属系部', value: d.orgUnitName },
    { label: '教学组', value: d.teachingGroup },
    { label: '最大周课时', value: String(d.maxWeeklyHours) },
    { label: '入职日期', value: d.hireDate },
    { label: '状态', value: TEACHER_STATUS_MAP[d.status] || '未知' },
    { label: '联系电话', value: d.phone },
  ]
})

// Courses
const allCourses = ref<{ id: number; name: string; code: string }[]>([])
const courseForm = reactive({ courseId: 0, qualificationLevel: 1, remark: '' })

// Methods
const statusClass = (status: number) => {
  const map: Record<number, string> = {
    1: 'rounded bg-green-50 px-1.5 py-0.5 text-xs text-green-600',
    2: 'rounded bg-red-50 px-1.5 py-0.5 text-xs text-red-600',
    3: 'rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500',
  }
  return map[status] || 'rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500'
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await teacherProfileApi.list({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      orgUnitId: queryParams.orgUnitId,
      title: queryParams.title || undefined,
      status: queryParams.status
    })
    list.value = res.records || []
    total.value = res.total || 0
  } catch {
    ElMessage.error('加载教师列表失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  loadList()
}

const resetQuery = () => {
  queryParams.keyword = undefined
  queryParams.orgUnitId = undefined
  queryParams.title = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  loadList()
}

const resetForm = () => {
  formData.userId = undefined
  formData.employeeNo = ''
  formData.title = ''
  formData.titleLevel = ''
  formData.orgUnitId = undefined
  formData.teachingGroup = ''
  formData.maxWeeklyHours = 0
  formData.qualification = ''
  formData.specialties = []
  formData.hireDate = ''
  formData.status = 1
  userSearchKeyword.value = ''
  selectedUserLabel.value = ''
  newSpecialty.value = ''
}

const handleAdd = () => {
  isEdit.value = false
  editId.value = undefined
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: TeacherProfile) => {
  isEdit.value = true
  editId.value = row.id
  Object.assign(formData, {
    userId: row.userId,
    employeeNo: row.employeeNo || '',
    title: row.title || '',
    titleLevel: row.titleLevel || '',
    orgUnitId: row.orgUnitId,
    teachingGroup: row.teachingGroup || '',
    maxWeeklyHours: row.maxWeeklyHours,
    qualification: row.qualification || '',
    specialties: [...(row.specialties || [])],
    hireDate: row.hireDate || '',
    status: row.status
  })
  userSearchKeyword.value = row.realName || row.userName || ''
  selectedUserLabel.value = row.realName || row.userName || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.userId) {
    ElMessage.warning('请选择关联用户')
    return
  }
  submitLoading.value = true
  try {
    if (isEdit.value && editId.value) {
      await teacherProfileApi.update(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await teacherProfileApi.create(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadList()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: TeacherProfile) => {
  try {
    await ElMessageBox.confirm('确认删除该教师档案？', '确认', { type: 'warning' })
    await teacherProfileApi.delete(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch {
    // cancelled
  }
}

const handleDetail = async (row: TeacherProfile) => {
  try {
    detailData.value = await teacherProfileApi.getById(row.id)
    courses.value = await teacherProfileApi.getCourses(row.id)
    activeTab.value = '基本信息'
    drawerVisible.value = true
  } catch {
    ElMessage.error('加载详情失败')
  }
}

const addSpecialty = () => {
  const val = newSpecialty.value.trim()
  if (val && !formData.specialties?.includes(val)) {
    if (!formData.specialties) formData.specialties = []
    formData.specialties.push(val)
  }
  newSpecialty.value = ''
}

const removeSpecialty = (idx: number) => {
  formData.specialties?.splice(idx, 1)
}

const handleAddCourse = async () => {
  if (!courseForm.courseId || !detailData.value) return
  try {
    await teacherProfileApi.addCourse(detailData.value.id, {
      courseId: courseForm.courseId,
      qualificationLevel: courseForm.qualificationLevel,
      remark: courseForm.remark || undefined
    })
    ElMessage.success('添加成功')
    courseDialogVisible.value = false
    courses.value = await teacherProfileApi.getCourses(detailData.value.id)
    courseForm.courseId = 0
    courseForm.qualificationLevel = 1
    courseForm.remark = ''
  } catch {
    ElMessage.error('添加失败')
  }
}

const handleRemoveCourse = async (c: TeacherCourseQualification) => {
  if (!detailData.value) return
  try {
    await ElMessageBox.confirm(`确认移除课程「${c.courseName}」？`, '确认', { type: 'warning' })
    await teacherProfileApi.removeCourse(detailData.value.id, c.courseId)
    ElMessage.success('已移除')
    courses.value = await teacherProfileApi.getCourses(detailData.value.id)
  } catch {
    // cancelled
  }
}

const loadCourses = async () => {
  try {
    const res = await courseApi.listAll()
    allCourses.value = (res || []).map((c: any) => ({ id: c.id, name: c.name || c.courseName, code: c.code || c.courseCode }))
  } catch {
    allCourses.value = []
  }
}

// Close user dropdown on outside click
const handleClickOutside = () => {
  setTimeout(() => { showUserDropdown.value = false }, 200)
}

onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  await Promise.all([loadList(), loadCourses()])
  try {
    orgTree.value = await getOrgUnitTree()
  } catch {
    orgTree.value = []
  }
})
</script>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.drawer-enter-active, .drawer-leave-active { transition: all 0.3s ease; }
.drawer-enter-from, .drawer-leave-to { opacity: 0; transform: translateX(100%); }
</style>
