<template>
  <div class="behavior-record-list">
    <!-- Tabs -->
    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="违规记录" name="violations" />
        <el-tab-pane label="表扬记录" name="commendations" />
        <el-tab-pane name="alerts">
          <template #label>
            预警管理
            <el-badge v-if="unhandledCount > 0" :value="unhandledCount" :max="99" class="alert-badge" />
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- Filters -->
      <el-row :gutter="16" class="filter-row">
        <el-col :span="6" v-if="activeTab !== 'alerts'">
          <el-select v-model="filterCategory" placeholder="分类筛选" clearable @change="loadData">
            <el-option v-for="(cfg, key) in BehaviorCategoryConfig" :key="key" :label="cfg.label" :value="key" />
          </el-select>
        </el-col>
        <el-col :span="6" v-if="activeTab !== 'alerts'">
          <el-input v-model="searchClassId" placeholder="班级ID" clearable @change="loadData" />
        </el-col>
        <el-col :span="6" v-if="activeTab !== 'alerts'">
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon> 新建记录
          </el-button>
        </el-col>
      </el-row>

      <!-- Violations / Commendations Table -->
      <el-table v-if="activeTab !== 'alerts'" :data="filteredRecords" stripe v-loading="loading">
        <el-table-column prop="title" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="80">
          <template #default="{ row }">
            {{ BehaviorCategoryConfig[row.category as BehaviorCategory]?.label || row.category }}
          </template>
        </el-table-column>
        <el-table-column prop="behaviorType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="BehaviorTypeConfig[row.behaviorType as BehaviorType]?.type" size="small">
              {{ BehaviorTypeConfig[row.behaviorType as BehaviorType]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deductionAmount" label="分值" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="BehaviorStatusConfig[row.status as BehaviorStatus]?.type" size="small">
              {{ BehaviorStatusConfig[row.status as BehaviorStatus]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recordedAt" label="记录时间" width="160">
          <template #default="{ row }">{{ formatDate(row.recordedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showStudentProfile(row.studentId)">学生画像</el-button>
            <el-button link type="warning" v-if="row.status === 'NOTIFIED'" @click="handleAcknowledge(row)">确认</el-button>
            <el-button link type="success" v-if="row.status === 'ACKNOWLEDGED'" @click="showResolveDialog(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Alerts Table -->
      <el-table v-if="activeTab === 'alerts'" :data="alerts" stripe v-loading="loading">
        <el-table-column prop="title" label="预警标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="alertType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="AlertTypeConfig[row.alertType as AlertType]?.type" size="small">
              {{ AlertTypeConfig[row.alertType as AlertType]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertLevel" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="row.alertLevel === 'DANGER' ? 'danger' : row.alertLevel === 'WARNING' ? 'warning' : 'info'" size="small">
              {{ row.alertLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isHandled" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isHandled ? 'success' : 'danger'" size="small">
              {{ row.isHandled ? '已处理' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-if="!row.isRead" @click="handleMarkRead(row)">标记已读</el-button>
            <el-button link type="success" v-if="!row.isHandled" @click="showHandleDialog(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Student Profile Dialog -->
    <el-dialog v-model="profileVisible" title="学生行为画像" width="500px">
      <template v-if="profile">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="总违规次数">
            <span class="text-danger">{{ profile.totalViolations }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="总表扬次数">
            <span class="text-success">{{ profile.totalCommendations }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="近30天违规">{{ profile.recentViolations }}</el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="profile.riskLevel === 'HIGH' ? 'danger' : profile.riskLevel === 'MEDIUM' ? 'warning' : 'success'" size="small">
              {{ profile.riskLevel === 'HIGH' ? '高' : profile.riskLevel === 'MEDIUM' ? '中' : '低' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="趋势" :span="2">
            {{ profile.trend === 'WORSENING' ? '恶化' : profile.trend === 'IMPROVING' ? '好转' : '稳定' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- Create Record Dialog -->
    <el-dialog v-model="showCreateDialog" title="新建行为记录" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="学生ID" required>
          <el-input v-model.number="createForm.studentId" type="number" />
        </el-form-item>
        <el-form-item label="班级ID" required>
          <el-input v-model.number="createForm.classId" type="number" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="createForm.behaviorType">
            <el-radio value="VIOLATION">违规</el-radio>
            <el-radio value="COMMENDATION">表扬</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="createForm.category">
            <el-option v-for="(cfg, key) in BehaviorCategoryConfig" :key="key" :label="cfg.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" required>
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number v-model="createForm.deductionAmount" :min="0" :precision="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="submitting">创建</el-button>
      </template>
    </el-dialog>

    <!-- Resolve Dialog -->
    <el-dialog v-model="resolveVisible" title="处理行为记录" width="400px">
      <el-form label-width="80px">
        <el-form-item label="处理说明" required>
          <el-input v-model="resolveNote" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResolve" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>

    <!-- Handle Alert Dialog -->
    <el-dialog v-model="handleVisible" title="处理预警" width="400px">
      <el-form label-width="80px">
        <el-form-item label="处理备注" required>
          <el-input v-model="handleNote" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAlertAction" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listByClass, createBehaviorRecord, acknowledgeBehaviorRecord, resolveBehaviorRecord, getStudentProfile,
  listAlertsByClass, getUnhandledAlertCount, markAlertRead, handleAlert
} from '@/api/v2/behavior'
import type {
  BehaviorRecord, CreateBehaviorRecordRequest, BehaviorProfile, BehaviorAlert,
  BehaviorType, BehaviorCategory, BehaviorStatus, AlertType
} from '@/types/v2/behavior'
import {
  BehaviorTypeConfig, BehaviorStatusConfig, BehaviorCategoryConfig, AlertTypeConfig
} from '@/types/v2/behavior'

const loading = ref(false)
const submitting = ref(false)
const activeTab = ref('violations')
const records = ref<BehaviorRecord[]>([])
const alerts = ref<BehaviorAlert[]>([])
const unhandledCount = ref(0)
const filterCategory = ref('')
const searchClassId = ref('')
const profileVisible = ref(false)
const profile = ref<BehaviorProfile | null>(null)
const showCreateDialog = ref(false)
const resolveVisible = ref(false)
const handleVisible = ref(false)
const resolveNote = ref('')
const handleNote = ref('')
const currentId = ref(0)
const currentAlertId = ref(0)

const createForm = ref<CreateBehaviorRecordRequest>({
  studentId: 0,
  classId: 0,
  behaviorType: 'VIOLATION',
  source: 'TEACHER_REPORT',
  category: 'OTHER',
  title: '',
  deductionAmount: 0
})

const filteredRecords = computed(() => {
  let result = records.value
  if (activeTab.value === 'violations') {
    result = result.filter(r => r.behaviorType === 'VIOLATION')
  } else if (activeTab.value === 'commendations') {
    result = result.filter(r => r.behaviorType === 'COMMENDATION')
  }
  if (filterCategory.value) {
    result = result.filter(r => r.category === filterCategory.value)
  }
  return result
})

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadData() {
  loading.value = true
  try {
    const classId = searchClassId.value ? Number(searchClassId.value) : 1
    if (activeTab.value === 'alerts') {
      alerts.value = await listAlertsByClass(classId)
    } else {
      records.value = await listByClass(classId)
    }
    unhandledCount.value = await getUnhandledAlertCount(classId)
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  loadData()
}

async function showStudentProfile(studentId: number) {
  try {
    profile.value = await getStudentProfile(studentId)
    profileVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '获取学生画像失败')
  }
}

async function handleCreate() {
  if (!createForm.value.title.trim()) {
    ElMessage.warning('请填写描述')
    return
  }
  submitting.value = true
  try {
    await createBehaviorRecord(createForm.value)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

async function handleAcknowledge(row: BehaviorRecord) {
  try {
    await acknowledgeBehaviorRecord(row.id)
    ElMessage.success('已确认')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function showResolveDialog(row: BehaviorRecord) {
  currentId.value = row.id
  resolveNote.value = ''
  resolveVisible.value = true
}

async function handleResolve() {
  submitting.value = true
  try {
    await resolveBehaviorRecord(currentId.value, { resolutionNote: resolveNote.value })
    ElMessage.success('处理完成')
    resolveVisible.value = false
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '处理失败')
  } finally {
    submitting.value = false
  }
}

async function handleMarkRead(row: BehaviorAlert) {
  try {
    await markAlertRead(row.id)
    ElMessage.success('已标记已读')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function showHandleDialog(row: BehaviorAlert) {
  currentAlertId.value = row.id
  handleNote.value = ''
  handleVisible.value = true
}

async function handleAlertAction() {
  submitting.value = true
  try {
    await handleAlert(currentAlertId.value, { note: handleNote.value })
    ElMessage.success('处理完成')
    handleVisible.value = false
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '处理失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.filter-row {
  margin: 16px 0;
}
.text-danger {
  color: #f56c6c;
  font-weight: bold;
}
.text-success {
  color: #67c23a;
  font-weight: bold;
}
.alert-badge {
  margin-left: 4px;
}
</style>
