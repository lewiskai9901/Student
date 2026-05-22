<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, ArrowRight, Play, Send, SkipForward,
  Camera, MapPin, Wifi, WifiOff, RefreshCw, AlertTriangle, Check,
} from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/inspection/inspExecutionStore'
import { useOfflineSync } from '@/composables/inspection/useOfflineSync'
import { useCamera } from '@/composables/inspection/useCamera'
import { useGeolocation } from '@/composables/inspection/useGeolocation'
import { uploadImage } from '@/api/upload'
import { addEvidence } from '@/api/inspection/submission'
import {
  TaskStatusConfig, type TaskStatus,
  SubmissionStatusConfig, type SubmissionStatus,
} from '@/types/insp/enums'
import type { InspTask, InspSubmission, SubmissionDetail } from '@/types/insp/project'

const route = useRoute()
const router = useRouter()
const store = useInspExecutionStore()

const taskId = route.params.id as string

// Offline sync
const {
  isOnline, pendingCount, conflicts, isSyncing, lastSyncAt,
  saveDraft, pullFromServer, pushToServer, fullSync,
  resolveKeepLocal, resolveKeepServer,
} = useOfflineSync(taskId)

// Camera & GPS
const { capturePhoto } = useCamera()
const { loading: geoLoading, error: geoError, getCurrentPosition } = useGeolocation()

// State
const loading = ref(false)
const task = ref<InspTask | null>(null)
const submissions = ref<InspSubmission[]>([])
const currentIndex = ref(0)
const details = ref<SubmissionDetail[]>([])
const showConflictDialog = ref(false)

// Computed
const currentSubmission = computed(() => submissions.value[currentIndex.value] ?? null)
const taskProgress = computed(() => {
  if (!task.value || task.value.totalTargets === 0) return 0
  return Math.round(
    (task.value.completedTargets + task.value.skippedTargets) / task.value.totalTargets * 100
  )
})
const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < submissions.value.length - 1)

const statusBarClass = computed(() => {
  if (!isOnline.value) return 'bg-red-500'
  if (pendingCount.value > 0) return 'bg-amber-500'
  return 'bg-green-500'
})

const statusBarText = computed(() => {
  if (!isOnline.value) return '离线模式'
  if (isSyncing.value) return '同步中...'
  if (pendingCount.value > 0) return `待同步 ${pendingCount.value} 项`
  return '已同步'
})

