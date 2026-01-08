<template>
  <div class="flex h-full flex-col rounded-lg border border-gray-200 bg-white overflow-hidden">
    <!-- 卡片头部 -->
    <div class="flex-shrink-0 flex items-center justify-between bg-gray-50 px-4 py-3 border-b border-gray-200">
      <div class="flex items-center gap-3">
        <span class="text-base font-semibold text-gray-900">{{ fullRoomNo }}</span>
        <span
          :class="[
            'rounded px-2 py-0.5 text-xs font-medium',
            getRoomTypeTag === 'primary' ? 'bg-blue-100 text-blue-700' :
            getRoomTypeTag === 'danger' ? 'bg-pink-100 text-pink-700' :
            'bg-gray-100 text-gray-700'
          ]"
        >
          {{ getRoomTypeText }}
        </span>
      </div>
      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-500">{{ room.currentOccupancy }}/{{ room.maxOccupancy }}</span>
        <div class="h-2 w-16 overflow-hidden rounded-full bg-gray-200">
          <div
            class="h-full rounded-full transition-all"
            :class="progressColor"
            :style="{ width: `${occupancyPercentage}%` }"
          ></div>
        </div>
      </div>
    </div>

    <!-- 床位网格 -->
    <div class="flex-1 grid grid-cols-2 gap-3 p-4 content-start">
      <div
        v-for="bed in beds"
        :key="bed.number"
        class="rounded-lg border-2 p-3 cursor-pointer transition-colors"
        :class="bed.student ? 'border-blue-200 bg-blue-50' : 'border-dashed border-gray-200 bg-gray-50 hover:border-blue-300'"
        @click="handleBedClick(bed)"
      >
        <!-- 有学生 -->
        <div v-if="bed.student" class="text-center">
          <div class="flex items-center justify-between mb-2">
            <div class="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100 text-xs font-medium text-blue-600">
              {{ bed.student.realName?.charAt(0) || '?' }}
            </div>
            <div class="relative">
              <button
                @click.stop="toggleDropdown(bed.number)"
                class="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
              >
                <MoreVertical class="h-4 w-4" />
              </button>
              <div
                v-if="activeDropdown === bed.number"
                class="absolute right-0 top-full z-10 mt-1 w-28 rounded-lg border border-gray-200 bg-white py-1 shadow-lg"
              >
                <button
                  @click.stop="handleStudentAction('swap', bed.student)"
                  class="flex w-full items-center gap-2 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
                >
                  <ArrowLeftRight class="h-4 w-4" />
                  交换宿舍
                </button>
                <button
                  @click.stop="handleStudentAction('remove', bed.student)"
                  class="flex w-full items-center gap-2 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50"
                >
                  <UserMinus class="h-4 w-4" />
                  移出宿舍
                </button>
              </div>
            </div>
          </div>
          <p class="text-sm font-medium text-gray-900 truncate">{{ bed.student.realName }}</p>
          <p class="text-xs text-gray-500">{{ bed.student.studentNo }}</p>
          <p v-if="bed.student.className" class="mt-1">
            <span class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500">{{ bed.student.className }}</span>
          </p>
          <p class="mt-1 text-xs text-blue-600">{{ bed.number }}号床</p>
        </div>

        <!-- 空床位 -->
        <div v-else class="flex flex-col items-center py-3 text-gray-400">
          <Plus class="h-6 w-6" />
          <span class="mt-1 text-xs">{{ bed.number }}号床</span>
          <span class="text-xs">点击添加</span>
        </div>
      </div>
    </div>

    <!-- 卡片底部 -->
    <div class="flex-shrink-0 mt-auto flex items-center justify-between border-t border-gray-200 bg-gray-50 px-4 py-2">
      <span class="flex items-center gap-1 text-xs text-gray-500">
        <MapPin class="h-3.5 w-3.5" />
        {{ room.buildingName }} {{ room.floor }}层
      </span>
      <button
        v-if="canAddStudent"
        @click="$emit('add-student', room)"
        class="inline-flex h-7 items-center gap-1 rounded bg-blue-600 px-2 text-xs font-medium text-white hover:bg-blue-700"
      >
        <Plus class="h-3.5 w-3.5" />
        添加学生
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, MapPin, MoreVertical, ArrowLeftRight, UserMinus } from 'lucide-vue-next'

interface Props {
  room: any
}

const props = defineProps<Props>()

const emit = defineEmits(['add-student', 'remove-student', 'swap-student', 'refresh'])

const activeDropdown = ref<number | null>(null)

