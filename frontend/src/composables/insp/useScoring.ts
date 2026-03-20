/**
 * V7 检查平台 - 客户端评分预览 Composable
 *
 * 职责：
 * 1. 根据表单填写数据 + 评分配置，实时预览分数
 * 2. 维度分数聚合 (SUM within dimension, WEIGHTED_AVG across dimensions)
 * 3. 归一化计算 (人口归一化等)
 * 4. 分数 → 等级映射
 */
import { ref } from 'vue'
import type { SubmissionDetail } from '@/types/insp/project'
import type {
  ScoringProfile,
  ScoreDimension,
  GradeBand,
} from '@/types/insp/scoring'

interface DimensionResult {
  dimensionCode: string
  dimensionName: string
  rawScore: number
  weight: number
  weightedScore: number
}

interface ScoreResult {
  totalScore: number
  grade: string
  dimensions: DimensionResult[]
}

export function useScoring() {
  const scoreResult = ref<ScoreResult | null>(null)
  const isCalculating = ref(false)

  // ==================== Preview Calculation ====================

  /**
   * Calculate a preview score from current form data.
   * This is a client-side approximation; the server performs the authoritative calculation.
   */
  function calculatePreview(
    details: SubmissionDetail[],
    profile: ScoringProfile,
    dimensions: ScoreDimension[],
    gradeBands: GradeBand[],
  ) {
    isCalculating.value = true
    try {
      // Step 1: Collect per-dimension raw scores from scored details
      const dimensionScoresMap = new Map<string, number[]>()
      for (const dim of dimensions) {
        dimensionScoresMap.set(dim.dimensionCode, [])
      }

      for (const detail of details) {
        if (detail.score == null) continue

        // Determine which dimension this detail belongs to
        const dimCode = extractDimensionCode(detail)
        if (dimCode && dimensionScoresMap.has(dimCode)) {
          dimensionScoresMap.get(dimCode)!.push(detail.score)
        } else if (dimensions.length === 1) {
          // Single dimension: all scores go to the one dimension
          const singleDim = dimensions[0]
          dimensionScoresMap.get(singleDim.dimensionCode)!.push(detail.score)
        }
      }

      // Step 2: Aggregate each dimension using SUM internally
      const dimensionResults: DimensionResult[] = []
      for (const dim of dimensions) {
        const scores = dimensionScoresMap.get(dim.dimensionCode) || []
        const rawScore = scores.length > 0 ? scores.reduce((a, b) => a + b, 0) : 0

        dimensionResults.push({
          dimensionCode: dim.dimensionCode,
          dimensionName: dim.dimensionName,
          rawScore,
          weight: dim.weight,
          weightedScore: rawScore * dim.weight,
        })
      }

      // Step 3: Aggregate across dimensions using WEIGHTED_AVG
      let totalScore = aggregateDimensions(dimensionResults)

      // Step 4: Apply dimension base score offset
      // For deduction-based systems, totalScore is typically negative (deductions)
      // Use the first dimension's baseScore as reference if available
      if (dimensions.length > 0 && dimensions[0].baseScore > 0 && totalScore <= 0) {
        totalScore = dimensions[0].baseScore + totalScore
      }

      // Step 5: Clamp to [minScore, maxScore]
      totalScore = Math.max(profile.minScore, Math.min(profile.maxScore, totalScore))

      // Step 6: Apply precision
      totalScore = roundToPrecision(totalScore, profile.precisionDigits)

      // Step 7: Map to grade
      const grade = mapToGrade(totalScore, gradeBands)

      scoreResult.value = {
        totalScore,
        grade,
        dimensions: dimensionResults,
      }
    } finally {
      isCalculating.value = false
    }
  }

  // ==================== Normalization ====================

  /**
   * Calculate a normalization factor.
   * Matches backend NormalizationMode enum values (PER_CAPITA, SQRT_ADJUSTED, NONE).
   *
   * @param mode - Normalization mode (uppercase enum values from backend)
   * @param population - Population count for per-capita normalization
   * @param baseline - Baseline value for normalization reference
   */
  function calculateNormFactor(mode: string, population: number, baseline: number): number {
    switch (mode) {
      case 'PER_CAPITA':
        return population > 0 ? baseline / population : 1
      case 'SQRT_ADJUSTED':
        return population > 0 ? Math.sqrt(baseline / population) : 1
      case 'NONE':
      default:
        return 1
    }
  }

  // ==================== Dimension Aggregation ====================

  /**
   * Aggregate dimension results into a single total score.
   * Uses WEIGHTED_AVG across dimensions (SUM is used within each dimension).
   *
   * @param dimensionResults - Array of DimensionResult with rawScore and weight
   */
  function aggregateDimensions(
    dimensionResults: DimensionResult[],
  ): number {
    if (dimensionResults.length === 0) return 0

    const totalWeight = dimensionResults.reduce((acc, d) => acc + d.weight, 0)
    if (totalWeight === 0) return 0
    const weightedSum = dimensionResults.reduce((acc, d) => acc + d.rawScore * d.weight, 0)
    return weightedSum / totalWeight
  }

  // ==================== Grade Mapping ====================

  /**
   * Map a numeric score to a grade label using the grade band configuration.
   * Grade bands are checked by [minScore, maxScore] inclusive ranges.
   */
  function mapToGrade(score: number, gradeBands: GradeBand[]): string {
    if (gradeBands.length === 0) return ''

    // Sort by minScore descending to find the highest applicable band first
    const sorted = [...gradeBands].sort((a, b) => b.minScore - a.minScore)
    for (const band of sorted) {
      if (score >= band.minScore && score <= band.maxScore) {
        return band.gradeName
      }
    }

    // Fallback: if score is below all bands, return the lowest grade
    const lowest = sorted[sorted.length - 1]
    if (score < lowest.minScore) return lowest.gradeName

    // If score is above all bands, return the highest grade
    return sorted[0].gradeName
  }

  // ==================== Helpers ====================

  /**
   * Extract dimension code from a SubmissionDetail.
   * The dimensions JSON string on the detail holds the dimension mapping.
   */
  function extractDimensionCode(detail: SubmissionDetail): string | null {
    if (!detail.dimensions) return null
    try {
      const parsed = JSON.parse(detail.dimensions)
      // Expected format: { "dimensionCode": "D01" } or just a string
      if (typeof parsed === 'string') return parsed
      if (parsed.dimensionCode) return parsed.dimensionCode
      return null
    } catch {
      return null
    }
  }

  function roundToPrecision(value: number, digits: number): number {
    const factor = Math.pow(10, digits)
    return Math.round(value * factor) / factor
  }

  return {
    scoreResult,
    isCalculating,
    calculatePreview,
    calculateNormFactor,
    aggregateDimensions,
    mapToGrade,
  }
}
