<template>
  <div class="adv-root">
    <div class="adv-top">
      <h3 class="adv-title">高级评分设置</h3>
      <button v-if="isDirty" class="adv-save" @click="handleSave">保存</button>
    </div>

    <!-- 1. 进退步奖惩 -->
    <div class="adv-group" :class="{ on: form.trendFactorEnabled }">
      <div class="adv-header" @click="toggleSection('trend')">
        <label class="adv-toggle" @click.stop>
          <input v-model="form.trendFactorEnabled" type="checkbox" @change="isDirty = true" />
          <span class="adv-slider"></span>
        </label>
        <div class="adv-header-text">
          <span class="adv-name">进退步奖惩</span>
          <span class="adv-desc">跟上次比，进步加分、退步扣分</span>
        </div>
        <ChevronDown :size="14" class="adv-arrow" :class="{ flip: expanded.trend }" />
      </div>
      <div v-if="expanded.trend" class="adv-body">
        <template v-if="form.trendFactorEnabled">
          <p class="adv-sentence">
            对比
            <input v-model.number="form.trendLookbackDays" type="number" class="adv-inline" min="1" max="90" @input="isDirty = true" />
            天前的分数，每进步1%加
            <input v-model.number="form.trendBonusPerPercent" type="number" class="adv-inline" step="0.1" @input="isDirty = true" />
            分，每退步1%扣
            <input v-model.number="form.trendPenaltyPerPercent" type="number" class="adv-inline" step="0.1" @input="isDirty = true" />
            分，最多调整
            <input v-model.number="form.trendMaxAdjustment" type="number" class="adv-inline" step="0.5" @input="isDirty = true" />
            分。
          </p>
          <div class="adv-eg">
            <b>例：</b>上次80分 → 这次85分，进步5% → 加 {{ (5 * (form.trendBonusPerPercent ?? 0.5)).toFixed(1) }} 分 = 最终 {{ (85 + 5 * (form.trendBonusPerPercent ?? 0.5)).toFixed(1) }} 分
          </div>
        </template>
        <p v-else class="adv-off-hint">开启后，系统会自动对比上次检查分数，进步奖励、退步惩罚。</p>
      </div>
    </div>

    <!-- 2. 时效衰减 -->
    <div class="adv-group" :class="{ on: form.decayEnabled }">
      <div class="adv-header" @click="toggleSection('decay')">
        <label class="adv-toggle" @click.stop>
          <input v-model="form.decayEnabled" type="checkbox" @change="isDirty = true" />
          <span class="adv-slider"></span>
        </label>
        <div class="adv-header-text">
          <span class="adv-name">时效衰减</span>
          <span class="adv-desc">不检查分数就逐天降低，督促定期检查</span>
        </div>
        <ChevronDown :size="14" class="adv-arrow" :class="{ flip: expanded.decay }" />
      </div>
      <div v-if="expanded.decay" class="adv-body">
        <template v-if="form.decayEnabled">
          <p class="adv-sentence">
            检查完成后，
            <select v-model="form.decayMode" class="adv-inline-sel" @change="isDirty = true">
              <option value="LINEAR">每天减</option>
              <option value="EXPONENTIAL">每天衰减</option>
            </select>
            <input v-model.number="form.decayRatePerDay" type="number" class="adv-inline" step="0.01" @input="isDirty = true" />
            {{ form.decayMode === 'LINEAR' ? '分' : '%' }}，最低不低于
            <input v-model.number="form.decayFloor" type="number" class="adv-inline" step="1" @input="isDirty = true" />
            分。
          </p>
          <div class="adv-eg">
            <b>例：</b>周一得95分 → 周五(4天后) =
            <template v-if="form.decayMode === 'LINEAR'">
              {{ Math.max(95 - 4 * (form.decayRatePerDay ?? 0.5), form.decayFloor ?? 60).toFixed(1) }} 分
            </template>
            <template v-else>
              {{ Math.max(95 * Math.pow(1 - (form.decayRatePerDay ?? 0.5) / 100, 4), form.decayFloor ?? 60).toFixed(1) }} 分
            </template>
          </div>
        </template>
        <p v-else class="adv-off-hint">开启后，检查得分会随时间自动降低，促使定期复查保持标准。</p>
      </div>
    </div>

    <!-- 3. 多人评分合并 -->
    <div class="adv-group" :class="{ on: form.multiRaterMode && form.multiRaterMode !== 'LATEST' }">
      <div class="adv-header" @click="toggleSection('rater')">
        <label class="adv-toggle" @click.stop>
          <input :checked="form.multiRaterMode !== 'LATEST'" type="checkbox" @change="form.multiRaterMode = ($event.target as HTMLInputElement).checked ? 'AVERAGE' : 'LATEST'; isDirty = true" />
          <span class="adv-slider"></span>
        </label>
        <div class="adv-header-text">
          <span class="adv-name">多人评分合并</span>
          <span class="adv-desc">多个检查员同时打分时如何取最终分</span>
        </div>
        <ChevronDown :size="14" class="adv-arrow" :class="{ flip: expanded.rater }" />
      </div>
      <div v-if="expanded.rater" class="adv-body">
        <template v-if="form.multiRaterMode !== 'LATEST'">
          <p class="adv-sentence">
            多人打分时，
            <select v-model="form.multiRaterMode" class="adv-inline-sel wide" @change="isDirty = true">
              <option value="AVERAGE">取平均分</option>
              <option value="WEIGHTED_AVERAGE">按权重加权平均</option>
              <option value="MEDIAN">取中间值（忽略极端）</option>
              <option value="MAX">取最高分</option>
              <option value="MIN">取最低分</option>
              <option value="CONSENSUS">检测打分分歧</option>
            </select>
            <template v-if="form.multiRaterMode === 'WEIGHTED_AVERAGE'">
              ，权重
              <select v-model="form.raterWeightBy" class="adv-inline-sel" @change="isDirty = true">
                <option value="EQUAL">相同</option>
                <option value="BY_ROLE">按角色</option>
                <option value="BY_EXPERIENCE">按经验</option>
                <option value="CUSTOM">手动指定</option>
              </select>
            </template>
            <template v-if="form.multiRaterMode === 'CONSENSUS'">
              ，偏差超过满分的
              <input v-model.number="form.consensusThreshold" type="number" class="adv-inline sm" min="0" max="1" step="0.05" @input="isDirty = true" />
              ({{ ((form.consensusThreshold ?? 0.1) * 100).toFixed(0) }}%) 标记需复核
            </template>
            。
          </p>
          <div class="adv-eg">
            <b>例：</b>3人打分 85、90、80 →
            <template v-if="form.multiRaterMode === 'AVERAGE'">平均 = 85 分</template>
            <template v-else-if="form.multiRaterMode === 'WEIGHTED_AVERAGE'">主管(权重2)打90 + 普通(权重1)打80 = 86.7 分</template>
            <template v-else-if="form.multiRaterMode === 'MEDIAN'">取中间值 = 85 分</template>
            <template v-else-if="form.multiRaterMode === 'MAX'">取最高 = 90 分</template>
            <template v-else-if="form.multiRaterMode === 'MIN'">取最低 = 80 分</template>
            <template v-else-if="form.multiRaterMode === 'CONSENSUS'">差距10分，超过阈值则标记"需复核"</template>
          </div>
        </template>
        <p v-else class="adv-off-hint">默认以最后一次提交为准。开启后可以对多人打分做平均、取中间值等合并处理。</p>
      </div>
    </div>

    <!-- 4. 评分尺度校准 -->
    <div class="adv-group" :class="{ on: form.calibrationEnabled }">
      <div class="adv-header" @click="toggleSection('cal')">
        <label class="adv-toggle" @click.stop>
          <input v-model="form.calibrationEnabled" type="checkbox" @change="isDirty = true" />
          <span class="adv-slider"></span>
        </label>
        <div class="adv-header-text">
          <span class="adv-name">评分尺度校准</span>
          <span class="adv-desc">有人打分松、有人紧？自动拉到同一水平</span>
        </div>
        <ChevronDown :size="14" class="adv-arrow" :class="{ flip: expanded.cal }" />
      </div>
      <div v-if="expanded.cal" class="adv-body">
        <template v-if="form.calibrationEnabled">
          <p class="adv-sentence">
            用
            <select v-model="form.calibrationMethod" class="adv-inline-sel wide" @change="isDirty = true">
              <option value="Z_SCORE">标准分（推荐）</option>
              <option value="MIN_MAX">最高最低拉伸</option>
              <option value="PERCENTILE_RANK">排名百分位</option>
              <option value="IRT">专业统计模型</option>
            </select>
            方法，采集最近
            <input v-model.number="form.calibrationPeriodDays" type="number" class="adv-inline" min="7" max="365" @input="isDirty = true" />
            天数据，至少
            <input v-model.number="form.calibrationMinSamples" type="number" class="adv-inline sm" min="1" @input="isDirty = true" />
            条记录才生效。
          </p>
          <div class="adv-eg">
            <b>例：</b>张老师平均打72分，他给A栋80(算高分) / 李老师平均打91分，她给B栋88(算低分) → 校准后 A栋 > B栋
          </div>
        </template>
        <p v-else class="adv-off-hint">多个检查员各管一片、打分松紧不一时开启，系统根据历史打分习惯自动校准，让排名更公平。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ChevronDown } from 'lucide-vue-next'
