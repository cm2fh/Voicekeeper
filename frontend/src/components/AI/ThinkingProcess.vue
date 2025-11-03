<template>
  <div class="thinking-process">
    <!-- 标题栏 -->
    <div class="process-header">
      <div class="header-left">
        <div class="ai-icon breathing">🧠</div>
        <h3 class="header-title">VoiceKeeper 正在思考...</h3>
      </div>
      <div class="header-right">
        <el-button
          v-if="showLogToggle"
          text
          size="small"
          @click="showDetailLog = !showDetailLog"
        >
          {{ showDetailLog ? '隐藏日志' : '查看详细日志' }}
          <el-icon>
            <component :is="showDetailLog ? 'ArrowUp' : 'ArrowDown'" />
          </el-icon>
        </el-button>
      </div>
    </div>

    <!-- 步骤列表 -->
    <div class="steps-container">
      <div
        v-for="(step, index) in steps"
        :key="index"
        class="step-item"
        :class="[`step-${step.status}`, { 'step-active': step.status === 'processing' }]"
      >
        <!-- 步骤图标 -->
        <div class="step-icon">
          <div v-if="step.status === 'pending'" class="icon-pending">
            <el-icon><Clock /></el-icon>
          </div>
          <div v-else-if="step.status === 'processing'" class="icon-processing">
            <el-icon class="spin"><Loading /></el-icon>
          </div>
          <div v-else-if="step.status === 'done'" class="icon-done">
            <el-icon><SuccessFilled /></el-icon>
          </div>
          <div v-else-if="step.status === 'error'" class="icon-error">
            <el-icon><CircleCloseFilled /></el-icon>
          </div>
        </div>

        <!-- 步骤内容 -->
        <div class="step-content">
          <div class="step-name">{{ step.name }}</div>
          <div v-if="step.message" class="step-message">{{ step.message }}</div>
          <div v-if="step.timestamp" class="step-time">
            {{ formatTime(step.timestamp) }}
          </div>
        </div>

        <!-- 连接线 -->
        <div v-if="index < steps.length - 1" class="step-line"></div>
      </div>

      <!-- 空状态 -->
      <div v-if="steps.length === 0" class="empty-state">
        <el-icon class="empty-icon"><ChatDotRound /></el-icon>
        <p>等待开始执行...</p>
      </div>
    </div>

    <!-- 详细日志 -->
    <transition name="slide-fade">
      <div v-if="showDetailLog && detailLogs.length > 0" class="detail-logs">
        <div class="logs-header">
          <el-icon><Document /></el-icon>
          <span>详细执行日志</span>
        </div>
        <div class="logs-content">
          <div
            v-for="(log, index) in detailLogs"
            :key="index"
            class="log-item"
          >
            <span class="log-time">{{ formatTime(log.timestamp) }}</span>
            <span class="log-text">{{ log.text }}</span>
          </div>
        </div>
      </div>
    </transition>

    <!-- 完成状态 -->
    <div v-if="isCompleted" class="completion-banner">
      <el-icon class="completion-icon"><CircleCheckFilled /></el-icon>
      <span class="completion-text">✨ 执行完成！</span>
    </div>

    <!-- 错误状态 -->
    <div v-if="hasError" class="error-banner">
      <el-icon class="error-icon"><WarningFilled /></el-icon>
      <span class="error-text">执行过程中遇到错误</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { AIStep } from '@/types/ai'
import {
  Clock,
  Loading,
  SuccessFilled,
  CircleCloseFilled,
  ChatDotRound,
  Document,
  CircleCheckFilled,
  WarningFilled,
  ArrowUp,
  ArrowDown
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'

interface DetailLog {
  timestamp: number
  text: string
}

interface Props {
  steps?: AIStep[]
  detailLogs?: DetailLog[]
  showLogToggle?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  steps: () => [],
  detailLogs: () => [],
  showLogToggle: true
})

const showDetailLog = ref(false)

// 计算是否完成
const isCompleted = computed(() => {
  return props.steps.length > 0 && props.steps.every(s => s.status === 'done')
})

// 计算是否有错误
const hasError = computed(() => {
  return props.steps.some(s => s.status === 'error')
})

// 格式化时间
const formatTime = (timestamp: number) => {
  return dayjs(timestamp).format('HH:mm:ss')
}
</script>

<style scoped>
.thinking-process {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-md);
}

/* 标题栏 */
.process-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 2px solid var(--color-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.ai-icon {
  width: 40px;
  height: 40px;
  font-size: 24px;
  background: var(--gradient-morning);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-sm);
}

.header-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

/* 步骤容器 */
.steps-container {
  position: relative;
  min-height: 100px;
}

/* 步骤项 */
.step-item {
  position: relative;
  display: flex;
  gap: var(--spacing-md);
  padding: var(--spacing-md) 0;
  transition: all var(--transition-base);
}

.step-active {
  animation: pulse 2s ease-in-out infinite;
}

/* 步骤图标 */
.step-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 18px;
  z-index: 1;
}

.icon-pending {
  color: var(--color-text-tertiary);
  background: var(--color-bg);
  border: 2px solid var(--color-border);
}

.icon-processing {
  color: white;
  background: var(--color-primary);
  box-shadow: 0 0 12px rgba(255, 154, 98, 0.5);
}

.icon-done {
  color: white;
  background: var(--color-success);
}

.icon-error {
  color: white;
  background: var(--color-error);
}

/* 步骤内容 */
.step-content {
  flex: 1;
  padding-top: 4px;
}

.step-name {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xs);
}

.step-message {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-xs);
}

.step-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  font-family: 'Courier New', monospace;
}

/* 连接线 */
.step-line {
  position: absolute;
  left: 15px;
  top: 40px;
  width: 2px;
  height: calc(100% - 8px);
  background: var(--color-border);
}

.step-done + .step-item .step-line {
  background: var(--color-success);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--color-text-tertiary);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-md);
  opacity: 0.5;
}

/* 详细日志 */
.detail-logs {
  margin-top: var(--spacing-lg);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--color-border);
}

.logs-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-md);
}

.logs-content {
  max-height: 300px;
  overflow-y: auto;
  background: var(--color-bg);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
}

.log-item {
  font-size: var(--font-size-sm);
  font-family: 'Courier New', monospace;
  margin-bottom: var(--spacing-xs);
  display: flex;
  gap: var(--spacing-md);
}

.log-time {
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.log-text {
  color: var(--color-text-secondary);
  word-break: break-all;
}

/* 完成横幅 */
.completion-banner {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
  background: linear-gradient(135deg, rgba(168, 230, 207, 0.2) 0%, rgba(168, 230, 207, 0.1) 100%);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--color-success);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  animation: slideInRight 0.5s ease-out;
}

.completion-icon {
  color: var(--color-success);
  font-size: 20px;
}

.completion-text {
  color: var(--color-success);
  font-weight: var(--font-weight-semibold);
}

/* 错误横幅 */
.error-banner {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
  background: linear-gradient(135deg, rgba(255, 107, 107, 0.2) 0%, rgba(255, 107, 107, 0.1) 100%);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--color-error);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.error-icon {
  color: var(--color-error);
  font-size: 20px;
}

.error-text {
  color: var(--color-error);
  font-weight: var(--font-weight-semibold);
}

/* 过渡动画 */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.3s ease-in;
}

.slide-fade-enter-from {
  transform: translateY(-10px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}
</style>

