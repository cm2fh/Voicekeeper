<template>
  <div 
    v-if="card"
    class="card-detail-page"
    :style="{ background: sceneGradient }"
  >
    <!-- 返回按钮 -->
    <div class="back-button">
      <el-button circle @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
    </div>

    <!-- 主要内容 -->
    <div class="content-wrapper">
      <!-- 场景标识 -->
      <div class="scene-badge">
        <span class="scene-emoji">{{ sceneInfo.emoji }}</span>
        <span class="scene-name">{{ sceneInfo.label }}</span>
      </div>

      <!-- 卡片标题 -->
      <h1 class="card-title">{{ card.cardTitle || '未命名卡片' }}</h1>

      <!-- 播放器区域 -->
      <div class="player-section">
        <!-- 隐藏的音频元素 -->
        <audio
          ref="audioRef"
          :src="card.audioUrl"
          @timeupdate="handleTimeUpdate"
          @loadedmetadata="handleLoadedMetadata"
          @ended="handleEnded"
          @play="handlePlay"
          @pause="handlePause"
        />

        <!-- 播放按钮 -->
        <div class="play-button-wrapper">
          <button 
            class="play-button"
            :class="{ 'is-playing': isPlaying }"
            @click="togglePlay"
          >
            <el-icon v-if="!isPlaying" class="play-icon">
              <VideoPlay />
            </el-icon>
            <el-icon v-else class="pause-icon">
              <VideoPause />
            </el-icon>
          </button>
          
          <!-- 播放波纹 -->
          <div v-if="isPlaying" class="play-ripple"></div>
        </div>

        <!-- 进度条 -->
        <div class="progress-section">
          <span class="time-text">{{ formatTime(currentTime) }}</span>
          <el-slider
            v-model="progressPercent"
            :show-tooltip="false"
            class="progress-slider"
            @change="handleSeek"
          />
          <span class="time-text">{{ formatTime(duration) }}</span>
        </div>

        <!-- 音量控制 -->
        <div class="volume-section">
          <el-icon class="volume-icon"><Microphone /></el-icon>
          <el-slider
            v-model="volume"
            :show-tooltip="false"
            class="volume-slider"
            @change="handleVolumeChange"
          />
        </div>
      </div>

      <!-- 文案内容 -->
      <div class="content-card glass">
        <h3 class="content-title">💬 文案内容</h3>
        <p class="content-text">{{ card.textContent }}</p>
        
        <!-- AI生成标识 -->
        <div v-if="card.aiGenerated === 1" class="ai-badge">
          <el-icon><MagicStick /></el-icon>
          <span>AI 生成</span>
        </div>
      </div>

      <!-- 卡片信息 -->
      <div class="info-card glass">
        <h3 class="info-title">ℹ️ 卡片信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ formatDate(card.createTime) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">播放次数</span>
            <span class="info-value">{{ card.playCount }} 次</span>
          </div>
          <div class="info-item">
            <span class="info-label">场景类型</span>
            <span class="info-value">{{ sceneInfo.label }}</span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="actions-section">
        <el-button size="large" class="action-btn" @click="handleShare">
          <el-icon><Share /></el-icon>
          <span>分享</span>
        </el-button>
        <el-button size="large" class="action-btn" @click="handleDownload">
          <el-icon><Download /></el-icon>
          <span>下载</span>
        </el-button>
        <el-button size="large" class="action-btn" type="danger" @click="handleDelete">
          <el-icon><Delete /></el-icon>
          <span>删除</span>
        </el-button>
      </div>
    </div>
  </div>

  <!-- 加载状态 -->
  <div v-else class="loading-state">
    <el-icon class="loading-icon spin"><Loading /></el-icon>
    <p>加载中...</p>
  </div>

  <!-- 分享对话框 -->
  <el-dialog
    v-model="showShareDialog"
    title="分享卡片"
    width="500px"
    :close-on-click-modal="false"
  >
    <div class="share-content">
      <div class="share-preview">
        <h3>{{ card?.cardTitle }}</h3>
        <p class="share-text">{{ card?.textContent }}</p>
        <div class="share-meta">
          <el-tag :type="getTagType(card?.sceneTag)">{{ getSceneLabel(card?.sceneTag) }}</el-tag>
          <span class="share-date">{{ formatDate(card?.createTime) }}</span>
        </div>
      </div>
      
      <div class="share-link-section">
        <div class="link-label">分享链接</div>
        <div class="link-input-wrapper">
          <el-input
            v-model="shareLink"
            readonly
            class="share-link-input"
          >
            <template #append>
              <el-button @click="copyShareLink">
                <el-icon><DocumentCopy /></el-icon>
                复制
              </el-button>
            </template>
          </el-input>
        </div>
      </div>

      <div class="share-tips">
        <el-icon><InfoFilled /></el-icon>
        <span>复制链接后可分享给好友，让他们也能听到这张声音卡片</span>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCardStore } from '@/stores/card'
