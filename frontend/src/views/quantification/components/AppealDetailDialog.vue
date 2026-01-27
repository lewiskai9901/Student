<template>
  <el-dialog
    v-model="visible"
    title="申诉详情"
    width="700px"
    @close="handleClose"
  >
    <el-descriptions v-if="appeal" :column="2" border>
      <!-- 基本信息 -->
      <el-descriptions-item label="申诉ID">
        {{ appeal.id }}
      </el-descriptions-item>

      <el-descriptions-item label="状态">
        <el-tag :type="getStatusType(appeal.status)">
          {{ getStatusName(appeal.status) }}
        </el-tag>
      </el-descriptions-item>

      <el-descriptions-item label="班级">
        {{ appeal.className }}
      </el-descriptions-item>

      <el-descriptions-item label="检查类别">
        {{ appeal.categoryName }}
      </el-descriptions-item>

      <el-descriptions-item label="扣分项" :span="2">
        {{ appeal.deductionItemName }}
      </el-descriptions-item>

      <!-- 扣分信息 -->
      <el-descriptions-item label="原扣分">
        <span class="original-score">-{{ appeal.originalScore }}</span>
      </el-descriptions-item>

      <el-descriptions-item label="修订扣分">
        <span v-if="appeal.revisedScore !== null && appeal.revisedScore !== undefined" class="revised-score">
          -{{ appeal.revisedScore }}
        </span>
        <span v-else>-</span>
      </el-descriptions-item>

      <!-- 申诉信息 -->
      <el-descriptions-item label="申诉人">
        {{ appeal.appealUserName }}
      </el-descriptions-item>

      <el-descriptions-item label="申诉时间">
        {{ appeal.appealTime }}
      </el-descriptions-item>

      <el-descriptions-item label="申诉理由" :span="2">
        <div class="appeal-reason">
          {{ appeal.appealReason }}
        </div>
      </el-descriptions-item>

      <el-descriptions-item label="申诉图片" :span="2" v-if="appeal.appealPhotoUrls">
        <div class="photo-list">
          <el-image
            v-for="(url, index) in getPhotoUrls(appeal.appealPhotoUrls)"
            :key="index"
            :src="url"
            :preview-src-list="getPhotoUrls(appeal.appealPhotoUrls)"
            :initial-index="index"
            fit="cover"
            style="width: 100px; height: 100px; margin-right: 10px; border-radius: 4px;"
          />
        </div>
      </el-descriptions-item>

      <!-- 审核信息 -->
      <template v-if="appeal.reviewerName">
        <el-descriptions-item label="审核人">
          {{ appeal.reviewerName }}
        </el-descriptions-item>

        <el-descriptions-item label="审核时间">
          {{ appeal.reviewTime }}
        </el-descriptions-item>

        <el-descriptions-item label="审核意见" :span="2" v-if="appeal.reviewOpinion">
          <div class="review-opinion">
            {{ appeal.reviewOpinion }}
          </div>
        </el-descriptions-item>
      </template>
    </el-descriptions>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AppealResponse } from '@/api/quantification-extra'

const props = defineProps<{
  modelValue: boolean
  appeal: AppealResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 获取状态标签类型
const getStatusType = (status: number) => {
  const typeMap: Record<number, any> = {
    0: 'warning',  // 待处理
    1: 'primary',  // 处理中
    2: 'success',  // 通过
    3: 'danger'    // 驳回
  }
  return typeMap[status] || 'info'
}

// 获取状态名称
const getStatusName = (status: number) => {
  const nameMap: Record<number, string> = {
    0: '待处理',
    1: '处理中',
    2: '通过',
    3: '驳回'
  }
  return nameMap[status] || '未知'
}

// 获取图片URL列表
const getPhotoUrls = (photoUrls: string) => {
  if (!photoUrls) return []
  return photoUrls.split(',').filter(url => url.trim())
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
}
</script>

<style scoped lang="scss">
.original-score {
  color: #f56c6c;
  font-weight: 600;
  font-size: 16px;
}

.revised-score {
  color: #67c23a;
  font-weight: 600;
  font-size: 16px;
}

.appeal-reason,
.review-opinion {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.photo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
