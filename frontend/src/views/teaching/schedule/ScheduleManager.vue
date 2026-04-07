<template>
  <div>
    <!-- Schedule plan list -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden; margin-bottom: 16px;">
      <div style="display: flex; align-items: center; justify-content: space-between; padding: 12px 20px;">
        <h2 style="font-size: 13px; font-weight: 600; color: #374151; margin: 0;">排课方案</h2>
        <button class="tm-btn tm-btn-primary" @click="showCreateDialog">新建方案</button>
      </div>
      <table class="tm-table" style="border-radius: 0; border-left: none; border-right: none; border-bottom: none;">
        <colgroup>
          <col />
          <col style="width: 80px" />
          <col style="width: 140px" />
          <col style="width: 70px" />
          <col style="width: 240px" />
        </colgroup>
        <thead>
          <tr>
            <th class="text-left">方案名称</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>条目数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="5" class="tm-empty"><span class="tm-spin" style="display:inline-block;width:16px;height:16px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...</td>
          </tr>
          <tr v-else-if="scheduleList.length === 0">
            <td colspan="5" class="tm-empty">暂无排课方案</td>
          </tr>
          <tr v-for="row in scheduleList" :key="row.id">
            <td class="text-left" style="font-weight: 500;">{{ row.name }}</td>
            <td><span :class="['tm-chip', { 0:'tm-chip-gray', 1:'tm-chip-green', 2:'tm-chip-amber' }[row.status] || 'tm-chip-gray']">{{ getStatusName(row.status) }}</span></td>
            <td style="font-size: 12px; color: #6b7280;">{{ row.createdAt }}</td>
            <td class="tm-mono">{{ row.entryCount ?? '-' }}</td>
            <td>
              <button class="tm-action" style="color: #2563eb;" @click="handleAutoSchedule(row)">智能排课</button>
              <button v-if="row.status === 0" class="tm-action" style="color: #16a34a;" @click="handlePublish(row)">��布</button>
              <button v-if="row.status === 0" class="tm-action" style="color: #d97706;" @click="handleArchive(row)">归档</button>
              <button v-if="row.status === 0" class="tm-action tm-action-danger" @click="handleDelete(row)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create Drawer -->
    <Transition name="tm-drawer">
      <div v-if="createDialogVisible" class="tm-drawer-overlay" @click.self="createDialogVisible = false">
        <div class="tm-drawer" style="width: 440px;">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">新建排课方案</h3>
            <button class="tm-drawer-close" @click="createDialogVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div class="tm-section">
              <div class="tm-field" :class="{ 'tm-error': !createForm.name?.trim() && createSubmitted }">
                <label class="tm-label">方案名称 <span class="req">*</span></label>
                <input v-model="createForm.name" class="tm-input" placeholder="例如：2025春季排课方案" />
              </div>
              <div class="tm-field">
                <label class="tm-label">说明</label>
                <textarea v-model="createForm.description" class="tm-textarea" rows="2" placeholder="可选"></textarea>
              </div>
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" @click="createDialogVisible = false">取消</button>
            <button class="tm-btn tm-btn-primary" :disabled="createSaving" @click="submitCreate">{{ createSaving ? '创建中...' : '创建' }}</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Auto Schedule Drawer -->
    <Transition name="tm-drawer">
      <div v-if="autoScheduleDialogVisible" class="tm-drawer-overlay" @click.self="!scheduling && (autoScheduleDialogVisible = false)">
        <div class="tm-drawer" style="width: 480px;">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">���能排课</h3>
            <button v-if="!scheduling" class="tm-drawer-close" @click="autoScheduleDialogVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div class="tm-section">
              <div style="padding:10px 14px;background:#eff6ff;border:1px solid #bfdbfe;border-radius:7px;font-size:12.5px;color:#1d4ed8;margin-bottom:16px;">
                系统将基于约束规则自动排课。已手动排定的课程不会被调整。
              </div>
              <div class="tm-fields tm-cols-2">
                <div class="tm-field">
                  <label class="tm-label">最大迭代</label>
                  <input v-model.number="autoScheduleParams.maxIterations" type="number" min="100" max="5000" step="100" class="tm-input" />
                </div>
                <div class="tm-field">
                  <label class="tm-label">种群大小</label>
                  <input v-model.number="autoScheduleParams.populationSize" type="number" min="10" max="100" step="10" class="tm-input" />
                </div>
              </div>
              <div class="tm-field">
                <label class="tm-label">变异率: {{ autoScheduleParams.mutationRate }}</label>
                <input v-model.number="autoScheduleParams.mutationRate" type="range" min="0.01" max="0.5" step="0.01" style="width: 100%;" />
              </div>
            </div>
            <div v-if="scheduling" class="tm-section">
              <p style="font-size:13px;color:#6b7280;margin-bottom:8px;">正在排课...</p>
              <div style="height:6px;background:#f3f4f6;border-radius:99px;overflow:hidden;"><div style="height:100%;background:#3b82f6;border-radius:99px;transition:width 0.3s;" :style="{width:autoProgress+'%'}" /></div>
            </div>
            <div v-if="autoResult" class="tm-section" :style="{background:autoResult.success?'#f0fdf4':'#fef2f2'}">
              <p style="font-weight:600;font-size:13px;" :style="{color:autoResult.success?'#16a34a':'#dc2626'}">{{ autoResult.success ? '排课完成' : '排课失败' }}</p>
              <p style="font-size:12px;color:#6b7280;margin-top:4px;">生成: {{ autoResult.entriesGenerated||0 }} 条 | 耗时: {{ ((autoResult.executionTime||0)/1000).toFixed(1) }}s</p>
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" :disabled="scheduling" @click="autoScheduleDialogVisible = false">取消</button>
            <button class="tm-btn tm-btn-primary" :disabled="scheduling" @click="runAutoSchedule">{{ scheduling ? '排课中...' : '开始排课' }}</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { scheduleApi } from '@/api/teaching'
