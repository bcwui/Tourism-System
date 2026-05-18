<template>
  <div class="accommodation-management">
    <div class="page-header">
      <h1 class="page-title">住宿管理</h1>
      <p class="page-subtitle">Accommodation Management</p>
    </div>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <div class="action-right">
        <el-button type="primary" @click="handleAdd" class="add-btn">
          <i class="el-icon-plus"></i> 添加住宿
        </el-button>
      </div>
    </div>
    
    <!-- 搜索和操作栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="住宿名称">
          <el-input v-model="searchForm.name" placeholder="输入住宿名称" clearable />
        </el-form-item>
        
        <el-form-item label="住宿类型">
          <el-select v-model="searchForm.type" placeholder="选择住宿类型" clearable>
            <el-option
              v-for="item in typeOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="关联景点">
          <el-select v-model="searchForm.scenicId" placeholder="选择关联景点" clearable>
            <el-option
              v-for="item in scenicOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch" class="search-btn">
            <i class="el-icon-search"></i> 查询
          </el-button>
          <el-button @click="resetSearch" class="reset-btn">
            <i class="el-icon-refresh"></i> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 数据表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
        class="accommodation-table"
      >
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="name" label="住宿名称" min-width="120">
          <template #default="scope">
            <span class="accommodation-name">{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="住宿类型" width="120">
          <template #default="scope">
            <el-tag size="small" effect="plain">{{ scope.row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priceRange" label="价格区间" width="120">
          <template #default="scope">
            <span class="price-range">{{ scope.row.priceRange }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="starLevel" label="评分" width="120">
          <template #default="{ row }">
            <el-rate
              v-model="row.starLevel"
              disabled
              show-score
              text-color="#ff9900"
            />
          </template>
        </el-table-column>
        
        <el-table-column prop="address" label="地址" min-width="200" />
        <el-table-column prop="scenicName" label="关联景点" width="120">
          <template #default="scope">
            <span class="scenic-name">{{ scope.row.scenicName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="getImageUrl(row.imageUrl)"
              :preview-teleported="true"
              :preview-src-list="[getImageUrl(row.imageUrl)]"
              style="height: 40px; width: 60px; border-radius: 4px"
              fit="cover"
            />
            <span v-else class="no-image">无图片</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="distance" label="距景点距离" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" plain @click="handleEdit(row)" class="action-btn">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" size="small" plain @click="handleDelete(row)" class="action-btn">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 住宿表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑住宿' : '添加住宿'"
      width="650px"
      :close-on-click-modal="false"
      class="accommodation-dialog"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        :disabled="formLoading"
      >
        <el-form-item label="住宿名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入住宿名称" />
        </el-form-item>
        
        <el-form-item label="住宿类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择住宿类型" style="width: 100%">
            <el-option
              v-for="item in typeOptions"
              :key="item"
              :label="item"
              :value="item"
            />
 
          </el-select>
        </el-form-item>
        
        <el-form-item label="关联景点" prop="scenicId">
          <el-select v-model="form.scenicId" placeholder="请选择关联景点" style="width: 100%">
            <el-option
              v-for="item in scenicOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="价格区间" prop="priceRange">
          <el-input v-model="form.priceRange" placeholder="例如: 200-500" />
        </el-form-item>
        
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入详细地址" />
        </el-form-item>
        
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        
        <el-form-item label="距景点距离" prop="distance">
          <el-input v-model="form.distance" placeholder="例如: 500米" />
        </el-form-item>
        
        <el-form-item label="特色服务" prop="features">
          <el-input v-model="form.features" placeholder="请输入特色服务，用逗号分隔" />
        </el-form-item>
        
        <el-form-item label="住宿图片">
          <el-upload
            class="avatar-uploader"
            action="#"
            :auto-upload="true"
            :show-file-list="false"
            :http-request="customUploadImage"
            :before-upload="beforeImageUpload"
          >
            <img
              v-if="form.imageUrl"
              :src="getImageUrl(form.imageUrl)"
              class="avatar"
            />
            <el-icon v-else class="avatar-uploader-icon">
              <Plus />
            </el-icon>
          </el-upload>
          <div class="upload-tip">
            图片格式: JPG/PNG, 大小不超过2MB
          </div>
        </el-form-item>
        
        <el-form-item label="详细描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入住宿详细描述"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="formLoading" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'

// 表格数据
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 查询条件
const searchForm = reactive({
  name: '',
  type: '',
  scenicId: ''
})

// 表单数据
const dialogVisible = ref(false)
const isEdit = ref(false)
const formLoading = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  name: '',
  type: '',
  address: '',
  scenicId: null,
  description: '',
  contactPhone: '',
  priceRange: '',
  starLevel: 0,
  imageUrl: '',
  features: '',
  distance: '',
})

// 表单验证规则
const rules = {
  name: [{ required: true, message: '请输入住宿名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择住宿类型', trigger: 'change' }],
  address: [{ required: true, message: '请输入住宿地址', trigger: 'blur' }],
  priceRange: [{ required: true, message: '请输入价格区间', trigger: 'blur' }]
}

// 选项数据
const scenicOptions = ref([])
const typeOptions = ref(['酒店', '民宿', '客栈', '青旅', '度假村'])

// 获取住宿列表
const fetchAccommodations = async () => {
  loading.value = true
  try {
    // 构建查询参数
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value
    }
    
    // 添加查询条件
    if (searchForm.name) params.name = searchForm.name
    if (searchForm.type) params.type = searchForm.type
    if (searchForm.scenicId) params.scenicId = searchForm.scenicId
    
    await request.get('/accommodation/page', params, {
      onSuccess: (res) => {
        tableData.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取住宿列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取景点列表
const fetchScenicOptions = async () => {
  try {
    await request.get('/scenic/all', {}, {
      onSuccess: (res) => {
        scenicOptions.value = res
      }
    })
  } catch (error) {
    console.error('获取景点列表失败:', error)
  }
}

// 获取住宿类型
const fetchAccommodationTypes = async () => {
  try {
    await request.get('/accommodation/types', {}, {
      onSuccess: (res) => {
        // 如果API返回的类型不为空，则合并预设类型和API返回的类型
        if (res && res.length > 0) {
          const defaultTypes = ['酒店', '民宿', '客栈', '青旅', '度假村']
          const mergedTypes = [...new Set([...defaultTypes, ...res])]
          typeOptions.value = mergedTypes
        }
      }
    })
  } catch (error) {
    console.error('获取住宿类型失败:', error)
  }
}

// 处理图片URL
const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return baseAPI + url
}

// 搜索处理
const handleSearch = () => {
  currentPage.value = 1
  fetchAccommodations()
}

// 重置搜索条件
const resetSearch = () => {
  searchForm.name = ''
  searchForm.type = ''
  searchForm.scenicId = ''
  currentPage.value = 1
  fetchAccommodations()
}

// 分页处理
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchAccommodations()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchAccommodations()
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  
  Object.assign(form, {
    id: null,
    name: '',
    type: '',
    address: '',
    scenicId: null,
    description: '',
    contactPhone: '',
    priceRange: '',
    starLevel: 0,
    imageUrl: '',
    features: '',
    distance: '',
  })
}

// 添加住宿
const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑住宿
const handleEdit = (row) => {
  resetForm()
  isEdit.value = true
  dialogVisible.value = true
  
  // 填充表单数据
  Object.assign(form, { ...row })
}

// 删除住宿
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除住宿 "${row.name}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/accommodation/${row.id}`, {
        successMsg: '删除成功',
        onSuccess: () => {
          fetchAccommodations()
        }
      })
    } catch (error) {
      console.error('删除住宿失败:', error)
    }
  }).catch(() => {})
}

