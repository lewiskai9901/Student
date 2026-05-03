<script setup lang="ts">
/**
 * ScoringProfileListView — 评分方案独立列表 (P0-2 Road to A)
 *
 * 评分方案是跨模板复用的强独立资源 (一套规则可被 N 个模板挂),
 * 之前只能从模板编辑器进, 没有列表页. 这里补上一等公民入口.
 */
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, ExternalLink } from 'lucide-vue-next'
import { getProfiles } from '@/api/inspection/scoring'
import type { ScoringProfile } from '@/types/insp/scoring'

const router = useRouter()
const loading = ref(false)
const profiles = ref<ScoringProfile[]>([])
const keyword = ref('')

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return profiles.value
  return profiles.value.filter(p =>
    String(p.id).includes(kw) ||
    String(p.sectionId || '').includes(kw)
  )
})

const stats = computed(() => ({
  total: profiles.value.length,
  withCalibration: profiles.value.filter(p => p.calibrationEnabled).length,
  withTrend: profiles.value.filter(p => p.trendFactorEnabled).length,
  withDecay: profiles.value.filter(p => p.decayEnabled).length,
}))

async function load() {
  loading.value = true
  try {
    profiles.value = await getProfiles()
  } catch (e: any) {
    ElMessage.error(e.message || '加载评分方案失败')
  } finally {
    loading.value = false
  }
}

function goEdit(profile: ScoringProfile) {
  router.push(`/inspection/scoring/${profile.id}`)
}

function goSection(sectionId: number) {
  router.push(`/inspection/templates/${sectionId}/edit`)
}

onMounted(load)
</script>

<template>
  <div class="insp-shell sp-page">
    <header class="sp-head">
      <div class="sp-head__lead">
        <span class="insp-eyebrow">评分方案 · Scoring Profiles</span>
        <h1 class="sp-title">评分方案</h1>
      </div>
      <div class="sp-stats">
        <div class="sp-stat">
          <span class="sp-stat__num insp-num">{{ stats.total }}</span>
          <span class="sp-stat__label">总数</span>
        </div>
        <div class="sp-stat">
          <span class="sp-stat__num insp-num" style="color: var(--insp-info)">{{ stats.withCalibration }}</span>
          <span class="sp-stat__label">启用校准</span>
        </div>
        <div class="sp-stat">
          <span class="sp-stat__num insp-num" style="color: var(--insp-pass)">{{ stats.withTrend }}</span>
          <span class="sp-stat__label">含趋势因子</span>
        </div>
        <div class="sp-stat">
          <span class="sp-stat__num insp-num" style="color: var(--insp-warn)">{{ stats.withDecay }}</span>
          <span class="sp-stat__label">含衰减</span>
        </div>
      </div>
    </header>

    <div class="sp-toolbar">
      <div class="sp-search">
        <Search :size="12" class="sp-search__icon" />
        <input v-model="keyword" placeholder="按 ID 或所属分区搜索..." class="sp-search__input" />
      </div>
      <span class="sp-total"><span class="insp-num">{{ filtered.length }}</span> 个方案</span>
    </div>

    <section class="sp-list" v-loading="loading">
      <div v-if="!loading && filtered.length === 0" class="sp-empty">
        <p>暂无评分方案</p>
        <p class="sp-empty__sub">评分方案在模板编辑器中通过分区配置, 这里可全局查看与编辑</p>
      </div>

      <div v-else class="sp-rows">
        <div class="sp-row sp-row--head">
          <span class="col col-id">ID</span>
          <span class="col col-section">所属分区</span>
          <span class="col col-score">分值</span>
          <span class="col col-mode">多评分模式</span>
          <span class="col col-features">高级特性</span>
          <span class="col col-actions">操作</span>
        </div>

        <div v-for="p in filtered" :key="p.id" class="sp-row sp-row--data" @click="goEdit(p)">
          <span class="col col-id insp-num">#{{ p.id }}</span>

          <span class="col col-section">
            <button v-if="p.sectionId" class="sp-section-link" @click.stop="goSection(p.sectionId)">
              <span class="insp-num">分区 #{{ p.sectionId }}</span>
              <ExternalLink :size="11" />
            </button>
            <span v-else class="dim">未关联</span>
          </span>

          <span class="col col-score insp-num">
            {{ p.minScore }} – {{ p.maxScore }}
            <span class="dim">· {{ p.precisionDigits }} 位精度</span>
          </span>

          <span class="col col-mode">{{ p.multiRaterMode || '—' }}</span>

          <span class="col col-features">
            <span v-if="p.calibrationEnabled" class="sp-feat sp-feat--info">校准</span>
            <span v-if="p.trendFactorEnabled" class="sp-feat sp-feat--pass">趋势</span>
            <span v-if="p.decayEnabled" class="sp-feat sp-feat--warn">衰减</span>
            <span v-if="!p.calibrationEnabled && !p.trendFactorEnabled && !p.decayEnabled" class="dim">基础</span>
          </span>

          <div class="col col-actions" @click.stop>
            <button class="insp-btn insp-btn--sm" @click="goEdit(p)">编辑</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.sp-page { padding: 12px 16px; }
