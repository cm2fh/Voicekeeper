import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { VoiceCard, SceneTag } from '@/types/card'
import * as cardApi from '@/api/card'
import type { VoiceCardVO } from '@/api/card'

export const useCardStore = defineStore('card', () => {
  // State
  const cards = ref<VoiceCardVO[]>([])
  const isLoading = ref(false)

  // Getters
  const hasCards = computed(() => cards.value.length > 0)

  const cardsByScene = computed(() => {
    return (scene: SceneTag) => cards.value.filter(c => c.sceneTag === scene)
  })

  const recentCards = computed(() => {
    return cards.value.slice(0, 6)  // 最近6张
  })

  // Actions
  async function fetchCardList(sceneTag?: SceneTag) {
    try {
      isLoading.value = true
      const res = await cardApi.listMyVoiceCards(sceneTag)
      cards.value = res.data || []
      return cards.value
    } catch (error) {
      console.error('获取卡片列表失败:', error)
      return []
    } finally {
      isLoading.value = false
    }
  }

  async function fetchCardListPage(query: cardApi.VoiceCardQueryRequest) {
    try {
      isLoading.value = true
      const res = await cardApi.listMyVoiceCardsPage(query)
      return res.data
    } catch (error) {
      console.error('获取卡片列表失败:', error)
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function addCard(cardData: cardApi.VoiceCardAddRequest) {
    try {
      const res = await cardApi.addVoiceCard(cardData)
      if (res.success) {
        // 重新获取列表
        await fetchCardList()
      }
      return res.success
    } catch (error) {
      console.error('添加卡片失败:', error)
      return false
    }
  }

  async function updateCard(id: number, updates: cardApi.VoiceCardUpdateRequest) {
    try {
      const updateData = { ...updates, id }
      const res = await cardApi.updateVoiceCard(updateData)
      if (res.success) {
        // 重新获取列表以确保数据一致
        await fetchCardList()
      }
      return res.success
    } catch (error) {
      console.error('更新卡片失败:', error)
      return false
    }
  }

  async function deleteCard(id: number) {
    try {
      const res = await cardApi.deleteVoiceCard(id)
      if (res.success) {
        // 删除本地数据
        const index = cards.value.findIndex(c => c.id === id)
        if (index !== -1) {
          cards.value.splice(index, 1)
        }
      }
      return res.success
    } catch (error) {
      console.error('删除卡片失败:', error)
      return false
    }
  }

  async function getCardById(id: number) {
    try {
      // 先从本地查找
      const localCard = cards.value.find(c => c.id === id)
      if (localCard) {
        return localCard
      }
      // 本地没有则从服务器获取
      const res = await cardApi.getVoiceCardById(id)
      return res.data
    } catch (error) {
      console.error('获取卡片详情失败:', error)
      return null
    }
  }

  async function increasePlayCount(id: number) {
    try {
      await cardApi.increasePlayCount(id)
      // 更新本地播放次数
      const card = cards.value.find(c => c.id === id)
      if (card) {
        card.playCount = (card.playCount || 0) + 1
        card.lastPlayTime = new Date().toISOString()
      }
    } catch (error) {
      console.error('增加播放次数失败:', error)
    }
  }

  function clearCards() {
    cards.value = []
  }

  return {
    // State
    cards,
    isLoading,
    // Getters
    hasCards,
    cardsByScene,
    recentCards,
    // Actions
    fetchCardList,
    fetchCardListPage,
    addCard,
    updateCard,
    deleteCard,
    getCardById,
    increasePlayCount,
    clearCards
  }
}, {
  persist: true  // 持久化到 localStorage
})


