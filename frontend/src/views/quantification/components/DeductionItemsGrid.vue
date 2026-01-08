<template>
  <div class="deduction-items-grid" v-loading="loading">
    <!-- 扣分项网格 -->
    <div v-if="deductionItems.length > 0" class="items-grid">
      <div
        v-for="item in deductionItems"
        :key="item.id"
        class="deduction-card"
        :class="{ 'is-selected': isItemDeducted(item.id) }"
      >
        <!-- 固定扣分模式 -->
        <div
          v-if="item.deductMode === 1"
          class="card-content fixed-mode"
          @click="handleToggleFixed(item)"
        >
          <div class="card-header">
            <div class="icon-wrapper">
              <el-icon class="item-icon"><WarningFilled /></el-icon>
            </div>
            <el-icon v-if="isItemDeducted(item.id)" class="check-icon"><CircleCheckFilled /></el-icon>
          </div>

          <div class="card-body">
            <div class="item-title">{{ item.itemName }}</div>
            <div class="score-display">
              <span class="score-number">{{ item.fixedScore }}</span>
              <span class="score-unit">分</span>
            </div>

            <div class="card-action">
              <span class="action-text">{{ isItemDeducted(item.id) ? '点击取消' : '点击扣分' }}</span>
            </div>

            <!-- 照片和备注按钮 - 始终显示,保持高度一致 -->
            <div v-if="item.allowPhoto || item.allowRemark" class="card-extras">
              <el-button
                v-if="item.allowPhoto"
                size="small"
                text
                :disabled="!isItemDeducted(item.id)"
                @click.stop="handleUploadPhoto(item)"
              >
                <el-icon><Camera /></el-icon>
                照片<span v-if="itemPhotos[item.id]?.length" class="badge">({{ itemPhotos[item.id].length }})</span>
              </el-button>
              <el-button
                v-if="item.allowRemark"
                size="small"
                text
                :disabled="!isItemDeducted(item.id)"
                @click.stop="handleAddRemark(item)"
              >
                <el-icon><Edit /></el-icon>
                备注
              </el-button>
            </div>
          </div>
        </div>

        <!-- 人次扣分模式 (mode 2) -->
        <div v-else-if="item.deductMode === 2" class="card-content count-mode">
          <div class="card-header">
            <div class="icon-wrapper">
              <el-icon class="item-icon"><UserFilled /></el-icon>
            </div>
            <span class="mode-badge">人次</span>
          </div>

          <div class="card-body">
            <div class="title-row">
              <span class="item-title">{{ item.itemName }}</span>
              <span v-if="personCounts[item.id] > 0" class="score-indicator">-{{ calculateCountScore(item) }}</span>
            </div>
            <div class="per-person-info">
              <span class="per-person-text">{{ item.baseScore || 0 }}分 + {{ item.scorePerPerson || item.perPersonScore || 0 }}分/人</span>
            </div>
            <div class="input-wrapper">
              <el-input-number
                v-model="personCounts[item.id]"
                :min="0"
                :step="1"
                size="default"
                controls-position="right"
                @change="handleCountChange(item)"
                placeholder="输入人数"
                :value-on-clear="0"
              />
            </div>

            <!-- 学生选择、照片和备注 - 始终显示,保持高度一致 -->
            <div v-if="item.allowStudents || item.allowPhoto || item.allowRemark" class="extras-section">
              <!-- 学生选择 -->
              <el-select
                v-if="item.allowStudents"
                v-model="itemStudents[item.id]"
                multiple
                filterable
                remote
                reserve-keyword
                :disabled="personCounts[item.id] <= 0"
                :remote-method="(query) => searchStudents(query, item.id)"
                placeholder="搜索学生姓名"
                size="small"
                class="student-select"
              >
                <el-option
                  v-for="student in studentOptions[item.id]"
                  :key="student.id"
                  :label="student.realName"
                  :value="student.realName"
                />
              </el-select>

              <!-- 照片和备注按钮 -->
              <div class="action-buttons">
                <el-button
                  v-if="item.allowPhoto"
                  size="small"
                  text
                  :disabled="personCounts[item.id] <= 0"
                  @click.stop="handleUploadPhoto(item)"
                >
                  <el-icon><Camera /></el-icon>
                  照片({{ itemPhotos[item.id]?.length || 0 }})
                </el-button>
                <el-button
                  v-if="item.allowRemark"
                  size="small"
                  text
                  :disabled="personCounts[item.id] <= 0"
                  @click.stop="handleAddRemark(item)"
                >
                  <el-icon><Edit /></el-icon>
                  备注
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 区间扣分模式 (mode 3) -->
        <div v-else-if="item.deductMode === 3" class="card-content range-mode">
          <div class="card-header">
            <div class="icon-wrapper">
              <el-icon class="item-icon"><Edit /></el-icon>
            </div>
            <span class="mode-badge">区间</span>
          </div>

          <div class="card-body">
            <div class="title-row">
              <span class="item-title">{{ item.itemName }}</span>
              <span v-if="rangeScores[item.id] > 0" class="score-indicator">-{{ rangeScores[item.id].toFixed(1) }}</span>
            </div>
            <div class="range-info">
              <span class="range-text">{{ parseRangeMin(item) }} ~ {{ parseRangeMax(item) }} 分</span>
            </div>
            <div class="input-wrapper">
              <el-input-number
                v-model="rangeScores[item.id]"
                :min="parseRangeMin(item)"
                :max="parseRangeMax(item)"
                :step="0.5"
                :precision="1"
                size="default"
                controls-position="right"
                @change="handleRangeChange(item)"
                placeholder="输入扣分"
              />
            </div>

            <!-- 照片和备注 - 始终显示,保持高度一致 -->
            <div v-if="item.allowPhoto || item.allowRemark" class="extras-section">
              <div class="action-buttons">
                <el-button
                  v-if="item.allowPhoto"
                  size="small"
                  text
                  :disabled="rangeScores[item.id] <= 0"
                  @click.stop="handleUploadPhoto(item)"
                >
                  <el-icon><Camera /></el-icon>
                  照片({{ itemPhotos[item.id]?.length || 0 }})
                </el-button>
                <el-button
                  v-if="item.allowRemark"
                  size="small"
                  text
                  :disabled="rangeScores[item.id] <= 0"
                  @click.stop="handleAddRemark(item)"
                >
                  <el-icon><Edit /></el-icon>
                  备注
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-else description="暂无扣分项" />

    <!-- 照片上传对话框 -->
    <el-dialog v-model="photoDialogVisible" title="上传照片" width="600px">
      <el-upload
        :action="uploadUrl"
        :headers="uploadHeaders"
        list-type="picture-card"
        :file-list="currentPhotoList"
        :on-success="handlePhotoSuccess"
        :on-remove="handlePhotoRemove"
        :on-preview="handlePhotoPreview"
        :limit="5"
        :on-exceed="handlePhotoExceed"
        accept="image/*"
      >
        <el-icon><Plus /></el-icon>
      </el-upload>
      <template #footer>
        <el-button @click="photoDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPhotos">确定</el-button>
      </template>
    </el-dialog>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="previewDialogVisible" title="图片预览" width="800px">
      <img :src="previewImageUrl" alt="预览图片" style="width: 100%; display: block;" />
    </el-dialog>

    <!-- 备注对话框 -->
    <el-dialog v-model="remarkDialogVisible" title="添加备注" width="500px">
      <el-input
        v-model="currentRemark"
        type="textarea"
        :rows="5"
        maxlength="500"
        show-word-limit
        placeholder="请输入备注信息..."
      />
      <template #footer>
        <el-button @click="remarkDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRemark">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { WarningFilled, Edit, UserFilled, CircleCheckFilled, Camera, Plus } from '@element-plus/icons-vue'
