<template>
  <div class="my-collection-container">
    <!-- 现代化页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <span class="title-icon">❤️</span>
          我的收藏
        </h1>
        <p class="page-subtitle">
          管理您收藏的景点和攻略，随时查看心仪的旅游内容
        </p>
      </div>
    </div>

    <!-- 现代化标签页区域 -->
    <div class="collection-section">
      <div class="section-container">
        <div class="collection-tabs">
          <el-tabs
            v-model="activeTab"
            @tab-change="handleTabChange"
            class="modern-tabs"
          >
            <!-- 景点收藏标签页 -->
            <el-tab-pane label="景点收藏" name="scenic">
              <template #label>
                <div class="tab-label">
                  <el-icon><MapLocation /></el-icon>
                  <span>景点收藏</span>
                  <span class="tab-count">{{ scenicTotal }}</span>
                </div>
              </template>

              <!-- 加载状态 -->
              <div v-if="scenicLoading" class="loading-state">
                <el-skeleton :rows="8" animated />
              </div>

              <!-- 空状态 -->
              <div v-else-if="scenicCollections.length === 0" class="empty-state">
                <div class="empty-icon">🏞️</div>
                <h3 class="empty-title">暂无收藏景点</h3>
                <p class="empty-desc">快去发现心仪的景点并收藏吧</p>
                <el-button type="primary" @click="goToScenicList" class="empty-action">
                  <el-icon><Search /></el-icon>
                  浏览景点
                </el-button>
              </div>

              <!-- 景点收藏网格 -->
              <div v-else class="collection-grid">
                <div
                  v-for="(collection, index) in scenicCollections"
                  :key="collection.id"
                  class="collection-card scenic-collection hover-lift"
                  :class="`delay-${(index % 6 + 1) * 100}`"
                  @click="goToScenicDetail(collection.scenicInfo.id)"
                >
                  <div class="card-image">
                    <img :src="getImageUrl(collection.scenicInfo.imageUrl)" :alt="collection.scenicInfo.name" />
                    <div class="image-overlay">
                      <div class="overlay-content">
                        <div class="collection-time">
                          <el-icon><Clock /></el-icon>
                          {{ formatDate(collection.createTime) }}
                        </div>
                      </div>
                    </div>
                    <div class="card-badges">
                      <span v-if="collection.scenicInfo.categoryInfo" class="badge category">
                        {{ collection.scenicInfo.categoryInfo.name }}
                      </span>
                      <span v-if="collection.scenicInfo.price === 0" class="badge free">免费</span>
                      <span v-else-if="collection.scenicInfo.price > 0" class="badge price">
                        ¥{{ collection.scenicInfo.price }}
                      </span>
                    </div>
                  </div>

                  <div class="card-content">
                    <h3 class="item-name">{{ collection.scenicInfo.name }}</h3>

                    <div class="item-location">
                      <el-icon><Location /></el-icon>
                      <span>{{ collection.scenicInfo.location }}</span>
                    </div>

                    <div class="card-footer">
                      <div class="collection-date">
                        收藏于 {{ formatDate(collection.createTime) }}
                      </div>
                      <div class="card-actions">
                        <el-button
                          type="primary"
                          size="small"
                          @click.stop="goToScenicDetail(collection.scenicInfo.id)"
                          class="detail-btn"
                        >
                          查看详情
                        </el-button>
                        <el-button
                          type="danger"
                          size="small"
                          @click.stop="handleCancelScenicCollection(collection.scenicInfo.id)"
                          class="cancel-btn"
                        >
                          <el-icon><Delete /></el-icon>
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 景点收藏分页 -->
              <div class="pagination-wrapper" v-if="scenicTotal > 0">
                <el-pagination
                  background
                  layout="total, prev, pager, next"
                  :total="scenicTotal"
                  :page-size="scenicPageSize"
                  :current-page="scenicCurrentPage"
                  @current-change="handleScenicPageChange"
                  class="modern-pagination"
                />
              </div>
            </el-tab-pane>

            <!-- 攻略收藏标签页 -->
            <el-tab-pane label="攻略收藏" name="guide">
              <template #label>
                <div class="tab-label">
                  <el-icon><Document /></el-icon>
                  <span>攻略收藏</span>
                  <span class="tab-count">{{ guideTotal }}</span>
                </div>
              </template>

              <!-- 加载状态 -->
              <div v-if="guideLoading" class="loading-state">
                <el-skeleton :rows="8" animated />
              </div>

              <!-- 空状态 -->
              <div v-else-if="guideCollections.length === 0" class="empty-state">
                <div class="empty-icon">📖</div>
                <h3 class="empty-title">暂无收藏攻略</h3>
                <p class="empty-desc">快去发现精彩的旅游攻略并收藏吧</p>
                <el-button type="primary" @click="goToGuideList" class="empty-action">
                  <el-icon><Search /></el-icon>
                  浏览攻略
                </el-button>
              </div>

              <!-- 攻略收藏网格 -->
              <div v-else class="collection-grid">
                <div
                  v-for="(collection, index) in guideCollections"
                  :key="collection.id"
                  class="collection-card guide-collection hover-lift"
                  :class="`delay-${(index % 6 + 1) * 100}`"
                  @click="goToGuideDetail(collection.guideId)"
                >
                  <div class="card-image">
                    <img :src="getImageUrl(collection.guideCoverImage)" :alt="collection.guideTitle" />
                    <div class="image-overlay">
                      <div class="overlay-content">
                        <div class="guide-views">
                          <el-icon><View /></el-icon>
                          {{ collection.guideViews || 0 }}
                        </div>
                      </div>
                    </div>
                    <div class="card-badges">
                      <span class="badge guide">攻略</span>
                    </div>
                  </div>

                  <div class="card-content">
                    <h3 class="item-name">{{ collection.guideTitle }}</h3>

                    <div class="guide-meta">
                      <div class="meta-item">
                        <el-icon><View /></el-icon>
                        <span>{{ collection.guideViews || 0 }} 浏览</span>
                      </div>
                      <div class="meta-item">
                        <el-icon><User /></el-icon>
                        <span>{{ collection.username || collection.userNickname }}</span>
                      </div>
                    </div>

                    <div class="card-footer">
                      <div class="collection-date">
                        收藏于 {{ formatDate(collection.createTime) }}
                      </div>
                      <div class="card-actions">
                        <el-button
                          type="primary"
                          size="small"
                          @click.stop="goToGuideDetail(collection.guideId)"
                          class="detail-btn"
                        >
                          查看详情
                        </el-button>
                        <el-button
                          type="danger"
                          size="small"
                          @click.stop="handleCancelGuideCollection(collection.guideId)"
                          class="cancel-btn"
                        >
                          <el-icon><Delete /></el-icon>
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 攻略收藏分页 -->
              <div class="pagination-wrapper" v-if="guideTotal > 0">
                <el-pagination
                  background
                  layout="total, prev, pager, next"
                  :total="guideTotal"
                  :page-size="guidePageSize"
                  :current-page="guideCurrentPage"
                  @current-change="handleGuidePageChange"
                  class="modern-pagination"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import {
  MapLocation,
  Document,
  Search,
  Clock,
  Location,
  Delete,
  View,
  User
} from '@element-plus/icons-vue'

