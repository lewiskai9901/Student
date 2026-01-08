<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑徽章' : '创建徽章'"
    width="700px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item label="徽章名称" prop="badgeName">
        <el-input
          v-model="form.badgeName"
          placeholder="如：卫生标兵班级、纪律模范班级"
          maxlength="50"
        />
      </el-form-item>

      <el-form-item label="徽章等级" prop="badgeLevel">
        <el-radio-group v-model="form.badgeLevel">
          <el-radio-button value="GOLD">
            <span :style="{ color: BADGE_LEVEL_COLORS.GOLD }">🥇 金牌</span>
          </el-radio-button>
          <el-radio-button value="SILVER">
            <span :style="{ color: BADGE_LEVEL_COLORS.SILVER }">🥈 银牌</span>
          </el-radio-button>
          <el-radio-button value="BRONZE">
            <span :style="{ color: BADGE_LEVEL_COLORS.BRONZE }">🥉 铜牌</span>
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="评级规则" prop="ruleId">
        <el-select
          v-model="form.ruleId"
          placeholder="选择评级规则"
          style="width: 100%"
          @change="loadRatingLevels"
        >
          <el-option
            v-for="rule in ratingRules"
            :key="rule.id"
            :label="rule.ruleName"
            :value="rule.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="条件类型" prop="conditionType">
        <el-radio-group v-model="form.conditionType">
          <el-radio value="FREQUENCY">频次条件</el-radio>
          <el-radio value="CONSECUTIVE">连续条件</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 频次条件 -->
      <template v-if="form.conditionType === 'FREQUENCY'">
        <el-form-item label="评级等级" prop="levelId">
          <el-select
            v-model="form.levelId"
            placeholder="选择评级等级"
            style="width: 100%"
          >
            <el-option
              v-for="level in ratingLevels"
              :key="level.id"
              :label="level.levelName"
              :value="level.id"
            >
              <span :style="{ color: level.color }">{{ level.levelName }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="频次阈值" prop="frequencyThreshold">
          <el-input-number
            v-model="form.frequencyThreshold"
            :min="1"
            :max="100"
            placeholder="获得次数"
          />
          <span class="form-tip">次以上</span>
        </el-form-item>

        <el-form-item label="统计周期" prop="periodType">
          <el-select v-model="form.periodType" placeholder="选择周期类型">
            <el-option label="周" value="WEEK" />
            <el-option label="月" value="MONTH" />
            <el-option label="学期" value="SEMESTER" />
          </el-select>
        </el-form-item>
      </template>

      <!-- 连续条件 -->
      <template v-if="form.conditionType === 'CONSECUTIVE'">
        <el-form-item label="评级等级" prop="levelId">
          <el-select
            v-model="form.levelId"
            placeholder="选择评级等级"
            style="width: 100%"
          >
            <el-option
              v-for="level in ratingLevels"
              :key="level.id"
              :label="level.levelName"
              :value="level.id"
            >
              <span :style="{ color: level.color }">{{ level.levelName }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="连续阈值" prop="consecutiveThreshold">
          <el-input-number
            v-model="form.consecutiveThreshold"
            :min="1"
            :max="50"
            placeholder="连续次数"
          />
          <span class="form-tip">次</span>
        </el-form-item>

        <el-form-item label="统计周期" prop="periodType">
          <el-select v-model="form.periodType" placeholder="选择周期类型">
            <el-option label="周" value="WEEK" />
            <el-option label="月" value="MONTH" />
          </el-select>
        </el-form-item>
      </template>

      <el-form-item label="徽章描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="简要描述此徽章的意义和授予标准"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="启用状态">
        <el-switch v-model="form.enabled" />
        <span class="form-tip">启用后徽章才会生效</span>
      </el-form-item>

      <el-form-item label="自动授予">
        <el-switch v-model="form.autoGrant" />
        <span class="form-tip">自动授予时，系统会定期检查并授予符合条件的班级</span>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEdit ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  createBadge,
  updateBadge,
  type RatingBadgeVO,
  type RatingBadgeCreateDTO,
  BADGE_LEVEL_COLORS
} from '@/api/ratingBadge'
import { getRatingRulesByPlan } from '@/api/checkPlanRating'
import { getRatingLevelsByRule } from '@/api/checkPlanRating'

// Props
const props = defineProps<{
  modelValue: boolean
  badge: RatingBadgeVO | null
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
const submitting = ref(false)

const ratingRules = ref<any[]>([])
const ratingLevels = ref<any[]>([])

// 表单数据
const form = reactive({
  badgeName: '',
  badgeLevel: 'GOLD',
  ruleId: undefined as number | undefined,
  conditionType: 'FREQUENCY',
  levelId: undefined as number | undefined,
  frequencyThreshold: 10,
  consecutiveThreshold: 3,
  periodType: 'MONTH',
  description: '',
  enabled: true,
  autoGrant: true
})

// 是否编辑模式
const isEdit = computed(() => !!props.badge)

// 表单验证规则
const rules: FormRules = {
  badgeName: [
    { required: true, message: '请输入徽章名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  badgeLevel: [{ required: true, message: '请选择徽章等级', trigger: 'change' }],
  ruleId: [{ required: true, message: '请选择评级规则', trigger: 'change' }],
  conditionType: [{ required: true, message: '请选择条件类型', trigger: 'change' }],
  levelId: [{ required: true, message: '请选择评级等级', trigger: 'change' }],
  frequencyThreshold: [
    { required: true, message: '请输入频次阈值', trigger: 'blur' }
  ],
  consecutiveThreshold: [
    { required: true, message: '请输入连续阈值', trigger: 'blur' }
  ],
  periodType: [{ required: true, message: '请选择统计周期', trigger: 'change' }]
}

// 监听visible变化
watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val) {
      loadRatingRules()
      if (props.badge) {
        // 编辑模式，填充数据
        form.badgeName = props.badge.badgeName
        form.badgeLevel = props.badge.badgeLevel as any
        form.ruleId = props.badge.ruleId as number
        form.description = props.badge.description || ''
        form.enabled = props.badge.enabled
        form.autoGrant = props.badge.autoGrant

        // 解析条件
        const condition = props.badge.grantCondition
        form.conditionType = condition.conditionType
        form.levelId = condition.levelId as number
        form.periodType = condition.periodType || 'MONTH'

        if (condition.conditionType === 'FREQUENCY') {
          form.frequencyThreshold = condition.frequencyThreshold || 10
        } else if (condition.conditionType === 'CONSECUTIVE') {
          form.consecutiveThreshold = condition.consecutiveThreshold || 3
        }

        loadRatingLevels()
      } else {
        // 创建模式，重置表单
        resetForm()
      }
    }
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 加载评级规则
const loadRatingRules = async () => {
  if (!props.checkPlanId) return

  try {
    const res = await getRatingRulesByPlan(props.checkPlanId)
    ratingRules.value = res.data || []
  } catch (error) {
    console.error('加载评级规则失败:', error)
  }
}

// 加载评级等级
const loadRatingLevels = async () => {
  if (!form.ruleId) return

  try {
    const res = await getRatingLevelsByRule(form.ruleId)
    ratingLevels.value = res.data || []
  } catch (error) {
    console.error('加载评级等级失败:', error)
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    const data: RatingBadgeCreateDTO = {
      badgeName: form.badgeName,
      badgeLevel: form.badgeLevel as any,
      ruleId: form.ruleId!,
      grantCondition: {
        conditionType: form.conditionType as any,
        levelId: form.levelId!,
        periodType: form.periodType
      },
      description: form.description,
      enabled: form.enabled,
      autoGrant: form.autoGrant
    }

    if (form.conditionType === 'FREQUENCY') {
      data.grantCondition.frequencyThreshold = form.frequencyThreshold
    } else if (form.conditionType === 'CONSECUTIVE') {
      data.grantCondition.consecutiveThreshold = form.consecutiveThreshold
    }

    submitting.value = true

    if (isEdit.value) {
      await updateBadge(props.badge!.id, data)
      ElMessage.success('徽章更新成功')
    } else {
      await createBadge(data)
      ElMessage.success('徽章创建成功')
    }

    visible.value = false
    emit('success')
  } catch (error: any) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  form.badgeName = ''
  form.badgeLevel = 'GOLD'
  form.ruleId = undefined
  form.conditionType = 'FREQUENCY'
  form.levelId = undefined
  form.frequencyThreshold = 10
  form.consecutiveThreshold = 3
  form.periodType = 'MONTH'
  form.description = ''
  form.enabled = true
  form.autoGrant = true

  formRef.value?.clearValidate()
}

// 对话框关闭回调
const handleClosed = () => {
  resetForm()
}
</script>

<style scoped lang="scss">
.form-tip {
  margin-left: 10px;
  font-size: 13px;
  color: #909399;
}
</style>
