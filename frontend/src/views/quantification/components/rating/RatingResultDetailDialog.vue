<template>
  <el-dialog v-model="visible" title="评级结果详情" width="800px" @closed="handleClosed">
    <el-descriptions v-if="result" :column="2" border>
      <el-descriptions-item label="评级名称">
        <div class="rating-name-cell">
          <el-icon v-if="result.icon" :style="{ color: result.color }">
            <component :is="result.icon" />
          </el-icon>
          <span>{{ result.ratingName }}</span>
        </div>
      </el-descriptions-item>

      <el-descriptions-item label="周期类型">
        {{ result.periodTypeText }}
      </el-descriptions-item>

      <el-descriptions-item label="班级">
        {{ result.className }}
      </el-descriptions-item>

      <el-descriptions-item label="评级周期">
        {{ result.periodText }}
      </el-descriptions-item>

      <el-descriptions-item label="排名">
        <el-tag type="warning">第{{ result.ranking }}名</el-tag>
      </el-descriptions-item>

      <el-descriptions-item label="最终得分">
        <span class="score-value">{{ result.finalScore }}</span>
      </el-descriptions-item>

      <el-descriptions-item label="获奖状态">
        <el-tag :type="result.awarded === 1 ? 'success' : 'info'">
          {{ result.awarded === 1 ? '已获奖' : '未获奖' }}
        </el-tag>
      </el-descriptions-item>

      <el-descriptions-item label="状态">
        <el-tag :type="getStatusTag(result.status)">
          {{ result.statusText }}
        </el-tag>
      </el-descriptions-item>

      <el-descriptions-item label="计算时间" :span="2">
        {{ result.calculatedAt }}
      </el-descriptions-item>

      <el-descriptions-item v-if="result.approvedBy" label="审核人" :span="2">
        {{ result.approvedByName }} ({{ result.approvedAt }})
      </el-descriptions-item>

      <el-descriptions-item v-if="result.publishedBy" label="发布人" :span="2">
        {{ result.publishedByName }} ({{ result.publishedAt }})
      </el-descriptions-item>
    </el-descriptions>

    <el-skeleton v-else :rows="10" animated />

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getRatingResultDetail, type RatingResultVO } from '@/api/checkPlanRatingResult'

// Props
const props = defineProps<{
  modelValue: boolean
  resultId?: number
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

// 响应式数据
const visible = ref(false)
const result = ref<RatingResultVO | null>(null)

// 监听visible变化
watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val && props.resultId) {
      loadDetail()
    }
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 加载详情
const loadDetail = async () => {
  if (!props.resultId) return

  try {
    const res = await getRatingResultDetail(props.resultId)
    result.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '加载详情失败')
  }
}

// 获取状态标签
const getStatusTag = (status: string) => {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PENDING_APPROVAL: 'warning',
    PUBLISHED: 'success'
  }
  return map[status] || ''
}

// 关闭回调
const handleClosed = () => {
  result.value = null
}
</script>

<style scoped lang="scss">
.rating-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .el-icon {
    font-size: 18px;
  }
}

.score-value {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}
</style>