import type { ScoringProfile, UpdateAdvancedSettingsRequest } from '@/types/insp/scoring'

const props = defineProps<{
  profile: ScoringProfile
}>()

const emit = defineEmits<{
  save: [data: UpdateAdvancedSettingsRequest]
}>()

const isDirty = ref(false)
const expanded = reactive({ trend: false, decay: false, rater: false, cal: false })

const form = reactive({
  trendFactorEnabled: false,
  trendLookbackDays: 7,
  trendBonusPerPercent: 0.5,
  trendPenaltyPerPercent: 0.3,
  trendMaxAdjustment: 10,
  decayEnabled: false,
  decayMode: 'LINEAR',
  decayRatePerDay: 0.5,
  decayFloor: 60,
  multiRaterMode: 'LATEST',
  raterWeightBy: 'EQUAL',
  consensusThreshold: 0.1,
  calibrationEnabled: false,
  calibrationMethod: 'Z_SCORE',
  calibrationPeriodDays: 30,
  calibrationMinSamples: 10,
})

function syncFromProfile(p: ScoringProfile) {
  form.trendFactorEnabled = p.trendFactorEnabled ?? false
  form.trendLookbackDays = p.trendLookbackDays ?? 7
  form.trendBonusPerPercent = p.trendBonusPerPercent ?? 0.5
  form.trendPenaltyPerPercent = p.trendPenaltyPerPercent ?? 0.3
  form.trendMaxAdjustment = p.trendMaxAdjustment ?? 10
  form.decayEnabled = p.decayEnabled ?? false
  form.decayMode = p.decayMode ?? 'LINEAR'
  form.decayRatePerDay = p.decayRatePerDay ?? 0.5
  form.decayFloor = p.decayFloor ?? 60
  form.multiRaterMode = p.multiRaterMode ?? 'LATEST'
  form.raterWeightBy = p.raterWeightBy ?? 'EQUAL'
  form.consensusThreshold = p.consensusThreshold ?? 0.1
  form.calibrationEnabled = p.calibrationEnabled ?? false
  form.calibrationMethod = p.calibrationMethod ?? 'Z_SCORE'
  form.calibrationPeriodDays = p.calibrationPeriodDays ?? 30
  form.calibrationMinSamples = p.calibrationMinSamples ?? 10
  isDirty.value = false
}

