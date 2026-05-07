export function genderLabel(g?: number): string {
  if (g === 1) return '男'
  if (g === 2) return '女'
  return '-'
}

export function statusLabel(s?: number, fallback?: string): string {
  if (fallback) return fallback
  return s != null ? String(s) : '-'
}

export function formatDate(s?: string): string {
  if (!s) return '-'
  const m = s.match(/^(\d{4}-\d{2}-\d{2})/)
  return m ? m[1] : s
}