// 上传图片前的校验
const beforeImageUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG && !isPNG) {
    ElMessage.error('上传图片只能是 JPG 或 PNG 格式!')
    return false
  }
  
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB!')
    return false
  }
  
  return true
}

// 自定义上传图片
const customUploadImage = async (options) => {
  try {
    const { file } = options
    
    // 创建FormData对象
    const formData = new FormData()
    formData.append('file', file)
    
    // 设置上传选项
    const uploadOptions = {
      headers: {
        token: localStorage.getItem('token') || ''
      },
      transformRequest: [(data) => data],
      successMsg: '图片上传成功',
      errorMsg: '图片上传失败',
      onSuccess: (data) => {
        form.imageUrl = data
        options.onSuccess({ data })
      },
      onError: (error) => {
        options.onError(new Error(error.message || '上传失败'))
      }
    }
    
    // 发送上传请求
    await request.post('/file/upload/img', formData, uploadOptions)
  } catch (error) {
    options.onError(error)
    console.error('图片上传过程发生错误:', error)
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      formLoading.value = true
      try {
        if (isEdit.value) {
          // 更新
          await request.put(`/accommodation/${form.id}`, form, {
            successMsg: '更新成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchAccommodations()
            }
          })
        } else {
          // 新增
          await request.post('/accommodation', form, {
            successMsg: '添加成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchAccommodations()
            }
          })
        }
      } catch (error) {
        console.error(`${isEdit.value ? '更新' : '添加'}住宿失败:`, error)
      } finally {
        formLoading.value = false
      }
    }
  })
}

// 初始加载
onMounted(() => {
  fetchScenicOptions()
  fetchAccommodationTypes()
  fetchAccommodations()
})
</script>

<style lang="scss" scoped>
.accommodation-management {
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
    
    .accommodation-table {
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
      
      .accommodation-name {
        font-weight: 500;
        color: #2980b9;
      }
      
      .scenic-name {
        color: #16a085;
        font-weight: 500;
      }
      
      .price-range {
        font-weight: 600;
        color: #e74c3c;
      }
      
      .no-image {
        color: #95a5a6;
        font-size: 12px;
      }
      
      .action-btn {
        margin-right: 5px;
      }
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    padding: 0 20px;
  }

  .accommodation-dialog {
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
    
    .dialog-footer {
      display: flex;
      justify-content: flex-end;
    }
  }

  .avatar-uploader {
    width: 178px;
    height: 178px;
    display: block;
    
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      width: 178px;
      height: 178px;
      transition: border-color 0.3s;
      
      &:hover {
        border-color: #409eff;
      }
    }
  }

  .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 178px;
    height: 178px;
    text-align: center;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .avatar {
    width: 178px;
    height: 178px;
    display: block;
    object-fit: cover;
  }

  .upload-tip {
    margin-top: 8px;
    font-size: 12px;
    color: #606266;
  }
}
</style> 