import { definePlugin } from '@core/plugin/package'

export default definePlugin({
  key: 'education',
  label: '教育',
  schemaVersion: 1,
  minCoreVersion: '0.1.0',
  enabled: (ctx) => ctx.tenantPlugins.includes('EDU'),
  contributions: [
    // ===== Permissions (4) =====
    { type: 'permission', code: 'education:student:list', description: '查看学生列表' },
    { type: 'permission', code: 'education:student:view', description: '查看学生详情' },
    { type: 'permission', code: 'education:class:list',   description: '查看班级列表' },
    { type: 'permission', code: 'education:class:view',   description: '查看班级详情' },

    // ===== Menus (2, all home-grid) =====
    {
      type: 'menu', key: 'education.students', label: '学生', icon: 'S',
      path: '/plugins/education/pages/students', perm: 'education:student:list',
      order: 40, group: 'home-grid'
    },
    {
      type: 'menu', key: 'education.classes', label: '班级', icon: 'C',
      path: '/plugins/education/pages/classes', perm: 'education:class:list',
      order: 41, group: 'home-grid'
    },

    // ===== Routes (4) =====
    { type: 'route', path: 'plugins/education/pages/students',       inSubPackage: true, perm: 'education:student:list' },
    { type: 'route', path: 'plugins/education/pages/student-detail', inSubPackage: true, perm: 'education:student:view' },
    { type: 'route', path: 'plugins/education/pages/classes',        inSubPackage: true, perm: 'education:class:list' },
    { type: 'route', path: 'plugins/education/pages/class-detail',   inSubPackage: true, perm: 'education:class:view' }
  ],
  subPackage: {
    root: 'plugins/education',
    pages: ['pages/students', 'pages/student-detail', 'pages/classes', 'pages/class-detail']
  }
})
