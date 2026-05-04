<template>
  <div class="export-center insp-shell">
    <header class="page-head">
      <div>
        <div class="insp-eyebrow">数据消费者 / Export Center</div>
        <h1 class="insp-display page-title">导出中心</h1>
      </div>
    </header>

    <hr class="insp-rule insp-rule--strong" />

    <div class="hint-banner">
      <span> 给 HR / 考核办 / 教务用 — 一键下载 Excel, 直接进考核系统</span>
    </div>

    <div class="export-grid">
      <!-- 排名报表 -->
      <el-card shadow="never" class="export-card">
        <template #header>
          <div class="card-head">
            <span class="card-title"> 周期排名报表</span>
            <el-tag size="small" type="info">XLSX</el-tag>
          </div>
        </template>
        <div class="card-desc">
          按日 daily_summaries 全量导出, 含目标 / 平均分 / 排名 / 等级 / 累计扣分.
          适合周/月度对外考核数据递交.
        </div>
        <div class="form-row">
          <el-select v-model="rank.projectId" placeholder="项目 (可选)" clearable size="small" class="w-full">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </div>
        <div class="form-row">
          <el-date-picker v-model="rank.range" type="daterange" value-format="YYYY-MM-DD"
            start-placeholder="开始" end-placeholder="结束" size="small" class="w-full" />
        </div>
        <div class="form-row">
          <span class="form-hint">默认导出最近 30 天 · 上限 5000 行</span>
        </div>
        <el-button type="primary" :icon="Download" :loading="loading.rank" class="w-full" @click="downloadRanking">
          下载 Excel
        </el-button>
      </el-card>

      <!-- 整改履约 -->
      <el-card shadow="never" class="export-card">
        <template #header>
          <div class="card-head">
            <span class="card-title"> 整改履约报表</span>
            <el-tag size="small" type="info">XLSX</el-tag>
          </div>
        </template>
        <div class="card-desc">
          整改单全量, 含案号 / 优先级 / 状态 / 截止 / 升级层级 / 关闭时间.
          适合 SLA 履约统计 + 部门整改 KPI.
        </div>
        <div class="form-row">
          <el-select v-model="corr.projectId" placeholder="项目 (可选)" clearable size="small" class="w-full">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </div>
        <div class="form-row">
          <el-select v-model="corr.status" placeholder="状态 (可选)" clearable size="small" class="w-full">
            <el-option v-for="s in CASE_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </div>
        <div class="form-row">
          <span class="form-hint">默认全部状态 · 上限 5000 行</span>
        </div>
        <el-button type="primary" :icon="Download" :loading="loading.corr" class="w-full" @click="downloadCorrective">
          下载 Excel
        </el-button>
      </el-card>

      <!-- 申诉记录 -->
      <el-card shadow="never" class="export-card">
        <template #header>
          <div class="card-head">
            <span class="card-title"> 申诉处理记录</span>
            <el-tag size="small" type="info">XLSX</el-tag>
          </div>
        </template>
        <div class="card-desc">
          申诉全量, 含申诉人 / 理由 / 状态 / 期望与最终调整 / 审核员意见.
          适合透明度 KPI + 季度复盘.
        </div>
        <div class="form-row">
          <span class="form-hint">默认全量 · 上限 5000 行 · 按提交时间倒序</span>
        </div>
        <el-button type="primary" :icon="Download" :loading="loading.appeal" class="w-full" @click="downloadAppeals">
          下载 Excel
        </el-button>
      </el-card>

      <!-- 审计日志 -->
      <el-card shadow="never" class="export-card">
        <template #header>
          <div class="card-head">
            <span class="card-title"> 审计日志</span>
            <el-tag size="small" type="info">XLSX</el-tag>
          </div>
        </template>
        <div class="card-desc">
          检查平台所有变更操作, 含操作人 / 时间 / 聚合 / 操作 / 备注.
          适合合规审计 / 异常追溯.
        </div>
        <div class="form-row">
          <el-select v-model="audit.aggregateType" placeholder="聚合类型 (可选)" clearable size="small" class="w-full">
            <el-option label="任务 (InspTask)" value="InspTask" />
            <el-option label="申诉 (InspAppeal)" value="InspAppeal" />
            <el-option label="项目 (InspProject)" value="InspProject" />
            <el-option label="整改 (CorrectiveCase)" value="CorrectiveCase" />
          </el-select>
        </div>
        <div class="form-row">
          <span class="form-hint">默认全部 · 上限 5000 行 · 倒序</span>
        </div>
        <el-button type="primary" :icon="Download" :loading="loading.audit" class="w-full" @click="downloadAudit">
          下载 Excel
        </el-button>
      </el-card>
    </div>

    <!-- API 直连提示 -->
    <el-card shadow="never" class="api-hint">
      <template #header>
        <div class="card-head"><span> 给考核系统直接调用</span></div>
      </template>
      <div class="api-row">
        <code>GET /api/inspection/export/ranking?projectId=&amp;startDate=&amp;endDate=</code>
        <span>带 Bearer token 即可直接 download .xlsx</span>
      </div>
      <div class="api-row">
        <code>GET /api/inspection/export/corrective?projectId=&amp;status=</code>
      </div>
      <div class="api-row">
        <code>GET /api/inspection/export/appeals</code>
      </div>
      <div class="api-row">
        <code>GET /api/inspection/export/audit?aggregateType=</code>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { useInspExecutionStore } from '@/stores/inspection/inspExecutionStore'

