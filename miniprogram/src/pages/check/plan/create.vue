<template>
  <view class="page">
    <!-- 顶部安全区 -->
    <view class="safe-top"></view>

    <!-- 导航栏 -->
    <view class="nav-bar">
      <view class="nav-back" @click="goBack">
        <u-icon name="arrow-left" size="20" color="#111827" />
      </view>
      <text class="nav-title">创建检查计划</text>
      <view class="nav-placeholder"></view>
    </view>

    <!-- 表单内容 -->
    <scroll-view scroll-y class="form-content">
      <!-- 基本信息 -->
      <view class="form-section">
        <view class="section-header">
          <text class="section-title">基本信息</text>
        </view>

        <view class="form-item">
          <text class="form-label required">计划名称</text>
          <input
            type="text"
            v-model="formData.planName"
            placeholder="请输入计划名称"
            class="form-input"
          />
        </view>

        <view class="form-item">
          <text class="form-label">计划编码</text>
          <input
            type="text"
            v-model="formData.planCode"
            placeholder="自动生成或手动输入"
            class="form-input"
          />
        </view>

        <view class="form-item" @click="showTemplatePicker = true">
          <text class="form-label required">检查模板</text>
          <view class="form-select">
            <text v-if="selectedTemplate" class="select-value">{{ selectedTemplate.templateName }}</text>
            <text v-else class="select-placeholder">请选择检查模板</text>
            <u-icon name="arrow-right" size="16" color="#9CA3AF" />
          </view>
        </view>
      </view>

      <!-- 时间设置 -->
      <view class="form-section">
        <view class="section-header">
          <text class="section-title">时间设置</text>
        </view>

        <view class="form-item" @click="openDatePicker('start')">
          <text class="form-label required">开始日期</text>
          <view class="form-select">
            <text v-if="formData.startDate" class="select-value">{{ formData.startDate }}</text>
            <text v-else class="select-placeholder">请选择开始日期</text>
            <u-icon name="calendar" size="16" color="#9CA3AF" />
          </view>
        </view>

        <view class="form-item" @click="openDatePicker('end')">
          <text class="form-label required">结束日期</text>
          <view class="form-select">
            <text v-if="formData.endDate" class="select-value">{{ formData.endDate }}</text>
            <text v-else class="select-placeholder">请选择结束日期</text>
            <u-icon name="calendar" size="16" color="#9CA3AF" />
          </view>
        </view>
      </view>

      <!-- 检查范围 -->
      <view class="form-section">
        <view class="section-header">
          <text class="section-title">检查范围</text>
        </view>

        <view class="form-item" @click="showDepartmentPicker = true">
          <text class="form-label">检查部门</text>
          <view class="form-select">
            <text v-if="selectedDepartments.length" class="select-value">
              已选 {{ selectedDepartments.length }} 个部门
            </text>
            <text v-else class="select-placeholder">全部部门（默认）</text>
            <u-icon name="arrow-right" size="16" color="#9CA3AF" />
          </view>
        </view>

        <view class="form-item" @click="showGradePicker = true">
          <text class="form-label">检查年级</text>
          <view class="form-select">
            <text v-if="selectedGrades.length" class="select-value">
              已选 {{ selectedGrades.length }} 个年级
            </text>
            <text v-else class="select-placeholder">全部年级（默认）</text>
            <u-icon name="arrow-right" size="16" color="#9CA3AF" />
          </view>
        </view>
      </view>

      <!-- 高级设置 -->
      <view class="form-section">
        <view class="section-header">
          <text class="section-title">高级设置</text>
        </view>

        <view class="form-item switch-item">
          <view class="switch-info">
            <text class="form-label">启用加权配置</text>
            <text class="form-hint">启用后将按配置的权重计算得分</text>
          </view>
          <switch
            :checked="formData.enableWeight === 1"
            @change="formData.enableWeight = $event.detail.value ? 1 : 0"
            color="#4F46E5"
          />
        </view>

        <view class="form-item switch-item">
          <view class="switch-info">
            <text class="form-label">启用评级配置</text>
            <text class="form-hint">启用后将自动进行等级评定</text>
          </view>
          <switch
            :checked="formData.enableRating === 1"
            @change="formData.enableRating = $event.detail.value ? 1 : 0"
            color="#4F46E5"
          />
        </view>

        <view class="form-item">
          <text class="form-label">备注说明</text>
          <textarea
            v-model="formData.remark"
            placeholder="请输入备注说明（选填）"
            class="form-textarea"
            :maxlength="500"
          />
        </view>
      </view>
    </scroll-view>

    <!-- 底部按钮 -->
    <view class="bottom-actions">
      <view class="action-btn secondary" @click="saveDraft">
        <text>保存草稿</text>
      </view>
      <view class="action-btn primary" @click="submitPlan">
        <text>提交计划</text>
      </view>
    </view>

    <!-- 模板选择器 -->
    <u-popup v-model:show="showTemplatePicker" mode="bottom" round="20">
      <view class="picker-popup">
        <view class="picker-header">
          <text class="picker-title">选择检查模板</text>
          <view class="picker-close" @click="showTemplatePicker = false">
            <u-icon name="close" size="20" color="#6B7280" />
          </view>
        </view>
        <scroll-view scroll-y class="picker-content">
          <view
            v-for="item in templates"
            :key="item.id"
            class="picker-item"
            :class="{ selected: selectedTemplate?.id === item.id }"
            @click="selectTemplate(item)"
          >
            <view class="picker-item-info">
              <text class="picker-item-name">{{ item.templateName }}</text>
              <text class="picker-item-desc">{{ item.categoryCount || 0 }}个类别 · {{ item.itemCount || 0 }}个扣分项</text>
            </view>
            <u-icon v-if="selectedTemplate?.id === item.id" name="checkmark" size="20" color="#4F46E5" />
          </view>
          <view v-if="!templates.length" class="picker-empty">
            <text>暂无可用模板</text>
          </view>
        </scroll-view>
      </view>
    </u-popup>

    <!-- 日期选择器 -->
    <u-datetime-picker
      v-model:show="showDatePicker"
      v-model="currentDate"
      mode="date"
      :min-date="minDate"
      @confirm="onDateConfirm"
    />

    <!-- 底部安全区 -->
    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getEnabledTemplates, type CheckTemplate } from '@/api/checkTemplate'
