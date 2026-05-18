<template>
  <div class="collection-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">收藏管理</h1>
      <p class="page-subtitle">Collection Management</p>
    </div>
    
    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable>
            <template #prefix>
              <i class="el-icon-user"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="攻略标题">
          <el-input v-model="searchForm.guideTitle" placeholder="请输入攻略标题" clearable>
            <template #prefix>
              <i class="el-icon-document"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" class="search-btn">
            <i class="el-icon-search"></i> 搜索
          </el-button>
          <el-button @click="resetSearch" class="reset-btn">
            <i class="el-icon-refresh"></i> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 表格区域 -->
    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" style="width: 100%" v-loading="loading" border class="collection-table">
        <el-table-column label="ID" prop="id" width="80" align="center" />
        <el-table-column label="用户信息" min-width="180">
          <template #default="scope">
            <div class="user-info">
              <el-avatar :src="baseAPI + (scope.row.userAvatar || '')" size="small" />
              <div class="user-details">
                <div class="username">{{ scope.row.username || '未知用户' }}</div>
                <div class="nickname" v-if="scope.row.userNickname">{{ scope.row.userNickname }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="攻略标题" prop="guideTitle">
          <template #default="scope">
            <span class="guide-title">{{ scope.row.guideTitle }}</span>
          </template>
        </el-table-column>
        <el-table-column label="攻略浏览量" prop="guideViews" width="120" align="center">
          <template #default="scope">
            <span class="view-count">
              <i class="el-icon-view"></i> {{ scope.row.guideViews || 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="收藏时间" width="180" align="center">
          <template #default="scope">
            <span class="date-text">{{ formatDate(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="scope">
            <el-button type="danger" size="small" plain @click="handleDelete(scope.row)" class="delete-btn">
              <i class="el-icon-delete"></i> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'
import { ElMessageBox, ElMessage } from 'element-plus'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchForm = reactive({
  username: '',
  guideTitle: ''
})

const fetchCollections = async () => {
  loading.value = true
  try {
    await request.get('/collection/admin/page', {
      username: searchForm.username,
      guideTitle: searchForm.guideTitle,
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
    console.error('获取收藏列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchCollections()
}

const resetSearch = () => {
  searchForm.username = ''
  searchForm.guideTitle = ''
  currentPage.value = 1
  fetchCollections()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchCollections()
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除这条收藏记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.delete(`/collection/admin/${row.id}`,  {
      successMsg: '删除成功',
      onSuccess: () => {
        fetchCollections()
      }
    })
  }).catch(() => {})
}

onMounted(fetchCollections)
</script>

<style lang="scss" scoped>
.collection-management {
  padding: 20px;
  background-color: #f9fafc;
  min-height: calc(100vh - 120px);

  .page-header {
    margin-bottom: 24px;
    text-align: left;
    
    .page-title {
      font-size: 24px;
      color: #34495e;
      margin: 0 0 8px 0;
      font-weight: 500;
    }
    
    .page-subtitle {
      font-size: 14px;
      color: #7f8c8d;
      margin: 0;
      font-style: italic;
    }
  }

  .search-card {
    margin-bottom: 20px;
    border-radius: 8px;
    background-color: #fff;
    box-shadow: none;
    
    .search-form {
      padding: 10px 0;
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      
      .el-form-item {
        margin-bottom: 0;
        margin-right: 16px;
      }
      
      .search-btn {
        background-color: #3498db;
        border-color: #3498db;
        
        &:hover, &:focus {
          background-color: #2980b9;
          border-color: #2980b9;
        }
      }
      
      .reset-btn {
        color: #7f8c8d;
        border-color: #bdc3c7;
        
        &:hover, &:focus {
          color: #34495e;
          border-color: #95a5a6;
          background-color: #f5f5f5;
        }
      }
    }
  }
  
  .table-card {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: none;
    
    .collection-table {
      border-radius: 4px;
      overflow: hidden;
      
      :deep(thead) {
        background-color: #ecf0f1;
        
        th {
          background-color: #ecf0f1;
          color: #34495e;
          font-weight: 500;
          padding: 12px 0;
        }
      }
      
      :deep(tbody tr) {
        transition: all 0.3s;
        
        &:hover {
          background-color: #f8f9fa;
        }
      }
      
      .user-info {
        display: flex;
        align-items: center;
        
        .el-avatar {
          margin-right: 10px;
          flex-shrink: 0;
        }
        
        .user-details {
          display: flex;
          flex-direction: column;
          
          .username {
            font-weight: 500;
            font-size: 14px;
            color: #2980b9;
          }
          
          .nickname {
            font-size: 12px;
            color: #909399;
          }
        }
      }
      
      .guide-title {
        color: #2c3e50;
        font-weight: 500;
      }
      
      .view-count {
        color: #7f8c8d;
        font-size: 14px;
      }
      
      .date-text {
        color: #7f8c8d;
        font-size: 12px;
      }
      
      .delete-btn {
        padding: 5px 12px;
      }
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    padding: 0 20px;
  }
}
</style>