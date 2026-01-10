<template>
  <div class="min-h-screen bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-6 rounded-xl bg-gradient-to-r from-slate-600 to-gray-500 p-6 shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="flex items-center gap-3 text-2xl font-bold text-white">
            <Building2 class="h-8 w-8" />
            楼宇管理
          </h1>
          <p class="mt-1 text-slate-200">管理校园建筑信息</p>
        </div>
        <button
          @click="handleAdd"
          class="flex items-center gap-2 rounded-lg bg-white/20 px-4 py-2.5 text-sm font-medium text-white backdrop-blur-sm transition-all hover:bg-white/30"
        >
          <Plus class="h-4 w-4" />
          新增楼宇
        </button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-4 gap-4">
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">楼宇总数</p>
            <p class="mt-1 text-2xl font-bold text-slate-600">{{ total }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-slate-100 text-slate-600">
            <Building2 class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-slate-500 to-gray-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">教学楼</p>
            <p class="mt-1 text-2xl font-bold text-blue-600">{{ stats.teaching }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-100 text-blue-600">
            <GraduationCap class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-blue-500 to-cyan-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">宿舍楼</p>
            <p class="mt-1 text-2xl font-bold text-emerald-600">{{ stats.dormitory }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600">
            <Home class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-emerald-500 to-teal-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">其他建筑</p>
            <p class="mt-1 text-2xl font-bold text-amber-600">{{ stats.other }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-amber-100 text-amber-600">
            <Warehouse class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-amber-500 to-orange-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
    </div>

    <!-- 搜索筛选 -->
    <div class="mb-6 rounded-xl bg-white p-4 shadow-sm">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <Search class="h-4 w-4 text-gray-400" />
          <input
            v-model="queryForm.buildingNo"
            type="text"
            placeholder="楼号"
            class="w-28 rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500"
          />
        </div>
        <input
          v-model="queryForm.buildingName"
          type="text"
          placeholder="楼宇名称"
          class="w-40 rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500"
        />
        <select
          v-model="queryForm.buildingType"
          class="w-36 rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500"
        >
          <option :value="undefined">全部类型</option>
          <option v-for="item in buildingTypeOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
        <select
          v-model="queryForm.status"
          class="w-28 rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500"
        >
          <option :value="undefined">全部状态</option>
          <option v-for="item in buildingStatusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
        <div class="flex gap-2">
          <button
            @click="handleQuery"
            class="flex items-center gap-1 rounded-lg bg-slate-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-slate-700"
          >
            <Search class="h-4 w-4" />
            查询
          </button>
          <button
            @click="handleReset"
            class="flex items-center gap-1 rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-50"
          >
            <RefreshCw class="h-4 w-4" />
            重置
          </button>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="rounded-xl bg-white shadow-sm">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-100 bg-gray-50/50">
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">楼号</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">楼宇名称</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">类型</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">楼层数</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">房间数</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">位置</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">建造年份</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">关联部门</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">状态</th>
              <th class="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">操作</th>
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr>
              <td colspan="10" class="py-20 text-center">
                <Loader2 class="mx-auto h-8 w-8 animate-spin text-slate-400" />
                <p class="mt-2 text-sm text-gray-500">加载中...</p>
              </td>
            </tr>
          </tbody>
          <tbody v-else-if="tableData.length === 0">
            <tr>
              <td colspan="10" class="py-20 text-center">
                <Building2 class="mx-auto h-12 w-12 text-gray-300" />
                <p class="mt-2 text-sm text-gray-500">暂无楼宇数据</p>
              </td>
            </tr>
          </tbody>
          <tbody v-else class="divide-y divide-gray-100">
            <tr
              v-for="(row, index) in tableData"
              :key="row.id"
              class="transition-colors hover:bg-gray-50/50"
              :style="{ animationDelay: `${index * 30}ms` }"
            >
              <td class="px-4 py-3">
                <span class="inline-flex items-center justify-center rounded-lg bg-slate-100 px-3 py-1 text-sm font-bold text-slate-700">
                  {{ row.buildingNo }}号
                </span>
              </td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-2">
                  <component :is="getBuildingIcon(row.buildingType)" class="h-5 w-5" :class="getBuildingIconColor(row.buildingType)" />
                  <span class="font-medium text-gray-900">{{ row.buildingName }}</span>
                </div>
              </td>
              <td class="px-4 py-3">
                <span
                  class="inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium"
                  :class="getBuildingTypeClass(row.buildingType)"
                >
                  {{ row.buildingTypeName || getBuildingTypeText(row.buildingType) }}
                </span>
              </td>
              <td class="px-4 py-3 text-center">
                <span class="inline-flex items-center gap-1 text-sm text-gray-600">
                  <Layers class="h-4 w-4 text-gray-400" />
                  {{ row.totalFloors }}层
                </span>
              </td>
              <td class="px-4 py-3 text-center">
                <span class="inline-flex items-center justify-center rounded-full bg-gray-100 px-2.5 py-0.5 text-sm font-medium text-gray-700">
                  {{ row.roomCount || 0 }}
                </span>
              </td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-1 text-sm text-gray-600" :title="row.location">
                  <MapPin class="h-4 w-4 flex-shrink-0 text-gray-400" />
                  <span class="max-w-32 truncate">{{ row.location || '-' }}</span>
                </div>
              </td>
              <td class="px-4 py-3 text-center">
                <span v-if="row.constructionYear" class="text-sm text-gray-600">{{ row.constructionYear }}年</span>
                <span v-else class="text-sm text-gray-400">-</span>
              </td>
              <td class="px-4 py-3">
                <div v-if="getDepartmentNamesArray(row.departmentNames).length > 0" class="flex flex-wrap gap-1">
                  <span
                    v-for="(name, idx) in getDepartmentNamesArray(row.departmentNames).slice(0, 2)"
                    :key="idx"
                    class="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700"
                  >
                    {{ name }}
                  </span>
                  <span
                    v-if="getDepartmentNamesArray(row.departmentNames).length > 2"
                    class="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500"
                    :title="getDepartmentNamesArray(row.departmentNames).join(', ')"
                  >
                    +{{ getDepartmentNamesArray(row.departmentNames).length - 2 }}
                  </span>
                </div>
                <span v-else class="text-sm text-gray-400">未关联</span>
              </td>
              <td class="px-4 py-3 text-center">
                <span
                  class="inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium"
                  :class="row.status === 1 ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-600'"
                >
                  <span class="h-1.5 w-1.5 rounded-full" :class="row.status === 1 ? 'bg-emerald-500' : 'bg-gray-400'"></span>
                  {{ getStatusText(row.status) }}
                </span>
              </td>
              <td class="px-4 py-3 text-center">
                <div class="flex items-center justify-center gap-1">
                  <button
                    @click="handleEdit(row)"
                    class="rounded-lg p-1.5 text-slate-600 transition-colors hover:bg-slate-100"
                    title="编辑"
                  >
                    <Pencil class="h-4 w-4" />
                  </button>
                  <button
                    @click="handleDelete(row)"
                    class="rounded-lg p-1.5 text-red-600 transition-colors hover:bg-red-50"
                    title="删除"
                  >
                    <Trash2 class="h-4 w-4" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="flex items-center justify-between border-t border-gray-100 px-6 py-4">
        <div class="text-sm text-gray-500">
          共 <span class="font-medium text-gray-900">{{ total }}</span> 条记录
        </div>
        <div class="flex items-center gap-2">
          <button
            :disabled="queryForm.pageNum === 1"
            @click="queryForm.pageNum--; loadBuildingList()"
            class="rounded-lg border border-gray-200 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            上一页
          </button>
          <span class="px-3 py-1.5 text-sm text-gray-600">
            第 {{ queryForm.pageNum }} / {{ Math.ceil(total / queryForm.pageSize) }} 页
          </span>
          <button
            :disabled="queryForm.pageNum >= Math.ceil(total / queryForm.pageSize)"
            @click="queryForm.pageNum++; loadBuildingList()"
            class="rounded-lg border border-gray-200 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            下一页
          </button>
          <select
            v-model="queryForm.pageSize"
            @change="queryForm.pageNum = 1; loadBuildingList()"
            class="pagination-select"
          >
            <option :value="10">10条/页</option>
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
            <option :value="100">100条/页</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <Teleport to="body">
      <div
        v-if="dialogVisible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
        @click.self="dialogVisible = false"
      >
        <div class="w-full max-w-xl animate-fade-in rounded-2xl bg-white shadow-2xl">
          <!-- 对话框头部 -->
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h3 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
              <Building2 class="h-5 w-5 text-slate-600" />
              {{ dialogTitle }}
            </h3>
            <button
              @click="dialogVisible = false"
              class="rounded-lg p-1.5 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
            >
              <X class="h-5 w-5" />
            </button>
          </div>

          <!-- 对话框内容 -->
          <div class="max-h-[65vh] overflow-y-auto px-6 py-4">
            <div class="space-y-4">
              <!-- 楼号 -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  楼号 <span class="text-red-500">*</span>
                </label>
                <div class="flex">
                  <input
                    v-model="formData.buildingNo"
                    type="text"
                    placeholder="请输入楼号(纯数字,如: 1, 2, 3)"
                    :disabled="isEdit"
                    class="flex-1 rounded-l-lg border border-r-0 border-gray-200 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500 disabled:bg-gray-50 disabled:text-gray-500"
                  />
                  <span class="inline-flex items-center rounded-r-lg border border-gray-200 bg-gray-50 px-3 text-sm text-gray-600">
                    号楼
                  </span>
                </div>
                <p class="mt-1 text-xs text-gray-500">楼号必须为纯数字，如: 1, 2, 3</p>
              </div>

              <!-- 楼宇名称 -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  楼宇名称 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="formData.buildingName"
                  type="text"
                  placeholder="请输入楼宇名称，如: 教学楼A座"
                  class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500"
                />
              </div>

              <!-- 楼宇类型 -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  楼宇类型 <span class="text-red-500">*</span>
                </label>
                <select
                  v-model="formData.buildingType"
                  class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500"
                >
                  <option v-for="item in buildingTypeOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
              </div>

              <!-- 总楼层数 -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  总楼层数 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model.number="formData.totalFloors"
                  type="number"
                  min="1"
                  max="100"
                  placeholder="请输入总楼层数"
                  class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500"
                />
              </div>

              <!-- 位置和建造年份 -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">位置</label>
                  <input
                    v-model="formData.location"
                    type="text"
                    placeholder="请输入位置"
                    class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500"
                  />
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">建造年份</label>
                  <input
                    v-model.number="formData.constructionYear"
                    type="number"
                    min="1900"
                    max="2100"
                    placeholder="请输入建造年份"
                    class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500"
                  />
                </div>
              </div>

              <!-- 关联部门 -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">关联部门</label>
                <div class="rounded-lg border border-gray-200 p-3">
                  <div class="mb-2 flex flex-wrap gap-2">
                    <span
                      v-for="deptId in formData.departmentIds"
                      :key="deptId"
                      class="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700"
                    >
                      {{ getDeptNameById(deptId) }}
                      <button @click="removeDepartment(deptId)" class="rounded-full p-0.5 hover:bg-slate-200">
                        <X class="h-3 w-3" />
                      </button>
                    </span>
                    <span v-if="formData.departmentIds.length === 0" class="text-sm text-gray-400">
                      未选择部门
                    </span>
                  </div>
                  <select
                    @change="addDepartment($event)"
                    class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
                  >
                    <option value="">选择部门...</option>
                    <option
                      v-for="dept in availableDepartments"
                      :key="dept.id"
                      :value="dept.id"
                    >
                      {{ dept.deptName }}
                    </option>
                  </select>
                </div>
                <p class="mt-1 text-xs text-gray-500">选择关联部门后，该部门的用户可以查看和管理此楼宇</p>
              </div>

              <!-- 描述 -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">描述</label>
                <textarea
                  v-model="formData.description"
                  rows="3"
                  placeholder="请输入描述"
                  class="w-full resize-none rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500"
                ></textarea>
              </div>

              <!-- 状态 -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  状态 <span class="text-red-500">*</span>
                </label>
                <div class="flex gap-6">
                  <label
                    v-for="item in buildingStatusOptions"
                    :key="item.value"
                    class="flex cursor-pointer items-center gap-2"
                  >
                    <input
                      type="radio"
                      v-model="formData.status"
                      :value="item.value"
                      class="h-4 w-4 text-slate-600 focus:ring-slate-500"
                    />
                    <span class="text-sm text-gray-700">{{ item.label }}</span>
                  </label>
                </div>
              </div>

              <!-- 提示 -->
              <div v-if="!isEdit" class="flex items-start gap-2 rounded-lg bg-blue-50 p-3">
                <Info class="mt-0.5 h-4 w-4 flex-shrink-0 text-blue-500" />
                <p class="text-sm text-blue-700">
                  创建楼宇后，请在对应的教室管理或宿舍管理中添加具体房间
                </p>
              </div>
            </div>
          </div>

          <!-- 对话框底部 -->
          <div class="flex items-center justify-end gap-3 border-t border-gray-100 px-6 py-4">
            <button
              @click="dialogVisible = false"
              class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              取消
            </button>
            <button
              @click="handleSubmit"
              :disabled="submitLoading"
              class="flex items-center gap-2 rounded-lg bg-slate-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-slate-700 disabled:opacity-50"
            >
              <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
              <Check v-else class="h-4 w-4" />
              确定
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Building2,
  Plus,
  Search,
  RefreshCw,
  GraduationCap,
  Home,
  Warehouse,
  Layers,
  MapPin,
  Pencil,
  Trash2,
  X,
  Check,
  Loader2,
  Info
} from 'lucide-vue-next'
import {
  getBuildingList,
  createBuilding,
  updateBuilding,
  deleteBuilding,
  checkBuildingNoExists
} from '@/api/v2/dormitory'
import { getAllEnabledDepartments, type DepartmentResponse } from '@/api/v2/organization'
import type { Building, BuildingQueryParams, BuildingFormData } from '@/types/building'
import { buildingTypeOptions, buildingStatusOptions } from '@/types/building'

// 查询表单
const queryForm = reactive<BuildingQueryParams>({
  pageNum: 1,
  pageSize: 10
})

// 表格数据
const tableData = ref<Building[]>([])
const total = ref(0)
const loading = ref(false)

// 统计数据
const stats = reactive({
  teaching: 0,
  dormitory: 0,
  other: 0
})

// 部门列表
const departmentList = ref<DepartmentResponse[]>([])

// 对话框
const dialogVisible = ref(false)
const dialogTitle = computed(() => (isEdit.value ? '编辑楼宇' : '新增楼宇'))
const isEdit = ref(false)
const submitLoading = ref(false)

// 表单数据
const formData = reactive<BuildingFormData>({
  buildingNo: '',
  buildingName: '',
  buildingType: 1,
  totalFloors: 6,
  location: '',
  constructionYear: new Date().getFullYear(),
  description: '',
  status: 1,
  departmentIds: []
})

// 当前编辑的ID
const currentEditId = ref<number>()

// 可选部门列表（排除已选）
const availableDepartments = computed(() => {
  return departmentList.value.filter(d => !formData.departmentIds.includes(d.id))
})

// 加载楼宇列表
const loadBuildingList = async () => {
  loading.value = true
  try {
    const res = await getBuildingList(queryForm)
    tableData.value = res.records
    total.value = res.total
    // 计算统计数据
    stats.teaching = res.records.filter((r: Building) => r.buildingType === 1).length
    stats.dormitory = res.records.filter((r: Building) => r.buildingType === 2).length
    stats.other = res.records.filter((r: Building) => r.buildingType === 3).length
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 查询
const handleQuery = () => {
  queryForm.pageNum = 1
  loadBuildingList()
}

// 重置
const handleReset = () => {
  queryForm.buildingNo = undefined
  queryForm.buildingName = undefined
  queryForm.buildingType = undefined
  queryForm.status = undefined
  queryForm.pageNum = 1
  loadBuildingList()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: Building) => {
  isEdit.value = true
  currentEditId.value = row.id
  Object.assign(formData, {
    buildingNo: row.buildingNo,
    buildingName: row.buildingName,
    buildingType: row.buildingType,
    totalFloors: row.totalFloors,
    location: row.location,
    constructionYear: row.constructionYear,
    description: row.description,
    status: row.status,
    departmentIds: row.departmentIds || []
  })
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: Building) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除楼宇 "${row.buildingName}" 吗?此操作不可恢复!`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteBuilding(row.id)
    ElMessage.success('删除成功')
    loadBuildingList()
  } catch (error: any) {
    if (error !== 'cancel') {
      const errorMsg = error.response?.data?.message || error.message
      ElMessage.error(errorMsg || '删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  // 表单验证
  if (!formData.buildingNo) {
    ElMessage.warning('请输入楼号')
    return
  }
  if (!/^[0-9]+$/.test(formData.buildingNo)) {
    ElMessage.warning('楼号必须为纯数字')
    return
  }
  if (!formData.buildingName) {
    ElMessage.warning('请输入楼宇名称')
    return
  }

  // 检查楼号是否已存在
  try {
    const exists = await checkBuildingNoExists(formData.buildingNo, currentEditId.value)
    if (exists) {
      ElMessage.warning('该楼号已存在')
      return
    }
  } catch (error) {
    // 忽略检查错误
  }

  submitLoading.value = true
  try {
    if (isEdit.value && currentEditId.value) {
      await updateBuilding(currentEditId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createBuilding(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadBuildingList()
  } catch (error: any) {
    const errorMsg = error.response?.data?.message || error.message
    ElMessage.error(errorMsg || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 重置表单
const resetForm = () => {
  formData.buildingNo = ''
  formData.buildingName = ''
  formData.buildingType = 1
  formData.totalFloors = 6
  formData.location = ''
  formData.constructionYear = new Date().getFullYear()
  formData.description = ''
  formData.status = 1
  formData.departmentIds = []
  currentEditId.value = undefined
}

// 获取楼宇类型文本
const getBuildingTypeText = (type: number) => {
  const option = buildingTypeOptions.find(item => item.value === type)
  return option?.label || '未知'
}

// 获取楼宇类型样式
const getBuildingTypeClass = (type: number) => {
  const classes: Record<number, string> = {
    1: 'bg-blue-100 text-blue-700',
    2: 'bg-emerald-100 text-emerald-700',
    3: 'bg-amber-100 text-amber-700'
  }
  return classes[type] || 'bg-gray-100 text-gray-700'
}

// 获取楼宇图标
const getBuildingIcon = (type: number) => {
  const icons: Record<number, any> = {
    1: GraduationCap,
    2: Home,
    3: Warehouse
  }
  return icons[type] || Building2
}

// 获取楼宇图标颜色
const getBuildingIconColor = (type: number) => {
  const colors: Record<number, string> = {
    1: 'text-blue-500',
    2: 'text-emerald-500',
    3: 'text-amber-500'
  }
  return colors[type] || 'text-gray-500'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const option = buildingStatusOptions.find(item => item.value === status)
  return option?.label || '未知'
}

// 加载部门列表
const loadDepartmentList = async () => {
  try {
    departmentList.value = await getAllEnabledDepartments()
  } catch (error: any) {
    ElMessage.error(error.message || '加载部门列表失败')
  }
}

// 将部门名称字符串转换为数组
const getDepartmentNamesArray = (departmentNames: string | string[] | undefined): string[] => {
  if (!departmentNames) {
    return []
  }
  if (Array.isArray(departmentNames)) {
    return departmentNames
  }
  if (typeof departmentNames === 'string' && departmentNames.trim()) {
    return departmentNames.split(',').map(name => name.trim()).filter(name => name)
  }
  return []
}

// 根据ID获取部门名称
const getDeptNameById = (id: number) => {
  const dept = departmentList.value.find(d => d.id === id)
  return dept?.deptName || '未知部门'
}

// 添加部门
const addDepartment = (event: Event) => {
  const target = event.target as HTMLSelectElement
  const value = Number(target.value)
  if (value && !formData.departmentIds.includes(value)) {
    formData.departmentIds.push(value)
  }
  target.value = ''
}

// 移除部门
const removeDepartment = (id: number) => {
  const index = formData.departmentIds.indexOf(id)
  if (index > -1) {
    formData.departmentIds.splice(index, 1)
  }
}

// 初始化加载数据
onMounted(() => {
  loadBuildingList()
  loadDepartmentList()
})
</script>

<style scoped>
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fade-in 0.2s ease-out;
}

tbody tr {
  animation: fade-in 0.3s ease-out both;
}
</style>
