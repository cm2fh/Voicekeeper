<template>
  <MainLayout>
    <div class="card-library-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">卡片库</h1>
      <p class="page-subtitle">管理你的所有声音卡片</p>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filters-section">
      <!-- 搜索框 -->
      <div class="search-box">
        <el-input
          v-model="searchQuery"
          placeholder="搜索卡片标题或内容..."
          size="large"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button
          v-if="searchQuery"
          type="primary"
          size="large"
          :loading="searching"
          @click="handleSemanticSearch"
        >
          <el-icon><MagicStick /></el-icon>
          <span>语义搜索</span>
        </el-button>
      </div>

      <!-- 场景筛选 -->
      <div class="scene-filters">
        <el-button
          :type="currentScene === 'all' ? 'primary' : ''"
          @click="handleSceneFilter('all')"
        >
          全部
        </el-button>
        <el-button
          v-for="(info, tag) in SceneTagMap"
          :key="tag"
          :type="currentScene === tag ? 'primary' : ''"
          @click="handleSceneFilter(tag as SceneTag)"
        >
          {{ info.emoji }} {{ info.label }}
        </el-button>
      </div>

      <!-- 排序 -->
      <div class="sort-section">
        <span class="sort-label">排序：</span>
        <el-radio-group v-model="sortBy" size="small" @change="handleSort">
          <el-radio-button value="createTime">最新创建</el-radio-button>
          <el-radio-button value="playCount">播放次数</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 卡片网格 -->
    <div v-loading="isLoading" class="cards-container">
      <div v-if="paginatedCards.length > 0" class="cards-grid">
        <CardItem
          v-for="card in paginatedCards"
          :key="card.id"
          :card="card"
          @play="handlePlay"
          @detail="handleDetail"
          @delete="handleDelete"
        />
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <el-icon class="empty-icon"><Box /></el-icon>
        <h3>{{ getEmptyText() }}</h3>
        <p class="empty-hint">{{ getEmptyHint() }}</p>
        <el-button type="primary" @click="goToCreate">
          <el-icon><Plus /></el-icon>
          创建卡片
        </el-button>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination-section">
      <el-pagination
        v-model:current-page="queryParams.current"
        :page-size="queryParams.pageSize"
        :total="total"
        layout="total, prev, pager, next, jumper"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 语义搜索结果弹窗 -->
    <el-dialog
      v-model="showSearchResult"
      title="语义搜索结果"
      width="800px"
    >
      <div class="search-results">
        <div v-if="searchResults.length > 0">
          <div
            v-for="result in searchResults"
            :key="result.card.id"
            class="search-result-item"
            @click="handleDetail(result.card)"
          >
            <div class="result-header">
              <span class="result-emoji">{{ SceneTagMap[result.card.sceneTag].emoji }}</span>
              <h4 class="result-title">{{ result.card.cardTitle }}</h4>
              <el-tag size="small" type="success">匹配度 {{ result.score }}%</el-tag>
            </div>
            <p class="result-content">{{ result.card.textContent }}</p>
            <div class="result-reason">
              <el-icon><InfoFilled /></el-icon>
              <span>{{ result.reason }}</span>
            </div>
          </div>
        </div>
        <div v-else class="no-results">
          <el-icon class="no-results-icon"><QuestionFilled /></el-icon>
          <p>没有找到相关卡片</p>
        </div>
      </div>
    </el-dialog>
  </div>
  </MainLayout>
</template>

<script setup lang="ts">
import MainLayout from '@/layouts/MainLayout.vue'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCardStore } from '@/stores/card'
import CardItem from '@/components/Card/CardItem.vue'
import type { VoiceCard, SceneTag } from '@/types/card'
import { SceneTagMap } from '@/types/card'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  MagicStick,
  Box,
  Plus,
  InfoFilled,
  QuestionFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const cardStore = useCardStore()

// 分页参数
const queryParams = ref({
  current: 1,
  pageSize: 12,
  sceneTag: undefined as string | undefined,
  cardTitle: undefined as string | undefined,
  sortField: 'createTime',
  sortOrder: 'descend'  // 后端期望的格式：'ascend' 或 'descend'
})

const total = ref(0)
const isLoading = ref(false)
const paginatedCards = ref<any[]>([])

// 搜索和筛选
const searchQuery = ref('')
const currentScene = ref<SceneTag | 'all'>('all')
const sortBy = ref('createTime')
const searching = ref(false)
const showSearchResult = ref(false)

interface SearchResult {
  card: VoiceCard
  score: number
  reason: string
}

const searchResults = ref<SearchResult[]>([])

// 加载卡片列表（分页）
const loadCards = async () => {
  try {
    isLoading.value = true
    const res = await cardStore.fetchCardListPage(queryParams.value)
    if (res) {
      paginatedCards.value = res.records
      total.value = res.total
    }
  } catch (error) {
    console.error('加载卡片失败:', error)
  } finally {
    isLoading.value = false
  }
}

