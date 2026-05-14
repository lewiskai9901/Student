import type { LongId } from '@core/types'
import { requestWrapped } from '@core/api/request'
import type {
  InspTask, CorrectiveCase, InspAppeal, InspEvidence, EvidenceType,
  SubmissionDetail, InspSubmission, ScoringMode
} from './types'

export const inspectionApi = {
  // ===== Reads =====
  myTasks: () => requestWrapped<InspTask[]>({ url: '/inspection/tasks/my-tasks' }),
  availableTasks: () => requestWrapped<InspTask[]>({ url: '/inspection/tasks/available' }),
  taskById: (id: LongId) => requestWrapped<InspTask>({ url: `/inspection/tasks/${id}` }),
  myCases: () => requestWrapped<CorrectiveCase[]>({ url: '/inspection/corrective-cases/my-cases' }),
  caseById: (id: LongId) => requestWrapped<CorrectiveCase>({ url: `/inspection/corrective-cases/${id}` }),
  myAppeals: () => requestWrapped<InspAppeal[]>({ url: '/inspection/appeals/my' }),
  appealById: (id: LongId) => requestWrapped<InspAppeal>({ url: `/inspection/appeals/${id}` }),

  // ===== Writes (Phase D-2) =====
  claimTask: (id: LongId, inspectorName: string) =>
    requestWrapped<InspTask>({
      url: `/inspection/tasks/${id}/claim`,
      method: 'POST',
      data: { inspectorName }
    }),
  startTask: (id: LongId) =>
    requestWrapped<InspTask>({ url: `/inspection/tasks/${id}/start`, method: 'POST' }),
  submitTask: (id: LongId) =>
    requestWrapped<InspTask>({ url: `/inspection/tasks/${id}/submit`, method: 'POST' }),
  startCaseWork: (id: LongId) =>
    requestWrapped<CorrectiveCase>({
      url: `/inspection/corrective-cases/${id}/start-work`,
      method: 'POST'
    }),
  submitCorrection: (id: LongId, correctionNote: string, evidenceIds: number[] = []) =>
    requestWrapped<CorrectiveCase>({
      url: `/inspection/corrective-cases/${id}/submit-correction`,
      method: 'POST',
      data: { correctionNote, evidenceIds }
    }),
  addEvidence: (
    submissionId: LongId,
    body: { detailId?: LongId; evidenceType: EvidenceType; fileName: string; fileUrl: string }
  ) => requestWrapped<InspEvidence>({
    url: `/inspection/submissions/${submissionId}/evidences`,
    method: 'POST',
    data: body
  }),

  // === D-3b-1 read ===
  submissionsByTask: (taskId: LongId) =>
    requestWrapped<InspSubmission[]>({ url: `/inspection/submissions?taskId=${taskId}` }),
  submissionDetails: (submissionId: LongId) =>
    requestWrapped<SubmissionDetail[]>({ url: `/inspection/submissions/${submissionId}/details` }),

  // === D-3b-1 write ===
  updateDetailResponse: (
    detailId: LongId,
    body: { responseValue?: string; scoringMode?: ScoringMode; score?: number; dimensions?: string }
  ) => requestWrapped<SubmissionDetail>({
    url: `/inspection/submissions/details/${detailId}/response`,
    method: 'PUT',
    data: body
  }),
  completeSubmission: (submissionId: LongId) =>
    requestWrapped<InspSubmission>({
      url: `/inspection/submissions/${submissionId}/complete`,
      method: 'POST'
    }),

  // === D-3b-2 write ===
  submitAppeal: (body: {
    submissionDetailId: LongId
    submitterName: string
    reason: string
    attachments?: string
  }) => requestWrapped<InspAppeal>({
    url: '/inspection/appeals',
    method: 'POST',
    data: body
  })
}
