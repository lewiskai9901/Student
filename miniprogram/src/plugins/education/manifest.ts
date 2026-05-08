import { definePlugin } from '@core/plugin/package'

export default definePlugin({
  key: 'education',
  label: '教育',
  schemaVersion: 1,
  minCoreVersion: '0.1.0',
  enabled: (ctx) => ctx.tenantPlugins.includes('EDU'),
  contributions: [
    // ===== Permissions (5) =====
    { type: 'permission', code: 'student:info:view',  description: '查看学生列表' },
    { type: 'permission', code: 'student:info:view',  description: '查看学生详情' },
    { type: 'permission', code: 'student:class:view',    description: '查看班级列表' },
    { type: 'permission', code: 'student:class:view',    description: '查看班级详情' },
    { type: 'permission', code: 'my:user_student:view', description: '查看我管理的班级' },

    // ===== Menus (3, all home-grid) =====
    {
      type: 'menu', key: 'education.students', label: '学生', icon: 'S',
      path: '/plugins/education/pages/students', perm: 'student:info:view',
      order: 40, group: 'home-grid'
    },
    {
      type: 'menu', key: 'education.classes', label: '班级', icon: 'C',
      path: '/plugins/education/pages/classes', perm: 'student:class:view',
      order: 41, group: 'home-grid'
    },
    {
      type: 'menu', key: 'education.my-class', label: '我的班级', icon: 'M',
      path: '/plugins/education/pages/my-class', perm: 'my:user_student:view',
      order: 39, group: 'home-grid'
    },

    // ===== Routes (5) =====
    { type: 'route', path: 'plugins/education/pages/students',       inSubPackage: true, perm: 'student:info:view' },
    { type: 'route', path: 'plugins/education/pages/student-detail', inSubPackage: true, perm: 'student:info:view' },
    { type: 'route', path: 'plugins/education/pages/classes',        inSubPackage: true, perm: 'student:class:view' },
    { type: 'route', path: 'plugins/education/pages/class-detail',   inSubPackage: true, perm: 'student:class:view' },
    { type: 'route', path: 'plugins/education/pages/my-class',       inSubPackage: true, perm: 'my:user_student:view' }
  ],
  subPackage: {
    root: 'plugins/education/pages',
    pages: ['students', 'student-detail', 'classes', 'class-detail', 'my-class']
  }
})
