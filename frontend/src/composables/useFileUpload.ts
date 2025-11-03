import { ref } from 'vue'
import request from '@/api/request'
import type { ApiResponse } from '@/types/api'
import { ElMessage } from 'element-plus'

interface UploadOptions {
  maxSize?: number  // 最大文件大小（字节）
  allowedTypes?: string[]  // 允许的文件类型
  onProgress?: (percent: number) => void
}

export function useFileUpload(biz: string, options: UploadOptions = {}) {
  const {
    maxSize = 52428800,  // 默认50MB
    allowedTypes = ['audio/mpeg', 'audio/wav', 'audio/mp4', 'audio/ogg', 'audio/flac', 'audio/x-m4a'],
    onProgress
  } = options

  const uploading = ref(false)
  const progress = ref(0)

  /**
   * 上传文件
   */
  async function upload(file: File): Promise<string> {
    // 验证文件大小
    if (file.size > maxSize) {
      throw new Error(`文件大小不能超过 ${(maxSize / 1024 / 1024).toFixed(0)}MB`)
    }

    // 验证文件类型
    if (allowedTypes.length > 0 && !allowedTypes.includes(file.type)) {
      throw new Error('不支持的文件类型')
    }

    const formData = new FormData()
    formData.append('file', file)
    formData.append('biz', biz)

    try {
      uploading.value = true
      progress.value = 0

      const res = await request.post<ApiResponse<string>>('/file/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            progress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            onProgress?.(progress.value)
          }
        }
      })

      return res.data.data
    } catch (error: any) {
      ElMessage.error(error.message || '上传失败')
      throw error
    } finally {
      uploading.value = false
    }
  }

  return {
    uploading,
    progress,
    upload
  }
}

