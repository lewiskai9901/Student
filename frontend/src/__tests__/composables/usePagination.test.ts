import { describe, it, expect } from 'vitest'
import { usePagination } from '@/composables/usePagination'
import { nextTick } from 'vue'

/**
 * usePagination Composable 测试
 */
describe('usePagination', () => {
  describe('初始化', () => {
    it('应该使用默认配置初始化', () => {
      const pagination = usePagination()

      expect(pagination.currentPage.value).toBe(1)
      expect(pagination.pageSize.value).toBe(10)
      expect(pagination.total.value).toBe(0)
      expect(pagination.pageSizes).toEqual([10, 20, 50, 100])
    })

    it('应该使用自定义配置初始化', () => {
      const pagination = usePagination({
        defaultPageSize: 20,
        pageSizes: [20, 50, 100]
      })

      expect(pagination.pageSize.value).toBe(20)
      expect(pagination.pageSizes).toEqual([20, 50, 100])
    })
  })

  describe('计算属性', () => {
    it('应该正确计算总页数', () => {
      const pagination = usePagination()
      pagination.setTotal(95)

      expect(pagination.totalPages.value).toBe(10) // ceil(95/10)
    })

    it('应该在没有数据时总页数为 0', () => {
      const pagination = usePagination()

      expect(pagination.totalPages.value).toBe(0)
    })

    it('应该正确判断是否有上一页', () => {
      const pagination = usePagination()
      pagination.setTotal(100)

      expect(pagination.hasPrevPage.value).toBe(false)

      pagination.currentPage.value = 2
      expect(pagination.hasPrevPage.value).toBe(true)
    })

    it('应该正确判断是否有下一页', () => {
      const pagination = usePagination()
      pagination.setTotal(25)

      expect(pagination.hasNextPage.value).toBe(true) // 当前第1页，共3页

      pagination.currentPage.value = 3
      expect(pagination.hasNextPage.value).toBe(false)
    })

    it('应该返回正确的分页参数', () => {
      const pagination = usePagination()
      pagination.currentPage.value = 2
      pagination.pageSize.value = 20

      expect(pagination.paginationParams.value).toEqual({
        pageNum: 2,
        pageSize: 20
      })
    })
  })

  describe('导航方法', () => {
    it('resetPage 应该重置到第一页', () => {
      const pagination = usePagination()
      pagination.currentPage.value = 5

      pagination.resetPage()

      expect(pagination.currentPage.value).toBe(1)
    })

    it('prevPage 应该跳转到上一页', () => {
      const pagination = usePagination()
      pagination.setTotal(100)
      pagination.currentPage.value = 3

      pagination.prevPage()

      expect(pagination.currentPage.value).toBe(2)
    })

    it('prevPage 在第一页时不应改变', () => {
      const pagination = usePagination()
      pagination.setTotal(100)

      pagination.prevPage()

      expect(pagination.currentPage.value).toBe(1)
    })

    it('nextPage 应该跳转到下一页', () => {
      const pagination = usePagination()
      pagination.setTotal(100)

      pagination.nextPage()

      expect(pagination.currentPage.value).toBe(2)
    })

    it('nextPage 在最后一页时不应改变', () => {
      const pagination = usePagination()
      pagination.setTotal(25) // 3页
      pagination.currentPage.value = 3

      pagination.nextPage()

      expect(pagination.currentPage.value).toBe(3)
    })

    it('goToPage 应该跳转到指定页', () => {
      const pagination = usePagination()
      pagination.setTotal(100)

      pagination.goToPage(5)

      expect(pagination.currentPage.value).toBe(5)
    })

    it('goToPage 不应跳转到无效页码', () => {
      const pagination = usePagination()
      pagination.setTotal(30) // 3页

      pagination.goToPage(0)
      expect(pagination.currentPage.value).toBe(1)

      pagination.goToPage(10)
      expect(pagination.currentPage.value).toBe(1)
    })
  })

  describe('setTotal', () => {
    it('应该正确设置总数', () => {
      const pagination = usePagination()

      pagination.setTotal(150)

      expect(pagination.total.value).toBe(150)
    })
  })

  describe('pageSize 变化', () => {
    it('pageSize 变化时应该重置到第一页', async () => {
      const pagination = usePagination()
      pagination.setTotal(100)
      pagination.currentPage.value = 5

      pagination.pageSize.value = 20
      await nextTick()

      expect(pagination.currentPage.value).toBe(1)
    })
  })

  describe('边界情况', () => {
    it('应该处理单页数据', () => {
      const pagination = usePagination()
      pagination.setTotal(5)

      expect(pagination.totalPages.value).toBe(1)
      expect(pagination.hasPrevPage.value).toBe(false)
      expect(pagination.hasNextPage.value).toBe(false)
    })

    it('应该处理刚好整除的情况', () => {
      const pagination = usePagination()
      pagination.setTotal(50)

      expect(pagination.totalPages.value).toBe(5)
    })
  })
})
