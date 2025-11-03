import request from './request'
import type { ApiResponse } from '@/types/api'
import type { LoginUserVO, LoginRequest, RegisterRequest } from '@/types/user'

/**
 * 用户登录
 */
export async function login(data: LoginRequest): Promise<ApiResponse<LoginUserVO>> {
  const response = await request.post<ApiResponse<LoginUserVO>>('/user/login', data)
  return response.data
}

/**
 * 用户注册
 */
export async function register(data: RegisterRequest): Promise<ApiResponse<number>> {
  const response = await request.post<ApiResponse<number>>('/user/register', data)
  return response.data
}

/**
 * 用户注销
 */
export async function logout(): Promise<ApiResponse<boolean>> {
  const response = await request.post<ApiResponse<boolean>>('/user/logout')
  return response.data
}

/**
 * 获取当前登录用户
 */
export async function getLoginUser(): Promise<ApiResponse<LoginUserVO>> {
  const response = await request.get<ApiResponse<LoginUserVO>>('/user/get/login')
  return response.data
}

