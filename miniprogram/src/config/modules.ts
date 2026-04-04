import type { AppModule } from '@/types/module'

/**
 * Module registry - permission-driven.
 * Home page filters this list based on user's permissions.
 * To add a new module: add one entry here + create the page + add the route in pages.json.
 */
export const moduleRegistry: AppModule[] = [
  { key: 'inspection:task:execute', label: '检查任务', icon: 'edit-outline', iconColor: '#3a7bd5', path: '/pages/inspection/task/list' },
  { key: 'inspection:task:review', label: '审核管理', icon: 'check-outline', iconColor: '#e0592a', path: '/pages/inspection/review/list' },
  { key: 'inspection:corrective:handle', label: '整改处理', icon: 'warning', iconColor: '#15a87e', path: '/pages/inspection/corrective/list' },
  { key: 'inspection:stats:view', label: '数据统计', icon: 'chart', iconColor: '#9a5cc6', path: '/pages/inspection/stats/dashboard' },
  { key: 'org:unit:view', label: '组织架构', icon: 'office-building', iconColor: '#d4a030', path: '/pages/organization/units' },
  { key: 'org:member:view', label: '人员管理', icon: 'friends', iconColor: '#4a90d9', path: '/pages/organization/members' },
  { key: 'schedule:view', label: '日程安排', icon: 'calendar', iconColor: '#e07070', path: '/pages/schedule/index' },
  { key: 'common:scan', label: '扫码', icon: 'scan', iconColor: '#50a0a0', path: '' },
]
