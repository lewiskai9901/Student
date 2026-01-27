<template>
  <div class="fixed inset-0 z-50 flex">
    <!-- 遮罩层 -->
    <div class="absolute inset-0 bg-black/40" @click="handleClose"></div>

    <!-- 控制面板 -->
    <div class="relative ml-auto flex h-full w-[560px] flex-col bg-white shadow-2xl">
      <!-- 头部 -->
      <div class="flex items-center justify-between border-b border-gray-200 bg-gradient-to-r from-teal-600 to-cyan-600 px-5 py-4 text-white">
        <div class="min-w-0 flex-1">
          <div class="flex items-center gap-3">
            <span class="rounded bg-white/20 px-2 py-1 text-sm font-bold">
              {{ dormitory?.buildingNo || '-' }}
            </span>
            <h2 class="text-lg font-semibold">{{ dormitory?.dormitoryNo || dormitory?.roomNo }}</h2>
            <span class="rounded-full bg-white/20 px-2 py-0.5 text-xs">{{ dormitory?.floorNumber || dormitory?.floor }}F</span>
          </div>
          <div class="mt-1 flex items-center gap-3 text-sm opacity-90">
            <span>{{ dormitory?.buildingName }}</span>
            <span class="h-1 w-1 rounded-full bg-white/50"></span>
            <span>{{ getRoomUsageLabel(dormitory?.roomUsageType) }}</span>
            <span class="h-1 w-1 rounded-full bg-white/50"></span>
            <span>{{ getGenderLabel(dormitory?.genderType) }}</span>
          </div>
        </div>
        <button @click="handleClose" class="rounded-lg p-2 hover:bg-white/20">
          <X class="h-5 w-5" />
        </button>
      </div>

      <!-- Tab 导航 -->
      <div class="flex border-b border-gray-200 bg-gray-50">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          @click="activeTab = tab.key"
          class="flex items-center gap-1.5 border-b-2 px-4 py-2.5 text-sm font-medium transition-colors"
          :class="activeTab === tab.key
            ? 'border-teal-600 text-teal-600'
            : 'border-transparent text-gray-500 hover:text-gray-700'"
        >
          <component :is="tab.icon" class="h-4 w-4" />
          {{ tab.label }}
        </button>
      </div>

      <!-- 内容区 -->
      <div class="flex-1 overflow-y-auto">
        <!-- 加载状态 -->
        <div v-if="loading" class="flex h-40 items-center justify-center">
          <Loader2 class="h-8 w-8 animate-spin text-teal-500" />
        </div>

        <template v-else-if="dormitory">
          <!-- 床位管理 Tab -->
          <div v-show="activeTab === 'beds'" class="p-4">
            <!-- 入住状态概览 -->
            <div class="mb-4 rounded-lg border border-gray-200 bg-gradient-to-r from-gray-50 to-white p-4">
              <div class="flex items-center justify-between">
                <div>
                  <div class="text-sm text-gray-500">入住状态</div>
                  <div class="mt-1 flex items-baseline gap-2">
                    <span class="text-2xl font-bold text-gray-900">{{ dormitory.currentOccupancy || 0 }}</span>
                    <span class="text-gray-400">/</span>
                    <span class="text-lg text-gray-600">{{ maxBeds }}</span>
                    <span class="text-sm text-gray-400">人</span>
                  </div>
                </div>
                <div class="text-right">
                  <div class="text-sm text-gray-500">入住率</div>
                  <div class="mt-1 text-2xl font-bold" :class="occupancyTextColor">{{ occupancyRate }}%</div>
                </div>
              </div>
              <div class="mt-3 h-2 overflow-hidden rounded-full bg-gray-200">
                <div
                  class="h-full rounded-full transition-all"
                  :class="occupancyBarColor"
                  :style="{ width: `${occupancyRate}%` }"
                ></div>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="mb-4 flex flex-wrap gap-2">
              <button
                v-if="hasEmptyBed"
                @click="showAssignDialog = true"
                class="flex items-center gap-1.5 rounded-lg bg-teal-600 px-3 py-2 text-sm font-medium text-white hover:bg-teal-700"
              >
                <UserPlus class="h-4 w-4" />
                分配学生
              </button>
              <button
                @click="showSwapDialog = true"
                class="flex items-center gap-1.5 rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                <ArrowLeftRight class="h-4 w-4" />
                交换床位
              </button>
            </div>

            <!-- 床位网格 -->
            <div class="grid gap-3" :class="bedGridClass">
              <div
                v-for="bedNo in maxBeds"
                :key="bedNo"
                class="group relative rounded-xl border-2 p-3 transition-all"
                :class="getBedCardClass(bedNo)"
              >
                <!-- 床位号 -->
                <div
                  class="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold shadow-sm"
                  :class="getBedNumberClass(bedNo)"
                >
                  {{ bedNo }}
                </div>

                <!-- 已入住 -->
                <div v-if="isBedOccupied(bedNo)" class="space-y-2">
                  <div class="flex items-center gap-3">
                    <div class="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-teal-400 to-cyan-500 text-sm font-bold text-white">
                      {{ getStudentByBed(bedNo)?.realName?.charAt(0) }}
                    </div>
                    <div class="min-w-0 flex-1">
                      <p class="truncate font-semibold text-gray-900">{{ getStudentByBed(bedNo)?.realName }}</p>
                      <p class="truncate text-xs text-gray-500">{{ getStudentByBed(bedNo)?.studentNo }}</p>
                    </div>
                  </div>
                  <div class="flex items-center gap-2 text-xs text-gray-500">
                    <span v-if="getStudentByBed(bedNo)?.className" class="truncate">
                      <GraduationCap class="mr-0.5 inline h-3 w-3" />
                      {{ getStudentByBed(bedNo)?.className }}
                    </span>
                  </div>
                  <!-- 悬浮操作 -->
                  <div class="flex gap-1 opacity-0 transition-opacity group-hover:opacity-100">
                    <button
                      @click="showStudentDetail(getStudentByBed(bedNo))"
                      class="flex-1 rounded bg-blue-50 py-1 text-xs text-blue-600 hover:bg-blue-100"
                    >
                      详情
                    </button>
                    <button
                      @click="handleRemoveStudent(bedNo)"
                      class="flex-1 rounded bg-red-50 py-1 text-xs text-red-600 hover:bg-red-100"
                    >
                      移出
                    </button>
                  </div>
                </div>

                <!-- 空床位 -->
                <div v-else class="flex flex-col items-center py-3 text-gray-300">
                  <BedDouble class="h-8 w-8" />
                  <span class="mt-1 text-xs">空置</span>
                  <button
                    @click="handleAssignToBed(bedNo)"
                    class="mt-2 rounded bg-teal-50 px-3 py-1 text-xs text-teal-600 opacity-0 transition-opacity hover:bg-teal-100 group-hover:opacity-100"
                  >
                    分配
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 宿舍信息 Tab -->
          <div v-show="activeTab === 'info'" class="p-4 space-y-4">
            <!-- 基本信息 -->
            <div class="rounded-lg border border-gray-200">
              <div class="flex items-center justify-between border-b border-gray-100 bg-gray-50 px-4 py-2">
                <span class="text-sm font-medium text-gray-700">基本信息</span>
                <button @click="showEditDialog = true" class="flex items-center gap-1 text-xs text-teal-600 hover:text-teal-700">
                  <Pencil class="h-3.5 w-3.5" />
                  编辑
                </button>
              </div>
              <div class="grid grid-cols-2 gap-4 p-4">
                <div>
                  <div class="text-xs text-gray-400">楼栋</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ dormitory.buildingNo }} - {{ dormitory.buildingName }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">房间号</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ dormitory.dormitoryNo || dormitory.roomNo }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">楼层</div>
                  <div class="mt-0.5 font-medium text-gray-900">第 {{ dormitory.floorNumber || dormitory.floor }} 层</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">容量</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ maxBeds }} 人间</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">房间用途</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ getRoomUsageLabel(dormitory.roomUsageType) }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">性别类型</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ getGenderLabel(dormitory.genderType) }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">状态</div>
                  <div class="mt-0.5">
                    <span class="rounded px-2 py-0.5 text-xs font-medium" :class="statusClass">{{ statusText }}</span>
                  </div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">所属部门</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ dormitory.orgUnitName || '-' }}</div>
                </div>
              </div>
            </div>

            <!-- 管理信息 -->
            <div class="rounded-lg border border-gray-200">
              <div class="flex items-center justify-between border-b border-gray-100 bg-gray-50 px-4 py-2">
                <span class="text-sm font-medium text-gray-700">管理信息</span>
              </div>
              <div class="grid grid-cols-2 gap-4 p-4">
                <div>
                  <div class="text-xs text-gray-400">宿舍管理员</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ dormitory.supervisorName || '-' }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">指定班级</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ dormitory.assignedClassNames || '-' }}</div>
                </div>
                <div class="col-span-2">
                  <div class="text-xs text-gray-400">设施设备</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ dormitory.facilities || '-' }}</div>
                </div>
                <div class="col-span-2">
                  <div class="text-xs text-gray-400">备注</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ dormitory.notes || '-' }}</div>
                </div>
              </div>
            </div>

            <!-- 状态切换 -->
            <div class="rounded-lg border border-gray-200">
              <div class="border-b border-gray-100 bg-gray-50 px-4 py-2">
                <span class="text-sm font-medium text-gray-700">状态管理</span>
              </div>
              <div class="flex gap-2 p-4">
                <button
                  @click="handleStatusChange(1)"
                  class="flex flex-1 items-center justify-center gap-2 rounded-lg border-2 py-3 transition-all"
                  :class="dormitory.status === 1 ? 'border-emerald-500 bg-emerald-50 text-emerald-700' : 'border-gray-200 text-gray-500 hover:border-emerald-300'"
                >
                  <CheckCircle class="h-5 w-5" />
                  正常
                </button>
                <button
                  @click="handleStatusChange(2)"
                  class="flex flex-1 items-center justify-center gap-2 rounded-lg border-2 py-3 transition-all"
                  :class="dormitory.status === 2 ? 'border-amber-500 bg-amber-50 text-amber-700' : 'border-gray-200 text-gray-500 hover:border-amber-300'"
                >
                  <Wrench class="h-5 w-5" />
                  维修
                </button>
                <button
                  @click="handleStatusChange(0)"
                  class="flex flex-1 items-center justify-center gap-2 rounded-lg border-2 py-3 transition-all"
                  :class="dormitory.status === 0 ? 'border-red-500 bg-red-50 text-red-700' : 'border-gray-200 text-gray-500 hover:border-red-300'"
                >
                  <XCircle class="h-5 w-5" />
                  停用
                </button>
              </div>
            </div>
          </div>

          <!-- 关联资产 Tab -->
          <div v-show="activeTab === 'assets'" class="p-4">
            <div v-if="assetsLoading" class="flex items-center justify-center py-12">
              <Loader2 class="h-6 w-6 animate-spin text-teal-500" />
            </div>
            <div v-else-if="relatedAssets.length === 0" class="py-12 text-center text-gray-400">
              <Package class="mx-auto h-12 w-12" />
              <p class="mt-2">暂无关联资产</p>
              <button
                @click="goToAssetManagement"
                class="mt-3 text-sm text-emerald-600 hover:underline"
              >
                前往资产管理 →
              </button>
            </div>
            <div v-else class="space-y-3">
              <div class="flex items-center justify-between">
                <span class="text-sm font-medium text-gray-700">
                  共 {{ relatedAssets.length }} 项资产
                </span>
                <button
                  @click="goToAssetManagement"
                  class="text-xs text-emerald-600 hover:underline"
                >
                  管理资产 →
                </button>
              </div>
              <div
                v-for="asset in relatedAssets"
                :key="asset.id"
                class="rounded-lg border border-gray-200 p-3"
              >
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-3">
                    <div class="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-100">
                      <Package class="h-4 w-4 text-emerald-600" />
                    </div>
                    <div>
                      <div class="font-medium text-gray-900">{{ asset.assetName }}</div>
                      <div class="text-xs text-gray-500">{{ asset.assetCode }}</div>
                    </div>
                  </div>
                  <span
                    class="rounded-full px-2 py-0.5 text-xs font-medium"
                    :class="getAssetStatusClass(asset.status)"
                  >
                    {{ getAssetStatusText(asset.status) }}
                  </span>
                </div>
                <div class="mt-2 flex items-center gap-4 text-xs text-gray-500">
                  <span v-if="asset.categoryName">
                    分类: {{ asset.categoryName }}
                  </span>
                  <span v-if="asset.brand">
                    品牌: {{ asset.brand }}
                  </span>
                  <span v-if="asset.model">
                    型号: {{ asset.model }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 入住历史 Tab -->
          <div v-show="activeTab === 'history'" class="p-4">
            <div v-if="historyLoading" class="flex items-center justify-center py-12">
              <Loader2 class="h-6 w-6 animate-spin text-teal-500" />
            </div>
            <div v-else-if="historyList.length === 0" class="py-12 text-center text-gray-400">
              <History class="mx-auto h-12 w-12" />
              <p class="mt-2">暂无入住历史记录</p>
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="record in historyList"
                :key="record.id"
                class="rounded-lg border border-gray-200 p-3"
              >
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-3">
                    <div class="flex h-9 w-9 items-center justify-center rounded-full bg-teal-100 text-sm font-bold text-teal-600">
                      {{ record.studentName?.charAt(0) }}
                    </div>
                    <div>
                      <div class="font-medium text-gray-900">{{ record.studentName }}</div>
                      <div class="text-xs text-gray-500">{{ record.studentNo }} · {{ record.bedNumber }}号床</div>
                    </div>
                  </div>
                  <span
                    class="rounded-full px-2 py-0.5 text-xs font-medium"
                    :class="record.checkOutDate ? 'bg-gray-100 text-gray-600' : 'bg-emerald-100 text-emerald-700'"
                  >
                    {{ record.checkOutDate ? '已退宿' : '在住' }}
                  </span>
                </div>
                <div class="mt-2 flex items-center gap-4 text-xs text-gray-500">
                  <span><Calendar class="mr-1 inline h-3 w-3" />入住: {{ formatDate(record.checkInDate) }}</span>
                  <span v-if="record.checkOutDate"><Calendar class="mr-1 inline h-3 w-3" />退宿: {{ formatDate(record.checkOutDate) }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- 底部操作栏 -->
      <div class="flex items-center justify-between border-t border-gray-200 bg-gray-50 px-4 py-3">
        <button @click="handleRefresh" class="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700">
          <RefreshCw class="h-4 w-4" />
          刷新
        </button>
        <button @click="handleClose" class="rounded-lg bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-800">
          关闭
        </button>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showEditDialog" class="fixed inset-0 z-[60] flex items-center justify-center">
          <div class="absolute inset-0 bg-black/50" @click="showEditDialog = false"></div>
          <div class="relative w-full max-w-md rounded-xl bg-white p-6 shadow-2xl">
            <div class="mb-5 flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">编辑宿舍信息</h3>
              <button @click="showEditDialog = false" class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100">
                <X class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">房间用途</label>
                  <select v-model="editForm.roomUsageType" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500">
                    <option :value="1">学生宿舍</option>
                    <option :value="2">教职工宿舍</option>
                    <option :value="3">配电室</option>
                    <option :value="4">卫生间</option>
                    <option :value="5">杂物间</option>
                    <option :value="6">其他</option>
                  </select>
                </div>
                <div v-if="editForm.roomUsageType === 1 || editForm.roomUsageType === 2">
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">性别类型</label>
                  <select v-model="editForm.genderType" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500">
                    <option :value="1">男生</option>
                    <option :value="2">女生</option>
                    <option :value="3">混合</option>
                  </select>
                </div>
              </div>

              <div v-if="editForm.roomUsageType === 1 || editForm.roomUsageType === 2">
                <label class="mb-1.5 block text-sm font-medium text-gray-700">床位数</label>
                <select v-model="editForm.bedCapacity" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500">
                  <option :value="2">2人间</option>
                  <option :value="4">4人间</option>
                  <option :value="6">6人间</option>
                  <option :value="8">8人间</option>
                </select>
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">设施设备</label>
                <input
                  v-model="editForm.facilities"
                  type="text"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
                  placeholder="如：空调、热水器、独立卫生间"
                />
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">备注</label>
                <textarea
                  v-model="editForm.notes"
                  rows="2"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
                  placeholder="可选备注信息"
                ></textarea>
              </div>
            </div>

            <div class="mt-6 flex justify-end gap-3">
              <button @click="showEditDialog = false" class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50">
                取消
              </button>
              <button @click="confirmEdit" class="rounded-lg bg-teal-600 px-4 py-2 text-sm font-medium text-white hover:bg-teal-700">
                保存
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 分配学生对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showAssignDialog" class="fixed inset-0 z-[60] flex items-center justify-center">
          <div class="absolute inset-0 bg-black/50" @click="showAssignDialog = false"></div>
          <div class="relative w-full max-w-md rounded-xl bg-white p-6 shadow-2xl">
            <div class="mb-5 flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">分配学生</h3>
              <button @click="showAssignDialog = false" class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100">
                <X class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">选择床位</label>
                <div class="flex flex-wrap gap-2">
                  <button
                    v-for="bed in emptyBeds"
                    :key="bed"
                    @click="assignForm.bedNumber = bed"
                    class="rounded-lg border-2 px-4 py-2 text-sm font-medium transition-all"
                    :class="assignForm.bedNumber === bed
                      ? 'border-teal-500 bg-teal-50 text-teal-700'
                      : 'border-gray-200 text-gray-600 hover:border-teal-300'"
                  >
                    {{ bed }}号床
                  </button>
                </div>
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">搜索学生</label>
                <el-select
                  v-model="assignForm.studentId"
                  filterable
                  remote
                  :remote-method="searchStudents"
                  :loading="searchLoading"
                  placeholder="输入学号或姓名搜索"
                  class="w-full"
                  size="large"
                >
                  <el-option
                    v-for="student in studentOptions"
                    :key="student.id"
                    :label="`${student.realName} (${student.studentNo})`"
                    :value="student.id"
                  >
                    <div class="flex items-center justify-between">
                      <span>{{ student.realName }}</span>
                      <span class="text-xs text-gray-400">{{ student.studentNo }}</span>
                    </div>
                  </el-option>
                </el-select>
              </div>
            </div>

            <div class="mt-6 flex justify-end gap-3">
              <button @click="showAssignDialog = false" class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50">
                取消
              </button>
              <button
                @click="confirmAssign"
                :disabled="!assignForm.studentId || !assignForm.bedNumber"
                class="rounded-lg bg-teal-600 px-4 py-2 text-sm font-medium text-white hover:bg-teal-700 disabled:cursor-not-allowed disabled:opacity-50"
              >
                确认分配
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 交换床位对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showSwapDialog" class="fixed inset-0 z-[60] flex items-center justify-center">
          <div class="absolute inset-0 bg-black/50" @click="showSwapDialog = false"></div>
          <div class="relative w-full max-w-md rounded-xl bg-white p-6 shadow-2xl">
            <div class="mb-5 flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">交换床位</h3>
              <button @click="showSwapDialog = false" class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100">
                <X class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">本宿舍学生</label>
                <select
                  v-model="swapForm.studentAId"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
                >
                  <option :value="null">请选择</option>
                  <option v-for="s in dormitory?.students" :key="s.id" :value="s.id">
                    {{ s.realName }} ({{ s.bedNumber }}号床)
                  </option>
                </select>
              </div>

              <div class="flex items-center justify-center">
                <ArrowLeftRight class="h-6 w-6 text-gray-400" />
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">搜索对换学生</label>
                <el-select
                  v-model="swapForm.studentBId"
                  filterable
                  remote
                  :remote-method="searchSwapStudents"
                  :loading="swapSearchLoading"
                  placeholder="输入学号或姓名搜索已入住学生"
                  class="w-full"
                  size="large"
                >
                  <el-option
                    v-for="student in swapStudentOptions"
                    :key="student.id"
                    :label="`${student.realName} - ${student.dormitoryName || ''}${student.bedNumber || ''}号床`"
                    :value="student.id"
                  >
                    <div class="flex items-center justify-between">
                      <span>{{ student.realName }}</span>
                      <span class="text-xs text-gray-400">{{ student.dormitoryName }} {{ student.bedNumber }}号床</span>
                    </div>
                  </el-option>
                </el-select>
              </div>
            </div>

            <div class="mt-6 flex justify-end gap-3">
              <button @click="showSwapDialog = false" class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50">
                取消
              </button>
              <button
                @click="confirmSwap"
                :disabled="!swapForm.studentAId || !swapForm.studentBId"
                class="rounded-lg bg-teal-600 px-4 py-2 text-sm font-medium text-white hover:bg-teal-700 disabled:cursor-not-allowed disabled:opacity-50"
              >
                确认交换
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 学生详情对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showStudentDetailDialog" class="fixed inset-0 z-[60] flex items-center justify-center">
          <div class="absolute inset-0 bg-black/50" @click="showStudentDetailDialog = false"></div>
          <div class="relative w-full max-w-md rounded-xl bg-white p-6 shadow-2xl">
            <div class="mb-5 flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">学生详情</h3>
              <button @click="showStudentDetailDialog = false" class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100">
                <X class="h-5 w-5" />
              </button>
            </div>

            <div v-if="currentStudent" class="space-y-4">
              <div class="flex items-center gap-4">
                <div class="flex h-16 w-16 items-center justify-center rounded-full bg-gradient-to-br from-teal-400 to-cyan-500 text-2xl font-bold text-white">
                  {{ currentStudent.realName?.charAt(0) }}
                </div>
                <div>
                  <div class="text-xl font-semibold text-gray-900">{{ currentStudent.realName }}</div>
                  <div class="text-sm text-gray-500">{{ currentStudent.studentNo }}</div>
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4 rounded-lg bg-gray-50 p-4">
                <div>
                  <div class="text-xs text-gray-400">班级</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ currentStudent.className || '-' }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">床位</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ currentStudent.bedNumber || '-' }}号床</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">手机号</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ currentStudent.phone || '-' }}</div>
                </div>
                <div>
                  <div class="text-xs text-gray-400">性别</div>
                  <div class="mt-0.5 font-medium text-gray-900">{{ currentStudent.gender === 1 ? '男' : currentStudent.gender === 2 ? '女' : '-' }}</div>
                </div>
              </div>
            </div>

            <div class="mt-6 flex justify-end">
              <button @click="showStudentDetailDialog = false" class="rounded-lg bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-800">
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
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  X,
  Loader2,
  BedDouble,
  UserPlus,
  Pencil,
  CheckCircle,
  Wrench,
  XCircle,
  RefreshCw,
  ArrowLeftRight,
  History,
  Calendar,
  GraduationCap,
  Info,
  Home,
  Clock,
  Package
} from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { assetApi } from '@/api/asset'
import type { Asset } from '@/types/asset'
import { AssetStatus, AssetStatusMap } from '@/types/asset'
import {
  getDormitory,
  updateDormitory,
  assignStudentToDormitory,
  removeStudentFromDormitory,
  updateDormitoryStatus,
  swapStudentDormitory,
  getStudentDormitoryHistory
} from '@/api/dormitory'
import { searchStudents as searchStudentsApi, getStudents } from '@/api/student'
import type { Dormitory } from '@/types/dormitory'

