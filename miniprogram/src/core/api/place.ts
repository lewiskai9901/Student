import { requestWrapped } from './request'

export interface PlaceTreeNode {
  id: number
  parentId?: number | null
  placeCode: string
  placeName: string
  typeCode: string
  typeName?: string
  typeIcon?: string
  description?: string
  level?: number
  capacity?: number
  currentOccupancy?: number
  orgUnitId?: number
  orgUnitName?: string
  responsibleUserName?: string
  status?: number
  attributes?: Record<string, unknown>
  hasCapacity?: boolean
  bookable?: boolean
  assignable?: boolean
  occupiable?: boolean
  capacityUnit?: string
  leaf?: boolean
  children?: PlaceTreeNode[]
}

export const placeApi = {
  tree: (maxDepth = 0) =>
    requestWrapped<PlaceTreeNode[]>({ url: `/v9/places/tree?maxDepth=${maxDepth}` })
}
