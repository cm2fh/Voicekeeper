/**
 * AI 执行步骤
 */
export interface AIStep {
  name: string
  status: 'pending' | 'processing' | 'done' | 'error'
  message?: string
  timestamp?: number
}

/**
 * 对话消息
 */
export interface ConversationMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
  steps?: AIStep[]  // AI消息才有步骤
}

/**
 * SSE 消息类型
 */
export interface SSEMessage {
  type: 'step' | 'complete' | 'error'
  data: string
}

/**
 * AI 对话请求
 */
export interface AIConversationRequest {
  message: string
  conversationId?: string
}

