import { requestWrapped } from '@core/api/request'
import type { InspTask, CorrectiveCase, InspAppeal } from './types'

export const inspectionApi = {
  myTasks: () => requestWrapped<InspTask[]>({ url: '/inspection/tasks/my-tasks' }),
  availableTasks: () => requestWrapped<InspTask[]>({ url: '/inspection/tasks/available' }),
  taskById: (id: number) => requestWrapped<InspTask>({ url: `/inspection/tasks/${id}` }),
  myCases: () => requestWrapped<CorrectiveCase[]>({ url: '/inspection/corrective-cases/my-cases' }),
  caseById: (id: number) => requestWrapped<CorrectiveCase>({ url: `/inspection/corrective-cases/${id}` }),
  myAppeals: () => requestWrapped<InspAppeal[]>({ url: '/inspection/appeals/my' }),
  appealById: (id: number) => requestWrapped<InspAppeal>({ url: `/inspection/appeals/${id}` })
}
