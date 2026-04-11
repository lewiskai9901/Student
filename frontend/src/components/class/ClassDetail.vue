<template>
  <div class="p-6">
    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
    </div>

    <div v-else-if="classInfo" class="space-y-6">
      <!-- 基本信息 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">班级基本信息</h3>
        </div>
        <div class="p-4">
          <div class="grid grid-cols-3 gap-x-8 gap-y-4">
            <div><span class="text-sm text-gray-500">班级名称:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.className }}</span></div>
            <div><span class="text-sm text-gray-500">班级编码:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.classCode }}</span></div>
            <div><span class="text-sm text-gray-500">年级:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.grade }}</span></div>
            <div><span class="text-sm text-gray-500">所属部门:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.orgUnitName }}</span></div>
            <div><span class="text-sm text-gray-500">专业:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.majorName }}</span></div>
            <div><span class="text-sm text-gray-500">班主任:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.teacherName || '未分配' }}</span></div>
            <div><span class="text-sm text-gray-500">学生人数:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.studentCount }}人</span></div>
            <div>
              <span class="text-sm text-gray-500">状态:</span>
              <span :class="['ml-2 inline-flex rounded px-2 py-0.5 text-xs font-medium', classInfo.status === 1 ? 'bg-green-50 text-green-700' : classInfo.status === 2 ? 'bg-amber-50 text-amber-700' : 'bg-gray-100 text-gray-700']">
                {{ getStatusText(classInfo.status) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 教室信息 - 待重构：后端尚无 /classes/{id}/classroom 端点 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">教室信息</h3>
        </div>
        <div class="p-4">
          <div class="flex flex-col items-center justify-center py-8 text-gray-400">
            <Building2 class="mb-2 h-10 w-10" />
            <span class="text-sm">教室分配功能待重构</span>
          </div>
        </div>
      </div>

      <!-- 宿舍信息 - 待重构：后端尚无 /classes/{id}/dormitories 端点 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">宿舍信息</h3>
        </div>
        <div class="p-4">
          <div class="flex flex-col items-center justify-center py-8 text-gray-400">
            <Home class="mb-2 h-10 w-10" />
            <span class="text-sm">宿舍分配功能待重构</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="flex flex-col items-center justify-center py-16 text-gray-400">
      <AlertCircle class="mb-2 h-12 w-12" />
      <span class="text-sm">班级信息不存在</span>
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
import { Building2, Home, AlertCircle } from 'lucide-vue-next'
import { getClass } from '@/api/organization'

interface Props { orgUnitId: number | null }
const props = defineProps<Props>()
const emit = defineEmits<{ close: [] }>()

const loading = ref(false)
const classInfo = ref<any>(null)

const getStatusText = (status: number) => ({ 1: '正常', 2: '停招', 3: '毕业' }[status] || '未知')

const loadClassDetail = async () => {
  if (!props.orgUnitId) return
  loading.value = true
  try {
    classInfo.value = await getClass(props.orgUnitId)
  } catch (error) {
    console.error('加载班级详情失败:', error)
    ElMessage.error('加载班级详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadClassDetail() })
</script>