// 完整房间号（楼号-房间号）
const fullRoomNo = computed(() => {
  const room = props.room
  // 如果有 buildingNo，使用 buildingNo-roomNo 格式
  if (room.buildingNo) {
    return `${room.buildingNo}-${room.roomNo || room.dormitoryNo}`
  }
  // 如果有 buildingName，尝试提取楼号
  if (room.buildingName) {
    // 尝试从 buildingName 提取数字（如 "27#宿舍楼" -> "27"）
    const match = room.buildingName.match(/^(\d+)/)
    if (match) {
      return `${match[1]}-${room.roomNo || room.dormitoryNo}`
    }
    // 如果是其他格式，如 "A栋" -> "A"
    const letterMatch = room.buildingName.match(/^([A-Za-z])/)
    if (letterMatch) {
      return `${letterMatch[1]}-${room.roomNo || room.dormitoryNo}`
    }
  }
  // 默认只返回房间号
  return room.roomNo || room.dormitoryNo
})

// 入住百分比
const occupancyPercentage = computed(() => {
  return Math.round((props.room.currentOccupancy / props.room.maxOccupancy) * 100)
})

// 进度条颜色
const progressColor = computed(() => {
  const percentage = occupancyPercentage.value
  if (percentage === 0) return 'bg-red-500'
  if (percentage < 50) return 'bg-amber-500'
  if (percentage < 100) return 'bg-blue-500'
  return 'bg-green-500'
})

// 房间类型标签
const getRoomTypeTag = computed(() => {
  if (props.room.genderType === 1) return 'primary'
  if (props.room.genderType === 2) return 'danger'
  return 'info'
})

// 房间类型文本
const getRoomTypeText = computed(() => {
  const maxOccupancy = props.room.maxOccupancy || props.room.bedCount
  let roomType = maxOccupancy === 4 ? '四人间' :
                 maxOccupancy === 6 ? '六人间' :
                 maxOccupancy === 8 ? '八人间' : `${maxOccupancy}人间`

  let genderType = props.room.genderType === 1 ? '男生' :
                   props.room.genderType === 2 ? '女生' : '混合'

  return `${roomType} · ${genderType}`
})

// 是否可以添加学生
const canAddStudent = computed(() => {
  return props.room.currentOccupancy < props.room.maxOccupancy
})

// 解析床位号，从 "床位1" 或 "1" 格式提取数字
const parseBedNumber = (bedNumber: any): string | null => {
  if (!bedNumber) return null
  const str = String(bedNumber)
  // 尝试匹配 "床位X" 格式
  const match = str.match(/床位(\d+)/)
  if (match) return match[1]
  // 如果是纯数字，直接返回
  if (/^\d+$/.test(str)) return str
  return null
}

// 床位数据
const beds = computed(() => {
  const bedsArray = []
  const studentsMap = new Map()
  const unassignedStudents: any[] = [] // 没有床位号的学生

  if (props.room.students && props.room.students.length > 0) {
    props.room.students.forEach((student: any) => {
      const bedNum = parseBedNumber(student.bedNumber)
      if (bedNum) {
        studentsMap.set(bedNum, student)
      } else {
        unassignedStudents.push(student)
      }
    })
  }

  // 先填充有床位号的学生
  for (let i = 1; i <= props.room.maxOccupancy; i++) {
    const assignedStudent = studentsMap.get(String(i))
    if (assignedStudent) {
      bedsArray.push({
        number: i,
        student: assignedStudent
      })
    } else if (unassignedStudents.length > 0) {
      // 没有床位号的学生，分配到空床位
      const student = unassignedStudents.shift()
      bedsArray.push({
        number: i,
        student: student
      })
    } else {
      bedsArray.push({
        number: i,
        student: null
      })
    }
  }

  return bedsArray
})

// 切换下拉菜单
const toggleDropdown = (bedNumber: number) => {
  activeDropdown.value = activeDropdown.value === bedNumber ? null : bedNumber
}

// 床位点击
const handleBedClick = (bed: any) => {
  if (bed.student) {
    return
  }

  if (canAddStudent.value) {
    emit('add-student', props.room)
  }
}

// 学生操作
const handleStudentAction = (command: string, student: any) => {
  activeDropdown.value = null
  if (command === 'remove') {
    emit('remove-student', student, props.room)
  } else if (command === 'swap') {
    emit('swap-student', student)
  }
}

// 点击外部关闭下拉菜单
document.addEventListener('click', () => {
  activeDropdown.value = null
})
</script>