import { getEnabledDeductionItemsByTypeId, type DeductionItem } from '@/api/deductionItems'
import type { ScoringDetailRequest } from '@/api/checkRecords'
import { searchStudents as searchStudentsApi } from '@/api/student'
import { getUploadUrl } from '@/api/upload'
import { getToken } from '@/utils/token'

// Props接口定义 - 使用 string | number 类型避免大ID精度丢失
interface Props {
  categoryId: string | number
  classId: string | number
  linkId?: string | number
  linkType?: number
  linkNo?: string
  deductedItems: Record<string, ScoringDetailRequest>  // 使用string避免ID精度丢失
}

const props = defineProps<Props>()

const emit = defineEmits<{
  toggleDeduct: [detail: ScoringDetailRequest, isAdd: boolean]
}>()

const loading = ref(false)
const deductionItems = ref<DeductionItem[]>([])
const rangeScores = reactive<Record<number, number>>({})
const personCounts = reactive<Record<number, number>>({})

// 照片、备注、学生相关数据
const itemPhotos = reactive<Record<number, string[]>>({})
const itemRemarks = reactive<Record<number, string>>({})
const itemStudents = reactive<Record<number, any[]>>({})

// 对话框控制
const photoDialogVisible = ref(false)
const remarkDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const currentItem = ref<any>(null)
const currentPhotoList = ref<any[]>([])
const currentRemark = ref('')
const previewImageUrl = ref('')

