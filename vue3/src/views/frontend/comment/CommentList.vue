<template>
  <div class="comment-list">
    <div class="comment-form-container" v-if="isLoggedIn">
      <h3 class="section-title">发表评论</h3>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="60px" class="comment-form">
        <el-form-item label="评分" prop="rating">
          <el-rate v-model="form.rating" :max="5" allow-half />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="说点什么吧..." />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitComment" :loading="submitLoading">发布</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <div class="comments-container">
      <h3 class="section-title">全部评论 ({{ total }})</h3>
      <div v-if="comments.length === 0" class="empty">暂无评论，快来发表第一条评论吧！</div>
      <el-list v-else>
        <el-list-item v-for="item in comments" :key="item.id" class="comment-item">
          <div class="comment-header">
            <el-avatar :src="baseAPI + (item.userAvatar || '')" size="small" />
            <span class="user">{{ item.userNickname || ('用户' + item.userId) }}</span>
            <el-rate v-model="item.rating" :max="5" disabled size="small" />
            <span class="date">{{ formatDate(item.createTime) }}</span>
          </div>
          <div class="content">{{ item.content }}</div>
          <div class="actions">
            <el-button 
              size="small" 
              type="text" 
              @click="likeComment(item)"
              :class="{ 'liked-btn': item.liked }"
            >
              <span class="like-icon">👍</span> {{ item.likes || 0 }}
            </el-button>
            <el-button size="small" type="text" v-if="canDelete(item)" @click="deleteComment(item)" class="delete-btn">删除</el-button>
          </div>
        </el-list-item>
      </el-list>
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'
import { useUserStore } from '@/store/user'
import { useRoute } from 'vue-router'


const userStore = useUserStore()
const route = useRoute()
const isLoggedIn = computed(() => !!userStore.token)
const isAdmin = computed(() => userStore.role === 'ADMIN')
const userId = computed(() => userStore.userInfo?.id)

const scenicId = computed(() => Number(route.params.id) || route.query.scenicId)
const comments = ref([])
const currentPage = ref(1)
const pageSize = ref(8)
const total = ref(0)

const form = reactive({
  content: '',
  rating: 5
})
const formRef = ref(null)
const submitLoading = ref(false)

const rules = {
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' },
    { min: 2, max: 200, message: '2-200字', trigger: 'blur' }
  ],
  rating: [
    { required: true, message: '请评分', trigger: 'change' }
  ]
}

const baseAPI = process.env.VUE_APP_BASE_API || '/api'

const fetchComments = async () => {
  await request.get('/comment/page', {
      scenicId: scenicId.value,
      currentPage: currentPage.value,
      size: pageSize.value
    },{
    onSuccess: (res) => {
      comments.value = res.records||[]
      total.value = res.total||0
    }
    }
  )
}

onMounted(fetchComments)

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchComments()
}

const submitComment = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        await request.post('/comment/add', {
          ...form,
          scenicId: scenicId.value
        }, {
          successMsg: '评论成功',
          onSuccess: () => {
            form.content = ''
            form.rating = 5
            fetchComments()
          }
        })
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const likeComment = async (item) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  
  try {
    await request.put(`/comment/like/${item.id}`, null, {
      showDefaultMsg: false,
      onSuccess: (isLiked) => {
        if (isLiked) {
          item.liked = true
          item.likes = (item.likes || 0) + 1
          ElMessage.success('点赞成功')
        } else {
          item.liked = false
          item.likes = Math.max(0, (item.likes || 0) - 1)
          ElMessage.success('取消点赞成功')
        }
      }
    })
  } catch (error) {
    console.error('点赞操作失败:', error)
  }
}

const canDelete = (item) => {
  return isAdmin.value || item.userId === userId.value
}

const deleteComment = (item) => {
  ElMessageBox.confirm('确定要删除该评论吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/comment/delete/${item.id}`, {
      successMsg: '删除成功',
      onSuccess: () => fetchComments()
    })
  }).catch(() => {})
}
</script>

<style lang="scss" scoped>
.comment-list {
  .section-title {
    font-size: 20px;
    color: #2c3e50;
    margin-bottom: 20px;
    font-weight: 600;
    border-left: 4px solid #3498db;
    padding-left: 15px;
  }
  
  .comment-form-container {
    background-color: #fff;
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 25px;
    border: 1px solid #eaeaea;
    
    .comment-form {
      background-color: #f9f9f9;
      padding: 20px;
      border-radius: 8px;
    }
  }
  
  .comments-container {
    background-color: #fff;
    border-radius: 12px;
    padding: 20px;
    border: 1px solid #eaeaea;
    
    .empty {
      color: #999;
      text-align: center;
      padding: 30px 0;
      font-size: 16px;
      background-color: #f9f9f9;
      border-radius: 8px;
      border: 1px dashed #ddd;
    }
    
    .comment-item {
      border-bottom: 1px solid #eee;
      padding: 15px 0;
      transition: background-color 0.3s;
      border-radius: 8px;
      
      &:hover {
        background-color: #f5f7fa;
      }
      
      .comment-header {
        display: flex;
        align-items: center;
        gap: 10px;
        
        .user { 
          color: #3498db; 
          font-size: 14px;
          font-weight: 500;
        }
        
        .date { 
          color: #999; 
          font-size: 13px; 
          margin-left: auto; 
        }
      }
      
      .content {
        margin: 12px 0 10px 0;
        font-size: 15px;
        text-align: left;
        line-height: 1.5;
        color: #333;
        padding: 0 5px;
      }
      
      .actions {
        display: flex;
        gap: 15px;
        
        .liked-btn {
          color: #e74c3c;
          font-weight: bold;
        }
        
        .like-icon {
          display: inline-block;
          margin-right: 3px;
          transition: transform 0.2s;
        }
        
        .liked-btn .like-icon {
          transform: scale(1.2);
        }
        
        .delete-btn {
          color: #7f8c8d;
          
          &:hover {
            color: #e74c3c;
          }
        }
      }
    }
    
    .pagination-container {
      display: flex;
      justify-content: center;
      margin: 25px 0 0 0;
    }
  }
}
</style> 