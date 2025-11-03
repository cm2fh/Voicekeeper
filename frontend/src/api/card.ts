import request from './request'
import type { ApiResponse } from '@/types/api'
import type { VoiceCard } from '@/types/card'

/**
 * 声音卡片查询请求
 */
export interface VoiceCardQueryRequest {
  current?: number
  pageSize?: number
  id?: number
  userId?: number
  voiceModelId?: number
  cardTitle?: string
  sceneTag?: string
  emotionTag?: string
  aiGenerated?: number
  searchText?: string
  sortField?: string
  sortOrder?: string
}

/**
 * 声音卡片VO（包含关联数据）
 */
export interface VoiceCardVO extends VoiceCard {
  voiceModelName?: string  // 声音模型名称
}

/**
 * 声音卡片新增请求
 */
export interface VoiceCardAddRequest {
  voiceModelId: number
  cardTitle: string
  textContent: string
  sceneTag?: string
  emotionTag?: string
  aiGenerated?: number
}

/**
 * 声音卡片更新请求
 */
export interface VoiceCardUpdateRequest {
  id: number
  cardTitle?: string
  textContent?: string
  sceneTag?: string
  emotionTag?: string
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
 * 创建声音卡片
 */
export async function addVoiceCard(data: VoiceCardAddRequest): Promise<ApiResponse<number>> {
  const response = await request.post<ApiResponse<number>>('/voicecard/add', data)
  return response.data
}

/**
 * 删除声音卡片
 */
export async function deleteVoiceCard(id: number): Promise<ApiResponse<boolean>> {
  const response = await request.post<ApiResponse<boolean>>('/voicecard/delete', { id })
  return response.data
}

/**
 * 更新声音卡片
 */
export async function updateVoiceCard(data: VoiceCardUpdateRequest): Promise<ApiResponse<boolean>> {
  const response = await request.post<ApiResponse<boolean>>('/voicecard/update', data)
  return response.data
}

/**
 * 根据ID获取声音卡片
 */
export async function getVoiceCardById(id: number): Promise<ApiResponse<VoiceCardVO>> {
  const response = await request.get<ApiResponse<VoiceCardVO>>(`/voicecard/get/vo?id=${id}`)
  return response.data
}

/**
 * 分页获取当前用户的声音卡片
 */
export async function listMyVoiceCardsPage(
  query: VoiceCardQueryRequest
): Promise<ApiResponse<PageResponse<VoiceCardVO>>> {
  const response = await request.post<ApiResponse<PageResponse<VoiceCardVO>>>(
    '/voicecard/my/list/page/vo',
    query
  )
  return response.data
}

/**
 * 获取当前用户的所有声音卡片（不分页）
 */
export async function listMyVoiceCards(sceneTag?: string): Promise<ApiResponse<VoiceCardVO[]>> {
  const params = sceneTag ? `?sceneTag=${sceneTag}` : ''
  const response = await request.get<ApiResponse<VoiceCardVO[]>>(`/voicecard/my/list${params}`)
  return response.data
}

/**
 * 增加播放次数
 */
export async function increasePlayCount(id: number): Promise<ApiResponse<boolean>> {
  const response = await request.post<ApiResponse<boolean>>(`/voicecard/play/${id}`)
  return response.data
}