interface Props {
  dormitoryId: number | null
  visible: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  close: []
  refresh: []
}>()

// Tabs
const tabs = [
  { key: 'beds', label: '床位管理', icon: BedDouble },
  { key: 'info', label: '宿舍信息', icon: Info },
  { key: 'assets', label: '关联资产', icon: Package },
  { key: 'history', label: '入住历史', icon: History }
]
const activeTab = ref('beds')
const router = useRouter()

// State
const loading = ref(false)
const dormitory = ref<Dormitory | null>(null)

// 关联资产
const assetsLoading = ref(false)
const relatedAssets = ref<Asset[]>([])
const showEditDialog = ref(false)
const showAssignDialog = ref(false)
const showSwapDialog = ref(false)
const showStudentDetailDialog = ref(false)
const searchLoading = ref(false)
const swapSearchLoading = ref(false)
const studentOptions = ref<any[]>([])
const swapStudentOptions = ref<any[]>([])
const currentStudent = ref<any>(null)
const historyLoading = ref(false)
const historyList = ref<any[]>([])

const editForm = ref({
  roomUsageType: 1,
  genderType: 1,
  bedCapacity: 4,
  facilities: '',
  notes: ''
})

const assignForm = ref({
  bedNumber: null as number | null,
  studentId: null as number | null
})