import { createCheckPlan } from '@/api/checkPlan'

// 表单数据
const formData = reactive({
  planName: '',
  planCode: '',
  templateId: null as number | null,
  startDate: '',
  endDate: '',
  departmentIds: [] as number[],
  gradeIds: [] as number[],
  enableWeight: 0,
  enableRating: 1,
  remark: ''
})

// 选择器状态
const showTemplatePicker = ref(false)
const showDatePicker = ref(false)
const showDepartmentPicker = ref(false)
const showGradePicker = ref(false)
const datePickerType = ref<'start' | 'end'>('start')
const currentDate = ref(Date.now())
const minDate = Date.now() - 30 * 24 * 60 * 60 * 1000

// 数据
const templates = ref<CheckTemplate[]>([])
const selectedTemplate = ref<CheckTemplate | null>(null)
const selectedDepartments = ref<any[]>([])
const selectedGrades = ref<any[]>([])

// 初始化
onMounted(() => {
  loadTemplates()
  generatePlanCode()
})

// 加载模板列表
async function loadTemplates() {
  try {
    const data = await getEnabledTemplates()
    templates.value = data || []
  } catch (error) {
    console.error('加载模板列表失败:', error)
    // 模拟数据
    templates.value = [
      {
        id: 1,
        templateCode: 'TPL001',
        templateName: '日常卫生检查模板',
        templateType: 1,
        categoryCount: 5,
        itemCount: 25,
        totalScore: 100,
        status: 1
      },
      {
        id: 2,
        templateCode: 'TPL002',
        templateName: '专项安全检查模板',
        templateType: 2,
        categoryCount: 3,
        itemCount: 15,
        totalScore: 100,
        status: 1
      }
    ]
  }
}

// 生成计划编码
function generatePlanCode() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const random = String(Math.floor(Math.random() * 1000)).padStart(3, '0')
  formData.planCode = `PLAN${year}${month}${day}${random}`
}

// 选择模板
function selectTemplate(item: CheckTemplate) {
  selectedTemplate.value = item
  formData.templateId = Number(item.id)
  showTemplatePicker.value = false
}

// 打开日期选择器
function openDatePicker(type: 'start' | 'end') {
  datePickerType.value = type
  if (type === 'start' && formData.startDate) {
    currentDate.value = new Date(formData.startDate).getTime()
  } else if (type === 'end' && formData.endDate) {
    currentDate.value = new Date(formData.endDate).getTime()
  } else {
    currentDate.value = Date.now()
  }
  showDatePicker.value = true
}

// 日期确认
function onDateConfirm(e: any) {
  const date = new Date(e.value)
  const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

  if (datePickerType.value === 'start') {
    formData.startDate = dateStr
    // 如果结束日期早于开始日期，清空结束日期
    if (formData.endDate && formData.endDate < dateStr) {
      formData.endDate = ''
    }
  } else {
    // 检查结束日期是否早于开始日期
    if (formData.startDate && dateStr < formData.startDate) {
      uni.showToast({ title: '结束日期不能早于开始日期', icon: 'none' })
      return
    }
    formData.endDate = dateStr
  }
  showDatePicker.value = false
}

// 表单验证
function validateForm(): boolean {
  if (!formData.planName.trim()) {
    uni.showToast({ title: '请输入计划名称', icon: 'none' })
    return false
  }
  if (!formData.templateId) {
    uni.showToast({ title: '请选择检查模板', icon: 'none' })
    return false
  }
  if (!formData.startDate) {
    uni.showToast({ title: '请选择开始日期', icon: 'none' })
    return false
  }
  if (!formData.endDate) {
    uni.showToast({ title: '请选择结束日期', icon: 'none' })
    return false
  }
  return true
}

