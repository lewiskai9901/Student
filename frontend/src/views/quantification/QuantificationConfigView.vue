<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        title="检查类别"
        :value="categoryList.length"
        :icon="FolderOpen"
        subtitle="类别总数"
        color="blue"
      />
      <StatCard
        title="扣分项"
        :value="deductionList.length"
        :icon="ListChecks"
        subtitle="配置项目"
        color="orange"
      />
      <StatCard
        title="启用类别"
        :value="enabledCategoryCount"
        :icon="CheckCircle"
        subtitle="正在使用"
        color="emerald"
      />
      <StatCard
        title="禁用类别"
        :value="categoryList.length - enabledCategoryCount"
        :icon="XCircle"
        subtitle="已禁用"
        color="gray"
      />
    </div>

    <!-- Tab 导航 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-1">
      <div class="flex gap-1">
        <button
          @click="activeTab = 'categories'"
          :class="[
            'flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium',
            activeTab === 'categories' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100'
          ]"
        >
          <FolderOpen class="h-4 w-4" />
          检查类别
        </button>
        <button
          @click="activeTab = 'deductions'"
          :class="[
            'flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium',
            activeTab === 'deductions' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100'
          ]"
        >
          <ListChecks class="h-4 w-4" />
          扣分项配置
        </button>
      </div>
    </div>

    <!-- 检查类别内容 -->
    <div v-if="activeTab === 'categories'" class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <span class="font-medium text-gray-900">检查类别列表</span>
        <button
          v-if="hasPermission('quantification:config:add')"
          @click="handleAddCategory"
          class="h-9 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
        >
          <Plus class="mr-1.5 inline h-4 w-4" />
          新增类别
        </button>
      </div>
      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">类别名称</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">类别编码</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">描述</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">排序</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">操作</th>
          </tr>
        </thead>
        <tbody v-if="categoryLoading">
          <tr>
            <td colspan="6" class="py-16 text-center">
              <Loader2 class="mx-auto h-6 w-6 animate-spin text-gray-400" />
              <p class="mt-2 text-sm text-gray-500">加载中...</p>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="categoryList.length === 0">
          <tr>
            <td colspan="6" class="py-16 text-center">
              <FolderOpen class="mx-auto h-10 w-10 text-gray-300" />
              <p class="mt-2 text-sm text-gray-500">暂无检查类别</p>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr v-for="row in categoryList" :key="row.id" class="border-b border-gray-100 hover:bg-gray-50">
            <td class="px-4 py-3">
              <div class="flex items-center gap-2">
                <FolderOpen class="h-4 w-4 text-blue-600" />
                <span class="font-medium text-gray-900">{{ row.categoryName }}</span>
              </div>
            </td>
            <td class="px-4 py-3">
              <code class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-600">{{ row.categoryCode }}</code>
            </td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.description || '-' }}</td>
            <td class="px-4 py-3 text-center text-sm text-gray-600">{{ row.sortOrder }}</td>
            <td class="px-4 py-3 text-center">
              <span
                :class="['rounded px-2 py-0.5 text-xs', row.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600']"
              >
                {{ row.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-center gap-1">
                <button
                  v-if="hasPermission('quantification:config:edit')"
                  @click="handleEditCategory(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-gray-100 hover:text-blue-600"
                  title="编辑"
                >
                  <Pencil class="h-4 w-4" />
                </button>
                <button
                  v-if="hasPermission('quantification:config:delete')"
                  @click="handleDeleteCategory(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-gray-100 hover:text-red-600"
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

    <!-- 扣分项配置内容 -->
    <div v-if="activeTab === 'deductions'" class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-4">
          <span class="font-medium text-gray-900">扣分项列表</span>
          <select
            v-model="selectedCategoryId"
            @change="handleCategoryChange"
            class="h-9 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option :value="null">选择检查类别</option>
            <option v-for="cat in categoryList" :key="cat.id" :value="cat.id">{{ cat.categoryName }}</option>
          </select>
        </div>
        <button
          v-if="hasPermission('quantification:config:add')"
          @click="handleAddDeduction"
          :disabled="!selectedCategoryId"
          class="h-9 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          <Plus class="mr-1.5 inline h-4 w-4" />
          新增扣分项
        </button>
      </div>
      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">扣分项名称</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">扣分项编码</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">扣分模式</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">扣分分值</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">描述</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">排序</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">操作</th>
          </tr>
        </thead>
        <tbody v-if="deductionLoading">
          <tr>
            <td colspan="8" class="py-16 text-center">
              <Loader2 class="mx-auto h-6 w-6 animate-spin text-gray-400" />
              <p class="mt-2 text-sm text-gray-500">加载中...</p>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="!selectedCategoryId">
          <tr>
            <td colspan="8" class="py-16 text-center">
              <ListChecks class="mx-auto h-10 w-10 text-gray-300" />
              <p class="mt-2 text-sm text-gray-500">请先选择检查类别</p>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="deductionList.length === 0">
          <tr>
            <td colspan="8" class="py-16 text-center">
              <ListChecks class="mx-auto h-10 w-10 text-gray-300" />
              <p class="mt-2 text-sm text-gray-500">暂无扣分项</p>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr v-for="row in deductionList" :key="row.id" class="border-b border-gray-100 hover:bg-gray-50">
            <td class="px-4 py-3 font-medium text-gray-900">{{ row.itemName }}</td>
            <td class="px-4 py-3">
              <code class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-600">{{ row.itemCode }}</code>
            </td>
            <td class="px-4 py-3 text-center">
              <span :class="['rounded px-2 py-0.5 text-xs', getDeductModeClass(row.deductMode || 1)]">
                {{ getDeductionModeText(row.deductMode || 1) }}
              </span>
            </td>
            <td class="px-4 py-3 text-center">
              <template v-if="row.deductMode === 1">
                <span class="text-sm font-medium text-red-600">-{{ row.fixedScore }}分</span>
              </template>
              <template v-else-if="row.deductMode === 2">
                <div class="text-xs text-gray-600">基础{{ row.baseScore }}分+{{ row.perPersonScore }}分/人</div>
              </template>
              <template v-else-if="row.deductMode === 3">
                <span class="text-xs text-gray-600">{{ formatRangeConfig(row.rangeConfig) }}</span>
              </template>
            </td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.description || '-' }}</td>
            <td class="px-4 py-3 text-center text-sm text-gray-600">{{ row.sortOrder }}</td>
            <td class="px-4 py-3 text-center">
              <span :class="['rounded px-2 py-0.5 text-xs', row.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600']">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-center gap-1">
                <button
                  v-if="hasPermission('quantification:config:edit')"
                  @click="handleEditDeduction(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-gray-100 hover:text-blue-600"
                  title="编辑"
                >
                  <Pencil class="h-4 w-4" />
                </button>
                <button
                  v-if="hasPermission('quantification:config:delete')"
                  @click="handleDeleteDeduction(row)"
                  class="rounded p-1.5 text-gray-500 hover:bg-gray-100 hover:text-red-600"
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

    <!-- 类别编辑弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="categoryDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="categoryDialogVisible = false"></div>
          <div class="relative z-10 w-full max-w-lg rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">{{ categoryEditMode === 'add' ? '新增检查类别' : '编辑检查类别' }}</h3>
              <button @click="categoryDialogVisible = false" class="rounded p-1 text-gray-400 hover:bg-gray-100">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="max-h-[65vh] overflow-y-auto p-6 space-y-4">
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">类别名称 <span class="text-red-500">*</span></label>
                <input
                  v-model="categoryForm.categoryName"
                  type="text"
                  placeholder="例如:卫生检查"
                  class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                />
              </div>
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">类别编码 <span class="text-red-500">*</span></label>
                <input
                  v-model="categoryForm.categoryCode"
                  type="text"
                  placeholder="例如:hygiene"
                  class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                />
              </div>
              <div>
                <label class="mb-2 block text-sm font-medium text-gray-700">状态</label>
                <div class="flex gap-6">
                  <label class="flex items-center gap-2">
                    <input type="radio" v-model="categoryForm.status" :value="1" class="h-4 w-4 text-blue-600" />
                    <span class="text-sm text-gray-700">启用</span>
                  </label>
                  <label class="flex items-center gap-2">
                    <input type="radio" v-model="categoryForm.status" :value="0" class="h-4 w-4 text-blue-600" />
                    <span class="text-sm text-gray-700">禁用</span>
                  </label>
                </div>
              </div>
            </div>
            <div class="flex items-center justify-end gap-3 border-t border-gray-200 px-6 py-4">
              <button @click="categoryDialogVisible = false" class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50">
                取消
              </button>
              <button @click="handleCategorySubmit" class="h-9 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700">
                确定
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 扣分项编辑弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="deductionDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="deductionDialogVisible = false"></div>
          <div class="relative z-10 w-full max-w-xl rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">{{ deductionEditMode === 'add' ? '新增扣分项' : '编辑扣分项' }}</h3>
              <button @click="deductionDialogVisible = false" class="rounded p-1 text-gray-400 hover:bg-gray-100">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="max-h-[65vh] overflow-y-auto p-6 space-y-4">
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">所属类别 <span class="text-red-500">*</span></label>
                <select
                  v-model="deductionForm.typeId"
                  class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                >
                  <option :value="null">请选择类别</option>
                  <option v-for="cat in categoryList" :key="cat.id" :value="cat.id">{{ cat.categoryName }}</option>
                </select>
              </div>
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">扣分项名称 <span class="text-red-500">*</span></label>
                <input
                  v-model="deductionForm.itemName"
                  type="text"
                  placeholder="例如:迟到"
                  class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                />
              </div>
              <div>
                <label class="mb-2 block text-sm font-medium text-gray-700">扣分模式 <span class="text-red-500">*</span></label>
                <div class="flex flex-wrap gap-4">
                  <label class="flex items-center gap-2">
                    <input type="radio" v-model="deductionForm.deductMode" :value="1" class="h-4 w-4 text-blue-600" />
                    <span class="text-sm text-gray-700">固定扣分</span>
                  </label>
                  <label class="flex items-center gap-2">
                    <input type="radio" v-model="deductionForm.deductMode" :value="2" class="h-4 w-4 text-blue-600" />
                    <span class="text-sm text-gray-700">按人数扣分</span>
                  </label>
                  <label class="flex items-center gap-2">
                    <input type="radio" v-model="deductionForm.deductMode" :value="3" class="h-4 w-4 text-blue-600" />
                    <span class="text-sm text-gray-700">区间扣分</span>
                  </label>
                </div>
              </div>
              <!-- 固定扣分模式 -->
              <div v-if="deductionForm.deductMode === 1" class="rounded-lg bg-gray-50 p-4">
                <label class="mb-1 block text-sm font-medium text-gray-700">扣分分数</label>
                <div class="flex items-center gap-2">
                  <input
                    v-model.number="deductionForm.fixedScore"
                    type="number"
                    min="0"
                    step="0.1"
                    class="h-9 w-28 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                  <span class="text-sm text-gray-500">分</span>
                </div>
              </div>
              <!-- 按人数扣分模式 -->
              <div v-else-if="deductionForm.deductMode === 2" class="rounded-lg bg-gray-50 p-4 space-y-3">
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">基础扣分</label>
                  <div class="flex items-center gap-2">
                    <input
                      v-model.number="deductionForm.baseScore"
                      type="number"
                      min="0"
                      step="0.1"
                      class="h-9 w-28 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    />
                    <span class="text-sm text-gray-500">分</span>
                  </div>
                </div>
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">每人扣分</label>
                  <div class="flex items-center gap-2">
                    <input
                      v-model.number="deductionForm.perPersonScore"
                      type="number"
                      min="0"
                      step="0.1"
                      class="h-9 w-28 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    />
                    <span class="text-sm text-gray-500">分/人</span>
                  </div>
                </div>
              </div>
              <!-- 区间扣分模式 -->
              <div v-else-if="deductionForm.deductMode === 3" class="rounded-lg bg-gray-50 p-4">
                <label class="mb-1 block text-sm font-medium text-gray-700">数值区间</label>
                <div class="flex items-center gap-2">
                  <input
                    v-model.number="deductionForm.rangeMin"
                    type="number"
                    min="0"
                    step="0.1"
                    placeholder="最小值"
                    class="h-9 w-24 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                  <span class="text-gray-400">-</span>
                  <input
                    v-model.number="deductionForm.rangeMax"
                    type="number"
                    min="0"
                    step="0.1"
                    placeholder="最大值"
                    class="h-9 w-24 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                  <span class="text-sm text-gray-500">分</span>
                </div>
              </div>
              <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">描述</label>
                <textarea
                  v-model="deductionForm.description"
                  rows="2"
                  placeholder="请输入描述"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                ></textarea>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1 block text-sm font-medium text-gray-700">排序</label>
                  <input
                    v-model.number="deductionForm.sortOrder"
                    type="number"
                    min="0"
                    class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label class="mb-2 block text-sm font-medium text-gray-700">状态</label>
                  <div class="flex gap-6">
                    <label class="flex items-center gap-2">
                      <input type="radio" v-model="deductionForm.status" :value="1" class="h-4 w-4 text-blue-600" />
                      <span class="text-sm text-gray-700">启用</span>
                    </label>
                    <label class="flex items-center gap-2">
                      <input type="radio" v-model="deductionForm.status" :value="0" class="h-4 w-4 text-blue-600" />
                      <span class="text-sm text-gray-700">禁用</span>
                    </label>
                  </div>
                </div>
              </div>
            </div>
            <div class="flex items-center justify-end gap-3 border-t border-gray-200 px-6 py-4">
              <button @click="deductionDialogVisible = false" class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50">
                取消
              </button>
              <button @click="handleDeductionSubmit" class="h-9 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700">
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
import { FolderOpen, ListChecks, CheckCircle, XCircle, Plus, Pencil, Trash2, X, Loader2 } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import type { CheckCategory } from '@/types/quantification'
import { getAllCategories, createCategory, updateCategory, deleteCategory } from '@/api/quantification'
import {
  getDeductionItemsByTypeId, getDeductionItemById,
  createDeductionItem as createDeductItem, updateDeductionItem as updateDeductItem, deleteDeductionItem as deleteDeductItem
} from '@/api/deductionItems'

