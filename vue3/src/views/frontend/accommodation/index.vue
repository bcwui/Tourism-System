<template>
  <div class="accommodation-frontend-container">
    <!-- 搜索和筛选区域 -->
    <div class="search-filter-section">
      <div class="section-container">
        <!-- 页面标题和统计 -->
        <div class="page-header">
          <div class="header-content">
            <h1 class="page-title">
              <span class="title-icon">🏨</span>
              精选住宿推荐
            </h1>
            <p class="page-subtitle">
              发现舒适便捷的住宿选择，让您的旅程更加完美
            </p>
          </div>
    
        </div>

        <!-- 搜索和筛选卡片 -->
        <div class="search-card">
          <div class="search-header">
            <h3 class="search-title">
              <el-icon><Search /></el-icon>
              智能筛选
            </h3>
          </div>
          <div class="search-form">
            <div class="search-inputs">
              <div class="search-input-group">
                <el-input
                  v-model="searchForm.name"
                  placeholder="搜索住宿名称或地址..."
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
              <div class="search-input-group">
                <el-select
                  v-model="filters.scenicId"
                  placeholder="选择景点"
                  clearable
                  size="large"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in scenicOptions"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
              </div>
              <div class="search-actions">
                <el-button type="primary" @click="handleSearch" class="search-btn" size="large">
                  <el-icon><Search /></el-icon>
                  搜索
                </el-button>
                <el-button @click="resetSearch" class="reset-btn" size="large">
                  <el-icon><Refresh /></el-icon>
                  重置
                </el-button>
              </div>
            </div>

            <!-- 高级筛选 -->
            <div class="advanced-filters">
              <div class="filter-row">
                <div class="filter-group">
                  <label class="filter-label">住宿类型</label>
                  <el-select v-model="filters.type" placeholder="选择类型" clearable>
                    <el-option
                      v-for="item in typeOptions"
                      :key="item"
                      :label="item"
                      :value="item"
                    />
                  </el-select>
                </div>
                <div class="filter-group">
                  <label class="filter-label">价格区间</label>
                  <div class="price-range">
                    <el-input v-model="filters.minPrice" placeholder="最低价" />
                    <span class="price-separator">-</span>
                    <el-input v-model="filters.maxPrice" placeholder="最高价" />
                  </div>
                </div>
                <div class="filter-group">
                  <label class="filter-label">最低评分</label>
                  <el-rate v-model="filters.minRating" :max="5" :colors="colors" show-score />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 住宿列表区域 -->
    <div class="accommodation-list-section">
      <div class="section-container">
        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="8" animated />
        </div>

        <div v-else-if="accommodationList && accommodationList.length > 0" class="accommodation-grid">
          <div
            v-for="item in accommodationList"
            :key="item.id"
            class="accommodation-card hover-lift"
            @click="goToDetail(item.id)"
          >
            <div class="card-image">
              <img :src="getImageUrl(item.imageUrl)" :alt="item.name" />
              <div class="image-overlay">
                <div class="overlay-content">
                  <div class="accommodation-rating">
                    <el-icon><Star /></el-icon>
                    {{ item.starLevel || '4.5' }}
                  </div>
                </div>
              </div>
              <div class="card-badges">
                <span v-if="item.type" class="badge type">{{ item.type }}</span>
                <span v-if="item.priceRange" class="badge price">{{ item.priceRange }}</span>
              </div>
            </div>
            <div class="card-content">
              <h3 class="accommodation-name">{{ item.name }}</h3>
              <div class="accommodation-location">
                <el-icon><Location /></el-icon>
                {{ item.address || '地址待更新' }}
              </div>
              <div v-if="item.scenicName" class="accommodation-scenic">
                <el-icon><MapLocation /></el-icon>
                靠近 {{ item.scenicName }}
                <span v-if="item.distance" class="distance">{{ item.distance }}</span>
              </div>
              <p class="accommodation-features">{{ truncateText(item.features || '舒适便捷的住宿环境，为您提供优质的服务体验', 60) }}</p>
              <div class="card-footer">
                <div class="card-meta">
                  <div class="meta-stats">
                    <span class="rating-info">
                      <el-icon><Star /></el-icon>
                      {{ getDisplayRating(item.starLevel) }}
                    </span>
                    <span class="price-info">{{ item.priceRange || '价格面议' }}</span>
                  </div>
                </div>
                <el-button type="primary" size="small" class="detail-btn" @click.stop="goToDetail(item.id)">
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <div class="empty-icon">🏨</div>
          <h3 class="empty-title">暂无住宿信息</h3>
          <p class="empty-desc">试试调整搜索条件或浏览其他选项</p>
          <el-button type="primary" @click="resetSearch" class="empty-action">
            重新搜索
          </el-button>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            background
            layout="total, prev, pager, next, jumper"
            :current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            @current-change="handleCurrentChange"
            @size-change="handleSizeChange"
            class="modern-pagination"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { Location, Star, Picture, Search, Refresh, MapLocation } from '@element-plus/icons-vue'

