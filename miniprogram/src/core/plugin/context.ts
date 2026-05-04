import type { PlatformCapability } from '../platform/capability'
import type { PluginEventBus } from './event-bus'

export interface UserInfo {
  id: number
  username: string
  name: string
  avatar?: string
  roles: readonly string[]
}

export interface PluginContext {
  readonly tenantPlugins: readonly string[]
  readonly permissions: readonly string[]
  readonly user: UserInfo
  readonly capability: PlatformCapability
  readonly bus: PluginEventBus
}

export function hasPerm(ctx: PluginContext, code: string): boolean {
  return ctx.permissions.includes(code)
}
