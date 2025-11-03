<template>
  <MainLayout>
    <div class="home-page">
      <!-- 欢迎区域 -->
      <div class="welcome-section">
        <div class="welcome-content">
          <div>
            <h2 class="welcome-title">你好，{{ userStore.username }} 👋</h2>
            <p class="welcome-subtitle">想为谁创建一张声音卡片？</p>
          </div>
          <el-button 
            v-if="messages.length > 0"
            type="primary" 
            plain
            @click="handleNewConversation"
            class="new-conversation-btn"
          >
            <el-icon><Plus /></el-icon>
            新建对话
          </el-button>
        </div>
      </div>

      <!-- 对话式输入区域（核心交互） -->
      <div class="conversation-section">
        <div class="conversation-container">
          <!-- 对话历史 -->
          <div v-if="messages.length > 0" class="messages-list">
            <div
              v-for="(msg, index) in messages"
              :key="index"
              class="message-item"
              :class="msg.role"
            >
              <div v-if="msg.role === 'user'" class="user-message">
                <div class="message-bubble">{{ msg.content }}</div>
              </div>
              <div v-else class="ai-message">
                <div class="message-avatar">🤖</div>
                <div class="message-bubble">
                  <!-- AI思考中提示 -->
                  <div v-if="!msg.content && msg.steps && msg.steps.length > 0" class="ai-thinking">
                    <span class="thinking-text">思考中...</span>
                  </div>
                  <!-- AI响应内容（排除所有卡片场景） -->
                  <div v-if="msg.content && !isCardList(msg.content) && !hasSingleCard(msg.content)" class="ai-response">
                    <span>{{ msg.content }}</span>
                  </div>
                  
                  <!-- 单张卡片场景（包括创建成功和查询结果）：简洁提示 + 音频播放器 -->
                  <div v-if="hasSingleCard(msg.content)" class="single-card-response">
                    <!-- 卡片前的引导文字 -->
                    <div v-if="getSingleCardIntro(msg.content)" class="card-intro">
                      {{ getSingleCardIntro(msg.content) }}
                    </div>
                    
                    <!-- 音频播放器 -->
                    <div v-if="msg.audioUrl" class="audio-player-card">
                      <div class="audio-title">
                        <el-icon><Headset /></el-icon>
                        <span>{{ msg.cardTitle || '语音卡片' }}</span>
                      </div>
                      <audio controls :src="msg.audioUrl" class="audio-player">
                        您的浏览器不支持音频播放
                      </audio>
                      <div v-if="msg.cardId" class="card-actions">
                        <el-button type="primary" size="small" @click="goToCardDetail(msg.cardId)">
                          <el-icon><View /></el-icon>
                          查看卡片详情
                        </el-button>
                      </div>
                    </div>
                    
                    <!-- 卡片后的引导文字 -->
                    <div v-if="getSingleCardOutro(msg.content)" class="card-outro">
                      {{ getSingleCardOutro(msg.content) }}
                    </div>
                  </div>
                  
                  <!-- 卡片列表场景：解析并渲染每张卡片 -->
                  <div v-if="isCardList(msg.content)" class="card-list-response">
                    <!-- 引导文字 -->
                    <div v-if="getCardListIntro(msg.content)" class="card-list-intro">
                      {{ getCardListIntro(msg.content) }}
                    </div>
                    
                    <!-- 卡片列表 -->
                    <div class="cards-grid">
                      <div 
                        v-for="(card, idx) in parseCardList(msg.content)" 
                        :key="idx"
                        class="card-item"
                      >
                        <div class="card-header">
                          <span class="card-number">{{ idx + 1 }}</span>
                          <span class="card-title">{{ card.title }}</span>
                        </div>
                        <audio v-if="card.audioUrl" controls :src="card.audioUrl" class="card-audio">
                          您的浏览器不支持音频播放
                        </audio>
                        <div class="card-footer">
                          <span class="card-id">ID: {{ card.cardId }}</span>
                          <el-button 
                            v-if="card.cardId" 
                            type="primary" 
                            size="small" 
                            text
                            @click="goToCardDetail(card.cardId)"
                          >
                            查看详情
                          </el-button>
                        </div>
                      </div>
                    </div>
                    
                    <!-- 结尾引导 -->
                    <div v-if="getCardListOutro(msg.content)" class="card-list-outro">
                      {{ getCardListOutro(msg.content) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- AI 头像和引导语（首次） -->
          <div v-else class="ai-greeting">
            <div class="ai-avatar">🤖</div>
            <div class="greeting-bubble glass">
              <p>我是 VoiceKeeper AI 助手，我可以帮你：</p>
              <ul>
                <li>🎙️ 克隆声音模型</li>
                <li>✨ 创建个性化声音卡片</li>
                <li>🔍 搜索和管理你的卡片</li>
              </ul>
              <p class="hint-text">试试说："用妈妈的声音做一张晚安卡片"</p>
            </div>
          </div>

          <!-- 对话输入框 -->
          <div class="input-section">
            <div 
              class="input-wrapper"
              :class="{ 'is-dragover': isDragoverChat }"
              @drop.prevent="handleChatDrop"
              @dragover.prevent="isDragoverChat = true"
              @dragleave.prevent="isDragoverChat = false"
            >
              <!-- 上传的文件预览 -->
              <div v-if="uploadedFile" class="uploaded-file-preview">
                <div class="file-info">
                  <el-icon class="file-icon"><Document /></el-icon>
                  <span class="file-name">{{ uploadedFile.name }}</span>
                  <span class="file-size">{{ formatFileSize(uploadedFile.size) }}</span>
                </div>
                <el-button circle size="small" @click="removeUploadedFile">
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>

              <el-input
                v-model="userInput"
                type="textarea"
                :rows="3"
                placeholder="告诉我你想做什么... 例如：用爸爸的声音创建一张早安问候卡片"
                resize="none"
                class="conversation-input"
                :disabled="isProcessing"
                @keydown.ctrl.enter="() => handleSendMessage()"
              />
              <div class="input-actions">
                <div class="left-actions">
                  <!-- 文件上传按钮 -->
                  <el-button 
                    circle 
                    size="small"
                    @click="triggerFileUpload"
                    title="上传音频文件"
                  >
                    <el-icon><Paperclip /></el-icon>
                  </el-button>
                  <div class="input-hint">
                    <el-icon><InfoFilled /></el-icon>
                    <span>Ctrl+Enter发送 | 支持拖拽音频</span>
                  </div>
                </div>
                <el-button
                  type="primary"
                  class="send-btn"
                  :disabled="(!userInput.trim() && !uploadedFile) || isProcessing"
                  :loading="isProcessing"
                  @click="() => handleSendMessage()"
                >
                  <el-icon><Promotion /></el-icon>
                  <span>{{ isProcessing ? '处理中...' : '发送' }}</span>
                </el-button>
              </div>
            </div>
          </div>

          <!-- 隐藏的文件输入 -->
          <input
            ref="fileInputRef"
            type="file"
            accept="audio/*"
            style="display: none"
            @change="handleFileSelect"
          />
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import MainLayout from '@/layouts/MainLayout.vue'
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useFileUpload } from '@/composables/useFileUpload'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  InfoFilled,
  Promotion,
  Right,
  CircleCheck,
  Loading,
  Clock,
  Paperclip,
  Document,
  Close,
  Headset,
  View,
  Plus
} from '@element-plus/icons-vue'
import * as aiApi from '@/api/ai'
import { parseSSEMessage } from '@/api/sse'