import { SceneTagMap } from '@/types/card'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  ArrowLeft,
  VideoPlay,
  VideoPause,
  Microphone,
  MagicStick,
  Share,
  Download,
  Delete,
  Loading,
  DocumentCopy,
  InfoFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const cardStore = useCardStore()

// 卡片数据
const cardId = Number(route.params.id)
const card = ref<any>(null)
const loading = ref(true)

// 分享对话框
const showShareDialog = ref(false)
const shareLink = ref('')

// 场景信息
const sceneInfo = computed(() => {
  if (!card.value || !card.value.sceneTag) return { emoji: '', label: '', gradient: '' }
  return SceneTagMap[card.value.sceneTag as keyof typeof SceneTagMap]
})

const sceneGradient = computed(() => sceneInfo.value.gradient)

// 音频引用
const audioRef = ref<HTMLAudioElement>()

// 播放状态
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const volume = ref(80)

// 进度百分比
const progressPercent = computed({
  get: () => duration.value > 0 ? (currentTime.value / duration.value) * 100 : 0,
  set: (val) => {
    currentTime.value = (val / 100) * duration.value
  }
})

// 音频事件处理
const handleTimeUpdate = () => {
  if (audioRef.value) {
    currentTime.value = audioRef.value.currentTime
  }
}

const handleLoadedMetadata = () => {
  if (audioRef.value) {
    duration.value = audioRef.value.duration
    audioRef.value.volume = volume.value / 100
  }
}

const handleEnded = () => {
  isPlaying.value = false
  currentTime.value = 0
}

const handlePlay = () => {
  isPlaying.value = true
}

const handlePause = () => {
  isPlaying.value = false
}

// 播放控制
const togglePlay = async () => {
  if (!audioRef.value || !card.value) return

  try {
    if (isPlaying.value) {
      audioRef.value.pause()
    } else {
      await audioRef.value.play()
      // 增加播放次数（只在首次播放时）
      if (currentTime.value === 0) {
        cardStore.increasePlayCount(card.value.id)
      }
    }
  } catch (error) {
    console.error('播放失败:', error)
    ElMessage.error('播放失败，请检查音频文件')
  }
}

// 进度拖动
const handleSeek = (value: number) => {
  if (audioRef.value) {
    audioRef.value.currentTime = (value / 100) * duration.value
  }
}

// 音量调整
const handleVolumeChange = (value: number) => {
  if (audioRef.value) {
    audioRef.value.volume = value / 100
  }
}

// 格式化时间
const formatTime = (seconds: number): string => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 格式化日期
const formatDate = (dateStr: string): string => {
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm')
}

// 返回
const goBack = () => {
  router.back()
}

// 分享
const handleShare = () => {
  if (!card.value) return
  
  // 生成分享链接
  shareLink.value = window.location.href
  showShareDialog.value = true
}

// 复制分享链接
const copyShareLink = async () => {
  try {
    await navigator.clipboard.writeText(shareLink.value)
    ElMessage.success('链接已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败，请手动复制')
  }
}

// 获取场景标签
const getSceneLabel = (sceneTag?: string) => {
  if (!sceneTag) return '自定义'
  const info = SceneTagMap[sceneTag as keyof typeof SceneTagMap]
  return info ? info.label : '自定义'
}

// 获取标签类型
const getTagType = (sceneTag?: string) => {
  const types: Record<string, any> = {
    morning: 'warning',
    night: 'info',
    encourage: 'danger',
    miss: ''
  }
  return sceneTag ? types[sceneTag] || '' : ''
}

