<template>
  <el-dialog
    v-model="visible"
    title="整改建议确认"
    width="640px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-loading="loading">
      <p class="hint">
        系统已根据评分识别出
        <span class="hl">{{ candidates.length }}</span>
        项可能需要整改的检查项. 请确认要建立整改单的项:
      </p>

      <div v-if="candidates.length === 0 && !loading" class="empty">
        无需整改 — 所有检查项都达标
      </div>

      <ul class="cand-list">
        <li
          v-for="c in candidates"
          :key="c.detailId"
          class="cand-row"
          :class="{ 'must': c.mustCorrect }"
        >
          <el-checkbox
            v-model="selected[c.detailId]"
            :disabled="c.mustCorrect"
          />
          <span class="sev" :class="`sev-${c.severity.toLowerCase()}`">
            {{ severityLabel(c.severity) }}
          </span>
          <div class="info">
            <div class="name">{{ c.itemName }}</div>
            <div class="reason">
              {{ c.reason }}
              <span v-if="c.suggestedDeadlineDays > 0" class="deadline">
                · {{ c.suggestedDeadlineDays }} 天内整改
              </span>
            </div>
          </div>
        </li>
      </ul>
    </div>

    <template #footer>
      <div class="footer">
        <el-button v-if="candidates.length" link @click="selectHighOnly">仅勾选高优先级</el-button>
        <el-button v-if="candidates.length" link @click="selectAll">全部勾选</el-button>
        <el-button @click="handleClose">{{ candidates.length ? '跳过' : '关闭' }}</el-button>
        <el-button
          v-if="candidates.length"
          type="primary"
          :loading="confirming"
          @click="handleConfirm"
        >
          确认建单 ({{ selectedCount }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getCorrectiveCandidates,
  confirmCorrectiveCandidates,
  type CorrectiveCandidate,
} from '@/api/inspection/correctiveCase'

interface Props {
  modelValue: boolean
  submissionIds: number[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'confirmed', caseIds: number[]): void
}>()

const visible = ref(false)
const loading = ref(false)
const confirming = ref(false)
const candidates = ref<CorrectiveCandidate[]>([])
const submissionIdMap = ref<Map<LongId, number>>(new Map())  // detailId → submissionId
const selected = ref<Record<LongId, boolean>>({})

watch(
  () => props.modelValue,
  async v => {
    visible.value = v
    if (v) await loadCandidates()
  },
)
watch(visible, v => emit('update:modelValue', v))

async function loadCandidates() {
  loading.value = true
  candidates.value = []
  submissionIdMap.value.clear()
  selected.value = {}
  try {
    for (const sid of props.submissionIds) {
      const list = await getCorrectiveCandidates(sid)
      for (const c of list) {
        candidates.value.push(c)
        submissionIdMap.value.set(c.detailId, sid)
        selected.value[c.detailId] = true   // 默认全选
      }
    }
  } catch (e: unknown) {
    console.warn('加载整改候选失败', e)
  } finally {
    loading.value = false
  }
}

const selectedCount = computed(
  () => Object.values(selected.value).filter(Boolean).length,
)

function selectAll() {
  for (const c of candidates.value) selected.value[c.detailId] = true
}

function selectHighOnly() {
  for (const c of candidates.value) {
    selected.value[c.detailId] = c.severity === 'HIGH' || c.mustCorrect
  }
}

function severityLabel(s: string) {
  switch (s) {
    case 'HIGH':   return '高'
    case 'MEDIUM': return '中'
    case 'LOW':    return '低'
    default:       return '—'
  }
}

async function handleConfirm() {
  // 按 submissionId 分组提交
  const bySubmission: Record<LongId, number[]> = {}
  for (const [detailIdStr, on] of Object.entries(selected.value)) {
    if (!on) continue
    const detailId = Number(detailIdStr)
    const sid = submissionIdMap.value.get(detailId)
    if (sid == null) continue
    ;(bySubmission[sid] ??= []).push(detailId)
  }

  const sids = Object.keys(bySubmission)
  if (sids.length === 0) {
    handleClose()
    return
  }

  confirming.value = true
  const allCreated: number[] = []
  try {
    for (const sid of sids) {
      const ids = await confirmCorrectiveCandidates(Number(sid), bySubmission[Number(sid)])
      allCreated.push(...ids)
    }
    ElMessage.success(`已建立 ${allCreated.length} 条整改单`)
    emit('confirmed', allCreated)
    handleClose()
  } catch (e: unknown) {
    console.error('建立整改单失败', e)
    ElMessage.error('建立整改单失败, 请重试')
  } finally {
    confirming.value = false
  }
}

function handleClose() {
  visible.value = false
}
</script>

<style scoped>
.hint {
  margin: 0 0 12px;
  color: #4b5563;
  font-size: 13px;
}
.hl { color: #dc2626; font-weight: 600; }
.empty {
  text-align: center;
  color: #9ca3af;
  padding: 30px 0;
  font-size: 13px;
}
.cand-list {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 380px;
  overflow-y: auto;
}
.cand-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 8px;
  background: #fff;
}
.cand-row.must {
  border-color: #fca5a5;
  background: #fef2f2;
}
.sev {
  display: inline-block;
  min-width: 28px;
  text-align: center;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}
.sev-high   { background: #fee2e2; color: #b91c1c; }
.sev-medium { background: #fef3c7; color: #b45309; }
.sev-low    { background: #dcfce7; color: #15803d; }
.info { flex: 1; }
.name {
  font-size: 13px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 2px;
}
.reason {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
}
.deadline { color: #92400e; }
.footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}
</style>
