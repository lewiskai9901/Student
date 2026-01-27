import { http } from '@/utils/request'

/**
 * 导出模板相关接口
 */

// 筛选配置
export interface FilterConfig {
  deductionItemIds?: (string | number)[]
  checkRounds?: number[]
  categoryIds?: (string | number)[]
  includeAllItems?: boolean
}

// 表格列配置
export interface ColumnConfig {
  field: string
  label: string
  alias?: string  // 列别名（显示名称）
  width?: number
  align?: 'left' | 'center' | 'right'
  groupable?: boolean  // 是否适合作为分组层级
  mergeType?: string   // 合并类型：'', 'group', 'concat'
  separator?: string   // 数据合并分隔符
  groupOrder?: number  // 分组优先级
  groupByField?: string // 数据合并时参考的分组字段
  // 字段组合配置
  isComposite?: boolean  // 是否为组合字段
  compositeFields?: string[]  // 组合的字段列表
  compositeSeparator?: string  // 组合字段分隔符
}

// 表格配置
export interface TableConfig {
  title?: string  // 表格标题
  showTitle?: boolean  // 是否显示标题
  columns?: ColumnConfig[]
  showOrgUnitHeader?: boolean
  showGradeHeader?: boolean
  showClassHeader?: boolean
  mergeMode?: string
  // 样式配置
  borderColor?: string
  borderWidth?: string
  borderStyle?: string
  headerBgColor?: string
  headerTextColor?: string
  headerFontWeight?: string
  headerFontSize?: string
  cellPadding?: string
  fontSize?: string
  textAlign?: string
  verticalAlign?: string
  lineHeight?: string
  rowHeight?: string
  zebraStripes?: boolean
  zebraColor?: string
  hoverHighlight?: boolean
  hoverColor?: string
  mergeConfig?: {
    enabled?: boolean
    hierarchyLevels?: string[]
    concatDataRows?: boolean
    separator?: string
  }
}

// 导出模板DTO
export interface ExportTemplateDTO {
  id?: string | number
  planId: string | number
  planName?: string
  templateName: string
  description?: string
  filterConfig?: FilterConfig
  tables?: TableConfig[]  // 多表格配置
  tableConfig?: TableConfig  // 向后兼容
  groupBy?: string
  sortBy?: string
  outputFormat?: 'PDF' | 'WORD' | 'EXCEL'
  documentTemplate?: string
  sortOrder?: number
  status?: number
  createdAt?: string
}

// 创建/更新模板请求
export interface ExportTemplateRequest {
  templateName: string
  description?: string
  filterConfig?: FilterConfig
  tables?: TableConfig[]  // 多表格配置
  tableConfig?: TableConfig  // 向后兼容
  groupBy?: string
  sortBy?: string
  outputFormat?: string
  documentTemplate?: string
  sortOrder?: number
  status?: number
}

// 导出请求
export interface ExportRequest {
  templateId: string | number
  classIds?: (string | number)[]
  checkRounds?: number[]
  format?: string
}

// 学生记录
export interface StudentRecordDTO {
  studentId: string | number
  studentNo?: string
  studentName?: string
  gender?: string
  classId?: string | number
  className?: string
  gradeId?: string | number
  gradeName?: string
  orgUnitId?: string | number
  orgUnitName?: string
  dormitoryNo?: string
  deductionItemId?: string | number
  deductionItemName?: string
  deductScore?: number
  remark?: string
  checkRound?: number
  checkerName?: string
}

// 班级统计
export interface ClassStatDTO {
  classId: string | number
  className?: string
  orgUnitId?: string | number
  orgUnitName?: string
  gradeId?: string | number
  gradeName?: string
  studentCount: number
}

// 班级分组
export interface ClassGroupDTO {
  classId: string | number
  className?: string
  totalCount: number
  students: StudentRecordDTO[]
}

// 年级分组
export interface GradeGroupDTO {
  gradeId: string | number
  gradeName?: string
  totalCount: number
  classes: ClassGroupDTO[]
}

// 组织单元分组
export interface OrgUnitGroupDTO {
  orgUnitId: string | number
  orgUnitName?: string
  totalCount: number
  grades: GradeGroupDTO[]
}

// 导出预览
export interface ExportPreviewDTO {
  checkDate?: string
  checkName?: string
  totalCount: number
  classCount: number
  orgUnitCount: number
  classStats?: ClassStatDTO[]
  groupedData?: OrgUnitGroupDTO[]
  renderedHtml?: string
}

