<template>
  <div class="h-full flex flex-col">
    <!-- 头部 -->
    <div class="px-6 py-4 border-b border-gray-200 bg-white flex items-center justify-between">
      <div>
        <h2 class="text-lg font-semibold text-gray-900">
          {{ isNew ? '新增扣分项' : '编辑扣分项' }}
        </h2>
        <p class="text-sm text-gray-500 mt-0.5">
          所属类别：<span class="text-blue-600">{{ categoryName }}</span>
        </p>
      </div>
      <button @click="emit('cancel')" class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg">
        <X class="h-5 w-5" />
      </button>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 overflow-auto p-6">
      <div class="bg-white rounded-xl border border-gray-200 p-6 max-w-2xl">
        <h3 class="text-sm font-medium text-gray-900 mb-4 flex items-center gap-2">
          <ListChecks class="h-4 w-4 text-amber-500" />
          扣分项信息
        </h3>

        <div class="space-y-4">
          <!-- 所属类别 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              所属类别 <span class="text-red-500">*</span>
            </label>
            <select
              v-model="form.typeId"
              class="w-full h-10 px-3 border border-gray-300 rounded-lg text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
            >
              <option :value="null">请选择类别</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                {{ cat.categoryName }}
              </option>
            </select>
          </div>

          <!-- 扣分项名称 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              扣分项名称 <span class="text-red-500">*</span>
            </label>
            <input
              v-model="form.itemName"
              type="text"
              placeholder="例如：迟到"
              class="w-full h-10 px-3 border border-gray-300 rounded-lg text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
            />
          </div>

          <!-- 扣分模式 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              扣分模式 <span class="text-red-500">*</span>
            </label>
            <div class="grid grid-cols-3 gap-3">
              <button
                v-for="mode in deductModes"
                :key="mode.value"
                @click="form.deductMode = mode.value"
                :class="[
                  'p-3 rounded-lg border-2 text-left transition-all',
                  form.deductMode === mode.value
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                ]"
              >
                <div class="text-sm font-medium" :class="form.deductMode === mode.value ? 'text-blue-700' : 'text-gray-900'">
                  {{ mode.label }}
                </div>
                <div class="text-xs mt-0.5" :class="form.deductMode === mode.value ? 'text-blue-500' : 'text-gray-400'">
                  {{ mode.desc }}
                </div>
              </button>
            </div>
          </div>

          <!-- 固定扣分配置 -->
          <div v-if="form.deductMode === 1" class="bg-gray-50 rounded-lg p-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">扣分分数</label>
            <div class="flex items-center gap-2">
              <input
                v-model.number="form.fixedScore"
                type="number"
                min="0"
                step="0.1"
                class="w-32 h-9 px-3 border border-gray-300 rounded-lg text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
              />
              <span class="text-sm text-gray-500">分</span>
            </div>
          </div>

          <!-- 按人数扣分配置 -->
          <div v-else-if="form.deductMode === 2" class="bg-gray-50 rounded-lg p-4 space-y-3">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">基础扣分</label>
              <div class="flex items-center gap-2">
                <input
                  v-model.number="form.baseScore"
                  type="number"
                  min="0"
                  step="0.1"
                  class="w-32 h-9 px-3 border border-gray-300 rounded-lg text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
                />
                <span class="text-sm text-gray-500">分</span>
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">每人扣分</label>
              <div class="flex items-center gap-2">
                <input
                  v-model.number="form.perPersonScore"
                  type="number"
                  min="0"
                  step="0.1"
                  class="w-32 h-9 px-3 border border-gray-300 rounded-lg text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
                />
                <span class="text-sm text-gray-500">分/人</span>
              </div>
            </div>
            <p class="text-xs text-gray-400">总扣分 = 基础扣分 + 每人扣分 × 人数</p>
          </div>

          <!-- 区间扣分配置 -->
          <div v-else-if="form.deductMode === 3" class="bg-gray-50 rounded-lg p-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">分数区间</label>
            <div class="flex items-center gap-2">
              <input
                v-model.number="form.rangeMin"
                type="number"
                min="0"
                step="0.1"
                placeholder="最小值"
                class="w-28 h-9 px-3 border border-gray-300 rounded-lg text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
              />
              <span class="text-gray-400">-</span>
              <input
                v-model.number="form.rangeMax"
                type="number"
                min="0"
                step="0.1"
                placeholder="最大值"
                class="w-28 h-9 px-3 border border-gray-300 rounded-lg text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
              />
              <span class="text-sm text-gray-500">分</span>
            </div>
            <p class="text-xs text-gray-400 mt-2">检查时可在此区间内选择具体扣分值</p>
          </div>

          <!-- 描述 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
            <textarea
              v-model="form.description"
              rows="2"
              placeholder="对该扣分项的补充说明..."
              class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
            ></textarea>
          </div>

          <!-- 排序和状态 -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">排序</label>
              <input
                v-model.number="form.sortOrder"
                type="number"
                min="0"
                class="w-full h-10 px-3 border border-gray-300 rounded-lg text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">状态</label>
              <div class="flex gap-4 pt-1">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="radio" v-model="form.status" :value="1" class="h-4 w-4 text-blue-600" />
                  <span class="text-sm text-gray-700">启用</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="radio" v-model="form.status" :value="0" class="h-4 w-4 text-blue-600" />
                  <span class="text-sm text-gray-700">禁用</span>
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作 -->
    <div class="px-6 py-4 border-t border-gray-200 bg-white flex items-center justify-between">
      <div>
        <button
          v-if="!isNew"
          @click="handleDelete"
          class="px-4 py-2 text-sm text-red-600 hover:bg-red-50 rounded-lg"
        >
          删除扣分项
        </button>
      </div>
      <div class="flex gap-3">
        <button
          @click="emit('cancel')"
          class="px-4 py-2 text-sm text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50"
        >
          取消
        </button>
        <button
          @click="handleSubmit"
          :disabled="saving"
          class="px-6 py-2 text-sm text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50 flex items-center gap-2"
        >
          <Loader2 v-if="saving" class="h-4 w-4 animate-spin" />
          保存
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { X, ListChecks, Loader2 } from 'lucide-vue-next'
import {
  createDeductionItem, updateDeductionItem, deleteDeductionItem
} from '@/api/deductionItems'

