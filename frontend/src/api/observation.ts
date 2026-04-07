import { http } from '@/utils/request'
import type { ObservationPageResult } from '@/types/observation'

export interface ObservationListParams {
  projectId?: number
  subjectType?: string
  severity?: string
  isNegative?: boolean
  linkedEventTypeCode?: string
  page?: number
  size?: number
}

export const observationApi = {
  list(params?: ObservationListParams): Promise<ObservationPageResult> {
    return http.get('/v7/insp/observations', { params })
  },

  bySubmission(submissionId: number) {
    return http.get(`/v7/insp/observations/by-submission/${submissionId}`)
  },
}