const router = useRouter()
const userStore = useUserStore()

// 对话状态
interface AIStep {
  text: string
  status: 'pending' | 'processing' | 'done' | 'error'
}

interface Message {
  role: 'user' | 'assistant'
  content: string
  steps?: AIStep[]
  audioUrl?: string    // 音频URL
  cardId?: number      // 卡片ID
  cardTitle?: string   // 卡片标题
}

const userInput = ref('')
const messages = ref<Message[]>([])
const isProcessing = ref(false)
const conversationId = ref<string>()
let currentEventSource: EventSource | null = null

// 本地存储键名
const STORAGE_KEY = 'voicekeeper_chat_history'
const CONVERSATION_ID_KEY = 'voicekeeper_conversation_id'

// 轮询相关
let pollingTimer: NodeJS.Timeout | null = null
const isPolling = ref(false)
const pollingModelName = ref<string | null>(null)

// 保存对话历史到本地存储
const saveToLocalStorage = () => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(messages.value))
    if (conversationId.value) {
      localStorage.setItem(CONVERSATION_ID_KEY, conversationId.value)
    }
  } catch (error) {
    console.error('保存对话历史失败:', error)
  }
}

// 从本地存储恢复对话历史
const loadFromLocalStorage = () => {
  try {
    const savedMessages = localStorage.getItem(STORAGE_KEY)
    const savedConversationId = localStorage.getItem(CONVERSATION_ID_KEY)
    
    if (savedMessages) {
      messages.value = JSON.parse(savedMessages)
    }
    if (savedConversationId) {
      conversationId.value = savedConversationId
    }
  } catch (error) {
    console.error('恢复对话历史失败:', error)
  }
}

// 监听 messages 变化，自动保存
watch(messages, () => {
  saveToLocalStorage()
}, { deep: true })

// 监听 conversationId 变化，自动保存
watch(conversationId, () => {
  if (conversationId.value) {
    localStorage.setItem(CONVERSATION_ID_KEY, conversationId.value)
  }
})

// 组件挂载时恢复对话历史
onMounted(() => {
  loadFromLocalStorage()
})

