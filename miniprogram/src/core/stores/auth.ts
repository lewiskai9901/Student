import { defineStore } from 'pinia'
import { authApi, type LoginResp } from '../api/auth'
import { capability } from '../platform/auto'
import type { UserInfo } from '../plugin/context'

interface State {
  user: UserInfo | null
  permissions: string[]
  enabledPlugins: string[]
}

export const useAuth = defineStore('auth', {
  state: (): State => ({
    user: capability.storage.get<UserInfo>('user') ?? null,
    permissions: capability.storage.get<string[]>('permissions') ?? [],
    enabledPlugins: capability.storage.get<string[]>('enabledPlugins') ?? []
  }),
  getters: {
    loggedIn: (s) => s.user !== null,
    hasPerm: (s) => (code: string) => s.permissions.includes(code)
  },
  actions: {
    async login(username: string, password: string): Promise<LoginResp> {
      const r = await authApi.login(username, password)
      capability.storage.set('accessToken', r.accessToken)
      capability.storage.set('refreshToken', r.refreshToken)
      capability.storage.set('user', r.user)
      capability.storage.set('permissions', r.permissions)
      capability.storage.set('enabledPlugins', r.enabledPlugins)
      this.user = r.user
      this.permissions = r.permissions
      this.enabledPlugins = r.enabledPlugins
      return r
    },
    logout() {
      capability.storage.remove('accessToken')
      capability.storage.remove('refreshToken')
      capability.storage.remove('user')
      capability.storage.remove('permissions')
      capability.storage.remove('enabledPlugins')
      this.user = null
      this.permissions = []
      this.enabledPlugins = []
    }
  }
})
