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
  targetMode: 'BY_SUBJECT' | 'BY_RELATION' | 'BY_ROLE' | 'BY_FEATURE'
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
  BY_SUBJECT:  { label: '事件主体本人', description: '当事件主体是用户时，通知该用户本人' },
  BY_RELATION: { label: '关系导航', description: '基于 access_relations 查询主体的关联人员（如"通知班级的管理员"）' },
  BY_ROLE:     { label: '按角色', description: '通知拥有指定角色/权限的所有用户' },
  BY_FEATURE:  { label: '按能力', description: '通知具备指定能力（feature）的所有用户（如所有 isLearner）' },
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