// 新建对话
const handleNewConversation = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要新建对话吗？当前对话记录将被删除，无法恢复。',
      '新建对话',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 清空当前状态
    messages.value = []
    conversationId.value = undefined
    userInput.value = ''
    uploadedFile.value = null
    isProcessing.value = false
    
    // 停止轮询
    if (isPolling.value) {
      stopPolling()
    }
    
    // 关闭当前SSE连接
    if (currentEventSource) {
      currentEventSource.close()
      currentEventSource = null
    }
    
    // 清除本地存储（删除之前的对话记录）
    localStorage.removeItem(STORAGE_KEY)
    localStorage.removeItem(CONVERSATION_ID_KEY)
    
    ElMessage.success('已开启新对话，旧对话记录已清除')
  } catch (error) {
    // 用户取消
  }
}

// 判断是否是卡片列表（包含多张卡片）
const isCardList = (content: string) => {
  if (!content) return false
  
  // 检测卡片列表的特征：
  // 1. 包含编号列表（1. 2. 3.）- 兼容各种格式：1. 【、1. **【、1. -、1. [
  // 2. 包含多个卡片ID
  // 3. 包含多个音频URL
  const hasNumberedList = /\n\s*[123]\.\s*(?:\*\*)?(?:【|\[|-|「)/.test(content)
  const urlMatches = content.match(/voice-keeper\.oss[-a-z0-9.]+\.aliyuncs\.com\/[^\s\)]+\.mp3/gi)
  const hasMultipleUrls = urlMatches && urlMatches.length > 1
  
  return hasNumberedList || hasMultipleUrls
}

// 判断是否包含单张卡片（完全通用，不依赖任何固定格式）
const hasSingleCard = (content: string) => {
  if (!content) return false
  
  // 只要有voice-keeper的MP3 URL就渲染播放器（卡片ID是可选的）
  const hasAudioUrl = /voice-keeper\.oss[-a-z0-9.]+\.aliyuncs\.com\/[^\s\)]+\.mp3/i.test(content)
  
  // 不是卡片列表（多张卡片）
  const isNotList = !isCardList(content)
  
  // 关键改动：只要有URL且不是列表，就渲染单卡片（不强制要求卡片ID）
  return hasAudioUrl && isNotList
}

// 清理文本中的所有结构化标记（完全通用，移除一切技术性标记）
const cleanStructuredText = (text: string) => {
  if (!text) return ''
  
  let cleaned = text
  
  // 1. 移除所有URL（voice-keeper和其他）
  cleaned = cleaned.replace(/https?:\/\/[^\s]+/gi, '')
  
  // 2. 移除完整的Markdown链接 [文字](URL)
  cleaned = cleaned.replace(/\[([^\]]+)\]\([^)]+\)/g, '')
  
  // 3. 移除不完整的Markdown链接 [文字](
  cleaned = cleaned.replace(/\[[^\]]+\]\(/g, '')
  
  // 4. 移除所有emoji（常见的）
  cleaned = cleaned.replace(/[🎵🆔📋🎙️✨🔍🌙💬❌✅🎶💡🚀📝🔊🏷️⚡]/g, '')
  
  // 5. 移除结构化字段标记（所有可能的格式）
  cleaned = cleaned.replace(/(?:【)?(?:\*\*)?(?:音频|卡片标题|卡片ID|卡片|标题|ID|id)(?:\*\*)?(?:】)?[：:]\s*/gi, '')
  
  // 6. 移除单独的【】方括号对
  cleaned = cleaned.replace(/【([^】]*)】/g, '$1')
  
  // 7. 移除Markdown加粗标记 **xxx**
  cleaned = cleaned.replace(/\*\*([^*]+)\*\*/g, '$1')
  
  // 8. 清理多余的空白
  cleaned = cleaned.replace(/\n{3,}/g, '\n\n') // 多余空行
  cleaned = cleaned.replace(/\s{3,}/g, ' ')     // 多余空格
  cleaned = cleaned.trim()
  
  return cleaned
}

// 提取单张卡片前的引导文字（完全通用，找到第一个结构化标记之前的文字）
const getSingleCardIntro = (content: string) => {
  if (!content) return ''
  
  // 通用策略：找到第一个"明显的结构化标记"之前的所有文字
  const patterns = [
    /^([\s\S]*?)(?=https:\/\/)/i,              // 任何URL之前
    /^([\s\S]*?)(?=\]\()/,                     // Markdown链接之前
    /^([\s\S]*?)(?=[🎵🆔📋🎙️✨🔍])/,          // 任何emoji之前
    /^([\s\S]*?)(?=【(?:音频|卡片|标题|ID))/i, // 【xxx】字段之前
    /^([\s\S]*?)(?=\*\*(?:音频|卡片|ID))/i,    // 加粗字段之前
  ]
  
  for (const pattern of patterns) {
    const match = content.match(pattern)
    if (match) {
      const intro = cleanStructuredText(match[1])
      // 过滤掉太短的或只有标点的
      if (intro && intro.length > 3 && /[\u4e00-\u9fa5a-zA-Z]/.test(intro)) {
        return intro
      }
    }
  }
  
  return ''
}

