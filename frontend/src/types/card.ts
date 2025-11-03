/**
 * 声音卡片
 */
export interface VoiceCard {
  id: number
  userId: number
  voiceModelId: number
  cardTitle?: string
  textContent: string
  aiGenerated: 0 | 1  // 0-否 1-是
  audioUrl?: string
  audioDuration?: number
  audioFileSize?: number
  sceneTag: SceneTag
  emotionTag?: EmotionTag
  playCount: number
  shareCount: number
  lastPlayTime?: string
  createTime: string
  updateTime: string
}

/**
 * 场景标签
 */
export type SceneTag = 'morning' | 'night' | 'encourage' | 'miss' | 'custom'

/**
 * 情感标签
 */
export type EmotionTag = 'warm' | 'gentle' | 'energetic' | 'sad'

/**
 * 场景标签映射
 */
export const SceneTagMap: Record<SceneTag, { label: string; emoji: string; gradient: string }> = {
  morning: { label: '早安问候', emoji: '🌅', gradient: 'var(--gradient-morning)' },
  night: { label: '晚安问候', emoji: '🌙', gradient: 'var(--gradient-night)' },
  encourage: { label: '鼓励支持', emoji: '💪', gradient: 'var(--gradient-encourage)' },
  miss: { label: '表达思念', emoji: '💭', gradient: 'var(--gradient-miss)' },
  custom: { label: '自定义', emoji: '✨', gradient: 'var(--color-primary)' }
}

/**
 * 创建卡片请求
 */
export interface CreateCardRequest {
  userId: number
  voiceModelId: number
  cardTitle?: string
  textContent: string
  audioUrl: string
  sceneTag: SceneTag
  aiGenerated: 0 | 1
}