const router = useRouter()
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const userStore = useUserStore()

// 当前活跃标签页
const activeTab = ref('scenic')

// 景点收藏相关数据
const scenicLoading = ref(false)
const scenicCollections = ref([])
const scenicCurrentPage = ref(1)
const scenicPageSize = ref(12)
const scenicTotal = ref(0)

// 攻略收藏相关数据
const guideLoading = ref(false)
const guideCollections = ref([])
const guideCurrentPage = ref(1)
const guidePageSize = ref(12)
const guideTotal = ref(0)

// 获取用户收藏的景点
const fetchScenicCollections = async () => {
  scenicLoading.value = true
  try {
    await request.get('/scenic-collection/user', {
      currentPage: scenicCurrentPage.value,
      size: scenicPageSize.value,
      userId: userStore.userInfo.id
    }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        scenicCollections.value = data.records || []
        scenicTotal.value = data.total || 0
      }
    })
  } catch (error) {
    console.error('获取收藏景点失败:', error)
  } finally {
    scenicLoading.value = false
  }
}

// 获取用户收藏的攻略
const fetchGuideCollections = async () => {
  guideLoading.value = true
  try {
    await request.get('/collection/page', {
      currentPage: guideCurrentPage.value,
      size: guidePageSize.value,
      userId: userStore.userInfo.id
    }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        guideCollections.value = data.records || []
        guideTotal.value = data.total || 0
      }
    })
  } catch (error) {
    console.error('获取收藏攻略失败:', error)
  } finally {
    guideLoading.value = false
  }
}

