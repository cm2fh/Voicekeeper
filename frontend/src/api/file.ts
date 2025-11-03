import request from './request'
import type { ApiResponse } from '@/types/api'

/**
 * 文件上传业务类型
 */
export type FileBizType = 'user_avatar' | 'voice_sample' | 'voice_card' | 'voice_generated'

/**
 * 上传文件
 * @param file 文件对象
 * @param biz 业务类型
 * @returns OSS 公网访问地址
 */
export async function uploadFile(
  file: File,
  biz: FileBizType
): Promise<ApiResponse<string>> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('biz', biz)

  const response = await request.post<ApiResponse<string>>('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
  
  return response.data
}

