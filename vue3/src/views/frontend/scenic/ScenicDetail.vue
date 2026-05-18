<!-- eslint-disable -->
<template>
  <div class="scenic-detail-container">
    <!-- 主要内容区域 -->
    <div class="detail-hero-section">
      <div class="hero-image-container">
        <div class="image-wrapper">
          <img :src="getImageUrl(scenic.imageUrl)" :alt="scenic.name" class="hero-image" />
          <div class="image-overlay">
            <div class="overlay-gradient"></div>
            <div class="hero-content">
              <div class="breadcrumb">
                <el-breadcrumb separator="/">
                  <el-breadcrumb-item @click="$router.push('/')">首页</el-breadcrumb-item>
                  <el-breadcrumb-item @click="$router.push('/scenic')">景点列表</el-breadcrumb-item>
                  <el-breadcrumb-item>{{ scenic.name }}</el-breadcrumb-item>
                </el-breadcrumb>
              </div>
              <h1 class="scenic-title">{{ scenic.name }}</h1>
              <div class="scenic-meta">
                <div class="meta-item">
                  <el-icon><Location /></el-icon>
                  <span>{{ scenic.location }}</span>
                </div>
                <div class="meta-item" v-if="scenic.categoryInfo">
                  <el-icon><CollectionTag /></el-icon>
                  <span>{{ scenic.categoryInfo.name }}</span>
                </div>
                <div class="meta-item rating">
                  <el-icon><Star /></el-icon>
                  <span>{{ getDisplayRating(scenic.rating) }}</span>
                  <span class="rating-text">({{ formatReviewCount(scenic.reviewCount) }})</span>
                </div>
              </div>
              <div class="action-buttons">
                <el-button
                  :type="isCollected ? 'danger' : 'primary'"
                  size="large"
                  :loading="collectionLoading"
                  @click="handleCollection"
                  class="collection-btn"
                >
                  <el-icon>
                    <StarFilled v-if="isCollected" />
                    <Star v-else />
                  </el-icon>
                  {{ isCollected ? '已收藏' : '收藏景点' }}
                </el-button>
                <el-button size="large" class="share-btn" @click="handleShare">
                  <el-icon><Share /></el-icon>
                  分享
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 详细信息区域 -->
    <div class="detail-content">
      <div class="section-container">
        <div class="content-grid">
          <!-- 左侧主要内容 -->
          <div class="main-content">
            <!-- 景点描述 -->
            <div class="info-card description-card">
              <h3 class="card-title">
                <el-icon><Document /></el-icon>
                景点介绍
              </h3>
              <div class="description-content">{{ scenic.description }}</div>
            </div>
            <!-- 天气信息卡片 -->
            <div class="info-card weather-card" v-if="weather.now || weatherLoading || weatherForecast.length > 0">
              <h3 class="card-title">
                <el-icon><Sunny /></el-icon>
                天气信息
              </h3>

              <!-- 当前天气 -->
              <div v-if="weather.now" class="weather-content">
                <div class="current-weather">
                  <div class="weather-main">
                    <div class="weather-temp">{{ weather.now.temp }}°C</div>
                    <div class="weather-text">{{ weather.now.text }}</div>
                  </div>
                  <div class="weather-details">
                    <div class="detail-item">
                      <span class="label">体感温度</span>
                      <span class="value">{{ weather.now.feelsLike }}°C</span>
                    </div>
                    <div class="detail-item">
                      <span class="label">湿度</span>
                      <span class="value">{{ weather.now.humidity }}%</span>
                    </div>
                    <div class="detail-item">
                      <span class="label">风向</span>
                      <span class="value">{{ weather.now.windDir }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="label">风速</span>
                      <span class="value">{{ weather.now.windSpeed }} km/h</span>
                    </div>
                  </div>
                </div>
                <div class="weather-update">
                  更新时间: {{ formatWeatherTime(weather.updateTime) }}
                </div>
              </div>

              <!-- 未来天气预报 -->
              <div v-if="weatherForecast.length > 0" class="forecast-section">
                <h4 class="forecast-title">未来天气</h4>
                <div class="forecast-list">
                  <div
                    v-for="(day, index) in weatherForecast.slice(0, 4)"
                    :key="index"
                    class="forecast-item"
                  >
                    <div class="forecast-date">{{ formatForecastDate(day.fxDate) }}</div>
                    <div class="forecast-weather">
                      <div class="weather-desc">{{ day.textDay }}</div>
                    </div>
                    <div class="forecast-temp">
                      <span class="temp-high">{{ day.tempMax }}°</span>
                      <span class="temp-low">{{ day.tempMin }}°</span>
                    </div>
                  </div>
                </div>
              </div>

              <div v-else-if="weatherLoading" class="weather-loading">
                <el-icon class="loading-icon"><Loading /></el-icon>
                <span>获取天气信息中...</span>
              </div>
            </div>
          </div>

          <!-- 右侧信息栏 -->
          <div class="sidebar-content">
            <!-- 基本信息卡片 -->
            <div class="info-card basic-info-card">
              <h3 class="card-title">
                <el-icon><InfoFilled /></el-icon>
                基本信息
              </h3>
              <div class="info-list">
                <div class="info-item">
                  <div class="info-label">
                    <el-icon><Money /></el-icon>
                    门票价格
                  </div>
                  <div class="info-value price-value">
                    <span v-if="scenic.price === 0" class="free-tag">免费</span>
                    <span v-else class="price">¥{{ scenic.price }}</span>
                  </div>
                </div>
                <div class="info-item">
                  <div class="info-label">
                    <el-icon><Timer /></el-icon>
                    开放时间
                  </div>
                  <div class="info-value">{{ scenic.openingHours || '全天开放' }}</div>
                </div>
                <div class="info-item" v-if="scenic.longitude && scenic.latitude">
                  <div class="info-label">
                    <el-icon><Location /></el-icon>
                    地理坐标
                  </div>
                  <div class="info-value coordinates" @click="copyCoordinates">
                    {{ scenic.longitude }}, {{ scenic.latitude }}
                    <el-icon class="copy-icon"><CopyDocument /></el-icon>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>



    <!-- 地图位置区域 -->
    <div class="map-section">
      <div class="section-container">
        <h2 class="section-title">
          <el-icon><Location /></el-icon>
          地理位置
        </h2>
        <div class="map-wrapper">
          <div id="map-container" class="map-container"></div>
        </div>
      </div>
    </div>

    <!-- 门票预订区域 -->
    <div class="ticket-section">
      <div class="section-container">
        <h2 class="section-title">
          <el-icon><Ticket /></el-icon>
          门票预订
        </h2>
        <div v-loading="ticketLoading" class="ticket-content">
          <div v-if="tickets.length === 0" class="empty-state">
            <div class="empty-icon">🎫</div>
            <h3 class="empty-title">暂无可预订门票</h3>
            <p class="empty-desc">该景点暂时没有开放在线预订服务</p>
          </div>
          <div v-else class="ticket-grid">
            <div
              v-for="ticket in tickets"
              :key="ticket.id"
              class="ticket-card"
            >
              <div class="ticket-header">
                <h4 class="ticket-name">{{ ticket.ticketName }}</h4>
                <div class="ticket-type-badge">{{ ticket.ticketType }}</div>
              </div>

              <div class="ticket-price">
                <template v-if="ticket.discountPrice">
                  <div class="price-original">¥{{ ticket.price }}</div>
                  <div class="price-discount">¥{{ ticket.discountPrice }}</div>
                  <div class="discount-tag">优惠</div>
                </template>
                <template v-else>
                  <div class="price-normal">¥{{ ticket.price }}</div>
                </template>
              </div>

              <div class="ticket-details">
                <div class="detail-item">
                  <el-icon><Timer /></el-icon>
                  <span>{{ ticket.validPeriod }}</span>
                </div>
                <div class="detail-item">
                  <el-icon><Tickets /></el-icon>
                  <span>剩余 {{ ticket.stock }} 张</span>
                </div>
              </div>

              <div class="ticket-description" v-if="ticket.description">
                {{ ticket.description }}
              </div>

              <div class="ticket-actions">
                <el-button
                  type="primary"
                  size="large"
                  @click="goToBooking(ticket.id)"
                  class="booking-btn"
                  :disabled="ticket.stock === 0"
                >
                  {{ ticket.stock === 0 ? '已售罄' : '立即预订' }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 评论区域 -->
    <div class="comment-section">
      <div class="section-container">
        <h2 class="section-title">
          <el-icon><ChatDotRound /></el-icon>
          游客评价
        </h2>
        <div class="comment-wrapper">
          <CommentList />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/* global AMap */
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import CommentList from '@/views/frontend/comment/CommentList.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import axios from 'axios'
import {
  Location, CollectionTag, Money, Timer, Sunny, Loading, Star, StarFilled,
  Document, InfoFilled, CopyDocument, Share, Ticket, Tickets, ChatDotRound
} from '@element-plus/icons-vue'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const scenic = ref({})
const tickets = ref([])
const ticketLoading = ref(false)
// 收藏相关状态
const isCollected = ref(false)
const collectionLoading = ref(false)

// 检查用户是否登录
const isLoggedIn = computed(() => userStore.isLoggedIn)

// 获取图片完整URL
const getImageUrl = (url) => {
  if (!url) return '/default-scenic.jpg'
  return url.startsWith('http') ? url : baseAPI + url
}

// 格式化评价数量
const formatReviewCount = (count) => {
  if (!count || count === 0) return '暂无评价'
  if (count === 1) return '1条评价'
  return `${count}条评价`
}

// 获取评分显示
const getDisplayRating = (rating) => {
  if (!rating) return '4.5'
  return parseFloat(rating).toFixed(1)
}

// 高德地图相关
let map = null
let marker = null

// 天气相关状态
const weather = ref({})
const weatherForecast = ref([])
const weatherLoading = ref(false)

const initMap = () => {
  // 创建地图实例
  map = new AMap.Map('map-container', {
    zoom: 15,
    resizeEnable: true
  })

  // 等待地图加载完成后再设置中心点和标记
  map.on('complete', () => {
    // 如果有经纬度信息，直接使用
    if (scenic.value.longitude && scenic.value.latitude) {
      const lnglat = new AMap.LngLat(scenic.value.longitude, scenic.value.latitude)
      
      // 设置地图中心点
      map.setCenter(lnglat)
      map.setZoom(16)
      
      // 创建标记
      marker = new AMap.Marker({
        position: lnglat,
        title: scenic.value.name,
        animation: 'AMAP_ANIMATION_DROP'
      })
      map.add(marker)
      
      // 添加信息窗体
      const infoWindow = new AMap.InfoWindow({
        content: `<div class="info-window">
                    <h4>${scenic.value.name}</h4>
                    <p>${scenic.value.location}</p>
                    <p>开放时间: ${scenic.value.openingHours}</p>
                  </div>`,
        offset: new AMap.Pixel(0, -30)
      })
      
      // 点击标记时打开信息窗体
      marker.on('click', () => {
        infoWindow.open(map, marker.getPosition())
      })
      
      // 默认打开信息窗体
      infoWindow.open(map, marker.getPosition())
      
      // 确保标记在地图中心
      setTimeout(() => {
        map.setCenter(lnglat)
      }, 100)
    }
    // 如果没有经纬度，显示默认位置
    else {
      console.warn('景点没有经纬度信息，显示默认地图')
      // 设置默认位置（北京天安门）
      const defaultLnglat = new AMap.LngLat(116.397428, 39.90923)
      map.setCenter(defaultLnglat)
      map.setZoom(10)
    }

    // 添加控件
    map.plugin(['AMap.ToolBar', 'AMap.Scale'], () => {
      map.addControl(new AMap.ToolBar())
      map.addControl(new AMap.Scale())
    })
  })
}



const loadMapScript = () => {
  return new Promise((resolve, reject) => {
    if (window.AMap) {
      resolve()
      return
    }
    // 设置高德安全密钥（securityJsCode）
    window._AMapSecurityConfig = {
      securityJsCode: 'f4b1c3a6cf3b20224ce247d9e20d272d'
    }
    const script = document.createElement('script')
    script.type = 'text/javascript'
    script.async = true
    script.src = `https://webapi.amap.com/maps?v=2.0&key=b5bc36af3686770df7682f4b211ad39d`
    script.onerror = reject
    script.onload = resolve
    document.head.appendChild(script)
  })
}

// 格式化天气时间
const formatWeatherTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 格式化预报日期
const formatForecastDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  const today = new Date()
  const tomorrow = new Date()
  tomorrow.setDate(today.getDate() + 1)
  
  if (date.toDateString() === today.toDateString()) {
    return '今天'
  } else if (date.toDateString() === tomorrow.toDateString()) {
    return '明天'
  } else {
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return weekdays[date.getDay()]
  }
}

// 获取天气信息
const fetchWeatherInfo = async (location) => {
  if (!location) return
  
  weatherLoading.value = true
  try {
    // 调用和风天气API
    const key = '308d7421f676413aab8b4064aba532d7'
    
    // 先通过地名查询获取经纬度
    const locationResponse = await axios.get(`https://geoapi.qweather.com/v2/city/lookup`, {
      params: {
        location,
        key,
        number: 1
      }
    })
    
    if (locationResponse.data.code === '200' && locationResponse.data.location && locationResponse.data.location.length > 0) {
      const locationId = locationResponse.data.location[0].id
      const lon = locationResponse.data.location[0].lon
      const lat = locationResponse.data.location[0].lat
      
      // 获取实时天气
      const weatherResponse = await axios.get(`https://devapi.qweather.com/v7/weather/now`, {
        params: {
          key,
          location: `${lon},${lat}`
        }
      })
      
      if (weatherResponse.data.code === '200') {
        weather.value = weatherResponse.data
      }
      
      // 获取天气预报
      const forecastResponse = await axios.get(`https://devapi.qweather.com/v7/weather/3d`, {
        params: {
          key,
          location: `${lon},${lat}`
        }
      })
      
      if (forecastResponse.data.code === '200') {
        weatherForecast.value = forecastResponse.data.daily
      }
    }
  } catch (error) {
    console.error('获取天气信息失败:', error)
  } finally {
    weatherLoading.value = false
  }
}

const fetchDetail = async () => {
  const id = route.params.id
  await request.get(`/scenic/${id}`,null, {
    onSuccess: (res) => {
      scenic.value = res
      fetchTickets(id)
      // 获取评论统计
      fetchCommentStats(id)
      // 加载天气信息
      fetchWeatherInfo(res.location)
      
      // 确保DOM已经渲染完成后再初始化地图
      nextTick(() => {
        loadMapScript()
          .then(() => {
            // 延长等待时间，确保地图容器已完全渲染
            setTimeout(() => {
              initMap()
            }, 500)
          })
          .catch(err => {
            console.error('加载高德地图失败:', err)
          })
      })
      
      // 如果用户已登录，检查收藏状态
      if (isLoggedIn.value) {
        checkCollectionStatus(id)
      }
    }
  })
}

// 获取评论统计信息
const fetchCommentStats = async (scenicId) => {
  try {
    await request.get('/comment/page', {
      scenicId: scenicId,
      currentPage: 1,
      size: 1  // 只需要获取总数，不需要具体数据
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        // 更新景点的评论统计信息
        if (res) {
          scenic.value.reviewCount = res.total || 0

          // 如果有评论数据，计算平均评分
          if (res.records && res.records.length > 0) {
            // 获取所有评论来计算平均评分
            fetchAllCommentsForRating(scenicId)
          }
        }
      }
    })
  } catch (error) {
    console.error('获取评论统计失败:', error)
    // 如果获取失败，设置默认值
    scenic.value.reviewCount = 0
  }
}