// 标签页切换
const handleTabChange = (tabName) => {
  activeTab.value = tabName
  if (tabName === 'scenic' && scenicCollections.value.length === 0) {
    fetchScenicCollections()
  } else if (tabName === 'guide' && guideCollections.value.length === 0) {
    fetchGuideCollections()
  }
}

// 景点收藏分页
const handleScenicPageChange = (page) => {
  scenicCurrentPage.value = page
  fetchScenicCollections()
}

// 攻略收藏分页
const handleGuidePageChange = (page) => {
  guideCurrentPage.value = page
  fetchGuideCollections()
}

// 跳转到景点详情
const goToScenicDetail = (scenicId) => {
  router.push(`/scenic/${scenicId}`)
}

// 跳转到攻略详情
const goToGuideDetail = (guideId) => {
  router.push(`/guide/detail/${guideId}`)
}

// 跳转到景点列表
const goToScenicList = () => {
  router.push('/scenic')
}

// 跳转到攻略列表
const goToGuideList = () => {
  router.push('/guide')
}

// 取消景点收藏
const handleCancelScenicCollection = (scenicId) => {
  ElMessageBox.confirm('确认取消收藏该景点?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/scenic-collection/${scenicId}`, {}, {
        successMsg: '取消收藏成功',
        onSuccess: () => {
          fetchScenicCollections()
        }
      })
    } catch (error) {
      console.error('取消景点收藏失败:', error)
    }
  }).catch(() => {
    // 用户取消操作
  })
}

// 取消攻略收藏
const handleCancelGuideCollection = (guideId) => {
  ElMessageBox.confirm('确认取消收藏该攻略?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/collection/cancel?guideId=${guideId}`, {}, {
        successMsg: '取消收藏成功',
        onSuccess: () => {
          fetchGuideCollections()
        }
      })
    } catch (error) {
      console.error('取消攻略收藏失败:', error)
    }
  }).catch(() => {
    // 用户取消操作
  })
}

// 获取图片完整URL
const getImageUrl = (url) => {
  if (!url) return '/default-image.jpg'
  return url.startsWith('http') ? url : baseAPI + url
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  // 默认加载景点收藏
  fetchScenicCollections()
})
</script>

