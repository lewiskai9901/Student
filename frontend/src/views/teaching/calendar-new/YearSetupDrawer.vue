<template>
  <div>
    <!-- Academic Years -->
    <div style="margin-bottom: 24px;">
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
        <h3 style="font-size: 15px; font-weight: 600; color: #111827; margin: 0;">学年管理</h3>
        <button class="tm-btn tm-btn-primary" style="font-size: 12px; padding: 6px 12px;" @click="showYearForm()">新建学年</button>
      </div>
      <table class="tm-table">
        <colgroup><col /><col style="width: 100px" /><col style="width: 100px" /><col style="width: 80px" /><col style="width: 100px" /></colgroup>
        <thead><tr><th class="text-left">学年名称</th><th>开始</th><th>结束</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-if="years.length === 0"><td colspan="5" class="tm-empty">暂无学年</td></tr>
          <tr v-for="y in years" :key="y.id">
            <td class="text-left" style="font-weight: 500;">{{ y.yearName }}</td>
            <td style="font-size: 12px;">{{ y.startDate }}</td>
            <td style="font-size: 12px;">{{ y.endDate }}</td>
            <td><span :class="['tm-chip', y.isCurrent ? 'tm-chip-green' : 'tm-chip-gray']">{{ y.isCurrent ? '当前' : '历史' }}</span></td>
            <td>
              <button v-if="!y.isCurrent" class="tm-action" style="color: #2563eb;" @click="setCurrentYear(y)">设为当前</button>
              <button class="tm-action tm-action-danger" @click="deleteYear(y)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Semesters -->
    <div>
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
        <h3 style="font-size: 15px; font-weight: 600; color: #111827; margin: 0;">学期管理</h3>
        <button class="tm-btn tm-btn-primary" style="font-size: 12px; padding: 6px 12px;" @click="showSemForm()">新建学期</button>
      </div>
      <table class="tm-table">
        <colgroup><col /><col style="width: 90px" /><col style="width: 100px" /><col style="width: 100px" /><col style="width: 80px" /><col style="width: 140px" /></colgroup>
        <thead><tr><th class="text-left">学期名称</th><th>学年</th><th>开始</th><th>结束</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-if="semesters.length === 0"><td colspan="6" class="tm-empty">暂无学期</td></tr>
          <tr v-for="s in semesters" :key="s.id">
            <td class="text-left" style="font-weight: 500;">{{ s.semesterName }}</td>
            <td style="font-size: 12px; color: #6b7280;">{{ getYearName(s.academicYearId) }}</td>
            <td style="font-size: 12px;">{{ s.startDate }}</td>
            <td style="font-size: 12px;">{{ s.endDate }}</td>
            <td><span :class="['tm-chip', s.isCurrent ? 'tm-chip-green' : 'tm-chip-gray']">{{ s.isCurrent ? '当前' : '' }}</span></td>
            <td>
              <button v-if="!s.isCurrent" class="tm-action" style="color: #2563eb;" @click="setCurrentSem(s)">设为当前</button>
              <button class="tm-action" @click="generateWeeks(s)">生成教学周</button>
              <button class="tm-action tm-action-danger" @click="deleteSem(s)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Year Form Drawer -->
    <Transition name="tm-drawer">
      <div v-if="yearFormVisible" class="tm-drawer-overlay" @click.self="yearFormVisible = false">
        <div class="tm-drawer" style="width: 400px;">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">新建学年</h3>
            <button class="tm-drawer-close" @click="yearFormVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div class="tm-section">
              <div class="tm-field"><label class="tm-label">学年名称</label><input v-model="yearForm.yearName" class="tm-input" placeholder="如: 2025-2026学年" /></div>
              <div class="tm-field"><label class="tm-label">学年编码</label><input v-model="yearForm.yearCode" class="tm-input" placeholder="如: 2025-2026" /></div>
              <div class="tm-fields tm-cols-2">
                <div class="tm-field"><label class="tm-label">开始日期</label><input v-model="yearForm.startDate" type="date" class="tm-input" /></div>
                <div class="tm-field"><label class="tm-label">结束日期</label><input v-model="yearForm.endDate" type="date" class="tm-input" /></div>
              </div>
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" @click="yearFormVisible = false">取消</button>
            <button class="tm-btn tm-btn-primary" @click="saveYear">保存</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Semester Form Drawer -->
    <Transition name="tm-drawer">
      <div v-if="semFormVisible" class="tm-drawer-overlay" @click.self="semFormVisible = false">
        <div class="tm-drawer" style="width: 400px;">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">新建学期</h3>
            <button class="tm-drawer-close" @click="semFormVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div class="tm-section">
              <div class="tm-field">
                <label class="tm-label">所属学年 <span style="color: #ef4444;">*</span></label>
                <select v-model="semForm.academicYearId" class="tm-input" @change="onYearSelected">
                  <option value="">请选择学年</option>
                  <option v-for="y in years" :key="y.id" :value="y.id">{{ y.yearName }}</option>
                </select>
                <span v-if="years.length === 0" style="font-size: 12px; color: #9ca3af; margin-top: 4px; display: block;">请先创建学年</span>
              </div>
              <div class="tm-field">
                <label class="tm-label">学期类型</label>
                <div class="tm-radios" style="width: 200px;">
                  <label :class="['tm-radio', { active: semForm.semesterType === 1 }]" @click="semForm.semesterType = 1; autoFillSem()"><input type="radio" />第一学期</label>
                  <label :class="['tm-radio', { active: semForm.semesterType === 2 }]" @click="semForm.semesterType = 2; autoFillSem()"><input type="radio" />第二学期</label>
                </div>
              </div>
              <div class="tm-field"><label class="tm-label">学期名称</label><input v-model="semForm.semesterName" class="tm-input" placeholder="自动生成" /></div>
              <div class="tm-field"><label class="tm-label">学期编码</label><input v-model="semForm.semesterCode" class="tm-input" placeholder="自动生成" readonly style="background: #f9fafb;" /></div>
              <div class="tm-fields tm-cols-2">
                <div class="tm-field"><label class="tm-label">开始日期</label><input v-model="semForm.startDate" type="date" class="tm-input" :min="semDateRange.min" :max="semDateRange.max" /></div>
                <div class="tm-field"><label class="tm-label">结束日期</label><input v-model="semForm.endDate" type="date" class="tm-input" :min="semForm.startDate || semDateRange.min" :max="semDateRange.max" /></div>
              </div>
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" @click="semFormVisible = false">取消</button>
            <button class="tm-btn tm-btn-primary" @click="saveSem">保存</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { academicYearApi, semesterApi } from '@/api/calendar'

