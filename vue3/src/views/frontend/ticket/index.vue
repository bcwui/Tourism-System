<template>
  <div class="ticket-list-container">
    <!-- 现代化页面头部 - 参考景点列表页面 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <span class="title-icon">🎫</span>
          精选门票预订
        </h1>
        <p class="page-subtitle">
          探索各地热门景点，预订优惠门票，开启美好旅程
        </p>
      </div>
      <div class="header-actions">
        <el-button
          type="danger"
          size="large"
          @click="goToSeckillZone"
          class="seckill-zone-btn"
        >
          <el-icon><Lightning /></el-icon>
          秒杀专区
          <span v-if="activeSeckillCount > 0" class="seckill-count">{{ activeSeckillCount }}场进行中</span>
        </el-button>
      </div>
    </div>

    <!-- 现代化搜索区域 - 参考景点列表页面 -->
    <div class="search-section">
      <div class="section-container">
        <div class="search-card">
          <div class="search-header">
            <h3 class="search-title">
              <el-icon><Search /></el-icon>
              智能筛选
            </h3>
            <p class="search-subtitle">快速找到心仪的门票</p>
          </div>
          <div class="search-form">
            <div class="search-inputs">
              <div class="search-input-group">
                <el-input
                  v-model="searchForm.ticketName"
                  placeholder="搜索门票名称或景点..."
                  clearable
                  size="large"
                  class="main-search-input"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
              </div>
              <div class="search-actions">
                <el-button
                  type="primary"
                  size="large"
                  @click="searchTickets"
                  class="search-btn"
                >
                  <el-icon><Search /></el-icon>
                  搜索门票
                </el-button>
                <el-button
                  size="large"
                  @click="resetSearch"
                  class="reset-btn"
                >
                  <el-icon><Refresh /></el-icon>
                  重置
                </el-button>
              </div>
            </div>

            <!-- 高级筛选选项 -->
            <div class="advanced-filters">
              <div class="filter-row">
                <div class="filter-group">
                  <label class="filter-label">门票类型</label>
                  <el-select
                    v-model="searchForm.ticketType"
                    placeholder="选择类型"
                    clearable
                    class="filter-select"
                  >
                    <el-option label="成人票" value="成人票" />
                    <el-option label="儿童票" value="儿童票" />
                    <el-option label="学生票" value="学生票" />
                    <el-option label="老人票" value="老人票" />
                  </el-select>
                </div>
                <div class="filter-group">
                  <label class="filter-label">选择景点</label>
                  <el-select
                    v-model="searchForm.scenicId"
                    placeholder="选择景点"
                    clearable
                    filterable
                    remote
                    :remote-method="fetchScenicOptions"
                    :loading="scenicLoading"
                    class="filter-select"
                  >
                    <el-option
                      v-for="item in scenicOptions"
                      :key="item.id"
                      :label="item.name"
                      :value="item.id"
                    />
                  </el-select>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 现代化门票列表区域 - 参考景点列表页面 -->
    <div class="ticket-list-section">
      <div class="section-container">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="8" />
        </div>

        <!-- 空状态 -->
        <div v-else-if="ticketList.length === 0" class="empty-state">
          <div class="empty-icon"></div>
          <h3 class="empty-title">暂无符合条件的门票</h3>
          <p class="empty-desc">试试调整搜索条件或浏览其他门票</p>
          <el-button type="primary" @click="resetSearch" class="empty-action">
            <el-icon><Refresh /></el-icon>
            重新搜索
          </el-button>
        </div>

        <!-- 门票网格 -->
        <div v-else ref="ticketGridRef" class="ticket-grid">
          <div
            v-for="ticket in ticketList"
            :key="ticket.id"
            class="ticket-card"
            @click="goToBooking(ticket.id)"
          >
            <div class="card-header">
              <div class="ticket-type-badge">{{ ticket.ticketType }}</div>
              <div
                v-if="hasSeckill(ticket.id)"
                class="seckill-badge"
                @click.stop="goToSeckill(ticket.id)"
              >
                ⚡秒杀 ¥{{ getSeckillPrice(ticket.id) }}
              </div>
              <div class="card-actions">
                <el-button
                  type="primary"
                  size="small"
                  @click.stop="goToBooking(ticket.id)"
                  class="quick-book-btn"
                >
                  <el-icon><Ticket /></el-icon>
                </el-button>
              </div>
            </div>

            <div class="card-content">
              <h3 class="ticket-name">{{ ticket.ticketName }}</h3>

              <div class="ticket-price-section">
                <div class="price-info">
                  <template v-if="ticket.discountPrice">
                    <span class="discount-price">¥{{ ticket.discountPrice }}</span>
                    <span class="original-price">¥{{ ticket.price }}</span>
                    <span class="discount-badge">特惠</span>
                  </template>
                  <template v-else>
                    <span class="normal-price">¥{{ ticket.price }}</span>
                  </template>
                </div>
                <div class="valid-period">
                  <el-icon><Calendar /></el-icon>
                  <span>{{ ticket.validPeriod }}</span>
                </div>
              </div>

              <div class="ticket-description">{{ ticket.description || '暂无描述' }}</div>

              <div class="card-footer">
                <div class="ticket-meta">
                  <div class="stock-info" v-if="ticket.stock">
                    <el-icon><Goods /></el-icon>
                    <span>余票 {{ ticket.stock }} 张</span>
                  </div>
                </div>
                <el-button
                  type="primary"
                  @click.stop="goToBooking(ticket.id)"
                  class="booking-btn"
                >
                  立即预订
                </el-button>
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { getSeckillList } from '@/api/seckill'
import { Search, Refresh, Ticket, Calendar, Goods, Lightning } from '@element-plus/icons-vue'