// 获取所有评论来计算平均评分
const fetchAllCommentsForRating = async (scenicId) => {
  try {
    await request.get(`/comment/scenic/${scenicId}`, null, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        if (res && res.length > 0) {
          // 计算平均评分
          const totalRating = res.reduce((sum, comment) => sum + (comment.rating || 0), 0)
          const averageRating = totalRating / res.length
          scenic.value.rating = averageRating.toFixed(1)
        }
      }
    })
  } catch (error) {
    console.error('获取评论评分失败:', error)
  }
}

// 检查收藏状态
const checkCollectionStatus = async (scenicId) => {
  if (!isLoggedIn.value) return
  
  try {
    await request.get(`/scenic-collection/is-collected/${scenicId}`, null, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        isCollected.value = res
      }
    })
  } catch (error) {
    console.error('获取收藏状态失败:', error)
  }
}

// 收藏/取消收藏操作
const handleCollection = async () => {
  // 检查登录状态
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('收藏功能需要登录，是否前往登录页面？', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info'
    }).then(() => {
      router.push({
        path: '/login',
        query: { redirect: route.fullPath }
      })
    }).catch(() => {})
    return
  }
  
  const scenicId = scenic.value.id
  if (!scenicId) return
  
  collectionLoading.value = true
  try {
    if (isCollected.value) {
      // 取消收藏 - 修复delete请求逻辑
      try {
        await request.delete(`/scenic-collection/${scenicId}`, {
          successMsg: '取消收藏成功'
        })
        // 手动更新状态
        isCollected.value = false
      } catch (error) {
        console.error('取消收藏失败:', error)
        ElMessage.error('取消收藏失败，请稍后重试')
      }
    } else {
      // 添加收藏
      try {
        await request.post(`/scenic-collection/${scenicId}`, null, {
          successMsg: '收藏成功'
        })
        // 手动更新状态
        isCollected.value = true
      } catch (error) {
        console.error('添加收藏失败:', error)
        ElMessage.error('添加收藏失败，请稍后重试')
      }
    }
  } catch (error) {
    console.error('操作收藏失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    collectionLoading.value = false
  }
}

