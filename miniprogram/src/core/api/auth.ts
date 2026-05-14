import type { LongId } from '@core/types'
import { requestWrapped } from './request'
import type { UserInfo } from '../plugin/context'

// Backend LoginResponse.UserInfo subset — fields the miniprogram UI consumes.
export interface LoginUserInfo {
  userId: LongId
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  status?: number
  roles: string[]
  permissions: string[]
  orgUnitId?: LongId
  tenantId?: LongId
  userTypeCode?: string
}

export interface LoginResp {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: LoginUserInfo
  enabledPlugins: string[]
}

export const authApi = {
  login: (username: string, password: string) =>
    requestWrapped<LoginResp>({
      url: '/auth/login',
      method: 'POST',
      data: { username, password },
      skipAuth: true
    }),
  me: () => requestWrapped<LoginUserInfo>({ url: '/auth/me' })
}

// Helper: flatten the rich LoginUserInfo into the narrow PluginContext.UserInfo
// shape (id/username/name/avatar/roles) used by the plugin SPI.
export function toUserInfo(u: LoginUserInfo): UserInfo {
  return {
    id: u.userId,
    username: u.username,
    name: u.realName,
    avatar: u.avatar,
    roles: u.roles ?? []
  }
}
