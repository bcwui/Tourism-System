<template>
  <div class="smart-search" ref="searchContainer">
    <el-input
      v-model="searchKeyword"
      :placeholder="placeholder"
      size="large"
      class="search-input"
      @input="handleInput"
      @keydown="handleKeydown"
      @focus="handleFocus"
      @blur="handleBlur"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
      <template #suffix>
        <el-button 
          type="primary" 
          @click="handleSearch" 
          class="search-btn"
          :loading="searching"
        >
          搜索
        </el-button>
      </template>
    </el-input>

    <!-- 搜索建议下拉框 -->
    <div 
      v-show="showSuggestions && (suggestions.scenics.length > 0 || suggestions.guides.length > 0)"
      class="suggestions-dropdown"
      @mousedown.prevent
    >
      <!-- 景点建议 -->
      <div v-if="suggestions.scenics.length > 0" class="suggestion-section">
        <div class="section-title">
          <el-icon><Location /></el-icon>
          <span>景点推荐</span>
        </div>
        <div 
          v-for="(item, index) in suggestions.scenics" 
          :key="`scenic-${item.id}`"
          class="suggestion-item"
          :class="{ active: selectedIndex === index }"
          @click="goToScenicDetail(item.id)"
          @mouseenter="selectedIndex = index"
        >
          <div class="item-image">
            <img
              :src="getImageUrl(item.imageUrl)"
              :alt="item.name"
              @error="$event.target.src = 'https://via.placeholder.com/48x48?text=No+Image'"
            />
          </div>
          <div class="item-content">
            <div class="item-title">{{ item.name }}</div>
            <div class="item-subtitle">
              <el-icon><Location /></el-icon>
              {{ item.location || '未知地区' }}
              <span v-if="item.price && item.price > 0" class="price">¥{{ item.price }}</span>
              <span v-else class="price free">免费</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 攻略建议 -->
      <div v-if="suggestions.guides.length > 0" class="suggestion-section">
        <div class="section-title">
          <el-icon><Document /></el-icon>
          <span>攻略推荐</span>
        </div>
        <div 
          v-for="(item, index) in suggestions.guides" 
          :key="`guide-${item.id}`"
          class="suggestion-item"
          :class="{ active: selectedIndex === (suggestions.scenics.length + index) }"
          @click="goToGuideDetail(item.id)"
          @mouseenter="selectedIndex = suggestions.scenics.length + index"
        >
          <div class="item-image">
            <img
              :src="getImageUrl(item.coverImage)"
              :alt="item.title"
              @error="$event.target.src = 'https://via.placeholder.com/48x48?text=No+Image'"
            />
          </div>
          <div class="item-content">
            <div class="item-title">{{ item.title }}</div>
            <div class="item-subtitle">
              <el-icon><User /></el-icon>
              {{ item.userNickname || '匿名用户' }}
              <span class="views">{{ item.views || 0 }}阅读</span>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Location, Document, User } from '@element-plus/icons-vue'
import request from '@/utils/request'

const props = defineProps({
  placeholder: {
    type: String,
    default: '搜索目的地、景点、攻略...'
  }
})

const emit = defineEmits(['search'])

const router = useRouter()
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

const searchKeyword = ref('')
const showSuggestions = ref(false)
const selectedIndex = ref(-1)
const searching = ref(false)
const searchContainer = ref(null)

const suggestions = reactive({
  scenics: [],
  guides: []
})

// 防抖定时器
let debounceTimer = null

// 获取图片完整URL
const getImageUrl = (url) => {
  if (!url) return 'https://via.placeholder.com/48x48?text=No+Image'
  return url.startsWith('http') ? url : baseAPI + url
}

// 防抖搜索建议
const handleInput = (value) => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  
  if (!value.trim()) {
    showSuggestions.value = false
    return
  }
  
  debounceTimer = setTimeout(() => {
    fetchSuggestions(value.trim())
  }, 300)
}

// 获取搜索建议
const fetchSuggestions = async (keyword) => {
  if (!keyword) return

  try {
    // 并行请求景点和攻略建议
    const [scenicResponse, guideResponse] = await Promise.all([
      request.get('/scenic/suggestions', {
        keyword,
        limit: 3
      }, {
        showDefaultMsg: false
      }).catch((error) => {
        console.log('景点建议API错误:', error)
        return []
      }),

      request.get('/guide/suggestions', {
        keyword,
        limit: 3
      }, {
        showDefaultMsg: false
      }).catch((error) => {
        console.log('攻略建议API错误:', error)
        return []
      })
    ])

    console.log('原始景点响应:', scenicResponse)
    console.log('原始攻略响应:', guideResponse)

    // 处理景点数据
    suggestions.scenics = Array.isArray(scenicResponse) ? scenicResponse : (scenicResponse?.data || [])
    // 处理攻略数据
    suggestions.guides = Array.isArray(guideResponse) ? guideResponse : (guideResponse?.data || [])

    console.log('景点建议:', suggestions.scenics)
    console.log('攻略建议:', suggestions.guides)

    showSuggestions.value = (suggestions.scenics.length > 0 || suggestions.guides.length > 0)
    selectedIndex.value = -1

  } catch (error) {
    console.error('获取搜索建议失败:', error)
    // 如果API失败，至少显示一个基本的搜索提示
    suggestions.scenics = []
    suggestions.guides = []
    showSuggestions.value = false
  }
}

