// Dashboard overview response types (通用核心分区)
//
// 行业分区 (如教育的 education/办学规模、teaching/教务) 不在此定义 —
// 它们由后端对应插件的 DashboardSectionContributor 产出, 经索引签名透传,
// 类型在各插件卡组件内部声明 (views/plugins/*/dashboard/**)。

export interface OrgStats {
  orgUnitCount: number
}

export interface InspectionStats {
  activeProjectCount: number
  pendingTaskCount: number
  correctiveOpenCount: number
}

export interface SystemStats {
  totalUsers: number
  todayLoginCount: number
}

interface DashboardOverviewCore {
  organization: OrgStats
  inspection: InspectionStats
  system: SystemStats
}

/** 核心分区 + 行业插件分区 (sectionKey → 分区统计 map, 经交叉类型透传) */
export type DashboardOverview = DashboardOverviewCore & {
  [sectionKey: string]: unknown
}
