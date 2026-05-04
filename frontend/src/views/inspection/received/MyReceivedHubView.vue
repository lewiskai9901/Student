<template>
  <div class="hub-page">
    <header class="hub-head">
      <div>
        <div class="rcv-eyebrow">受检主体 · Hub</div>
        <h1 class="rcv-title">我的受检中心</h1>
        <p class="hub-sub">
          班主任 / 场所负责人 / 受检单位的"被检查"视角. 一站式查看检查记录、整改任务、趋势与高频问题.
        </p>
      </div>
      <div v-if="summary" class="hub-org">
        <div class="org-label">覆盖组织</div>
        <div class="org-num">{{ summary.orgUnitCount }}</div>
        <div class="org-hint">个组织单元</div>
      </div>
    </header>

    <div v-loading="loading" class="hub-body">
      <!-- 顶部 KPI -->
      <div class="kpi-grid">
        <div class="kpi-card kpi-insp" @click="goTo('/inspection/received/inspections')">
          <div class="kpi-card-label">检查次数</div>
          <div class="kpi-card-num">{{ summary?.totalInspections ?? 0 }}</div>
          <div class="kpi-card-hint">近 30 天</div>
          <span class="kpi-card-link">查看记录 →</span>
        </div>
        <div class="kpi-card kpi-score">
          <div class="kpi-card-label">平均得分</div>
          <div class="kpi-card-num">
            {{ summary?.avgScore != null ? Number(summary.avgScore).toFixed(1) : '—' }}
          </div>
          <div class="kpi-card-hint">最近 30 天</div>
        </div>
        <div class="kpi-card kpi-correct" @click="goTo('/inspection/my-corrective')">
          <div class="kpi-card-label">待整改</div>
          <div class="kpi-card-num" :class="{ 'is-warn': (summary?.openCorrectives ?? 0) > 0 }">
            {{ summary?.openCorrectives ?? 0 }}
          </div>
          <div class="kpi-card-hint">未关闭整改单</div>
          <span class="kpi-card-link">去处理 →</span>
        </div>
        <div class="kpi-card kpi-overdue" @click="goTo('/inspection/my-corrective')">
          <div class="kpi-card-label">已逾期</div>
          <div class="kpi-card-num" :class="{ 'is-bad': (summary?.overdueCorrectives ?? 0) > 0 }">
            {{ summary?.overdueCorrectives ?? 0 }}
          </div>
          <div class="kpi-card-hint">超过 deadline</div>
          <span class="kpi-card-link" v-if="(summary?.overdueCorrectives ?? 0) > 0">立即处理 →</span>
        </div>
      </div>

      <!-- 提示: 如未挂任何组织 -->
      <div v-if="summary && summary.orgUnitCount === 0" class="hub-empty">
        <h3>您当前未关联任何受检组织</h3>
        <p>请联系管理员为您配置 access_relation (USER → MEMBER_OF/OWNER_OF/MANAGES → ORG_UNIT)
          后, 即可在此查看您所负责组织的检查情况.</p>
      </div>

      <!-- 快速入口 -->
      <div v-else class="entry-grid">
        <router-link to="/inspection/received/inspections" class="entry-card">
          <div class="entry-icon entry-i-insp">●</div>
          <div class="entry-info">
            <div class="entry-title">我被检查的记录</div>
            <div class="entry-desc">查看所有针对我所在组织的检查记录, 按时间倒序, 标记问题项.</div>
          </div>
        </router-link>

        <router-link to="/inspection/my-corrective" class="entry-card">
          <div class="entry-icon entry-i-correct">●</div>
          <div class="entry-info">
            <div class="entry-title">我的整改任务</div>
            <div class="entry-desc">领取并完成分配给我的整改单, 上传整改证据, 提交验收.</div>
          </div>
          <span v-if="(summary?.openCorrectives ?? 0) > 0" class="entry-badge">
            {{ summary?.openCorrectives }}
          </span>
        </router-link>

        <router-link to="/inspection/received/trends" class="entry-card">
          <div class="entry-icon entry-i-trend">●</div>
          <div class="entry-info">
            <div class="entry-title">检查趋势</div>
            <div class="entry-desc">4-26 周内得分率与问题数变化. 看团队进步还是退步.</div>
          </div>
        </router-link>

        <router-link to="/inspection/received/recurring" class="entry-card">
          <div class="entry-icon entry-i-recur">●</div>
          <div class="entry-info">
            <div class="entry-title">高频问题</div>
            <div class="entry-desc">过去 30 天反复出现的检查项 Top N. 优先解决, 显著提升整体得分.</div>
          </div>
        </router-link>

        <router-link to="/inspection/appeals/my" class="entry-card">
          <div class="entry-icon entry-i-appeal">●</div>
          <div class="entry-info">
            <div class="entry-title">我的申诉</div>
            <div class="entry-desc">对扣分有异议? 在此提交申诉, 等待审核员复核.</div>
          </div>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMySummary, type ReceivedSummary } from '@/api/inspection/received'