// 键盘事件处理
const handleKeydown = (event) => {
  const totalItems = suggestions.scenics.length + suggestions.guides.length
  
  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      selectedIndex.value = Math.min(selectedIndex.value + 1, totalItems - 1)
      break
    case 'ArrowUp':
      event.preventDefault()
      selectedIndex.value = Math.max(selectedIndex.value - 1, -1)
      break
    case 'Enter':
      event.preventDefault()
      if (selectedIndex.value >= 0) {
        selectSuggestion(selectedIndex.value)
      } else {
        handleSearch()
      }
      break
    case 'Escape':
      showSuggestions.value = false
      selectedIndex.value = -1
      break
  }
}

// 选择建议项
const selectSuggestion = (index) => {
  const scenicCount = suggestions.scenics.length
  
  if (index < scenicCount) {
    // 选择景点
    const scenic = suggestions.scenics[index]
    goToScenicDetail(scenic.id)
  } else {
    // 选择攻略
    const guide = suggestions.guides[index - scenicCount]
    goToGuideDetail(guide.id)
  }
}

// 跳转到景点详情
const goToScenicDetail = (id) => {
  router.push(`/scenic/${id}`)
  hideSuggestions()
}

// 跳转到攻略详情
const goToGuideDetail = (id) => {
  router.push(`/guide/detail/${id}`)
  hideSuggestions()
}

// 执行搜索
const handleSearch = () => {
  if (!searchKeyword.value.trim()) return
  
  searching.value = true
  router.push(`/scenic?search=${encodeURIComponent(searchKeyword.value.trim())}`)
  hideSuggestions()
  
  setTimeout(() => {
    searching.value = false
  }, 1000)
  
  emit('search', searchKeyword.value.trim())
}

// 获得焦点
const handleFocus = () => {
  if (searchKeyword.value.trim() && (suggestions.scenics.length > 0 || suggestions.guides.length > 0)) {
    showSuggestions.value = true
  }
}

// 失去焦点
const handleBlur = () => {
  // 延迟隐藏，允许点击建议项
  setTimeout(() => {
    hideSuggestions()
  }, 200)
}

// 隐藏建议
const hideSuggestions = () => {
  showSuggestions.value = false
  selectedIndex.value = -1
}

// 暴露方法给父组件
defineExpose({
  focus: () => {
    nextTick(() => {
      const input = searchContainer.value?.querySelector('input')
      input?.focus()
    })
  },
  clear: () => {
    searchKeyword.value = ''
    hideSuggestions()
  }
})
</script>

<style lang="scss" scoped>
.smart-search {
  position: relative;
  width: 100%;
  max-width: 600px;
  z-index: 2000;
}

.search-input {
  :deep(.el-input__wrapper) {
    border-radius: 50px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
    border: none;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    padding: 8px 20px;
    
    .el-input__inner {
      font-size: 16px;
      color: #333;
      
      &::placeholder {
        color: #999;
      }
    }
  }
  
  .search-btn {
    border-radius: 50px;
    background: linear-gradient(45deg, #667eea, #764ba2);
    border: none;
    padding: 12px 24px;
    font-weight: 600;
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;
    
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
      transition: left 0.5s;
    }
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
      
      &::before {
        left: 100%;
      }
    }
  }
}

.suggestions-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  z-index: 99999!important;
  margin-top: 8px;
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;  
  backdrop-filter: blur(10px);
}

.suggestion-section {
    z-index: 99999!important;
  &:not(:last-child) {
    border-bottom: 1px solid #f1f5f9;
  }
}

.section-title {
  display: flex;
  align-items: center;
  padding: 12px 16px 8px;
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  background: #f8fafc;
  
  .el-icon {
    margin-right: 6px;
    color: #667eea;
  }
}

.suggestion-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover,
  &.active {
    background: #f8fafc;
  }
  
  .item-image {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    overflow: hidden;
    margin-right: 12px;
    flex-shrink: 0;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  
  .item-content {
    flex: 1;
    min-width: 0;
    
    .item-title {
      font-size: 15px;
      font-weight: 600;
      color: #2d3748;
      margin-bottom: 4px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    
    .item-subtitle {
      font-size: 13px;
      color: #64748b;
      display: flex;
      align-items: center;
      
      .el-icon {
        margin-right: 4px;
        font-size: 12px;
      }
      
      .price {
        margin-left: auto;
        font-weight: 600;
        color: #667eea;
        
        &.free {
          color: #10b981;
        }
      }
      
      .views {
        margin-left: auto;
        color: #94a3b8;
      }
    }
  }
}



// 滚动条样式
.suggestions-dropdown::-webkit-scrollbar {
  width: 6px;
}

.suggestions-dropdown::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.suggestions-dropdown::-webkit-scrollbar-thumb {
  background: #cbd5e0;
  border-radius: 3px;
  
  &:hover {
    background: #a0aec0;
  }
}
</style>
