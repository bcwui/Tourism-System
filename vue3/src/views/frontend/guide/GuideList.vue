<template>
  <div class="guide-list-container">
    <!-- 搜索和筛选区域 -->
    <div class="search-filter-section">
      <div class="section-container">
        <!-- 页面标题和统计 -->
        <div class="page-header">
          <div class="header-content">
            <h1 class="page-title">
              <span class="title-icon">📖</span>
              旅游攻略
            </h1>
            <p class="page-subtitle">
              探索旅行者分享的精彩旅游体验和实用建议
            </p>
          </div>

        </div>

        <!-- 搜索栏 -->
        <div class="search-card">
          <div class="search-header">
            <h3 class="search-title">
              <el-icon><Search /></el-icon>
              智能搜索
            </h3>
          </div>
          <div class="search-form">
            <div class="search-inputs">
              <div class="search-input-group">
                <el-input
                  v-model="searchForm.title"
                  placeholder="搜索攻略标题或内容..."
                  clearable
                  size="large"
                  class="main-search-input"
                  @keyup.enter="handleSearch"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
              </div>
              <div class="search-actions">
                <el-button type="primary" @click="handleSearch" class="search-btn" size="large">
                  <el-icon><Search /></el-icon>
                  搜索
                </el-button>
                <el-button @click="resetSearch" class="reset-btn" size="large">
                  <el-icon><RefreshLeft /></el-icon>
                  重置
                </el-button>
                <el-button
                  type="primary"
                  @click="goEdit"
                  class="publish-btn"
                  size="large"
                >
                  <el-icon><Edit /></el-icon>
                  发布攻略
                </el-button>
              </div>
            </div>

            <!-- 搜索结果提示 -->
            <div v-if="searchForm.title" class="search-tags">
              <el-tag
                v-if="searchForm.title"
                closable
                @close="clearSearchTitle"
                type="info"
                effect="dark"
                class="search-tag"
              >
                关键词: {{ searchForm.title }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 攻略列表区域 -->
    <div class="guide-list-section">
      <div class="section-container">
        <div class="guides-container" v-loading="loading">
          <div class="empty-state" v-if="tableData.length === 0 && !loading">
            <div class="empty-icon">📝</div>
            <h3 class="empty-title">暂无攻略内容</h3>
            <p class="empty-desc">成为第一个分享旅游攻略的人吧！</p>
            <el-button type="primary" @click="goEdit" class="empty-action">
              <el-icon><Edit /></el-icon>
              发布攻略
            </el-button>
          </div>

          <div class="guides-grid" v-else>
            <div
              v-for="item in tableData"
              :key="item.id"
              class="guide-card hover-lift"
              @click="goDetail(item.id)"
            >
              <div class="card-image">
                <img :src="getImageUrl(item.coverImage)" :alt="item.title" v-if="item.coverImage" />
                <div class="default-cover" v-else>
                  <div class="cover-icon">📖</div>
                  <div class="cover-text">旅游攻略</div>
                </div>
                <div class="image-overlay">
                  <div class="overlay-content">
                    <div class="guide-views">
                      <el-icon><View /></el-icon>
                      {{ formatNumber(item.views) }}
                    </div>
                  </div>
                </div>
              </div>

              <div class="card-content">
                <h3 class="guide-title">{{ item.title }}</h3>
                <div class="card-footer">
                  <div class="author-info">
                    <el-avatar
                      :src="getImageUrl(item.userAvatar)"
                      :size="28"
                      class="author-avatar"
                    >
                      <el-icon><User /></el-icon>
                    </el-avatar>
                    <span class="nickname">{{ item.userNickname || '旅行者' + item.userId }}</span>
                  </div>
                  <div class="publish-time">
                    <el-icon><Calendar /></el-icon>
                    <span>{{ formatDate(item.createTime) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 分页 -->
    <div class="pagination-section" v-if="total > 0">
      <div class="section-container">
        <div class="pagination-wrapper">
          <el-pagination
            background
            layout="total, prev, pager, next, jumper"
            :current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            @current-change="handleCurrentChange"
            class="modern-pagination"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'
import {
  Search, View, Calendar, Edit, User, Reading, RefreshLeft
} from '@element-plus/icons-vue'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const router = useRouter()
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12) // 改为12，每行4个，3行
const total = ref(0)
const totalViews = ref(0)
const searchForm = reactive({
  title: ''
})

// 获取图片完整URL
const getImageUrl = (url) => {
  if (!url) return ''
  return url.startsWith('http') ? url : baseAPI + url
}

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

const fetchGuides = async () => {
  loading.value = true
  try {
    await request.get('/guide/page', {
      title: searchForm.title || undefined,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        tableData.value = res.records||[]
        total.value = res.total||0

        // 计算总阅读量
        totalViews.value = tableData.value.reduce((sum, item) => sum + (item.views || 0), 0)
      }
    })
  } catch (error) {
    console.error('获取攻略列表失败', error)
  } finally {
    loading.value = false
  }
}

// 清除搜索标题
const clearSearchTitle = () => {
  searchForm.title = ''
  handleSearch()
}

onMounted(fetchGuides)

const handleSearch = () => {
  currentPage.value = 1
  fetchGuides()
}

const resetSearch = () => {
  searchForm.title = ''
  currentPage.value = 1
  fetchGuides()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchGuides()
}

const goDetail = (id) => {
  router.push(`/guide/detail/${id}`)
}

const goEdit = () => {
  router.push('/guide/edit')
}
</script>

<style lang="scss" scoped>
.guide-list-container {
  min-height: 100vh;
  background: #f8fafc;
  font-family: "思源黑体", "Source Han Sans", "Noto Sans CJK SC", sans-serif;
  color: #333;
}

// 通用容器样式
.section-container {
  max-width: 1300px;
  margin: 0 auto;
  padding: 40px 20px;
}

// 搜索筛选区域
.search-filter-section {
  background: white;
  padding: 0;
}

// 页面头部
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
  padding: 40px 0 20px;
  border-bottom: 1px solid #e2e8f0;
}

