/**
 * 容量告警相关类型定义
 * 对标: AWS CloudWatch Alarms
 */

/**
 * 告警级别
 */
export type AlertLevel =
  | 'WARNING'    // 警告 (80-94%)
  | 'CRITICAL'   // 严重 (95-99%)
  | 'FULL'       // 已满 (100%)

/**
 * 容量告警DTO
 */
export interface CapacityAlertDTO {
  /** 场所ID */
  placeId: number | string
  /** 场所编码 */
  placeCode: string
  /** 场所名称 */
  placeName: string
  /** 场所类型编码 */
  typeCode: string
  /** 场所类型名称 */
  typeName?: string
  /** 总容量 */
  capacity: number
  /** 当前占用数 */
  currentOccupancy: number
  /** 占用率（百分比） */
  occupancyRate: number
  /** 告警级别 */
  alertLevel: AlertLevel
  /** 所属组织单元ID */
  orgUnitId?: number | string
  /** 所属组织单元名称 */
  orgUnitName?: string
  /** 负责人ID */
  responsibleUserId?: number | string
  /** 负责人姓名 */
  responsibleUserName?: string
  /** 首次告警时间 */
  firstAlertTime?: string
  /** 最后更新时间 */
  lastUpdatedAt: string
}

/**
 * 类型告警汇总
 */
export interface TypeAlertSummary {
  /** 类型编码 */
  typeCode: string
  /** 总告警数 */
  totalAlerts: number
  /** 警告级别数量 */
  warningCount: number
  /** 严重级别数量 */
  criticalCount: number
  /** 已满数量 */
  fullCount: number
}

/**
 * 告警检查响应
 */
export interface AlertCheckResponse {
  /** 场所ID */
  placeId: number | string
  /** 是否应该告警 */
  shouldAlert: boolean
}

/**
 * 告警横幅配置
 */
export interface AlertBannerOptions {
  /** 自动刷新间隔（秒） */
  refreshInterval?: number
  /** 显示汇总统计 */
  showSummary?: boolean
  /** 可折叠 */
  collapsible?: boolean
  /** 默认展开 */
  defaultExpanded?: boolean
}

/**
 * 告警级别配置
 */
export interface AlertLevelConfig {
  /** 级别 */
  level: AlertLevel
  /** 颜色 */
  color: string
  /** 图标 */
  icon: string
  /** 标签 */
  label: string
  /** 阈值 */
  threshold: number
}

/**
 * 默认告警级别配置
 */
export const DEFAULT_ALERT_LEVELS: AlertLevelConfig[] = [
  {
    level: 'WARNING',
    color: 'warning',
    icon: 'Warning',
    label: '警告',
    threshold: 80
  },
  {
    level: 'CRITICAL',
    color: 'danger',
    icon: 'CircleClose',
    label: '严重',
    threshold: 95
  },
  {
    level: 'FULL',
    color: 'danger',
    icon: 'CircleCloseFilled',
    label: '已满',
    threshold: 100
  }
]
