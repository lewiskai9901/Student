/** Shared helper functions for the grade module */

export const getGradeTypeName = (type: number) => {
  const names: Record<number, string> = { 1: '平时成绩', 2: '期中成绩', 3: '期末成绩', 4: '总评成绩' }
  return names[type] || '未知'
}

export const getGradeTypeTag = (type: number): 'primary' | 'success' | 'warning' | 'danger' | 'info' => {
  const types: Record<number, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    1: 'info',
    2: 'warning',
    3: 'primary',
    4: 'success',
  }
  return types[type] || 'info'
}

export const getStatusName = (status: number) => {
  const names: Record<number, string> = { 0: '草稿', 1: '已提交', 2: '已审核', 3: '已发布' }
  return names[status] || '未知'
}

export const getStatusTag = (status: number): 'primary' | 'success' | 'warning' | 'danger' | 'info' => {
  const types: Record<number, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'info',
    1: 'warning',
    2: 'primary',
    3: 'success',
  }
  return types[status] || 'info'
}

export const getGradeLevelTag = (level: string) => {
  if (level.startsWith('A')) return 'success'
  if (level.startsWith('B')) return ''
  if (level.startsWith('C')) return 'warning'
  if (level.startsWith('D')) return 'danger'
  return 'info'
}