// 提取单张卡片后的引导文字（完全通用，找到ID之后的所有文字）
const getSingleCardOutro = (content: string) => {
  if (!content) return ''
  
  // 通用策略：找到最后一个数字ID之后的所有文字
  // 支持所有可能的格式：
  // - 卡片ID: 15、【卡片ID】: 15、**卡片ID**: 15
  // - 🆔 16、📋 卡片ID：15
  // - ID: 15
  const patterns = [
    /(?:【)?(?:卡片)?(?:\*\*)?(?:ID|id)(?:\*\*)?(?:】)?[：:]\s*\d+\s*\n*([\s\S]*?)$/i,  // 通用ID之后（含【】）
    /[🆔📋]\s*\d+\s*\n*([\s\S]*?)$/i  // emoji ID之后
  ]
  
  for (const pattern of patterns) {
    const match = content.match(pattern)
    if (match) {
      const outro = cleanStructuredText(match[1])
      // 过滤掉太短的或只有标点的
      if (outro && outro.length > 5 && /[\u4e00-\u9fa5a-zA-Z]/.test(outro)) {
        return outro
      }
    }
  }
  
  return ''
}

// 解析卡片列表，提取每张卡片的信息
const parseCardList = (content: string) => {
  if (!content) return []
  
  const cards: Array<{
    title: string
    audioUrl?: string
    cardId?: number
  }> = []
  
  // 使用正则匹配所有编号的卡片块
  // 兼容多种格式：1. 【标题】、1. **【标题】**、1. [标题]、1. - 标题
  const cardPattern = /(\d+)\.\s*(?:\*\*)?(?:【([^】]+)】|\[([^\]]+)\]|[-–]\s*([^\n]+?))(?:\*\*)?([\s\S]*?)(?=\n\s*\d+\.\s*|$)/g
  let match
  
  while ((match = cardPattern.exec(content)) !== null) {
    const cardBlock = match[0]
    // 标题可能在不同的捕获组中
    const title = (match[2] || match[3] || match[4] || '').trim()
    if (!title) continue
    
    // 提取音频URL - 支持多种格式：
    // 1. 音频: https://...
    // 2. [点击播放](https://...)
    // 3. 直接的URL
    let audioUrl: string | undefined
    
    // 优先匹配Markdown链接
    const markdownMatch = cardBlock.match(/\[([^\]]+)\]\((https:\/\/voice-keeper[^\)]+\.mp3)\)/)
    if (markdownMatch) {
      audioUrl = markdownMatch[2]
    } else {
      // 匹配 "音频:" 格式
      const audioMatch = cardBlock.match(/(?:音频|🔊)[：:]\s*(https:\/\/[^\s\n\)]+\.mp3)/i)
      if (audioMatch) {
        audioUrl = audioMatch[1]
      } else {
        // 匹配直接的URL
        const directUrlMatch = cardBlock.match(/(https:\/\/voice-keeper\.oss[^\s\n\)]+\.mp3)/i)
        if (directUrlMatch) {
          audioUrl = directUrlMatch[1]
        }
      }
    }
    
    // 提取卡片ID（如果有）
    const idMatch = cardBlock.match(/卡片ID[：:]\s*(\d+)/)
    const cardId = idMatch ? parseInt(idMatch[1]) : undefined
    
    if (title || audioUrl) {
      cards.push({ title, audioUrl, cardId })
    }
  }
  
  return cards
}

