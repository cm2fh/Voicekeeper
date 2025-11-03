import { ref, onUnmounted } from 'vue'
import { chatWithAgent } from '@/api/ai'
import { parseSSEMessage } from '@/api/sse'
import type { AIStep } from '@/types/ai'

export function useSSE() {
  const isConnected = ref(false)
  const steps = ref<AIStep[]>([])
  const finalResult = ref<string>('')
  const error = ref<string>('')
  
  let eventSource: EventSource | null = null

  /**
   * 发送消息给 AI Agent
   */
  function sendMessage(message: string, conversationId?: string) {
    // 重置状态
    steps.value = []
    finalResult.value = ''
    error.value = ''
    isConnected.value = true

    eventSource = chatWithAgent(message, conversationId, {
      onMessage: (data) => {
        const parsed = parseSSEMessage(data)

        if (parsed.type === 'step') {
          // 解析步骤信息
          const stepInfo = parseStepInfo(parsed.data)
          if (stepInfo) {
            updateOrAddStep(stepInfo)
          }
        } else if (parsed.type === 'complete') {
          finalResult.value = parsed.data
        } else if (parsed.type === 'error') {
          error.value = parsed.data
        }
      },
      onComplete: () => {
        isConnected.value = false
      },
      onError: (err) => {
        error.value = err.message
        isConnected.value = false
      }
    })
  }

  /**
   * 解析步骤信息
   * 
   * 输入示例：
   * "✓ 查找声音模型"
   * "⏳ 生成文案中..."
   * "⏸️ 合成语音"
   */
  function parseStepInfo(data: string): AIStep | null {
    if (data.includes('✓') || data.includes('✅')) {
      return {
        name: data.replace(/[✓✅]/g, '').trim(),
        status: 'done',
        timestamp: Date.now()
      }
    } else if (data.includes('⏳') || data.includes('⌛')) {
      return {
        name: data.replace(/[⏳⌛]/g, '').trim(),
        status: 'processing',
        timestamp: Date.now()
      }
    } else if (data.includes('❌') || data.includes('错误')) {
      return {
        name: data.replace(/[❌]/g, '').trim(),
        status: 'error',
        timestamp: Date.now()
      }
    } else if (data.includes('⏸️')) {
      return {
        name: data.replace(/⏸️/g, '').trim(),
        status: 'pending',
        timestamp: Date.now()
      }
    }
    return null
  }

  /**
   * 更新或添加步骤
   */
  function updateOrAddStep(step: AIStep) {
    const existingIndex = steps.value.findIndex(s => s.name === step.name)
    if (existingIndex !== -1) {
      // 更新现有步骤
      steps.value[existingIndex] = step
    } else {
      // 添加新步骤
      steps.value.push(step)
    }
  }

  /**
   * 关闭连接
   */
  function close() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
      isConnected.value = false
    }
  }

  // 组件卸载时关闭连接
  onUnmounted(() => {
    close()
  })

  return {
    isConnected,
    steps,
    finalResult,
    error,
    sendMessage,
    close
  }
}