// 创建导出模板
export function createExportTemplate(planId: string | number, data: ExportTemplateRequest) {
  return http.post<ExportTemplateDTO>(`/check-plans/${planId}/export-templates`, data)
}

// 获取检查计划的导出模板列表
export function getExportTemplatesByPlan(planId: string | number) {
  return http.get<ExportTemplateDTO[]>(`/check-plans/${planId}/export-templates`)
}

// 获取模板详情
export function getExportTemplate(templateId: string | number) {
  return http.get<ExportTemplateDTO>(`/export-templates/${templateId}`)
}

// 更新导出模板
export function updateExportTemplate(templateId: string | number, data: ExportTemplateRequest) {
  return http.put<ExportTemplateDTO>(`/export-templates/${templateId}`, data)
}

// 删除导出模板
export function deleteExportTemplate(templateId: string | number) {
  return http.delete<void>(`/export-templates/${templateId}`)
}

// 获取日常检查可用的导出模板
export function getExportTemplatesForCheck(checkId: string | number) {
  return http.get<ExportTemplateDTO[]>(`/daily-checks/${checkId}/export-templates`)
}

// 获取导出预览
export function getExportPreview(checkId: string | number, data: ExportRequest) {
  return http.post<ExportPreviewDTO>(`/daily-checks/${checkId}/export/preview`, data)
}

// 导出文件下载
export function exportFile(checkId: string | number, data: ExportRequest) {
  return http.post<Blob>(`/daily-checks/${checkId}/export/download`, data, {
    responseType: 'blob'
  })
}

// 默认可选字段列表
// groupable: true  => 适合作为分组层级（值会重复，适合合并单元格）
// groupable: false => 数据字段（每行不同或不适合分组）
export const AVAILABLE_FIELDS: ColumnConfig[] = [
  // 数据字段（不适合分组）
  { field: 'index', label: '序号', width: 60, align: 'center', groupable: false },
  { field: 'studentNo', label: '学号', width: 100, align: 'center', groupable: false },
  { field: 'studentName', label: '姓名', width: 80, align: 'center', groupable: false },
  { field: 'gender', label: '性别', width: 60, align: 'center', groupable: false },
  // 可分组字段（适合作为层级）
  { field: 'orgUnitName', label: '组织单元', width: 120, align: 'center', groupable: true },
  { field: 'gradeName', label: '年级', width: 100, align: 'center', groupable: true },
  { field: 'className', label: '班级', width: 120, align: 'center', groupable: true },
  { field: 'headTeacher', label: '班主任', width: 80, align: 'center', groupable: true },
  // 关联字段（宿舍/教室共用，根据关联类型显示不同内容）
  { field: 'buildingName', label: '楼号', width: 80, align: 'center', groupable: true },
  { field: 'roomNo', label: '房间号', width: 80, align: 'center', groupable: true },
  // 检查相关字段
  { field: 'categoryName', label: '检查类别', width: 100, align: 'center', groupable: true },
  { field: 'checkRound', label: '检查轮次', width: 80, align: 'center', groupable: true },
  { field: 'relationType', label: '关联类型', width: 80, align: 'center', groupable: true },
  { field: 'deductionItemName', label: '扣分项', width: 120, align: 'left', groupable: true },
  { field: 'deductMode', label: '扣分模式', width: 80, align: 'center', groupable: true },
  { field: 'personCount', label: '次数', width: 60, align: 'center', groupable: false },
  // 数据字段
  { field: 'deductScore', label: '扣分项原扣分分值', width: 60, align: 'center', groupable: false },
  { field: 'weightedDeductScore', label: '扣分项加权扣分分值', width: 100, align: 'center', groupable: false },
  { field: 'originalTotalScore', label: '扣分项原总扣分', width: 80, align: 'center', groupable: false },
  { field: 'totalWeightedDeductScore', label: '扣分项加权总扣分', width: 100, align: 'center', groupable: false },
  { field: 'categoryOriginalScore', label: '检查类别原总扣分', width: 100, align: 'center', groupable: false },
  { field: 'categoryWeightedScore', label: '检查类别加权总扣分', width: 110, align: 'center', groupable: false },
  { field: 'classOriginalScore', label: '班级原总扣分', width: 100, align: 'center', groupable: false },
  { field: 'classWeightedScore', label: '班级加权总扣分', width: 110, align: 'center', groupable: false },
  { field: 'remark', label: '备注', width: 150, align: 'left', groupable: false },
  { field: 'checkerName', label: '检查人', width: 80, align: 'center', groupable: false }
]