// 分页改变
const handlePageChange = (page: number) => {
  queryParams.value.current = page
  loadCards()
}

// 场景筛选
const handleSceneFilter = (scene: SceneTag | 'all') => {
  currentScene.value = scene
  queryParams.value.sceneTag = scene === 'all' ? undefined : scene
  queryParams.value.current = 1
  loadCards()
}

// 排序
const handleSort = () => {
  queryParams.value.sortField = sortBy.value
  // 根据排序字段设置合理的默认排序方向
  // createTime: 降序（最新的在前）
  // playCount: 降序（播放最多的在前）
  queryParams.value.sortOrder = 'descend'
  queryParams.value.current = 1
  loadCards()
}

// 普通搜索
const handleSearch = () => {
  showSearchResult.value = false
  queryParams.value.cardTitle = searchQuery.value || undefined
  queryParams.value.current = 1
  loadCards()
}

// 语义搜索
const handleSemanticSearch = async () => {
  if (!searchQuery.value.trim()) {
    return
  }

  searching.value = true
  
  try {
    // 模拟语义搜索
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 模拟搜索结果
    const query = searchQuery.value.toLowerCase()
    searchResults.value = cardStore.cards
      .filter(card =>
        card.cardTitle?.toLowerCase().includes(query) ||
        card.textContent.toLowerCase().includes(query)
      )
      .map(card => ({
        card,
        score: 85 + Math.floor(Math.random() * 15),
        reason: '内容与搜索词高度相关'
      }))
      .sort((a, b) => b.score - a.score)
      .slice(0, 5)

    if (searchResults.value.length > 0) {
      showSearchResult.value = true
    } else {
      ElMessage.info('没有找到相关卡片')
    }
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}

// 播放卡片
const handlePlay = (card: VoiceCard) => {
  ElMessage.info(`播放卡片：${card.cardTitle}`)
  // TODO: 实现播放功能
}

// 查看详情
const handleDetail = (card: VoiceCard) => {
  router.push(`/card/${card.id}`)
}

// 删除卡片
const handleDelete = async (card: VoiceCard) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除卡片"${card.cardTitle}"吗？`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const success = await cardStore.deleteCard(card.id)
    if (success) {
      ElMessage.success('删除成功')
      // 重新加载当前页
      loadCards()
    }
  } catch (error) {
    // 取消删除
  }
}

// 跳转创建页
const goToCreate = () => {
  router.push('/create-card')
}

// 获取空状态文本
const getEmptyText = (): string => {
  if (searchQuery.value) {
    return '没有找到匹配的卡片'
  }
  if (currentScene.value !== 'all') {
    return `还没有${SceneTagMap[currentScene.value as SceneTag].label}卡片`
  }
  return '还没有声音卡片'
}

// 获取空状态提示
const getEmptyHint = (): string => {
  if (searchQuery.value) {
    return '试试其他关键词或使用语义搜索'
  }
  return '创建你的第一张声音卡片吧'
}

// 初始化
onMounted(() => {
  // 加载第一页数据
  loadCards()
})
</script>

<style scoped>
.card-library-page {
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

/* 筛选区域 */
.filters-section {
  max-width: 1200px;
  margin: 0 auto var(--spacing-2xl);
  background: var(--color-card);
  padding: var(--spacing-xl);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

/* 搜索框 */
.search-box {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.search-box .el-input {
  flex: 1;
}

/* 场景筛选 */
.scene-filters {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
  flex-wrap: wrap;
}

/* 排序 */
.sort-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.sort-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 卡片容器 */
.cards-container {
  max-width: 1200px;
  margin: 0 auto;
  min-height: 400px;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-lg);
}

/* 分页 */
.pagination-section {
  display: flex;
  justify-content: center;
  padding: var(--spacing-2xl) 0;
  margin-top: var(--spacing-xl);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--color-text-tertiary);
}

.empty-icon {
  font-size: 80px;
  margin-bottom: var(--spacing-lg);
  opacity: 0.5;
}

.empty-state h3 {
  font-size: var(--font-size-xl);
  color: var(--color-text-secondary);
  margin: 0 0 var(--spacing-sm);
}

.empty-hint {
  font-size: var(--font-size-base);
  margin: 0 0 var(--spacing-xl);
}

/* 搜索结果 */
.search-results {
  max-height: 600px;
  overflow-y: auto;
}

.search-result-item {
  padding: var(--spacing-lg);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  margin-bottom: var(--spacing-md);
  cursor: pointer;
  transition: all var(--transition-base);
}

.search-result-item:hover {
  background: rgba(255, 154, 98, 0.05);
  transform: translateX(4px);
}

.result-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.result-emoji {
  font-size: 24px;
}

.result-title {
  flex: 1;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.result-content {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0 0 var(--spacing-sm);
}

.result-reason {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-xs);
  color: var(--color-primary);
}

.no-results {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--color-text-tertiary);
}

.no-results-icon {
  font-size: 64px;
  margin-bottom: var(--spacing-md);
  opacity: 0.5;
}
</style>