const authStore = useAuthStore()
const hasPermission = (permission: string) => authStore.hasPermission(permission)

const activeTab = ref('categories')

const categoryLoading = ref(false)
const categoryList = ref<CheckCategory[]>([])
const categoryDialogVisible = ref(false)
const categoryEditMode = ref<'add' | 'edit'>('add')
const currentCategoryId = ref<string | number | null>(null)
const categoryForm = reactive<any>({
  categoryName: '',
  categoryCode: '',
  status: 1
})

const enabledCategoryCount = computed(() => categoryList.value.filter(c => c.status === 1).length)

const deductionLoading = ref(false)
const deductionList = ref<any[]>([])
const selectedCategoryId = ref<string | number | null>(null)
const deductionDialogVisible = ref(false)
const deductionEditMode = ref<'add' | 'edit'>('add')
const currentDeductionId = ref<string | number | null>(null)
const deductionForm = reactive<any>({
  typeId: null,
  itemName: '',
  deductMode: 1,
  fixedScore: 0,
  baseScore: 0,
  perPersonScore: 0,
  rangeMin: 0,
  rangeMax: 0,
  description: '',
  sortOrder: 0,
  status: 1
})

const getDeductModeClass = (mode: number) => {
  const classes: Record<number, string> = { 1: 'bg-green-100 text-green-700', 2: 'bg-amber-100 text-amber-700', 3: 'bg-blue-100 text-blue-700' }
  return classes[mode] || 'bg-gray-100 text-gray-600'
}

