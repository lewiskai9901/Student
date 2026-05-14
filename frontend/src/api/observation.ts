import type { LongId } from '@/types/common'
import { http } from '@/utils/request'
import type { ObservationPageResult } from '@/types/observation'

export interface ObservationListParams {
  projectId?: LongId
  subjectType?: string
  severity?: string
  isNegative?: boolean
  linkedEventTypeCode?: string
  page?: number
  size?: number
}

export const observationApi = {
  list(params?: ObservationListParams): Promise<ObservationPageResult> {
    return http.get('/inspection/observations', { params })
  },

  bySubmission(submissionId: LongId) {
    return http.get(`/inspection/observations/by-submission/${submissionId}`)
  },
}