const fetchTickets = async (scenicId) => {
  ticketLoading.value = true
  try {
    await request.get(`/ticket/scenic/${scenicId}`, {
      currentPage: 1,
      size: 10
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        tickets.value = res.records || []
      }
    })
  } catch (error) {
    console.error('获取景点门票失败:', error)
  } finally {
    ticketLoading.value = false
  }
}

const goToBooking = (ticketId) => {
  router.push(`/ticket/booking/${ticketId}`)
}

// 复制坐标到剪贴板
const copyCoordinates = () => {
  if (!scenic.value.longitude || !scenic.value.latitude) return
  
  const text = `${scenic.value.longitude}, ${scenic.value.latitude}`
  navigator.clipboard.writeText(text)
    .then(() => {
      ElMessage.success('坐标已复制到剪贴板')
    })
    .catch(err => {
      console.error('复制失败:', err)
      ElMessage.error('复制失败，请手动复制')
    })
}

// 分享功能
const handleShare = () => {
  // 复制链接到剪贴板
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制，快去分享吧！')
  }).catch(() => {
    ElMessage.error('复制失败，请手动复制地址栏链接')
  })
}

onUnmounted(() => {
  if (map) {
    map.destroy()
    map = null
  }
})

onMounted(fetchDetail)
</script>

