<template>
  <div>
    <!-- Action buttons -->
    <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px;">
      <button class="tm-btn tm-btn-primary" :disabled="feasibilityLoading" @click="runFeasibilityCheck">{{ feasibilityLoading ? '检测中...' : '运行可行性检测' }}</button>
      <button class="tm-btn tm-btn-secondary" :disabled="conflictDetecting" @click="runConflictDetection">{{ conflictDetecting ? '检测中...' : '检测排课冲突' }}</button>
    </div>

    <!-- Feasibility report -->
    <div v-if="feasibilityReport" style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 20px; margin-bottom: 16px;">
      <h3 style="font-size: 13px; font-weight: 600; color: #374151; margin: 0 0 10px 0;">可行性报告</h3>
      <div class="tm-stats" style="margin-top: 0; margin-bottom: 12px;">
        <span style="color: #16a34a;">通过 <b>{{ feasibilityReport.passedChecks }}</b> 项</span>
        <span class="sep" />
        <span style="color: #dc2626;">阻塞 <b>{{ feasibilityReport.blockingIssues.length }}</b> 个</span>
        <span class="sep" />
        <span style="color: #d97706;">警告 <b>{{ feasibilityReport.warnings.length }}</b> 个</span>
      </div>
      <div v-if="feasibilityReport.blockingIssues.length > 0" style="margin-bottom: 10px;">
        <div style="font-size: 11px; font-weight: 600; color: #dc2626; margin-bottom: 6px;">阻塞项</div>
        <div v-for="(issue, idx) in feasibilityReport.blockingIssues" :key="'b'+idx" style="padding: 6px 10px; background: #fef2f2; border: 1px solid #fecaca; border-radius: 6px; font-size: 11px; color: #991b1b; margin-bottom: 4px;">
          <b>[{{ issue.type }}]</b> {{ issue.target }}: {{ issue.description }}
          <span v-if="issue.suggestion" style="margin-left: 8px; color: #dc2626;">-- {{ issue.suggestion }}</span>
        </div>
      </div>
      <div v-if="feasibilityReport.warnings.length > 0">
        <div style="font-size: 11px; font-weight: 600; color: #d97706; margin-bottom: 6px;">警告项</div>
        <div v-for="(issue, idx) in feasibilityReport.warnings" :key="'w'+idx" style="padding: 6px 10px; background: #fffbeb; border: 1px solid #fde68a; border-radius: 6px; font-size: 11px; color: #92400e; margin-bottom: 4px;">
          <b>[{{ issue.type }}]</b> {{ issue.target }}: {{ issue.description }}
        </div>
      </div>
    </div>

    <!-- Conflict list -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden;">
      <div style="display: flex; align-items: center; justify-content: space-between; padding: 12px 20px;">
        <h2 style="font-size: 13px; font-weight: 600; color: #374151; margin: 0;">冲突列表</h2>
        <select v-model="conflictStatusFilter" class="tm-select" @change="loadConflicts">
          <option :value="undefined">全部状态</option>
          <option :value="0">未处理</option>
          <option :value="1">已解决</option>
          <option :value="2">已忽略</option>
        </select>
      </div>
      <table class="tm-table" style="border-radius: 0; border-left: none; border-right: none; border-bottom: none;">
        <colgroup>
          <col style="width: 90px" />
          <col style="width: 80px" />
          <col style="width: 110px" />
          <col />
          <col style="width: 80px" />
          <col style="width: 120px" />
        </colgroup>
        <thead>
          <tr>
            <th>类型</th>
            <th>严重度</th>
            <th>冲突类别</th>
            <th class="text-left">描述</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="conflictsLoading">
            <td colspan="6" class="tm-empty"><span class="tm-spin" style="display:inline-block;width:16px;height:16px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...</td>
          </tr>
          <tr v-else-if="conflicts.length === 0">
            <td colspan="6" class="tm-empty">暂无冲突</td>
          </tr>
          <tr v-for="row in conflicts" :key="row.id">
            <td><span :class="['tm-chip', { 1:'tm-chip-red', 2:'tm-chip-amber', 3:'tm-chip-blue' }[row.conflictCategory] || 'tm-chip-gray']">{{ getConflictCategoryName(row.conflictCategory) }}</span></td>
            <td><span :class="['tm-chip', { 1:'tm-chip-gray', 2:'tm-chip-amber', 3:'tm-chip-red' }[row.severity] || 'tm-chip-gray']">{{ getSeverityName(row.severity) }}</span></td>
            <td>{{ row.conflictType }}</td>
            <td class="text-left" style="white-space: normal !important; font-size: 12px;">{{ row.description }}</td>
            <td><span :class="['tm-chip', { 0:'tm-chip-red', 1:'tm-chip-green', 2:'tm-chip-gray' }[row.resolutionStatus] || 'tm-chip-gray']">{{ getResolutionStatusName(row.resolutionStatus) }}</span></td>
            <td>
              <template v-if="row.resolutionStatus === 0">
                <button class="tm-action" style="color: #16a34a;" @click="resolveConflict(row)">解决</button>
                <button class="tm-action" style="color: #d97706;" @click="ignoreConflict(row)">忽略</button>
              </template>
              <span v-else style="font-size: 11px; color: #9ca3af;">{{ row.resolutionNote || '-' }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Resolve Drawer -->
    <Transition name="tm-drawer">
      <div v-if="resolveDialogVisible" class="tm-drawer-overlay" @click.self="resolveDialogVisible = false">
        <div class="tm-drawer" style="width: 380px;">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">处理冲突</h3>
            <button class="tm-drawer-close" @click="resolveDialogVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div class="tm-section">
              <div class="tm-field">
                <label class="tm-label">{{ resolveAction === 'resolve' ? '解决方案' : '忽略原因' }}</label>
                <textarea v-model="resolveNote" class="tm-textarea" rows="3" :placeholder="resolveAction === 'resolve' ? '请描述解决方案' : '请说明忽略原因'"></textarea>
              </div>
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" @click="resolveDialogVisible = false">取消</button>
            <button class="tm-btn tm-btn-primary" :disabled="resolveSaving" @click="confirmResolve">确认</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { conflictApi } from '@/api/teaching'
import type { DetectedConflict, FeasibilityReport } from '@/types/teaching'

const props = defineProps<{ semesterId: number | string | undefined }>()

const conflicts = ref<DetectedConflict[]>([])
const conflictsLoading = ref(false)
const conflictDetecting = ref(false)
const feasibilityLoading = ref(false)
const feasibilityReport = ref<FeasibilityReport | null>(null)
const conflictStatusFilter = ref<number | undefined>(undefined)

const resolveDialogVisible = ref(false)
const resolveNote = ref('')
const resolveAction = ref<'resolve' | 'ignore'>('resolve')
const resolveTargetId = ref<number>()
const resolveSaving = ref(false)

async function loadConflicts() {
  if (!props.semesterId) return
  conflictsLoading.value = true
  try { const res = await conflictApi.list({ semesterId: props.semesterId, status: conflictStatusFilter.value }); conflicts.value = (res as any).data || res }
  catch { /* */ } finally { conflictsLoading.value = false }
}

async function runFeasibilityCheck() {
  if (!props.semesterId) return
  feasibilityLoading.value = true
  try { const res = await conflictApi.feasibilityCheck(props.semesterId); feasibilityReport.value = (res as any).data || res; ElMessage.success('可行性检测完成') }
  catch { ElMessage.error('检测失败') } finally { feasibilityLoading.value = false }
}

async function runConflictDetection() {
  if (!props.semesterId) return
  conflictDetecting.value = true
  try { const res = await conflictApi.detect(props.semesterId); const d = (res as any).data || res; if (Array.isArray(d)) ElMessage.success(`检测完成，发现 ${d.length} 个冲突`); loadConflicts() }
  catch { ElMessage.error('检测失败') } finally { conflictDetecting.value = false }
}

function resolveConflict(c: DetectedConflict) { resolveAction.value = 'resolve'; resolveTargetId.value = c.id; resolveNote.value = ''; resolveDialogVisible.value = true }
function ignoreConflict(c: DetectedConflict) { resolveAction.value = 'ignore'; resolveTargetId.value = c.id; resolveNote.value = ''; resolveDialogVisible.value = true }

async function confirmResolve() {
  if (!resolveTargetId.value || !resolveNote.value.trim()) { ElMessage.warning('请填写备注'); return }
  resolveSaving.value = true
  try {
    if (resolveAction.value === 'resolve') await conflictApi.resolve(resolveTargetId.value, resolveNote.value)
    else await conflictApi.ignore(resolveTargetId.value, resolveNote.value)
    ElMessage.success('操作成功'); resolveDialogVisible.value = false; loadConflicts()
  } catch { ElMessage.error('操作失败') } finally { resolveSaving.value = false }
}

function getConflictCategoryName(c: number) { return ({ 1: '资源冲突', 2: '约束违反', 3: '时间冲突' } as any)[c] || '未知' }
function getSeverityName(s: number) { return ({ 1: '低', 2: '中', 3: '高' } as any)[s] || '-' }
function getResolutionStatusName(s: number) { return ({ 0: '未处理', 1: '已解决', 2: '已忽略' } as any)[s] || '-' }

watch(() => props.semesterId, () => { loadConflicts() }, { immediate: true })
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
