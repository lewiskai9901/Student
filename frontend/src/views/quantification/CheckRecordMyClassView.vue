<template>
  <div class="min-h-screen bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-6">
      <button
        @click="goBack"
        class="mb-4 flex items-center gap-2 text-sm text-gray-600 transition-colors hover:text-cyan-600"
      >
        <ArrowLeft class="h-4 w-4" />
        返回列表
      </button>

      <div class="rounded-xl bg-gradient-to-r from-cyan-600 to-blue-500 p-6 shadow-lg">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="flex items-center gap-3 text-2xl font-bold text-white">
              <GraduationCap class="h-8 w-8" />
              本班检查详情
            </h1>
            <p class="mt-1 text-cyan-100">查看本班在本次检查中的扣分情况</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="flex flex-col items-center gap-3">
        <Loader2 class="h-8 w-8 animate-spin text-cyan-600" />
        <span class="text-gray-500">加载中...</span>
      </div>
    </div>

    <template v-if="!loading">
      <!-- 检查基本信息 -->
      <div class="mb-6 rounded-xl bg-white p-6 shadow-sm">
        <h2 class="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900">
          <ClipboardList class="h-5 w-5 text-cyan-600" />
          检查信息
        </h2>
        <div class="grid grid-cols-3 gap-4 lg:grid-cols-6">
          <div class="rounded-lg bg-gray-50 p-4">
            <p class="text-sm text-gray-500">检查名称</p>
            <p class="mt-1 font-semibold text-gray-900">{{ recordInfo.checkName || '-' }}</p>
          </div>
          <div class="rounded-lg bg-gray-50 p-4">
            <p class="text-sm text-gray-500">检查日期</p>
            <p class="mt-1 font-semibold text-gray-900">{{ recordInfo.checkDate || '-' }}</p>
          </div>
          <div class="rounded-lg bg-gray-50 p-4">
            <p class="text-sm text-gray-500">检查人</p>
            <p class="mt-1 font-semibold text-gray-900">{{ recordInfo.checkerName || '-' }}</p>
          </div>
          <div class="rounded-lg bg-amber-50 p-4">
            <p class="text-sm text-amber-600">全校平均分</p>
            <p class="mt-1 text-xl font-bold text-amber-600">{{ recordInfo.avgScore?.toFixed(2) || '0.00' }} 分</p>
          </div>
          <div class="rounded-lg bg-cyan-50 p-4">
            <p class="text-sm text-cyan-600">涉及班级数</p>
            <p class="mt-1 text-xl font-bold text-cyan-700">{{ recordInfo.totalClasses || 0 }} 个</p>
          </div>
          <div class="rounded-lg bg-gray-50 p-4">
            <p class="text-sm text-gray-500">发布时间</p>
            <p class="mt-1 font-semibold text-gray-900">{{ recordInfo.publishTime || '-' }}</p>
          </div>
        </div>
      </div>

      <!-- 本班情况总览 -->
      <div class="mb-6 rounded-xl bg-white shadow-sm" v-if="myClassStats">
        <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
          <h2 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
            <School class="h-5 w-5 text-cyan-600" />
            {{ myClassStats.className }} - 扣分情况
          </h2>
          <span
            class="rounded-lg px-4 py-2 text-sm font-bold"
            :class="getLevelTagClass(myClassStats.scoreLevel)"
          >
            {{ myClassStats.scoreLevel }}
          </span>
        </div>

        <div class="p-6">
          <!-- 统计卡片 -->
          <div class="mb-6 grid grid-cols-4 gap-4">
            <div class="rounded-xl bg-gradient-to-br from-red-50 to-rose-50 p-5">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-red-600">总扣分</p>
                  <p class="mt-1 text-3xl font-bold text-red-600">{{ myClassStats.totalScore.toFixed(2) }}</p>
                  <p class="mt-1 text-xs text-red-500">分</p>
                </div>
                <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-red-100 text-red-600">
                  <Minus class="h-6 w-6" />
                </div>
              </div>
            </div>
            <div class="rounded-xl bg-gradient-to-br from-blue-50 to-indigo-50 p-5">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-blue-600">全校排名</p>
                  <p class="mt-1 text-3xl font-bold" :class="getRankingColorClass(myClassStats.ranking)">
                    第 {{ myClassStats.ranking }} 名
                  </p>
                </div>
                <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-100 text-blue-600">
                  <Trophy class="h-6 w-6" />
                </div>
              </div>
            </div>
            <div class="rounded-xl bg-gradient-to-br from-purple-50 to-violet-50 p-5">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-purple-600">扣分项数</p>
                  <p class="mt-1 text-3xl font-bold text-purple-600">{{ myClassStats.totalItems }}</p>
                  <p class="mt-1 text-xs text-purple-500">项</p>
                </div>
                <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-purple-100 text-purple-600">
                  <FileText class="h-6 w-6" />
                </div>
              </div>
            </div>
            <div class="rounded-xl p-5" :class="myClassStats.vsAvgScore > 0 ? 'bg-gradient-to-br from-red-50 to-orange-50' : 'bg-gradient-to-br from-green-50 to-emerald-50'">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium" :class="myClassStats.vsAvgScore > 0 ? 'text-red-600' : 'text-green-600'">
                    与平均分差值
                  </p>
                  <p class="mt-1 text-3xl font-bold" :class="myClassStats.vsAvgScore > 0 ? 'text-red-600' : 'text-green-600'">
                    {{ myClassStats.vsAvgScore > 0 ? '+' : '' }}{{ myClassStats.vsAvgScore?.toFixed(2) }}
                  </p>
                </div>
                <div
                  class="flex h-12 w-12 items-center justify-center rounded-xl"
                  :class="myClassStats.vsAvgScore > 0 ? 'bg-red-100 text-red-600' : 'bg-green-100 text-green-600'"
                >
                  <TrendingUp v-if="myClassStats.vsAvgScore > 0" class="h-6 w-6" />
                  <TrendingDown v-else class="h-6 w-6" />
                </div>
              </div>
            </div>
          </div>

          <!-- 分类扣分统计 -->
          <div class="flex flex-wrap gap-3">
            <span v-if="myClassStats.hygieneScore > 0" class="rounded-lg bg-amber-100 px-4 py-2 text-sm font-semibold text-amber-700">
              卫生类: {{ myClassStats.hygieneScore.toFixed(2) }} 分
            </span>
            <span v-if="myClassStats.disciplineScore > 0" class="rounded-lg bg-red-100 px-4 py-2 text-sm font-semibold text-red-700">
              纪律类: {{ myClassStats.disciplineScore.toFixed(2) }} 分
            </span>
            <span v-if="myClassStats.attendanceScore > 0" class="rounded-lg bg-purple-100 px-4 py-2 text-sm font-semibold text-purple-700">
              考勤类: {{ myClassStats.attendanceScore.toFixed(2) }} 分
            </span>
            <span v-if="myClassStats.otherScore > 0" class="rounded-lg bg-gray-100 px-4 py-2 text-sm font-semibold text-gray-700">
              其他: {{ myClassStats.otherScore.toFixed(2) }} 分
            </span>
          </div>
        </div>
      </div>

      <!-- 扣分明细 -->
      <div class="mb-6 rounded-xl bg-white shadow-sm" v-if="myClassStats && myClassStats.items?.length > 0">
        <div class="border-b border-gray-100 px-6 py-4">
          <h2 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
            <ListChecks class="h-5 w-5 text-cyan-600" />
            扣分明细
          </h2>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="bg-gray-50 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                <th class="px-4 py-3 w-12">#</th>
                <th class="px-4 py-3 w-28">检查类别</th>
                <th class="px-4 py-3">扣分项</th>
                <th class="px-4 py-3 w-20 text-center">扣分</th>
                <th class="px-4 py-3 w-20 text-center">涉及人数</th>
                <th class="px-4 py-3 w-28">关联信息</th>
                <th class="px-4 py-3 w-24 text-center">证据照片</th>
                <th class="px-4 py-3">详细说明</th>
                <th class="px-4 py-3 w-28 text-center">申诉</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr
                v-for="(row, index) in myClassStats.items"
                :key="row.id"
                class="transition-colors hover:bg-gray-50"
                :style="{ animationDelay: `${index * 30}ms` }"
              >
                <td class="px-4 py-3 text-sm text-gray-500">{{ index + 1 }}</td>
                <td class="px-4 py-3">
                  <span class="rounded-full bg-cyan-100 px-2 py-1 text-xs font-medium text-cyan-700">
                    {{ row.categoryName }}
                  </span>
                </td>
                <td class="px-4 py-3 text-sm text-gray-900">{{ row.itemName }}</td>
                <td class="px-4 py-3 text-center text-lg font-bold text-red-600">
                  {{ row.deductScore.toFixed(2) }}
                </td>
                <td class="px-4 py-3 text-center text-sm text-gray-600">
                  {{ row.personCount || '-' }}
                </td>
                <td class="px-4 py-3 text-sm text-gray-600">
                  {{ row.linkInfo || '-' }}
                </td>
                <td class="px-4 py-3 text-center">
                  <button
                    v-if="row.photoUrls && row.photoUrls.length > 0"
                    @click="viewPhotos(row.photoUrls)"
                    class="text-sm text-cyan-600 hover:text-cyan-700"
                  >
                    查看({{ row.photoUrls.length }})
                  </button>
                  <span v-else class="text-sm text-gray-400">无</span>
                </td>
                <td class="px-4 py-3 text-sm text-gray-600">
                  {{ row.remark || '-' }}
                </td>
                <td class="px-4 py-3 text-center">
                  <button
                    v-if="row.appealStatus === 0"
                    @click="submitAppeal(row.id)"
                    class="rounded-lg bg-amber-500 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-amber-600"
                  >
                    提交申诉
                  </button>
                  <span v-else-if="row.appealStatus === 1" class="rounded-full bg-amber-100 px-2 py-1 text-xs font-medium text-amber-700">
                    申诉中
                  </span>
                  <span v-else-if="row.appealStatus === 2" class="rounded-full bg-green-100 px-2 py-1 text-xs font-medium text-green-700">
                    已通过
                  </span>
                  <span v-else class="rounded-full bg-red-100 px-2 py-1 text-xs font-medium text-red-700">
                    已驳回
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 其他班级排名 -->
      <div class="rounded-xl bg-white shadow-sm">
        <div class="border-b border-gray-100 px-6 py-4">
          <h2 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
            <BarChart3 class="h-5 w-5 text-cyan-600" />
            附近班级排名
          </h2>
          <div class="mt-2 flex items-start gap-2 rounded-lg bg-blue-50 p-3 text-sm text-blue-700">
            <Info class="mt-0.5 h-4 w-4 flex-shrink-0" />
            <span>为保护隐私，仅显示排名相近的班级信息（班级名称已脱敏）</span>
          </div>
        </div>

        <div class="p-4">
          <div class="space-y-3">
            <div
              v-for="(item, index) in otherClasses"
              :key="item.ranking"
              class="flex items-center gap-4 rounded-xl border-2 p-4 transition-all"
              :class="!item.className.includes('班级') ? 'border-cyan-400 bg-gradient-to-r from-cyan-50 to-blue-50' : 'border-gray-200 bg-white hover:border-gray-300'"
              :style="{ animationDelay: `${index * 50}ms` }"
            >
              <!-- 排名 -->
              <div class="flex w-14 justify-center">
                <span v-if="item.ranking === 1" class="text-4xl">🥇</span>
                <span v-else-if="item.ranking === 2" class="text-4xl">🥈</span>
                <span v-else-if="item.ranking === 3" class="text-4xl">🥉</span>
                <span v-else class="flex h-10 w-10 items-center justify-center rounded-full bg-gray-200 text-lg font-bold text-gray-600">
                  {{ item.ranking }}
                </span>
              </div>

              <!-- 班级信息 -->
              <div class="flex-1">
                <div class="flex items-center gap-2">
                  <span class="text-lg font-bold text-gray-900">{{ item.className }}</span>
                  <span
                    v-if="!item.className.includes('班级')"
                    class="rounded-full bg-cyan-600 px-2 py-0.5 text-xs font-medium text-white"
                  >
                    本班
                  </span>
                  <span
                    class="rounded-full px-2 py-0.5 text-xs font-medium"
                    :class="getLevelTagClass(item.scoreLevel)"
                  >
                    {{ item.scoreLevel }}
                  </span>
                </div>
                <div v-if="item.teacherName" class="mt-1 flex items-center gap-1 text-sm text-gray-500">
                  <User class="h-4 w-4" />
                  班主任: {{ item.teacherName }}
                </div>
              </div>

              <!-- 分数 -->
              <div class="text-center">
                <div class="text-2xl font-bold" :class="getScoreColorClass(item.totalScore)">
                  {{ item.totalScore.toFixed(2) }}
                </div>
                <div class="text-xs text-gray-500">分</div>
              </div>

              <!-- 排名显示 -->
              <div class="flex items-center gap-2 text-gray-600">
                <TrendingUp class="h-4 w-4" />
                <span class="font-medium">排名 {{ item.ranking }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 照片查看器 Modal -->
    <Teleport to="body">
      <div
        v-if="photoDialogVisible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm"
        @click.self="photoDialogVisible = false"
      >
        <div class="animate-modal-in relative w-full max-w-4xl rounded-2xl bg-white shadow-2xl">
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h3 class="text-lg font-semibold text-gray-900">查看证据照片</h3>
            <button
              @click="photoDialogVisible = false"
              class="rounded-lg p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
            >
              <X class="h-5 w-5" />
            </button>
          </div>
          <div class="max-h-[70vh] overflow-auto p-6">
            <div class="grid grid-cols-2 gap-4 md:grid-cols-3">
              <div
                v-for="(url, index) in currentPhotos"
                :key="index"
                class="aspect-square cursor-pointer overflow-hidden rounded-lg"
                @click="previewImage(url)"
              >
                <img
                  :src="url"
                  class="h-full w-full object-cover transition-transform hover:scale-105"
                  :alt="`照片 ${index + 1}`"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 申诉对话框 -->
    <Teleport to="body">
      <div
        v-if="appealDialogVisible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
        @click.self="appealDialogVisible = false"
      >
        <div class="animate-modal-in w-full max-w-lg rounded-2xl bg-white shadow-2xl">
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h3 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
              <MessageSquare class="h-5 w-5 text-amber-600" />
              提交申诉
            </h3>
            <button
              @click="appealDialogVisible = false"
              class="rounded-lg p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
            >
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="p-6">
            <div class="space-y-4">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  申诉理由 <span class="text-red-500">*</span>
                </label>
                <textarea
                  v-model="appealForm.reason"
                  rows="5"
                  maxlength="500"
                  placeholder="请详细说明申诉理由..."
                  class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-gray-900 transition-colors focus:border-amber-500 focus:outline-none focus:ring-2 focus:ring-amber-500/20"
                ></textarea>
                <div class="mt-1 text-right text-xs text-gray-400">
                  {{ appealForm.reason.length }}/500
                </div>
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">补充材料</label>
                <div class="rounded-lg border-2 border-dashed border-gray-200 p-4">
                  <div class="flex flex-wrap gap-2">
                    <div
                      v-for="(url, index) in appealForm.attachments"
                      :key="index"
                      class="group relative h-20 w-20 overflow-hidden rounded-lg"
                    >
                      <img :src="url" class="h-full w-full object-cover" />
                      <button
                        @click="removeAttachment(index)"
                        class="absolute inset-0 flex items-center justify-center bg-black/50 opacity-0 transition-opacity group-hover:opacity-100"
                      >
                        <X class="h-5 w-5 text-white" />
                      </button>
                    </div>
                    <label
                      v-if="appealForm.attachments.length < 3"
                      class="flex h-20 w-20 cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-300 text-gray-400 transition-colors hover:border-amber-500 hover:text-amber-500"
                    >
                      <Plus class="h-6 w-6" />
                      <input
                        type="file"
                        accept="image/*"
                        class="hidden"
                        @change="handleFileUpload"
                      />
                    </label>
                  </div>
                  <p class="mt-2 text-xs text-gray-500">可上传相关证明材料，最多3张</p>
                </div>
              </div>
            </div>
          </div>

          <div class="flex justify-end gap-3 border-t border-gray-100 px-6 py-4">
            <button
              @click="appealDialogVisible = false"
              class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              取消
            </button>
            <button
              @click="confirmAppeal"
              :disabled="submitting"
              class="flex items-center gap-2 rounded-lg bg-amber-500 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-amber-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Loader2 v-if="submitting" class="h-4 w-4 animate-spin" />
              提交申诉
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  GraduationCap,
  Loader2,
  ClipboardList,
  School,
  Minus,
  Trophy,
  FileText,
  TrendingUp,
  TrendingDown,
  ListChecks,
  BarChart3,
  Info,
  User,
  X,
  MessageSquare,
  Plus
} from 'lucide-vue-next'
import { getMyClassDetail } from '@/api/quantification'
import type { CheckRecordDetail, ClassStats } from '@/api/quantification'
import { createAppeal } from '@/api/appeal'

