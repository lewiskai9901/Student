<template>
  <el-dialog
    v-model="dialogVisible"
    title="分配学生床位"
    width="650"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <!-- 宿舍信息 -->
    <div class="dormitory-info">
      <div class="info-left">
        <h3 class="dormitory-name">
          {{ dormitory?.buildingName }} - {{ dormitory?.dormitoryNo }}
        </h3>
        <p class="dormitory-meta">
          {{ dormitory?.floorNumber }}层 ·
          {{ getGenderLabel(dormitory?.genderType) }}
        </p>
      </div>
      <div class="info-right">
        <div class="occupancy" :class="getOccupancyClass()">
          {{ currentOccupants.length }}/{{ dormitory?.bedCapacity || 0 }}
        </div>
        <div class="occupancy-label">已入住/总床位</div>
      </div>
    </div>

    <!-- 床位网格 -->
    <div class="bed-section">
      <div class="section-header">
        <span class="section-title">床位分布</span>
        <div class="legend">
          <span class="legend-item">
            <span class="dot empty"></span>空闲
          </span>
          <span class="legend-item">
            <span class="dot occupied"></span>已入住
          </span>
          <span class="legend-item">
            <span class="dot selected"></span>选中
          </span>
        </div>
      </div>
      <div class="bed-grid">
        <div
          v-for="bed in bedList"
          :key="bed.number"
          class="bed-item"
          :class="getBedClass(bed)"
          @click="selectBed(bed)"
        >
          <div class="bed-number">{{ bed.number }}号床</div>
          <div v-if="bed.student" class="bed-student">
            {{ bed.student.occupantName }}
          </div>
          <div v-else class="bed-empty">空闲</div>
        </div>
      </div>
    </div>

    <!-- 空床位选中后显示学生选择 -->
    <div v-if="selectedBed && !selectedBed.student" class="student-selection">
      <div class="selection-header">
        <el-icon><User /></el-icon>
        <span>已选择: {{ selectedBed.number }}号床位，请选择学生</span>
      </div>

      <!-- 搜索框 -->
      <div class="search-box">
        <el-input
          v-model="studentSearchQuery"
          placeholder="搜索学生姓名或学号..."
          clearable
          :prefix-icon="Search"
        />
      </div>

      <!-- 学生列表 -->
      <div class="student-list">
        <div v-if="studentsLoading" class="loading-state">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        <div v-else-if="availableStudents.length === 0" class="empty-state">
          暂无可分配的学生（未分配宿舍的学生）
        </div>
        <div v-else class="student-items">
          <div
            v-for="student in availableStudents"
            :key="student.id"
            class="student-item"
            :class="{ selected: selectedStudent?.id === student.id }"
            @click="selectStudent(student)"
          >
            <div class="student-avatar" :class="student.gender === '男' ? 'male' : 'female'">
              {{ student.gender }}
            </div>
            <div class="student-info">
              <div class="student-name">{{ student.name }}</div>
              <div class="student-no">{{ student.studentNo }}</div>
            </div>
            <el-icon v-if="selectedStudent?.id === student.id" class="check-icon">
              <Check />
            </el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 已入住床位选中后显示学生信息 -->
    <div v-else-if="selectedBed && selectedBed.student" class="occupied-bed-info">
      <div class="student-detail">
        <div class="student-avatar large" :class="selectedBed.student.gender === 1 ? 'male' : 'female'">
          {{ selectedBed.student.gender === 1 ? '男' : '女' }}
        </div>
        <div class="student-info">
          <div class="student-name">{{ selectedBed.student.occupantName }}</div>
          <div class="student-no">{{ selectedBed.student.username }}</div>
        </div>
        <el-button type="danger" size="small" plain @click="handleCheckOut">
          办理退宿
        </el-button>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="handleSubmit"
        >
          确认分配
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Search, Check, Loading } from '@element-plus/icons-vue'
import { getClassStudents } from '@/api/myClass'
import { universalPlaceApi } from '@/api/universalPlace'
import type { PlaceOccupant } from '@/types/universalPlace'
import type { MyClassStudent } from '@/types/myClass'

