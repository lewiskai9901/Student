<template>
  <div class="p-4">
    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <Loader2 class="h-6 w-6 animate-spin text-blue-600" />
      <span class="ml-2 text-gray-500">加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!dormitoryInfo" class="py-12 text-center">
      <Building2 class="mx-auto h-12 w-12 text-gray-300" />
      <p class="mt-2 text-gray-400">宿舍信息不存在</p>
    </div>

    <!-- 详情内容 -->
    <div v-else class="space-y-6">
      <!-- 基本信息 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">宿舍基本信息</h3>
        </div>
        <div class="p-4">
          <div class="grid grid-cols-3 gap-x-8 gap-y-4">
            <div>
              <p class="text-sm text-gray-500">房间号</p>
              <p class="mt-1 text-sm font-medium text-gray-900">{{ dormitoryInfo.dormitoryNo }}</p>
            </div>
            <div>
              <p class="text-sm text-gray-500">宿舍楼号</p>
              <p class="mt-1 text-sm font-medium text-gray-900">{{ dormitoryInfo.buildingNo || '-' }}</p>
            </div>
            <div>
              <p class="text-sm text-gray-500">宿舍楼名称</p>
              <p class="mt-1 text-sm font-medium text-gray-900">{{ dormitoryInfo.buildingName || '-' }}</p>
            </div>
            <div>
              <p class="text-sm text-gray-500">楼层</p>
              <p class="mt-1 text-sm font-medium text-gray-900">{{ dormitoryInfo.floor || dormitoryInfo.floorNumber }}楼</p>
            </div>
            <div>
              <p class="text-sm text-gray-500">宿舍容量</p>
              <p class="mt-1 text-sm font-medium text-gray-900">{{ dormitoryInfo.roomTypeName || getRoomTypeName(dormitoryInfo.roomType) }}</p>
            </div>
            <div>
              <p class="text-sm text-gray-500">性别类型</p>
              <p class="mt-1 text-sm font-medium text-gray-900">{{ dormitoryInfo.genderTypeName || getGenderTypeName(dormitoryInfo.genderType) }}</p>
            </div>
            <div>
              <p class="text-sm text-gray-500">当前入住</p>
              <p class="mt-1">
                <span :class="getOccupancyClass(dormitoryInfo.currentOccupancy, dormitoryInfo.maxOccupancy)">
                  {{ dormitoryInfo.currentOccupancy }}人
                </span>
              </p>
            </div>
            <div>
              <p class="text-sm text-gray-500">空余床位</p>
              <p class="mt-1 text-sm font-medium text-gray-900">{{ (dormitoryInfo.maxOccupancy || 0) - (dormitoryInfo.currentOccupancy || 0) }}个</p>
            </div>
            <div>
              <p class="text-sm text-gray-500">状态</p>
              <p class="mt-1">
                <span :class="getStatusClass(dormitoryInfo.status)">
                  {{ dormitoryInfo.statusName || getStatusText(dormitoryInfo.status) }}
                </span>
              </p>
            </div>
            <div>
              <p class="text-sm text-gray-500">宿管员</p>
              <p class="mt-1 text-sm font-medium text-gray-900">{{ dormitoryInfo.supervisorName || '未分配' }}</p>
            </div>
            <div>
              <p class="text-sm text-gray-500">宿舍类型</p>
              <p class="mt-1 text-sm font-medium text-gray-900">{{ dormitoryInfo.dormitoryTypeName || '学生宿舍' }}</p>
            </div>
            <div v-if="dormitoryInfo.notes" class="col-span-3">
              <p class="text-sm text-gray-500">备注</p>
              <p class="mt-1 text-sm text-gray-900">{{ dormitoryInfo.notes }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 入住学生信息 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">入住学生信息</h3>
          <span class="text-sm text-gray-500">共 {{ dormitoryInfo.students?.length || 0 }} 人</span>
        </div>
        <div class="p-4">
          <div v-if="dormitoryInfo.students && dormitoryInfo.students.length > 0" class="grid grid-cols-2 gap-4">
            <div
              v-for="student in dormitoryInfo.students"
              :key="student.id"
              class="flex items-center gap-3 rounded-lg border border-gray-200 p-3 hover:bg-gray-50"
            >
              <div class="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-blue-100 text-sm font-medium text-blue-600">
                {{ student.realName?.charAt(0) || '?' }}
              </div>
              <div class="min-w-0 flex-1">
                <p class="text-sm font-medium text-gray-900">{{ student.realName }}</p>
                <p class="text-xs text-gray-500">学号: {{ student.studentNo }}</p>
                <p v-if="student.className" class="text-xs text-gray-500">班级: {{ student.className }}</p>
                <p v-if="student.bedNumber" class="text-xs text-blue-600">床位: {{ student.bedNumber }}号</p>
              </div>
            </div>
          </div>
          <div v-else class="py-8 text-center">
            <Users class="mx-auto h-10 w-10 text-gray-300" />
            <p class="mt-2 text-sm text-gray-400">暂无学生入住</p>
          </div>
        </div>
      </div>

      <!-- 床位分配情况 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">床位分配情况</h3>
        </div>
        <div class="p-4">
          <div class="grid grid-cols-4 gap-4">
            <div
              v-for="bedNo in dormitoryInfo.maxOccupancy"
              :key="bedNo"
              class="rounded-lg border-2 p-4 text-center transition-colors"
              :class="isBedOccupied(bedNo) ? 'border-green-300 bg-green-50' : 'border-dashed border-gray-300 bg-gray-50'"
            >
              <p class="text-sm font-medium" :class="isBedOccupied(bedNo) ? 'text-green-700' : 'text-gray-600'">
                {{ bedNo }}号床
              </p>
              <p class="mt-1 text-sm" :class="isBedOccupied(bedNo) ? 'text-green-600' : 'text-gray-400'">
                {{ getStudentByBed(bedNo)?.realName || '空床位' }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="mt-6 flex justify-end border-t border-gray-200 pt-4">
      <button
        @click="handleClose"
        class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
      >
        关闭
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loader2, Building2, Users } from 'lucide-vue-next'
import { getDormitoryDetail } from '@/api/dormitory'
import type { Dormitory, StudentSimpleInfo } from '@/types/dormitory'

interface Props {
  dormitoryId: number | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  close: []
}>()

const loading = ref(false)
const dormitoryInfo = ref<Dormitory | null>(null)

// 获取房间类型名称
const getRoomTypeName = (roomType: number | undefined) => {
  if (!roomType) return '未知'
  const typeMap: Record<number, string> = {
    1: '四人间',
    2: '六人间',
    3: '八人间'
  }
  return typeMap[roomType] || '未知'
}

// 获取性别类型名称
const getGenderTypeName = (genderType: number | undefined) => {
  if (!genderType) return '未知'
  const typeMap: Record<number, string> = {
    1: '男生宿舍',
    2: '女生宿舍',
    3: '混合宿舍'
  }
  return typeMap[genderType] || '未知'
}

// 获取状态样式
const getStatusClass = (status: number) => {
  const classes: Record<number, string> = {
    1: 'inline-flex rounded bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700',
    2: 'inline-flex rounded bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700',
    3: 'inline-flex rounded bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700'
  }
  return classes[status] || 'inline-flex rounded bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-700'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    1: '正常',
    2: '维修',
    3: '停用'
  }
  return statusMap[status] || '未知'
}