<style lang="scss" scoped>
.scenic-detail-container {
  min-height: 100vh;
  background: #f8fafc;
  font-family: "思源黑体", "Source Han Sans", "Noto Sans CJK SC", sans-serif;
}

// Hero区域样式
.detail-hero-section {
  position: relative;
  height: 50vh;
  min-height: 400px;
  overflow: hidden;
}

.hero-image-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.hero-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: flex-end;
}

.overlay-gradient {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.1) 0%,
    rgba(0, 0, 0, 0.3) 50%,
    rgba(0, 0, 0, 0.8) 100%
  );
}

.hero-content {
  position: relative;
  z-index: 10;
  color: white;
  padding: 30px 40px;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.breadcrumb {
  margin-bottom: 20px;

  :deep(.el-breadcrumb__item) {
    .el-breadcrumb__inner {
      color: rgba(255, 255, 255, 0.8);
      cursor: pointer;

      &:hover {
        color: white;
      }
    }

    &:last-child .el-breadcrumb__inner {
      color: white;
    }
  }

  :deep(.el-breadcrumb__separator) {
    color: rgba(255, 255, 255, 0.6);
  }
}

.scenic-title {
  font-size: 42px;
  font-weight: 700;
  margin: 0 0 16px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
  line-height: 1.2;
}

.scenic-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 24px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;

  .el-icon {
    font-size: 18px;
    opacity: 0.9;
  }

  &.rating {
    .el-icon {
      color: #ffd700;
    }

    .rating-text {
      opacity: 0.8;
      margin-left: 4px;
    }
  }
}