.header-content {
  flex: 1;
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 8px;
  color: #2d3748;
  display: flex;
  align-items: center;
  gap: 12px;

  .title-icon {
    font-size: 32px;
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
  }
}

.page-subtitle {
  text-align: left;
  font-size: 16px;
  color: #64748b;
  margin: 0;
  line-height: 1.6;
}



// 搜索卡片样式
.search-card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
  margin-bottom: 20px;
}

.search-header {
  margin-bottom: 24px;
}

.search-title {
  font-size: 20px;
  font-weight: 700;
  color: #2d3748;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;

  .el-icon {
    color: #667eea;
    font-size: 20px;
  }
}

.search-form {
  .search-inputs {
    display: flex;
    gap: 16px;
    align-items: flex-end;
    flex-wrap: wrap;
  }

  .search-input-group {
    flex: 1;
    min-width: 300px;
  }

  .main-search-input {
    :deep(.el-input__wrapper) {
      border-radius: 12px;
      border: 2px solid #e2e8f0;
      transition: all 0.3s ease;

      &:hover {
        border-color: #667eea;
      }

      &.is-focus {
        border-color: #667eea;
        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
      }
    }
  }

  .search-actions {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }

  .search-btn, .reset-btn, .publish-btn {
    border-radius: 12px;
    font-weight: 600;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
    }
  }

  .search-btn {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);

    &:hover {
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
    }
  }

  .publish-btn {
    background: linear-gradient(135deg, #48bb78 0%, #38a169 100%);
    border: none;
    box-shadow: 0 4px 15px rgba(72, 187, 120, 0.3);

    &:hover {
      box-shadow: 0 6px 20px rgba(72, 187, 120, 0.4);
    }
  }

  .reset-btn {
    background: #f7fafc;
    color: #4a5568;
    border: 2px solid #e2e8f0;

    &:hover {
      background: #edf2f7;
      border-color: #cbd5e0;
    }
  }
}

