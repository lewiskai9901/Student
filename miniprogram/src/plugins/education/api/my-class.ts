import { requestWrapped } from '@core/api/request'
import type {
  MyClass,
  MyClassOverview,
  MyClassStudent,
  DormitoryDistribution
} from './types'

/**
 * 我的班级 API (班主任视角)
 * 后端: MyClassController @ /my-class/*
 * 注意: 学生子路径用的是 /user_student (后端实际路径,非 /students)
 */
export const myClassApi = {
  list: () =>
    requestWrapped<MyClass[]>({ url: '/my-class/classes' }),

  overview: (classId: number) =>
    requestWrapped<MyClassOverview>({ url: `/my-class/classes/${classId}/overview` }),

  students: (classId: number, params: { keyword?: string; status?: string } = {}) => {
    const qs = new URLSearchParams()
    if (params.keyword) qs.set('keyword', params.keyword)
    if (params.status) qs.set('status', params.status)
    const q = qs.toString()
    return requestWrapped<MyClassStudent[]>({
      url: `/my-class/classes/${classId}/user_student${q ? '?' + q : ''}`
    })
  },

  dormitoryDistribution: (classId: number) =>
    requestWrapped<DormitoryDistribution[]>({
      url: `/my-class/classes/${classId}/dormitory-distribution`
    })
}
