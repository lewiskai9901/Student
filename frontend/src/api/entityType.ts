/**
 * 统一实体类型配置 API
 * 查询 entity_type_configs 表
 */
import { http } from '@/utils/request'

export interface EntityTypeConfig {
  id: number
  entityType: string       // ORG_UNIT / PLACE / USER
  typeCode: string
  typeName: string
  category?: string
  parentTypeCode?: string
  allowedChildTypeCodes?: string[]
  metadataSchema?: { fields: FieldSchema[] }
  features?: Record<string, boolean>
  uiConfig?: Record<string, any>
  isPluginRegistered?: boolean
  isEnabled?: boolean
}

export interface FieldSchema {
  key: string
  label: string
  type: string
  group?: string
  required?: boolean
  system?: boolean
  defaultValue?: any
  config?: Record<string, any>
}

export const entityTypeApi = {
  /** 查询某实体类型的所有类型配置 */
  list: (entityType: string) =>
    http.get<EntityTypeConfig[]>('/entity-type-configs', { params: { entityType } }),

  /** 查询单个类型配置 */
  get: (entityType: string, typeCode: string) =>
    http.get<EntityTypeConfig>('/entity-type-configs/detail', { params: { entityType, typeCode } }),

  /** 获取某父类型下允许的子类型 */
  getAllowedChildren: (entityType: string, parentTypeCode: string) =>
    http.get<EntityTypeConfig[]>('/entity-type-configs/allowed-children', { params: { entityType, parentTypeCode } }),

  /** 管理员添加自定义字段 */
  addCustomField: (id: number, field: FieldSchema) =>
    http.post(`/entity-type-configs/${id}/custom-fields`, field),

  /** 管理员删除自定义字段 */
  removeCustomField: (id: number, fieldKey: string) =>
    http.delete(`/entity-type-configs/${id}/custom-fields/${fieldKey}`),
}
