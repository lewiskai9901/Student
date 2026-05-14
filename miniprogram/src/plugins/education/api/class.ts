import type { LongId } from '@core/types'
import { requestWrapped } from '@core/api/request'
import type { SchoolClass, PageResult } from './types'

export const classApi = {
  list: (params: { keyword?: string; orgUnitId?: LongId; pageNum?: number; pageSize?: number } = {}) => {
    const qs = new URLSearchParams()
    if (params.keyword) qs.set('keyword', params.keyword)
    if (params.orgUnitId != null) qs.set('orgUnitId', String(params.orgUnitId))
    qs.set('pageNum', String(params.pageNum ?? 1))
    qs.set('pageSize', String(params.pageSize ?? 20))
    return requestWrapped<PageResult<SchoolClass>>({
      url: `/user_student/classes?${qs.toString()}`
    })
  },
  byId: (id: LongId) =>
    requestWrapped<SchoolClass>({ url: `/user_student/classes/${id}` })
}
