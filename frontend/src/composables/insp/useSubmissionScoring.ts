/**
 * useSubmissionScoring - Scoring logic composable for task execution
 *
 * Extracted from TaskExecutionView.vue. Handles all 13 scoring modes,
 * score summary computation, quick actions, validation, display helpers.
 */
import { ref, computed, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { SubmissionDetail } from '@/types/insp/project'

// ==================== Types ====================

export interface ScoreSummaryData {
  total: number
  scored: number
  passCount: number
  failCount: number
  flagged: number
  baseScore: number
  deductions: number
  bonuses: number
  finalScore: number
}

interface ScoringStore {
  updateDetailResponse: (detailId: number, data: any) => Promise<any>
  updateDetailRemark: (detailId: number, remark: string) => Promise<any>
  flagDetail: (detailId: number, data: { reason: string }) => Promise<any>
  unflagDetail: (detailId: number) => Promise<any>
}

// ==================== Constants ====================

export const MODE_LABEL: Record<string, string> = {
  PASS_FAIL: '通过/不通过',
  DEDUCTION: '扣分',
  ADDITION: '加分',
  DIRECT: '直接打分',
  LEVEL: '等级评分',
  SCORE_TABLE: '评分标准',
  CUMULATIVE: '累计计次',
  TIERED_DEDUCTION: '分档扣分',
  RATING_SCALE: '评级量表',
  WEIGHTED_MULTI: '多维加权',
  RISK_MATRIX: '风险矩阵',
  THRESHOLD: '阈值判定',
}

export const MODE_TAG_TYPE: Record<string, string> = {
  PASS_FAIL: '',
  DEDUCTION: 'danger',
  ADDITION: 'success',
  DIRECT: 'warning',
  LEVEL: 'warning',
  SCORE_TABLE: 'warning',
  CUMULATIVE: 'danger',
  TIERED_DEDUCTION: 'danger',
  RATING_SCALE: 'warning',
  WEIGHTED_MULTI: 'primary',
  RISK_MATRIX: 'danger',
  THRESHOLD: 'primary',
}

// ==================== Composable ====================

export function useSubmissionScoring(
  details: Ref<SubmissionDetail[]>,
  store: ScoringStore,
  getScoreOverrideFn: (detail: SubmissionDetail) => number | null,
  isItemDisabledFn: (detail: SubmissionDetail) => boolean,
  isItemVisibleFn: (detail: SubmissionDetail) => boolean,
  baseScoreRef?: Ref<number>,
) {
  // ==================== State ====================

  const numberInputs = ref<Record<number, number>>({})
  const remarkInputs = ref<Record<number, string>>({})
  const selectInputs = ref<Record<number, string>>({})
  const multiInputs = ref<Record<number, Record<string, number>>>({})
  const scoringInProgress = ref(false)

  // ==================== Mode Resolution ====================

  function isNonScoring(d: SubmissionDetail): boolean {
    return !d.scoringMode
  }

  function resolveMode(d: SubmissionDetail): string {
    if (d.scoringMode) return d.scoringMode
    return 'INPUT_ONLY'
  }

  function getScoringParams(detail: SubmissionDetail) {
    let config: Record<string, any> = {}
    try { if (detail.scoringConfig) config = JSON.parse(detail.scoringConfig) } catch { /* ignore */ }
    const mode = resolveMode(detail)
    switch (mode) {
      case 'PASS_FAIL':
        return { passScore: config.passScore ?? 0, failScore: config.failScore ?? -2 }
      case 'DEDUCTION':
        return { maxDeduction: config.maxDeduction ?? -10, step: config.deductionStep ?? 1 }
      case 'ADDITION':
        return { maxBonus: config.maxBonus ?? 5, step: config.bonusStep ?? 1 }
      case 'DIRECT':
        return { minScore: config.minScore ?? 0, maxScore: config.maxScore ?? 10 }
      case 'LEVEL':
        return { levels: config.levels ?? [] }
      case 'SCORE_TABLE':
        return { options: config.options ?? [] }
      case 'CUMULATIVE':
        return { label: config.label ?? '次数', scorePerCount: config.scorePerCount ?? -2, maxCount: config.maxCount ?? 10 }
      case 'TIERED_DEDUCTION':
        return { tiers: config.tiers ?? [] }
      case 'RATING_SCALE':
        return { maxStars: config.maxStars ?? 5, scorePerStar: config.scorePerStar ?? 2 }
      case 'WEIGHTED_MULTI':
        return { maxScore: config.maxScore ?? 10, dimensions: config.dimensions ?? [] }
      case 'RISK_MATRIX':
        return { probabilities: config.probabilities ?? [], impacts: config.impacts ?? [] }
      case 'THRESHOLD':
        return { unit: config.unit ?? '', thresholds: config.thresholds ?? [] }
      case 'FORMULA':
        return { formulaType: config.formulaType ?? 'ratio', inputs: config.inputs ?? [], maxScore: config.maxScore ?? 10, minScore: config.minScore ?? 0 }
      default:
        return {}
    }
  }

  function getMaxScoreForMode(detail: SubmissionDetail): number {
    const params = getScoringParams(detail)
    switch (resolveMode(detail)) {
      case 'DIRECT': return (params as any).maxScore ?? 10
      case 'LEVEL': {
        const scores = ((params as any).levels || []).map((l: any) => l.score)
        return scores.length > 0 ? Math.max(...scores) : 10
      }
      case 'SCORE_TABLE': {
        const scores = ((params as any).options || []).map((o: any) => o.score)
        return scores.length > 0 ? Math.max(...scores) : 10
      }
      case 'RATING_SCALE': return ((params as any).maxStars ?? 5) * ((params as any).scorePerStar ?? 2)
      case 'WEIGHTED_MULTI': return (params as any).maxScore ?? 10
      case 'THRESHOLD': {
        const scores = ((params as any).thresholds || []).map((t: any) => t.score)
        return scores.length > 0 ? Math.max(...scores) : 10
      }
      case 'FORMULA': return (params as any).maxScore ?? 10
      default: return 10
    }
  }

  // ==================== Filtered Details ====================

  const visibleDetails = computed(() =>
    details.value.filter(d => isItemVisibleFn(d)),
  )

  const scoreableDetails = computed(() =>
    visibleDetails.value.filter(d => !isItemDisabledFn(d) && !isNonScoring(d)),
  )

  // ==================== Score Summary ====================

  const scoreSummary = computed<ScoreSummaryData>(() => {
    const all = scoreableDetails.value
    const total = all.length
    const scored = all.filter(d => d.responseValue != null).length
    const flagged = all.filter(d => d.isFlagged).length

    let deductions = 0
    let bonuses = 0
    let passCount = 0
    let failCount = 0

    for (const d of all) {
      if (d.responseValue == null) continue
      const override = getScoreOverrideFn(d)
      const s = override !== null ? override : (d.score ?? 0)
      if (isNaN(s) || !isFinite(s)) continue
      const mode = resolveMode(d)

      switch (mode) {
        case 'PASS_FAIL':
          if (d.responseValue === 'PASS') {
            passCount++
            if (s > 0) bonuses += s
          } else {
            failCount++
            deductions += Math.abs(s)
          }
          break
        case 'DEDUCTION':
        case 'TIERED_DEDUCTION':
          deductions += Math.abs(s)
          break
        case 'ADDITION':
          bonuses += s
          break
        case 'CUMULATIVE': {
          const cp = getScoringParams(d) as { scorePerCount: number }
          if ((cp.scorePerCount ?? -2) < 0) deductions += Math.abs(s)
          else bonuses += s
          break
        }
        case 'RISK_MATRIX':
          deductions += Math.abs(s)
          break
        case 'DIRECT':
        case 'LEVEL':
        case 'SCORE_TABLE':
        case 'RATING_SCALE':
        case 'WEIGHTED_MULTI':
        case 'THRESHOLD':
        case 'FORMULA': {
          const maxS = getMaxScoreForMode(d)
          deductions += Math.max(0, maxS - s)
          break
        }
      }
    }

    const baseScore = baseScoreRef?.value ?? 100
    const finalScore = Math.max(0, baseScore - deductions + bonuses)
    return { total, scored, passCount, failCount, flagged, baseScore, deductions, bonuses, finalScore }
  })

  // ==================== Local State Helper ====================

  function updateLocal(detailId: number, patch: Partial<SubmissionDetail>) {
    const idx = details.value.findIndex(d => d.id === detailId)
    if (idx !== -1) details.value[idx] = { ...details.value[idx], ...patch }
  }

  // ==================== Scoring Handlers ====================

  async function handlePassFail(detail: SubmissionDetail, value: 'PASS' | 'FAIL') {
    if (scoringInProgress.value) return
    scoringInProgress.value = true
    try {
      const params = getScoringParams(detail) as { passScore: number; failScore: number }
      const score = value === 'PASS' ? params.passScore : params.failScore
      await store.updateDetailResponse(detail.id, {
        responseValue: value, scoringMode: 'PASS_FAIL', score,
      })
      updateLocal(detail.id, { responseValue: value, score, scoringMode: 'PASS_FAIL' })
    } catch (e: any) {
      ElMessage.error(e.message || '打分失败')
    } finally {
      scoringInProgress.value = false
    }
  }

  async function handleDeduction(detail: SubmissionDetail) {
    if (scoringInProgress.value) return
    scoringInProgress.value = true
    try {
      let score = Math.min(0, numberInputs.value[detail.id] ?? 0)
      if (isNaN(score) || !isFinite(score)) score = 0
      await store.updateDetailResponse(detail.id, {
        responseValue: String(score), scoringMode: 'DEDUCTION', score,
      })
      updateLocal(detail.id, { responseValue: String(score), score, scoringMode: 'DEDUCTION' })
    } catch (e: any) {
      ElMessage.error(e.message || '打分失败')
    } finally {
      scoringInProgress.value = false
    }
  }

  async function handleAddition(detail: SubmissionDetail) {
    if (scoringInProgress.value) return
    scoringInProgress.value = true
    try {
      let score = Math.max(0, numberInputs.value[detail.id] ?? 0)
      if (isNaN(score) || !isFinite(score)) score = 0
      await store.updateDetailResponse(detail.id, {
        responseValue: String(score), scoringMode: 'ADDITION', score,
      })
      updateLocal(detail.id, { responseValue: String(score), score, scoringMode: 'ADDITION' })
    } catch (e: any) {
      ElMessage.error(e.message || '打分失败')
    } finally {
      scoringInProgress.value = false
    }
  }

  async function handleDirect(detail: SubmissionDetail) {
    if (scoringInProgress.value) return
    scoringInProgress.value = true
    try {
      const params = getScoringParams(detail) as { minScore: number; maxScore: number }
      let score = Math.max(params.minScore, Math.min(params.maxScore, numberInputs.value[detail.id] ?? params.maxScore))
      if (isNaN(score) || !isFinite(score)) score = 0
      await store.updateDetailResponse(detail.id, {
        responseValue: String(score), scoringMode: 'DIRECT', score,
      })
      updateLocal(detail.id, { responseValue: String(score), score, scoringMode: 'DIRECT' })
    } catch (e: any) {
      ElMessage.error(e.message || '打分失败')
    } finally {
      scoringInProgress.value = false
    }
  }

  async function handleLevel(detail: SubmissionDetail, label: string, score: number) {
    if (scoringInProgress.value) return
    scoringInProgress.value = true
    try {
      await store.updateDetailResponse(detail.id, {
        responseValue: label, scoringMode: 'LEVEL', score,
      })
      updateLocal(detail.id, { responseValue: label, score, scoringMode: 'LEVEL' })
      selectInputs.value[detail.id] = label
    } catch (e: any) {
      ElMessage.error(e.message || '打分失败')
    } finally {
      scoringInProgress.value = false
    }
  }

  async function handleScoreTable(detail: SubmissionDetail, label: string, score: number) {
    if (scoringInProgress.value) return
    scoringInProgress.value = true
    try {
      await store.updateDetailResponse(detail.id, {
        responseValue: label, scoringMode: 'SCORE_TABLE', score,
      })
      updateLocal(detail.id, { responseValue: label, score, scoringMode: 'SCORE_TABLE' })
      selectInputs.value[detail.id] = label
    } catch (e: any) {
      ElMessage.error(e.message || '打分失败')
    } finally {
      scoringInProgress.value = false
    }
  }

  async function handleCumulative(detail: SubmissionDetail) {
    if (scoringInProgress.value) return
    scoringInProgress.value = true
    try {
      const params = getScoringParams(detail) as { scorePerCount: number; maxCount: number }
      const count = Math.max(0, Math.min(params.maxCount ?? 10, numberInputs.value[detail.id] ?? 0))
      let score = count * (params.scorePerCount ?? -2)
      if (isNaN(score) || !isFinite(score)) score = 0
      await store.updateDetailResponse(detail.id, {
        responseValue: String(count), scoringMode: 'CUMULATIVE', score,
      })
      updateLocal(detail.id, { responseValue: String(count), score, scoringMode: 'CUMULATIVE' })
    } catch (e: any) {
      ElMessage.error(e.message || '打分失败')
    } finally {
      scoringInProgress.value = false
    }
  }

  async function handleTieredDeduction(detail: SubmissionDetail, label: string, score: number) {
    if (scoringInProgress.value) return
    scoringInProgress.value = true
    try {
      await store.updateDetailResponse(detail.id, {
        responseValue: label, scoringMode: 'TIERED_DEDUCTION', score,
      })
      updateLocal(detail.id, { responseValue: label, score, scoringMode: 'TIERED_DEDUCTION' })
      selectInputs.value[detail.id] = label
    } catch (e: any) {
      ElMessage.error(e.message || '打分失败')
    } finally {
      scoringInProgress.value = false
    }
  }

  async function handleRatingScale(detail: SubmissionDetail, stars: number) {
    if (scoringInProgress.value) return
    scoringInProgress.value = true
    try {
      const params = getScoringParams(detail) as { scorePerStar: number }
      let score = stars * (params.scorePerStar ?? 2)
      if (isNaN(score) || !isFinite(score)) score = 0
      await store.updateDetailResponse(detail.id, {
        responseValue: String(stars), scoringMode: 'RATING_SCALE', score,
      })
      updateLocal(detail.id, { responseValue: String(stars), score, scoringMode: 'RATING_SCALE' })
      numberInputs.value[detail.id] = stars
    } catch (e: any) {
      ElMessage.error(e.message || '打分失败')
    } finally {
      scoringInProgress.value = false
    }
  }

  async function handleWeightedMulti(detail: SubmissionDetail) {
    if (scoringInProgress.value) return
    scoringInProgress.value = true
    try {
      const params = getScoringParams(detail) as { maxScore: number; dimensions: any[] }
      const dims = params.dimensions || []
      const vals = multiInputs.value[detail.id] || {}
      let weightedSum = 0
      for (const dim of dims) {
        const v = vals[dim.key] ?? 0
        weightedSum += (v / (dim.maxScore || 10)) * (dim.weight || 0)
      }
      const maxS = params.maxScore ?? 10
      let score = Math.round(Math.max(0, Math.min(maxS, (weightedSum / 100) * maxS)) * 100) / 100
      if (isNaN(score) || !isFinite(score)) score = 0
      await store.updateDetailResponse(detail.id, {
        responseValue: JSON.stringify(vals), scoringMode: 'WEIGHTED_MULTI', score,
      })
      updateLocal(detail.id, { responseValue: JSON.stringify(vals), score, scoringMode: 'WEIGHTED_MULTI' })
    } catch (e: any) {
      ElMessage.error(e.message || '打分失败')
    } finally {
      scoringInProgress.value = false
    }
  }

  async function handleRiskMatrix(detail: SubmissionDetail) {
    if (scoringInProgress.value) return
    scoringInProgress.value = true
    try {
      const vals = multiInputs.value[detail.id] || {}
      const p = vals.probability ?? 1
      const i = vals.impact ?? 1
      let score = -(p * i)
      if (isNaN(score) || !isFinite(score)) score = 0
      await store.updateDetailResponse(detail.id, {
        responseValue: JSON.stringify({ probability: p, impact: i }), scoringMode: 'RISK_MATRIX', score,
      })
      updateLocal(detail.id, { responseValue: JSON.stringify({ probability: p, impact: i }), score, scoringMode: 'RISK_MATRIX' })
    } catch (e: any) {
      ElMessage.error(e.message || '打分失败')
    } finally {
      scoringInProgress.value = false
    }
  }

  async function handleThreshold(detail: SubmissionDetail) {
    if (scoringInProgress.value) return
    scoringInProgress.value = true
    try {
      const params = getScoringParams(detail) as { thresholds: any[] }
      const val = numberInputs.value[detail.id] ?? 0
      const sorted = [...(params.thresholds || [])].sort((a, b) => {
        if (a.max == null) return 1
        if (b.max == null) return -1
        return a.max - b.max
      })
      let score = 0
      let label = ''
      for (const th of sorted) {
        if (th.max == null || val <= th.max) {
          score = th.score ?? 0
          label = th.label ?? ''
          break
        }
      }
      if (!label && sorted.length > 0) {
        const last = sorted[sorted.length - 1]
        score = last.score ?? 0
        label = last.label ?? ''
      }
      if (isNaN(score) || !isFinite(score)) score = 0
      await store.updateDetailResponse(detail.id, {
        responseValue: String(val), scoringMode: 'THRESHOLD', score,
      })
      updateLocal(detail.id, { responseValue: String(val), score, scoringMode: 'THRESHOLD' })
      selectInputs.value[detail.id] = label
    } catch (e: any) {
      ElMessage.error(e.message || '打分失败')
    } finally {
      scoringInProgress.value = false
    }
  }

  async function handleFormula(detail: SubmissionDetail) {
    if (scoringInProgress.value) return
    scoringInProgress.value = true
    try {
      const params = getScoringParams(detail) as { formulaType: string; inputs: any[]; maxScore: number; minScore: number }
      const vals = multiInputs.value[detail.id] || {}
      let score = 0
      const maxS = params.maxScore ?? 10
      const minS = params.minScore ?? 0
      switch (params.formulaType) {
        case 'ratio': {
          const div = vals.standard || 1
          score = div !== 0 ? (vals.actual ?? 0) / div * maxS : 0
          break
        }
        case 'difference':
          score = maxS - Math.abs((vals.actual ?? 0) - (vals.standard ?? 0))
          break
        case 'percentage': {
          const div = vals.total || 1
          score = div !== 0 ? (vals.actual ?? 0) / div * maxS : 0
          break
        }
        case 'compliance_rate': {
          const div = vals.total || 1
          score = div !== 0 ? (vals.compliant ?? 0) / div * maxS : 0
          break
        }
      }
      if (isNaN(score) || !isFinite(score)) score = 0
      score = Math.round(Math.max(minS, Math.min(maxS, score)) * 100) / 100
      await store.updateDetailResponse(detail.id, {
        responseValue: JSON.stringify(vals), scoringMode: 'FORMULA', score,
      })
      updateLocal(detail.id, { responseValue: JSON.stringify(vals), score, scoringMode: 'FORMULA' })
    } catch (e: any) {
      ElMessage.error(e.message || '打分失败')
    } finally {
      scoringInProgress.value = false
    }
  }

  // ==================== Flag Toggle ====================

  async function toggleFlag(detail: SubmissionDetail) {
    try {
      if (detail.isFlagged) {
        await store.unflagDetail(detail.id)
        updateLocal(detail.id, { isFlagged: false })
      } else {
        await store.flagDetail(detail.id, { reason: 'Inspector flagged' })
        updateLocal(detail.id, { isFlagged: true })
      }
    } catch (e: any) {
      ElMessage.error(e.message || '标记失败')
    }
  }

  // ==================== Quick Actions ====================

  async function markAllPass() {
    const items = details.value.filter(d => resolveMode(d) === 'PASS_FAIL' && d.responseValue == null)
    for (const d of items) await handlePassFail(d, 'PASS')
    ElMessage.success(`已将 ${items.length} 项标记为通过`)
  }

  async function markAllDeductionZero() {
    const items = details.value.filter(d => resolveMode(d) === 'DEDUCTION' && d.responseValue == null)
    for (const d of items) {
      numberInputs.value[d.id] = 0
      await handleDeduction(d)
    }
    ElMessage.success(`已将 ${items.length} 项设为不扣分`)
  }

  async function markAllDirectPerfect() {
    const autoFillModes = ['DIRECT', 'LEVEL', 'SCORE_TABLE', 'RATING_SCALE']
    const items = details.value.filter(d => autoFillModes.includes(resolveMode(d)) && d.responseValue == null)
    for (const d of items) {
      const mode = resolveMode(d)
      const params = getScoringParams(d)
      if (mode === 'DIRECT') {
        numberInputs.value[d.id] = (params as any).maxScore ?? 10
        await handleDirect(d)
      } else if (mode === 'LEVEL') {
        const levels = (params as any).levels || []
        if (levels.length > 0) {
          const best = levels.reduce((a: any, b: any) => a.score >= b.score ? a : b)
          await handleLevel(d, best.label, best.score)
        }
      } else if (mode === 'SCORE_TABLE') {
        const opts = (params as any).options || []
        if (opts.length > 0) {
          const best = opts.reduce((a: any, b: any) => a.score >= b.score ? a : b)
          await handleScoreTable(d, best.label, best.score)
        }
      } else if (mode === 'RATING_SCALE') {
        const stars = (params as any).maxStars ?? 5
        await handleRatingScale(d, stars)
      }
    }
    ElMessage.success(`已将 ${items.length} 项设为满分`)
  }

  // ==================== Validation ====================

  function validateDetail(detail: SubmissionDetail): string | null {
    // Check condition-logic 'require' action
    if (getConditionRequiredFn(detail) && !detail.responseValue) {
      return '此项为条件必填'
    }
    if (!detail.validationRules) return null
    try {
      const rules = JSON.parse(detail.validationRules)
      if (!Array.isArray(rules)) return null
      for (const rule of rules) {
        switch (rule.type) {
          case 'required':
            if (!detail.responseValue) return rule.message || '此项必须填写'
            break
          case 'requiredIfFail': {
            const remark = remarkInputs.value[detail.id] || detail.remark
            if (detail.responseValue === 'FAIL' && !remark)
              return rule.message || '不通过时必须填写原因'
            break
          }
          case 'minLength':
            if (detail.responseValue && detail.responseValue.length < (rule.value || 0))
              return rule.message || `最少需要 ${rule.value} 个字符`
            break
          case 'maxLength':
            if (detail.responseValue && detail.responseValue.length > (rule.value || 999))
              return rule.message || `最多允许 ${rule.value} 个字符`
            break
          case 'range': {
            const num = Number(detail.responseValue)
            if (rule.min != null && num < rule.min) return rule.message || `不能小于 ${rule.min}`
            if (rule.max != null && num > rule.max) return rule.message || `不能大于 ${rule.max}`
            break
          }
          case 'minPhotos':
            break
          case 'maxPhotos':
            break
          case 'pattern':
            if (detail.responseValue && rule.value) {
              try {
                const re = new RegExp(rule.value)
                if (!re.test(detail.responseValue)) return rule.message || '格式不正确'
              } catch { /* invalid regex, skip */ }
            }
            break
        }
      }
    } catch { /* ignore parse errors */ }
    return null
  }

  // Internal reference to the condition-required function (passed via isItemConditionallyRequired)
  // We need a separate parameter for this since validateDetail needs it
  let getConditionRequiredFn: (detail: SubmissionDetail) => boolean = () => false
  function setConditionRequiredFn(fn: (detail: SubmissionDetail) => boolean) {
    getConditionRequiredFn = fn
  }

  function needsRemark(detail: SubmissionDetail): boolean {
    if (!detail.validationRules) return false
    try {
      const rules = JSON.parse(detail.validationRules)
      return Array.isArray(rules) && rules.some((r: any) => r.type === 'requiredIfFail')
    } catch { return false }
  }

  async function handleRemarkChange(detail: SubmissionDetail) {
    const remark = remarkInputs.value[detail.id] ?? ''
    try {
      await store.updateDetailRemark(detail.id, remark)
      updateLocal(detail.id, { remark })
    } catch (e: any) {
      ElMessage.error(e.message || '保存备注失败')
    }
  }

  // ==================== Display Helpers ====================

  function formatScore(detail: SubmissionDetail): string {
    if (detail.responseValue == null) return '--'
    const override = getScoreOverrideFn(detail)
    const s = override !== null ? override : (detail.score ?? 0)
    const mode = resolveMode(detail)
    if (mode === 'ADDITION') return s > 0 ? `+${s}` : '0'
    if (mode === 'PASS_FAIL') return String(s)
    const directLike = ['DIRECT', 'LEVEL', 'SCORE_TABLE', 'RATING_SCALE', 'WEIGHTED_MULTI', 'THRESHOLD', 'FORMULA']
    if (directLike.includes(mode)) {
      const maxS = getMaxScoreForMode(detail)
      return `${s}/${maxS}`
    }
    if (['DEDUCTION', 'TIERED_DEDUCTION', 'RISK_MATRIX'].includes(mode)) return String(s)
    if (mode === 'CUMULATIVE') return String(s)
    return String(s)
  }

  function scoreColor(detail: SubmissionDetail): string {
    if (detail.responseValue == null) return '#d1d5db'
    const s = detail.score ?? 0
    const mode = resolveMode(detail)
    if (mode === 'ADDITION') return s > 0 ? '#10b981' : '#6b7280'
    const directLike = ['DIRECT', 'LEVEL', 'SCORE_TABLE', 'RATING_SCALE', 'WEIGHTED_MULTI', 'THRESHOLD', 'FORMULA']
    if (directLike.includes(mode)) {
      const max = getMaxScoreForMode(detail)
      return s >= max * 0.8 ? '#10b981' : s >= max * 0.6 ? '#f59e0b' : '#ef4444'
    }
    if (s < 0) return '#ef4444'
    return '#10b981'
  }

  function rowBg(detail: SubmissionDetail): string {
    if (detail.responseValue == null) return ''
    const mode = resolveMode(detail)
    const s = detail.score ?? 0
    if (mode === 'PASS_FAIL') {
      return detail.responseValue === 'PASS' ? 'bg-green-50/60' : 'bg-red-50/60'
    }
    if (['DEDUCTION', 'TIERED_DEDUCTION', 'RISK_MATRIX'].includes(mode)) {
      return s < 0 ? 'bg-red-50/40' : 'bg-green-50/40'
    }
    if (mode === 'ADDITION') {
      return s > 0 ? 'bg-emerald-50/40' : ''
    }
    if (mode === 'CUMULATIVE') {
      return s < 0 ? 'bg-red-50/40' : s > 0 ? 'bg-emerald-50/40' : 'bg-green-50/40'
    }
    const directLike = ['DIRECT', 'LEVEL', 'SCORE_TABLE', 'RATING_SCALE', 'WEIGHTED_MULTI', 'THRESHOLD', 'FORMULA']
    if (directLike.includes(mode)) {
      const max = getMaxScoreForMode(detail)
      return s >= max * 0.8 ? 'bg-green-50/40' : s >= max * 0.6 ? 'bg-yellow-50/40' : 'bg-red-50/40'
    }
    return ''
  }

  // ==================== Init Inputs ====================

  /**
   * Initialize input states from loaded details.
   * Called after loading submission details.
   */
  function initInputsFromDetails(list: SubmissionDetail[]) {
    const inputs: Record<number, number> = {}
    const remarks: Record<number, string> = {}
    const selects: Record<number, string> = {}
    const multis: Record<number, Record<string, number>> = {}
    for (const d of list) {
      const mode = resolveMode(d)
      const params = getScoringParams(d)
      if (mode === 'DEDUCTION') {
        inputs[d.id] = d.responseValue != null ? (d.score ?? 0) : 0
      } else if (mode === 'ADDITION') {
        inputs[d.id] = d.responseValue != null ? (d.score ?? 0) : 0
      } else if (mode === 'DIRECT') {
        const maxS = (params as any).maxScore ?? 10
        inputs[d.id] = d.responseValue != null ? (d.score ?? maxS) : maxS
      } else if (mode === 'CUMULATIVE') {
        inputs[d.id] = d.responseValue != null ? Number(d.responseValue) : 0
      } else if (mode === 'THRESHOLD') {
        inputs[d.id] = d.responseValue != null ? Number(d.responseValue) : 0
      } else if (mode === 'RATING_SCALE') {
        inputs[d.id] = d.responseValue != null ? Number(d.responseValue) : 0
      } else if (mode === 'LEVEL' || mode === 'SCORE_TABLE' || mode === 'TIERED_DEDUCTION') {
        selects[d.id] = d.responseValue ?? ''
      } else if (mode === 'WEIGHTED_MULTI') {
        const dims = (params as any).dimensions || []
        try {
          multis[d.id] = d.responseValue ? JSON.parse(d.responseValue) : Object.fromEntries(dims.map((dm: any) => [dm.key, 0]))
        } catch { multis[d.id] = Object.fromEntries(dims.map((dm: any) => [dm.key, 0])) }
      } else if (mode === 'RISK_MATRIX') {
        try {
          multis[d.id] = d.responseValue ? JSON.parse(d.responseValue) : { probability: 1, impact: 1 }
        } catch { multis[d.id] = { probability: 1, impact: 1 } }
      } else if (mode === 'FORMULA') {
        const fInputs = (params as any).inputs || []
        try {
          multis[d.id] = d.responseValue ? JSON.parse(d.responseValue) : Object.fromEntries(fInputs.map((fi: any) => [fi.key, 0]))
        } catch { multis[d.id] = Object.fromEntries(fInputs.map((fi: any) => [fi.key, 0])) }
      }
      if (d.remark) remarks[d.id] = d.remark
    }
    numberInputs.value = inputs
    remarkInputs.value = remarks
    selectInputs.value = selects
    multiInputs.value = multis
  }

  return {
    // State
    numberInputs,
    remarkInputs,
    selectInputs,
    multiInputs,
    scoringInProgress,
    // Computed
    visibleDetails,
    scoreableDetails,
    scoreSummary,
    // Mode helpers
    resolveMode,
    getScoringParams,
    getMaxScoreForMode,
    // Scoring handlers
    handlePassFail,
    handleDeduction,
    handleAddition,
    handleDirect,
    handleLevel,
    handleScoreTable,
    handleCumulative,
    handleTieredDeduction,
    handleRatingScale,
    handleWeightedMulti,
    handleRiskMatrix,
    handleThreshold,
    handleFormula,
    // Flag
    toggleFlag,
    // Quick actions
    markAllPass,
    markAllDeductionZero,
    markAllDirectPerfect,
    // Validation & remark
    validateDetail,
    needsRemark,
    handleRemarkChange,
    setConditionRequiredFn,
    // Display helpers
    formatScore,
    scoreColor,
    rowBg,
    // Init
    initInputsFromDetails,
    updateLocal,
    // Non-scoring check
    isNonScoring,
  }
}
