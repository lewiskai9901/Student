import { requestWrapped } from '@core/api/request'
import type { Student, PageResult } from './types'

export const studentApi = {
  list: (params: { keyword?: string; orgUnitId?: number; pageNum?: number; pageSize?: number } = {}) => {
    const qs = new URLSearchParams()
    if (params.keyword) qs.set('keyword', params.keyword)
    if (params.orgUnitId != null) qs.set('orgUnitId', String(params.orgUnitId))
    qs.set('pageNum', String(params.pageNum ?? 1))
    qs.set('pageSize', String(params.pageSize ?? 20))
    return requestWrapped<PageResult<Student>>({ url: `/user_student?${qs.toString()}` })
  },
  byId: (id: number) =>
    requestWrapped<Student>({ url: `/user_student/${id}` }),
  byClass: (classId: number) =>
    requestWrapped<Student[]>({ url: `/user_student/by-class/${classId}` })
}
