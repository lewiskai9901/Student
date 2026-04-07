<template>
  <div>
    <h3 class="setup-step-title">数据准备检查</h3>
    <p class="setup-step-desc">排课前需要准备好以下数据，点击按钮可快速生成。</p>

    <div class="checklist">
      <!-- 开课计划 -->
      <div class="check-row">
        <div class="check-icon" :class="r.offerings?.status === 'ready' ? 'ok' : 'fail'">
          {{ r.offerings?.status === 'ready' ? '&#10003;' : '!' }}
        </div>
        <div class="check-info">
          <div class="check-label">开课计划</div>
          <div class="check-value">{{ r.offerings?.count || 0 }} 门课程</div>
        </div>
        <button v-if="r.offerings?.count === 0" class="tm-btn tm-btn-workflow" style="font-size: 12px; padding: 5px 10px;" :disabled="generating.offerings" @click="generateOfferings">
          {{ generating.offerings ? '生成中...' : '从培养方案导入' }}
        </button>
      </div>

      <!-- 教学任务 -->
      <div class="check-row">
        <div class="check-icon" :class="r.tasks?.status === 'ready' ? 'ok' : r.tasks?.status === 'warning' ? 'warn' : 'fail'">
          {{ r.tasks?.status === 'ready' ? '&#10003;' : r.tasks?.status === 'warning' ? '!' : '&#10007;' }}
        </div>
        <div class="check-info">
          <div class="check-label">教学任务</div>
          <div class="check-value">
            {{ r.tasks?.count || 0 }} 条
            <span v-if="r.tasks?.withoutTeacher > 0" style="color: #d97706; margin-left: 6px;">（{{ r.tasks.withoutTeacher }} 条未分配教师）</span>
          </div>
        </div>
        <button v-if="r.tasks?.count === 0 && r.offerings?.count > 0" class="tm-btn tm-btn-workflow" style="font-size: 12px; padding: 5px 10px;" :disabled="generating.tasks" @click="generateTasks">
          {{ generating.tasks ? '生成中...' : '从开课计划生成' }}
        </button>
      </div>

      <!-- 教室 -->
      <div class="check-row">
        <div class="check-icon" :class="r.classrooms?.count > 0 ? 'ok' : 'warn'">
          {{ r.classrooms?.count > 0 ? '&#10003;' : '!' }}
        </div>
        <div class="check-info">
          <div class="check-label">教室/场所</div>
          <div class="check-value">{{ r.classrooms?.count || 0 }} 间可用</div>
        </div>
      </div>

      <!-- 约束规则 -->
      <div class="check-row">
        <div class="check-icon" :class="r.constraints?.count > 0 ? 'ok' : 'info'">
          {{ r.constraints?.count > 0 ? '&#10003;' : '-' }}
        </div>
        <div class="check-info">
          <div class="check-label">约束规则 <span style="font-weight: 400; color: #9ca3af;">(可选)</span></div>
          <div class="check-value">{{ r.constraints?.count || 0 }} 条启用</div>
        </div>
      </div>

      <!-- 已有排课 -->
      <div class="check-row">
        <div class="check-icon" :class="r.plans?.entryCount > 0 ? 'ok' : 'info'">
          {{ r.plans?.entryCount > 0 ? '&#10003;' : '-' }}
        </div>
        <div class="check-info">
          <div class="check-label">排课条目</div>
          <div class="check-value">{{ r.plans?.entryCount || 0 }} 条（{{ r.plans?.count || 0 }} 个方案）</div>
        </div>
      </div>
    </div>

    <button class="tm-btn tm-btn-secondary" style="margin-top: 16px;" @click="emit('refresh')">刷新检查</button>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowApi } from '@/api/teaching'

const props = defineProps<{ semesterId: number | string | undefined; readiness: any }>()
const emit = defineEmits<{ refresh: [] }>()

const r = computed(() => props.readiness || {})
const generating = reactive({ offerings: false, tasks: false })

async function generateOfferings() {
  if (!props.semesterId) return
  generating.offerings = true
  try {
    await workflowApi.generateMappings(props.semesterId)
    const res = await workflowApi.generateOfferings(props.semesterId)
    ElMessage.success(`导入 ${res.generated || 0} 门课程`)
    emit('refresh')
  } catch (e: any) { ElMessage.error(e.message || '导入失败') }
  finally { generating.offerings = false }
}

async function generateTasks() {
  if (!props.semesterId) return
  generating.tasks = true
  try {
    const res = await workflowApi.generateTasks(props.semesterId)
    ElMessage.success(`生成 ${res.generated || 0} 条教学任务`)
    emit('refresh')
  } catch (e: any) { ElMessage.error(e.message || '生成失败') }
  finally { generating.tasks = false }
}
</script>

<style scoped>
.setup-step-title { font-size: 16px; font-weight: 700; color: #111827; margin: 0 0 6px; }
.setup-step-desc { font-size: 13px; color: #6b7280; margin: 0 0 20px; }

.checklist { display: flex; flex-direction: column; gap: 12px; }
.check-row {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px; border: 1px solid #e5e7eb; border-radius: 10px;
  background: #fff; transition: all 0.15s;
}
.check-icon {
  width: 32px; height: 32px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%; font-size: 14px; font-weight: 700;
}
.check-icon.ok { background: #dcfce7; color: #16a34a; }
.check-icon.warn { background: #fef3c7; color: #d97706; }
.check-icon.fail { background: #fef2f2; color: #dc2626; }
.check-icon.info { background: #f3f4f6; color: #9ca3af; }

.check-info { flex: 1; min-width: 0; }
.check-label { font-size: 13px; font-weight: 600; color: #111827; }
.check-value { font-size: 12px; color: #6b7280; margin-top: 2px; }
</style>
