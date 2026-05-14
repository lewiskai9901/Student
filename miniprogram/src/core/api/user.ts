import type { LongId } from '@core/types'
import { requestWrapped } from './request'

export interface UserListItem {
  id: LongId
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  gender?: number
  userType?: string
  orgUnitId?: LongId
  orgUnitName?: string
  status?: string
  roleNames?: string[]
  lastLoginTime?: string
}

export const userApi = {
  byOrgUnit: (orgUnitId: LongId) =>
    requestWrapped<UserListItem[]>({ url: `/users/by-org-unit/${orgUnitId}` })
}