import type { CourseSchedule } from '@/types/teaching'

const props = defineProps<{ semesterId: number | string | undefined }>()

const loading = ref(false)
const scheduleList = ref<CourseSchedule[]>([])

const createDialogVisible = ref(false)
const createSaving = ref(false)
const createSubmitted = ref(false)
const createForm = ref({ name: '', description: '' })

const autoScheduleDialogVisible = ref(false)
const scheduling = ref(false)
const autoProgress = ref(0)
const autoResult = ref<any>(null)
const autoScheduleParams = ref({ scheduleId: undefined as number | string | undefined, maxIterations: 1000, populationSize: 100, mutationRate: 0.1 })

async function loadScheduleList() {
  if (!props.semesterId) return
  loading.value = true
  try { const res = await scheduleApi.list({ semesterId: props.semesterId }); scheduleList.value = (res as any).data || res; if (!Array.isArray(scheduleList.value)) scheduleList.value = [] }
  catch { /* */ } finally { loading.value = false }
}

function showCreateDialog() { createForm.value = { name: '', description: '' }; createSubmitted.value = false; createDialogVisible.value = true }

async function submitCreate() {
  createSubmitted.value = true
  if (!createForm.value.name?.trim() || !props.semesterId) return
  createSaving.value = true
  try {
    await scheduleApi.create({ semesterId: props.semesterId, name: createForm.value.name, description: createForm.value.description })
    ElMessage.success('创建成功'); createDialogVisible.value = false; loadScheduleList()
  } catch { ElMessage.error('创建失败') } finally { createSaving.value = false }
}

function handleAutoSchedule(schedule: CourseSchedule) {
  autoScheduleParams.value = { scheduleId: schedule.id, maxIterations: 500, populationSize: 30, mutationRate: 0.1 }
  autoProgress.value = 0; autoResult.value = null; autoScheduleDialogVisible.value = true
}

async function runAutoSchedule() {
  if (!props.semesterId) { ElMessage.warning('请先选择学期'); return }
  scheduling.value = true; autoProgress.value = 0; autoResult.value = null
  const timer = setInterval(() => { if (autoProgress.value < 90) autoProgress.value += Math.random() * 15 }, 500)
  try {
    const result = await scheduleApi.autoSchedule({ semesterId: props.semesterId, ...autoScheduleParams.value })
    autoProgress.value = 100; const data = (result as any).data || result; autoResult.value = data
    if (data.success) { ElMessage.success(`排课完成！共生成 ${data.entriesGenerated} 条`); loadScheduleList() } else { ElMessage.error('排课失败') }
  } catch (e: any) { ElMessage.error('排课失败: ' + (e.message || '')) } finally { clearInterval(timer); scheduling.value = false }
}

async function handlePublish(s: CourseSchedule) {
  await ElMessageBox.confirm(`确定发布方案「${s.name}」？`, '发布确认', { type: 'warning' })
  try { await scheduleApi.publish(s.id); ElMessage.success('发布成功'); loadScheduleList() } catch { ElMessage.error('发布失败') }
}

async function handleArchive(s: CourseSchedule) {
  await ElMessageBox.confirm(`确定归档方案「${s.name}」？`, '归档确认')
  try { await scheduleApi.archive(s.id); ElMessage.success('归档成功'); loadScheduleList() } catch { ElMessage.error('归档失败') }
}

async function handleDelete(s: CourseSchedule) {
  await ElMessageBox.confirm(`确定删除方案「${s.name}」？此操作不可撤销。`, '删除确认', { type: 'warning' })
  try { await scheduleApi.delete(s.id); ElMessage.success('删除成功'); loadScheduleList() } catch { ElMessage.error('删除失败') }
}

function getStatusName(status: number) { return ({ 0: '草稿', 1: '已发布', 2: '已归档' } as any)[status] || '未知' }

watch(() => props.semesterId, () => { loadScheduleList() }, { immediate: true })
defineExpose({ loadScheduleList })
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
