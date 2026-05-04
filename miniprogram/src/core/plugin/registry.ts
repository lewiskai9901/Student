import type { Contribution } from './contribution'

export class ContributionRegistry<T extends Contribution> {
  private items: Array<{ pluginKey: string; contrib: T }> = []
  add(pluginKey: string, contrib: T) { this.items.push({ pluginKey, contrib }) }
  all(): T[] { return this.items.map(i => i.contrib) }
  byPlugin(pluginKey: string): T[] {
    return this.items.filter(i => i.pluginKey === pluginKey).map(i => i.contrib)
  }
}
