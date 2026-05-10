/**
 * F2: workflow API 单测.
 *
 * 验证 URL 拼装、payload 形状、HTTP 方法.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

import { workflowApi } from '@/api/workflow'
import { http } from '@/utils/request'

describe('workflowApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('流程定义', () => {
    it('listDefinitions GET /workflow/process-definitions', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await workflowApi.listDefinitions()
      expect(http.get).toHaveBeenCalledWith('/workflow/process-definitions')
    })

    it('deploy 用 multipart/form-data POST', async () => {
      vi.mocked(http.post).mockResolvedValueOnce({} as any)
      const file = new File(['<bpmn/>'], 'test.bpmn')
      await workflowApi.deploy(file, 'My-Process')
      expect(http.post).toHaveBeenCalledTimes(1)
      const [url, formData, opts] = vi.mocked(http.post).mock.calls[0]
      expect(url).toBe('/workflow/process-definitions/deploy')
      expect(formData).toBeInstanceOf(FormData)
      expect((opts as any)?.headers?.['Content-Type']).toBe('multipart/form-data')
    })

    it('deploy 不传 name 时不附加 name 字段', async () => {
      vi.mocked(http.post).mockResolvedValueOnce({} as any)
      const file = new File(['<bpmn/>'], 'p.bpmn')
      await workflowApi.deploy(file)
      const formData = vi.mocked(http.post).mock.calls[0][1] as FormData
      expect(formData.get('file')).toBeInstanceOf(File)
      expect(formData.get('name')).toBeNull()
    })

    it('deleteDeployment 默认 cascade=false', async () => {
      vi.mocked(http.delete).mockResolvedValueOnce(undefined)
      await workflowApi.deleteDeployment('dep-1')
      expect(http.delete).toHaveBeenCalledWith('/workflow/process-definitions/deployments/dep-1?cascade=false')
    })

    it('deleteDeployment cascade=true', async () => {
      vi.mocked(http.delete).mockResolvedValueOnce(undefined)
      await workflowApi.deleteDeployment('dep-1', true)
      expect(http.delete).toHaveBeenCalledWith('/workflow/process-definitions/deployments/dep-1?cascade=true')
    })

    it('suspendDefinition POST .../suspend', async () => {
      vi.mocked(http.post).mockResolvedValueOnce(undefined)
      await workflowApi.suspendDefinition('def-1')
      expect(http.post).toHaveBeenCalledWith('/workflow/process-definitions/def-1/suspend')
    })

    it('activateDefinition POST .../activate', async () => {
      vi.mocked(http.post).mockResolvedValueOnce(undefined)
      await workflowApi.activateDefinition('def-1')
      expect(http.post).toHaveBeenCalledWith('/workflow/process-definitions/def-1/activate')
    })
  })

  describe('流程实例', () => {
    it('listInstances 透传 params', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await workflowApi.listInstances({ processKey: 'k1', businessKey: 'biz' })
      expect(http.get).toHaveBeenCalledWith('/workflow/process-instances', {
        params: { processKey: 'k1', businessKey: 'biz' },
      })
    })

    it('listInstances 无参数也调用', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await workflowApi.listInstances()
      expect(http.get).toHaveBeenCalledWith('/workflow/process-instances', { params: undefined })
    })

    it('startInstance POST 含 variables', async () => {
      vi.mocked(http.post).mockResolvedValueOnce({} as any)
      const body = { processKey: 'p', businessKey: 'b', variables: { x: 1 } }
      await workflowApi.startInstance(body)
      expect(http.post).toHaveBeenCalledWith('/workflow/process-instances/start', body)
    })

    it('cancelInstance 携带 reason', async () => {
      vi.mocked(http.post).mockResolvedValueOnce(undefined)
      await workflowApi.cancelInstance('inst-1', '过期')
      expect(http.post).toHaveBeenCalledWith('/workflow/process-instances/inst-1/cancel', { reason: '过期' })
    })
  })

  describe('我的待办', () => {
    it('myTasks GET /workflow/tasks/mine', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await workflowApi.myTasks()
      expect(http.get).toHaveBeenCalledWith('/workflow/tasks/mine')
    })

    it('claimTask POST .../claim', async () => {
      vi.mocked(http.post).mockResolvedValueOnce(undefined)
      await workflowApi.claimTask('t-1')
      expect(http.post).toHaveBeenCalledWith('/workflow/tasks/t-1/claim')
    })

    it('unclaimTask POST .../unclaim', async () => {
      vi.mocked(http.post).mockResolvedValueOnce(undefined)
      await workflowApi.unclaimTask('t-1')
      expect(http.post).toHaveBeenCalledWith('/workflow/tasks/t-1/unclaim')
    })

    it('completeTask POST 携带 variables', async () => {
      vi.mocked(http.post).mockResolvedValueOnce(undefined)
      await workflowApi.completeTask('t-1', { result: 'ok' })
      expect(http.post).toHaveBeenCalledWith('/workflow/tasks/t-1/complete', { variables: { result: 'ok' } })
    })

    it('completeTask 无 variables 也带 {variables: undefined}', async () => {
      vi.mocked(http.post).mockResolvedValueOnce(undefined)
      await workflowApi.completeTask('t-1')
      expect(http.post).toHaveBeenCalledWith('/workflow/tasks/t-1/complete', { variables: undefined })
    })

    it('delegateTask POST 携带 assignee', async () => {
      vi.mocked(http.post).mockResolvedValueOnce(undefined)
      await workflowApi.delegateTask('t-1', 'bob')
      expect(http.post).toHaveBeenCalledWith('/workflow/tasks/t-1/delegate', { assignee: 'bob' })
    })
  })

  describe('历史', () => {
    it('historicalInstances 透传 params', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await workflowApi.historicalInstances({ limit: 10 })
      expect(http.get).toHaveBeenCalledWith('/workflow/history/instances', { params: { limit: 10 } })
    })

    it('instanceTaskHistory URL 拼接', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await workflowApi.instanceTaskHistory('i-1')
      expect(http.get).toHaveBeenCalledWith('/workflow/history/instances/i-1/tasks')
    })

    it('myHistoricalTasks 默认 limit=50', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await workflowApi.myHistoricalTasks()
      expect(http.get).toHaveBeenCalledWith('/workflow/history/my-tasks?limit=50')
    })

    it('myHistoricalTasks 自定义 limit', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await workflowApi.myHistoricalTasks(200)
      expect(http.get).toHaveBeenCalledWith('/workflow/history/my-tasks?limit=200')
    })
  })
})
