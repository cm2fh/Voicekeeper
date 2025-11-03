/**
 * API 统一响应格式
 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  success: boolean
}

/**
 * 分页响应
 */
export interface PageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
}

/**
 * 分页请求参数
 */
export interface PageRequest {
  current: number
  pageSize: number
  sortField?: string
  sortOrder?: 'ascend' | 'descend'
}