watch(() => props.profile, (p) => { if (p) syncFromProfile(p) }, { immediate: true })

function toggleSection(key: 'trend' | 'decay' | 'rater' | 'cal') {
  expanded[key] = !expanded[key]
}

function handleSave() {
  emit('save', { ...form })
  isDirty.value = false
}
</script>

<style scoped>
.adv-root { display:flex; flex-direction:column; gap:10px; }

.adv-top { display:flex; align-items:center; justify-content:space-between; }
.adv-title { font-size:14px; font-weight:600; color:#1e2a3a; margin:0; }
.adv-save { padding:6px 12px; background:#1a6dff; color:#fff; border:none; border-radius:6px; font-size:12px; font-weight:500; cursor:pointer; transition:background 0.15s; }
.adv-save:hover { background:#1558d6; }

/* ---- Group card ---- */
.adv-group { border:1px solid #e8ecf0; border-radius:10px; overflow:hidden; transition:border-color 0.2s; }
.adv-group.on { border-color:#c6daff; }

/* ---- Header ---- */
.adv-header { display:flex; align-items:center; gap:10px; padding:10px 14px; cursor:pointer; transition:background 0.15s; }
.adv-header:hover { background:#f8f9fb; }
.adv-header-text { flex:1; min-width:0; }
.adv-name { font-size:13px; font-weight:600; color:#1e2a3a; display:block; }
.adv-desc { font-size:11px; color:#8c95a3; display:block; margin-top:1px; }
.adv-arrow { color:#b8c0cc; flex-shrink:0; transition:transform 0.2s; }
.adv-arrow.flip { transform:rotate(180deg); }

/* ---- Toggle switch ---- */
.adv-toggle { position:relative; display:inline-block; width:32px; height:18px; flex-shrink:0; }
.adv-toggle input { opacity:0; width:0; height:0; position:absolute; }
.adv-slider {
  position:absolute; inset:0; background:#dce1e8; border-radius:18px;
  cursor:pointer; transition:background 0.2s;
}
.adv-slider::before {
  content:''; position:absolute; left:2px; top:2px; width:14px; height:14px;
  background:#fff; border-radius:50%; transition:transform 0.2s;
}
.adv-toggle input:checked + .adv-slider { background:#1a6dff; }
.adv-toggle input:checked + .adv-slider::before { transform:translateX(14px); }

/* ---- Body ---- */
.adv-body { padding:2px 14px 14px; border-top:1px solid #f0f2f5; }

/* ---- Sentence with inline inputs ---- */
.adv-sentence {
  font-size:13px; color:#3a4555; line-height:2.2; margin:8px 0 0;
}
.adv-inline {
  display:inline-block; width:52px; border:1px solid #dce1e8; border-radius:6px;
  padding:3px 6px; font-size:13px; text-align:center; outline:none;
  color:#1a6dff; font-weight:600; background:#f8faff;
  transition:border-color 0.2s, box-shadow 0.2s;
  margin:0 2px; vertical-align:baseline;
}
.adv-inline.sm { width:44px; }
.adv-inline:focus { border-color:#7aadff; box-shadow:0 0 0 2px rgba(26,109,255,0.1); }
.adv-inline-sel {
  display:inline-block; border:1px solid #dce1e8; border-radius:6px;
  padding:3px 8px; font-size:13px; outline:none;
  color:#1a6dff; font-weight:500; background:#f8faff;
  cursor:pointer; transition:border-color 0.2s;
  margin:0 2px; vertical-align:baseline;
}
.adv-inline-sel.wide { min-width:130px; }
.adv-inline-sel:focus { border-color:#7aadff; box-shadow:0 0 0 2px rgba(26,109,255,0.1); }

/* ---- Example ---- */
.adv-eg {
  margin-top:8px; background:#f8faff; border-left:3px solid #7aadff;
  border-radius:0 6px 6px 0; padding:6px 10px;
  font-size:12px; color:#5a6474; line-height:1.6;
}
.adv-eg b { color:#1a6dff; font-weight:600; }

/* ---- Off hint ---- */
.adv-off-hint {
  margin:8px 0 0; font-size:12px; color:#a0a8b4; line-height:1.6;
}
</style>
