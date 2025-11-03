<template>
  <MainLayout>
    <div class="voice-clone-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">声音克隆</h1>
      <p class="page-subtitle">上传音频文件，训练专属的声音模型</p>
    </div>

    <!-- 主要内容 -->
    <div class="content-container">
      <!-- 左侧：上传区域 -->
      <div class="upload-section">
        <div class="upload-card">
          <h3 class="section-title">🎙️ 上传音频</h3>
          
          <!-- 上传区域 -->
          <div
            class="upload-area"
            :class="{ 'is-dragover': isDragover, 'is-uploading': uploading }"
            @drop.prevent="handleDrop"
            @dragover.prevent="isDragover = true"
            @dragleave.prevent="isDragover = false"
          >
            <div v-if="!audioFile" class="upload-placeholder">
              <el-icon class="upload-icon"><Upload /></el-icon>
              <p class="upload-text">将音频文件拖拽到此处</p>
              <p class="upload-hint">或点击按钮选择文件</p>
              <el-button type="primary" @click="triggerFileInput">
                <el-icon><FolderOpened /></el-icon>
                选择文件
              </el-button>
              <p class="upload-tip">支持 MP3、WAV、M4A 等格式，最大 50MB</p>
            </div>

            <div v-else class="audio-preview">
              <div class="audio-info">
                <el-icon class="audio-icon"><Headset /></el-icon>
                <div class="audio-details">
                  <p class="audio-name">{{ audioFile.name }}</p>
                  <p class="audio-size">{{ formatFileSize(audioFile.size) }}</p>
                </div>
                <el-button
                  circle
                  size="small"
                  @click="removeAudioFile"
                >
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
              
              <!-- 上传进度 -->
              <div v-if="uploading" class="upload-progress">
                <el-progress :percentage="progress" :stroke-width="8" />
                <p class="progress-text">上传中... {{ progress }}%</p>
              </div>
            </div>
          </div>

          <!-- 表单 -->
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="100px"
            class="clone-form"
          >
            <el-form-item label="模型名称" prop="modelName">
              <el-input
                v-model="form.modelName"
                placeholder="例如：妈妈的声音"
                maxlength="20"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="模型描述" prop="voiceDesc">
              <el-input
                v-model="form.voiceDesc"
                type="textarea"
                :rows="3"
                placeholder="简单描述这个声音的特点（可选）"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :disabled="!audioFile || uploading || cloning"
                :loading="cloning"
                @click="handleClone"
              >
                <el-icon><MagicStick /></el-icon>
                <span>{{ cloning ? '克隆中...' : '开始克隆' }}</span>
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 使用提示 -->
        <div class="tips-card">
          <h4 class="tips-title">💡 使用提示</h4>
          <ul class="tips-list">
            <li>推荐上传清晰、无噪音的音频文件</li>
            <li>音频时长建议在 10 秒以上</li>
            <li>单人说话效果最佳</li>
            <li>克隆过程需要 1-3 分钟</li>
          </ul>
        </div>
      </div>

      <!-- 右侧：模型列表 -->
      <div class="models-section">
        <div class="models-header">
          <h3 class="section-title">我的声音模型</h3>
          <el-button
            text
            @click="refreshModels"
            :loading="modelStore.isLoading"
          >
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>

        <!-- 模型列表 -->
        <div v-if="modelStore.hasModels" class="models-list">
          <div
            v-for="model in modelStore.models"
            :key="model.id"
            class="model-item"
          >
            <div class="model-icon">
              <el-icon><User /></el-icon>
            </div>
            
            <div class="model-info">
              <h4 class="model-name">{{ model.modelName }}</h4>
              <p v-if="model.voiceDesc" class="model-desc">{{ model.voiceDesc }}</p>
              
              <!-- 状态标签 -->
              <el-tag
                :type="getStatusType(model.trainingStatus)"
                size="small"
                class="model-status"
              >
                {{ getStatusText(model.trainingStatus) }}
              </el-tag>
            </div>

            <!-- 操作 -->
            <div class="model-actions">
              <el-button
                v-if="model.sampleAudioUrl"
                text
                type="primary"
                @click="playPreview(model)"
                size="small"
              >
                <el-icon><VideoPlay /></el-icon>
                试听
              </el-button>
              <el-button
                text
                type="danger"
                @click="handleDeleteModel(model)"
                size="small"
              >
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-models">
          <el-icon class="empty-icon"><Box /></el-icon>
          <p>还没有声音模型</p>
          <p class="empty-hint">上传音频开始克隆吧</p>
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
  </MainLayout>
