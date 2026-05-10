/**
 * F2: useSharedDataStore 单测.
 *
 * 验证 5 分钟 TTL 缓存 / force 刷新 / invalidate 单项与全量.
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/utils/request', () => ({
  http: { get: vi.fn() },
}))

vi.mock('@/api/calendar', () => ({
  semesterApi: { list: vi.fn() },
}))

vi.mock('@/api/academic', () => ({
  courseApi: { listAll: vi.fn() },
  majorApi: { getAllEnabled: vi.fn() },
}))

import { useSharedDataStore } from '@/stores/sharedData'
import { http } from '@/utils/request'
import { semesterApi } from '@/api/calendar'

describe('useSharedDataStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-01-01T00:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('getSemesters', () => {
    it('首次调用 fetch + 缓存', async () => {
      vi.mocked(semesterApi.list).mockResolvedValueOnce([{ id: 1, isCurrent: true }] as never)
      const store = useSharedDataStore()
      const data = await store.getSemesters()
      expect(data).toEqual([{ id: 1, isCurrent: true }])
      expect(semesterApi.list).toHaveBeenCalledTimes(1)
    })

    it('5 分钟内重复调用走缓存', async () => {
      vi.mocked(semesterApi.list).mockResolvedValue([{ id: 1 }] as any)
      const store = useSharedDataStore()
      await store.getSemesters()
      vi.advanceTimersByTime(60_000)
      await store.getSemesters()
      expect(semesterApi.list).toHaveBeenCalledTimes(1)
    })

    it('TTL 过期后重新 fetch', async () => {
      vi.mocked(semesterApi.list).mockResolvedValue([{ id: 1 }] as any)
      const store = useSharedDataStore()
      await store.getSemesters()
      vi.advanceTimersByTime(6 * 60 * 1000) // 6 min > 5 min TTL
      await store.getSemesters()
      expect(semesterApi.list).toHaveBeenCalledTimes(2)
    })

    it('force=true 强制刷新', async () => {
      vi.mocked(semesterApi.list).mockResolvedValue([{ id: 1 }] as any)
      const store = useSharedDataStore()
      await store.getSemesters()
      await store.getSemesters(true)
      expect(semesterApi.list).toHaveBeenCalledTimes(2)
    })

    it('设置 isCurrent 学期到 currentSemester', async () => {
      vi.mocked(semesterApi.list).mockResolvedValueOnce([
        { id: 1, isCurrent: false },
        { id: 2, isCurrent: true },
      ] as never)
      const store = useSharedDataStore()
      await store.getSemesters()
      const cur = await store.getCurrentSemester()
      expect(cur).toEqual({ id: 2, isCurrent: true })
    })
  })

  describe('getOrgTree / getDepartments', () => {
    it('getOrgTree 调用 /org-units/tree 并缓存', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([{ id: 1, unitName: 'Root' }])
      const store = useSharedDataStore()
      const data = await store.getOrgTree()
      expect(http.get).toHaveBeenCalledWith('/org-units/tree')
      expect(data).toHaveLength(1)
    })

    it('getDepartments 把树扁平化', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([
        {
          id: 1,
          unitName: 'A',
          treeLevel: 1,
          children: [{ id: 2, unitName: 'A1', treeLevel: 2, children: [] }],
        },
      ])
      const store = useSharedDataStore()
      const list = await store.getDepartments()
      expect(list).toHaveLength(2)
      expect(list[0]).toEqual({ id: 1, name: 'A', level: 1 })
      expect(list[1]).toEqual({ id: 2, name: 'A1', level: 2 })
    })

    it('getDepartments 非数组安全降级', async () => {
      vi.mocked(http.get).mockResolvedValueOnce({} as any)
      const store = useSharedDataStore()
      const list = await store.getDepartments()
      expect(list).toEqual([])
    })
  })

  describe('getClassList', () => {
    it('records 字段优先', async () => {
      vi.mocked(http.get).mockResolvedValueOnce({ records: [{ id: 1 }] })
      const store = useSharedDataStore()
      const data = await store.getClassList()
      expect(data).toEqual([{ id: 1 }])
    })

    it('裸数组也能解析', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([{ id: 2 }])
      const store = useSharedDataStore()
      const data = await store.getClassList()
      expect(data).toEqual([{ id: 2 }])
    })
  })

  describe('invalidate', () => {
    it('invalidate("semesters") 清单项', async () => {
      vi.mocked(semesterApi.list).mockResolvedValue([{ id: 1, isCurrent: true }] as any)
      const store = useSharedDataStore()
      await store.getSemesters()
      store.invalidate('semesters')
      await store.getSemesters()
      expect(semesterApi.list).toHaveBeenCalledTimes(2)
    })

    it('invalidateAll 清全部缓存', async () => {
      vi.mocked(semesterApi.list).mockResolvedValue([{ id: 1 }] as any)
      vi.mocked(http.get).mockResolvedValue([] as any)
      const store = useSharedDataStore()
      await store.getSemesters()
      await store.getOrgTree()
      store.invalidateAll()
      await store.getSemesters()
      await store.getOrgTree()
      expect(semesterApi.list).toHaveBeenCalledTimes(2)
      expect(http.get).toHaveBeenCalledWith('/org-units/tree')
    })
  })
})