// Actions
async function loadData() {
  loading.value = true
  try {
    task.value = await store.loadTask(taskId)
    submissions.value = await store.loadSubmissions(taskId)
    if (currentSubmission.value) {
      details.value = await store.loadDetails(currentSubmission.value.id)
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

watch(currentIndex, async () => {
  if (currentSubmission.value) {
    try {
      details.value = await store.loadDetails(currentSubmission.value.id)
    } catch (e) { console.warn('Load submission details failed', e); details.value = [] }
  }
})

function goPrev() { if (hasPrev.value) currentIndex.value-- }
function goNext() { if (hasNext.value) currentIndex.value++ }

async function handleStartTask() {
  try {
    await store.startTask(taskId)
    ElMessage.success('任务已开始')
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

async function handleSubmitTask() {
  try {
    await ElMessageBox.confirm('确定提交该任务？', '确认提交', { type: 'warning' })
    // Push pending offline changes first
    if (pendingCount.value > 0 && isOnline.value) {
      await pushToServer()
    }
    await store.submitTask(taskId)
    ElMessage.success('任务已提交')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('提交失败: ' + (e?.message || '未知错误'))
  }
}

async function handleStartFilling() {
  if (!currentSubmission.value) return
  try {
    await store.startFillingSubmission(currentSubmission.value.id)
    ElMessage.success('开始填写')
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

async function handleSkipSubmission() {
  if (!currentSubmission.value) return
  try {
    await ElMessageBox.confirm(`跳过「${currentSubmission.value.targetName}」？`, '确认')
    await store.skipSubmission(currentSubmission.value.id)
    ElMessage.success('已跳过')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('跳过失败: ' + (e?.message || '未知错误'))
  }
}

async function handleSync() {
  try {
    const results = await fullSync()
    const conflictCount = results.filter(r => r.status === 'CONFLICT').length
    if (conflictCount > 0) {
      showConflictDialog.value = true
      ElMessage.warning(`${conflictCount} 条数据有冲突，请处理`)
    } else {
      ElMessage.success('同步完成')
    }
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '同步失败')
  }
}

// 上传中状态 (拍照/定位共用, 防重复点击)
const capturing = ref(false)

async function handleTakePhoto() {
  if (!currentSubmission.value) {
    ElMessage.warning('请先选择检查目标')
    return
  }
  const file = await capturePhoto()
  if (!file) return
  capturing.value = true
  try {
    // 1. 上传图片文件拿到 URL
    const uploaded = await uploadImage(file)
    // 2. 关联为当前提交的证据
    await addEvidence(currentSubmission.value.id, {
      evidenceType: 'PHOTO',
      fileName: uploaded.name || file.name,
      fileUrl: uploaded.url,
    })
    ElMessage.success('照片已上传并关联')
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '未知错误'
    ElMessage.error('照片上传失败: ' + msg)
  } finally {
    capturing.value = false
  }
}

async function handleGetLocation() {
  if (!currentSubmission.value) {
    ElMessage.warning('请先选择检查目标')
    return
  }
  const pos = await getCurrentPosition()
  if (!pos) {
    // useGeolocation 已设置 error, 给出可见反馈
    ElMessage.error('定位失败: ' + (geoError.value || '无法获取位置'))
    return
  }
  capturing.value = true
  try {
    const coord = `${pos.latitude.toFixed(6)}, ${pos.longitude.toFixed(6)}`
    // 以地图链接作为 fileUrl 落库, 坐标写入 fileName, evidenceType=GPS_POINT
    await addEvidence(currentSubmission.value.id, {
      evidenceType: 'GPS_POINT',
      fileName: `GPS ${coord}`,
      fileUrl: `https://uri.amap.com/marker?position=${pos.longitude},${pos.latitude}`,
    })
    ElMessage.success(`定位已记录: ${coord}`)
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '未知错误'
    ElMessage.error('定位记录失败: ' + msg)
  } finally {
    capturing.value = false
  }
}

async function handleResolveConflict(submissionId: LongId, keepLocal: boolean) {
  if (keepLocal) {
    await resolveKeepLocal(submissionId)
  } else {
    await resolveKeepServer(submissionId)
  }
  if (conflicts.value.length === 0) {
    showConflictDialog.value = false
  }
  ElMessage.success('冲突已解决')
}

function goBack() {
  router.push('/inspection/tasks')
}

// 冲突对话框: 字段级 diff, 取代暴力 substring(0,200) 截断
interface FieldDiff { key: string; local: string; server: string }
function computeConflictDiff(localStr?: string | null, serverStr?: string | null): FieldDiff[] {
  let local: Record<string, any> = {}
  let server: Record<string, any> = {}
  try { local = JSON.parse(localStr || '{}') || {} } catch { local = {} }
  try { server = JSON.parse(serverStr || '{}') || {} } catch { server = {} }
  const keys = new Set([...Object.keys(local), ...Object.keys(server)])
  const diffs: FieldDiff[] = []
  for (const k of keys) {
    const lv = local[k]
    const sv = server[k]
    const ls = lv == null ? '' : String(lv)
    const ss = sv == null ? '' : String(sv)
    if (ls !== ss) diffs.push({ key: k, local: ls || '（空）', server: ss || '（空）' })
  }
  return diffs
}

onMounted(async () => {
  await loadData()
  // Initial sync if online
  if (isOnline.value) {
    pullFromServer().catch(() => {})
  }
})
</script>

<template>
  <!-- 移动端居中容器: 桌面浏览器看也是手机宽度, 真机 480px 以下全宽 -->
  <div class="mobile-shell">
  <div class="flex flex-col h-screen bg-gray-50">
    <!-- 同步状态栏 -->
    <div :class="[statusBarClass, 'text-white text-xs px-4 py-1.5 flex items-center justify-between']">
      <div class="flex items-center gap-1.5">
        <Wifi v-if="isOnline" class="w-3 h-3" />
        <WifiOff v-else class="w-3 h-3" />
        <span>{{ statusBarText }}</span>
      </div>
      <button
        v-if="isOnline && (pendingCount > 0 || conflicts.length > 0)"
        class="flex items-center gap-1 text-white/90 hover:text-white"
        @click="handleSync"
        :disabled="isSyncing"
      >
        <RefreshCw class="w-3 h-3" :class="{ 'animate-spin': isSyncing }" />
        <span>同步</span>
      </button>
      <span v-if="lastSyncAt" class="text-white/70">{{ lastSyncAt?.substring(11, 16) }}</span>
    </div>

    <!-- 顶部导航 -->
    <div class="bg-white border-b px-4 py-3 flex items-center justify-between">
      <div class="flex items-center gap-2">
        <button @click="goBack" class="text-gray-500 hover:text-gray-800">
          <ArrowLeft class="w-5 h-5" />
        </button>
        <div>
          <div class="text-sm font-semibold truncate max-w-[200px]">{{ task?.taskCode || '加载中' }}</div>
          <div class="text-xs text-gray-400">{{ task?.taskDate }}</div>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <el-tag
          v-if="task"
          :type="(TaskStatusConfig[task.status as TaskStatus]?.type as any)"
          size="small"
        >
          {{ TaskStatusConfig[task.status as TaskStatus]?.label }}
        </el-tag>
        <el-button v-if="task?.status === 'CLAIMED'" size="small" type="primary" @click="handleStartTask">
          <Play class="w-3.5 h-3.5 mr-1" />开始
        </el-button>
        <el-button v-if="task?.status === 'IN_PROGRESS'" size="small" type="warning" @click="handleSubmitTask">
          <Send class="w-3.5 h-3.5 mr-1" />提交
        </el-button>
      </div>
    </div>

    <!-- 进度条 -->
    <div class="bg-white px-4 py-2 border-b">
      <el-progress :percentage="taskProgress" :stroke-width="4" :show-text="false" />
      <div class="flex justify-between text-xs text-gray-400 mt-1">
        <span>{{ task?.completedTargets ?? 0 }}/{{ task?.totalTargets ?? 0 }} 已完成</span>
        <span>{{ currentIndex + 1 }}/{{ submissions.length }}</span>
      </div>
    </div>

    <!-- 主体内容：当前目标 -->
    <div class="flex-1 overflow-y-auto" v-loading="loading">
      <template v-if="currentSubmission">
        <!-- 目标卡片 -->
        <div class="bg-white mx-4 mt-4 rounded-lg shadow-sm border">
          <div class="px-4 py-3 border-b flex items-center justify-between">
            <div>
              <div class="font-medium text-base">{{ currentSubmission.targetName }}</div>
              <div class="text-xs text-gray-400 mt-0.5">
                {{ currentSubmission.orgUnitName || '' }}
              </div>
            </div>
            <el-tag
              :type="(SubmissionStatusConfig[currentSubmission.status as SubmissionStatus]?.type as any)"
              size="small"
            >
              {{ SubmissionStatusConfig[currentSubmission.status as SubmissionStatus]?.label }}
            </el-tag>
          </div>

          <!-- 操作按钮 -->
          <div class="px-4 py-3 flex flex-wrap gap-2">
            <el-button
              v-if="currentSubmission.status === 'PENDING' || currentSubmission.status === 'LOCKED'"
              size="small" type="primary" @click="handleStartFilling"
            >
              <Play class="w-3.5 h-3.5 mr-1" />开始填写
            </el-button>
            <el-button
              v-if="currentSubmission.status === 'PENDING' || currentSubmission.status === 'LOCKED'"
              size="small" @click="handleSkipSubmission"
            >
              <SkipForward class="w-3.5 h-3.5 mr-1" />跳过
            </el-button>
            <el-button size="small" @click="handleTakePhoto" :loading="capturing">
              <Camera class="w-3.5 h-3.5 mr-1" />拍照取证
            </el-button>
            <el-button size="small" @click="handleGetLocation" :loading="geoLoading || capturing">
              <MapPin class="w-3.5 h-3.5 mr-1" />定位取证
            </el-button>
          </div>

          <!-- 分数概要 -->
          <div v-if="currentSubmission.finalScore != null" class="px-4 py-2 bg-gray-50 border-t text-sm">
            <div class="flex items-center gap-4">
              <span>得分: <strong>{{ currentSubmission.finalScore }}</strong></span>
              <span v-if="currentSubmission.grade">等级: <strong>{{ currentSubmission.grade }}</strong></span>
              <el-tag
                v-if="currentSubmission.passed != null"
                :type="currentSubmission.passed ? 'success' : 'danger'"
                size="small"
              >
                {{ currentSubmission.passed ? '通过' : '不通过' }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 检查明细列表 (卡片式，适合移动) -->
        <div class="px-4 py-3 space-y-2">
          <div class="flex items-center justify-between mb-1">
            <span class="text-xs text-gray-500 font-medium">检查项 ({{ details.length }})</span>
            <!-- 移动端为只读查看视图: 打分请在桌面执行页完成 -->
            <span class="text-[11px] text-gray-400 bg-gray-100 rounded px-1.5 py-0.5">仅查看 · 打分请用桌面执行页</span>
          </div>
          <div
            v-for="detail in details"
            :key="detail.id"
            class="bg-white rounded-lg border p-3"
            :class="{ 'border-l-2 border-l-red-400': detail.isFlagged }"
          >
            <div class="flex items-start justify-between">
              <div class="text-sm font-medium">{{ detail.itemName }}</div>
              <div class="flex items-center gap-1.5">
                <span v-if="detail.score != null" class="text-xs bg-blue-50 text-blue-600 px-1.5 py-0.5 rounded">
                  {{ detail.score }}分
                </span>
                <el-tag v-if="detail.isFlagged" type="danger" size="small">异常</el-tag>
              </div>
            </div>
            <div class="text-xs text-gray-400 mt-1">
              <span>{{ detail.itemType }}</span>
              <span v-if="detail.sectionName" class="ml-2">{{ detail.sectionName }}</span>
            </div>
            <div v-if="detail.responseValue" class="text-sm mt-1.5 text-gray-600 bg-gray-50 px-2 py-1 rounded">
              {{ detail.responseValue }}
            </div>
          </div>
          <div v-if="details.length === 0" class="text-center text-gray-400 py-8">
            <div class="text-sm">暂无检查明细</div>
            <div class="text-xs mt-1 text-gray-300">
              点击上方"开始填写"后，检查项将自动加载
            </div>
          </div>
        </div>
      </template>

      <div v-else class="flex flex-col items-center justify-center h-full text-gray-400 px-8 text-center">
        <span class="text-sm">暂无检查目标</span>
        <span class="text-xs mt-1 text-gray-300">
          该任务还未分配检查目标，请联系任务管理员，或返回任务列表重新进入
        </span>
        <el-button class="mt-3" size="small" @click="goBack">返回任务列表</el-button>
      </div>
    </div>

    <!-- 底部导航栏 -->
    <div class="bg-white border-t px-4 py-3 flex items-center justify-between">
      <el-button :disabled="!hasPrev" @click="goPrev" size="default">
        <ArrowLeft class="w-4 h-4 mr-1" />上一个
      </el-button>
      <div class="text-sm text-gray-500">
        {{ currentIndex + 1 }} / {{ submissions.length }}
      </div>
      <el-button :disabled="!hasNext" @click="goNext" size="default">
        下一个<ArrowRight class="w-4 h-4 ml-1" />
      </el-button>
    </div>

    <!-- 冲突解决对话框 -->
    <el-dialog v-model="showConflictDialog" title="数据冲突" width="95%" :close-on-click-modal="false">
      <div class="space-y-4">
        <div v-for="conflict in conflicts" :key="conflict.submissionId" class="border rounded-lg p-3">
          <div class="text-sm font-medium mb-2">
            <AlertTriangle class="w-4 h-4 inline text-amber-500 mr-1" />
            提交 #{{ conflict.submissionId }}
          </div>
          <!-- 字段级 diff: 仅展示有差异的字段 -->
          <div class="text-xs">
            <div class="grid grid-cols-[1fr_1fr] gap-1 mb-1 font-medium">
              <span class="text-blue-600">本地版本 (v{{ conflict.localSyncVersion }})</span>
              <span class="text-green-600">服务器版本 (v{{ conflict.serverSyncVersion }})</span>
            </div>
            <template v-if="computeConflictDiff(conflict.localFormData, conflict.serverFormData).length">
              <div
                v-for="diff in computeConflictDiff(conflict.localFormData, conflict.serverFormData)"
                :key="diff.key"
                class="mb-1"
              >
                <div class="text-gray-400">{{ diff.key }}</div>
                <div class="grid grid-cols-[1fr_1fr] gap-1">
                  <span class="bg-blue-50 px-1.5 py-0.5 rounded text-gray-700 break-all">{{ diff.local }}</span>
                  <span class="bg-green-50 px-1.5 py-0.5 rounded text-gray-700 break-all">{{ diff.server }}</span>
                </div>
              </div>
            </template>
            <div v-else class="text-gray-400 py-1">两个版本内容相同（仅版本号不同）</div>
          </div>
          <div class="flex gap-2 mt-2">
            <el-button size="small" type="primary" @click="handleResolveConflict(conflict.submissionId, true)">
              保留本地
            </el-button>
            <el-button size="small" @click="handleResolveConflict(conflict.submissionId, false)">
              使用服务器
            </el-button>
          </div>
        </div>
        <div v-if="conflicts.length === 0" class="text-center text-sm text-gray-400 py-4">
          <Check class="w-5 h-5 inline text-green-500 mr-1" />所有冲突已解决
        </div>
      </div>
    </el-dialog>
  </div>
  </div>
</template>

<style scoped>
.mobile-shell {
  max-width: 480px;
  margin: 0 auto;
  height: 100vh;
  background: #f9fafb;
  box-shadow: 0 0 32px rgba(0,0,0,0.06);
  position: relative;
}
@media (min-width: 481px) {
  .mobile-shell {
    border-left: 1px solid #e5e7eb;
    border-right: 1px solid #e5e7eb;
  }
}
@media (max-width: 480px) {
  .mobile-shell {
    max-width: 100%;
    box-shadow: none;
  }
}
</style>