</template>

<script setup lang="ts">
import MainLayout from '@/layouts/MainLayout.vue'
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useVoiceModelStore } from '@/stores/voiceModel'
import { useFileUpload } from '@/composables/useFileUpload'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { VoiceModel } from '@/types/voice'
import {
  Upload,
  FolderOpened,
  Headset,
  Close,
  MagicStick,
  Refresh,
  User,
  VideoPlay,
  Box,
  Delete
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const modelStore = useVoiceModelStore()

// 文件上传
const fileInputRef = ref<HTMLInputElement>()
const audioFile = ref<File | null>(null)
const isDragover = ref(false)

const { uploading, progress, upload } = useFileUpload('voice_sample', {
  maxSize: 52428800,  // 50MB
  allowedTypes: ['audio/mpeg', 'audio/wav', 'audio/mp4', 'audio/ogg', 'audio/flac', 'audio/x-m4a']
})

// 表单
const formRef = ref<FormInstance>()
const form = reactive({
  modelName: '',
  voiceDesc: ''
})

const rules: FormRules = {
  modelName: [
    { required: true, message: '请输入模型名称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ]
}

const cloning = ref(false)

// 音频播放器
const currentAudio = ref<HTMLAudioElement | null>(null)

// 触发文件选择
const triggerFileInput = () => {
  fileInputRef.value?.click()
}

// 处理文件选择
const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    audioFile.value = file
  }
}

// 处理拖拽上传
const handleDrop = (event: DragEvent) => {
  isDragover.value = false
  const file = event.dataTransfer?.files[0]
  if (file && file.type.startsWith('audio/')) {
    audioFile.value = file
  } else {
    ElMessage.error('请上传音频文件')
  }
}

// 移除文件
const removeAudioFile = () => {
  audioFile.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 开始克隆
const handleClone = async () => {
  if (!audioFile.value) {
    ElMessage.error('请先上传音频文件')
    return
  }

  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        cloning.value = true

        // 1. 上传音频文件
        ElMessage.info('正在上传音频文件...')
        const audioUrl = await upload(audioFile.value!)

        // 2. 调用真实的创建模型接口
        const modelData = {
          modelName: form.modelName,
          voiceDesc: form.voiceDesc,
          sampleAudioUrl: audioUrl,
          sampleDuration: 0,  // TODO: 实际计算音频时长
          sampleFileSize: audioFile.value!.size
        }
        
        const success = await modelStore.addModel(modelData)
        
        if (success) {
          ElMessage.success('声音模型创建成功！')
          
          // 重置表单
          form.modelName = ''
          form.voiceDesc = ''
          audioFile.value = null
          if (fileInputRef.value) {
            fileInputRef.value.value = ''
          }
        } else {
          ElMessage.error('创建失败，请重试')
        }
      } catch (error: any) {
        ElMessage.error(error.message || '克隆失败')
      } finally {
        cloning.value = false
      }
    }
  })
}

// 获取状态类型
const getStatusType = (status: number) => {
  const types: Record<number, any> = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger'
  }
  return types[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '待训练',
    1: '训练中',
    2: '已完成',
    3: '失败'
  }
  return texts[status] || '未知'
}

