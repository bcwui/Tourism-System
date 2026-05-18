<template>
  <div class="comment-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">评论管理</h1>
      <p class="page-subtitle">Comment Management</p>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="景点名称">
          <el-input v-model="searchForm.scenicName" placeholder="请输入景点名称" clearable>
            <template #prefix>
              <i class="el-icon-search"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="用户名/昵称">
          <el-input v-model="searchForm.userName" placeholder="请输入用户名或昵称" clearable>
            <template #prefix>
              <i class="el-icon-user"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="searchForm.content" placeholder="请输入内容关键词" clearable>
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
      <el-table :data="tableData" v-loading="loading" border stripe class="comment-table">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column label="用户" width="180">
          <template #default="scope">
            <div class="user-info">
              <el-avatar :src="baseAPI + (scope.row.userAvatar || '')" size="small" />
              <span class="user-name">{{ scope.row.userNickname || ('用户' + scope.row.userId) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="景点名称" width="180">
          <template #default="scope">
            <span class="scenic-name">{{ scope.row.scenicName || '未知景点' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column prop="rating" label="评分" width="80" align="center">
          <template #default="scope">
            <el-rate
              v-model="scope.row.rating"
              disabled
              text-color="#ff9900"
              score-template="{value}"
              :max="5"
            />
          </template>
        </el-table-column>
        <el-table-column label="点赞" width="80" align="center">
          <template #default="scope">
            <div class="like-info">
              <i class="el-icon-thumb"></i> {{ scope.row.likes || 0 }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="160" align="center">
          <template #default="scope">
            <span class="date-text">{{ formatDate(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="scope">
            <el-button size="small" type="danger" plain @click="deleteComment(scope.row)" class="delete-btn">
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
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'

const searchForm = reactive({
  scenicName: '',
  userName: '',
  content: ''
})

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

const fetchComments = async () => {
  loading.value = true
  try {
    // 构建查询参数
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value
    }
    
    if (searchForm.scenicName) params.scenicName = searchForm.scenicName
    if (searchForm.userName) params.userName = searchForm.userName
    if (searchForm.content) params.content = searchForm.content
    
    await request.get('/comment/page', params, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        tableData.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取评论列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchComments()
}

const resetSearch = () => {
  searchForm.scenicName = ''
  searchForm.userName = ''
  searchForm.content = ''
  currentPage.value = 1
  fetchComments()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchComments()
}

const deleteComment = (row) => {
  ElMessageBox.confirm('确定要删除该评论吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/comment/delete/${row.id}`, {
      successMsg: '删除成功',
      onSuccess: () => fetchComments()
    })
  }).catch(() => {})
}

onMounted(fetchComments)
</script>

<style lang="scss" scoped>
.comment-management {
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
    
    .comment-table {
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
        
        .user-name {
          margin-left: 8px;
          font-weight: 500;
        }
      }
      
      .scenic-name {
        color: #2980b9;
        font-weight: 500;
      }
      
      .like-info {
        display: flex;
        align-items: center;
        justify-content: center;
        color: #e67e22;
        
        i {
          margin-right: 5px;
          font-size: 16px;
        }
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