.sp-head {
  display: flex; align-items: center; gap: 16px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 12px 16px; margin-bottom: 10px;
}
.sp-head__lead { display: flex; flex-direction: column; gap: 2px; }
.sp-title { font-size: 17px; font-weight: 700; margin: 2px 0 0; color: var(--insp-ink-primary); }
.sp-stats { display: flex; gap: 6px; margin-left: auto; }
.sp-stat {
  display: inline-flex; flex-direction: column; align-items: center;
  min-width: 64px; padding: 4px 12px;
  border-radius: var(--insp-radius-sm);
}
.sp-stat__num {
  font-family: var(--insp-font-mono);
  font-size: 17px; font-weight: 700; line-height: 1;
  color: var(--insp-ink-primary);
}
.sp-stat__label {
  font-size: 10px; color: var(--insp-ink-tertiary);
  letter-spacing: 0.04em; text-transform: uppercase; margin-top: 2px;
}

.sp-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 8px 14px; margin-bottom: 10px;
}
.sp-search { position: relative; display: flex; align-items: center; }
.sp-search__icon { position: absolute; left: 8px; color: var(--insp-ink-tertiary); }
.sp-search__input {
  height: 26px; padding: 0 10px 0 24px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  font-size: 12px; font-family: inherit; width: 260px;
  background: var(--insp-bg-surface);
}
.sp-search__input:focus {
  outline: none; border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}
.sp-total { font-size: 11px; color: var(--insp-ink-tertiary); }

.sp-list {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  overflow: hidden;
}
.sp-empty { padding: 60px 20px; text-align: center; color: var(--insp-ink-tertiary); }
.sp-empty p { margin: 0 0 4px; font-size: 13px; }
.sp-empty__sub { font-size: 11px; color: var(--insp-ink-quaternary); }

.sp-row {
  display: grid;
  grid-template-columns: 60px 1fr 1.2fr 1fr 1.2fr 100px;
  gap: 10px; align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid var(--insp-border-subtle);
}
.sp-row--head {
  background: var(--insp-bg-subtle);
  font-size: 10px; font-weight: 600; color: var(--insp-ink-tertiary);
  text-transform: uppercase; letter-spacing: 0.04em; padding: 8px 14px;
}
.sp-row--data { cursor: pointer; transition: background var(--insp-t-fast); }
.sp-row--data:hover { background: var(--insp-bg-subtle); }
.col { min-width: 0; font-size: 12px; color: var(--insp-ink-secondary); }
.col-id { font-weight: 600; color: var(--insp-ink-primary); }
.dim { color: var(--insp-ink-quaternary); }

.sp-section-link {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 2px 8px; background: var(--insp-bg-subtle);
  border: 1px solid var(--insp-border-subtle);
  border-radius: 3px;
  color: var(--insp-accent); font-size: 11px;
  font-family: inherit; cursor: pointer;
}
.sp-section-link:hover { border-color: var(--insp-accent); }

.sp-feat {
  display: inline-flex; padding: 1px 6px; border-radius: 3px;
  font-size: 10px; font-weight: 600; margin-right: 4px;
}
.sp-feat--info { background: var(--insp-info-pale, var(--insp-bg-subtle)); color: var(--insp-info); }
.sp-feat--pass { background: var(--insp-pass-pale, #dcfce7); color: var(--insp-pass); }
.sp-feat--warn { background: var(--insp-warn-pale, #fef3c7); color: var(--insp-warn); }

.col-actions { display: flex; gap: 4px; justify-content: flex-end; }
</style>