const router = useRouter()

// 分页参数
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 门票列表数据
const ticketList = ref([])
const loading = ref(false)

// 秒杀活动数据
const seckillMap = ref({})  // ticketId -> { activityId, seckillPrice }
const activeSeckillCount = ref(0)

// 搜索表单
const searchForm = reactive({
  ticketName: '',
  ticketType: '',
  scenicId: null
})

// 景点选择器数据
const scenicOptions = ref([])
const scenicLoading = ref(false)

// 获取门票列表
const fetchTickets = async () => {
  loading.value = true
  try {
    await request.get('/ticket/page', {
      ticketName: searchForm.ticketName,
      ticketType: searchForm.ticketType,
      scenicId: searchForm.scenicId,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        ticketList.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取门票列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索景点选项
const fetchScenicOptions = async (query) => {
  if (query === '') {
    scenicOptions.value = []
    return
  }
  
  scenicLoading.value = true
  try {
    await request.get('/scenic/page', {
      name: query,
      currentPage: 1,
      size: 20
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        scenicOptions.value = res.records || []
      }
    })
  } catch (error) {
    console.error('获取景点列表失败:', error)
  } finally {
    scenicLoading.value = false
  }
}

// 搜索按钮点击事件
const searchTickets = () => {
  currentPage.value = 1
  fetchTickets()
}

// 重置搜索条件
const resetSearch = () => {
  searchForm.ticketName = ''
  searchForm.ticketType = ''
  searchForm.scenicId = null
  currentPage.value = 1
  fetchTickets()
}

// 分页变化事件
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchTickets()
}

// 前往预订页面
const goToBooking = (ticketId) => {
  router.push(`/ticket/booking/${ticketId}`)
}

// 获取秒杀活动列表
const fetchSeckillList = async () => {
  try {
    const res = await getSeckillList()
    const map = {}
    let count = 0
    if (res && Array.isArray(res)) {
      res.forEach(item => {
        map[item.ticketId] = {
          activityId: item.activityId,
          seckillPrice: item.seckillPrice
        }
        count++
      })
    }
    seckillMap.value = map
    activeSeckillCount.value = count
  } catch (error) {
    console.error('获取秒杀列表失败:', error)
  }
}

// 检查门票是否有秒杀活动
const hasSeckill = (ticketId) => {
  return !!seckillMap.value[ticketId]
}

// 获取秒杀价格
const getSeckillPrice = (ticketId) => {
  const item = seckillMap.value[ticketId]
  return item ? item.seckillPrice : ''
}

// 前往秒杀页面
const goToSeckill = (ticketId) => {
  router.push(`/ticket/seckill/${ticketId}`)
}

const ticketGridRef = ref(null)

// 前往秒杀专区 — 滚动到门票列表
const goToSeckillZone = () => {
  ticketGridRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 页面加载时获取门票列表
onMounted(() => {
  fetchTickets()
  fetchSeckillList()
})
</script>

<style lang="scss" scoped>
.ticket-list-container {
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

  .header-actions {
    flex-shrink: 0;
    margin-left: 24px;

    .seckill-zone-btn {
      background: linear-gradient(45deg, #e53e3e, #c53030);
      border: none;
      border-radius: 12px;
      font-weight: 600;
      font-size: 15px;
      padding: 12px 24px;
      box-shadow: 0 4px 15px rgba(229, 62, 62, 0.4);
      transition: all 0.3s ease;
      position: relative;
      overflow: hidden;

      &::after {
        content: '';
        position: absolute;
        top: -50%;
        left: -50%;
        width: 200%;
        height: 200%;
        background: radial-gradient(circle, rgba(255,255,255,0.3) 0%, transparent 70%);
        animation: shimmer 2s ease-in-out infinite;
      }

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(229, 62, 62, 0.6);
      }

      .seckill-count {
        background: rgba(255, 255, 255, 0.25);
        padding: 2px 8px;
        border-radius: 10px;
        font-size: 12px;
        margin-left: 8px;
        animation: pulse 1.5s ease-in-out infinite;
      }
    }
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



  // 搜索区域
  .search-section {
    background: white;
    padding: 0;
  }

  .search-card {
    background: white;
    border-radius: 16px;
    padding: 24px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    margin-bottom: 30px;
    border: 1px solid #e2e8f0;
  }

  .search-header {
    margin-bottom: 20px;
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
    }
  }

  .search-subtitle {
    font-size: 14px;
    color: #64748b;
    margin: 4px 0 0;
  }

  .search-form {
    .search-inputs {
      display: grid;
      grid-template-columns: 2fr auto;
      gap: 16px;
      align-items: end;
      margin-bottom: 20px;
    }

    .search-input-group {
      display: flex;
      flex-direction: column;
    }

    .main-search-input {
      :deep(.el-input__wrapper) {
        border-radius: 12px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        border: 2px solid #e2e8f0;
        transition: all 0.3s ease;

        &:hover {
          border-color: #667eea;
        }

        &.is-focus {
          border-color: #667eea;
          box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
        }
      }
    }

    .search-actions {
      display: flex;
      gap: 12px;
    }

    .search-btn {
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

    .reset-btn {
      border-radius: 12px;
      border: 2px solid #e2e8f0;
      color: #64748b;
      background: white;

      &:hover {
        border-color: #667eea;
        color: #667eea;
        background: #f8fafc;
      }
    }
  }

  // 高级筛选
  .advanced-filters {
    margin-top: 20px;
  }

  .filter-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 16px;
  }

  .filter-group {
    display: flex;
    flex-direction: column;
  }

  .filter-label {
    font-size: 14px;
    font-weight: 600;
    color: #2d3748;
    margin-bottom: 8px;
  }

  .filter-select {
    :deep(.el-input__wrapper) {
      border-radius: 8px;
      border: 1px solid #e2e8f0;
      transition: all 0.3s ease;

      &:hover {
        border-color: #667eea;
      }

      &.is-focus {
        border-color: #667eea;
      }
    }
  }

  // 门票列表区域
  .ticket-list-section {
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

  // 门票网格布局 - 4卡片一行设计
  .ticket-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 40px;
  }

  .ticket-card {
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
    }
  }

  .card-header {
    position: relative;
    padding: 16px;
    background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .ticket-type-badge {
    padding: 6px 12px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 600;
    background: linear-gradient(45deg, #667eea, #764ba2);
    color: white;
    backdrop-filter: blur(10px);
  }

  .seckill-badge {
    padding: 6px 12px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 700;
    background: linear-gradient(45deg, #fc8181, #e53e3e);
    color: white;
    cursor: pointer;
    animation: pulse 1.5s ease-in-out infinite;
    transition: transform 0.2s ease;

    &:hover {
      transform: scale(1.05);
    }
  }

  .card-actions {
    .quick-book-btn {
      border-radius: 50%;
      width: 36px;
      height: 36px;
      padding: 0;
      background: linear-gradient(45deg, #667eea, #764ba2);
      border: none;
      box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
      transition: all 0.3s ease;

      &:hover {
        transform: scale(1.1);
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.5);
      }
    }
  }

  .card-content {
    padding: 20px;
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .ticket-name {
    margin: 0 0 16px;
    font-size: 18px;
    font-weight: 700;
    color: #2d3748;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    line-height: 1.3;
  }

  .ticket-price-section {
    margin-bottom: 16px;
  }

  .price-info {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;

    .discount-price {
      font-size: 24px;
      font-weight: 700;
      color: #e53e3e;
    }

    .original-price {
      font-size: 16px;
      color: #94a3b8;
      text-decoration: line-through;
    }

    .normal-price {
      font-size: 24px;
      font-weight: 700;
      color: #2d3748;
    }

    .discount-badge {
      padding: 2px 6px;
      border-radius: 8px;
      font-size: 10px;
      font-weight: 600;
      background: linear-gradient(45deg, #f56565, #e53e3e);
      color: white;
    }
  }

  .valid-period {
    display: flex;
    align-items: center;
    font-size: 12px;
    color: #64748b;
    gap: 4px;

    .el-icon {
      color: #667eea;
    }
  }

  .ticket-description {
    font-size: 14px;
    color: #64748b;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    line-height: 1.5;
    margin-bottom: 16px;
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: auto;
    padding-top: 16px;
  }

  .ticket-meta {
    .stock-info {
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

  .booking-btn {
    border-radius: 20px;
    background: linear-gradient(45deg, #667eea, #764ba2);
    border: none;
    font-weight: 600;
    padding: 8px 16px;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
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



  .hover-lift {
    transition: transform 0.3s ease, box-shadow 0.3s ease;
  }

  // 延迟动画类
  .delay-100 { animation-delay: 0.1s; }
  .delay-200 { animation-delay: 0.2s; }
  .delay-300 { animation-delay: 0.3s; }
  .delay-400 { animation-delay: 0.4s; }
  .delay-500 { animation-delay: 0.5s; }
  .delay-600 { animation-delay: 0.6s; }

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

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.75; }
  }

  @keyframes shimmer {
    0% { transform: translateX(-100%) rotate(0deg); }
    100% { transform: translateX(100%) rotate(0deg); }
  }

}
</style>