// 学生搜索
const studentOptions = reactive<Record<number, any[]>>({})

// 上传相关
const uploadUrl = ref(getUploadUrl())
const uploadHeaders = ref({ Authorization: `Bearer ${getToken()}` })

// 解析区间最小值
const parseRangeMin = (item: DeductionItem): number => {
  if (item.minScore !== undefined) return item.minScore
  // 尝试从rangeConfig解析
  if (item.rangeConfig) {
    try {
      const config = typeof item.rangeConfig === 'string' ? JSON.parse(item.rangeConfig) : item.rangeConfig
      if (Array.isArray(config) && config.length > 0 && config[0].min !== undefined) {
        return config[0].min
      }
    } catch {
      // 解析失败
    }
  }
  return 0
}

// 解析区间最大值
const parseRangeMax = (item: DeductionItem): number => {
  if (item.maxScore !== undefined) return item.maxScore
  // 尝试从rangeConfig解析
  if (item.rangeConfig) {
    try {
      const config = typeof item.rangeConfig === 'string' ? JSON.parse(item.rangeConfig) : item.rangeConfig
      if (Array.isArray(config) && config.length > 0 && config[0].max !== undefined) {
        return config[0].max
      }
    } catch {
      // 解析失败
    }
  }
  return 10
}

// 同步扣分数据的函数
const syncDeductedData = () => {
  // 清空现有数据
  Object.keys(personCounts).forEach(key => delete personCounts[key])
  Object.keys(rangeScores).forEach(key => delete rangeScores[key])
  Object.keys(itemPhotos).forEach(key => delete itemPhotos[key])
  Object.keys(itemRemarks).forEach(key => delete itemRemarks[key])
  Object.keys(itemStudents).forEach(key => delete itemStudents[key])

  // 为所有扣分项重新初始化默认值
  deductionItems.value.forEach(item => {
    if (item.deductMode === 2) {
      personCounts[item.id] = 0
    } else if (item.deductMode === 3) {
      rangeScores[item.id] = 0
    }
  })

  // 然后从deductedItems中恢复已扣分的项目数据
  Object.keys(props.deductedItems).forEach(key => {
    const detail = props.deductedItems[key]

    // 找到对应的扣分项(使用字符串比较,避免ID精度丢失)
    const item = deductionItems.value.find(i => String(i.id) === String(key))

    if (item) {
      const itemId = item.id

      if (item.deductMode === 2) {
        // 人次扣分 - 只有当personCount大于0时才设置
        if (detail.personCount && detail.personCount > 0) {
          personCounts[itemId] = detail.personCount
        }
      } else if (item.deductMode === 3) {
        // 区间扣分
        rangeScores[itemId] = detail.deductScore || 0
      }
      // 固定扣分不需要额外处理,isItemDeducted()会自动判断

      // 回显照片URLs
      if (detail.photoUrls) {
        try {
          const parsed = JSON.parse(detail.photoUrls)
          itemPhotos[itemId] = parsed
        } catch {
          itemPhotos[itemId] = []
        }
      }

      // 回显备注
      if (detail.remark) {
        itemRemarks[itemId] = detail.remark
      }

      // 回显学生列表
      if (detail.students && Array.isArray(detail.students)) {
        itemStudents[itemId] = detail.students
      }
    }
  })
}

