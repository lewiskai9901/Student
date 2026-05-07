import { requestWrapped } from './request'

export interface OrgUnitTreeNode {
  id: string                  // snowflake, string-serialized
  parentId: string | null
  unitCode: string
  unitName: string
  unitType: string            // typeCode
  category: string            // ROOT/BRANCH/FUNCTIONAL/GROUP/CONTAINER
  typeName?: string
  typeIcon?: string
  typeColor?: string
  status: string              // DRAFT/ACTIVE/FROZEN/MERGING/DISSOLVED
  statusLabel?: string
  headcount?: number
  attributes?: Record<string, unknown>
  children?: OrgUnitTreeNode[]
}

export const orgApi = {
  tree: () => requestWrapped<OrgUnitTreeNode[]>({ url: '/org-units/tree' })
}
