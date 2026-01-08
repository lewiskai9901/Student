/**
 * Vitest 测试设置文件
 * 在每个测试文件运行前执行
 */
import { config } from '@vue/test-utils'
import { vi } from 'vitest'

// 模拟 localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
Object.defineProperty(window, 'localStorage', { value: localStorageMock })

// 模拟 sessionStorage
const sessionStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
Object.defineProperty(window, 'sessionStorage', { value: sessionStorageMock })

// 模拟 matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
})

// 模拟 ResizeObserver
global.ResizeObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn()
}))

// 模拟 IntersectionObserver
global.IntersectionObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn()
}))

// 全局组件存根
config.global.stubs = {
  // Element Plus 组件存根
  'el-button': true,
  'el-input': true,
  'el-form': true,
  'el-form-item': true,
  'el-table': true,
  'el-table-column': true,
  'el-pagination': true,
  'el-dialog': true,
  'el-drawer': true,
  'el-select': true,
  'el-option': true,
  'el-date-picker': true,
  'el-message': true,
  'el-message-box': true,
  'el-notification': true,
  // 路由组件存根
  'router-link': true,
  'router-view': true
}

// 全局模拟
config.global.mocks = {
  $t: (key: string) => key, // i18n 模拟
  $router: {
    push: vi.fn(),
    replace: vi.fn(),
    go: vi.fn(),
    back: vi.fn()
  },
  $route: {
    params: {},
    query: {},
    path: '/'
  }
}

// 清理 mock 数据
beforeEach(() => {
  vi.clearAllMocks()
  localStorageMock.getItem.mockClear()
  localStorageMock.setItem.mockClear()
  localStorageMock.removeItem.mockClear()
  localStorageMock.clear.mockClear()
})

// 测试完成后清理
afterEach(() => {
  vi.restoreAllMocks()
})

// 导出工具函数供测试使用
export { localStorageMock, sessionStorageMock }