// 防抖定时器
let syncTimer: any = null

// 监听deductedItems变化,同步到本地状态
watch(() => props.deductedItems, () => {
  // 清除之前的定时器,实现防抖
  if (syncTimer) {
    clearTimeout(syncTimer)
  }

  // 延迟执行同步,避免频繁触发
  syncTimer = setTimeout(() => {
    // 只有在扣分项加载完成后才同步
    if (deductionItems.value.length > 0) {
      syncDeductedData()
    }
    syncTimer = null
  }, 150) // 150ms防抖延迟
}, { deep: true, immediate: true })

// 判断扣分项是否已扣分
const isItemDeducted = (itemId: number | string) => {
  // 使用字符串比较,避免精度丢失
  const itemIdStr = String(itemId)
  return !!props.deductedItems[itemIdStr]
}

// 计算人次扣分总分
const calculateCountScore = (item: DeductionItem) => {
  const count = personCounts[item.id] || 0
  const baseScore = item.baseScore || 0
  const perPersonScore = item.scorePerPerson || item.perPersonScore || 0
  return (baseScore + count * perPersonScore).toFixed(1)
}

// 固定扣分切换
const handleToggleFixed = (item: DeductionItem) => {
  const isDeducted = isItemDeducted(item.id)

  const detail: ScoringDetailRequest = {
    categoryId: props.categoryId,
    deductionItemId: item.id,
    deductionItemName: item.itemName,
    deductMode: item.deductMode,
    classId: props.classId,
    deductScore: item.fixedScore || 0,
    linkType: props.linkType || 0,
    linkId: props.linkId || 0,
    dormitoryId: props.linkType === 1 ? props.linkId : undefined,
    dormitoryNo: props.linkType === 1 ? props.linkNo : undefined,
    classroomId: props.linkType === 2 ? props.linkId : undefined,
    classroomNo: props.linkType === 2 ? props.linkNo : undefined,
    // 新增字段
    photoUrls: itemPhotos[item.id] ? JSON.stringify(itemPhotos[item.id]) : null,
    remark: itemRemarks[item.id] || null,
    students: itemStudents[item.id] || []
  }

  const isAdd = !isDeducted
  emit('toggleDeduct', detail, isAdd)
}

// 固定扣分更新(不切换状态,仅更新照片/备注等附加信息)
const handleUpdateFixed = (item: DeductionItem) => {
  const isDeducted = isItemDeducted(item.id)

  // 如果当前没有扣分,则不更新
  if (!isDeducted) {
    return
  }

  const detail: ScoringDetailRequest = {
    categoryId: props.categoryId,
    deductionItemId: item.id,
    deductionItemName: item.itemName,
    deductMode: item.deductMode,
    classId: props.classId,
    deductScore: item.fixedScore || 0,
    linkType: props.linkType || 0,
    linkId: props.linkId || 0,
    dormitoryId: props.linkType === 1 ? props.linkId : undefined,
    dormitoryNo: props.linkType === 1 ? props.linkNo : undefined,
    classroomId: props.linkType === 2 ? props.linkId : undefined,
    classroomNo: props.linkType === 2 ? props.linkNo : undefined,
    // 新增字段
    photoUrls: itemPhotos[item.id] ? JSON.stringify(itemPhotos[item.id]) : null,
    remark: itemRemarks[item.id] || null,
    students: itemStudents[item.id] || []
  }

  // 保持当前状态,只更新数据
  emit('toggleDeduct', detail, true)
}