<style lang="scss" scoped>
.my-collection-container {
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
    }
  }

  .page-subtitle {
    font-size: 16px;
    color: #64748b;
    text-align: left;
    margin: 0;
  }

  .header-stats {
    display: flex;
    gap: 24px;
  }



  // 收藏区域
  .collection-section {
    background: white;
    margin: 0;
    padding-top: 20px;
  }

  .collection-tabs {
    background: white;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    overflow: hidden;
    border: 1px solid #e2e8f0;
  }

  // 现代化标签页样式
  .modern-tabs {
    :deep(.el-tabs__header) {
      margin: 0;
      background: #f8fafc;
      border-bottom: 1px solid #e2e8f0;
    }

    :deep(.el-tabs__nav-wrap) {
      padding: 0 24px;
    }

    :deep(.el-tabs__item) {
      padding: 20px 0;
      font-size: 16px;
      font-weight: 600;
      color: #64748b;
      border: none;
      margin-right: 40px;

      &.is-active {
        color: #667eea;
      }

      &:hover {
        color: #667eea;
      }
    }

    :deep(.el-tabs__active-bar) {
      background: linear-gradient(45deg, #667eea, #764ba2);
      height: 3px;
    }

    :deep(.el-tabs__content) {
      padding: 40px 24px;
    }

    .tab-label {
      display: flex;
      align-items: center;
      gap: 8px;

      .tab-count {
        background: #667eea;
        color: white;
        padding: 2px 8px;
        border-radius: 12px;
        font-size: 12px;
        font-weight: 600;
      }
    }
  }

  // 加载状态
  .loading-state {
    padding: 40px 20px;
  }

  // 空状态
  .empty-state {
    text-align: center;
    padding: 80px 20px;

    .empty-icon {
      font-size: 64px;
      margin-bottom: 20px;
    }

    .empty-title {
      font-size: 24px;
      font-weight: 600;
      color: #2d3748;
      margin: 0 0 8px;
    }

    .empty-desc {
      font-size: 16px;
      color: #64748b;
      margin: 0 0 24px;
    }

    .empty-action {
      background: linear-gradient(45deg, #667eea, #764ba2);
      border: none;
      border-radius: 20px;
      padding: 12px 24px;
      font-weight: 600;
    }
  }

  // 收藏网格布局
  .collection-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 40px;
  }

  .collection-card {
    border-radius: 16px;
    overflow: hidden;
    background: #fff;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    transition: all 0.4s ease;
    cursor: pointer;
    position: relative;
    height: 100%;
    display: flex;
    flex-direction: column;

    &:hover {
      transform: translateY(-8px);
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);

      .card-image img {
        transform: scale(1.1);
      }

      .image-overlay {
        opacity: 1;
      }
    }
  }

  .card-image {
    height: 220px;
    overflow: hidden;
    position: relative;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.6s ease;
    }
  }

  .image-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(to bottom, transparent 0%, rgba(0, 0, 0, 0.7) 100%);
    opacity: 0;
    transition: opacity 0.3s ease;
    display: flex;
    align-items: flex-end;
    padding: 20px;
  }

  .overlay-content {
    color: white;

    .collection-time,
    .guide-views {
      display: flex;
      align-items: center;
      font-size: 14px;
      font-weight: 600;
      gap: 4px;

      .el-icon {
        color: #ffd700;
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

    &.category {
      background: linear-gradient(45deg, #667eea, #764ba2);
      color: white;
    }

    &.free {
      background: linear-gradient(45deg, #10b981, #059669);
      color: white;
    }

    &.price {
      background: rgba(255, 255, 255, 0.9);
      color: #333;
    }

    &.guide {
      background: linear-gradient(45deg, #f59e0b, #d97706);
      color: white;
    }
  }

  .card-content {
    padding: 20px;
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .item-name {
    margin: 0 0 12px;
    font-size: 18px;
    font-weight: 700;
    color: #2d3748;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    line-height: 1.3;
  }

  .item-location {
    display: flex;
    align-items: center;
    font-size: 14px;
    color: #64748b;
    margin-bottom: 12px;
    gap: 4px;

    .el-icon {
      color: #667eea;
    }
  }

  .guide-meta {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;

    .meta-item {
      display: flex;
      align-items: center;
      font-size: 12px;
      color: #64748b;
      gap: 4px;

      .el-icon {
        color: #667eea;
      }
    }
  }

  .card-footer {
    margin-top: auto;
    padding-top: 16px;

    .collection-date {
      font-size: 12px;
      color: #94a3b8;
      margin-bottom: 12px;
    }

    .card-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;

      .detail-btn {
        border-radius: 20px;
        background: linear-gradient(45deg, #667eea, #764ba2);
        border: none;
        font-weight: 600;
        padding: 8px 16px;
        flex: 1;

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
        }
      }

      .cancel-btn {
        border-radius: 50%;
        width: 36px;
        height: 36px;
        padding: 0;
        background: #f56565;
        border: none;
        color: white;

        &:hover {
          background: #e53e3e;
          transform: scale(1.1);
        }
      }
    }
  }

  // 分页样式
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
          background: linear-gradient(45deg, #667eea, #764ba2);
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



}
</style>