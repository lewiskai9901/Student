/**
 * V62 检查平台 - 枚举定义
 */

// ==================== 模板状态 ====================

export type TemplateStatus = 'DRAFT' | 'PUBLISHED' | 'DEPRECATED' | 'ARCHIVED'

export const TemplateStatusConfig: Record<TemplateStatus, { label: string; type: string; color: string }> = {
  DRAFT: { label: '草稿', type: 'info', color: '#909399' },
  PUBLISHED: { label: '已发布', type: 'success', color: '#67C23A' },
  DEPRECATED: { label: '已废弃', type: 'warning', color: '#E6A23C' },
  ARCHIVED: { label: '已归档', type: 'danger', color: '#F56C6C' },
}

// ==================== 采集项类型（即 ItemType）====================
//
// 检查项分两类：
//   - 评分项：isScored=true，类型由 scoringConfig.mode（ScoringMode）决定
//   - 采集项：isScored=false，类型由 itemType（ItemType）决定
//
// ItemType 只包含数据采集类型，不包含评分相关类型。
// 评分相关概念（通过/不通过、等级、扣分等）全部在 ScoringMode 中定义。

export type ItemType =
  | 'TEXT' | 'TEXTAREA' | 'RICH_TEXT'
  | 'NUMBER' | 'SLIDER'
  | 'SELECT' | 'MULTI_SELECT' | 'CHECKBOX' | 'RADIO'
  | 'DATE' | 'TIME' | 'DATETIME'
  | 'PHOTO' | 'VIDEO' | 'SIGNATURE' | 'FILE_UPLOAD'
  | 'GPS' | 'BARCODE'
  | 'PERSON_SCORE' | 'VIOLATION_RECORD'

export interface ItemTypeInfo {
  label: string
  icon: string
  group: string
  description: string
}

export const ItemTypeConfig: Record<ItemType, ItemTypeInfo> = {
  TEXT: { label: '单行文本', icon: 'Type', group: '文本', description: '单行文本输入' },
  TEXTAREA: { label: '多行文本', icon: 'AlignLeft', group: '文本', description: '多行文本输入' },
  RICH_TEXT: { label: '富文本', icon: 'FileText', group: '文本', description: '富文本编辑器' },
  NUMBER: { label: '数字', icon: 'Hash', group: '数字', description: '数字输入' },
  SLIDER: { label: '滑块', icon: 'SlidersHorizontal', group: '数字', description: '滑块选择' },
  SELECT: { label: '单选下拉', icon: 'ChevronDown', group: '选择', description: '下拉单选' },
  MULTI_SELECT: { label: '多选下拉', icon: 'ListChecks', group: '选择', description: '下拉多选' },
  CHECKBOX: { label: '复选框', icon: 'CheckSquare', group: '选择', description: '复选框组' },
  RADIO: { label: '单选按钮', icon: 'Circle', group: '选择', description: '单选按钮组' },
  DATE: { label: '日期', icon: 'Calendar', group: '时间', description: '日期选择' },
  TIME: { label: '时间', icon: 'Clock', group: '时间', description: '时间选择' },
  DATETIME: { label: '日期时间', icon: 'CalendarClock', group: '时间', description: '日期时间选择' },
  PHOTO: { label: '拍照', icon: 'Camera', group: '媒体', description: '拍照或上传图片' },
  VIDEO: { label: '视频', icon: 'Video', group: '媒体', description: '录制或上传视频' },
  SIGNATURE: { label: '签名', icon: 'PenTool', group: '媒体', description: '手写签名' },
  FILE_UPLOAD: { label: '文件上传', icon: 'Upload', group: '媒体', description: '上传文件' },
  GPS: { label: 'GPS定位', icon: 'MapPin', group: '特殊', description: 'GPS坐标采集' },
  BARCODE: { label: '条码扫描', icon: 'ScanLine', group: '特殊', description: '条码/二维码扫描' },
  PERSON_SCORE: { label: '逐人评分', icon: 'Users', group: '关联', description: '按人员逐个评分' },
  VIOLATION_RECORD: { label: '违纪记录', icon: 'AlertTriangle', group: '关联', description: '记录违纪情况' },
}

/** 字段类型分组 */
export const ItemTypeGroups = ['文本', '数字', '选择', '时间', '媒体', '特殊', '关联'] as const


// ==================== 执行引擎枚举 ====================

export type ProjectStatus = 'DRAFT' | 'PUBLISHED' | 'PAUSED' | 'COMPLETED' | 'ARCHIVED'

