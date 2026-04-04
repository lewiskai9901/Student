import { http } from '@/utils/request'
import type { LoginRequest, LoginResponse, UserInfo } from '@/types/auth'

export const authApi = {
  login(data: LoginRequest) {
    return http.post<LoginResponse>('/auth/login', data)
  },

  refresh(refreshToken: string) {
    return http.post<LoginResponse>('/auth/refresh', { refreshToken })
  },

  logout(refreshToken?: string) {
    return http.post<void>('/auth/logout', { refreshToken })
  },

  getMe() {
    return http.get<UserInfo>('/auth/me')
  },
}
