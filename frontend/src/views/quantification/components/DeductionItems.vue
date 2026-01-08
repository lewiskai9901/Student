<template>
  <div class="deduction-items" v-loading="loading">
    <!-- 扣分项卡片列表 -->
    <div v-if="deductionItems.length > 0" class="items-grid">
      <div
        v-for="item in deductionItems"
        :key="item.id"
        class="deduction-item-card"
        :class="{ 'is-deducted': isItemDeducted(item) }"
      >
        <!-- 固定扣分模式：点击即扣分 -->
        <div
          v-if="item.deductMode === 1"
          class="item-fixed"
          @click="handleFixedDeduct(item)"
        >
          <div class="item-icon">
            <el-icon><WarningFilled /></el-icon>
          </div>
          <div class="item-info">
            <div class="item-name">{{ item.itemName }}</div>
            <div class="item-score">-{{ item.fixedScore }}</div>
          </div>
          <div v-if="isItemDeducted(item)" class="item-badge">
            <el-icon><Check /></el-icon>
          </div>
        </div>

        <!-- 区间扣分模式：需要输入分数 -->
        <div
          v-else-if="item.deductMode === 2"
          class="item-range"
        >
          <div class="item-header">
            <div class="item-icon">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="item-name">{{ item.itemName }}</div>
          </div>
          <div class="item-body">
            <el-input-number
              v-model="rangeScores[item.id]"
              :min="item.minScore"
              :max="item.maxScore"
              :step="0.5"
              :precision="1"
              size="small"
            />
            <span class="range-hint">{{ item.minScore }} ~ {{ item.maxScore }}</span>
          </div>
          <div class="item-footer">
            <el-button
              type="primary"
              size="small"
              :disabled="!rangeScores[item.id]"
              @click="handleRangeDeduct(item)"
            >
              扣分
            </el-button>
          </div>
        </div>

        <!-- 人次扣分模式：需要输入人数 -->
        <div
          v-else-if="item.deductMode === 3"
          class="item-count"
        >
          <div class="item-header">
            <div class="item-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="item-name">{{ item.itemName }}</div>
          </div>
          <div class="item-body">
            <el-input-number
              v-model="personCounts[item.id]"
              :min="1"
              :step="1"
              size="small"
            />
            <span class="count-hint">{{ item.scorePerPerson }}/人</span>
          </div>
          <div class="item-footer">
            <span class="total-score">
              共 {{ calculateCountScore(item) }} 分
            </span>
            <el-button
              type="primary"
              size="small"
              :disabled="!personCounts[item.id]"
              @click="handleCountDeduct(item)"
            >
              扣分
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-else description="该类别暂无扣分项" />

    <!-- 图片上传对话框 -->
    <el-dialog v-model="imageDialogVisible" title="上传检查图片" width="600px">
      <el-upload
        v-model:file-list="uploadImages"
        action="#"
        list-type="picture-card"
        :auto-upload="false"
        :limit="5"
      >
        <el-icon><Plus /></el-icon>
      </el-upload>
      <template #footer>
        <el-button @click="imageDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmDeduct">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { WarningFilled, Warning, User, Check, Plus } from '@element-plus/icons-vue'
import { getEnabledDeductionItemsByTypeId, type DeductionItem } from '@/api/deductionItems'
import type { ScoringDetailRequest } from '@/api/checkRecords'
import { uploadFiles, type FileUploadResponse } from '@/api/file'

interface Props {
  categoryId: number
  classId: number
  linkId?: number
  linkType?: number
  linkNo?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  deduct: [detail: ScoringDetailRequest]
}>()

const loading = ref(false)
const deductionItems = ref<DeductionItem[]>([])
const rangeScores = reactive<Record<number, number>>({})
const personCounts = reactive<Record<number, number>>({})
const deductedItems = ref<Set<number>>(new Set())

const imageDialogVisible = ref(false)
const uploadImages = ref<any[]>([])
const pendingDeduction = ref<ScoringDetailRequest | null>(null)

// 判断扣分项是否已扣分
const isItemDeducted = (item: DeductionItem) => {
  return deductedItems.value.has(item.id)
}

// 计算人次扣分总分
const calculateCountScore = (item: DeductionItem) => {
  const count = personCounts[item.id] || 0
  const score = item.scorePerPerson || 0
  return (count * score).toFixed(1)
}

// 固定扣分
const handleFixedDeduct = (item: DeductionItem) => {
  if (isItemDeducted(item)) {
    ElMessage.warning('该项已扣分')
    return
  }

  const detail: ScoringDetailRequest = {
    categoryId: props.categoryId,
    deductionItemId: item.id,
    deductionItemName: item.itemName,
    deductMode: item.deductMode,
    classId: props.classId,
    deductScore: item.fixedScore || 0,
    dormitoryId: props.linkType === 1 ? props.linkId : undefined,
    dormitoryNo: props.linkType === 1 ? props.linkNo : undefined,
    classroomId: props.linkType === 2 ? props.linkId : undefined,
    classroomNo: props.linkType === 2 ? props.linkNo : undefined
  }

  // 标记为已扣分
  deductedItems.value.add(item.id)

  emit('deduct', detail)
}