.action-buttons {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.collection-btn,
.share-btn {
  border-radius: 25px;
  font-weight: 600;
  padding: 12px 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
  }
}

.collection-btn {
  background: linear-gradient(45deg, #667eea, #764ba2);
  border: none;

  &.el-button--danger {
    background: linear-gradient(45deg, #ff6b6b, #ee5a24);
  }
}

.share-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
  color: white;
  backdrop-filter: blur(10px);

  &:hover {
    background: rgba(255, 255, 255, 0.3);
    border-color: rgba(255, 255, 255, 0.5);
  }
}

// 详细内容区域
.detail-content {
  background: white;
  margin: 0;
  position: relative;
  z-index: 1;
}

.section-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 30px;
  align-items: start;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.sidebar-content {
  position: sticky;
  top: 20px;
}

// 信息卡片通用样式
.info-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid #e2e8f0;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #2d3748;
  margin: 0 0 16px;
  display: flex;
  align-items: center;
  gap: 8px;

  .el-icon {
    color: #667eea;
  }
}

// 描述卡片
.description-content {
  font-size: 16px;
  line-height: 1.8;
  color: #4a5568;
}

// 天气卡片
.weather-content {
  .current-weather {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .weather-main {
    text-align: center;

    .weather-temp {
      font-size: 36px;
      font-weight: 700;
      color: #667eea;
      margin-bottom: 4px;
    }

    .weather-text {
      font-size: 16px;
      color: #64748b;
    }
  }

  .weather-details {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .detail-item {
    display: flex;
    justify-content: space-between;
    padding: 8px 12px;
    background: #f8fafc;
    border-radius: 8px;

    .label {
      font-size: 14px;
      color: #64748b;
    }

    .value {
      font-size: 14px;
      font-weight: 600;
      color: #2d3748;
    }
  }

  .weather-update {
    font-size: 12px;
    color: #94a3b8;
    text-align: center;
    margin-top: 16px;
  }
}

.weather-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #64748b;

  .loading-icon {
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

// 基本信息卡片
.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #e2e8f0;

  &:last-child {
    border-bottom: none;
  }
}

.info-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #64748b;

  .el-icon {
    color: #667eea;
  }
}

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;

  &.price-value {
    .free-tag {
      background: linear-gradient(45deg, #10b981, #059669);
      color: white;
      padding: 4px 8px;
      border-radius: 12px;
      font-size: 12px;
    }

    .price {
      color: #667eea;
      font-size: 18px;
    }
  }

  &.coordinates {
    cursor: pointer;
    color: #667eea;
    display: flex;
    align-items: center;
    gap: 4px;

    &:hover {
      color: #5a67d8;
    }

    .copy-icon {
      font-size: 12px;
      opacity: 0.7;
    }
  }
}

// 区域标题
.section-title {
  font-size: 24px;
  font-weight: 700;
  color: #2d3748;
  margin: 0 0 20px;
  display: flex;
  align-items: center;
  gap: 10px;

  .el-icon {
    color: #667eea;
    font-size: 20px;
  }
}

// 天气预报样式
.forecast-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
}

