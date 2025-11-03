<template>
  <MainLayout>
    <div class="create-card-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">创建声音卡片</h1>
      <p class="page-subtitle">通过对话式交互，让 AI 帮你创建个性化的声音卡片</p>
    </div>

    <!-- 主要内容区域 -->
    <div class="content-container">
      <!-- 左侧：对话区域 -->
      <div class="conversation-area">
        <div class="conversation-messages">
          <!-- 对话消息列表 -->
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message-item"
            :class="`message-${msg.role}`"
          >
            <div v-if="msg.role === 'assistant'" class="message-avatar">🤖</div>
            <div class="message-bubble">
              <div class="message-content">{{ msg.content }}</div>
              <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
            </div>
            <div v-if="msg.role === 'user'" class="message-avatar">👤</div>
          </div>

          <!-- AI 思考中 -->
          <div v-if="isThinking" class="message-item message-assistant">
            <div class="message-avatar">🤖</div>
            <div class="message-bubble thinking-bubble">
              <ThinkingProcess
                :steps="currentSteps"
                :show-log-toggle="false"
              />
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="messages.length === 0 && !isThinking" class="empty-conversation">
            <el-icon class="empty-icon"><ChatDotRound /></el-icon>
            <h3>开始创建你的声音卡片</h3>
            <p>试试说："用妈妈的声音做一张晚安卡片"</p>
          </div>
        </div>

        <!-- 输入框 -->
        <div class="input-area">
          <el-input
            v-model="userInput"
            type="textarea"
            :rows="3"
            placeholder="告诉我你想创建什么样的卡片..."
            resize="none"
            :disabled="isThinking"
            @keydown.ctrl.enter="handleSend"
          />
          <div class="input-actions">
            <el-button
              type="primary"
              :disabled="!userInput.trim() || isThinking"
              :loading="isThinking"
              @click="handleSend"
            >
              <el-icon><Promotion /></el-icon>
              <span>{{ isThinking ? '思考中...' : '发送' }}</span>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 右侧：提示区域 -->
      <div class="tips-area">
        <div class="tips-card">
          <h3 class="tips-title">💡 创建提示</h3>
          <div class="tips-content">
            <div class="tip-item">
              <strong>第一步：选择声音</strong>
              <p>告诉我要用谁的声音，例如"用妈妈的声音"</p>
            </div>
            <div class="tip-item">
              <strong>第二步：选择场景</strong>
              <p>选择一个场景，例如"晚安问候"、"早安鼓励"</p>
            </div>
            <div class="tip-item">
              <strong>第三步：自定义内容（可选）</strong>
              <p>你可以指定具体的文案，或让 AI 帮你生成</p>
            </div>
          </div>
        </div>

        <div class="tips-card">
          <h3 class="tips-title">✨ 示例指令</h3>
          <div class="example-commands">
            <div
              v-for="example in exampleCommands"
              :key="example"
              class="example-item"
              @click="userInput = example"
            >
              <el-icon><ChatDotSquare /></el-icon>
              <span>{{ example }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  </MainLayout>
</template>

<script setup lang="ts">
import MainLayout from '@/layouts/MainLayout.vue'
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import ThinkingProcess from '@/components/AI/ThinkingProcess.vue'
import { useSSE } from '@/composables/useSSE'
import { ChatDotRound, Promotion, ChatDotSquare } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import type { AIStep } from '@/types/ai'

const route = useRoute()
const { steps: currentSteps, sendMessage, isConnected } = useSSE()

// 对话消息
interface Message {
  role: 'user' | 'assistant'
  content: string
  timestamp: number
  steps?: AIStep[]
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isThinking = ref(false)

// 示例指令
const exampleCommands = [
  '用妈妈的声音做一张晚安卡片',
  '用爸爸的声音创建早安问候',
  '用奶奶的声音做一张鼓励卡片',
  '创建一张表达思念的声音卡片'
]

// 初始化：如果有场景参数，自动填充
if (route.query.scene) {
  const sceneMap: Record<string, string> = {
    morning: '早安问候',
    night: '晚安问候',
    encourage: '鼓励支持',
    miss: '表达思念'
  }
  const sceneName = sceneMap[route.query.scene as string]
  if (sceneName) {
    userInput.value = `创建一张${sceneName}卡片`
  }
}

// 发送消息
const handleSend = () => {
  if (!userInput.value.trim() || isThinking.value) {
    return
  }

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: userInput.value,
    timestamp: Date.now()
  })

  // 模拟 AI 思考（实际应该调用后端）
  isThinking.value = true
  
  // 暂时使用模拟数据
  ElMessage.info('对话式创建功能正在开发中，这里展示 AI 思考过程')
  
  // 模拟步骤更新
  setTimeout(() => {
    currentSteps.value = [
      { name: '理解你的需求', status: 'done', timestamp: Date.now() },
      { name: '查找声音模型', status: 'processing', timestamp: Date.now() }
    ]
  }, 500)

  setTimeout(() => {
    currentSteps.value = [
      { name: '理解你的需求', status: 'done', timestamp: Date.now() - 2000 },
      { name: '查找声音模型', status: 'done', timestamp: Date.now() - 1000 },
      { name: '生成文案', status: 'processing', timestamp: Date.now() }
    ]
  }, 2000)

  setTimeout(() => {
    currentSteps.value = [
      { name: '理解你的需求', status: 'done', timestamp: Date.now() - 4000 },
      { name: '查找声音模型', status: 'done', timestamp: Date.now() - 3000 },
      { name: '生成文案', status: 'done', timestamp: Date.now() - 1000 },
      { name: '合成语音', status: 'processing', timestamp: Date.now() }
    ]
  }, 4000)

  setTimeout(() => {
    currentSteps.value = [
      { name: '理解你的需求', status: 'done', timestamp: Date.now() - 6000 },
      { name: '查找声音模型', status: 'done', timestamp: Date.now() - 5000 },
      { name: '生成文案', status: 'done', timestamp: Date.now() - 3000 },
      { name: '合成语音', status: 'done', timestamp: Date.now() - 1000 },
      { name: '保存卡片', status: 'done', timestamp: Date.now() }
    ]

    // 添加 AI 回复
    messages.value.push({
      role: 'assistant',
      content: '✨ 卡片创建成功！已为你生成了一张温馨的晚安卡片。',
      timestamp: Date.now(),
      steps: [...currentSteps.value]
    })

    isThinking.value = false
    userInput.value = ''
  }, 6000)
}