export const ProjectStatusConfig: Record<ProjectStatus, { label: string; type: string; color: string }> = {
  DRAFT: { label: '草稿', type: 'info', color: '#909399' },
  PUBLISHED: { label: '进行中', type: 'success', color: '#67C23A' },
  PAUSED: { label: '已暂停', type: 'warning', color: '#E6A23C' },
  COMPLETED: { label: '已完成', type: 'primary', color: '#409EFF' },
  ARCHIVED: { label: '已归档', type: 'danger', color: '#F56C6C' },
}

export type TaskStatus = 'PENDING' | 'CLAIMED' | 'IN_PROGRESS' | 'SUBMITTED' | 'UNDER_REVIEW' | 'REVIEWED' | 'PUBLISHED' | 'CANCELLED' | 'EXPIRED'

export const TaskStatusConfig: Record<TaskStatus, { label: string; type: string; color: string }> = {
  PENDING: { label: '待领取', type: 'info', color: '#909399' },
  CLAIMED: { label: '已领取', type: '', color: '#606266' },
  IN_PROGRESS: { label: '进行中', type: 'primary', color: '#409EFF' },
  SUBMITTED: { label: '已提交', type: 'warning', color: '#E6A23C' },
  UNDER_REVIEW: { label: '审核中', type: 'warning', color: '#E6A23C' },
  REVIEWED: { label: '已审核', type: 'success', color: '#67C23A' },
  PUBLISHED: { label: '已发布', type: 'success', color: '#67C23A' },
  CANCELLED: { label: '已取消', type: 'danger', color: '#F56C6C' },
  EXPIRED: { label: '已过期', type: 'danger', color: '#F56C6C' },
}

export type SubmissionStatus = 'PENDING' | 'LOCKED' | 'IN_PROGRESS' | 'COMPLETED' | 'SKIPPED'

export const SubmissionStatusConfig: Record<SubmissionStatus, { label: string; type: string; color: string }> = {
  PENDING: { label: '待检查', type: 'info', color: '#909399' },
  LOCKED: { label: '已锁定', type: '', color: '#606266' },
  IN_PROGRESS: { label: '填写中', type: 'primary', color: '#409EFF' },
  COMPLETED: { label: '已完成', type: 'success', color: '#67C23A' },
  SKIPPED: { label: '已跳过', type: 'warning', color: '#E6A23C' },
}

export type ScopeType = 'ORG' | 'PLACE' | 'USER' | 'CUSTOM'
export type TargetType = 'ORG' | 'PLACE' | 'USER' | 'ASSET' | 'COMPOSITE'
export type CycleType = 'DAILY' | 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'CUSTOM'
export type AssignmentMode = 'FREE' | 'ASSIGNED'
export type InspectorRole = 'INSPECTOR' | 'REVIEWER' | 'LEAD'
export type ScoringMode = 'DEDUCTION' | 'ADDITION' | 'DIRECT' | 'PASS_FAIL'
  | 'LEVEL' | 'SCORE_TABLE' | 'CUMULATIVE' | 'TIERED_DEDUCTION'
  | 'RATING_SCALE' | 'WEIGHTED_MULTI' | 'RISK_MATRIX' | 'THRESHOLD' | 'FORMULA'

export const ScoringModeConfig: Record<ScoringMode, { label: string; description: string }> = {
  PASS_FAIL: { label: '通过/不通过', description: '二元判定：通过或不通过' },
  DEDUCTION: { label: '扣分', description: '从基准分中扣除' },
  ADDITION: { label: '加分', description: '在基准分上增加' },
  DIRECT: { label: '直接打分', description: '在范围内直接给分' },
  LEVEL: { label: '等级评分', description: '选择等级标签，映射固定分值' },
  SCORE_TABLE: { label: '评分标准表', description: '按档位描述对照评分' },
  CUMULATIVE: { label: '累计计次', description: '计次 × 单次分值 = 总分' },
  TIERED_DEDUCTION: { label: '分档扣分', description: '按严重程度选档扣分' },
  RATING_SCALE: { label: '评级量表', description: '星级评分，可视化选择' },
  WEIGHTED_MULTI: { label: '多维加权', description: '多维度分别打分加权汇总' },
  RISK_MATRIX: { label: '风险矩阵', description: '可能性 × 影响度 = 风险值' },
  THRESHOLD: { label: '阈值判定', description: '输入实测值，按阈值判定等级' },
  FORMULA: { label: '公式计算', description: '输入参数，按公式自动计算' },
}
export type EvidenceType = 'PHOTO' | 'VIDEO' | 'DOCUMENT' | 'SIGNATURE' | 'GPS_POINT'

