<template>
  <div class="collection-list">
    <h2 class="page-title">我的收藏</h2>
    
    <div class="collections" v-loading="loading">
      <el-empty v-if="tableData.length === 0" description="暂无收藏内容" />
      
      <el-row :gutter="20">
        <el-col :span="8" v-for="item in tableData" :key="item.id">
          <el-card class="guide-card" @click="goDetail(item.guideId)">
            <img :src="baseAPI + item.guideCoverImage" class="cover" v-if="item.guideCoverImage" />
            <div class="title">{{ item.guideTitle }}</div>
            <div class="meta">
              <span>浏览量: {{ item.guideViews }}</span>
              <span class="time">收藏时间: {{ formatDate(item.createTime) }}</span>
            </div>
            <div class="actions">
              <el-button type="danger" size="small" @click.stop="handleCancelCollection(item)">
                取消收藏
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    
    <div class="pagination-container">
      <el-pagination
        v-if="tableData.length > 0"
        background
        layout="total, prev, pager, next, jumper"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const router = useRouter()
const userStore = useUserStore()
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(0)

const fetchCollections = async () => {
  loading.value = true
  try {
    await request.get('/collection/page', {
      currentPage: currentPage.value,
      size: pageSize.value,
      userId: userStore.userInfo.id
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        tableData.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取收藏列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchCollections()
}

const goDetail = (guideId) => {
  router.push(`/guide/detail/${guideId}`)
}

const handleCancelCollection = (item) => {
  ElMessageBox.confirm('确定要取消收藏这个攻略吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.delete('/collection/cancel?guideId=' + item.guideId, {
      successMsg: '取消收藏成功',
      onSuccess: () => {
        // 刷新列表
        fetchCollections()
      }
    })
  }).catch(() => {})
}

onMounted(fetchCollections)
</script>

<style lang="scss" scoped>
.collection-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  
  .page-title {
    margin-bottom: 30px;
    font-size: 24px;
    font-weight: 600;
    color: #333;
  }
  
  .guide-card {
    margin-bottom: 20px;
    cursor: pointer;
    transition: box-shadow 0.3s;
    
    &:hover {
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    }
    
    .cover {
      width: 100%;
      height: 180px;
      object-fit: cover;
      border-radius: 4px;
      margin-bottom: 10px;
    }
    
    .title {
      font-size: 18px;
      font-weight: bold;
      margin-bottom: 8px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .meta {
      display: flex;
      justify-content: space-between;
      font-size: 14px;
      color: #999;
      margin-bottom: 10px;
    }
    
    .actions {
      display: flex;
      justify-content: flex-end;
      margin-top: 10px;
    }
  }
  
  .pagination-container {
    margin-top: 30px;
    text-align: center;
  }
}
</style> 