.forecast-title {
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
  margin: 0 0 16px;
}

.forecast-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

.forecast-item {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;

  &:hover {
    background: #f1f5f9;
    transform: translateY(-2px);
  }
}

.forecast-date {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 8px;
}

.forecast-weather {
  margin-bottom: 8px;

  .weather-desc {
    font-size: 12px;
    color: #64748b;
    line-height: 1.3;
    font-weight: 500;
  }
}

.forecast-temp {
  display: flex;
  justify-content: center;
  gap: 4px;
  font-size: 12px;

  .temp-high {
    font-weight: 600;
    color: #e53e3e;
  }

  .temp-low {
    font-weight: 600;
    color: #3182ce;
  }
}



// 地图区域
.map-section {
  background: #f8fafc;
  padding: 40px 0;
}

.map-wrapper {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.map-container {
  width: 100%;
  height: 400px;
}



// 门票区域
.ticket-section {
  background: white;
  padding: 40px 0;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, #e2e8f0, transparent);
  }
}

.ticket-content {
  min-height: 200px;
}

.ticket-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}

.ticket-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  }
}

.ticket-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.ticket-name {
  font-size: 18px;
  font-weight: 700;
  color: #2d3748;
  margin: 0;
  flex: 1;
}

.ticket-type-badge {
  background: linear-gradient(45deg, #667eea, #764ba2);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.ticket-price {
  margin-bottom: 16px;
  position: relative;
}

.price-original {
  font-size: 14px;
  color: #94a3b8;
  text-decoration: line-through;
  margin-bottom: 4px;
}

.price-discount {
  font-size: 24px;
  font-weight: 700;
  color: #e53e3e;
}

.price-normal {
  font-size: 24px;
  font-weight: 700;
  color: #667eea;
}

.discount-tag {
  position: absolute;
  top: -8px;
  right: 0;
  background: #e53e3e;
  color: white;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
}

.ticket-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #64748b;

  .el-icon {
    color: #667eea;
  }
}

