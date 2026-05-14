/**
 * 分区树构建工具
 * 将扁平分区列表构建为嵌套树结构
 */
import type { LongId } from '@/types/common'

export interface SectionTreeNode {
  id: LongId
  sectionName: string
  targetType?: string
  parentSectionId: LongId | null
  sortOrder: number
  children: SectionTreeNode[]
  isLeaf: boolean
}

/**
 * 从扁平分区列表构建树
 * @param sections 扁平分区列表（从 getSections API 返回）
 * @param rootId 根分区 ID（不包含在树中，其直接子分区为树的根节点）
 */
export function buildSectionTree(sections: Array<any>, rootId: LongId | string | number): SectionTreeNode[] {
  // Normalize all comparisons to strings (Jackson Long→string contract).
  const rid = rootId == null ? null : String(rootId)
  const map = new Map<LongId, SectionTreeNode>()

  // 初始化所有节点 — id/parentSectionId 都规范化为 string 以一致比较
  for (const s of sections) {
    const sid = String(s.id)
    map.set(sid, {
      id: sid,
      sectionName: s.sectionName,
      targetType: s.targetType,
      parentSectionId: s.parentSectionId != null ? String(s.parentSectionId) : null,
      sortOrder: s.sortOrder ?? 0,
      children: [],
      isLeaf: true,
    })
  }

  // 构建父子关系
  const roots: SectionTreeNode[] = []
  for (const node of map.values()) {
    if (node.parentSectionId === rid) {
      roots.push(node)
    } else {
      const parent = map.get(node.parentSectionId!)
      if (parent) {
        parent.children.push(node)
        parent.isLeaf = false
      }
    }
  }

  // 排序
  const sortNodes = (nodes: SectionTreeNode[]) => {
    nodes.sort((a, b) => a.sortOrder - b.sortOrder)
    nodes.forEach(n => sortNodes(n.children))
  }
  sortNodes(roots)

  return roots
}

/**
 * 扁平化树（用于遍历）
 */
export function flattenTree(nodes: SectionTreeNode[]): SectionTreeNode[] {
  const result: SectionTreeNode[] = []
  const walk = (list: SectionTreeNode[]) => {
    for (const n of list) {
      result.push(n)
      walk(n.children)
    }
  }
  walk(nodes)
  return result
}

/**
 * 获取所有叶子节点
 */
export function getLeafSections(nodes: SectionTreeNode[]): SectionTreeNode[] {
  return flattenTree(nodes).filter(n => n.isLeaf)
}

/**
 * 获取节点深度
 */
export function getDepth(nodes: SectionTreeNode[], targetId: LongId | number): number {
  const tid = String(targetId)
  const find = (list: SectionTreeNode[], depth: number): number => {
    for (const n of list) {
      if (n.id === tid) return depth
      const d = find(n.children, depth + 1)
      if (d >= 0) return d
    }
    return -1
  }
  return find(nodes, 0)
}