// 提取卡片列表前的引导文字
const getCardListIntro = (content: string) => {
  if (!content) return ''
  
  // 提取第一个编号列表之前的文字 - 兼容多种格式
  const introMatch = content.match(/^([\s\S]*?)\n\s*1\.\s*(?:\*\*)?(?:【|\[)/)
  if (introMatch) {
    return cleanStructuredText(introMatch[1])
  }
  return ''
}

// 提取卡片列表后的引导文字
const getCardListOutro = (content: string) => {
  if (!content) return ''
  
  // 找到最后一个编号卡片的结束位置
  // 先找到所有的卡片块
  const lastCardPattern = /\d+\.\s*【[^】]+】[\s\S]*?卡片ID[：:]\s*\d+/g
  const matches = content.match(lastCardPattern)
  
  if (!matches || matches.length === 0) return ''
  
  // 找到最后一个卡片的结束位置
  const lastCard = matches[matches.length - 1]
  const lastCardIndex = content.lastIndexOf(lastCard)
  const afterLastCard = content.substring(lastCardIndex + lastCard.length)
  
  // 清理并提取后面的文字
  const outro = cleanStructuredText(afterLastCard)
  
  return outro || ''
}

// 解析AI返回的音频URL和卡片ID（完全通用，不依赖固定格式）
const parseAIResponse = (content: string) => {
  // 如果是卡片列表，不提取单个URL（避免只显示第一个）
  if (isCardList(content)) {
    return {
      audioUrl: undefined,
      cardId: undefined,
      cardTitle: undefined
    }
  }
  

  let audioUrl: string | undefined
  
  // 优先从Markdown链接中提取（更准确）
  const markdownMatch = content.match(/\]\((https:\/\/voice-keeper\.oss[-a-z0-9.]+\.aliyuncs\.com\/[^\)]+\.mp3)\)/i)
  if (markdownMatch) {
    audioUrl = markdownMatch[1]
  } else {
    // 直接查找voice-keeper的mp3 URL（最通用）
    const urlMatch = content.match(/(https:\/\/voice-keeper\.oss[-a-z0-9.]+\.aliyuncs\.com\/[^\s\)]+\.mp3)/i)
    audioUrl = urlMatch ? urlMatch[1] : undefined
  }

  const idPatterns = [
    /(?:【)?(?:卡片)?(?:\*\*)?(?:ID|id|Id)(?:\*\*)?(?:】)?[：:]\s*(\d+)/i,  // 支持【卡片ID】: 15
    /[🆔📋]\s*(\d+)/i  // emoji + 数字
  ]
  
  let cardId: number | undefined
  for (const pattern of idPatterns) {
    const match = content.match(pattern)
    if (match) {
      cardId = parseInt(match[1])
      break
    }
  }

  let cardTitle: string | undefined
  const titlePatterns = [
    /「([^」]+)」/,  // 中文引号「xxx」
    /【卡片标题】[：:]\s*([^\n]+)/i,  // 【卡片标题】: xxx
    /(?:【)?(?:\*\*)?(?:卡片)?标题(?:\*\*)?(?:】)?[：:]\s*([^\n*]+)/i,  // 标题: xxx（所有变体）
    /\*\*【([^】]+)】\*\*/,  // **【xxx】**
    /【([^】]+)】/  // 【xxx】
  ]
  
  for (const pattern of titlePatterns) {
    const match = content.match(pattern)
    if (match) {
      cardTitle = match[1].trim()
      break
    }
  }
  
  return {
    audioUrl: audioUrl,
    cardId: cardId,
    cardTitle: cardTitle
  }
}

// 文件上传相关
const fileInputRef = ref<HTMLInputElement>()
const uploadedFile = ref<File | null>(null)
const isDragoverChat = ref(false)
const { upload: uploadFile } = useFileUpload('voice_sample', {
  maxSize: 52428800,  // 50MB
  allowedTypes: ['audio/mpeg', 'audio/wav', 'audio/mp4', 'audio/ogg', 'audio/flac', 'audio/x-m4a']
})

// 推荐场景

// 文件上传相关函数
const triggerFileUpload = () => {
  fileInputRef.value?.click()
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    if (!file.type.startsWith('audio/')) {
      ElMessage.error('请上传音频文件')
      return
    }
    if (file.size > 52428800) { // 50MB
      ElMessage.error('文件大小不能超过50MB')
      return
    }
    uploadedFile.value = file
    ElMessage.success('音频文件已添加，请输入消息后发送')
  }
}

const handleChatDrop = (event: DragEvent) => {
  isDragoverChat.value = false
  const file = event.dataTransfer?.files[0]
  if (file) {
    if (!file.type.startsWith('audio/')) {
      ElMessage.error('请上传音频文件')
      return
    }
    if (file.size > 52428800) {
      ElMessage.error('文件大小不能超过50MB')
      return
    }
    uploadedFile.value = file
    ElMessage.success('音频文件已添加，请输入消息后发送')
  }
}

