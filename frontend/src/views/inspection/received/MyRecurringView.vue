<template>
  <div class="rec-page">
    <header class="rec-head">
      <div>
        <div class="rcv-eyebrow">受检主体 · Recurring Issues</div>
        <h1 class="rcv-title">高频问题</h1>
        <p class="rec-sub">
          过去 30 天反复出现的检查项 — 优先解决可大幅提升整体得分.
        </p>
      </div>
      <div class="rec-toolbar">
        <span>显示前</span>
        <el-select v-model="limit" size="small" style="width:80px" @change="reload">
          <el-option label="5" :value="5" />
          <el-option label="10" :value="10" />
          <el-option label="20" :value="20" />
          <el-option label="50" :value="50" />
        </el-select>
        <el-button size="small" @click="reload">刷新</el-button>
      </div>
    </header>

    <div v-loading="loading" class="rec-body">
      <div v-if="!loading && items.length === 0" class="rcv-empty">
        过去 30 天暂无问题项 — 表现优秀!
      </div>

      <ol v-else class="rec-list">
        <li v-for="(it, i) in items" :key="it.itemCode"
            class="rec-row"
            :class="{ 'top3': i < 3 }">
          <div class="rec-rank">
            <span class="rank-num">{{ i + 1 }}</span>
            <span v-if="i === 0" class="rank-medal">●</span>
          </div>
          <div class="rec-info">
            <div class="rec-name">{{ it.itemName || it.itemCode }}</div>
            <div class="rec-meta">
              <span v-if="it.sectionName" class="rec-section">{{ it.sectionName }}</span>
              <span class="rec-code">{{ it.itemCode }}</span>
              <span class="rec-last">最近 {{ fmtRel(it.lastSeenAt) }}</span>
            </div>
          </div>
          <div class="rec-count">
            <span class="rec-num">{{ it.recurCount }}</span>
            <span class="rec-unit">次</span>
          </div>
        </li>
      </ol>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyRecurring, type ReceivedRecurringItem } from '@/api/inspection/received'

const loading = ref(false)
const limit = ref(10)
const items = ref<ReceivedRecurringItem[]>([])

async function reload() {
  loading.value = true
  try {
    items.value = await getMyRecurring(limit.value)
  } catch (e: unknown) {
    console.error(e); ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function fmtRel(s: string): string {
  if (!s) return '-'
  const d = new Date(s)
  const ms = Date.now() - d.getTime()
  const day = Math.floor(ms / 86400000)
  if (day === 0) return '今天'
  if (day === 1) return '昨天'
  if (day < 30) return day + ' 天前'
  return d.getMonth() + 1 + '/' + d.getDate()
}

onMounted(reload)
</script>

<style scoped>
.rec-page { padding: 18px 24px; max-width: 1100px; margin: 0 auto; }
.rec-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px; margin-bottom: 16px;
  background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
}
.rcv-eyebrow { font-size: 11px; color: #9ca3af; letter-spacing: 0.05em; }
.rcv-title { font-size: 22px; font-weight: 700; color: #111827; margin: 4px 0 0; }
.rec-sub { color: #6b7280; font-size: 13px; margin: 4px 0 0; }
.rec-toolbar { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #6b7280; }
.rcv-empty {
  padding: 60px; text-align: center; color: #047857;
  background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px;
}

.rec-list {
  list-style: none; padding: 0; margin: 0;
  display: flex; flex-direction: column; gap: 6px;
}
.rec-row {
  display: grid;
  grid-template-columns: 50px 1fr auto;
  gap: 14px; align-items: center;
  padding: 12px 16px;
  background: #fff; border: 1px solid #e5e7eb; border-radius: 6px;
}
.rec-row.top3 { border-left: 3px solid #b91c1c; background: #fef2f2; }
.rec-row.top3:nth-child(2) { border-left-color: #d97706; background: #fffbeb; }
.rec-row.top3:nth-child(3) { border-left-color: #d97706; background: #fff; }

.rec-rank {
  display: flex; align-items: center; gap: 4px;
  justify-content: center;
}
.rank-num {
  font-size: 18px; font-weight: 700;
  font-family: ui-monospace, monospace;
  color: #6b7280;
}
.top3 .rank-num { color: #b91c1c; font-size: 22px; }
.rank-medal { font-size: 10px; color: #f59e0b; }

.rec-info { min-width: 0; }
.rec-name { font-size: 14px; font-weight: 500; color: #111827; }
.rec-meta {
  font-size: 11px; color: #9ca3af;
  margin-top: 4px; display: flex; gap: 10px; flex-wrap: wrap;
}
.rec-section {
  background: #f3f4f6; padding: 1px 6px; border-radius: 3px; color: #4b5563;
}
.rec-code { font-family: ui-monospace, monospace; }
.rec-last { color: #b45309; }

.rec-count { text-align: right; }
.rec-num {
  font-size: 24px; font-weight: 700;
  font-family: ui-monospace, monospace;
  color: #b91c1c;
}
.top3 .rec-num { color: #991b1b; font-size: 28px; }
.rec-unit { font-size: 12px; color: #9ca3af; margin-left: 2px; }
</style>