const emit = defineEmits<{ semesterChanged: [] }>()

const years = ref<any[]>([])
const semesters = ref<any[]>([])
const yearFormVisible = ref(false)
const semFormVisible = ref(false)
const yearForm = ref<any>({})
const semForm = ref<any>({ semesterType: 1, academicYearId: '' })
const semDateRange = ref<{ min: string; max: string }>({ min: '', max: '' })

async function loadYears() {
  try { const res = await academicYearApi.list(); years.value = (res as any).data || res || [] } catch { years.value = [] }
}
async function loadSemesters() {
  try { const res = await semesterApi.list(); semesters.value = Array.isArray(res) ? res : (res as any).data || [] } catch { semesters.value = [] }
}

function showYearForm() { yearForm.value = {}; yearFormVisible.value = true }
function showSemForm() {
  semForm.value = { semesterType: 1, academicYearId: '' }
  semDateRange.value = { min: '', max: '' }
  semFormVisible.value = true
}

function onYearSelected() {
  autoFillSem()
}

function autoFillSem() {
  const yearId = semForm.value.academicYearId
  if (!yearId) {
    semForm.value.semesterName = ''
    semForm.value.semesterCode = ''
    semDateRange.value = { min: '', max: '' }
    return
  }
  const year = years.value.find((y: any) => String(y.id) === String(yearId))
  if (!year) return

  const code = year.yearName?.replace('学年', '').trim() || year.yearCode
  const typeNum = semForm.value.semesterType || 1
  const typeName = typeNum === 1 ? '第一学期' : '第二学期'

  semForm.value.semesterName = `${code}${typeName}`
  semForm.value.semesterCode = `${code}-${typeNum}`
  semDateRange.value = { min: year.startDate, max: year.endDate }
}

async function saveYear() {
  try { await academicYearApi.create(yearForm.value); ElMessage.success('创建成功'); yearFormVisible.value = false; loadYears() } catch { ElMessage.error('创建失败') }
}
async function saveSem() {
  if (!semForm.value.academicYearId) { ElMessage.warning('请选择所属学年'); return }
  try { await semesterApi.create(semForm.value); ElMessage.success('创建成功'); semFormVisible.value = false; loadSemesters(); emit('semesterChanged') } catch (e: any) { ElMessage.error(e?.response?.data?.message || '创建失败') }
}
async function setCurrentYear(y: any) {
  try { await academicYearApi.setCurrent(y.id); ElMessage.success('已设为当前学年'); loadYears() } catch { ElMessage.error('设置失败') }
}
async function setCurrentSem(s: any) {
  try { await semesterApi.setCurrent(s.id); ElMessage.success('已设为当前学期'); loadSemesters(); emit('semesterChanged') } catch { ElMessage.error('设置失败') }
}
async function deleteYear(y: any) {
  await ElMessageBox.confirm(`确定删除学年"${y.yearName}"？`, '删除', { type: 'warning' })
  try { await academicYearApi.delete(y.id); ElMessage.success('已删除'); loadYears() } catch { ElMessage.error('删除失败') }
}
async function deleteSem(s: any) {
  await ElMessageBox.confirm(`确定删除学期"${s.semesterName}"？`, '删除', { type: 'warning' })
  try { await semesterApi.delete(s.id); ElMessage.success('已删除'); loadSemesters(); emit('semesterChanged') } catch { ElMessage.error('删除失败') }
}
async function generateWeeks(s: any) {
  try { await semesterApi.generateWeeks(s.id); ElMessage.success('教学周已生成') } catch { ElMessage.error('生成失败') }
}

function getYearName(yearId: any) {
  if (!yearId) return '-'
  const y = years.value.find((yr: any) => String(yr.id) === String(yearId))
  return y ? y.yearName : '-'
}

onMounted(() => { loadYears(); loadSemesters() })
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