// Props & Emits
const props = defineProps<{
  visible: boolean
  dormitory: {
    id: number
    dormitoryNo: string
    buildingId?: number
    buildingName?: string
    floorNumber?: number
    genderType?: number
    bedCapacity?: number
    occupiedBeds?: number
  } | null
  classId?: string | number
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

// Types
interface BedInfo {
  number: string
  student: PlaceOccupant | null
}

// State
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const currentOccupants = ref<PlaceOccupant[]>([])
const bedList = ref<BedInfo[]>([])
const selectedBed = ref<BedInfo | null>(null)
const selectedStudent = ref<MyClassStudent | null>(null)
const studentSearchQuery = ref('')
const classStudents = ref<MyClassStudent[]>([])
const studentsLoading = ref(false)
const submitting = ref(false)

// Computed - 过滤出可分配的学生（未分配宿舍的）
const availableStudents = computed(() => {
  let result = classStudents.value.filter(s => !s.dormitoryName)

  if (studentSearchQuery.value) {
    const query = studentSearchQuery.value.toLowerCase()
    result = result.filter(s =>
      s.name?.toLowerCase().includes(query) ||
      s.studentNo?.toLowerCase().includes(query)
    )
  }

  // 按性别过滤（如果宿舍有性别限制）
  if (props.dormitory?.genderType === 1) {
    result = result.filter(s => s.gender === '男')
  } else if (props.dormitory?.genderType === 2) {
    result = result.filter(s => s.gender === '女')
  }

  return result.slice(0, 50)
})

const canSubmit = computed(() => {
  return selectedBed.value && !selectedBed.value.student && selectedStudent.value
})

// Methods
const loadDormitoryData = async () => {
  if (!props.dormitory?.id) return

  try {
    currentOccupants.value = await universalPlaceApi.getOccupants(props.dormitory.id)

    const capacity = props.dormitory.bedCapacity || 6
    const beds: BedInfo[] = []
    for (let i = 1; i <= capacity; i++) {
      const bedNumber = String(i)
      const occupant = currentOccupants.value.find(o => o.positionNo === bedNumber)
      beds.push({
        number: bedNumber,
        student: occupant || null
      })
    }
    bedList.value = beds
  } catch (error: any) {
    ElMessage.error(error.message || '加载宿舍信息失败')
    bedList.value = []
  }
}

const loadClassStudents = async () => {
  if (!props.classId) {
    classStudents.value = []
    return
  }

  studentsLoading.value = true
  try {
    classStudents.value = await getClassStudents(props.classId)
  } catch (error: any) {
    ElMessage.error(error.message || '加载学生列表失败')
    classStudents.value = []
  } finally {
    studentsLoading.value = false
  }
}

const selectBed = (bed: BedInfo) => {
  selectedBed.value = bed
  selectedStudent.value = null
}

const selectStudent = (student: MyClassStudent) => {
  selectedStudent.value = student
}

const handleSubmit = async () => {
  if (!canSubmit.value) return

  submitting.value = true
  try {
    await universalPlaceApi.checkIn(props.dormitory!.id, {
      occupantType: 'STUDENT',
      occupantId: selectedStudent.value!.id,
      occupantName: selectedStudent.value!.name,
      username: selectedStudent.value!.studentNo,
      positionNo: selectedBed.value!.number
    })

    ElMessage.success('学生入住成功')
    emit('success')
    handleClose()
  } catch (error: any) {
    ElMessage.error(error.message || '分配失败')
  } finally {
    submitting.value = false
  }
}

const handleCheckOut = async () => {
  if (!selectedBed.value?.student || !props.dormitory) return

  try {
    await ElMessageBox.confirm(
      `确定要为 ${selectedBed.value.student.occupantName} 办理退宿吗？`,
      '确认退宿',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await universalPlaceApi.checkOut(props.dormitory.id, selectedBed.value.student.id)

    ElMessage.success('退宿成功')
    loadDormitoryData()
    emit('success')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const handleClose = () => {
  selectedBed.value = null
  selectedStudent.value = null
  studentSearchQuery.value = ''
  classStudents.value = []
  dialogVisible.value = false
}

const getGenderLabel = (genderType?: number) => {
  if (genderType === 1) return '男生宿舍'
  if (genderType === 2) return '女生宿舍'
  return '混合宿舍'
}

const getOccupancyClass = () => {
  const rate = currentOccupants.value.length / (props.dormitory?.bedCapacity || 1)
  if (rate === 0) return 'empty'
  if (rate < 1) return 'partial'
  return 'full'
}

const getBedClass = (bed: BedInfo) => {
  const classes = []
  if (selectedBed.value?.number === bed.number) {
    classes.push('selected')
  }
  if (bed.student) {
    classes.push('occupied')
  } else {
    classes.push('empty')
  }
  return classes.join(' ')
}

// Watchers
watch(() => props.visible, (newVal) => {
  if (newVal && props.dormitory) {
    loadDormitoryData()
    loadClassStudents()
  }
})

watch(() => props.dormitory, () => {
  if (props.visible && props.dormitory) {
    loadDormitoryData()
  }
})
</script>

<style scoped lang="scss">
.dormitory-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;

  .info-left {
    .dormitory-name {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin: 0 0 4px 0;
    }

    .dormitory-meta {
      font-size: 13px;
      color: #909399;
      margin: 0;
    }
  }

  .info-right {
    text-align: right;

    .occupancy {
      font-size: 20px;
      font-weight: bold;

      &.empty { color: #909399; }
      &.partial { color: #409eff; }
      &.full { color: #67c23a; }
    }

    .occupancy-label {
      font-size: 12px;
      color: #909399;
    }
  }
}

.bed-section {
  margin-bottom: 20px;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .section-title {
      font-size: 14px;
      font-weight: 500;
      color: #606266;
    }

    .legend {
      display: flex;
      gap: 12px;

      .legend-item {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #909399;

        .dot {
          width: 10px;
          height: 10px;
          border-radius: 2px;

          &.empty { background: #e4e7ed; }
          &.occupied { background: #67c23a; }
          &.selected { background: #409eff; border: 2px solid #409eff; }
        }
      }
    }
  }

  .bed-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 10px;
  }

  .bed-item {
    background: #fff;
    border: 2px solid #e4e7ed;
    border-radius: 8px;
    padding: 12px;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: #c0c4cc;
    }

    &.selected {
      border-color: #409eff;
      background: #ecf5ff;

      .bed-number { color: #409eff; }
    }

    &.occupied {
      border-color: #b3e19d;
      background: #f0f9eb;

      .bed-number { color: #67c23a; }
    }

    .bed-number {
      font-size: 14px;
      font-weight: 600;
      color: #606266;
      margin-bottom: 4px;
    }

    .bed-student {
      font-size: 12px;
      color: #67c23a;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .bed-empty {
      font-size: 12px;
      color: #c0c4cc;
    }
  }
}

.student-selection {
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 8px;
  padding: 16px;

  .selection-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 500;
    color: #409eff;
    margin-bottom: 12px;
  }

  .search-box {
    margin-bottom: 12px;
  }

  .student-list {
    max-height: 200px;
    overflow-y: auto;
    background: #fff;
    border-radius: 6px;
    border: 1px solid #e4e7ed;

    .loading-state,
    .empty-state {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 24px;
      color: #909399;
      font-size: 14px;
    }

    .loading-icon {
      animation: spin 1s linear infinite;
    }

    .student-items {
      .student-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 10px 12px;
        cursor: pointer;
        transition: background 0.2s;

        &:not(:last-child) {
          border-bottom: 1px solid #f0f0f0;
        }

        &:hover {
          background: #f5f7fa;
        }

        &.selected {
          background: #ecf5ff;
        }

        .student-avatar {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 12px;
          font-weight: 500;
          flex-shrink: 0;

          &.male {
            background: #e8f4ff;
            color: #409eff;
          }

          &.female {
            background: #fef0f0;
            color: #f56c6c;
          }
        }

        .student-info {
          flex: 1;
          min-width: 0;

          .student-name {
            font-size: 14px;
            font-weight: 500;
            color: #303133;
          }

          .student-no {
            font-size: 12px;
            color: #909399;
          }
        }

        .check-icon {
          color: #409eff;
          font-size: 18px;
        }
      }
    }
  }
}

.occupied-bed-info {
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 8px;
  padding: 16px;

  .student-detail {
    display: flex;
    align-items: center;
    gap: 12px;

    .student-avatar.large {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 500;

      &.male {
        background: #e8f4ff;
        color: #409eff;
      }

      &.female {
        background: #fef0f0;
        color: #f56c6c;
      }
    }

    .student-info {
      flex: 1;

      .student-name {
        font-size: 15px;
        font-weight: 500;
        color: #303133;
      }

      .student-no {
        font-size: 13px;
        color: #909399;
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
