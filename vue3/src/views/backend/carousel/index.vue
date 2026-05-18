<template>
  <div class="carousel-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">轮播图管理</h1>
      <p class="page-subtitle">Carousel Management</p>
    </div>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <div class="action-right">
        <el-button type="primary" @click="handleAdd" class="add-btn">
          <i class="el-icon-plus"></i> 添加轮播图
        </el-button>
      </div>
    </div>

    <!-- 表格区域 -->
    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" border style="width: 100%" class="carousel-table">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="预览图" width="180" align="center">
          <template #default="scope">
            <el-image 
              :src="getImageUrl(scope.row.imageUrl)" 
              :preview-src-list="[getImageUrl(scope.row.imageUrl)]"
              :preview-teleported="true"
              style="width: 150px; height: 80px; object-fit: cover; border-radius: 4px"
              fit="cover"
            />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center">
          <template #default="scope">
            <span class="date-text">{{ scope.row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" align="center">
          <template #default="scope">
            <el-button type="primary" size="small" plain @click="handleEdit(scope.row)" class="edit-btn">
              <i class="el-icon-edit"></i> 编辑
            </el-button>
            <el-button 
              :type="scope.row.status === 1 ? 'warning' : 'success'" 
              size="small" 
              plain
              @click="handleStatusChange(scope.row)"
              class="status-btn"
            >
              <i :class="scope.row.status === 1 ? 'el-icon-close' : 'el-icon-check'"></i>
              {{ scope.row.status === 1 ? '禁用' : '启用' }}
            </el-button>
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
          layout="total, sizes, prev, pager, next, jumper"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 添加/编辑轮播图对话框 -->
    <el-dialog 
      :title="dialogType === 'add' ? '添加轮播图' : '编辑轮播图'" 
      v-model="dialogVisible" 
      width="500px"
      class="carousel-dialog"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="carousel-form">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入轮播图标题" />
        </el-form-item>
        
        <el-form-item label="图片" prop="imageUrl">
          <el-upload
            class="carousel-uploader"
            action="#"
            :auto-upload="true"
            :show-file-list="false"
            :http-request="uploadImage"
            :before-upload="beforeImageUpload"
          >
            <img v-if="imageUrl" :src="imageUrl" class="preview-image" />
            <el-icon v-else class="upload-icon"><Plus /></el-icon>
          </el-upload>
          <div class="el-upload-tip">建议尺寸: 1920×500像素, 大小不超过2MB</div>
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 基础 API 路径
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

// 表格数据
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 对话框数据
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' 或 'edit'
const formRef = ref(null)
const submitLoading = ref(false)

// 表单数据
const form = reactive({
  id: null,
  title: '',
  imageUrl: '',
  status: 1
})

// 图片预览
const imageUrl = computed(() => {
  return form.imageUrl ? (form.imageUrl.startsWith('http') ? form.imageUrl : baseAPI + form.imageUrl) : ''
})

// 获取图片URL
const getImageUrl = (url) => {
  if (!url) return ''
  return url.startsWith('http') ? url : baseAPI + url
}

// 表单验证规则
const rules = {
  title: [
    { required: false, message: '请输入轮播图标题', trigger: 'blur' },
    { max: 100, message: '标题长度不能超过100个字符', trigger: 'blur' }
  ],
  imageUrl: [
    { required: true, message: '请上传轮播图片', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 获取轮播图列表
const fetchCarousels = async () => {
  loading.value = true
  try {
    await request.get('/carousel/page', {
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        tableData.value = res.records || []
        total.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取轮播图列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 页码改变
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchCarousels()
}

// 每页条数改变
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchCarousels()
}

// 添加轮播图
const handleAdd = () => {
  dialogType.value = 'add'
  resetForm()
  dialogVisible.value = true
}

// 编辑轮播图
const handleEdit = (row) => {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, row)
  dialogVisible.value = true
}

// 修改状态
const handleStatusChange = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const statusText = newStatus === 1 ? '启用' : '禁用'
  
  try {
    await request.put(`/carousel/status/${row.id}`, null, {
      params: { status: newStatus },
      successMsg: `${statusText}成功`,
      onSuccess: () => {
        row.status = newStatus
      }
    })
  } catch (error) {
    console.error(`${statusText}失败:`, error)
  }
}

// 删除轮播图
const handleDelete = (row) => {
  ElMessageBox.confirm(
    '确认删除该轮播图吗？此操作不可恢复',
    '提示',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await request.delete(`/carousel/${row.id}`, {
        successMsg: '删除成功',
        onSuccess: () => {
          fetchCarousels()
        }
      })
    } catch (error) {
      console.error('删除失败:', error)
    }
  }).catch(() => {})
}

// 上传前的校验
const beforeImageUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG && !isPNG) {
    ElMessage.error('轮播图片只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('轮播图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 自定义上传方法
const uploadImage = async (options) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)

  try {
    await request.post('/file/upload/img', formData, {
      headers: {
        token: localStorage.getItem('token') || '',
      },
      transformRequest: [(data) => data],
      successMsg: '图片上传成功',
      onSuccess: (data) => {
        form.imageUrl = data
        options.onSuccess()
      }
    })
  } catch (error) {
    console.error('图片上传失败:', error)
    options.onError(error)
  }
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.title = ''
  form.imageUrl = ''
  form.status = 1
  
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 提交表单
const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (dialogType.value === 'add') {
          // 添加
          await request.post('/carousel', form, {
            successMsg: '添加成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchCarousels()
            }
          })
        } else {
          // 编辑
          await request.put('/carousel', form, {
            successMsg: '编辑成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchCarousels()
            }
          })
        }
      } catch (error) {
        console.error('保存失败:', error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

onMounted(() => {
  fetchCarousels()
})
</script>

<style lang="scss" scoped>
.carousel-management {
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

  .action-bar {
    margin-bottom: 20px;
    display: flex;
    justify-content: flex-end;
    
    .action-right {
      display: flex;
      justify-content: flex-end;
      
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
    border-radius: 8px;
    overflow: hidden;
    box-shadow: none;
    
    .carousel-table {
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
      
      .date-text {
        color: #7f8c8d;
        font-size: 12px;
      }
      
      .edit-btn {
        margin-right: 8px;
      }
      
      .status-btn {
        margin-right: 8px;
      }
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    padding: 0 20px;
  }

  .carousel-dialog {
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
    
    .carousel-form {
      .carousel-uploader {
        border: 1px dashed #d9d9d9;
        border-radius: 6px;
        cursor: pointer;
        position: relative;
        overflow: hidden;
        width: 300px;
        height: 150px;
        display: flex;
        justify-content: center;
        align-items: center;
        
        &:hover {
          border-color: #409EFF;
        }
      }
      
      .preview-image {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      .upload-icon {
        font-size: 28px;
        color: #8c939d;
      }
      
      .el-upload-tip {
        font-size: 12px;
        color: #606266;
        margin-top: 8px;
      }
    }
  }
}
</style> 