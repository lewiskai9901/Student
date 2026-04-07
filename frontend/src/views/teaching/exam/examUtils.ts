/** Shared helper functions for the exam module */

export const examTypeChipClass = (type: number) => {
  const map: Record<number, string> = {
    1: 'tm-chip-blue',
    2: 'tm-chip-purple',
    3: 'tm-chip-amber',
    4: 'tm-chip-red',
  }
  return map[type] || 'tm-chip-gray'
}

export const statusChipClass = (status: number) => {
  const map: Record<number, string> = {
    0: 'tm-chip-gray',
    1: 'tm-chip-green',
    2: 'tm-chip-amber',
    3: 'tm-chip-red',
  }
  return map[status] || 'tm-chip-gray'
}

export const getExamTypeName = (type: number) => {
  const names: Record<number, string> = { 1: '期中考试', 2: '期末考试', 3: '补考', 4: '重修考试' }
  return names[type] || '未知'
}

export const getStatusName = (status: number) => {
  const names: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '进行中', 3: '已结束' }
  return names[status] || '未知'
}