// 区间扣分变化 - 自动保存
const handleRangeChange = (item: DeductionItem) => {
  const score = rangeScores[item.id]

  // 如果分数为0或undefined,则删除该扣分项
  if (!score || score === 0) {
    if (isItemDeducted(item.id)) {
      const detail: ScoringDetailRequest = {
        categoryId: props.categoryId,
        deductionItemId: item.id,
        deductionItemName: item.itemName,
        deductMode: item.deductMode,
        classId: props.classId,
        deductScore: 0,
        linkType: props.linkType || 0,
        linkId: props.linkId || 0,
        dormitoryId: props.linkType === 1 ? props.linkId : undefined,
        dormitoryNo: props.linkType === 1 ? props.linkNo : undefined,
        classroomId: props.linkType === 2 ? props.linkId : undefined,
        classroomNo: props.linkType === 2 ? props.linkNo : undefined
      }
      emit('toggleDeduct', detail, false)
    }
    return
  }

  // 验证分数范围
  const minScore = parseRangeMin(item)
  const maxScore = parseRangeMax(item)

  if (score < minScore || score > maxScore) {
    ElMessage.warning(`扣分必须在 ${minScore} ~ ${maxScore} 之间`)
    rangeScores[item.id] = minScore
    return
  }

  const detail: ScoringDetailRequest = {
    categoryId: props.categoryId,
    deductionItemId: item.id,
    deductionItemName: item.itemName,
    deductMode: item.deductMode,
    classId: props.classId,
    deductScore: score,
    linkType: props.linkType || 0,
    linkId: props.linkId || 0,
    dormitoryId: props.linkType === 1 ? props.linkId : undefined,
    dormitoryNo: props.linkType === 1 ? props.linkNo : undefined,
    classroomId: props.linkType === 2 ? props.linkId : undefined,
    classroomNo: props.linkType === 2 ? props.linkNo : undefined,
    // 新增字段
    photoUrls: itemPhotos[item.id] ? JSON.stringify(itemPhotos[item.id]) : null,
    remark: itemRemarks[item.id] || null,
    students: itemStudents[item.id] || []
  }

  emit('toggleDeduct', detail, true)
}

// 人次扣分变化 - 自动保存
const handleCountChange = (item: DeductionItem) => {
  const count = personCounts[item.id]

  // 如果人数为0或undefined,则删除该扣分项
  if (!count || count === 0) {
    if (isItemDeducted(item.id)) {
      const detail: ScoringDetailRequest = {
        categoryId: props.categoryId,
        deductionItemId: item.id,
        deductionItemName: item.itemName,
        deductMode: item.deductMode,
        classId: props.classId,
        deductScore: 0,
        personCount: 0,
        linkType: props.linkType || 0,
        linkId: props.linkId || 0,
        dormitoryId: props.linkType === 1 ? props.linkId : undefined,
        dormitoryNo: props.linkType === 1 ? props.linkNo : undefined,
        classroomId: props.linkType === 2 ? props.linkId : undefined,
        classroomNo: props.linkType === 2 ? props.linkNo : undefined
      }
      emit('toggleDeduct', detail, false)
    }
    // 确保personCounts保持为0,避免被重新设置
    personCounts[item.id] = 0
    return
  }

  if (count < 0) {
    ElMessage.warning('违规人数不能小于0')
    personCounts[item.id] = 0
    return
  }

  // 按人次扣分公式: 基础分 + 每人扣分 × 人数
  const baseScore = item.baseScore || 0
  const perPersonScore = item.scorePerPerson || item.perPersonScore || 0
  const totalScore = baseScore + (count * perPersonScore)

  const detail: ScoringDetailRequest = {
    categoryId: props.categoryId,
    deductionItemId: item.id,
    deductionItemName: item.itemName,
    deductMode: item.deductMode,
    classId: props.classId,
    deductScore: totalScore,
    personCount: count,
    linkType: props.linkType || 0,
    linkId: props.linkId || 0,
    dormitoryId: props.linkType === 1 ? props.linkId : undefined,
    dormitoryNo: props.linkType === 1 ? props.linkNo : undefined,
    classroomId: props.linkType === 2 ? props.linkId : undefined,
    classroomNo: props.linkType === 2 ? props.linkNo : undefined,
    // 新增字段
    photoUrls: itemPhotos[item.id] ? JSON.stringify(itemPhotos[item.id]) : null,
    remark: itemRemarks[item.id] || null,
    students: itemStudents[item.id] || []
  }

  emit('toggleDeduct', detail, true)
}

