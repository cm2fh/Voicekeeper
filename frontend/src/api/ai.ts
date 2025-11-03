import { createSSE, type SSEConfig } from './sse'

const BASE_URL = 'http://localhost:8101/api'

/**
 * 与 AI Agent 对话（SSE 流式返回）
 * 
 * @param message 用户消息
 * @param conversationId 会话ID（可选，不传则自动生成）
 * @param config SSE 配置
 * @returns EventSource 实例（可用于关闭连接）
 */
export function chatWithAgent(
  message: string,
  conversationId: string | undefined,
  config: SSEConfig
): EventSource {
  const params = new URLSearchParams({
    message,
    ...(conversationId && { conversationId })
  })

  const url = `${BASE_URL}/ai/agent/chat?${params.toString()}`
  return createSSE(url, config)
}

/**
 * 清理会话
 */
export async function clearConversation(conversationId: string): Promise<any> {
  const response = await fetch(`${BASE_URL}/conversation/clear?conversationId=${conversationId}`)
  return response.json()
}

/**
 * 获取会话信息
 */
export async function getConversationInfo(conversationId: string): Promise<any> {
  const response = await fetch(`${BASE_URL}/conversation/info?conversationId=${conversationId}`)
  return response.json()
}

