import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { VoiceModel } from '@/types/voice'
import * as voiceModelApi from '@/api/voiceModel'
import type { VoiceModelVO } from '@/api/voiceModel'

export const useVoiceModelStore = defineStore('voiceModel', () => {
  // State
  const models = ref<VoiceModelVO[]>([])
  const isLoading = ref(false)

  // Getters
  const completedModels = computed(() =>
    models.value.filter(m => m.trainingStatus === 2)
  )

  const processingModels = computed(() =>
    models.value.filter(m => m.trainingStatus === 1)
  )

  const hasModels = computed(() => models.value.length > 0)

  // Actions
  async function fetchModelList() {
    try {
      isLoading.value = true
      const res = await voiceModelApi.listMyVoiceModels()
      models.value = res.data || []
      return models.value
    } catch (error) {
      console.error('获取声音模型列表失败:', error)
      return []
    } finally {
      isLoading.value = false
    }
  }

  async function addModel(modelData: voiceModelApi.VoiceModelAddRequest) {
    try {
      const res = await voiceModelApi.addVoiceModel(modelData)
      if (res.success) {
        // 重新获取列表
        await fetchModelList()
      }
      return res.success
    } catch (error) {
      console.error('添加声音模型失败:', error)
      return false
    }
  }

  async function updateModel(id: number, updates: voiceModelApi.VoiceModelUpdateRequest) {
    try {
      const updateData = { ...updates, id }
      const res = await voiceModelApi.updateVoiceModel(updateData)
      if (res.success) {
        // 重新获取列表以确保数据一致
        await fetchModelList()
      }
      return res.success
    } catch (error) {
      console.error('更新声音模型失败:', error)
      return false
    }
  }

  async function deleteModel(id: number) {
    try {
      const res = await voiceModelApi.deleteVoiceModel(id)
      if (res.success) {
        // 删除本地数据
        const index = models.value.findIndex(m => m.id === id)
        if (index !== -1) {
          models.value.splice(index, 1)
        }
      }
      return res.success
    } catch (error) {
      console.error('删除声音模型失败:', error)
      return false
    }
  }

  function getModelById(id: number) {
    return models.value.find(m => m.id === id)
  }

  function getModelByName(name: string) {
    return models.value.find(m => m.modelName === name)
  }

  function clearModels() {
    models.value = []
  }

  return {
    // State
    models,
    isLoading,
    // Getters
    completedModels,
    processingModels,
    hasModels,
    // Actions
    fetchModelList,
    addModel,
    updateModel,
    deleteModel,
    getModelById,
    getModelByName,
    clearModels
  }
}, {
  persist: true  // 持久化到 localStorage
})