export const ScopeTypeConfig: Record<ScopeType, { label: string }> = {
  ORG: { label: '组织范围' },
  PLACE: { label: '场所范围' },
  USER: { label: '人员范围' },
  CUSTOM: { label: '自定义范围' },
}

export const TargetTypeConfig: Record<TargetType, { label: string }> = {
  ORG: { label: '组织' },
  PLACE: { label: '场所' },
  USER: { label: '人员' },
  ASSET: { label: '资产' },
  COMPOSITE: { label: '组合' },
}

export const CycleTypeConfig: Record<CycleType, { label: string }> = {
  DAILY: { label: '每日' },
  WEEKLY: { label: '每周' },
  BIWEEKLY: { label: '双周' },
  MONTHLY: { label: '每月' },
  QUARTERLY: { label: '每季度' },
  CUSTOM: { label: '自定义' },
}

export const AssignmentModeConfig: Record<AssignmentMode, { label: string; description: string }> = {
  FREE: { label: '自由领取', description: '检查员自行领取可用任务' },
  ASSIGNED: { label: '指定分配', description: '管理员指定检查员' },
}

export const InspectorRoleConfig: Record<InspectorRole, { label: string }> = {
  INSPECTOR: { label: '检查员' },
  REVIEWER: { label: '审核员' },
  LEAD: { label: '组长' },
}

export const EvidenceTypeConfig: Record<EvidenceType, { label: string; icon: string }> = {
  PHOTO: { label: '照片', icon: 'Camera' },
  VIDEO: { label: '视频', icon: 'Video' },
  DOCUMENT: { label: '文档', icon: 'FileText' },
  SIGNATURE: { label: '签名', icon: 'PenTool' },
  GPS_POINT: { label: 'GPS定位', icon: 'MapPin' },
}

// ==================== 违纪严重程度 ====================

export type ViolationSeverity = 'MINOR' | 'MODERATE' | 'SEVERE'

export const ViolationSeverityConfig: Record<ViolationSeverity, { label: string; type: string; color: string }> = {
  MINOR: { label: '轻微', type: 'info', color: '#909399' },
  MODERATE: { label: '一般', type: 'warning', color: '#E6A23C' },
  SEVERE: { label: '严重', type: 'danger', color: '#F56C6C' },
}

// ==================== 分析报表 ====================

export type PeriodType = 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
export type TrendDirection = 'UP' | 'DOWN' | 'STABLE'

export const PeriodTypeConfig: Record<PeriodType, { label: string }> = {
  WEEKLY: { label: '周' },
  MONTHLY: { label: '月' },
  QUARTERLY: { label: '季度' },
  YEARLY: { label: '年' },
}

export const TrendDirectionConfig: Record<TrendDirection, { label: string; icon: string; color: string }> = {
  UP: { label: '上升', icon: 'TrendingUp', color: '#67C23A' },
  DOWN: { label: '下降', icon: 'TrendingDown', color: '#F56C6C' },
  STABLE: { label: '持平', icon: 'Minus', color: '#909399' },
}

// ==================== 整改管理 ====================

export type CaseStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'SUBMITTED' | 'VERIFIED' | 'REJECTED' | 'CLOSED' | 'ESCALATED'
export type CasePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export const CaseStatusConfig: Record<CaseStatus, { label: string; type: string; color: string }> = {
  OPEN: { label: '待分配', type: 'info', color: '#909399' },
  ASSIGNED: { label: '已分配', type: 'primary', color: '#409EFF' },
  IN_PROGRESS: { label: '整改中', type: 'warning', color: '#E6A23C' },
  SUBMITTED: { label: '已提交', type: 'primary', color: '#409EFF' },
  VERIFIED: { label: '已验证', type: 'success', color: '#67C23A' },
  REJECTED: { label: '已驳回', type: 'danger', color: '#F56C6C' },
  CLOSED: { label: '已关闭', type: 'success', color: '#67C23A' },
  ESCALATED: { label: '已升级', type: 'danger', color: '#F56C6C' },
}

export const CasePriorityConfig: Record<CasePriority, { label: string; type: string; color: string }> = {
  LOW: { label: '低', type: 'info', color: '#909399' },
  MEDIUM: { label: '中', type: 'warning', color: '#E6A23C' },
  HIGH: { label: '高', type: 'danger', color: '#F56C6C' },
  CRITICAL: { label: '紧急', type: 'danger', color: '#CC0000' },
}