// 保存草稿
async function saveDraft() {
  if (!formData.planName.trim()) {
    uni.showToast({ title: '请至少输入计划名称', icon: 'none' })
    return
  }

  uni.showLoading({ title: '保存中...' })
  try {
    await createCheckPlan({
      ...formData,
      status: 0 // 草稿状态
    })
    uni.hideLoading()
    uni.showToast({ title: '草稿已保存', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (error) {
    uni.hideLoading()
    console.error('保存草稿失败:', error)
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

// 提交计划
async function submitPlan() {
  if (!validateForm()) return

  uni.showLoading({ title: '提交中...' })
  try {
    await createCheckPlan({
      ...formData,
      status: 1 // 进行中状态
    })
    uni.hideLoading()
    uni.showToast({ title: '计划已创建', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (error) {
    uni.hideLoading()
    console.error('提交计划失败:', error)
    uni.showToast({ title: '提交失败', icon: 'none' })
  }
}

// 返回
function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
// 变量
$primary: #4F46E5;
$primary-light: #EEF2FF;
$text-primary: #111827;
$text-secondary: #6B7280;
$text-muted: #9CA3AF;
$border: #E5E7EB;
$bg: #F9FAFB;
$white: #FFFFFF;
$danger: #DC2626;

.page {
  min-height: 100vh;
  background: $bg;
}

.safe-top {
  height: env(safe-area-inset-top);
  background: $white;
}

.safe-bottom {
  height: calc(140rpx + env(safe-area-inset-bottom));
}

// 导航栏
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 24rpx;
  background: $white;
  border-bottom: 1rpx solid $border;

  .nav-back, .nav-placeholder {
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .nav-title {
    font-size: 32rpx;
    font-weight: 600;
    color: $text-primary;
  }
}

// 表单内容
.form-content {
  height: calc(100vh - 88rpx - env(safe-area-inset-top) - 140rpx - env(safe-area-inset-bottom));
  padding: 24rpx;
}

// 表单分组
.form-section {
  background: $white;
  border-radius: 12rpx;
  border: 1rpx solid $border;
  margin-bottom: 24rpx;
  overflow: hidden;

  .section-header {
    padding: 20rpx 24rpx;
    border-bottom: 1rpx solid $border;

    .section-title {
      font-size: 28rpx;
      font-weight: 600;
      color: $text-primary;
    }
  }
}

// 表单项
.form-item {
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid $border;

  &:last-child {
    border-bottom: none;
  }

  .form-label {
    display: block;
    font-size: 26rpx;
    color: $text-secondary;
    margin-bottom: 12rpx;

    &.required::before {
      content: '*';
      color: $danger;
      margin-right: 4rpx;
    }
  }

  .form-input {
    width: 100%;
    height: 80rpx;
    font-size: 28rpx;
    color: $text-primary;
    background: $bg;
    border-radius: 8rpx;
    padding: 0 20rpx;
  }

  .form-select {
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 80rpx;
    background: $bg;
    border-radius: 8rpx;
    padding: 0 20rpx;

    .select-value {
      font-size: 28rpx;
      color: $text-primary;
    }

    .select-placeholder {
      font-size: 28rpx;
      color: $text-muted;
    }
  }

  .form-textarea {
    width: 100%;
    height: 160rpx;
    font-size: 28rpx;
    color: $text-primary;
    background: $bg;
    border-radius: 8rpx;
    padding: 16rpx 20rpx;
    line-height: 1.5;
  }

  &.switch-item {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .switch-info {
      flex: 1;

      .form-label {
        margin-bottom: 4rpx;
      }

      .form-hint {
        font-size: 24rpx;
        color: $text-muted;
      }
    }
  }
}

// 底部按钮
.bottom-actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 20rpx;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: $white;
  border-top: 1rpx solid $border;

  .action-btn {
    flex: 1;
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12rpx;
    font-size: 30rpx;
    font-weight: 500;

    &.primary {
      background: $primary;
      color: $white;
    }

    &.secondary {
      background: $bg;
      color: $text-secondary;
      border: 1rpx solid $border;
    }
  }
}

// 选择器弹窗
.picker-popup {
  .picker-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx;
    border-bottom: 1rpx solid $border;

    .picker-title {
      font-size: 32rpx;
      font-weight: 600;
      color: $text-primary;
    }

    .picker-close {
      width: 60rpx;
      height: 60rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .picker-content {
    max-height: 600rpx;
    padding: 16rpx;
  }

  .picker-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20rpx 16rpx;
    border-radius: 8rpx;
    margin-bottom: 8rpx;

    &:active, &.selected {
      background: $primary-light;
    }

    .picker-item-info {
      flex: 1;

      .picker-item-name {
        display: block;
        font-size: 28rpx;
        color: $text-primary;
        margin-bottom: 4rpx;
      }

      .picker-item-desc {
        font-size: 24rpx;
        color: $text-muted;
      }
    }
  }

  .picker-empty {
    padding: 60rpx 0;
    text-align: center;
    color: $text-muted;
    font-size: 28rpx;
  }
}
</style>
