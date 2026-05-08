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
  // Enable gate priority:
  //   1. If manifest declares `enabled(ctx)`, that function is the sole gate (it can read ctx.tenantPlugins itself).
  //   2. Otherwise default to "plugin key included in tenantPlugins".
  // This lets core/通用 plugins (inspection) ship `enabled: () => true` while industry plugins (EDU)
  // still gate on backend's enabledPlugins. Plugin keys (lowercase) and backend industry codes
  // (often UPPERCASE like 'EDU') don't need to match — the manifest's enabled function handles the mapping.
  const active = all.filter(p =>
    p.enabled ? p.enabled(ctx) : input.tenantPlugins.includes(p.key)
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
