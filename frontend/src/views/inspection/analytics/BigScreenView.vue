<template>
  <div class="big-screen">
    <header class="big-screen-header">
      <h1>检查数据大屏</h1>
      <div class="header-right">
        <span class="datetime">{{ currentTime }}</span>
        <el-button size="small" text style="color:#8cf" @click="toggleFullscreen">
          {{ isFullscreen ? '退出全屏' : '全屏' }}
        </el-button>
        <el-button size="small" text style="color:#8cf" @click="$router.back()">返回</el-button>
      </div>
    </header>

    <div class="big-screen-body">
      <!-- Top row: key metrics -->
      <div class="metric-row">
        <div class="metric-card" v-for="m in metrics" :key="m.label">
          <div class="metric-value">{{ m.value }}</div>
          <div class="metric-label">{{ m.label }}</div>
        </div>
      </div>

      <!-- Middle row: charts -->
      <div class="chart-row">
        <div class="chart-panel">
          <h3>检查趋势</h3>
          <div class="chart-placeholder">
            <div v-for="(v, i) in trendData" :key="i" class="bar-item">
              <div class="bar" :style="{ height: v.pct + '%' }"></div>
              <span class="bar-label">{{ v.label }}</span>
            </div>
          </div>
        </div>
        <div class="chart-panel">
          <h3>部门得分排名</h3>
          <div class="rank-list">
            <div v-for="(r, i) in rankData" :key="i" class="rank-item">
              <span class="rank-no" :class="{ top3: i < 3 }">{{ i + 1 }}</span>
              <span class="rank-name">{{ r.name }}</span>
              <span class="rank-score">{{ r.score }}</span>
              <div class="rank-bar-bg"><div class="rank-bar" :style="{ width: r.pct + '%' }"></div></div>
            </div>
          </div>
        </div>
        <div class="chart-panel">
          <h3>问题分类</h3>
          <div class="category-list">
            <div v-for="c in categoryData" :key="c.name" class="cat-item">
              <span class="cat-name">{{ c.name }}</span>
              <div class="cat-bar-bg"><div class="cat-bar" :style="{ width: c.pct + '%', background: c.color }"></div></div>
              <span class="cat-count">{{ c.count }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Bottom row: live feed + status -->
      <div class="bottom-row">
        <div class="chart-panel wide">
          <h3>实时动态</h3>
          <div class="live-feed">
            <div v-for="(item, i) in liveFeed" :key="i" class="feed-item">
              <span class="feed-time">{{ item.time }}</span>
              <span class="feed-text">{{ item.text }}</span>
            </div>
            <div v-if="!liveFeed.length" class="text-center text-gray-500 py-8">暂无实时数据</div>
          </div>
        </div>
        <div class="chart-panel">
          <h3>检查完成率</h3>
          <div class="completion-ring">
            <svg viewBox="0 0 120 120" class="ring-svg">
              <circle cx="60" cy="60" r="50" class="ring-bg" />
              <circle cx="60" cy="60" r="50" class="ring-fill"
                :stroke-dasharray="314" :stroke-dashoffset="314 * (1 - completionRate / 100)" />
            </svg>
            <div class="ring-text">{{ completionRate }}%</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const currentTime = ref('')
const isFullscreen = ref(false)
const completionRate = ref(87)

const metrics = ref([
  { label: '今日检查', value: 24 },
  { label: '完成任务', value: 18 },
  { label: '待整改', value: 6 },
  { label: '平均得分', value: '92.5' },
])

const trendData = ref([
  { label: '周一', pct: 60 }, { label: '周二', pct: 75 }, { label: '周三', pct: 85 },
  { label: '周四', pct: 70 }, { label: '周五', pct: 90 }, { label: '周六', pct: 45 }, { label: '周日', pct: 30 },
])

const rankData = ref([
  { name: '一部门', score: 96.5, pct: 96.5 }, { name: '二部门', score: 94.2, pct: 94.2 },
  { name: '三部门', score: 91.8, pct: 91.8 }, { name: '四部门', score: 89.3, pct: 89.3 },
  { name: '五部门', score: 87.1, pct: 87.1 },
])

const categoryData = ref([
  { name: '卫生', count: 12, pct: 80, color: '#f56c6c' },
  { name: '纪律', count: 8, pct: 53, color: '#e6a23c' },
  { name: '安全', count: 5, pct: 33, color: '#409eff' },
  { name: '设备', count: 3, pct: 20, color: '#67c23a' },
])

const liveFeed = ref([
  { time: '14:32', text: '张三 完成了 A楼3层 的卫生检查 - 得分 95' },
  { time: '14:28', text: '李四 开始检查 B楼2层' },
  { time: '14:15', text: '王五 提交了整改报告 #C-0032' },
])

let timer: ReturnType<typeof setInterval>

function updateTime() {
  currentTime.value = new Date().toLocaleString('zh-CN', { hour12: false })
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.big-screen {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
}
.big-screen-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.big-screen-header h1 {
  font-size: 20px;
  font-weight: 600;
  background: linear-gradient(90deg, #60a5fa, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.header-right { display: flex; align-items: center; gap: 16px; }
.datetime { font-size: 13px; color: #94a3b8; font-variant-numeric: tabular-nums; }

.big-screen-body { flex: 1; padding: 16px 24px; display: flex; flex-direction: column; gap: 16px; }

.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.metric-card {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
}
.metric-value { font-size: 32px; font-weight: 700; color: #60a5fa; }
.metric-label { font-size: 13px; color: #94a3b8; margin-top: 4px; }

.chart-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px; flex: 1; }
.chart-panel {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
}
.chart-panel h3 { font-size: 14px; font-weight: 500; color: #cbd5e1; margin-bottom: 12px; }
.chart-panel.wide { flex: 2; }

.bottom-row { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; }

/* Simple bar chart */
.chart-placeholder { display: flex; align-items: flex-end; gap: 8px; flex: 1; padding-top: 8px; }
.bar-item { flex: 1; display: flex; flex-direction: column; align-items: center; }
.bar { width: 100%; background: linear-gradient(180deg, #60a5fa, #3b82f6); border-radius: 4px 4px 0 0; min-height: 4px; transition: height 0.5s; }
.bar-label { font-size: 11px; color: #94a3b8; margin-top: 6px; }

/* Rank list */
.rank-list { display: flex; flex-direction: column; gap: 8px; }
.rank-item { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.rank-no { width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; border-radius: 4px; font-size: 11px; font-weight: 600; background: rgba(255,255,255,0.1); }
.rank-no.top3 { background: #f59e0b; color: #0f172a; }
.rank-name { width: 60px; }
.rank-score { width: 40px; text-align: right; color: #60a5fa; font-weight: 500; }
.rank-bar-bg { flex: 1; height: 6px; background: rgba(255,255,255,0.08); border-radius: 3px; }
.rank-bar { height: 100%; background: #3b82f6; border-radius: 3px; transition: width 0.5s; }

/* Category */
.category-list { display: flex; flex-direction: column; gap: 10px; }
.cat-item { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.cat-name { width: 50px; }
.cat-bar-bg { flex: 1; height: 8px; background: rgba(255,255,255,0.08); border-radius: 4px; }
.cat-bar { height: 100%; border-radius: 4px; transition: width 0.5s; }
.cat-count { width: 30px; text-align: right; color: #94a3b8; }

/* Live feed */
.live-feed { flex: 1; overflow-y: auto; }
.feed-item { display: flex; gap: 12px; padding: 8px 0; border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 13px; }
.feed-time { color: #60a5fa; font-variant-numeric: tabular-nums; white-space: nowrap; }
.feed-text { color: #cbd5e1; }

/* Completion ring */
.completion-ring { display: flex; align-items: center; justify-content: center; flex: 1; position: relative; }
.ring-svg { width: 120px; height: 120px; }
.ring-bg { fill: none; stroke: rgba(255,255,255,0.08); stroke-width: 8; }
.ring-fill { fill: none; stroke: #60a5fa; stroke-width: 8; stroke-linecap: round; transform: rotate(-90deg); transform-origin: center; transition: stroke-dashoffset 0.5s; }
.ring-text { position: absolute; font-size: 28px; font-weight: 700; color: #60a5fa; }
</style>
