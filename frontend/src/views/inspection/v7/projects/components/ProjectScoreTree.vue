<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { inspProjectApi } from '@/api/insp/project'
import { TargetTypeConfig, ProjectStatusConfig, type TargetType, type ProjectStatus } from '@/types/insp/enums'
import type { InspProject, ProjectScore } from '@/types/insp/project'

interface ScoreNode {
  project: InspProject
  scores: ProjectScore[]
  children: ScoreNode[]
  latestScore: ProjectScore | null
}

const props = defineProps<{
  projectId: number
}>()

const loading = ref(false)
const tree = ref<ScoreNode | null>(null)

function mapNode(raw: any): ScoreNode {
  const scores: ProjectScore[] = raw.scores ?? []
  return {
    project: raw.project,
    scores,
    children: (raw.children ?? []).map(mapNode),
    latestScore: scores.length > 0 ? scores[0] : null,
  }
}

async function loadTree() {
  loading.value = true
  try {
    const raw = await inspProjectApi.getScoreTree(props.projectId)
    tree.value = mapNode(raw)
  } catch (e) {
    console.error('加载分数树失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTree()
})
</script>

<template>
  <div v-loading="loading">
    <div v-if="tree" class="score-tree">
      <!-- Root -->
      <div class="score-node score-node--root">
        <div class="score-node__header">
          <span class="font-medium">{{ tree.project.projectName }}</span>
          <el-tag v-if="tree.project.targetType" size="small" type="info">
            {{ TargetTypeConfig[tree.project.targetType as TargetType]?.label }}
          </el-tag>
          <el-tag size="small" :type="(ProjectStatusConfig[tree.project.status as ProjectStatus]?.type as any)">
            {{ ProjectStatusConfig[tree.project.status as ProjectStatus]?.label }}
          </el-tag>
        </div>
        <div class="score-node__score" v-if="tree.latestScore">
          <span class="text-2xl font-bold" :class="tree.latestScore.score != null && tree.latestScore.score >= 60 ? 'text-green-600' : 'text-red-500'">
            {{ tree.latestScore.score ?? '-' }}
          </span>
          <span class="text-sm text-gray-500 ml-2" v-if="tree.latestScore.grade">{{ tree.latestScore.grade }}</span>
          <span class="text-xs text-gray-400 ml-2">{{ tree.latestScore.cycleDate }}</span>
        </div>
        <div v-else class="text-sm text-gray-400">暂无分数</div>

        <!-- Children -->
        <div v-if="tree.children.length > 0" class="score-node__children">
          <div v-for="child in tree.children" :key="child.project.id" class="score-node score-node--child">
            <div class="score-node__header">
              <span class="text-gray-400 mr-1">└</span>
              <span class="font-medium text-sm">{{ child.project.projectName }}</span>
              <el-tag v-if="child.project.targetType" size="small" type="info">
                {{ TargetTypeConfig[child.project.targetType as TargetType]?.label }}
              </el-tag>
            </div>
            <div class="score-node__score" v-if="child.latestScore">
              <span class="text-lg font-semibold" :class="child.latestScore.score != null && child.latestScore.score >= 60 ? 'text-green-600' : 'text-red-500'">
                {{ child.latestScore.score ?? '-' }}
              </span>
              <span class="text-xs text-gray-500 ml-1" v-if="child.latestScore.grade">{{ child.latestScore.grade }}</span>
              <span class="text-xs text-gray-400 ml-1">目标数: {{ child.latestScore.targetCount }}</span>
            </div>
            <div v-else class="text-xs text-gray-400 pl-5">暂无分数</div>

            <!-- Grandchildren (2 levels should suffice) -->
            <div v-if="child.children.length > 0" class="ml-5 mt-1 space-y-1">
              <div v-for="gc in child.children" :key="gc.project.id" class="flex items-center gap-2 text-xs text-gray-500">
                <span class="text-gray-300">└</span>
                <span>{{ gc.project.projectName }}</span>
                <span class="font-medium">{{ gc.latestScore?.score ?? '-' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading && !tree" class="text-sm text-gray-400 py-8 text-center">
      暂无数据
    </div>
  </div>
</template>

<style scoped>
.score-node {
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}
.score-node--root {
  border-color: #d1d5db;
}
.score-node__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.score-node__children {
  margin-top: 12px;
  padding-left: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.score-node--child {
  border-color: #e5e7eb;
  background: #fafafa;
}
</style>