// 试听
const playPreview = (model: VoiceModel) => {
  if (!model.sampleAudioUrl) {
    ElMessage.warning('该模型没有音频文件')
    return
  }

  // 停止当前播放的音频
  if (currentAudio.value) {
    currentAudio.value.pause()
    currentAudio.value = null
  }

  // 创建新的音频实例
  const audio = new Audio(model.sampleAudioUrl)
  currentAudio.value = audio

  audio.play()
    .then(() => {
      ElMessage.success(`正在播放: ${model.modelName}`)
    })
    .catch((error) => {
      console.error('播放失败:', error)
      ElMessage.error('音频播放失败')
    })

  // 播放结束后清理
  audio.onended = () => {
    currentAudio.value = null
  }
}

// 删除模型
const handleDeleteModel = async (model: VoiceModel) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除声音模型"${model.modelName}"吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const success = await modelStore.deleteModel(model.id)
    if (success) {
      ElMessage.success('删除成功')
    }
  } catch (error) {
    // 用户取消删除
  }
}

// 刷新模型列表
const refreshModels = async () => {
  await modelStore.fetchModelList()
}

// 初始化
onMounted(() => {
  refreshModels()
})
</script>

<style scoped>
.voice-clone-page {
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
  grid-template-columns: 1fr 400px;
  gap: var(--spacing-xl);
}

/* 上传区域 */
.upload-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.upload-card,
.tips-card {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-md);
}

.section-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-lg);
}

/* 上传区域 */
.upload-area {
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-2xl);
  text-align: center;
  transition: all var(--transition-base);
  background: var(--color-bg);
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-area.is-dragover {
  border-color: var(--color-primary);
  background: rgba(255, 154, 98, 0.05);
}

.upload-area.is-uploading {
  pointer-events: none;
  opacity: 0.8;
}

.upload-placeholder {
  width: 100%;
}

.upload-icon {
  font-size: 64px;
  color: var(--color-text-tertiary);
  margin-bottom: var(--spacing-lg);
}

.upload-text {
  font-size: var(--font-size-lg);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-xs);
}

.upload-hint {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0 0 var(--spacing-lg);
}

.upload-tip {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  margin: var(--spacing-md) 0 0;
}

/* 音频预览 */
.audio-preview {
  width: 100%;
}

.audio-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  background: white;
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-md);
}

.audio-icon {
  font-size: 32px;
  color: var(--color-primary);
}

.audio-details {
  flex: 1;
}

.audio-name {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-xs);
  font-weight: var(--font-weight-medium);
}

.audio-size {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

.upload-progress {
  margin-top: var(--spacing-md);
}

.progress-text {
  text-align: center;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: var(--spacing-sm) 0 0;
}

/* 表单 */
.clone-form {
  margin-top: var(--spacing-xl);
}

/* 提示卡片 */
.tips-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-md);
}

.tips-list {
  margin: 0;
  padding-left: var(--spacing-lg);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.8;
}

.tips-list li {
  margin-bottom: var(--spacing-xs);
}

/* 模型列表区域 */
.models-section {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-md);
  height: fit-content;
}

.models-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-lg);
}

.models-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.model-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  background: var(--color-bg);
  border-radius: var(--radius-md);
  transition: all var(--transition-base);
}

.model-item:hover {
  background: rgba(255, 154, 98, 0.05);
}

.model-icon {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--gradient-morning);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.model-info {
  flex: 1;
}

.model-name {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-xs);
}

.model-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0 0 var(--spacing-xs);
}

.model-status {
  margin-top: var(--spacing-xs);
}

.model-actions {
  flex-shrink: 0;
  display: flex;
  gap: var(--spacing-xs);
  align-items: center;
}

/* 空状态 */
.empty-models {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--color-text-tertiary);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: var(--spacing-lg);
  opacity: 0.5;
}

.empty-models p {
  margin: var(--spacing-xs) 0;
}

.empty-hint {
  font-size: var(--font-size-sm);
}
</style>