const swapForm = ref({
  studentAId: null as number | null,
  studentBId: null as number | null
})

// 房间用途类型
const roomUsageTypes = [
  { value: 1, label: '学生宿舍' },
  { value: 2, label: '教职工宿舍' },
  { value: 3, label: '配电室' },
  { value: 4, label: '卫生间' },
  { value: 5, label: '杂物间' },
  { value: 6, label: '其他' }
]

// Computed
const maxBeds = computed(() => dormitory.value?.maxOccupancy || dormitory.value?.bedCapacity || 0)

const occupancyRate = computed(() => {
  if (!maxBeds.value) return 0
  return Math.round(((dormitory.value?.currentOccupancy || 0) / maxBeds.value) * 100)
})

const occupancyBarColor = computed(() => {
  if (occupancyRate.value === 0) return 'bg-gray-300'
  if (occupancyRate.value < 50) return 'bg-amber-400'
  if (occupancyRate.value < 100) return 'bg-teal-500'
  return 'bg-emerald-500'
})

const occupancyTextColor = computed(() => {
  if (occupancyRate.value >= 100) return 'text-emerald-600'
  if (occupancyRate.value >= 50) return 'text-teal-600'
  return 'text-amber-600'
})

const statusClass = computed(() => {
  const status = dormitory.value?.status
  if (status === 0) return 'bg-red-100 text-red-700'
  if (status === 2) return 'bg-amber-100 text-amber-700'
  return 'bg-emerald-100 text-emerald-700'
})