const getDeductionModeText = (mode: number) => {
  const map: Record<number, string> = { 1: '固定扣分', 2: '按人数扣分', 3: '区间扣分' }
  return map[mode] || '未知'
}

const formatRangeConfig = (rangeConfigStr: string) => {
  try {
    const ranges = JSON.parse(rangeConfigStr)
    if (ranges && ranges.length > 0) return `${ranges[0].min}-${ranges[0].max}分`
  } catch (e) {}
  return '未配置'
}

const loadCategories = async () => {
  categoryLoading.value = true
  try {
    categoryList.value = await getAllCategories()
  } catch (error: any) {
    ElMessage.error(error.message || '加载类别列表失败')
  } finally {
    categoryLoading.value = false
  }
}

const loadDeductions = async () => {
  if (!selectedCategoryId.value) {
    deductionList.value = []
    return
  }
  deductionLoading.value = true
  try {
    deductionList.value = await getDeductionItemsByTypeId(selectedCategoryId.value)
  } catch (error: any) {
    ElMessage.error(error.message || '加载扣分项列表失败')
  } finally {
    deductionLoading.value = false
  }
}

const handleAddCategory = () => {
  categoryEditMode.value = 'add'
  currentCategoryId.value = null
  Object.assign(categoryForm, { categoryName: '', categoryCode: '', status: 1 })
  categoryDialogVisible.value = true
}

