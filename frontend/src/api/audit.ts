import { http } from '@/utils/request'

export const auditApi = {
  list: (params: any) => http.get('/audit-trail', { params }),
  getByResource: (resourceType: string, resourceId: LongId) =>
    http.get('/audit-trail/by-resource', { params: { resourceType, resourceId } }),
}
