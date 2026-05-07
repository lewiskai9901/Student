import { requestWrapped } from './request'

export interface UserListItem {
  id: number
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  gender?: number
  userType?: string
  orgUnitId?: number
  orgUnitName?: string
  status?: string
  roleNames?: string[]
  lastLoginTime?: string
}

export const userApi = {
  byOrgUnit: (orgUnitId: number) =>
    requestWrapped<UserListItem[]>({ url: `/users/by-org-unit/${orgUnitId}` })
}