const route = useRoute()
const router = useRouter()

// 数据
const loading = ref(false)
const recordInfo = ref<Partial<CheckRecordDetail>>({})
const myClassStats = ref<ClassStats | null>(null)
const otherClasses = ref<ClassStats[]>([])

// 照片查看
const photoDialogVisible = ref(false)
const currentPhotos = ref<string[]>([])

// 申诉
const appealDialogVisible = ref(false)
const submitting = ref(false)
const appealForm = reactive({
  itemId: null as number | null,
  reason: '',
  attachments: [] as string[]
})

// 返回
const goBack = () => {
  router.back()
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const recordId = route.params.id as string
    const response = await getMyClassDetail(recordId)
    recordInfo.value = response
    myClassStats.value = response.myClassStats || null
    otherClasses.value = response.classStats || []

    // 将本班也加入排名列表
    if (myClassStats.value) {
      const myClassInList = {
        ...myClassStats.value,
        className: myClassStats.value.className
      }
      const allClasses = [...otherClasses.value, myClassInList]
      allClasses.sort((a, b) => a.ranking - b.ranking)
      otherClasses.value = allClasses
    }
  } catch (error) {
    console.error('加载数据失败', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 查看照片
const viewPhotos = (urls: string[]) => {
  currentPhotos.value = urls
  photoDialogVisible.value = true
}

// 预览图片
const previewImage = (url: string) => {
  window.open(url, '_blank')
}

// 提交申诉
const submitAppeal = (itemId: number) => {
  appealForm.itemId = itemId
  appealForm.reason = ''
  appealForm.attachments = []
  appealDialogVisible.value = true
}

// 处理文件上传
const handleFileUpload = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file && appealForm.attachments.length < 3) {
    const url = URL.createObjectURL(file)
    appealForm.attachments.push(url)
  }
  target.value = ''
}

