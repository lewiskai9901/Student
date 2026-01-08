/**
 * Store统一导出
 */
export { useUserStore } from './user'
export { useCheckStore } from './check'
export { useScoringStore } from './scoring'

// 类型导出
export type { UserInfo, StudentInfo } from './user'
export type {
  CheckTemplate,
  CheckTemplateItem,
  ScoreRange,
  DailyCheck,
  CheckDetail,
  CheckAppeal
} from './check'