const executionStore = useInspExecutionStore()
const projects = ref<{ id: number; projectName: string }[]>([])

const rank = reactive({ projectId: null as number | null, range: null as [string, string] | null })
const corr = reactive({ projectId: null as number | null, status: null as string | null })
const audit = reactive({ aggregateType: null as string | null })

const loading = reactive({ rank: false, corr: false, appeal: false, audit: false })

const CASE_STATUSES = [
  { value: 'OPEN', label: '待分配' },
  { value: 'ASSIGNED', label: '已分配' },
  { value: 'IN_PROGRESS', label: '整改中' },
  { value: 'SUBMITTED', label: '待验证' },
  { value: 'VERIFIED', label: '已验证' },
  { value: 'CLOSED', label: '已关闭' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'ESCALATED', label: '已升级' },
]

async function loadProjects() {
  await executionStore.loadProjects()
  projects.value = (executionStore.projects || []).map((p: any) => ({ id: p.id, projectName: p.projectName }))
}

/** 通用下载 — 带 auth header, 服务器返回 .xlsx blob */
async function download(url: string, params: any, fileName: string, key: keyof typeof loading) {
  loading[key] = true
  try {
    const blob = await http.get<Blob>(url, { params, responseType: 'blob' as any })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob as any)
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    setTimeout(() => URL.revokeObjectURL(link.href), 1000)
    ElMessage.success('已开始下载')
  } catch (e: any) {
    ElMessage.error('下载失败: ' + (e?.message || '未知'))
  } finally {
    loading[key] = false
  }
}

function downloadRanking() {
  const params: any = {}
  if (rank.projectId) params.projectId = rank.projectId
  if (rank.range) { params.startDate = rank.range[0]; params.endDate = rank.range[1] }
  const today = new Date().toISOString().slice(0, 10)
  download('/inspection/export/ranking', params, `排名_${today}.xlsx`, 'rank')
}
function downloadCorrective() {
  const params: any = {}
  if (corr.projectId) params.projectId = corr.projectId
  if (corr.status) params.status = corr.status
  const today = new Date().toISOString().slice(0, 10)
  download('/inspection/export/corrective', params, `整改履约_${today}.xlsx`, 'corr')
}
function downloadAppeals() {
  const today = new Date().toISOString().slice(0, 10)
  download('/inspection/export/appeals', {}, `申诉记录_${today}.xlsx`, 'appeal')
}
function downloadAudit() {
  const params: any = {}
  if (audit.aggregateType) params.aggregateType = audit.aggregateType
  const today = new Date().toISOString().slice(0, 10)
  download('/inspection/export/audit', params, `审计日志_${today}.xlsx`, 'audit')
}

onMounted(loadProjects)
</script>

<style scoped>
.export-center { padding: 32px 48px 64px; max-width: 1500px; margin: 0 auto; min-height: 100vh; background: var(--insp-bg-page); }

.page-head { margin-bottom: 16px; }
.page-title { font-size: 44px; margin: 0; font-weight: 500; }

.hint-banner {
  background: linear-gradient(90deg, #eff6ff, #dbeafe);
  padding: 12px 18px; border-radius: 10px; color: #1e40af;
  font-size: 13px; margin: 24px 0;
}

.export-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 16px; }
.export-card .card-head { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: 600; font-size: 15px; }
.card-desc { font-size: 13px; color: #64748b; line-height: 1.6; margin-bottom: 12px; min-height: 60px; }

.form-row { margin-bottom: 8px; }
.w-full { width: 100%; }
.form-hint { font-size: 11px; color: #94a3b8; }

/* API 提示 */
.api-hint { margin-top: 16px; }
.api-row { display: flex; gap: 12px; align-items: center; padding: 6px 0; font-size: 12px; flex-wrap: wrap; }
.api-row code {
  background: #f1f5f9; padding: 4px 10px; border-radius: 4px;
  font-family: 'Consolas', monospace; font-size: 11.5px; color: #1e40af;
}
.api-row span { color: #94a3b8; font-size: 11px; }
</style>