const router = useRouter()
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

// 数据状态
const loading = ref(false)
const accommodationList = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const scenicOptions = ref([])
const typeOptions = ref([])

// 搜索表单
const searchForm = reactive({
  name: ''
})

// 筛选条件
const filters = reactive({
  scenicId: '',
  type: '',
  minPrice: '',
  maxPrice: '',
  minRating: 0
})

// 评分颜色
const colors = ['#99A9BF', '#F7BA2A', '#FF9900']

// 获取住宿列表
const fetchAccommodations = async () => {
  loading.value = true
  try {
    // 构建查询参数
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value
    }

    // 添加搜索条件
    if (searchForm.name) params.name = searchForm.name

    // 添加筛选条件
    if (filters.scenicId) params.scenicId = filters.scenicId
    if (filters.type) params.type = filters.type
    if (filters.minPrice) params.minPrice = filters.minPrice
    if (filters.maxPrice) params.maxPrice = filters.maxPrice
    if (filters.minRating > 0) params.minRating = filters.minRating

    // 发送请求
    await request.get('/accommodation/page', params, {
      onSuccess: (res) => {
        accommodationList.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取住宿列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取景点列表（用于筛选）
const fetchScenicOptions = async () => {
  try {
    await request.get('/scenic/all', {}, {
      onSuccess: (res) => {
        scenicOptions.value = res||[]
      }
    })
  } catch (error) {
    console.error('获取景点列表失败:', error)
  }
}

// 获取住宿类型列表（用于筛选）
const fetchAccommodationTypes = async () => {
  try {
    await request.get('/accommodation/types', {}, {
      onSuccess: (res) => {
        typeOptions.value = res||[]
      }
    })
  } catch (error) {
    console.error('获取住宿类型列表失败:', error)
  }
}

// 处理图片URL
const getImageUrl = (url) => {
  if (!url) return require('@/assets/images/no-image.png')
  if (url.startsWith('http')) return url
  return baseAPI + url
}

// 搜索处理
const handleSearch = () => {
  currentPage.value = 1
  fetchAccommodations()
}

// 重置搜索和筛选条件
const resetSearch = () => {
  searchForm.name = ''
  filters.scenicId = ''
  filters.type = ''
  filters.minPrice = ''
  filters.maxPrice = ''
  filters.minRating = 0
  currentPage.value = 1
  fetchAccommodations()
}

// 筛选处理（保持兼容性）
const handleFilter = () => {
  handleSearch()
}

// 重置筛选条件（保持兼容性）
const resetFilter = () => {
  resetSearch()
}

// 分页处理
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchAccommodations()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchAccommodations()
}

// 跳转到详情页
const goToDetail = (id) => {
  router.push(`/accommodation/${id}`)
}

// 截取文本
const truncateText = (text, length) => {
  if (!text) return ''
  return text.length > length ? text.substring(0, length) + '...' : text
}

// 获取评分显示
const getDisplayRating = (rating) => {
  if (!rating) return '4.5'
  return parseFloat(rating).toFixed(1)
}

// 初始加载
onMounted(() => {
  fetchScenicOptions()
  fetchAccommodationTypes()
  fetchAccommodations()
})
</script>

<style lang="scss" scoped>
.accommodation-frontend-container {
  min-height: 100vh;
  background: #f8fafc;
  font-family: "思源黑体", "Source Han Sans", "Noto Sans CJK SC", sans-serif;
  color: #333;

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
    font-size: 16px;
    color: #64748b;
    text-align: left;
    width: 100%;
    margin: 0;
    line-height: 1.6;
  }

 

  // 搜索卡片
  .search-card {
    background: white;
    border-radius: 16px;
    padding: 32px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    border: 1px solid #e2e8f0;
    margin-bottom: 40px;
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
      display: grid;
      grid-template-columns: 1fr 200px auto;
      gap: 16px;
      align-items: end;
      margin-bottom: 24px;
    }

    .search-input-group {
      .main-search-input {
        :deep(.el-input__wrapper) {
          border-radius: 12px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          border: 1px solid #e2e8f0;
          transition: all 0.3s ease;

          &:hover {
            border-color: #667eea;
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
          }

          &.is-focus {
            border-color: #667eea;
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
          }
        }
      }
    }

    .search-actions {
      display: flex;
      gap: 12px;
    }

    .search-btn {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
      border-radius: 12px;
      padding: 12px 24px;
      font-weight: 600;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
      }
    }

    .reset-btn {
      border-radius: 12px;
      padding: 12px 24px;
      font-weight: 600;
      border: 2px solid #e2e8f0;
      background: white;
      color: #64748b;
      transition: all 0.3s ease;

      &:hover {
        border-color: #667eea;
        color: #667eea;
        transform: translateY(-2px);
      }
    }
  }

  // 高级筛选
  .advanced-filters {
    border-top: 1px solid #f1f5f9;
    padding-top: 24px;
  }

  .filter-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
  }

  .filter-group {
    .filter-label {
      display: block;
      font-size: 14px;
      font-weight: 600;
      color: #374151;
      margin-bottom: 8px;
    }

    .price-range {
      display: flex;
      align-items: center;
      gap: 8px;

      .price-separator {
        color: #9ca3af;
        font-weight: 500;
      }
    }
  }

  // 住宿列表区域
  .accommodation-list-section {
    background: #f8fafc;
    min-height: 60vh;
  }

  .loading-state {
    background: white;
    border-radius: 16px;
    padding: 40px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  }

  // 住宿网格
  .accommodation-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 24px;
    margin-bottom: 40px;
  }

  .accommodation-card {
    background: white;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    border: 1px solid #e2e8f0;
    cursor: pointer;
    transition: all 0.3s ease;
    position: relative;

    &:hover {
      transform: translateY(-8px);
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
      border-color: #667eea;

      .card-image img {
        transform: scale(1.05);
      }

      .detail-btn {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        transform: translateY(-2px);
      }
    }
  }

  .card-image {
    position: relative;
    height: 200px;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.5s ease;
    }

    .image-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(
        to bottom,
        rgba(0, 0, 0, 0.1) 0%,
        rgba(0, 0, 0, 0.3) 100%
      );
      display: flex;
      align-items: flex-end;
      padding: 16px;
      opacity: 0;
      transition: opacity 0.3s ease;
    }

    &:hover .image-overlay {
      opacity: 1;
    }

    .overlay-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      width: 100%;
    }

    .accommodation-rating {
      background: rgba(255, 255, 255, 0.9);
      backdrop-filter: blur(10px);
      padding: 6px 12px;
      border-radius: 20px;
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 14px;
      font-weight: 600;
      color: #2d3748;

      .el-icon {
        color: #fbbf24;
        font-size: 16px;
      }
    }
  }

  .card-badges {
    position: absolute;
    top: 12px;
    left: 12px;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .badge {
    padding: 4px 8px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 600;
    backdrop-filter: blur(10px);

    &.type {
      background: rgba(102, 126, 234, 0.9);
      color: white;
    }

    &.price {
      background: rgba(239, 68, 68, 0.9);
      color: white;
    }
  }

  .card-content {
    padding: 20px;
  }

  .accommodation-name {
    font-size: 18px;
    font-weight: 700;
    color: #2d3748;
    margin: 0 0 12px;
    line-height: 1.3;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .accommodation-location,
  .accommodation-scenic {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    color: #64748b;
    margin-bottom: 8px;

    .el-icon {
      color: #667eea;
      font-size: 16px;
    }

    .distance {
      color: #10b981;
      font-weight: 600;
    }
  }

  .accommodation-features {
    font-size: 14px;
    color: #64748b;
    line-height: 1.5;
    margin: 12px 0;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid #f1f5f9;
  }

  .card-meta {
    flex: 1;
  }

  .meta-stats {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .rating-info {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 14px;
    font-weight: 600;
    color: #2d3748;

    .el-icon {
      color: #fbbf24;
      font-size: 16px;
    }
  }

  .price-info {
    font-size: 14px;
    font-weight: 600;
    color: #ef4444;
  }

  .detail-btn {
    border-radius: 20px;
    padding: 8px 16px;
    font-size: 14px;
    font-weight: 600;
    transition: all 0.3s ease;
    border: none;
    background: #f1f5f9;
    color: #667eea;

    &:hover {
      color: white;
    }
  }

  // 空状态
  .empty-state {
    text-align: center;
    padding: 80px 20px;
    background: white;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);

    .empty-icon {
      font-size: 64px;
      margin-bottom: 16px;
      opacity: 0.6;
    }

    .empty-title {
      font-size: 24px;
      font-weight: 700;
      color: #2d3748;
      margin: 0 0 8px;
    }

    .empty-desc {
      font-size: 16px;
      color: #64748b;
      margin: 0 0 24px;
    }

    .empty-action {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
      border-radius: 12px;
      padding: 12px 24px;
      font-weight: 600;
    }
  }

  // 分页
  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: 40px;
  }

  .modern-pagination {
    :deep(.el-pagination) {
      .el-pager li {
        border-radius: 8px;
        margin: 0 4px;
        transition: all 0.3s ease;

        &:hover {
          background: #667eea;
          color: white;
        }

        &.is-active {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: white;
        }
      }

      .btn-prev,
      .btn-next {
        border-radius: 8px;
        transition: all 0.3s ease;

        &:hover {
          background: #667eea;
          color: white;
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
    transition: transform 0.3s ease, box-shadow 0.3s ease;
  }

  // 响应式设计
  @media (max-width: 1200px) {
    .section-container {
      padding: 30px 16px;
    }

    .accommodation-grid {
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 20px;
    }
  }

  @media (max-width: 768px) {
    .page-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 20px;
    }

    .header-stats {
      width: 100%;
      justify-content: space-between;
    }

    .stat-card {
      flex: 1;
      min-width: auto;
    }

    .page-title {
      font-size: 28px;
    }

    .search-card {
      padding: 24px 20px;
    }

    .search-inputs {
      grid-template-columns: 1fr;
      gap: 12px;
    }

    .search-actions {
      justify-content: stretch;

      .search-btn,
      .reset-btn {
        flex: 1;
      }
    }

    .filter-row {
      grid-template-columns: 1fr;
      gap: 16px;
    }

    .accommodation-grid {
      grid-template-columns: 1fr;
      gap: 16px;
    }

    .accommodation-card {
      &:hover {
        transform: translateY(-4px);
      }
    }

    .card-content {
      padding: 16px;
    }

    .accommodation-name {
      font-size: 16px;
    }
  }

  @media (max-width: 480px) {
    .section-container {
      padding: 20px 12px;
    }

    .page-title {
      font-size: 24px;
    }

    .search-card {
      padding: 20px 16px;
    }

    .header-stats {
      flex-direction: column;
      gap: 12px;
    }

    .stat-card {
      padding: 12px 16px;

      .stat-number {
        font-size: 20px;
      }
    }

    .card-image {
      height: 160px;
    }

    .card-content {
      padding: 12px;
    }

    .card-footer {
      flex-direction: column;
      align-items: stretch;
      gap: 12px;

      .detail-btn {
        width: 100%;
        justify-content: center;
      }
    }
  }
}
</style>