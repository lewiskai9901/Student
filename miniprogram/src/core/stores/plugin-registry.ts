import { defineStore } from 'pinia'
import { dispatcher } from '../plugin/dispatcher'
import { capability } from '../platform/auto'
import { createEventBus, type PluginEventBus } from '../plugin/event-bus'
import type { PluginContext, UserInfo } from '../plugin/context'
import type { PluginPackage } from '../plugin/package'
import type { ContributionDispatcher } from '../plugin/dispatcher'

const sharedBus = createEventBus()

export interface ActivateInput {
  user: UserInfo
  permissions: string[]
  tenantPlugins: string[]
  capability?: PluginContext['capability']
  bus?: PluginEventBus
}

export async function activatePlugins(d: ContributionDispatcher, input: ActivateInput): Promise<PluginPackage[]> {
  const ctx: PluginContext = {
    user: input.user,
    permissions: input.permissions,
    tenantPlugins: input.tenantPlugins,
    capability: input.capability ?? capability,
    bus: input.bus ?? sharedBus
  }
  const all = d.allPlugins()
  const active = all.filter(p =>
    input.tenantPlugins.includes(p.key) && (p.enabled?.(ctx) ?? true)
  )
  for (const p of active) await p.bootstrap?.(ctx)
  return active
}

export const usePluginRegistry = defineStore('plugin-registry', {
  state: () => ({
    activePlugins: [] as PluginPackage[],
    bus: sharedBus
  }),
  getters: {
    activePluginKeys: (s) => s.activePlugins.map(p => p.key)
  },
  actions: {
    async activate(input: ActivateInput) {
      this.activePlugins = await activatePlugins(dispatcher, input)
    },
    deactivate() {
      this.activePlugins = []
    }
  }
})