// 下载
const handleDownload = () => {
  if (!card.value || !card.value.audioUrl) {
    ElMessage.error('无法下载音频')
    return
  }
  
  try {
    // 创建一个隐藏的 a 标签来触发下载
    const link = document.createElement('a')
    link.href = card.value.audioUrl
    link.download = `${card.value.cardTitle}.mp3`
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    ElMessage.success('开始下载音频')
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

// 删除
const handleDelete = async () => {
  if (!card.value) return

  try {
    await ElMessageBox.confirm(
      '确定要删除这张卡片吗？',
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const success = await cardStore.deleteCard(card.value.id)
    if (success) {
      ElMessage.success('删除成功')
      router.push('/card-library')
    }
  } catch (error) {
    // 取消删除
  }
}

// 组件卸载时清理
onUnmounted(() => {
  if (audioRef.value) {
    audioRef.value.pause()
    audioRef.value.src = ''
  }
})

// 初始化
onMounted(async () => {
  try {
    loading.value = true
    const cardData = await cardStore.getCardById(cardId)
    if (cardData) {
      card.value = cardData
    } else {
      ElMessage.error('卡片不存在')
      router.push('/card-library')
    }
  } catch (error) {
    console.error('加载卡片失败:', error)
    ElMessage.error('加载失败')
    router.push('/card-library')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.card-detail-page {
  min-height: 100vh;
  padding: var(--spacing-2xl);
  position: relative;
  color: white;
  overflow: hidden;
}

.card-detail-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 0;
}

/* 返回按钮 */
.back-button {
  position: fixed;
  top: var(--spacing-xl);
  left: var(--spacing-xl);
  z-index: 10;
}

.back-button .el-button {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: none;
  color: white;
  font-size: 20px;
  width: 48px;
  height: 48px;
}

.back-button .el-button:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 主要内容 */
.content-wrapper {
  max-width: 800px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
  padding-top: var(--spacing-2xl);
}

/* 场景标识 */
.scene-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-lg);
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-full);
  margin-bottom: var(--spacing-lg);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
}

.scene-emoji {
  font-size: 24px;
}

/* 卡片标题 */
.card-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  margin: 0 0 var(--spacing-2xl);
  text-align: center;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

/* 播放器区域 */
.player-section {
  margin-bottom: var(--spacing-2xl);
}

/* 播放按钮 */
.play-button-wrapper {
  position: relative;
  display: flex;
  justify-content: center;
  margin-bottom: var(--spacing-xl);
}

.play-button {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(20px);
  cursor: pointer;
  transition: all var(--transition-base);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: white;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 2;
}

.play-button:hover {
  transform: scale(1.1);
  background: rgba(255, 255, 255, 0.4);
}

.play-button:active {
  transform: scale(1.05);
}

.play-button.is-playing {
  animation: pulse 2s ease-in-out infinite;
}

/* 播放波纹 */
.play-ripple {
  position: absolute;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.5);
  animation: ripple 2s ease-out infinite;
}

@keyframes ripple {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

/* 进度条 */
.progress-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.time-text {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  min-width: 40px;
  text-align: center;
}

.progress-slider {
  flex: 1;
}

.progress-slider :deep(.el-slider__runway) {
  background: rgba(255, 255, 255, 0.3);
  height: 6px;
}

.progress-slider :deep(.el-slider__bar) {
  background: white;
}

.progress-slider :deep(.el-slider__button) {
  border-color: white;
  width: 16px;
  height: 16px;
}

/* 音量控制 */
.volume-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  max-width: 300px;
  margin: 0 auto;
}

.volume-icon {
  font-size: 20px;
}

.volume-slider {
  flex: 1;
}

.volume-slider :deep(.el-slider__runway) {
  background: rgba(255, 255, 255, 0.3);
  height: 4px;
}

.volume-slider :deep(.el-slider__bar) {
  background: white;
}

.volume-slider :deep(.el-slider__button) {
  border-color: white;
  width: 12px;
  height: 12px;
}

/* 内容卡片 */
.content-card,
.info-card {
  margin-bottom: var(--spacing-xl);
  padding: var(--spacing-xl);
  border-radius: var(--radius-lg);
}

.content-title,
.info-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  margin: 0 0 var(--spacing-lg);
}

.content-text {
  font-size: var(--font-size-lg);
  line-height: 1.8;
  margin: 0 0 var(--spacing-md);
  white-space: pre-wrap;
}

/* AI标识 */
.ai-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) var(--spacing-md);
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-lg);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.info-label {
  font-size: var(--font-size-sm);
  opacity: 0.8;
}

.info-value {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
}

/* 操作按钮 */
.actions-section {
  display: flex;
  gap: var(--spacing-md);
  justify-content: center;
}

.action-btn {
  flex: 1;
  max-width: 200px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: none;
  color: white;
  height: 48px;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.action-btn.is-danger {
  background: rgba(255, 107, 107, 0.3);
}

.action-btn.is-danger:hover {
  background: rgba(255, 107, 107, 0.4);
}

/* 加载状态 */
.loading-state {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
}

.loading-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-md);
}

/* 分享对话框 */
.share-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.share-preview {
  padding: var(--spacing-lg);
  background: var(--color-bg);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--color-primary);
}

.share-preview h3 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-sm);
}

.share-text {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0 0 var(--spacing-md);
  max-height: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.share-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.share-date {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.share-link-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.link-label {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.share-link-input {
  font-family: 'Courier New', monospace;
}

.share-link-input :deep(.el-input__inner) {
  font-size: var(--font-size-sm);
}

.share-tips {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  background: rgba(255, 154, 98, 0.1);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.share-tips .el-icon {
  font-size: 16px;
  color: var(--color-primary);
  flex-shrink: 0;
}
</style>

