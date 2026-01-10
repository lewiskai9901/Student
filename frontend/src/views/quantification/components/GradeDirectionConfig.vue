<template>
  <div v-loading="loading" class="grade-direction-config">
    <!-- 已配置的专业方向列表 -->
    <div v-if="directionList.length > 0" class="direction-list">
      <div
        v-for="item in directionList"
        :key="item.id"
        class="direction-card"
      >
        <div class="card-header">
          <div class="header-left">
            <el-icon class="major-icon" :style="{ color: getMajorColor(item.majorId) }">
              <Document />
            </el-icon>
            <div class="title-group">
              <h4>{{ item.majorName }}</h4>
              <div class="subtitle">
                {{ item.educationSystem }} · {{ item.skillLevel }}
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-switch
              v-model="item.isEnabled"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(item)"
            />
            <el-button
              link
              type="danger"
              :icon="Delete"
              @click="handleRemove(item)"
            >
              移除
            </el-button>
          </div>
        </div>

        <div class="card-body">
          <div class="info-row">
            <div class="info-item">
              <span class="label">计划班级数:</span>
              <el-input-number
                v-model="item.plannedClassCount"
                :min="0"
                :max="99"
                size="small"
                @change="handlePlannedClassCountChange(item)"
              />
            </div>
            <div class="info-item">
              <span class="label">实际班级数:</span>
              <span class="value">{{ item.actualClassCount || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="label">学生总数:</span>
              <span class="value">{{ item.actualStudentCount || 0 }}</span>
            </div>
          </div>

          <div class="progress-row">
            <div class="progress-item">
              <span class="progress-label">招生进度</span>
              <el-progress
                :percentage="getEnrollmentProgress(item)"
                :color="getProgressColor(getEnrollmentProgress(item))"
                :stroke-width="8"
              >
                <template #default="{ percentage }">
                  <span class="progress-text">{{ percentage }}%</span>
                </template>
              </el-progress>
            </div>
          </div>

          <div class="meta-info">
            <el-tag size="small" type="info">
              学制: {{ item.duration || 0 }}年
            </el-tag>
            <el-tag size="small" type="warning">
              标准班额: {{ item.standardClassSize || 40 }}人
            </el-tag>
            <el-tag v-if="item.directionCode" size="small">
              {{ item.directionCode }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty
      v-else
      description="暂未配置专业方向"
      :image-size="120"
    >
      <template #image>
        <el-icon :size="80" color="#909399">
          <FolderOpened />
        </el-icon>
      </template>
    </el-empty>

    <!-- 添加专业方向按钮 -->
    <div class="add-direction-section">
      <el-button
        type="primary"
        :icon="Plus"
        @click="showAddDialog = true"
      >
        添加专业方向
      </el-button>
    </div>

    <!-- 添加专业方向对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="添加专业方向"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="专业" prop="majorId">
          <el-select
            v-model="formData.majorId"
            placeholder="请选择专业"
            style="width: 100%"
            filterable
            @change="handleMajorChange"
          >
            <el-option
              v-for="major in majorList"
              :key="major.id"
              :label="major.majorName"
              :value="major.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="专业方向" prop="majorDirectionId">
          <el-select
            v-model="formData.majorDirectionId"
            placeholder="请先选择专业"
            style="width: 100%"
            :disabled="!formData.majorId"
            filterable
          >
            <el-option
              v-for="direction in availableDirections"
              :key="direction.id"
              :label="`${direction.educationSystem} ${direction.skillLevel}`"
              :value="direction.id"
            >
              <div style="display: flex; justify-content: space-between">
                <span>{{ direction.educationSystem }} {{ direction.skillLevel }}</span>
                <span style="color: #8492a6; font-size: 12px">
                  {{ direction.duration }}年 · {{ direction.standardClassSize || 40 }}人/班
                </span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="计划班级数" prop="plannedClassCount">
          <el-input-number
            v-model="formData.plannedClassCount"
            :min="1"
            :max="99"
            placeholder="请输入计划班级数"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="是否启用" prop="isEnabled">
          <el-switch
            v-model="formData.isEnabled"
            :active-value="1"
            :inactive-value="0"
          />
          <span style="margin-left: 12px; color: #909399; font-size: 13px">
            启用后可创建该专业方向的班级
          </span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage, ElMessageBox, ElForm } from 'element-plus'
import {
  Document,
  Delete,
  Plus,
  FolderOpened
} from '@element-plus/icons-vue'
import {
  getDirectionsByGrade,
  addDirectionToGrade,
  updateGradeMajorDirection,
  deleteGradeMajorDirection
} from '@/api/v2/gradeMajorDirection'
import { getAllEnabledMajors } from '@/api/v2/major'
import { getDirectionsByMajor } from '@/api/v2/majorDirection'
import type { GradeMajorDirection } from '@/api/v2/gradeMajorDirection'

interface Props {
  gradeId: number
  gradeName: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  refresh: []
}>()

const formRef = ref<InstanceType<typeof ElForm>>()
const loading = ref(false)
const showAddDialog = ref(false)
const directionList = ref<GradeMajorDirection[]>([])
const majorList = ref<any[]>([])
const availableDirections = ref<any[]>([])

const formData = reactive({
  majorId: null as number | null,
  majorDirectionId: null as number | null,
  plannedClassCount: 1,
  isEnabled: 1
})

const formRules = {
  majorId: [
    { required: true, message: '请选择专业', trigger: 'change' }
  ],
  majorDirectionId: [
    { required: true, message: '请选择专业方向', trigger: 'change' }
  ],
  plannedClassCount: [
    { required: true, message: '请输入计划班级数', trigger: 'blur' }
  ]
}

// 获取专业颜色(根据专业ID生成固定颜色)
const getMajorColor = (majorId: number | null) => {
  if (!majorId) return '#909399'
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
  return colors[majorId % colors.length]
}

// 计算招生进度
const getEnrollmentProgress = (item: GradeMajorDirection) => {
  const planned = item.plannedClassCount || 0
  const actual = item.actualClassCount || 0
  if (planned === 0) return 0
  return Math.min(Math.round((actual / planned) * 100), 100)
}

// 获取进度条颜色
const getProgressColor = (percentage: number) => {
  if (percentage >= 100) return '#67C23A'
  if (percentage >= 50) return '#409EFF'
  return '#E6A23C'
}

// 加载年级的专业方向配置
const loadDirections = async () => {
  loading.value = true
  try {
    const data = await getDirectionsByGrade(props.gradeId)
    directionList.value = data || []
  } catch (error) {
    console.error('加载专业方向配置失败:', error)
    ElMessage.error('加载专业方向配置失败')
  } finally {
    loading.value = false
  }
}

// 加载所有专业
const loadMajors = async () => {
  try {
    const data = await getAllEnabledMajors()
    majorList.value = data || []
  } catch (error) {
    console.error('加载专业列表失败:', error)
  }
}

// 专业变更处理
const handleMajorChange = async (majorId: number) => {
  formData.majorDirectionId = null
  availableDirections.value = []

  if (!majorId) return

  try {
    loading.value = true
    const data = await getDirectionsByMajor(majorId)

    // 过滤掉已经配置的专业方向
    const configuredIds = directionList.value.map(d => d.majorDirectionId)
    availableDirections.value = (data || []).filter(
      d => !configuredIds.includes(d.id)
    )

    if (availableDirections.value.length === 0) {
      ElMessage.warning('该专业下没有可用的专业方向')
    }
  } catch (error) {
    console.error('加载专业方向列表失败:', error)
    ElMessage.error('加载专业方向列表失败')
  } finally {
    loading.value = false
  }
}

// 添加专业方向
const handleAdd = async () => {
  if (!formRef.value) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    loading.value = true

    await addDirectionToGrade({
      gradeId: props.gradeId,
      majorDirectionId: formData.majorDirectionId!,
      plannedClassCount: formData.plannedClassCount,
      isEnabled: formData.isEnabled
    })

    ElMessage.success('添加成功')
    showAddDialog.value = false
    formData.majorId = null
    formData.majorDirectionId = null
    formData.plannedClassCount = 1
    formData.isEnabled = 1

    await loadDirections()
    emit('refresh')
  } catch (error: any) {
    console.error('添加失败:', error)
    ElMessage.error(error.message || '添加失败')
  } finally {
    loading.value = false
  }
}

// 启用/禁用状态变更
const handleStatusChange = async (item: GradeMajorDirection) => {
  try {
    loading.value = true
    await updateGradeMajorDirection(item.id!, {
      gradeId: item.gradeId,
      majorDirectionId: item.majorDirectionId,
      isEnabled: item.isEnabled
    })
    ElMessage.success(item.isEnabled === 1 ? '已启用' : '已禁用')
    emit('refresh')
  } catch (error: any) {
    console.error('更新失败:', error)
    ElMessage.error(error.message || '更新失败')
    // 回滚状态
    item.isEnabled = item.isEnabled === 1 ? 0 : 1
  } finally {
    loading.value = false
  }
}

// 计划班级数变更
const handlePlannedClassCountChange = async (item: GradeMajorDirection) => {
  try {
    loading.value = true
    await updateGradeMajorDirection(item.id!, {
      gradeId: item.gradeId,
      majorDirectionId: item.majorDirectionId,
      plannedClassCount: item.plannedClassCount
    })
    ElMessage.success('更新成功')
    emit('refresh')
  } catch (error: any) {
    console.error('更新失败:', error)
    ElMessage.error(error.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 移除专业方向
const handleRemove = async (item: GradeMajorDirection) => {
  try {
    const actualCount = item.actualClassCount || 0
    const studentCount = item.actualStudentCount || 0

    let confirmMessage = `确定要移除【${item.majorName} - ${item.educationSystem} ${item.skillLevel}】吗?`

    if (actualCount > 0 || studentCount > 0) {
      confirmMessage += `\n\n该专业方向下有 ${actualCount} 个班级, ${studentCount} 名学生。`
      confirmMessage += '\n移除后将无法再创建该专业方向的班级。'
    }

    await ElMessageBox.confirm(confirmMessage, '移除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    loading.value = true
    await deleteGradeMajorDirection(item.id!)
    ElMessage.success('移除成功')
    await loadDirections()
    emit('refresh')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('移除失败:', error)
      ElMessage.error(error.message || '移除失败')
    }
  } finally {
    loading.value = false
  }
}

// 监听年级ID变化
watch(
  () => props.gradeId,
  () => {
    if (props.gradeId) {
      loadDirections()
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadMajors()
})
</script>

<style scoped lang="scss">
.grade-direction-config {
  padding: 20px 0;

  .direction-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
    gap: 20px;
    margin-bottom: 20px;
  }

  .direction-card {
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    overflow: hidden;
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
      border-color: #409eff;
    }

    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px;
      background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
      border-bottom: 1px solid #e4e7ed;

      .header-left {
        display: flex;
        align-items: center;
        flex: 1;

        .major-icon {
          font-size: 32px;
          margin-right: 12px;
        }

        .title-group {
          h4 {
            margin: 0 0 4px 0;
            font-size: 16px;
            font-weight: 600;
            color: #303133;
          }

          .subtitle {
            font-size: 13px;
            color: #606266;
          }
        }
      }

      .header-actions {
        display: flex;
        align-items: center;
        gap: 12px;
      }
    }

    .card-body {
      padding: 16px;

      .info-row {
        display: flex;
        justify-content: space-between;
        margin-bottom: 16px;
        gap: 12px;

        .info-item {
          display: flex;
          flex-direction: column;
          gap: 4px;

          .label {
            font-size: 13px;
            color: #909399;
          }

          .value {
            font-size: 18px;
            font-weight: 600;
            color: #303133;
          }
        }
      }

      .progress-row {
        margin-bottom: 16px;

        .progress-item {
          .progress-label {
            display: block;
            font-size: 13px;
            color: #909399;
            margin-bottom: 8px;
          }

          .progress-text {
            font-size: 12px;
            font-weight: 600;
          }
        }
      }

      .meta-info {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
      }
    }
  }

  .add-direction-section {
    text-align: center;
    padding: 20px 0;
  }
}
</style>
