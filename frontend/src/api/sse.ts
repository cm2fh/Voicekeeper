import type { SSEMessage } from '@/types/ai'

/**
 * SSE 连接配置
 */
export interface SSEConfig {
  onMessage: (message: string, event?: MessageEvent) => void
  onComplete?: () => void
  onError?: (error: Error) => void
}

/**
 * 创建 SSE 连接
 */
export function createSSE(url: string, config: SSEConfig): EventSource {
  console.log('创建SSE连接:', url)
  
  const eventSource = new EventSource(url, { withCredentials: true })

  eventSource.onopen = () => {
    console.log('SSE连接已建立')
  }

  eventSource.onmessage = (event) => {
    console.log('收到SSE消息:', event.data)
    const data = event.data
    config.onMessage(data, event)
  }

  // 监听conversationId事件
  eventSource.addEventListener('conversationId', (event) => {
    console.log('收到conversationId:', event.data)
    config.onMessage(event.data, event as MessageEvent)
  })

  eventSource.onerror = (error) => {
    console.error('SSE 连接错误:', error)
    console.log('EventSource readyState:', eventSource.readyState)
    
    // readyState: 0=CONNECTING, 1=OPEN, 2=CLOSED
    if (eventSource.readyState === EventSource.CLOSED) {
      console.log('SSE连接已被服务器关闭（正常）')
      // 不调用onError，这是正常关闭
      return
    } else if (eventSource.readyState === EventSource.CONNECTING) {
      console.log('SSE正在重连...')
      // 不关闭，让它自动重连
      return
    }
    
    // 只有在OPEN状态下出错才是真正的错误
    eventSource.close()
    config.onError?.(new Error('SSE连接中断'))
  }

  // 监听后端发送的done事件（正常完成）
  eventSource.addEventListener('done', (event) => {
    console.log('收到完成信号:', event.data)
    eventSource.close()
    config.onComplete?.()
  })

  // 兼容：监听自定义complete事件
  eventSource.addEventListener('complete', () => {
    console.log('收到完成事件')
    eventSource.close()
    config.onComplete?.()
  })

  return eventSource
}

/**
 * 解析 SSE 消息
 * 
 * 后端返回的消息格式：
 * "Step 1: 查找声音模型 ✓"
 * "Step 2: ⏳ 生成文案中..."
 * "执行完成: 卡片ID: 123"
 */
export function parseSSEMessage(message: string): SSEMessage {
  // 判断是否是步骤消息
  if (message.startsWith('Step')) {
    return {
      type: 'step',
      data: message.replace(/Step \d+: /, '')
    }
  }

  // 判断是否是完成消息
  if (message.includes('执行完成') || message.includes('完成')) {
    return {
      type: 'complete',
      data: message
    }
  }

  // 判断是否是错误消息
  if (message.includes('错误') || message.includes('失败')) {
    return {
      type: 'error',
      data: message
    }
  }

  return {
    type: 'step',
    data: message
  }
}