const removeUploadedFile = () => {
  uploadedFile.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 发送消息（支持静默模式用于轮询）
const handleSendMessage = async (customMessage?: string, silent = false) => {
  // 自定义消息（用于轮询）或用户输入
  const messageText = customMessage || userInput.value.trim()
  
  // 允许只上传文件不输入文字
  if (!messageText && !uploadedFile.value) {
    return
  }
  
  if (isProcessing.value) {
    return
  }

  let userMessage = customMessage || userInput.value.trim()
  const hasFile = uploadedFile.value !== null
  const fileToUpload = uploadedFile.value
  
  // 清空输入（静默模式不清空）
  if (!silent) {
    userInput.value = ''
    uploadedFile.value = null
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
  }
  
  isProcessing.value = true

  try {
    // 如果有文件，先上传
    if (hasFile && fileToUpload) {
      ElMessage.info('正在上传音频文件...')
      const audioUrl = await uploadFile(fileToUpload)
      
      // 构造包含文件信息的消息
      const fileInfo = `[已上传音频: ${fileToUpload.name}, 大小: ${formatFileSize(fileToUpload.size)}, URL: ${audioUrl}]`
      
      if (userMessage) {
        userMessage = `${userMessage}\n${fileInfo}`
      } else {
        userMessage = `我上传了一个音频文件：${fileToUpload.name}\n${fileInfo}`
      }
      
      ElMessage.success('音频上传成功！')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '文件上传失败')
    isProcessing.value = false
    return
  }

  // 添加用户消息（静默模式不显示）
  if (!silent) {
    messages.value.push({
      role: 'user',
      content: userMessage
    })
  }

  // 添加AI消息占位（静默模式不显示）
  const aiMessageIndex = silent ? -1 : messages.value.length
  if (!silent) {
    messages.value.push({
      role: 'assistant',
      content: '',
      steps: []
    })
  }

  try {
    // 调用AI Agent SSE接口
    currentEventSource = aiApi.chatWithAgent(
      userMessage,
      conversationId.value,
      {
        onMessage: (data: string, event?: MessageEvent) => {
          const parsed = parseSSEMessage(data)
          const aiMessage = !silent && aiMessageIndex >= 0 ? messages.value[aiMessageIndex] : null

          // 接收conversationId
          if (event && event.type === 'conversationId') {
            conversationId.value = data
            console.log('收到conversationId:', data)
            return
          }
          
          // 检测克隆命令，启动轮询
          if (parsed.data && (parsed.data.includes('正在克隆') || parsed.data.includes('克隆处理中')) && (parsed.data.includes('1-2分钟') || parsed.data.includes('预计'))) {
            const nameMatch = parsed.data.match(/正在.*克隆.*[【"]([^【】"]+)[】"]/)||parsed.data.match(/克隆[【"]([^【】"]+)[】"]/)||parsed.data.match(/[【"]([^【】"]+)[】"].*的声音/)
            if (nameMatch) {
              console.log('检测到克隆任务，启动轮询:', nameMatch[1])
              startPolling(nameMatch[1])
            }
          }
          
          // 检测克隆完成，停止轮询（扩展关键词）
          if (parsed.data && (parsed.data.includes('声音已就绪') || parsed.data.includes('克隆成功') || parsed.data.includes('处理完成') || parsed.data.includes('已完成') && parsed.data.includes('状态'))) {
            if (isPolling.value) {
              console.log('检测到克隆完成，停止轮询')
              stopPolling()
              // 静默模式下需要显示这条消息
              if (silent && !messages.value.some(m => m.content.includes(parsed.data))) {
                messages.value.push({
                  role: 'assistant',
                  content: parsed.data
                })
                // 显示成功提示
                ElMessage.success('声音克隆已完成！')
              }
            }
          }

          if (!aiMessage) return  // 静默模式跳过消息更新

          if (parsed.type === 'step') {
            // 解析步骤消息（支持多行内容）
            const stepMatch = data.match(/^Step (\d+): ([\s\S]+)/)
            if (stepMatch) {
              const stepText = stepMatch[2]
              const isDone = stepText.includes('✓')
              const isProcessing = stepText.includes('⏳')
              
              if (!aiMessage.steps) {
                aiMessage.steps = []
              }
              
              // 所有步骤都显示在思考过程中
              aiMessage.steps.push({
                text: stepText.replace('✓', '').replace('⏳', '').trim(),
                status: isDone ? 'done' : isProcessing ? 'processing' : 'pending'
              })

              // 最后一步的内容也显示在回复区域（作为最终答案）
              // 排除纯技术性的消息（如"工具调用成功"）
              const isTechnicalOnly = stepText.match(/^(工具调用成功|查找成功|完成)$/)
              if (!isTechnicalOnly) {
                const cleanedStepText = stepText.replace('✓', '').replace('⏳', '').trim()
                aiMessage.content = cleanedStepText
                
                // 尝试从步骤内容中提取音频URL、卡片ID和标题
                const { audioUrl, cardId, cardTitle } = parseAIResponse(cleanedStepText)
                if (audioUrl) {
                  aiMessage.audioUrl = audioUrl
                  aiMessage.cardId = cardId
                  aiMessage.cardTitle = cardTitle
                }
              }
            }
          } else if (parsed.type === 'complete') {
            // 完成消息（如果有额外的完成文本）
            if (parsed.data && !parsed.data.includes('[DONE]') && !parsed.data.includes('执行结束')) {
              aiMessage.content = parsed.data
              
              // 解析音频URL、卡片ID和标题
              const { audioUrl, cardId, cardTitle } = parseAIResponse(parsed.data)
              if (audioUrl) {
                aiMessage.audioUrl = audioUrl
                aiMessage.cardId = cardId
                aiMessage.cardTitle = cardTitle
              }
            }
          } else if (parsed.type === 'error') {
            // 错误消息
            aiMessage.content = parsed.data
            ElMessage.error('AI处理失败')
          }
        },
        onComplete: () => {
          console.log('AI对话完成，当前conversationId:', conversationId.value)
          isProcessing.value = false
          currentEventSource = null
        },
        onError: (error) => {
          console.error('SSE错误:', error)
          messages.value[aiMessageIndex].content = '抱歉，处理过程中出现错误，请重试'
          isProcessing.value = false
          currentEventSource = null
        }
      }
    )
  } catch (error: any) {
    console.error('发送消息失败:', error)
    messages.value[aiMessageIndex].content = '发送失败，请重试'
    isProcessing.value = false
  }
}

// 场景卡片点击

// 跳转到卡片详情
const goToCardDetail = (cardId: number) => {
  router.push(`/card/${cardId}`)
}

// 轮询检查模型状态
const startPolling = (modelName: string) => {
  if (isPolling.value) return
  
  pollingModelName.value = modelName
  isPolling.value = true
  
  console.log(`开始轮询检查模型【${modelName}】的状态...`)
  ElMessage.info(`开始自动检测${modelName}的声音克隆进度...`)
  
  let pollCount = 0
  const maxPolls = 24 // 最多轮询24次（2分钟）
  
  pollingTimer = setInterval(() => {
    pollCount++
    
    if (pollCount > maxPolls) {
      stopPolling()
      console.log('轮询超时，停止检查')
      ElMessage.warning('自动检测超时，请手动询问克隆状态')
      return
    }
    
    // 自动发送检查消息
    console.log(`轮询第${pollCount}次，检查模型状态...`)
    // 模拟用户询问，触发Agent检查
    handleSendMessage(`${modelName}克隆完成了吗？`, true)
  }, 5000) // 每5秒检查一次
}

const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
  isPolling.value = false
  pollingModelName.value = null
}

// 导航跳转

// 退出登录
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: var(--spacing-xl);
}

