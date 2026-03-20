/**
 * Shared base types for the unified type system (OrgType, UserType, PlaceType).
 * Eliminates duplicated interface definitions across orgType.ts, userType.ts, universalPlace.ts.
 */

/**
 * Base interface for all configurable type entities.
 * Matches the 13 core fields from backend ConfigurableType.java.
 */
export interface ConfigurableType {
  id: number | string
  typeCode: string
  typeName: string
  category: string | null
  parentTypeCode: string | null
  icon: string | null
  description: string | null
  features: Record<string, boolean> | null
  metadataSchema: string | null
  allowedChildTypeCodes: string[] | null
  maxDepth: number | null
  system: boolean
  enabled: boolean
  sortOrder: number
}

/**
 * Generic tree node wrapping any ConfigurableType.
 * Backend uses @JsonUnwrapped so entity fields are flattened into the node.
 */
export interface TypeTreeNode<T extends ConfigurableType> extends T {
  children: TypeTreeNode<T>[]
}

/**
 * Category info returned by /xxx-types/categories endpoints.
 */
export interface CategoryInfo {
  code: string
  label: string
  defaultFeatures: Record<string, boolean>
}
