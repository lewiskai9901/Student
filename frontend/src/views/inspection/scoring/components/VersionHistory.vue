<template>
  <div class="vh-root">
    <div class="vh-head">
      <div class="vh-title">
        <History :size="14" class="vh-icon" />
        <span>版本历史</span>
      </div>
      <button class="vh-publish-btn" @click="showPublish = true">
        发布版本
      </button>
    </div>

    <div v-if="versions.length === 0" class="vh-empty">
      暂无版本记录
    </div>

    <div v-else class="vh-list">
      <div
        v-for="ver in versions"
        :key="ver.id"
        class="vh-item"
        :class="{ active: expandedVersion === ver.version }"
        @click="toggleExpand(ver)"
      >
        <div class="vh-item-head">
          <span class="vh-ver-badge">v{{ ver.version }}</span>
          <span class="vh-time">{{ formatTime(ver.publishedAt) }}</span>
        </div>
        <div v-if="ver.changeSummary" class="vh-summary">{{ ver.changeSummary }}</div>
        <div v-if="expandedVersion === ver.version && snapshotPreview" class="vh-snapshot">
          <div class="vh-snap-row">
            <span class="vh-snap-label">分区</span>
            <span class="vh-snap-val">{{ snapshotPreview.dimensionCount }} 个</span>
          </div>
          <div class="vh-snap-row">
            <span class="vh-snap-label">等级</span>
            <span class="vh-snap-val">{{ snapshotPreview.gradeBandCount }} 个</span>
          </div>
          <div class="vh-snap-row">
            <span class="vh-snap-label">规则</span>
            <span class="vh-snap-val">{{ snapshotPreview.ruleCount }} 条</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Publish dialog -->
    <Teleport to="body">
      <Transition name="sp-modal">
        <div v-if="showPublish" class="sp-mask" @click.self="showPublish = false">
          <div class="sp-modal" style="width:420px;">
            <div class="sp-modal-head">
              <h3>发布评分配置版本</h3>
              <button class="sp-modal-close" @click="showPublish = false">&times;</button>
            </div>
            <div class="sp-modal-body">
              <div class="sp-fld">
                <label>变更说明</label>
                <textarea
                  v-model="changeSummary"
                  rows="3"
                  placeholder="描述此版本的变更内容..."
                ></textarea>
              </div>
              <p class="vh-publish-hint">
                发布后将生成当前配置的完整快照，版本号: v{{ (currentVersion || 0) + 1 }}
              </p>
            </div>
            <div class="sp-modal-foot">
              <button class="sp-btn-ghost" @click="showPublish = false">取消</button>
              <button class="sp-btn-primary" @click="handlePublish" :disabled="publishing">
                {{ publishing ? '发布中...' : '发布' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { History } from 'lucide-vue-next'
import type { ScoringProfileVersion } from '@/types/insp/scoring'

const props = defineProps<{
  versions: ScoringProfileVersion[]
  currentVersion: number
}>()

const emit = defineEmits<{
  publish: [changeSummary: string]
}>()

const showPublish = ref(false)
const changeSummary = ref('')
const publishing = ref(false)
const expandedVersion = ref<number | null>(null)
const snapshotPreview = ref<{
  dimensionCount: number
  gradeBandCount: number
  ruleCount: number
} | null>(null)

function formatTime(isoStr: string): string {
  if (!isoStr) return ''
  const d = new Date(isoStr)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${mm}-${dd} ${hh}:${mi}`
}

function toggleExpand(ver: ScoringProfileVersion) {
  if (expandedVersion.value === ver.version) {
    expandedVersion.value = null
    snapshotPreview.value = null
    return
  }
  expandedVersion.value = ver.version
  try {
    const snap = JSON.parse(ver.snapshot)
    snapshotPreview.value = {
      dimensionCount: (snap.dimensions || []).length,
      gradeBandCount: (snap.gradeBands || []).length,
      ruleCount: (snap.rules || []).length,
    }
  } catch {
    snapshotPreview.value = null
  }
}

async function handlePublish() {
  publishing.value = true
  try {
    emit('publish', changeSummary.value)
    showPublish.value = false
    changeSummary.value = ''
  } finally {
    publishing.value = false
  }
}
</script>

<style scoped>
.vh-root { padding:16px; border-bottom:1px solid #eef0f3; }
.vh-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; }
.vh-title { display:flex; align-items:center; gap:6px; font-size:13px; font-weight:600; color:#1e2a3a; }
.vh-icon { color:#8c95a3; }
.vh-publish-btn { font-size:11px; padding:4px 10px; background:#1a6dff; color:#fff; border:none; border-radius:6px; cursor:pointer; transition:background 0.15s; }
.vh-publish-btn:hover { background:#1558d6; }

.vh-empty { text-align:center; padding:20px 0; color:#b8c0cc; font-size:12px; }

.vh-list { display:flex; flex-direction:column; gap:6px; max-height:240px; overflow-y:auto; }
.vh-item { padding:8px 10px; border:1px solid #e8ecf0; border-radius:8px; cursor:pointer; transition:all 0.15s; }
.vh-item:hover { border-color:#7aadff; }
.vh-item.active { border-color:#1a6dff; background:#f8faff; }
.vh-item-head { display:flex; align-items:center; justify-content:space-between; }
.vh-ver-badge { font-size:12px; font-weight:600; color:#1a6dff; font-family:monospace; }
.vh-time { font-size:11px; color:#8c95a3; }
.vh-summary { font-size:11px; color:#5a6474; margin-top:4px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }

.vh-snapshot { margin-top:8px; padding-top:8px; border-top:1px dashed #e8ecf0; }
.vh-snap-row { display:flex; justify-content:space-between; font-size:11px; padding:2px 0; }
.vh-snap-label { color:#8c95a3; }
.vh-snap-val { color:#5a6474; font-family:monospace; }

.vh-publish-hint { font-size:12px; color:#8c95a3; }

/* Modal styles (shared) */
.sp-mask { position:fixed; inset:0; z-index:1000; display:flex; align-items:center; justify-content:center; background:rgba(15,23,42,0.4); backdrop-filter:blur(2px); }
.sp-modal { background:#fff; border-radius:14px; box-shadow:0 24px 64px rgba(0,0,0,0.18); overflow:hidden; display:flex; flex-direction:column; }
.sp-modal-head { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0; }
.sp-modal-head h3 { font-size:16px; font-weight:600; color:#1e2a3a; margin:0; }
.sp-modal-close { background:none; border:none; font-size:22px; color:#b8c0cc; cursor:pointer; }
.sp-modal-close:hover { color:#5a6474; }
.sp-modal-body { display:flex; flex-direction:column; gap:16px; padding:20px 24px; }
.sp-modal-foot { display:flex; justify-content:flex-end; gap:10px; padding:0 24px 20px; }

.sp-modal-enter-active { transition:all 0.2s ease-out; }
.sp-modal-leave-active { transition:all 0.15s ease-in; }
.sp-modal-enter-from, .sp-modal-leave-to { opacity:0; }

.sp-btn-primary { display:inline-flex; align-items:center; gap:5px; padding:8px 16px; background:#1a6dff; color:#fff; border:none; border-radius:8px; font-size:13px; font-weight:500; cursor:pointer; }
.sp-btn-primary:hover { background:#1558d6; }
.sp-btn-primary:disabled { background:#b3d1ff; cursor:not-allowed; }
.sp-btn-ghost { padding:8px 16px; background:none; border:1px solid #dce1e8; border-radius:8px; font-size:13px; color:#5a6474; cursor:pointer; }
.sp-btn-ghost:hover { background:#f4f6f9; }

.sp-fld label { display:block; font-size:12px; font-weight:500; color:#5a6474; margin-bottom:5px; }
.sp-fld textarea { width:100%; border:1px solid #dce1e8; border-radius:8px; padding:8px 12px; font-size:13px; outline:none; color:#1e2a3a; background:#fff; resize:vertical; }
.sp-fld textarea:focus { border-color:#7aadff; box-shadow:0 0 0 3px rgba(26,109,255,0.08); }
</style>
