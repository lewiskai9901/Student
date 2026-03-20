<script setup lang="ts">
/**
 * PersonScoreGrid - Per-person scoring grid
 *
 * Loads persons from the target (place/org) via API, then displays
 * a compact grid with score inputs for each person.
 * Emits update:scores with the full array on every change.
 */
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'

interface TargetPerson {
  id: number
  name: string
  orgUnitId?: number
}

interface PersonScore {
  userId: number
  userName: string
  score: number | null
}

const props = withDefaults(defineProps<{
  targetType: string
  targetId: number
  detailId: number
  disabled?: boolean
  maxScore?: number
}>(), {
  disabled: false,
  maxScore: 100,
})

const emit = defineEmits<{
  'update:scores': [scores: PersonScore[]]
}>()

// ========== State ==========
const loading = ref(false)
const persons = ref<TargetPerson[]>([])
const scoreMap = ref<Record<number, number | null>>({})

// ========== Computed ==========
const stats = computed(() => {
  const vals = Object.values(scoreMap.value).filter(
    (v): v is number => v != null && !isNaN(v)
  )
  if (vals.length === 0) return { avg: 0, min: 0, max: 0, count: 0 }
  return {
    avg: Math.round((vals.reduce((a, b) => a + b, 0) / vals.length) * 10) / 10,
    min: Math.min(...vals),
    max: Math.max(...vals),
    count: vals.length,
  }
})

// ========== Load persons ==========
async function loadPersons() {
  if (!props.targetType || !props.targetId) return
  loading.value = true
  try {
    persons.value = await http.get<TargetPerson[]>('/v7/insp/projects/targets/persons', {
      params: {
        targetType: props.targetType,
        targetId: props.targetId,
      },
    })
    // Initialize scoreMap for new persons not yet in the map
    for (const p of persons.value) {
      if (!(p.id in scoreMap.value)) {
        scoreMap.value[p.id] = null
      }
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载人员列表失败')
    persons.value = []
  } finally {
    loading.value = false
  }
}

// ========== Score change ==========
function handleScoreChange(personId: number, value: number | null) {
  scoreMap.value[personId] = value
  emitScores()
}

function emitScores() {
  const scores: PersonScore[] = persons.value
    .filter((p) => scoreMap.value[p.id] != null)
    .map((p) => ({
      userId: p.id,
      userName: p.name,
      score: scoreMap.value[p.id]!,
    }))
  emit('update:scores', scores)
}

// ========== Batch actions ==========
function setAllScores(value: number) {
  for (const p of persons.value) {
    scoreMap.value[p.id] = value
  }
  emitScores()
}

function clearAllScores() {
  for (const p of persons.value) {
    scoreMap.value[p.id] = null
  }
  emitScores()
}

// ========== Lifecycle ==========
onMounted(() => loadPersons())

watch(
  () => [props.targetType, props.targetId],
  () => {
    scoreMap.value = {}
    loadPersons()
  }
)
</script>

<template>
  <div v-loading="loading">
    <!-- Header -->
    <div class="flex items-center justify-between mb-2">
      <span class="text-xs font-medium text-gray-600">
        逐人评分
        <span v-if="persons.length > 0" class="text-gray-400">({{ persons.length }}人)</span>
      </span>
      <div v-if="persons.length > 0 && !disabled" class="flex items-center gap-2">
        <button
          class="text-[10px] text-blue-500 hover:text-blue-700 transition-colors"
          @click="setAllScores(maxScore)"
        >全部满分</button>
        <span class="text-gray-300">|</span>
        <button
          class="text-[10px] text-gray-400 hover:text-gray-600 transition-colors"
          @click="clearAllScores"
        >清空</button>
      </div>
    </div>

    <!-- Summary -->
    <div
      v-if="stats.count > 0"
      class="flex items-center gap-4 text-xs text-gray-500 mb-3 pb-2 border-b border-gray-100"
    >
      <span>已评分: <b class="text-gray-700">{{ stats.count }}</b>/{{ persons.length }}</span>
      <span>平均: <b class="text-blue-600">{{ stats.avg }}</b></span>
      <span>最低: <b class="text-red-500">{{ stats.min }}</b></span>
      <span>最高: <b class="text-green-600">{{ stats.max }}</b></span>
    </div>

    <!-- Empty state -->
    <div v-if="!loading && persons.length === 0" class="text-sm text-gray-400 py-4 text-center">
      暂无人员数据。请确认目标关联了人员。
    </div>

    <!-- Grid -->
    <div v-else class="grid grid-cols-2 gap-2">
      <div
        v-for="person in persons"
        :key="person.id"
        class="flex items-center gap-3 px-3 py-2 bg-white border border-gray-200 rounded-lg"
      >
        <div
          class="w-7 h-7 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 text-white flex items-center justify-center text-xs font-bold shrink-0"
        >
          {{ person.name?.[0] || '?' }}
        </div>
        <span class="text-sm font-medium text-gray-700 flex-1 truncate">{{ person.name }}</span>
        <div class="flex items-center gap-1">
          <el-input-number
            :model-value="scoreMap[person.id] ?? undefined"
            @update:model-value="(val: number | undefined) => handleScoreChange(person.id, val ?? null)"
            :min="0"
            :max="maxScore"
            :step="1"
            :precision="0"
            :disabled="disabled"
            size="small"
            controls-position="right"
            placeholder="-"
            class="!w-24"
          />
          <span class="text-[10px] text-gray-400 shrink-0">/{{ maxScore }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