// 区间扣分
const handleRangeDeduct = (item: DeductionItem) => {
  const score = rangeScores[item.id]
  if (!score) {
    ElMessage.warning('请输入扣分分数')
    return
  }

  const detail: ScoringDetailRequest = {
    categoryId: props.categoryId,
    deductionItemId: item.id,
    deductionItemName: item.itemName,
    deductMode: item.deductMode,
    classId: props.classId,
    deductScore: score,
    dormitoryId: props.linkType === 1 ? props.linkId : undefined,
    dormitoryNo: props.linkType === 1 ? props.linkNo : undefined,
    classroomId: props.linkType === 2 ? props.linkId : undefined,
    classroomNo: props.linkType === 2 ? props.linkNo : undefined
  }

  // 清空输入
  rangeScores[item.id] = 0

  emit('deduct', detail)
}

// 人次扣分
const handleCountDeduct = (item: DeductionItem) => {
  const count = personCounts[item.id]
  if (!count) {
    ElMessage.warning('请输入违规人数')
    return
  }

  const totalScore = count * (item.scorePerPerson || 0)

  const detail: ScoringDetailRequest = {
    categoryId: props.categoryId,
    deductionItemId: item.id,
    deductionItemName: item.itemName,
    deductMode: item.deductMode,
    classId: props.classId,
    deductScore: totalScore,
    personCount: count,
    dormitoryId: props.linkType === 1 ? props.linkId : undefined,
    dormitoryNo: props.linkType === 1 ? props.linkNo : undefined,
    classroomId: props.linkType === 2 ? props.linkId : undefined,
    classroomNo: props.linkType === 2 ? props.linkNo : undefined
  }

  // 清空输入
  personCounts[item.id] = 0

  emit('deduct', detail)
}

// 确认扣分（带图片）
const handleConfirmDeduct = async () => {
  if (!pendingDeduction.value) return

  try {
    loading.value = true

    // 上传图片并获取URL
    const imageUrls: string[] = []
    if (uploadImages.value && uploadImages.value.length > 0) {
      // 从 el-upload 的 file-list 中提取实际的 File 对象
      const files = uploadImages.value
        .map(item => item.raw)
        .filter(file => file instanceof File) as File[]

      if (files.length > 0) {
        const uploadResults = await uploadFiles(files, 'check_record', undefined)
        imageUrls.push(...uploadResults.map(r => r.fileUrl))
      }
    }

    pendingDeduction.value.images = imageUrls
    emit('deduct', pendingDeduction.value)

    imageDialogVisible.value = false
    uploadImages.value = []
    pendingDeduction.value = null

    ElMessage.success('扣分成功')
  } catch (error: any) {
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    loading.value = false
  }
}

// 加载扣分项
const loadDeductionItems = async () => {
  loading.value = true
  try {
    const data = await getEnabledDeductionItemsByTypeId(props.categoryId)
    deductionItems.value = data
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
.deduction-items {
  padding: var(--spacing-md);

  .items-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: var(--spacing-md);
  }

  .deduction-item-card {
    border: 2px solid var(--border-color);
    border-radius: var(--radius-md);
    transition: all 0.3s;
    overflow: hidden;

    &:hover {
      border-color: var(--primary-color);
      box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
    }

    &.is-deducted {
      background: #f0f9ff;
      border-color: #3b82f6;
    }

    .item-fixed {
      padding: var(--spacing-md);
      cursor: pointer;
      position: relative;

      .item-icon {
        font-size: 32px;
        color: #f59e0b;
        margin-bottom: var(--spacing-sm);
      }

      .item-info {
        .item-name {
          font-size: 14px;
          font-weight: 600;
          color: var(--text-primary);
          margin-bottom: var(--spacing-xs);
        }

        .item-score {
          font-size: 20px;
          font-weight: bold;
          color: #f56c6c;
        }
      }

      .item-badge {
        position: absolute;
        top: var(--spacing-sm);
        right: var(--spacing-sm);
        width: 24px;
        height: 24px;
        background: #10b981;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-size: 16px;
      }
    }

    .item-range,
    .item-count {
      padding: var(--spacing-md);

      .item-header {
        display: flex;
        align-items: center;
        gap: var(--spacing-sm);
        margin-bottom: var(--spacing-md);

        .item-icon {
          font-size: 24px;
          color: #f59e0b;
        }

        .item-name {
          flex: 1;
          font-size: 14px;
          font-weight: 600;
          color: var(--text-primary);
        }
      }

      .item-body {
        margin-bottom: var(--spacing-md);

        .el-input-number {
          width: 100%;
        }

        .range-hint,
        .count-hint {
          display: block;
          margin-top: var(--spacing-xs);
          font-size: 12px;
          color: var(--text-secondary);
        }
      }

      .item-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .total-score {
          font-size: 14px;
          font-weight: bold;
          color: #f56c6c;
        }

        .el-button {
          flex: 1;
        }
      }
    }
  }
}
</style>