// 格式化时间
const formatTime = (timestamp: number) => {
  return dayjs(timestamp).format('HH:mm')
}
</script>

<style scoped>
.create-card-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: var(--spacing-xl);
}

/* 页面标题 */
.page-header {
  text-align: center;
  margin-bottom: var(--spacing-2xl);
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-sm);
}

.page-subtitle {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin: 0;
}

/* 内容容器 */
.content-container {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: var(--spacing-xl);
}

/* 对话区域 */
.conversation-area {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  height: calc(100vh - 250px);
}

.conversation-messages {
  flex: 1;
  padding: var(--spacing-xl);
  overflow-y: auto;
}

/* 消息项 */
.message-item {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  animation: fadeIn 0.3s ease-out;
}

.message-assistant {
  justify-content: flex-start;
}

.message-user {
  justify-content: flex-end;
}

.message-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: var(--color-bg);
}

.message-bubble {
  max-width: 70%;
  padding: var(--spacing-md) var(--spacing-lg);
  border-radius: var(--radius-lg);
  background: var(--color-bg);
}

.message-user .message-bubble {
  background: var(--gradient-morning);
  color: white;
}

.thinking-bubble {
  max-width: 90%;
  padding: 0;
  background: transparent;
}

.message-content {
  font-size: var(--font-size-base);
  line-height: 1.6;
  margin-bottom: var(--spacing-xs);
}

.message-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.message-user .message-time {
  color: rgba(255, 255, 255, 0.8);
}

/* 空状态 */
.empty-conversation {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--color-text-tertiary);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: var(--spacing-lg);
  opacity: 0.5;
}

.empty-conversation h3 {
  font-size: var(--font-size-xl);
  color: var(--color-text-secondary);
  margin: 0 0 var(--spacing-sm);
}

.empty-conversation p {
  font-size: var(--font-size-base);
  margin: 0;
}

/* 输入区域 */
.input-area {
  padding: var(--spacing-lg);
  border-top: 1px solid var(--color-border);
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--spacing-md);
}

/* 提示区域 */
.tips-area {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.tips-card {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

.tips-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-md);
}

.tips-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.tip-item strong {
  display: block;
  font-size: var(--font-size-base);
  color: var(--color-primary);
  margin-bottom: var(--spacing-xs);
}

.tip-item p {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.6;
}

/* 示例命令 */
.example-commands {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.example-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-bg);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.example-item:hover {
  background: var(--color-primary);
  color: white;
  transform: translateX(4px);
}
</style>