// 移除附件
const removeAttachment = (index: number) => {
  appealForm.attachments.splice(index, 1)
}

// 确认申诉
const confirmAppeal = async () => {
  if (!appealForm.reason) {
    ElMessage.warning('请填写申诉理由')
    return
  }

  if (!appealForm.itemId) {
    ElMessage.error('缺少申诉项ID')
    return
  }

  submitting.value = true
  try {
    await createAppeal({
      recordId: Number(route.params.id),
      itemId: appealForm.itemId,
      reason: appealForm.reason,
      evidence: appealForm.attachments.join(','),
      appealType: 1,
      contactPhone: undefined
    })
    ElMessage.success('申诉提交成功，请等待审核')
    appealDialogVisible.value = false
    appealForm.reason = ''
    appealForm.attachments = []
    loadData()
  } catch (error: any) {
    console.error('申诉提交失败', error)
    ElMessage.error(error.message || '申诉提交失败')
  } finally {
    submitting.value = false
  }
}

// 获取等级标签样式
const getLevelTagClass = (level: string) => {
  const map: Record<string, string> = {
    '优秀': 'bg-green-100 text-green-700',
    '良好': 'bg-blue-100 text-blue-700',
    '一般': 'bg-amber-100 text-amber-700',
    '较差': 'bg-red-100 text-red-700'
  }
  return map[level] || 'bg-gray-100 text-gray-700'
}

// 获取排名颜色样式
const getRankingColorClass = (ranking: number) => {
  if (ranking <= 3) return 'text-green-600'
  if (ranking <= 10) return 'text-blue-600'
  if (ranking <= 20) return 'text-amber-600'
  return 'text-red-600'
}

// 获取分数颜色样式
const getScoreColorClass = (score: number) => {
  if (score === 0) return 'text-green-600'
  if (score < 3) return 'text-amber-600'
  return 'text-red-600'
}

// 初始化
onMounted(() => {
  loadData()
})
</script>

<style scoped>
@keyframes modal-in {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.animate-modal-in {
  animation: modal-in 0.2s ease-out;
}
</style>
