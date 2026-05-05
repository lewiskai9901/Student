import type { Contribution } from './contribution'
import type { PluginContext } from './context'

export interface SubPackageConfig {
  root: string
  pages: readonly string[]
}

export interface PluginPackage {
  readonly key: string
  readonly label: string
  readonly schemaVersion: 1
  readonly minCoreVersion: string
  readonly contributions: readonly Contribution[]
  readonly enabled?: (ctx: PluginContext) => boolean
  readonly bootstrap?: (ctx: PluginContext) => void | Promise<void>
  readonly subPackage?: SubPackageConfig
}

export function definePlugin(pkg: PluginPackage): PluginPackage {
  if (!pkg.key || pkg.key.trim() === '') {
    throw new Error('PluginPackage.key is required')
  }
  if (pkg.schemaVersion !== 1) {
    throw new Error(`Unsupported schemaVersion: ${pkg.schemaVersion}`)
  }
  if (!/^\d+\.\d+\.\d+$/.test(pkg.minCoreVersion)) {
    throw new Error(`minCoreVersion must be semver: ${pkg.minCoreVersion}`)
  }
  return Object.freeze({ ...pkg, contributions: Object.freeze([...pkg.contributions]) })
}
