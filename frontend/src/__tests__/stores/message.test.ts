/**
 * F2: useMessageStore 单测.
 *
 * 验证未读计数 fetch / decrement / clear / 轮询启停.
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/message', () => ({
  messageApi: {
    getUnreadCount: vi.fn(),
  },
}))

import { useMessageStore } from '@/stores/message'
import { messageApi } from '@/api/message'

describe('useMessageStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('初始 unreadCount=0', () => {
    const store = useMessageStore()
    expect(store.unreadCount).toBe(0)
  })

  it('fetchUnreadCount 写入 count', async () => {
    vi.mocked(messageApi.getUnreadCount).mockResolvedValueOnce({ count: 5 } as any)
    const store = useMessageStore()
    await store.fetchUnreadCount()
    expect(store.unreadCount).toBe(5)
  })

  it('fetchUnreadCount API 抛错时静默 — 保持原值', async () => {
    vi.mocked(messageApi.getUnreadCount).mockRejectedValueOnce(new Error('500'))
    const store = useMessageStore()
    store.unreadCount = 3
    await store.fetchUnreadCount()
    expect(store.unreadCount).toBe(3)
  })

  it('fetchUnreadCount 缺 count 字段时回退 0', async () => {
    vi.mocked(messageApi.getUnreadCount).mockResolvedValueOnce({} as any)
    const store = useMessageStore()
    await store.fetchUnreadCount()
    expect(store.unreadCount).toBe(0)
  })

  it('decrementUnread 默认减 1', () => {
    const store = useMessageStore()
    store.unreadCount = 5
    store.decrementUnread()
    expect(store.unreadCount).toBe(4)
  })

  it('decrementUnread 不会变负数', () => {
    const store = useMessageStore()
    store.unreadCount = 1
    store.decrementUnread(5)
    expect(store.unreadCount).toBe(0)
  })

  it('decrementUnread 自定义数量', () => {
    const store = useMessageStore()
    store.unreadCount = 10
    store.decrementUnread(3)
    expect(store.unreadCount).toBe(7)
  })

  it('clearUnread 直接归零', () => {
    const store = useMessageStore()
    store.unreadCount = 99
    store.clearUnread()
    expect(store.unreadCount).toBe(0)
  })

  it('startPolling 立刻 fetch 一次并设置定时器', () => {
    vi.mocked(messageApi.getUnreadCount).mockResolvedValue({ count: 1 } as any)
    const store = useMessageStore()
    store.startPolling(1000)
    expect(messageApi.getUnreadCount).toHaveBeenCalledTimes(1)
    vi.advanceTimersByTime(1000)
    expect(messageApi.getUnreadCount).toHaveBeenCalledTimes(2)
  })

  it('startPolling 重复调用不会创建多个定时器', () => {
    vi.mocked(messageApi.getUnreadCount).mockResolvedValue({ count: 0 } as any)
    const store = useMessageStore()
    store.startPolling(500)
    store.startPolling(500)
    // 立刻调用一次, 第二次 startPolling 因 pollTimer 已存在不再 fetch
    expect(messageApi.getUnreadCount).toHaveBeenCalledTimes(1)
  })

  it('stopPolling 后定时器不再触发', () => {
    vi.mocked(messageApi.getUnreadCount).mockResolvedValue({ count: 0 } as any)
    const store = useMessageStore()
    store.startPolling(500)
    expect(messageApi.getUnreadCount).toHaveBeenCalledTimes(1)
    store.stopPolling()
    vi.advanceTimersByTime(2000)
    expect(messageApi.getUnreadCount).toHaveBeenCalledTimes(1)
  })

  it('stopPolling 在未启动时调用安全', () => {
    const store = useMessageStore()
    expect(() => store.stopPolling()).not.toThrow()
  })
})
