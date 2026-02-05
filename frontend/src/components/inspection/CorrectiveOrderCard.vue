<!--
  整改工单卡片组件
  显示单个整改工单的详细信息和操作按钮
-->
<template>
  <div
    class="corrective-order-card bg-white rounded-xl border shadow-sm hover:shadow-md transition-shadow"
    :class="borderColorClass"
  >
    <!-- 卡片头部 -->
    <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-lg flex items-center justify-center" :class="statusBgClass">
          <component :is="statusIcon" class="h-5 w-5" :class="statusIconClass" />
        </div>
        <div>
          <div class="font-medium text-gray-900">{{ order.orderCode }}</div>
          <div class="text-xs text-gray-400">{{ order.createdAt }}</div>
        </div>
      </div>

      <span class="px-3 py-1 rounded-full text-xs font-medium" :class="statusBadgeClass">
        {{ statusLabel }}
      </span>
    </div>

    <!-- 卡片内容 -->
    <div class="p-4">
      <!-- 关联信息 -->
      <div class="mb-4 space-y-2">
        <div class="flex items-center gap-2 text-sm">
          <Users class="h-4 w-4 text-gray-400" />
          <span class="text-gray-500">责任班级：</span>
          <span class="text-gray-900 font-medium">{{ order.className }}</span>
        </div>

        <div class="flex items-center gap-2 text-sm">
          <MapPin class="h-4 w-4 text-gray-400" />
          <span class="text-gray-500">检查地点：</span>
          <span class="text-gray-900">{{ order.spaceName || '-' }}</span>
        </div>

        <div class="flex items-center gap-2 text-sm">
          <AlertCircle class="h-4 w-4 text-gray-400" />
          <span class="text-gray-500">问题描述：</span>
          <span class="text-gray-900">{{ order.problemDescription }}</span>
        </div>
      </div>

      <!-- 扣分项 -->
      <div class="mb-4 p-3 bg-gray-50 rounded-lg">
        <div class="flex items-center justify-between">
          <div>
            <span class="text-sm text-gray-500">扣分项目：</span>
            <span class="text-gray-900">{{ order.deductionItemName }}</span>
          </div>
          <span class="text-lg font-bold text-red-600">-{{ order.deductionScore }}分</span>
        </div>
      </div>

      <!-- 整改要求 -->
      <div class="mb-4">
        <div class="text-sm text-gray-500 mb-1">整改要求：</div>
        <div class="text-sm text-gray-700 bg-amber-50 border border-amber-100 rounded-lg p-3">
          {{ order.correctiveRequirement || '请按规范要求进行整改' }}
        </div>
      </div>

      <!-- 截止时间 -->
      <div class="flex items-center gap-2 text-sm mb-4">
        <Clock class="h-4 w-4" :class="isOverdue ? 'text-red-500' : 'text-gray-400'" />
        <span class="text-gray-500">整改期限：</span>
        <span :class="isOverdue ? 'text-red-600 font-medium' : 'text-gray-900'">
          {{ order.deadline }}
          <span v-if="isOverdue" class="ml-1">(已逾期)</span>
        </span>
      </div>

      <!-- 整改记录 -->
      <div v-if="order.records && order.records.length > 0" class="mb-4">
        <div class="text-sm text-gray-500 mb-2">整改记录：</div>
        <div class="space-y-2">
          <div
            v-for="(record, idx) in order.records"
            :key="idx"
            class="p-3 border rounded-lg"
            :class="record.status === 'VERIFIED' ? 'border-green-200 bg-green-50' : 'border-gray-200 bg-gray-50'"
          >
            <div class="flex items-center justify-between mb-1">
              <span class="text-xs text-gray-400">{{ record.submittedAt }}</span>
              <span
                class="px-2 py-0.5 rounded-full text-xs"
                :class="record.status === 'VERIFIED' ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'"
              >
                {{ record.status === 'VERIFIED' ? '已验收' : '待验收' }}
              </span>
            </div>
            <div class="text-sm text-gray-700">{{ record.description }}</div>
            <div v-if="record.photos && record.photos.length > 0" class="mt-2 flex gap-2">
              <div
                v-for="(photo, pidx) in record.photos.slice(0, 3)"
                :key="pidx"
                class="w-12 h-12 rounded overflow-hidden cursor-pointer"
                @click="$emit('preview-image', photo)"
              >
                <img :src="photo" class="w-full h-full object-cover" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 图片证据 -->
      <div v-if="order.photos && order.photos.length > 0" class="mb-4">
        <div class="text-sm text-gray-500 mb-2">问题照片：</div>
        <div class="flex gap-2 flex-wrap">
          <div
            v-for="(photo, idx) in order.photos"
            :key="idx"
            class="w-16 h-16 rounded-lg overflow-hidden cursor-pointer hover:opacity-80"
            @click="$emit('preview-image', photo)"
          >
            <img :src="photo" class="w-full h-full object-cover" />
          </div>
        </div>
      </div>
    </div>

    <!-- 卡片底部操作 -->
    <div class="px-4 py-3 border-t border-gray-100 flex items-center justify-between bg-gray-50/50">
      <div class="flex items-center gap-2 text-xs text-gray-400">
        <User class="h-3.5 w-3.5" />
        <span>检查员：{{ order.inspectorName }}</span>
      </div>

      <div class="flex items-center gap-2">
        <!-- 提交整改 -->
        <el-button
          v-if="canSubmitCorrective"
          type="primary"
          size="small"
          @click="$emit('submit-corrective', order)"
        >
          <Upload class="h-4 w-4 mr-1" />
          提交整改
        </el-button>

        <!-- 验收整改 -->
        <el-button
          v-if="canVerify"
          type="success"
          size="small"
          @click="$emit('verify', order)"
        >
          <CheckCircle class="h-4 w-4 mr-1" />
          验收通过
        </el-button>

        <!-- 驳回 -->
        <el-button
          v-if="canVerify"
          type="danger"
          size="small"
          plain
          @click="$emit('reject', order)"
        >
          <XCircle class="h-4 w-4 mr-1" />
          验收不通过
        </el-button>

        <!-- 关闭 -->
        <el-button
          v-if="canClose"
          size="small"
          @click="$emit('close', order)"
        >
          <Archive class="h-4 w-4 mr-1" />
          关闭工单
        </el-button>

        <!-- 查看详情 -->
        <el-button
          size="small"
          link
          @click="$emit('view-detail', order)"
        >
          查看详情
          <ChevronRight class="h-4 w-4" />
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Clock,
  Users,
  MapPin,
  AlertCircle,
  User,
  Upload,
  CheckCircle,
  XCircle,
  Archive,
  ChevronRight,
  FileWarning,
  CheckCheck,
  AlertTriangle,
  Ban
} from 'lucide-vue-next'