const router = useRouter()
const loading = ref(false)
const summary = ref<ReceivedSummary | null>(null)

async function reload() {
  loading.value = true
  try {
    summary.value = await getMySummary(30)
  } catch (e: unknown) {
    console.error(e)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function goTo(path: string) {
  router.push(path)
}

onMounted(reload)
</script>

<style scoped>
.hub-page { padding: 18px 24px; max-width: 1500px; margin: 0 auto; }

.hub-head {
  display: flex; align-items: flex-start; justify-content: space-between;
  padding: 18px 24px; margin-bottom: 18px;
  background: linear-gradient(135deg, #fef3c7 0%, #fff 60%);
  border: 1px solid #fde68a;
  border-radius: 8px;
}
.rcv-eyebrow { font-size: 11px; color: #92400e; letter-spacing: 0.05em; }
.rcv-title { font-size: 24px; font-weight: 700; color: #111827; margin: 4px 0 0; }
.hub-sub { color: #6b7280; font-size: 13px; max-width: 600px; margin: 8px 0 0; line-height: 1.6; }

.hub-org { text-align: center; }
.org-label { font-size: 11px; color: #92400e; }
.org-num { font-size: 32px; font-weight: 800; color: #b45309; font-family: ui-monospace, monospace; }
.org-hint { font-size: 11px; color: #9ca3af; }

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}
.kpi-card {
  position: relative;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}
.kpi-card:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,0.06); border-color: #d1d5db; }
.kpi-card-label { font-size: 12px; color: #6b7280; }
.kpi-card-num {
  font-size: 32px; font-weight: 700;
  font-family: ui-monospace, monospace;
  margin: 4px 0; color: #111827;
}
.kpi-card-num.is-warn { color: #b45309; }
.kpi-card-num.is-bad { color: #b91c1c; }
.kpi-card-hint { font-size: 11px; color: #9ca3af; }
.kpi-card-link {
  display: inline-block;
  margin-top: 6px;
  font-size: 11px; color: #2563eb;
  font-weight: 500;
}
.kpi-insp    { border-top: 3px solid #6366f1; }
.kpi-score   { border-top: 3px solid #14b8a6; cursor: default; }
.kpi-score:hover { transform: none; box-shadow: none; }
.kpi-correct { border-top: 3px solid #f59e0b; }
.kpi-overdue { border-top: 3px solid #dc2626; }

.hub-empty {
  padding: 40px;
  text-align: center;
  background: #fff;
  border: 1px dashed #e5e7eb;
  border-radius: 8px;
}
.hub-empty h3 { font-size: 16px; color: #6b7280; margin-bottom: 8px; }
.hub-empty p { font-size: 13px; color: #9ca3af; max-width: 500px; margin: 0 auto; line-height: 1.6; }

.entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}
.entry-card {
  position: relative;
  display: flex; align-items: flex-start; gap: 14px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  text-decoration: none; color: inherit;
  transition: all 0.15s;
}
.entry-card:hover {
  border-color: #93c5fd;
  background: #f0f9ff;
  box-shadow: 0 4px 12px rgba(59,130,246,0.08);
}
.entry-icon {
  width: 40px; height: 40px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: 8px;
  font-size: 18px;
  color: #fff;
}
.entry-i-insp { background: #6366f1; }
.entry-i-correct { background: #f59e0b; }
.entry-i-trend { background: #14b8a6; }
.entry-i-recur { background: #dc2626; }
.entry-i-appeal { background: #8b5cf6; }
.entry-info { flex: 1; min-width: 0; }
.entry-title { font-size: 14px; font-weight: 600; color: #111827; }
.entry-desc { font-size: 12px; color: #6b7280; margin-top: 4px; line-height: 1.5; }
.entry-badge {
  position: absolute; top: 12px; right: 12px;
  padding: 2px 8px;
  background: #fef3c7; color: #92400e;
  border-radius: 10px;
  font-size: 11px; font-weight: 600;
}

@media (max-width: 768px) {
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
  .hub-head { flex-direction: column; gap: 12px; }
}
</style>