const statusText = computed(() => {
  const status = dormitory.value?.status
  if (status === 0) return '停用'
  if (status === 2) return '维修'
  return '正常'
})

const hasEmptyBed = computed(() => {
  return (dormitory.value?.currentOccupancy || 0) < maxBeds.value
})

const emptyBeds = computed(() => {
  if (!dormitory.value) return []
  const occupied = new Set(dormitory.value.students?.map(s => parseInt(String(s.bedNumber))) || [])
  return Array.from({ length: maxBeds.value }, (_, i) => i + 1).filter(bed => !occupied.has(bed))
})

const bedGridClass = computed(() => {
  if (maxBeds.value <= 4) return 'grid-cols-2'
  if (maxBeds.value <= 6) return 'grid-cols-3'
  return 'grid-cols-4'
})

// Methods
const loadDormitory = async () => {
  if (!props.dormitoryId) return
  loading.value = true
  try {
    dormitory.value = await getDormitory(props.dormitoryId)
    editForm.value = {
      roomUsageType: dormitory.value.roomUsageType || 1,
      genderType: dormitory.value.genderType || 1,
      bedCapacity: dormitory.value.maxOccupancy || dormitory.value.bedCapacity || 4,
      facilities: dormitory.value.facilities || '',
      notes: dormitory.value.notes || ''
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadHistory = async () => {
  if (!props.dormitoryId) return
  historyLoading.value = true
  try {
    // 暂时模拟数据，实际需要后端提供API
    historyList.value = dormitory.value?.students?.map(s => ({
      id: s.id,
      studentName: s.realName,
      studentNo: s.studentNo,
      bedNumber: s.bedNumber,
      checkInDate: new Date().toISOString(),
      checkOutDate: null
    })) || []
  } catch (error) {
    historyList.value = []
  } finally {
    historyLoading.value = false
  }
}

// 加载关联资产
const loadRelatedAssets = async () => {
  if (!props.dormitoryId) return
  assetsLoading.value = true
  try {
    const res = await assetApi.getAssetsByLocation('dormitory', props.dormitoryId)
    relatedAssets.value = res.data || []
  } catch (error) {
    console.error('Failed to load related assets:', error)
    relatedAssets.value = []
  } finally {
    assetsLoading.value = false
  }
}

// 资产状态样式
const getAssetStatusClass = (status: number) => {
  switch (status) {
    case AssetStatus.IN_USE:
      return 'bg-green-100 text-green-700'
    case AssetStatus.IDLE:
      return 'bg-gray-100 text-gray-700'
    case AssetStatus.REPAIRING:
      return 'bg-amber-100 text-amber-700'
    case AssetStatus.SCRAPPED:
      return 'bg-red-100 text-red-700'
    default:
      return 'bg-gray-100 text-gray-700'
  }
}

// 资产状态文本
const getAssetStatusText = (status: number) => {
  return AssetStatusMap[status] || '未知'
}

// 跳转到资产管理
const goToAssetManagement = () => {
  router.push({
    path: '/asset/center',
    query: {
      locationType: 'dormitory',
      locationId: String(props.dormitoryId)
    }
  })
}

const handleClose = () => {
  emit('update:visible', false)
  emit('close')
}

const handleRefresh = () => {
  loadDormitory()
  if (activeTab.value === 'history') {
    loadHistory()
  }
  emit('refresh')
}

const handleStatusChange = async (status: number) => {
  if (!dormitory.value || dormitory.value.status === status) return
  try {
    await updateDormitoryStatus(dormitory.value.id as number, status)
    ElMessage.success('状态已更新')
    loadDormitory()
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
  }
}

const getRoomUsageLabel = (type?: number) => {
  const t = roomUsageTypes.find(r => r.value === type)
  return t?.label || '学生宿舍'
}

const getGenderLabel = (type?: number) => {
  if (type === 1) return '男生宿舍'
  if (type === 2) return '女生宿舍'
  return '混合宿舍'
}

const isBedOccupied = (bedNo: number) => {
  return dormitory.value?.students?.some(s => parseInt(String(s.bedNumber)) === bedNo)
}

const getStudentByBed = (bedNo: number) => {
  return dormitory.value?.students?.find(s => parseInt(String(s.bedNumber)) === bedNo)
}

const getBedCardClass = (bedNo: number) => {
  if (isBedOccupied(bedNo)) return 'border-emerald-300 bg-emerald-50/50'
  return 'border-dashed border-gray-300 bg-gray-50/30'
}

const getBedNumberClass = (bedNo: number) => {
  if (isBedOccupied(bedNo)) return 'bg-emerald-500 text-white'
  return 'bg-gray-300 text-white'
}

const handleAssignToBed = (bedNo: number) => {
  assignForm.value.bedNumber = bedNo
  showAssignDialog.value = true
}

const handleRemoveStudent = async (bedNo: number) => {
  const student = getStudentByBed(bedNo)
  if (!student) return
  try {
    await ElMessageBox.confirm(`确定将 ${student.realName} 从宿舍移出吗？`, '确认移出', { type: 'warning' })
    await removeStudentFromDormitory(student.id as number)
    ElMessage.success('已移出')
    loadDormitory()
    emit('refresh')
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '操作失败')
  }
}

const showStudentDetail = (student: any) => {
  currentStudent.value = student
  showStudentDetailDialog.value = true
}

const searchStudents = async (query: string) => {
  if (!query || query.length < 2) {
    studentOptions.value = []
    return
  }
  searchLoading.value = true
  try {
    const results = await searchStudentsApi({ keyword: query, limit: 20 })
    studentOptions.value = results.filter(s => !s.dormitoryId)
  } catch {
    studentOptions.value = []
  } finally {
    searchLoading.value = false
  }
}

const searchSwapStudents = async (query: string) => {
  if (!query || query.length < 2) {
    swapStudentOptions.value = []
    return
  }
  swapSearchLoading.value = true
  try {
    const results = await searchStudentsApi({ keyword: query, limit: 20 })
    // 过滤：只显示已入住的学生，且排除本宿舍的学生
    swapStudentOptions.value = results.filter(s =>
      s.dormitoryId &&
      s.dormitoryId !== dormitory.value?.id
    )
  } catch {
    swapStudentOptions.value = []
  } finally {
    swapSearchLoading.value = false
  }
}

const confirmAssign = async () => {
  if (!assignForm.value.studentId || !assignForm.value.bedNumber || !dormitory.value) return
  try {
    await assignStudentToDormitory({
      dormitoryId: dormitory.value.id as number,
      studentId: assignForm.value.studentId,
      bedNumber: assignForm.value.bedNumber
    })
    ElMessage.success('分配成功')
    showAssignDialog.value = false
    assignForm.value = { bedNumber: null, studentId: null }
    loadDormitory()
    emit('refresh')
  } catch (error: any) {
    ElMessage.error(error.message || '分配失败')
  }
}

const confirmSwap = async () => {
  if (!swapForm.value.studentAId || !swapForm.value.studentBId) return
  try {
    await swapStudentDormitory({
      studentAId: swapForm.value.studentAId,
      studentBId: swapForm.value.studentBId
    })
    ElMessage.success('交换成功')
    showSwapDialog.value = false
    swapForm.value = { studentAId: null, studentBId: null }
    loadDormitory()
    emit('refresh')
  } catch (error: any) {
    ElMessage.error(error.message || '交换失败')
  }
}

const confirmEdit = async () => {
  if (!dormitory.value) return
  try {
    await updateDormitory(dormitory.value.id as number, {
      roomUsageType: editForm.value.roomUsageType,
      genderType: editForm.value.genderType,
      bedCapacity: editForm.value.bedCapacity,
      facilities: editForm.value.facilities,
      notes: editForm.value.notes
    })
    ElMessage.success('保存成功')
    showEditDialog.value = false
    loadDormitory()
    emit('refresh')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

const formatDate = (date: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

// Watch
watch(() => props.visible, (val) => {
  if (val && props.dormitoryId) {
    loadDormitory()
    activeTab.value = 'beds'
  }
})

watch(() => props.dormitoryId, (val) => {
  if (val && props.visible) {
    loadDormitory()
    activeTab.value = 'beds'
  }
})

watch(activeTab, (val) => {
  if (val === 'history' && props.dormitoryId) {
    loadHistory()
  }
  if (val === 'assets' && props.dormitoryId) {
    loadRelatedAssets()
  }
})

onMounted(() => {
  if (props.visible && props.dormitoryId) loadDormitory()
})
</script>

<style scoped>
.modal-enter-active, .modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from, .modal-leave-to {
  opacity: 0;
}
</style>
