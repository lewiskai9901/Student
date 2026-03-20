/**
 * useConditionLogicState - Condition logic state composable
 *
 * Extracted from TaskExecutionView.vue. Manages the condition logic engine
 * that controls item visibility, required state, disabled state, and score overrides.
 */
import { computed, watch, type Ref } from 'vue'
import { evaluateAllConditions } from '@/utils/insp/conditionLogicEngine'
import type { ItemConditionState } from '@/types/insp/conditionLogic'
import { DEFAULT_CONDITION_STATE } from '@/types/insp/conditionLogic'
import type { SubmissionDetail } from '@/types/insp/project'
import type { SectionCondition } from '@/stores/insp/inspExecutionStore'

export function useConditionLogicState(
  details: Ref<SubmissionDetail[]>,
  sectionConditions: Ref<SectionCondition[]>,
  _remarkInputs?: Ref<Record<number, string>>,
) {
  // Build detail -> sectionId map
  const detailSectionMap = computed(() => {
    const map = new Map<string, number | string>()
    for (const d of details.value) {
      if (d.sectionName) {
        map.set(d.itemCode, d.sectionName)
      }
    }
    return map
  })

  // Condition states computed with topological sort
  const conditionStates = computed<Map<string, ItemConditionState>>(() => {
    if (details.value.length === 0) return new Map()
    return evaluateAllConditions(
      details.value.map(d => ({
        itemCode: d.itemCode,
        conditionLogic: d.conditionLogic,
        responseValue: d.responseValue,
      })),
      sectionConditions.value.length > 0 ? sectionConditions.value : undefined,
      sectionConditions.value.length > 0 ? detailSectionMap.value : undefined,
    )
  })

  function getConditionState(detail: SubmissionDetail): ItemConditionState {
    return conditionStates.value.get(detail.itemCode) ?? { ...DEFAULT_CONDITION_STATE }
  }

  function isItemVisible(detail: SubmissionDetail): boolean {
    return getConditionState(detail).visible
  }

  function isItemDisabled(detail: SubmissionDetail): boolean {
    return getConditionState(detail).disabled
  }

  function isItemConditionallyRequired(detail: SubmissionDetail): boolean {
    return getConditionState(detail).required
  }

  function getScoreOverride(detail: SubmissionDetail): number | null {
    return getConditionState(detail).scoreOverride
  }

  // clearValue: when condition triggers, clear the response value
  watch(conditionStates, (states) => {
    for (const [code, state] of states) {
      if (state.clearValue) {
        const detail = details.value.find(d => d.itemCode === code)
        if (detail && detail.responseValue) {
          detail.responseValue = null
        }
      }
    }
  })

  // Section visibility - always visible (section-level conditionLogic removed)
  function isSectionVisible(_sectionName: string): boolean {
    return true
  }

  return {
    conditionStates,
    getConditionState,
    isItemVisible,
    isItemDisabled,
    isItemConditionallyRequired,
    getScoreOverride,
    isSectionVisible,
  }
}
