<template>
  <div class="p-6">
    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
    </div>

    <div v-else-if="recordInfo" class="space-y-6">
      <!-- 基本信息 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">检查基本信息</h3>
        </div>
        <div class="p-4">
          <div class="grid grid-cols-3 gap-x-8 gap-y-4">
            <div><span class="text-sm text-gray-500">检查类型:</span><span class="ml-2 text-sm text-gray-900">{{ recordInfo.typeName }}</span></div>
            <div><span class="text-sm text-gray-500">检查对象:</span><span class="ml-2 text-sm text-gray-900">{{ recordInfo.targetName }}</span></div>
            <div><span class="text-sm text-gray-500">检查员:</span><span class="ml-2 text-sm text-gray-900">{{ recordInfo.checkerName }}</span></div>
            <div><span class="text-sm text-gray-500">检查时间:</span><span class="ml-2 text-sm text-gray-900">{{ formatDateTime(recordInfo.checkDate) }}</span></div>
            <div>
              <span class="text-sm text-gray-500">总分:</span>
              <span :class="['ml-2 inline-flex rounded px-2 py-0.5 text-sm font-semibold', getScoreClass(recordInfo.scoreRate)]">
                {{ recordInfo.totalScore }}分 / {{ recordInfo.maxScore }}分
              </span>
            </div>
            <div><span class="text-sm text-gray-500">得分率:</span><span class="ml-2 text-sm font-medium text-gray-900">{{ recordInfo.scoreRate }}%</span></div>
            <div>
              <span class="text-sm text-gray-500">状态:</span>
              <span :class="['ml-2 inline-flex rounded px-2 py-0.5 text-xs font-medium', getStatusClass(recordInfo.status)]">
                {{ getStatusText(recordInfo.status) }}
              </span>
            </div>
          </div>
          <div v-if="recordInfo.remark" class="mt-4 border-t border-gray-100 pt-4">
            <span class="text-sm text-gray-500">备注:</span>
            <span class="ml-2 text-sm text-gray-900">{{ recordInfo.remark }}</span>
          </div>
        </div>
      </div>

      <!-- 检查明细 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">检查明细</h3>
        </div>
        <div class="p-4">
          <div class="overflow-hidden rounded-lg border border-gray-200">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-4 py-2 text-left text-xs font-medium text-gray-500">检查项目</th>
                  <th class="px-4 py-2 text-center text-xs font-medium text-gray-500">满分</th>
                  <th class="px-4 py-2 text-center text-xs font-medium text-gray-500">得分</th>
                  <th class="px-4 py-2 text-center text-xs font-medium text-gray-500">得分率</th>
                  <th class="px-4 py-2 text-left text-xs font-medium text-gray-500">扣分原因</th>
                  <th class="px-4 py-2 text-left text-xs font-medium text-gray-500">备注</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200 bg-white">
                <tr v-for="(row, index) in checkDetails" :key="index" class="hover:bg-gray-50">
                  <td class="whitespace-nowrap px-4 py-2 text-sm font-medium text-gray-900">{{ row.itemName }}</td>
                  <td class="whitespace-nowrap px-4 py-2 text-center text-sm text-gray-600">{{ row.maxScore }}</td>
                  <td class="whitespace-nowrap px-4 py-2 text-center">
                    <span :class="['text-sm font-medium', row.itemScore < row.maxScore * 0.6 ? 'text-red-600' : 'text-gray-900']">{{ row.itemScore }}</span>
                  </td>
                  <td class="whitespace-nowrap px-4 py-2">
                    <div class="flex items-center justify-center gap-2">
                      <div class="h-1.5 w-16 overflow-hidden rounded-full bg-gray-200">
                        <div :class="['h-full rounded-full', getProgressColor((row.itemScore / row.maxScore) * 100)]" :style="{ width: `${(row.itemScore / row.maxScore) * 100}%` }"></div>
                      </div>
                      <span class="text-xs text-gray-500">{{ Math.round((row.itemScore / row.maxScore) * 100) }}%</span>
                    </div>
                  </td>
                  <td class="px-4 py-2 text-sm text-gray-600">{{ row.deductionReason || '-' }}</td>
                  <td class="px-4 py-2 text-sm text-gray-600">{{ row.remark || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 检查照片 -->
      <div v-if="recordInfo.photos && recordInfo.photos.length > 0" class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">检查照片</h3>
        </div>
        <div class="p-4">
          <div class="grid grid-cols-6 gap-4">
            <div v-for="(photo, index) in recordInfo.photos" :key="index" class="aspect-square overflow-hidden rounded-lg border border-gray-200">
              <img :src="photo" class="h-full w-full cursor-pointer object-cover transition-transform hover:scale-105" @click="previewImage(photo)" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="flex flex-col items-center justify-center py-16 text-gray-400">
      <FileQuestion class="mb-2 h-12 w-12" />
      <span class="text-sm">检查记录不存在</span>
    </div>

    <!-- 操作按钮 -->
    <div class="mt-6 flex justify-end border-t border-gray-200 pt-4">
      <button class="h-9 rounded-lg border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50" @click="$emit('close')">关闭</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { FileQuestion } from 'lucide-vue-next'
import { formatDateTime } from '@/utils/date'

interface Props { recordId: number | null }
const props = defineProps<Props>()
const emit = defineEmits<{ close: [] }>()

const loading = ref(false)
const recordInfo = ref<any>(null)
const checkDetails = ref<any[]>([])

const getScoreClass = (scoreRate: number) => {
  if (scoreRate >= 90) return 'bg-green-50 text-green-700'
  if (scoreRate >= 80) return 'bg-amber-50 text-amber-700'
  if (scoreRate >= 60) return 'bg-blue-50 text-blue-700'
  return 'bg-red-50 text-red-700'
}

const getStatusClass = (status: number) => ({ 1: 'bg-amber-50 text-amber-700', 2: 'bg-green-50 text-green-700', 3: 'bg-red-50 text-red-700' }[status] || 'bg-gray-100 text-gray-700')
const getStatusText = (status: number) => ({ 1: '待审核', 2: '已审核', 3: '已驳回' }[status] || '未知')
const getProgressColor = (percentage: number) => {
  if (percentage >= 90) return 'bg-green-500'
  if (percentage >= 60) return 'bg-amber-500'
  return 'bg-red-500'
}

const previewImage = (url: string) => { window.open(url, '_blank') }

const loadRecordDetail = async () => {
  if (!props.recordId) return
  loading.value = true
  try {
    // 模拟数据
    recordInfo.value = {
      id: props.recordId, typeName: '宿舍卫生检查', targetName: '1号楼201', checkerName: '张老师',
      checkDate: '2024-01-15T10:30:00', totalScore: 85, maxScore: 100, scoreRate: 85, status: 2,
      remark: '整体较好，需注意地面清洁', photos: ['/uploads/check1.jpg', '/uploads/check2.jpg']
    }
    checkDetails.value = [
      { itemName: '地面清洁', maxScore: 20, itemScore: 15, deductionReason: '有少量垃圾', remark: '需要及时清理' },
      { itemName: '床铺整理', maxScore: 20, itemScore: 20, deductionReason: '', remark: '整理规范' },
      { itemName: '物品摆放', maxScore: 20, itemScore: 18, deductionReason: '部分物品摆放不整齐', remark: '需要改进' },
      { itemName: '垃圾处理', maxScore: 15, itemScore: 15, deductionReason: '', remark: '处理及时' },
      { itemName: '空气质量', maxScore: 15, itemScore: 12, deductionReason: '通风不良', remark: '建议开窗通风' },
      { itemName: '安全检查', maxScore: 10, itemScore: 5, deductionReason: '有安全隐患', remark: '需要立即整改' }
    ]
  } catch (error) { ElMessage.error('加载检查记录失败') } finally { loading.value = false }
}

onMounted(() => { loadRecordDetail() })
</script>