// 照片上传
const handleUploadPhoto = (item: DeductionItem) => {
  currentItem.value = item
  currentPhotoList.value = (itemPhotos[item.id] || []).map((url, index) => ({
    name: `photo-${index}`,
    url: url
  }))
  photoDialogVisible.value = true
}

const handlePhotoSuccess = (response: any) => {
  if (response.code === 200) {
    currentPhotoList.value.push({
      name: response.data.name,
      url: response.data.url
    })
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handlePhotoRemove = (file: any) => {
  const index = currentPhotoList.value.findIndex(f => f.url === file.url)
  if (index > -1) {
    currentPhotoList.value.splice(index, 1)
  }
}

const handlePhotoExceed = () => {
  ElMessage.warning('最多只能上传5张照片')
}

const handlePhotoPreview = (file: any) => {
  previewImageUrl.value = file.url
  previewDialogVisible.value = true
}

const confirmPhotos = () => {
  if (currentItem.value) {
    itemPhotos[currentItem.value.id] = currentPhotoList.value.map(f => f.url)
    // 触发保存
    if (currentItem.value.deductMode === 1) {
      handleUpdateFixed(currentItem.value)  // 使用更新函数而不是切换函数
    } else if (currentItem.value.deductMode === 2) {
      handleCountChange(currentItem.value)
    } else if (currentItem.value.deductMode === 3) {
      handleRangeChange(currentItem.value)
    }
  }
  photoDialogVisible.value = false
}

// 备注输入
const handleAddRemark = (item: DeductionItem) => {
  currentItem.value = item
  currentRemark.value = itemRemarks[item.id] || ''
  remarkDialogVisible.value = true
}

const confirmRemark = () => {
  if (currentItem.value) {
    itemRemarks[currentItem.value.id] = currentRemark.value
    // 触发保存
    if (currentItem.value.deductMode === 1) {
      handleUpdateFixed(currentItem.value)  // 使用更新函数而不是切换函数
    } else if (currentItem.value.deductMode === 2) {
      handleCountChange(currentItem.value)
    } else if (currentItem.value.deductMode === 3) {
      handleRangeChange(currentItem.value)
    }
  }
  remarkDialogVisible.value = false
}

// 学生搜索
const searchStudents = async (keyword: string, itemId: number) => {
  if (!keyword || keyword.length < 2) {
    studentOptions[itemId] = []
    return
  }

  try {
    const result = await searchStudentsApi({
      keyword,
      classId: props.classId,
      limit: 10
    })
    studentOptions[itemId] = result
  } catch {
    // 搜索失败
  }
}

// 加载扣分项
const loadDeductionItems = async () => {
  loading.value = true
  try {
    const data = await getEnabledDeductionItemsByTypeId(props.categoryId)
    deductionItems.value = data

    // 加载完成后,同步已有的扣分数据(syncDeductedData会处理初始化)
    syncDeductedData()
  } catch (error: any) {
    ElMessage.error(error.message || '加载扣分项失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDeductionItems()
})
</script>

<style lang="scss" scoped>
.deduction-items-grid {
  padding: 20px;

  .items-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 20px;
  }

  .deduction-card {
    position: relative;
    border-radius: 16px;
    overflow: hidden;
    background: #ffffff;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    }

    &.is-selected {
      box-shadow: 0 4px 20px rgba(16, 185, 129, 0.3);

      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 4px;
        background: linear-gradient(90deg, #10b981 0%, #059669 100%);
      }
    }

    .card-content {
      padding: 24px 24px 0;  // 移除底部padding
      min-height: 200px;
      display: flex;
      flex-direction: column;

      &.fixed-mode {
        cursor: pointer;
        background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);

        &:active {
          transform: scale(0.98);
        }
      }

      &.range-mode {
        background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
      }

      &.count-mode {
        background: linear-gradient(135deg, #f3e8ff 0%, #e9d5ff 100%);
      }
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;

      .icon-wrapper {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        background: rgba(255, 255, 255, 0.9);
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

        .item-icon {
          font-size: 24px;
          color: #f59e0b;
        }
      }

      .check-icon {
        font-size: 28px;
        color: #10b981;
        animation: bounceIn 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55);
      }

      .mode-badge {
        padding: 4px 12px;
        border-radius: 20px;
        background: rgba(255, 255, 255, 0.9);
        font-size: 12px;
        font-weight: 600;
        color: #6b7280;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
      }
    }

    .card-body {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding-bottom: 24px;  // card-body负责底部padding,确保背景色延伸到底

      .item-title {
        font-size: 16px;
        font-weight: 700;
        color: #1f2937;
        line-height: 1.4;
        letter-spacing: 0.3px;
      }

      .score-display {
        display: flex;
        align-items: baseline;
        gap: 4px;
        margin-top: 8px;

        .score-number {
          font-size: 40px;
          font-weight: 800;
          color: #dc2626;
          line-height: 1;
          font-family: 'SF Pro Display', -apple-system, sans-serif;
        }

        .score-unit {
          font-size: 18px;
          font-weight: 600;
          color: #991b1b;
        }
      }

      .range-info,
      .per-person-info {
        padding: 8px 12px;
        border-radius: 8px;
        background: rgba(255, 255, 255, 0.7);

        .range-text,
        .per-person-text {
          font-size: 14px;
          font-weight: 600;
          color: #4b5563;
        }
      }

      .input-wrapper {
        margin-top: 4px;

        :deep(.el-input-number) {
          width: 100%;

          .el-input__wrapper {
            border-radius: 10px;
            padding: 8px 12px;
            background: rgba(255, 255, 255, 0.9);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
            border: 2px solid transparent;
            transition: all 0.3s;

            &:hover {
              border-color: #3b82f6;
            }

            &.is-focus {
              border-color: #2563eb;
              box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
            }
          }

          .el-input__inner {
            text-align: center;
            font-size: 15px;
            font-weight: 600;
            color: #1f2937;
          }
        }
      }

      .title-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        min-height: 24px;

        .score-indicator {
          flex-shrink: 0;
          padding: 2px 10px;
          background: #fee2e2;
          color: #dc2626;
          font-size: 14px;
          font-weight: 700;
          border-radius: 12px;
          font-family: 'SF Pro Display', -apple-system, sans-serif;
          white-space: nowrap;
        }
      }
    }

    .card-action {
      margin-top: 12px;
      padding: 12px 0 0;  // 移除底部padding,由card-content的padding提供
      border-top: 2px dashed rgba(0, 0, 0, 0.1);
      text-align: center;

      .action-text {
        font-size: 13px;
        font-weight: 600;
        color: #6b7280;
        letter-spacing: 0.5px;
      }
    }

    .card-extras {
      display: flex;
      gap: 8px;
      padding: 8px 0 0;  // 移除底部padding,由card-content的padding提供
      border-top: 1px solid rgba(0, 0, 0, 0.05);
      margin-top: 8px;

      :deep(.el-button) {
        margin: 0;  // 移除Element Plus按钮的默认margin
      }

      .badge {
        margin-left: 2px;
        color: #409eff;
        font-weight: bold;
      }
    }

    .extras-section {
      margin-top: 10px;
      padding: 10px 0 0;  // 移除底部padding,由card-content的padding提供
      border-top: 1px solid rgba(0, 0, 0, 0.05);

      .student-select {
        width: 100%;
        margin-bottom: 8px;
      }

      .action-buttons {
        display: flex;
        gap: 8px;
        margin-bottom: 0;

        :deep(.el-button) {
          margin: 0;  // 移除Element Plus按钮的默认margin
        }
      }
    }
  }
}

@keyframes bounceIn {
  0% {
    opacity: 0;
    transform: scale(0.3);
  }
  50% {
    opacity: 1;
    transform: scale(1.05);
  }
  70% {
    transform: scale(0.9);
  }
  100% {
    transform: scale(1);
  }
}
</style>
