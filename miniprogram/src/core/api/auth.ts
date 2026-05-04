import { request } from './request'
import type { UserInfo } from '../plugin/context'

export interface LoginResp {
  accessToken: string
  refreshToken: string
  user: UserInfo
  permissions: string[]
  enabledPlugins: string[]
}

export const authApi = {
  login: (username: string, password: string) =>
    request<LoginResp>({ url: '/auth/login', method: 'POST', data: { username, password }, skipAuth: true })
}
