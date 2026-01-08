<template>
  <view class="appeal-create">
    <!-- 加载状态 -->
    <view v-if="loading" class="loading-container">
      <view class="loading-spinner"></view>
      <text class="loading-text">加载中...</text>
    </view>

    <template v-else>
      <!-- 不可申诉提示 -->
      <view v-if="!canAppeal" class="cannot-appeal">
        <view class="tip-icon">&#128683;</view>
        <text class="tip-title">无法提交申诉</text>
        <text class="tip-reason">{{ cannotAppealReason }}</text>
        <view class="back-btn" @click="goBack">
          <text>返回</text>
        </view>
      </view>

      <!-- 申诉表单 -->
      <template v-else>
        <!-- 原扣分信息 -->
        <view class="info-card">
          <view class="card-title">扣分信息</view>
          <view class="deduction-info">
            <view class="info-row">
              <text class="label">检查记录</text>
              <text class="value">{{ originalInfo.recordName || '-' }}</text>
            </view>
            <view class="info-row">
              <text class="label">所属班级</text>
              <text class="value">{{ originalInfo.className || '-' }}</text>
            </view>
            <view class="info-row">
              <text class="label">检查类别</text>
              <text class="value">{{ originalInfo.categoryName || '-' }}</text>
            </view>
            <view class="info-row">
              <text class="label">扣分项目</text>
              <text class="value">{{ originalInfo.itemName || '-' }}</text>
            </view>
            <view class="info-row">
              <text class="label">原扣分</text>
              <text class="value negative">-{{ originalInfo.score || 0 }}分</text>
            </view>
          </view>
        </view>

        <!-- 申诉表单 -->
        <view class="form-card">
          <view class="card-title">填写申诉</view>

          <!-- 申诉类型 -->
          <view class="form-item">
            <view class="form-label required">申诉类型</view>
            <view class="type-selector">
              <view
                class="type-option"
                :class="{ active: form.appealType === AppealType.SCORE }"
                @click="form.appealType = AppealType.SCORE"
              >
                <text class="type-icon">&#128178;</text>
                <text class="type-name">分数异议</text>
                <text class="type-desc">对扣分分值有异议</text>
              </view>
              <view
                class="type-option"
                :class="{ active: form.appealType === AppealType.FACT }"
                @click="form.appealType = AppealType.FACT"
              >
                <text class="type-icon">&#128269;</text>
                <text class="type-name">事实异议</text>
                <text class="type-desc">对扣分事实有异议</text>
              </view>
              <view
                class="type-option"
                :class="{ active: form.appealType === AppealType.PROCEDURE }"
                @click="form.appealType = AppealType.PROCEDURE"
              >
                <text class="type-icon">&#128203;</text>
                <text class="type-name">程序异议</text>
                <text class="type-desc">对检查程序有异议</text>
              </view>
            </view>
          </view>

          <!-- 申诉理由 -->
          <view class="form-item">
            <view class="form-label required">申诉理由</view>
            <textarea
              class="form-textarea"
              v-model="form.appealReason"
              placeholder="请详细描述您的申诉理由..."
              :maxlength="500"
            />
            <text class="char-count">{{ form.appealReason.length }}/500</text>
          </view>

          <!-- 期望分数 -->
          <view class="form-item" v-if="form.appealType === AppealType.SCORE">
            <view class="form-label">期望调整后分数</view>
            <view class="score-input">
              <text class="score-prefix">-</text>
              <input
                type="number"
                v-model="form.expectedScore"
                placeholder="0"
                class="score-field"
              />
              <text class="score-suffix">分</text>
            </view>
            <text class="form-hint">请填写您认为合理的扣分值（0表示不应扣分）</text>
          </view>

          <!-- 证据上传 -->
          <view class="form-item">
            <view class="form-label">上传证据</view>
            <view class="upload-section">
              <view
                class="upload-item"
                v-for="(url, index) in form.evidenceUrls"
                :key="index"
              >
                <image :src="url" mode="aspectFill" />
                <view class="remove-btn" @click="removeImage(index)">
                  <text>&#10005;</text>
                </view>
              </view>
              <view
                class="upload-btn"
                v-if="form.evidenceUrls.length < 5"
                @click="chooseImage"
              >
                <text class="upload-icon">&#43;</text>
                <text class="upload-text">上传图片</text>
              </view>
            </view>
            <text class="form-hint">最多上传5张图片，支持jpg、png格式</text>
          </view>
        </view>

        <!-- 提交按钮 -->
        <view class="submit-section">
          <view class="submit-btn" :class="{ disabled: submitting }" @click="handleSubmit">
            <text>{{ submitting ? '提交中...' : '提交申诉' }}</text>
          </view>
          <text class="submit-tip">提交后请耐心等待审核，审核结果将以消息通知</text>
        </view>
      </template>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  checkCanAppeal,
  createAppeal,
  AppealType,
  type CreateAppealRequest
} from '@/api/appeal'

