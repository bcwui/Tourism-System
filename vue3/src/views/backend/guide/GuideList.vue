<template>
  <div class="guide-back-list">
    <div class="page-header">
      <h1 class="page-title">攻略管理</h1>
      <p class="page-subtitle">Travel Guide Management</p>
    </div>

    <!-- 搜索区域 -->
    <div class="filter-container">
      <el-card shadow="never" class="search-card">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="标题">
            <el-input v-model="searchForm.title" placeholder="攻略标题" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch" :icon="Search" class="search-btn">搜索</el-button>
            <el-button @click="resetSearch" :icon="Refresh" class="reset-btn">重置</el-button>
            <el-button type="success" @click="handleCreate" :icon="Plus" class="add-btn">新建攻略</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 表格区域 -->
    <el-card shadow="never" class="table-card">
      <el-table 
        :data="tableData" 
        v-loading="loading" 
        border 
        stripe 
        highlight-current-row
        @row-click="handleRowClick"
        row-key="id"
        class="guide-table"
      >
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="作者" width="180">
          <template #default="scope">
            <div class="author-info">
              <el-avatar :src="baseAPI + (scope.row.userAvatar || '')" size="small" />
              <span class="author-name">{{ scope.row.userNickname || ('用户' + scope.row.userId) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column label="封面" width="120">
          <template #default="scope">
            <el-image 
              v-if="scope.row.coverImage" 
              :src="baseAPI + scope.row.coverImage" 
              fit="cover"
              style="width:80px;height:50px;border-radius:4px;"
              :preview-src-list="[baseAPI + scope.row.coverImage]"
              preview-teleported
            />
            <div v-else class="no-image">无封面</div>
          </template>
        </el-table-column>
        <el-table-column prop="views" label="浏览量" width="90" sortable>
          <template #default="scope">
            <el-tag type="info" effect="plain">{{ scope.row.views || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" sortable>
          <template #default="scope">
            <span class="date-text">{{ formatDate(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="内容预览" min-width="200">
          <template #default="scope">
            <div class="content-preview" v-html="getContentPreview(scope.row.content)"></div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button 
              size="small" 
              type="primary" 
              :icon="Edit" 
              plain
              @click.stop="handleEdit(scope.row)"
              class="action-btn"
            >
              编辑
            </el-button>
            <el-button 
              size="small" 
              type="danger" 
              :icon="Delete" 
              plain
              @click.stop="deleteGuide(scope.row)"
              class="action-btn"
            >
              删除
            </el-button>
            <el-button 
              size="small" 
              type="info" 
              :icon="View" 
              plain
              @click.stop="previewGuide(scope.row)"
              class="action-btn"
            >
              预览
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 预览对话框 -->
    <el-dialog 
      v-model="previewDialogVisible" 
      title="攻略预览" 
      width="70%" 
      destroy-on-close
      fullscreen
      class="guide-dialog"
    >
      <div class="guide-preview" v-if="previewGuideData">
        <h1 class="preview-title">{{ previewGuideData.title }}</h1>
        <div class="preview-info">
          <span>作者: {{ previewGuideData.userNickname || ('用户' + previewGuideData.userId) }}</span>
          <span>发布时间: {{ formatDate(previewGuideData.createTime) }}</span>
          <span>浏览量: {{ previewGuideData.views || 0 }}</span>
        </div>
        <el-divider />
        <div class="preview-content" v-html="previewGuideData.content"></div>
      </div>
    </el-dialog>

    <!-- 编辑/创建对话框 -->
    <el-dialog
      v-model="formDialogVisible"
      :title="isEdit ? '编辑攻略' : '新建攻略'"
      width="75%"
      destroy-on-close
      :close-on-click-modal="false"
      :before-close="handleDialogClose"
      class="guide-dialog"
    >
      <el-form 
        ref="guideFormRef" 
        :model="guideForm" 
        :rules="guideFormRules" 
        label-width="80px"
        label-position="top"
        :disabled="submitLoading"
      >
        <!-- 标题区域 -->
        <el-form-item label="攻略标题" prop="title" required>
          <el-input 
            v-model="guideForm.title" 
            placeholder="请输入攻略标题" 
            maxlength="100" 
            show-word-limit
          />
        </el-form-item>
        
        <!-- 两列布局 -->
        <el-row :gutter="20">
          <!-- 左侧 - 封面图片 -->
          <el-col :span="8">
            <el-form-item label="封面图片" prop="coverImage">
              <el-upload
                class="cover-upload"
                action="#"
                :auto-upload="true"
                :show-file-list="false"
                :http-request="customUploadCover"
                :before-upload="beforeCoverUpload"
              >
                <div v-if="coverImageUrl" class="cover-preview">
                  <img :src="coverImageUrl" class="preview-image" />
                  <div class="hover-mask">
                    <el-icon><Camera /></el-icon>
                    <span>更换图片</span>
                  </div>
                </div>
                <div v-else class="upload-placeholder">
                  <el-icon><Picture /></el-icon>
                  <span>点击上传封面</span>
                  <div class="upload-tips">支持JPG、PNG (800×450px)</div>
                </div>
              </el-upload>
            </el-form-item>
          </el-col>
          
          <!-- 右侧 - 文本编辑器 -->
          <el-col :span="16">
            <el-form-item label="攻略内容" prop="content" required>
              <wang-editor
                v-model="guideForm.content"
                :height="'450px'"
                :placeholder="'请输入攻略内容...'"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleDialogClose">取消</el-button>
          <el-button 
            type="primary" 
            @click="submitGuideForm" 
            :loading="submitLoading"
          >
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Search, Refresh, Edit, Delete, View, Plus, Close, Check, CircleCheckFilled, EditPen, Camera, InfoFilled, Warning, Upload, Picture } from '@element-plus/icons-vue'
import WangEditor from '@/components/WangEditor.vue'
import { useUserStore } from '@/store/user'

// 用户状态
const userStore = useUserStore()

// 常量定义
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchForm = reactive({
  title: ''
})

// 预览相关
const previewDialogVisible = ref(false)
const previewGuideData = ref(null)

// 表单相关
const formDialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const guideFormRef = ref(null)
const guideForm = reactive({
  id: null,
  title: '',
  content: '',
  coverImage: ''
})

// 表单校验规则
const guideFormRules = {
  title: [
    { required: true, message: '请输入攻略标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度在2到100个字符之间', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入攻略内容', trigger: 'blur' }
  ]
}

// 封面图片URL
const coverImageUrl = computed(() => {
  if (!guideForm.coverImage) return ''
  return baseAPI + guideForm.coverImage
})

// 获取攻略列表
const fetchGuides = async () => {
  loading.value = true
  try {
    await request.get('/guide/page', {
      title: searchForm.title || undefined,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      onSuccess: (res) => {
        tableData.value = res.records||[]
        total.value = res.total||0
      },
      onError: (error) => {
        ElMessage.error('获取攻略列表失败')
        console.error('获取攻略列表失败:', error)
      }
    })
  } catch (error) {
    console.error('获取攻略列表错误:', error)
  } finally {
    loading.value = false
  }
}

// 生命周期钩子
onMounted(fetchGuides)

// 搜索相关
const handleSearch = () => {
  currentPage.value = 1
  fetchGuides()
}

const resetSearch = () => {
  searchForm.title = ''
  currentPage.value = 1
  fetchGuides()
}

// 分页相关
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchGuides()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchGuides()
}

// 删除攻略
const deleteGuide = (row) => {
  ElMessageBox.confirm('确定要删除该攻略吗？此操作不可逆！', '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
    closeOnClickModal: false
  }).then(async () => {
    try {
      await request.delete(`/guide/delete/${row.id}`, {
        successMsg: '删除成功',
        onSuccess: () => {
          fetchGuides()
        }
      })
    } catch (error) {
      console.error('删除攻略失败:', error)
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 预览攻略
const previewGuide = (row) => {
  previewGuideData.value = row
  previewDialogVisible.value = true
}

// 行点击事件
const handleRowClick = (row) => {
  // 点击行查看详情
  previewGuide(row)
}

// 内容预览处理
const getContentPreview = (content) => {
  if (!content) return '暂无内容'
  
  // 移除HTML标签，仅保留纯文本
  const textContent = content.replace(/<[^>]+>/g, '')
  
  // 返回前200个字符
  return textContent.length > 200 
    ? textContent.substring(0, 200) + '...' 
    : textContent
}

// 创建攻略
const handleCreate = () => {
  isEdit.value = false
  resetGuideForm()
  formDialogVisible.value = true
}

// 编辑攻略
const handleEdit = (row) => {
  isEdit.value = true
  resetGuideForm()
  Object.assign(guideForm, {
    id: row.id,
    title: row.title,
    content: row.content,
    coverImage: row.coverImage
  })
  formDialogVisible.value = true
}

// 重置表单
const resetGuideForm = () => {
  guideForm.id = null
  guideForm.title = ''
  guideForm.content = ''
  guideForm.coverImage = ''
  
  // 如果表单引用存在，重置校验状态
  if (guideFormRef.value) {
    guideFormRef.value.resetFields()
  }
}

// 封面上传前校验
const beforeCoverUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG && !isPNG) {
    ElMessage.error('封面图片只能是JPG或PNG格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('封面图片大小不能超过2MB!')
    return false
  }
  return true
}

// 自定义封面上传
const customUploadCover = async (options) => {
  try {
    const { file } = options
    const formData = new FormData()
    formData.append('file', file)

    await request.post('/file/upload/img', formData, {
      headers: {
        token: localStorage.getItem('token') || ''
      },
      transformRequest: [(data) => data],
      successMsg: '图片上传成功',
      onSuccess: (data) => {
        guideForm.coverImage = data
        options.onSuccess()
      },
      onError: (error) => {
        console.error('封面上传错误:', error)
        options.onError(new Error('封面上传失败'))
      }
    })
  } catch (error) {
    options.onError(error)
    console.error('封面上传过程错误:', error)
  }
}

// 提交表单
const submitGuideForm = () => {
  if (!guideFormRef.value) return

  guideFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请完善表单信息')
      return
    }

    submitLoading.value = true
    try {
      const formData = {
        id: guideForm.id,
        title: guideForm.title,
        content: guideForm.content,
        coverImage: guideForm.coverImage
      }

      if (isEdit.value) {
        // 编辑模式
        await request.put(`/guide/update`, formData, {
          successMsg: '攻略更新成功',
          onSuccess: () => {
            formDialogVisible.value = false
            fetchGuides()
          }
        })
      } else {
        // 创建模式
        formData.userId = userStore.userInfo.id
        await request.post('/guide/add', formData, {
          successMsg: '攻略创建成功',
          onSuccess: () => {
            formDialogVisible.value = false
            fetchGuides()
          }
        })
      }
    } catch (error) {
      console.error('提交攻略表单失败:', error)
    } finally {
      submitLoading.value = false
    }
  })
}

// 对话框关闭处理
const handleDialogClose = () => {
  formDialogVisible.value = false
  resetGuideForm()
}
</script>

<style lang="scss" scoped>
.guide-back-list {
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
  
  .filter-container {
    margin-bottom: 16px;
  }
  
  .search-card {
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
      
      .add-btn {
        background-color: #2ecc71;
        border-color: #2ecc71;
        
        &:hover, &:focus {
          background-color: #27ae60;
          border-color: #27ae60;
        }
      }
    }
  }
  
  .table-card {
    margin-bottom: 16px;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: none;
    
    .guide-table {
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
    }
  }
  
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    padding: 0 20px;
  }
  
  .content-preview {
    max-height: 80px;
    overflow: hidden;
    font-size: 13px;
    color: #666;
    word-break: break-all;
    line-height: 1.5;
  }
  
  .author-info {
    display: flex;
    align-items: center;
    
    .author-name {
      margin-left: 8px;
      font-size: 14px;
    }
  }
  
  .no-image {
    width: 80px;
    height: 50px;
    display: flex;
    justify-content: center;
    align-items: center;
    background-color: #f5f7fa;
    color: #909399;
    font-size: 12px;
    border-radius: 4px;
  }
  
  .date-text {
    color: #7f8c8d;
    font-size: 12px;
  }
  
  .action-btn {
    margin-right: 5px;
  }
  
  // 预览样式
  .guide-preview {
    padding: 0 20px;
    
    .preview-title {
      font-size: 24px;
      font-weight: bold;
      text-align: center;
      margin-bottom: 16px;
    }
    
    .preview-info {
      display: flex;
      justify-content: center;
      color: #909399;
      font-size: 14px;
      margin-bottom: 20px;
      
      span {
        margin: 0 10px;
      }
    }
    
    .preview-content {
      line-height: 1.8;
      font-size: 16px;
      text-align: left;
      :deep(img) {
        max-width: 100%;
        height: auto;
      }
    }
  }
  
  .guide-dialog {
    :deep(.el-dialog__header) {
      border-bottom: 1px solid #ecf0f1;
      padding: 20px;
      
      .el-dialog__title {
        font-size: 18px;
        color: #34495e;
        font-weight: 500;
      }
    }
    
    :deep(.el-dialog__body) {
      padding: 30px 20px;
    }
    
    :deep(.el-dialog__footer) {
      border-top: 1px solid #ecf0f1;
      padding: 15px 20px;
    }
  }
  
  // 封面上传
  .cover-upload {
    width: 100%;
    height: 240px;
    cursor: pointer;
    border-radius: 4px;
    overflow: hidden;
    position: relative;
    
    .cover-preview {
      width: 100%;
      height: 100%;
      position: relative;
      
      .preview-image {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      .hover-mask {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.5);
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        color: white;
        opacity: 0;
        transition: all 0.3s;
        
        .el-icon {
          font-size: 32px;
          margin-bottom: 12px;
        }
        
        span {
          font-size: 16px;
        }
      }
      
      &:hover .hover-mask {
        opacity: 1;
      }
    }
    
    .upload-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      background-color: #f8f9fa;
      border: 2px dashed #dcdfe6;
      border-radius: 4px;
      transition: all 0.3s;
      
      .el-icon {
        font-size: 48px;
        color: #c0c4cc;
        margin-bottom: 16px;
      }
      
      span {
        font-size: 16px;
        color: #606266;
        margin-bottom: 8px;
      }
      
      .upload-tips {
        font-size: 13px;
        color: #909399;
      }
      
      &:hover {
        border-color: #409EFF;
        background-color: #f0f7ff;
        
        .el-icon {
          color: #409EFF;
        }
      }
    }
  }
  
  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    
    .el-button {
      padding: 10px 24px;
      font-size: 14px;
    }
  }
}
</style> 