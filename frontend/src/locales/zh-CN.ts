/**
 * 中文 (默认) 翻译表 — 嵌套结构按模块组织.
 *
 * 添加新 key 优先来此, 然后同步到 en-US.ts. 缺英文翻译时自动回退到中文.
 */
export default {
  common: {
    confirm: '确认',
    cancel: '取消',
    save: '保存',
    delete: '删除',
    edit: '编辑',
    add: '新增',
    refresh: '刷新',
    loading: '加载中...',
    empty: '暂无数据',
    success: '操作成功',
    error: '操作失败',
    backToHome: '返回首页',
    confirmDelete: '确认删除?',
  },
  inspection: {
    project: '检查项目',
    task: '检查任务',
    template: '检查模板',
    submission: '提交记录',
    correctiveCase: '整改单',
    score: '得分',
    pass: '通过',
    fail: '不通过',
  },
  corrective: {
    candidates: '整改候选',
    confirmAll: '全部确认',
    skipAll: '全部跳过',
    severityHigh: '高',
    severityMedium: '中',
    severityLow: '低',
    severityNone: '不触发',
    sourceEngine: '系统建议',
    sourceManual: '人工',
    strictnessStrict: '严格',
    strictnessNormal: '标准',
    strictnessLenient: '宽松',
    strictnessOff: '关闭',
  },
  received: {
    title: '我的受检中心',
    inspections: '我被检查的记录',
    trends: '检查趋势',
    recurring: '高频问题',
    summary: '统计概览',
  },
  theme: {
    light: '浅色',
    dark: '深色',
    system: '跟随系统',
  },
}
