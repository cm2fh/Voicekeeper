/**
 * 用户信息
 */
export interface User {
  id: number
  userNo: string
  username: string
  realName?: string
  phone?: string
  email?: string
  avatar?: string
  gender?: 0 | 1 | 2  // 0-未知 1-男 2-女
  birthday?: string
  role: 'user' | 'admin'
  status: 0 | 1  // 0-禁用 1-正常
  registerTime: string
  lastLoginTime?: string
  createTime: string
  updateTime: string
}

/**
 * 登录用户信息（脱敏）
 */
export interface LoginUserVO extends User {}

/**
 * 登录请求
 */
export interface LoginRequest {
  username: string
  password: string
}

/**
 * 注册请求
 */
export interface RegisterRequest {
  username: string
  password: string
  checkPassword: string
}

