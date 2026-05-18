<template>
  <div class="my-guide-container">
    <!-- 现代化页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <span class="title-icon">📖</span>
          我的攻略
        </h1>
        <p class="page-subtitle">
          管理您已发布的旅游攻略内容，分享您的旅行经验
        </p>
      </div>
    </div>

    <!-- 操作区域 -->
    <div class="action-section">
      <div class="section-container">
        <div class="action-bar">
          <div class="action-left">
            <h3 class="section-title">
              <el-icon><Document /></el-icon>
              攻略管理
            </h3>
          </div>
          <div class="action-right">
            <el-button
              type="primary"
              @click="goEdit"
              :icon="Edit"
              class="publish-btn"
              size="large"
            >
              发布新攻略
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 攻略列表区域 -->
    <div class="guide-list-section">
      <div class="section-container">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="8" animated />
        </div>

        <!-- 空状态 -->
        <div v-else-if="tableData.length === 0" class="empty-state">
          <div class="empty-icon">📝</div>
          <h3 class="empty-title">您还没有发布任何攻略</h3>
          <p class="empty-desc">分享您的旅行经验，帮助更多人发现美好</p>
          <el-button type="primary" @click="goEdit" class="empty-action">
            <el-icon><Edit /></el-icon>
            立即发布
          </el-button>
        </div>

        <!-- 攻略网格 -->
        <div v-else class="guide-grid">
          <div
            v-for="(guide, index) in tableData"
            :key="guide.id"
            class="guide-card"
            :class="`delay-${(index % 6 + 1) * 100}`"
            @click="viewGuide(guide)"
          >
            <div class="card-image">
              <img :src="getImageUrl(guide.coverImage)" :alt="guide.title" />
              <div class="image-overlay">
                <div class="overlay-content">
                  <div class="guide-views">
                    <el-icon><View /></el-icon>
                    {{ guide.views || 0 }}
                  </div>
                </div>
              </div>
              <div class="card-badges">
                <span v-if="isNew(guide.createTime)" class="badge new">新</span>
                <span class="badge guide">攻略</span>
              </div>
            </div>

            <div class="card-content">
              <h3 class="guide-title">{{ guide.title }}</h3>

              <div class="guide-meta">
                <div class="meta-item">
                  <el-icon><View /></el-icon>
                  <span>{{ guide.views || 0 }} 浏览</span>
                </div>
                <div class="meta-item">
                  <el-icon><Calendar /></el-icon>
                  <span>{{ formatDate(guide.createTime) }}</span>
                </div>
              </div>

              <div class="card-footer">
                <div class="guide-status">
                  <el-tag v-if="isNew(guide.createTime)" type="success" size="small">
                    最新发布
                  </el-tag>
                  <el-tag v-else type="info" size="small">
                    已发布
                  </el-tag>
                </div>
                <div class="card-actions">
                  <el-button
                    type="primary"
                    size="small"
                    @click.stop="viewGuide(guide)"
                    class="view-btn"
                  >
                    查看
                  </el-button>
                  <el-button
                    size="small"
                    @click.stop="goEdit(guide)"
                    class="edit-btn"
                  >
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    @click.stop="deleteGuide(guide)"
                    class="delete-btn"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 现代化分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="currentPage"
            @current-change="handleCurrentChange"
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
import { ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'
import { useUserStore } from '@/store/user'
import {Edit,View,Calendar,Delete,EditPen} from '@element-plus/icons-vue'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const router = useRouter()
const userStore = useUserStore()
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 计算总浏览量
const totalViews = computed(() => {
  return tableData.value.reduce((sum, guide) => sum + (guide.views || 0), 0)
})

// 计算本月新增攻略数量
const newGuideCount = computed(() => {
  const now = new Date()
  const currentMonth = now.getMonth()
  const currentYear = now.getFullYear()

  return tableData.value.filter(guide => {
    if (!guide.createTime) return false
    const createDate = new Date(guide.createTime)
    return createDate.getMonth() === currentMonth && createDate.getFullYear() === currentYear
  }).length
})

// 获取图片完整URL
const getImageUrl = (url) => {
  if (!url) return '/default-guide-cover.jpg'
  return url.startsWith('http') ? url : baseAPI + url
}

// 判断是否是新发布的攻略（7天内）
const isNew = (dateString) => {
  if (!dateString) return false
  const publishDate = new Date(dateString)
  const now = new Date()
  const diffTime = now - publishDate
  const diffDays = diffTime / (1000 * 60 * 60 * 24)
  return diffDays < 7
}

const fetchGuides = async () => {
  loading.value = true
  try {
    await request.get('/guide/page', {
      userId: userStore.userInfo?.id,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        tableData.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取我的攻略列表失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchGuides)

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchGuides()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchGuides()
}

const handleRowClick = (row) => {
  viewGuide(row)
}

const viewGuide = (row) => {
  router.push(`/guide/detail/${row.id}`)
}

const goEdit = (row) => {
  if (row) {
    router.push({ name: 'GuideEdit', query: { id: row.id } })
  } else {
    router.push({ name: 'GuideEdit' })
  }
}

const deleteGuide = (row) => {
  ElMessageBox.confirm('确定要删除该攻略吗？删除后无法恢复！', '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
    closeOnClickModal: false
  }).then(async () => {
    try {
      await request.delete(`/guide/delete/${row.id}`, {
        successMsg: '删除成功',
        onSuccess: () => fetchGuides()
      })
    } catch (error) {
      console.error('删除攻略失败', error)
    }
  }).catch(() => {})
}
</script>

<style lang="scss" scoped>
.my-guide-container {
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
    text-align: left;
    font-size: 16px;
    color: #64748b;
    margin: 0;
  }



  // 操作区域
  .action-section {
    background: white;
    margin: 0;
    padding-top: 20px;
  }

  .action-bar {
    background: white;
    border-radius: 16px;
    padding: 24px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    margin-bottom: 30px;
    border: 1px solid #e2e8f0;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .action-left {
      .section-title {
        font-size: 20px;
        font-weight: 700;
        color: #2d3748;
        margin: 0;
        display: flex;
        align-items: center;
        gap: 8px;

        .el-icon {
          color: #667eea;
        }
      }
    }

    .publish-btn {
      background: linear-gradient(45deg, #667eea, #764ba2);
      border: none;
      border-radius: 12px;
      font-weight: 600;
      box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
      }
    }
  }

  // 攻略列表区域
  .guide-list-section {
    background: white;
    margin: 0;
    padding-top: 20px;
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

  // 攻略网格布局
  .guide-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 40px;
  }

  .guide-card {
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

    &.new {
      background: linear-gradient(45deg, #10b981, #059669);
      color: white;
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

  .guide-title {
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

    .guide-status {
      margin-bottom: 12px;
    }

    .card-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;

      .view-btn {
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

      .edit-btn {
        border-radius: 50%;
        width: 36px;
        height: 36px;
        padding: 0;
        background: #f59e0b;
        border: none;
        color: white;

        &:hover {
          background: #d97706;
          transform: scale(1.1);
        }
      }

      .delete-btn {
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