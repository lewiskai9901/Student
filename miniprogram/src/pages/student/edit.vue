<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">{{ isEdit ? '编辑学生' : '添加学生' }}</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <!-- 表单内容 -->
    <scroll-view scroll-y class="form-content" :style="{ marginTop: navBarHeight + 'px' }">
      <view class="form-section">
        <view class="section-title">基本信息</view>

        <view class="form-item">
          <text class="label required">学号</text>
          <input
            class="input"
            v-model="form.studentNo"
            placeholder="请输入学号"
            :disabled="isEdit"
          />
        </view>

        <view class="form-item">
          <text class="label required">姓名</text>
          <input
            class="input"
            v-model="form.realName"
            placeholder="请输入姓名"
          />
        </view>

        <view class="form-item">
          <text class="label">性别</text>
          <view class="radio-group">
            <view
              class="radio-item"
              :class="{ active: form.gender === 1 }"
              @click="form.gender = 1"
            >
              <u-icon name="man" size="16" :color="form.gender === 1 ? '#fff' : '#3B82F6'" />
              <text>男</text>
            </view>
            <view
              class="radio-item"
              :class="{ active: form.gender === 2 }"
              @click="form.gender = 2"
            >
              <u-icon name="woman" size="16" :color="form.gender === 2 ? '#fff' : '#EC4899'" />
              <text>女</text>
            </view>
          </view>
        </view>

        <view class="form-item" @click="showDatePicker('birthDate')">
          <text class="label">出生日期</text>
          <view class="picker-value">
            <text :class="{ placeholder: !form.birthDate }">
              {{ form.birthDate || '请选择出生日期' }}
            </text>
            <u-icon name="arrow-right" size="16" color="#9CA3AF" />
          </view>
        </view>

        <view class="form-item">
          <text class="label">身份证号</text>
          <input
            class="input"
            v-model="form.identityCard"
            placeholder="请输入身份证号"
          />
        </view>
      </view>

      <view class="form-section">
        <view class="section-title">联系方式</view>

        <view class="form-item">
          <text class="label">手机号码</text>
          <input
            class="input"
            v-model="form.phone"
            type="number"
            placeholder="请输入手机号码"
          />
        </view>

        <view class="form-item">
          <text class="label">电子邮箱</text>
          <input
            class="input"
            v-model="form.email"
            placeholder="请输入电子邮箱"
          />
        </view>
      </view>

      <view class="form-section">
        <view class="section-title">学籍信息</view>

        <view class="form-item" @click="showClassPicker">
          <text class="label required">所属班级</text>
          <view class="picker-value">
            <text :class="{ placeholder: !form.classId }">
              {{ selectedClassName || '请选择班级' }}
            </text>
            <u-icon name="arrow-right" size="16" color="#9CA3AF" />
          </view>
        </view>

        <view class="form-item" @click="showDatePicker('enrollmentDate')">
          <text class="label">入学日期</text>
          <view class="picker-value">
            <text :class="{ placeholder: !form.enrollmentDate }">
              {{ form.enrollmentDate || '请选择入学日期' }}
            </text>
            <u-icon name="arrow-right" size="16" color="#9CA3AF" />
          </view>
        </view>

        <view class="form-item">
          <text class="label">学生状态</text>
          <view class="radio-group status-group">
            <view
              v-for="status in statusOptions"
              :key="status.value"
              class="status-item"
              :class="{ active: form.status === status.value }"
              @click="form.status = status.value"
            >
              <text>{{ status.label }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 提交按钮 -->
      <view class="submit-section">
        <button class="submit-btn" @click="submitForm" :loading="submitting">
          {{ isEdit ? '保存修改' : '添加学生' }}
        </button>
      </view>
    </scroll-view>

    <!-- 日期选择器 -->
    <u-datetime-picker
      :show="showPicker"
      mode="date"
      v-model="pickerValue"
      @confirm="onDateConfirm"
      @cancel="showPicker = false"
    />

    <!-- 班级选择器 -->
    <u-popup v-model:show="showClassPopup" mode="bottom" round="20">
      <view class="class-picker">
        <view class="picker-header">
          <text class="cancel" @click="showClassPopup = false">取消</text>
          <text class="title">选择班级</text>
          <text class="confirm" @click="confirmClass">确定</text>
        </view>
        <scroll-view scroll-y class="picker-content">
          <view
            v-for="cls in classList"
            :key="cls.id"
            class="class-option"
            :class="{ active: tempClassId === cls.id }"
            @click="tempClassId = cls.id"
          >
            <text class="class-name">{{ cls.className }}</text>
            <text class="dept-name">{{ cls.departmentName }}</text>
            <u-icon v-if="tempClassId === cls.id" name="checkmark" size="18" color="#667eea" />
          </view>
        </scroll-view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getStudentById, createStudent, updateStudent, type StudentFormData } from '@/api/student'
