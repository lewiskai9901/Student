/** Shared helper functions for the exam module */

export const examTypeBadgeClass = (type: number) => {
  const map: Record<number, string> = {
    1: 'bg-blue-50 text-blue-700',
    2: 'bg-indigo-50 text-indigo-700',
    3: 'bg-amber-50 text-amber-700',
    4: 'bg-red-50 text-red-700',
  }
  return map[type] || 'bg-gray-100 text-gray-600'
}

export const statusBadgeClass = (status: number) => {
  const map: Record<number, string> = {
    0: 'bg-gray-100 text-gray-600',
    1: 'bg-emerald-50 text-emerald-700',
    2: 'bg-amber-50 text-amber-700',
    3: 'bg-red-50 text-red-600',
  }
  return map[status] || 'bg-gray-100 text-gray-600'
}

export const getExamTypeName = (type: number) => {
  const names: Record<number, string> = { 1: '期中考试', 2: '期末考试', 3: '补考', 4: '重修考试' }
  return names[type] || '未知'
}

export const getStatusName = (status: number) => {
  const names: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '进行中', 3: '已结束' }
  return names[status] || '未知'
}
