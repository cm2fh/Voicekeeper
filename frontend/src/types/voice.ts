/**
 * 声音模型
 */
export interface VoiceModel {
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
  voiceType?: string
  useCount: number
  createTime: string
  updateTime: string
}

/**
 * 声音克隆请求
 */
export interface VoiceCloneRequest {
  userId: number
  audioUrl: string
  modelName: string
  voiceDesc?: string
}

/**
 * 声音模型状态
 */
export type VoiceModelStatus = 0 | 1 | 2 | 3

export const VoiceModelStatusMap: Record<VoiceModelStatus, string> = {
  0: '待训练',
  1: '训练中',
  2: '已完成',
  3: '失败'
}