/* 欢迎区域 */
.welcome-section {
  margin-bottom: var(--spacing-2xl);
}

.welcome-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 800px;
  margin: 0 auto;
}

.welcome-content > div {
  text-align: center;
  flex: 1;
}

.welcome-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-sm);
}

.welcome-subtitle {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
  margin: 0;
}

.new-conversation-btn {
  flex-shrink: 0;
  margin-left: var(--spacing-lg);
  transition: all var(--transition-base);
}

.new-conversation-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* 对话式输入区域 */
.conversation-section {
  margin-bottom: var(--spacing-2xl);
}

.conversation-container {
  max-width: 800px;
  margin: 0 auto;
}

/* AI 引导语 */
.ai-greeting {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-xl);
}

.ai-avatar {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--gradient-morning);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  box-shadow: var(--shadow-md);
}

/* 消息列表 */
.messages-list {
  max-height: 500px;
  overflow-y: auto;
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);
}

.message-item {
  margin-bottom: var(--spacing-lg);
}

.user-message,
.ai-message {
  display: flex;
  gap: var(--spacing-md);
  align-items: flex-start;
}

.user-message {
  justify-content: flex-end;
}

.user-message .message-bubble {
  background: var(--gradient-morning);
  color: white;
  max-width: 70%;
  padding: var(--spacing-md);
  border-radius: var(--radius-lg);
  border-top-right-radius: var(--radius-xs);
}

.ai-message {
  justify-content: flex-start;
}

.message-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.ai-message .message-bubble {
  background: var(--color-card);
  max-width: 70%;
  padding: var(--spacing-md);
  border-radius: var(--radius-lg);
  border-top-left-radius: var(--radius-xs);
  box-shadow: var(--shadow-sm);
}

/* 包含音频播放器的消息气泡不受宽度限制 */
.ai-message .message-bubble:has(.audio-player-card) {
  max-width: fit-content;
  min-width: 430px;
}

/* 包含卡片列表的消息气泡 */
.ai-message .message-bubble:has(.card-list-response) {
  max-width: 650px;
  min-width: 500px;
}

.ai-thinking {
  padding: var(--spacing-md);
}

.thinking-text {
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
  animation: breathing 1.5s ease-in-out infinite;
}

@keyframes breathing {
  0%, 100% {
    opacity: 0.4;
  }
  50% {
    opacity: 1;
  }
}

.ai-response {
  color: var(--color-text-primary);
  line-height: 1.6;
}

/* 单张卡片样式（通用：创建成功 + 查询结果） */
.single-card-response {
  width: 100%;
}

.card-intro {
  color: var(--color-text-primary);
  font-size: var(--font-size-base);
  line-height: 1.6;
  margin-bottom: var(--spacing-md);
}

.card-outro {
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
  line-height: 1.6;
  margin-top: var(--spacing-md);
}

/* 卡片列表样式 */
.card-list-response {
  width: 100%;
  max-width: 600px;
}

.card-list-intro {
  color: var(--color-text-primary);
  font-size: var(--font-size-base);
  line-height: 1.6;
  margin-bottom: var(--spacing-lg);
}

