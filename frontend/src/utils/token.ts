// Token管理工具

const TOKEN_KEY = 'access_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 解析JWT Token的payload部分
 * @param token JWT token
 * @returns 解析后的payload对象，如果解析失败返回null
 */
export function parseJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      return null
    }
    // Base64Url解码
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const decoded = atob(payload)
    return JSON.parse(decoded)
  } catch {
    return null
  }
}

/**
 * 检查Token是否已过期
 * @param token JWT token
 * @param bufferSeconds 缓冲时间（秒），在过期前多少秒视为过期，默认60秒
 * @returns true表示已过期或无效，false表示未过期
 */
export function isTokenExpired(token: string | null, bufferSeconds: number = 60): boolean {
  if (!token) {
    return true
  }

  const payload = parseJwtPayload(token)
  if (!payload || typeof payload.exp !== 'number') {
    return true
  }

  // JWT的exp是秒级时间戳
  const expiryTime = payload.exp * 1000
  const currentTime = Date.now()
  const bufferMs = bufferSeconds * 1000

  return currentTime >= (expiryTime - bufferMs)
}

/**
 * 获取Token的剩余有效时间（毫秒）
 * @param token JWT token
 * @returns 剩余时间（毫秒），如果token无效返回0
 */
export function getTokenRemainingTime(token: string | null): number {
  if (!token) {
    return 0
  }

  const payload = parseJwtPayload(token)
  if (!payload || typeof payload.exp !== 'number') {
    return 0
  }

  const expiryTime = payload.exp * 1000
  const remainingTime = expiryTime - Date.now()

  return remainingTime > 0 ? remainingTime : 0
}