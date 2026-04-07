export interface MsgNotification {
  id: number
  userId: number
  title: string
  content: string | null
  msgType: 'SYSTEM' | 'EVENT' | 'EVALUATION' | 'MANUAL'
  sourceEventType: string | null
  sourceRefType: string | null
  sourceRefId: number | null
  subjectType: string | null
  subjectId: number | null
  subjectName: string | null
  eventCategory: string | null
  sourceModule: string | null
  eventId: number | null
  isRead: boolean
  readAt: string | null
  createdAt: string
}

export interface MsgSubscriptionRule {
  id: number
  ruleName: string
  eventCategory: string | null
  eventType: string | null
  targetMode: 'BY_ROLE' | 'BY_ORG_ADMIN' | 'BY_USER' | 'BY_RELATED'
  targetConfig: string | null
  channel: 'IN_APP' | 'EMAIL' | 'WECHAT'
  templateId: number | null
  isEnabled: boolean
}

export interface MsgTemplate {
  id: number
  templateCode: string
  templateName: string
  titleTemplate: string
  contentTemplate: string | null
  isSystem: boolean
  isEnabled: boolean
}

export const TargetModeConfig: Record<MsgSubscriptionRule['targetMode'], { label: string; description: string }> = {
  BY_ROLE: { label: '按角色', description: '通知指定角色的所有用户' },
  BY_ORG_ADMIN: { label: '组织管理员', description: '通知相关组织的管理员' },
  BY_USER: { label: '指定用户', description: '直接通知指定用户' },
  BY_RELATED: { label: '关联人员', description: '通知事件关联的人员' },
}

export const ChannelConfig: Record<MsgSubscriptionRule['channel'], { label: string }> = {
  IN_APP: { label: '站内消息' },
  EMAIL: { label: '邮件' },
  WECHAT: { label: '微信' },
}

export const MsgTypeConfig: Record<MsgNotification['msgType'], { label: string }> = {
  SYSTEM: { label: '系统' },
  EVENT: { label: '事件' },
  EVALUATION: { label: '测评' },
  MANUAL: { label: '手动' },
}
