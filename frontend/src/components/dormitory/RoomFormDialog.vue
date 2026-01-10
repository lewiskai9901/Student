<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="handleClose"></div>
        <div class="relative w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
          <div class="mb-6 flex items-center justify-between">
            <h3 class="text-lg font-semibold text-gray-900">{{ title }}</h3>
            <button @click="handleClose" class="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
              <X class="h-5 w-5" />
            </button>
          </div>

          <form @submit.prevent="handleSubmit" class="space-y-4">
            <!-- 宿舍楼(只读) -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">宿舍楼</label>
              <input
                :value="buildingInfo"
                disabled
                class="h-9 w-full rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm text-gray-500"
              />
            </div>

            <!-- 宿舍楼类型(只读) -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">宿舍楼类型</label>
              <span :class="getDormitoryTypeClass(dormitoryType)" class="inline-flex rounded px-2 py-0.5 text-xs font-medium">
                {{ dormitoryTypeName }}
              </span>
            </div>

            <!-- 房间号 -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                房间号 <span class="text-red-500">*</span>
              </label>
              <input
                v-model="formData.dormitoryNo"
                type="text"
                required
                placeholder="例如: 101"
                maxlength="20"
                class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
              />
            </div>

            <!-- 楼层 -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                楼层 <span class="text-red-500">*</span>
              </label>
              <input
                v-model.number="formData.floorNumber"
                type="number"
                required
                min="1"
                max="30"
                class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
              />
            </div>

            <!-- 房间用途类型 -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                房间用途 <span class="text-red-500">*</span>
              </label>
              <select
                v-model="formData.roomUsageType"
                required
                @change="onRoomUsageTypeChange"
                class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
              >
                <option v-for="item in filteredRoomUsageOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
              <p class="mt-1 text-xs text-gray-500">根据宿舍楼类型已自动过滤可选的房间用途</p>
            </div>

            <!-- 床位容量 -->
            <div v-if="needsBedCapacity">
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                床位容量 <span class="text-red-500">*</span>
              </label>
              <select
                v-model="formData.bedCapacity"
                required
                @change="onBedCapacityChange"
                class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
              >
                <option v-for="item in bedCapacityOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </div>

            <!-- 自定义床位数 -->
            <div v-if="showCustomBedInput">
              <label class="mb-1.5 block text-sm font-medium text-gray-700">
                自定义床位数 <span class="text-red-500">*</span>
              </label>
              <input
                v-model.number="formData.customBedCount"
                type="number"
                required
                min="1"
                max="20"
                class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
              />
            </div>

            <!-- 性别类型(只读) -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">性别类型</label>
              <span :class="getGenderTypeClass(autoGenderType)" class="inline-flex rounded px-2 py-0.5 text-xs font-medium">
                {{ getGenderTypeName(autoGenderType) }} (根据宿舍楼自动确定)
              </span>
            </div>

            <!-- 状态 -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">状态</label>
              <div class="flex items-center gap-6">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="radio" v-model="formData.status" :value="1" class="h-4 w-4 text-blue-600" />
                  <span class="text-sm text-gray-700">正常</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="radio" v-model="formData.status" :value="2" class="h-4 w-4 text-blue-600" />
                  <span class="text-sm text-gray-700">维修</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="radio" v-model="formData.status" :value="3" class="h-4 w-4 text-blue-600" />
                  <span class="text-sm text-gray-700">停用</span>
                </label>
              </div>
            </div>

            <!-- 备注 -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">备注</label>
              <textarea
                v-model="formData.notes"
                rows="2"
                placeholder="请输入备注"
                maxlength="500"
                class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
              ></textarea>
            </div>

            <!-- 操作按钮 -->
            <div class="flex justify-end gap-3 border-t border-gray-200 pt-4">
              <button
                type="button"
                @click="handleClose"
                class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                type="submit"
                :disabled="submitLoading"
                class="inline-flex h-9 items-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
                确定
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { X, Loader2 } from 'lucide-vue-next'
// V2 DDD API
import { createDormitory } from '@/api/v2/dormitory'
// 房间类型选项暂保留从 V1 types 导入
import { roomUsageTypeOptions, bedCapacityOptions } from '@/types/dormitory'

