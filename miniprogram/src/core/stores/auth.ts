import { defineStore } from 'pinia'
import { authApi, toUserInfo } from '../api/auth'
import { capability } from '../platform/auto'
import type { UserInfo } from '../plugin/context'

interface State {
  user: UserInfo | null
  permissions: string[]
  enabledPlugins: string[]
  tenantId: number | null
}

export const useAuth = defineStore('auth', {
  state: (): State => ({
    user: capability.storage.get<UserInfo>('user') ?? null,
    permissions: capability.storage.get<string[]>('permissions') ?? [],
    enabledPlugins: capability.storage.get<string[]>('enabledPlugins') ?? [],
    tenantId: capability.storage.get<number>('tenantId') ?? null
  }),
  getters: {
    loggedIn: (s) => s.user !== null,
    hasPerm: (s) => (code: string) => s.permissions.includes(code)
  },
  actions: {
    async login(username: string, password: string) {
      const r = await authApi.login(username, password)
      const u = toUserInfo(r.userInfo)
      const perms = r.userInfo.permissions ?? []
      const plugins = r.enabledPlugins ?? []
      const tenantId = r.userInfo.tenantId ?? null

      capability.storage.set('accessToken', r.accessToken)
      capability.storage.set('refreshToken', r.refreshToken)
      capability.storage.set('user', u)
      capability.storage.set('permissions', perms)
      capability.storage.set('enabledPlugins', plugins)
      capability.storage.set('tenantId', tenantId)

      this.user = u
      this.permissions = perms
      this.enabledPlugins = plugins
      this.tenantId = tenantId
      return r
    },
    logout() {
      capability.storage.remove('accessToken')
      capability.storage.remove('refreshToken')
      capability.storage.remove('user')
      capability.storage.remove('permissions')
      capability.storage.remove('enabledPlugins')
      capability.storage.remove('tenantId')
      this.user = null
      this.permissions = []
      this.enabledPlugins = []
      this.tenantId = null
    }
  }
})