const props = defineProps<{
  deduction: any
  category?: any
  categories: any[]
  isNew?: boolean
}>()

const emit = defineEmits(['saved', 'deleted', 'cancel'])

const saving = ref(false)

const deductModes = [
  { value: 1, label: '固定扣分', desc: '每次扣固定分数' },
  { value: 2, label: '按人数扣分', desc: '基础分+人数×单价' },
  { value: 3, label: '区间扣分', desc: '在区间内选择分数' }
]

const form = reactive({
  typeId: null as number | null,
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

const categoryName = computed(() => {
  if (props.category) return props.category.categoryName
  const cat = props.categories.find(c => c.id === form.typeId)
  return cat?.categoryName || '未选择'
})

// 提交
const handleSubmit = async () => {
  if (!form.typeId) {
    ElMessage.warning('请选择所属类别')
    return
  }
  if (!form.itemName.trim()) {
    ElMessage.warning('请输入扣分项名称')
    return
  }

  saving.value = true
  try {
    const submitData: any = {
      typeId: form.typeId,
      itemName: form.itemName,
      deductMode: form.deductMode,
      description: form.description,
      sortOrder: form.sortOrder,
      status: form.status
    }

    if (form.deductMode === 1) {
      submitData.fixedScore = form.fixedScore
    } else if (form.deductMode === 2) {
      submitData.baseScore = form.baseScore
      submitData.perPersonScore = form.perPersonScore
    } else if (form.deductMode === 3) {
      submitData.rangeConfig = JSON.stringify([{ min: form.rangeMin, max: form.rangeMax }])
    }

    if (props.isNew) {
      await createDeductionItem(submitData)
      ElMessage.success('创建成功')
    } else {
      await updateDeductionItem(props.deduction.id, submitData)
      ElMessage.success('更新成功')
    }
    emit('saved', form.typeId)
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 删除
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除扣分项"${form.itemName}"吗？`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteDeductionItem(props.deduction.id)
    ElMessage.success('删除成功')
    emit('deleted', form.typeId)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 监听props变化，更新表单
watch(() => props.deduction, (val) => {
  if (val) {
    form.typeId = val.typeId || null
    form.itemName = val.itemName || ''
    form.deductMode = val.deductMode || 1
    form.fixedScore = val.fixedScore || 0
    form.baseScore = val.baseScore || 0
    form.perPersonScore = val.perPersonScore || 0
    form.description = val.description || ''
    form.sortOrder = val.sortOrder || 0
    form.status = val.status ?? 1

    // 解析区间配置
    if (val.rangeConfig) {
      try {
        const ranges = JSON.parse(val.rangeConfig)
        if (ranges && ranges.length > 0) {
          form.rangeMin = ranges[0].min || 0
          form.rangeMax = ranges[0].max || 0
        }
      } catch (e) {}
    }
  }
}, { immediate: true })

// 监听category变化，设置typeId
watch(() => props.category, (val) => {
  if (val && props.isNew) {
    form.typeId = val.id
  }
}, { immediate: true })
</script>
