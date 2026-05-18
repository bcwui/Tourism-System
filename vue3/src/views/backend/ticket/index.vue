<template>
  <div class="ticket-management">
    <div class="page-header">
      <h1 class="page-title">门票管理</h1>
      <p class="page-subtitle">Ticket Management</p>
    </div>

    <div class="action-bar">
      <div class="action-right">
        <el-button type="primary" @click="showAddDialog" class="add-btn">
          <i class="el-icon-plus"></i> 添加门票
        </el-button>
      </div>
    </div>

    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="门票名称">
          <el-input v-model="searchForm.ticketName" placeholder="请输入门票名称" clearable></el-input>
        </el-form-item>
        <el-form-item label="门票类型">
          <el-select v-model="searchForm.ticketType" placeholder="请选择门票类型" clearable>
            <el-option label="成人票" value="成人票"></el-option>
            <el-option label="儿童票" value="儿童票"></el-option>
            <el-option label="学生票" value="学生票"></el-option>
            <el-option label="老人票" value="老人票"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="景点">
          <el-select 
            v-model="searchForm.scenicId" 
            placeholder="请选择景点" 
            clearable
            filterable
          >
            <el-option
              v-for="item in allScenicSpots"
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

    <!-- 门票列表表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="ticketList"
        border
        style="width: 100%"
        class="ticket-table"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="ticketName" label="门票名称" min-width="150" />
        <el-table-column prop="scenicId" label="所属景点" width="150">
          <template #default="scope">
            <span class="scenic-name">{{ getScenicSpotName(scope.row.scenicId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="原价" width="100">
          <template #default="scope">
            <span class="price">¥{{ scope.row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="discountPrice" label="优惠价" width="100">
          <template #default="scope">
            <span class="discount-price">{{ scope.row.discountPrice ? `¥${scope.row.discountPrice}` : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ticketType" label="票种类型" width="100">
          <template #default="scope">
            <el-tag size="small" effect="plain">{{ scope.row.ticketType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="validPeriod" label="有效期" width="150" />
        <el-table-column prop="stock" label="库存" width="80">
          <template #default="scope">
            <span class="stock">{{ scope.row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '可预订' : '不可预订' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" plain @click="handleEdit(scope.row)" class="action-btn">
              <i class="el-icon-edit"></i> 编辑
            </el-button>
            <el-button 
              :type="scope.row.status === 1 ? 'warning' : 'success'" 
              size="small"
              plain
              @click="handleToggleStatus(scope.row)"
              class="action-btn"
            >
              <i :class="scope.row.status === 1 ? 'el-icon-close' : 'el-icon-check'"></i>
              {{ scope.row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button type="danger" size="small" plain @click="handleDelete(scope.row)" class="action-btn">
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
          :total="total"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :current-page="currentPage"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 添加/编辑门票对话框 -->
    <el-dialog
      :title="isEdit ? '编辑门票' : '添加门票'"
      v-model="dialogVisible"
      width="60%"
      class="ticket-dialog"
    >
      <el-form 
        ref="ticketFormRef" 
        :model="ticketForm" 
        :rules="ticketRules" 
        label-width="120px"
        :disabled="formLoading"
      >
        <el-form-item label="关联景点" prop="scenicId">
          <el-select
            v-model="ticketForm.scenicId"
            placeholder="请选择关联景点"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="item in allScenicSpots"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="门票名称" prop="ticketName">
          <el-input v-model="ticketForm.ticketName" placeholder="请输入门票名称" />
        </el-form-item>
        <el-form-item label="门票类型" prop="ticketType">
          <el-select v-model="ticketForm.ticketType" placeholder="请选择门票类型" style="width: 100%">
            <el-option label="成人票" value="成人票"></el-option>
            <el-option label="儿童票" value="儿童票"></el-option>
            <el-option label="学生票" value="学生票"></el-option>
            <el-option label="老人票" value="老人票"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="门票价格" prop="price">
          <el-input-number v-model="ticketForm.price" :precision="2" :min="0" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="优惠价格" prop="discountPrice">
          <el-input-number v-model="ticketForm.discountPrice" :precision="2" :min="0" :step="10" style="width: 100%" />
          <div class="form-tip">不填则表示无优惠价</div>
        </el-form-item>
        <el-form-item label="有效期" prop="validPeriod">
          <el-input v-model="ticketForm.validPeriod" placeholder="例如：购买后30日内有效" />
        </el-form-item>
        <el-form-item label="门票库存" prop="stock">
          <el-input-number v-model="ticketForm.stock" :min="0" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="门票状态" prop="status">
          <el-switch
            v-model="ticketForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="可预订"
            inactive-text="不可预订"
          />
        </el-form-item>
        <el-form-item label="门票描述">
          <el-input v-model="ticketForm.description" type="textarea" :rows="4" placeholder="请输入门票描述信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="formLoading">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 门票列表数据
const ticketList = ref([])
const loading = ref(false)

// 搜索表单
const searchForm = reactive({
  ticketName: '',
  ticketType: '',
  scenicId: null
})

// 景点选择器数据
const scenicOptions = ref([])
const scenicLoading = ref(false)
const allScenicSpots = ref([]) // 添加全部景点缓存

// 对话框控制
const dialogVisible = ref(false)
const isEdit = ref(false)
const formLoading = ref(false)
const ticketFormRef = ref(null)

// 门票表单数据
const ticketForm = reactive({
  id: null,
  scenicId: null,
  ticketName: '',
  price: 0,
  discountPrice: null,
  ticketType: '',
  validPeriod: '',
  description: '',
  stock: 0,
  status: 1
})

// 表单验证规则
const ticketRules = {
  scenicId: [
    { required: true, message: '请选择关联景点', trigger: 'change' }
  ],
  ticketName: [
    { required: true, message: '请输入门票名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入门票价格', trigger: 'blur' }
  ],
  ticketType: [
    { required: true, message: '请选择门票类型', trigger: 'change' }
  ],
  validPeriod: [
    { required: true, message: '请输入有效期', trigger: 'blur' }
  ],
  stock: [
    { required: true, message: '请输入库存数量', trigger: 'blur' }
  ]
}

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
        ticketList.value = res.records || []
        total.value = res.total || 0
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
    // 当输入为空时，显示所有已加载的景点
    if (allScenicSpots.value.length > 0) {
      scenicOptions.value = allScenicSpots.value
      return
    }
  }
  
  scenicLoading.value = true
  try {
    if (query === '') {
      // 获取所有景点
      await request.get('/scenic/all', {}, {
        showDefaultMsg: false,
        onSuccess: (res) => {
          allScenicSpots.value = res || []
          scenicOptions.value = allScenicSpots.value
        }
      })
    } else {
      // 按名称搜索景点
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
    }
  } catch (error) {
    console.error('获取景点列表失败:', error)
  } finally {
    scenicLoading.value = false
  }
}

// 获取所有景点
const fetchAllScenicSpots = async () => {
  try {
    await request.get('/scenic/all', {}, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        allScenicSpots.value = res || []
      }
    })
  } catch (error) {
    console.error('获取所有景点失败:', error)
  }
}

// 搜索按钮点击事件
const handleSearch = () => {
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
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchTickets()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchTickets()
}

// 显示添加对话框
const showAddDialog = () => {
  isEdit.value = false
  resetForm()
  // 如果没有预加载景点数据，则加载
  if (allScenicSpots.value.length === 0) {
    fetchScenicOptions('')
  } else {
    scenicOptions.value = allScenicSpots.value
  }
  dialogVisible.value = true
}

// 编辑门票
const handleEdit = (row) => {
  isEdit.value = true
  resetForm()
  
  // 填充表单数据
  Object.keys(ticketForm).forEach(key => {
    if (row[key] !== undefined) {
      ticketForm[key] = row[key]
    }
  })
  
  // 确保景点选项已加载
  if (allScenicSpots.value.length === 0) {
    fetchAllScenicSpots().then(() => {
      dialogVisible.value = true
    })
  } else {
    dialogVisible.value = true
  }
}

// 切换门票状态
const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const statusText = newStatus === 1 ? '上架' : '下架'
  
  try {
    await request.put(`/ticket/${row.id}`, {
      status: newStatus
    }, {
      successMsg: `门票${statusText}成功`,
      onSuccess: () => {
        row.status = newStatus
      }
    })
  } catch (error) {
    console.error(`门票${statusText}失败:`, error)
  }
}

// 删除门票
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除门票"${row.ticketName}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/ticket/${row.id}`, {
        successMsg: '门票删除成功',
        onSuccess: () => {
          fetchTickets()
        }
      })
    } catch (error) {
      console.error('删除门票失败:', error)
    }
  }).catch(() => {})
}

// 重置表单
const resetForm = () => {
  if (ticketFormRef.value) {
    ticketFormRef.value.resetFields()
  }
  
  ticketForm.id = null
  ticketForm.scenicId = null
  ticketForm.ticketName = ''
  ticketForm.price = 0
  ticketForm.discountPrice = null
  ticketForm.ticketType = ''
  ticketForm.validPeriod = ''
  ticketForm.description = ''
  ticketForm.stock = 0
  ticketForm.status = 1
}

// 提交表单
const submitForm = async () => {
  ticketFormRef.value.validate(async (valid) => {
    if (valid) {
      formLoading.value = true
      
      try {
        if (isEdit.value) {
          // 更新门票
          await request.put(`/ticket/${ticketForm.id}`, ticketForm, {
            successMsg: '门票更新成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchTickets()
            }
          })
        } else {
          // 添加门票
          await request.post('/ticket', ticketForm, {
            successMsg: '门票添加成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchTickets()
            }
          })
        }
      } catch (error) {
        console.error(isEdit.value ? '更新门票失败:' : '添加门票失败:', error)
      } finally {
        formLoading.value = false
      }
    }
  })
}

// 获取景点名称
const getScenicSpotName = (scenicId) => {
  if (!scenicId || allScenicSpots.value.length === 0) return '-'
  const scenic = allScenicSpots.value.find(item => item.id === scenicId)
  return scenic ? scenic.name : '-'
}

// 页面加载时获取门票列表和景点数据
onMounted(() => {
  fetchTickets()
  // 预加载所有景点数据
  fetchAllScenicSpots()
})
</script>

<style lang="scss" scoped>
.ticket-management {
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
    
    .ticket-table {
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
      
      .scenic-name {
        color: #2980b9;
        font-weight: 500;
      }
      
      .price {
        font-weight: 600;
        color: #e74c3c;
      }
      
      .discount-price {
        font-weight: 600;
        color: #27ae60;
      }
      
      .stock {
        font-weight: 500;
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

  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 5px;
  }

  .ticket-dialog {
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
}
</style> 