.search-tags {
  margin-top: 16px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.search-tag {
  border-radius: 20px;
  font-size: 12px;
}

// 攻略列表区域
.guide-list-section {
  background: #f8fafc;
  min-height: 50vh;
  padding: 0;
}

.guides-container {
  min-height: 400px;
}

.guides-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-bottom: 40px;
}

// 空状态样式
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #64748b;

  .empty-icon {
    font-size: 64px;
    margin-bottom: 24px;
    opacity: 0.6;
  }

  .empty-title {
    font-size: 24px;
    font-weight: 600;
    margin: 0 0 12px;
    color: #2d3748;
  }

  .empty-desc {
    font-size: 16px;
    margin: 0 0 32px;
    opacity: 0.8;
  }

  .empty-action {
    border-radius: 12px;
    padding: 12px 32px;
    font-weight: 600;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    box-shadow: 0 4px 20px rgba(102, 126, 234, 0.3);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 30px rgba(102, 126, 234, 0.4);
    }
  }
}

// 攻略卡片样式
.guide-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;

  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
    border-color: rgba(102, 126, 234, 0.3);

    .card-image img {
      transform: scale(1.1);
    }

    .image-overlay {
      opacity: 1;
    }
  }

  .card-image {
    position: relative;
    height: 180px;
    overflow: hidden;
    background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
    }

    .default-cover {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;

      .cover-icon {
        font-size: 36px;
        margin-bottom: 8px;
        opacity: 0.8;
      }

      .cover-text {
        font-size: 14px;
        font-weight: 600;
        opacity: 0.9;
      }
    }

    .image-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(to bottom, rgba(0, 0, 0, 0.1) 0%, transparent 50%, rgba(0, 0, 0, 0.4) 100%);
      opacity: 0;
      transition: opacity 0.3s ease;

      .overlay-content {
        position: absolute;
        top: 12px;
        right: 12px;

        .guide-views {
          background: rgba(0, 0, 0, 0.7);
          color: white;
          padding: 4px 8px;
          border-radius: 12px;
          font-size: 12px;
          font-weight: 500;
          display: flex;
          align-items: center;
          gap: 4px;
          backdrop-filter: blur(10px);

          .el-icon {
            font-size: 12px;
          }
        }
      }
    }
  }
  
  .card-content {
    padding: 16px;

    .guide-title {
      font-size: 16px;
      font-weight: 700;
      margin: 0 0 12px;
      color: #2d3748;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      line-height: 1.4;
      min-height: 44px;
      transition: color 0.3s ease;
    }

    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .author-info {
        display: flex;
        align-items: center;
        flex: 1;

        .author-avatar {
          border: 2px solid #e2e8f0;
          transition: all 0.3s ease;
          margin-right: 8px;
        }

        .nickname {
          font-size: 13px;
          font-weight: 600;
          color: #4a5568;
          max-width: 80px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }

      .publish-time {
        font-size: 11px;
        color: #64748b;
        display: flex;
        align-items: center;
        gap: 4px;

        .el-icon {
          font-size: 11px;
        }
      }
    }
  }
}

// 分页样式
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;

  .modern-pagination {
    :deep(.el-pagination) {
      .el-pager li {
        border-radius: 8px;
        margin: 0 4px;
        transition: all 0.3s ease;

        &:hover {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: white;
        }

        &.is-active {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: white;
        }
      }

      .btn-prev, .btn-next {
        border-radius: 8px;
        transition: all 0.3s ease;

        &:hover {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: white;
        }
      }
    }
  }
}

// 动画效果
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}


.hover-lift {
  transition: transform 0.3s ease;

  &:hover {
    transform: translateY(-4px);
  }
}



</style>