<template>
  <div class="p-6">
    <!-- Loading 状态 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
    </div>

    <!-- 详情内容 -->
    <div v-else-if="studentInfo" class="space-y-6">
      <!-- 顶部基本信息卡片 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-medium text-gray-900">学生信息</h3>
          <button
            v-if="hasPermission('student:info:edit')"
            class="text-sm text-blue-600 hover:text-blue-700"
            @click="handleEdit"
          >
            编辑
          </button>
        </div>
        <div class="p-4">
          <!-- 头像和基本信息 -->
          <div class="flex items-start gap-4 border-b border-gray-100 pb-4 mb-4">
            <div class="flex h-16 w-16 items-center justify-center rounded-full bg-blue-100 text-xl font-semibold text-blue-600">
              {{ studentInfo.name?.charAt(0) || '?' }}
            </div>
            <div class="flex-1">
              <h2 class="text-lg font-semibold text-gray-900">{{ studentInfo.name }}</h2>
              <p class="mt-0.5 text-sm text-gray-500">学号: {{ studentInfo.studentNo }}</p>
              <div class="mt-2 flex items-center gap-2">
                <span
                  class="inline-flex items-center rounded px-2 py-0.5 text-xs font-medium"
                  :class="getStatusClass(studentInfo.status)"
                >
                  {{ getStatusText(studentInfo.status) }}
                </span>
                <span
                  :class="[
                    'inline-flex items-center rounded px-2 py-0.5 text-xs font-medium',
                    studentInfo.gender === 1 ? 'bg-blue-50 text-blue-700' : 'bg-pink-50 text-pink-700'
                  ]"
                >
                  {{ studentInfo.gender === 1 ? '男' : '女' }}
                </span>
              </div>
            </div>
          </div>

          <!-- 快速信息 -->
          <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div class="flex items-center">
              <span class="text-sm text-gray-500 w-16">班级:</span>
              <span class="text-sm text-gray-900">{{ studentInfo.className || '-' }}</span>
            </div>
            <div class="flex items-center">
              <span class="text-sm text-gray-500 w-16">专业:</span>
              <span class="text-sm text-gray-900">{{ studentInfo.majorName || '-' }}</span>
            </div>
            <div class="flex items-center">
              <span class="text-sm text-gray-500 w-16">年级:</span>
              <span class="text-sm text-gray-900">{{ studentInfo.gradeLevel ? `${studentInfo.gradeLevel}级` : '-' }}</span>
            </div>
            <div class="flex items-center">
              <span class="text-sm text-gray-500 w-16">联系电话:</span>
              <span class="text-sm text-gray-900">{{ studentInfo.phone || '-' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 标签页详情 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <!-- 标签页导航 -->
        <div class="flex border-b border-gray-200">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            :class="[
              'px-4 py-3 text-sm font-medium border-b-2 transition-colors',
              activeTab === tab.key
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            ]"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- 标签页内容 -->
        <div class="p-4">
          <!-- 基本信息 -->
          <div v-if="activeTab === 'basic'" class="space-y-4">
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <InfoItem label="学号" :value="studentInfo.studentNo" />
              <InfoItem label="姓名" :value="studentInfo.name" />
              <InfoItem label="性别" :value="studentInfo.gender === 1 ? '男' : '女'" />
              <InfoItem label="证件类型" :value="studentInfo.idCardType || '身份证'" />
              <InfoItem label="身份证号" :value="maskIdCard(studentInfo.idCard)" />
              <InfoItem label="出生日期" :value="formatDate(studentInfo.birthDate)" />
              <InfoItem label="民族" :value="studentInfo.ethnicity" />
              <InfoItem label="政治面貌" :value="studentInfo.politicalStatus" />
              <InfoItem label="联系电话" :value="studentInfo.phone" />
            </div>
          </div>

          <!-- 学籍信息 -->
          <div v-if="activeTab === 'academic'" class="space-y-4">
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <InfoItem label="招生年度" :value="formatDate(studentInfo.enrollmentDate)" />
              <InfoItem label="年级" :value="studentInfo.gradeName || (studentInfo.gradeLevel ? `${studentInfo.gradeLevel}级` : '-')" />
              <InfoItem label="专业" :value="studentInfo.majorName" />
              <InfoItem label="班级" :value="studentInfo.className" />
              <InfoItem label="层次" :value="studentInfo.educationLevel" />
              <InfoItem label="学制" :value="studentInfo.studyLength" />
              <InfoItem label="入学前学历" :value="studentInfo.degreeType" />
              <InfoItem label="学生状态" :value="getStatusText(studentInfo.status)" />
              <InfoItem label="预计毕业日期" :value="formatDate(studentInfo.expectedGraduationDate)" />
            </div>
          </div>

          <!-- 户籍与资助信息 -->
          <div v-if="activeTab === 'hukou'" class="space-y-4">
            <h4 class="text-sm font-medium text-gray-700 border-b pb-2">户籍信息</h4>
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <InfoItem label="户口所在地-省" :value="studentInfo.hukouProvince" />
              <InfoItem label="户口所在地-市" :value="studentInfo.hukouCity" />
              <InfoItem label="户口所在地-区" :value="studentInfo.hukouDistrict" />
              <InfoItem label="户口详细地址" :value="studentInfo.hukouAddress" class="sm:col-span-2" />
              <InfoItem label="户口性质" :value="studentInfo.hukouType" />
              <InfoItem label="邮政编码" :value="studentInfo.postalCode" />
              <InfoItem label="家庭住址" :value="studentInfo.homeAddress" class="sm:col-span-3" />
            </div>

            <h4 class="text-sm font-medium text-gray-700 border-b pb-2 mt-6">资助信息</h4>
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <InfoItem label="是否建档立卡" :value="studentInfo.isPovertyRegistered === 1 ? '是' : '否'" />
              <InfoItem label="资助申请类型" :value="studentInfo.financialAidType" />
            </div>
          </div>

          <!-- 家庭信息 -->
          <div v-if="activeTab === 'family'" class="space-y-4">
            <h4 class="text-sm font-medium text-gray-700 border-b pb-2">父亲信息</h4>
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <InfoItem label="父亲姓名" :value="studentInfo.fatherName" />
              <InfoItem label="父亲身份证号" :value="maskIdCard(studentInfo.fatherIdCard)" />
              <InfoItem label="父亲电话" :value="studentInfo.fatherPhone" />
            </div>

            <h4 class="text-sm font-medium text-gray-700 border-b pb-2 mt-6">母亲信息</h4>
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <InfoItem label="母亲姓名" :value="studentInfo.motherName" />
              <InfoItem label="母亲身份证号" :value="maskIdCard(studentInfo.motherIdCard)" />
              <InfoItem label="母亲电话" :value="studentInfo.motherPhone" />
            </div>

            <h4 class="text-sm font-medium text-gray-700 border-b pb-2 mt-6">其他监护人</h4>
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <InfoItem label="监护人姓名" :value="studentInfo.guardianName" />
              <InfoItem label="监护人身份证号" :value="maskIdCard(studentInfo.guardianIdCard)" />
              <InfoItem label="监护人电话" :value="studentInfo.guardianPhone" />
              <InfoItem label="与学生关系" :value="studentInfo.guardianRelation" />
            </div>

            <h4 class="text-sm font-medium text-gray-700 border-b pb-2 mt-6">紧急联系人</h4>
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <InfoItem label="紧急联系人" :value="studentInfo.emergencyContact" />
              <InfoItem label="紧急联系电话" :value="studentInfo.emergencyPhone" />
            </div>
          </div>

          <!-- 住宿与其他 -->
          <div v-if="activeTab === 'other'" class="space-y-4">
            <div class="flex items-center justify-between border-b pb-2">
              <h4 class="text-sm font-medium text-gray-700">当前住宿信息</h4>
              <button
                v-if="dormitoryHistory.length > 0"
                class="text-xs text-blue-600 hover:text-blue-700"
                @click="showHistory = !showHistory"
              >
                {{ showHistory ? '隐藏历史' : '查看住宿历史' }} ({{ dormitoryHistory.length }}条)
              </button>
            </div>
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-4">
              <InfoItem label="楼栋名称" :value="studentInfo.buildingName" />
              <InfoItem label="楼号" :value="studentInfo.buildingNo" />
              <InfoItem label="房间号" :value="studentInfo.roomNo" />
              <InfoItem label="床位号" :value="studentInfo.bedNumber" />
            </div>

            <!-- 住宿历史 -->
            <div v-if="showHistory && dormitoryHistory.length > 0" class="mt-4">
              <h4 class="text-sm font-medium text-gray-700 border-b pb-2 mb-3">住宿历史记录</h4>
              <div class="overflow-hidden rounded-lg border border-gray-200">
                <table class="min-w-full divide-y divide-gray-200">
                  <thead class="bg-gray-50">
                    <tr>
                      <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">楼栋</th>
                      <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">房间号</th>
                      <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">床位</th>
                      <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">入住日期</th>
                      <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">退宿日期</th>
                      <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">状态</th>
                      <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">原因</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-gray-200 bg-white">
                    <tr v-for="record in dormitoryHistory" :key="record.id">
                      <td class="whitespace-nowrap px-3 py-2 text-xs text-gray-900">{{ record.buildingName || '-' }}</td>
                      <td class="whitespace-nowrap px-3 py-2 text-xs text-gray-600">{{ record.placeName || '-' }}</td>
                      <td class="whitespace-nowrap px-3 py-2 text-xs text-gray-600">{{ record.positionNo || '-' }}</td>
                      <td class="whitespace-nowrap px-3 py-2 text-xs text-gray-600">{{ record.checkInTime || '-' }}</td>
                      <td class="whitespace-nowrap px-3 py-2 text-xs text-gray-600">{{ record.checkOutTime || '-' }}</td>
                      <td class="whitespace-nowrap px-3 py-2 text-xs">
                        <span :class="getDormitoryStatusClass(record.status)" class="inline-flex rounded px-1.5 py-0.5 text-xs font-medium">
                          {{ getDormitoryStatusText(record.status) }}
                        </span>
                      </td>
                      <td class="px-3 py-2 text-xs text-gray-600 max-w-32 truncate" :title="record.remark">{{ record.remark || '-' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <h4 class="text-sm font-medium text-gray-700 border-b pb-2 mt-6">健康信息</h4>
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <InfoItem label="健康状况" :value="studentInfo.healthStatus" />
              <InfoItem label="过敏史" :value="studentInfo.allergies" class="sm:col-span-2" />
            </div>

            <h4 class="text-sm font-medium text-gray-700 border-b pb-2 mt-6">其他信息</h4>
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <InfoItem label="特殊备注" :value="studentInfo.specialNotes" class="sm:col-span-3" />
              <InfoItem label="创建时间" :value="formatDateTime(studentInfo.createdAt)" />
              <InfoItem label="更新时间" :value="formatDateTime(studentInfo.updatedAt)" />
            </div>
          </div>

          <!-- 异动记录 -->
          <div v-if="activeTab === 'changes'" class="space-y-4">
            <div v-if="statusChangesLoading" class="flex items-center justify-center py-8">
              <div class="h-6 w-6 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
            </div>
            <div v-else-if="statusChanges.length === 0" class="py-8 text-center text-sm text-gray-500">
              暂无异动记录
            </div>
            <div v-else class="overflow-hidden rounded-lg border border-gray-200">
              <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                  <tr>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">异动类型</th>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">原状态</th>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">新状态</th>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">班级变动</th>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">原因</th>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">操作人</th>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">生效日期</th>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">记录时间</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200 bg-white">
                  <tr v-for="record in statusChanges" :key="record.id">
                    <td class="whitespace-nowrap px-3 py-2 text-xs">
                      <span
                        :class="getChangeTypeClass(record.changeType)"
                        class="inline-flex rounded px-1.5 py-0.5 text-xs font-medium"
                      >
                        {{ getChangeTypeText(record.changeType) }}
                      </span>
                    </td>
                    <td class="whitespace-nowrap px-3 py-2 text-xs text-gray-600">{{ record.fromStatus || '-' }}</td>
                    <td class="whitespace-nowrap px-3 py-2 text-xs text-gray-600">{{ record.toStatus || '-' }}</td>
                    <td class="whitespace-nowrap px-3 py-2 text-xs text-gray-600">
                      <template v-if="record.fromClassName || record.toClassName">
                        {{ record.fromClassName || '-' }} -> {{ record.toClassName || '-' }}
                      </template>
                      <template v-else>-</template>
                    </td>
                    <td class="px-3 py-2 text-xs text-gray-600 max-w-40 truncate" :title="record.reason">{{ record.reason || '-' }}</td>
                    <td class="whitespace-nowrap px-3 py-2 text-xs text-gray-600">{{ record.operatorName || '-' }}</td>
                    <td class="whitespace-nowrap px-3 py-2 text-xs text-gray-600">{{ record.effectiveDate || '-' }}</td>
                    <td class="whitespace-nowrap px-3 py-2 text-xs text-gray-600">{{ formatDateTime(record.createdAt) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="flex flex-col items-center justify-center py-16">
      <p class="text-sm text-gray-500">学生信息不存在</p>
    </div>

    <!-- 操作按钮 -->
    <div class="mt-6 flex justify-end border-t border-gray-200 pt-4">
      <button
        class="h-9 rounded-md border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
        @click="$emit('close')"
      >
        关闭
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { formatDate, formatDateTime } from '@/utils/date'
// V2 DDD API
import { getStudent, getStudentStatusChanges } from '@/api/student'
import { universalPlaceApi } from '@/api/universalPlace'
import type { PlaceOccupantWithPlace } from '@/types/universalPlace'
import type { Student, StudentStatusChange } from '@/types/student'
import { StudentStatusMap, ChangeTypeMap } from '@/types/student'

// 信息项组件
const InfoItem = (props: { label: string; value?: string | null; class?: string }) => {
  return h('div', { class: ['flex items-start', props.class] }, [
    h('span', { class: 'text-sm text-gray-500 w-28 flex-shrink-0' }, props.label + ':'),
    h('span', { class: 'text-sm text-gray-900 break-all' }, props.value || '-')
  ])
}

interface Props {
  studentId: number | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  close: []
  edit: [id: number]
}>()

const authStore = useAuthStore()

// 权限检查
const hasPermission = (permission: string) => authStore.hasPermission(permission)

// 标签页配置
const tabs = [
  { key: 'basic', label: '基本信息' },
  { key: 'academic', label: '学籍信息' },
  { key: 'hukou', label: '户籍与资助' },
  { key: 'family', label: '家庭信息' },
  { key: 'other', label: '住宿与其他' },
  { key: 'changes', label: '异动记录' }
]

const activeTab = ref('basic')

// 数据
const loading = ref(false)
const studentInfo = ref<Student | null>(null)
const dormitoryHistory = ref<PlaceOccupantWithPlace[]>([])
const showHistory = ref(false)
const statusChanges = ref<StudentStatusChange[]>([])
const statusChangesLoading = ref(false)

// 获取状态样式类 - V2: 0=在读, 1=休学, 2=退学, 3=毕业, 4=转学
const getStatusClass = (status: number) => {
  const classMap: Record<number, string> = {
    0: 'bg-green-50 text-green-700',
    1: 'bg-amber-50 text-amber-700',
    2: 'bg-gray-100 text-gray-700',
    3: 'bg-red-50 text-red-700',
    4: 'bg-purple-50 text-purple-700'
  }
  return classMap[status] || 'bg-gray-100 text-gray-700'
}

// 获取状态文本 - V2 使用 StudentStatusMap
const getStatusText = (status: number) => {
  return StudentStatusMap[status as keyof typeof StudentStatusMap] || '未知'
}

// 脱敏身份证号
const maskIdCard = (idCard?: string) => {
  if (!idCard) return '-'
  if (idCard.length >= 14) {
    return idCard.replace(/(\d{6})\d{8}(\d{4})/, '$1********$2')
  }
  return idCard
}

// 住宿状态样式类 (UniversalPlace: 1=在住, 0=已退)
const getDormitoryStatusClass = (status: number) => {
  const classMap: Record<number, string> = {
    1: 'bg-green-50 text-green-700',
    0: 'bg-gray-100 text-gray-700'
  }
  return classMap[status] || 'bg-gray-100 text-gray-700'
}

// 住宿状态文本
const getDormitoryStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    1: '在住',
    0: '已退宿'
  }
  return statusMap[status] || '未知'
}

// 加载学籍异动记录
const loadStatusChanges = async () => {
  if (!props.studentId) return
  statusChangesLoading.value = true
  try {
    const response = await getStudentStatusChanges(props.studentId)
    statusChanges.value = response || []
  } catch (error) {
    console.error('加载异动记录失败:', error)
  } finally {
    statusChangesLoading.value = false
  }
}

// 获取异动类型文本
const getChangeTypeText = (type: string) => {
  return ChangeTypeMap[type] || type
}

// 获取异动类型样式
const getChangeTypeClass = (type: string) => {
  const classMap: Record<string, string> = {
    ENROLL: 'bg-green-50 text-green-700',
    SUSPEND: 'bg-amber-50 text-amber-700',
    RESUME: 'bg-blue-50 text-blue-700',
    GRADUATE: 'bg-purple-50 text-purple-700',
    WITHDRAW: 'bg-gray-100 text-gray-700',
    EXPEL: 'bg-red-50 text-red-700',
    TRANSFER_CLASS: 'bg-indigo-50 text-indigo-700',
    TRANSFER_MAJOR: 'bg-cyan-50 text-cyan-700'
  }
  return classMap[type] || 'bg-gray-100 text-gray-700'
}

// 加载住宿历史 (via UniversalPlace occupant history)
const loadDormitoryHistory = async () => {
  if (!props.studentId) return
  try {
    // studentId is the user ID in the UniversalPlace system
    const response = await universalPlaceApi.getOccupantHistoryByOccupant('STUDENT', props.studentId)
    dormitoryHistory.value = response || []
  } catch (error) {
    console.error('加载住宿历史失败:', error)
  }
}

// 加载学生详情 - V2 API
const loadStudentDetail = async () => {
  if (!props.studentId) return

  loading.value = true
  try {
    const response = await getStudent(props.studentId)
    studentInfo.value = response
  } catch (error: any) {
    console.error('加载学生详情失败:', error)
    ElMessage.error(error.message || '加载学生详情失败')
  } finally {
    loading.value = false
  }
}

// 编辑信息
const handleEdit = () => {
  if (props.studentId) {
    emit('edit', props.studentId)
  }
}

// 初始化
onMounted(() => {
  loadStudentDetail()
  loadDormitoryHistory()
  loadStatusChanges()
})
</script>
