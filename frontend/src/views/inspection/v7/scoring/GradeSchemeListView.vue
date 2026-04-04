<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Trash2, Pencil, Copy, Award } from 'lucide-vue-next'
import {
  getGradeSchemes, createGradeScheme, updateGradeScheme,
  deleteGradeScheme, cloneGradeScheme,
} from '@/api/insp/gradeScheme'
import type { GradeScheme } from '@/types/insp/gradeScheme'

const loading = ref(false)
const schemes = ref<GradeScheme[]>([])
const dialogVisible = ref(false)
const editingScheme = ref<GradeScheme | null>(null)
const saving = ref(false)

const form = ref({
  displayName: '',
  description: '',
  schemeType: 'PERCENT_RANGE' as string,
  grades: [] as Array<{ code: string; name: string; color: string; icon: string }>,
})

const systemSchemes = computed(() => schemes.value.filter(s => s.isSystem))
const customSchemes = computed(() => schemes.value.filter(s => !s.isSystem))

// ── Presets ──
const PRESETS: Record<string, typeof form.value> = {
  five: {
    displayName: '五级评定', description: '', schemeType: 'PERCENT_RANGE',
    grades: [
      { code: 'A', name: '优秀', color: '#22c55e', icon: '' },
      { code: 'B', name: '良好', color: '#3b82f6', icon: '' },
      { code: 'C', name: '中等', color: '#f59e0b', icon: '' },
      { code: 'D', name: '及格', color: '#f97316', icon: '' },
      { code: 'F', name: '不及格', color: '#ef4444', icon: '' },
    ],
  },
  pass: {
    displayName: '合格评定', description: '', schemeType: 'PERCENT_RANGE',
    grades: [
      { code: 'PASS', name: '合格', color: '#22c55e', icon: '' },
      { code: 'FAIL', name: '不合格', color: '#ef4444', icon: '' },
    ],
  },
  flag: {
    displayName: '流动红旗', description: '', schemeType: 'PERCENT_RANGE',
    grades: [
      { code: 'RED', name: '红旗', color: '#ef4444', icon: 'flag' },
      { code: 'BLUE', name: '蓝旗', color: '#3b82f6', icon: 'flag' },
      { code: 'YELLOW', name: '黄旗', color: '#f59e0b', icon: 'flag' },
    ],
  },
  star: {
    displayName: '星级评定', description: '', schemeType: 'PERCENT_RANGE',
    grades: [
      { code: '5STAR', name: '五星', color: '#ffd700', icon: 'star' },
      { code: '4STAR', name: '四星', color: '#ffa500', icon: 'star' },
      { code: '3STAR', name: '三星', color: '#c0c0c0', icon: 'star' },
      { code: '2STAR', name: '二星', color: '#cd7f32', icon: 'star' },
      { code: '1STAR', name: '一星', color: '#808080', icon: 'star' },
    ],
  },
}

async function loadSchemes() {
  loading.value = true
  try { schemes.value = await getGradeSchemes() }
  catch (e: any) { ElMessage.error(e.message || '加载失败') }
  finally { loading.value = false }
}

function openCreate() {
  editingScheme.value = null
  form.value = { displayName: '', description: '', schemeType: 'PERCENT_RANGE', grades: [] }
  dialogVisible.value = true
}

function openEdit(scheme: GradeScheme) {
  editingScheme.value = scheme
  form.value = {
    displayName: scheme.displayName,
    description: scheme.description || '',
    schemeType: scheme.schemeType,
    grades: scheme.grades.map(g => ({
      code: g.code, name: g.name,
      color: g.color || '#6b7280', icon: g.icon || '',
    })),
  }
  dialogVisible.value = true
}

function applyPreset(key: string) {
  const preset = PRESETS[key]
  if (preset) {
    form.value = JSON.parse(JSON.stringify(preset))
  }
}

function addGrade() {
  form.value.grades.push({ code: '', name: '', color: '#6b7280', icon: '' })
}

function removeGrade(index: number) {
  form.value.grades.splice(index, 1)
}

