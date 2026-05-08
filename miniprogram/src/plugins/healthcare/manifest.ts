import { definePlugin } from '@core/plugin/package'

export default definePlugin({
  key: 'healthcare',
  label: '医疗',
  schemaVersion: 1,
  minCoreVersion: '0.1.0',
  enabled: (ctx) => ctx.tenantPlugins.includes('healthcare'),
  contributions: [
    { type: 'permission', code: 'healthcare:patient:view', description: '患者档案查看' },
    {
      type: 'menu',
      key: 'healthcare.patients',
      label: '患者档案',
      icon: 'p',
      path: '/plugins/healthcare/pages/index',
      perm: 'healthcare:patient:view',
      order: 10,
      group: 'home-grid'
    },
    {
      type: 'route',
      path: 'plugins/healthcare/pages/index',
      inSubPackage: true,
      perm: 'healthcare:patient:view'
    },
    {
      type: 'scan-resolver',
      prefix: 'PATIENT:',
      resolve: (raw) => ({ path: '/plugins/healthcare/pages/index', params: { id: raw.slice(8) } }),
      priority: 10
    },
    {
      type: 'event',
      eventName: 'healthcare.patient.scanned',
      payloadSchema: {
        type: 'object',
        properties: {
          patientId: { type: 'string' },
          scannedAt: { type: 'number' }
        },
        required: ['patientId']
      }
    }
  ],
  subPackage: { root: 'plugins/healthcare/pages', pages: ['index'] }
})
