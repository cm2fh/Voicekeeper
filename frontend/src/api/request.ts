import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import type { ApiResponse } from '@/types/api'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const request: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8101/api',  // 后端地址
  timeout: 60000,  // 60秒超时（AI处理可能较慢）
  withCredentials: true  // 携带 cookie（Session 认证）
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // 后端返回的 success 字段为 true 表示成功
    if (res.success) {
      return response
    } else {
      // 业务错误
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  (error) => {
    console.error('响应错误:', error)

    // 处理不同的错误状态码
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401:
          ElMessage.error('未登录或登录已过期')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error(data?.message || '服务器内部错误')
          break
        default:
          ElMessage.error(data?.message || '请求失败')
      }
    } else if (error.request) {
      ElMessage.error('网络错误，请检查网络连接')
    } else {
      ElMessage.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

export default request