import { getAllClasses } from '@/api/class'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)

// 编辑模式
const isEdit = ref(false)
const studentId = ref<string>('')

// 表单数据
const form = reactive<StudentFormData>({
  studentNo: '',
  realName: '',
  gender: 1,
  birthDate: '',
  identityCard: '',
  phone: '',
  email: '',
  classId: undefined,
  enrollmentDate: '',
  status: 1,
})

// 班级列表
const classList = ref<any[]>([])
const showClassPopup = ref(false)
const tempClassId = ref<number | undefined>()

const selectedClassName = computed(() => {
  const cls = classList.value.find(c => c.id === form.classId)
  return cls?.className || ''
})

// 日期选择
const showPicker = ref(false)
const pickerValue = ref(Date.now())
const currentDateField = ref<'birthDate' | 'enrollmentDate'>('birthDate')

// 状态选项
const statusOptions = [
  { label: '在籍', value: 1 },
  { label: '休学', value: 3 },
  { label: '退学', value: 4 },
]

// 提交状态
const submitting = ref(false)

onMounted(() => {
  initSystemInfo()
  loadClassList()
  loadData()
})

const initSystemInfo = () => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navBarHeight.value = statusBarHeight.value + 44
}

const loadClassList = async () => {
  try {
    const res = await getAllClasses()
    classList.value = res || []
  } catch (error) {
    console.error('加载班级列表失败', error)
  }
}

const loadData = async () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  studentId.value = (currentPage as any).options?.id

  if (studentId.value) {
    isEdit.value = true
    try {
      const res = await getStudentById(studentId.value)
      Object.assign(form, {
        studentNo: res.studentNo,
        realName: res.realName,
        gender: res.gender || 1,
        birthDate: res.birthDate || '',
        identityCard: res.identityCard || '',
        phone: res.phone || '',
        email: res.email || '',
        classId: res.classId,
        enrollmentDate: res.enrollmentDate || '',
        status: res.status || 1,
      })
    } catch (error: any) {
      uni.showToast({ title: error.message || '加载失败', icon: 'none' })
    }
  }
}

const goBack = () => {
  uni.navigateBack()
}

const showDatePicker = (field: 'birthDate' | 'enrollmentDate') => {
  currentDateField.value = field
  if (form[field]) {
    pickerValue.value = new Date(form[field]).getTime()
  } else {
    pickerValue.value = Date.now()
  }
  showPicker.value = true
}

const onDateConfirm = (e: any) => {
  const date = new Date(e.value)
  const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  form[currentDateField.value] = dateStr
  showPicker.value = false
}

const showClassPicker = () => {
  tempClassId.value = form.classId
  showClassPopup.value = true
}

const confirmClass = () => {
  form.classId = tempClassId.value
  showClassPopup.value = false
}

const validateForm = (): boolean => {
  if (!form.studentNo?.trim()) {
    uni.showToast({ title: '请输入学号', icon: 'none' })
    return false
  }
  if (!form.realName?.trim()) {
    uni.showToast({ title: '请输入姓名', icon: 'none' })
    return false
  }
  if (!form.classId) {
    uni.showToast({ title: '请选择班级', icon: 'none' })
    return false
  }
  return true
}