// URL参数
const recordId = ref<string>('')
const itemId = ref<string>('')

// 加载状态
const loading = ref(true)
const submitting = ref(false)

// 是否可以申诉
const canAppeal = ref(true)
const cannotAppealReason = ref('')

// 原扣分信息
const originalInfo = reactive({
  recordName: '',
  className: '',
  categoryName: '',
  itemName: '',
  score: 0
})

// 表单数据
const form = reactive<{
  appealType: AppealType
  appealReason: string
  expectedScore: string
  evidenceUrls: string[]
}>({
  appealType: AppealType.SCORE,
  appealReason: '',
  expectedScore: '',
  evidenceUrls: []
})

// 页面加载
onLoad((options) => {
  if (options?.recordId && options?.itemId) {
    recordId.value = options.recordId
    itemId.value = options.itemId
    checkAppealable()
  } else {
    loading.value = false
    canAppeal.value = false
    cannotAppealReason.value = '参数错误，请返回重试'
  }
})

// 检查是否可以申诉
async function checkAppealable() {
  loading.value = true
  try {
    const res = await checkCanAppeal(itemId.value)
    if (res.code === 200) {
      canAppeal.value = res.data.canAppeal
      if (!res.data.canAppeal) {
        cannotAppealReason.value = res.data.reason || '该项目不可申诉'
      } else {
        // TODO: 加载原扣分信息（可以从API获取或从页面参数传递）
        // 这里先用模拟数据
        originalInfo.recordName = '日常检查'
        originalInfo.className = '高一(1)班'
        originalInfo.categoryName = '卫生类'
        originalInfo.itemName = '地面有垃圾'
        originalInfo.score = 2
      }
    } else {
      canAppeal.value = false
      cannotAppealReason.value = res.message || '检查失败'
    }
  } catch (error) {
    console.error('检查申诉状态失败:', error)
    canAppeal.value = false
    cannotAppealReason.value = '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 选择图片
function chooseImage() {
  uni.chooseImage({
    count: 5 - form.evidenceUrls.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      // 这里应该上传到服务器，暂时直接使用本地路径
      form.evidenceUrls.push(...res.tempFilePaths)
    }
  })
}

// 移除图片
function removeImage(index: number) {
  form.evidenceUrls.splice(index, 1)
}

// 提交申诉
async function handleSubmit() {
  // 表单验证
  if (!form.appealReason.trim()) {
    uni.showToast({
      title: '请填写申诉理由',
      icon: 'none'
    })
    return
  }

  if (form.appealReason.length < 10) {
    uni.showToast({
      title: '申诉理由至少10个字',
      icon: 'none'
    })
    return
  }

  // 构建请求数据
  const data: CreateAppealRequest = {
    recordId: recordId.value,
    itemId: itemId.value,
    appealType: form.appealType,
    appealReason: form.appealReason.trim()
  }

  if (form.appealType === AppealType.SCORE && form.expectedScore) {
    data.expectedScore = parseInt(form.expectedScore)
  }

  if (form.evidenceUrls.length > 0) {
    data.evidenceUrls = form.evidenceUrls
  }

  submitting.value = true
  try {
    const res = await createAppeal(data)
    if (res.code === 200) {
      uni.showToast({
        title: '提交成功',
        icon: 'success'
      })
      // 跳转到详情页
      setTimeout(() => {
        uni.redirectTo({
          url: `/pages/appeal/detail?id=${res.data.id}`
        })
      }, 1500)
    } else {
      uni.showToast({
        title: res.message || '提交失败',
        icon: 'none'
      })
    }
  } catch (error) {
    console.error('提交申诉失败:', error)
    uni.showToast({
      title: '网络错误',
      icon: 'none'
    })
  } finally {
    submitting.value = false
  }
}

// 返回上一页
function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.appeal-create {
  min-height: 100vh;
  background: #f5f6fa;
  padding: 24rpx;
  box-sizing: border-box;
}

// 加载状态
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 0;
}

