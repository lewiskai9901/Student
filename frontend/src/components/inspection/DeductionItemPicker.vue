<!--
  扣分项选择器组件
  用于检查执行时快速选择扣分项并记录扣分
-->
<template>
  <div class="deduction-item-picker">
    <!-- 类别标签页 -->
    <div class="flex gap-1 mb-4 p-1 bg-gray-100 rounded-lg overflow-x-auto">
      <button
        v-for="category in categories"
        :key="category.id"
        @click="activeCategory = category.id"
        class="flex-shrink-0 px-4 py-2 rounded-lg text-sm font-medium transition-colors whitespace-nowrap"
        :class="activeCategory === category.id
          ? 'bg-white text-gray-900 shadow-sm'
          : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'"
      >
        {{ category.name }}
        <span
          v-if="getCategoryDeductionCount(category.id) > 0"
          class="ml-1.5 px-1.5 py-0.5 rounded-full text-xs bg-red-500 text-white"
        >
          {{ getCategoryDeductionCount(category.id) }}
        </span>
      </button>
    </div>

    <!-- 扣分项列表 -->
    <div class="space-y-2 max-h-[400px] overflow-y-auto pr-1">
      <div
        v-for="item in currentCategoryItems"
        :key="item.id"
        class="group relative bg-white rounded-xl border p-4 transition-all cursor-pointer hover:shadow-md"
        :class="isItemSelected(item.id)
          ? 'border-red-300 bg-red-50'
          : 'border-gray-200 hover:border-gray-300'"
        @click="handleItemClick(item)"
      >
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1">
              <span class="font-medium text-gray-900">{{ item.name }}</span>
              <!-- 标记 -->
              <span
                v-if="item.requirePhoto"
                class="px-1.5 py-0.5 rounded text-xs bg-blue-100 text-blue-600"
              >
                需拍照
              </span>
              <span
                v-if="item.requireStudent"
                class="px-1.5 py-0.5 rounded text-xs bg-purple-100 text-purple-600"
              >
                关联学生
              </span>
            </div>
            <div class="text-sm text-gray-500">{{ item.description || '无描述' }}</div>
          </div>

          <div class="flex items-center gap-3 ml-4">
            <!-- 扣分值 -->
            <div class="text-right">
              <div class="text-lg font-bold text-red-600">
                -{{ getItemDeductionDisplay(item) }}
              </div>
              <div v-if="item.deductMode === 'RANGE'" class="text-xs text-gray-400">
                {{ item.minScore }}-{{ item.maxScore }}
              </div>
            </div>

            <!-- 选中标记 -->
            <div
              class="w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors"
              :class="isItemSelected(item.id)
                ? 'bg-red-500 border-red-500'
                : 'border-gray-300 group-hover:border-gray-400'"
            >
              <Check v-if="isItemSelected(item.id)" class="h-4 w-4 text-white" />
            </div>
          </div>
        </div>

        <!-- 选中后显示扣分详情编辑 -->
        <Transition name="expand">
          <div
            v-if="isItemSelected(item.id)"
            class="mt-4 pt-4 border-t border-red-200"
            @click.stop
          >
            <!-- 扣分数量/分值 -->
            <div class="flex flex-wrap items-center gap-4 mb-3">
              <!-- 按人/次扣分 -->
              <div v-if="item.deductMode === 'PER_PERSON'" class="flex items-center gap-2">
                <span class="text-sm text-gray-600">数量：</span>
                <el-input-number
                  v-model="selectedItems[item.id].count"
                  :min="1"
                  :max="20"
                  size="small"
                  class="w-24"
                  @change="updateDeduction(item)"
                />
                <span class="text-sm text-gray-500">人/次</span>
              </div>

              <!-- 区间扣分 -->
              <div v-if="item.deductMode === 'RANGE'" class="flex items-center gap-2">
                <span class="text-sm text-gray-600">扣分：</span>
                <el-input-number
                  v-model="selectedItems[item.id].score"
                  :min="item.minScore"
                  :max="item.maxScore"
                  :step="0.5"
                  size="small"
                  class="w-24"
                />
              </div>

              <!-- 扣分小计 -->
              <div class="text-sm">
                <span class="text-gray-500">小计：</span>
                <span class="font-bold text-red-600">-{{ selectedItems[item.id].totalScore }}</span>
              </div>
            </div>

            <!-- 备注输入 -->
            <div class="mb-3">
              <el-input
                v-model="selectedItems[item.id].remark"
                type="textarea"
                :rows="2"
                placeholder="添加备注说明（可选）"
                maxlength="200"
              />
            </div>

            <!-- 拍照/上传图片 -->
            <div v-if="item.requirePhoto" class="mb-3">
              <div class="text-sm text-gray-600 mb-2">
                <Camera class="h-4 w-4 inline-block mr-1" />
                证据照片
                <span class="text-red-500">*</span>
              </div>
              <el-upload
                v-model:file-list="selectedItems[item.id].photos"
                :action="uploadUrl"
                list-type="picture-card"
                :limit="5"
                accept="image/*"
              >
                <Plus class="h-6 w-6 text-gray-400" />
              </el-upload>
            </div>

            <!-- 关联学生 -->
            <div v-if="item.requireStudent">
              <div class="text-sm text-gray-600 mb-2">
                <User class="h-4 w-4 inline-block mr-1" />
                关联学生
                <span class="text-red-500">*</span>
              </div>
              <el-select
                v-model="selectedItems[item.id].studentIds"
                multiple
                filterable
                remote
                :remote-method="searchStudents"
                placeholder="搜索并选择学生"
                class="w-full"
              >
                <el-option
                  v-for="student in studentOptions"
                  :key="student.id"
                  :label="`${student.name} (${student.studentNo})`"
                  :value="student.id"
                />
              </el-select>
            </div>
          </div>
        </Transition>
      </div>

      <!-- 空状态 -->
      <div v-if="currentCategoryItems.length === 0" class="py-12 text-center text-gray-400">
        <FileText class="h-12 w-12 mx-auto mb-3 text-gray-300" />
        <p>该类别暂无扣分项</p>
      </div>
    </div>

    <!-- 底部汇总 -->
    <div class="mt-4 pt-4 border-t border-gray-200">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <span class="text-gray-600">已选扣分项：</span>
          <span class="text-lg font-bold text-gray-900">{{ selectedCount }} 项</span>
        </div>
        <div class="flex items-center gap-4">
          <span class="text-gray-600">扣分合计：</span>
          <span class="text-2xl font-bold text-red-600">-{{ totalDeduction.toFixed(1) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { Check, Camera, User, Plus, FileText } from 'lucide-vue-next'

// 类型定义
interface DeductionItem {
  id: number
  name: string
  description?: string
  categoryId: number
  deductMode: 'FIXED' | 'PER_PERSON' | 'RANGE'
  score: number
  minScore?: number
  maxScore?: number
  requirePhoto: boolean
  requireRemark: boolean
  requireStudent: boolean
}

interface Category {
  id: number
  name: string
  items: DeductionItem[]
}

interface SelectedItem {
  itemId: number
  count: number
  score: number
  totalScore: number
  remark: string
  photos: any[]
  studentIds: number[]
}

interface Student {
  id: number
  name: string
  studentNo: string
}

// Props
const props = defineProps<{
  categories: Category[]
  uploadUrl?: string
}>()

// Emits
const emit = defineEmits<{
  change: [items: SelectedItem[]]
}>()

// 状态
const activeCategory = ref(props.categories[0]?.id || 0)
const selectedItems = reactive<Record<number, SelectedItem>>({})
const studentOptions = ref<Student[]>([])

// 计算属性
const currentCategoryItems = computed(() => {
  const category = props.categories.find(c => c.id === activeCategory.value)
  return category?.items || []
})

const selectedCount = computed(() => Object.keys(selectedItems).length)

const totalDeduction = computed(() => {
  return Object.values(selectedItems).reduce((sum, item) => sum + item.totalScore, 0)
})

// 方法
function isItemSelected(itemId: number): boolean {
  return itemId in selectedItems
}

function getCategoryDeductionCount(categoryId: number): number {
  const category = props.categories.find(c => c.id === categoryId)
  if (!category) return 0
  return category.items.filter(item => isItemSelected(item.id)).length
}

function getItemDeductionDisplay(item: DeductionItem): string {
  if (item.deductMode === 'RANGE') {
    return `${item.minScore}-${item.maxScore}`
  }
  if (item.deductMode === 'PER_PERSON') {
    return `${item.score}/人`
  }
  return String(item.score)
}

function handleItemClick(item: DeductionItem) {
  if (isItemSelected(item.id)) {
    // 取消选择
    delete selectedItems[item.id]
  } else {
    // 选择
    const initialScore = item.deductMode === 'RANGE' ? (item.minScore || 0) : item.score
    selectedItems[item.id] = {
      itemId: item.id,
      count: 1,
      score: initialScore,
      totalScore: initialScore,
      remark: '',
      photos: [],
      studentIds: []
    }
  }
  emitChange()
}

function updateDeduction(item: DeductionItem) {
  if (!isItemSelected(item.id)) return

  const selected = selectedItems[item.id]
  if (item.deductMode === 'PER_PERSON') {
    selected.totalScore = item.score * selected.count
  } else if (item.deductMode === 'RANGE') {
    selected.totalScore = selected.score
  } else {
    selected.totalScore = item.score
  }
  emitChange()
}

function searchStudents(query: string) {
  // 模拟搜索学生
  // 实际应调用 API
  if (!query) {
    studentOptions.value = []
    return
  }
  // TODO: 调用学生搜索API
  studentOptions.value = [
    { id: 1, name: '张三', studentNo: '2021001' },
    { id: 2, name: '李四', studentNo: '2021002' }
  ]
}

function emitChange() {
  emit('change', Object.values(selectedItems))
}

// 监听变化
watch(() => selectedItems, () => {
  emitChange()
}, { deep: true })

// 暴露方法
defineExpose({
  getSelectedItems: () => Object.values(selectedItems),
  clearAll: () => {
    Object.keys(selectedItems).forEach(key => {
      delete selectedItems[Number(key)]
    })
  }
})
</script>

<style scoped>
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
  margin-top: 0;
  padding-top: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 500px;
}
</style>