const submitForm = async () => {
  if (!validateForm()) return
  if (submitting.value) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateStudent(studentId.value, form)
      uni.showToast({ title: '保存成功', icon: 'success' })
    } else {
      await createStudent(form)
      uni.showToast({ title: '添加成功', icon: 'success' })
    }
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (error: any) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background: #F3F4F6;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .nav-content {
    display: flex;
    align-items: center;
    height: 88rpx;
    padding: 0 24rpx;

    .nav-back, .nav-placeholder {
      width: 64rpx;
      height: 64rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .nav-title {
      flex: 1;
      font-size: 36rpx;
      font-weight: 600;
      color: #fff;
      text-align: center;
    }
  }
}

.form-content {
  height: calc(100vh - var(--nav-height));
  padding-bottom: 40rpx;
}

.form-section {
  background: #fff;
  margin-bottom: 20rpx;
  padding: 24rpx 32rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #1F2937;
    margin-bottom: 24rpx;
    padding-left: 16rpx;
    border-left: 6rpx solid #667eea;
  }

  .form-item {
    display: flex;
    align-items: center;
    padding: 24rpx 0;
    border-bottom: 1rpx solid #F3F4F6;

    &:last-child {
      border-bottom: none;
    }

    .label {
      width: 180rpx;
      font-size: 28rpx;
      color: #4B5563;
      flex-shrink: 0;

      &.required::before {
        content: '*';
        color: #EF4444;
        margin-right: 8rpx;
      }
    }

    .input {
      flex: 1;
      font-size: 28rpx;
      color: #1F2937;
      text-align: right;
    }

    .picker-value {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 8rpx;

      text {
        font-size: 28rpx;
        color: #1F2937;

        &.placeholder {
          color: #9CA3AF;
        }
      }
    }

    .radio-group {
      flex: 1;
      display: flex;
      justify-content: flex-end;
      gap: 16rpx;

      .radio-item {
        display: flex;
        align-items: center;
        gap: 8rpx;
        padding: 12rpx 24rpx;
        background: #F3F4F6;
        border-radius: 32rpx;

        &.active {
          background: #667eea;

          text {
            color: #fff;
          }
        }

        text {
          font-size: 26rpx;
          color: #4B5563;
        }
      }
    }

    .status-group {
      flex-wrap: wrap;

      .status-item {
        padding: 12rpx 20rpx;
        background: #F3F4F6;
        border-radius: 8rpx;

        &.active {
          background: #667eea;

          text {
            color: #fff;
          }
        }

        text {
          font-size: 24rpx;
          color: #4B5563;
        }
      }
    }
  }
}

.submit-section {
  padding: 40rpx 32rpx;

  .submit-btn {
    width: 100%;
    height: 88rpx;
    line-height: 88rpx;
    font-size: 32rpx;
    font-weight: 500;
    color: #fff;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 44rpx;
    border: none;
  }
}

.class-picker {
  .picker-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24rpx 32rpx;
    border-bottom: 1rpx solid #F3F4F6;

    .cancel {
      font-size: 28rpx;
      color: #6B7280;
    }

    .title {
      font-size: 32rpx;
      font-weight: 600;
      color: #1F2937;
    }

    .confirm {
      font-size: 28rpx;
      color: #667eea;
    }
  }

  .picker-content {
    max-height: 60vh;
    padding: 16rpx 0;

    .class-option {
      display: flex;
      align-items: center;
      padding: 24rpx 32rpx;
      gap: 16rpx;

      &.active {
        background: rgba(102, 126, 234, 0.05);
      }

      .class-name {
        flex: 1;
        font-size: 28rpx;
        color: #1F2937;
      }

      .dept-name {
        font-size: 24rpx;
        color: #9CA3AF;
      }
    }
  }
}
</style>
