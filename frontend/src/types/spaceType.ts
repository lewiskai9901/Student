/**
 * V6 场所类型 类型定义（简化版）
 */

export interface SpaceType {
  id: number
  typeCode: string
  typeName: string
  parentTypeCode: string | null
  levelOrder: number
  icon: string | null
  description: string | null
  isSystem: boolean
  isEnabled: boolean
  sortOrder: number
}

export interface SpaceTypeTreeNode extends SpaceType {
  children: SpaceTypeTreeNode[]
}

export interface CreateSpaceTypeRequest {
  typeName: string
  parentTypeCode?: string
  levelOrder?: number
  icon?: string
  description?: string
  sortOrder?: number
}

export interface UpdateSpaceTypeRequest {
  typeName?: string
  icon?: string
  description?: string
  sortOrder?: number
}

// 类型图标映射
export const SPACE_TYPE_ICONS: Record<string, string> = {
  Home: 'House',
  House: 'House',
  School: 'School',
  Building: 'OfficeBuilding',
  OfficeBuilding: 'OfficeBuilding',
  BookOpen: 'Reading',
  Reading: 'Reading',
  Users: 'UserFilled',
  Briefcase: 'Briefcase',
  Trophy: 'Trophy',
  Layers: 'List',
  Monitor: 'Monitor',
  Mic: 'Mic',
  Activity: 'Soccer'
}