.ticket-description {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 20px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ticket-actions {
  .booking-btn {
    width: 100%;
    border-radius: 25px;
    background: linear-gradient(45deg, #667eea, #764ba2);
    border: none;
    font-weight: 600;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
    }

    &:disabled {
      background: #e2e8f0;
      color: #94a3b8;
    }
  }
}

// 空状态
.empty-state {
  text-align: center;
  padding: 60px 20px;

  .empty-icon {
    font-size: 64px;
    margin-bottom: 20px;
  }

  .empty-title {
    font-size: 20px;
    font-weight: 600;
    color: #2d3748;
    margin: 0 0 8px;
  }

  .empty-desc {
    font-size: 16px;
    color: #64748b;
    margin: 0;
  }
}

// 评论区域
.comment-section {
  background: #f8fafc;
  padding: 40px 0;
}

.comment-wrapper {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

// 响应式设计
@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 1fr;
    gap: 30px;
  }

  .sidebar-content {
    position: static;
  }
}

@media (max-width: 768px) {
  .scenic-title {
    font-size: 28px;
  }

  .scenic-meta {
    gap: 16px;
  }

  .action-buttons {
    flex-direction: column;
  }

  .collection-btn,
  .share-btn {
    width: 100%;
    justify-content: center;
  }

  .section-container {
    padding: 30px 15px;
  }

  .hero-content {
    padding: 20px;
  }

  .forecast-list {
    grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  }

  .ticket-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .scenic-title {
    font-size: 24px;
  }

  .detail-hero-section {
    height: 45vh;
    min-height: 350px;
  }

  .weather-details {
    grid-template-columns: 1fr;
  }

  .forecast-list {
    grid-template-columns: 1fr 1fr;
  }
}



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

// 高德地图信息窗体样式
:deep(.amap-info-window) {
  .info-window {
    padding: 12px;

    h4 {
      margin: 0 0 8px;
      font-size: 16px;
      color: #2d3748;
    }

    p {
      margin: 4px 0;
      font-size: 14px;
      color: #64748b;
    }
  }
}

// 加载状态
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
}

// 滚动条美化
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;

  &:hover {
    background: #a8a8a8;
  }
}

// 选择文本样式
::selection {
  background: rgba(102, 126, 234, 0.2);
  color: #2d3748;
}

::-moz-selection {
  background: rgba(102, 126, 234, 0.2);
  color: #2d3748;
}
</style>