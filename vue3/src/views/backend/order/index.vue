<template>
  <div class="order-management">
    <div class="page-header">
      <h1 class="page-title">订单管理</h1>
      <p class="page-subtitle">Order Management</p>
    </div>

    <!-- 搜索表单 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable></el-input>
        </el-form-item>
        <el-form-item label="游客姓名">
          <el-input v-model="searchForm.visitorName" placeholder="请输入游客姓名" clearable></el-input>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="searchForm.visitorPhone" placeholder="请输入联系电话" clearable></el-input>
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="请选择订单状态" clearable>
            <el-option label="待支付" :value="0"></el-option>
            <el-option label="已支付" :value="1"></el-option>
            <el-option label="已取消" :value="2"></el-option>
            <el-option label="已退款" :value="3"></el-option>
            <el-option label="已完成" :value="4"></el-option>
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

    <!-- 订单列表表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="orderList"
        border
        style="width: 100%"
        class="order-table"
      >
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="ticketName" label="门票名称" min-width="150" />
        <el-table-column prop="visitorName" label="游客姓名" width="100" />
        <el-table-column prop="visitorPhone" label="联系电话" width="120" />
        <el-table-column prop="visitDate" label="游玩日期" width="100">
          <template #default="scope">
            {{ formatDate(scope.row.visitDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="60" align="center" />
        <el-table-column prop="totalAmount" label="总金额" width="100">
          <template #default="scope">
            <span class="amount">¥{{ scope.row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="订单状态" width="100">
          <template #default="scope">
            <el-tag :type="getOrderStatusType(scope.row.status)">
              {{ getOrderStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="150">
          <template #default="scope">
            <span class="date-text">{{ formatTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" plain @click="handleViewDetail(scope.row)" class="action-btn">
              <i class="el-icon-view"></i> 查看详情
            </el-button>
            <el-button 
              v-if="scope.row.status === 1"
              type="warning" 
              size="small"
              plain
              @click="handleRefund(scope.row)"
              class="action-btn"
            >
              <i class="el-icon-money"></i> 退款
            </el-button>
            <el-button 
              v-if="scope.row.status === 1"
              type="success" 
              size="small"
              plain
              @click="handleComplete(scope.row)"
              class="action-btn"
            >
              <i class="el-icon-check"></i> 标记完成
            </el-button>
            <el-button 
              v-if="[2, 3, 4].includes(scope.row.status)"
              type="danger" 
              size="small"
              plain
              @click="handleDelete(scope.row)"
              class="action-btn"
            >
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

    <!-- 订单详情对话框 -->
    <el-dialog
      title="订单详情"
      v-model="detailDialogVisible"
      width="650px"
      class="order-dialog"
    >
      <div class="order-detail" v-if="currentOrder">
        <el-descriptions title="订单信息" :column="1" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="门票名称">{{ currentOrder.ticketName }}</el-descriptions-item>
          <el-descriptions-item label="景点名称">{{ currentOrder.scenicName }}</el-descriptions-item>
          <el-descriptions-item label="游玩日期">{{ formatDate(currentOrder.visitDate) }}</el-descriptions-item>
          <el-descriptions-item label="购买数量">{{ currentOrder.quantity }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">¥{{ currentOrder.totalAmount }}</el-descriptions-item>
        </el-descriptions>
        
        <el-descriptions title="游客信息" :column="1" border style="margin-top: 20px">
          <el-descriptions-item label="游客姓名">{{ currentOrder.visitorName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentOrder.visitorPhone }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">
            {{ currentOrder.idCard || '未提供' }}
          </el-descriptions-item>
        </el-descriptions>
        
        <el-descriptions title="订单状态" :column="1" border style="margin-top: 20px">
          <el-descriptions-item label="订单状态">
            <el-tag :type="getOrderStatusType(currentOrder.status)">
              {{ getOrderStatusText(currentOrder.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">
            {{ formatTime(currentOrder.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="支付时间" v-if="currentOrder.paymentTime">
            {{ formatTime(currentOrder.paymentTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="支付方式" v-if="currentOrder.paymentMethod">
            {{ getPaymentMethodText(currentOrder.paymentMethod) }}
          </el-descriptions-item>
        </el-descriptions>
        
        <el-descriptions title="用户信息" :column="1" border style="margin-top: 20px">
          <el-descriptions-item label="用户ID">{{ currentOrder.userId }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ currentOrder.username }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button 
            v-if="currentOrder && currentOrder.status === 1" 
            type="warning" 
            plain
            @click="handleRefund(currentOrder)"
          >
            <i class="el-icon-money"></i> 退款
          </el-button>
          <el-button 
            v-if="currentOrder && currentOrder.status === 1" 
            type="success" 
            plain
            @click="handleComplete(currentOrder)"
          >
            <i class="el-icon-check"></i> 标记完成
          </el-button>
          <el-button 
            v-if="currentOrder && [2, 3, 4].includes(currentOrder.status)" 
            type="danger" 
            plain
            @click="handleDelete(currentOrder)"
          >
            <i class="el-icon-delete"></i> 删除
          </el-button>
        </span>
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

// 订单列表数据
const orderList = ref([])
const loading = ref(false)

// 搜索表单
const searchForm = reactive({
  orderNo: '',
  visitorName: '',
  visitorPhone: '',
  status: null
})

// 当前查看的订单
const currentOrder = ref(null)
const detailDialogVisible = ref(false)

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    await request.get('/order/admin/page', {
      orderNo: searchForm.orderNo,
      visitorName: searchForm.visitorName,
      visitorPhone: searchForm.visitorPhone,
      status: searchForm.status,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        orderList.value = res.records || []
        total.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索按钮点击事件
const handleSearch = () => {
  currentPage.value = 1
  fetchOrders()
}

// 重置搜索条件
const resetSearch = () => {
  searchForm.orderNo = ''
  searchForm.visitorName = ''
  searchForm.visitorPhone = ''
  searchForm.status = null
  currentPage.value = 1
  fetchOrders()
}

// 分页变化事件
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchOrders()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchOrders()
}

// 查看订单详情
const handleViewDetail = async (row) => {
  try {
    await request.get(`/order/${row.id}`, {}, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        currentOrder.value = res
        detailDialogVisible.value = true
      }
    })
  } catch (error) {
    console.error('获取订单详情失败:', error)
  }
}

// 退款操作
const handleRefund = (row) => {
  ElMessageBox.confirm(`确定要对订单"${row.orderNo}"进行退款操作吗？`, '退款确认', {
    confirmButtonText: '确定退款',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.post(`/order/${row.id}/refund`, {}, {
        successMsg: '订单退款成功',
        onSuccess: () => {
          fetchOrders()
          if (detailDialogVisible.value) {
            detailDialogVisible.value = false
          }
        }
      })
    } catch (error) {
      console.error('订单退款失败:', error)
    }
  }).catch(() => {})
}

// 完成订单操作
const handleComplete = (row) => {
  ElMessageBox.confirm(`确定要将订单"${row.orderNo}"标记为已完成吗？`, '确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.post(`/order/${row.id}/complete`, {}, {
        successMsg: '订单已标记为完成',
        onSuccess: () => {
          fetchOrders()
          if (detailDialogVisible.value) {
            detailDialogVisible.value = false
          }
        }
      })
    } catch (error) {
      console.error('标记订单完成失败:', error)
    }
  }).catch(() => {})
}

// 删除订单操作
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除订单"${row.orderNo}"吗？此操作不可恢复！`, '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/order/${row.id}`, {
        successMsg: '订单删除成功',
        onSuccess: () => {
          fetchOrders()
          if (detailDialogVisible.value) {
            detailDialogVisible.value = false
          }
        }
      })
    } catch (error) {
      console.error('删除订单失败:', error)
    }
  }).catch(() => {})
}

// 获取订单状态文本
const getOrderStatusText = (status) => {
  const statusMap = {
    0: '待支付',
    1: '已支付',
    2: '已取消',
    3: '已退款',
    4: '已完成'
  }
  return statusMap[status] || '未知状态'
}

// 获取订单状态类型（用于标签颜色）
const getOrderStatusType = (status) => {
  const typeMap = {
    0: 'warning',   // 待支付
    1: 'success',   // 已支付
    2: 'info',      // 已取消
    3: 'danger',    // 已退款
    4: 'primary'    // 已完成
  }
  return typeMap[status] || 'info'
}

// 获取支付方式文本
const getPaymentMethodText = (method) => {
  const methodMap = {
    'WECHAT': '微信支付',
    'ALIPAY': '支付宝',
    'BANK_CARD': '银行卡'
  }
  return methodMap[method] || '未知方式'
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
}

// 页面加载时获取数据
onMounted(() => {
  fetchOrders()
})
</script>

<style lang="scss" scoped>
.order-management {
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
    
    .order-table {
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
      
      .amount {
        font-weight: 600;
        color: #e74c3c;
      }
      
      .date-text {
        color: #7f8c8d;
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

  .order-dialog {
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