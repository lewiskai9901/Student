<template>
  <el-dialog
    v-model="visible"
    title="自动授予徽章"
    width="600px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <el-alert
      title="自动授予说明"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 20px"
    >
      <template #default>
        系统将根据徽章配置的条件，自动检查并授予符合条件的班级。请选择统计周期。
      </template>
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="统计周期" prop="dateRange">
        <el-date-picker
          v-model="form.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <!-- 预览结果 -->
    <div v-if="grantResults.length > 0" class="grant-results">
      <el-divider>授予结果</el-divider>
      <el-scrollbar max-height="300px">
        <div class="result-list">
          <div
            v-for="(result, index) in grantResults"
            :key="index"
            class="result-item"
            :class="{ success: result.success, error: !result.success }"
          >
            <el-icon v-if="result.success" :size="18" color="#67C23A">
              <CircleCheck />
            </el-icon>
            <el-icon v-else :size="18" color="#F56C6C">
              <CircleClose />
            </el-icon>
            <div class="result-info">
              <p class="result-title">
                <strong>{{ result.className }}</strong> - {{ result.badgeName }}
              </p>
              <p class="result-message">{{ result.message }}</p>
            </div>
          </div>
        </div>
      </el-scrollbar>
    </div>

    <template #footer>
      <el-button @click="visible = false">
        {{ grantResults.length > 0 ? '关闭' : '取消' }}
      </el-button>
      <el-button
        v-if="grantResults.length === 0"
        type="primary"
        :loading="granting"
        @click="handleGrant"
      >
        开始授予
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { checkAndGrantBadges, type BadgeGrantResultVO } from '@/api/v2/rating'

// Props
const props = defineProps<{
  modelValue: boolean
  checkPlanId?: number
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

// 响应式数据
const formRef = ref<FormInstance>()
const visible = ref(false)
const granting = ref(false)

const grantResults = ref<BadgeGrantResultVO[]>([])

// 表单数据
const form = reactive({
  dateRange: [] as string[]
})

// 表单验证规则
const rules: FormRules = {
  dateRange: [{ required: true, message: '请选择统计周期', trigger: 'change' }]
}

// 监听visible变化
watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val) {
      // 默认设置最近3个月
      const now = new Date()
      const periodEnd = now.toISOString().split('T')[0]
      const periodStart = new Date(now.getFullYear(), now.getMonth() - 3, 1)
        .toISOString()
        .split('T')[0]
      form.dateRange = [periodStart, periodEnd]
    }
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 授予徽章
const handleGrant = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    if (!props.checkPlanId) {
      ElMessage.warning('请先选择检查计划')
      return
    }

    granting.value = true

    const res = await checkAndGrantBadges({
      checkPlanId: props.checkPlanId,
      periodStart: form.dateRange[0],
      periodEnd: form.dateRange[1]
    })

    grantResults.value = res.data || []

    const successCount = grantResults.value.filter((r) => r.success).length
    const failCount = grantResults.value.length - successCount

    if (failCount === 0) {
      ElMessage.success(`自动授予完成！成功授予 ${successCount} 个徽章`)
    } else {
      ElMessage.warning(
        `自动授予完成！成功 ${successCount} 个，失败 ${failCount} 个`
      )
    }

    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '自动授予失败')
  } finally {
    granting.value = false
  }
}

// 重置表单
const resetForm = () => {
  form.dateRange = []
  grantResults.value = []
  formRef.value?.clearValidate()
}

// 对话框关闭回调
const handleClosed = () => {
  resetForm()
}
</script>

<style scoped lang="scss">
.grant-results {
  margin-top: 20px;

  .result-list {
    .result-item {
      display: flex;
      gap: 12px;
      padding: 12px;
      margin-bottom: 10px;
      border-radius: 6px;
      background: #f5f7fa;

      &.success {
        background: #f0f9ff;
        border: 1px solid #b3e19d;
      }

      &.error {
        background: #fef0f0;
        border: 1px solid #fbc4c4;
      }

      .result-info {
        flex: 1;

        .result-title {
          margin: 0 0 4px 0;
          font-size: 14px;
          color: #303133;
        }

        .result-message {
          margin: 0;
          font-size: 13px;
          color: #909399;
        }
      }
    }
  }
}
</style>
