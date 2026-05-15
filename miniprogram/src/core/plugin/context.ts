import type { PlatformCapability } from '../platform/capability'
import type { PluginEventBus } from './event-bus'
import type { LongId } from '../types'

export interface UserInfo {
  id: LongId
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
