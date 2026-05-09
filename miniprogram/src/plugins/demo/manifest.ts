import { definePlugin } from '@core/plugin/package'

// V4 (cross-plugin event) test probe — module-level audit log readable by integration tests.
// Underscore prefix marks it as test-only public surface.
export const _crossPluginAuditLog: Array<{ event: string; payload: unknown }> = []

export default definePlugin({
  key: 'demo',
  label: '演示',
  schemaVersion: 1,
  minCoreVersion: '0.1.0',
  contributions: [
    { type: 'permission', code: 'demo:hello:view', description: 'Demo 页面访问' },
    {
      type: 'menu', key: 'demo.hello', label: 'Hello', icon: 'h',
      path: '/plugins/demo/pages/hello', perm: 'demo:hello:view',
      order: 1, group: 'home-grid'
    },
    { type: 'route', path: 'plugins/demo/pages/hello', inSubPackage: true, perm: 'demo:hello:view' },
    { type: 'event', eventName: 'demo.hello.clicked', payloadSchema: { type: 'object' } }
  ],
  bootstrap: (ctx) => {
    ctx.bus.on('inspection.task.submitted', (payload) => {
      _crossPluginAuditLog.push({ event: 'inspection.task.submitted', payload })
    })
  },
  subPackage: { root: 'plugins/demo/pages', pages: ['hello'] }
})