// 获取入住率样式
const getOccupancyClass = (current: number | undefined, max: number | undefined) => {
  if (!current || !max) return 'inline-flex rounded bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-700'
  const rate = current / max
  if (rate >= 1) return 'inline-flex rounded bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700'
  if (rate >= 0.8) return 'inline-flex rounded bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700'
  return 'inline-flex rounded bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700'
}

// 判断床位是否被占用
const isBedOccupied = (bedNo: number) => {
  return dormitoryInfo.value?.students?.some(s =>
    s.bedNumber && parseInt(s.bedNumber) === bedNo
  ) || false
}

// 根据床位号获取学生信息
const getStudentByBed = (bedNo: number): StudentSimpleInfo | undefined => {
  return dormitoryInfo.value?.students?.find(s =>
    s.bedNumber && parseInt(s.bedNumber) === bedNo
  )
}

// 加载宿舍详情
const loadDormitoryDetail = async () => {
  if (!props.dormitoryId) {
    ElMessage.warning('宿舍ID不能为空')
    return
  }

  loading.value = true
  try {
    const data = await getDormitoryDetail(props.dormitoryId)
    dormitoryInfo.value = data
  } catch (error: any) {
    ElMessage.error(error.message || '加载宿舍详情失败')
  } finally {
    loading.value = false
  }
}

// 关闭
const handleClose = () => {
  emit('close')
}

onMounted(() => {
  loadDormitoryDetail()
})
</script>