.loading-spinner {
  width: 60rpx;
  height: 60rpx;
  border: 4rpx solid #e0e0e0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  margin-top: 20rpx;
  color: #999;
  font-size: 28rpx;
}

// 不可申诉提示
.cannot-appeal {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 40rpx;
  background: #fff;
  border-radius: 20rpx;
  margin-top: 100rpx;

  .tip-icon {
    font-size: 100rpx;
    margin-bottom: 30rpx;
  }

  .tip-title {
    font-size: 36rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 16rpx;
  }

  .tip-reason {
    font-size: 28rpx;
    color: #999;
    text-align: center;
    margin-bottom: 40rpx;
  }

  .back-btn {
    padding: 20rpx 60rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 30rpx;

    text {
      font-size: 30rpx;
      color: #fff;
    }
  }
}

// 信息卡片
.info-card,
.form-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.card-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
  margin-bottom: 20rpx;
}

// 扣分信息
.deduction-info {
  .info-row {
    display: flex;
    justify-content: space-between;
    padding: 16rpx 0;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .label {
      font-size: 28rpx;
      color: #999;
    }

    .value {
      font-size: 28rpx;
      color: #333;

      &.negative {
        color: #f44336;
        font-weight: 500;
      }
    }
  }
}

// 表单项
.form-item {
  margin-bottom: 30rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.form-label {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 16rpx;
  display: flex;
  align-items: center;

  &.required::before {
    content: '*';
    color: #f44336;
    margin-right: 8rpx;
  }
}

// 类型选择器
.type-selector {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.type-option {
  display: flex;
  align-items: center;
  padding: 24rpx;
  background: #f9f9f9;
  border-radius: 16rpx;
  border: 2rpx solid transparent;
  transition: all 0.3s;

  &.active {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
    border-color: #667eea;
  }

  .type-icon {
    font-size: 40rpx;
    margin-right: 20rpx;
  }

  .type-name {
    font-size: 30rpx;
    font-weight: 500;
    color: #333;
  }

  .type-desc {
    margin-left: auto;
    font-size: 24rpx;
    color: #999;
  }
}

// 文本域
.form-textarea {
  width: 100%;
  height: 200rpx;
  padding: 20rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

.char-count {
  display: block;
  text-align: right;
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

// 分数输入
.score-input {
  display: flex;
  align-items: center;
  padding: 20rpx;
  background: #f9f9f9;
  border-radius: 12rpx;

  .score-prefix {
    font-size: 36rpx;
    font-weight: 600;
    color: #f44336;
    margin-right: 8rpx;
  }

  .score-field {
    flex: 1;
    font-size: 36rpx;
    font-weight: 600;
    color: #333;
    text-align: center;
  }

  .score-suffix {
    font-size: 28rpx;
    color: #999;
  }
}

.form-hint {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-top: 12rpx;
}

// 上传区域
.upload-section {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.upload-item {
  position: relative;
  width: 160rpx;
  height: 160rpx;

  image {
    width: 100%;
    height: 100%;
    border-radius: 12rpx;
    object-fit: cover;
  }

  .remove-btn {
    position: absolute;
    top: -12rpx;
    right: -12rpx;
    width: 40rpx;
    height: 40rpx;
    background: #f44336;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;

    text {
      font-size: 24rpx;
      color: #fff;
    }
  }
}

.upload-btn {
  width: 160rpx;
  height: 160rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f9f9f9;
  border: 2rpx dashed #ddd;
  border-radius: 12rpx;

  &:active {
    background: #f0f0f0;
  }

  .upload-icon {
    font-size: 48rpx;
    color: #999;
  }

  .upload-text {
    font-size: 22rpx;
    color: #999;
    margin-top: 8rpx;
  }
}

// 提交区域
.submit-section {
  margin-top: 40rpx;
  padding-bottom: 40rpx;
}

.submit-btn {
  padding: 32rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20rpx;
  text-align: center;

  text {
    font-size: 34rpx;
    font-weight: 500;
    color: #fff;
  }

  &.disabled {
    opacity: 0.6;
  }

  &:active:not(.disabled) {
    opacity: 0.9;
  }
}

.submit-tip {
  display: block;
  text-align: center;
  font-size: 24rpx;
  color: #999;
  margin-top: 20rpx;
}
</style>
