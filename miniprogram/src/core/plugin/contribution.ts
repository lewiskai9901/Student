import type { PluginContext } from './context'

export type ContributionType =
  | 'menu' | 'route' | 'permission' | 'message-handler'
  | 'scan-resolver' | 'subscribe-template' | 'config-schema' | 'event'

export interface BizMessage {
  id: string
  category: string
  title: string
  body: string
  data?: Record<string, unknown>
  createdAt: string
  read: boolean
}

export interface MessageRendered {
  icon: string
  iconColor?: string
  title: string
  summary: string
  navigateTo?: string
}

export interface ScanResult { code: string; rawType: string }
export interface ScanAction { path: string; params?: Record<string, string> }

export interface MenuContribution {
  type: 'menu'
  key: string
  label: string
  icon: string
  path: string
  perm?: string
  order: number
  group?: 'home-grid' | 'mine-extra'
  badge?: (ctx: PluginContext) => Promise<number>
}

export interface RouteContribution {
  type: 'route'
  path: string
  perm?: string
  meta?: { title?: string; navStyle?: 'custom' | 'default' }
  inSubPackage: true
}

export interface PermissionContribution {
  type: 'permission'
  code: string
  description: string
}

export interface MessageHandlerContribution {
  type: 'message-handler'
  category: string
  render: (msg: BizMessage) => MessageRendered
}

export interface ScanResolverContribution {
  type: 'scan-resolver'
  prefix: string
  resolve: (raw: string) => ScanAction | null
  priority: number
}

export interface SubscribeMessageContribution {
  type: 'subscribe-template'
  templateId: string
  scenario: string
  description: string
}

export type JsonSchema = { type: string; [k: string]: unknown }

export interface ConfigSchemaContribution {
  type: 'config-schema'
  schema: JsonSchema
}

export interface EventContribution {
  type: 'event'
  eventName: string
  payloadSchema: JsonSchema
}

export type Contribution =
  | MenuContribution
  | RouteContribution
  | PermissionContribution
  | MessageHandlerContribution
  | ScanResolverContribution
  | SubscribeMessageContribution
  | ConfigSchemaContribution
  | EventContribution
