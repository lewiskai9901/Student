import { describe, it, expect } from 'vitest'
import {
  buildSectionTree,
  flattenTree,
  getLeafSections,
  getDepth,
} from '@/utils/sectionTree'

describe('sectionTree utils', () => {
  describe('buildSectionTree', () => {
    it('空列表返回空树', () => {
      expect(buildSectionTree([], 0)).toEqual([])
    })

    it('单根 + 多 children 正确嵌套', () => {
      const list = [
        { id: 1, sectionName: '卫生', parentSectionId: 100, sortOrder: 1 },
        { id: 2, sectionName: '安全', parentSectionId: 100, sortOrder: 2 },
        { id: 3, sectionName: '教室', parentSectionId: 1, sortOrder: 1 },
        { id: 4, sectionName: '宿舍', parentSectionId: 1, sortOrder: 2 },
      ]
      const tree = buildSectionTree(list, 100)
      expect(tree).toHaveLength(2)
      expect(tree[0].sectionName).toBe('卫生')
      expect(tree[0].children).toHaveLength(2)
      expect(tree[0].isLeaf).toBe(false)
      expect(tree[1].sectionName).toBe('安全')
      expect(tree[1].isLeaf).toBe(true)
    })

    it('按 sortOrder 升序', () => {
      const list = [
        { id: 1, sectionName: 'B', parentSectionId: 100, sortOrder: 2 },
        { id: 2, sectionName: 'A', parentSectionId: 100, sortOrder: 1 },
        { id: 3, sectionName: 'C', parentSectionId: 100, sortOrder: 3 },
      ]
      const tree = buildSectionTree(list, 100)
      expect(tree.map(n => n.sectionName)).toEqual(['A', 'B', 'C'])
    })

    it('rootId 字符串/数字皆兼容', () => {
      const list = [{ id: 1, sectionName: 'X', parentSectionId: 99, sortOrder: 0 }]
      expect(buildSectionTree(list, '99')).toHaveLength(1)
      expect(buildSectionTree(list, 99)).toHaveLength(1)
    })

    it('isLeaf 正确标记', () => {
      const list = [
        { id: 1, sectionName: 'P', parentSectionId: 0, sortOrder: 0 },
        { id: 2, sectionName: 'C', parentSectionId: 1, sortOrder: 0 },
      ]
      const tree = buildSectionTree(list, 0)
      expect(tree[0].isLeaf).toBe(false)
      expect(tree[0].children[0].isLeaf).toBe(true)
    })
  })

  describe('flattenTree', () => {
    it('深度优先遍历', () => {
      const list = [
        { id: 1, sectionName: 'A', parentSectionId: 0, sortOrder: 1 },
        { id: 2, sectionName: 'A1', parentSectionId: 1, sortOrder: 1 },
        { id: 3, sectionName: 'B', parentSectionId: 0, sortOrder: 2 },
      ]
      const tree = buildSectionTree(list, 0)
      const flat = flattenTree(tree)
      expect(flat.map(n => n.sectionName)).toEqual(['A', 'A1', 'B'])
    })

    it('空树空数组', () => {
      expect(flattenTree([])).toEqual([])
    })
  })

  describe('getLeafSections', () => {
    it('只返回叶子节点', () => {
      const list = [
        { id: 1, sectionName: 'P', parentSectionId: 0, sortOrder: 0 },
        { id: 2, sectionName: 'L1', parentSectionId: 1, sortOrder: 0 },
        { id: 3, sectionName: 'L2', parentSectionId: 1, sortOrder: 1 },
        { id: 4, sectionName: 'X', parentSectionId: 0, sortOrder: 1 },
      ]
      const tree = buildSectionTree(list, 0)
      const leaves = getLeafSections(tree)
      expect(leaves.map(l => l.sectionName).sort()).toEqual(['L1', 'L2', 'X'])
    })
  })

  describe('getDepth', () => {
    it('根节点深度 0, 子节点 1, 孙节点 2', () => {
      const list = [
        { id: 1, sectionName: 'R', parentSectionId: 0, sortOrder: 0 },
        { id: 2, sectionName: 'C', parentSectionId: 1, sortOrder: 0 },
        { id: 3, sectionName: 'GC', parentSectionId: 2, sortOrder: 0 },
      ]
      const tree = buildSectionTree(list, 0)
      expect(getDepth(tree, 1)).toBe(0)
      expect(getDepth(tree, 2)).toBe(1)
      expect(getDepth(tree, 3)).toBe(2)
    })

    it('找不到的节点返回 -1', () => {
      const list = [{ id: 1, sectionName: 'A', parentSectionId: 0, sortOrder: 0 }]
      const tree = buildSectionTree(list, 0)
      expect(getDepth(tree, 999)).toBe(-1)
    })
  })
})