async function handleSave() {
  if (!form.value.displayName.trim()) { ElMessage.warning('请填写方案名称'); return }
  if (form.value.grades.length === 0) { ElMessage.warning('请至少添加一个等级'); return }
  saving.value = true
  try {
    const data = {
      displayName: form.value.displayName,
      description: form.value.description || undefined,
      schemeType: form.value.schemeType,
      grades: form.value.grades.map((g, i) => ({ ...g, sortOrder: i })),
    }
    if (editingScheme.value) {
      await updateGradeScheme(editingScheme.value.id, data)
      ElMessage.success('已更新')
    } else {
      await createGradeScheme(data)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await loadSchemes()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
  finally { saving.value = false }
}

async function handleClone(scheme: GradeScheme) {
  try {
    await cloneGradeScheme({ sourceSchemeId: scheme.id, displayName: scheme.displayName + ' (副本)' })
    ElMessage.success('已克隆')
    await loadSchemes()
  } catch (e: any) { ElMessage.error(e.message || '克隆失败') }
}

async function handleDelete(scheme: GradeScheme) {
  try {
    await ElMessageBox.confirm(`删除「${scheme.displayName}」？`, '确认', { type: 'warning' })
    await deleteGradeScheme(scheme.id)
    ElMessage.success('已删除')
    await loadSchemes()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

onMounted(() => loadSchemes())
</script>

<template>
  <div class="gs-page" v-loading="loading">
    <!-- Header -->
    <div class="gs-header">
      <div class="gs-header-left">
        <Award class="w-5 h-5" style="color: #d97706" />
        <span class="gs-title">等级方案</span>
        <span class="gs-count" v-if="schemes.length">{{ schemes.length }}</span>
      </div>
      <button class="gs-btn primary" @click="openCreate">
        <Plus class="w-3.5 h-3.5" /> 新建方案
      </button>
    </div>

    <!-- System presets -->
    <div v-if="systemSchemes.length > 0" class="gs-section">
      <div class="gs-section-title">系统预设</div>
      <div class="gs-grid">
        <div v-for="s in systemSchemes" :key="s.id" class="gs-card system">
          <div class="gs-card-head">
            <span class="gs-card-name">{{ s.displayName }}</span>
            <button class="gs-icon-btn" @click="handleClone(s)" title="克隆为自定义"><Copy class="w-3.5 h-3.5" /></button>
          </div>
          <div class="gs-card-desc" v-if="s.description">{{ s.description }}</div>
          <div class="gs-grade-list">
            <span v-for="g in s.grades" :key="g.code" class="gs-grade" :style="{ background: g.color + '18', color: g.color, borderColor: g.color + '40' }">
              {{ g.name }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Custom schemes -->
    <div class="gs-section">
      <div class="gs-section-title">自定义方案</div>
      <div v-if="customSchemes.length === 0" class="gs-empty">
        暂无自定义方案，点击「新建方案」创建，或从系统预设克隆
      </div>
      <div v-else class="gs-grid">
        <div v-for="s in customSchemes" :key="s.id" class="gs-card">
          <div class="gs-card-head">
            <span class="gs-card-name">{{ s.displayName }}</span>
            <div class="gs-card-actions">
              <button class="gs-icon-btn" @click="openEdit(s)" title="编辑"><Pencil class="w-3.5 h-3.5" /></button>
              <button class="gs-icon-btn danger" @click="handleDelete(s)" title="删除"><Trash2 class="w-3.5 h-3.5" /></button>
            </div>
          </div>
          <div class="gs-card-desc" v-if="s.description">{{ s.description }}</div>
          <div class="gs-grade-list">
            <span v-for="g in s.grades" :key="g.code" class="gs-grade" :style="{ background: g.color + '18', color: g.color, borderColor: g.color + '40' }">
              {{ g.name }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- ─── Dialog ─── -->
    <el-dialog v-model="dialogVisible" :title="editingScheme ? '编辑等级方案' : '新建等级方案'" width="580px" :close-on-click-modal="false">
      <div class="dlg">
        <div class="dlg-field">
          <label class="dlg-label">方案名称 <span class="dlg-req">*</span></label>
          <input v-model="form.displayName" class="dlg-input" placeholder="如：卫生流动红旗" />
        </div>
        <div class="dlg-field">
          <label class="dlg-label">描述</label>
          <input v-model="form.description" class="dlg-input" placeholder="可选描述" />
        </div>
        <!-- Quick presets -->
        <div class="dlg-presets" v-if="!editingScheme">
          <span class="dlg-preset-label">快速模板:</span>
          <button v-for="(_, key) in PRESETS" :key="key" class="dlg-preset-btn" @click="applyPreset(key)">{{ PRESETS[key].displayName }}</button>
        </div>

        <!-- Grades table -->
        <div class="dlg-divider">等级定义</div>
        <div class="dlg-grades">
          <div v-for="(g, i) in form.grades" :key="i" class="dlg-grade-row">
            <input v-model="g.code" class="dlg-grade-input code" placeholder="编码" />
            <input v-model="g.name" class="dlg-grade-input name" placeholder="名称" />
            <input v-model="g.color" class="dlg-grade-color" type="color" />
            <button class="dlg-grade-del" @click="removeGrade(i)">&times;</button>
          </div>
          <button class="dlg-grade-add" @click="addGrade">
            <Plus class="w-3.5 h-3.5" /> 添加等级
          </button>
        </div>
      </div>

      <template #footer>
        <div class="dlg-footer">
          <button class="dlg-btn cancel" @click="dialogVisible = false">取消</button>
          <button class="dlg-btn confirm" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中...' : (editingScheme ? '更新' : '创建') }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.gs-page { padding: 20px 24px; max-width: 1000px; margin: 0 auto; }

.gs-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
}
.gs-header-left { display: flex; align-items: center; gap: 8px; }
.gs-title { font-size: 16px; font-weight: 700; color: #1e1b4b; }
.gs-count { font-size: 11px; color: #d97706; background: #fffbeb; padding: 2px 8px; border-radius: 10px; font-weight: 600; }

.gs-btn {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 7px 16px; border-radius: 8px; font-size: 13px; font-weight: 600;
  border: none; cursor: pointer; transition: all 0.15s;
}
.gs-btn.primary { background: #d97706; color: #fff; }
.gs-btn.primary:hover { background: #b45309; }

.gs-section { margin-bottom: 24px; }
.gs-section-title { font-size: 12px; font-weight: 700; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 10px; }

.gs-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }

.gs-card {
  background: #fff; border: 1px solid #e5e7eb; border-radius: 10px;
  padding: 14px 16px; transition: box-shadow 0.2s;
}
.gs-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.gs-card.system { background: #fffdf7; border-color: #fde68a; }

.gs-card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.gs-card-name { font-size: 14px; font-weight: 700; color: #1e1b4b; }
.gs-card-actions { display: flex; gap: 2px; }
.gs-card-desc { font-size: 11px; color: #9ca3af; margin-bottom: 8px; }

.gs-icon-btn {
  width: 28px; height: 28px; border: none; border-radius: 6px;
  background: transparent; color: #9ca3af; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center; transition: all 0.15s;
}
.gs-icon-btn:hover { background: #f3f4f6; color: #374151; }
.gs-icon-btn.danger:hover { background: #fef2f2; color: #ef4444; }

.gs-grade-list { display: flex; flex-wrap: wrap; gap: 5px; }
.gs-grade {
  font-size: 11px; font-weight: 600; padding: 3px 8px; border-radius: 6px;
  border: 1px solid; display: inline-flex; align-items: center; gap: 4px;
}

.gs-empty { text-align: center; padding: 32px; color: #9ca3af; font-size: 13px; background: #fafbfc; border-radius: 10px; border: 1px dashed #e5e7eb; }

/* ── Dialog ── */
.dlg { display: flex; flex-direction: column; gap: 12px; }
.dlg-field { display: flex; flex-direction: column; gap: 4px; }
.dlg-label { font-size: 12px; font-weight: 600; color: #4b5563; }
.dlg-req { color: #ef4444; }
.dlg-input {
  width: 100%; padding: 8px 12px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 13px; color: #1f2937; outline: none; transition: border-color 0.15s;
}
.dlg-input:focus { border-color: #d97706; }
.dlg-input::placeholder { color: #d1d5db; }

.dlg-presets { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.dlg-preset-label { font-size: 12px; color: #9ca3af; }
.dlg-preset-btn {
  font-size: 11px; font-weight: 600; padding: 4px 10px; border-radius: 6px;
  border: 1px solid #e5e7eb; background: #fff; color: #6b7280; cursor: pointer; transition: all 0.15s;
}
.dlg-preset-btn:hover { border-color: #d97706; color: #d97706; background: #fffbeb; }

.dlg-divider {
  font-size: 11px; font-weight: 700; color: #9ca3af; letter-spacing: 0.5px;
  padding-top: 4px; border-top: 1px solid #f3f4f6;
}

.dlg-grades { display: flex; flex-direction: column; gap: 6px; }
.dlg-grade-row { display: flex; align-items: center; gap: 4px; }
.dlg-grade-input {
  padding: 6px 8px; border: 1px solid #e5e7eb; border-radius: 6px;
  font-size: 12px; outline: none; transition: border-color 0.15s;
}
.dlg-grade-input:focus { border-color: #d97706; }
.dlg-grade-input.code { width: 60px; }
.dlg-grade-input.name { width: 70px; }
.dlg-grade-color { width: 28px; height: 28px; border: 1px solid #e5e7eb; border-radius: 6px; cursor: pointer; padding: 2px; }
.dlg-grade-del {
  width: 24px; height: 24px; border: none; border-radius: 4px;
  background: transparent; color: #d1d5db; cursor: pointer; font-size: 14px;
  display: flex; align-items: center; justify-content: center; transition: all 0.15s;
}
.dlg-grade-del:hover { background: #fef2f2; color: #ef4444; }

.dlg-grade-add {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 5px 12px; border: 1px dashed #e5e7eb; border-radius: 6px;
  font-size: 12px; color: #9ca3af; background: none; cursor: pointer; transition: all 0.15s;
  align-self: flex-start;
}
.dlg-grade-add:hover { color: #d97706; border-color: #fde68a; background: #fffbeb; }

.dlg-footer { display: flex; justify-content: flex-end; gap: 8px; }
.dlg-btn {
  padding: 8px 20px; border-radius: 8px; font-size: 13px; font-weight: 600;
  border: none; cursor: pointer; transition: all 0.15s;
}
.dlg-btn.cancel { background: #f3f4f6; color: #6b7280; }
.dlg-btn.cancel:hover { background: #e5e7eb; }
.dlg-btn.confirm { background: #d97706; color: #fff; }
.dlg-btn.confirm:hover { background: #b45309; }
.dlg-btn:disabled { opacity: 0.5; cursor: not-allowed; }

@media (max-width: 700px) {
  .gs-grid { grid-template-columns: 1fr; }
}
</style>
