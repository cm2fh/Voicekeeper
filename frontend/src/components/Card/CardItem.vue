<template>
  <div 
    class="card-item"
    :style="{ background: sceneGradient }"
    @click="handleClick"
  >
    <!-- 场景标识 -->
    <div class="card-header">
      <span class="scene-emoji">{{ sceneInfo.emoji }}</span>
      <el-tag size="small" :type="tagType" class="scene-tag">
        {{ sceneInfo.label }}
      </el-tag>
    </div>

    <!-- 卡片内容 -->
    <div class="card-content">
      <h3 class="card-title">{{ card.cardTitle || '未命名卡片' }}</h3>
      <p class="card-text">{{ truncateText(card.textContent, 60) }}</p>
    </div>

    <!-- 卡片信息 -->
    <div class="card-footer">
      <div class="card-meta">
        <span class="meta-item">
          <el-icon><Headset /></el-icon>
          播放 {{ card.playCount }} 次
        </span>
      </div>
      
      <!-- 操作按钮 -->
      <div class="card-actions" @click.stop>
        <el-button
          circle
          size="small"
          class="action-btn"
          @click="handlePlay"
        >
          <el-icon><VideoPlay /></el-icon>
        </el-button>
        <el-dropdown trigger="click" @command="handleCommand">
          <el-button circle size="small" class="action-btn">
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="play">
                <el-icon><VideoPlay /></el-icon>
                <span>播放</span>
              </el-dropdown-item>
              <el-dropdown-item command="detail">
                <el-icon><View /></el-icon>
                <span>查看详情</span>
              </el-dropdown-item>
              <el-dropdown-item command="delete" divided>
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { VoiceCard } from '@/types/card'
import { SceneTagMap } from '@/types/card'
import {
  Headset,
  VideoPlay,
  MoreFilled,
  View,
  Delete
} from '@element-plus/icons-vue'

interface Props {
  card: VoiceCard
}

interface Emits {
  (e: 'play', card: VoiceCard): void
  (e: 'detail', card: VoiceCard): void
  (e: 'delete', card: VoiceCard): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 场景信息
const sceneInfo = computed(() => SceneTagMap[props.card.sceneTag])

// 场景渐变
const sceneGradient = computed(() => sceneInfo.value.gradient)

// 标签类型
const tagType = computed(() => {
  const types: Record<string, any> = {
    morning: 'warning',
    night: 'info',
    encourage: 'danger',
    miss: ''
  }
  return types[props.card.sceneTag] || ''
})

// 截断文本
const truncateText = (text: string, maxLength: number): string => {
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

// 点击卡片
const handleClick = () => {
  emit('detail', props.card)
}

// 播放
const handlePlay = () => {
  emit('play', props.card)
}

// 下拉菜单命令
const handleCommand = (command: string) => {
  switch (command) {
    case 'play':
      emit('play', props.card)
      break
    case 'detail':
      emit('detail', props.card)
      break
    case 'delete':
      emit('delete', props.card)
      break
  }
}
</script>

<style scoped>
.card-item {
  position: relative;
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-base);
  color: white;
  overflow: hidden;
  box-shadow: var(--shadow-md);
  min-height: 200px;
  display: flex;
  flex-direction: column;
}

.card-item::before {
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

.card-item:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-xl);
}

.card-item:hover::before {
  opacity: 1;
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-md);
  position: relative;
  z-index: 1;
}

.scene-emoji {
  font-size: 28px;
}

.scene-tag {
  background: rgba(255, 255, 255, 0.3);
  border: none;
  color: white;
  font-weight: var(--font-weight-medium);
}

/* 卡片内容 */
.card-content {
  flex: 1;
  margin-bottom: var(--spacing-md);
  position: relative;
  z-index: 1;
}

.card-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  margin: 0 0 var(--spacing-sm);
  line-height: 1.4;
}

.card-text {
  font-size: var(--font-size-sm);
  line-height: 1.6;
  opacity: 0.95;
  margin: 0;
}

/* 卡片底部 */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
}

.card-meta {
  display: flex;
  gap: var(--spacing-md);
  font-size: var(--font-size-sm);
  opacity: 0.9;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

/* 操作按钮 */
.card-actions {
  display: flex;
  gap: var(--spacing-xs);
}

.action-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>

