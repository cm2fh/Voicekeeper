import request from './request'
import type { ApiResponse } from '@/types/api'

/**
 * 声音模型VO
 */
export interface VoiceModelVO {
  id: number
  userId: number
  modelName: string
  voiceDesc?: string
  sampleAudioUrl: string
  sampleDuration?: number
  sampleFileSize?: number
  aiModelId?: string
  aiProvider?: string
  trainingStatus: number  // 0-待训练 1-训练中 2-成功 3-失败
  trainingMessage?: string
  qualityScore?: number
  voiceType?: string  // male/female/child
  useCount: number
  createTime: string
  updateTime: string
}

/**
 * 声音模型查询请求
 */
export interface VoiceModelQueryRequest {
  current?: number
  pageSize?: number
  id?: number
  userId?: number
  modelName?: string
  trainingStatus?: number
  voiceType?: string
}

/**
 * 声音模型新增请求
 */
export interface VoiceModelAddRequest {
  modelName: string
  voiceDesc?: string
  sampleAudioUrl: string
  sampleDuration?: number
  sampleFileSize?: number
  voiceType?: string
}

/**
 * 声音模型更新请求
 */
export interface VoiceModelUpdateRequest {
  id: number
  modelName?: string
  voiceDesc?: string
  voiceType?: string
}

/**
 * 分页响应
 */
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 创建声音模型
 */
export async function addVoiceModel(data: VoiceModelAddRequest): Promise<ApiResponse<number>> {
  const response = await request.post<ApiResponse<number>>('/voicemodel/add', data)
  return response.data
}

/**
 * 删除声音模型
 */
export async function deleteVoiceModel(id: number): Promise<ApiResponse<boolean>> {
  const response = await request.post<ApiResponse<boolean>>('/voicemodel/delete', { id })
  return response.data
}

/**
 * 更新声音模型
 */
export async function updateVoiceModel(data: VoiceModelUpdateRequest): Promise<ApiResponse<boolean>> {
  const response = await request.post<ApiResponse<boolean>>('/voicemodel/update', data)
  return response.data
}

/**
 * 根据ID获取声音模型
 */
export async function getVoiceModelById(id: number): Promise<ApiResponse<VoiceModelVO>> {
  const response = await request.get<ApiResponse<VoiceModelVO>>(`/voicemodel/get/vo?id=${id}`)
  return response.data
}

/**
 * 分页获取当前用户的声音模型
 */
export async function listMyVoiceModelsPage(
  query: VoiceModelQueryRequest
): Promise<ApiResponse<PageResponse<VoiceModelVO>>> {
  const response = await request.post<ApiResponse<PageResponse<VoiceModelVO>>>(
    '/voicemodel/my/list/page/vo',
    query
  )
  return response.data
}

/**
 * 获取当前用户的所有声音模型（不分页）
 */
export async function listMyVoiceModels(): Promise<ApiResponse<VoiceModelVO[]>> {
  const response = await request.get<ApiResponse<VoiceModelVO[]>>('/voicemodel/my/list')
  return response.data
}