interface Props {
  buildingId: number
  buildingName: string
  buildingNo?: string
  dormitoryType: number
  dormitoryTypeName: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  success: []
  close: []
}>()

const visible = defineModel<boolean>('visible', { required: true })

const submitLoading = ref(false)

const formData = reactive({
  dormitoryNo: '',
  floorNumber: 1,
  roomUsageType: 1,
  bedCapacity: 4,
  customBedCount: 4,
  status: 1,
  notes: ''
})

// 标题
const title = computed(() => `新增房间 - ${props.buildingName}`)

// 宿舍楼信息
const buildingInfo = computed(() => {
  return props.buildingNo ? `${props.buildingNo} - ${props.buildingName}` : props.buildingName
})

// 过滤房间用途选项
const filteredRoomUsageOptions = computed(() => {
  const type = props.dormitoryType
  if (type === 1 || type === 2) {
    return roomUsageTypeOptions.filter(opt => opt.value !== 2)
  } else if (type === 3 || type === 4 || type === 5) {
    return roomUsageTypeOptions.filter(opt => opt.value !== 1)
  }
  return roomUsageTypeOptions
})

// 是否需要床位容量
const needsBedCapacity = computed(() => {
  return formData.roomUsageType === 1 || formData.roomUsageType === 2
})

// 是否显示自定义床位数
const showCustomBedInput = computed(() => {
  return needsBedCapacity.value && formData.bedCapacity === 0
})

// 自动确定性别类型
const autoGenderType = computed(() => {
  const type = props.dormitoryType
  switch (type) {
    case 1:
    case 3:
      return 1
    case 2:
    case 4:
      return 2
    case 5:
      return 3
    default:
      return 1
  }
})

// 房间用途类型改变
const onRoomUsageTypeChange = () => {
  if (!needsBedCapacity.value) {
    formData.bedCapacity = 0
  } else {
    formData.bedCapacity = 4
  }
}

// 床位容量改变
const onBedCapacityChange = (value: number) => {
  if (value === 0) {
    formData.customBedCount = 4
  }
}

// 获取宿舍楼类型样式
const getDormitoryTypeClass = (type: number) => {
  const classes: Record<number, string> = {
    1: 'bg-blue-100 text-blue-700',
    2: 'bg-pink-100 text-pink-700',
    3: 'bg-green-100 text-green-700',
    4: 'bg-amber-100 text-amber-700',
    5: 'bg-gray-100 text-gray-700'
  }
  return classes[type] || 'bg-gray-100 text-gray-700'
}

// 获取性别类型样式
const getGenderTypeClass = (type: number) => {
  const classes: Record<number, string> = {
    1: 'bg-blue-100 text-blue-700',
    2: 'bg-pink-100 text-pink-700',
    3: 'bg-amber-100 text-amber-700'
  }
  return classes[type] || 'bg-gray-100 text-gray-700'
}

// 获取性别类型名称
const getGenderTypeName = (type: number) => {
  const names: Record<number, string> = {
    1: '男',
    2: '女',
    3: '混合'
  }
  return names[type] || '未知'
}

// 提交表单
const handleSubmit = async () => {
  if (!formData.dormitoryNo) {
    ElMessage.warning('请输入房间号')
    return
  }

  submitLoading.value = true
  try {
    const finalBedCapacity = formData.bedCapacity === 0 ? formData.customBedCount : formData.bedCapacity

    await createDormitory({
      buildingId: props.buildingId,
      dormitoryNo: formData.dormitoryNo,
      floorNumber: formData.floorNumber,
      roomUsageType: formData.roomUsageType,
      bedCapacity: finalBedCapacity,
      status: formData.status,
      notes: formData.notes
    })

    ElMessage.success('新增房间成功')
    emit('success')
    handleClose()
  } catch (error: any) {
    ElMessage.error(error.message || '新增房间失败')
  } finally {
    submitLoading.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  formData.dormitoryNo = ''
  formData.floorNumber = 1
  formData.roomUsageType = 1
  formData.bedCapacity = 4
  formData.customBedCount = 4
  formData.status = 1
  formData.notes = ''
  emit('close')
  visible.value = false
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
