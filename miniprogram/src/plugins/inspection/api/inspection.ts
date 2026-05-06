import { requestWrapped } from '@core/api/request'
import type { InspTask, CorrectiveCase, InspAppeal } from './types'

export const inspectionApi = {
  // ===== Reads =====
  myTasks: () => requestWrapped<InspTask[]>({ url: '/inspection/tasks/my-tasks' }),
  availableTasks: () => requestWrapped<InspTask[]>({ url: '/inspection/tasks/available' }),
  taskById: (id: number) => requestWrapped<InspTask>({ url: `/inspection/tasks/${id}` }),
  myCases: () => requestWrapped<CorrectiveCase[]>({ url: '/inspection/corrective-cases/my-cases' }),
  caseById: (id: number) => requestWrapped<CorrectiveCase>({ url: `/inspection/corrective-cases/${id}` }),
  myAppeals: () => requestWrapped<InspAppeal[]>({ url: '/inspection/appeals/my' }),
  appealById: (id: number) => requestWrapped<InspAppeal>({ url: `/inspection/appeals/${id}` }),

  // ===== Writes (Phase D-2) =====
  claimTask: (id: number, inspectorName: string) =>
    requestWrapped<InspTask>({
      url: `/inspection/tasks/${id}/claim`,
      method: 'POST',
      data: { inspectorName }
    }),
  startTask: (id: number) =>
    requestWrapped<InspTask>({ url: `/inspection/tasks/${id}/start`, method: 'POST' }),
  submitTask: (id: number) =>
    requestWrapped<InspTask>({ url: `/inspection/tasks/${id}/submit`, method: 'POST' }),
  startCaseWork: (id: number) =>
    requestWrapped<CorrectiveCase>({
      url: `/inspection/corrective-cases/${id}/start-work`,
      method: 'POST'
    }),
  submitCorrection: (id: number, correctionNote: string) =>
    requestWrapped<CorrectiveCase>({
      url: `/inspection/corrective-cases/${id}/submit-correction`,
      method: 'POST',
      data: { correctionNote, evidenceIds: [] }
    })
}