const handleEditCategory = (row: CheckCategory) => {
  categoryEditMode.value = 'edit'
  currentCategoryId.value = row.id
  Object.assign(categoryForm, {
    categoryName: row.typeName || row.categoryName,
    categoryCode: row.typeCode || row.categoryCode,
    status: row.isActive !== undefined ? row.isActive : row.status
  })
  categoryDialogVisible.value = true
}

const handleCategorySubmit = async () => {
  if (!categoryForm.categoryName) { ElMessage.warning('请输入类别名称'); return }
  if (!categoryForm.categoryCode) { ElMessage.warning('请输入类别编码'); return }
  try {
    const submitData = {
      categoryName: categoryForm.categoryName,
      categoryCode: categoryForm.categoryCode,
      status: categoryForm.status
    }
    if (categoryEditMode.value === 'add') {
      await createCategory(submitData)
      ElMessage.success('创建成功')
    } else {
      await updateCategory(currentCategoryId.value!, submitData)
      ElMessage.success('更新成功')
    }
    categoryDialogVisible.value = false
    loadCategories()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDeleteCategory = async (row: CheckCategory) => {
  try {
    await ElMessageBox.confirm(`确定要删除类别"${row.categoryName}"吗？`, '删除确认', { type: 'warning' })
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    loadCategories()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const handleCategoryChange = () => loadDeductions()

const handleAddDeduction = () => {
  deductionEditMode.value = 'add'
  currentDeductionId.value = null
  Object.assign(deductionForm, { typeId: selectedCategoryId.value, itemName: '', deductMode: 1, fixedScore: 0, baseScore: 0, perPersonScore: 0, rangeMin: 0, rangeMax: 0, description: '', sortOrder: 0, status: 1 })
  deductionDialogVisible.value = true
}

const handleEditDeduction = async (row: any) => {
  deductionEditMode.value = 'edit'
  currentDeductionId.value = row.id
  try {
    const latestData = await getDeductionItemById(row.id)
    let rangeMin = 0, rangeMax = 0
    if (latestData.rangeConfig) {
      try {
        const ranges = JSON.parse(latestData.rangeConfig)
        if (ranges && ranges.length > 0) { rangeMin = ranges[0].min || 0; rangeMax = ranges[0].max || 0 }
      } catch (e) {}
    }
    Object.assign(deductionForm, {
      typeId: latestData.typeId,
      itemName: latestData.itemName,
      deductMode: latestData.deductMode || 1,
      fixedScore: latestData.fixedScore || 0,
      baseScore: latestData.baseScore || 0,
      perPersonScore: latestData.perPersonScore || 0,
      rangeMin, rangeMax,
      description: latestData.description || '',
      sortOrder: latestData.sortOrder || 0,
      status: latestData.status !== undefined ? latestData.status : 1
    })
    deductionDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '加载扣分项详情失败')
  }
}

const handleDeductionSubmit = async () => {
  if (!deductionForm.typeId) { ElMessage.warning('请选择所属类别'); return }
  if (!deductionForm.itemName) { ElMessage.warning('请输入扣分项名称'); return }
  try {
    const submitData: any = {
      typeId: deductionForm.typeId,
      itemName: deductionForm.itemName,
      deductMode: deductionForm.deductMode,
      description: deductionForm.description,
      sortOrder: deductionForm.sortOrder,
      status: deductionForm.status
    }
    if (deductionForm.deductMode === 1) submitData.fixedScore = deductionForm.fixedScore
    else if (deductionForm.deductMode === 2) { submitData.baseScore = deductionForm.baseScore; submitData.perPersonScore = deductionForm.perPersonScore }
    else if (deductionForm.deductMode === 3) submitData.rangeConfig = JSON.stringify([{ min: deductionForm.rangeMin, max: deductionForm.rangeMax }])

    if (deductionEditMode.value === 'add') {
      await createDeductItem(submitData)
      ElMessage.success('创建成功')
    } else {
      await updateDeductItem(currentDeductionId.value!, submitData)
      ElMessage.success('更新成功')
    }
    deductionDialogVisible.value = false
    loadDeductions()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDeleteDeduction = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除扣分项"${row.itemName}"吗？`, '删除确认', { type: 'warning' })
    await deleteDeductItem(row.id)
    ElMessage.success('删除成功')
    loadDeductions()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

onMounted(() => loadCategories())
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
