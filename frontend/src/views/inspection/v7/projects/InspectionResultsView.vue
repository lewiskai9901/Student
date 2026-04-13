<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { BarChart3, ChevronRight, Trophy } from 'lucide-vue-next'
import { inspProjectApi } from '@/api/insp/project'
import { getSubmissions } from '@/api/insp/submission'
import type { InspProject, InspSubmission } from '@/types/insp/project'

const router = useRouter()
const loading = ref(false)
const projects = ref<InspProject[]>([])

interface ProjectResult {
  project: InspProject
  scores: { targetName: string; score: number; grade: string | null }[]
  avgScore: number | null
  topTarget: string | null
}
const results = ref<ProjectResult[]>([])

async function loadData() {
  loading.value = true
  try {
    const allProjects = await inspProjectApi.getList()
    // Only show PUBLISHED or COMPLETED projects that have been actively used
    projects.value = allProjects.filter(p =>
      ['PUBLISHED', 'COMPLETED'].includes(p.status)
    )

    const loaded: ProjectResult[] = []
    for (const project of projects.value) {
      try {
        const submissions = await getSubmissions(project.id).catch(() => [])
        const completed = submissions.filter(
          (s: InspSubmission) => s.status === 'COMPLETED' && s.finalScore != null
        )
        if (completed.length === 0) {
          loaded.push({ project, scores: [], avgScore: null, topTarget: null })
          continue
        }

        // Group by target, average scores
        const byTarget = new Map<string, { name: string; scores: number[]; grade: string | null }>()
        for (const s of completed) {
          const name = s.targetName || `#${s.targetId}`
          if (!byTarget.has(name)) byTarget.set(name, { name, scores: [], grade: null })
          const entry = byTarget.get(name)!
          entry.scores.push(s.finalScore!)
          if (s.grade) entry.grade = s.grade
        }

        const scores = [...byTarget.values()]
          .map(g => ({
            targetName: g.name,
            score: Math.round((g.scores.reduce((a, b) => a + b, 0) / g.scores.length) * 100) / 100,
            grade: g.grade,
          }))
          .sort((a, b) => b.score - a.score)

        const avgScore = scores.length > 0
          ? Math.round((scores.reduce((s, x) => s + x.score, 0) / scores.length) * 100) / 100
          : null

        loaded.push({
          project,
          scores,
          avgScore,
          topTarget: scores.length > 0 ? scores[0].targetName : null,
        })
      } catch {
        loaded.push({ project, scores: [], avgScore: null, topTarget: null })
      }
    }
    results.value = loaded
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function goToProject(projectId: number) {
  router.push(`/inspection/v7/projects/${projectId}`)
}

function getScoreColor(score: number): string {
  if (score >= 85) return '#10b981'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

onMounted(() => loadData())
</script>

<template>
  <div class="results-page" v-loading="loading">
    <header class="results-header">
      <BarChart3 class="w-5 h-5" style="color: #1a6dff" />
      <span class="results-title">检查结果</span>
    </header>

    <div class="results-body">
      <div v-if="results.length === 0 && !loading" class="results-empty">
        暂无已发布的检查项目
      </div>

      <div v-for="r in results" :key="r.project.id" class="result-card" @click="goToProject(r.project.id)">
        <div class="rc-header">
          <span class="rc-name">{{ r.project.projectName }}</span>
          <ChevronRight class="w-4 h-4 text-gray-400" />
        </div>

        <div class="rc-stats">
          <div v-if="r.avgScore != null" class="rc-stat">
            <span class="rc-stat-num" :style="{ color: getScoreColor(r.avgScore) }">{{ r.avgScore }}</span>
            <span class="rc-stat-label">平均分</span>
          </div>
          <div class="rc-stat">
            <span class="rc-stat-num">{{ r.scores.length }}</span>
            <span class="rc-stat-label">已评目标</span>
          </div>
          <div v-if="r.topTarget" class="rc-stat">
            <Trophy class="w-3.5 h-3.5" style="color: #f59e0b" />
            <span class="rc-stat-label">{{ r.topTarget }}</span>
          </div>
        </div>

        <!-- Top 5 ranking preview -->
        <div v-if="r.scores.length > 0" class="rc-ranking">
          <div v-for="(s, i) in r.scores.slice(0, 5)" :key="s.targetName" class="rc-rank-row">
            <span class="rc-rank-num" :class="{ gold: i === 0, silver: i === 1, bronze: i === 2 }">{{ i + 1 }}</span>
            <span class="rc-rank-name">{{ s.targetName }}</span>
            <span class="rc-rank-score" :style="{ color: getScoreColor(s.score) }">{{ s.score }}</span>
            <span v-if="s.grade" class="rc-rank-grade">{{ s.grade }}</span>
          </div>
        </div>
        <div v-else class="rc-no-scores">暂无成绩数据</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.results-page { background: #f7f8fa; min-height: 100vh; }
.results-header {
  position: sticky; top: 0; z-index: 10;
  height: 48px; background: #fff; border-bottom: 1px solid #e5e7eb;
  display: flex; align-items: center; gap: 8px; padding: 0 24px;
}
.results-title { font-size: 16px; font-weight: 700; color: #1a1a2e; }
.results-body {
  max-width: 1100px; margin: 0 auto;
  padding: 16px 20px 60px;
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px;
}
.results-empty {
  grid-column: 1 / -1;
  text-align: center; padding: 60px; color: #9ca3af; font-size: 14px;
}

.result-card {
  background: #fff; border: 1px solid #e5e7eb; border-radius: 10px;
  padding: 14px 16px; cursor: pointer;
  transition: box-shadow 0.15s, border-color 0.15s;
}
.result-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); border-color: #d1d5db; }

.rc-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 10px;
}
.rc-name { font-size: 14px; font-weight: 700; color: #1a1a2e; }

.rc-stats { display: flex; gap: 16px; margin-bottom: 10px; }
.rc-stat { display: flex; align-items: baseline; gap: 4px; }
.rc-stat-num { font-size: 18px; font-weight: 700; color: #1a1a2e; }
.rc-stat-label { font-size: 12px; color: #9ca3af; }

.rc-ranking { border-top: 1px solid #f3f4f6; padding-top: 8px; }
.rc-rank-row {
  display: flex; align-items: center; gap: 8px;
  padding: 3px 0; font-size: 12.5px;
}
.rc-rank-num {
  width: 18px; height: 18px; border-radius: 50%;
  background: #f3f4f6; color: #6b7280;
  display: flex; align-items: center; justify-content: center;
  font-size: 10px; font-weight: 700; flex-shrink: 0;
}
.rc-rank-num.gold { background: #fef3c7; color: #d97706; }
.rc-rank-num.silver { background: #f3f4f6; color: #6b7280; }
.rc-rank-num.bronze { background: #fed7aa; color: #c2410c; }
.rc-rank-name { flex: 1; color: #1a1a2e; font-weight: 500; }
.rc-rank-score { font-weight: 700; }
.rc-rank-grade { font-size: 11px; padding: 1px 6px; border-radius: 6px; background: #f3f4f6; color: #6b7280; }
.rc-no-scores { font-size: 12px; color: #9ca3af; padding: 8px 0; }

@media (max-width: 700px) {
  .results-body { grid-template-columns: 1fr; padding: 12px; }
}
</style>