.cards-grid {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  margin: var(--spacing-md) 0;
}

.card-item {
  background: var(--color-bg);
  border: 2px solid var(--color-primary);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  transition: all var(--transition-base);
}

.card-item:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.card-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: var(--gradient-morning);
  color: white;
  border-radius: 50%;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-bold);
  flex-shrink: 0;
}

.card-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  flex: 1;
}

.card-audio {
  width: 100%;
  height: 40px;
  margin: var(--spacing-sm) 0;
  border-radius: var(--radius-sm);
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border);
}

.card-id {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.card-list-outro {
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
  line-height: 1.6;
  margin-top: var(--spacing-lg);
  font-style: italic;
}

/* 音频播放器卡片 */
.audio-player-card {
  margin-top: var(--spacing-md);
  padding: var(--spacing-md);
  background: var(--color-bg);
  border-radius: var(--radius-md);
  border: 2px solid var(--color-primary);
  min-width: 400px;
}

.audio-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  margin-bottom: var(--spacing-sm);
}

.audio-player {
  width: 100%;
  min-width: 350px;
  margin: var(--spacing-sm) 0;
  border-radius: var(--radius-sm);
  height: 40px;
}

.card-actions {
  margin-top: var(--spacing-sm);
  display: flex;
  gap: var(--spacing-sm);
}

.greeting-bubble {
  flex: 1;
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
  animation: slideInRight 0.5s ease-out;
}

.greeting-bubble p {
  margin: 0 0 var(--spacing-sm);
  color: var(--color-text-primary);
  font-size: var(--font-size-base);
}

.greeting-bubble ul {
  margin: var(--spacing-md) 0;
  padding-left: var(--spacing-lg);
  color: var(--color-text-secondary);
}

.greeting-bubble li {
  margin: var(--spacing-xs) 0;
}

.hint-text {
  margin-top: var(--spacing-md) !important;
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--color-border);
  color: var(--color-primary) !important;
  font-size: var(--font-size-sm) !important;
  font-weight: var(--font-weight-medium);
}

/* 输入区域 */
.input-section {
  margin-top: var(--spacing-lg);
}

.input-wrapper {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-md);
  transition: all var(--transition-base);
}

.input-wrapper.is-dragover {
  border: 2px dashed var(--color-primary);
  background: rgba(255, 154, 98, 0.05);
}

.uploaded-file-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md);
  background: var(--color-bg);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-md);
}

.file-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex: 1;
}

.file-icon {
  font-size: 24px;
  color: var(--color-primary);
}

.file-name {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.file-size {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.conversation-input :deep(.el-textarea__inner) {
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  padding: var(--spacing-md);
  transition: all var(--transition-base);
}

.conversation-input :deep(.el-textarea__inner):focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 154, 98, 0.1);
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--spacing-md);
}

.left-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.input-hint {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
}

.send-btn {
  background: var(--gradient-morning);
  border: none;
  padding: var(--spacing-sm) var(--spacing-xl);
}

/* 快捷操作区域 */
.quick-actions-section {
  margin-bottom: var(--spacing-2xl);
}

.section-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-lg);
}

.action-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-lg);
}

.action-card {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-sm);
}

.action-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.action-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  margin-bottom: var(--spacing-md);
}

.action-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-xs);
}

.action-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

/* 推荐区域 */
.recommendation-section {
  padding-top: 1rem;
  margin-bottom: var(--spacing-2xl);
}

.recommendation-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-lg);
}

.scene-card {
  position: relative;
  padding: var(--spacing-xl);
  border-radius: var(--radius-lg);
  cursor: pointer;
  overflow: hidden;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-md);
  color: white;
}

.scene-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-xl);
}

.scene-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.1);
  opacity: 0;
  transition: opacity var(--transition-base);
}

.scene-card:hover::before {
  opacity: 1;
}

.scene-emoji {
  font-size: 48px;
  margin-bottom: var(--spacing-md);
}

.scene-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  margin: 0 0 var(--spacing-sm);
}

.scene-desc {
  font-size: var(--font-size-base);
  margin: 0;
  opacity: 0.9;
}

.scene-action {
  position: absolute;
  bottom: var(--spacing-lg);
  right: var(--spacing-lg);
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

/* 响应式设计 - 移动端适配 */
@media (max-width: 768px) {
  .audio-player-card {
    min-width: 300px;
  }

  .audio-player {
    min-width: 250px;
  }

  .ai-message .message-bubble:has(.audio-player-card) {
    min-width: 330px;
  }

  .ai-message .message-bubble:has(.card-list-response) {
    max-width: 95%;
    min-width: auto;
  }

  .card-list-response {
    max-width: 100%;
  }

  .card-item {
    padding: var(--spacing-sm);
  }

  .ai-message .message-bubble,
  .user-message .message-bubble {
    max-width: 90%;
  }
}
</style>
