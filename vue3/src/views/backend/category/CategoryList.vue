<template>
  <div class="category-management">
    <div class="page-header">
      <h1 class="page-title">景点分类管理</h1>
      <p class="page-subtitle">Scenic Spot Category Management</p>
    </div>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <div class="action-right">
        <el-button type="primary" @click="handleAdd" class="add-btn">
          <i class="el-icon-plus"></i> 新增分类
        </el-button>
      </div>
    </div>

    <!-- 搜索表单 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="分类名称">
          <el-input v-model="searchForm.name" placeholder="请输入分类名称" clearable>
            <template #prefix>
              <i class="el-icon-search"></i>
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

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" style="width: 100%" v-loading="loading" border class="category-table">
        <el-table-column label="ID" prop="id" width="80" align="center" />
        <el-table-column label="分类名称" prop="name">
          <template #default="scope">
            <div class="category-name">{{ scope.row.name }}</div>
          </template>
        </el-table-column>
        <el-table-column label="分类描述" prop="description" show-overflow-tooltip />
        <el-table-column label="排序" prop="sortOrder" width="100" align="center">
          <template #default="scope">
            <span class="order-tag">{{ scope.row.sortOrder }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="scope">
            <span class="date-text">{{ formatDate(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="scope">
            <el-button type="primary" size="small" plain @click="handleEdit(scope.row)" class="edit-btn">
              <i class="el-icon-edit"></i> 编辑
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
          layout="total, prev, pager, next"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="dialogType === 'add' ? '新增分类' : '编辑分类'"
      v-model="dialogVisible"
      width="500px"
      class="category-dialog"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
        class="category-form"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类描述" prop="description">
          <el-input v-model="form.description" type="textarea" rows="3" placeholder="请输入分类描述" />
        </el-form-item>
        <el-form-item label="父级分类">
          <el-select v-model="form.parentId" placeholder="请选择父级分类" clearable style="width: 100%">
            <el-option label="无（作为一级分类）" :value="0" />
            <el-option
              v-for="item in parentCategories"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDate } from '@/utils/dateUtils'

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchForm = reactive({
  name: ''
})
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' 或 'edit'
const form = reactive({
  id: null,
  name: '',
  description: '',
  parentId: 0,
  sortOrder: 0
})
const formRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}
const formRef = ref(null)
const submitting = ref(false)
const parentCategories = ref([])

// 基础API路径
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

// 获取分类列表
const fetchCategories = async () => {
  loading.value = true
  try {
    await request.get('/scenic-category/page', {
      name: searchForm.name,
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
    console.error('获取分类列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取父级分类选项（只获取一级分类）
const fetchParentCategories = async () => {
  try {
    await request.get('/scenic-category/tree', {}, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        parentCategories.value = res
      }
    })
  } catch (error) {
    console.error('获取父级分类失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchCategories()
}

// 重置搜索
const resetSearch = () => {
  searchForm.name = ''
  currentPage.value = 1
  fetchCategories()
}

// 分页变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchCategories()
}

// 打开新增对话框
const handleAdd = () => {
  dialogType.value = 'add'
  resetForm()
  fetchParentCategories()
  dialogVisible.value = true
}

// 打开编辑对话框
const handleEdit = (row) => {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, row)
  dialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.name = ''
  form.description = ''
  form.parentId = 0
  form.sortOrder = 0
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (dialogType.value === 'add') {
          // 新增
          await request.post('/scenic-category', form, {
            successMsg: '新增成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchCategories()
            }
          })
        } else {
          // 编辑
          await request.put(`/scenic-category/${form.id}`, form, {
            successMsg: '更新成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchCategories()
            }
          })
        }
      } finally {
        submitting.value = false
      }
    }
  })
}

// 删除分类
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该分类吗？如果有景点使用此分类，删除可能会导致数据异常', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/scenic-category/${row.id}`, {
        successMsg: '删除成功',
        onSuccess: () => {
          fetchCategories()
        }
      })
    } catch (error) {
      console.error('删除分类失败:', error)
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchCategories()
})
</script>

<style lang="scss" scoped>
.category-management {
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
    
    .category-table {
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
      
      .category-name {
        font-weight: 500;
        color: #2980b9;
      }
      
      .order-tag {
        background-color: #ecf0f1;
        color: #7f8c8d;
        padding: 2px 8px;
        border-radius: 10px;
        font-size: 12px;
      }
      
      .date-text {
        color: #7f8c8d;
        font-size: 12px;
      }
      
      .edit-btn {
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

  .category-dialog {
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
    
    .category-form {
    }
  }
}
</style> 