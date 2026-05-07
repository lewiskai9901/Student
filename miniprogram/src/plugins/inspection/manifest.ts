import { definePlugin } from '@core/plugin/package'

export default definePlugin({
  key: 'inspection',
  label: '检查',
  schemaVersion: 1,
  minCoreVersion: '0.1.0',
  enabled: (ctx) => ctx.tenantPlugins.includes('inspection'),
  contributions: [
    // ===== Permissions (12) =====
    { type: 'permission', code: 'inspection:task:view',       description: '查看检查任务详情' },
    { type: 'permission', code: 'inspection:task:list',       description: '查看我的检查任务列表' },
    { type: 'permission', code: 'inspection:task:claim',      description: '认领可领取任务' },
    { type: 'permission', code: 'inspection:task:start',      description: '开始执行任务' },
    { type: 'permission', code: 'inspection:task:submit',     description: '提交任务结果' },
    { type: 'permission', code: 'inspection:correction:view', description: '查看整改单详情' },
    { type: 'permission', code: 'inspection:correction:list', description: '查看我的整改单列表' },
    { type: 'permission', code: 'inspection:correction:process', description: '处理整改单(开始 / 阻塞)' },
    { type: 'permission', code: 'inspection:correction:submit',  description: '提交整改完成' },
    { type: 'permission', code: 'inspection:appeal:view',     description: '查看申诉详情' },
    { type: 'permission', code: 'inspection:appeal:list',     description: '查看我的申诉列表' },
    { type: 'permission', code: 'inspection:appeal:submit',   description: '提交评分申诉' },

    // ===== Menus (5, all home-grid) =====
    {
      type: 'menu', key: 'inspection.my-tasks', label: '我的任务', icon: 't',
      path: '/plugins/inspection/pages/my-tasks', perm: 'inspection:task:list',
      order: 10, group: 'home-grid'
    },
    {
      type: 'menu', key: 'inspection.scan', label: '扫码检查', icon: 's',
      path: '/plugins/inspection/pages/scan-landing', perm: 'inspection:task:view',
      order: 11, group: 'home-grid'
    },
    {
      type: 'menu', key: 'inspection.available', label: '可领任务', icon: 'a',
      path: '/plugins/inspection/pages/available-tasks', perm: 'inspection:task:list',
      order: 12, group: 'home-grid'
    },
    {
      type: 'menu', key: 'inspection.my-corrections', label: '我的整改', icon: 'c',
      path: '/plugins/inspection/pages/my-corrections', perm: 'inspection:correction:list',
      order: 20, group: 'home-grid'
    },
    {
      type: 'menu', key: 'inspection.my-appeals', label: '我的申诉', icon: 'r',
      path: '/plugins/inspection/pages/my-appeals', perm: 'inspection:appeal:list',
      order: 30, group: 'home-grid'
    },

    // ===== Routes (7) =====
    { type: 'route', path: 'plugins/inspection/pages/my-tasks',          inSubPackage: true, perm: 'inspection:task:list' },
    { type: 'route', path: 'plugins/inspection/pages/task-detail',       inSubPackage: true, perm: 'inspection:task:view' },
    { type: 'route', path: 'plugins/inspection/pages/scan-landing',      inSubPackage: true, perm: 'inspection:task:view' },
    { type: 'route', path: 'plugins/inspection/pages/available-tasks',   inSubPackage: true, perm: 'inspection:task:list' },
    { type: 'route', path: 'plugins/inspection/pages/my-corrections',    inSubPackage: true, perm: 'inspection:correction:list' },
    { type: 'route', path: 'plugins/inspection/pages/correction-detail', inSubPackage: true, perm: 'inspection:correction:view' },
    { type: 'route', path: 'plugins/inspection/pages/my-appeals',        inSubPackage: true, perm: 'inspection:appeal:list' },
    { type: 'route', path: 'plugins/inspection/pages/submission-detail', inSubPackage: true, perm: 'inspection:task:submit' },

    // ===== Scan Resolver (1) =====
    {
      type: 'scan-resolver',
      prefix: 'INSPECTION:TASK:',
      resolve: (raw) => {
        const id = raw.slice('INSPECTION:TASK:'.length).trim()
        if (!id) return null
        return { path: '/plugins/inspection/pages/task-detail', params: { id } }
      },
      priority: 10
    },

    // ===== Events (3) =====
    {
      type: 'event',
      eventName: 'inspection.task.submitted',
      payloadSchema: {
        type: 'object',
        properties: { taskId: { type: 'number' }, submitterId: { type: 'number' } },
        required: ['taskId']
      }
    },
    {
      type: 'event',
      eventName: 'inspection.case.processed',
      payloadSchema: {
        type: 'object',
        properties: { caseId: { type: 'number' }, action: { type: 'string' } },
        required: ['caseId']
      }
    },
    {
      type: 'event',
      eventName: 'inspection.appeal.created',
      payloadSchema: {
        type: 'object',
        properties: { appealId: { type: 'number' }, submissionDetailId: { type: 'number' } },
        required: ['appealId']
      }
    }
  ],
  subPackage: {
    root: 'plugins/inspection',
    pages: [
      'pages/my-tasks',
      'pages/task-detail',
      'pages/scan-landing',
      'pages/available-tasks',
      'pages/my-corrections',
      'pages/correction-detail',
      'pages/my-appeals',
      'pages/submission-detail'
    ]
  }
})
