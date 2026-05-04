import type { Contribution, ContributionType } from './contribution'
import type { PluginPackage } from './package'
import { ContributionRegistry } from './registry'

function compareSemver(a: string, b: string): number {
  const pa = a.split('.').map(Number), pb = b.split('.').map(Number)
  for (let i = 0; i < 3; i++) if (pa[i] !== pb[i]) return pa[i] - pb[i]
  return 0
}

export class ContributionDispatcher {
  private readonly registries = new Map<ContributionType, ContributionRegistry<Contribution>>()
  private readonly plugins = new Map<string, PluginPackage>()
  private readonly menuKeys = new Set<string>()
  private readonly routePaths = new Set<string>()
  private readonly scanPrefixes = new Set<string>()

  constructor(private readonly coreVersion: string) {}

  register(plugin: PluginPackage): void {
    if (this.plugins.has(plugin.key)) {
      throw new Error(`Plugin "${plugin.key}" already registered`)
    }
    if (compareSemver(this.coreVersion, plugin.minCoreVersion) < 0) {
      throw new Error(`Plugin "${plugin.key}" requires minCoreVersion ${plugin.minCoreVersion}, current core is ${this.coreVersion}`)
    }
    for (const c of plugin.contributions) this.detectConflicts(plugin.key, c)
    for (const c of plugin.contributions) this.getRegistry(c.type).add(plugin.key, c)
    this.plugins.set(plugin.key, plugin)
  }

  private detectConflicts(pluginKey: string, c: Contribution) {
    if (c.type === 'menu') {
      if (this.menuKeys.has(c.key)) throw new Error(`Duplicate menu key "${c.key}" from plugin "${pluginKey}"`)
      this.menuKeys.add(c.key)
    } else if (c.type === 'route') {
      if (this.routePaths.has(c.path)) throw new Error(`Duplicate route path "${c.path}" from plugin "${pluginKey}"`)
      this.routePaths.add(c.path)
    } else if (c.type === 'scan-resolver') {
      if (this.scanPrefixes.has(c.prefix)) throw new Error(`Duplicate scan-resolver prefix "${c.prefix}" from plugin "${pluginKey}"`)
      this.scanPrefixes.add(c.prefix)
    }
  }

  private getRegistry(type: ContributionType): ContributionRegistry<Contribution> {
    let r = this.registries.get(type)
    if (!r) this.registries.set(type, r = new ContributionRegistry<Contribution>())
    return r
  }

  query<T extends Contribution>(type: T['type']): T[] {
    return this.getRegistry(type).all() as T[]
  }

  queryFiltered<T extends Contribution>(type: T['type'], pred: (c: T) => boolean): T[] {
    return this.query<T>(type).filter(pred)
  }

  allPlugins(): PluginPackage[] {
    return Array.from(this.plugins.values())
  }

  getPlugin(key: string): PluginPackage | undefined {
    return this.plugins.get(key)
  }
}

export const CORE_VERSION = '0.1.0'
export const dispatcher = new ContributionDispatcher(CORE_VERSION)