// 类型定义
interface CorrectiveRecord {
  id: number
  description: string
  submittedAt: string
  photos: string[]
  status: 'PENDING_VERIFY' | 'VERIFIED' | 'REJECTED'
}

interface CorrectiveOrder {
  id: number
  orderCode: string
  className: string
  spaceName?: string
  problemDescription: string
  deductionItemName: string
  deductionScore: number
  correctiveRequirement?: string
  deadline: string
  status: 'PENDING' | 'SUBMITTED' | 'VERIFIED' | 'REJECTED' | 'CLOSED' | 'OVERDUE'
  photos?: string[]
  records?: CorrectiveRecord[]
  inspectorName: string
  createdAt: string
}

// Props
const props = defineProps<{
  order: CorrectiveOrder
  userRole?: 'inspector' | 'class_teacher' | 'admin'
}>()

// Emits
defineEmits<{
  'submit-corrective': [order: CorrectiveOrder]
  'verify': [order: CorrectiveOrder]
  'reject': [order: CorrectiveOrder]
  'close': [order: CorrectiveOrder]
  'view-detail': [order: CorrectiveOrder]
  'preview-image': [url: string]
}>()

// 计算属性
const isOverdue = computed(() => {
  if (!props.order.deadline) return false
  return new Date(props.order.deadline) < new Date()
})

const statusLabel = computed(() => {
  const labels: Record<string, string> = {
    PENDING: '待整改',
    SUBMITTED: '待验收',
    VERIFIED: '已完成',
    REJECTED: '验收不通过',
    CLOSED: '已关闭',
    OVERDUE: '已逾期'
  }
  return labels[props.order.status] || props.order.status
})

const statusIcon = computed(() => {
  const icons: Record<string, any> = {
    PENDING: AlertTriangle,
    SUBMITTED: Clock,
    VERIFIED: CheckCheck,
    REJECTED: XCircle,
    CLOSED: Archive,
    OVERDUE: FileWarning
  }
  return icons[props.order.status] || AlertCircle
})

const statusBgClass = computed(() => {
  const classes: Record<string, string> = {
    PENDING: 'bg-amber-100',
    SUBMITTED: 'bg-blue-100',
    VERIFIED: 'bg-green-100',
    REJECTED: 'bg-red-100',
    CLOSED: 'bg-gray-100',
    OVERDUE: 'bg-red-100'
  }
  return classes[props.order.status] || 'bg-gray-100'
})

const statusIconClass = computed(() => {
  const classes: Record<string, string> = {
    PENDING: 'text-amber-600',
    SUBMITTED: 'text-blue-600',
    VERIFIED: 'text-green-600',
    REJECTED: 'text-red-600',
    CLOSED: 'text-gray-600',
    OVERDUE: 'text-red-600'
  }
  return classes[props.order.status] || 'text-gray-600'
})

const statusBadgeClass = computed(() => {
  const classes: Record<string, string> = {
    PENDING: 'bg-amber-50 text-amber-700',
    SUBMITTED: 'bg-blue-50 text-blue-700',
    VERIFIED: 'bg-green-50 text-green-700',
    REJECTED: 'bg-red-50 text-red-700',
    CLOSED: 'bg-gray-100 text-gray-600',
    OVERDUE: 'bg-red-50 text-red-700'
  }
  return classes[props.order.status] || 'bg-gray-100 text-gray-600'
})

const borderColorClass = computed(() => {
  const classes: Record<string, string> = {
    PENDING: 'border-amber-200',
    SUBMITTED: 'border-blue-200',
    VERIFIED: 'border-green-200',
    REJECTED: 'border-red-200',
    CLOSED: 'border-gray-200',
    OVERDUE: 'border-red-300'
  }
  return classes[props.order.status] || 'border-gray-200'
})

// 权限判断
const canSubmitCorrective = computed(() => {
  // 班主任可以在待整改或验收不通过时提交整改
  return props.userRole === 'class_teacher' &&
    (props.order.status === 'PENDING' || props.order.status === 'REJECTED')
})

const canVerify = computed(() => {
  // 检查员可以验收待验收的工单
  return props.userRole === 'inspector' && props.order.status === 'SUBMITTED'
})

const canClose = computed(() => {
  // 管理员可以关闭已完成的工单
  return props.userRole === 'admin' && props.order.status === 'VERIFIED'
})
</script>
